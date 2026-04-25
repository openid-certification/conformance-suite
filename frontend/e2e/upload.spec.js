import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";
import {
  MOCK_IMAGES_PENDING,
  MOCK_IMAGES_EMPTY,
  MOCK_UPLOAD_TEST_INFO,
} from "./fixtures/upload-data.js";

/**
 * Helper: register upload-page-specific routes.
 * Must be called after setupFailFast and before setupCommonRoutes.
 *
 * upload.html loads:
 *   - /api/log/:testId/images  — list of pending/existing image entries
 *   - /api/info/:testId        — test info for the page header
 *   - /api/log/:testId/images/:uploadId (POST) — individual upload slot submit
 *   - /api/log/:testId/images  (POST) — "add additional image" submit
 */
async function setupUploadRoutes(page, { testId, images, uploadStatus = 200 }) {
  // GET images list
  await page.route(`**/api/log/${testId}/images`, (route) => {
    if (route.request().method() === "GET") {
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(images),
      });
    }
    // POST additional image upload — use uploadStatus so tests can simulate 400
    if (uploadStatus === 200) {
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          _id: "img-new-001",
          img: "data:image/png;base64,iVBORw0KGgo=",
        }),
      });
    }
    return route.fulfill({
      status: uploadStatus,
      contentType: "application/json",
      body: JSON.stringify({ error: "Upload rejected" }),
    });
  });

  // /api/info/:testId — page header
  await page.route(`**/api/info/${testId}`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ...MOCK_UPLOAD_TEST_INFO, testId }),
    }),
  );
}

test.describe("upload.html — Image Uploader", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("page renders with Upload button as cts-button (disabled by default)", async ({ page }) => {
    await setupFailFast(page);
    await setupUploadRoutes(page, {
      testId: "test-upload-001",
      images: MOCK_IMAGES_EMPTY,
    });
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    // The page-level "additional uploader" block is always present.
    const additionalUploader = page.locator("#additionalUploader");
    await expect(additionalUploader).toBeVisible();

    // The Upload button is now a <cts-button class="uploadBtn">.
    const uploadBtn = additionalUploader.locator("cts-button.uploadBtn");
    await expect(uploadBtn).toHaveCount(1);
    await expect(uploadBtn).toHaveAttribute("label", "Upload");

    // The cts-button renders an inner <button> that is disabled initially.
    const innerBtn = uploadBtn.locator("button");
    await expect(innerBtn).toBeDisabled();
    await expect(innerBtn).toHaveText("Upload");
  });

  test("file-picker label-wrapping-input pattern still renders as raw HTML", async ({ page }) => {
    await setupFailFast(page);
    await setupUploadRoutes(page, {
      testId: "test-upload-001",
      images: MOCK_IMAGES_EMPTY,
    });
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    // The file-picker label is NOT wrapped in a cts-button because the
    // label-wrapping-input pattern is file-picker specific and out-of-scope
    // for the cts-button migration. After U34 the label uses the
    // OIDF-token-styled .upload-filepicker class instead of .btn.btn-light.
    const label = page.locator("#additionalUploader label.upload-filepicker").first();
    await expect(label).toBeVisible();
    await expect(label).toContainText("Select File");

    const fileInput = label.locator('input[type="file"]');
    await expect(fileInput).toHaveAttribute("hidden", "");
    await expect(fileInput).toHaveAttribute("accept", ".jpg,.jpeg,.png,image/png,image/jpeg");
  });

  test("pending image (from template) renders the Upload cts-button", async ({ page }) => {
    await setupFailFast(page);
    await setupUploadRoutes(page, {
      testId: "test-upload-001",
      images: MOCK_IMAGES_PENDING,
    });
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    // The pendingImageUploader.html template is injected via FAPI_UI.logTemplates.PENDING
    // for each image entry with `upload: ...`.
    const pendingItem = page.locator('#imageBlocks [data-upload-id="placeholder-1"]').first();
    await expect(pendingItem).toBeVisible();

    // The template's Upload button is now a <cts-button class="uploadBtn">.
    const pendingUploadBtn = pendingItem.locator("cts-button.uploadBtn");
    await expect(pendingUploadBtn).toHaveCount(1);
    await expect(pendingUploadBtn).toHaveAttribute("label", "Upload");

    // And it renders a disabled inner <button>.
    await expect(pendingUploadBtn.locator("button")).toBeDisabled();
  });

  test("setting .disabled=false on cts-button.uploadBtn enables the inner button", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupUploadRoutes(page, {
      testId: "test-upload-001",
      images: MOCK_IMAGES_EMPTY,
    });
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    const uploadBtn = page.locator("#additionalUploader cts-button.uploadBtn");
    const innerBtn = uploadBtn.locator("button");

    // Disabled by default.
    await expect(innerBtn).toBeDisabled();

    // The page's JS enables the button by setting `.disabled = false` on the
    // queried cts-button element. Simulate that path directly (Lit re-renders
    // the inner <button> accordingly).
    await uploadBtn.evaluate((el) => {
      /** @type {HTMLElement & { disabled?: boolean }} */ (el).disabled = false;
    });

    await expect(innerBtn).toBeEnabled();
  });

  test("page renders cts-page-head and cts-form-field for the description input", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupUploadRoutes(page, {
      testId: "test-upload-001",
      images: MOCK_IMAGES_EMPTY,
    });
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    // U34 composes the page via cts-page-head + cts-form-field. Verify the
    // page-head heading shows up with the expected title text and that the
    // description field is rendered as a labelled cts-form-field with the
    // schema-derived "Description" label.
    const pageHead = page.locator("cts-page-head");
    await expect(pageHead).toBeVisible();
    await expect(pageHead).toContainText("Image Uploader");

    const field = page.locator("#additionalUploader cts-form-field");
    await expect(field).toHaveCount(1);
    // cts-form-field renders an <input class="oidf-input"> in light DOM
    // and a sibling <span class="oidf-label"> driven by schema.title.
    await expect(field.locator("span.oidf-label")).toContainText("Description");
    await expect(field.locator("input.oidf-input")).toBeVisible();
  });

  test("clicking Upload with no description surfaces the error modal", async ({ page }) => {
    await setupFailFast(page);
    await setupUploadRoutes(page, {
      testId: "test-upload-001",
      images: MOCK_IMAGES_EMPTY,
    });
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    // Force-enable the Upload button — the page only enables it after a file
    // is selected, but the description-required validation runs on click
    // independently of file selection.
    const uploadBtn = page.locator("#additionalUploader cts-button.uploadBtn");
    await uploadBtn.evaluate((el) => {
      /** @type {HTMLElement & { disabled?: boolean }} */ (el).disabled = false;
    });

    // Click the rendered inner button (host.click() does not fire the inner
    // click handler — see components/AGENTS.md §2).
    await uploadBtn.locator("button").click();

    // The validation surfaces through FAPI_UI.showError() → #errorModal.
    const errorModal = page.locator("#errorModal");
    await expect(errorModal).toBeVisible();
    await expect(errorModal).toContainText("Description required");
  });

  test("server error on initial load surfaces through the errorModal (cts-modal)", async ({
    page,
  }) => {
    await setupFailFast(page);

    // Fail the images list load to trigger FAPI_UI.showError() → #errorModal.
    await page.route("**/api/log/test-upload-001/images", (route) =>
      route.fulfill({
        status: 400,
        contentType: "application/json",
        body: JSON.stringify({ error: "Invalid log id" }),
      }),
    );
    await page.route("**/api/info/test-upload-001", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(MOCK_UPLOAD_TEST_INFO),
      }),
    );
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    // upload.html uses FAPI_UI.showError() for error display (not an inline
    // cts-alert). The error surface is the #errorModal cts-modal.
    const errorModal = page.locator("#errorModal");
    await expect(errorModal).toBeVisible();
    await expect(errorModal).toContainText("Invalid log id");
  });
});
