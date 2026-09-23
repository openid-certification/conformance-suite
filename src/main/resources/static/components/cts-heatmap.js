import { LitElement, html, nothing, css } from "lit";
import { injectDataTableStyles } from "./data-table-styles.js";
import { heatmapIntensity, heatmapMax, heatmapScaleSteps, heatmapTotal } from "./heatmap-model.js";
import { NUMBER_FORMAT } from "./number-format.js";

/**
 * The sequential hue every cell is mixed from. One hue, light → dark: a
 * heatmap encodes MAGNITUDE, and a multi-hue ramp would invent categories
 * that are not in the data. It is deliberately the same token slot 1 of the
 * categorical palette uses — the page never shows both at once for the same
 * thing, and reusing it keeps the dashboard to one blue.
 */
const RAMP_HUE_VAR = "--chart-cat-1";

const STYLE_ID = "cts-heatmap-styles";

const STYLE_TEXT = css`
  cts-heatmap {
    display: block;
  }
  .cts-heatmap {
    margin: 0;
  }
  .cts-heatmap-heading {
    margin: 0 0 var(--space-2, 8px);
    font-family: var(--font-sans);
    font-size: var(--fs-16, 16px);
    font-weight: var(--fw-bold, 700);
    line-height: var(--lh-snug, 1.3);
    color: var(--fg);
  }
  .cts-heatmap-caption {
    margin: 0 0 var(--space-3, 12px);
    font-size: var(--fs-12, 12px);
    color: var(--fg-soft);
  }
  /* 24 hourly columns do not fit a phone, and squeezing them below a few
     pixels each would destroy the pattern the chart exists to show — so the
     grid scrolls inside its own box rather than shrinking or pushing the
     page sideways. */
  .cts-heatmap-scroll {
    overflow-x: auto;
    scrollbar-width: thin;
    padding-bottom: var(--space-1, 4px);
  }
  /* The box is a tab stop (arrow keys scroll it), so it shows where focus is. */
  .cts-heatmap-scroll:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
    border-radius: var(--radius-2, 4px);
  }
  /* The scroll box's positioned parent: an absolutely positioned fade inside
     the scroll box itself would scroll away with the cells. The fade says
     "there is more to the right" and goes once the reader has reached the
     end (data-scroll-end, kept by the component), or when nothing overflows. */
  .cts-heatmap-scroller {
    position: relative;
  }
  .cts-heatmap-scroller::after {
    content: "";
    position: absolute;
    inset: 0 0 var(--space-1, 4px) auto;
    width: 32px;
    background: linear-gradient(to right, transparent, var(--bg-elev));
    pointer-events: none;
    transition: opacity var(--dur-1, 120ms) var(--ease-standard, ease);
  }
  .cts-heatmap-scroller[data-scroll-end]::after {
    opacity: 0;
  }
  @media (prefers-reduced-motion: reduce) {
    .cts-heatmap-scroller::after {
      transition: none;
    }
  }
  .cts-heatmap-hint {
    margin: var(--space-1, 4px) 0 0;
    font-size: var(--fs-12, 12px);
    color: var(--fg-soft);
  }
  .cts-heatmap-grid {
    display: grid;
    /* The 2px gap IS the separator between cells — the surface doing the
       work, so no cell needs a border drawn around it. */
    gap: 2px;
    min-width: 480px;
    align-items: center;
  }
  .cts-heatmap-colhead,
  .cts-heatmap-rowhead {
    font-family: var(--font-sans);
    font-size: var(--fs-11, 11px);
    color: var(--fg-soft);
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }
  .cts-heatmap-colhead {
    text-align: center;
  }
  /* Day labels stay put while the hours scroll under them, so a cell in
     view is never separated from its row's name. The corner cell is sticky
     too, so the hour headers pass under it rather than through the gap. */
  .cts-heatmap-rowhead,
  .cts-heatmap-corner {
    position: sticky;
    left: 0;
    z-index: 1;
    /* The label's box fills its whole row, and the shadow paints the 2px
       gaps beside and below it, so no scrolled-under cell shows around it. */
    align-self: stretch;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    background: var(--bg-elev);
    box-shadow: 2px 2px 0 var(--bg-elev);
  }
  .cts-heatmap-rowhead {
    padding-right: var(--space-2, 8px);
    text-align: right;
    white-space: nowrap;
  }
  .cts-heatmap-cell {
    height: 18px;
    border-radius: 2px;
    background: var(--bg-muted);
  }
  .cts-heatmap-scale {
    display: flex;
    align-items: center;
    gap: var(--space-2, 8px);
    margin-top: var(--space-3, 12px);
    font-size: var(--fs-12, 12px);
    color: var(--fg-soft);
    font-variant-numeric: tabular-nums;
  }
  .cts-heatmap-scale-ramp {
    display: inline-flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-1, 4px) var(--space-3, 12px);
  }
  /* Swatch and the value it stands for travel together: on a square-root ramp
     the reader cannot infer the middle steps from the two ends. */
  .cts-heatmap-scale-step {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1, 4px);
  }
  .cts-heatmap-swatch {
    width: 18px;
    height: 10px;
    border-radius: 2px;
  }
  .cts-heatmap-empty {
    margin: 0;
    padding: var(--space-5, 20px) 0;
    font-size: var(--fs-13, 13px);
    color: var(--fg-soft);
  }

  .cts-heatmap-data {
    margin-top: var(--space-3, 12px);
  }
  /* 168 cells: tighter than the shared table so the grid stays one screen. */
  .cts-heatmap-table.cts-data-table th,
  .cts-heatmap-table.cts-data-table td {
    padding: var(--space-1, 4px) var(--space-2, 8px);
  }
`;

/**
 * Append the scoped stylesheet to `<head>` once per page lifetime.
 * @returns {void}
 */
function injectStyles() {
  injectDataTableStyles();
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/** Per-instance id counter, so each `<figure>` can point at its own `<h3>`. */
let headingSeq = 0;

/**
 * A 2-D magnitude grid — the statistics page's day-of-week × hour-of-day
 * activity chart.
 *
 * Color is a SEQUENTIAL ramp: one hue, mixed into the page background in
 * proportion to the cell's value ({@link heatmapIntensity}, square-root
 * scaled — see its documentation for why), so more is darker and nothing but
 * magnitude is encoded. An empty cell keeps the muted surface, which is
 * visibly not "a little bit of traffic".
 *
 * Because the ramp is square-root scaled, the legend labels EVERY swatch with
 * the count it stands for rather than only its two ends: evenly spaced
 * swatches under a "0 … max" label would read as linear and put the middle one
 * at a quarter of the value it actually means.
 *
 * Every value is reachable three ways — the cell's own `title`, the `<details>`
 * data table, and the aria-label's summary — so none of it is gated behind
 * discriminating a fill, which is what a color scale must never do.
 *
 * Light DOM (`createRenderRoot()` returns `this`), so page CSS and the
 * design-system tokens reach the rendered markup directly.
 * @property {Array<string>} rows - Row labels, one per row of `values` (e.g. `["Mon", …]`).
 * @property {Array<string>} cols - Column labels, one per column (e.g. `["00", …]`).
 * @property {Array<Array<number>>} values - `rows.length` rows of `cols.length` counts.
 * @property {string} heading - Chart title; the `<h3>`, the table `<caption>`
 *   and the leading half of the grid's `aria-label`.
 * @property {string} caption - One line under the heading saying what the
 *   grid covers (the time zone, what it is and is not filtered by). The
 *   component prefixes it with the grid's total.
 * @property {string} valueLabel - What one cell counts, plural and lower case
 *   (`"runs"`). Used in the tooltip, the legend and the empty state.
 * @property {(row: number, col: number, value: number) => string} cellTitle -
 *   Optional property-only hook returning one cell's hover text. Defaults to
 *   `"<row> <col> — <n> <valueLabel>"`. Not settable as an attribute.
 */
class CtsHeatmap extends LitElement {
  static properties = {
    rows: { type: Array },
    cols: { type: Array },
    values: { type: Array },
    heading: { type: String },
    caption: { type: String },
    valueLabel: { type: String, attribute: "value-label" },
    cellTitle: { attribute: false },
    _overflows: { state: true },
    _atEnd: { state: true },
  };

  constructor() {
    super();
    /** @type {Array<string>} */
    this.rows = [];
    /** @type {Array<string>} */
    this.cols = [];
    /** @type {Array<Array<number>>} */
    this.values = [];
    /** @type {string} */
    this.heading = "";
    /** @type {string} */
    this.caption = "";
    /** @type {string} */
    this.valueLabel = "runs";
    /** @type {((row: number, col: number, value: number) => string)|undefined} */
    this.cellTitle = undefined;
    /** @type {string} Ties the <figure> to its own <h3>. */
    this._headingId = `cts-heatmap-heading-${++headingSeq}`;
    /** @type {boolean} Whether the grid is wider than its scroll box. */
    this._overflows = false;
    /** @type {boolean} Whether the scroll box is scrolled to its right end. */
    this._atEnd = true;
    /** @type {ResizeObserver | null} */
    this._resizeObserver = null;
    /** @type {HTMLElement | null} The scroll box the observer is watching. */
    this._observedBox = null;
    this._handleScroll = this._handleScroll.bind(this);
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    // A reconnect (the element moved in the DOM) does not re-run updated()
    // on its own, so ask for one to re-attach the resize observer.
    if (this.hasUpdated) this.requestUpdate();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._resizeObserver) {
      this._resizeObserver.disconnect();
      this._resizeObserver = null;
    }
    this._observedBox = null;
  }

  /**
   * Watch the scroll box's size once it exists: the grid is only in the DOM
   * once there is data, and a resize (the phone rotating, a sidebar opening)
   * changes whether it overflows at all.
   * @param {Map<string, unknown>} changed - The properties that changed.
   */
  updated(changed) {
    super.updated(changed);
    const box = this._scrollBox();
    if (!box) {
      if (this._resizeObserver) {
        this._resizeObserver.disconnect();
        this._resizeObserver = null;
      }
      this._observedBox = null;
      return;
    }
    if (!this._resizeObserver && typeof ResizeObserver === "function") {
      this._resizeObserver = new ResizeObserver(() => this._measureScroll());
    }
    // The observer reports once on observe(), which is the first measurement;
    // it arrives outside the update cycle, so it never chains a second render
    // onto this one. Observing the same box again would queue another
    // notification per render, so each box is observed once.
    if (this._resizeObserver) {
      if (this._observedBox !== box) {
        if (this._observedBox) this._resizeObserver.unobserve(this._observedBox);
        this._resizeObserver.observe(box);
        this._observedBox = box;
      }
    } else {
      this._measureScroll();
    }
  }

  /** @returns {HTMLElement | null} The grid's scroll box, once rendered. */
  _scrollBox() {
    return this.querySelector(".cts-heatmap-scroller > .cts-heatmap-scroll");
  }

  _handleScroll() {
    this._measureScroll();
  }

  /**
   * Whether the grid overflows its box and whether the reader has scrolled
   * to its end — the two facts the fade and the hint are drawn from.
   */
  _measureScroll() {
    const box = this._scrollBox();
    if (!box) return;
    const overflows = box.scrollWidth > box.clientWidth + 1;
    // Within a pixel: fractional scroll positions never land exactly on the end.
    const atEnd = !overflows || box.scrollLeft + box.clientWidth >= box.scrollWidth - 1;
    if (overflows !== this._overflows) this._overflows = overflows;
    if (atEnd !== this._atEnd) this._atEnd = atEnd;
  }

  /**
   * One cell's count, tolerating a short or missing row — a truncated payload
   * must render an empty cell, not throw inside `render()`.
   * @param {number} row - Row index.
   * @param {number} col - Column index.
   * @returns {number} The count, or 0.
   */
  _valueAt(row, col) {
    const values = Array.isArray(this.values) ? this.values : [];
    const line = Array.isArray(values[row]) ? values[row] : [];
    return Number(line[col]) || 0;
  }

  /**
   * @param {number} row - Row index.
   * @param {number} col - Column index.
   * @param {number} value - Its count.
   * @returns {string} The cell's hover text.
   */
  _titleAt(row, col, value) {
    if (typeof this.cellTitle === "function") return this.cellTitle(row, col, value);
    const rowLabel = (this.rows || [])[row] ?? "";
    const colLabel = (this.cols || [])[col] ?? "";
    return `${rowLabel} ${colLabel} — ${NUMBER_FORMAT.format(value)} ${this.valueLabel}`;
  }

  /**
   * The busiest cell, for the summary an assistive technology hears instead
   * of the grid. "When is the suite busiest" is the question this chart
   * answers, so the answer belongs in its label.
   * @param {number} max - The largest value in the grid.
   * @returns {string} e.g. `"busiest Tue 14 with 123 runs"`, or `""`.
   */
  _peakSummary(max) {
    if (max <= 0) return "";
    const rows = this.rows || [];
    const cols = this.cols || [];
    for (let row = 0; row < rows.length; row += 1) {
      for (let col = 0; col < cols.length; col += 1) {
        if (this._valueAt(row, col) === max) {
          return `busiest ${rows[row]} ${cols[col]} with ${NUMBER_FORMAT.format(max)} ${this.valueLabel}`;
        }
      }
    }
    return "";
  }

  render() {
    const rows = this.rows || [];
    const cols = this.cols || [];
    const max = heatmapMax(this.values);
    const total = heatmapTotal(this.values);
    // How much the grid is counting altogether: without it the reader can see
    // the shape of the week but not whether it is made of hundreds of runs or
    // hundreds of thousands. Suppressed when there are none, because the empty
    // state below says that better.
    const caption = [
      total > 0 ? `${NUMBER_FORMAT.format(total)} ${this.valueLabel} in this range.` : "",
      this.caption,
    ]
      .filter(Boolean)
      .join(" ");

    return html`
      <figure class="cts-heatmap" aria-labelledby=${this._headingId}>
        <h3 class="cts-heatmap-heading" id=${this._headingId}>${this.heading}</h3>
        ${caption ? html`<p class="cts-heatmap-caption">${caption}</p>` : nothing}
        ${max === 0
          ? html`<p class="cts-heatmap-empty" data-testid="cts-heatmap-empty">
              No ${this.valueLabel} were recorded in this range.
            </p>`
          : html`
              ${this._renderGrid(rows, cols, max)} ${this._renderScale(max)}
              ${this._renderTable(rows, cols)}
            `}
      </figure>
    `;
  }

  /**
   * The grid itself. It is one `role="img"` rather than 168 announced boxes:
   * a screen reader walking every cell is unusable, and the data table below
   * is the same numbers in a form that is meant to be read linearly. Its
   * scroll box is a tab stop, so a keyboard can reach the hours a narrow
   * screen hides.
   * @param {Array<string>} rows - Row labels.
   * @param {Array<string>} cols - Column labels.
   * @param {number} max - The largest value, for the ramp.
   * @returns {unknown} The grid.
   */
  _renderGrid(rows, cols, max) {
    const peak = this._peakSummary(max);
    const label =
      `${this.heading}: ${rows.length} rows by ${cols.length} columns` +
      `${peak ? `, ${peak}` : ""}. Data table available below.`;
    return html`
      <div class="cts-heatmap-scroller" data-scroll-end=${this._atEnd ? "" : nothing}>
        <div
          class="cts-heatmap-scroll"
          role="group"
          aria-label="${this.heading} grid"
          tabindex="0"
          @scroll=${this._handleScroll}
        >
          <div
            class="cts-heatmap-grid"
            role="img"
            aria-label=${label}
            style="grid-template-columns: auto repeat(${cols.length}, minmax(14px, 1fr));"
          >
            <span class="cts-heatmap-corner"></span>
            ${cols.map((col) => html`<span class="cts-heatmap-colhead">${col}</span>`)}
            ${rows.map(
              (row, rowIndex) => html`
                <span class="cts-heatmap-rowhead">${row}</span>
                ${cols.map((col, colIndex) => this._renderCell(rowIndex, colIndex, max))}
              `,
            )}
          </div>
        </div>
      </div>
      ${this._overflows
        ? html`<p class="cts-heatmap-hint" data-testid="cts-heatmap-hint">
            Scroll sideways for later hours.
          </p>`
        : nothing}
    `;
  }

  /**
   * @param {number} row - Row index.
   * @param {number} col - Column index.
   * @param {number} max - The largest value in the grid.
   * @returns {unknown} One cell.
   */
  _renderCell(row, col, max) {
    const value = this._valueAt(row, col);
    const mix = heatmapIntensity(value, max);
    // An empty cell keeps the muted surface rather than the palest step of the
    // ramp: "no runs" and "a handful of runs" must not look the same.
    const fill =
      mix > 0 ? `color-mix(in oklab, var(${RAMP_HUE_VAR}) ${mix}%, var(--bg))` : "var(--bg-muted)";
    return html`<span
      class="cts-heatmap-cell"
      title=${this._titleAt(row, col, value)}
      style="background:${fill}"
    ></span>`;
  }

  /**
   * The scale legend. A sequential ramp is unreadable without one: the
   * reader has to be told which end is "more" and what the dark end is worth.
   * @param {number} max - The largest value in the grid.
   * @returns {unknown} The legend.
   */
  _renderScale(max) {
    return html`
      <div class="cts-heatmap-scale">
        <span>Scale: ${this.valueLabel} per cell (square-root scale)</span>
        <span class="cts-heatmap-scale-ramp">
          <span class="cts-heatmap-scale-step">
            <span class="cts-heatmap-swatch" style="background:var(--bg-muted)"></span>
            <span>0</span>
          </span>
          ${heatmapScaleSteps(max).map(
            (step) => html`
              <span class="cts-heatmap-scale-step">
                <span
                  class="cts-heatmap-swatch"
                  style="background: color-mix(in oklab, var(${RAMP_HUE_VAR}) ${step.mix}%, var(--bg));"
                ></span>
                <span>${NUMBER_FORMAT.format(step.value)}</span>
              </span>
            `,
          )}
        </span>
      </div>
    `;
  }

  /**
   * The accessible twin of the grid: every cell as a number, in a table that
   * reads linearly.
   * @param {Array<string>} rows - Row labels.
   * @param {Array<string>} cols - Column labels.
   * @returns {unknown} The disclosure.
   */
  _renderTable(rows, cols) {
    return html`
      <details class="cts-heatmap-data cts-data-disclosure">
        <summary>Show data table</summary>
        <div
          class="cts-data-table-scroll"
          role="group"
          aria-label="${this.heading} data table"
          tabindex="0"
        >
          <table class="cts-heatmap-table cts-data-table">
            <caption>${this.heading}</caption>
            <thead>
              <tr>
                <th scope="col">Day</th>
                ${cols.map((col) => html`<th scope="col">${col}</th>`)}
              </tr>
            </thead>
            <tbody>
              ${rows.map(
                (row, rowIndex) => html`
                  <tr>
                    <th scope="row">${row}</th>
                    ${cols.map(
                      (col, colIndex) => html`<td>${this._valueAt(rowIndex, colIndex)}</td>`,
                    )}
                  </tr>
                `,
              )}
            </tbody>
          </table>
        </div>
      </details>
    `;
  }
}

customElements.define("cts-heatmap", CtsHeatmap);

export {};
