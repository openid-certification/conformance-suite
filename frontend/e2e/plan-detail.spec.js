import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLAN_DETAIL, MOCK_TEST_STATUS } from "./fixtures/mock-test-data.js";
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

    // Plan header renders (cts-plan-header exposes id="planHeader")
    const header = page.locator("#planHeader");
    await expect(header).toContainText("oidcc-basic-certification-test-plan");
    await expect(header).toContainText("plan-abc-123");
    await expect(header).toContainText("client_secret_basic");

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

  test("View configuration button opens an inline panel with plan configuration JSON", async ({
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

    await setupTestInfoRoute(page);
    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Wait for the View configuration button to render
    const configBtn = page.locator('[data-testid="view-config-btn"]');
    await expect(configBtn).toBeVisible();

    // Config panel should not exist initially
    await expect(page.locator('[data-testid="config-panel"]')).toHaveCount(0);

    // Click the inner <button> inside cts-button (Lit binds @click on the inner button)
    await configBtn.locator("button").click();

    // Panel appears with the plan ID and the config JSON inside the
    // read-only Monaco editor. Monaco virtualises rendered content, so we
    // assert on the editor's `.value` rather than the panel's textContent.
    const configPanel = page.locator('[data-testid="config-panel"]');
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
        status: "FINISHED",
        result: "FAILED",
      },
    });

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
    // the module has an instance.
    const statusLink = firstRow.locator('[data-testid="module-status-link"]');
    await expect(statusLink).toHaveAttribute("href", /log-detail\.html\?log=test-inst-001/);

    // Each module name has a help-icon tooltip wrapper. Hovering the
    // testSummary help-icon mounts an .oidf-tooltip in document.body.
    // The icon presence confirms the tooltip wrap is intact (the
    // cts-tooltip wrap is supplied at the page-component level).
    const helpIcon = firstRow.locator(".help-icon");
    await expect(helpIcon).toHaveAttribute("title", /Verify basic OpenID Connect/);
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

    await setupCommonRoutes(page);

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Wait for the page to finish wiring the action rail. The Private
    // link button is always rendered in the same branch as Certify and
    // arrives first, so we use it as the readiness signal.
    await expect(page.locator('[data-testid="private-link-btn"]')).toBeVisible();

    // Wait for /api/info to drain so canCertify has had a chance to flip.
    await page.waitForLoadState("networkidle");

    // No certify button — at least one FAILED result.
    await expect(page.locator('[data-testid="certify-btn"]')).toHaveCount(0);
  });
});
