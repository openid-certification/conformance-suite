import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_LOG_LIST } from "./fixtures/mock-log-list.js";
import { MOCK_ADMIN_USER } from "./fixtures/mock-users.js";

// All filter / search / sort / pagination behaviour is now client-side over a
// single 1000-row fetch envelope — matching the cts-dashboard stats pattern.
// The route helper returns the same PaginationResponse shape regardless of
// pagination params, which mirrors backend behaviour for a dataset that fits
// in one page.
function setupLogListRoute(page, rows = MOCK_LOG_LIST) {
  return page.route("**/api/log?*", (route) => {
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        draw: 1,
        recordsTotal: rows.length,
        recordsFiltered: rows.length,
        data: rows,
      }),
    });
  });
}

test.describe("logs.html — Logs List", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads and renders log cards (R1)", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    // Cross-page contract: every wired page mounts a single <cts-toast-host>.
    await expect(page.locator("cts-toast-host")).toHaveCount(1);

    const items = page.locator('#logsListing [data-testid="log-list-item"]');
    await expect(items.first()).toBeVisible();
    await expect(items).toHaveCount(MOCK_LOG_LIST.length);

    // Test names render as the card headline.
    await expect(page.locator("#logsListing")).toContainText("oidcc-server");
    await expect(page.locator("#logsListing")).toContainText("oidcc-server-rotate-keys");

    // Status + result badges render.
    await expect(page.locator("#logsListing cts-badge[label='PASSED']")).toHaveCount(1);
    await expect(page.locator("#logsListing cts-badge[label='WARNING']")).toHaveCount(1);
    await expect(page.locator("#logsListing cts-badge[label='RUNNING']")).toHaveCount(1);
  });

  test("whole-card click navigates to log-detail.html (R12)", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    const firstCard = page.locator('#logsListing [data-testid="log-list-item"]').first();
    await expect(firstCard).toBeVisible();
    const href = await firstCard.getAttribute("href");
    expect(href).toMatch(/^log-detail\.html\?log=test-log-\d+/);
  });

  test("search input live-filters the rendered cards (R3)", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    await expect(page.locator('#logsListing [data-testid="log-list-item"]').first()).toBeVisible();

    const searchInput = page.locator("#logsListing .cts-log-list-search input");
    await searchInput.fill("rotate-keys");

    // Live filter — no /api/log re-fetch fires. The dataset is already in
    // memory from the initial fetch.
    const items = page.locator('#logsListing [data-testid="log-list-item"]');
    await expect(items).toHaveCount(1);
    await expect(items.first()).toContainText("oidcc-server-rotate-keys");

    // Clear the search — full list returns.
    await searchInput.fill("");
    await expect(items).toHaveCount(MOCK_LOG_LIST.length);
  });

  test("sort selector defaults to Started (newest) and reorders on change (R4)", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    await expect(page.locator('#logsListing [data-testid="log-list-item"]').first()).toBeVisible();

    const sortSelect = page.locator("#logsListing .cts-log-list-sort select");
    await expect(sortSelect).toHaveValue("started-desc");

    // Switch to name-asc — the first card is the alphabetically-earliest test
    // name in the fixture.
    await sortSelect.selectOption("name-asc");
    const firstCardName = page
      .locator('#logsListing [data-testid="log-list-item"]')
      .first()
      .locator(".cts-log-card-name");
    // Fixture's alphabetically-first name is "fapi2-running".
    await expect(firstCardName).toContainText("fapi2-running");
  });

  test("config button in card opens config modal and stops card navigation", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);

    // Config button fetches the plan to show its config. Cover every plan
    // referenced in the fixture so the test does not couple to which plan
    // ends up first under the started-desc default sort.
    await page.route("**/api/plan/**", (route) => {
      const url = new URL(route.request().url());
      const planId = url.pathname.replace("/api/plan/", "");
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          _id: planId,
          config: { "server.issuer": "https://op.example.com" },
        }),
      });
    });

    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    await expect(page.locator('#logsListing [data-testid="log-list-item"]').first()).toBeVisible();

    // Click the config button on a specific card (test-log-001) so the
    // expected testId in the modal toolbar is deterministic regardless of
    // the default sort order.
    const targetCard = page.locator(
      '#logsListing [data-testid="log-list-item"][data-test-id="test-log-001"]',
    );
    await expect(targetCard).toBeVisible();
    const configBtn = targetCard.locator(".showConfigBtn button");
    await expect(configBtn).toBeVisible();
    await configBtn.click();

    // Card navigation must NOT have fired — we are still on logs.html.
    expect(page.url()).toContain("logs.html");

    // Config modal opens.
    const configModal = page.locator("#cts-log-list-config-modal");
    await expect(configModal).toBeVisible();
    await expect(page.locator("#cts-log-list-config-editor")).toContainText("server.issuer");

    // Toolbar shows the test id.
    await expect(configModal.locator("#cts-log-list-config-test-id")).toHaveText("test-log-001");

    // Copy button uses the canonical `copy` icon and the visible label "Copy
    // configuration" — same contract as the legacy modal.
    const copyBtn = configModal.locator(".btn-clipboard").first();
    await expect(copyBtn).toHaveAttribute("icon", "copy");
    await expect(copyBtn.locator('cts-icon[name="copy"]')).toBeVisible();
    await expect(copyBtn).toContainText("Copy configuration");
    await expect(
      configModal.locator('cts-tooltip[content="Copy configuration JSON to clipboard"]'),
    ).toBeAttached();

    // Close modal.
    await configModal.locator(".oidf-modal-close").first().click();
    await expect(configModal).toBeHidden();
  });
});

test.describe("logs.html — Faceted filter chips and URL sync", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("?status=running,waiting boots with chips pre-selected (R9)", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=running,waiting");

    const runningChip = page.locator('#logsListing .cts-log-filter-chip[data-status="RUNNING"]');
    const waitingChip = page.locator('#logsListing .cts-log-filter-chip[data-status="WAITING"]');
    await expect(runningChip).toHaveAttribute("aria-pressed", "true");
    await expect(waitingChip).toHaveAttribute("aria-pressed", "true");

    // Only RUNNING + WAITING rows render.
    await expect(page.locator('#logsListing [data-testid="log-list-item"]')).toHaveCount(2);
    await expect(page.locator("#logsListing")).toContainText("fapi2-running");
    await expect(page.locator("#logsListing")).toContainText("fapi2-waiting");
    await expect(page.locator("#logsListing")).not.toContainText("oidcc-server-rotate-keys");

    // Active-filter summary visible.
    const summary = page.locator('#logsListing [data-testid="active-filter-summary"]');
    await expect(summary).toBeVisible();
    await expect(summary).toContainText("Status: running or waiting");
    await expect(summary).toContainText("(2 matches)");
  });

  test("?result=failed,unknown filters to failure rows", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?result=failed,unknown");

    const summary = page.locator('#logsListing [data-testid="active-filter-summary"]');
    await expect(summary).toBeVisible();
    await expect(summary).toContainText("Result: failed or unknown");
    // 3 rows match: 2 UNKNOWN (running, waiting) + 1 FAILED (vci-failed).
    await expect(summary).toContainText("(3 matches)");
    await expect(page.locator('#logsListing [data-testid="log-list-item"]')).toHaveCount(3);
  });

  test("combined ?status and ?result apply both filters", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=finished&result=failed");

    const summary = page.locator('#logsListing [data-testid="active-filter-summary"]');
    await expect(summary).toContainText("Status: finished");
    await expect(summary).toContainText("Result: failed");
    await expect(page.locator('#logsListing [data-testid="log-list-item"]')).toHaveCount(1);
    await expect(page.locator("#logsListing")).toContainText("vci-failed");
  });

  test("activating a status chip writes ?status= to the URL", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    await expect(page.locator('#logsListing [data-testid="log-list-item"]').first()).toBeVisible();

    const runningChip = page.locator('#logsListing .cts-log-filter-chip[data-status="RUNNING"]');
    await runningChip.click();

    // Wait for URL to update via history.replaceState.
    await page.waitForFunction(() => window.location.search.includes("status=running"));
    expect(page.url()).toContain("status=running");

    // Toggle it off — URL param disappears.
    await runningChip.click();
    await page.waitForFunction(() => !window.location.search.includes("status="));
    expect(page.url()).not.toContain("status=");
  });

  test("clicking the active-filter summary clears all filters", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=running,waiting");

    const summary = page.locator('#logsListing [data-testid="active-filter-summary"]');
    await expect(summary).toBeVisible();
    await summary.click();

    await page.waitForFunction(() => !window.location.search.includes("status="));
    await expect(summary).toHaveCount(0);
    await expect(page.locator('#logsListing [data-testid="log-list-item"]')).toHaveCount(
      MOCK_LOG_LIST.length,
    );
  });

  test('summary uses the vendored close glyph (regression: name="x")', async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=running,waiting");

    const summary = page.locator('#logsListing [data-testid="active-filter-summary"]');
    await expect(summary).toBeVisible();

    const dismissIcon = summary.locator('cts-icon[name="close-md"]');
    await expect(dismissIcon).toHaveCount(1);
    await expect(dismissIcon.locator("svg use")).toHaveAttribute(
      "href",
      "/vendor/coolicons/icons/close-md.svg#i",
    );
  });

  test("?public=true&result=failed,unknown preserves the public flag when cleared", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    // Capture the request so we can assert public=true was forwarded.
    const requestPromise = page.waitForRequest(
      (req) => req.url().includes("/api/log") && req.url().includes("length=1000"),
    );
    await page.goto("/logs.html?public=true&result=failed,unknown");
    const req = await requestPromise;
    expect(req.url()).toContain("public=true");

    // Clicking the summary preserves ?public=true.
    const summary = page.locator('#logsListing [data-testid="active-filter-summary"]');
    await summary.click();
    await page.waitForURL((url) => {
      const u = new URL(url);
      return (
        u.pathname.endsWith("/logs.html") &&
        u.searchParams.get("public") === "true" &&
        !u.searchParams.has("status") &&
        !u.searchParams.has("result")
      );
    });
  });

  test("?status=running,bogus drops unknown tokens silently", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=running,bogus");

    // "bogus" was dropped — only RUNNING is pressed and 1 row matches.
    const summary = page.locator('#logsListing [data-testid="active-filter-summary"]');
    await expect(summary).toContainText("Status: running");
    await expect(page.locator('#logsListing [data-testid="log-list-item"]')).toHaveCount(1);
  });

  test("?status=bogus with no valid tokens leaves all rows visible", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=bogus");

    // No summary rendered — no valid token survived.
    await expect(page.locator('#logsListing [data-testid="active-filter-summary"]')).toHaveCount(0);
    await expect(page.locator('#logsListing [data-testid="log-list-item"]')).toHaveCount(
      MOCK_LOG_LIST.length,
    );
  });

  test("Owner pill renders the two-tone chip with icons (admin only)", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/logs.html");

    const firstCard = page.locator('#logsListing [data-testid="log-list-item"]').first();
    await expect(firstCard).toBeVisible();

    const firstOwner = firstCard.locator(".log-owner");
    await expect(firstOwner).toBeVisible();

    const subPill = firstOwner.locator(".ownerSub");
    const issPill = firstOwner.locator(".ownerIss");
    await expect(subPill).toHaveCount(1);
    await expect(issPill).toHaveCount(1);
    await expect(subPill.locator('cts-icon[name="user-01"]')).toHaveCount(1);
    await expect(issPill.locator('cts-icon[name="globe"]')).toHaveCount(1);
    await expect(firstOwner.locator('cts-tooltip[content="12345"] > .ownerSub')).toHaveCount(1);
    await expect(
      firstOwner.locator('cts-tooltip[content="https://accounts.google.com"] > .ownerIss'),
    ).toHaveCount(1);
    await expect(subPill).toHaveAttribute("aria-label", "Subject: 12345");
    await expect(issPill).toHaveAttribute("aria-label", "Issuer: https://accounts.google.com");

    // Anti-wrap layout regression — pill stays on one line at narrow viewport.
    await page.setViewportSize({ width: 600, height: 800 });
    const box = await firstOwner.boundingBox();
    if (!box) throw new Error(".log-owner has no bounding box");
    expect(box.height).toBeLessThanOrEqual(32);
  });
});
