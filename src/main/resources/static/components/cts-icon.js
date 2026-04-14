import { LitElement, html, nothing } from "lit";

const SIZE_CLASSES = {
  sm: "fs-6",
  lg: "fs-4",
};

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

  createRenderRoot() { return this; }

  // Icon names come from the Bootstrap Icons set (2000+ icons).
  // Constructed from the name prop, not a finite variant set.
  _iconClass() {
    return `bi bi-${this.name}`;
  }

  render() {
    if (!this.name) return nothing;
    const sizeClass = SIZE_CLASSES[this.size] || "";
    return html`<span
      class="${this._iconClass()}${sizeClass ? ` ${sizeClass}` : ""}"
      aria-hidden="true"
    ></span>`;
  }
}

customElements.define("cts-icon", CtsIcon);
