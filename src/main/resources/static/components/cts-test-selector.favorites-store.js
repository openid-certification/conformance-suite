// Favorites persistence store for the cts-test-selector picker. localStorage
// IS the interim backend (the deferred `/api/favorite-plans` Java store is a
// later follow-up), so this module is imported by BOTH the production page
// (schedule-test.html) and the Storybook stories — single-sourcing the exact
// get/add/remove surface that the future backend swaps `fetch()` into.
//
// The request/response wire shape is mirrored deliberately, so swapping the
// localStorage reads/writes for `fetch()` behind get/add/remove is all the
// production wiring needs — the component never changes:
//
//   GET    /api/favorite-plans          -> 200 { plans: string[] }
//   POST   /api/favorite-plans { plan }  -> 200 { plans: string[] }   (added)
//   DELETE /api/favorite-plans/{plan}    -> 200 { plans: string[] }   (removed)
//
// Ordering is insertion order, most-recently-added LAST (a plain append),
// matching how a chronological server-side list would grow.
//
// `latency` / `failOn` are optional test scaffolding the stories inject to
// demo slow / failed writes (default off in production, where they stay
// inert). They are distinct from real browser storage errors (quota,
// private-mode `setItem` throws), which add/remove always surface as a
// rejected promise so the page can toast.

const DEFAULT_KEY = "cts:favorite-plans";

/**
 * Minimal Storage shape the controller actually uses — narrower than the DOM
 * `Storage` interface so a Map-backed fake (node unit test) satisfies it.
 * @typedef {object} StorageLike
 * @property {(key: string) => string | null} getItem - Read a value.
 * @property {(key: string, value: string) => void} setItem - Write a value.
 * @property {(key: string) => void} removeItem - Delete a value.
 */

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
 * Create a favorites controller backed by `storage` (defaults to the page's
 * `localStorage`). Storage is injectable so a node unit test can pass a
 * Map-backed fake without a DOM.
 * @param {object} [options] - Controller configuration.
 * @param {string} [options.key] - Storage key (namespaced by default).
 * @param {number} [options.latency] - Milliseconds delay applied to every op.
 * @param {string|string[]|((name: string, op: string) => boolean)|null} [options.failOn] -
 *   Which operations reject — see {@link normalizeFailOn}.
 * @param {StorageLike} [options.storage] - Storage backend (default `localStorage`).
 * @returns {{
 *   snapshot: () => string[],
 *   reset: () => void,
 *   get: () => Promise<{ plans: string[] }>,
 *   add: (name: string) => Promise<{ plans: string[] }>,
 *   remove: (name: string) => Promise<{ plans: string[] }>,
 * }} A controller mirroring the `/api/favorite-plans` surface.
 */
export function createFavoritesController({
  key = DEFAULT_KEY,
  latency = 0,
  failOn = null,
  storage = globalThis.localStorage,
} = {}) {
  const shouldFail = normalizeFailOn(failOn);

  /**
   * Read the persisted favorites, tolerating corrupt/unset storage as empty.
   * @returns {string[]} The persisted planNames.
   */
  function read() {
    try {
      const raw = storage.getItem(key);
      const parsed = raw ? JSON.parse(raw) : null;
      return Array.isArray(parsed?.plans) ? parsed.plans : [];
    } catch {
      return [];
    }
  }

  /**
   * Persist the favorites set.
   * @param {string[]} plans - The planNames to store.
   * @returns {void}
   */
  function write(plans) {
    storage.setItem(key, JSON.stringify({ plans }));
  }

  return {
    /**
     * Synchronous current set — for seeding a story's initial render and for
     * assertions that storage matches the rendered favorites.
     * @returns {string[]} The persisted planNames.
     */
    snapshot() {
      return read();
    },

    /**
     * Clear persisted state. The meta-level `beforeEach` calls this so
     * favorites never leak between stories.
     * @returns {void}
     */
    reset() {
      storage.removeItem(key);
    },

    /**
     * GET /api/favorite-plans.
     * @returns {Promise<{ plans: string[] }>} The current favorites.
     */
    async get() {
      await delay(latency);
      return { plans: read() };
    },

    /**
     * POST /api/favorite-plans { plan } — append, idempotent.
     * @param {string} name - The planName to add.
     * @returns {Promise<{ plans: string[] }>} The updated favorites.
     */
    async add(name) {
      await delay(latency);
      if (shouldFail(name, "add")) throw new Error(`favorites: save failed for ${name}`);
      const plans = read();
      if (!plans.includes(name)) plans.push(name);
      write(plans);
      return { plans };
    },

    /**
     * DELETE /api/favorite-plans/{plan}.
     * @param {string} name - The planName to remove.
     * @returns {Promise<{ plans: string[] }>} The updated favorites.
     */
    async remove(name) {
      await delay(latency);
      if (shouldFail(name, "remove")) throw new Error(`favorites: remove failed for ${name}`);
      const plans = read().filter((n) => n !== name);
      write(plans);
      return { plans };
    },
  };
}

/**
 * Wire a `cts-test-selector` to a controller exactly the way schedule-test.html
 * wires it (and the way the future `/api/favorite-plans` swap will): optimistically
 * update the `favorites` prop on every `cts-favorite-toggle`, reconcile with the
 * persisted truth on success, and revert + raise an error toast on failure.
 * @param {HTMLElement & { favorites: string[] }} host - The cts-test-selector.
 * @param {ReturnType<typeof createFavoritesController>} controller - The
 *   persistence controller to drive.
 * @returns {() => void} A detach function that removes the listener.
 */
export function attachFavorites(host, controller) {
  const handler = async (/** @type {Event} */ e) => {
    const { plan, favorite } = /** @type {CustomEvent} */ (e).detail;
    // Optimistic: append on add (insertion order), filter out on remove.
    host.favorites = favorite
      ? [...host.favorites.filter((n) => n !== plan), plan]
      : host.favorites.filter((n) => n !== plan);
    try {
      const { plans } = favorite ? await controller.add(plan) : await controller.remove(plan);
      host.favorites = plans;
    } catch {
      // Revert ONLY this plan's optimistic change, computed against the
      // *current* favorites (not a snapshot captured before the await): an
      // add-failure removes the plan, a remove-failure re-adds it. This keeps
      // a concurrent toggle of another plan that resolved during the await
      // intact, so one plan's failure never mutates another's state (KTD4: no
      // undo affordance — re-starring is the undo path).
      host.favorites = favorite
        ? host.favorites.filter((n) => n !== plan)
        : [...host.favorites.filter((n) => n !== plan), plan];
      const ctsToast =
        typeof window !== "undefined"
          ? /** @type {{ ctsToast?: Function }} */ (/** @type {unknown} */ (window)).ctsToast
          : undefined;
      if (typeof ctsToast === "function") {
        ctsToast({
          title: favorite ? "Couldn’t save favorite" : "Couldn’t remove favorite",
          message: "Please try again.",
          kind: "error",
        });
      }
    }
  };
  host.addEventListener("cts-favorite-toggle", /** @type {EventListener} */ (handler));
  return () =>
    host.removeEventListener("cts-favorite-toggle", /** @type {EventListener} */ (handler));
}
