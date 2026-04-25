import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_TOKEN_USER, MOCK_TOKENS } from "./fixtures/tokens-data.js";

test.describe("tokens.html — API Tokens", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads and renders tokens in DataTable", async ({ page }) => {
    await setupFailFast(page);

    // GET /api/token returns a plain array (DataTable uses `dataSrc: ''`),
    // NOT the DataTables server-side envelope.
    await page.route("**/api/token?*", (route) => {
      if (route.request().method() !== "GET") {
        return route.fallback();
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_TOKENS),
      });
    });

    await setupCommonRoutes(page, { user: MOCK_TOKEN_USER });

    await page.goto("/tokens.html");

    // Wait for the DataTable to render rows
    const rows = page.locator("#tokensListing tbody tr");
    await expect(rows.first()).toBeVisible();
    expect(await rows.count()).toBeGreaterThanOrEqual(1);

    // Token IDs from the fixture should be rendered
    await expect(page.locator("#tokensListing")).toContainText("token-abc-001");
  });

  test("each row's Delete button renders as cts-button with variant=danger", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/token?*", (route) => {
      if (route.request().method() !== "GET") {
        return route.fallback();
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_TOKENS),
      });
    });

    await setupCommonRoutes(page, { user: MOCK_TOKEN_USER });

    await page.goto("/tokens.html");

    await expect(page.locator("#tokensListing tbody tr").first()).toBeVisible();

    // Delete button is a <cts-button variant="danger" class="deleteBtn">
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

    await page.route("**/api/token?*", (route) => {
      if (route.request().method() !== "GET") {
        return route.fallback();
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_TOKENS),
      });
    });

    await setupCommonRoutes(page, { user: MOCK_TOKEN_USER });

    await page.goto("/tokens.html");

    // Wait for the tokenTable template to render the header buttons
    const newTemporary = page.locator("cts-button#newTemporaryToken");
    const newPermanent = page.locator("cts-button#newPermanentToken");
    const apiDocs = page.locator('cts-link-button[href="/api-document.html"]');

    await expect(newTemporary).toBeVisible();
    await expect(newPermanent).toBeVisible();
    await expect(apiDocs).toBeVisible();

    await expect(newTemporary).toHaveAttribute("size", "lg");
    await expect(newTemporary).toHaveAttribute("variant", "primary");
    await expect(newPermanent).toHaveAttribute("size", "lg");
    await expect(newPermanent).toHaveAttribute("variant", "primary");
    await expect(apiDocs).toHaveAttribute("size", "lg");
    await expect(apiDocs).toHaveAttribute("variant", "primary");

    // Inner elements should carry the .oidf-btn-lg class
    await expect(newTemporary.locator("button.oidf-btn-lg")).toBeVisible();
    await expect(newPermanent.locator("button.oidf-btn-lg")).toBeVisible();
    await expect(apiDocs.locator("a.oidf-btn-lg")).toBeVisible();
  });

  test("clipboard button in createdModal renders as cts-button with class btn-clipboard", async ({
    page,
  }) => {
    await setupFailFast(page);

    await page.route("**/api/token?*", (route) => {
      if (route.request().method() !== "GET") {
        return route.fallback();
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_TOKENS),
      });
    });

    await setupCommonRoutes(page, { user: MOCK_TOKEN_USER });

    await page.goto("/tokens.html");

    // createdModal is hidden but present in the DOM
    const clipboardBtn = page.locator("cts-button.btn-clipboard");
    await expect(clipboardBtn).toHaveCount(1);
    await expect(clipboardBtn).toHaveAttribute("icon", "box-arrow-in-right");
    await expect(clipboardBtn).toHaveAttribute("data-clipboard-target", "#tokenValue");
  });

  test("clicking Delete then Confirm fires DELETE /api/token/:id and the row disappears", async ({
    page,
  }) => {
    await setupFailFast(page);

    // Stateful token list: after DELETE succeeds, subsequent GETs return the
    // reduced list so the page's post-delete refetch actually sees the row
    // gone. The previous test only asserted the DELETE fired — a bug that
    // left the row visible after server success would have gone uncaught.
    let currentTokens = [...MOCK_TOKENS];

    await page.route("**/api/token?*", (route) => {
      if (route.request().method() !== "GET") {
        return route.fallback();
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(currentTokens),
      });
    });

    await page.route("**/api/token/*", (route) => {
      if (route.request().method() !== "DELETE") {
        return route.fallback();
      }
      const url = route.request().url();
      const match = url.match(/\/api\/token\/([^/?]+)/);
      if (match) {
        const deletedId = decodeURIComponent(match[1]);
        currentTokens = currentTokens.filter((t) => t._id !== deletedId);
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

    const confirmBtn = page.locator("#confirmDelete");
    await confirmBtn.waitFor({ state: "visible" });
    await confirmBtn.click();

    await deleteRequest;

    // After the DELETE completes and the page re-fetches, the row for
    // token-abc-001 must be gone. The other token remains.
    await expect(page.locator("#tokensListing tbody tr", { hasText: "token-abc-001" })).toHaveCount(
      0,
      { timeout: 5000 },
    );
    await expect(page.locator("#tokensListing tbody tr", { hasText: "token-xyz-002" })).toHaveCount(
      1,
    );
  });
});
