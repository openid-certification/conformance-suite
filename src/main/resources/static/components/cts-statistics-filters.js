import { LitElement, html, nothing, css } from "lit";
import "./cts-button.js";
import {
  RANGE_PRESETS,
  isFiltered,
  memoiseByArgs,
  rangePreset,
  visibleVariants,
} from "./statistics-model.js";

/** @typedef {import("./statistics-model.js").FilterState} FilterState */
/** @typedef {import("./statistics-model.js").FilterOptions} FilterOptions */

/**
 * The two range groups, in display order. Weekly and monthly are separate
 * `role="group"`s rather than one long strip because picking one of them is
 * picking a granularity, not just a length: "52 weeks" and "12 months" cover
 * nearly the same span and answer different questions.
 * @type {Array<{group: string, label: string, testid: string}>}
 */
const RANGE_GROUPS = [
  { group: "weekly", label: "Weekly range", testid: "stats-range-weekly" },
  { group: "monthly", label: "Monthly range", testid: "stats-range-monthly" },
];

/** Option counts are small; group them anyway so 1,024 does not read as 1024. */
const NUMBER_FORMAT = new Intl.NumberFormat();

/**
 * Render a boolean as an ARIA state string, which only accepts the literal
 * `"true"` / `"false"`.
 * @param {boolean} value - The state.
 * @returns {"true"|"false"} The attribute value.
 */
function aria(value) {
  return value ? "true" : "false";
}

const STYLE_ID = "cts-statistics-filters-styles";

const STYLE_TEXT = css`
  /* One filter row above everything it scopes — never per chart. */
  cts-statistics-filters {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    margin: var(--space-4) 0;
  }
  /* The two granularity groups read as one control cluster, with a visible
     gap between them so they are not mistaken for one six-button strip. */
  .cts-stats-ranges {
    display: inline-flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }
  /* No overflow:hidden: --focus-ring is drawn OUTSIDE the element it is on,
     so clipping the strip would clip the ring off the first and last preset -
     the two a keyboard reaches first. The end buttons round their own outer
     corners instead. */
  .cts-stats-range {
    display: inline-flex;
    border: 1px solid var(--ink-300);
    border-radius: var(--radius-2);
  }
  .cts-stats-range button {
    padding: 0 var(--space-3);
    height: var(--control-height);
    border: 0;
    border-right: 1px solid var(--ink-300);
    background: var(--bg-elev);
    color: var(--fg);
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    line-height: 1;
    cursor: pointer;
  }
  .cts-stats-range button:first-child {
    border-start-start-radius: calc(var(--radius-2) - 1px);
    border-end-start-radius: calc(var(--radius-2) - 1px);
  }
  .cts-stats-range button:last-child {
    border-right: 0;
    border-start-end-radius: calc(var(--radius-2) - 1px);
    border-end-end-radius: calc(var(--radius-2) - 1px);
  }
  .cts-stats-range button:hover {
    background: var(--bg-muted);
  }
  .cts-stats-range button[aria-pressed="true"] {
    background: var(--ink-900);
    color: var(--ink-0);
  }
  .cts-stats-range button:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
    /* The ring overlaps the neighbouring buttons, which would otherwise paint
       their own backgrounds over the half of it that is on their side. */
    position: relative;
    z-index: 1;
  }

  /* Mirrors cts-form-field's .oidf-select, which is scoped to
     .oidf-form-field and so does not reach a bare select on this page. */
  cts-statistics-filters .oidf-select {
    max-width: 22ch;
    height: var(--control-height);
    padding: 0 36px 0 var(--space-3);
    border: 1px solid var(--ink-300);
    border-radius: var(--radius-2);
    background-color: var(--bg-elev);
    color: var(--fg);
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    line-height: 1;
    appearance: none;
    -webkit-appearance: none;
    background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 16 16'><path fill='none' stroke='%2371695E' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' d='M4 6l4 4 4-4'/></svg>");
    background-repeat: no-repeat;
    background-position: right 12px center;
  }
  cts-statistics-filters .oidf-select:focus {
    outline: none;
    border-color: var(--orange-400);
    box-shadow: var(--focus-ring);
  }
`;

/**
 * Append the scoped stylesheet to `<head>` once per page lifetime.
 * @returns {void}
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/**
 * The statistics page's filter row: range presets, then the cascade of spec
 * family → test plan → plan-level variant → certification profile.
 *
 * A controlled component — it holds no state of its own. Every control emits
 * the WHOLE next {@link FilterState} on `cts-filters-change` and waits to be
 * given it back, so the page (which owns the URL and the fetch) is the only
 * place a filter can change.
 *
 * Three rules make the cascade behave rather than merely narrow:
 *
 * - picking a plan implies its family, so the family select follows;
 * - picking a family clears the plan, the certification profile and every
 *   variant, because all three are family-specific: a plan belongs to one
 *   family, profiles are awarded per family, and the variant parameters are
 *   the ones that family's plans declare. Carrying them over would leave the
 *   user two clicks from `family=OpenID Connect Core &
 *   cert=Brazil Open Finance | FAPI-CIBA` — five charts of zeros with nothing
 *   on screen to explain why;
 * - picking a plan clears the certification profile too, and drops any
 *   variant whose parameter the current selection does not even offer;
 * - the variant selects appear only once a family or a plan narrows the view
 *   (see {@link visibleVariants}) — unfiltered, the suite has getting on for
 *   forty plan-level variant parameters, and a row of forty selects is not a
 *   filter row.
 *
 * Light DOM (`createRenderRoot()` returns `this`) so the page's design-system
 * tokens and stylesheet reach the rendered markup.
 * @property {string} range - Selected range preset, one of `RANGE_PRESETS`.
 * @property {string} family - Selected spec family, `""` for all.
 * @property {string} plan - Selected plan name, `""` for all.
 * @property {string} cert - Selected certification profile, `""` for all.
 * @property {Record<string, string>} variant - Plan-level variant parameter → value.
 * @property {Array<string>} families - Families to offer, from the unfiltered payload.
 * @property {FilterOptions} options - Plan / variant / certification options to offer.
 * @fires cts-filters-change - The whole next filter state, as `detail`.
 */
class CtsStatisticsFilters extends LitElement {
  static properties = {
    range: { type: String },
    family: { type: String },
    plan: { type: String },
    cert: { type: String },
    variant: { attribute: false },
    families: { attribute: false },
    options: { attribute: false },
  };

  constructor() {
    super();
    /** @type {string} */
    this.range = "12m";
    /** @type {string} */
    this.family = "";
    /** @type {string} */
    this.plan = "";
    /** @type {string} */
    this.cert = "";
    /** @type {Record<string, string>} */
    this.variant = {};
    /** @type {Array<string>} */
    this.families = [];
    /** @type {FilterOptions} */
    this.options = { plans: [], variants: {}, certProfiles: [] };
    // Rebuilt only when the options or the narrowing change, so a re-render
    // for the busy flag does not walk forty variant parameters again.
    this._visibleVariants = memoiseByArgs(
      (
        /** @type {FilterOptions} */ options,
        /** @type {Record<string, string>} */ variant,
        /** @type {string} */ family,
        /** @type {string} */ plan,
      ) => visibleVariants(options, { range: "", family, plan, variant, cert: "" }),
    );
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  /**
   * Push the selected value into each `<select>` AFTER its options have been
   * rendered. Lit commits an element's own bindings before its children, so a
   * `.value` binding on the select itself would be assigned while the old
   * (or no) options are still in the DOM and would silently be dropped —
   * exactly what a deep link into a filtered view would hit.
   * @returns {void}
   */
  updated() {
    const selects = /** @type {Array<HTMLSelectElement>} */ (
      Array.from(this.querySelectorAll("select[data-value]"))
    );
    for (const select of selects) {
      const wanted = select.dataset.value || "";
      if (select.value !== wanted) select.value = wanted;
    }
  }

  /** @returns {FilterState} The state as the page last set it. */
  get _state() {
    return {
      range: this.range,
      family: this.family,
      plan: this.plan,
      variant: { ...(this.variant || {}) },
      cert: this.cert,
    };
  }

  /**
   * @param {Partial<FilterState>} change - What the user just changed.
   * @returns {void}
   */
  _emit(change) {
    this.dispatchEvent(
      new CustomEvent("cts-filters-change", {
        bubbles: true,
        composed: true,
        detail: { ...this._state, ...change },
      }),
    );
  }

  /**
   * @param {Event} event - Click on a range preset button.
   * @returns {void}
   */
  _handleRange(event) {
    const button = /** @type {HTMLElement} */ (event.currentTarget);
    const range = button.dataset.range;
    if (range) this._emit({ range });
  }

  /**
   * @param {Event} event - Change on the family select.
   * @returns {void}
   */
  _handleFamily(event) {
    const family = /** @type {HTMLSelectElement} */ (event.currentTarget).value;
    // The plan, the certification profile and the variant parameters all
    // belong to the family that was selected before, so carrying any of them
    // over would leave the controls contradicting each other and the charts
    // at zero with no explanation.
    this._emit({ family, plan: "", variant: {}, cert: "" });
  }

  /**
   * @param {Event} event - Change on the plan select.
   * @returns {void}
   */
  _handlePlan(event) {
    const plan = /** @type {HTMLSelectElement} */ (event.currentTarget).value;
    const chosen = this._plans().find((option) => option.planName === plan);
    // A plan IS a family, so selecting one says which family is on screen;
    // clearing it leaves the family filter exactly as it was. The
    // certification profile goes either way — profiles are per plan, so the
    // one on screen is almost certainly not this plan's.
    this._emit({
      plan,
      family: plan && chosen && chosen.family ? chosen.family : this.family,
      variant: this._applicableVariants(),
      cert: "",
    });
  }

  /**
   * The variant filters worth carrying across a change of plan: the ones
   * whose parameter the current selection actually offers. A parameter the
   * new plan does not declare can only match nothing.
   *
   * "Offers" is the PREVIOUS selection's dimensions - the options the page was
   * handed for the view being left, not the one being moved to, which nobody
   * has fetched yet. It is therefore a prune of the obviously impossible
   * rather than a guarantee: a parameter the old view offered and the new plan
   * does not is carried over once, and the refetch that follows is what
   * settles it.
   *
   * When nothing is known yet (no payload, so no dimensions) the filters are
   * kept as they are rather than silently dropped — the server will say what
   * matches.
   * @returns {Record<string, string>} The variant filters to keep.
   */
  _applicableVariants() {
    const variant = { ...(this.variant || {}) };
    const offered = (this.options && this.options.variants) || {};
    const names = Object.keys(offered);
    if (names.length === 0) return variant;
    for (const name of Object.keys(variant)) {
      if (!names.includes(name)) delete variant[name];
    }
    return variant;
  }

  /**
   * @param {Event} event - Change on one of the variant selects.
   * @returns {void}
   */
  _handleVariant(event) {
    const select = /** @type {HTMLSelectElement} */ (event.currentTarget);
    const name = select.dataset.variant || "";
    const variant = { ...(this.variant || {}) };
    if (select.value) {
      variant[name] = select.value;
    } else {
      delete variant[name];
    }
    this._emit({ variant });
  }

  /**
   * @param {Event} event - Change on the certification profile select.
   * @returns {void}
   */
  _handleCert(event) {
    this._emit({ cert: /** @type {HTMLSelectElement} */ (event.currentTarget).value });
  }

  /**
   * Clear every filter but keep the range: the range is a viewing choice, not
   * a filter, and resetting it too would move the charts under the user.
   * @returns {void}
   */
  _handleClear() {
    this._emit({ family: "", plan: "", variant: {}, cert: "" });
  }

  /**
   * The plan options, alphabetically. The payload ranks them busiest first,
   * which is the order a chart wants; a select is scanned by name.
   * @returns {Array<{planName: string, family: string, runs: number, plans: number}>} Plan options.
   */
  _plans() {
    const plans = (this.options && this.options.plans) || [];
    if (!Array.isArray(plans)) return [];
    return [...plans].sort((a, b) => String(a.planName).localeCompare(String(b.planName)));
  }

  render() {
    const variants = this._visibleVariants(this.options, this.variant, this.family, this.plan);
    const certProfiles = (this.options && this.options.certProfiles) || [];
    return html`
      <div class="cts-stats-ranges" data-testid="stats-range">
        ${RANGE_GROUPS.map((group) => this._renderRangeGroup(group))}
      </div>
      ${this._renderFamily()} ${this._renderPlan()}
      ${Object.keys(variants).map((name) => this._renderVariant(name, variants[name]))}
      ${this._renderCert(Array.isArray(certProfiles) ? certProfiles : [])}
      ${isFiltered(this._state)
        ? html`<cts-button
            variant="secondary"
            label="Clear filters"
            data-testid="stats-clear-filters"
            @cts-click=${this._handleClear}
          ></cts-button>`
        : nothing}
    `;
  }

  /**
   * @param {{group: string, label: string, testid: string}} group - One range group.
   * @returns {unknown} Its button strip.
   */
  _renderRangeGroup(group) {
    const selected = rangePreset(this.range).value;
    return html`
      <div
        class="cts-stats-range"
        role="group"
        aria-label=${group.label}
        data-testid=${group.testid}
      >
        ${RANGE_PRESETS.filter((preset) => preset.group === group.group).map(
          (preset) => html`
            <button
              type="button"
              data-range=${preset.value}
              aria-pressed=${aria(selected === preset.value)}
              @click=${this._handleRange}
            >
              ${preset.label}
            </button>
          `,
        )}
      </div>
    `;
  }

  /** @returns {unknown} The spec family select. */
  _renderFamily() {
    const families = Array.isArray(this.families) ? this.families : [];
    const offered =
      families.includes(this.family) || !this.family ? families : [...families, this.family];
    return html`
      <select
        class="oidf-select"
        aria-label="Spec family"
        data-testid="stats-family"
        data-value=${this.family}
        @change=${this._handleFamily}
      >
        <option value="">All families</option>
        ${offered.map((name) => html`<option value=${name}>${name}</option>`)}
      </select>
    `;
  }

  /** @returns {unknown} The test plan select, or nothing when there is nothing to pick. */
  _renderPlan() {
    const plans = this._plans();
    const known = plans.some((option) => option.planName === this.plan);
    // A deep link may name a plan the current range has no data for; it must
    // still be selectable, or the user cannot see what they are looking at.
    const offered = this.plan && !known ? [{ planName: this.plan, family: "" }, ...plans] : plans;
    if (offered.length === 0) return nothing;
    return html`
      <select
        class="oidf-select"
        aria-label="Test plan"
        data-testid="stats-plan"
        data-value=${this.plan}
        @change=${this._handlePlan}
      >
        <option value="">All plans</option>
        ${offered.map(
          (option) => html`<option value=${option.planName}>${option.planName}</option>`,
        )}
      </select>
    `;
  }

  /**
   * One select per plan-level variant parameter the current slice knows
   * about. The "Any" option carries the parameter name so the collapsed
   * control says what it filters even before anything is picked.
   * @param {string} name - The variant parameter name.
   * @param {Array<{value: string, users: number, plans: number}>} values - Its values.
   * @returns {unknown} The select, or nothing when the parameter has no values.
   */
  _renderVariant(name, values) {
    const options = Array.isArray(values) ? values : [];
    const selected = (this.variant || {})[name] || "";
    const known = options.some((option) => option.value === selected);
    const offered =
      selected && !known ? [{ value: selected, users: 0, plans: 0 }, ...options] : options;
    if (offered.length === 0) return nothing;
    return html`
      <select
        class="oidf-select"
        aria-label="Variant: ${name}"
        data-testid="stats-variant-${name}"
        data-variant=${name}
        data-value=${selected}
        @change=${this._handleVariant}
      >
        <option value="">Any ${name}</option>
        ${offered.map(
          (option) =>
            html`<option value=${option.value}>
              ${option.value} (${NUMBER_FORMAT.format(Number(option.users) || 0)})
            </option>`,
        )}
      </select>
    `;
  }

  /**
   * @param {Array<{name: string, users: number, plans: number}>} profiles - Certification profiles.
   * @returns {unknown} The select, or nothing when nothing has been certified.
   */
  _renderCert(profiles) {
    const known = profiles.some((profile) => profile.name === this.cert);
    const offered =
      this.cert && !known ? [{ name: this.cert, users: 0, plans: 0 }, ...profiles] : profiles;
    if (offered.length === 0) return nothing;
    return html`
      <select
        class="oidf-select"
        aria-label="Certification profile"
        data-testid="stats-cert"
        data-value=${this.cert}
        @change=${this._handleCert}
      >
        <option value="">All profiles</option>
        ${offered.map(
          (profile) =>
            html`<option value=${profile.name}>
              ${profile.name} (${NUMBER_FORMAT.format(Number(profile.users) || 0)})
            </option>`,
        )}
      </select>
    `;
  }
}

customElements.define("cts-statistics-filters", CtsStatisticsFilters);

export {};
