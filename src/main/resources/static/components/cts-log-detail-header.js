import { LitElement, html, nothing } from "lit";
import "./cts-icon.js";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-link-button.js";
import "./cts-alert.js";
import "./cts-json-editor.js";
import "./cts-test-nav-controls.js";
import { splitTestSummary } from "./test-summary-split.js";

/**
 * Top-level test result -> canonical cts-badge variant. INTERRUPTED maps to
 * `fail` because an interrupted run did not complete successfully and we
 * surface that alongside the FINISHED/INTERRUPTED status badge.
 * Lookup table per components/AGENTS.md §7 (no dynamic class concatenation).
 * @type {Object.<string, string>}
 */
const RESULT_BADGE_VARIANTS = {
  PASSED: "pass",
  FAILED: "fail",
  WARNING: "warn",
  REVIEW: "review",
  SKIPPED: "skip",
  INTERRUPTED: "fail",
};

/**
 * Test running-state -> canonical cts-badge variant. FINISHED is neutral
 * (`skip`) because the sibling result badge carries the outcome; WAITING
 * uses `warn` to signal user action; INTERRUPTED matches RESULT_BADGE_VARIANTS.
 * @type {Object.<string, string>}
 */
const STATUS_BADGE_VARIANTS = {
  RUNNING: "running",
  WAITING: "warn",
  FINISHED: "skip",
  INTERRUPTED: "fail",
};

/**
 * Per-condition result keys (lowercase, mirroring backend log entries) used
 * to aggregate counts in the "Results:" summary row. These keys must match
 * `entry.result.toLowerCase()` from the backend (success/failure/warning/
 * review/info), so they are intentionally NOT the canonical badge variant
 * names — see RESULT_TYPE_BADGE_VARIANTS for the key -> variant mapping.
 * @type {ReadonlyArray<string>}
 */
const RESULT_TYPES = ["success", "failure", "warning", "review", "info"];

/**
 * Per-condition result key -> canonical cts-badge variant. `info` aggregates
 * informational log messages (not a status), so it keeps the retokenized
 * `info-subtle` utility variant on the status-info palette.
 * @type {Object.<string, string>}
 */
const RESULT_TYPE_BADGE_VARIANTS = {
  success: "pass",
  failure: "fail",
  warning: "warn",
  review: "review",
  info: "info-subtle",
};

const STYLE_ID = "cts-log-detail-header-styles";

// Scoped CSS for the log-detail header. All values flow from oidf-tokens.css.
// Mirrors the design archive's card pattern for the test summary panel; the
// metadata grid uses a 2-column layout (label / value) instead of the legacy
// 12-column Bootstrap row.
const STYLE_TEXT = `
  cts-log-detail-header {
    display: block;
  }

  cts-log-detail-header .logHeaderCard,
  cts-log-detail-header .logSecondaryCard {
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    box-shadow: var(--shadow-1);
    margin-bottom: var(--space-4);
  }
  cts-log-detail-header .logHeaderBody,
  cts-log-detail-header .logSecondaryBody {
    padding: var(--space-5);
  }

  cts-log-detail-header .logHeaderGrid {
    display: grid;
    grid-template-columns: minmax(120px, 160px) 1fr auto;
    gap: var(--space-4);
    align-items: start;
  }
  cts-log-detail-header .logStatusStack {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  cts-log-detail-header .logMetaTable {
    display: grid;
    grid-template-columns: minmax(120px, 180px) 1fr;
    gap: var(--space-2) var(--space-4);
    font-size: var(--fs-13);
  }
  cts-log-detail-header .logMetaLabel {
    color: var(--fg-soft);
    font-weight: var(--fw-bold);
  }
  cts-log-detail-header .logMetaValue {
    color: var(--fg);
    word-break: break-word;
  }

  cts-log-detail-header .logActionStack {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    min-width: 180px;
  }
  cts-log-detail-header .logActionStack cts-button,
  cts-log-detail-header .logActionStack cts-link-button {
    width: 100%;
  }

  cts-log-detail-header .logResultRow {
    margin-top: var(--space-3);
  }
  cts-log-detail-header .logResultBadges {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-1);
  }

  cts-log-detail-header .failureSummary {
    margin-top: var(--space-4);
    border-top: 1px solid var(--border);
    padding-top: var(--space-3);
  }
  cts-log-detail-header .failureSummaryTitle {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-weight: var(--fw-bold);
    color: var(--fg);
    cursor: pointer;
    border-radius: var(--radius-2);
  }
  cts-log-detail-header .failureSummaryTitle:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-log-detail-header .failureList {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin-top: var(--space-3);
  }
  cts-log-detail-header .failureItem {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--fs-13);
  }
  cts-log-detail-header .failureText {
    color: var(--fg);
    cursor: pointer;
    text-decoration: underline;
    text-decoration-thickness: 1px;
    text-underline-offset: 2px;
    border-radius: var(--radius-2);
  }
  cts-log-detail-header .failureText:hover { color: var(--fg-link); }
  cts-log-detail-header .failureText:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-log-detail-header .logRequirementBadge {
    display: inline-block;
    background: var(--ink-50);
    border: 1px solid var(--border);
    border-radius: var(--radius-pill);
    color: var(--fg-muted);
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    padding: 1px var(--space-2);
  }

  cts-log-detail-header .configHeader {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
    margin-bottom: var(--space-3);
  }
  /* '.configBlock' is a pre-existing class name from when this slot
     rendered a <pre>; the slot is now a <cts-json-editor>. The companion
     'data-testid="config-json"' attribute (used by cts-log-detail-header
     .stories.js to find the editor) is the test seam — keep both in
     sync if either is renamed. */
  cts-log-detail-header .configBlock {
    display: block;
    margin: 0;
    min-height: calc(var(--space-6) * 14);
  }
  cts-log-detail-header .runningTestRow {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }
  cts-log-detail-header .runningExportedLabel {
    font-weight: var(--fw-bold);
    color: var(--fg);
  }
  /* '.runningExportedBlock' is a pre-existing class name from when this
     slot rendered a <pre>; the slot is now a <cts-json-editor>. Used
     only as a CSS hook (no test/clipboard seam), so renaming requires
     coordinating only with this CSS rule and the matching call site. */
  cts-log-detail-header .runningExportedBlock {
    display: block;
    margin: var(--space-2) 0 0 0;
    min-height: calc(var(--space-6) * 10);
  }
  cts-log-detail-header .runningTestActions {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    max-width: 200px;
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
 * @property {string} summary - Test-level summary. Renders as an
 *   "About this test" zone above the metadata. Test authors who want
 *   to surface imperative user instructions (e.g. "remove cookies
 *   before proceeding") as a distinct callout split the summary with
 *   the marker exposed by `./test-summary-split.js`
 *   (`SUMMARY_SPLIT_MARKER`). Splitting happens at render time only —
 *   the backend `@PublishTestModule.summary()` contract is unchanged.
 *   R24 origin: `docs/brainstorms/2026-04-13-cts-ux-improvement-plan-requirements.md`.
 */

/**
 * Header card for the log-detail page. Shows status/result badges, metadata,
 * a configuration viewer, a failure summary that deep-links into individual
 * log entries, and action buttons (repeat, upload images, download, publish,
 * start/stop). Does not perform the actions itself — emits events.
 *
 * Light DOM. Scoped CSS is injected once on first render. All visual styling
 * routes through the OIDF tokens vendored in `oidf-tokens.css`; no Bootstrap
 * `row`/`col-*`/`btn-*`/`badge bg-*`/`alert-*` classes are emitted.
 *
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
 * @fires cts-edit-config - When the Edit configuration button is clicked,
 *   with `{ detail: { testId, planId, config } }`; bubbles. The page-level
 *   handler navigates to schedule-test.html seeded with the supplied
 *   `planId` (preferred) or `config`.
 * @fires cts-share-link - When the Private link button is clicked, with
 *   `{ detail: { testId } }`; bubbles. The page-level handler opens the
 *   private-link expiration modal and POSTs to `/api/info/{testId}/share`.
 * @fires cts-download-log - When the Download Logs button is clicked, with
 *   `{ detail: { testId } }`; bubbles.
 * @fires cts-publish - When a Publish (summary / everything) or Unpublish
 *   button is clicked, with `{ detail: { testId, action, mode? } }` where
 *   `action` is `publish` or `unpublish` and `mode` is `summary` or
 *   `everything` (omitted for unpublish); bubbles.
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
    ensureStylesInjected();
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

  _handleFailureClick(event) {
    const entryId = event.currentTarget.dataset.entryId;
    if (entryId) this._handleScrollToEntry(entryId);
  }

  _handleFailureKeydown(event) {
    if (event.key !== "Enter" && event.key !== " ") return;
    event.preventDefault();
    const entryId = event.currentTarget.dataset.entryId;
    if (entryId) this._handleScrollToEntry(entryId);
  }

  _handleFailureSummaryKeydown(event) {
    if (event.key !== "Enter" && event.key !== " ") return;
    event.preventDefault();
    this._toggleFailures();
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

  _handleEditConfig() {
    this.dispatchEvent(
      new CustomEvent("cts-edit-config", {
        bubbles: true,
        detail: {
          testId: this.testInfo.testId,
          planId: this.testInfo.planId || null,
          config: this.testInfo.config || null,
        },
      }),
    );
  }

  _handleShareLink() {
    this.dispatchEvent(
      new CustomEvent("cts-share-link", {
        bubbles: true,
        detail: { testId: this.testInfo.testId },
      }),
    );
  }

  _dispatchPublish(action, mode) {
    this.dispatchEvent(
      new CustomEvent("cts-publish", {
        bubbles: true,
        detail: {
          testId: this.testInfo.testId,
          action,
          ...(mode ? { mode } : {}),
        },
      }),
    );
  }

  _handlePublishSummary() {
    this._dispatchPublish("publish", "summary");
  }

  _handlePublishEverything() {
    this._dispatchPublish("publish", "everything");
  }

  _handleUnpublish() {
    this._dispatchPublish("unpublish", null);
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

  _shouldShowRunningCard() {
    // RUNNING and WAITING are mid-flight states. INTERRUPTED also keeps the
    // card visible so the page-level FINAL_ERROR alert has a host to render
    // into via the [data-slot="error"] placeholder. FINISHED hides the card.
    const status = (this.testInfo.status || "").toUpperCase();
    return status === "RUNNING" || status === "WAITING" || status === "INTERRUPTED";
  }

  _isReadonly() {
    return this.isPublic;
  }

  _renderTestInfoCard() {
    const test = this.testInfo;
    const variantStr = this._formatVariant(test.variant);
    const resultVariant = RESULT_BADGE_VARIANTS[test.result] || "skip";

    return html`
      <div class="logHeaderCard">
        <div class="logHeaderBody" data-instance-id="${test.testId}">
          <div class="logHeaderGrid" id="logHeader">
            <div class="logStatusStack" id="testStatusAndResult">
              ${test.result
                ? html`<cts-badge variant="${resultVariant}" label="${test.result}"></cts-badge>`
                : nothing}
              ${test.status
                ? html`<cts-badge
                    variant="${STATUS_BADGE_VARIANTS[test.status] || "skip"}"
                    label="${test.status}"
                  ></cts-badge>`
                : nothing}
            </div>

            <div>
              <div class="logMetaTable">
                <div class="logMetaLabel">Test Name:</div>
                <div class="logMetaValue">${test.testName}</div>
                ${variantStr
                  ? html`
                      <div class="logMetaLabel"> Variant: </div>
                      <div class="logMetaValue"> ${variantStr} </div>
                    `
                  : nothing}
                <div class="logMetaLabel">Test ID:</div>
                <div class="logMetaValue">${test.testId}</div>
                <div class="logMetaLabel">Created:</div>
                <div class="logMetaValue"> ${this._formatDate(test.created)} </div>
                ${test.description
                  ? html`
                      <div class="logMetaLabel"> Description: </div>
                      <div class="logMetaValue"> ${test.description} </div>
                    `
                  : nothing}
                ${test.version
                  ? html`
                      <div class="logMetaLabel"> Test Version: </div>
                      <div class="logMetaValue"> ${test.version} </div>
                    `
                  : nothing}
                ${this.isAdmin && test.owner
                  ? html`
                      <div class="logMetaLabel" data-testid="owner-row"> Test Owner: </div>
                      <div class="logMetaValue">
                        ${test.owner.sub}${test.owner.iss ? ` (${test.owner.iss})` : ""}
                      </div>
                    `
                  : nothing}
                ${test.planId
                  ? html`
                      <div class="logMetaLabel"> Plan ID: </div>
                      <div class="logMetaValue"> ${test.planId} </div>
                    `
                  : nothing}
              </div>
              ${this._renderSummaryZones(test.summary)} ${this._renderResultSummary()}
              ${this._renderFailureSummary()}
            </div>

            <div>${this._renderActionButtons()}</div>
          </div>
        </div>
      </div>
    `;
  }

  _renderSummaryZones(rawSummary) {
    const { description, instructions } = splitTestSummary(rawSummary);
    if (!description && !instructions) return nothing;

    return html`
      ${description
        ? html`<cts-alert variant="info">
            <div class="summaryZone summaryZone--about" data-testid="about-test-zone">
              <span class="summaryEyebrow">About this test</span>
              <div class="summaryBody">${description}</div>
            </div>
          </cts-alert>`
        : nothing}
      ${instructions
        ? html`<cts-alert variant="warning">
            <div class="summaryZone summaryZone--instructions" data-testid="user-instructions-zone">
              <span class="summaryEyebrow">What you need to do</span>
              <div class="summaryBody">${instructions}</div>
            </div>
          </cts-alert>`
        : nothing}
    `;
  }

  _renderResultSummary() {
    const counts = this._getResultCounts();
    return html`
      <div class="logResultRow">
        <div class="logMetaTable">
          <div class="logMetaLabel">Results:</div>
          <div class="logResultBadges" data-testid="result-summary">
            ${this._renderResultBadges(counts)}
          </div>
        </div>
      </div>
    `;
  }

  _renderResultBadges(counts) {
    return RESULT_TYPES.map(
      (type) => html`
        <cts-badge
          variant="${RESULT_TYPE_BADGE_VARIANTS[type]}"
          label="${type.toUpperCase()} ${counts[type]}"
          data-testid="count-${type}"
        ></cts-badge>
      `,
    );
  }

  _renderFailureSummary() {
    const failures = this._getFailures();
    if (failures.length === 0) return nothing;

    return html`
      <div class="failureSummary" data-testid="failure-summary">
        <div
          class="failureSummaryTitle"
          role="button"
          tabindex="0"
          @click=${this._toggleFailures}
          @keydown=${this._handleFailureSummaryKeydown}
        >
          Failure summary:
          <cts-icon name="${this._failuresExpanded ? "chevron-up" : "chevron-down"}"></cts-icon>
        </div>
        ${this._failuresExpanded
          ? html`<div class="failureList" data-testid="failure-list">
              ${this._renderFailureList(failures)}
            </div>`
          : nothing}
      </div>
    `;
  }

  _renderFailureList(failures) {
    return failures.map(
      (item) => html`
        <div class="failureItem">
          <cts-badge
            variant="${RESULT_BADGE_VARIANTS[item.result] || "skip"}"
            label="${item.result}"
          ></cts-badge>
          ${this._renderRequirementBadges(item.requirements)}
          <span
            class="failureText"
            role="button"
            tabindex="0"
            data-entry-id=${item._id}
            @click=${this._handleFailureClick}
            @keydown=${this._handleFailureKeydown}
            >${item.src}: ${item.msg}</span
          >
        </div>
      `,
    );
  }

  _renderRequirementBadges(requirements) {
    return (requirements || []).map((req) => html`<span class="logRequirementBadge">${req}</span>`);
  }

  _renderActionButtons() {
    const test = this.testInfo;
    const readonly = this._isReadonly();
    const uploadCount = this._getUploadCount();

    return html`
      <div class="logActionStack">
        <cts-test-nav-controls
          id="testNavControls"
          data-testid="test-nav-controls"
          test-id="${test.testId}"
          plan-id="${test.planId || ""}"
          ?readonly=${readonly}
          ?public-view=${this.isPublic}
        ></cts-test-nav-controls>
        ${!readonly
          ? html`
              <cts-button
                variant="secondary"
                size="sm"
                icon="arrow-down-up"
                label="Repeat Test"
                data-testid="repeat-test-btn"
                @cts-click=${this._handleRepeatTest}
              ></cts-button>
              <cts-button
                variant="${uploadCount ? "primary" : "secondary"}"
                size="sm"
                icon="image-01"
                label="${uploadCount ? `Upload Images (${uploadCount})` : "Upload Images"}"
                data-testid="upload-images-btn"
                @cts-click=${this._handleUploadImages}
              ></cts-button>
            `
          : nothing}
        <cts-button
          variant="secondary"
          size="sm"
          icon="settings"
          label="View configuration"
          data-testid="view-config-btn"
          @cts-click=${this._toggleConfig}
        ></cts-button>
        ${!readonly
          ? html`<cts-button
              variant="secondary"
              size="sm"
              icon="edit-pencil-01"
              label="Edit configuration"
              title="Create a new test plan based on the configuration used in this one"
              data-testid="edit-config-btn"
              @cts-click=${this._handleEditConfig}
            ></cts-button>`
          : nothing}
        ${!readonly || test.publish === "everything"
          ? html`<cts-button
              variant="secondary"
              size="sm"
              icon="save"
              label="Download Logs"
              data-testid="download-log-btn"
              @cts-click=${this._handleDownloadLog}
            ></cts-button>`
          : nothing}
        ${test.planId
          ? html`<cts-link-button
              variant="secondary"
              size="sm"
              icon="bookmark"
              label="Return to Plan"
              href="plan-detail.html?plan=${encodeURIComponent(test.planId)}"
              data-testid="return-to-plan-link"
            ></cts-link-button>`
          : nothing}
        ${!readonly
          ? html`<cts-button
              variant="secondary"
              size="sm"
              icon="bookmark"
              label="Private link"
              data-testid="share-link-btn"
              @cts-click=${this._handleShareLink}
            ></cts-button>`
          : nothing}
        ${this._renderPublishButtons()}
      </div>
    `;
  }

  _renderPublishButtons() {
    const test = this.testInfo;
    const readonly = this._isReadonly();
    if (readonly) return nothing;

    if (!test.publish) {
      // Pre-publish: admin sees the summary + everything split.
      if (!this.isAdmin) return nothing;
      return html`
        <cts-button
          variant="secondary"
          size="sm"
          icon="bookmark"
          label="Publish summary"
          data-testid="publish-summary-btn"
          @cts-click=${this._handlePublishSummary}
        ></cts-button>
        <cts-button
          variant="secondary"
          size="sm"
          icon="bookmark"
          label="Publish everything"
          data-testid="publish-btn"
          @cts-click=${this._handlePublishEverything}
        ></cts-button>
      `;
    }

    // Already published: admin sees Unpublish; everyone (admin or not, in
    // non-readonly state) sees the public link.
    return html`
      ${this.isAdmin
        ? html`<cts-button
            variant="secondary"
            size="sm"
            icon="close-circle"
            label="Unpublish"
            data-testid="unpublish-btn"
            @cts-click=${this._handleUnpublish}
          ></cts-button>`
        : nothing}
      <cts-link-button
        variant="secondary"
        size="sm"
        icon="bookmark"
        label="Public link"
        href="log-detail.html?log=${encodeURIComponent(test.testId)}&amp;public=true"
        data-testid="public-link"
      ></cts-link-button>
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
      <div class="logSecondaryCard" data-testid="config-panel">
        <div class="logSecondaryBody">
          <div class="configHeader">
            <span>Configuration for <code>${this.testInfo.testId}</code></span>
            <cts-button
              variant="secondary"
              size="sm"
              icon="x"
              label="Close"
              @cts-click=${this._toggleConfig}
            ></cts-button>
          </div>
          <cts-json-editor
            class="configBlock"
            data-testid="config-json"
            readonly
            aria-label="Test configuration JSON"
            .value=${configJson}
          ></cts-json-editor>
        </div>
      </div>
    `;
  }

  _renderRunningTestInfo() {
    if (!this._shouldShowRunningCard()) return nothing;
    const test = this.testInfo;
    const status = (test.status || "").toUpperCase();
    const isInterrupted = status === "INTERRUPTED";

    return html`
      <div class="logSecondaryCard" data-testid="running-test-info">
        <div class="logSecondaryBody">
          <div id="runningTestError" data-slot="error" data-testid="running-error-slot"></div>
          ${status === "RUNNING"
            ? html`<cts-alert variant="info"
                ><b>This test is currently running.</b> Values exported from the test are available
                below along with any URLs that need to be visited interactively.</cts-alert
              >`
            : status === "WAITING"
              ? html`<cts-alert variant="warning"
                  ><b>This test is waiting.</b> The test is awaiting user interaction or an external
                  event.</cts-alert
                >`
              : html`<cts-alert variant="danger"
                  ><b>This test was interrupted.</b> See the error details above.</cts-alert
                >`}
          <div class="runningTestRow">
            ${test.exposed && Object.keys(test.exposed).length > 0
              ? html`
                  <div>
                    <div class="runningExportedLabel"> Exported values: </div>
                    <cts-json-editor
                      class="runningExportedBlock"
                      readonly
                      aria-label="Exported test values"
                      .value=${JSON.stringify(test.exposed, null, 2)}
                    ></cts-json-editor>
                  </div>
                `
              : nothing}
            <div
              id="runningTestBrowser"
              data-slot="browser"
              data-testid="running-browser-slot"
            ></div>
            ${isInterrupted
              ? nothing
              : html`<div class="runningTestActions">
                  <cts-button
                    variant="primary"
                    size="sm"
                    icon="play"
                    label="Start"
                    data-testid="start-btn"
                    @cts-click=${this._handleStartTest}
                  ></cts-button>
                  <cts-button
                    variant="secondary"
                    size="sm"
                    icon="stop"
                    label="Stop"
                    data-testid="stop-btn"
                    @cts-click=${this._handleStopTest}
                  ></cts-button>
                </div>`}
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

export {};
