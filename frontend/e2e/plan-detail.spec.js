import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import {
  MOCK_PLAN_DETAIL,
  MOCK_PLAN_DETAIL_LONG_VARIANT,
  withLatestRuns,
} from "./fixtures/mock-test-data.js";
import { MOCK_ADMIN_USER } from "./fixtures/mock-users.js";
import { MOCK_PLAN_FINDINGS } from "./fixtures/mock-plan-findings.js";

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
        body: JSON.stringify(
          withLatestRuns(MOCK_PLAN_DETAIL, {
            "test-inst-001": {},
            "test-inst-002": {
              testName: "oidcc-server-rotate-keys",
            },
            "test-inst-003": {
              testName: "oidcc-ensure-redirect-uri-in-authorization-request",
            },
          }),
        ),
      }),
    );

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
    // read-only JSON view. Assert on the view's `.value` rather than the
    // modal's textContent.
    const configPanel = page.locator('[data-testid="config-modal"]');
    await expect(configPanel).toBeVisible();
    await expect(configPanel).toContainText("plan-abc-123");
    await expect
      .poll(
        () =>
          page.evaluate(() => {
            const el = /** @type {any} */ (document.querySelector("cts-json-view.config-json"));
            return el ? el.value : "";
          }),
        { timeout: 10000 },
      )
      .toContain("server.issuer");
    // Both substrings are free reads off the same `.value` string;
    // preserving the pre-swap assertion (testing-reviewer T4) keeps the
    // JSON content check honest rather than relying on key existence alone.
    const configValue = await page.evaluate(() => {
      const el = /** @type {any} */ (document.querySelector("cts-json-view.config-json"));
      return el ? el.value : "";
    });
    expect(configValue).toContain("op.example.com");
  });

  test("module status badges render from the plan response", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          withLatestRuns(MOCK_PLAN_DETAIL, {
            "test-inst-001": {
              status: "FINISHED",
              result: "PASSED",
            },
            "test-inst-002": {
              status: "FINISHED",
              result: "WARNING",
            },
            "test-inst-003": {
              // A failed test is reported as INTERRUPTED+FAILED, not FINISHED+FAILED
              // (it never reaches FINISHED). The row badge and the status-bar segment
              // must still render the FAILED verdict in red (GitLab #1858/#1859).
              status: "INTERRUPTED",
              result: "FAILED",
            },
          }),
        ),
      }),
    );

    await setupCommonRoutes(page);
    await page.route("**/api/plan/plan-abc-123/findings", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_FINDINGS),
      }),
    );

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Wait for module rows to render
    const firstRow = page.locator("#planItems .module-row").first();
    await expect(firstRow).toBeVisible();

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

    // R28: the FAILED row's lozenge deep-links to the run's earliest
    // failure, named by the findings response, so a click lands on the
    // failure entry rather than the top of the log. The aria-label
    // switches to the "Jump to first failure" form (R7).
    const failedRow = page.locator("#planItems .module-row").nth(2);
    const failedLink = failedRow.locator('[data-testid="module-status-link"]');
    await expect(failedLink).toHaveAttribute(
      "href",
      "log-detail.html?log=test-inst-003#entry-test-inst-003-bbbb",
    );
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
    // fell through to the neutral grey.
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

  test("whole-plan status overview resolves, settles unseen runs, and segment click flashes the row (R8/R11/R18)", async ({
    page,
  }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        // 001 → PASSED, 002 → WARNING, 003 → a latest run this viewer cannot
        // see, which the server leaves without status. The 4th never ran.
        body: JSON.stringify(
          withLatestRuns(MOCK_PLAN_DETAIL, {
            "test-inst-001": { result: "PASSED" },
            "test-inst-002": { result: "WARNING" },
            "test-inst-003": null,
          }),
        ),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    const segments = page.locator('#planDetailStatus [data-testid="plan-status-segment"]');
    await expect(segments).toHaveCount(4);

    // Segments resolve to their colours. The run without a status settles to
    // a STATIC neutral (never --pending); the never-run module is neutral too.
    await expect(segments.nth(0)).toHaveClass(/cts-pst-seg--pass/);
    await expect(segments.nth(1)).toHaveClass(/cts-pst-seg--warn/);
    await expect(segments.nth(2)).toHaveClass(/cts-pst-seg--neutral/);
    await expect(segments.nth(2)).not.toHaveClass(/cts-pst-seg--pending/);
    await expect(segments.nth(3)).toHaveClass(/cts-pst-seg--neutral/);

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
        body: JSON.stringify(
          withLatestRuns(MOCK_PLAN_DETAIL, {
            "test-inst-001": { result: "PASSED" },
            "test-inst-002": { result: "WARNING" },
            "test-inst-003": { status: "INTERRUPTED", result: "FAILED" },
          }),
        ),
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

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Modal hidden initially
    const certModal = page.locator("#certificationPackageModal");
    await expect(certModal).toBeHidden();

    // R26: the Certify button is hidden until the plan-detail page has
    // read the status of each module and confirmed at least one
    // FINISHED test with no FAILED result. The default plan fixture
    // carries PASSED for every instance, so it appears once the plan loads.
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
        body: JSON.stringify(
          withLatestRuns(MOCK_PLAN_DETAIL, {
            "test-inst-001": { result: "PASSED" },
            "test-inst-002": { result: "PASSED" },
            "test-inst-003": { result: "FAILED" },
          }),
        ),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Wait for the page to finish wiring the action rail. The Private
    // link button is always rendered in the same branch as Certify and
    // arrives first, so we use it as the readiness signal.
    await expect(page.locator('[data-testid="private-link-btn"]')).toBeVisible();

    // Positive readiness signal that the FAILED status has been
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

  test("certify button stays hidden when the plan has no certification profile", async ({
    page,
  }) => {
    await setupFailFast(page);

    // A plan whose class doesn't override certificationProfileName is not
    // part of the certification program; the server refuses the package
    // with a 422 no_certification_profile, so don't offer the button.
    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_PLAN_DETAIL, certificationProfileName: [] }),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Same readiness signal as the FAILED-module test above: the action
    // rail is rendered, so canCertify has been computed.
    await expect(page.locator('[data-testid="private-link-btn"]')).toBeVisible();
    await expect(
      page.locator('#planItems .module-row cts-badge[label="PASSED"]').first(),
    ).toBeVisible();

    await expect(page.locator('[data-testid="certify-btn"]')).toHaveCount(0);
  });

  test("R28: a FAILED badge links to the run's earliest failure without downloading its log", async ({
    page,
  }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          withLatestRuns(MOCK_PLAN_DETAIL, {
            "test-inst-001": { result: "FAILED" },
            "test-inst-002": { result: "PASSED" },
            "test-inst-003": { result: "FAILED" },
          }),
        ),
      }),
    );
    await setupCommonRoutes(page);
    await page.route("**/api/plan/plan-abc-123/findings", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ...MOCK_PLAN_FINDINGS,
          // test-inst-001's earliest failure, and one for a run that is no
          // longer module 3's latest instance, which must be ignored.
          firstFailures: [
            { moduleIndex: 0, testId: "test-inst-001", entryId: "test-inst-001-aaaa" },
            { moduleIndex: 2, testId: "test-inst-003-older", entryId: "stale-entry" },
          ],
        }),
      }),
    );
    /** @type {string[]} */
    const logRequests = [];
    page.on("request", (req) => {
      if (req.url().includes("/api/log/")) logRequests.push(req.url());
    });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    const links = page.locator('#planItems .module-row [data-testid="module-status-link"]');
    await expect(links.nth(0)).toHaveAttribute(
      "href",
      "log-detail.html?log=test-inst-001#entry-test-inst-001-aaaa",
    );
    // A FAILED run the findings name no earliest failure for links to the top
    // of its log.
    await expect(links.nth(2)).toHaveAttribute("href", "log-detail.html?log=test-inst-003");
    // No log was downloaded to build either link.
    expect(logRequests).toEqual([]);
  });

  test("R28 public view: a FAILED badge links to the top of the log", async ({ page }) => {
    await setupFailFast(page);

    // Public viewers get no findings, so no first-failure fragment either;
    // the public flag still composes into the query string.
    await page.route("**/api/plan/plan-abc-123*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          withLatestRuns(
            { ...MOCK_PLAN_DETAIL, publish: "everything" },
            {
              "test-inst-001": { result: "PASSED" },
              "test-inst-002": { result: "PASSED" },
              "test-inst-003": { result: "FAILED" },
            },
          ),
        ),
      }),
    );
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123&public=true");

    const failedLink = page
      .locator("#planItems .module-row")
      .nth(2)
      .locator('[data-testid="module-status-link"]');
    await expect(failedLink).toHaveAttribute(
      "href",
      "log-detail.html?log=test-inst-003&public=true",
    );
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
        body: JSON.stringify(
          withLatestRuns(MOCK_PLAN_DETAIL_LONG_VARIANT, {
            "test-inst-001": {},
            "test-inst-002": {},
            "test-inst-003": {},
          }),
        ),
      }),
    );
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

  test("findings summary lists each distinct finding once with the modules it occurred in", async ({
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
    await setupCommonRoutes(page);
    await page.route("**/api/plan/plan-abc-123/findings", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_FINDINGS),
      }),
    );

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    const summary = page.locator('#planDetailFindings [data-testid="plan-findings"]');
    await expect(summary.locator("summary").first()).toHaveText(
      "Summary of results: 1 failure, 1 warning, 1 image to review, 1 module not run",
    );

    // Three findings for four module occurrences: the failure shared by two
    // modules is one row. The plan's never-run module is a row of its own,
    // last, linking to that module's row on this page.
    const rows = summary.locator('[data-testid="plan-finding"]');
    await expect(rows).toHaveCount(4);
    await expect(rows.nth(3)).toHaveAttribute("data-kind", "NOT_RUN");
    await expect(rows.nth(3).locator('[data-testid="plan-finding-link"]')).toHaveAttribute(
      "href",
      "#cts-module-3",
    );
    const shared = rows.nth(0);
    await expect(shared).toContainText("CheckDiscEndpointIssuer");
    await expect(shared).toContainText("in 2 modules");

    // Its modules sit under a disclosure, each linking to the entry in that
    // module's own log.
    const links = shared.locator('[data-testid="plan-finding-link"]');
    await expect(links.first()).toBeHidden();
    await shared.locator("summary").click();
    await expect(links).toHaveCount(2);
    await expect(links.nth(0)).toHaveAttribute(
      "href",
      "log-detail.html?log=test-inst-001#entry-test-inst-001-aaaa",
    );
    await expect(links.nth(1)).toHaveAttribute(
      "href",
      "log-detail.html?log=test-inst-003#entry-test-inst-003-bbbb",
    );

    // The summary sits above the module rows.
    const summaryBox = await summary.boundingBox();
    const modulesBox = await page.locator("#planDetailModules").boundingBox();
    if (!summaryBox || !modulesBox) throw new Error("summary and modules must both be laid out");
    expect(summaryBox.y + summaryBox.height).toBeLessThanOrEqual(modulesBox.y);
  });

  test("an image finding shows a thumbnail that opens the image with what it should show", async ({
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
    await setupCommonRoutes(page);
    await page.route("**/api/plan/plan-abc-123/findings", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_FINDINGS),
      }),
    );
    /** @type {string[]} */
    const imageRequests = [];
    await page.route("**/api/plan/plan-abc-123/findings/*/image", (route) => {
      imageRequests.push(new URL(route.request().url()).pathname);
      return route.fulfill({
        status: 200,
        contentType: "image/svg+xml",
        body: '<svg xmlns="http://www.w3.org/2000/svg" width="4" height="4"/>',
      });
    });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Only the IMAGE row has a thumbnail, pointing at that entry's image.
    const thumbs = page.locator('#planDetailFindings [data-testid="plan-finding-thumb"]');
    await expect(thumbs).toHaveCount(1);
    await expect(thumbs.locator("img")).toHaveAttribute(
      "src",
      "/api/plan/plan-abc-123/findings/test-inst-003-dddd/image",
    );
    await expect(thumbs.locator("img")).toHaveJSProperty("complete", true);

    await thumbs.click();

    const modal = page.locator('[data-testid="plan-finding-image-modal"]');
    await expect(modal.locator('[data-testid="plan-finding-expected"]')).toBeVisible();
    await expect(modal.locator('[data-testid="plan-finding-expected"]')).toContainText(
      "ExpectRedirectUriMissingErrorPage",
    );
    await expect(modal.locator('[data-testid="plan-finding-expected"]')).toContainText(
      "Show an error page saying the redirect URI is missing",
    );
    await expect(modal.locator('[data-testid="plan-finding-viewed-link"]')).toHaveAttribute(
      "href",
      "log-detail.html?log=test-inst-003#entry-test-inst-003-dddd",
    );
    await expect(modal.locator("img.planFindingFullImage")).toBeVisible();
    expect(new Set(imageRequests)).toEqual(
      new Set(["/api/plan/plan-abc-123/findings/test-inst-003-dddd/image"]),
    );
  });

  test("the summary can be copied as text and downloaded as CSV", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );
    await setupCommonRoutes(page);
    await page.route("**/api/plan/plan-abc-123/findings", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_FINDINGS),
      }),
    );
    // Record clipboard writes: the harness grants no real clipboard access.
    await page.addInitScript(() => {
      /** @type {string[]} */
      const written = [];
      /** @type {any} */ (window).__copied = written;
      Object.defineProperty(navigator, "clipboard", {
        configurable: true,
        value: { writeText: (text) => (written.push(text), Promise.resolve()) },
      });
    });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    await page.locator('[data-testid="plan-findings-copy-text"] button').click();
    await expect
      .poll(() => page.evaluate(() => /** @type {any} */ (window).__copied.length))
      .toBe(1);
    const text = await page.evaluate(() => /** @type {any} */ (window).__copied[0]);
    expect(text).toContain(
      "Summary of results: 1 failure, 1 warning, 1 image to review, 1 module not run\n",
    );
    expect(text).toContain("/plan-detail.html?plan=plan-abc-123\n");
    expect(text).toContain(
      "  - oidcc-server (client_auth_type=client_secret_basic, response_type=code): http://localhost:9876/log-detail.html?log=test-inst-001#entry-test-inst-001-aaaa\n",
    );
    expect(text).toMatch(
      /- oidcc-codereuse \(.*\): http:\/\/localhost:9876\/plan-detail\.html\?plan=plan-abc-123#cts-module-3\n/,
    );

    const downloadPromise = page.waitForEvent("download");
    await page.locator('[data-testid="plan-findings-download-csv"] button').click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toBe("summary-of-results-plan-abc-123.csv");
    const csv = (await (await download.createReadStream()).toArray()).join("");
    const lines = csv.trimEnd().split("\r\n");
    expect(lines[0]).toBe("kind,condition,message,requirements,module,variant,test_id,count,link");
    expect(lines.length).toBe(1 + 4 + 1);
    expect(lines[1]).toBe(
      'FAILURE,CheckDiscEndpointIssuer,issuer in discovery document does not match the configured issuer,OIDCD-4.3,oidcc-server,"client_auth_type=client_secret_basic, response_type=code",test-inst-001,1,http://localhost:9876/log-detail.html?log=test-inst-001#entry-test-inst-001-aaaa',
    );
  });

  test("findings are neither requested nor shown on the public view", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123?public=true", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...MOCK_PLAN_DETAIL, publish: "everything" }),
      }),
    );
    await setupCommonRoutes(page);
    let findingsRequests = 0;
    await page.route("**/api/plan/*/findings*", (route) => {
      findingsRequests++;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_FINDINGS),
      });
    });

    await page.goto("/plan-detail.html?plan=plan-abc-123&public=true");

    await expect(page.locator("#planItems .module-row").first()).toBeVisible();
    // The public view still says which modules have not finished, which it
    // knows from the plan itself, but asks for and shows no findings.
    const rows = page.locator('[data-testid="plan-finding"]');
    await expect(rows).toHaveCount(1);
    await expect(rows).toHaveAttribute("data-kind", "NOT_RUN");
    expect(findingsRequests).toBe(0);
  });

  test("a failed findings request leaves the rest of the page working", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );
    await setupCommonRoutes(page);
    await page.route("**/api/plan/plan-abc-123/findings", (route) =>
      route.fulfill({ status: 500, contentType: "application/json", body: "{}" }),
    );

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    await expect(page.locator("#planItems .module-row")).toHaveCount(4);
    const rows = page.locator('[data-testid="plan-finding"]');
    await expect(rows).toHaveCount(1);
    await expect(rows).toHaveAttribute("data-kind", "NOT_RUN");
    await expect(page.locator("#errorModal")).toBeHidden();
  });

  test("findings summary wraps inside a 375px viewport", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 800 });
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );
    await setupCommonRoutes(page);
    await page.route("**/api/plan/plan-abc-123/findings", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_FINDINGS),
      }),
    );

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    await expect(page.locator('[data-testid="plan-finding"]')).toHaveCount(4);
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(0);
  });
});

test.describe("plan-detail.html — private link", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("private link: auto-copies, shows server message + Copy button + copied status", async ({
    page,
  }) => {
    const SHARE_LINK = "https://example.test/login.html?token=plan-xyz";
    const SHARE_MESSAGE = "INFO: This link will be invalidated on a server restart";

    // Spy on both clipboard paths: navigator.clipboard.write (the Safari-safe
    // ClipboardItem auto-copy the page starts synchronously in the
    // cts-generate-private-link handler) and writeText (the manual Copy button).
    await page.addInitScript(() => {
      window.__clipboardWriteValue = null;
      window.__clipboardWriteCalled = false;
      window.__clipboardWriteText = null;
      if (navigator.clipboard) {
        navigator.clipboard.writeText = (text) => {
          window.__clipboardWriteText = text;
          return Promise.resolve();
        };
        navigator.clipboard.write = async (items) => {
          window.__clipboardWriteCalled = true;
          try {
            const item = items && items[0];
            if (item && item.getType) {
              const blob = await item.getType("text/plain");
              window.__clipboardWriteValue = await blob.text();
            }
          } catch {
            /* ignore — spy must never throw */
          }
          return Promise.resolve();
        };
      }
    });

    await setupFailFast(page);
    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          withLatestRuns(MOCK_PLAN_DETAIL, {
            "test-inst-001": { result: "PASSED" },
            "test-inst-002": { result: "PASSED" },
            "test-inst-003": { result: "PASSED" },
          }),
        ),
      }),
    );
    await page.route("**/api/plan/plan-abc-123/share*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ link: SHARE_LINK, message: SHARE_MESSAGE }),
      }),
    );
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Open the shared private-link dialog and generate.
    await page.locator('[data-testid="private-link-btn"]').click();
    const dialog = page.locator('[data-testid="private-link-dialog"]');
    await expect(dialog).toBeVisible();
    await dialog.locator(".plinkGenerateBtn").click();

    // Result shows the link + server message.
    const result = dialog.locator('[data-testid="private-link-result"]');
    await expect(result).toBeVisible();
    await expect(result.locator(".plinkUrl")).toContainText(SHARE_LINK);
    await expect(result.locator(".plinkMessage")).toContainText("invalidated on a server restart");

    // Auto-copy fired with the link; copied status reflects the real outcome.
    await expect.poll(() => page.evaluate(() => window.__clipboardWriteCalled)).toBe(true);
    await expect.poll(() => page.evaluate(() => window.__clipboardWriteValue)).toBe(SHARE_LINK);
    await expect(dialog.locator('[data-testid="private-link-copy-status"]')).toHaveText(
      "Copied to clipboard.",
    );

    // The manual Copy button re-copies via writeText.
    await result.locator(".plinkCopyBtn").click();
    await expect.poll(() => page.evaluate(() => window.__clipboardWriteText)).toBe(SHARE_LINK);

    // "Send via email" is an anchor carrying the mailto: URL. The subject is
    // worded for both consumers of the shared dialog, so it reads correctly
    // for a plan as well as a log.
    const emailLink = result.locator(".plinkEmailBtn a");
    await expect(emailLink).toBeVisible();
    await expect(emailLink).toHaveAttribute(
      "href",
      `mailto:?subject=OpenID%20Foundation%20Conformance%20Test%20Results&body=${encodeURIComponent(SHARE_LINK)}`,
    );
  });
});
