import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  wrapDataTablesResponse,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_LOG_LIST } from "./fixtures/mock-log-list.js";
import { MOCK_ADMIN_USER } from "./fixtures/mock-users.js";

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

  test("Started header is not sortable when it is the only sortable default", async ({ page }) => {
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

    // Capture the first /api/log request so we can confirm the default sort
    // order is still seeded by `initialSort` (server-side `order[0]` params).
    const initialLogRequestPromise = page.waitForRequest((req) => req.url().includes("/api/log?"));
    await page.goto("/logs.html");
    const initialLogRequest = await initialLogRequestPromise;

    await expect(
      page.locator("#logsListing .oidf-dt-table tbody tr[data-row-index]").first(),
    ).toBeVisible();

    const startedHeader = page.locator('#logsListing .oidf-dt-table th[data-column-key="started"]');
    await expect(startedHeader).toHaveCount(1);

    // Affordance gone: no sortable class, no arrow icon, no aria-sort attribute.
    await expect(startedHeader).not.toHaveClass(/(^|\s)is-sortable(\s|$)/);
    await expect(startedHeader.locator('cts-icon[name^="arrow-"]')).toHaveCount(0);
    expect(await startedHeader.getAttribute("aria-sort")).toBeNull();

    // Default sort still applied: initial fetch URL carries the order param
    // seeded by `initialSort = { column: "started", direction: "desc" }`.
    // logs.html uses `request-shape="datatables-comma-order"`, which
    // serializes the order as `order=COL,DIR`.
    const initialUrl = new URL(initialLogRequest.url());
    expect(initialUrl.searchParams.get("order")).toBe("started,desc");
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

  test('active-filter chip renders a vendored close glyph (regression: name="x")', async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupFilterModeRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=running,waiting");

    const chip = page.locator("#activeFilterChip .oidf-page-filter-chip");
    await expect(chip).toBeVisible();

    // The chip's trailing dismiss glyph must resolve to a vendored SVG. The
    // April 2026 coolicons migration mapped `x` → `close-md`; a later refactor
    // re-introduced `name="x"`, which 404s silently because no x.svg exists in
    // vendor/coolicons/icons/. Pin the name and the resolved <use href>.
    const dismissIcon = chip.locator('cts-icon[name="close-md"]');
    await expect(dismissIcon).toHaveCount(1);
    await expect(dismissIcon.locator("svg use")).toHaveAttribute(
      "href",
      "/vendor/coolicons/icons/close-md.svg#i",
    );
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

  test("Owner column renders the two-tone pill with icons inside each half (admin only)", async ({
    page,
  }) => {
    await setupFailFast(page);
    await page.route("**/api/log?*", (route) => {
      const envelope = wrapDataTablesResponse(MOCK_LOG_LIST, route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/logs.html");

    // Wait for at least one row to render so the owner cell template has run.
    await expect(
      page.locator("#logsListing .oidf-dt-table tbody tr[data-row-index]").first(),
    ).toBeVisible();

    const firstOwner = page
      .locator("#logsListing .oidf-dt-table tbody tr[data-row-index]")
      .first()
      .locator(".log-owner");
    await expect(firstOwner).toBeVisible();

    // The April 2026 bi-* → cts-icon migration corrupted this template so the
    // cts-icons appeared OUTSIDE the .ownerSub/.ownerIss pills. Pin the
    // correct hierarchy: each pill wraps exactly one cts-icon, and the
    // wrapping cts-tooltip + the chip's aria-label expose the sub/iss
    // values (replacing the native `title` attribute that used to live on
    // the chip itself).
    const subPill = firstOwner.locator(".ownerSub");
    const issPill = firstOwner.locator(".ownerIss");
    await expect(subPill).toHaveCount(1);
    await expect(issPill).toHaveCount(1);
    await expect(subPill.locator('cts-icon[name="user-01"]')).toHaveCount(1);
    await expect(issPill.locator('cts-icon[name="globe"]')).toHaveCount(1);
    // cts-tooltip wraps each chip and carries the value in `content`. The
    // chip itself keeps `aria-label` (screen readers don't reliably read
    // visual tooltip content) and gets `tabindex="0"` so the tooltip is
    // keyboard-reachable.
    await expect(firstOwner.locator('cts-tooltip[content="12345"] > .ownerSub')).toHaveCount(1);
    await expect(
      firstOwner.locator('cts-tooltip[content="https://accounts.google.com"] > .ownerIss'),
    ).toHaveCount(1);
    await expect(subPill).toHaveAttribute("aria-label", "Subject: 12345");
    await expect(issPill).toHaveAttribute("aria-label", "Issuer: https://accounts.google.com");
    await expect(subPill).toHaveAttribute("tabindex", "0");
    await expect(issPill).toHaveAttribute("tabindex", "0");
    // The native `title` attribute is gone — replaced by cts-tooltip.
    expect(await subPill.getAttribute("title")).toBeNull();
    expect(await issPill.getAttribute("title")).toBeNull();

    // Failing-shape negative assertion: the pre-fix bug rendered cts-icon
    // as the OUTER wrapper with .ownerSub nested inside it. If this shape
    // ever returns, fail loudly here rather than discovering it visually.
    await expect(page.locator("#logsListing cts-icon > .ownerSub")).toHaveCount(0);
    await expect(page.locator("#logsListing cts-icon > .ownerIss")).toHaveCount(0);

    // Anti-wrap layout regression. Shrinking the logs table cell would have
    // historically caused the two-tone pill to break onto two lines because
    // .ownerSub and .ownerIss were inline-block siblings with no nowrap
    // scope. After the fix (display: inline-flex; flex-wrap: nowrap on
    // .log-owner), the pill stays on one line at any viewport width.
    await page.setViewportSize({ width: 600, height: 800 });
    const box = await firstOwner.boundingBox();
    if (!box) throw new Error(".log-owner has no bounding box");
    // Pill is .ownerSub padding (2px+2px) + ~16px icon ≈ 20px. The chip is
    // read-only per CLAUDE.md's badge convention (fill only, no border).
    // 32px is the spec-defined ceiling for "single line".
    expect(box.height).toBeLessThanOrEqual(32);
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
