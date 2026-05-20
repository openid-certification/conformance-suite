import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLAN_DETAIL } from "./fixtures/mock-test-data.js";

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

  // The legacy "plans.html: config modal copy button" test was deleted
  // when plans.html migrated to <cts-plan-list>: the page no longer renders
  // a global #configModal or a ClipboardJS-wired .btn-clipboard. cts-plan-list
  // owns its own #planConfigModal and copies via navigator.clipboard.writeText
  // reading from internal _selectedConfig state — not from a DOM target.
  // Equivalent end-to-end coverage lives in cts-plan-list.stories.js (ViewConfig
  // play function asserts navigator.clipboard.writeText is called with the
  // config JSON). plans.spec.js verifies the modal opens and the copy button
  // is reachable. The Monaco-fallback variant is moot for cts-plan-list
  // because the copy path bypasses Monaco entirely.

  // The legacy "logs.html: config modal copy button copies #config text" test
  // was deleted in the 2026-05-20 logs-page redesign: logs.html now composes
  // <cts-log-list>, which mounts its own config modal (`#cts-log-list-config-
  // modal`) and copies via navigator.clipboard.writeText directly (not
  // ClipboardJS, not a DOM target). The structural contract (modal opens,
  // copy button visible, label is "Copy configuration") is enforced by
  // frontend/e2e/logs.spec.js's "config button in card opens config modal"
  // test and by the ConfigModal play function in cts-log-list.stories.js
  // (when added). The Monaco-fallback variant below is moot for the same
  // reason — the copy reads from internal state, not the editor's .value.

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

  // Fallback-path coverage: when Monaco's loader is blocked the wrapper
  // renders a real <textarea> instead. The shared resolver in
  // /js/cts-clipboard-resolver.js still has to read `.value` (now off the
  // textarea) for the copy to land. Without these tests, only
  // schedule-test-monaco.spec.js exercises the loader-blocked path — and
  // it doesn't touch the configModal copy button on plans.html / logs.html.

  // The legacy "plans.html (Monaco fallback)" test was deleted alongside the
  // primary plans.html clipboard test above — the fallback path is irrelevant
  // for cts-plan-list because its copy reads from internal state, not from
  // the cts-json-editor's .value. See the comment above for the equivalent
  // coverage map.

  // The legacy "logs.html (Monaco fallback)" test was deleted alongside the
  // primary logs.html clipboard test above. After the cts-log-list migration,
  // the copy path reads from internal _selectedConfig state, not from the
  // cts-json-editor's .value — so the Monaco-fallback path no longer affects
  // copy correctness on logs.html.
});
