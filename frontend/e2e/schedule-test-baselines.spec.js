/**
 * U1 verification finding (per docs/plans/2026-04-25-003-feat-schedule-test-derisking-baseline-plan.md):
 * No JS dependency on `col-md-2 key` as a CSS selector found in either
 * src/main/resources/static/js/fapi.ui.js or schedule-test.html's inline
 * <script> block. The class is purely visual; R42 can add <label for> to
 * the ~134 dynamic config fields without breaking JS query/traversal.
 *
 * These baselines exist to make drift visible across the five Tier 1 R-MRs
 * (R9, R13, R14, R15, R42) that will land on schedule-test.html. Each MR
 * refreshes the affected snapshots in the same commit; reviewers inspect
 * the diff line-by-line.
 *
 * Snapshots catch missing labels and structural drift. They do NOT catch
 * label/input misassociation — assertLabelInputPairing handles that.
 *
 * Stress-test command before committing snapshot updates:
 *   npx playwright test schedule-test-baselines.spec.js --repeat-each=10 --workers=5
 */

import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import {
  assertNoIdCollisions,
  assertLabelInputPairing,
  getNormalizedInnerHTML,
} from "./helpers/assertions.js";
import { MOCK_PLANS, MOCK_PLAN_NO_VARIANTS } from "./fixtures/mock-plans.js";

const ALL_PLANS = [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS];

/** Custom elements imported by schedule-test.html. */
const CTS_CUSTOM_ELEMENTS = [
  "cts-navbar",
  "cts-modal",
  "cts-tabs",
  "cts-badge",
  "cts-button",
  "cts-alert",
];

/**
 * Wait for all imported custom elements to upgrade before capturing
 * snapshots — a half-upgraded component leaves the aria tree in a
 * non-deterministic state across runs.
 *
 * @param {import('@playwright/test').Page} page
 */
async function waitForCustomElementsUpgrade(page) {
  await page.evaluate(
    /** @param {string[]} tags */
    (tags) => Promise.all(tags.map((t) => customElements.whenDefined(t))),
    CTS_CUSTOM_ELEMENTS,
  );
}

/**
 * Wait for FAPI_UI's init chain to install `createPlanBtn.onclick`. This
 * is the signal documented in
 *   docs/solutions/test-failures/playwright-e2e-flaky-after-web-component-merge-2026-04-14.md
 * It marks the end of `loadScheduleTestPage()` (schedule-test.html:2461),
 * which sits between `loadConfig()` and the trailing init steps.
 *
 * @param {import('@playwright/test').Page} page
 */
async function waitForPageInit(page) {
  await page.waitForFunction(() => document.getElementById("createPlanBtn")?.onclick !== null);
}

test.describe("schedule-test.html — baselines", () => {
  test.beforeEach(async ({ page }) => {
    // Defensive: loadLastConfig() (schedule-test.html:~2382) reads
    // localStorage first and short-circuits if `savedConfig` is present,
    // bypassing the /api/lastconfig mock. Clear both stores on every
    // page navigation so the mock is the single source of truth.
    await page.addInitScript(() => {
      try {
        localStorage.clear();
        sessionStorage.clear();
      } catch {
        /* storage access can throw on file: URLs; ignore */
      }
    });
  });

  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("State A — empty new plan", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    await waitForCustomElementsUpgrade(page);
    await waitForPageInit(page);

    await assertNoIdCollisions(page);
    await assertLabelInputPairing(page);

    await expect(page.locator("#scheduleTestPage")).toMatchAriaSnapshot({
      name: "state-a.aria.yml",
    });
    const html = await getNormalizedInnerHTML(page.locator("#scheduleTestPage"));
    expect(html).toMatchSnapshot("state-a.html");
  });

  test("State B — cascade mid-selection (OIDCC basic, variants visible)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    await waitForCustomElementsUpgrade(page);
    await waitForPageInit(page);

    // Cascade: select OIDCC family, then `basic` entity → variant selectors
    // become visible (no variant chosen yet).
    await page.locator("#specFamilySelect").selectOption("OIDCC");
    const entitySelect = page.locator("#entitySelect");
    await expect(entitySelect).toBeVisible();
    await entitySelect.selectOption("basic");

    await expect(page.locator("#variantSelectors")).toBeVisible();
    await expect(page.locator("select.variant-selector").first()).toBeVisible();

    await assertNoIdCollisions(page);
    await assertLabelInputPairing(page);

    await expect(page.locator("#scheduleTestPage")).toMatchAriaSnapshot({
      name: "state-b.aria.yml",
    });
    const html = await getNormalizedInnerHTML(page.locator("#scheduleTestPage"));
    expect(html).toMatchSnapshot("state-b.html");
  });

  test("State C — plan with config loaded (no-variants plan, lastconfig populated)", async ({
    page,
  }) => {
    await setupFailFast(page);

    // Use ALL_PLANS so entitySelect is visible (multiple OIDCC entities:
    // `basic` and `client-basic`). With only one entity, the cascade
    // auto-selects and hides the entity dropdown, which would defeat the
    // "config form rendered" goal of this state.
    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );

    // Non-empty /api/lastconfig payload — surfaced into the #config
    // textarea by the explicit "Load last configuration" click below.
    // Before R13 this test relied on auto-prefill at init; the load step
    // is now user-driven.
    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          config: {
            alias: "baseline-state-c",
            description: "Snapshot baseline — plan with config loaded",
            server: { issuer: "https://baseline.example.com" },
          },
        }),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/schedule-test.html");

    await waitForCustomElementsUpgrade(page);
    await waitForPageInit(page);

    // Cascade through to the no-variants plan so the config form renders.
    await page.locator("#specFamilySelect").selectOption("OIDCC");
    const entitySelect = page.locator("#entitySelect");
    await expect(entitySelect).toBeVisible();
    await entitySelect.selectOption("client-basic");

    // R13: explicitly load the saved config to populate the textarea.
    await page.getByTestId("load-last-config").click();

    // Wait for the config textarea to have content.
    await page.waitForFunction(() => {
      const el = /** @type {HTMLTextAreaElement | null} */ (document.getElementById("config"));
      return !!el && typeof el.value === "string" && el.value.length > 0;
    });

    await assertNoIdCollisions(page);
    await assertLabelInputPairing(page);

    await expect(page.locator("#scheduleTestPage")).toMatchAriaSnapshot({
      name: "state-c.aria.yml",
    });
    const html = await getNormalizedInnerHTML(page.locator("#scheduleTestPage"));
    expect(html).toMatchSnapshot("state-c.html");
  });
});
