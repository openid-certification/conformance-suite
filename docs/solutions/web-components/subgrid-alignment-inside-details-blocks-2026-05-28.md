---
title: "Aligning CSS subgrid columns inside a <details> block via the ::details-content relay"
date: 2026-05-28
category: web-components
module: frontend
problem_type: design_pattern
component: tooling
severity: medium
applies_when:
  - "Aligning grid/subgrid columns across rows where some rows are nested inside a <details> element (collapsible block, disclosure group, expandable section)"
  - "A CSS subgrid that should relay its parent grid's tracks instead 'collapses' so its children stack in a single column"
  - "Building multi-column log/table-like layouts that use <details> for progressive disclosure"
related_components:
  - cts-log-viewer
  - cts-log-entry
tags:
  - web-components
  - css-subgrid
  - details-element
  - details-content
  - container-queries
  - display-contents
  - log-viewer
---

# Aligning CSS subgrid columns inside a `<details>` block via the `::details-content` relay

> **Superseded 2026-05-28** (same day): the `cts-log-viewer` logBlock was de-collapsed from a `<details>` to a plain `<div class="logBlock">` (see `docs/plans/2026-05-28-002-refactor-log-block-decollapse-plan.md`). With no `<details>`, there is no UA `::details-content` wrapper, so the `[open]`-gated `::details-content { display: contents }` relay documented here **no longer exists in the codebase** — the `.logBlock` subgrids directly. The collapse-regression guard (toggle closed → assert rows hidden) was also removed.
>
> **The one durable insight survives and still applies** to any *future* `<details>`-based subgrid layout: `display: contents` disables `content-visibility`, so dissolving a UA `::details-content` wrapper *unconditionally* breaks the disclosure element's hide-when-closed behaviour — gate the dissolve on `[open]`. Everything below about the `.logBlock`-as-`<details>` shape is historical.

## Context

The log viewer aligns every row's columns (timestamp / severity / HTTP / body / actions) by making `.logEntries` a master grid and having each row's `.logItem` **subgrid** into it. Top-level rows do this through a single `display: contents` host (`cts-log-entry`), so their columns line up by the widest content in each track.

Rows that belong to a block render *inside* a `<details class="logBlock">`, one level deeper. They would not align: their columns drifted row-to-row, and in the worst case **all five cells collapsed into the first ~92px column and stacked vertically**. Two dead-ends preceded the fix, both worth recording:

1. **"Subgrid relay through `<details>` fails" (the original code's conclusion).** A prior author tried relaying the master tracks down into the block and concluded it didn't work, falling back to independent per-entry `max-content` grids — which is what produced the ragged, misaligned layout. The relay *does* work; the real blocker was elsewhere (see Guidance).
2. **Unconditional `::details-content { display: contents }` silently broke collapse.** Once the relay was working, an unconditional dissolve of the wrapper left **collapsed blocks showing their rows** — the `<details>` no longer hid its content when closed. No error; the block just stopped collapsing. (auto memory [claude]: this matches the existing note `feedback_display_contents_for_wrapper_custom_elements` — `display: contents` is the right tool for layout-neutral wrappers, but it has a sharp edge with `content-visibility`.)

## Guidance

Relay the master grid's tracks two levels deep, and **dissolve the UA `::details-content` wrapper only while the block is open**:

```css
@container ctsLogViewer (min-width: 640px) {
  /* master grid */
  .logEntries { display: grid; grid-template-columns: 92px auto auto minmax(0, 1fr) auto; column-gap: var(--space-3); }

  /* top-level rows: host dissolves, .logItem subgrids one level */
  .logEntries > cts-log-entry { display: contents; }

  /* block: relay the master tracks down one more level */
  .logEntries > .logBlock {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: subgrid;
    column-gap: var(--space-3);   /* a subgrid inherits TRACKS but not gap — restate it */
  }
  /* dissolve the UA wrapper ONLY when open (see Why This Matters) */
  .logEntries > .logBlock[open]::details-content { display: contents; }
  .logEntries > .logBlock > summary.startBlock { grid-column: 1 / -1; }
  .logEntries > .logBlock > cts-log-entry { display: contents; }

  /* each nested .logItem subgrids into .logBlock's relayed tracks */
  .logBlock cts-log-entry .logItem { grid-template-columns: subgrid; grid-column: 1 / -1; }
}
```

The keystone is `::details-content`. Chrome and Safari wrap a `<details>`'s non-summary content in a UA-generated `::details-content` pseudo-element that defaults to `display: block`. That block box sits between `.logBlock` (the grid) and the entries, so **any subgrid below it has no grid parent and collapses to a single track**. Setting `display: contents` on it removes the box so the entries become direct grid items of `.logBlock`. Firefox ships no `::details-content`, so the rule is a harmless no-op there and the relay works natively.

## Why This Matters

- **`content-visibility` needs a box; `display: contents` removes it.** The browser collapses a closed `<details>` by applying `content-visibility: hidden` to `::details-content`. `content-visibility` has no effect on a box-less (`display: contents`) element, so an *unconditional* dissolve disables collapse. Scoping the dissolve to `[open]` means the default wrapper (and its collapse behaviour) is present whenever the block is closed, and only dissolves while the block is open and the rows are actually shown.
- **Two debugging signals lie here, so verify with screenshots and `checkVisibility()`, not rects.** `::details-content` is a render-tree pseudo, not a DOM node — it never appears in `querySelector` walks, and Chrome DevTools surfaces it as "the parent has `display: block`, which prevents `grid-column`." `getBoundingClientRect()` on a `display: contents` element is always `0×0` (no box), and on a `content-visibility: hidden` subtree it can still report a non-zero height — both mislead. `element.checkVisibility()` is the trustworthy "is this actually shown?" signal, and a screenshot is the trustworthy layout signal.
- **The relay was always viable.** The "subgrid relay fails" folklore was a misattribution: the failure was the `::details-content` block box, not a limit on nested subgrid. Nested subgrid (a subgrid whose parent is also a subgrid) propagates fine across the browsers the suite targets.

## When to Apply

Reach for this whenever a multi-column subgrid layout must include rows nested inside a `<details>` (or any element with a UA-generated content wrapper) and you need those rows to share columns with rows outside the block. The `[open]` scoping is mandatory any time the disclosure must still collapse.

## Examples

Regression guard (Storybook play function) — assert column alignment AND that collapse still hides rows at the wide layout:

```js
// block rows share the message-column x with the top-level reference row
for (const item of blockItems) {
  expect(Math.abs(left(item.querySelector(".logBody")) - left(topBody))).toBeLessThanOrEqual(1);
}
// collapse must still hide block rows (the [open]-scoping guard)
await userEvent.click(summary);
await waitFor(() => {
  expect(block.open).toBe(false);
  for (const item of blockItems) expect(item.checkVisibility()).toBe(false);
});
```

Implementation: `src/main/resources/static/components/cts-log-viewer.js` (the `.logBlock` relay + `[open]`-scoped `::details-content` rule) and `cts-log-entry.js` (the `.logBlock cts-log-entry .logItem` subgrid rule). Regression story: `AlignedBlocks` in `cts-log-viewer.stories.js` with the `MOCK_BLOCKS_ALIGN` fixture.

Related: [[feedback_display_contents_for_wrapper_custom_elements]], [[feedback_layout_css_logitem_hover]] (the global `.logItem:hover` rule applies to block rows too).
