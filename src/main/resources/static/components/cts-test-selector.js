import { LitElement, html, nothing, css } from "lit";
import { classMap } from "lit/directives/class-map.js";
import { formatSummaryPreview } from "./format-description.js";
import "./cts-icon.js";
import "./cts-tooltip.js";
import "./cts-loading-state.js";

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
 * @property {boolean} loading - When set, the list area shows a shared
 *   `<cts-loading-state>` spinner instead of rows or the empty message. The
 *   caller (schedule-test.html) sets it while `/api/plan/available` is in
 *   flight and clears it once `plans` is populated, so the page chrome stays
 *   visible instead of being hidden behind a full-page modal.
 * @property {Array} favourites - Controlled list of favourited `planName`s.
 *   The component renders these as starred/pinned but never mutates the
 *   array itself — exactly like `selected`. The caller owns persistence:
 *   it listens for `cts-favourite-toggle`, updates the array optimistically,
 *   and reverts on a backend failure. In the prototype the caller is a
 *   Storybook adapter backed by `localStorage`; in production it becomes
 *   `/api/favourite-plans`. The component template is identical either way.
 * @property {boolean} favouritesLoading - When set, the favourites surface
 *   (the V1 group region, the V2 view count, etc.) shows a skeleton instead
 *   of stars while the caller's first favourites fetch is in flight. Distinct
 *   from `loading`, which governs the whole plan list.
 * @property {boolean} canFavourite - Whether the current principal may save
 *   favourites. Defaults to `true`. When `false` (anonymous / private-link
 *   users, who have no server-side principal to key a favourite on) the star
 *   controls render disabled with an explanatory tooltip rather than
 *   vanishing, so the affordance is discoverable.
 * @property {string} favouritesLayout - Selects where favourites surface,
 *   reflected to the `favourites-layout` attribute so a story (or
 *   schedule-test.html) can set it declaratively. One of `'group'` (pinned
 *   "★ Favourites" section atop the list), `'view'` (a saved-view entry in
 *   the family listbox), `'chip'` (a "Favourites only" filter toggle), or
 *   `''`/absent (today's plain list — full back-compat).
 * @fires cts-plan-select - When a list item is selected, with
 *   `{ detail: { plan, via } }` where `via` is `'click'` or `'keyboard'`;
 *   bubbles.
 * @fires cts-favourite-toggle - When a plan's star is toggled, with
 *   `{ detail: { plan, favourite, via } }` where `plan` is the `planName`,
 *   `favourite` is the requested next state (`true` = add, `false` = remove),
 *   and `via` is `'click'` or `'keyboard'`; bubbles. The component does not
 *   update `favourites` in response — the caller does (optimistically), which
 *   is what lets the same event drive both the localStorage fake and the
 *   future `/api/favourite-plans` wiring.
 */

const STYLE_ID = "cts-test-selector-styles";

const STYLE_TEXT = css`
  .oidf-test-selector {
    display: grid;
    /* Left rail (search + family listbox) sits beside the plan list. The
     1fr column gets minmax(0,…) so a long plan name can't blow the grid
     wider than its container. */
    grid-template-columns: minmax(200px, 280px) minmax(0, 1fr);
    gap: var(--space-4);
    align-items: start;
    /* 32px gap to the spec cascade below — gives the scroll-in highlight
     (which overhangs the cascade + variant group by 16px) room to breathe
     without colliding with the selector. See cts-flash-highlight. */
    margin-bottom: var(--space-8);
  }
  @media (max-width: 768px) {
    .oidf-test-selector {
      grid-template-columns: 1fr;
    }
  }
  .oidf-test-selector__rail {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    min-width: 0;
  }
  .oidf-test-selector__search-wrap,
  .oidf-test-selector__family {
    width: 100%;
    box-sizing: border-box;
    border: 1px solid var(--ink-300);
    border-radius: var(--radius-2);
    background: var(--bg-elev);
    color: var(--fg);
    font-family: var(--font-sans);
    font-size: var(--fs-13);
  }
  .oidf-test-selector__family {
    /* Rendered as a listbox (size attribute on the element), not a
     dropdown — so the height is driven by the visible row count, there
     is no chevron indicator, and the native single-line clipping is
     replaced by wrapping option rows (see the option rules below). */
    appearance: none;
    -webkit-appearance: none;
    padding: var(--space-1);
    line-height: var(--lh-snug);
    overflow-y: auto;
    cursor: pointer;
  }
  .oidf-test-selector__family option {
    /* Long spec-family names wrap onto multiple lines instead of being
     clipped to one row (the native listbox default). */
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-1);
    white-space: normal;
    text-wrap: pretty;
    line-height: var(--lh-snug);
  }
  .oidf-test-selector__family option:checked {
    /* Under appearance:none the OS still paints the selected row via
     background-color, which a plain background-color cannot override. A
     background-image (a flat gradient) layers on top and wins, so the
     active family reads in the design-system orange rather than the
     browser's blue/grey system highlight — matching the .is-active row. */
    background: var(--orange-50) linear-gradient(0deg, var(--orange-50), var(--orange-50));
    color: var(--fg);
    font-weight: var(--fw-medium);
  }
  .oidf-test-selector__search-wrap {
    height: var(--control-height);
    display: flex;
    align-items: center;
    gap: var(--space-2);
    padding: 0 var(--space-2);
    line-height: var(--lh-base);
    transition:
      border-color var(--dur-1) var(--ease-standard),
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
    transition:
      background var(--dur-1) var(--ease-standard),
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
  /* Row container. The single-button row was split into a select button +
     a sibling favourite button (button-in-button is invalid and fails
     a11y), so the divider/background that used to live on the row now live
     on this flex wrapper. role="listitem" sits here; the inner controls are
     plain buttons. */
  .oidf-test-selector__item {
    display: flex;
    align-items: stretch;
    border-top: 1px solid var(--divider);
    background: var(--bg-elev);
  }
  .oidf-test-selector__item:first-child {
    border-top: none;
  }
  /* The whole item follows the select button's hover/active state so the
     star strip never reads as a separate, lighter column. :has() keeps the
     background rules single-sourced on the row (where the
     RowHoverStyleRegistered story still asserts them) instead of
     duplicating them here. */
  .oidf-test-selector__item:has(.oidf-test-selector__row:hover) {
    background: var(--ink-50);
  }
  .oidf-test-selector__item:has(.oidf-test-selector__row.is-active) {
    background: var(--orange-50);
  }
  .oidf-test-selector__row {
    flex: 1 1 auto;
    min-width: 0;
    text-align: left;
    padding: var(--space-3) var(--space-4);
    border: none;
    /* Transparent so the item container's background (and its hover/active
       variants) shows through; the row's own hover/active rules below layer
       the same token on top, so the column reads uniformly either way. */
    background: transparent;
    color: var(--fg);
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    line-height: var(--lh-base);
    cursor: pointer;
    transition: background var(--dur-1) var(--ease-standard);
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
  /* Secondary favourite toggle, sibling of the select button. A roving
     tabindex (mirroring the select button's) keeps Tab from cycling every
     star; the focused row exposes both its select and star as tab stops,
     and the "f" shortcut toggles the focused row's star without leaving the
     keyboard roving model. */
  .oidf-test-selector__fav {
    flex: 0 0 auto;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    padding: 0;
    border: none;
    border-left: 1px solid var(--divider);
    background: transparent;
    color: var(--fg-faint);
    cursor: pointer;
    transition:
      background var(--dur-1) var(--ease-standard),
      color var(--dur-1) var(--ease-standard);
  }
  .oidf-test-selector__fav:hover {
    background: var(--ink-100);
    color: var(--fg-soft);
  }
  .oidf-test-selector__fav:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
    position: relative;
    z-index: 1;
  }
  .oidf-test-selector__fav.is-favourited {
    color: var(--orange-500);
  }
  .oidf-test-selector__fav[aria-disabled="true"] {
    color: var(--fg-faint);
    cursor: not-allowed;
  }
  .oidf-test-selector__fav[aria-disabled="true"]:hover {
    background: transparent;
    color: var(--fg-faint);
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
    /* mirrors .t-meta except it doesn't wrap */
    font-family: var(--font-sans);
    font-size: var(--fs-12);
    line-height: var(--lh-snug);
    color: var(--fg-soft);
    font-weight: var(--fw-regular);
    white-space: nowrap;
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
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

class CtsTestSelector extends LitElement {
  static properties = {
    plans: { type: Array },
    selected: { type: String },
    // Reflected so the boot attribute (`<cts-test-selector loading>`) is
    // cleared from the DOM when the page sets `loading = false` after the
    // plans fetch — otherwise the stale attribute lingers on the element.
    loading: { type: Boolean, reflect: true },
    favourites: { type: Array },
    favouritesLoading: { type: Boolean, attribute: "favourites-loading" },
    canFavourite: { type: Boolean, attribute: "can-favourite" },
    // Reflected so a declarative `<cts-test-selector favourites-layout="group">`
    // round-trips and the active variant is inspectable in the DOM.
    favouritesLayout: {
      type: String,
      reflect: true,
      attribute: "favourites-layout",
    },
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
    this.loading = false;
    this.favourites = [];
    this.favouritesLoading = false;
    this.canFavourite = true;
    this.favouritesLayout = "";
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
    if ((key === "f" || key === "F") && this._favouritesActive && this.canFavourite) {
      // Focused-row shortcut: toggle this row's favourite without leaving the
      // roving model or reaching for the star with Tab. The star is also a
      // real tab stop on the focused row, so both affordances coexist.
      e.preventDefault();
      const plan = this._filteredPlans[index];
      if (plan) {
        this._emitFavouriteToggle(plan.planName, !this._isFavourite(plan.planName), "keyboard");
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
      new CustomEvent("cts-plan-select", {
        bubbles: true,
        detail: { plan, via },
      }),
    );
  }

  // True when a favourites layout is selected. The star controls and the
  // layout-specific surfaces (group / view / chip) only render when this is
  // on; absent / "off" keeps the plain back-compat list.
  get _favouritesActive() {
    return (
      this.favouritesLayout === "group" ||
      this.favouritesLayout === "view" ||
      this.favouritesLayout === "chip"
    );
  }

  _isFavourite(planName) {
    return this.favourites.includes(planName);
  }

  // Announce the user's intent to toggle a favourite. Crucially this does NOT
  // touch `this.favourites` — the caller owns that array and updates it
  // optimistically, so the same event drives the localStorage fake (prototype)
  // and `/api/favourite-plans` (production) with no template change.
  _emitFavouriteToggle(planName, favourite, via) {
    this.dispatchEvent(
      new CustomEvent("cts-favourite-toggle", {
        bubbles: true,
        detail: { plan: planName, favourite, via },
      }),
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
        <div class="oidf-test-selector__rail">
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
            size="14"
            aria-label="Filter test plans by specification family"
            @change=${this._handleFamilyFilter}
          >
            <option value="" ?selected=${this._selectedFamily === ""}> All specifications </option>
            ${this._renderFamilyOptions()}
          </select>
        </div>
        <div class="oidf-test-selector__list" role="list">
          ${this._filteredPlans.length > 0
            ? this._filteredPlans.map((plan, index) => this._renderRow(plan, index))
            : this.loading
              ? html`<cts-loading-state label="Loading test plans"></cts-loading-state>`
              : html`<div class="oidf-test-selector__empty"> No plans match your search </div>`}
        </div>
      </div>
    `;
  }

  // Render one plan row: a role="listitem" container holding the primary
  // select <button> (unchanged class/data/handlers/roving tabindex so the
  // existing stories and keyboard model keep working) and, when a favourites
  // layout is active, a sibling favourite <button>. Extracted from render()
  // so the V1 group sublist (U3) can reuse the same row markup.
  _renderRow(plan, index) {
    const favTabindex = this._focusedRowIndex === index ? 0 : -1;
    return html`
      <div class="oidf-test-selector__item" role="listitem">
        <button
          type="button"
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
                ? html`<span class="oidf-test-selector__row-family">${plan.specFamily}</span>`
                : nothing}
            </span>
            ${plan.modules
              ? html`<cts-tooltip content="Number of test modules in this plan" placement="left"
                  ><span
                    class="oidf-test-selector__row-count"
                    aria-label="${plan.modules.length} test ${plan.modules.length === 1
                      ? "module"
                      : "modules"}"
                    >${plan.modules.length}</span
                  ></cts-tooltip
                >`
              : nothing}
          </span>
          ${plan.summary
            ? html`<span class="oidf-test-selector__row-summary"
                >${formatSummaryPreview(plan.summary)}</span
              >`
            : nothing}
        </button>
        ${this._favouritesActive ? this._renderFavouriteButton(plan, favTabindex) : nothing}
      </div>
    `;
  }

  // The per-row star. aria-pressed is the source of truth for the favourited
  // state (the filled/outline icon swap is the matching visual cue). The
  // tabindex roves in lockstep with its row's select button, so Tab visits
  // "select then star" on the focused row and skips the rest of the list.
  // When the principal can't favourite, the star renders as an aria-disabled,
  // no-op affordance with an explanatory tooltip rather than disappearing.
  _renderFavouriteButton(plan, tabindex) {
    const planName = plan.planName;
    const name = plan.displayName || planName;
    if (!this.canFavourite) {
      return html`<cts-tooltip content="Sign in to save favourites" placement="left"
        ><button
          type="button"
          class="oidf-test-selector__fav"
          aria-disabled="true"
          aria-label="Sign in to save favourites: ${name}"
          tabindex="${tabindex}"
        >
          <cts-icon name="star" size="20"></cts-icon></button
      ></cts-tooltip>`;
    }
    const fav = this._isFavourite(planName);
    return html`<button
      type="button"
      class=${classMap({
        "oidf-test-selector__fav": true,
        "is-favourited": fav,
      })}
      aria-pressed="${fav ? "true" : "false"}"
      aria-label="${fav ? "Remove favourite" : "Add favourite"}: ${name}"
      data-plan-name="${planName}"
      tabindex="${tabindex}"
      @click=${this._handleFavouriteClick}
    >
      <cts-icon name="${fav ? "star-fill" : "star"}" size="20"></cts-icon>
    </button>`;
  }

  _handleFavouriteClick(e) {
    const planName = /** @type {HTMLElement} */ (e.currentTarget).dataset.planName;
    if (!planName) return;
    // Request the opposite of the current state; the caller flips the prop.
    this._emitFavouriteToggle(planName, !this._isFavourite(planName), "click");
  }

  _renderFamilyOptions() {
    return this._families.map(
      (f) => html`<option value="${f}" ?selected=${this._selectedFamily === f}> ${f} </option>`,
    );
  }
}
customElements.define("cts-test-selector", CtsTestSelector);

export {};
