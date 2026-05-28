---
title: "refactor: De-collapse cts-log-viewer logBlock sections"
status: active
date: 2026-05-28
type: refactor
origin: memory/project_log_block_decollapse.md (investigation 2026-05-28)
---

# refactor: De-collapse cts-log-viewer logBlock sections

## Summary

The `cts-log-viewer` renders each run of related log entries as a collapsible
native `<details class="logBlock">` with a `<summary class="startBlock">`
header. The disclosure (collapse/expand) behaviour was adopted in U5 (commit
`35de1d42d`) only to get keyboard collapse "for free" — it was never a product
requirement and carries no SUS or accessibility recommendation behind it. This
plan removes the disclosure behaviour while keeping the two things that earn
their place: the **startBlock visual band** and the **`✓N ✗N ⚠N ◆N` per-block
count cluster**. Blocks become always-open, non-collapsible groupings.

The work is a bounded refactor across four surfaces: the viewer component, two
external scroll consumers (`log-detail.js`, `cts-log-toc.js`), the Storybook
stories, and the Playwright e2e spec.

---

## Problem Frame

`<details>`/`<summary>` couples three concerns into one element: (1) the visual
band, (2) the per-block status summary, and (3) an open/closed state machine.
Only (1) and (2) are wanted here. Concern (3) leaked real machinery across the
component and its consumers:

- `_collapsedBlocks` Set state + `_handleBlockToggle` + `?open=${…}` binding +
  `@toggle` handler, all to persist collapse choices across the 3s polling
  re-render.
- A `::details-content { display: contents }` CSS hack gated on `[open]` (recent
  commits `cacb75708`, `bcf79c7ba`) needed *because* `<details>` wraps its
  non-summary content in a UA pseudo-element that breaks the subgrid alignment
  relay.
- A chevron glyph that rotates on `[open]` — an affordance for a behaviour that
  is going away.
- An ancestor `tagName === "DETAILS"` walk in `_scrollToHashIfPresent()` (viewer)
  and `handleScrollToEntry()` (log-detail.js) that force-opens collapsed blocks
  before scrolling to a deep-linked entry.
- `details.logBlock` selectors in `cts-log-toc.js` (scroll-spy) and
  `log-detail.js` (`handleScrollToBlock`), plus `.open = true` calls.

Removing collapse lets us delete all of (3) and *simplify* the subgrid relay:
with no UA `::details-content` wrapper to neutralise, a plain
`<div class="logBlock">` subgrids directly with no `[open]`-gated hack.

**Not in scope / explicitly preserved:**
- The `cts-log-detail-header` config/test-info drawer `<details>` elements
  (Region C) are a *separate* component and a legitimate disclosure use — they
  stay collapsible and are untouched. (Verified: `cts-log-detail-header.js`
  renders its own `data-testid="drawer-test-details"` / `drawer-config`
  `<details>`; none are `.logBlock`.)
- The random per-block colours from the legacy `master` build
  (`randomColor({seed: blockId})`) are **not** resurrected — they had no
  semantic meaning and the count badges already supersede them (see origin memory).

---

## Requirements

- **R1.** Each block renders as a non-collapsible container that visually
  matches the current `.startBlock` band (light `--ink-100` surface, bold,
  padded) and shows the `✓N ✗N ⚠N ◆N` count cluster.
- **R2.** No collapse/expand interaction exists: no chevron affordance, no
  click/keyboard toggle, no persisted collapse state, no `[open]` attribute.
- **R3.** Block entries align with top-level entries on the master grid's
  column tracks (the subgrid relay keeps working), with the `::details-content`
  hack removed.
- **R4.** Deep-link scroll-to-entry (`#LOG-NNNN`) and TOC rail
  scroll-to-block still locate and scroll to their targets — entries are now
  always visible, so the "open the collapsed ancestor first" step is dropped,
  not reimplemented.
- **R5.** The block header is presentational (no `role`, not focusable, no
  pointer cursor) since it is no longer a control.
- **R6.** Stories and e2e are updated to assert the new non-collapsible
  structure; obsolete collapse tests are removed, alignment/aggregation tests
  are retained with updated selectors.

---

## Key Technical Decisions

- **KTD1 — Block element: `<div class="logBlock">` with a header
  `<div class="startBlock">`.** A plain `<div>` is the lowest-risk choice: it
  satisfies the `display: grid; grid-template-columns: subgrid` relay without
  imposing an accessible-name obligation. A `<section>` would be semantically
  "a labelled region" but an unnamed `<section>` adds landmark noise to the a11y
  tree without benefit; the count badges + TOC rail already provide navigation.
  Keep `data-block-id` on the container (consumers query it).
- **KTD2 — Drop the chevron entirely.** A chevron communicates "this expands."
  With no expansion it is misleading. Removing it also deletes the rotation CSS.
- **KTD3 — Simplify the subgrid relay, don't just leave it.** Without
  `<details>` there is no UA `::details-content` wrapper, so the
  `[open]::details-content { display: contents }` rule is deleted. The
  `.logBlock` keeps `grid-column: 1 / -1; display: grid;
  grid-template-columns: subgrid; column-gap: var(--space-3)` and the header
  keeps `grid-column: 1 / -1`. Net: the relay gets *simpler* and more robust
  (the prior comment called nested-subgrid the blocker; the real blocker was the
  wrapper, which now does not exist).
- **KTD4 — Keep `_blockCounts` / `_blockSummaries` / `_aggregateBlockCounts`
  untouched.** Aggregation is concern (2) and stays exactly as-is. Only the
  render shape and the disclosure state change.
- **KTD5 — `_scrollToHashIfPresent()` keeps the hash parse + `scrollIntoView`
  (with reduced-motion handling) and drops the entire `DETAILS` ancestor walk
  and `_collapsedBlocks` resync.** Same for `log-detail.js` `handleScrollToEntry`
  (drop the walk) and `handleScrollToBlock` (selector + drop `.open`).

---

## High-Level Technical Design

Before → after for one block's rendered shape:

```text
BEFORE (collapsible)                    AFTER (de-collapsed)
─────────────────────                   ────────────────────
<details.logBlock [open] @toggle        <div.logBlock data-block-id>
        data-block-id>                     <div.startBlock>            ← presentational
  <summary.startBlock>          ──▶           <span.startBlockMsg>…    (no chevron,
    <cts-icon chevron-down>                    <span.startBlockCounts> ✓N ✗N ⚠N ◆N
    <span.startBlockMsg>…                   </div>
    <span.startBlockCounts> ✓N…          <cts-log-entry>…             ← direct subgrid
  </summary>                              <cts-log-entry>…                children
  <cts-log-entry>… (wrapped in           </div>
   UA ::details-content pseudo)
</details>

State removed: _collapsedBlocks, _handleBlockToggle, ?open binding, @toggle.
CSS removed:   ::details-content[open] hack, chevron rotation, ::marker resets,
               cursor:pointer, summary :focus-visible ring.
Consumers:     details.logBlock → .logBlock ; drop all `.open = true`.
```

---

## Implementation Units

### U1. De-collapse the block markup and CSS in `cts-log-viewer.js`

**Goal:** Render blocks as non-collapsible `<div class="logBlock">` + header
`<div class="startBlock">`, remove all disclosure state/handlers, drop the
chevron, and simplify the subgrid CSS — while keeping the band and count cluster.

**Requirements:** R1, R2, R3, R5.

**Dependencies:** none.

**Files:**
- `src/main/resources/static/components/cts-log-viewer.js` (modify)
- `src/main/resources/static/components/cts-log-viewer.stories.js` (covered in U3 — render-shape change is asserted there; component-scoped tests live in stories)

**Approach:**
- **Render (`_renderEntries` / `flushBlock`):** replace the `<details … ?open
  @toggle>` template with `<div class="logBlock" data-block-id=${currentBlockId}>`,
  and the `<summary class="logItem startBlock">` with
  `<div class="startBlock">`. Drop the `<cts-icon name="chevron-down">`. Keep
  `.startBlockMsg` and `.startBlockCounts` spans and the `${blockChildren}`.
  Drop the `isCollapsed` lookup.
- **State:** remove `_collapsedBlocks` from `static properties` and the
  constructor; delete `_handleBlockToggle`.
- **`_scrollToHashIfPresent`:** keep the hash regex, `getElementById`, and the
  reduced-motion `scrollIntoView`. Delete the `while (parent) … tagName ===
  "DETAILS" … details.open = true … _collapsedBlocks` block (KTD5). The function
  still returns `true`/`false` on found/not-found.
- **CSS (`STYLE_TEXT`):**
  - Delete the `[open]::details-content { display: contents }` rule and its long
    comment (the wrapper no longer exists). Keep the `.logBlock` subgrid rule
    (`grid-column: 1 / -1; display: grid; grid-template-columns: subgrid;
    column-gap: var(--space-3)`), and rename the selector targets from
    `> .logBlock > summary.startBlock` to `> .logBlock > .startBlock` and
    `> .logBlock > cts-log-entry` (unchanged).
  - In the `.startBlock` rule: remove `cursor: pointer`, `list-style: none`, and
    the `border: 0` is harmless to keep or drop. Delete the
    `::-webkit-details-marker` and `::marker` reset rules and the
    `.startBlock:focus-visible` ring (the header is no longer focusable).
  - Keep `.startBlock:hover { background: var(--ink-200) }`? **Decision:** remove
    it — hover feedback signals interactivity that no longer exists. (Keeps the
    band static like a label.) Note this in the commit so the
    `layout.css .logItem:hover` override rationale is understood as obsolete here.
  - Delete the chevron rules (`.logBlock > .startBlock cts-icon { transition }`
    and `.logBlock:not([open]) > .startBlock cts-icon { transform }`).
  - Keep the `scroll-margin-top: 70px` rule (the `.logBlock` is still the TOC
    scroll target).
  - Update the leading comment block on `.startBlock` (lines ~223–225) to drop
    the "keyboard collapse semantics for free" framing.
- Update the class JSDoc (lines ~292–296) — replace "Supports collapsible
  blocks" with "Groups entries into non-collapsible blocks."

**Patterns to follow:** the existing `_renderResultSummary` / `_renderBlockBadges`
templates; light-DOM `createRenderRoot` + injected `STYLE_TEXT` pattern already
in the file.

**Test scenarios** (assertions land in U3 stories — this unit is the
implementation those tests drive):
- A block renders as `div.logBlock[data-block-id]`, not `<details>`.
- The header renders as `div.startBlock` with `.startBlockMsg` text and a
  `.startBlockCounts` cluster whose badges match the aggregated counts.
- No `<cts-icon>` chevron inside `.startBlock`.
- The header is not focusable (`tabIndex === -1` / not in tab order) and has no
  `role`.
- Block children (`cts-log-entry`) are present and visible (not `display:none`).
- `_collapsedBlocks` and `_handleBlockToggle` no longer exist on the instance.

**Verification:** Storybook (U3) renders blocks as always-open divs; no console
warnings; `lint:lit-analyzer` clean (no unknown bindings left behind).

---

### U2. Update external scroll consumers (`log-detail.js`, `cts-log-toc.js`)

**Goal:** Point the scroll-to-entry, scroll-to-block, and scroll-spy code at the
new `.logBlock` div and remove the now-impossible "open the collapsed block
first" steps.

**Requirements:** R4.

**Dependencies:** U1 (markup shape).

**Files:**
- `src/main/resources/static/js/log-detail.js` (modify — `handleScrollToEntry`
  ~lines 941–948, `handleScrollToBlock` ~lines 959–969)
- `src/main/resources/static/components/cts-log-toc.js` (modify — `_setupScrollSpy`
  ~line 293)

**Approach:**
- **`handleScrollToEntry`:** delete the `while (ancestor … tagName !== "DETAILS")
  … ancestor.open = true` lines (945–946) and the obsolete U5 comment. Keep the
  `cts-log-entry[data-entry-id]` lookup and the `scrollIntoView`. The entry is
  always visible now.
- **`handleScrollToBlock`:** change the selector from
  `details.logBlock[data-block-id="…"]` to `.logBlock[data-block-id="…"]`; delete
  `target.open = true` (967). Keep `scrollIntoView`. Update the JSDoc that
  references "the matching `<details>` … opens before scrolling."
- **`cts-log-toc.js` `_setupScrollSpy`:** change
  `document.querySelectorAll("details.logBlock")` to
  `document.querySelectorAll(".logBlock")` (the `.dataset.blockId` filter stays).

**Patterns to follow:** existing query-by-attribute + `scrollIntoView` style in
both handlers; the `.replace(/"/g, '\\"')` selector-escaping idiom is kept.

**Test scenarios:**
- *Integration (e2e in U4):* clicking a TOC rail row scrolls the matching block
  into view (block is already visible — no open step needed).
- *Integration (e2e in U4):* a failure-summary jump-link to an entry inside a
  block scrolls that entry into view.
- *Integration (e2e in U4):* `#LOG-NNNN` hash navigation scrolls to the entry.
- Scroll-spy registers an `IntersectionObserver` target per `.logBlock` with a
  `blockId` (verified indirectly via the active-rail-highlight e2e if present;
  otherwise covered by U3 story for the rail).

**Verification:** e2e (U4) deep-link and rail-jump tests pass against
always-visible blocks.

---

### U3. Update Storybook stories (`cts-log-viewer.stories.js`)

**Goal:** Replace collapse-behaviour assertions with non-collapsible structure
assertions; retain and reframe the subgrid-alignment regression test without the
`::details-content` dependency.

**Requirements:** R6.

**Dependencies:** U1.

**Files:**
- `src/main/resources/static/components/cts-log-viewer.stories.js` (modify)

**Approach:**
- Replace `canvasElement.querySelectorAll("details.logBlock")` /
  `firstBlock.open` assertions (~lines 56–69, 285, 347–365) with
  `.logBlock` selectors and "block is a div, children are visible" assertions.
- Delete the collapse-interaction play function (~lines 585–626: "toggle handler
  syncs `_collapsedBlocks`") and its narrative comment — that behaviour is gone.
- Reframe the subgrid-alignment story (~lines 678–768): keep the column-
  alignment assertion (block-row message column lines up with top-level rows),
  but remove the `.open = true/false` toggling and the `::details-content`
  narrative; assert alignment directly on the always-open block.
- Add/adjust a play function asserting the header has no chevron and is not
  focusable (covers U1 R2/R5).
- Update story-file JSDoc/comments referencing `<details>` collapse.

**Patterns to follow:** existing `play` + `expect`/`waitFor` story conventions;
the project's Storybook interaction-test requirement (every cts-* component has
play-function tests); `getNormalizedInnerHTML` helper if any snapshot is touched
(Lit marker normalization).

**Test scenarios:**
- Block renders as `div.logBlock`; `querySelectorAll("details.logBlock")` is
  empty.
- `.startBlock` header shows message + count badges; contains no chevron icon.
- Header is not focusable.
- Subgrid alignment: a block child's message-column left edge matches a
  top-level row's message-column left edge (the retained alignment regression).

**Verification:** `run-story-tests` for `cts-log-viewer` stories pass; broad
Storybook pass shows no *new* failures vs. the documented feat/redesign baseline
(12 pre-existing flakes per memory — reproduce on HEAD before attributing).

---

### U4. Update Playwright e2e (`log-detail.spec.js`)

**Goal:** Update block selectors, remove collapse-specific tests, and rewrite the
two "opens a collapsed block" tests as plain scroll-into-view tests.

**Requirements:** R6.

**Dependencies:** U1, U2.

**Files:**
- `frontend/e2e/log-detail.spec.js` (modify)

**Approach:**
- **U5 aggregation test (~838–866):** change `details[data-block-id="…"]` locators
  to `[data-block-id="…"]` (or `.logBlock[data-block-id="…"]`). The
  `.startBlockCounts cts-badge` assertions stay — aggregation is unchanged.
- **"clicking a block summary collapses the children via `<details>`" (~1052):**
  delete this test outright — the behaviour no longer exists.
- **"failure-summary jump-link opens a collapsed block and scrolls the entry into
  view" (~1073):** rewrite as "failure-summary jump-link scrolls the entry into
  view." Remove the pre-condition collapse click (1105–1108) and the
  `.open` polling (1122); assert the target entry is visible and scrolled. Update
  the comment block (1077–1079).
- **"U6: hash navigation opens a collapsed `<details>` ancestor before scrolling"
  (~1219):** rewrite as "U6: hash navigation scrolls to the entry." Drop the
  collapse precondition and `.open` assertions; assert `scrollIntoView` lands the
  entry (visible + roughly in viewport).
- Update the top-of-file coverage comments (~42–43, 838) that describe
  `<details>` collapse expectations.

**Patterns to follow:** existing `page.locator(...).poll(...)` and
`toBeAttached`/`toBeVisible` idioms in this spec; the e2e route-setup ordering
rule (`setupFailFast()` first, routes before `goto`).

**Test scenarios:**
- Three blocks render as `[data-block-id]` elements with correct count badges
  (retained, selector updated).
- Failure-summary jump-link scrolls the target entry into view (no collapse step).
- `#LOG-NNNN` hash navigation scrolls the target entry into view.
- (Removed) the dedicated collapse test no longer exists.

**Verification:** `cd frontend && ./node_modules/.bin/playwright test
e2e/log-detail.spec.js` passes; cross-check against the documented e2e baseline
(12 e2e flakes on feat/redesign HEAD per memory — reproduce on HEAD before
attributing any failure to this change).

---

## System-Wide Impact

- **Deep-link contract preserved:** `#LOG-NNNN` and TOC `cts-scroll-to-block`
  still work; they get *simpler* because targets are always visible.
- **`plan-detail.html` LOG-NNNN shim** (the inline ordinal computation mirrored
  from `_buildReferences`) is unaffected — this plan does not touch reference-ID
  computation, only block disclosure.
- **`cts-log-detail-header` drawers** are a separate disclosure surface and stay
  collapsible — no change.
- **Accessibility:** net improvement — a non-interactive label correctly stops
  advertising itself as a control (removes a focusable summary that did nothing
  semantically meaningful for screen-reader users beyond collapse).

---

## Risks & Dependencies

- **Risk: a hidden consumer still queries `details.logBlock` or `summary.startBlock`.**
  Mitigation: grep the full `static/` tree and `frontend/e2e/` for
  `details.logBlock`, `summary.startBlock`, `\.logBlock`, and `_collapsedBlocks`
  before declaring done; U2 already covers the three known queriers
  (`log-detail.js` ×2, `cts-log-toc.js` ×1).
- **Risk: subgrid alignment regresses when the `::details-content` rule is
  removed.** Mitigation: U3 retains the alignment regression story (reframed);
  visual check in the running app on a multi-block module.
- **Risk: baseline flakes masking real failures.** Mitigation: per memory,
  reproduce the documented Storybook (12) and e2e (12) baselines on HEAD before
  attributing any red to this change.
- **Dependency:** none external; all four units are in-repo frontend changes.

---

## Verification Strategy

1. `cd frontend && npm run test:ci` (format/lint/type-check/jsdoc/icons/lit-analyzer) clean.
2. `run-story-tests` for `cts-log-viewer` green (modulo documented baseline flakes).
3. `./node_modules/.bin/playwright test e2e/log-detail.spec.js` green (modulo baseline).
4. Manual: load a multi-block log in the running app; confirm blocks show the
   band + counts, do not collapse on click, deep-links scroll, TOC rail jumps land,
   and column alignment holds.
