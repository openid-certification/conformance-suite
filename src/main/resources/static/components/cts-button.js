import { LitElement, html, nothing } from "lit";

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

  // Use light DOM so Bootstrap CSS applies
  createRenderRoot() {
    return this;
  }

  _handleClick() {
    if (this.disabled || this.loading) return;
    this.dispatchEvent(
      new CustomEvent("cts-click", { bubbles: true, composed: true }),
    );
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
        class="bi bi-${this.icon}"
        aria-hidden="true"
      ></span>`;
    }
    return nothing;
  }

  render() {
    const isDisabled = this.disabled || this.loading;
    const iconContent = this._renderIcon();
    const hasIcon = iconContent !== nothing;
    return html`<button
      type="${this.type}"
      class="btn btn-sm btn-${this.variant} bg-gradient border border-secondary"
      ?disabled="${isDisabled}"
      @click="${this._handleClick}"
    >${iconContent}${hasIcon && this.label ? " " : ""}${this.label
        ? this.label
        : nothing}</button
    >`;
  }
}

customElements.define("cts-button", CtsButton);
