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
 * The five settled result verdicts mapped to their canonical cts-badge variant.
 * Module-scoped (allocated once, not per call — these functions run once per
 * module in a `repeat()` over the whole plan) and shared by both helpers:
 * `statusBadgeVariant` returns the variant; `statusLabel` uses it as the
 * membership test for "is this a settled verdict?" (a settled verdict labels
 * itself). A result absent from this map (UNKNOWN/null) is not settled.
 * @type {{ [result: string]: string }}
 */
const RESULT_VARIANTS = {
  PASSED: "pass",
  FAILED: "fail",
  WARNING: "warn",
  REVIEW: "review",
  SKIPPED: "skip",
};

/**
 * Maps module status/result to a canonical cts-badge variant.
 *
 * A settled result verdict wins over the lifecycle status, because a failed
 * test is reported by the runner as status=INTERRUPTED (it stops on the first
 * hard failure and never reaches FINISHED) with result=FAILED. Gating the
 * colour on FINISHED alone painted such a test the neutral `skip` grey instead
 * of `fail` red (GitLab #1858/#1859). `status` is consulted only when there is
 * no settled result.
 *
 * - null/empty status                 -> "skip" (PENDING — neutral until run)
 * - any settled result (regardless of FINISHED vs INTERRUPTED status):
 *     PASSED -> "pass", FAILED -> "fail", WARNING -> "warn",
 *     REVIEW -> "review", SKIPPED -> "skip"
 * - else RUNNING                      -> "running"
 * - else (WAITING, bare INTERRUPTED, UNKNOWN) -> "skip"
 * @param {string|null|undefined} status - Module status: null/undefined,
 *   "RUNNING", "WAITING", "INTERRUPTED", or "FINISHED".
 * @param {string|null|undefined} result - Module result: "PASSED", "FAILED",
 *   "WARNING", "REVIEW", "SKIPPED", "UNKNOWN", or null.
 * @returns {string} Canonical cts-badge variant.
 */
export function statusBadgeVariant(status, result) {
  if (!status) return "skip";
  if (result && RESULT_VARIANTS[result]) return RESULT_VARIANTS[result];
  if (status === "RUNNING") return "running";
  return "skip";
}

/**
 * Maps module status/result to a human-readable badge label.
 *
 * A settled result verdict wins over the lifecycle status: a failed test is
 * reported as status=INTERRUPTED, result=FAILED and must read "FAILED", not
 * "INTERRUPTED" (GitLab #1859). `status` is the label only when there is no
 * settled verdict — an in-flight test (RUNNING/WAITING), a verdict-less
 * interruption (bare INTERRUPTED), or a never-run module (PENDING).
 * @param {string|null|undefined} status - Module status: null/undefined,
 *   "RUNNING", "WAITING", "INTERRUPTED", or "FINISHED".
 * @param {string|null|undefined} result - Module result: "PASSED", "FAILED",
 *   "WARNING", "REVIEW", "SKIPPED", "UNKNOWN", or null.
 * @returns {string} Display label (e.g. "PENDING", "RUNNING", "PASSED",
 *   "FAILED").
 */
export function statusLabel(status, result) {
  if (!status) return "PENDING";
  if (result && RESULT_VARIANTS[result]) return result;
  if (status === "RUNNING") return "RUNNING";
  return status;
}

/**
 * Resolve the status variant for a plan module *segment* — the single source
 * of truth shared by `cts-plan-status` (all three surfaces) and the legacy
 * `cts-plan-list` status boxes. Lifted verbatim from
 * `cts-plan-list._statusVariantFor` so the pending-vs-settled-vs-resolved logic
 * lives in one place rather than being re-derived per component (KTD3):
 *
 * - never-run module (no instances) → static `skip` (neutral gray)
 * - has run, status not yet fetched (`_statusResolved !== true`) → `pending`
 *   (gray, pulsing) — distinct from the static `skip` of a never-run module
 * - status resolved → the concrete variant from `statusBadgeVariant`
 *   (a fetch failure settles status/result undefined → `skip`)
 *
 * Each surface MUST set `_statusResolved = true` when it merges the resolved
 * `{ status, result }` — in BOTH the success and the error/404 branches —
 * otherwise a resolved or 404'd segment pulses `pending` forever.
 * @param {{instances?: string[], status?: string, result?: string,
 *   _statusResolved?: boolean}} mod - A plan module entry.
 * @returns {string} The segment status variant (one of `pass`, `fail`, `warn`,
 *   `running`, `review`, `skip`, `pending`).
 */
export function segmentVariant(mod) {
  const hasInstance = Array.isArray(mod.instances) && mod.instances.length > 0;
  if (!hasInstance) return "skip";
  if (mod._statusResolved) return statusBadgeVariant(mod.status, mod.result);
  return "pending";
}

/**
 * Sentinel filter value for "Not yet run" modules. Kept distinct from the real
 * result tokens (PASSED/FAILED/WARNING/REVIEW/SKIPPED) so a result filter can
 * select never-run modules without colliding with a real result name.
 * @type {string}
 */
export const NOT_RUN_FILTER_VALUE = "NOT_RUN";

/**
 * Whether a module matches an active "Filter by result" selection. Shared by
 * `cts-plan-status` (segment dimming, R10/R18) and `cts-plan-modules` (row
 * narrowing, R9) so the two never drift.
 *
 * Matching reads the module's RAW `{ status, result }`, NOT the collapsed
 * `segmentVariant` — `statusBadgeVariant` maps both null-status and
 * FINISHED+SKIPPED to `skip`, so a variant-keyed filter would wrongly catch a
 * genuinely SKIPPED module under "Not yet run". So:
 *
 * - `NOT_RUN_FILTER_VALUE` matches a never-run module (no instances) or one
 *   that resolved to a null status — NOT a FINISHED+SKIPPED module.
 * - A result token (e.g. `"FAILED"`) matches the module's raw `result`.
 * - A still-pending module (has instances, `_statusResolved !== true`) has no
 *   raw result yet, so it matches nothing and is treated as non-matching until
 *   it settles (R18).
 *
 * An empty / absent filter matches everything (no narrowing/dimming).
 * @param {{instances?: string[], status?: string, result?: string,
 *   _statusResolved?: boolean}} mod - A plan module entry.
 * @param {Set<string>|null|undefined} filter - Selected result tokens (plus the
 *   `NOT_RUN_FILTER_VALUE` sentinel).
 * @returns {boolean} `true` when the module matches the active filter (or when
 *   no filter is active).
 */
export function moduleMatchesResultFilter(mod, filter) {
  if (!filter || filter.size === 0) return true;
  const hasInstance = Array.isArray(mod.instances) && mod.instances.length > 0;
  if (filter.has(NOT_RUN_FILTER_VALUE)) {
    if (!hasInstance) return true;
    // A resolved module whose status came back null/empty is also "not run".
    if (mod._statusResolved && !mod.status) return true;
  }
  return !!mod.result && filter.has(mod.result);
}

/**
 * DOM id for a plan module's row in `cts-plan-modules`, and the matching
 * in-page anchor target for that row's `cts-plan-status` segment (detail mode).
 * Keyed by plan-order index so the segment's `href="#<moduleRowId>"` and the
 * row's `id` always line up. Shared so the two never drift (a classic page
 * script that cannot import this — e.g. plan-detail.html's coordinator — must
 * replicate the `cts-module-<index>` scheme literally and say so).
 * @param {number} index - The module's index in plan order.
 * @returns {string} The row's DOM id (e.g. `cts-module-3`).
 */
export function moduleRowId(index) {
  return `cts-module-${index}`;
}

/**
 * Index of the plan module whose instance list includes `instanceId` — the
 * "you are here" module when viewing a log. Matches against each module's FULL
 * instance list (not just the most recent) so viewing an older re-run still
 * resolves to the right module (R17). Shared by `cts-plan-status` (the log-mode
 * "you are here" marker) and `cts-test-nav-controls` (the "Module N of M"
 * position label it renders beside the bar) so the two never disagree on which
 * module is current.
 * @param {Array<{instances?: string[]}>} modules - Plan modules in plan order.
 * @param {string} instanceId - The instance currently being viewed.
 * @returns {number} The matching index, or -1 when there is no match.
 */
export function currentModuleIndex(modules, instanceId) {
  if (!instanceId || !Array.isArray(modules)) return -1;
  return modules.findIndex((m) => Array.isArray(m.instances) && m.instances.includes(instanceId));
}

/**
 * Stable, content-derived identity key for a plan module entry — unique across
 * lists so a keyed `repeat()` never reuses DOM across a full module-set swap,
 * and so an action handler resolves to the right module regardless of array
 * order. Shared by `cts-plan-status` (segment repeat key) and `cts-plan-modules`
 * (the Run button's `data-module-key`) so the key never drifts between them.
 * @param {{testModule?: string, variant?: object}} mod - A plan module entry.
 * @returns {string} `testModule` plus its serialized variant.
 */
export function moduleKey(mod) {
  return `${mod.testModule}|${JSON.stringify(mod.variant ?? null)}`;
}
