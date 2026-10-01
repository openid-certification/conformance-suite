/**
 * Memoised loader for the OIDF spec-link prefix map served by
 * `GET /api/ui/spec_links?public=true`. The map is a flat object of
 * `"<PREFIX>-"` → URL-prefix entries — a requirement string like
 * `OIDCC-3.1.3.7-6` resolves to `https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.3.7`
 * by appending the section part of the suffix after the matched prefix.
 *
 * One fetch per page (KTD4 in
 * `docs/plans/2026-05-22-002-fix-mr1998-maintainer-feedback-plan.md`):
 * the in-flight Promise is cached at module scope so every cts-log-entry
 * on the page shares a single network round-trip.
 *
 * Failures degrade silently to an empty map — chips then render as
 * static text instead of broken links.
 *
 * @module lib/spec-links
 */

/** @type {Promise<Record<string, string>> | null} */
let pending = null;

/**
 * Fetch the spec-link prefix map, memoising the in-flight Promise so a
 * second caller reuses the first call.
 *
 * @returns {Promise<Record<string, string>>} Map of prefix → URL prefix. Empty on error.
 */
export function loadSpecLinks() {
  if (!pending) {
    pending = fetch("api/ui/spec_links?public=true", {
      credentials: "same-origin",
    })
      .then((r) => (r.ok ? r.json() : {}))
      .catch(() => ({}));
  }
  return pending;
}

/**
 * Resolve a requirement string (e.g. `OIDCC-3.1.3.7-6`) to a full URL
 * via longest-prefix match against the spec-link map. Longest-prefix is
 * required because the map contains overlapping prefixes like `OIDCC-`
 * and (hypothetically) `OIDCC-A-` — the longer one wins.
 *
 * A trailing numeric item part (`-6`, `-2.5`) identifies a list item or
 * paragraph within the section for the reader. The published specs do not
 * use that number as the item's fragment (xml2rfc fragments count
 * paragraphs, so `#section-5.3.2.2-5` is the fifth paragraph, not list
 * item 5), so the link targets the section and the chip text keeps the
 * full requirement. Suffixes that are not `<section>-<numeric item>`
 * (named fragments such as `request-object`) are appended unchanged.
 *
 * @param {string} ref Requirement string from `entry.requirements[i]`.
 * @param {Record<string, string> | null | undefined} map Spec-link map.
 * @returns {string | null} Full URL, or `null` when no prefix matches.
 */
export function resolveSpecLink(ref, map) {
  if (!ref || !map) return null;
  let bestPrefix = "";
  for (const prefix of Object.keys(map)) {
    if (ref.startsWith(prefix) && prefix.length > bestPrefix.length) {
      bestPrefix = prefix;
    }
  }
  if (!bestPrefix) return null;
  const suffix = ref.slice(bestPrefix.length);
  const itemised = /^([^-]+)-[0-9][0-9.-]*$/.exec(suffix);
  return map[bestPrefix] + (itemised ? itemised[1] : suffix);
}

/**
 * Test-only helper: seed the cache with a synchronous map so stories and
 * unit tests can bypass the real fetch.
 *
 * @param {Record<string, string>} map Map to seed.
 * @returns {void}
 */
export function __seedSpecLinks(map) {
  pending = Promise.resolve(map);
}

/**
 * Test-only helper: clear the cache so the next `loadSpecLinks()` call
 * re-fetches. Call from a `beforeEach` to prevent cross-test leakage.
 *
 * @returns {void}
 */
export function __resetSpecLinks() {
  pending = null;
}
