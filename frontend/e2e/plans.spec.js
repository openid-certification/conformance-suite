import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_PLAN_LIST } from "./fixtures/mock-plans.js";
import { MOCK_ADMIN_USER } from "./fixtures/mock-users.js";

/**
 * After the cts-plan-list wire-up, plans.html mounts a single
 * <cts-plan-list> element. The component fetches /api/plan directly
 * (or /api/plan?public=true for the published listing) and returns a
 * plain array — no DataTables server-side envelope. Search is
 * client-side filtered (live-debounced), so no fetch fires on search
 * input. Plan-name clicks emit `cts-plan-navigate`, which the page
 * routes to `plan-detail.html?plan=<id>`.
 *
 * The host element keeps `id="plansListing"` so `#plansListing` selectors
 * still resolve; cts-plan-list is Light DOM so descendant queries work
 * through to the inner cts-data-table.
 */

/**
 * @param {import('@playwright/test').Page} page
 */
function mockPlanRoute(page) {
  return page.route("**/api/plan*", (route) => {
    const url = new URL(route.request().url());
    const isPublic = url.searchParams.get("public") === "true";
    const body = isPublic ? MOCK_PLAN_LIST.filter((p) => p.publish) : MOCK_PLAN_LIST;
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(body),
    });
  });
}

test.describe("plans.html — Plans List", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads and renders plans in cts-plan-list", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    // cts-plan-list owns its own loading state; wait for the table to
    // mount inside the host (rows carry data-row-index from cts-data-table).
    const rows = page.locator("#plansListing tbody tr[data-row-index]");
    await expect(rows.first()).toBeVisible();
    await expect(rows).toHaveCount(MOCK_PLAN_LIST.length);

    // Plan names render as links inside the Plan Name column.
    await expect(page.locator("#plansListing")).toContainText(
      "oidcc-basic-certification-test-plan",
    );

    // Variant key=value pairs render in the Variant column.
    await expect(page.locator("#plansListing")).toContainText("client_secret_basic");

    // The started timestamp renders as a real <span class="tabular-nums">,
    // not as escaped markup. Guard the wrap to catch a future regression
    // where the cellRenderer drops the tabular-nums helper class.
    const startedSpan = rows.first().locator("span.tabular-nums").first();
    await expect(startedSpan).toBeVisible();
    await expect(rows.first()).not.toContainText('<span class="tabular-nums">');

    // Non-admin users do not see the Owner column.
    await expect(page.locator("#plansListing thead")).not.toContainText("Owner");
  });

  test("admin users see the Owner column", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plans.html");

    const rows = page.locator("#plansListing tbody tr[data-row-index]");
    await expect(rows.first()).toBeVisible();

    await expect(page.locator("#plansListing thead")).toContainText("Owner");
    const ownerCells = page.locator("#plansListing tbody .owner-cell");
    await expect(ownerCells.first()).toBeVisible();
  });

  test("?public=true requests the published listing and hides admin affordances", async ({
    page,
  }) => {
    await setupFailFast(page);

    // Capture the request URL so we can assert the ?public=true query
    // was preserved on the fetch (R2 wiring contract).
    /** @type {string[]} */
    const planRequests = [];
    await page.route("**/api/plan*", (route) => {
      const url = route.request().url();
      planRequests.push(url);
      const parsed = new URL(url);
      const isPublic = parsed.searchParams.get("public") === "true";
      const body = isPublic ? MOCK_PLAN_LIST.filter((p) => p.publish) : MOCK_PLAN_LIST;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(body),
      });
    });
    await setupCommonRoutes(page);

    await page.goto("/plans.html?public=true");

    const rows = page.locator("#plansListing tbody tr[data-row-index]");
    await expect(rows.first()).toBeVisible();

    // Only published plans surface in the public view.
    const publishedCount = MOCK_PLAN_LIST.filter((p) => p.publish).length;
    await expect(rows).toHaveCount(publishedCount);

    // The component fetched with ?public=true on the URL.
    expect(planRequests.some((url) => url.includes("public=true"))).toBe(true);

    // Owner column stays hidden in the public view regardless of session.
    await expect(page.locator("#plansListing thead")).not.toContainText("Owner");

    // Config column is also hidden in public view. PublicPlan omits the
    // config field server-side, so a Config button would open an empty
    // modal. Legacy plans.html had `visible: !public` on the Config
    // column; cts-plan-list mirrors that.
    await expect(page.locator("#plansListing thead")).not.toContainText("Config");
    await expect(page.locator("#plansListing .showConfigBtn")).toHaveCount(0);
  });

  test("admin viewing ?public=true still hides Owner and Config", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plans.html?public=true");

    const rows = page.locator("#plansListing tbody tr[data-row-index]");
    await expect(rows.first()).toBeVisible();

    // Legacy parity: the public listing never reveals owner or config
    // affordances, even to an admin. Without this guard the admin's
    // session would gain visibility into owner.sub on the published
    // page, which the legacy code explicitly suppressed.
    await expect(page.locator("#plansListing thead")).not.toContainText("Owner");
    await expect(page.locator("#plansListing thead")).not.toContainText("Config");
  });

  test("clicking a plan name navigates to plan-detail.html", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);

    // Stub plan-detail.html so the assertion can verify the navigation
    // target without depending on the real detail page loading.
    await page.route("**/plan-detail.html*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "text/html",
        body: "<!DOCTYPE html><html><body>plan-detail stub</body></html>",
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    const planLink = page.locator("#plansListing .plan-name-link").first();
    await expect(planLink).toBeVisible();

    await Promise.all([page.waitForURL(/plan-detail\.html\?plan=plan-001/), planLink.click()]);

    expect(page.url()).toContain("plan-detail.html?plan=plan-001");
  });

  test("config button opens modal and exposes copy affordance", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    await expect(page.locator("#plansListing tbody tr[data-row-index]").first()).toBeVisible();

    // Click the inner <button> rendered inside the cts-button host —
    // cts-button binds @click on the inner button, so clicking the host
    // bypasses Lit's handler (see cts-plan-list.stories.js for the same
    // pattern documented).
    const configBtn = page.locator(".showConfigBtn button").first();
    await expect(configBtn).toBeVisible();
    await configBtn.click();

    // The component owns its own modal — id="planConfigModal".
    const modal = page.locator("#planConfigModal");
    await expect(modal).toBeVisible();

    // Plan ID surfaces in the modal toolbar so the user can correlate
    // the config blob with the row they clicked.
    await expect(modal).toContainText("plan-001");

    // The JSON editor renders inside the modal with the config JSON.
    const editor = page.locator("cts-json-editor.config-json");
    await expect(editor).toBeAttached();

    // Assert the editor is populated with the row's config — not just
    // attached. The component reads `.value` synchronously off the
    // editor host, so the page wiring is verified end-to-end without
    // depending on Monaco's render lifecycle.
    const editorValue = await editor.evaluate((el) => /** @type {any} */ (el).value);
    expect(editorValue).toContain("server.issuer");
    expect(editorValue).toContain("op.example.com");

    // Copy button is present and clickable; we don't assert clipboard
    // writes here because headless Chromium denies real clipboard ops —
    // the storybook play function covers the clipboard.writeText call.
    const copyBtn = page.locator(".copy-config-btn button").first();
    await expect(copyBtn).toBeVisible();
  });

  test("search input filters plans client-side without re-fetching", async ({ page }) => {
    await setupFailFast(page);

    /** @type {string[]} */
    const planRequests = [];
    await page.route("**/api/plan*", (route) => {
      planRequests.push(route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_LIST),
      });
    });
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    const rows = page.locator("#plansListing tbody tr[data-row-index]");
    await expect(rows).toHaveCount(MOCK_PLAN_LIST.length);

    const fetchesBeforeSearch = planRequests.length;

    // cts-data-table in live-debounced mode renders the search input
    // with the placeholder cts-plan-list configures.
    const searchInput = page.locator('input[placeholder="Search test plans..."]');
    await expect(searchInput).toBeVisible();
    await searchInput.fill("fapi2");

    // Live-debounced filter narrows the rendered rows to the match.
    await expect(rows).toHaveCount(1);
    await expect(page.locator("#plansListing")).toContainText(
      "fapi2-security-profile-final-test-plan",
    );

    // No additional /api/plan fetch should fire — search is local.
    expect(planRequests.length).toBe(fetchesBeforeSearch);
  });
});
