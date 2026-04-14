import { LitElement, html, nothing } from "lit";

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

  // Use light DOM so Bootstrap CSS applies
  createRenderRoot() {
    return this;
  }

  _renderIcon() {
    if (this.icon) {
      return html`<span
        class="bi bi-${this.icon}"
        aria-hidden="true"
      ></span>`;
    }
    return nothing;
  }

  render() {
    const iconContent = this._renderIcon();
    const hasIcon = iconContent !== nothing;
    return html`<a
      class="btn btn-sm btn-${this.variant} bg-gradient border border-secondary${this.disabled ? " disabled" : ""}"
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
