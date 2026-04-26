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
 *   planModules?: Array<{ testModule: string, variant?: object|null }>,
 * }} options
 */
async function setupLogDetailRoutes(
  page,
  { testInfo, logEntries, runnerStatus, runnerError, runner404, planModules },
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

  // /api/plan/:planId — for the cts-test-nav-controls widget
  // (Return-to-Plan link, Continue Plan button, "Module X of N"
  // progress indicator). Pass `planModules` to drive the widget's
  // currentIndex/totalCount/nextEnabled state.
  if (testInfo.planId) {
    await page.route(`**/api/plan/${testInfo.planId}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          _id: testInfo.planId,
          planName: "test-plan",
          ...(planModules ? { modules: planModules } : {}),
        }),
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
    await expect(moreBtn.locator('cts-icon[name="chevron-up"]')).toBeVisible();

    // Click again to collapse
    await moreBtn.click();
    await expect(moreInfo).toBeHidden();

    // Chevron should point down when collapsed
    await expect(moreBtn.locator('cts-icon[name="chevron-down"]')).toBeVisible();
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
  // Integration coverage only — the splitter's input/output edge cases live in
  // src/main/resources/static/components/test-summary-split.test.js. These
  // tests prove the live-page lodash template wires the shared splitter
  // correctly, that the marker is consumed before reaching the DOM, and that
  // both zones render under their action-coded cts-alert variants.
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

    // Eyebrows identify each zone for assistive tech and visual scan.
    await expect(aboutZone).toContainText("About this test");
    await expect(instructionsZone).toContainText("What you need to do");

    // Each zone wraps its half in the right cts-alert variant.
    await expect(
      page.locator('cts-alert[variant="info"]', {
        has: page.locator('[data-testid="about-test-zone"]'),
      }),
    ).toBeVisible();
    await expect(
      page.locator('cts-alert[variant="warning"]', {
        has: page.locator('[data-testid="user-instructions-zone"]'),
      }),
    ).toBeVisible();

    // The literal `---` marker is consumed by the splitter, not surfaced
    // to the user. This is the integration contract — the unit tests
    // prove the splitter strips it; this asserts the live page actually
    // calls the splitter rather than dumping raw `test.summary`.
    await expect(aboutZone).not.toContainText("---");
    await expect(instructionsZone).not.toContainText("---");
  });

  test("R24: summary without split marker renders only the About zone", async ({ page }) => {
    await setupFailFast(page);
    await setupLogDetailRoutes(page, {
      testInfo: MOCK_TEST_STATUS_WITH_DESCRIPTION_ONLY,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto("/log-detail.html?log=test-desc-001");

    await expect(page.locator('[data-testid="about-test-zone"]')).toBeVisible();
    await expect(page.locator('[data-testid="user-instructions-zone"]')).toHaveCount(0);
  });

  // R21: group test navigation controls (back / repeat / continue / progress)
  // into a single cts-test-nav-controls widget. Plan:
  // docs/plans/2026-04-25-007-feat-test-nav-controls-widget-plan.md
  test.describe("R21: test navigation widget", () => {
    /**
     * Build a modules array where the test under test sits at
     * `currentIndex` (matches MOCK_TEST_STATUS's testName + variant).
     * @param {number} currentIndex
     * @param {number} total
     */
    function modulesAt(currentIndex, total) {
      return Array.from({ length: total }, (_, i) => ({
        testModule: i === currentIndex ? "oidcc-server" : `oidcc-other-${i}`,
        variant:
          i === currentIndex
            ? {
                client_auth_type: "client_secret_basic",
                response_type: "code",
              }
            : null,
      }));
    }

    test("renders unified nav cluster with Module X of N for a mid-plan test", async ({ page }) => {
      await setupFailFast(page);
      await setupLogDetailRoutes(page, {
        testInfo: MOCK_TEST_STATUS,
        logEntries: MOCK_LOG_ENTRIES,
        planModules: modulesAt(5, 30),
      });
      await setupCommonRoutes(page);

      await page.goto("/log-detail.html?log=test-inst-001");

      const widget = page.locator("cts-test-nav-controls");
      await expect(widget).toBeVisible();

      // Semantic grouping
      const group = widget.locator('[role="group"]');
      await expect(group).toHaveAttribute("aria-label", "Test plan navigation");

      // Progress count + ARIA values
      await expect(widget).toContainText("Module 6 of 30");
      const progressBar = widget.locator('[role="progressbar"]');
      await expect(progressBar).toHaveAttribute("aria-valuenow", "6");
      await expect(progressBar).toHaveAttribute("aria-valuemin", "1");
      await expect(progressBar).toHaveAttribute("aria-valuemax", "30");

      // All three controls present
      await expect(widget.locator('[data-testid="back-btn"]')).toBeVisible();
      await expect(widget.locator('[data-testid="repeat-btn"]')).toBeVisible();
      await expect(widget.locator('[data-testid="continue-btn"]')).toBeVisible();

      // Return to Plan link points at plan-detail.html with the planId
      const backHref = await widget.locator('[data-testid="back-btn"] a').getAttribute("href");
      expect(backHref).toContain("plan-detail.html?plan=");
      expect(backHref).toContain(encodeURIComponent(MOCK_TEST_STATUS.planId));
    });

    test("hides Continue Plan on the last module; progress reads N of N", async ({ page }) => {
      await setupFailFast(page);
      await setupLogDetailRoutes(page, {
        testInfo: MOCK_TEST_STATUS,
        logEntries: MOCK_LOG_ENTRIES,
        planModules: modulesAt(2, 3), // last position in a 3-module plan
      });
      await setupCommonRoutes(page);

      await page.goto("/log-detail.html?log=test-inst-001");

      const widget = page.locator("cts-test-nav-controls");
      await expect(widget).toContainText("Module 3 of 3");

      // Continue button is hidden when there is no next module — the
      // progress text "Module N of N" carries the end-of-plan signal.
      await expect(widget.locator('[data-testid="continue-btn"]')).toHaveCount(0);

      // Back and Repeat are still rendered
      await expect(widget.locator('[data-testid="back-btn"]')).toBeVisible();
      await expect(widget.locator('[data-testid="repeat-btn"]')).toBeVisible();
    });

    test("readonly (?public=true) view shows only Return to Plan", async ({ page }) => {
      await setupFailFast(page);
      const publishedTest = {
        ...MOCK_TEST_STATUS,
        publish: "everything",
      };
      // The public view fetches /api/info with ?public=true on the URL —
      // route both forms so the route handler matches under either path.
      await page.route(`**/api/info/${publishedTest.testId}?public=true`, (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(publishedTest),
        }),
      );
      await setupLogDetailRoutes(page, {
        testInfo: publishedTest,
        logEntries: MOCK_LOG_ENTRIES,
      });
      await setupCommonRoutes(page);

      await page.goto("/log-detail.html?log=test-inst-001&public=true");

      const widget = page.locator("cts-test-nav-controls");
      await expect(widget.locator('[data-testid="back-btn"]')).toBeVisible();

      // Repeat / Continue are not rendered in the public/readonly view.
      await expect(widget.locator('[data-testid="repeat-btn"]')).toHaveCount(0);
      await expect(widget.locator('[data-testid="continue-btn"]')).toHaveCount(0);

      // Back link appends &public=true so the linked plan-detail
      // page renders its public-share variant — same semantic as the
      // legacy planBtn JS appended this flag conditionally.
      const backHref = await widget.locator('[data-testid="back-btn"] a').getAttribute("href");
      expect(backHref).toContain("&public=true");
    });

    test("Cmd+Shift+U keyboard shortcut activates Continue Plan via the widget", async ({
      page,
    }) => {
      await setupFailFast(page);
      await setupLogDetailRoutes(page, {
        testInfo: MOCK_TEST_STATUS,
        logEntries: MOCK_LOG_ENTRIES,
        planModules: modulesAt(0, 3),
      });
      // The shortcut path: press → page-level keydown → click the
      // widget's inner button → cts-continue event → fetch
      // /api/runner. Hold the runner request indefinitely so the
      // page does not navigate away mid-assertion.
      await page.route("**/api/runner?**", () => {
        // never resolves — pin the page so we can read the cts-continue spy
      });
      await setupCommonRoutes(page);

      await page.goto("/log-detail.html?log=test-inst-001");

      const continueBtn = page.locator('cts-test-nav-controls [data-testid="continue-btn"]');
      await expect(continueBtn).toBeVisible();

      // Spy on the bubbling cts-continue event before triggering
      // the shortcut. The event fires synchronously from the inner
      // button click; the page only navigates after the runner fetch
      // resolves (which the route stub above never does).
      await page.evaluate(() => {
        // @ts-expect-error — runtime window probe
        window.__r21ContinueFired = false;
        document.addEventListener(
          "cts-continue",
          // @ts-expect-error — runtime window probe
          () => (window.__r21ContinueFired = true),
        );
      });

      // Match the page's keydown handler: Ctrl+Shift+U on non-Mac,
      // Meta+Shift+U on Mac. The handler reads navigator.platform to
      // decide which modifier to accept, so try both.
      await page.keyboard.press("Meta+Shift+U");
      await page.keyboard.press("Control+Shift+U");

      const fired = await page.evaluate(() => {
        // @ts-expect-error — runtime window probe
        return window.__r21ContinueFired;
      });
      expect(fired).toBe(true);
    });
  });
});
