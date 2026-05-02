import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_TEST_RUNNING, MOCK_TEST_RUNNING_2 } from "./fixtures/mock-test-data.js";

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

test.describe("running-test.html — Running Tests", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

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
        body: JSON.stringify(MOCK_TEST_RUNNING),
      }),
    );
    await page.route("**/api/info/test-running-002", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_TEST_RUNNING_2),
      }),
    );

    await page.goto("/running-test.html");

    // Two cards rendered. After U32 each row is a <cts-running-test-card>
    // (light-DOM), driven by the `runningTest.html` template stub +
    // inline JS that sets `.test`/`.isAdmin` on the host.
    const cards = page.locator("cts-running-test-card");
    await expect(cards).toHaveCount(2);

    // First test shows name and status (status badge text comes from cts-badge,
    // which the card sets from /api/info → test.status).
    await expect(cards.nth(0)).toContainText("oidcc-server");
    await expect(cards.nth(0).locator("cts-badge")).toHaveAttribute("label", "RUNNING");

    // Second test shows name and the R19 friendly label. The underlying
    // `test.status` enum stays "WAITING" (variant lookup, data hooks);
    // only the rendered badge label is mapped at render time.
    await expect(cards.nth(1)).toContainText("oidcc-server-rotate-keys");
    await expect(cards.nth(1).locator("cts-badge")).toHaveAttribute(
      "label",
      "Waiting for user input",
    );
  });

  test("manual refresh re-fetches and updates statuses (R13)", async ({ page }) => {
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
        body: JSON.stringify(MOCK_TEST_RUNNING),
      }),
    );

    await page.goto("/running-test.html");

    // Initially one card
    await expect(page.locator("cts-running-test-card")).toHaveCount(1);
    await expect(page.locator("cts-running-test-card").first()).toContainText("oidcc-server");

    // Switch mock to return empty list, then click the Refresh cts-button.
    // The host listens on `cts-click`, but a native click on the inner
    // <button> dispatches it (cts-button._handleClick).
    returnEmpty = true;
    await page.locator("#refresh button").click();

    // After refresh, no tests running
    await expect(page.locator("cts-running-test-card")).toHaveCount(0);
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

    // No cards rendered
    await expect(page.locator("cts-running-test-card")).toHaveCount(0);

    // Empty state renders inside the list container, explaining the blank
    // area and offering a primary CTA to the schedule-test page.
    const emptyState = page.locator("#running-tests cts-empty-state");
    await expect(emptyState).toBeVisible();
    await expect(emptyState).toContainText("No tests are currently running");
    await expect(emptyState.locator("cts-link-button")).toHaveAttribute(
      "href",
      "schedule-test.html",
    );
  });

  test("rows contain View Test Details link and Download button", async ({ page }) => {
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
        body: JSON.stringify(MOCK_TEST_RUNNING),
      }),
    );

    await page.goto("/running-test.html");

    const card = page.locator("cts-running-test-card").first();
    await expect(card).toBeVisible();

    // View Test Details link should point to log-detail with the correct
    // test ID. cts-link-button renders the inner <a>.
    const viewBtn = card.locator(".viewBtn");
    await expect(viewBtn).toBeVisible();
    await expect(viewBtn.locator("a")).toHaveAttribute(
      "href",
      "log-detail.html?log=test-running-001",
    );

    // Download button should be present
    const downloadBtn = card.locator(".downloadBtn");
    await expect(downloadBtn).toBeVisible();
    await expect(downloadBtn).toContainText("Download Logs");
  });

  test("status badge reflects the test status from /api/info", async ({ page }) => {
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
        body: JSON.stringify(MOCK_TEST_RUNNING),
      }),
    );

    await page.goto("/running-test.html");

    // The cts-badge inside the card carries the canonical RUNNING status
    // and the design-system `running` variant (which renders the spinner).
    const badge = page.locator("cts-running-test-card").first().locator("cts-badge");
    await expect(badge).toHaveAttribute("label", "RUNNING");
    await expect(badge).toHaveAttribute("variant", "running");
  });
});
