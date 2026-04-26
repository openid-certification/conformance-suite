import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  wrapDataTablesResponse,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLAN_DETAIL } from "./fixtures/mock-test-data.js";
import { MOCK_LOG_LIST } from "./fixtures/mock-log-list.js";
import { MOCK_TOKEN_USER, MOCK_TOKENS } from "./fixtures/tokens-data.js";

/**
 * Guards ClipboardJS + cts-button integration on every page that renders a
 * `.btn-clipboard` cts-button. The page-level inits use the selector
 * `.btn-clipboard > button` (the inner button rendered by cts-button's light
 * DOM) and a `text` callback that reads `data-clipboard-target` from the host.
 * If cts-button ever switches to shadow DOM, the selector stops matching and
 * these tests fail loudly instead of silently breaking user copy flows.
 *
 * Verification strategy: an init script defines a setter on `window.ClipboardJS`
 * that wraps each new instance so every `success` event records the copied
 * text into `window.__copiedText`. This captures ClipboardJS's own view of the
 * copied text, which is more reliable than reading the system clipboard or
 * sniffing the DOM `copy` event in Playwright-controlled Chromium.
 */

async function installClipboardSpy(page) {
  await page.addInitScript(() => {
    window.__copiedText = null;
    let original;
    Object.defineProperty(window, "ClipboardJS", {
      configurable: true,
      get() {
        return original;
      },
      set(value) {
        const wrapped = function (...args) {
          const instance = new value(...args);
          instance.on("success", (e) => {
            window.__copiedText = e.text;
          });
          return instance;
        };
        wrapped.prototype = value.prototype;
        // Preserve static methods ClipboardJS attaches (isSupported, etc.)
        for (const key of Object.keys(value)) {
          wrapped[key] = value[key];
        }
        original = wrapped;
      },
    });
    // Also spy on navigator.clipboard.writeText so the cts-plan-actions
    // copy flow (which uses the modern Async Clipboard API directly,
    // not ClipboardJS) is observable in tests too. The test harness
    // requires user-activation for real clipboard writes; the spy
    // resolves immediately so the await chain inside _handleCopyConfig
    // doesn't time out.
    if (navigator.clipboard) {
      const originalWriteText = navigator.clipboard.writeText.bind(navigator.clipboard);
      navigator.clipboard.writeText = (text) => {
        window.__copiedText = text;
        try {
          return originalWriteText(text);
        } catch {
          return Promise.resolve();
        }
      };
    }
  });
}

async function readCopiedText(page) {
  return page.evaluate(() => window.__copiedText);
}

test.describe("ClipboardJS copy buttons render text from cts-button hosts", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("plans.html: config modal copy button copies #config text", async ({ page }) => {
    await installClipboardSpy(page);
    await setupFailFast(page);

    await page.route("**/api/plan?*", (route) => {
      const envelope = wrapDataTablesResponse([], route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });

    await setupCommonRoutes(page);

    await page.goto("/plans.html");

    await page.evaluate(() => {
      // #config is now <cts-json-editor>; populate via .value (string),
      // not .textContent — Monaco virtualises text content, and the
      // ClipboardJS handler now reads .value when the target exposes it.
      const config = /** @type {any} */ (document.getElementById("config"));
      if (!config) throw new Error("#config missing");
      config.value = '{"server.issuer":"https://op.example.com"}';
      const modalEl = document.getElementById("configModal");
      /** @type {any} */ (modalEl).show();
    });

    const copyBtn = page.locator("cts-button.btn-clipboard > button");
    await expect(copyBtn).toBeVisible();
    await copyBtn.click();

    await expect
      .poll(() => readCopiedText(page))
      .toBe('{"server.issuer":"https://op.example.com"}');
  });

  test("logs.html: config modal copy button copies #config text", async ({ page }) => {
    await installClipboardSpy(page);
    await setupFailFast(page);

    await page.route("**/api/log?*", (route) => {
      const envelope = wrapDataTablesResponse(MOCK_LOG_LIST, route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(envelope),
      });
    });

    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    await page.evaluate(() => {
      // #config is now <cts-json-editor>; populate via .value, see notes
      // in the plans.html test above for the rationale.
      const config = /** @type {any} */ (document.getElementById("config"));
      if (!config) throw new Error("#config missing");
      config.value = '{"client.client_id":"test-client-id"}';
      const modalEl = document.getElementById("configModal");
      /** @type {any} */ (modalEl).show();
    });

    const copyBtn = page.locator("cts-button.btn-clipboard > button");
    await expect(copyBtn).toBeVisible();
    await copyBtn.click();

    await expect.poll(() => readCopiedText(page)).toBe('{"client.client_id":"test-client-id"}');
  });

  test("plan-detail.html: cts-plan-actions inline copy button copies plan config", async ({
    page,
  }) => {
    await installClipboardSpy(page);
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // After U35, the plan config viewer is an inline panel inside
    // cts-plan-actions, not a modal. The Copy button uses
    // navigator.clipboard.writeText directly (not ClipboardJS).
    await page.locator('cts-plan-actions [data-testid="view-config-btn"] button').click();
    const configPanel = page.locator('cts-plan-actions [data-testid="config-panel"]');
    await expect(configPanel).toBeVisible();

    const copyBtn = configPanel.locator("cts-button.copy-config-btn > button");
    await expect(copyBtn).toBeVisible();
    await copyBtn.click();

    await expect.poll(() => readCopiedText(page)).toContain("server.issuer");
    expect(await readCopiedText(page)).toContain("op.example.com");
  });

  // The legacy "tokens.html: created-modal copy button" test was deleted
  // in U33: tokens.html now composes <cts-token-manager> which renders
  // its own #createdTokenModal and copy button using navigator.clipboard
  // .writeText (not ClipboardJS). Equivalent end-to-end coverage lives in
  // cts-token-manager.stories.js (CreateTemporaryToken / CreatePermanentToken
  // / CopyTokenClipboardFailure / CopyTokenClipboardAbsent).
});
