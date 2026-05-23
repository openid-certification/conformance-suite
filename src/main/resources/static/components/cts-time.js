import { LitElement, html, nothing } from "lit";
import {
  formatRelative,
  formatAbsolute,
  formatTimeOfDay,
  formatCompact,
  toIso,
} from "../lib/time-format.js";

const STYLE_ID = "cts-time-styles";

// `display: contents` makes the custom-element box vanish from layout — the
// inner <time> becomes the effective child of whatever flex/grid/inline
// context the host sits in, so dropping <cts-time> next to text or inside a
// table cell never adds an inline-block gap or shifts baselines. Same trick
// cts-tooltip uses. The inner <time> stays a real DOM node, so it keeps its
// place in the accessibility tree.
const STYLE_TEXT = `
  cts-time {
    display: contents;
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Layout-neutral timestamp primitive. Renders a native
 * `<time datetime="…">` element whose visible text is chosen by `mode`,
 * with the full absolute date/time always exposed via the `title` attribute
 * so hovering (or long-pressing on touch) reveals the unambiguous form.
 *
 * The host element is `display: contents`, so it never affects layout — it
 * can be dropped inline next to text, inside a table cell, or in a flex row
 * without adding a box. All formatting routes through `lib/time-format.js`,
 * the single source of truth shared across every timestamp surface.
 *
 * Read-only by design: this is a label, not a control. It carries no focus
 * ring, no click affordance, and emits no events.
 *
 * @example
 *   <cts-time value="2026-05-22T09:42:13Z"></cts-time>
 *   <cts-time value=${iso} mode="time-of-day"></cts-time>
 *
 * @property {string} value - The timestamp to render. ISO 8601 string
 *   (also accepts anything `lib/time-format` can parse). When missing or
 *   unparseable, the component renders nothing.
 * @property {string} mode - Visible-text strategy. One of:
 *   `auto` (default — relative for ≤30 days, absolute beyond),
 *   `relative` (always relative, e.g. "5 minutes ago"),
 *   `absolute` (full locale date/time),
 *   `compact` (medium date + short time, e.g. "May 22, 2026, 9:42 AM"),
 *   `time-of-day` (clock time only, e.g. "9:42:13 AM"). The `title`
 *   attribute is always the full absolute form regardless of mode.
 */
class CtsTime extends LitElement {
  static properties = {
    value: { type: String },
    mode: { type: String },
  };

  constructor() {
    super();
    /** @type {string} */
    this.value = "";
    /** @type {string} */
    this.mode = "auto";
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  /**
   * Resolve the visible label for the current `mode`.
   * @returns {string} The text to render inside the `<time>` element.
   */
  _displayText() {
    switch (this.mode) {
      case "relative":
        return formatRelative(this.value);
      case "absolute":
        return formatAbsolute(this.value);
      case "compact":
        return formatCompact(this.value);
      case "time-of-day":
        return formatTimeOfDay(this.value);
      case "auto":
      default:
        return formatRelative(this.value);
    }
  }

  render() {
    const iso = toIso(this.value);
    if (!iso) return nothing;
    const absolute = formatAbsolute(this.value);
    const display = this._displayText();
    return html`<time datetime=${iso} title=${absolute}>${display}</time>`;
  }
}

if (!customElements.get("cts-time")) {
  customElements.define("cts-time", CtsTime);
}

export {};
