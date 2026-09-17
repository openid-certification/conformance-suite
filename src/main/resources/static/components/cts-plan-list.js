import { LitElement, html, nothing, css, unsafeCSS } from "lit";
import { repeat } from "lit/directives/repeat.js";
import { ifDefined } from "lit/directives/if-defined.js";
import { classMap } from "lit/directives/class-map.js";
import "./cts-button.js";
import "./cts-icon.js";
import "./cts-modal.js";
import "./cts-alert.js";
import "./cts-tooltip.js";
import "./cts-time.js";
import "./cts-empty-state.js";
import "./cts-loading-state.js";
import "./cts-json-view.js";
import "./cts-spinner.js";
import "./cts-badge.js";
import { flashCopyConfirmed } from "../js/cts-copy-flash.js";
import "./cts-plan-status.js";
import {
  emptyFilter,
  hasFilters,
  text,
  toChips,
  toParams,
  urlFromFilter,
  without,
} from "./plan-list-filter.js";
import { listingParams, readListingPage } from "../lib/listing-request.js";

// The sort selector's options, as the server's `order` parameter. The name
// sort falls back to newest-first so plans of one name come out in a stable,
// useful order.
const SORT_ORDERS = {
  "started-desc": "started,desc",
  "started-asc": "started,asc",
  "name-asc": "planName,asc,started,desc",
};

// How long after the last keystroke the search box asks the server.
const SEARCH_DEBOUNCE_MS = 300;

/** How often to ask how far a running bulk delete has got. */
const BULK_POLL_MS = 2000;

/** What the dialog says while the two slow server round-trips are happening. */
const COUNTING = "Counting the plans this would delete.";
const STARTING = "Checking that count still holds, then starting.";

/**
 * The ages the listing can be narrowed to, as whole years back from today. Age
 * is the one thing an operator pruning a database actually filters on, and it
 * is otherwise only expressible by editing `to=` into the URL by hand.
 * @type {Array<{value: string, label: string, years: number}>}
 */
const AGE_PRESETS = [
  { value: "1y", label: "Over 1 year ago", years: 1 },
  { value: "2y", label: "Over 2 years ago", years: 2 },
  { value: "3y", label: "Over 3 years ago", years: 3 },
  { value: "5y", label: "Over 5 years ago", years: 5 },
];

/**
 * @param {number} years - How many whole years back.
 * @returns {string} That date as `YYYY-MM-DD`, which is what `to` takes.
 */
function yearsAgo(years) {
  const date = new Date();
  date.setFullYear(date.getFullYear() - years);
  return date.toISOString().slice(0, 10);
}

const STYLE_ID = "cts-plan-list-styles";

// Inline SVG chevron used as the custom select indicator. Copied from
// cts-log-list (which copied cts-form-field's `.oidf-select`). Stroke
// colour is `--ink-500` (`#71695E`), encoded as `%2371695E`.
const SELECT_CHEVRON =
  "url(\"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 16 16'><path fill='none' stroke='%2371695E' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' d='M4 6l4 4 4-4'/></svg>\")";

// Scoped CSS. The toolbar + card chrome is mirrored from cts-log-list.js as
// of this change so the plans listing reads as a sibling of the logs
// listing (same search/sort affordances, same card silhouette and
// hierarchy). The two components deliberately do NOT share a base class —
// they diverge (logs carry status/result filter chips + URL sync; plans
// carry the module badge stack + config modal). Visual drift between the
// two card styles is an accepted cost until a third listing motivates a
// shared `cts-listing-base`. If you restyle the cards here, mirror the
// change in cts-log-list.js (and vice versa).
const STYLE_TEXT = css`
  cts-plan-list {
    display: block;
    font-family: var(--font-sans);
    color: var(--fg);
  }
  /* The owner pill is the link; it carries its own chip affordance, so the
     anchor adds none of its own. */
  .cts-plan-card-owner-link {
    text-decoration: none;
    color: inherit;
  }
  .cts-plan-card-owner-link:hover .ownerSub,
  .cts-plan-card-owner-link:hover .ownerIss {
    text-decoration: underline;
  }
  .cts-plan-list-bulk-limit {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
    margin-bottom: var(--space-3);
  }
  .cts-plan-list-bulk-limit input {
    width: 7ch;
  }
  .cts-plan-list-toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    align-items: center;
    margin-bottom: var(--space-4);
  }
  /* What the listing has been narrowed to (a drill-down from the statistics
     charts, or a shared link). Above the search box, because it scopes the
     dataset the search then searches. */
  .cts-plan-list-filters {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    align-items: center;
    margin-bottom: var(--space-3);
  }
  .cts-plan-list-filters-label {
    font-size: var(--fs-13);
    color: var(--fg-soft);
  }
  .cts-plan-list-filters-clear {
    padding: 0;
    border: 0;
    background: none;
    font: inherit;
    font-size: var(--fs-13);
    color: var(--fg-link);
    text-decoration-line: underline;
    text-decoration-thickness: 1px;
    text-underline-offset: 2px;
    text-decoration-color: var(--link-decoration-color);
    cursor: pointer;
  }
  .cts-plan-list-filters-clear:hover {
    text-decoration-color: currentColor;
  }
  .cts-plan-list-filters-clear:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
    border-radius: var(--radius-2);
  }
  .cts-plan-list-search {
    position: relative;
    flex: 1 1 280px;
    min-width: 220px;
  }
  .cts-plan-list-search input {
    width: 100%;
    box-sizing: border-box;
    /* border-box + --control-height so the bordered input is 34px outer (was
       content-sized ~36px), aligning with adjacent default-size controls. */
    height: var(--control-height);
    padding: var(--space-1) var(--space-3) var(--space-1) calc(var(--space-3) + var(--space-6));
    background: var(--bg);
    color: var(--fg);
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    font-family: var(--font-sans);
    font-size: var(--fs-14);
    line-height: var(--control-height);
  }
  .cts-plan-list-search input:focus {
    outline: none;
    border-color: var(--border-strong);
    box-shadow: var(--focus-ring);
  }
  .cts-plan-list-search cts-icon {
    position: absolute;
    left: var(--space-3);
    top: 50%;
    transform: translateY(-50%);
    color: var(--fg-soft);
    pointer-events: none;
  }
  .cts-plan-list-sort,
  .cts-plan-list-age {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--fs-13);
    color: var(--fg-soft);
  }
  .cts-plan-list-sort select,
  .cts-plan-list-age select {
    box-sizing: border-box;
    height: var(--control-height);
    padding: 0 36px 0 var(--space-3);
    background: var(--bg-elev);
    color: var(--fg);
    border: 1px solid var(--ink-300);
    border-radius: var(--radius-2);
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    line-height: 1;
    appearance: none;
    -webkit-appearance: none;
    background-image: ${unsafeCSS(SELECT_CHEVRON)};
    background-repeat: no-repeat;
    background-position: right 12px center;
  }
  /* the copy of these rules for the filter selects dropped this one, so they had no focus ring
     while Sort had one */
  .cts-plan-list-sort select:focus,
  .cts-plan-list-age select:focus {
    outline: none;
    border-color: var(--orange-400);
    box-shadow: var(--focus-ring);
  }
  .cts-plan-list-items {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }
  /* Adrian Roselli "block link" pattern (pseudo-element overlay): the card
     root is a non-interactive article; the plan-name headline is the single
     real anchor per card; its ::after overlay covers the whole card so the
     click target spans the card silhouette. Nested interactive controls
     (config button, owner pills) sit on z-index: 1 so they receive their own
     clicks. The cts-plan-status overview bar also sits at z-index: 1 (its
     segments need hover for tooltips) but is pointer-events: none except on the
     tooltip-wrapped segments, so clicks in its band still fall through to the
     card link (that pointer-events split lives in cts-plan-status's own scoped
     CSS, keyed on mode="overview"). See https://adrianroselli.com/2020/02/block-links-cards-clickable-regions-etc.html */
  .cts-plan-card {
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
  .cts-plan-card:hover {
    border-color: var(--border-strong);
    background: var(--bg);
  }
  .cts-plan-card:focus-within {
    outline: none;
    box-shadow: var(--focus-ring);
    border-color: var(--border-strong);
  }
  .cts-plan-card-header {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-3);
  }
  .cts-plan-card-identity {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    min-width: 0;
    flex: 1 1 280px;
  }
  .cts-plan-card-name {
    display: inline-block;
    font-size: var(--fs-16);
    line-height: var(--lh-snug);
    font-weight: var(--fw-bold);
    color: var(--fg);
    text-decoration-line: none;
    word-break: break-word;
  }
  .cts-plan-card-name::after {
    content: "";
    position: absolute;
    inset: 0;
    border-radius: inherit;
  }
  .cts-plan-card-name:hover,
  .cts-plan-card-name:focus-visible {
    text-decoration-line: none;
  }
  .cts-plan-card-name:focus-visible {
    outline: none;
  }
  .cts-plan-card-slug {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    line-height: var(--lh-snug);
    color: var(--fg-soft);
    word-break: break-all;
  }
  .cts-plan-card-description {
    margin: 0;
    color: var(--fg-soft);
    font-size: var(--fs-14);
    line-height: var(--lh-snug);
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
  /* The per-plan module status overview is a <cts-plan-status mode="overview">
     bar — small color-coded segments (one per module), each wrapped in a
     tooltip that reveals the module + status on hover. The component owns its
     own scoped CSS (segment palette, the z-index:1 lift above the card-link
     ::after overlay, and the pointer-events split that lets clicks in its band
     fall through to the card link). Clicking a segment does nothing — the card
     stays the single click target (R12/R16). */
  .cts-plan-card-meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-1) var(--space-4);
    font-size: var(--fs-13);
    line-height: var(--lh-snug);
    color: var(--fg-soft);
  }
  .cts-plan-card-meta-item {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
  }
  .cts-plan-card-meta-key {
    color: var(--fg-soft);
    font-weight: var(--fw-medium);
  }
  .cts-plan-card-meta-value {
    color: var(--fg);
  }
  .cts-plan-card-meta-value.is-mono {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
  }
  /* Nested controls lift above the headline link's ::after overlay so the
     browser routes clicks on them to the control, not the card link. */
  .cts-plan-card .showConfigBtn,
  .cts-plan-card .plan-owner {
    position: relative;
    z-index: 1;
  }
  .cts-plan-card-actions {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin-left: auto;
  }
  /* Owner pill — markup mirrors cts-log-list / templates/owner.html so the
     two-tone chip matches the rest of the suite. */
  .cts-plan-card .plan-owner {
    display: inline-flex;
    flex-wrap: nowrap;
    align-items: center;
    gap: 0;
  }
  .cts-plan-card .ownerSub,
  .cts-plan-card .ownerIss {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 2px;
    background: var(--bg);
    border: 1px solid var(--border);
    color: var(--fg-soft);
  }
  .cts-plan-card .ownerSub {
    border-top-left-radius: var(--radius-pill);
    border-bottom-left-radius: var(--radius-pill);
    border-right: none;
  }
  .cts-plan-card .ownerIss {
    border-top-right-radius: var(--radius-pill);
    border-bottom-right-radius: var(--radius-pill);
  }
  .cts-plan-card .ownerSub:focus-visible,
  .cts-plan-card .ownerIss:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  .cts-plan-list-empty {
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
  .cts-plan-list-items.is-refreshing {
    opacity: 0.6;
    pointer-events: none;
    transition: opacity 150ms ease-out;
  }
  .cts-plan-list-refreshing {
    margin: 0;
    color: var(--fg-soft);
    font-size: var(--fs-13);
  }
  .cts-plan-list-footer {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);
    margin-top: var(--space-4);
    color: var(--fg-soft);
    font-size: var(--fs-13);
  }
  .cts-plan-list-config-toolbar {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin-bottom: var(--space-2);
  }
  .cts-plan-list-config-toolbar code {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    color: var(--fg-soft);
    background: var(--ink-50);
    padding: 1px 6px;
    border-radius: var(--radius-1);
  }
  /* '.planConfigJson' / '.config-json' are pre-existing class names the
     cts-plan-list.stories.js ViewConfig play function queries to read the
     view value. Do not rename without updating that story. */
  cts-plan-list .planConfigJson {
    display: block;
    margin: 0;
    max-height: 60vh;
    min-height: calc(var(--space-6) * 14);
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/**
 * Whether a plan row carries a saved configuration worth opening. The
 * backend (DBTestPlanService) always writes a `config` field; plans
 * created without configuration come over the wire as `config: {}`,
 * which yields an empty modal if surfaced.
 * @param {unknown} config - Row payload's `config` value (any wire shape).
 * @returns {boolean} `true` when `config` is an object with at least one key.
 */
function hasNonEmptyConfig(config) {
  return !!config && typeof config === "object" && Object.keys(config).length > 0;
}

/**
 * What to tell the reader about a listing request the server refused.
 *
 * `TestPlanApi` answers a filter it cannot use with 400 and
 * `{"error": "<which parameter and why>"}` — the only thing that says WHICH
 * chip to remove — so that message is preferred over the bare status code.
 * `message` is accepted alongside it because the statistics endpoint words its
 * own 400 that way and a mock may do either. Anything else (an empty body, a
 * proxy's HTML error page, a 500) falls back to the status.
 * @param {Response} response - The failed response.
 * @returns {Promise<string>} The message for the alert.
 */
/**
 * The message a failed response carries, if it carries one: the API answers `{"error": "..."}`
 * — the only thing that says WHICH parameter is wrong — and `message` is accepted alongside it
 * because the statistics endpoint words its own 400 that way.
 * @param {Response} response - The failed response.
 * @returns {Promise<string|null>} That message, or null when there is none to show.
 */
async function serverMessage(response) {
  let body = null;
  try {
    body = await response.json();
  } catch {
    // not JSON at all; the status code is all there is
  }
  return body && typeof body === "object" ? body.error || body.message || null : null;
}

/**
 * @param {Response} response - The failed response.
 * @returns {Promise<string>} The message for the listing's alert.
 */
async function failureMessage(response) {
  const detail = await serverMessage(response);
  return detail
    ? `Failed to load test plans: ${detail}`
    : `Failed to load test plans (HTTP ${response.status})`;
}

/**
 * The bulk delete's 400 says which parameter is wrong or what `confirm` should have been, and
 * its 409 says a delete is already running — all worth showing verbatim.
 * @param {Response} response - The failed response.
 * @returns {Promise<string>} The message for the dialog's alert.
 */
async function bulkFailureMessage(response) {
  const detail = await serverMessage(response);
  if (detail) return detail;
  return response.status === 403
    ? "Only an admin can delete plans in bulk"
    : `Deleting failed (HTTP ${response.status})`;
}

function formatVariant(variant) {
  if (!variant) return "";
  if (typeof variant === "string") return variant;
  return Object.entries(variant)
    .map(([key, value]) => `${key}=${value}`)
    .join(", ");
}

/**
 * Searchable, sortable list of test plans. Fetches from `/api/plan` (or
 * `/api/plan?public=true`) and renders a single-column card layout mirroring
 * `cts-log-list`: a top toolbar (search box + sort selector), block-link
 * cards (plan name headline, plan id slug, description, per-module status
 * overview, metadata row), "Show more" pagination, and a config-viewer modal.
 *
 * The SERVER does the work: the filters, the search term and the sort order
 * all travel with the `/api/plan` request, which returns one page of rows;
 * "Show more" asks for the next page and appends it. The search box IS the
 * `search` filter - what it holds is what the server matched (a MongoDB
 * `$text` phrase over the plan name, description and certification profile,
 * whole words only), so the listing, its chip and a bulk delete all mean the
 * same set of plans.
 *
 * Each card's per-module status is a `<cts-plan-status mode="overview">` bar —
 * one color-coded segment per module, each wrapped in a tooltip that reveals
 * the module + status on hover. Every row of the listing already carries the
 * `{status, result}` of each module's latest run, so rendering a page needs
 * no further requests; the shared `segmentVariant` helper maps that data to a
 * segment colour.
 *
 * Light DOM. Scoped CSS is injected once on first connect.
 *
 * @property {boolean} isAdmin - Reveals the Owner pill on each card when set.
 *   Reflects the `is-admin` attribute. Ignored when `isPublic` is true.
 * @property {boolean} isPublic - Fetches the published plan listing and hides
 *   admin affordances (Owner pill, Config button). Reflects the `is-public`
 *   attribute.
 * @property {boolean} deferInitialFetch - When set, suppresses the
 *   connect-time `/api/plan` fetch so the page can resolve the auth-dependent
 *   default (My for authed, Published for anon) before fetching, then trigger
 *   it via `fetchPlans()`. Reflects the `defer-initial-fetch` attribute (KTD3).
 * @property {import("./plan-list-filter.js").PlanListFilter} filters - What
 *   the listing is narrowed to: `family`, `plan`, plan-level `variant`
 *   values, `cert` and the `from`/`to` bounds on `started`, plus the
 *   `search` term the search box holds. Forwarded to `GET /api/plan` (the
 *   SERVER applies them) and rendered as one removable chip each above the
 *   toolbar.
 *   Property-only, because it is an object: `plans.html` reads it out of
 *   `location.search` with `planListFilterFromUrl()` and assigns it
 *   BEFORE the element upgrades, exactly as it sets `is-public`, so the
 *   connect-time fetch already carries the filter. Removing a chip rewrites
 *   the page URL (`replaceState`) and refetches.
 *
 * DOM hooks for e2e (`data-testid`): `plan-list-item`, `plan-list-link`,
 * `plan-list-items`, `plan-list-empty`, `plan-list-show-more`, plus the
 * filter row's `plan-filters`,
 * `plan-filter-<key>` (`family`, `plan`, `cert`, `from`, `variant-<name>`)
 * and `plan-filters-clear`.
 * @fires cts-plan-navigate - When a plan name is clicked, with
 *   `{ detail: { planId } }`; bubbles and is composed.
 */
class CtsPlanList extends LitElement {
  static properties = {
    isAdmin: { type: Boolean, attribute: "is-admin" },
    isPublic: { type: Boolean, attribute: "is-public" },
    deferInitialFetch: { type: Boolean, attribute: "defer-initial-fetch" },
    filters: { attribute: false },
    _plans: { state: true },
    _loading: { state: true },
    _refreshing: { state: true },
    _loadingMore: { state: true },
    _hasMore: { state: true },
    _error: { state: true },
    _sortKey: { state: true },
    _selectedConfig: { state: true },
    _selectedPlanId: { state: true },
    _bulkPreview: { state: true },
    _bulkLimit: { state: true },
    _bulkProgress: { state: true },
    _bulkError: { state: true },
    _bulkBusy: { state: true },
    _bulkBusyLabel: { state: true },
    _filterOptions: { state: true },
  };

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  constructor() {
    super();
    this.isAdmin = false;
    this.isPublic = false;
    this.deferInitialFetch = false;
    /** @type {import("./plan-list-filter.js").PlanListFilter} */
    this.filters = emptyFilter();
    this._plans = [];
    this._loading = true;
    this._refreshing = false;
    this._loadingMore = false;
    this._hasMore = false;
    this._error = null;
    this._sortKey = "started-desc";
    this._selectedConfig = null;
    this._selectedPlanId = "";
    /** @type {{listed: number, deletable: number, kept: number, target: number}|null} */
    this._bulkPreview = null;
    // A limit by default, because the first thing anyone should do with this is
    // delete a small batch and look at the result
    this._bulkLimit = "100";
    /** @type {{state: string, plans: number, tests: number, logEntries: number, target: number|null}|null} */
    this._bulkProgress = null;
    this._bulkError = null;
    this._bulkBusy = false;
    this._bulkBusyLabel = COUNTING;
    // set when a finished job means the listing is out of date, acted on when
    // the dialog closes
    this._bulkNeedsRefresh = false;
    /**
     * @type {{
     *   families: Array<string>,
     *   plans: Array<{name: string, family: string, retired: boolean}>,
     *   variants?: Record<string, Record<string, Array<string>>>,
     * }|null}
     */
    this._filterOptions = null;
    // Non-reactive: the handle of the status poll, cleared on disconnect
    /** @type {ReturnType<typeof setTimeout>|undefined} */
    this._bulkPollTimer = undefined;
    // The pending search-box debounce, cleared on disconnect. Non-reactive.
    /** @type {ReturnType<typeof setTimeout>|undefined} */
    this._searchTimer = undefined;
    // Monotonic id of the most recent listing request. Two filter changes in
    // quick succession (a chip removed, then another) are two fetches with no
    // ordering guarantee between them, and the loser must not overwrite the
    // winner's rows — or clear its loading state. Non-reactive.
    this._fetchSeq = 0;
    // Pre-bind handlers wired through Lit EventParts on rendered cards. Lit
    // dispatches with `this` set to the host element of the listener; these
    // must retain this component as `this`.
    this._handleSearchInput = this._handleSearchInput.bind(this);
    this._handleSearchCommit = this._handleSearchCommit.bind(this);
    this._handleSortChange = this._handleSortChange.bind(this);
    this._handleShowMoreClick = this._handleShowMoreClick.bind(this);
    this._handlePlanLinkClick = this._handlePlanLinkClick.bind(this);
    this._handleConfigButtonClick = this._handleConfigButtonClick.bind(this);
    this._handleCopyConfig = this._handleCopyConfig.bind(this);
    this._handleChipRemove = this._handleChipRemove.bind(this);
    this._handleClearFilters = this._handleClearFilters.bind(this);
  }

  connectedCallback() {
    super.connectedCallback();
    // The host is the stable wrapper that persists across loading→loaded
    // renders, so announce dataset changes (e.g. a My⇄Published tab swap)
    // here rather than on a fragment that re-creates itself each render
    // (R17/R19). The list region is supplementary, so a polite live region is
    // appropriate.
    this.setAttribute("aria-live", "polite");
    this._loadFilterOptions();
    // KTD3: on the no-`public`-param path the page sets `defer-initial-fetch`
    // synchronously so the auth-dependent default resolves before fetching.
    // The list still renders its loading state (`_loading` defaults true) in
    // the meantime; the page calls `fetchPlans()` once auth resolves.
    if (!this.deferInitialFetch) {
      this._fetchPlans({ fresh: true });
    }
  }

  /**
   * Public entry point the page calls after `/api/currentuser` resolves on the
   * deferred (no-`public`-param) first-paint path (KTD3), and on every
   * `cts-view-tab-change` (My⇄Published switch / back-forward). Delegates to
   * the internal fetch, which flips `_loading` true (re-rendering the loading
   * state) and then fetches the dataset selected by the current `isPublic`.
   * @returns {Promise<void>} Resolves once the fetch settles.
   */
  fetchPlans() {
    return this._fetchPlans({ fresh: true });
  }

  updated() {
    // Every filter control carries the value it should show as `data-value`; it is applied
    // here because a <select> cannot be set before its options exist (see _renderFilterSelect).
    for (const select of this.querySelectorAll("select[data-value]")) {
      if (select instanceof HTMLSelectElement) {
        select.value = select.dataset.value ?? "";
      }
    }
  }

  /**
   * Ask the server for a page: the first page of the current filters and
   * sort, or - on "Show more" - the page after the rows already shown, which
   * is appended to them.
   *
   * A re-query keeps the rows it is about to replace on screen, dimmed, so
   * a sort, a search or a chip does not flash the list through the spinner;
   * the spinner is for when there is nothing to keep - the first load, and
   * a My/Published swap (`fresh`), where the old rows are the wrong dataset.
   * @param {{append?: boolean, fresh?: boolean}} [options] - `append` for the
   *   next page; `fresh` to show the loading state in place of the rows.
   * @returns {Promise<void>} Resolves once the fetch settles.
   */
  async _fetchPlans({ append = false, fresh = false } = {}) {
    const seq = ++this._fetchSeq;
    const start = append ? this._plans.length : 0;
    if (append) {
      this._loadingMore = true;
    } else if (fresh || this._plans.length === 0) {
      this._loading = true;
    } else {
      this._refreshing = true;
    }
    this._error = null;
    try {
      // The filters - the search term among them - are applied by the SERVER,
      // inside the same owner/admin/public scoping as an unfiltered listing:
      // they can only narrow what this user could already see.
      const params = listingParams({
        start,
        order: SORT_ORDERS[this._sortKey] || SORT_ORDERS["started-desc"],
        isPublic: this.isPublic,
        extra: toParams(this.filters),
      });
      const response = await fetch(`/api/plan?${params}`);
      // A later request has already been made, so this answer is stale
      // whatever it says: dropping it here is what keeps two quick chip
      // removals from landing out of order.
      if (seq !== this._fetchSeq) return;
      if (!response.ok) {
        throw new Error(await failureMessage(response));
      }
      const payload = await response.json();
      if (seq !== this._fetchSeq) return;
      const { rows, hasMore } = readListingPage(payload, start);
      // The server resolved every module's latest run as part of the listing,
      // so a module with no status now is one whose run could not be found -
      // the neutral segment, not a pending one.
      for (const plan of rows) {
        for (const mod of plan.modules || []) mod._statusResolved = true;
      }
      this._plans = append ? [...this._plans, ...rows] : rows;
      this._hasMore = hasMore;
    } catch (err) {
      if (seq !== this._fetchSeq) return;
      this._error = err instanceof Error ? err.message : String(err);
      this._plans = [];
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

  _handlePlanClick(planId) {
    this.dispatchEvent(
      new CustomEvent("cts-plan-navigate", {
        bubbles: true,
        composed: true,
        detail: { planId },
      }),
    );
  }

  _handlePlanLinkClick(event) {
    // Let the browser handle modifier-key clicks (cmd/ctrl/shift/alt) and
    // non-primary mouse buttons natively so "open in new tab/window" works.
    if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0) {
      return;
    }
    event.preventDefault();
    const planId = event.currentTarget.dataset.planId;
    this._handlePlanClick(planId);
  }

  _handleConfigClick(plan) {
    this._selectedConfig = plan.config;
    this._selectedPlanId = plan._id;
    this.updateComplete.then(() => {
      const modal = /** @type {HTMLElement & { show?: () => void }} */ (
        this.querySelector("#planConfigModal")
      );
      if (modal && typeof modal.show === "function") modal.show();
    });
  }

  /**
   * The parameters that say WHICH plans a bulk delete is about: the same ones
   * the listing itself was fetched with, so what is deleted is what is on
   * screen, plus the limit.
   * @returns {URLSearchParams} Those parameters.
   */
  _bulkParams() {
    // toParams already returns a fresh URLSearchParams, so there is nothing to copy
    const params = toParams(this.filters);
    const limit = Number.parseInt(this._bulkLimit, 10);
    if (Number.isFinite(limit) && limit > 0) params.set("limit", String(limit));
    return params;
  }

  /**
   * Ask what deleting would do, then open the confirmation. Nothing is deleted
   * until the button in the dialog is pressed.
   * @returns {Promise<void>} When the dialog is open.
   */
  async _openBulkDelete() {
    this._bulkError = null;
    this._bulkProgress = null;
    this._bulkPreview = null;
    this._bulkBusy = true;
    this._bulkBusyLabel = COUNTING;
    await this.updateComplete;
    // the element may not have upgraded yet on a quick click, and an un-upgraded
    // one has no show(): waiting for the definition is what stops the button
    // doing nothing at all
    await customElements.whenDefined("cts-modal");
    const modal = /** @type {HTMLElement & { show?: () => void }} */ (
      this.querySelector("#planBulkDeleteModal")
    );
    if (modal && typeof modal.show === "function") modal.show();
    await this._loadBulkPreview();
  }

  /** @returns {Promise<void>} When the count is in, or the error is shown. */
  async _loadBulkPreview() {
    this._bulkBusy = true;
    this._bulkBusyLabel = COUNTING;
    this._bulkError = null;
    try {
      const response = await fetch(`/api/plan/delete-preview?${this._bulkParams()}`);
      if (!response.ok) throw new Error(await bulkFailureMessage(response));
      this._bulkPreview = await response.json();
    } catch (err) {
      this._bulkPreview = null;
      this._bulkError = err instanceof Error ? err.message : String(err);
    } finally {
      this._bulkBusy = false;
    }
  }

  /**
   * @param {Event} event - The input event from the limit box.
   * @returns {void}
   */
  _handleBulkLimitInput(event) {
    this._bulkLimit = /** @type {HTMLInputElement} */ (event.target).value;
  }

  /**
   * How many plans the delete would remove: what the preview said is deletable,
   * capped by the limit. Derived rather than re-counted, because neither
   * `listed` nor `deletable` depends on the limit - only this does - and the
   * count behind them is a scan of every plan, which is far too slow to repeat
   * on a keystroke. The server recomputes the same number and refuses the
   * delete if it disagrees.
   * @returns {number} The number to confirm, and to put on the button.
   */
  _bulkTarget() {
    if (!this._bulkPreview) return 0;
    const limit = Number.parseInt(this._bulkLimit, 10);
    return Number.isFinite(limit) && limit > 0
      ? Math.min(this._bulkPreview.deletable, limit)
      : this._bulkPreview.deletable;
  }

  /**
   * Start deleting. `confirm` is the number the dialog just showed, so a
   * listing that moved since then stops the request instead of deleting
   * something else.
   * @returns {Promise<void>} When the job has started, or the error is shown.
   */
  async _confirmBulkDelete() {
    if (!this._bulkPreview) return;
    this._bulkBusy = true;
    // the server counts again before it accepts the confirmation, which on a big
    // database is another wait - so this must not look like nothing is happening
    this._bulkBusyLabel = STARTING;
    this._bulkError = null;
    try {
      const params = this._bulkParams();
      params.set("confirm", String(this._bulkTarget()));
      const response = await fetch(`/api/plan?${params}`, { method: "DELETE" });
      if (!response.ok) throw new Error(await bulkFailureMessage(response));
      this._bulkProgress = await response.json();
      this._pollBulkStatus();
    } catch (err) {
      this._bulkError = err instanceof Error ? err.message : String(err);
    } finally {
      this._bulkBusy = false;
    }
  }

  /**
   * Poll until the job stops, then reload the listing so the page shows what
   * is left.
   * @returns {void}
   */
  _pollBulkStatus() {
    clearTimeout(this._bulkPollTimer);
    this._bulkPollTimer = setTimeout(async () => {
      try {
        const response = await fetch("/api/plan/delete-status");
        if (!response.ok) throw new Error(await bulkFailureMessage(response));
        this._bulkProgress = await response.json();
      } catch (err) {
        this._bulkError = err instanceof Error ? err.message : String(err);
        return;
      }
      if (this._bulkProgress && this._bulkProgress.state === "RUNNING") {
        this._pollBulkStatus();
      } else {
        // NOT _fetchPlans() here: it flips the component into its loading state,
        // which unmounts this dialog mid-sentence and loses the only report of
        // what was deleted. The listing is refreshed when the dialog is closed.
        this._bulkNeedsRefresh = true;
      }
    }, BULK_POLL_MS);
  }

  /** @returns {Promise<void>} When the job has been asked to stop. */
  async _cancelBulkDelete() {
    try {
      const response = await fetch("/api/plan/delete-cancel", { method: "POST" });
      if (response.ok) this._bulkProgress = await response.json();
    } catch {
      // the poll reports what actually happened; a failed cancel is not worth
      // an error of its own
    }
  }

  disconnectedCallback() {
    clearTimeout(this._bulkPollTimer);
    clearTimeout(this._searchTimer);
    super.disconnectedCallback();
  }

  /**
   * Which age preset the current `to` bound corresponds to, so the select shows
   * what the listing is actually narrowed to after a reload or a drill-down.
   * @returns {string} A preset value, `"custom"` for a bound that is none of
   *   them, or `""` when the listing is not narrowed by age.
   */
  _agePreset() {
    const to = (this.filters && this.filters.to) || "";
    if (!to) return "";
    const match = AGE_PRESETS.find((preset) => yearsAgo(preset.years) === to);
    return match ? match.value : "custom";
  }

  /**
   * @param {Event} event - The change event from the age select.
   * @returns {void}
   */
  _handleAgeChange(event) {
    const value = /** @type {HTMLSelectElement} */ (event.target).value;
    const preset = AGE_PRESETS.find((candidate) => candidate.value === value);
    // "Custom period" is only ever shown for a bound that came from a URL, so
    // choosing it changes nothing; anything else sets or clears the bound
    if (value === "custom") return;
    this._applyFilters({ ...this.filters, to: preset ? yearsAgo(preset.years) : "" });
  }

  /**
   * The dialog has been closed, so the listing can be brought up to date now
   * without taking the result off the screen while it is still being read.
   * @returns {void}
   */
  _handleBulkDeleteClosed() {
    if (!this._bulkNeedsRefresh) return;
    this._bulkNeedsRefresh = false;
    this._bulkProgress = null;
    this._bulkPreview = null;
    this._fetchPlans();
  }

  /**
   * The families and plan names the listing can be narrowed to.
   *
   * A listing cannot offer these from what it has fetched: it holds at most a
   * thousand plans, while the values worth offering are every family the suite
   * has and every plan name it has ever published - most of a pruning listing
   * is made of names it no longer publishes at all. So they come from the
   * registry, through an endpoint that carries only the names.
   *
   * Asked for with `public=true` on the published listing, where the viewer
   * may not be logged in: the endpoint answers a public request with the same
   * registry data. Failure is still not an error: the two controls simply
   * stay hidden.
   * @returns {Promise<void>} When they are loaded, or given up on.
   */
  async _loadFilterOptions() {
    try {
      const response = await fetch(
        `/api/plan/filter-options${this.isPublic ? "?public=true" : ""}`,
      );
      if (!response.ok) return;
      const options = await response.json();
      if (Array.isArray(options?.families) && Array.isArray(options?.plans)) {
        this._filterOptions = options;
      }
    } catch {
      // offline, or an endpoint that is not there: the controls stay hidden
    }
  }

  /**
   * @returns {Array<{name: string, family: string, retired: boolean}>} The plan
   *   names worth offering: those of the chosen family, or all of them.
   */
  _plansToOffer() {
    const plans = this._filterOptions?.plans ?? [];
    const family = (this.filters && this.filters.family) || "";
    return family ? plans.filter((plan) => plan.family === family) : plans;
  }

  /**
   * @param {Event} event - The change event from the family select.
   * @returns {void}
   */
  _handleFamilyChange(event) {
    const family = /** @type {HTMLSelectElement} */ (event.target).value;
    const next = { ...this.filters, family };
    // a plan of another family would leave the two contradicting each other,
    // which the server answers with an empty listing rather than an error
    if (
      next.plan &&
      family &&
      !this._filterOptions?.plans.some((plan) => plan.name === next.plan && plan.family === family)
    ) {
      next.plan = "";
    }
    this._applyFilters(next);
  }

  /**
   * @param {Event} event - The change event from the immutable select.
   * @returns {void}
   */
  _handleImmutableChange(event) {
    const immutable = /** @type {HTMLSelectElement} */ (event.target).value;
    this._applyFilters({ ...this.filters, immutable });
  }

  /**
   * The variant parameters of the plan the listing is narrowed to, and the
   * values each may take.
   *
   * Only ever for ONE plan: a variant parameter means nothing without the plan
   * that defines it - `client_auth_type` belongs to a dozen plans and
   * `ciba_mode` to four - so these controls appear once a plan is chosen and
   * not before. A plan the suite no longer publishes has no entry, because the
   * registry is where the values come from; its variant chips still show if
   * the URL carries them.
   * @returns {Array<{name: string, values: Array<string>}>} Those parameters.
   */
  _variantsToOffer() {
    const plan = (this.filters && this.filters.plan) || "";
    const forPlan = plan ? this._filterOptions?.variants?.[plan] : null;
    if (!forPlan) return [];
    return Object.keys(forPlan)
      .sort()
      .map((name) => ({ name, values: forPlan[name] }));
  }

  /**
   * @param {Event} event - The change event from one of the variant selects.
   * @returns {void}
   */
  _handleVariantChange(event) {
    const select = /** @type {HTMLSelectElement} */ (event.target);
    const name = select.dataset.variant;
    if (!name) return;
    const variant = { ...(this.filters.variant || {}) };
    if (select.value) {
      variant[name] = select.value;
    } else {
      delete variant[name];
    }
    this._applyFilters({ ...this.filters, variant });
  }

  /**
   * @param {Event} event - The change event from the plan select.
   * @returns {void}
   */
  _handlePlanChange(event) {
    const plan = /** @type {HTMLSelectElement} */ (event.target).value;
    // a variant chosen for the previous plan may not exist on this one, and
    // would then match nothing at all
    const keep = plan ? (this._filterOptions?.variants?.[plan] ?? {}) : {};
    const variant = Object.fromEntries(
      Object.entries(this.filters.variant || {}).filter(([name, value]) =>
        (keep[name] || []).includes(value),
      ),
    );
    this._applyFilters({ ...this.filters, plan, variant });
  }

  _handleConfigButtonClick(event) {
    const planId = event.currentTarget.dataset.planId;
    const plan = this._plans.find((p) => p._id === planId);
    if (plan) this._handleConfigClick(plan);
  }

  async _handleCopyConfig(event) {
    // Capture currentTarget synchronously: the await below clears it
    // because event dispatch has completed by the time we resume.
    const trigger = event && event.currentTarget;
    if (!this._selectedConfig) return;
    try {
      await navigator.clipboard.writeText(JSON.stringify(this._selectedConfig, null, 4));
    } catch (err) {
      console.warn("[cts-plan-list] clipboard.writeText failed:", err);
      return;
    }
    flashCopyConfirmed(trigger);
  }

  /**
   * The search box is the `search` filter: what is typed goes to the server,
   * once the typing pauses rather than per keystroke.
   * @param {Event} event - The `input` event.
   * @returns {void}
   */
  _handleSearchInput(event) {
    const term = /** @type {HTMLInputElement} */ (event.target).value;
    clearTimeout(this._searchTimer);
    this._searchTimer = setTimeout(() => this._applySearch(term), SEARCH_DEBOUNCE_MS);
  }

  /**
   * Enter (or the box's own clear button) commits the term at once.
   * @param {Event} event - The `change` event.
   * @returns {void}
   */
  _handleSearchCommit(event) {
    clearTimeout(this._searchTimer);
    this._applySearch(/** @type {HTMLInputElement} */ (event.target).value);
  }

  /**
   * @param {string} term - What the search box holds.
   * @returns {void}
   */
  _applySearch(term) {
    const search = text(term);
    if (search === text(this.filters && this.filters.search)) return;
    this._applyFilters({ ...this.filters, search });
  }

  _handleSortChange(event) {
    this._sortKey = event.target.value;
    this._fetchPlans();
  }

  _handleShowMoreClick() {
    if (this._loadingMore || this._refreshing) return;
    this._fetchPlans({ append: true });
  }

  /**
   * Remove one filter: the chip that was clicked names it.
   * @param {Event} event - `cts-badge-click` from the chip.
   * @returns {void}
   */
  _handleChipRemove(event) {
    const key = /** @type {HTMLElement} */ (event.currentTarget).dataset.filterKey;
    if (!key) return;
    this._applyFilters(without(this.filters, key));
  }

  /**
   * Drop every filter at once.
   * @returns {void}
   */
  _handleClearFilters() {
    this._applyFilters(emptyFilter());
  }

  /**
   * Adopt a narrower (or wider) filter: mirror it into the page URL and
   * refetch, because the server is what applies it.
   *
   * `replaceState`, not `pushState`: removing a chip is a change to the view,
   * not navigation, and the way back to the statistics page that linked here
   * must stay one press of Back away. The rewrite preserves `?public`, so
   * clearing a filter cannot silently switch the Published tab back to My.
   * @param {import("./plan-list-filter.js").PlanListFilter} next - The filter to move to.
   * @returns {void}
   */
  _applyFilters(next) {
    this.filters = next;
    const search = urlFromFilter(next, window.location.search);
    window.history.replaceState(null, "", window.location.pathname + search + window.location.hash);
    this._fetchPlans();
  }

  /**
   * Render the per-module status overview for a card as a read-only
   * `<cts-plan-status mode="overview">`. The component reads each module's
   * `{status, result}`, which the listing row carries, via the shared
   * `segmentVariant` helper and initiates no fetches of its own (R5). A
   * no-module plan renders nothing (avoids an empty grid track adding a card
   * gap).
   * @param {object[]|undefined} modules - The plan's module entries.
   * @returns {import('lit').TemplateResult | typeof nothing} The overview bar.
   */
  _renderModuleStatus(modules) {
    if (!modules || modules.length === 0) return nothing;
    return html`<cts-plan-status mode="overview" .modules=${modules}></cts-plan-status>`;
  }

  _renderOwner(owner) {
    if (!owner) return nothing;
    const sub = owner.sub || "";
    const iss = owner.iss || "";
    return html`
      <span class="plan-owner">
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

  _renderCard(plan) {
    // In public mode the detail link must carry public=true so anonymous
    // cmd/middle-click ("open in new tab") resolves — plan-detail.html is
    // public ONLY with the param. Mirrors cts-log-list's detail-href handling.
    // The primary-click path is handled by the page's cts-plan-navigate handler,
    // which appends the same suffix.
    const publicSuffix = this.isPublic ? "&public=true" : "";
    const planHref = `plan-detail.html?plan=${encodeURIComponent(plan._id)}${publicSuffix}`;
    const variantString = formatVariant(plan.variant);
    const showOwner = !this.isPublic && this.isAdmin && plan.owner;
    // The public listing omits config server-side, so a Config button there
    // would open an empty modal; legacy plans.html gated it on !public too.
    const showConfig = !this.isPublic && hasNonEmptyConfig(plan.config);
    return html`
      <article class="cts-plan-card" data-testid="plan-list-item" data-plan-id="${plan._id}">
        <div class="cts-plan-card-header">
          <div class="cts-plan-card-identity">
            <a
              class="cts-plan-card-name plan-name-link"
              href="${planHref}"
              data-testid="plan-list-link"
              data-plan-id="${plan._id}"
              @click=${this._handlePlanLinkClick}
              >${plan.planName}</a
            >
            <span class="cts-plan-card-slug">${plan._id}</span>
          </div>
        </div>
        ${plan.description
          ? html`<p class="cts-plan-card-description">${plan.description}</p>`
          : nothing}
        ${this._renderModuleStatus(plan.modules)}
        <div class="cts-plan-card-meta">
          ${variantString
            ? html`
                <span class="cts-plan-card-meta-item">
                  <span class="cts-plan-card-meta-key">Variant</span>
                  <span class="cts-plan-card-meta-value is-mono">${variantString}</span>
                </span>
              `
            : nothing}
          ${plan.started
            ? html`
                <span class="cts-plan-card-meta-item">
                  <span class="cts-plan-card-meta-key">Started</span>
                  <span class="cts-plan-card-meta-value">
                    <cts-time mode="auto" value=${plan.started}></cts-time>
                  </span>
                </span>
              `
            : nothing}
          ${showOwner
            ? html`
                <span class="cts-plan-card-meta-item">
                  <span class="cts-plan-card-meta-key">Owner</span>
                  <a
                    class="cts-plan-card-owner-link"
                    href="plans.html${urlFromFilter(
                      {
                        ...this.filters,
                        // both halves: a sub names an account only within its issuer, and
                        // this same narrowing is what a bulk delete is aimed with
                        owner: plan.owner.sub || "",
                        owner_iss: plan.owner.iss || "",
                      },
                      window.location.search,
                    )}"
                    title="Show only this owner's plans"
                    data-testid="plan-owner-link"
                    >${this._renderOwner(plan.owner)}</a
                  >
                </span>
              `
            : nothing}
          <span class="cts-plan-card-actions">
            ${showConfig
              ? html`
                  <cts-tooltip content="View configuration JSON" placement="top">
                    <cts-button
                      class="showConfigBtn"
                      variant="ghost"
                      size="sm"
                      icon="settings"
                      label="View configuration"
                      data-plan-id="${plan._id}"
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

  /**
   * The "Filtered by" row: one removable chip per active filter, and a Clear
   * all shortcut once there is more than one to remove.
   *
   * Each chip is `clickable` (not merely `interactive`): the badge IS the
   * click target and nothing wraps it, so it carries `role="button"`,
   * keyboard activation and the stronger affordance ring — per the badge
   * affordance rule in static/AGENTS.md.
   * @returns {unknown} The row, or nothing when the listing is unfiltered.
   */
  _renderFilters() {
    const chips = toChips(this.filters);
    if (chips.length === 0) return nothing;
    return html`
      <div class="cts-plan-list-filters" data-testid="plan-filters">
        <span class="cts-plan-list-filters-label">Filtered by</span>
        ${repeat(
          chips,
          (chip) => chip.key,
          (chip) => html`
            <cts-badge
              variant="secondary"
              clickable
              icon="close-md"
              label=${chip.label}
              aria-label=${chip.removeLabel}
              data-testid="plan-filter-${chip.key}"
              data-filter-key=${chip.key}
              @cts-badge-click=${this._handleChipRemove}
            ></cts-badge>
          `,
        )}
        ${chips.length > 1
          ? html`<button
              type="button"
              class="cts-plan-list-filters-clear"
              data-testid="plan-filters-clear"
              @click=${this._handleClearFilters}
            >
              Clear all
            </button>`
          : nothing}
        ${this.isAdmin && !this.isPublic
          ? html`<cts-button
              variant="danger"
              size="sm"
              icon="trash-empty"
              label="Delete these plans..."
              data-testid="plan-bulk-delete"
              title="Delete every plan these filters match"
              @cts-click=${this._openBulkDelete}
            ></cts-button>`
          : nothing}
      </div>
    `;
  }

  /**
   * The confirmation, and then the progress of the job it starts. Only ever
   * reachable with a filter active, because the button that opens it lives in
   * the row of filter chips - which is also what the server insists on.
   * @returns {unknown} The dialog.
   */
  _renderBulkDeleteModal() {
    const preview = this._bulkPreview;
    const progress = this._bulkProgress;
    const running = progress?.state === "RUNNING";
    return html`
      <cts-modal
        id="planBulkDeleteModal"
        heading="Delete these test plans?"
        size="md"
        @cts-modal-close=${this._handleBulkDeleteClosed}
      >
        ${this._bulkError
          ? html`<cts-alert variant="danger" data-testid="plan-bulk-delete-error"
              >${this._bulkError}</cts-alert
            >`
          : nothing}
        ${progress
          ? html`
              <p data-testid="plan-bulk-delete-progress">
                <strong>${progress.state === "RUNNING" ? "Deleting" : progress.state}</strong>
                — ${progress.plans.toLocaleString()} of ${(progress.target ?? 0).toLocaleString()}
                plans, ${progress.tests.toLocaleString()} test runs and
                ${progress.logEntries.toLocaleString()} log entries removed.
              </p>
              ${running
                ? html`<cts-button
                    variant="secondary"
                    label="Stop"
                    data-testid="plan-bulk-delete-stop"
                    @cts-click=${this._cancelBulkDelete}
                  ></cts-button>`
                : nothing}
            `
          : html`
              <p>
                This deletes every plan matching the filters below - not just the ones on this page
                - along with every test run in them and every log entry of those runs. It cannot be
                undone.
              </p>
              <ul>
                ${repeat(
                  toChips(this.filters),
                  (chip) => chip.key,
                  (chip) => html`<li>${chip.label}</li>`,
                )}
              </ul>
              <label class="cts-plan-list-bulk-limit">
                <span>Delete at most</span>
                <input
                  type="number"
                  min="1"
                  step="1"
                  aria-label="Most plans to delete"
                  data-testid="plan-bulk-delete-limit"
                  .value=${this._bulkLimit}
                  @input=${this._handleBulkLimitInput}
                />
                <span>plans, oldest first. Leave empty for all of them.</span>
              </label>
              ${this._bulkBusy && !this._bulkError
                ? html`<p data-testid="plan-bulk-delete-counting">
                    <cts-spinner size="sm" label=${this._bulkBusyLabel}></cts-spinner>
                    ${this._bulkBusyLabel} On a large database this takes a few seconds.
                  </p>`
                : nothing}
              ${preview && !this._bulkBusy
                ? html`<p data-testid="plan-bulk-delete-counts">
                    ${preview.listed.toLocaleString()} plans match.
                    <strong>${this._bulkTarget().toLocaleString()} will be deleted.</strong>
                    ${preview.kept > 0
                      ? html`${preview.kept.toLocaleString()} are kept because they are immutable or
                        published.`
                      : nothing}
                  </p>`
                : nothing}
              <cts-button
                variant="danger"
                icon="trash-empty"
                label=${preview && !this._bulkBusy
                  ? `Delete ${this._bulkTarget().toLocaleString()} plans`
                  : "Delete"}
                ?disabled=${this._bulkBusy || !preview || this._bulkTarget() === 0}
                data-testid="plan-bulk-delete-confirm"
                @cts-click=${this._confirmBulkDelete}
              ></cts-button>
            `}
      </cts-modal>
    `;
  }

  /**
   * One control per variant parameter of the chosen plan, on their own row: a plan can define a
   * dozen, which would not fit beside the other controls.
   * @returns {unknown} The row, or nothing when no plan is chosen.
   */
  _renderVariantControls() {
    const variants = this._variantsToOffer();
    if (variants.length === 0) return nothing;
    const chosen = (this.filters && this.filters.variant) || {};
    return html`
      <div class="cts-plan-list-toolbar" data-testid="plan-variant-controls">
        ${repeat(
          variants,
          (variant) => variant.name,
          (variant) =>
            this._renderFilterSelect({
              label: variant.name,
              aria: `Only show plans with this ${variant.name}`,
              testid: `plan-variant-${variant.name}`,
              variant: variant.name,
              value: chosen[variant.name] || "",
              options: [
                { value: "", label: "Any" },
                ...variant.values.map((value) => ({ value, label: value })),
              ],
              onChange: this._handleVariantChange,
            }),
        )}
      </div>
    `;
  }

  /**
   * One filter control: a labelled `<select>` carrying the value it should show.
   *
   * That value is applied in {@link updated}, after the options exist, because neither obvious
   * alternative works. `.value` on the `<select>` is committed by Lit before its `<option>`
   * children are created, so the assignment is dropped and the control falls back to its first
   * option — reading "Any" beside a chip that says otherwise. Marking the option instead does
   * not survive either: the browser selects the first as they are appended. Naming the element
   * by `data-value` keeps production behaviour off the e2e hooks and means a new control needs
   * no edit in `updated`.
   * @param {object} control - The control to render.
   * @param {string} control.label - The visible label.
   * @param {string} control.aria - The select's accessible name.
   * @param {string} control.testid - Its `data-testid`.
   * @param {string} control.value - The value currently selected.
   * @param {Array<{value: string, label: string}>} control.options - Its options, "any" first.
   * @param {(event: Event) => void} control.onChange - The change handler.
   * @param {string} [control.variant] - The variant parameter, for the variant controls.
   * @returns {unknown} The control.
   */
  _renderFilterSelect({ label, aria, testid, value, options, onChange, variant = undefined }) {
    return html`
      <label class="cts-plan-list-age">
        <span>${label}</span>
        <select
          aria-label=${aria}
          data-testid=${testid}
          data-variant=${ifDefined(variant)}
          data-value=${value}
          @change=${onChange}
        >
          ${repeat(
            options,
            (option) => option.value,
            (option) => html`<option value=${option.value}>${option.label}</option>`,
          )}
        </select>
      </label>
    `;
  }

  /** @returns {Array<{value: string, label: string}>} The age options, "Any time" first. */
  _ageOptions() {
    const options = [
      { value: "", label: "Any time" },
      ...AGE_PRESETS.map((preset) => ({ value: preset.value, label: preset.label })),
    ];
    // a bound that is none of the presets came from a hand-edited URL or a drill-down; it is
    // offered so the control can show it rather than claiming the listing spans any time
    if (this._agePreset() === "custom") options.push({ value: "custom", label: "Custom period" });
    return options;
  }

  _renderSearchAndSort() {
    const filters = this.filters || emptyFilter();
    return html`
      <div class="cts-plan-list-toolbar">
        <label class="cts-plan-list-search">
          <cts-icon name="search-magnifying-glass" size="16" aria-hidden="true"></cts-icon>
          <input
            type="search"
            aria-label="Search test plans"
            placeholder="Search test plans..."
            title="Whole words, matched by the server against the plan name, description and certification profile"
            .value=${text(filters.search)}
            @input=${this._handleSearchInput}
            @change=${this._handleSearchCommit}
          />
        </label>
        ${this._filterOptions
          ? html`
              ${this._renderFilterSelect({
                label: "Family",
                aria: "Only show plans of this spec family",
                testid: "plan-family-filter",
                value: filters.family || "",
                options: [
                  { value: "", label: "Any family" },
                  ...this._filterOptions.families.map((family) => ({
                    value: family,
                    label: family,
                  })),
                ],
                onChange: this._handleFamilyChange,
              })}
              ${this._renderFilterSelect({
                label: "Plan",
                aria: "Only show plans with this name",
                testid: "plan-name-filter",
                value: filters.plan || "",
                options: [
                  { value: "", label: "Any plan" },
                  ...this._plansToOffer().map((plan) => ({
                    value: plan.name,
                    label: plan.retired ? `${plan.name} (retired)` : plan.name,
                  })),
                ],
                onChange: this._handlePlanChange,
              })}
            `
          : nothing}
        ${this._renderFilterSelect({
          label: "Immutable",
          aria: "Only show plans that are immutable, or only those that are not",
          testid: "plan-immutable-filter",
          value: filters.immutable || "",
          options: [
            { value: "", label: "Any" },
            { value: "true", label: "Yes" },
            { value: "false", label: "No" },
          ],
          onChange: this._handleImmutableChange,
        })}
        ${this._renderFilterSelect({
          label: "Started",
          aria: "Only show plans older than",
          testid: "plan-age-filter",
          value: this._agePreset(),
          options: this._ageOptions(),
          onChange: this._handleAgeChange,
        })}
        <label class="cts-plan-list-sort">
          <span>Sort</span>
          <select
            aria-label="Sort test plans"
            .value=${this._sortKey}
            @change=${this._handleSortChange}
          >
            <option value="started-desc">Started (newest)</option>
            <option value="started-asc">Started (oldest)</option>
            <option value="name-asc">Plan name (A–Z)</option>
          </select>
        </label>
      </div>
    `;
  }

  _renderConfigModal() {
    const configJson = this._selectedConfig ? JSON.stringify(this._selectedConfig, null, 4) : "";
    return html`
      <cts-modal id="planConfigModal" heading="Configuration" size="xl">
        <div class="cts-plan-list-config-toolbar">
          <cts-button
            class="copy-config-btn"
            variant="secondary"
            size="sm"
            icon="copy"
            label="Copy"
            title="Copy config to clipboard"
            @cts-click=${this._handleCopyConfig}
          ></cts-button>
          <span>Configuration for <code>${this._selectedPlanId}</code></span>
        </div>
        <cts-json-view
          class="planConfigJson config-json"
          aria-label="Plan configuration JSON"
          .value=${configJson}
        ></cts-json-view>
      </cts-modal>
    `;
  }

  _renderLoading() {
    return html` <cts-loading-state label="Loading test plans"></cts-loading-state> `;
  }

  /**
   * Render the empty state, branched by why the list is empty so the copy
   * matches the user's situation (R18). Every non-search empty state offers a
   * "Schedule test" action — on the My view, on the Published view, and for
   * anonymous visitors (an anonymous click lands on the server-auth-gated
   * schedule page):
   * - search returned nothing → "widen the search" (no action — the list is
   *   filtered, not empty);
   * - Published view is empty → an orienting placeholder plus the
   *   Schedule-test action. The body ("Published test plans will appear here
   *   once they are shared.") uses the same "Published test plans" vocabulary
   *   as the page-level Published descriptor on plans.html (R21/R22, U12), so
   *   the empty state and the descriptor read as one product. The action is the
   *   universal entry point, not a fix for this specific emptiness (scheduling a
   *   test starts a private run, not a published plan); that copy/CTA seam is
   *   deliberate;
   * - the listing is filtered (a drill-down from the statistics charts, or a
   *   shared link) → say the filters are what emptied it and offer the way
   *   out. The action is a real link to the same listing without them, so it
   *   works with the keyboard, the middle button and JavaScript turned off,
   *   and it preserves `?public`;
   * - otherwise (the My view, or an anonymous / unknown-auth visitor) → guide
   *   the user to schedule their first test, with the Schedule-test action,
   *   plus a secondary "View published plans" action so an empty personal
   *   list still offers something to look at. The Published view does NOT get
   *   the secondary action — it would link to the view the user is already on.
   * @param {boolean} hasSearch - Whether a search query is currently active.
   * @returns {import('lit').TemplateResult} The empty-state template.
   */
  _renderEmpty(hasSearch) {
    if (hasSearch) {
      return html`
        <cts-empty-state
          icon="folder"
          heading="No plans match your search"
          body="Try a different search term to widen the results."
          data-testid="plan-list-empty"
        ></cts-empty-state>
      `;
    }
    if (hasFilters(this.filters)) {
      return html`
        <cts-empty-state
          icon="folder"
          heading="No plans match these filters"
          body="No test plan in this view was started in that period, or matches that family, plan, variant or certification profile."
          cta-label="Clear filters"
          cta-href="plans.html${urlFromFilter(emptyFilter(), window.location.search)}"
          data-testid="plan-list-empty"
        ></cts-empty-state>
      `;
    }
    const isPublishedView = this.isPublic;
    return html`
      <cts-empty-state
        icon="folder"
        heading="${isPublishedView ? "No published plans yet" : "No test plans yet"}"
        body="${isPublishedView
          ? "Published test plans will appear here once they are shared."
          : "Schedule your first test to get started."}"
        cta-label="Create a new test"
        cta-href="schedule-test.html"
        secondary-cta-label="${ifDefined(isPublishedView ? undefined : "View published plans")}"
        secondary-cta-href="${ifDefined(isPublishedView ? undefined : "plans.html?public=true")}"
        data-testid="plan-list-empty"
      ></cts-empty-state>
    `;
  }

  render() {
    // The filter row and the toolbar are part of the page's scope, not of
    // the result: rendered once, outside the loading/error/loaded branch, so
    // their node identity is stable across every refetch. The search box in
    // particular must survive the refetch it causes, or typing would lose
    // focus each time the server is asked.
    return html`
      ${this._renderFilters()} ${this._renderSearchAndSort()} ${this._renderVariantControls()}
      ${this._renderBody()}
      ${this.isAdmin && !this.isPublic ? this._renderBulkDeleteModal() : nothing}
    `;
  }

  /**
   * The listing itself, in whichever of its three states it is in.
   *
   * Kept apart from {@link render} so the bulk-delete dialog can sit OUTSIDE
   * it: this returns early while a fetch is in flight, and a dialog rendered in
   * here would not exist to be opened during a refetch, and would be torn down
   * mid-sentence by the refetch that follows a delete - taking the only report
   * of what was deleted off the screen with it.
   * @returns {unknown} The body.
   */
  _renderBody() {
    if (this._loading) {
      return this._renderLoading();
    }

    if (this._error) {
      // The filter row (rendered above) survives a failed fetch too: a filter
      // is what can CAUSE the failure (a hand-edited `variant.<bad>` is a
      // 400), so hiding the chips would leave the reader an error with no way
      // to see what was asked for, let alone clear it.
      return html`
        <cts-alert variant="danger" role="alert">
          <strong>Error:</strong> ${this._error}
        </cts-alert>
      `;
    }

    const rows = this._plans;
    const hasSearch = text(this.filters && this.filters.search) !== "";

    return html`
      ${rows.length === 0
        ? this._renderEmpty(hasSearch)
        : html`
            <div
              class=${classMap({ "cts-plan-list-items": true, "is-refreshing": this._refreshing })}
              data-testid="plan-list-items"
              aria-busy=${this._refreshing ? "true" : "false"}
            >
              ${repeat(
                rows,
                (plan) => plan._id,
                (plan) => this._renderCard(plan),
              )}
            </div>
          `}
      <div class="cts-plan-list-footer">
        ${this._refreshing
          ? html`<p
              class="cts-plan-list-refreshing"
              role="status"
              data-testid="plan-list-refreshing"
            >
              Updating…
            </p>`
          : nothing}
        ${this._hasMore
          ? html`
              <cts-button
                variant="secondary"
                size="md"
                data-testid="plan-list-show-more"
                label="${this._loadingMore
                  ? "Loading…"
                  : `Show more (${rows.length.toLocaleString()} loaded)`}"
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

customElements.define("cts-plan-list", CtsPlanList);

export {};
