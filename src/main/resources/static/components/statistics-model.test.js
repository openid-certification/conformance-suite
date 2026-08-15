import { describe, it, expect } from "vitest";
import {
  CATEGORY_COLOR_VARS,
  NO_PLAN_FAMILY,
  OTHER_COLOR_VAR,
  OTHER_RETIRED_FAMILY,
  OTHER_LABEL,
  RESULT_COLOR_VARS,
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

// --- Fixture -----------------------------------------------------------
//
// 30 months split 18 "early" + 12 "late" so the last-12 slice can be given a
// deliberately DIFFERENT family ranking from the all-time one. That is the
// only way to prove the slot assignment is computed from the full payload
// (a range change must never repaint the charts).
//
// All-time totals (per family, 18*early + 12*late):
//   FAPI2 Security Profile 6000 | FAPI1 Advanced 6000 | OpenID Connect Core 4500
//   OID4VP 3600 | OID4VCI 3000 | FAPI-CIBA 2400 | eKYC & Identity Assurance 1800
//   -> those seven are the top seven, in that order (the first two tie and are
//      broken by `families` order).
//   OpenID Federation 600 | No plan 600 | Other / retired 300 | SSF 0 -> tail.
//
// Last-12 totals: eKYC drops to 0 and OpenID Federation climbs to 600, so a
// slice-derived ranking would hand eKYC's colour to OpenID Federation.

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

const MONTHS = Array.from({ length: MONTH_COUNT }, (_, i) => {
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
 * The full `data` payload the endpoint returns, rebuilt per test so a helper
 * that mutates its input is caught by the next test rather than shared.
 * @returns {any} A statistics overview payload.
 */
function makeData() {
  return {
    families: [...FAMILIES],
    resultBuckets: [...RESULT_BUCKETS],
    months: [...MONTHS],
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
      activeByMonth: series(5, 9),
      newByMonth: series(1, 2),
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
    },
    unresolvedPlans: [{ planName: "fapi-rw-id2", runs: 300 }],
  };
}

describe("sliceRange", () => {
  it('returns the same object for "all" (no copying, no repaint)', () => {
    const data = makeData();
    expect(sliceRange(data, "all")).toBe(data);
  });

  it("returns the same object when the range is longer than the history", () => {
    const data = makeData();
    data.months = MONTHS.slice(0, 8);
    expect(sliceRange(data, "12m")).toBe(data);
  });

  it('keeps only the last 12 months of every aligned array for "12m"', () => {
    const sliced = sliceRange(makeData(), "12m");
    expect(sliced.months).toHaveLength(12);
    expect(sliced.months[0]).toBe(MONTHS[18]);
    expect(sliced.months[11]).toBe(MONTHS[29]);
    expect(sliced.testRunsByFamily["eKYC & Identity Assurance"]).toEqual(new Array(12).fill(0));
    expect(sliced.testRunsByFamily["OpenID Federation"]).toEqual(new Array(12).fill(50));
    expect(sliced.plansByFamily["FAPI2 Security Profile"]).toEqual(new Array(12).fill(20));
    expect(sliced.resultsByFamily["FAPI2 Security Profile"].PASSED).toEqual(
      new Array(12).fill(150),
    );
    expect(sliced.users.activeByMonth).toEqual(new Array(12).fill(9));
    expect(sliced.users.newByMonth).toEqual(new Array(12).fill(2));
  });

  it('keeps the last 24 months for "24m"', () => {
    const sliced = sliceRange(makeData(), "24m");
    expect(sliced.months).toHaveLength(24);
    expect(sliced.months[0]).toBe(MONTHS[6]);
    expect(sliced.testRunsByFamily["eKYC & Identity Assurance"]).toEqual([
      ...new Array(12).fill(100),
      ...new Array(12).fill(0),
    ]);
  });

  it("leaves the unfiltered fields (tiles, families, buckets) untouched", () => {
    const data = makeData();
    const sliced = sliceRange(data, "12m");
    expect(sliced.tiles).toBe(data.tiles);
    expect(sliced.families).toBe(data.families);
    expect(sliced.resultBuckets).toBe(data.resultBuckets);
    expect(sliced.unresolvedPlans).toBe(data.unresolvedPlans);
  });

  it("does not mutate the source payload", () => {
    const data = makeData();
    sliceRange(data, "12m");
    expect(data.months).toHaveLength(MONTH_COUNT);
    expect(data.testRunsByFamily["OpenID Federation"]).toHaveLength(MONTH_COUNT);
    expect(data.users.activeByMonth).toHaveLength(MONTH_COUNT);
  });

  it("tolerates an unknown range and a payload with no months", () => {
    const data = makeData();
    expect(sliceRange(data, "nonsense")).toBe(data);
    const monthless = /** @type {any} */ ({ months: [] });
    expect(sliceRange(monthless, "12m")).toBe(monthless);
  });
});

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
    expect(slots[NO_PLAN_FAMILY]).toBe(OTHER_COLOR_VAR);
    expect(slots[OTHER_RETIRED_FAMILY]).toBe(OTHER_COLOR_VAR);
  });

  it("never lets the synthetic buckets take a hue slot, however busy they are", () => {
    // "No plan" and "Other / retired" are "this run had no family", not
    // families: they must never spend one of the seven separable hues, even
    // when they dwarf every real family.
    const data = makeData();
    data.testRunsByFamily[NO_PLAN_FAMILY] = series(9000, 9000);
    data.testRunsByFamily[OTHER_RETIRED_FAMILY] = series(8000, 8000);
    const slots = assignFamilySlots(data);
    expect(slots[NO_PLAN_FAMILY]).toBe(OTHER_COLOR_VAR);
    expect(slots[OTHER_RETIRED_FAMILY]).toBe(OTHER_COLOR_VAR);
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
    data.testRunsByFamily["eKYC & Identity Assurance"] = series(0, 0);
    data.testRunsByFamily[NO_PLAN_FAMILY] = series(9000, 9000);
    const slots = assignFamilySlots(data);
    expect(slots["OpenID Federation"]).toBe("--chart-cat-7");
    expect(slots[NO_PLAN_FAMILY]).toBe(OTHER_COLOR_VAR);
  });

  it("covers every family in the payload and uses each categorical slot once", () => {
    const slots = assignFamilySlots(makeData());
    expect(Object.keys(slots).sort()).toEqual([...FAMILIES].sort());
    const categorical = Object.values(slots).filter((v) => v !== OTHER_COLOR_VAR);
    expect(categorical.sort()).toEqual([...CATEGORY_COLOR_VARS].sort());
  });

  it("is computed from the full payload, so a range change never repaints", () => {
    const data = makeData();
    const full = assignFamilySlots(data);
    // The full-payload answer genuinely differs from what the slice alone
    // would produce, so this test fails the day a caller passes the slice.
    const sliceDerived = assignFamilySlots(sliceRange(data, "12m"));
    expect(sliceDerived["OpenID Federation"]).toBe("--chart-cat-7");
    expect(sliceDerived["eKYC & Identity Assurance"]).toBe(OTHER_COLOR_VAR);
    expect(full["OpenID Federation"]).toBe(OTHER_COLOR_VAR);
    expect(full["eKYC & Identity Assurance"]).toBe("--chart-cat-7");
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
      months: null,
      testRunsByFamily: "nope",
    });
    expect(assignFamilySlots(broken)).toEqual({});
    expect(runsDatasets(broken, {}, "")).toEqual([]);
    expect(plansDatasets(broken, {}, "")).toEqual([]);
    expect(resultsDatasets(broken, "")).toEqual([]);
    expect(otherBreakdown(broken, {}, 0)).toEqual([]);
    expect(familiesWithRuns(broken)).toEqual([]);
  });
});

describe("runsDatasets / plansDatasets", () => {
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

  it("drops families whose series is all zero in the current slice", () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    // Shared Signals Framework is zero across all time.
    expect(runsDatasets(data, slots, "").map((d) => d.label)).not.toContain(
      "Shared Signals Framework",
    );
    // eKYC is zero only in the last 12 months.
    const sliced = sliceRange(data, "12m");
    expect(runsDatasets(sliced, slots, "").map((d) => d.label)).not.toContain(
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

  it("keeps a selected family even when it has no runs in the slice", () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    const sliced = sliceRange(data, "12m");
    const datasets = runsDatasets(sliced, slots, "eKYC & Identity Assurance");
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

  it("omits families that are zero in the hovered month", () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    // Month 0: OpenID Federation is 0, Shared Signals Framework is always 0.
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

  it("reads the sliced payload's month index, not the full one", () => {
    const data = makeData();
    const slots = assignFamilySlots(data);
    const sliced = sliceRange(data, "12m");
    // Index 0 of the slice is month 18 — the first "late" month.
    expect(otherBreakdown(sliced, slots, 0)).toEqual([
      "OpenID Federation: 50",
      "No plan: 20",
      "Other / retired: 10",
    ]);
  });

  it("returns nothing for an out-of-range month index", () => {
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

  it("drops buckets that are zero everywhere in the slice", () => {
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

  it("respects the slice", () => {
    const datasets = resultsDatasets(sliceRange(makeData(), "12m"), "");
    expect(datasets[0].data).toHaveLength(12);
  });
});

describe("usersDatasets", () => {
  it("emits Active and New in the first two categorical slots", () => {
    const data = makeData();
    expect(usersDatasets(data)).toEqual([
      { label: "Active", data: data.users.activeByMonth, colorVar: "--chart-cat-1" },
      { label: "New", data: data.users.newByMonth, colorVar: "--chart-cat-2" },
    ]);
  });

  it("keeps both series even when one is empty, and copies the arrays", () => {
    const datasets = usersDatasets(
      /** @type {any} */ ({ users: { activeByMonth: [1, 2], newByMonth: [] } }),
    );
    expect(datasets.map((d) => d.label)).toEqual(["Active", "New"]);
    expect(datasets[1].data).toEqual([]);
  });

  it("tolerates a payload with no users block", () => {
    expect(usersDatasets(/** @type {any} */ ({})).map((d) => d.data)).toEqual([[], []]);
  });
});

describe("familiesWithRuns", () => {
  it("lists only families with all-time runs, in the payload's order", () => {
    expect(familiesWithRuns(makeData())).toEqual([
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

  it("is meant for the full payload: a family idle in the recent slice still appears", () => {
    // The select must not lose options when the user narrows the range,
    // so the caller passes the full payload — proven by the contrast.
    const data = makeData();
    expect(familiesWithRuns(data)).toContain("eKYC & Identity Assurance");
    expect(familiesWithRuns(sliceRange(data, "12m"))).not.toContain("eKYC & Identity Assurance");
  });

  it("tolerates an empty payload", () => {
    expect(familiesWithRuns(/** @type {any} */ ({}))).toEqual([]);
  });
});
