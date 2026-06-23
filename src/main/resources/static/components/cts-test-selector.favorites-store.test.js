import { describe, it, expect, beforeEach } from "vitest";
import { createFavoritesController, attachFavorites } from "./cts-test-selector.favorites-store.js";

/**
 * Map-backed Storage stand-in so the controller is testable without a DOM.
 * @returns {{
 *   getItem: (k: string) => string | null,
 *   setItem: (k: string, v: string) => void,
 *   removeItem: (k: string) => void,
 *   _map: Map<string, string>,
 * }} A minimal Storage-shaped object.
 */
function fakeStorage() {
  const m = new Map();
  return {
    getItem: (k) => (m.has(k) ? m.get(k) : null),
    setItem: (k, v) => m.set(k, String(v)),
    removeItem: (k) => m.delete(k),
    _map: m,
  };
}

/**
 * @typedef {{ detail: { plan: string, favorite: boolean } }} ToggleEvent - The
 *   shape attachFavorites destructures off a cts-favorite-toggle event.
 */

/**
 * DOM-free stand-in for the cts-test-selector host: just the `favorites`
 * prop attachFavorites reads/writes plus a tiny event bus, so the optimistic
 * wiring is testable without jsdom or DOM globals.
 * @returns {{
 *   favorites: string[],
 *   addEventListener: (type: string, fn: (e: ToggleEvent) => void) => void,
 *   removeEventListener: (type: string, fn: (e: ToggleEvent) => void) => void,
 *   emit: (type: string, detail: { plan: string, favorite: boolean }) => void,
 * }} A minimal host.
 */
function fakeHost() {
  /** @type {Record<string, Array<(e: ToggleEvent) => void>>} */
  const listeners = {};
  return {
    favorites: [],
    addEventListener(type, fn) {
      (listeners[type] ||= []).push(fn);
    },
    removeEventListener(type, fn) {
      listeners[type] = (listeners[type] || []).filter((f) => f !== fn);
    },
    emit(type, detail) {
      (listeners[type] || []).forEach((fn) => fn({ detail }));
    },
  };
}

describe("createFavoritesController", () => {
  /** @type {ReturnType<typeof fakeStorage>} */
  let storage;
  beforeEach(() => {
    storage = fakeStorage();
  });

  it("starts empty and mirrors the { plans } wire shape", async () => {
    const c = createFavoritesController({ storage });
    expect(c.snapshot()).toEqual([]);
    expect(await c.get()).toEqual({ plans: [] });
  });

  it("add persists and re-get returns the new set (insertion order)", async () => {
    const c = createFavoritesController({ storage });
    expect(await c.add("a")).toEqual({ plans: ["a"] });
    expect(await c.add("b")).toEqual({ plans: ["a", "b"] });
    // A fresh controller over the same storage sees the persisted set —
    // this is what makes "persist across reload" work in the stories.
    const reloaded = createFavoritesController({ storage });
    expect(await reloaded.get()).toEqual({ plans: ["a", "b"] });
  });

  it("add is idempotent", async () => {
    const c = createFavoritesController({ storage });
    await c.add("a");
    expect(await c.add("a")).toEqual({ plans: ["a"] });
  });

  it("remove reverses add", async () => {
    const c = createFavoritesController({ storage });
    await c.add("a");
    await c.add("b");
    expect(await c.remove("a")).toEqual({ plans: ["b"] });
    expect(c.snapshot()).toEqual(["b"]);
  });

  it("a failed add rejects and leaves storage unchanged (so the caller reverts)", async () => {
    const c = createFavoritesController({ storage, failOn: "boom" });
    await c.add("a");
    await expect(c.add("boom")).rejects.toThrow(/save failed/);
    expect(c.snapshot()).toEqual(["a"]);
  });

  it("a failed remove rejects and leaves storage unchanged", async () => {
    const c = createFavoritesController({ storage, failOn: (_name, op) => op === "remove" });
    await c.add("a");
    await expect(c.remove("a")).rejects.toThrow(/remove failed/);
    expect(c.snapshot()).toEqual(["a"]);
  });

  it("reset clears persisted state between stories", async () => {
    const c = createFavoritesController({ storage });
    await c.add("a");
    c.reset();
    expect(c.snapshot()).toEqual([]);
  });

  it("tolerates corrupt storage as empty", async () => {
    storage.setItem("cts:favorite-plans", "{not json");
    const c = createFavoritesController({ storage });
    expect(c.snapshot()).toEqual([]);
  });

  it("surfaces a real storage write error as a rejection (so the page can toast)", async () => {
    // A quota / private-mode setItem throw is distinct from corrupt-read
    // tolerance: the write must reject, not swallow, so attachFavorites reverts
    // and toasts.
    const throwing = {
      getItem: () => null,
      setItem: () => {
        throw new Error("QuotaExceededError");
      },
      removeItem: () => {},
    };
    const c = createFavoritesController({ storage: throwing });
    await expect(c.add("a")).rejects.toThrow();
  });
});

describe("attachFavorites", () => {
  /** @type {ReturnType<typeof fakeStorage>} */
  let storage;
  beforeEach(() => {
    storage = fakeStorage();
  });

  it("optimistically adds then reconciles with the persisted truth", async () => {
    const c = createFavoritesController({ storage });
    const host = fakeHost();
    attachFavorites(
      /** @type {HTMLElement & { favorites: string[] }} */ (/** @type {unknown} */ (host)),
      c,
    );
    host.emit("cts-favorite-toggle", { plan: "a", favorite: true });
    expect(host.favorites).toEqual(["a"]); // optimistic, before persist resolves
    await new Promise((r) => setTimeout(r, 0));
    expect(host.favorites).toEqual(["a"]); // reconciled with storage
    expect(c.snapshot()).toEqual(["a"]);
  });

  it("a failed toggle reverts only that plan, preserving a concurrent toggle", async () => {
    // Star A (will fail) then B (succeeds) before A's persist resolves. With a
    // stale-snapshot revert, A's failure would wipe B; the per-plan revert
    // leaves B starred.
    const c = createFavoritesController({ storage, latency: 5, failOn: "A" });
    const host = fakeHost();
    attachFavorites(
      /** @type {HTMLElement & { favorites: string[] }} */ (/** @type {unknown} */ (host)),
      c,
    );
    host.emit("cts-favorite-toggle", { plan: "A", favorite: true });
    host.emit("cts-favorite-toggle", { plan: "B", favorite: true });
    expect(host.favorites).toEqual(["A", "B"]); // both optimistic
    await new Promise((r) => setTimeout(r, 20));
    expect(host.favorites).toEqual(["B"]); // A reverted, B preserved
    expect(c.snapshot()).toEqual(["B"]);
  });
});
