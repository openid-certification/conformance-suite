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
 * @module lib/time-format
 */

// Relative-time crossover: anything older than this falls back to the
// absolute locale string, matching the long-standing cts-log-list heuristic.
const RELATIVE_MAX_DAYS = 30;

/**
 * Coerce an ISO string / Date / epoch-ms number to a millisecond timestamp.
 * Also accepts a bare epoch-ms *string* (e.g. `"1764500000000"`), because
 * `<cts-time value>` is a string attribute and several payloads (notably log
 * entries) carry epoch-ms timestamps that Lit stringifies on the way in.
 *
 * @param {string | number | Date | null | undefined} value Input timestamp.
 * @returns {number | null} Epoch milliseconds, or `null` when missing/unparseable.
 */
function toMillis(value) {
  if (value === null || value === undefined || value === "") return null;
  if (value instanceof Date) {
    const t = value.getTime();
    return Number.isNaN(t) ? null : t;
  }
  if (typeof value === "number") {
    return Number.isFinite(value) ? value : null;
  }
  const trimmed = value.trim();
  // A 12+ digit all-digit string is an epoch-ms value (1e12 ms ≈ Sep 2001),
  // which Date.parse would otherwise reject. The 12-digit floor keeps short
  // numeric strings like "2026" flowing to Date.parse, where they correctly
  // read as a calendar year rather than 2 seconds past the epoch.
  if (/^\d{12,}$/.test(trimmed)) {
    const ms = Number(trimmed);
    return Number.isFinite(ms) ? ms : null;
  }
  const t = Date.parse(value);
  return Number.isNaN(t) ? null : t;
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
  const rtf = new Intl.RelativeTimeFormat(undefined, { numeric: "auto" });
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
 * Convenience pairing for the common "show relative, hover absolute" case:
 * a single call powers both the visible text and the `title` attribute
 * without re-parsing.
 *
 * @param {string | number | Date | null | undefined} value Input timestamp.
 * @returns {{ display: string, absolute: string }} `display` is the relative
 *   label, `absolute` is the full locale string. Both are `""` on bad input.
 */
export function formatAuto(value) {
  return { display: formatRelative(value), absolute: formatAbsolute(value) };
}

/**
 * ISO 8601 string for the native `<time datetime>` attribute. Returns the
 * canonical machine-readable form regardless of how the input was supplied.
 *
 * @param {string | number | Date | null | undefined} value Input timestamp.
 * @returns {string} ISO 8601 string, or `""` when input is missing/unparseable.
 */
export function toIso(value) {
  const t = toMillis(value);
  if (t === null) return "";
  return new Date(t).toISOString();
}
