import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";

/**
 * Sample dashboard-stats fixtures. The real backend returns a
 * PaginationResponse envelope (`{ data: [...], recordsTotal }`); the
 * component also accepts plain arrays. We use plain arrays here so the
 * fixture shape stays scannable — the envelope path is exercised in
 * cts-plan-list's spec and in the dashboard stories.
 */
const STATS_PLANS = [{ _id: "plan-1" }, { _id: "plan-2" }, { _id: "plan-3" }];
const STATS_LOGS = [
  // INTERRUPTED is a terminal (stopped) state and CREATED is pre-execution;
  // neither should appear in the "Logs in progress" count, so the whitelist
  // filter on the component must include only RUNNING + WAITING. UNKNOWN as
  // a result counts as a failure, matching LogApi.java's existing
  // "FAILED || UNKNOWN" convention.
  { testId: "log-1", status: "FINISHED", result: "PASSED" },
  { testId: "log-2", status: "FINISHED", result: "PASSED" },
  { testId: "log-3", status: "FINISHED", result: "FAILED" },
  { testId: "log-4", status: "RUNNING", result: null },
  { testId: "log-5", status: "WAITING", result: null },
  { testId: "log-6", status: "INTERRUPTED", result: "FAILED" },
  { testId: "log-7", status: "CREATED", result: null },
  { testId: "log-8", status: "FINISHED", result: "UNKNOWN" },
];

/**
 * Register mocks for the two list endpoints the dashboard stats row hits.
 *
 * The dashboard always fires these fetches on connect (the constructor
 * defaults `isAuthenticated` to true, so the call fires before the page's
 * inline `/api/currentuser` probe resolves). Without these mocks the
 * setupFailFast catch-all would abort the requests and the test would
 * fail even when the assertion under test is unrelated to the stats row.
 *
 * Register BEFORE setupCommonRoutes / setupFailFast so Playwright's
 * reverse-order route matching gives the catch-all the last word for
 * other endpoints.
 *
 * @param {import('@playwright/test').Page} page
 * @param {object} [options]
 * @param {Array<object>} [options.plans] - Mock response for /api/plan*
 * @param {Array<object>} [options.logs] - Mock response for /api/log*
 * @param {number} [options.plansStatus] - HTTP status for /api/plan*
 * @param {number} [options.logsStatus] - HTTP status for /api/log*
 */
async function setupStatsRoutes(page, options = {}) {
  const plans = options.plans !== undefined ? options.plans : [];
  const logs = options.logs !== undefined ? options.logs : [];
  const plansStatus = options.plansStatus ?? 200;
  const logsStatus = options.logsStatus ?? 200;

  await page.route("**/api/plan*", (route) => {
    if (plansStatus >= 400) {
      return route.fulfill({ status: plansStatus, body: "" });
    }
    return route.fulfill({
      status: plansStatus,
      contentType: "application/json",
      body: JSON.stringify(plans),
    });
  });

  await page.route("**/api/log*", (route) => {
    if (logsStatus >= 400) {
      return route.fulfill({ status: logsStatus, body: "" });
    }
    return route.fulfill({
      status: logsStatus,
      contentType: "application/json",
      body: JSON.stringify(logs),
    });
  });
}

test.describe("index.html — Home page", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads with server info and user info (R25, R23)", async ({ page }) => {
    // Fail-fast registered first so it runs last (Playwright matches in reverse order)
    await setupFailFast(page);
    await setupStatsRoutes(page);
    await setupCommonRoutes(page);

    await page.goto("/index.html");

    // Server version info renders in the dashboard footer (cts-dashboard owns it)
    await expect(page.locator(".serverInfo")).toContainText("5.1.24-SNAPSHOT");

    // User info renders in the navbar. The avatar popover holds the
    // display name; the legacy inline "Logged in as X" cluster was removed
    // when cts-navbar adopted the avatar+popover pattern, so we assert on
    // the display name alone.
    const navbar = page.locator("cts-navbar");
    await expect(navbar).toContainText("Test User");

    // Navigation tiles are visible in the dashboard
    const homePage = page.locator("#homePage");
    await expect(
      homePage.locator('a.oidf-dashboard-tile[href="schedule-test.html"]'),
    ).toBeVisible();
    await expect(homePage.locator('a.oidf-dashboard-tile[href="logs.html"]')).toBeVisible();
    await expect(homePage.locator('a.oidf-dashboard-tile[href="plans.html"]')).toBeVisible();
  });

  test("unauthenticated state hides user info (R24)", async ({ page }) => {
    await setupFailFast(page);
    // Stats fetches still fire on connect (constructor defaults isAuthenticated
    // = true). The /api/currentuser 401 then flips the attribute and the stats
    // row re-renders to nothing; we just need handlers in place so the racing
    // fetches don't get caught by setupFailFast.
    await setupStatsRoutes(page);
    await setupCommonRoutes(page, { user: null });

    await page.goto("/index.html");

    // Cross-page contract: every wired page mounts a single <cts-toast-host>
    // for window.ctsToast(...). A silent removal of the mount from index.html
    // would otherwise pass all tests in this file. (Mirrors upload.spec.js:210.)
    await expect(page.locator("cts-toast-host")).toHaveCount(1);

    // Server info still renders
    await expect(page.locator(".serverInfo")).toContainText("5.1.24-SNAPSHOT");

    // Navbar should not contain login info
    const navbar = page.locator("cts-navbar");
    await expect(navbar).not.toContainText("Logged in as");

    // Public tiles are visible in the dashboard
    const homePage = page.locator("#homePage");
    await expect(
      homePage.locator('a.oidf-dashboard-tile[href="logs.html?public=true"]'),
    ).toBeVisible();
    await expect(
      homePage.locator('a.oidf-dashboard-tile[href="plans.html?public=true"]'),
    ).toBeVisible();

    // R3: the stats row is gated on isAuthenticated and must not render
    // for public users — even though the stats fetches fired briefly
    // during the initial render window.
    await expect(page.locator("#dashboardStats")).toHaveCount(0);
  });

  test("renders stats row with mocked counts when authenticated", async ({ page }) => {
    await setupFailFast(page);
    await setupStatsRoutes(page, { plans: STATS_PLANS, logs: STATS_LOGS });
    await setupCommonRoutes(page);

    await page.goto("/index.html");

    // Stats row mounts immediately (placeholder em-dashes) and then re-renders
    // once the fetches resolve. Wait for the real value to land.
    const planValue = page.locator('#dashboardStats [data-stat-key="plans"] .oidf-stat-value');
    await expect(planValue).toHaveText("3");

    // Logs tile mirrors the eight-row fixture (the fixture exercises the
    // INTERRUPTED + CREATED + UNKNOWN edge cases that the in-progress and
    // failures filters must respectively exclude and include).
    await expect(
      page.locator('#dashboardStats [data-stat-key="logs"] .oidf-stat-value'),
    ).toHaveText("8");

    // In-progress tile: only RUNNING + WAITING count. INTERRUPTED and
    // CREATED from the fixture must NOT be counted — that's the whitelist
    // filter contract.
    await expect(
      page.locator('#dashboardStats [data-stat-key="in-progress"] .oidf-stat-value'),
    ).toHaveText("2");

    // Failures tile: FAILED + UNKNOWN both count. The fixture has 2 FAILED
    // (log-3, log-6) and 1 UNKNOWN (log-8) → "3", with "fail" tone forwarded
    // to cts-stat. Visual colour is asserted in cts-stat's own stories.
    const failedTile = page.locator('#dashboardStats [data-stat-key="failed"]');
    await expect(failedTile.locator(".oidf-stat-value")).toHaveText("3");
    await expect(failedTile.locator("cts-stat")).toHaveAttribute("tone", "fail");
  });

  test("stats row degrades per-tile when only /api/log fails (R5)", async ({ page }) => {
    // Mixed-failure: plans succeeds, logs 500s. The plans tile must render
    // its count while the two logs-derived tiles fall back to "—". This
    // exercises the per-tile-independent degradation contract in
    // _buildStats — a regression that cascades one fetch failure to all
    // tiles would only be caught by this scenario.
    await setupFailFast(page);
    await setupStatsRoutes(page, { plans: STATS_PLANS, logsStatus: 500 });
    await setupCommonRoutes(page);

    await page.goto("/index.html");

    await expect(
      page.locator('#dashboardStats [data-stat-key="plans"] .oidf-stat-value'),
    ).toHaveText("3");
    await expect(
      page.locator('#dashboardStats [data-stat-key="logs"] .oidf-stat-value'),
    ).toHaveText("—");
    await expect(
      page.locator('#dashboardStats [data-stat-key="in-progress"] .oidf-stat-value'),
    ).toHaveText("—");
    await expect(
      page.locator('#dashboardStats [data-stat-key="failed"] .oidf-stat-value'),
    ).toHaveText("—");
  });

  test("stats tiles degrade to em-dash when both endpoints 5xx (R5)", async ({ page }) => {
    await setupFailFast(page);
    await setupStatsRoutes(page, { plansStatus: 500, logsStatus: 500 });
    await setupCommonRoutes(page);

    await page.goto("/index.html");

    // All four tiles fall back to the em-dash placeholder. We wait on the
    // navigation tile first as the "page is mounted" signal, then check
    // the stats tiles — they stay at "—" forever after the rejected
    // fetches resolve because the placeholder is what they started with.
    await expect(
      page.locator('#homePage a.oidf-dashboard-tile[href="schedule-test.html"]'),
    ).toBeVisible();

    const tiles = page.locator("#dashboardStats .oidf-dashboard-stat-tile .oidf-stat-value");
    await expect(tiles).toHaveCount(4);
    // Every tile is the em-dash. Use a regex anchored to the single character
    // so a future "0+" overflow indicator wouldn't accidentally satisfy this.
    for (let i = 0; i < 4; i += 1) {
      await expect(tiles.nth(i)).toHaveText("—");
    }

    // The navigation grid is unaffected by the stats failure (R5: dashboard
    // renders the rest of its content).
    const navTiles = page.locator("#homePage a.oidf-dashboard-tile");
    await expect(navTiles).toHaveCount(6);
  });

  test("stat tiles are clickable anchors with filter-driven hrefs", async ({ page }) => {
    // Each stat tile becomes an <a> wrapping the cts-stat. The "in progress"
    // and "with failures" tiles route through ?status= / ?result= URL params
    // on logs.html so users can drill from a summary count into the matching
    // rows. The aria-label combines label + value for assistive tech.
    await setupFailFast(page);
    await setupStatsRoutes(page, { plans: STATS_PLANS, logs: STATS_LOGS });
    await setupCommonRoutes(page);

    await page.goto("/index.html");

    // Wait for the real counts to land so the aria-label includes the value.
    await expect(
      page.locator('#dashboardStats [data-stat-key="plans"] .oidf-stat-value'),
    ).toHaveText("3");

    const expectations = [
      { key: "plans", href: "plans.html", labelStart: "Your test plans" },
      { key: "logs", href: "logs.html", labelStart: "Your test logs" },
      {
        key: "in-progress",
        href: "logs.html?status=running,waiting",
        labelStart: "Logs in progress",
      },
      { key: "failed", href: "logs.html?result=failed,unknown", labelStart: "Logs with failures" },
    ];
    for (const { key, href, labelStart } of expectations) {
      const tile = page.locator(
        `#dashboardStats a.oidf-dashboard-stat-tile[data-stat-key="${key}"]`,
      );
      await expect(tile).toHaveCount(1);
      await expect(tile).toHaveAttribute("href", href);
      const ariaLabel = await tile.getAttribute("aria-label");
      expect(ariaLabel).not.toBeNull();
      // Non-null asserted by the line above — narrow for the remaining checks.
      const label = /** @type {string} */ (ariaLabel);
      expect(label.startsWith(labelStart)).toBe(true);
      // aria-label should include the numeric value (or "loading" during
      // the placeholder phase). After the await above, real values are in.
      expect(/[0-9]+/.test(label) || label.includes("loading")).toBe(true);
    }
  });
});
