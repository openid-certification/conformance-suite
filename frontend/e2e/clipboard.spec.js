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
      const config = document.getElementById("config");
      if (!config) throw new Error("#config missing");
      config.textContent = '{"server.issuer":"https://op.example.com"}';
      const modalEl = document.getElementById("configModal");
      modalEl.show();
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
      const config = document.getElementById("config");
      if (!config) throw new Error("#config missing");
      config.textContent = '{"client.client_id":"test-client-id"}';
      const modalEl = document.getElementById("configModal");
      modalEl.show();
    });

    const copyBtn = page.locator("cts-button.btn-clipboard > button");
    await expect(copyBtn).toBeVisible();
    await copyBtn.click();

    await expect.poll(() => readCopiedText(page)).toBe('{"client.client_id":"test-client-id"}');
  });

  test("plan-detail.html: config modal copy button copies #config text", async ({ page }) => {
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

    // The real View Config button fills #config and opens the modal.
    await page.locator("#showConfigBtn").click();
    await expect(page.locator("#configModal")).toBeVisible();

    const copyBtn = page.locator("#configModal cts-button.btn-clipboard > button");
    await expect(copyBtn).toBeVisible();
    await copyBtn.click();

    await expect.poll(() => readCopiedText(page)).toContain("server.issuer");
    expect(await readCopiedText(page)).toContain("op.example.com");
  });

  test("tokens.html: created-modal copy button copies #tokenValue text", async ({ page }) => {
    await installClipboardSpy(page);
    await setupFailFast(page);

    await page.route("**/api/token?*", (route) => {
      if (route.request().method() !== "GET") return route.fallback();
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_TOKENS),
      });
    });

    await setupCommonRoutes(page, { user: MOCK_TOKEN_USER });

    await page.goto("/tokens.html");

    await expect(page.locator("cts-button.btn-clipboard")).toBeAttached();

    await page.evaluate(() => {
      const tokenValue = document.getElementById("tokenValue");
      if (!tokenValue) throw new Error("#tokenValue missing");
      tokenValue.textContent = "super-secret-token-value-123";
      const modalEl = document.getElementById("createdModal");
      modalEl.show();
    });

    const copyBtn = page.locator("#createdModal cts-button.btn-clipboard > button");
    await expect(copyBtn).toBeVisible();
    await copyBtn.click();

    await expect.poll(() => readCopiedText(page)).toBe("super-secret-token-value-123");
  });
});
