import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLAN_LIST, MOCK_PLAN_INFO } from "./fixtures/mock-plans.js";

/**
 * U1 — URL-compatibility gate for plans.html (page-level layer).
 *
 * This spec locks the OBSERVABLE URL contract of the plans page BEFORE any
 * retirement / redirect / My-Published-tab change lands (plan
 * docs/plans/2026-05-28-004-feat-plans-page-as-home-plan.md, R25/AE7). It is
 * the migration gate: legacy assertions must pass against today's behavior;
 * target assertions are committed as `test.fixme` so CI stays green and each
 * flips active when its owning unit lands.
 *
 * SERVER-LEVEL ASSERTIONS ARE DEFERRED TO U10. The `/` and `/index.html` 302
 * redirects and the OTT token-generation redirect target are server-observable
 * (Spring routing / security filter chain) and cannot be exercised by this
 * mocked-API Playwright harness. They are asserted by U10's server-level test
 * (HomeRoutingTest.java), which is intentionally NOT created here — there is no
 * MockMvc/@SpringBootTest infrastructure in the repo yet, and the redirect
 * mechanism is maintainer-flagged (KTD4). The Group-B `/` page-level fixme
 * below is a placeholder only; the real contract is the server test.
 *
 * Route ordering: setupFailFast() FIRST (catch-all runs last); specific routes
 * after; all routes registered before page.goto() (the page fetches at
 * script-parse time).
 */

/**
 * Record every /api/plan request URL and serve the My / Published dataset by
 * the presence of ?public=true. Returns the recorded-URL array so a test can
 * assert WHICH dataset was fetched first (the no-flash contract).
 *
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<string[]>} requested /api/plan URLs, in order
 */
async function recordPlanRoute(page) {
  // plans.html mounts cts-run-status-strip (U7), which fetches /api/log on the
  // authed My view (including after a switch back to My). Serve an empty run
  // window so the strip renders nothing and the fail-fast catch-all stays green
  // for these URL-contract tests, which don't assert the strip.
  await page.route("**/api/log*", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ draw: 1, recordsTotal: 0, recordsFiltered: 0, data: [] }),
    }),
  );
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

const CARD = "#plansListing [data-testid='plan-list-item']";

// ---------------------------------------------------------------------------
// GROUP A — LEGACY (active now; MUST PASS against current behavior)
// ---------------------------------------------------------------------------
test.describe("plans.html URL compat — GROUP A: legacy (active)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("?public=true renders the Published view on first paint, no flash of the My dataset", async ({
    page,
  }) => {
    await setupFailFast(page);
    const planRequests = await recordPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html?public=true");

    const cards = page.locator(CARD);
    await expect(cards.first()).toBeVisible();

    // Only published plans surface — this IS the Published dataset.
    const publishedCount = MOCK_PLAN_LIST.filter((p) => p.publish).length;
    await expect(cards).toHaveCount(publishedCount);

    // No-flash contract: the VERY FIRST /api/plan request already carried
    // ?public=true. If the My dataset had been fetched first (a flash), the
    // first recorded request would lack the param. is-public is set
    // synchronously in plans.html before the component's connectedCallback
    // fetch fires, so there is exactly one request and it is the public one.
    expect(planRequests.length).toBeGreaterThan(0);
    expect(planRequests[0]).toContain("public=true");
    expect(planRequests.every((u) => u.includes("public=true"))).toBe(true);

    // Owner / config affordances stay hidden in the Published view.
    await expect(page.locator("#plansListing .plan-owner")).toHaveCount(0);
    await expect(page.locator("#plansListing .showConfigBtn")).toHaveCount(0);
  });
});

// ---------------------------------------------------------------------------
// GROUP B — TARGET (not implemented yet; test.fixme so CI stays GREEN).
// Each is owned by the unit that will flip it to active.
// ---------------------------------------------------------------------------
test.describe("plans.html URL compat — GROUP B: target (pending)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  // OWNER: U10 (backend / index.html retirement). Page-level placeholder only —
  // the real `/` and `/index.html` → 302 `/plans.html` redirect is
  // server-side and is asserted by U10's HomeRoutingTest.java, not here.
  test.fixme("/ and /index.html resolve to the plans home (owning unit U10)", async ({ page }) => {
    await setupFailFast(page);
    await recordPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/");
    await page.waitForURL("**/plans.html");
    expect(new URL(page.url()).pathname).toBe("/plans.html");

    await page.goto("/index.html");
    await page.waitForURL("**/plans.html");
    expect(new URL(page.url()).pathname).toBe("/plans.html");
  });

  // OWNER: U5 (My/Published tabs + URL sync on plans.html).
  test("clicking My <-> Published keeps the URL in sync (?public=true added/removed) (owning unit U5)", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");
    await expect(page.locator(CARD).first()).toBeVisible();

    // Switching to Published adds ?public=true.
    await page.locator("cts-view-tabs a[data-view='published']").click();
    await page.waitForFunction(() => window.location.search.includes("public=true"));
    expect(page.url()).toContain("public=true");

    // Switching back to My removes it.
    await page.locator("cts-view-tabs a[data-view='my']").click();
    await page.waitForFunction(() => !window.location.search.includes("public="));
    expect(page.url()).not.toContain("public=");
  });

  // OWNER: U5 (back/forward popstate restores tab + dataset on plans.html).
  test("back/forward restores the prior tab and dataset (owning unit U5)", async ({ page }) => {
    await setupFailFast(page);
    await recordPlanRoute(page);
    await setupTestInfoRoute(page, MOCK_PLAN_INFO);
    await setupCommonRoutes(page);

    await page.goto("/plans.html");
    await expect(page.locator(CARD).first()).toBeVisible();

    await page.locator("cts-view-tabs a[data-view='published']").click();
    await page.waitForFunction(() => window.location.search.includes("public=true"));

    await page.goBack();
    await page.waitForFunction(() => !window.location.search.includes("public="));
    await expect(
      page.locator("cts-view-tabs a[data-view='my'][aria-current='page']"),
    ).toBeVisible();

    await page.goForward();
    await page.waitForFunction(() => window.location.search.includes("public=true"));
    await expect(
      page.locator("cts-view-tabs a[data-view='published'][aria-current='page']"),
    ).toBeVisible();
  });
});
