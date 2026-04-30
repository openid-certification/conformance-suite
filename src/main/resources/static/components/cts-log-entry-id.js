import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";
import "./cts-tooltip.js";
import { flashCopyConfirmed } from "../js/cts-copy-flash.js";

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
 * Visual treatment is delegated to `<cts-badge variant="secondary"
 * clickable>` — the same chip-scale used elsewhere for code-like
 * identifiers. The on-success affordance is the shared icon-flash
 * (copy → check → copy) handled by `flashCopyConfirmed`; this is the
 * canonical local feedback for any copy button in the suite, so keep
 * adding new ones uses the same pattern.
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
  };

  constructor() {
    super();
    this.referenceId = "";
    this.testId = "";
  }

  createRenderRoot() {
    return this;
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

  /**
   * Resolve the cts-badge host so the icon-flash can act on it. Lit's
   * render is already complete by the time event handlers fire, so a
   * straight querySelector is enough; no ref needed.
   * @returns {Element | null}
   */
  _badge() {
    return this.querySelector("cts-badge");
  }

  async _handleClick() {
    if (!this.referenceId) return;
    const url = this._buildDeepUrl();
    const ok = await copyText(url);
    if (!ok) return;
    flashCopyConfirmed(this._badge());
    this._emitCopied("url", url);
  }

  /** @param {Event} event */
  async _handleContextMenu(event) {
    if (!this.referenceId) return;
    event.preventDefault();
    const ok = await copyText(this.referenceId);
    if (!ok) return;
    flashCopyConfirmed(this._badge());
    this._emitCopied("plain", this.referenceId);
  }

  render() {
    if (!this.referenceId) return nothing;
    const ariaLabel = `Log entry ${this.referenceId} — click to copy link, right-click to copy plain reference`;
    const tooltipContent = `Click to copy link · Right-click to copy ${this.referenceId}`;
    return html`<cts-tooltip content=${tooltipContent} placement="top"
      ><cts-badge
        variant="secondary"
        clickable
        icon="copy"
        aria-label=${ariaLabel}
        data-testid="log-entry-id-chip"
        @cts-badge-click=${this._handleClick}
        @contextmenu=${this._handleContextMenu}
        >${this.referenceId}</cts-badge
      ></cts-tooltip>`;
  }
}

customElements.define("cts-log-entry-id", CtsLogEntryId);

export {};
