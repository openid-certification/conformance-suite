import { LitElement, html, nothing } from "lit";
import { repeat } from "lit/directives/repeat.js";
import "./cts-badge.js";
import "./cts-log-entry.js";

const FAILURE_THRESHOLD = 3;
const POLL_INTERVAL_MS = 3000;

/**
 * Polls `/api/log/{testId}` for log entries and renders them via
 * `<cts-log-entry>` children. Supports collapsible blocks and shows a
 * connection-lost banner after repeated fetch failures.
 * @property {string} testId - Test log ID to fetch. Reflects the `test-id`
 *   attribute.
 * @property {boolean} autoScroll - Auto-scroll to the newest entry as rows
 *   arrive. Reflects the `auto-scroll` attribute.
 */
class CtsLogViewer extends LitElement {
  static properties = {
    testId: { type: String, attribute: "test-id" },
    autoScroll: { type: Boolean, attribute: "auto-scroll" },
    _entries: { state: true },
    _loading: { state: true },
    _collapsedBlocks: { state: true },
    _error: { state: true },
  };

  createRenderRoot() {
    return this;
  }

  constructor() {
    super();
    this.testId = "";
    this.autoScroll = true;
    this._entries = [];
    this._loading = true;
    this._collapsedBlocks = new Set();
    this._error = "";
    this._latestTimestamp = 0;
    this._pollTimer = null;
    this._consecutiveFailures = 0;
    // Test hook: stories may override this to run the retry loop fast.
    this._pollIntervalMs = POLL_INTERVAL_MS;
  }

  connectedCallback() {
    super.connectedCallback();
    if (this.testId) this._fetchEntries();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._pollTimer) {
      clearTimeout(this._pollTimer);
      this._pollTimer = null;
    }
  }

  async _fetchEntries() {
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
    } catch (err) {
      this._consecutiveFailures += 1;
      if (this._consecutiveFailures >= FAILURE_THRESHOLD) {
        this._error = "Log connection lost — retrying…";
      }
      console.warn("[cts-log-viewer] /api/log fetch failed:", err);
    } finally {
      this._loading = false;
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
    return html`
      <div class="d-flex gap-2 mb-3 flex-wrap">
        ${Object.entries(counts).map(
          ([result, count]) =>
            html`<cts-badge
              variant="${result.toLowerCase()}"
              label="${result} (${count})"
            ></cts-badge>`,
        )}
      </div>
    `;
  }

  _renderBlockStart(entry) {
    const isCollapsed = this._collapsedBlocks.has(entry.blockId);
    return html`
      <div class="row">
        <div
          class="col-md-12 logItem startBlock p-2"
          style="background: #336; color: white; cursor: pointer;"
          data-block-id=${entry.blockId}
          @click=${this._handleBlockClick}
        >
          <span
            class="${isCollapsed ? "bi bi-chevron-right" : "bi bi-chevron-down"}"
            aria-hidden="true"
          ></span>
          ${entry.msg || entry.blockId}
        </div>
      </div>
    `;
  }

  _renderEntry(entry) {
    if (entry.startBlock) return this._renderBlockStart(entry);
    if (entry.blockId && this._collapsedBlocks.has(entry.blockId)) return nothing;
    return html`<cts-log-entry .entry=${entry}></cts-log-entry>`;
  }

  render() {
    if (this._loading && this._entries.length === 0) {
      return html`<div class="text-center p-3"
        ><span class="spinner-border" role="status"></span> Loading log…</div
      >`;
    }
    return html`
      ${this._error
        ? html`<div
            class="alert alert-warning"
            role="status"
            aria-live="polite"
            data-testid="log-viewer-error"
            >${this._error}</div
          >`
        : nothing}
      ${this._renderResultSummary()}
      <div class="log-entries">
        ${repeat(
          this._entries,
          (entry) => entry._id,
          (entry) => this._renderEntry(entry),
        )}
      </div>
      ${this._entries.length === 0 && !this._error
        ? html`<div class="text-muted text-center p-3">No log entries</div>`
        : nothing}
    `;
  }
}
customElements.define("cts-log-viewer", CtsLogViewer);
