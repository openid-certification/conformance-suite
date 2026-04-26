import { LitElement, html, nothing } from "lit";
import { classMap } from "lit/directives/class-map.js";
import "./cts-icon.js";

/**
 * Searchable, family-filterable list of test plans. Caller supplies the
 * `plans` array and reacts to selection events.
 *
 * Light DOM. Scoped CSS lives in a single `<style>` element injected into
 * `<head>` on first connect (gated by a module-level flag). Styling uses
 * OIDF tokens — the search input mirrors cts-data-table's
 * `.oidf-dt-search-input-wrap` (leading magnifier glyph + trailing clear
 * button) but in its simplified live-debounced form: filtering happens as
 * the user types, so there is no submit/return affordance. Pressing Escape
 * also clears the field.
 *
 * @property {Array} plans - Test plans to list; each has `planName`,
 *   `displayName`, `specFamily`, `modules`, `summary`.
 * @property {string} selected - Currently selected `planName`; the matching
 *   row is highlighted.
 * @fires cts-plan-select - When a list item is clicked, with
 *   `{ detail: { plan } }`; bubbles.
 */

const STYLE_ID = "cts-test-selector-styles";

// Inline SVG chevron used as the custom select indicator. Stroke colour is
// `--ink-500` (`#71695E`) — encoded as `%2371695E` in the data: URL.
const SELECT_CHEVRON =
  "url(\"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 16 16'><path fill='none' stroke='%2371695E' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' d='M4 6l4 4 4-4'/></svg>\")";

const STYLE_TEXT = `
.oidf-test-selector {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}
.oidf-test-selector__filters {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--space-3);
}
@media (max-width: 576px) {
  .oidf-test-selector__filters {
    grid-template-columns: 1fr;
  }
}
.oidf-test-selector__search-wrap,
.oidf-test-selector__family {
  width: 100%;
  box-sizing: border-box;
  height: 34px;
  border: 1px solid var(--ink-300);
  border-radius: var(--radius-2);
  background: var(--bg-elev);
  color: var(--fg);
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  line-height: var(--lh-base);
}
.oidf-test-selector__family {
  appearance: none;
  -webkit-appearance: none;
  padding: 0 36px 0 var(--space-3);
  background-image: ${SELECT_CHEVRON};
  background-repeat: no-repeat;
  background-position: right 12px center;
  /* See cts-form-field .oidf-select — pin to 1 for crisp closed-state baseline. */
  line-height: 1;
}
.oidf-test-selector__search-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-2);
  transition: border-color var(--dur-1) var(--ease-standard),
    box-shadow var(--dur-1) var(--ease-standard);
}
.oidf-test-selector__search-wrap:hover {
  border-color: var(--ink-400);
}
.oidf-test-selector__search-wrap:focus-within,
.oidf-test-selector__family:focus {
  outline: none;
  border-color: var(--orange-400);
  box-shadow: var(--focus-ring);
}
.oidf-test-selector__search-leading {
  display: inline-flex;
  align-items: center;
  color: var(--ink-400);
  flex-shrink: 0;
}
.oidf-test-selector__search {
  flex: 1;
  min-width: 0;
  border: 0;
  background: transparent;
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  line-height: 16px;
  color: var(--fg);
  outline: none;
  padding: 0;
  text-indent: 0;
}
.oidf-test-selector__search::-webkit-search-cancel-button,
.oidf-test-selector__search::-webkit-search-decoration {
  -webkit-appearance: none;
  appearance: none;
}
.oidf-test-selector__search::placeholder {
  color: var(--fg-faint);
}
.oidf-test-selector__search-clear {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  height: 24px;
  width: 24px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--fg-soft);
  border-radius: var(--radius-2);
  cursor: pointer;
  transition: background var(--dur-1) var(--ease-standard),
    color var(--dur-1) var(--ease-standard);
}
.oidf-test-selector__search-clear:hover {
  background: var(--ink-100);
  color: var(--fg);
}
.oidf-test-selector__search-clear:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
}
.oidf-test-selector__list {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border);
  border-radius: var(--radius-2);
  background: var(--bg-elev);
  overflow: hidden;
}
.oidf-test-selector__row {
  display: block;
  width: 100%;
  text-align: left;
  padding: var(--space-3) var(--space-4);
  border: none;
  border-top: 1px solid var(--divider);
  background: var(--bg-elev);
  color: var(--fg);
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  line-height: var(--lh-base);
  cursor: pointer;
  transition: background var(--dur-1) var(--ease-standard);
}
.oidf-test-selector__row:first-child {
  border-top: none;
}
.oidf-test-selector__row:hover {
  background: var(--ink-50);
}
.oidf-test-selector__row:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
  position: relative;
  z-index: 1;
}
.oidf-test-selector__row.is-active {
  background: var(--orange-50);
  color: var(--fg);
  font-weight: var(--fw-bold);
}
.oidf-test-selector__row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-3);
}
.oidf-test-selector__row-title {
  display: inline-flex;
  align-items: baseline;
  gap: var(--space-2);
  min-width: 0;
}
.oidf-test-selector__row-name {
  font-weight: var(--fw-medium);
  color: var(--fg);
  word-break: break-word;
}
.oidf-test-selector__row-family {
  /* mirrors .t-meta */
  font-family: var(--font-sans);
  font-size: var(--fs-12);
  line-height: var(--lh-snug);
  color: var(--fg-soft);
  font-weight: var(--fw-regular);
}
.oidf-test-selector__row-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--ink-100);
  color: var(--fg-soft);
  font-size: var(--fs-12);
  font-weight: var(--fw-medium);
  font-family: var(--font-sans);
}
.oidf-test-selector__row-summary {
  display: block;
  margin-top: var(--space-1);
  font-size: var(--fs-12);
  line-height: var(--lh-snug);
  color: var(--fg-soft);
  font-weight: var(--fw-regular);
}
.oidf-test-selector__empty {
  padding: var(--space-4);
  text-align: center;
  color: var(--fg-soft);
  font-family: var(--font-sans);
  font-size: var(--fs-13);
  line-height: var(--lh-base);
}
`;

function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

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

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
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
  _handleSearchKeydown(e) {
    if (e.key === "Escape" && this._searchTerm !== "") {
      e.preventDefault();
      this._searchTerm = "";
    }
  }
  _handleSearchClear() {
    this._searchTerm = "";
    // Restore focus to the input so the user can keep typing.
    const input = /** @type {HTMLInputElement | null} */ (
      this.querySelector(".oidf-test-selector__search")
    );
    if (input) input.focus();
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
      <div class="oidf-test-selector">
        <div class="oidf-test-selector__filters">
          <div class="oidf-test-selector__search-wrap">
            <cts-icon
              name="search-magnifying-glass"
              class="oidf-test-selector__search-leading"
              aria-hidden="true"
            ></cts-icon>
            <input
              type="search"
              class="oidf-test-selector__search"
              placeholder="Search test plans..."
              aria-label="Search test plans"
              autocomplete="off"
              spellcheck="false"
              .value=${this._searchTerm}
              @input=${this._handleSearch}
              @keydown=${this._handleSearchKeydown}
            />
            ${this._searchTerm
              ? html`<button
                  type="button"
                  class="oidf-test-selector__search-clear"
                  aria-label="Clear search"
                  title="Clear search (Esc)"
                  @click=${this._handleSearchClear}
                >
                  <cts-icon name="close-md" aria-hidden="true"></cts-icon>
                </button>`
              : nothing}
          </div>
          <select
            class="oidf-test-selector__family"
            aria-label="Filter test plans by specification family"
            @change=${this._handleFamilyFilter}
          >
            <option value="">All specifications</option>
            ${this._renderFamilyOptions()}
          </select>
        </div>
        <div class="oidf-test-selector__list" role="list">
          ${this._filteredPlans.length > 0
            ? this._filteredPlans.map(
                (plan) => html`
                  <button
                    type="button"
                    role="listitem"
                    class=${classMap({
                      "oidf-test-selector__row": true,
                      "is-active": this.selected === plan.planName,
                    })}
                    data-plan-name="${plan.planName}"
                    @click=${this._handleSelectPlanFromEvent}
                  >
                    <span class="oidf-test-selector__row-head">
                      <span class="oidf-test-selector__row-title">
                        <strong class="oidf-test-selector__row-name"
                          >${plan.displayName || plan.planName}</strong
                        >
                        ${plan.specFamily
                          ? html`<span class="oidf-test-selector__row-family"
                              >${plan.specFamily}</span
                            >`
                          : nothing}
                      </span>
                      ${plan.modules
                        ? html`<span class="oidf-test-selector__row-count"
                            >${plan.modules.length}</span
                          >`
                        : nothing}
                    </span>
                    ${plan.summary
                      ? html`<span class="oidf-test-selector__row-summary">${plan.summary}</span>`
                      : nothing}
                  </button>
                `,
              )
            : html`<div class="oidf-test-selector__empty">No plans match your search</div>`}
        </div>
      </div>
    `;
  }

  _renderFamilyOptions() {
    return this._families.map((f) => html`<option value="${f}">${f}</option>`);
  }
}
customElements.define("cts-test-selector", CtsTestSelector);

export {};
