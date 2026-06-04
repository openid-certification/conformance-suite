---
title: "fix: Account-menu item hover background overshoots menu width"
type: fix
status: completed
date: 2026-06-04
---

# fix: Account-menu item hover background overshoots menu width

## Summary

In the navbar's user dropdown (the dark popover with the user's email, `{sub, iss}` line, "Tokens", and "Sign out"), hovering a menu item paints a background rectangle that extends past the menu's right edge. Root cause: `.cts-account-item` in `src/main/resources/static/components/cts-navbar.js` sets `width: 100%` plus `12px` horizontal padding with no `box-sizing: border-box`. The redesign removed Bootstrap's global `* { box-sizing: border-box }` reset and there is no replacement reset in `css/oidf-tokens.css` / `css/layout.css` / `css/oidf-app.css`, so the item defaults to `content-box` and renders 24px wider than the menu's content area. The menu container (`.cts-account-menu`) has no `overflow: hidden`, so the over-wide hover fill is visible. The fix adds the missing `box-sizing: border-box` declaration, matching the established per-element convention, plus a Storybook geometry regression test.

## Requirements

- R1. Hovering or focusing "Tokens" in the account menu paints its background entirely within the menu's bounds (no overshoot past the menu border).
- R2. Same for "Sign out" (`.cts-account-item--danger` button — identical box-model defect via the shared class).
- R3. Menu items still span the menu's full inner width edge-to-edge (the fix must shrink the overshoot, not the intended fill).
- R4. A Storybook play-function regression test asserts menu-item containment within the menu box.

## Key Technical Decisions

- **Fix with `box-sizing: border-box` on `.cts-account-item`, not `overflow: hidden` on `.cts-account-menu`.** Border-box corrects the geometry; overflow-hidden would only mask it (the item would still be 24px too wide, with its right padding clipped, and future shadows/focus rings would clip too). Per-element `box-sizing: border-box` is the repo's established convention for `width: 100%` + padding elements in light-DOM components — precedents: `src/main/resources/static/components/cts-button.js` (~line 111), `cts-link-button.js` (~line 93), `cts-plan-list.js` (~lines 84, 118), `cts-form-field.js` (~line 107).
- **Regression test asserts box geometry, not paint.** The hover background paints the item's padding box, so the testable invariant is containment: the item's bounding rect must sit within the menu's bounding rect. A play-function `getBoundingClientRect()` comparison catches the bug deterministically without visual-snapshot infrastructure. `.cts-account-item` is a real box (not `display: contents`), so rect measurement is trustworthy here (cf. `docs/solutions/web-components/subgrid-alignment-inside-details-blocks-2026-05-28.md` on box-less measurement pitfalls).

## Assumptions

- The fix is scoped to `cts-navbar` per the reported bug; the structurally identical latent defect in `cts-action-overflow.js` is deferred (see Scope Boundaries) rather than fixed in the same change.
- No global `box-sizing` reset is introduced — adopting one is an architectural decision with page-wide blast radius, out of proportion to this fix.

## Implementation Units

### U1. Add `box-sizing: border-box` to `.cts-account-item` and regression-test containment

- **Goal:** The account-menu items' hover/focus background stays inside the menu's bounds while still filling the menu's inner width.
- **Requirements:** R1, R2, R3, R4
- **Dependencies:** none
- **Files:**
  - `src/main/resources/static/components/cts-navbar.js` — add `box-sizing: border-box;` to the `.cts-nav .cts-account-item` rule (the rule block around lines 374-402 that sets `width: 100%` and `padding: var(--space-2) var(--space-3)`).
  - `src/main/resources/static/components/cts-navbar.stories.js` — extend the existing `AccountMenuOpens` play function (or add a sibling story) with containment assertions.
- **Approach:** One-declaration CSS fix inside the component's injected stylesheet (`injectStyles()` pattern — `cts-navbar` is light DOM via `createRenderRoot() { return this; }`, so its styles are page-level and the box-model default comes from the page, which has no reset). While in the rule block, audit the other `width: 100%` + padding selectors inside `cts-navbar.js`'s stylesheet for the same defect and fix any found in the same commit — same-component instances of the same bug are in scope; other components are not.
- **Patterns to follow:** `cts-button.js`, `cts-link-button.js`, `cts-plan-list.js`, `cts-form-field.js` — all declare `box-sizing: border-box` locally on full-width padded elements.
- **Test scenarios:** (one consistent measurement basis: `getBoundingClientRect()` on both the item and the menu; tolerance ±2px absorbs sub-pixel rounding)
  - Happy path (Tokens): with a logged-in user (Tokens visible), open the account menu via `.cts-account-trigger`; assert the "Tokens" item's rect satisfies `item.right <= menu.right` and `item.left >= menu.left`.
  - Happy path (Sign out): assert the same two rect comparisons for the `.cts-account-item--danger` button.
  - Fill check (guards R3): for each item, assert the item's rect width is within 2px of `menu rect width − 2×8px padding − 2×1px border` (8px = `--space-2` menu padding per side, 1px = menu border per side) — catches an over-correction that would leave items shrink-wrapped.
  - Edge case: render the story with a long principal (e.g., a 60+ character email) and repeat the containment and fill assertions with wrapped header content. (Implementation note: the menu cannot grow past its `min-width: 240px` — the absolutely-positioned popover shrink-wraps against the avatar-sized `.cts-account` containing block and all header content wraps via `word-break: break-all` — so the long principal exercises multi-line wrapping at min-width, not a wider menu.)
- **Verification:** Storybook tests for `cts-navbar` pass including the new containment and fill assertions (primary verification of the fix); visual check in the Storybook preview shows the hover fill flush inside the menu's rounded border on both items; the full frontend e2e suite still passes (secondary — no e2e spec covers the account menu's geometry, so a green e2e run only confirms no collateral regression elsewhere; baseline is 0 failures).

## Scope Boundaries

### Deferred to Follow-Up Work

- `src/main/resources/static/components/cts-action-overflow.js` — `.overflowItem` (~lines 71-91) has the structurally identical defect (`width: 100%` + padding, no `box-sizing`, container `.overflowPopover` without `overflow: hidden`). Same one-line fix and test shape apply; deferred because it is outside the reported bug's confirmed scope.
- A global `box-sizing: border-box` reset for the redesigned pages (would eliminate this bug class wholesale; needs its own blast-radius review across all legacy pages and components).
- `.cts-account-item:focus-visible` relies on the background fill alone as its focus indicator (the existing rule sets `outline: none`). Whether a fill-only indicator meets WCAG 2.4.11 Focus Appearance is a pre-existing question this fix does not change — review it separately rather than expanding this fix's scope.

### Non-goals

- Any visual redesign of the account menu (spacing, colors, radii stay as-is).
- Changes to menu behavior (open/close, Escape, outside-click — covered by existing stories and untouched).

## Sources

- Diagnosis: `cts-navbar.js` `_renderAccount()` (~lines 791-843) renders the menu; `.cts-account-menu` container rule (~lines 312-336); `.cts-account-item` rule (~lines 374-402) with `width: 100%` (~line 377), `padding` (~line 378), hover background `var(--ink-700)` (~line 393). Token values in `src/main/resources/static/css/oidf-tokens.css` (`--space-2: 8px`, `--space-3: 12px`).
- Prior art on light-DOM styling hazards: `docs/solutions/web-components/cts-button-host-vs-inner-button-semantics-2026-04-17.md` (light-DOM components are styled by page CSS; no shadow boundary).
- Load-bearing detail for the "Sign out" assertions: the button is not a direct flex child of the menu — it sits inside `.cts-account-form`, which has `margin: 0; padding: 0` and no border, so the form's content box equals the menu's inner width and the button's `width: 100%` resolves against it. Padding or border added to `.cts-account-form` later would reintroduce the overshoot for "Sign out" only, which is why both items carry their own containment/fill assertions.
- Existing test touchpoints: `cts-navbar.stories.js` `AccountMenuOpens` / `AccountMenuClosesOnEscape` / `AccountMenuClosesOnOutsideClick` (~lines 402-501); no dedicated navbar e2e spec (`frontend/e2e/plans.spec.js` only asserts trigger visibility).
