/**
 * A modal dialog web component that wraps Bootstrap 5's modal with a
 * declarative HTML API.
 *
 * @property {string} heading - The modal title text
 * @property {string} [size] - Dialog size: "sm", "lg", or "xl"
 * @property {string} [footer-buttons] - JSON array of button descriptors
 *   (see below). When set, replaces the default Close button.
 *
 *   Button descriptor shape:
 *   {
 *     "label": "Delete",          // required — button text
 *     "class": "btn-danger",      // optional — extra CSS class(es), default "btn-light"
 *     "dismiss": true,            // optional — adds data-bs-dismiss="modal", default true
 *     "id": "confirmDelete",      // optional — HTML id attribute
 *     "data": { "key": "value" }  // optional — data-* attributes
 *   }
 *
 * @property {boolean} [static-backdrop] - Prevents closing on backdrop click;
 *   also suppresses the header close button and auto-generated footer
 * @property {boolean} [no-keyboard] - Prevents closing via Escape key
 *
 * @fires cts-modal-close - When the modal finishes hiding (after Bootstrap's
 *   hidden.bs.modal event)
 */
class CtsModal extends HTMLElement {
  /** @type {Set<string>} Valid size attribute values */
  static VALID_SIZES = new Set(["sm", "lg", "xl"]);

  connectedCallback() {
    if (this._initialized) return;
    this._initialized = true;
    const heading = this.getAttribute("heading");
    const size = this.getAttribute("size");
    const staticBackdrop = this.hasAttribute("static-backdrop");
    const noKeyboard = this.hasAttribute("no-keyboard");
    const hostId = this.id;
    const children = Array.from(this.childNodes);

    // Build modal wrapper
    const modal = document.createElement("div");
    modal.className = "modal";
    modal.setAttribute("tabindex", "-1");
    modal.setAttribute("role", "dialog");

    // Transfer id from host to inner .modal so getElementById + bootstrap.Modal works
    if (hostId) {
      modal.id = hostId;
      modal.setAttribute("aria-labelledby", hostId + "-title");
      this.removeAttribute("id");
    }

    if (staticBackdrop) {
      modal.setAttribute("data-bs-backdrop", "static");
    }
    if (noKeyboard) {
      modal.setAttribute("data-bs-keyboard", "false");
    }

    // modal-dialog
    const dialog = document.createElement("div");
    dialog.className = "modal-dialog";
    if (size && CtsModal.VALID_SIZES.has(size)) {
      dialog.classList.add(`modal-${size}`);
    }
    dialog.setAttribute("role", "document");

    // modal-content
    const content = document.createElement("div");
    content.className = "modal-content";

    // modal-header
    const header = document.createElement("div");
    header.className = "modal-header";

    const title = document.createElement("h4");
    title.className = "modal-title";
    if (hostId) {
      title.id = hostId + "-title";
    }
    title.textContent = heading || "";

    header.appendChild(title);

    // Close button in header (skip for static-backdrop loading modals)
    if (!staticBackdrop) {
      const closeBtn = document.createElement("button");
      closeBtn.type = "button";
      closeBtn.className = "btn-close";
      closeBtn.setAttribute("data-bs-dismiss", "modal");
      closeBtn.setAttribute("aria-label", "Close");
      header.appendChild(closeBtn);
    }

    // modal-body — move captured children here
    const body = document.createElement("div");
    body.className = "modal-body";
    for (const child of children) {
      body.appendChild(child);
    }

    content.appendChild(header);
    content.appendChild(body);

    // modal-footer (skip for static-backdrop loading modals)
    if (!staticBackdrop) {
      const footerButtonsAttr = this.getAttribute("footer-buttons");
      const customButtons = CtsModal._parseFooterButtons(footerButtonsAttr);

      const footer = document.createElement("div");
      footer.className = "modal-footer";

      if (customButtons) {
        // Custom footer buttons declared via attribute
        for (const desc of customButtons) {
          footer.appendChild(CtsModal._createButton(desc));
        }
      } else {
        // Default or malformed fallback: auto-generated Close button
        const footerCloseBtn = document.createElement("button");
        footerCloseBtn.type = "button";
        footerCloseBtn.className =
          "btn btn-sm btn-light bg-gradient border border-secondary";
        footerCloseBtn.setAttribute("data-bs-dismiss", "modal");
        footerCloseBtn.textContent = "Close";
        footer.appendChild(footerCloseBtn);
      }
      content.appendChild(footer);
    }

    // Assemble
    dialog.appendChild(content);
    modal.appendChild(dialog);
    this.appendChild(modal);

    // Dispatch cts-modal-close when Bootstrap hides the modal
    modal.addEventListener("hidden.bs.modal", () => {
      this.dispatchEvent(
        new CustomEvent("cts-modal-close", { bubbles: true, composed: true }),
      );
    });

    this._modalEl = modal;
  }

  /**
   * Parse the footer-buttons JSON attribute.
   * @param {string|null} attr - Raw attribute value
   * @returns {Array<Object>|null} Parsed array, or null if absent/malformed
   */
  static _parseFooterButtons(attr) {
    if (!attr) return null;
    try {
      const parsed = JSON.parse(attr);
      if (!Array.isArray(parsed)) {
        console.warn("cts-modal: footer-buttons must be a JSON array, got:", typeof parsed);
        return null;
      }
      return parsed;
    } catch (e) {
      console.warn("cts-modal: malformed footer-buttons JSON:", e.message);
      return null;
    }
  }

  /**
   * Create a button element from a descriptor object.
   * @param {Object} desc - Button descriptor
   * @returns {HTMLButtonElement}
   */
  static _createButton(desc) {
    const btn = document.createElement("button");
    btn.type = "button";

    const variant = desc.class || "btn-light";
    btn.className = `btn btn-sm ${variant} bg-gradient border border-secondary`;

    // dismiss defaults to true
    if (desc.dismiss !== false) {
      btn.setAttribute("data-bs-dismiss", "modal");
    }

    if (desc.id) {
      btn.id = desc.id;
    }

    // data-* attributes
    if (desc.data) {
      for (const [key, value] of Object.entries(desc.data)) {
        btn.setAttribute(`data-${key}`, value);
      }
    }

    btn.textContent = desc.label || "";
    return btn;
  }

  show() {
    if (typeof bootstrap === "undefined" || !this._modalEl) return;
    bootstrap.Modal.getOrCreateInstance(this._modalEl).show();
  }

  hide() {
    if (typeof bootstrap === "undefined" || !this._modalEl) return;
    bootstrap.Modal.getOrCreateInstance(this._modalEl).hide();
  }
}

customElements.define("cts-modal", CtsModal);
