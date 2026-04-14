import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast } from "./helpers/routes.js";

test.describe("index.html — Home page", () => {
  test("loads with server info and user info (R25, R23)", async ({ page }) => {
    // Fail-fast registered first so it runs last (Playwright matches in reverse order)
    await setupFailFast(page);
    await setupCommonRoutes(page);

    await page.goto("/index.html");

    // Server version info renders in the footer
    await expect(page.locator(".serverInfo")).toContainText("5.1.24-SNAPSHOT");

    // User info renders in the header
    await expect(page.locator("#userInfoHolder")).toContainText("Test User");
    await expect(page.locator("#userInfoHolder")).toContainText("Logged in as");

    // Navigation buttons are visible
    await expect(page.locator('a[href="schedule-test.html"]')).toBeVisible();
    await expect(page.locator('a[href="logs.html"]')).toBeVisible();
    await expect(page.locator('a[href="plans.html"]')).toBeVisible();
  });

  test("unauthenticated state hides user info (R24)", async ({ page }) => {
    await setupFailFast(page);
    await setupCommonRoutes(page, { user: null });

    await page.goto("/index.html");

    // Server info still renders
    await expect(page.locator(".serverInfo")).toContainText("5.1.24-SNAPSHOT");

    // User info holder should not contain login info
    await expect(page.locator("#userInfoData")).not.toBeVisible();

    // Navigation buttons are still visible (public links)
    await expect(
      page.locator('a[href="logs.html?public=true"]'),
    ).toBeVisible();
    await expect(
      page.locator('a[href="plans.html?public=true"]'),
    ).toBeVisible();
  });
});
