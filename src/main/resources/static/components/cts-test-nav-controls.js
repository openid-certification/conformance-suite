import { LitElement, html, nothing, css } from "lit";
import "./cts-button.js";
import "./cts-link-button.js";
import "./cts-plan-status.js";
import { currentModuleIndex } from "../js/module-status.js";

const STYLE_ID = "cts-test-nav-controls-styles";

// Scoped CSS for the test-plan navigation cluster. Plan-level progress is
// delegated to cts-plan-status in `log` mode (one colour-coded segment per
// module + the "you are here" marker + the "Module N of M" label), so this
// widget owns only the group container and the button row. The cluster sits
// inside log-detail.html's existing flex column of header controls, so it
// picks up the surrounding gap without owning margins of its own.
const STYLE_TEXT = css`
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
  /* The progress row holds the segment bar and the Continue button as
     vertically-centred siblings (align-items: center). Keeping the "Module N of
     M" position label OUT of this row (it sits below as .cts-tnc-position) is
     what lets the button centre against the 14px bar rather than the taller
     bar+label block. */
  cts-test-nav-controls .cts-tnc-progress-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-3);
  }
  cts-test-nav-controls .cts-tnc-progress-row cts-plan-status {
    flex: 1 1 200px;
    min-width: 160px;
  }
  cts-test-nav-controls .cts-tnc-position {
    margin: 0;
    font-size: var(--fs-12);
    line-height: var(--lh-snug);
    color: var(--fg-soft);
    font-variant-numeric: tabular-nums;
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
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/**
 * Test-plan navigation cluster — groups Return-to-Plan, Repeat Test,
 * Continue Plan, and a plan-level progress indicator into a single
 * semantically-labelled widget. Lands recommendation R21 (and the
 * associated R20 progress orientation) from the CTS UX brainstorm.
 *
 * Plan progress is delegated to `cts-plan-status` in `log` mode: it renders
 * one colour-coded segment per plan module, the "you are here" marker on the
 * segment whose module ran `currentInstanceId`, and the "Module N of M"
 * label (which it computes from that match). Each reachable segment is a real
 * `<a href>` (the page supplies the URL via `modules[].href`), so navigating to
 * a sibling's log is a native link load — no event round-trip. This replaces the
 * legacy single-bar "Plan progress: Module X of N" track — the segment bar
 * carries both the count orientation and the per-module status at a glance, and
 * disambiguates scope (plan vs current-test) the same way the eyebrow used to
 * (MR 1998 finding A4, thomasdarimont).
 *
 * Light DOM. Scoped CSS is injected once on first render. Render returns
 * `nothing` when `planId` is empty (an ad-hoc test that does not belong
 * to a plan), so the cluster simply disappears for non-plan tests.
 *
 * The Continue button is rendered only when `nextEnabled` is true
 * (mirrors the legacy hide-when-no-next-module behaviour).
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
 * @property {Array<object>} modules - Plan modules in plan order, forwarded
 *   verbatim to the embedded `cts-plan-status` (which colours each segment
 *   via the shared `segmentVariant` helper). Each module's `href` (set by the
 *   page for reachable siblings) makes that segment a navigable link; the page
 *   threads any `?public=true` into the href itself. An empty array renders no
 *   progress bar. Set via JS only (attribute:false).
 * @property {string} currentInstanceId - The instance currently being
 *   viewed; forwarded to `cts-plan-status` so it marks the segment whose
 *   module's `instances` array includes it and derives the "Module N of M"
 *   label. Reflects the `current-instance-id` attribute.
 * @property {boolean} nextEnabled - Whether a next module exists. Drives
 *   visibility of the Continue Plan button.
 * @property {boolean} readonly - When true (public/readonly view), only
 *   Return to Plan is rendered; Repeat and Continue are hidden. Ignored
 *   in `slim` mode where the back link does not exist either. Not forwarded to
 *   the embedded `cts-plan-status`: progress-bar navigation is carried per
 *   segment by `modules[].href`, so a published-plan public view can still
 *   click through to reachable siblings.
 * @property {boolean} publicView - When true, appends `&public=true` to the
 *   Return-to-Plan link so the linked plan-detail page renders its public-share
 *   variant. NOT forwarded to the embedded `cts-plan-status` — the page already
 *   threads `?public=true` into each segment's `href`, so the bar needs no
 *   public flag. Independent of `readonly` because a summary-published test is
 *   readonly but not (necessarily) public. No-op for the link in `slim` mode.
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
 *
 * Sibling navigation no longer flows through an event: the embedded
 * `cts-plan-status` renders each reachable segment as a real `<a href>` (the
 * page supplies the URL), so clicking a segment is a native link load. The
 * widget neither emits nor re-dispatches `cts-plan-status-activate`.
 */
class CtsTestNavControls extends LitElement {
  static properties = {
    testId: { type: String, attribute: "test-id" },
    planId: { type: String, attribute: "plan-id" },
    modules: { type: Array, attribute: false },
    currentInstanceId: { type: String, attribute: "current-instance-id" },
    nextEnabled: { type: Boolean, attribute: "next-enabled" },
    readonly: { type: Boolean },
    publicView: { type: Boolean, attribute: "public-view" },
    slim: { type: Boolean },
  };

  constructor() {
    super();
    this.testId = "";
    this.planId = "";
    this.modules = [];
    this.currentInstanceId = "";
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

  /**
   * The plan-level progress bar (cts-plan-status in `log` mode).
   * @param {{ hideLabel?: boolean }} [opts] - When `hideLabel` is true the
   *   component suppresses its built-in "Module N of M" label so this widget can
   *   render it on its own row (see `_renderPosition`). Used by the slim layout.
   * @returns {import('lit').TemplateResult | typeof nothing} The bar, or
   *   `nothing` when there are no modules.
   */
  _renderProgress({ hideLabel = false } = {}) {
    const modules = Array.isArray(this.modules) ? this.modules : [];
    if (modules.length === 0) return nothing;

    // Plan-level progress is the cts-plan-status bar in `log` mode: one
    // colour-coded segment per module, the "you are here" marker on the
    // viewed instance's module, and the "Module N of M" label (computed by
    // the component from `currentInstanceId`). Navigability is carried per
    // module by `modules[].href` (the page sets it only for reachable
    // siblings), so nothing public-related is forwarded to the bar — a segment
    // with an href is a real link and the browser navigates natively. This
    // widget's own `readonly` still governs the Repeat/Continue actions.
    return html`
      <cts-plan-status
        data-testid="progress"
        mode="log"
        .modules=${modules}
        current-instance-id=${this.currentInstanceId}
        ?hide-label=${hideLabel}
      ></cts-plan-status>
    `;
  }

  /**
   * The "Module N of M" position label, rendered on its own row beneath the
   * bar+button row in the slim layout. Computed from the same shared
   * `currentModuleIndex` the bar uses for its "you are here" marker, so the
   * label and the marker can never point at different modules.
   * @returns {import('lit').TemplateResult | typeof nothing} The label, or
   *   `nothing` when no module matches the viewed instance.
   */
  _renderPosition() {
    const modules = Array.isArray(this.modules) ? this.modules : [];
    const index = currentModuleIndex(modules, this.currentInstanceId);
    if (index < 0) return nothing;
    return html`<p class="cts-tnc-position" data-testid="progress-position">
      Module ${index + 1} of ${modules.length}
    </p>`;
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
      const showProgress = Array.isArray(this.modules) && this.modules.length > 0;
      const showContinue = !this.readonly && this.nextEnabled;
      // Bail out when neither the progress widget nor the Continue
      // button has anything to render. This is the transient state
      // between the page's first paint and the /api/plan fetch
      // resolving (modules stays empty until the bootstrap sets it).
      // Returning `nothing` leaves the host with no children, which
      // lets the page-level `:has(cts-test-nav-controls:empty)`
      // selector hide the wrapping .ctsNavRow so its border-bottom
      // doesn't sit alone under the status bar.
      if (!showProgress && !showContinue) return nothing;
      return html`
        <div class="cts-tnc-group" role="group" aria-label="Test plan navigation">
          <div class="cts-tnc-progress-row">
            ${this._renderProgress({ hideLabel: true })}
            ${showContinue
              ? html`<div class="cts-tnc-buttons"> ${this._renderContinueButton()} </div>`
              : nothing}
          </div>
          ${this._renderPosition()}
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
