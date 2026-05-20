---
status: active
depth: lightweight
type: fix
created: 2026-05-20
---

# fix: Remove cts-tooltip arrow and reduce offset

## Summary

Tone down `cts-tooltip` to match the modern arrow-less convention used by Notion, Linear, and similar tools. The chevron arrow rendered under each tooltip is removed entirely, and the gap between the tooltip and its trigger shrinks from 8px to 4px so the affordance sits closer to what the user is pointing at.

## Problem Frame

The current `cts-tooltip` primitive renders a CSS-border chevron arrow centered against its trigger and offsets the tooltip body by 8px to make room for it. Two visual issues:

1. The arrow adds visual noise without functional value — the tooltip's proximity to its trigger already communicates the relationship.
2. The 8px offset (sized to accommodate the arrow's 6px half-width) leaves the tooltip floating noticeably away from the trigger, which reads as imprecise next to denser modern UIs.

Both knobs live in one place — `src/main/resources/static/components/cts-tooltip.js` — and are referenced nowhere else in the app code. The Storybook tests cover hover/focus/dismiss/placement/auto-flip/reposition behavior but do not assert on the arrow element or the exact offset distance, so the change is mechanically safe.

## Scope Boundaries

**In scope**
- Remove the `oidf-tooltip__arrow` element creation, the four `.oidf-tooltip[data-placement="..."] .oidf-tooltip__arrow` style rules, the `ARROW_HALF_PX` constant, and the `arrowLeft` / `arrowTop` per-placement computations in `_position()`.
- Reduce `OFFSET_PX` from `8` to `4`.
- Update the JSDoc preamble line that references the chevron arrow size so the comment doesn't go stale.

**Out of scope**
- Animation, fade-in, or hide-delay tuning — the show/hide UX is unchanged.
- Color, typography, radius, or shadow changes to the tooltip body.
- Any call-site changes — no consumer references the arrow class or `OFFSET_PX` directly.

### Deferred to Follow-Up Work

None.

## Key Technical Decisions

- **Delete the arrow code outright; do not keep a feature flag.** The arrow is purely cosmetic and the design direction is unambiguous. A flag would be dead code on day one.
- **4px offset, not 0px.** Zero would touch the trigger and read as a styling bug. 4px maps to `--space-1` in the token system (the same unit used elsewhere for tight visual gaps) and keeps the tooltip clearly detached without floating.
- **No need to renumber `_resolvePlacement()` spacing checks.** The `tipRect.height + OFFSET_PX` / `tipRect.width + OFFSET_PX` checks continue to work with the smaller offset — they just become slightly more generous, which is fine for an auto-placement heuristic.

## Implementation Units

### U1. Remove arrow rendering and shrink offset

- **Goal:** Strip the chevron arrow from `cts-tooltip` and bring the tooltip body closer to its trigger.
- **Requirements:** Match the design direction stated in the task ("remove the arrow pointer on tooltips, and reduce the offset distance").
- **Dependencies:** None.
- **Files:**
  - `src/main/resources/static/components/cts-tooltip.js` (modify)
  - `src/main/resources/static/components/cts-tooltip.stories.js` (only if a new arrow-absence story is added; otherwise untouched — existing stories already pass)
- **Approach:**
  - Change `const OFFSET_PX = 8;` to `const OFFSET_PX = 4;`. Update the leading comment so it no longer claims the offset "matches the chevron arrow size".
  - Delete `const ARROW_HALF_PX = 6;` and its leading comment.
  - In `injectStylesOnce()`, delete the `.oidf-tooltip__arrow` base rule and all four placement-specific arrow rules.
  - In `_show()`, delete the lines that create the `arrow` span, append it to the tooltip element, and assign it to `this._arrowEl`. Stop tracking `_arrowEl` entirely (also remove the `this._arrowEl = null;` reset in `_removeTooltip()`).
  - In `_position()`, delete the `arrowLeft` / `arrowTop` locals, all four per-placement assignments to them, and the trailing block that writes those values back to `this._arrowEl.style`.
- **Patterns to follow:**
  - Keep the single `injectStylesOnce()` style block — do not split it.
  - Keep the existing structure of `_position()`'s switch statement; only remove the arrow-related lines from each case.
- **Test scenarios:**
  - Existing Storybook play functions (`Default`, `BottomPlacement`, `NoContent`, `TooltipAppearsOnHover`, `FocusAndEscapeDismiss`, `DynamicallyInsertedChild`, `AutoPlacementFlipsBelow`, `RepositionsOnReshow`) must continue to pass — they assert on tooltip presence, content, placement attribute, and position relative to the trigger, none of which the change should affect.
  - Add one new story `NoArrowRendered` that hovers the trigger and asserts `document.body.querySelector(".oidf-tooltip .oidf-tooltip__arrow")` returns `null` after the tooltip appears. This pins the absence so a future "let's bring back the arrow" change has to update the test deliberately.
  - Manual visual check: hover a tooltip on `log-detail.html` (the owner-cell tooltip introduced in commit `9c640d26a`) and confirm the body sits 4px from the trigger with no chevron.
- **Verification:**
  - `npm run test:ci` from `frontend/` passes (format, lint, type-check, lit-analyzer, icons).
  - Storybook test runner is green for the `Primitives/cts-tooltip` stories.
  - No remaining references to `ARROW_HALF_PX`, `oidf-tooltip__arrow`, `arrowLeft`, `arrowTop`, or `_arrowEl` in `cts-tooltip.js`.

## Risks

- **Visual regression on dense rows.** A 4px gap is tight; on small triggers (icon-only buttons), the tooltip body may sit close enough that pointer movement from trigger to tooltip area could trigger flicker — but the tooltip has `pointer-events: none`, so it cannot itself intercept hover. The trigger keeps hover, so the show/hide loop stays clean.
- **Test snapshot or pixel-diff failures.** None expected — the existing Storybook tests assert on position relative to the trigger with a 1px slack (`>=`) and do not snapshot the full DOM. Confirmed by re-reading `BottomPlacement` and `RepositionsOnReshow` play functions.

## System-Wide Impact

- **Affected surfaces:** Every page that uses `<cts-tooltip>` — currently the owner-cell tooltip on `log-detail.html` and any future call sites. The change is purely presentational and behavior-preserving.
- **Token coupling:** None. The tooltip body still consumes `--ink-900`, `--ink-0`, `--font-sans`, `--fs-12`, `--radius-2`, and `--shadow-2`; removing the arrow does not touch any token.
- **A11y:** `role="tooltip"` and the focus/escape behavior are unchanged. The arrow was decorative and not announced.
