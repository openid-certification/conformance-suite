# Residual Review Findings — cts-batch-runner U1 wiring

> **STATUS — DESCOPED 2026-05-19.** The U1 page wiring was reverted from `plan-detail.html` because `master` has no batch-running functionality and the team has not yet validated batch dispatch as a desirable workflow. All findings below are moot until / unless wiring is restored. The component (`cts-batch-runner.js`) and its Storybook stories remain intact. See the board note at `docs/plans/2026-05-18-006-orphan-components-wire-up-board.md` (row 6) and the decision analysis at `tmp/row-6-decision.html`.

**Commit:** `645ee04d3` (`feat(plan-detail): wire cts-batch-runner with Run All / Run Remaining`) — reverted
**Plan:** [`docs/plans/2026-05-18-012-feat-wire-cts-batch-runner-into-plan-detail-plan.md`](../plans/2026-05-18-012-feat-wire-cts-batch-runner-into-plan-detail-plan.md)
**Board:** [`docs/plans/2026-05-18-006-orphan-components-wire-up-board.md`](../plans/2026-05-18-006-orphan-components-wire-up-board.md) (row 6)
**Review run:** `/tmp/compound-engineering/ce-code-review/20260519-131631-b6fe6a58/`
**Branch:** `feat/redesign`
**Reviewers (9):** correctness, testing, maintainability, project-standards, agent-native, learnings, julik-frontend-races, adversarial, reliability

## Context

U1 of the plan ships first as a draft MR. U2 (Storybook play-function coverage) and U3 (Playwright E2E spec) are gated on maintainer (Joseph/Thomas) sign-off via the plan's ⚠ pause point. The plan explicitly carries three Open Questions for the maintainer review:

1. Does batch-run match CTS workflow conventions (or break a deliberate variant-inspection workflow)?
2. Keep or subsume the existing per-module Run button?
3. Backend support: does `/api/runner` tolerate N concurrent POSTs?

This document records residual review findings that need maintainer judgment before this draft MR promotes to ready. Five safe_auto fixes were already applied in the feature commit (`645ee04d3`); the items below are gated on intentional decisions that the autofix loop cannot make.

## P0 — Critical (must resolve before merge)

### #1. Alias-collision self-stop on Run All

- **Reviewer:** adversarial (confidence 80, severity high)
- **File (client):** `src/main/resources/static/plan-detail.html` `dispatchBatch`
- **File (server):** `src/main/java/net/openid/conformance/runner/TestRunner.java:446-496`
- **Issue:** If a plan's modules share an `alias` (a common configuration pattern), `createTestAlias` in TestRunner.java stops every prior same-owner test before claiming the alias. The pre-existing per-module Run button never triggered this at human-click speed; parallel batch dispatch makes Run All **structurally broken** for the common shared-alias case. The author's own comment at `TestRunner.java:493` acknowledges the race.
- **Fix paths:**
  - **Client:** serialise dispatch (await each POST before the next) — slower but trivially correct.
  - **Server:** add a transactional bulk-dispatch endpoint that resolves alias precedence atomically.
- **Routing:** `manual` · maintainer decision required (intersects plan Open Question 3).

## P1 — High (should resolve before draft promotes)

### #2. `_running` direct-assignment bypasses Lit `{ state: true }` encapsulation (3-reviewer agreement)

- **Reviewers:** maintainability (M-02, P1 conf 75), reliability (REL-003, P2 conf 85), julik-frontend-races (JFR-06, conf 100)
- **File:** `src/main/resources/static/components/cts-batch-runner.js:104-108` (out of this PR's single-file scope)
- **Issue:** `dispatchBatch` writes `batchRunner._running = true` directly to a Lit private reactive state field. Lit honors the write today, but a future Lit upgrade, minification, or component-side refactor could silently break it. Per learnings research, no existing solution doc covers this seam.
- **Fix paths:**
  - Promote `_running` to a public reflected attribute (`running: { type: Boolean, reflect: true }`); page writes `batchRunner.running = true`.
  - Expose a `startBatch()` method on the component; page calls `batchRunner.startBatch()`.
- **Routing:** `gated_auto` · `downstream-resolver`. Per learnings, **if the maintainer approves the direct-assignment pattern, a solution doc should follow** — this seam will recur on the next orphan-component wiring.

### #3. Tile badges never reflect terminal status (correctness)

- **Reviewer:** correctness (P1 conf 90)
- **File:** `src/main/resources/static/components/cts-batch-runner.js:140-149`
- **Issue:** `mod.instances` on plan-detail.html is an array of **string** instance IDs. The status-merge writes `status`/`result` to the top-level module object. But `_moduleResult` reads `instances[last].result` — `.result` on a string is undefined — falls back to `'RUNNING'`. Every started module's tile shows RUNNING; the tile grid is effectively useless for PASSED/FAILED/WARNING/SKIPPED.
- **Fix paths:**
  - **Component-side (preferred):** change `_moduleResult` to prefer top-level `module.result` (and `module.status === 'FINISHED' ? module.result : module.status`).
  - **Page-side:** synthesise `instances: [{ result: m.result }]` when writing to `batchRunner.modules` — works but hacks the data shape.
- **Routing:** `gated_auto` · `downstream-resolver` · component-file change.

### #4. No fetch timeout — hung POST blocks the page indefinitely

- **Reviewer:** reliability (REL-001, P1 conf 90)
- **File:** `src/main/resources/static/plan-detail.html` `runTestNoNavigate`
- **Issue:** No `AbortSignal`, no timeout. A single hung request leaves `Promise.all` unresolved; `_running` is stuck true; both buttons permanently disabled until manual reload.
- **Fix:** wrap each fetch in `AbortController` + `setTimeout(N s)`; on abort, route to the per-attempt `.catch` so the batch continues.
- **Maintainer call:** what timeout value? 30s is a reasonable default; the server's own response budget should drive it.
- **Routing:** `gated_auto` · `downstream-resolver`.

### #5. N-modal storm on batch failure (3-reviewer agreement)

- **Reviewers:** reliability (REL-002, P1 conf 95), julik-frontend-races (JFR-03 chain), adversarial (adv-batch-error-modal-storm, conf 90)
- **File:** `src/main/resources/static/plan-detail.html` `dispatchBatch` per-attempt `.catch`
- **Issue:** Each rejected `runTestNoNavigate` calls `handleApiError` → `FAPI_UI.showError` → modal dialog. On 50 failures the user is locked in a 50-modal dismiss loop.
- **Fix:** coalesce per-attempt failures into a single consolidated modal after `Promise.all` settles ("3 of 50 modules failed to start").
- **Maintainer call:** modal copy + count threshold (do we always coalesce, or only above a threshold?).
- **Routing:** `gated_auto` · `downstream-resolver`.

### #6. Non-owner re-run confirmation modal bypassed by batch dispatch

- **Reviewer:** agent-native (warning, severity high)
- **File:** `src/main/resources/static/plan-detail.html:350` (single-module `cts-run-test` handler) vs `:380-388` (batch handlers)
- **Issue:** Single-module path checks `FAPI_UI.currentUser.sub !== data.owner.sub` and shows `reRunTestModal` ("Are you sure you want to rerun this test? It is owned by a different user."). Batch path skips this entirely. A logged-in non-owner with write permissions can Run All against someone else's plan without the friction the single-module path enforces.
- **Fix paths:**
  - Add an equivalent one-time confirmation modal for batch dispatch when the user isn't the plan owner.
  - Explicitly accept the asymmetry (per-N modal confirmations don't scale) and document it.
- **Routing:** `gated_auto` · `downstream-resolver` · maintainer decision.

## P2 — Moderate

### #7. `_completedCount` overcounts RUNNING modules (2-reviewer agreement)

- **Reviewers:** julik-frontend-races (JFR-05, conf 80), adversarial (adv-batch-completedcount-overcounts-running, conf 95)
- **File:** `src/main/resources/static/components/cts-batch-runner.js:127`
- **Issue:** `_completedCount` filters `instances?.length > 0` — any module with run history counts as "completed", regardless of whether the last run is RUNNING/PENDING/PASSED/FAILED. The progress display reads "3 of 10" while modules are still in-flight.
- **Fix:** rename to `_startedCount` (truthful) or compute from terminal status (PASSED/FAILED/WARNING/REVIEW/SKIPPED).
- **Routing:** `gated_auto` · `downstream-resolver` · component-file change.

### #8. No backpressure on N parallel fetches (plan Open Question 3)

- **Reviewer:** reliability (REL-005, P2 conf 75); also raised by adversarial (adv-batch-resource-exhaustion-100-parallel, conf 70)
- **Issue:** 100-module plans send 100 parallel POSTs on a single click. Tomcat threads, Mongo pool, HTTP client pool affected. The single-module Run button operates at human-click speed (~1/sec); batch is 100x burst.
- **Maintainer call (this is plan Open Question 3):** does `/api/runner` tolerate N concurrent POSTs, or does the implementer need a fixed-window queue / sequential async loop?
- **Routing:** `manual` · `downstream-resolver`.

## P3 / Advisory

- **#9. Listener accumulation if `getPlan()` called twice** (julik JFR-01, conf 90): structural; won't fire today (getPlan called once on DOMContentLoaded). Fix when adding a refresh-without-reload path (AbortController + `{ signal }` on each addEventListener). `advisory`.
- **#10. `_running` race with Lit element-upgrade lifecycle** (reliability REL-003, conf 85): `customElements.whenDefined` resolves on class registration, not `connectedCallback`. Same fix as #2. `gated_auto`.
- **#11. Two-tab concurrent batch** (adversarial conf 65): two tabs each clicking Run All → 2x dispatch with no coordination. Accept as inherent to client-driven dispatch.
- **#12. Tab close orphans in-flight POSTs** (adversarial conf 70): pre-existing posture; single-module path has the same shape. Acceptable.
- **#13. Inner R28 Promise chain has no `.catch`** (julik JFR-07, conf 75): pre-existing; not introduced by this PR.

## Pre-existing (separate from this PR's verdict)

- **Authorization gap on `/api/runner`** (adversarial adv-readonly-client-only-no-server-ownership-check, conf 80): server only checks `authenticated()`, not plan ownership. The diff's `batchRunner.hidden = readonly` is pure UX. `WebSecurityResourceServerConfig:144` + `TestRunner.createTest:215-244` show no ownership gate. **Pre-existing** — the per-module Run button has the same gap. The batch endpoint multiplies impact but does not introduce the vulnerability.

## Verdict

**Ready for draft MR review.** Maintainer (Joseph/Thomas) sign-off required on:

1. The plan's three Open Questions (workflow conventions, per-module button keep/remove, backend concurrent POST tolerance).
2. P0 finding #1 (alias-collision self-stop) — the most consequential discovery from this review pass; intersects Open Question 3.
3. P1 findings #2–#6 (encapsulation, tile data shape, timeout, modal storm, non-owner confirmation).

Five safe_auto fixes are already applied in `645ee04d3`. The remaining items intentionally surface decisions the maintainer should make before this draft promotes to ready — that's exactly why the plan's pause point exists.
