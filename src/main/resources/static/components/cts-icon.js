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
 * the OIDF design-system migration. Existing call sites that still pass
 * sm/md/lg keep working; new call sites should use 16/20/24.
 * @type {{[key: string]: string}}
 */
const LEGACY_SIZE_ALIASES = {
  sm: "16",
  md: "20",
  lg: "24",
};

/**
 * Base path for the per-icon coolicons SVG files. Each icon is its own
 * tiny SVG file at `${ICONS_BASE}/${name}.svg` with `id="i"` on the root
 * element, so `<use href="${ICONS_BASE}/${name}.svg#i">` references it
 * reliably across browsers.
 *
 * Per-icon files (rather than a sprite) keep the network footprint
 * proportional to the icons actually used — HTTP/2 multiplexes the
 * small parallel requests, the browser's HTTP cache dedupes repeated
 * references, and shipping unused symbols is impossible by construction.
 *
 * The public `name` is the filename, full stop — no manifest, no
 * symbol-id translation, no runtime fetch-and-inline.
 * @type {string}
 */
const ICONS_BASE = "/vendor/coolicons/icons";

/**
 * Coolicons glyph wrapper. Renders an inline `<svg><use>` referencing a
 * per-icon SVG file from the vendored coolicons set, sized by an OIDF
 * spacing token and coloured via `currentColor`. The icon inherits its
 * colour from the surrounding text and its dimensions from the
 * design-system spacing scale.
 *
 * @property {string} name - Coolicons icon name in lowercase kebab-case
 *   (e.g. `external-link`, `info`, `chevron-right`). Must match the
 *   filename of an SVG in
 *   `src/main/resources/static/vendor/coolicons/icons/`. Empty string
 *   renders nothing. A name with no matching file produces a silent
 *   fetch-404 + empty render — verify the name exists in the icons
 *   directory or the AllIcons Storybook story before wiring up new
 *   call sites.
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
    // not bleed onto unrelated `<svg>` elements in the consumer's DOM.
    // Sizes 16/20/24 are driven by --space-4/5/6 (the design-system intent
    // — see colors_and_type.css). Stroke colour comes from the per-icon
    // file's baked-in `stroke="currentColor"`, so the icon inherits the
    // surrounding text colour without needing any host-level rule.
    return html`<style>
        svg[data-cts-icon-size] {
          display: inline-block;
          line-height: 1;
          flex-shrink: 0;
        }
        svg[data-cts-icon-size="16"] {
          width: var(--space-4);
          height: var(--space-4);
        }
        svg[data-cts-icon-size="20"] {
          width: var(--space-5);
          height: var(--space-5);
        }
        svg[data-cts-icon-size="24"] {
          width: var(--space-6);
          height: var(--space-6);
        }
      </style>
      <svg
        viewBox="0 0 24 24"
        data-cts-icon-size="${size}"
        aria-hidden="true"
      ><use href="${ICONS_BASE}/${this.name}.svg#i"></use></svg>`;
  }
}

customElements.define("cts-icon", CtsIcon);
