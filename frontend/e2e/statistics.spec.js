import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_ADMIN_USER, MOCK_USER } from "./fixtures/mock-users.js";
import {
  MOCK_STATS_EMPTY,
  MOCK_STATS_ERROR,
  MOCK_STATS_MONTHS,
  MOCK_STATS_PENDING,
  MOCK_STATS_READY,
  MOCK_STATS_REFRESHING,
} from "./fixtures/mock-statistics.js";

/**
 * statistics.html — the admin-only usage dashboard.
 *
 * Everything on the page hangs off one endpoint, `GET /api/statistics/overview`,
 * whose contract has four outcomes the page must handle differently: 200 with a
 * snapshot, 200 with `refreshing: true` (a newer snapshot is on the way), 202
 * while the first snapshot is being computed, 403 for a non-admin, and 500 when
 * there is nothing to serve. These specs drive the real page against each of
 * them, so the state machine, the poll cadence, the filters and the navbar
 * gating are all exercised end to end rather than per component.
 *
 * Chart.js is NOT stubbed: `<cts-chart>` lazily injects
 * `/vendor/chart.js/chart.umd.js`, which the e2e static server serves for real,
 * so the four plots are actually painted. Exact values are read from each
 * chart's `<details>` data table — the accessible twin of the canvas and the
 * only place the numbers are assertable.
 */

/** Matches the endpoint with and without the `?refresh=true` query. */
const ENDPOINT_GLOB = "**/api/statistics/overview*";

/**
 * The page polls every 2 s for the first 30 s, so a route that answers 202
 * twice before the snapshot needs ~4 s of wall clock. Every wait that spans a
 * poll gets this timeout instead of the 5 s assertion default.
 */
const POLL_TIMEOUT = 20000;

/** Tests that sit through one or more 2 s polls need more than the 30 s default. */
const POLLING_TEST_TIMEOUT = 60000;

/**
 * The nine KPI tiles as the page must render them from MOCK_STATS_READY:
 * exact grouped figures, never compacted — this is an admin console where the
 * difference between 91,800 and 91,842 is the point.
 * @type {Record<string, string>}
 */
const READY_TILE_TEXT = {
  totalTests: "91,800",
  totalPlans: "1,932",
  totalUsers: "210",
  testsLast24h: "41",
  testsLast7d: "260",
  testsLast30d: "980",
  inProgress: "3",
  stuck: "7",
  certifiedPlans: "88",
};

const CHART_TESTIDS = [
  "stats-chart-runs",
  "stats-chart-plans",
  "stats-chart-results",
  "stats-chart-users",
];

/**
 * One response from the mocked statistics endpoint.
 * @typedef {object} StatsRouteResponse
 * @property {number} status - HTTP status to answer with.
 * @property {any} [body] - JSON body; omit entirely for an empty body (403).
 * @property {Record<string, string>} [headers] - Extra response headers.
 */

/**
 * Register the statistics endpoint with a per-call responder and record what
 * was asked for. The recorded strings are URL *search* parts (`""` or
 * `"?refresh=true"`), which is what the `?refresh` assertions care about, and
 * their count doubles as the poll counter.
 *
 * Responders are module-scope functions rather than inline closures so the
 * `if`s they need stay out of test bodies (`playwright/no-conditional-in-test`).
 *
 * @param {import('@playwright/test').Page} page
 * @param {(callIndex: number, url: URL) => StatsRouteResponse} respond - Called
 *   with the 1-based request number and the parsed request URL.
 * @returns {Promise<Array<string>>} Search strings, in request order; grows as
 *   the page polls.
 */
async function setupStatisticsRoute(page, respond) {
  /** @type {Array<string>} */
  const searches = [];
  await page.route(ENDPOINT_GLOB, (route) => {
    const url = new URL(route.request().url());
    searches.push(url.search);
    const response = respond(searches.length, url);
    if (response.body === undefined) {
      // 403 is documented as having no body at all.
      return route.fulfill({ status: response.status, body: "" });
    }
    return route.fulfill({
      status: response.status,
      contentType: "application/json",
      headers: response.headers || {},
      body: JSON.stringify(response.body),
    });
  });
  return searches;
}

/** @returns {StatsRouteResponse} A settled snapshot, every time. */
function respondReady() {
  return { status: 200, body: MOCK_STATS_READY };
}

/**
 * No snapshot yet: 202 twice, then the first one. The page must poll through
 * both without showing an error.
 * @param {number} callIndex - 1-based request number.
 * @returns {StatsRouteResponse} The response for this call.
 */
function respondPendingTwiceThenReady(callIndex) {
  if (callIndex <= 2) {
    return { status: 202, body: MOCK_STATS_PENDING, headers: { "retry-after": "2" } };
  }
  return { status: 200, body: MOCK_STATS_READY };
}

/**
 * Mirrors the endpoint's refresh behaviour: a forced recompute answers 200
 * with `refreshing: true` (keep showing this snapshot, a newer one is coming),
 * and the poll that follows — which carries no `?refresh` — finds it settled.
 * @param {number} callIndex - 1-based request number (unused).
 * @param {URL} url - The request URL.
 * @returns {StatsRouteResponse} The response for this call.
 */
function respondRefreshingThenSettled(callIndex, url) {
  return {
    status: 200,
    body: url.searchParams.get("refresh") === "true" ? MOCK_STATS_REFRESHING : MOCK_STATS_READY,
  };
}

/** @returns {StatsRouteResponse} The non-admin answer: 403, empty body. */
function respondForbidden() {
  return { status: 403 };
}

/** @returns {StatsRouteResponse} A snapshot of a database with no test runs. */
function respondEmpty() {
  return { status: 200, body: MOCK_STATS_EMPTY };
}

/**
 * Nothing to serve and computing failed — then a Retry that succeeds.
 * @param {number} callIndex - 1-based request number.
 * @returns {StatsRouteResponse} The response for this call.
 */
function respondErrorThenReady(callIndex) {
  if (callIndex === 1) return { status: 500, body: MOCK_STATS_ERROR };
  return { status: 200, body: MOCK_STATS_READY };
}

/**
 * Record uncaught page errors so a crash inside the page's own JS fails the
 * test rather than hiding behind a locator timeout. Mirrors the listener
 * `lit-importmap.spec.js` attaches.
 * @param {import('@playwright/test').Page & { __pageErrors?: Error[] }} page
 */
function watchPageErrors(page) {
  /** @type {Error[]} */
  const pageErrors = [];
  page.__pageErrors = pageErrors;
  page.on("pageerror", (err) => pageErrors.push(err));
}

/**
 * Fail the test if {@link watchPageErrors} recorded anything.
 * @param {import('@playwright/test').Page & { __pageErrors?: Error[] }} page
 */
function expectNoPageErrors(page) {
  if (!page.__pageErrors) {
    throw new Error("watchPageErrors() was not called — page errors would not be detected");
  }
  if (page.__pageErrors.length > 0) {
    throw new Error(
      `Unexpected page errors:\n  ${page.__pageErrors.map((e) => e.message).join("\n  ")}`,
    );
  }
}

/**
 * The `<tbody>` rows of one chart's data table — one per month in the current
 * range.
 * @param {import('@playwright/test').Page} page
 * @param {string} testid - Chart wrapper test id, e.g. `stats-chart-runs`.
 * @returns {import('@playwright/test').Locator} The rows.
 */
function chartRows(page, testid) {
  return page.locator(`[data-testid="${testid}"] table tbody tr`);
}

/**
 * The `<thead>` cells of one chart's data table — the category column
 * followed by one column per rendered series.
 * @param {import('@playwright/test').Page} page
 * @param {string} testid - Chart wrapper test id, e.g. `stats-chart-runs`.
 * @returns {import('@playwright/test').Locator} The header cells.
 */
function chartHeaders(page, testid) {
  return page.locator(`[data-testid="${testid}"] table thead th`);
}

/**
 * Wait until all four plots exist. The `<canvas>` lands in the DOM a beat
 * before Chart.js has been fetched and instantiated, so the element's
 * `chartInstance` getter is the reliable "the plot is painted" signal — and
 * asserting on it also proves the vendored bundle is really being served.
 * @param {import('@playwright/test').Page} page
 */
async function expectChartsPainted(page) {
  await expect(page.locator(".cts-stats-chart figure canvas")).toHaveCount(4, {
    timeout: POLL_TIMEOUT,
  });
  await expect
    .poll(
      () =>
        page
          .locator(".cts-stats-chart cts-chart")
          .evaluateAll(
            (hosts) => hosts.filter((el) => /** @type {any} */ (el).chartInstance).length,
          ),
      { timeout: POLL_TIMEOUT },
    )
    .toBe(4);
}

test.describe("statistics.html — admin usage dashboard", () => {
  test.beforeEach(async ({ page }) => {
    watchPageErrors(page);
  });

  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
    expectNoPageErrors(page);
  });

  test("polls through 202 responses and then renders tiles and four charts", async ({ page }) => {
    test.setTimeout(POLLING_TEST_TIMEOUT);
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page, respondPendingTwiceThenReady);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");

    // While the server computes, the page says so and shows nothing else:
    // no charts, and — crucially — no error, because 202 is not a failure.
    const loading = page.locator('[data-testid="stats-loading"]');
    await expect(loading).toBeVisible();
    await expect(loading).toHaveAttribute("label", "Computing statistics for the first time");
    await expect(page.locator('[data-testid="stats-charts"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="stats-error"]')).toHaveCount(0);

    // The third response carries the snapshot; the loading block goes away.
    const tiles = page.locator('[data-testid="stats-tiles"] .cts-stats-tile');
    await expect(tiles).toHaveCount(9, { timeout: POLL_TIMEOUT });
    await expect(loading).toHaveCount(0);
    expect(searches.length).toBeGreaterThanOrEqual(3);

    // Nine tiles, exact grouped figures from the fixture.
    for (const [key, value] of Object.entries(READY_TILE_TEXT)) {
      await expect(
        page.locator(`[data-testid="stat-tile-${key}"] .cts-stats-tile-value`),
      ).toHaveText(value);
    }
    await expect(page.locator('[data-testid="stat-tile-stuck"] .cts-stats-tile-label')).toHaveText(
      "Stuck / abandoned (>24 h)",
    );

    // The snapshot's own age, not "now".
    const computedAt = page.locator('[data-testid="stats-computed-at"]');
    await expect(computedAt).toContainText("Data as of");
    await expect(computedAt.locator("time")).toHaveAttribute(
      "datetime",
      "2026-06-01T09:12:33.000Z",
    );

    await expectChartsPainted(page);

    // Every chart's data table is sliced to the default 12-month range,
    // oldest kept month first.
    for (const testid of CHART_TESTIDS) {
      await expect(chartRows(page, testid)).toHaveCount(12);
      await expect(chartRows(page, testid).first().locator("th")).toHaveText(
        MOCK_STATS_MONTHS[MOCK_STATS_MONTHS.length - 12],
      );
      await expect(chartHeaders(page, testid).first()).toHaveText("Month");
    }

    // The results chart is keyed by outcome, in the payload's bucket order.
    await expect(chartHeaders(page, "stats-chart-results")).toHaveText([
      "Month",
      "PASSED",
      "WARNING",
      "REVIEW",
      "FAILED",
      "SKIPPED",
      "NEVER_FINISHED",
    ]);

    // Under the charts, the plan names the server could not map to a spec
    // family — one row per entry the payload carries, collapsed until asked
    // for. Their runs are what the "Other / retired" segment is made of.
    const unresolved = MOCK_STATS_READY.data.unresolvedPlans;
    const unresolvedBlock = page.locator('[data-testid="stats-unresolved"]');
    await expect(unresolvedBlock).toHaveCount(1);
    await expect(unresolvedBlock.locator("summary")).toHaveText(
      `Plans not mapped to a spec family (${unresolved.length})`,
    );
    await expect(unresolvedBlock.locator("tbody tr")).toHaveCount(unresolved.length);
    await unresolvedBlock.locator("summary").click();
    await expect(unresolvedBlock.locator("tbody tr th")).toHaveText(
      unresolved.map((plan) => plan.planName),
    );
  });

  test("the initial request never asks the server to recompute", async ({ page }) => {
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page, respondReady);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");
    await expectChartsPainted(page);

    // A `?refresh=true` on load would make every page view kick off a
    // multi-minute whole-collection aggregation.
    expect(searches[0]).toBe("");
    // ...and a settled snapshot (refreshing: false) must not be polled at all.
    expect(searches).toHaveLength(1);
    await expect(page.locator('[data-testid="stats-charts"]')).toHaveAttribute(
      "aria-busy",
      "false",
    );
  });

  test("Refresh sends refresh=true, dims the charts, and clears on the next poll", async ({
    page,
  }) => {
    test.setTimeout(POLLING_TEST_TIMEOUT);
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page, respondRefreshingThenSettled);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");
    await expectChartsPainted(page);

    const charts = page.locator('[data-testid="stats-charts"]');
    const refresh = page.locator('[data-testid="stats-refresh"] button');
    await expect(charts).toHaveAttribute("aria-busy", "false");
    await expect(refresh).toBeEnabled();

    await refresh.click();

    await expect.poll(() => searches.length, { timeout: POLL_TIMEOUT }).toBeGreaterThan(1);
    expect(searches[1]).toBe("?refresh=true");

    // refreshing: true is stale-while-revalidate — the previous render stays
    // mounted and only dims, so there is no skeleton flash and no layout jump.
    await expect(charts).toHaveClass(/is-busy/);
    await expect(charts).toHaveAttribute("aria-busy", "true");
    await expect(page.locator('cts-spinner[label="Refreshing statistics"]')).toHaveCount(1);
    await expect(refresh).toBeDisabled();
    await expect(page.locator(".cts-stats-chart figure canvas")).toHaveCount(4);

    // The poll that follows carries no ?refresh and finds the snapshot
    // settled, which is what clears the busy state.
    await expect.poll(() => searches.length, { timeout: POLL_TIMEOUT }).toBeGreaterThan(2);
    expect(searches[2]).toBe("");
    await expect(charts).toHaveAttribute("aria-busy", "false", { timeout: POLL_TIMEOUT });
    await expect(charts).not.toHaveClass(/is-busy/);
    await expect(page.locator("cts-spinner")).toHaveCount(0);
    await expect(refresh).toBeEnabled();
  });

  test("a non-admin gets the admin-only alert, no charts, and no Statistics nav link", async ({
    page,
  }) => {
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page, respondForbidden);
    await setupCommonRoutes(page, { user: MOCK_USER });

    await page.goto("/statistics.html");

    const forbidden = page.locator('[data-testid="stats-forbidden"]');
    await expect(forbidden).toBeVisible();
    await expect(forbidden).toHaveAttribute("variant", "warning");
    await expect(forbidden).toContainText("Statistics are only available to administrators.");
    await expect(forbidden.locator(".oidf-alert-warning")).toHaveCount(1);

    // The 403 replaces the whole dashboard — no tiles, no filters, no plots.
    await expect(page.locator('[data-testid="stats-tiles"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="stats-charts"]')).toHaveCount(0);
    await expect(page.locator("canvas")).toHaveCount(0);
    // ...and a forbidden response is terminal: the page must not poll it.
    expect(searches).toHaveLength(1);

    // The navbar hides the link from non-admins (belt and braces — the 403 is
    // the authoritative check, the link is just discoverability).
    await expect(page.locator(".cts-navlink[href='plans.html']")).toBeVisible();
    await expect(page.locator(".cts-navlink[href='statistics.html']")).toHaveCount(0);
  });

  test("an admin's navbar links to Statistics and marks it current", async ({ page }) => {
    await setupFailFast(page);
    await setupStatisticsRoute(page, respondReady);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");

    const link = page.locator(".cts-navlink[href='statistics.html']");
    await expect(link).toBeVisible();
    await expect(link).toHaveText("Statistics");
    await expect(link).toHaveClass(/active/);
  });

  test("a database with no runs shows the empty state and zeroed tiles", async ({ page }) => {
    await setupFailFast(page);
    await setupStatisticsRoute(page, respondEmpty);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");

    const empty = page.locator('[data-testid="stats-empty"]');
    await expect(empty).toBeVisible();
    await expect(empty).toHaveAttribute("heading", "No test data yet");
    await expect(empty.locator(".oidf-empty-state-heading")).toHaveText("No test data yet");

    // The tiles still render (all zero) — the page is not blank, it is empty.
    await expect(page.locator('[data-testid="stats-tiles"] .cts-stats-tile')).toHaveCount(9);
    await expect(
      page.locator('[data-testid="stat-tile-totalTests"] .cts-stats-tile-value'),
    ).toHaveText("0");

    // Nothing to chart, so no chart frames and no filter row.
    await expect(page.locator('[data-testid="stats-charts"]')).toHaveCount(0);
    await expect(page.locator("canvas")).toHaveCount(0);
    await expect(page.locator('[data-testid="stats-range"]')).toHaveCount(0);
  });

  test("a 500 shows the server's message and Retry re-requests the snapshot", async ({ page }) => {
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page, respondErrorThenReady);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");

    const alert = page.locator('[data-testid="stats-error"]');
    await expect(alert).toBeVisible();
    await expect(alert).toHaveAttribute("variant", "danger");
    await expect(alert.locator(".oidf-alert-danger")).toHaveCount(1);
    await expect(alert).toContainText("MongoSocketReadException");
    await expect(page.locator('[data-testid="stats-charts"]')).toHaveCount(0);
    // The failure is terminal until the admin acts: no background polling.
    expect(searches).toHaveLength(1);

    await page.locator('[data-testid="stats-retry"] button').click();

    await expect.poll(() => searches.length, { timeout: POLL_TIMEOUT }).toBe(2);
    // Retry re-requests the snapshot; it must not force a recompute.
    expect(searches[1]).toBe("");
    await expectChartsPainted(page);
    await expect(alert).toHaveCount(0);
    await expect(page.locator('[data-testid="stats-tiles"] .cts-stats-tile')).toHaveCount(9);
  });

  test("the range and family filters rescope the charts but not the tiles", async ({ page }) => {
    await setupFailFast(page);
    await setupStatisticsRoute(page, respondReady);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");
    await expectChartsPainted(page);

    const runsHeaders = chartHeaders(page, "stats-chart-runs");
    const usersHeaders = chartHeaders(page, "stats-chart-users");
    const familySelect = page.locator('[data-testid="stats-family"]');
    const allTime = page.locator('[data-testid="stats-range"] button[data-range="all"]');
    const twelveMonths = page.locator('[data-testid="stats-range"] button[data-range="12m"]');

    // Default range is 12 months: "OpenID Connect Logout", busy only in the
    // first two months of the 14-month history, has nothing left in that
    // window, so only six categorical families plus the folded neutral tail
    // (OpenID Federation, No plan, Other / retired) render.
    await expect(twelveMonths).toHaveAttribute("aria-pressed", "true");
    await expect(allTime).toHaveAttribute("aria-pressed", "false");
    await expect(chartRows(page, "stats-chart-runs")).toHaveCount(12);
    await expect(chartRows(page, "stats-chart-runs").first().locator("th")).toHaveText(
      MOCK_STATS_MONTHS[2],
    );
    await expect(runsHeaders).toHaveCount(8);
    await expect(runsHeaders.last()).toHaveText("Other");
    await expect(
      page.locator("[data-testid='stats-chart-runs'] table thead th", {
        hasText: "OpenID Connect Logout",
      }),
    ).toHaveCount(0);

    // Only families that have ever run are offered — "Shared Signals
    // Framework" never did, so it is not in the list.
    await expect(familySelect).toHaveAttribute("aria-label", "Spec family");
    await expect(familySelect.locator("option")).toHaveCount(11);
    await expect(familySelect.locator("option").first()).toHaveText("All families");
    await expect(familySelect.locator("option[value='Shared Signals Framework']")).toHaveCount(0);
    await expect(familySelect.locator("option[value='FAPI2 Security Profile']")).toHaveCount(1);

    await allTime.click();

    // Widening the range brings back four months' worth of rows and the
    // "OpenID Connect Logout" column...
    await expect(allTime).toHaveAttribute("aria-pressed", "true");
    await expect(twelveMonths).toHaveAttribute("aria-pressed", "false");
    await expect(chartRows(page, "stats-chart-runs")).toHaveCount(14);
    await expect(chartRows(page, "stats-chart-runs").first().locator("th")).toHaveText(
      MOCK_STATS_MONTHS[0],
    );
    await expect(runsHeaders).toHaveCount(9);
    await expect(
      page.locator("[data-testid='stats-chart-runs'] table thead th", {
        hasText: "OpenID Connect Logout",
      }),
    ).toHaveCount(1);

    await twelveMonths.click();

    // ...and back to 12 months drops the same four months and the same
    // column again.
    await expect(twelveMonths).toHaveAttribute("aria-pressed", "true");
    await expect(allTime).toHaveAttribute("aria-pressed", "false");
    await expect(chartRows(page, "stats-chart-runs")).toHaveCount(12);
    await expect(chartRows(page, "stats-chart-users")).toHaveCount(12);
    await expect(chartRows(page, "stats-chart-runs").first().locator("th")).toHaveText(
      MOCK_STATS_MONTHS[2],
    );
    await expect(runsHeaders).toHaveCount(8);
    await expect(
      page.locator("[data-testid='stats-chart-runs'] table thead th", {
        hasText: "OpenID Connect Logout",
      }),
    ).toHaveCount(0);

    await familySelect.selectOption("FAPI2 Security Profile");

    // One family selected: charts 1-2 collapse to that single series...
    await expect(runsHeaders).toHaveText(["Month", "FAPI2 Security Profile"]);
    await expect(chartHeaders(page, "stats-chart-plans")).toHaveText([
      "Month",
      "FAPI2 Security Profile",
    ]);
    // ...chart 3 switches to that family's own outcome mix (still by bucket)...
    await expect(chartHeaders(page, "stats-chart-results").nth(1)).toHaveText("PASSED");
    // ...and chart 4 ignores the family filter entirely.
    await expect(usersHeaders).toHaveText(["Month", "Active", "New"]);
    // The range is still 12 months.
    await expect(chartRows(page, "stats-chart-runs")).toHaveCount(12);

    // The tiles are whole-database counters and are deliberately NOT scoped by
    // either filter.
    await expect(
      page.locator('[data-testid="stat-tile-totalTests"] .cts-stats-tile-value'),
    ).toHaveText(READY_TILE_TEXT.totalTests);
  });
});
