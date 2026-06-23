import { describe, it, expect } from "vitest";
import { createFavoritesController, attachFavorites } from "./cts-test-selector.favorites-store.js";
import { createFakeFavoritesController } from "./cts-test-selector.favorites-store.fake.js";

/**
 * @typedef {{ detail: { plan: string, favorite: boolean } }} ToggleEvent - The
 *   shape attachFavorites destructures off a cts-favorite-toggle event.
 * @typedef {import("./cts-test-selector.favorites-store.js").FavoritesFetch} FavoritesFetch
 * @typedef {import("./cts-test-selector.favorites-store.js").FavoritesRequestInit} FavoritesRequestInit
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

/**
 * Build a fetch stub (a {@link FavoritesFetch}) that records calls and returns a
 * `{ plans }` JSON body.
 * @param {object} opts - Stub configuration.
 * @param {string[]} [opts.plans] - The `plans` array to return.
 * @param {boolean} [opts.ok] - Whether the response is 2xx.
 * @param {number} [opts.status] - The HTTP status.
 * @param {object} [opts.body] - Override the JSON body entirely.
 * @returns {{
 *   fetchImpl: FavoritesFetch,
 *   calls: Array<{ url: string, init: FavoritesRequestInit }>,
 * }} The stub.
 */
function fetchStub({ plans = [], ok = true, status = 200, body } = {}) {
  /** @type {Array<{ url: string, init: FavoritesRequestInit }>} */
  const calls = [];
  /** @type {FavoritesFetch} */
  const fetchImpl = (input, init) => {
    calls.push({ url: input, init });
    return Promise.resolve({ ok, status, json: () => Promise.resolve(body ?? { plans }) });
  };
  return { fetchImpl, calls };
}

describe("createFavoritesController (fetch)", () => {
  it("GET issues GET /api/favorite-plans and returns { plans }", async () => {
    const { fetchImpl, calls } = fetchStub({ plans: ["a"] });
    const c = createFavoritesController({ fetchImpl });
    expect(await c.get()).toEqual({ plans: ["a"] });
    expect(calls[0]?.url).toBe("/api/favorite-plans");
    expect(calls[0]?.init.method).toBe("GET");
  });

  it("POST sends { plan } as JSON and returns the updated set", async () => {
    const { fetchImpl, calls } = fetchStub({ plans: ["a", "b"] });
    const c = createFavoritesController({ fetchImpl });
    expect(await c.add("b")).toEqual({ plans: ["a", "b"] });
    expect(calls[0]?.url).toBe("/api/favorite-plans");
    expect(calls[0]?.init.method).toBe("POST");
    expect(calls[0]?.init.headers["Content-Type"]).toBe("application/json");
    expect(JSON.parse(/** @type {string} */ (calls[0]?.init.body))).toEqual({ plan: "b" });
  });

  it("DELETE targets the URL-encoded plan path", async () => {
    const { fetchImpl, calls } = fetchStub({ plans: [] });
    const c = createFavoritesController({ fetchImpl });
    await c.remove("plan a/b");
    expect(calls[0]?.url).toBe("/api/favorite-plans/plan%20a%2Fb");
    expect(calls[0]?.init.method).toBe("DELETE");
  });

  it("rejects on a non-2xx (e.g. 401) so the page can disable favorites", async () => {
    const { fetchImpl } = fetchStub({ ok: false, status: 401, body: {} });
    const c = createFavoritesController({ fetchImpl });
    await expect(c.get()).rejects.toThrow(/401/);
  });

  it("tolerates a response missing `plans` as an empty set", async () => {
    const { fetchImpl } = fetchStub({ body: {} });
    const c = createFavoritesController({ fetchImpl });
    expect(await c.get()).toEqual({ plans: [] });
  });
});

describe("createFakeFavoritesController (in-memory story double)", () => {
  it("starts from `initial` and mirrors the { plans } wire shape", async () => {
    const c = createFakeFavoritesController({ initial: ["a"] });
    expect(c.snapshot()).toEqual(["a"]);
    expect(await c.get()).toEqual({ plans: ["a"] });
  });

  it("add persists, is idempotent, and appends in insertion order", async () => {
    const c = createFakeFavoritesController();
    expect(await c.add("a")).toEqual({ plans: ["a"] });
    expect(await c.add("b")).toEqual({ plans: ["a", "b"] });
    expect(await c.add("a")).toEqual({ plans: ["a", "b"] });
  });

  it("remove reverses add", async () => {
    const c = createFakeFavoritesController({ initial: ["a", "b"] });
    expect(await c.remove("a")).toEqual({ plans: ["b"] });
    expect(c.snapshot()).toEqual(["b"]);
  });

  it("failOn rejects and leaves state unchanged (so the caller reverts)", async () => {
    const c = createFakeFavoritesController({ initial: ["a"], failOn: "boom" });
    await expect(c.add("boom")).rejects.toThrow(/save failed/);
    expect(c.snapshot()).toEqual(["a"]);
    const r = createFakeFavoritesController({
      initial: ["a"],
      failOn: (_n, op) => op === "remove",
    });
    await expect(r.remove("a")).rejects.toThrow(/remove failed/);
    expect(r.snapshot()).toEqual(["a"]);
  });
});

describe("attachFavorites", () => {
  it("optimistically adds then reconciles with the persisted truth", async () => {
    const c = createFakeFavoritesController();
    const host = fakeHost();
    attachFavorites(
      /** @type {HTMLElement & { favorites: string[] }} */ (/** @type {unknown} */ (host)),
      c,
    );
    host.emit("cts-favorite-toggle", { plan: "a", favorite: true });
    expect(host.favorites).toEqual(["a"]); // optimistic, before persist resolves
    await new Promise((r) => setTimeout(r, 0));
    expect(host.favorites).toEqual(["a"]); // reconciled
    expect(c.snapshot()).toEqual(["a"]);
  });

  it("a failed toggle reverts only that plan, preserving a concurrent toggle", async () => {
    // Star A (will fail) then B (succeeds) before A's persist resolves. A
    // stale-snapshot revert would wipe B; the per-plan revert leaves B starred.
    const c = createFakeFavoritesController({ latency: 5, failOn: "A" });
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
