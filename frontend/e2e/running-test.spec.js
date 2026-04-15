import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast } from "./helpers/routes.js";

const NOW = Date.now();

/** Mock /api/runner/:testId — shape used by the RUNNING_TEST template */
const RUNNER_DETAIL_1 = {
  id: "test-running-001",
  name: "oidcc-server",
  created: new Date(NOW - 300000).toISOString(),
  updated: new Date(NOW - 60000).toISOString(),
  owner: { sub: "12345", iss: "https://accounts.google.com" },
};

const RUNNER_DETAIL_2 = {
  id: "test-running-002",
  name: "oidcc-server-rotate-keys",
  created: new Date(NOW - 120000).toISOString(),
  updated: new Date(NOW - 30000).toISOString(),
  owner: { sub: "12345", iss: "https://accounts.google.com" },
};

/** Mock /api/info/:testId — shape used by the TEST_STATUS template */
const INFO_RUNNING = { status: "RUNNING", result: null };
const INFO_WAITING = { status: "WAITING", result: null };

test.describe("running-test.html — Running Tests", () => {
  test("loads and renders running tests (R12)", async ({ page }) => {
    await setupFailFast(page);
    await setupCommonRoutes(page);

    // /api/runner/running returns string test IDs
    await page.route("**/api/runner/running", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(["test-running-001", "test-running-002"]),
      }),
    );

    // /api/runner/:testId returns test detail
    await page.route("**/api/runner/test-running-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(RUNNER_DETAIL_1),
      }),
    );
    await page.route("**/api/runner/test-running-002", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(RUNNER_DETAIL_2),
      }),
    );

    // /api/info/:testId returns status
    await page.route("**/api/info/test-running-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(INFO_RUNNING),
      }),
    );
    await page.route("**/api/info/test-running-002", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(INFO_WAITING),
      }),
    );

    await page.goto("/running-test.html");

    // Two test rows rendered
    const rows = page.locator(".runningTest");
    await expect(rows).toHaveCount(2);

    // First test shows name and status
    await expect(rows.nth(0)).toContainText("oidcc-server");
    await expect(rows.nth(0)).toContainText("RUNNING");

    // Second test shows name and status
    await expect(rows.nth(1)).toContainText("oidcc-server-rotate-keys");
    await expect(rows.nth(1)).toContainText("WAITING");
  });

  test("manual refresh re-fetches and updates statuses (R13)", async ({
    page,
  }) => {
    let returnEmpty = false;

    await setupFailFast(page);
    await setupCommonRoutes(page);

    // Switch response after we signal it
    await page.route("**/api/runner/running", (route) => {
      const data = returnEmpty ? [] : ["test-running-001"];
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(data),
      });
    });

    await page.route("**/api/runner/test-running-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(RUNNER_DETAIL_1),
      }),
    );
    await page.route("**/api/info/test-running-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(INFO_RUNNING),
      }),
    );

    await page.goto("/running-test.html");

    // Initially one test row
    await expect(page.locator(".runningTest")).toHaveCount(1);
    await expect(page.locator(".runningTest").first()).toContainText(
      "oidcc-server",
    );

    // Switch mock to return empty list, then click refresh
    returnEmpty = true;
    await page.click("#refresh");

    // After refresh, no tests running
    await expect(page.locator(".runningTest")).toHaveCount(0);
  });

  test("empty state when no tests are running (R15)", async ({ page }) => {
    await setupFailFast(page);
    await setupCommonRoutes(page);

    await page.route("**/api/runner/running", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([]),
      }),
    );

    await page.goto("/running-test.html");

    // No test rows rendered
    await expect(page.locator(".runningTest")).toHaveCount(0);

    // The running-tests container is empty
    await expect(page.locator("#running-tests")).toBeEmpty();
  });

  test("test rows contain View Test Details link and Download button", async ({ page }) => {
    await setupFailFast(page);
    await setupCommonRoutes(page);

    await page.route("**/api/runner/running", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(["test-running-001"]),
      }),
    );

    await page.route("**/api/runner/test-running-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(RUNNER_DETAIL_1),
      }),
    );
    await page.route("**/api/info/test-running-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(INFO_RUNNING),
      }),
    );

    await page.goto("/running-test.html");

    const row = page.locator(".runningTest").first();
    await expect(row).toBeVisible();

    // View Test Details link should point to log-detail with the correct test ID
    const viewBtn = row.locator(".viewBtn");
    await expect(viewBtn).toBeVisible();
    await expect(viewBtn).toHaveAttribute(
      "href",
      "log-detail.html?log=test-running-001",
    );

    // Download button should be present
    const downloadBtn = row.locator(".downloadBtn");
    await expect(downloadBtn).toBeVisible();
    await expect(downloadBtn).toContainText("Download Logs");
  });

  test("status tooltips render on test status blocks", async ({ page }) => {
    await setupFailFast(page);
    await setupCommonRoutes(page);

    await page.route("**/api/runner/running", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(["test-running-001"]),
      }),
    );

    await page.route("**/api/runner/test-running-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(RUNNER_DETAIL_1),
      }),
    );
    await page.route("**/api/info/test-running-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(INFO_RUNNING),
      }),
    );

    await page.goto("/running-test.html");

    // Wait for status to render
    const statusBlock = page.locator(".testStatusResultBlock").first();
    await expect(statusBlock).toBeVisible();
    await expect(statusBlock).toContainText("RUNNING");

    // Tooltip trigger should exist with help text
    const tooltip = statusBlock.locator('[data-bs-toggle="tooltip"]');
    await expect(tooltip).toBeVisible();
    // Bootstrap moves title to data-bs-original-title after tooltip init
    const origTitle = await tooltip.getAttribute("data-bs-original-title");
    expect(origTitle || "").toContain("actively executing");
  });
});
