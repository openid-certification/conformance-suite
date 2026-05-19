/**
 * Sticky bottom action bar. Wraps its children in a viewport-pinned bar with
 * an inner content wrapper aligned to a caller-supplied content width preset,
 * so primary page actions stay visible while the user scrolls a long form.
 *
 * Vanilla HTMLElement following the cts-card / cts-modal / cts-alert
 * convention (light DOM, scoped style injected once into <head>, children
 * captured at first connect and moved into the inner wrapper).
 *
 * The bar publishes its measured height as `--cts-action-bar-height` on the
 * document root after first paint and re-measures on ResizeObserver
 * callbacks. Callers can opt in to spacing with a single CSS line, e.g.
 *
 *     .schedule-test-page {
 *       padding-bottom: calc(var(--cts-action-bar-height, 80px) + var(--space-5));
 *     }
 *
 * @property {string} align-to - Content-width preset for the inner wrapper.
 *   See ALIGN_PRESETS for the supported names. Unknown values warn once and
 *   fall back to the default preset.
 * @property {string} position - Layout mode. `"bottom"` (default) renders
 *   the bar as `position: fixed; bottom: 0; left: 0; right: 0`. `"static"`
 *   renders the bar inline at its source position — useful for Storybook
 *   and dev previews.
 * @property {string} aria-label - Accessible label for the bar's region.
 *   Defaults to `"Actions"` when absent.
 */

/**
 * Inner-wrapper alignment presets. Each preset declares the max-width and
 * inline padding the wrapper should use to land on the same content column
 * as the page below it. Add a new entry when a third call site needs the
 * affordance — do not concatenate class names from the attribute value.
 * @type {{[key: string]: {maxWidth: string, paddingInline: string}}}
 */
const ALIGN_PRESETS = {
  "schedule-test-page": {
    maxWidth: "1100px",
    paddingInline: "var(--space-5)",
  },
  "full-bleed": {
    maxWidth: "100%",
    paddingInline: "var(--space-4)",
  },
};

const DEFAULT_PRESET = "schedule-test-page";

const STYLE_ID = "cts-action-bar-styles";

const STYLE_TEXT = `
.oidf-action-bar {
  background: var(--bg);
  border-top: 1px solid var(--border);
  box-shadow: var(--shadow-3, 0 -4px 16px rgba(26, 22, 17, 0.08));
  z-index: 2;
}
.oidf-action-bar[data-position="bottom"] {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
}
.oidf-action-bar[data-position="static"] {
  position: static;
}
.oidf-action-bar__inner {
  margin: 0 auto;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  align-items: center;
  padding-block: var(--space-3);
}
@media (prefers-reduced-motion: no-preference) {
  .oidf-action-bar[data-position="bottom"] {
    animation: cts-action-bar-slide-in 220ms cubic-bezier(0.2, 0, 0, 1);
  }
  @keyframes cts-action-bar-slide-in {
    from { transform: translateY(100%); opacity: 0; }
    to   { transform: translateY(0);    opacity: 1; }
  }
}
`;

const SUPPORTED_POSITIONS = new Set(["bottom", "static"]);

/**
 * Tracks whether an unknown `align-to` preset has already warned in this
 * page load, so noisy markup does not flood the console.
 * @type {Set<string>}
 */
const WARNED_PRESETS = new Set();

/**
 * Inject the cts-action-bar scoped stylesheet into `<head>` exactly once.
 * Idempotent: subsequent calls find the existing `<style>` tag by id and bail.
 * @returns {void}
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

class CtsActionBar extends HTMLElement {
  connectedCallback() {
    if (this._initialized) return;
    this._initialized = true;
    injectStyles();

    const rawPosition = this.getAttribute("position");
    const position = rawPosition && SUPPORTED_POSITIONS.has(rawPosition) ? rawPosition : "bottom";

    const rawPreset = this.getAttribute("align-to");
    let preset = DEFAULT_PRESET;
    if (rawPreset) {
      if (rawPreset in ALIGN_PRESETS) {
        preset = rawPreset;
      } else if (!WARNED_PRESETS.has(rawPreset)) {
        WARNED_PRESETS.add(rawPreset);
        console.warn(
          `cts-action-bar: unknown align-to preset "${rawPreset}", falling back to "${DEFAULT_PRESET}".`,
        );
      }
    }
    const { maxWidth, paddingInline } = ALIGN_PRESETS[preset];

    const ariaLabel = this.getAttribute("aria-label") || "Actions";

    const children = Array.from(this.childNodes);

    const bar = document.createElement("div");
    bar.className = "oidf-action-bar";
    bar.setAttribute("data-position", position);
    bar.setAttribute("role", "region");
    bar.setAttribute("aria-label", ariaLabel);

    const inner = document.createElement("div");
    inner.className = "oidf-action-bar__inner";
    inner.style.maxWidth = maxWidth;
    inner.style.paddingInline = paddingInline;
    for (const child of children) {
      inner.appendChild(child);
    }
    bar.appendChild(inner);

    this.appendChild(bar);
    this._barEl = bar;

    if (position === "bottom") {
      this._startHeightTracking(bar);
    }
  }

  disconnectedCallback() {
    if (this._resizeObserver) {
      this._resizeObserver.disconnect();
      this._resizeObserver = null;
    }
    // Only clear the custom property if this instance was the publisher.
    if (this._publishedHeight) {
      document.documentElement.style.removeProperty("--cts-action-bar-height");
      this._publishedHeight = false;
    }
  }

  /**
   * Publish the bar's measured height as `--cts-action-bar-height` on the
   * document root, and keep it in sync via ResizeObserver. Bottom-positioned
   * bars only; static bars participate in normal flow and need no spacer.
   * @param {HTMLElement} bar - The inner `.oidf-action-bar` element to measure
   * @returns {void}
   */
  _startHeightTracking(bar) {
    const publish = () => {
      const rect = bar.getBoundingClientRect();
      // Hosts can be inserted while their parent / themselves are display:none
      // (e.g., schedule-test.html toggles #launchButtons.style.display via
      // updateConfigFieldVisibility). A 0px publish would clobber the CSS
      // fallback (`var(--cts-action-bar-height, 80px)`) and collapse the page's
      // bottom-padding reservation until the bar becomes visible. Skip
      // publishing in that case and let the fallback stand; ResizeObserver
      // fires once the host gains a rendered box and the next publish lands.
      if (rect.height <= 0) return;
      const px = `${Math.ceil(rect.height)}px`;
      document.documentElement.style.setProperty("--cts-action-bar-height", px);
      this._publishedHeight = true;
    };
    publish();
    if (typeof ResizeObserver !== "undefined") {
      this._resizeObserver = new ResizeObserver(publish);
      this._resizeObserver.observe(bar);
    }
  }
}

customElements.define("cts-action-bar", CtsActionBar);

export {};
