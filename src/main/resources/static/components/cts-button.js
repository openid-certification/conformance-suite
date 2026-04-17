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
 * Bootstrap-styled button. Dispatches a bubbling `cts-click` event in addition
 * to the native click.
 *
 * @property {string} variant - One of: light, info, primary, danger, secondary, success, warning
 * @property {string} label - Visible text
 * @property {string} icon - Bootstrap Icons name (without the `bi-` prefix)
 * @property {boolean} loading - Shows a spinner and disables the button
 * @property {boolean} disabled - Disables the button
 * @property {string} type - Native button type: "button" (default) or "submit"
 * @property {boolean} full-width - Stretches the button to fill its parent's width
 */
class CtsButton extends LitElement {
  static properties = {
    variant: { type: String },
    label: { type: String },
    icon: { type: String },
    loading: { type: Boolean },
    disabled: { type: Boolean },
    type: { type: String },
    fullWidth: { type: Boolean, attribute: "full-width", reflect: true },
  };

  constructor() {
    super();
    this.variant = "light";
    this.label = "";
    this.icon = "";
    this.loading = false;
    this.disabled = false;
    this.type = "button";
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
    const widthClass = this.fullWidth ? " w-100" : "";
    return html`<button
      type="${this.type}"
      class="btn btn-sm ${variantClass} bg-gradient border border-secondary${widthClass}"
      ?disabled="${isDisabled}"
      @click="${this._handleClick}"
    >${iconContent}${hasIcon && this.label ? " " : ""}${this.label
        ? this.label
        : nothing}</button
    >`;
  }
}

customElements.define("cts-button", CtsButton);
