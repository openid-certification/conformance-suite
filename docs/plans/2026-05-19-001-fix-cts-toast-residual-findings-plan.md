---
title: "fix: Resolve cts-toast cross-page residual review findings"
type: plan
status: active
created: 2026-05-19
branch: feat/redesign
depth: standard
origin: docs/residual-review-findings/2026-05-19-cts-toast-cross-page-379767a39.md
parent_plan: docs/plans/2026-05-18-011-feat-wire-cts-toast-cross-page-plan.md
board: docs/plans/2026-05-18-006-orphan-components-wire-up-board.md
---

# fix: Resolve cts-toast cross-page residual review findings

## Summary

The cts-toast cross-page wiring landed in commits ab0900918 (feat) + 379767a39 (review autofix). ce-code-review surfaced 10 findings; the 6 `safe_auto` ones were absorbed into 379767a39, leaving 4 residual findings (#7-#10) that the autofix gate held back as `gated_auto` or `manual`.

This plan closes those four — a defensive null guard, a cross-page test-coverage sweep, an extra Storybook story, and an explicit time-coupling note on the upload e2e — without re-litigating the cross-page wiring itself.

## Problem Frame

- **Today:** `<cts-toast-host>` is mounted on 10 pages and `window.ctsToast(...)` is the public API. Coverage and defensiveness gaps remain:
  - `CtsToastHost.getOrCreate()` calls `document.body.appendChild(host)` unconditionally — a future `<head>`-time caller would `TypeError`.
  - Only `upload.spec.js` asserts the host's presence; a silent removal from any of the other 9 pages would not fail a test.
  - The `ErrorKind` story exercises `kind="error"` statically; no story exercises the error kind through `window.ctsToast` (a refactor dropping `kind` from `CtsToastHost.show`'s option propagation would not be caught).
  - The upload-toast e2e waits 6s for auto-dismiss, hard-coupling to the 5000ms default duration.
- **Target:** Each residual finding is closed with the smallest defensible fix.
- **Why:** The residuals are already documented in `docs/residual-review-findings/2026-05-19-cts-toast-cross-page-379767a39.md` — the autofix gate explicitly held them back because each needed a per-finding design call. This plan makes those calls.

## Requirements

- R1. `CtsToastHost.getOrCreate()` does not throw when called before `<body>` parses. (Finding #7)
- R2. Every wired page's e2e spec asserts `cts-toast-host` is present exactly once. (Finding #8)
- R3. Storybook covers `kind: "error"` through the `window.ctsToast` path. (Finding #9)
- R4. The upload e2e's 6s auto-dismiss assertion carries an explicit, durable comment documenting the time coupling and the deferred option-B alternative. (Finding #10)
- R5. No regressions: existing tests stay green; the smoke-test upload e2e and existing Storybook play functions still pass.

## Scope Boundaries

**In scope:**
- One null guard in `src/main/resources/static/components/cts-toast.js` (Finding #7).
- A presence assertion (`await expect(page.locator("cts-toast-host")).toHaveCount(1);`) added to at least one existing test in each of the 9 other wired specs (Finding #8). Specs: `home.spec.js`, `login.spec.js`, `logs.spec.js`, `plans.spec.js`, `plan-detail.spec.js`, `log-detail.spec.js`, `running-test.spec.js`, `schedule-test.spec.js`, `tokens.spec.js`.
- One new Storybook story `ViaWindowApiError` in `src/main/resources/static/components/cts-toast.stories.js` (Finding #9).
- An explicit docblock-style comment in `frontend/e2e/upload.spec.js` documenting the time coupling and deferring option B (Finding #10).

**Out of scope:**
- Option B for Finding #10 (plumb `toastDurationOverride` into the `cts-image-uploaded` event detail). Deferred as a follow-up — production code stays clean for now; if CI wall-clock becomes a bottleneck across multiple toast e2es, revisit.
- Migrating any other ad-hoc notification path. The cross-page wiring plan's R7 smoke-test migration is sufficient.
- The advisory items in the residual findings doc (agent-native obs #2, maintainability M3, learnings #1-5) — explicitly marked "no action required".
- The pre-existing `cts-toast.js:238` `title` property shadow — unchanged by this plan.

### Deferred to Follow-Up Work
- **Option B for Finding #10.** Plumb a `toastDurationOverride` into the `cts-image-uploaded` event handler so the upload e2e auto-dismiss completes in <1s. Worth doing if multiple future toast-driven e2es accumulate similar 5s waits.
- **Post-dismiss history API on `CtsToastHost`** (agent-native obs #2). Revisit if a future agent workflow needs to read dismissed toast content.

---

## Implementation Units

### U1. Add `document.body` null guard to `CtsToastHost.getOrCreate()`

- **Goal:** Defer the host append when `<body>` is not yet present, so `getOrCreate()` is safe to call from a `<head>` inline script or a synchronous module evaluation path.
- **Requirements:** R1.
- **Dependencies:** None.
- **Files:**
  - `src/main/resources/static/components/cts-toast.js` (modify the `static getOrCreate()` method around line 200)
  - `src/main/resources/static/components/cts-toast.stories.js` (add a play-function story exercising the null-body fallback path)
- **Approach:**
  - Replace the unconditional `document.body.appendChild(host)` with a `document.body ?` branch. When `<body>` is `null`, queue a one-shot `DOMContentLoaded` listener to perform the append.
  - The function still returns the host element synchronously — only the actual DOM insertion is deferred. Callers that do not immediately read child counts are unaffected; pages already mount the host explicitly, so this path is only the fallback.
  - The existing JSDoc on `getOrCreate()` already calls out "auto-creation is a degraded fallback, not the nominal path" — extend it with one sentence noting the `<body>`-not-yet-parsed deferral.
- **Patterns to follow:**
  - The `{ once: true }` listener option is already used elsewhere in `cts-toast.js` (the auto-dismiss `setTimeout` cleanup). Reuse the same convention.
- **Test scenarios:**
  - Storybook play-function: call `CtsToastHost.getOrCreate()` after deleting `document.body` (via a wrapper that nulls it briefly), assert no throw and that the returned host is an element. **Note:** if test-environment isolation prevents reliably nulling `document.body` mid-play-function without breaking other tests, drop to a smaller assertion — call `getOrCreate()` twice and assert both calls return the same host node (the idempotency claim the JSDoc already makes). Implementer's call during U1.
  - Existing `HelperAutoDismiss` and `ViaWindowApi` stories still pass — the deferred branch must not regress the nominal path.
- **Verification:**
  - `cd frontend && npm run test:ci` is green.
  - Story `run-story-tests` for `cts-toast` passes.

---

### U2. Sweep 9 e2e specs to assert `cts-toast-host` presence

- **Goal:** Each of the 9 specs covering pages that mount `<cts-toast-host>` (other than `upload.spec.js`, which already asserts it) gains a single `toHaveCount(1)` assertion against the host selector. A silent accidental removal of the mount from any wired page now fails a test.
- **Requirements:** R2.
- **Dependencies:** None (independent of U1).
- **Files:**
  - `frontend/e2e/home.spec.js`
  - `frontend/e2e/login.spec.js`
  - `frontend/e2e/logs.spec.js`
  - `frontend/e2e/plans.spec.js`
  - `frontend/e2e/plan-detail.spec.js`
  - `frontend/e2e/log-detail.spec.js`
  - `frontend/e2e/running-test.spec.js`
  - `frontend/e2e/schedule-test.spec.js`
  - `frontend/e2e/tokens.spec.js`
- **Approach:**
  - In each spec, pick the simplest existing test that already calls `page.goto(...)` against the wired page (typically the first happy-path test in the file). Add `await expect(page.locator("cts-toast-host")).toHaveCount(1);` immediately after the goto, mirroring the pattern in `upload.spec.js:210`.
  - Do NOT add a new test file or new top-level test. The presence check rides on an existing test to avoid adding 9 new test runs.
  - Skip specs that are already in the project memory's pre-existing-flakes list **for the test being touched**: review `feedback_e2e_pre_existing_failures_2026_05_18.md` before editing each file. If the only happy-path test in a spec is already a pre-existing flake, pick a more stable test in the same spec, or add a tiny `test("mounts cts-toast-host on page load", ...)` standalone block. Document the choice per-spec in the commit message.
  - Use the existing setup helpers (`setupFailFast`, `setupCommonRoutes`, etc.) — do not introduce new fixtures.
- **Patterns to follow:**
  - `frontend/e2e/upload.spec.js:210` is the canonical pattern: a single `toHaveCount(1)` assertion after the goto.
- **Test scenarios:**
  - Each modified spec, when run individually, still passes.
  - Each new assertion fails if the corresponding `<cts-toast-host>` mount is removed from the page (verify by temporarily commenting out one mount during local development; revert before commit).
- **Verification:**
  - `cd frontend && npm run test:e2e` runs the same 105-passing/19-pre-existing-failing baseline; the 9 modified specs contribute 9 new passing assertions, no new failures.
  - Each new assertion line references the page mount via the same `cts-toast-host` selector — no per-page variation.

---

### U3. Add `ViaWindowApiError` Storybook story

- **Goal:** A new play-function story exercises `kind: "error"` through the `window.ctsToast` global path, asserting the rust left-rule and the `close-circle` glyph appear via the global API (not just static HTML).
- **Requirements:** R3.
- **Dependencies:** None (independent of U1/U2).
- **Files:**
  - `src/main/resources/static/components/cts-toast.stories.js` (add new story after `ViaWindowApi`)
- **Approach:**
  - Mirror the structure of `ViaWindowApi` but pass `kind: "error"` and assert the rust-400 left-rule (`--rust-400` in the inline `style`) and `close-circle` glyph — matching what `ErrorKind` already asserts statically.
  - Use `duration: 0` (and call `.dismiss()` at the end) rather than auto-dismiss, so the test does not race a 50ms timer; the existing `ViaWindowApi` story already covers the auto-dismiss-with-window-api path for the `ok` kind. Keep this story focused on the kind-propagation claim.
  - Call `resetHost()` at the start and end of the play function, mirroring the existing `ViaWindowApi` / `Persistent` convention.
- **Patterns to follow:**
  - `cts-toast.stories.js` `ErrorKind` (static error assertions) + `ViaWindowApi` (window-api invocation) — the new story combines both.
- **Test scenarios:**
  - The play function calls `window.ctsToast({ title, kind: "error", duration: 0 })` and asserts:
    1. The returned element is a `<cts-toast>` inside the auto-created host.
    2. The card's inline style contains `--rust-400`.
    3. The `cts-icon` name is `close-circle`.
    4. Manual `.dismiss()` removes the toast and fires `cts-toast-dismiss` on the host.
- **Verification:**
  - `run-story-tests` for `cts-toast` is green and the new story appears in the Storybook canvas.

---

### U4. Document time-coupling on upload e2e auto-dismiss (Option A)

- **Goal:** Make the time-coupling tradeoff explicit and durable in `upload.spec.js` so a future maintainer reading the test does not need to reconstruct the choice from PR history.
- **Requirements:** R4.
- **Dependencies:** None (independent of U1/U2/U3).
- **Files:**
  - `frontend/e2e/upload.spec.js` (modify the comment block above `toHaveCount(0, { timeout: 6000 })` at line ~238)
- **Approach:**
  - Replace the existing slack-rationale comment with a slightly expanded docblock that:
    1. Names the coupling explicitly: this test depends on the 5000ms `cts-toast` default duration.
    2. Notes the wall-clock cost (~5.5s on every CI run).
    3. Records the decision to accept the cost (option A) rather than plumb a test-only `toastDurationOverride` into the production event handler (option B), with a one-line trigger condition for revisiting ("if multiple toast-driven e2es accumulate similar 5s waits, revisit option B").
    4. Cross-references the residual findings doc (`docs/residual-review-findings/2026-05-19-cts-toast-cross-page-379767a39.md` #10) so the decision context is one search away.
  - Do NOT change the timeout value or the assertion shape. The behavior is unchanged; only the documentation is.
- **Patterns to follow:**
  - The existing comment in `upload.spec.js:238-242` is the baseline.
- **Test scenarios:** none — pure documentation change, no behavioral surface.
- **Verification:**
  - `cd frontend && npm run test:e2e -- upload.spec.js` still passes.
  - The comment block reads as a deliberate maintainer decision, not a TODO.

---

## Open Questions

- **U1 test scenario fallback.** If nulling `document.body` mid-play-function breaks isolation across other Storybook tests, drop to the idempotency assertion (call `getOrCreate()` twice, assert same node). The plan accepts either outcome; the unit succeeds as long as the null-body branch is exercised or, failing that, the idempotency claim is verified.
- **U2 per-spec stability.** Some target specs (per `feedback_e2e_pre_existing_failures_2026_05_18.md`) have pre-existing flakes. The plan defers the per-spec choice ("ride an existing stable test" vs "add a tiny standalone test") to implementation time. Whichever path is taken, the commit message should call it out per-spec for traceability.

## Verification

- `cd frontend && npm run test:ci` is green (lint + format + type-check + lit-analyzer).
- `cd frontend && npm run test:e2e` matches the pre-existing baseline (105 passing, 19 pre-existing failures, 1 pre-existing flaky) plus the new presence assertions on the 9 swept specs.
- `run-story-tests` for `cts-toast` is green with the new `ViaWindowApiError` story.
- Manual: open `index.html` in a dev server, confirm `window.ctsToast({ title: "test" })` still works (sanity check that U1's null-guard refactor did not regress the nominal path).
- The residual findings doc (`docs/residual-review-findings/2026-05-19-cts-toast-cross-page-379767a39.md`) gains a closing addendum noting each finding's resolution commit SHA after this plan lands.
