---
title: "fix: Make the Pages/PlanDetail story mirror the real plan-detail page"
type: fix
status: completed
date: 2026-06-05
---

# fix: Make the Pages/PlanDetail story mirror the real plan-detail page

## Summary

The Storybook story `Pages/PlanDetail` (`frontend/stories/pages/plan-detail.stories.js`) renders its three components inside dead Bootstrap markup (`container-fluid`, `card`, `card-body bg-gradient`, `row`, `col-md-10`/`col-md-2`) that stopped existing when Bootstrap was removed from the codebase. With those classes inert, the actions panel stacks full-width below the header instead of sitting in the page's 240px right rail, and the page's padding, max-width, and breadcrumb are missing — so the story no longer looks like `plan-detail.html` in the running app. Fix it by rebuilding the story markup on the real page structure, following the pattern already established by `frontend/stories/pages/running-test.stories.js`.

## Problem Frame

- The real page (`src/main/resources/static/plan-detail.html`) renders: `<main class="oidf-plan-detail-page">` (padding `var(--space-5) var(--space-6)`, `max-width: 1320px`, centered, `font-family: var(--font-sans)`) → `cts-crumb.oidf-plan-detail-crumb` ("Plans → planName") → `.oidf-plan-detail-grid` (`grid-template-columns: minmax(0, 1fr) 240px`, `gap: var(--space-5)`, `align-items: start`, stacking at ≤900px) with `cts-plan-header` + `cts-plan-modules` in the left column and `cts-plan-actions` as the right rail.
- The story renders the same three components inside Bootstrap-era wrappers whose classes are now unstyled. Net effect at any canvas width: no page padding/max-width, no crumb, no card-less header zone, and — most visibly — `cts-plan-actions` renders as a full-width block *between* the header and the modules instead of a 240px rail beside them.
- Root cause is historical: the story predates the Bootstrap removal (written in `f539bffa5`; Phase D later deleted Bootstrap and these classes silently became no-ops). The sibling `running-test.stories.js` was already rebuilt on the current pattern — story-local `PAGE_STYLES` `<style>` block copying the page's inline styles, real page class names, and a comment documenting the intentional navbar/skip-link omission — and is the proof the approach renders faithfully.
- Storybook already loads all production stylesheets globally (`frontend/.storybook/preview-head.html`: `oidf-tokens.css`, `layout.css`, `oidf-app.css`, `cert-package.css`, Inter font). The only CSS a page story must supply itself is the page's **inline** `<style>` block, which is exactly what the `PAGE_STYLES` pattern does.

## Requirements

- R1. At desktop widths the story composes the page like the app: crumb above, header+modules in the left column, actions as a 240px right rail, inside a padded, max-width-1320 centered wrapper.
- R2. The story carries no dead Bootstrap class names; markup mirrors `plan-detail.html`'s structure and class names so a future page-layout change has one obvious story counterpart to update.
- R3. Both stories (`Default`, `AdminView`) keep their existing content/behavior play assertions green, and new layout assertions lock the rail composition so the story cannot silently rot again.
- R4. The full Storybook suite stays green.

## Key Technical Decisions

- **Mirror the `running-test.stories.js` pattern, not a shared-CSS refactor.** The page's layout CSS lives in an inline `<style>` in `plan-detail.html`, so a story cannot link it; the established convention (see the comment block in `running-test.stories.js`) is a story-local `PAGE_STYLES` `html`-tagged `<style>` copying the relevant rules, with the duplication accepted. Extracting page CSS to a shared file would also touch the production page — out of scope for a story-parity fix (deferred).
- **Copy only the rules the story composes:** `.oidf-plan-detail-page`, `.oidf-plan-detail-crumb`, `.oidf-plan-detail-grid`, and its ≤900px stacking media query. Skip `.oidf-private-link-result` (modal-internal, not rendered by these stories).
- **Include the breadcrumb with static items.** `cts-crumb` is purely property-driven (`.items` array of `{label, target}`); the page sets `[{Plans → /plans.html}, {planName}]` from fetched data, which the story can hardcode from its fixture. This is cheap, faithful parity — unlike the navbar/skip-link, which depend on `/api/currentuser` and stay intentionally omitted with the same explanatory comment `running-test.stories.js` uses.
- **Use a `div.oidf-plan-detail-page` wrapper, not `<main>`.** Storybook docs view renders multiple stories on one document; duplicating `<main>` landmarks there is an a11y smell. The class carries all the styling.
- **Pin both stories to the `desktop` viewport preset** (`parameters.viewport.defaultViewport` + `globals.viewport` — the dual-field pin shape from `cts-plan-header.stories.js` `MetaTwoColumnOnDesktop`; `running-test.stories.js` is the PAGE_STYLES precedent but pins no viewport) so the 900px stacking media query and the 240px-rail layout assertion are deterministic rather than dependent on the test runner's default canvas width.
- **Lock layout with computed-style assertions** (house measurement idiom — no `getBoundingClientRect()` layout proxies): the grid's `gridTemplateColumns` must resolve to exactly two tracks with the second `240px`. That assertion fails on a revert to the dead-Bootstrap markup (no grid → `none`) and on accidental rail removal.

## Assumptions

- Visual parity at desktop widths is the goal stated by the user; pixel-perfect parity of fetch-driven states (live module statuses, navbar) is not expected from a mocked story. Navbar/skip-link omission follows the documented sibling precedent.
- The lesser rot in `log-detail.stories.js` and `upload.stories.js` (inert `container-fluid p-3` wrapper only — no structural damage) is out of scope; deferred below.
- `MOCK_PLAN_DETAIL` + `MOCK_MODULES_WITH_STATUS` remain the story fixtures; no fixture changes are needed for parity.

## Implementation Units

### U1. Rebuild Pages/PlanDetail story markup on the real page structure

**Goal:** The `Default` and `AdminView` stories render the same composition a user sees on `plan-detail.html`, with layout assertions that keep it that way.

**Requirements:** R1, R2, R3, R4

**Dependencies:** none

**Files:**
- `frontend/stories/pages/plan-detail.stories.js`

**Approach:** Add a `PAGE_STYLES` `html`-tagged template copying the four relevant rules from `plan-detail.html`'s inline `<style>` (page wrapper, crumb margin, grid, 900px stack), with a comment naming the source file so drift is traceable. Replace both stories' Bootstrap markup with the real structure: `PAGE_STYLES` + `div.oidf-plan-detail-page` → `cts-crumb.oidf-plan-detail-crumb` with static `.items` (`Plans → /plans.html`, then the fixture's `planName`) → `div.oidf-plan-detail-grid` → one left `div` wrapper holding `cts-plan-header` + `cts-plan-modules`, and `cts-plan-actions` directly as the second grid child. **Add the `cts-crumb` component import** alongside the three existing component imports — stories register custom elements themselves (nothing is globally registered in `preview-head.html`); without the import the crumb renders as an unknown inline element and the parity goal silently fails. The crumb's `cts-crumb-navigate` click event gets no handler in the story — clicking "Plans" is a visual no-op, acceptable for design review; state this in the page-chrome comment alongside the navbar/skip-link omission. Carry over the existing property bindings (`.plan`, `.modules`, `plan-id`, `is-admin` on AdminView). Add the running-test-style comment block documenting what is mirrored and what is intentionally omitted. Pin both stories to the `desktop` preset (1280px — comfortably above the 900px stack breakpoint). Extend both play functions with a layout step asserting the grid composition; keep all existing content assertions untouched.

**Patterns to follow:** `frontend/stories/pages/running-test.stories.js` (PAGE_STYLES block, page-chrome comment, source-file attribution); `src/main/resources/static/plan-detail.html` lines for the markup structure being mirrored; `src/main/resources/static/components/cts-plan-header.stories.js` `MetaTwoColumnOnDesktop` (viewport-pin shape, computed-style assertion register).

**Test scenarios:**
- Covers R1/R3. `Default` play (at the pinned `desktop` viewport, so the 900px media query cannot stack the grid): `getComputedStyle(grid).gridTemplateColumns` resolves to exactly two tracks and the second track is `240px` (strict — the dead-Bootstrap markup computes `none`, and a `1fr`-only rewrite fails the track count).
- Covers R1. `Default` play: the crumb renders both items ("Plans" and the fixture's plan name) — locks the breadcrumb parity cheaply via text content.
- Covers R3. Existing `Default` assertions (plan name, plan id, module rows, badge count, view-config button, delete-plan present, download-all absent) stay green unchanged.
- Covers R3. Existing `AdminView` assertions (owner row, admin buttons, publish buttons) stay green unchanged; `AdminView` gets the same grid assertion so both stories are locked.
- Edge case: `cts-plan-header`'s own `.planMeta` renders its two-column branch inside the left grid column at the desktop pin (left column ≈ canvas − padding − 240 − gap ≥ 640px container width) — no assertion needed beyond the suite staying green; noted so an implementer doesn't mistake the stacked branch for a regression at narrow canvases.

**Verification:** Storybook suite fully green (`npx vitest --project=storybook --run`, CLI fallback runner); visual check of the story against the live page (same composition: crumb, left column, 240px rail); `npm run test:ci` green; touched file formatted via the project prettier config.

---

## Scope Boundaries

**In scope:** `frontend/stories/pages/plan-detail.stories.js` only.

**Out of scope (true non-goals):**
- Navbar/skip-link in page stories (fetch-dependent; documented omission per the running-test precedent).
- `cts-footer` (page-level chrome below the composition under review; the running-test precedent omits it too — document the omission in the same comment block).
- Crumb navigation behavior (the story renders the crumb visually; its navigate event is a deliberate no-op — the page-side `window.location.assign` wiring belongs to `plan-detail.html`).
- Extracting page-inline CSS to shared files so stories can link instead of copy — production-page refactor beyond a story fix.

### Deferred to Follow-Up Work

- `log-detail.stories.js` and `upload.stories.js` still wrap content in the inert `container-fluid p-3` div (page padding lost, no structural damage). Same PAGE_STYLES treatment applies; separate change.

---

## Sources & Research

- Live comparison (this session): story at :6006 renders actions full-width below the header (col-md classes inert); real page at :8443 renders the 240px rail beside the header column.
- Canonical pattern: `frontend/stories/pages/running-test.stories.js` — PAGE_STYLES block + page-chrome comment ("Recreates running-test.html for design review… Mirrors the page chrome… navbar and skip-link intentionally omitted").
- Global story CSS: `frontend/.storybook/preview-head.html` loads `oidf-tokens.css`, `layout.css`, `oidf-app.css`, `cert-package.css`, and the Inter font — page-inline styles are the only gap a page story must fill.
- Page structure + inline CSS being mirrored: `src/main/resources/static/plan-detail.html` (`.oidf-plan-detail-page`, `.oidf-plan-detail-crumb`, `.oidf-plan-detail-grid` with `minmax(0, 1fr) 240px` and the 900px stack; markup `main → crumb → grid → [header+modules | actions]`; crumb items `Plans → planName` set from fetched data).
- `cts-crumb` API: property-driven `.items` array (`@property {Array<{label, target}>}`) — no fetch dependency, safe for static story parity.
- Story rot origin: `git log` shows the story markup dates to `f539bffa5`, before the Bootstrap-removal phase made the classes inert.
- Repo-research note: dedicated research agents were not re-dispatched — the affected files were read directly this session, and the learnings inventory was surveyed twice earlier today for the adjacent plan-header work (no `docs/solutions/` entry covers page-story parity; the closest convention is the running-test pattern itself).
