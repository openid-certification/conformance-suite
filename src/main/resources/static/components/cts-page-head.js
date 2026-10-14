import { LitElement, html, nothing } from "lit";

/**
 * Standard page-header pattern used by every page-level migration in
 * Phase D: a title (`.t-title`, 32px bold) with optional subtitle
 * (`.t-meta`) on the left, plus an actions container on the right that
 * accepts any markup placed inside `<cts-page-head slot="actions">…`
 * children — typically `<cts-button>` / `<cts-link-button>` controls.
 *
 * Light DOM. Scoped CSS lives in a single `<style>` element injected into
 * `<head>` on first connect (gated by a module-level flag) so the rules
 * appear once regardless of how many `cts-page-head` instances are on
 * the page.
 *
 * Children placed with `slot="actions"` are captured ONCE on the first
 * render and moved into the right-hand actions container. This mirrors
 * the slot-children capture pattern documented in
 * components/AGENTS.md §4 — to update the actions list dynamically after
 * mount, recreate the element rather than mutating its children.
 *
 * @property {string} title - Page title text. Renders inside an `<h1>`
 *   styled with `.t-title`.
 * @property {string} sub - Optional subtitle text. When omitted, the
 *   subtitle paragraph is not rendered.
 */
class CtsPageHead extends LitElement {
  static properties = {
    title: { type: String },
    sub: { type: String },
  };

  constructor() {
    super();
    this.title = "";
    this.sub = "";
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    // Capture slotted action children BEFORE LitElement's first render
    // overwrites the host's children. The captured nodes are moved into
    // the actions container in `firstUpdated()`.
    if (this._capturedActions === undefined) {
      this._capturedActions = Array.from(this.querySelectorAll(':scope > [slot="actions"]'));
      // Detach immediately so Lit's render does not see them as part of
      // the render root's existing content.
      for (const node of this._capturedActions) {
        node.remove();
      }
    }
    super.connectedCallback();
  }

  firstUpdated() {
    const actionsContainer = this.querySelector(".oidf-page-head-actions");
    if (!actionsContainer || !this._capturedActions) return;
    for (const node of this._capturedActions) {
      // Strip the slot attribute now that the node is in its rendered
      // home — light-DOM consumers do not expect to see leftover slot
      // metadata on the rendered tree.
      if (node instanceof Element) {
        node.removeAttribute("slot");
      }
      actionsContainer.appendChild(node);
    }
  }

  render() {
    return html`<div class="oidf-page-head">
      <div class="oidf-page-head-text">
        <h1 class="t-title oidf-page-head-title">${this.title}</h1>
        ${this.sub ? html`<p class="t-meta oidf-page-head-sub">${this.sub}</p>` : nothing}
      </div>
      <div class="oidf-page-head-actions"></div>
    </div>`;
  }
}

customElements.define("cts-page-head", CtsPageHead);

export {};
