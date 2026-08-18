import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLAN_LIST, MOCK_PLAN_INFO } from "./fixtures/mock-plans.js";
import { MOCK_ADMIN_USER, MOCK_USER } from "./fixtures/mock-users.js";

/**
 * The admin-only bulk delete on plans.html: `DELETE /api/plan?<the filters the
 * listing is showing>`.
 *
 * The affordance deliberately lives in the row of filter chips, so it exists
 * only where the listing is already narrowed — which is also the one thing the
 * server insists on. These tests therefore drive the page with a filter in the
 * URL, which `plans.html` resolves into the component's `filters` property.
 *
 * Nothing here reaches a database: `/api/plan/delete-preview` and
 * `/api/plan/delete-status` are mocked, and the DELETE is asserted by what the
 * page *sent* — above all that `confirm` carries the number the dialog showed,
 * which is what stops a listing that moved from being deleted by mistake.
 */

const FILTERED_URL = "/plans.html?owner=104383237143811096540&to=2025-08-17";
const DELETE_BUTTON = "[data-testid='plan-bulk-delete']";
const CONFIRM_BUTTON = "[data-testid='plan-bulk-delete-confirm']";
const SERVER_SEARCH = "[data-testid='plan-bulk-delete-server-search']";

/**
 * @param {import('@playwright/test').Page} page - The page under test.
 * @returns {Promise<void>} When the listing route is mocked.
 */
async function mockPlanRoute(page) {
  await page.route("**/api/plan?*", (route) => {
    if (route.request().method() !== "GET") return route.fallback();
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(MOCK_PLAN_LIST),
    });
  });
}

/**
 * @param {import('@playwright/test').Page} page - The page under test.
 * @param {object} preview - What the preview endpoint should answer.
 * @returns {Promise<void>} When it is mocked.
 */
async function mockPreview(page, preview) {
  await page.route("**/api/plan/delete-preview*", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(preview),
    }),
  );
}

test.describe("plans.html — bulk delete", () => {
  test.beforeEach(async ({ page }) => {
    // must be first: Playwright matches routes in reverse registration order
    await setupFailFast(page);
  });

  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("is not offered to a non-admin, however filtered the listing is", async ({ page }) => {
    await setupCommonRoutes(page, { user: MOCK_USER });
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await mockPlanRoute(page);

    await page.goto(FILTERED_URL);

    await expect(page.locator("[data-testid='plan-filters']")).toBeVisible();
    await expect(page.locator(DELETE_BUTTON)).toHaveCount(0);
  });

  test("is not offered to an admin when the listing is not narrowed", async ({ page }) => {
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await mockPlanRoute(page);

    await page.goto("/plans.html");

    // no chips, so no chip row, so nothing to delete "these" of
    await expect(page.locator("[data-testid='plan-filters']")).toHaveCount(0);
    await expect(page.locator(DELETE_BUTTON)).toHaveCount(0);
  });

  test("shows what would go, then deletes it and reports progress", async ({ page }) => {
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await mockPlanRoute(page);
    await mockPreview(page, { listed: 348585, deletable: 348583, kept: 2, target: 100 });

    /** @type {Array<string>} */
    const deleteUrls = [];
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() !== "DELETE") return route.fallback();
      deleteUrls.push(route.request().url());
      return route.fulfill({
        status: 202,
        contentType: "application/json",
        body: JSON.stringify({
          state: "RUNNING",
          plans: 0,
          tests: 0,
          logEntries: 0,
          target: 100,
        }),
      });
    });

    let polls = 0;
    await page.route("**/api/plan/delete-status", (route) => {
      polls += 1;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          polls === 1
            ? { state: "RUNNING", plans: 50, tests: 600, logEntries: 7000, target: 100 }
            : { state: "DONE", plans: 100, tests: 1200, logEntries: 14000, target: 100 },
        ),
      });
    });

    await page.goto(FILTERED_URL);
    await page.locator(DELETE_BUTTON).click();

    // the dialog says what will go before anything does
    const counts = page.locator("[data-testid='plan-bulk-delete-counts']");
    await expect(counts).toContainText("348,585 plans match");
    await expect(counts).toContainText("100 will be deleted");
    await expect(counts).toContainText("2 are kept");
    await expect(page.locator(CONFIRM_BUTTON)).toContainText("Delete 100 plans");

    await page.locator(CONFIRM_BUTTON).click();

    // what was sent: the listing's own filters, the limit, and the number the
    // dialog had just shown
    await expect.poll(() => deleteUrls.length).toBe(1);
    const sent = new URL(deleteUrls[0]);
    expect(sent.searchParams.get("owner")).toBe("104383237143811096540");
    expect(sent.searchParams.get("to")).toBe("2025-08-17");
    expect(sent.searchParams.get("limit")).toBe("100");
    expect(sent.searchParams.get("confirm")).toBe("100");

    const progress = page.locator("[data-testid='plan-bulk-delete-progress']");
    await expect(progress).toContainText("Deleting");
    await expect(progress).toContainText("DONE", { timeout: 15000 });
    await expect(progress).toContainText("100");
    await expect(progress).toContainText("14,000");

    // and it STAYS: the dialog is the only report of what was deleted, so
    // refreshing the listing must not tear it down while it is being read
    await page.waitForTimeout(1000);
    await expect(progress).toBeVisible();
    await expect(progress).toContainText("DONE");
  });

  test("shows the reason when the server refuses", async ({ page }) => {
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await mockPlanRoute(page);
    await mockPreview(page, { listed: 10, deletable: 10, kept: 0, target: 10 });

    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() !== "DELETE") return route.fallback();
      return route.fulfill({
        status: 409,
        contentType: "application/json",
        body: JSON.stringify({ error: "a bulk delete is already running" }),
      });
    });

    await page.goto(FILTERED_URL);
    await page.locator(DELETE_BUTTON).click();
    await page.locator(CONFIRM_BUTTON).click();

    await expect(page.locator("[data-testid='plan-bulk-delete-error']")).toContainText(
      "a bulk delete is already running",
    );
  });

  test("offers nothing to delete when everything matching is kept", async ({ page }) => {
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await mockPlanRoute(page);
    await mockPreview(page, { listed: 32, deletable: 0, kept: 32, target: 0 });

    await page.goto(FILTERED_URL);
    await page.locator(DELETE_BUTTON).click();

    await expect(page.locator("[data-testid='plan-bulk-delete-counts']")).toContainText(
      "32 are kept",
    );
    // cts-button forwards the host's `disabled` to the inner native button,
    // which is what :disabled (and so toBeDisabled) reads
    await expect(page.locator(`${CONFIRM_BUTTON} button`)).toBeDisabled();
  });

  test("is refused while a search is narrowing what is on screen", async ({ page }) => {
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await mockPlanRoute(page);

    await page.goto(FILTERED_URL);
    await expect(page.locator(`${DELETE_BUTTON} button`)).toBeEnabled();

    // the search box narrows only what is rendered, so what would be deleted is
    // no longer what the admin can see
    await page.locator("input[type='search']").fill("ciba");

    await expect(page.locator(`${DELETE_BUTTON} button`)).toBeDisabled();
    await expect(page.locator(DELETE_BUTTON)).toHaveAttribute("title", /search box narrows only/);
    await expect(page.locator(SERVER_SEARCH)).toBeVisible();
  });

  test("offers to hand the search to the server, which puts the delete back in reach", async ({
    page,
  }) => {
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);

    /** @type {Array<string>} */
    const listingUrls = [];
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() !== "GET") return route.fallback();
      listingUrls.push(route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_LIST),
      });
    });
    await mockPreview(page, { listed: 3, deletable: 3, kept: 0, target: 3 });

    await page.goto(FILTERED_URL);
    await page.locator("input[type='search']").fill("ciba");
    await page.locator(SERVER_SEARCH).click();

    // the term moved out of the box and into the listing itself
    await expect(page.locator("input[type='search']")).toHaveValue("");
    await expect(page.locator("[data-testid='plan-filter-search']")).toHaveAttribute(
      "label",
      "Search: ciba",
    );
    await expect(page).toHaveURL(/[?&]search=ciba/);

    // the listing was re-fetched from the server WITH the term
    await expect
      .poll(() => listingUrls.some((url) => new URL(url).searchParams.get("search") === "ciba"))
      .toBe(true);

    // and deleting is on the table again, carrying that same term
    await expect(page.locator(`${DELETE_BUTTON} button`)).toBeEnabled();
    await page.locator(DELETE_BUTTON).click();
    await expect(page.locator("[data-testid='plan-bulk-delete-counts']")).toContainText(
      "3 plans match",
    );
  });
});

test.describe("plans.html — narrowing the listing without editing the URL", () => {
  test.beforeEach(async ({ page }) => {
    await setupFailFast(page);
  });

  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("the owner pill narrows the listing to that account", async ({ page }) => {
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await mockPlanRoute(page);

    await page.goto("/plans.html");

    const link = page.locator("[data-testid='plan-owner-link']").first();
    const owner = MOCK_PLAN_LIST.find((plan) => plan.owner)?.owner?.sub;
    await expect(link).toHaveAttribute("href", new RegExp(`owner=${owner}`));

    await link.click();
    await expect(page).toHaveURL(new RegExp(`owner=${owner}`));
    await expect(page.locator("[data-testid='plan-filter-owner']")).toHaveAttribute(
      "label",
      `Owner: ${owner}`,
    );
  });

  test("the Started control narrows the listing to an age", async ({ page }) => {
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await mockPlanRoute(page);

    await page.goto("/plans.html");
    await page.locator("[data-testid='plan-age-filter']").selectOption("1y");

    const to = new Date();
    to.setFullYear(to.getFullYear() - 1);
    await expect(page).toHaveURL(new RegExp(`to=${to.toISOString().slice(0, 10)}`));
    await expect(page.locator("[data-testid='plan-filter-from']")).toBeVisible();
  });

  test("the Started control shows the age the listing is already narrowed to", async ({ page }) => {
    // a <select> cannot be set through Lit's `.value`: the property is committed
    // before its <option> children exist, so without setting it after render the
    // control reads "Any time" while the listing is narrowed to a period
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await mockPlanRoute(page);

    const twoYearsAgo = new Date();
    twoYearsAgo.setFullYear(twoYearsAgo.getFullYear() - 2);
    await page.goto(`/plans.html?to=${twoYearsAgo.toISOString().slice(0, 10)}`);

    await expect(page.locator("[data-testid='plan-age-filter']")).toHaveValue("2y");

    // and a bound that is none of the presets is shown as such, not as "Any time"
    await page.goto("/plans.html?to=2024-03-07");
    await expect(page.locator("[data-testid='plan-age-filter']")).toHaveValue("custom");
  });
});
