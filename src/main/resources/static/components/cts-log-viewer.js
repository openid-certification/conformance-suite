import { LitElement, html, nothing } from "lit";
import "./cts-icon.js";
import "./cts-badge.js";
import "./cts-alert.js";
import "./cts-log-entry.js";

const FAILURE_THRESHOLD = 3;
const POLL_INTERVAL_MS = 3000;

/**
 * Maps the raw log-entry `result` value to a canonical cts-badge variant
 * from the OIDF status palette. Lookup table per components/AGENTS.md §7
 * (no dynamic class concatenation). INFO renders on the status-info palette
 * via the retained `info-subtle` utility variant — info messages aren't a
 * status, they're aggregated counts.
 * @type {Object.<string, string>}
 */
const COUNT_BADGE_VARIANTS = {
  SUCCESS: "pass",
  FAILURE: "fail",
  WARNING: "warn",
  REVIEW: "review",
  SKIPPED: "skip",
  INFO: "info-subtle",
};

const STYLE_ID = "cts-log-viewer-styles";

// Scoped CSS for the log viewer chrome. Failure banner uses cts-alert.
// Result-summary badges arrange in a wrap. The startBlock header (used to
// collapse a related run of entries) keeps its dark band but is now token-
// driven (orange-700 surface) instead of inline `#336`.
const STYLE_TEXT = `
  cts-log-viewer {
    display: block;
  }
  cts-log-viewer .logResultSummary {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    margin-bottom: var(--space-3);
  }
  cts-log-viewer .logEntries {
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    overflow: hidden;
    background: var(--bg-elev);
  }
  cts-log-viewer .logEntries:empty {
    display: none;
  }
  cts-log-viewer .logEmpty {
    padding: var(--space-5);
    text-align: center;
    color: var(--fg-soft);
  }
  cts-log-viewer .logLoading {
    padding: var(--space-5);
    text-align: center;
    color: var(--fg-soft);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-2);
  }
  cts-log-viewer .logLoading .spinner-border {
    display: inline-block;
    width: 16px;
    height: 16px;
    border: 2px solid var(--border-strong);
    border-top-color: var(--orange-400);
    border-radius: 50%;
    animation: cts-log-viewer-spin 0.9s linear infinite;
  }
  @keyframes cts-log-viewer-spin {
    to { transform: rotate(360deg); }
  }
  cts-log-viewer .startBlock {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    background: var(--ink-700);
    color: var(--fg-on-ink);
    padding: var(--space-2) var(--space-3);
    cursor: pointer;
    font-size: var(--fs-13);
    font-weight: var(--fw-bold);
    border: 0;
  }
  cts-log-viewer .startBlock:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
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
 * Polls `/api/log/{testId}` for log entries and renders them via
 * `<cts-log-entry>` children. Supports collapsible blocks and shows a
 * connection-lost banner (a token-styled `cts-alert variant="warning"`)
 * after `FAILURE_THRESHOLD` consecutive fetch failures.
 *
 * Light DOM. Scoped CSS is injected once on first connect; all visual
 * styling routes through OIDF tokens. No Bootstrap `alert-*`, `text-muted`,
 * or `bg-info` markup is emitted; the only Bootstrap-derived class kept is
 * `spinner-border` (re-skinned via scoped CSS) so existing E2E loading
 * checks keep working.
 *
 * @property {string} testId - Test log ID to fetch. Reflects the `test-id`
 *   attribute.
 * @property {boolean} autoScroll - Auto-scroll to the newest entry as rows
 *   arrive. Reflects the `auto-scroll` attribute.
 * @property {object} testInfo - Optional pre-fetched `/api/info` payload.
 *   Stored without further processing; consumers (e.g. log-detail-v2.js)
 *   may use it to coordinate header + viewer state without a second
 *   fetch. Not reflected to an attribute.
 * @fires cts-first-fetch-resolved - Fires once after the viewer's first
 *   successful `/api/log` poll resolves with HTTP 200. Detail:
 *   `{ testId, entriesCount }`. Bubbles. Used by log-detail-v2.js to
 *   defer hash-anchor scroll-to-entry until rows are present in the DOM.
 */
class CtsLogViewer extends LitElement {
  static properties = {
    testId: { type: String, attribute: "test-id" },
    autoScroll: { type: Boolean, attribute: "auto-scroll" },
    testInfo: { type: Object, attribute: false },
    _entries: { state: true },
    _loading: { state: true },
    _collapsedBlocks: { state: true },
    _error: { state: true },
  };

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  constructor() {
    super();
    this.testId = "";
    this.autoScroll = true;
    this.testInfo = null;
    this._entries = [];
    this._loading = true;
    this._collapsedBlocks = new Set();
    this._error = "";
    this._latestTimestamp = 0;
    this._pollTimer = null;
    this._consecutiveFailures = 0;
    // Test hook: stories may override this to run the retry loop fast.
    this._pollIntervalMs = POLL_INTERVAL_MS;
    // Track whether the cts-first-fetch-resolved event has been dispatched
    // already. The event is single-shot — it covers the new-page hash-
    // navigation contract that depends on rows being in the DOM, not a
    // continuous stream of poll resolutions.
    this._firstFetchDispatched = false;
  }

  connectedCallback() {
    super.connectedCallback();
    if (this.testId) this._fetchEntries();
  }

  updated(changedProperties) {
    super.updated(changedProperties);
    // Kick off the first fetch when `testId` is assigned imperatively
    // AFTER connectedCallback fired (the new-page bootstrap pattern in
    // log-detail-v2.js: the viewer is declared statically in HTML, then
    // `viewer.testId = …` is set once URL params are read). The legacy
    // attribute path (`<cts-log-viewer test-id="…">`) is unaffected
    // because that already feeds testId before connectedCallback runs.
    if (
      changedProperties.has("testId") &&
      this.testId &&
      !changedProperties.get("testId") &&
      !this._pollTimer &&
      this.isConnected
    ) {
      this._fetchEntries();
    }
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._pollTimer) {
      clearTimeout(this._pollTimer);
      this._pollTimer = null;
    }
  }

  async _fetchEntries() {
    let succeeded = false;
    let entriesCount = 0;
    try {
      let url = "/api/log/" + encodeURIComponent(this.testId);
      if (this._latestTimestamp > 0) url += "?since=" + this._latestTimestamp;
      const response = await fetch(url);
      if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      const newEntries = await response.json();
      if (newEntries.length > 0) {
        this._entries = [...this._entries, ...newEntries];
        this._latestTimestamp = Math.max(...newEntries.map((e) => e.time || 0));
      }
      this._consecutiveFailures = 0;
      this._error = "";
      succeeded = true;
      entriesCount = this._entries.length;
    } catch (err) {
      this._consecutiveFailures += 1;
      if (this._consecutiveFailures >= FAILURE_THRESHOLD) {
        this._error = "Log connection lost — retrying…";
      }
      console.warn("[cts-log-viewer] /api/log fetch failed:", err);
    } finally {
      this._loading = false;
      // Single-shot event: dispatch only on the first successful resolution.
      // The new log-detail page registers a hash-navigation handler that
      // waits for this event before scrolling to an entry, so rows are
      // guaranteed to be in the DOM by the time the scroll runs.
      if (succeeded && !this._firstFetchDispatched) {
        this._firstFetchDispatched = true;
        this.dispatchEvent(
          new CustomEvent("cts-first-fetch-resolved", {
            bubbles: true,
            detail: { testId: this.testId, entriesCount },
          }),
        );
      }
      // Guard after the in-flight fetch resolves: if the element was removed
      // while we were awaiting, do NOT schedule another poll. Placing the check
      // here (not at the top of _fetchEntries) lets an in-flight cycle finish
      // cleanly without spawning a new one.
      if (this.isConnected) {
        this._pollTimer = setTimeout(() => this._fetchEntries(), this._pollIntervalMs);
      }
    }
  }

  _toggleBlock(blockId) {
    const newSet = new Set(this._collapsedBlocks);
    if (newSet.has(blockId)) newSet.delete(blockId);
    else newSet.add(blockId);
    this._collapsedBlocks = newSet;
  }

  _handleBlockClick(event) {
    const blockId = event.currentTarget.dataset.blockId;
    if (blockId) this._toggleBlock(blockId);
  }

  _renderResultSummary() {
    const counts = {};
    for (const entry of this._entries) {
      if (entry.result) counts[entry.result] = (counts[entry.result] || 0) + 1;
    }
    if (Object.keys(counts).length === 0) return nothing;
    return html` <div class="logResultSummary">${this._renderCountBadges(counts)}</div> `;
  }

  _renderCountBadges(counts) {
    return Object.entries(counts).map(
      ([result, count]) =>
        html`<cts-badge
          variant="${COUNT_BADGE_VARIANTS[result] || "skip"}"
          label="${result} (${count})"
        ></cts-badge>`,
    );
  }

  _renderBlockStart(entry) {
    const isCollapsed = this._collapsedBlocks.has(entry.blockId);
    return html`
      <button
        type="button"
        class="logItem startBlock"
        data-block-id=${entry.blockId}
        @click=${this._handleBlockClick}
      >
        <cts-icon
          name="${isCollapsed ? "chevron-right" : "chevron-down"}"
          aria-hidden="true"
        ></cts-icon>
        ${entry.msg || entry.blockId}
      </button>
    `;
  }

  _renderEntry(entry) {
    if (entry.startBlock) return this._renderBlockStart(entry);
    if (entry.blockId && this._collapsedBlocks.has(entry.blockId)) return nothing;
    return html`<cts-log-entry .entry=${entry}></cts-log-entry>`;
  }

  render() {
    if (this._loading && this._entries.length === 0) {
      return html`<div class="logLoading"
        ><span class="spinner-border" role="status" aria-label="Loading log"></span> Loading
        log…</div
      >`;
    }
    return html`
      ${this._error
        ? html`<cts-alert variant="warning" data-testid="log-viewer-error" aria-live="polite"
            >${this._error}</cts-alert
          >`
        : nothing}
      ${this._renderResultSummary()}
      <div class="logEntries">${this._renderEntries()}</div>
      ${this._entries.length === 0 && !this._error
        ? html`<div class="logEmpty">No log entries</div>`
        : nothing}
    `;
  }

  _renderEntries() {
    return this._entries.map((entry) => this._renderEntry(entry));
  }
}
customElements.define("cts-log-viewer", CtsLogViewer);

export {};
