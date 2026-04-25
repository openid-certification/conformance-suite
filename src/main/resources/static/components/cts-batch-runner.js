import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";
import "./cts-button.js";

/**
 * Maps a module result string to a canonical cts-badge variant from the
 * OIDF status palette. PENDING is treated as the neutral "skip" tone
 * (the design archive's Skipped palette doubles as the not-yet-run
 * surface), RUNNING uses the spinner-bearing "running" variant, and the
 * terminal results map onto the matching pass/fail/warn/review/skip
 * tones. Unknown results fall through to "skip" so the badge still
 * renders something defined.
 * @type {Object.<string, string>}
 */
const RESULT_BADGE_VARIANTS = {
  PENDING: "skip",
  RUNNING: "running",
  PASSED: "pass",
  FAILED: "fail",
  WARNING: "warn",
  REVIEW: "review",
  SKIPPED: "skip",
};

const STYLE_ID = "cts-batch-runner-styles";

// Scoped, token-driven CSS. Replaces the previous Bootstrap utility soup
// (`row g-2`, `col-md-4 col-lg-3`, `card`/`card-body`, `d-flex gap-2 mb-3
// align-items-center`, `text-muted`) with a self-contained grid + tile
// surface that draws color, spacing, type, and radius from oidf-tokens.css.
const STYLE_TEXT = `
cts-batch-runner .oidf-batch-runner {
  display: block;
}
cts-batch-runner .oidf-batch-runner-bar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}
cts-batch-runner .oidf-batch-runner-progress {
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  color: var(--ink-700);
}
cts-batch-runner .oidf-batch-runner-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-2);
}
cts-batch-runner .oidf-batch-runner-tile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  background: var(--bg-elev);
  border: 1px solid var(--border);
  border-radius: var(--radius-2);
  box-shadow: var(--shadow-1);
  min-width: 0;
}
cts-batch-runner .oidf-batch-runner-name {
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  color: var(--ink-900);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
  flex: 1 1 auto;
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
 * "Run All" / "Run Remaining" controls and a grid of module status badges
 * for a plan. Does not run the tests itself; emits events for the host page
 * to act on.
 *
 * Action buttons compose `cts-button` (primary for Run All, secondary for
 * Run Remaining). Status indicators use `cts-badge` with the canonical
 * OIDF status palette (`pass` / `fail` / `warn` / `running` / `skip` /
 * `review`), mapped via the `RESULT_BADGE_VARIANTS` lookup. Layout and
 * tile surfaces are styled with scoped, token-driven CSS instead of the
 * previous Bootstrap `row`/`col-*`/`card` markup.
 *
 * @property {string} planId - Plan id the batch applies to. Reflects the
 *   `plan-id` attribute.
 * @property {Array} modules - Plan modules; each has `testModule` and an
 *   optional `instances` array used to derive pass/fail status.
 * @fires cts-run-all - When the Run All button is clicked; bubbles.
 * @fires cts-run-remaining - When the Run Remaining button is clicked;
 *   bubbles.
 */
class CtsBatchRunner extends LitElement {
  static properties = {
    planId: { type: String, attribute: "plan-id" },
    modules: { type: Array },
    _running: { state: true },
  };

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  constructor() {
    super();
    this.planId = "";
    this.modules = [];
    this._running = false;
  }

  get _completedCount() {
    return this.modules.filter((m) => m.instances?.length > 0).length;
  }
  get _hasRemaining() {
    return this.modules.some((m) => !m.instances?.length);
  }

  _handleRunAll() {
    this.dispatchEvent(new CustomEvent("cts-run-all", { bubbles: true }));
  }
  _handleRunRemaining() {
    this.dispatchEvent(new CustomEvent("cts-run-remaining", { bubbles: true }));
  }

  _moduleResult(module) {
    if (!module.instances?.length) return "PENDING";
    const lastInstance = module.instances[module.instances.length - 1];
    return lastInstance.result || "RUNNING";
  }

  _moduleVariant(module) {
    const result = this._moduleResult(module);
    return RESULT_BADGE_VARIANTS[result] || "skip";
  }

  render() {
    return html`
      <div class="oidf-batch-runner">
        <div class="oidf-batch-runner-bar">
          <cts-button
            variant="primary"
            size="sm"
            icon="play-fill"
            label="Run All"
            ?disabled=${this._running}
            @cts-click=${this._handleRunAll}
          ></cts-button>
          ${this._hasRemaining
            ? html`<cts-button
                variant="secondary"
                size="sm"
                icon="play-fill"
                label="Run Remaining"
                ?disabled=${this._running}
                @cts-click=${this._handleRunRemaining}
              ></cts-button>`
            : nothing}
          ${this._running
            ? html`<span class="oidf-batch-runner-progress"
                >${this._completedCount} of ${this.modules.length}</span
              >`
            : nothing}
        </div>
        <div class="oidf-batch-runner-grid">${this._renderModuleTiles()}</div>
      </div>
    `;
  }

  _renderModuleTiles() {
    return this.modules.map(
      (mod) => html`
        <div class="oidf-batch-runner-tile">
          <span class="oidf-batch-runner-name" title="${mod.testModule}">${mod.testModule}</span>
          <cts-badge
            variant="${this._moduleVariant(mod)}"
            label="${this._moduleResult(mod)}"
          ></cts-badge>
        </div>
      `,
    );
  }
}
customElements.define("cts-batch-runner", CtsBatchRunner);

export {};
