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
    padding: var(--space-2);
    background: var(--bg-elev);
    color: var(--fg);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    box-shadow: var(--shadow-3);
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
    padding: var(--space-2) var(--space-3);
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
  };

  constructor() {
    super();
    /** @type {Array<OverflowAction>} */
    this.actions = [];
    this.triggerLabel = "More actions";
    this._popoverId = nextPopoverId();
    this._supported = popoverApiSupported();
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

  _handleToggle(event) {
    if (event.newState !== "open") return;
    // Position the popover relative to the trigger's current bounding
    // rect each time it opens. The Popover API renders in the top
    // layer, so position:fixed lifts the menu above any sticky ancestor.
    this._positionPopover();
    // Move focus to the first item so keyboard navigation lands somewhere
    // sensible. The Popover API's default focus moves to the popover
    // root, which isn't focusable — focus the first menu item explicitly.
    queueMicrotask(() => this._focusItemAt(0));
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
    }
  }

  _handleItemClick(event) {
    const target = /** @type {HTMLElement} */ (event.currentTarget);
    const actionId = target && target.dataset ? target.dataset.actionId : undefined;
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
    return html`<li role="none">
      <button
        type="button"
        role="menuitem"
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

    return html`
      <button
        type="button"
        class="overflowTrigger"
        aria-label="${this.triggerLabel}"
        aria-haspopup="menu"
        data-testid="overflow-trigger"
        popovertarget="${ifDefined(this._supported ? this._popoverId : undefined)}"
      >
        <cts-icon name="more-vertical" size="20" aria-hidden="true"></cts-icon>
      </button>
      <div
        id="${this._popoverId}"
        class="overflowPopover"
        popover="auto"
        role="menu"
        aria-label="${this.triggerLabel}"
        data-testid="overflow-popover"
        @beforetoggle=${this._handleToggle}
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
