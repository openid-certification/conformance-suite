import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";
import "./cts-alert.js";
import "./cts-log-entry.js";

const FAILURE_THRESHOLD = 3;
const POLL_INTERVAL_MS = 3000;

/**
 * Format a 1-based ordinal as the canonical `LOG-NNNN` label. Padding to
 * four digits gives 9999 stable references per test before the label
 * widens; modules in this codebase rarely exceed a couple of thousand
 * entries, so the four-digit width holds for the foreseeable future.
 * @param {number} ordinal 1-based position in `_entries`.
 * @returns {string} e.g. `"LOG-0042"`.
 */
function formatReferenceId(ordinal) {
  return `LOG-${String(ordinal).padStart(4, "0")}`;
}

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
 * each block's `.startBlock` header. Lookup table per components/AGENTS.md §7
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
// Result-summary badges arrange in a wrap. The startBlock header labels a
// related run of entries; it is a presentational band (token-driven
// --ink-100 surface), not an interactive control — blocks are not collapsible.
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
  /* Connection-lost banner. No longer sticky — the previous
     'position: sticky; top: var(--status-bar-height)' made the banner
     race with the sticky status bar above it (and on long log pages it
     stayed pinned even after the viewer recovered, distracting from
     the entries the user actually wants to read). The banner now
     scrolls with the content like any other element; the header bar
     above it stays sticky and continues to anchor the page. */
  cts-log-viewer .logViewerErrorBanner {
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
  /* Master grid for horizontal alignment across rows. The widest
     severity badge (e.g. INTERRUPTED) and the widest source/HTTP cell
     in the visible set determine the column width once at the parent;
     each row's .logItem then subgrids into these tracks so the source
     column starts at the same x-position on every row regardless of
     the badge text length. Only kicks in at the wide layout — below
     640px each entry keeps its own two-column stacked layout, where
     per-row alignment is not a concern. */
  cts-log-viewer {
    /* Container scope for the wide-layout master grid below. Moved
       up from cts-log-entry so the parent .logEntries can establish
       a single-level subgrid: each .logItem subgrids directly into
       the parent's 5-track template instead of cascading through two
       host elements (cts-log-entry + .logItem), which Chrome's track
       sizing algorithm fails to propagate intrinsic widths through. */
    container-type: inline-size;
    container-name: ctsLogViewer;
  }
  @container ctsLogViewer (min-width: 640px) {
    cts-log-viewer .logEntries {
      display: grid;
      /* minmax(0, 1fr) on the body track prevents 1fr from eating
         the budget for the auto badge columns. The badge columns use
         'auto' (= minmax(min-content, max-content)) so they size to
         the widest badge across ALL rows that subgrid into this
         parent — that is the alignment the screenshot was missing. */
      grid-template-columns: 92px auto auto minmax(0, 1fr) auto;
      column-gap: var(--space-3);
    }
    /* Top-level cts-log-entry hosts vanish so each .logItem
       participates directly in .logEntries as a single-level
       subgrid descendant — that is what aligns the severity column
       across rows. The host's row separator (1px border-bottom)
       is restored on .logItem in cts-log-entry.js. */
    cts-log-viewer .logEntries > cts-log-entry {
      display: contents;
    }
    /* Entries inside <div class="logBlock"> align with top-level rows
       on the SAME column tracks, via a two-level subgrid relay:
       .logBlock spans all five master columns (grid-column: 1 / -1)
       and is itself a subgrid, so its column lines ARE the master's;
       each nested cts-log-entry host is display: contents so the
       child .logItem becomes a direct grid item of .logBlock and
       subgrids one more level (see cts-log-entry.js's
       .logBlock cts-log-entry .logItem rule). The master grid's auto
       badge tracks then size to the widest content across top-level
       AND block rows, so every row — block or not — shares one set
       of column positions: the message column lines up everywhere.

       This block was previously a <details>, whose UA-generated
       ::details-content wrapper sat between .logBlock and the rows and
       broke the relay (the wrapper defaulted to display: block, so the
       subgrid below it collapsed to a single column). De-collapsing the
       block to a plain <div> removes that wrapper entirely, so the relay
       now propagates with no neutralising hack. */
    cts-log-viewer .logEntries > .logBlock {
      grid-column: 1 / -1;
      display: grid;
      grid-template-columns: subgrid;
      /* Must match .logEntries' column-gap above: a subgrid inherits
         the parent's column TRACKS but not its gap, so the gap is
         restated here. If the master gap changes, change it here too
         or block rows drift out of alignment with top-level rows. */
      column-gap: var(--space-3);
    }
    cts-log-viewer .logEntries > .logBlock > .startBlock {
      grid-column: 1 / -1;
    }
    cts-log-viewer .logEntries > .logBlock > cts-log-entry {
      display: contents;
    }
    /* The filtered-to-nothing empty state is a single child of the grid
       container — span all five master tracks so it centres across the
       full width instead of being squeezed into the 92px first track. */
    cts-log-viewer .logEntries > .logFilterEmpty {
      grid-column: 1 / -1;
    }
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
  /* Scroll offset for the cts-log-toc rail's "Test structure" jumps and
     the document-level cts-scroll-to-entry handler in log-detail.js.
     Without this offset, scrollIntoView lands the target underneath the
     sticky cts-log-detail-header status bar (~70px tall when stuck).
     Applied on .logBlock (the block container the TOC scrolls to) and on
     .logItem (the painted element inside cts-log-entry at the wide
     layout, where the host is display:contents and has no box of its
     own to anchor margin against). */
  cts-log-viewer .logBlock,
  cts-log-viewer cts-log-entry,
  cts-log-viewer cts-log-entry .logItem {
    scroll-margin-top: 70px;
  }
  /* Block-start header rows. Presentational only — a label band for the
     run of entries that follows, not an interactive control. Blocks are
     not collapsible, so there is no disclosure marker, chevron, pointer
     cursor, or focus ring here. */
  cts-log-viewer .startBlock {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    background: var(--ink-100);
    color: var(--ink-800);
    padding: var(--space-2) var(--space-3);
    font-size: var(--fs-13);
    font-weight: var(--fw-bold);
  }
  cts-log-viewer .startBlockMsg {
    flex: 1 1 auto;
    min-width: 0;
    overflow-wrap: anywhere;
  }
  /* Compact ✓N ✗N ⚠N ◆N badge cluster on the right of the block header row.
     tabular-nums keeps the integer portions baseline-aligned across stacked
     block headers so the visual rhythm holds even with mixed digit widths. */
  cts-log-viewer .startBlockCounts {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    flex: 0 0 auto;
    font-variant-numeric: tabular-nums;
  }
  /* Result-summary filter chrome. The count badges (rendered by
     cts-badge) carry their own interactive ring + pressed inversion; the
     rules here style the surrounding affordances: a leading discoverability
     hint, the Clear-filters reset button, the visually-hidden live region,
     and the filtering-active de-emphasis. */
  cts-log-viewer .logResultSummaryHint {
    align-self: center;
    font-size: var(--fs-12);
    color: var(--fg-soft);
    margin-right: var(--space-1);
  }
  /* Clear-filters is a reset action, not a toggle. It is a borderless,
     underlined TEXT button — deliberately not a bordered pill, so it neither
     reads as another filter chip nor fakes an affordance ring with a
     hand-rolled border (per the Badges rule in CLAUDE.md). The underline is
     the affordance; it sits inline in the badge row as a text action. */
  cts-log-viewer .logFilterClear {
    align-self: center;
    font: inherit;
    font-size: var(--fs-12);
    line-height: 16px;
    color: var(--fg-muted);
    background: transparent;
    border: 0;
    padding: 2px var(--space-1);
    cursor: pointer;
    text-decoration: underline;
    text-underline-offset: 2px;
  }
  cts-log-viewer .logFilterClear:hover {
    color: var(--fg);
  }
  cts-log-viewer .logFilterClear:focus-visible {
    outline: 2px solid var(--rust-400, #C75A3F);
    outline-offset: 2px;
  }
  /* Visually-hidden polite live region — announces the active-filter
     description to assistive tech on user-initiated changes only. */
  cts-log-viewer .logFilterAnnounce {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }
  /* While a filter is active, de-emphasize the inactive (unpressed) result
     badges so the active ones stand out. Opacity stays at 0.6 (not lower)
     so the count text inside the still-visible chips remains AA-legible
     against the page. Pressed badges keep full opacity (their inverted
     fill is the active signal). */
  cts-log-viewer.is-filtering .logResultSummary cts-badge:not([pressed]) {
    opacity: 0.6;
  }
  /* Per-block header counts are full-block totals, not the filtered subset.
     Mute them while filtering to signal "this counts the whole block, not
     what you're currently seeing" (R11). */
  cts-log-viewer.is-filtering .startBlockCounts {
    opacity: 0.55;
  }
  /* Empty state shown when an active filter matches zero entries across the
     whole log — keeps the user from being stranded when the top filter row
     has scrolled out of view. */
  cts-log-viewer .logFilterEmpty {
    padding: var(--space-5);
    text-align: center;
    color: var(--fg-soft);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-3);
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
 * `<cts-log-entry>` children. Groups related entries into non-collapsible
 * blocks (a labelled `.startBlock` band + per-block `✓N ✗N ⚠N ◆N` counts)
 * and shows a connection-lost banner (a token-styled `cts-alert
 * variant="warning"`) after `FAILURE_THRESHOLD` consecutive fetch failures.
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
 *   Stored without further processing; consumers (e.g. log-detail.js)
 *   may use it to coordinate header + viewer state without a second
 *   fetch. Not reflected to an attribute.
 * @fires cts-first-fetch-resolved - Fires once after the viewer's first
 *   successful `/api/log` poll resolves with HTTP 200. Detail:
 *   `{ testId, entriesCount }`. Bubbles. Used by log-detail.js to
 *   defer hash-anchor scroll-to-entry until rows are present in the DOM.
 * @fires cts-references-updated - Fires after each successful poll once
 *   the per-entry `LOG-NNNN` reference map has been recomputed. Detail:
 *   `{ testId, references }`, where `references` is an
 *   `Object<entryId, referenceId>` plain object so consumers (e.g. the
 *   page-level cts-failure-summary instances) can render reference chips
 *   without a second walk over `_entries`. Bubbles.
 */
class CtsLogViewer extends LitElement {
  static properties = {
    testId: { type: String, attribute: "test-id" },
    autoScroll: { type: Boolean, attribute: "auto-scroll" },
    testInfo: { type: Object, attribute: false },
    _entries: { state: true },
    _loading: { state: true },
    _error: { state: true },
    _activeFilters: { state: true },
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
    this._error = "";
    /**
     * Active result-type filters as a Set of raw uppercase `result` values
     * (e.g. "FAILURE", "REVIEW"). Empty = show everything (the default and
     * current behavior). Treated immutably (clone-and-reassign so Lit
     * detects the change), mirroring `_collapsedBlocks`. A pure view
     * concern: consulted only in `_renderEntries` and when computing each
     * summary badge's pressed state — the `willUpdate` model recomputations
     * (`_buildReferences` / `_aggregateBlockCounts` / `_collectBlockSummaries`)
     * never read it, so `LOG-NNNN` ordinals, block counts, and the TOC stay
     * computed over the full stream (R8).
     * @type {Set<string>}
     */
    this._activeFilters = new Set();
    // Transient one-shot flag: set true ONLY by user-initiated filter
    // changes (`_toggleFilter` / `clearFilters`) and consumed once by the
    // aria-live announcement in the next render. Polling re-renders never
    // set it, so the live region stays silent on the 3s poll cadence (R6).
    this._announceFilterChange = false;
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
    /**
     * Per-block summary list keyed by `blockId` insertion order, sourced
     * from `_entries`'s `startBlock` rows. Recomputed in willUpdate
     * whenever `_entries` changes; consumed by U8's cts-log-toc rail
     * via `getBlockSummaries()` and the `cts-blocks-updated` event.
     * @type {Array<{ blockId: string, label: string, counts: { success: number, failure: number, warning: number, review: number, info: number, total: number } }>}
     */
    this._blockSummaries = [];
    /**
     * Map of `entry._id` → `LOG-NNNN` for every entry in `_entries`,
     * recomputed in `willUpdate` whenever `_entries` changes. The plain
     * object shape (rather than a Map) lets consumers JSON-stringify it
     * for storybook fixtures and avoids cross-realm `instanceof Map`
     * fragility when the page boundary mounts the viewer.
     * @type {Object.<string, string>}
     */
    this._references = Object.create(null);
    // Tracks whether the initial-load #LOG-NNNN scroll has SUCCEEDED. It
    // flips true only once the target row was found and scrolled — so a
    // hash pointing at an entry that arrives in a later poll keeps being
    // retried on each fetch, then stops re-scrolling the reader once it
    // has landed. (Earlier this flipped true on the first fetch regardless,
    // which is why late-arriving targets never scrolled.)
    this._initialHashScrolled = false;
    // Bound hashchange handler: drives the same scroll routine when the
    // fragment changes after load (e.g. the user clicks an entry's
    // timestamp deep-link). Stored so connected/disconnected add and remove
    // the exact same reference. Defers past any in-flight Lit render (a
    // concurrent poll may be re-rendering) so scrollIntoView measures
    // settled geometry, and marks _initialHashScrolled on success so the
    // post-fetch retry does not re-scroll the reader on the next poll.
    this._onHashChange = () => {
      this.updateComplete.then(() => {
        if (!this.isConnected) return;
        if (this._scrollToHashIfPresent()) this._initialHashScrolled = true;
      });
    };
  }

  connectedCallback() {
    super.connectedCallback();
    if (typeof window !== "undefined") {
      window.addEventListener("hashchange", this._onHashChange);
    }
    if (this.testId) this._fetchEntries();
  }

  /**
   * Recompute the per-block result counts whenever the entries stream
   * changes. Single-pass O(n) walk; with ~5000 entries (a long FAPI2
   * module) this runs once per polling cycle (~3s), which is negligible.
   * Re-running on every `_entries` update keeps the badge totals in sync
   * with streaming additions without a separate subscription.
   *
   * @param {Map<string, unknown>} changedProps - Lit's changed-property map for this update cycle.
   */
  willUpdate(changedProps) {
    if (changedProps.has("_entries")) {
      this._blockCounts = this._aggregateBlockCounts();
      this._references = this._buildReferences();
      this._blockSummaries = this._collectBlockSummaries();
    }
  }

  /**
   * Build the rail-friendly view of `_entries`: one summary per
   * `startBlock` entry, in the order the entries arrived. Each summary
   * carries the `blockId`, the human-readable `label` (the startBlock
   * row's `msg`, falling back to the blockId when the message is
   * missing), and the aggregated `counts` from `_blockCounts`. U8's
   * cts-log-toc consumes this list verbatim — keeping the walk inside
   * the viewer means the rail does not need access to `_entries`.
   * @returns {Array<{ blockId: string, label: string, counts: { success: number, failure: number, warning: number, review: number, info: number, total: number } }>} One summary per startBlock entry, in arrival order.
   */
  _collectBlockSummaries() {
    const summaries = [];
    for (const entry of this._entries) {
      if (!entry.startBlock || !entry.blockId) continue;
      const counts = this._blockCounts.get(entry.blockId) || {
        success: 0,
        failure: 0,
        warning: 0,
        review: 0,
        info: 0,
        total: 0,
      };
      summaries.push({
        blockId: entry.blockId,
        label: entry.msg || entry.blockId,
        counts,
      });
    }
    return summaries;
  }

  /**
   * Public read-only view of the block-summary list. The rail (U8) reads
   * this once on mount and again whenever the viewer dispatches
   * `cts-blocks-updated`. Returns a defensive copy so downstream
   * consumers cannot mutate the cached list and trigger phantom
   * recomputes.
   * @returns {Array<{ blockId: string, label: string, counts: { success: number, failure: number, warning: number, review: number, info: number, total: number } }>} A defensive shallow copy of the cached block summaries.
   */
  getBlockSummaries() {
    return Array.isArray(this._blockSummaries) ? this._blockSummaries.slice() : [];
  }

  /**
   * Build the `entry._id` → `LOG-NNNN` lookup. Indexes every entry in
   * `_entries` (including `startBlock` rows so the ordinal stays stable
   * across the chronological stream — gaps in user-visible chips are
   * intentional, since startBlock rows render as a `.startBlock` header and
   * not as `<cts-log-entry>`). Skips entries without an `_id` defensively.
   *
   * Sibling consumer: `src/main/resources/static/plan-detail.html`
   * mirrors this iteration rule inline (sort-by-time entries, ordinal =
   * index + 1, pad to 4 digits) when it derives the deep-link target
   * for the FAILED-row lozenge. Plan-detail is a classic
   * `<script type="text/javascript">` and cannot ES-import this
   * helper; if the rule changes here, update the shim there too.
   * @returns {Object.<string, string>} A map of entry `_id` to its `LOG-NNNN` reference label.
   */
  _buildReferences() {
    /** @type {Object.<string, string>} */
    const refs = Object.create(null);
    for (let i = 0; i < this._entries.length; i++) {
      const id = this._entries[i] && this._entries[i]._id;
      if (id) refs[id] = formatReferenceId(i + 1);
    }
    return refs;
  }

  /**
   * Public read-only view of the `_id` → `LOG-NNNN` reference map. The
   * page-level cts-failure-summary instances consume this so the
   * failure-row chip and the entry-row chip resolve to the same label
   * without re-walking the entries on each render.
   * @returns {Object.<string, string>} The cached `_id` to `LOG-NNNN` reference map.
   */
  get references() {
    return this._references;
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
   * @returns {Map<string, { success: number, failure: number, warning: number, review: number, info: number, total: number }>} A map from `blockId` to its per-result counts bucket.
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
   * @returns {Map<string, { success: number, failure: number, warning: number, review: number, info: number, total: number }>} The cached per-block aggregation map.
   */
  get blockCounts() {
    return this._blockCounts;
  }

  updated(changedProperties) {
    super.updated(changedProperties);
    // Kick off the first fetch when `testId` is assigned imperatively
    // AFTER connectedCallback fired (the new-page bootstrap pattern in
    // log-detail.js: the viewer is declared statically in HTML, then
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
    // Reflect the filtering state onto the host so scoped CSS can reach
    // BOTH the result-summary badges and the block-header counts (which
    // live in a sibling subtree, .logEntries) from one ancestor selector.
    this.classList.toggle("is-filtering", this._activeFilters.size > 0);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (typeof window !== "undefined") {
      window.removeEventListener("hashchange", this._onHashChange);
    }
    if (this._pollTimer) {
      clearTimeout(this._pollTimer);
      this._pollTimer = null;
    }
  }

  async _fetchEntries() {
    let succeeded = false;
    let entriesCount = 0;
    let appendedAny = false;
    try {
      let url = "/api/log/" + encodeURIComponent(this.testId);
      if (this._latestTimestamp > 0) url += "?since=" + this._latestTimestamp;
      const response = await fetch(url);
      if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      const newEntries = await response.json();
      if (newEntries.length > 0) {
        // U6: warn on out-of-order delivery. The ordinal index is the
        // entry's position in the cumulative buffer (`_entries`), so a
        // poll cycle that delivers an entry whose `time` is older than
        // the latest cached timestamp implies a future page reload may
        // produce a different ordering — and therefore a different
        // LOG-NNNN for the same entry. `startBlock` rows can legitimately
        // arrive out-of-order (they describe a block whose first child
        // already streamed) and are exempt.
        for (const e of newEntries) {
          const time = e && e.time;
          if (
            this._latestTimestamp > 0 &&
            typeof time === "number" &&
            time < this._latestTimestamp &&
            !e.startBlock
          ) {
            console.warn(
              "[cts-log-viewer] out-of-order entry detected — LOG-NNNN may shift across reloads",
              { entryId: e._id, entryTime: time, latestKnown: this._latestTimestamp },
            );
            break;
          }
        }
        this._entries = [...this._entries, ...newEntries];
        this._latestTimestamp = Math.max(...newEntries.map((e) => e.time || 0));
        appendedAny = true;
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
      // U6: dispatch the per-entry reference map after every successful
      // poll that appended rows. Page-level consumers (the failure
      // summary instances) update their `references` prop in response
      // so chip rendering stays in lockstep with the entries stream as
      // it grows. Wait for `updateComplete` so the willUpdate-rebuilt
      // `_references` is the value being shipped; otherwise polling
      // additions lag one cycle behind.
      // U8: emit cts-blocks-updated alongside cts-references-updated so
      // the cts-log-toc rail re-syncs its block list on the same cadence.
      // Same updateComplete gating reason — the willUpdate-rebuilt
      // `_blockSummaries` must be the value shipped to consumers.
      if (succeeded && appendedAny) {
        this.updateComplete.then(() => {
          this.dispatchEvent(
            new CustomEvent("cts-references-updated", {
              bubbles: true,
              detail: { testId: this.testId, references: this._references },
            }),
          );
          this.dispatchEvent(
            new CustomEvent("cts-blocks-updated", {
              bubbles: true,
              detail: { testId: this.testId, blocks: this.getBlockSummaries() },
            }),
          );
        });
      }
      // Initial-load hash navigation — retried after every successful
      // resolution that left rows in the DOM, until the target is actually
      // found and scrolled. `updateComplete` defers the lookup past Lit's
      // pending render so getElementById sees the freshly-committed host
      // elements (which now carry their `id` from the entry template).
      // Marking _initialHashScrolled only on success means a hash pointing
      // at an entry that arrives in a later poll keeps retrying instead of
      // giving up after the first fetch.
      if (succeeded && !this._initialHashScrolled && this._entries.length > 0) {
        this.updateComplete.then(() => {
          // Re-check inside the callback: a second poll resolving (or a
          // hashchange) before this microtask runs may have already
          // scrolled. isConnected guards a disconnect mid-fetch — the
          // continuation is already enqueued when disconnectedCallback
          // fires (mirrors cts-log-entry's loadSpecLinks isConnected guard).
          if (!this.isConnected || this._initialHashScrolled) return;
          if (this._scrollToHashIfPresent()) this._initialHashScrolled = true;
        });
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
   * Scroll the entry named by `window.location.hash` into view. Used both
   * for initial-load navigation and for in-page fragment changes (the
   * `hashchange` listener fires when the user clicks an entry's timestamp
   * deep-link). Honours the entry's `scroll-margin-top` so the row lands
   * below the sticky status bar (U2). No-ops gracefully when the hash is
   * absent, malformed, or points at an out-of-range ordinal (e.g.
   * `#LOG-9999` on a 50-entry test) — in which case it returns `false` so
   * the caller knows the target was not reached and can retry later.
   *
   * Blocks are not collapsible (every entry is always rendered visible), so
   * there is no collapsed ancestor to reveal before scrolling — the target
   * is in the layout the moment it exists in `_entries`.
   * @returns {boolean} `true` when the target was found and scrolled; `false` otherwise.
   */
  _scrollToHashIfPresent() {
    if (typeof window === "undefined") return false;
    const hash = window.location.hash;
    if (!/^#LOG-\d+$/.test(hash)) return false;
    const target = document.getElementById(hash.slice(1));
    if (!target) return false;
    // Honour prefers-reduced-motion: an instant jump avoids both the
    // animation and the multi-frame window during which a concurrent
    // re-render could disturb the smooth scroll.
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    target.scrollIntoView({ behavior: reduceMotion ? "instant" : "smooth", block: "start" });
    return true;
  }

  /**
   * Toggle a result-type filter on/off. Clones `_activeFilters` (immutable
   * update so Lit re-renders), adds or removes the raw uppercase `result`,
   * and marks the change user-initiated so the next render announces it.
   *
   * Focus management: `cts-badge` rebuilds its inner `role="button"` span
   * whenever `?pressed` flips, which detaches the element the user just
   * activated and would drop keyboard focus to `<body>`. After the
   * re-render lands, focus is restored to the same badge (located by its
   * `data-result`) so keyboard toggling stays on the control.
   * @param {string} result Raw uppercase result value, e.g. "FAILURE".
   */
  _toggleFilter(result) {
    const next = new Set(this._activeFilters);
    if (next.has(result)) next.delete(result);
    else next.add(result);
    this._activeFilters = next;
    this._announceFilterChange = true;
    this.updateComplete.then(() => {
      if (!this.isConnected) return;
      const badge = this.querySelector(
        `.logResultSummary cts-badge[data-result="${CSS.escape(result)}"] .badge`,
      );
      if (badge instanceof HTMLElement) badge.focus();
    });
  }

  /**
   * Reset to the unfiltered view. Idempotent — a no-op when no filter is
   * active (so it never spuriously steals focus or announces). Does NOT
   * touch `_collapsedBlocks`: collapsed/open block state is preserved
   * across filter cycles (R11). Public so a future reveal-on-navigate
   * follow-up (and host pages) can clear filters before scrolling to a
   * now-hidden target.
   *
   * Focus management: the Clear button only renders while a filter is
   * active, so clearing removes it from the DOM and would drop focus to
   * `<body>`. After the re-render, focus moves to the first toggle badge
   * in the group.
   */
  clearFilters() {
    if (this._activeFilters.size === 0) return;
    this._activeFilters = new Set();
    this._announceFilterChange = true;
    this.updateComplete.then(() => {
      if (!this.isConnected) return;
      const firstBadge = this.querySelector(".logResultSummary cts-badge[data-result] .badge");
      if (firstBadge instanceof HTMLElement) firstBadge.focus();
    });
  }

  /**
   * `@cts-badge-click` handler for the result-summary toggle badges. Lit
   * binds `this` to the host; the clicked badge carries its result type in
   * `data-result` (read off `currentTarget`), mirroring the cts-log-list
   * filter-chip pattern. A bound method — not an inline arrow — satisfies
   * `lit/no-template-arrow`.
   * @param {Event} event The bubbled `cts-badge-click` event.
   */
  _onResultBadgeClick(event) {
    const target = /** @type {HTMLElement | null} */ (event.currentTarget);
    const result = target && target.dataset.result;
    if (result) this._toggleFilter(result);
  }

  /**
   * Text for the polite live region. Returns content ONLY when a
   * user-initiated change is pending (`_announceFilterChange`), then
   * consumes the flag so a subsequent poll-driven re-render does not
   * re-announce. The phrase deliberately omits the live entry count — the
   * count changes on every 3s poll, the filter description does not, so
   * keying the announcement on the description avoids a screen-reader
   * announcement storm (R6).
   * @returns {string} Announcement text, or "" when nothing to announce.
   */
  _filterAnnouncement() {
    if (!this._announceFilterChange) return "";
    this._announceFilterChange = false;
    if (this._activeFilters.size === 0) return "Filters cleared";
    return `Filtering by ${[...this._activeFilters].join(", ")}`;
  }

  /**
   * Render-time predicate: is this entry visible under the active filter?
   * With no filter active, everything shows (current behavior). Otherwise
   * an entry shows iff its raw `result` is in the active set; result-less
   * structural/HTTP rows are excluded while filtering because
   * `Set.has(undefined)` is `false`.
   * @param {{ result?: string }} entry A log entry.
   * @returns {boolean} Whether the entry should be rendered.
   */
  _entryMatchesFilter(entry) {
    if (this._activeFilters.size === 0) return true;
    // Result-less structural/HTTP rows are excluded while filtering (the
    // `!= null` guard both encodes that and narrows the type for `has`).
    return entry.result != null && this._activeFilters.has(entry.result);
  }

  _renderResultSummary() {
    /** @type {Object.<string, number>} */
    const counts = {};
    for (const entry of this._entries) {
      if (entry.result) counts[entry.result] = (counts[entry.result] || 0) + 1;
    }
    const resultTypes = Object.keys(counts);
    if (resultTypes.length === 0) return nothing;

    // A badge becomes an interactive filter only when there are 2+ result
    // types to choose between — a single-type log has nothing to narrow,
    // so its lone badge stays a read-only label and never produces a no-op
    // toggle into an empty view (R1/R11).
    const filterable = resultTypes.length >= 2;
    const filtering = this._activeFilters.size > 0;

    return html`
      <div
        class="logResultSummary"
        role=${filterable ? "group" : nothing}
        aria-label=${filterable ? "Filter log entries by result" : nothing}
      >
        ${filterable
          ? html`<span class="logResultSummaryHint" aria-hidden="true">Filter by result:</span>`
          : nothing}
        ${this._renderCountBadges(counts, filterable)}
        ${filtering
          ? html`<button type="button" class="logFilterClear" @click=${this.clearFilters}>
              Clear filters
            </button>`
          : nothing}
        ${filterable
          ? html`<span class="logFilterAnnounce" aria-live="polite"
              >${this._filterAnnouncement()}</span
            >`
          : nothing}
      </div>
    `;
  }

  /**
   * Render the result-summary count badges. The count text is always the
   * TRUE total per result type, never a filtered subset (R4). When
   * `filterable`, each badge becomes a multi-select toggle: `clickable`,
   * `?pressed` bound to membership in `_activeFilters` (boolean sigil
   * required — an unsigiled `pressed=${false}` would mount every badge
   * pressed), an action-describing `aria-label`, a `data-result` hook for
   * focus restoration, and a per-badge `@cts-badge-click` listener (the
   * event bubbles to the host where `@` sits, firing once). When not
   * filterable (single result type) the badges render as read-only labels.
   * @param {Object.<string, number>} counts Per-result totals.
   * @param {boolean} filterable Whether the badges are interactive filters.
   * @returns {Array<unknown>} Lit templates for the count badges.
   */
  _renderCountBadges(counts, filterable) {
    return Object.entries(counts).map(([result, count]) => {
      const variant = COUNT_BADGE_VARIANTS[result] || "skip";
      if (!filterable) {
        return html`<cts-badge variant="${variant}" label="${result} (${count})"></cts-badge>`;
      }
      const pressed = this._activeFilters.has(result);
      const ariaLabel = pressed ? `Stop filtering by ${result}` : `Show only ${result} entries`;
      return html`<cts-badge
        variant="${variant}"
        label="${result} (${count})"
        aria-label="${ariaLabel}"
        data-result="${result}"
        clickable
        ?pressed=${pressed}
        @cts-badge-click=${this._onResultBadgeClick}
      ></cts-badge>`;
    });
  }

  /**
   * Render the compact `✓N ✗N ⚠N ◆N` cluster inside a block's header
   * row. INFO is intentionally omitted — a block with 47 INFO and zero
   * problems should read clean. Empty buckets are skipped so a passing
   * block reads as a single `✓N` chip rather than four chips with three
   * zero-counts.
   * @param {{ success: number, failure: number, warning: number, review: number, info: number, total: number } | undefined} counts - The per-block result counts bucket, or undefined when no entries have arrived yet.
   * @returns {unknown} The Lit template for the badge cluster, or `nothing` when the block is empty.
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
   * `<div class="logBlock">` whose header `<div class="startBlock">` carries
   * the block label and the per-block status badges. Blocks are not
   * collapsible — every entry is always rendered. Entries that arrive before
   * any block starts (or have no `blockId`) render as flat siblings —
   * preserves the legacy behaviour for pre-block prefix entries.
   *
   * Each `<cts-log-entry>` host is stamped with `data-entry-id` so the
   * document-level `cts-scroll-to-entry` listener (in
   * `js/log-detail.js`) can locate the target by the same `_id` the
   * failure-summary dispatches in its event detail.
   * @returns {unknown} The Lit template fragments for each block and any
   *   pre-block flat entries (an array), or a single empty-state template
   *   when an active filter matches no entries across the whole log.
   */
  _renderEntries() {
    const filtering = this._activeFilters.size > 0;
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
        // Pre-block flat entries: push whatever survived the filter (none
        // when filtering hides them all — no empty wrapper).
        out.push(...blockChildren);
      } else if (!(filtering && blockChildren.length === 0)) {
        // Emit the block. The guard elides it only while filtering AND no
        // child survived — an all-filtered-out block leaves no empty header.
        // When NOT filtering, an empty block (a streamed startBlock awaiting
        // its first child) still renders its header — see the EmptyBlock story.
        const counts = this._blockCounts.get(currentBlockId);
        const headerText = (blockStart && blockStart.msg) || currentBlockId;
        out.push(html`
          <div class="logBlock" data-block-id=${currentBlockId}>
            <div class="startBlock">
              <span class="startBlockMsg">${headerText}</span>
              <span class="startBlockCounts">${this._renderBlockBadges(counts)}</span>
            </div>
            ${blockChildren}
          </div>
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
      // Filter the rendered stream: while a filter is active, skip entries
      // whose result is not in the active set. The block-grouping logic
      // above runs for every entry (so filtering never merges two blocks);
      // only the push into blockChildren is gated. Result-less structural /
      // HTTP rows are excluded while filtering (Set.has(undefined) is false).
      if (!this._entryMatchesFilter(entry)) continue;
      const referenceId = (entry && entry._id && this._references[entry._id]) || "";
      blockChildren.push(
        // Set the host `id` here (in addition to cts-log-entry's own
        // willUpdate mirror, which covers standalone use) so the fragment
        // target exists the moment the viewer's render commits — before the
        // child entries flush their own update cycle. Without this, the
        // post-fetch hash scroll could run while the child id is still
        // unwritten and silently find nothing.
        html`<cts-log-entry
          .entry=${entry}
          .referenceId=${referenceId}
          .testId=${this.testId}
          data-entry-id=${entry._id}
          id=${referenceId || nothing}
        ></cts-log-entry>`,
      );
    }
    flushBlock();
    // A filter that matches nothing across the whole log: render an inline
    // empty state (with its own Clear affordance) inside .logEntries so the
    // user is not stranded when the top filter row has scrolled away.
    if (filtering && out.length === 0) {
      return html`<div class="logFilterEmpty">
        <span>No entries match the active filters.</span>
        <button type="button" class="logFilterClear" @click=${this.clearFilters}>
          Clear filters
        </button>
      </div>`;
    }
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
