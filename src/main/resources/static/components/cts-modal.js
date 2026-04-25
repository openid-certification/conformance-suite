/** @type {Object.<string, string>} Maps variant name → Bootstrap modifier class */
const VARIANT_CLASSES = {
  light: "btn-light",
  info: "btn-info",
  primary: "btn-primary",
  danger: "btn-danger",
  secondary: "btn-secondary",
  success: "btn-success",
  warning: "btn-warning",
  dark: "btn-dark",
  "outline-light": "btn-outline-light",
  "outline-info": "btn-outline-info",
  "outline-primary": "btn-outline-primary",
  "outline-danger": "btn-outline-danger",
  "outline-secondary": "btn-outline-secondary",
  "outline-success": "btn-outline-success",
  "outline-warning": "btn-outline-warning",
  "outline-dark": "btn-outline-dark",
};

/** @type {Object.<string, string>} Maps size name → Bootstrap modifier class (empty string = no class for md) */
const SIZE_CLASSES = {
  sm: "btn-sm",
  md: "",
  lg: "btn-lg",
};

/**
 * Build the full Bootstrap button class string.
 *
 * @param {Object} options
 * @param {string} [options.variant="light"] - Variant key. Unknown values fall back to "light".
 * @param {string} [options.size="sm"] - Size key. Unknown values fall back to "sm".
 * @returns {string} Full class string, e.g. `"btn btn-sm btn-primary"`
 */
function buildButtonClasses({ variant = "light", size = "sm" } = {}) {
  const variantClass = VARIANT_CLASSES[variant] ?? "btn-light";
  const sizeClass = SIZE_CLASSES[size] ?? "btn-sm";
  const sizeSegment = sizeClass ? `${sizeClass} ` : "";
  return `btn ${sizeSegment}${variantClass}`;
}

/**
 * A modal dialog web component that renders a Bootstrap-shaped
 * `<div class="modal">` into its light DOM and delegates show/hide to
 * Bootstrap's modal plugin.
 *
 * ## Integration contract: inner `.modal`, not the host
 *
 * `<cts-modal id="X">` transfers the id to the inner `<div class="modal">`
 * during `connectedCallback`, so `document.getElementById("X")` returns the
 * inner div. That is the integration point for the Bootstrap API:
 *
 *     const modalDiv = document.getElementById("myModal");
 *     bootstrap.Modal.getOrCreateInstance(modalDiv).show();
 *
 * Do NOT call `bootstrap.Modal.getOrCreateInstance(ctsModalHostEl)` directly
 * on the host custom element. It currently resolves to the inner `.modal`
 * only because of the id transfer; passing the host would fail as soon as
 * the internal render tree changes (shadow DOM, additional wrapper, etc.).
 *
 * This component also exposes imperative {@link CtsModal#show} and
 * {@link CtsModal#hide} methods that wrap `getOrCreateInstance` on the
 * inner div. Prefer those when you have a reference to the `<cts-modal>`
 * element itself.
 * @see docs/solutions/web-components/cts-modal-bootstrap-interop-2026-04-17.md
 * @property {string} heading - The modal title text
 * @property {string} [size] - Dialog size: "sm", "lg", or "xl"
 * @property {string} [footer-buttons] - JSON array of button descriptors
 *   (see below). When set, replaces the default Close button.
 *
 *   Button descriptor shape:
 *   {
 *     "label": "Delete",          // required — button text
 *     "class": "btn-danger",      // optional — Bootstrap variant class
 *                                 //   (e.g. "btn-danger", "btn-outline-primary").
 *                                 //   Also accepts variant-style keys without
 *                                 //   the "btn-" prefix via the shared helper.
 *                                 //   Default "btn-light".
 *     "icon": "trash",            // optional — Bootstrap Icon name (a-z, 0-9, "-").
 *                                 //   Rendered as <span class="bi bi-trash">.
 *                                 //   Values outside [a-z0-9-] are rejected.
 *     "dismiss": true,            // optional — adds data-bs-dismiss="modal", default true
 *     "id": "confirmDelete",      // optional — HTML id attribute
 *     "data": { "key": "value" }  // optional — data-* attributes
 *   }
 * @property {boolean} [static-backdrop] - Prevents closing on backdrop click;
 *   also suppresses the header close button and auto-generated footer
 * @property {boolean} [no-keyboard] - Prevents closing via Escape key
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
    // aria-modal is set at construction time (option a per the plan): the
    // component takes responsibility rather than deferring to Bootstrap's
    // show() lifecycle. This keeps assistive tech consistent even if a test
    // or page inspects the modal before show() is ever called.
    modal.setAttribute("aria-modal", "true");

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
        footerCloseBtn.className = buildButtonClasses({ variant: "light", size: "sm" });
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
      this.dispatchEvent(new CustomEvent("cts-modal-close", { bubbles: true, composed: true }));
    });

    this._modalEl = modal;
  }

  /**
   * Parse the footer-buttons JSON attribute.
   * @param {string|null} attr - Raw attribute value
   * @returns {Array<object> | null} Parsed array, or null if absent/malformed
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
      console.warn("cts-modal: malformed footer-buttons JSON:", e instanceof Error ? e.message : e);
      return null;
    }
  }

  /**
   * Create a button element from a descriptor object.
   * @param {object} desc - Button descriptor
   * @returns {HTMLButtonElement} Button element ready to append to the modal footer
   */
  static _createButton(desc) {
    const btn = document.createElement("button");
    btn.type = "button";

    // desc.class may be a Bootstrap variant class ("btn-danger",
    // "btn-outline-primary") or an arbitrary additive class. Strip "btn-"
    // to get a variant key and look it up. If the key is NOT a known
    // variant, fall back to treating desc.class as an additive class — the
    // previous implementation silently downgraded unknown btn-* values to
    // btn-light, so "btn-outline-primary" lost its outline.
    let variantKey = "light";
    let extraClass = "";
    if (desc.class) {
      if (desc.class.startsWith("btn-")) {
        const stripped = desc.class.slice("btn-".length);
        if (stripped in VARIANT_CLASSES) {
          variantKey = stripped;
        } else {
          extraClass = ` ${desc.class}`;
        }
      } else {
        extraClass = ` ${desc.class}`;
      }
    }
    btn.className = buildButtonClasses({ variant: variantKey, size: "sm" }) + extraClass;

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

    // Icon + label. Sanitize the icon name against [a-z0-9-]; any descriptor
    // value that fails the check is dropped entirely — we do not emit an
    // empty `<span class="bi bi-">` (an invisible broken icon is worse than
    // no icon).
    const sanitizedIcon = CtsModal._sanitizeIconName(desc.icon);
    if (sanitizedIcon) {
      const iconEl = document.createElement("span");
      iconEl.className = `bi bi-${sanitizedIcon}`;
      iconEl.setAttribute("aria-hidden", "true");
      btn.appendChild(iconEl);
      // A space between the icon and the label reads better in both visual
      // and screen-reader output.
      btn.appendChild(document.createTextNode(" "));
    }
    btn.appendChild(document.createTextNode(desc.label || ""));
    return btn;
  }

  /**
   * Validate an icon name from a footer-buttons descriptor. Returns the name
   * if it matches `[a-z0-9-]+`, otherwise null. Descriptor JSON is
   * caller-supplied string data; this guard prevents HTML/class injection.
   * @param {unknown} raw - Caller-supplied icon-name value from a descriptor
   * @returns {string|null} The validated icon name, or null when invalid or empty
   */
  static _sanitizeIconName(raw) {
    if (typeof raw !== "string" || !raw) return null;
    return /^[a-z0-9-]+$/.test(raw) ? raw : null;
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

export {};
