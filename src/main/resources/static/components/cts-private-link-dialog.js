import { LitElement, html, nothing, css } from "lit";
import { ref, createRef } from "lit/directives/ref.js";
import "./cts-modal.js";
import "./cts-button.js";
import "./cts-alert.js";
import "./cts-icon.js";
import { flashCopyConfirmed } from "../js/cts-copy-flash.js";

const STYLE_ID = "cts-private-link-dialog-styles";

const STYLE_TEXT = css`
  cts-private-link-dialog .plinkLabel {
    display: block;
    font-size: var(--fs-13);
    color: var(--fg);
    margin-bottom: var(--space-1);
  }
  cts-private-link-dialog .plinkDays {
    display: block;
    width: 6em;
    height: var(--control-height);
    box-sizing: border-box;
    margin-top: var(--space-1);
    padding: 0 var(--space-2);
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    color: var(--fg);
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-1);
  }
  cts-private-link-dialog .plinkGenerate {
    margin-top: var(--space-3);
  }
  cts-private-link-dialog .plinkError {
    margin-top: var(--space-3);
  }
  cts-private-link-dialog .plinkBusy {
    margin: var(--space-3) 0 0;
    font-size: var(--fs-13);
    color: var(--fg-soft);
  }
  cts-private-link-dialog .plinkResult {
    margin-top: var(--space-3);
    padding: var(--space-3);
    background: var(--ink-50);
    border-radius: var(--radius-2);
  }
  cts-private-link-dialog .plinkMessage {
    margin: 0 0 var(--space-2);
    font-size: var(--fs-12);
    color: var(--fg-soft);
  }
  cts-private-link-dialog .plinkUrl {
    display: block;
    font-family: var(--font-mono);
    font-size: var(--fs-13);
    color: var(--ink-900);
    word-break: break-all;
  }
  cts-private-link-dialog .plinkCopy {
    margin-top: var(--space-2);
  }
  cts-private-link-dialog .plinkStatus {
    margin: var(--space-2) 0 0;
    min-height: 1.25em;
    font-size: var(--fs-12);
    color: var(--fg-soft);
  }
`;

/** Inject the component's scoped light-DOM styles once per document. */
function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

// Expiry bounds for the generated link. These are a UI convenience only:
// the server (AssetSharing.generateSharingToken) enforces exp >= 1 with no
// upper bound, so MAX_DAYS is not a security control. log-detail (3650) and
// plan-detail (formerly 1000) used to diverge; unified here on 3650.
const MIN_DAYS = 1;
const MAX_DAYS = 3650;
const DEFAULT_DAYS = 30;

/**
 * Shared "Private link" dialog used by both log-detail and plan-detail so the
 * two pages render an identical flow. One dialog: pick an expiry, click
 * Generate, the link is shown and auto-copied, with an explicit Copy button
 * as a fallback.
 *
 * **Safari clipboard contract.** The whole point of consolidating here is to
 * get the clipboard behaviour right in one place:
 *   - Generate is wired with a native `@click` (the real click bubbling from
 *     cts-button's inner `<button>`), NOT the synthetic `@cts-click` — Safari
 *     only honours `navigator.clipboard.*` from a genuine user gesture.
 *   - `navigator.clipboard.write()` is called synchronously, first thing in
 *     the handler, with a `ClipboardItem` whose blob resolves from the
 *     in-flight fetch — so the gesture is still live when `.write()` runs and
 *     the data (only known after the POST) is delivered later. There is no
 *     `showModal()` of a separate busy dialog between the gesture and the
 *     write; the busy/result states render inline.
 *
 * Custom element: `<cts-private-link-dialog>`.
 * @property {string} shareUrl - POST endpoint that mints the link (without the
 *   `?exp=` query); e.g. `/api/info/{testId}/share` or `/api/plan/{id}/share`.
 *   The consumer sets this before calling `show()`.
 */
export class CtsPrivateLinkDialog extends LitElement {
  static properties = {
    shareUrl: { attribute: "share-url" },
    _days: { state: true },
    _link: { state: true },
    _message: { state: true },
    _copyStatus: { state: true },
    _busy: { state: true },
    _error: { state: true },
  };

  constructor() {
    super();
    this.shareUrl = "";
    this._days = DEFAULT_DAYS;
    this._link = "";
    this._message = "";
    this._copyStatus = "";
    this._busy = false;
    this._error = "";
    this._modalRef = createRef();
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  /** Reset to a clean state and open the dialog. */
  show() {
    this._link = "";
    this._message = "";
    this._copyStatus = "";
    this._error = "";
    this._busy = false;
    this._days = DEFAULT_DAYS;
    this.updateComplete.then(() => {
      const modal = /** @type {{ show?: () => void } | undefined} */ (this._modalRef.value);
      modal?.show?.();
    });
  }

  _daysValid() {
    return Number.isFinite(this._days) && this._days >= MIN_DAYS && this._days <= MAX_DAYS;
  }

  _onDaysInput(e) {
    this._days = Number(e.target.value);
  }

  /**
   * Native-click handler for Generate. MUST stay synchronous up to the
   * `navigator.clipboard.write()` call so Safari keeps the user gesture.
   */
  _handleGenerate() {
    if (this._busy || !this._daysValid() || !this.shareUrl) return;

    const url = `${this.shareUrl}?exp=${encodeURIComponent(this._days)}`;
    const fetchPromise = fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
    }).then((response) => {
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return response.json();
    });

    // Start the clipboard write synchronously, before any state change, so the
    // user gesture is still active. The blob resolves from the in-flight fetch.
    let clipboardWritePromise = null;
    try {
      clipboardWritePromise = navigator.clipboard.write([
        new ClipboardItem({
          "text/plain": fetchPromise.then(
            (data) => new Blob([data.link || ""], { type: "text/plain" }),
          ),
        }),
      ]);
    } catch (e) {
      console.warn("[cts-private-link-dialog] async clipboard write unavailable:", e);
    }

    this._busy = true;
    this._error = "";
    this._link = "";
    this._copyStatus = "";

    fetchPromise
      .then((data) => {
        this._busy = false;
        this._link = data.link || "";
        this._message = data.message || "";
        // Reflect the auto-copy outcome honestly: only claim "copied" once the
        // write resolves; otherwise point the user at the Copy button.
        if (clipboardWritePromise) {
          clipboardWritePromise.then(
            () => {
              this._copyStatus = "Copied to clipboard.";
              this._flashCopyButton();
            },
            () => {
              this._copyStatus = "Press “Copy to clipboard” to copy the link.";
            },
          );
        } else {
          this._copyStatus = "Press “Copy to clipboard” to copy the link.";
        }
      })
      .catch((err) => {
        this._busy = false;
        this._error = `Failed to create private link: ${err.message}`;
      });
  }

  /**
   * Manual copy — a direct user gesture, so plain writeText is honoured.
   * @param {Event} e - The native click event from the Copy button.
   */
  async _handleCopy(e) {
    const trigger = /** @type {Element | null} */ (e.currentTarget);
    if (!this._link) return;
    if (!navigator.clipboard) {
      this._copyStatus = "Clipboard not available — select the link and copy it manually.";
      return;
    }
    try {
      await navigator.clipboard.writeText(this._link);
    } catch (err) {
      console.warn("[cts-private-link-dialog] clipboard.writeText failed:", err);
      this._copyStatus = "Copy failed — select the link and copy it manually.";
      return;
    }
    this._copyStatus = "Copied to clipboard.";
    flashCopyConfirmed(trigger);
  }

  _flashCopyButton() {
    this.updateComplete.then(() => {
      const btn = this.querySelector(".plinkCopyBtn");
      if (btn) flashCopyConfirmed(btn);
    });
  }

  render() {
    return html`
      <cts-modal ${ref(this._modalRef)} heading="Private link" data-testid="private-link-dialog">
        <label class="plinkLabel">
          Valid for (days, ${MIN_DAYS}–${MAX_DAYS})
          <input
            class="plinkDays"
            type="number"
            min="${MIN_DAYS}"
            max="${MAX_DAYS}"
            .value=${String(this._days)}
            @input=${this._onDaysInput}
          />
        </label>
        <div class="plinkGenerate">
          <cts-button
            class="plinkGenerateBtn"
            variant="primary"
            size="sm"
            label="Generate"
            ?disabled=${this._busy || !this._daysValid()}
            @click=${this._handleGenerate}
          ></cts-button>
        </div>

        ${this._busy ? html`<p class="plinkBusy">Creating private link…</p>` : nothing}
        ${this._error
          ? html`<cts-alert variant="danger" class="plinkError">${this._error}</cts-alert>`
          : nothing}
        ${this._link
          ? html`<div class="plinkResult" data-testid="private-link-result">
              ${this._message ? html`<p class="plinkMessage">${this._message}</p>` : nothing}
              <code class="plinkUrl">${this._link}</code>
              <div class="plinkCopy">
                <cts-button
                  class="plinkCopyBtn"
                  variant="secondary"
                  size="sm"
                  icon="copy"
                  label="Copy to clipboard"
                  @click=${this._handleCopy}
                ></cts-button>
              </div>
              ${this._copyStatus
                ? html`<p
                    class="plinkStatus"
                    aria-live="polite"
                    data-testid="private-link-copy-status"
                  >
                    ${this._copyStatus}
                  </p>`
                : nothing}
            </div>`
          : nothing}
      </cts-modal>
    `;
  }
}

customElements.define("cts-private-link-dialog", CtsPrivateLinkDialog);
