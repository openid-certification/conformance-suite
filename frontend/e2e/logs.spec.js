import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, wrapDataTablesResponse } from "./helpers/routes.js";

/** Mock log list entries — shape returned by /api/log */
const MOCK_LOG_LIST = [
  {
    testId: "test-log-001",
    testName: "oidcc-server",
    variant: { client_auth_type: "client_secret_basic", response_type: "code" },
    description: "Tests basic OpenID Connect server functionality",
    started: new Date(Date.now() - 86400000).toISOString(),
    planId: "plan-001",
    status: "FINISHED",
    result: "PASSED",
    owner: { sub: "12345", iss: "https://accounts.google.com" },
  },
  {
    testId: "test-log-002",
    testName: "oidcc-server-rotate-keys",
    variant: { client_auth_type: "client_secret_basic", response_type: "code" },
    description: "Tests key rotation behavior",
    started: new Date(Date.now() - 43200000).toISOString(),
    planId: "plan-001",
    status: "FINISHED",
    result: "WARNING",
    owner: { sub: "12345", iss: "https://accounts.google.com" },
  },
];

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
