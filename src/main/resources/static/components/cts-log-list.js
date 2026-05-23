import { LitElement, html, nothing } from "lit";
import { repeat } from "lit/directives/repeat.js";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-icon.js";
import "./cts-modal.js";
import "./cts-alert.js";
import "./cts-spinner.js";
import "./cts-tooltip.js";
import "./cts-empty-state.js";
import "./cts-json-editor.js";
import { flashCopyConfirmed } from "../js/cts-copy-flash.js";

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
  FINISHED: "skip",
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

const STATUS_SORT_ORDER = {
  RUNNING: 0,
  WAITING: 1,
  INTERRUPTED: 2,
  FINISHED: 3,
  CONFIGURED: 4,
  CREATED: 5,
  NOT_YET_CREATED: 6,
};

// Matches the cap used by cts-dashboard and the previous logs.html URL-filter
// codepath. The backend PaginationRequest caps `length` at 1000, so this is
// the largest single-call dataset we can render client-side without paging.
const MAX_FILTERED_LOGS = 1000;

const PAGE_SIZE = 25;

const STYLE_ID = "cts-log-list-styles";

const STYLE_TEXT = `
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
  .cts-log-list-search {
    position: relative;
    flex: 1 1 280px;
    min-width: 220px;
  }
  .cts-log-list-search input {
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
  .cts-log-list-search input:focus {
    outline: none;
    border-color: var(--border-strong);
    box-shadow: var(--focus-ring);
  }
  .cts-log-list-search cts-icon {
    position: absolute;
    left: var(--space-3);
    top: 50%;
    transform: translateY(-50%);
    color: var(--fg-soft);
    pointer-events: none;
  }
  .cts-log-list-sort {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--fs-13);
    color: var(--fg-soft);
  }
  .cts-log-list-sort select {
    padding: var(--space-2) var(--space-3);
    background: var(--bg);
    color: var(--fg);
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    font-family: var(--font-sans);
    font-size: var(--fs-14);
  }
  .cts-log-list-sort select:focus {
    outline: none;
    border-color: var(--border-strong);
    box-shadow: var(--focus-ring);
  }
  .cts-log-filter-group {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    margin-bottom: var(--space-3);
  }
  .cts-log-filter-group-label {
    font-size: var(--fs-13);
    color: var(--fg-soft);
    font-weight: var(--fw-medium);
    margin-right: var(--space-1);
  }
  .cts-log-filter-chip {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    padding: var(--space-1) var(--space-3);
    background: transparent;
    color: var(--fg);
    border: 1px solid var(--border);
    border-radius: var(--radius-pill);
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    line-height: var(--lh-snug);
    cursor: pointer;
    transition: border-color var(--dur-1) var(--ease-standard),
                background var(--dur-1) var(--ease-standard);
  }
  .cts-log-filter-chip:hover {
    border-color: var(--border-strong);
  }
  .cts-log-filter-chip:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  .cts-log-filter-chip[aria-pressed="true"] {
    background: var(--bg-elev);
    border-color: var(--border-strong);
    color: var(--fg);
    font-weight: var(--fw-medium);
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
    transition: border-color var(--dur-1) var(--ease-standard),
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
    transition: border-color var(--dur-1) var(--ease-standard),
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
    text-decoration: none;
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
    text-decoration: none;
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
  .cts-log-card-plan-link {
    position: relative;
    z-index: 1;
    color: var(--fg-link);
    text-decoration: none;
    font-family: var(--font-mono);
    font-size: var(--fs-12);
  }
  .cts-log-card-plan-link:hover {
    text-decoration: underline;
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
  .cts-log-list-loading,
  .cts-log-list-empty {
    padding: var(--space-5);
    text-align: center;
    color: var(--fg-soft);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);
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
  .cts-log-list-truncation {
    margin: 0;
    color: var(--fg-soft);
    font-size: var(--fs-13);
    text-align: center;
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
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
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

// Pretty-prints `started` as a relative time (e.g. "5 minutes ago"). Falls
// back to the absolute locale string for anything older than ~30 days so the
// tooltip remains the canonical source for absolute timestamps anyway.
function formatRelativeTime(iso) {
  if (!iso) return "";
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return iso;
  const deltaSec = Math.max(0, Math.round((Date.now() - then) / 1000));
  const rtf = new Intl.RelativeTimeFormat(undefined, { numeric: "auto" });
  if (deltaSec < 60) return rtf.format(-deltaSec, "second");
  const deltaMin = Math.round(deltaSec / 60);
  if (deltaMin < 60) return rtf.format(-deltaMin, "minute");
  const deltaHour = Math.round(deltaMin / 60);
  if (deltaHour < 24) return rtf.format(-deltaHour, "hour");
  const deltaDay = Math.round(deltaHour / 24);
  if (deltaDay < 30) return rtf.format(-deltaDay, "day");
  return new Date(iso).toLocaleString();
}

function formatAbsoluteTime(iso) {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString();
}

/**
 * Filterable list of test logs. Replaces the legacy 10-column DataTable on
 * `logs.html` with a single-column card layout. Each card carries the test
 * name (headline link), the instance id (slug), the description, status +
 * result badges, a metadata footer (variant, started, plan id, owner when
 * admin), and a "View configuration" icon button.
 *
 * Faceted filter chip groups (status + result) sit above the list, plus a
 * free-text search input and a sort selector. Filter chips sync to the
 * `?status=` and `?result=` URL params via `history.replaceState`, so the
 * existing dashboard deep-link contract is preserved. Search and sort live
 * in component state and reset on reload.
 *
 * The component fetches up to `MAX_FILTERED_LOGS = 1000` rows once via
 * `/api/log?length=1000` (matching `cts-dashboard`'s stats fetch) and runs
 * all filter / search / sort / pagination logic client-side. Above 1000
 * matches, the truncation hint nudges the user to refine the filter.
 *
 * Light DOM. Scoped CSS is injected once on first connect.
 *
 * @property {boolean} isAdmin - Reveals the Owner pill on each card when set.
 *   Reflects the `is-admin` attribute. Ignored when `isPublic` is true.
 * @property {boolean} isPublic - Switches the fetch to `/api/log?public=true`
 *   and suppresses admin-only affordances (Owner pill, config button).
 *   Reflects the `is-public` attribute.
 * @fires cts-log-filter-change - Bubbles when the user toggles a status or
 *   result filter chip, or clears all filters. `detail: { status: string[],
 *   result: string[] }` carries the post-change selection sets as arrays.
 */
class CtsLogList extends LitElement {
  static properties = {
    isAdmin: { type: Boolean, attribute: "is-admin" },
    isPublic: { type: Boolean, attribute: "is-public" },
    _logs: { state: true },
    _loading: { state: true },
    _error: { state: true },
    _truncated: { state: true },
    _statusFilter: { state: true },
    _resultFilter: { state: true },
    _searchText: { state: true },
    _sortKey: { state: true },
    _visibleCount: { state: true },
    _selectedConfig: { state: true },
    _selectedTestId: { state: true },
    _planNames: { state: true },
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
    this._error = null;
    this._truncated = false;
    this._statusFilter = new Set();
    this._resultFilter = new Set();
    this._searchText = "";
    this._sortKey = "started-desc";
    this._visibleCount = PAGE_SIZE;
    this._selectedConfig = null;
    this._selectedTestId = "";
    // Resolved `planName` per `planId` referenced in `_logs`. `/api/log` only
    // returns opaque plan ids, so each unique id is fetched via
    // `/api/plan/<id>` and the kebab-case `planName` cached here for the
    // meta-row link text and the search haystack. `null` marks "tried, no
    // name available" so failed lookups (404, deleted plan, permission
    // denied) don't retry on every re-render — the link falls back to
    // `planId` in both the unresolved and the null case.
    this._planNames = new Map();
    // In-flight planId set so concurrent `_logs` reassignments (e.g. an
    // initial fetch plus a follow-up refresh) don't fan out duplicate
    // `/api/plan/<id>` requests for the same id. Non-reactive — never read
    // from render.
    this._planNameFetchesInFlight = new Set();
    // Pre-bind handlers used by Lit EventParts on rendered cards. Lit
    // dispatches with `this` set to the host element of the listener; the
    // handlers need to retain this component as `this`.
    this._handleSearchInput = this._handleSearchInput.bind(this);
    this._handleSortChange = this._handleSortChange.bind(this);
    this._handleStatusChipClick = this._handleStatusChipClick.bind(this);
    this._handleResultChipClick = this._handleResultChipClick.bind(this);
    this._handleClearAllClick = this._handleClearAllClick.bind(this);
    this._handleConfigButtonClick = this._handleConfigButtonClick.bind(this);
    this._handleShowMoreClick = this._handleShowMoreClick.bind(this);
    this._handleCopyConfig = this._handleCopyConfig.bind(this);
    this._handleChipGroupKeydown = this._handleChipGroupKeydown.bind(this);
  }

  connectedCallback() {
    super.connectedCallback();
    this._hydrateFromUrl();
    this._fetchLogs();
  }

  _hydrateFromUrl() {
    const params = new URLSearchParams(window.location.search);
    this._statusFilter = parseFilterSet(params.get("status"), VALID_STATUSES);
    this._resultFilter = parseFilterSet(params.get("result"), VALID_RESULTS);
  }

  async _fetchLogs() {
    this._loading = true;
    this._error = null;
    this._truncated = false;
    try {
      const url = "/api/log?length=" + MAX_FILTERED_LOGS + (this.isPublic ? "&public=true" : "");
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Failed to load logs (HTTP ${response.status})`);
      }
      const payload = await response.json();
      // Accept both PaginationResponse envelope ({ draw, recordsTotal,
      // recordsFiltered, data }) and a raw array, matching the dual-shape
      // handling in cts-dashboard / cts-plan-list.
      const data = Array.isArray(payload)
        ? payload
        : Array.isArray(payload?.data)
          ? payload.data
          : [];
      const hasTotal = typeof payload?.recordsTotal === "number";
      const total = hasTotal ? payload.recordsTotal : data.length;
      this._logs = data;
      // Only fall back to "data filled the cap" when the response had no
      // authoritative total. A response with `recordsTotal === data.length`
      // is the canonical signal that the dataset is complete, so an exact
      // 1000-row dataset must not raise the truncation hint.
      this._truncated = hasTotal ? total > data.length : data.length >= MAX_FILTERED_LOGS;
      this._resolvePlanNames(data);
    } catch (err) {
      this._error = err instanceof Error ? err.message : String(err);
      this._logs = [];
    } finally {
      this._loading = false;
    }
  }

  /**
   * Resolve a kebab-case `planName` for every unique `planId` referenced by
   * the freshly loaded logs. `/api/log` only carries the opaque plan id, so
   * each unique id is fetched via `/api/plan/<id>` once. Results are cached
   * on `_planNames` (planId → planName, or planId → null when the lookup
   * fails). The render path consults the map and falls back to `planId`
   * when an entry is missing or null, so unresolved plans still get a
   * usable link label.
   *
   * Idempotent: ids already in `_planNames` or in
   * `_planNameFetchesInFlight` are skipped. Honors `isPublic` so the
   * public-mode listing uses the same `?public=true` route the rest of the
   * component uses.
   *
   * @param {Array<{planId?: string}>} logs - Log rows whose planIds should
   *   be resolved. Rows without a planId are ignored.
   */
  _resolvePlanNames(logs) {
    const publicSuffix = this.isPublic ? "?public=true" : "";
    const toFetch = new Set();
    for (const log of logs) {
      const id = log && log.planId;
      if (!id) continue;
      if (this._planNames.has(id)) continue;
      if (this._planNameFetchesInFlight.has(id)) continue;
      toFetch.add(id);
    }
    if (toFetch.size === 0) return;
    for (const id of toFetch) {
      this._planNameFetchesInFlight.add(id);
    }
    const fetches = Array.from(toFetch).map((id) =>
      fetch(`/api/plan/${encodeURIComponent(id)}${publicSuffix}`)
        .then((response) => {
          if (!response.ok) return null;
          return response.json().catch(() => null);
        })
        .then((body) => {
          const name = body && typeof body.planName === "string" ? body.planName : null;
          return [id, name];
        })
        .catch(() => [id, null]),
    );
    Promise.allSettled(fetches).then((results) => {
      // Re-assign the Map to a new instance so Lit treats the state as
      // changed (Maps mutated in-place don't trigger reactive updates).
      // The `status === "fulfilled"` check is for TypeScript narrowing —
      // every fetch chain ends in `.catch(() => [id, null])`, so each
      // settled result is in practice always fulfilled. If that catch
      // ever moves or is removed, a rejected result would leave its id
      // permanently in `_planNameFetchesInFlight`; the check guards that
      // shape implicitly by simply not consuming rejected entries.
      const next = new Map(this._planNames);
      for (const result of results) {
        if (result.status !== "fulfilled") continue;
        const [id, name] = result.value;
        next.set(id, name);
        this._planNameFetchesInFlight.delete(id);
      }
      this._planNames = next;
    });
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

  _resetPagination() {
    this._visibleCount = PAGE_SIZE;
  }

  _handleSearchInput(event) {
    this._searchText = event.target.value;
    this._resetPagination();
  }

  _handleSortChange(event) {
    this._sortKey = event.target.value;
    this._resetPagination();
  }

  _toggleSetMember(set, value) {
    const next = new Set(set);
    if (next.has(value)) next.delete(value);
    else next.add(value);
    return next;
  }

  _handleStatusChipClick(event) {
    const value = event.currentTarget.dataset.status;
    if (!value) return;
    this._statusFilter = this._toggleSetMember(this._statusFilter, value);
    this._resetPagination();
    this._writeUrl();
  }

  _handleResultChipClick(event) {
    const value = event.currentTarget.dataset.result;
    if (!value) return;
    this._resultFilter = this._toggleSetMember(this._resultFilter, value);
    this._resetPagination();
    this._writeUrl();
  }

  _handleClearAllClick(event) {
    event.preventDefault();
    this._statusFilter = new Set();
    this._resultFilter = new Set();
    this._searchText = "";
    this._resetPagination();
    this._writeUrl();
    // Restore focus to the search input so keyboard users have a natural
    // landing point after dismissing the summary.
    this.updateComplete.then(() => {
      const search = /** @type {HTMLInputElement | null} */ (
        this.querySelector(".cts-log-list-search input")
      );
      if (search) search.focus();
    });
  }

  _handleChipGroupKeydown(event) {
    if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
    const chips = Array.from(event.currentTarget.querySelectorAll(".cts-log-filter-chip"));
    const current = chips.indexOf(document.activeElement);
    if (current === -1) return;
    event.preventDefault();
    const dir = event.key === "ArrowRight" ? 1 : -1;
    const nextIndex = (current + dir + chips.length) % chips.length;
    chips[nextIndex].focus();
  }

  _handleShowMoreClick() {
    this._visibleCount += PAGE_SIZE;
  }

  _filteredLogs() {
    const status = this._statusFilter;
    const result = this._resultFilter;
    if (status.size === 0 && result.size === 0) return this._logs;
    return this._logs.filter(
      (row) =>
        (status.size === 0 || status.has(row.status)) &&
        (result.size === 0 || result.has(row.result)),
    );
  }

  _searchedLogs(rows) {
    const query = this._searchText.trim().toLowerCase();
    if (!query) return rows;
    return rows.filter((row) => {
      const haystack = [
        row.testName,
        row.testId,
        row.description,
        row.planId,
        this._planNames.get(row.planId),
        formatVariant(row.variant),
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return haystack.includes(query);
    });
  }

  _sortedLogs(rows) {
    const key = this._sortKey;
    const copy = rows.slice();
    if (key === "started-desc") {
      copy.sort((a, b) => (b.started || "").localeCompare(a.started || ""));
    } else if (key === "started-asc") {
      copy.sort((a, b) => (a.started || "").localeCompare(b.started || ""));
    } else if (key === "name-asc") {
      copy.sort((a, b) => (a.testName || "").localeCompare(b.testName || ""));
    } else if (key === "status-asc") {
      copy.sort((a, b) => {
        const aRank = STATUS_SORT_ORDER[a.status] ?? 99;
        const bRank = STATUS_SORT_ORDER[b.status] ?? 99;
        if (aRank !== bRank) return aRank - bRank;
        return (b.started || "").localeCompare(a.started || "");
      });
    }
    return copy;
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
        <label class="cts-log-list-search">
          <cts-icon name="search-magnifying-glass" size="16" aria-hidden="true"></cts-icon>
          <input
            type="search"
            aria-label="Search logs"
            placeholder="Search logs"
            .value=${this._searchText}
            @input=${this._handleSearchInput}
          />
        </label>
        <label class="cts-log-list-sort">
          <span>Sort</span>
          <select aria-label="Sort logs" .value=${this._sortKey} @change=${this._handleSortChange}>
            <option value="started-desc">Started (newest)</option>
            <option value="started-asc">Started (oldest)</option>
            <option value="name-asc">Test name (A–Z)</option>
            <option value="status-asc">Status</option>
          </select>
        </label>
      </div>
    `;
  }

  _renderFilterRow() {
    const statusChip = (value) => html`
      <button
        type="button"
        class="cts-log-filter-chip"
        data-status="${value}"
        aria-pressed="${this._statusFilter.has(value)}"
        @click=${this._handleStatusChipClick}
      >
        ${value.toLowerCase()}
      </button>
    `;
    const resultChip = (value) => html`
      <button
        type="button"
        class="cts-log-filter-chip"
        data-result="${value}"
        aria-pressed="${this._resultFilter.has(value)}"
        @click=${this._handleResultChipClick}
      >
        ${value.toLowerCase()}
      </button>
    `;
    return html`
      <div
        class="cts-log-filter-group"
        role="group"
        aria-label="Filter by status"
        @keydown=${this._handleChipGroupKeydown}
      >
        <span class="cts-log-filter-group-label">Status</span>
        ${STATUS_FILTER_CHIPS.map(statusChip)}
      </div>
      <div
        class="cts-log-filter-group"
        role="group"
        aria-label="Filter by result"
        @keydown=${this._handleChipGroupKeydown}
      >
        <span class="cts-log-filter-group-label">Result</span>
        ${RESULT_FILTER_CHIPS.map(resultChip)}
      </div>
    `;
  }

  _renderActiveFilterSummary(filteredCount) {
    const hasFacet = this._statusFilter.size > 0 || this._resultFilter.size > 0;
    const hasSearch = this._searchText.trim().length > 0;
    if (!hasFacet && !hasSearch) return nothing;
    const truncatedMarker = this._truncated && filteredCount >= MAX_FILTERED_LOGS ? "+" : "";
    const matchLabel = filteredCount === 1 ? "match" : "matches";
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
    const statusVariant = STATUS_BADGE_VARIANTS[log.status] || "skip";
    const resultVariant = RESULT_BADGE_VARIANTS[log.result] || "skip";
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
                  <cts-tooltip content="${formatAbsoluteTime(log.started)}" placement="top">
                    <span class="cts-log-card-meta-value">
                      ${formatRelativeTime(log.started)}
                    </span>
                  </cts-tooltip>
                </span>
              `
            : nothing}
          ${planHref
            ? html`
                <span class="cts-log-card-meta-item">
                  <span class="cts-log-card-meta-key">Plan</span>
                  <a class="cts-log-card-plan-link" href="${planHref}"
                    >${this._planNames.get(log.planId) ?? log.planId}</a
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
      <cts-modal id="cts-log-list-config-modal" heading="Configuration">
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
        <cts-json-editor
          id="cts-log-list-config-editor"
          class="config-json"
          readonly
          aria-label="Test configuration JSON"
          .value=${configJson}
        ></cts-json-editor>
      </cts-modal>
    `;
  }

  _renderLoading() {
    return html`
      <div class="cts-log-list-loading">
        <cts-spinner size="lg" label="Loading logs"></cts-spinner>
        <span>Loading logs…</span>
      </div>
    `;
  }

  _renderError() {
    return html`
      <cts-alert variant="danger" role="alert"> <strong>Error:</strong> ${this._error} </cts-alert>
    `;
  }

  _renderEmpty(hasFilter) {
    const heading = hasFilter ? "No logs match the active filter" : "No logs to show";
    const body = hasFilter
      ? "Try clearing one or more filters to widen the search."
      : "Logs will appear here as tests are scheduled.";
    return html`
      <cts-empty-state
        icon="folder"
        heading="${heading}"
        body="${body}"
        data-testid="log-list-empty"
      ></cts-empty-state>
    `;
  }

  render() {
    if (this._loading) {
      return html`
        ${this._renderSearchAndSort()} ${this._renderFilterRow()} ${this._renderLoading()}
      `;
    }
    if (this._error) {
      return html`
        ${this._renderSearchAndSort()} ${this._renderFilterRow()} ${this._renderError()}
      `;
    }
    const filtered = this._filteredLogs();
    const searched = this._searchedLogs(filtered);
    const sorted = this._sortedLogs(searched);
    const visible = sorted.slice(0, this._visibleCount);
    const hasMore = sorted.length > visible.length;
    const hasFilter =
      this._statusFilter.size > 0 ||
      this._resultFilter.size > 0 ||
      this._searchText.trim().length > 0;
    const empty = sorted.length === 0;

    return html`
      ${this._renderSearchAndSort()} ${this._renderFilterRow()}
      ${this._renderActiveFilterSummary(sorted.length)}
      ${empty
        ? this._renderEmpty(hasFilter)
        : html`
            <div class="cts-log-list-items" data-testid="log-list-items">
              ${repeat(
                visible,
                (log) => log.testId,
                (log) => this._renderCard(log),
              )}
            </div>
          `}
      <div class="cts-log-list-footer">
        ${hasMore
          ? html`
              <cts-button
                variant="secondary"
                size="md"
                data-testid="log-list-show-more"
                label="Show more (${visible.length} of ${sorted.length})"
                @cts-click=${this._handleShowMoreClick}
              ></cts-button>
            `
          : nothing}
        ${this._truncated
          ? html`
              <p class="cts-log-list-truncation" data-testid="log-list-truncation">
                Showing the first ${MAX_FILTERED_LOGS} matches. Refine the filter to narrow further.
              </p>
            `
          : nothing}
      </div>
      ${this._renderConfigModal()}
    `;
  }
}

customElements.define("cts-log-list", CtsLogList);
