// Favorites persistence store for the cts-test-selector picker. The favorites
// LIST is account data, persisted server-side per logged-in principal via the
// `/api/favorite-plans` REST API (the component-internal filter preference is a
// separate localStorage concern — see cts-test-selector.js). This module is
// imported by the production page (schedule-test.html); the Storybook stories
// drive `attachFavorites` with an in-memory double from
// cts-test-selector.favorites-store.fake.js (real `fetch` can't run against a
// server in Storybook), so production ships no test scaffolding.
//
// Wire shape (all responses 200 with the full updated set; 401 when there is no
// authenticated principal — the API is auth-gated, mirroring /api/lastconfig):
//
//   GET    /api/favorite-plans            -> { plans: string[] }
//   POST   /api/favorite-plans { plan }   -> { plans: string[] }   (idempotent add)
//   DELETE /api/favorite-plans/{plan}     -> { plans: string[] }   (remove)
//
// Ordering is insertion order, most-recently-added LAST (a chronological list),
// matching how the picker renders the saved view.

const DEFAULT_BASE_URL = "/api/favorite-plans";

/**
 * The request init the controller builds — a structural subset of the DOM
 * `RequestInit` (named locally so the JSDoc linter doesn't need the DOM lib).
 * @typedef {{ method: string, headers: Record<string, string>, body?: string }} FavoritesRequestInit
 */

/**
 * The minimal `fetch` surface the controller actually uses. The DOM `Response`
 * satisfies the return shape (it has `ok`/`status`/`json`), and the page's
 * `fetch` accepts a {@link FavoritesRequestInit}, so neither the page nor a unit
 * test needs to fake a whole `Response`.
 * @typedef {(input: string, init: FavoritesRequestInit) => Promise<{
 *   ok: boolean, status: number, json: () => Promise<unknown>
 * }>} FavoritesFetch
 */

/**
 * The rejection value for a non-2xx response: an Error carrying the HTTP
 * status that produced it. `status` is absent when `fetch` itself rejected
 * (no response arrived), which callers must treat as retryable rather than as
 * a signed-out answer.
 * @typedef {Error & { status?: number }} FavoritesHttpError
 */

/**
 * Create a favorites controller backed by the `/api/favorite-plans` API. The
 * `fetch` implementation is injectable so a node unit test can drive it without
 * a real server.
 * @param {object} [options] - Controller configuration.
 * @param {string} [options.baseUrl] - API base path (default `/api/favorite-plans`).
 * @param {FavoritesFetch} [options.fetchImpl] - Fetch implementation (default the
 *   page's `fetch`).
 * @returns {{
 *   get: () => Promise<{ plans: string[] }>,
 *   add: (name: string) => Promise<{ plans: string[] }>,
 *   remove: (name: string) => Promise<{ plans: string[] }>,
 * }} A controller over the `/api/favorite-plans` surface.
 */
export function createFavoritesController({ baseUrl = DEFAULT_BASE_URL, fetchImpl } = {}) {
  const doFetch = fetchImpl ?? ((input, init) => fetch(input, init));

  /**
   * Issue one request and normalize the `{ plans }` response.
   * @param {string} method - HTTP method.
   * @param {string} path - Path appended to `baseUrl`.
   * @param {object} [body] - Optional JSON body.
   * @returns {Promise<{ plans: string[] }>} The updated favorites set.
   */
  async function request(method, path, body) {
    /** @type {FavoritesRequestInit} */
    const init = { method, headers: { Accept: "application/json" } };
    if (body !== undefined) {
      init.headers["Content-Type"] = "application/json";
      init.body = JSON.stringify(body);
    }
    const res = await doFetch(`${baseUrl}${path}`, init);
    // Any non-2xx rejects so the caller can fall back (on the seed) or revert +
    // toast (on a toggle). The status rides along on the error because the two
    // cases are not the same: 401/403 means "no principal — favorites are not
    // available to you", while a 5xx is transient and the caller should leave
    // the stars enabled so a retry is one click away. A rejected fetch (network
    // down, request aborted) never reaches here and so carries no status, which
    // the caller reads as retryable.
    if (!res.ok) {
      const error = /** @type {FavoritesHttpError} */ (
        new Error(`favorites: ${method} ${baseUrl}${path} -> ${res.status}`)
      );
      error.status = res.status;
      throw error;
    }
    const data = /** @type {{ plans?: string[] }} */ (await res.json());
    return { plans: Array.isArray(data.plans) ? data.plans : [] };
  }

  return {
    /**
     * GET /api/favorite-plans.
     * @returns {Promise<{ plans: string[] }>} The current favorites.
     */
    get() {
      return request("GET", "", undefined);
    },

    /**
     * POST /api/favorite-plans { plan } — idempotent add.
     * @param {string} name - The planName to add.
     * @returns {Promise<{ plans: string[] }>} The updated favorites.
     */
    add(name) {
      return request("POST", "", { plan: name });
    },

    /**
     * DELETE /api/favorite-plans/{plan}.
     * @param {string} name - The planName to remove.
     * @returns {Promise<{ plans: string[] }>} The updated favorites.
     */
    remove(name) {
      return request("DELETE", `/${encodeURIComponent(name)}`, undefined);
    },
  };
}

/**
 * Wire a `cts-test-selector` to a controller: optimistically update the
 * `favorites` prop on every `cts-favorite-toggle`, reconcile with the server
 * truth on success, and revert + raise an error toast on failure. The same
 * function drives both production (a `fetch` controller) and the stories (an
 * in-memory fake), so the optimistic logic is single-sourced.
 * @param {HTMLElement & { favorites: string[] }} host - The cts-test-selector.
 * @param {{
 *   add: (name: string) => Promise<{ plans: string[] }>,
 *   remove: (name: string) => Promise<{ plans: string[] }>,
 * }} controller - The persistence controller to drive.
 * @param {object} [options] - Optional hooks.
 * @param {(plan: string, favorite: boolean, ok: boolean) => void} [options.onSettled] - Called
 *   once per write when it finishes, with the outcome. Writes are the only thing that can make
 *   a caller's other in-flight request (notably the page's initial seed GET) stale, and a
 *   *failed* write makes nothing stale — it leaves `favorites` back at whatever baseline the
 *   optimistic edit started from, which on a fresh page load is a placeholder rather than the
 *   truth. A caller reconciling several responses needs both facts, and neither is observable
 *   from the toggle event alone.
 * @returns {() => void} A detach function that removes the listener.
 */
export function attachFavorites(host, controller, { onSettled } = {}) {
  const settled = (/** @type {string} */ plan, /** @type {boolean} */ favorite, ok) => {
    if (typeof onSettled === "function") onSettled(plan, favorite, ok);
  };
  const handler = async (/** @type {Event} */ e) => {
    const { plan, favorite } = /** @type {CustomEvent} */ (e).detail;
    // Optimistic: append on add (insertion order), filter out on remove.
    host.favorites = favorite
      ? [...host.favorites.filter((n) => n !== plan), plan]
      : host.favorites.filter((n) => n !== plan);
    try {
      const { plans } = favorite ? await controller.add(plan) : await controller.remove(plan);
      host.favorites = plans;
      settled(plan, favorite, true);
    } catch (err) {
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
          ? /** @type {{ ctsToast?: (opts: { title: string, message?: string, kind: string }) => void }} */ (
              /** @type {unknown} */ (window)
            ).ctsToast
          : undefined;
      if (typeof ctsToast === "function") {
        ctsToast({
          title: favorite ? "Couldn’t save favorite" : "Couldn’t remove favorite",
          message: "Please try again.",
          kind: "error",
        });
      }
      settled(plan, favorite, false);
    }
  };
  host.addEventListener("cts-favorite-toggle", handler);
  return () => host.removeEventListener("cts-favorite-toggle", handler);
}
