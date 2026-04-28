import { LitElement, html, nothing } from "lit";
import { classMap } from "lit/directives/class-map.js";
import "./cts-icon.js";
import "./cts-tooltip.js";

const STYLE_ID = "cts-log-entry-id-styles";

// Reference-chip styling. Light DOM + scoped CSS injected once on first
// render — same pattern as the rest of the cts-log-* triad. Token-only
// values; no Bootstrap classes are emitted. The `--copied` modifier
// briefly tints the chip on a successful copy so the action reads as
// confirmed even though the clipboard write itself is invisible.
const STYLE_TEXT = `
  cts-log-entry-id {
    display: inline-flex;
    vertical-align: middle;
  }
  cts-log-entry-id .logIdChip {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    background: transparent;
    border: 1px solid var(--border);
    border-radius: var(--radius-pill);
    color: var(--fg-muted);
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    font-weight: var(--fw-medium);
    line-height: 1;
    padding: 1px var(--space-2);
    cursor: pointer;
    user-select: none;
    -webkit-user-select: none;
    transition:
      background-color 120ms ease-in-out,
      color 120ms ease-in-out,
      border-color 120ms ease-in-out;
  }
  cts-log-entry-id .logIdChip:hover {
    background: var(--ink-50);
    color: var(--fg);
  }
  cts-log-entry-id .logIdChip:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-log-entry-id .logIdChip--copied,
  cts-log-entry-id .logIdChip--copied:hover {
    background: var(--status-info-soft, var(--ink-50));
    border-color: var(--status-info, var(--border-strong));
    color: var(--status-info, var(--fg));
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
 * Best-effort fallback when `navigator.clipboard.writeText` rejects (HTTP
 * pages, missing user-gesture context, older browsers). Mirrors the
 * `document.execCommand('copy')` shim widely used pre-Clipboard-API. Returns
 * `true` on apparent success, `false` otherwise — the caller decides how to
 * surface the result.
 * @param {string} value
 * @returns {boolean}
 */
function fallbackCopy(value) {
  if (typeof document === "undefined") return false;
  const ta = document.createElement("textarea");
  ta.value = value;
  ta.setAttribute("readonly", "");
  ta.style.position = "fixed";
  ta.style.opacity = "0";
  ta.style.pointerEvents = "none";
  document.body.appendChild(ta);
  ta.select();
  let ok;
  try {
    // The Clipboard API requires HTTPS + a user gesture; on legacy / HTTP
    // contexts execCommand is the documented fallback. The deprecation
    // warning is acknowledged but the shim has no replacement on those
    // platforms.
    ok = /** @type {any} */ (document).execCommand("copy");
  } catch {
    ok = false;
  }
  document.body.removeChild(ta);
  return Boolean(ok);
}

/**
 * @param {string} value
 * @returns {Promise<boolean>}
 */
async function copyText(value) {
  if (
    typeof navigator !== "undefined" &&
    navigator.clipboard &&
    typeof navigator.clipboard.writeText === "function"
  ) {
    try {
      await navigator.clipboard.writeText(value);
      return true;
    } catch {
      return fallbackCopy(value);
    }
  }
  return fallbackCopy(value);
}

/**
 * Reference chip rendered alongside a log entry. Click / tap copies the
 * full deep URL `?log={testId}#LOG-NNNN` to the clipboard so cross-run
 * citation in Slack / Jira always disambiguates by `testId`. Right-click
 * / long-press copies the plain `LOG-NNNN` for in-document references
 * where the URL would be redundant.
 *
 * Light DOM. Scoped CSS lives in a single `<style>` element injected
 * into `<head>` on first render; all colors / spacing route through OIDF
 * tokens. The chip serves as a focus target (it is a real `<button>`)
 * and emits `cts-reference-copied` so callers (or tests) can observe
 * which mode succeeded without inspecting the clipboard contents.
 *
 * @property {string} referenceId - Full label, e.g. `"LOG-0042"`. Empty
 *   string renders nothing.
 * @property {string} testId - Test instance ID for the deep URL. Required
 *   for click-to-copy-URL to work; missing testId still copies a
 *   plain-reference URL fragment.
 * @fires cts-reference-copied - When the chip's copy succeeds, with
 *   `{ detail: { mode: 'url' | 'plain', referenceId, value } }`. Bubbles
 *   AND is composed so a document-level listener catches it from any
 *   mount position (including future shadow-DOM hosts).
 */
class CtsLogEntryId extends LitElement {
  static properties = {
    referenceId: { type: String, attribute: "reference-id" },
    testId: { type: String, attribute: "test-id" },
    _copied: { state: true },
  };

  constructor() {
    super();
    this.referenceId = "";
    this.testId = "";
    this._copied = false;
    /** @type {ReturnType<typeof setTimeout> | null} */
    this._copiedTimer = null;
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._copiedTimer) {
      clearTimeout(this._copiedTimer);
      this._copiedTimer = null;
    }
  }

  /**
   * Build the canonical deep URL for this reference. Always uses
   * `log-detail.html` (not `-v2.html`) because the page swap during the
   * flag-flip MR is invisible to the URL: shared links cite the canonical
   * filename, and the redirect script handles routing for opt-outs.
   * @returns {string}
   */
  _buildDeepUrl() {
    if (typeof window === "undefined") return this.referenceId;
    const url = new URL("log-detail.html", window.location.origin);
    if (this.testId) url.searchParams.set("log", this.testId);
    url.hash = this.referenceId;
    return url.toString();
  }

  _flashCopied() {
    this._copied = true;
    if (this._copiedTimer) clearTimeout(this._copiedTimer);
    this._copiedTimer = setTimeout(() => {
      this._copied = false;
      this._copiedTimer = null;
    }, 1500);
  }

  /**
   * @param {"url" | "plain"} mode
   * @param {string} value
   */
  _emitCopied(mode, value) {
    this.dispatchEvent(
      new CustomEvent("cts-reference-copied", {
        bubbles: true,
        composed: true,
        detail: { mode, referenceId: this.referenceId, value },
      }),
    );
  }

  async _handleClick() {
    if (!this.referenceId) return;
    const url = this._buildDeepUrl();
    const ok = await copyText(url);
    if (!ok) return;
    this._flashCopied();
    this._emitCopied("url", url);
  }

  /** @param {Event} event */
  async _handleContextMenu(event) {
    if (!this.referenceId) return;
    event.preventDefault();
    const ok = await copyText(this.referenceId);
    if (!ok) return;
    this._flashCopied();
    this._emitCopied("plain", this.referenceId);
  }

  render() {
    if (!this.referenceId) return nothing;
    const ariaLabel = `Log entry ${this.referenceId} — click to copy link, right-click to copy plain reference`;
    const chipClasses = {
      logIdChip: true,
      "logIdChip--copied": this._copied,
    };
    const tooltipContent = `Click to copy link · Right-click to copy ${this.referenceId}`;
    return html`<cts-tooltip content=${tooltipContent} placement="top"
      ><button
        type="button"
        class=${classMap(chipClasses)}
        aria-label=${ariaLabel}
        data-testid="log-entry-id-chip"
        @click=${this._handleClick}
        @contextmenu=${this._handleContextMenu}
      >
        <cts-icon name="link" size="16" aria-hidden="true"></cts-icon>
        <span>${this.referenceId}</span>
      </button></cts-tooltip>`;
  }
}

customElements.define("cts-log-entry-id", CtsLogEntryId);

export {};
