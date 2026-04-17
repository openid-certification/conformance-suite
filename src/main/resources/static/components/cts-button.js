import { LitElement, html, nothing } from "lit";
import { buildButtonClasses } from "./_button-classes.js";

/**
 * Bootstrap-styled button. Dispatches a bubbling `cts-click` event in addition
 * to the native click.
 *
 * @property {string} variant - One of: light, info, primary, danger, secondary, success, warning
 * @property {string} size - One of: sm (default), md, lg
 * @property {string} label - Visible text
 * @property {string} icon - Bootstrap Icons name (without the `bi-` prefix)
 * @property {boolean} loading - Shows a spinner and disables the button
 * @property {boolean} disabled - Disables the button
 * @property {string} type - Native button type: "button" (default) or "submit"
 * @property {boolean} full-width - Stretches the button to fill its parent's width
 *
 * ## Programmatic activation
 *
 * `host.click()` and `$(host).trigger('click')` fire a synthetic click on the
 * cts-button host element, NOT on the inner `<button>`. Lit's `@click` handler
 * is registered on the inner button, so `_handleClick` does not run and
 * `cts-click` is not dispatched. User clicks still work because the native
 * click bubbles from inner button to host.
 *
 * To activate a cts-button programmatically from tests or automation:
 *
 * ```js
 * // GOOD — triggers the Lit click handler and dispatches cts-click
 * host.querySelector('button').click();
 *
 * // GOOD — listen for cts-click if you want the disabled/loading guard to fire
 * host.addEventListener('cts-click', handler);
 * ```
 *
 * ## Light-DOM dependencies
 *
 * cts-button intentionally renders to its own light DOM (see
 * `createRenderRoot()`). ClipboardJS (`.btn-clipboard`), Bootstrap 5 data
 * attributes (`data-bs-dismiss`, `data-bs-toggle`), and jQuery delegated
 * handlers rely on the native click bubbling from the inner button through
 * the host — all of these break silently if this component ever switches to
 * shadow DOM or adds `event.preventDefault()` inside `_handleClick`. Do not
 * change the render root without first migrating every consumer.
 */
class CtsButton extends LitElement {
  static properties = {
    variant: { type: String },
    size: { type: String },
    label: { type: String },
    icon: { type: String },
    loading: { type: Boolean },
    disabled: { type: Boolean },
    type: { type: String },
    fullWidth: { type: Boolean, attribute: "full-width", reflect: true },
  };

  constructor() {
    super();
    this.variant = "light";
    this.size = "sm";
    this.label = "";
    this.icon = "";
    this.loading = false;
    this.disabled = false;
    this.type = "button";
    this.fullWidth = false;
  }

  createRenderRoot() { return this; }

  updated(changed) {
    if (changed.has("fullWidth")) {
      // Light-DOM components can't style their own host from CSS;
      // set display imperatively so the host stretches in block/flex/grid parents.
      this.style.display = this.fullWidth ? "block" : "";
    }
  }

  _handleClick() {
    if (this.disabled || this.loading) return;
    this.dispatchEvent(
      new CustomEvent("cts-click", { bubbles: true, composed: true }),
    );
  }

  // Icon names come from the Bootstrap Icons set (2000+ icons).
  // Constructed from the icon prop, not a finite variant set.
  _iconClass() {
    return `bi bi-${this.icon}`;
  }

  _renderIcon() {
    if (this.loading) {
      return html`<span
        class="spinner-border spinner-border-sm"
        role="status"
        aria-hidden="true"
      ></span>`;
    }
    if (this.icon) {
      return html`<span
        class="${this._iconClass()}"
        aria-hidden="true"
      ></span>`;
    }
    return nothing;
  }

  render() {
    const isDisabled = this.disabled || this.loading;
    const iconContent = this._renderIcon();
    const hasIcon = iconContent !== nothing;
    const buttonClass = buildButtonClasses({ variant: this.variant, size: this.size, fullWidth: this.fullWidth });
    return html`<button
      type="${this.type}"
      class="${buttonClass}"
      ?disabled="${isDisabled}"
      @click="${this._handleClick}"
    >${iconContent}${hasIcon && this.label ? " " : ""}${this.label
        ? this.label
        : nothing}</button
    >`;
  }
}

customElements.define("cts-button", CtsButton);
