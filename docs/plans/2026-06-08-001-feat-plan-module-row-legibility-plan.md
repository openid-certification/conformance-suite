---
title: "feat: Plan-module row legibility — badge-near-name + whole-row click"
status: active
date: 2026-06-08
type: feat
component: src/main/resources/static/components/cts-plan-modules.js
---

# feat: Plan-module row legibility — status badge near name + whole-row block link

## Summary

Re-layout each `.module-row` in `cts-plan-modules.js` so the status badge sits in
a vertical stack directly under the module name (instead of in its own far-right
grid column), and make the **entire row** a click target for the test's
log-detail page using the Adrian Roselli block-link pattern already established in
`cts-plan-list.js` / `cts-log-list.js` (single real anchor + full-bleed `::after`
overlay, nested controls lifted on `z-index`). The test instance ID moves to plain
mono text immediately after the badge — no "Test ID:" label, no callout styling.

This is a presentation-only change to one Lit light-DOM component. No API, data, or
backend surface is touched.

---

## Problem Frame

The current row is a 4-column grid (`28px 1fr auto auto`): row number, name-stack,
status badge (far right), action stack. The badge is visually divorced from the
module name it describes — the eye has to traverse the full row width to connect
"oidcc-server" with "PASSED". The variant params and a labelled `Test ID: NONE`
share a single `.desc` line under the name, which reads as boilerplate.

Two legibility goals:
1. **Proximity** — pull the status badge up next to the name so status reads as a
   property of the module, not a right-rail afterthought.
2. **Whole-row affordance** — today only the name link and badge link navigate; the
   large empty middle of the row is dead space. Make the whole row clickable to the
   same `logHref`, while keeping the Run / View Logs / Download Logs buttons, the
   help tooltip, and the focusable links independently operable.

---

## Requirements

- **R1** — Status badge renders inside the `.name` stack on a line directly below
  the `.nameLine`, immediately followed by the test instance ID as plain mono text
  (no "Test ID:" label, no badge/callout chrome on the ID).
- **R2** — Variant params (`variantStr`) render on their own line below the
  badge+ID line; when there is no variant, that line is omitted.
- **R3** — The full `.module-row` is a click target navigating to `logHref` when a
  test instance exists, via the block-link `::after`-overlay pattern (not a
  wrapping `<a>` around interactive descendants).
- **R4** — Run Test, View Logs, Download Logs buttons, the help-tooltip icon, the
  module-name link, and the status-badge link all remain independently
  clickable/focusable (lifted above the overlay).
- **R5** — Rows with **no** instance (`lastInstance` null) render with no row-level
  overlay and no navigation — unchanged behavior for unrun modules.
- **R6** — Preserve `data-testid="module-name-link"` and
  `data-testid="module-status-link"` anchors and the R28 deep-link / `aria-label`
  semantics (e2e in `frontend/e2e/plan-detail.spec.js` asserts on both).
- **R7** — Keyboard focus order stays logical and every interactive element keeps a
  visible focus ring; the row overlay must not trap or steal focus.

---

## Key Technical Decisions

**KTD-1 — Block-link overlay, not a wrapping anchor.** Wrapping the row in `<a>`
would nest the Run/View/Download buttons and the help icon inside a link (invalid
and breaks their clicks). Use the established repo pattern: one real anchor carries
`::after { position:absolute; inset:0 }` over a `position: relative` row; nested
controls get `position: relative; z-index: 1`. This mirrors
`.cts-plan-card-name::after` (`cts-plan-list.js:199`) and the `cts-log-list.js`
headline overlay — same idiom, same reviewer mental model.

**KTD-2 — Which anchor owns the overlay.** The `moduleStatusLink` (badge anchor)
becomes the overlay owner. Rationale: it already exists, already points at
`logHref`, already carries the two-state `aria-label` (`View logs…` /
`Jump to first failure…`), and now lives in the name-stack where the row's primary
status affordance sits. The `moduleNameLink` stays a normal in-flow link lifted
above the overlay (its accessible name is the module name — a distinct, useful
target). Two anchors to the same href is acceptable and pre-existing; only one owns
the stretch.

**KTD-3 — Grid drops to 3 columns.** `grid-template-columns: 28px 1fr auto`
(number, name-stack, actionStack). The badge column is removed because the badge
now lives in the name-stack. The existing `@container (max-width: 780px)` rule
already targets `28px minmax(0,1fr) auto` — it converges with the new default, so
the narrow-card reflow simplifies.

**KTD-4 — Remove the badge/status vertical nudge.** The
`.module-row > cts-badge, .module-row > .moduleStatusLink { top: -1px }` rule
(`cts-plan-modules.js:146`) existed to align the badge baseline with the right-rail
action buttons. Once the badge moves into the name-stack it no longer sits beside
those buttons, so the nudge is removed rather than relocated.

---

## High-Level Technical Design

Row anatomy after the change (grid `28px 1fr auto`):

```
┌──────┬───────────────────────────────────┬───────────────────────┐
│  01  │ nameLine:  oidcc-server  (?)       │  [Download][View][Run]│
│      │ statusLine: [PASSED] hMkO3lEMvu79..│   ← actionStack       │
│      │ desc:       client_auth_type=…     │                       │
└──────┴───────────────────────────────────┴───────────────────────┘
   ▲ whole row = ::after overlay owned by moduleStatusLink → logHref
     actionStack buttons + name link + status link + help icon: z-index:1
```

Stacking contract:
- `.module-row` → `position: relative` (overlay containing block).
- `.moduleStatusLink::after` → `inset: 0; z-index: 0` (covers the row).
- `.moduleNameLink`, `.moduleStatusLink` (its own box), `.help`/`.help-icon`,
  `.actionStack` (and its buttons) → `position: relative; z-index: 1`.

---

## Implementation Units

### U1. Re-layout the name-stack template and grid

**Goal:** Move the badge into the `.name` stack and restructure the three text
lines; drop the badge grid column.

**Requirements:** R1, R2, R5.

**Files:**
- `src/main/resources/static/components/cts-plan-modules.js` (template + CSS)

**Approach:**
- In `_renderModuleRow`, remove `${linkedBadge}` from its standalone grid-cell
  position and place it inside `.name`, in a new `.statusLine` wrapper directly
  after `.nameLine`:
  - `.statusLine` = `linkedBadge` followed by the plain instance id
    (`<span class="mono">${lastInstance}</span>`), rendered only when
    `lastInstance` exists.
  - Move `variantStr` to its own `.desc` line below `.statusLine`, omitted when
    empty (drop the current `Test ID:` label text and the `·` separator entirely).
- CSS:
  - `.module-row` grid → `28px 1fr auto`.
  - Add `.name .statusLine { display: flex; align-items: center; gap: var(--space-2); }`.
  - `.name .statusLine .mono { font-family: var(--font-mono); color: var(--fg-soft); font-size: var(--fs-12); }`.
  - Keep `.name` as a vertical flex/stack with small `row-gap` (e.g. `var(--space-1)`).
  - Remove the `.module-row > cts-badge, .module-row > .moduleStatusLink { top:-1px }` rule (KTD-4).
  - Simplify the `@container (max-width: 780px)` block: the grid already matches the
    new default, so only keep the `padding` / `row-gap` / `actionStack` grid-column
    overrides that still differ.

**Patterns to follow:** existing `.name` / `.desc` / `.nameLine` structure in the
same file; badge rendering via `linkedBadge` is reused unchanged.

**Test scenarios:**
- Module with instance + variant: badge and plain instance id appear on one line;
  variant string appears on the line below; no "Test ID:" label text present.
- Module with instance, no variant: badge + id line present; no empty variant line.
- Module with no instance (`lastInstance` null): no badge, no id text, no variant
  line beyond existing behavior; row renders without error.
- Snapshot/DOM assertion: `module-name-link` and `module-status-link` testids still
  present (R6).

### U2. Whole-row block-link overlay + z-index stacking

**Goal:** Make the entire row navigate to `logHref` while keeping all controls
independently operable.

**Requirements:** R3, R4, R5, R6, R7.

**Files:**
- `src/main/resources/static/components/cts-plan-modules.js` (CSS only; template
  unchanged from U1)

**Approach:**
- `.module-row { position: relative; }`.
- `.moduleStatusLink::after { content:""; position:absolute; inset:0; border-radius: inherit; z-index: 0; }` — only present when the status link renders (i.e. instance exists), satisfying R5 automatically since unrun rows have no `moduleStatusLink`.
- Lift nested interactives above the overlay: `.module-row .actionStack`,
  `.module-row .name .moduleNameLink`, `.module-row .name .help`,
  `.module-row .name .moduleStatusLink` → `position: relative; z-index: 1`.
  (The overlay is `::after` of `moduleStatusLink`; the link's own box at `z-index:1`
  sits above its own pseudo, which is correct — the badge stays clickable and the
  pseudo extends the hit area outward.)
- Optional row hover affordance mirroring `cts-plan-card:hover` (subtle
  `background`/`border-color` shift) for discoverability — keep within existing
  token vocabulary; do not introduce new tokens.
- Verify the help-tooltip icon (`.help-icon`, `tabindex=0`) still receives hover
  and focus (it must be above the overlay).

**Patterns to follow:** `cts-plan-list.js:145-204` (block-link comment + `::after`),
`cts-log-list.js:336-480` (z-index lift of nested controls above headline overlay).

**Test scenarios:**
- Clicking the empty middle band of a row with an instance navigates to `logHref`.
- Clicking Run Test fires `cts-run-test` and does NOT navigate.
- Clicking View Logs navigates via its own anchor (not double-handled).
- Clicking Download Logs fires `cts-download-log` and does NOT navigate.
- Clicking the module-name link navigates (its own anchor).
- Hovering the help icon shows the tooltip; the icon is keyboard-focusable.
- A row with no instance has no overlay and no row-level navigation.
- Keyboard: Tab reaches name link, status link, and each action button with a
  visible focus ring; overlay does not intercept focus.

---

## Scope Boundaries

In scope: presentation/markup of `cts-plan-modules.js` rows.

Out of scope (non-goals):
- Changing `logHref` construction, R28 deep-link logic, or `aria-label` wording.
- Backend, API response shape, or `module-status.js` status mapping.
- Other consumers of the status badge or plan-detail page chrome.

### Deferred to Follow-Up Work
- None.

---

## System-Wide Impact

- **e2e:** `frontend/e2e/plan-detail.spec.js` asserts on `#planItems .module-row`,
  `[data-testid="module-status-link"]`, and `[data-testid="module-name-link"]` — all
  preserved. New whole-row-click behavior may warrant an added assertion but does not
  break existing selectors.
- **Storybook:** `cts-plan-detail.stories.js` / `cts-plan-modules` stories render the
  rows; any interaction/play tests should still pass. Re-run story tests after the
  change (CLAUDE.md requires Storybook play tests for cts-* components).
- **Visual:** before/after screenshot set may need a refresh for plan-detail.

---

## Verification

- `cd frontend && ./node_modules/.bin/playwright test e2e/plan-detail.spec.js` green.
- `cd frontend && npm run test:ci` (format/lint/lit-analyzer/icons) green.
- Storybook play tests for `cts-plan-modules` / `cts-plan-detail` green.
- Manual: on plan-detail, badge sits under the name with the bare instance id beside
  it; whole row navigates; all three action buttons and the help tooltip work; unrun
  rows are inert.

---

## Sources & Research

- Block-link pattern: `src/main/resources/static/components/cts-plan-list.js:145-204`
  (Adrian Roselli reference inline), `cts-log-list.js:336-480`.
- Current component: `src/main/resources/static/components/cts-plan-modules.js`.
- e2e selectors: `frontend/e2e/plan-detail.spec.js`.
