import { LitElement, html, css } from "lit";
// Self-import the spinner dependency: render() emits <cts-spinner>, so the
// element must be defined whenever this component is used. Importing it here
// (rather than relying on each consumer to register it) makes any consumer
// safe — mirrors how cts-empty-state.js self-imports cts-link-button.
import "./cts-spinner.js";

/**
 * Shared `<style>` block injected into `<head>` once per page (gated by
 * `STYLE_ID`). Mirrors the head-style injection pattern in cts-empty-state.js
 * so a page that hosts many `cts-loading-state` instances still pays for the
 * rules exactly once.
 *
 * Layout intent: a centered flex column with `align-items: center` and a
 * small gap so the spinner sits above its caption with consistent rhythm.
 * The caption inherits `--fg-soft` so it reads as secondary copy. These are
 * the exact wrapper rules that cts-log-list and cts-plan-list previously
 * duplicated as `.cts-log-list-loading` / `.cts-plan-list-loading`; factoring
 * them here keeps the two lists' loading states pixel-identical.
 */
const STYLE_ID = "cts-loading-state-styles";

const STYLE_TEXT = css`
  .cts-loading-state {
    padding: var(--space-5);
    text-align: center;
    color: var(--fg-soft);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);
  }
`;

function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/**
 * Loading-state pattern: a `<cts-spinner>` above a caption, centered in a
 * flex column. The shared loading equivalent of `cts-empty-state` — both
 * list components (`cts-log-list`, `cts-plan-list`) delegate their loading
 * branch here so the spinner is consistent across the app.
 *
 * Accessibility is owned by `cts-spinner`: it carries `role="status"` and an
 * `aria-label`/visually-hidden copy of `label`, so AT announces the loading
 * activity when the element appears. The visible caption (`label` + an `…`
 * ellipsis) is purely visual and duplicates that announced text, matching the
 * markup `cts-log-list` shipped before this component existed.
 *
 * Light DOM (`createRenderRoot` returns `this`). Scoped CSS lives in a single
 * `<style>` element injected into `<head>` on first connect. The
 * `:not(:defined)` block-level fallback is declared in `css/layout.css`
 * (mirrors cts-empty-state) so the host reserves height before it upgrades.
 *
 * @property {string} label - Activity description used both as the spinner's
 *   accessible name and as the visible caption (rendered with a trailing
 *   `…`). Defaults to "Loading".
 */
class CtsLoadingState extends LitElement {
  static properties = {
    label: { type: String },
  };

  constructor() {
    super();
    this.label = "Loading";
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  createRenderRoot() {
    return this;
  }

  render() {
    return html`<div class="cts-loading-state">
      <cts-spinner size="lg" label=${this.label}></cts-spinner>
      <span class="cts-loading-state-caption">${this.label}…</span>
    </div>`;
  }
}

customElements.define("cts-loading-state", CtsLoadingState);

export {};
