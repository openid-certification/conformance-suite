import { LitElement, html, nothing } from "lit";

/** @type {Object.<string, string>} Maps variant name → Bootstrap modifier class */
const VARIANT_CLASSES = {
  light: "btn-light",
  info: "btn-info",
  primary: "btn-primary",
  danger: "btn-danger",
  secondary: "btn-secondary",
  success: "btn-success",
  warning: "btn-warning",
  dark: "btn-dark",
  "outline-light": "btn-outline-light",
  "outline-info": "btn-outline-info",
  "outline-primary": "btn-outline-primary",
  "outline-danger": "btn-outline-danger",
  "outline-secondary": "btn-outline-secondary",
  "outline-success": "btn-outline-success",
  "outline-warning": "btn-outline-warning",
  "outline-dark": "btn-outline-dark",
};

/** @type {Object.<string, string>} Maps size name → Bootstrap modifier class (empty string = no class for md) */
const SIZE_CLASSES = {
  sm: "btn-sm",
  md: "",
  lg: "btn-lg",
};

/**
 * Build the full Bootstrap button class string.
 *
 * @param {Object} options
 * @param {string} [options.variant="light"] - Variant key. Unknown values fall back to "light".
 * @param {string} [options.size="sm"] - Size key. Unknown values fall back to "sm".
 * @returns {string} Full class string, e.g. `"btn btn-sm btn-primary"`
 */
function buildButtonClasses({ variant = "light", size = "sm" } = {}) {
  const variantClass = VARIANT_CLASSES[variant] ?? "btn-light";
  const sizeClass = SIZE_CLASSES[size] ?? "btn-sm";
  const sizeSegment = sizeClass ? `${sizeClass} ` : "";
  return `btn ${sizeSegment}${variantClass}`;
}

/**
 * Bootstrap-styled anchor that behaves like a button.
 * @property {string} href - Target URL. Omitted when `disabled`.
 * @property {string} variant - One of: light, info, primary, danger, secondary, success, warning
 * @property {string} size - One of: sm (default), md, lg
 * @property {string} label - Visible text
 * @property {string} icon - Bootstrap Icons name (without the `bi-` prefix)
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
    disabled: { type: Boolean },
    fullWidth: { type: Boolean, attribute: "full-width", reflect: true },
  };

  constructor() {
    super();
    this.href = "#";
    this.variant = "light";
    this.size = "sm";
    this.label = "";
    this.icon = "";
    this.disabled = false;
    this.fullWidth = false;
  }

  createRenderRoot() {
    return this;
  }

  updated(changed) {
    if (changed.has("fullWidth")) {
      // Light-DOM components can't style their own host from CSS;
      // set display imperatively so the host stretches in block/flex/grid parents.
      this.style.display = this.fullWidth ? "block" : "";
    }
  }

  // Icon names come from the Bootstrap Icons set (2000+ icons).
  // Constructed from the icon prop, not a finite variant set.
  _iconClass() {
    return `bi bi-${this.icon}`;
  }

  _renderIcon() {
    if (this.icon) {
      return html`<span class="${this._iconClass()}" aria-hidden="true"></span>`;
    }
    return nothing;
  }

  render() {
    const iconContent = this._renderIcon();
    const hasIcon = iconContent !== nothing;
    const disabledClass = this.disabled ? " disabled" : "";
    const anchorClass =
      buildButtonClasses({ variant: this.variant, size: this.size }) + disabledClass;
    return html`<a
      class="${anchorClass}"
      href=${this.disabled ? nothing : this.href}
      role="button"
      aria-disabled=${this.disabled ? "true" : nothing}
      tabindex=${this.disabled ? "-1" : nothing}
      >${iconContent}${hasIcon && this.label ? " " : ""}${this.label}</a
    >`;
  }
}

customElements.define("cts-link-button", CtsLinkButton);
