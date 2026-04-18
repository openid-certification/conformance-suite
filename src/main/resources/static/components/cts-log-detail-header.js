import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";

const RESULT_BADGE_VARIANTS = {
  PASSED: "success",
  FAILED: "failure",
  WARNING: "warning",
  REVIEW: "review",
  SKIPPED: "skipped",
  INTERRUPTED: "interrupted",
};

const STATUS_BADGE_VARIANTS = {
  RUNNING: "info",
  WAITING: "warning",
  FINISHED: "finished",
  INTERRUPTED: "interrupted",
};

const RESULT_TYPES = ["success", "failure", "warning", "review", "info"];

/**
 * @typedef {object} TestInfo
 * @property {string} testId - Test instance ID.
 * @property {string} testName - Module class name.
 * @property {string} status - One of: RUNNING, WAITING, FINISHED, INTERRUPTED.
 * @property {string} result - Final result (PASSED/FAILED/WARNING/REVIEW/SKIPPED).
 * @property {Array} results - Log entries used for the result/failure summary.
 * @property {string} created - ISO timestamp of test creation.
 * @property {string} description - Human-readable description.
 * @property {string} version - Test module version.
 * @property {object} variant - Variant parameters map.
 * @property {string} planId - Parent plan ID, if the test belongs to one.
 * @property {object} owner - `{ sub, iss }` owner identity (admin only).
 * @property {object} config - Test configuration JSON.
 * @property {object} exposed - Values exported by a running test.
 * @property {string|boolean} publish - Publish mode ("summary", "everything") or falsy.
 * @property {string} summary - Test-level summary shown as an info banner.
 */

/**
 * Header card for the log-detail page. Shows status/result badges, metadata,
 * a configuration viewer, a failure summary that deep-links into individual
 * log entries, and action buttons (repeat, upload images, download, publish,
 * start/stop). Does not perform the actions itself — emits events.
 * @property {TestInfo} testInfo - The test info object fetched from
 *   `/api/info`. Reflects the `test-info` attribute when set as a string.
 * @property {boolean} isAdmin - Reveals admin-only rows and actions.
 *   Reflects the `is-admin` attribute.
 * @property {boolean} isPublic - Public (read-only) view hides repeat /
 *   upload / publish actions. Reflects the `is-public` attribute.
 * @fires cts-scroll-to-entry - When a failure-summary item is clicked, with
 *   `{ detail: { entryId } }`; bubbles.
 * @fires cts-repeat-test - When the Repeat Test button is clicked, with
 *   `{ detail: { testId } }`; bubbles.
 * @fires cts-upload-images - When the Upload Images button is clicked, with
 *   `{ detail: { testId } }`; bubbles.
 * @fires cts-download-log - When the Download Logs button is clicked, with
 *   `{ detail: { testId } }`; bubbles.
 * @fires cts-publish - When the Publish/Unpublish button is clicked, with
 *   `{ detail: { testId, action } }` where `action` is `publish` or
 *   `unpublish`; bubbles.
 * @fires cts-start-test - When the Start button is clicked on a running /
 *   waiting test, with `{ detail: { testId } }`; bubbles.
 * @fires cts-stop-test - When the Stop button is clicked on a running /
 *   waiting test, with `{ detail: { testId } }`; bubbles.
 */
class CtsLogDetailHeader extends LitElement {
  static properties = {
    testInfo: { type: Object, attribute: "test-info" },
    isAdmin: { type: Boolean, attribute: "is-admin" },
    isPublic: { type: Boolean, attribute: "is-public" },
    _configVisible: { state: true },
    _failuresExpanded: { state: true },
  };

  constructor() {
    super();
    this.testInfo = null;
    this.isAdmin = false;
    this.isPublic = false;
    this._configVisible = false;
    this._failuresExpanded = true;
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

  _getResultCounts() {
    const counts = {};
    for (const type of RESULT_TYPES) {
      counts[type] = 0;
    }
    if (this.testInfo && Array.isArray(this.testInfo.results)) {
      for (const entry of this.testInfo.results) {
        const key = (entry.result || "").toLowerCase();
        if (key in counts) {
          counts[key]++;
        }
      }
    }
    return counts;
  }

  _getFailures() {
    if (!this.testInfo || !Array.isArray(this.testInfo.results)) return [];
    return this.testInfo.results.filter(
      (entry) =>
        entry.result === "FAILURE" ||
        entry.result === "WARNING" ||
        entry.result === "SKIPPED" ||
        entry.result === "INTERRUPTED",
    );
  }

  _handleScrollToEntry(entryId) {
    this.dispatchEvent(
      new CustomEvent("cts-scroll-to-entry", {
        bubbles: true,
        detail: { entryId },
      }),
    );
  }

  _handleRepeatTest() {
    this.dispatchEvent(
      new CustomEvent("cts-repeat-test", {
        bubbles: true,
        detail: { testId: this.testInfo.testId },
      }),
    );
  }

  _handleUploadImages() {
    this.dispatchEvent(
      new CustomEvent("cts-upload-images", {
        bubbles: true,
        detail: { testId: this.testInfo.testId },
      }),
    );
  }

  _handleDownloadLog() {
    this.dispatchEvent(
      new CustomEvent("cts-download-log", {
        bubbles: true,
        detail: { testId: this.testInfo.testId },
      }),
    );
  }

  _handlePublish() {
    this.dispatchEvent(
      new CustomEvent("cts-publish", {
        bubbles: true,
        detail: {
          testId: this.testInfo.testId,
          action: this.testInfo.publish ? "unpublish" : "publish",
        },
      }),
    );
  }

  _handleStartTest() {
    this.dispatchEvent(
      new CustomEvent("cts-start-test", {
        bubbles: true,
        detail: { testId: this.testInfo.testId },
      }),
    );
  }

  _handleStopTest() {
    this.dispatchEvent(
      new CustomEvent("cts-stop-test", {
        bubbles: true,
        detail: { testId: this.testInfo.testId },
      }),
    );
  }

  _toggleConfig() {
    this._configVisible = !this._configVisible;
  }

  _toggleFailures() {
    this._failuresExpanded = !this._failuresExpanded;
  }

  _isRunning() {
    // render() guards `!this.testInfo`, so everything downstream can assume
    // testInfo is non-null. This keeps the optional-chaining style consistent
    // with the rest of the render tree.
    const status = (this.testInfo.status || "").toUpperCase();
    return status === "RUNNING" || status === "WAITING";
  }

  _isReadonly() {
    return this.isPublic;
  }

  _renderTestInfoCard() {
    const test = this.testInfo;
    const variantStr = this._formatVariant(test.variant);
    const resultVariant = RESULT_BADGE_VARIANTS[test.result] || "secondary";

    return html`
      <div class="card">
        <div class="card-body" data-instance-id="${test.testId}">
          <div class="row" id="logHeader">
            <div class="col-md-2" id="testStatusAndResult">
              ${test.result
                ? html`<cts-badge variant="${resultVariant}" label="${test.result}"></cts-badge>`
                : nothing}
              ${test.status
                ? html`<cts-badge
                    variant="${STATUS_BADGE_VARIANTS[test.status] || "secondary"}"
                    label="${test.status}"
                  ></cts-badge>`
                : nothing}
            </div>

            <div class="col-md-8">
              <div class="row">
                <div class="col-md-2">Test Name:</div>
                <div class="col-md-10">${test.testName}</div>
              </div>
              ${variantStr
                ? html` <div class="row">
                    <div class="col-md-2">Variant:</div>
                    <div class="col-md-10">${variantStr}</div>
                  </div>`
                : nothing}
              <div class="row">
                <div class="col-md-2">Test ID:</div>
                <div class="col-md-10">${test.testId}</div>
              </div>
              <div class="row">
                <div class="col-md-2">Created:</div>
                <div class="col-md-10">${this._formatDate(test.created)}</div>
              </div>
              ${test.description
                ? html` <div class="row">
                    <div class="col-md-2">Description:</div>
                    <div class="col-md-10">${test.description}</div>
                  </div>`
                : nothing}
              ${test.version
                ? html` <div class="row">
                    <div class="col-md-2">Test Version:</div>
                    <div class="col-md-10">${test.version}</div>
                  </div>`
                : nothing}
              ${this.isAdmin && test.owner
                ? html` <div class="row" data-testid="owner-row">
                    <div class="col-md-2">Test Owner:</div>
                    <div class="col-md-10"
                      >${test.owner.sub}${test.owner.iss ? ` (${test.owner.iss})` : ""}</div
                    >
                  </div>`
                : nothing}
              ${test.planId
                ? html` <div class="row">
                    <div class="col-md-2">Plan ID:</div>
                    <div class="col-md-10">${test.planId}</div>
                  </div>`
                : nothing}
              ${test.summary
                ? html` <div class="row">
                    <div class="col-md-12">
                      <p class="bgSummary bg-info">${test.summary}</p>
                    </div>
                  </div>`
                : nothing}
              ${this._renderResultSummary()} ${this._renderFailureSummary()}
            </div>

            <div class="col-md-2"> ${this._renderActionButtons()} </div>
          </div>
        </div>
      </div>
    `;
  }

  _renderResultSummary() {
    const counts = this._getResultCounts();
    return html`
      <div class="row">
        <div class="col-md-2">Results:</div>
        <div class="col-md-10 labelCollection" data-testid="result-summary">
          ${RESULT_TYPES.map(
            (type) => html`
              <span class="badge result-${type}"
                >${type.toUpperCase()}
                <span class="badge rounded-pill" data-testid="count-${type}"
                  >${counts[type]}</span
                ></span
              >
            `,
          )}
        </div>
      </div>
    `;
  }

  _renderFailureSummary() {
    const failures = this._getFailures();
    if (failures.length === 0) return nothing;

    return html`
      <div class="row failureSummary" data-testid="failure-summary">
        <div class="col-md-12">
          <div
            class="failureSummaryTitle"
            role="button"
            tabindex="0"
            @click="${this._toggleFailures}"
            @keydown="${(e) => {
              if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                this._toggleFailures();
              }
            }}"
          >
            Failure summary:
            <span class="bi ${this._failuresExpanded ? "bi-chevron-up" : "bi-chevron-down"}"></span>
          </div>
          ${this._failuresExpanded
            ? html` <div data-testid="failure-list">
                ${failures.map(
                  (item) =>
                    html` <div class="col-md-12">
                      <span class="badge labelCollection result-${item.result.toLowerCase()}"
                        >${item.result}</span
                      >
                      ${(item.requirements || []).map(
                        (req) =>
                          html`<span class="log-requirement badge labelCollection bg-secondary"
                            >${req}</span
                          >`,
                      )}
                      <span
                        class="failureText showHover"
                        role="button"
                        tabindex="0"
                        @click="${() => this._handleScrollToEntry(item._id)}"
                        @keydown="${(e) => {
                          if (e.key === "Enter" || e.key === " ") {
                            e.preventDefault();
                            this._handleScrollToEntry(item._id);
                          }
                        }}"
                        >${item.src}: ${item.msg}</span
                      >
                    </div>`,
                )}
              </div>`
            : nothing}
        </div>
      </div>
    `;
  }

  _renderActionButtons() {
    const test = this.testInfo;
    const readonly = this._isReadonly();
    const uploadCount = this._getUploadCount();

    return html`
      <div class="d-grid gap-1">
        ${!readonly
          ? html` <button
                class="btn btn-sm btn-light bg-gradient border border-secondary"
                @click="${this._handleRepeatTest}"
                data-testid="repeat-test-btn"
                ><span class="bi bi-arrow-down-up"></span> Repeat Test</button
              >
              <button
                class="btn btn-sm btn-light bg-gradient border border-secondary ${uploadCount
                  ? "btn-info"
                  : ""}"
                @click="${this._handleUploadImages}"
                data-testid="upload-images-btn"
                ><span class="bi bi-file-image"></span> Upload Images
                ${uploadCount
                  ? html`<span class="badge rounded-pill bg-secondary text-info"
                      >${uploadCount}</span
                    >`
                  : nothing}
              </button>`
          : nothing}
        <button
          class="btn btn-sm btn-light bg-gradient border border-secondary"
          @click="${this._toggleConfig}"
          data-testid="view-config-btn"
          ><span class="bi bi-wrench-adjustable"></span> View Config</button
        >
        ${!readonly || test.publish === "everything"
          ? html` <button
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              @click="${this._handleDownloadLog}"
              data-testid="download-log-btn"
              ><span class="bi bi-save2"></span> Download Logs</button
            >`
          : nothing}
        ${test.planId
          ? html` <a
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              href="plan-detail.html?plan=${encodeURIComponent(test.planId)}"
              data-testid="return-to-plan-link"
              ><span class="bi bi-bookmarks"></span> Return to Plan</a
            >`
          : nothing}
        ${!readonly && this.isAdmin
          ? html` <button
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              @click="${this._handlePublish}"
              data-testid="publish-btn"
              ><span class="bi ${test.publish ? "bi-slash-circle" : "bi-bookmarks"}"></span>
              ${test.publish ? "Unpublish" : "Publish"}</button
            >`
          : nothing}
      </div>
    `;
  }

  _getUploadCount() {
    if (!this.testInfo || !Array.isArray(this.testInfo.results)) return 0;
    return this.testInfo.results.filter((entry) => entry.upload).length;
  }

  _renderConfigPanel() {
    if (!this._configVisible || !this.testInfo) return nothing;
    const configJson = JSON.stringify(this.testInfo.config || {}, null, 4);

    return html`
      <div class="card mt-2" data-testid="config-panel">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-center mb-2">
            <span>Configuration for <code>${this.testInfo.testId}</code></span>
            <button
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              @click="${this._toggleConfig}"
              ><span class="bi bi-x"></span> Close</button
            >
          </div>
          <div class="wrapLongStrings">
            <pre class="row-bg-light p-1" data-testid="config-json">${configJson}</pre>
          </div>
        </div>
      </div>
    `;
  }

  _renderRunningTestInfo() {
    if (!this._isRunning()) return nothing;
    const test = this.testInfo;
    const isActive = test.status === "RUNNING";

    return html`
      <div class="card mt-2" data-testid="running-test-info">
        <div class="card-body">
          ${isActive
            ? html`<div class="alert alert-info"
                ><b>This test is currently running.</b> Values exported from the test are available
                below along with any URLs that need to be visited interactively.</div
              >`
            : html`<div class="alert alert-warning"
                ><b>This test is waiting.</b> The test is awaiting user interaction or an external
                event.</div
              >`}
          ${test.exposed && Object.keys(test.exposed).length > 0
            ? html` <div class="row mb-2">
                <div class="col-md-12">
                  <strong>Exported values:</strong>
                  <pre class="row-bg-light p-1">${JSON.stringify(test.exposed, null, 2)}</pre>
                </div>
              </div>`
            : nothing}
          <div class="row">
            <div class="col-md-12">
              <div class="d-grid gap-1" style="max-width: 200px;">
                <button
                  class="btn btn-sm btn-success bg-gradient border border-secondary"
                  @click="${this._handleStartTest}"
                  data-testid="start-btn"
                  ><span class="bi bi-play-fill"></span> Start</button
                >
                <button
                  class="btn btn-sm btn-light bg-gradient border border-secondary"
                  @click="${this._handleStopTest}"
                  data-testid="stop-btn"
                  ><span class="bi bi-stop-fill"></span> Stop</button
                >
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  render() {
    if (!this.testInfo) return nothing;

    return html`
      ${this._renderTestInfoCard()} ${this._renderConfigPanel()} ${this._renderRunningTestInfo()}
    `;
  }
}

customElements.define("cts-log-detail-header", CtsLogDetailHeader);
