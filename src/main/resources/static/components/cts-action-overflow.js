import { LitElement, html, nothing } from "lit";
import { ifDefined } from "lit/directives/if-defined.js";
import "./cts-icon.js";

const STYLE_ID = "cts-action-overflow-styles";

// Scoped CSS for the kebab trigger and the popover surface. Tokens flow
// from oidf-tokens.css. The popover is rendered in the top layer (via the
// HTML Popover API), so it does not need a z-index dance.
const STYLE_TEXT = `
  cts-action-overflow {
    display: inline-flex;
  }

  cts-action-overflow .overflowTrigger {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: var(--space-8);
    height: var(--space-8);
    padding: 0;
    background: transparent;
    color: var(--fg);
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    cursor: pointer;
    line-height: 1;
  }
  cts-action-overflow .overflowTrigger:hover {
    background: var(--bg-muted);
  }
  cts-action-overflow .overflowTrigger:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }

  /* The popover container itself. Reset UA defaults (margin, padding,
     border) and replace with the design-system surface treatment. The
     :popover-open pseudo-class doesn't need a separate style — the
     default closed state is display:none, and the open state inherits
     these rules. */
  cts-action-overflow .overflowPopover {
    margin: 0;
    padding: var(--space-1);
    background: var(--bg-elev);
    color: var(--fg);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    box-shadow: var(--shadow-3);
    /* Popover-scale typography: matches the navbar account menu so all
       overlay surfaces in the app share one text scale. Children pick this
       up via .overflowItem { font: inherit }. */
    font-size: var(--fs-13);
    min-width: 220px;
    /* Position is set imperatively in _positionPopover() against the
       trigger's bounding rect. The Popover API renders in the top layer,
       so position:fixed flows above any sticky/transformed ancestor. */
    position: fixed;
    inset: auto;
  }

  cts-action-overflow .overflowMenu {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    list-style: none;
    margin: 0;
    padding: 0;
  }

  cts-action-overflow .overflowItem {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    width: 100%;
    padding: var(--space-2);
    background: transparent;
    color: var(--fg);
    border: none;
    border-radius: var(--radius-2);
    font: inherit;
    text-align: left;
    cursor: pointer;
  }
  cts-action-overflow .overflowItem:hover {
    background: var(--bg-muted);
  }
  cts-action-overflow .overflowItem:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-action-overflow .overflowItem--danger {
    color: var(--status-fail);
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

let popoverIdCounter = 0;
function nextPopoverId() {
  popoverIdCounter += 1;
  return `cts-action-overflow-popover-${popoverIdCounter}`;
}

let triggerIdCounter = 0;
function nextTriggerId() {
  triggerIdCounter += 1;
  return `cts-action-overflow-trigger-${triggerIdCounter}`;
}

function popoverApiSupported() {
  return (
    typeof HTMLElement !== "undefined" &&
    Object.prototype.hasOwnProperty.call(HTMLElement.prototype, "popover")
  );
}

/**
 * @typedef {object} OverflowAction
 * @property {string} id - Stable identifier returned in the
 *   `cts-overflow-action` event detail. Used by the host to route the
 *   activation back to the matching handler.
 * @property {string} label - Visible text for the menu row.
 * @property {string} [icon] - Coolicons name; renders as `<cts-icon>`
 *   inline before the label.
 * @property {boolean} [hidden] - When true, omit the action from the
 *   popover entirely. Mobile-friendly principle: no greyed-out items
 *   in a popover; either show or hide.
 * @property {('primary'|'secondary'|'danger')} [variant] - Visual
 *   weight; the only treatment today is a colour shift for `danger`.
 *   Defaults to `secondary`.
 */

/**
 * Kebab-triggered popover surface for secondary actions in the status bar.
 * Uses the native HTML Popover API (Chrome ≥ 114, Firefox ≥ 125, Safari
 * ≥ 17.0). The popover renders in the top layer with native light-dismiss
 * (outside click + Escape) and focus-management (focus moves into the
 * popover on show, returns to the trigger on dismiss). On browsers that
 * lack the Popover API, the trigger silently does nothing — that ships as
 * a known constraint until the manual focus-trap fallback lands; the
 * production audience runs current browsers.
 *
 * Light DOM. Scoped CSS is injected once on first render.
 *
 * @property {Array<OverflowAction>} actions - Action descriptors. Hidden
 *   actions are omitted entirely (no greyed-out items in a popover).
 * @property {string} triggerLabel - Accessible label for the kebab
 *   trigger. Defaults to "More actions".
 * @fires cts-overflow-action - When an item is activated, with
 *   `{ detail: { actionId } }`. Bubbles AND is composed.
 */
class CtsActionOverflow extends LitElement {
  static properties = {
    actions: { type: Array },
    triggerLabel: { type: String, attribute: "trigger-label" },
    // Internal-reactive open state mirrors the popover's :popover-open
    // status so we can reflect it as aria-expanded on the trigger.
    // beforetoggle/toggle events drive it; consumers don't set it.
    _open: { state: true },
  };

  constructor() {
    super();
    /** @type {Array<OverflowAction>} */
    this.actions = [];
    this.triggerLabel = "More actions";
    this._popoverId = nextPopoverId();
    this._triggerId = nextTriggerId();
    this._supported = popoverApiSupported();
    this._open = false;
    // Where to land focus the next time the popover opens. Set by the
    // trigger's ArrowUp handler to focus the last item; otherwise 0.
    /** @type {number} */
    this._pendingFocusIndex = 0;
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  _visibleActions() {
    return Array.isArray(this.actions) ? this.actions.filter((a) => a && !a.hidden) : [];
  }

  _menuItems() {
    return /** @type {NodeListOf<HTMLButtonElement>} */ (this.querySelectorAll(".overflowItem"));
  }

  _focusItemAt(index) {
    const items = this._menuItems();
    if (items.length === 0) return;
    const wrapped = (index + items.length) % items.length;
    items[wrapped].focus();
  }

  _focusedItemIndex() {
    const items = Array.from(this._menuItems());
    return items.indexOf(/** @type {HTMLButtonElement} */ (document.activeElement));
  }

  // beforetoggle fires before the visual state change. Use it for work
  // that has to happen prior to paint (positioning) and to mirror the
  // open state into aria-expanded synchronously.
  _handleBeforeToggle(event) {
    if (event.newState === "open") {
      // Position the popover relative to the trigger's current bounding
      // rect each time it opens. The Popover API renders in the top
      // layer, so position:fixed lifts the menu above any sticky ancestor.
      this._positionPopover();
    }
    this._open = event.newState === "open";
  }

  // toggle fires after the popover is in its final visible state, so
  // focus calls land on a paintable element. The Popover API's default
  // focus targets the popover root (not focusable), so we move focus to
  // a menuitem explicitly. Index respects the trigger's keyboard intent
  // (ArrowUp on the trigger asks for the last item).
  _handleToggle(event) {
    if (event.newState !== "open") return;
    const initial = this._pendingFocusIndex ?? 0;
    this._pendingFocusIndex = 0;
    this._focusItemAt(initial);
  }

  _positionPopover() {
    const trigger = this.querySelector(".overflowTrigger");
    const popover = this.querySelector(".overflowPopover");
    if (!trigger || !popover) return;
    const rect = trigger.getBoundingClientRect();
    const popoverEl = /** @type {HTMLElement} */ (popover);
    // Default placement: bottom-end (right-aligned with the trigger).
    // The popover surface is in the top layer, so the offset values are
    // viewport-relative.
    popoverEl.style.top = `${Math.round(rect.bottom + 4)}px`;
    // Right-align the popover with the trigger to mirror the visual
    // weight of action overflow menus on most apps.
    const popoverWidth = popoverEl.offsetWidth || 220;
    popoverEl.style.left = `${Math.max(8, Math.round(rect.right - popoverWidth))}px`;
  }

  _handleItemKeydown(event) {
    const key = event.key;
    if (key === "ArrowDown") {
      event.preventDefault();
      this._focusItemAt(this._focusedItemIndex() + 1);
    } else if (key === "ArrowUp") {
      event.preventDefault();
      this._focusItemAt(this._focusedItemIndex() - 1);
    } else if (key === "Home") {
      event.preventDefault();
      this._focusItemAt(0);
    } else if (key === "End") {
      event.preventDefault();
      this._focusItemAt(this._menuItems().length - 1);
    } else if (key === "Tab") {
      // APG menu pattern: Tab closes the menu and lets the browser
      // continue its tab traversal (Shift+Tab too — closes, then default
      // moves focus to the previous focusable element). Don't
      // preventDefault — we want native focus advancement to happen.
      // Native popover light-dismiss covers Escape and outside-click but
      // does NOT dismiss on focus moving outside via Tab, so this
      // handler is required to keep visual + AT state consistent.
      this._hidePopover();
    }
  }

  // Closed-trigger keyboard support (APG menu button):
  //   - Down opens the menu and focuses the first item.
  //   - Up opens the menu and focuses the last item.
  //   - Enter/Space rely on the default <button> activation, which
  //     invokes popovertarget — handled natively, no extra wiring.
  _handleTriggerKeydown(event) {
    if (!this._supported) return;
    const key = event.key;
    if (key !== "ArrowDown" && key !== "ArrowUp") return;
    event.preventDefault();
    this._pendingFocusIndex = key === "ArrowUp" ? -1 : 0;
    const popover = /** @type {HTMLElement & { showPopover?: () => void }} */ (
      this.querySelector(".overflowPopover")
    );
    if (popover && typeof popover.showPopover === "function") {
      try {
        popover.showPopover();
      } catch {
        // Already open — _focusItemAt below still lands on the requested item.
      }
    }
  }

  _hidePopover() {
    const popover = /** @type {HTMLElement & { hidePopover?: () => void }} */ (
      this.querySelector(".overflowPopover")
    );
    if (popover && typeof popover.hidePopover === "function") {
      try {
        popover.hidePopover();
      } catch {
        // Ignore — already hidden, or the API is unavailable.
      }
    }
  }

  _handleItemClick(event) {
    const target = /** @type {HTMLElement} */ (event.currentTarget);
    const actionId = target && target.dataset ? target.dataset.actionId : undefined;
    this._hidePopover();
    if (!actionId) return;
    this.dispatchEvent(
      new CustomEvent("cts-overflow-action", {
        bubbles: true,
        composed: true,
        detail: { actionId },
      }),
    );
  }

  _renderItem(action) {
    const variantClass =
      action.variant === "danger" ? "overflowItem overflowItem--danger" : "overflowItem";
    // tabindex="-1" on every menuitem keeps the items out of the page's
    // tab order (APG Menu pattern). Focus is moved between items
    // imperatively via Arrow/Home/End handlers; Tab from any item
    // closes the popover and lets the browser advance to the next
    // focusable element on the page.
    return html`<li role="none">
      <button
        type="button"
        role="menuitem"
        tabindex="-1"
        class="${variantClass}"
        data-action-id="${action.id}"
        @click=${this._handleItemClick}
        @keydown=${this._handleItemKeydown}
      >
        ${action.icon
          ? html`<cts-icon name="${action.icon}" size="20" aria-hidden="true"></cts-icon>`
          : nothing}
        <span class="overflowLabel">${action.label}</span>
      </button>
    </li>`;
  }

  render() {
    const visible = this._visibleActions();
    if (visible.length === 0) return nothing;

    // ARIA wiring (APG Menu Button + Menu patterns):
    //   - aria-haspopup="menu" + aria-controls + aria-expanded reflect
    //     the trigger's relationship to the popover and its open state.
    //     All three are gated on _supported so we don't claim "this
    //     opens a menu" on browsers where the popover never appears.
    //   - The menu uses aria-labelledby pointing at the trigger
    //     instead of a duplicated aria-label, so SRs don't read back
    //     "More actions menu, More actions" when focus enters an item.
    //   - beforetoggle handles position + state-mirroring (synchronous,
    //     pre-paint); toggle handles focus management (post-paint, so
    //     .focus() lands on a paintable element).
    const ariaHaspopup = this._supported ? "menu" : undefined;
    const ariaControls = this._supported ? this._popoverId : undefined;
    const ariaExpanded = this._supported ? (this._open ? "true" : "false") : undefined;

    return html`
      <button
        id="${this._triggerId}"
        type="button"
        class="overflowTrigger"
        aria-label="${this.triggerLabel}"
        aria-haspopup="${ifDefined(ariaHaspopup)}"
        aria-controls="${ifDefined(ariaControls)}"
        aria-expanded="${ifDefined(ariaExpanded)}"
        data-testid="overflow-trigger"
        popovertarget="${ifDefined(this._supported ? this._popoverId : undefined)}"
        @keydown=${this._handleTriggerKeydown}
      >
        <cts-icon name="more-vertical" size="20" aria-hidden="true"></cts-icon>
      </button>
      <div
        id="${this._popoverId}"
        class="overflowPopover"
        popover="auto"
        role="menu"
        aria-labelledby="${this._triggerId}"
        data-testid="overflow-popover"
        @beforetoggle=${this._handleBeforeToggle}
        @toggle=${this._handleToggle}
      >
        <ul class="overflowMenu" role="presentation">
          ${visible.map((action) => this._renderItem(action))}
        </ul>
      </div>
    `;
  }
}

customElements.define("cts-action-overflow", CtsActionOverflow);

export {};
