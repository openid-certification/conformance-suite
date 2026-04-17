import { LitElement, html, nothing } from "lit";

const VARIANT_CLASSES = {
  light: "btn-light",
  info: "btn-info",
  primary: "btn-primary",
  danger: "btn-danger",
  secondary: "btn-secondary",
  success: "btn-success",
  warning: "btn-warning",
};

/**
 * Bootstrap-styled anchor that behaves like a button.
 *
 * @property {string} href - Target URL. Omitted when `disabled`.
 * @property {string} variant - One of: light, info, primary, danger, secondary, success, warning
 * @property {string} label - Visible text
 * @property {string} icon - Bootstrap Icons name (without the `bi-` prefix)
 * @property {boolean} disabled - Renders as a disabled link (no href, aria-disabled, tabindex=-1)
 * @property {boolean} full-width - Stretches the button to fill its parent's width
 */
class CtsLinkButton extends LitElement {
  static properties = {
    href: { type: String },
    variant: { type: String },
    label: { type: String },
    icon: { type: String },
    disabled: { type: Boolean },
    fullWidth: { type: Boolean, attribute: "full-width", reflect: true },
  };

  constructor() {
    super();
    this.href = "#";
    this.variant = "light";
    this.label = "";
    this.icon = "";
    this.disabled = false;
    this.fullWidth = false;
  }

  createRenderRoot() { return this; }

  updated(changed) {
    if (changed.has("fullWidth")) {
      // Light-DOM components can't style their own host from CSS;
      // set display imperatively so the host stretches in block/flex/grid parents.
      this.style.display = this.fullWidth ? "block" : "";
    }
  }

  // Icon names come from the Bootstrap Icons set (2000+ icons).
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
    const variantClass = VARIANT_CLASSES[this.variant] || "btn-light";
    const disabledClass = this.disabled ? " disabled" : "";
    const widthClass = this.fullWidth ? " w-100" : "";
    return html`<a
      class="btn btn-sm ${variantClass} bg-gradient border border-secondary${disabledClass}${widthClass}"
      href=${this.disabled ? nothing : this.href}
      role="button"
      aria-disabled=${this.disabled ? "true" : nothing}
      tabindex=${this.disabled ? "-1" : nothing}
    >${iconContent}${hasIcon && this.label ? " " : ""}${this.label
        ? this.label
        : nothing}</a
    >`;
  }
}

customElements.define("cts-link-button", CtsLinkButton);
