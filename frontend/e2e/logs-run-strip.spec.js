import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  expectNoUnmockedCalls,
  recordLogRoute,
} from "./helpers/routes.js";
import { MOCK_LOG_LIST } from "./fixtures/mock-log-list.js";

/**
 * Runs strip on logs.html (relocated from plans.html — Fit & Finish batch).
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
 *
 * Split out of logs.spec.js so neither file crosses the ~1000-line maintainability
 * threshold (mirrors the plans.spec.js split pattern).
 */
const ITEM = '#logsListing [data-testid="log-list-item"]';
const SUMMARY = '#logsListing [data-testid="active-filter-summary"]';
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

// The strip's window is the only /api/log request carrying start=0. Match the
// full window (start=0&length=1000), not a bare "start=0" substring, so the
// isolation stays correct even if cts-log-list ever adds a start= param to its
// own fetch — today the list emits only ?length=1000[&public=true].
const isStripFetch = (/** @type {string} */ u) => u.includes("start=0&length=1000");

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
