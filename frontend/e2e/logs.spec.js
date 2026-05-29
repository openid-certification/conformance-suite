import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  expectNoUnmockedCalls,
  recordLogRoute,
} from "./helpers/routes.js";
import { MOCK_LOG_LIST } from "./fixtures/mock-log-list.js";
import { MOCK_ADMIN_USER } from "./fixtures/mock-users.js";

// All filter / search / sort / pagination behaviour is now client-side over a
// single 1000-row fetch envelope — matching the cts-dashboard stats pattern.
// The route helper returns the same PaginationResponse shape regardless of
// pagination params, which mirrors backend behaviour for a dataset that fits
// in one page.
//
// `cts-log-list` also resolves a kebab-case `planName` per unique `planId`
// via `/api/plan/<id>` so the meta-row "Plan" chip shows the spec identifier
// instead of the opaque MongoDB id. The default plan-name stub is bundled
// here so every test in this file picks it up — individual tests can still
// register a more specific `**/api/plan/**` route AFTER calling this helper
// to override the default (Playwright matches routes in reverse registration
// order).
/**
 * @param {import('@playwright/test').Page} page
 * @param {ReadonlyArray<{planId?: string}>} [rows]
 * @param {Record<string, string>} [planNamesById]
 */
async function setupLogListRoute(page, rows = MOCK_LOG_LIST, planNamesById = {}) {
  await page.route("**/api/log?*", (route) => {
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
  await page.route("**/api/plan/*", (route) => {
    const url = new URL(route.request().url());
    const planId = url.pathname.replace("/api/plan/", "");
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        _id: planId,
        planName: planNamesById[planId] || `mock-plan-name-${planId}`,
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

  test("card headline is the single real link per card (R12)", async ({ page }) => {
    // Adrian Roselli block-link pattern: one real <a> per card lives on the
    // headline; the rest of the card is a ::after pseudo-element overlay so
    // the click target is the full silhouette. Critically, the card root is
    // an <article>, NOT an <a>, so the HTML is valid (no nested links).
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    const firstCard = page.locator('#logsListing [data-testid="log-list-item"]').first();
    await expect(firstCard).toBeVisible();

    // The card root has no href (it's an <article>).
    expect(await firstCard.getAttribute("href")).toBeNull();
    expect(await firstCard.evaluate((el) => el.tagName)).toBe("ARTICLE");

    // Exactly one anchor descendant carries the testid="log-list-link" — the
    // headline. The plan chip and config button live alongside it as
    // separate interactive controls (no nested <a> inside the headline).
    const headline = firstCard.locator('a[data-testid="log-list-link"]');
    await expect(headline).toHaveCount(1);
    const href = await headline.getAttribute("href");
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

  test("Plan chip text resolves to planName, not planId", async ({ page }) => {
    // /api/log only carries `planId`. The cts-log-card-plan-link must show
    // the human-meaningful kebab-case `planName` from /api/plan/<id> instead
    // of the opaque MongoDB id, while keeping the link target pointed at
    // plan-detail by planId.
    await setupFailFast(page);
    await setupLogListRoute(page, MOCK_LOG_LIST, {
      "plan-001": "oidcc-basic-certification-test-plan",
      "plan-002": "fapi2-security-profile-final-test-plan",
      "plan-003": "vci-id-1-wallet-test-plan",
    });
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    const card001 = page.locator(
      '#logsListing [data-testid="log-list-item"][data-test-id="test-log-001"]',
    );
    await expect(card001).toBeVisible();

    const planLink001 = card001.locator(".cts-log-card-plan-link");
    // Resolution is async — wait for the chip text to settle on the
    // resolved planName rather than the optimistic planId fallback.
    await expect(planLink001).toHaveText("oidcc-basic-certification-test-plan");

    // Same id (plan-001) shared with another row — second card also shows
    // the resolved name, proving the cache hits across rows.
    const card002 = page.locator(
      '#logsListing [data-testid="log-list-item"][data-test-id="test-log-002"]',
    );
    await expect(card002.locator(".cts-log-card-plan-link")).toHaveText(
      "oidcc-basic-certification-test-plan",
    );

    // Distinct planId resolves to its own planName.
    const card003 = page.locator(
      '#logsListing [data-testid="log-list-item"][data-test-id="test-log-003"]',
    );
    await expect(card003.locator(".cts-log-card-plan-link")).toHaveText(
      "fapi2-security-profile-final-test-plan",
    );

    // Link target still uses planId (the routable identifier) — only the
    // visible text changed.
    await expect(planLink001).toHaveAttribute("href", /plan=plan-001/);
  });

  test("Plan chip falls back to planId when /api/plan/<id> returns 404", async ({ page }) => {
    // When the plan lookup fails (deleted plan, permission denied), the
    // chip must stay readable rather than going blank. Falling back to the
    // raw planId preserves both a usable label and the link target.
    await setupFailFast(page);
    await page.route("**/api/log?*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          draw: 1,
          recordsTotal: MOCK_LOG_LIST.length,
          recordsFiltered: MOCK_LOG_LIST.length,
          data: MOCK_LOG_LIST,
        }),
      }),
    );
    await page.route("**/api/plan/*", (route) =>
      route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
    );
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    const card001 = page.locator(
      '#logsListing [data-testid="log-list-item"][data-test-id="test-log-001"]',
    );
    await expect(card001).toBeVisible();
    // Chip text equals the raw planId (no blank chip, no error surface).
    await expect(card001.locator(".cts-log-card-plan-link")).toHaveText("plan-001");
    // Link target unchanged regardless of resolution outcome.
    await expect(card001.locator(".cts-log-card-plan-link")).toHaveAttribute(
      "href",
      /plan=plan-001/,
    );
  });
});

test.describe("logs.html — Faceted filter dropdown and URL sync", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("?status=running,waiting boots with options pre-checked (R9)", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=running,waiting");

    // Open the filter dropdown; the two status options boot checked.
    await page.locator('#logsListing [data-testid="log-filter-trigger"]').click();
    const runningOption = page.locator('#logsListing input[data-status="RUNNING"]');
    const waitingOption = page.locator('#logsListing input[data-status="WAITING"]');
    await expect(runningOption).toBeChecked();
    await expect(waitingOption).toBeChecked();

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

  test("checking a status option writes ?status= to the URL", async ({ page }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    await expect(page.locator('#logsListing [data-testid="log-list-item"]').first()).toBeVisible();

    await page.locator('#logsListing [data-testid="log-filter-trigger"]').click();
    const runningOption = page.locator('#logsListing input[data-status="RUNNING"]');
    await runningOption.check();

    // Wait for URL to update via history.replaceState.
    await page.waitForFunction(() => window.location.search.includes("status=running"));
    expect(page.url()).toContain("status=running");

    // Toggle it off — URL param disappears.
    await runningOption.uncheck();
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

    // "bogus" was dropped — only RUNNING is active and 1 row matches.
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

// ---------------------------------------------------------------------------
// My/Published view tabs (U6). cts-view-tabs is a URL-driven nav control (My =
// absence of ?public=true, Published = ?public=true). logs.html resolves the
// auth-gated default after /api/currentuser and wires a single
// cts-view-tab-change handler that serves both tab clicks and back/forward
// popstate. On a view change the page calls cts-log-list.reloadForViewChange(),
// which resets the status/result chip filters (R16) and refetches the dataset
// for the new is-public view. URL-sync and back/forward tab restoration are
// asserted in logs-url-compat.spec.js GROUP B; this block covers the
// logs-specific behaviors (chip reset, dataset refetch, loading state,
// aria-live, anon).
// ---------------------------------------------------------------------------

const ITEM = '#logsListing [data-testid="log-list-item"]';
const SUMMARY = '#logsListing [data-testid="active-filter-summary"]';

test.describe("logs.html — My/Published view tabs (U6)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("R16: switching My→Published resets the chip filters and drops ?status/?result", async ({
    page,
  }) => {
    await setupFailFast(page);
    const logRequests = await recordLogRoute(page);
    await setupCommonRoutes(page);

    // Deep-link with status chips applied; authed default is the My view.
    await page.goto("/logs.html?status=running,waiting");
    await expect(page.locator(SUMMARY)).toBeVisible();
    await expect(page.locator(ITEM)).toHaveCount(2);

    // Switch to Published → chips reset (R16): the URL drops ?status/?result
    // but keeps ?public=true, the active-filter summary disappears, and the
    // full unfiltered dataset renders.
    await page.locator("cts-view-tabs a[data-view='published']").click();
    await page.waitForFunction(
      () =>
        window.location.search.includes("public=true") &&
        !window.location.search.includes("status="),
    );
    await expect(page.locator(SUMMARY)).toHaveCount(0);
    expect(page.url()).not.toContain("status=");
    expect(page.url()).not.toContain("result=");
    await expect(page.locator(ITEM)).toHaveCount(MOCK_LOG_LIST.length);
    // The refetch targeted the Published dataset.
    expect(logRequests.some((u) => u.includes("public=true"))).toBe(true);

    // Switching back to My starts unfiltered (chips do not reappear).
    await page.locator("cts-view-tabs a[data-view='my']").click();
    await page.waitForFunction(() => !window.location.search.includes("public="));
    await expect(page.locator(SUMMARY)).toHaveCount(0);
    await expect(page.locator(ITEM)).toHaveCount(MOCK_LOG_LIST.length);
  });

  test("R16/KTD4: a Back from Published does NOT restore the prior chip selection", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=running,waiting");
    await expect(page.locator(SUMMARY)).toBeVisible();
    await expect(page.locator(ITEM)).toHaveCount(2);

    await page.locator("cts-view-tabs a[data-view='published']").click();
    await page.waitForFunction(
      () =>
        window.location.search.includes("public=true") &&
        !window.location.search.includes("status="),
    );

    // Back to My: the popstate-driven view change re-runs reloadForViewChange(),
    // so the chips stay reset by design (the URL-compat gate restores the tab +
    // dataset, NOT the prior filter). This locks the intentional behavior so a
    // future change cannot silently add chip-restoration.
    await page.goBack();
    await expect(
      page.locator("cts-view-tabs a[data-view='my'][aria-current='page']"),
    ).toBeVisible();
    await expect(page.locator(SUMMARY)).toHaveCount(0);
    expect(page.url()).not.toContain("status=");
    await expect(page.locator(ITEM)).toHaveCount(MOCK_LOG_LIST.length);
  });

  test("R5/F3: tab click and back/forward refetch the matching dataset", async ({ page }) => {
    await setupFailFast(page);
    const logRequests = await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");
    await expect(page.locator(ITEM).first()).toBeVisible();
    // Initial My fetch carries no public flag. The runs strip also fires its own
    // unfiltered window (start=0) on the authed My view, so assert on the LIST's
    // fetch only — it is the one without start=0.
    const listRequests = logRequests.filter((u) => !u.includes("start=0"));
    expect(listRequests).toHaveLength(1);
    expect(listRequests[0]).not.toContain("public=true");

    await page.locator("cts-view-tabs a[data-view='published']").click();
    await page.waitForFunction(() => window.location.search.includes("public=true"));
    expect(logRequests.some((u) => u.includes("public=true"))).toBe(true);

    // Back → popstate re-emits cts-view-tab-change; the page refetches the My
    // dataset (a fresh request without public=true), not just the address bar.
    const beforeBack = logRequests.length;
    await page.goBack();
    await page.waitForFunction(() => !window.location.search.includes("public="));
    const afterBack = logRequests.slice(beforeBack);
    expect(afterBack.length).toBeGreaterThan(0);
    expect(afterBack.some((u) => !u.includes("public=true"))).toBe(true);
  });

  test("R17: the dataset swap shows the loading state, prior rows not left visible", async ({
    page,
  }) => {
    await setupFailFast(page);
    // First fetch resolves immediately; the second (the tab switch) is held on
    // an explicit gate — not a wall-clock sleep — so the in-place loading state
    // is observed deterministically rather than racing a fixed timeout on a
    // slow CI runner.
    let releaseSecondFetch = () => {};
    const secondFetchGate = /** @type {Promise<void>} */ (
      new Promise((resolve) => {
        releaseSecondFetch = () => resolve();
      })
    );
    let callCount = 0;
    await page.route("**/api/log?*", async (route) => {
      callCount += 1;
      if (callCount >= 2) {
        await secondFetchGate;
      }
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
    await page.route("**/api/plan/*", (route) => {
      const planId = new URL(route.request().url()).pathname.replace("/api/plan/", "");
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ _id: planId, planName: `mock-plan-name-${planId}` }),
      });
    });
    await setupCommonRoutes(page);

    await page.goto("/logs.html");
    await expect(page.locator(ITEM).first()).toBeVisible();

    await page.locator("cts-view-tabs a[data-view='published']").click();
    // Loading state replaces the prior rows while the second fetch is gated.
    // Assert via the stable data-testid, not the internal CSS class name.
    await expect(page.locator('#logsListing [data-testid="log-list-loading"]')).toBeVisible();
    await expect(page.locator(ITEM)).toHaveCount(0);
    // Release the held fetch; rows render once it resolves.
    releaseSecondFetch();
    await expect(page.locator(ITEM).first()).toBeVisible();
  });

  test("R19: the log list host carries aria-live=polite for swap announcements", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupLogListRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");
    await expect(page.locator(ITEM).first()).toBeVisible();
    await expect(page.locator("#logsListing")).toHaveAttribute("aria-live", "polite");
  });

  test("R6: anonymous hides the My tab, shows Published, and fetches public=true", async ({
    page,
  }) => {
    await setupFailFast(page);
    const logRequests = await recordLogRoute(page);
    await setupCommonRoutes(page, { user: null });

    await page.goto("/logs.html");
    await expect(page.locator(ITEM).first()).toBeVisible();

    await expect(page.locator("cts-view-tabs a[data-view='my']")).toHaveCount(0);
    await expect(
      page.locator("cts-view-tabs a[data-view='published'][aria-current='page']"),
    ).toBeVisible();

    // The anon fetch carried public=true — the My path is not silently emptied (R23).
    expect(logRequests.length).toBeGreaterThan(0);
    expect(logRequests.every((u) => u.includes("public=true"))).toBe(true);
  });
});

/**
 * Runs strip (relocated from plans.html — Fit & Finish batch).
 *
 * cts-run-status-strip is user-scoped run telemetry, so it lives on the logs
 * page where runs are the subject, shown on the authenticated My tab only. It
 * stays page-driven (KTD5): logs.html resolves auth once via
 * FAPI_UI.getUserInfo() and calls fetchRuns() on the authed My view, hide() for
 * anon / Published. It classifies its OWN unfiltered /api/log window
 * (?start=0&length=1000) via js/run-classification.js, distinct from the list's
 * ?length=1000 fetch, so the counts ignore the list's search/chip filtering (R10).
 *
 * On logs.html the list and the strip both hit /api/log; the strip's request is
 * the ONLY one carrying `start=0`, which the anon/Published/failure tests key on
 * to isolate strip behaviour from the list's own fetch.
 */
const STRIP = "#runStatusStrip";

// classifyRuns reads only status / result; cts-log-list keys cards by testId,
// so each run row carries a unique testId to render as a distinct card.
const RUNS_2_RUNNING_3_FAILING = [
  { testId: "r1", status: "RUNNING" },
  { testId: "r2", status: "WAITING" },
  { testId: "r3", result: "FAILED" },
  { testId: "r4", result: "UNKNOWN" },
  { testId: "r5", result: "FAILED" },
  { testId: "r6", status: "FINISHED", result: "PASSED" },
];
const RUNS_2_RUNNING_0_FAILING = [
  { testId: "r1", status: "RUNNING" },
  { testId: "r2", status: "WAITING" },
  { testId: "r3", status: "FINISHED", result: "PASSED" },
];
const RUNS_ALL_CLEAR = [
  { testId: "r1", status: "FINISHED", result: "PASSED" },
  { testId: "r2", status: "FINISHED", result: "WARNING" },
];

// The strip's window is the only /api/log request carrying start=0.
const isStripFetch = (/** @type {string} */ u) => u.includes("start=0");

test.describe("logs.html — runs strip (relocated from plans home)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("authed My with in-progress + failing → both counts link to the filtered logs", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordLogRoute(page, RUNS_2_RUNNING_3_FAILING);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    // The strip resolves to its actionable state, above the log list (counts and
    // the runs they summarise in one view).
    await expect(page.locator(`${STRIP} .runStrip--actionable`)).toBeVisible();
    await expect(page.locator(ITEM).first()).toBeVisible();

    // In-progress (RUNNING + WAITING = 2) → ?status=running,waiting.
    const inProgress = page.locator(`${STRIP} a[href="logs.html?status=running,waiting"]`);
    await expect(inProgress).toBeVisible();
    await expect(inProgress).toContainText("in progress");
    await expect(inProgress.locator("cts-badge")).toHaveAttribute("count", "2");

    // Failing (FAILED + UNKNOWN + FAILED = 3) → ?result=failed,unknown.
    const failing = page.locator(`${STRIP} a[href="logs.html?result=failed,unknown"]`);
    await expect(failing).toBeVisible();
    await expect(failing).toContainText("failing");
    await expect(failing.locator("cts-badge")).toHaveAttribute("count", "3");

    // The strip host is a polite live region.
    await expect(page.locator(STRIP)).toHaveAttribute("aria-live", "polite");
  });

  test("KTD6: clicking a count navigates to logs.html with the filter applied in place", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordLogRoute(page, RUNS_2_RUNNING_3_FAILING);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");
    const failing = page.locator(`${STRIP} a[href="logs.html?result=failed,unknown"]`);
    await expect(failing).toBeVisible();

    // KTD6: the count link is a plain href to logs.html?<filter>. On logs.html
    // that reloads the page and the existing URL-param filter sync applies in
    // place — the active-filter summary appears and only failing runs show.
    await Promise.all([page.waitForURL(/logs\.html\?result=failed,unknown/), failing.click()]);
    expect(page.url()).toContain("logs.html?result=failed,unknown");
    await expect(page.locator(SUMMARY)).toBeVisible();
    await expect(page.locator(ITEM)).toHaveCount(3);
  });

  test("in-progress only → one count link, no fabricated '0 failing' element", async ({ page }) => {
    await setupFailFast(page);
    await recordLogRoute(page, RUNS_2_RUNNING_0_FAILING);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    await expect(page.locator(`${STRIP} a[href="logs.html?status=running,waiting"]`)).toBeVisible();
    // The failing link is absent entirely (no "0 failing").
    await expect(page.locator(`${STRIP} a[href="logs.html?result=failed,unknown"]`)).toHaveCount(0);
  });

  test("has runs but none actionable → 'all caught up', not hidden", async ({ page }) => {
    await setupFailFast(page);
    await recordLogRoute(page, RUNS_ALL_CLEAR);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    const clear = page.locator(`${STRIP} .runStrip--clear`);
    await expect(clear).toBeVisible();
    await expect(clear).toContainText(/all caught up/i);
    // No count links and no fabricated counts in the all-clear state.
    await expect(page.locator(`${STRIP} a[href^="logs.html?"]`)).toHaveCount(0);
    await expect(page.locator(`${STRIP} cts-badge`)).toHaveCount(0);
  });

  test("a zero-runs account renders no strip (the log list still renders)", async ({ page }) => {
    await setupFailFast(page);
    await recordLogRoute(page, []);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    // The strip collapses to nothing; the log list still renders its empty state.
    await expect(page.locator(`${STRIP} .runStrip`)).toHaveCount(0);
    await expect(page.locator("#logsListing")).toBeVisible();
    await expect(page.locator(ITEM)).toHaveCount(0);
  });

  test("R9: anonymous → strip never rendered and its /api/log window is never requested", async ({
    page,
  }) => {
    await setupFailFast(page);
    const logRequests = await recordLogRoute(page);
    await setupCommonRoutes(page, { user: null });

    await page.goto("/logs.html");

    // The published list renders for the anonymous visitor...
    await expect(page.locator(ITEM).first()).toBeVisible();
    // ...but the personal-home strip is absent (R9)...
    await expect(page.locator(`${STRIP} .runStrip`)).toHaveCount(0);
    // ...and the strip's own window (start=0) was never fetched. The list's
    // public fetch (length=1000&public=true) is expected and carries no start=0.
    expect(logRequests.some(isStripFetch)).toBe(false);
  });

  test("R9: Published view (authed) hides the strip and never requests its /api/log window", async ({
    page,
  }) => {
    await setupFailFast(page);
    const logRequests = await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?public=true");

    // Authed but on the public browser: the personal-home strip stays hidden and
    // never fires its start=0 window fetch.
    await expect(page.locator(ITEM).first()).toBeVisible();
    await expect(page.locator(`${STRIP} .runStrip`)).toHaveCount(0);
    expect(logRequests.some(isStripFetch)).toBe(false);
  });

  test("R20: the strip's /api/log failure → degraded strip state, log list still functional", async ({
    page,
  }) => {
    await setupFailFast(page);
    // Fail ONLY the strip's window (start=0); the list's own fetch (length=1000)
    // still succeeds. logs.html shares /api/log between the two, so isolating the
    // failure to the strip's request keeps the list-still-functional claim honest.
    await page.route("**/api/log?*", (route) => {
      if (route.request().url().includes("start=0")) {
        return route.fulfill({ status: 500, body: "" });
      }
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
    await page.route("**/api/plan/*", (route) => {
      const planId = new URL(route.request().url()).pathname.replace("/api/plan/", "");
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ _id: planId, planName: `mock-plan-name-${planId}` }),
      });
    });
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    // Degraded "couldn't load" state — not hidden, not implying all-clear.
    const errorState = page.locator(`${STRIP} .runStrip--error`);
    await expect(errorState).toBeVisible();
    await expect(errorState).toContainText(/couldn't load run status/i);
    await expect(page.locator(`${STRIP} .runStrip--clear`)).toHaveCount(0);

    // The log list is unaffected by the strip's fetch failure.
    await expect(page.locator(ITEM).first()).toBeVisible();
    await expect(page.locator(ITEM)).toHaveCount(MOCK_LOG_LIST.length);
  });

  test("tab change: strip hides on Published and re-appears on return to My", async ({ page }) => {
    await setupFailFast(page);
    await recordLogRoute(page, RUNS_2_RUNNING_3_FAILING);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    // Authed My: the strip is actionable.
    await expect(page.locator(`${STRIP} .runStrip--actionable`)).toBeVisible();

    // Switch to Published: the personal-home strip collapses (hide()).
    await page.locator("cts-view-tabs a[data-view='published']").click();
    await page.waitForFunction(() => window.location.search.includes("public=true"));
    await expect(page.locator(`${STRIP} .runStrip`)).toHaveCount(0);

    // Switch back to My: the strip re-fetches and re-appears (fetchRuns()).
    await page.locator("cts-view-tabs a[data-view='my']").click();
    await page.waitForFunction(() => !window.location.search.includes("public="));
    await expect(page.locator(`${STRIP} .runStrip--actionable`)).toBeVisible();
  });
});

const LOGS_PUBLISHED_HELP = "#viewTabs [data-testid='published-help']";

test.describe("logs.html — Published help tooltip + terminology (U12)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("R21: document title + navbar use the 'Test Logs' vocabulary; the page heading and persistent descriptor are gone", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    await expect(page).toHaveTitle(/Test Logs/);
    await expect(page.locator("cts-navbar")).toContainText("Test Logs");
    await expect(page.locator(".listing-page-header")).toHaveCount(0);
    await expect(page.locator("#publishedDesc")).toHaveCount(0);
  });

  test("R22: a circled-help icon sits next to Published, carrying the descriptor as tooltip + accessible name", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    const help = page.locator(LOGS_PUBLISHED_HELP);
    await expect(help).toBeVisible();
    await expect(help).toHaveAttribute("name", "circle-help");
    await expect(help).toHaveAttribute("tabindex", "0");
    await expect(help).toHaveAttribute("aria-label", /Published test logs are conformance/);
    const tooltip = page.locator("#viewTabs a[data-view='published'] + cts-tooltip");
    await expect(tooltip).toHaveAttribute("content", /Published test logs are conformance/);
    await expect(tooltip.locator("[data-testid='published-help']")).toHaveCount(1);
  });

  test("R22: the help affordance is present for anonymous visitors (Published is their only view)", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page, { user: null });

    await page.goto("/logs.html");

    await expect(page.locator("#viewTabs a[data-view='my']")).toHaveCount(0);
    await expect(page.locator(LOGS_PUBLISHED_HELP)).toBeVisible();
  });

  test("R22: focusing the help icon reveals the descriptor tooltip", async ({ page }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    const help = page.locator(LOGS_PUBLISHED_HELP);
    await expect(help).toBeVisible();
    await expect(page.locator("body > .oidf-tooltip[role='tooltip']")).toHaveCount(0);
    await help.focus();
    const tip = page.locator("body > .oidf-tooltip[role='tooltip']");
    await expect(tip).toBeVisible();
    await expect(tip).toContainText("Published test logs are conformance");
  });
});
