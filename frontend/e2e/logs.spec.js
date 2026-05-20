import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  wrapDataTablesResponse,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_LOG_LIST } from "./fixtures/mock-log-list.js";

test.describe("logs.html — Logs List", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads and renders logs in cts-data-table (R27)", async ({ page }) => {
    await setupFailFast(page);

    // /api/log — DataTables-style server-side endpoint
    await page.route("**/api/log?*", (route) => {
      const envelope = wrapDataTablesResponse(MOCK_LOG_LIST, route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });

    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    // Cross-page contract: every wired page mounts a single <cts-toast-host>
    // for window.ctsToast(...). A silent removal of the mount from logs.html
    // would otherwise pass all tests in this file. (Mirrors upload.spec.js:210.)
    await expect(page.locator("cts-toast-host")).toHaveCount(1);

    // Wait for cts-data-table to render rows. The host element keeps the
    // legacy `#logsListing` id; the inner table lives in light DOM so the
    // descendant selector still matches.
    const rows = page.locator("#logsListing .oidf-dt-table tbody tr[data-row-index]");
    await expect(rows.first()).toBeVisible();

    // Should show test names
    await expect(page.locator("#logsListing")).toContainText("oidcc-server");

    // Should show results
    await expect(page.locator("#logsListing")).toContainText("PASSED");
    await expect(page.locator("#logsListing")).toContainText("WARNING");
  });

  test("search triggers cts-data-table re-fetch with Enter key", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/log?*", (route) => {
      const envelope = wrapDataTablesResponse(MOCK_LOG_LIST, route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });

    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    // Wait for initial table load
    await expect(
      page.locator("#logsListing .oidf-dt-table tbody tr[data-row-index]").first(),
    ).toBeVisible();

    // Type search term and press Enter — set up waitForRequest BEFORE the
    // action so the listener is active when the request fires
    const searchInput = page.locator("#logsListing .oidf-dt-search-input");
    await searchInput.fill("rotate-keys");
    const searchRequest = page.waitForRequest(
      (req) => req.url().includes("/api/log?") && req.url().includes("search=rotate-keys"),
    );
    await searchInput.press("Enter");
    await searchRequest;
  });

  test("config button in log row opens config modal", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/log?*", (route) => {
      const envelope = wrapDataTablesResponse(MOCK_LOG_LIST, route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });

    // Config button fetches the plan to show its config
    await page.route("**/api/plan/plan-001**", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          _id: "plan-001",
          config: { "server.issuer": "https://op.example.com" },
        }),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    // Wait for rows
    await expect(
      page.locator("#logsListing .oidf-dt-table tbody tr[data-row-index]").first(),
    ).toBeVisible();

    // Click the inner <button> rendered by the .showConfigBtn cts-button host.
    // Targeting the inner button rather than the host element matches the
    // pattern in plans.spec.js (and avoids edge cases where Playwright's
    // host-bbox click misses the actual button on hosts with margin/padding).
    const configBtn = page.locator(".showConfigBtn button").first();
    await expect(configBtn).toBeVisible();
    await configBtn.click();

    // Config modal opens
    const configModal = page.locator("#configModal");
    await expect(configModal).toBeVisible();
    await expect(page.locator("#config")).toContainText("server.issuer");

    // Toolbar shows the test id rather than a redundant "Configuration for"
    // line. The cts-modal heading already says "Configuration".
    await expect(configModal.locator("#configTestId")).toHaveText("test-log-001");

    // Copy button uses the canonical `copy` icon (was `log-out` before the
    // 2026-05-19 modal redesign). The cts-button host reflects the attribute,
    // and the inner cts-icon renders the matching glyph. The visible label
    // names the payload ("Copy configuration") so users don't conflate it
    // with the test-id label next to it.
    const copyBtn = configModal.locator(".btn-clipboard").first();
    await expect(copyBtn).toHaveAttribute("icon", "copy");
    await expect(copyBtn.locator('cts-icon[name="copy"]')).toBeVisible();
    await expect(copyBtn).toContainText("Copy configuration");
    // Structural smoke test: the cts-tooltip wrapper is present with the
    // expected content. The popover body is mounted on hover/focus by
    // cts-tooltip itself; covering the hover-reveal behavior is the
    // cts-tooltip primitive's own play-test, not this page-level spec.
    await expect(
      configModal.locator('cts-tooltip[content="Copy configuration JSON to clipboard"]'),
    ).toBeAttached();

    // Close modal
    await configModal.locator(".oidf-modal-close").first().click();
    await expect(configModal).toBeHidden();
  });
});

test.describe("logs.html — URL filtering", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  // When ?status= or ?result= is present the page switches cts-data-table to
  // client-side mode and fetches /api/log?length=1000 in a single request.
  // The route below returns the full fixture as a PaginationResponse envelope
  // regardless of pagination params, which matches the backend behaviour for
  // a dataset that fits in one page.
  /** @param {import('@playwright/test').Page} page */
  function setupFilterModeRoute(page) {
    return page.route("**/api/log?*", (/** @type {import('@playwright/test').Route} */ route) => {
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          draw: 1,
          recordsTotal: MOCK_LOG_LIST.length,
          recordsFiltered: MOCK_LOG_LIST.length,
          data: MOCK_LOG_LIST,
        }),
      });
    });
  }

  test("?status=running,waiting filters to in-progress rows and shows chip", async ({ page }) => {
    await setupFailFast(page);
    await setupFilterModeRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=running,waiting");

    // Chip is visible and labelled with the active facet
    const chip = page.locator("#activeFilterChip .oidf-page-filter-chip");
    await expect(chip).toBeVisible();
    await expect(chip).toContainText("Status: running or waiting");
    await expect(chip).toContainText("(2 matches)");

    // Only RUNNING + WAITING rows are rendered
    await expect(page.locator("#logsListing .oidf-dt-table tbody tr[data-row-index]")).toHaveCount(
      2,
    );
    await expect(page.locator("#logsListing")).toContainText("fapi2-running");
    await expect(page.locator("#logsListing")).toContainText("fapi2-waiting");
    await expect(page.locator("#logsListing")).not.toContainText("oidcc-server-rotate-keys");
  });

  test("?result=failed,unknown filters to failure rows", async ({ page }) => {
    await setupFailFast(page);
    await setupFilterModeRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?result=failed,unknown");

    const chip = page.locator("#activeFilterChip .oidf-page-filter-chip");
    await expect(chip).toBeVisible();
    await expect(chip).toContainText("Result: failed or unknown");
    // 3 rows match: 2 UNKNOWN (running, waiting) + 1 FAILED (vci-failed)
    await expect(chip).toContainText("(3 matches)");
    await expect(page.locator("#logsListing .oidf-dt-table tbody tr[data-row-index]")).toHaveCount(
      3,
    );
    await expect(page.locator("#logsListing")).not.toContainText("oidcc-server-rotate-keys");
  });

  test("combined ?status and ?result apply both filters", async ({ page }) => {
    await setupFailFast(page);
    await setupFilterModeRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=finished&result=failed");

    const chip = page.locator("#activeFilterChip .oidf-page-filter-chip");
    await expect(chip).toContainText("Status: finished");
    await expect(chip).toContainText("Result: failed");
    // Only vci-failed matches FINISHED + FAILED
    await expect(page.locator("#logsListing .oidf-dt-table tbody tr[data-row-index]")).toHaveCount(
      1,
    );
    await expect(page.locator("#logsListing")).toContainText("vci-failed");
  });

  test("clicking the clear-filter chip navigates to the unfiltered URL", async ({ page }) => {
    await setupFailFast(page);
    await setupFilterModeRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=running,waiting");

    const chip = page.locator("#activeFilterChip .oidf-page-filter-chip");
    await expect(chip).toBeVisible();
    await chip.click();
    await page.waitForURL("**/logs.html");
    // Chip is gone (the hidden wrapper has nothing inside)
    await expect(page.locator("#activeFilterChip .oidf-page-filter-chip")).toHaveCount(0);
  });

  test("?public=true&result=failed,unknown filters the published-logs view", async ({ page }) => {
    await setupFailFast(page);
    await setupFilterModeRoute(page);
    await setupCommonRoutes(page);

    // Capture the request so we can assert public=true was forwarded
    const requestPromise = page.waitForRequest(
      (req) => req.url().includes("/api/log") && req.url().includes("length=1000"),
    );
    await page.goto("/logs.html?public=true&result=failed,unknown");
    const req = await requestPromise;
    expect(req.url()).toContain("public=true");

    // Clicking the chip preserves ?public=true
    const chip = page.locator("#activeFilterChip .oidf-page-filter-chip");
    await chip.click();
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

  test("unknown filter tokens are silently dropped", async ({ page }) => {
    await setupFailFast(page);
    await setupFilterModeRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=running,bogus");

    const chip = page.locator("#activeFilterChip .oidf-page-filter-chip");
    await expect(chip).toContainText("Status: running");
    // "bogus" was dropped — only RUNNING rows match (1)
    await expect(page.locator("#logsListing .oidf-dt-table tbody tr[data-row-index]")).toHaveCount(
      1,
    );
  });

  test("?status=bogus with no valid tokens treats the param as inactive", async ({ page }) => {
    await setupFailFast(page);

    // No filter is active — page falls through to server-side mode, which
    // hits the standard /api/log?draw=...&start=...&length=... endpoint.
    await page.route("**/api/log?*", (route) => {
      const envelope = wrapDataTablesResponse(MOCK_LOG_LIST, route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });

    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=bogus");

    // No chip rendered
    await expect(page.locator("#activeFilterChip .oidf-page-filter-chip")).toHaveCount(0);
    // All rows visible (server-side mode with default page size = 25 shows all 5)
    await expect(page.locator("#logsListing .oidf-dt-table tbody tr[data-row-index]")).toHaveCount(
      MOCK_LOG_LIST.length,
    );
  });
});
