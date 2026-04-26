import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import {
  MOCK_TEST_STATUS,
  MOCK_TEST_FAILED,
  MOCK_TEST_WARNING,
  MOCK_TEST_RUNNING,
  MOCK_TEST_STATUS_WITH_INSTRUCTIONS,
  MOCK_TEST_STATUS_WITH_DESCRIPTION_ONLY,
} from "./fixtures/mock-test-data.js";
import {
  MOCK_LOG_ENTRIES,
  MOCK_FAILED_LOG_ENTRIES,
  MOCK_WARNING_LOG_ENTRIES,
} from "./fixtures/mock-log-entries.js";

/**
 * Helper: register log-detail-specific routes.
 * Must be called after setupFailFast and before setupCommonRoutes
 * (in practice: after failFast, before goto).
 *
 * @param {import('@playwright/test').Page} page
 * @param {{
 *   testInfo: any,
 *   logEntries: any,
 *   runnerStatus?: string,
 *   runnerError?: string | Record<string, unknown>,
 *   runner404?: boolean,
 * }} options
 */
async function setupLogDetailRoutes(
  page,
  { testInfo, logEntries, runnerStatus, runnerError, runner404 },
) {
  const testId = testInfo.testId;
  const runnerStatusValue = runnerStatus ?? "FINISHED";

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

  // /api/runner/:testId — drives the Active/Inactive/Archived/Error banner transitions.
  // 404 triggers the .catch() branch and shows the Archived banner.
  await page.route(`**/api/runner/${testId}`, (route) => {
    if (runner404) {
      return route.fulfill({ status: 404, body: "" });
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        id: testId,
        name: testInfo.testName,
        status: runnerStatusValue,
        created: testInfo.created,
        updated: testInfo.created,
        owner: testInfo.owner,
        ...(runnerError ? { error: runnerError } : {}),
      }),
    });
  });

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

    // Requirement badge renders as a clickable link when specLinks prefix matches (R7-specLinks)
    const requirementLink = page.locator('.log-requirement a[href*="openid-connect-core"]').first();
    await expect(requirementLink).toBeVisible();
    await expect(requirementLink).toContainText("OIDCC-3.1.3.3");
    await expect(requirementLink).toHaveAttribute(
      "href",
      "https://openid.net/specs/openid-connect-core-1_0.html#section-3.1.3.3",
    );
    await expect(requirementLink).toHaveAttribute("target", "_blank");
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
    await expect(moreInfo).toBeHidden();

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
    await expect(configModal).toBeHidden();

    // Click config button → modal opens with JSON
    await configBtn.click();
    await expect(configModal).toBeVisible();
    await expect(page.locator("#config")).toContainText("server.issuer");
    await expect(page.locator("#configTestId")).toContainText("test-inst-001");

    // Close modal
    await configModal.locator(".oidf-modal-close").first().click();
    await expect(configModal).toBeHidden();
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

    // After Bootstrap removal (U36), tooltip triggers are <cts-tooltip>
    // wrappers around the help-icon span. The component reads its tooltip
    // body from the `content` attribute, so we assert that attribute is
    // populated rather than poking at the now-gone Bootstrap state.
    const tooltips = statusBlock.locator("cts-tooltip");
    await expect(tooltips.first()).toBeAttached();

    const content = await tooltips.first().getAttribute("content");
    expect(content || "").toBeTruthy();
  });

  test("log entry more panel shows HTTP request/response details and collapses on second click", async ({
    page,
  }) => {
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
    await expect(moreInfo).toBeHidden();

    // Chevron should point down when collapsed
    await expect(moreBtn.locator(".bi-chevron-down")).toBeVisible();
  });

  // --- cts-alert banner visibility transitions ---
  //
  // The running-test header contains four cts-alert banners in
  // templates/logHeader.html. Three are driven by the /api/runner response:
  //   #runningTestActive   (info)    — runner status is RUNNING
  //   #runningTestInactive (warning) — runner status is FINISHED/INTERRUPTED
  //   #runningTestArchived (info)    — runner endpoint 404s
  // The fourth (#runningTestSuccess) is dead template markup: no JS ever
  // toggles its container visible, so it's intentionally not exercised here.

  test("banner transitions: FINISHED runner shows Inactive, hides Active/Archived", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
      runnerStatus: "FINISHED",
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-inst-001");

    const active = page.locator("#runningTestActive");
    const inactive = page.locator("#runningTestInactive");
    const archived = page.locator("#runningTestArchived");

    // Inactive banner visible — the warning "no longer running" state.
    await expect(inactive).toBeVisible();
    await expect(inactive.locator(".oidf-alert.oidf-alert-warning")).toBeVisible();

    // Active is hidden inline (style.display = 'none').
    await expect(active).toBeHidden();

    // Archived banner present but collapsed (no .show class).
    await expect(archived).toHaveClass(/collapse/);
    await expect(archived).not.toHaveClass(/show/);
  });

  test("banner transitions: RUNNING runner shows Active, hides Inactive/Archived", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_RUNNING,
      logEntries: MOCK_LOG_ENTRIES,
      runnerStatus: "RUNNING",
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-running-001");

    const active = page.locator("#runningTestActive");
    const inactive = page.locator("#runningTestInactive");
    const archived = page.locator("#runningTestArchived");

    await expect(active).toBeVisible();
    await expect(active.locator(".oidf-alert.oidf-alert-info")).toBeVisible();
    await expect(inactive).toBeHidden();
    await expect(archived).not.toHaveClass(/show/);
  });

  test("banner transitions: runner 404 shows Archived banner (dismissible info)", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
      runner404: true,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-inst-001");

    const active = page.locator("#runningTestActive");
    const inactive = page.locator("#runningTestInactive");
    const archived = page.locator("#runningTestArchived");

    // Archived becomes .show (Bootstrap collapse toggled on).
    await expect(archived).toHaveClass(/show/);
    await expect(archived.locator(".oidf-alert.oidf-alert-info")).toBeVisible();

    // Active and Inactive forced hidden inline.
    await expect(active).toBeHidden();
    await expect(inactive).toBeHidden();
  });

  // --- finalError.html INTERRUPTED state path ---
  //
  // When /api/runner returns an `error` object, log-detail.html injects the
  // FINAL_ERROR template (templates/finalError.html) into #runningTestError.
  // The template renders a danger cts-alert containing a cts-button
  // (#stacktraceBtn) that reveals the stacktrace list on click.

  test("runner error response injects cts-alert + stacktrace reveals on click", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: { ...MOCK_TEST_STATUS, status: "INTERRUPTED", result: null },
      logEntries: MOCK_LOG_ENTRIES,
      runnerStatus: "INTERRUPTED",
      runnerError: {
        error: "Something exploded",
        error_class: "RuntimeException",
        stacktrace: [
          "at net.openid.ExampleCondition.evaluate(ExampleCondition.java:42)",
          "at net.openid.TestRunner.run(TestRunner.java:99)",
        ],
        cause_stacktrace: ["at net.openid.Inner.cause(Inner.java:7)"],
      },
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-inst-001");

    const errorContainer = page.locator("#runningTestError");
    await expect(errorContainer).toHaveClass(/show/);

    // The template renders a danger cts-alert.
    const alert = errorContainer.locator("cts-alert[variant='danger']");
    await expect(alert).toBeVisible();
    await expect(alert).toContainText("Something exploded");
    await expect(alert).toContainText("RuntimeException");

    // Stacktrace and cause sections are collapsed until the button is clicked.
    const stacktrace = page.locator("#stacktrace");
    const causeStacktrace = page.locator("#causeStacktrace");
    await expect(stacktrace).not.toHaveClass(/show/);
    await expect(causeStacktrace).not.toHaveClass(/show/);

    // The stacktrace cts-button is visible; click its inner <button>
    // (the onclick is bound on the host, but clicking the inner button
    //  still bubbles — this matches the real user interaction path).
    const stacktraceBtn = page.locator("#stacktraceBtn");
    await expect(stacktraceBtn).toBeVisible();
    await stacktraceBtn.locator("button").click();

    // After click: the button hides itself and both collapses expand.
    await expect(stacktraceBtn).toBeHidden();
    await expect(stacktrace).toHaveClass(/show/);
    await expect(causeStacktrace).toHaveClass(/show/);
    await expect(stacktrace).toContainText("ExampleCondition.evaluate");
    await expect(causeStacktrace).toContainText("Inner.cause");
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

  // R24: split test description from user instructions in the blue summary box.
  // Plan: docs/plans/2026-04-25-008-feat-r24-test-description-vs-instructions-plan.md
  test("R24: summary with split marker renders About + What-you-need-to-do zones", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_STATUS_WITH_INSTRUCTIONS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-instr-001");

    const aboutZone = page.locator('[data-testid="about-test-zone"]');
    const instructionsZone = page.locator('[data-testid="user-instructions-zone"]');

    await expect(aboutZone).toBeVisible();
    await expect(instructionsZone).toBeVisible();

    // Description content lands in the about zone; instructions in the
    // instructions zone. The split marker is consumed and not surfaced.
    await expect(aboutZone).toContainText("About this test");
    await expect(aboutZone).toContainText("must not result in errors");
    await expect(instructionsZone).toContainText("What you need to do");
    await expect(instructionsZone).toContainText("Please remove any cookies");

    // Instructions zone is wrapped in a warning-variant cts-alert (action-coded palette).
    const instructionsAlert = page.locator('cts-alert[variant="warning"]', {
      has: page.locator('[data-testid="user-instructions-zone"]'),
    });
    await expect(instructionsAlert).toBeVisible();

    // About zone is wrapped in an info-variant cts-alert (the "blue box").
    const aboutAlert = page.locator('cts-alert[variant="info"]', {
      has: page.locator('[data-testid="about-test-zone"]'),
    });
    await expect(aboutAlert).toBeVisible();
  });

  test("R24: summary without split marker renders only the About zone", async ({ page }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_STATUS_WITH_DESCRIPTION_ONLY,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-desc-001");

    const aboutZone = page.locator('[data-testid="about-test-zone"]');
    const instructionsZone = page.locator('[data-testid="user-instructions-zone"]');

    await expect(aboutZone).toBeVisible();
    await expect(aboutZone).toContainText("About this test");
    await expect(aboutZone).toContainText("normal login page");

    // No instructions zone when the marker is absent.
    await expect(instructionsZone).toHaveCount(0);
  });
});
