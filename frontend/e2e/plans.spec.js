import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLAN_LIST, MOCK_PLAN_INFO } from "./fixtures/mock-plans.js";
import { MOCK_ADMIN_USER } from "./fixtures/mock-users.js";

/**
 * plans.html mounts a single <cts-plan-list>. The component fetches /api/plan
 * (or /api/plan?public=true) and renders a single-column card layout mirroring
 * cts-log-list: a search + sort toolbar, block-link cards (plan name headline,
 * plan id slug, description, module status grid, metadata row), Show-more
 * pagination, and a config-viewer modal.
 *
 * Module status is NOT in the /api/plan payload (Plan.Module carries only
 * testModule + instances). Each module's latest result is fetched from
 * /api/info/<lastInstance> and drives a color-coded status box — so these
 * tests mock /api/info via setupTestInfoRoute(MOCK_PLAN_INFO).
 *
 * The host keeps id="plansListing"; cts-plan-list is Light DOM so descendant
 * queries resolve through to the cards. Plan-name clicks emit
 * `cts-plan-navigate`, routed by the page to `plan-detail.html?plan=<id>`.
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

const CARD = "#plansListing [data-testid='plan-list-item']";

test.describe("plans.html — Plans List", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads and renders plans as cards", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    // Cross-page contract: every wired page mounts a single <cts-toast-host>
    // for window.ctsToast(...). (Mirrors upload.spec.js.)
    await expect(page.locator("cts-toast-host")).toHaveCount(1);

    // One card per plan.
    const cards = page.locator(CARD);
    await expect(cards.first()).toBeVisible();
    await expect(cards).toHaveCount(MOCK_PLAN_LIST.length);

    // The search + sort toolbar mirrors the logs listing.
    await expect(page.locator("#plansListing .cts-plan-list-search input")).toBeVisible();
    await expect(page.locator("#plansListing .cts-plan-list-sort select")).toBeVisible();

    // Plan name renders as a link; variant pairs render in the meta row.
    await expect(page.locator("#plansListing")).toContainText(
      "oidcc-basic-certification-test-plan",
    );
    await expect(page.locator("#plansListing")).toContainText("client_secret_basic");

    // Started renders through cts-time (a native <time>), not escaped markup.
    const planCard = page.locator(`${CARD}[data-plan-id='plan-001']`);
    await expect(planCard.locator("cts-time time")).toBeVisible();

    // Non-admin users do not see the owner pill.
    await expect(page.locator("#plansListing .plan-owner")).toHaveCount(0);

    // Config button renders only for rows with a non-empty config object;
    // plan-003 (config {}) gets none. Tie to the fixture to stay honest.
    const expectedConfigBtns = MOCK_PLAN_LIST.filter(
      (p) => p.config && Object.keys(p.config).length > 0,
    ).length;
    await expect(page.locator("#plansListing .showConfigBtn")).toHaveCount(expectedConfigBtns);
    await expect(page.locator("#plansListing .showConfigBtn").first()).toContainText(
      "View configuration",
    );

    // Module status boxes resolve from /api/info: a run module recolors to its
    // status, a never-run module stays a static skip box. Each box is keyed by
    // its module id via the wrapping tooltip's content.
    await expect(
      page.locator("#plansListing cts-tooltip[content='oidcc-server'] .moduleStatusBox"),
    ).toHaveClass(/moduleStatusBox--pass/);
    await expect(
      page.locator(
        "#plansListing cts-tooltip[content='oidcc-server-rotate-keys'] .moduleStatusBox",
      ),
    ).toHaveClass(/moduleStatusBox--warn/);
    await expect(
      page.locator("#plansListing cts-tooltip[content='oidcc-codereuse'] .moduleStatusBox"),
    ).toHaveClass(/moduleStatusBox--skip/);
  });

  test("admin users see owner pills", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plans.html");

    await expect(page.locator(CARD).first()).toBeVisible();

    // One owner pill per card, exposing the subject via its accessible label.
    const ownerPills = page.locator("#plansListing .plan-owner");
    await expect(ownerPills.first()).toBeVisible();
    await expect(ownerPills).toHaveCount(MOCK_PLAN_LIST.length);
    await expect(page.locator(`${CARD}[data-plan-id='plan-001'] .ownerSub`)).toHaveAttribute(
      "aria-label",
      "Subject: 12345",
    );
  });

  test("?public=true requests the published listing and hides admin affordances", async ({
    page,
  }) => {
    await setupFailFast(page);

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
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html?public=true");

    const cards = page.locator(CARD);
    await expect(cards.first()).toBeVisible();

    // Only published plans surface in the public view.
    const publishedCount = MOCK_PLAN_LIST.filter((p) => p.publish).length;
    await expect(cards).toHaveCount(publishedCount);

    // The component fetched with ?public=true on the URL.
    expect(planRequests.some((url) => url.includes("public=true"))).toBe(true);

    // Owner pill and config button stay hidden in the public view: PublicPlan
    // omits config server-side, and owner.sub must not leak publicly.
    await expect(page.locator("#plansListing .plan-owner")).toHaveCount(0);
    await expect(page.locator("#plansListing .showConfigBtn")).toHaveCount(0);
  });

  test("admin viewing ?public=true still hides owner and config", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plans.html?public=true");

    await expect(page.locator(CARD).first()).toBeVisible();

    // Legacy parity: the public listing never reveals owner or config
    // affordances, even to an admin.
    await expect(page.locator("#plansListing .plan-owner")).toHaveCount(0);
    await expect(page.locator("#plansListing .showConfigBtn")).toHaveCount(0);
  });

  test("clicking a plan name navigates to plan-detail.html", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);

    // Stub plan-detail.html so the navigation target can be verified without
    // the real detail page loading.
    await page.route("**/plan-detail.html*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "text/html",
        body: "<!DOCTYPE html><html><body>plan-detail stub</body></html>",
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    // Target plan-001 by id: the default sort is Started (newest), so the
    // first card in the DOM is not necessarily the first fixture entry.
    const planLink = page.locator(`${CARD}[data-plan-id='plan-001'] .plan-name-link`);
    await expect(planLink).toBeVisible();

    // Real destination href (not "#") so cmd-click / middle-click / "Open in
    // new tab" / screen-reader destination all work.
    await expect(planLink).toHaveAttribute("href", "plan-detail.html?plan=plan-001");

    await Promise.all([page.waitForURL(/plan-detail\.html\?plan=plan-001/), planLink.click()]);

    expect(page.url()).toContain("plan-detail.html?plan=plan-001");
  });

  test("config button opens modal and exposes copy affordance", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    await expect(page.locator(`${CARD}[data-plan-id='plan-001']`)).toBeVisible();

    // Click plan-001's config button (the inner <button> inside the cts-button
    // host — cts-button binds @click on the inner button).
    const configBtn = page.locator("#plansListing .showConfigBtn[data-plan-id='plan-001'] button");
    await expect(configBtn).toBeVisible();
    await configBtn.click();

    // The component owns its own modal — id="planConfigModal".
    const modal = page.locator("#planConfigModal");
    await expect(modal).toBeVisible();

    // Plan ID surfaces in the modal toolbar.
    await expect(modal.locator(".cts-plan-list-config-toolbar code")).toHaveText("plan-001");

    // The JSON editor renders inside the modal, populated with the row config.
    const editor = page.locator("cts-json-editor.config-json");
    await expect(editor).toBeAttached();
    const editorValue = await editor.evaluate((el) => /** @type {any} */ (el).value);
    expect(editorValue).toContain("server.issuer");
    expect(editorValue).toContain("op.example.com");

    // Copy button is present (clipboard writes are covered by the story).
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
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    const cards = page.locator(CARD);
    await expect(cards).toHaveCount(MOCK_PLAN_LIST.length);

    const fetchesBeforeSearch = planRequests.length;

    const searchInput = page.locator('#plansListing input[placeholder="Search test plans..."]');
    await expect(searchInput).toBeVisible();
    await searchInput.fill("fapi2");

    // Live filter narrows the rendered cards to the match.
    await expect(cards).toHaveCount(1);
    await expect(page.locator("#plansListing")).toContainText(
      "fapi2-security-profile-final-test-plan",
    );

    // No additional /api/plan fetch should fire — search is local.
    expect(planRequests).toHaveLength(fetchesBeforeSearch);
  });
});

/**
 * U5 — My/Published view tabs + auth-gated first paint (KTD3).
 *
 * cts-view-tabs is a URL-driven nav control (My = absence of ?public=true,
 * Published = ?public=true). The page gates the initial /api/plan fetch on
 * auth resolution: ?public=true fetches Published immediately at connect (no
 * gate), while the no-param path defers the fetch until /api/currentuser
 * resolves the auth-dependent default (My for authed, Published for anon). The
 * page wires a single cts-view-tab-change handler that serves both tab clicks
 * and back/forward popstate.
 */
const PUBLISHED_COUNT = MOCK_PLAN_LIST.filter((p) => p.publish).length;

/**
 * Record every /api/plan request URL and serve My / Published by the presence
 * of ?public=true. Returns the recorded-URL array so a test can assert WHICH
 * dataset was fetched, in what order, and how many times.
 *
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<string[]>} requested /api/plan URLs, in order
 */
async function recordPlanRoute(page) {
  /** @type {string[]} */
  const planRequests = [];
  await page.route("**/api/plan*", (route) => {
    const url = route.request().url();
    planRequests.push(url);
    const isPublic = new URL(url).searchParams.get("public") === "true";
    const body = isPublic ? MOCK_PLAN_LIST.filter((p) => p.publish) : MOCK_PLAN_LIST;
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(body),
    });
  });
  return planRequests;
}

test.describe("plans.html — My/Published view tabs (U5)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("AE5: authed, no param → My active first paint, exactly one /api/plan and never the Published dataset before auth resolves", async ({
    page,
  }) => {
    await setupFailFast(page);
    const planRequests = await recordPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    // My dataset (all plans) renders; My tab is the active one.
    const cards = page.locator(CARD);
    await expect(cards.first()).toBeVisible();
    await expect(cards).toHaveCount(MOCK_PLAN_LIST.length);
    await expect(
      page.locator("cts-view-tabs a[data-view='my'][aria-current='page']"),
    ).toBeVisible();

    // No Published fetch ever fired (no flash / premature fetch), and the
    // no-param authed path issues exactly ONE /api/plan total (no eager +
    // post-auth double fetch).
    expect(planRequests.every((u) => !u.includes("public=true"))).toBe(true);
    expect(planRequests).toHaveLength(1);
  });

  test("AE4: ?public=true → Published active first paint, first /api/plan carries public=true, no My flash", async ({
    page,
  }) => {
    await setupFailFast(page);
    const planRequests = await recordPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html?public=true");

    const cards = page.locator(CARD);
    await expect(cards.first()).toBeVisible();
    await expect(cards).toHaveCount(PUBLISHED_COUNT);

    // Published tab active; the VERY FIRST /api/plan request carried
    // public=true (no My-dataset flash before an auth-gated fetch).
    await expect(
      page.locator("cts-view-tabs a[data-view='published'][aria-current='page']"),
    ).toBeVisible();
    expect(planRequests.length).toBeGreaterThan(0);
    expect(planRequests[0]).toContain("public=true");
    expect(planRequests.every((u) => u.includes("public=true"))).toBe(true);
  });

  test("AE6/R17: clicking the other tab shows the loading state with the target tab active", async ({
    page,
  }) => {
    await setupFailFast(page);
    const planRequests = await recordPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");
    await expect(page.locator(CARD).first()).toBeVisible();

    // Switching to Published re-enters the loading state (the list flips
    // _loading true synchronously when fetchPlans runs) with Published marked
    // active. The fetch then resolves to the Published dataset.
    await page.locator("cts-view-tabs a[data-view='published']").click();
    await expect(
      page.locator("cts-view-tabs a[data-view='published'][aria-current='page']"),
    ).toBeVisible();

    // Published dataset rendered; the URL carries the param.
    await expect(page.locator(CARD)).toHaveCount(PUBLISHED_COUNT);
    expect(page.url()).toContain("public=true");

    // Both datasets were fetched (My on load, Published on switch).
    expect(planRequests.some((u) => !u.includes("public=true"))).toBe(true);
    expect(planRequests.some((u) => u.includes("public=true"))).toBe(true);
  });

  test("R6/R23: anonymous → My tab not rendered, Published shown and active", async ({ page }) => {
    await setupFailFast(page);
    const planRequests = await recordPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page, { user: null });

    await page.goto("/plans.html");

    // Anon lands on Published (the auth-gated default), and the My anchor is
    // never rendered.
    const cards = page.locator(CARD);
    await expect(cards.first()).toBeVisible();
    await expect(cards).toHaveCount(PUBLISHED_COUNT);
    await expect(page.locator("cts-view-tabs a[data-view='my']")).toHaveCount(0);
    await expect(
      page.locator("cts-view-tabs a[data-view='published'][aria-current='page']"),
    ).toBeVisible();

    // The anon fetch carried public=true (My path is not silently emptied).
    expect(planRequests.length).toBeGreaterThan(0);
    expect(planRequests.every((u) => u.includes("public=true"))).toBe(true);
  });

  test("R5: back/forward popstate updates the active tab AND loads the matching dataset", async ({
    page,
  }) => {
    await setupFailFast(page);
    const planRequests = await recordPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");
    await expect(page.locator(CARD).first()).toBeVisible();
    await expect(page.locator(CARD)).toHaveCount(MOCK_PLAN_LIST.length);

    await page.locator("cts-view-tabs a[data-view='published']").click();
    await page.waitForFunction(() => window.location.search.includes("public=true"));
    await expect(page.locator(CARD)).toHaveCount(PUBLISHED_COUNT);

    const requestsBeforeBack = planRequests.length;

    // Back to My: the popstate listener re-derives the tab, re-emits, and the
    // page re-fetches the My dataset (not just the URL).
    await page.goBack();
    await page.waitForFunction(() => !window.location.search.includes("public="));
    await expect(
      page.locator("cts-view-tabs a[data-view='my'][aria-current='page']"),
    ).toBeVisible();
    await expect(page.locator(CARD)).toHaveCount(MOCK_PLAN_LIST.length);

    // A fresh /api/plan (the My dataset) fired on the back navigation.
    const requestsAfterBack = planRequests.slice(requestsBeforeBack);
    expect(requestsAfterBack.length).toBeGreaterThan(0);
    expect(requestsAfterBack.some((u) => !u.includes("public=true"))).toBe(true);
  });

  test("R2: search and sort still function after the tab wiring", async ({ page }) => {
    await setupFailFast(page);
    await recordPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");
    await expect(page.locator(CARD)).toHaveCount(MOCK_PLAN_LIST.length);

    // Search narrows the rendered cards (client-side).
    const searchInput = page.locator('#plansListing input[placeholder="Search test plans..."]');
    await searchInput.fill("fapi2");
    await expect(page.locator(CARD)).toHaveCount(1);

    // Sort selector remains operable.
    const sortSelect = page.locator("#plansListing .cts-plan-list-sort select");
    await expect(sortSelect).toBeVisible();
    await sortSelect.selectOption("name-asc");
    await searchInput.fill("");
    await expect(page.locator(CARD)).toHaveCount(MOCK_PLAN_LIST.length);
  });
});
