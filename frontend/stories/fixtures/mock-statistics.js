/**
 * Mock payloads for `GET /api/statistics/overview`, matching
 * `net.openid.conformance.statistics.StatisticsResponse` and
 * `StatisticsOverview`.
 *
 * TWIN: `frontend/e2e/fixtures/mock-statistics.js` carries the same families,
 * shapes and figures for the Playwright specs and is kept in sync BY HAND —
 * change one, change the other, or the two suites start quoting different
 * numbers at each other.
 *
 * The READY payload is 14 months (and 26 weeks) of synthetic traffic across
 * eight spec families plus the two synthetic buckets ("No plan", "Other /
 * retired") and one family that never ran. It is deliberately shaped so the
 * page's interesting behaviours are all reachable from it:
 *
 * - more families with data (10) than categorical colour slots (7), so the
 *   tail folds into "Other" and the tooltip footer has something to list;
 * - a family that is busy early and idle late ("OpenID Connect Logout"), so
 *   narrowing the range changes which series render without changing any
 *   family's colour;
 * - every result bucket populated somewhere, including NEVER_FINISHED;
 * - a family with runs but no plans (standalone modules);
 * - plans, variants and certification profiles to cascade through, including
 *   a variant parameter with a single value (nothing to choose between, so
 *   neither the filter row nor the distributions section shows it) and one
 *   whose delivered order is by PLANS while the chart plots USERS;
 * - fifteen test modules — more than either module chart plots — including
 *   one nobody failed, one everybody failed, and ties in both rankings.
 *
 * Since phase 2 the SERVER slices, so a fixture that ignores the query would
 * make every range and filter look broken. {@link statisticsOverviewFor}
 * applies the query to this fixture the way the slicer does — clip the axis,
 * restrict the families, narrow the dimensions — so stories can drive the
 * real controls and assert on what comes back. The e2e twin has the same
 * function with one documented difference: it does not clip the axis, because
 * Playwright runs on the real clock rather than Storybook's frozen one.
 */

const WEEK_COUNT = 26;

/**
 * The 14 contiguous `YYYY-MM` keys, ending 2026-06 — the month of the
 * fixture's `computedAt`, which in turn sits just before Storybook's frozen
 * clock (2026-06-01T12:00:00Z) so every `<cts-time>` renders deterministically.
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
 * The 26 ISO-week Mondays ending 2026-06-01 — which is itself a Monday, and
 * the day Storybook's clock is frozen on, so a weekly range preset lines up
 * exactly with the axis the way it does against a live server.
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
 * the bars are not a flat block but the numbers stay predictable for
 * assertions (`base + step * i + (i % 3)`).
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
 * The per-family shape of the fixture, as `[base, step, from, to]` arguments
 * to {@link ramp} — one row per family, reused at both granularities so the
 * weekly view is recognisably the same database as the monthly one.
 * @type {Record<string, {runs: Array<number>, plans: Array<number>, certified: Array<number>}>}
 */
const SHAPE = {
  "FAPI2 Security Profile": { runs: [180, 24], plans: [12, 2], certified: [2, 0] },
  "FAPI1 Advanced": { runs: [150, 6], plans: [9, 0], certified: [1, 0] },
  "OpenID Connect Core": { runs: [120, 4], plans: [7, 0], certified: [1, 0] },
  OID4VP: { runs: [30, 18], plans: [2, 1], certified: [0, 0] },
  OID4VCI: { runs: [20, 14], plans: [1, 1], certified: [0, 0] },
  "FAPI-CIBA": { runs: [40, 2], plans: [3, 0], certified: [1, 0] },
  "OpenID Federation": { runs: [10, 3], plans: [1, 0], certified: [0, 0] },
  // Busy for the first two periods, then retired. It still earns a
  // categorical slot on all-time totals, so narrowing the range drops it
  // from the charts without repainting anyone else — the exact property
  // assignFamilySlots exists to guarantee.
  "OpenID Connect Logout": { runs: [700, 0, 0, 1], plans: [4, 0, 0, 1], certified: [0, 0] },
  "Shared Signals Framework": { runs: [0, 0], plans: [0, 0], certified: [0, 0] },
  // Standalone runs never belong to a plan.
  "No plan": { runs: [25, 1], plans: [0, 0], certified: [0, 0] },
  "Other / retired": { runs: [12, 0], plans: [1, 0], certified: [0, 0] },
};

/**
 * @param {number} count - How many periods.
 * @param {"runs"|"plans"|"certified"} key - Which series of {@link SHAPE}.
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
 * Split a family's runs into result buckets with a fixed, plausible mix.
 * The residual lands in NEVER_FINISHED, exactly as the slicer computes it
 * server-side.
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
  // Weekly user counts are smaller than monthly ones: the same people, seen
  // over a shorter window.
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

const MONTHLY = seriesFor(MOCK_STATS_MONTHS, "month");
const WEEKLY = seriesFor(MOCK_STATS_WEEKS, "week");

/**
 * What the filter selects can offer, all-time and unfiltered. Plan names are
 * real ones, so the cascade reads like the live page.
 * @type {any}
 */
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
    // A second plan in the same family as the first: selecting one of them
    // narrows `dimensions.plans` to it alone, which is what the page's
    // remembered option lists have to survive.
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

/**
 * Per-collection storage counters, behind the storage KPI row. The numbers
 * are plausible rather than measured, and deliberately span three orders of
 * magnitude so the byte formatter has to reach MB and GB.
 * @type {any}
 */
const STORAGE = [
  {
    collection: "TEST_INFO",
    count: 91800,
    size: 1_820_000_000,
    storageSize: 610_000_000,
    totalIndexSize: 96_000_000,
  },
  {
    collection: "TEST_PLAN",
    count: 1932,
    size: 24_000_000,
    storageSize: 9_400_000,
    totalIndexSize: 2_100_000,
  },
  {
    collection: "EVENT_LOG",
    count: 4_120_000,
    size: 18_400_000_000,
    storageSize: 6_900_000_000,
    totalIndexSize: 740_000_000,
  },
];

/**
 * Runs by day of the week (Monday first) and hour of the day, UTC: 7 rows of
 * 24. Deterministic and shaped like office hours — a working-day bulge, quiet
 * nights and quieter weekends — so the heatmap has a pattern to read and its
 * sequential ramp is exercised from its palest step to its darkest.
 * @type {Array<Array<number>>}
 */
const HEATMAP = Array.from({ length: 7 }, (_, day) =>
  Array.from({ length: 24 }, (_, hour) => {
    const workday = day < 5 ? 1 : 0.2;
    const office = hour >= 7 && hour <= 18 ? 1 : 0.15;
    return Math.round(120 * workday * office + (hour % 5) * 3);
  }),
);

/**
 * The external servers the suite has been pointed at, all-time top rows.
 * @type {any}
 */
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
 * `data.modules` unfiltered, in the server's order (runs descending, ties by
 * name). Exported so a story or a spec can assert against the whole list
 * rather than restating it.
 * @type {Array<any>}
 */
export const MOCK_STATS_MODULES = MODULE_ROWS.map(moduleRow);

/**
 * `data.modules` under one query. Family and plan narrow it — registry
 * membership on the server, the row's own keys here — and nothing else does:
 * the variant and certification filters do not reach the module cube, which
 * is exactly what the section's caption tells the reader. A synthetic family
 * ("No plan", "Other / retired") has no modules under it at all, so it comes
 * back empty, which is the section's empty state.
 * @param {string} family - The family filter, or `""`.
 * @param {string} plan - The plan filter, or `""`.
 * @returns {Array<any>} The modules to answer with.
 */
function narrowModules(family, plan) {
  return MODULE_ROWS.filter(
    (row) => (!family || row.family === family) && (!plan || row.planName === plan),
  ).map(moduleRow);
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

/**
 * The snapshot itself — the `data` half of a READY response, unfiltered and
 * monthly, which is exactly what the page's colour/family baseline request
 * (no query parameters at all) asks for.
 * @type {any}
 */
export const MOCK_STATS_DATA = {
  families: FAMILIES,
  resultBuckets: RESULT_BUCKETS,
  ...MONTHLY,
  tiles: TILES,
  storage: STORAGE,
  dimensions: DIMENSIONS,
  heatmap: HEATMAP,
  modules: MOCK_STATS_MODULES,
  externalHosts: EXTERNAL_HOSTS,
  unresolvedPlans: UNRESOLVED_PLANS,
};

/**
 * 200 with a snapshot and nothing in flight — the ordinary case, unfiltered
 * and monthly.
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
 * @param {Record<string, Array<number>>} map - Family → series.
 * @param {number} start - First period index to keep.
 * @param {number} end - One past the last period index to keep.
 * @param {(family: string) => number} weight - 0 drops a family, 1 keeps it whole.
 * @returns {Record<string, Array<number>>} The clipped, weighted map.
 */
function clipFamilies(map, start, end, weight) {
  return Object.fromEntries(
    Object.entries(map).map(([family, series]) => [
      family,
      series.slice(start, end).map((value) => Math.round(value * weight(family))),
    ]),
  );
}

/**
 * Answer one `GET /api/statistics/overview` request from this fixture, the
 * way `StatisticsSlicer` answers it from the cube: the axis is clipped to
 * `from`/`to`, families outside the filter are zeroed (never removed — the
 * contract is that every family has a series), the dimensions are counted
 * under the whole query INCLUDING their own filter, and the tiles, storage,
 * heatmap, hosts and unresolved plans are never filtered at all.
 *
 * Variant and certification filters halve every series rather than modelling
 * real per-variant traffic: what a story needs is that filtering visibly
 * changes the numbers and the dimensions, not that the fixture is a database.
 * @param {string|URL} requestUrl - The request URL.
 * @returns {any} A READY response body.
 */
export function statisticsOverviewFor(requestUrl) {
  const url = requestUrl instanceof URL ? requestUrl : new URL(requestUrl);
  const params = url.searchParams;
  const weekly = params.get("granularity") === "week";
  const base = weekly ? WEEKLY : MONTHLY;

  const from = params.get("from");
  const to = params.get("to");
  const start = from ? base.periods.findIndex((period) => period >= from) : 0;
  const end = to ? base.periods.filter((period) => period <= to).length : base.periods.length;
  const first = start === -1 ? base.periods.length : start;

  const plan = params.get("plan") || "";
  const planFamily = (DIMENSIONS.plans.find((option) => option.planName === plan) || {}).family;
  const family = params.get("family") || planFamily || "";
  const cert = params.get("cert") || "";
  /** @type {Record<string, string>} */
  const variant = {};
  for (const [key, value] of params.entries()) {
    if (key.startsWith("variant.") && value) variant[key.slice("variant.".length)] = value;
  }

  const narrowing = cert || Object.keys(variant).length > 0 ? 0.5 : 1;
  const weight = (/** @type {string} */ name) => (family && name !== family ? 0 : narrowing);
  const periods = base.periods.slice(first, end);

  return {
    status: "ready",
    computedAt: MOCK_STATS_READY.computedAt,
    computeDurationMs: MOCK_STATS_READY.computeDurationMs,
    refreshing: false,
    lastError: null,
    data: {
      families: FAMILIES,
      resultBuckets: RESULT_BUCKETS,
      periods,
      granularity: weekly ? "week" : "month",
      testRunsByFamily: clipFamilies(base.testRunsByFamily, first, end, weight),
      plansByFamily: clipFamilies(base.plansByFamily, first, end, weight),
      certifiedByFamily: clipFamilies(base.certifiedByFamily, first, end, weight),
      resultsByFamily: Object.fromEntries(
        Object.entries(base.resultsByFamily).map(([name, byBucket]) => [
          name,
          clipFamilies(byBucket, first, end, () => weight(name)),
        ]),
      ),
      users: {
        activeByPeriod: base.users.activeByPeriod
          .slice(first, end)
          .map((value) => Math.round(value * (family ? 0.5 : 1) * narrowing)),
        newByPeriod: base.users.newByPeriod
          .slice(first, end)
          .map((value) => Math.round(value * (family ? 0.5 : 1) * narrowing)),
      },
      tiles: TILES,
      storage: STORAGE,
      dimensions: narrowDimensions(family, plan, variant, cert),
      heatmap: HEATMAP,
      modules: narrowModules(family, plan),
      externalHosts: EXTERNAL_HOSTS,
      unresolvedPlans: UNRESOLVED_PLANS,
    },
  };
}

/**
 * The dimensions under one query. Each dimension is narrowed by every filter,
 * its own included — which is what makes the page's remembered option lists
 * worth having.
 * @param {string} family - The family filter, or `""`.
 * @param {string} plan - The plan filter, or `""`.
 * @param {Record<string, string>} variant - The variant filters.
 * @param {string} cert - The certification profile filter, or `""`.
 * @returns {any} The dimensions.
 */
function narrowDimensions(family, plan, variant, cert) {
  const plans = DIMENSIONS.plans.filter(
    (option) => (!family || option.family === family) && (!plan || option.planName === plan),
  );
  const variants = Object.fromEntries(
    Object.entries(DIMENSIONS.variants).map(([name, values]) => [
      name,
      variant[name]
        ? /** @type {Array<any>} */ (values).filter((option) => option.value === variant[name])
        : values,
    ]),
  );
  return {
    plans,
    variants,
    certProfiles: cert
      ? DIMENSIONS.certProfiles.filter((profile) => profile.name === cert)
      : DIMENSIONS.certProfiles,
    entities: DIMENSIONS.entities,
  };
}

/**
 * 200 with the same snapshot while a newer one is being computed
 * (stale-while-revalidate): the page keeps its charts, dims them, and polls.
 * @type {any}
 */
export const MOCK_STATS_REFRESHING = { ...MOCK_STATS_READY, refreshing: true };

/**
 * 200 with an older snapshot plus the failure of the most recent recompute.
 * @type {any}
 */
export const MOCK_STATS_LAST_ERROR = {
  ...MOCK_STATS_READY,
  lastError: {
    message: "MongoTimeoutException: aggregation exceeded 120000 ms",
    failedAt: "2026-06-01T11:00:02Z",
  },
};

/**
 * 200 against a database with no test runs at all: the tiles are all zero
 * and there are no periods to chart.
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
    modules: [],
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
