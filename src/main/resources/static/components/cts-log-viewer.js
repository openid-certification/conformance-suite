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

/**
 * Symbol + variant + count for the per-block status badges rendered inside
 * each block's `<summary>`. Lookup table per components/AGENTS.md §7
 * (no dynamic class concatenation). INFO is intentionally absent — a block
 * with 47 INFO entries and zero problems should read clean, not noisy.
 * Keys mirror lowercase `result` values produced by `_aggregateBlockCounts`.
 * @type {Array<{ key: string, symbol: string, variant: string }>}
 */
const BLOCK_BADGE_SPECS = [
  { key: "success", symbol: "✓", variant: "pass" },
  { key: "failure", symbol: "✗", variant: "fail" },
  { key: "warning", symbol: "⚠", variant: "warn" },
  { key: "review", symbol: "◆", variant: "review" },
];

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
  /* Connection-lost banner stacks below the cts-log-detail-header sticky
     status bar (z-index 10) at top: var(--status-bar-height). The status
     bar publishes its measured height to document.documentElement; pages
     that do not mount the header fall back to the 0px default in
     oidf-tokens.css, leaving the banner pinned to the top of the page.
     Mirrors the bar sticky-only-at-tablet+ behaviour: on small viewports
     the banner is static and scrolls with the content. */
  cts-log-viewer .logViewerErrorBanner {
    margin-bottom: var(--space-3);
  }
  @media (min-width: 640px) {
    cts-log-viewer .logViewerErrorBanner {
      position: sticky;
      top: var(--status-bar-height);
      z-index: 9;
    }
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
  /* Block-start <summary> rows. Native <details>/<summary> gives us
     keyboard collapse semantics for free; we strip the default disclosure
     marker because the chevron icon already carries the affordance. */
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
    list-style: none;
  }
  cts-log-viewer .startBlock::-webkit-details-marker { display: none; }
  cts-log-viewer .startBlock::marker { content: ""; }
  cts-log-viewer .startBlock:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-log-viewer .startBlockMsg {
    flex: 1 1 auto;
    min-width: 0;
    overflow-wrap: anywhere;
  }
  /* Compact ✓N ✗N ⚠N ◆N badge cluster on the right of the summary row.
     tabular-nums keeps the integer portions baseline-aligned across stacked
     block headers so the visual rhythm holds even with mixed digit widths. */
  cts-log-viewer .startBlockCounts {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    flex: 0 0 auto;
    font-variant-numeric: tabular-nums;
  }
  /* CSS-only chevron swap: a single <cts-icon name="chevron-down"> rotates
     -90deg when the <details> is closed. This stays correct under any
     toggle path (mouse, keyboard, programmatic open=true) because it reads
     the [open] attribute directly — no Lit re-render race. */
  cts-log-viewer .logBlock > .startBlock cts-icon {
    transition: transform 120ms ease-in-out;
  }
  cts-log-viewer .logBlock:not([open]) > .startBlock cts-icon {
    transform: rotate(-90deg);
  }
  /* The legacy implementation hid collapsed children with a CSS rule keyed
     on _collapsedBlocks. With <details>, the browser does that for us via
     the open/closed state; no extra rule needed. */
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
    /**
     * Per-block result-count cache keyed by `blockId`. Recomputed in
     * willUpdate whenever `_entries` changes; consumed by
     * `_renderBlockBadges` and exposed via the `blockCounts` getter so
     * U8's TOC rail can read the same data without a second walk.
     * @type {Map<string, { success: number, failure: number, warning: number, review: number, info: number, total: number }>}
     */
    this._blockCounts = new Map();
  }

  connectedCallback() {
    super.connectedCallback();
    if (this.testId) this._fetchEntries();
  }

  /**
   * Recompute the per-block result counts whenever the entries stream
   * changes. Single-pass O(n) walk; with ~5000 entries (a long FAPI2
   * module) this runs once per polling cycle (~3s), which is negligible.
   * Re-running on every `_entries` update keeps the badge totals in sync
   * with streaming additions without a separate subscription.
   *
   * @param {Map<string, unknown>} changedProps
   */
  willUpdate(changedProps) {
    if (changedProps.has("_entries")) {
      this._blockCounts = this._aggregateBlockCounts();
    }
  }

  /**
   * Walk `_entries` once and bucket each entry's `result` under its
   * `blockId`. Trusts the backend's chronological-with-`blockId`
   * contract: a `startBlock` entry seeds an empty bucket; subsequent
   * entries with the same `blockId` increment that bucket. Defensive
   * guard: child entries whose `blockId` has no corresponding
   * `startBlock` (rare cross-poll-cycle race) are silently dropped from
   * aggregation; the next poll catches up. The user-visible impact is a
   * brief under-count in the badges; the UI never crashes.
   *
   * @returns {Map<string, { success: number, failure: number, warning: number, review: number, info: number, total: number }>}
   */
  _aggregateBlockCounts() {
    /** @type {Map<string, { success: number, failure: number, warning: number, review: number, info: number, total: number }>} */
    const counts = new Map();
    for (const entry of this._entries) {
      if (entry.startBlock) {
        if (!counts.has(entry.blockId)) {
          counts.set(entry.blockId, {
            success: 0,
            failure: 0,
            warning: 0,
            review: 0,
            info: 0,
            total: 0,
          });
        }
        continue;
      }
      if (!entry.blockId) continue;
      const bucket = counts.get(entry.blockId);
      if (!bucket) continue;
      const result = (entry.result || "").toLowerCase();
      if (result === "success") bucket.success += 1;
      else if (result === "failure") bucket.failure += 1;
      else if (result === "warning") bucket.warning += 1;
      else if (result === "review") bucket.review += 1;
      else if (result === "info") bucket.info += 1;
      bucket.total += 1;
    }
    return counts;
  }

  /**
   * Public read-only view of the per-block aggregation map. U8's TOC
   * rail consumes this via the host element so the rail and the inline
   * block badges stay in lockstep without duplicating the walk.
   * @returns {Map<string, { success: number, failure: number, warning: number, review: number, info: number, total: number }>}
   */
  get blockCounts() {
    return this._blockCounts;
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

  /**
   * `<details>` toggle handler. Mirrors the element's `open` state into
   * `_collapsedBlocks` so collapse choices survive a polling-driven
   * re-render. Reading `event.currentTarget.open` (the post-toggle
   * value) is the source of truth — Lit's `?open=` binding restores
   * this on the next render.
   * @param {Event} event
   */
  _handleBlockToggle(event) {
    const target = /** @type {HTMLDetailsElement & { dataset: DOMStringMap }} */ (
      event.currentTarget
    );
    const blockId = target.dataset.blockId;
    if (!blockId) return;
    const newSet = new Set(this._collapsedBlocks);
    if (target.open) newSet.delete(blockId);
    else newSet.add(blockId);
    this._collapsedBlocks = newSet;
  }

  _renderResultSummary() {
    /** @type {Object.<string, number>} */
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

  /**
   * Render the compact `✓N ✗N ⚠N ◆N` cluster inside a block's summary
   * row. INFO is intentionally omitted — a block with 47 INFO and zero
   * problems should read clean. Empty buckets are skipped so a passing
   * block reads as a single `✓N` chip rather than four chips with three
   * zero-counts.
   * @param {{ success: number, failure: number, warning: number, review: number, info: number, total: number } | undefined} counts
   */
  _renderBlockBadges(counts) {
    if (!counts || counts.total === 0) return nothing;
    return BLOCK_BADGE_SPECS.map(({ key, symbol, variant }) => {
      const n = counts[key];
      if (!n) return nothing;
      return html`<cts-badge variant="${variant}" label="${symbol}${n}"></cts-badge>`;
    });
  }

  /**
   * Group entries by the block they belong to and render each block as a
   * `<details>` element with a `<summary>` carrying the block label and
   * the per-block status badges. Entries that arrive before any block
   * starts (or have no `blockId`) render as flat siblings — preserves
   * the legacy behaviour for pre-block prefix entries.
   *
   * Each `<cts-log-entry>` host is stamped with `data-entry-id` so the
   * document-level `cts-scroll-to-entry` listener (in
   * `js/log-detail-v2.js`) can locate the target by the same `_id` the
   * failure-summary dispatches in its event detail.
   */
  _renderEntries() {
    /** @type {Array<unknown>} */
    const out = [];
    /** @type {string | null} */
    let currentBlockId = null;
    /** @type {Array<unknown>} */
    let blockChildren = [];
    /** @type {{ msg?: string, blockId: string } | null} */
    let blockStart = null;

    const flushBlock = () => {
      if (currentBlockId === null) {
        out.push(...blockChildren);
      } else {
        const counts = this._blockCounts.get(currentBlockId);
        const isCollapsed = this._collapsedBlocks.has(currentBlockId);
        const headerText = (blockStart && blockStart.msg) || currentBlockId;
        out.push(html`
          <details
            class="logBlock"
            data-block-id=${currentBlockId}
            ?open=${!isCollapsed}
            @toggle=${this._handleBlockToggle}
          >
            <summary class="logItem startBlock">
              <cts-icon name="chevron-down" aria-hidden="true"></cts-icon>
              <span class="startBlockMsg">${headerText}</span>
              <span class="startBlockCounts">${this._renderBlockBadges(counts)}</span>
            </summary>
            ${blockChildren}
          </details>
        `);
      }
      blockChildren = [];
      blockStart = null;
    };

    for (const entry of this._entries) {
      if (entry.startBlock) {
        flushBlock();
        currentBlockId = entry.blockId || null;
        blockStart = { msg: entry.msg, blockId: entry.blockId };
        continue;
      }
      const entryBlockId = entry.blockId || null;
      if (entryBlockId !== currentBlockId) {
        flushBlock();
        currentBlockId = entryBlockId;
      }
      blockChildren.push(
        html`<cts-log-entry .entry=${entry} data-entry-id=${entry._id}></cts-log-entry>`,
      );
    }
    flushBlock();
    return out;
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
        ? html`<div class="logViewerErrorBanner">
            <cts-alert variant="warning" data-testid="log-viewer-error" aria-live="polite"
              >${this._error}</cts-alert
            >
          </div>`
        : nothing}
      ${this._renderResultSummary()}
      <div class="logEntries">${this._renderEntries()}</div>
      ${this._entries.length === 0 && !this._error
        ? html`<div class="logEmpty">No log entries</div>`
        : nothing}
    `;
  }
}
customElements.define("cts-log-viewer", CtsLogViewer);

export {};
