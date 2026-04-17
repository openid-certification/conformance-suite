# cts-* Component Conventions

This document captures the authoring rules for all `cts-*` web components in
this directory. Violating them is caught in code review; understanding them
upfront saves iteration.

---

## 1. LitElement vs vanilla HTMLElement

### Use LitElement when

The component has **reactive properties** whose changes should trigger a
re-render of its own DOM. The canonical signal is: "if I change a property
from the outside, the rendered output must update."

Examples: `cts-button`, `cts-link-button`, `cts-navbar`, `cts-form-field`,
`cts-tabs` (if it had reactive state).

Lit handles the update scheduling; you declare `static properties` and write
a `render()` method.

### Use vanilla HTMLElement when

The component is a **"wrap children in a styled box"** pattern with
one-shot rendering on connect. The content is captured in `connectedCallback`,
wrapped in Bootstrap markup, and nothing needs to change afterwards.

Examples: `cts-card`, `cts-modal`, `cts-alert`, `cts-tooltip`.

`cts-badge` is a hybrid: vanilla HTMLElement that also implements
`attributeChangedCallback` + `observedAttributes` to re-render when
attributes change. Use this pattern when reactive updates are needed but
the Lit import is undesirable.

### Rule of thumb

> If you find yourself needing `attributeChangedCallback` and
> `observedAttributes` on more than two or three attributes, switch to
> LitElement — it provides the same mechanism with less boilerplate.

---

## 2. Light DOM rendering (no Shadow DOM)

Every `cts-*` component renders to its own **light DOM**. LitElement
components opt in via:

```js
createRenderRoot() { return this; }
```

Consequences that every consumer must understand:

- **Bootstrap CSS applies naturally.** No per-component import of Bootstrap
  is needed; the global stylesheet on the page reaches the rendered markup.

- **Element IDs and classes remain reachable.** `document.getElementById`
  and CSS selectors find elements inside a `cts-button` just as they would
  find bare `<button>` elements.

- **Click events bubble normally.** A jQuery handler bound to the host,
  e.g. `$('.deleteBtn').on('click', handler)`, catches the click that
  bubbled up from the inner `<button>`.

- **`host.click()` does NOT trigger the inner button.** `host.click()`
  fires a synthetic `click` on the host element itself, which is not the
  rendered `<button>`. Use `addEventListener('cts-click', ...)` to respond
  to button activations, or `host.querySelector('button').click()` in tests
  that must simulate a real browser click on the rendered control.

---

## 3. Integration patterns — use property setters, not classList

JS that needs to update component state **must use the Lit property setter**,
not CSS class manipulation on the host.

```js
// GOOD — Lit re-renders with the new variant class on the inner <button>
el.variant = 'success';

// BAD — the class lands on the cts-button host element, not on the inner
// <button> rendered by Lit. The rendered button keeps its old classes.
el.classList.add('btn-success');
el.classList.remove('btn-light');
```

The same principle applies to all reactive properties:

```js
el.disabled = true;      // GOOD — Lit propagates to inner button's disabled attr
el.loading  = false;     // GOOD
el.label    = 'Saved';   // GOOD
```

When the property change must be observed before the next frame (e.g., in
a test assertion), await Lit's microtask flush:

```js
el.variant = 'success';
await el.updateComplete;
// inner button now has btn-success
```

---

## 4. Slot-children pattern (cts-card, cts-modal, cts-alert, cts-badge)

Children are **captured once** at the first render inside `connectedCallback`
(or on first call to `_render()` for cts-badge). After that the host contains
the rendered wrapper, so mutating its children has no effect on subsequent
re-renders.

Implications:

- To update displayed text dynamically, set the relevant **attribute**
  (`label`, `heading`) instead of mutating child nodes.
- To embed inline rich content (`<a>`, `<em>`, `<strong>`), put raw HTML
  *inside* the host element in the initial HTML. The slot wrapping preserves
  it as-is.
- To completely replace content, remove the element from the DOM and
  re-insert it — `connectedCallback` runs again on reconnect (for components
  that guard with `if (this._initialized) return;` you must also clear that
  flag first, or simply recreate the element).

```html
<!-- Rich content preserved through the slot-children capture -->
<cts-alert variant="info">
  See the <a href="/docs">documentation</a> for details.
</cts-alert>
```

---

## 5. JSDoc @property convention

Every `cts-*` component **must** have a JSDoc `@property` annotation for
each entry in its `static properties` declaration (LitElement) or each
entry in `static observedAttributes` (vanilla HTMLElement).

```js
/**
 * @property {string} variant - One of: light, info, primary, danger,
 *   secondary, success, warning
 * @property {boolean} disabled - Disables the button
 */
class CtsButton extends LitElement {
  static properties = {
    variant: { type: String },
    disabled: { type: Boolean },
  };
  ...
}
```

This is enforced in code review. If you add a new property, add the
annotation in the same commit.

---

## 6. Storybook play() interaction tests

Every component story **must** have a `play()` function that exercises the
**rendered DOM** (not just the host element). Assertions target:

- The inner `<button>` or `<a>` element's class list.
- Relevant attributes (`disabled`, `aria-*`).
- Custom events dispatched by the component.

```js
async play({ canvasElement }) {
  const btn = canvasElement.querySelector('button');
  expect(btn.classList.contains('btn-primary')).toBe(true);
}
```

Never assert on the `cts-button` host element's class list for variant
correctness — the host does not carry variant classes; the inner button does.

### Story file colocation for composite sub-components

Stories may live in a parent's `.stories.js` when a sub-component is only
rendered as part of a composite. For example, `cts-plan-header`,
`cts-plan-modules`, and `cts-plan-actions` are each sub-components of
`cts-plan-detail` — their stories live in `cts-plan-detail.stories.js`
(prefixed `PlanHeader*`, `Modules*`, `Actions*`). The rule is **play-function
coverage per component**, not one file per component. A dedicated
`.stories.js` for an internal sub-component is optional.

---

## 7. No dynamic class concatenation

Variant-to-class mappings live in **explicit lookup tables** at the top of
the component file:

```js
const VARIANT_CLASSES = {
  light: 'btn-light',
  info: 'btn-info',
  primary: 'btn-primary',
  danger: 'btn-danger',
  secondary: 'btn-secondary',
  success: 'btn-success',
  warning: 'btn-warning',
};
```

Never use string interpolation like `` `btn-${variant}` ``. Reasons:

1. Bundlers and linters cannot statically analyse which classes are used.
2. An unknown/misspelled variant silently produces an invalid class name;
   a lookup table returns `undefined` (clearly wrong) or you can fall back
   to a safe default with `VARIANT_CLASSES[variant] || 'btn-light'`.
3. CSP `style-src` configurations may reject dynamically-constructed class
   references.

---

## 8. Page integration

Every page that uses a `cts-*` component must:

1. Include the `<script type="importmap">` that maps `lit` to the vendored
   bundle. This is already present in all HTML pages via the shared layout.

2. Add a `<script type="module">` tag for **each component** used on that page:

```html
<script type="importmap">{"imports":{"lit":"/vendor/lit/lit.js",...}}</script>
<script type="module" src="/components/cts-button.js"></script>
<script type="module" src="/components/cts-navbar.js"></script>
```

The `importmap` must appear **before** any `type="module"` script. Browsers
process import maps synchronously; a module script that imports `lit` before
the map is parsed will throw a resolution error.

---

## Quick reference

| Component | Base class | Reactive? | Notes |
|---|---|---|---|
| `cts-button` | LitElement | Yes | Variant, size, loading, disabled |
| `cts-link-button` | LitElement | Yes | Same shape as cts-button but renders `<a>` |
| `cts-navbar` | LitElement | Yes | Fetches user via `/api/currentuser` on connect |
| `cts-form-field` | LitElement | Yes | Schema-driven input field |
| `cts-card` | HTMLElement | No | One-shot `connectedCallback` |
| `cts-modal` | HTMLElement | No | Wraps Bootstrap 5 Modal; exposes `show()`/`hide()` |
| `cts-alert` | HTMLElement | No | Optional dismiss; fires `cts-alert-dismissed` |
| `cts-tooltip` | HTMLElement | No | Wraps Bootstrap 5 Tooltip on first child |
| `cts-tabs` | HTMLElement | No | Restructures `<cts-tab-panel>` children into WCAG tablist |
| `cts-badge` | HTMLElement | Partial | Uses `observedAttributes` for attribute-driven re-render |
