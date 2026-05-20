---
name: fix-logs-owner-column-tone-down
description: Tone down the logs.html owner column chip so it reads as read-only metadata, not an actionable affordance. Drop the 1px darkgrey border around each two-tone half while preserving the lavender/pale-blue fill and the hover-to-reveal subject/issuer tooltips. No new feature work — purely an affordance fix per the CTS badge convention.
status: active
created: 2026-05-20
type: fix
depth: lightweight
---

# Tone down the logs.html owner column chip

## Problem Frame

On `https://localhost.emobix.co.uk:8443/logs.html` (admin view), the Owner column renders a compound two-tone chip: a lavender half with a user glyph (subject) and a pale-blue half with a globe glyph (issuer), with the real `sub` / `iss` values disclosed on hover via `title` and `aria-label`. The structural shape was restored on `2026-05-19` after the April coolicons migration regressed it (see origin: `docs/plans/2026-05-19-007-fix-logs-owner-cell-and-icon-guardrails-plan.md`).

The current CSS (`src/main/resources/static/css/layout.css:558-582`) wraps each half in a `1px darkgrey solid` border on three sides. That literal border around a chip-shaped element is exactly the visual signal CLAUDE.md's badge convention reserves for *interactive* affordance:

> Read-only (default): fill only, no border.
> Interactive: fill + 1px inset `box-shadow` ring + hover/focus.
> Do NOT: hand-roll a 1px `border` around a chip-like element to fake the affordance ring.
> — CLAUDE.md "Badges" section

The chip is **not** clickable anywhere — there is no click handler in `logs.html`, no filter-by-owner feature, no link wrapping the cell, and grepping the codebase finds zero handlers attached to `.log-owner`, `.ownerSub`, or `.ownerIss`. The user looking at the column reads "this is something I can click" and gets no response.

The user's brief gives two paths: (a) make it actually clickable if it should be, or (b) tone the style down if it isn't. Option (b) is correct here: there's no existing owner-related action surface in the product (no owner-filter URL, no owner-detail page), and adding one is feature scope well beyond a visual cleanup. The current information design — hover-to-reveal `sub` + `iss` for admins inspecting the listing — works for its purpose.

## Scope

In-scope:
- Remove the literal 1px borders on `.ownerSub` and `.ownerIss` so the chip lands on the canonical "read-only = fill only" state.
- Preserve the two-tone fill (`#d9c9fe` lavender for subject, `#d3dcf5` pale-blue for issuer), the matching rounded outer corners on the chip, and the `title` / `aria-label` hover semantics.
- Keep the anti-wrap layout invariant (`display: inline-flex; flex-wrap: nowrap` on `.log-owner`) that the e2e suite pins.
- Refresh the e2e height-tolerance comment that currently mentions "borders ≈ 20-24px" so it does not claim a property the new rule no longer asserts.

### Deferred to Follow-Up Work
- **Adding an owner-filter feature.** A "click an owner chip → filter logs by `sub`" or `iss` flow would be genuinely useful (it would mirror the existing `?status=` / `?result=` URL-driven filter chips), but it is a real feature with backend filter support, UI for clearing the filter, and `cts-data-table` integration. It is intentionally not in this plan — file a separate plan if/when it becomes priority.
- **Migrating the chip fill colors to design tokens.** The two hex literals `#d9c9fe` and `#d3dcf5` are not in `oidf-tokens.css` — they're one-off paint. Replacing them with tokens is part of the broader Claude-Design token rollout tracked under the design-system migration thread, not this fix. Touching them now would either invent two new tokens that nothing else uses (`--owner-sub-bg` / `--owner-iss-bg`) or pick the wrong existing token and shift the visual identity. Hold for the systemic migration pass.
- **Converting the cell to a single `cts-badge`.** The chip is two paired halves with different fills; the current `cts-badge` variant set (`pass`/`fail`/`warn`/`running`/`skip`/`review` + utility primary/secondary/danger/info-subtle) does not describe a "paired sub+iss compound" affordance. Forcing the chip through `cts-badge` either requires inventing a new variant or rendering it as two adjacent badges with a visible seam between them — neither is an improvement. Out of scope.

### Out of scope
- Backend changes — frontend-only per `feedback_minimal_backend_touching` memory.
- Reordering, hiding, or relabeling the Owner column.
- The active-filter chip, status pill, result pill, or any other cell renderer on `logs.html`.

## Requirements

- **R1.** The Owner column chip on `https://localhost.emobix.co.uk:8443/logs.html` must not present an interactive affordance (no `1px solid` border around chip-shaped elements; no inset ring; no hover/focus state change that would imply clickability).
- **R2.** The chip must keep its two-tone visual identity: lavender (subject half) and pale-blue (issuer half), with rounded outer corners, so a reader can still distinguish the paired subject + issuer information.
- **R3.** The chip must keep the hover-to-reveal contract: `title` and `aria-label` on `.ownerSub` / `.ownerIss` expose the underlying `sub` and `issuer` strings.
- **R4.** The chip must stay on a single line in narrow table cells (the existing `inline-flex; flex-wrap: nowrap` invariant must not regress).
- **R5.** Existing e2e coverage (`frontend/e2e/logs.spec.js` owner-cell test) must continue to pass without weakening assertions to accommodate the change.

## Key Technical Decisions

- **Decision: drop the border, keep the fill.** The CTS badge convention is unambiguous about which signal carries which meaning. Removing the border is the minimum change that matches "read-only = fill only" while leaving the chip's two-color identity intact.
- **Decision: leave the hex literals in place for now.** Token migration is a separate, larger thread; introducing new tokens here would either be one-off or pre-empt the systemic pass. The existing test pins markup, not paint values, so this choice is reversible without churn.
- **Decision: no DOM changes to `templates/owner.html`.** Markup stays exactly as restored by yesterday's plan. All of the change lives in CSS. This keeps the e2e selectors (`.log-owner > .ownerSub > cts-icon[name="user-01"]`, `.log-owner > .ownerIss > cts-icon[name="globe"]`) stable.
- **Decision: do not add `cursor: default`.** It's already the default on `<span>` so adding it is dead style; the only reason to set it explicitly would be to *override* a clickable cursor inherited from a parent, and there isn't one.

## Implementation Units

### U1. Drop chip borders and refresh the e2e tolerance comment

**Goal:** Land the read-only fill-only state on the owner chip per the CTS badge convention. The chip should look like a static information chip, not a button.

**Requirements:** R1, R2, R3, R4

**Dependencies:** None.

**Files:**
- `src/main/resources/static/css/layout.css` — remove the `border-top`, `border-left`, `border-bottom` declarations from `.ownerSub` (lines 566-568) and the `border-top`, `border-right`, `border-bottom` declarations from `.ownerIss` (lines 579-581). Leave the `background`, `color`, `padding`, and `border-*-radius` rules untouched.
- `frontend/e2e/logs.spec.js` — refresh the in-line comment at the height assertion (around line 416) so it no longer claims "padding (2px+2px) + ~16px icon + borders ≈ 20-24px". The new shape is "padding + ~16px icon ≈ 20px" — the 32px ceiling stays the same; only the comment changes. Do not weaken the assertion itself.

**Approach:**
- The literal three-sided border on each half is what produces the "this is a button" visual reading. With it gone, both halves are pure fill-against-page-background, which is the canonical read-only chip per `CLAUDE.md`'s Badges section.
- Padding (`2px 10px`) and the rounded outer corners stay: padding gives the chip air around the 16px icon, and the rounded corners are what make it read as a *paired chip* rather than as two adjacent square swatches. Both are visual identity, not affordance.
- The two halves remain visually distinguished by their fills (lavender vs. pale-blue), and the icon glyphs (user vs. globe) keep their semantic role. Hover/`aria-label` continues to expose the real `sub` and `iss` values.
- No JS, no template, no token, no new variant. One CSS file, one comment refresh.

**Patterns to follow:**
- The "fill-only" read-only chip pattern documented in CLAUDE.md's Badges section.
- The seam between the two halves stays implicit (both fills meet at the chip's vertical midline); no new divider rule needed.

**Test scenarios:**
- **Visual / manual (Chrome at `https://localhost.emobix.co.uk:8443/logs.html` admin view):** The owner cell renders as a two-tone lozenge with no visible border. The lavender half (subject) sits left, the pale-blue half (issuer) sits right, the rounded outer corners are preserved, both halves stay on the same baseline, no border is drawn anywhere on the chip.
- **Visual / manual hover:** Hovering the subject half shows the tooltip with the `sub` value; hovering the issuer half shows the tooltip with the issuer URL. The chip does not change shape, color, cursor, or position on hover.
- **e2e regression (`frontend/e2e/logs.spec.js` owner-cell test):** Existing test continues to pass without modification beyond the comment refresh — selector hierarchy (`.log-owner > .ownerSub > cts-icon[name="user-01"]`, `.log-owner > .ownerIss > cts-icon[name="globe"]`), `title` and `aria-label` attributes, and the `≤ 32px` height invariant all still hold.
- **e2e narrow-viewport regression (same test, `setViewportSize({ width: 600, height: 800 })`):** Chip stays on one line.
- **Token regression (negative):** No new design tokens are added to `oidf-tokens.css`; no hex literal is changed; no `cts-badge` import appears in `owner.html`.

**Verification:**
- `npm run test:e2e -- e2e/logs.spec.js` from `frontend/` passes (the existing owner-cell test plus the narrow-viewport check).
- `npm run test:ci` from `frontend/` passes (format, lint, type-check, jsdoc, icons, lit-analyzer, codegen). The change is a CSS rule deletion in a `.css` file, so none of the lit/icon/codegen checks are touched.
- Manual visual smoke at `https://localhost.emobix.co.uk:8443/logs.html` (admin view) shows the chip without borders, with hover tooltips intact.

## Scope Boundaries

This plan ships a visual tone-down only. Information design, click semantics, filtering, and token migration are all explicitly deferred. The change is reversible by re-adding three CSS lines per half if priorities shift.

## Risks

- **Low risk: the deleted border is contributing more shape than expected.** Possible but unlikely — the border is `1px solid darkgrey` and the fills are saturated enough that the chip's silhouette is carried by the fills alone. If the read-only chip looks "weak" against the table row background, the fallback is to swap the border for a token-routed `--border` (which is `var(--ink-200)`, much lighter) rather than reintroduce the heavy `darkgrey` literal. Out of scope unless the visual smoke flags it.
- **Low risk: the e2e height assertion was actually relying on the borders.** Currently the assertion is `≤ 32px` and the chip-with-borders is `~24px`; removing 2px of border drops it to `~22px`, still well within tolerance. No test change beyond the comment refresh is needed.
- **No accessibility risk.** The `aria-label` semantics carry the actual `sub` and `iss` values regardless of border styling. Screen reader announcement is identical before and after.

## System-Wide Impact

- **Affected page:** `logs.html` (admin view only — the Owner column is hidden when `isPublic` is set or the current user is not admin).
- **Not affected:** `plans.html`, `log-detail.html`, `schedule-test.html`, or any other CTS page. `.ownerSub` / `.ownerIss` / `.log-owner` are scoped to the owner template and are not used elsewhere (verified by grep).
- **Bootstrap-removal alignment:** The change uses no Bootstrap utility classes (the file already left Bootstrap behind for this template); it stays consistent with the broader Bootstrap-removal track on `feat/redesign`.
