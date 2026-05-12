import { LitElement, html } from "lit";
import "./cts-icon.js";

/**
 * Maps a `kind` value → the design tokens that drive the colored left-rule
 * accent and the matching Bootstrap Icons glyph rendered inside the toast.
 *
 * The two documented kinds (`ok`, `error`) align with the OIDF status
 * palette: `ok` reads as a calm green pass via `--status-pass`, while
 * `error` reuses the brand rust used elsewhere for failure surfaces. An
 * unknown kind falls back to `ok` so a misspelled value never produces
 * an invisible toast.
 *
 * @type {Object.<string, {ruleVar: string, icon: string, iconColorVar: string}>}
 */
const KIND_STYLES = {
  ok: {
    ruleVar: "--status-pass",
    icon: "circle-check",
    iconColorVar: "--status-pass",
  },
  error: {
    ruleVar: "--rust-400",
    icon: "close-circle",
    iconColorVar: "--rust-400",
  },
};

const STYLE_ID = "cts-toast-styles";

// Scoped CSS for both cts-toast-host and cts-toast. Mirrors the design
// archive's toast iteration: a white card on `--bg-elev` with a 4px
// colored left rule, a matching icon glyph, and `--shadow-2` for elevation.
//
// The host is fixed bottom-right with an 8px (`--space-2`) gap between
// stacked toasts. Unupgraded `<cts-toast>` instances are hidden so the
// fixed-position card never flashes pre-upgrade (FOUC isn't an issue
// because toasts are only created via the static helper below).
const STYLE_TEXT = `
cts-toast-host {
  position: fixed;
  bottom: var(--space-4);
  right: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  z-index: 1000;
  pointer-events: none;
}

cts-toast {
  pointer-events: auto;
}

cts-toast:not(:defined) {
  display: none;
}

.oidf-toast {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  min-width: 280px;
  max-width: 420px;
  padding: var(--space-3) var(--space-4);
  background: var(--bg-elev);
  color: var(--fg);
  border: 1px solid var(--border);
  border-left: 4px solid var(--ink-300);
  border-radius: var(--radius-3);
  box-shadow: var(--shadow-2);
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  line-height: var(--lh-base);
  opacity: 0;
  transform: translateY(8px);
  transition: opacity var(--dur-2) var(--ease-standard),
              transform var(--dur-2) var(--ease-standard);
}
.oidf-toast.is-visible {
  opacity: 1;
  transform: translateY(0);
}

.oidf-toast-icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
}

.oidf-toast-body {
  flex: 1;
  min-width: 0;
}

.oidf-toast-title {
  margin: 0 0 2px 0;
  font-size: var(--fs-14);
  font-weight: var(--fw-bold);
  color: var(--fg);
  line-height: var(--lh-snug);
}

.oidf-toast-message {
  margin: 0;
  color: var(--fg-muted);
  font-size: var(--fs-13);
  line-height: var(--lh-base);
}

.oidf-toast-close {
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
  color: var(--fg-muted);
  opacity: 0.7;
  cursor: pointer;
  flex-shrink: 0;
  border-radius: var(--radius-2);
}
.oidf-toast-close i {
  font-size: 18px;
  line-height: 1;
  display: block;
}
.oidf-toast-close:hover,
.oidf-toast-close:focus-visible {
  opacity: 1;
  color: var(--fg);
}
.oidf-toast-close:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
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
 * Singleton container fixed to the bottom-right of the viewport. Stacks
 * its `<cts-toast>` children vertically with an 8px gap. Pages should
 * not instantiate this element directly — `CtsToastHost.show(...)`
 * auto-creates one when needed.
 *
 * Light DOM, no reactive properties: the host is purely a layout shell.
 * The static helper {@link CtsToastHost.show} is the public API.
 */
class CtsToastHost extends HTMLElement {
  connectedCallback() {
    if (this._initialized) return;
    this._initialized = true;
    injectStyles();
  }

  /**
   * Returns the singleton `<cts-toast-host>` for the current document,
   * creating and appending it to `<body>` on first access. Idempotent —
   * subsequent calls return the same node.
   * @returns {CtsToastHost} The singleton host element
   */
  static getOrCreate() {
    let host = /** @type {CtsToastHost | null} */ (document.querySelector("cts-toast-host"));
    if (!host) {
      host = /** @type {CtsToastHost} */ (document.createElement("cts-toast-host"));
      document.body.appendChild(host);
    }
    return host;
  }

  /**
   * @typedef {object} ToastOptions
   * @property {string} [title] - Bold heading line (defaults to "").
   * @property {string} [message] - Optional secondary copy.
   * @property {"ok"|"error"} [kind="ok"] - Visual variant.
   * @property {number} [duration=5000] - Auto-dismiss delay in milliseconds. Pass `0` to disable auto-dismiss.
   */

  /**
   * Convenience entry point. Creates a `<cts-toast>` from the given
   * options and appends it to the singleton host (auto-creating the
   * host if not yet in the document). Returns the toast element so
   * callers can listen for `cts-toast-dismiss` or remove it early.
   *
   * @param {ToastOptions} [options] - Toast configuration.
   * @returns {CtsToast} The created toast element
   */
  static show(options = {}) {
    const { title = "", message = "", kind = "ok", duration = 5000 } = options;
    injectStyles();
    const host = CtsToastHost.getOrCreate();
    const toast = /** @type {CtsToast} */ (document.createElement("cts-toast"));
    toast.title = title || "";
    toast.message = message;
    toast.kind = kind;
    toast.duration = duration;
    host.appendChild(toast);
    return toast;
  }
}

customElements.define("cts-toast-host", CtsToastHost);

/**
 * Single toast item. Renders title + optional message + dismiss button
 * inside a tokenized white card with a colored left rule and matching
 * icon glyph. Auto-removes itself after `duration` (default 5000ms);
 * pass `duration=0` to disable auto-dismiss.
 *
 * Callers should prefer {@link CtsToastHost.show} over instantiating
 * `<cts-toast>` directly.
 *
 * @property {string} title - Bold heading line.
 * @property {string} message - Optional secondary copy under the title.
 * @property {string} kind - One of: "ok" (default), "error". Unknown
 *   values fall back to "ok".
 * @property {number} duration - Auto-dismiss delay in milliseconds
 *   (default 5000). Pass `0` to disable.
 * @fires cts-toast-dismiss - When the toast is removed (either via the
 *   close button or the auto-dismiss timer). Bubbles and is composed.
 */
class CtsToast extends LitElement {
  static properties = {
    title: { type: String },
    message: { type: String },
    kind: { type: String },
    duration: { type: Number },
  };

  constructor() {
    super();
    this.title = "";
    this.message = "";
    this.kind = "ok";
    this.duration = 5000;
    /** @type {ReturnType<typeof setTimeout> | null} */
    this._dismissTimer = null;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  firstUpdated() {
    // Trigger the fade-in transition on the next frame so the initial
    // opacity:0 / translateY(8px) takes effect before the .is-visible
    // class flips both back to their resting values.
    requestAnimationFrame(() => {
      const card = this.querySelector(".oidf-toast");
      if (card) card.classList.add("is-visible");
    });
    if (this.duration > 0) {
      this._dismissTimer = setTimeout(() => this.dismiss(), this.duration);
    }
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._dismissTimer !== null) {
      clearTimeout(this._dismissTimer);
      this._dismissTimer = null;
    }
  }

  /**
   * Remove the toast from the DOM and fire `cts-toast-dismiss`. Safe
   * to call multiple times — the second call is a no-op because the
   * element is already disconnected.
   * @returns {void}
   */
  dismiss() {
    if (this._dismissTimer !== null) {
      clearTimeout(this._dismissTimer);
      this._dismissTimer = null;
    }
    if (!this.isConnected) return;
    this.dispatchEvent(new CustomEvent("cts-toast-dismiss", { bubbles: true, composed: true }));
    this.remove();
  }

  /**
   * Bound click handler for the close button. Class-arrow field so `this` is
   * preserved without an inline arrow in the lit template (which would trip
   * eslint-plugin-lit's no-template-arrow rule).
   */
  _onDismissClick = () => {
    this.dismiss();
  };

  render() {
    const style = KIND_STYLES[this.kind] ?? KIND_STYLES.ok;
    const cardStyle = `border-left-color: var(${style.ruleVar});`;
    const iconStyle = `color: var(${style.iconColorVar});`;
    return html`<div class="oidf-toast" role="status" style=${cardStyle}>
      <span class="oidf-toast-icon" style=${iconStyle}>
        <cts-icon name=${style.icon} size="20"></cts-icon>
      </span>
      <div class="oidf-toast-body">
        ${this.title ? html`<p class="oidf-toast-title">${this.title}</p>` : ""}
        ${this.message ? html`<p class="oidf-toast-message">${this.message}</p>` : ""}
      </div>
      <button
        type="button"
        class="oidf-toast-close"
        aria-label="Dismiss"
        @click=${this._onDismissClick}
      >
        <cts-icon name="close-md" aria-hidden="true"></cts-icon>
      </button>
    </div>`;
  }
}

customElements.define("cts-toast", CtsToast);

export { CtsToastHost, CtsToast };
