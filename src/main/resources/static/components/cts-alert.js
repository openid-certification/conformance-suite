const VARIANT_CLASSES = {
  info: "oidf-alert-info",
  success: "oidf-alert-success",
  warning: "oidf-alert-warning",
  danger: "oidf-alert-danger",
};

const STYLE_ID = "cts-alert-styles";

// Scoped styles, injected once per page on first upgrade. Uses the OIDF
// status palette tokens vendored in oidf-tokens.css. The `info` variant has
// no `--status-info-text` token; foreground falls back to --ink-900 via
// inheritance from the page (set by oidf-tokens.css `html { color: var(--fg) }`).
const STYLES = `
  .oidf-alert {
    display: flex;
    align-items: flex-start;
    gap: var(--space-3, 12px);
    padding: var(--space-3, 12px) var(--space-4, 16px);
    border-radius: var(--radius-3, 6px);
    border: 1px solid;
    margin-bottom: var(--space-3, 12px);
    font-size: var(--fs-13, 13px);
    line-height: var(--lh-base, 1.5);
  }
  .oidf-alert-body {
    flex: 1;
    min-width: 0;
  }
  .oidf-alert-info {
    background: var(--status-info-bg);
    border-color: var(--status-info-border);
    color: var(--ink-900);
  }
  .oidf-alert-success {
    background: var(--status-pass-bg);
    border-color: var(--status-pass-border);
    color: var(--status-pass);
  }
  .oidf-alert-warning {
    background: var(--status-warning-bg);
    border-color: var(--status-warning-border);
    color: var(--status-warning);
  }
  .oidf-alert-danger {
    background: var(--status-fail-bg);
    border-color: var(--status-fail-border);
    color: var(--status-fail);
  }
  .oidf-alert-close {
    appearance: none;
    background: transparent;
    border: 0;
    padding: 0;
    margin: 0;
    width: 20px;
    height: 20px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: inherit;
    opacity: 0.7;
    cursor: pointer;
    flex-shrink: 0;
    border-radius: var(--radius-2, 4px);
  }
  .oidf-alert-close i {
    font-size: 18px;
    line-height: 1;
    display: block;
  }
  .oidf-alert-close:hover,
  .oidf-alert-close:focus-visible {
    opacity: 1;
  }
  .oidf-alert-close:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLES;
  document.head.appendChild(style);
}

/**
 * Token-based alert with optional dismiss button. Default-slot children
 * are moved into the alert body, so any HTML (including links, `<em>`,
 * `<strong>`, and other custom elements) is preserved.
 *
 * Renders `<div class="oidf-alert oidf-alert-{variant}" role="alert">`.
 * Bootstrap's `alert`/`alert-*`/`btn-close` classes are no longer emitted;
 * styling is driven entirely by the design-system status palette tokens
 * defined in `oidf-tokens.css` (--status-info-*, --status-pass-*,
 * --status-warning-*, --status-fail-*).
 *
 * Vanilla HTMLElement — has no `static properties`; attributes are read
 * directly in `connectedCallback`.
 * @property {string} variant - One of: info (default), success, warning,
 *   danger (read from the `variant` attribute). Unknown values fall back
 *   to info.
 * @property {boolean} dismissible - Renders a close button that removes the
 *   alert from the DOM and dispatches `cts-alert-dismissed` (presence of
 *   the `dismissible` attribute).
 * @fires cts-alert-dismissed - When the close button is clicked. The event
 *   bubbles and is composed; the alert removes itself from the DOM
 *   immediately afterward.
 */
class CtsAlert extends HTMLElement {
  connectedCallback() {
    if (this._initialized) return;
    this._initialized = true;
    ensureStylesInjected();

    const variant = this.getAttribute("variant") || "info";
    const variantClass = VARIANT_CLASSES[variant] || VARIANT_CLASSES.info;
    const dismissible = this.hasAttribute("dismissible");
    const children = Array.from(this.childNodes);

    const alert = document.createElement("div");
    alert.className = `oidf-alert ${variantClass}`;
    alert.setAttribute("role", "alert");

    const body = document.createElement("div");
    body.className = "oidf-alert-body";
    for (const child of children) {
      body.appendChild(child);
    }
    alert.appendChild(body);

    if (dismissible) {
      const closeBtn = document.createElement("button");
      closeBtn.type = "button";
      closeBtn.className = "oidf-alert-close";
      closeBtn.setAttribute("aria-label", "Close");
      const closeIcon = document.createElement("i");
      closeIcon.className = "bi bi-x";
      closeIcon.setAttribute("aria-hidden", "true");
      closeBtn.appendChild(closeIcon);
      closeBtn.addEventListener("click", () => this._dismiss());
      alert.appendChild(closeBtn);
    }

    this.appendChild(alert);
    this._alertEl = alert;
  }

  _dismiss() {
    this.dispatchEvent(new CustomEvent("cts-alert-dismissed", { bubbles: true, composed: true }));
    this.remove();
  }
}

customElements.define("cts-alert", CtsAlert);
