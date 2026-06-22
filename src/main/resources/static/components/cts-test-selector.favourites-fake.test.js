import { describe, it, expect, beforeEach } from "vitest";
import { createFavouritesController } from "./cts-test-selector.favourites-fake.js";

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

describe("createFavouritesController", () => {
  /** @type {ReturnType<typeof fakeStorage>} */
  let storage;
  beforeEach(() => {
    storage = fakeStorage();
  });

  it("starts empty and mirrors the { plans } wire shape", async () => {
    const c = createFavouritesController({ storage });
    expect(c.snapshot()).toEqual([]);
    expect(await c.get()).toEqual({ plans: [] });
  });

  it("add persists and re-get returns the new set (insertion order)", async () => {
    const c = createFavouritesController({ storage });
    expect(await c.add("a")).toEqual({ plans: ["a"] });
    expect(await c.add("b")).toEqual({ plans: ["a", "b"] });
    // A fresh controller over the same storage sees the persisted set —
    // this is what makes "persist across reload" work in the stories.
    const reloaded = createFavouritesController({ storage });
    expect(await reloaded.get()).toEqual({ plans: ["a", "b"] });
  });

  it("add is idempotent", async () => {
    const c = createFavouritesController({ storage });
    await c.add("a");
    expect(await c.add("a")).toEqual({ plans: ["a"] });
  });

  it("remove reverses add", async () => {
    const c = createFavouritesController({ storage });
    await c.add("a");
    await c.add("b");
    expect(await c.remove("a")).toEqual({ plans: ["b"] });
    expect(c.snapshot()).toEqual(["b"]);
  });

  it("a failed add rejects and leaves storage unchanged (so the caller reverts)", async () => {
    const c = createFavouritesController({ storage, failOn: "boom" });
    await c.add("a");
    await expect(c.add("boom")).rejects.toThrow(/save failed/);
    expect(c.snapshot()).toEqual(["a"]);
  });

  it("a failed remove rejects and leaves storage unchanged", async () => {
    const c = createFavouritesController({ storage, failOn: (_name, op) => op === "remove" });
    await c.add("a");
    await expect(c.remove("a")).rejects.toThrow(/remove failed/);
    expect(c.snapshot()).toEqual(["a"]);
  });

  it("reset clears persisted state between stories", async () => {
    const c = createFavouritesController({ storage });
    await c.add("a");
    c.reset();
    expect(c.snapshot()).toEqual([]);
  });

  it("tolerates corrupt storage as empty", async () => {
    storage.setItem("cts:favourite-plans", "{not json");
    const c = createFavouritesController({ storage });
    expect(c.snapshot()).toEqual([]);
  });
});
