import { LitElement, html, nothing } from "lit";

/**
 * Public size keys for cts-icon. The three documented sizes (16, 20, 24)
 * align 1:1 with the OIDF spacing tokens --space-4/5/6 (16px/20px/24px),
 * so every cts-icon scales automatically with future token revisions.
 * @type {readonly string[]}
 */
const VALID_SIZES = ["16", "20", "24"];

/**
 * Backwards-compatibility map for the legacy sm/md/lg size API used before
 * U6 (OIDF design-system migration). Existing call sites that still pass
 * sm/md/lg keep working; new call sites should use 16/20/24.
 * @type {{[key: string]: string}}
 */
const LEGACY_SIZE_ALIASES = {
  sm: "16",
  md: "20",
  lg: "24",
};

/**
 * Bootstrap Icons glyph wrapper. Renders a `<span class="bi bi-{name}">`
 * sized by an OIDF spacing token and coloured via `currentColor`, so the
 * icon inherits its colour from the surrounding text and its dimensions
 * from the design-system spacing scale.
 * @property {string} name - Bootstrap Icons name without the `bi-` prefix
 *   (e.g. `camera-fill`). Empty string renders nothing.
 * @property {string} size - One of: "16", "20" (default), "24". The legacy
 *   aliases sm/md/lg are accepted and mapped to 16/20/24 respectively.
 */
class CtsIcon extends LitElement {
  static properties = {
    name: { type: String },
    size: { type: String },
  };

  constructor() {
    super();
    this.name = "";
    this.size = "20";
  }

  createRenderRoot() {
    return this;
  }

  // Icon names come from the Bootstrap Icons set (2000+ icons).
  // Constructed from the name prop, not a finite variant set.
  _iconClass() {
    return `bi bi-${this.name}`;
  }

  /**
   * Resolve the public `size` value to one of the three documented tokens.
   * Unknown values fall back to "20" so a misspelled size never produces
   * an invisible icon.
   * @returns {string} One of "16", "20", "24".
   */
  _resolvedSize() {
    const aliased = LEGACY_SIZE_ALIASES[this.size] ?? this.size;
    return VALID_SIZES.includes(aliased) ? aliased : "20";
  }

  render() {
    if (!this.name) return nothing;
    const size = this._resolvedSize();
    // Light-DOM scoped style: the [data-cts-icon-size] attribute selector
    // limits the rules to glyphs rendered by this component, so they do
    // not bleed onto unrelated `.bi` icons in the consumer's DOM. Sizes
    // 16/20/24 are driven by --space-4/5/6 (the design-system intent —
    // see colors_and_type.css). Colour is `currentColor` so the icon
    // inherits the surrounding text colour.
    return html`<style>
        span.bi[data-cts-icon-size] {
          color: currentColor;
          display: inline-block;
          line-height: 1;
        }
        span.bi[data-cts-icon-size="16"] {
          font-size: var(--space-4);
          width: var(--space-4);
          height: var(--space-4);
        }
        span.bi[data-cts-icon-size="20"] {
          font-size: var(--space-5);
          width: var(--space-5);
          height: var(--space-5);
        }
        span.bi[data-cts-icon-size="24"] {
          font-size: var(--space-6);
          width: var(--space-6);
          height: var(--space-6);
        }
      </style>
      <span class="${this._iconClass()}" data-cts-icon-size="${size}" aria-hidden="true"></span>`;
  }
}

customElements.define("cts-icon", CtsIcon);
