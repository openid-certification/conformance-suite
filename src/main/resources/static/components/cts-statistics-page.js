import { LitElement, html, nothing, css } from "lit";
import { classMap } from "lit/directives/class-map.js";
import "./cts-alert.js";
import "./cts-button.js";
import "./cts-chart.js";
import "./cts-empty-state.js";
import "./cts-loading-state.js";
import "./cts-spinner.js";
import "./cts-time.js";
import {
  assignFamilySlots,
  familiesWithRuns,
  foldOther,
  otherBreakdown,
  plansDatasets,
  resultsDatasets,
  runsDatasets,
  sliceRange,
  usersDatasets,
} from "./statistics-model.js";

/** @typedef {import("./statistics-model.js").StatisticsData} StatisticsData */
/** @typedef {import("./statistics-model.js").ChartDataset} ChartDataset */

/** Admin-only endpoint backing the whole page; one payload feeds every chart. */
const ENDPOINT = "/api/statistics/overview";

/**
 * Poll cadence while the server is computing a snapshot. Fast for the first
 * half-minute (a warm database answers in seconds and the admin is watching
 * a spinner), then slower, because past that point this is a multi-minute
 * whole-collection aggregation and there is no point hammering it.
 */
const POLL_FAST_MS = 2000;
const POLL_SLOW_MS = 5000;
const POLL_FAST_WINDOW_MS = 30000;
/** Stop polling and offer a Retry rather than spinning forever. */
const POLL_GIVE_UP_MS = 600000;

/**
 * Lead line for the tooltip footer that names what the folded "Other"
 * segment contains. Without it the family lines read as extra detail about
 * the series the pointer is actually on.
 */
const OTHER_FOOTER_HEADING = "Other includes:";

const GIVE_UP_MESSAGE =
  "Statistics are still being computed after 10 minutes. The server may be busy — try again.";
const FORBIDDEN_MESSAGE = "Statistics are only available to administrators.";
const UNEXPECTED_MESSAGE = "The statistics endpoint returned an unexpected response.";

/**
 * The KPI row, in display order. `key` indexes the payload's `tiles` object
 * and also names the tile's `data-testid` (`stat-tile-totalTests`, …).
 * These are whole-database counters: the range and family controls below
 * deliberately do not scope them.
 * @type {Array<{key: string, label: string, hint: string}>}
 */
const TILES = [
  { key: "totalTests", label: "Test runs", hint: "All time" },
  { key: "totalPlans", label: "Test plans", hint: "All time" },
  { key: "totalUsers", label: "Users", hint: "Ever ran a test" },
  { key: "testsLast24h", label: "Runs last 24 h", hint: "Rolling window" },
  { key: "testsLast7d", label: "Runs last 7 d", hint: "Rolling window" },
  { key: "testsLast30d", label: "Runs last 30 d", hint: "Rolling window" },
  { key: "inProgress", label: "In progress", hint: "Running or waiting" },
  { key: "stuck", label: "Stuck / abandoned (>24 h)", hint: "Non-terminal, started over 24 h ago" },
  { key: "certifiedPlans", label: "Certified plans", hint: "Made immutable" },
];

/**
 * Range presets, date-range-first per the dashboard filter convention.
 * "12 months" is the default because the question this page answers day to
 * day is recent usage; "All time" and "24 months" are one click away.
 * @type {Array<{value: string, label: string}>}
 */
const RANGE_PRESETS = [
  { value: "12m", label: "12 months" },
  { value: "24m", label: "24 months" },
  { value: "all", label: "All time" },
];

/**
 * Exact grouped figures ("91,800"), never compacted. The dashboard
 * convention would auto-compact past 10k, but this is an admin console
 * where the difference between 91,800 and 91,842 is the point.
 */
const NUMBER_FORMAT = new Intl.NumberFormat();

/**
 * Render a boolean as an ARIA state string. Returning the literal union
 * rather than `String(value)` keeps `lit-analyzer` happy about
 * `aria-pressed` / `aria-busy`, which only accept `"true"` / `"false"`.
 * @param {boolean} value - The state.
 * @returns {"true"|"false"} The attribute value.
 */
function aria(value) {
  return value ? "true" : "false";
}

const STYLE_ID = "cts-statistics-page-styles";

const STYLE_TEXT = css`
  cts-statistics-page {
    display: block;
    font-family: var(--font-sans);
    color: var(--fg);
  }
  .cts-stats-section-heading {
    margin: var(--space-6) 0 var(--space-3);
    font-size: var(--fs-16);
    font-weight: var(--fw-bold);
    line-height: var(--lh-snug);
    color: var(--fg);
  }
  .cts-stats-section-heading:first-child {
    margin-top: 0;
  }

  /* KPI row. auto-fit keeps nine tiles on one or two rows on a desktop and
     collapses to a single column on a phone without a media query. */
  .cts-stats-tiles {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: var(--space-3);
  }
  .cts-stats-tile {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding: var(--space-4);
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
  }
  .cts-stats-tile-value {
    font-size: var(--fs-28);
    font-weight: var(--fw-bold);
    line-height: var(--lh-tight);
    color: var(--fg);
    /* Proportional figures: tabular-nums is for columns that must align,
       and it makes a standalone display-size number look loose. */
    font-variant-numeric: proportional-nums;
  }
  .cts-stats-tile-label {
    font-size: var(--fs-13);
    color: var(--fg);
  }
  .cts-stats-tile-hint {
    font-size: var(--fs-12);
    color: var(--fg-soft);
  }

  .cts-stats-toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    margin-top: var(--space-5);
  }
  .cts-stats-asof {
    margin: 0;
    font-size: var(--fs-13);
    color: var(--fg-soft);
  }

  /* One filter row above everything it scopes — never per chart. */
  .cts-stats-filters {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    margin: var(--space-4) 0;
  }
  .cts-stats-range {
    display: inline-flex;
    border: 1px solid var(--ink-300);
    border-radius: var(--radius-2);
    overflow: hidden;
  }
  .cts-stats-range button {
    padding: 0 var(--space-3);
    height: var(--control-height);
    border: 0;
    border-right: 1px solid var(--ink-300);
    background: var(--bg-elev);
    color: var(--fg);
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    line-height: 1;
    cursor: pointer;
  }
  .cts-stats-range button:last-child {
    border-right: 0;
  }
  .cts-stats-range button:hover {
    background: var(--bg-muted);
  }
  .cts-stats-range button[aria-pressed="true"] {
    background: var(--ink-900);
    color: var(--ink-0);
  }
  .cts-stats-range button:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }

  /* Mirrors cts-form-field's .oidf-select, which is scoped to
     .oidf-form-field and so does not reach a bare select on this page. */
  .cts-stats-filters .oidf-select {
    height: var(--control-height);
    padding: 0 36px 0 var(--space-3);
    border: 1px solid var(--ink-300);
    border-radius: var(--radius-2);
    background-color: var(--bg-elev);
    color: var(--fg);
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    line-height: 1;
    appearance: none;
    -webkit-appearance: none;
    background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 16 16'><path fill='none' stroke='%2371695E' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' d='M4 6l4 4 4-4'/></svg>");
    background-repeat: no-repeat;
    background-position: right 12px center;
  }
  .cts-stats-filters .oidf-select:focus {
    outline: none;
    border-color: var(--orange-400);
    box-shadow: var(--focus-ring);
  }

  .cts-stats-charts {
    display: grid;
    /* min() so a viewport narrower than the track floor still gets a
       single full-width column instead of a horizontally scrolling page. */
    grid-template-columns: repeat(auto-fit, minmax(min(420px, 100%), 1fr));
    gap: var(--space-5);
    /* Refetch keeps the frame: the previous render stays put and only
       dims, so there is no skeleton flash and no layout jump. */
    transition: opacity var(--dur-2) var(--ease-standard);
  }
  .cts-stats-charts.is-busy {
    opacity: 0.55;
  }
  @media (prefers-reduced-motion: reduce) {
    .cts-stats-charts {
      transition: none;
    }
  }
  /* The unresolved-plans disclosure borrows cts-chart's own .cts-chart-data
     / .cts-chart-table rules so it is visually the same object as a chart's
     data table; only its own spacing and hint are declared here. It renders
     inside the trends section, where four <cts-chart>s have always injected
     those rules. */
  /* Element + class so the spacing wins over .cts-chart-data's own
     margin-top regardless of which component injected its stylesheet
     first. */
  details.cts-stats-unresolved {
    margin-top: var(--space-5);
  }
  .cts-stats-unresolved-hint {
    margin: var(--space-2) 0 0;
    font-size: var(--fs-12);
    color: var(--fg-soft);
  }

  .cts-stats-chart {
    padding: var(--space-4);
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    /* The chart frame is a fixed 320px; without this the grid item can be
       wider than its track and force a horizontal page scrollbar. */
    min-width: 0;
  }
`;

/**
 * Append the scoped stylesheet to `<head>` once per page lifetime.
 * @returns {void}
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/**
 * Suite-wide usage dashboard for `statistics.html`: a KPI row over four
 * monthly charts, all fed by one `GET /api/statistics/overview` payload.
 *
 * The endpoint serves a snapshot recomputed in the background at most every
 * 12 hours, so the component has three things to handle beyond a plain
 * fetch: a `202 pending` state it polls through (2 s for the first 30 s,
 * then 5 s, giving up after 10 minutes), a `refreshing: true` flag that
 * means "keep showing this snapshot, a newer one is on the way", and a
 * `lastError` that reports a failed recompute while an older snapshot is
 * still being served.
 *
 * Colour is an identity here: {@link assignFamilySlots} is computed once
 * from the FULL payload, so changing the range or the family filter
 * re-slices the data without ever repainting a family.
 *
 * Light DOM (`createRenderRoot()` returns `this`) so the page's design-system
 * tokens and stylesheet reach the rendered markup.
 *
 * Takes no attributes — the endpoint's 403 is the authoritative admin check,
 * so the page needs no `is-admin` input.
 * @property {undefined} [noAttributes] - This component has no public
 *   attributes or properties; all state is internal and derived from the
 *   statistics endpoint.
 */
class CtsStatisticsPage extends LitElement {
  static properties = {
    _status: { state: true },
    _payload: { state: true },
    _range: { state: true },
    _family: { state: true },
    _busy: { state: true },
    _errorMessage: { state: true },
    _dismissedErrorAt: { state: true },
  };

  constructor() {
    super();
    /**
     * The request lifecycle. Whether there is anything to CHART is a
     * property of the payload, not of this machine — see `_hasMonths()`.
     * @type {"loading"|"pending"|"ready"|"forbidden"|"error"}
     */
    this._status = "loading";
    /** @type {any} The whole response body, not just `data`. */
    this._payload = null;
    /** @type {string} One of the RANGE_PRESETS values. Defaults to "12m". */
    this._range = "12m";
    /** @type {string} Selected family, `""` for all. */
    this._family = "";
    /** @type {boolean} A request is in flight, or the server is recomputing. */
    this._busy = false;
    /** @type {string} */
    this._errorMessage = "";
    /** @type {string} `failedAt` of the lastError the user dismissed. */
    this._dismissedErrorAt = "";
    /** @type {Record<string, string>} Family → colour token, from the FULL payload. */
    this._slots = {};
    /** @type {ReturnType<typeof setTimeout>|null} */
    this._pollTimer = null;
    /** @type {number|null} When the current polling episode began. */
    this._pollStartedAt = null;
    /** @type {AbortController|null} */
    this._abort = null;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    this._load(false);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._stopPolling();
    if (this._abort) {
      this._abort.abort();
      this._abort = null;
    }
  }

  // --- Data ------------------------------------------------------------

  /**
   * Fetch the snapshot and move the state machine. Any in-flight request is
   * aborted first, so a Refresh during a poll cannot land out of order.
   * @param {boolean} refresh - Ask the server to recompute (`?refresh=true`).
   * @returns {Promise<void>}
   */
  async _load(refresh) {
    this._abortInFlight();
    const controller = new AbortController();
    this._abort = controller;
    this._busy = true;
    this._errorMessage = "";
    if (!this._payload) this._status = "loading";

    try {
      const response = await fetch(refresh ? `${ENDPOINT}?refresh=true` : ENDPOINT, {
        credentials: "same-origin",
        headers: { Accept: "application/json" },
        signal: controller.signal,
      });
      // A response that arrives after this request was superseded (a Refresh
      // overtook a poll) or after disconnect must not touch state. Both of
      // those paths abort the controller, so the signal is the whole test —
      // and, unlike comparing `this._abort`, it stays correct once the
      // controller has been settled below.
      if (controller.signal.aborted) return;
      const body = await this._readJson(response);
      if (controller.signal.aborted) return;
      this._settle(controller);
      // _apply is inside the try on purpose: a payload that breaks the
      // shaping helpers must surface as the error state, not vanish.
      this._apply(response, body);
    } catch (err) {
      if (controller.signal.aborted) return;
      this._settle(controller);
      this._fail(`Could not load statistics: ${this._messageOf(err)}`);
    }
  }

  /**
   * Forget the controller once its request has finished, but only if it is
   * still the current one.
   * @param {AbortController} controller - The finished request's controller.
   * @returns {void}
   */
  _settle(controller) {
    if (this._abort === controller) this._abort = null;
  }

  /**
   * Parse a JSON body, tolerating an empty one (403 has no body at all).
   * @param {Response} response - The fetch response.
   * @returns {Promise<any>} The parsed body, or null.
   */
  async _readJson(response) {
    try {
      return await response.json();
    } catch {
      // 403 is documented as an empty body, and a proxy error page is not
      // JSON either; the status code alone drives those branches.
      return null;
    }
  }

  /**
   * Move the state machine for one response.
   * @param {Response} response - The fetch response.
   * @param {any} body - Its parsed body, or null.
   * @returns {void}
   */
  _apply(response, body) {
    if (response.status === 401 || response.status === 403) {
      this._stopPolling();
      this._busy = false;
      this._status = "forbidden";
      return;
    }

    if (response.status === 202) {
      this._status = "pending";
      this._schedulePoll();
      return;
    }

    if (!response.ok) {
      this._fail((body && body.message) || `The server returned HTTP ${response.status}.`);
      return;
    }

    // Validate before committing anything: a half-applied payload would
    // leave `_payload` set for `render()` while the error state says
    // otherwise. `families` and `months` are the two arrays every chart
    // indexes, so they are what "this is a snapshot" means here.
    const data = body && body.data;
    if (!data || !Array.isArray(data.families) || !Array.isArray(data.months)) {
      this._fail(UNEXPECTED_MESSAGE);
      return;
    }

    // From the FULL payload, never the slice — a family keeps its colour
    // across every range and filter change.
    this._slots = assignFamilySlots(data);
    this._payload = body;
    this._status = "ready";

    if (body.refreshing === true) {
      // Stale-while-revalidate: keep this snapshot on screen (dimmed) and
      // poll for the newer one.
      this._schedulePoll();
      return;
    }
    this._stopPolling();
    this._busy = false;
  }

  /**
   * Whether the snapshot on hand has anything to chart. Derived from the
   * payload on every render rather than mirrored into `_status`, so the two
   * can never disagree — and so a failed refresh (`_status === "error"`)
   * still shows the charts it has.
   * @returns {boolean} True when the snapshot covers at least one month.
   */
  _hasMonths() {
    const months = this._payload && this._payload.data && this._payload.data.months;
    return Array.isArray(months) && months.length > 0;
  }

  /**
   * Enter the error state: stop polling, surface the message, but keep any
   * snapshot already on screen — a failed refresh must not blank the page.
   * @param {string} message - What to tell the admin.
   * @returns {void}
   */
  _fail(message) {
    this._stopPolling();
    this._busy = false;
    this._errorMessage = message;
    this._status = "error";
  }

  /**
   * Schedule the next poll, or give up. The cadence is measured from the
   * start of the polling episode, not from the last request, so a slow
   * response cannot stretch the fast window.
   * @returns {void}
   */
  _schedulePoll() {
    this._clearTimer();
    if (this._pollStartedAt === null) this._pollStartedAt = Date.now();
    const elapsed = Date.now() - this._pollStartedAt;
    if (elapsed >= POLL_GIVE_UP_MS) {
      this._fail(GIVE_UP_MESSAGE);
      return;
    }
    const delay = elapsed < POLL_FAST_WINDOW_MS ? POLL_FAST_MS : POLL_SLOW_MS;
    this._pollTimer = setTimeout(() => {
      this._pollTimer = null;
      this._load(false);
    }, delay);
  }

  /** @returns {void} */
  _clearTimer() {
    if (this._pollTimer === null) return;
    clearTimeout(this._pollTimer);
    this._pollTimer = null;
  }

  /** @returns {void} */
  _stopPolling() {
    this._clearTimer();
    this._pollStartedAt = null;
  }

  /** @returns {void} */
  _abortInFlight() {
    if (!this._abort) return;
    this._abort.abort();
    this._abort = null;
  }

  /**
   * @param {unknown} err - Whatever the fetch or JSON parse threw.
   * @returns {string} A message safe to render.
   */
  _messageOf(err) {
    if (err instanceof Error && err.message) return err.message;
    return "network error";
  }

  // --- Events ----------------------------------------------------------

  /**
   * Force a recompute. Restarts the polling episode, so the give-up budget
   * is measured from this click rather than from an earlier one.
   * @returns {void}
   */
  _handleRefresh() {
    this._stopPolling();
    // A Refresh clicked while an earlier refresh's error is on screen must
    // clear that error, not leave a message-less danger alert hanging over
    // the charts until the response lands. Mirrors _handleRetry.
    if (this._payload) this._status = "ready";
    this._load(true);
  }

  /**
   * Retry after an error, without forcing a recompute. Any snapshot still on
   * screen goes back to being the current state while the request is away.
   * @returns {void}
   */
  _handleRetry() {
    this._stopPolling();
    if (this._payload) this._status = "ready";
    this._load(false);
  }

  /**
   * @param {Event} event - Click on one of the range preset buttons.
   * @returns {void}
   */
  _handleRange(event) {
    const button = /** @type {HTMLElement} */ (event.currentTarget);
    const range = button.dataset.range;
    if (range) this._range = range;
  }

  /**
   * @param {Event} event - Change on the family select.
   * @returns {void}
   */
  _handleFamily(event) {
    this._family = /** @type {HTMLSelectElement} */ (event.currentTarget).value;
  }

  /**
   * Remember which failure the admin dismissed, so the warning stays gone
   * across re-renders but re-arms for the next distinct failure.
   * @returns {void}
   */
  _handleLastErrorDismissed() {
    const lastError = this._payload && this._payload.lastError;
    this._dismissedErrorAt = (lastError && lastError.failedAt) || "dismissed";
  }

  // --- Render ----------------------------------------------------------

  render() {
    if (this._status === "forbidden") {
      return html`<cts-alert variant="warning" data-testid="stats-forbidden"
        >${FORBIDDEN_MESSAGE}</cts-alert
      >`;
    }

    const data = (this._payload && this._payload.data) || null;
    const isLoading = this._status === "loading" || this._status === "pending";

    return html`
      ${this._renderError()} ${data ? this._renderTiles(data) : nothing}
      ${data ? this._renderToolbar() : nothing} ${this._renderLastError()}
      ${isLoading ? this._renderLoading() : nothing}
      ${data && this._hasMonths() ? this._renderTrends(data) : nothing}
      ${data && !this._hasMonths() ? this._renderEmpty() : nothing}
    `;
  }

  /**
   * @returns {unknown} The blocking-error alert, or nothing.
   */
  _renderError() {
    if (this._status !== "error") return nothing;
    return html`
      <cts-alert variant="danger" data-testid="stats-error">
        ${this._errorMessage}
        <cts-button
          variant="secondary"
          label="Retry"
          data-testid="stats-retry"
          @cts-click=${this._handleRetry}
        ></cts-button>
      </cts-alert>
    `;
  }

  /**
   * The "an older snapshot is being served because the last recompute
   * failed" warning. Dismissible, and re-armed by the next distinct failure.
   * @returns {unknown} The alert, or nothing.
   */
  _renderLastError() {
    const lastError = this._payload && this._payload.lastError;
    if (!lastError || !lastError.message) return nothing;
    if (this._dismissedErrorAt && this._dismissedErrorAt === (lastError.failedAt || "dismissed")) {
      return nothing;
    }
    return html`
      <cts-alert
        variant="warning"
        dismissible
        data-testid="stats-last-error"
        @cts-alert-dismissed=${this._handleLastErrorDismissed}
        >The last recompute failed (${lastError.message}); showing the previous snapshot.</cts-alert
      >
    `;
  }

  /**
   * @returns {unknown} The loading / first-computation block.
   */
  _renderLoading() {
    // A 202 with a snapshot already on screen is not the first computation:
    // the server restarted and lost its cache, or the TTL expired while the
    // page was open. Saying "for the first time" over the dimmed charts the
    // admin is looking at contradicts what they can see.
    const label = this._status === "pending" ? this._pendingLabel() : "Loading statistics";
    return html`<cts-loading-state label=${label} data-testid="stats-loading"></cts-loading-state>`;
  }

  /**
   * @returns {string} The 202 caption. `cts-loading-state` appends its own
   *   ellipsis, so neither variant carries one.
   */
  _pendingLabel() {
    return this._payload ? "Recomputing statistics" : "Computing statistics for the first time";
  }

  /**
   * @returns {unknown} The empty state shown when the database has no runs.
   */
  _renderEmpty() {
    return html`
      <cts-empty-state
        icon="chart-bar-vertical-01"
        heading="No test data yet"
        body="Run a test and the monthly charts will appear here."
        data-testid="stats-empty"
      ></cts-empty-state>
    `;
  }

  /**
   * @param {StatisticsData} data - The full payload.
   * @returns {unknown} The KPI row.
   */
  _renderTiles(data) {
    const tiles = data.tiles || {};
    return html`
      <h2 class="cts-stats-section-heading">Totals</h2>
      <div class="cts-stats-tiles" data-testid="stats-tiles">
        ${TILES.map(
          (tile) => html`
            <div class="cts-stats-tile" data-testid="stat-tile-${tile.key}">
              <span class="cts-stats-tile-value"
                >${NUMBER_FORMAT.format(Number(tiles[tile.key]) || 0)}</span
              >
              <span class="cts-stats-tile-label">${tile.label}</span>
              <span class="cts-stats-tile-hint">${tile.hint}</span>
            </div>
          `,
        )}
      </div>
    `;
  }

  /**
   * @returns {unknown} Refresh button, busy spinner, and the snapshot age.
   */
  _renderToolbar() {
    const computedAt = (this._payload && this._payload.computedAt) || "";
    return html`
      <div class="cts-stats-toolbar">
        <cts-button
          variant="secondary"
          icon="arrow-reload-02"
          label="Refresh"
          data-testid="stats-refresh"
          ?disabled=${this._busy}
          @cts-click=${this._handleRefresh}
        ></cts-button>
        ${this._busy
          ? html`<cts-spinner size="sm" label="Refreshing statistics"></cts-spinner>`
          : nothing}
        <p class="cts-stats-asof" role="status" data-testid="stats-computed-at">
          Data as of <cts-time value=${computedAt}></cts-time>
        </p>
      </div>
    `;
  }

  /**
   * Tooltip footer for the folded "Other" segment: the families inside it
   * and their counts for the hovered month. Returns an empty list — so
   * Chart.js draws no footer at all — once a single family is on screen and
   * nothing is folded.
   * @param {StatisticsData} sliced - The range-sliced payload the chart was
   *   built from.
   * @param {string} source - `"runs"` or `"plans"`.
   * @returns {(hoveredIndex: number) => Array<string>} The footer callback.
   */
  _footerFor(sliced, source) {
    return (hoveredIndex) => {
      if (this._family) return [];
      const lines = otherBreakdown(sliced, this._slots, hoveredIndex, source);
      return lines.length === 0 ? [] : [OTHER_FOOTER_HEADING, ...lines];
    };
  }

  /**
   * Filter row, the four charts, and the unresolved-plans disclosure. The
   * filters scope everything below them, so the numbers on every chart
   * always agree.
   * @param {StatisticsData} data - The full payload.
   * @returns {unknown} The trends section.
   */
  _renderTrends(data) {
    const sliced = sliceRange(data, this._range);
    const slots = this._slots;
    const family = this._family;
    const months = sliced.months || [];
    // Fold the neutral tail into one "Other" series only while every family
    // is on screen. With a family selected there is exactly one dataset, and
    // that family may itself be a neutral-slot one — folding would rename it
    // "Other" and drop the name the user just picked from the legend,
    // tooltip and data table.
    const fold = (/** @type {Array<ChartDataset>} */ datasets) =>
      family ? datasets : foldOther(datasets);

    return html`
      <h2 class="cts-stats-section-heading">Trends</h2>
      <div class="cts-stats-filters">
        <div class="cts-stats-range" role="group" aria-label="Range" data-testid="stats-range">
          ${RANGE_PRESETS.map(
            (preset) => html`
              <button
                type="button"
                data-range=${preset.value}
                aria-pressed=${aria(this._range === preset.value)}
                @click=${this._handleRange}
              >
                ${preset.label}
              </button>
            `,
          )}
        </div>
        <select
          class="oidf-select"
          aria-label="Spec family"
          data-testid="stats-family"
          .value=${family}
          @change=${this._handleFamily}
        >
          <option value="">All families</option>
          ${familiesWithRuns(data).map((name) => html`<option value=${name}>${name}</option>`)}
        </select>
      </div>

      <div
        class=${classMap({ "cts-stats-charts": true, "is-busy": this._busy })}
        data-testid="stats-charts"
        aria-busy=${aria(this._busy)}
      >
        <div class="cts-stats-chart" data-testid="stats-chart-runs">
          <cts-chart
            heading="Test module runs per month"
            category-label="Month"
            stacked
            .labels=${months}
            .datasets=${fold(runsDatasets(sliced, slots, family))}
            .tooltipFooter=${this._footerFor(sliced, "runs")}
          ></cts-chart>
        </div>
        <div class="cts-stats-chart" data-testid="stats-chart-plans">
          <cts-chart
            heading="Test plans per month"
            category-label="Month"
            stacked
            .labels=${months}
            .datasets=${fold(plansDatasets(sliced, slots, family))}
            .tooltipFooter=${this._footerFor(sliced, "plans")}
          ></cts-chart>
        </div>
        <div class="cts-stats-chart" data-testid="stats-chart-results">
          <cts-chart
            heading="Results per month"
            category-label="Month"
            stacked
            .labels=${months}
            .datasets=${resultsDatasets(sliced, family)}
          ></cts-chart>
        </div>
        <div class="cts-stats-chart" data-testid="stats-chart-users">
          <cts-chart
            heading="Users per month — all families"
            category-label="Month"
            .labels=${months}
            .datasets=${usersDatasets(sliced)}
          ></cts-chart>
        </div>
      </div>
      ${this._renderUnresolved(data)}
    `;
  }

  /**
   * The plan names that fell into "Other / retired", biggest first — the
   * server's top 20. Collapsed by default and absent entirely when the list
   * is empty: it explains one bar segment, so it must not compete with the
   * charts it sits under.
   *
   * All-time, like the tiles: the list is not scoped by the range control,
   * so it is read from the FULL payload rather than the slice.
   * @param {StatisticsData} data - The full payload.
   * @returns {unknown} The disclosure, or nothing.
   */
  _renderUnresolved(data) {
    const plans = Array.isArray(data.unresolvedPlans) ? data.unresolvedPlans : [];
    if (plans.length === 0) return nothing;
    return html`
      <details class="cts-stats-unresolved cts-chart-data" data-testid="stats-unresolved">
        <summary>Plans not mapped to a spec family (${plans.length})</summary>
        <p class="cts-stats-unresolved-hint">
          Retired, renamed or hidden plan names; their runs are counted under “Other / retired”.
        </p>
        <table class="cts-chart-table">
          <thead>
            <tr>
              <th scope="col">Plan name</th>
              <th scope="col">Runs</th>
            </tr>
          </thead>
          <tbody>
            ${plans.map(
              (plan) => html`
                <tr>
                  <th scope="row">${plan.planName}</th>
                  <td>${NUMBER_FORMAT.format(Number(plan.runs) || 0)}</td>
                </tr>
              `,
            )}
          </tbody>
        </table>
      </details>
    `;
  }
}

customElements.define("cts-statistics-page", CtsStatisticsPage);

export {};
