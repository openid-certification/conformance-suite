import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { MOCK_PLAN_DETAIL } from "./fixtures/mock-test-data.js";
import { MOCK_ADMIN_USER } from "./fixtures/mock-users.js";

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

    // Open the certification package modal via its button.
    const certBtn = page.locator("#certificationPackageBtn");
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
    const alertBody = alert.locator(".alert.alert-danger.alert-dismissible");
    await expect(alertBody).toBeVisible();
    await expect(alertBody).toContainText("oversized.zip");
    await expect(alertBody).toContainText("exceeded the maximum allowed size");

    // Dismiss clears the alert from the DOM.
    await alertBody.locator("button.btn-close").click();
    await expect(errorContainer.locator("cts-alert")).toHaveCount(0);
  });
});
