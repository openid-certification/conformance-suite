import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import { MOCK_PLANS, MOCK_PLAN_NO_VARIANTS } from "./fixtures/mock-plans.js";

/**
 * R12 — Monaco JSON editor on the schedule-test.html JSON tab.
 *
 * After Phase 2 the JSON editor is owned by <cts-config-form>: the page
 * mounts <cts-config-form id="ctsConfigForm"> which renders Form/JSON tabs
 * internally, with a <cts-json-editor> living inside the JSON tab. There is
 * no longer a top-level `#config` element. These tests cover both the happy
 * path (Monaco mounts inside the component's JSON tab) and the explicit
 * fallback path (Monaco loader blocked → textarea renders, .value still
 * round-trips).
 */

const ALL_PLANS = [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS];

/**
 * @param {import('@playwright/test').Page} page
 */
async function setupSchedulePageRoutes(page) {
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
}

/**
 * cts-config-form lazy-mounts its inner <cts-json-editor> only after the
 * JSON tab is activated at least once. The cascade also needs to reach a
 * plan + all variants so updateConfigFieldVisibility binds the schema and
 * shows the form. Use the no-variants plan so the form renders immediately
 * after the cascade resolves, then click the JSON tab to trigger the mount.
 *
 * @param {import('@playwright/test').Page} page
 */
async function showJsonTab(page) {
  await page.locator("#specFamilySelect").selectOption("OIDCC");
  await page.locator("#entitySelect").selectOption("client-basic");
  // Wait for cts-config-form to actually render fields (schema bound).
  await expect(page.locator('cts-form-field[name="alias"]').first()).toBeAttached({
    timeout: 5000,
  });
  // Click the JSON tab inside the host (it's an oidf-tab in cts-tabs light DOM).
  await page.getByRole("tab", { name: "JSON" }).click();
}

test.describe("schedule-test.html — R12 Monaco JSON editor inside <cts-config-form>", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("cts-json-editor mounts inside the JSON tab and exposes .value as a string", async ({
    page,
  }) => {
    await setupSchedulePageRoutes(page);
    await page.goto("/schedule-test.html");
    await showJsonTab(page);

    const editor = page.locator("#ctsConfigForm cts-json-editor");
    await expect(editor).toBeAttached({ timeout: 10000 });

    // Wait until either Monaco's editor chrome appears or the wrapper
    // falls back to a textarea — both are valid mounted states.
    await page.waitForFunction(
      () => {
        const host = document.querySelector("#ctsConfigForm cts-json-editor");
        if (!host) return false;
        return Boolean(
          host.querySelector(".monaco-editor") || host.querySelector(".oidf-json-editor-fallback"),
        );
      },
      null,
      { timeout: 10000 },
    );

    // cts-json-editor's .value getter/setter is the contract the host page
    // (and the surrounding cts-config-form merge logic) depends on.
    const echoedValue = await page.evaluate(() => {
      const host = /** @type {any} */ (document.querySelector("#ctsConfigForm cts-json-editor"));
      if (!host) throw new Error("cts-json-editor inside #ctsConfigForm not found");
      host.value = '{"alias":"e2e-mounted"}';
      return host.value;
    });
    expect(echoedValue).toBe('{"alias":"e2e-mounted"}');
  });

  test("input event on the editor updates cts-config-form's .config", async ({ page }) => {
    await setupSchedulePageRoutes(page);
    await page.goto("/schedule-test.html");
    await showJsonTab(page);

    await page.waitForFunction(
      () => {
        const host = document.querySelector("#ctsConfigForm cts-json-editor");
        return (
          host?.querySelector(".monaco-editor") || host?.querySelector(".oidf-json-editor-fallback")
        );
      },
      null,
      { timeout: 10000 },
    );

    const result = await page.evaluate(() => {
      const editor = /** @type {any} */ (document.querySelector("#ctsConfigForm cts-json-editor"));
      if (!editor) throw new Error("cts-json-editor not found");
      const form = /** @type {any} */ (document.getElementById("ctsConfigForm"));
      // Drive a synthetic edit through the fallback when present, otherwise
      // through the editor's public .value setter — the host then dispatches
      // input which cts-config-form's _handleJsonInput catches and merges.
      const fallback = /** @type {HTMLTextAreaElement | null} */ (
        editor.querySelector(".oidf-json-editor-fallback")
      );
      if (fallback) {
        fallback.value = '{"alias":"edited"}';
        fallback.dispatchEvent(new Event("input", { bubbles: true }));
      } else {
        editor.value = '{"alias":"edited"}';
        editor.dispatchEvent(new Event("input", { bubbles: true }));
      }
      return { editorValue: editor.value, formConfig: form.config };
    });

    expect(result.editorValue).toBe('{"alias":"edited"}');
    expect(result.formConfig).toEqual({ alias: "edited" });
  });

  test("fallback path: loader.js blocked → textarea renders and .value round-trips", async ({
    page,
  }) => {
    await setupSchedulePageRoutes(page);

    // Block Monaco's loader BEFORE page.goto so the very first request
    // for /vendor/monaco-editor/vs/loader.js returns 503. This is the
    // shape of the production failure mode (CSP block, network drop):
    // the wrapper notices the script error and renders a textarea.
    await page.route("**/vendor/monaco-editor/**", (route) =>
      route.fulfill({ status: 503, body: "" }),
    );

    await page.goto("/schedule-test.html");
    await showJsonTab(page);

    const fallback = page.locator("#ctsConfigForm cts-json-editor .oidf-json-editor-fallback");
    await expect(fallback).toBeVisible({ timeout: 10000 });
    await expect(page.locator("#ctsConfigForm cts-json-editor .monaco-editor")).toHaveCount(0);

    const result = await page.evaluate(() => {
      const host = /** @type {any} */ (document.querySelector("#ctsConfigForm cts-json-editor"));
      if (!host) throw new Error("cts-json-editor not found");
      const ta = /** @type {HTMLTextAreaElement | null} */ (
        host.querySelector(".oidf-json-editor-fallback")
      );
      if (!ta) throw new Error("fallback textarea not rendered");
      ta.value = '{"alias":"fallback-edit"}';
      ta.dispatchEvent(new Event("input", { bubbles: true }));
      return host.value;
    });
    expect(result).toBe('{"alias":"fallback-edit"}');
  });
});
