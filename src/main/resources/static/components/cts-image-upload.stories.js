import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import { http, HttpResponse } from "msw";
import "./cts-image-upload.js";

export default {
  title: "Components/cts-image-upload",
  component: "cts-image-upload",
  argTypes: {
    layout: {
      control: { type: "inline-radio" },
      options: ["hero", "inline", "card"],
      description:
        "Visual treatment for each pending image. `hero` (default) shows a large drop zone above the upload action; `inline` puts the zone in the thumbnail slot for compact lists; `card` uses a tall preview-first card.",
    },
  },
  args: {
    layout: "hero",
  },
};

/** @type {{ name: string; description: string }[]} */
const MOCK_PENDING_IMAGES = [
  { name: "screenshot-login", description: "Screenshot of the login page after authentication" },
  { name: "screenshot-consent", description: "Screenshot of the consent page" },
];

/** @type {{ name: string; url: string }[]} */
const MOCK_EXISTING_IMAGES = [
  { name: "screenshot-result", url: "images/placeholder.jpg" },
  { name: "screenshot-token", url: "images/placeholder.jpg" },
];

/**
 * Build a synthetic PNG `File`. The conformance suite enforces JPEG/PNG via
 * MIME type, so we lie about the body bytes — only `type` and `size` are
 * checked client-side.
 *
 * @param {string} name
 * @param {number} bytes - target file size, default 8 KB
 * @returns {File}
 */
function makeMockPng(name, bytes = 8 * 1024) {
  const buf = new Uint8Array(bytes);
  return new File([buf], name, { type: "image/png" });
}

/**
 * Build a DataTransfer carrying the provided files. Used to simulate drag
 * sources for `dragenter` / `dragover` / `drop` synthetic events.
 *
 * @param {File[]} files
 */
function makeDataTransfer(files) {
  const dt = new DataTransfer();
  for (const f of files) dt.items.add(f);
  return dt;
}

/**
 * Walk the DOM tree for an element whose tag and content match the expected
 * Replace affordance. Used because hero/card layouts only render Replace
 * after a file is selected.
 */
async function waitForReplace(root) {
  return waitFor(() => {
    const btn = root.querySelector(".oidf-image-upload__replace-btn");
    expect(btn).toBeTruthy();
    return btn;
  });
}

// --- Stories ---

export const PendingImages = {
  args: { layout: "hero" },
  render: (args) => html`
    <cts-image-upload
      test-id="test-abc-123"
      layout="${args.layout}"
      .pendingImages=${MOCK_PENDING_IMAGES}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Both pending images render
    const pendingBlocks = canvasElement.querySelectorAll('[data-testid="pending-image"]');
    expect(pendingBlocks.length).toBe(2);

    // Description text displayed
    expect(
      canvas.getByText("Screenshot of the login page after authentication"),
    ).toBeInTheDocument();
    expect(canvas.getByText("Screenshot of the consent page")).toBeInTheDocument();

    // Each block renders a disabled Upload button
    const uploadButtons = canvas.getAllByRole("button", { name: /^upload$/i });
    expect(uploadButtons.length).toBe(2);
    for (const btn of uploadButtons) {
      expect(/** @type {HTMLButtonElement} */ (btn).disabled).toBe(true);
    }

    // Default layout renders the hero drop zone with role="button" and tabindex
    const dropzones = canvasElement.querySelectorAll('[data-testid="hero-dropzone"]');
    expect(dropzones.length).toBe(2);
    const dropzone = /** @type {HTMLElement} */ (dropzones[0]);
    expect(dropzone.getAttribute("role")).toBe("button");
    expect(dropzone.getAttribute("tabindex")).toBe("0");
    expect(dropzone.getAttribute("aria-describedby")).toMatch(/cts-image-upload-\d+-hint-/);

    // No "All images uploaded" message
    expect(canvas.queryByText("All images uploaded")).toBeNull();
  },
};

export const LayoutHero = {
  args: { layout: "hero" },
  render: (args) => html`
    <cts-image-upload
      test-id="test-hero"
      layout="${args.layout}"
      .pendingImages=${[MOCK_PENDING_IMAGES[0]]}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const dropzone = /** @type {HTMLElement} */ (
      canvasElement.querySelector('[data-testid="hero-dropzone"]')
    );
    expect(dropzone).toBeTruthy();
    expect(dropzone.getAttribute("role")).toBe("button");
    expect(dropzone.getAttribute("tabindex")).toBe("0");

    // Dragover transitions to the highlight state
    const file = makeMockPng("hero-drop.png");
    dropzone.dispatchEvent(
      new DragEvent("dragenter", { bubbles: true, dataTransfer: makeDataTransfer([file]) }),
    );
    await waitFor(() => {
      expect(dropzone.classList.contains("oidf-image-upload__hero-zone--dragover")).toBe(true);
    });

    // Dropping the file enables the Upload button and renders the preview row
    dropzone.dispatchEvent(
      new DragEvent("drop", { bubbles: true, dataTransfer: makeDataTransfer([file]) }),
    );
    const canvas = within(canvasElement);
    await waitFor(() => {
      const btn = canvas.getByRole("button", { name: /^upload$/i });
      expect(/** @type {HTMLButtonElement} */ (btn).disabled).toBe(false);
    });

    // The hero preview row replaces the empty drop zone
    await waitForReplace(canvasElement);
    expect(canvasElement.querySelector('[data-testid="hero-dropzone"]')).toBeNull();
  },
};

export const LayoutInline = {
  args: { layout: "inline" },
  render: (args) => html`
    <cts-image-upload
      test-id="test-inline"
      layout="${args.layout}"
      .pendingImages=${MOCK_PENDING_IMAGES}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    // Inline layout uses a <label> wrapping the hidden input — keyboard
    // accessibility comes from the native label/input pairing.
    const zones = canvasElement.querySelectorAll('label[data-testid="inline-dropzone"]');
    expect(zones.length).toBe(2);
    const zone = /** @type {HTMLLabelElement} */ (zones[0]);
    expect(zone.tagName).toBe("LABEL");
    const innerInput = zone.querySelector('input[type="file"]');
    expect(innerInput).toBeTruthy();

    // The fallback "Select file…" picker button is still rendered as a
    // visible affordance for users who don't recognise the drop zone.
    const canvas = within(canvasElement);
    const pickerLabels = canvas.getAllByText(/Select file/);
    expect(pickerLabels.length).toBe(2);

    // Simulating a drop on the inline zone enables Upload.
    const file = makeMockPng("inline-drop.png");
    zone.dispatchEvent(
      new DragEvent("drop", { bubbles: true, dataTransfer: makeDataTransfer([file]) }),
    );
    await waitFor(() => {
      const btns = canvas.getAllByRole("button", { name: /^upload$/i });
      expect(/** @type {HTMLButtonElement} */ (btns[0]).disabled).toBe(false);
    });

    // The thumbnail slot now shows the preview image inside the zone.
    await waitFor(() => {
      const thumb = zone.querySelector("img.oidf-image-upload__thumb");
      expect(thumb).toBeTruthy();
    });
  },
};

export const LayoutCard = {
  args: { layout: "card" },
  render: (args) => html`
    <cts-image-upload
      test-id="test-card"
      layout="${args.layout}"
      .pendingImages=${[MOCK_PENDING_IMAGES[0]]}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const zone = /** @type {HTMLElement} */ (
      canvasElement.querySelector('[data-testid="card-dropzone"]')
    );
    expect(zone).toBeTruthy();
    expect(zone.getAttribute("role")).toBe("button");

    // Dropping a file flips the zone into the preview state and exposes a Replace button.
    const file = makeMockPng("card-drop.png");
    zone.dispatchEvent(
      new DragEvent("drop", { bubbles: true, dataTransfer: makeDataTransfer([file]) }),
    );

    const replace = await waitForReplace(canvasElement);
    expect(replace.textContent).toContain("Replace");

    // After file selection, the filled zone no longer carries role=button
    // (the Replace button is the only interactive child).
    const filledZone = canvasElement.querySelector('[data-testid="card-dropzone"]');
    expect(filledZone).toBeTruthy();
    expect(filledZone.getAttribute("role")).toBeNull();

    // Upload becomes enabled.
    const canvas = within(canvasElement);
    const upload = canvas.getByRole("button", { name: /^upload$/i });
    expect(/** @type {HTMLButtonElement} */ (upload).disabled).toBe(false);
  },
};

export const KeyboardActivation = {
  args: { layout: "hero" },
  render: (args) => html`
    <cts-image-upload
      test-id="test-keyboard"
      layout="${args.layout}"
      .pendingImages=${[MOCK_PENDING_IMAGES[0]]}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const dropzone = /** @type {HTMLElement} */ (
      canvasElement.querySelector('[data-testid="hero-dropzone"]')
    );
    const hiddenInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector('input[type="file"][data-image-name="screenshot-login"]')
    );

    /** @type {Event[]} */
    const clicks = [];
    hiddenInput.addEventListener("click", (e) => {
      clicks.push(e);
      // Prevent the OS dialog from actually opening in the test runner.
      e.preventDefault();
    });

    dropzone.focus();
    dropzone.dispatchEvent(new KeyboardEvent("keydown", { key: "Enter", bubbles: true }));
    await waitFor(() => {
      expect(clicks.length).toBeGreaterThanOrEqual(1);
    });

    dropzone.dispatchEvent(new KeyboardEvent("keydown", { key: " ", bubbles: true }));
    await waitFor(() => {
      expect(clicks.length).toBeGreaterThanOrEqual(2);
    });
  },
};

export const InvalidFileType = {
  args: { layout: "hero" },
  render: (args) => html`
    <cts-image-upload
      test-id="test-invalid-type"
      layout="${args.layout}"
      .pendingImages=${[MOCK_PENDING_IMAGES[0]]}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const dropzone = /** @type {HTMLElement} */ (
      canvasElement.querySelector('[data-testid="hero-dropzone"]')
    );

    // Drop a PDF — the component must reject it and show the error alert.
    const bogus = new File([new Uint8Array(8)], "not-an-image.pdf", { type: "application/pdf" });
    dropzone.dispatchEvent(
      new DragEvent("drop", { bubbles: true, dataTransfer: makeDataTransfer([bogus]) }),
    );

    await waitFor(() => {
      const alert = canvasElement.querySelector(".oidf-image-upload__alert--error");
      expect(alert).toBeTruthy();
      expect(alert.textContent).toContain("not a supported type");
    });

    // The upload button remains disabled (no valid file was stored).
    const canvas = within(canvasElement);
    const upload = canvas.getByRole("button", { name: /^upload$/i });
    expect(/** @type {HTMLButtonElement} */ (upload).disabled).toBe(true);
  },
};

export const NoPendingImages = {
  args: { layout: "hero" },
  render: (args) => html`
    <cts-image-upload
      test-id="test-abc-123"
      layout="${args.layout}"
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
    expect(status.classList.contains("oidf-image-upload__alert")).toBe(true);

    // No file inputs rendered
    const pendingBlocks = canvasElement.querySelectorAll('[data-testid="pending-image"]');
    expect(pendingBlocks.length).toBe(0);
  },
};

export const ExistingImages = {
  args: { layout: "hero" },
  render: (args) => html`
    <cts-image-upload
      test-id="test-abc-123"
      layout="${args.layout}"
      .existingImages=${MOCK_EXISTING_IMAGES}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    expect(canvas.getByText("Uploaded Images")).toBeInTheDocument();

    const existingBlocks = canvasElement.querySelectorAll('[data-testid="existing-image"]');
    expect(existingBlocks.length).toBe(2);

    expect(canvas.getByText("screenshot-result")).toBeInTheDocument();
    expect(canvas.getByText("screenshot-token")).toBeInTheDocument();

    const successBlocks = canvasElement.querySelectorAll(".oidf-image-upload__status--uploaded");
    expect(successBlocks.length).toBe(2);

    const imgs = canvasElement.querySelectorAll('[data-testid="existing-image"] img');
    expect(imgs.length).toBe(2);
    for (const img of imgs) {
      expect(img.getAttribute("src")).toBe("images/placeholder.jpg");
    }

    const status = canvas.getByRole("status");
    expect(status).toBeInTheDocument();
    expect(status.textContent).toContain("All images uploaded");
  },
};

export const UploadSuccess = {
  args: { layout: "hero" },
  parameters: {
    msw: {
      handlers: [
        http.post("/api/log/test-upload-ok/images/:imageName", () =>
          HttpResponse.json({ success: true }),
        ),
      ],
    },
  },
  render: (args) => html`
    <cts-image-upload
      test-id="test-upload-ok"
      layout="${args.layout}"
      .pendingImages=${[MOCK_PENDING_IMAGES[0]]}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    const fileInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector('input[type="file"]')
    );
    expect(fileInput).toBeTruthy();

    const file = new File(["fake image data"], "test-screenshot.png", { type: "image/png" });
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    fileInput.files = dataTransfer.files;
    fileInput.dispatchEvent(new Event("change", { bubbles: true }));

    await waitFor(
      () => {
        const uploadBtn = canvas.getByRole("button", { name: /^upload$/i });
        expect(/** @type {HTMLButtonElement} */ (uploadBtn).disabled).toBe(false);
      },
      { timeout: 3000 },
    );

    /** @type {any} */
    let uploadEvent = null;
    canvasElement.addEventListener("cts-image-uploaded", (e) => {
      uploadEvent = /** @type {CustomEvent} */ (e).detail;
    });

    const uploadBtn = canvas.getByRole("button", { name: /^upload$/i });
    await uploadBtn.click();

    await waitFor(
      () => {
        expect(uploadEvent).toBeTruthy();
      },
      { timeout: 3000 },
    );

    expect(uploadEvent.testId).toBe("test-upload-ok");
    expect(uploadEvent.imageName).toBe("screenshot-login");

    const alert = canvasElement.querySelector(".oidf-image-upload__alert--error");
    expect(alert).toBeNull();
  },
};

export const UploadError = {
  args: { layout: "hero" },
  parameters: {
    msw: {
      handlers: [
        http.post("/api/log/test-upload-fail/images/:imageName", () =>
          HttpResponse.json({ error: "Internal server error" }, { status: 500 }),
        ),
      ],
    },
  },
  render: (args) => html`
    <cts-image-upload
      test-id="test-upload-fail"
      layout="${args.layout}"
      .pendingImages=${[MOCK_PENDING_IMAGES[0]]}
    ></cts-image-upload>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    const fileInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector('input[type="file"]')
    );
    expect(fileInput).toBeTruthy();

    const file = new File(["fake image data"], "test-screenshot.png", { type: "image/png" });
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    fileInput.files = dataTransfer.files;
    fileInput.dispatchEvent(new Event("change", { bubbles: true }));

    await waitFor(
      () => {
        const uploadBtn = canvas.getByRole("button", { name: /^upload$/i });
        expect(/** @type {HTMLButtonElement} */ (uploadBtn).disabled).toBe(false);
      },
      { timeout: 3000 },
    );

    const uploadBtn = canvas.getByRole("button", { name: /^upload$/i });
    await uploadBtn.click();

    await waitFor(
      () => {
        const alert = canvasElement.querySelector(".oidf-image-upload__alert--error");
        expect(alert).toBeTruthy();
        expect(alert.textContent).toContain("Internal server error");
      },
      { timeout: 3000 },
    );
  },
};
