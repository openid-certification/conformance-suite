import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_TEST_STATUS, MOCK_TEST_FAILED } from "./fixtures/mock-test-data.js";
import { MOCK_LOG_ENTRIES, MOCK_FAILED_LOG_ENTRIES } from "./fixtures/mock-log-entries.js";

/**
 * Coverage for log-detail-v2.html — the new Lit-triad-based page.
 * This spec navigates directly to the new page (no flag, no cookie),
 * mocks the API surface the bootstrap depends on, and asserts that
 * cts-log-detail-header + cts-log-viewer render with the right data
 * and that the new affordances (Edit configuration, Share Link,
 * Repeat Test) fire the expected events / navigations.
 *
 * The legacy log-detail.spec.js continues to assert the legacy path
 * unchanged — both specs run side by side during the rollout window.
 *
 * Plan: docs/plans/2026-04-26-002-refactor-log-detail-page-to-lit-triad-plan.md
 */

/**
 * Register all routes the new bootstrap depends on. Mirror of
 * setupLogDetailRoutes from log-detail.spec.js but trimmed to the
 * surface the new bootstrap actually fires (no /api/runner poll for
 * a FINISHED test, no /api/uploaded-images multi-shape variants, etc.).
 *
 * @param {import('@playwright/test').Page} page
 * @param {{ testInfo: any, logEntries: any, planModules?: any[] }} options
 */
async function setupV2Routes(page, { testInfo, logEntries, planModules }) {
  const testId = testInfo.testId;

  await page.route(`**/api/info/${testId}*`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(testInfo),
    }),
  );

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

  if (testInfo.planId) {
    await page.route(`**/api/plan/${testInfo.planId}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          _id: testInfo.planId,
          planName: "test-plan",
          ...(planModules
            ? { modules: planModules }
            : {
                modules: [{ testModule: testInfo.testName, variant: testInfo.variant || {} }],
              }),
        }),
      }),
    );
  }

  // /api/runner — return 404 for FINISHED tests so the runner-poll
  // helper exits cleanly. RUNNING / WAITING tests would return data
  // here; specs that need that path will register a more specific
  // route before calling this helper.
  await page.route(`**/api/runner/${testId}`, (route) => route.fulfill({ status: 404, body: "" }));

  await page.route("**/api/uploaded-images*", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
  );
}

test.describe("log-detail-v2.html — new Lit-triad page", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("renders cts-log-detail-header with test metadata", async ({ page }) => {
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail-v2.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`);

    // Test name + ID land in the header.
    const header = page.locator("cts-log-detail-header");
    await expect(header).toContainText(MOCK_TEST_STATUS.testName);
    await expect(header).toContainText(MOCK_TEST_STATUS.testId);

    // Result + status badges.
    await expect(page.locator('cts-badge[label="PASSED"]')).toBeVisible();
    await expect(page.locator('cts-badge[label="FINISHED"]')).toBeVisible();
  });

  test("renders cts-log-viewer with mocked log entries", async ({ page }) => {
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail-v2.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`);

    const viewer = page.locator("cts-log-viewer");
    await expect(viewer).toBeVisible();

    // Wait for at least one log entry row.
    await expect(page.locator(".logItem").first()).toBeVisible();
  });

  test("Edit configuration button fires cts-edit-config with the right detail", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail-v2.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`);

    // Wait for the header to render (await testInfo flow).
    const header = page.locator("cts-log-detail-header");
    await expect(header).toContainText(MOCK_TEST_STATUS.testName);

    // Capture the cts-edit-config event before the click — the page-level
    // handler in log-detail-v2.js tries to navigate to schedule-test.html
    // which in turn fires its own /api/plan/available call. Asserting on
    // the event detail (rather than the URL) keeps this test focused on
    // U1's contract: header → event → page-level handler.
    const detailJson = /** @type {string} */ (
      await page.evaluate(() => {
        return new Promise((resolve) => {
          document.addEventListener(
            "cts-edit-config",
            /** @param {Event} e */ (e) => {
              const detail = /** @type {CustomEvent} */ (e).detail;
              resolve(JSON.stringify(detail));
            },
            { once: true },
          );
          // Stop the page-level handler from following through to
          // schedule-test.html — we don't need to load that page, only
          // observe the bubbled event.
          window.addEventListener(
            "beforeunload",
            (ev) => {
              ev.preventDefault();
              ev.returnValue = "";
            },
            { once: true },
          );
          const inner = document.querySelector(
            'cts-button[data-testid="edit-config-btn"] button',
          );
          if (inner) /** @type {HTMLButtonElement} */ (inner).click();
        });
      })
    );

    const detail = JSON.parse(detailJson);
    expect(detail.testId).toBe(MOCK_TEST_STATUS.testId);
    expect(detail.planId).toBe(MOCK_TEST_STATUS.planId);
    expect(detail.config).toEqual(MOCK_TEST_STATUS.config);
  });

  test("Private link button opens the expiration modal", async ({ page }) => {
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail-v2.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`);

    // Wait for the header → action stack → share-link button.
    const header = page.locator("cts-log-detail-header");
    await expect(header).toContainText(MOCK_TEST_STATUS.testName);

    const expirationModal = page.locator("#privateLinkExpirationModal");
    await expect(expirationModal).toBeHidden();

    // Click via inner <button> — the cts-button host-click pattern.
    const shareInnerBtn = page.locator('cts-button[data-testid="share-link-btn"] button');
    await shareInnerBtn.click();

    // cts-modal is built on native <dialog>; show() sets the host's
    // `open` state and the host becomes visible via :host([open]) CSS.
    await expect(expirationModal).toBeVisible();
  });

  test("failure summary jump-link bubbles cts-scroll-to-entry to the page", async ({ page }) => {
    // Inject FAILURE entries into testInfo.results so the Lit header's
    // _renderFailureSummary() has data to render. The base MOCK_TEST_FAILED
    // fixture defines result: "FAILED" but no per-condition results.
    const failedTestInfo = {
      ...MOCK_TEST_FAILED,
      results: [
        {
          _id: "fail-r1",
          result: "FAILURE",
          src: "ValidateIdToken",
          msg: "Signature invalid",
          requirements: ["OIDCC-3.1.3.7-6"],
        },
      ],
    };

    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: failedTestInfo,
      logEntries: MOCK_FAILED_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail-v2.html?log=${encodeURIComponent(failedTestInfo.testId)}`);

    // The failure-summary section is gated on having FAILURE/WARNING/
    // SKIPPED/INTERRUPTED entries in testInfo.results; injected above.
    const failureItem = page.locator(".failureSummary .failureText").first();
    await expect(failureItem).toBeVisible();

    // Capture the bubbled cts-scroll-to-entry event from the document.
    const eventDetail = await page.evaluate(() => {
      return new Promise((resolve) => {
        document.addEventListener(
          "cts-scroll-to-entry",
          /** @param {Event} e */ (e) => {
            const detail = /** @type {CustomEvent} */ (e).detail;
            resolve(detail);
          },
          { once: true },
        );
        const failure = document.querySelector(".failureSummary .failureText");
        if (failure) /** @type {HTMLElement} */ (failure).click();
      });
    });

    expect(eventDetail).toMatchObject({ entryId: expect.any(String) });
  });
});
