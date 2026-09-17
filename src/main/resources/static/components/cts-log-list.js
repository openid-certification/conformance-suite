import { LitElement, html, nothing, css, unsafeCSS } from "lit";
import { repeat } from "lit/directives/repeat.js";
import { ifDefined } from "lit/directives/if-defined.js";
import { classMap } from "lit/directives/class-map.js";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-icon.js";
import "./cts-modal.js";
import "./cts-alert.js";
import "./cts-tooltip.js";
import "./cts-time.js";
import "./cts-empty-state.js";
import "./cts-loading-state.js";
import "./cts-json-view.js";
import { flashCopyConfirmed } from "../js/cts-copy-flash.js";
import { listingParams, readListingPage } from "../lib/listing-request.js";

const RESULT_BADGE_VARIANTS = {
  PASSED: "pass",
  FAILED: "fail",
  WARNING: "warn",
  REVIEW: "review",
  SKIPPED: "skip",
};

const STATUS_BADGE_VARIANTS = {
  RUNNING: "running",
  WAITING: "warn",
  FINISHED: "neutral",
  INTERRUPTED: "fail",
};

const VALID_STATUSES = [
  "NOT_YET_CREATED",
  "CREATED",
  "CONFIGURED",
  "RUNNING",
  "WAITING",
  "FINISHED",
  "INTERRUPTED",
];

const VALID_RESULTS = ["PASSED", "FAILED", "WARNING", "REVIEW", "SKIPPED", "UNKNOWN"];

// Status filter chips shown above the list. CREATED / CONFIGURED /
// NOT_YET_CREATED rarely surface to end users — kept out of the primary chip
// row to reduce noise. They still pass through URL params if a dashboard tile
// or saved link references them.
const STATUS_FILTER_CHIPS = ["RUNNING", "WAITING", "FINISHED", "INTERRUPTED"];

const RESULT_FILTER_CHIPS = ["PASSED", "FAILED", "WARNING", "REVIEW", "SKIPPED", "UNKNOWN"];

// The sort selector's options, as the server's `order` parameter. Every
// secondary key is newest-first so that ties (the same name, the same status)
// come out in a stable, useful order.
const SORT_ORDERS = {
  "started-desc": "started,desc",
  "started-asc": "started,asc",
  "name-asc": "testName,asc,started,desc",
  // alphabetical: the server sorts the stored status word, so equal statuses
  // group together, newest first within each
  "status-asc": "status,asc,started,desc",
};

// How long after the last keystroke the search box asks the server.
const SEARCH_DEBOUNCE_MS = 300;

const STYLE_ID = "cts-log-list-styles";

// Inline SVG chevron used as the custom select indicator, matching
// cts-form-field's `.oidf-select`. Stroke colour is `--ink-500` (`#71695E`),
// encoded as `%2371695E` in the data: URL.
const SELECT_CHEVRON =
  "url(\"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 16 16'><path fill='none' stroke='%2371695E' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' d='M4 6l4 4 4-4'/></svg>\")";

const STYLE_TEXT = css`
  cts-log-list {
    display: block;
    font-family: var(--font-sans);
    color: var(--fg);
  }
  .cts-log-list-toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    align-items: center;
    margin-bottom: var(--space-4);
  }
  /* Search input + filter trigger share a single bordered envelope so they
     read as one segmented control. The envelope owns the border, radius, and
     focus ring (via :focus-within); the children render borderless inside it. */
  .cts-log-list-searchbar {
    display: flex;
    align-items: stretch;
    flex: 1 1 320px;
    min-width: 260px;
    /* The bordered envelope is itself a default-size control: pin it to
       --control-height with border-box so its 1px border sits INSIDE the 34px
       and it aligns with adjacent buttons/selects (content-box would render
       34px content + 2px border = 36px). align-items:stretch fills the inner
       input + filter trigger to the envelope height. */
    height: var(--control-height);
    box-sizing: border-box;
    background: var(--bg);
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
  }
  .cts-log-list-searchbar:focus-within {
    border-color: var(--border-strong);
    box-shadow: var(--focus-ring);
  }
  .cts-log-list-search {
    position: relative;
    display: flex;
    align-items: center;
    flex: 1 1 auto;
    min-width: 0;
  }
  .cts-log-list-search input {
    width: 100%;
    box-sizing: border-box;
    /* border-box + --control-height so the bordered input is 34px outer (was
       content-sized ~36px), aligning with adjacent default-size controls. */
    height: calc(var(--control-height) - 2px);
    padding: var(--space-1) var(--space-3) var(--space-1) calc(var(--space-3) + var(--space-6));
    background: var(--bg);
    color: var(--fg);
    border: 0;
    /* preceded by the filter dropdown, so border and border-radius are removed on the left */
    border-radius: 0 var(--radius-2) var(--radius-2) 0;
    font-family: var(--font-sans);
    font-size: var(--fs-14);
    line-height: var(--control-height);
  }
  .cts-log-list-search input:focus {
    /* The envelope's :focus-within already paints the ring. */
    outline: none;
  }
  .cts-log-list-search cts-icon {
    position: absolute;
    left: var(--space-3);
    top: 50%;
    transform: translateY(-50%);
    color: var(--fg-soft);
    pointer-events: none;
  }
  /* Filter trigger — the left-hand segment of the searchbar. A 1px right
     divider is the seam between it and the search field; the dropdown panel
     anchors to this button. */
  .cts-log-filter-trigger {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    flex: 0 0 auto;
    padding: var(--space-2) var(--space-3);
    background: transparent;
    color: var(--fg);
    border: none;
    border-right: 1px solid var(--border);
    border-radius: calc(var(--radius-2) - 1px) 0 0 calc(var(--radius-2) - 1px);
    font-family: var(--font-sans);
    font-size: var(--fs-14);
    line-height: var(--lh-snug);
    white-space: nowrap;
    cursor: pointer;
    transition: background var(--dur-1) var(--ease-standard);
  }
  .cts-log-filter-trigger:hover {
    background: var(--bg-elev);
  }
  .cts-log-filter-trigger:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  .cts-log-filter-trigger[aria-expanded="true"] {
    background: var(--bg-elev);
  }
  .cts-log-list-sort {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--fs-13);
    color: var(--fg-soft);
  }
  .cts-log-list-sort select {
    box-sizing: border-box;
    height: var(--control-height);
    padding: 0 36px 0 var(--space-3);
    background: var(--bg-elev);
    color: var(--fg);
    border: 1px solid var(--ink-300);
    border-radius: var(--radius-2);
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    /* See cts-form-field .oidf-select — pin to 1 for crisp closed-state baseline. */
    line-height: 1;
    appearance: none;
    -webkit-appearance: none;
    background-image: ${unsafeCSS(SELECT_CHEVRON)};
    background-repeat: no-repeat;
    background-position: right 12px center;
  }
  .cts-log-list-sort select:focus {
    outline: none;
    border-color: var(--orange-400);
    box-shadow: var(--focus-ring);
  }
  /* The dropdown panel. Rendered in the top layer via the HTML Popover API,
     so it needs no z-index dance; position is set imperatively in
     _positionFilterPanel() against the filter trigger's bounding rect. The
     closed state is the UA default display:none — the open state inherits
     these rules. Mirrors the surface treatment of cts-action-overflow's
     popover. */
  .cts-log-filter-panel {
    margin: 0;
    padding: var(--space-3);
    background: var(--bg-elev);
    color: var(--fg);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    box-shadow: var(--shadow-3);
    font-size: var(--fs-14);
    min-width: 260px;
    position: fixed;
    inset: auto;
  }
  .cts-log-filter-panel fieldset {
    margin: 0;
    padding: 0;
    border: none;
  }
  .cts-log-filter-panel fieldset + fieldset {
    margin-top: var(--space-3);
  }
  .cts-log-filter-panel legend {
    padding: 0;
    margin-bottom: var(--space-2);
    font-size: var(--fs-13);
    font-weight: var(--fw-medium);
    color: var(--fg-soft);
  }
  .cts-log-filter-options {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }
  .cts-log-filter-option {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-2);
    cursor: pointer;
  }
  .cts-log-filter-option:hover {
    background: var(--bg);
  }
  /* Checkbox treatment mirrors cts-form-field's .oidf-checkbox so form
     controls look identical across the suite (size, accent, focus ring). */
  .cts-log-filter-option input {
    width: var(--space-4);
    height: var(--space-4);
    margin: 0;
    accent-color: var(--orange-500);
    cursor: pointer;
  }
  .cts-log-filter-option input:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
    border-radius: var(--radius-1);
  }
  .cts-log-filter-option span {
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    line-height: var(--lh-snug);
    color: var(--fg);
  }
  .cts-log-filter-panel-footer {
    display: flex;
    justify-content: flex-end;
    margin-top: var(--space-3);
    padding-top: var(--space-2);
    border-top: 1px solid var(--border);
  }
  .cts-log-filter-clear {
    padding: var(--space-1) var(--space-2);
    background: transparent;
    color: var(--fg-link);
    border: none;
    border-radius: var(--radius-2);
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    cursor: pointer;
  }
  .cts-log-filter-clear:hover {
    text-decoration: underline;
  }
  .cts-log-filter-clear:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  .cts-log-active-summary {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-3);
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-pill);
    color: var(--fg);
    font-family: var(--font-sans);
    font-size: var(--fs-14);
    line-height: var(--lh-snug);
    cursor: pointer;
    margin-bottom: var(--space-4);
    transition:
      border-color var(--dur-1) var(--ease-standard),
      background var(--dur-1) var(--ease-standard);
  }
  .cts-log-active-summary:hover {
    border-color: var(--border-strong);
    background: var(--bg);
  }
  .cts-log-active-summary:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  .cts-log-list-items {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }
  /* Adrian Roselli's "block link" pattern (a.k.a. pseudo-element overlay):
     the card root is a non-interactive article; the test-name headline is
     the single real anchor per card; that headline carries an ::after
     pseudo-element absolutely positioned to cover the whole card so the
     click target spans the card silhouette. Nested interactive controls
     (config button, plan chip, owner pills) sit on z-index: 1 so they
     receive their own clicks instead of the headline overlay — no
     stopPropagation gymnastics needed and the HTML stays valid (no nested
     anchor inside an anchor). Text selection, Cmd+click "open in new tab",
     and right-click context menu all keep working because the overlay is a
     pseudo-element, not a layered element.
     See https://adrianroselli.com/2020/02/block-links-cards-clickable-regions-etc.html */
  .cts-log-card {
    position: relative;
    display: grid;
    gap: var(--space-2);
    padding: var(--space-4);
    background: var(--bg-elev);
    color: var(--fg);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    transition:
      border-color var(--dur-1) var(--ease-standard),
      background var(--dur-1) var(--ease-standard);
  }
  .cts-log-card:hover {
    border-color: var(--border-strong);
    background: var(--bg);
  }
  /* When the focus lands on the headline link (the only focusable bit of
     the block-link surface), promote the focus ring to the card border so
     keyboard users see the full card as the focused unit. */
  .cts-log-card:focus-within {
    outline: none;
    box-shadow: var(--focus-ring);
    border-color: var(--border-strong);
  }
  .cts-log-card-header {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-3);
  }
  .cts-log-card-identity {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    min-width: 0;
    flex: 1 1 280px;
  }
  .cts-log-card-name {
    display: inline-block;
    font-size: var(--fs-16);
    line-height: var(--lh-snug);
    font-weight: var(--fw-bold);
    color: var(--fg);
    text-decoration-line: none;
    word-break: break-word;
  }
  /* Pseudo-element overlay: the headline link's clickable area expands to
     the whole card. Other interactive children explicitly opt-in to
     z-index: 1 so they sit above this layer. */
  .cts-log-card-name::after {
    content: "";
    position: absolute;
    inset: 0;
    border-radius: inherit;
  }
  .cts-log-card-name:hover,
  .cts-log-card-name:focus-visible {
    text-decoration-line: none;
  }
  .cts-log-card-name:focus-visible {
    outline: none;
  }
  .cts-log-card-slug {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    line-height: var(--lh-snug);
    color: var(--fg-soft);
    word-break: break-all;
  }
  .cts-log-card-badges {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    flex-shrink: 0;
  }
  .cts-log-card-description {
    margin: 0;
    color: var(--fg-soft);
    font-size: var(--fs-14);
    line-height: var(--lh-snug);
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
  .cts-log-card-meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-1) var(--space-4);
    font-size: var(--fs-13);
    line-height: var(--lh-snug);
    color: var(--fg-soft);
  }
  .cts-log-card-meta-item {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
  }
  .cts-log-card-meta-key {
    color: var(--fg-soft);
    font-weight: var(--fw-medium);
  }
  .cts-log-card-meta-value {
    color: var(--fg);
  }
  .cts-log-card-meta-value.is-mono {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
  }
  /* Light-DOM anchor: the underline fade (line underline + transparent at
     rest, token color on hover, transition on text-decoration-color) comes
     from the global \`a\` rule. Toggling text-decoration-line here would break
     it — line is discrete and can't animate — so we keep only layout. */
  .cts-log-card-plan-link {
    position: relative;
    z-index: 1;
    color: var(--fg-link);
    text-underline-offset: 2px;
    font-family: var(--font-mono);
    font-size: var(--fs-12);
  }
  /* Nested controls lift above the headline link's ::after overlay so the
     browser routes clicks on them to the control, not the card link. */
  .cts-log-card .showConfigBtn,
  .cts-log-card .log-owner {
    position: relative;
    z-index: 1;
  }
  /* The Started value carries a tooltip but is not itself interactive in
     the navigation sense — it inherits the card-link click area via the
     ::after overlay, so we deliberately do NOT lift it on z-index. */
  .cts-log-card-actions {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin-left: auto;
  }
  /* Owner pill — markup mirrors templates/owner.html so the two-tone chip
     visually matches the rest of the suite. Kept inline-flex with nowrap so
     the chip never breaks across two lines. */
  .cts-log-card .log-owner {
    display: inline-flex;
    flex-wrap: nowrap;
    align-items: center;
    gap: 0;
  }
  .cts-log-card .ownerSub,
  .cts-log-card .ownerIss {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 2px;
    background: var(--bg);
    border: 1px solid var(--border);
    color: var(--fg-soft);
  }
  .cts-log-card .ownerSub {
    border-top-left-radius: var(--radius-pill);
    border-bottom-left-radius: var(--radius-pill);
    border-right: none;
  }
  .cts-log-card .ownerIss {
    border-top-right-radius: var(--radius-pill);
    border-bottom-right-radius: var(--radius-pill);
  }
  .cts-log-card .ownerSub:focus-visible,
  .cts-log-card .ownerIss:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  .cts-log-list-empty {
    padding: var(--space-5);
    text-align: center;
    color: var(--fg-soft);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);
  }
  /* A re-query (sort, search, chip) keeps the rows on screen, dimmed and
     inert, until the server answers; only the first load and a My/Published
     swap show the spinner in their place. */
  .cts-log-list-items.is-refreshing {
    opacity: 0.6;
    pointer-events: none;
    transition: opacity 150ms ease-out;
  }
  .cts-log-list-refreshing {
    margin: 0;
    color: var(--fg-soft);
    font-size: var(--fs-13);
  }
  .cts-log-list-footer {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);
    margin-top: var(--space-4);
    color: var(--fg-soft);
    font-size: var(--fs-13);
  }
  .cts-log-list-config-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
    margin-bottom: var(--space-4);
  }
  .cts-log-list-config-toolbar code {
    font-family: var(--font-mono);
    font-size: var(--fs-13);
    word-break: break-all;
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

// Per-instance id ties the filter trigger to its popover panel via
// aria-controls + popovertarget. The counter keeps it unique when multiple
// cts-log-list instances coexist (e.g. a Storybook docs page).
let filterPanelIdCounter = 0;
function nextFilterPanelId() {
  filterPanelIdCounter += 1;
  return `cts-log-filter-panel-${filterPanelIdCounter}`;
}

function popoverApiSupported() {
  return (
    typeof HTMLElement !== "undefined" &&
    Object.prototype.hasOwnProperty.call(HTMLElement.prototype, "popover")
  );
}

function parseFilterSet(raw, valid) {
  if (!raw) return new Set();
  const validSet = new Set(valid);
  return new Set(
    raw
      .split(",")
      .map((t) => t.trim().toUpperCase())
      .filter((t) => validSet.has(t)),
  );
}

function formatVariant(variant) {
  if (!variant) return "";
  if (typeof variant === "string") return variant;
  return Object.entries(variant)
    .map(([key, value]) => `${key}=${value}`)
    .join(", ");
}

/**
 * Filterable list of test logs. Replaces the legacy 10-column DataTable on
 * `logs.html` with a single-column card layout. Each card carries the test
 * name (headline link), the instance id (slug), the description, status +
 * result badges, a metadata footer (variant, started, plan id, owner when
 * admin), and a "View configuration" icon button.
 *
 * A free-text search input and a faceted filter dropdown share one bordered
 * "searchbar" container so they read as a single control; a sort selector
 * sits alongside. The filter dropdown is a native HTML Popover (top layer,
 * light-dismiss on outside-click + Escape) holding two checkbox groups —
 * Status and Result — for multiselect faceting. Selections sync to the
 * `?status=` and `?result=` URL params via `history.replaceState`, so the
 * existing dashboard deep-link contract is preserved. Search and sort live
 * in component state and reset on reload. On browsers without the Popover
 * API the trigger silently does nothing — the production audience runs
 * current browsers (mirrors the cts-action-overflow constraint).
 *
 * The SERVER does the work: every filter, the search term and the sort
 * order travel with the `/api/log` request (`status`, `result`, `search`,
 * `order`), which returns one page of rows; "Show more" asks for
 * the next page and appends it. Each row already carries the name of the
 * plan it belongs to, so rendering a page needs no further requests.
 *
 * Light DOM. Scoped CSS is injected once on first connect.
 *
 * @property {boolean} isAdmin - Reveals the Owner pill on each card when set.
 *   Reflects the `is-admin` attribute. Ignored when `isPublic` is true.
 * @property {boolean} isPublic - Switches the fetch to `/api/log?public=true`
 *   and suppresses admin-only affordances (Owner pill, config button).
 *   Reflects the `is-public` attribute.
 * @fires cts-log-filter-change - Bubbles when the user toggles a status or
 *   result filter checkbox, or clears all filters. `detail: { status:
 *   string[], result: string[] }` carries the post-change selection sets as
 *   arrays.
 */
class CtsLogList extends LitElement {
  static properties = {
    isAdmin: { type: Boolean, attribute: "is-admin" },
    isPublic: { type: Boolean, attribute: "is-public" },
    _logs: { state: true },
    _loading: { state: true },
    _refreshing: { state: true },
    _loadingMore: { state: true },
    _hasMore: { state: true },
    _error: { state: true },
    _statusFilter: { state: true },
    _resultFilter: { state: true },
    _searchText: { state: true },
    _sortKey: { state: true },
    _selectedConfig: { state: true },
    _selectedTestId: { state: true },
    _filterOpen: { state: true },
  };

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  constructor() {
    super();
    this.isAdmin = false;
    this.isPublic = false;
    this._logs = [];
    this._loading = true;
    this._refreshing = false;
    this._loadingMore = false;
    this._hasMore = false;
    this._error = null;
    this._statusFilter = new Set();
    this._resultFilter = new Set();
    this._searchText = "";
    this._sortKey = "started-desc";
    this._selectedConfig = null;
    this._selectedTestId = "";
    // Filter dropdown (HTML Popover API). Ids tie the trigger to the panel;
    // `_supported` gates the ARIA + popovertarget wiring so we don't claim a
    // popover exists on browsers that lack the API. `_filterOpen` mirrors the
    // panel's :popover-open state for the trigger's aria-expanded.
    this._filterPanelId = nextFilterPanelId();
    this._popoverSupported = popoverApiSupported();
    this._filterOpen = false;
    // Monotonic id of the most recent listing request. A sort change while a
    // search is still out, or two quick chip toggles, are two fetches with no
    // ordering guarantee between them, and the loser must not overwrite the
    // winner's rows - or clear its loading state. Non-reactive.
    this._fetchSeq = 0;
    // The pending search-box debounce, cleared on disconnect. Non-reactive.
    /** @type {ReturnType<typeof setTimeout>|undefined} */
    this._searchTimer = undefined;
    // The trimmed term the rows were last asked for, so committing the box
    // (Enter, or the blur that follows typing) does not ask the server for
    // what it already answered. Non-reactive.
    this._appliedSearch = "";
    // Pre-bind handlers used by Lit EventParts on rendered cards. Lit
    // dispatches with `this` set to the host element of the listener; the
    // handlers need to retain this component as `this`.
    this._handleSearchInput = this._handleSearchInput.bind(this);
    this._handleSearchCommit = this._handleSearchCommit.bind(this);
    this._handleSortChange = this._handleSortChange.bind(this);
    this._handleStatusToggle = this._handleStatusToggle.bind(this);
    this._handleResultToggle = this._handleResultToggle.bind(this);
    this._handleClearAllClick = this._handleClearAllClick.bind(this);
    this._handleConfigButtonClick = this._handleConfigButtonClick.bind(this);
    this._handleShowMoreClick = this._handleShowMoreClick.bind(this);
    this._handleCopyConfig = this._handleCopyConfig.bind(this);
    this._handleFilterBeforeToggle = this._handleFilterBeforeToggle.bind(this);
  }

  connectedCallback() {
    super.connectedCallback();
    // The host is the stable wrapper that persists across loading→loaded
    // renders, so announce dataset changes (e.g. a My⇄Published tab swap via
    // reloadForViewChange()) here rather than on a fragment that re-creates
    // itself each render (R17/R19). The list region is supplementary, so a
    // polite live region is appropriate. Mirrors cts-plan-list.
    this.setAttribute("aria-live", "polite");
    this._hydrateFromUrl();
    this._fetchLogs({ fresh: true });
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    clearTimeout(this._searchTimer);
    // The browser evicts an open popover from the top layer without firing
    // `beforetoggle` when the host is removed, so the mirror would otherwise
    // stay stuck. Reset it so a re-attached host renders aria-expanded=false.
    this._filterOpen = false;
  }

  _hydrateFromUrl() {
    const params = new URLSearchParams(window.location.search);
    this._statusFilter = parseFilterSet(params.get("status"), VALID_STATUSES);
    this._resultFilter = parseFilterSet(params.get("result"), VALID_RESULTS);
  }

  /**
   * Public entry point the page calls on every My⇄Published view change
   * (`cts-view-tab-change`, fired by `cts-view-tabs` on both click and
   * back/forward). Resets the status/result chip filters and the free-text
   * search to their defaults so a new dataset never inherits the prior view's
   * filters (R16), drops `?status`/`?result` from the URL (via `_writeUrl`,
   * which preserves `?public`), and reloads the dataset for the current
   * `isPublic` view from its first page. The caller MUST set/remove the `is-public`
   * attribute BEFORE invoking this — `_fetchLogs` reads `this.isPublic`, and
   * Lit reflects the boolean attribute synchronously, so the refetch targets
   * the correct dataset.
   *
   * State changes here are synchronous, so Lit batches them into a single
   * render: the user sees the loading state directly (R17), never a flash of
   * the prior dataset rendered unfiltered. Unlike `_handleClearAllClick`, this
   * does NOT restore focus to the search input — the triggering tab anchor
   * already holds focus, which is the correct landing point for a keyboard user
   * who just switched views.
   * @returns {void}
   */
  reloadForViewChange() {
    this._statusFilter = new Set();
    this._resultFilter = new Set();
    this._clearSearch();
    this._writeUrl();
    this._fetchLogs({ fresh: true });
  }

  /**
   * Empty the search box and forget the term it was last asked for, dropping
   * any debounce still pending so it cannot fire after the reset.
   * @returns {void}
   */
  _clearSearch() {
    clearTimeout(this._searchTimer);
    this._searchTimer = undefined;
    this._searchText = "";
    this._appliedSearch = "";
  }

  /**
   * The endpoint-specific parameters: the status and result lists, as the
   * `?status=` / `?result=` URL contract spells them (lower-case, comma-joined).
   * @returns {URLSearchParams} Those parameters, empty when nothing is filtered.
   */
  _filterParams() {
    const params = new URLSearchParams();
    if (this._statusFilter.size > 0) {
      params.set("status", Array.from(this._statusFilter).join(",").toLowerCase());
    }
    if (this._resultFilter.size > 0) {
      params.set("result", Array.from(this._resultFilter).join(",").toLowerCase());
    }
    return params;
  }

  /**
   * Ask the server for a page: the first page of the current filter, search
   * and sort, or - on "Show more" - the page after the rows already shown,
   * which is appended to them.
   *
   * A re-query keeps the rows it is about to replace on screen, dimmed, so
   * a sort or a search does not flash the list through the spinner; the
   * spinner is for when there is nothing to keep - the first load, and a
   * My/Published swap (`fresh`), where the old rows are the wrong dataset.
   * @param {{append?: boolean, fresh?: boolean}} [options] - `append` for the
   *   next page; `fresh` to show the loading state in place of the rows.
   * @returns {Promise<void>} Resolves once the fetch settles.
   */
  async _fetchLogs({ append = false, fresh = false } = {}) {
    const seq = ++this._fetchSeq;
    const start = append ? this._logs.length : 0;
    if (append) {
      this._loadingMore = true;
    } else if (fresh || this._logs.length === 0) {
      this._loading = true;
    } else {
      this._refreshing = true;
    }
    this._error = null;
    try {
      const params = listingParams({
        start,
        order: SORT_ORDERS[this._sortKey] || SORT_ORDERS["started-desc"],
        search: this._searchText,
        isPublic: this.isPublic,
        extra: this._filterParams(),
      });
      const response = await fetch(`/api/log?${params}`);
      // A later request has already been made, so this answer is stale
      // whatever it says.
      if (seq !== this._fetchSeq) return;
      if (!response.ok) {
        throw new Error(`Failed to load logs (HTTP ${response.status})`);
      }
      const payload = await response.json();
      if (seq !== this._fetchSeq) return;
      const { rows, hasMore } = readListingPage(payload, start);
      this._logs = append ? [...this._logs, ...rows] : rows;
      this._hasMore = hasMore;
    } catch (err) {
      if (seq !== this._fetchSeq) return;
      this._error = err instanceof Error ? err.message : String(err);
      this._logs = [];
      this._hasMore = false;
    } finally {
      // A superseded request must not clear the loading state the request that
      // superseded it set.
      if (seq === this._fetchSeq) {
        this._loading = false;
        this._refreshing = false;
        this._loadingMore = false;
      }
    }
  }

  _writeUrl() {
    const params = new URLSearchParams(window.location.search);
    if (this._statusFilter.size > 0) {
      params.set(
        "status",
        Array.from(this._statusFilter)
          .map((s) => s.toLowerCase())
          .join(","),
      );
    } else {
      params.delete("status");
    }
    if (this._resultFilter.size > 0) {
      params.set(
        "result",
        Array.from(this._resultFilter)
          .map((s) => s.toLowerCase())
          .join(","),
      );
    } else {
      params.delete("result");
    }
    const newSearch = params.toString();
    const newUrl =
      window.location.pathname + (newSearch ? "?" + newSearch : "") + window.location.hash;
    window.history.replaceState(null, "", newUrl);
    this.dispatchEvent(
      new CustomEvent("cts-log-filter-change", {
        bubbles: true,
        composed: true,
        detail: {
          status: Array.from(this._statusFilter),
          result: Array.from(this._resultFilter),
        },
      }),
    );
  }

  /**
   * The search box asks the server, so it waits for the typing to pause
   * rather than sending a request per keystroke.
   * @param {Event} event - The `input` event.
   * @returns {void}
   */
  _handleSearchInput(event) {
    this._searchText = /** @type {HTMLInputElement} */ (event.target).value;
    clearTimeout(this._searchTimer);
    this._searchTimer = setTimeout(() => {
      this._searchTimer = undefined;
      this._applySearch(this._searchText);
    }, SEARCH_DEBOUNCE_MS);
  }

  /**
   * Enter (or the box's own clear button) commits the term at once. `change`
   * also fires on the blur after typing, by which time the debounce has
   * usually already asked; `_applySearch` is what keeps that from asking twice.
   * @param {Event} event - The `change` event.
   * @returns {void}
   */
  _handleSearchCommit(event) {
    clearTimeout(this._searchTimer);
    this._searchTimer = undefined;
    this._applySearch(/** @type {HTMLInputElement} */ (event.target).value);
  }

  /**
   * Ask the server for a term, unless it is the one the rows already answer.
   * @param {string} term - What the search box holds.
   * @returns {void}
   */
  _applySearch(term) {
    this._searchText = term;
    const wanted = term.trim();
    if (wanted === this._appliedSearch) return;
    this._appliedSearch = wanted;
    this._fetchLogs();
  }

  _handleSortChange(event) {
    this._sortKey = event.target.value;
    this._fetchLogs();
  }

  _toggleSetMember(set, value) {
    const next = new Set(set);
    if (next.has(value)) next.delete(value);
    else next.add(value);
    return next;
  }

  _handleStatusToggle(event) {
    const value = event.currentTarget.dataset.status;
    if (!value) return;
    this._statusFilter = this._toggleSetMember(this._statusFilter, value);
    this._writeUrl();
    this._fetchLogs();
  }

  _handleResultToggle(event) {
    const value = event.currentTarget.dataset.result;
    if (!value) return;
    this._resultFilter = this._toggleSetMember(this._resultFilter, value);
    this._writeUrl();
    this._fetchLogs();
  }

  _handleClearAllClick(event) {
    event.preventDefault();
    this._statusFilter = new Set();
    this._resultFilter = new Set();
    this._clearSearch();
    this._writeUrl();
    this._fetchLogs();
    // Clearing from inside the panel closes it; clearing from the summary
    // button is a no-op here (the panel is already closed).
    this._hideFilterPanel();
    // Restore focus to the search input so keyboard users have a natural
    // landing point after dismissing the summary or panel.
    this.updateComplete.then(() => {
      const search = /** @type {HTMLInputElement | null} */ (
        this.querySelector(".cts-log-list-search input")
      );
      if (search) search.focus();
    });
  }

  // beforetoggle fires before the visual state change. Use it to position the
  // top-layer panel against the trigger's current rect (pre-paint) and to
  // mirror the open state into the trigger's aria-expanded synchronously.
  _handleFilterBeforeToggle(event) {
    if (event.newState === "open") {
      this._positionFilterPanel();
    }
    this._filterOpen = event.newState === "open";
  }

  _positionFilterPanel() {
    // Anchor the panel under the filter trigger's left edge so the menu hangs
    // directly off the button. The trigger is the left-hand segment of the
    // searchbar, so this also aligns with the searchbar's left edge. The
    // Popover API renders in the top layer, so position:fixed offsets are
    // viewport-relative and flow above any sticky/transformed ancestor.
    const trigger = this.querySelector(".cts-log-filter-trigger");
    const panel = /** @type {HTMLElement | null} */ (this.querySelector(".cts-log-filter-panel"));
    if (!trigger || !panel) return;
    const rect = trigger.getBoundingClientRect();
    panel.style.top = `${Math.round(rect.bottom + 4)}px`;
    panel.style.left = `${Math.max(8, Math.round(rect.left))}px`;
  }

  _hideFilterPanel() {
    const panel = /** @type {(HTMLElement & { hidePopover?: () => void }) | null} */ (
      this.querySelector(".cts-log-filter-panel")
    );
    if (panel && typeof panel.hidePopover === "function") {
      try {
        panel.hidePopover();
      } catch {
        // Ignore — already hidden, or the API is unavailable.
      }
    }
  }

  _handleShowMoreClick() {
    if (this._loadingMore || this._refreshing) return;
    this._fetchLogs({ append: true });
  }

  _describeActiveFilter() {
    const parts = [];
    if (this._statusFilter.size > 0) {
      const tokens = Array.from(this._statusFilter)
        .map((s) => s.toLowerCase())
        .sort();
      parts.push(`Status: ${tokens.join(" or ")}`);
    }
    if (this._resultFilter.size > 0) {
      const tokens = Array.from(this._resultFilter)
        .map((s) => s.toLowerCase())
        .sort();
      parts.push(`Result: ${tokens.join(" or ")}`);
    }
    if (this._searchText.trim()) {
      parts.push(`Search: ${this._searchText.trim()}`);
    }
    return parts.join(" · ");
  }

  _renderSearchAndSort() {
    return html`
      <div class="cts-log-list-toolbar">
        <div class="cts-log-list-searchbar">
          ${this._renderFilterDropdown()}
          <label class="cts-log-list-search">
            <cts-icon name="search-magnifying-glass" size="16" aria-hidden="true"></cts-icon>
            <input
              type="search"
              aria-label="Search logs"
              placeholder="Search logs"
              title="Whole words, matched by the server against the test name and description"
              .value=${this._searchText}
              @input=${this._handleSearchInput}
              @change=${this._handleSearchCommit}
            />
          </label>
        </div>
        <label class="cts-log-list-sort">
          <span>Sort</span>
          <select aria-label="Sort logs" .value=${this._sortKey} @change=${this._handleSortChange}>
            <option value="started-desc">Started (newest)</option>
            <option value="started-asc">Started (oldest)</option>
            <option value="name-asc">Test name (A–Z)</option>
            <option value="status-asc">Status (A–Z)</option>
          </select>
        </label>
      </div>
    `;
  }

  _renderFilterDropdown() {
    const activeCount = this._statusFilter.size + this._resultFilter.size;
    // The trigger advertises and opens the panel only when the Popover API is
    // present; aria-controls and popovertarget both point at the same id.
    const panelId = this._popoverSupported ? this._filterPanelId : undefined;
    const ariaExpanded = this._popoverSupported ? (this._filterOpen ? "true" : "false") : undefined;
    // Two closures rather than one shared helper: the data-status / data-result
    // attribute name is part of each <input>'s tested + agent-facing contract,
    // and Lit cannot interpolate attribute *names* (only values), so a single
    // parameterised row is not expressible without breaking that contract.
    const statusOption = (value) => html`
      <label class="cts-log-filter-option">
        <input
          type="checkbox"
          data-status="${value}"
          .checked=${this._statusFilter.has(value)}
          @change=${this._handleStatusToggle}
        />
        <span>${value.toLowerCase()}</span>
      </label>
    `;
    const resultOption = (value) => html`
      <label class="cts-log-filter-option">
        <input
          type="checkbox"
          data-result="${value}"
          .checked=${this._resultFilter.has(value)}
          @change=${this._handleResultToggle}
        />
        <span>${value.toLowerCase()}</span>
      </label>
    `;
    return html`
      <button
        type="button"
        class="cts-log-filter-trigger"
        data-testid="log-filter-trigger"
        aria-controls="${ifDefined(panelId)}"
        aria-expanded="${ifDefined(ariaExpanded)}"
        popovertarget="${ifDefined(panelId)}"
      >
        <cts-icon name="filter" size="16" aria-hidden="true"></cts-icon>
        <span>Filter</span>
        ${activeCount > 0
          ? html`<cts-badge variant="primary" count="${activeCount}"></cts-badge>`
          : nothing}
        <cts-icon name="chevron-down" size="16" aria-hidden="true"></cts-icon>
      </button>
      <div
        id="${this._filterPanelId}"
        class="cts-log-filter-panel"
        popover="auto"
        aria-label="Filter logs"
        data-testid="log-filter-panel"
        @beforetoggle=${this._handleFilterBeforeToggle}
      >
        <fieldset>
          <legend>Status</legend>
          <div class="cts-log-filter-options">${STATUS_FILTER_CHIPS.map(statusOption)}</div>
        </fieldset>
        <fieldset>
          <legend>Result</legend>
          <div class="cts-log-filter-options">${RESULT_FILTER_CHIPS.map(resultOption)}</div>
        </fieldset>
        <div class="cts-log-filter-panel-footer">
          <button type="button" class="cts-log-filter-clear" @click=${this._handleClearAllClick}>
            Clear all
          </button>
        </div>
      </div>
    `;
  }

  _renderActiveFilterSummary() {
    const hasFacet = this._statusFilter.size > 0 || this._resultFilter.size > 0;
    const hasSearch = this._searchText.trim().length > 0;
    if (!hasFacet && !hasSearch) return nothing;
    // The server never counts: only the rows loaded so far are known, plus
    // whether there is a further page.
    const filteredCount = this._logs.length;
    const truncatedMarker = this._hasMore ? "+" : "";
    const matchLabel = filteredCount === 1 && !this._hasMore ? "match" : "matches";
    return html`
      <button
        type="button"
        class="cts-log-active-summary"
        data-testid="active-filter-summary"
        aria-label="Clear active filters"
        @click=${this._handleClearAllClick}
      >
        <cts-icon name="filter" size="16" aria-hidden="true"></cts-icon>
        <span
          >${this._describeActiveFilter()} (${filteredCount}${truncatedMarker} ${matchLabel})</span
        >
        <cts-icon name="close-md" size="16" aria-hidden="true"></cts-icon>
      </button>
    `;
  }

  _renderOwner(owner) {
    if (!owner) return nothing;
    const sub = owner.sub || "";
    const iss = owner.iss || "";
    // The chip is a labelled tooltip host. tabindex="0" stays so keyboard
    // users can reach the tooltip content; the chip itself has no Enter
    // activation (it is not a button), so screen-reader users hear the
    // aria-label and move on. The chip sits inside .log-owner which is
    // lifted on z-index: 1, so a click on the chip does NOT activate the
    // card's headline-link overlay.
    return html`
      <span class="log-owner">
        <cts-tooltip content="${sub}" placement="top">
          <span class="ownerSub" aria-label="Subject: ${sub}" tabindex="0">
            <cts-icon name="user-01" size="16" aria-hidden="true"></cts-icon>
          </span>
        </cts-tooltip>
        <cts-tooltip content="${iss}" placement="top">
          <span class="ownerIss" aria-label="Issuer: ${iss}" tabindex="0">
            <cts-icon name="globe" size="16" aria-hidden="true"></cts-icon>
          </span>
        </cts-tooltip>
      </span>
    `;
  }

  _renderCard(log) {
    const publicSuffix = this.isPublic ? "&public=true" : "";
    const href = `log-detail.html?log=${encodeURIComponent(log.testId)}${publicSuffix}`;
    const variantString = formatVariant(log.variant);
    const statusVariant = STATUS_BADGE_VARIANTS[log.status] || "neutral";
    const resultVariant = RESULT_BADGE_VARIANTS[log.result] || "neutral";
    const showOwner = !this.isPublic && this.isAdmin && log.owner;
    const showConfig = !this.isPublic;
    const planHref = log.planId
      ? `plan-detail.html?plan=${encodeURIComponent(log.planId)}${publicSuffix}`
      : null;
    return html`
      <article class="cts-log-card" data-testid="log-list-item" data-test-id="${log.testId}">
        <div class="cts-log-card-header">
          <div class="cts-log-card-identity">
            <a class="cts-log-card-name" href="${href}" data-testid="log-list-link"
              >${log.testName || log.testId}</a
            >
            <span class="cts-log-card-slug">${log.testId}</span>
          </div>
          <div class="cts-log-card-badges">
            ${log.status
              ? html`<cts-badge variant="${statusVariant}" label="${log.status}"></cts-badge>`
              : nothing}
            ${log.result && log.result !== "UNKNOWN"
              ? html`<cts-badge variant="${resultVariant}" label="${log.result}"></cts-badge>`
              : nothing}
          </div>
        </div>
        ${log.description
          ? html`<p class="cts-log-card-description">${log.description}</p>`
          : nothing}
        <div class="cts-log-card-meta">
          ${variantString
            ? html`
                <span class="cts-log-card-meta-item">
                  <span class="cts-log-card-meta-key">Variant</span>
                  <span class="cts-log-card-meta-value is-mono">${variantString}</span>
                </span>
              `
            : nothing}
          ${log.started
            ? html`
                <span class="cts-log-card-meta-item">
                  <span class="cts-log-card-meta-key">Started</span>
                  <span class="cts-log-card-meta-value">
                    <cts-time mode="auto" value=${log.started}></cts-time>
                  </span>
                </span>
              `
            : nothing}
          ${planHref
            ? html`
                <span class="cts-log-card-meta-item">
                  <span class="cts-log-card-meta-key">Plan</span>
                  <a class="cts-log-card-plan-link" href="${planHref}"
                    >${log.planName || log.planId}</a
                  >
                </span>
              `
            : nothing}
          ${showOwner
            ? html`
                <span class="cts-log-card-meta-item">
                  <span class="cts-log-card-meta-key">Owner</span>
                  ${this._renderOwner(log.owner)}
                </span>
              `
            : nothing}
          <span class="cts-log-card-actions">
            ${showConfig
              ? html`
                  <cts-tooltip content="View configuration JSON" placement="top">
                    <cts-button
                      class="showConfigBtn"
                      variant="ghost"
                      size="sm"
                      icon="settings"
                      label="Config"
                      data-test-id="${log.testId}"
                      data-plan-id="${log.planId || ""}"
                      @cts-click=${this._handleConfigButtonClick}
                    ></cts-button>
                  </cts-tooltip>
                `
              : nothing}
          </span>
        </div>
      </article>
    `;
  }

  _handleConfigButtonClick(event) {
    event.stopPropagation();
    const trigger = event.currentTarget;
    const planId = trigger.dataset.planId;
    const testId = trigger.dataset.testId;
    if (!planId) return;
    const publicSuffix = this.isPublic ? "?public=true" : "";
    fetch(`/api/plan/${encodeURIComponent(planId)}${publicSuffix}`)
      .then((response) => {
        if (!response.ok) {
          return Promise.reject(response);
        }
        return response.json();
      })
      .then((jsonData) => {
        this._selectedConfig = jsonData && jsonData.config ? jsonData.config : {};
        this._selectedTestId = testId || "";
        this.updateComplete.then(() => {
          const modal = /** @type {HTMLElement & { show?: () => void }} */ (
            this.querySelector("#cts-log-list-config-modal")
          );
          if (modal && typeof modal.show === "function") modal.show();
        });
      })
      .catch((err) => {
        const fapi = /** @type {any} */ (window).FAPI_UI;
        if (fapi && typeof fapi.showError === "function") {
          const body =
            err && err.status
              ? { code: err.status, error: err.statusText || "Failed to load configuration" }
              : { code: 0, error: String(err) };
          fapi.showError(body);
        }
      });
  }

  async _handleCopyConfig(event) {
    const trigger = event && event.currentTarget;
    if (!this._selectedConfig) return;
    try {
      await navigator.clipboard.writeText(JSON.stringify(this._selectedConfig, null, 4));
    } catch (err) {
      console.warn("[cts-log-list] clipboard.writeText failed:", err);
      return;
    }
    flashCopyConfirmed(trigger);
  }

  _renderConfigModal() {
    const configJson = this._selectedConfig ? JSON.stringify(this._selectedConfig, null, 4) : "";
    return html`
      <cts-modal id="cts-log-list-config-modal" heading="Configuration" size="xl">
        <div class="cts-log-list-config-toolbar">
          <span>
            <span class="text-muted">Test ID:</span>
            <code id="cts-log-list-config-test-id">${this._selectedTestId}</code>
          </span>
          <cts-tooltip content="Copy configuration JSON to clipboard" placement="top">
            <cts-button
              class="btn-clipboard copy-config-btn"
              variant="secondary"
              size="sm"
              icon="copy"
              label="Copy configuration"
              @cts-click=${this._handleCopyConfig}
            ></cts-button>
          </cts-tooltip>
        </div>
        <cts-json-view
          id="cts-log-list-config-editor"
          class="config-json"
          aria-label="Test configuration JSON"
          .value=${configJson}
        ></cts-json-view>
      </cts-modal>
    `;
  }

  _renderLoading() {
    return html`
      <cts-loading-state label="Loading logs" data-testid="log-list-loading"></cts-loading-state>
    `;
  }

  _renderError() {
    return html`
      <cts-alert variant="danger" role="alert"> <strong>Error:</strong> ${this._error} </cts-alert>
    `;
  }

  /**
   * Render the empty state, branched by why the list is empty:
   * - a filter is active → "widen the search" (no action — the list is
   *   filtered, not empty);
   * - the My view is empty → orienting copy plus a secondary
   *   "View published logs" action, so an empty personal list still offers
   *   something to look at;
   * - the Published view is empty → orienting copy only. No published-logs
   *   action here — it would link to the view the user is already on.
   * @param {boolean} hasFilter - Whether a status/search filter is active.
   * @returns {import('lit').TemplateResult} The empty-state template.
   */
  _renderEmpty(hasFilter) {
    const heading = hasFilter ? "No logs match the active filter" : "No logs to show";
    const body = hasFilter
      ? "Try clearing one or more filters to widen the search."
      : "Logs will appear here as tests are scheduled.";
    const offerPublishedLogs = !hasFilter && !this.isPublic;
    return html`
      <cts-empty-state
        icon="folder"
        heading="${heading}"
        body="${body}"
        secondary-cta-label="${ifDefined(offerPublishedLogs ? "View published logs" : undefined)}"
        secondary-cta-href="${ifDefined(offerPublishedLogs ? "logs.html?public=true" : undefined)}"
        data-testid="log-list-empty"
      ></cts-empty-state>
    `;
  }

  render() {
    // The toolbar (search + filter dropdown) is rendered once, outside the
    // loading/error/loaded branch, so its node identity is stable. This keeps
    // the popover panel from being torn out of the top layer mid-open when the
    // fetch resolves and the body swaps from loading to the list — a branch
    // swap would otherwise orphan an open popover and leave aria-expanded stuck.
    return html` ${this._renderSearchAndSort()} ${this._renderBody()} `;
  }

  _renderBody() {
    if (this._loading) {
      return this._renderLoading();
    }
    if (this._error) {
      return this._renderError();
    }
    const rows = this._logs;
    const hasFilter =
      this._statusFilter.size > 0 ||
      this._resultFilter.size > 0 ||
      this._searchText.trim().length > 0;

    return html`
      ${this._renderActiveFilterSummary()}
      ${rows.length === 0
        ? this._renderEmpty(hasFilter)
        : html`
            <div
              class=${classMap({ "cts-log-list-items": true, "is-refreshing": this._refreshing })}
              data-testid="log-list-items"
              aria-busy=${this._refreshing ? "true" : "false"}
            >
              ${repeat(
                rows,
                (log) => log.testId,
                (log) => this._renderCard(log),
              )}
            </div>
          `}
      <div class="cts-log-list-footer">
        ${this._refreshing
          ? html`<p class="cts-log-list-refreshing" role="status" data-testid="log-list-refreshing">
              Updating…
            </p>`
          : nothing}
        ${this._hasMore
          ? html`
              <cts-button
                variant="secondary"
                size="md"
                data-testid="log-list-show-more"
                label="${this._loadingMore ? "Loading…" : `Show more (${rows.length} loaded)`}"
                ?disabled=${this._loadingMore || this._refreshing}
                @cts-click=${this._handleShowMoreClick}
              ></cts-button>
            `
          : nothing}
      </div>
      ${this._renderConfigModal()}
    `;
  }
}

customElements.define("cts-log-list", CtsLogList);
