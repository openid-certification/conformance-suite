import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-link-button.js";

/**
 * Stable identity key for a module entry. Used as the data-module-key
 * attribute the Run button reads back so the click handler resolves to
 * the right module regardless of array order.
 * @param {object} mod - Plan module with `testModule` and optional `variant`.
 * @returns {string} Content-derived key (testModule plus serialized variant).
 */
function _moduleKey(mod) {
  return `${mod.testModule}|${JSON.stringify(mod.variant ?? null)}`;
}

/**
 * Maps module status/result to a canonical cts-badge variant.
 *
 * - null status        -> "skip" (PENDING — neutral until run)
 * - RUNNING            -> "running"
 * - FINISHED + PASSED  -> "pass"
 * - FINISHED + FAILED  -> "fail"
 * - FINISHED + WARNING -> "warn"
 * - FINISHED + REVIEW  -> "review"
 * - FINISHED + SKIPPED -> "skip"
 * @param {string|null} status - Module status: null, "RUNNING", or "FINISHED".
 * @param {string|null} result - Module result when status is "FINISHED":
 *   "PASSED", "FAILED", "WARNING", "REVIEW", "SKIPPED", or null.
 * @returns {string} Canonical cts-badge variant.
 */
function statusBadgeVariant(status, result) {
  if (!status) return "skip";
  if (status === "RUNNING") return "running";
  if (status === "FINISHED") {
    const map = {
      PASSED: "pass",
      FAILED: "fail",
      WARNING: "warn",
      REVIEW: "review",
      SKIPPED: "skip",
    };
    return map[result] || "skip";
  }
  return "skip";
}

/**
 * Maps module status/result to a human-readable badge label.
 * @param {string|null} status - Module status: null, "RUNNING", or "FINISHED".
 * @param {string|null} result - Module result when status is "FINISHED".
 * @returns {string} Display label (e.g. "PENDING", "RUNNING", "PASSED").
 */
function statusLabel(status, result) {
  if (!status) return "PENDING";
  if (status === "RUNNING") return "RUNNING";
  if (status === "FINISHED" && result) return result;
  return status;
}

const STYLE_ID = "cts-plan-modules-styles";

// Scoped CSS for the plan-modules list. Adopts the design archive's
// `.module-row` pattern (`project/ui_kits/certification-suite/app.css`):
// 4-column grid `28px 1fr auto auto` with a mono row number, name + meta
// stack, status badge, and right-side action stack. Mono number/duration
// styling uses `--font-mono` per the archive.
const STYLE_TEXT = `
  cts-plan-modules {
    display: block;
  }
  cts-plan-modules .planModulesCard {
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    overflow: hidden;
  }
  cts-plan-modules .module-row {
    display: grid;
    grid-template-columns: 28px 1fr auto auto;
    gap: var(--space-3);
    padding: var(--space-3) var(--space-4);
    border-bottom: 1px solid var(--ink-100);
    align-items: center;
  }
  cts-plan-modules .module-row:last-child {
    border-bottom: 0;
  }
  cts-plan-modules .module-row .num {
    font-family: var(--font-mono);
    font-size: 11px;
    color: var(--fg-soft);
  }
  cts-plan-modules .module-row .name {
    font-weight: var(--fw-bold);
    font-size: var(--fs-13);
    color: var(--fg);
    word-break: break-word;
  }
  cts-plan-modules .module-row .name .desc {
    color: var(--fg-soft);
    font-weight: var(--fw-regular);
    font-size: var(--fs-12);
    margin-top: 1px;
  }
  cts-plan-modules .module-row .name .desc .mono {
    font-family: var(--font-mono);
  }
  cts-plan-modules .module-row .name .help-icon {
    color: var(--fg-faint);
    margin-left: var(--space-1);
    font-size: var(--fs-12);
    vertical-align: super;
  }
  cts-plan-modules .module-row .actionStack {
    display: grid;
    grid-auto-flow: column;
    gap: var(--space-1);
  }
  cts-plan-modules .planModulesEmpty {
    padding: var(--space-5);
    text-align: center;
    color: var(--fg-soft);
  }
  /* R28: status badges wrap in an anchor to log-detail when an instance
     exists, so a click on the lozenge takes the user to the test's logs.
     The anchor matches the badge's pill silhouette and inherits color so
     the badge's own foreground stays in charge of its text. */
  cts-plan-modules .moduleStatusLink {
    display: inline-flex;
    text-decoration: none;
    color: inherit;
    border-radius: var(--radius-pill);
  }
  cts-plan-modules .moduleStatusLink:focus-visible {
    outline: 2px solid var(--orange-400);
    outline-offset: 2px;
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
 * Per-module rows for a plan-detail page. Each row shows status/result
 * badges, the test module name and variant, the last test instance, and
 * action buttons (Run / View Logs / Download Logs).
 *
 * Light DOM. Scoped CSS is injected once on first connect; rows adopt the
 * design archive's `.module-row` 4-column grid (`28px 1fr auto auto`).
 *
 * @property {Array<object>} modules - Modules rendered from the plan-detail
 *   API response; see cts-plan-detail.stories.js for shape.
 * @property {string} planId - Parent plan ID. Reflects the `plan-id`
 *   attribute.
 * @property {boolean} isReadonly - Hides the Run Test button. Reflects the
 *   `is-readonly` attribute.
 * @property {boolean} isImmutable - Hides the Run Test button on immutable
 *   plans. Reflects the `is-immutable` attribute.
 * @property {boolean} isPublic - Appends `&public=true` to log-detail links.
 *   Reflects the `is-public` attribute.
 * The status badge is rendered as a real `<a>` link to `log-detail.html`
 * when a test instance exists (R28), so clicking the lozenge takes the
 * user to that test's log page. Modules with no instance render the
 * badge unwrapped.
 *
 * @fires cts-run-test - When the Run Test button is clicked, with
 *   `{ detail: { testModule, variant } }`; bubbles.
 * @fires cts-download-log - When the Download Logs button is clicked, with
 *   `{ detail: { testId } }`; bubbles.
 */
class CtsPlanModules extends LitElement {
  static properties = {
    modules: { type: Array },
    planId: { type: String, attribute: "plan-id" },
    isReadonly: { type: Boolean, attribute: "is-readonly" },
    isImmutable: { type: Boolean, attribute: "is-immutable" },
    isPublic: { type: Boolean, attribute: "is-public" },
  };

  constructor() {
    super();
    this.modules = [];
    this.planId = "";
    this.isReadonly = false;
    this.isImmutable = false;
    this.isPublic = false;
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  _formatVariant(variant) {
    if (!variant || typeof variant !== "object") return "";
    return Object.entries(variant)
      .map(([key, value]) => `${key}=${value}`)
      .join(", ");
  }

  _handleRunTest(e) {
    const key = e.currentTarget.dataset.moduleKey;
    const mod = this.modules?.find((m) => _moduleKey(m) === key);
    if (!mod) return;
    this.dispatchEvent(
      new CustomEvent("cts-run-test", {
        bubbles: true,
        detail: {
          testModule: mod.testModule,
          variant: mod.variant,
        },
      }),
    );
  }

  _handleDownloadLog(e) {
    const testId = e.currentTarget.dataset.instanceId;
    if (!testId) return;
    this.dispatchEvent(
      new CustomEvent("cts-download-log", {
        bubbles: true,
        detail: { testId },
      }),
    );
  }

  _getLastInstance(mod) {
    if (!mod.instances || mod.instances.length === 0) return null;
    return mod.instances[mod.instances.length - 1];
  }

  _canRunTest() {
    return !this.isReadonly && !this.isImmutable;
  }

  /**
   * Format a 1-based row number as a zero-padded string (matches the design
   * archive's "01" / "02" mono row numbers).
   * @param {number} index - 1-based row index.
   * @returns {string} 2-digit zero-padded number (e.g. "01"); falls back to
   *   the raw string when the count exceeds 2 digits.
   */
  _rowNumber(index) {
    return String(index).padStart(2, "0");
  }

  _renderModuleRow(mod, index) {
    const lastInstance = this._getLastInstance(mod);
    const variant = statusBadgeVariant(mod.status, mod.result);
    const label = statusLabel(mod.status, mod.result);
    const variantStr = this._formatVariant(mod.variant);
    const logHref = lastInstance
      ? `log-detail.html?log=${encodeURIComponent(lastInstance)}${this.isPublic ? "&public=true" : ""}`
      : null;

    const badge = html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`;
    const linkedBadge = lastInstance
      ? html`<a
          class="moduleStatusLink"
          data-testid="module-status-link"
          href="${logHref}"
          aria-label="View logs for ${mod.testModule} (${label})"
          >${badge}</a
        >`
      : badge;

    return html`
      <div class="module-row" data-instance-id="${lastInstance || ""}">
        <span class="num">${this._rowNumber(index + 1)}</span>
        <div class="name">
          ${mod.testModule}${mod.testSummary
            ? html`<span
                class="bi bi-question-circle-fill help-icon"
                title="${mod.testSummary}"
                aria-hidden="true"
              ></span>`
            : nothing}
          <div class="desc">
            <span class="mono">${variantStr}</span>
            ${variantStr ? html` · ` : nothing}Test ID:
            <span class="mono">${lastInstance || "NONE"}</span>
          </div>
        </div>
        ${linkedBadge}
        <div class="actionStack">
          ${this._canRunTest()
            ? html`<cts-button
                class="startBtn"
                data-testid="run-test-btn"
                data-module-key="${_moduleKey(mod)}"
                variant="primary"
                size="sm"
                icon="play-fill"
                label="Run Test"
                @cts-click=${this._handleRunTest}
              ></cts-button>`
            : nothing}
          ${lastInstance
            ? html`<cts-link-button
                  class="viewBtn"
                  href="${logHref}"
                  variant="secondary"
                  size="sm"
                  icon="file-earmark"
                  label="View Logs"
                ></cts-link-button>
                <cts-button
                  class="downloadBtn"
                  data-instance-id="${lastInstance}"
                  variant="ghost"
                  size="sm"
                  icon="save2"
                  label="Download Logs"
                  @cts-click=${this._handleDownloadLog}
                ></cts-button>`
            : nothing}
        </div>
      </div>
    `;
  }

  render() {
    if (!this.modules || this.modules.length === 0) {
      return html`<div class="planModulesEmpty">No modules in this plan</div>`;
    }

    return html`<div class="planModulesCard" id="planItems">${this._renderModuleRows()}</div>`;
  }

  _renderModuleRows() {
    return this.modules.map((mod, idx) => this._renderModuleRow(mod, idx));
  }
}

customElements.define("cts-plan-modules", CtsPlanModules);

export {};
