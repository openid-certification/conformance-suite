import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";

/**
 * "Run All" / "Run Remaining" controls and a grid of module status badges
 * for a plan. Does not run the tests itself; emits events for the host page
 * to act on.
 *
 * @property {string} planId - Plan id the batch applies to. Reflects the
 *   `plan-id` attribute.
 * @property {Array} modules - Plan modules; each has `testModule` and an
 *   optional `instances` array used to derive pass/fail status.
 *
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

  createRenderRoot() { return this; }

  constructor() {
    super();
    this.planId = "";
    this.modules = [];
    this._running = false;
  }

  get _completedCount() { return this.modules.filter((m) => m.instances?.length > 0).length; }
  get _hasRemaining() { return this.modules.some((m) => !m.instances?.length); }

  _handleRunAll() { this.dispatchEvent(new CustomEvent("cts-run-all", { bubbles: true })); }
  _handleRunRemaining() { this.dispatchEvent(new CustomEvent("cts-run-remaining", { bubbles: true })); }

  _moduleResult(module) {
    if (!module.instances?.length) return "PENDING";
    const lastInstance = module.instances[module.instances.length - 1];
    return lastInstance.result || "RUNNING";
  }

  _moduleVariant(module) {
    const result = this._moduleResult(module);
    if (result === "PENDING") return "secondary";
    if (result === "RUNNING") return "info";
    return result.toLowerCase();
  }

  render() {
    return html`
      <div>
        <div class="d-flex gap-2 mb-3 align-items-center">
          <button class="btn btn-sm btn-info bg-gradient border border-secondary"
            @click=${this._handleRunAll} ?disabled=${this._running}>
            <span class="bi bi-play-fill" aria-hidden="true"></span> Run All
          </button>
          ${this._hasRemaining ? html`
            <button class="btn btn-sm btn-light bg-gradient border border-secondary"
              @click=${this._handleRunRemaining} ?disabled=${this._running}>
              <span class="bi bi-play-fill" aria-hidden="true"></span> Run Remaining
            </button>
          ` : nothing}
          ${this._running ? html`<span class="text-muted">${this._completedCount} of ${this.modules.length}</span>` : nothing}
        </div>
        <div class="row g-2">
          ${this.modules.map((mod) => html`
            <div class="col-md-4 col-lg-3">
              <div class="card">
                <div class="card-body p-2">
                  <div class="d-flex justify-content-between align-items-center">
                    <small class="text-truncate" title="${mod.testModule}">${mod.testModule}</small>
                    <cts-badge variant="${this._moduleVariant(mod)}" label="${this._moduleResult(mod)}"></cts-badge>
                  </div>
                </div>
              </div>
            </div>
          `)}
        </div>
      </div>
    `;
  }
}
customElements.define("cts-batch-runner", CtsBatchRunner);
