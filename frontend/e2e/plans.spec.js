import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, setupTestInfoRoute, wrapDataTablesResponse, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_PLAN_LIST } from "./fixtures/mock-plans.js";

test.describe("plans.html — Plans List", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads and renders plans in DataTable (R26)", async ({ page }) => {
    await setupFailFast(page);

    // /api/plan — DataTables server-side endpoint
    await page.route("**/api/plan?*", (route) => {
      const envelope = wrapDataTablesResponse(
        MOCK_PLAN_LIST,
        route.request().url(),
      );
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

    // Wait for DataTable to render rows
    const rows = page.locator("#plansListing tbody tr");
    await expect(rows.first()).toBeVisible();

    // Should show plan names
    await expect(page.locator("#plansListing")).toContainText(
      "oidcc-basic-certification-test-plan",
    );

    // Should show variant info
    await expect(page.locator("#plansListing")).toContainText(
      "client_secret_basic",
    );
  });

  test("config button opens modal with plan configuration", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan?*", (route) => {
      const envelope = wrapDataTablesResponse(
        MOCK_PLAN_LIST,
        route.request().url(),
      );
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
    await expect(page.locator("#plansListing tbody tr").first()).toBeVisible();

    // Click the config button in the first row
    const configBtn = page.locator(".showConfigBtn").first();
    await expect(configBtn).toBeVisible();
    await configBtn.click();

    // Config modal should open with the plan's config JSON
    const configModal = page.locator("#configModal");
    await expect(configModal).toBeVisible();
    await expect(page.locator("#config")).toContainText("server.issuer");

    // Close the modal
    await configModal.locator('[data-bs-dismiss="modal"]').first().click();
    await expect(configModal).not.toBeVisible();
  });

  test("search button triggers DataTable re-fetch with search term", async ({ page }) => {
    let searchTerm = "";

    await setupFailFast(page);

    await page.route("**/api/plan?*", (route) => {
      const url = new URL(route.request().url());
      searchTerm = url.searchParams.get("search") || "";
      const envelope = wrapDataTablesResponse(
        MOCK_PLAN_LIST,
        route.request().url(),
      );
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
    await expect(page.locator("#plansListing tbody tr").first()).toBeVisible();

    // Type a search term into the DataTables search input
    const searchInput = page.locator("div.dataTables_filter input");
    await expect(searchInput).toBeVisible();
    await searchInput.fill("fapi2");

    // Click the search button
    const searchBtn = page.locator("div.dataTables_filter button");
    await expect(searchBtn).toBeVisible();
    await searchBtn.click();

    // Verify the search parameter was sent to the API
    await page.waitForTimeout(500);
    expect(searchTerm).toBe("fapi2");
  });
});
