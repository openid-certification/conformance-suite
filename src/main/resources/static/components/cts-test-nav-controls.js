import { LitElement, html, nothing } from "lit";
import "./cts-button.js";
import "./cts-link-button.js";

const STYLE_ID = "cts-test-nav-controls-styles";

// Scoped CSS for the test-plan navigation cluster. Mirrors the
// cts-running-test-card progress treatment (8px ink-100 track with an
// orange-400 fill and steady transition — no striped/pulsing animation)
// per project/preview/components-progress.html in the OIDF design archive.
// The cluster sits inside log-detail.html's existing flex column of
// header controls, so it picks up the surrounding gap without owning
// margins of its own.
const STYLE_TEXT = `
  cts-test-nav-controls {
    display: block;
  }
  cts-test-nav-controls .cts-tnc-group {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    padding: var(--space-3);
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
  }
  cts-test-nav-controls .cts-tnc-progress {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }
  cts-test-nav-controls .cts-tnc-progress-label {
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    line-height: var(--lh-snug);
    color: var(--fg-soft);
    font-weight: var(--fw-bold);
  }
  cts-test-nav-controls .cts-tnc-progress-track {
    height: 8px;
    background: var(--ink-100);
    border-radius: var(--radius-pill);
    overflow: hidden;
  }
  cts-test-nav-controls .cts-tnc-progress-fill {
    height: 100%;
    background: var(--orange-400);
    border-radius: var(--radius-pill);
    transition: width var(--dur-3) var(--ease-standard);
  }
  cts-test-nav-controls .cts-tnc-buttons {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }
  cts-test-nav-controls .cts-tnc-buttons cts-button,
  cts-test-nav-controls .cts-tnc-buttons cts-link-button {
    width: 100%;
  }
`;

/** Inject the widget's scoped CSS into <head> exactly once. */
function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Test-plan navigation cluster — groups Return-to-Plan, Repeat Test,
 * Continue Plan, and a "Module X of N" progress indicator into a single
 * semantically-labelled widget. Lands recommendation R21 (and the
 * associated R20 progress count) from the CTS UX brainstorm.
 *
 * Light DOM. Scoped CSS is injected once on first render. Render returns
 * `nothing` when `planId` is empty (an ad-hoc test that does not belong
 * to a plan), so the cluster simply disappears for non-plan tests.
 *
 * The Continue button is rendered only when `nextEnabled` is true
 * (mirrors the legacy hide-when-no-next-module behaviour). The progress
 * block keeps rendering and reads "Module N of N" so the user has clear
 * end-of-plan feedback.
 *
 * Return-to-Plan is a real `<a href>` (via cts-link-button) so middle-
 * click and Cmd-click open the plan in a new tab. The widget does not
 * fire a synthetic event on back-link activation — `cts-link-button`
 * is a thin <a> wrapper without a `cts-click` event, and intercepting
 * native link navigation would defeat the middle-click / Cmd-click
 * affordance.
 *
 * @property {string} testId - Test instance ID; included in event details.
 * @property {string} planId - Parent plan ID. When falsy the widget
 *   renders nothing.
 * @property {number} currentIndex - Zero-based index of the current
 *   module within the plan (e.g. 5 for the 6th module).
 * @property {number} totalCount - Total number of modules in the plan.
 * @property {boolean} nextEnabled - Whether a next module exists. Drives
 *   visibility of the Continue Plan button.
 * @property {boolean} readonly - When true (public/readonly view), only
 *   Return to Plan is rendered; Repeat and Continue are hidden. Ignored
 *   in `slim` mode where the back link does not exist either.
 * @property {boolean} publicView - When true, appends `&public=true` to
 *   the Return-to-Plan link so the linked plan-detail page renders its
 *   public-share variant. Independent of `readonly` because a
 *   summary-published test is readonly but not (necessarily) public.
 *   No-op in `slim` mode.
 * @property {boolean} slim - When true, the widget renders only the
 *   progress indicator and (when applicable) the Continue Plan CTA.
 *   Used by the log-detail page where the page-level breadcrumb
 *   already carries the back affordance and the sticky status bar's
 *   primary action already carries Repeat — emitting them here too
 *   would duplicate two prominent affordances inside one viewport.
 *   The default (non-slim) form renders the full Return / Repeat /
 *   Continue cluster and is reserved for surfaces with no breadcrumb
 *   and no status-bar Repeat to inherit from; Storybook stories use it
 *   to document the widget's complete contract.
 * @fires cts-repeat - When Repeat Test is clicked. Detail:
 *   `{ testId, planId }`. Bubbles. Not fired in `slim` mode (no button).
 * @fires cts-continue - When Continue Plan is clicked. Detail:
 *   `{ testId, planId }`. Bubbles.
 */
class CtsTestNavControls extends LitElement {
  static properties = {
    testId: { type: String, attribute: "test-id" },
    planId: { type: String, attribute: "plan-id" },
    currentIndex: { type: Number, attribute: "current-index" },
    totalCount: { type: Number, attribute: "total-count" },
    nextEnabled: { type: Boolean, attribute: "next-enabled" },
    readonly: { type: Boolean },
    publicView: { type: Boolean, attribute: "public-view" },
    slim: { type: Boolean },
  };

  constructor() {
    super();
    this.testId = "";
    this.planId = "";
    this.currentIndex = 0;
    this.totalCount = 0;
    this.nextEnabled = false;
    this.readonly = false;
    this.publicView = false;
    this.slim = false;
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  _detail() {
    return { testId: this.testId, planId: this.planId };
  }

  _handleRepeat() {
    this.dispatchEvent(
      new CustomEvent("cts-repeat", {
        bubbles: true,
        detail: this._detail(),
      }),
    );
  }

  _handleContinue() {
    this.dispatchEvent(
      new CustomEvent("cts-continue", {
        bubbles: true,
        detail: this._detail(),
      }),
    );
  }

  _renderProgress() {
    const total = Math.max(0, Number(this.totalCount) || 0);
    if (total <= 0) return nothing;

    // 1-based for the user-facing label; clamp the displayed position so
    // an out-of-range currentIndex (e.g. results haven't loaded yet)
    // still produces a sensible "Module N of N" string.
    const rawCurrent = Math.max(0, Number(this.currentIndex) || 0);
    const displayPosition = Math.min(rawCurrent + 1, total);
    const fillPercent = (displayPosition / total) * 100;

    return html`
      <div class="cts-tnc-progress" data-testid="progress">
        <div class="cts-tnc-progress-label" data-testid="progress-label">
          Module ${displayPosition} of ${total}
        </div>
        <div
          class="cts-tnc-progress-track"
          role="progressbar"
          aria-valuenow="${displayPosition}"
          aria-valuemin="1"
          aria-valuemax="${total}"
          aria-label="Test plan progress"
        >
          <div class="cts-tnc-progress-fill" style="width: ${fillPercent}%;"></div>
        </div>
      </div>
    `;
  }

  _renderBackLink() {
    const baseHref = `plan-detail.html?plan=${encodeURIComponent(this.planId)}`;
    const href = this.publicView ? `${baseHref}&public=true` : baseHref;
    return html`
      <cts-link-button
        variant="secondary"
        size="sm"
        icon="bookmark"
        label="Return to Plan"
        href="${href}"
        data-testid="back-btn"
      ></cts-link-button>
    `;
  }

  _renderRepeatButton() {
    return html`
      <cts-button
        variant="secondary"
        size="sm"
        icon="arrows-reload-01"
        label="Repeat Test"
        data-testid="repeat-btn"
        @cts-click=${this._handleRepeat}
      ></cts-button>
    `;
  }

  _renderContinueButton() {
    return html`
      <cts-button
        variant="primary"
        size="sm"
        icon="skip-forward"
        label="Continue Plan"
        title="Run the next test in this test plan"
        data-testid="continue-btn"
        @cts-click=${this._handleContinue}
      ></cts-button>
    `;
  }

  render() {
    if (!this.planId) return nothing;

    // Slim mode (used by log-detail): the page-level breadcrumb
    // already carries the back affordance and cts-log-detail-header's
    // sticky status bar primary already carries the Repeat action, so
    // we render only progress + (optional) Continue. The legacy page
    // uses slim=false so it keeps the full Return / Repeat / Continue
    // cluster as its own primary affordance home.
    if (this.slim) {
      const showProgress = (Number(this.totalCount) || 0) > 0;
      const showContinue = !this.readonly && this.nextEnabled;
      // Bail out when neither the progress widget nor the Continue
      // button has anything to render. This is the transient state
      // between the page's first paint and the /api/plan fetch
      // resolving (totalCount stays at the default 0 until the
      // bootstrap sets it). Returning `nothing` leaves the host with
      // no children, which lets the page-level
      // `:has(cts-test-nav-controls:empty)` selector hide the
      // wrapping .ctsNavRow so its border-bottom doesn't sit alone
      // under the status bar.
      if (!showProgress && !showContinue) return nothing;
      return html`
        <div class="cts-tnc-group" role="group" aria-label="Test plan navigation">
          ${this._renderProgress()}
          ${showContinue
            ? html`<div class="cts-tnc-buttons"> ${this._renderContinueButton()} </div>`
            : nothing}
        </div>
      `;
    }

    return html`
      <div class="cts-tnc-group" role="group" aria-label="Test plan navigation">
        ${this._renderProgress()}
        <div class="cts-tnc-buttons">
          ${this._renderBackLink()} ${this.readonly ? nothing : this._renderRepeatButton()}
          ${!this.readonly && this.nextEnabled ? this._renderContinueButton() : nothing}
        </div>
      </div>
    `;
  }
}

customElements.define("cts-test-nav-controls", CtsTestNavControls);

export {};
