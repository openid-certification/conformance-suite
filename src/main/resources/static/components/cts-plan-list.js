import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";
import "./cts-modal.js";
import "./cts-button.js";
import "./cts-alert.js";

const RESULT_BADGE_VARIANTS = {
  PASSED: "success",
  FAILED: "failure",
  WARNING: "warning",
  REVIEW: "review",
  SKIPPED: "skipped",
  INTERRUPTED: "interrupted",
};

const STYLE_ID = "cts-plan-list-styles";

// Scoped CSS for the plan-list table chrome. Mirrors the design archive's
// `.tbl-wrap` / `.tbl` rules in `project/ui_kits/certification-suite/app.css`:
// a single bordered/rounded surface, sticky uppercase header on `--ink-50`,
// row hover on `--ink-50`, and `--ink-100` row dividers. No Bootstrap table
// classes are emitted (Adv F12).
const STYLE_TEXT = `
  cts-plan-list {
    display: block;
  }
  cts-plan-list .planSearch {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    padding: 0 var(--space-3);
    height: 36px;
    border: 1px solid var(--ink-300);
    border-radius: var(--radius-2);
    background: var(--bg-elev);
    margin-bottom: var(--space-4);
    max-width: 480px;
  }
  cts-plan-list .planSearch input {
    flex: 1;
    border: 0;
    background: transparent;
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    color: var(--fg);
    outline: none;
    padding: 0;
  }
  cts-plan-list .planSearch input::placeholder {
    color: var(--fg-faint);
  }
  cts-plan-list .planSearch:focus-within {
    box-shadow: var(--focus-ring);
    border-color: var(--orange-400);
  }
  cts-plan-list .planTableWrap {
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    overflow: hidden;
  }
  cts-plan-list .planTable {
    width: 100%;
    border-collapse: collapse;
    font-size: var(--fs-13);
  }
  cts-plan-list .planTable th {
    position: sticky;
    top: 0;
    text-align: left;
    padding: 10px 14px;
    background: var(--ink-50);
    border-bottom: 1px solid var(--border);
    font-size: var(--fs-12);
    font-weight: var(--fw-medium);
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: var(--fg-soft);
  }
  cts-plan-list .planTable td {
    padding: 12px 14px;
    border-bottom: 1px solid var(--ink-100);
    vertical-align: middle;
    color: var(--fg);
  }
  cts-plan-list .planTable tbody tr:hover td {
    background: var(--ink-50);
  }
  cts-plan-list .planTable tbody tr:last-child td {
    border-bottom: 0;
  }
  cts-plan-list .planTable .plan-name-link {
    color: var(--fg-link);
    text-decoration: none;
    font-weight: var(--fw-medium);
  }
  cts-plan-list .planTable .plan-name-link:hover {
    text-decoration: underline;
  }
  cts-plan-list .planTable .moduleBadgeStack {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-1);
  }
  cts-plan-list .planEmpty {
    padding: var(--space-5);
    text-align: center;
    color: var(--fg-soft);
  }
  cts-plan-list .planLoading {
    padding: var(--space-5);
    text-align: center;
    color: var(--fg-soft);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-2);
  }
  cts-plan-list .planLoading .spinner-border {
    display: inline-block;
    width: 16px;
    height: 16px;
    border: 2px solid var(--border-strong);
    border-top-color: var(--orange-400);
    border-radius: 50%;
    animation: cts-plan-list-spin 0.9s linear infinite;
  }
  @keyframes cts-plan-list-spin {
    to { transform: rotate(360deg); }
  }
  cts-plan-list .planConfigToolbar {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin-bottom: var(--space-2);
  }
  cts-plan-list .planConfigToolbar code {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    color: var(--fg-soft);
    background: var(--ink-50);
    padding: 1px 6px;
    border-radius: var(--radius-1);
  }
  cts-plan-list .planConfigJson {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    background: var(--ink-50);
    color: var(--ink-900);
    border-radius: var(--radius-2);
    padding: var(--space-3);
    margin: 0;
    white-space: pre-wrap;
    word-break: break-word;
    max-height: 60vh;
    overflow: auto;
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
 * Searchable table of test plans. Fetches from `/api/plan` (or
 * `/api/plan?public=true`) and renders rows with name, variant, module
 * badges, and a config viewer modal.
 *
 * Light DOM. Scoped CSS is injected once on first connect; the rendered
 * `<table>` carries no Bootstrap `table-*` classes — header, hover, and
 * border styling all route through the scoped `.planTable` rules above
 * (Adv F12).
 *
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
    ensureStylesInjected();
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
    return html`<div class="moduleBadgeStack">${this._moduleBadgeList(modules)}</div>`;
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
      <div class="planTableWrap">
        <table class="planTable">
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
          <tbody> ${this._renderRows(plans)} </tbody>
        </table>
      </div>
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
          <cts-button
            class="showConfigBtn"
            variant="ghost"
            size="sm"
            icon="settings"
            title="View configuration"
            data-plan-id="${plan._id}"
            @cts-click=${this._handleConfigButtonClick}
          ></cts-button>
        </td>
        ${this.isAdmin ? html`<td class="owner-cell">${ownerDisplay}</td>` : nothing}
      </tr>
    `;
  }

  _renderConfigModal() {
    const configJson = this._selectedConfig ? JSON.stringify(this._selectedConfig, null, 4) : "";
    return html`
      <cts-modal id="planConfigModal" heading="Configuration">
        <div class="planConfigToolbar">
          <cts-button
            class="copy-config-btn"
            variant="secondary"
            size="sm"
            icon="copy"
            label="Copy"
            title="Copy config to clipboard"
            @cts-click=${this._handleCopyConfig}
          ></cts-button>
          <span>Configuration for <code>${this._selectedPlanId}</code></span>
        </div>
        <pre class="planConfigJson config-json">${configJson}</pre>
      </cts-modal>
    `;
  }

  render() {
    if (this._loading) {
      return html`
        <div class="planLoading">
          <span class="spinner-border" role="status"></span>
          <span>Loading test plans...</span>
        </div>
      `;
    }

    if (this._error) {
      return html`
        <cts-alert variant="danger" role="alert">
          <strong>Error:</strong> ${this._error}
        </cts-alert>
      `;
    }

    const filteredPlans = this._filteredPlans();

    return html`
      <div class="planSearch">
        <cts-icon name="search-magnifying-glass" aria-hidden="true"></cts-icon>
        <input
          type="text"
          placeholder="Search test plans..."
          .value=${this._searchQuery}
          @input=${this._handleSearchInput}
        />
      </div>

      ${filteredPlans.length > 0
        ? this._renderTable(filteredPlans)
        : html`<div class="planEmpty">No test plans found</div>`}
      ${this._renderConfigModal()}
    `;
  }
}

customElements.define("cts-plan-list", CtsPlanList);

export {};
