import { LitElement, html, nothing } from "lit";

/**
 * Searchable, family-filterable list of test plans. Caller supplies the
 * `plans` array and reacts to selection events.
 * @property {Array} plans - Test plans to list; each has `planName`,
 *   `displayName`, `specFamily`, `modules`, `summary`.
 * @property {string} selected - Currently selected `planName`; the matching
 *   row is highlighted.
 * @fires cts-plan-select - When a list item is clicked, with
 *   `{ detail: { plan } }`; bubbles.
 */
class CtsTestSelector extends LitElement {
  static properties = {
    plans: { type: Array },
    selected: { type: String },
    _searchTerm: { state: true },
    _selectedFamily: { state: true },
  };

  createRenderRoot() {
    return this;
  }

  constructor() {
    super();
    this.plans = [];
    this.selected = "";
    this._searchTerm = "";
    this._selectedFamily = "";
  }

  get _families() {
    const families = new Set(this.plans.map((p) => p.specFamily).filter(Boolean));
    return Array.from(families).sort();
  }

  get _filteredPlans() {
    let filtered = this.plans;
    if (this._selectedFamily) {
      filtered = filtered.filter((p) => p.specFamily === this._selectedFamily);
    }
    if (this._searchTerm) {
      const term = this._searchTerm.toLowerCase();
      filtered = filtered.filter(
        (p) =>
          (p.displayName || "").toLowerCase().includes(term) ||
          (p.planName || "").toLowerCase().includes(term),
      );
    }
    return filtered;
  }

  _handleSearch(e) {
    this._searchTerm = e.target.value;
  }
  _handleFamilyFilter(e) {
    this._selectedFamily = e.target.value;
  }

  _handleSelectPlan(plan) {
    this.selected = plan.planName;
    this.dispatchEvent(new CustomEvent("cts-plan-select", { bubbles: true, detail: { plan } }));
  }

  _handleSelectPlanFromEvent(event) {
    const planName = event.currentTarget.dataset.planName;
    const plan = this.plans.find((p) => p.planName === planName);
    if (plan) this._handleSelectPlan(plan);
  }

  render() {
    return html`
      <div class="mb-3">
        <div class="row g-2 mb-3">
          <div class="col-md-8">
            <input
              type="text"
              class="form-control"
              placeholder="Search test plans..."
              .value=${this._searchTerm}
              @input=${this._handleSearch}
            />
          </div>
          <div class="col-md-4">
            <select class="form-select" @change=${this._handleFamilyFilter}>
              <option value="">All specifications</option>
              ${this._families.map((f) => html`<option value="${f}">${f}</option>`)}
            </select>
          </div>
        </div>
        <div class="list-group">
          ${this._filteredPlans.length > 0
            ? this._filteredPlans.map(
                (plan) => html`
                  <button
                    type="button"
                    class="list-group-item list-group-item-action${this.selected === plan.planName
                      ? " active"
                      : ""}"
                    data-plan-name="${plan.planName}"
                    @click=${this._handleSelectPlanFromEvent}
                  >
                    <div class="d-flex justify-content-between align-items-center">
                      <div>
                        <strong>${plan.displayName || plan.planName}</strong>
                        ${plan.specFamily
                          ? html`<small class="text-muted ms-2">${plan.specFamily}</small>`
                          : nothing}
                      </div>
                      ${plan.modules
                        ? html`<span class="badge bg-secondary rounded-pill"
                            >${plan.modules.length}</span
                          >`
                        : nothing}
                    </div>
                    ${plan.summary
                      ? html`<small class="text-muted">${plan.summary}</small>`
                      : nothing}
                  </button>
                `,
              )
            : html`<div class="list-group-item text-muted text-center"
                >No plans match your search</div
              >`}
        </div>
      </div>
    `;
  }
}
customElements.define("cts-test-selector", CtsTestSelector);
