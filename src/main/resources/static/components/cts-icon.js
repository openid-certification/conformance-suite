import { LitElement, html, nothing } from "lit";

class CtsIcon extends LitElement {
  static properties = {
    name: { type: String },
    size: { type: String },
  };

  constructor() {
    super();
    this.name = "";
    this.size = "md";
  }

  // Use light DOM so Bootstrap CSS applies
  createRenderRoot() {
    return this;
  }

  _sizeClass() {
    if (this.size === "sm") return " fs-6";
    if (this.size === "lg") return " fs-4";
    return "";
  }

  render() {
    if (!this.name) return nothing;
    return html`<span
      class="bi bi-${this.name}${this._sizeClass()}"
      aria-hidden="true"
    ></span>`;
  }
}

customElements.define("cts-icon", CtsIcon);
