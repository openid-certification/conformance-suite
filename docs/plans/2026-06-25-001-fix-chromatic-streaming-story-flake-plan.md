---
title: "fix: Stop the live-polling flow story from flaking Chromatic VRT"
date: 2026-06-25
type: fix
status_note: "Lightweight — single-file story-parameter change"
---

# fix: Stop the live-polling flow story from flaking Chromatic VRT

## Summary

The `flows-test-lifecycle--plan-to-log-detail` Storybook story embeds a live
`<cts-log-viewer>` that polls `/api/log` on an interval and keeps mutating the
DOM after the play function finishes. Chromatic captures the story while the
poll loop is still feeding it data, so the snapshot is non-deterministic and produces
spurious diffs on every visual-regression CI run.

The repo already solved this exact class of problem for the six standalone
polling stories in `cts-log-viewer.stories.js` by setting
`chromatic: { disableSnapshot: true }` — Chromatic skips the snapshot while
`test-storybook` still runs the play function for behavior coverage. This plan
applies the same opt-out to the one flow story that polls and was never
opted in. Behavior coverage and component-level visual coverage are unchanged;
the only thing dropped is a non-deterministic snapshot that was never a
trustworthy baseline.

---

## Problem Frame

**What breaks.** Every Chromatic build that includes `Flows/Test Lifecycle`
flags `plan-to-log-detail` as changed even when nothing relevant changed,
because the captured pixels depend on poll timing rather than on the code.

**Why it polls.** The story (`src/main/resources/static/components/flows/test-lifecycle-flow.stories.js`,
`PlanToLogDetail` export) renders `<cts-log-viewer test-id="test-fail-001">`.
The viewer's `_fetchEntries()` reschedules itself with
`setTimeout(() => this._fetchEntries(), this._pollIntervalMs)` (default
`POLL_INTERVAL_MS`, ~3s) and only stops on disconnect or a terminal state
(`src/main/resources/static/components/cts-log-viewer.js`). The play function
additionally flips `#log-section` from `display:none` to `display:block`
partway through and waits for entries to load. The frozen clock
(`frontend/.storybook/frozen-clock.js`) pins `Date.now()` but intentionally
leaves timers running, so the polling itself is *not* frozen. Chromatic's
"settle then capture" heuristic races the poll cycle and the mid-play DOM
mutation, so the captured DOM differs build to build.

**Why exclusion is the right shape (not "make it deterministic").** This was a
confirmed decision with the requester: match the established precedent rather
than fight the viewer internals + Chromatic's settle heuristic. The flow
story's job is to verify composition and event wiring — a *behavior* test, not
a pinned visual state. The composed components are already snapshotted in
isolation (`cts-log-viewer`, `cts-log-detail-header`, `cts-plan-modules`, …),
and the two deterministic sibling stories in the same file (`AllTestsPassed`,
`RunTestEvent`) do not embed the viewer and continue to snapshot normally.

**Scope confirmed: the one offender only.** No sweep of other files, no shared
cross-file constant, no lint guard in this change.

---

## Requirements

- R1. `flows-test-lifecycle--plan-to-log-detail` must not produce a Chromatic
  snapshot, so it can no longer cause VRT diffs.
- R2. The story's play function must keep running under `npm run test-storybook`
  so the composition / event-wiring behavior stays covered.
- R3. The two deterministic siblings in the same file (`AllTestsPassed`,
  `RunTestEvent`) must remain snapshotted — the change is scoped to the
  polling export only.
- R4. The change must pass the frontend quality gates (`npm run test:ci`:
  format → lint → type-check → lint:jsdoc → lint:icons → lint:lit-analyzer →
  codegen:check) and `npm run test-storybook`.

---

## Key Technical Decisions

- **Use `chromatic: { disableSnapshot: true }`, not `disable: true`.** Mirrors
  the `pollingStoryParameters` precedent in `cts-log-viewer.stories.js`.
  `disableSnapshot` skips only the Chromatic snapshot; the story still renders
  and its play function still executes under `test-storybook`, preserving
  behavior coverage. `disable: true` would also work but removes the story from
  Chromatic entirely and diverges from the established pattern — rejected for
  consistency.
- **Inline the parameter in the flow story; do not extract a shared constant.**
  The confirmed scope is "just the offender." A cross-file shared
  `liveDataStoryParameters` would imply a systemic convention (and an
  audit/guard) that the requester explicitly deferred. A short inline comment
  pointing at the `pollingStoryParameters` rationale keeps the decision
  discoverable without widening scope.
- **Leave the play function untouched.** It is the behavior coverage that
  justifies keeping the story at all. Trimming it would weaken the only
  composed plan→log wiring test.
- **Accept the loss of the composed-layout snapshot as a documented trade-off.**
  No other story snapshots `cts-log-detail-header` + `cts-log-viewer` composed
  together. The requester chose exclude-only over adding a deterministic static
  twin; this is recorded under Scope Boundaries so a future contributor can
  revisit it deliberately.

---

## Implementation Units

### U1. Disable the Chromatic snapshot on the live-polling flow story

**Goal:** Stop `flows-test-lifecycle--plan-to-log-detail` from generating a
non-deterministic Chromatic snapshot, while keeping its play function running
in `test-storybook`. (Advances R1, R2, R3.)

**Dependencies:** None.

**Files:**
- `src/main/resources/static/components/flows/test-lifecycle-flow.stories.js` — modify

**Approach:**
- On the `PlanToLogDetail` export, merge `chromatic: { disableSnapshot: true }`
  into its existing `parameters` object (which already holds `msw.handlers`).
  Do not touch `render`, `play`, or the `msw` handlers.
- Add a brief comment above the `chromatic` key explaining the why and
  pointing to the `pollingStoryParameters` precedent in
  `cts-log-viewer.stories.js`, e.g. *"Embeds a live cts-log-viewer that polls
  /api/log; the polled DOM never settles for capture — behavior test only,
  static stories cover the visuals."*
- Leave `AllTestsPassed` and `RunTestEvent` unchanged — they do not embed the
  viewer and stay snapshotted (R3).

**Patterns to follow:**
- `pollingStoryParameters` and its rationale comment in
  `src/main/resources/static/components/cts-log-viewer.stories.js` (lines ~23–31)
  — match the exact `chromatic: { disableSnapshot: true }` shape and comment tone.

**Technical design** (directional, not a spec):

```javascript
export const PlanToLogDetail = {
  parameters: {
    // Embeds a live cts-log-viewer that polls /api/log; the polled DOM
    // never settles for Chromatic capture (frozen clock leaves timers
    // running). Behavior test only — see pollingStoryParameters in
    // cts-log-viewer.stories.js. test-storybook still runs the play function.
    chromatic: { disableSnapshot: true },
    msw: {
      handlers: [
        /* unchanged */
      ],
    },
  },
  render: () => { /* unchanged */ },
  async play({ canvasElement, step }) { /* unchanged */ },
};
```

**Test scenarios:** `Test expectation: none -- config-only story parameter, no
new runtime behavior. Behavior coverage is the unchanged `play` function,
exercised by `npm run test-storybook`.`

**Verification:**
- `npm run test-storybook` (from `frontend/`) still runs and passes
  `Flows/Test Lifecycle` → `PlanToLogDetail` — the play function executes,
  confirming the snapshot opt-out did not disable behavior coverage (R2).
- `npm run test:ci` (from `frontend/`) passes — format, lint, type-check,
  lint:lit-analyzer, etc. all green (R4).
- The next Chromatic build no longer lists `plan-to-log-detail` under
  `Flows/Test Lifecycle` as a snapshotted story / no longer reports it as a
  spurious change (R1). This is observed on the CI Chromatic build, not locally
  — execution-time confirmation, since the diff only manifests in the hosted
  VRT run.
- `AllTestsPassed` and `RunTestEvent` still appear as snapshotted stories in the
  Chromatic build (R3).

---

## Scope Boundaries

**In scope:**
- The single `PlanToLogDetail` export in `test-lifecycle-flow.stories.js`.

**Deferred to Follow-Up Work** (explicitly out per the confirmed "just the
offender" scope):
- Auditing `cts-badge.stories.js` and `cts-log-entry.stories.js` (which
  reference `cts-log-viewer`) for any incidental live/polling embed that also
  snapshots non-deterministically.
- Extracting a shared `liveDataStoryParameters` constant and a documented
  convention (README rule) so future polling stories opt out by default.
- A lint/CI guard that fails when a snapshotted story embeds a live-polling
  component without `disableSnapshot`.
- Adding a deterministic static twin (e.g. `PlanToLogDetailStatic`) to recover
  a Chromatic snapshot of the composed plan→log layout. Accepted trade-off:
  this change drops that composed-layout snapshot; component-level visuals
  remain covered by the standalone `cts-log-viewer` / `cts-log-detail-header` /
  `cts-plan-modules` stories.

---

## Risks & Dependencies

- **Low risk.** Single story-parameter addition; no production code, no shared
  module, no CI config touched. `chromatic.config.json` and the `.gitlab-ci.yml`
  `chromatic` job are unchanged.
- **Coverage trade-off** (noted above): the composed plan→log layout is no
  longer pixel-snapshotted. Mitigated by isolated component snapshots and the
  retained play-function behavior test; recorded as a deliberate, revisitable
  decision under Deferred to Follow-Up Work.
- **Confirmation timing:** R1 can only be verified on a hosted Chromatic build,
  not from a local `test-storybook` run. The local gates confirm the story
  still works; the absence of the spurious diff is confirmed on the next CI
  Chromatic build.
