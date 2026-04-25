/**
 * Self-contained tooltip primitive. Wraps a single child trigger; on
 * `mouseenter`/`focusin` it positions a `<div class="oidf-tooltip">` in
 * `document.body` relative to the trigger's bounding rect, and removes it on
 * `mouseleave`/`focusout`/`Escape`.
 *
 * No Bootstrap, no Popper, no global state. The tooltip element is appended
 * to `document.body` so it never gets clipped by an `overflow: hidden`
 * ancestor.
 *
 * Vanilla HTMLElement — has no `static properties`; attributes are read
 * directly in `connectedCallback` and on each show.
 * @property {string} content - Tooltip text (read from the `content`
 *   attribute). When absent, the component is inert.
 * @property {string} placement - One of `top` (default), `bottom`, `left`,
 *   `right`, `auto`. `auto` picks the side with the most viewport space.
 */

// Max time to wait for a dynamically-inserted child after the element is
// connected. If no child appears within this window, we give up silently —
// the component is probably being used incorrectly, but we don't want a
// stray observer holding a reference forever.
const DYNAMIC_INIT_TIMEOUT_MS = 2000;

// Distance between the trigger edge and the tooltip body. Matches the
// chevron arrow size below.
const OFFSET_PX = 8;

// Half the chevron arrow width — used to position the arrow centred under
// the tooltip on top/bottom placements (or vertically on left/right).
const ARROW_HALF_PX = 6;

const VALID_PLACEMENTS = new Set(["top", "bottom", "left", "right", "auto"]);

let stylesInjected = false;

function injectStylesOnce() {
  if (stylesInjected) return;
  stylesInjected = true;
  const style = document.createElement("style");
  style.setAttribute("data-cts-tooltip-styles", "");
  style.textContent = `
    .oidf-tooltip {
      position: absolute;
      top: 0;
      left: 0;
      z-index: 1080;
      max-width: 280px;
      padding: 6px 10px;
      background: var(--ink-900);
      color: var(--ink-0);
      font-family: var(--font-sans);
      font-size: var(--fs-12);
      line-height: 1.35;
      border-radius: var(--radius-2);
      box-shadow: var(--shadow-2);
      pointer-events: none;
      word-wrap: break-word;
    }
    .oidf-tooltip__arrow {
      position: absolute;
      width: 0;
      height: 0;
      border: ${ARROW_HALF_PX}px solid transparent;
    }
    .oidf-tooltip[data-placement="top"] .oidf-tooltip__arrow {
      bottom: -${ARROW_HALF_PX}px;
      border-top-color: var(--ink-900);
      border-bottom-width: 0;
    }
    .oidf-tooltip[data-placement="bottom"] .oidf-tooltip__arrow {
      top: -${ARROW_HALF_PX}px;
      border-bottom-color: var(--ink-900);
      border-top-width: 0;
    }
    .oidf-tooltip[data-placement="left"] .oidf-tooltip__arrow {
      right: -${ARROW_HALF_PX}px;
      border-left-color: var(--ink-900);
      border-right-width: 0;
    }
    .oidf-tooltip[data-placement="right"] .oidf-tooltip__arrow {
      left: -${ARROW_HALF_PX}px;
      border-right-color: var(--ink-900);
      border-left-width: 0;
    }
  `;
  document.head.appendChild(style);
}

class CtsTooltip extends HTMLElement {
  connectedCallback() {
    injectStylesOnce();

    const content = this.getAttribute("content") || "";
    if (!content) return;
    if (this._wireUpIfReady()) return;

    // No direct child yet. Watch for one — supports dynamic insertion via
    // innerHTML after connect, templating libraries that attach children
    // post-render, etc. Bounded so a cts-tooltip that never gets a child
    // doesn't leak an observer.
    this._childObserver = new MutationObserver(() => {
      if (this._wireUpIfReady()) this._stopObserving();
    });
    this._childObserver.observe(this, { childList: true });
    this._initTimer = setTimeout(() => this._stopObserving(), DYNAMIC_INIT_TIMEOUT_MS);
  }

  disconnectedCallback() {
    this._stopObserving();
    this._teardownTrigger();
    this._removeTooltip();
  }

  _wireUpIfReady() {
    const trigger = this.querySelector(":scope > *");
    if (!trigger) return false;

    this._trigger = trigger;
    // Bound handlers so add/removeEventListener pair correctly.
    this._onShow = () => this._show();
    this._onHide = () => this._hide();
    this._onKey = (/** @type {Event} */ e) => {
      if (/** @type {KeyboardEvent} */ (e).key === "Escape") this._hide();
    };

    trigger.addEventListener("mouseenter", this._onShow);
    trigger.addEventListener("mouseleave", this._onHide);
    trigger.addEventListener("focusin", this._onShow);
    trigger.addEventListener("focusout", this._onHide);
    trigger.addEventListener("keydown", this._onKey);

    return true;
  }

  _teardownTrigger() {
    const trigger = this._trigger;
    if (!trigger) return;
    if (this._onShow) {
      trigger.removeEventListener("mouseenter", this._onShow);
      trigger.removeEventListener("focusin", this._onShow);
    }
    if (this._onHide) {
      trigger.removeEventListener("mouseleave", this._onHide);
      trigger.removeEventListener("focusout", this._onHide);
    }
    if (this._onKey) {
      trigger.removeEventListener("keydown", this._onKey);
    }
    this._trigger = null;
  }

  _show() {
    // Re-read content so dynamic content updates take effect on next show.
    const content = this.getAttribute("content") || "";
    if (!content || !this._trigger) return;

    // If a tooltip is already showing for this instance, just reposition.
    if (!this._tooltipEl) {
      const el = document.createElement("div");
      el.className = "oidf-tooltip";
      el.setAttribute("role", "tooltip");
      const arrow = document.createElement("span");
      arrow.className = "oidf-tooltip__arrow";
      const body = document.createElement("span");
      body.className = "oidf-tooltip__inner";
      body.textContent = content;
      el.appendChild(body);
      el.appendChild(arrow);
      document.body.appendChild(el);
      this._tooltipEl = el;
      this._arrowEl = arrow;
    } else {
      const inner = this._tooltipEl.querySelector(".oidf-tooltip__inner");
      if (inner) inner.textContent = content;
    }

    this._position();
  }

  _hide() {
    this._removeTooltip();
  }

  _removeTooltip() {
    if (this._tooltipEl && this._tooltipEl.parentNode) {
      this._tooltipEl.parentNode.removeChild(this._tooltipEl);
    }
    this._tooltipEl = null;
    this._arrowEl = null;
  }

  _resolvePlacement() {
    const raw = this.getAttribute("placement") || "top";
    const requested = VALID_PLACEMENTS.has(raw) ? raw : "top";
    if (requested !== "auto") return requested;
    if (!this._trigger || !this._tooltipEl) return "top";

    // Pick the side with the most viewport space.
    const triggerRect = this._trigger.getBoundingClientRect();
    const tipRect = this._tooltipEl.getBoundingClientRect();
    const vw = window.innerWidth;
    const vh = window.innerHeight;

    const space = {
      top: triggerRect.top,
      bottom: vh - triggerRect.bottom,
      left: triggerRect.left,
      right: vw - triggerRect.right,
    };
    const need = {
      top: tipRect.height + OFFSET_PX,
      bottom: tipRect.height + OFFSET_PX,
      left: tipRect.width + OFFSET_PX,
      right: tipRect.width + OFFSET_PX,
    };

    // Prefer top when it fits; then bottom; then the side with the most
    // remaining space among any that fit; otherwise the side with the most
    // raw space.
    if (space.top >= need.top) return "top";
    if (space.bottom >= need.bottom) return "bottom";
    /** @type {Array<"top"|"bottom"|"left"|"right">} */
    const order = ["top", "bottom", "right", "left"];
    let best = order[0];
    let bestSpace = space[best];
    for (const side of order) {
      if (space[side] > bestSpace) {
        best = side;
        bestSpace = space[side];
      }
    }
    return best;
  }

  _position() {
    if (!this._trigger || !this._tooltipEl) return;

    const placement = this._resolvePlacement();
    this._tooltipEl.setAttribute("data-placement", placement);

    const triggerRect = this._trigger.getBoundingClientRect();
    const tipRect = this._tooltipEl.getBoundingClientRect();
    // Page-relative coordinates so absolute positioning survives scroll.
    const scrollX = window.scrollX || window.pageXOffset;
    const scrollY = window.scrollY || window.pageYOffset;

    let top;
    let left;
    let arrowLeft = "";
    let arrowTop = "";

    switch (placement) {
      case "bottom":
        top = triggerRect.bottom + scrollY + OFFSET_PX;
        left = triggerRect.left + scrollX + (triggerRect.width - tipRect.width) / 2;
        arrowLeft = `${tipRect.width / 2 - ARROW_HALF_PX}px`;
        break;
      case "left":
        top = triggerRect.top + scrollY + (triggerRect.height - tipRect.height) / 2;
        left = triggerRect.left + scrollX - tipRect.width - OFFSET_PX;
        arrowTop = `${tipRect.height / 2 - ARROW_HALF_PX}px`;
        break;
      case "right":
        top = triggerRect.top + scrollY + (triggerRect.height - tipRect.height) / 2;
        left = triggerRect.right + scrollX + OFFSET_PX;
        arrowTop = `${tipRect.height / 2 - ARROW_HALF_PX}px`;
        break;
      case "top":
      default:
        top = triggerRect.top + scrollY - tipRect.height - OFFSET_PX;
        left = triggerRect.left + scrollX + (triggerRect.width - tipRect.width) / 2;
        arrowLeft = `${tipRect.width / 2 - ARROW_HALF_PX}px`;
        break;
    }

    // Clamp horizontally to the viewport so a centred tooltip near a screen
    // edge doesn't overflow. Vertical clamp not applied — auto placement
    // already chose the side with the most space.
    const minLeft = scrollX + 4;
    const maxLeft = scrollX + window.innerWidth - tipRect.width - 4;
    if (left < minLeft) left = minLeft;
    if (left > maxLeft) left = maxLeft;

    this._tooltipEl.style.top = `${top}px`;
    this._tooltipEl.style.left = `${left}px`;

    if (this._arrowEl) {
      this._arrowEl.style.left = arrowLeft || "";
      this._arrowEl.style.top = arrowTop || "";
    }
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
}

customElements.define("cts-tooltip", CtsTooltip);
