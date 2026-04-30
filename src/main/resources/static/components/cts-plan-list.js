import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";
import "./cts-modal.js";
import "./cts-button.js";
import "./cts-alert.js";
import "./cts-data-table.js";
import "./cts-json-editor.js";
import { flashCopyConfirmed } from "../js/cts-copy-flash.js";

const RESULT_BADGE_VARIANTS = {
  PASSED: "success",
  FAILED: "failure",
  WARNING: "warning",
  REVIEW: "review",
  SKIPPED: "skipped",
  INTERRUPTED: "interrupted",
};

const STYLE_ID = "cts-plan-list-styles";

// Scoped CSS for the plan-list-specific bits that cts-data-table doesn't
// own: the module badge stack, the plan-name link inside the planName cell,
// the loading spinner shown before the table mounts, and the config modal
// chrome. The search input and table chrome (border, sticky header, hover,
// dividers) all come from cts-data-table now (Adv F12). The legacy plan
// listing is unpaged, so the pager strip is suppressed.
const STYLE_TEXT = `
  cts-plan-list {
    display: block;
  }
  cts-plan-list cts-data-table .oidf-dt-pager {
    display: none;
  }
  cts-plan-list .plan-name-link {
    color: var(--fg-link);
    text-decoration: none;
    font-weight: var(--fw-medium);
  }
  cts-plan-list .plan-name-link:hover {
    text-decoration: underline;
  }
  cts-plan-list .moduleBadgeStack {
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
  /* '.planConfigJson' (and the '.config-json' sibling on the same element)
     are pre-existing class names from when this slot rendered a <pre>;
     the slot is now a <cts-json-editor> inside the cts-modal. Do not
     rename without updating all three call sites at once: this CSS rule,
     the '.config-json' selector that cts-plan-list.stories.js + the
     ClipboardJS handlers in plans.html / logs.html / log-detail.html
     match on, and the data-clipboard-target=".config-json" attribute. */
  cts-plan-list .planConfigJson {
    display: block;
    margin: 0;
    max-height: 60vh;
    min-height: calc(var(--space-6) * 14);
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
 * Light DOM. Scoped CSS is injected once on first connect. The table
 * chrome and the advanced search affordances (clear button, active-filter
 * chip with "Show all" reset, Escape-to-clear) are delegated to
 * `<cts-data-table>` in client-side, live-debounced mode. cts-plan-list
 * supplies a `cellRenderer` for the bespoke cells (plan-link click,
 * variant formatter, formatted date, module badge stack, config button).
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
    this._selectedConfig = null;
    this._selectedPlanId = "";
    // The cellRenderer's TemplateResult is interpolated and rendered by
    // cts-data-table. Lit's EventPart dispatches event listeners with `this`
    // set to the rendering host (cts-data-table), not this component, so we
    // pre-bind every handler that may be wired through cellRenderer.
    this._cellRenderer = this._cellRenderer.bind(this);
    this._handlePlanLinkClick = this._handlePlanLinkClick.bind(this);
    this._handleConfigButtonClick = this._handleConfigButtonClick.bind(this);
    this._handleCopyConfig = this._handleCopyConfig.bind(this);
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

  async _handleCopyConfig(event) {
    // Capture currentTarget synchronously: the await below clears it
    // because event dispatch has completed by the time we resume.
    const trigger = event && event.currentTarget;
    if (!this._selectedConfig) return;
    try {
      await navigator.clipboard.writeText(JSON.stringify(this._selectedConfig, null, 4));
    } catch (err) {
      console.warn("[cts-plan-list] clipboard.writeText failed:", err);
      return;
    }
    flashCopyConfirmed(trigger);
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

  _columns() {
    const cols = [
      { key: "planName", label: "Plan Name" },
      { key: "variant", label: "Variant" },
      { key: "description", label: "Description" },
      { key: "started", label: "Started" },
      { key: "modules", label: "Modules" },
      { key: "_config", label: "Config" },
    ];
    if (this.isAdmin) cols.push({ key: "owner.sub", label: "Owner" });
    return cols;
  }

  _cellRenderer(row, key) {
    // Event listeners reference handlers that are pre-bound in the
    // constructor. Lit's EventPart in cts-data-table dispatches with `this`
    // set to the cts-data-table host, but the bound functions retain their
    // original `this` (CtsPlanList) regardless of how Lit invokes them.
    if (key === "planName") {
      return html`<a
        href="#"
        class="plan-name-link"
        data-plan-id="${row._id}"
        @click=${this._handlePlanLinkClick}
        >${row.planName}</a
      >`;
    }
    if (key === "variant") return this._formatVariant(row.variant);
    if (key === "started")
      return html`<span class="tabular-nums">${this._formatDate(row.started)}</span>`;
    if (key === "modules") return this._renderModuleBadges(row.modules);
    if (key === "_config") {
      return html`<cts-button
        class="showConfigBtn"
        variant="ghost"
        size="sm"
        icon="settings"
        title="View configuration"
        data-plan-id="${row._id}"
        @cts-click=${this._handleConfigButtonClick}
      ></cts-button>`;
    }
    if (key === "owner.sub") {
      const display = row.owner ? row.owner.sub || "unknown" : "";
      return html`<span class="owner-cell">${display}</span>`;
    }
    return undefined;
  }

  _rowClass() {
    // Stable class name on each <tr> so consumers (and existing tests that
    // filter on `tbody tr`) can target rendered rows without the data-row-index
    // attribute alone.
    return "planRow";
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
        <cts-json-editor
          class="planConfigJson config-json"
          readonly
          aria-label="Plan configuration JSON"
          .value=${configJson}
        ></cts-json-editor>
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

    if (this._plans.length === 0) {
      return html`<div class="planEmpty">No test plans found</div>`;
    }

    return html`
      <cts-data-table
        .columns=${this._columns()}
        .rows=${this._plans}
        .serverSide=${false}
        page-size="1000"
        search-placeholder="Search test plans..."
        search-mode="live-debounced"
        empty-state="No test plans match your search"
        .cellRenderer=${this._cellRenderer}
        .rowClass=${this._rowClass}
      ></cts-data-table>
      ${this._renderConfigModal()}
    `;
  }
}

customElements.define("cts-plan-list", CtsPlanList);

export {};
