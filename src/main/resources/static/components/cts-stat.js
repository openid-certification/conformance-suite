import { LitElement, html, nothing } from "lit";

/**
 * Maps tone name → CSS custom property used as the value's `color`.
 *
 * The lookup table avoids dynamic class concatenation (AGENTS.md §7) and
 * lets a misspelled tone fall back safely to the default ink colour rather
 * than producing an invalid CSS variable reference.
 *
 * @type {Object.<string, string>}
 */
const TONE_COLOR = {
  pass: "var(--status-pass)",
  fail: "var(--rust-400)",
  default: "var(--fg)",
};

/**
 * Delta-line colour per tone. Default leaves the delta at the muted
 * `--fg-soft` colour from `.t-meta`; pass / fail colour it to mirror the
 * value's tone so the reader can scan the trend at a glance.
 *
 * @type {Object.<string, string>}
 */
const DELTA_COLOR = {
  pass: "var(--status-pass)",
  fail: "var(--rust-400)",
  default: "var(--fg-soft)",
};

const STYLE_ID = "cts-stat-styles";

// Scoped CSS for cts-stat tiles. Mirrors the dashboard stat-tile pattern
// from project/preview/spacing-shadows.html: an overline label above a
// display-font value, with an optional meta-sized delta line beneath. All
// dimensions and colours come from oidf-tokens.css.
const STYLE_TEXT = `
.oidf-stat {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}
.oidf-stat-value {
  font-family: var(--font-display);
  font-weight: var(--fw-black);
  font-size: var(--fs-32);
  line-height: var(--lh-tight);
  color: var(--fg);
}
.oidf-stat-delta {
  color: var(--fg-soft);
}
`;

function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Resolve a `tone` value to one of the keys in the lookup tables. Unknown
 * or empty tones collapse to `default` so an unexpected attribute value
 * never produces an invalid CSS variable reference.
 *
 * @param {string} tone - Raw tone attribute value.
 * @returns {string} One of "pass", "fail", "default".
 */
function resolveTone(tone) {
  return tone in TONE_COLOR && tone !== "default" ? tone : "default";
}

/**
 * Dashboard stat tile: an overline label, a display-font value, and an
 * optional delta line. The `tone` attribute switches the value (and delta)
 * colour between calm green (`pass`), rust (`fail`), and the default ink.
 *
 * @property {string} label - Overline text shown above the value.
 * @property {string} value - The primary number/string displayed in the
 *   display font.
 * @property {string} delta - Optional secondary text shown below the
 *   value (e.g. "+12% vs last week"). When empty, the delta line is
 *   omitted entirely.
 * @property {string} tone - One of: "pass", "fail", or unset (default).
 *   `pass` colours the value `--status-pass`; `fail` colours it
 *   `--rust-400`; otherwise the value uses `--fg`.
 */
class CtsStat extends LitElement {
  static properties = {
    label: { type: String },
    value: { type: String },
    delta: { type: String },
    tone: { type: String },
  };

  constructor() {
    super();
    this.label = "";
    this.value = "";
    this.delta = "";
    this.tone = "";
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  createRenderRoot() {
    return this;
  }

  render() {
    const toneKey = resolveTone(this.tone);
    const valueColor = TONE_COLOR[toneKey];
    const deltaColor = DELTA_COLOR[toneKey];
    return html`<div class="oidf-stat">
      <div class="t-overline oidf-stat-label">${this.label}</div>
      <div class="oidf-stat-value" style="color: ${valueColor};">${this.value}</div>
      ${this.delta
        ? html`<div class="t-meta oidf-stat-delta" style="color: ${deltaColor};">
            ${this.delta}
          </div>`
        : nothing}
    </div>`;
  }
}

customElements.define("cts-stat", CtsStat);

export {};
