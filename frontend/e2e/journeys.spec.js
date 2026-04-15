import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, setupTestInfoRoute, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_PLANS, MOCK_PLAN_NO_VARIANTS } from "./fixtures/mock-plans.js";
import { MOCK_PLAN_DETAIL, MOCK_TEST_STATUS } from "./fixtures/mock-test-data.js";
import { MOCK_LOG_ENTRIES } from "./fixtures/mock-log-entries.js";

const ALL_PLANS = [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS];

test.describe("Cross-page journeys", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("schedule → plan-detail → log-detail journey (R21)", async ({ page }) => {
    // Register ALL routes before first navigation — they persist across page loads
    await setupFailFast(page);

    // --- Schedule-test routes ---
    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    // POST /api/plan — create a new plan, return plan ID.
    // Registered BEFORE /api/plan/available and /api/plan/plan-journey-001
    // so those more specific routes are tried first (Playwright checks
    // last-registered first, and **/api/plan?* would match them via its
    // single-char wildcard ?).
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ id: "plan-journey-001" }),
        });
      }
      return route.fallback();
    });

    // --- Plan-detail routes ---
    await page.route("**/api/plan/plan-journey-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ...MOCK_PLAN_DETAIL,
          _id: "plan-journey-001",
          planName: "oidcc-client-basic-certification-test-plan",
          modules: [
            {
              testModule: "oidcc-client-test",
              testSummary: "Client-side test",
              variant: {},
              instances: ["test-journey-001"],
            },
          ],
          config: { "server.issuer": "https://op.example.com" },
        }),
      }),
    );

    // POST /api/runner — run a test, return test ID
    await page.route("**/api/runner?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ id: "test-journey-001", name: "oidcc-client-test" }),
        });
      }
      return route.fallback();
    });

    // --- Log-detail routes ---
    await page.route("**/api/log/test-journey-001**", (route) => {
      const url = new URL(route.request().url());
      const since = url.searchParams.get("since");
      if (since && Number(since) > 0) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify([]),
        });
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_LOG_ENTRIES),
      });
    });

    await page.route("**/api/runner/test-journey-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          id: "test-journey-001",
          name: "oidcc-client-test",
          status: "FINISHED",
          created: new Date().toISOString(),
          updated: new Date().toISOString(),
          owner: { sub: "12345", iss: "https://accounts.google.com" },
        }),
      }),
    );

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await setupTestInfoRoute(page, {
      "test-journey-001": {
        ...MOCK_TEST_STATUS,
        _id: "test-journey-001",
        testId: "test-journey-001",
        testName: "oidcc-client-test",
        planId: "plan-journey-001",
      },
    });
    await setupCommonRoutes(page);

    // === Step 1: schedule-test.html — navigate cascade and create plan ===
    await page.goto("/schedule-test.html");

    await page.locator("#specFamilySelect").selectOption("OIDCC");
    await expect(page.locator("#entitySelect")).toBeVisible();
    await page.locator("#entitySelect").selectOption("client-basic");
    await expect(page.locator("#planSelect")).toBeVisible();

    const createBtn = page.locator("#createPlanBtn");
    await expect(createBtn).toBeEnabled({ timeout: 5000 });
    await createBtn.click();

    // === Step 2: Redirected to plan-detail.html ===
    await page.waitForURL("**/plan-detail.html?plan=plan-journey-001");
    await expect(page.locator("#planHeader")).toContainText("oidcc-client-basic-certification-test-plan");

    // Verify module list rendered
    const moduleRows = page.locator("#planItems .logItem");
    await expect(moduleRows).toHaveCount(1);
    await expect(moduleRows.first()).toContainText("oidcc-client-test");

    // === Step 3: Click Run Test → redirected to log-detail.html ===
    const runBtn = page.locator(".startBtn").first();
    await expect(runBtn).toBeVisible();
    await runBtn.click();

    await page.waitForURL("**/log-detail.html?log=test-journey-001");

    // Verify log-detail loaded with correct test
    await expect(page.locator("#logHeader")).toContainText("oidcc-client-test");
    await expect(page.locator("#logHeader")).toContainText("test-journey-001");

    // Verify log entries rendered
    const logEntries = page.locator(".logItem");
    await expect(logEntries.first()).toBeVisible();
  });

  test("plan-detail → log-detail journey (R22)", async ({ page }) => {
    await setupFailFast(page);

    // Plan-detail routes
    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    // POST /api/runner — run a test from plan-detail
    let runnerPostCalled = false;
    await page.route("**/api/runner?*", (route) => {
      if (route.request().method() === "POST") {
        runnerPostCalled = true;
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ id: "test-run-001", name: "oidcc-server" }),
        });
      }
      return route.fallback();
    });

    // Log-detail routes
    await page.route("**/api/log/test-run-001**", (route) => {
      const url = new URL(route.request().url());
      const since = url.searchParams.get("since");
      if (since && Number(since) > 0) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify([]),
        });
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_LOG_ENTRIES),
      });
    });

    await page.route("**/api/runner/test-run-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          id: "test-run-001",
          name: "oidcc-server",
          status: "FINISHED",
          created: new Date().toISOString(),
          updated: new Date().toISOString(),
          owner: { sub: "12345", iss: "https://accounts.google.com" },
        }),
      }),
    );

    await setupTestInfoRoute(page, {
      "test-run-001": {
        ...MOCK_TEST_STATUS,
        _id: "test-run-001",
        testId: "test-run-001",
        planId: "plan-abc-123",
      },
    });
    await setupCommonRoutes(page);

    // === Step 1: plan-detail.html ===
    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Verify plan loaded
    await expect(page.locator("#planHeader")).toContainText("oidcc-basic-certification-test-plan");

    // Click "Run Test" on the first module
    const runBtn = page.locator(".startBtn").first();
    await expect(runBtn).toBeVisible();
    await runBtn.click();

    // === Step 2: Redirected to log-detail.html ===
    await page.waitForURL("**/log-detail.html?log=test-run-001");
    expect(runnerPostCalled).toBe(true);

    // Verify log-detail loaded with correct test
    await expect(page.locator("#logHeader")).toContainText("oidcc-server");
    await expect(page.locator("#logHeader")).toContainText("test-run-001");
  });
});
