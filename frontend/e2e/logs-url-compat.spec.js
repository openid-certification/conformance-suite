import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  expectNoUnmockedCalls,
  recordLogRoute,
} from "./helpers/routes.js";

/**
 * U1 — URL-compatibility gate for logs.html (page-level layer).
 *
 * Locks the OBSERVABLE URL contract of the logs page BEFORE any My/Published
 * tab change lands (plan docs/plans/2026-05-28-004-feat-plans-page-as-home-plan.md,
 * R25/AE7). Legacy assertions pass against today's behavior; target assertions
 * are `test.fixme` so CI stays green until the owning unit lands.
 *
 * SERVER-LEVEL ASSERTIONS ARE DEFERRED TO U10 (see plans-url-compat.spec.js).
 * No `/` / `/index.html` redirect or OTT assertion lives here — those are
 * server-observable and asserted by U10's HomeRoutingTest.java.
 *
 * Route ordering: setupFailFast() FIRST; specific routes after; all before
 * page.goto(). logs.html loads fapi.ui.js, which fires
 * api/ui/spec_links?public=true at parse time (covered by setupCommonRoutes).
 *
 * recordLogRoute (the /api/log URL recorder + /api/plan stub) is shared from
 * helpers/routes.js so it stays in sync with logs.spec.js.
 */

const ITEM = '#logsListing [data-testid="log-list-item"]';

// ---------------------------------------------------------------------------
// GROUP A — LEGACY (active now; MUST PASS against current behavior)
// ---------------------------------------------------------------------------
test.describe("logs.html URL compat — GROUP A: legacy (active)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("?public=true renders the Published logs view (fetches /api/log?public=true)", async ({
    page,
  }) => {
    await setupFailFast(page);
    const logRequests = await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?public=true");

    await expect(page.locator(ITEM).first()).toBeVisible();

    // The list fetched the Published dataset — every /api/log request carries
    // public=true. is-public is set before the component's connectedCallback
    // fetch fires (logs.html mounts the element imperatively after reading the
    // param), so there is no flash of the My dataset.
    expect(logRequests.length).toBeGreaterThan(0);
    expect(logRequests[0]).toContain("public=true");
    expect(logRequests.every((u) => u.includes("public=true"))).toBe(true);
  });

  test("?status=running,waiting lands with the status filter applied on initial load", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?status=running,waiting");

    // Filter applied on first paint: only RUNNING + WAITING rows render.
    await expect(page.locator(ITEM)).toHaveCount(2);
    await expect(page.locator("#logsListing")).toContainText("fapi2-running");
    await expect(page.locator("#logsListing")).toContainText("fapi2-waiting");
    await expect(page.locator("#logsListing")).not.toContainText("oidcc-server-rotate-keys");

    // Active-filter summary confirms the chips booted applied (not via a
    // post-load interaction).
    const summary = page.locator('#logsListing [data-testid="active-filter-summary"]');
    await expect(summary).toBeVisible();
    await expect(summary).toContainText("Status: running or waiting");
  });

  test("?result=failed,unknown lands with the result filter applied on initial load", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html?result=failed,unknown");

    // 3 rows match on first paint: 2 UNKNOWN (running, waiting) + 1 FAILED.
    await expect(page.locator(ITEM)).toHaveCount(3);

    const summary = page.locator('#logsListing [data-testid="active-filter-summary"]');
    await expect(summary).toBeVisible();
    await expect(summary).toContainText("Result: failed or unknown");
  });
});

// ---------------------------------------------------------------------------
// GROUP B — TARGET (now ACTIVE; flipped from test.fixme when U6 landed the
// My/Published tabs on logs.html). Both assertions are owned by U6.
// ---------------------------------------------------------------------------
test.describe("logs.html URL compat — GROUP B: target (active)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  // OWNER: U6 (My/Published tabs + URL sync on logs.html). ACTIVE as of U6.
  test("clicking My <-> Published keeps the URL in sync (?public=true added/removed) (owning unit U6)", async ({
    page,
  }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");
    await expect(page.locator(ITEM).first()).toBeVisible();

    await page.locator("cts-view-tabs a[data-view='published']").click();
    await page.waitForFunction(() => window.location.search.includes("public=true"));
    expect(page.url()).toContain("public=true");

    await page.locator("cts-view-tabs a[data-view='my']").click();
    await page.waitForFunction(() => !window.location.search.includes("public="));
    expect(page.url()).not.toContain("public=");
  });

  // OWNER: U6 (back/forward popstate restores tab + dataset on logs.html). ACTIVE as of U6.
  test("back/forward restores the prior tab and dataset (owning unit U6)", async ({ page }) => {
    await setupFailFast(page);
    await recordLogRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/logs.html");
    await expect(page.locator(ITEM).first()).toBeVisible();

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
