/**
 * Shared mapping from a test module's status/result to design-system badge
 * presentation. Extracted from cts-plan-modules.js so both the plan-detail
 * module list (cts-plan-modules) and the plans listing (cts-plan-list) draw
 * status color from one source of truth rather than two drifting copies.
 *
 * These are pure functions over the `{ status, result }` shape returned by
 * `/api/info/<instance>`; they carry no component-specific logic, so a future
 * divergence should be handled by the caller rather than by forking this file.
 */

/**
 * Maps module status/result to a canonical cts-badge variant.
 *
 * - null status        -> "skip" (PENDING — neutral until run)
 * - RUNNING            -> "running"
 * - FINISHED + PASSED  -> "pass"
 * - FINISHED + FAILED  -> "fail"
 * - FINISHED + WARNING -> "warn"
 * - FINISHED + REVIEW  -> "review"
 * - FINISHED + SKIPPED -> "skip"
 * @param {string|null|undefined} status - Module status: null/undefined,
 *   "RUNNING", or "FINISHED".
 * @param {string|null|undefined} result - Module result when status is
 *   "FINISHED": "PASSED", "FAILED", "WARNING", "REVIEW", "SKIPPED", or null.
 * @returns {string} Canonical cts-badge variant.
 */
export function statusBadgeVariant(status, result) {
  if (!status) return "skip";
  if (status === "RUNNING") return "running";
  if (status === "FINISHED") {
    const map = {
      PASSED: "pass",
      FAILED: "fail",
      WARNING: "warn",
      REVIEW: "review",
      SKIPPED: "skip",
    };
    return map[result] || "skip";
  }
  return "skip";
}

/**
 * Maps module status/result to a human-readable badge label.
 * @param {string|null|undefined} status - Module status: null/undefined,
 *   "RUNNING", or "FINISHED".
 * @param {string|null|undefined} result - Module result when status is
 *   "FINISHED".
 * @returns {string} Display label (e.g. "PENDING", "RUNNING", "PASSED").
 */
export function statusLabel(status, result) {
  if (!status) return "PENDING";
  if (status === "RUNNING") return "RUNNING";
  if (status === "FINISHED" && result) return result;
  return status;
}
