// Story-only fake persistence adapter for the cts-test-selector favourites
// prototype. It is NOT imported by any page — it stands in for the deferred
// `/api/favourite-plans` backend so the Storybook stories can exercise the
// full optimistic lifecycle (persist across reload, injectable latency, and
// failure → revert) without a server.
//
// The request/response wire shape is mirrored deliberately, so swapping the
// localStorage reads/writes for `fetch()` behind get/add/remove is all the
// production wiring (schedule-test.html) needs — the component never changes:
//
//   GET    /api/favourite-plans          -> 200 { plans: string[] }
//   POST   /api/favourite-plans { plan }  -> 200 { plans: string[] }   (added)
//   DELETE /api/favourite-plans/{plan}    -> 200 { plans: string[] }   (removed)
//
// Ordering is insertion order, most-recently-added LAST (a plain append),
// matching how a chronological server-side list would grow.

const DEFAULT_KEY = "cts:favourite-plans";

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
 * Normalise the `failOn` option into a predicate.
 * @param {string|string[]|((name: string, op: string) => boolean)|null} failOn - A
 *   planName, a list of planNames, or a `(name, op) => boolean` predicate where
 *   `op` is `"add"` or `"remove"`.
 * @returns {(name: string, op: string) => boolean} A predicate that returns
 *   true when the operation should reject.
 */
function normaliseFailOn(failOn) {
  if (!failOn) return () => false;
  if (typeof failOn === "function") return failOn;
  const set = new Set(Array.isArray(failOn) ? failOn : [failOn]);
  return (name) => set.has(name);
}

/**
 * Create a fake favourites controller backed by `storage` (defaults to the
 * page's `localStorage`). Storage is injectable so a node unit test can pass a
 * Map-backed fake without a DOM.
 * @param {object} [options] - Controller configuration.
 * @param {string} [options.key] - Storage key (namespaced by default).
 * @param {number} [options.latency] - Milliseconds delay applied to every op.
 * @param {string|string[]|((name: string, op: string) => boolean)|null} [options.failOn] -
 *   Which operations reject — see {@link normaliseFailOn}.
 * @param {Storage} [options.storage] - Storage backend (default `localStorage`).
 * @returns {{
 *   snapshot: () => string[],
 *   reset: () => void,
 *   get: () => Promise<{ plans: string[] }>,
 *   add: (name: string) => Promise<{ plans: string[] }>,
 *   remove: (name: string) => Promise<{ plans: string[] }>,
 * }} A controller mirroring the `/api/favourite-plans` surface.
 */
export function createFavouritesController({
  key = DEFAULT_KEY,
  latency = 0,
  failOn = null,
  storage = globalThis.localStorage,
} = {}) {
  const shouldFail = normaliseFailOn(failOn);

  /**
   * Read the persisted favourites, tolerating corrupt/unset storage as empty.
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
   * Persist the favourites set.
   * @param {string[]} plans - The planNames to store.
   * @returns {void}
   */
  function write(plans) {
    storage.setItem(key, JSON.stringify({ plans }));
  }

  return {
    /**
     * Synchronous current set — for seeding a story's initial render and for
     * assertions that storage matches the rendered favourites.
     * @returns {string[]} The persisted planNames.
     */
    snapshot() {
      return read();
    },

    /**
     * Clear persisted state. The meta-level `beforeEach` calls this so
     * favourites never leak between stories.
     * @returns {void}
     */
    reset() {
      storage.removeItem(key);
    },

    /**
     * GET /api/favourite-plans.
     * @returns {Promise<{ plans: string[] }>} The current favourites.
     */
    async get() {
      await delay(latency);
      return { plans: read() };
    },

    /**
     * POST /api/favourite-plans { plan } — append, idempotent.
     * @param {string} name - The planName to add.
     * @returns {Promise<{ plans: string[] }>} The updated favourites.
     */
    async add(name) {
      await delay(latency);
      if (shouldFail(name, "add")) throw new Error(`favourites: save failed for ${name}`);
      const plans = read();
      if (!plans.includes(name)) plans.push(name);
      write(plans);
      return { plans };
    },

    /**
     * DELETE /api/favourite-plans/{plan}.
     * @param {string} name - The planName to remove.
     * @returns {Promise<{ plans: string[] }>} The updated favourites.
     */
    async remove(name) {
      await delay(latency);
      if (shouldFail(name, "remove")) throw new Error(`favourites: remove failed for ${name}`);
      const plans = read().filter((n) => n !== name);
      write(plans);
      return { plans };
    },
  };
}

/**
 * Wire a `cts-test-selector` to a controller exactly the way schedule-test.html
 * will wire it to `/api/favourite-plans`: optimistically update the `favourites`
 * prop on every `cts-favourite-toggle`, reconcile with the persisted truth on
 * success, and revert + raise an error toast on failure.
 * @param {HTMLElement & { favourites: string[] }} host - The cts-test-selector.
 * @param {ReturnType<typeof createFavouritesController>} controller - The fake
 *   persistence controller to drive.
 * @returns {() => void} A detach function that removes the listener.
 */
export function attachFavourites(host, controller) {
  /**
   * @param {CustomEvent} e - The cts-favourite-toggle event.
   * @returns {Promise<void>} Resolves once the persist round-trip settles.
   */
  const handler = async (e) => {
    const { plan, favourite } = e.detail;
    const before = host.favourites;
    // Optimistic: append on add (insertion order), filter out on remove.
    host.favourites = favourite
      ? [...before.filter((n) => n !== plan), plan]
      : before.filter((n) => n !== plan);
    try {
      const { plans } = favourite ? await controller.add(plan) : await controller.remove(plan);
      host.favourites = plans;
    } catch {
      // Revert the optimistic change and surface a plain error toast (KTD4:
      // no undo affordance — re-starring is the undo path).
      host.favourites = before;
      if (typeof window !== "undefined" && typeof window.ctsToast === "function") {
        window.ctsToast({
          title: favourite ? "Couldn’t save favourite" : "Couldn’t remove favourite",
          message: "Please try again.",
          kind: "error",
        });
      }
    }
  };
  host.addEventListener("cts-favourite-toggle", handler);
  return () => host.removeEventListener("cts-favourite-toggle", handler);
}
