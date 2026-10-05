import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";
import "./cts-tooltip.js";
import { flashCopyConfirmed } from "../js/cts-copy-flash.js";
import { copyText } from "../js/copy-text.js";

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
   * @returns {string} Absolute deep-link URL for this reference, or the bare `referenceId` when `window` is unavailable.
   */
  _buildDeepUrl() {
    if (typeof window === "undefined") return this.referenceId;
    const url = new URL("log-detail.html", window.location.origin);
    if (this.testId) url.searchParams.set("log", this.testId);
    url.hash = this.referenceId;
    return url.toString();
  }

  /**
   * @param {"url" | "plain"} mode - Which copy variant succeeded: deep URL or plain reference.
   * @param {string} value - The text that was actually placed on the clipboard.
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
   * @returns {Element | null} The rendered `<cts-badge>` host, or `null` before first render.
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

  /** @param {Event} event - The `contextmenu` event (right-click / long-press); default is prevented so the plain reference can be copied instead of the browser menu opening. */
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
      ></cts-tooltip
    >`;
  }
}

customElements.define("cts-log-entry-id", CtsLogEntryId);

export {};
