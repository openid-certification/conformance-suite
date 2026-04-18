# cts-\* Component Conventions

This document captures the authoring rules for all `cts-*` web components in
this directory. Violating them is caught in code review; understanding them
upfront saves iteration.

---

## 1. LitElement vs vanilla HTMLElement

### Use LitElement when

The component has **reactive properties** whose changes should trigger a
re-render of its own DOM. The canonical signal is: "if I change a property
from the outside, the rendered output must update."

Examples: `cts-button`, `cts-link-button`, `cts-navbar`, `cts-form-field`.

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
el.variant = "success";

// BAD — the class lands on the cts-button host element, not on the inner
// <button> rendered by Lit. The rendered button keeps its old classes.
el.classList.add("btn-success");
el.classList.remove("btn-light");
```

The same principle applies to all reactive properties:

```js
el.disabled = true; // GOOD — Lit propagates to inner button's disabled attr
el.loading = false; // GOOD
el.label = "Saved"; // GOOD
```

When the property change must be observed before the next frame (e.g., in
a test assertion), await Lit's microtask flush:

```js
el.variant = "success";
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
  _inside_ the host element in the initial HTML. The slot wrapping preserves
  it as-is.
- To completely replace content, remove the element from the DOM and
  re-insert it — `connectedCallback` runs again on reconnect (for components
  that guard with `if (this._initialized) return;` you must also clear that
  flag first, or simply recreate the element).

```html
<!-- Rich content preserved through the slot-children capture -->
<cts-alert variant="info"> See the <a href="/docs">documentation</a> for details. </cts-alert>
```

---

## 5. JSDoc @property convention

Every `cts-*` component **must** have a JSDoc `@property` annotation for
each entry in its `static properties` declaration (LitElement), each
entry in `static observedAttributes` (vanilla HTMLElement with
`attributeChangedCallback`), or — for vanilla HTMLElements that read
attributes imperatively in `connectedCallback` — each `getAttribute(...)`
call site. The rule is about documenting the component's external
attribute/property API, not about the mechanism of property declaration.

Underscore-prefixed internal state (`_loading`, `_tokens`, etc.) is
intentionally NOT documented — by convention it is private and not part
of the component's external API.

Components that dispatch custom events should additionally declare
`@fires eventName - description` for each `CustomEvent` they bubble.

```js
/**
 * @property {string} variant - One of: light, info, primary, danger,
 *   secondary, success, warning
 * @property {boolean} disabled - Disables the button
 * @fires cts-click - When the inner button is activated
 */
class CtsButton extends LitElement {
  static properties = {
    variant: { type: String },
    disabled: { type: Boolean },
  };
  ...
}
```

This is enforced by **two complementary checks**, both of which run in
`npm run test:ci` (locally and in the `frontend_lint` CI job). They
catch different failure modes and do not duplicate each other:

- **`npm run lint:jsdoc`** (the shell script at
  `frontend/scripts/lint-jsdoc-properties.sh`) — a **presence** check
  with zero false positives. It asserts every `cts-*.js` carries at
  least one `@property` tag. If the JSDoc block is missing entirely,
  this is the check that catches it.
- **`eslint-plugin-jsdoc`** (runs as part of `npm run lint`) — a
  **semantic** check _inside_ present blocks. It catches malformed
  tags, property names that don't match the declared `static properties`,
  and bad types via `require-property` / `check-property-names`. If the
  JSDoc block exists but is wrong, this is the check that catches it.

Both checks are kept intentionally — a parser-based tool dilutes the
one signal the presence check gives, and a presence check can't see
inside a malformed block. See
[`docs/solutions/best-practices/jsdoc-property-presence-lint-2026-04-17.md`](../../../../../docs/solutions/best-practices/jsdoc-property-presence-lint-2026-04-17.md)
for the decision record.

Reviewer judgement — accurate descriptions, complete coverage of
non-obvious semantics — remains a code-review concern beyond what
either tool catches.

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
  light: "btn-light",
  info: "btn-info",
  primary: "btn-primary",
  danger: "btn-danger",
  secondary: "btn-secondary",
  success: "btn-success",
  warning: "btn-warning",
};
```

Never use string interpolation like `` `btn-${variant}` ``. Reasons:

1. Bundlers and linters cannot statically analyse which classes are used.
2. An unknown/misspelled variant silently produces an invalid class name;
   a lookup table returns `undefined` (clearly wrong) or you can fall back
   to a safe default with `VARIANT_CLASSES[variant] || 'btn-light'`.
3. CSP `style-src` configurations may reject dynamically-constructed class
   references.

**For toggled-on/off class names in Lit templates**, use the `classMap`
directive instead of concatenating strings into the `class` attribute:

```js
import { classMap } from "lit/directives/class-map.js";

// GOOD
html`<a class=${classMap({ "nav-link": true, active: this.isActive })}>…</a>`;

// BAD — string concatenation
html`<a class="nav-link${this.isActive ? " active" : ""}">…</a>`;
```

`classMap` is resolved at binding time, not via string interpolation, so
Lit's template compiler can cache the static parts correctly.

---

## 8. Page integration

Every page that uses a `cts-*` component must:

1. Include the `<script type="importmap">` that maps `lit` to the vendored
   bundle. This is already present in all HTML pages via the shared layout.

2. Add a `<script type="module">` tag for **each component** used on that page:

```html
<script type="importmap">
  {"imports":{"lit":"/vendor/lit/lit.js",...}}
</script>
<script type="module" src="/components/cts-button.js"></script>
<script type="module" src="/components/cts-navbar.js"></script>
```

The `importmap` must appear **before** any `type="module"` script. Browsers
process import maps synchronously; a module script that imports `lit` before
the map is parsed will throw a resolution error.

The map also aliases every `lit/directives/*.js` specifier to the same
single bundle (`/vendor/lit/lit.js`, the `lit-all.min.js` build), so
`import { repeat } from "lit/directives/repeat.js"` resolves without
shipping a separate file per directive. A `window.litDisableBundleWarning
= true` flag is set before the importmap to suppress Lit's on-load
bundle-size notice (the bundle is chosen, not a mistake).

**When adding a new directive entry** to the importmap, update all 10
HTML pages under `src/main/resources/static/*.html` and add the new
specifier to the `DIRECTIVE_PROBES` list in
`frontend/e2e/lit-importmap.spec.js`. The importmap JSON is
copy-pasted verbatim across pages; there is no single-source mechanism
today. The Playwright drift-detection spec is the only automated
backstop — it catches a page where the entry was forgotten, but only
if the entry appears in `DIRECTIVE_PROBES`.

---

## 9. Directives available at runtime

The vendored bundle exports every standard Lit directive. Import each
from its sub-path — the importmap aliases them all back to the same
bundle at runtime, so there is no per-directive network cost.

| Directive         | Import path                          | Use for                                                                                                     |
| ----------------- | ------------------------------------ | ----------------------------------------------------------------------------------------------------------- |
| `classMap`        | `lit/directives/class-map.js`        | Toggled-on/off class names (see §7).                                                                        |
| `styleMap`        | `lit/directives/style-map.js`        | Dynamic inline style objects.                                                                               |
| `when`            | `lit/directives/when.js`             | `when(cond, ifTrue, ifFalse?)` — clearer than long ternaries.                                               |
| `choose`          | `lit/directives/choose.js`           | Switch-style multi-branch rendering.                                                                        |
| `repeat`          | `lit/directives/repeat.js`           | Keyed list rendering: `repeat(items, keyFn, templateFn)`. Use when item identity matters across re-renders. |
| `map`             | `lit/directives/map.js`              | Unkeyed list rendering alternative to `.map()`.                                                             |
| `join`            | `lit/directives/join.js`             | Interpose a separator between rendered items.                                                               |
| `range`           | `lit/directives/range.js`            | Range iteration inside templates.                                                                           |
| `keyed`           | `lit/directives/keyed.js`            | Force re-render of a subtree when a key changes.                                                            |
| `guard`           | `lit/directives/guard.js`            | Memoize a subtree on dependency array.                                                                      |
| `cache`           | `lit/directives/cache.js`            | Cache alternate subtrees across conditional swaps.                                                          |
| `ifDefined`       | `lit/directives/if-defined.js`       | Omit an attribute when the value is `undefined`.                                                            |
| `live`            | `lit/directives/live.js`             | Bind to the live DOM value (forms where DOM can drift from state).                                          |
| `ref`             | `lit/directives/ref.js`              | Get a reference to an element — replaces `querySelector` patterns.                                          |
| `until`           | `lit/directives/until.js`            | Render a placeholder until a promise resolves.                                                              |
| `asyncAppend`     | `lit/directives/async-append.js`     | Append items from an async iterable.                                                                        |
| `asyncReplace`    | `lit/directives/async-replace.js`    | Replace content as an async iterable yields.                                                                |
| `templateContent` | `lit/directives/template-content.js` | Render cloned `<template>` element contents.                                                                |
| `unsafeHTML`      | `lit/directives/unsafe-html.js`      | Render HTML from a trusted string (audit every use).                                                        |
| `unsafeSVG`       | `lit/directives/unsafe-svg.js`       | Render SVG from a trusted string (audit every use).                                                         |
| `unsafeMathml`    | `lit/directives/unsafe-mathml.js`    | Render MathML from a trusted string (audit every use).                                                      |

Canonical docs: https://lit.dev/docs/templates/directives/

Every directive is checked by `ts-lit-plugin` (IDE) and `lit-analyzer`
CLI (under `npm run test:ci`). A misspelled import path or wrong binding
sigil fails the lint step.

---

## Quick reference

| Component               | Base class  | Reactive? | Notes                                                                                                 |
| ----------------------- | ----------- | --------- | ----------------------------------------------------------------------------------------------------- |
| `cts-alert`             | HTMLElement | No        | Optional dismiss; fires `cts-alert-dismissed`                                                         |
| `cts-badge`             | HTMLElement | Partial   | Uses `observedAttributes` for attribute-driven re-render                                              |
| `cts-batch-runner`      | LitElement  | Yes       | Dispatches `cts-run-all` / `cts-run-remaining`                                                        |
| `cts-button`            | LitElement  | Yes       | Variant, size, loading, disabled                                                                      |
| `cts-card`              | HTMLElement | No        | One-shot `connectedCallback` — wraps children in Bootstrap card markup                                |
| `cts-config-form`       | LitElement  | Yes       | JSON-schema-driven form; schema/uiSchema/config/errors as Object props                                |
| `cts-dashboard`         | LitElement  | Yes       | Home-page card grid; fetches `/api/server` for footer info                                            |
| `cts-form-field`        | LitElement  | Yes       | Schema-driven input field                                                                             |
| `cts-icon`              | LitElement  | Yes       | Renders a Bootstrap Icon `<span>` from a name and size                                                |
| `cts-image-upload`      | LitElement  | Yes       | Multi-image upload widget; fires `cts-image-uploaded`                                                 |
| `cts-link-button`       | LitElement  | Yes       | Same shape as cts-button but renders `<a>`                                                            |
| `cts-log-detail-header` | LitElement  | Yes       | Header for log-detail page; dispatches several action events                                          |
| `cts-log-entry`         | LitElement  | Yes       | Single log line; supports block start/end formatting                                                  |
| `cts-log-viewer`        | LitElement  | Yes       | Polls `/api/log/:id`; surfaces persistent failures as a banner                                        |
| `cts-login-page`        | LitElement  | Yes       | Login form with OAuth2 buttons and logout-message slot                                                |
| `cts-modal`             | HTMLElement | No        | Wraps Bootstrap 5 Modal; exposes `show()`/`hide()`                                                    |
| `cts-navbar`            | LitElement  | Yes       | Fetches user via `/api/currentuser` on connect                                                        |
| `cts-plan-actions`      | LitElement  | Yes       | Plan-detail action bar; dispatches publish/delete/certify/etc. events                                 |
| `cts-plan-detail`       | LitElement  | Yes       | Composite: header + modules + actions. See `cts-plan-detail.stories.js` for the sub-component stories |
| `cts-plan-header`       | LitElement  | Yes       | Sub-component of cts-plan-detail                                                                      |
| `cts-plan-list`         | LitElement  | Yes       | Plans table; dispatches `cts-plan-navigate`                                                           |
| `cts-plan-modules`      | LitElement  | Yes       | Sub-component of cts-plan-detail; dispatches run/download events                                      |
| `cts-running-test-card` | LitElement  | Yes       | Active-test panel; dispatches `cts-download-log`                                                      |
| `cts-spec-cascade`      | LitElement  | Yes       | Family → entity → version → plan dropdowns; dispatches `cts-plan-selected`                            |
| `cts-tabs`              | HTMLElement | No        | Restructures `<cts-tab-panel>` children into WCAG tablist; dispatches `cts-tab-change`                |
| `cts-test-selector`     | LitElement  | Yes       | Plan-selector UI; dispatches `cts-plan-select`                                                        |
| `cts-token-manager`     | LitElement  | Yes       | Token CRUD; fetches `/api/token`                                                                      |
| `cts-tooltip`           | HTMLElement | No        | Wraps Bootstrap 5 Tooltip on first child                                                              |
