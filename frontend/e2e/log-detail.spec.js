import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import {
  MOCK_TEST_STATUS,
  MOCK_TEST_FAILED,
  MOCK_TEST_RUNNING,
} from "./fixtures/mock-test-data.js";
import {
  MOCK_LOG_ENTRIES,
  MOCK_FAILED_LOG_ENTRIES,
  MOCK_BLOCKS_WITH_STATUS,
  MOCK_BLOCKS_POLL_FIRST,
  MOCK_BLOCKS_POLL_SECOND,
} from "./fixtures/mock-log-entries.js";

/**
 * Coverage for log-detail.html — the new Lit-triad-based page.
 * This spec navigates directly to the new page (no flag, no cookie),
 * mocks the API surface the bootstrap depends on, and asserts that
 * cts-log-detail-header + cts-log-viewer render with the right data
 * and that the new affordances (Edit configuration, Share Link,
 * Repeat Test) fire the expected events / navigations.
 *
 * Plan: docs/plans/2026-04-26-002-refactor-log-detail-page-to-lit-triad-plan.md
 *
 * ─────────────────────────────────────────────────────────────────────
 * U2 — Coverage matrix vs legacy frontend/e2e/log-detail.spec.js
 * ─────────────────────────────────────────────────────────────────────
 * The legacy spec is the behavioral oracle for parity. For each
 * legacy test() block, this matrix records either the v2 equivalent
 * here or an explicit "obsolete: <reason>" note. Built during U2 of
 * docs/plans/2026-04-27-002-refactor-retire-legacy-log-detail-plan.md.
 *
 * R16 "loads and renders log header" → covered by
 *     "renders cts-log-detail-header with test metadata".
 * R17 "renders log entries with source/message/result badges" →
 *     covered by "renders cts-log-viewer with mocked log entries"
 *     and "per-block status badges render in each block summary".
 * R18 "clicking a log entry expands detailed content" → covered at
 *     component scope by cts-log-entry stories; v2 page-level
 *     expansion is exercised by "clicking a block summary collapses
 *     the children via <details>".
 * R19 "failed test shows failure summary section" → covered by
 *     "failure summary jump-link bubbles cts-scroll-to-entry to the
 *     page" and "failure summary swaps between header and page-level
 *     positions".
 * R20 "warning results are styled distinctly from failures and
 *     passes" → obsolete at e2e scope: variant rendering is owned by
 *     cts-badge stories; v2 uses canonical badge variants throughout
 *     and a regression there would be caught by the badge story
 *     suite, not log-detail e2e.
 * "View configuration button opens modal with test configuration
 *     JSON" → covered by "Edit configuration button fires
 *     cts-edit-config" plus the cts-action-overflow stories that
 *     drive the new kebab-housed view-config flow; the legacy modal
 *     template (templates/privateLinkModals.html, etc.) is gone in U5.
 * "status and result tooltips render on header" → obsolete: the v2
 *     sticky bar uses self-describing cts-badge labels (PASSED /
 *     FAILED / RUNNING). No tooltip surface remains in the new chrome.
 * "log entry more panel shows HTTP request/response details and
 *     collapses on second click" → covered at component scope by
 *     cts-log-entry stories.
 * "banner transitions: FINISHED runner shows Inactive, hides
 *     Active/Archived" → obsolete: the legacy three-banner Active /
 *     Inactive / Archived semantics collapsed in v2 into the hero's
 *     lifecycle-driven dispatch. FINISHED is the absence of the
 *     RUNNING / WAITING / archived states.
 * "banner transitions: RUNNING runner shows Active" → obsolete: the
 *     RUNNING hero (data-testid="hero-running") replaces the legacy
 *     #runningTestActive banner; verified by the
 *     cts-log-detail-header RunningTest story.
 * "banner transitions: runner 404 shows Archived banner" → covered
 *     by "archived banner appears when /api/runner returns 404".
 * "runner error response injects cts-alert + stacktrace reveals on
 *     click" → covered by "INTERRUPTED runner error renders danger
 *     alert with stacktrace toggle" (this file).
 * "failure summary items are clickable" → covered by "failure
 *     summary jump-link bubbles cts-scroll-to-entry" and
 *     "failure-summary jump-link opens a collapsed block".
 * R24 split-marker variants → covered by cts-log-detail-header
 *     stories (PassedHeroDescriptionAndMarkerSplit /
 *     PassedHeroDescriptionOnly / WaitingHeroWithInstructions /
 *     WaitingHeroFallbackInstructions).
 * R21 nav widget (4 legacy tests) → covered at component scope by
 *     cts-test-nav-controls.stories.js; v2 page-level wiring is the
 *     same handler set.
 * ─────────────────────────────────────────────────────────────────────
 */

/**
 * Register all routes the new bootstrap depends on. Mirror of
 * setupLogDetailRoutes from log-detail.spec.js but trimmed to the
 * surface the new bootstrap actually fires (no /api/runner poll for
 * a FINISHED test, no /api/uploaded-images multi-shape variants, etc.).
 *
 * @param {import('@playwright/test').Page} page
 * @param {{ testInfo: any, logEntries: any, planModules?: any[] }} options
 */
async function setupV2Routes(page, { testInfo, logEntries, planModules }) {
  const testId = testInfo.testId;

  await page.route(`**/api/info/${testId}*`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(testInfo),
    }),
  );

  await page.route(`**/api/log/${testId}**`, (route) => {
    const url = new URL(route.request().url());
    const since = url.searchParams.get("since");
    if (since && Number(since) > 0) {
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([]),
      });
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(logEntries),
    });
  });

  if (testInfo.planId) {
    await page.route(`**/api/plan/${testInfo.planId}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          _id: testInfo.planId,
          planName: "test-plan",
          ...(planModules
            ? { modules: planModules }
            : {
                modules: [{ testModule: testInfo.testName, variant: testInfo.variant || {} }],
              }),
        }),
      }),
    );
  }

  // /api/runner — return 404 for FINISHED tests so the runner-poll
  // helper exits cleanly. RUNNING / WAITING tests would return data
  // here; specs that need that path will register a more specific
  // route before calling this helper.
  await page.route(`**/api/runner/${testId}`, (route) => route.fulfill({ status: 404, body: "" }));

  await page.route("**/api/uploaded-images*", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
  );
}

test.describe("log-detail.html — new Lit-triad page", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("renders cts-log-detail-header with test metadata", async ({ page }) => {
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`);

    // Test name + ID land in the header.
    const header = page.locator("cts-log-detail-header");
    await expect(header).toContainText(MOCK_TEST_STATUS.testName);
    await expect(header).toContainText(MOCK_TEST_STATUS.testId);

    // Result + status badges.
    await expect(page.locator('cts-badge[label="PASSED"]')).toBeVisible();
    await expect(page.locator('cts-badge[label="FINISHED"]')).toBeVisible();
  });

  test("renders cts-log-viewer with mocked log entries", async ({ page }) => {
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`);

    const viewer = page.locator("cts-log-viewer");
    await expect(viewer).toBeVisible();

    // Wait for at least one log entry row.
    await expect(page.locator(".logItem").first()).toBeVisible();
  });

  test("Edit configuration button fires cts-edit-config with the right detail", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`);

    // Wait for the header to render (await testInfo flow).
    const header = page.locator("cts-log-detail-header");
    await expect(header).toContainText(MOCK_TEST_STATUS.testName);

    // Capture the cts-edit-config event before the click — the page-level
    // handler in log-detail.js tries to navigate to schedule-test.html
    // which in turn fires its own /api/plan/available call. Asserting on
    // the event detail (rather than the URL) keeps this test focused on
    // U1's contract: header → event → page-level handler.
    const detailJson = /** @type {string} */ (
      await page.evaluate(() => {
        return new Promise((resolve) => {
          document.addEventListener(
            "cts-edit-config",
            /** @param {Event} e */ (e) => {
              const detail = /** @type {CustomEvent} */ (e).detail;
              resolve(JSON.stringify(detail));
            },
            { once: true },
          );
          // Stop the page-level handler from following through to
          // schedule-test.html — we don't need to load that page, only
          // observe the bubbled event.
          window.addEventListener(
            "beforeunload",
            (ev) => {
              ev.preventDefault();
              ev.returnValue = "";
            },
            { once: true },
          );
          const inner = document.querySelector('cts-button[data-testid="edit-config-btn"] button');
          if (inner) /** @type {HTMLButtonElement} */ (inner).click();
        });
      })
    );

    const detail = JSON.parse(detailJson);
    expect(detail.testId).toBe(MOCK_TEST_STATUS.testId);
    expect(detail.planId).toBe(MOCK_TEST_STATUS.planId);
    expect(detail.config).toEqual(MOCK_TEST_STATUS.config);
  });

  test("Private link button opens the expiration modal", async ({ page }) => {
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`);

    // Wait for the header → action stack → share-link button.
    const header = page.locator("cts-log-detail-header");
    await expect(header).toContainText(MOCK_TEST_STATUS.testName);

    const expirationModal = page.locator("#privateLinkExpirationModal");
    await expect(expirationModal).toBeHidden();

    // Click via inner <button> — the cts-button host-click pattern.
    const shareInnerBtn = page.locator('cts-button[data-testid="share-link-btn"] button');
    await shareInnerBtn.click();

    // cts-modal is built on native <dialog>; show() sets the host's
    // `open` state and the host becomes visible via :host([open]) CSS.
    await expect(expirationModal).toBeVisible();
  });

  test("failure summary jump-link bubbles cts-scroll-to-entry to the page", async ({ page }) => {
    // Inject FAILURE entries into testInfo.results so the Lit header's
    // _renderFailureSummary() has data to render. The base MOCK_TEST_FAILED
    // fixture defines result: "FAILED" but no per-condition results.
    const failedTestInfo = {
      ...MOCK_TEST_FAILED,
      results: [
        {
          _id: "fail-r1",
          result: "FAILURE",
          src: "ValidateIdToken",
          msg: "Signature invalid",
          requirements: ["OIDCC-3.1.3.7-6"],
        },
      ],
    };

    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: failedTestInfo,
      logEntries: MOCK_FAILED_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(failedTestInfo.testId)}`);

    // The failure-summary section is gated on having FAILURE/WARNING/
    // SKIPPED/INTERRUPTED entries in testInfo.results; injected above.
    const failureItem = page.locator(".failureSummary .failureText").first();
    await expect(failureItem).toBeVisible();

    // Capture the bubbled cts-scroll-to-entry event from the document.
    const eventDetail = await page.evaluate(() => {
      return new Promise((resolve) => {
        document.addEventListener(
          "cts-scroll-to-entry",
          /** @param {Event} e */ (e) => {
            const detail = /** @type {CustomEvent} */ (e).detail;
            resolve(detail);
          },
          { once: true },
        );
        const failure = document.querySelector(".failureSummary .failureText");
        if (failure) /** @type {HTMLElement} */ (failure).click();
      });
    });

    expect(eventDetail).toMatchObject({ entryId: expect.any(String) });
  });

  test("failure summary swaps between header and page-level positions at 1024px breakpoint", async ({
    page,
  }) => {
    // U4: two `<cts-failure-summary>` instances render simultaneously in
    // log-detail.html — one inside the header card (desktop position)
    // and one directly below the sticky status bar
    // (`#ctsTopFailureSummary`, mobile/tablet position). Page-level CSS
    // hides whichever doesn't apply at the current breakpoint
    // (`render-twice-hide-one`).
    // Plan: docs/plans/2026-04-26-005-feat-extract-cts-failure-summary-plan.md
    const failedTestInfo = {
      ...MOCK_TEST_FAILED,
      results: [
        {
          _id: "swap-r1",
          result: "FAILURE",
          src: "ValidateIdToken",
          msg: "Signature invalid",
        },
      ],
    };

    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: failedTestInfo,
      logEntries: MOCK_FAILED_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(failedTestInfo.testId)}`);

    // Wait for the failure summary to mount inside the header before
    // measuring breakpoints — the header reactively renders when
    // testInfo lands.
    await expect(page.locator("cts-log-detail-header cts-failure-summary")).toBeAttached();

    // Desktop (≥ 1024px): in-header instance is visible, page-level
    // instance is `display: none`.
    await page.setViewportSize({ width: 1280, height: 800 });
    const headerSummaryDisplay = await page.evaluate(
      () =>
        getComputedStyle(
          /** @type {Element} */ (
            document.querySelector("cts-log-detail-header cts-failure-summary")
          ),
        ).display,
    );
    const topSummaryDisplay = await page.evaluate(
      () =>
        getComputedStyle(/** @type {Element} */ (document.getElementById("ctsTopFailureSummary")))
          .display,
    );
    expect(headerSummaryDisplay).not.toBe("none");
    expect(topSummaryDisplay).toBe("none");

    // Tablet (< 1024px): inverse — page-level visible, in-header hidden.
    await page.setViewportSize({ width: 768, height: 1024 });
    const headerSummaryDisplayTablet = await page.evaluate(
      () =>
        getComputedStyle(
          /** @type {Element} */ (
            document.querySelector("cts-log-detail-header cts-failure-summary")
          ),
        ).display,
    );
    const topSummaryDisplayTablet = await page.evaluate(
      () =>
        getComputedStyle(/** @type {Element} */ (document.getElementById("ctsTopFailureSummary")))
          .display,
    );
    expect(headerSummaryDisplayTablet).toBe("none");
    expect(topSummaryDisplayTablet).not.toBe("none");

    // The visible (page-level) instance still bubbles
    // cts-scroll-to-entry to the document — same contract as the
    // in-header instance.
    const eventDetail = await page.evaluate(() => {
      return new Promise((resolve) => {
        document.addEventListener(
          "cts-scroll-to-entry",
          /** @param {Event} e */ (e) => {
            const detail = /** @type {CustomEvent} */ (e).detail;
            resolve(detail);
          },
          { once: true },
        );
        const failure = document.querySelector(
          "#ctsTopFailureSummary .failureSummary .failureText",
        );
        if (failure) /** @type {HTMLElement} */ (failure).click();
      });
    });
    expect(eventDetail).toMatchObject({ entryId: "swap-r1" });
  });

  test("entries stream does not overflow horizontally at 375px viewport", async ({ page }) => {
    // U3: cts-log-entry uses a container query keyed on the host's inline
    // size to reflow at < 640px. At 375px the entries stream must stack
    // each row's meta cluster on top of body+actions rather than overflow
    // horizontally — measured directly on the .logEntries scroll container.
    // Plan: docs/plans/2026-04-26-004-feat-log-entry-container-query-reflow-plan.md
    await page.setViewportSize({ width: 375, height: 800 });

    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`);

    // Wait for at least one row to land in the DOM.
    await expect(page.locator(".logItem").first()).toBeVisible();

    const overflow = await page.evaluate(() => {
      const stream = document.querySelector("cts-log-viewer .logEntries");
      if (!stream) return { found: false, scrollWidth: 0, clientWidth: 0 };
      return {
        found: true,
        scrollWidth: stream.scrollWidth,
        clientWidth: stream.clientWidth,
      };
    });

    expect(overflow.found).toBe(true);
    expect(overflow.scrollWidth).toBeLessThanOrEqual(overflow.clientWidth);
  });

  test("sticky status bar pins to the top of the viewport on scroll", async ({ page }) => {
    // U2: <cts-log-detail-header>'s status bar uses position: sticky at
    // >= 640px. Playwright's default viewport (1280x720) sits in that
    // range. The bar publishes its measured height to
    // document.documentElement.--status-bar-height, which downstream
    // sticky descendants (connection-lost banner, R32 entry anchors)
    // read for top-offset coordination.
    // Plan: docs/plans/2026-04-26-003-feat-status-bar-sticky-and-mode-aware-plan.md
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`);

    // Wait for the bar to mount before measuring.
    const bar = page.locator('[data-testid="status-bar"]');
    await expect(bar).toBeVisible();

    // The component publishes a non-zero pixel value to the inline style
    // on document.documentElement after firstUpdated() runs the
    // ResizeObserver attach + initial measure. Downstream descendants
    // (banner, R32 anchors) read this property for top-offset.
    await expect
      .poll(async () =>
        page.evaluate(() =>
          getComputedStyle(document.documentElement).getPropertyValue("--status-bar-height").trim(),
        ),
      )
      .toMatch(/^\d+px$/);

    const publishedHeight = await page.evaluate(() =>
      getComputedStyle(document.documentElement).getPropertyValue("--status-bar-height").trim(),
    );
    expect(publishedHeight).not.toBe("0px");

    // Pad the body so a 1200px scroll has somewhere to go without
    // depending on the height of mock log entries.
    await page.evaluate(() => {
      document.body.style.minHeight = "3000px";
    });
    await page.evaluate(() => window.scrollTo(0, 1200));

    // After scrolling, the sticky bar should still report top === 0
    // because it pins to the viewport, not the document.
    const top = await bar.evaluate((el) => el.getBoundingClientRect().top);
    expect(top).toBe(0);

    // Primary action button stays in the viewport (its top y is
    // less than viewport height) so it remains clickable through
    // the scroll. boundingBox() returns null only for elements with
    // display: none / not in layout — the visibility assertion above
    // already excludes that case, so destructure with a non-null cast.
    const primary = page.locator('[data-testid="status-bar-primary"]');
    const primaryBox = /** @type {{ x: number; y: number; width: number; height: number }} */ (
      await primary.boundingBox()
    );
    expect(primaryBox).not.toBeNull();
    const viewportHeight = page.viewportSize()?.height ?? 720;
    expect(primaryBox.y).toBeLessThan(viewportHeight);
  });

  // U5 — Per-block status aggregation + <details> semantics.
  // Plan: docs/plans/2026-04-26-006-feat-r27-per-block-status-aggregation-plan.md
  test("per-block status badges render in each block summary", async ({ page }) => {
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: { ...MOCK_TEST_STATUS, testId: "test-blocks-001" },
      logEntries: MOCK_BLOCKS_WITH_STATUS,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent("test-blocks-001")}`);

    await expect(page.locator('details[data-block-id="block-a"]')).toBeAttached();
    await expect(page.locator('details[data-block-id="block-b"]')).toBeAttached();
    await expect(page.locator('details[data-block-id="block-c"]')).toBeAttached();

    // Block A: ✓2 only.
    const aBadges = page.locator('details[data-block-id="block-a"] .startBlockCounts cts-badge');
    await expect(aBadges).toHaveCount(1);
    await expect(aBadges.first()).toHaveAttribute("label", "✓2");

    // Block B: ✓1 ✗1, in spec order.
    const bBadges = page.locator('details[data-block-id="block-b"] .startBlockCounts cts-badge');
    await expect(bBadges).toHaveCount(2);
    await expect(bBadges.nth(0)).toHaveAttribute("label", "✓1");
    await expect(bBadges.nth(1)).toHaveAttribute("label", "✗1");

    // Block C: ⚠1 only — INFO is excluded by design.
    const cBadges = page.locator('details[data-block-id="block-c"] .startBlockCounts cts-badge');
    await expect(cBadges).toHaveCount(1);
    await expect(cBadges.first()).toHaveAttribute("label", "⚠1");
  });

  test("clicking a block summary collapses the children via <details>", async ({ page }) => {
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: { ...MOCK_TEST_STATUS, testId: "test-blocks-001" },
      logEntries: MOCK_BLOCKS_WITH_STATUS,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent("test-blocks-001")}`);

    const block = page.locator('details[data-block-id="block-a"]');
    await expect(block).toBeAttached();
    expect(await block.evaluate((el) => /** @type {HTMLDetailsElement} */ (el).open)).toBe(true);

    await block.locator("summary.startBlock").click();

    await expect
      .poll(async () => block.evaluate((el) => /** @type {HTMLDetailsElement} */ (el).open))
      .toBe(false);
  });

  test("failure-summary jump-link opens a collapsed block and scrolls the entry into view", async ({
    page,
  }) => {
    // Block-aware failed test info: the failure entry's _id (blk-b-2)
    // matches a child of <details data-block-id="block-b">. The bootstrap's
    // document-level cts-scroll-to-entry handler must walk up to the
    // <details> ancestor and set open=true before scrolling.
    const failedTestInfo = {
      ...MOCK_TEST_FAILED,
      testId: "test-blocks-001",
      results: [
        {
          _id: "blk-b-2",
          result: "FAILURE",
          src: "ValidateIdToken",
          msg: "Signature validation failed",
        },
      ],
    };

    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: failedTestInfo,
      logEntries: MOCK_BLOCKS_WITH_STATUS,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent("test-blocks-001")}`);

    const block = page.locator('details[data-block-id="block-b"]');
    await expect(block).toBeAttached();

    // Pre-condition: collapse block B so the jump-link has work to do.
    await block.locator("summary.startBlock").click();
    await expect
      .poll(async () => block.evaluate((el) => /** @type {HTMLDetailsElement} */ (el).open))
      .toBe(false);

    // Click the matching failure-summary item — the visible instance at the
    // current viewport (the page-level #ctsTopFailureSummary one for the
    // default 1280x720 viewport falls under desktop, so click the in-header
    // one). Use first() to avoid ambiguity between the two render-twice-
    // hide-one positions.
    const link = page.locator(`.failureSummary .failureText[data-entry-id="blk-b-2"]`).first();
    await expect(link).toBeAttached();
    await link.click();

    // Block B re-opens.
    await expect
      .poll(async () => block.evaluate((el) => /** @type {HTMLDetailsElement} */ (el).open))
      .toBe(true);

    // Target entry is now visible (not display:none under collapsed parent).
    const entry = page.locator(`cts-log-entry[data-entry-id="blk-b-2"]`);
    await expect(entry).toBeVisible();
  });

  test("U6: chip click copies the deep URL; right-click copies plain LOG-NNNN", async ({
    page,
  }) => {
    // Spy on navigator.clipboard.writeText so the test does not need
    // real OS-level clipboard permissions inside Playwright's chromium.
    await page.addInitScript(() => {
      window.__copiedText = null;
      const original = navigator.clipboard?.writeText?.bind(navigator.clipboard);
      if (navigator.clipboard) {
        navigator.clipboard.writeText = (text) => {
          window.__copiedText = text;
          if (original) {
            try {
              return original(text);
            } catch {
              return Promise.resolve();
            }
          }
          return Promise.resolve();
        };
      }
    });

    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}`);

    // Wait for the entries to render with their reference chips.
    const chip = page.locator('cts-log-entry [data-testid="log-entry-id-chip"]').first();
    await expect(chip).toBeAttached();
    // The chip's label is the canonical LOG-NNNN reference.
    await expect(chip).toContainText(/LOG-\d{4}/);

    // Left-click → deep URL on the clipboard.
    await chip.click();
    await expect
      .poll(async () => page.evaluate(() => window.__copiedText), { timeout: 2000 })
      .toMatch(/log-detail\.html\?log=.+#LOG-\d{4}$/);

    const urlCopied = await page.evaluate(() => window.__copiedText);
    expect(urlCopied).toContain(`log=${MOCK_TEST_STATUS.testId}`);

    // Right-click → plain LOG-NNNN on the clipboard.
    await page.evaluate(() => {
      window.__copiedText = null;
    });
    await chip.click({ button: "right" });
    await expect
      .poll(async () => page.evaluate(() => window.__copiedText), { timeout: 2000 })
      .toMatch(/^LOG-\d{4}$/);
  });

  test("U6: deep-URL hash scrolls the targeted entry into view below the sticky bar", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    // Navigate directly with the hash already on the URL so the viewer
    // reads window.location.hash inside its first-fetch finally block.
    await page.goto(
      `/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}#LOG-0005`,
    );

    // The targeted entry must be visible (not still hidden under chrome).
    const target = page.locator("#LOG-0005");
    await expect(target).toBeAttached();
    await expect(target).toBeVisible();

    // The status bar publishes its measured height to documentElement;
    // the entry's scroll-margin-top consumes it. After the scroll lands,
    // the entry's top must sit below the status bar's bottom.
    const targetTop = await target.evaluate((el) => el.getBoundingClientRect().top);
    const barBottom = await page.evaluate(() => {
      const bar = document.getElementById("ctsLogStatusBar");
      if (!bar) return 0;
      return bar.getBoundingClientRect().bottom;
    });
    // Allow a 1 px float-rounding fudge.
    expect(targetTop).toBeGreaterThanOrEqual(Math.floor(barBottom) - 1);
  });

  test("U6: out-of-range hash loads the page without errors", async ({ page }) => {
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: MOCK_TEST_STATUS,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    /** @type {string[]} */
    const errors = [];
    page.on("pageerror", (err) => errors.push(err.message));

    await page.goto(
      `/log-detail.html?log=${encodeURIComponent(MOCK_TEST_STATUS.testId)}#LOG-9999`,
    );
    // First entry still renders normally; the page recovers gracefully.
    await expect(page.locator("cts-log-entry").first()).toBeAttached();
    expect(errors).toHaveLength(0);
  });

  test("U6: hash navigation opens a collapsed <details> ancestor before scrolling", async ({
    page,
  }) => {
    // MOCK_BLOCKS_WITH_STATUS has block-b children including blk-b-2.
    // We render that fixture, then navigate to the matching reference id
    // (which the viewer assigns in chronological order). The viewer
    // scroll handler walks up to <details data-block-id="block-b">,
    // sets open=true, and scrolls the entry into view.
    const blockTestId = "test-blocks-001";
    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: { ...MOCK_TEST_STATUS, testId: blockTestId, planId: undefined },
      logEntries: MOCK_BLOCKS_WITH_STATUS,
    });
    await setupCommonRoutes(page);

    // Pre-load to discover the reference id assigned to blk-b-2 — the
    // ordinal depends on the fixture order, and computing it inline keeps
    // the test resilient if the fixture is later reshuffled.
    await page.goto(`/log-detail.html?log=${encodeURIComponent(blockTestId)}`);
    const referenceId = await page.evaluate(() => {
      const target = document.querySelector('cts-log-entry[data-entry-id="blk-b-2"]');
      return target ? target.id : "";
    });
    expect(referenceId).toMatch(/^LOG-\d{4}$/);

    // Now collapse block-b so the hash navigation has work to do.
    const block = page.locator('details[data-block-id="block-b"]');
    await block.locator("summary.startBlock").click();
    await expect
      .poll(async () => block.evaluate((el) => /** @type {HTMLDetailsElement} */ (el).open))
      .toBe(false);

    // Same-page navigation: change only the hash. The viewer's scroll
    // handler doesn't re-run on hash-only changes, so we drive the same
    // logic through a simulated scroll-to-id call instead.
    await page.evaluate((refId) => {
      const target = document.getElementById(refId);
      if (!target) return;
      let parent = target.parentElement;
      while (parent) {
        if (parent.tagName === "DETAILS") {
          /** @type {HTMLDetailsElement} */ (parent).open = true;
        }
        parent = parent.parentElement;
      }
      target.scrollIntoView({ behavior: "auto", block: "start" });
    }, referenceId);

    await expect
      .poll(async () => block.evaluate((el) => /** @type {HTMLDetailsElement} */ (el).open))
      .toBe(true);

    const entry = page.locator(`cts-log-entry[data-entry-id="blk-b-2"]`);
    await expect(entry).toBeVisible();
  });

  test("U6: failure-summary chip click copies the same deep URL contract", async ({ page }) => {
    await page.addInitScript(() => {
      window.__copiedText = null;
      const original = navigator.clipboard?.writeText?.bind(navigator.clipboard);
      if (navigator.clipboard) {
        navigator.clipboard.writeText = (text) => {
          window.__copiedText = text;
          if (original) {
            try {
              return original(text);
            } catch {
              return Promise.resolve();
            }
          }
          return Promise.resolve();
        };
      }
    });

    // Use entry-3 from MOCK_LOG_ENTRIES so the failure summary chip and
    // the entry chip resolve to the same LOG-NNNN.
    const failedTestInfo = {
      ...MOCK_TEST_FAILED,
      results: [
        {
          _id: "entry-3",
          result: "FAILURE",
          src: "ValidateIdToken",
          msg: "Signature invalid",
        },
      ],
    };

    await setupFailFast(page);
    await setupV2Routes(page, {
      testInfo: failedTestInfo,
      logEntries: MOCK_LOG_ENTRIES,
    });
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(failedTestInfo.testId)}`);

    // The failure summary renders a chip alongside its severity badge.
    const chip = page.locator('.failureSummary [data-testid="log-entry-id-chip"]').first();
    await expect(chip).toBeAttached();
    await expect(chip).toBeVisible();

    await chip.click();
    await expect
      .poll(async () => page.evaluate(() => window.__copiedText), { timeout: 2000 })
      .toMatch(/log-detail\.html\?log=.+#LOG-\d{4}$/);

    const urlCopied = await page.evaluate(() => window.__copiedText);
    expect(urlCopied).toContain(`log=${failedTestInfo.testId}`);
  });

  test("polling-driven badge updates as new entries arrive", async ({ page }) => {
    // Custom /api/log route: first call returns batch 1 (✓2); subsequent
    // calls return batch 2 (the third success + failure). The viewer
    // polls every POLL_INTERVAL_MS (3s) — we don't override that here;
    // expect.poll handles waiting for the next cycle.
    //
    // Route registration order: setupFailFast() must come FIRST because
    // Playwright matches routes in reverse registration order. Our
    // specific routes register after, so they take priority over the
    // catch-all unmocked-call recorder.
    await setupFailFast(page);

    let callCount = 0;
    const testIdLocal = "test-poll-001";
    await page.route(`**/api/log/${testIdLocal}**`, (route) => {
      callCount += 1;
      const body = callCount === 1 ? MOCK_BLOCKS_POLL_FIRST : MOCK_BLOCKS_POLL_SECOND;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(body),
      });
    });
    await page.route(`**/api/info/${testIdLocal}*`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_TEST_STATUS, testId: testIdLocal, planId: undefined }),
      }),
    );
    await page.route(`**/api/runner/${testIdLocal}`, (route) =>
      route.fulfill({ status: 404, body: "" }),
    );
    await page.route("**/api/uploaded-images*", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
    );
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(testIdLocal)}`);

    const block = page.locator('details[data-block-id="block-poll"]');
    const badges = block.locator(".startBlockCounts cts-badge");

    // Initial state: ✓2.
    await expect(badges).toHaveCount(1);
    await expect(badges.first()).toHaveAttribute("label", "✓2");

    // After the second poll fires, the cluster should transition to ✓3 ✗1.
    // Default poll interval is 3s; allow up to 8s for the next cycle plus
    // re-render to land.
    await expect(badges).toHaveCount(2, { timeout: 8000 });
    await expect(badges.nth(0)).toHaveAttribute("label", "✓3");
    await expect(badges.nth(1)).toHaveAttribute("label", "✗1");
  });

  // ──────────── U1: DC API handler + archived banner parity ────────────
  // Mirrors the legacy DC handler at log-detail.html:1491–1538 and the
  // archived banner trigger at log-detail.html:1662–1664. Wire format is
  // frozen — Java parses it structurally in
  // src/main/java/.../ExtractBrowserApiResponse.java and
  // ExtractVP1FinalBrowserApiResponse.java. Schema lives in
  // js/log-detail.js handleVisitBrowserApi.

  test("DC API: success POSTs {data, protocol} to submitUrl", async ({ page }) => {
    await setupFailFast(page);
    const testIdLocal = MOCK_TEST_RUNNING.testId;
    const submitUrl = `https://example.com/api/dc-callback/${testIdLocal}`;
    const browserApiRequest = { providers: [{ protocol: "openid4vp", request: "abc" }] };

    await page.route(`**/api/info/${testIdLocal}*`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_TEST_RUNNING, planId: undefined }),
      }),
    );
    await page.route(`**/api/log/${testIdLocal}**`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_LOG_ENTRIES),
      }),
    );
    await page.route(`**/api/runner/${testIdLocal}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          status: "WAITING",
          browser: { browserApiRequests: [{ request: browserApiRequest, submitUrl }] },
        }),
      }),
    );
    await page.route("**/api/uploaded-images*", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
    );

    /** @type {{ url: string, body: string }[]} */
    const captured = [];
    await page.route(submitUrl, async (route) => {
      const req = route.request();
      captured.push({ url: req.url(), body: req.postData() || "" });
      await route.fulfill({ status: 200, body: "" });
    });

    // Stub navigator.credentials.get BEFORE the page loads so the v2
    // bootstrap and the click handler both see the mock. Use init script
    // so it runs in every document context. Override via Object.defineProperty
    // because navigator.credentials is a non-writable accessor in Chromium.
    await page.addInitScript(() => {
      class DigitalCredential {
        constructor(data, protocol) {
          /** @type {any} */ (this).data = data;
          /** @type {any} */ (this).protocol = protocol;
        }
      }
      Object.defineProperty(navigator, "credentials", {
        configurable: true,
        value: {
          get: async () => new DigitalCredential("ABC123", "openid4vp"),
        },
      });
    });

    await setupCommonRoutes(page);
    await page.goto(`/log-detail.html?log=${encodeURIComponent(testIdLocal)}`);

    // Wait for the running-test browser slot to render the DC button and click it.
    const apiBtn = page.locator(".visitBrowserApiBtn button").first();
    await expect(apiBtn).toBeVisible();
    await apiBtn.click();

    await expect.poll(() => captured.length, { timeout: 5000 }).toBeGreaterThan(0);
    const post = JSON.parse(captured[0].body);
    expect(post).toEqual({ data: "ABC123", protocol: "openid4vp" });
  });

  test("DC API: exception POSTs {exception:{name,message}} and surfaces error chrome", async ({
    page,
  }) => {
    await setupFailFast(page);
    const testIdLocal = MOCK_TEST_RUNNING.testId;
    const submitUrl = `https://example.com/api/dc-callback/${testIdLocal}`;

    await page.route(`**/api/info/${testIdLocal}*`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_TEST_RUNNING, planId: undefined }),
      }),
    );
    await page.route(`**/api/log/${testIdLocal}**`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_LOG_ENTRIES),
      }),
    );
    await page.route(`**/api/runner/${testIdLocal}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          status: "WAITING",
          browser: { browserApiRequests: [{ request: { foo: "bar" }, submitUrl }] },
        }),
      }),
    );
    await page.route("**/api/uploaded-images*", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
    );

    /** @type {{ body: string }[]} */
    const captured = [];
    await page.route(submitUrl, async (route) => {
      captured.push({ body: route.request().postData() || "" });
      await route.fulfill({ status: 200, body: "" });
    });

    await page.addInitScript(() => {
      Object.defineProperty(navigator, "credentials", {
        configurable: true,
        value: {
          get: async () => {
            const err = new Error("user dismissed");
            err.name = "NotAllowedError";
            throw err;
          },
        },
      });
    });

    await setupCommonRoutes(page);
    await page.goto(`/log-detail.html?log=${encodeURIComponent(testIdLocal)}`);

    const apiBtn = page.locator(".visitBrowserApiBtn button").first();
    await expect(apiBtn).toBeVisible();
    await apiBtn.click();

    await expect.poll(() => captured.length, { timeout: 5000 }).toBeGreaterThan(0);
    const post = JSON.parse(captured[0].body);
    expect(post).toEqual({ exception: { name: "NotAllowedError", message: "user dismissed" } });

    // Error chrome is the v2 page's #errorModal (showError), not window.alert.
    await expect(page.locator("#errorModal")).toBeVisible();
    await expect(page.locator("#errorMessage")).toContainText("user dismissed");
  });

  test("DC API: non-DigitalCredential response POSTs {bad_response_type}", async ({ page }) => {
    await setupFailFast(page);
    const testIdLocal = MOCK_TEST_RUNNING.testId;
    const submitUrl = `https://example.com/api/dc-callback/${testIdLocal}`;

    await page.route(`**/api/info/${testIdLocal}*`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_TEST_RUNNING, planId: undefined }),
      }),
    );
    await page.route(`**/api/log/${testIdLocal}**`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_LOG_ENTRIES),
      }),
    );
    await page.route(`**/api/runner/${testIdLocal}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          status: "WAITING",
          browser: { browserApiRequests: [{ request: { foo: "bar" }, submitUrl }] },
        }),
      }),
    );
    await page.route("**/api/uploaded-images*", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
    );

    /** @type {{ body: string }[]} */
    const captured = [];
    await page.route(submitUrl, async (route) => {
      captured.push({ body: route.request().postData() || "" });
      await route.fulfill({ status: 200, body: "" });
    });

    await page.addInitScript(() => {
      class PasswordCredential {}
      Object.defineProperty(navigator, "credentials", {
        configurable: true,
        value: {
          get: async () => new PasswordCredential(),
        },
      });
    });

    await setupCommonRoutes(page);
    await page.goto(`/log-detail.html?log=${encodeURIComponent(testIdLocal)}`);

    const apiBtn = page.locator(".visitBrowserApiBtn button").first();
    await expect(apiBtn).toBeVisible();
    await apiBtn.click();

    await expect.poll(() => captured.length, { timeout: 5000 }).toBeGreaterThan(0);
    const post = JSON.parse(captured[0].body);
    expect(post).toEqual({ bad_response_type: "PasswordCredential" });
  });

  test("INTERRUPTED runner error renders danger alert with stacktrace toggle", async ({
    page,
  }) => {
    await setupFailFast(page);
    const testIdLocal = MOCK_TEST_RUNNING.testId;

    await page.route(`**/api/info/${testIdLocal}*`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ...MOCK_TEST_RUNNING,
          status: "INTERRUPTED",
          result: null,
          planId: undefined,
        }),
      }),
    );
    await page.route(`**/api/log/${testIdLocal}**`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_LOG_ENTRIES),
      }),
    );
    await page.route(`**/api/runner/${testIdLocal}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          status: "INTERRUPTED",
          error: {
            error: "Something exploded",
            error_class: "RuntimeException",
            stacktrace: [
              "at net.openid.ExampleCondition.evaluate(ExampleCondition.java:42)",
              "at net.openid.TestRunner.run(TestRunner.java:99)",
            ],
            cause_stacktrace: ["at net.openid.Inner.cause(Inner.java:7)"],
          },
        }),
      }),
    );
    await page.route("**/api/uploaded-images*", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
    );
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(testIdLocal)}`);

    const errorSlot = page.locator('[data-testid="running-error-slot"]');
    const alert = errorSlot.locator('cts-alert[variant="danger"]');
    await expect(alert).toBeVisible();
    await expect(alert).toContainText("Something exploded");
    await expect(alert).toContainText("RuntimeException");

    const stacktrace = page.locator("#stacktrace");
    const causeStacktrace = page.locator("#causeStacktrace");
    await expect(stacktrace).toBeHidden();
    await expect(causeStacktrace).toBeHidden();

    const stacktraceBtn = page.locator("#stacktraceBtn");
    await expect(stacktraceBtn).toBeVisible();
    await stacktraceBtn.locator("button").click();

    await expect(stacktraceBtn).toBeHidden();
    await expect(stacktrace).toHaveClass(/show/);
    await expect(causeStacktrace).toHaveClass(/show/);
    await expect(stacktrace).toContainText("ExampleCondition.evaluate");
    await expect(causeStacktrace).toContainText("Inner.cause");
  });

  test("archived banner appears when /api/runner returns 404 for a once-running test", async ({
    page,
  }) => {
    await setupFailFast(page);
    const testIdLocal = MOCK_TEST_RUNNING.testId;

    await page.route(`**/api/info/${testIdLocal}*`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_TEST_RUNNING, planId: undefined }),
      }),
    );
    await page.route(`**/api/log/${testIdLocal}**`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_LOG_ENTRIES),
      }),
    );
    // Runner reports the test is gone — mirrors the legacy `.catch` path.
    await page.route(`**/api/runner/${testIdLocal}`, (route) =>
      route.fulfill({ status: 404, body: "" }),
    );
    await page.route("**/api/uploaded-images*", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
    );
    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${encodeURIComponent(testIdLocal)}`);

    const banner = page.locator('[data-testid="archived-banner"]');
    await expect(banner).toBeVisible({ timeout: 8000 });
    // The /api/info mock above returns MOCK_TEST_RUNNING (status: "RUNNING").
    // R22's banner falls through to the default branch — neither FINISHED
    // nor INTERRUPTED — so the lead reads "This test is no longer active."
    // semantically correct: the runner is gone, but we don't have a
    // definitive terminal status to claim "completed" or "interrupted".
    await expect(banner).toContainText("This test is no longer active.");
    await expect(banner).toContainText("This log has been archived");
  });
});
