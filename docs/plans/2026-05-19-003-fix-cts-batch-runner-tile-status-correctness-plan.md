---
title: "fix: cts-batch-runner tile status mapping and completed-count semantics"
type: plan
status: active
created: 2026-05-19
branch: feat/redesign
depth: lightweight
origin: docs/residual-review-findings/2026-05-19-cts-batch-runner-plan-detail-645ee04d3.md
board: docs/plans/2026-05-18-006-orphan-components-wire-up-board.md
parent_plan: docs/plans/2026-05-18-012-feat-wire-cts-batch-runner-into-plan-detail-plan.md
---

# fix: cts-batch-runner tile status mapping and completed-count semantics

## Summary

`cts-batch-runner` (`src/main/resources/static/components/cts-batch-runner.js`) renders a tile per module with a status badge and shows an "N of M" progress count while a batch runs. Both readers were authored against a synthetic data shape (each `instances[i]` is `{ result: "..." }`) but `plan-detail.html` writes the real production shape (`instances` is an array of string instance IDs; `status` and `result` live on the **top-level** module). The result:

1. **P1 #3 (tile badges, correctness conf 90).** `_moduleResult` reads `instances[last].result`. `.result` on a string is `undefined`, the fallback returns `"RUNNING"`, so every module that has run at least once shows the running pulse — even when it has actually FINISHED/PASSED/FAILED/WARNING. The tile grid is currently useless for terminal status.
2. **P2 #7 (`_completedCount` semantics, 2-reviewer agreement, conf 80/95).** `_completedCount` counts modules whose `instances?.length > 0`. Any module with run history counts as "completed", regardless of whether the latest run is RUNNING, PENDING, PASSED, FAILED, etc. The progress display reads "3 of 10" while modules are still in-flight.

Both bugs are component-internal, mechanical, and do not intersect any of the three Open Questions gated on Joseph/Thomas maintainer review (workflow conventions, per-module Run button keep/remove, backend concurrent POST tolerance). Fixing them advances the row 6 draft MR (`645ee04d3` on `feat/redesign`, MR [!1998](https://gitlab.com/openid/conformance-suite/-/merge_requests/1998)) toward review-ready without violating the plan's ⚠ pause point on U2 (Storybook play-function coverage) or U3 (Playwright E2E).

## Problem Frame

- **Today:** Every started module in the batch-runner tile grid shows the RUNNING badge regardless of true status; the "N of M" progress reads as soon as a module has any run history, not when it has terminated. Both readings are wrong against real `/api/plan/<id>` + `/api/info/<instance>` merge data.
- **Target:** Tile badges show the module's actual terminal status (PASSED / FAILED / WARNING / REVIEW / SKIPPED) once `status === "FINISHED"`, the live transient status (RUNNING / WAITING) while in-flight, and PENDING when there is no run history. The "N of M" count reflects modules whose latest run has terminated.
- **Why:** A maintainer who opens the draft MR's plan-detail page expects the tile grid to show real status. The broken reader makes the row-6 wiring look like it doesn't work, which would unfairly bias the Open Questions review.

## Requirements

- R1. `_moduleResult` returns the module's actual terminal result (`module.result`) when `module.status === "FINISHED"`.
- R2. `_moduleResult` returns the in-flight status (`"RUNNING"` / `"WAITING"`) when `module.status` is set but not `"FINISHED"`.
- R3. `_moduleResult` returns `"PENDING"` when `module.status` is absent (no run history yet).
- R4. `_completedCount` counts modules whose `status === "FINISHED"` — i.e., the latest run has terminated.
- R5. Existing Storybook stories (Default / WithResults / Empty / Persistent) render visually correct status badges against the new reader, using fixtures whose shape matches production (top-level `status`/`result`, `instances` as string-or-empty).
- R6. No regression to the `cts-run-all` / `cts-run-remaining` event dispatch, the `_running` disable behavior, or the `_hasRemaining` "Run Remaining" affordance.

## Scope Boundaries

**In scope:**
- `src/main/resources/static/components/cts-batch-runner.js` — fix the two readers.
- `src/main/resources/static/components/cts-batch-runner.stories.js` — update fixtures so stories use the real data shape.

**Out of scope:**
- Adding new Storybook play-function tests for the run-all / run-remaining / disabled flows (that is U2 of the parent plan; gated on maintainer sign-off).
- Adding the Playwright E2E spec (that is U3 of the parent plan; gated).
- Promoting `_running` to a public reflected attribute or `startBatch()` method (P1 #2; gated_auto downstream-resolver — API design decision).
- Adding fetch timeout / AbortController on `runTestNoNavigate` (P1 #4; gated_auto — needs maintainer timeout value).
- Coalescing the N-modal storm on batch failure (P1 #5; gated_auto — needs maintainer copy + threshold).
- Non-owner batch-confirmation modal (P1 #6; gated_auto — design decision).
- Backpressure / concurrency control on N parallel POSTs (P2 #8 — intersects parent plan Open Question 3).
- The P0 alias-collision in `TestRunner.createTestAlias` — server-side fix, needs maintainer routing decision.

### Deferred to Follow-Up Work
- A solution-doc capturing the "page writes top-level status; component reads top-level status" pattern for other batch-style components that may follow. Document only if this seam recurs.

## Implementation Units

### U1. Fix `_moduleResult` to read top-level module status

- **Goal:** The tile badge reflects each module's actual status (terminal result, in-flight status, or PENDING).
- **Requirements:** R1, R2, R3, R5, R6.
- **Dependencies:** None.
- **Files:**
  - `src/main/resources/static/components/cts-batch-runner.js` (modify)
  - `src/main/resources/static/components/cts-batch-runner.stories.js` (modify fixtures)
- **Approach:**
  - Replace the body of `_moduleResult(module)` to dispatch on `module.status`:
    - No `status` → `"PENDING"`.
    - `status === "FINISHED"` → `module.result` (caller's `RESULT_BADGE_VARIANTS` map already covers PASSED/FAILED/WARNING/REVIEW/SKIPPED).
    - Otherwise → `module.status` (covers `"RUNNING"`, `"WAITING"`, and any other transient TestModule.Status values).
  - Story fixtures currently shape `instances: [{ result: "PASSED" }]`. Update them to mirror real data: `instances: ["mock-uuid-1"]` with `status: "FINISHED"` and `result: "PASSED"` on the top-level entry. Empty `instances: []` for PENDING. The synthetic `instances[i].result` reader is the bug; keep stories on a faithful shape so future test coverage doesn't drift back.
- **Patterns to follow:**
  - `plan-detail.html` lines 418-421 is the canonical write path — it does `Object.assign({}, modulesData[idx], { status: info.status, result: info.result })`. The reader must mirror this contract.
  - The status-vs-result split is the same one `cts-plan-modules` uses in its own badge logic — confirm shape parity before declaring the unit complete.
- **Test scenarios:**
  - Module with `status: "FINISHED"`, `result: "PASSED"` → `_moduleResult` returns `"PASSED"`, tile badge variant is `pass`.
  - Module with `status: "FINISHED"`, `result: "FAILED"` → returns `"FAILED"`, variant `fail`.
  - Module with `status: "FINISHED"`, `result: "WARNING"` → returns `"WARNING"`, variant `warn`.
  - Module with `status: "FINISHED"`, `result: "SKIPPED"` → returns `"SKIPPED"`, variant `skip`.
  - Module with `status: "RUNNING"` (no `result` yet) → returns `"RUNNING"`, variant `running`.
  - Module with `status: "WAITING"` → returns `"WAITING"` (and `RESULT_BADGE_VARIANTS["WAITING"]` is presumed defined; if absent, fall back through the existing `|| "skip"` chain).
  - Module with no `status` and empty `instances` → returns `"PENDING"`.
  - Visual: the `WithResults` story renders a tile grid with one tile per result variant — pass, fail, warn, skip — not all RUNNING.
- **Verification:**
  - `cd frontend && npm run test:ci` is green.
  - The `WithResults` Storybook story shows a tile grid with the expected mixed-status badges (no all-RUNNING).
  - Real-page smoke: load `https://localhost.emobix.co.uk:8443/plan-detail.html?plan=<id>` with at least one FINISHED module; tile reflects the actual result, not RUNNING.

### U2. Fix `_completedCount` to count terminal modules only

- **Goal:** The "N of M" progress display reflects modules whose latest run has terminated, not modules with any run history.
- **Requirements:** R4, R6.
- **Dependencies:** None (independent of U1; can land in the same commit).
- **Files:**
  - `src/main/resources/static/components/cts-batch-runner.js` (modify)
- **Approach:**
  - Replace `_completedCount` body to filter `modules.filter((m) => m.status === "FINISHED").length`. `FINISHED` is the TestModule.Status terminal sentinel — when present, the module's `result` is populated (PASSED/FAILED/WARNING/REVIEW/SKIPPED). The count is "how many runs are done", regardless of pass/fail.
  - Leave `_hasRemaining` unchanged — its semantic of "any module without run history" is still correct for the Run Remaining affordance ("PENDING modules only, do not redo FINISHED").
- **Patterns to follow:**
  - The same `m.status === "FINISHED"` predicate already exists in `plan-detail.html:446-448` for the "Publish for certification" gate. Keep the predicate identical so the page-level and component-level definitions of "completed" do not drift.
- **Test scenarios:**
  - All 10 modules `status: "FINISHED"` → count is `10`.
  - 3 `FINISHED` + 5 `RUNNING` + 2 no-status → count is `3`.
  - 0 modules with any status → count is `0`.
  - During an in-flight batch (some still RUNNING), the count does not reach `modules.length` until every module has FINISHED.
- **Verification:**
  - Same `npm run test:ci` pass as U1.
  - Visual: Storybook `WithResults` story shows progress text `"N of M"` where N is the count of terminal modules in the fixture.

## Open Questions

None blocking. The fixes are mechanical correctness changes that do not touch the maintainer-gated UX decisions (parent plan Open Questions 1-3 remain gated; this plan does not surface new judgment calls).

If U1's status-dispatch finds a transient `TestModule.Status` value not covered by `RESULT_BADGE_VARIANTS` (e.g., `"CREATED"`, `"CONFIGURED"`), the existing `|| "skip"` fallback in `_moduleVariant` preserves the safe behavior. No new variants are added by this plan.

## Verification

- `cd frontend && npm run test:ci` is green.
- `cd frontend && npm run test:e2e` is green (with the same baseline of pre-existing failures recorded in [E2E pre-existing failures 2026-05-18](../../.claude/projects/-Users-kaelig-src-openid-conformance-suite/memory/feedback_e2e_pre_existing_failures_2026_05_18.md); the fix does not introduce regressions).
- Storybook `WithResults` story renders mixed-status badges (verified via `mcp__storybook-mcp__preview-stories`).
- Storybook `run-story-tests` is green for `cts-batch-runner` (the existing play functions assert event dispatch, not status badges; they should remain green).
- Real-page smoke: load `plan-detail.html` for a plan with FINISHED modules and confirm tile badges show actual status, not all RUNNING.
- Commit lands on `feat/redesign` and pushes to MR !1998 (or a stacked fix MR if maintainer review on !1998 is already in flight).
- Row 6 of the board stays `[ ]` (still gated on maintainer sign-off for U2/U3); a one-line note is appended noting the residual #3 + #7 fix.
