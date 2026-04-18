import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";

const STATUS_BADGE_VARIANTS = {
  RUNNING: "info",
  WAITING: "warning",
  INTERRUPTED: "interrupted",
};

/**
 * Card summarising a currently running / waiting / interrupted test instance
 * with quick actions for downloading the log and viewing details.
 *
 * @property {Object} test - Test instance object; expects `_id`, `testName`,
 *   `created`, `status`, `variant`, `version`, `owner`.
 * @property {boolean} isAdmin - Reveals the Test Owner row. Reflects the
 *   `is-admin` attribute.
 *
 * @fires cts-download-log - When the Download Logs button is clicked, with
 *   `{ detail: { testId } }`; bubbles.
 */
class CtsRunningTestCard extends LitElement {
  static properties = {
    test: { type: Object },
    isAdmin: { type: Boolean, attribute: "is-admin" },
  };

  constructor() {
    super();
    this.test = {};
    this.isAdmin = false;
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

  render() {
    const test = this.test;
    if (!test || !test._id) return nothing;

    const badgeVariant = STATUS_BADGE_VARIANTS[test.status] || "secondary";
    const variantStr = this._formatVariant(test.variant);

    return html`
      <div class="runningTest row" data-instance-id="${test._id}">
        <div class="col-md-2 testStatusAndResult">
          <cts-badge variant="${badgeVariant}" label="${test.status || "UNKNOWN"}"></cts-badge>
        </div>
        <div class="col-md-8">
          <div class="row">
            <div class="col-md-1">Test Name:</div>
            <div class="col-md-11">${test.testName}</div>
          </div>
          <div class="row">
            <div class="col-md-1">Test ID:</div>
            <div class="col-md-11">${test._id}</div>
          </div>
          <div class="row">
            <div class="col-md-1">Created:</div>
            <div class="col-md-11">${this._formatDate(test.created)}</div>
          </div>
          ${variantStr
            ? html` <div class="row">
                <div class="col-md-1">Variant:</div>
                <div class="col-md-11">${variantStr}</div>
              </div>`
            : nothing}
          ${test.version
            ? html` <div class="row">
                <div class="col-md-1">Version:</div>
                <div class="col-md-11">${test.version}</div>
              </div>`
            : nothing}
          ${this.isAdmin && test.owner
            ? html` <div class="row" data-testid="owner-row">
                <div class="col-md-1">Test Owner:</div>
                <div class="col-md-11"
                  >${test.owner.sub}${test.owner.iss ? ` (${test.owner.iss})` : ""}</div
                >
              </div>`
            : nothing}
        </div>
        <div class="col-md-2">
          <div class="d-grid gap-2">
            <button
              class="btn btn-sm btn-light bg-gradient border border-secondary downloadBtn"
              @click="${this._handleDownload}"
              ><span class="bi bi-save2"></span> Download Logs</button
            >
            <a
              class="btn btn-sm btn-light bg-gradient border border-secondary viewBtn"
              href="log-detail.html?log=${encodeURIComponent(test._id)}"
              ><span class="bi bi-card-list"></span> View Test Details</a
            >
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define("cts-running-test-card", CtsRunningTestCard);
