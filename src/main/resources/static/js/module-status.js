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
