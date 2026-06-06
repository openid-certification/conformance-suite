---
title: "fix: Make log-detail Test details readable on mobile"
type: fix
status: completed
date: 2026-06-05
---

# fix: Make log-detail Test details readable on mobile

## Summary

On phone-width viewports, the log-detail page's "Test details" drawer renders metadata values in a column so narrow that variant values wrap one character per line, and the sticky status bar forces page-wide horizontal overflow that makes the whole page render zoomed-out and tiny. Fix both with targeted CSS in `cts-log-detail-header.js`: let the status bar's test name actually truncate, and stack the metadata table to a single column at narrow widths using the codebase's established container-query pattern.

---

## Problem Frame

Reproduced at a 360×844 viewport against `log-detail.html` with a long test name (`oid4vci-id2-issuer-credential-offer-flow-with-pre-authorized-code`) and a five-entry variant map. Two independent root causes compound, both in `src/main/resources/static/components/cts-log-detail-header.js`:

1. **Page-wide horizontal overflow from the sticky status bar.** `.ctsStatusBar` uses `grid-template-columns: auto 1fr auto`. The left cluster contains `.ctsStatusBarTestNameText` with `white-space: nowrap` + `text-overflow: ellipsis`, but the grid item (`.ctsStatusBarLeft`) keeps its implicit `min-width: auto`, so the `auto` track grows to the name's full max-content width instead of letting the ellipsis engage. Measured: `document.documentElement.scrollWidth` = 644px at a 360px viewport (status bar left cluster 450px wide, primary cluster's right edge at 644px). On a real phone this makes the browser render the page zoomed out — every text on the page becomes tiny.

2. **Metadata table crushes values at narrow widths.** `.logMetaTable` uses `grid-template-columns: minmax(120px, 180px) 1fr` with no responsive rule. Because the minmax has a *fixed* 180px max, the track-maximization step always grows the label column to 180px before `1fr` receives leftovers — measured label width is 180px at every viewport (360/390/768px), even though the longest label ("Test Version:") needs ~90px. At 360px the value column gets ~100px. The nested `.variantList` (`grid-template-columns: auto 1fr`) inside that 100px cell gives its `auto` key column most of the space, leaving variant values ~10px wide — they wrap one character per line (screenshots: `tmp/screenshots/repro-meta-360.png`). The `.ctsDrawerBody` 24px left indent (`padding: 0 0 var(--space-4) var(--space-6)`) further shrinks the available width.

---

## Requirements

**Readability**

- R1. At a 360px viewport, every "Test details" metadata value (including each variant key/value pair) renders with the full content width available to it — no value column narrower than the drawer's content box.
- R2. The log-detail page has no page-wide horizontal overflow at phone widths: `document.documentElement.scrollWidth <= window.innerWidth` at 360px and 375px with a long test name.
- R3. Long unbreakable values (test IDs, module names, mono chips) wrap within their cell rather than forcing overflow.

**Regression safety**

- R4. At desktop widths (≥1024px viewport) the drawer keeps a two-column label/value layout and the sticky status bar keeps its current single-row composition; the label column hugs its content instead of always maximizing to 180px.
- R5. Existing Storybook play tests and Playwright e2e suites for log-detail stay green; new coverage locks the mobile behavior (stacked metadata layout + no horizontal overflow).

---

## Key Technical Decisions

- **Container query (stack-up), not media query, for the metadata table.** Default `.logMetaTable` to a stacked single-column layout (label above value), and opt into the two-column grid only at `@container (min-width: 640px)`. This mirrors the codebase's Pattern A used by `cts-running-test-card.js` (`@container ctsRunningTestCard (min-width: 560px)`) and `cts-log-entry.js` (`@container ctsLogViewer (min-width: 640px)`). A container query keys on the drawer's actual available width — correct even when the ≥1440px TOC rail or Storybook isolation changes the relationship between viewport and component width. The host element is `display: contents` and cannot be a container; declare `container: ctsLogDrawer / inline-size` on `.ctsDrawer` (a plain block div, safe for inline-size containment).
- **`min-width: 0` on the status bar's left grid item** so the `auto` track can shrink below max-content and the existing nowrap/ellipsis on `.ctsStatusBarTestNameText` finally engages. No breakpoint needed — this is a min-sizing bug, not a responsive-design gap. The badges in the cluster already `flex-wrap`.
- **`fit-content(180px)` for the wide-layout label column** (replacing `minmax(120px, 180px)`), plus `minmax(0, 1fr)` for the value column. `fit-content` sizes to the longest label clamped at 180px instead of unconditionally maximizing; `minmax(0, 1fr)` removes the value track's implicit min-size so long content wraps instead of expanding the grid (house convention per `cts-log-entry.js` R31 comment).
- **`minmax(0, 1fr)` for the `.variantList` value track** (`auto minmax(0, 1fr)`). Once the outer table stacks on mobile the variant list inherits the full drawer width, and the keyed `auto` column with a shrinkable value track stays readable; no separate breakpoint for the nested list.
- **Drop the `.ctsDrawerBody` 24px left indent in the stacked layout only.** At 360px the indent is 7% of the viewport; reclaim it when stacked, keep it in the ≥640px container layout where it aligns the body with the summary label.

---

## Assumptions

- The "whole page unreadably small" symptom in the bug report's screenshot is caused by the status-bar horizontal overflow (root cause 1); fixing it is in scope alongside the metadata table, since the table fix alone would leave the page zoomed out.
- Desktop appearance changes are limited to the label column hugging content (R4); no other visual change at wide widths is intended.
- `cts-plan-header.js`'s `.planMeta` has the same never-stacking two-column weakness but is a different page; it is deferred, not fixed here.

---

## Implementation Units

### U1. Let the sticky status bar shrink at narrow widths

**Goal:** Eliminate page-wide horizontal overflow on log-detail at phone widths so the page renders at natural scale.

**Requirements:** R2, R4, R5

**Dependencies:** none

**Files:**
- `src/main/resources/static/components/cts-log-detail-header.js` (STYLE_TEXT: `.ctsStatusBarLeft`)
- `src/main/resources/static/components/cts-log-detail-header.stories.js` (new play assertions)
- `frontend/e2e/log-detail.spec.js` (page-level overflow guard)

**Approach:** Add `min-width: 0` to `.ctsStatusBarLeft` so the `auto` grid track can shrink and the test-name span's existing `min-width: 0` + ellipsis chain engages. Audit the two sibling clusters while there: `.ctsStatusBarMiddle` already has `min-width: 0`; `.ctsStatusBarPrimary` holds fixed-size controls (~149px) and fits. Update the component's structural comment block if it describes the bar's sizing.

**Patterns to follow:** `cts-log-entry.js` R31 comment (min-size + wrap idiom); existing ellipsis treatment on `.ctsStatusBarTestNameText`.

**Test scenarios:**
- Covers R2. e2e: at a 375px viewport with the `MOCK_TEST_STATUS_LONG_VARIANT` fixture (see U3), after load, `document.documentElement.scrollWidth <= document.documentElement.clientWidth` (mirror the existing `"entries stream does not overflow horizontally at 375px viewport"` template at `frontend/e2e/log-detail.spec.js`, viewport set before `goto`; note that template measures the `.logEntries` sub-container — this guard must target the document element, which is what the status bar overflows).
- Storybook: viewport-pinned story (`mobile1`, pattern from `cts-navbar.stories.js` `parameters.viewport` + `globals.viewport`) asserting the status bar's width does not exceed its parent and the test-name span's `scrollWidth > clientWidth` (i.e., truncation engaged). Use a story-local fixture whose `testName` is the repro string `oid4vci-id2-issuer-credential-offer-flow-with-pre-authorized-code` — the shared `MOCK_TEST_STATUS` name (`oidcc-server`, 12 chars) never truncates, so the assertion would not exercise the fix.
- Happy path: at 1280px viewport, status bar still lays out name + badges + primary action on one row (existing stories cover this; verify no regression in the suite).

**Verification:** Storybook suite green; log-detail e2e green including the new overflow guard; manual check of `tmp/screenshots` repro at 360px shows no horizontal scroll.

### U2. Stack the Test details metadata table at narrow container widths

**Goal:** Make every metadata value (including variant pairs) readable at phone widths by giving values the full drawer width.

**Requirements:** R1, R3, R4

**Dependencies:** none (independent of U1; both touch STYLE_TEXT but different rules)

**Files:**
- `src/main/resources/static/components/cts-log-detail-header.js` (STYLE_TEXT: `.ctsDrawer`, `.ctsDrawerBody`, `.logMetaTable`, `.logMetaLabel`, `.variantList`)

**Approach:** Declare `container: ctsLogDrawer / inline-size` on `.ctsDrawer`. Make the stacked layout the default: `.logMetaTable { grid-template-columns: 1fr }` with the label rendering as a compact heading above its value. Spacing commitment: `gap: var(--space-1)` as the stacked grid's row gap (value hugs its label), plus `margin-top: var(--space-3)` on `.logMetaLabel:not(:first-child)` to separate one label/value pair from the next. Drop the `.ctsDrawerBody` left indent in this default layout. Inside `@container ctsLogDrawer (min-width: 640px)`, restore the two-column layout as `grid-template-columns: fit-content(180px) minmax(0, 1fr)` with the current gap, and restore the body indent. Change `.variantList` to `grid-template-columns: auto minmax(0, 1fr)` unconditionally.

**Technical design (directional):**

```css
/* default = stacked (mobile-first) */
.ctsDrawer { container: ctsLogDrawer / inline-size; }
.ctsDrawerBody { padding-left: 0; }
.logMetaTable { grid-template-columns: 1fr; gap: var(--space-1); }
.logMetaLabel:not(:first-child) { margin-top: var(--space-3); }
@container ctsLogDrawer (min-width: 640px) {
  .ctsDrawerBody { padding-left: var(--space-6); }
  .logMetaTable { grid-template-columns: fit-content(180px) minmax(0, 1fr); gap: var(--space-2) var(--space-4); }
  .logMetaLabel:not(:first-child) { margin-top: 0; }
}
.variantList { grid-template-columns: auto minmax(0, 1fr); }
```

**Patterns to follow:** `cts-running-test-card.js` stack-up container query; `cts-log-entry.js` `minmax(0, 1fr)` + `overflow-wrap: anywhere` idiom (already present on `.logMetaValue`).

**Test scenarios:** owned by U3 (single test-bearing unit so story/e2e assertions land once, after both CSS changes).

**Verification:** At 360px the variant list renders each value on readable full-width lines (compare against `tmp/screenshots/repro-meta-360.png` baseline); at ≥1024px viewport the two-column layout matches current rendering except the label column width hugs content.

### U3. Lock mobile behavior in Storybook and e2e

**Goal:** Regression-proof the fix at both component and page scope.

**Requirements:** R1, R2, R4, R5

**Dependencies:** U1, U2

**Files:**
- `src/main/resources/static/components/cts-log-detail-header.stories.js`
- `frontend/e2e/log-detail.spec.js`
- `frontend/e2e/fixtures/mock-test-data.js` (new `MOCK_TEST_STATUS_LONG_VARIANT` fixture)

**Approach:** Add a `MOCK_TEST_STATUS_LONG_VARIANT` fixture to `frontend/e2e/fixtures/mock-test-data.js` that spreads `MOCK_TEST_STATUS` with `testName: "oid4vci-id2-issuer-credential-offer-flow-with-pre-authorized-code"` and a five-entry `variant` map (`client_auth_type`, `response_type`, `credential_format`, `sender_constrain`, `response_mode`) so the e2e tests exercise the measured repro conditions. Component scope: add a viewport-pinned story (e.g. `DrawerMetadataStacksOnMobile`, `mobile1` = 320×568) that opens the Test details disclosure in `play` and asserts `getComputedStyle(metaTable).gridTemplateColumns` resolves to a single track; extend `DrawerExpandedRevealsMetadata` (desktop) to assert two tracks with the first ≤ 180px. Page scope: add one e2e test at 375×800 (viewport before `goto`, `setupFailFast` → `setupV2Routes` → `setupCommonRoutes` ordering) using the new fixture that asserts (a) no document-level horizontal overflow (U1's guard can live in this same test) and (b) the opened drawer's `.logMetaTable` computes to one column. Use computed style / `scrollWidth` assertions, not `getBoundingClientRect()` on the `display: contents` host.

**Patterns to follow:** `cts-navbar.stories.js` viewport pinning; `frontend/e2e/log-detail.spec.js` breakpoint test ("failure summary swaps between header and page-level positions at 1024px breakpoint") and 375px overflow guard ("entries stream does not overflow horizontally at 375px viewport").

**Test scenarios:**
- Covers R1. Storybook mobile story: drawer open at 320px → `gridTemplateColumns` is a single track; variant values get a readable column (assert via `getComputedStyle(variantList).gridTemplateColumns` that the value track resolves wider than 100px — computed-style check, consistent with the no-`getBoundingClientRect()` guidance).
- Covers R4. Storybook desktop story: drawer open → two tracks, first track ≤ 180px. Pin the story to a wide viewport preset (tablet/desktop from `MINIMAL_VIEWPORTS`) so the `.ctsDrawer` container is deterministically above the 640px threshold instead of depending on the default canvas width.
- Covers R2. e2e at 375px: `scrollWidth <= clientWidth` at document level with long test name.
- Covers R1. e2e at 375px: opened drawer's `.logMetaTable` computed `grid-template-columns` is a single track.
- Covers R5. Regression: with `.ctsDrawer` now an inline-size container, the Configuration disclosure's `cts-json-editor` still mounts and the `.ctsConfigJson` height lock holds (the existing `ConfigDrawerHeightLockedAtFixedValue` story asserts this; verify it stays green and extend it only if containment shifts the measured height) — inline-size containment makes `.ctsDrawer` the containing block for Monaco's absolutely-positioned overlays.
- Edge case: test with no variant map (`MOCK_TEST_STATUS` without `variant`) still renders the stacked table without an empty variant row (existing conditional rendering; assert via existing stories staying green).

**Verification:** `cd frontend && npm run test:ci` passes (format, lint, type-check, jsdoc, icons, lit-analyzer); Storybook suite green (fallback runner: `npx vitest --project=storybook --run`); `cd frontend && npm run test:e2e` green (log-detail spec fully passing; schedule-test flakes under parallel load are a known pre-existing caveat).

---

## Scope Boundaries

**In scope:** the two root-cause CSS fixes in `cts-log-detail-header.js` plus their tests.

### Deferred to Follow-Up Work

- `cts-plan-header.js` `.planMeta` (`grid-template-columns: max-content 1fr`, no stacking rule) shares the identical mobile weakness on plan-detail.html — same fix shape applies, separate change.
- Broader mobile audit of log-detail (hero, failure summary, nav row) — no overflow was measured in those zones at 360px; leave untouched.
- A shared breakpoint/container token in `oidf-tokens.css` (today every component hard-codes px values) — convention change beyond this fix.

---

## Sources & Research

- Repro measurements (Playwright, mocked routes, 360/390/768px): `.logMetaTable` computed `180px 100px` at 360px; `docScrollWidth` 644 at 360px viewport; screenshots `tmp/screenshots/repro-meta-{360,390,768}.png`.
- Responsive stack-up precedents: `src/main/resources/static/components/cts-running-test-card.js` (container + 560px), `src/main/resources/static/components/cts-log-entry.js` (`@container ctsLogViewer (min-width: 640px)`, R31 wrap idiom), `src/main/resources/static/plan-detail.html` (media-query stack at 900px).
- Breakpoint census: 640px and 1024px dominate; 640px already used inside `cts-log-detail-header.js` for the sticky bar.
- Institutional learning: `docs/solutions/web-components/subgrid-alignment-inside-details-blocks-2026-05-28.md` — `minmax(0, 1fr)` is load-bearing for shrinkable grid tracks; verify layout with screenshots/computed style, not `getBoundingClientRect()` on `display: contents` subtrees; container queries are the house responsive mechanism on log surfaces.
- Storybook viewport-pinning precedent: `src/main/resources/static/components/cts-navbar.stories.js` (`parameters.viewport` + `globals.viewport`, `MINIMAL_VIEWPORTS` registered in `frontend/.storybook/preview.js`).
