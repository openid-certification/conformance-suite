import { LitElement, html, nothing } from "lit";

/**
 * Shared `<style>` block injected into `<head>` once per page (gated by
 * `STYLE_ID`). Mirrors the head-style injection pattern in cts-card.js
 * and cts-tabs.js so a page that hosts many `cts-empty-state` instances
 * still pays for the rules exactly once.
 *
 * Layout intent (matches the OIDF design archive empty-state pattern):
 *   - Centered flex column with `align-items: center` and a generous gap
 *     scaled from `--space-4` so the icon, heading, body, and CTA stack
 *     with consistent vertical rhythm.
 *   - Icon glyph rendered above the heading at `--ink-300` so it reads
 *     as a soft watermark rather than competing with the heading.
 *   - Heading uses the standard h2 stack (no override needed — the host
 *     inherits the page's typographic scale).
 *   - Body text uses `--fg-soft` so it reads as secondary copy under the
 *     heading.
 *   - Single CTA below the body. The component renders either a built-in
 *     `<cts-link-button>` (when both `cta-label` and `cta-href` are set)
 *     or whatever the consumer puts in the default slot.
 */
const STYLE_ID = "cts-empty-state-styles";

const STYLE_TEXT = `
.oidf-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  padding: var(--space-6) var(--space-5);
  text-align: center;
}
.oidf-empty-state-icon {
  color: var(--ink-300);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.oidf-empty-state-heading {
  margin: 0;
  color: var(--fg);
  font-family: var(--font-sans);
  font-size: var(--fs-18);
  font-weight: var(--fw-bold);
  line-height: var(--lh-snug);
}
.oidf-empty-state-body {
  margin: 0;
  color: var(--fg-soft);
  font-family: var(--font-sans);
  font-size: var(--fs-14);
  line-height: var(--lh-normal);
  max-width: 48ch;
}
.oidf-empty-state-cta {
  display: inline-flex;
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
 * Empty-state pattern: optional icon, heading, optional body copy, optional
 * CTA. Used in dashboard tiles, table-empty rows, and onboarding surfaces.
 *
 * Composition:
 *   - Icon (when `icon` is set) renders via `<cts-icon>` at size 24, tinted
 *     `--ink-300`.
 *   - Heading (required) renders as a `<h2>` styled with the OIDF type
 *     scale.
 *   - Body (optional) renders as a `<p>` in `--fg-soft`.
 *   - CTA (optional) renders a `<cts-link-button variant="primary" size="sm">`
 *     when both `cta-label` and `cta-href` are present. Otherwise the
 *     component falls back to the default slot, so consumers can pass an
 *     arbitrary CTA element (e.g. a `<cts-button>` wired to a click handler).
 *
 * Light DOM. Scoped CSS lives in a single `<style>` element injected into
 * `<head>` on first connect. The `:not(:defined)` block-level fallback is
 * declared in `css/layout.css` (Coherence F4 — empty states are often used
 * as a block-level fill, so we reserve a 120px slot pre-upgrade).
 *
 * @property {string} heading - Heading text (required for a meaningful
 *   empty state).
 * @property {string} body - Optional secondary copy rendered under the
 *   heading in `--fg-soft`. Omit to drop the paragraph entirely.
 * @property {string} icon - Optional Bootstrap Icons name (without the
 *   `bi-` prefix, e.g. `inbox`). Omit to drop the glyph.
 * @property {string} cta-label - Optional label for the built-in primary
 *   CTA. Requires `cta-href`. Both must be set for the built-in CTA to
 *   render; otherwise the component falls back to the default slot.
 * @property {string} cta-href - Optional href for the built-in primary
 *   CTA. See `cta-label`.
 */
class CtsEmptyState extends LitElement {
  static properties = {
    heading: { type: String },
    body: { type: String },
    icon: { type: String },
    ctaLabel: { type: String, attribute: "cta-label" },
    ctaHref: { type: String, attribute: "cta-href" },
  };

  constructor() {
    super();
    this.heading = "";
    this.body = "";
    this.icon = "";
    this.ctaLabel = "";
    this.ctaHref = "";
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  createRenderRoot() {
    return this;
  }

  _renderIcon() {
    if (!this.icon) return nothing;
    return html`<span class="oidf-empty-state-icon">
      <cts-icon name=${this.icon} size="24"></cts-icon>
    </span>`;
  }

  _renderBody() {
    if (!this.body) return nothing;
    return html`<p class="oidf-empty-state-body">${this.body}</p>`;
  }

  _renderCta() {
    // Both attributes must be set for the built-in CTA to render. Otherwise
    // the consumer's slot content is used unchanged — supporting arbitrary
    // CTAs (cts-button with an event handler, multi-action stacks, etc.).
    if (this.ctaLabel && this.ctaHref) {
      return html`<span class="oidf-empty-state-cta">
        <cts-link-button
          variant="primary"
          size="sm"
          href=${this.ctaHref}
          label=${this.ctaLabel}
        ></cts-link-button>
      </span>`;
    }
    return html`<span class="oidf-empty-state-cta"><slot></slot></span>`;
  }

  render() {
    return html`<div class="oidf-empty-state">
      ${this._renderIcon()}
      <h2 class="oidf-empty-state-heading">${this.heading}</h2>
      ${this._renderBody()} ${this._renderCta()}
    </div>`;
  }
}

customElements.define("cts-empty-state", CtsEmptyState);

export {};
