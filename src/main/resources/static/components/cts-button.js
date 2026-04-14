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

class CtsButton extends LitElement {
  static properties = {
    variant: { type: String },
    label: { type: String },
    icon: { type: String },
    loading: { type: Boolean },
    disabled: { type: Boolean },
    type: { type: String },
  };

  constructor() {
    super();
    this.variant = "light";
    this.label = "";
    this.icon = "";
    this.loading = false;
    this.disabled = false;
    this.type = "button";
  }

  createRenderRoot() { return this; }

  _handleClick() {
    if (this.disabled || this.loading) return;
    this.dispatchEvent(
      new CustomEvent("cts-click", { bubbles: true, composed: true }),
    );
  }

  // Icon names come from the Bootstrap Icons set (2000+ icons).
  // Constructed from the icon prop, not a finite variant set.
  _iconClass() {
    return `bi bi-${this.icon}`;
  }

  _renderIcon() {
    if (this.loading) {
      return html`<span
        class="spinner-border spinner-border-sm"
        role="status"
        aria-hidden="true"
      ></span>`;
    }
    if (this.icon) {
      return html`<span
        class="${this._iconClass()}"
        aria-hidden="true"
      ></span>`;
    }
    return nothing;
  }

  render() {
    const isDisabled = this.disabled || this.loading;
    const iconContent = this._renderIcon();
    const hasIcon = iconContent !== nothing;
    const variantClass = VARIANT_CLASSES[this.variant] || "btn-light";
    return html`<button
      type="${this.type}"
      class="btn btn-sm ${variantClass} bg-gradient border border-secondary"
      ?disabled="${isDisabled}"
      @click="${this._handleClick}"
    >${iconContent}${hasIcon && this.label ? " " : ""}${this.label
        ? this.label
        : nothing}</button
    >`;
  }
}

customElements.define("cts-button", CtsButton);
