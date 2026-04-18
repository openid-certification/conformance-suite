import { LitElement, html, nothing } from "lit";
import { repeat } from "lit/directives/repeat.js";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-link-button.js";

// Stable identity key for a module entry. Used both as the repeat() key
// and as the data-module-key attribute the Run button reads back —
// keeping them in sync means the click handler resolves to the right
// module regardless of array order.
function _moduleKey(mod) {
  return `${mod.testModule}|${JSON.stringify(mod.variant ?? null)}`;
}

/**
 * Maps module status/result to badge variant.
 *
 * - null status  -> "pending" (secondary)
 * - RUNNING      -> "info"
 * - FINISHED + PASSED  -> "success"
 * - FINISHED + FAILED  -> "failure"
 * - FINISHED + WARNING -> "warning"
 * @param {string|null} status - Module status: null, "RUNNING", or "FINISHED".
 * @param {string|null} result - Module result when status is "FINISHED":
 *   "PASSED", "FAILED", "WARNING", "REVIEW", "SKIPPED", or null.
 * @returns {string} Badge variant name (e.g. "success", "failure",
 *   "warning", "info", "secondary").
 */
function statusBadgeVariant(status, result) {
  if (!status) return "secondary";
  if (status === "RUNNING") return "info";
  if (status === "FINISHED") {
    const map = {
      PASSED: "success",
      FAILED: "failure",
      WARNING: "warning",
      REVIEW: "review",
      SKIPPED: "skipped",
    };
    return map[result] || "secondary";
  }
  return "secondary";
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

/**
 * Per-module rows for a plan-detail page. Each row shows status/result
 * badges, the test module name and variant, the last test instance, and
 * action buttons (Run / View Logs / Download Logs).
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

  _renderModuleRow(mod) {
    const lastInstance = this._getLastInstance(mod);
    const variant = statusBadgeVariant(mod.status, mod.result);
    const label = statusLabel(mod.status, mod.result);
    const variantStr = this._formatVariant(mod.variant);
    const logHref = lastInstance
      ? `log-detail.html?log=${encodeURIComponent(lastInstance)}${this.isPublic ? "&public=true" : ""}`
      : null;

    return html`
      <div class="row logItem" data-instance-id="${lastInstance || ""}">
        <div class="col-md-2 testStatusAndResult">
          <cts-badge variant="${variant}" label="${label}"></cts-badge>
        </div>
        <div class="col-md-2">
          <div class="d-grid gap-1">
            ${this._canRunTest()
              ? html` <cts-button
                  class="startBtn"
                  data-testid="run-test-btn"
                  data-module-key="${_moduleKey(mod)}"
                  variant="light"
                  icon="play-fill"
                  label="Run Test"
                  full-width
                  @cts-click=${this._handleRunTest}
                ></cts-button>`
              : nothing}
            ${lastInstance
              ? html` <cts-link-button
                    class="viewBtn"
                    href="${logHref}"
                    variant="light"
                    icon="file-earmark"
                    label="View Logs"
                    full-width
                  ></cts-link-button>
                  <cts-button
                    class="downloadBtn"
                    data-instance-id="${lastInstance}"
                    variant="light"
                    icon="save2"
                    label="Download Logs"
                    full-width
                    @cts-click=${this._handleDownloadLog}
                  ></cts-button>`
              : nothing}
          </div>
        </div>
        <div class="col-md-8">
          <div class="row">
            <div class="col-md-2">Test Name:</div>
            <div class="col-md-10">
              ${mod.testModule}${mod.testSummary
                ? html`<sup
                    ><span class="bi bi-question-circle-fill" title="${mod.testSummary}"></span
                  ></sup>`
                : nothing}
            </div>
          </div>
          <div class="row">
            <div class="col-md-2">Variant:</div>
            <div class="col-md-10">${variantStr}</div>
          </div>
          <div class="row">
            <div class="col-md-2">Test ID:</div>
            <div class="col-md-10">${lastInstance || "NONE"}</div>
          </div>
        </div>
      </div>
    `;
  }

  render() {
    if (!this.modules || this.modules.length === 0) {
      return html`<div class="text-muted text-center p-3">No modules in this plan</div>`;
    }

    return html`
      <div class="container-fluid" id="planItems">
        ${repeat(this.modules, _moduleKey, (mod) => this._renderModuleRow(mod))}
      </div>
    `;
  }
}

customElements.define("cts-plan-modules", CtsPlanModules);
