/**
 * Single source of truth for "which log entries are findings?" — the rows the
 * `cts-failure-summary` surfaces (in the log-detail hero, below the sticky bar
 * on mobile/tablet, and in the wide-viewport `cts-log-toc` rail).
 *
 * Previously this predicate existed as two byte-identical copies —
 * `cts-log-detail-header._getFailures()` and `js/log-detail.js selectFailures()`
 * — and both omitted `REVIEW`, so a test whose only findings need human review
 * produced an empty list and the hero fell through to its "nothing to show"
 * placeholder (GitLab #1866). `REVIEW` belongs here: the header already buckets
 * it in `_countFailureSeverities` and can render an "N need review" headline
 * for it, and `AbstractTestModule.fireTestReviewNeeded` makes it a first-class
 * verdict alongside FAILED / WARNING.
 */

/**
 * Per-condition `result` values that count as a finding. `SUCCESS` and `INFO`
 * are the two remaining values the backend emits, and neither is actionable.
 * @type {ReadonlySet<string>}
 */
const FINDING_RESULTS = new Set(["FAILURE", "WARNING", "REVIEW", "SKIPPED", "INTERRUPTED"]);

/**
 * Filter log entries down to the findings shown in a failure summary.
 *
 * Deliberately defensive about its input: callers hand it `testInfo.results`
 * (a field `/api/info` never serializes, so `undefined` is the normal case) as
 * well as real `/api/log` entries. Anything that is not an array — and any
 * null hole or entry without a recognised `result` — yields no findings rather
 * than throwing.
 * @param {any} entries - Log entries in stream order, or a non-array.
 * @returns {Array<any>} The finding entries, order preserved.
 */
export function selectFindings(entries) {
  if (!Array.isArray(entries)) return [];
  return entries.filter((entry) => !!entry && FINDING_RESULTS.has(entry.result || ""));
}
