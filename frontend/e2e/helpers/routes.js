/**
 * Playwright page.route() helpers for E2E tests.
 *
 * All routes MUST be registered before page.goto() because fapi.ui.js fires
 * fetch('api/ui/spec_links?public=true') at script parse time (IIFE at line 741).
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
 * @param {import('@playwright/test').Page} page
 * @param {object} [options]
 * @param {object|null} [options.user] - User data, or null for 401
 */
export async function setupCommonRoutes(page, options = {}) {
  const user = options.user !== undefined ? options.user : MOCK_USER;

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
      body: JSON.stringify({}),
    }),
  );
}

/**
 * Register a wildcard /api/info/:testId route that returns MOCK_TEST_STATUS
 * with the testId from the URL. Pages that need specific data can register
 * a more specific route before this one.
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
 * MUST be called LAST — Playwright matches routes in registration order.
 *
 * The handler aborts the request and records the URL so tests can assert
 * which route was missed.
 */
export async function setupFailFast(page) {
  const unmockedUrls = [];

  await page.route("**/api/**", (route) => {
    const url = route.request().url();
    unmockedUrls.push(url);
    console.error(`[fail-fast] Unmocked API route: ${url}`);
    return route.abort("failed");
  });

  return unmockedUrls;
}
