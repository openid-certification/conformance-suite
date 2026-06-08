---
title: "fix: Replace plan-detail blocking loading modal with in-page loader"
type: fix
status: active
date: 2026-06-08
plan_depth: lightweight
---

# fix: Replace plan-detail blocking loading modal with in-page loader

## Summary

On initial load, `plan-detail.html` shows a full-screen blocking **modal + dimmed/blurred backdrop overlay** (`#loadingModal`, driven by `FAPI_UI.showBusy()` / `FAPI_UI.hideBusy()`). `log-detail.html` does not do this — it leaves an **in-page loader** in the content flow and fills the resident components in as data arrives. This plan makes `plan-detail.html` load the same way by rendering the shared `cts-loading-state` component inline in the main content area during the initial fetch and revealing the plan grid once the load settles. The `#loadingModal` markup stays in the page because `runTest()` still uses it for the transitional "Creating a new test…" action (the same pattern `log-detail` keeps its modal for `Repeat` / `Continue` / `Publish`).

---

## Problem Frame

`plan-detail.html` (`src/main/resources/static/plan-detail.html`) is a classic-script page. Its bootstrap (`DOMContentLoaded`, lines 759–783) calls `FAPI_UI.showBusy()` immediately, which `.show()`s `#loadingModal` — a `cts-modal` whose `::backdrop` paints `rgba(26,22,17,0.55)` + `backdrop-filter: blur(4px)` (see `src/main/resources/static/components/cts-modal.js`). The whole viewport is dimmed and interaction-blocked until `getPlan()` settles, at which point `.finally()` calls `FAPI_UI.hideBusy()`.

The desired experience is `log-detail.html`'s: the page chrome (breadcrumb, content region) is visible immediately, an unobtrusive **in-page** spinner sits in the content flow, and the components populate without a blocking scrim. `log-detail.js`'s `bootstrap()` never calls `showBusy()` on initial load; its `cts-log-viewer` renders an inline `.logLoading` spinner while `_loading && _entries.length === 0`.

**Scope:** initial page load on `plan-detail.html` only.

---

## Scope Boundaries

**In scope**
- Swap the initial-load blocking modal on `plan-detail.html` for an in-page `cts-loading-state`.
- E2E coverage proving the in-page loader is used and the modal overlay is not shown on load.

**Non-goals**
- `runTest()`'s transitional `FAPI_UI.showBusy('Creating a new test for …')` busy state (lines 602–603) stays as a modal. It is action feedback that immediately navigates to `log-detail.html`, mirroring how `log-detail` keeps its modal for `Repeat` / `Continue` / `Publish` transitions. Changing it is out of scope.
- The `#errorModal`, `#publishModal`, `#certificationPackageModal`, `#reRunTestModal` modals are untouched.
- The `cts-modal` component itself is not modified.

### Deferred to Follow-Up Work
- `running-test.html` uses the same `FAPI_UI.showBusy()`-on-load pattern (`#loadingModal` at lines 58–59). If page-load consistency across the suite is wanted, it can get the same treatment in a separate change. Not part of this fix (the request named `plan-detail.html`).

---

## Key Technical Decisions

1. **Reuse `cts-loading-state`, not a bespoke spinner.** `src/main/resources/static/components/cts-loading-state.js` is the shared loading component already used by `cts-plan-list.js` (line 968: `<cts-loading-state label="Loading test plans">`) and `cts-log-list.js`. It self-imports `cts-spinner`, injects its own head styles once, and has a `:not(:defined)` height-reserving fallback in `css/layout.css` (line 143) — and `plan-detail.html` already links `css/layout.css` (line 16). This satisfies the request's "re-use the same loading component if possible."

2. **Hide the plan grid until load settles; show the loader in its place.** Keeping the grid visible during load would expose empty `cts-plan-header` / `cts-plan-modules` / `cts-plan-actions` shells. Instead the grid starts `hidden` and is revealed when the fetch settles — matching how the modal previously gated the reveal.

3. **Reveal timing = `.finally()` (parity with the old `hideBusy()`).** The current code removes the overlay in `.finally()` on both success and error, exposing the grid behind any error modal. The replacement removes the loader and un-hides the grid in the same `.finally()`, preserving that exact reveal timing and the on-error behavior (error surfaces via `handleApiError`'s `#errorModal` over the revealed page). This keeps the diff minimal and behavior-equivalent.

4. **Explicit `[hidden]` CSS rule is required (gotcha).** The native `[hidden]` attribute sets `display: none` via the UA stylesheet, but the page's author rule `.oidf-plan-detail-grid { display: grid }` (specificity 0,1,0) **overrides** it, so the `hidden` attribute alone will *not* hide the grid. A higher-specificity author rule `.oidf-plan-detail-grid[hidden] { display: none }` (specificity 0,2,0) must be added so the attribute actually hides the grid. Without this rule the grid renders empty components during load and the change is silently ineffective.

5. **Keep `#loadingModal` in the DOM.** `FAPI_UI.showBusy()` (`js/fapi.ui.js:281`) targets `#loadingModal`, and `runTest()` still calls it. Removing the markup would break that transitional state.

---

## Implementation Units

### U1. Replace the initial-load blocking modal with an in-page `cts-loading-state`

**Goal:** On `plan-detail.html` initial load, render `cts-loading-state` inline in the content region instead of opening the blocking `#loadingModal` overlay; reveal the plan grid when the load settles.

**Requirements:** Page loads with an in-page loader (like `log-detail.html`), no modal+overlay; reuse the shared loading component.

**Dependencies:** none.

**Files:**
- `src/main/resources/static/plan-detail.html` (modify)

**Approach:**
1. **Head import** — add `<script type="module" src="/components/cts-loading-state.js"></script>` alongside the other component module imports (near line 27, by `cts-spinner.js`).
2. **Style block** — add `.oidf-plan-detail-grid[hidden] { display: none; }` to the page `<style>` (the gotcha in KTD 4 — without it the `hidden` attribute is overridden by the existing `display: grid` rule).
3. **Markup** — inside `<main id="planDetail">`:
   - After `<cts-crumb id="planDetailCrumb">` (line 150), add `<cts-loading-state id="planDetailLoading" label="Loading test plan"></cts-loading-state>`.
   - Add `id="planDetailGrid"` and the `hidden` attribute to the existing `<div class="oidf-plan-detail-grid">` (line 151).
4. **Bootstrap (`DOMContentLoaded`, lines 759–783):**
   - Remove the `FAPI_UI.showBusy();` call (line 764). The in-page loader is already present in the static markup, so nothing needs to be shown imperatively.
   - In `.finally()`, replace `FAPI_UI.hideBusy();` with: remove `#planDetailLoading` (guard for null) and set `#planDetailGrid`'s `hidden = false`. Keep the existing `maybeShowAlsoRequiredBanner(planId, isPublic)` call after the reveal (banner inserts after the crumb, outside the grid, so it is unaffected).
5. **Leave untouched:** the `#loadingModal` markup (lines 86–91), `runTest()`'s `FAPI_UI.showBusy(...)` (lines 602–603), and all other modals.

**Patterns to follow:**
- `cts-plan-list.js:968` for the `cts-loading-state` label form.
- `log-detail.js bootstrap()` for the "no blocking modal on initial load; reveal content on settle" shape.

**Test scenarios:** Covered by U2 (page-level behavior is only observable through the E2E harness; there is no component unit under test here).

**Verification:**
- Loading `plan-detail.html?plan=…` shows an inline spinner in the content area with no dimmed/blurred full-screen backdrop.
- After data loads, the spinner is gone and the plan header + modules + actions are visible.
- Running a test from a module row still shows the transitional "Creating a new test…" modal (unchanged).
- `npm run test:ci` (from `frontend/`) passes — format, lint, type-check, `lint:icons`, `lit-analyzer`.

---

### U2. E2E coverage: in-page loader used, no modal overlay on load

**Goal:** Lock in that initial load uses the in-page `cts-loading-state` and never opens the `#loadingModal` overlay, and that content is revealed after the fetch settles.

**Requirements:** Same as U1 — proven via Playwright.

**Dependencies:** U1.

**Files:**
- `frontend/e2e/plan-detail.spec.js` (modify — add one test to the existing `plan-detail.html — Plan Detail` describe block)

**Approach:**
- Add a test that **delays** the `**/api/plan/plan-abc-123` mock response (e.g. resolve `route.fulfill` after a short `setTimeout`, or use `page.route` with an awaited delay) so the loading window is observable. While the plan fetch is in flight:
  - assert `cts-loading-state#planDetailLoading` is visible,
  - assert `#loadingModal` is **hidden** (no overlay).
- After the response resolves:
  - assert `#planDetailLoading` has count 0 (removed),
  - assert `#planDetailGrid` (or `#planDetailHeader`) is visible and contains the plan name,
  - assert `#loadingModal` is still hidden.
- Follow the file's existing harness conventions: `setupFailFast(page)` first, then specific routes, then `setupCommonRoutes(page)`, then `goto`. Reuse `MOCK_PLAN_DETAIL` and `setupTestInfoRoute`. The `afterEach` `expectNoUnmockedCalls(page)` hook already applies — make sure any route the delayed test triggers (`/api/info/...`) is mocked.

**Patterns to follow:**
- Existing tests in `frontend/e2e/plan-detail.spec.js` (route setup order, `MOCK_PLAN_DETAIL`, `setupTestInfoRoute`, `MOCK_TEST_STATUS`).
- The delayed-route technique keeps the loading-window assertions non-racy (instant mocks would remove the loader before the assertion runs).

**Test scenarios:**
- **Happy path / loading window:** with a delayed `/api/plan` response — `cts-loading-state#planDetailLoading` visible AND `#loadingModal` hidden during the fetch.
- **Happy path / settled:** after the response — `#planDetailLoading` removed (count 0), `#planDetailGrid` visible with the plan name (`oidcc-basic-certification-test-plan`), `#loadingModal` still hidden.
- **Regression (no modal overlay):** `#loadingModal` is hidden across the whole initial-load lifecycle.
- **No regression in existing tests:** the existing `plan-detail.html` tests (header/modules render, publish, delete, certify, R28 deep-links, also-required banner) still pass — they navigate and assert on `#planDetailHeader` / `#planItems`, which are revealed in `.finally()` after `getPlan()` settles.

**Verification:**
- `cd frontend && ./node_modules/.bin/playwright test e2e/plan-detail.spec.js` is green, including the new test and all pre-existing ones.

---

## Risks & Mitigations

- **`[hidden]` overridden by `display: grid` (silent no-op).** Mitigated by KTD 4's explicit `.oidf-plan-detail-grid[hidden] { display: none }` rule and the U2 assertion that the grid is not visible during the (delayed) loading window.
- **Flaky loading-window assertions.** Instant mocks would tear down the loader before assertions run. Mitigated by delaying the `/api/plan` mock in the new test so the loading state is deterministically observable.
- **`expectNoUnmockedCalls` afterEach trips on the new test.** The delayed-plan test still triggers the per-module `/api/info` fetches; mitigated by registering `setupTestInfoRoute` like the sibling tests.

---

## Notes / Out of Scope Confirmations

- `src/main/resources/static/components/cts-plan-detail.stories.js` is a synthetic Storybook page that composes `cts-plan-header` / `cts-plan-modules` / `cts-plan-actions` / `cts-modal` for review; it does **not** render `#loadingModal` or an initial-load loader, so **no story changes are required**. (Optionally, a story demonstrating `cts-loading-state` in the plan-detail context could be added, but `cts-loading-state` already has its own stories, so this is not needed.)
- `cts-loading-state` already has Storybook coverage (`cts-loading-state.stories.js`) and is unchanged by this plan.
