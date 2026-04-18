import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLAN_DETAIL, MOCK_TEST_STATUS } from "./fixtures/mock-test-data.js";
import { MOCK_ADMIN_USER } from "./fixtures/mock-users.js";

test.describe("plan-detail.html — Plan Detail", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

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
      "test-inst-002": {
        ...MOCK_TEST_STATUS,
        testId: "test-inst-002",
        testName: "oidcc-server-rotate-keys",
      },
      "test-inst-003": {
        ...MOCK_TEST_STATUS,
        testId: "test-inst-003",
        testName: "oidcc-ensure-redirect-uri-in-authorization-request",
      },
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
      "test-inst-001": {
        ...MOCK_TEST_STATUS,
        testId: "test-inst-001",
        status: "FINISHED",
        result: "PASSED",
      },
      "test-inst-002": {
        ...MOCK_TEST_STATUS,
        testId: "test-inst-002",
        status: "FINISHED",
        result: "WARNING",
      },
      "test-inst-003": {
        ...MOCK_TEST_STATUS,
        testId: "test-inst-003",
        status: "FINISHED",
        result: "FAILED",
      },
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

  test("publish button opens confirmation modal with secrets warning (R1)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Publish button visible for admin user on unpublished plan
    const publishBtn = page.locator("#publishBtn");
    await expect(publishBtn).toBeVisible();

    // Modal hidden initially
    const publishModal = page.locator("#publishModal");
    await expect(publishModal).not.toBeVisible();

    // Click publish → modal opens with secrets warning
    await publishBtn.click();
    await expect(publishModal).toBeVisible();
    await expect(publishModal).toContainText(
      "keys, secrets, and all other test information publicly visible",
    );
  });

  test("publish confirm sends POST /api/plan/:id/publish and navigates (R2)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123?*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_PLAN_DETAIL, publish: "everything" }),
      }),
    );

    await page.route("**/api/plan/plan-abc-123", (route) => {
      if (route.request().method() === "DELETE") {
        return route.fulfill({ status: 200, body: "" });
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      });
    });

    await page.route("**/api/plan/plan-abc-123/publish", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_PLAN_DETAIL, publish: "everything" }),
      }),
    );

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Open publish modal
    await page.locator("#publishBtn").click();
    await expect(page.locator("#publishModal")).toBeVisible();

    // Set up request interception BEFORE clicking
    const publishRequest = page.waitForRequest(
      (req) => req.url().includes("/api/plan/plan-abc-123/publish") && req.method() === "POST",
    );

    // Click the publish confirm button (has data-publish="everything")
    await page.locator('#publishModal [data-publish="everything"]').click();

    // Verify POST was sent with correct body
    const req = await publishRequest;
    expect(JSON.parse(req.postData())).toEqual({ publish: "everything" });

    // Should navigate to public view
    await page.waitForURL("**/plan-detail.html?plan=plan-abc-123&public=true");
  });

  test("publish cancel closes modal without POST (R3)", async ({ page }) => {
    await setupFailFast(page);

    let publishPostCalled = false;

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await page.route("**/api/plan/plan-abc-123/publish", (route) => {
      publishPostCalled = true;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      });
    });

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Open modal
    await page.locator("#publishBtn").click();
    await expect(page.locator("#publishModal")).toBeVisible();

    // Click Cancel (the button WITHOUT data-publish attribute)
    await page.locator("#publishModal").getByRole("button", { name: "Cancel" }).click();

    // Modal should close
    await expect(page.locator("#publishModal")).not.toBeVisible();

    // No POST should have been made
    expect(publishPostCalled).toBe(false);
  });

  test("delete confirm sends DELETE /api/plan/:planId (R4)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) => {
      if (route.request().method() === "DELETE") {
        return route.fulfill({ status: 200, body: "" });
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      });
    });

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Open delete modal
    await page.locator("#deleteMutablePlanBtn").click();
    await expect(page.locator("#deletePlanModal")).toBeVisible();

    // Set up request interception BEFORE clicking confirm
    const deleteRequest = page.waitForRequest(
      (req) => req.url().includes("/api/plan/plan-abc-123") && req.method() === "DELETE",
    );

    // Click confirm delete
    await page.locator("#confirmDeletePlanBtn").click();

    // Verify DELETE was sent
    const req = await deleteRequest;
    expect(req.method()).toBe("DELETE");
    expect(req.url()).toContain("/api/plan/plan-abc-123");
  });
});
