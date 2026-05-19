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
 * upload.html consumes:
 *   - /api/log/:testId/images       (GET) — list of pending/existing entries
 *   - /api/info/:testId             (GET) — test info for the page header
 *   - /api/log/:testId/images/:slot (POST) — fixed-slot upload submit
 *   - /api/log/:testId/images       (POST) — ad-hoc additional upload submit
 *
 * @param {import('@playwright/test').Page} page
 * @param {{ testId: string; images: unknown; uploadStatus?: number }} options
 */
async function setupUploadRoutes(page, { testId, images, uploadStatus = 200 }) {
  await page.route(`**/api/log/${testId}/images*`, (/** @type {any} */ route) => {
    if (route.request().method() === "GET") {
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(images),
      });
    }
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

  await page.route(`**/api/info/${testId}`, (/** @type {any} */ route) =>
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

  test("renders a single cts-image-upload instance hooked to the test id", async ({ page }) => {
    await setupFailFast(page);
    await setupUploadRoutes(page, {
      testId: "test-upload-001",
      images: MOCK_IMAGES_EMPTY,
    });
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    // The page wraps cts-image-upload in a headerless <cts-card>. Assert
    // the card chrome is present so a future revert of the wrapper surfaces
    // as a test failure rather than passing silently. toBeVisible() is
    // strictly stronger than toHaveCount(1) — it also catches a card that
    // exists in the DOM but is hidden (display:none, zero dimensions, etc.).
    await expect(page.locator("cts-card")).toBeVisible();

    const uploader = page.locator("cts-image-upload");
    await expect(uploader).toHaveCount(1);

    // The page sets testId as a property (no attribute reflection).
    await expect
      .poll(async () => uploader.evaluate((el) => /** @type {any} */ (el).testId))
      .toBe("test-upload-001");
  });

  test("empty server list still offers an ad-hoc editable-description slot", async ({ page }) => {
    await setupFailFast(page);
    await setupUploadRoutes(page, {
      testId: "test-upload-001",
      images: MOCK_IMAGES_EMPTY,
    });
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    // One pending image (the synthetic additional uploader) and zero existing.
    const uploader = page.locator("cts-image-upload");
    await expect(uploader.locator('[data-testid="pending-image"]')).toHaveCount(1);

    // The slot renders the editable description input.
    const desc = uploader.locator(".oidf-image-upload__description-input");
    await expect(desc).toBeVisible();
    await expect(desc).toHaveAttribute("required", "");

    // Upload button starts disabled (no file + no description).
    const upload = uploader.locator("button.oidf-image-upload__upload-btn");
    await expect(upload).toBeDisabled();
  });

  test("server-provided pending entry renders read-only description", async ({ page }) => {
    await setupFailFast(page);
    await setupUploadRoutes(page, {
      testId: "test-upload-001",
      images: MOCK_IMAGES_PENDING,
    });
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    const uploader = page.locator("cts-image-upload");
    // One server pending + one synthetic additional (total < limit).
    await expect(uploader.locator('[data-testid="pending-image"]')).toHaveCount(2);

    // The server pending renders the spec message as a read-only label.
    await expect(uploader).toContainText(
      "Upload a screenshot showing the browser when the authorization endpoint was reached.",
    );

    // Exactly one description-input on the page (only the editable slot has one).
    await expect(uploader.locator(".oidf-image-upload__description-input")).toHaveCount(1);
  });

  test("page-head and test info render from /api/info", async ({ page }) => {
    await setupFailFast(page);
    await setupUploadRoutes(page, {
      testId: "test-upload-001",
      images: MOCK_IMAGES_EMPTY,
    });
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    const pageHead = page.locator("cts-page-head");
    await expect(pageHead).toBeVisible();
    await expect(pageHead).toContainText("Image Uploader");

    const testInfo = page.locator("#testInfo");
    await expect(testInfo).toContainText("oidcc-server");
    await expect(testInfo).toContainText("test-upload-001");
    await expect(testInfo).toContainText("Upload screenshots for this test run");
  });

  test("dropping a valid PNG on the inline zone enables Upload (with description)", async ({
    page,
  }) => {
    await setupFailFast(page);
    await setupUploadRoutes(page, {
      testId: "test-upload-001",
      images: MOCK_IMAGES_EMPTY,
    });
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    const uploader = page.locator("cts-image-upload");
    const zone = uploader.locator('label[data-testid="inline-dropzone"]');
    await expect(zone).toBeVisible();

    const upload = uploader.locator("button.oidf-image-upload__upload-btn");
    await expect(upload).toBeDisabled();

    // Type a description.
    await uploader
      .locator(".oidf-image-upload__description-input")
      .fill("Manual additional screenshot");

    // Synthesize a drop with a small valid PNG.
    await zone.evaluate((el) => {
      // 1x1 transparent PNG header — decodes as a real image so the preview
      // doesn't render the broken-image glyph during interactive review.
      const PNG = atob(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=",
      );
      const buf = new Uint8Array(PNG.length);
      for (let i = 0; i < PNG.length; i++) buf[i] = PNG.charCodeAt(i);
      const file = new File([buf], "drop.png", { type: "image/png" });
      const dt = new DataTransfer();
      dt.items.add(file);
      el.dispatchEvent(new DragEvent("drop", { bubbles: true, dataTransfer: dt }));
    });

    // FileReader is async — wait for the Upload button to flip.
    await expect(upload).toBeEnabled();
  });

  test("successful upload surfaces a non-blocking 'Image uploaded' toast", async ({ page }) => {
    await setupFailFast(page);
    await setupUploadRoutes(page, {
      testId: "test-upload-001",
      images: MOCK_IMAGES_EMPTY,
    });
    await setupCommonRoutes(page);

    await page.goto("/upload.html?log=test-upload-001");

    // The host mount must be on the page before the upload event fires —
    // the cross-page wiring promises an always-mounted region per page.
    await expect(page.locator("cts-toast-host")).toHaveCount(1);

    const uploader = page.locator("cts-image-upload");
    await expect(uploader).toBeVisible();

    // The page's `cts-image-uploaded` listener calls window.ctsToast +
    // refreshImages. Dispatching the event here exercises the exact
    // seam the plan's R7 smoke-test migration touches, without depending
    // on cts-image-upload's internal POST plumbing.
    await uploader.evaluate((el) => {
      el.dispatchEvent(new CustomEvent("cts-image-uploaded", { bubbles: true }));
    });

    const toast = page.locator("cts-toast-host cts-toast");
    await expect(toast).toBeVisible();
    await expect(toast).toContainText("Image uploaded");
    // `kind: "ok"` resolves to the status-pass left rule (green) — the
    // inline style on `.oidf-toast` carries the CSS custom-property name.
    await expect(toast.locator(".oidf-toast")).toHaveAttribute("style", /--status-pass/);

    // Default duration is 5000ms. Allow a small buffer to absorb the
    // fade-out transition (200ms) and any scheduler jitter so the
    // assertion is robust without inflating per-test cost.
    await expect(toast).toHaveCount(0, { timeout: 6000 });
  });

  test("server error on initial load surfaces through the errorModal", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/log/test-upload-001/images*", (route) =>
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

    const errorModal = page.locator("#errorModal");
    await expect(errorModal).toBeVisible();
    await expect(errorModal).toContainText("Invalid log id");
  });
});
