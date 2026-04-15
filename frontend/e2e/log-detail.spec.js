import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_TEST_STATUS, MOCK_TEST_FAILED, MOCK_TEST_WARNING } from "./fixtures/mock-test-data.js";
import { MOCK_LOG_ENTRIES, MOCK_FAILED_LOG_ENTRIES, MOCK_WARNING_LOG_ENTRIES } from "./fixtures/mock-log-entries.js";

/**
 * Helper: register log-detail-specific routes.
 * Must be called after setupFailFast and before setupCommonRoutes
 * (in practice: after failFast, before goto).
 */
async function setupLogDetailRoutes(page, { testInfo, logEntries }) {
  const testId = testInfo.testId;

  // /api/info/:testId
  await page.route(`**/api/info/${testId}`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(testInfo),
    }),
  );

  // /api/log/:testId — return entries on first call, empty on subsequent (since > 0)
  await page.route(`**/api/log/${testId}**`, (route) => {
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
      body: JSON.stringify(logEntries),
    });
  });

  // /api/runner/:testId — return FINISHED to stop the reloader
  await page.route(`**/api/runner/${testId}`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        id: testId,
        name: testInfo.testName,
        status: "FINISHED",
        created: testInfo.created,
        updated: testInfo.created,
        owner: testInfo.owner,
      }),
    }),
  );

  // /api/plan/:planId — for the "Return to Plan" button
  if (testInfo.planId) {
    await page.route(`**/api/plan/${testInfo.planId}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ _id: testInfo.planId, planName: "test-plan" }),
      }),
    );
  }
}

test.describe("log-detail.html — Log Detail", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads and renders log header (R16)", async ({ page }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-inst-001");

    // Header shows test name and test ID
    const header = page.locator("#logHeader");
    await expect(header).toContainText("oidcc-server");
    await expect(header).toContainText("test-inst-001");

    // Status and result display
    await expect(page.locator("#testStatusAndResult")).toContainText("FINISHED");
    await expect(page.locator("#testStatusAndResult")).toContainText("PASSED");
  });

  test("renders log entries with source, message, and result badges (R17)", async ({ page }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-inst-001");

    // Wait for log entries to render
    const logItems = page.locator(".logItem");
    await expect(logItems.first()).toBeVisible();

    // Check that entries have source labels
    await expect(page.locator(".logContent")).toContainText("CheckServerConfiguration");

    // Check result badges exist
    await expect(page.locator('[data-entry-result="success"]').first()).toBeVisible();

    // Check WARNING badge exists (entry-6 has result: "WARNING")
    await expect(page.locator('[data-entry-result="warning"]').first()).toBeVisible();
  });

  test("clicking a log entry expands detailed content (R18)", async ({ page }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-inst-001");

    // Wait for entries to render
    await expect(page.locator(".logItem").first()).toBeVisible();

    // Find a log entry that has a "more" button (entries with extra fields get one)
    const moreBtn = page.locator(".moreBtn").first();
    await expect(moreBtn).toBeVisible();

    // The more info panel should be hidden initially
    const moreInfo = page.locator(".moreInfo").first();
    await expect(moreInfo).not.toBeVisible();

    // Click the more button
    await moreBtn.click();

    // The more info panel should now be visible
    await expect(moreInfo).toBeVisible();
  });

  test("failed test shows failure summary section (R19)", async ({ page }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_FAILED,
      logEntries: MOCK_FAILED_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-fail-001");

    // Header shows FAILED result
    await expect(page.locator("#testStatusAndResult")).toContainText("FAILED");

    // Failure summary section is visible
    await expect(page.locator(".failureSummaryTitle")).toBeVisible();

    // Failure summary contains the failing condition details
    await expect(page.locator(".failureSummary")).toContainText("ValidateIdToken");
    await expect(page.locator(".failureSummary")).toContainText("FAILURE");
  });

  test("warning results are styled distinctly from failures and passes (R20)", async ({ page }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_WARNING,
      logEntries: MOCK_WARNING_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-warn-001");

    // Header shows WARNING result
    await expect(page.locator("#testStatusAndResult")).toContainText("WARNING");

    // Warning badges use result-warning class
    await expect(page.locator('[data-entry-result="warning"]').first()).toBeVisible();

    // Failure summary section also visible for warnings
    await expect(page.locator(".failureSummaryTitle")).toBeVisible();
  });

  test("View Config button opens modal with test configuration JSON", async ({ page }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-inst-001");

    // Config button should be visible
    const configBtn = page.locator("#showConfigBtn");
    await expect(configBtn).toBeVisible();

    // Config modal hidden initially
    const configModal = page.locator("#configModal");
    await expect(configModal).not.toBeVisible();

    // Click config button → modal opens with JSON
    await configBtn.click();
    await expect(configModal).toBeVisible();
    await expect(page.locator("#config")).toContainText("server.issuer");
    await expect(page.locator("#configTestId")).toContainText("test-inst-001");

    // Close modal
    await configModal.locator('[data-bs-dismiss="modal"]').first().click();
    await expect(configModal).not.toBeVisible();
  });

  test("status and result tooltips render on header", async ({ page }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-inst-001");

    // Status/result block should have tooltip elements
    const statusBlock = page.locator("#testStatusAndResult");
    await expect(statusBlock).toBeVisible();

    // Tooltip trigger elements should exist
    const tooltips = statusBlock.locator('[data-bs-toggle="tooltip"]');
    await expect(tooltips.first()).toBeVisible();

    // Bootstrap moves title to data-bs-original-title after tooltip init
    const origTitle = await tooltips.first().getAttribute("data-bs-original-title");
    expect(origTitle || "").toBeTruthy();
  });

  test("log entry more panel shows HTTP request/response details and collapses on second click", async ({ page }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-inst-001");

    await expect(page.locator(".logItem").first()).toBeVisible();

    // Find a more button and click it
    const moreBtn = page.locator(".moreBtn").first();
    await moreBtn.click();

    // Panel expands — should show key-value pairs from the "more" data
    const moreInfo = page.locator(".moreInfo").first();
    await expect(moreInfo).toBeVisible();

    // Chevron should point up when expanded
    await expect(moreBtn.locator(".bi-chevron-up")).toBeVisible();

    // Click again to collapse
    await moreBtn.click();
    await expect(moreInfo).not.toBeVisible();

    // Chevron should point down when collapsed
    await expect(moreBtn.locator(".bi-chevron-down")).toBeVisible();
  });

  test("failure summary items are clickable", async ({ page }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_FAILED,
      logEntries: MOCK_FAILED_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-fail-001");

    // Failure summary should be visible
    await expect(page.locator(".failureSummaryTitle")).toBeVisible();

    // Failure text items should be clickable (they have onclick that scrolls)
    const failureText = page.locator(".failureText").first();
    await expect(failureText).toBeVisible();
    await expect(failureText).toContainText("ValidateIdToken");

    // Click the failure text — should not throw an error (scrolls to entry)
    await failureText.click();
  });
});
