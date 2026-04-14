import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";

/**
 * Maps module status/result to badge variant.
 *
 * - null status  -> "pending" (secondary)
 * - RUNNING      -> "info"
 * - FINISHED + PASSED  -> "success"
 * - FINISHED + FAILED  -> "failure"
 * - FINISHED + WARNING -> "warning"
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

function statusLabel(status, result) {
  if (!status) return "PENDING";
  if (status === "RUNNING") return "RUNNING";
  if (status === "FINISHED" && result) return result;
  return status;
}

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

  createRenderRoot() { return this; }

  _formatVariant(variant) {
    if (!variant || typeof variant !== "object") return "";
    return Object.entries(variant)
      .map(([key, value]) => `${key}=${value}`)
      .join(", ");
  }

  _handleRunTest(mod) {
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

  _handleDownloadLog(testId) {
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
              ? html`
                <button
                  class="btn btn-sm btn-light bg-gradient border border-secondary startBtn"
                  data-testid="run-test-btn"
                  @click=${() => this._handleRunTest(mod)}
                ><span class="bi bi-play-fill"></span> Run Test</button>`
              : nothing}
            ${lastInstance
              ? html`
                <a
                  class="btn btn-sm btn-light bg-gradient border border-secondary viewBtn"
                  href="${logHref}"
                ><span class="bi bi-file-earmark"></span> View Logs</a>
                <button
                  class="btn btn-sm btn-light bg-gradient border border-secondary downloadBtn"
                  @click=${() => this._handleDownloadLog(lastInstance)}
                ><span class="bi bi-save2"></span> Download Logs</button>`
              : nothing}
          </div>
        </div>
        <div class="col-md-8">
          <div class="row">
            <div class="col-md-2">Test Name:</div>
            <div class="col-md-10">
              ${mod.testModule}${mod.testSummary
                ? html`<sup><span
                    class="bi bi-question-circle-fill"
                    title="${mod.testSummary}"
                  ></span></sup>`
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
        ${this.modules.map((mod) => this._renderModuleRow(mod))}
      </div>
    `;
  }
}

customElements.define("cts-plan-modules", CtsPlanModules);
