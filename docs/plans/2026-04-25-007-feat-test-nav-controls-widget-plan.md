---
title: "feat: Group test navigation controls into single widget (R21)"
type: feat
status: shipped
date: 2026-04-25
origin: docs/brainstorms/2026-04-13-cts-ux-improvement-plan-requirements.md
---

> **Shipped 2026-05-12.** `cts-test-nav-controls` is in production via
> `cts-log-detail-header._renderTestNavControlsRow` (slim mode); 14 Storybook
> stories cover its full contract; page wiring lives in
> `js/log-detail.js → fetchPlanModules`. The Repeat button was de-duplicated
> against the sticky status bar's primary action and is no longer emitted by
> the slim consumption — captured as a design decision in
> `cts-test-nav-controls.js` and the brainstorm R21 row. The originally-planned
> U3 ("wire into legacy `templates/logHeader.html`") was superseded by the
> U1–U8 log-viewer flag-flip (2026-04-27) which deleted that template; live
> wiring now lives directly in the Lit triad page.

# feat: Group test navigation controls into single widget (R21)

## Overview

R21 from the CTS UX improvement brainstorm calls for grouping the per-test navigation controls (back to plan, repeat current test, continue to next test, plus a progress indicator) into a single, semantically-grouped widget on the test-detail page (`log-detail.html`). Today these controls are scattered through the legacy Mustache template `templates/logHeader.html` as four independent `cts-button`s mixed in with publish/share/upload actions, with no visual or semantic grouping and no "X of N" position indicator.

This plan introduces a new Lit component `cts-test-nav-controls` that bundles back / repeat / continue / progress into one cohesive widget, wires it into the legacy `templates/logHeader.html` (so the change reaches users on the live `log-detail.html` page), and updates the existing `cts-log-detail-header` Storybook component to consume the same widget for forward consistency.

R20 (progress indicator "6 of 30") is naturally bundled here because R21 explicitly names "progress" as one of the four grouped controls. The brainstorm lists R20 and R21 together in the "Phase 2: Running Tests" execution slate (see origin) — combining them in one widget is the lowest-friction realization of both.

---

## Problem Frame

When a user runs a multi-module test plan, they navigate from one module to the next using three separate buttons in the test-detail header — `Repeat Test`, `Return to Plan`, `Continue Plan` — interleaved with unrelated actions (`Upload Images`, `View Config`, `Download Logs`, `Publish`, `Private link`). There is no:

- **Visual grouping**: nav controls and publish/upload/share controls are stacked in the same `.log-header-controls` flex column.
- **Semantic grouping**: no `role="group"` / `aria-label` to tell assistive tech "these three controls navigate the test plan".
- **Position context**: the user has no idea whether they are on module 6 of 30 or module 28 of 30. They must click `Return to Plan` to find out.
- **Progress affordance**: there is no progress bar, dot indicator, or count. The "Continue Plan" button does not even disable on the last module — it just stays clickable.

This costs P1 (RP developers) and P2 (test automation engineers) attention and orientation throughout long test plans (origin J: "Navigate between test modules efficiently"; "Track progress through a test plan").

---

## Requirements Trace

- R1. Group `Return to Plan`, `Repeat Test`, `Continue Plan` into one labelled cluster on the test-detail page (origin R21).
- R2. Show "X of N" position indicator inside the cluster when the test belongs to a plan (origin R20, bundled per R21's "progress" requirement).
- R3. Hide / disable `Continue Plan` when the current module is the last one in the plan (a corollary of R20 — the user must be able to tell when they are done).
- R4. Hide the entire navigation cluster when the test does not belong to a plan (e.g., ad-hoc test runs).
- R5. Preserve existing keyboard shortcuts: `Ctrl/Cmd+Shift+X` repeats, `Ctrl/Cmd+Shift+U` continues — they must still work after the refactor.
- R6. Preserve the existing public/readonly view: hide `Repeat Test` and `Continue Plan` when `public=true` (`readonly` mode), keep `Return to Plan` visible.
- R7. Land the change in the live page (`log-detail.html` via `templates/logHeader.html`) so users see it, not only Storybook.
- R8. Mirror the same widget inside `cts-log-detail-header` so the (currently Storybook-only) component stays a faithful blueprint for the eventual all-component log-detail page.

**Origin actors:** P1 (RP developer), P2 (test automation engineer)
**Origin flows:** Phase 2 — Running Tests
**Origin acceptance examples:** none specified at the brainstorm tier; AE-style acceptance is encoded in the unit-level test scenarios below.

---

## Scope Boundaries

- **Not** redesigning the rest of the test-detail header (config viewer, publish/share controls, upload images) — those remain as-is.
- **Not** wiring up `cts-log-detail-header` onto the live `log-detail.html` page. That migration is separate and out of scope for R21.
- **Not** introducing a per-module status timeline (e.g., dots showing pass/fail for each completed module). That is a richer R20 follow-up.
- **Not** changing the `/api/plan/{planId}` API or how the next-module URL is computed — only the UI layer.
- **Not** changing the keyboard shortcut bindings — only making sure they still work.
- **Not** addressing R22 (remove "no longer running" message) or R23 (delete-plan prominence). Those are separate items in the same brainstorm.

---

## Context & Research

### Relevant Code and Patterns

- `src/main/resources/static/components/cts-running-test-card.js` — the component cited in R21's "Status" note; bundles status badge + progress fill in one card. Its scoped-CSS pattern (`STYLE_TEXT` constant + `injectStyles()` + `createRenderRoot() { return this; }` for light DOM) is the canonical pattern for new CTS widgets.
- `src/main/resources/static/components/cts-log-detail-header.js` — already-built (Storybook-only) component that already has Repeat Test + Return to Plan as `_handleRepeatTest` / `cts-link-button` to plan-detail. Missing: Continue Plan button, progress, semantic grouping. The `cts-test-nav-controls` widget should slot into this component so a future migration to the all-component log-detail page picks it up automatically.
- `src/main/resources/static/templates/logHeader.html` — the live Mustache template that renders into `log-detail.html`. Today contains the legacy `reloadBtn`, `planBtn`, `nextPlanBtn` as three independent `cts-button`s. The shipping change point.
- `src/main/resources/static/log-detail.html` — the page that currently:
  - Wires `nextPlanBtn.onclick` after fetching `/api/plan/{planId}` and locating `thisModuleIndex` in `planData.modules` (lines 974–1077).
  - Toggles `nextPlanBtn.style.display = ''` only when a next module exists (line 1075).
  - Targets `#nextPlanBtn button` and `#reloadBtn button` from the keyboard-shortcut handler (lines 1929–1942).
  - Calls `document.getElementById('planBtn').onclick = …` for the back button (line 958).
- `src/main/resources/static/components/cts-button.js` / `cts-link-button.js` — the design-system primitives used. `cts-button` emits a bubbling `cts-click` event; `cts-link-button` renders a real `<a href>` that the keyboard-shortcut handler can target.

### Institutional Learnings

- `docs/solutions/best-practices/playwright-route-pattern-with-fallback-is-safe-2026-04-17.md` — relevant for the e2e test fixture wiring; the existing `frontend/e2e/log-detail.spec.js` (verified to exist via earlier audit) already uses `setupFailFast()` first.
- Repo memory `feedback_storybook_interaction_tests.md` — every CTS Web Component must have Storybook play-function tests. `cts-test-nav-controls` therefore needs full play coverage from the first commit.
- Repo memory `feedback_component_conventions.md` — no dynamic class concatenation; stories colocated with components. The widget will follow the `STYLE_TEXT` lookup-table pattern used by `cts-running-test-card`.
- Repo memory `feedback_jsdoc_annotations.md` — every cts-* component carries `@property` JSDoc annotations and `@fires` declarations. The widget will mirror `cts-running-test-card`'s docblock style.
- Repo memory `feedback_lit_marker_snapshot_normalization.md` — relevant when this widget is e2e-tested with `getNormalizedInnerHTML` later.

### External References

- None needed — this is an internal-pattern refactor with no new framework decisions.

---

## Key Technical Decisions

- **Widget granularity: a dedicated `cts-test-nav-controls` widget rather than expanding `cts-log-detail-header`.** Rationale: `cts-log-detail-header` is not yet wired into `log-detail.html`. Putting nav controls inside it would not reach users. A standalone widget can be dropped into the live Mustache template *and* into the (Storybook-only) header component, so the refactor lands once and then both surfaces consume it.
- **Progress shape: textual "X of N" with a `<progress>`-style bar.** The `cts-running-test-card` pattern (`<div role="progressbar">` + 8px track + orange fill) is the design-system precedent; reuse it. Brainstorm R20 asks for "6 of 30" wording — render *both* the count and a thin track so users get glance-readability on long plans.
- **Eventing contract: `cts-back`, `cts-repeat`, `cts-continue` events with `{ detail: { testId, planId } }`.** Mirrors the existing `cts-repeat-test`, `cts-download-log` patterns in `cts-log-detail-header` / `cts-running-test-card`. Keeps the page-level glue thin.
- **`Return to Plan` stays a real `<a href>`** (using `cts-link-button`) so middle-click / Cmd-click open in a new tab, matching the legacy `planBtn` behaviour and accessibility expectations. The `cts-back` event is fired in addition to the navigation, allowing test instrumentation without breaking native link semantics.
- **`Continue Plan` rendering: render only when `nextEnabled` is true** (instead of always rendering and toggling `disabled`). This matches the legacy behaviour in `log-detail.html` (`#nextPlanBtn` is hidden until a next module exists) and avoids a confusing always-visible-but-disabled button on the last module. Progress text and bar still render and read "30 of 30" so the user knows they are done.
- **Keyboard shortcuts stay in `log-detail.html`.** They target `#nextPlanBtn button` / `#reloadBtn button` today. We expose `data-testid="continue-btn"` / `data-testid="repeat-btn"` *and* keep stable IDs on the inner buttons so the existing shortcut selectors still match. (See U3 below for the exact selectors.)
- **Light DOM, scoped CSS via `injectStyles()`.** Same pattern as `cts-running-test-card` and `cts-log-detail-header`. Keeps theming and `cts-icon` / `cts-button` slot rendering simple.

---

## Open Questions

### Resolved During Planning

- **Q: Should the widget include the "Edit configuration" button (which sometimes acts as a "fork plan" navigation)?** A: No. Edit configuration is a config-management action, not test-plan navigation. R21 explicitly enumerates back / repeat / continue / progress.
- **Q: Should we replace `cts-log-detail-header`'s existing inline Repeat / Return-to-Plan buttons with the widget too?** A: Yes — the second-pass U4 below. Keeps both surfaces consistent.
- **Q: Where does the progress count come from?** A: From the `/api/plan/{planId}` response that `log-detail.html` already fetches (`planData.modules` + `thisModuleIndex`). The page passes `currentIndex` (0-based) and `totalCount` as widget properties.

### Deferred to Implementation

- The exact CSS for the cluster border / spacing — refine during implementation against the existing `.log-header-controls` flex column. Likely a `border-radius: var(--radius-3)` panel with `--space-3` internal gap.
- Whether to render the progress bar above or below the "X of N" text — implementer's call after running the Storybook stories.

---

## Implementation Units

- U1. **Create `cts-test-nav-controls` Lit component**

**Goal:** New `cts-test-nav-controls.js` Lit component that renders a labelled cluster of back / repeat / continue buttons plus a progress indicator. Light-DOM, scoped CSS via `injectStyles()`, JSDoc `@property` + `@fires` annotations.

**Requirements:** R1, R2, R3, R4, R6.

**Dependencies:** none.

**Files:**
- Create: `src/main/resources/static/components/cts-test-nav-controls.js`

**Approach:**
- Properties: `testId: string`, `planId: string`, `currentIndex: number` (0-based), `totalCount: number`, `nextEnabled: boolean`, `readonly: boolean`.
- Render structure: outer `<div role="group" aria-label="Test plan navigation">` containing (a) progress block (`<div role="progressbar" aria-valuenow="…" aria-valuemax="…">` + count text "Module X of N") and (b) button row with cts-link-button (Return to Plan) + cts-button (Repeat Test) + cts-button (Continue Plan, only when `nextEnabled`). Use lookup tables (no dynamic class concatenation per `feedback_component_conventions.md`).
- Hide `Repeat Test` and `Continue Plan` when `readonly` is true; keep `Return to Plan` visible.
- Hide the entire group when `planId` is falsy.
- Emit `cts-back`, `cts-repeat`, `cts-continue` bubbling CustomEvents with `{ testId, planId }` detail.
- Stable inner-button IDs `id="reloadBtn"` and `id="nextPlanBtn"` are *not* set here — the page wraps the widget; we expose `data-testid="repeat-btn"` and `data-testid="continue-btn"` plus a `data-button-id` hook so the page can wire the legacy IDs in U3 without coupling the widget to legacy selectors.

**Patterns to follow:**
- `src/main/resources/static/components/cts-running-test-card.js` — `STYLE_ID` / `STYLE_TEXT` / `injectStyles()` / `createRenderRoot() { return this; }` / lookup tables / event-dispatch pattern.
- `src/main/resources/static/components/cts-log-detail-header.js` — JSDoc style with `@property`, `@fires`, full TestInfo typedef.

**Test scenarios:**
- Happy path: with `planId="abc"`, `currentIndex=5`, `totalCount=30`, `nextEnabled=true` — renders the cluster with role="group", an `aria-label="Test plan navigation"`, the text "Module 6 of 30" (1-based for the user), all three buttons.
- Happy path: clicking `Repeat Test` fires a bubbling `cts-repeat` event whose `detail.testId` matches the prop.
- Happy path: clicking `Continue Plan` fires a bubbling `cts-continue` event whose `detail.planId` matches the prop.
- Happy path: `Return to Plan` is rendered as a real `<a>` with `href="plan-detail.html?plan=…"` (encoded).
- Edge case: `nextEnabled=false` — the Continue Plan button is *not* rendered; the progress block still shows "Module 30 of 30".
- Edge case: `planId=""` (or null) — the entire `<div role="group">` does not render (returns `nothing`), so an ad-hoc test page shows nothing.
- Edge case: `readonly=true` — Repeat Test and Continue Plan are not rendered; Return to Plan still rendered.
- Edge case: `currentIndex=0`, `totalCount=1` — renders "Module 1 of 1", `Continue Plan` not rendered (assuming `nextEnabled=false` is passed by caller).
- Edge case: progress bar `aria-valuenow` reflects 1-based current ("6") and `aria-valuemax` reflects total ("30"); `aria-valuemin="1"`.
- Error path: `currentIndex >= totalCount` — clamp progress at `aria-valuenow=totalCount` instead of overflowing.

**Verification:**
- Component registers as `cts-test-nav-controls` and renders without runtime warnings.
- All test scenarios pass in Storybook play tests.

---

- U2. **Storybook stories with play-function tests for `cts-test-nav-controls`**

**Goal:** Colocated `cts-test-nav-controls.stories.js` covering the variants in U1 with userEvent-driven play tests, per `feedback_storybook_interaction_tests.md`.

**Requirements:** R1, R2, R3, R6 (verification).

**Dependencies:** U1.

**Files:**
- Create: `src/main/resources/static/components/cts-test-nav-controls.stories.js`

**Approach:**
- Stories: `MidPlan` (currentIndex=5, totalCount=30, nextEnabled=true), `LastModule` (currentIndex=29, totalCount=30, nextEnabled=false), `FirstModule` (currentIndex=0, totalCount=30, nextEnabled=true), `Readonly` (readonly=true), `NoPlan` (planId=""), `SingleModulePlan` (currentIndex=0, totalCount=1, nextEnabled=false).
- Each story has a `play({ canvasElement })` that asserts the rendered DOM and, for interactive stories, fires the relevant click and asserts the bubbling event.
- Use `storybook/test`'s `fn()` to spy on the bubbling event listener attached to `canvasElement`.
- Mirror the inner-button click pattern from `cts-log-detail-header.stories.js` (`canvasElement.querySelector('[data-testid="…"] button')`) — clicking the `cts-button` host bypasses Lit's `@cts-click` handler.

**Patterns to follow:**
- `src/main/resources/static/components/cts-log-detail-header.stories.js` (esp. `RepeatTest` story for event-fn pattern, lines 226-243).
- `src/main/resources/static/components/cts-running-test-card.stories.js` for status/data-fixture style.

**Test scenarios:**
- Happy path: `MidPlan` story renders all three buttons, the "Module 6 of 30" text, and the progressbar with `aria-valuenow="6"`.
- Happy path: clicking the `Repeat Test` inner `<button>` fires `cts-repeat` once; spy receives the testId.
- Happy path: clicking the `Continue Plan` inner `<button>` fires `cts-continue` once; spy receives the planId.
- Edge case: `LastModule` story has no Continue Plan button and shows "Module 30 of 30".
- Edge case: `Readonly` story has only the Return to Plan link visible.
- Edge case: `NoPlan` story renders an empty fragment (no `role="group"` element in the DOM).

**Verification:**
- `cd frontend && ./node_modules/.bin/storybook test` (or whichever command the repo uses) runs play tests and they pass.

---

- U3. **Wire `cts-test-nav-controls` into `templates/logHeader.html`**

**Goal:** Replace the legacy `planBtn`, `reloadBtn`, `nextPlanBtn` in `templates/logHeader.html` with a single `<cts-test-nav-controls>` element, and update `log-detail.html` JS to set the widget's `currentIndex` / `totalCount` / `nextEnabled` props from `planData` and to listen for `cts-back` / `cts-repeat` / `cts-continue` events instead of attaching `onclick` to the legacy IDs.

**Requirements:** R1, R2, R3, R4, R5, R6, R7.

**Dependencies:** U1.

**Files:**
- Modify: `src/main/resources/static/templates/logHeader.html`
- Modify: `src/main/resources/static/log-detail.html`

**Approach:**
- In `templates/logHeader.html`, replace the three legacy buttons with a single `<cts-test-nav-controls id="testNavControls" data-testid="test-nav-controls"></cts-test-nav-controls>` placed at the top of the `.log-header-controls` block, *above* the upload/config/download/publish buttons (which keep their existing scattered layout). Ensure the existing `<% if (!readonly) { %>` Mustache gate still controls whether `repeatable=true` or `readonly=true` is set on the widget. Pass `readonly` as an attribute via the Mustache scope.
- In `log-detail.html`, in the existing `/api/plan/{planId}` `.then((planData) => { … })` block (around lines 974-1077):
  - After computing `thisModuleIndex`, look up the widget: `const nav = document.getElementById('testNavControls');`
  - Set props: `nav.testId = data.testId; nav.planId = data.planId; nav.currentIndex = thisModuleIndex; nav.totalCount = planData.modules.length; nav.nextEnabled = thisModuleIndex >= 0 && thisModuleIndex < (planData.modules.length - 1); nav.readonly = readonly;`
  - Replace the `document.getElementById('nextPlanBtn').onclick = …` block with a `nav.addEventListener('cts-continue', (evt) => { /* same body */ })`.
  - Replace `document.getElementById('planBtn').onclick = …` with a `nav.addEventListener('cts-back', (evt) => { /* same body */ })` — the link still navigates natively because it's an `<a href>`, but the listener stays for instrumentation parity.
  - Find the existing `reloadBtn` `onclick` wiring (search log-detail.html for `reloadBtn`) and switch to listening on the widget for `cts-repeat`. Keep the implementation identical so the existing keyboard-shortcut path (`#reloadBtn button`) still works — we keep the legacy `id="reloadBtn"` on the inner button by exposing it via the widget's `repeatButtonId` property, which the page sets to `"reloadBtn"`. Same for `id="nextPlanBtn"` and `nextButtonId="nextPlanBtn"`.
  - **Alternative if exposing inner-button IDs is messy**: update the keyboard-shortcut handler in `log-detail.html` (lines 1917-1944) to query `cts-test-nav-controls [data-testid="repeat-btn"] button` and `cts-test-nav-controls [data-testid="continue-btn"] button`. This is cleaner and is the path U5 should also reflect.
- The cleaner path is the alternative — choose it. The widget exposes `data-testid="repeat-btn"` / `data-testid="continue-btn"`; `log-detail.html`'s keyboard handler updates its selectors accordingly. This avoids leaking legacy IDs into the widget's API.

**Execution note:** Update `log-detail.html` and `templates/logHeader.html` together in a single commit. Verify in the browser that all four behaviours (back, repeat, continue, progress count) work and that both keyboard shortcuts still fire before declaring U3 complete.

**Patterns to follow:**
- Existing `card.addEventListener('cts-download-log', …)` wiring in `running-test.html` (lines 163-168) — same pattern for listening to bubbling widget events.

**Test scenarios:**
- Happy path: e2e — load `log-detail.html?log=…` for a test that belongs to a plan; the widget appears with the correct count.
- Happy path: e2e — clicking `Continue Plan` triggers the `/api/runner` POST and navigates to the new test's URL.
- Happy path: e2e — clicking `Repeat Test` calls `/api/runner` with the same module + variant and navigates.
- Happy path: e2e — clicking `Return to Plan` navigates to `/plan-detail.html?plan=…`.
- Keyboard: e2e — `Cmd+Shift+X` triggers the same `cts-repeat` flow; `Cmd+Shift+U` triggers `cts-continue`.
- Edge case: e2e — load a test with no `planId` → widget is absent from the DOM.
- Edge case: e2e — load a test that is the last module of a plan → widget shows "Module N of N", Continue button absent.
- Edge case: e2e — load with `?public=true` → widget shows only Return-to-Plan link; no Repeat / Continue.

**Verification:**
- `cd frontend && npm run test:e2e -- log-detail` passes (or whichever spec covers the test-detail page).
- Manual: open `log-detail.html` for a test in a multi-module plan, confirm visual grouping, count text, keyboard shortcuts, public view.

---

- U4. **Update `cts-log-detail-header` to use `cts-test-nav-controls`**

**Goal:** Replace the inline Repeat Test button + Return to Plan link inside `cts-log-detail-header` with `<cts-test-nav-controls>`. Re-emit the widget's events as the existing `cts-repeat-test` event so existing consumers and stories don't break.

**Requirements:** R8 (Storybook surface stays consistent with the live widget).

**Dependencies:** U1.

**Files:**
- Modify: `src/main/resources/static/components/cts-log-detail-header.js`
- Modify: `src/main/resources/static/components/cts-log-detail-header.stories.js`

**Approach:**
- Add `currentIndex`, `totalCount`, `nextEnabled` to the component's `static properties` (numbers / boolean). Default them to `null` / `0` / `false`.
- In `_renderActionButtons`, replace the inline `Repeat Test` cts-button and the `Return to Plan` cts-link-button with `<cts-test-nav-controls>` (passing testId, planId, currentIndex, totalCount, nextEnabled, readonly).
- Add internal listeners that re-fire `cts-repeat-test` (and a new `cts-continue-plan`) so existing test scenarios that listen to `cts-repeat-test` keep working. Document the new event with `@fires`.
- Update the `cts-log-detail-header.stories.js` `RepeatTest` and `ReturnToPlan` stories: select `cts-test-nav-controls [data-testid="repeat-btn"] button` instead of `[data-testid="repeat-test-btn"] button`. Add a new `ContinuePlan` story.

**Patterns to follow:**
- The existing `_renderActionButtons` method's nothing/readonly pattern in `cts-log-detail-header.js` (lines 611-678).

**Test scenarios:**
- Happy path: `RepeatTest` story still passes — clicking the inner button fires the existing `cts-repeat-test` event.
- Happy path: `ReturnToPlan` story still passes — the link's `href` still contains `plan-detail.html?plan=`.
- Happy path: new `ContinuePlan` story — clicking the inner Continue button fires a `cts-continue-plan` event with the correct planId.
- Edge case: existing `Public` / `Readonly` story behaviour is preserved (Repeat hidden, Return-to-Plan visible).
- Edge case: when `testInfo.planId` is missing (ad-hoc test), the widget renders nothing and no Repeat/Return buttons appear. Adjust the existing `RepeatTest` story's fixture if it currently relies on a missing planId.

**Verification:**
- `cd frontend && ./node_modules/.bin/storybook test` passes including the updated and new stories.

---

- U5. **E2E test for the wired widget on `log-detail.html`**

**Goal:** Add (or update) a Playwright e2e spec that exercises the widget end-to-end on `log-detail.html` using mocked API responses, per `frontend/README.md` conventions.

**Requirements:** R1, R2, R3, R4, R5, R6, R7 (verification).

**Dependencies:** U3.

**Files:**
- Modify or create: `frontend/e2e/log-detail.spec.js` (extend; do not duplicate the file if it already exists).
- Modify: `frontend/e2e/fixtures/` — add a fixture for `/api/plan/{planId}` returning a 30-module plan and a 1-module plan; reuse the existing `/api/info/{testId}` fixture if present.

**Approach:**
- Set up `setupFailFast()` first, then specific routes, before any `page.goto()` (per `feedback_lit_marker_snapshot_normalization.md` and the Playwright-route-pattern learning doc).
- Cover the e2e test scenarios listed in U3.

**Patterns to follow:**
- `frontend/e2e/journeys.spec.js` for cross-page flows.
- `frontend/e2e/helpers/routes.js` for fail-fast registration order.
- Existing log-detail spec if present (search for `frontend/e2e/log-detail.spec.js`).

**Test scenarios:**
- (See U3 — those scenarios are reified here as Playwright `test(...)` blocks.)

**Verification:**
- `cd frontend && ./node_modules/.bin/playwright test e2e/log-detail.spec.js` passes locally.
- The full `cd frontend && npm run test:e2e` suite still passes.

---

## System-Wide Impact

- **Interaction graph:** `log-detail.html` JS depends on `templates/logHeader.html` rendering DOM with stable IDs. Replacing the three legacy IDs (`planBtn`, `reloadBtn`, `nextPlanBtn`) with one widget means the JS contract changes from "find element by ID + onclick" to "find widget + addEventListener on bubbling event". Keyboard-shortcut selectors must be updated in lockstep.
- **Error propagation:** none changed — the underlying `/api/runner` POST and error-handling flow stays identical; only the trigger surface moves.
- **State lifecycle risks:** The widget reads its props once on each `setProperty` call; the page must set props *after* it has fetched both `/api/info/{testId}` and `/api/plan/{planId}`. The existing `Promise.then` chain already establishes that ordering — preserve it.
- **API surface parity:** `cts-log-detail-header` is a Storybook-only consumer today; updating it in U4 keeps it consistent for future migration.
- **Integration coverage:** U5's e2e tests are the primary cross-layer guarantee; U2's Storybook play tests cover the widget in isolation.
- **Unchanged invariants:** the public/readonly view (`?public=true`) keeps its rule "no Repeat, no Continue, yes Return-to-Plan". The `/api/plan/{planId}` shape is untouched. Keyboard shortcuts still fire on `Cmd+Shift+X` / `Cmd+Shift+U`. The publish/share/upload buttons keep their identity, position, and event wiring.

---

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Keyboard shortcut handler in `log-detail.html` breaks because it targets `#nextPlanBtn button` and `#reloadBtn button` by literal selector. | U3 explicitly updates the handler's selectors to query through the widget's `data-testid`. U5 covers this via Playwright keyboard tests. |
| `templates/logHeader.html` is rendered by lodash's Mustache template via `FAPI_UI.logTemplates`; if the widget element is unknown at template-render time (script not yet loaded), it appears as an unstyled inline element. | The widget's module is preloaded via `<script type="module" src="/components/cts-test-nav-controls.js">` in `log-detail.html`'s `<head>`, mirroring the existing module loads (cts-button, cts-link-button, etc.). |
| `cts-log-detail-header` story regressions: the existing `RepeatTest` / `ReturnToPlan` stories' selectors change. | U4 explicitly updates those stories. CI Storybook tests will catch any miss. |
| Continue Plan disabled-vs-hidden choice: legacy code hides the button when no next module exists; some users may have built muscle memory for "always-visible button greys out". | Decision documented in Key Technical Decisions: hide, matching legacy. The progress text "Module N of N" makes the end-of-plan state legible. |
| The widget receives props post-render (after fetch). Lit's reactive update may flicker an empty cluster between first paint and prop assignment. | Render `nothing` until `planId` is set (the widget's "no plan" path). The cluster simply appears once props are set. |

---

## Documentation / Operational Notes

- Add the new component to the Storybook side-nav (automatic via the `default.title` export).
- No DB / config / migration impact.
- No backend code touched — frontend-only change.

---

## Sources & References

- **Origin document:** [docs/brainstorms/2026-04-13-cts-ux-improvement-plan-requirements.md](../brainstorms/2026-04-13-cts-ux-improvement-plan-requirements.md) (R20, R21)
- Related code:
  - `src/main/resources/static/templates/logHeader.html`
  - `src/main/resources/static/log-detail.html`
  - `src/main/resources/static/components/cts-log-detail-header.js`
  - `src/main/resources/static/components/cts-running-test-card.js` (pattern reference)
- Related memory entries: `feedback_storybook_interaction_tests.md`, `feedback_component_conventions.md`, `feedback_jsdoc_annotations.md`.
