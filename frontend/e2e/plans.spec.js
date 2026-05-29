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
 * Register an /api/log route serving the given run rows in the DataTables
 * envelope. The plans home now mounts cts-run-status-strip (U7), which fetches
 * /api/log on the authenticated My view. Tests that don't care about the strip
 * register an EMPTY window (the default) so the strip renders nothing and the
 * fail-fast catch-all is not tripped; strip-focused tests pass explicit rows.
 *
 * @param {import('@playwright/test').Page} page
 * @param {ReadonlyArray<{status?: string, result?: string}>} [rows]
 */
function mockLogRoute(page, rows = []) {
  return page.route("**/api/log*", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        draw: 1,
        recordsTotal: rows.length,
        recordsFiltered: rows.length,
        data: rows,
      }),
    }),
  );
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function mockPlanRoute(page) {
  await mockLogRoute(page);
  await page.route("**/api/plan*", (route) => {
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
    // My view → cts-run-status-strip (U7) fetches /api/log; serve an empty
    // window so the strip stays silent and fail-fast is not tripped.
    await mockLogRoute(page);

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
  // The plans home mounts cts-run-status-strip (U7) → /api/log on the authed My
  // view. Co-register an empty run window so the strip renders nothing and the
  // fail-fast catch-all stays green for tab/auth tests that don't assert it.
  await mockLogRoute(page);
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

/**
 * U7 — In-progress / failing runs strip on the plans home.
 *
 * cts-run-status-strip is the one dashboard signal kept after the launchpad is
 * retired: an always-on (authed) strip naming in-progress and failing run
 * counts, each linking into the matching filtered logs. It is page-driven
 * (KTD3): the page resolves auth once and calls fetchRuns() on the authed My
 * view, hide() for anon / Published. It classifies the most-recent /api/log
 * window via js/run-classification.js.
 */
const STRIP = "#runStatusStrip";

// Minimal run rows — classifyRuns reads only status / result.
const RUNS_2_RUNNING_3_FAILING = [
  { status: "RUNNING" },
  { status: "WAITING" },
  { result: "FAILED" },
  { result: "UNKNOWN" },
  { result: "FAILED" },
  { status: "FINISHED", result: "PASSED" },
];
const RUNS_2_RUNNING_0_FAILING = [
  { status: "RUNNING" },
  { status: "WAITING" },
  { status: "FINISHED", result: "PASSED" },
];
const RUNS_ALL_CLEAR = [
  { status: "FINISHED", result: "PASSED" },
  { status: "FINISHED", result: "WARNING" },
];

test.describe("plans.html — runs strip (U7)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("F1/R19: authed My with in-progress + failing → both counts link to the filtered logs", async ({
    page,
  }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    // Override the empty default with a window that has both kinds of runs.
    await mockLogRoute(page, RUNS_2_RUNNING_3_FAILING);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    // The strip resolves to its actionable state, alongside the plan cards
    // (F1: counts and the plan list in one view).
    await expect(page.locator(`${STRIP} .runStrip--actionable`)).toBeVisible();
    await expect(page.locator(CARD).first()).toBeVisible();

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

    // R19: the strip host is a polite live region.
    await expect(page.locator(STRIP)).toHaveAttribute("aria-live", "polite");
  });

  test("F1: clicking a count navigates to the matching filtered logs", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await mockLogRoute(page, RUNS_2_RUNNING_3_FAILING);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    // Stub the navigation target so the click can be verified standalone.
    await page.route("**/logs.html*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "text/html",
        body: "<!DOCTYPE html><html><body>logs stub</body></html>",
      }),
    );
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    const failing = page.locator(`${STRIP} a[href="logs.html?result=failed,unknown"]`);
    await expect(failing).toBeVisible();

    await Promise.all([page.waitForURL(/logs\.html\?result=failed,unknown/), failing.click()]);
    expect(page.url()).toContain("logs.html?result=failed,unknown");
  });

  test("AE2: in-progress only → one count link, no fabricated '0 failing' element", async ({
    page,
  }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await mockLogRoute(page, RUNS_2_RUNNING_0_FAILING);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    await expect(page.locator(`${STRIP} a[href="logs.html?status=running,waiting"]`)).toBeVisible();
    // AE2: the failing link is absent entirely (no "0 failing").
    await expect(page.locator(`${STRIP} a[href="logs.html?result=failed,unknown"]`)).toHaveCount(0);
  });

  test("AE1/R8: has runs but none actionable → 'all caught up', not hidden", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await mockLogRoute(page, RUNS_ALL_CLEAR);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    const clear = page.locator(`${STRIP} .runStrip--clear`);
    await expect(clear).toBeVisible();
    await expect(clear).toContainText(/all caught up/i);
    // No count links and no fabricated counts in the all-clear state.
    await expect(page.locator(`${STRIP} a[href^="logs.html?"]`)).toHaveCount(0);
    await expect(page.locator(`${STRIP} cts-badge`)).toHaveCount(0);
  });

  test("AE1b: a zero-runs account renders no strip (plan cards still render)", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page); // empty /api/log window by default
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    // Plan cards render, but the strip collapses to nothing.
    await expect(page.locator(CARD).first()).toBeVisible();
    await expect(page.locator(`${STRIP} .runStrip`)).toHaveCount(0);
  });

  test("R9: anonymous → strip never rendered and /api/log is never requested", async ({ page }) => {
    await setupFailFast(page);

    // Record any /api/log request so a wrongful anon fetch is detectable.
    /** @type {string[]} */
    const logRequests = [];
    await page.route("**/api/log*", (route) => {
      logRequests.push(route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ draw: 1, recordsTotal: 0, recordsFiltered: 0, data: [] }),
      });
    });
    // Anonymous lands on Published, so serve the published plan list.
    await page.route("**/api/plan*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_LIST.filter((p) => p.publish)),
      }),
    );
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page, { user: null });

    await page.goto("/plans.html");

    // Published cards render; the strip is absent (R9) and never fetched.
    await expect(page.locator(CARD).first()).toBeVisible();
    await expect(page.locator(`${STRIP} .runStrip`)).toHaveCount(0);
    expect(logRequests).toHaveLength(0);
  });

  test("R9: Published view (authed) hides the strip and never requests /api/log", async ({
    page,
  }) => {
    await setupFailFast(page);

    /** @type {string[]} */
    const logRequests = [];
    await page.route("**/api/log*", (route) => {
      logRequests.push(route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ draw: 1, recordsTotal: 0, recordsFiltered: 0, data: [] }),
      });
    });
    await page.route("**/api/plan*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_LIST.filter((p) => p.publish)),
      }),
    );
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html?public=true");

    // Authed but on the public browser: the personal-home strip stays hidden
    // and never fires its /api/log fetch.
    await expect(page.locator(CARD).first()).toBeVisible();
    await expect(page.locator(`${STRIP} .runStrip`)).toHaveCount(0);
    expect(logRequests).toHaveLength(0);
  });

  test("R20: /api/log failure → degraded state, plan list still functional", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    // Override /api/log with a 500 to drive the degraded state.
    await page.route("**/api/log*", (route) => route.fulfill({ status: 500, body: "" }));
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    // Degraded "couldn't load" state — not hidden, not implying all-clear.
    const errorState = page.locator(`${STRIP} .runStrip--error`);
    await expect(errorState).toBeVisible();
    await expect(errorState).toContainText(/couldn't load run status/i);
    await expect(page.locator(`${STRIP} .runStrip--clear`)).toHaveCount(0);

    // The plan list is unaffected by the strip's fetch failure.
    await expect(page.locator(CARD).first()).toBeVisible();
    await expect(page.locator(CARD)).toHaveCount(MOCK_PLAN_LIST.length);
  });

  test("tab change: strip hides on Published and re-appears on return to My", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await mockLogRoute(page, RUNS_2_RUNNING_3_FAILING);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

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

/**
 * U8 — Create-test CTA (in cts-view-tabs) + My-empty empty state on the plans home.
 *
 * The persistent "Create test" CTA renders at the END of the cts-view-tabs row
 * for authenticated users on the My view (R11), linking to schedule-test.html.
 * It is gated on the My view (cts-view-tabs derives `_activeView === "my"` from
 * auth + URL), so anonymous visitors and the Published view never show it (R6) —
 * consistent with the runs strip. When the authenticated My dataset is empty,
 * the empty state guides the user to create their first test (R18); the
 * Published-empty state shows orienting copy with no Create action.
 */
const CREATE_CTA = "#viewTabs [data-testid='create-test-cta']";
const PLAN_EMPTY = "#plansListing [data-testid='plan-list-empty']";

/**
 * Serve an empty /api/plan dataset (both My and Published) plus an empty run
 * window so the strip stays silent (AE1b) and the empty-state copy is exercised.
 * @param {import('@playwright/test').Page} page
 */
async function mockEmptyPlanRoute(page) {
  await mockLogRoute(page);
  await page.route("**/api/plan*", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    }),
  );
}

test.describe("plans.html — Create-test CTA + empty state (U8)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("R11: authed My view renders a Create-test CTA linking to schedule-test.html", async ({
    page,
  }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    const cta = page.locator(CREATE_CTA);
    await expect(cta).toBeVisible();
    const link = cta.locator("a");
    await expect(link).toHaveAttribute("href", "schedule-test.html");
    await expect(link).toContainText("Create test");

    // It sits at the END of the cts-view-tabs row (after the My/Published tabs).
    await expect(page.locator("#viewTabs nav.cts-view-tabs > :last-child")).toHaveAttribute(
      "data-testid",
      "create-test-cta",
    );
  });

  test("R11/R6: anonymous → Create-test CTA not rendered", async ({ page }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page, { user: null });

    await page.goto("/plans.html");

    // Anon lands on Published; the authed-only CTA never renders.
    await expect(page.locator(CARD).first()).toBeVisible();
    await expect(page.locator(CREATE_CTA)).toHaveCount(0);
  });

  test("R11: authed user on the Published view → CTA hidden (gated on authenticated && !isPublic)", async ({
    page,
  }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html?public=true");

    // Authed but on the public browser: the personal Create-test CTA stays
    // hidden, matching the runs strip (U7) and the Published-empty state's
    // lack of a Create action — they must not disagree on one screen.
    await expect(page.locator(CARD).first()).toBeVisible();
    await expect(page.locator(CREATE_CTA)).toHaveCount(0);
  });

  test("R11: switching My⇄Published hides and re-shows the CTA (gated on the My view)", async ({
    page,
  }) => {
    await setupFailFast(page);
    await mockPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    // My view: CTA shown.
    await expect(page.locator(CREATE_CTA)).toBeVisible();

    // Switch to Published: the CTA hides (consistent with the runs strip).
    await page.locator("cts-view-tabs a[data-view='published']").click();
    await page.waitForFunction(() => window.location.search.includes("public=true"));
    await expect(page.locator(CREATE_CTA)).toHaveCount(0);

    // Back to My: the CTA re-appears.
    await page.locator("cts-view-tabs a[data-view='my']").click();
    await page.waitForFunction(() => !window.location.search.includes("public="));
    await expect(page.locator(CREATE_CTA)).toBeVisible();
  });

  test("R18: My empty → empty state offers a Create-test action", async ({ page }) => {
    await setupFailFast(page);
    await mockEmptyPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    const empty = page.locator(PLAN_EMPTY);
    await expect(empty).toBeVisible();
    await expect(empty).toContainText("No test plans yet");
    await expect(empty.locator("a[href='schedule-test.html']")).toBeVisible();
  });

  test("R18: Published empty → published-empty copy without a Create action", async ({ page }) => {
    await setupFailFast(page);
    await mockEmptyPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page, { user: null });

    await page.goto("/plans.html");

    const empty = page.locator(PLAN_EMPTY);
    await expect(empty).toBeVisible();
    await expect(empty).toContainText("No published plans yet");
    // No Create action: neither inside the empty state nor as a toolbar CTA.
    await expect(empty.locator("a[href='schedule-test.html']")).toHaveCount(0);
    await expect(page.locator(CREATE_CTA)).toHaveCount(0);
  });
});
