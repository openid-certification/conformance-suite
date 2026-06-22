import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import {
  MOCK_PLAN_DETAIL,
  MOCK_PLAN_DETAIL_LONG_VARIANT,
  MOCK_TEST_STATUS,
} from "./fixtures/mock-test-data.js";
import { MOCK_ADMIN_USER } from "./fixtures/mock-users.js";

test.describe("plan-detail.html — Plan Detail", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("loads and renders plan info with modules (R28)", async ({ page }) => {
    await setupFailFast(page);

    // /api/plan/:planId
    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    // /api/info/:testId for each module with instances
    await setupTestInfoRoute(page, {
      "test-inst-001": { ...MOCK_TEST_STATUS, testId: "test-inst-001" },
      "test-inst-002": {
        ...MOCK_TEST_STATUS,
        testId: "test-inst-002",
        testName: "oidcc-server-rotate-keys",
      },
      "test-inst-003": {
        ...MOCK_TEST_STATUS,
        testId: "test-inst-003",
        testName: "oidcc-ensure-redirect-uri-in-authorization-request",
      },
    });

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Plan header renders (plan-detail.html hosts cts-plan-header at id="planDetailHeader")
    const header = page.locator("#planDetailHeader");
    await expect(header).toContainText("oidcc-basic-certification-test-plan");
    await expect(header).toContainText("plan-abc-123");
    await expect(header).toContainText("client_secret_basic");

    // Alias row surfaces the user-set config.alias next to the other metadata.
    await expect(header.locator('[data-testid="alias-row"]')).toHaveCount(1);
    await expect(header).toContainText("oidcc-basic-run-1");

    // Module list renders (4 modules in MOCK_PLAN_DETAIL).
    // cts-plan-modules exposes id="planItems" with .module-row children.
    const moduleRows = page.locator("#planItems .module-row");
    await expect(moduleRows).toHaveCount(4);

    // Modules show their test names
    await expect(moduleRows.nth(0)).toContainText("oidcc-server");
    await expect(moduleRows.nth(1)).toContainText("oidcc-server-rotate-keys");

    // View configuration action button visible (rendered by cts-plan-actions)
    await expect(page.locator('[data-testid="view-config-btn"]')).toBeVisible();
  });

  test("omits the Alias row when the plan has no config (e.g. public view)", async ({ page }) => {
    // The public projection (PublicPlan) drops `config` entirely, and
    // dynamic-registration plans may leave the alias blank. In both cases the
    // header must suppress the Alias row rather than render an empty cell —
    // and the component's optional chaining must not throw on a missing config.
    await setupFailFast(page);

    const planWithoutConfig = { ...MOCK_PLAN_DETAIL, config: undefined };
    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(planWithoutConfig),
      }),
    );

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    const header = page.locator("#planDetailHeader");
    // Header still renders (plan name proves the page mounted without error)…
    await expect(header).toContainText("oidcc-basic-certification-test-plan");
    // …but the Alias row is absent.
    await expect(header.locator('[data-testid="alias-row"]')).toHaveCount(0);
  });

  test("initial load shows an in-page loader, not a blocking modal overlay", async ({ page }) => {
    // Regression guard: plan-detail used to open #loadingModal (a full-screen
    // modal + dimmed/blurred backdrop) on initial load via FAPI_UI.showBusy().
    // It now renders an in-page cts-loading-state instead, matching
    // log-detail.html. The /api/plan response is gated behind a promise the
    // test releases explicitly, so the loading-window assertions are
    // race-free — the fetch cannot settle until releasePlan() fires.
    await setupFailFast(page);

    /** @type {(value?: unknown) => void} */
    let releasePlan = () => {};
    const planGate = new Promise((resolve) => {
      releasePlan = resolve;
    });

    await page.route("**/api/plan/plan-abc-123", async (route) => {
      await planGate;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      });
    });

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // While the plan fetch is in flight: in-page loader visible, the grid
    // hidden behind it, and crucially NO modal overlay.
    const loader = page.locator("cts-loading-state#planDetailLoading");
    await expect(loader).toBeVisible();
    await expect(page.locator("#planDetailGrid")).toBeHidden();
    await expect(page.locator("#loadingModal")).toBeHidden();

    // Let the fetch settle.
    releasePlan();

    // After the load settles: loader removed, grid + header visible with the
    // plan name, and the modal overlay still never shown.
    const header = page.locator("#planDetailHeader");
    await expect(header).toContainText("oidcc-basic-certification-test-plan");
    await expect(page.locator("#planDetailGrid")).toBeVisible();
    await expect(loader).toHaveCount(0);
    await expect(page.locator("#loadingModal")).toBeHidden();
  });

  test("failed initial load still removes the loader and reveals the page under the error modal", async ({
    page,
  }) => {
    // The .finally() reveal fires on both success and error. This locks the
    // on-error contract: a getPlan() rejection must still drop the in-page
    // loader and un-hide the grid (surfacing the error via #errorModal),
    // never strand a stuck loader. Guards a future edit that mistakenly moves
    // the reveal into a .then() instead of .finally().
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ error: "boom" }),
      }),
    );

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Error surfaces via the error modal, and the load indicator is cleared.
    await expect(page.locator("#errorModal")).toBeVisible();
    await expect(page.locator("cts-loading-state#planDetailLoading")).toHaveCount(0);
    await expect(page.locator("#planDetailGrid")).toBeVisible();
  });

  test("View configuration button opens a modal with plan configuration JSON", async ({ page }) => {
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

    // Cross-page contract: every wired page mounts a single <cts-toast-host>
    // for window.ctsToast(...). A silent removal of the mount from plan-detail.html
    // would otherwise pass all tests in this file. (Mirrors upload.spec.js:210.)
    await expect(page.locator("cts-toast-host")).toHaveCount(1);

    // Wait for the View configuration button to render
    const configBtn = page.locator('[data-testid="view-config-btn"]');
    await expect(configBtn).toBeVisible();

    // Config modal should not be visible initially
    await expect(page.locator('[data-testid="config-modal"]')).not.toBeVisible();

    // Click the inner <button> inside cts-button (Lit binds @click on the inner button)
    await configBtn.locator("button").click();

    // Modal appears with the plan ID and the config JSON inside the
    // read-only Monaco editor. Monaco virtualises rendered content, so we
    // assert on the editor's `.value` rather than the modal's textContent.
    const configPanel = page.locator('[data-testid="config-modal"]');
    await expect(configPanel).toBeVisible();
    await expect(configPanel).toContainText("plan-abc-123");
    await expect
      .poll(
        () =>
          page.evaluate(() => {
            const el = /** @type {any} */ (document.querySelector("cts-json-editor.config-json"));
            return el ? el.value : "";
          }),
        { timeout: 10000 },
      )
      .toContain("server.issuer");
    // Both substrings are free reads off the same `.value` string;
    // preserving the pre-swap assertion (testing-reviewer T4) keeps the
    // JSON content check honest rather than relying on key existence alone.
    const configValue = await page.evaluate(() => {
      const el = /** @type {any} */ (document.querySelector("cts-json-editor.config-json"));
      return el ? el.value : "";
    });
    expect(configValue).toContain("op.example.com");
  });

  test("module status badges render after /api/info fetch", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await setupTestInfoRoute(page, {
      "test-inst-001": {
        ...MOCK_TEST_STATUS,
        testId: "test-inst-001",
        status: "FINISHED",
        result: "PASSED",
      },
      "test-inst-002": {
        ...MOCK_TEST_STATUS,
        testId: "test-inst-002",
        status: "FINISHED",
        result: "WARNING",
      },
      "test-inst-003": {
        ...MOCK_TEST_STATUS,
        testId: "test-inst-003",
        // A failed test is reported as INTERRUPTED+FAILED, not FINISHED+FAILED
        // (it never reaches FINISHED). The row badge and the status-bar segment
        // must still render the FAILED verdict in red (GitLab #1858/#1859).
        status: "INTERRUPTED",
        result: "FAILED",
      },
    });

    // R28 deep-link follow-on: every FAILED row now triggers a
    // /api/log/{id} fetch from plan-detail.html. The expectNoUnmockedCalls
    // afterEach hook would trip every plan-detail test that uses a FAILED
    // /api/info fixture, so register a permissive log mock here too.
    await page.route("**/api/log/test-inst-003*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          { _id: "entry-1", result: "INFO", time: 1 },
          { _id: "entry-2", result: "FAILURE", time: 2 },
        ]),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Wait for module rows to render
    const firstRow = page.locator("#planItems .module-row").first();
    await expect(firstRow).toBeVisible();

    // Wait for /api/info to merge status into the first row's badge.
    // cts-plan-modules renders a cts-badge per row whose label reflects
    // the result text (PASSED / WARNING / FAILED / PENDING) and whose
    // variant maps onto the canonical cts-badge status palette.
    await expect(firstRow.locator("cts-badge")).toHaveAttribute("label", "PASSED");
    await expect(firstRow.locator("cts-badge")).toHaveAttribute("variant", "pass");

    // R28: the badge is wrapped in a link to that test's log page when
    // the module has an instance. Non-FAILED row → no fragment.
    const statusLink = firstRow.locator('[data-testid="module-status-link"]');
    await expect(statusLink).toHaveAttribute("href", "log-detail.html?log=test-inst-001");

    // The module name links to the same log-detail URL as the status
    // badge and the "View Logs" button.
    const nameLink = firstRow.locator('[data-testid="module-name-link"]');
    await expect(nameLink).toHaveAttribute("href", "log-detail.html?log=test-inst-001");
    await expect(nameLink).toHaveText("oidcc-server");

    // R28 deep-link follow-on: the FAILED row's lozenge resolves the
    // first FAILURE entry's LOG-NNNN ordinal and appends it as a
    // fragment, so a click lands on the failure entry rather than the
    // top of the log. The mocked /api/log/test-inst-003 returns the
    // FAILURE at index 1 (ordinal 2) → LOG-0002. The aria-label
    // switches to the "Jump to first failure" form (R7).
    const failedRow = page.locator("#planItems .module-row").nth(2);
    const failedLink = failedRow.locator('[data-testid="module-status-link"]');
    await expect(failedLink).toHaveAttribute("href", "log-detail.html?log=test-inst-003#LOG-0002");
    await expect(failedLink).toHaveAttribute(
      "aria-label",
      "Jump to first failure in logs for oidcc-ensure-redirect-uri-in-authorization-request",
    );

    // GitLab #1859: the FAILED row's status badge reads "FAILED" (not the
    // lifecycle status "INTERRUPTED") on the fail palette — the verdict wins
    // over the status.
    await expect(failedRow.locator("cts-badge")).toHaveAttribute("label", "FAILED");
    await expect(failedRow.locator("cts-badge")).toHaveAttribute("variant", "fail");

    // GitLab #1858: the whole-plan status bar paints the failed module's
    // segment red. This is the regression that shipped because the segment
    // colour gated on status === "FINISHED" and an INTERRUPTED+FAILED test
    // fell through to the neutral skip grey.
    const failedSegment = page
      .locator('#planDetailStatus [data-testid="plan-status-segment"]')
      .nth(2);
    await expect(failedSegment).toHaveClass(/cts-pst-seg--fail/);

    // Each module name with a testSummary is wrapped in a cts-tooltip
    // whose content attribute carries the summary. Hovering the help-icon
    // mounts a positioned .oidf-tooltip in document.body. We assert the
    // attribute carries the right text and that the tooltip pops on hover —
    // that exercises the full wrap (no stale `title=` regression).
    const helpTooltip = firstRow.locator("cts-tooltip.help");
    await expect(helpTooltip).toHaveAttribute("content", /Verify basic OpenID Connect/);
    const helpIcon = firstRow.locator(".help-icon");
    await expect(helpIcon).not.toHaveAttribute("title", /.+/);
    await helpIcon.hover();
    const popover = page.locator(".oidf-tooltip");
    await expect(popover).toContainText(/Verify basic OpenID Connect/);
  });

  test("whole-plan status overview resolves, settles 404s, and segment click flashes the row (R8/R11/R18)", async ({
    page,
  }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    // 001 → PASSED, 002 → WARNING, 003 → 404 (inaccessible run). The never-run
    // 4th module has no instance, so no /api/info fetch fires for it.
    await page.route("**/api/info/test-inst-001*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_TEST_STATUS, status: "FINISHED", result: "PASSED" }),
      }),
    );
    await page.route("**/api/info/test-inst-002*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_TEST_STATUS, status: "FINISHED", result: "WARNING" }),
      }),
    );
    await page.route("**/api/info/test-inst-003*", (route) =>
      route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
    );

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    const segments = page.locator('#planDetailStatus [data-testid="plan-status-segment"]');
    await expect(segments).toHaveCount(4);

    // Segments resolve to their colours. The 404 settles to a STATIC skip
    // (never --pending) — proving _statusResolved is set in the catch branch
    // (R18), not just on success; the never-run module is skip too.
    await expect(segments.nth(0)).toHaveClass(/cts-pst-seg--pass/);
    await expect(segments.nth(1)).toHaveClass(/cts-pst-seg--warn/);
    await expect(segments.nth(2)).toHaveClass(/cts-pst-seg--skip/);
    await expect(segments.nth(2)).not.toHaveClass(/cts-pst-seg--pending/);
    await expect(segments.nth(3)).toHaveClass(/cts-pst-seg--skip/);

    // Detail mode shows the merged count-badge filter (R9) — a "Passed" pill.
    await expect(
      page.locator('#planDetailStatus [data-testid="plan-status-filter"]'),
    ).toContainText("Passed");

    // A segment is an in-page anchor: clicking it sets the URL hash to the
    // module's row, the row gets the persistent :target highlight, and a flash
    // fires on arrival (note 5).
    await expect(segments.nth(0)).toHaveJSProperty("tagName", "A");
    await segments.nth(0).click();
    await expect(page).toHaveURL(/#cts-module-0$/);
    const firstRow = page.locator("#planItems .module-row").nth(0);
    await expect(firstRow).toHaveAttribute("id", "cts-module-0");
    await expect(firstRow).toHaveClass(/is-flash/);
  });

  test("result filter narrows rows, dims segments, and a dimmed-segment click clears it (R9/R10/R11)", async ({
    page,
  }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );
    await page.route("**/api/info/test-inst-001*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_TEST_STATUS, status: "FINISHED", result: "PASSED" }),
      }),
    );
    await page.route("**/api/info/test-inst-002*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_TEST_STATUS, status: "FINISHED", result: "WARNING" }),
      }),
    );
    await page.route("**/api/info/test-inst-003*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_TEST_STATUS, status: "INTERRUPTED", result: "FAILED" }),
      }),
    );
    // The FAILED row triggers an R28 /api/log deep-link fetch.
    await page.route("**/api/log/test-inst-003*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([{ _id: "e1", result: "FAILURE", time: 1 }]),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    const segments = page.locator('#planDetailStatus [data-testid="plan-status-segment"]');
    await expect(segments.nth(2)).toHaveClass(/cts-pst-seg--fail/);

    // Click the FAILED count badge (the merged summary + filter, R9).
    await page.locator('#planDetailStatus cts-badge[data-result="FAILED"]').click();

    // Rows narrow to the single FAILED module; segments dim except the FAILED
    // one; the FAILED badge presses and a Clear-filters button appears.
    const rows = page.locator("#planItems .module-row");
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText("oidcc-ensure-redirect-uri-in-authorization-request");
    await expect(segments.nth(2)).not.toHaveClass(/is-dimmed/);
    await expect(segments.nth(0)).toHaveClass(/is-dimmed/);
    await expect(page.locator('#planDetailStatus cts-badge[data-result="FAILED"]')).toHaveAttribute(
      "pressed",
      "",
    );
    await expect(page.locator('[data-testid="plan-status-filter-clear"]')).toBeVisible();

    // Click a DIMMED segment (the passed one). R11: the coordinator clears the
    // filter first so the row is visible, then flashes it.
    await segments.nth(0).click();

    await expect(rows).toHaveCount(4);
    await expect(page.locator('[data-testid="plan-status-filter-clear"]')).toHaveCount(0);
    await expect(page.locator('#planItems .module-row[data-module-index="0"]')).toHaveClass(
      /is-flash/,
    );
  });

  test("delete plan button reveals an inline delete-confirmation panel", async ({ page }) => {
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

    // Delete button visible (plan is mutable, not readonly)
    const deleteBtn = page.locator('[data-testid="delete-plan-btn"]');
    await expect(deleteBtn).toBeVisible();

    // Confirm panel hidden initially
    await expect(page.locator('[data-testid="delete-confirm-panel"]')).toHaveCount(0);

    // Click delete → confirmation panel appears
    await deleteBtn.locator("button").click();
    const panel = page.locator('[data-testid="delete-confirm-panel"]');
    await expect(panel).toBeVisible();
    await expect(panel).toContainText("permanently and irrevocably");

    // Cancel → panel disappears, no DELETE call made
    await panel.getByRole("button", { name: "Cancel" }).click();
    await expect(panel).toHaveCount(0);
  });

  test("publish button opens confirmation modal with secrets warning (R1)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Publish button visible for admin user on unpublished plan
    const publishBtn = page.locator('[data-testid="publish-everything-btn"]');
    await expect(publishBtn).toBeVisible();

    // Modal hidden initially
    const publishModal = page.locator("#publishModal");
    await expect(publishModal).toBeHidden();

    // Click publish → modal opens with secrets warning
    await publishBtn.locator("button").click();
    await expect(publishModal).toBeVisible();
    await expect(publishModal).toContainText(
      "keys, secrets, and all other test information publicly visible",
    );
  });

  test("publish confirm sends POST /api/plan/:id/publish and navigates (R2)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123?*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_PLAN_DETAIL, publish: "everything" }),
      }),
    );

    await page.route("**/api/plan/plan-abc-123", (route) => {
      if (route.request().method() === "DELETE") {
        return route.fulfill({ status: 200, body: "" });
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      });
    });

    await page.route("**/api/plan/plan-abc-123/publish", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_PLAN_DETAIL, publish: "everything" }),
      }),
    );

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Open publish modal
    await page.locator('[data-testid="publish-everything-btn"]').locator("button").click();
    await expect(page.locator("#publishModal")).toBeVisible();

    // Set up request interception BEFORE clicking
    const publishRequest = page.waitForRequest(
      (req) => req.url().includes("/api/plan/plan-abc-123/publish") && req.method() === "POST",
    );

    // Click the publish confirm button (carries data-publish="everything")
    await page.locator("#confirmPublishBtn").click();

    // Verify POST was sent with correct body
    const req = await publishRequest;
    expect(JSON.parse(req.postData() || "")).toEqual({ publish: "everything" });

    // Should navigate to public view
    await page.waitForURL("**/plan-detail.html?plan=plan-abc-123&public=true");
  });

  test("publish cancel closes modal without POST (R3)", async ({ page }) => {
    await setupFailFast(page);

    let publishPostCalled = false;

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await page.route("**/api/plan/plan-abc-123/publish", (route) => {
      publishPostCalled = true;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      });
    });

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Open modal
    await page.locator('[data-testid="publish-everything-btn"]').locator("button").click();
    await expect(page.locator("#publishModal")).toBeVisible();

    // Click Cancel (the auto-generated cancel button without data-publish)
    await page.locator("#publishModal").getByRole("button", { name: "Cancel" }).click();

    // Modal should close
    await expect(page.locator("#publishModal")).toBeHidden();

    // No POST should have been made
    expect(publishPostCalled).toBe(false);
  });

  test("delete confirm sends DELETE /api/plan/:planId (R4)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) => {
      if (route.request().method() === "DELETE") {
        return route.fulfill({ status: 200, body: "" });
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      });
    });

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Open delete confirm panel
    await page.locator('[data-testid="delete-plan-btn"]').locator("button").click();
    await expect(page.locator('[data-testid="delete-confirm-panel"]')).toBeVisible();

    // Set up request interception BEFORE clicking confirm
    const deleteRequest = page.waitForRequest(
      (req) => req.url().includes("/api/plan/plan-abc-123") && req.method() === "DELETE",
    );

    // Click the inner confirm Delete button (.confirm-delete-btn host)
    await page.locator(".confirm-delete-btn").locator("button").click();

    // Verify DELETE was sent
    const req = await deleteRequest;
    expect(req.method()).toBe("DELETE");
    expect(req.url()).toContain("/api/plan/plan-abc-123");
  });

  test("certify button opens the certification package modal (U35)", async ({ page }) => {
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

    // Modal hidden initially
    const certModal = page.locator("#certificationPackageModal");
    await expect(certModal).toBeHidden();

    // R26: the Certify button is hidden until the plan-detail page has
    // resolved /api/info for each module and confirmed at least one
    // FINISHED test with no FAILED result. The default test-info fixture
    // returns PASSED for every instance, so it appears after polling.
    const certifyBtn = page.locator('[data-testid="certify-btn"]');
    await expect(certifyBtn).toBeVisible();

    await certifyBtn.locator("button").click();

    // The certification package modal opens
    await expect(certModal).toBeVisible();
    await expect(certModal).toContainText("Prepare Certification Submission Package");
    await expect(certModal).toContainText("Create Certification Package");
  });

  test("certify button stays hidden when any module FAILED (R26)", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    // Mix of PASSED and FAILED — Publish for certification must stay hidden.
    await setupTestInfoRoute(page, {
      "test-inst-001": { ...MOCK_TEST_STATUS, testId: "test-inst-001", result: "PASSED" },
      "test-inst-002": { ...MOCK_TEST_STATUS, testId: "test-inst-002", result: "PASSED" },
      "test-inst-003": { ...MOCK_TEST_STATUS, testId: "test-inst-003", result: "FAILED" },
    });

    // R28 deep-link follow-on: the FAILED row triggers a /api/log/{id}
    // fetch from plan-detail.html. Mock it so the fail-fast catch-all
    // doesn't trip this R26 certify-gate test.
    await page.route("**/api/log/test-inst-003*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          { _id: "entry-1", result: "INFO", time: 1 },
          { _id: "entry-2", result: "FAILURE", time: 2 },
        ]),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Wait for the page to finish wiring the action rail. The Private
    // link button is always rendered in the same branch as Certify and
    // arrives first, so we use it as the readiness signal.
    await expect(page.locator('[data-testid="private-link-btn"]')).toBeVisible();

    // Positive readiness signal that the FAILED /api/info has been
    // processed: a row's badge attribute only resolves to "FAILED" after
    // that fetch settles. Asserting this *before* the negative certify-btn
    // check avoids the flaky `networkidle` waiter (which Playwright
    // discourages for polling apps) while proving the canCertify path
    // has consumed its inputs. We match by attribute rather than by row
    // index so the test stays robust as the fixture grows new modules.
    await expect(page.locator('#planItems .module-row cts-badge[label="FAILED"]')).toBeVisible();

    // No certify button — at least one FAILED result.
    await expect(page.locator('[data-testid="certify-btn"]')).toHaveCount(0);
  });

  test("R28 deep-link composes ?public=true with the #LOG fragment correctly", async ({ page }) => {
    await setupFailFast(page);

    // Public-mode plan (admin published "everything"). plan-detail
    // builds the log link with `?log={id}&public=true#LOG-NNNN` —
    // the fragment must come AFTER the query string.
    await page.route("**/api/plan/plan-abc-123*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_PLAN_DETAIL, publish: "everything" }),
      }),
    );

    await setupTestInfoRoute(page, {
      "test-inst-001": { ...MOCK_TEST_STATUS, testId: "test-inst-001", result: "PASSED" },
      "test-inst-002": { ...MOCK_TEST_STATUS, testId: "test-inst-002", result: "PASSED" },
      "test-inst-003": { ...MOCK_TEST_STATUS, testId: "test-inst-003", result: "FAILED" },
    });

    // FAILURE at index 4 (5th entry) → LOG-0005. Earlier entries cover
    // the INFO + startBlock cases that consume an ordinal in the
    // canonical site's iteration but never qualify as the "first
    // failure" themselves.
    await page.route("**/api/log/test-inst-003**", (route) => {
      // Sanity-check the request: the public flag must propagate to
      // the log fetch as well, otherwise the summary projection
      // wouldn't apply on the live server.
      expect(route.request().url()).toContain("public=true");
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          { _id: "e1", startBlock: true, time: 1 },
          { _id: "e2", result: "INFO", time: 2 },
          { _id: "e3", result: "INFO", time: 3 },
          { _id: "e4", result: "WARNING", time: 4 },
          { _id: "e5", result: "FAILURE", time: 5 },
        ]),
      });
    });

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123&public=true");

    // FAILED row's lozenge gets the fragment after the public-mode
    // query string. Composition order: ?log={id}&public=true#LOG-NNNN.
    const failedLink = page
      .locator("#planItems .module-row")
      .nth(2)
      .locator('[data-testid="module-status-link"]');
    await expect(failedLink).toHaveAttribute(
      "href",
      "log-detail.html?log=test-inst-003&public=true#LOG-0005",
    );
  });

  test("R28 deep-link falls back to top-of-log when /api/log returns 404", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await setupTestInfoRoute(page, {
      "test-inst-001": { ...MOCK_TEST_STATUS, testId: "test-inst-001", result: "PASSED" },
      "test-inst-002": { ...MOCK_TEST_STATUS, testId: "test-inst-002", result: "PASSED" },
      "test-inst-003": { ...MOCK_TEST_STATUS, testId: "test-inst-003", result: "FAILED" },
    });

    // Capture console.warn calls so we can assert the fail-soft branch
    // emitted its observability breadcrumb. Set this up BEFORE goto so
    // no warn from the page is missed.
    /** @type {string[]} */
    const warnings = [];
    page.on("console", (msg) => {
      if (msg.type() === "warning") {
        warnings.push(msg.text());
      }
    });

    await page.route("**/api/log/test-inst-003*", (route) =>
      route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
    );

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Wait until the FAILED row's badge has settled. The deep-link
    // resolution kicks off after this point, so we know any warn it
    // emits has had time to fire by the time we assert below.
    await expect(page.locator('#planItems .module-row cts-badge[label="FAILED"]')).toBeVisible();

    // Lozenge keeps the R28 top-of-log href — no `#`, no broken
    // fragment. Aria-label keeps the original "View logs ..." form so
    // SR announcement matches the actual landing.
    const failedLink = page
      .locator("#planItems .module-row")
      .nth(2)
      .locator('[data-testid="module-status-link"]');
    await expect(failedLink).toHaveAttribute("href", "log-detail.html?log=test-inst-003");
    await expect(failedLink).toHaveAttribute("aria-label", /^View logs for /);

    // No user-facing error modal — the failure is intentionally swallowed.
    await expect(page.locator("#errorModal")).toBeHidden();

    // The fail-soft branch emitted a single console.warn for
    // observability. The exact wording is brittle to lock down; the
    // test asserts it mentions the test ID so a maintainer reading
    // dev tools knows which row failed.
    await expect.poll(() => warnings.some((w) => w.includes("test-inst-003"))).toBe(true);
  });

  test("R28 deep-link falls back when no FAILURE entry exists in the log", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await setupTestInfoRoute(page, {
      "test-inst-001": { ...MOCK_TEST_STATUS, testId: "test-inst-001", result: "PASSED" },
      "test-inst-002": { ...MOCK_TEST_STATUS, testId: "test-inst-002", result: "PASSED" },
      "test-inst-003": { ...MOCK_TEST_STATUS, testId: "test-inst-003", result: "FAILED" },
    });

    // Pathological state: result-level says FAILED but the log itself
    // contains no FAILURE entry. The shim treats this the same as a
    // 404 — leave firstFailureRef undefined and warn for observability.
    await page.route("**/api/log/test-inst-003*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          { _id: "e1", result: "INFO", time: 1 },
          { _id: "e2", result: "WARNING", time: 2 },
        ]),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    await expect(page.locator('#planItems .module-row cts-badge[label="FAILED"]')).toBeVisible();

    const failedLink = page
      .locator("#planItems .module-row")
      .nth(2)
      .locator('[data-testid="module-status-link"]');
    await expect(failedLink).toHaveAttribute("href", "log-detail.html?log=test-inst-003");
  });

  test("R28 deep-link fetch fires exactly once per FAILED row", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await setupTestInfoRoute(page, {
      "test-inst-001": { ...MOCK_TEST_STATUS, testId: "test-inst-001", result: "PASSED" },
      "test-inst-002": { ...MOCK_TEST_STATUS, testId: "test-inst-002", result: "PASSED" },
      "test-inst-003": { ...MOCK_TEST_STATUS, testId: "test-inst-003", result: "FAILED" },
    });

    // Count fetches per test ID. R5 pins "fetch fires once per FAILED
    // row, never on a polling cadence" — only test-inst-003 should be
    // fetched, exactly once. The PASSED rows must not trigger a log
    // fetch at all.
    /** @type {Record<string, number>} */
    const logFetchCounts = { "test-inst-001": 0, "test-inst-002": 0, "test-inst-003": 0 };
    await page.route("**/api/log/**", (route) => {
      const url = new URL(route.request().url());
      const id = url.pathname.split("/api/log/")[1];
      if (id in logFetchCounts) logFetchCounts[id] += 1;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          { _id: "e1", result: "INFO", time: 1 },
          { _id: "e2", result: "FAILURE", time: 2 },
        ]),
      });
    });

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Wait until the deep-link has resolved (href ends with the
    // fragment) so we know the fetch lifecycle has fully settled
    // before we count.
    const failedLink = page
      .locator("#planItems .module-row")
      .nth(2)
      .locator('[data-testid="module-status-link"]');
    await expect(failedLink).toHaveAttribute("href", "log-detail.html?log=test-inst-003#LOG-0002");

    // Give any rogue polling cadence ~one full tick to misbehave. The
    // page does not currently implement /api/info polling for
    // plan-detail; this short wait is a defensive pin so a future
    // regression that introduces polling without updating the deep-link
    // logic surfaces here.
    await page.waitForTimeout(250);

    expect(logFetchCounts["test-inst-003"]).toBe(1);
    expect(logFetchCounts["test-inst-001"]).toBe(0);
    expect(logFetchCounts["test-inst-002"]).toBe(0);
  });

  test("page does not overflow and plan metadata stacks at 375px viewport", async ({ page }) => {
    // Doc-element guard, not a sub-container guard: cts-plan-header's
    // metadata <dl> kept a two-column max-content 1fr grid at every
    // width, squeezing values into a ~132px sliver of the ~312px
    // content box at phone widths. Pre-fix measurement showed no
    // page-level overflow on plan-detail (unlike log-detail's status
    // bar), so the scrollWidth guard locks that healthy state while
    // the single-track assertion locks the stacked metadata layout.
    // Plan: docs/plans/2026-06-05-004-fix-plan-header-mobile-responsive-plan.md
    await page.setViewportSize({ width: 375, height: 800 });

    await setupFailFast(page);
    await page.route("**/api/plan/plan-long-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL_LONG_VARIANT),
      }),
    );
    await setupTestInfoRoute(page, {
      "test-inst-001": { ...MOCK_TEST_STATUS, testId: "test-inst-001" },
      "test-inst-002": { ...MOCK_TEST_STATUS, testId: "test-inst-002" },
      "test-inst-003": { ...MOCK_TEST_STATUS, testId: "test-inst-003" },
    });
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-long-001");
    await expect(page.locator("cts-plan-header .planMeta")).toBeVisible();

    // Document-level horizontal overflow guard (R4).
    const doc = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    }));
    expect(doc.scrollWidth).toBeLessThanOrEqual(doc.clientWidth);

    // Stacked single-track metadata layout (R1).
    const tracks = await page
      .locator("cts-plan-header .planMeta")
      .evaluate((el) => getComputedStyle(el).gridTemplateColumns.trim().split(/\s+/));
    expect(tracks).toHaveLength(1);
  });
});

test.describe("plan-detail.html — also-required banner (R12)", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  /** The handoff record schedule-test writes after a guided Brazil-OP create. */
  const HANDOFF_RECORD = {
    planId: "plan-abc-123",
    ecosystemId: "open_finance_brazil",
    ecosystemLabel: "🇧🇷 OpenFinance Brazil",
    preset: {
      ecosystemId: "open_finance_brazil",
      answers: ["op"],
      completedPlanNames: ["fapi1-advanced-final-test-plan"],
    },
    remainingSiblings: [
      {
        id: "dcr_brazil_op",
        label: "Dynamic Client Registration",
        planName: "fapi1-advanced-final-brazil-dcr-test-plan",
      },
    ],
    completedPlanNames: ["fapi1-advanced-final-test-plan"],
  };

  /**
   * Routes + a seeded oidf-also-required record.
   *
   * @param {import('@playwright/test').Page} page
   * @param {object|null} record - Seeded sessionStorage record (null = none).
   * @param {object} [options]
   * @param {object|null} [options.user]
   */
  async function bootWithRecord(page, record, options = {}) {
    await setupFailFast(page);
    await page.route("**/api/plan/plan-abc-123*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );
    await setupTestInfoRoute(page);
    await setupCommonRoutes(page, options.user !== undefined ? { user: options.user } : {});
    if (record) {
      await page.addInitScript((rec) => {
        sessionStorage.setItem("oidf-also-required", JSON.stringify(rec));
      }, record);
    }
  }

  test("matching record → banner names the sibling, links with wizard_preset, record consumed", async ({
    page,
  }) => {
    await bootWithRecord(page, HANDOFF_RECORD);
    await page.goto("/plan-detail.html?plan=plan-abc-123");

    const banner = page.locator("#alsoRequiredBanner");
    await expect(banner).toBeVisible();
    await expect(banner).toContainText("OpenFinance Brazil");
    await expect(banner).toContainText("Dynamic Client Registration");

    const href = await banner.locator("a").getAttribute("href");
    expect(href).toContain("schedule-test.html?wizard_preset=");
    const preset = JSON.parse(decodeURIComponent(String(href).split("wizard_preset=")[1]));
    expect(preset).toEqual(HANDOFF_RECORD.preset);

    // Consumed once — the record is gone after the read.
    expect(await page.evaluate(() => sessionStorage.getItem("oidf-also-required"))).toBeNull();
  });

  test("banner is dismissible", async ({ page }) => {
    await bootWithRecord(page, HANDOFF_RECORD);
    await page.goto("/plan-detail.html?plan=plan-abc-123");

    const banner = page.locator("#alsoRequiredBanner");
    await expect(banner).toBeVisible();
    await banner.locator("button").click();
    await expect(banner).toHaveCount(0);
  });

  test("no record → no banner", async ({ page }) => {
    await bootWithRecord(page, null);
    await page.goto("/plan-detail.html?plan=plan-abc-123");
    await expect(page.locator("#planDetailHeader")).toContainText("plan-abc-123");
    await expect(page.locator("#alsoRequiredBanner")).toHaveCount(0);
  });

  test("mismatched planId → no banner", async ({ page }) => {
    await bootWithRecord(page, { ...HANDOFF_RECORD, planId: "some-other-plan" });
    await page.goto("/plan-detail.html?plan=plan-abc-123");
    await expect(page.locator("#planDetailHeader")).toContainText("plan-abc-123");
    await expect(page.locator("#alsoRequiredBanner")).toHaveCount(0);
  });

  test("public view → no banner", async ({ page }) => {
    await bootWithRecord(page, HANDOFF_RECORD);
    await page.goto("/plan-detail.html?plan=plan-abc-123&public=true");
    await expect(page.locator("#planDetailHeader")).toContainText("plan-abc-123");
    await expect(page.locator("#alsoRequiredBanner")).toHaveCount(0);
  });

  test("anonymous viewer → no banner", async ({ page }) => {
    await bootWithRecord(page, HANDOFF_RECORD, { user: null });
    await page.goto("/plan-detail.html?plan=plan-abc-123");
    await expect(page.locator("#planDetailHeader")).toContainText("plan-abc-123");
    await expect(page.locator("#alsoRequiredBanner")).toHaveCount(0);
  });
});
