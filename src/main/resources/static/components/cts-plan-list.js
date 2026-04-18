import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";
import "./cts-modal.js";

const RESULT_BADGE_VARIANTS = {
  PASSED: "success",
  FAILED: "failure",
  WARNING: "warning",
  REVIEW: "review",
  SKIPPED: "skipped",
  INTERRUPTED: "interrupted",
};

/**
 * Searchable table of test plans. Fetches from `/api/plan` (or
 * `/api/plan?public=true`) and renders rows with name, variant, module
 * badges, and a config viewer modal.
 * @property {boolean} isAdmin - Adds the Owner column when true. Reflects the
 *   `is-admin` attribute.
 * @property {boolean} isPublic - Fetches the published plan listing and hides
 *   admin affordances. Reflects the `is-public` attribute.
 * @fires cts-plan-navigate - When a plan name is clicked, with
 *   `{ detail: { planId } }`; bubbles and is composed.
 */
class CtsPlanList extends LitElement {
  static properties = {
    isAdmin: { type: Boolean, attribute: "is-admin" },
    isPublic: { type: Boolean, attribute: "is-public" },
    _plans: { state: true },
    _loading: { state: true },
    _error: { state: true },
    _searchQuery: { state: true },
    _selectedConfig: { state: true },
    _selectedPlanId: { state: true },
  };

  createRenderRoot() {
    return this;
  }

  constructor() {
    super();
    this.isAdmin = false;
    this.isPublic = false;
    this._plans = [];
    this._loading = true;
    this._error = null;
    this._searchQuery = "";
    this._selectedConfig = null;
    this._selectedPlanId = "";
  }

  connectedCallback() {
    super.connectedCallback();
    this._fetchPlans();
  }

  async _fetchPlans() {
    this._loading = true;
    this._error = null;
    try {
      const url = this.isPublic ? "/api/plan?public=true" : "/api/plan";
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Failed to load test plans (HTTP ${response.status})`);
      }
      this._plans = await response.json();
    } catch (err) {
      this._error = err instanceof Error ? err.message : String(err);
      this._plans = [];
    } finally {
      this._loading = false;
    }
  }

  _handleSearchInput(e) {
    this._searchQuery = e.target.value;
  }

  _handlePlanClick(planId) {
    this.dispatchEvent(
      new CustomEvent("cts-plan-navigate", {
        bubbles: true,
        composed: true,
        detail: { planId },
      }),
    );
  }

  _handlePlanLinkClick(event) {
    event.preventDefault();
    const planId = event.currentTarget.dataset.planId;
    this._handlePlanClick(planId);
  }

  _handleConfigClick(plan) {
    this._selectedConfig = plan.config;
    this._selectedPlanId = plan._id;
    this.updateComplete.then(() => {
      const modal = /** @type {HTMLElement & { show?: () => void }} */ (
        this.querySelector("#planConfigModal")
      );
      if (modal && typeof modal.show === "function") modal.show();
    });
  }

  _handleConfigButtonClick(event) {
    const planId = event.currentTarget.dataset.planId;
    const plan = this._plans.find((p) => p._id === planId);
    if (plan) this._handleConfigClick(plan);
  }

  async _handleCopyConfig() {
    if (this._selectedConfig) {
      await navigator.clipboard.writeText(JSON.stringify(this._selectedConfig, null, 4));
    }
  }

  _formatVariant(variant) {
    if (!variant) return "";
    if (typeof variant === "string") return variant;
    return Object.entries(variant)
      .map(([key, value]) => `${key}=${value}`)
      .join(", ");
  }

  _formatDate(dateString) {
    if (!dateString) return "";
    const date = new Date(dateString);
    return date.toLocaleString();
  }

  _filteredPlans() {
    if (!this._searchQuery.trim()) return this._plans;
    const query = this._searchQuery.toLowerCase();
    return this._plans.filter((plan) => {
      const nameMatch = (plan.planName || "").toLowerCase().includes(query);
      const descMatch = (plan.description || "").toLowerCase().includes(query);
      return nameMatch || descMatch;
    });
  }

  _renderModuleBadges(modules) {
    if (!modules || modules.length === 0) return nothing;
    return html` <div class="d-flex gap-1 flex-wrap">${this._moduleBadgeList(modules)}</div> `;
  }

  _moduleBadgeList(modules) {
    return modules.map((mod) => {
      if (mod.result) {
        const variant = RESULT_BADGE_VARIANTS[mod.result] || "info";
        return html`<cts-badge
          variant="${variant}"
          label="${mod.testModule}"
          title="${mod.testModule}: ${mod.result}"
        ></cts-badge>`;
      }
      return html`<cts-badge
        variant="secondary"
        label="${mod.testModule}"
        title="${mod.testModule}: not started"
      ></cts-badge>`;
    });
  }

  _renderRows(plans) {
    return plans.map((plan) => this._renderRow(plan));
  }

  _renderTable(plans) {
    return html`
      <table class="table table-striped table-bordered table-hover">
        <thead>
          <tr>
            <th>Plan Name</th>
            <th>Variant</th>
            <th>Description</th>
            <th>Started</th>
            <th>Modules</th>
            <th>Config</th>
            ${this.isAdmin ? html`<th>Owner</th>` : nothing}
          </tr>
        </thead>
        <tbody>${this._renderRows(plans)}</tbody>
      </table>
    `;
  }

  _renderRow(plan) {
    const ownerDisplay = plan.owner ? plan.owner.sub || "unknown" : "";
    return html`
      <tr>
        <td>
          <a
            href="#"
            class="plan-name-link"
            data-plan-id="${plan._id}"
            @click=${this._handlePlanLinkClick}
            >${plan.planName}</a
          >
        </td>
        <td>${this._formatVariant(plan.variant)}</td>
        <td>${plan.description || ""}</td>
        <td>${this._formatDate(plan.started)}</td>
        <td>${this._renderModuleBadges(plan.modules)}</td>
        <td>
          <button
            class="btn btn-sm btn-outline-secondary showConfigBtn"
            title="View configuration"
            data-plan-id="${plan._id}"
            @click=${this._handleConfigButtonClick}
          >
            <span class="bi bi-gear" aria-hidden="true"></span>
          </button>
        </td>
        ${this.isAdmin ? html`<td class="owner-cell">${ownerDisplay}</td>` : nothing}
      </tr>
    `;
  }

  _renderConfigModal() {
    const configJson = this._selectedConfig ? JSON.stringify(this._selectedConfig, null, 4) : "";
    return html`
      <cts-modal id="planConfigModal" heading="Configuration">
        <div>
          <button
            class="btn btn-sm btn-outline-secondary me-2 copy-config-btn"
            title="Copy config to clipboard"
            @click=${this._handleCopyConfig}
          >
            <span class="bi bi-clipboard" aria-hidden="true"></span> Copy
          </button>
          Configuration for <code class="text-muted">${this._selectedPlanId}</code>
        </div>
        <div class="wrapLongStrings mt-2">
          <pre class="row-bg-light p-1 config-json">${configJson}</pre>
        </div>
      </cts-modal>
    `;
  }

  render() {
    if (this._loading) {
      return html`
        <div class="text-center p-4">
          <span class="spinner-border" role="status"></span>
          <span class="ms-2">Loading test plans...</span>
        </div>
      `;
    }

    if (this._error) {
      return html`
        <div class="alert alert-danger" role="alert"> <strong>Error:</strong> ${this._error} </div>
      `;
    }

    const filteredPlans = this._filteredPlans();

    return html`
      <div class="mb-3">
        <input
          type="text"
          class="form-control"
          placeholder="Search test plans..."
          .value=${this._searchQuery}
          @input=${this._handleSearchInput}
        />
      </div>

      ${filteredPlans.length > 0
        ? this._renderTable(filteredPlans)
        : html`<div class="text-muted text-center p-3">No test plans found</div>`}
      ${this._renderConfigModal()}
    `;
  }
}

customElements.define("cts-plan-list", CtsPlanList);
