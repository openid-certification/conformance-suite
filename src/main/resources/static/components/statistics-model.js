/**
 * Pure shaping helpers for the admin statistics page.
 *
 * Everything here is a plain function over the `data` object served by
 * `GET /api/statistics/overview` — no DOM, no fetch, no Lit — so the
 * page component (`cts-statistics-page.js`) stays fetch/state/composition
 * only and every shaping rule is unit-testable in the vitest `unit`
 * project (`statistics-model.test.js`).
 *
 * The payload contract (see `net.openid.conformance.statistics.StatisticsOverview`):
 * every per-month array has exactly `months.length` entries and every
 * per-family map has an entry for every family in `families`, so these
 * helpers never have to fill gaps — the defensive guards below exist only
 * so a truncated or mocked payload degrades to an empty chart instead of
 * throwing inside `render()`.
 */

/**
 * The statistics payload. Every field is declared as present because the
 * server contract guarantees it; the runtime guards throughout this file are
 * defence against a truncated or mocked payload (a permissive test route
 * answering `{}`, an older server), and callers that deliberately pass a
 * partial object have to say so with a cast.
 * @typedef {object} StatisticsData
 * @property {Array<string>} families - Every family a series may be keyed by, fixed order.
 * @property {Array<string>} resultBuckets - Result buckets, fixed order.
 * @property {Array<string>} months - Contiguous `YYYY-MM` keys, oldest first.
 * @property {Record<string, Array<number>>} testRunsByFamily - Family → runs per month.
 * @property {Record<string, Array<number>>} plansByFamily - Family → plans per month.
 * @property {Record<string, Record<string, Array<number>>>} resultsByFamily - Family → bucket → runs per month.
 * @property {{activeByMonth: Array<number>, newByMonth: Array<number>}} users - User counts per month.
 * @property {Record<string, number>} tiles - Whole-collection counters.
 * @property {Array<{planName: string, runs: number}>} unresolvedPlans - Busiest unresolved plan names.
 */

/**
 * One Chart.js series in `<cts-chart>`'s shape.
 * @typedef {object} ChartDataset
 * @property {string} label - Series name (legend key, tooltip row).
 * @property {Array<number>} data - One value per month in the current slice.
 * @property {string} colorVar - CSS custom-property name, leading `--` included.
 */

/**
 * How many months each range preset keeps. `0` means "everything".
 * @type {Record<string, number>}
 */
export const RANGE_MONTHS = { "12m": 12, "24m": 24, all: 0 };

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
const BREAKDOWN_SOURCES = { runs: "testRunsByFamily", plans: "plansByFamily" };

/**
 * Sum a monthly series, tolerating a missing or non-array one.
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

/**
 * Keep the last `count` entries of a month-aligned array.
 * @template T
 * @param {Array<T>|undefined} values - The array.
 * @param {number} count - How many trailing entries to keep.
 * @returns {Array<T>} The tail, or `[]` when there is no array.
 */
function tail(values, count) {
  if (!Array.isArray(values)) return [];
  return values.slice(Math.max(0, values.length - count));
}

/**
 * Narrow a payload to the last N months of every month-aligned array.
 *
 * `families`, `resultBuckets`, `tiles` and `unresolvedPlans` are carried
 * over by reference: the tiles are a whole-database snapshot and are
 * deliberately NOT scoped by the range control.
 *
 * Returns the input object unchanged when nothing needs slicing (range
 * `"all"`, an unknown range, or a history shorter than the range), so the
 * common case allocates nothing.
 * @param {StatisticsData} data - The full payload.
 * @param {string} range - One of the {@link RANGE_MONTHS} keys.
 * @returns {StatisticsData} The narrowed payload.
 */
export function sliceRange(data, range) {
  if (!data || !Array.isArray(data.months)) return data;
  const count = RANGE_MONTHS[range] || 0;
  if (count <= 0 || count >= data.months.length) return data;

  /**
   * @param {Record<string, Array<number>>|undefined} byFamily - Family → series.
   * @returns {Record<string, Array<number>>} Same keys, sliced series.
   */
  const sliceFamilyMap = (byFamily) => {
    /** @type {Record<string, Array<number>>} */
    const out = {};
    for (const [family, series] of Object.entries(byFamily || {})) {
      out[family] = tail(series, count);
    }
    return out;
  };

  /** @type {Record<string, Record<string, Array<number>>>} */
  const results = {};
  for (const [family, buckets] of Object.entries(data.resultsByFamily || {})) {
    results[family] = sliceFamilyMap(buckets);
  }

  return {
    ...data,
    months: tail(data.months, count),
    testRunsByFamily: sliceFamilyMap(data.testRunsByFamily),
    plansByFamily: sliceFamilyMap(data.plansByFamily),
    resultsByFamily: results,
    users: {
      activeByMonth: tail(data.users && data.users.activeByMonth, count),
      newByMonth: tail(data.users && data.users.newByMonth, count),
    },
  };
}

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
 * Callers must pass the FULL payload, never a slice: a family's colour is
 * an identity, so changing the range or the family filter must never
 * repaint the charts. `statistics-model.test.js` pins that with a fixture
 * whose recent ranking differs from its all-time ranking.
 * @param {StatisticsData} data - The full payload.
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
 * Shared body of {@link runsDatasets} and {@link plansDatasets}.
 * @param {StatisticsData} data - The payload (already range-sliced).
 * @param {Record<string, string>} slots - From {@link assignFamilySlots}.
 * @param {string} family - Selected family, or `""` for all.
 * @param {"testRunsByFamily"|"plansByFamily"} key - Which map to read.
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

  // A family with nothing in the current slice contributes an invisible
  // segment and a dead legend key, so it is dropped rather than rendered.
  return orderedFamilies(data, slots)
    .filter((name) => total(byFamily[name]) > 0)
    .map((name) => ({ label: name, data: [...byFamily[name]], colorVar: colorOf(name) }));
}

/**
 * Test module runs per month, one dataset per family.
 * @param {StatisticsData} data - The payload (already range-sliced).
 * @param {Record<string, string>} slots - From {@link assignFamilySlots}.
 * @param {string} family - Selected family, or `""` for all.
 * @returns {Array<ChartDataset>} Datasets for `<cts-chart>`.
 */
export function runsDatasets(data, slots, family) {
  return familyDatasets(data, slots, family, "testRunsByFamily");
}

/**
 * Test plans created per month, one dataset per family, coloured with the
 * same slots as {@link runsDatasets} so a family reads identically on both
 * charts.
 * @param {StatisticsData} data - The payload (already range-sliced).
 * @param {Record<string, string>} slots - From {@link assignFamilySlots}.
 * @param {string} family - Selected family, or `""` for all.
 * @returns {Array<ChartDataset>} Datasets for `<cts-chart>`.
 */
export function plansDatasets(data, slots, family) {
  return familyDatasets(data, slots, family, "plansByFamily");
}

/**
 * Result mix per month: one dataset per result bucket, in the payload's
 * bucket order, wearing the suite's reserved status colours.
 *
 * With no family selected the buckets are summed across every family in
 * `families`; with one selected only that family's buckets are read.
 * Bucket labels are the server's own strings (`PASSED`, `NEVER_FINISHED`,
 * …) — the same vocabulary the rest of the suite shows.
 * @param {StatisticsData} data - The payload (already range-sliced).
 * @param {string} family - Selected family, or `""` for all.
 * @returns {Array<ChartDataset>} Datasets for `<cts-chart>`.
 */
export function resultsDatasets(data, family) {
  const buckets = list(data && data.resultBuckets);
  const byFamily = (data && data.resultsByFamily) || {};
  const months = list(data && data.months).length;
  const keys = family ? [family] : list(data && data.families);

  return buckets
    .map((bucket) => {
      const summed = new Array(months).fill(0);
      for (const key of keys) {
        const series = byFamily[key] && byFamily[key][bucket];
        if (!Array.isArray(series)) continue;
        for (let i = 0; i < months; i++) summed[i] += Number(series[i]) || 0;
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
 * Active and first-seen users per month. Always two series — "New" is
 * meaningful at zero, and a disappearing legend key on a two-series chart
 * reads as a bug — and never scoped by the family filter, because the
 * user pipeline does not key by family.
 * @param {StatisticsData} data - The payload (already range-sliced).
 * @returns {Array<ChartDataset>} Datasets for `<cts-chart>`.
 */
export function usersDatasets(data) {
  const users = (data && data.users) || {};
  const active = Array.isArray(users.activeByMonth) ? users.activeByMonth : [];
  const fresh = Array.isArray(users.newByMonth) ? users.newByMonth : [];
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
 * one month, biggest first (`"OpenID Federation: 12"`). Families that are
 * zero in that month are omitted — a footer line reading `": 0"` is noise.
 *
 * `monthIndex` indexes the SLICED payload, so pass the same `data` the
 * chart was built from.
 * @param {StatisticsData} data - The payload (already range-sliced).
 * @param {Record<string, string>} slots - From {@link assignFamilySlots}.
 * @param {number} monthIndex - Hovered position in `months`.
 * @param {string} [source] - `"runs"` (default) or `"plans"`.
 * @returns {Array<string>} `"<family>: <count>"` lines.
 */
export function otherBreakdown(data, slots, monthIndex, source = "runs") {
  const key = BREAKDOWN_SOURCES[source] || BREAKDOWN_SOURCES.runs;
  const byFamily = (data && data[key]) || {};
  const families = list(data && data.families);

  return families
    .map((family, index) => ({
      family,
      index,
      count: Number((byFamily[family] || [])[monthIndex]) || 0,
    }))
    .filter((entry) => slots && slots[entry.family] === OTHER_COLOR_VAR && entry.count > 0)
    .sort((a, b) => b.count - a.count || a.index - b.index)
    .map((entry) => `${entry.family}: ${entry.count}`);
}

/**
 * Families worth offering in the filter select: those with at least one run
 * ever. Call with the FULL payload — options that vanish when the user
 * narrows the range would make the control feel broken.
 * @param {StatisticsData} data - The full payload.
 * @returns {Array<string>} Family names in the payload's order.
 */
export function familiesWithRuns(data) {
  const families = list(data && data.families);
  const runs = (data && data.testRunsByFamily) || {};
  return families.filter((family) => total(runs[family]) > 0);
}
