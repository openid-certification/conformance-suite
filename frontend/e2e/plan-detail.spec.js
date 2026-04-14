import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, setupTestInfoRoute } from "./helpers/routes.js";
import { MOCK_PLAN_DETAIL, MOCK_TEST_STATUS } from "./fixtures/mock-test-data.js";

test.describe("plan-detail.html — Plan Detail", () => {
  test("loads and renders plan info with modules (R28)", async ({ page }) => {
    await setupFailFast(page);

    // /api/plan/:planId
    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    // /api/info/:testId for each module with instances
    await setupTestInfoRoute(page, {
      "test-inst-001": { ...MOCK_TEST_STATUS, testId: "test-inst-001" },
      "test-inst-002": { ...MOCK_TEST_STATUS, testId: "test-inst-002", testName: "oidcc-server-rotate-keys" },
      "test-inst-003": { ...MOCK_TEST_STATUS, testId: "test-inst-003", testName: "oidcc-ensure-redirect-uri-in-authorization-request" },
    });

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Plan header renders
    const header = page.locator("#planHeader");
    await expect(header).toContainText("oidcc-basic-certification-test-plan");
    await expect(header).toContainText("plan-abc-123");
    await expect(header).toContainText("client_secret_basic");

    // Module list renders (4 modules in MOCK_PLAN_DETAIL)
    const moduleRows = page.locator("#planItems .logItem");
    await expect(moduleRows).toHaveCount(4);

    // Modules show their test names
    await expect(moduleRows.nth(0)).toContainText("oidcc-server");
    await expect(moduleRows.nth(1)).toContainText("oidcc-server-rotate-keys");

    // Action buttons visible
    await expect(page.locator("#showConfigBtn")).toBeVisible();
  });
});
