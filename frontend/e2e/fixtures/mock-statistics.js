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
 * Like the Storybook twin, {@link statisticsOverviewFor} answers one request
 * the way `StatisticsSlicer` would — with ONE deliberate difference: it does
 * **not** clip the axis to `from`/`to`. Storybook runs on a frozen clock, but
 * Playwright runs on the real one, so clipping fixed fixture dates against
 * "12 weeks before today" would empty the payload the moment this fixture aged
 * past the preset — a test that rots on a calendar. The specs assert on the
 * `from` the page *sent* instead, which is the half the page is responsible
 * for; what clipping does to the numbers is covered by the Storybook play
 * functions. Everything else (granularity, filters, dimensions, the 400 for a
 * malformed `variant.<name>`) is applied here.
 *
 * The canned constants below (`MOCK_STATS_READY`, `MOCK_STATS_EMPTY`, …) are
 * the whole, unfiltered payload and are what the state-machine specs (202
 * polling, refresh, 500) answer with, so their axis assertions do not move.
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
 * - plans, variants and certification profiles for the filter cascade,
 *   including a variant parameter with a single value (which neither the
 *   filter row nor the distributions section shows);
 * - storage rows, a day x hour heatmap and external hosts for the sections
 *   below the trend charts;
 * - fifteen test modules — more than either module chart plots — including
 *   one nobody failed, one everybody failed, and ties in both rankings.
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
    // Ranked by PLANS on the way in, and by users the other way round, so the
    // distribution chart has to sort on the measure it plots rather than trust
    // the delivered order.
    server_metadata: [
      { value: "discovery", users: 23, plans: 512 },
      { value: "static", users: 71, plans: 145 },
    ],
    // A parameter with only one value: nothing to choose between, so neither
    // the filter row nor the distributions section shows it.
    client_registration: [{ value: "dynamic_client", users: 9, plans: 34 }],
  },
  certProfiles: [
    { name: "FAPI2 Security Profile Final", users: 31, plans: 120 },
    { name: "Brazil Open Finance | FAPI-CIBA", users: 18, plans: 64 },
    { name: "OpenID Connect Basic OP", users: 11, plans: 39 },
    // A long name, so the distribution chart's axis has something to elide.
    { name: "FAPI2 Message Signing Final | Brazil Open Finance", users: 7, plans: 21 },
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
  { host: "wallet.example.net", runs: 1180, users: 9, lastSeen: "2026-05-27T11:05:31Z" },
  { host: "issuer.example.coop", runs: 640, users: 5, lastSeen: "2026-05-21T19:48:00Z" },
  { host: "op.staging.example", runs: 210, users: 3, lastSeen: "2026-04-30T06:12:19Z" },
];

/**
 * The test modules behind `data.modules`, with the family and the plan each
 * one belongs to so this fixture can apply the family/plan filters the way
 * the registry does on the server. Those two keys are stripped on the way
 * out — the wire shape is `{testName, runs, users, failingUsers,
 * failingShare}` and nothing else.
 *
 * Fifteen of them, deliberately more than the dozen either chart plots, and
 * shaped so every rule the section has is reachable: a module nobody failed
 * (`oidcc-discovery-endpoint-verification`), one every user failed
 * (`fapi2-message-signing-final-signed-request-object`, share 1.0), two with
 * identical run counts and three with identical failing-user counts, so both
 * charts have a tie to break on the module name — and a failing-users
 * ranking that is emphatically not the delivered order, which is by runs.
 * @type {Array<any>}
 */
const MODULE_ROWS = [
  {
    testName: "fapi2-security-profile-final-ensure-request-object-signature-algorithm-is-not-none",
    runs: 1420,
    users: 38,
    failingUsers: 9,
    family: "FAPI2 Security Profile",
    planName: "fapi2-security-profile-final-test-plan",
  },
  {
    testName: "fapi1-advanced-final-ensure-registered-redirect-uri",
    runs: 1180,
    users: 31,
    failingUsers: 4,
    family: "FAPI1 Advanced",
    planName: "fapi1-advanced-final-test-plan",
  },
  {
    // Nobody failed it: the failing-users chart must leave it out, and the
    // table must still say 0 rather than a blank.
    testName: "oidcc-discovery-endpoint-verification",
    runs: 960,
    users: 44,
    failingUsers: 0,
    family: "OpenID Connect Core",
    planName: "oidcc-basic-certification-test-plan",
  },
  {
    testName: "fapi2-security-profile-final-user-rejects-authentication",
    runs: 880,
    users: 29,
    failingUsers: 22,
    family: "FAPI2 Security Profile",
    planName: "fapi2-security-profile-final-test-plan",
  },
  {
    testName: "oidcc-server",
    runs: 760,
    users: 40,
    failingUsers: 12,
    family: "OpenID Connect Core",
    planName: "oidcc-basic-certification-test-plan",
  },
  {
    // Every user who ran it hit a failure: share 1.0, the top of the scale.
    testName: "fapi2-message-signing-final-signed-request-object",
    runs: 640,
    users: 24,
    failingUsers: 24,
    family: "FAPI2 Security Profile",
    planName: "fapi2-message-signing-final-test-plan",
  },
  {
    // Same run count as the row above, so the runs chart has a tie to break;
    // the delivered order is the one the server would deliver (by name).
    testName: "fapi2-security-profile-final-par-without-request-uri",
    runs: 640,
    users: 21,
    failingUsers: 6,
    family: "FAPI2 Security Profile",
    planName: "fapi2-security-profile-final-test-plan",
  },
  {
    testName: "oid4vp-1final-verifier-happy-path",
    runs: 520,
    users: 18,
    failingUsers: 5,
    family: "OID4VP",
    planName: "oid4vp-1final-verifier-test-plan",
  },
  {
    testName: "fapi-ciba-id1-poll-happy-path",
    runs: 460,
    users: 16,
    failingUsers: 9,
    family: "FAPI-CIBA",
    planName: "fapi-ciba-id1-test-plan",
  },
  {
    testName: "oidcc-refresh-token",
    runs: 430,
    users: 27,
    failingUsers: 3,
    family: "OpenID Connect Core",
    planName: "oidcc-basic-certification-test-plan",
  },
  {
    testName: "openid-federation-op-fetch-endpoint",
    runs: 380,
    users: 12,
    failingUsers: 11,
    family: "OpenID Federation",
    planName: "openid-federation-op-test-plan",
  },
  {
    // One of the three modules with nine failing users; they are what the
    // failing chart's name tie-break sorts.
    testName: "fapi1-advanced-final-ensure-request-object-signature-algorithm-is-not-none",
    runs: 340,
    users: 14,
    failingUsers: 9,
    family: "FAPI1 Advanced",
    planName: "fapi1-advanced-final-test-plan",
  },
  {
    testName: "oidcc-claims-essential",
    runs: 300,
    users: 19,
    failingUsers: 2,
    family: "OpenID Connect Core",
    planName: "oidcc-basic-certification-test-plan",
  },
  {
    testName: "fapi-ciba-id1-notification-happy-path",
    runs: 260,
    users: 9,
    failingUsers: 7,
    family: "FAPI-CIBA",
    planName: "fapi-ciba-id1-test-plan",
  },
  {
    testName: "oid4vp-1final-verifier-invalid-nonce",
    runs: 210,
    users: 8,
    failingUsers: 6,
    family: "OID4VP",
    planName: "oid4vp-1final-verifier-test-plan",
  },
];

/**
 * One module row on the wire: the family and plan keys dropped, and
 * `failingShare` computed the way `ModuleRanker` computes it — three
 * decimals, 0 when nobody ran it.
 * @param {any} row - One row of {@link MODULE_ROWS}.
 * @returns {any} The `StatisticsModule` the server would emit.
 */
function moduleRow(row) {
  return {
    testName: row.testName,
    runs: row.runs,
    users: row.users,
    failingUsers: row.failingUsers,
    failingShare: row.users === 0 ? 0 : Math.round((row.failingUsers / row.users) * 1000) / 1000,
  };
}

/**
 * `data.modules.rows` unfiltered, in the server's order (runs descending,
 * ties by name). Exported so a story or a spec can assert against the whole
 * list rather than restating it.
 * @type {Array<any>}
 */
export const MOCK_STATS_MODULES = MODULE_ROWS.map(moduleRow);

/**
 * Compare two module names the way `ModuleRanker` does: Java's
 * `String::compareTo`, i.e. UTF-16 code units, not a locale.
 * @param {any} a - One row.
 * @param {any} b - The other.
 * @returns {number} Comparator result.
 */
function byName(a, b) {
  return a.testName < b.testName ? -1 : a.testName > b.testName ? 1 : 0;
}

/**
 * `data.modules` the way `ModuleRanker` builds it from a set of rows: the
 * rows sorted by runs (ties by name) for the table, plus the two rankings the
 * charts plot — by runs then name, and by failing users then runs then name
 * — each cut at the server's fifty.
 * @param {Array<any>} rows - Module rows, in any order.
 * @returns {{rows: Array<any>, byRuns: Array<string>, byFailingUsers: Array<string>}}
 *   The payload's modules section.
 */
function rankModules(rows) {
  const byRuns = [...rows].sort((a, b) => b.runs - a.runs || byName(a, b));
  const byFailingUsers = [...rows].sort(
    (a, b) => b.failingUsers - a.failingUsers || b.runs - a.runs || byName(a, b),
  );
  return {
    rows: byRuns,
    byRuns: byRuns.slice(0, 50).map((row) => row.testName),
    byFailingUsers: byFailingUsers.slice(0, 50).map((row) => row.testName),
  };
}

/**
 * `data.modules` under one query. Family and plan narrow it — registry
 * membership on the server, the row's own keys here — and nothing else does:
 * the variant and certification filters do not reach the module cube, which
 * is exactly what the section's caption tells the reader. A synthetic family
 * ("No plan", "Other / retired") has no modules under it at all, so it comes
 * back empty, which is the section's empty state.
 * @param {string} family - The family filter, or `""`.
 * @param {string} plan - The plan filter, or `""`.
 * @returns {{rows: Array<any>, byRuns: Array<string>, byFailingUsers: Array<string>}}
 *   The modules section to answer with.
 */
function narrowModules(family, plan) {
  return rankModules(
    MODULE_ROWS.filter(
      (row) => (!family || row.family === family) && (!plan || row.planName === plan),
    ).map(moduleRow),
  );
}

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

/** The per-period half of the fixture at each granularity. */
const MONTHLY = seriesFor(MOCK_STATS_MONTHS, "month");
const WEEKLY = seriesFor(MOCK_STATS_WEEKS, "week");

/**
 * The families that are not families, as `SpecFamilyResolver.SYNTHETIC_FAMILIES`
 * names them on every payload.
 * @type {Array<string>}
 */
const SYNTHETIC_FAMILIES = ["No plan", "Other / retired"];

/**
 * @param {Array<number>} values - A series.
 * @returns {number} Its sum.
 */
function sum(values) {
  return values.reduce((total, value) => total + value, 0);
}

/**
 * Every family's whole history, as `StatisticsCube.familyTotals()` computes
 * it: the monthly series summed, never filtered and never clipped, on every
 * payload whatever it was asked for. It is what ranks the families for
 * colour and fills the family select.
 * @type {Record<string, {runs: number, plans: number, certified: number}>}
 */
const FAMILY_TOTALS = Object.fromEntries(
  FAMILIES.map((family) => [
    family,
    {
      runs: sum(MONTHLY.testRunsByFamily[family]),
      plans: sum(MONTHLY.plansByFamily[family]),
      certified: sum(MONTHLY.certifiedByFamily[family]),
    },
  ]),
);

/**
 * The snapshot itself — the `data` half of a READY response, monthly.
 * @type {any}
 */
export const MOCK_STATS_DATA = {
  families: FAMILIES,
  syntheticFamilies: SYNTHETIC_FAMILIES,
  resultBuckets: RESULT_BUCKETS,
  ...MONTHLY,
  familyTotals: FAMILY_TOTALS,
  tiles: TILES,
  storage: STORAGE,
  dimensions: DIMENSIONS,
  heatmap: HEATMAP,
  modules: rankModules(MOCK_STATS_MODULES),
  externalHosts: EXTERNAL_HOSTS,
  unresolvedPlans: UNRESOLVED_PLANS,
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

// --- The slicer, in miniature ------------------------------------------

/** Matches `QueryParams.VARIANT_NAME` on the server. */
const VARIANT_NAME = /^[A-Za-z0-9_-]+$/;

/**
 * The server's verbatim complaint about a `variant.<name>` whose NAME is not
 * one (`QueryParams.variant()`), which is the realistic way a page reaches a
 * 400: a value is never rejected, only a name.
 * @param {string} name - The offending name, without the `variant.` prefix.
 * @returns {string} The message the server would answer with.
 */
export function invalidVariantMessage(name) {
  return (
    `'variant.${name}' is not a variant parameter name; ` +
    "only letters, digits, '_' and '-' can be used"
  );
}

/**
 * @param {URLSearchParams} params - The request's query.
 * @returns {Record<string, string>} The `variant.<name>=<value>` filters.
 */
function variantFilters(params) {
  /** @type {Record<string, string>} */
  const variant = {};
  for (const [key, value] of params.entries()) {
    if (key.startsWith("variant.") && value) variant[key.slice("variant.".length)] = value;
  }
  return variant;
}

/**
 * @param {Record<string, Array<number>>} map - Family → series.
 * @param {(family: string) => number} weight - 0 drops a family, 1 keeps it whole.
 * @returns {Record<string, Array<number>>} The weighted map.
 */
function scaleFamilies(map, weight) {
  return Object.fromEntries(
    Object.entries(map).map(([family, series]) => [
      family,
      series.map((value) => Math.round(value * weight(family))),
    ]),
  );
}

/**
 * The dimensions under one query. The server counts each dimension with its
 * own filter left out, so picking a plan, a variant value or a certification
 * profile never narrows its own select: here only the family — which a plan
 * implies — narrows anything, and it narrows the plans.
 * @param {string} family - The family filter, or `""`.
 * @returns {any} The dimensions.
 */
function narrowDimensions(family) {
  return {
    plans: DIMENSIONS.plans.filter((option) => !family || option.family === family),
    variants: DIMENSIONS.variants,
    certProfiles: DIMENSIONS.certProfiles,
    entities: DIMENSIONS.entities,
  };
}

/**
 * Answer one `GET /api/statistics/overview` request from this fixture: the
 * granularity picks the axis, families outside the filter are zeroed (never
 * removed — the contract is that every family has a series), the dimensions
 * are counted with each dimension's own filter left out, and the tiles,
 * storage, heatmap, hosts and unresolved plans are never filtered at all.
 * The axis itself is NOT clipped to `from`/`to` — see the file header.
 *
 * Variant and certification filters halve every series rather than modelling
 * real per-variant traffic: what a spec needs is that filtering visibly
 * changes the numbers and the dimensions, not that the fixture is a database.
 * A family that never ran ("Shared Signals Framework") therefore yields a
 * payload of zeros, which is what the page's no-match state is made of.
 * @param {string|URL} requestUrl - The request URL.
 * @returns {any} A READY response body.
 */
export function statisticsOverviewFor(requestUrl) {
  const url = requestUrl instanceof URL ? requestUrl : new URL(requestUrl);
  const params = url.searchParams;
  const weekly = params.get("granularity") === "week";
  const base = weekly ? WEEKLY : MONTHLY;

  const plan = params.get("plan") || "";
  const planFamily = (DIMENSIONS.plans.find((option) => option.planName === plan) || {}).family;
  const family = params.get("family") || planFamily || "";
  const cert = params.get("cert") || "";
  const variant = variantFilters(params);

  const narrowing = cert || Object.keys(variant).length > 0 ? 0.5 : 1;
  const weight = (/** @type {string} */ name) => (family && name !== family ? 0 : narrowing);
  const userScale = (family ? 0.5 : 1) * narrowing;

  return {
    status: "ready",
    computedAt: MOCK_STATS_READY.computedAt,
    computeDurationMs: MOCK_STATS_READY.computeDurationMs,
    refreshing: false,
    lastError: null,
    data: {
      families: FAMILIES,
      syntheticFamilies: SYNTHETIC_FAMILIES,
      resultBuckets: RESULT_BUCKETS,
      periods: [...base.periods],
      granularity: weekly ? "week" : "month",
      testRunsByFamily: scaleFamilies(base.testRunsByFamily, weight),
      plansByFamily: scaleFamilies(base.plansByFamily, weight),
      certifiedByFamily: scaleFamilies(base.certifiedByFamily, weight),
      resultsByFamily: Object.fromEntries(
        Object.entries(base.resultsByFamily).map(([name, byBucket]) => [
          name,
          scaleFamilies(byBucket, () => weight(name)),
        ]),
      ),
      familyTotals: FAMILY_TOTALS,
      users: {
        activeByPeriod: base.users.activeByPeriod.map((value) => Math.round(value * userScale)),
        newByPeriod: base.users.newByPeriod.map((value) => Math.round(value * userScale)),
      },
      tiles: TILES,
      storage: STORAGE,
      dimensions: narrowDimensions(family),
      heatmap: HEATMAP,
      modules: narrowModules(family, plan),
      externalHosts: EXTERNAL_HOSTS,
      unresolvedPlans: UNRESOLVED_PLANS,
    },
  };
}

/**
 * The whole response — status included — the way the endpoint would answer
 * it, so a route helper can be handed the request URL and nothing else.
 * @param {string|URL} requestUrl - The request URL.
 * @returns {{status: number, body: any}} The status and body to answer with.
 */
export function statisticsResponseFor(requestUrl) {
  const url = requestUrl instanceof URL ? requestUrl : new URL(requestUrl);
  for (const key of url.searchParams.keys()) {
    if (!key.startsWith("variant.")) continue;
    const name = key.slice("variant.".length).trim();
    // The server validates the NAME whether or not a value came with it.
    if (!VARIANT_NAME.test(name)) {
      return { status: 400, body: { status: "invalid", message: invalidVariantMessage(name) } };
    }
  }
  return { status: 200, body: statisticsOverviewFor(url) };
}

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
    syntheticFamilies: SYNTHETIC_FAMILIES,
    resultBuckets: RESULT_BUCKETS,
    periods: [],
    granularity: "month",
    testRunsByFamily: Object.fromEntries(FAMILIES.map((family) => [family, []])),
    plansByFamily: Object.fromEntries(FAMILIES.map((family) => [family, []])),
    certifiedByFamily: Object.fromEntries(FAMILIES.map((family) => [family, []])),
    familyTotals: Object.fromEntries(
      FAMILIES.map((family) => [family, { runs: 0, plans: 0, certified: 0 }]),
    ),
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
    modules: { rows: [], byRuns: [], byFailingUsers: [] },
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
  message: invalidVariantMessage("bad name"),
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
