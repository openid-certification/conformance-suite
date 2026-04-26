import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-link-button.js";

/**
 * Maps a backend test status to a canonical cts-badge variant. RUNNING
 * triggers the spinning circular SVG; WAITING uses the warn palette to
 * signal that user action is required; INTERRUPTED maps to `fail` because
 * an interrupted run did not complete successfully.
 * @type {Object.<string, string>}
 */
const STATUS_BADGE_VARIANTS = {
  RUNNING: "running",
  WAITING: "warn",
  INTERRUPTED: "fail",
};

const STYLE_ID = "cts-running-test-card-styles";

// Scoped CSS for the running-test card. Mirrors
// project/preview/components-progress.html in the OIDF design archive: an
// 8px progress track on --ink-100 with a solid --orange-400 fill (no
// striped/pulsing animation — the design rule for active runs is "steady
// continuous tick, no pulse"). Card frame mirrors cts-card so a running-
// test card stacks visually with surrounding plan/log cards.
const STYLE_TEXT = `
  cts-running-test-card {
    display: block;
  }
  .cts-rtc-card {
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: var(--space-5);
    align-items: start;
    padding: var(--space-4) var(--space-5);
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    box-shadow: var(--shadow-1);
    font-family: var(--font-sans);
    color: var(--fg);
  }
  .cts-rtc-status {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
    min-width: 90px;
  }
  .cts-rtc-info {
    min-width: 0;
  }
  .cts-rtc-row {
    display: grid;
    grid-template-columns: 110px 1fr;
    gap: var(--space-3);
    font-size: var(--fs-13);
    line-height: var(--lh-snug);
    margin-bottom: var(--space-1);
  }
  .cts-rtc-label {
    color: var(--fg-soft);
    font-weight: var(--fw-bold);
  }
  .cts-rtc-value {
    color: var(--fg);
    overflow-wrap: anywhere;
  }
  .cts-rtc-progress {
    margin-top: var(--space-3);
  }
  .cts-rtc-progress-track {
    height: 8px;
    background: var(--ink-100);
    border-radius: var(--radius-pill);
    overflow: hidden;
  }
  .cts-rtc-progress-fill {
    height: 100%;
    background: var(--orange-400);
    border-radius: var(--radius-pill);
    transition: width var(--dur-3) var(--ease-standard);
  }
  .cts-rtc-actions {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    min-width: 180px;
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
 * Card summarising a currently running / waiting / interrupted test instance
 * with quick actions for downloading the log and viewing details. Replaces
 * the Wave-1 Bootstrap layout with a scoped OIDF grid: status column on the
 * left (cts-badge + optional progress bar), test metadata in the centre,
 * and a stacked control button group on the right.
 *
 * The progress bar is driven by the numeric `progress` prop (0-100, clamped).
 * It uses an `--ink-100` track with an `--orange-400` solid fill, per the
 * design archive's components-progress.html "steady continuous tick" rule
 * (no striped/pulsing animation). The fill width transitions with
 * `var(--dur-3) var(--ease-standard)` when the prop changes.
 *
 * The running-status spinner is provided by cts-badge[variant="running"]
 * (a 1.1s `currentColor` SVG arc). The legacy `bi bi-arrow-clockwise` glyph
 * is no longer rendered.
 *
 * @property {object} test - Test instance object; expects `_id`, `testName`,
 *   `created`, `status`, `variant`, `version`, `owner`.
 * @property {boolean} isAdmin - Reveals the Test Owner row. Reflects the
 *   `is-admin` attribute.
 * @property {number} progress - Optional progress percentage (0-100). When
 *   set, an orange progress bar is rendered under the status badge. Values
 *   outside the range are clamped.
 * @fires cts-download-log - When the Download Logs button is clicked, with
 *   `{ detail: { testId } }`; bubbles.
 */
class CtsRunningTestCard extends LitElement {
  static properties = {
    test: { type: Object },
    isAdmin: { type: Boolean, attribute: "is-admin" },
    progress: { type: Number },
  };

  constructor() {
    super();
    this.test = {};
    this.isAdmin = false;
    this.progress = NaN;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  createRenderRoot() {
    return this;
  }

  _formatDate(dateStr) {
    if (!dateStr) return "";
    return new Date(dateStr).toString();
  }

  _formatVariant(variant) {
    if (!variant || typeof variant !== "object") return "";
    return Object.entries(variant)
      .map(([key, value]) => `${key}: ${value}`)
      .join(", ");
  }

  _handleDownload() {
    this.dispatchEvent(
      new CustomEvent("cts-download-log", {
        bubbles: true,
        detail: { testId: this.test._id },
      }),
    );
  }

  _renderProgressBar() {
    const raw = Number(this.progress);
    if (!Number.isFinite(raw)) return nothing;
    const clamped = Math.max(0, Math.min(100, raw));
    return html`
      <div
        class="cts-rtc-progress"
        role="progressbar"
        aria-valuenow="${clamped}"
        aria-valuemin="0"
        aria-valuemax="100"
        aria-label="Test progress"
      >
        <div class="cts-rtc-progress-track">
          <div class="cts-rtc-progress-fill" style="width: ${clamped}%;"></div>
        </div>
      </div>
    `;
  }

  render() {
    const test = this.test;
    if (!test || !test._id) return nothing;

    const badgeVariant = STATUS_BADGE_VARIANTS[test.status] || "skip";
    const variantStr = this._formatVariant(test.variant);

    return html`
      <div class="cts-rtc-card" data-instance-id="${test._id}">
        <div class="cts-rtc-status testStatusAndResult">
          <cts-badge variant="${badgeVariant}" label="${test.status || "UNKNOWN"}"></cts-badge>
          ${this._renderProgressBar()}
        </div>
        <div class="cts-rtc-info">
          <div class="cts-rtc-row">
            <div class="cts-rtc-label">Test Name:</div>
            <div class="cts-rtc-value">${test.testName}</div>
          </div>
          <div class="cts-rtc-row">
            <div class="cts-rtc-label">Test ID:</div>
            <div class="cts-rtc-value">${test._id}</div>
          </div>
          <div class="cts-rtc-row">
            <div class="cts-rtc-label">Created:</div>
            <div class="cts-rtc-value">${this._formatDate(test.created)}</div>
          </div>
          ${variantStr
            ? html`<div class="cts-rtc-row">
                <div class="cts-rtc-label">Variant:</div>
                <div class="cts-rtc-value">${variantStr}</div>
              </div>`
            : nothing}
          ${test.version
            ? html`<div class="cts-rtc-row">
                <div class="cts-rtc-label">Version:</div>
                <div class="cts-rtc-value">${test.version}</div>
              </div>`
            : nothing}
          ${this.isAdmin && test.owner
            ? html`<div class="cts-rtc-row" data-testid="owner-row">
                <div class="cts-rtc-label">Test Owner:</div>
                <div class="cts-rtc-value"
                  >${test.owner.sub}${test.owner.iss ? ` (${test.owner.iss})` : ""}</div
                >
              </div>`
            : nothing}
        </div>
        <div class="cts-rtc-actions">
          <cts-button
            class="downloadBtn"
            variant="secondary"
            size="sm"
            icon="save"
            label="Download Logs"
            full-width
            @cts-click="${this._handleDownload}"
          ></cts-button>
          <cts-link-button
            class="viewBtn"
            variant="secondary"
            size="sm"
            icon="label"
            label="View Test Details"
            full-width
            href="log-detail.html?log=${encodeURIComponent(test._id)}"
          ></cts-link-button>
        </div>
      </div>
    `;
  }
}

customElements.define("cts-running-test-card", CtsRunningTestCard);

export {};
