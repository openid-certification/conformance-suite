---
title: "fix: Stack the Schedule test CTA below the view tabs on narrow viewports"
type: fix
status: completed
date: 2026-06-05
---

# fix: Stack the Schedule test CTA below the view tabs on narrow viewports

## Summary

On mobile widths (~430px and below), the `cts-view-tabs` header row on plans.html is cramped: the "My Test Plans" / "Published Test Plans" tab labels wrap to two lines and the "Schedule test" CTA button wraps its own label ("Schedule" / "test"). This plan makes the row responsive with a container query inside the component's injected stylesheet: below a 640px container width the CTA drops to its own full-width row beneath the tabs and their divider rule, and tab/CTA labels are pinned to a single line. The component's flat DOM, its documented `> a` specificity guard, and every existing DOM-order assertion are preserved.

---

## Problem Frame

`cts-view-tabs` renders a single non-wrapping flex `<nav>`: My/Published anchor tabs (with the dataset-noun suffix and the Published help icon), then a `cts-link-button` CTA pushed right with `margin-left: auto`. The row's natural single-line width is ~470px of container space (~510px viewport), so on phones everything squeezes and wraps mid-label. The user-confirmed direction is to move the CTA after (below) the tabs on mobile rather than shrinking or hiding anything.

---

## Requirements

**Layout behavior**

- R1. Below the narrow threshold (container width < 640px), the Schedule test CTA renders on its own row below the tabs and below the divider rule — not beside the tabs.
- R2. The stacked CTA spans the full width of the tabs row (conventional mobile tap target).
- R3. Tab labels and the CTA label never wrap mid-label at viewports ≥ 360px — each stays on a single line.
- R4. The grey divider rule stays visually attached directly beneath the tabs row in both layouts, and the active tab's 2px orange underline continues to overlap it exactly as on desktop.
- R5. At container widths ≥ 640px the rendering is pixel-identical to today: single row, CTA right-aligned, no behavior change.

**Cross-page neutrality**

- R6. logs.html (tabs without CTA) keeps a visually unchanged single-row layout at all widths, apart from the label nowrap fix.

**Stability**

- R7. The `cts-view-tabs:not(:defined)` pre-upgrade height reservation in `css/layout.css` matches the taller stacked footprint at mobile widths, so the layout-shift posture does not regress.
- R8. DOM order is untouched — the CTA remains the last child of the `<nav>`, and all existing Storybook play assertions and Playwright e2e assertions stay green.

**Coverage**

- R9. A Storybook story pinned to a mobile viewport asserts the stacked layout with play-function assertions.
- R10. A Playwright e2e test on plans.html asserts the stacked layout at a phone viewport.

---

## Key Technical Decisions

- **Container query, not viewport media query.** Set `display: block; container-type: inline-size` on the `cts-view-tabs` host and switch layout with `@container (width < 640px)` inside the existing head-injected `STYLE_TEXT`. Container queries are the established responsive primitive in this codebase (`cts-log-entry` / `cts-log-viewer` at 640px, `cts-running-test-card` at 560px, `cts-plan-modules` at 780px), are Baseline Widely Available (2023) within the project's evergreen-only browser policy, and keep the component correct at any embedding width — including Storybook iframes. 640px is the dominant small-tier precedent and sits comfortably above the ~470px squeeze point.
- **Flat DOM preserved; stacking is pure CSS.** No template change: `flex-wrap: wrap` on the nav (with `row-gap: 0` in the narrow state — the inherited `gap: var(--space-1)` shorthand would otherwise insert a 4px row-gap between wrapped lines), a `::after` pseudo-element with `flex-basis: 100%` carries the divider rule between the tab line and the CTA line (the nav's own `border-bottom` is disabled in the narrow state), and the CTA gets `order: 1; flex-basis: 100%; margin-left: 0`. This preserves the documented `.cts-view-tabs > a` specificity guard verbatim and keeps the CTA as `nav`'s last DOM child, which both `cts-view-tabs.stories.js` (`nav.lastElementChild === cta`) and `frontend/e2e/plans.spec.js` (`nav.cts-view-tabs > :last-child`) assert.
- **Full-width CTA on mobile.** A right-aligned compact button floating below the rule reads as detached; a full-width primary button below a tab bar is the conventional mobile pattern and maximizes the tap target.
- **`white-space: nowrap` on tab anchors and the CTA label, applied at all widths.** Mid-label wrapping is never acceptable; below ~360px viewport the tab row may overflow instead, which is accepted (see Scope Boundaries).
- **Reservation bump via viewport media query.** `layout.css` cannot container-query the unupgraded host (no container exists pre-upgrade), so the `:not(:defined)` bump uses a `@media` query approximating the container threshold (container 640px ≈ viewport ~680px: 640 + 2× the 20px `--space-5` padding on `.listing-page`). The small mismatch band is acceptable for a pre-upgrade placeholder heuristic.

---

## High-Level Technical Design

Wide container (≥ 640px) — unchanged:

```text
[ My Test Plans ] [ Published Test Plans (?) ]            [ + Schedule test ]
──────────────────────────────────────────────────────────────────────────────
        ▲ active-tab 2px orange underline overlaps the 1px nav border
```

Narrow container (< 640px) — three flex lines via `flex-wrap` + `::after`:

```text
[ My Test Plans ] [ Published Test Plans (?) ]      ← line 1: anchors (nowrap)
──────────────────────────────────────────────      ← line 2: ::after divider,
                                                      flex-basis: 100% (replaces
                                                      the nav border-bottom)
[            + Schedule test                  ]      ← line 3: CTA, order: 1,
                                                      flex-basis: 100%
```

The anchors keep their `margin-bottom: -1px` so the active tab's 2px underline overlaps the `::after` divider line the same way it overlaps the nav border on desktop. This depends on `row-gap: 0` in the narrow state: the nav's existing `gap: var(--space-1)` shorthand would otherwise insert a 4px row-gap between the anchor line and the divider that a -1px margin cannot close (the cited `cts-plan-modules` precedent manages its wrapped row-gap explicitly for the same reason). Directional guidance, not implementation specification — if the overlap still fights the wrapped flex line after the row-gap reset, the fallback is documented in Risks.

---

## Implementation Units

### U1. Narrow-container stacking CSS in cts-view-tabs + mobile Storybook story

- **Goal:** The CTA stacks full-width below the tabs and divider at narrow container widths; labels never wrap mid-label; wide rendering unchanged.
- **Requirements:** R1, R2, R3, R4, R5, R6, R8, R9
- **Dependencies:** none
- **Files:** `src/main/resources/static/components/cts-view-tabs.js`, `src/main/resources/static/components/cts-view-tabs.stories.js`
- **Approach:**
  - Add a host rule to `STYLE_TEXT`: `cts-view-tabs { display: block; container-type: inline-size; }` (custom elements default to inline; `container-type` needs a block-level box).
  - Add `white-space: nowrap` to `.cts-view-tabs > a` and to the CTA (scoped via `.cts-view-tabs .cts-view-tabs-cta`, reaching the inner `.oidf-btn` if needed — keep selector specificity above `0,1,0` per the existing guard comment).
  - Add an `@container (width < 640px)` block: nav gets `flex-wrap: wrap; border-bottom: none; row-gap: 0;` (the inherited `gap: var(--space-1)` shorthand otherwise becomes a 4px row-gap between wrapped lines, defeating the `-1px` underline overlap — keep the horizontal spacing via `column-gap: var(--space-1)`); `.cts-view-tabs::after` carries the divider (`content: ""; flex-basis: 100%; border-bottom: 1px solid var(--border);`); the CTA gets `order: 1; flex-basis: 100%; margin-left: 0; margin-top: var(--space-3); min-height: 44px;` (the `sm` button renders at 30px — bump the stacked CTA to a 44px touch-target height per WCAG 2.5.8 / platform guidelines) and its inner anchor stretches full width.
  - Verify the Published help tooltip stays readable and dismissable in the stacked state. It may overlay the CTA transiently while open — standard tooltip overlay behavior, same as it overlays the plan cards on desktop — so only change `placement` if it renders unreadably (note: `placement="auto"` resolves to the side with the most viewport space, which near the top of the page is still `bottom`).
  - Update the CSS comments (the `> a` guard comment and the CTA comment) to describe the narrow state.
  - New story `MobileStackedCta` pinned to the `mobile2` (414×896) preset, mirroring the `cts-navbar.stories.js` `MobileMenuTogglesNavlinks` pattern — the exact shape is `parameters: { viewport: { defaultViewport: "mobile2" } }` together with `globals: { viewport: { value: "mobile2", isRotated: false } }` — with `create-test-href`, `published-help`, and `dataset-noun="Test Plans"` set. (The story uses 414px while the U3 e2e test uses 390px — intentional: two common phone widths, both comfortably below the 640px threshold.) The stories file already has the module-level `beforeEach` URL reset — keep it.
- **Patterns to follow:** `cts-running-test-card.js` (container-query layout switch in injected style text), `cts-spec-cascade.js` / `cts-test-selector.js` (responsive blocks inside `STYLE_TEXT`), `cts-navbar.stories.js` `MobileMenuTogglesNavlinks` (viewport-pinned story), `cts-action-bar.js` (flex-wrap row precedent).
- **Test scenarios:**
  - At mobile2 viewport, the CTA's bounding box top is at or below the Published anchor's bounding box bottom (stacked), and the divider sits between them (`getComputedStyle(nav, "::after")` shows the 1px border in the narrow state).
  - The stacked CTA's width is approximately the nav's content width (full-width).
  - The CTA label renders on one line (computed `white-space: nowrap`; box height equals the one-line button height).
  - Tab anchors render on one line at 414px (anchor box height matches the single-line height).
  - DOM order unchanged: `nav.lastElementChild` is still the CTA element in the stacked state.
  - The active anchor's underline sits flush against the divider: no measurable vertical gap between the active anchor's bottom edge and the `::after` divider line (locks the `row-gap: 0` reset).
  - The stacked CTA's rendered height is ≥ 44px (touch-target floor).
  - Keyboard order in the stacked state: tabbing moves My anchor → Published anchor → help icon → CTA, matching the visual top-to-bottom order (pins the DOM-order keyboard sequence).
  - Focus and active states survive the narrow state: the focused Published anchor shows the `--focus-ring` box-shadow, and the active anchor's `border-bottom-color` still resolves to the orange token.
  - Anonymous narrow state (no `authenticated` attribute): only the Published anchor renders, and the CTA still stacks full-width below the divider.
  - Tabs-only narrow state (no `create-test-href`, the logs.html shape): single row, divider attached beneath the anchors, no stray second line.
  - All 11 existing stories stay green (12 total with the new `MobileStackedCta`), including `ScheduleTestCta`'s `lastElementChild` and `borderBottomWidth` assertions at the default desktop viewport.
- **Verification:** `npm run test:ci` and the Storybook Vitest suite pass from `frontend/`; visually confirm both layouts in Storybook at desktop and mobile2 viewports.

### U2. Bump the pre-upgrade height reservation for the stacked layout

- **Goal:** No new layout shift on phones: the `cts-view-tabs:not(:defined)` reservation matches the stacked footprint at narrow widths.
- **Requirements:** R7
- **Dependencies:** U1 (the stacked footprint must exist to be measured)
- **Files:** `src/main/resources/static/css/layout.css`
- **Approach:** Inside a `@media (max-width: 680px)` block (640px container threshold + 2× the 20px `--space-5` horizontal padding on `.listing-page` = 680px viewport — verify the computed padding at implementation), raise `cts-view-tabs:not(:defined) { min-height: … }` to the measured stacked height (estimate ~96-100px: anchor row + divider + gap + small button + bottom margin; measure the real post-upgrade value in devtools). Extend the existing KTD6 comment to explain the two-tier reservation.
- **Patterns to follow:** the existing `:not(:defined)` reservation block in `css/layout.css` (KTD6) and the `docs/solutions/web-components/cts-navbar-inline-visibility-bug-2026-04-24.md` rule that reservations need `display: block` + explicit `min-height`.
- **Test scenarios:** Test expectation: none — pure pre-upgrade placeholder CSS with no behavioral surface; there is no FOUC harness spec in the tree (the `fouc.spec.js` referenced by an older learning no longer exists). Verify by hard-reloading plans.html at a phone viewport with network throttling and confirming the list below the tabs does not jump when the component upgrades.
- **Verification:** Visual check per above; the reserved height within the mobile media query equals the measured stacked footprint within a few pixels.

### U3. Playwright e2e viewport regression tests + scratch-spec cleanup

- **Goal:** Lock the stacked mobile layout into the page-level e2e suite and remove the leftover scratch spec.
- **Requirements:** R3, R6, R8, R10
- **Dependencies:** U1
- **Files:** `frontend/e2e/plans.spec.js`, `frontend/e2e/logs.spec.js`, delete `frontend/e2e/__tmp_mobile_repro.spec.js` (untracked scratch file, self-marked "DELETE ME")
- **Approach:** Add a test to `plans.spec.js` that sets `page.setViewportSize({ width: 390, height: 844 })` before navigation (the `setViewportSize` API pattern is established in `logs.spec.js:511` at 600×800 and `schedule-test.spec.js` at 1024×360 — those citations validate the API usage, not the phone width; 390×844 is new here), reuses the existing `SCHEDULE_CTA` selector, and waits for the component upgrade with `waitForFunction` rather than assertion timeouts (per `docs/solutions/test-failures/playwright-e2e-flaky-after-web-component-merge-2026-04-14.md`). Keep `setupFailFast()` registered first per the route-order convention.
- **Test scenarios:**
  - plans.html at 390×844: the CTA's bounding box sits fully below the Published tab anchor's bounding box, and its width is approximately the tabs row's width.
  - plans.html at 390×844: tab anchors render on a single line (anchor bounding-box height equals the single-line height).
  - plans.html at 390×844: the existing `nav.cts-view-tabs > :last-child` → `schedule-test-cta` assertion holds in the stacked state.
  - logs.html at 390×844: the tabs row renders on a single line with no CTA present.
  - Existing desktop-viewport plans.spec.js tests (CTA visibility, href, text, last-child, tab active-state suites) remain green without modification.
- **Verification:** `cd frontend && npm run test:e2e` passes; `git status` no longer lists the scratch spec.

---

## Scope Boundaries

- **Viewports below ~360px:** the two tab labels at their natural nowrap width may exceed the container; the row may clip. Accepted — 360px is the smallest common evergreen device width, and R3's single-line guarantee starts there. Do not add horizontal scrolling, label truncation, or anchor-padding trimming for this band.
- **No DOM restructure:** the flat `nav > a, a, cts-link-button` template stays. A wrapper-div restructure is the documented fallback only (see Risks), not the plan of record.
- **No CTA on logs.html:** unchanged product decision; this plan only inherits the nowrap fix there.
- **The search input / sort dropdown rows on plans.html** are already stacked and are out of scope.
- **Tab label copy and the dataset-noun mechanism** are out of scope — no shortening labels on mobile.

### Deferred to Follow-Up Work

- Capture a `docs/solutions/web-components/` learning on responsive stacking for light-DOM header rows (container query + two-tier `:not(:defined)` reservation) once the fix is verified — the learnings researcher flagged this as a genuine gap.

---

## Risks

- **The `-1px` underline overlap inside a wrapped flex line is the fiddliest piece.** The deterministic hazard — the nav's `gap: var(--space-1)` becoming a 4px row-gap under `flex-wrap` — is handled by the explicit `row-gap: 0` in U1's narrow-state block. After that reset, negative bottom margin shrinks the flex line's cross size so the orange underline overlaps the `::after` divider; only if residual sub-pixel disagreement remains across browsers, fall back to restructuring the template with a tabs-row wrapper `<div>` that owns the divider border on mobile. That fallback requires updating the `> a` selector (to keep the specificity guard against `.oidf-btn`), the guard comment, and re-checking the `lastElementChild` assertions (the CTA stays the nav's last child either way).
- **Full-width CTA depends on `cts-link-button` internals.** The host is block-level and the inner `<a class="oidf-btn">` is `inline-flex`; stretching it may need a scoped width rule. Keep it scoped under `.cts-view-tabs .cts-view-tabs-cta` so other `cts-link-button` consumers are unaffected.
- **Reservation mismatch band:** the `layout.css` media query approximates the container threshold; between ~640-688px viewport the reservation may be one row off for the pre-upgrade flash only. Accepted.

---

## Sources & Research

- Component and consumers: `src/main/resources/static/components/cts-view-tabs.js` (flex row, `> a` specificity guard, CTA `margin-left: auto`), `src/main/resources/static/plans.html` (sets `create-test-href`), logs.html (no CTA).
- Responsive precedents: `cts-running-test-card.js` (`@container … (min-width: 560px)` stack→row), `cts-log-entry.js` / `cts-log-viewer.js` (`@container ctsLogViewer (min-width: 640px)`), `cts-plan-modules.js` (`@container planModulesCard (max-width: 780px)` full-width action row), `cts-action-bar.js` (flex-wrap row), `cts-spec-cascade.js` / `cts-test-selector.js` (`@media (max-width: 768px)` inside injected `STYLE_TEXT`).
- CLS reservation: `css/layout.css` `cts-view-tabs:not(:defined) { display: block; min-height: 40px; }` (KTD6 of the plans-page-as-home plan); `docs/solutions/web-components/cts-navbar-inline-visibility-bug-2026-04-24.md`.
- Test precedents: `cts-navbar.stories.js` `MobileMenuTogglesNavlinks` (viewport-pinned story: `parameters.viewport` + `globals.viewport`), `frontend/.storybook/preview.js` (`MINIMAL_VIEWPORTS`: mobile1 320×568, mobile2 414×896), `frontend/e2e/plans.spec.js` (`SCHEDULE_CTA`, `PUBLISHED_HELP`, last-child assertion), `frontend/e2e/logs.spec.js:511` (`setViewportSize` precedent), `docs/solutions/test-failures/playwright-e2e-flaky-after-web-component-merge-2026-04-14.md` (route order, upgrade waits).
- External: modern-web-guidance `size-aware-styling` — container queries Baseline Widely Available since 2023-02 (Chrome 105 / Firefox 110 / Safari 16); confirms `container-type: inline-size` + `@container` switch as the standard pattern. No fallback needed under the project's evergreen-only browser policy.
