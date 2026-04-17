const VARIANT_CLASSES = {
  info: "alert-info",
  success: "alert-success",
  warning: "alert-warning",
  danger: "alert-danger",
};

/**
 * Bootstrap-styled alert with optional dismiss button. Default-slot children
 * are moved into the alert body, so any HTML (including other custom elements)
 * works as content.
 *
 * Vanilla HTMLElement — has no `static properties`; attributes are read
 * directly in `connectedCallback`.
 *
 * @property {string} variant - One of: info (default), success, warning,
 *   danger (read from the `variant` attribute).
 * @property {boolean} dismissible - Renders a close button that removes the
 *   alert from the DOM and dispatches `cts-alert-dismissed` (presence of
 *   the `dismissible` attribute).
 *
 * @fires cts-alert-dismissed - When the close button is clicked. The event
 *   bubbles and is composed; the alert removes itself from the DOM
 *   immediately afterward.
 */
class CtsAlert extends HTMLElement {
  connectedCallback() {
    if (this._initialized) return;
    this._initialized = true;

    const variant = this.getAttribute("variant") || "info";
    const variantClass = VARIANT_CLASSES[variant] || VARIANT_CLASSES.info;
    const dismissible = this.hasAttribute("dismissible");
    const children = Array.from(this.childNodes);

    const alert = document.createElement("div");
    alert.className = `alert ${variantClass}${dismissible ? " alert-dismissible" : ""}`;
    alert.setAttribute("role", "alert");

    for (const child of children) {
      alert.appendChild(child);
    }

    if (dismissible) {
      const closeBtn = document.createElement("button");
      closeBtn.type = "button";
      closeBtn.className = "btn-close";
      closeBtn.setAttribute("aria-label", "Close");
      closeBtn.addEventListener("click", () => this._dismiss());
      alert.appendChild(closeBtn);
    }

    this.appendChild(alert);
    this._alertEl = alert;
  }

  _dismiss() {
    this.dispatchEvent(
      new CustomEvent("cts-alert-dismissed", { bubbles: true, composed: true }),
    );
    this.remove();
  }
}

customElements.define("cts-alert", CtsAlert);
