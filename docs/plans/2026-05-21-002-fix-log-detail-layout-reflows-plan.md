---
title: "fix: log-detail page reflows on data arrival and drawer toggle"
type: fix
status: active
created: 2026-05-21
---

# fix: log-detail page reflows on data arrival and drawer toggle

## Problem Frame

On `log-detail.html` (e.g.
`https://localhost.emobix.co.uk:8443/log-detail.html?log=tc501NvFrVDKh4t`),
two large layout reflows happen after the initial paint:

1. **Test-structure rail appears mid page-load** — the right rail
   (`#ctsLogToc`) is born hidden because the page has no blocks yet.
   When the first `cts-blocks-updated` event fires (driven by the
   bootstrap's `/api/log` polling cycle in
   `src/main/resources/static/js/log-detail.js:809`), the rail removes
   its `hidden` attribute and the page CSS at
   `src/main/resources/static/log-detail.html:97-104` flips the main
   grid from single-column to `1fr 320px` via
   `:has(#ctsLogToc:not([hidden]))`. The whole main content column
   suddenly shrinks by 320 px + a `--space-10` gap. Visible as a hard
   right-shrink of the entries stream a second or two after first
   paint.

2. **Configuration drawer toggle causes a second jump** — opening
   `<details data-testid="drawer-config">` (rendered by
   `_renderDrawer` in
   `src/main/resources/static/components/cts-log-detail-header.js:1311`)
   reveals a `<cts-json-editor>` host with
   `min-height: calc(var(--space-6) * 14)` (≈ 336 px). Monaco initialises
   asynchronously the moment the host becomes visible. If the rendered
   config JSON exceeds the min-height, Monaco grows the host to its
   intrinsic content size (capped by the editor's fallback
   `MAX_HEIGHT_PX`), so the page reflows once on disclosure and again
   on Monaco mount.

This plan reverses the column-collapse decision from
`docs/plans/2026-05-20-002-fix-cts-log-toc-empty-rail-visible-plan.md`
U2 (which removed the 320 px column when the rail hid itself) in favour
of a stable, always-reserved column. The original concern — "an
interrupted test reserves an empty 320 px column" — is accepted as the
right trade-off because the layout reflow on every normal log-detail
load is louder than the rare interrupted-test whitespace.

## Scope

In scope:

- Make `.log-page--with-toc` an unconditional two-column grid at
  ≥ 1440 px so the rail column is reserved from first paint.
- Lock the configuration JSON editor to a fixed height so opening the
  drawer is a single predictable shift (the disclosure) instead of a
  disclosure + Monaco-grow combo.
- Add regression coverage to `frontend/e2e/log-detail.spec.js` for both
  behaviours.

Out of scope:

- Any change to what `cts-log-toc._applyVisibility()` decides about
  showing/hiding itself. The rail's own paint logic stays untouched —
  we are only changing whether the page reserves the column.
- Re-introducing a skeleton/placeholder inside the rail for the
  pre-blocks window. The user explicitly asked for an always-visible
  column; a skeleton inside the column is a follow-up if we ever want
  to make the empty period feel populated.
- Reworking the drawer disclosure into a side panel or modal. The
  `<details>` semantics stay; we just stop the second jump.
- Pre-initialising Monaco before the drawer opens. Lazy mount remains
  the right default for the page's perf budget; fixing the editor
  height removes the layout symptom without paying that cost.

### Deferred to Follow-Up Work

- A loading skeleton inside `cts-log-toc` during the
  pre-`cts-blocks-updated` window, so the reserved column communicates
  "blocks loading" rather than reading as blank space.
- A targeted audit of other Lit components that grow asynchronously
  after mount (any `min-height`-only async editor will exhibit the
  same drawer-style jump if disclosed inside `<details>`).

## Requirements

R1. At viewport ≥ 1440 px with `.log-page--with-toc` on `<main>`, the
    main grid renders as `1fr 320px` from first paint — independent
    of whether `cts-log-toc` has blocks yet. No grid-template change
    occurs when `cts-blocks-updated` fires.

R2. The `cts-log-toc` rail itself still honours its
    `_applyVisibility()` contract: it remains `display: none` when
    empty (no blocks AND no failures) or when the user preference is
    off. The hidden rail simply leaves its 320 px grid track as
    visual whitespace.

R3. Opening or closing the `drawer-config` disclosure produces
    exactly one layout shift — the disclosure itself. The
    `<cts-json-editor>` inside it does not grow after Monaco mounts;
    long configuration JSON scrolls within the editor instead of
    extending the page.

R4. Existing log-detail coverage continues to pass — Storybook plays
    for `cts-log-toc` and `cts-log-detail-header`, the
    `frontend/e2e/log-detail.spec.js` suite, and the
    `frontend/e2e/lit-importmap.spec.js` drift check.

## Key Technical Decisions

- **Drop the `:has()` guard from `.log-page--with-toc`.** Reverts the
  conditional column from U2 of the 2026-05-20-002 plan in favour of
  an always-reserved column. The previous concern (empty 320 px slot
  on an interrupted test) is the lesser of two evils compared to the
  full-page reflow that happens on every normal log-detail load.

  Alternative considered: render a `cts-log-toc` skeleton during the
  blocks-loading window so the column is both reserved AND populated.
  Rejected for this plan because it adds a new render path and
  visually competes with the empty-on-interrupted-test case the
  previous plan was protecting; deferred to follow-up.

  Alternative considered: keep the `:has()` guard but emit
  `cts-blocks-updated` once synchronously during bootstrap with
  `blocks: []` so the rail's `[hidden]` state is decided before
  paint. Rejected — the rail's `hidden` decision is driven by data
  state, which is genuinely unknown until the first poll returns; an
  empty synchronous event would not change the outcome and still
  causes the reflow when real data arrives.

- **Switch `.ctsConfigJson` from `min-height` to a fixed `height`.**
  This pins the editor's outer dimensions before Monaco mounts, so
  the only layout shift on drawer toggle is the disclosure itself.
  Long config JSON scrolls inside the editor (Monaco's native
  behaviour when its host has a bounded height).

  Alternative considered: keep `min-height` and add an explicit
  `max-height` cap at the same value. Equivalent in effect but
  noisier — `height: …` reads as "exactly this tall", which matches
  the intent. The bare `min-height` was inherited from the legacy
  stand-alone config panel where unbounded growth made sense; inside
  a `<details>` disclosure, unbounded growth is the bug.

  Alternative considered: pre-mount Monaco off-screen during page
  bootstrap so first-disclosure paint is already final. Rejected —
  pays a startup cost on every page load for a problem that fixed
  dimensions solve at zero runtime cost.

## High-Level Technical Design

This is illustrative — directional guidance for review, not
implementation specification.

Before (current behaviour at viewport ≥ 1440 px):

```text
t=0   first paint           [ main ────────────────────── ] (single col)
                             (cts-log-toc is [hidden], grid stays 1-col)

t=~1s blocks arrive          [ main ───────────── ][ rail ] (1fr 320px)
      ⟵ HARD REFLOW ⟶          grid flips because :has() now matches
```

After:

```text
t=0   first paint           [ main ───────────── ][ rail ] (1fr 320px from start)
                             rail is [hidden] → empty 320px track

t=~1s blocks arrive          [ main ───────────── ][ rail ] (same)
                             rail un-hides → fills its own track,
                             grid does not change
```

For the drawer, same idea but along the vertical axis:

```text
Before:  closed → open  →  main column grows by ~336px (min-height reserved)
                       →  ~50ms later, Monaco mounts → main column grows
                          again to fit content (single second jump)

After:   closed → open  →  main column grows by exactly the fixed editor
                          height; Monaco renders into bounded dimensions
                          and scrolls internally for long content.
```

## Implementation Units

### U1. Always reserve the rail column at ≥ 1440 px

- **Goal:** Eliminate the page-load reflow by reserving the 320 px
  right column from first paint, regardless of rail content state.
- **Requirements:** R1, R2, R4.
- **Dependencies:** none.
- **Files:**
  - `src/main/resources/static/log-detail.html`
    (modify the `.log-page--with-toc` rule at lines 97-104, refresh the
    surrounding comment at lines 88-96)
  - `frontend/e2e/log-detail.spec.js`
    (add a regression assertion under the existing wide-viewport
    coverage for log-detail)
- **Approach:**
  - Replace the current selector
    `.log-page--with-toc:has(#ctsLogToc:not([hidden]))` with the
    unconditional `.log-page--with-toc`. The rest of the rule
    (`display: grid`, `grid-template-columns: 1fr 320px`,
    `gap: var(--space-10)`, `align-items: start`) stays unchanged.
  - Rewrite the inline comment block that currently explains the
    `:has()` collapse so it documents the new contract: the column
    is reserved unconditionally; the rail's own
    `display: none[hidden]` rule leaves the track as whitespace when
    the rail decides to hide itself; this is intentional and
    preferred over the previous reflow.
  - Reference the previous plan
    (`docs/plans/2026-05-20-002-fix-cts-log-toc-empty-rail-visible-plan.md`)
    in the new comment so a future reader can find the rationale for
    the reversal without git-blame archaeology.
- **Patterns to follow:**
  - Other page-level grid rules in the same file (the failure-summary
    and test-summary breakpoint swaps below this block) — single
    `@media` rule, declarations grouped, with an explanatory comment
    above. Match that voice.
- **Test scenarios** (Playwright e2e against the static page with
  `/api/log` and `/api/info` mocked):
  - Load `log-detail.html?log=<id>` with `/api/log` mocked to return
    only INFO + FAILURE + INTERRUPTED rows (no `startBlock` entries)
    and `/api/info` mocked accordingly. At viewport 1500 px, assert
    that `#main-content` reports a two-column
    `grid-template-columns` (a `1fr 320px` projection) AND that
    `#ctsLogToc` reports `offsetParent === null` (hidden). The column
    is reserved; the rail does not paint.
  - Load the same page with `/api/log` returning two `startBlock`
    rows. At viewport 1500 px, assert the same two-column
    `grid-template-columns` and that `#ctsLogToc` is visible.
    Critically, snapshot the `grid-template-columns` value before the
    `cts-blocks-updated` event fires and after — they must be equal.
    This is the regression guard for the live bug.
  - Re-use existing fixture patterns under `frontend/e2e/fixtures/`;
    do not invent new fixture shapes.
- **Verification:**
  - `cd frontend && ./node_modules/.bin/playwright test
    e2e/log-detail.spec.js` is green.
  - Manually loading the bug URL at ≥ 1440 px shows the page
    rendering with the column already reserved on first paint; the
    entries stream does not shrink horizontally when blocks arrive.

### U2. Lock the configuration drawer editor to a fixed height

- **Goal:** Make opening the configuration drawer a single
  predictable disclosure instead of a disclosure plus a Monaco-grow
  jump.
- **Requirements:** R3, R4.
- **Dependencies:** none.
- **Files:**
  - `src/main/resources/static/components/cts-log-detail-header.js`
    (modify the `.ctsConfigJson` CSS block around line 508-511 inside
    `STYLE_TEXT`)
  - `src/main/resources/static/components/cts-log-detail-header.stories.js`
    (extend the existing `drawer-config` story play function so it
    asserts the editor's bounded height after Monaco mounts)
- **Approach:**
  - Replace
    `cts-log-detail-header .ctsConfigJson { display: block; min-height: calc(var(--space-6) * 14); }`
    with
    `cts-log-detail-header .ctsConfigJson { display: block; height: calc(var(--space-6) * 14); }`.
    The fixed height pins Monaco's outer dimensions; long JSON
    scrolls within the editor (Monaco's default behaviour when its
    host has a bounded height — see the host sizing logic referenced
    around lines 22-40 of `cts-json-editor.js`).
  - Add a brief comment above the rule pointing at this plan's
    rationale ("Fixed height — see plan
    `docs/plans/2026-05-21-002-fix-log-detail-layout-reflows-plan.md`
    U2: Monaco growth after mount caused a second jump on drawer
    toggle.").
- **Patterns to follow:**
  - Other components in the codebase that bound a JSON editor inside
    a disclosure or modal — search for `cts-json-editor` references
    with a sibling `height:` declaration. Mirror that voice and
    declaration order. If no such pattern exists, this becomes the
    pattern.
- **Test scenarios** (Storybook play function, since the drawer
  state machine is the component's own story coverage):
  - In the existing `drawer-config` story (or the closest existing
    story that opens the configuration disclosure), after opening
    the drawer and awaiting Monaco mount, assert that the
    `.ctsConfigJson` host's `getBoundingClientRect().height` matches
    the configured fixed height within ±1 px tolerance. Use a
    payload whose serialised JSON would clearly overflow the fixed
    height if the editor were free to grow (≥ 40 lines).
  - Assert the editor's internal scroll surface is reachable
    (Monaco's textarea or scroll container exists inside the host)
    so the fixed height does not silently clip the content.
- **Verification:**
  - `cd frontend && ./node_modules/.bin/playwright test -g
    "cts-log-detail-header"` via the Storybook runner is green.
  - On the dev server, toggling
    `[data-testid="drawer-config"]` open or closed produces a
    single, predictable layout shift; no visible second jump as
    Monaco mounts; long config JSON scrolls inside the editor.

## System-Wide Impact

- **Other pages reading `cts-log-toc`:** none. The rail is only
  mounted on `log-detail.html`; the U1 grid change does not affect
  Storybook stories (they do not embed the rail in the page grid).
- **Other pages using `cts-json-editor` inside `<details>`:** unknown
  surface. Grep for `<cts-json-editor` inside other components that
  also live inside `<details>` to see if the same fixed-height
  treatment should be propagated. Out of scope for this plan but
  worth a follow-up sweep if more than two callers match.
- **Visual regression for interrupted tests:** at ≥ 1440 px, an
  interrupted test that never produced blocks now shows a 320 px
  empty column on the right where the previous plan collapsed it.
  This is the explicit trade-off the user requested; document it in
  the inline comment so the next reviewer sees the intent.
- **Storybook contracts:** the existing `cts-log-toc` plays
  (`EmptyDuringWaiting`, `ReappearsWhenBlocksArrive`) continue to
  pass — they assert on `hidden` attribute behaviour, which we are
  not touching. The new e2e assertions in U1 cover the page-level
  grid behaviour.

## Risks

- **Wider whitespace impact on interrupted tests.** The reserved
  320 px column reads as a visible gap when the rail hides itself.
  Mitigated by the explicit user ask for this trade-off; the
  follow-up skeleton (deferred) would close the loop if the
  whitespace later proves distracting in practice.
- **Fixed-height editor clips operationally important configs.**
  Modern Monaco scrolls correctly inside a bounded host, but a
  reviewer eyeballing the configuration without scrolling could miss
  later keys. Mitigated by the existing `_openConfigDisclosure()`
  path scrolling the disclosure into view — the editor's scroll
  surface is then immediately visible. The U2 test scenario explicitly
  asserts the scroll surface exists.
- **Reversal of a recently-shipped decision.** Reverting U2 of the
  2026-05-20-002 plan three weeks after it landed risks the next
  reviewer wondering whether the reversal was intentional. Mitigated
  by the cross-reference comment in `log-detail.html` and a clear
  link from this plan to the prior plan.

## Verification (overall)

1. `cd frontend && npm run test:ci` — passes (no lint/typecheck/
   story regressions).
2. `cd frontend && ./node_modules/.bin/playwright test
   e2e/log-detail.spec.js` — passes; new U1 grid assertions are
   green.
3. `cd frontend && ./node_modules/.bin/playwright test -g
   "cts-log-detail-header"` via the Storybook runner — passes;
   new U2 fixed-height assertions are green.
4. On the dev server, navigating to
   `https://localhost.emobix.co.uk:8443/log-detail.html?log=tc501NvFrVDKh4t`
   at ≥ 1440 px shows the rail column reserved from first paint;
   no horizontal shift of the entries stream when blocks arrive.
5. On the same page, toggling `[data-testid="drawer-config"]`
   produces a single layout shift on disclosure; no perceptible
   second jump.
6. Navigating to a healthy log URL with real blocks still shows the
   populated rail beside the entries stream and the
   active-row highlight still tracks scroll.
