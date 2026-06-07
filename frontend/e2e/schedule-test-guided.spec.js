import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_PLANS, MOCK_PLAN_NO_VARIANTS } from "./fixtures/mock-plans.js";

const ALL_PLANS = [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS];

/**
 * Guided-mode coverage for schedule-test.html: the persistent
 * Guided | Advanced toggle, the mode-resolution ladder's user-visible
 * behavior, and (in later units) the guided journey itself.
 *
 * The advanced surface keeps its own coverage in schedule-test.spec.js,
 * which forces `oidf-guided-mode=advanced` up front; this file owns the
 * guided default and the switching behavior.
 */

/**
 * Register the routes the schedule-test init chain always hits, regardless
 * of mode: plans catalog, lastconfig probe, and the common trio.
 *
 * @param {import('@playwright/test').Page} page
 * @param {object} [options]
 * @param {Array<object>} [options.plans]
 * @param {object|null} [options.user] - Forwarded to setupCommonRoutes (null → 401).
 */
async function setupScheduleTestRoutes(page, options = {}) {
  await setupFailFast(page);
  await page.route("**/api/plan/available", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(options.plans || ALL_PLANS),
    }),
  );
  await page.route("**/api/lastconfig", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({}) }),
  );
  await setupCommonRoutes(page, options.user !== undefined ? { user: options.user } : {});
}

test.describe("schedule-test.html — Guided | Advanced mode toggle", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("first visit (no stored preference) lands in guided mode", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");

    await expect(page.locator("#guidedIsland")).toBeVisible();
    await expect(page.locator("#scheduleTestPage")).toBeHidden();
    await expect(page.locator("#modeGuidedBtn")).toHaveAttribute("aria-pressed", "true");
    await expect(page.locator("#modeAdvancedBtn")).toHaveAttribute("aria-pressed", "false");
  });

  test("toggle to advanced persists across a reload; toggling back restores guided", async ({
    page,
  }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");
    await expect(page.locator("#guidedIsland")).toBeVisible();

    await page.locator("#modeAdvancedBtn").click();
    await expect(page.locator("#scheduleTestPage")).toBeVisible();
    await expect(page.locator("#guidedIsland")).toBeHidden();
    await expect(page.locator("#modeAdvancedBtn")).toHaveAttribute("aria-pressed", "true");

    // The explicit switch persisted — a reload stays in advanced.
    await page.reload();
    await expect(page.locator("#scheduleTestPage")).toBeVisible();
    await expect(page.locator("#guidedIsland")).toBeHidden();

    // And the toggle is symmetric.
    await page.locator("#modeGuidedBtn").click();
    await expect(page.locator("#guidedIsland")).toBeVisible();
    await expect(page.locator("#scheduleTestPage")).toBeHidden();
    await page.reload();
    await expect(page.locator("#guidedIsland")).toBeVisible();
  });

  test("?test_plan= deep-link forces advanced for a stored-guided user and applies the preset", async ({
    page,
  }) => {
    await page.addInitScript(() => {
      try {
        localStorage.setItem("oidf-guided-mode", "guided");
      } catch {
        /* storage unavailable — the test will surface it */
      }
    });
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html?test_plan=oidcc-basic-certification-test-plan");

    // Advanced island shown, guided untouched (R9 forcing).
    await expect(page.locator("#scheduleTestPage")).toBeVisible();
    await expect(page.locator("#guidedIsland")).toBeHidden();
    await expect(page.locator("#modeAdvancedBtn")).toHaveAttribute("aria-pressed", "true");

    // The advanced hydration ran: the cascade resolved the deep-linked plan.
    await expect(page.locator("#planSelect")).toHaveValue("oidcc-basic-certification-test-plan");

    // Deep-link forcing is transient — it must NOT overwrite the stored
    // preference. A plain reload returns the user to guided.
    await page.goto("/schedule-test.html");
    await expect(page.locator("#guidedIsland")).toBeVisible();
    await expect(page.locator("#scheduleTestPage")).toBeHidden();
  });

  test("mode switch moves focus to the revealed island", async ({ page }) => {
    await setupScheduleTestRoutes(page);
    await page.goto("/schedule-test.html");
    await expect(page.locator("#guidedIsland")).toBeVisible();

    await page.locator("#modeAdvancedBtn").click();
    await expect(page.locator("#scheduleTestPage")).toBeFocused();

    await page.locator("#modeGuidedBtn").click();
    // The guided island's stage heading is the focus target when present.
    await expect(page.locator("#guidedIsland h1")).toBeFocused();
  });
});
