import { test, expect } from "@playwright/test";
import { setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";

test.describe("login.html — Login page", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  // The login page is unauthenticated — the embedded cts-navbar still calls
  // /api/currentuser on load, so we stub it with a 401 to simulate the
  // unauthenticated state. No other API calls are expected.
  async function setupLoginRoutes(page) {
    await page.route("**/api/currentuser", (route) => route.fulfill({ status: 401, body: "" }));
  }

  test("renders both OAuth cts-link-button elements with correct href and label", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html");

    const googleBtn = page.locator('cts-link-button[href="/oauth2/authorization/google"]');
    const gitlabBtn = page.locator('cts-link-button[href="/oauth2/authorization/gitlab"]');

    await expect(googleBtn).toBeVisible();
    await expect(googleBtn).toContainText("Proceed with Google");

    await expect(gitlabBtn).toBeVisible();
    await expect(gitlabBtn).toContainText("Proceed with GitLab");
  });

  test("both OAuth buttons render at size=lg (inner anchor has oidf-btn-lg)", async ({ page }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html");

    const googleAnchor = page.locator('cts-link-button[href="/oauth2/authorization/google"] a');
    const gitlabAnchor = page.locator('cts-link-button[href="/oauth2/authorization/gitlab"] a');

    await expect(googleAnchor).toHaveClass(/\boidf-btn-lg\b/);
    await expect(gitlabAnchor).toHaveClass(/\boidf-btn-lg\b/);
  });

  test("Google button uses variant=danger (inner anchor has oidf-btn-danger)", async ({ page }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html");

    const googleAnchor = page.locator('cts-link-button[href="/oauth2/authorization/google"] a');

    await expect(googleAnchor).toHaveClass(/\boidf-btn-danger\b/);
    await expect(googleAnchor).toHaveAttribute("href", "/oauth2/authorization/google");
  });

  test("GitLab button uses variant=primary (inner anchor has oidf-btn-primary)", async ({ page }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html");

    const gitlabAnchor = page.locator('cts-link-button[href="/oauth2/authorization/gitlab"] a');

    await expect(gitlabAnchor).toHaveClass(/\boidf-btn-primary\b/);
    await expect(gitlabAnchor).toHaveAttribute("href", "/oauth2/authorization/gitlab");
  });

  test("cts-navbar renders in unauthenticated state", async ({ page }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html");

    const navbar = page.locator("cts-navbar");
    await expect(navbar).toBeVisible();

    // Unauthenticated: no "Logged in as" text, and public nav links are shown.
    await expect(navbar).not.toContainText("Logged in as");
    await expect(navbar.locator('a[href="logs.html?public=true"]')).toBeVisible();
    await expect(navbar.locator('a[href="plans.html?public=true"]')).toBeVisible();
  });
});
