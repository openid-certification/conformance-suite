import { LitElement, html, nothing } from "lit";
import { repeat } from "lit/directives/repeat.js";
import "./cts-button.js";
import "./cts-icon.js";
import "./cts-modal.js";
import "./cts-alert.js";
import "./cts-tooltip.js";
import "./cts-time.js";
import "./cts-empty-state.js";
import "./cts-json-editor.js";
import { flashCopyConfirmed } from "../js/cts-copy-flash.js";
import { statusBadgeVariant } from "../js/module-status.js";

const PAGE_SIZE = 25;

// Variant -> status-box class. Explicit lookup table per components/AGENTS.md
// §7 (no dynamic class concatenation); the variant comes from
// `statusBadgeVariant` plus the local `pending` in-flight state. Unknown
// values fall back to the neutral `skip` box.
const STATUS_BOX_CLASSES = {
  pass: "moduleStatusBox moduleStatusBox--pass",
  fail: "moduleStatusBox moduleStatusBox--fail",
  warn: "moduleStatusBox moduleStatusBox--warn",
  running: "moduleStatusBox moduleStatusBox--running",
  skip: "moduleStatusBox moduleStatusBox--skip",
  review: "moduleStatusBox moduleStatusBox--review",
  pending: "moduleStatusBox moduleStatusBox--pending",
};

// Variant -> accessible status word for the box aria-label, so a non-visual
// agent/AT gets the outcome the box color conveys (not just the module id).
// Derived from the box variant rather than statusLabel(status,result) because
// the variant distinguishes a never-run/settled box (`skip`) from an
// in-flight fetch (`pending`) — both carry undefined status/result, which
// statusLabel would collapse to "PENDING".
const STATUS_BOX_LABELS = {
  pass: "passed",
  fail: "failed",
  warn: "warning",
  running: "running",
  review: "review",
  skip: "no result",
  pending: "checking status",
};

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
const STYLE_TEXT = `
  cts-plan-list {
    display: block;
    font-family: var(--font-sans);
    color: var(--fg);
  }
  .cts-plan-list-toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    align-items: center;
    margin-bottom: var(--space-4);
  }
  .cts-plan-list-search {
    position: relative;
    flex: 1 1 280px;
    min-width: 220px;
  }
  .cts-plan-list-search input {
    width: 100%;
    box-sizing: border-box;
    padding: var(--space-2) var(--space-3) var(--space-2) calc(var(--space-3) + var(--space-5));
    background: var(--bg);
    color: var(--fg);
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    font-family: var(--font-sans);
    font-size: var(--fs-14);
    line-height: var(--lh-snug);
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
  .cts-plan-list-sort {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--fs-13);
    color: var(--fg-soft);
  }
  .cts-plan-list-sort select {
    box-sizing: border-box;
    height: 34px;
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
    background-image: ${SELECT_CHEVRON};
    background-repeat: no-repeat;
    background-position: right 12px center;
  }
  .cts-plan-list-sort select:focus {
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
     clicks. The module status grid also sits at z-index: 1 (its boxes need
     hover for tooltips) but is pointer-events: none except on the boxes, so
     clicks in its band still fall through to the card link (see
     .moduleStatusGrid below). See https://adrianroselli.com/2020/02/block-links-cards-clickable-regions-etc.html */
  .cts-plan-card {
    position: relative;
    display: grid;
    gap: var(--space-2);
    padding: var(--space-4);
    background: var(--bg-elev);
    color: var(--fg);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    transition: border-color var(--dur-1) var(--ease-standard),
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
  /* The per-plan module status grid: small color-coded rounded rectangles
     (one per module), each wrapped in a tooltip that reveals the full module
     id on hover. This is the design-token successor to the legacy
     .testStatusResultBox squares — names would not fit on a listing card, so
     status reads as color and the id is on demand. The grid lifts above the
     card-link ::after overlay (z-index: 1) so the per-box tooltips receive
     hover/focus; the trade-off is that the grid area does not trigger card
     navigation (clicking a status box does nothing, which matches the legacy
     non-interactive squares). */
  .cts-plan-card .moduleStatusGrid {
    position: relative;
    z-index: 1;
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-1);
    /* The grid spans the full card column at z-index 1, which would put its
       gaps and trailing whitespace above the card-link ::after overlay and
       swallow navigation clicks in that band. Make the grid transparent to
       pointer events and re-enable them only on the tooltip-wrapped boxes:
       hover/tooltip still works on a box, every other pixel of the band
       falls through to the card link. */
    pointer-events: none;
  }
  .cts-plan-card .moduleStatusGrid cts-tooltip {
    pointer-events: auto;
  }
  .moduleStatusBox {
    /* Fixed 32x18 rounded rectangle (per design); flex-shrink off so the
       grid wraps rather than squashing the boxes. */
    width: 32px;
    height: 18px;
    border-radius: var(--radius-1);
    flex-shrink: 0;
    background: var(--status-skipped);
  }
  .moduleStatusBox--pass { background: var(--status-pass); }
  .moduleStatusBox--fail { background: var(--status-fail); }
  .moduleStatusBox--warn { background: var(--status-warning); }
  .moduleStatusBox--running { background: var(--status-running); }
  /* A settled not-run / unresolved box uses a lighter neutral than the
     darker pulsing pending box, so "nothing to report" recedes visually. */
  .moduleStatusBox--skip { background: var(--ink-300); }
  /* Review has no --status-review token yet; use the legacy review teal so
     it stays distinguishable from the gray skip/pending boxes. */
  .moduleStatusBox--review { background: #6AC4C2; }
  /* Pending shares the neutral gray with skip — motion is the only
     differentiator (a running fetch pulses; a settled box is static).
     Capped at 10 iterations, gated behind prefers-reduced-motion. */
  .moduleStatusBox--pending { background: var(--status-skipped); }
  @media (prefers-reduced-motion: no-preference) {
    .moduleStatusBox--pending {
      animation: cts-plan-list-status-pulse 1.2s ease-in-out 10;
    }
  }
  @keyframes cts-plan-list-status-pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.35; }
  }
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
  .cts-plan-list-loading,
  .cts-plan-list-empty {
    padding: var(--space-5);
    text-align: center;
    color: var(--fg-soft);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);
  }
  .cts-plan-list-loading .spinner-border {
    display: inline-block;
    width: 16px;
    height: 16px;
    border: 2px solid var(--border-strong);
    border-top-color: var(--orange-400);
    border-radius: 50%;
    animation: cts-plan-list-spin 0.9s linear infinite;
  }
  @keyframes cts-plan-list-spin {
    to { transform: rotate(360deg); }
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
     editor value. Do not rename without updating that story. */
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
  style.textContent = STYLE_TEXT;
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
 * `cts-log-list`: a top toolbar (free-text search + sort selector), block-link
 * cards (plan name headline, plan id slug, description, module status grid,
 * metadata row), "Show more" pagination, and a config-viewer modal.
 *
 * The module status grid is a row of small color-coded rounded rectangles,
 * one per module, each wrapped in a tooltip that reveals the full module id on
 * hover (the design-token successor to the legacy .testStatusResultBox
 * squares). A box starts gray (pulsing for modules that have run, static for
 * never-run modules) and recolors once the per-module status resolves — see
 * `_statusVariantFor` and the `/api/info` resolution path.
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
 * @fires cts-plan-navigate - When a plan name is clicked, with
 *   `{ detail: { planId } }`; bubbles and is composed.
 */
class CtsPlanList extends LitElement {
  static properties = {
    isAdmin: { type: Boolean, attribute: "is-admin" },
    isPublic: { type: Boolean, attribute: "is-public" },
    deferInitialFetch: { type: Boolean, attribute: "defer-initial-fetch" },
    _plans: { state: true },
    _loading: { state: true },
    _error: { state: true },
    _searchText: { state: true },
    _sortKey: { state: true },
    _visibleCount: { state: true },
    _selectedConfig: { state: true },
    _selectedPlanId: { state: true },
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
    this._plans = [];
    this._loading = true;
    this._error = null;
    this._searchText = "";
    this._sortKey = "started-desc";
    this._visibleCount = PAGE_SIZE;
    this._selectedConfig = null;
    this._selectedPlanId = "";
    // In-flight `/api/info/<instance>` set so repeated renders (search, sort,
    // show-more, and the re-render the resolution itself triggers) don't fan
    // out duplicate requests for the same instance. Non-reactive — never read
    // from render.
    this._infoFetchesInFlight = new Set();
    // The {sorted, visible} view computed by the most recent render(), reused
    // by the status-resolution pass (which runs in updated(), after render)
    // so the search→sort→slice work happens once per render, not twice.
    // Non-reactive — never read from render itself.
    this._currentView = null;
    // Pre-bind handlers wired through Lit EventParts on rendered cards. Lit
    // dispatches with `this` set to the host element of the listener; these
    // must retain this component as `this`.
    this._handleSearchInput = this._handleSearchInput.bind(this);
    this._handleSortChange = this._handleSortChange.bind(this);
    this._handleShowMoreClick = this._handleShowMoreClick.bind(this);
    this._handlePlanLinkClick = this._handlePlanLinkClick.bind(this);
    this._handleConfigButtonClick = this._handleConfigButtonClick.bind(this);
    this._handleCopyConfig = this._handleCopyConfig.bind(this);
  }

  connectedCallback() {
    super.connectedCallback();
    // The host is the stable wrapper that persists across loading→loaded
    // renders, so announce dataset changes (e.g. a My⇄Published tab swap)
    // here rather than on a fragment that re-creates itself each render
    // (R17/R19). The list region is supplementary, so a polite live region is
    // appropriate.
    this.setAttribute("aria-live", "polite");
    // KTD3: on the no-`public`-param path the page sets `defer-initial-fetch`
    // synchronously so the auth-dependent default resolves before fetching.
    // The list still renders its loading state (`_loading` defaults true) in
    // the meantime; the page calls `fetchPlans()` once auth resolves.
    if (!this.deferInitialFetch) {
      this._fetchPlans();
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
    return this._fetchPlans();
  }

  updated(changedProperties) {
    // After a render that changed the visible set, fetch the latest result
    // for the modules of the currently-visible cards. Gating to visible cards
    // (rather than every loaded plan) bounds the fan-out: a listing can hold
    // up to 1000 plans and a single FAPI/OIDCC plan has dozens of modules, so
    // fetching all of them on load would fire thousands of parallel requests.
    // Search, sort, and "Show more" all change one of these props and
    // re-enter here, so newly-visible modules get resolved lazily;
    // resolved/in-flight instances are skipped, so this is idempotent across
    // the re-render the resolution itself triggers. Renders driven only by
    // unrelated state (e.g. opening the config modal) do not re-run the
    // resolver.
    const viewKeys = ["_plans", "_loading", "_searchText", "_sortKey", "_visibleCount"];
    if (viewKeys.some((k) => changedProperties.has(k))) {
      this._resolveVisibleModuleStatuses();
    }
  }

  async _fetchPlans() {
    this._loading = true;
    this._error = null;
    try {
      const url = this.isPublic ? "/api/plan?public=true" : "/api/plan";
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Failed to load test plans (HTTP ${response.status})`);
      }
      // Real backend (TestPlanApi.getTestPlansForCurrentUser) returns a
      // PaginationResponse envelope: { draw, recordsTotal, recordsFiltered,
      // data: [...] }. Some test mocks and the storybook MSW handlers
      // return a plain array. Accept both.
      const payload = await response.json();
      this._plans = Array.isArray(payload)
        ? payload
        : Array.isArray(payload?.data)
          ? payload.data
          : [];
    } catch (err) {
      this._error = err instanceof Error ? err.message : String(err);
      this._plans = [];
    } finally {
      this._loading = false;
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

  _handleSearchInput(event) {
    this._searchText = event.target.value;
    this._visibleCount = PAGE_SIZE;
  }

  _handleSortChange(event) {
    this._sortKey = event.target.value;
    this._visibleCount = PAGE_SIZE;
  }

  _handleShowMoreClick() {
    this._visibleCount += PAGE_SIZE;
  }

  _searchedPlans(rows) {
    const query = this._searchText.trim().toLowerCase();
    if (!query) return rows;
    return rows.filter((row) => {
      const haystack = [row.planName, row._id, row.description, formatVariant(row.variant)]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return haystack.includes(query);
    });
  }

  _sortedPlans(rows) {
    const key = this._sortKey;
    const copy = rows.slice();
    if (key === "started-desc") {
      copy.sort((a, b) => (b.started || "").localeCompare(a.started || ""));
    } else if (key === "started-asc") {
      copy.sort((a, b) => (a.started || "").localeCompare(b.started || ""));
    } else if (key === "name-asc") {
      copy.sort((a, b) => (a.planName || "").localeCompare(b.planName || ""));
    }
    return copy;
  }

  /**
   * Compute the search → sort → paginate view once so render() and the
   * status-resolution pass operate on the same visible set.
   * @returns {{sorted: object[], visible: object[]}} The fully sorted list
   *   (for empty/pagination decisions) and the visible slice.
   */
  _computeView() {
    const searched = this._searchedPlans(this._plans);
    const sorted = this._sortedPlans(searched);
    const visible = sorted.slice(0, this._visibleCount);
    return { sorted, visible };
  }

  /**
   * Fetch `/api/info/<lastInstance>` for the modules of the currently-visible
   * cards and merge the resolved `{ status, result }` back into the module
   * entries so their dots recolor. Mirrors the merge shape of
   * plan-detail.html, but takes its error/batching shape from
   * cts-log-list._resolvePlanNames: a terminal per-fetch catch settles the
   * dot at the neutral `skip` color, `Promise.allSettled` never rejects the
   * batch, and a single batched `_plans` reassign triggers one re-render for
   * the whole batch. Unique by instance id, so a shared instance is fetched
   * once and applied to every module that references it.
   */
  _resolveVisibleModuleStatuses() {
    if (this._loading || this._error) return;
    // Reuse the view computed by the render that just completed (updated()
    // always runs after render). Fall back to computing only if a render has
    // not populated it yet.
    const { visible } = this._currentView || this._computeView();
    // Group unresolved, not-in-flight modules by their last instance id so
    // each instance is fetched exactly once.
    const byInstance = new Map();
    for (const plan of visible) {
      for (const mod of plan.modules || []) {
        if (!Array.isArray(mod.instances) || mod.instances.length === 0) continue;
        if (mod._statusResolved) continue;
        const lastInstance = mod.instances[mod.instances.length - 1];
        if (this._infoFetchesInFlight.has(lastInstance)) continue;
        if (!byInstance.has(lastInstance)) byInstance.set(lastInstance, []);
        byInstance.get(lastInstance).push(mod);
      }
    }
    if (byInstance.size === 0) return;

    const publicSuffix = this.isPublic ? "?public=true" : "";
    for (const inst of byInstance.keys()) this._infoFetchesInFlight.add(inst);

    const fetches = Array.from(byInstance.entries()).map(([inst, mods]) =>
      fetch(`/api/info/${encodeURIComponent(inst)}${publicSuffix}`)
        .then((response) => {
          if (!response.ok) throw new Error(`HTTP ${response.status}`);
          return response.json();
        })
        .then((info) => {
          for (const mod of mods) {
            mod.status = info.status;
            mod.result = info.result;
            mod._statusResolved = true;
          }
        })
        .catch((err) => {
          // Fail-soft: the run may be inaccessible (404 unpublished/deleted)
          // or the endpoint may error. Settle the dot at the neutral `skip`
          // color rather than leaving it pulsing forever, and warn once per
          // instance so a real /api/info contract drift is visible.
          for (const mod of mods) mod._statusResolved = true;
          console.warn(`[cts-plan-list] /api/info/${inst} failed:`, err);
        })
        .finally(() => this._infoFetchesInFlight.delete(inst)),
    );

    Promise.allSettled(fetches).then(() => {
      // One batched reassign so the whole batch re-renders once (new array
      // reference; the mutated module objects carry the resolved status).
      this._plans = [...this._plans];
    });
  }

  /**
   * Resolve the status variant for a module's status box:
   * - never-run module (no instances) → static `skip` (neutral gray)
   * - has run, status not yet fetched → `pending` (gray, pulsing)
   * - status resolved → the concrete color from `statusBadgeVariant`
   *   (a fetch failure settles status/result undefined → `skip`)
   * The `_statusResolved` marker is set by `_resolveVisibleModuleStatuses`
   * on both success and failure, so a failed fetch settles the box rather
   * than leaving it pulsing forever.
   * @param {{instances?: string[], status?: string, result?: string,
   *   _statusResolved?: boolean}} mod - A plan module entry.
   * @returns {string} A status variant name used as the box color modifier.
   */
  _statusVariantFor(mod) {
    const hasInstance = Array.isArray(mod.instances) && mod.instances.length > 0;
    if (!hasInstance) return "skip";
    if (mod._statusResolved) return statusBadgeVariant(mod.status, mod.result);
    return "pending";
  }

  _renderModuleStatusGrid(modules) {
    if (!modules || modules.length === 0) return nothing;
    return html`<div class="moduleStatusGrid">
      ${modules.map((mod) => {
        const variant = this._statusVariantFor(mod);
        const boxClass = STATUS_BOX_CLASSES[variant] || STATUS_BOX_CLASSES.skip;
        const statusWord = STATUS_BOX_LABELS[variant] || STATUS_BOX_LABELS.skip;
        // The box is color-only; the tooltip reveals the full module id on
        // hover. The aria-label carries the id AND the status word so
        // assistive tech (and DOM-reading agents) get the outcome the color
        // conveys, not just the module name.
        return html`<cts-tooltip content="${mod.testModule}" placement="top">
          <span class="${boxClass}" role="img" aria-label="${mod.testModule}: ${statusWord}"></span>
        </cts-tooltip>`;
      })}
    </div>`;
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
    const planHref = `plan-detail.html?plan=${encodeURIComponent(plan._id)}`;
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
        ${this._renderModuleStatusGrid(plan.modules)}
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
                  ${this._renderOwner(plan.owner)}
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

  _renderSearchAndSort() {
    return html`
      <div class="cts-plan-list-toolbar">
        <label class="cts-plan-list-search">
          <cts-icon name="search-magnifying-glass" size="16" aria-hidden="true"></cts-icon>
          <input
            type="search"
            aria-label="Search test plans"
            placeholder="Search test plans..."
            .value=${this._searchText}
            @input=${this._handleSearchInput}
          />
        </label>
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
        <cts-json-editor
          class="planConfigJson config-json"
          readonly
          aria-label="Plan configuration JSON"
          .value=${configJson}
        ></cts-json-editor>
      </cts-modal>
    `;
  }

  _renderLoading() {
    return html`
      <div class="cts-plan-list-loading">
        <span class="spinner-border" role="status"></span>
        <span>Loading test plans...</span>
      </div>
    `;
  }

  _renderEmpty(hasSearch) {
    const heading = hasSearch ? "No plans match your search" : "No test plans found";
    const body = hasSearch
      ? "Try a different search term to widen the results."
      : "Test plans will appear here once they are created.";
    return html`
      <cts-empty-state
        icon="folder"
        heading="${heading}"
        body="${body}"
        data-testid="plan-list-empty"
      ></cts-empty-state>
    `;
  }

  render() {
    if (this._loading) {
      return html`${this._renderSearchAndSort()} ${this._renderLoading()}`;
    }

    if (this._error) {
      return html`
        <cts-alert variant="danger" role="alert">
          <strong>Error:</strong> ${this._error}
        </cts-alert>
      `;
    }

    const view = this._computeView();
    this._currentView = view;
    const { sorted, visible } = view;
    const hasMore = sorted.length > visible.length;
    const hasSearch = this._searchText.trim().length > 0;

    return html`
      ${this._renderSearchAndSort()}
      ${sorted.length === 0
        ? this._renderEmpty(hasSearch)
        : html`
            <div class="cts-plan-list-items" data-testid="plan-list-items">
              ${repeat(
                visible,
                (plan) => plan._id,
                (plan) => this._renderCard(plan),
              )}
            </div>
          `}
      <div class="cts-plan-list-footer">
        ${hasMore
          ? html`
              <cts-button
                variant="secondary"
                size="md"
                data-testid="plan-list-show-more"
                label="Show more (${visible.length} of ${sorted.length})"
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
