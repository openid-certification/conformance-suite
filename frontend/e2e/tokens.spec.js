import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_TOKEN_USER, MOCK_TOKENS } from "./fixtures/tokens-data.js";

/**
 * Register a stateful GET /api/token handler so create/delete flows can
 * mutate the list and the next refetch sees the change. Returns a `state`
 * object whose `tokens` array can be reassigned by per-test handlers.
 *
 * cts-token-manager fetches `/api/token` with no query string — the
 * pattern omits the trailing `?*` the legacy DataTables route used.
 *
 * @param {import('@playwright/test').Page} page
 * @param {Array<object>} initialTokens
 * @returns {Promise<{ tokens: Array<object> }>}
 */
async function setupTokenListRoute(page, initialTokens) {
  /** @type {{ tokens: Array<object> }} */
  const state = { tokens: [...initialTokens] };
  await page.route("**/api/token", (route) => {
    if (route.request().method() !== "GET") {
      return route.fallback();
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(state.tokens),
    });
  });
  return state;
}

test.describe("tokens.html — API Tokens", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads and renders tokens in the cts-token-manager table", async ({ page }) => {
    await setupFailFast(page);
    await setupTokenListRoute(page, MOCK_TOKENS);
    await setupCommonRoutes(page, { user: MOCK_TOKEN_USER });

    await page.goto("/tokens.html");

    // Wait for cts-token-manager to fetch and render the table. The host
    // keeps `id="tokensListing"` for backward-compatible selectors.
    const rows = page.locator("#tokensListing tbody tr");
    await expect(rows.first()).toBeVisible();
    expect(await rows.count()).toBe(MOCK_TOKENS.length);

    // Token IDs from the fixture should be rendered as cell content
    await expect(page.locator("#tokensListing")).toContainText("token-abc-001");
    await expect(page.locator("#tokensListing")).toContainText("token-xyz-002");

    // The token list is delegated to cts-data-table; the host carries the
    // id="tokensListing" so descendant selectors keep working. The inner
    // <table class="oidf-dt-table"> is rendered by cts-data-table itself.
    await expect(page.locator("cts-data-table#tokensListing table.oidf-dt-table")).toBeVisible();
  });

  test("each row's Delete button renders as cts-button with variant=danger", async ({ page }) => {
    await setupFailFast(page);
    await setupTokenListRoute(page, MOCK_TOKENS);
    await setupCommonRoutes(page, { user: MOCK_TOKEN_USER });

    await page.goto("/tokens.html");

    await expect(page.locator("#tokensListing tbody tr").first()).toBeVisible();

    const deleteButtons = page.locator("#tokensListing tbody tr cts-button.deleteBtn");
    await expect(deleteButtons).toHaveCount(MOCK_TOKENS.length);

    const firstDelete = deleteButtons.first();
    await expect(firstDelete).toHaveAttribute("variant", "danger");
    await expect(firstDelete).toHaveAttribute("label", "Delete");

    // The cts-button renders an inner <button> with .oidf-btn-danger
    await expect(firstDelete.locator("button.oidf-btn-danger")).toBeVisible();
  });

  test("three header action buttons render as size=lg cts-button / cts-link-button", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupTokenListRoute(page, MOCK_TOKENS);
    await setupCommonRoutes(page, { user: MOCK_TOKEN_USER });

    await page.goto("/tokens.html");

    // cts-token-manager renders the create buttons inside its actions row.
    // It does not assign ids to them, so locate by label text.
    const actions = page.locator("cts-token-manager .cts-token-manager-actions");
    await expect(actions).toBeVisible();

    const newTemporary = actions.locator('cts-button[label="New temporary token"]');
    const newPermanent = actions.locator('cts-button[label="New permanent token"]');
    const apiDocs = actions.locator('cts-link-button[href="/api-document.html"]');

    await expect(newTemporary).toBeVisible();
    await expect(newPermanent).toBeVisible();
    await expect(apiDocs).toBeVisible();

    await expect(newTemporary).toHaveAttribute("size", "lg");
    await expect(newTemporary).toHaveAttribute("variant", "primary");
    await expect(newPermanent).toHaveAttribute("size", "lg");
    await expect(newPermanent).toHaveAttribute("variant", "primary");
    await expect(apiDocs).toHaveAttribute("size", "lg");
    // cts-link-button defaults to secondary inside cts-token-manager
    await expect(apiDocs).toHaveAttribute("variant", "secondary");

    // Inner elements should carry the .oidf-btn-lg class
    await expect(newTemporary.locator("button.oidf-btn-lg")).toBeVisible();
    await expect(newPermanent.locator("button.oidf-btn-lg")).toBeVisible();
    await expect(apiDocs.locator("a.oidf-btn-lg")).toBeVisible();
  });

  test("clicking Delete then Confirm fires DELETE /api/token/:id and the row disappears", async ({
    page,
  }) => {
    await setupFailFast(page);

    // Stateful token list: after DELETE succeeds, the next refetch sees the
    // reduced list so the row visibly disappears. Without a real server,
    // a stub that always returns MOCK_TOKENS would leave the row visible
    // and mask any regression that breaks the post-delete refetch.
    const state = await setupTokenListRoute(page, MOCK_TOKENS);

    await page.route("**/api/token/*", (route) => {
      if (route.request().method() !== "DELETE") {
        return route.fallback();
      }
      const url = route.request().url();
      const match = url.match(/\/api\/token\/([^/?]+)/);
      if (match) {
        const deletedId = decodeURIComponent(match[1]);
        state.tokens = state.tokens.filter((t) => t._id !== deletedId);
      }
      return route.fulfill({ status: 200, body: "" });
    });

    await setupCommonRoutes(page, { user: MOCK_TOKEN_USER });

    await page.goto("/tokens.html");

    const targetRow = page.locator("#tokensListing tbody tr", {
      hasText: "token-abc-001",
    });
    await expect(targetRow).toBeVisible();
    const targetDelete = targetRow.locator("cts-button.deleteBtn button");
    await expect(targetDelete).toBeVisible();

    const deleteRequest = page.waitForRequest(
      (req) => req.method() === "DELETE" && /\/api\/token\/token-abc-001$/.test(req.url()),
    );

    await targetDelete.click();

    // cts-token-manager opens its own #deleteTokenModal cts-modal; the
    // confirm button is rendered by cts-modal from the footer-buttons
    // descriptor with id="confirmDeleteBtn".
    const confirmBtn = page.locator("#confirmDeleteBtn");
    await confirmBtn.waitFor({ state: "visible" });
    await confirmBtn.click();

    await deleteRequest;

    // After the DELETE completes and cts-token-manager re-fetches, the row
    // for token-abc-001 must be gone. The other token remains.
    await expect(page.locator("#tokensListing tbody tr", { hasText: "token-abc-001" })).toHaveCount(
      0,
      { timeout: 5000 },
    );
    await expect(page.locator("#tokensListing tbody tr", { hasText: "token-xyz-002" })).toHaveCount(
      1,
    );
  });

  test("admin user sees the read-only admin message instead of the table", async ({ page }) => {
    await setupFailFast(page);
    await setupTokenListRoute(page, MOCK_TOKENS);
    await setupCommonRoutes(page, {
      user: { ...MOCK_TOKEN_USER, isAdmin: true },
    });

    await page.goto("/tokens.html");

    // Admin view: cts-token-manager hides the table and create buttons.
    const adminMessage = page.locator("cts-token-manager .admin-message");
    await expect(adminMessage).toBeVisible();
    await expect(adminMessage).toHaveText(/Admin users cannot create tokens/);

    await expect(page.locator("#tokensListing")).toHaveCount(0);
    await expect(page.locator('cts-button[label="New temporary token"]')).toHaveCount(0);
  });

  test("AJAX failure on token list surfaces an error message in cts-token-manager", async ({
    page,
  }) => {
    await setupFailFast(page);

    // Force /api/token to fail. cts-token-manager catches the rejection and
    // sets `_error`, swapping the table for the empty state — the create
    // buttons stay visible so the user can still try to mint a token.
    await page.route("**/api/token", (route) => {
      if (route.request().method() !== "GET") {
        return route.fallback();
      }
      return route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ error: "Internal server error" }),
      });
    });

    await setupCommonRoutes(page, { user: MOCK_TOKEN_USER });

    await page.goto("/tokens.html");

    // The table never materializes because the fetch failed; the empty
    // state message renders in its place once the loading flag clears.
    await expect(page.locator("cts-token-manager .no-tokens-message")).toBeVisible();
    await expect(page.locator("cts-data-table#tokensListing")).toHaveCount(0);

    // Create buttons are still present so the user is not stranded.
    await expect(page.locator('cts-button[label="New temporary token"]')).toBeVisible();
  });
});
