import { LitElement, html, nothing } from "lit";
import "./cts-alert.js";
import { splitTestSummary } from "./test-summary-split.js";

const STYLE_ID = "cts-test-summary-styles";

// Scoped CSS for the test summary. The summaryEyebrow / summaryBody / summaryZone
// rules mirror the inline styles previously rendered inside
// cts-log-detail-header._renderSummaryZones() so the visual treatment is
// preserved when the alerts move out of the header card.
const STYLE_TEXT = `
  cts-test-summary {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  cts-test-summary .summaryZone {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }
  cts-test-summary .summaryEyebrow {
    font-size: var(--fs-12);
    font-weight: var(--fw-bold);
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--fg-soft);
  }
  cts-test-summary .summaryBody {
    color: var(--fg);
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Renders the R24 about / instructions alerts in two zones using the shared
 * splitTestSummary() helper. Designed to be hoistable out of
 * cts-log-detail-header so the WAITING-state instructions banner can render
 * in Region B1 (above the failure summary) without dragging the rest of the
 * header card. Three positions, one component:
 *   - inside cts-log-detail-header's card (desktop B4 position)
 *   - directly below the sticky status bar (mobile / tablet B1 position)
 *   - future hosts (e.g. notification surfaces) reuse the same component
 *
 * Light DOM. Scoped CSS is injected once on first render. All visual styling
 * routes through the OIDF tokens vendored in `oidf-tokens.css`.
 *
 * @property {string} summary - The raw `test.summary` string (may contain
 *   the `\n\n---\n\n` split marker per test-summary-split.js). Empty / null
 *   renders nothing.
 */
class CtsTestSummary extends LitElement {
  static properties = {
    summary: { type: String },
  };

  constructor() {
    super();
    this.summary = "";
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  render() {
    const { description, instructions } = splitTestSummary(this.summary);
    if (!description && !instructions) return nothing;

    return html`
      ${description
        ? html`<cts-alert variant="info">
            <div class="summaryZone summaryZone--about" data-testid="about-test-zone">
              <span class="summaryEyebrow">About this test</span>
              <div class="summaryBody">${description}</div>
            </div>
          </cts-alert>`
        : nothing}
      ${instructions
        ? html`<cts-alert variant="warning">
            <div class="summaryZone summaryZone--instructions" data-testid="user-instructions-zone">
              <span class="summaryEyebrow">What you need to do</span>
              <div class="summaryBody">${instructions}</div>
            </div>
          </cts-alert>`
        : nothing}
    `;
  }
}

customElements.define("cts-test-summary", CtsTestSummary);

export {};
