import { LitElement, html, nothing } from "lit";
import { repeat } from "lit/directives/repeat.js";
import "./cts-icon.js";
import "./cts-empty-state.js";
import "./cts-button.js";
import "./cts-badge.js";
import "./cts-form-field.js";

/**
 * Native Lit replacement for jQuery DataTables. Drives plans.html,
 * logs.html, and tokens.html after Phase D. Supports both server-side
 * (DataTables-style envelope) and client-side data sources, sticky
 * headers, sortable columns, paginated views, and a search box whose
 * timing model is mode-driven (button-driven by default to mirror the
 * existing plans/logs UX).
 *
 * Light DOM, scoped CSS injected once into `<head>`. The
 * `:not(:defined)` block-level fallback is declared in `css/layout.css`
 * (Coherence F4 — the host reserves ~200px so the page chrome does not
 * jump on first fetch).
 *
 * Cell-rendering decision tree (in priority order):
 *   1. `column.format` of `date` / `mono` / `badge` is handled inline.
 *   2. `column.render` (e.g. `"actions"`) looks up a child
 *      `<template slot="cell-actions">…</template>` whose contents are
 *      cloned per row and rendered inside the cell. Strings inside the
 *      template are passed through `${row.<key>}` substitution at render
 *      time, replicating the Mustache `{{key}}` shape used today.
 *   3. `cellRenderer(row, columnKey, columnDescriptor)` callback runs
 *      when both 1 and 2 are absent. The callback may return a string,
 *      `HTMLElement`, or Lit `TemplateResult` — covering the dynamic
 *      Mustache template usage in `plansListing` / `logsListing` /
 *      `tokensListing` today.
 *
 * Server-side mode (the default) issues a debounced (250ms) `fetch()`
 * call on every search, sort, or page change with a DataTables-style
 * envelope. Out-of-order responses are dropped by tracking the
 * monotonic `_draw` counter — only responses whose `draw` matches the
 * latest request paint the table.
 *
 * @property {Array<object>} columns - Column descriptors. Each entry:
 *   `{ key, label, sortable?, format?, render?, visible?, mono? }`. The
 *   `key` is read with optional dot-path navigation for nested data
 *   (e.g. `owner.sub`). `label` is the header cell text. `format` is
 *   one of `date` / `mono` / `badge`. `render` is the slot-by-name
 *   template id (e.g. `"actions"` looks up `<template slot="cell-actions">`).
 *   `visible: false` hides the column entirely. `mono: true` renders
 *   cells in `var(--font-mono)`.
 * @property {Array<object>} rows - Client-side data source. Used only
 *   when `serverSide=false`. Ignored otherwise.
 * @property {number} pageSize - Page length (defaults to 10).
 * @property {boolean} serverSide - When true (the default), the
 *   component fetches `ajaxUrl` with DataTables-style query params and
 *   expects the standard envelope. When false, the component renders
 *   `rows` directly with client-side filter / sort / paginate.
 * @property {string} ajaxUrl - Required when `serverSide=true`. The
 *   request URL is built by appending `draw`, `start`, `length`,
 *   `search`, and `order` query params.
 * @property {string} requestShape - One of `datatables-default` or
 *   `datatables-comma-order`. Defaults to `datatables-comma-order`,
 *   matching the `order=col,dir` shape that `/api/plan` and `/api/log`
 *   accept today. The `datatables-default` shape uses the verbose
 *   `order[0][column]=N&order[0][dir]=D&columns[N][data]=KEY` form.
 * @property {string} searchMode - One of `live-debounced` or
 *   `explicit`. Defaults to `explicit` — search fires on Enter or
 *   inline-submit click only (matching plans/logs today). `live-debounced`
 *   re-fetches on every keystroke after the 250ms debounce. Across both
 *   modes, `Escape` clears the input and commits the empty search, and
 *   the leading × button does the same. After a non-empty search is
 *   committed, an active-filter chip is rendered below the input
 *   showing the live query + filtered count, with a "Show all" reset.
 * @property {object} initialSort - `{ column, direction }` to seed
 *   the sort state on first mount. `direction` is `asc` or `desc`.
 * @property {Function} cellRenderer - Optional `(row, columnKey,
 *   columnDescriptor) => string | HTMLElement | TemplateResult`.
 *   Wired imperatively after `customElements.whenDefined("cts-data-table")`
 *   resolves (the function shape is JSON-incompatible). Runs only
 *   when `column.format` and `column.render` are both absent.
 * @property {Function} rowClass - Optional `(row) => string` that
 *   produces an extra class name per row, mirroring the legacy
 *   DataTables `createdRow` class additions.
 * @property {string} searchPlaceholder - Placeholder text for the
 *   search input. Empty string suppresses the search row entirely.
 * @property {string} emptyState - Heading text for the empty state
 *   when filtered rows = 0. Composes `<cts-empty-state>` underneath.
 * @property {boolean} loading - Read-only. True while a fetch is in
 *   flight (server-side mode only). Surfaces as a loading row.
 * @fires cts-row-click - When a row is clicked, with
 *   `{ detail: { row, index, rowEl } }`.
 * @fires cts-page-change - When pagination advances, with
 *   `{ detail: { draw, start, length } }`.
 * @fires cts-sort-change - When a sortable header is clicked, with
 *   `{ detail: { columnKey, direction } }`.
 * @fires cts-row-rendered - Once per row after each draw, with
 *   `{ detail: { row, index, rowEl } }`. Mirrors the legacy
 *   `createdRow` callback so consumers can wire per-row imperative
 *   behaviour without owning the render pipeline.
 * @fires cts-draw-complete - Once per draw after every row mounts,
 *   with `{ detail: { rows, rowEls } }`. Mirrors the legacy
 *   `drawCallback` (used by `fetchTestResults` and `activeTooltip`).
 * @fires cts-data-loaded - After a successful fetch (server-side) or
 *   first render (client-side), with `{ detail: { recordsTotal,
 *   recordsFiltered } }`. The `xhr.dt` analog.
 * @fires cts-data-error - On fetch failure, with `{ detail: { error } }`.
 *   The component also renders an inline error message inside the
 *   table area; the event lets consumers escalate (toast, banner) on
 *   top of that.
 */

const STYLE_ID = "cts-data-table-styles";

const STYLE_TEXT = `
  cts-data-table {
    display: block;
    font-family: var(--font-sans);
  }
  cts-data-table .oidf-dt-search {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin-bottom: var(--space-4);
  }
  cts-data-table .oidf-dt-search-row {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }
  cts-data-table .oidf-dt-search-input-wrap {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    padding: 0 var(--space-2);
    height: 34px;
    cursor: text;
    border: 1px solid var(--ink-300);
    border-radius: var(--radius-2);
    background: var(--bg-elev);
    flex: 1;
    max-width: 480px;
    transition: border-color var(--dur-1) var(--ease-standard),
      box-shadow var(--dur-1) var(--ease-standard);
  }
  cts-data-table .oidf-dt-search-input-wrap:hover {
    border-color: var(--ink-400);
  }
  cts-data-table .oidf-dt-search-input-wrap:focus-within {
    box-shadow: var(--focus-ring);
    border-color: var(--orange-400);
  }
  cts-data-table .oidf-dt-search-leading {
    display: inline-flex;
    align-items: center;
    color: var(--ink-400);
    line-height: 1;
    flex-shrink: 0;
  }
  cts-data-table .oidf-dt-search-leading i {
    font-size: 14px;
    line-height: 1;
  }
  cts-data-table .oidf-dt-search-input {
    flex: 1;
    min-width: 0;
    border: 0;
    background: transparent;
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    line-height: 1rem;
    color: var(--fg);
    outline: none;
    padding: 0;
    /* Reset legacy layout.css text-indent so the search caret sits flush
       against the leading cts-icon glyph instead of being pushed 5px right. */
    text-indent: 0;
  }
  cts-data-table .oidf-dt-search-input::placeholder {
    color: var(--fg-faint);
  }
  cts-data-table .oidf-dt-search-input::-webkit-search-cancel-button,
  cts-data-table .oidf-dt-search-input::-webkit-search-decoration {
    -webkit-appearance: none;
    appearance: none;
  }
  cts-data-table .oidf-dt-search-clear,
  cts-data-table .oidf-dt-search-submit {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    height: 24px;
    width: 24px;
    padding: 0;
    border: 0;
    background: transparent;
    color: var(--fg-soft);
    border-radius: var(--radius-2);
    cursor: pointer;
    transition: background var(--dur-1) var(--ease-standard),
      color var(--dur-1) var(--ease-standard),
      opacity var(--dur-1) var(--ease-standard);
  }
  cts-data-table .oidf-dt-search-clear:hover,
  cts-data-table .oidf-dt-search-submit:hover {
    background: var(--ink-100);
    color: var(--fg);
  }
  cts-data-table .oidf-dt-search-clear:focus-visible,
  cts-data-table .oidf-dt-search-submit:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-data-table .oidf-dt-search-clear cts-icon svg {
    width: var(--space-3);
    height: var(--space-3);
  }
  /* When both action buttons are visible, pull the submit slightly closer
     to the clear so the pair reads as one action cluster, distinct from
     the gap between the input and the cluster. */
  cts-data-table .oidf-dt-search-clear + .oidf-dt-search-submit {
    margin-left: calc(var(--space-1) - var(--space-2));
  }
  cts-data-table .oidf-dt-search-submit {
    color: var(--orange-500);
  }
  cts-data-table .oidf-dt-search-filter {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    align-self: flex-start;
    max-width: 100%;
    padding: 4px var(--space-2) 4px var(--space-3);
    background: var(--orange-50);
    border: 1px solid var(--orange-100);
    border-radius: var(--radius-pill);
    font-size: var(--fs-12);
    color: var(--ink-800);
    line-height: 1.3;
  }
  cts-data-table .oidf-dt-search-filter-label {
    color: var(--fg-soft);
  }
  cts-data-table .oidf-dt-search-filter-query {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    color: var(--ink-900);
    max-width: 220px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  cts-data-table .oidf-dt-search-filter-count {
    color: var(--fg-soft);
  }
  cts-data-table .oidf-dt-search-filter-reset {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    padding: 2px 8px;
    background: transparent;
    border: 0;
    border-radius: var(--radius-pill);
    color: var(--orange-600);
    font-size: var(--fs-12);
    font-weight: var(--fw-medium);
    cursor: pointer;
    transition: background var(--dur-1) var(--ease-standard),
      color var(--dur-1) var(--ease-standard);
  }
  cts-data-table .oidf-dt-search-filter-reset:hover {
    background: var(--orange-100);
    color: var(--orange-700);
  }
  cts-data-table .oidf-dt-search-filter-reset:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-data-table .oidf-dt-search-filter-reset cts-icon svg {
    width: var(--space-3);
    height: var(--space-3);
  }
  cts-data-table .oidf-dt-table-wrap {
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    overflow: auto;
    position: relative;
  }
  cts-data-table .oidf-dt-table {
    width: 100%;
    border-collapse: collapse;
    font-size: var(--fs-13);
  }
  cts-data-table .oidf-dt-table th {
    position: sticky;
    top: 0;
    text-align: left;
    padding: 10px 14px;
    background: var(--ink-50);
    border-bottom: 1px solid var(--border);
    font-size: var(--fs-12);
    font-weight: var(--fw-medium);
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: var(--fg-soft);
    z-index: 1;
    white-space: nowrap;
  }
  cts-data-table .oidf-dt-th-inner {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    line-height: 1;
  }
  cts-data-table .oidf-dt-table th.is-sortable {
    cursor: pointer;
    user-select: none;
  }
  cts-data-table .oidf-dt-table th.is-sortable:hover {
    color: var(--fg);
  }
  cts-data-table .oidf-dt-table th.is-sortable:hover
    .oidf-dt-sort-arrow:not(.is-active) {
    color: var(--ink-500);
  }
  cts-data-table .oidf-dt-sort-arrow {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    flex-shrink: 0;
    color: var(--ink-300);
    transition: color var(--dur-1) var(--ease-standard);
  }
  cts-data-table .oidf-dt-sort-arrow.is-active {
    color: var(--orange-500);
  }
  cts-data-table .oidf-dt-table td {
    padding: 12px 14px;
    border-bottom: 1px solid var(--ink-100);
    vertical-align: middle;
    color: var(--fg);
  }
  cts-data-table .oidf-dt-table tbody tr {
    cursor: default;
  }
  cts-data-table .oidf-dt-table tbody tr:hover td {
    background: var(--ink-50);
  }
  cts-data-table .oidf-dt-table tbody tr:last-child td {
    border-bottom: 0;
  }
  cts-data-table .oidf-dt-cell-mono {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
  }
  cts-data-table .oidf-dt-loading,
  cts-data-table .oidf-dt-error {
    padding: var(--space-5);
    text-align: center;
    color: var(--fg-soft);
  }
  cts-data-table .oidf-dt-error {
    color: var(--danger-fg, #b00020);
  }
  cts-data-table .oidf-dt-pager {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
    padding: var(--space-3) 0 0;
    font-size: var(--fs-13);
    color: var(--fg-soft);
  }
  cts-data-table .oidf-dt-pager-controls {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }
`;

function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/** @type {Object<string, string>} Lookup table from format name → CSS class for the cell wrapper. */
const FORMAT_CELL_CLASSES = {
  mono: "oidf-dt-cell-mono",
  date: "",
  badge: "",
};

/**
 * Resolve a (possibly dotted) key against a row.
 *
 * @param {object} row
 * @param {string} key
 * @returns {*}
 */
function readKey(row, key) {
  if (!row || !key) return undefined;
  if (Object.prototype.hasOwnProperty.call(row, key)) return row[key];
  if (key.indexOf(".") === -1) return undefined;
  return key.split(".").reduce((acc, part) => (acc == null ? acc : acc[part]), row);
}

function formatDate(value) {
  if (value == null || value === "") return "";
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  const pad = (n) => String(n).padStart(2, "0");
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    ` ${pad(date.getHours())}:${pad(date.getMinutes())}`
  );
}

class CtsDataTable extends LitElement {
  static properties = {
    columns: { type: Array },
    rows: { type: Array },
    pageSize: { type: Number, attribute: "page-size" },
    serverSide: { type: Boolean, attribute: "server-side" },
    ajaxUrl: { type: String, attribute: "ajax-url" },
    requestShape: { type: String, attribute: "request-shape" },
    searchMode: { type: String, attribute: "search-mode" },
    initialSort: { type: Object },
    cellRenderer: { type: Object, attribute: false },
    rowClass: { type: Object, attribute: false },
    searchPlaceholder: { type: String, attribute: "search-placeholder" },
    emptyState: { type: String, attribute: "empty-state" },
    loading: { type: Boolean, reflect: true },
    _currentPage: { state: true },
    _search: { state: true },
    _searchInput: { state: true },
    _sortColumn: { state: true },
    _sortDir: { state: true },
    _rowsView: { state: true },
    _totalRows: { state: true },
    _filteredRows: { state: true },
    _error: { state: true },
  };

  constructor() {
    super();
    /** @type {Array<object>} */
    this.columns = [];
    /** @type {Array<object>} */
    this.rows = [];
    this.pageSize = 10;
    this.serverSide = true;
    this.ajaxUrl = "";
    this.requestShape = "datatables-comma-order";
    this.searchMode = "explicit";
    /** @type {{ column: string, direction: string } | null} */
    this.initialSort = null;
    /** @type {Function | null} */
    this.cellRenderer = null;
    /** @type {Function | null} */
    this.rowClass = null;
    this.searchPlaceholder = "";
    this.emptyState = "No results";
    this.loading = false;

    this._currentPage = 0;
    this._search = "";
    this._searchInput = "";
    this._sortColumn = "";
    this._sortDir = "";
    this._rowsView = [];
    this._totalRows = 0;
    this._filteredRows = 0;
    this._error = null;

    // Monotonically increasing draw counter for out-of-order suppression.
    this._draw = 0;
    /** @type {ReturnType<typeof setTimeout> | null} */
    this._debounceTimer = null;
    /** @type {AbortController | null} */
    this._inflightController = null;
  }

  createRenderRoot() {
    injectStyles();
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    if (this.initialSort && this.initialSort.column) {
      this._sortColumn = this.initialSort.column;
      this._sortDir = this.initialSort.direction === "desc" ? "desc" : "asc";
    }
    if (this.serverSide) {
      // Defer the first fetch so consumers have a chance to wire up
      // imperative properties (cellRenderer, rowClass, columns) after
      // the element connects but before the first network round-trip.
      queueMicrotask(() => {
        if (this.isConnected) this.reload();
      });
    } else {
      this._refreshClientView();
    }
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._debounceTimer) {
      clearTimeout(this._debounceTimer);
      this._debounceTimer = null;
    }
    if (this._inflightController) {
      this._inflightController.abort();
      this._inflightController = null;
    }
  }

  updated(changed) {
    super.updated(changed);
    // Client-side: when rows / sort / search / page changes, recompute view.
    if (
      !this.serverSide &&
      (changed.has("rows") ||
        changed.has("_search") ||
        changed.has("_sortColumn") ||
        changed.has("_sortDir") ||
        changed.has("_currentPage"))
    ) {
      this._refreshClientView();
    }
    // Per-row mount + post-draw lifecycle events. Always runs after
    // a render that touched the visible rows.
    if (changed.has("_rowsView")) {
      this._dispatchDrawEvents();
    }
  }

  // ------------------------------------------------------------------
  // Public API
  // ------------------------------------------------------------------

  /**
   * Force an AJAX refetch (server-side mode). No-op in client-side mode.
   */
  reload() {
    if (!this.serverSide) {
      this._refreshClientView();
      return;
    }
    this._fetchPage();
  }

  /** Alias for reload(); preserved because tokens.html uses `.refresh()`. */
  refresh() {
    this.reload();
  }

  /**
   * Programmatic search trigger; mirrors `api.search(q).draw()`.
   *
   * @param {string} query
   */
  search(query) {
    this._search = query == null ? "" : String(query);
    this._searchInput = this._search;
    this._currentPage = 0;
    if (this.serverSide) this._scheduleFetch();
  }

  // ------------------------------------------------------------------
  // Internal: client-side view
  // ------------------------------------------------------------------

  _refreshClientView() {
    const all = Array.isArray(this.rows) ? this.rows.slice() : [];
    let filtered = all;
    if (this._search) {
      const q = this._search.toLowerCase();
      filtered = all.filter((row) =>
        this._visibleColumns().some((col) => {
          const v = readKey(row, col.key);
          return v != null && String(v).toLowerCase().includes(q);
        }),
      );
    }
    if (this._sortColumn) {
      const dir = this._sortDir === "desc" ? -1 : 1;
      filtered = filtered.slice().sort((a, b) => {
        const av = readKey(a, this._sortColumn);
        const bv = readKey(b, this._sortColumn);
        if (av == null && bv == null) return 0;
        if (av == null) return -1 * dir;
        if (bv == null) return 1 * dir;
        if (av < bv) return -1 * dir;
        if (av > bv) return 1 * dir;
        return 0;
      });
    }
    this._totalRows = all.length;
    this._filteredRows = filtered.length;
    const start = this._currentPage * this.pageSize;
    this._rowsView = filtered.slice(start, start + this.pageSize);

    this.dispatchEvent(
      new CustomEvent("cts-data-loaded", {
        bubbles: true,
        composed: true,
        detail: {
          recordsTotal: this._totalRows,
          recordsFiltered: this._filteredRows,
        },
      }),
    );
  }

  // ------------------------------------------------------------------
  // Internal: server-side fetch
  // ------------------------------------------------------------------

  _scheduleFetch() {
    if (this._debounceTimer) clearTimeout(this._debounceTimer);
    this._debounceTimer = setTimeout(() => {
      this._debounceTimer = null;
      this._fetchPage();
    }, 250);
  }

  _buildRequestUrl() {
    const url = new URL(this.ajaxUrl, window.location.origin);
    const draw = ++this._draw;
    url.searchParams.set("draw", String(draw));
    url.searchParams.set("start", String(this._currentPage * this.pageSize));
    url.searchParams.set("length", String(this.pageSize));
    url.searchParams.set("search-magnifying-glass", this._search || "");

    if (this._sortColumn) {
      const dir = this._sortDir === "desc" ? "desc" : "asc";
      if (this.requestShape === "datatables-default") {
        const visible = this._visibleColumns();
        const idx = visible.findIndex((c) => c.key === this._sortColumn);
        const colIdx = idx === -1 ? 0 : idx;
        url.searchParams.set(`order[0][column]`, String(colIdx));
        url.searchParams.set(`order[0][dir]`, dir);
        visible.forEach((c, i) => {
          url.searchParams.set(`columns[${i}][data]`, c.key);
        });
      } else {
        // datatables-comma-order (default): order=col,dir
        url.searchParams.set("order", `${this._sortColumn},${dir}`);
      }
    }
    return { url: url.toString(), draw };
  }

  async _fetchPage() {
    if (!this.ajaxUrl) {
      this._error = "ajaxUrl is required for server-side mode";
      return;
    }
    // Abort any prior in-flight request — its draw is already stale.
    if (this._inflightController) this._inflightController.abort();
    const controller = new AbortController();
    this._inflightController = controller;

    this.loading = true;
    this._error = null;
    const { url, draw } = this._buildRequestUrl();
    try {
      const response = await fetch(url, { signal: controller.signal });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const payload = await response.json();
      // Out-of-order suppression: only paint when the response's
      // draw matches the latest request issued. The DataTables convention
      // returns `draw` in the envelope; we coerce numerically because some
      // backends echo it as a string.
      const responseDraw = payload && payload.draw != null ? Number(payload.draw) : draw;
      if (responseDraw !== this._draw) {
        // A newer request has been issued in the meantime; drop this payload.
        return;
      }
      this._totalRows = payload.recordsTotal != null ? payload.recordsTotal : 0;
      this._filteredRows = payload.recordsFiltered != null ? payload.recordsFiltered : 0;
      this._rowsView = Array.isArray(payload.data) ? payload.data : [];
      this.dispatchEvent(
        new CustomEvent("cts-data-loaded", {
          bubbles: true,
          composed: true,
          detail: {
            recordsTotal: this._totalRows,
            recordsFiltered: this._filteredRows,
          },
        }),
      );
    } catch (err) {
      const e = /** @type {any} */ (err);
      if (e && e.name === "AbortError") {
        // Superseded by a newer request — silent.
        return;
      }
      this._error = e && e.message ? e.message : String(err);
      this._rowsView = [];
      this.dispatchEvent(
        new CustomEvent("cts-data-error", {
          bubbles: true,
          composed: true,
          detail: { error: err },
        }),
      );
    } finally {
      // Only flip loading off if this is still the active controller.
      if (this._inflightController === controller) {
        this.loading = false;
        this._inflightController = null;
      }
    }
  }

  // ------------------------------------------------------------------
  // Internal: lifecycle event dispatch
  // ------------------------------------------------------------------

  _dispatchDrawEvents() {
    // After the render that produced the new _rowsView, find each
    // <tr data-row-index="N"> and fire cts-row-rendered + cts-draw-complete.
    const rowEls = Array.from(this.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]"));
    rowEls.forEach((rowEl, index) => {
      const row = this._rowsView[index];
      if (row === undefined) return;
      this.dispatchEvent(
        new CustomEvent("cts-row-rendered", {
          bubbles: true,
          composed: true,
          detail: { row, index, rowEl },
        }),
      );
    });
    this.dispatchEvent(
      new CustomEvent("cts-draw-complete", {
        bubbles: true,
        composed: true,
        detail: { rows: this._rowsView, rowEls },
      }),
    );
  }

  // ------------------------------------------------------------------
  // Internal: event handlers
  // ------------------------------------------------------------------

  _visibleColumns() {
    return (this.columns || []).filter((c) => c && c.visible !== false);
  }

  _onHeaderClick(e) {
    // Resolve the clicked column from the header's data-column-key.
    const th = /** @type {HTMLElement} */ (e.currentTarget);
    const key = th.dataset.columnKey;
    const column = this._visibleColumns().find((c) => c.key === key);
    if (!column || !column.sortable) return;
    if (this._sortColumn === column.key) {
      this._sortDir = this._sortDir === "asc" ? "desc" : "asc";
    } else {
      this._sortColumn = column.key;
      this._sortDir = "asc";
    }
    this._currentPage = 0;
    this.dispatchEvent(
      new CustomEvent("cts-sort-change", {
        bubbles: true,
        composed: true,
        detail: { columnKey: this._sortColumn, direction: this._sortDir },
      }),
    );
    if (this.serverSide) this._scheduleFetch();
  }

  _onSearchInput(e) {
    this._searchInput = e.target.value;
    if (this.searchMode === "live-debounced") {
      this._search = this._searchInput;
      this._currentPage = 0;
      if (this.serverSide) this._scheduleFetch();
    }
  }

  _onSearchKeydown(e) {
    if (e.key === "Enter") {
      e.preventDefault();
      this._commitSearch();
      return;
    }
    if (e.key === "Escape" && (this._searchInput !== "" || this._search !== "")) {
      e.preventDefault();
      this._clearSearch();
    }
  }

  _onSearchButton() {
    this._commitSearch();
  }

  /**
   * Make the whole input pill behave as a focus target: clicking the leading
   * icon or the gaps inside the wrap focuses the inner <input>. Buttons
   * inside the wrap (clear / submit) handle their own clicks unchanged.
   *
   * Uses mousedown + preventDefault rather than click so focus never visibly
   * lands on the non-focusable leading <cts-icon> in between.
   *
   * @param {MouseEvent} e
   */
  _onSearchWrapMouseDown(e) {
    const target = /** @type {Element} */ (e.target);
    if (target.closest("button")) return;
    const input = /** @type {HTMLInputElement | null} */ (
      this.querySelector(".oidf-dt-search-input")
    );
    if (!input || target === input) return;
    e.preventDefault();
    input.focus();
  }

  _onSearchClear() {
    this._clearSearch();
    // Restore focus to the input so the user can keep typing.
    const input = /** @type {HTMLInputElement | null} */ (
      this.querySelector(".oidf-dt-search-input")
    );
    if (input) input.focus();
  }

  _clearSearch() {
    if (this._searchInput === "" && this._search === "") return;
    this._searchInput = "";
    this._search = "";
    this._currentPage = 0;
    if (this.serverSide) {
      if (this._debounceTimer) {
        clearTimeout(this._debounceTimer);
        this._debounceTimer = null;
      }
      this._fetchPage();
    }
  }

  _commitSearch() {
    this._search = this._searchInput;
    this._currentPage = 0;
    if (this.serverSide) {
      // Skip the debounce — the user explicitly committed.
      if (this._debounceTimer) {
        clearTimeout(this._debounceTimer);
        this._debounceTimer = null;
      }
      this._fetchPage();
    }
  }

  _onRowClick(e) {
    const rowEl = /** @type {HTMLElement} */ (e.currentTarget);
    const indexAttr = rowEl.dataset.rowIndex;
    if (indexAttr == null) return;
    const index = Number(indexAttr);
    const row = this._rowsView[index];
    if (row === undefined) return;
    this.dispatchEvent(
      new CustomEvent("cts-row-click", {
        bubbles: true,
        composed: true,
        detail: { row, index, rowEl },
      }),
    );
  }

  _onPagerPrev() {
    this._changePage(-1);
  }

  _onPagerNext() {
    this._changePage(1);
  }

  _changePage(delta) {
    const totalPages = Math.max(1, Math.ceil(this._filteredRows / this.pageSize));
    const next = Math.min(Math.max(0, this._currentPage + delta), totalPages - 1);
    if (next === this._currentPage) return;
    this._currentPage = next;
    this.dispatchEvent(
      new CustomEvent("cts-page-change", {
        bubbles: true,
        composed: true,
        detail: {
          draw: this._draw,
          start: this._currentPage * this.pageSize,
          length: this.pageSize,
        },
      }),
    );
    if (this.serverSide) this._scheduleFetch();
  }

  // ------------------------------------------------------------------
  // Render
  // ------------------------------------------------------------------

  _renderCellContent(row, column) {
    const value = readKey(row, column.key);

    // 1. Built-in formats.
    if (column.format === "date") {
      return formatDate(value);
    }
    if (column.format === "badge") {
      const label = value == null ? "" : String(value);
      return html`<cts-badge label=${label}></cts-badge>`;
    }
    if (column.format === "mono") {
      // mono is also handled via the wrapper class (FORMAT_CELL_CLASSES);
      // here we just render the raw value.
      return value == null ? "" : String(value);
    }

    // 2. Slot-by-name template lookup. Locate
    // `<template slot="cell-<render>">…</template>` in the host's children
    // and clone its contents for this cell. String substitution: any
    // text-node `${row.key}` is replaced with the row's value (best-effort,
    // covers the common Mustache shapes used today).
    if (column.render) {
      const slotName = `cell-${column.render}`;
      const template = this.querySelector(`:scope > template[slot="${slotName}"]`);
      if (template instanceof HTMLTemplateElement) {
        const fragment = /** @type {DocumentFragment} */ (template.content.cloneNode(true));
        // Substitute ${row.key} placeholders in text nodes and attributes.
        substituteRowPlaceholders(fragment, row);
        return fragment;
      }
    }

    // 3. Function-pointer renderer.
    if (typeof this.cellRenderer === "function") {
      const result = this.cellRenderer(row, column.key, column);
      if (result !== undefined && result !== null) return result;
    }

    // 4. Default: raw value.
    return value == null ? "" : String(value);
  }

  _renderCell(row, column) {
    const wrapperClass = column.format ? FORMAT_CELL_CLASSES[column.format] || "" : "";
    const monoClass = column.mono && !column.format ? "oidf-dt-cell-mono" : "";
    const cls = [wrapperClass, monoClass].filter(Boolean).join(" ");
    const content = this._renderCellContent(row, column);
    return html`<td class=${cls}>${content}</td>`;
  }

  _renderHeaderCell(column) {
    const isActive = this._sortColumn === column.key;
    const isDesc = isActive && this._sortDir === "desc";
    // Keep the sort indicator inside one icon family (coolicons "arrow") so
    // the glyph weight and arrowhead style stay consistent across neutral /
    // asc / desc states. Mixing arrows with carets used to make the active
    // state pop in an awkward, "different family" way.
    const arrowIcon = isActive
      ? isDesc
        ? "arrow-down-md"
        : "arrow-up-md"
      : "arrow-down-up";
    const headerClasses = column.sortable ? "is-sortable" : "";
    const ariaSort = column.sortable
      ? isActive
        ? isDesc
          ? "descending"
          : "ascending"
        : "none"
      : nothing;
    return html`
      <th
        class=${headerClasses}
        data-column-key=${column.key}
        aria-sort=${ariaSort}
        @click=${column.sortable ? this._onHeaderClick : null}
      >
        <span class="oidf-dt-th-inner">
          <span class="oidf-dt-th-label">${column.label || ""}</span>
          ${column.sortable
            ? html`<span class="oidf-dt-sort-arrow ${isActive ? "is-active" : ""}"
                ><cts-icon name="${arrowIcon}" size="16" aria-hidden="true"></cts-icon
              ></span>`
            : nothing}
        </span>
      </th>
    `;
  }

  _renderRows() {
    const visibleCols = this._visibleColumns();
    return repeat(
      this._rowsView,
      (row, index) => this._rowKey(row, index),
      (row, index) => {
        const extra = typeof this.rowClass === "function" ? this.rowClass(row) || "" : "";
        return html`
          <tr data-row-index=${index} class=${extra} @click=${this._onRowClick}>
            ${visibleCols.map((col) => this._renderCell(row, col))}
          </tr>
        `;
      },
    );
  }

  _rowKey(row, index) {
    if (row && row._id != null) return String(row._id);
    if (row && row.id != null) return String(row.id);
    if (row && row.testId != null) return String(row.testId);
    return `row-${index}`;
  }

  _renderSearch() {
    if (this.searchPlaceholder === "") return nothing;
    const hasDraft = this._searchInput !== "";
    const draftDiffersFromCommitted = this._searchInput !== this._search;
    const showSubmit = this.searchMode === "explicit" && draftDiffersFromCommitted;
    const ariaLabel = this.searchPlaceholder || "Search";
    return html`
      <div class="oidf-dt-search">
        <div class="oidf-dt-search-row">
          <div class="oidf-dt-search-input-wrap" @mousedown=${this._onSearchWrapMouseDown}>
            <cts-icon
              name="search-magnifying-glass"
              class="oidf-dt-search-leading"
              aria-hidden="true"
            ></cts-icon>
            <input
              type="search"
              class="oidf-dt-search-input"
              placeholder=${this.searchPlaceholder || "Search"}
              .value=${this._searchInput}
              @input=${this._onSearchInput}
              @keydown=${this._onSearchKeydown}
              aria-label=${ariaLabel}
              autocomplete="off"
              spellcheck="false"
            />
            ${hasDraft
              ? html`<button
                  type="button"
                  class="oidf-dt-search-clear"
                  aria-label="Clear search"
                  title="Clear search (Esc)"
                  @click=${this._onSearchClear}
                >
                  <cts-icon name="close-lg" aria-hidden="true"></cts-icon>
                </button>`
              : nothing}
            ${showSubmit
              ? html`<button
                  type="button"
                  class="oidf-dt-search-submit"
                  aria-label="Apply search"
                  title="Apply search (Enter)"
                  @click=${this._onSearchButton}
                >
                  <cts-icon name="arrow-undo-down-left" size="16" aria-hidden="true"></cts-icon>
                </button>`
              : nothing}
          </div>
        </div>
        ${this._renderFilterChip()}
      </div>
    `;
  }

  _renderFilterChip() {
    if (!this._search) return nothing;
    const count = this._filteredRows;
    const total = this._totalRows;
    let countText;
    if (total > 0 && count !== total) {
      countText = `${count} of ${total}`;
    } else {
      countText = `${count} match${count === 1 ? "" : "es"}`;
    }
    return html`
      <div class="oidf-dt-search-filter" role="status" aria-live="polite">
        <span class="oidf-dt-search-filter-label">Filtered to</span>
        <span class="oidf-dt-search-filter-query" title=${this._search}>${this._search}</span>
        <span class="oidf-dt-search-filter-count">· ${countText}</span>
        <button type="button" class="oidf-dt-search-filter-reset" @click=${this._onSearchClear}>
          <cts-icon name="close-md" aria-hidden="true"></cts-icon>
          Show all
        </button>
      </div>
    `;
  }

  _renderPager() {
    const totalPages = Math.max(1, Math.ceil(this._filteredRows / this.pageSize));
    const showingStart = this._filteredRows === 0 ? 0 : this._currentPage * this.pageSize + 1;
    const showingEnd = Math.min((this._currentPage + 1) * this.pageSize, this._filteredRows);
    const atFirst = this._currentPage === 0;
    const atLast = this._currentPage >= totalPages - 1;
    return html`
      <div class="oidf-dt-pager">
        <div class="oidf-dt-pager-info">
          ${this._filteredRows === 0
            ? "No entries to show"
            : `Showing ${showingStart} to ${showingEnd} of ${this._filteredRows}`}
        </div>
        <div class="oidf-dt-pager-controls">
          <cts-button
            class="oidf-dt-pager-prev"
            variant="secondary"
            size="sm"
            label="Previous"
            ?disabled=${atFirst}
            @cts-click=${this._onPagerPrev}
          ></cts-button>
          <cts-button
            class="oidf-dt-pager-next"
            variant="secondary"
            size="sm"
            label="Next"
            ?disabled=${atLast}
            @cts-click=${this._onPagerNext}
          ></cts-button>
        </div>
      </div>
    `;
  }

  _renderTableBody() {
    if (this._error) {
      return html`<tr>
        <td colspan=${this._visibleColumns().length} class="oidf-dt-error">
          Error loading data: ${this._error}
        </td>
      </tr>`;
    }
    if (this.loading && this._rowsView.length === 0) {
      return html`<tr>
        <td colspan=${this._visibleColumns().length} class="oidf-dt-loading">Loading...</td>
      </tr>`;
    }
    if (this._rowsView.length === 0) {
      return html`<tr>
        <td colspan=${this._visibleColumns().length} class="oidf-dt-empty-cell">
          <cts-empty-state heading=${this.emptyState}></cts-empty-state>
        </td>
      </tr>`;
    }
    return this._renderRows();
  }

  render() {
    const visibleCols = this._visibleColumns();
    return html`
      ${this._renderSearch()}
      <div class="oidf-dt-table-wrap">
        <table class="oidf-dt-table">
          <thead>
            <tr> ${visibleCols.map((col) => this._renderHeaderCell(col))} </tr>
          </thead>
          <tbody> ${this._renderTableBody()} </tbody>
        </table>
      </div>
      ${this._renderPager()}
    `;
  }
}

/**
 * Walk a fragment and substitute `${row.key}` (and `${key}`) placeholders
 * in text nodes and attribute values with values resolved from the row.
 *
 * Best-effort replacement — covers the small Mustache patterns used by
 * the existing per-cell templates. For richer per-cell rendering,
 * consumers should use the `cellRenderer` callback instead.
 *
 * @param {DocumentFragment | Element} root
 * @param {object} row
 */
function substituteRowPlaceholders(root, row) {
  const PLACEHOLDER = /\$\{(?:row\.)?([^}]+)\}/g;
  const replace = (raw) =>
    raw.replace(PLACEHOLDER, (_match, key) => {
      const v = readKey(row, key.trim());
      return v == null ? "" : String(v);
    });

  const walker = document.createTreeWalker(
    root,
    // 1 = SHOW_ELEMENT, 4 = SHOW_TEXT
    NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT,
  );
  let node = walker.nextNode();
  while (node) {
    if (node.nodeType === Node.TEXT_NODE) {
      const value = /** @type {Text} */ (node).nodeValue || "";
      if (value.indexOf("${") !== -1) {
        /** @type {Text} */ (node).nodeValue = replace(value);
      }
    } else if (node.nodeType === Node.ELEMENT_NODE) {
      const el = /** @type {Element} */ (node);
      for (const attr of Array.from(el.attributes)) {
        if (attr.value.indexOf("${") !== -1) {
          el.setAttribute(attr.name, replace(attr.value));
        }
      }
    }
    node = walker.nextNode();
  }
}

customElements.define("cts-data-table", CtsDataTable);

export {};
