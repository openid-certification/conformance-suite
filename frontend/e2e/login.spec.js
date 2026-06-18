import { test, expect } from "@playwright/test";
import { setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_SERVER_INFO } from "./fixtures/mock-server.js";

test.describe("login.html — Login page", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  // The login page is unauthenticated — the embedded cts-navbar still calls
  // /api/currentuser on load, so we stub it with a 401 to simulate the
  // unauthenticated state. The cts-footer (added suite-wide in U4) fetches
  // /api/server for its server-info line, so we stub that too. No other API
  // calls are expected.
  async function setupLoginRoutes(page) {
    await page.route("**/api/currentuser", (route) => route.fulfill({ status: 401, body: "" }));
    await page.route("**/api/server", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_SERVER_INFO),
      }),
    );
  }

  test("renders cts-login-page with both OAuth buttons", async ({ page }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html");

    const loginPage = page.locator("cts-login-page");
    await expect(loginPage).toBeVisible();

    const googleBtn = loginPage.locator('a[href="/oauth2/authorization/google"]');
    const gitlabBtn = loginPage.locator('a[href="/oauth2/authorization/gitlab"]');

    await expect(googleBtn).toBeVisible();
    await expect(googleBtn).toContainText("Proceed with Google");

    await expect(gitlabBtn).toBeVisible();
    await expect(gitlabBtn).toContainText("Proceed with GitLab");
  });

  test("both OAuth buttons render at size=lg (anchor has oidf-btn-lg)", async ({ page }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html");

    const googleAnchor = page.locator('a[href="/oauth2/authorization/google"]');
    const gitlabAnchor = page.locator('a[href="/oauth2/authorization/gitlab"]');

    await expect(googleAnchor).toHaveClass(/\boidf-btn-lg\b/);
    await expect(gitlabAnchor).toHaveClass(/\boidf-btn-lg\b/);
  });

  test("Google button uses variant=secondary (anchor has oidf-btn-secondary)", async ({ page }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html");

    const googleAnchor = page.locator('a[href="/oauth2/authorization/google"]');

    await expect(googleAnchor).toHaveClass(/\boidf-btn-secondary\b/);
    await expect(googleAnchor).toHaveAttribute("href", "/oauth2/authorization/google");
  });

  test("GitLab button uses variant=secondary (anchor has oidf-btn-secondary)", async ({ page }) => {
    await setupFailFast(page);
    await setupLoginRoutes(page);

    await page.goto("/login.html");

    const gitlabAnchor = page.locator('a[href="/oauth2/authorization/gitlab"]');

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

    // Cross-page contract: every wired page mounts a single <cts-toast-host>
    // for window.ctsToast(...). A silent removal of the mount from login.html
    // would otherwise pass all tests in this file. (Mirrors upload.spec.js:210.)
    await expect(page.locator("cts-toast-host")).toHaveCount(1);

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

    // Logged-out visitors: the brand/logo points at the login page, not the
    // plans listing (logged-out-landing fix). On login.html the navbar
    // resolves the anonymous state synchronously, so this is a self-link.
    await expect(navbar.locator("a.cts-brand")).toHaveAttribute("href", "login.html");
  });
});
