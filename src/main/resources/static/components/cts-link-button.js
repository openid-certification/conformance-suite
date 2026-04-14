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

class CtsLinkButton extends LitElement {
  static properties = {
    href: { type: String },
    variant: { type: String },
    label: { type: String },
    icon: { type: String },
    disabled: { type: Boolean },
  };

  constructor() {
    super();
    this.href = "#";
    this.variant = "light";
    this.label = "";
    this.icon = "";
    this.disabled = false;
  }

  createRenderRoot() { return this; }

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
    return html`<a
      class="btn btn-sm ${variantClass} bg-gradient border border-secondary${disabledClass}"
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
