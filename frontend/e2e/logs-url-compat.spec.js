import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_LOG_LIST } from "./fixtures/mock-log-list.js";

/**
 * U1 — URL-compatibility gate for logs.html (page-level layer).
 *
 * Locks the OBSERVABLE URL contract of the logs page BEFORE any My/Published
 * tab change lands (plan docs/plans/2026-05-28-004-feat-plans-page-as-home-plan.md,
 * R25/AE7). Legacy assertions pass against today's behavior; target assertions
 * are `test.fixme` so CI stays green until the owning unit lands.
 *
 * SERVER-LEVEL ASSERTIONS ARE DEFERRED TO U10 (see plans-url-compat.spec.js).
 * No `/` / `/index.html` redirect or OTT assertion lives here — those are
 * server-observable and asserted by U10's HomeRoutingTest.java.
 *
 * Route ordering: setupFailFast() FIRST; specific routes after; all before
 * page.goto(). logs.html loads fapi.ui.js, which fires
 * api/ui/spec_links?public=true at parse time (covered by setupCommonRoutes).
 */

/**
 * Record every /api/log request URL and serve MOCK_LOG_LIST in the 1000-row
 * envelope. Returns the recorded-URL array so tests can assert which dataset
 * (My vs Published) was fetched.
 *
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<string[]>} requested /api/log URLs, in order
 */
async function recordLogRoute(page) {
  /** @type {string[]} */
  const logRequests = [];
  await page.route("**/api/log?*", (route) => {
    logRequests.push(route.request().url());
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        draw: 1,
        recordsTotal: MOCK_LOG_LIST.length,
        recordsFiltered: MOCK_LOG_LIST.length,
        data: MOCK_LOG_LIST,
      }),
    });
  });
  // /api/plan/<id> name resolution fired per unique planId by cts-log-list.
  await page.route("**/api/plan/*", (route) => {
    const planId = new URL(route.request().url()).pathname.replace("/api/plan/", "");
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ _id: planId, planName: `mock-plan-name-${planId}` }),
    });
  });
  return logRequests;
}

const ITEM = '#logsListing [data-testid="log-list-item"]';

// ---------------------------------------------------------------------------
// GROUP A — LEGACY (active now; MUST PASS against current behavior)
// ---------------------------------------------------------------------------
test.describe("logs.html URL compat — GROUP A: legacy (active)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("?public=true renders the Published logs view (fetches /api/log?public=true)", async ({
    page,
  }) => {
    await setupFailFast(page);
    const logRequests = await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?public=true");

    await expect(page.locator(ITEM).first()).toBeVisible();

    // The list fetched the Published dataset — every /api/log request carries
    // public=true. is-public is set before the component's connectedCallback
    // fetch fires (logs.html mounts the element imperatively after reading the
    // param), so there is no flash of the My dataset.
    expect(logRequests.length).toBeGreaterThan(0);
    expect(logRequests[0]).toContain("public=true");
    expect(logRequests.every((u) => u.includes("public=true"))).toBe(true);
  });

  test("?status=running,waiting lands with the status filter applied on initial load", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=running,waiting");

    // Filter applied on first paint: only RUNNING + WAITING rows render.
    await expect(page.locator(ITEM)).toHaveCount(2);
    await expect(page.locator("#logsListing")).toContainText("fapi2-running");
    await expect(page.locator("#logsListing")).toContainText("fapi2-waiting");
    await expect(page.locator("#logsListing")).not.toContainText("oidcc-server-rotate-keys");

    // Active-filter summary confirms the chips booted applied (not via a
    // post-load interaction).
    const summary = page.locator('#logsListing [data-testid="active-filter-summary"]');
    await expect(summary).toBeVisible();
    await expect(summary).toContainText("Status: running or waiting");
  });

  test("?result=failed,unknown lands with the result filter applied on initial load", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?result=failed,unknown");

    // 3 rows match on first paint: 2 UNKNOWN (running, waiting) + 1 FAILED.
    await expect(page.locator(ITEM)).toHaveCount(3);

    const summary = page.locator('#logsListing [data-testid="active-filter-summary"]');
    await expect(summary).toBeVisible();
    await expect(summary).toContainText("Result: failed or unknown");
  });
});

// ---------------------------------------------------------------------------
// GROUP B — TARGET (now ACTIVE; flipped from test.fixme when U6 landed the
// My/Published tabs on logs.html). Both assertions are owned by U6.
// ---------------------------------------------------------------------------
test.describe("logs.html URL compat — GROUP B: target (active)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  // OWNER: U6 (My/Published tabs + URL sync on logs.html). ACTIVE as of U6.
  test("clicking My <-> Published keeps the URL in sync (?public=true added/removed) (owning unit U6)", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");
    await expect(page.locator(ITEM).first()).toBeVisible();

    await page.locator("cts-view-tabs a[data-view='published']").click();
    await page.waitForFunction(() => window.location.search.includes("public=true"));
    expect(page.url()).toContain("public=true");

    await page.locator("cts-view-tabs a[data-view='my']").click();
    await page.waitForFunction(() => !window.location.search.includes("public="));
    expect(page.url()).not.toContain("public=");
  });

  // OWNER: U6 (back/forward popstate restores tab + dataset on logs.html). ACTIVE as of U6.
  test("back/forward restores the prior tab and dataset (owning unit U6)", async ({ page }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");
    await expect(page.locator(ITEM).first()).toBeVisible();

    await page.locator("cts-view-tabs a[data-view='published']").click();
    await page.waitForFunction(() => window.location.search.includes("public=true"));

    await page.goBack();
    await page.waitForFunction(() => !window.location.search.includes("public="));
    await expect(
      page.locator("cts-view-tabs a[data-view='my'][aria-current='page']"),
    ).toBeVisible();

    await page.goForward();
    await page.waitForFunction(() => window.location.search.includes("public=true"));
    await expect(
      page.locator("cts-view-tabs a[data-view='published'][aria-current='page']"),
    ).toBeVisible();
  });
});
