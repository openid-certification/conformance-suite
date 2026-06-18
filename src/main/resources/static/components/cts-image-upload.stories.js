import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import { http, HttpResponse } from "msw";
import "./cts-image-upload.js";

export default {
  title: "Components/cts-image-upload",
  component: "cts-image-upload",
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

// A valid 1x1 transparent PNG. Stories prepend these bytes so previews
// decode as a real image (otherwise the FileReader data-URL shows the
// browser's broken-image glyph). PNG decoders stop at IEND, so trailing
// padding bytes from the `bytes` knob below are visually ignored — only
// `.size` is affected, which is what the validation tests care about.
const TINY_PNG_BASE64 =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";

function tinyPngBytes() {
  const bin = atob(TINY_PNG_BASE64);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

/**
 * Build a synthetic PNG `File` with a real decodable header so the live
 * Storybook preview shows an actual image. The `bytes` knob controls total
 * file size for validating the 500 KB cap.
 *
 * @param {string} name
 * @param {number} bytes - target file size, default 8 KB
 * @returns {File}
 */
function makeMockPng(name, bytes = 8 * 1024) {
  const png = tinyPngBytes();
  const total = Math.max(png.length, bytes);
  const buf = new Uint8Array(total);
  buf.set(png, 0);
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

// --- Stories ---

export const PendingImages = {
  render: () => html`
    <cts-image-upload
      test-id="test-abc-123"
      .pendingImages=${MOCK_PENDING_IMAGES}
    ></cts-image-upload>
  `,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("both pending images render", async () => {
      const pendingBlocks = canvasElement.querySelectorAll('[data-testid="pending-image"]');
      expect(pendingBlocks.length).toBe(2);
    });

    await step("description text displayed", async () => {
      expect(
        canvas.getByText("Screenshot of the login page after authentication"),
      ).toBeInTheDocument();
      expect(canvas.getByText("Screenshot of the consent page")).toBeInTheDocument();
    });

    await step("each block renders a disabled Upload button (no file selected yet)", async () => {
      const uploadButtons = canvas.getAllByRole("button", { name: /^upload$/i });
      expect(uploadButtons.length).toBe(2);
      for (const btn of uploadButtons) {
        expect(/** @type {HTMLButtonElement} */ (btn).disabled).toBe(true);
      }
    });

    await step("inline drop zones render with native file inputs", async () => {
      // Keyboard activation comes from the native <label>/<input type="file">
      // pairing inside each zone.
      const zones = canvasElement.querySelectorAll('label[data-testid="inline-dropzone"]');
      expect(zones.length).toBe(2);
      for (const zone of zones) {
        expect(zone.tagName).toBe("LABEL");
        expect(zone.querySelector('input[type="file"]')).toBeTruthy();
      }
    });

    await step('fallback "Select file…" picker buttons render', async () => {
      // Visible affordance for users who don't recognize the drop zone.
      const pickerLabels = canvas.getAllByText(/Select file/);
      expect(pickerLabels.length).toBe(2);
    });

    await step('no "All images uploaded" message', async () => {
      expect(canvas.queryByText("All images uploaded")).toBeNull();
    });
  },
};

/**
 * Drop a file on the inline zone — the preview thumbnail replaces the empty
 * state and Upload becomes enabled.
 */
export const DropToEnableUpload = {
  render: () => html`
    <cts-image-upload
      test-id="test-drop"
      .pendingImages=${[MOCK_PENDING_IMAGES[0]]}
    ></cts-image-upload>
  `,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    const zone = /** @type {HTMLLabelElement} */ (
      canvasElement.querySelector('label[data-testid="inline-dropzone"]')
    );
    expect(zone).toBeTruthy();
    const file = makeMockPng("inline-drop.png");

    await step("dragenter transitions to the highlight state", async () => {
      zone.dispatchEvent(
        new DragEvent("dragenter", { bubbles: true, dataTransfer: makeDataTransfer([file]) }),
      );
      await waitFor(() => {
        expect(zone.classList.contains("oidf-image-upload__inline-zone--dragover")).toBe(true);
      });
    });

    await step(
      "dropping enables Upload and renders the preview thumb inside the zone",
      async () => {
        zone.dispatchEvent(
          new DragEvent("drop", { bubbles: true, dataTransfer: makeDataTransfer([file]) }),
        );
        await waitFor(() => {
          const btn = canvas.getByRole("button", { name: /^upload$/i });
          expect(/** @type {HTMLButtonElement} */ (btn).disabled).toBe(false);
        });
        await waitFor(() => {
          const thumb = zone.querySelector("img.oidf-image-upload__thumb");
          expect(thumb).toBeTruthy();
        });
      },
    );
  },
};

/**
 * Additional-uploader use case: the user must type their own description
 * before the upload can fire. The description input sits beside an inline
 * drop zone and an existing list of uploaded thumbnails.
 */
export const EditableDescription = {
  render: () => html`
    <cts-image-upload
      test-id="test-editable"
      .pendingImages=${[
        {
          name: "additional-screenshot",
          editableDescription: true,
        },
      ]}
      .existingImages=${MOCK_EXISTING_IMAGES}
    ></cts-image-upload>
  `,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    const desc = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-image-upload__description-input")
    );

    await step("description input is rendered as a required text field", async () => {
      expect(desc).toBeTruthy();
      expect(desc.required).toBe(true);
    });

    await step("both pending and existing rows render at once", async () => {
      expect(canvasElement.querySelectorAll('[data-testid="pending-image"]').length).toBe(1);
      expect(canvasElement.querySelectorAll('[data-testid="existing-image"]').length).toBe(2);
    });

    await step("dropping a file leaves Upload disabled until a description is typed", async () => {
      const zone = /** @type {HTMLLabelElement} */ (
        canvasElement.querySelector('label[data-testid="inline-dropzone"]')
      );
      const file = makeMockPng("editable-drop.png");
      zone.dispatchEvent(
        new DragEvent("drop", { bubbles: true, dataTransfer: makeDataTransfer([file]) }),
      );
      await waitFor(() => {
        const thumb = zone.querySelector("img.oidf-image-upload__thumb");
        expect(thumb).toBeTruthy();
      });
      const upload = canvas.getByRole("button", { name: /^upload$/i });
      expect(/** @type {HTMLButtonElement} */ (upload).disabled).toBe(true);
    });

    await step("typing a description enables Upload", async () => {
      const upload = canvas.getByRole("button", { name: /^upload$/i });
      desc.value = "Additional screenshot for this run";
      desc.dispatchEvent(new Event("input", { bubbles: true }));
      await waitFor(() => {
        expect(/** @type {HTMLButtonElement} */ (upload).disabled).toBe(false);
      });
    });
  },
};

export const InvalidFileType = {
  render: () => html`
    <cts-image-upload
      test-id="test-invalid-type"
      .pendingImages=${[MOCK_PENDING_IMAGES[0]]}
    ></cts-image-upload>
  `,
  async play({ canvasElement, step }) {
    const zone = /** @type {HTMLLabelElement} */ (
      canvasElement.querySelector('label[data-testid="inline-dropzone"]')
    );

    await step("dropping a PDF surfaces the error alert", async () => {
      // The component must reject the PDF and show the error alert.
      const bogus = new File([new Uint8Array(8)], "not-an-image.pdf", { type: "application/pdf" });
      zone.dispatchEvent(
        new DragEvent("drop", { bubbles: true, dataTransfer: makeDataTransfer([bogus]) }),
      );
      await waitFor(() => {
        const alert = canvasElement.querySelector(".oidf-image-upload__alert--error");
        expect(alert).toBeTruthy();
        expect(alert.textContent).toContain("not a supported type");
      });
    });

    await step("the upload button remains disabled (no valid file was stored)", async () => {
      const canvas = within(canvasElement);
      const upload = canvas.getByRole("button", { name: /^upload$/i });
      expect(/** @type {HTMLButtonElement} */ (upload).disabled).toBe(true);
    });
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
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step('"All images uploaded" message shown', async () => {
      const status = canvas.getByRole("status");
      expect(status).toBeInTheDocument();
      expect(status.textContent).toContain("All images uploaded");
      expect(status.classList.contains("oidf-image-upload__alert")).toBe(true);
    });

    await step("no file inputs rendered", async () => {
      const pendingBlocks = canvasElement.querySelectorAll('[data-testid="pending-image"]');
      expect(pendingBlocks.length).toBe(0);
    });
  },
};

export const ExistingImages = {
  render: () => html`
    <cts-image-upload
      test-id="test-abc-123"
      .existingImages=${MOCK_EXISTING_IMAGES}
    ></cts-image-upload>
  `,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step('"Uploaded Images" heading and both existing rows render', async () => {
      expect(canvas.getByText("Uploaded Images")).toBeInTheDocument();

      const existingBlocks = canvasElement.querySelectorAll('[data-testid="existing-image"]');
      expect(existingBlocks.length).toBe(2);

      expect(canvas.getByText("screenshot-result")).toBeInTheDocument();
      expect(canvas.getByText("screenshot-token")).toBeInTheDocument();
    });

    await step("each row shows the uploaded status and image", async () => {
      const successBlocks = canvasElement.querySelectorAll(".oidf-image-upload__status--uploaded");
      expect(successBlocks.length).toBe(2);

      const imgs = canvasElement.querySelectorAll('[data-testid="existing-image"] img');
      expect(imgs.length).toBe(2);
      for (const img of imgs) {
        expect(img.getAttribute("src")).toBe("images/placeholder.jpg");
      }
    });

    await step('"All images uploaded" status message shown', async () => {
      const status = canvas.getByRole("status");
      expect(status).toBeInTheDocument();
      expect(status.textContent).toContain("All images uploaded");
    });
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
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("selecting a file enables the Upload button", async () => {
      const fileInput = /** @type {HTMLInputElement} */ (
        canvasElement.querySelector('input[type="file"]')
      );
      expect(fileInput).toBeTruthy();

      const file = makeMockPng("test-screenshot.png");
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
    });

    await step("clicking Upload emits cts-image-uploaded with no error alert", async () => {
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
    });
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
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("selecting a file enables the Upload button", async () => {
      const fileInput = /** @type {HTMLInputElement} */ (
        canvasElement.querySelector('input[type="file"]')
      );
      expect(fileInput).toBeTruthy();

      const file = makeMockPng("test-screenshot.png");
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
    });

    await step("clicking Upload surfaces the server error alert", async () => {
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
    });
  },
};
