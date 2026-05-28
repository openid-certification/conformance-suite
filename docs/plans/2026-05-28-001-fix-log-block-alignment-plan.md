---
title: "fix: Align is-block log entries with non-block rows in the log viewer"
type: fix
status: active
date: 2026-05-28
plan_id: 2026-05-28-001
---

# fix: Align is-block log entries with non-block rows in the log viewer

## Summary

Log entries that belong to a block (rendered inside `<details class="logBlock">`, with the `is-block` class on their `.logItem`) do not align with non-block (top-level) entries at the wide layout. Their timestamp / severity / HTTP / body / actions columns drift row-to-row and sit at different x-positions than the top-level rows above and below the block, producing a ragged, messy column layout.

The cause is structural: top-level rows align by subgridding into a master grid on `.logEntries`, but block children are nested one level deeper inside the `<details>` and never receive the `display: contents` + `subgrid` treatment. They fall back to a per-entry grid with `max-content` badge columns that resolve independently on every row.

This plan makes block children share a single grid so they align with each other and visually align with the surrounding top-level rows, while preserving the `<details>`/`<summary>` collapse semantics and the `is-block` left-edge stripe.

---

## Problem Frame

**Where it lives:** `src/main/resources/static/components/cts-log-viewer.js` (master grid + block rendering) and `src/main/resources/static/components/cts-log-entry.js` (per-row grid + `is-block` styling).

**Current alignment mechanism (wide layout, `@container ctsLogViewer (min-width: 640px)`):**

- `cts-log-viewer .logEntries` is a `display: grid` with `grid-template-columns: 92px auto auto minmax(0, 1fr) auto` (timestamp / severity / http / body / actions).
- Top-level `cts-log-entry` hosts are set to `display: contents`, so their `.logItem` becomes a direct grid item of `.logEntries`.
- `.logEntries > cts-log-entry .logItem` then sets `grid-template-columns: subgrid; grid-column: 1 / -1`, so every top-level row inherits the master tracks and the columns line up across all top-level rows.

**Why block children break:**

- Block children live at `.logEntries > .logBlock(<details>) > cts-log-entry > .logItem`. They are **not** direct children of `.logEntries`.
- The `display: contents` rule (`cts-log-viewer .logEntries > cts-log-entry`) and the subgrid rule (`.logEntries > cts-log-entry .logItem`) both use the `>` direct-child combinator, so neither matches block children.
- Block children therefore fall back to the default wide rule in `cts-log-entry.js`: `grid-template-columns: 92px max-content max-content 1fr auto`. Each nested `.logItem` is an **independent** grid; the `max-content` severity/HTTP columns size to *that row's* content, so the body column starts at a different x-position on every block row — ragged within the block and misaligned versus top-level rows.

**Documented constraint (from existing code comments in `cts-log-viewer.js`):** the prior author found that "Chrome's subgrid track propagation does not work through two consecutive contents-eliding ancestors, and switching `.logBlock` to a subgrid relay also fails," and deliberately fell back to per-entry grids for block children. This plan's central technical question is whether a **concrete** (non-`contents`) grid on `.logBlock` — leaving only one `display: contents` host (the `cts-log-entry`) between `.logBlock` and `.logItem` — avoids that limitation. That is the same single-level subgrid case the working top-level rule already relies on, so it is expected to work; **the assumption must be verified in-browser during execution** (see U1).

**Secondary contributor:** legacy global rules in `src/main/resources/static/css/layout.css` (`.logItem { padding-top: 7px; padding-bottom: 3px; border-bottom: 1px #eee }`, `.startBlock`, `.logItem:hover`) are unscoped and bleed into the component, adding vertical/padding noise that compounds the perceived misalignment. The `:hover` bleed is already documented in institutional memory (`feedback_layout_css_logitem_hover`).

---

## Scope

**In scope**
- Align block (`is-block`) child rows with each other at the wide layout (shared grid tracks).
- Align block child rows visually with surrounding top-level rows (matching column template; identical fixed timestamp column; consistent badge column rhythm).
- Preserve `<details>`/`<summary>` keyboard-collapse semantics, the per-block status badge cluster, the chevron rotation, and the `is-block` left-edge orange stripe.
- Preserve the small-layout (`< 640px`) stacked behavior unchanged.
- Storybook play-test coverage proving block/non-block alignment at the wide layout.

### Deferred to Follow-Up Work
- Pixel-perfect track-width unification across the block boundary if the chosen approach yields only *approximate* cross-boundary alignment (e.g. `auto` badge tracks resolving to slightly different widths inside vs. outside the block). Acceptable as long as the timestamp column matches exactly and badge columns are visually consistent; a follow-up can pursue full subgrid relay if the verification in U1 shows it is reliable.
- Broader cleanup / removal of the conflicting legacy `layout.css` `.logItem` / `.startBlock` / `.logItem:hover` global rules beyond the minimum needed to stop them interfering here. Full removal is its own Bootstrap-cleanup-adjacent task.

### Out of scope
- Any change to the entry data model, polling, block aggregation counts, or `LOG-NNNN` reference logic.
- The narrow/mobile stacked layout's visual design.

---

## Key Technical Decisions

**KTD1 — Give `.logBlock` a real grid that child rows subgrid into.** Rather than leaving block children on independent per-entry grids, make `.logBlock` (the `<details>`) a concrete grid at the wide layout, set nested `cts-log-entry` hosts to `display: contents`, and let each nested `.logItem` subgrid into `.logBlock`. This collapses the nesting to a single `display: contents` hop between the grid container and the subgridding item — the same shape the working top-level rule uses — so all child rows of a block share one set of column tracks and align with each other.

**KTD2 — Two-tier approach with a verified preference.** Two candidate column templates for `.logBlock`:

- **Approach B (default / robust):** `.logBlock { grid-template-columns: 92px auto auto minmax(0,1fr) auto }` — a concrete template matching the master grid. Guarantees sibling block rows align; cross-boundary alignment with top-level rows is exact on the fixed `92px` timestamp track and visually consistent on the badge tracks. No dependency on multi-level subgrid propagation.
- **Approach A (preferred *if verified*):** `.logBlock { grid-template-columns: subgrid }` (relaying the master `.logEntries` tracks) so block rows share the *exact* same resolved tracks as top-level rows for pixel-perfect cross-boundary alignment.

Decision: **implement Approach B as the committed default** (it does not depend on the propagation behavior the old comment warns about), and **attempt to upgrade to Approach A only if U1's in-browser verification confirms the relay propagates cleanly** across current Chrome/Safari/Firefox. The repo targets latest evergreen browsers only (`feedback_browser_support_policy`), so the old Chrome limitation may no longer apply — but we do not ship Approach A on faith. If A is not reliably verifiable, B stands and the residual is deferred (see Scope).

**KTD3 — The `<summary>` spans all tracks.** The block `<summary class="logItem startBlock">` carries the `.logItem` class but renders as a flex row (`.startBlock { display: flex }`). When `.logBlock` becomes a grid, the summary must explicitly span `grid-column: 1 / -1` so it occupies the full block width as a header band and does not get distributed across the timestamp/badge tracks.

**KTD4 — Keep the `is-block::before` stripe non-displacing.** The 3px left-edge stripe is an absolutely-positioned `::before` on `.logItem` (anchored by `position: relative`) precisely so it does not push content inward and break alignment with non-block rows. This invariant must be preserved — do not convert it to a `border-left` or grid gutter. Verify the stripe still anchors correctly once `.logItem` participates in the block grid.

**KTD5 — Neutralize legacy `layout.css` interference minimally.** Only to the extent it interferes with this alignment, ensure the scoped component rules win over the global `.logItem`/`.startBlock` padding and `.logItem:hover` rules (the component CSS is more specific in most cases, but verify the padding does not double up inside blocks). Do not undertake wholesale removal here (deferred).

---

## High-Level Technical Design

DOM and grid participation at the wide layout (`>= 640px`), comparing today vs. the fix:

```
.logEntries  (display: grid; 92px auto auto minmax(0,1fr) auto)   <-- master grid
│
├─ cts-log-entry            display: contents          ┐ top-level row:
│   └─ .logItem             subgrid; grid-column 1/-1  ┘ aligns to master  ✅ (today & after)
│
└─ .logBlock  <details>     grid-column: 1 / -1
   │
   │   TODAY:  block-flow container; children fall back to per-entry grids
   │           cts-log-entry  display: block
   │             └─ .logItem  92px max-content max-content 1fr auto   <-- independent ❌ ragged
   │
   │   AFTER (KTD1):  concrete grid (Approach B) OR subgrid relay (Approach A, if verified)
   │           .logBlock      display: grid; <template>; grid-column 1/-1
   │           ├─ summary.startBlock   grid-column: 1 / -1   (full-width header band)  [KTD3]
   │           └─ cts-log-entry        display: contents
   │                └─ .logItem        subgrid; grid-column 1/-1   <-- shares block tracks ✅ aligned
```

Why a concrete grid on `.logBlock` is expected to work where the old "subgrid relay" failed: the failing case the comment describes is `.logItem` subgridding through **two** `display: contents` ancestors into `.logEntries`. Approach B introduces no second contents hop — `.logBlock` is a real grid box, and only the single `cts-log-entry` host between it and `.logItem` is `display: contents`. That is structurally identical to the proven top-level rule.

---

## Implementation Units

### U1. Verify subgrid propagation behavior for block children (spike)

**Goal:** Establish empirically which approach (A subgrid-relay vs. B concrete-grid) aligns block children at the wide layout in current Chrome/Safari/Firefox, so U2 commits the right CSS instead of guessing against a possibly-stale code comment.

**Requirements:** Resolves KTD2's open fork before committing CSS.

**Dependencies:** none.

**Files:**
- `src/main/resources/static/components/cts-log-viewer.js` (scratch experimentation only — no committed change in this unit)
- Scratch screenshots under `tmp/screenshots/` (gitignored)

**Approach:**
- Run the viewer locally (or in Storybook) at a container width `>= 640px` with the existing `BlocksWithStatus` fixture (`MOCK_BLOCKS_WITH_STATUS` in `cts-log-viewer.stories.js`), which renders three `<details class="logBlock">` blocks.
- Temporarily apply Approach A (`.logBlock { display:grid; grid-template-columns: subgrid; grid-column:1/-1 }` + nested `cts-log-entry { display:contents }` + nested `.logItem { grid-template-columns: subgrid; grid-column:1/-1 }` + summary `grid-column:1/-1`). Measure whether block-row column x-positions match top-level rows (use `getBoundingClientRect()` on the body cell of a block row vs. a top-level row).
- If A does not propagate cleanly, fall back to Approach B (concrete `92px auto auto minmax(0,1fr) auto` template on `.logBlock`).
- Capture before/after screenshots to `tmp/screenshots/` for the PR.

**Patterns to follow:** mirror the measurement style of the existing `MobileContainer` play test, which reads `getComputedStyle(...).gridTemplateColumns`.

**Execution note:** This is a throwaway spike — do not commit experimental CSS. Its only output is the decision (A or B) carried into U2 and a note in the PR description.

**Test scenarios:** Test expectation: none — spike/verification unit; coverage is added in U3.

**Verification:** A documented decision (A or B) with a screenshot showing aligned columns for the chosen approach.

---

### U2. Make block child rows share a grid and align with top-level rows

**Goal:** Implement the chosen approach so all `.logItem` rows inside a `.logBlock` share one set of column tracks at the wide layout, aligning with each other and visually with the surrounding top-level rows.

**Requirements:** Core fix. Realizes KTD1, KTD2 (chosen approach), KTD3, KTD4, KTD5.

**Dependencies:** U1.

**Files:**
- `src/main/resources/static/components/cts-log-viewer.js` (modify the `@container ctsLogViewer (min-width: 640px)` block: convert `.logEntries > .logBlock` from a plain `grid-column: 1/-1` divider into a grid that child rows subgrid into; set nested `cts-log-entry` to `display: contents`; span the `summary.startBlock` across all tracks)
- `src/main/resources/static/components/cts-log-entry.js` (ensure the nested `.logItem` receives `grid-template-columns: subgrid; grid-column: 1/-1` when inside a block — add a selector parallel to the existing `.logEntries > cts-log-entry .logItem` rule, e.g. targeting `.logBlock cts-log-entry .logItem`; confirm the `is-block::before` stripe still anchors via `position: relative`)

**Approach:**
- At the wide container query only. The small-layout (`< 640px`) rules are untouched: block children keep stacking.
- `.logBlock` gets `display: grid` with the template chosen in U1 (default: `92px auto auto minmax(0,1fr) auto`), plus `grid-column: 1 / -1` to keep spanning the master grid.
- `summary.startBlock` gets `grid-column: 1 / -1` (KTD3) so it stays a full-width header band; its internal `display: flex` is unchanged.
- Nested `cts-log-entry` hosts inside a block become `display: contents` so each `.logItem` is a direct grid item of `.logBlock`.
- Nested `.logItem` subgrids into `.logBlock` (`grid-template-columns: subgrid; grid-column: 1 / -1`).
- Preserve `is-block::before` (KTD4) and the row separators / hover behavior.
- Keep the existing top-level subgrid rules working unchanged — the new block rules must not accidentally match top-level rows (scope the new selectors under `.logBlock`).

**Patterns to follow:** the existing top-level alignment trio in `cts-log-viewer.js` (`.logEntries { display:grid }`, `.logEntries > cts-log-entry { display:contents }`, `.logEntries > cts-log-entry .logItem { subgrid }`) is the exact pattern to mirror one level down for `.logBlock`.

**Technical design (directional):**
```
@container ctsLogViewer (min-width: 640px) {
  .logEntries > .logBlock { display: grid; grid-template-columns: <U1 choice>; grid-column: 1/-1; }
  .logEntries > .logBlock > summary.startBlock { grid-column: 1/-1; }
  .logEntries > .logBlock > cts-log-entry { display: contents; }
  /* in cts-log-entry.js */
  .logBlock cts-log-entry .logItem { grid-template-columns: subgrid; grid-column: 1/-1; }
}
```
Directional guidance only — exact selectors/specificity resolved during implementation.

**Test scenarios:**
- **Happy path (wide layout):** With the `BlocksWithStatus` fixture at container width `>= 640px`, the left x-position (`getBoundingClientRect().left`) of the body/message cell is equal across all child rows of a single block. *Covers the primary "block rows align with each other" requirement.*
- **Cross-boundary alignment:** The body cell's left x-position of a block child row equals (Approach A) or is within a small tolerance of (Approach B) the body cell's left x-position of a top-level row in the same viewer.
- **Timestamp column exact match:** The timestamp column left edge of a block row matches a top-level row exactly (fixed `92px` track).
- **Subgrid participation:** A nested block `.logItem` reports resolved (subgrid) tracks consistent with its siblings — not independent `max-content` per row. Assert via `getComputedStyle(item).gridTemplateColumns` returning the same track string for two block rows with different badge content.
- **Summary unaffected:** `summary.startBlock` still renders as a single full-width flex header with the chevron and the `✓N ✗N ⚠N ◆N` badge cluster (existing `BlocksWithStatus` assertions still pass).
- **is-block stripe intact:** The `.logItem.is-block::before` 3px orange stripe is present and does not shift content (a block row and a top-level row start their content at the same inline offset).
- **Edge — single-child block:** A block with one child row aligns that row's columns to the master template (no `max-content` collapse).
- **Edge — collapsed block:** Collapsing a `<details>` (toggle) hides children; reopening restores aligned columns. Existing toggle play test still passes.

**Verification:** At `>= 640px`, block rows and top-level rows present a single clean column grid (timestamp, severity, http, body, actions all line up). At `< 640px`, blocks stack as before. All existing `cts-log-viewer` play tests pass.

---

### U3. Storybook play-test coverage for block/non-block alignment

**Goal:** Lock the fix with an automated wide-layout story that fails if block children regress to independent/ragged grids.

**Requirements:** Regression guard for U2. Satisfies the project rule that all CTS web components carry Storybook play tests (`feedback_storybook_interaction_tests`).

**Dependencies:** U2.

**Files:**
- `src/main/resources/static/components/cts-log-viewer.stories.js` (add a wide-container story — mirror `BlocksWithStatus` but with a fixture mixing top-level rows and a block containing rows with *differing* badge widths, e.g. one row with no HTTP marker and one with a wide `REDIRECT` marker, so a per-row `max-content` regression would visibly diverge)

**Approach:**
- Render the viewer in a `>= 640px` container (default Storybook width is wide; assert the container query is active by checking a `.logItem` reports the 5-track grid, mirroring `MobileContainer`'s 2-track assertion inverted).
- In the play function, gather the body-cell `getBoundingClientRect().left` for each block child row and for a top-level row; assert equality (Approach A) or within-tolerance (Approach B, tolerance documented in the test).
- Reuse existing fixture-building helpers in the stories file; add a new `MOCK_*` fixture if needed for the mixed-width block.

**Patterns to follow:** `MobileContainer` and `BlocksWithStatus` plays in the same file; the URL-reset `beforeEach` convention (`feedback_storybook_story_url_pollution`); Lit marker normalization helper if any DOM snapshot is used (`feedback_lit_marker_snapshot_normalization`).

**Test scenarios:**
- **Alignment assertion (happy path):** block child rows share the body-column left offset and match the top-level row (per chosen approach's tolerance).
- **Differing-badge robustness:** two block rows with different-width HTTP/severity badges still align — the assertion that specifically catches a `max-content`-per-row regression.
- **Wide-layout precondition:** confirm the story is exercising the `>= 640px` branch (track count == 5), so the test cannot silently pass on the small layout.

**Verification:** `npm run test:ci` / story tests green for the new story; deliberately reverting U2 makes the new story fail. Be mindful of the documented pre-existing Storybook flake baseline (`feedback_storybook_pre_existing_flakes`) — reproduce on HEAD before attributing any unrelated failure to this change.

---

## System-Wide Impact

- **Affected surface:** the log-detail page (`log-detail.html` → `cts-log-viewer`), which is the only consumer of the block layout. No backend, API, or data changes.
- **Existing tests:** `cts-log-viewer.stories.js` block stories (`BlocksWithStatus`, `EmptyBlock`, toggle, polling) and `frontend/e2e/log-detail.spec.js` / `journeys.spec.js` exercise blocks. Re-run after the change; update only if DOM structure (not just CSS) shifts.
- **Cross-cutting CSS:** changes are scoped under `@container ctsLogViewer (min-width: 640px)` and under `.logBlock`/`.logEntries`. Guard against the new block rules leaking onto top-level rows or onto the small layout.

---

## Risks & Mitigations

- **R1 — Subgrid relay (Approach A) silently fails to propagate on one browser.** Mitigation: U1 verifies in-browser first; Approach B (concrete grid) is the committed default and does not depend on the relay. Cross-boundary perfection is explicitly deferred if A is not verifiable.
- **R2 — `<details>` as a grid container breaks `<summary>` collapse rendering.** Mitigation: KTD3 spans the summary across all tracks; preserve native `<details>` semantics (no change to the toggle handler or `?open` binding); the existing toggle play test is the guard.
- **R3 — Legacy `layout.css` global `.logItem` padding double-applies inside blocks.** Mitigation: KTD5 — verify scoped component padding wins; adjust specificity minimally if needed; full legacy cleanup deferred.
- **R4 — `is-block::before` stripe shifts content once `.logItem` joins the block grid.** Mitigation: KTD4 keeps it absolutely positioned over a `position: relative` anchor; explicit test scenario in U2.

---

## Verification Strategy

1. Storybook play tests (U3) green, including the new alignment story; reverting U2 fails it.
2. Existing `cts-log-viewer` block stories still pass.
3. `frontend/e2e/log-detail.spec.js` and `journeys.spec.js` pass (reproduce any failure on HEAD first per the flake-baseline memory).
4. Manual/visual: at `>= 640px`, block and non-block rows share one clean column grid; at `< 640px`, blocks stack unchanged. Screenshots in `tmp/screenshots/` attached to the PR.
5. `npm run test:ci` (format → lint → type-check → jsdoc → icons → lit-analyzer) clean.
