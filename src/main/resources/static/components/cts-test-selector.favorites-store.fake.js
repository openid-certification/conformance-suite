// Story/test-only in-memory double for the favorites controller. Production
// favorites persist server-side via `/api/favorite-plans` (see
// cts-test-selector.favorites-store.js) — real `fetch` against a server can't
// run in Storybook, so the stories drive the shared `attachFavorites` logic
// with this stateful in-memory controller instead. It exposes the same
// get/add/remove surface the production controller does, plus `snapshot()` for
// assertions and injectable `latency`/`failOn` so the slow / failed-write
// stories can exercise the optimistic-revert path. NOT imported by any page.

/**
 * Resolve after `ms` (0 → a resolved promise) so a story can observe the
 * optimistic state before the persisted truth lands.
 * @param {number} ms - Delay in milliseconds.
 * @returns {Promise<void>} A promise that settles after the delay.
 */
function delay(ms) {
  return ms > 0 ? new Promise((resolve) => setTimeout(resolve, ms)) : Promise.resolve();
}

/**
 * Normalize the `failOn` option into a predicate.
 * @param {string|string[]|((name: string, op: string) => boolean)|null} failOn - A
 *   planName, a list of planNames, or a `(name, op) => boolean` predicate where
 *   `op` is `"add"` or `"remove"`.
 * @returns {(name: string, op: string) => boolean} A predicate that returns
 *   true when the operation should reject.
 */
function normalizeFailOn(failOn) {
  if (!failOn) return () => false;
  if (typeof failOn === "function") return failOn;
  const set = new Set(Array.isArray(failOn) ? failOn : [failOn]);
  return (name) => set.has(name);
}

/**
 * Create an in-memory favorites controller mirroring the production
 * `/api/favorite-plans` surface. State lives in a closure, so it survives a
 * component re-mount within a single story (the "persist across reload" path).
 * @param {object} [options] - Controller configuration.
 * @param {string[]} [options.initial] - Seed favorites (insertion order).
 * @param {number} [options.latency] - Milliseconds delay applied to every op.
 * @param {string|string[]|((name: string, op: string) => boolean)|null} [options.failOn] -
 *   Which operations reject — see {@link normalizeFailOn}.
 * @returns {{
 *   snapshot: () => string[],
 *   get: () => Promise<{ plans: string[] }>,
 *   add: (name: string) => Promise<{ plans: string[] }>,
 *   remove: (name: string) => Promise<{ plans: string[] }>,
 * }} An in-memory controller.
 */
export function createFakeFavoritesController({ initial = [], latency = 0, failOn = null } = {}) {
  let plans = [...initial];
  const shouldFail = normalizeFailOn(failOn);
  return {
    /**
     * Synchronous current set — for assertions that the server (here, memory)
     * matches the rendered favorites.
     * @returns {string[]} The current planNames.
     */
    snapshot() {
      return [...plans];
    },

    /**
     * GET /api/favorite-plans.
     * @returns {Promise<{ plans: string[] }>} The current favorites.
     */
    async get() {
      await delay(latency);
      return { plans: [...plans] };
    },

    /**
     * POST /api/favorite-plans { plan } — idempotent add.
     * @param {string} name - The planName to add.
     * @returns {Promise<{ plans: string[] }>} The updated favorites.
     */
    async add(name) {
      await delay(latency);
      if (shouldFail(name, "add")) throw new Error(`favorites: save failed for ${name}`);
      if (!plans.includes(name)) plans = [...plans, name];
      return { plans: [...plans] };
    },

    /**
     * DELETE /api/favorite-plans/{plan}.
     * @param {string} name - The planName to remove.
     * @returns {Promise<{ plans: string[] }>} The updated favorites.
     */
    async remove(name) {
      await delay(latency);
      if (shouldFail(name, "remove")) throw new Error(`favorites: remove failed for ${name}`);
      plans = plans.filter((n) => n !== name);
      return { plans: [...plans] };
    },
  };
}
