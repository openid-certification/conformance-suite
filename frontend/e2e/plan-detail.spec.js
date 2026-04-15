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

  test("View Config button opens modal with plan configuration JSON", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Wait for the config button to render
    const configBtn = page.locator("#showConfigBtn");
    await expect(configBtn).toBeVisible();

    // Config modal should be hidden initially
    const configModal = page.locator("#configModal");
    await expect(configModal).not.toBeVisible();

    // Click the config button
    await configBtn.click();

    // Modal opens and shows the plan's config JSON
    await expect(configModal).toBeVisible();
    await expect(page.locator("#config")).toContainText("server.issuer");
    await expect(page.locator("#config")).toContainText("op.example.com");
    await expect(page.locator("#configTestId")).toContainText("plan-abc-123");

    // Close the modal
    await configModal.locator('[data-bs-dismiss="modal"]').first().click();
    await expect(configModal).not.toBeVisible();
  });

  test("module status badges render with tooltips after /api/info fetch", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await setupTestInfoRoute(page, {
      "test-inst-001": { ...MOCK_TEST_STATUS, testId: "test-inst-001", status: "FINISHED", result: "PASSED" },
      "test-inst-002": { ...MOCK_TEST_STATUS, testId: "test-inst-002", status: "FINISHED", result: "WARNING" },
      "test-inst-003": { ...MOCK_TEST_STATUS, testId: "test-inst-003", status: "FINISHED", result: "FAILED" },
    });

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Wait for module rows and status fetch to complete
    const firstRow = page.locator("#planItems .logItem").first();
    await expect(firstRow.locator(".testStatusResultBlock").first()).toBeVisible();

    // Status blocks should contain status text
    await expect(firstRow).toContainText("FINISHED");
    await expect(firstRow).toContainText("PASSED");

    // Bootstrap moves title to data-bs-original-title after tooltip init
    const tooltip = firstRow.locator('[data-bs-toggle="tooltip"]').first();
    await expect(tooltip).toBeVisible();
    const origTitle = await tooltip.getAttribute("data-bs-original-title");
    expect(origTitle || "").toBeTruthy();
  });

  test("delete plan button opens confirmation modal", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Delete button visible (plan is mutable)
    const deleteBtn = page.locator("#deleteMutablePlanBtn");
    await expect(deleteBtn).toBeVisible();

    // Confirmation modal should be hidden
    const deleteModal = page.locator("#deletePlanModal");
    await expect(deleteModal).not.toBeVisible();

    // Click delete → confirmation modal appears
    await deleteBtn.click();
    await expect(deleteModal).toBeVisible();
    await expect(deleteModal).toContainText("permanently and irrevocably");

    // Cancel → modal closes, no API call
    await deleteModal.locator('[data-bs-dismiss="modal"]').first().click();
    await expect(deleteModal).not.toBeVisible();
  });
});
