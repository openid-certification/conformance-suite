import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, wrapDataTablesResponse } from "./helpers/routes.js";
import { MOCK_LOG_LIST } from "./fixtures/mock-log-list.js";

test.describe("logs.html — Logs List", () => {
  test("loads and renders logs in DataTable (R27)", async ({ page }) => {
    await setupFailFast(page);

    // /api/log — DataTables server-side endpoint
    await page.route("**/api/log?*", (route) => {
      const envelope = wrapDataTablesResponse(
        MOCK_LOG_LIST,
        route.request().url(),
      );
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });

    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    // Wait for DataTable to render rows
    const rows = page.locator("#logsListing tbody tr");
    await expect(rows.first()).toBeVisible();

    // Should show test names
    await expect(page.locator("#logsListing")).toContainText("oidcc-server");

    // Should show results
    await expect(page.locator("#logsListing")).toContainText("PASSED");
    await expect(page.locator("#logsListing")).toContainText("WARNING");
  });
});
