/**
 * Shared timestamp formatters. Consolidates the half-dozen near-duplicate
 * `_formatDate*` helpers that previously lived inside individual components
 * (cts-log-list, cts-running-test-card, cts-plan-header, cts-plan-list,
 * cts-log-detail-header, cts-token-manager) into a single source of truth
 * consumed by the `<cts-time>` primitive.
 *
 * Every helper accepts an ISO 8601 string, a `Date`, or an epoch-ms number,
 * returns `""` on falsy / unparseable input, and never throws — callers can
 * feed raw payload fields straight in without guarding.
 *
 * Formatting uses the modern `Intl` APIs — `Intl.RelativeTimeFormat` for the
 * relative form and `Intl.DateTimeFormat` (via `toLocaleString` with
 * `dateStyle`/`timeStyle`) for the absolute and compact forms — both Baseline
 * widely available. `Temporal` is deliberately NOT used: as of 2026 it is
 * unsupported in Safari, and the project's no-polyfill policy rules out the
 * reference shim. Revisit once Temporal reaches Baseline across Chrome,
 * Safari, Firefox, and Edge. `Date` remains only as the parse/representation
 * primitive Temporal would otherwise replace.
 *
 * @module lib/time-format
 */

// Relative-time crossover: anything older than this falls back to the
// absolute locale string, matching the long-standing cts-log-list heuristic.
const RELATIVE_MAX_DAYS = 30;

// ECMAScript's maximum representable time value (±100,000,000 days from the
// epoch). Constructing a Date beyond this yields an Invalid Date whose
// toISOString() throws a RangeError — so any candidate past this bound is
// treated as unparseable to preserve the module's "never throws" contract.
const MAX_TIME_MS = 8.64e15;

// One RelativeTimeFormat instance, reused across every formatRelative call.
// The Intl constructor negotiates locale data and is far more expensive than
// .format(); a per-call construction would cost one constructor per row in a
// large log list.
const RELATIVE_TIME_FORMAT = new Intl.RelativeTimeFormat(undefined, { numeric: "auto" });

/**
 * Coerce an ISO string / Date / epoch-ms number to a millisecond timestamp.
 * Also accepts a bare epoch-ms *string* (e.g. `"1764500000000"`), because
 * `<cts-time value>` is a string attribute and several payloads (notably log
 * entries) carry epoch-ms timestamps that Lit stringifies on the way in.
 * Out-of-range and unparseable inputs return `null` — never a value that
 * would make a downstream `new Date(...).toISOString()` throw.
 *
 * @param {string | number | Date | null | undefined} value Input timestamp.
 * @returns {number | null} Epoch milliseconds within the valid Date range, or `null` when missing/unparseable/out-of-range.
 */
export function toMillis(value) {
  if (value === null || value === undefined || value === "") return null;
  let ms;
  if (value instanceof Date) {
    ms = value.getTime();
  } else if (typeof value === "number") {
    ms = value;
  } else {
    const trimmed = value.trim();
    // A 12+ digit all-digit string is an epoch-ms value (1e12 ms ≈ Sep 2001),
    // which Date.parse would otherwise reject. The 12-digit floor keeps short
    // numeric strings like "2026" flowing to Date.parse, where they correctly
    // read as a calendar year rather than 2 seconds past the epoch.
    ms = /^\d{12,}$/.test(trimmed) ? Number(trimmed) : Date.parse(value);
  }
  if (!Number.isFinite(ms) || Math.abs(ms) > MAX_TIME_MS) return null;
  return ms;
}

/**
 * Relative-time label ("now", "5 minutes ago", "3 hours ago", "2 days ago").
 * Falls back to {@link formatAbsolute} for timestamps older than
 * {@link RELATIVE_MAX_DAYS}. Future timestamps clamp to "now" to defend
 * against client-clock skew.
 *
 * @param {string | number | Date | null | undefined} value Input timestamp.
 * @returns {string} Localised relative label, absolute string beyond the
 *   crossover, or `""` when input is missing/unparseable.
 */
export function formatRelative(value) {
  const then = toMillis(value);
  if (then === null) return "";
  const deltaSec = Math.max(0, Math.round((Date.now() - then) / 1000));
  const rtf = RELATIVE_TIME_FORMAT;
  if (deltaSec < 60) return rtf.format(-deltaSec, "second");
  const deltaMin = Math.round(deltaSec / 60);
  if (deltaMin < 60) return rtf.format(-deltaMin, "minute");
  const deltaHour = Math.round(deltaMin / 60);
  if (deltaHour < 24) return rtf.format(-deltaHour, "hour");
  const deltaDay = Math.round(deltaHour / 24);
  if (deltaDay < RELATIVE_MAX_DAYS) return rtf.format(-deltaDay, "day");
  return formatAbsolute(then);
}

/**
 * Full absolute date + time in the user's locale
 * (e.g. `5/22/2026, 9:42:13 AM`). This is the canonical hover form surfaced
 * via the `title` attribute on `<cts-time>`.
 *
 * @param {string | number | Date | null | undefined} value Input timestamp.
 * @returns {string} Locale date/time string, or `""` when input is missing/unparseable.
 */
export function formatAbsolute(value) {
  const t = toMillis(value);
  if (t === null) return "";
  return new Date(t).toLocaleString();
}

/**
 * Time-of-day only (e.g. `9:42:13 AM`). Used in dense log rows that share an
 * obvious date context; the absolute form is still available on hover.
 *
 * @param {string | number | Date | null | undefined} value Input timestamp.
 * @returns {string} Locale time-of-day string, or `""` when input is missing/unparseable.
 */
export function formatTimeOfDay(value) {
  const t = toMillis(value);
  if (t === null) return "";
  return new Date(t).toLocaleTimeString();
}

/**
 * Compact date + time (e.g. `May 22, 2026, 9:42 AM`) via medium date / short
 * time styles. Used in dense headers and sticky bars where the full locale
 * string is too long but the date still matters; hover still reveals the full
 * absolute form.
 *
 * @param {string | number | Date | null | undefined} value Input timestamp.
 * @returns {string} Compact locale date/time string, or `""` when input is missing/unparseable.
 */
export function formatCompact(value) {
  const t = toMillis(value);
  if (t === null) return "";
  return new Date(t).toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}
