/**
 * Pure shaping helpers for the admin statistics page.
 *
 * Everything here is a plain function over the `data` object served by
 * `GET /api/statistics/overview`, or over the page's own filter state — no
 * DOM, no fetch, no Lit — so the page component (`cts-statistics-page.js`)
 * stays fetch/state/composition only and every shaping rule is unit-testable
 * in the vitest `unit` project (`statistics-model.test.js`).
 *
 * The payload contract (see `net.openid.conformance.statistics.StatisticsOverview`):
 * every per-period array has exactly `periods.length` entries and every
 * per-family map has an entry for every family in `families`, so these
 * helpers never have to fill gaps — the defensive guards below exist only
 * so a truncated or mocked payload degrades to an empty chart instead of
 * throwing inside `render()`.
 *
 * Slicing is the SERVER's job since phase 2: the range presets and the
 * filter selects are turned into request parameters here
 * ({@link queryFromState}) and the payload that comes back is already
 * narrowed. Nothing in this file re-slices a payload.
 */

// The drill-down link lands on the plans listing, so its parameters are that
// listing's vocabulary, not this page's — `plan-list-filter.js` owns the
// names, the variant prefix and the order they are written in, and parses
// them back on the other side. Pure module, no DOM, no cycle.
import {
  CERT_PARAM,
  DAY_MS,
  FAMILY_PARAM,
  PLAN_PARAM,
  SHORT_MONTHS,
  VARIANT_PREFIX,
  emptyFilter,
  text,
  toParams as planListParams,
  variantEntries,
  variantsFromParams,
} from "./plan-list-filter.js";

/**
 * The statistics payload. Every field is declared as present because the
 * server contract guarantees it; the runtime guards throughout this file are
 * defence against a truncated or mocked payload (a permissive test route
 * answering `{}`, an older server), and callers that deliberately pass a
 * partial object have to say so with a cast.
 * @typedef {object} StatisticsData
 * @property {Array<string>} families - Every family a series may be keyed by, fixed order.
 * @property {Array<string>} resultBuckets - Result buckets, fixed order.
 * @property {Array<string>} periods - Contiguous period keys, oldest first: `YYYY-MM`
 *   months or the `YYYY-MM-DD` Mondays of ISO weeks.
 * @property {string} granularity - Which of the two `periods` are: `month` or `week`.
 * @property {Record<string, Array<number>>} testRunsByFamily - Family → runs per period.
 * @property {Record<string, Array<number>>} plansByFamily - Family → plans per period.
 * @property {Record<string, Record<string, Array<number>>>} resultsByFamily - Family → bucket → runs per period.
 * @property {Record<string, Array<number>>} certifiedByFamily - Family → plans made immutable per period.
 * @property {{activeByPeriod: Array<number>, newByPeriod: Array<number>}} users - User counts per period.
 * @property {Record<string, number>} tiles - Whole-collection counters.
 * @property {StatisticsDimensions} dimensions - What the filter selects can offer.
 * @property {Array<object>} storage - Per-collection storage counters.
 * @property {Array<Array<number>>} heatmap - Runs by day of week × hour, UTC.
 * @property {Array<ModuleRow>} modules - Test modules over the trailing 24
 *   months, busiest first; narrowed by family and plan only.
 * @property {Array<object>} externalHosts - External servers tested against, all time.
 * @property {Array<{planName: string, runs: number}>} unresolvedPlans - Busiest unresolved plan names.
 */

/**
 * The values the filter selects can offer, counted under the query that
 * produced the payload.
 * @typedef {object} StatisticsDimensions
 * @property {Array<{planName: string, family: string, runs: number, plans: number}>} plans - Plans with data, busiest first.
 * @property {Record<string, Array<{value: string, users: number, plans: number}>>} variants - Variant parameter → its values.
 * @property {Array<{name: string, users: number, plans: number}>} certProfiles - Certification profiles with data.
 * @property {Array<{entity: string, runs: number}>} entities - What was under test.
 */

/**
 * Everything the page lets the user choose, and the whole of what it puts in
 * the URL. `variant` maps a plan-level variant parameter name to the single
 * value being filtered on (`{fapi_profile: "openbanking_brazil"}`).
 * @typedef {object} FilterState
 * @property {string} range - One of the {@link RANGE_PRESETS} values.
 * @property {string} family - Selected spec family, or `""`.
 * @property {string} plan - Selected plan name, or `""`.
 * @property {Record<string, string>} variant - Variant parameter → value.
 * @property {string} cert - Selected certification profile, or `""`.
 */

/**
 * The option lists the filter selects offer. Not simply the current
 * payload's `dimensions`: see {@link rememberOptions}.
 * @typedef {object} FilterOptions
 * @property {Array<{planName: string, family: string, runs: number, plans: number}>} plans - Plan options.
 * @property {Record<string, Array<{value: string, users: number, plans: number}>>} variants - Variant options per parameter.
 * @property {Array<{name: string, users: number, plans: number}>} certProfiles - Certification profile options.
 */

/**
 * One Chart.js series in `<cts-chart>`'s shape.
 * @typedef {object} ChartDataset
 * @property {string} label - Series name (legend key, tooltip row).
 * @property {Array<number>} data - One value per period in the payload.
 * @property {string} colorVar - CSS custom-property name, leading `--` included.
 */

/**
 * The range presets, in display order within their group. `periods` is how
 * many periods back the range reaches, counting the current one; `0` means
 * "no lower bound at all".
 *
 * Weekly is capped at 52 because the server only keeps 104 weekly cells and
 * an axis of more than a year of weeks stops being readable anyway.
 * @type {Array<{value: string, label: string, granularity: string, periods: number, group: string}>}
 */
export const RANGE_PRESETS = [
  { value: "12w", label: "12 weeks", granularity: "week", periods: 12, group: "weekly" },
  { value: "26w", label: "26 weeks", granularity: "week", periods: 26, group: "weekly" },
  { value: "52w", label: "52 weeks", granularity: "week", periods: 52, group: "weekly" },
  { value: "12m", label: "12 months", granularity: "month", periods: 12, group: "monthly" },
  { value: "24m", label: "24 months", granularity: "month", periods: 24, group: "monthly" },
  { value: "all", label: "All time", granularity: "month", periods: 0, group: "monthly" },
];

/**
 * "12 months" is the default because the question this page answers day to
 * day is recent usage; every other preset is one click away.
 */
export const DEFAULT_RANGE = "12m";

/** @type {Record<string, {value: string, label: string, granularity: string, periods: number, group: string}>} */
const RANGE_BY_VALUE = Object.fromEntries(RANGE_PRESETS.map((preset) => [preset.value, preset]));

/**
 * The one request/URL parameter this page owns that the plans listing does
 * not; the filter parameters (`family`, `plan`, `variant.*`, `cert`) are the
 * listing's own, imported so the drill-down link and this page cannot
 * disagree about them. Anything else in the page's query string is left
 * alone by {@link urlFromState}, so a deep link that also carries someone
 * else's parameter survives a filter change.
 */
const RANGE_PARAM = "range";

/**
 * The seven categorical slots, in rank order: the busiest family all-time
 * gets slot 1. Fixed length by design — past seven hues a categorical
 * palette stops being separable, which is what `OTHER_COLOR_VAR` is for.
 * @type {Array<string>}
 */
export const CATEGORY_COLOR_VARS = [
  "--chart-cat-1",
  "--chart-cat-2",
  "--chart-cat-3",
  "--chart-cat-4",
  "--chart-cat-5",
  "--chart-cat-6",
  "--chart-cat-7",
];

/** The deliberate neutral every non-top-seven family shares. */
export const OTHER_COLOR_VAR = "--chart-other";

/**
 * The two synthetic buckets the server emits alongside the real spec
 * families (`SpecFamilyResolver.NO_PLAN` / `OTHER_RETIRED`). They are part of
 * the API contract, so the names are pinned here rather than sniffed.
 */
export const NO_PLAN_FAMILY = "No plan";
export const OTHER_RETIRED_FAMILY = "Other / retired";

/**
 * Buckets that are excluded from the categorical ranking entirely: they are
 * "this run had no family", not a family, so they must never spend one of the
 * seven separable hues. They always wear {@link OTHER_COLOR_VAR}.
 * @type {Set<string>}
 */
const SYNTHETIC_FAMILIES = new Set([NO_PLAN_FAMILY, OTHER_RETIRED_FAMILY]);

/** Legend/tooltip label for the folded neutral series. */
export const OTHER_LABEL = "Other";

/**
 * Result bucket → status token. These are the suite's reserved outcome
 * colours (the same ones `cts-badge` and `cts-plan-status` paint), so the
 * result chart reads the same way as every status pill on every other page.
 * `NEVER_FINISHED` is not an outcome, so it takes a neutral ink instead.
 * @type {Record<string, string>}
 */
export const RESULT_COLOR_VARS = {
  PASSED: "--status-pass",
  WARNING: "--status-warning",
  REVIEW: "--status-review",
  FAILED: "--status-fail",
  SKIPPED: "--status-skipped",
  NEVER_FINISHED: "--ink-300",
};

/**
 * Which per-family map `otherBreakdown` reads. An explicit lookup rather
 * than a caller-supplied key so a typo cannot reach into the payload.
 * @type {Record<string, string>}
 */
const BREAKDOWN_SOURCES = {
  runs: "testRunsByFamily",
  plans: "plansByFamily",
  certified: "certifiedByFamily",
};

/**
 * Sum a per-period series, tolerating a missing or non-array one.
 * @param {Array<number>|undefined} list - The series.
 * @returns {number} Total, or 0.
 */
function total(list) {
  if (!Array.isArray(list)) return 0;
  let sum = 0;
  for (const value of list) sum += Number(value) || 0;
  return sum;
}

/**
 * Coerce a value that should be an array into one. A malformed payload must
 * degrade to an empty chart, never throw inside `render()`.
 * @template T
 * @param {Array<T>|undefined} value - The candidate.
 * @returns {Array<T>} The array, or `[]`.
 */
function list(value) {
  return Array.isArray(value) ? value : [];
}

// --- Range presets, URL and request query ------------------------------

/**
 * @param {string} range - A preset value.
 * @returns {{value: string, label: string, granularity: string, periods: number, group: string}}
 *   The preset, falling back to the default for an unknown one — a stale
 *   bookmark must show the dashboard, not an error.
 */
export function rangePreset(range) {
  return RANGE_BY_VALUE[range] || RANGE_BY_VALUE[DEFAULT_RANGE];
}

/**
 * @param {Date} date - Any instant.
 * @returns {string} Its UTC date as `YYYY-MM-DD`.
 */
function isoDay(date) {
  return date.toISOString().slice(0, 10);
}

/**
 * The Monday that starts the ISO week `date` falls in, in UTC — the period
 * key format the server uses for weekly cells. Sunday belongs to the week
 * that started six days earlier, which is the one case a naive
 * `getUTCDay() - 1` gets wrong.
 * @param {Date} date - Any instant.
 * @returns {Date} Midnight UTC on that Monday.
 */
function utcMonday(date) {
  const midnight = Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate());
  const sinceMonday = (new Date(midnight).getUTCDay() + 6) % 7;
  return new Date(midnight - sinceMonday * DAY_MS);
}

/**
 * Turn a range preset into the granularity and lower bound the server takes.
 *
 * `to` is deliberately never sent: the server's axis already ends at the
 * period containing today, so an upper bound would only be a second way of
 * saying the same thing — and a wrong one the moment a page left open
 * crosses midnight.
 * @param {string} range - One of the {@link RANGE_PRESETS} values.
 * @param {Date} [now] - The clock, injectable for tests. UTC is read from it.
 * @returns {{granularity: string, from: string}} `from` is `""` for "All time".
 */
export function rangeToQuery(range, now = new Date()) {
  const preset = rangePreset(range);
  if (preset.periods <= 0) return { granularity: preset.granularity, from: "" };

  if (preset.granularity === "week") {
    const monday = utcMonday(now);
    return {
      granularity: "week",
      from: isoDay(new Date(monday.getTime() - (preset.periods - 1) * 7 * DAY_MS)),
    };
  }
  // Date.UTC normalises a negative or out-of-range month, so this crosses
  // year boundaries without any arithmetic of its own.
  const from = new Date(
    Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - (preset.periods - 1), 1),
  );
  return { granularity: "month", from: from.toISOString().slice(0, 7) };
}

/** @returns {FilterState} The state a page with no query string starts in. */
export function defaultFilterState() {
  return { range: DEFAULT_RANGE, family: "", plan: "", variant: {}, cert: "" };
}

/**
 * @param {FilterState} state - The current state.
 * @returns {boolean} True when anything other than the range is set — which
 *   is exactly when "Clear filters" has something to do.
 */
export function isFiltered(state) {
  if (!state) return false;
  return Boolean(
    state.family || state.plan || state.cert || Object.keys(state.variant || {}).length > 0,
  );
}

/**
 * Whether the view is showing less than everything there is - which is what
 * decides whether an empty result means "nothing here" or "nothing anywhere",
 * and whether the unfiltered baseline has to be fetched separately at all.
 * Derived from the preset table, so a new "everything" preset needs no edit
 * here.
 * @param {FilterState} state - The current state.
 * @returns {boolean} True unless the range covers the whole history and
 *   nothing is filtered.
 */
export function isNarrowed(state) {
  return rangePreset(state && state.range).periods > 0 || isFiltered(state);
}

/**
 * @param {FilterState} a - One state.
 * @param {FilterState} b - Another.
 * @returns {boolean} True when they would produce the same request, so the
 *   page can ignore a change event that changed nothing.
 */
export function sameState(a, b) {
  if (!a || !b) return a === b;
  if (a.range !== b.range || a.family !== b.family || a.plan !== b.plan || a.cert !== b.cert) {
    return false;
  }
  const left = a.variant || {};
  const right = b.variant || {};
  const keys = Object.keys(left);
  if (keys.length !== Object.keys(right).length) return false;
  return keys.every((key) => left[key] === right[key]);
}

/**
 * Write the filter half of a state — everything but the range — into a query,
 * the same way for the request and for the page URL.
 * @param {URLSearchParams} params - The query to write into.
 * @param {FilterState} state - The current state.
 * @returns {void}
 */
function writeFilters(params, state) {
  if (text(state && state.family)) params.set(FAMILY_PARAM, text(state.family));
  if (text(state && state.plan)) params.set(PLAN_PARAM, text(state.plan));
  for (const [name, value] of variantEntries(state)) {
    params.set(VARIANT_PREFIX + text(name), text(value));
  }
  if (text(state && state.cert)) params.set(CERT_PARAM, text(state.cert));
}

/**
 * The request parameters for `GET /api/statistics/overview`. `refresh` is
 * NOT included: forcing a recompute is a property of one request, not of the
 * view, so the caller appends it.
 * @param {FilterState} state - The current state.
 * @param {Date} [now] - The clock, injectable for tests.
 * @returns {URLSearchParams} The query.
 */
export function queryFromState(state, now = new Date()) {
  const params = new URLSearchParams();
  const { granularity, from } = rangeToQuery(state && state.range, now);
  params.set("granularity", granularity);
  if (from) params.set("from", from);
  writeFilters(params, state);
  return params;
}

/**
 * Read the page's own state out of its query string. Unknown parameters and
 * an unknown range are ignored rather than rejected: a link that outlives a
 * preset must still open the dashboard.
 * @param {string} [search] - `location.search`, with or without the leading `?`.
 * @returns {FilterState} The state it describes.
 */
export function stateFromUrl(search) {
  const params = new URLSearchParams(search || "");
  const requested = text(params.get(RANGE_PARAM));
  return {
    range: RANGE_BY_VALUE[requested] ? requested : DEFAULT_RANGE,
    family: text(params.get(FAMILY_PARAM)),
    plan: text(params.get(PLAN_PARAM)),
    variant: variantsFromParams(params),
    cert: text(params.get(CERT_PARAM)),
  };
}

/**
 * The query string the page should be at, given its state. Parameters this
 * page does not own are carried over from `search` untouched, and its own
 * are rewritten from scratch — so clearing a filter really removes it from
 * the URL instead of leaving an empty one behind.
 * @param {FilterState} state - The current state.
 * @param {string} [search] - The current `location.search`.
 * @returns {string} A query string starting with `?` (never empty: the range
 *   is always named, so a copied link opens the view that was shared).
 */
export function urlFromState(state, search = "") {
  const params = new URLSearchParams(search || "");
  for (const key of [...params.keys()]) {
    if (key === RANGE_PARAM || key === FAMILY_PARAM || key === PLAN_PARAM || key === CERT_PARAM) {
      params.delete(key);
    } else if (key.startsWith(VARIANT_PREFIX)) {
      params.delete(key);
    }
  }
  params.set(RANGE_PARAM, rangePreset(state && state.range).value);
  writeFilters(params, state);
  return `?${params.toString()}`;
}

/**
 * The axis labels for a payload's periods.
 *
 * Months are shown as the server's own `YYYY-MM` key. Weeks would be
 * unreadable that way — 52 ISO dates on one axis — so they become the day
 * and month of the Monday they start on, carrying the year only on the
 * first label and wherever the year changes, which is the usual convention
 * for a dated axis and keeps a shared screenshot unambiguous.
 * @param {Array<string>|undefined} periods - The payload's `periods`.
 * @param {string} granularity - The payload's `granularity`.
 * @returns {Array<string>} One label per period.
 */
export function periodLabels(periods, granularity) {
  const keys = list(periods);
  if (granularity !== "week") return keys.map((period) => String(period));

  let shownYear = "";
  return keys.map((period) => {
    const parts = String(period).split("-");
    const month = SHORT_MONTHS[Number(parts[1]) - 1];
    if (parts.length !== 3 || !month) return String(period);
    const label = `${Number(parts[2])} ${month}`;
    if (parts[0] === shownYear) return label;
    shownYear = parts[0];
    return `${label} ${parts[0]}`;
  });
}

/**
 * The half-open date range one period covers, as the `from`/`to` parameters
 * of `GET /api/plan` take them (`from` inclusive, `to` exclusive) — the
 * drill-down from a chart bar to the plans that made it.
 * @param {string} period - A period key from the payload.
 * @param {string} granularity - The payload's `granularity`.
 * @returns {{from: string, to: string}|null} `YYYY-MM-DD` bounds, or null if
 *   the key is not one of that granularity.
 */
export function periodBounds(period, granularity) {
  const key = text(period);
  if (granularity === "week") {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(key)) return null;
    const start = new Date(`${key}T00:00:00Z`);
    if (Number.isNaN(start.getTime())) return null;
    return { from: isoDay(start), to: isoDay(new Date(start.getTime() + 7 * DAY_MS)) };
  }
  if (!/^\d{4}-\d{2}$/.test(key)) return null;
  const year = Number(key.slice(0, 4));
  const month = Number(key.slice(5, 7));
  if (month < 1 || month > 12) return null;
  return {
    from: isoDay(new Date(Date.UTC(year, month - 1, 1))),
    to: isoDay(new Date(Date.UTC(year, month, 1))),
  };
}

// --- Drill-down --------------------------------------------------------

/** The listing the drill-down lands on. Both pages sit at the web root. */
const PLANS_PAGE = "plans.html";

/**
 * The separator `StatisticsCube` joins a plan's certification profiles with
 * when it builds a `certKey`. `GET /api/plan?cert=` matches ONE profile
 * exactly, so a joined key cannot be forwarded as it stands.
 */
const CERT_JOIN = " | ";

/**
 * What a click on a chart bar identifies, over and above the page's own
 * filter state.
 * @typedef {object} DrillDownClick
 * @property {number} periodIndex - Position in the payload's `periods`, from
 *   `cts-chart-click`. Out of range (or `-1` for the keyboard route on a chart
 *   with no period) simply drops the date bounds.
 * @property {string} [family] - The family the clicked dataset stands for, for
 *   the charts whose datasets ARE families (runs, plans, certified). Empty for
 *   the results chart (its datasets are result buckets, which the plans
 *   listing cannot filter on) and for the keyboard row route.
 */

/**
 * The plans-listing URL a chart click drills into: the page's current filters,
 * narrowed to the clicked family and the clicked period.
 *
 * Returns `null` when the click cannot name a listing — the folded "Other"
 * series and the two synthetic buckets stand for plans that are not in the
 * registry (or, for "No plan", for runs with no plan at all), and
 * `GET /api/plan?family=` has no way to express either, so it would answer
 * with an empty list. The caller says so with a toast instead of navigating.
 * @param {FilterState} state - The page's filter state.
 * @param {DrillDownClick} click - What was clicked.
 * @param {StatisticsData} data - The payload the chart was built from (its
 *   `periods` and `granularity` turn `periodIndex` into date bounds).
 * @returns {string|null} A relative `plans.html?…` URL, or `null` when the
 *   clicked bucket has no plans to list.
 */
export function drillDownUrl(state, click, data) {
  const family = text(state && state.family) || text(click && click.family);
  if (family === OTHER_LABEL || SYNTHETIC_FAMILIES.has(family)) return null;

  const periods = list(data && data.periods);
  const index = Number(click && click.periodIndex);
  const bounds = Number.isInteger(index)
    ? periodBounds(periods[index], (data && data.granularity) || "month")
    : null;
  const cert = text(state && state.cert);

  // Built as the listing's own filter object and serialised by the listing's
  // own `toParams`, so the two ends of this link cannot drift: the parameter
  // names, the variant prefix and the order the variants come out in have
  // exactly one definition, in `plan-list-filter.js`, and `plans.html` parses
  // back what this produced.
  const query = planListParams({
    ...emptyFilter(),
    family,
    plan: text(state && state.plan),
    variant: (state && state.variant) || {},
    // The cube joins every certification profile of a plan into one key, while
    // the listing matches a single element of `certificationProfileName`.
    // Sending the first element is therefore a SUPERSET of the statistics
    // slice: the listing also shows plans certified for that profile alongside
    // others. Sending the joined key would match nothing at all.
    cert: cert ? cert.split(CERT_JOIN)[0] : "",
    from: bounds ? bounds.from : "",
    to: bounds ? bounds.to : "",
  }).toString();
  return query ? `${PLANS_PAGE}?${query}` : PLANS_PAGE;
}

/**
 * The bucket a drill-down click resolved to, for the message shown when
 * {@link drillDownUrl} declines it.
 * @param {FilterState} state - The page's filter state.
 * @param {DrillDownClick} click - What was clicked.
 * @returns {string} The family name, or `""` when the click named none.
 */
export function drillDownFamily(state, click) {
  return text(state && state.family) || text(click && click.family);
}

// --- Filter options ----------------------------------------------------

/** @type {FilterOptions} The option lists of a page that has no payload yet. */
export const EMPTY_OPTIONS = { plans: [], variants: {}, certProfiles: [] };

/**
 * What the selects should offer, given the payload that just arrived and the
 * filter it was fetched under.
 *
 * The server counts `dimensions` under the WHOLE query, its own dimension
 * included — so once a plan is selected, `dimensions.plans` contains that one
 * plan and nothing else. Rendering that straight into the select would make
 * every choice a dead end (you could clear it, but never switch to a sibling),
 * so the last list seen while that dimension was unfiltered is kept and
 * offered instead. Every other dimension still narrows, which is the point of
 * the cascade.
 * @param {FilterOptions} previous - The options currently on screen.
 * @param {StatisticsData} data - The payload that just arrived.
 * @param {FilterState} state - The state it was fetched with.
 * @returns {FilterOptions} The options to render.
 */
export function rememberOptions(previous, data, state) {
  const kept = previous || EMPTY_OPTIONS;
  const dimensions = (data && data.dimensions) || {};
  const filter = (state && state.variant) || {};

  const fresh = /** @type {Record<string, Array<any>>} */ (dimensions.variants || {});
  /** @type {Record<string, Array<any>>} */
  const variants = {};
  // Every parameter the payload knows about, plus any that is being filtered
  // on — a filter the user cannot see is a filter they cannot clear.
  for (const name of new Set([...Object.keys(fresh), ...Object.keys(filter)])) {
    variants[name] = keepWhenFiltered(
      kept.variants && kept.variants[name],
      fresh[name],
      filter[name],
    );
  }

  return {
    plans: keepWhenFiltered(kept.plans, dimensions.plans, state && state.plan),
    variants,
    certProfiles: keepWhenFiltered(kept.certProfiles, dimensions.certProfiles, state && state.cert),
  };
}

/**
 * Which variant parameters are worth putting a select on the screen for.
 *
 * The suite publishes dozens of plan-level variant parameters, and an
 * unfiltered snapshot mentions nearly all of them — a filter row of 38
 * selects is not a filter row. So they appear only once the view has been
 * narrowed to a family or a plan, which is where a variant parameter means
 * something anyway, and a parameter whose values are all the same is left out
 * because there is nothing to choose between. A parameter that is currently
 * being filtered on is ALWAYS offered: the select is the only way to clear it.
 * @param {FilterOptions} options - From {@link rememberOptions}.
 * @param {FilterState} state - The current filter state.
 * @returns {Record<string, Array<{value: string, users: number, plans: number}>>}
 *   Parameter → values, in the order the payload listed them.
 */
export function visibleVariants(options, state) {
  const variants = (options && options.variants) || {};
  const filter = (state && state.variant) || {};
  const narrowed = Boolean(text(state && state.family) || text(state && state.plan));

  /** @type {Record<string, Array<any>>} */
  const visible = {};
  for (const name of Object.keys(variants)) {
    const values = list(variants[name]);
    if (text(filter[name])) {
      visible[name] = values;
    } else if (narrowed && values.length > 1) {
      visible[name] = values;
    }
  }
  return visible;
}

/**
 * @template T
 * @param {Array<T>} remembered - What was offered before this filter was set.
 * @param {Array<T>|undefined} offered - What the payload offers now.
 * @param {string|undefined} selected - The filter on this dimension, if any.
 * @returns {Array<T>} The list to render.
 */
function keepWhenFiltered(remembered, offered, selected) {
  const kept = list(remembered);
  // A page opened straight onto a filtered link has nothing remembered, so
  // it falls back to the payload's one-entry list rather than an empty select.
  if (text(selected) && kept.length > 0) return kept;
  return list(offered);
}

// --- Colour slots and datasets -----------------------------------------

/**
 * Map every family to the colour it wears everywhere on the page.
 *
 * Real spec families are ranked by their **all-time** run totals (descending,
 * ties broken by the payload's `families` order); the top seven take
 * `--chart-cat-1..7` in rank order and the tail shares `--chart-other`. The
 * two synthetic buckets ({@link SYNTHETIC_FAMILIES}) never enter the ranking
 * at all — however busy "No plan" is, it is not a family and must not spend a
 * hue that a real one needs.
 *
 * Callers must pass the UNFILTERED, all-time payload, never the one on
 * screen: a family's colour is an identity, so changing the range or any
 * filter must never repaint the charts — and under a filter every other
 * family is zero, which would hand slot 1 to whatever was selected.
 * `cts-statistics-page.js` fetches that baseline once on load and keeps it.
 * @param {StatisticsData} data - The unfiltered, all-time payload.
 * @returns {Record<string, string>} Family → CSS custom-property name.
 */
export function assignFamilySlots(data) {
  const families = list(data && data.families);
  const runs = (data && data.testRunsByFamily) || {};

  /** @type {Record<string, string>} */
  const slots = {};
  for (const family of families) slots[family] = OTHER_COLOR_VAR;

  families
    .map((family, index) => ({ family, index, runs: total(runs[family]) }))
    .filter((entry) => !SYNTHETIC_FAMILIES.has(entry.family))
    .sort((a, b) => b.runs - a.runs || a.index - b.index)
    .slice(0, CATEGORY_COLOR_VARS.length)
    .forEach((entry, rank) => {
      slots[entry.family] = CATEGORY_COLOR_VARS[rank];
    });

  return slots;
}

/**
 * Families in stack order: categorical slots first (which, because slots
 * are handed out by all-time rank, puts the biggest series at the bottom of
 * a stacked bar), then the neutral tail in the payload's own order.
 * @param {StatisticsData} data - The payload being charted.
 * @param {Record<string, string>} slots - From {@link assignFamilySlots}.
 * @returns {Array<string>} Family names.
 */
function orderedFamilies(data, slots) {
  const families = list(data && data.families);
  const rank = (/** @type {string} */ family) => {
    const index = CATEGORY_COLOR_VARS.indexOf((slots && slots[family]) || OTHER_COLOR_VAR);
    return index === -1 ? CATEGORY_COLOR_VARS.length : index;
  };
  return families
    .map((family, index) => ({ family, index }))
    .sort((a, b) => rank(a.family) - rank(b.family) || a.index - b.index)
    .map((entry) => entry.family);
}

/**
 * Shared body of {@link runsDatasets}, {@link plansDatasets} and
 * {@link certifiedDatasets}.
 * @param {StatisticsData} data - The payload.
 * @param {Record<string, string>} slots - From {@link assignFamilySlots}.
 * @param {string} family - Selected family, or `""` for all.
 * @param {"testRunsByFamily"|"plansByFamily"|"certifiedByFamily"} key - Which map to read.
 * @returns {Array<ChartDataset>} One dataset per rendered family.
 */
function familyDatasets(data, slots, family, key) {
  const byFamily = (data && data[key]) || {};
  const colorOf = (/** @type {string} */ name) => (slots && slots[name]) || OTHER_COLOR_VAR;

  if (family) {
    const series = byFamily[family];
    if (!Array.isArray(series)) return [];
    return [{ label: family, data: [...series], colorVar: colorOf(family) }];
  }

  // A family with nothing in the current payload contributes an invisible
  // segment and a dead legend key, so it is dropped rather than rendered.
  return orderedFamilies(data, slots)
    .filter((name) => total(byFamily[name]) > 0)
    .map((name) => ({ label: name, data: [...byFamily[name]], colorVar: colorOf(name) }));
}

/**
 * Test module runs per period, one dataset per family.
 * @param {StatisticsData} data - The payload.
 * @param {Record<string, string>} slots - From {@link assignFamilySlots}.
 * @param {string} family - Selected family, or `""` for all.
 * @returns {Array<ChartDataset>} Datasets for `<cts-chart>`.
 */
export function runsDatasets(data, slots, family) {
  return familyDatasets(data, slots, family, "testRunsByFamily");
}

/**
 * Test plans created per period, one dataset per family, coloured with the
 * same slots as {@link runsDatasets} so a family reads identically on both
 * charts.
 * @param {StatisticsData} data - The payload.
 * @param {Record<string, string>} slots - From {@link assignFamilySlots}.
 * @param {string} family - Selected family, or `""` for all.
 * @returns {Array<ChartDataset>} Datasets for `<cts-chart>`.
 */
export function plansDatasets(data, slots, family) {
  return familyDatasets(data, slots, family, "plansByFamily");
}

/**
 * Plans made immutable per period — certification activity — one dataset per
 * family, in the same colours as the other family charts.
 * @param {StatisticsData} data - The payload.
 * @param {Record<string, string>} slots - From {@link assignFamilySlots}.
 * @param {string} family - Selected family, or `""` for all.
 * @returns {Array<ChartDataset>} Datasets for `<cts-chart>`.
 */
export function certifiedDatasets(data, slots, family) {
  return familyDatasets(data, slots, family, "certifiedByFamily");
}

/**
 * Result mix per period: one dataset per result bucket, in the payload's
 * bucket order, wearing the suite's reserved status colours.
 *
 * With no family selected the buckets are summed across every family in
 * `families`; with one selected only that family's buckets are read.
 * Bucket labels are the server's own strings (`PASSED`, `NEVER_FINISHED`,
 * …) — the same vocabulary the rest of the suite shows.
 * @param {StatisticsData} data - The payload.
 * @param {string} family - Selected family, or `""` for all.
 * @returns {Array<ChartDataset>} Datasets for `<cts-chart>`.
 */
export function resultsDatasets(data, family) {
  const buckets = list(data && data.resultBuckets);
  const byFamily = (data && data.resultsByFamily) || {};
  const periods = list(data && data.periods).length;
  const keys = family ? [family] : list(data && data.families);

  return buckets
    .map((bucket) => {
      const summed = new Array(periods).fill(0);
      for (const key of keys) {
        const series = byFamily[key] && byFamily[key][bucket];
        if (!Array.isArray(series)) continue;
        for (let i = 0; i < periods; i++) summed[i] += Number(series[i]) || 0;
      }
      return {
        label: bucket,
        data: summed,
        colorVar: RESULT_COLOR_VARS[bucket] || OTHER_COLOR_VAR,
      };
    })
    .filter((dataset) => total(dataset.data) > 0);
}

/**
 * Active and first-seen users per period. Always two series — "New" is
 * meaningful at zero, and a disappearing legend key on a two-series chart
 * reads as a bug.
 *
 * These are counted on a plan basis (a user is active in the period they
 * created a plan), which is what lets the server filter them like every
 * other series; the page says so in the chart heading.
 * @param {StatisticsData} data - The payload.
 * @returns {Array<ChartDataset>} Datasets for `<cts-chart>`.
 */
export function usersDatasets(data) {
  const users = (data && data.users) || {};
  const active = Array.isArray(users.activeByPeriod) ? users.activeByPeriod : [];
  const fresh = Array.isArray(users.newByPeriod) ? users.newByPeriod : [];
  return [
    { label: "Active", data: [...active], colorVar: CATEGORY_COLOR_VARS[0] },
    { label: "New", data: [...fresh], colorVar: CATEGORY_COLOR_VARS[1] },
  ];
}

/**
 * Collapse every neutral-slot dataset into a single trailing "Other"
 * series, so a stacked bar never carries more than eight separable fills.
 * A lone neutral dataset is folded too, so the legend key is always
 * "Other" and the tooltip footer from {@link otherBreakdown} always names
 * what is inside it.
 *
 * Returns the input array unchanged when there is nothing in the neutral
 * slot. Callers must NOT fold when a single family is selected: that family's
 * one dataset may itself be a neutral-slot family, and renaming it "Other"
 * would drop the name the user just picked from the legend, tooltip and table.
 * @param {Array<ChartDataset>} datasets - Datasets to fold.
 * @returns {Array<ChartDataset>} Categorical datasets, then "Other".
 */
export function foldOther(datasets) {
  const all = list(datasets);
  const neutral = all.filter((dataset) => dataset && dataset.colorVar === OTHER_COLOR_VAR);
  if (neutral.length === 0) return all;

  const length = neutral.reduce(
    (max, dataset) => Math.max(max, Array.isArray(dataset.data) ? dataset.data.length : 0),
    0,
  );
  const summed = new Array(length).fill(0);
  for (const dataset of neutral) {
    for (let i = 0; i < length; i++) summed[i] += Number(dataset.data && dataset.data[i]) || 0;
  }

  return [
    ...all.filter((dataset) => dataset && dataset.colorVar !== OTHER_COLOR_VAR),
    { label: OTHER_LABEL, data: summed, colorVar: OTHER_COLOR_VAR },
  ];
}

/**
 * Tooltip footer lines naming what the folded "Other" segment contains for
 * one period, biggest first (`"OpenID Federation: 12"`). Families that are
 * zero in that period are omitted — a footer line reading `": 0"` is noise.
 * @param {StatisticsData} data - The payload the chart was built from.
 * @param {Record<string, string>} slots - From {@link assignFamilySlots}.
 * @param {number} periodIndex - Hovered position in `periods`.
 * @param {string} [source] - `"runs"` (default), `"plans"` or `"certified"`.
 * @returns {Array<string>} `"<family>: <count>"` lines.
 */
export function otherBreakdown(data, slots, periodIndex, source = "runs") {
  const key = BREAKDOWN_SOURCES[source] || BREAKDOWN_SOURCES.runs;
  const byFamily = (data && data[key]) || {};
  const families = list(data && data.families);

  return families
    .map((family, index) => ({
      family,
      index,
      count: Number((byFamily[family] || [])[periodIndex]) || 0,
    }))
    .filter((entry) => slots && slots[entry.family] === OTHER_COLOR_VAR && entry.count > 0)
    .sort((a, b) => b.count - a.count || a.index - b.index)
    .map((entry) => `${entry.family}: ${entry.count}`);
}

/**
 * Wrap a pure function in a one-entry memo keyed on the IDENTITY of its
 * arguments.
 *
 * This exists for one reason: `<cts-chart>` pushes new data into Chart.js
 * whenever its `labels` or `datasets` property changes, and Lit compares
 * those by identity. Rebuilding the same arrays on every render - and the
 * page renders three times per fetch, for the busy flag alone - would replay
 * the whole plot each time. Handing back the same instances makes those
 * renders free.
 * @template {(...args: Array<any>) => any} F
 * @param {F} compute - The function to memoise. Must be pure.
 * @returns {F} The memoised function.
 */
export function memoiseByArgs(compute) {
  /** @type {{args: Array<any>, value: any}|null} */
  let last = null;
  const memoised = (/** @type {Array<any>} */ ...args) => {
    if (last && last.args.length === args.length && last.args.every((arg, i) => arg === args[i])) {
      return last.value;
    }
    last = { args, value: compute(...args) };
    return last.value;
  };
  return /** @type {F} */ (/** @type {unknown} */ (memoised));
}

/**
 * One chart's series, and whether its neutral tail was folded into "Other"
 * (which is what decides whether a tooltip footer naming the tail makes
 * sense).
 * @typedef {object} ChartSeries
 * @property {Array<ChartDataset>} datasets - The datasets to render.
 * @property {boolean} folded - True when an "Other" series was produced.
 */

/**
 * Fold the neutral tail into one "Other" series — but only when there is more
 * than one series to begin with. A single series is whatever the user
 * filtered down to (a family, or the one family a plan or variant belongs
 * to), and it may itself be a neutral-slot family; renaming it "Other" would
 * drop the name they just picked from the legend, the tooltip and the data
 * table.
 * @param {Array<ChartDataset>} datasets - One chart's datasets.
 * @returns {ChartSeries} What to render.
 */
function foldSeries(datasets) {
  if (datasets.length <= 1) return { datasets, folded: false };
  return { datasets: foldOther(datasets), folded: true };
}

/**
 * Everything the five charts render, built once per payload.
 *
 * Pure, and meant to be called through {@link memoiseByArgs}: the arrays it
 * returns are handed straight to `<cts-chart>`, which re-plots whenever their
 * identity changes.
 * @param {StatisticsData} data - The payload on screen.
 * @param {Record<string, string>} slots - From {@link assignFamilySlots}.
 * @param {string} family - Selected family, or `""` for all.
 * @returns {{granularity: string, labels: Array<string>, runs: ChartSeries,
 *   plans: ChartSeries, certified: ChartSeries, results: ChartSeries,
 *   users: ChartSeries}} The chart inputs.
 */
export function buildChartInputs(data, slots, family) {
  const granularity = data && data.granularity === "week" ? "week" : "month";
  return {
    granularity,
    labels: periodLabels(data && data.periods, granularity),
    runs: foldSeries(runsDatasets(data, slots, family)),
    plans: foldSeries(plansDatasets(data, slots, family)),
    certified: foldSeries(certifiedDatasets(data, slots, family)),
    results: { datasets: resultsDatasets(data, family), folded: false },
    users: { datasets: usersDatasets(data), folded: false },
  };
}

/**
 * Whether a payload has anything in it at all: any run, any plan, in any
 * family, in any period. A filter that matches nothing still comes back with
 * the full axis and a zero for every cell — the periods are the cube's, not
 * the filter's — so this is the only way to tell "nothing matched" from
 * "here is your data".
 * @param {StatisticsData} data - The payload on screen.
 * @returns {boolean} True when something, anywhere in it, is non-zero.
 */
export function hasAnyData(data) {
  for (const key of ["testRunsByFamily", "plansByFamily"]) {
    const byFamily = (data && data[key]) || {};
    for (const family of Object.keys(byFamily)) {
      if (total(byFamily[family]) > 0) return true;
    }
  }
  return false;
}

/**
 * Families worth offering in the filter select: those with at least one run,
 * plan or certified plan ever — a family with plans but zero runs (e.g. every
 * plan still in progress) is still something a user might filter to. Call
 * with the same unfiltered, all-time baseline {@link assignFamilySlots} gets
 * — options that vanish when the user narrows the range or picks a plan
 * would make the control feel broken.
 * @param {StatisticsData} data - The unfiltered, all-time payload.
 * @returns {Array<string>} Family names in the payload's order.
 */
export function familiesWithActivity(data) {
  const families = list(data && data.families);
  const runs = (data && data.testRunsByFamily) || {};
  const plans = (data && data.plansByFamily) || {};
  const certified = (data && data.certifiedByFamily) || {};
  return families.filter(
    (family) => total(runs[family]) > 0 || total(plans[family]) > 0 || total(certified[family]) > 0,
  );
}

// --- Distributions, storage and the activity heatmap -------------------

/**
 * How many bars a distribution chart plots. Past a dozen horizontal bars the
 * shortest ones stop being comparable and the card grows taller than the
 * charts beside it. The tail is not lost: `<cts-chart>` says how many rows it
 * left out and its data table carries every one of them.
 */
export const DISTRIBUTION_LIMIT = 12;

/**
 * One distribution: a single-series horizontal bar chart's inputs.
 *
 * `labels`, `datasets[0].data` and every `extras[].data` are the WHOLE ranked
 * list — `<cts-chart max-bars>` plots the leading few and keeps the data table
 * complete, so capping the bars hides nothing.
 * @typedef {object} Distribution
 * @property {Array<string>} labels - Category labels, biggest first.
 * @property {Array<ChartDataset>} datasets - Exactly one series, in `--chart-cat-1`.
 * @property {Array<{label: string, data: Array<number>}>} extras - Table-only columns.
 */

/**
 * Which fields of a dimension row to read.
 * @typedef {object} DistributionSpec
 * @property {string} label - Field carrying the category name.
 * @property {string} value - Field carrying the plotted measure.
 * @property {string} valueLabel - What that measure is called ("Users", "Runs").
 * @property {string} [extra] - Field carrying a second, table-only measure.
 * @property {string} [extraLabel] - What THAT is called ("Plans").
 */

/**
 * Split a ranked list into the head that gets plotted and the tail that does
 * not.
 *
 * The list is sorted here rather than trusted: the server ranks each
 * dimension by its own measure, which is not always the one being plotted
 * (certification profiles and variant values carry both a user count and a
 * plan count), and a "top 12" taken off a list ranked by something else is
 * not a top 12. Ties keep the delivered order.
 * @template T
 * @param {Array<T>} items - The rows.
 * @param {(item: T) => number} valueOf - Reads the measure being ranked on.
 * @param {number} [limit] - How many rows the head keeps.
 * @returns {{shown: Array<T>, hidden: Array<T>}} The head and the tail, both
 *   in rank order.
 */
export function topN(items, valueOf, limit = DISTRIBUTION_LIMIT) {
  const sorted = list(items)
    .map((item, index) => ({ item, index }))
    .sort((a, b) => valueOf(b.item) - valueOf(a.item) || a.index - b.index)
    .map((entry) => entry.item);
  const cut = Math.max(0, limit);
  return { shown: sorted.slice(0, cut), hidden: sorted.slice(cut) };
}

/**
 * Turn one dimension list into a horizontal bar chart's inputs.
 *
 * One series, so every bar wears the same hue (`--chart-cat-1`) and the chart
 * needs no legend: these are nominal categories, and colouring them by their
 * own value would spend the identity channel re-encoding what bar length
 * already shows.
 *
 * The head and the tail are concatenated back together, so the caller hands
 * `<cts-chart>` the WHOLE ranked list and lets `max-bars` decide how much of
 * it is plotted — the data table then carries every row.
 * @param {Array<any>} items - Dimension rows from the payload.
 * @param {DistributionSpec} spec - Which fields to read.
 * @param {number} [limit] - How many rows are meant to be plotted.
 * @returns {Distribution} The chart inputs.
 */
export function distributionDatasets(items, spec, limit = DISTRIBUTION_LIMIT) {
  const valueOf = (/** @type {any} */ row) => Number(row && row[spec.value]) || 0;
  const ranked = topN(items, valueOf, limit);
  const rows = [...ranked.shown, ...ranked.hidden];
  const extra = spec.extra || "";
  return {
    labels: rows.map((row) => String((row && row[spec.label]) ?? "")),
    datasets: [
      {
        label: spec.valueLabel,
        data: rows.map(valueOf),
        colorVar: CATEGORY_COLOR_VARS[0],
      },
    ],
    extras: extra
      ? [
          {
            label: spec.extraLabel || extra,
            data: rows.map((row) => Number(row && row[extra]) || 0),
          },
        ]
      : [],
  };
}

/**
 * The three distribution charts, ready to render.
 * @typedef {object} Distributions
 * @property {Array<{key: string, distribution: Distribution}>} variants - One per variant parameter worth charting.
 * @property {Distribution|null} certProfiles - Certification profiles, or null when there are none.
 * @property {Distribution|null} entities - What was under test, or null when there is nothing.
 */

/**
 * Build all three distributions out of one payload's `dimensions`.
 *
 * A variant parameter with a single value is left out: a chart of one bar
 * says only that everything used the one value it could have used, which the
 * filter row already says by not offering a choice.
 *
 * Pure, and meant to be called through {@link memoiseByArgs} — the arrays it
 * returns go straight to `<cts-chart>`, which re-plots whenever their
 * identity changes.
 * @param {StatisticsDimensions} dimensions - The payload's dimensions.
 * @returns {Distributions} The distributions to render.
 */
export function buildDistributions(dimensions) {
  const variantValues = (dimensions && dimensions.variants) || {};
  const certProfiles = list(dimensions && dimensions.certProfiles);
  const entities = list(dimensions && dimensions.entities);

  return {
    variants: Object.keys(variantValues)
      .filter((key) => list(variantValues[key]).length > 1)
      .map((key) => ({
        key,
        distribution: distributionDatasets(variantValues[key], {
          label: "value",
          value: "users",
          valueLabel: "Users",
          extra: "plans",
          extraLabel: "Plans",
        }),
      })),
    certProfiles:
      certProfiles.length > 0
        ? distributionDatasets(certProfiles, {
            label: "name",
            value: "users",
            valueLabel: "Users",
            extra: "plans",
            extraLabel: "Plans",
          })
        : null,
    entities:
      entities.length > 0
        ? distributionDatasets(entities, {
            label: "entity",
            value: "runs",
            valueLabel: "Runs",
          })
        : null,
  };
}

// --- Modules -----------------------------------------------------------

/**
 * How many modules each of the two module charts plots. The same dozen the
 * distributions stop at, and for the same reason — but here the tail is not
 * carried by `<cts-chart max-bars>`: module names are long enough that the
 * chart's own data table would be a second, worse copy of the section's
 * table, so the charts are handed the twelve rows they plot and the section
 * table below them lists every module the server returned.
 */
export const MODULE_LIMIT = 12;

/**
 * One row of `data.modules` — a test module's traffic over the trailing 24
 * months, under the range and the family/plan filters (variant and
 * certification filters do not apply to it; the section says so).
 * @typedef {object} ModuleRow
 * @property {string} testName - The module's name, e.g. `oidcc-server`.
 * @property {number} runs - Test runs of it.
 * @property {number} users - Distinct identified users who ran it.
 * @property {number} failingUsers - How many of those users had it fail at
 *   least once; a user who hit a failure ten times still counts once.
 * @property {number} failingShare - `failingUsers / users`, 0–1, three
 *   decimals; 0 when `users` is 0.
 */

/**
 * A module row's four counts, read off the payload and coerced to numbers.
 * What a tooltip footer is written from — the name is already the bar's
 * label, so the footer never needs it.
 * @typedef {object} ModuleCounts
 * @property {number} runs - Test runs of the module.
 * @property {number} users - Distinct identified users who ran it.
 * @property {number} failingUsers - How many of those it failed for.
 * @property {number} failingShare - `failingUsers / users`, 0–1.
 */

/**
 * One module chart's inputs: a single-series horizontal bar chart, plus the
 * tooltip lines that carry the measures it does NOT plot.
 * @typedef {object} ModuleChart
 * @property {Array<string>} labels - Module names, biggest first — the whole
 *   of what is plotted, already cut to {@link MODULE_LIMIT}.
 * @property {Array<ChartDataset>} datasets - Exactly one series, in `--chart-cat-1`.
 * @property {Array<Array<string>>} footers - Tooltip footer lines, one entry
 *   per plotted bar, in the same order as `labels`.
 */

/**
 * Grouped figures — 1,420 must not read as 1420. Exported so the components
 * that render these numbers share ONE formatter with the helpers that write
 * them into tooltip footers, rather than each keeping a copy that could drift
 * to different options.
 */
export const NUMBER_FORMAT = new Intl.NumberFormat();

/**
 * @param {number} count - How many.
 * @param {string} noun - Singular noun.
 * @returns {string} e.g. `"1 user"`, `"38 users"`.
 */
function counted(count, noun) {
  return `${NUMBER_FORMAT.format(count)} ${noun}${count === 1 ? "" : "s"}`;
}

/**
 * Order two module rows by name — the last tier of both rankings, and the one
 * that makes them total: with it, two modules the reader cannot tell apart
 * still come back in the same order from one recomputation to the next.
 * Plain `<`, not `localeCompare`, because the server breaks the tie with
 * Java's `String::compareTo` (UTF-16 code units) and the two must agree.
 * @param {any} a - One row.
 * @param {any} b - The other.
 * @returns {number} Comparator result.
 */
function compareModuleNames(a, b) {
  const nameA = moduleName(a);
  const nameB = moduleName(b);
  return nameA < nameB ? -1 : nameA > nameB ? 1 : 0;
}

/**
 * The two measures a module chart can plot, how to rank on each, and what the
 * tooltip should say about the measures it does not plot. An explicit table
 * rather than a caller-supplied field name, so a typo cannot reach into the
 * payload; each key IS the field it reads.
 *
 * `compare` mirrors `ModuleRanker.BY_RUNS` / `BY_FAILING_USERS` on the server
 * exactly — runs then name, failing users then RUNS then name. The middle tier
 * is not decoration: at the twelve-row cut two modules on the same failing-user
 * count are not interchangeable, and keeping the alphabetically earlier one
 * over the busier one would plot a different twelve than the server ranked.
 * @type {Record<string, {label: string, compare: (a: any, b: any) => number,
 *   footer: (row: ModuleCounts) => Array<string>}>}
 */
const MODULE_METRICS = {
  runs: {
    label: "Runs",
    compare: (a, b) => moduleValue(b, "runs") - moduleValue(a, "runs") || compareModuleNames(a, b),
    // Runs are what the bar is; the reader's next question is how many people
    // that was, and how many of them it went wrong for.
    footer: (row) =>
      row.users === 0
        ? ["No identified users"]
        : [
            counted(row.users, "user"),
            `${NUMBER_FORMAT.format(row.failingUsers)} hit a failure ` +
              `(${formatShare(row.failingShare)})`,
          ],
  },
  failingUsers: {
    label: "Users who hit a failure",
    compare: (a, b) =>
      moduleValue(b, "failingUsers") - moduleValue(a, "failingUsers") ||
      moduleValue(b, "runs") - moduleValue(a, "runs") ||
      compareModuleNames(a, b),
    // A bar of 22 says nothing without the denominator: 22 of 29 is a module
    // with a problem, 22 of 3,000 is not.
    footer: (row) =>
      row.users === 0
        ? [counted(row.runs, "run")]
        : [
            `${NUMBER_FORMAT.format(row.failingUsers)} of ` +
              `${counted(row.users, "user")} (${formatShare(row.failingShare)})`,
            counted(row.runs, "run"),
          ],
  },
};

/**
 * Read one numeric field off a module row, tolerating a malformed one.
 * @param {any} row - The row.
 * @param {string} field - Field name.
 * @returns {number} The value, or 0.
 */
function moduleValue(row, field) {
  return Number(row && row[field]) || 0;
}

/**
 * @param {any} row - A module row.
 * @returns {string} Its name, or `""`.
 */
function moduleName(row) {
  return String((row && row.testName) ?? "");
}

/**
 * A module's share of users who hit a failure, as a percentage.
 *
 * Always one decimal, including for 0 and 1: a column of "0%", "100%" and
 * "23.7%" does not line up, and the third place the server rounds to is
 * noise at this width.
 * @param {number} share - `failingUsers / users`, 0–1.
 * @returns {string} e.g. `"23.7%"`, `"0.0%"`, `"100.0%"`; `"—"` for a value
 *   that is not a number at all.
 */
export function formatShare(share) {
  const value = Number(share);
  if (!Number.isFinite(value)) return "—";
  return `${(Math.min(1, Math.max(0, value)) * 100).toFixed(1)}%`;
}

/**
 * Rank `data.modules` on one of its two measures and turn the leading
 * {@link MODULE_LIMIT} into a horizontal bar chart's inputs.
 *
 * The list is ranked here rather than trusted: the server delivers ONE array
 * sorted by runs (the union of its two top-50 lists), so the failing-users
 * chart has to re-rank it — a "top 12" taken off a list ordered by something
 * else is not a top 12. Each ranking uses the server's own comparator, tier
 * for tier (see {@link MODULE_METRICS}), so the twelve plotted here are the
 * twelve the server would have picked, in the same order, every time.
 *
 * One series, so every bar wears the same hue (`--chart-cat-1`) and the chart
 * needs no legend: these are nominal categories, and colouring them by their
 * own value would re-encode what bar length already shows.
 * @param {Array<ModuleRow>} modules - The payload's `data.modules`.
 * @param {string} metric - `"runs"` or `"failingUsers"`.
 * @param {number} [limit] - How many bars to keep.
 * @returns {ModuleChart} The chart inputs.
 */
export function moduleDatasets(modules, metric, limit = MODULE_LIMIT) {
  // An unrecognised metric plots runs rather than nothing; the key is also the
  // field, so there is one name to get wrong instead of two.
  const key = MODULE_METRICS[metric] ? metric : "runs";
  const spec = MODULE_METRICS[key];
  const rows = list(modules).slice().sort(spec.compare).slice(0, Math.max(0, limit));
  return {
    labels: rows.map(moduleName),
    datasets: [
      {
        label: spec.label,
        data: rows.map((row) => moduleValue(row, key)),
        colorVar: CATEGORY_COLOR_VARS[0],
      },
    ],
    footers: rows.map((row) =>
      spec.footer({
        runs: moduleValue(row, "runs"),
        users: moduleValue(row, "users"),
        failingUsers: moduleValue(row, "failingUsers"),
        failingShare: Number(row && row.failingShare),
      }),
    ),
  };
}

/**
 * Everything the modules section renders: the two charts and the rows behind
 * the table under them.
 *
 * `rows` is the payload's array untouched — server order, which is by runs —
 * because the table is the complete listing the charts' top twelve are taken
 * out of, and re-sorting it would leave the reader without the order the
 * server ranked on.
 *
 * Pure, and meant to be called through {@link memoiseByArgs}: what it returns
 * goes straight to `<cts-chart>`, which re-plots whenever the identity of an
 * array it was handed changes.
 * @param {Array<ModuleRow>} modules - The payload's `data.modules`.
 * @returns {{rows: Array<ModuleRow>, byRuns: ModuleChart, byFailingUsers: ModuleChart}}
 *   The section's inputs; `rows` empty means "nothing in this window", which
 *   the section renders as its empty state.
 */
export function buildModules(modules) {
  const rows = list(modules);
  return {
    rows,
    byRuns: moduleDatasets(rows, "runs"),
    byFailingUsers: moduleDatasets(rows, "failingUsers"),
  };
}

/** Byte units, biggest last; `formatBytes` walks them from the small end. */
const BYTE_UNITS = ["B", "KB", "MB", "GB", "TB", "PB"];
/** Binary step. Storage counters come off `collStats`, which reports what the
 *  filesystem allocated, so the same 1024 step `df -h` and MongoDB Compass
 *  use is what makes these numbers comparable to those tools. */
const BYTE_STEP = 1024;

/**
 * Format a byte count for a storage tile: one decimal from KB up, none for
 * bytes (a tenth of a byte is not a thing), and never more than PB.
 * @param {number} bytes - The count.
 * @returns {string} e.g. `"1.7 GB"`, `"820 B"`, `"0 B"`.
 */
export function formatBytes(bytes) {
  const value = Number(bytes);
  if (!Number.isFinite(value) || value <= 0) return "0 B";
  let scaled = value;
  let unit = 0;
  while (scaled >= BYTE_STEP && unit < BYTE_UNITS.length - 1) {
    scaled /= BYTE_STEP;
    unit += 1;
  }
  return `${unit === 0 ? Math.round(scaled) : scaled.toFixed(1)} ${BYTE_UNITS[unit]}`;
}

/**
 * The busiest cell in a 2-D grid — the top of the heatmap's colour scale.
 * @param {Array<Array<number>>} values - Rows of counts.
 * @returns {number} The largest value, or 0 for an empty or all-zero grid.
 */
export function heatmapMax(values) {
  let max = 0;
  for (const row of list(values)) {
    for (const value of list(row)) {
      const n = Number(value) || 0;
      if (n > max) max = n;
    }
  }
  return max;
}

/**
 * Every cell added up — the caption's "N runs in this range".
 * @param {Array<Array<number>>} values - Rows of counts.
 * @returns {number} The sum.
 */
export function heatmapTotal(values) {
  let sum = 0;
  for (const row of list(values)) sum += total(row);
  return sum;
}

/** The palest a non-zero cell may be: below this it is indistinguishable
 *  from an empty one, which would hide a quiet hour rather than show it. */
const HEATMAP_MIN_MIX = 10;

/**
 * Where one cell sits on the sequential ramp, as the percentage of
 * `--chart-cat-1` mixed into the surface.
 *
 * The scale is **square-root**, not linear. Suite activity is heavily skewed
 * — a weekday office hour runs an order of magnitude more tests than 03:00 on
 * a Sunday — and on a linear ramp everything but the peak collapses into the
 * same near-white, which is the one thing a heatmap must not do. A sqrt ramp
 * is still monotonic, so it never misstates which cell is busier; it only
 * spends more of the colour range on the low end. Exact counts are in the
 * cell tooltip and the data table, so no comparison depends on judging a
 * fill.
 * @param {number} value - The cell's count.
 * @param {number} max - The busiest cell, from {@link heatmapMax}.
 * @returns {number} 0 for an empty cell, otherwise 10-100.
 */
export function heatmapIntensity(value, max) {
  const n = Number(value) || 0;
  const top = Number(max) || 0;
  if (n <= 0 || top <= 0) return 0;
  const share = Math.sqrt(Math.min(n, top) / top);
  return Math.round(HEATMAP_MIN_MIX + (100 - HEATMAP_MIN_MIX) * share);
}

/**
 * Where the legend samples the ramp: fractions of its LENGTH, low end first.
 * The top of the ramp is always sampled, so the darkest swatch is labelled
 * with the busiest cell.
 * @type {Array<number>}
 */
const HEATMAP_SCALE_POSITIONS = [0.25, 0.5, 0.75, 1];

/**
 * The legend's swatches: how dark each one is, and the value it stands for.
 *
 * This is the INVERSE of {@link heatmapIntensity}, and it exists because the
 * ramp is square-root scaled: five evenly spaced swatches labelled only
 * "0 … max" would read as linear and put the middle one at a quarter of the
 * value it actually means. A swatch a fraction `f` along the ramp stands for
 * `f² × max`, so the labels have to say so.
 *
 * The empty-cell swatch is not in here — it is not on the ramp at all (an
 * empty cell keeps the muted surface), so the component renders it itself.
 *
 * Two things happen to the labels on a SMALL scale, where `f² × max` rounds
 * away: a step never reads "0" (a swatch labelled 0 beside the muted 0 swatch
 * says the ramp starts at nothing, when in fact its palest step is one run),
 * so a non-zero step is clamped to 1; and steps that then say the same number
 * are collapsed onto the DARKEST of them, so a peak of 5 shows three swatches
 * rather than four, and the one labelled "1" is the shade a cell holding 1
 * actually gets. The exact inverse of {@link heatmapIntensity} therefore holds
 * only while the rounding is not clamped — with a peak of 1,600 it does; with
 * a peak of 5 the "1" swatch is one step darker than a real cell of 1.
 * @param {number} max - The busiest cell, from {@link heatmapMax}.
 * @returns {Array<{mix: number, value: number}>} Percentage of the hue to mix
 *   in, and the count it represents, palest first. Empty when there is no
 *   scale; never two steps with the same label.
 */
export function heatmapScaleSteps(max) {
  const top = Number(max) || 0;
  if (top <= 0) return [];
  /** @type {Array<{mix: number, value: number}>} */
  const steps = [];
  for (const position of HEATMAP_SCALE_POSITIONS) {
    const step = {
      mix: Math.round(HEATMAP_MIN_MIX + (100 - HEATMAP_MIN_MIX) * position),
      value: Math.max(1, Math.round(top * position * position)),
    };
    // The darkest of the equal steps wins, so the top of the ramp is always
    // sampled and every swatch is at least as dark as the cells it stands for.
    if (steps.length > 0 && steps[steps.length - 1].value === step.value) steps.pop();
    steps.push(step);
  }
  return steps;
}
