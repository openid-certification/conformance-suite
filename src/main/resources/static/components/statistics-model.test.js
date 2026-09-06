import { describe, it, expect } from "vitest";
import { emptyFilter, planListFilterFromUrl } from "./plan-list-filter.js";
import {
  CATEGORY_COLOR_VARS,
  DEFAULT_RANGE,
  EMPTY_OPTIONS,
  OTHER_COLOR_VAR,
  OTHER_LABEL,
  RANGE_PRESETS,
  RESULT_COLOR_VARS,
  DISTRIBUTION_LIMIT,
  assignFamilySlots,
  buildChartInputs,
  buildDistributions,
  buildModules,
  certifiedDatasets,
  defaultFilterState,
  distributionDatasets,
  drillDownFamily,
  drillDownUrl,
  familiesWithActivity,
  foldOther,
  formatBytes,
  formatShare,
  hasAnyData,
  heatmapIntensity,
  isSyntheticFamily,
  heatmapMax,
  heatmapScaleSteps,
  heatmapTotal,
  isFiltered,
  isNarrowed,
  memoiseByArgs,
  moduleDatasets,
  otherBreakdown,
  periodBounds,
  periodLabels,
  plansDatasets,
  queryFromState,
  rangePreset,
  rangeToQuery,
  optionsForTransition,
  optionsFrom,
  resultsDatasets,
  runsDatasets,
  sameState,
  stateFromUrl,
  rankBy,
  urlFromState,
  usersDatasets,
  visibleVariants,
} from "./statistics-model.js";

// --- Fixture -----------------------------------------------------------
//
// 30 months split 18 "early" + 12 "late" so the last-12 series can be given
// a deliberately DIFFERENT family ranking from the all-time totals. That is
// the only way to prove the slot assignment is read from `familyTotals` and
// never from the series on screen (a range or filter change must never
// repaint the charts).
//
// All-time totals (per family, 18*early + 12*late):
//   FAPI2 Security Profile 6000 | FAPI1 Advanced 6000 | OpenID Connect Core 4500
//   OID4VP 3600 | OID4VCI 3000 | FAPI-CIBA 2400 | eKYC & Identity Assurance 1800
//   -> those seven are the top seven, in that order (the first two tie and are
//      broken by `families` order).
//   OpenID Federation 600 | No plan 600 | Other / retired 300 | SSF 0 -> tail.
//
// Last-12 totals: eKYC drops to 0 and OpenID Federation climbs to 600, so a
// ranking derived from that payload would hand eKYC's colour to OpenID
// Federation.

const MONTH_COUNT = 30;
const LATE_FROM = 18;

/**
 * Build a 30-entry monthly series: `early` for the first 18 months, `late`
 * for the last 12.
 * @param {number} early - Value for months 0..17.
 * @param {number} late - Value for months 18..29.
 * @returns {Array<number>} The series.
 */
function series(early, late) {
  return Array.from({ length: MONTH_COUNT }, (_, i) => (i < LATE_FROM ? early : late));
}

const PERIODS = Array.from({ length: MONTH_COUNT }, (_, i) => {
  const month = (i % 12) + 1;
  const year = 2024 + Math.floor(i / 12);
  return `${year}-${String(month).padStart(2, "0")}`;
});

const FAMILIES = [
  "FAPI2 Security Profile",
  "FAPI1 Advanced",
  "OpenID Connect Core",
  "OID4VP",
  "OID4VCI",
  "FAPI-CIBA",
  "eKYC & Identity Assurance",
  "OpenID Federation",
  "Shared Signals Framework",
  "No plan",
  "Other / retired",
];

const RESULT_BUCKETS = ["PASSED", "WARNING", "REVIEW", "FAILED", "SKIPPED", "NEVER_FINISHED"];

/**
 * The full `data` payload the endpoint returns for the unfiltered, all-time
 * baseline request, rebuilt per test so a helper that mutates its input is
 * caught by the next test rather than shared.
 * @returns {any} A statistics overview payload.
 */
function makeData() {
  return withTotals({
    families: [...FAMILIES],
    syntheticFamilies: [NO_PLAN, OTHER_RETIRED],
    resultBuckets: [...RESULT_BUCKETS],
    periods: [...PERIODS],
    granularity: "month",
    testRunsByFamily: {
      "FAPI2 Security Profile": series(200, 200),
      "FAPI1 Advanced": series(200, 200),
      "OpenID Connect Core": series(150, 150),
      OID4VP: series(120, 120),
      OID4VCI: series(100, 100),
      "FAPI-CIBA": series(80, 80),
      "eKYC & Identity Assurance": series(100, 0),
      "OpenID Federation": series(0, 50),
      "Shared Signals Framework": series(0, 0),
      "No plan": series(20, 20),
      "Other / retired": series(10, 10),
    },
    plansByFamily: {
      "FAPI2 Security Profile": series(20, 20),
      "FAPI1 Advanced": series(10, 10),
      "OpenID Connect Core": series(0, 0),
      OID4VP: series(0, 0),
      OID4VCI: series(0, 0),
      "FAPI-CIBA": series(0, 0),
      "eKYC & Identity Assurance": series(0, 0),
      "OpenID Federation": series(0, 5),
      "Shared Signals Framework": series(0, 0),
      "No plan": series(0, 0),
      "Other / retired": series(1, 1),
    },
    certifiedByFamily: {
      "FAPI2 Security Profile": series(3, 4),
      "FAPI1 Advanced": series(1, 0),
      "OpenID Connect Core": series(0, 0),
      OID4VP: series(0, 0),
      OID4VCI: series(0, 0),
      "FAPI-CIBA": series(0, 0),
      "eKYC & Identity Assurance": series(0, 0),
      "OpenID Federation": series(0, 2),
      "Shared Signals Framework": series(0, 0),
      "No plan": series(0, 0),
      "Other / retired": series(0, 0),
    },
    resultsByFamily: {
      "FAPI2 Security Profile": {
        PASSED: series(150, 150),
        WARNING: series(0, 0),
        REVIEW: series(10, 10),
        FAILED: series(30, 30),
        SKIPPED: series(0, 0),
        NEVER_FINISHED: series(10, 10),
      },
      "FAPI1 Advanced": {
        PASSED: series(100, 100),
        WARNING: series(0, 0),
        REVIEW: series(0, 0),
        FAILED: series(100, 100),
        SKIPPED: series(0, 0),
        NEVER_FINISHED: series(0, 0),
      },
    },
    users: {
      activeByPeriod: series(5, 9),
      newByPeriod: series(1, 2),
    },
    tiles: {
      totalTests: 91800,
      totalPlans: 1932,
      totalUsers: 210,
      testsLast24h: 41,
      testsLast7d: 260,
      testsLast30d: 980,
      inProgress: 3,
      stuck: 7,
      certifiedPlans: 88,
      publishedPlans: 45,
    },
    dimensions: {
      plans: [
        { planName: "plan-a", family: "FAPI2 Security Profile", runs: 900, plans: 30 },
        { planName: "plan-b", family: "FAPI2 Security Profile", runs: 400, plans: 12 },
        { planName: "plan-c", family: "OID4VP", runs: 100, plans: 4 },
      ],
      variants: {
        client_auth_type: [
          { value: "private_key_jwt", users: 9, plans: 40 },
          { value: "mtls", users: 4, plans: 12 },
        ],
        fapi_profile: [{ value: "plain_fapi", users: 6, plans: 20 }],
      },
      certProfiles: [{ name: "FAPI2 Security Profile Final", users: 3, plans: 8 }],
      entities: [{ entity: "Test an OpenID Provider / Authorization Server", runs: 5000 }],
    },
    unresolvedPlans: [{ planName: "fapi-rw-id2", runs: 300 }],
  });
}

/** The two synthetic buckets, as the server names them on every payload. */
const NO_PLAN = "No plan";
const OTHER_RETIRED = "Other / retired";

/**
 * @param {Array<number>} values - A series.
 * @returns {number} Its sum.
 */
function sum(values) {
  return values.reduce((total, value) => total + value, 0);
}

/**
 * Give a payload the `familyTotals` the server would compute for it: every
 * family's whole-history series summed. Tests that want the totals to
 * disagree with the series on screen overwrite them afterwards.
 * @param {any} data - A payload without totals.
 * @returns {any} The same payload, with `familyTotals` filled in.
 */
function withTotals(data) {
  data.familyTotals = Object.fromEntries(
    data.families.map((/** @type {string} */ family) => [
      family,
      {
        runs: sum(data.testRunsByFamily[family] || []),
        plans: sum(data.plansByFamily[family] || []),
        certified: sum(data.certifiedByFamily[family] || []),
      },
    ]),
  );
  return data;
}

/**
 * Stand in for what the SERVER returns for a narrower range: the same payload
 * with every period-aligned array cut to its last `count` entries. Test-only
 * — the page has not sliced anything since the range moved server-side.
 * @param {any} data - A full payload.
 * @param {number} count - How many trailing periods to keep.
 * @returns {any} The narrowed payload.
 */
function lastPeriods(data, count) {
  const cut = (/** @type {Array<any>} */ values) => values.slice(values.length - count);
  const cutMap = (/** @type {Record<string, Array<number>>} */ map) =>
    Object.fromEntries(Object.entries(map).map(([key, values]) => [key, cut(values)]));
  return {
    ...data,
    periods: cut(data.periods),
    testRunsByFamily: cutMap(data.testRunsByFamily),
    plansByFamily: cutMap(data.plansByFamily),
    certifiedByFamily: cutMap(data.certifiedByFamily),
    resultsByFamily: Object.fromEntries(
      Object.entries(data.resultsByFamily).map(([family, byBucket]) => [
        family,
        cutMap(/** @type {Record<string, Array<number>>} */ (byBucket)),
      ]),
    ),
    users: {
      activeByPeriod: cut(data.users.activeByPeriod),
      newByPeriod: cut(data.users.newByPeriod),
    },
  };
}

/** @returns {any} The payload the server returns for the 12-month range. */
function makeRecent() {
  return lastPeriods(makeData(), 12);
}

// --- Range presets and the request query -------------------------------

describe("RANGE_PRESETS / rangeToQuery", () => {
  it("offers three weekly and three monthly presets, monthly by default", () => {
    expect(RANGE_PRESETS.map((preset) => preset.value)).toEqual([
      "12w",
      "26w",
      "52w",
      "12m",
      "24m",
      "all",
    ]);
    expect(RANGE_PRESETS.filter((preset) => preset.group === "weekly")).toHaveLength(3);
    expect(DEFAULT_RANGE).toBe("12m");
    expect(rangePreset(DEFAULT_RANGE).granularity).toBe("month");
  });

  it("counts monthly ranges back from the current month, inclusive", () => {
    const now = new Date("2026-06-15T09:00:00Z");
    expect(rangeToQuery("12m", now)).toEqual({ granularity: "month", from: "2025-07" });
    expect(rangeToQuery("24m", now)).toEqual({ granularity: "month", from: "2024-07" });
  });

  it("crosses the year boundary without arithmetic of its own", () => {
    expect(rangeToQuery("12m", new Date("2026-01-15T00:00:00Z"))).toEqual({
      granularity: "month",
      from: "2025-02",
    });
    expect(rangeToQuery("24m", new Date("2026-01-01T00:00:00Z"))).toEqual({
      granularity: "month",
      from: "2024-02",
    });
  });

  it("counts weekly ranges back from the Monday of the current ISO week", () => {
    // 2026-06-01 IS a Monday.
    expect(rangeToQuery("12w", new Date("2026-06-01T23:59:59Z"))).toEqual({
      granularity: "week",
      from: "2026-03-16",
    });
    expect(rangeToQuery("26w", new Date("2026-06-01T00:00:00Z"))).toEqual({
      granularity: "week",
      from: "2025-12-08",
    });
    expect(rangeToQuery("52w", new Date("2026-06-01T00:00:00Z"))).toEqual({
      granularity: "week",
      from: "2025-06-09",
    });
  });

  it("puts Sunday in the week that started six days earlier, across the year end", () => {
    // 2026-01-04 is a Sunday; its ISO week began on 2025-12-29.
    expect(rangeToQuery("12w", new Date("2026-01-04T18:30:00Z"))).toEqual({
      granularity: "week",
      from: "2025-10-13",
    });
    // ...and every day of that week gives the same answer.
    for (const day of ["2025-12-29", "2025-12-31", "2026-01-01", "2026-01-04"]) {
      expect(rangeToQuery("12w", new Date(`${day}T12:00:00Z`)).from).toBe("2025-10-13");
    }
  });

  it('asks for no lower bound at all for "All time"', () => {
    expect(rangeToQuery("all", new Date("2026-06-01T00:00:00Z"))).toEqual({
      granularity: "month",
      from: "",
    });
  });

  it("falls back to the default for a range that no longer exists", () => {
    const now = new Date("2026-06-15T00:00:00Z");
    expect(rangeToQuery("18m", now)).toEqual(rangeToQuery(DEFAULT_RANGE, now));
    expect(rangePreset("nonsense").value).toBe(DEFAULT_RANGE);
  });
});

describe("queryFromState", () => {
  const now = new Date("2026-06-15T00:00:00Z");

  it("sends the granularity and the lower bound, and never an upper one", () => {
    const query = queryFromState(defaultFilterState(), now);
    // No `to`: the server's axis already ends at the period containing today,
    // so an upper bound would only be a second way of saying so.
    expect(query.toString()).toBe("granularity=month&from=2025-07");
  });

  it("sends every filter, with variants under the server's prefix", () => {
    const query = queryFromState(
      {
        range: "26w",
        family: "FAPI-CIBA",
        plan: "fapi-ciba-id1-test-plan",
        variant: { fapi_profile: "openbanking_brazil", client_auth_type: "mtls" },
        cert: "Brazil Open Finance",
      },
      now,
    );
    expect(query.get("granularity")).toBe("week");
    expect(query.get("family")).toBe("FAPI-CIBA");
    expect(query.get("plan")).toBe("fapi-ciba-id1-test-plan");
    expect(query.get("variant.fapi_profile")).toBe("openbanking_brazil");
    expect(query.get("variant.client_auth_type")).toBe("mtls");
    expect(query.get("cert")).toBe("Brazil Open Finance");
  });

  it("orders variants by name, so the same filter is always the same request", () => {
    const one = queryFromState(
      { ...defaultFilterState(), variant: { z_last: "1", a_first: "2" } },
      now,
    );
    const other = queryFromState(
      { ...defaultFilterState(), variant: { a_first: "2", z_last: "1" } },
      now,
    );
    expect(one.toString()).toBe(other.toString());
    expect(one.toString()).toContain("variant.a_first=2&variant.z_last=1");
  });

  it("leaves out empty filters rather than sending blank ones", () => {
    const query = queryFromState(
      { range: "all", family: "", plan: "  ", variant: { k: "" }, cert: "" },
      now,
    );
    expect(query.toString()).toBe("granularity=month");
  });
});

// --- URL state ---------------------------------------------------------

describe("stateFromUrl / urlFromState", () => {
  it("reads every parameter the page owns", () => {
    const state = stateFromUrl(
      "?range=26w&family=OID4VP&plan=oid4vp-1final-verifier-test-plan" +
        "&variant.client_auth_type=mtls&variant.fapi_profile=plain_fapi&cert=Some+Profile",
    );
    expect(state).toEqual({
      range: "26w",
      family: "OID4VP",
      plan: "oid4vp-1final-verifier-test-plan",
      variant: { client_auth_type: "mtls", fapi_profile: "plain_fapi" },
      cert: "Some Profile",
    });
  });

  it("falls back to the default range and ignores blanks and strangers", () => {
    expect(stateFromUrl("?range=fortnightly&family=&variant.=x&variant.k=+&public=true")).toEqual(
      defaultFilterState(),
    );
    expect(stateFromUrl("")).toEqual(defaultFilterState());
    expect(stateFromUrl(undefined)).toEqual(defaultFilterState());
  });

  it("round-trips a state through the URL", () => {
    const state = {
      range: "52w",
      family: "FAPI1 Advanced",
      plan: "fapi1-advanced-final-test-plan",
      variant: { fapi_profile: "openbanking_brazil" },
      cert: "Brazil Open Finance",
    };
    expect(stateFromUrl(urlFromState(state))).toEqual(state);
  });

  it("always names the range, so a copied link opens the view that was shared", () => {
    expect(urlFromState(defaultFilterState())).toBe("?range=12m");
    expect(urlFromState({ ...defaultFilterState(), range: "nonsense" })).toBe("?range=12m");
  });

  it("removes a cleared filter instead of leaving an empty parameter behind", () => {
    const before = urlFromState({
      range: "12m",
      family: "OID4VP",
      plan: "p",
      variant: { k: "v" },
      cert: "c",
    });
    const after = urlFromState(defaultFilterState(), before);
    expect(after).toBe("?range=12m");
  });

  it("leaves parameters this page does not own alone", () => {
    const search = urlFromState({ ...defaultFilterState(), family: "OID4VP" }, "?id=story&debug=1");
    const params = new URLSearchParams(search);
    expect(params.get("id")).toBe("story");
    expect(params.get("debug")).toBe("1");
    expect(params.get("family")).toBe("OID4VP");
  });
});

describe("isFiltered / sameState / defaultFilterState", () => {
  it("starts unfiltered on the default range", () => {
    expect(defaultFilterState()).toEqual({
      range: DEFAULT_RANGE,
      family: "",
      plan: "",
      variant: {},
      cert: "",
    });
    expect(isFiltered(defaultFilterState())).toBe(false);
  });

  it("does not count the range as a filter", () => {
    expect(isFiltered({ ...defaultFilterState(), range: "52w" })).toBe(false);
    expect(isFiltered({ ...defaultFilterState(), family: "OID4VP" })).toBe(true);
    expect(isFiltered({ ...defaultFilterState(), variant: { k: "v" } })).toBe(true);
    expect(isFiltered({ ...defaultFilterState(), cert: "c" })).toBe(true);
  });

  it("is narrowed by a filter or by anything short of the whole history", () => {
    expect(isNarrowed(defaultFilterState())).toBe(true);
    expect(isNarrowed({ ...defaultFilterState(), range: "all" })).toBe(false);
    expect(isNarrowed({ ...defaultFilterState(), range: "all", cert: "c" })).toBe(true);
    expect(isNarrowed({ ...defaultFilterState(), range: "52w" })).toBe(true);
    // Derived from the preset table, not from the string "all".
    expect(isNarrowed({ ...defaultFilterState(), range: "nonsense" })).toBe(true);
  });

  it("compares two states by everything that reaches the request", () => {
    const base = { range: "12m", family: "A", plan: "", variant: { k: "v" }, cert: "" };
    expect(sameState(base, { ...base, variant: { k: "v" } })).toBe(true);
    expect(sameState(base, { ...base, range: "24m" })).toBe(false);
    expect(sameState(base, { ...base, variant: { k: "w" } })).toBe(false);
    expect(sameState(base, { ...base, variant: {} })).toBe(false);
    expect(sameState(base, { ...base, variant: { k: "v", j: "w" } })).toBe(false);
  });
});

// --- Period labels and bounds ------------------------------------------

describe("periodLabels", () => {
  it("shows a month as the key the server sent", () => {
    expect(periodLabels(["2026-05", "2026-06"], "month")).toEqual(["2026-05", "2026-06"]);
  });

  it("shows a week as its Monday, carrying the year only where it changes", () => {
    expect(periodLabels(["2025-12-22", "2025-12-29", "2026-01-05", "2026-01-12"], "week")).toEqual([
      "22 Dec 2025",
      "29 Dec",
      "5 Jan 2026",
      "12 Jan",
    ]);
  });

  it("passes an unusable key through rather than inventing a date", () => {
    expect(periodLabels(["not-a-date", "2026-13-01"], "week")).toEqual([
      "not-a-date",
      "2026-13-01",
    ]);
  });

  it("tolerates a missing axis", () => {
    expect(periodLabels(undefined, "week")).toEqual([]);
  });
});

describe("periodBounds", () => {
  it("covers a month from its first day to the first day of the next", () => {
    expect(periodBounds("2026-06", "month")).toEqual({ from: "2026-06-01", to: "2026-07-01" });
    expect(periodBounds("2026-12", "month")).toEqual({ from: "2026-12-01", to: "2027-01-01" });
  });

  it("covers a week from its Monday to the next Monday", () => {
    expect(periodBounds("2025-12-29", "week")).toEqual({ from: "2025-12-29", to: "2026-01-05" });
  });

  it("returns nothing for a key of the wrong granularity", () => {
    expect(periodBounds("2026-06-01", "month")).toBeNull();
    expect(periodBounds("2026-06", "week")).toBeNull();
    expect(periodBounds("2026-13", "month")).toBeNull();
    expect(periodBounds("", "month")).toBeNull();
  });
});

// --- Drill-down --------------------------------------------------------

describe("drillDownUrl", () => {
  /** @type {() => any} The payload's period axis, monthly. */
  const monthly = () => ({ periods: [...PERIODS], granularity: "month" });
  /** @type {any} A payload with no periods at all. */
  const noPeriods = {};

  it("takes the family from the clicked dataset when none is filtered", () => {
    const url = drillDownUrl(
      defaultFilterState(),
      { periodIndex: 0, family: "FAPI-CIBA" },
      monthly(),
    );
    expect(url).toBe("plans.html?family=FAPI-CIBA&from=2024-01-01&to=2024-02-01");
  });

  it("keeps the filtered family even when a different series was clicked", () => {
    // The page is already showing one family, so every bar in every chart
    // belongs to it; the dataset label cannot widen the slice.
    const state = { ...defaultFilterState(), family: "OID4VP" };
    const url = drillDownUrl(state, { periodIndex: 1, family: "FAPI-CIBA" }, monthly());
    expect(url).toBe("plans.html?family=OID4VP&from=2024-02-01&to=2024-03-01");
  });

  it("carries the plan and every variant, variants in a stable order", () => {
    const state = {
      ...defaultFilterState(),
      family: "FAPI1 Advanced",
      plan: "fapi1-advanced-final-test-plan",
      variant: { fapi_profile: "openbanking_brazil", client_auth_type: "mtls" },
    };
    expect(drillDownUrl(state, { periodIndex: 0, family: "" }, monthly())).toBe(
      "plans.html?family=FAPI1+Advanced&plan=fapi1-advanced-final-test-plan" +
        "&variant.client_auth_type=mtls&variant.fapi_profile=openbanking_brazil" +
        "&from=2024-01-01&to=2024-02-01",
    );
  });

  it("forwards the certification profile as it is", () => {
    // The statistics count a plan under each profile it names and the
    // listing matches any one of them, so one name means the same on both
    // ends and nothing is split or rewritten.
    const state = {
      ...defaultFilterState(),
      family: "FAPI-CIBA",
      cert: "FAPI-CIBA: Poll w/ MTLS",
    };
    const url = new URL(
      String(drillDownUrl(state, { periodIndex: 0, family: "" }, monthly())),
      "https://example.test/",
    );
    expect(url.searchParams.get("cert")).toBe("FAPI-CIBA: Poll w/ MTLS");
  });

  it("percent-encodes what a query string cannot carry", () => {
    const state = {
      ...defaultFilterState(),
      family: "eKYC & Identity Assurance",
      cert: "FAPI-CIBA: Poll w/ MTLS",
    };
    const raw = String(drillDownUrl(state, { periodIndex: 0, family: "" }, monthly()));
    expect(raw).toContain("family=eKYC+%26+Identity+Assurance");
    expect(raw).toContain("cert=FAPI-CIBA%3A+Poll+w%2F+MTLS");
    const url = new URL(raw, "https://example.test/");
    expect(url.searchParams.get("family")).toBe("eKYC & Identity Assurance");
    expect(url.searchParams.get("cert")).toBe("FAPI-CIBA: Poll w/ MTLS");
  });

  it("bounds a weekly bar by its Monday and the next", () => {
    /** @type {any} */
    const weekly = { periods: ["2026-04-27", "2026-05-04"], granularity: "week" };
    expect(drillDownUrl(defaultFilterState(), { periodIndex: 1, family: "OIDCC" }, weekly)).toBe(
      "plans.html?family=OIDCC&from=2026-05-04&to=2026-05-11",
    );
  });

  it("declines the folded Other series and the payload's synthetic buckets", () => {
    const state = defaultFilterState();
    const data = { ...monthly(), syntheticFamilies: [NO_PLAN, OTHER_RETIRED] };
    expect(drillDownUrl(state, { periodIndex: 0, family: OTHER_LABEL }, data)).toBeNull();
    expect(drillDownUrl(state, { periodIndex: 0, family: NO_PLAN }, data)).toBeNull();
    expect(drillDownUrl(state, { periodIndex: 0, family: OTHER_RETIRED }, data)).toBeNull();
    // Also when the synthetic bucket is the FILTER rather than the click: the
    // listing cannot express "not in the registry" either way.
    expect(
      drillDownUrl({ ...state, family: NO_PLAN }, { periodIndex: 0, family: "" }, data),
    ).toBeNull();
    // The names are the payload's, not the client's: a payload that names
    // nothing as synthetic lets the same family through.
    expect(drillDownUrl(state, { periodIndex: 0, family: NO_PLAN }, monthly())).toBe(
      "plans.html?family=No+plan&from=2024-01-01&to=2024-02-01",
    );
  });

  it("drills into the period alone when nothing names a family", () => {
    // The results chart's datasets are result buckets, so the click carries
    // no family — the period is still a useful narrowing on its own.
    expect(drillDownUrl(defaultFilterState(), { periodIndex: 2, family: "" }, monthly())).toBe(
      "plans.html?from=2024-03-01&to=2024-04-01",
    );
  });

  it("drops the dates rather than the link when the period cannot be resolved", () => {
    const state = { ...defaultFilterState(), family: "OID4VP" };
    expect(drillDownUrl(state, { periodIndex: 999, family: "" }, monthly())).toBe(
      "plans.html?family=OID4VP",
    );
    expect(drillDownUrl(state, { periodIndex: -1, family: "" }, monthly())).toBe(
      "plans.html?family=OID4VP",
    );
    expect(drillDownUrl(state, { periodIndex: 0, family: "" }, noPeriods)).toBe(
      "plans.html?family=OID4VP",
    );
  });

  it("produces exactly the filter the plans page reads back out of it", () => {
    // The parity test across the link: this is the ONE assertion that fails if
    // the two ends ever disagree about a parameter name, the variant prefix or
    // how a value is encoded. `drillDownUrl` serialises through the listing's
    // own `toParams`, and this parses with the listing's own parser.
    const state = {
      ...defaultFilterState(),
      family: "eKYC & Identity Assurance",
      plan: "ekyc-test-plan",
      variant: { fapi_profile: "openbanking_brazil", client_auth_type: "mtls" },
      cert: "FAPI-CIBA: Poll w/ MTLS",
    };
    const url = String(drillDownUrl(state, { periodIndex: 5, family: "" }, monthly()));
    expect(url.startsWith("plans.html?")).toBe(true);
    expect(planListFilterFromUrl(url.slice(url.indexOf("?")))).toEqual({
      ...emptyFilter(),
      family: "eKYC & Identity Assurance",
      plan: "ekyc-test-plan",
      variant: { fapi_profile: "openbanking_brazil", client_auth_type: "mtls" },
      cert: "FAPI-CIBA: Poll w/ MTLS",
      from: "2024-06-01",
      to: "2024-07-01",
    });
  });

  it("links to the whole listing when there is nothing at all to narrow by", () => {
    expect(drillDownUrl(defaultFilterState(), { periodIndex: 999, family: "" }, noPeriods)).toBe(
      "plans.html",
    );
  });
});

describe("isSyntheticFamily", () => {
  it("is whatever the payload names, and nothing when it names nothing", () => {
    expect(isSyntheticFamily(makeData(), NO_PLAN)).toBe(true);
    expect(isSyntheticFamily(makeData(), OTHER_RETIRED)).toBe(true);
    expect(isSyntheticFamily(makeData(), "OID4VP")).toBe(false);
    expect(isSyntheticFamily(/** @type {any} */ ({}), NO_PLAN)).toBe(false);
    expect(isSyntheticFamily(/** @type {any} */ ({ syntheticFamilies: "no" }), NO_PLAN)).toBe(
      false,
    );
  });
});

describe("drillDownFamily", () => {
  it("names the bucket a refused click resolved to, for the message", () => {
    expect(drillDownFamily(defaultFilterState(), { periodIndex: 0, family: OTHER_LABEL })).toBe(
      OTHER_LABEL,
    );
    expect(drillDownFamily({ ...defaultFilterState(), family: NO_PLAN }, { periodIndex: 0 })).toBe(
      NO_PLAN,
    );
    expect(drillDownFamily(defaultFilterState(), { periodIndex: 0 })).toBe("");
  });
});

// --- Filter options ----------------------------------------------------

describe("optionsFrom", () => {
  it("renders the payload's dimensions as they come", () => {
    // The server counts each dimension with its own filter left out, so a
    // filtered payload already offers the siblings of what is selected.
    const narrowed = makeData();
    narrowed.dimensions.plans = [narrowed.dimensions.plans[0], narrowed.dimensions.plans[1]];
    const options = optionsFrom(narrowed, { ...defaultFilterState(), plan: "plan-a" });
    expect(options.plans.map((plan) => plan.planName)).toEqual(["plan-a", "plan-b"]);
    expect(Object.keys(options.variants)).toEqual(["client_auth_type", "fapi_profile"]);
    expect(options.certProfiles).toHaveLength(1);
  });

  it("keeps a filtered variant parameter the payload no longer mentions", () => {
    // Otherwise the only control that could clear it would disappear.
    const narrowed = makeData();
    narrowed.dimensions.variants = {};
    const options = optionsFrom(narrowed, {
      ...defaultFilterState(),
      variant: { fapi_profile: "openbanking_brazil" },
    });
    expect(options.variants).toEqual({ fapi_profile: [] });
  });

  it("tolerates a payload with no dimensions at all", () => {
    expect(optionsFrom(/** @type {any} */ ({}), defaultFilterState())).toEqual({
      plans: [],
      variants: {},
      certProfiles: [],
    });
    const malformed = /** @type {any} */ ({ dimensions: { plans: "no", variants: { x: "no" } } });
    expect(optionsFrom(malformed, defaultFilterState())).toEqual({
      plans: [],
      variants: { x: [] },
      certProfiles: [],
    });
  });
});

describe("optionsForTransition", () => {
  it("drops the variant lists of the old family and keeps the plan and profile lists", () => {
    const before = optionsFrom(makeData(), defaultFilterState());
    const during = optionsForTransition(before, { ...defaultFilterState(), family: "OID4VP" });
    expect(during.variants).toEqual({});
    expect(during.plans).toBe(before.plans);
    expect(during.certProfiles).toBe(before.certProfiles);
    // ...so nothing is offered until the payload for the new family lands
    expect(visibleVariants(during, { ...defaultFilterState(), family: "OID4VP" })).toEqual({});
  });

  it("keeps a parameter that is still filtered on, with its values, so it can be cleared", () => {
    const before = optionsFrom(makeData(), defaultFilterState());
    const during = optionsForTransition(before, {
      ...defaultFilterState(),
      plan: "plan-b",
      variant: { client_auth_type: "mtls" },
    });
    expect(Object.keys(during.variants)).toEqual(["client_auth_type"]);
    expect(during.variants.client_auth_type).toBe(before.variants.client_auth_type);
  });

  it("tolerates missing options", () => {
    expect(optionsForTransition(/** @type {any} */ (undefined), defaultFilterState())).toEqual({
      plans: [],
      variants: {},
      certProfiles: [],
    });
  });
});

describe("visibleVariants", () => {
  /** @returns {any} The options an unfiltered payload produces. */
  function options() {
    return optionsFrom(makeData(), defaultFilterState());
  }

  it("offers nothing until the view is narrowed to a family or a plan", () => {
    // The suite publishes dozens of plan-level variant parameters; a filter
    // row of forty selects is not a filter row.
    expect(visibleVariants(options(), defaultFilterState())).toEqual({});
  });

  it("offers the parameters with something to choose between once it is", () => {
    const withFamily = visibleVariants(options(), {
      ...defaultFilterState(),
      family: "FAPI2 Security Profile",
    });
    expect(Object.keys(withFamily)).toEqual(["client_auth_type"]);
    // fapi_profile has a single value here, so a select on it could only
    // filter out the cells that do not carry the parameter at all.
    expect(withFamily.client_auth_type).toHaveLength(2);
    expect(
      Object.keys(visibleVariants(options(), { ...defaultFilterState(), plan: "plan-a" })),
    ).toEqual(["client_auth_type"]);
  });

  it("always offers a parameter that is being filtered on", () => {
    // Otherwise the only control that could clear it would not be on screen.
    const visible = visibleVariants(options(), {
      ...defaultFilterState(),
      variant: { fapi_profile: "plain_fapi" },
    });
    expect(Object.keys(visible)).toEqual(["fapi_profile"]);
  });

  it("tolerates missing options and state", () => {
    expect(visibleVariants(EMPTY_OPTIONS, defaultFilterState())).toEqual({});
    expect(visibleVariants(/** @type {any} */ ({}), /** @type {any} */ ({}))).toEqual({});
  });
});

describe("memoiseByArgs", () => {
  it("returns the very same value while the arguments are identical", () => {
    let calls = 0;
    const memo = memoiseByArgs((/** @type {any} */ a, /** @type {any} */ b) => {
      calls += 1;
      return { a, b };
    });
    const one = {};
    const two = {};
    expect(memo(one, two)).toBe(memo(one, two));
    expect(calls).toBe(1);
  });

  it("recomputes when any argument is a different instance", () => {
    const memo = memoiseByArgs((/** @type {any} */ value) => ({ value }));
    const first = memo({});
    expect(memo({})).not.toBe(first);
    // Equal-but-not-identical is a different instance on purpose: identity is
    // exactly what <cts-chart> compares.
    expect(memo([1])).not.toBe(memo([1]));
  });

  it("recomputes when the number of arguments changes", () => {
    const memo = memoiseByArgs((/** @type {Array<any>} */ ...args) => args.length);
    expect(memo(1)).toBe(1);
    expect(memo(1, 2)).toBe(2);
    expect(memo(1)).toBe(1);
  });
});

describe("buildChartInputs", () => {
  it("builds all five charts from one payload", () => {
    const data = makeData();
    const inputs = buildChartInputs(data, assignFamilySlots(data), "");
    expect(inputs.granularity).toBe("month");
    expect(inputs.labels).toEqual(PERIODS);
    expect(inputs.runs.datasets.map((d) => d.label).at(-1)).toBe(OTHER_LABEL);
    expect(inputs.runs.folded).toBe(true);
    expect(inputs.plans.datasets.map((d) => d.label).at(-1)).toBe(OTHER_LABEL);
    expect(inputs.certified.datasets.map((d) => d.label)).toEqual([
      "FAPI2 Security Profile",
      "FAPI1 Advanced",
      OTHER_LABEL,
    ]);
    expect(inputs.results.datasets.map((d) => d.label)).toEqual([
      "PASSED",
      "REVIEW",
      "FAILED",
      "NEVER_FINISHED",
    ]);
    expect(inputs.users.datasets.map((d) => d.label)).toEqual(["Active", "New"]);
    expect(inputs.results.folded).toBe(false);
  });

  it("does not fold a single series, whatever colour it wears", () => {
    // Regression guard: the one series left by a family, plan or variant
    // filter may itself be a neutral-slot family, and folding would rename it
    // "Other" — losing the name the user just picked.
    const data = makeData();
    const inputs = buildChartInputs(data, assignFamilySlots(data), "OpenID Federation");
    expect(inputs.runs.datasets.map((d) => d.label)).toEqual(["OpenID Federation"]);
    expect(inputs.runs.folded).toBe(false);
  });

  it("labels a weekly payload as weeks", () => {
    const data = makeData();
    data.granularity = "week";
    data.periods = ["2025-12-29", "2026-01-05"];
    expect(buildChartInputs(data, {}, "").labels).toEqual(["29 Dec 2025", "5 Jan 2026"]);
  });
});

describe("hasAnyData", () => {
  it("is true for a payload with runs or plans anywhere in it", () => {
    expect(hasAnyData(makeData())).toBe(true);
  });

  it("is false when every family series is zero", () => {
    // What a filter that matches nothing looks like: the axis is the cube's,
    // so the periods are all still there and every cell is zero.
    const data = makeData();
    for (const family of FAMILIES) {
      data.testRunsByFamily[family] = series(0, 0);
      data.plansByFamily[family] = series(0, 0);
    }
    expect(data.periods).toHaveLength(MONTH_COUNT);
    expect(hasAnyData(data)).toBe(false);
    // Users are NOT consulted: they are counted per plan owner, and a plan
    // basis with no plans cannot be non-zero on its own.
    expect(hasAnyData(/** @type {any} */ ({ users: { activeByPeriod: [3] } }))).toBe(false);
  });

  it("counts plans even when nothing ran", () => {
    const data = makeData();
    for (const family of FAMILIES) data.testRunsByFamily[family] = series(0, 0);
    expect(hasAnyData(data)).toBe(true);
  });

  it("tolerates an empty payload", () => {
    expect(hasAnyData(/** @type {any} */ ({}))).toBe(false);
  });
});

// --- Colour slots and datasets -----------------------------------------

describe("assignFamilySlots", () => {
  it("hands the seven categorical slots to the seven busiest families, in rank order", () => {
    const slots = assignFamilySlots(makeData());
    expect(slots["FAPI2 Security Profile"]).toBe("--chart-cat-1");
    expect(slots["FAPI1 Advanced"]).toBe("--chart-cat-2");
    expect(slots["OpenID Connect Core"]).toBe("--chart-cat-3");
    expect(slots["OID4VP"]).toBe("--chart-cat-4");
    expect(slots["OID4VCI"]).toBe("--chart-cat-5");
    expect(slots["FAPI-CIBA"]).toBe("--chart-cat-6");
    expect(slots["eKYC & Identity Assurance"]).toBe("--chart-cat-7");
  });

  it("breaks ties by the payload's families order", () => {
    // Both FAPI families total 6000; FAPI2 is listed first, so it takes slot 1.
    const data = makeData();
    data.families = ["FAPI1 Advanced", "FAPI2 Security Profile", ...FAMILIES.slice(2)];
    const slots = assignFamilySlots(data);
    expect(slots["FAPI1 Advanced"]).toBe("--chart-cat-1");
    expect(slots["FAPI2 Security Profile"]).toBe("--chart-cat-2");
  });

  it("folds every other family — including No plan and Other / retired — into the neutral", () => {
    const slots = assignFamilySlots(makeData());
    expect(slots["OpenID Federation"]).toBe(OTHER_COLOR_VAR);
    expect(slots["Shared Signals Framework"]).toBe(OTHER_COLOR_VAR);
    expect(slots[NO_PLAN]).toBe(OTHER_COLOR_VAR);
    expect(slots[OTHER_RETIRED]).toBe(OTHER_COLOR_VAR);
  });

  it("never lets the synthetic buckets take a hue slot, however busy they are", () => {
    // "No plan" and "Other / retired" are "this run had no family", not
    // families: they must never spend one of the seven separable hues, even
    // when they dwarf every real family.
    const data = makeData();
    data.familyTotals[NO_PLAN].runs = 9_000_000;
    data.familyTotals[OTHER_RETIRED].runs = 8_000_000;
    const slots = assignFamilySlots(data);
    expect(slots[NO_PLAN]).toBe(OTHER_COLOR_VAR);
    expect(slots[OTHER_RETIRED]).toBe(OTHER_COLOR_VAR);
    // The seven hues still go to the seven busiest REAL families, unshifted.
    expect(slots["FAPI2 Security Profile"]).toBe("--chart-cat-1");
    expect(slots["eKYC & Identity Assurance"]).toBe("--chart-cat-7");
    expect(
      Object.values(slots)
        .filter((v) => v !== OTHER_COLOR_VAR)
        .sort(),
    ).toEqual([...CATEGORY_COLOR_VARS].sort());
  });

  it("promotes a real family into the slot a synthetic bucket would have taken", () => {
    // Drop one real family out of contention and make "No plan" the busiest
    // bucket in the payload; the freed hue must go to the next REAL family
    // (OpenID Federation), never to the synthetic bucket.
    const data = makeData();
    data.familyTotals["eKYC & Identity Assurance"].runs = 0;
    data.familyTotals[NO_PLAN].runs = 9_000_000;
    const slots = assignFamilySlots(data);
    expect(slots["OpenID Federation"]).toBe("--chart-cat-7");
    expect(slots[NO_PLAN]).toBe(OTHER_COLOR_VAR);
  });

  it("covers every family in the payload and uses each categorical slot once", () => {
    const slots = assignFamilySlots(makeData());
    expect(Object.keys(slots).sort()).toEqual([...FAMILIES].sort());
    const categorical = Object.values(slots).filter((v) => v !== OTHER_COLOR_VAR);
    expect(categorical.sort()).toEqual([...CATEGORY_COLOR_VARS].sort());
  });

  it("is read from familyTotals, so a range change never repaints", () => {
    // The last twelve months rank the families differently from the whole
    // history (OpenID Federation is busy only lately), and a payload for that
    // range still carries the all-time totals — so the slots do not move.
    const full = assignFamilySlots(makeData());
    const narrowed = assignFamilySlots(makeRecent());
    expect(narrowed).toEqual(full);
    expect(full["eKYC & Identity Assurance"]).toBe("--chart-cat-7");
    expect(full["OpenID Federation"]).toBe(OTHER_COLOR_VAR);
    // ...and it is the totals that rank, not the series: rank them the other
    // way and the slots follow.
    const rearranged = makeRecent();
    rearranged.familyTotals = withTotals({ ...makeRecent(), familyTotals: {} }).familyTotals;
    expect(assignFamilySlots(rearranged)["OpenID Federation"]).toBe("--chart-cat-7");
    expect(assignFamilySlots(rearranged)["eKYC & Identity Assurance"]).toBe(OTHER_COLOR_VAR);
  });

  it("is unmoved by a filter that zeroes every other family's series", () => {
    // Under a family filter every other family's SERIES is zero; the totals
    // are not, so the selected family keeps its own hue rather than taking
    // the first one.
    const filtered = makeData();
    for (const family of FAMILIES) {
      if (family !== "OID4VP") filtered.testRunsByFamily[family] = series(0, 0);
    }
    expect(assignFamilySlots(filtered)["OID4VP"]).toBe("--chart-cat-4");
  });

  it("tolerates a payload with no familyTotals at all", () => {
    const data = makeData();
    delete data.familyTotals;
    const slots = assignFamilySlots(data);
    expect(Object.keys(slots).sort()).toEqual([...FAMILIES].sort());
    // Nothing ranks, so the seven hues go to the first seven real families.
    expect(slots["FAPI2 Security Profile"]).toBe("--chart-cat-1");
  });

  it("tolerates a payload with no families", () => {
    expect(assignFamilySlots(/** @type {any} */ ({}))).toEqual({});
  });

  it("never throws on a payload whose arrays are not arrays", () => {
    // A contract-violating payload must degrade to an empty chart rather
    // than throw inside render(); the page reports it separately.
    const broken = /** @type {any} */ ({
      families: "boom",
      resultBuckets: 7,
      periods: null,
      testRunsByFamily: "nope",
    });
    expect(assignFamilySlots(broken)).toEqual({});
    expect(runsDatasets(broken, {}, "")).toEqual([]);
    expect(plansDatasets(broken, {}, "")).toEqual([]);
    expect(certifiedDatasets(broken, {}, "")).toEqual([]);
    expect(resultsDatasets(broken, "")).toEqual([]);
    expect(otherBreakdown(broken, {}, 0)).toEqual([]);
    expect(familiesWithActivity(broken)).toEqual([]);
    expect(periodLabels(broken.periods, "week")).toEqual([]);
  });
});

describe("runsDatasets / plansDatasets / certifiedDatasets", () => {
  it("emits one dataset per family with data, ordered by colour slot", () => {
    const data = makeData();
    const datasets = runsDatasets(data, assignFamilySlots(data), "");
    expect(datasets.map((d) => d.label)).toEqual([
      "FAPI2 Security Profile",
      "FAPI1 Advanced",
      "OpenID Connect Core",
      "OID4VP",
      "OID4VCI",
      "FAPI-CIBA",
      "eKYC & Identity Assurance",
      "OpenID Federation",
      "No plan",
      "Other / retired",
    ]);
    expect(datasets.map((d) => d.colorVar)).toEqual([
      ...CATEGORY_COLOR_VARS,
      OTHER_COLOR_VAR,
      OTHER_COLOR_VAR,
      OTHER_COLOR_VAR,
    ]);
    expect(datasets[0].data).toEqual(data.testRunsByFamily["FAPI2 Security Profile"]);
  });

  it("drops families whose series is all zero in the payload", () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    // Shared Signals Framework is zero across all time.
    expect(runsDatasets(data, slots, "").map((d) => d.label)).not.toContain(
      "Shared Signals Framework",
    );
    // eKYC is zero only in the last 12 months, which is what the server sends
    // back for that range.
    expect(runsDatasets(makeRecent(), slots, "").map((d) => d.label)).not.toContain(
      "eKYC & Identity Assurance",
    );
  });

  it("returns a single dataset in the family's own slot colour when one is selected", () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    expect(runsDatasets(data, slots, "OID4VP")).toEqual([
      { label: "OID4VP", data: data.testRunsByFamily["OID4VP"], colorVar: "--chart-cat-4" },
    ]);
    expect(runsDatasets(data, slots, "No plan")).toEqual([
      { label: "No plan", data: data.testRunsByFamily["No plan"], colorVar: OTHER_COLOR_VAR },
    ]);
  });

  it("keeps a selected family even when it has nothing in the payload", () => {
    const slots = assignFamilySlots(makeData());
    const datasets = runsDatasets(makeRecent(), slots, "eKYC & Identity Assurance");
    expect(datasets).toHaveLength(1);
    expect(datasets[0].data).toEqual(new Array(12).fill(0));
  });

  it("copies the series so a chart cannot write back into the payload", () => {
    const data = makeData();
    const datasets = runsDatasets(data, assignFamilySlots(data), "OID4VP");
    datasets[0].data[0] = -1;
    expect(data.testRunsByFamily["OID4VP"][0]).toBe(120);
  });

  it("plansDatasets reads plansByFamily with the same slots", () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    const datasets = plansDatasets(data, slots, "");
    expect(datasets.map((d) => d.label)).toEqual([
      "FAPI2 Security Profile",
      "FAPI1 Advanced",
      "OpenID Federation",
      "Other / retired",
    ]);
    expect(datasets[0].colorVar).toBe("--chart-cat-1");
    expect(datasets[2].colorVar).toBe(OTHER_COLOR_VAR);
  });

  it("certifiedDatasets reads certifiedByFamily with the same slots", () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    const datasets = certifiedDatasets(data, slots, "");
    expect(datasets.map((d) => d.label)).toEqual([
      "FAPI2 Security Profile",
      "FAPI1 Advanced",
      "OpenID Federation",
    ]);
    expect(datasets[0].colorVar).toBe("--chart-cat-1");
    expect(datasets[0].data[0]).toBe(3);
    expect(datasets[2].colorVar).toBe(OTHER_COLOR_VAR);
  });

  it("returns nothing for an unknown selected family", () => {
    const data = makeData();
    expect(runsDatasets(data, assignFamilySlots(data), "Nope")).toEqual([]);
  });
});

describe("foldOther", () => {
  it("sums every neutral-slot dataset into one trailing Other series", () => {
    const data = makeData();
    const folded = foldOther(runsDatasets(data, assignFamilySlots(data), ""));
    expect(folded.map((d) => d.label)).toEqual([
      "FAPI2 Security Profile",
      "FAPI1 Advanced",
      "OpenID Connect Core",
      "OID4VP",
      "OID4VCI",
      "FAPI-CIBA",
      "eKYC & Identity Assurance",
      OTHER_LABEL,
    ]);
    const other = folded[folded.length - 1];
    expect(other.colorVar).toBe(OTHER_COLOR_VAR);
    // Early month: Federation 0 + No plan 20 + Other/retired 10.
    expect(other.data[0]).toBe(30);
    // Late month: Federation 50 + No plan 20 + Other/retired 10.
    expect(other.data[29]).toBe(80);
  });

  it("returns the input untouched when nothing is in the neutral slot", () => {
    const datasets = [{ label: "A", data: [1], colorVar: "--chart-cat-1" }];
    expect(foldOther(datasets)).toBe(datasets);
  });

  it("folds a single neutral dataset too, so the legend key is always Other", () => {
    const folded = foldOther([
      { label: "A", data: [1, 2], colorVar: "--chart-cat-1" },
      { label: "B", data: [3, 4], colorVar: OTHER_COLOR_VAR },
    ]);
    expect(folded).toEqual([
      { label: "A", data: [1, 2], colorVar: "--chart-cat-1" },
      { label: OTHER_LABEL, data: [3, 4], colorVar: OTHER_COLOR_VAR },
    ]);
  });

  it("tolerates a non-array argument", () => {
    expect(foldOther(/** @type {any} */ (undefined))).toEqual([]);
  });
});

describe("otherBreakdown", () => {
  it("lists the folded families with a non-zero count, biggest first", () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    expect(otherBreakdown(data, slots, 29)).toEqual([
      "OpenID Federation: 50",
      "No plan: 20",
      "Other / retired: 10",
    ]);
  });

  it("omits families that are zero in the hovered period", () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    // Period 0: OpenID Federation is 0, Shared Signals Framework is always 0.
    expect(otherBreakdown(data, slots, 0)).toEqual(["No plan: 20", "Other / retired: 10"]);
  });

  it("never lists a family that owns a categorical slot", () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    const lines = otherBreakdown(data, slots, 0);
    expect(lines.some((line) => line.startsWith("FAPI2 Security Profile"))).toBe(false);
  });

  it('reads plansByFamily when asked for the "plans" source', () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    expect(otherBreakdown(data, slots, 29, "plans")).toEqual([
      "OpenID Federation: 5",
      "Other / retired: 1",
    ]);
  });

  it('reads certifiedByFamily when asked for the "certified" source', () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    expect(otherBreakdown(data, slots, 29, "certified")).toEqual(["OpenID Federation: 2"]);
  });

  it("indexes the payload it was given, not some wider one", () => {
    const slots = assignFamilySlots(makeData());
    // Index 0 of the 12-month payload is period 18 — the first "late" one.
    expect(otherBreakdown(makeRecent(), slots, 0)).toEqual([
      "OpenID Federation: 50",
      "No plan: 20",
      "Other / retired: 10",
    ]);
  });

  it("returns nothing for an out-of-range period index", () => {
    const data = makeData();
    expect(otherBreakdown(data, assignFamilySlots(data), 999)).toEqual([]);
  });
});

describe("resultsDatasets", () => {
  it("sums each bucket across every family when no family is selected", () => {
    const datasets = resultsDatasets(makeData(), "");
    expect(datasets.map((d) => d.label)).toEqual(["PASSED", "REVIEW", "FAILED", "NEVER_FINISHED"]);
    // PASSED = FAPI2 150 + FAPI1 100.
    expect(datasets[0].data[0]).toBe(250);
    expect(datasets[0].data).toHaveLength(MONTH_COUNT);
    // FAILED = 30 + 100.
    expect(datasets[2].data[0]).toBe(130);
  });

  it("keeps the payload's bucket order and maps each to its status token", () => {
    const datasets = resultsDatasets(makeData(), "");
    expect(datasets.map((d) => d.colorVar)).toEqual([
      RESULT_COLOR_VARS.PASSED,
      RESULT_COLOR_VARS.REVIEW,
      RESULT_COLOR_VARS.FAILED,
      RESULT_COLOR_VARS.NEVER_FINISHED,
    ]);
    expect(RESULT_COLOR_VARS).toEqual({
      PASSED: "--status-pass",
      WARNING: "--status-warning",
      REVIEW: "--status-review",
      FAILED: "--status-fail",
      SKIPPED: "--status-skipped",
      NEVER_FINISHED: "--ink-300",
    });
  });

  it("drops buckets that are zero everywhere in the payload", () => {
    const labels = resultsDatasets(makeData(), "").map((d) => d.label);
    expect(labels).not.toContain("WARNING");
    expect(labels).not.toContain("SKIPPED");
  });

  it("shows one family's own buckets when a family is selected", () => {
    const datasets = resultsDatasets(makeData(), "FAPI1 Advanced");
    expect(datasets.map((d) => d.label)).toEqual(["PASSED", "FAILED"]);
    expect(datasets[0].data[0]).toBe(100);
    expect(datasets[1].data[0]).toBe(100);
  });

  it("returns nothing for a family with no result rows", () => {
    expect(resultsDatasets(makeData(), "OID4VP")).toEqual([]);
  });

  it("is as long as the payload's own axis", () => {
    const datasets = resultsDatasets(makeRecent(), "");
    expect(datasets[0].data).toHaveLength(12);
  });
});

describe("usersDatasets", () => {
  it("emits Active and New in the first two categorical slots", () => {
    const data = makeData();
    expect(usersDatasets(data)).toEqual([
      { label: "Active", data: data.users.activeByPeriod, colorVar: "--chart-cat-1" },
      { label: "New", data: data.users.newByPeriod, colorVar: "--chart-cat-2" },
    ]);
  });

  it("keeps both series even when one is empty, and copies the arrays", () => {
    const datasets = usersDatasets(
      /** @type {any} */ ({ users: { activeByPeriod: [1, 2], newByPeriod: [] } }),
    );
    expect(datasets.map((d) => d.label)).toEqual(["Active", "New"]);
    expect(datasets[1].data).toEqual([]);
  });

  it("tolerates a payload with no users block", () => {
    expect(usersDatasets(/** @type {any} */ ({})).map((d) => d.data)).toEqual([[], []]);
  });
});

describe("familiesWithActivity", () => {
  it("lists families with runs, plans or certified plans, in the payload's order", () => {
    expect(familiesWithActivity(makeData())).toEqual([
      "FAPI2 Security Profile",
      "FAPI1 Advanced",
      "OpenID Connect Core",
      "OID4VP",
      "OID4VCI",
      "FAPI-CIBA",
      "eKYC & Identity Assurance",
      "OpenID Federation",
      "No plan",
      "Other / retired",
    ]);
  });

  it("offers a family with plans but zero runs", () => {
    // "Shared Signals Framework" is zero everywhere in the base fixture; give
    // it plans only, so this proves inclusion does not require a run.
    const data = makeData();
    data.familyTotals["Shared Signals Framework"].plans = 2;
    expect(familiesWithActivity(data)).toContain("Shared Signals Framework");
  });

  it("offers a family with only certified plans and no runs or plans", () => {
    const data = makeData();
    data.familyTotals["Shared Signals Framework"].certified = 1;
    expect(familiesWithActivity(data)).toContain("Shared Signals Framework");
  });

  it("does not offer a family that is zero everywhere", () => {
    // Runs, plans AND certified are all-zero for "Shared Signals Framework"
    // in the base fixture.
    expect(familiesWithActivity(makeData())).not.toContain("Shared Signals Framework");
  });

  it("reads familyTotals, so a narrowed payload loses no options", () => {
    // eKYC has no runs in the last twelve months; the select must not lose
    // it when the user narrows the range, and does not, because the totals
    // are all-time whatever the range.
    const recent = makeRecent();
    expect(sum(recent.testRunsByFamily["eKYC & Identity Assurance"])).toBe(0);
    expect(familiesWithActivity(recent)).toContain("eKYC & Identity Assurance");
  });

  it("tolerates an empty payload", () => {
    expect(familiesWithActivity(/** @type {any} */ ({}))).toEqual([]);
  });
});

// --- Distributions -----------------------------------------------------

describe("rankBy", () => {
  const valueOf = (/** @type {{n: number}} */ item) => item.n;

  it("ranks on the measure being plotted, not on the delivered order", () => {
    // The server ranks certification profiles and variant values by ONE of
    // the two counts they carry; a ranking taken off the other one has to be
    // re-sorted or the chart plots the wrong bars first.
    expect(rankBy([{ n: 1 }, { n: 9 }, { n: 5 }], valueOf).map((item) => item.n)).toEqual([
      9, 5, 1,
    ]);
  });

  it("breaks ties on the delivered order, so the ranking is stable", () => {
    const items = [
      { n: 4, id: "a" },
      { n: 4, id: "b" },
      { n: 4, id: "c" },
    ];
    expect(rankBy(items, valueOf).map((item) => item.id)).toEqual(["a", "b", "c"]);
  });

  it("keeps every row — the cut belongs to <cts-chart max-bars>", () => {
    const items = Array.from({ length: DISTRIBUTION_LIMIT + 3 }, (_, i) => ({ n: i }));
    expect(rankBy(items, valueOf).length).toBe(DISTRIBUTION_LIMIT + 3);
  });

  it("tolerates a missing list", () => {
    expect(rankBy(/** @type {any} */ (undefined), valueOf)).toEqual([]);
  });
});

describe("distributionDatasets", () => {
  const values = [
    { value: "discovery", users: 23, plans: 512 },
    { value: "static", users: 71, plans: 145 },
  ];
  const spec = {
    label: "value",
    value: "users",
    valueLabel: "Users",
    extra: "plans",
    extraLabel: "Plans",
  };

  it("builds one series, biggest first, in the single categorical hue", () => {
    const distribution = distributionDatasets(values, spec);
    expect(distribution.labels).toEqual(["static", "discovery"]);
    expect(distribution.datasets.length).toBe(1);
    expect(distribution.datasets[0]).toEqual({
      label: "Users",
      data: [71, 23],
      // Nominal categories: one series, one hue. Colouring each bar by its own
      // value would re-encode what the bar length already shows.
      colorVar: CATEGORY_COLOR_VARS[0],
    });
  });

  it("carries the second measure as a table-only column, in the same order", () => {
    expect(distributionDatasets(values, spec).extras).toEqual([
      { label: "Plans", data: [145, 512] },
    ]);
  });

  it("has no extra column when the spec names no second measure", () => {
    expect(
      distributionDatasets([{ entity: "Wallet", runs: 8 }], {
        label: "entity",
        value: "runs",
        valueLabel: "Runs",
      }).extras,
    ).toEqual([]);
  });

  it("keeps every row, plotted or not — the data table is not truncated", () => {
    const many = Array.from({ length: 20 }, (_, i) => ({ value: `v${i}`, users: i, plans: i }));
    const distribution = distributionDatasets(many, spec);
    expect(distribution.labels.length).toBe(20);
    expect(distribution.datasets[0].data.length).toBe(20);
    expect(distribution.extras[0].data.length).toBe(20);
    // ...and the leading 12, which are what <cts-chart max-bars> plots, are
    // the twelve biggest.
    expect(distribution.datasets[0].data.slice(0, 12)).toEqual([
      19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8,
    ]);
  });

  it("degrades to an empty chart on a missing list", () => {
    const distribution = distributionDatasets(/** @type {any} */ (undefined), spec);
    expect(distribution.labels).toEqual([]);
    expect(distribution.datasets[0].data).toEqual([]);
  });
});

describe("buildDistributions", () => {
  /** @type {any} */
  const dimensions = {
    plans: [],
    variants: {
      client_auth_type: [
        { value: "private_key_jwt", users: 88, plans: 640 },
        { value: "mtls", users: 61, plans: 410 },
      ],
      client_registration: [{ value: "dynamic_client", users: 9, plans: 34 }],
    },
    certProfiles: [{ name: "FAPI2 Security Profile Final", users: 31, plans: 120 }],
    entities: [{ entity: "Test a Wallet", runs: 8200 }],
  };

  it("charts one variant parameter per key with something to choose between", () => {
    const built = buildDistributions(dimensions);
    expect(built.variants.map((variant) => variant.key)).toEqual(["client_auth_type"]);
    expect(built.variants[0].distribution.labels).toEqual(["private_key_jwt", "mtls"]);
  });

  it("leaves out a variant parameter with a single value", () => {
    // One bar says only that everything used the one value it could have
    // used, which the filter row already says by not offering a choice.
    expect(buildDistributions(dimensions).variants.map((v) => v.key)).not.toContain(
      "client_registration",
    );
  });

  it("plots users for profiles and runs for entities", () => {
    const built = buildDistributions(dimensions);
    expect(built.certProfiles?.datasets[0].label).toBe("Users");
    expect(built.certProfiles?.extras[0].label).toBe("Plans");
    expect(built.entities?.datasets[0].label).toBe("Runs");
    expect(built.entities?.extras).toEqual([]);
  });

  it("returns null for a dimension with nothing in it, so no empty card renders", () => {
    const built = buildDistributions(
      /** @type {any} */ ({ plans: [], variants: {}, certProfiles: [], entities: [] }),
    );
    expect(built.variants).toEqual([]);
    expect(built.certProfiles).toBeNull();
    expect(built.entities).toBeNull();
  });

  it("tolerates a payload with no dimensions at all", () => {
    const built = buildDistributions(/** @type {any} */ (undefined));
    expect(built.variants).toEqual([]);
    expect(built.certProfiles).toBeNull();
  });
});

// --- Storage -----------------------------------------------------------

describe("formatShare", () => {
  it("is always one decimal, so a column of shares lines up", () => {
    expect(formatShare(0)).toBe("0.0%");
    expect(formatShare(1)).toBe("100.0%");
    expect(formatShare(0.237)).toBe("23.7%");
    expect(formatShare(0.5)).toBe("50.0%");
  });

  it("rounds the server's third decimal away rather than showing noise", () => {
    expect(formatShare(0.759)).toBe("75.9%");
    expect(formatShare(0.105)).toBe("10.5%");
    expect(formatShare(0.563)).toBe("56.3%");
  });

  it("clamps to the 0–1 the server promises", () => {
    expect(formatShare(1.4)).toBe("100.0%");
    expect(formatShare(-0.2)).toBe("0.0%");
  });

  it("says nothing rather than NaN% when there is no number", () => {
    expect(formatShare(/** @type {any} */ (undefined))).toBe("—");
    expect(formatShare(/** @type {any} */ ("x"))).toBe("—");
  });
});

describe("moduleDatasets", () => {
  /**
   * `data.modules.rows`: the table, sorted by runs. The rankings are the
   * server's and arrive separately; the rows' own order says nothing about
   * which chart plots what.
   * @type {Array<any>}
   */
  const rows = [
    { testName: "b-most-run", runs: 900, users: 30, failingUsers: 3, failingShare: 0.1 },
    { testName: "e-busy-failer", runs: 700, users: 10, failingUsers: 8, failingShare: 0.8 },
    { testName: "a-worst", runs: 400, users: 8, failingUsers: 8, failingShare: 1 },
    { testName: "c-clean", runs: 400, users: 20, failingUsers: 0, failingShare: 0 },
    { testName: "d-middling", runs: 400, users: 9, failingUsers: 8, failingShare: 0.889 },
  ];
  const byRuns = ["b-most-run", "e-busy-failer", "a-worst", "c-clean", "d-middling"];
  const byFailingUsers = ["e-busy-failer", "a-worst", "d-middling", "b-most-run", "c-clean"];

  it("plots the ranking's names in the ranking's order, as one series in the single hue", () => {
    const chart = moduleDatasets(rows, byRuns, "runs");
    expect(chart.labels).toEqual(byRuns);
    expect(chart.datasets.length).toBe(1);
    expect(chart.datasets[0]).toEqual({
      label: "Runs",
      data: [900, 700, 400, 400, 400],
      colorVar: CATEGORY_COLOR_VARS[0],
    });
  });

  it("trusts the server's failing-users order rather than re-sorting the rows", () => {
    const chart = moduleDatasets(rows, byFailingUsers, "failingUsers");
    expect(chart.labels).toEqual(byFailingUsers);
    expect(chart.datasets[0].label).toBe("Users who hit a failure");
    expect(chart.datasets[0].data).toEqual([8, 8, 8, 3, 0]);
    // Whatever order the server sends is the order plotted: the client has no
    // comparator of its own to disagree with it.
    expect(moduleDatasets(rows, ["c-clean", "a-worst"], "failingUsers").labels).toEqual([
      "c-clean",
      "a-worst",
    ]);
  });

  it("skips a ranked name the rows do not carry rather than plotting a zero", () => {
    const chart = moduleDatasets(rows, ["b-most-run", "gone-module", "a-worst"], "runs");
    expect(chart.labels).toEqual(["b-most-run", "a-worst"]);
    expect(chart.datasets[0].data).toEqual([900, 400]);
  });

  it("puts the measures it does not plot in the tooltip footer", () => {
    const runs = moduleDatasets(rows, byRuns, "runs");
    expect(runs.footers[0]).toEqual(["30 users", "3 hit a failure (10.0%)"]);
    // A module nobody failed still says so, rather than leaving the reader to
    // infer it from a chart it is not on.
    expect(runs.footers[3]).toEqual(["20 users", "0 hit a failure (0.0%)"]);

    const failing = moduleDatasets(rows, byFailingUsers, "failingUsers");
    // The denominator is the point: 8 of 10 is a module with a problem.
    expect(failing.footers[0]).toEqual(["8 of 10 users (80.0%)", "700 runs"]);
    expect(failing.footers[1]).toEqual(["8 of 8 users (100.0%)", "400 runs"]);
    expect(failing.footers[3]).toEqual(["3 of 30 users (10.0%)", "900 runs"]);
  });

  it("keeps the footer singular for one user or one run", () => {
    const chart = moduleDatasets(
      [{ testName: "solo", runs: 1, users: 1, failingUsers: 1, failingShare: 1 }],
      ["solo"],
      "failingUsers",
    );
    expect(chart.footers[0]).toEqual(["1 of 1 user (100.0%)", "1 run"]);
  });

  it("plots at most twelve bars — the tail belongs to the section's table", () => {
    const many = Array.from({ length: 15 }, (_, i) => ({
      testName: `module-${String(i).padStart(2, "0")}`,
      runs: 100 - i,
      users: 10,
      failingUsers: i,
      failingShare: i / 10,
    }));
    const names = many.map((row) => row.testName);
    const chart = moduleDatasets(many, names, "runs");
    expect(chart.labels.length).toBe(12);
    expect(chart.datasets[0].data.length).toBe(12);
    expect(chart.footers.length).toBe(12);
    expect(chart.labels[0]).toBe("module-00");
    expect(chart.labels[11]).toBe("module-11");
    // ...and the other ranking keeps a different twelve.
    expect(moduleDatasets(many, [...names].reverse(), "failingUsers").labels[0]).toBe("module-14");
  });

  it("honours an explicit limit, including zero", () => {
    expect(moduleDatasets(rows, byRuns, "runs", 2).labels).toEqual(["b-most-run", "e-busy-failer"]);
    expect(moduleDatasets(rows, byRuns, "runs", 0).labels).toEqual([]);
    expect(moduleDatasets(rows, byRuns, "runs", -3).labels).toEqual([]);
  });

  it("falls back to runs for an unknown metric rather than plotting nothing", () => {
    expect(moduleDatasets(rows, byRuns, "nonsense").datasets[0].label).toBe("Runs");
  });

  it("degrades to an empty chart on a missing or malformed list", () => {
    for (const input of [undefined, null, "not a list"]) {
      const chart = moduleDatasets(/** @type {any} */ (input), byRuns, "runs");
      expect(chart.labels).toEqual([]);
      expect(chart.datasets[0].data).toEqual([]);
      expect(chart.footers).toEqual([]);
      expect(moduleDatasets(rows, /** @type {any} */ (input), "runs").labels).toEqual([]);
    }
    // A row with nothing in it must not throw inside render().
    const chart = moduleDatasets([/** @type {any} */ ({})], [""], "runs");
    expect(chart.labels).toEqual([""]);
    expect(chart.datasets[0].data).toEqual([0]);
    expect(chart.footers[0]).toEqual(["No identified users"]);
  });

  it("does not reorder the caller's arrays", () => {
    const delivered = [...rows];
    const ranking = [...byFailingUsers];
    moduleDatasets(delivered, ranking, "failingUsers");
    expect(delivered.map((row) => row.testName)).toEqual(rows.map((row) => row.testName));
    expect(ranking).toEqual(byFailingUsers);
  });
});

describe("buildModules", () => {
  /** @type {any} */
  const modules = {
    rows: [
      { testName: "b-most-run", runs: 900, users: 30, failingUsers: 3, failingShare: 0.1 },
      { testName: "a-worst", runs: 400, users: 8, failingUsers: 8, failingShare: 1 },
    ],
    byRuns: ["b-most-run", "a-worst"],
    byFailingUsers: ["a-worst", "b-most-run"],
  };

  it("builds both charts from the server's rankings and hands the table the rows", () => {
    const built = buildModules(modules);
    expect(built.rows).toBe(modules.rows);
    expect(built.byRuns.labels).toEqual(["b-most-run", "a-worst"]);
    expect(built.byFailingUsers.labels).toEqual(["a-worst", "b-most-run"]);
  });

  it("is empty, not absent, when the window has no modules", () => {
    // The section tells the reader there is nothing here rather than
    // vanishing; that needs an object with no rows, not a null.
    const built = buildModules({ rows: [], byRuns: [], byFailingUsers: [] });
    expect(built.rows).toEqual([]);
    expect(built.byRuns.labels).toEqual([]);
    expect(built.byFailingUsers.datasets[0].data).toEqual([]);
  });

  it("survives a payload with no modules field at all", () => {
    expect(buildModules(/** @type {any} */ (undefined)).rows).toEqual([]);
    expect(buildModules(/** @type {any} */ ([])).byRuns.labels).toEqual([]);
  });
});

describe("formatBytes", () => {
  it("steps through the binary units with one decimal", () => {
    expect(formatBytes(0)).toBe("0 B");
    expect(formatBytes(820)).toBe("820 B");
    expect(formatBytes(24_000_000)).toBe("22.9 MB");
    expect(formatBytes(1_820_000_000)).toBe("1.7 GB");
    expect(formatBytes(18_400_000_000)).toBe("17.1 GB");
  });

  it("rounds bytes to whole numbers — a tenth of a byte is not a thing", () => {
    expect(formatBytes(1023.6)).toBe("1024 B");
    expect(formatBytes(1024)).toBe("1.0 KB");
  });

  it("stops at petabytes rather than inventing a unit", () => {
    expect(formatBytes(1024 ** 6)).toBe("1024.0 PB");
  });

  it("treats junk as nothing rather than rendering NaN in a tile", () => {
    expect(formatBytes(/** @type {any} */ ("nonsense"))).toBe("0 B");
    expect(formatBytes(/** @type {any} */ (null))).toBe("0 B");
    expect(formatBytes(-5)).toBe("0 B");
  });
});

// --- Heatmap -----------------------------------------------------------

describe("heatmapMax / heatmapTotal", () => {
  const grid = [
    [0, 3, 9],
    [1, 0, 2],
  ];

  it("finds the busiest cell and the sum of every cell", () => {
    expect(heatmapMax(grid)).toBe(9);
    expect(heatmapTotal(grid)).toBe(15);
  });

  it("reports 0 for an empty or all-zero grid", () => {
    expect(heatmapMax([])).toBe(0);
    expect(heatmapMax([[0, 0]])).toBe(0);
    expect(heatmapTotal(/** @type {any} */ (undefined))).toBe(0);
  });

  it("tolerates a ragged or malformed grid", () => {
    expect(heatmapMax(/** @type {any} */ ([[1], null, [4, 2]]))).toBe(4);
  });
});

describe("heatmapIntensity", () => {
  it("is 0 only for an empty cell, so a quiet hour is still visible", () => {
    expect(heatmapIntensity(0, 100)).toBe(0);
    expect(heatmapIntensity(1, 100)).toBeGreaterThanOrEqual(10);
  });

  it("puts the busiest cell at the dark end", () => {
    expect(heatmapIntensity(100, 100)).toBe(100);
  });

  it("is square-root scaled, so a skewed grid keeps its low end apart", () => {
    // Linear would put a cell at 4% of the peak at 4% of the ramp —
    // indistinguishable from the surface. The sqrt ramp gives it a fifth of
    // the range.
    expect(heatmapIntensity(4, 100)).toBe(28);
    expect(heatmapIntensity(25, 100)).toBe(55);
  });

  it("never decreases as the value grows", () => {
    let previous = -1;
    for (let value = 0; value <= 200; value += 1) {
      const intensity = heatmapIntensity(value, 200);
      expect(intensity).toBeGreaterThanOrEqual(previous);
      previous = intensity;
    }
  });

  it("is 0 when there is no scale to be on", () => {
    expect(heatmapIntensity(5, 0)).toBe(0);
    expect(heatmapIntensity(/** @type {any} */ ("x"), 10)).toBe(0);
  });
});

describe("heatmapScaleSteps", () => {
  it("labels each swatch with the value it actually stands for", () => {
    // The inverse of the sqrt ramp: a swatch a quarter of the way along stands
    // for a SIXTEENTH of the peak, not a quarter of it. Labelling the ends
    // only would leave the reader assuming the latter.
    expect(heatmapScaleSteps(1600)).toEqual([
      { mix: 33, value: 100 },
      { mix: 55, value: 400 },
      { mix: 78, value: 900 },
      { mix: 100, value: 1600 },
    ]);
  });

  it("agrees with heatmapIntensity, which is what makes the legend honest", () => {
    for (const step of heatmapScaleSteps(1600)) {
      expect(heatmapIntensity(step.value, 1600)).toBe(step.mix);
    }
  });

  it("has no steps when there is no scale to show", () => {
    expect(heatmapScaleSteps(0)).toEqual([]);
    expect(heatmapScaleSteps(/** @type {any} */ (undefined))).toEqual([]);
  });

  it("never labels a swatch 0, because the legend already has a 0 swatch", () => {
    // f² × max rounds away on a small scale: a quarter of the way along a
    // ramp topping out at 7 is 0.44 runs. A step labelled "0" next to the
    // muted "no runs" swatch says the ramp starts at nothing.
    for (const max of [1, 2, 3, 5, 7, 8]) {
      for (const step of heatmapScaleSteps(max)) expect(step.value).toBeGreaterThanOrEqual(1);
    }
  });

  it("collapses steps that would carry the same label onto the darkest of them", () => {
    // One run is all there is: one swatch, and it is the colour a cell with
    // one run actually gets.
    expect(heatmapScaleSteps(1)).toEqual([{ mix: 100, value: 1 }]);
    expect(heatmapScaleSteps(5)).toEqual([
      { mix: 55, value: 1 },
      { mix: 78, value: 3 },
      { mix: 100, value: 5 },
    ]);
    // Four distinct labels again from a peak of 7 upwards.
    expect(heatmapScaleSteps(7)).toEqual([
      { mix: 33, value: 1 },
      { mix: 55, value: 2 },
      { mix: 78, value: 4 },
      { mix: 100, value: 7 },
    ]);
    expect(heatmapScaleSteps(8)).toEqual([
      { mix: 33, value: 1 },
      { mix: 55, value: 2 },
      { mix: 78, value: 5 },
      { mix: 100, value: 8 },
    ]);
  });

  it("always samples the top of the ramp, and never paler than the cells it stands for", () => {
    for (const max of [1, 2, 5, 7, 8, 16, 1600]) {
      const steps = heatmapScaleSteps(max);
      const darkest = steps[steps.length - 1];
      const labels = steps.map((step) => step.value);
      expect(labels.at(-1)).toBe(max);
      expect(darkest.mix).toBe(100);
      // strictly increasing, so no two swatches say the same thing
      expect([...labels].sort((a, b) => a - b)).toEqual(labels);
      expect(new Set(labels).size).toBe(labels.length);
      // and the ramp itself still darkens step by step
      const mixes = steps.map((step) => step.mix);
      expect([...mixes].sort((a, b) => a - b)).toEqual(mixes);
      // The exact inverse of heatmapIntensity only survives while the labels
      // are not rounded: on a small scale a swatch can be a step off the shade
      // a cell of that count gets, which is why the label carries the count.
      expect(heatmapIntensity(darkest.value, max)).toBe(darkest.mix);
    }
  });
});
