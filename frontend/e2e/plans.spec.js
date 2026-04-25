import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  wrapDataTablesResponse,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLAN_LIST } from "./fixtures/mock-plans.js";

/**
 * After U37 plans.html is driven by `<cts-data-table>` instead of jQuery
 * DataTables. The page keeps `id="plansListing"` on the host so the
 * `#plansListing tbody tr` selector still resolves — the cts-data-table
 * renders a `<table class="oidf-dt-table">` directly into its light DOM.
 */

test.describe("plans.html — Plans List", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads and renders plans in cts-data-table", async ({ page }) => {
    await setupFailFast(page);

    // /api/plan — DataTables-style server-side endpoint
    await page.route("**/api/plan?*", (route) => {
      const envelope = wrapDataTablesResponse(MOCK_PLAN_LIST, route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });

    // /api/info/:testId for the fetchTestResults cascade
    await setupTestInfoRoute(page);

    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    // Wait for the cts-data-table to render rows (the host is upgraded
    // first, then the imperative columns / cellRenderer setup triggers a
    // reload(), then the response paints data-row-index="N" rows).
    const rows = page.locator("#plansListing tbody tr[data-row-index]");
    await expect(rows.first()).toBeVisible();

    // Should show plan names
    await expect(page.locator("#plansListing")).toContainText(
      "oidcc-basic-certification-test-plan",
    );

    // Should show variant info
    await expect(page.locator("#plansListing")).toContainText("client_secret_basic");
  });

  test("config button opens modal with plan configuration", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan?*", (route) => {
      const envelope = wrapDataTablesResponse(MOCK_PLAN_LIST, route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    // Wait for rows to render
    await expect(page.locator("#plansListing tbody tr[data-row-index]").first()).toBeVisible();

    // Click the config button in the first row. The cts-data-table row
    // wires the click via the `cts-click` event that bubbles from the
    // inner <button>, so we click the inner button directly.
    const configBtn = page.locator(".showConfigBtn button").first();
    await expect(configBtn).toBeVisible();
    await configBtn.click();

    // Config modal should open with the plan's config JSON
    const configModal = page.locator("#configModal");
    await expect(configModal).toBeVisible();
    await expect(page.locator("#config")).toContainText("server.issuer");

    // Close the modal
    await configModal.locator(".oidf-modal-close").first().click();
    await expect(configModal).toBeHidden();
  });

  test("search button triggers cts-data-table re-fetch with search term", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan?*", (route) => {
      const envelope = wrapDataTablesResponse(MOCK_PLAN_LIST, route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    // Wait for initial table load
    await expect(page.locator("#plansListing tbody tr[data-row-index]").first()).toBeVisible();

    // Type a search term into the cts-data-table's search input. The
    // input lives inside the host as `.oidf-dt-search-input`.
    const searchInput = page.locator("#plansListing .oidf-dt-search-input");
    await expect(searchInput).toBeVisible();
    await searchInput.fill("fapi2");

    // Click the explicit-mode Search button. The cts-data-table commits
    // the search synchronously on `cts-click` and skips the 250ms debounce.
    const searchBtn = page.locator("#plansListing .oidf-dt-search-btn button");
    await expect(searchBtn).toBeVisible();
    const searchRequest = page.waitForRequest(
      (req) => req.url().includes("/api/plan?") && req.url().includes("search=fapi2"),
    );
    await searchBtn.click();
    await searchRequest;
  });
});
