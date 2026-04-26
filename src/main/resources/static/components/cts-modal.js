import "./cts-icon.js";

/**
 * Maps a footer-button descriptor variant key → OIDF token-styled
 * `oidf-btn-*` modifier class. Mirrors the table in `cts-button.js` so
 * footer-button descriptors written in the Bootstrap era (`btn-danger`,
 * `btn-outline-primary`, …) keep rendering meaningfully.
 * @type {{[key: string]: string}}
 */
const VARIANT_CLASSES = {
  primary: "oidf-btn-primary",
  secondary: "oidf-btn-secondary",
  ghost: "oidf-btn-ghost",
  danger: "oidf-btn-danger",
  // Legacy aliases — Variant Migration table from
  // docs/plans/2026-04-25-001-feat-oidf-design-system-bootstrap-removal-plan.md
  light: "oidf-btn-secondary",
  info: "oidf-btn-primary",
  success: "oidf-btn-secondary",
  warning: "oidf-btn-secondary",
  dark: "oidf-btn-secondary",
  "outline-light": "oidf-btn-secondary",
  "outline-info": "oidf-btn-primary",
  "outline-primary": "oidf-btn-primary",
  "outline-danger": "oidf-btn-danger",
  "outline-secondary": "oidf-btn-secondary",
  "outline-success": "oidf-btn-secondary",
  "outline-warning": "oidf-btn-secondary",
  "outline-dark": "oidf-btn-secondary",
};

const STYLE_ID = "cts-modal-styles";

// Scoped CSS for the OIDF modal. Mirrors the design archive's
// project/preview/components-modal.html shape: a 520px-wide white card with
// an ink-tinted scrim, ink-200 dividers between header/body/footer, and
// flush 14px header padding for the close button affordance. Sizes (sm/lg/
// xl) bend the width only — height is content-driven. The
// `dialog:not([open]) { display: none }` rule lives in oidf-app.css (U3).
const STYLE_TEXT = `
/* Host signals open state via the [open] attribute mirrored from the inner
   <dialog>'s open property. When closed the host is removed from layout
   entirely; when open it covers the viewport as an invisible passthrough so
   external observers (Playwright toBeVisible(), CSS, integration scripts)
   can see "this modal is showing" without reaching into the inner dialog.
   The dialog renders in the browser's top layer above this passthrough box;
   pointer-events: none keeps clicks falling through to the dialog itself. */
cts-modal:not([open]) {
  display: none;
}
cts-modal[open] {
  display: block;
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}
cts-modal[open] dialog.oidf-modal {
  pointer-events: auto;
}
dialog.oidf-modal {
  border: 0;
  padding: 0;
  margin: auto;
  border-radius: var(--radius-3);
  background: var(--bg);
  color: var(--fg);
  box-shadow: var(--shadow-3);
  font-family: var(--font-sans);
  font-size: var(--fs-14);
  width: 520px;
  max-width: calc(100vw - 40px);
  max-height: calc(100vh - 40px);
  overflow: visible;
}
dialog.oidf-modal[data-size="sm"] {
  width: 300px;
}
dialog.oidf-modal[data-size="lg"] {
  width: 800px;
}
dialog.oidf-modal[data-size="xl"] {
  width: 1140px;
}
dialog.oidf-modal::backdrop {
  background: rgba(26, 22, 17, 0.55);
  backdrop-filter: blur(4px);
}

.oidf-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 14px 14px 20px;
  border-bottom: 1px solid var(--border);
}
.oidf-modal-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: var(--fw-black, 900);
  font-size: var(--fs-18);
  color: var(--fg);
}
.oidf-modal-close {
  background: transparent;
  border: 0;
  color: var(--fg-muted);
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-2);
  padding: 0;
  transition: background var(--dur-1) var(--ease-standard),
              color var(--dur-1) var(--ease-standard);
}
.oidf-modal-close i {
  font-size: 22px;
  line-height: 1;
  display: block;
}
.oidf-modal-close:hover {
  background: var(--ink-100);
  color: var(--ink-900);
}
.oidf-modal-close:active {
  background: var(--ink-200);
}
.oidf-modal-close:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
}

.oidf-modal-body {
  padding: 20px;
  font-size: var(--fs-13);
  color: var(--fg);
  line-height: 1.5;
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}

.oidf-modal-footer {
  padding: 14px 20px;
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  background: var(--ink-50);
  border-bottom-left-radius: var(--radius-3);
  border-bottom-right-radius: var(--radius-3);
}
`;

/**
 * Inject the cts-modal scoped stylesheet into `<head>` exactly once. Idempotent:
 * subsequent calls find the existing `<style>` tag by id and bail.
 * @returns {void}
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * A modal dialog web component built on the native HTML5 `<dialog>` element.
 *
 * ## Integration contract: id stays on the host
 *
 * `<cts-modal id="X">` keeps its id on the host element. `document.getElementById("X")`
 * returns the `<cts-modal>` host, which exposes `.show()` and `.hide()` directly.
 * This is a deliberate change from the pre-U9 implementation, which transferred
 * the id to an inner Bootstrap `.modal` div so the Bootstrap modal plugin
 * could be reached via that id. With the dialog rewrite, that indirection is
 * gone:
 *
 *     // Old (Bootstrap era):
 *     //   bootstrapModal(document.getElementById("myModal")).show();
 *     // (consult the Bootstrap docs for the exact factory call)
 *
 *     // New (dialog era):
 *     document.getElementById("myModal").show();
 *
 * The component also dispatches `cts-modal-show` immediately before opening
 * and `cts-modal-close` after closing, so consumers can react to lifecycle
 * events without subscribing to Bootstrap-specific events.
 *
 * ## Footer-button descriptors
 *
 * The `footer-buttons` JSON attribute renders custom buttons in the dialog
 * footer. Each descriptor:
 *
 *   {
 *     "label": "Delete",                // required — button text
 *     "class": "btn-danger",            // optional — variant key (with or
 *                                       //   without the `btn-` prefix). Maps
 *                                       //   to OIDF variants per VARIANT_CLASSES.
 *                                       //   Unknown values become an additive
 *                                       //   class so caller-supplied themes
 *                                       //   are not silently downgraded.
 *     "icon": "trash-empty",                  // optional — Bootstrap Icon name
 *                                       //   (sanitized against `[a-z0-9-]+`)
 *     "dismiss": true,                  // optional — clicking closes the
 *                                       //   dialog. Default true.
 *     "id": "confirmDelete",            // optional — HTML id
 *     "data": { "lock": "value" }        // optional — data-* attributes
 *   }
 * @property {string} heading - The modal title text
 * @property {string} [size] - Dialog size: "sm", "lg", or "xl" (default ~520px)
 * @property {string} [footer-buttons] - JSON array of button descriptors
 *   (see above). When set, replaces the default Close button.
 * @property {boolean} [static-backdrop] - Prevents closing on backdrop click
 *   and ESC; also suppresses the header close button and auto-generated
 *   footer Close.
 * @property {boolean} [no-keyboard] - Prevents closing via Escape key.
 * @fires cts-modal-show - Dispatched on the host immediately before the
 *   dialog opens. Listeners can mutate the body content synchronously.
 * @fires cts-modal-close - Dispatched on the host after the dialog closes.
 */
class CtsModal extends HTMLElement {
  /** @type {Set<string>} Valid size attribute values */
  static VALID_SIZES = new Set(["sm", "lg", "xl"]);

  connectedCallback() {
    if (this._initialized) return;
    this._initialized = true;
    injectStyles();

    const heading = this.getAttribute("heading");
    const size = this.getAttribute("size");
    const staticBackdrop = this.hasAttribute("static-backdrop");
    const noKeyboard = this.hasAttribute("no-keyboard");
    const hostId = this.id;
    const titleId = hostId ? `${hostId}-title` : "";
    const children = Array.from(this.childNodes);

    // Build the dialog
    const dialog = document.createElement("dialog");
    dialog.className = "oidf-modal";
    if (size && CtsModal.VALID_SIZES.has(size)) {
      dialog.setAttribute("data-size", size);
    }
    if (titleId) {
      dialog.setAttribute("aria-labelledby", titleId);
    }

    // Header
    const header = document.createElement("div");
    header.className = "oidf-modal-header";

    const title = document.createElement("h4");
    title.className = "oidf-modal-title";
    if (titleId) {
      title.id = titleId;
    }
    title.textContent = heading || "";
    header.appendChild(title);

    // Close button in header (skip for static-backdrop loading modals)
    if (!staticBackdrop) {
      const closeBtn = document.createElement("button");
      closeBtn.type = "button";
      closeBtn.className = "oidf-modal-close";
      closeBtn.setAttribute("aria-label", "Close");
      const closeIcon = document.createElement("cts-icon");
      closeIcon.setAttribute("name", "close-md");
      closeIcon.setAttribute("aria-hidden", "true");
      closeBtn.appendChild(closeIcon);
      closeBtn.addEventListener("click", () => this.hide());
      header.appendChild(closeBtn);
    }

    // Body — move captured children here
    const body = document.createElement("div");
    body.className = "oidf-modal-body";
    for (const child of children) {
      body.appendChild(child);
    }

    dialog.appendChild(header);
    dialog.appendChild(body);

    // Footer (skip for static-backdrop loading modals)
    if (!staticBackdrop) {
      const footerButtonsAttr = this.getAttribute("footer-buttons");
      const customButtons = CtsModal._parseFooterButtons(footerButtonsAttr);

      const footer = document.createElement("div");
      footer.className = "oidf-modal-footer";

      if (customButtons) {
        for (const desc of customButtons) {
          footer.appendChild(this._createButton(desc));
        }
      } else {
        // Default fallback: an auto-generated Close button.
        const footerCloseBtn = document.createElement("button");
        footerCloseBtn.type = "button";
        footerCloseBtn.className = "oidf-btn oidf-btn-sm oidf-btn-secondary";
        footerCloseBtn.textContent = "Close";
        footerCloseBtn.addEventListener("click", () => this.hide());
        footer.appendChild(footerCloseBtn);
      }
      dialog.appendChild(footer);
    }

    // ESC key handling — `cancel` fires before `close` when ESC is pressed.
    // Default `<dialog>` behaviour closes on ESC. For `no-keyboard` (loading
    // modals) AND `static-backdrop` (which the prior Bootstrap behaviour also
    // blocked from ESC dismissal), preventDefault to keep the dialog open.
    if (noKeyboard || staticBackdrop) {
      dialog.addEventListener("cancel", (event) => {
        event.preventDefault();
      });
    }

    // Backdrop click handling. Native `<dialog>` does NOT close on backdrop
    // click — clicks on the backdrop dispatch a click event whose target is
    // the dialog itself. Implement the close-on-backdrop-click affordance
    // unless `static-backdrop` is set.
    dialog.addEventListener("click", (event) => {
      if (event.target !== dialog) return;
      // The dialog is the event target only when the click landed on the
      // backdrop area (clicks on actual children bubble from those children).
      // For `static-backdrop`, swallow the event to keep the dialog open.
      if (staticBackdrop) {
        event.preventDefault();
        return;
      }
      const rect = dialog.getBoundingClientRect();
      const inside =
        event.clientX >= rect.left &&
        event.clientX <= rect.right &&
        event.clientY >= rect.top &&
        event.clientY <= rect.bottom;
      if (!inside) {
        this.hide();
      }
    });

    // close event → cts-modal-close on host. The native event fires whether
    // closure was via ESC, dialog.close(), or any of the inner buttons. Also
    // mirror the close back to the host's `open` attribute so external
    // observers always see a consistent state — `host.hasAttribute("open")`
    // matches `dialog.open` regardless of the close path.
    dialog.addEventListener("close", () => {
      this.removeAttribute("open");
      this.dispatchEvent(new CustomEvent("cts-modal-close", { bubbles: true, composed: true }));
    });

    this.appendChild(dialog);
    this._dialog = dialog;
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
   * Create a footer button from a descriptor object.
   * @param {object} desc - Button descriptor
   * @returns {HTMLButtonElement} Button element ready to append to the modal footer
   */
  _createButton(desc) {
    const btn = document.createElement("button");
    btn.type = "button";

    // desc.class may be a Bootstrap-era variant name ("btn-danger",
    // "btn-outline-primary") or an arbitrary additive class. Strip "btn-"
    // to get a variant key and look it up. Unknown btn-* values fall through
    // to additive-class mode rather than being silently downgraded — this
    // preserves caller-supplied custom themes.
    let variantKey = "secondary";
    let extraClass = "";
    if (desc.class) {
      if (desc.class.startsWith("btn-")) {
        const stripped = desc.class.slice("btn-".length);
        if (stripped in VARIANT_CLASSES) {
          variantKey = stripped;
        } else {
          extraClass = ` ${desc.class}`;
        }
      } else if (desc.class in VARIANT_CLASSES) {
        variantKey = desc.class;
      } else {
        extraClass = ` ${desc.class}`;
      }
    }
    const variantClass = VARIANT_CLASSES[variantKey] ?? "oidf-btn-secondary";
    btn.className = `oidf-btn oidf-btn-sm ${variantClass}${extraClass}`;

    // dismiss defaults to true — clicking the button closes the dialog.
    if (desc.dismiss !== false) {
      btn.addEventListener("click", () => this.hide());
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

    // Icon + label. Sanitize the icon name against [a-z0-9-]; values that
    // fail the check are dropped entirely — we do not emit an empty
    // `<cts-icon>` (an invisible broken icon is worse than none).
    const sanitizedIcon = CtsModal._sanitizeIconName(desc.icon);
    if (sanitizedIcon) {
      const iconEl = document.createElement("cts-icon");
      iconEl.setAttribute("name", sanitizedIcon);
      iconEl.setAttribute("aria-hidden", "true");
      btn.appendChild(iconEl);
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

  /**
   * Open the dialog modally. Dispatches `cts-modal-show` immediately before
   * `showModal()` so listeners can mutate body content synchronously. Mirrors
   * the dialog's open state onto the host as the `open` attribute so external
   * tools (Playwright, CSS, integration tests) can detect "this modal is
   * showing" by inspecting the host element rather than reaching into the
   * inner dialog. The :host(:not([open])) rule in the scoped style hides the
   * host so toBeVisible() works without leaking layout when closed.
   */
  show() {
    if (!this._dialog) return;
    if (this._dialog.open) return;
    this.dispatchEvent(new CustomEvent("cts-modal-show", { bubbles: true, composed: true }));
    this._dialog.showModal();
    this.setAttribute("open", "");
  }

  /**
   * Close the dialog if it is open. No-op otherwise.
   */
  hide() {
    if (!this._dialog) return;
    if (!this._dialog.open) return;
    this._dialog.close();
    this.removeAttribute("open");
  }
}

customElements.define("cts-modal", CtsModal);

export {};
