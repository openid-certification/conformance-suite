/**
 * Mock payloads for `GET /api/statistics/overview` (the admin-only endpoint
 * behind `statistics.html`), matching
 * `net.openid.conformance.statistics.StatisticsResponse` /
 * `StatisticsOverview`.
 *
 * TWIN: a deliberate self-contained copy of `frontend/stories/fixtures/mock-statistics.js`:
 * e2e fixtures never reach across into the Storybook fixture tree, so the two
 * files are kept in sync by hand. Keep the numbers identical — the e2e
 * assertions and the Storybook play functions quote the same figures, so a
 * drift in one shows up as a puzzling mismatch in the other.
 *
 * Unlike the Storybook twin this one does NOT apply the query: since phase 2
 * the server slices, so these specs answer every request with the same payload
 * and assert on the query the page *sent*. What each control does to the data
 * is covered by the Storybook play functions, which run against a fixture that
 * really slices.
 *
 * The READY payload is 14 months of synthetic traffic across eight spec
 * families plus the two synthetic buckets ("No plan", "Other / retired") and
 * one family that never ran. It is shaped so every behaviour the page has is
 * reachable from it:
 *
 * - more families with data (10) than categorical colour slots (7), so the
 *   neutral tail folds into a single "Other" series;
 * - a family busy only in the first two months ("OpenID Connect Logout");
 * - every result bucket populated somewhere, including NEVER_FINISHED;
 * - a family with runs but no plans ("No plan" — standalone modules);
 * - a family with no runs at all ("Shared Signals Framework"), which must not
 *   be offered in the family filter;
 * - plans, variants and certification profiles for the filter cascade.
 */

const WEEK_COUNT = 26;

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
 * The 26 ISO-week Mondays ending 2026-06-01, for the weekly payload.
 * @type {Array<string>}
 */
export const MOCK_STATS_WEEKS = Array.from({ length: WEEK_COUNT }, (_, i) =>
  new Date(Date.UTC(2026, 5, 1) - (WEEK_COUNT - 1 - i) * 7 * 86400000).toISOString().slice(0, 10),
);

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
 * Build a per-period series from a base value with a deterministic wobble, so
 * the bars are not a flat block but the numbers stay predictable
 * (`base + step * i + (i % 3)`).
 * @param {number} count - How many periods.
 * @param {number} base - Value in the first period.
 * @param {number} step - Per-period growth.
 * @param {number} [from] - First period index that has any traffic.
 * @param {number} [to] - Last period index (inclusive) that has traffic.
 * @returns {Array<number>} The series.
 */
function ramp(count, base, step, from = 0, to = count - 1) {
  return Array.from({ length: count }, (_, i) => {
    // A zero base means "this family never ran"; the wobble must not
    // quietly resurrect it.
    if (base === 0 || i < from || i > to) return 0;
    return base + step * i + (i % 3);
  });
}

/**
 * The per-family shape of the fixture, reused at both granularities.
 * @type {Record<string, {runs: Array<number>, plans: Array<number>, certified: Array<number>}>}
 */
const SHAPE = {
  "FAPI2 Security Profile": { runs: [180, 24], plans: [12, 2], certified: [2, 0] },
  "FAPI1 Advanced": { runs: [150, 6], plans: [9, 0], certified: [1, 0] },
  "OpenID Connect Core": { runs: [120, 4], plans: [7, 0], certified: [1, 0] },
  OID4VP: { runs: [30, 18], plans: [2, 1], certified: [0, 0] },
  OID4VCI: { runs: [20, 14], plans: [1, 1], certified: [0, 0] },
  "FAPI-CIBA": { runs: [40, 2], plans: [3, 0], certified: [1, 0] },
  // Ranks eighth all-time, so it wears the neutral and folds into "Other".
  "OpenID Federation": { runs: [10, 3], plans: [1, 0], certified: [0, 0] },
  // Busy for the first two periods, then retired.
  "OpenID Connect Logout": { runs: [700, 0, 0, 1], plans: [4, 0, 0, 1], certified: [0, 0] },
  "Shared Signals Framework": { runs: [0, 0], plans: [0, 0], certified: [0, 0] },
  // Standalone runs never belong to a plan.
  "No plan": { runs: [25, 1], plans: [0, 0], certified: [0, 0] },
  "Other / retired": { runs: [12, 0], plans: [1, 0], certified: [0, 0] },
};

/**
 * @param {number} count - How many periods.
 * @param {"runs"|"plans"|"certified"} key - Which series of the shape.
 * @returns {Record<string, Array<number>>} Family → series.
 */
function byFamily(count, key) {
  return Object.fromEntries(
    FAMILIES.map((family) => [
      family,
      ramp(
        count,
        SHAPE[family][key][0],
        SHAPE[family][key][1],
        SHAPE[family][key][2] ?? 0,
        SHAPE[family][key][3] ?? count - 1,
      ),
    ]),
  );
}

/**
 * Split a family's runs into result buckets with a fixed, plausible mix. The
 * residual lands in NEVER_FINISHED, exactly as the slicer computes it.
 * @param {Array<number>} runs - The family's runs per period.
 * @returns {Record<string, Array<number>>} Bucket → runs per period.
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

/**
 * Everything that is keyed by period, at one granularity.
 * @param {Array<string>} periods - The axis.
 * @param {string} granularity - `"month"` or `"week"`.
 * @returns {any} The per-period half of a payload.
 */
function seriesFor(periods, granularity) {
  const count = periods.length;
  const runs = byFamily(count, "runs");
  const scale = granularity === "week" ? 0.45 : 1;
  return {
    periods: [...periods],
    granularity,
    testRunsByFamily: runs,
    plansByFamily: byFamily(count, "plans"),
    certifiedByFamily: byFamily(count, "certified"),
    resultsByFamily: Object.fromEntries(FAMILIES.map((family) => [family, buckets(runs[family])])),
    users: {
      activeByPeriod: Array.from({ length: count }, (_, i) =>
        Math.round((16 + 2 * i + (i % 3)) * scale),
      ),
      newByPeriod: Array.from({ length: count }, (_, i) => Math.round((6 + (i % 4)) * scale)),
    },
  };
}

/** @type {any} */
const DIMENSIONS = {
  plans: [
    {
      planName: "fapi2-security-profile-final-test-plan",
      family: "FAPI2 Security Profile",
      runs: 24800,
      plans: 420,
    },
    {
      planName: "fapi1-advanced-final-test-plan",
      family: "FAPI1 Advanced",
      runs: 18200,
      plans: 310,
    },
    {
      planName: "fapi2-message-signing-final-test-plan",
      family: "FAPI2 Security Profile",
      runs: 11600,
      plans: 180,
    },
    {
      planName: "oidcc-basic-certification-test-plan",
      family: "OpenID Connect Core",
      runs: 9400,
      plans: 260,
    },
    { planName: "oid4vp-1final-verifier-test-plan", family: "OID4VP", runs: 4100, plans: 96 },
    { planName: "fapi-ciba-id1-test-plan", family: "FAPI-CIBA", runs: 3300, plans: 74 },
    {
      planName: "openid-federation-op-test-plan",
      family: "OpenID Federation",
      runs: 820,
      plans: 22,
    },
  ],
  variants: {
    client_auth_type: [
      { value: "private_key_jwt", users: 88, plans: 640 },
      { value: "mtls", users: 61, plans: 410 },
    ],
    fapi_profile: [
      { value: "plain_fapi", users: 54, plans: 380 },
      { value: "openbanking_brazil", users: 37, plans: 290 },
      { value: "openbanking_uk", users: 12, plans: 60 },
    ],
  },
  certProfiles: [
    { name: "FAPI2 Security Profile Final", users: 31, plans: 120 },
    { name: "Brazil Open Finance | FAPI-CIBA", users: 18, plans: 64 },
    { name: "OpenID Connect Basic OP", users: 11, plans: 39 },
  ],
  entities: [
    { entity: "Test an OpenID Provider / Authorization Server", runs: 61200 },
    { entity: "Test a Relying Party / Client", runs: 22400 },
    { entity: "Test a Wallet", runs: 8200 },
  ],
};

/** @type {any} */
const STORAGE = [
  {
    collection: "TEST_INFO",
    count: 91800,
    size: 1820000000,
    storageSize: 610000000,
    totalIndexSize: 96000000,
  },
  {
    collection: "TEST_PLAN",
    count: 1932,
    size: 24000000,
    storageSize: 9400000,
    totalIndexSize: 2100000,
  },
  {
    collection: "EVENT_LOG",
    count: 4120000,
    size: 18400000000,
    storageSize: 6900000000,
    totalIndexSize: 740000000,
  },
];

/**
 * Runs by day of the week (Monday first) and hour of the day, UTC.
 * @type {Array<Array<number>>}
 */
const HEATMAP = Array.from({ length: 7 }, (_, day) =>
  Array.from({ length: 24 }, (_, hour) => {
    const workday = day < 5 ? 1 : 0.2;
    const office = hour >= 7 && hour <= 18 ? 1 : 0.15;
    return Math.round(120 * workday * office + (hour % 5) * 3);
  }),
);

/** @type {any} */
const EXTERNAL_HOSTS = [
  { host: "as.example.com", runs: 8200, users: 41, lastSeen: "2026-05-31T22:14:02Z" },
  { host: "auth.bank.example", runs: 5100, users: 12, lastSeen: "2026-05-30T08:02:44Z" },
  { host: "idp.example.org", runs: 2400, users: 26, lastSeen: "2026-05-28T16:39:10Z" },
];

/** @type {any} */
const TILES = {
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
};

/** @type {any} */
const UNRESOLVED_PLANS = [
  { planName: "fapi-rw-id2", runs: 412 },
  { planName: "openbanking-uk-v1", runs: 205 },
];

/**
 * The snapshot itself — the `data` half of a READY response, monthly.
 * @type {any}
 */
export const MOCK_STATS_DATA = {
  families: FAMILIES,
  resultBuckets: RESULT_BUCKETS,
  ...seriesFor(MOCK_STATS_MONTHS, "month"),
  tiles: TILES,
  storage: STORAGE,
  dimensions: DIMENSIONS,
  heatmap: HEATMAP,
  externalHosts: EXTERNAL_HOSTS,
  unresolvedPlans: UNRESOLVED_PLANS,
};

/**
 * The same snapshot at weekly granularity, for the weekly range presets.
 * @type {any}
 */
export const MOCK_STATS_WEEKLY_DATA = {
  ...MOCK_STATS_DATA,
  ...seriesFor(MOCK_STATS_WEEKS, "week"),
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
 * 200 with the weekly snapshot, for a request that asked for `granularity=week`.
 * @type {any}
 */
export const MOCK_STATS_WEEKLY = { ...MOCK_STATS_READY, data: MOCK_STATS_WEEKLY_DATA };

/**
 * 200 with the same snapshot while a newer one is being computed
 * (stale-while-revalidate): the page keeps its charts, dims them, and polls.
 * @type {any}
 */
export const MOCK_STATS_REFRESHING = { ...MOCK_STATS_READY, refreshing: true };

/**
 * 200 against a database with no test runs at all: the tiles are all zero and
 * there are no periods to chart.
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
    periods: [],
    granularity: "month",
    testRunsByFamily: Object.fromEntries(FAMILIES.map((family) => [family, []])),
    plansByFamily: Object.fromEntries(FAMILIES.map((family) => [family, []])),
    certifiedByFamily: Object.fromEntries(FAMILIES.map((family) => [family, []])),
    resultsByFamily: {},
    users: { activeByPeriod: [], newByPeriod: [] },
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
      publishedPlans: 0,
    },
    storage: [],
    dimensions: { plans: [], variants: {}, certProfiles: [], entities: [] },
    heatmap: Array.from({ length: 7 }, () => new Array(24).fill(0)),
    externalHosts: [],
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
 * 400: a filter parameter could not be used at all. The message is verbatim
 * from `QueryParams.variant()`, and the story that serves this triggers it the
 * way the server would — with a variant parameter whose NAME is not one.
 * @type {any}
 */
export const MOCK_STATS_INVALID = {
  status: "invalid",
  message:
    "'variant.bad name' is not a variant parameter name; only letters, digits, '_' and '-' can be used",
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
