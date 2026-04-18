/**
 * Declarative wrapper around Bootstrap's Tooltip plugin. Applies
 * `data-bs-toggle="tooltip"` + `title` to the first child element and
 * instantiates a `bootstrap.Tooltip` on connect; disposes the instance on
 * disconnect.
 *
 * Vanilla HTMLElement — has no `static properties`; attributes are read
 * directly in `connectedCallback`.
 *
 * @property {string} content - Tooltip text (read from the `content`
 *   attribute).
 * @property {string} placement - Tooltip placement (read from the
 *   `placement` attribute); defaults to `top`.
 */
// Max time to wait for a dynamically-inserted child after the element is
// connected. If no child appears within this window, we give up silently —
// the component is probably being used incorrectly, but we don't want a
// stray observer holding a reference forever.
const DYNAMIC_INIT_TIMEOUT_MS = 2000;

class CtsTooltip extends HTMLElement {
  connectedCallback() {
    const content = this.getAttribute("content") || "";
    if (!content) return;
    if (this._initTooltipIfReady()) return;

    // No direct child yet. Watch for one — supports dynamic insertion via
    // innerHTML after connect, templating libraries that attach children
    // post-render, etc. Bounded so a cts-tooltip that never gets a child
    // doesn't leak an observer.
    this._childObserver = new MutationObserver(() => {
      if (this._initTooltipIfReady()) this._stopObserving();
    });
    this._childObserver.observe(this, { childList: true });
    this._initTimer = setTimeout(() => this._stopObserving(), DYNAMIC_INIT_TIMEOUT_MS);
  }

  _initTooltipIfReady() {
    const trigger = this.querySelector(":scope > *");
    if (!trigger) return false;
    const content = this.getAttribute("content") || "";
    const placement = this.getAttribute("placement") || "top";
    trigger.setAttribute("data-bs-toggle", "tooltip");
    trigger.setAttribute("data-bs-placement", placement);
    trigger.setAttribute("title", content);
    if (typeof bootstrap !== "undefined") {
      new bootstrap.Tooltip(trigger);
    }
    return true;
  }

  _stopObserving() {
    if (this._childObserver) {
      this._childObserver.disconnect();
      this._childObserver = null;
    }
    if (this._initTimer) {
      clearTimeout(this._initTimer);
      this._initTimer = null;
    }
  }

  disconnectedCallback() {
    this._stopObserving();
    const trigger = this.querySelector(':scope > [data-bs-toggle="tooltip"]');
    if (trigger && typeof bootstrap !== "undefined") {
      const instance = bootstrap.Tooltip.getInstance(trigger);
      if (instance) {
        instance.dispose();
      }
    }
  }
}

customElements.define("cts-tooltip", CtsTooltip);
