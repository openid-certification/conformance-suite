import { LitElement, html, nothing, css } from "lit";
import { classMap } from "lit/directives/class-map.js";
import "./cts-alert.js";
import "./cts-button.js";
import "./cts-chart.js";
import "./cts-empty-state.js";
import "./cts-loading-state.js";
import "./cts-spinner.js";
import "./cts-statistics-filters.js";
import "./cts-statistics-insights.js";
import "./cts-time.js";
import { ctsToast } from "../js/cts-toast-api.js";
import {
  EMPTY_OPTIONS,
  assignFamilySlots,
  buildChartInputs,
  buildDistributions,
  defaultFilterState,
  drillDownFamily,
  drillDownUrl,
  familiesWithRuns,
  formatBytes,
  hasAnyData,
  isFiltered,
  isNarrowed,
  memoiseByArgs,
  otherBreakdown,
  queryFromState,
  rangePreset,
  rememberOptions,
  sameState,
  stateFromUrl,
  urlFromState,
} from "./statistics-model.js";

/** @typedef {import("./statistics-model.js").StatisticsData} StatisticsData */
/** @typedef {import("./statistics-model.js").ChartDataset} ChartDataset */
/** @typedef {import("./statistics-model.js").FilterState} FilterState */
/** @typedef {import("./statistics-model.js").Distributions} Distributions */

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
 * How many times the unfiltered baseline is asked for. It is retried because
 * the first attempt can land while the very first snapshot is still being
 * computed (202), but it is capped because the page is perfectly usable
 * without it and a poll loop must not turn into two.
 */
const BASELINE_MAX_ATTEMPTS = 3;

/**
 * Lead line for the tooltip footer that names what the folded "Other"
 * segment contains. Without it the family lines read as extra detail about
 * the series the pointer is actually on.
 */
const OTHER_FOOTER_HEADING = "Other includes:";

/**
 * The tooltip footer naming what the folded "Other" segment contains for the
 * hovered period. Built once per payload (not per render) so `<cts-chart>`
 * is handed the same callback every time.
 * @param {StatisticsData} data - The payload the chart was built from.
 * @param {Record<string, string>} slots - Family → colour token.
 * @param {string} source - `"runs"`, `"plans"` or `"certified"`.
 * @param {boolean} folded - Whether the chart actually has an "Other" series.
 * @returns {(hoveredIndex: number) => Array<string>} The footer callback.
 */
function footerFor(data, slots, source, folded) {
  return (hoveredIndex) => {
    if (!folded) return [];
    const lines = otherBreakdown(data, slots, hoveredIndex, source);
    return lines.length === 0 ? [] : [OTHER_FOOTER_HEADING, ...lines];
  };
}

/**
 * @param {Record<string, string>} a - One family → colour mapping.
 * @param {Record<string, string>} b - Another.
 * @returns {boolean} True when they say the same thing.
 */
function sameSlots(a, b) {
  const keys = Object.keys(b);
  return keys.length === Object.keys(a).length && keys.every((family) => a[family] === b[family]);
}

const GIVE_UP_MESSAGE =
  "Statistics are still being computed after 10 minutes. The server may be busy — try again.";
const FORBIDDEN_MESSAGE = "Statistics are only available to administrators.";
const UNEXPECTED_MESSAGE = "The statistics endpoint returned an unexpected response.";

/**
 * The KPI row, in display order. `key` indexes the payload's `tiles` object
 * and also names the tile's `data-testid` (`stat-tile-totalTests`, …).
 * These are whole-database counters: the range and the filter row below them
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
  { key: "publishedPlans", label: "Published plans", hint: "Visible to everyone" },
];

/**
 * What activating a data-table row does, as `<cts-chart>` names its row
 * buttons ("List the test plans in 2026-06"). The keyboard twin of clicking
 * the column: it drills into the whole period rather than one family.
 */
const DRILL_DOWN_LABEL = "List the test plans in";

/**
 * What one period is called on an axis and in a heading, per granularity.
 * @type {Record<string, {axis: string, unit: string}>}
 */
const PERIOD_NAMES = {
  month: { axis: "Month", unit: "month" },
  week: { axis: "Week starting", unit: "week" },
};

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

  /* KPI row. auto-fit keeps ten tiles on one or two rows on a desktop and
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

  /* The storage row is the same tile, one step quieter: it answers "how big
     is this database", not "how much is it being used", so it sits under the
     usage counters rather than beside them. */
  .cts-stats-subheading {
    margin: var(--space-4) 0 var(--space-2);
    font-size: var(--fs-13);
    font-weight: var(--fw-bold);
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
     inside the trends section, where five <cts-chart>s have always injected
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

  /* cts-empty-state centres its own content; the action under it has to be
     centred too, or it reads as belonging to whatever comes next. */
  .cts-stats-no-match-action {
    display: flex;
    justify-content: center;
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
 * Suite-wide usage dashboard for `statistics.html`: a KPI row, a storage row,
 * a filter row, five trend charts and — in a sibling
 * `<cts-statistics-insights>` — the distributions, the activity heatmap and
 * the external servers, all fed by one `GET /api/statistics/overview`.
 *
 * The endpoint serves a snapshot recomputed in the background at most every
 * 12 hours, so the component has four things to handle beyond a plain fetch:
 * a `202 pending` state it polls through (2 s for the first 30 s, then 5 s,
 * giving up after 10 minutes), a `refreshing: true` flag that means "keep
 * showing this snapshot, a newer one is on the way", a `lastError` that
 * reports a failed recompute while an older snapshot is still being served,
 * and a `400 invalid` that reports a range or filter the server cannot use.
 *
 * Slicing is the server's: the range presets and every filter go out as
 * request parameters ({@link queryFromState}) and the payload comes back
 * already narrowed, so a filter change is a refetch. The snapshot is cached,
 * so that refetch is a cheap slice — the charts merely dim, keeping the
 * previous render on screen (stale-while-revalidate).
 *
 * The whole filter state lives in the page URL (`?range=&family=&plan=
 * &variant.<k>=&cert=`, written with `history.replaceState`), so a view is
 * shareable and survives a reload.
 *
 * The four family/result charts drill down: clicking a bar (or activating a
 * row button in a chart's data table, the keyboard equivalent) navigates to
 * `plans.html` filtered to the same slice plus the clicked period —
 * {@link drillDownUrl} builds the URL. A click on the folded "Other" series,
 * or under a synthetic family, is refused with a toast, because
 * `GET /api/plan` cannot express either.
 *
 * Colour is an identity here: {@link assignFamilySlots} is computed from an
 * UNFILTERED, all-time baseline payload fetched once on load, so no range or
 * filter can repaint a family (under a filter every other family is zero,
 * which would otherwise hand the first hue to whatever was selected).
 *
 * DOM hooks for e2e (`data-testid`):
 * `stats-forbidden`, `stats-error`, `stats-retry`, `stats-reset-filters`,
 * `stats-last-error`, `stats-loading`, `stats-empty`, `stats-tiles`,
 * `stat-tile-<key>`, `stats-storage`, `stat-storage-<collection>`,
 * `stats-refresh`, `stats-computed-at`, `stats-filters`,
 * `stats-range`, `stats-range-weekly`, `stats-range-monthly`, `stats-family`,
 * `stats-plan`, `stats-variant-<name>`, `stats-cert`, `stats-clear-filters`,
 * `stats-charts`, `stats-chart-runs`, `stats-chart-plans`,
 * `stats-chart-results`, `stats-chart-users`, `stats-chart-certified`,
 * `stats-no-match`, `stats-no-match-clear`, `stats-unresolved`,
 * `stats-insights` and, inside it (`cts-statistics-insights.js`),
 * `stats-distributions`, `stats-dist-variants`, `stats-dist-variant-<key>`,
 * `stats-dist-certs`, `stats-dist-entities`, `stats-heatmap`, `stats-hosts`;
 * plus `cts-chart-more` / `cts-heatmap-empty` from the two primitives and,
 * on the four drillable charts, `<cts-chart>`'s own `.cts-chart-row-link`
 * buttons — one per data-table row, the keyboard route into a period.
 *
 * Light DOM (`createRenderRoot()` returns `this`) so the page's design-system
 * tokens and stylesheet reach the rendered markup.
 *
 * Takes no attributes — the endpoint's 403 is the authoritative admin check,
 * so the page needs no `is-admin` input.
 * @property {undefined} [noAttributes] - This component has no public
 *   attributes or properties; all state is internal and derived from the
 *   statistics endpoint and the page URL.
 * @fires cts-drill-down - Before navigating to the plans listing a chart
 *   click resolved to. `detail: {url}`, cancelable: calling
 *   `preventDefault()` keeps the page where it is, which is how a story or a
 *   spec asserts the URL without navigating. Bubbles and is composed.
 */
class CtsStatisticsPage extends LitElement {
  static properties = {
    _status: { state: true },
    _payload: { state: true },
    _state: { state: true },
    _baseline: { state: true },
    _options: { state: true },
    _busy: { state: true },
    _errorMessage: { state: true },
    _errorAction: { state: true },
    _dismissedErrorAt: { state: true },
  };

  constructor() {
    super();
    /**
     * The request lifecycle. Whether there is anything to CHART is a
     * property of the payload, not of this machine — see `_hasPeriods()`.
     * @type {"loading"|"pending"|"ready"|"forbidden"|"error"}
     */
    this._status = "loading";
    /** @type {any} The whole response body, not just `data`. */
    this._payload = null;
    /** @type {FilterState} Range and filters; mirrored in the page URL. */
    this._state = defaultFilterState();
    /** @type {StatisticsData|null} Unfiltered, all-time payload: the colour and family-option source. */
    this._baseline = null;
    /** @type {import("./statistics-model.js").FilterOptions} What the filter selects offer. */
    this._options = EMPTY_OPTIONS;
    /** @type {boolean} A request is in flight, or the server is recomputing. */
    this._busy = false;
    /** @type {string} */
    this._errorMessage = "";
    /** @type {"retry"|"reset"} Which action the error alert offers. */
    this._errorAction = "retry";
    /** @type {string} `failedAt` of the lastError the user dismissed. */
    this._dismissedErrorAt = "";
    /** @type {Record<string, string>} Family → colour token. */
    this._slots = {};
    /** @type {"none"|"payload"|"baseline"} Where `_slots` came from. */
    this._slotSource = "none";
    /** @type {ReturnType<typeof setTimeout>|null} */
    this._pollTimer = null;
    /** @type {number|null} When the current polling episode began. */
    this._pollStartedAt = null;
    /** @type {AbortController|null} */
    this._abort = null;
    /** @type {AbortController|null} The baseline request's own controller. */
    this._baselineAbort = null;
    /** @type {number} How many baseline requests have been made. */
    this._baselineAttempts = 0;
    // Everything derived from a payload is memoised on the identity of what
    // it is derived from: the page re-renders three times per fetch (busy on,
    // payload, busy off) and <cts-chart> re-plots whenever the array it was
    // handed is not the same one as last time.
    this._view = memoiseByArgs(
      (
        /** @type {StatisticsData} */ data,
        /** @type {Record<string, string>} */ slots,
        /** @type {string} */ family,
      ) => {
        const inputs = buildChartInputs(data, slots, family);
        return {
          ...inputs,
          names: PERIOD_NAMES[inputs.granularity],
          footers: {
            runs: footerFor(data, slots, "runs", inputs.runs.folded),
            plans: footerFor(data, slots, "plans", inputs.plans.folded),
            certified: footerFor(data, slots, "certified", inputs.certified.folded),
          },
        };
      },
    );
    this._familyOptions = memoiseByArgs(familiesWithRuns);
    this._hasAnyData = memoiseByArgs(hasAnyData);
    this._distributions = memoiseByArgs(buildDistributions);
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    // The URL is the source of truth for the view, so a shared link opens
    // exactly what was shared; writing it straight back normalises it.
    this._state = stateFromUrl(window.location.search);
    this._syncUrl();
    // An unfiltered, whole-history view IS the baseline, so asking for it
    // twice would fetch the same payload twice; _apply adopts it instead.
    if (isNarrowed(this._state)) this._loadBaseline();
    this._load(false);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._stopPolling();
    if (this._abort) {
      this._abort.abort();
      this._abort = null;
    }
    if (this._baselineAbort) {
      this._baselineAbort.abort();
      this._baselineAbort = null;
    }
  }

  // --- Data ------------------------------------------------------------

  /**
   * Fetch the snapshot for the current filter state and move the state
   * machine. Any in-flight request is aborted first, so a Refresh during a
   * poll — or a filter change during either — cannot land out of order.
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

    const query = queryFromState(this._state);
    if (refresh) query.set("refresh", "true");

    try {
      const response = await fetch(`${ENDPOINT}?${query.toString()}`, {
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
   * Fetch the unfiltered, all-time snapshot that colours the charts and fills
   * the family select. It is the same cached cube the filtered request slices,
   * so this costs the server one extra slice and nothing else.
   *
   * Deliberately silent: a page whose baseline failed still charts everything
   * it has, falling back to colouring from the first payload it receives.
   * @returns {Promise<void>}
   */
  async _loadBaseline() {
    if (this._baseline || this._baselineAbort || this._baselineAttempts >= BASELINE_MAX_ATTEMPTS) {
      return;
    }
    this._baselineAttempts += 1;
    const controller = new AbortController();
    this._baselineAbort = controller;
    try {
      const response = await fetch(ENDPOINT, {
        credentials: "same-origin",
        headers: { Accept: "application/json" },
        signal: controller.signal,
      });
      if (controller.signal.aborted) return;
      const body = await this._readJson(response);
      if (controller.signal.aborted || !response.ok) return;
      const data = body && body.data;
      if (!data || !Array.isArray(data.families)) return;
      this._baseline = data;
      this._adoptSlots(data, true);
    } catch {
      // Network error, 202 while the first snapshot computes, a 403 — none of
      // them is worth an alert, because the request the page is really making
      // reports all three itself.
    } finally {
      if (this._baselineAbort === controller) this._baselineAbort = null;
    }
  }

  /**
   * Adopt the family → colour mapping. A filtered payload may only fill in
   * for a baseline that has not arrived yet, and is never allowed to replace
   * one: colour is an identity, and a filtered ranking is not one.
   * @param {StatisticsData} data - The payload to rank families from.
   * @param {boolean} fromBaseline - True when `data` is the unfiltered baseline.
   * @returns {void}
   */
  _adoptSlots(data, fromBaseline) {
    if (!fromBaseline && this._slotSource !== "none") return;
    const slots = assignFamilySlots(data);
    this._slotSource = fromBaseline ? "baseline" : "payload";
    // Identity matters: the chart inputs are memoised on it, so replacing an
    // equal mapping would re-plot all five charts for nothing — which is
    // exactly what the baseline landing a moment after the first payload
    // would otherwise do.
    if (sameSlots(this._slots, slots)) return;
    this._slots = slots;
    this.requestUpdate();
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

    // 400: the range or a filter cannot be used. Nothing was computed, so
    // retrying the same request is pointless — clearing the filters is the
    // way out, and the server's message says which one is at fault.
    if (response.status === 400) {
      this._fail(
        (body && body.message) || "The current filters could not be used.",
        isFiltered(this._state) ? "reset" : "retry",
      );
      return;
    }

    if (!response.ok) {
      this._fail((body && body.message) || `The server returned HTTP ${response.status}.`);
      return;
    }

    // Validate before committing anything: a half-applied payload would
    // leave `_payload` set for `render()` while the error state says
    // otherwise. `families` and `periods` are the two arrays every chart
    // indexes, so they are what "this is a snapshot" means here.
    const data = body && body.data;
    if (!data || !Array.isArray(data.families) || !Array.isArray(data.periods)) {
      this._fail(UNEXPECTED_MESSAGE);
      return;
    }

    if (!this._baseline && !isNarrowed(this._state)) {
      // Nothing is filtered and the range is the whole history, so this
      // payload is the baseline; a second request would fetch it again.
      this._baseline = data;
      this._adoptSlots(data, true);
    } else {
      this._adoptSlots(data, false);
      // A baseline that 202'd on load (no snapshot existed yet) can be had
      // now that one demonstrably does.
      if (!this._baseline) this._loadBaseline();
    }
    this._options = rememberOptions(this._options, data, this._state);
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
   * @returns {boolean} True when the snapshot covers at least one period.
   */
  _hasPeriods() {
    const periods = this._payload && this._payload.data && this._payload.data.periods;
    return Array.isArray(periods) && periods.length > 0;
  }

  /**
   * Enter the error state: stop polling, surface the message, but keep any
   * snapshot already on screen — a failed refresh must not blank the page.
   * @param {string} message - What to tell the admin.
   * @param {"retry"|"reset"} [action] - Which way out the alert offers.
   * @returns {void}
   */
  _fail(message, action = "retry") {
    this._stopPolling();
    this._busy = false;
    this._errorMessage = message;
    this._errorAction = action;
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
   * The way out of a 400: drop every filter (keeping the range, which is not
   * one) and ask again.
   * @returns {void}
   */
  _handleResetFilters() {
    this._applyState({ ...this._state, family: "", plan: "", variant: {}, cert: "" });
  }

  /**
   * @param {CustomEvent} event - `cts-filters-change` from the filter row,
   *   carrying the whole next state.
   * @returns {void}
   */
  _handleFiltersChange(event) {
    this._applyState(/** @type {FilterState} */ (event.detail));
  }

  /**
   * Adopt a new filter state: mirror it into the URL and refetch. A change
   * that would produce the same request is dropped, so re-picking the value
   * that is already selected costs nothing.
   * @param {FilterState} next - The state to move to.
   * @returns {void}
   */
  _applyState(next) {
    if (sameState(this._state, next)) return;
    this._state = { ...next, variant: { ...(next.variant || {}) } };
    this._syncUrl();
    this._stopPolling();
    if (this._payload) this._status = "ready";
    this._load(false);
  }

  /**
   * Write the current state into the page URL, leaving any parameter this
   * page does not own untouched. `replaceState`, not `pushState`: a filter
   * row is not navigation, and burying the way back to the previous page
   * under six presses of Back is worse than losing the filter history.
   * @returns {void}
   */
  _syncUrl() {
    const search = urlFromState(this._state, window.location.search);
    window.history.replaceState(null, "", window.location.pathname + search + window.location.hash);
  }

  /**
   * A click on the runs, plans or certified chart. Their datasets ARE spec
   * families, so the clicked series narrows the drill-down when the family
   * filter is not already set.
   * @param {CustomEvent} event - `cts-chart-click` from `<cts-chart>`.
   * @returns {void}
   */
  _handleFamilyChartClick(event) {
    this._drillDown(event.detail, (event.detail && event.detail.datasetLabel) || "");
  }

  /**
   * A click on the results chart. Its datasets are result buckets (`PASSED`,
   * `FAILED`, …), which `GET /api/plan` cannot filter on — a plan has no
   * single result — so the bucket is dropped and the click drills into the
   * same slice the whole column stands for: this period, under the filters
   * that are already set. It is still the useful answer to "what is behind
   * this bar", and the alternative (refusing the click) would make three of
   * the four charts behave one way and one another.
   * @param {CustomEvent} event - `cts-chart-click` from `<cts-chart>`.
   * @returns {void}
   */
  _handleResultChartClick(event) {
    this._drillDown(event.detail, "");
  }

  /**
   * Navigate to the plans listing for what was clicked.
   *
   * Emits a cancelable `cts-drill-down` first and only navigates if nothing
   * cancelled it: `window.location.assign` cannot be stubbed reliably, so
   * this is what lets a story or a spec assert the URL the page built without
   * tearing the page out from under itself. Production has no listener, so
   * the click navigates.
   * @param {{periodIndex: number}} detail - The `cts-chart-click` detail.
   * @param {string} family - The clicked dataset's family, or `""` when the
   *   chart's datasets are not families.
   * @returns {void}
   */
  _drillDown(detail, family) {
    const data = this._payload && this._payload.data;
    if (!data) return;
    const click = { periodIndex: detail && detail.periodIndex, family };
    const url = drillDownUrl(this._state, click, data);
    if (!url) {
      // "Other" is a fold of the families outside the seven colour slots, and
      // the two synthetic buckets are "these runs have no plan in the
      // registry" — neither is something the listing can be asked for.
      ctsToast({
        title: `No drill-down for “${drillDownFamily(this._state, click)}”`,
        message:
          "Those runs are not one spec family — pick a family in the filter row to list its plans.",
      });
      return;
    }
    const event = new CustomEvent("cts-drill-down", {
      bubbles: true,
      composed: true,
      cancelable: true,
      detail: { url },
    });
    this.dispatchEvent(event);
    if (!event.defaultPrevented) window.location.assign(url);
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
    // A filter that matches nothing still comes back with the full axis and a
    // zero in every cell — the periods are the cube's, not the filter's — so
    // without this the reader is left interpreting five charts of zeros.
    const noMatch =
      Boolean(data) &&
      this._hasPeriods() &&
      isFiltered(this._state) &&
      !this._hasAnyData(/** @type {StatisticsData} */ (data));

    return html`
      ${this._renderError()} ${data ? this._renderTiles(data) : nothing}
      ${data ? this._renderToolbar() : nothing} ${this._renderLastError()}
      ${data ? this._renderFilters(data) : nothing} ${isLoading ? this._renderLoading() : nothing}
      ${data && this._hasPeriods() && !noMatch ? this._renderTrends(data) : nothing}
      ${noMatch ? this._renderNoMatch() : nothing}
      ${data && !this._hasPeriods() ? this._renderEmpty() : nothing}
      ${data ? this._renderInsights(data, noMatch) : nothing}
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
        ${this._errorAction === "reset"
          ? html`<cts-button
              variant="secondary"
              label="Reset filters"
              data-testid="stats-reset-filters"
              @cts-click=${this._handleResetFilters}
            ></cts-button>`
          : html`<cts-button
              variant="secondary"
              label="Retry"
              data-testid="stats-retry"
              @cts-click=${this._handleRetry}
            ></cts-button>`}
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
   * @returns {unknown} The empty state shown when the range covers no data.
   */
  _renderEmpty() {
    // The filter row stays on screen above this, so "widen the range" is
    // advice the reader can act on without leaving the page.
    const narrowed = isNarrowed(this._state);
    return html`
      <cts-empty-state
        icon="chart-bar-vertical-01"
        heading=${narrowed ? "Nothing in this range" : "No test data yet"}
        body=${narrowed
          ? "No test runs were recorded in the selected range. Widen it to look further back."
          : "Run a test and the charts will appear here."}
        data-testid="stats-empty"
      ></cts-empty-state>
    `;
  }

  /**
   * The filters are valid and the range has data, but nothing in it matches:
   * five charts of zeros say so much less clearly than one sentence, and the
   * way out is one click.
   * @returns {unknown} The no-match state.
   */
  _renderNoMatch() {
    return html`
      <cts-empty-state
        icon="filter-off"
        heading="No test plans match these filters"
        body="Nothing in the selected range was run with this combination. Clear the filters to see everything again."
        data-testid="stats-no-match"
      ></cts-empty-state>
      <div class="cts-stats-no-match-action">
        <cts-button
          variant="secondary"
          label="Clear filters"
          data-testid="stats-no-match-clear"
          @cts-click=${this._handleResetFilters}
        ></cts-button>
      </div>
    `;
  }

  /**
   * @param {StatisticsData} data - The current payload.
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
      ${this._renderStorage(data)}
    `;
  }

  /**
   * How much room the database is taking up, one tile per collection. Same
   * mark as the counters above it, because it answers the same kind of
   * question, and — like them — it is a whole-database figure that neither the
   * range nor any filter scopes.
   * @param {StatisticsData} data - The current payload.
   * @returns {unknown} The storage row, or nothing when the server sent none.
   */
  _renderStorage(data) {
    const storage = Array.isArray(data.storage) ? data.storage : [];
    if (storage.length === 0) return nothing;
    return html`
      <h3 class="cts-stats-subheading">Storage</h3>
      <div class="cts-stats-tiles" data-testid="stats-storage">
        ${storage.map(
          (row) => html`
            <div class="cts-stats-tile" data-testid="stat-storage-${row.collection}">
              <span class="cts-stats-tile-value"
                >${NUMBER_FORMAT.format(Number(row.count) || 0)}</span
              >
              <span class="cts-stats-tile-label">${row.collection} documents</span>
              <span class="cts-stats-tile-hint">
                ${formatBytes(row.size)} data · ${formatBytes(row.storageSize)} on disk ·
                ${formatBytes(row.totalIndexSize)} indexes
              </span>
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
   * The filter row, above everything it scopes. Rendered as soon as there is
   * any payload — including one whose range turned out to be empty, so the
   * range that emptied it can be widened again.
   *
   * The family options come from the unfiltered baseline, so narrowing never
   * removes the option that would widen things back out.
   * @param {StatisticsData} data - The current payload.
   * @returns {unknown} The heading and the filter row.
   */
  _renderFilters(data) {
    return html`
      <h2 class="cts-stats-section-heading">Trends</h2>
      <cts-statistics-filters
        data-testid="stats-filters"
        range=${this._state.range}
        family=${this._state.family}
        plan=${this._state.plan}
        cert=${this._state.cert}
        .variant=${this._state.variant}
        .families=${this._familyOptions(this._baseline || data)}
        .options=${this._options}
        @cts-filters-change=${this._handleFiltersChange}
      ></cts-statistics-filters>
    `;
  }

  /**
   * The five charts and the unresolved-plans disclosure. The filter row above
   * scopes every one of them, so their numbers always agree.
   *
   * Everything the charts are handed comes out of one memo keyed on the
   * payload, the colour slots and the family filter, so a re-render that
   * changes none of those (the busy flag going on and off around a fetch)
   * hands `<cts-chart>` the very same arrays and it does not re-plot.
   * @param {StatisticsData} data - The current payload.
   * @returns {unknown} The charts.
   */
  _renderTrends(data) {
    const view = this._view(data, this._slots, this._state.family);
    const names = view.names;

    return html`
      <div
        class=${classMap({ "cts-stats-charts": true, "is-busy": this._busy })}
        data-testid="stats-charts"
        aria-busy=${aria(this._busy)}
      >
        <div class="cts-stats-chart" data-testid="stats-chart-runs">
          <cts-chart
            heading="Test module runs per ${names.unit}"
            category-label=${names.axis}
            stacked
            clickable
            click-label=${DRILL_DOWN_LABEL}
            .labels=${view.labels}
            .datasets=${view.runs.datasets}
            .tooltipFooter=${view.footers.runs}
            @cts-chart-click=${this._handleFamilyChartClick}
          ></cts-chart>
        </div>
        <div class="cts-stats-chart" data-testid="stats-chart-plans">
          <cts-chart
            heading="Test plans per ${names.unit}"
            category-label=${names.axis}
            stacked
            clickable
            click-label=${DRILL_DOWN_LABEL}
            .labels=${view.labels}
            .datasets=${view.plans.datasets}
            .tooltipFooter=${view.footers.plans}
            @cts-chart-click=${this._handleFamilyChartClick}
          ></cts-chart>
        </div>
        <div class="cts-stats-chart" data-testid="stats-chart-results">
          <cts-chart
            heading="Results per ${names.unit}"
            category-label=${names.axis}
            stacked
            clickable
            click-label=${DRILL_DOWN_LABEL}
            .labels=${view.labels}
            .datasets=${view.results.datasets}
            @cts-chart-click=${this._handleResultChartClick}
          ></cts-chart>
        </div>
        <div class="cts-stats-chart" data-testid="stats-chart-users">
          <cts-chart
            heading="Users per ${names.unit} — by plan owner"
            category-label=${names.axis}
            .labels=${view.labels}
            .datasets=${view.users.datasets}
          ></cts-chart>
        </div>
        <div class="cts-stats-chart" data-testid="stats-chart-certified">
          <cts-chart
            heading="Certified plans per ${names.unit}"
            category-label=${names.axis}
            stacked
            clickable
            click-label=${DRILL_DOWN_LABEL}
            .labels=${view.labels}
            .datasets=${view.certified.datasets}
            .tooltipFooter=${view.footers.certified}
            @cts-chart-click=${this._handleFamilyChartClick}
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
   * All-time and unfiltered, like the tiles: the server does not scope this
   * list by the query, so narrowing the range does not hide the diagnostic
   * the admin came for.
   * @param {StatisticsData} data - The current payload.
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

  /**
   * The phase-2 extras under the trends: the three distributions, the
   * activity heatmap and the external servers.
   *
   * They are scoped differently from each other and the component says so:
   * the distributions are counted under the whole query, so they are withheld
   * when nothing matches it (they would be three empty cards saying what the
   * no-match state already says); the heatmap is sliced by the range only and
   * the hosts not at all, so both stay on screen in every state that has a
   * payload — including the one where the filters match nothing, which is
   * exactly when an admin wants to know the database is not empty.
   * @param {StatisticsData} data - The current payload.
   * @param {boolean} noMatch - Whether the filters matched nothing.
   * @returns {unknown} The insights block.
   */
  _renderInsights(data, noMatch) {
    /** @type {Distributions|null} */
    const distributions = noMatch ? null : this._distributions(data.dimensions);
    return html`
      <cts-statistics-insights
        data-testid="stats-insights"
        range-label=${rangePreset(this._state.range).label}
        ?narrowed=${Boolean(this._state.family || this._state.plan)}
        ?busy=${this._busy}
        .distributions=${distributions}
        .heatmap=${data.heatmap}
        .hosts=${data.externalHosts}
      ></cts-statistics-insights>
    `;
  }
}

customElements.define("cts-statistics-page", CtsStatisticsPage);

export {};
