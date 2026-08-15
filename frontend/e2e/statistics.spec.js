import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLAN_LIST, MOCK_PLAN_INFO } from "./fixtures/mock-plans.js";
import { MOCK_ADMIN_USER, MOCK_USER } from "./fixtures/mock-users.js";
import {
  MOCK_STATS_EMPTY,
  MOCK_STATS_ERROR,
  MOCK_STATS_MONTHS,
  MOCK_STATS_PENDING,
  MOCK_STATS_READY,
  MOCK_STATS_REFRESHING,
  MOCK_STATS_WEEKS,
  invalidVariantMessage,
  statisticsOverviewFor,
  statisticsResponseFor,
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
 * Since phase 2 the SERVER slices, so these specs assert first and foremost on
 * the query the page SENT. The default route answers it the way the endpoint
 * would (`statisticsResponseFor`): the granularity picks the axis, the filters
 * zero what they exclude and narrow the dimensions, and a malformed
 * `variant.<name>` is a 400 — enough for the no-match state, the 400 alert and
 * the cascade to be driven end to end. It deliberately does not clip the axis
 * to `from` (see the fixture's header: Playwright runs on the real clock), so
 * a range preset is asserted on its request, not on a row count.
 *
 * The page also makes a second, deliberately unfiltered request on load — the
 * baseline the family colours and the family select come from. It is the only
 * one with an empty query string, which is how the routes below tell them
 * apart, and it is skipped entirely when the view IS the baseline (All time,
 * no filters).
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
 * The ten KPI tiles as the page must render them from MOCK_STATS_READY:
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
  publishedPlans: "45",
};

const CHART_TESTIDS = [
  "stats-chart-runs",
  "stats-chart-plans",
  "stats-chart-results",
  "stats-chart-users",
  "stats-chart-certified",
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
 * was asked for. The recorded strings are URL *search* parts (`""` for the
 * baseline request, `"?granularity=month&from=2025-09"` for a real one), and
 * the count of the non-baseline ones doubles as the poll counter.
 *
 * Responders are module-scope functions rather than inline closures so the
 * `if`s they need stay out of test bodies (`playwright/no-conditional-in-test`).
 * Both default to {@link respondSliced}, which answers the request the way the
 * endpoint would, so only a spec about the *state machine* (202, refresh, 500,
 * 403) has to name one.
 *
 * @param {import('@playwright/test').Page} page
 * @param {object} [options]
 * @param {(callIndex: number, url: URL) => StatsRouteResponse} [options.respond]
 *   - Called with the 1-based number of the FILTERED request and the parsed
 *   request URL.
 * @param {(callIndex: number, url: URL) => StatsRouteResponse} [options.respondBaseline]
 *   - Answers the unfiltered baseline request.
 * @returns {Promise<Array<string>>} Search strings, in request order; grows as
 *   the page polls.
 */
async function setupStatisticsRoute(page, options = {}) {
  const respond = options.respond || respondSliced;
  const respondBaseline = options.respondBaseline || respondSliced;
  /** @type {Array<string>} */
  const searches = [];
  let calls = 0;
  await page.route(ENDPOINT_GLOB, (route) => {
    const url = new URL(route.request().url());
    searches.push(url.search);
    // The baseline is a fixed extra request, so it must not move the counter
    // the per-call responders are driven by.
    const baseline = url.search === "";
    calls += baseline ? 0 : 1;
    const response = baseline ? respondBaseline(calls, url) : respond(calls, url);
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

/**
 * The queries of the filtered requests — everything the page asked for on
 * behalf of a control, with the load-time baseline left out.
 * @param {Array<string>} searches - The recorded search strings.
 * @returns {Array<string>} The non-empty ones, in request order.
 */
function filtered(searches) {
  return searches.filter((search) => search !== "");
}

/** @returns {StatsRouteResponse} A settled snapshot, every time. */
function respondReady() {
  return { status: 200, body: MOCK_STATS_READY };
}

/**
 * A settled snapshot sliced the way the server would slice it: the
 * granularity picks the axis (a weekly request must come back on a weekly
 * axis or the page has nothing to prove it asked), the filters zero the
 * families and narrow the dimensions they exclude, and a `variant.<name>`
 * that is not a name at all is a 400.
 * @param {number} callIndex - 1-based request number (unused).
 * @param {URL} url - The request URL.
 * @returns {StatsRouteResponse} The response for this call.
 */
function respondSliced(callIndex, url) {
  return statisticsResponseFor(url);
}

/**
 * A sliced snapshot whose certification-profile dimension is longer than a
 * distribution chart plots, so the "top N of M" note has something to say.
 * Built here rather than in the fixture: the fixture's four profiles are what
 * every other assertion counts, and this is one spec's shape.
 * @param {number} callIndex - 1-based request number (unused).
 * @param {URL} url - The request URL.
 * @returns {StatsRouteResponse} The response for this call.
 */
function respondManyCertProfiles(callIndex, url) {
  const body = statisticsOverviewFor(url);
  body.data.dimensions = {
    ...body.data.dimensions,
    certProfiles: Array.from({ length: 20 }, (_, i) => ({
      name: `Certification profile ${i + 1}`,
      users: 40 - i,
      plans: 100 - i,
    })),
  };
  return { status: 200, body };
}

/** @returns {StatsRouteResponse} No snapshot to serve, every time. */
function respondFailed() {
  return { status: 500, body: MOCK_STATS_ERROR };
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

/** One day, in milliseconds — the step both range helpers below count in. */
const DAY_MS = 86400000;

/**
 * The `from` a monthly preset resolves to, computed the way
 * `statistics-model.js` does it: the first of the month `back` months ago, in
 * UTC. Recomputed per assertion rather than frozen, because the page reads the
 * real clock and a fixed string would rot overnight.
 * @param {number} back - How many months before this one (11 for "12 months").
 * @returns {string} `YYYY-MM`.
 */
function monthsBack(back) {
  const now = new Date();
  return new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - back, 1))
    .toISOString()
    .slice(0, 7);
}

/**
 * The `from` a weekly preset resolves to: the Monday of the ISO week `back`
 * weeks before this one, in UTC (Sunday belongs to the week that began six
 * days earlier).
 * @param {number} back - How many weeks before this one (11 for "12 weeks").
 * @returns {string} `YYYY-MM-DD`.
 */
function weeksBack(back) {
  const now = new Date();
  const midnight = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
  const sinceMonday = (new Date(midnight).getUTCDay() + 6) % 7;
  return new Date(midnight - (sinceMonday + back * 7) * DAY_MS).toISOString().slice(0, 10);
}

/**
 * Stop a chart's entry animation and jump it to its final layout, then read
 * where one bar actually is. A click is hit-tested against where the marks
 * ARE, so a coordinate read mid-flight would miss.
 * @param {import('@playwright/test').Page} page
 * @param {string} testid - Chart wrapper test id, e.g. `stats-chart-runs`.
 * @returns {Promise<{x: number, y: number, family: string, period: string}>} The
 *   pixel to click, and what the click means.
 */
function settledBar(page, testid) {
  return page.evaluate((id) => {
    const host = /** @type {any} */ (document.querySelector(`[data-testid="${id}"] cts-chart`));
    const chart = host.chartInstance;
    chart.config.options.animation = false;
    chart.stop();
    chart.update("none");
    const datasetIndex = chart.data.datasets.findIndex(
      (/** @type {any} */ ds) => ds.label !== "Other",
    );
    const values = chart.data.datasets[datasetIndex].data;
    const index = values.indexOf(Math.max(...values));
    const bar = chart.getDatasetMeta(datasetIndex).data[index];
    return {
      x: bar.x,
      y: (bar.y + bar.base) / 2,
      family: chart.data.datasets[datasetIndex].label,
      period: chart.data.labels[index],
    };
  }, testid);
}

/**
 * Listen for the page's cancelable `cts-drill-down` and cancel it, so the URL
 * a click produced can be asserted without the navigation tearing the page
 * out from under the spec. Production has no listener, so a real click
 * navigates — which the click-through test covers.
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<() => Promise<Array<string>>>} Reads the URLs so far.
 */
async function interceptDrillDown(page) {
  await page.evaluate(() => {
    /** @type {Array<string>} */
    const urls = [];
    /** @type {any} */ (window).__drillDowns = urls;
    document.addEventListener("cts-drill-down", (event) => {
      event.preventDefault();
      urls.push(/** @type {any} */ (event).detail.url);
    });
  });
  return () => page.evaluate(() => /** @type {any} */ (window).__drillDowns);
}

/**
 * Wait until all four plots exist. The `<canvas>` lands in the DOM a beat
 * before Chart.js has been fetched and instantiated, so the element's
 * `chartInstance` getter is the reliable "the plot is painted" signal — and
 * asserting on it also proves the vendored bundle is really being served.
 * @param {import('@playwright/test').Page} page
 */
async function expectChartsPainted(page) {
  await expect(page.locator(".cts-stats-chart figure canvas")).toHaveCount(CHART_TESTIDS.length, {
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
    .toBe(CHART_TESTIDS.length);
}

test.describe("statistics.html — admin usage dashboard", () => {
  test.beforeEach(async ({ page }) => {
    watchPageErrors(page);
  });

  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
    expectNoPageErrors(page);
  });

  test("polls through 202 responses and then renders tiles and five charts", async ({ page }) => {
    test.setTimeout(POLLING_TEST_TIMEOUT);
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page, { respond: respondPendingTwiceThenReady });
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
    await expect(tiles).toHaveCount(10, { timeout: POLL_TIMEOUT });
    await expect(loading).toHaveCount(0);
    expect(filtered(searches).length).toBeGreaterThanOrEqual(3);

    // Ten tiles, exact grouped figures from the fixture.
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

    // Every chart is on the axis the SERVER sent — the page does not slice.
    for (const testid of CHART_TESTIDS) {
      await expect(chartRows(page, testid)).toHaveCount(MOCK_STATS_MONTHS.length);
      await expect(chartRows(page, testid).first().locator("th")).toHaveText(MOCK_STATS_MONTHS[0]);
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
    const searches = await setupStatisticsRoute(page, { respond: respondReady });
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");
    await expectChartsPainted(page);

    // A `?refresh=true` on load would make every page view kick off a
    // multi-minute whole-collection aggregation.
    expect(searches.every((search) => !search.includes("refresh"))).toBe(true);
    // Exactly two requests: the unfiltered baseline and the view itself...
    expect(searches).toHaveLength(2);
    expect(searches).toContain("");
    // ...and the view's one carries the default range, resolved to a period key.
    expect(filtered(searches)[0]).toMatch(/^\?granularity=month&from=\d{4}-\d{2}$/);
    // ...and a settled snapshot (refreshing: false) must not be polled at all.
    await expect(page).toHaveURL(/\?range=12m$/);
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
    const searches = await setupStatisticsRoute(page, { respond: respondRefreshingThenSettled });
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");
    await expectChartsPainted(page);

    const charts = page.locator('[data-testid="stats-charts"]');
    const refresh = page.locator('[data-testid="stats-refresh"] button');
    await expect(charts).toHaveAttribute("aria-busy", "false");
    await expect(refresh).toBeEnabled();

    const before = searches.length;
    await refresh.click();

    await expect.poll(() => searches.length, { timeout: POLL_TIMEOUT }).toBeGreaterThan(before);
    expect(searches[before]).toMatch(/^\?granularity=month&from=\d{4}-\d{2}&refresh=true$/);

    // refreshing: true is stale-while-revalidate — the previous render stays
    // mounted and only dims, so there is no skeleton flash and no layout jump.
    await expect(charts).toHaveClass(/is-busy/);
    await expect(charts).toHaveAttribute("aria-busy", "true");
    await expect(page.locator('cts-spinner[label="Refreshing statistics"]')).toHaveCount(1);
    await expect(refresh).toBeDisabled();
    await expect(page.locator(".cts-stats-chart figure canvas")).toHaveCount(CHART_TESTIDS.length);

    // The poll that follows carries no ?refresh and finds the snapshot
    // settled, which is what clears the busy state.
    await expect.poll(() => searches.length, { timeout: POLL_TIMEOUT }).toBeGreaterThan(before + 1);
    expect(searches[before + 1]).not.toContain("refresh");
    await expect(charts).toHaveAttribute("aria-busy", "false", { timeout: POLL_TIMEOUT });
    await expect(charts).not.toHaveClass(/is-busy/);
    await expect(page.locator("cts-spinner")).toHaveCount(0);
    await expect(refresh).toBeEnabled();
  });

  test("a non-admin gets the admin-only alert, no charts, and no Statistics nav link", async ({
    page,
  }) => {
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page, {
      respond: respondForbidden,
      respondBaseline: respondForbidden,
    });
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
    // ...and a forbidden response is terminal: the page must not poll it. Only
    // the baseline and the view itself were ever asked for.
    expect(searches).toHaveLength(2);

    // The navbar hides the link from non-admins (belt and braces — the 403 is
    // the authoritative check, the link is just discoverability).
    await expect(page.locator(".cts-navlink[href='plans.html']")).toBeVisible();
    await expect(page.locator(".cts-navlink[href='statistics.html']")).toHaveCount(0);
  });

  test("an admin's navbar links to Statistics and marks it current", async ({ page }) => {
    await setupFailFast(page);
    await setupStatisticsRoute(page, { respond: respondReady });
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");

    const link = page.locator(".cts-navlink[href='statistics.html']");
    await expect(link).toBeVisible();
    await expect(link).toHaveText("Statistics");
    await expect(link).toHaveClass(/active/);
  });

  test("a database with no runs shows the empty state and zeroed tiles", async ({ page }) => {
    await setupFailFast(page);
    await setupStatisticsRoute(page, { respond: respondEmpty, respondBaseline: respondEmpty });
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");

    // The default range is 12 months, so the empty state must not claim there
    // has never been any data — only that there is none in what is on screen.
    const empty = page.locator('[data-testid="stats-empty"]');
    await expect(empty).toBeVisible();
    await expect(empty).toHaveAttribute("heading", "Nothing in this range");
    await expect(empty.locator(".oidf-empty-state-heading")).toHaveText("Nothing in this range");

    // The tiles still render (all zero) — the page is not blank, it is empty.
    await expect(page.locator('[data-testid="stats-tiles"] .cts-stats-tile')).toHaveCount(10);
    await expect(
      page.locator('[data-testid="stat-tile-totalTests"] .cts-stats-tile-value'),
    ).toHaveText("0");

    // Nothing to chart, so no chart frames...
    await expect(page.locator('[data-testid="stats-charts"]')).toHaveCount(0);
    await expect(page.locator("canvas")).toHaveCount(0);
    // ...but the filter row stays: the range that emptied the view is the very
    // thing the reader has to be able to widen, and widening it all the way is
    // what turns "nothing here" into "nothing anywhere".
    await expect(page.locator('[data-testid="stats-range"]')).toHaveCount(1);
    await page.locator('[data-testid="stats-range-monthly"] button[data-range="all"]').click();
    await expect(empty).toHaveAttribute("heading", "No test data yet");
  });

  test("a 500 shows the server's message and Retry re-requests the snapshot", async ({ page }) => {
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page, {
      respond: respondErrorThenReady,
      respondBaseline: respondFailed,
    });
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");

    const alert = page.locator('[data-testid="stats-error"]');
    await expect(alert).toBeVisible();
    await expect(alert).toHaveAttribute("variant", "danger");
    await expect(alert.locator(".oidf-alert-danger")).toHaveCount(1);
    await expect(alert).toContainText("MongoSocketReadException");
    await expect(page.locator('[data-testid="stats-charts"]')).toHaveCount(0);
    // The failure is terminal until the admin acts: no background polling.
    expect(filtered(searches)).toHaveLength(1);

    await page.locator('[data-testid="stats-retry"] button').click();

    await expect.poll(() => filtered(searches).length, { timeout: POLL_TIMEOUT }).toBe(2);
    // Retry re-requests the snapshot; it must not force a recompute.
    expect(filtered(searches)[1]).not.toContain("refresh");
    await expectChartsPainted(page);
    await expect(alert).toHaveCount(0);
    await expect(page.locator('[data-testid="stats-tiles"] .cts-stats-tile')).toHaveCount(10);
  });

  test("every filter goes to the server and into the page URL", async ({ page }) => {
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");
    await expectChartsPainted(page);

    const familySelect = page.locator('[data-testid="stats-family"]');
    const planSelect = page.locator('[data-testid="stats-plan"]');
    const variantSelect = page.locator('[data-testid="stats-variant-client_auth_type"]');
    const certSelect = page.locator('[data-testid="stats-cert"]');
    const last = () => filtered(searches).at(-1);

    // Only families that have ever run are offered — "Shared Signals
    // Framework" never did, so it is not in the list. The options come from
    // the unfiltered baseline, so no filter can take them away.
    await expect(familySelect).toHaveAttribute("aria-label", "Spec family");
    await expect(familySelect.locator("option")).toHaveCount(11);
    await expect(familySelect.locator("option").first()).toHaveText("All families");
    await expect(familySelect.locator("option[value='Shared Signals Framework']")).toHaveCount(0);

    // A weekly preset is a different granularity, not a shorter range: the
    // axis, its label and its ticks all come back different.
    await page.locator('[data-testid="stats-range-weekly"] button[data-range="26w"]').click();
    await expect
      .poll(last, { timeout: POLL_TIMEOUT })
      .toMatch(/^\?granularity=week&from=\d{4}-\d{2}-\d{2}$/);
    await expect(page).toHaveURL(/\?range=26w$/);
    await expect(chartHeaders(page, "stats-chart-runs").first()).toHaveText("Week starting");
    await expect(chartRows(page, "stats-chart-runs")).toHaveCount(MOCK_STATS_WEEKS.length);
    // Compact labels: the day and the month, with the year only where it changes.
    await expect(chartRows(page, "stats-chart-runs").first().locator("th")).toHaveText(
      "8 Dec 2025",
    );

    await page.locator('[data-testid="stats-range-monthly"] button[data-range="12m"]').click();
    await expect
      .poll(last, { timeout: POLL_TIMEOUT })
      .toMatch(/^\?granularity=month&from=\d{4}-\d{2}$/);
    await expect(chartHeaders(page, "stats-chart-runs").first()).toHaveText("Month");

    await familySelect.selectOption("FAPI2 Security Profile");
    await expect.poll(last, { timeout: POLL_TIMEOUT }).toContain("family=FAPI2+Security+Profile");

    // The plan select is offered from the payload's dimensions; picking one
    // implies its family, so the two controls can never contradict each other.
    await expect(planSelect).toHaveAttribute("aria-label", "Test plan");
    await planSelect.selectOption("fapi2-security-profile-final-test-plan");
    await expect
      .poll(last, { timeout: POLL_TIMEOUT })
      .toContain("plan=fapi2-security-profile-final-test-plan");
    await expect(familySelect).toHaveValue("FAPI2 Security Profile");

    await variantSelect.selectOption("mtls");
    await expect.poll(last, { timeout: POLL_TIMEOUT }).toContain("variant.client_auth_type=mtls");

    await certSelect.selectOption("FAPI2 Security Profile Final");
    await expect
      .poll(last, { timeout: POLL_TIMEOUT })
      .toContain("cert=FAPI2+Security+Profile+Final");

    // Changing the family drops the filters that belonged to the old one:
    // a plan, a certification profile and the variant parameters all belong
    // to a family, and carrying them over leaves charts of zeros.
    await familySelect.selectOption("OID4VP");
    await expect
      .poll(last, { timeout: POLL_TIMEOUT })
      .toMatch(/^\?granularity=month&from=\d{4}-\d{2}&family=OID4VP$/);
    await expect(page).toHaveURL(/\?range=12m&family=OID4VP$/);
    await expect(certSelect).toHaveValue("");
    await expect(planSelect).toHaveValue("");

    await familySelect.selectOption("FAPI2 Security Profile");
    await planSelect.selectOption("fapi2-security-profile-final-test-plan");
    await variantSelect.selectOption("mtls");
    await certSelect.selectOption("FAPI2 Security Profile Final");
    await expect
      .poll(last, { timeout: POLL_TIMEOUT })
      .toContain("cert=FAPI2+Security+Profile+Final");

    // Everything is in the URL, so the view can be shared as it stands.
    const url = new URL(page.url());
    expect(url.searchParams.get("range")).toBe("12m");
    expect(url.searchParams.get("family")).toBe("FAPI2 Security Profile");
    expect(url.searchParams.get("plan")).toBe("fapi2-security-profile-final-test-plan");
    expect(url.searchParams.get("variant.client_auth_type")).toBe("mtls");
    expect(url.searchParams.get("cert")).toBe("FAPI2 Security Profile Final");

    // The tiles are whole-database counters and are deliberately NOT scoped by
    // any of it.
    await expect(
      page.locator('[data-testid="stat-tile-totalTests"] .cts-stats-tile-value'),
    ).toHaveText(READY_TILE_TEXT.totalTests);

    // Clear filters drops all of them and keeps the range.
    await page.locator('[data-testid="stats-clear-filters"] button').click();
    await expect
      .poll(last, { timeout: POLL_TIMEOUT })
      .toMatch(/^\?granularity=month&from=\d{4}-\d{2}$/);
    await expect(page).toHaveURL(/\?range=12m$/);
    await expect(page.locator('[data-testid="stats-clear-filters"]')).toHaveCount(0);
  });

  test("a click on a chart bar opens the plans list filtered to it", async ({ page }) => {
    await setupFailFast(page);
    await setupStatisticsRoute(page);
    // The landing page's own endpoints: the drill-down is only proven if the
    // plans listing really loads with the parameters the click produced.
    /** @type {Array<string>} */
    const planRequests = [];
    await page.route("**/api/plan*", (route) => {
      planRequests.push(route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_LIST),
      });
    });
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");
    await expectChartsPainted(page);

    // Where to click, and what the click means — read off the live chart so
    // the expectation cannot drift from the fixture.
    const target = await settledBar(page, "stats-chart-runs");

    await page
      .locator('[data-testid="stats-chart-runs"] canvas')
      .click({ position: { x: target.x, y: target.y } });

    await page.waitForURL(/plans\.html\?/);
    const landed = new URL(page.url());
    expect(landed.searchParams.get("family")).toBe(target.family);
    expect(landed.searchParams.get("from")).toBe(`${target.period}-01`);
    // Half-open: the upper bound is the next month's first day, so the month
    // clicked is included whole and the next one not at all.
    expect(landed.searchParams.get("to")).toMatch(/^\d{4}-\d{2}-01$/);
    expect(landed.searchParams.get("to")).not.toBe(`${target.period}-01`);

    // The listing asked the server for exactly that slice...
    await expect.poll(() => planRequests.length).toBeGreaterThan(0);
    const requested = new URL(planRequests[0]);
    expect(requested.searchParams.get("family")).toBe(target.family);
    expect(requested.searchParams.get("from")).toBe(`${target.period}-01`);
    // ...and says what it is showing.
    await expect(page.locator("[data-testid='plan-filter-family']")).toContainText(
      `Family: ${target.family}`,
    );
    await expect(page.locator("[data-testid='plan-filter-from']")).toBeVisible();
  });

  test("a deep link opens the view it names", async ({ page }) => {
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto(
      "/statistics.html?range=24m&family=OID4VP&plan=oid4vp-1final-verifier-test-plan" +
        "&variant.fapi_profile=plain_fapi",
    );
    await expectChartsPainted(page);

    // The very first request already carries what the link asked for — the
    // page must not render an unfiltered view and then correct itself.
    expect(filtered(searches)[0]).toMatch(
      /^\?granularity=month&from=\d{4}-\d{2}&family=OID4VP&plan=oid4vp-1final-verifier-test-plan&variant\.fapi_profile=plain_fapi$/,
    );
    await expect(
      page.locator('[data-testid="stats-range-monthly"] button[data-range="24m"]'),
    ).toHaveAttribute("aria-pressed", "true");
    await expect(page.locator('[data-testid="stats-family"]')).toHaveValue("OID4VP");
    await expect(page.locator('[data-testid="stats-plan"]')).toHaveValue(
      "oid4vp-1final-verifier-test-plan",
    );
    await expect(page.locator('[data-testid="stats-variant-fapi_profile"]')).toHaveValue(
      "plain_fapi",
    );
    await expect(page.locator('[data-testid="stats-clear-filters"]')).toHaveCount(1);

    // The same contract at the other granularity: a weekly range in the link
    // is a weekly request, and the preset strip says which one is on.
    await page.goto("/statistics.html?range=26w&family=FAPI2+Security+Profile");
    await expectChartsPainted(page);
    expect(filtered(searches).at(-1)).toBe(
      `?granularity=week&from=${weeksBack(25)}&family=FAPI2+Security+Profile`,
    );
    await expect(
      page.locator('[data-testid="stats-range-weekly"] button[data-range="26w"]'),
    ).toHaveAttribute("aria-pressed", "true");
    await expect(page.locator('[data-testid="stats-family"]')).toHaveValue(
      "FAPI2 Security Profile",
    );
    await expect(chartHeaders(page, "stats-chart-runs").first()).toHaveText("Week starting");
  });

  test("each range preset asks the server for the axis it names", async ({ page }) => {
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");
    await expectChartsPainted(page);
    const last = () => filtered(searches).at(-1);

    // The default view is two requests and no more: the unfiltered baseline
    // (the colour slots and the family options) and the twelve-month slice.
    expect(searches).toHaveLength(2);
    expect(searches).toContain("");
    expect(last()).toBe(`?granularity=month&from=${monthsBack(11)}`);

    // A weekly preset is a different granularity, not a shorter range: the
    // bound is the Monday eleven weeks back and the axis comes back keyed by
    // week, which the category column and its labels both say.
    await page.locator('[data-testid="stats-range-weekly"] button[data-range="12w"]').click();
    await expect
      .poll(last, { timeout: POLL_TIMEOUT })
      .toBe(`?granularity=week&from=${weeksBack(11)}`);
    await expect(page).toHaveURL(/\?range=12w$/);
    await expect(chartHeaders(page, "stats-chart-runs").first()).toHaveText("Week starting");
    // Compact labels: the day and the month, with the year only where it changes.
    await expect(chartRows(page, "stats-chart-runs").first().locator("th")).toHaveText(
      "8 Dec 2025",
    );
    await expect(chartRows(page, "stats-chart-runs")).toHaveCount(MOCK_STATS_WEEKS.length);

    await page.locator('[data-testid="stats-range-monthly"] button[data-range="24m"]').click();
    await expect
      .poll(last, { timeout: POLL_TIMEOUT })
      .toBe(`?granularity=month&from=${monthsBack(23)}`);
    await expect(page).toHaveURL(/\?range=24m$/);
    await expect(chartHeaders(page, "stats-chart-runs").first()).toHaveText("Month");
    await expect(chartRows(page, "stats-chart-runs").first().locator("th")).toHaveText(
      MOCK_STATS_MONTHS[0],
    );

    // "All time" is the one preset with no lower bound: `from` is dropped
    // rather than sent empty, and `to` is never sent by any of them — the
    // server's axis already ends at the period containing today.
    await page.locator('[data-testid="stats-range-monthly"] button[data-range="all"]').click();
    await expect.poll(last, { timeout: POLL_TIMEOUT }).toBe("?granularity=month");
    await expect(page).toHaveURL(/\?range=all$/);
    expect(searches.some((search) => search.includes("to="))).toBe(false);
  });

  test("the whole-history view is its own baseline and is fetched once", async ({ page }) => {
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html?range=all");
    await expectChartsPainted(page);

    // Unfiltered and all-time IS the baseline, so asking for it twice would
    // fetch the same payload twice. The empty-query request is not made.
    expect(searches).toEqual(["?granularity=month"]);
    // ...and the family select is still fully populated, from that one payload.
    await expect(page.locator('[data-testid="stats-family"] option')).toHaveCount(11);
  });

  test("a filter that matches nothing says so, and one click clears it", async ({ page }) => {
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    // A family that has never run: the server answers with the full axis and a
    // zero in every cell, which is what a no-match payload looks like.
    await page.goto("/statistics.html?family=Shared+Signals+Framework");

    const noMatch = page.locator('[data-testid="stats-no-match"]');
    await expect(noMatch).toBeVisible();
    await expect(noMatch).toHaveAttribute("heading", "No test plans match these filters");
    // Five charts of zeros are what this state exists to replace.
    await expect(page.locator('[data-testid="stats-charts"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="stats-empty"]')).toHaveCount(0);

    // The tiles and the filter row stay: they are the context for the sentence
    // and the way out of it. So do the sections the filters do not scope — a
    // reader has to be able to see that the database itself is not empty.
    await expect(page.locator('[data-testid="stats-tiles"] .cts-stats-tile')).toHaveCount(10);
    await expect(page.locator('[data-testid="stats-filters"]')).toHaveCount(1);
    await expect(page.locator('[data-testid="stats-heatmap"]')).toHaveCount(1);
    await expect(page.locator('[data-testid="stats-hosts"]')).toHaveCount(1);
    // The distributions are counted under the whole query, so under a no-match
    // they would be empty cards restating what the sentence already says.
    await expect(page.locator('[data-testid="stats-distributions"]')).toHaveCount(0);

    await page.locator('[data-testid="stats-no-match-clear"] button').click();

    await expect
      .poll(() => filtered(searches).at(-1), { timeout: POLL_TIMEOUT })
      .toBe(`?granularity=month&from=${monthsBack(11)}`);
    await expect(page).toHaveURL(/\?range=12m$/);
    await expect(noMatch).toHaveCount(0);
    await expectChartsPainted(page);
  });

  test("a 400 shows the server's message and offers a reset instead of a retry", async ({
    page,
  }) => {
    await setupFailFast(page);
    const searches = await setupStatisticsRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    // The realistic way to a 400: a hand-edited link whose variant parameter
    // NAME is not one. The server never rejects a value.
    await page.goto("/statistics.html?family=OID4VP&variant.bad%20name=mtls");

    const alert = page.locator('[data-testid="stats-error"]');
    await expect(alert).toBeVisible();
    await expect(alert).toHaveAttribute("variant", "danger");
    await expect(alert).toContainText(invalidVariantMessage("bad name"));
    await expect(page.locator('[data-testid="stats-charts"]')).toHaveCount(0);

    // Retrying a request the server refused is pointless, so the way out is to
    // drop the filters, not to ask again.
    await expect(page.locator('[data-testid="stats-reset-filters"]')).toHaveCount(1);
    await expect(page.locator('[data-testid="stats-retry"]')).toHaveCount(0);
    // A 400 is terminal until the admin acts: nothing is polled.
    expect(filtered(searches)).toHaveLength(1);

    await page.locator('[data-testid="stats-reset-filters"] button').click();

    await expect
      .poll(() => filtered(searches).at(-1), { timeout: POLL_TIMEOUT })
      .toBe(`?granularity=month&from=${monthsBack(11)}`);
    await expect(page).toHaveURL(/\?range=12m$/);
    await expect(alert).toHaveCount(0);
    await expectChartsPainted(page);
  });

  test("the sections under the charts render from the payload", async ({ page }) => {
    await setupFailFast(page);
    await setupStatisticsRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");
    await expectChartsPainted(page);

    // Storage: one tile per collection, the document count as the value and
    // the three sizes as the hint, formatted in binary units.
    const storage = page.locator('[data-testid="stats-storage"] .cts-stats-tile');
    await expect(storage).toHaveCount(3);
    const testInfo = page.locator('[data-testid="stat-storage-TEST_INFO"]');
    await expect(testInfo.locator(".cts-stats-tile-value")).toHaveText("91,800");
    await expect(testInfo.locator(".cts-stats-tile-label")).toHaveText("TEST_INFO documents");
    await expect(testInfo.locator(".cts-stats-tile-hint")).toContainText(
      "1.7 GB data · 581.7 MB on disk · 91.6 MB indexes",
    );

    // Distributions: the certification-profile and entity charts are always
    // there; the variant small multiples are withheld until a family or plan
    // narrows the view, because unfiltered the suite offers dozens of them.
    await expect(page.locator('[data-testid="stats-distributions"]')).toHaveCount(1);
    await expect(page.locator('[data-testid="stats-dist-variants"]')).toContainText(
      "Select a family or plan to see variant usage.",
    );
    await expect(page.locator('[data-testid^="stats-dist-variant-"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="stats-dist-certs"] tbody tr')).toHaveCount(4);
    await expect(page.locator('[data-testid="stats-dist-entities"] tbody tr')).toHaveCount(3);
    // Ranked by the measure that is PLOTTED, whatever order the server sent.
    await expect(
      page.locator('[data-testid="stats-dist-certs"] tbody tr').first().locator("th"),
    ).toHaveText("FAPI2 Security Profile Final");

    // The heatmap: 7 x 24 cells, a caption that names the total and the fact
    // that only the range scopes it, and a linear table twin.
    const heatmap = page.locator('[data-testid="stats-heatmap"]');
    await expect(heatmap.locator(".cts-heatmap-cell")).toHaveCount(7 * 24);
    await expect(heatmap.locator(".cts-heatmap-caption")).toContainText("9,918 runs in this range");
    await expect(heatmap.locator(".cts-heatmap-caption")).toContainText(
      "All hours are UTC. Sliced by the selected range (12 months) only",
    );
    await expect(heatmap.locator("table tbody tr")).toHaveCount(7);
    await expect(heatmap.locator("table thead th")).toHaveCount(25);

    // External servers: all-time, so a disclosure rather than a section.
    const hosts = page.locator('[data-testid="stats-hosts"]');
    await expect(hosts.locator("summary")).toHaveText("External servers under test (6)");
    await expect(hosts.locator("tbody tr")).toHaveCount(6);
    await expect(hosts.locator("tbody tr").first().locator("th")).toHaveText("as.example.com");

    // Narrowing the view is what brings the variant small multiples out — one
    // per parameter with something to choose between, so the single-valued
    // `client_registration` stays out of it.
    await page.locator('[data-testid="stats-family"]').selectOption("FAPI2 Security Profile");
    await expect(page.locator('[data-testid^="stats-dist-variant-"]')).toHaveCount(3);
    await expect(page.locator('[data-testid="stats-dist-variant-client_auth_type"]')).toHaveCount(
      1,
    );
    await expect(
      page.locator('[data-testid="stats-dist-variant-client_registration"]'),
    ).toHaveCount(0);
  });

  test("a distribution longer than the plot says where the rest is", async ({ page }) => {
    await setupFailFast(page);
    await setupStatisticsRoute(page, { respond: respondManyCertProfiles });
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");
    await expectChartsPainted(page);

    // Capping the bars hides nothing: the note says how many were plotted and
    // the data table still carries every row.
    const certs = page.locator('[data-testid="stats-dist-certs"]');
    await expect(certs.locator('[data-testid="cts-chart-more"]')).toHaveText(
      "Showing the top 12 of 20; the rest are in the data table.",
    );
    await expect(certs.locator("tbody tr")).toHaveCount(20);
  });

  test("a drill-down names the slice behind the mark that was activated", async ({ page }) => {
    await setupFailFast(page);
    await setupStatisticsRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/statistics.html");
    await expectChartsPainted(page);
    const drillDowns = await interceptDrillDown(page);

    // The keyboard route: a data-table row is a PERIOD, not a series, so it
    // drills into the whole column under the filters already set — here, none.
    const runs = page.locator('[data-testid="stats-chart-runs"]');
    await runs.locator("details summary").click();
    const rowLink = runs.locator("tbody tr").nth(2).locator("button.cts-chart-row-link");
    const period = await rowLink.innerText();
    await expect(rowLink).toHaveAttribute("aria-label", `List the test plans in ${period}`);
    await rowLink.click();

    await expect.poll(async () => (await drillDowns()).length).toBe(1);
    const byPeriod = new URL((await drillDowns())[0], page.url());
    expect(byPeriod.pathname).toBe("/plans.html");
    expect(byPeriod.searchParams.get("from")).toBe(`${period}-01`);
    expect(byPeriod.searchParams.get("to")).toMatch(/^\d{4}-\d{2}-01$/);
    expect(byPeriod.searchParams.get("family")).toBeNull();

    // The pointer route: a bar names its own series, so the family comes from
    // the dataset that was clicked.
    const target = await settledBar(page, "stats-chart-plans");
    await page
      .locator('[data-testid="stats-chart-plans"] canvas')
      .click({ position: { x: target.x, y: target.y } });

    await expect.poll(async () => (await drillDowns()).length).toBe(2);
    const bySeries = new URL((await drillDowns())[1], page.url());
    expect(bySeries.searchParams.get("family")).toBe(target.family);
    expect(bySeries.searchParams.get("from")).toBe(`${target.period}-01`);

    // The results chart drills down by PERIOD only — its datasets are result
    // buckets and a plan has no single result — so its label says so before
    // the click, rather than leaving "FAILED" landing on every plan of the
    // month looking like a bug.
    const results = page.locator('[data-testid="stats-chart-results"]');
    await results.locator("details summary").click();
    const resultRow = results.locator("tbody tr").nth(2).locator("button.cts-chart-row-link");
    const resultPeriod = await resultRow.innerText();
    await expect(resultRow).toHaveAttribute(
      "aria-label",
      `List all test plans, whatever their result, in ${resultPeriod}`,
    );

    // Cancelling the event is what keeps the page here; production has no
    // listener, so the same click navigates (see the click-through test).
    await expect(page).toHaveURL(/statistics\.html/);
  });
});
