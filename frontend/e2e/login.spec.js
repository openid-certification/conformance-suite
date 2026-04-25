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

  test("renders cts-login-page with both OAuth cts-link-button elements", async ({ page }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html");

    const loginPage = page.locator("cts-login-page");
    await expect(loginPage).toBeVisible();

    const googleBtn = loginPage.locator('cts-link-button[href="/oauth2/authorization/google"]');
    const gitlabBtn = loginPage.locator('cts-link-button[href="/oauth2/authorization/gitlab"]');

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

  test("Google button uses variant=secondary (inner anchor has oidf-btn-secondary)", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html");

    const googleAnchor = page.locator('cts-link-button[href="/oauth2/authorization/google"] a');

    await expect(googleAnchor).toHaveClass(/\boidf-btn-secondary\b/);
    await expect(googleAnchor).toHaveAttribute("href", "/oauth2/authorization/google");
  });

  test("GitLab button uses variant=secondary (inner anchor has oidf-btn-secondary)", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html");

    const gitlabAnchor = page.locator('cts-link-button[href="/oauth2/authorization/gitlab"] a');

    await expect(gitlabAnchor).toHaveClass(/\boidf-btn-secondary\b/);
    await expect(gitlabAnchor).toHaveAttribute("href", "/oauth2/authorization/gitlab");
  });

  test("Bootstrap CSS/JS assets are not requested by the page", async ({ page }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    /** @type {string[]} */
    const requestedUrls = [];
    page.on("request", (request) => {
      requestedUrls.push(request.url());
    });

    await page.goto("/login.html");

    const bootstrapAssetRequests = requestedUrls.filter(
      (url) => /\/vendor\/bootstrap\//.test(url) || /\/vendor\/popper\//.test(url),
    );
    expect(bootstrapAssetRequests).toEqual([]);
  });

  test("renders error alert when ?error= URL parameter is present", async ({ page }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html?error=Invalid+credentials");

    const errorAlert = page.locator("cts-login-page .oidf-alert-danger");
    await expect(errorAlert).toBeVisible();
    await expect(errorAlert).toContainText("There was an error logging you in:");
    await expect(errorAlert).toContainText("Invalid credentials");
  });

  test("renders logout alert when ?logout= URL parameter is present", async ({ page }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html?logout=true");

    const logoutAlert = page.locator("cts-login-page .oidf-alert-info");
    await expect(logoutAlert).toBeVisible();
    await expect(logoutAlert).toContainText("You have been logged out.");
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
