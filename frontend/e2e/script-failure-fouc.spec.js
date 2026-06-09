// @ts-check
import { test, expect } from "@playwright/test";

/**
 * Regression spec for the "random Private Link dialog" bug (reported by
 * Thomas Darimont): until cts-modal's definition executes, the light-DOM
 * contents of declarative <cts-modal> elements are ordinary inline content
 * and render as plain page text ("Error:", "How many days should this link
 * remain active?", a number input…). layout.css guards this with
 * `cts-modal:not(:defined) { display: none }`, but log-detail.html loads
 * only the OIDF sheets and the guard was never carried over — so slow first
 * loads (cold cache, long component-module chain) flashed the raw modal
 * text; warm-cache reloads upgraded instantly, which made the bug look
 * random. The guard now also lives in oidf-app.css.
 *
 * The spec makes the upgrade window permanent by aborting every .js request
 * before navigation, then asserts no modal innards are visible on any page
 * carrying declarative modals.
 */

const PAGES_WITH_DECLARATIVE_MODALS = [
  "/log-detail.html?log=test123",
  "/logs.html",
  "/running-test.html",
  "/plan-detail.html?plan=plan123",
  "/upload.html",
  "/schedule-test.html",
];

for (const path of PAGES_WITH_DECLARATIVE_MODALS) {
  test(`un-upgraded cts-modal content stays hidden when scripts fail: ${path}`, async ({
    page,
  }) => {
    // Abort ALL JavaScript (module scripts, vendor bundles, importmap
    // targets) — the worst-case load failure. No API routes are needed:
    // nothing runs to fetch them.
    await page.route("**/*.js", (route) => route.abort());
    await page.goto(path, { waitUntil: "domcontentloaded" });

    // Every <cts-modal> must be invisible while un-upgraded.
    for (const modal of await page.locator("cts-modal").all()) {
      await expect(modal).toBeHidden();
    }

    // Belt and braces for the reported page: the private-link expiration
    // prompt must not be readable as page text.
    if (path.startsWith("/log-detail")) {
      await expect(page.getByText("How many days should this link remain active?")).toBeHidden();
    }
  });
}
