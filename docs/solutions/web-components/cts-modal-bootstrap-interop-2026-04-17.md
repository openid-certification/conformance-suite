---
date: 2026-04-17
topic: web-components, cts-modal, bootstrap-interop, light-DOM
related: docs/plans/2026-04-17-001-fix-mr1911-pr-review-remediation-plan.md
related-solutions: docs/solutions/web-components/cts-button-host-vs-inner-button-semantics-2026-04-17.md
---

# cts-modal Bootstrap interop: inner `.modal`, not the host

The original `cts-modal` JSDoc described the component as "backward compatible with the `bootstrap.Modal` API." That framing was wrong in a way that would have bitten a future contributor: callers appeared to be passing a cts-modal host element to `bootstrap.Modal.getOrCreateInstance(...)`, but they were actually passing the *inner* `<div class="modal">` that cts-modal renders into its light DOM. The compatibility was coincidental, not contractual.

Retracting the claim and replacing it with an explicit contract prevents a regression the moment someone restructures the render tree (shadow DOM, nested wrapper, different id-transfer strategy).

## The model

`cts-modal` is a vanilla `HTMLElement` that, in `connectedCallback`, creates a Bootstrap-shaped subtree and appends it into its own light DOM:

```
<cts-modal id="X">              ← HOST (custom element). `id` is transferred away in connectedCallback.
  <div class="modal" id="X" role="dialog" aria-modal="true">   ← INNER. This is the Bootstrap integration point.
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">…</div>
        <div class="modal-body">…</div>
        <div class="modal-footer">…</div>
      </div>
    </div>
  </div>
</cts-modal>
```

The `id` transfer is load-bearing: `document.getElementById("X")` returns the inner `<div class="modal">`, not the host. Existing call sites (e.g. `privateLinkModals.html`, callers in `fapi.ui.js`) rely on this — they call `bootstrap.Modal.getOrCreateInstance(document.getElementById("X"))` and it works because `X` resolves to the inner div.

## What works (use these)

- **`document.getElementById("X")` → `bootstrap.Modal.getOrCreateInstance(el).show()`.** The id-transfer convention makes this the canonical integration path. Keep it.
- **`ctsModalHost.show()` / `ctsModalHost.hide()`.** The component exposes imperative methods that internally do `bootstrap.Modal.getOrCreateInstance(this._modalEl).show()`. Prefer these when you already have a reference to the `<cts-modal>` host element.
- **`hidden.bs.modal` → `cts-modal-close` event.** The component rebroadcasts Bootstrap's `hidden.bs.modal` as a composed, bubbling `cts-modal-close` CustomEvent. Listen for `cts-modal-close` on any ancestor of the `<cts-modal>` host.
- **Footer-button icons via `{ "icon": "box-arrow-in-right" }`.** The descriptor accepts a Bootstrap Icon name matching `[a-z0-9-]+`. Values outside that character set are rejected (no `<span class="bi bi-">` is rendered — an invisible broken icon is worse than no icon).
- **Outline variants via `{ "class": "btn-outline-primary" }`.** `_button-classes.js` knows the outline family. Unknown `btn-*` values are still passed through as additive classes (no silent downgrade to `btn-light`).

## What breaks silently (avoid these)

| Pattern | Why it breaks | Use instead |
|---------|---------------|-------------|
| `bootstrap.Modal.getOrCreateInstance(ctsModalHostEl)` | Works today only because the id-transfer makes the inner `.modal` the element with the transferred id. Pass the host directly and Bootstrap attaches its internal state to the wrong node. Any future change to the render tree breaks this path silently. | `bootstrap.Modal.getOrCreateInstance(document.getElementById("X"))` — or `ctsModalHostEl.show()` |
| Re-assigning the cts-modal host's markup after connect | The body captures children once, at `connectedCallback`. Mutating the body later is fine *for content updates*, but wholesale replacement of the host's markup wipes the already-built inner `.modal` and leaves the component with a stale `_modalEl` reference. | Mutate the inner `.modal-body` directly (or re-render the cts-modal via `element.remove()` + reinsertion). |
| `document.getElementById("X").classList.add("modal-xl")` on a running modal | Bootstrap caches size-related layout on show(). Adding a size class after the first show is inconsistent across browsers. | Set `size="xl"` on `<cts-modal>` before first show, or destroy+recreate. |
| Passing unsanitized strings into `footer-buttons` icon field | The component rejects non-`[a-z0-9-]` values — but callers that paste user input into the descriptor still expose injection via `class` or `data` entries. Those fields don't go through the same guard. | Keep `footer-buttons` JSON developer-authored; don't template user input into it. |
| Calling `ctsModalHost.show()` before `connectedCallback` runs | `_modalEl` is set at the end of `connectedCallback`; calling `show()` before that is a no-op. | Wait for the element to be in the document. |

## Operational rule

Treat the `<cts-modal>` host as the **declarative surface** — attributes, id, children, `footer-buttons`, lifecycle events. Treat the inner `<div class="modal">` as the **Bootstrap integration surface** — that is where `bootstrap.Modal.getOrCreateInstance` lives, where `aria-modal`/`aria-labelledby` live, and where show/hide transitions happen.

Any code that reaches into Bootstrap's API should either (a) use the cts-modal host's `show()`/`hide()` methods, or (b) resolve the inner `.modal` via `document.getElementById(X)` and use that. Do not pass the host.

## Related artifacts in this branch

- `src/main/resources/static/components/cts-modal.js` — the class-level JSDoc now carries this contract verbatim (`@see` references this doc)
- `src/main/resources/static/components/_button-classes.js` — shared variant lookup; now covers `outline-*` family
- `src/main/resources/static/templates/privateLinkModals.html` — consumer of the new `icon` descriptor field (restores the pre-MR box-arrow-in-right affordance on the Copy button)
- `docs/plans/2026-04-17-001-fix-mr1911-pr-review-remediation-plan.md` — MR !1911 review remediation plan, Unit 4
