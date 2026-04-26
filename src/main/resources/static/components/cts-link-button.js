import { LitElement, html, nothing } from "lit";
import "./cts-icon.js";

/**
 * Maps variant name → OIDF token-styled modifier class.
 *
 * Mirrors the table in cts-button.js — kept as a sibling copy rather than
 * imported so that loading either component independently still injects the
 * shared `<style id="cts-button-styles">` (gated by the `STYLE_ID` flag in
 * both files, so a page that uses both gets exactly one stylesheet).
 *
 * Legacy aliases (`light`, `info`, `success`, `warning`, `dark`,
 * `outline-*`) follow the same Variant Migration table documented in
 * cts-button.js.
 *
 * @type {Object.<string, string>}
 */
const VARIANT_CLASSES = {
  primary: "oidf-btn-primary",
  secondary: "oidf-btn-secondary",
  ghost: "oidf-btn-ghost",
  danger: "oidf-btn-danger",
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
 * Maps size name → OIDF token-styled size modifier class.
 *
 * @type {Object.<string, string>}
 */
const SIZE_CLASSES = {
  xs: "oidf-btn-xs",
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

// Shared with cts-button.js — both components inject the same <style> block
// keyed on this id, so the second one to load is a no-op. This lets either
// component be loaded independently and still pick up the shared OIDF
// button surface styles defined in cts-button.js.
const STYLE_ID = "cts-button-styles";

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
  font-weight: var(--fw-medium);
  line-height: 1;
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

.oidf-btn-xs {
  /* Compact size used to replace bespoke "More" / pill-style buttons. 24px
     = 16px line-box + 4px top + 4px bottom on the 4px grid. fs-12 +
     space-1 horizontal padding keeps it visibly smaller than sm. Kept in
     sync with cts-button.js. */
  height: 24px;
  padding: 0 var(--space-1);
  font-size: var(--fs-12);
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
 * OIDF token-styled anchor that behaves like a button.
 *
 * @property {string} href - Target URL. Omitted when `disabled`.
 * @property {string} variant - One of: primary, secondary, ghost, danger.
 *   Legacy aliases accepted: light/info/success/warning/dark and their
 *   `outline-*` forms.
 * @property {string} size - One of: xs, sm (default), md, lg
 * @property {string} label - Visible text
 * @property {string} icon - Bootstrap Icons name (without the `bi-` prefix).
 *   When `target="_blank"` is set and `icon` is omitted, defaults to
 *   `box-arrow-up-right` to signal the link opens in a new tab.
 * @property {string} target - Anchor target attribute (e.g. `_blank`). When
 *   `_blank` is used, `rel="noopener noreferrer"` is added automatically.
 * @property {boolean} disabled - Renders as a disabled link (no href, aria-disabled, tabindex=-1)
 * @property {boolean} full-width - Stretches the button to fill its parent's width
 */
class CtsLinkButton extends LitElement {
  static properties = {
    href: { type: String },
    variant: { type: String },
    size: { type: String },
    label: { type: String },
    icon: { type: String },
    target: { type: String },
    disabled: { type: Boolean },
    fullWidth: { type: Boolean, attribute: "full-width", reflect: true },
  };

  constructor() {
    super();
    this.href = "#";
    this.variant = "secondary";
    this.size = "sm";
    this.label = "";
    this.icon = "";
    this.target = "";
    this.disabled = false;
    this.fullWidth = false;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  createRenderRoot() {
    return this;
  }

  render() {
    const anchorClass = buildButtonClasses({ variant: this.variant, size: this.size });
    const isExternal = this.target === "_blank";
    const leadingIcon = this.icon
      ? html`<cts-icon name="${this.icon}" aria-hidden="true"></cts-icon>`
      : nothing;
    // External-link indicator goes after the label so it reads as
    // "API Documentation ↗", matching the well-known convention. Only
    // emitted when target=_blank and the caller hasn't supplied a custom icon.
    const trailingIcon =
      isExternal && !this.icon
        ? html`<cts-icon name="external-link" aria-hidden="true"></cts-icon>`
        : nothing;
    const hasLeading = leadingIcon !== nothing;
    const hasTrailing = trailingIcon !== nothing;
    return html`<a
      class="${anchorClass}"
      href=${this.disabled ? nothing : this.href}
      role="button"
      target=${this.target || nothing}
      rel=${isExternal ? "noopener noreferrer" : nothing}
      aria-disabled=${this.disabled ? "true" : nothing}
      tabindex=${this.disabled ? "-1" : nothing}
      >${leadingIcon}${hasLeading && this.label ? " " : ""}${this.label}${hasTrailing && this.label
        ? " "
        : ""}${trailingIcon}</a
    >`;
  }
}

customElements.define("cts-link-button", CtsLinkButton);

export {};
