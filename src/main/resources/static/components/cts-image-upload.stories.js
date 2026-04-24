import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import { http, HttpResponse } from "msw";
import "./cts-image-upload.js";

export default {
  title: "Components/cts-image-upload",
  component: "cts-image-upload",
};

const MOCK_PENDING_IMAGES = [
  { name: "screenshot-login", description: "Screenshot of the login page after authentication" },
  { name: "screenshot-consent", description: "Screenshot of the consent page" },
];

const MOCK_EXISTING_IMAGES = [
  { name: "screenshot-result", url: "images/placeholder.jpg" },
  { name: "screenshot-token", url: "images/placeholder.jpg" },
];

// --- Stories ---

export const PendingImages = {
  render: () => html`
    <cts-image-upload
      test-id="test-abc-123"
      .pendingImages=${MOCK_PENDING_IMAGES}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Both pending images render file inputs
    const pendingBlocks = canvasElement.querySelectorAll('[data-testid="pending-image"]');
    expect(pendingBlocks.length).toBe(2);

    // Description text displayed
    expect(
      canvas.getByText("Screenshot of the login page after authentication"),
    ).toBeInTheDocument();
    expect(canvas.getByText("Screenshot of the consent page")).toBeInTheDocument();

    // Upload instructions present
    const instructions = canvas.getAllByText(/Please upload a screenshot/);
    expect(instructions.length).toBe(2);

    // Each block has Select File button and disabled Upload button
    const selectButtons = canvas.getAllByText(/Select File/);
    expect(selectButtons.length).toBe(2);

    const uploadButtons = canvas.getAllByText("Upload");
    expect(uploadButtons.length).toBe(2);
    for (const btn of uploadButtons) {
      expect(/** @type {HTMLButtonElement} */ (btn).disabled).toBe(true);
    }

    // No "All images uploaded" message
    expect(canvas.queryByText("All images uploaded")).toBeNull();
  },
};

export const NoPendingImages = {
  render: () => html`
    <cts-image-upload
      test-id="test-abc-123"
      .pendingImages=${[]}
      .existingImages=${[]}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // "All images uploaded" message shown
    const status = canvas.getByRole("status");
    expect(status).toBeInTheDocument();
    expect(status.textContent).toContain("All images uploaded");

    // No file inputs rendered
    const pendingBlocks = canvasElement.querySelectorAll('[data-testid="pending-image"]');
    expect(pendingBlocks.length).toBe(0);
  },
};

export const ExistingImages = {
  render: () => html`
    <cts-image-upload
      test-id="test-abc-123"
      .existingImages=${MOCK_EXISTING_IMAGES}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Existing images heading present
    expect(canvas.getByText("Uploaded Images")).toBeInTheDocument();

    // Thumbnails rendered
    const existingBlocks = canvasElement.querySelectorAll('[data-testid="existing-image"]');
    expect(existingBlocks.length).toBe(2);

    // Image names displayed
    expect(canvas.getByText("screenshot-result")).toBeInTheDocument();
    expect(canvas.getByText("screenshot-token")).toBeInTheDocument();

    // Success status blocks with check icons
    const successBlocks = canvasElement.querySelectorAll(".bg-success");
    expect(successBlocks.length).toBe(2);

    // Images have src attributes
    const imgs = canvasElement.querySelectorAll('[data-testid="existing-image"] img');
    expect(imgs.length).toBe(2);
    for (const img of imgs) {
      expect(img.getAttribute("src")).toBe("images/placeholder.jpg");
    }

    // "All images uploaded" message shown since no pending images
    const status = canvas.getByRole("status");
    expect(status).toBeInTheDocument();
    expect(status.textContent).toContain("All images uploaded");
  },
};

export const UploadSuccess = {
  parameters: {
    msw: {
      handlers: [
        http.post("/api/log/test-upload-ok/images/:imageName", () =>
          HttpResponse.json({ success: true }),
        ),
      ],
    },
  },
  render: () => html`
    <cts-image-upload
      test-id="test-upload-ok"
      .pendingImages=${[MOCK_PENDING_IMAGES[0]]}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Find the file input
    const fileInput = canvasElement.querySelector('input[type="file"]');
    expect(fileInput).toBeTruthy();

    // Create a mock file
    const file = new File(["fake image data"], "test-screenshot.png", {
      type: "image/png",
    });

    // Simulate file selection via native change event
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    fileInput.files = dataTransfer.files;
    fileInput.dispatchEvent(new Event("change", { bubbles: true }));

    // Wait for the preview and upload button to become enabled
    await waitFor(
      () => {
        const uploadBtn = canvas.getByText("Upload");
        expect(/** @type {HTMLButtonElement} */ (uploadBtn).disabled).toBe(false);
      },
      { timeout: 3000 },
    );

    // Listen for the upload event
    /** @type {any} */
    let uploadEvent = null;
    canvasElement.addEventListener("cts-image-uploaded", (e) => {
      uploadEvent = /** @type {CustomEvent} */ (e).detail;
    });

    // Click upload
    const uploadBtn = canvas.getByText("Upload");
    await uploadBtn.click();

    // Wait for upload to complete
    await waitFor(
      () => {
        expect(uploadEvent).toBeTruthy();
      },
      { timeout: 3000 },
    );

    expect(uploadEvent.testId).toBe("test-upload-ok");
    expect(uploadEvent.imageName).toBe("screenshot-login");

    // No error message
    const alert = canvasElement.querySelector(".alert-danger");
    expect(alert).toBeNull();
  },
};

export const UploadError = {
  parameters: {
    msw: {
      handlers: [
        http.post("/api/log/test-upload-fail/images/:imageName", () =>
          HttpResponse.json({ error: "Internal server error" }, { status: 500 }),
        ),
      ],
    },
  },
  render: () => html`
    <cts-image-upload
      test-id="test-upload-fail"
      .pendingImages=${[MOCK_PENDING_IMAGES[0]]}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Find the file input
    const fileInput = canvasElement.querySelector('input[type="file"]');
    expect(fileInput).toBeTruthy();

    // Create a mock file
    const file = new File(["fake image data"], "test-screenshot.png", {
      type: "image/png",
    });

    // Simulate file selection via native change event
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    fileInput.files = dataTransfer.files;
    fileInput.dispatchEvent(new Event("change", { bubbles: true }));

    // Wait for upload button to become enabled
    await waitFor(
      () => {
        const uploadBtn = canvas.getByText("Upload");
        expect(/** @type {HTMLButtonElement} */ (uploadBtn).disabled).toBe(false);
      },
      { timeout: 3000 },
    );

    // Click upload
    const uploadBtn = canvas.getByText("Upload");
    await uploadBtn.click();

    // Wait for error message to appear
    await waitFor(
      () => {
        const alert = canvasElement.querySelector(".alert-danger");
        expect(alert).toBeTruthy();
        expect(alert.textContent).toContain("Internal server error");
      },
      { timeout: 3000 },
    );
  },
};
