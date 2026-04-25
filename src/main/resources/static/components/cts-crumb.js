import { LitElement, html, nothing } from "lit";
import "./cts-icon.js";

const STYLE_ID = "cts-crumb-styles";

// Scoped CSS for the breadcrumb. Inline flex row of ghost-link buttons and a
// terminal `<b>` label, separated by a chevron icon. The link colour comes
// from --fg-link, sized at --fs-13 so the breadcrumb sits comfortably above
// the page title (matches the design archive's plan-detail / log-detail
// header treatment).
const STYLE_TEXT = `
  cts-crumb {
    display: inline-flex;
    align-items: center;
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    line-height: var(--lh-base);
    color: var(--fg);
  }
  cts-crumb .crumbNav {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
  }
  cts-crumb .crumbItem {
    display: inline-flex;
    align-items: center;
  }
  cts-crumb .crumbLink {
    background: transparent;
    border: 0;
    padding: 0;
    margin: 0;
    font: inherit;
    color: var(--fg-link);
    cursor: pointer;
    text-decoration: none;
  }
  cts-crumb .crumbLink:hover {
    text-decoration: underline;
  }
  cts-crumb .crumbLink:focus {
    outline: none;
  }
  cts-crumb .crumbLink:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
    border-radius: var(--radius-1);
  }
  cts-crumb .crumbCurrent {
    font-weight: var(--fw-bold);
    color: var(--fg);
  }
  cts-crumb .crumbSeparator {
    color: var(--fg-faint);
    display: inline-flex;
    align-items: center;
    line-height: 1;
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Breadcrumb trail for plan-detail and log-detail pages. Renders a row of
 * ghost-link buttons separated by chevron icons, with the final entry shown
 * as bold non-interactive text. Click a non-terminal crumb to dispatch
 * `cts-crumb-navigate`; the host page handles routing.
 *
 * Light DOM. Scoped CSS is injected once on first connect.
 *
 * @property {Array<{label: string, target: string}>} items - Ordered crumb
 *   trail. Each entry needs a `label` (rendered text) and a `target` (echoed
 *   in the navigate event detail). The last entry is rendered as a bold
 *   `<b>` label and is not clickable. An empty array renders nothing.
 * @fires cts-crumb-navigate - When a non-terminal crumb is clicked, with
 *   `{ detail: { target } }` matching the clicked item's `target`; bubbles
 *   and is composed.
 */
class CtsCrumb extends LitElement {
  static properties = {
    items: { type: Array },
  };

  constructor() {
    super();
    /** @type {Array<{label: string, target: string}>} */
    this.items = [];
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  _handleClick(target) {
    this.dispatchEvent(
      new CustomEvent("cts-crumb-navigate", {
        bubbles: true,
        composed: true,
        detail: { target },
      }),
    );
  }

  /**
   * Bound click handler for crumb buttons. Reads the target value from the
   * button's data-target attribute and forwards to _handleClick. Bound as a
   * method reference (not an inline arrow) so eslint-plugin-lit's
   * no-template-arrow rule is satisfied.
   *
   * @param {Event} e
   */
  _onCrumbClick = (e) => {
    const btn = /** @type {HTMLElement | null} */ (e.currentTarget);
    if (!btn) return;
    const target = btn.getAttribute("data-target") ?? "";
    this._handleClick(target);
  };

  render() {
    const items = Array.isArray(this.items) ? this.items : [];
    if (items.length === 0) return nothing;

    const lastIndex = items.length - 1;

    return html`<nav aria-label="Breadcrumb" class="crumbNav">
      ${items.map((item, index) => {
        const isLast = index === lastIndex;
        const separator = isLast
          ? nothing
          : html`<span class="crumbSeparator" aria-hidden="true">
              <cts-icon name="chevron-right" size="16"></cts-icon>
            </span>`;
        const content = isLast
          ? html`<b class="crumbCurrent" aria-current="page">${item.label}</b>`
          : html`<button
              type="button"
              class="crumbLink"
              data-target=${item.target}
              @click=${this._onCrumbClick}
            >
              ${item.label}
            </button>`;
        return html`<span class="crumbItem">${content}</span>${separator}`;
      })}
    </nav>`;
  }
}

customElements.define("cts-crumb", CtsCrumb);

export {};
