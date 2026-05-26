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
 * Keyboard navigation: from the search input, ArrowDown moves focus into
 * the first visible row. From a row, ArrowDown/ArrowUp roves across rows;
 * ArrowUp on the first row returns focus to the search input. Enter (or
 * Space) on a focused row commits the selection. Rows use roving
 * tabindex so Tab escapes the list cleanly rather than cycling every row.
 *
 * @property {Array} plans - Test plans to list; each has `planName`,
 *   `displayName`, `specFamily`, `modules`, `summary`.
 * @property {string} selected - Currently selected `planName`; the matching
 *   row is highlighted.
 * @fires cts-plan-select - When a list item is selected, with
 *   `{ detail: { plan, via } }` where `via` is `'click'` or `'keyboard'`;
 *   bubbles.
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
    _focusedRowIndex: { state: true },
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
    // -1 means "no row focused — search input owns focus (or focus is
    // elsewhere on the page)". A non-negative value names the row that
    // should receive focus on the next render-completion tick.
    this._focusedRowIndex = -1;
    // Set on row keydown(Enter|Space) so the click handler that follows
    // can report `via: 'keyboard'`. Native <button> elements fire a
    // synthetic click event on Enter/Space activation; consolidating the
    // dispatch on that single click avoids the double-fire that any
    // keyup-based dispatch would risk (testing libraries fire click
    // BEFORE keyup, so a keyup-driven dispatch would land after the
    // click handler already ran). Cleared after each click dispatch.
    this._activationVia = null;
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
    // Typing rebuilds the filtered list; any prior row-focus pointer
    // would be stale (could exceed the new list bounds, or land on a
    // different plan). Reset so updated() doesn't re-focus a row the
    // user moved away from by typing.
    this._focusedRowIndex = -1;
  }
  _handleSearchKeydown(e) {
    if (e.key === "Escape" && this._searchTerm !== "") {
      e.preventDefault();
      this._searchTerm = "";
      return;
    }
    if (e.key === "ArrowDown" && this._filteredPlans.length > 0) {
      e.preventDefault();
      this._focusedRowIndex = 0;
    }
  }
  _handleSearchClear() {
    this._searchTerm = "";
    this._focusedRowIndex = -1;
    // Restore focus to the input so the user can keep typing.
    const input = /** @type {HTMLInputElement | null} */ (
      this.querySelector(".oidf-test-selector__search")
    );
    if (input) input.focus();
  }
  _handleFamilyFilter(e) {
    this._selectedFamily = e.target.value;
    this._focusedRowIndex = -1;
  }

  // Read the row's position from data-index. Closing over `index` in the
  // template would trigger lit/no-template-arrow; dataset lookup keeps
  // the template handler a bare method reference.
  _rowIndexFromEvent(event) {
    const raw = /** @type {HTMLElement} */ (event.currentTarget).dataset.index;
    const parsed = raw === undefined ? NaN : Number(raw);
    return Number.isFinite(parsed) ? parsed : -1;
  }

  _handleRowKeydown(e) {
    const index = this._rowIndexFromEvent(e);
    if (index < 0) return;
    const key = e.key;
    if (key === "ArrowDown") {
      e.preventDefault();
      const last = this._filteredPlans.length - 1;
      if (index < last) this._focusedRowIndex = index + 1;
      return;
    }
    if (key === "ArrowUp") {
      e.preventDefault();
      if (index > 0) {
        this._focusedRowIndex = index - 1;
      } else {
        // ArrowUp on the first row returns to the search input. Move
        // focus synchronously since the input is already in the DOM —
        // no need to wait for the next render cycle.
        this._focusedRowIndex = -1;
        const input = /** @type {HTMLInputElement | null} */ (
          this.querySelector(".oidf-test-selector__search")
        );
        if (input) input.focus();
      }
      return;
    }
    if (key === "Enter" || key === " ") {
      // Tag the activation modality so the click event that follows
      // (native <button> Enter/Space activation fires a click) reports
      // via:'keyboard'. Do not preventDefault — we want the native
      // click to fire so dispatch happens exactly once per activation.
      this._activationVia = "keyboard";
    }
  }

  _handleRowClick(e) {
    const index = this._rowIndexFromEvent(e);
    if (index < 0) return;
    const plan = this._filteredPlans[index];
    if (!plan) return;
    // Read and clear the intent flag in one step. If keydown(Enter|Space)
    // marked the activation as keyboard-driven, honor that; otherwise the
    // click came from a real pointer and reports as 'click'.
    const via = this._activationVia ?? "click";
    this._activationVia = null;
    this._handleSelectPlan(plan, via);
  }

  _handleSelectPlan(plan, via) {
    this.selected = plan.planName;
    this.dispatchEvent(
      new CustomEvent("cts-plan-select", { bubbles: true, detail: { plan, via } }),
    );
  }

  updated(changed) {
    // After the post-render commit, move real DOM focus to the row that
    // the index now points at. Running here (not inside the keydown
    // handler) ensures the new tabindex="0" / -1 mapping is already in
    // the DOM before .focus() is called.
    if (changed.has("_focusedRowIndex") && this._focusedRowIndex >= 0) {
      const rows = this.querySelectorAll(".oidf-test-selector__row");
      const target = /** @type {HTMLElement | undefined} */ (rows[this._focusedRowIndex]);
      if (target) target.focus();
    }
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
                (plan, index) => html`
                  <button
                    type="button"
                    role="listitem"
                    class=${classMap({
                      "oidf-test-selector__row": true,
                      "is-active": this.selected === plan.planName,
                    })}
                    data-plan-name="${plan.planName}"
                    data-index="${index}"
                    tabindex="${this._focusedRowIndex === index ? 0 : -1}"
                    @click=${this._handleRowClick}
                    @keydown=${this._handleRowKeydown}
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
