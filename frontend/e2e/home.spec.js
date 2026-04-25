import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";

test.describe("index.html — Home page", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads with server info and user info (R25, R23)", async ({ page }) => {
    // Fail-fast registered first so it runs last (Playwright matches in reverse order)
    await setupFailFast(page);
    await setupCommonRoutes(page);

    await page.goto("/index.html");

    // Server version info renders in the dashboard footer (cts-dashboard owns it)
    await expect(page.locator(".serverInfo")).toContainText("5.1.24-SNAPSHOT");

    // User info renders in the navbar
    const navbar = page.locator("cts-navbar");
    await expect(navbar).toContainText("Test User");
    await expect(navbar).toContainText("Logged in as");

    // Navigation tiles are visible in the dashboard
    const homePage = page.locator("#homePage");
    await expect(
      homePage.locator('a.oidf-dashboard-tile[href="schedule-test.html"]'),
    ).toBeVisible();
    await expect(homePage.locator('a.oidf-dashboard-tile[href="logs.html"]')).toBeVisible();
    await expect(homePage.locator('a.oidf-dashboard-tile[href="plans.html"]')).toBeVisible();
  });

  test("unauthenticated state hides user info (R24)", async ({ page }) => {
    await setupFailFast(page);
    await setupCommonRoutes(page, { user: null });

    await page.goto("/index.html");

    // Server info still renders
    await expect(page.locator(".serverInfo")).toContainText("5.1.24-SNAPSHOT");

    // Navbar should not contain login info
    const navbar = page.locator("cts-navbar");
    await expect(navbar).not.toContainText("Logged in as");

    // Public tiles are visible in the dashboard
    const homePage = page.locator("#homePage");
    await expect(
      homePage.locator('a.oidf-dashboard-tile[href="logs.html?public=true"]'),
    ).toBeVisible();
    await expect(
      homePage.locator('a.oidf-dashboard-tile[href="plans.html?public=true"]'),
    ).toBeVisible();
  });
});
