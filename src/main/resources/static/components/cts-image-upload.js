import { LitElement, html, nothing } from "lit";

const UPLOAD_SIZE_LIMIT = 500 * 1024;
const ACCEPTED_TYPES = ["image/jpeg", "image/png"];

/**
 * Image upload UI for test logs. Renders pending images that require an
 * upload and previously-uploaded images, with client-side type/size validation
 * (JPEG/PNG, 500KB max).
 *
 * @property {string} testId - Test log ID used in the upload URL. Reflects the
 *   `test-id` attribute.
 * @property {Array} pendingImages - Images awaiting upload; each item has
 *   `{ name, description }`.
 * @property {Array} existingImages - Already-uploaded images; each item has
 *   `{ name, url }`.
 *
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

  _handleFileSelect(e, imageName) {
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
      this._previews = { ...this._previews, [imageName]: evt.target.result };
    };
    reader.readAsDataURL(file);
  }

  async _handleUpload(imageName) {
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
      this._error = err.message || "Upload failed";
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
        <div class="row mb-3" data-testid="uploaded-image">
          <div class="col-md-12 logItem">
            <div class="row">
              <div class="col-md-1">
                <div class="bg-success testStatusResultBlock">
                  <span class="bi bi-check-circle-fill"></span>
                </div>
              </div>
              <div class="col-md-11">
                <p class="log-message">${imageName} uploaded successfully.</p>
              </div>
            </div>
          </div>
        </div>
      `;
    }

    return html`
      <div class="row mb-3" data-testid="pending-image" data-image-name="${imageName}">
        <div class="col-md-12 logItem">
          <div class="row">
            <div class="col-md-1">
              <div class="bg-warning testStatusResultBlock">
                <span class="bi bi-cloud-upload"></span>
              </div>
            </div>
            <div class="col-md-3">
              <img
                src="${preview || "images/placeholder.png"}"
                alt="${imageName}"
                class="img-fluid mx-auto d-block imagePreview"
              />
            </div>
            <div class="col-md-8">
              ${image.description
                ? html`<p class="log-message wrapLongStrings">${image.description}</p>`
                : nothing}
              <p class="log-message wrapLongStrings"
                >Please upload a screenshot showing just your web browser. Images must not be larger
                than 500 kilobytes.</p
              >
              <label class="btn btn-sm btn-light bg-gradient border border-secondary">
                <span class="bi bi-camera-fill"></span> Select File
                <input
                  type="file"
                  accept=".jpg,.jpeg,.png,image/png,image/jpeg"
                  hidden
                  @change="${(e) => this._handleFileSelect(e, imageName)}"
                />
              </label>
              <button
                class="btn btn-sm bg-gradient border border-secondary uploadBtn ${hasFile
                  ? "btn-success"
                  : "btn-light"}"
                ?disabled="${!hasFile || this._uploading}"
                @click="${() => this._handleUpload(imageName)}"
                >${this._uploading ? "Uploading..." : "Upload"}</button
              >
            </div>
          </div>
        </div>
      </div>
    `;
  }

  _renderExistingImage(image) {
    return html`
      <div class="row mb-3" data-testid="existing-image">
        <div class="col-md-12 logItem">
          <div class="row">
            <div class="col-md-1">
              <div class="bg-success testStatusResultBlock">
                <span class="bi bi-check-circle-fill"></span>
              </div>
            </div>
            <div class="col-md-3">
              <img
                src="${image.url}"
                alt="${image.name}"
                class="img-fluid mx-auto d-block imagePreview"
              />
            </div>
            <div class="col-md-8">
              <p class="log-message">${image.name}</p>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  render() {
    const hasPending = this.pendingImages && this.pendingImages.length > 0;
    const hasExisting = this.existingImages && this.existingImages.length > 0;

    return html`
      <div class="logContent container-fluid">
        ${this._error
          ? html`<div class="alert alert-danger" role="alert">${this._error}</div>`
          : nothing}
        ${hasExisting
          ? html` <h5>Uploaded Images</h5>
              ${this.existingImages.map((img) => this._renderExistingImage(img))}`
          : nothing}
        ${hasPending
          ? html`${this.pendingImages.map((img) => this._renderPendingImage(img))}`
          : nothing}
        ${!hasPending && !hasExisting
          ? html`<div class="alert alert-info" role="status">All images uploaded</div>`
          : nothing}
        ${!hasPending && hasExisting
          ? html`<div class="alert alert-info" role="status">All images uploaded</div>`
          : nothing}
      </div>
    `;
  }
}

customElements.define("cts-image-upload", CtsImageUpload);
