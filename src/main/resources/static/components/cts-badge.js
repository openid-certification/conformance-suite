import { LitElement, html, nothing } from "lit";

const RESULT_VARIANT_CLASSES = {
  success: "result-success",
  failure: "result-failure",
  warning: "result-warning",
  review: "result-review",
  skipped: "result-skipped",
  interrupted: "result-interrupted",
  info: "result-info",
  finished: "result-finished",
};

const BOOTSTRAP_VARIANT_CLASSES = {
  primary: "bg-primary",
  secondary: "bg-secondary",
  danger: "bg-danger",
  light: "bg-light",
  dark: "bg-dark",
};

class CtsBadge extends LitElement {
  static properties = {
    variant: { type: String },
    label: { type: String },
    count: { type: Number },
    pill: { type: Boolean },
    clickable: { type: Boolean },
  };

  constructor() {
    super();
    this.variant = "info";
    this.label = "";
    this.count = undefined;
    this.pill = false;
    this.clickable = false;
  }

  createRenderRoot() { return this; }

  _variantClass() {
    return RESULT_VARIANT_CLASSES[this.variant]
      || BOOTSTRAP_VARIANT_CLASSES[this.variant]
      || "bg-info";
  }

  _handleClick() {
    if (!this.clickable) return;
    this.dispatchEvent(
      new CustomEvent("cts-badge-click", { bubbles: true, composed: true }),
    );
  }

  _handleKeyDown(e) {
    if (!this.clickable) return;
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      this._handleClick();
    }
  }

  render() {
    const variantClass = this._variantClass();
    const pillClass = this.pill ? " rounded-pill" : "";
    const content = this.count !== undefined ? this.count : this.label;

    return html`<span
      class="badge ${variantClass}${pillClass}"
      role="${this.clickable ? "button" : nothing}"
      tabindex="${this.clickable ? "0" : nothing}"
      @click="${this._handleClick}"
      @keydown="${this._handleKeyDown}"
      >${content}</span
    >`;
  }
}

customElements.define("cts-badge", CtsBadge);
