/**
 * Shared partner-theme client (THEMING SPIKE).
 *
 * Components that brand themselves (cts-navbar, cts-login-page, cts-footer) and
 * pages that surface presets (plans.html, schedule-test.html) import this module
 * instead of fetching `/api/theme` independently — one in-flight request per page,
 * one sessionStorage seed for flash-free first paint across navigations (the same
 * contract cts-navbar uses for `/api/currentuser` via `cts-navbar:user`).
 *
 * The resolved value is `null` when no partner theme is active, otherwise:
 * `{ source: "file"|"database", partner: {name}, brand: {accent, logo?}, presets?: [...] }`
 * where `brand.logo.url` is the stable `/api/theme/logo` binary endpoint (the
 * backend strips embedded logo data out of the JSON).
 *
 * Note the accent COLOR itself never flows through this module — pages load it as
 * a render-blocking `<link href="/api/theme/css">` in their head, so the accent is
 * correct on first paint without JS.
 */

const CACHE_KEY = "cts-theme";

/** @type {Promise<object|null>|null} */
let themePromise = null;

/**
 * Synchronous read of the last-seen theme for flash-free first paint. May be
 * stale; callers should still await {@link getTheme} for the authoritative value.
 * @returns {object|null} The cached theme, or null.
 */
export function getCachedTheme() {
  try {
    const raw = sessionStorage.getItem(CACHE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

/**
 * Fetch the active partner theme, memoized per page load.
 * Fail-soft: on network/HTTP failure resolves to the cached value (or null) —
 * branding must never take a page down.
 * @returns {Promise<object|null>} The active theme, or null when unthemed.
 */
export function getTheme() {
  if (!themePromise) {
    themePromise = fetch("/api/theme")
      .then((response) => (response.ok ? response.json() : null))
      .then((body) => {
        const theme =
          body && body.source && body.source !== "none" && body.theme
            ? { source: body.source, ...body.theme }
            : null;
        try {
          if (theme) {
            sessionStorage.setItem(CACHE_KEY, JSON.stringify(theme));
          } else {
            sessionStorage.removeItem(CACHE_KEY);
          }
        } catch {
          // storage unavailable (private browsing etc.) — caching is best-effort
        }
        return theme;
      })
      .catch(() => getCachedTheme());
  }
  return themePromise;
}
