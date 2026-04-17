import { LitElement, html } from "lit";

/**
 * Four-level cascading selector (Specification -> Entity -> Version -> Plan)
 * backed by `/api/runner/available`. Auto-selects single-option tiers.
 *
 * @property {Array} plans - Available plans. If not provided the component
 *   fetches from `/api/runner/available` on connect.
 *
 * @fires cts-plan-selected - When a plan is chosen (manually or via
 *   auto-select), with `{ detail: { plan } }`; bubbles.
 */
class CtsSpecCascade extends LitElement {
  static properties = {
    plans: { type: Array },
    _selectedFamily: { state: true },
    _selectedEntity: { state: true },
    _selectedVersion: { state: true },
    _selectedPlan: { state: true },
    _loading: { state: true },
    _error: { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this.plans = null;
    this._selectedFamily = "";
    this._selectedEntity = "";
    this._selectedVersion = "";
    this._selectedPlan = "";
    this._loading = false;
    this._error = "";
  }

  connectedCallback() {
    super.connectedCallback();
    if (!this.plans) {
      this._fetchPlans();
    }
  }

  async _fetchPlans() {
    this._loading = true;
    this._error = "";
    try {
      const response = await fetch("/api/runner/available");
      // The previous implementation called `response.json()` unconditionally,
      // which let a 5xx with a JSON error body silently become "plans = [error
      // payload]" and then fall through to an empty dropdown. Check ok first so
      // real failures route to the error state rather than the empty state.
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      this.plans = await response.json();
    } catch (err) {
      this.plans = [];
      this._error = "Unable to load plans — please reload the page.";
      console.warn("[cts-spec-cascade] /api/runner/available fetch failed:", err);
    } finally {
      this._loading = false;
    }
  }

  /** Build a 3-level index: family -> entity -> version -> [plan, ...] */
  get _planIndex() {
    const index = {};
    if (!this.plans) return index;
    for (const plan of this.plans) {
      const family = plan.specFamily || "";
      const entity = plan.entityUnderTest || "";
      const version = plan.specVersion || "";
      if (!family) continue;
      if (!index[family]) index[family] = {};
      if (!index[family][entity]) index[family][entity] = {};
      if (!index[family][entity][version]) index[family][entity][version] = [];
      index[family][entity][version].push(plan);
    }
    return index;
  }

  get _families() {
    return Object.keys(this._planIndex).sort();
  }

  get _entities() {
    const familyNode = this._planIndex[this._selectedFamily];
    if (!familyNode) return [];
    return Object.keys(familyNode).sort();
  }

  get _versions() {
    const familyNode = this._planIndex[this._selectedFamily];
    if (!familyNode) return [];
    const entityNode = familyNode[this._selectedEntity];
    if (!entityNode) return [];
    return Object.keys(entityNode).sort();
  }

  get _filteredPlans() {
    const familyNode = this._planIndex[this._selectedFamily];
    if (!familyNode) return [];
    const entityNode = familyNode[this._selectedEntity];
    if (!entityNode) return [];
    const versionNode = entityNode[this._selectedVersion];
    if (!versionNode) return [];
    return [...versionNode].sort((a, b) =>
      (a.displayName || "").localeCompare(b.displayName || ""),
    );
  }

  _handleFamilyChange(e) {
    this._selectedFamily = e.target.value;
    this._selectedEntity = "";
    this._selectedVersion = "";
    this._selectedPlan = "";

    // Auto-select entity if there is only one
    const entities = this._entities;
    if (entities.length === 1) {
      this._selectedEntity = entities[0];
      this._autoSelectVersion();
    }
  }

  _handleEntityChange(e) {
    this._selectedEntity = e.target.value;
    this._selectedVersion = "";
    this._selectedPlan = "";

    this._autoSelectVersion();
  }

  _autoSelectVersion() {
    // Auto-select version if there is only one
    const versions = this._versions;
    if (versions.length === 1) {
      this._selectedVersion = versions[0];
      this._autoSelectPlan();
    }
  }

  _handleVersionChange(e) {
    this._selectedVersion = e.target.value;
    this._selectedPlan = "";

    this._autoSelectPlan();
  }

  _autoSelectPlan() {
    // Auto-select plan if there is only one
    const plans = this._filteredPlans;
    if (plans.length === 1) {
      this._selectedPlan = plans[0].planName;
      this._emitPlanSelected(plans[0]);
    }
  }

  _handlePlanChange(e) {
    this._selectedPlan = e.target.value;
    const plan = this._filteredPlans.find((p) => p.planName === this._selectedPlan);
    if (plan) {
      this._emitPlanSelected(plan);
    }
  }

  _emitPlanSelected(plan) {
    this.dispatchEvent(new CustomEvent("cts-plan-selected", {
      bubbles: true,
      detail: { plan },
    }));
  }

  _renderRow(label, selectId, tooltipText, options, value, placeholder, changeHandler, visible) {
    return html`
      <div class="mb-3 row" style="${visible ? "" : "display:none"}">
        <label for="${selectId}" class="col-md-2 col-form-label">${label}</label>
        <div class="col-md-10">
          <div class="input-group">
            <div class="input-group-text">
              <span class="bi bi-info-circle-fill" data-bs-toggle="tooltip"
                data-bs-placement="bottom" title="${tooltipText}"></span>
            </div>
            <select id="${selectId}" class="form-select" .value=${value}
              @change=${changeHandler}>
              <option value="">${placeholder}</option>
              ${options.map((opt) => html`
                <option value="${typeof opt === "string" ? opt : opt.value}"
                  ?selected=${value === (typeof opt === "string" ? opt : opt.value)}>
                  ${typeof opt === "string" ? opt : opt.label}
                </option>
              `)}
            </select>
          </div>
        </div>
      </div>
    `;
  }

  render() {
    if (this._loading) {
      return html`
        <div class="mb-3 row">
          <div class="col-md-12 text-center">
            <div class="spinner-border text-primary" role="status">
              <span class="visually-hidden">Loading available test plans...</span>
            </div>
          </div>
        </div>
      `;
    }

    if (this._error) {
      return html`
        <div class="alert alert-danger" role="alert" data-testid="spec-cascade-error">
          ${this._error}
        </div>
      `;
    }

    if (this.plans && this.plans.length === 0) {
      return html`
        <div class="alert alert-info" role="status" data-testid="spec-cascade-empty">
          No test plans are available.
        </div>
      `;
    }

    const showEntity = this._selectedFamily && this._entities.length > 1;
    const showVersion = this._selectedEntity && this._versions.length > 1;
    const showPlan = this._selectedEntity && this._selectedVersion && this._filteredPlans.length > 0;

    const planOptions = this._filteredPlans.map((p) => ({
      value: p.planName,
      label: p.displayName || p.planName,
    }));

    const versionOpts = this._versions.map((v) => ({
      value: v,
      label: v || "(default)",
    }));

    return html`
      ${this._renderRow(
        "Specification",
        "specFamilySelect",
        "Select the specification family you want to run tests for.",
        this._families,
        this._selectedFamily,
        "--- Select a Specification ---",
        (e) => this._handleFamilyChange(e),
        true,
      )}
      ${this._renderRow(
        "Entity Under Test",
        "entitySelect",
        "Select the type of implementation you are testing, such as a client, authorization server, wallet, verifier, or issuer.",
        this._entities,
        this._selectedEntity,
        "--- Select Entity ---",
        (e) => this._handleEntityChange(e),
        showEntity,
      )}
      ${this._renderRow(
        "Version",
        "specVersionSelect",
        "Select the specification version you'd like to test with.",
        versionOpts,
        this._selectedVersion,
        "--- Select Version ---",
        (e) => this._handleVersionChange(e),
        showVersion,
      )}
      ${this._renderRow(
        "Test Plan",
        "planSelect",
        "Select the exact test plan you want to run.",
        planOptions,
        this._selectedPlan,
        "--- Select A Test Plan ----",
        (e) => this._handlePlanChange(e),
        showPlan,
      )}
    `;
  }
}
customElements.define("cts-spec-cascade", CtsSpecCascade);
