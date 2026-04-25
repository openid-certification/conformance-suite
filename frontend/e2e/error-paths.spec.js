import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLAN_DETAIL } from "./fixtures/mock-test-data.js";
import { MOCK_ADMIN_USER } from "./fixtures/mock-users.js";
import { MOCK_PLANS, MOCK_PLAN_NO_VARIANTS } from "./fixtures/mock-plans.js";

/**
 * Exercises `showDialogError()` in plan-detail.html: a certification-package
 * upload rejected for exceeding 1,024,000 bytes must inject a dismissible
 * danger `cts-alert` into `#certificationPackageFormErrors`. This is the
 * only dynamic cts-alert injection path in the static pages; it's untested
 * elsewhere so this is where silent regressions would slip in.
 */
test.describe("plan-detail.html — dynamic error alert injection", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("oversized certification package file injects danger cts-alert", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );

    await setupTestInfoRoute(page);
    // Admin, so the certification package button is visible.
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    // Open the certification package modal via cts-plan-actions' Certify
    // button (the page-level JS catches `cts-certify` and opens the modal).
    const certBtn = page.locator('cts-plan-actions [data-testid="certify-btn"] button');
    await expect(certBtn).toBeVisible();
    await certBtn.click();

    const modal = page.locator("#certificationPackageModal");
    await expect(modal).toBeVisible();

    // Error container starts empty (no cts-alert yet).
    const errorContainer = page.locator("#certificationPackageFormErrors");
    await expect(errorContainer.locator("cts-alert")).toHaveCount(0);

    // Upload a file one byte over the 1,024,000 limit. setInputFiles takes a
    // buffer directly — no FS dependency.
    const oversizedBytes = 1024001;
    await page.locator("#clientSideDataBtn").setInputFiles({
      name: "oversized.zip",
      mimeType: "application/zip",
      buffer: Buffer.alloc(oversizedBytes, 0),
    });

    // Submit the form. showDialogError runs inside the submit handler.
    await page.locator("#certificationPackageFormSubmitBtn > button").click();

    // A single danger cts-alert with dismiss button appears.
    const alert = errorContainer.locator("cts-alert");
    await expect(alert).toHaveCount(1);
    await expect(alert).toHaveAttribute("variant", "danger");
    await expect(alert).toHaveAttribute("dismissible", "");

    // The inner rendered markup reflects the variant + dismiss affordance.
    const alertBody = alert.locator(".oidf-alert.oidf-alert-danger");
    await expect(alertBody).toBeVisible();
    await expect(alertBody).toContainText("oversized.zip");
    await expect(alertBody).toContainText("exceeded the maximum allowed size");

    // Dismiss clears the alert from the DOM.
    await alertBody.locator("button.oidf-alert-close").click();
    await expect(errorContainer.locator("cts-alert")).toHaveCount(0);
  });
});

/**
 * The three tests below cover the page-level error branches the filename
 * promises — the GET/POST API failures that fapi.ui.js funnels through
 * FAPI_UI.showError() → #errorModal. Each asserts the error surface AND a
 * realistic next-action affordance (T-8) so silently-broken modals without
 * any recovery path are caught too.
 */

test.describe("logs.html — DataTables server error", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("GET /api/log 500 surfaces #errorModal and table stays empty", async ({ page }) => {
    await setupFailFast(page);

    // DataTables ajax receives 500 — fapi.ui.js logs.html has a dedicated
    // ajax.error handler that calls FAPI_UI.showError with the responseJSON.
    await page.route("**/api/log?*", (route) =>
      route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ code: 500, error: "backend unavailable" }),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    // The error modal appears with the error message from the response.
    const errorModal = page.locator("#errorModal");
    await expect(errorModal).toBeVisible();
    const errorText = errorModal.locator("#errorMessage");
    // After U38 migrated to cts-data-table, the error message format
    // changed from the responseJSON.error passthrough to "HTTP <status>"
    // (cts-data-table's _fetchPage throws Error("HTTP " + status) on
    // non-OK responses). The actionable response body is no longer
    // surfaced through this path; "HTTP 500" is the new contract.
    await expect(errorText).toContainText("HTTP 500");

    // The table body has no data rows (the 500 meant nothing was rendered).
    // cts-data-table renders one error-state row inside tbody on fetch
    // failure; data rows carry data-row-index, error rows do not.
    const dataRows = page.locator("#logsListing tbody tr[data-row-index]");
    await expect(dataRows).toHaveCount(0);

    // T-8 "realistic next action": dismissing the modal doesn't navigate the
    // user away — the page is still usable (e.g. reload to retry).
    await errorModal.locator(".oidf-modal-close").first().click();
    await expect(errorModal).toBeHidden();
    await expect(page).toHaveURL(/\/logs\.html/);
  });
});

test.describe("log-detail.html — /api/info/:id 404", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("GET /api/info/:testId 404 surfaces #errorModal; navbar remains usable", async ({
    page,
  }) => {
    const testId = "missing-test-xyz";

    await setupFailFast(page);

    // 404 on /api/info/:id — the primary resource the page needs.
    await page.route(`**/api/info/${testId}*`, (route) =>
      route.fulfill({
        status: 404,
        contentType: "application/json",
        body: JSON.stringify({ error: "log not found" }),
      }),
    );

    // log-detail.html also polls /api/log/:id and /api/runner/:id on boot.
    // Return empty/idle so those don't throw their own errors and muddy the
    // #errorModal content.
    await page.route(`**/api/log/${testId}**`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "[]",
      }),
    );
    await page.route(`**/api/runner/${testId}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ id: testId, status: "FINISHED" }),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto(`/log-detail.html?log=${testId}`);

    // The error modal shows with "log not found" (the 404 body was parsed).
    const errorModal = page.locator("#errorModal");
    await expect(errorModal).toBeVisible();
    const errorText = errorModal.locator("#errorMessage");
    await expect(errorText).toContainText("log not found");

    // T-8 "realistic next action": after dismissing the modal, the navbar is
    // still rendered and offers a way back to the app.
    await errorModal.locator(".oidf-modal-close").first().click();
    await expect(errorModal).toBeHidden();
    const navbar = page.locator("cts-navbar");
    await expect(navbar).toBeVisible();
    // At least one link back to a working page (Test Logs or Home).
    const anyNavLink = navbar.locator("a.nav-link").first();
    await expect(anyNavLink).toBeVisible();
  });
});

test.describe("schedule-test.html — POST /api/plan 400", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("POST /api/plan 400 surfaces #errorModal; form state preserved", async ({ page }) => {
    const ALL_PLANS = [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS];

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

    // Reject the plan creation POST with a 400 and a structured error body.
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({
            code: 400,
            error: "invalid plan configuration",
          }),
        });
      }
      return route.fallback();
    });

    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");

    // Fill the cascade with client-basic (no variants so Create auto-enables).
    await page.locator("#specFamilySelect").selectOption("OIDCC");
    const entitySelect = page.locator("#entitySelect");
    await expect(entitySelect).toBeVisible();
    await entitySelect.selectOption("client-basic");

    const createBtn = page.locator("#createPlanBtn");
    await expect(createBtn).toBeEnabled({ timeout: 5000 });
    await createBtn.click();

    // Error modal shows. schedule-test.html's catch only parses message bodies
    // for 500 responses; for 400 it displays statusText ("Bad Request"). Assert
    // on the modal being visible + a non-empty error message, not the body
    // content — the real contract is "the user is told something went wrong".
    const errorModal = page.locator("#errorModal");
    await expect(errorModal).toBeVisible();
    const errorText = errorModal.locator("#errorMessage");
    await expect(errorText).not.toBeEmpty();
    await expect(errorText).toContainText("Bad Request");

    // T-8 "realistic next action": after dismissing, the user is still on
    // schedule-test.html (not navigated away to a plan that doesn't exist)
    // AND their cascade selections are preserved — they can retry without
    // re-entering everything.
    await errorModal.locator(".oidf-modal-close").first().click();
    await expect(errorModal).toBeHidden();
    await expect(page).toHaveURL(/\/schedule-test\.html/);
    await expect(page.locator("#specFamilySelect")).toHaveValue("OIDCC");
    await expect(entitySelect).toHaveValue("client-basic");
  });
});
