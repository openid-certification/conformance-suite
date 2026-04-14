import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, setupTestInfoRoute, wrapDataTablesResponse } from "./helpers/routes.js";
import { MOCK_PLAN_LIST } from "./fixtures/mock-plans.js";

test.describe("plans.html — Plans List", () => {
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
});
