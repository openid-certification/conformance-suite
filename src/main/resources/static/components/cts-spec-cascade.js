import { LitElement, html } from "lit";
import { classMap } from "lit/directives/class-map.js";

/**
 * Four-level cascading selector (Specification -> Entity -> Version -> Plan)
 * backed by `/api/runner/available`. Auto-selects single-option tiers.
 *
 * Light DOM. Scoped CSS lives in a single `<style>` element injected into
 * `<head>` on first connect (gated by a module-level flag). The component
 * emits OIDF-tokenized class names (`.oidf-spec-cascade__*`) that mirror
 * `cts-form-field`'s `.oidf-select` styling so the dropdowns visually align
 * with other OIDF form controls.
 *
 * @property {Array} plans - Available plans. If not provided the component
 *   fetches from `/api/runner/available` on connect.
 * @fires cts-plan-selected - When a plan is chosen (manually or via
 *   auto-select), with `{ detail: { plan } }`; bubbles.
 */

const STYLE_ID = "cts-spec-cascade-styles";

// Inline SVG chevron used as the custom select indicator. Stroke colour is
// `--ink-500` (`#71695E`) — encoded as `%2371695E` in the data: URL.
const SELECT_CHEVRON =
  "url(\"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 16 16'><path fill='none' stroke='%2371695E' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' d='M4 6l4 4 4-4'/></svg>\")";

const STYLE_TEXT = `
.oidf-spec-cascade {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}
@media (max-width: 768px) {
  .oidf-spec-cascade {
    grid-template-columns: 1fr;
  }
}
.oidf-spec-cascade__field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}
.oidf-spec-cascade__label {
  /* mirrors .t-overline from oidf-tokens.css */
  font-family: var(--font-sans);
  font-weight: var(--fw-bold);
  font-size: var(--fs-12);
  line-height: var(--lh-snug);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--fg-soft);
}
.oidf-spec-cascade__select {
  width: 100%;
  box-sizing: border-box;
  height: 34px;
  padding: 0 36px 0 var(--space-3);
  border: 1px solid var(--ink-300);
  border-radius: var(--radius-2);
  background: var(--bg-elev);
  color: var(--fg);
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  /* See cts-form-field .oidf-select — pin to 1 for crisp closed-state baseline. */
  line-height: 1;
  appearance: none;
  -webkit-appearance: none;
  background-image: ${SELECT_CHEVRON};
  background-repeat: no-repeat;
  background-position: right 12px center;
}
.oidf-spec-cascade__select:focus {
  outline: none;
  border-color: var(--orange-400);
  box-shadow: var(--focus-ring);
}
.oidf-spec-cascade__select:disabled {
  background-color: var(--bg-muted);
  color: var(--fg-faint);
  cursor: not-allowed;
}
.oidf-spec-cascade__loading,
.oidf-spec-cascade__alert {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-2);
  border: 1px solid;
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  line-height: var(--lh-base);
}
.oidf-spec-cascade__loading {
  background: var(--bg-muted);
  border-color: var(--border);
  color: var(--fg-soft);
  text-align: center;
}
.oidf-spec-cascade__alert--error {
  background: var(--status-fail-bg);
  border-color: var(--status-fail-border);
  color: var(--rust-500);
}
.oidf-spec-cascade__alert--info {
  background: var(--status-info-bg);
  border-color: var(--status-info-border);
  color: var(--status-info);
}
`;

function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

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

  createRenderRoot() {
    return this;
  }

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
    injectStyles();
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

  /**
   * Build a 3-level index: family -> entity -> version -> [plan, ...]
   * @returns {object} Nested index keyed by spec family, then entity-under-test,
   *   then spec version, with plan objects as the leaf array.
   */
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
    this.dispatchEvent(
      new CustomEvent("cts-plan-selected", {
        bubbles: true,
        detail: { plan },
      }),
    );
  }

  _renderField(label, selectId, options, value, placeholder, changeHandler, visible) {
    return html`
      <div
        class="oidf-spec-cascade__field"
        data-testid="${selectId}-field"
        style="${visible ? "" : "display:none"}"
      >
        <label for="${selectId}" class="oidf-spec-cascade__label">${label}</label>
        <select
          id="${selectId}"
          class="oidf-spec-cascade__select"
          .value=${value}
          @change=${changeHandler}
        >
          <option value="">${placeholder}</option>
          ${this._renderSelectOptions(options, value)}
        </select>
      </div>
    `;
  }

  _renderSelectOptions(options, value) {
    return options.map(
      (opt) => html`
        <option
          value="${typeof opt === "string" ? opt : opt.value}"
          ?selected=${value === (typeof opt === "string" ? opt : opt.value)}
        >
          ${typeof opt === "string" ? opt : opt.label}
        </option>
      `,
    );
  }

  render() {
    if (this._loading) {
      return html`
        <div class="oidf-spec-cascade__loading" role="status">
          <span>Loading available test plans...</span>
        </div>
      `;
    }

    if (this._error) {
      return html`
        <div
          class=${classMap({
            "oidf-spec-cascade__alert": true,
            "oidf-spec-cascade__alert--error": true,
          })}
          role="alert"
          data-testid="spec-cascade-error"
        >
          ${this._error}
        </div>
      `;
    }

    if (this.plans && this.plans.length === 0) {
      return html`
        <div
          class=${classMap({
            "oidf-spec-cascade__alert": true,
            "oidf-spec-cascade__alert--info": true,
          })}
          role="status"
          data-testid="spec-cascade-empty"
        >
          No test plans are available.
        </div>
      `;
    }

    const showEntity = this._selectedFamily && this._entities.length > 1;
    const showVersion = this._selectedEntity && this._versions.length > 1;
    const showPlan =
      this._selectedEntity && this._selectedVersion && this._filteredPlans.length > 0;

    const planOptions = this._filteredPlans.map((p) => ({
      value: p.planName,
      label: p.displayName || p.planName,
    }));

    const versionOpts = this._versions.map((v) => ({
      value: v,
      label: v || "(default)",
    }));

    return html`
      <div class="oidf-spec-cascade">
        ${this._renderField(
          "Specification",
          "specFamilySelect",
          this._families,
          this._selectedFamily,
          "--- Select a Specification ---",
          (e) => this._handleFamilyChange(e),
          true,
        )}
        ${this._renderField(
          "Entity Under Test",
          "entitySelect",
          this._entities,
          this._selectedEntity,
          "--- Select Entity ---",
          (e) => this._handleEntityChange(e),
          showEntity,
        )}
        ${this._renderField(
          "Version",
          "specVersionSelect",
          versionOpts,
          this._selectedVersion,
          "--- Select Version ---",
          (e) => this._handleVersionChange(e),
          showVersion,
        )}
        ${this._renderField(
          "Test Type",
          "planSelect",
          planOptions,
          this._selectedPlan,
          "--- Select A Test Type ----",
          (e) => this._handlePlanChange(e),
          showPlan,
        )}
      </div>
    `;
  }
}
customElements.define("cts-spec-cascade", CtsSpecCascade);

export {};
