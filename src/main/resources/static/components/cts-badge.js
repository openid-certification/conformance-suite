const RESULT_VARIANT_CLASSES = {
  success: "result-success",
  failure: "result-failure",
  warning: "result-warning",
  review: "result-review",
  skipped: "result-skipped",
  interrupted: "result-interrupted",
  info: "result-info",
  finished: "result-finished",
};

const BOOTSTRAP_VARIANT_CLASSES = {
  primary: "bg-primary",
  secondary: "bg-secondary",
  danger: "bg-danger",
  light: "bg-light",
  dark: "bg-dark",
  "info-subtle": "bg-info-subtle border border-info-subtle text-info-emphasis",
};

/**
 * Bootstrap-styled badge. Supports result-state variants (success, failure,
 * warning, review, skipped, interrupted, info, finished) and Bootstrap
 * variants (primary, secondary, danger, light, dark, info-subtle).
 *
 * @property {string} variant - One of: success, failure, warning, review,
 *   skipped, interrupted, info (default), finished, primary, secondary,
 *   danger, light, dark, info-subtle
 * @property {string} label - Visible text
 * @property {number} count - Numeric content; overrides `label` when set
 * @property {string} icon - Bootstrap Icons name (without the `bi-` prefix)
 * @property {boolean} pill - Renders with `rounded-pill`
 * @property {boolean} clickable - Gives the badge a `button` role, keyboard
 *   support, and emits `cts-badge-click` on activation
 *
 * When neither `label` nor `count` is set, the badge wraps whatever child
 * nodes are inside the host element. This is the only way to embed inline
 * `<a>`, `<em>`, or other rich content inside a badge.
 *
 * The slot children are captured ONCE on the first render and cached on the
 * instance (`_capturedChildren`). Every subsequent re-render moves the same
 * cached nodes into the newly-built badge wrapper, so inline rich content
 * survives attribute changes. HOWEVER, children APPENDED AFTER the first
 * render are not picked up — they land inside the current `<span class=
 * "badge">` wrapper and get discarded when the next re-render rebuilds the
 * wrapper. If you need to swap in different rich content dynamically, set
 * the `label` attribute (for plain text), remove + re-insert the element,
 * or update `_capturedChildren` directly before triggering a re-render.
 *
 * @fires cts-badge-click - When the badge is clicked/activated while
 *   `clickable` is set. Bubbles and is composed.
 */
class CtsBadge extends HTMLElement {
  static observedAttributes = ["variant", "label", "count", "icon", "pill", "clickable"];

  connectedCallback() {
    if (this._initialized) return;
    this._initialized = true;
    this._render();
  }

  attributeChangedCallback() {
    if (this._initialized) this._render();
  }

  _variantClass() {
    const variant = this.getAttribute("variant") || "info";
    return RESULT_VARIANT_CLASSES[variant] || BOOTSTRAP_VARIANT_CLASSES[variant] || "bg-info";
  }

  _render() {
    // Capture the original child nodes once. After the first render the host
    // contains the rendered <span class="badge"> wrapper, so re-rendering
    // would otherwise nest spans recursively.
    if (this._capturedChildren === undefined) {
      this._capturedChildren = Array.from(this.childNodes);
    }

    const variantClass = this._variantClass();
    const pill = this.hasAttribute("pill");
    const clickable = this.hasAttribute("clickable");
    const icon = this.getAttribute("icon") || "";
    const label = this.getAttribute("label") || "";
    const countAttr = this.getAttribute("count");
    const hasCount = countAttr !== null && countAttr !== "";
    const hasLabel = label !== "";
    const useSlot = !hasCount && !hasLabel && this._capturedChildren.length > 0;

    const span = document.createElement("span");
    span.className = `badge ${variantClass}${pill ? " rounded-pill" : ""}`;
    if (clickable) {
      span.setAttribute("role", "button");
      span.setAttribute("tabindex", "0");
      span.addEventListener("click", () => this._dispatchClick());
      span.addEventListener("keydown", (e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          this._dispatchClick();
        }
      });
    }

    if (icon) {
      const iconEl = document.createElement("i");
      iconEl.className = `bi bi-${icon}`;
      iconEl.setAttribute("aria-hidden", "true");
      span.appendChild(iconEl);
      if (hasCount || hasLabel || useSlot) {
        span.appendChild(document.createTextNode(" "));
      }
    }

    if (hasCount) {
      span.appendChild(document.createTextNode(countAttr));
    } else if (hasLabel) {
      span.appendChild(document.createTextNode(label));
    } else if (useSlot) {
      for (const child of this._capturedChildren) {
        span.appendChild(child);
      }
    }

    this.replaceChildren(span);
  }

  _dispatchClick() {
    this.dispatchEvent(new CustomEvent("cts-badge-click", { bubbles: true, composed: true }));
  }
}

customElements.define("cts-badge", CtsBadge);
