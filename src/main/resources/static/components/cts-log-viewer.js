import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";

class CtsLogViewer extends LitElement {
  static properties = {
    testId: { type: String, attribute: "test-id" },
    autoScroll: { type: Boolean, attribute: "auto-scroll" },
    _entries: { state: true },
    _loading: { state: true },
    _collapsedBlocks: { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this.testId = "";
    this.autoScroll = true;
    this._entries = [];
    this._loading = true;
    this._collapsedBlocks = new Set();
    this._latestTimestamp = 0;
    this._pollTimer = null;
  }

  connectedCallback() {
    super.connectedCallback();
    if (this.testId) this._fetchEntries();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._pollTimer) clearTimeout(this._pollTimer);
  }

  async _fetchEntries() {
    try {
      let url = "/api/log/" + encodeURIComponent(this.testId);
      if (this._latestTimestamp > 0) url += "?since=" + this._latestTimestamp;
      const response = await fetch(url);
      if (!response.ok) throw new Error("Failed to fetch log");
      const newEntries = await response.json();
      if (newEntries.length > 0) {
        this._entries = [...this._entries, ...newEntries];
        this._latestTimestamp = Math.max(...newEntries.map((e) => e.time || 0));
      }
    } catch {
      // Log fetch errors are non-fatal -- the viewer retries on next poll
    } finally {
      this._loading = false;
      this._pollTimer = setTimeout(() => this._fetchEntries(), 3000);
    }
  }

  _toggleBlock(blockId) {
    const newSet = new Set(this._collapsedBlocks);
    if (newSet.has(blockId)) newSet.delete(blockId);
    else newSet.add(blockId);
    this._collapsedBlocks = newSet;
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
          ([result, count]) => html`<cts-badge variant="${result.toLowerCase()}" label="${result} (${count})"></cts-badge>`,
        )}
      </div>
    `;
  }

  _renderBlockStart(entry) {
    const isCollapsed = this._collapsedBlocks.has(entry.blockId);
    return html`
      <div class="row">
        <div class="col-md-12 logItem startBlock p-2"
          style="background: #336; color: white; cursor: pointer;"
          @click=${() => this._toggleBlock(entry.blockId)}>
          <span class="${isCollapsed ? "bi bi-chevron-right" : "bi bi-chevron-down"}" aria-hidden="true"></span>
          ${entry.msg || entry.blockId}
        </div>
      </div>
    `;
  }

  _renderEntry(entry) {
    if (entry.startBlock) return this._renderBlockStart(entry);
    if (entry.blockId && this._collapsedBlocks.has(entry.blockId)) return nothing;
    return html`
      <div class="row">
        <div class="col-md-12 logItem p-1" style=${entry.blockId ? "border-left: 3px solid #336" : ""}>
          <div class="row">
            <div class="col-md-1">
              ${entry.time ? html`<small class="text-muted">${new Date(entry.time).toLocaleTimeString()}</small>` : nothing}
            </div>
            <div class="col-md-2 labelCollection">
              ${entry.result ? html`<cts-badge variant="${entry.result.toLowerCase()}" label="${entry.result}"></cts-badge>` : nothing}
            </div>
            <div class="col-md-9">
              ${entry.src ? html`<small class="text-muted me-2">${entry.src}</small>` : nothing}
              ${entry.msg ? html`<span>${entry.msg}</span>` : nothing}
            </div>
          </div>
        </div>
      </div>
    `;
  }

  render() {
    if (this._loading && this._entries.length === 0) {
      return html`<div class="text-center p-3"><span class="spinner-border" role="status"></span> Loading log…</div>`;
    }
    return html`
      ${this._renderResultSummary()}
      <div class="log-entries">${this._entries.map((entry) => this._renderEntry(entry))}</div>
      ${this._entries.length === 0 ? html`<div class="text-muted text-center p-3">No log entries</div>` : nothing}
    `;
  }
}
customElements.define("cts-log-viewer", CtsLogViewer);
