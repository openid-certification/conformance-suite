/**
 * Shared run-classification helper.
 *
 * Single source of truth for how the suite decides whether a test-log run is
 * "in progress" or "failing", lifted verbatim from the retired dashboard's
 * `cts-dashboard._fetchStats` so the runs strip (cts-run-status-strip) and the
 * rest of the suite agree on those two signals.
 *
 * The classification is intentionally a *whitelist* of actively-executing
 * statuses, NOT the negation of FINISHED:
 *
 * - In progress = status is RUNNING or WAITING. The negation-against-FINISHED
 *   shape would also count INTERRUPTED (terminal: stopped before completion)
 *   and the pre-execution NOT_YET_CREATED/CREATED/CONFIGURED states from
 *   TestModule.Status, inflating the "in progress" count. See
 *   src/main/java/net/openid/conformance/testmodule/TestModule.java for the
 *   7-value enum.
 * - Failing = result is FAILED or UNKNOWN. This matches LogApi.java's existing
 *   "failed" convention (LogApi.java:691-695): both FAILED and UNKNOWN results
 *   count as failures in the certification package builder, so this helper must
 *   do the same to avoid hiding UNKNOWN failures from the user.
 *
 * Both sides case-normalize (`.toUpperCase()`) so lowercase enum values
 * classify correctly. The helper is PURE over an array of log records — it
 * never fetches anything; the caller owns the `/api/log` request.
 */

/**
 * Statuses that count as "in progress" (actively executing). Whitelist, not a
 * negation of FINISHED — see the module-level comment.
 * @type {ReadonlyArray<string>}
 */
const IN_PROGRESS_STATUSES = ["RUNNING", "WAITING"];

/**
 * Results that count as "failing". Mirrors LogApi.java's failed convention:
 * both FAILED and UNKNOWN are treated as failures.
 * @type {ReadonlyArray<string>}
 */
const FAILING_RESULTS = ["FAILED", "UNKNOWN"];

/**
 * Query string (no leading "?") for the logs page filtered to in-progress runs.
 * Build a deep link with e.g. `logs.html?${IN_PROGRESS_LOGS_QUERY}`.
 * @type {string}
 */
export const IN_PROGRESS_LOGS_QUERY = "status=running,waiting";

/**
 * Query string (no leading "?") for the logs page filtered to failing runs.
 * Build a deep link with e.g. `logs.html?${FAILING_LOGS_QUERY}`.
 * @type {string}
 */
export const FAILING_LOGS_QUERY = "result=failed,unknown";

/**
 * Classifies an array of log records into in-progress and failing counts.
 *
 * Pure and side-effect-free: it does not fetch. Case-insensitive on both
 * `status` and `result`. Records missing the relevant field simply do not
 * match either bucket.
 * @param {ReadonlyArray<{status?: string, result?: string}>} logs - Log records,
 *   typically the `data` array from `/api/log`.
 * @returns {{inProgressCount: number, failingCount: number}} Counts of
 *   in-progress (RUNNING/WAITING) and failing (FAILED/UNKNOWN) runs.
 */
export function classifyRuns(logs) {
  const rows = Array.isArray(logs) ? logs : [];

  let inProgressCount = 0;
  let failingCount = 0;

  for (const log of rows) {
    const status = typeof log?.status === "string" ? log.status.toUpperCase() : "";
    const result = typeof log?.result === "string" ? log.result.toUpperCase() : "";

    if (IN_PROGRESS_STATUSES.includes(status)) {
      inProgressCount += 1;
    }
    if (FAILING_RESULTS.includes(result)) {
      failingCount += 1;
    }
  }

  return { inProgressCount, failingCount };
}
