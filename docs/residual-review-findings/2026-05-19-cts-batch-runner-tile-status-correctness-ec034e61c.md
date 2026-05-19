# Residual Review Findings — cts-batch-runner tile status correctness fix

**Commit:** `ec034e61c` (`fix(cts-batch-runner): tile status mapping and completed-count semantics`)
**Plan:** [`docs/plans/2026-05-19-003-fix-cts-batch-runner-tile-status-correctness-plan.md`](../plans/2026-05-19-003-fix-cts-batch-runner-tile-status-correctness-plan.md)
**Parent plan:** [`docs/plans/2026-05-18-012-feat-wire-cts-batch-runner-into-plan-detail-plan.md`](../plans/2026-05-18-012-feat-wire-cts-batch-runner-into-plan-detail-plan.md)
**Board:** [`docs/plans/2026-05-18-006-orphan-components-wire-up-board.md`](../plans/2026-05-18-006-orphan-components-wire-up-board.md) (row 6, still gated)
**Review run:** `/tmp/compound-engineering/ce-code-review/20260519-160934-9bbad6c9/`
**Branch:** `feat/redesign`
**Reviewers (5):** correctness, testing, maintainability, project-standards, julik-frontend-races

## Context

This commit addresses the parent row-6 ce-code-review's residuals **#3 (P1, tile-badge data-shape)** and **#7 (P2, `_completedCount` overcounts)** — both component-internal correctness bugs that don't intersect the maintainer-gated UX decisions (parent plan Open Questions 1-3). Five `safe_auto` autofixes were folded into the feature commit during the autofix loop (3-branch dispatch + console.warn on FINISHED-without-result, brittle assertion removal, FINISHED-without-result and WAITING fixture coverage). The two items below are gated on intentional maintainer judgment.

## P1 — High (should resolve before this branch ships)

### #1. Field-split smell: `_hasRemaining` (instances) vs `_completedCount` (status) — 2-reviewer agreement

- **Reviewers:** maintainability (M-1, P2 conf 60), julik-frontend-races (JFR-1, P1 conf 75) — promoted to confidence **100** by cross-reviewer agreement
- **File:** `src/main/resources/static/components/cts-batch-runner.js:141-143`
- **Issue:** `_completedCount` now filters on `status === "FINISHED"` while `_hasRemaining` still filters on `!m.instances?.length`. The two getters derive related-but-different concepts ("finished" vs "never started") from different fields. Both produce correct results in steady state because `plan-detail.html` writes `instances` and `status`/`result` atomically. If the write timing ever splits across cycles, the divergence becomes visible (e.g., a module with instances but no status yet shows `_hasRemaining=false` but `_completedCount=0`).
- **⚠ Why both reviewers' suggested fixes are wrong:** M-1 suggested `!m.status`; JFR-1 suggested `!m.status || m.status !== 'FINISHED'`. **Both would re-dispatch RUNNING modules** when "Run Remaining" is clicked, because they fold RUNNING into the "remaining" set. The page-level dispatch (`plan-detail.html:394-396`) uses `!m.instances`, so "remaining" semantically means "never been dispatched" — RUNNING modules are deliberately excluded to avoid duplicate runs. The field-split is intentional, not accidental.
- **Maintainer judgment needed on:**
  - (a) **Document the intentional split** with inline comments on both getters (smallest change)
  - (b) **Introduce a unified `_moduleLifecycleStage(m)` accessor** returning `'PENDING' | 'IN_FLIGHT' | 'FINISHED'`, with both getters derived from it (cleaner abstraction)
  - (c) **Refactor the page's dispatch contract** to align with whichever approach (a)/(b) takes
- **Routing:** `gated_auto` · `downstream-resolver` · component-internal refactor.

## P2 — Moderate

### #2. `_completedCount` has zero automated test coverage

- **Reviewer:** testing (T01, P2 conf 90)
- **File:** `src/main/resources/static/components/cts-batch-runner.stories.js`
- **Issue:** The `_completedCount` fix is the core behavioral change of #7 from the parent residuals, but the progress text `${this._completedCount} of ${this.modules.length}` only renders when `_running === true`. No story sets `_running = true`, so no play function can assert the counter's correctness.
- **Why this is gated, not safe_auto:** Setting `batchRunner._running = true` from a play function would use the same direct-private-state access pattern that's flagged as **parent plan P1 #2** (3-reviewer agreement: maintainability + reliability + julik-frontend-races). Adding test coverage via the same anti-pattern would reinforce it instead of letting the U2 work decide whether to expose `running` as a public reflected attribute or a `startBatch()` method.
- **Suggestion:** address as part of parent plan's U2 (gated on Joseph/Thomas). When the maintainers approve a public API for batch state, retrofit the coverage assertions then.
- **Routing:** `gated_auto` · `downstream-resolver` · awaits parent plan P1 #2 resolution.

## P3 / Advisory (suppressed by confidence gate or demoted to advisory)

- **#3. (correctness, suppressed advisory):** Raw `TestModule.Status` strings (`CREATED` / `CONFIGURED` / `INTERRUPTED`) leak as user-visible badge labels for any non-FINISHED, non-RUNNING, non-WAITING status. The `_moduleVariant` fall-through to `"skip"` handles styling but the label is the raw enum string. Mitigation would be an explicit map for transient statuses; deferred until any leak surface in practice.
- **#4. (maintainability M-2, suppressed at anchor 50):** Rename `_moduleResult` → `_moduleBadgeLabel` because the method now also returns lifecycle states (RUNNING/WAITING), not just terminal results. Cosmetic; left for the parent plan's wider refactor.
- **#5. (maintainability M-3, suppressed at anchor 50):** Inline fixture duplication across `MOCK_MODULES_*` constants. Park until a 4th fixture appears.
- **#6. (project-standards):** No E2E coverage of the corrected tile-status path on `plan-detail.html`. The parent plan's U3 (gated) is the right home for this coverage.

## Verdict

**Ready for branch ship.** Five `safe_auto` autofixes are already applied in `ec034e61c`. The two residual items above are intentional handoffs — both intersect parent plan gates that need Joseph/Thomas judgment, and acting on the maintainability/julik suggestions for #1 would actively regress Run Remaining semantics.
