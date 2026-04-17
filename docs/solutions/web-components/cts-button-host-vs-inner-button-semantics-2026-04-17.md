---
date: 2026-04-17
topic: web-components, cts-button, click semantics, light-DOM
related: docs/plans/2026-04-16-002-refactor-html-to-web-components-plan.md
related-solutions: docs/solutions/test-failures/playwright-e2e-flaky-after-web-component-merge-2026-04-14.md
---

# cts-button host vs inner button click semantics

The most subtle class of bugs during the HTML-to-cts-* migration came from confusion between the `<cts-button>` HOST element and the inner `<button>` rendered by Lit. These look like the same thing visually but have different click and styling semantics.

## The model

`cts-button` (and `cts-link-button`) uses Lit with `createRenderRoot() { return this; }` — light DOM. The DOM tree is:

```
<cts-button id="reloadBtn" class="myCustomClass">  ← HOST (the custom element)
  <button class="btn btn-sm btn-light bg-gradient border border-secondary">  ← INNER (rendered by Lit)
    Repeat Test
  </button>
</cts-button>
```

The host has the user-authored attributes (`id`, `class`, `data-*`, `title`). The inner button has the Bootstrap variant classes and the actual `<button>` semantics. JS that targets one vs the other has different consequences.

## What works (use these)

- **`getElementById('reloadBtn')`** returns the HOST. Setting `host.disabled = true` propagates to the inner button via Lit's reactive properties. Setting `host.variant = 'success'` triggers a Lit re-render with the new class. This is the integration seam.
- **User clicks** on the visible button area fire a real DOM click on the inner button. The click bubbles up through the host. jQuery handlers bound on `.deleteBtn` (host class) catch the bubbled event. ClipboardJS reads `data-clipboard-target` from the delegateTarget (the host) via `closest()`. Both work because of light-DOM bubbling.
- **Bootstrap `data-bs-dismiss="modal"`** on the cts-button host works because Bootstrap's event delegation walks `closest('[data-bs-dismiss]')` from the click target up.
- **ID preservation:** `<cts-button id="X">` keeps the id on the host. `getElementById('X')` returns the host (a real DOM element), and `$('#X').on('click', ...)` works because clicks bubble up.

## What breaks silently (avoid these)

| Pattern | Why it breaks | Use instead |
|---------|---------------|-------------|
| `host.classList.add('btn-success')` | Lit owns the inner button's class; mutations on the host don't propagate to the rendered button. | `host.variant = 'success'` |
| `host.click()` (programmatic) | Synthetic clicks on the host don't reach the inner button's `@click` handler, so `_handleClick` never runs and `cts-click` never dispatches. | `host.querySelector('button').click()` for tests, or `dispatchEvent(new CustomEvent('cts-click'))` to skip the inner button entirely |
| Playwright `expect(host).toBeDisabled()` | `:disabled` pseudo-class only matches native form controls. The custom-element host is never `:disabled`. | `expect(host.locator('button')).toBeDisabled()` |
| `host.style.display = 'none'` to hide a `full-width` cts-button | The component imperatively sets `style.display='block'` on the host when `full-width`, overriding `[hidden]` and clobbering the hide. | Use `host.hidden = true` AND don't set `full-width` on hide-able buttons, or remove the element entirely |

## Migration-time hazards we hit

1. **Asymmetric `full-width`** — adding `full-width` to one button in a side-by-side pair stretches it to its own row. Both must have the attribute or neither.
2. **Missing component imports** — every page that uses cts-button needs `<script type="module" src="/components/cts-button.js">`. Pages that only used cts-modal/cts-navbar before silently fail to upgrade `<cts-button>` elements until the import is added. Symptom: visible-but-not-clickable buttons.
3. **DataTables-injected button strings** — `defaultContent: '<cts-button …></cts-button>'` works because Custom Elements upgrade on DOM insertion regardless of how (`innerHTML`, `appendChild`, jQuery `.html()`, DataTables createdRow). The `class` and `id` on the host are preserved during upgrade.
4. **E2E spec drift** — tests that were written when buttons were native `<button>` may target the host now after migration. `page.click("#refresh")` works (Playwright clicks the visible bounding box = the inner button). `expect(page.locator("#refresh")).toBeDisabled()` doesn't work (use `... locator("#refresh button")`).

## Operational rule

Treat the cts-button host as the **integration point** for ID, class, data-attributes, and reactive properties. Treat the inner button as the **interaction point** for clicks, focus, and form submission. Tests, JS handlers, and CSS selectors should know which one they want.

The shared `_button-classes.js` helper extracted during this work centralizes the Bootstrap class string so `cts-button`, `cts-link-button`, and `cts-modal._createButton` can't drift. The `components/AGENTS.md` documents the conventions so future contributors don't have to re-learn them.

## Related artifacts in this branch

- `src/main/resources/static/components/AGENTS.md` — conventions for cts-* components
- `src/main/resources/static/components/_button-classes.js` — shared Bootstrap class helper
- The `VariantPropertySetter` Storybook story in `cts-button.stories.js` — demonstrates the property-setter integration pattern
