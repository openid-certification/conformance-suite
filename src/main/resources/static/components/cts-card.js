/**
 * Token-styled card container. Wraps its children in an `.oidf-card` /
 * `.oidf-card-body` structure, with an optional `.oidf-card-header` driven
 * by the `header` attribute and an optional 3px top brand bar driven by the
 * `tone` attribute.
 *
 * Vanilla HTMLElement — has no `static properties`; attributes are read
 * directly in `connectedCallback`. The slot-children pattern (children
 * captured once at first connect, then moved into `.oidf-card-body`) is
 * preserved; subsequent attribute changes do not re-render.
 *
 * Light DOM. Scoped CSS lives in a single `<style>` element injected into
 * `<head>` on first connect (gated by a module-level flag) so the rules
 * appear once regardless of how many `cts-card` instances are on the page.
 *
 * @property {string} header - Optional header text (read from the `header`
 *   attribute). When omitted, no header element is rendered.
 * @property {string} tone - Optional brand-bar tone. One of `orange`,
 *   `rust`, `sand`. When omitted (or unknown), no top brand bar is rendered.
 */

/** @type {Object.<string, string>} Maps tone name → CSS variable for the brand-bar background. */
const TONE_VARS = {
  orange: "var(--orange-400)",
  rust: "var(--rust-400)",
  sand: "var(--sand-300)",
};

const STYLE_ID = "cts-card-styles";

const STYLE_TEXT = `
.oidf-card {
  position: relative;
  background: var(--bg-elev);
  border: 1px solid var(--border);
  border-radius: var(--radius-3);
  box-shadow: var(--shadow-1);
  overflow: hidden;
}
.oidf-card-bar {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  height: 3px;
}
.oidf-card-header {
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border);
  font-weight: var(--fw-bold);
  color: var(--fg);
}
.oidf-card-body {
  padding: var(--space-5);
}
`;

function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

class CtsCard extends HTMLElement {
  connectedCallback() {
    injectStyles();

    const header = this.getAttribute("header");
    const tone = this.getAttribute("tone");
    const barColor = tone ? TONE_VARS[tone] : undefined;

    const children = Array.from(this.childNodes);

    const card = document.createElement("div");
    card.className = "oidf-card";

    if (barColor) {
      const bar = document.createElement("div");
      bar.className = "oidf-card-bar";
      // Tone-to-color mapping is driven by the TONE_VARS lookup table (no
      // dynamic class concatenation, per components/AGENTS.md §7). The
      // resulting class is constant; the per-tone color is applied via
      // an inline style so unknown tones simply render no bar.
      bar.style.background = barColor;
      card.appendChild(bar);
    }

    if (header) {
      const cardHeader = document.createElement("div");
      cardHeader.className = "oidf-card-header";
      cardHeader.textContent = header;
      card.appendChild(cardHeader);
    }

    const cardBody = document.createElement("div");
    cardBody.className = "oidf-card-body";
    for (const child of children) {
      cardBody.appendChild(child);
    }
    card.appendChild(cardBody);

    this.appendChild(card);
  }
}

customElements.define("cts-card", CtsCard);

export {};
