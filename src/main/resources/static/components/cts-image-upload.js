import { LitElement, html, nothing } from "lit";

const UPLOAD_SIZE_LIMIT = 500 * 1024;
const ACCEPTED_TYPES = ["image/jpeg", "image/png"];
const VALID_LAYOUTS = new Set(["hero", "inline", "card"]);

const STYLE_ID = "cts-image-upload-styles";

const STYLE_TEXT = `
.oidf-image-upload {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.oidf-image-upload__heading {
  margin: 0;
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
.oidf-image-upload__sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* ---------- Shared item shell ---------- */

.oidf-image-upload__item {
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-2);
  background: var(--bg-elev);
}
.oidf-image-upload__item--uploaded {
  display: grid;
  grid-template-columns: var(--space-6) auto 1fr;
  gap: var(--space-4);
  align-items: center;
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
  flex: 0 0 auto;
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
  border-radius: var(--radius-2);
  background: var(--bg-muted);
}
.oidf-image-upload__message {
  margin: 0;
  font-size: var(--fs-13);
  line-height: var(--lh-base);
  color: var(--fg);
  word-break: break-word;
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

/* ---------- Hero layout (default) ---------- */

.oidf-image-upload__hero {
  display: grid;
  grid-template-columns: var(--space-6) 1fr;
  grid-template-rows: auto auto;
  column-gap: var(--space-4);
  row-gap: var(--space-3);
  align-items: start;
}
.oidf-image-upload__hero-meta {
  grid-column: 2;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}
.oidf-image-upload__hero-description {
  margin: 0;
  font-family: var(--font-sans);
  font-weight: var(--fw-bold);
  font-size: var(--fs-14);
  line-height: var(--lh-snug);
  color: var(--fg);
}
.oidf-image-upload__hero-zone {
  grid-column: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: var(--space-2);
  padding: var(--space-6) var(--space-4);
  border: 2px dashed var(--border-strong);
  border-radius: var(--radius-2);
  background: var(--bg-muted);
  color: var(--fg);
  cursor: pointer;
  transition: border-color var(--dur-1) var(--ease-standard),
    background var(--dur-1) var(--ease-standard);
}
.oidf-image-upload__hero-zone:hover {
  border-color: var(--orange-300);
  background: var(--orange-50);
}
.oidf-image-upload__hero-zone:focus-visible {
  outline: none;
  border-color: var(--orange-400);
  box-shadow: var(--focus-ring);
}
.oidf-image-upload__hero-zone--dragover {
  border-style: solid;
  border-color: var(--orange-400);
  background: var(--orange-50);
}
.oidf-image-upload__hero-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: var(--radius-pill);
  background: var(--bg-elev);
  color: var(--orange-500);
  border: 1px solid var(--border);
}
.oidf-image-upload__hero-zone--dragover .oidf-image-upload__hero-icon {
  background: var(--orange-400);
  color: var(--fg-on-ink);
  border-color: var(--orange-400);
}
.oidf-image-upload__hero-title {
  margin: 0;
  font-family: var(--font-sans);
  font-weight: var(--fw-medium);
  font-size: var(--fs-14);
  line-height: var(--lh-snug);
  color: var(--fg);
}
.oidf-image-upload__hero-title strong {
  color: var(--orange-500);
  font-weight: var(--fw-bold);
}
.oidf-image-upload__hero-hint {
  margin: 0;
  font-family: var(--font-sans);
  font-size: var(--fs-12);
  line-height: var(--lh-snug);
  color: var(--fg-soft);
}
.oidf-image-upload__hero-preview {
  grid-column: 2;
  display: grid;
  grid-template-columns: 96px 1fr auto;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-2);
  background: var(--bg-elev);
}
.oidf-image-upload__hero-preview-thumb {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border-radius: var(--radius-2);
  background: var(--bg-muted);
  border: 1px solid var(--border);
}
.oidf-image-upload__hero-preview-meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}
.oidf-image-upload__hero-preview-name {
  margin: 0;
  font-family: var(--font-sans);
  font-weight: var(--fw-medium);
  font-size: var(--fs-13);
  line-height: var(--lh-snug);
  color: var(--fg);
  word-break: break-all;
}
.oidf-image-upload__hero-preview-size {
  margin: 0;
  font-family: var(--font-sans);
  font-size: var(--fs-12);
  line-height: var(--lh-snug);
  color: var(--fg-soft);
}
.oidf-image-upload__hero-actions {
  grid-column: 2;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-2);
  align-items: center;
}

/* ---------- Inline layout ---------- */

.oidf-image-upload__inline {
  display: grid;
  grid-template-columns: var(--space-6) auto 1fr;
  gap: var(--space-4);
  align-items: start;
}
.oidf-image-upload__inline-zone {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 96px;
  border-radius: var(--radius-2);
  background: var(--bg-muted);
  color: var(--fg-soft);
  cursor: pointer;
  box-shadow: inset 0 0 0 1px var(--border-strong);
  transition: box-shadow var(--dur-1) var(--ease-standard),
    background var(--dur-1) var(--ease-standard);
}
.oidf-image-upload__inline-zone:hover {
  box-shadow: inset 0 0 0 1px var(--orange-300);
  background: var(--orange-50);
}
.oidf-image-upload__inline-zone:focus-within {
  outline: none;
  box-shadow: inset 0 0 0 1px var(--orange-400), var(--focus-ring);
  background: var(--orange-50);
}
.oidf-image-upload__inline-zone--dragover {
  box-shadow: inset 0 0 0 2px var(--orange-400);
  background: var(--orange-50);
  color: var(--orange-500);
}
.oidf-image-upload__inline-zone-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  font-size: var(--fs-12);
  font-weight: var(--fw-medium);
  text-align: center;
  line-height: var(--lh-snug);
}
.oidf-image-upload__inline-zone .oidf-image-upload__thumb {
  display: block;
  margin: 0;
}
.oidf-image-upload__inline-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}
.oidf-image-upload__inline-description {
  margin: 0;
  font-family: var(--font-sans);
  font-weight: var(--fw-bold);
  font-size: var(--fs-14);
  line-height: var(--lh-snug);
  color: var(--fg);
}
.oidf-image-upload__inline-hint {
  margin: 0;
  font-family: var(--font-sans);
  font-size: var(--fs-12);
  line-height: var(--lh-snug);
  color: var(--fg-soft);
}
.oidf-image-upload__inline-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: center;
  margin-top: var(--space-1);
}

/* ---------- Card layout ---------- */

.oidf-image-upload__card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.oidf-image-upload__card-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.oidf-image-upload__card-description {
  margin: 0;
  font-family: var(--font-sans);
  font-weight: var(--fw-bold);
  font-size: var(--fs-14);
  line-height: var(--lh-snug);
  color: var(--fg);
}
.oidf-image-upload__card-zone {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: var(--space-3);
  width: 100%;
  aspect-ratio: 4 / 3;
  padding: var(--space-4);
  border: 2px dashed var(--border-strong);
  border-radius: var(--radius-2);
  background: var(--bg-muted);
  color: var(--fg);
  cursor: pointer;
  overflow: hidden;
  transition: border-color var(--dur-1) var(--ease-standard),
    background var(--dur-1) var(--ease-standard);
}
.oidf-image-upload__card-zone:hover {
  border-color: var(--orange-300);
  background: var(--orange-50);
}
.oidf-image-upload__card-zone:focus-visible {
  outline: none;
  border-color: var(--orange-400);
  box-shadow: var(--focus-ring);
}
.oidf-image-upload__card-zone--dragover {
  border-style: solid;
  border-color: var(--orange-400);
  background: var(--orange-50);
}
.oidf-image-upload__card-zone--filled {
  padding: 0;
  background: var(--bg-elev);
  aspect-ratio: auto;
  min-height: 240px;
}
.oidf-image-upload__card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: var(--radius-pill);
  background: var(--bg-elev);
  color: var(--orange-500);
  border: 1px solid var(--border);
}
.oidf-image-upload__card-zone--dragover .oidf-image-upload__card-icon {
  background: var(--orange-400);
  color: var(--fg-on-ink);
  border-color: var(--orange-400);
}
.oidf-image-upload__card-title {
  margin: 0;
  font-family: var(--font-sans);
  font-weight: var(--fw-medium);
  font-size: var(--fs-14);
  line-height: var(--lh-snug);
  color: var(--fg);
}
.oidf-image-upload__card-title strong {
  color: var(--orange-500);
  font-weight: var(--fw-bold);
}
.oidf-image-upload__card-hint {
  margin: 0;
  font-family: var(--font-sans);
  font-size: var(--fs-12);
  line-height: var(--lh-snug);
  color: var(--fg-soft);
}
.oidf-image-upload__card-preview {
  width: 100%;
  height: 100%;
  max-height: 360px;
  object-fit: contain;
  background: var(--bg-muted);
  border-radius: var(--radius-2);
  display: block;
}
.oidf-image-upload__card-replace {
  position: absolute;
  right: var(--space-3);
  bottom: var(--space-3);
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-2);
  background: rgba(26, 22, 17, 0.78);
  color: var(--fg-on-ink);
  font-family: var(--font-sans);
  font-size: var(--fs-12);
  font-weight: var(--fw-medium);
  border: 0;
  cursor: pointer;
}
.oidf-image-upload__card-replace:hover {
  background: var(--ink-900);
}
.oidf-image-upload__card-replace:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
}
.oidf-image-upload__card-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: var(--space-2);
  align-items: center;
}
.oidf-image-upload__card-meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}
.oidf-image-upload__card-filename {
  margin: 0;
  font-family: var(--font-sans);
  font-weight: var(--fw-medium);
  font-size: var(--fs-13);
  line-height: var(--lh-snug);
  color: var(--fg);
  word-break: break-all;
}

/* ---------- Action buttons (shared) ---------- */

.oidf-image-upload__hidden-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}
.oidf-image-upload__picker-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  height: 34px;
  padding: 0 var(--space-4);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-2);
  background: var(--bg-elev);
  color: var(--fg);
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  font-weight: var(--fw-medium);
  cursor: pointer;
  transition: border-color var(--dur-1) var(--ease-standard),
    background var(--dur-1) var(--ease-standard);
}
.oidf-image-upload__picker-btn:hover {
  border-color: var(--ink-500);
}
.oidf-image-upload__picker-btn:focus-within {
  outline: none;
  border-color: var(--orange-400);
  box-shadow: var(--focus-ring);
}
.oidf-image-upload__upload-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
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
  border-color: var(--border);
}
.oidf-image-upload__upload-btn--ready {
  background: var(--orange-400);
  border-color: var(--orange-400);
  color: var(--fg-on-ink);
}
.oidf-image-upload__upload-btn--ready:hover:not(:disabled) {
  background: var(--orange-500);
  border-color: var(--orange-500);
}
.oidf-image-upload__replace-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  height: 28px;
  padding: 0 var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-2);
  background: transparent;
  color: var(--fg-link);
  font-family: var(--font-sans);
  font-size: var(--fs-12);
  font-weight: var(--fw-medium);
  cursor: pointer;
}
.oidf-image-upload__replace-btn:hover {
  border-color: var(--orange-400);
  background: var(--orange-50);
}
.oidf-image-upload__replace-btn:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
  border-color: var(--orange-400);
}
`;

/** Inject the component stylesheet once per page; reused across instances. */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Human-readable byte size for the preview row.
 * @param {number} bytes
 * @returns {string}
 */
function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

let _instanceSeq = 0;

/**
 * Stable, ID-friendly hash for an imageName so we can build per-slot
 * aria-describedby anchors without leaking arbitrary characters into the DOM.
 */
function _slugify(value) {
  return String(value)
    .replace(/[^a-zA-Z0-9_-]+/g, "-")
    .replace(/^-+|-+$/g, "");
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
 * Each pending image renders an **interactive drop zone** that accepts:
 * - drag-and-drop of a single file,
 * - click on the zone (opens the file picker),
 * - keyboard activation (Enter or Space on the focused zone),
 * - paste of an image from the clipboard while the component has focus.
 *
 * All four paths converge on `_acceptFile(imageName, file)`, which validates,
 * stores the file, generates the data-URL preview, and emits an accessible
 * announcement through a single `role="status" aria-live="polite"` region.
 * The visible file-picker button stays present in the `inline` layout as the
 * single-pointer alternative required by WCAG 2.1 SC 2.5.7 (Dragging
 * Movements); the `hero` and `card` layouts make the drop zone itself
 * keyboard-activatable so the same affordance covers both pointer and
 * keyboard users.
 *
 * @property {string} testId - Test log ID used in the upload URL. Reflects the
 *   `test-id` attribute.
 * @property {Array} pendingImages - Images awaiting upload; each item has
 *   `{ name, description }`.
 * @property {Array} existingImages - Already-uploaded images; each item has
 *   `{ name, url }`.
 * @property {string} layout - Visual treatment for each pending item. One of
 *   `"hero"` (default), `"inline"`, or `"card"`. Reflects the `layout`
 *   attribute. Unknown values fall back to `"hero"`.
 * @fires cts-image-uploaded - After a successful POST, with
 *   `{ detail: { testId, imageName } }`; bubbles.
 */
class CtsImageUpload extends LitElement {
  static properties = {
    testId: { type: String, attribute: "test-id" },
    pendingImages: { type: Array, attribute: false },
    existingImages: { type: Array, attribute: false },
    layout: { type: String, reflect: true },
    _selectedFiles: { type: Object, state: true },
    _previews: { type: Object, state: true },
    _uploading: { type: Boolean, state: true },
    _error: { type: String, state: true },
    _uploadedIds: { type: Object, state: true },
    _dragOver: { type: Object, state: true },
    _announce: { type: String, state: true },
  };

  constructor() {
    super();
    this.testId = "";
    this.pendingImages = [];
    this.existingImages = [];
    this.layout = "hero";
    this._selectedFiles = {};
    this._previews = {};
    this._uploading = false;
    this._error = "";
    this._uploadedIds = new Set();
    this._dragOver = new Set();
    this._announce = "";
    this._dragDepth = {};
    this._readerGeneration = {};
    _instanceSeq += 1;
    this._instanceId = `cts-image-upload-${_instanceSeq}`;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    this.addEventListener("paste", this._handlePaste);
    // Reset stuck drag state on dragend (drag canceled with ESC, dropped
    // outside the component, or left the viewport without firing dragleave
    // on the slot). Without this the dragover styling and announcement can
    // linger until the next interaction.
    this.addEventListener("dragend", this._resetDragState);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this.removeEventListener("paste", this._handlePaste);
    this.removeEventListener("dragend", this._resetDragState);
  }

  _resetDragState = () => {
    if (this._dragOver.size === 0) return;
    this._dragDepth = {};
    this._dragOver = new Set();
  };

  _resolvedLayout() {
    return VALID_LAYOUTS.has(this.layout) ? this.layout : "hero";
  }

  _hintIdFor(imageName) {
    return `${this._instanceId}-hint-${_slugify(imageName)}`;
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

  _acceptFile(imageName, file) {
    if (!imageName || !file) return;

    const validationError = this._validateFile(file);
    if (validationError) {
      this._error = validationError;
      this._announce = `${validationError}. The image was rejected.`;
      return;
    }

    this._error = "";
    this._selectedFiles = { ...this._selectedFiles, [imageName]: file };

    // Per-slot generation token: if a newer _acceptFile call lands for the
    // same slot while this FileReader is still running, the older onload
    // must not overwrite the newer preview. Without this, two rapid drops
    // can leave _previews[imageName] holding the older file's bytes while
    // _selectedFiles[imageName] holds the newer file — and _handleUpload
    // POSTs _previews, so the wrong bytes would be uploaded.
    const generation = (this._readerGeneration[imageName] || 0) + 1;
    this._readerGeneration = { ...this._readerGeneration, [imageName]: generation };

    const reader = new FileReader();
    reader.onload = (evt) => {
      if (this._readerGeneration[imageName] !== generation) return;
      const target = /** @type {FileReader} */ (evt.target);
      this._previews = { ...this._previews, [imageName]: target.result };
      this._announce = `Selected ${file.name}. Ready to upload.`;
    };
    reader.readAsDataURL(file);
  }

  _handleFileSelect = (e) => {
    const target = /** @type {HTMLInputElement} */ (e.currentTarget);
    const imageName = target.dataset.imageName;
    const file = target.files && target.files[0];
    if (!imageName || !file) return;
    this._acceptFile(imageName, file);
    target.value = "";
  };

  _slotFromEvent(e) {
    const el = /** @type {HTMLElement} */ (e.currentTarget);
    return el.dataset.imageName || "";
  }

  _handleZoneClick = (e) => {
    if (/** @type {HTMLElement} */ (e.target).closest(".oidf-image-upload__replace-btn")) return;
    const imageName = this._slotFromEvent(e);
    this._openPicker(imageName);
  };

  _handleZoneKeyDown = (e) => {
    if (e.key !== "Enter" && e.key !== " ") return;
    e.preventDefault();
    const imageName = this._slotFromEvent(e);
    this._openPicker(imageName);
  };

  _openPicker(imageName) {
    if (!imageName) return;
    const input = /** @type {HTMLInputElement|null} */ (
      this.querySelector(`input[type="file"][data-image-name="${CSS.escape(imageName)}"]`)
    );
    if (input) input.click();
  }

  _handleReplace = (e) => {
    e.stopPropagation();
    const button = /** @type {HTMLElement} */ (e.currentTarget);
    const imageName = button.dataset.imageName;
    if (!imageName) return;
    const nextSelected = { ...this._selectedFiles };
    delete nextSelected[imageName];
    const nextPreviews = { ...this._previews };
    delete nextPreviews[imageName];
    this._selectedFiles = nextSelected;
    this._previews = nextPreviews;
    this._announce = `Selection cleared. Choose another file.`;
    this._openPicker(imageName);
  };

  _handleDragEnter = (e) => {
    e.preventDefault();
    const imageName = this._slotFromEvent(e);
    if (!imageName) return;
    const depth = (this._dragDepth[imageName] || 0) + 1;
    this._dragDepth = { ...this._dragDepth, [imageName]: depth };
    if (depth === 1) {
      const next = new Set(this._dragOver);
      next.add(imageName);
      this._dragOver = next;
      this._announce = `Drop image to attach to ${imageName}.`;
    }
  };

  _handleDragOver = (e) => {
    e.preventDefault();
    if (e.dataTransfer) e.dataTransfer.dropEffect = "copy";
  };

  _handleDragLeave = (e) => {
    const imageName = this._slotFromEvent(e);
    if (!imageName) return;
    const depth = Math.max(0, (this._dragDepth[imageName] || 0) - 1);
    this._dragDepth = { ...this._dragDepth, [imageName]: depth };
    if (depth === 0) {
      const next = new Set(this._dragOver);
      next.delete(imageName);
      this._dragOver = next;
    }
  };

  _handleDrop = (e) => {
    e.preventDefault();
    const imageName = this._slotFromEvent(e);
    if (!imageName) return;
    this._dragDepth = { ...this._dragDepth, [imageName]: 0 };
    const next = new Set(this._dragOver);
    next.delete(imageName);
    this._dragOver = next;
    const file = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0];
    if (file) this._acceptFile(imageName, file);
  };

  _handlePaste = (e) => {
    if (!this.pendingImages || this.pendingImages.length === 0) return;
    const items = e.clipboardData && e.clipboardData.items;
    if (!items) return;
    for (const item of items) {
      if (item.type && item.type.startsWith("image/")) {
        const file = item.getAsFile();
        if (!file) continue;
        const focused = /** @type {HTMLElement|null} */ (document.activeElement);
        const slot = focused && focused.closest && focused.closest("[data-image-name]");
        const targetName =
          (slot && /** @type {HTMLElement} */ (slot).dataset.imageName) ||
          this.pendingImages[0].name;
        if (!targetName) continue;
        e.preventDefault();
        this._acceptFile(targetName, file);
        break;
      }
    }
  };

  _handleUpload = async (e) => {
    // Synchronous re-entrancy guard. Lit re-renders that flip _uploading
    // back into the button's disabled attribute are async; a key-repeat
    // Enter or a programmatic double-click can otherwise dispatch two POSTs.
    if (this._uploading) return;

    const target = /** @type {HTMLElement} */ (e.currentTarget);
    const imageName = target.dataset.imageName;
    if (!imageName) return;
    const file = this._selectedFiles[imageName];
    if (!file || !this.testId) return;

    // Guard against the FileReader not having completed yet. Without this,
    // the button can become enabled (we set _selectedFiles synchronously)
    // before _previews has the data URL, and fetch would POST `undefined`.
    const body = this._previews[imageName];
    if (!body) return;

    this._uploading = true;
    this._error = "";
    this._announce = `Uploading ${file.name}.`;

    try {
      // The endpoint accepts the dataURL body directly (see ImageAPI#uploadImageToExistingLogEntry,
      // `@RequestBody String encoded`); it is not a multipart upload.
      const url = `/api/log/${encodeURIComponent(this.testId)}/images/${encodeURIComponent(imageName)}`;
      const response = await fetch(url, {
        method: "POST",
        body,
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
      this._announce = `Uploaded ${imageName}.`;

      this.dispatchEvent(
        new CustomEvent("cts-image-uploaded", {
          bubbles: true,
          detail: { testId: this.testId, imageName },
        }),
      );
    } catch (err) {
      const message = (err instanceof Error && err.message) || "Upload failed";
      this._error = message;
      this._announce = `Upload failed: ${message}.`;
    } finally {
      this._uploading = false;
    }
  };

  _renderHiddenInput(imageName) {
    // The hidden input is the fallback affordance triggered by the drop
    // zone's click/keyboard handlers; screen-reader focus lives on the
    // visible drop zone (which carries its own aria-label and
    // aria-describedby). Avoid an aria-describedby on the hidden input so
    // its reference never dangles when the hint paragraph is conditionally
    // unmounted on file selection.
    return html`
      <input
        type="file"
        accept=".jpg,.jpeg,.png,image/png,image/jpeg"
        class="oidf-image-upload__hidden-input"
        data-image-name="${imageName}"
        @change="${this._handleFileSelect}"
      />
    `;
  }

  _renderUploadButton(imageName, hasFile) {
    const classes = ["oidf-image-upload__upload-btn", "uploadBtn"];
    if (hasFile) classes.push("oidf-image-upload__upload-btn--ready");
    return html`
      <button
        type="button"
        class="${classes.join(" ")}"
        ?disabled="${!hasFile || this._uploading}"
        data-image-name="${imageName}"
        @click="${this._handleUpload}"
        >${this._uploading ? "Uploading…" : "Upload"}</button
      >
    `;
  }

  _renderPendingHero(image) {
    const imageName = image.name;
    const preview = this._previews[imageName];
    const file = this._selectedFiles[imageName];
    // "Ready to upload" is gated on the preview data URL being populated
    // (FileReader resolved), not just on the file being accepted. This
    // prevents the Upload button from being clickable before the data is
    // available to POST.
    const hasFile = !!(file && preview);
    const isDragOver = this._dragOver.has(imageName);
    const hintId = this._hintIdFor(imageName);

    const zoneClasses = ["oidf-image-upload__hero-zone"];
    if (isDragOver) zoneClasses.push("oidf-image-upload__hero-zone--dragover");

    return html`
      <div
        class="oidf-image-upload__item"
        data-testid="pending-image"
        data-image-name="${imageName}"
      >
        <div class="oidf-image-upload__hero">
          <span
            class="oidf-image-upload__status oidf-image-upload__status--pending"
            aria-hidden="true"
          >
            <cts-icon name="cloud-upload" size="16"></cts-icon>
          </span>
          <div class="oidf-image-upload__hero-meta">
            ${image.description
              ? html`<p class="oidf-image-upload__hero-description">${image.description}</p>`
              : nothing}
          </div>
          ${hasFile && preview
            ? html`
                <div class="oidf-image-upload__hero-preview">
                  <img
                    src="${preview}"
                    alt="Preview of ${imageName}"
                    class="oidf-image-upload__hero-preview-thumb imagePreview"
                  />
                  <div class="oidf-image-upload__hero-preview-meta">
                    <p class="oidf-image-upload__hero-preview-name">${file.name}</p>
                    <p class="oidf-image-upload__hero-preview-size"> ${formatBytes(file.size)} </p>
                  </div>
                  <button
                    type="button"
                    class="oidf-image-upload__replace-btn"
                    data-image-name="${imageName}"
                    @click="${this._handleReplace}"
                    >Replace</button
                  >
                </div>
              `
            : html`
                <div
                  class="${zoneClasses.join(" ")}"
                  role="button"
                  tabindex="0"
                  aria-label="Drag a screenshot here or click to browse"
                  aria-describedby="${hintId}"
                  data-image-name="${imageName}"
                  data-testid="hero-dropzone"
                  @click="${this._handleZoneClick}"
                  @keydown="${this._handleZoneKeyDown}"
                  @dragenter="${this._handleDragEnter}"
                  @dragover="${this._handleDragOver}"
                  @dragleave="${this._handleDragLeave}"
                  @drop="${this._handleDrop}"
                >
                  <span class="oidf-image-upload__hero-icon" aria-hidden="true">
                    <cts-icon
                      name="${isDragOver ? "cloud-add" : "cloud-upload"}"
                      size="24"
                    ></cts-icon>
                  </span>
                  <p class="oidf-image-upload__hero-title" aria-hidden="true">
                    <strong>Drag a screenshot here</strong> or click to browse
                  </p>
                  <p class="oidf-image-upload__hero-hint" id="${hintId}"
                    >JPEG or PNG, up to 500&nbsp;KB</p
                  >
                </div>
              `}
          <div class="oidf-image-upload__hero-actions">
            ${this._renderUploadButton(imageName, hasFile)}
          </div>
        </div>
        ${this._renderHiddenInput(imageName)}
      </div>
    `;
  }

  _renderPendingInline(image) {
    const imageName = image.name;
    const preview = this._previews[imageName];
    const file = this._selectedFiles[imageName];
    // "Ready to upload" is gated on the preview data URL being populated
    // (FileReader resolved), not just on the file being accepted. This
    // prevents the Upload button from being clickable before the data is
    // available to POST.
    const hasFile = !!(file && preview);
    const isDragOver = this._dragOver.has(imageName);
    const hintId = this._hintIdFor(imageName);

    const zoneClasses = ["oidf-image-upload__inline-zone"];
    if (isDragOver) zoneClasses.push("oidf-image-upload__inline-zone--dragover");

    return html`
      <div
        class="oidf-image-upload__item"
        data-testid="pending-image"
        data-image-name="${imageName}"
      >
        <div class="oidf-image-upload__inline">
          <span
            class="oidf-image-upload__status oidf-image-upload__status--pending"
            aria-hidden="true"
          >
            <cts-icon name="cloud-upload" size="16"></cts-icon>
          </span>
          <label
            class="${zoneClasses.join(" ")}"
            data-image-name="${imageName}"
            data-testid="inline-dropzone"
            aria-label="${hasFile
              ? `Replace file for ${imageName}`
              : `Drop or click to attach a screenshot for ${imageName}`}"
            @dragenter="${this._handleDragEnter}"
            @dragover="${this._handleDragOver}"
            @dragleave="${this._handleDragLeave}"
            @drop="${this._handleDrop}"
          >
            ${hasFile && preview
              ? html`<img
                  src="${preview}"
                  alt="Preview of ${imageName}"
                  class="oidf-image-upload__thumb imagePreview"
                />`
              : html`<span class="oidf-image-upload__inline-zone-empty">
                  <cts-icon
                    name="${isDragOver ? "cloud-add" : "cloud-upload"}"
                    size="24"
                    aria-hidden="true"
                  ></cts-icon>
                  <span>Drop or click</span>
                </span>`}
            ${this._renderHiddenInput(imageName)}
          </label>
          <div class="oidf-image-upload__inline-body">
            ${image.description
              ? html`<p class="oidf-image-upload__inline-description">${image.description}</p>`
              : nothing}
            <p class="oidf-image-upload__inline-hint" id="${hintId}">
              Please upload a screenshot showing just your web browser. JPEG or PNG, up to
              500&nbsp;KB.
            </p>
            <div class="oidf-image-upload__inline-actions">
              <label class="oidf-image-upload__picker-btn">
                <cts-icon name="camera" size="16" aria-hidden="true"></cts-icon>
                <span>Select file…</span>
                <input
                  type="file"
                  accept=".jpg,.jpeg,.png,image/png,image/jpeg"
                  class="oidf-image-upload__hidden-input"
                  data-image-name="${imageName}"
                  aria-describedby="${hintId}"
                  @change="${this._handleFileSelect}"
                />
              </label>
              ${this._renderUploadButton(imageName, hasFile)}
            </div>
          </div>
        </div>
      </div>
    `;
  }

  _renderPendingCard(image) {
    const imageName = image.name;
    const preview = this._previews[imageName];
    const file = this._selectedFiles[imageName];
    // "Ready to upload" is gated on the preview data URL being populated
    // (FileReader resolved), not just on the file being accepted. This
    // prevents the Upload button from being clickable before the data is
    // available to POST.
    const hasFile = !!(file && preview);
    const isDragOver = this._dragOver.has(imageName);
    const hintId = this._hintIdFor(imageName);

    const zoneClasses = ["oidf-image-upload__card-zone"];
    if (isDragOver) zoneClasses.push("oidf-image-upload__card-zone--dragover");
    if (hasFile) zoneClasses.push("oidf-image-upload__card-zone--filled");

    return html`
      <div
        class="oidf-image-upload__item"
        data-testid="pending-image"
        data-image-name="${imageName}"
      >
        <div class="oidf-image-upload__card">
          <div class="oidf-image-upload__card-header">
            <span
              class="oidf-image-upload__status oidf-image-upload__status--pending"
              aria-hidden="true"
            >
              <cts-icon name="cloud-upload" size="16"></cts-icon>
            </span>
            <div class="oidf-image-upload__card-meta">
              ${image.description
                ? html`<p class="oidf-image-upload__card-description">${image.description}</p>`
                : nothing}
            </div>
          </div>
          ${hasFile && preview
            ? html`
                <div
                  class="${zoneClasses.join(" ")}"
                  data-image-name="${imageName}"
                  data-testid="card-dropzone"
                  @dragenter="${this._handleDragEnter}"
                  @dragover="${this._handleDragOver}"
                  @dragleave="${this._handleDragLeave}"
                  @drop="${this._handleDrop}"
                >
                  <img
                    src="${preview}"
                    alt="Preview of ${imageName}"
                    class="oidf-image-upload__card-preview imagePreview"
                  />
                  <button
                    type="button"
                    class="oidf-image-upload__card-replace oidf-image-upload__replace-btn"
                    data-image-name="${imageName}"
                    @click="${this._handleReplace}"
                    >Replace</button
                  >
                </div>
              `
            : html`
                <div
                  class="${zoneClasses.join(" ")}"
                  role="button"
                  tabindex="0"
                  aria-label="Drop a screenshot here or click to browse"
                  aria-describedby="${hintId}"
                  data-image-name="${imageName}"
                  data-testid="card-dropzone"
                  @click="${this._handleZoneClick}"
                  @keydown="${this._handleZoneKeyDown}"
                  @dragenter="${this._handleDragEnter}"
                  @dragover="${this._handleDragOver}"
                  @dragleave="${this._handleDragLeave}"
                  @drop="${this._handleDrop}"
                >
                  <span class="oidf-image-upload__card-icon" aria-hidden="true">
                    <cts-icon
                      name="${isDragOver ? "cloud-add" : "cloud-upload"}"
                      size="24"
                    ></cts-icon>
                  </span>
                  <p class="oidf-image-upload__card-title" aria-hidden="true">
                    <strong>Drop a screenshot</strong> or click to browse
                  </p>
                  <p class="oidf-image-upload__card-hint" id="${hintId}"
                    >JPEG or PNG, up to 500&nbsp;KB</p
                  >
                </div>
              `}
          <div class="oidf-image-upload__card-actions">
            ${hasFile
              ? html`<p class="oidf-image-upload__card-filename"
                  >${file.name}
                  <span class="oidf-image-upload__card-hint"
                    >&nbsp;·&nbsp;${formatBytes(file.size)}</span
                  ></p
                >`
              : html`<span></span>`}
            ${this._renderUploadButton(imageName, hasFile)}
          </div>
        </div>
        ${this._renderHiddenInput(imageName)}
      </div>
    `;
  }

  _renderPendingImage(image) {
    const imageName = image.name;
    if (this._uploadedIds.has(imageName)) {
      return html`
        <div
          class="oidf-image-upload__item oidf-image-upload__item--uploaded"
          data-testid="uploaded-image"
        >
          <span
            class="oidf-image-upload__status oidf-image-upload__status--uploaded"
            aria-hidden="true"
          >
            <cts-icon name="circle-check" size="16"></cts-icon>
          </span>
          <span></span>
          <p class="oidf-image-upload__message">${imageName} uploaded successfully.</p>
        </div>
      `;
    }

    switch (this._resolvedLayout()) {
      case "inline":
        return this._renderPendingInline(image);
      case "card":
        return this._renderPendingCard(image);
      case "hero":
      default:
        return this._renderPendingHero(image);
    }
  }

  _renderExistingImage(image) {
    return html`
      <div
        class="oidf-image-upload__item oidf-image-upload__item--uploaded"
        data-testid="existing-image"
      >
        <span
          class="oidf-image-upload__status oidf-image-upload__status--uploaded"
          aria-hidden="true"
        >
          <cts-icon name="circle-check" size="16"></cts-icon>
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
              <div class="oidf-image-upload__list">
                ${this.existingImages.map((img) => this._renderExistingImage(img))}
              </div>`
          : nothing}
        ${hasPending
          ? html`<div class="oidf-image-upload__list">
              ${this.pendingImages.map((img) => this._renderPendingImage(img))}
            </div>`
          : nothing}
        ${!hasPending
          ? html`<div class="oidf-image-upload__alert oidf-image-upload__alert--info" role="status"
              >All images uploaded</div
            >`
          : nothing}
        <div class="oidf-image-upload__sr-only" aria-live="polite" aria-atomic="true">
          ${this._announce}
        </div>
      </div>
    `;
  }
}

customElements.define("cts-image-upload", CtsImageUpload);
