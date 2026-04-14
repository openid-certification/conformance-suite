import { LitElement, html, nothing } from "lit";

const RESULT_VARIANTS = [
  "success",
  "failure",
  "warning",
  "review",
  "skipped",
  "interrupted",
  "info",
  "finished",
];

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

  // Use light DOM so Bootstrap CSS applies
  createRenderRoot() {
    return this;
  }

  _variantClass() {
    if (RESULT_VARIANTS.includes(this.variant)) {
      return `result-${this.variant}`;
    }
    return `bg-${this.variant}`;
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
