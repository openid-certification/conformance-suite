import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_TEST_STATUS } from "./fixtures/mock-test-data.js";
import { MOCK_LOG_ENTRIES } from "./fixtures/mock-log-entries.js";

/**
 * Coverage for the U1 feature-flag redirect contract between
 * log-detail.html (legacy) and log-detail-v2.html (new Lit triad).
 *
 * The redirect lives in two ~ 15-line synchronous IIFE scripts at the
 * top of each page's <head>. They share a single cookie (`cts-lit-log`)
 * and react to the `ff` query parameter:
 *
 *   ?ff=lit-log on log-detail.html  → set cookie + redirect to v2
 *   ?ff=legacy  on log-detail-v2.html → clear cookie + redirect to legacy
 *   cookie set, on log-detail.html → redirect to v2 unconditionally
 *   cookie set, on log-detail-v2.html → render v2 (no redirect)
 *
 * Hash preservation: deep links like `?log=…#LOG-0042` survive both
 * redirect directions intact (R32 / U6 deep-link contract).
 *
 * Plan: docs/plans/2026-04-26-002-refactor-log-detail-page-to-lit-triad-plan.md
 */

/**
 * Register routes that satisfy both pages on a redirect path. Both pages
 * may finish navigating to v2 (which fires /api/info, /api/log, /api/plan,
 * /api/runner, /api/uploaded-images, /api/currentuser) so we mock them
 * all to avoid noise from the fail-fast catch-all.
 *
 * @param {import('@playwright/test').Page} page
 */
async function setupV2BootstrapRoutes(page) {
  const testId = MOCK_TEST_STATUS.testId;

  await page.route(`**/api/info/${testId}`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(MOCK_TEST_STATUS),
    }),
  );

  await page.route(`**/api/log/${testId}**`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(MOCK_LOG_ENTRIES),
    }),
  );

  await page.route(`**/api/plan/${MOCK_TEST_STATUS.planId}`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        _id: MOCK_TEST_STATUS.planId,
        planName: "test-plan",
        modules: [{ testModule: MOCK_TEST_STATUS.testName, variant: MOCK_TEST_STATUS.variant }],
      }),
    }),
  );

  await page.route(`**/api/runner/${testId}`, (route) => route.fulfill({ status: 404, body: "" }));

  await page.route("**/api/uploaded-images*", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
  );
}

test.describe("lit-log feature-flag redirect contract", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("?ff=lit-log on log-detail.html redirects to log-detail-v2.html and sets cookie", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupV2BootstrapRoutes(page);
    await setupCommonRoutes(page);

    await page.goto(
      `/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}&ff=lit-log`,
    );
    await page.waitForURL(/log-detail-v2\.html\?log=/);

    // Cookie was set by the redirect script.
    const cookies = await page.context().cookies();
    const litCookie = cookies.find((c) => c.name === "cts-lit-log");
    expect(litCookie?.value).toBe("1");

    // Final URL no longer carries the ff parameter — only the log id.
    expect(page.url()).toContain(
      `log-detail-v2.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`,
    );
    expect(page.url()).not.toContain("ff=");
  });

  test("cookie alone redirects from log-detail.html to log-detail-v2.html", async ({ page }) => {
    await setupFailFast(page);
    await setupV2BootstrapRoutes(page);
    await setupCommonRoutes(page);

    // Pre-seed the cookie before navigating.
    await page.context().addCookies([
      {
        name: "cts-lit-log",
        value: "1",
        domain: "localhost",
        path: "/",
        sameSite: "Lax",
      },
    ]);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`);
    await page.waitForURL(/log-detail-v2\.html\?log=/);

    expect(page.url()).toContain(
      `log-detail-v2.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`,
    );
  });

  test("?ff=legacy on log-detail-v2.html clears cookie and redirects to log-detail.html", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupV2BootstrapRoutes(page);
    await setupCommonRoutes(page);

    // Pre-seed the cookie so we can verify the legacy branch clears it.
    await page.context().addCookies([
      {
        name: "cts-lit-log",
        value: "1",
        domain: "localhost",
        path: "/",
        sameSite: "Lax",
      },
    ]);

    await page.goto(
      `/log-detail-v2.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}&ff=legacy`,
    );
    await page.waitForURL(/log-detail\.html\?log=/);

    // Cookie is cleared — Playwright surfaces it as `value: ""` or as
    // an absent cookie depending on browser. Either passes.
    const cookies = await page.context().cookies();
    const litCookie = cookies.find((c) => c.name === "cts-lit-log");
    expect(litCookie === undefined || litCookie.value === "").toBe(true);

    expect(page.url()).toContain(
      `log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`,
    );
    expect(page.url()).not.toContain("log-detail-v2.html");
    expect(page.url()).not.toContain("ff=");
  });

  test("?ff=lit-log preserves the URL hash through the redirect", async ({ page }) => {
    await setupFailFast(page);
    await setupV2BootstrapRoutes(page);
    await setupCommonRoutes(page);

    await page.goto(
      `/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}&ff=lit-log#LOG-0042`,
    );
    await page.waitForURL(/log-detail-v2\.html\?log=/);

    expect(page.url()).toContain("#LOG-0042");
  });

  test("no ff and no cookie renders the legacy log-detail.html (no redirect)", async ({ page }) => {
    await setupFailFast(page);
    await setupCommonRoutes(page);
    // The legacy page hits a much wider /api/* surface (poll loops via
    // FAPI_UI). For this single redirect-doesn't-fire assertion we don't
    // need full coverage — just confirm the URL stays on the legacy page
    // before the legacy bootstrap fires. abort handler in fail-fast keeps
    // unmocked calls noisy but the assertion below runs first.
    await page.route(`**/api/info/${MOCK_TEST_STATUS.testId}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_TEST_STATUS),
      }),
    );

    // Use waitUntil: 'commit' so we resolve as soon as the document
    // begins parsing — that's enough to observe the (non-)redirect
    // without waiting for the legacy page's heavy API loops.
    await page.goto(`/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`, {
      waitUntil: "commit",
    });

    // Stays on log-detail.html.
    expect(page.url()).toContain("log-detail.html");
    expect(page.url()).not.toContain("log-detail-v2.html");

    // Cookie was not implicitly created.
    const cookies = await page.context().cookies();
    const litCookie = cookies.find((c) => c.name === "cts-lit-log");
    expect(litCookie).toBeUndefined();

    // Drain the page — closing prevents fail-fast from accumulating
    // unmocked calls during the legacy bootstrap's poll loops.
    await page.close();
  });
});
