import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  wrapDataTablesResponse,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_LOG_LIST } from "./fixtures/mock-log-list.js";

test.describe("logs.html — Logs List", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads and renders logs in DataTable (R27)", async ({ page }) => {
    await setupFailFast(page);

    // /api/log — DataTables server-side endpoint
    await page.route("**/api/log?*", (route) => {
      const envelope = wrapDataTablesResponse(MOCK_LOG_LIST, route.request().url());
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

  test("search triggers DataTable re-fetch with Enter key", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/log?*", (route) => {
      const envelope = wrapDataTablesResponse(MOCK_LOG_LIST, route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });

    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    // Wait for initial table load
    await expect(page.locator("#logsListing tbody tr").first()).toBeVisible();

    // Type search term and press Enter — set up waitForRequest BEFORE the
    // action so the listener is active when the request fires
    const searchInput = page.locator("div.dataTables_filter input");
    await searchInput.fill("rotate-keys");
    const searchRequest = page.waitForRequest(
      (req) => req.url().includes("/api/log?") && req.url().includes("search=rotate-keys"),
    );
    await searchInput.press("Enter");
    await searchRequest;
  });

  test("config button in log row opens config modal", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/log?*", (route) => {
      const envelope = wrapDataTablesResponse(MOCK_LOG_LIST, route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });

    // Config button fetches the plan to show its config
    await page.route("**/api/plan/plan-001**", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          _id: "plan-001",
          config: { "server.issuer": "https://op.example.com" },
        }),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    // Wait for rows
    await expect(page.locator("#logsListing tbody tr").first()).toBeVisible();

    // Click config button in the first row
    const configBtn = page.locator(".showConfigBtn").first();
    await expect(configBtn).toBeVisible();
    await configBtn.click();

    // Config modal opens
    const configModal = page.locator("#configModal");
    await expect(configModal).toBeVisible();
    await expect(page.locator("#config")).toContainText("server.issuer");

    // Close modal
    await configModal.locator('[data-bs-dismiss="modal"]').first().click();
    await expect(configModal).not.toBeVisible();
  });
});
