import { LitElement, html, nothing } from "lit";

const UPLOAD_SIZE_LIMIT = 500 * 1024;
const ACCEPTED_TYPES = ["image/jpeg", "image/png"];

const STYLE_ID = "cts-image-upload-styles";

const STYLE_TEXT = `
.oidf-image-upload {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.oidf-image-upload__heading {
  margin: 0;
  /* mirrors .t-h3 from oidf-tokens.css */
  font-family: var(--font-sans);
  font-weight: var(--fw-bold);
  font-size: var(--fs-16);
  line-height: var(--lh-snug);
  color: var(--fg);
}
.oidf-image-upload__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.oidf-image-upload__item {
  display: grid;
  grid-template-columns: var(--space-6) auto 1fr;
  gap: var(--space-4);
  align-items: start;
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-2);
  background: var(--bg-elev);
}
.oidf-image-upload__status {
  width: var(--space-6);
  height: var(--space-6);
  border-radius: var(--radius-pill);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--fg-on-ink);
  font-size: var(--fs-14);
}
.oidf-image-upload__status--pending {
  background: var(--orange-400);
}
.oidf-image-upload__status--uploaded {
  background: var(--status-pass);
}
.oidf-image-upload__thumb {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border: 1px dashed var(--ink-300);
  border-radius: var(--radius-2);
  background: var(--bg-muted);
  padding: var(--space-1);
}
.oidf-image-upload__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}
.oidf-image-upload__message {
  margin: 0;
  font-size: var(--fs-13);
  line-height: var(--lh-base);
  color: var(--fg);
  word-break: break-word;
}
.oidf-image-upload__hint {
  margin: 0;
  /* mirrors .t-meta */
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  line-height: var(--lh-snug);
  color: var(--fg-soft);
}
.oidf-image-upload__dropzone {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-6);
  border: 1px dashed var(--ink-300);
  border-radius: var(--radius-2);
  background: var(--bg-muted);
  color: var(--fg);
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  line-height: var(--lh-snug);
  cursor: pointer;
  transition: border-color var(--dur-1) var(--ease-standard),
    background var(--dur-1) var(--ease-standard);
}
.oidf-image-upload__dropzone:hover,
.oidf-image-upload__dropzone:focus-within {
  border-color: var(--orange-400);
  background: var(--orange-50);
}
.oidf-image-upload__dropzone:focus-within {
  box-shadow: var(--focus-ring);
  outline: none;
}
.oidf-image-upload__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: center;
}
.oidf-image-upload__upload-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 var(--space-4);
  border: 1px solid var(--ink-300);
  border-radius: var(--radius-2);
  background: var(--bg-elev);
  color: var(--fg);
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  font-weight: var(--fw-bold);
  cursor: pointer;
  transition: background var(--dur-1) var(--ease-standard),
    border-color var(--dur-1) var(--ease-standard);
}
.oidf-image-upload__upload-btn:hover:not(:disabled) {
  border-color: var(--ink-500);
}
.oidf-image-upload__upload-btn:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
  border-color: var(--orange-400);
}
.oidf-image-upload__upload-btn:disabled {
  background: var(--bg-muted);
  color: var(--fg-faint);
  cursor: not-allowed;
}
.oidf-image-upload__upload-btn--ready {
  background: var(--status-pass);
  border-color: var(--status-pass);
  color: var(--fg-on-ink);
}
.oidf-image-upload__upload-btn--ready:hover:not(:disabled) {
  border-color: var(--status-pass);
}
.oidf-image-upload__alert {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-2);
  border: 1px solid;
  font-size: var(--fs-13);
  line-height: var(--lh-base);
}
.oidf-image-upload__alert--error {
  background: var(--status-fail-bg);
  border-color: var(--status-fail-border);
  color: var(--rust-500);
}
.oidf-image-upload__alert--info {
  background: var(--status-info-bg);
  border-color: var(--status-info-border);
  color: var(--status-info);
}
`;

function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Image upload UI for test logs. Renders pending images that require an
 * upload and previously-uploaded images, with client-side type/size validation
 * (JPEG/PNG, 500KB max).
 *
 * Light DOM. Scoped CSS lives in a single `<style>` element injected into
 * `<head>` on first connect (gated by a module-level flag) so the rules
 * appear once regardless of how many `cts-image-upload` instances are on
 * the page.
 *
 * @property {string} testId - Test log ID used in the upload URL. Reflects the
 *   `test-id` attribute.
 * @property {Array} pendingImages - Images awaiting upload; each item has
 *   `{ name, description }`.
 * @property {Array} existingImages - Already-uploaded images; each item has
 *   `{ name, url }`.
 * @fires cts-image-uploaded - After a successful POST, with
 *   `{ detail: { testId, imageName } }`; bubbles.
 */
class CtsImageUpload extends LitElement {
  static properties = {
    testId: { type: String, attribute: "test-id" },
    pendingImages: { type: Array, attribute: false },
    existingImages: { type: Array, attribute: false },
    _selectedFiles: { type: Object, state: true },
    _previews: { type: Object, state: true },
    _uploading: { type: Boolean, state: true },
    _error: { type: String, state: true },
    _uploadedIds: { type: Object, state: true },
  };

  constructor() {
    super();
    this.testId = "";
    this.pendingImages = [];
    this.existingImages = [];
    this._selectedFiles = {};
    this._previews = {};
    this._uploading = false;
    this._error = "";
    this._uploadedIds = new Set();
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  _validateFile(file) {
    if (!file) return null;
    if (file.size > UPLOAD_SIZE_LIMIT) {
      return `File "${file.name}" exceeds the 500KB limit`;
    }
    if (!ACCEPTED_TYPES.includes(file.type)) {
      return `File "${file.name}" is not a supported type. Only JPEG and PNG files are accepted`;
    }
    return null;
  }

  _handleFileSelect(e) {
    const imageName = e.currentTarget.dataset.imageName;
    if (!imageName) return;
    const file = e.target.files[0];
    if (!file) return;

    const validationError = this._validateFile(file);
    if (validationError) {
      this._error = validationError;
      e.target.value = "";
      return;
    }

    this._error = "";
    this._selectedFiles = { ...this._selectedFiles, [imageName]: file };

    const reader = new FileReader();
    reader.onload = (evt) => {
      const target = /** @type {FileReader} */ (evt.target);
      this._previews = { ...this._previews, [imageName]: target.result };
    };
    reader.readAsDataURL(file);
  }

  async _handleUpload(e) {
    const imageName = e.currentTarget.dataset.imageName;
    if (!imageName) return;
    const file = this._selectedFiles[imageName];
    if (!file || !this.testId) return;

    this._uploading = true;
    this._error = "";

    try {
      // The endpoint accepts the dataURL body directly (see ImageAPI#uploadImageToExistingLogEntry,
      // `@RequestBody String encoded`); it is not a multipart upload.
      const url = `/api/log/${encodeURIComponent(this.testId)}/images/${encodeURIComponent(imageName)}`;
      const response = await fetch(url, {
        method: "POST",
        body: this._previews[imageName],
      });

      if (!response.ok) {
        let message = response.statusText;
        try {
          const errorData = await response.json();
          if (errorData.error) {
            message = errorData.error;
          }
        } catch {
          // Use statusText fallback
        }
        throw new Error(message);
      }

      this._uploadedIds = new Set([...this._uploadedIds, imageName]);

      const newSelected = { ...this._selectedFiles };
      delete newSelected[imageName];
      this._selectedFiles = newSelected;

      this.dispatchEvent(
        new CustomEvent("cts-image-uploaded", {
          bubbles: true,
          detail: { testId: this.testId, imageName },
        }),
      );
    } catch (err) {
      this._error = (err instanceof Error && err.message) || "Upload failed";
    } finally {
      this._uploading = false;
    }
  }

  _renderPendingImage(image) {
    const imageName = image.name;
    const isUploaded = this._uploadedIds.has(imageName);
    const preview = this._previews[imageName];
    const hasFile = !!this._selectedFiles[imageName];

    if (isUploaded) {
      return html`
        <div class="oidf-image-upload__item" data-testid="uploaded-image">
          <span
            class="oidf-image-upload__status oidf-image-upload__status--uploaded"
            aria-hidden="true"
          >
            <cts-icon name="circle-check"></cts-icon>
          </span>
          <span></span>
          <p class="oidf-image-upload__message">${imageName} uploaded successfully.</p>
        </div>
      `;
    }

    const uploadBtnClasses = ["oidf-image-upload__upload-btn"];
    if (hasFile) uploadBtnClasses.push("oidf-image-upload__upload-btn--ready");

    return html`
      <div
        class="oidf-image-upload__item"
        data-testid="pending-image"
        data-image-name="${imageName}"
      >
        <span
          class="oidf-image-upload__status oidf-image-upload__status--pending"
          aria-hidden="true"
        >
          <cts-icon name="cloud-upload"></cts-icon>
        </span>
        <img
          src="${preview || "images/placeholder.jpg"}"
          alt="${imageName}"
          class="oidf-image-upload__thumb imagePreview"
        />
        <div class="oidf-image-upload__body">
          ${image.description
            ? html`<p class="oidf-image-upload__message">${image.description}</p>`
            : nothing}
          <p class="oidf-image-upload__hint"
            >Please upload a screenshot showing just your web browser. Images must not be larger
            than 500 kilobytes.</p
          >
          <div class="oidf-image-upload__actions">
            <label class="oidf-image-upload__dropzone">
              <cts-icon name="camera" aria-hidden="true"></cts-icon>
              <span>Select File</span>
              <input
                type="file"
                accept=".jpg,.jpeg,.png,image/png,image/jpeg"
                hidden
                data-image-name="${imageName}"
                @change="${this._handleFileSelect}"
              />
            </label>
            <button
              class="${uploadBtnClasses.join(" ")} uploadBtn"
              ?disabled="${!hasFile || this._uploading}"
              data-image-name="${imageName}"
              @click="${this._handleUpload}"
              >${this._uploading ? "Uploading..." : "Upload"}</button
            >
          </div>
        </div>
      </div>
    `;
  }

  _renderExistingImage(image) {
    return html`
      <div class="oidf-image-upload__item" data-testid="existing-image">
        <span
          class="oidf-image-upload__status oidf-image-upload__status--uploaded"
          aria-hidden="true"
        >
          <cts-icon name="circle-check"></cts-icon>
        </span>
        <img src="${image.url}" alt="${image.name}" class="oidf-image-upload__thumb imagePreview" />
        <p class="oidf-image-upload__message">${image.name}</p>
      </div>
    `;
  }

  render() {
    const hasPending = this.pendingImages && this.pendingImages.length > 0;
    const hasExisting = this.existingImages && this.existingImages.length > 0;

    return html`
      <div class="oidf-image-upload">
        ${this._error
          ? html`<div class="oidf-image-upload__alert oidf-image-upload__alert--error" role="alert">
              ${this._error}
            </div>`
          : nothing}
        ${hasExisting
          ? html`<h5 class="oidf-image-upload__heading">Uploaded Images</h5>
              <div class="oidf-image-upload__list">${this._renderExistingImages()}</div>`
          : nothing}
        ${hasPending
          ? html`<div class="oidf-image-upload__list">${this._renderPendingImages()}</div>`
          : nothing}
        ${!hasPending && !hasExisting
          ? html`<div class="oidf-image-upload__alert oidf-image-upload__alert--info" role="status"
              >All images uploaded</div
            >`
          : nothing}
        ${!hasPending && hasExisting
          ? html`<div class="oidf-image-upload__alert oidf-image-upload__alert--info" role="status"
              >All images uploaded</div
            >`
          : nothing}
      </div>
    `;
  }

  _renderExistingImages() {
    return this.existingImages.map((img) => this._renderExistingImage(img));
  }

  _renderPendingImages() {
    return this.pendingImages.map((img) => this._renderPendingImage(img));
  }
}

customElements.define("cts-image-upload", CtsImageUpload);
