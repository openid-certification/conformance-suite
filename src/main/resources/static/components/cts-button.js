import { LitElement, html, nothing } from "lit";
import "./cts-icon.js";

/**
 * Maps variant name → OIDF token-styled modifier class.
 *
 * Wave-1 (U4) used Bootstrap's `btn-{variant}` table here. U5 swaps that for
 * the OIDF token-styled `oidf-btn-*` set defined inline below. The legacy
 * variant names (`light`, `info`, `success`, `warning`, `dark`) remain as
 * aliases to keep call sites that still pass them rendering correctly per
 * the plan's Variant Migration table:
 *
 *   - `light`   → `secondary` (white + ink-300 border)
 *   - `info`    → `primary`   (orange-400 CTA)
 *   - `success` → `secondary` (no tone accent yet)
 *   - `warning` → `secondary`
 *   - `dark`    → `secondary`
 *   - `outline-*` → corresponding non-outline (the visual gap closed once
 *                   `secondary` became the white-with-border variant)
 *
 * @type {Object.<string, string>}
 */
const VARIANT_CLASSES = {
  primary: "oidf-btn-primary",
  secondary: "oidf-btn-secondary",
  ghost: "oidf-btn-ghost",
  danger: "oidf-btn-danger",
  // Legacy aliases — see Variant Migration table above.
  light: "oidf-btn-secondary",
  info: "oidf-btn-primary",
  success: "oidf-btn-secondary",
  warning: "oidf-btn-secondary",
  dark: "oidf-btn-secondary",
  "outline-light": "oidf-btn-secondary",
  "outline-info": "oidf-btn-primary",
  "outline-primary": "oidf-btn-primary",
  "outline-danger": "oidf-btn-danger",
  "outline-secondary": "oidf-btn-secondary",
  "outline-success": "oidf-btn-secondary",
  "outline-warning": "oidf-btn-secondary",
  "outline-dark": "oidf-btn-secondary",
};

/**
 * Maps size name → OIDF token-styled size modifier class. The default `md`
 * size carries no modifier — height/padding come from the base `.oidf-btn`
 * rule.
 *
 * @type {Object.<string, string>}
 */
const SIZE_CLASSES = {
  sm: "oidf-btn-sm",
  md: "",
  lg: "oidf-btn-lg",
};

/**
 * Build the full OIDF button class string.
 *
 * @param {Object} options
 * @param {string} [options.variant="secondary"] - Variant key. Unknown values fall back to `secondary`.
 * @param {string} [options.size="sm"] - Size key. Unknown values fall back to `sm`.
 * @returns {string} Full class string, e.g. `"oidf-btn oidf-btn-sm oidf-btn-primary"`
 */
function buildButtonClasses({ variant = "secondary", size = "sm" } = {}) {
  const variantClass = VARIANT_CLASSES[variant] ?? "oidf-btn-secondary";
  const sizeClass = SIZE_CLASSES[size] ?? "oidf-btn-sm";
  const sizeSegment = sizeClass ? `${sizeClass} ` : "";
  return `oidf-btn ${sizeSegment}${variantClass}`;
}

const STYLE_ID = "cts-button-styles";

// Scoped CSS for OIDF buttons. Mirrors
// project/preview/components-buttons.html from the OIDF design archive: a
// 36px-tall token-driven button surface with primary/secondary/ghost/danger
// variants and sm/md/lg sizes. Used by both cts-button and cts-link-button.
const STYLE_TEXT = `
.oidf-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  height: 36px;
  padding: 0 var(--space-4);
  font-family: var(--font-sans);
  font-size: var(--fs-14);
  font-weight: var(--fw-bold);
  /* Even-pixel line-height so Inter's cap-height resolves to whole pixels.
     A 16px line-box on 13px (sm), 14px (md), or 15px (lg, overridden below)
     text leaves consistent breathing room and aligns the text baseline with
     adjacent inline chrome (badges, link-buttons) sharing the same 16px
     line-box. line-height:1 produced odd 13/14/15px boxes whose cap-heights
     drifted by sub-pixel amounts when mixed inline. */
  line-height: 16px;
  border: 1px solid transparent;
  border-radius: var(--radius-2);
  background: transparent;
  color: var(--ink-900);
  cursor: pointer;
  text-decoration: none;
  box-sizing: border-box;
  transition: background var(--dur-1) var(--ease-standard),
              border-color var(--dur-1) var(--ease-standard),
              color var(--dur-1) var(--ease-standard);
}
.oidf-btn:hover {
  text-decoration: none;
}
.oidf-btn:focus {
  outline: none;
}
.oidf-btn:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
}
.oidf-btn[disabled],
.oidf-btn[aria-disabled="true"] {
  opacity: 0.55;
  cursor: not-allowed;
  pointer-events: none;
}

.oidf-btn-primary {
  background: var(--orange-400);
  color: var(--ink-0);
  border-color: var(--orange-400);
}
.oidf-btn-primary:hover {
  background: var(--orange-500);
  border-color: var(--orange-500);
  color: var(--ink-0);
}

.oidf-btn-secondary {
  background: var(--ink-0);
  color: var(--ink-900);
  border-color: var(--ink-300);
}
.oidf-btn-secondary:hover {
  background: var(--ink-50);
  color: var(--ink-900);
}

.oidf-btn-ghost {
  background: transparent;
  color: var(--ink-900);
  border-color: transparent;
}
.oidf-btn-ghost:hover {
  background: var(--ink-100);
  color: var(--ink-900);
}

.oidf-btn-danger {
  background: var(--rust-400);
  color: var(--ink-0);
  border-color: var(--rust-400);
}
.oidf-btn-danger:hover {
  background: var(--rust-500);
  border-color: var(--rust-500);
  color: var(--ink-0);
}

.oidf-btn-sm {
  height: 30px;
  padding: 0 var(--space-3);
  font-size: var(--fs-13);
}
.oidf-btn-lg {
  height: 44px;
  padding: 0 var(--space-5);
  font-size: var(--fs-15);
  /* 4px-grid line-height for the larger 15px text. */
  line-height: 20px;
}

.oidf-btn-spinner {
  width: 12px;
  height: 12px;
  animation: oidf-btn-spin 0.9s linear infinite;
}
.oidf-btn-spinner-track {
  stroke: var(--ink-300);
}
.oidf-btn-spinner-head {
  stroke: var(--orange-400);
}
@keyframes oidf-btn-spin {
  to { transform: rotate(360deg); }
}

cts-button[full-width],
cts-link-button[full-width] {
  display: block;
}
cts-button[full-width] .oidf-btn,
cts-link-button[full-width] .oidf-btn {
  width: 100%;
}
`;

function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * OIDF token-styled button. Dispatches a bubbling `cts-click` event in
 * addition to the native click.
 *
 * @property {string} variant - One of: primary, secondary, ghost, danger.
 *   Legacy aliases accepted: light/info/success/warning/dark and their
 *   `outline-*` forms (each maps to the closest new variant).
 * @property {string} size - One of: sm (default), md, lg
 * @property {string} label - Visible text
 * @property {string} icon - Bootstrap Icons name (without the `bi-` prefix)
 * @property {boolean} loading - Shows a spinner and disables the button
 * @property {boolean} disabled - Disables the button
 * @property {string} type - Native button type: "button" (default) or "submit"
 * @property {boolean} full-width - Stretches the button to fill its parent's width
 * @fires cts-click - When the inner button is activated (not fired while disabled or loading)
 *
 * ## Programmatic activation
 *
 * `host.click()` and `$(host).trigger('click')` fire a synthetic click on the
 * cts-button host element, NOT on the inner `<button>`. Lit's `@click` handler
 * is registered on the inner button, so `_handleClick` does not run and
 * `cts-click` is not dispatched. User clicks still work because the native
 * click bubbles from inner button to host.
 *
 * To activate a cts-button programmatically from tests or automation:
 *
 * ```js
 * // GOOD — triggers the Lit click handler and dispatches cts-click
 * host.querySelector('button').click();
 *
 * // GOOD — listen for cts-click if you want the disabled/loading guard to fire
 * host.addEventListener('cts-click', handler);
 * ```
 *
 * ## Light-DOM dependencies
 *
 * cts-button intentionally renders to its own light DOM (see
 * `createRenderRoot()`). ClipboardJS (`.btn-clipboard`), Bootstrap 5 data
 * attributes (`data-bs-dismiss`, `data-bs-toggle`), and jQuery delegated
 * handlers rely on the native click bubbling from the inner button through
 * the host — all of these break silently if this component ever switches to
 * shadow DOM or adds `event.preventDefault()` inside `_handleClick`. Do not
 * change the render root without first migrating every consumer.
 */
class CtsButton extends LitElement {
  static properties = {
    variant: { type: String },
    size: { type: String },
    label: { type: String },
    icon: { type: String },
    loading: { type: Boolean },
    disabled: { type: Boolean },
    type: { type: String },
    fullWidth: { type: Boolean, attribute: "full-width", reflect: true },
  };

  constructor() {
    super();
    this.variant = "secondary";
    this.size = "sm";
    this.label = "";
    this.icon = "";
    this.loading = false;
    this.disabled = false;
    this.type = "button";
    this.fullWidth = false;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  createRenderRoot() {
    return this;
  }

  _handleClick() {
    if (this.disabled || this.loading) return;
    this.dispatchEvent(new CustomEvent("cts-click", { bubbles: true, composed: true }));
  }

  _renderIcon() {
    if (this.loading) {
      // Inline SVG ring spinner — replaces Bootstrap's spinner-border markup.
      // Sized 12×12 to match the previous spinner-border-sm footprint, with
      // an ink-300 track and an orange-400 head driven by token vars.
      return html`<svg
        class="oidf-btn-spinner"
        viewBox="0 0 16 16"
        role="status"
        aria-hidden="true"
      >
        <circle
          class="oidf-btn-spinner-track"
          cx="8"
          cy="8"
          r="6"
          fill="none"
          stroke-width="2"
        ></circle>
        <path
          class="oidf-btn-spinner-head"
          d="M14 8a6 6 0 0 0-6-6"
          fill="none"
          stroke-width="2"
          stroke-linecap="round"
        ></path>
      </svg>`;
    }
    if (this.icon) {
      return html`<cts-icon name="${this.icon}" aria-hidden="true"></cts-icon>`;
    }
    return nothing;
  }

  render() {
    const isDisabled = this.disabled || this.loading;
    const iconContent = this._renderIcon();
    const hasIcon = iconContent !== nothing;
    const buttonClass = buildButtonClasses({
      variant: this.variant,
      size: this.size,
    });
    return html`<button
      type="${this.type}"
      class="${buttonClass}"
      ?disabled="${isDisabled}"
      @click="${this._handleClick}"
      >${iconContent}${hasIcon && this.label ? " " : ""}${this.label}</button
    >`;
  }
}

customElements.define("cts-button", CtsButton);

export {};
