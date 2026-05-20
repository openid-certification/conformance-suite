---
title: "fix: cts-log-toc empty rail still renders on log-detail"
type: fix
status: active
created: 2026-05-20
---

# fix: cts-log-toc empty rail still renders on log-detail

## Problem Frame

On `log-detail.html` for an interrupted or never-started test (e.g.
`https://localhost.emobix.co.uk:8443/log-detail.html?log=pcVtgTbyQ9Zzt8v`),
the wide-viewport "Test structure" rail (`#ctsLogToc`) appears as an
empty card showing only the "TEST STRUCTURE" heading and nothing else.

The component is doing the right work — it correctly toggles the
`hidden` attribute when both `blocks` and `failures` are empty
(see `_applyVisibility()` in
`src/main/resources/static/components/cts-log-toc.js`). Live diagnosis
on the broken URL confirmed:

- `rail.hasAttribute("hidden") === true`
- `getComputedStyle(rail).display === "block"` (should be `"none"`)

Root cause: the component's own scoped stylesheet declares
`cts-log-toc { display: block; ... }`, and a regular type-selector
beats the user-agent `[hidden] { display: none }` rule on specificity.
The `hidden` attribute is therefore visually inert at viewports
≥ 1440 px.

There is also a documentation/CSS mismatch on the page layout: the
inline comment in `src/main/resources/static/log-detail.html` (line 90)
states the grid uses a `:has(#ctsLogToc:not([hidden]))` guard so the
320 px column collapses when the rail hides — but the actual CSS rule
at line 98 has no such guard. Even after fixing the component, the
grid still reserves the 320 px slot, leaving visible empty space.

## Scope

In scope:

- Make `cts-log-toc[hidden]` actually hide on screen.
- Make `.log-page--with-toc` only become a two-column grid when the
  rail is unhidden, matching the comment that already describes this.
- Confirm via the live page that the rail no longer paints for an
  empty/interrupted test and the main content reclaims the full width.

Out of scope:

- Any change to when the rail decides to hide itself
  (`_applyVisibility` already gets this right).
- Changes to the empty-state copy or any new "no structure yet"
  placeholder — the correct behaviour for an empty rail is to
  disappear, not to render an empty card.
- Wider refactors of the U8 rail, the grid layout, or any of the
  triad components.

### Deferred to Follow-Up Work

- None.

## Requirements

R1. When `cts-log-toc.blocks` and `cts-log-toc.failures` are both empty
    (or when the user preference disables the rail), the element
    occupies zero pixels on screen — `getComputedStyle(rail).display`
    is `"none"`, not `"block"`.

R2. When the rail is hidden, the page layout reclaims the 320 px
    column it would otherwise reserve — at ≥ 1440 px the
    `.log-page--with-toc` grid collapses back to a single column, so
    the main content uses the full page width.

R3. When `blocks` later arrives via `cts-blocks-updated`, the rail
    reappears and the grid re-expands — no re-mount, no manual
    refresh.

R4. Existing story-driven contracts continue to hold:
    `Default`, `ClickDispatchesScrollEvent`, `WithFailures`,
    `ActiveBlockHighlight`, `EmptyDuringWaiting`,
    `ReappearsWhenBlocksArrive`, and `PreferenceTogglesVisibility`
    must still pass.

## Key Technical Decisions

- **Add `cts-log-toc[hidden] { display: none; }` to the component's
  scoped stylesheet** (in `STYLE_TEXT`) rather than relying on the
  bare `[hidden]` UA rule. This keeps the visibility contract
  colocated with the rest of the rail's own CSS and avoids the
  specificity trap silently re-emerging if anyone ever raises the
  base `display` rule's specificity again.

  Alternative considered: wrap the base rule in `:where(cts-log-toc)`
  so it stays at specificity 0. Rejected — `:where()` would also
  knock out the unrelated sticky/border/box-shadow declarations from
  any future override the page might want to apply, which is a
  bigger surface than the actual problem.

- **Add the `:has(#ctsLogToc:not([hidden]))` guard to
  `.log-page--with-toc`** in `log-detail.html`, matching what the
  surrounding comment already documents. `:has()` is available in all
  evergreen browsers the CTS frontend supports, so no fallback
  needed.

  Alternative considered: keep the grid two-column unconditionally
  and rely solely on the hidden rail collapsing visually. Rejected —
  the grid would still gap-pad a dead 320 px column to the right of
  the entries stream, which is visible whitespace even when the rail
  itself paints nothing.

## Implementation Units

### U1. Make `cts-log-toc[hidden]` visually hide

- **Goal:** Restore the `hidden` attribute's `display: none` semantics
  on the rail so an empty/interrupted-test view renders nothing where
  the empty card currently sits.
- **Requirements:** R1, R3, R4.
- **Dependencies:** none.
- **Files:**
  - `src/main/resources/static/components/cts-log-toc.js`
    (modify `STYLE_TEXT`)
  - `src/main/resources/static/components/cts-log-toc.stories.js`
    (extend `EmptyDuringWaiting` and `ReappearsWhenBlocksArrive`)
- **Approach:**
  - Add a `cts-log-toc[hidden] { display: none; }` rule to the
    `STYLE_TEXT` constant, placed immediately after the base
    `cts-log-toc { display: block; ... }` block so the override is
    visible to a reader scanning the stylesheet top-down.
  - Do NOT change `_applyVisibility()` — it is already correct.
- **Patterns to follow:**
  - The scoped-stylesheet injection pattern in
    `cts-log-toc.js` (`ensureStylesInjected()` + `STYLE_TEXT`).
  - Other components in the codebase keep the override colocated
    with the base declaration (see `cts-badge` and `cts-modal` for
    similar `[attribute] { display: ... }` pairings).
- **Test scenarios** (extend existing stories — keep the file as the
  single home for cts-log-toc tests):
  - `EmptyDuringWaiting`: in addition to the existing
    `hasAttribute("hidden") === true` assertion, assert
    `getComputedStyle(rail).display === "none"`. This is the
    regression guard for the live bug.
  - `ReappearsWhenBlocksArrive`: after assigning `rail.blocks =
    BLOCKS`, assert `getComputedStyle(rail).display === "block"`
    alongside the existing `hasAttribute("hidden") === false`
    assertion. Verifies the override is keyed only on `[hidden]`
    and does not leak into the populated state.
- **Verification:**
  - `cd frontend && ./node_modules/.bin/playwright test -g "cts-log-toc"`
    via the Storybook runner is green.
  - Manually loading the bug URL on the dev server shows no empty
    "TEST STRUCTURE" card at viewport ≥ 1440 px.

### U2. Make the grid drop the rail column when the rail hides

- **Goal:** Stop the page layout from reserving a 320 px column for
  a hidden rail, matching what the existing comment already promises.
- **Requirements:** R2, R3, R4.
- **Dependencies:** none (this works without U1 but is most useful
  paired with it).
- **Files:**
  - `src/main/resources/static/log-detail.html`
    (modify the `.log-page--with-toc` rule)
  - `frontend/e2e/log-detail.spec.js` (add a regression assertion;
    the file already exists for log-detail e2e coverage — confirm
    via `ls frontend/e2e/`)
- **Approach:**
  - Replace the existing `@media (min-width: 1440px) {
    .log-page--with-toc { display: grid; ... } }` block with one
    whose selector is
    `.log-page--with-toc:has(#ctsLogToc:not([hidden]))`.
  - Leave the rest of the rule's declarations
    (`grid-template-columns`, `gap`, `align-items`) unchanged.
- **Patterns to follow:**
  - The existing `@media (max-width: 1439px) { #ctsLogToc {
    display: none; } }` rule a few lines below (same file) — the
    pair of media queries is the canonical place for rail-related
    page layout, so the `:has()` guard belongs in the wide-viewport
    half of that pair.
- **Test scenarios** (Playwright e2e against the static page, with
  the API mocked so /api/info returns a results-less interrupted
  test like the live bug repro):
  - Load `log-detail.html?log=<id>` with /api/log mocked to return
    only INFO + FAILURE + INTERRUPTED rows (no `startBlock` entries)
    and /api/info mocked to omit `results`. Assert that
    `#main-content` has `grid-template-columns: none` (or some other
    falsy projection) AND that `#ctsLogToc` reports
    `offsetParent === null`.
  - Load the same page with /api/log returning two `startBlock`
    rows. Assert that `#main-content` has a two-column
    `grid-template-columns` value and that `#ctsLogToc` is visible.
  - Re-use existing fixture patterns under `frontend/e2e/fixtures/`
    rather than inventing new fixture shapes.
- **Verification:**
  - `cd frontend && ./node_modules/.bin/playwright test
    e2e/log-detail.spec.js` is green.
  - Manually loading the bug URL shows the main content occupying
    the full page width.

## System-Wide Impact

- **Other pages reading `cts-log-toc`:** none. The component is only
  mounted on `log-detail.html`. Storybook stories exercise it but
  do not embed it in the live `.log-page` grid, so the U2 grid
  change has no effect there.
- **CSS specificity in other components:** the same `display` /
  `[hidden]` trap exists in any light-DOM Lit component that gives
  itself an explicit `display`. This plan does not sweep the rest of
  the suite — see "Deferred to Follow-Up Work" if a future ticket
  wants to audit them.
- **Storybook contracts:** the existing assertions only checked the
  `hidden` attribute. After U1 they also pin the computed style, so
  any future regression of this exact class fails fast in CI.

## Risks

- **Specificity creep on the rail's CSS.** If someone later wraps the
  base `cts-log-toc` rule in something more specific than a type
  selector, the `[hidden]` override may need a matching bump.
  Mitigated by colocating the two rules in `STYLE_TEXT` so the next
  reader sees them together.
- **`:has()` browser support.** All four evergreen browsers in the
  CTS support policy (Chrome, Safari, Firefox, Edge) ship `:has()`.
  Older browsers are explicitly out of scope per
  `feedback_browser_support_policy.md` in user auto-memory.
- **Story coverage gap.** Before this plan, the storybook play
  function checked the attribute but not the computed display, so
  the live bug shipped with green tests. U1 closes that gap.

## Verification (overall)

1. `cd frontend && npm run test:ci` — passes (no lint/typecheck/
   story regressions).
2. `cd frontend && ./node_modules/.bin/playwright test
   e2e/log-detail.spec.js` — passes (U2's new e2e assertions are
   green).
3. On the dev server, navigating to the bug URL
   `https://localhost.emobix.co.uk:8443/log-detail.html?log=pcVtgTbyQ9Zzt8v`
   shows no "TEST STRUCTURE" card and the main content takes the
   full page width at ≥ 1440 px.
4. Navigating to a healthy log URL (any test with real blocks) still
   shows the populated rail beside the entries stream and the
   active-row highlight still tracks scroll.
