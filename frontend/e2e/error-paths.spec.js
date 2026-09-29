import { test, expect } from "@playwright/test";
import {
  setupCommonRoutes,
  setupFailFast,
  setupTestInfoRoute,
  expectNoUnmockedCalls,
} from "./helpers/routes.js";
import { selectPlanViaSearch } from "./helpers/pick-plan.js";
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

  test("repeated submits of an oversized file do not stack banners", async ({ page }) => {
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

    const certBtn = page.locator('cts-plan-actions [data-testid="certify-btn"] button');
    await expect(certBtn).toBeVisible();
    await certBtn.click();

    const modal = page.locator("#certificationPackageModal");
    await expect(modal).toBeVisible();

    const errorContainer = page.locator("#certificationPackageFormErrors");
    await expect(errorContainer.locator("cts-alert")).toHaveCount(0);

    // Upload a file one byte over the 1,024,000 limit. The input retains this
    // selection across clicks, so every submit re-fails size validation.
    await page.locator("#clientSideDataBtn").setInputFiles({
      name: "oversized.zip",
      mimeType: "application/zip",
      buffer: Buffer.alloc(1024001, 0),
    });

    // Click Create several times. Before the fix each click appended a new
    // banner (N clicks -> N banners); the regression is exactly this stacking.
    const submitBtn = page.locator("#certificationPackageFormSubmitBtn > button");
    for (let i = 0; i < 4; i++) {
      await submitBtn.click();
    }

    // The decisive assertion: exactly one banner survives, not four.
    const alert = errorContainer.locator("cts-alert");
    await expect(alert).toHaveCount(1);
    await expect(alert).toHaveAttribute("variant", "danger");
    await expect(alert).toHaveAttribute("dismissible", "");

    const alertBody = alert.locator(".oidf-alert.oidf-alert-danger");
    await expect(alertBody).toBeVisible();
    await expect(alertBody).toContainText("oversized.zip");
    await expect(alertBody).toContainText("exceeded the maximum allowed size");

    // Dismissing the single banner returns the container to empty.
    await alertBody.locator("button.oidf-alert-close").click();
    await expect(errorContainer.locator("cts-alert")).toHaveCount(0);
  });

  test("a stale banner does not survive closing and reopening the modal", async ({ page }) => {
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

    const certBtn = page.locator('cts-plan-actions [data-testid="certify-btn"] button');
    await expect(certBtn).toBeVisible();
    await certBtn.click();

    const modal = page.locator("#certificationPackageModal");
    await expect(modal).toBeVisible();

    const errorContainer = page.locator("#certificationPackageFormErrors");

    // Trigger an error, then close the modal WITHOUT dismissing the banner.
    await page.locator("#clientSideDataBtn").setInputFiles({
      name: "oversized.zip",
      mimeType: "application/zip",
      buffer: Buffer.alloc(1024001, 0),
    });
    await page.locator("#certificationPackageFormSubmitBtn > button").click();
    await expect(errorContainer.locator("cts-alert")).toHaveCount(1);

    await page.locator("#cancelCertificationPackageFormModal > button").click();
    await expect(modal).toBeHidden();

    // Reopening must present a clean form — the stale banner is gone.
    await certBtn.click();
    await expect(modal).toBeVisible();
    await expect(errorContainer.locator("cts-alert")).toHaveCount(0);
  });

  test("a filename containing HTML is rendered as text, not injected as markup", async ({
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
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    const certBtn = page.locator('cts-plan-actions [data-testid="certify-btn"] button');
    await expect(certBtn).toBeVisible();
    await certBtn.click();

    const modal = page.locator("#certificationPackageModal");
    await expect(modal).toBeVisible();

    const errorContainer = page.locator("#certificationPackageFormErrors");

    // A filename with an <img onerror> payload would execute if the message
    // were interpolated into HTML. setInputFiles preserves the literal name.
    await page.locator("#clientSideDataBtn").setInputFiles({
      name: "<img src=x onerror=window.__xss=1>.zip",
      mimeType: "application/zip",
      buffer: Buffer.alloc(1024001, 0),
    });
    await page.locator("#certificationPackageFormSubmitBtn > button").click();

    const alert = errorContainer.locator("cts-alert");
    await expect(alert).toHaveCount(1);
    // No element was injected from the filename, and the payload never ran.
    await expect(alert.locator("img")).toHaveCount(0);
    const xssRan = await page.evaluate(() => Object.prototype.hasOwnProperty.call(window, "__xss"));
    expect(xssRan).toBe(false);
    // The literal filename text is shown to the user.
    await expect(alert).toContainText("exceeded the maximum allowed size");
  });
});

/**
 * POST /api/plan/{id}/certificationpackage refusals (#1954). The form is
 * submitted with fetch, so a 422/403 must render in its own dialog rather
 * than the browser navigating to the raw JSON error body.
 */
test.describe("plan-detail.html — certification package submission", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  async function openCertificationPackageModal(page, certPackageRoute) {
    await setupFailFast(page);
    await page.route("**/api/plan/plan-abc-123", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_PLAN_DETAIL),
      }),
    );
    await page.route("**/api/plan/plan-abc-123/certificationpackage", certPackageRoute);
    await setupTestInfoRoute(page);
    await setupCommonRoutes(page, { user: MOCK_ADMIN_USER });

    await page.goto("/plan-detail.html?plan=plan-abc-123");

    const certBtn = page.locator('cts-plan-actions [data-testid="certify-btn"] button');
    await expect(certBtn).toBeVisible();
    await certBtn.click();
    await expect(page.locator("#certificationPackageModal")).toBeVisible();
  }

  /**
   * Replaces the certification package response's body stream, which the
   * page reads chunk by chunk. "error" fails the stream straight away;
   * "hold" delivers one 1.5 MB chunk and keeps the stream open until the
   * test calls window.__finishPackage().
   * @param {import("@playwright/test").Page} page
   * @param {"error" | "hold"} mode
   */
  async function stubCertificationPackageBody(page, mode) {
    await page.addInitScript((mode) => {
      const descriptor = /** @type {PropertyDescriptor} */ (
        Object.getOwnPropertyDescriptor(Response.prototype, "body")
      );
      const bodyGetter = /** @type {(this: Response) => ReadableStream | null} */ (descriptor.get);
      Object.defineProperty(Response.prototype, "body", {
        /** @this {Response} */
        get() {
          if (!this.url.includes("/certificationpackage")) {
            return bodyGetter.call(this);
          }
          return new ReadableStream({
            start(controller) {
              if (mode === "error") {
                controller.error(new TypeError("network error"));
                return;
              }
              controller.enqueue(new Uint8Array(1500000));
              /** @type {any} */ (window).__finishPackage = () => controller.close();
            },
          });
        },
      });
    }, mode);
  }

  /**
   * Closes the certification package error dialog and waits for the page
   * reload that follows when the server may already have changed the plan.
   * @param {import("@playwright/test").Page} page
   */
  async function closeErrorDialogExpectingReload(page) {
    const reloaded = page.waitForEvent("load");
    await page.locator("#certificationPackageErrorModal .oidf-modal-footer button").click();
    await reloaded;
    await expect(page.locator("#certificationPackageErrorModal")).toBeHidden();
  }

  function jsonError(status, body) {
    return (route) =>
      route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
  }

  test("failed or unrun tests are listed in the dialog, not shown as raw JSON", async ({
    page,
  }) => {
    await openCertificationPackageModal(
      page,
      jsonError(422, {
        error: "Unable to create certification package",
        error_description: "All tests have not been completed or tests have failed. ",
        plan_name: "oidcc-basic-certification-test-plan",
        test_plan_id: "plan-abc-123",
        variant: "{}",
        failed_tests: {
          "oidcc-server": { testId: "test-inst-001", status: "FINISHED", result: "FAILED" },
          "oidcc-response-type-missing": {
            testId: "NOT_YET_CREATED",
            status: "NOT_YET_CREATED",
            result: "NOT_YET_CREATED",
          },
        },
      }),
    );

    await page.locator("#certificationPackageFormSubmitBtn > button").click();

    const alert = page.locator("#certificationPackageErrorBody cts-alert");
    await expect(alert).toHaveCount(1);
    await expect(alert).toHaveAttribute("variant", "danger");
    await expect(alert).toContainText("every test in the plan has finished without failing");
    await expect(alert.locator("li")).toHaveCount(2);
    await expect(alert).toContainText("oidcc-server: failed");
    await expect(alert).toContainText("oidcc-response-type-missing: not run yet");
    await expect(alert.getByRole("link", { name: "oidcc-server" })).toHaveAttribute(
      "href",
      "/log-detail.html?log=test-inst-001",
    );
    await expect(alert.getByRole("link", { name: "oidcc-response-type-missing" })).toHaveCount(0);
    await expect(alert).not.toContainText("failed_tests");

    // Still on the plan page; the error has its own dialog in place of the
    // creation dialog, and closing it leaves the plan page as it was.
    await expect(page).toHaveURL(/\/plan-detail\.html\?plan=plan-abc-123$/);
    await expect(page.locator("#certificationPackageModal")).toBeHidden();
    const errorModal = page.locator("#certificationPackageErrorModal");
    await expect(errorModal).toBeVisible();
    await expect(errorModal).not.toContainText("Clicking the");
    // Nothing changed on the server, so closing must not reload the page.
    await page.evaluate(() => {
      /** @type {any} */ (window).__beforeClose = true;
    });
    await errorModal.locator(".oidf-modal-footer button").click();
    await expect(errorModal).toBeHidden();
    await expect(page.locator("#certificationPackageModal")).toBeHidden();
    expect(await page.evaluate(() => /** @type {any} */ (window).__beforeClose)).toBe(true);
  });

  test("an error_description without failed tests is shown as the message", async ({ page }) => {
    await openCertificationPackageModal(
      page,
      jsonError(422, {
        error: "no_certification_profile",
        error_description:
          "This test plan has no certification profile, so it is not part of the certification program.",
        plan_name: "oidcc-basic-certification-test-plan",
        test_plan_id: "plan-abc-123",
        variant: "{}",
      }),
    );

    await page.locator("#certificationPackageFormSubmitBtn > button").click();

    const alert = page.locator("#certificationPackageErrorBody cts-alert");
    await expect(alert).toHaveCount(1);
    await expect(alert).toContainText("This test plan has no certification profile");
    await expect(alert).not.toContainText("no_certification_profile");
  });

  test("an unknown plan shows the server's reason, not its error fields as tests", async ({
    page,
  }) => {
    await openCertificationPackageModal(
      page,
      jsonError(422, {
        error: "Unable to create certification package",
        error_description: "All tests have not been completed or tests have failed. ",
        plan_name: "",
        test_plan_id: "plan-abc-123",
        variant: "",
        failed_tests: {
          error: "invalid_plan_id",
          error_description: "plan ID plan-abc-123 does not exist",
        },
      }),
    );

    await page.locator("#certificationPackageFormSubmitBtn > button").click();

    const alert = page.locator("#certificationPackageErrorBody cts-alert");
    await expect(alert).toHaveText("plan ID plan-abc-123 does not exist");
    await expect(alert.locator("li")).toHaveCount(0);
  });

  test("an empty-bodied 422 says the plan was published but not made immutable", async ({
    page,
  }) => {
    await openCertificationPackageModal(page, (route) => route.fulfill({ status: 422, body: "" }));

    await page.locator("#certificationPackageFormSubmitBtn > button").click();

    const alert = page.locator("#certificationPackageErrorBody cts-alert");
    await expect(alert).toContainText(
      "has been published, but it could not be marked as immutable",
    );
    await closeErrorDialogExpectingReload(page);
  });

  test("a failed download after a 200 is not reported as the server being unreachable", async ({
    page,
  }) => {
    await stubCertificationPackageBody(page, "error");
    await openCertificationPackageModal(page, (route) =>
      route.fulfill({ status: 200, contentType: "application/zip", body: "zip-bytes" }),
    );

    await page.locator("#certificationPackageFormSubmitBtn > button").click();

    const alert = page.locator("#certificationPackageErrorBody cts-alert");
    await expect(alert).toContainText("download did not complete");
    await expect(alert).not.toContainText("Could not reach the server");
    await expect(page.locator("#certificationPackageModal")).toBeHidden();
    await closeErrorDialogExpectingReload(page);
  });

  test("an unexpected error while handling the response still ends in the error dialog", async ({
    page,
  }) => {
    await page.addInitScript(() => {
      URL.createObjectURL = () => {
        throw new Error("createObjectURL unavailable");
      };
    });
    await openCertificationPackageModal(page, (route) =>
      route.fulfill({ status: 200, contentType: "application/zip", body: "zip-bytes" }),
    );

    await page.locator("#certificationPackageFormSubmitBtn > button").click();

    const alert = page.locator("#certificationPackageErrorBody cts-alert");
    await expect(alert).toContainText(
      "Something went wrong while creating the certification package",
    );
    await expect(page.locator("#certificationPackageModal")).toBeHidden();
    await expect(page.locator("#certificationPackageDownloaded")).toBeHidden();
    await closeErrorDialogExpectingReload(page);
  });

  test("a 500 from building the package says the plan may already be published", async ({
    page,
  }) => {
    await openCertificationPackageModal(
      page,
      jsonError(500, {
        timestamp: "2026-09-29T10:00:00.000+00:00",
        status: 500,
        error: "Internal Server Error",
        path: "/api/plan/plan-abc-123/certificationpackage",
      }),
    );

    await page.locator("#certificationPackageFormSubmitBtn > button").click();

    const alert = page.locator("#certificationPackageErrorBody cts-alert");
    await expect(alert).toContainText("failed (HTTP 500)");
    await expect(alert).toContainText("may already have been published and marked as immutable");
    await expect(alert).not.toContainText("Internal Server Error");
    await closeErrorDialogExpectingReload(page);
  });

  test("an empty-bodied 403 shows a readable message", async ({ page }) => {
    await openCertificationPackageModal(page, (route) => route.fulfill({ status: 403, body: "" }));

    await page.locator("#certificationPackageFormSubmitBtn > button").click();

    const alert = page.locator("#certificationPackageErrorBody cts-alert");
    await expect(alert).toHaveCount(1);
    await expect(alert).toContainText("not allowed to create a certification package");
    await expect(page.locator("#certificationPackageModal")).toBeHidden();
  });

  test("a module name containing HTML is rendered as text", async ({ page }) => {
    await openCertificationPackageModal(
      page,
      jsonError(422, {
        error: "Unable to create certification package",
        failed_tests: {
          "<img src=x onerror=window.__xss=1>": {
            testId: "NOT_YET_CREATED",
            status: "NOT_YET_CREATED",
            result: "NOT_YET_CREATED",
          },
        },
      }),
    );

    await page.locator("#certificationPackageFormSubmitBtn > button").click();

    const alert = page.locator("#certificationPackageErrorBody cts-alert");
    await expect(alert).toContainText("<img src=x onerror=window.__xss=1>: not run yet");
    await expect(alert.locator("img")).toHaveCount(0);
    const xssRan = await page.evaluate(() => Object.prototype.hasOwnProperty.call(window, "__xss"));
    expect(xssRan).toBe(false);
  });

  test("the bytes received so far are shown while the package downloads", async ({ page }) => {
    await stubCertificationPackageBody(page, "hold");
    await openCertificationPackageModal(page, (route) =>
      route.fulfill({ status: 200, contentType: "application/zip", body: "zip-bytes" }),
    );

    await page.locator("#certificationPackageFormSubmitBtn > button").click();

    const progress = page.locator("#certificationPackageProgress");
    await expect(progress).toBeVisible();
    await expect(progress).toHaveText("Downloaded 1.5 MB");
    // formatByteCount is reachable here only because plan-detail.html's
    // inline script is a classic script, whose top-level functions are globals.
    const formatted = await page.evaluate(() =>
      [999, 1000, 999499, 999500, 1000000].map(/** @type {any} */ (window).formatByteCount),
    );
    expect(formatted).toEqual(["999 bytes", "1 KB", "999 KB", "1.0 MB", "1.0 MB"]);

    const downloadPromise = page.waitForEvent("download");
    await page.evaluate(() => /** @type {any} */ (window).__finishPackage());
    await downloadPromise;

    await expect(page.locator("#certificationPackageDownloaded")).toBeVisible();
    await expect(progress).toBeHidden();
  });

  test("a successful submission downloads the zip under the server's filename", async ({
    page,
  }) => {
    await openCertificationPackageModal(page, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/zip",
        headers: {
          "Content-Disposition":
            'attachment; filename="oidcc-basic-certification-test-plan-plan-abc-123-01-Sep-2026.zip"',
        },
        body: "zip-bytes",
      }),
    );

    const downloadPromise = page.waitForEvent("download");
    await page.locator("#certificationPackageFormSubmitBtn > button").click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toBe(
      "oidcc-basic-certification-test-plan-plan-abc-123-01-Sep-2026.zip",
    );

    await expect(page.locator("#certificationPackageDownloaded")).toBeVisible();
    await expect(page.locator("#certificationPackageFormModalBody")).toBeHidden();
    await expect(page.locator("#closeCertificationPackageFormModal")).toBeVisible();
    await expect(page.locator("#certificationPackageErrorBody cts-alert")).toHaveCount(0);
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

  test("GET /api/log 500 surfaces an inline error and no cards render", async ({ page }) => {
    await setupFailFast(page);

    // The cts-log-list component fetches /api/log?length=1000&order=started,desc once at mount.
    // A 500 sets the component's _error state and renders a danger cts-alert
    // inline above the empty list region — there is no #errorModal hop here.
    // The legacy modal contract was specific to the cts-data-table path that
    // the redesign retired in May 2026.
    await page.route("**/api/log?*", (route) =>
      route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ code: 500, error: "backend unavailable" }),
      }),
    );

    await setupCommonRoutes(page);

    await page.goto("/logs.html");

    const alert = page.locator("#logsListing cts-alert[variant='danger']");
    await expect(alert).toBeVisible();
    await expect(alert).toContainText("HTTP 500");

    // No cards render.
    await expect(page.locator('#logsListing [data-testid="log-list-item"]')).toHaveCount(0);

    // T-8 "realistic next action": no full-page hijack — the user can still
    // navigate via the navbar after seeing the error.
    await expect(page).toHaveURL(/\/logs\.html/);
    await expect(page.locator("cts-navbar")).toBeVisible();
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

test.describe("schedule-test.html — POST /api/plan errors", () => {
  test.beforeEach(async ({ page }) => {
    // This test drives the advanced island's picker + create button.
    // Guided is the page default, so force the stored mode preference.
    await page.addInitScript(() => {
      try {
        localStorage.setItem("oidf-guided-mode", "advanced");
      } catch {
        /* storage unavailable — the test will surface it */
      }
    });
  });

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

    // Pick the no-variants client-basic plan (Create auto-enables).
    await selectPlanViaSearch(page, "oidcc-client-basic-certification-test-plan");

    const createBtn = page.locator("#createPlanBtn");
    await expect(createBtn).toBeEnabled({ timeout: 5000 });
    await createBtn.click();

    // Error modal shows the handler's {"error"} text, not the status line.
    const errorModal = page.locator("#errorModal");
    await expect(errorModal).toBeVisible();
    const errorText = errorModal.locator("#errorMessage");
    await expect(errorText).not.toBeEmpty();
    await expect(errorText).toContainText("invalid plan configuration");

    // T-8 "realistic next action": after dismissing, the user is still on
    // schedule-test.html (not navigated away to a plan that doesn't exist)
    // AND their plan selection is preserved — they can retry without
    // re-picking. The picker's row stays highlighted.
    await errorModal.locator(".oidf-modal-close").first().click();
    await expect(errorModal).toBeHidden();
    await expect(page).toHaveURL(/\/schedule-test\.html/);
    await expect(
      page.locator('#planSearch [data-plan-name="oidcc-client-basic-certification-test-plan"]'),
    ).toHaveClass(/is-active/);
  });

  test("POST /api/plan 401 (expired session) keeps the refresh hint in #errorModal", async ({
    page,
  }) => {
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
      route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({}) }),
    );
    // Body shape written by RestAuthenticationEntryPoint.
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 401,
          contentType: "application/json",
          body: JSON.stringify({
            error: "Unauthorized",
            message: "Full authentication is required to access this resource",
          }),
        });
      }
      return route.fallback();
    });

    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");
    await selectPlanViaSearch(page, "oidcc-client-basic-certification-test-plan");

    const createBtn = page.locator("#createPlanBtn");
    await expect(createBtn).toBeEnabled({ timeout: 5000 });
    await createBtn.click();

    const errorText = page.locator("#errorModal #errorMessage");
    await expect(errorText).toContainText(
      "Unauthorized : Full authentication is required to access this resource",
    );
    await expect(errorText).toContainText("Refresh the page to renew your session");
  });

  /**
   * `FAPI_UI.showError()` used to call `#errorModal.show()` unconditionally.
   * cts-modal is a lazily-upgraded custom element and the page CSS hides
   * `cts-modal:not(:defined)`, so on a page whose component module failed to
   * load the call threw a TypeError against a plain unknown element and the
   * user was told nothing at all. Blocking the module reproduces that; the
   * error must still reach the user, now via the toast fallback (#1860).
   */
  test("cts-modal module unavailable: the create failure falls back to a toast", async ({
    page,
  }) => {
    const ALL_PLANS = [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS];

    await setupFailFast(page);
    await page.route("**/components/cts-modal.js", (route) => route.abort());

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );
    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({}) }),
    );
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({ code: 400, error: "invalid plan configuration" }),
        });
      }
      return route.fallback();
    });

    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");
    await selectPlanViaSearch(page, "oidcc-client-basic-certification-test-plan");

    const createBtn = page.locator("#createPlanBtn");
    await expect(createBtn).toBeEnabled({ timeout: 5000 });
    await createBtn.click();

    // The modal element exists but never upgraded, so it shows nothing...
    await expect(page.locator("#errorModal")).toBeHidden();
    // ...and the toast carries the error instead.
    await expect(page.locator("cts-toast-host cts-toast")).toContainText(
      "invalid plan configuration",
    );

    // The user is still on the page with their selections intact.
    await expect(page).toHaveURL(/\/schedule-test\.html/);
    await expect(
      page.locator('#planSearch [data-plan-name="oidcc-client-basic-certification-test-plan"]'),
    ).toHaveClass(/is-active/);
  });

  /**
   * The last rung of the same degrade chain: with neither the modal nor the
   * toast available, `showError()` must still reach the user via a native
   * `alert()`. Without it the branch is unexercised and a future refactor
   * could silently drop the only remaining surface.
   */
  test("neither cts-modal nor the toast API: the create failure falls back to alert()", async ({
    page,
  }) => {
    const ALL_PLANS = [...MOCK_PLANS, MOCK_PLAN_NO_VARIANTS];

    await setupFailFast(page);
    await page.route("**/components/cts-modal.js", (route) => route.abort());
    await page.route("**/js/cts-toast-api.js", (route) => route.abort());

    /** @type {string[]} */
    const dialogs = [];
    page.on("dialog", (dialog) => {
      dialogs.push(dialog.message());
      return dialog.dismiss();
    });

    await page.route("**/api/plan/available", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(ALL_PLANS),
      }),
    );
    await page.route("**/api/lastconfig", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({}) }),
    );
    await page.route("**/api/plan?*", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({ code: 400, error: "invalid plan configuration" }),
        });
      }
      return route.fallback();
    });

    await setupCommonRoutes(page);

    await page.goto("/schedule-test.html");
    await selectPlanViaSearch(page, "oidcc-client-basic-certification-test-plan");

    const createBtn = page.locator("#createPlanBtn");
    await expect(createBtn).toBeEnabled({ timeout: 5000 });
    await createBtn.click();

    await expect.poll(() => dialogs.join("\n")).toContain("invalid plan configuration");
  });
});
