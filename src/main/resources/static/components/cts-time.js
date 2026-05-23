import { LitElement, html, nothing } from "lit";
import {
  formatRelative,
  formatAbsolute,
  formatTimeOfDay,
  formatCompact,
  toMillis,
} from "../lib/time-format.js";

/**
 * Visible-text strategy for {@link CtsTime}. The `title` attribute always
 * carries the full absolute form regardless of which mode is selected.
 * @typedef {"auto" | "relative" | "absolute" | "compact" | "time-of-day"} CtsTimeMode
 */

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
 * @property {string | number} value - The timestamp to render. ISO 8601
 *   string, epoch-ms number, or epoch-ms string (log-entry payloads carry
 *   numeric timestamps that Lit stringifies through the attribute). Anything
 *   `lib/time-format` can parse is accepted. When missing or unparseable, the
 *   component renders nothing.
 * @property {CtsTimeMode} mode - Visible-text strategy. One of:
 *   `auto` (default — relative for ≤30 days, absolute beyond),
 *   `relative` (relative label; like `auto`, falls back to the absolute
 *   string beyond 30 days),
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
   * @param {Date} date - The already-parsed timestamp.
   * @returns {string} The text to render inside the `<time>` element.
   */
  _displayText(date) {
    switch (this.mode) {
      case "relative":
        return formatRelative(date);
      case "absolute":
        return formatAbsolute(date);
      case "compact":
        return formatCompact(date);
      case "time-of-day":
        return formatTimeOfDay(date);
      case "auto":
      default:
        return formatRelative(date);
    }
  }

  render() {
    // Parse the value once, then derive the ISO datetime, the absolute-form
    // title, and the visible text from the same Date — avoids re-parsing the
    // string on every formatter call (matters at hundreds of rows per page).
    const ms = toMillis(this.value);
    if (ms === null) return nothing;
    const date = new Date(ms);
    const iso = date.toISOString();
    const absolute = formatAbsolute(date);
    const display = this._displayText(date);
    return html`<time datetime=${iso} title=${absolute}>${display}</time>`;
  }
}

if (!customElements.get("cts-time")) {
  customElements.define("cts-time", CtsTime);
}

export {};
