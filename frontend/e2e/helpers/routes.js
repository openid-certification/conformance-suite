/**
 * Playwright page.route() helpers for E2E tests.
 *
 * All routes MUST be registered before page.goto() because fapi.ui.js fires
 * fetch('api/ui/spec_links?public=true') at script parse time (loadSpecLinksMapping IIFE).
 *
 * Playwright matches routes in REVERSE registration order (last registered = first tried).
 * Register setupFailFast() FIRST, then specific routes, so the catch-all runs last.
 */

import { MOCK_USER } from "../fixtures/mock-users.js";
import { MOCK_SERVER_INFO } from "../fixtures/mock-server.js";
import { MOCK_TEST_STATUS } from "../fixtures/mock-test-data.js";

/**
 * Register the three routes every page needs:
 * - /api/currentuser
 * - /api/server
 * - api/ui/spec_links (note: no leading slash — the IIFE uses a relative URL)
 *
 * Also stubs Google Fonts so the JetBrains Mono <link> on
 * log-detail/schedule-test/tokens/upload never stalls page.goto() on a real
 * CDN fetch — the resulting load-event delay was the source of intermittent
 * modal/visibility flakes after the OIDF design tokens landed.
 *
 * @param {import('@playwright/test').Page} page
 * @param {object} [options]
 * @param {object|null} [options.user] - User data, or null for 401
 */
export async function setupCommonRoutes(page, options = {}) {
  const user = options.user !== undefined ? options.user : MOCK_USER;

  await page.route("**fonts.googleapis.com/**", (route) =>
    route.fulfill({ status: 200, contentType: "text/css", body: "" }),
  );
  await page.route("**fonts.gstatic.com/**", (route) =>
    route.fulfill({ status: 200, contentType: "font/woff2", body: "" }),
  );

  await page.route("**/api/currentuser", (route) => {
    if (user === null) {
      return route.fulfill({ status: 401, body: "" });
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(user),
    });
  });

  await page.route("**/api/server", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(MOCK_SERVER_INFO),
    }),
  );

  await page.route("**/api/ui/spec_links*", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        "OIDCC-": "https://openid.net/specs/openid-connect-core-1_0.html#section-",
      }),
    }),
  );
}

/**
 * Register a wildcard /api/info/:testId route that returns MOCK_TEST_STATUS
 * with the testId from the URL. To provide test-specific data, pass entries
 * in testStatusMap — do NOT register separate /api/info/:id routes before
 * calling this function, because this wildcard would be registered after them
 * and Playwright checks last-registered routes first, causing it to shadow
 * more specific routes.
 */
export async function setupTestInfoRoute(page, testStatusMap = {}) {
  await page.route("**/api/info/**", (route) => {
    const url = new URL(route.request().url());
    const testId = url.pathname.split("/api/info/")[1];
    const data = testStatusMap[testId] || { ...MOCK_TEST_STATUS, _id: testId, testId };
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(data),
    });
  });
}

/**
 * Wrap a plain data array in the DataTables server-side response envelope.
 * Reads draw/start/length from the request URL query params.
 */
export function wrapDataTablesResponse(data, requestUrl) {
  const url = new URL(requestUrl);
  const draw = parseInt(url.searchParams.get("draw") || "1", 10);
  const start = parseInt(url.searchParams.get("start") || "0", 10);
  const length = parseInt(url.searchParams.get("length") || "10", 10);
  const page = data.slice(start, start + length);

  return {
    draw,
    recordsTotal: data.length,
    recordsFiltered: data.length,
    data: page,
  };
}

/**
 * Register a fail-fast catch-all for any unmocked /api/* route.
 * MUST be called FIRST — Playwright matches routes in reverse registration
 * order, so the first-registered route runs last (as a true fallback).
 *
 * The handler aborts the request AND records the URL. After the test,
 * call {@link expectNoUnmockedCalls} to fail the test if any unmocked
 * API calls were made — route.abort() alone doesn't reliably fail tests
 * because pages may catch fetch errors and render an error modal.
 */
export async function setupFailFast(page) {
  /** @type {string[]} */
  page.__unmockedApiCalls = [];

  await page.route("**/api/**", (route) => {
    const url = route.request().url();
    page.__unmockedApiCalls.push(url);
    console.error(`[fail-fast] Unmocked API route: ${url}`);
    return route.abort("failed");
  });
}

/**
 * Assert that no unmocked API calls were recorded by setupFailFast.
 * Call this at the end of each test (or in afterEach) to surface
 * unmocked calls as real test failures.
 *
 * @param {import('@playwright/test').Page} page
 */
export function expectNoUnmockedCalls(page) {
  if (!page.__unmockedApiCalls) {
    throw new Error(
      "setupFailFast() was not called for this test — unmocked API calls would not be detected",
    );
  }
  const calls = page.__unmockedApiCalls;
  if (calls.length > 0) {
    throw new Error(`Unmocked API calls detected:\n  ${calls.join("\n  ")}`);
  }
}
