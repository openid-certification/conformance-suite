/**
 * Mock payloads for `GET /api/statistics/overview` (the admin-only endpoint
 * behind `statistics.html`), matching
 * `net.openid.conformance.statistics.StatisticsResponse` /
 * `StatisticsOverview`.
 *
 * A deliberate self-contained twin of `frontend/stories/fixtures/mock-statistics.js`:
 * e2e fixtures never reach across into the Storybook fixture tree, so the two
 * files are kept in sync by hand. Keep the numbers identical — the e2e
 * assertions and the Storybook play functions quote the same figures, so a
 * drift in one shows up as a puzzling mismatch in the other.
 *
 * The READY payload is 14 months of synthetic traffic across eight spec
 * families plus the two synthetic buckets ("No plan", "Other / retired") and
 * one family that never ran. It is shaped so every behaviour the page has is
 * reachable from it:
 *
 * - more families with data (10) than categorical colour slots (7), so the
 *   neutral tail folds into a single "Other" series;
 * - a family busy only in the first two months ("OpenID Connect Logout"), so
 *   narrowing the range to 12 months drops a series from the charts;
 * - every result bucket populated somewhere, including NEVER_FINISHED;
 * - a family with runs but no plans ("No plan" — standalone modules);
 * - a family with no runs at all ("Shared Signals Framework"), which must not
 *   be offered in the family filter.
 */

const MONTH_COUNT = 14;

/**
 * The 14 contiguous `YYYY-MM` keys ending 2026-06, the month of `computedAt`.
 * @type {Array<string>}
 */
export const MOCK_STATS_MONTHS = [
  "2025-05",
  "2025-06",
  "2025-07",
  "2025-08",
  "2025-09",
  "2025-10",
  "2025-11",
  "2025-12",
  "2026-01",
  "2026-02",
  "2026-03",
  "2026-04",
  "2026-05",
  "2026-06",
];

/**
 * The families this fixture carries. Deliberately NOT the order the server
 * emits (`SpecFamilyResolver.familyOrder()`): the page renders whatever order
 * `data.families` gives it, so an arbitrary order here proves it follows the
 * payload rather than a list of its own.
 */
const FAMILIES = [
  "FAPI2 Security Profile",
  "FAPI1 Advanced",
  "OpenID Connect Core",
  "OID4VP",
  "OID4VCI",
  "FAPI-CIBA",
  "OpenID Federation",
  "OpenID Connect Logout",
  "Shared Signals Framework",
  "No plan",
  "Other / retired",
];

const RESULT_BUCKETS = ["PASSED", "WARNING", "REVIEW", "FAILED", "SKIPPED", "NEVER_FINISHED"];

/**
 * Build a 14-entry monthly series from a base value with a deterministic
 * wobble, so the bars are not a flat block but the numbers stay predictable
 * (`base + step * i + (i % 3)`).
 * @param {number} base - Value in the first month.
 * @param {number} step - Per-month growth.
 * @param {number} [from] - First month index that has any traffic.
 * @param {number} [to] - Last month index (inclusive) that has traffic.
 * @returns {Array<number>} The series.
 */
function ramp(base, step, from = 0, to = MONTH_COUNT - 1) {
  return Array.from({ length: MONTH_COUNT }, (_, i) => {
    // A zero base means "this family never ran"; the wobble must not
    // quietly resurrect it.
    if (base === 0 || i < from || i > to) return 0;
    return base + step * i + (i % 3);
  });
}

/** @type {Record<string, Array<number>>} */
const TEST_RUNS_BY_FAMILY = {
  "FAPI2 Security Profile": ramp(180, 24),
  "FAPI1 Advanced": ramp(150, 6),
  "OpenID Connect Core": ramp(120, 4),
  OID4VP: ramp(30, 18),
  OID4VCI: ramp(20, 14),
  "FAPI-CIBA": ramp(40, 2),
  // Ranks eighth all-time, so it wears the neutral and folds into "Other".
  "OpenID Federation": ramp(10, 3),
  // Busy for the first two months, then retired: a 12-month range drops it.
  "OpenID Connect Logout": ramp(700, 0, 0, 1),
  "Shared Signals Framework": ramp(0, 0),
  "No plan": ramp(25, 1),
  "Other / retired": ramp(12, 0),
};

/** @type {Record<string, Array<number>>} */
const PLANS_BY_FAMILY = {
  "FAPI2 Security Profile": ramp(12, 2),
  "FAPI1 Advanced": ramp(9, 0),
  "OpenID Connect Core": ramp(7, 0),
  OID4VP: ramp(2, 1),
  OID4VCI: ramp(1, 1),
  "FAPI-CIBA": ramp(3, 0),
  "OpenID Federation": ramp(1, 0),
  "OpenID Connect Logout": ramp(4, 0, 0, 1),
  "Shared Signals Framework": ramp(0, 0),
  // Standalone runs never belong to a plan.
  "No plan": ramp(0, 0),
  "Other / retired": ramp(1, 0),
};

/**
 * Split a family's runs into result buckets with a fixed, plausible mix. The
 * residual lands in NEVER_FINISHED, exactly as the server-side assembler
 * computes it.
 * @param {Array<number>} runs - The family's runs per month.
 * @returns {Record<string, Array<number>>} Bucket → runs per month.
 */
function buckets(runs) {
  const share = (/** @type {number} */ fraction) =>
    runs.map((value) => Math.round(value * fraction));
  const passed = share(0.62);
  const warning = share(0.11);
  const review = share(0.04);
  const failed = share(0.15);
  const skipped = share(0.03);
  return {
    PASSED: passed,
    WARNING: warning,
    REVIEW: review,
    FAILED: failed,
    SKIPPED: skipped,
    NEVER_FINISHED: runs.map((value, i) =>
      Math.max(0, value - passed[i] - warning[i] - review[i] - failed[i] - skipped[i]),
    ),
  };
}

/** @type {Record<string, Record<string, Array<number>>>} */
const RESULTS_BY_FAMILY = Object.fromEntries(
  FAMILIES.map((family) => [family, buckets(TEST_RUNS_BY_FAMILY[family])]),
);

/**
 * The snapshot itself — the `data` half of a READY response.
 * @type {any}
 */
export const MOCK_STATS_DATA = {
  families: FAMILIES,
  resultBuckets: RESULT_BUCKETS,
  months: MOCK_STATS_MONTHS,
  testRunsByFamily: TEST_RUNS_BY_FAMILY,
  plansByFamily: PLANS_BY_FAMILY,
  resultsByFamily: RESULTS_BY_FAMILY,
  users: {
    activeByMonth: [18, 21, 19, 24, 26, 22, 31, 33, 30, 35, 38, 36, 41, 44],
    newByMonth: [6, 4, 3, 7, 5, 2, 9, 6, 4, 8, 7, 5, 9, 11],
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
  unresolvedPlans: [
    { planName: "fapi-rw-id2", runs: 412 },
    { planName: "openbanking-uk-v1", runs: 205 },
  ],
};

/**
 * 200 with a snapshot and nothing in flight — the ordinary case.
 * @type {any}
 */
export const MOCK_STATS_READY = {
  status: "ready",
  computedAt: "2026-06-01T09:12:33Z",
  computeDurationMs: 8421,
  refreshing: false,
  lastError: null,
  data: MOCK_STATS_DATA,
};

/**
 * 200 with the same snapshot while a newer one is being computed
 * (stale-while-revalidate): the page keeps its charts, dims them, and polls.
 * @type {any}
 */
export const MOCK_STATS_REFRESHING = { ...MOCK_STATS_READY, refreshing: true };

/**
 * 200 against a database with no test runs at all: the tiles are all zero and
 * there are no months to chart.
 * @type {any}
 */
export const MOCK_STATS_EMPTY = {
  status: "ready",
  computedAt: "2026-06-01T09:12:33Z",
  computeDurationMs: 120,
  refreshing: false,
  lastError: null,
  data: {
    families: FAMILIES,
    resultBuckets: RESULT_BUCKETS,
    months: [],
    testRunsByFamily: Object.fromEntries(FAMILIES.map((family) => [family, []])),
    plansByFamily: Object.fromEntries(FAMILIES.map((family) => [family, []])),
    resultsByFamily: {},
    users: { activeByMonth: [], newByMonth: [] },
    tiles: {
      totalTests: 0,
      totalPlans: 0,
      totalUsers: 0,
      testsLast24h: 0,
      testsLast7d: 0,
      testsLast30d: 0,
      inProgress: 0,
      stuck: 0,
      certifiedPlans: 0,
    },
    unresolvedPlans: [],
  },
};

/**
 * 202: no snapshot exists yet and the first computation is running.
 * @type {any}
 */
export const MOCK_STATS_PENDING = {
  status: "pending",
  startedAt: "2026-06-01T11:59:30Z",
};

/**
 * 500: there is no snapshot to serve and computing one failed.
 * @type {any}
 */
export const MOCK_STATS_ERROR = {
  status: "error",
  message: "MongoSocketReadException: prematurely reached end of stream",
  failedAt: "2026-06-01T11:59:31Z",
};
