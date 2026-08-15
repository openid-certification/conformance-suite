import { LitElement, html, css, nothing } from "lit";
import { classMap } from "lit/directives/class-map.js";
import "./cts-alert.js";

/**
 * Same-origin path to the vendored Chart.js UMD bundle. The bundle defines
 * the global `window.Chart`; nothing else on the page may load it (see
 * `src/main/resources/static/vendor/chart.js/README.md`) — this component
 * owns the load lifecycle the way `<cts-json-editor>` owns Monaco's.
 * @type {string}
 */
const CHART_JS_DEFAULT_URL = "/vendor/chart.js/chart.umd.js";

/**
 * Live URL the loader injects. Only `__setChartJsUrlForTests()` changes it.
 * @type {string}
 */
let chartJsUrl = CHART_JS_DEFAULT_URL;

/**
 * Singleton Promise resolving to the `Chart` constructor. Every
 * `<cts-chart>` on the page shares it, so the ~204 KB bundle is fetched
 * once per page lifetime no matter how many charts render.
 * @type {Promise<any>|null}
 */
let chartLoader = null;

/**
 * The two chart types this wrapper supports, as an explicit lookup so an
 * unknown/misspelled `type` falls back to `bar` instead of being handed
 * straight to Chart.js (which throws on an unregistered controller).
 * @type {Record<string, string>}
 */
const CHART_TYPES = { bar: "bar", line: "line" };

/** Bar cap, per the data-viz mark spec (never fill the band). */
const MAX_BAR_THICKNESS = 24;
/** Surface-coloured gap between touching marks / around point rings. */
const SURFACE_GAP_PX = 2;
/** Rounded data-end on bars. */
const BAR_RADIUS_PX = 4;
/** Line stroke width. */
const LINE_WIDTH_PX = 2;
/** Point radius — an 8px marker, per the mark spec. */
const POINT_RADIUS_PX = 4;
/** Fixed plot height; the frame is sized so the x-axis band fits inside it. */
const FRAME_HEIGHT_PX = 320;

/**
 * Horizontal bars are laid out the other way round: the height is the number
 * of categories, not a constant. One band per bar plus the value-axis band at
 * the bottom, so a 3-row chart is short and a 12-row one is tall enough that
 * every label has room.
 */
const HORIZONTAL_BAND_PX = 26;
const HORIZONTAL_AXIS_PX = 44;
const HORIZONTAL_MIN_HEIGHT_PX = 120;

/**
 * Properties whose change actually affects the plot. Everything else
 * (`heading`, `categoryLabel`) only re-renders the surrounding markup, so
 * pushing it into Chart.js would replay the animation and re-read computed
 * styles for nothing.
 * @type {Array<string>}
 */
const PLOT_PROPS = [
  "type",
  "labels",
  "datasets",
  "stacked",
  "horizontal",
  "maxBars",
  "clickable",
  "_status",
];

/**
 * Verb phrase the data-table row buttons are named with when the consumer
 * sets `clickable` but not `click-label`. Deliberately vague, because the
 * primitive does not know what the consumer does with the click; every real
 * consumer should say what it does.
 */
const DEFAULT_CLICK_LABEL = "Show details for";

/** Per-instance id counter, so each `<figure>` can point at its own `<h3>`. */
let headingSeq = 0;

/**
 * Inject the Chart.js UMD bundle once and resolve the `Chart` constructor
 * it registers on `window`. Memoised; the memo is cleared on rejection so
 * a later instance can retry after a transient network failure.
 * @returns {Promise<any>} Resolves to `window.Chart`.
 */
function loadChartJs() {
  if (chartLoader) return chartLoader;

  /** @type {HTMLScriptElement|null} The tag this attempt injected, if any. */
  let injected = null;

  const attempt = new Promise((resolve, reject) => {
    if (typeof window === "undefined") {
      reject(new Error("Chart.js requires a browser environment"));
      return;
    }
    if (window.Chart) {
      resolve(window.Chart);
      return;
    }

    const script = document.createElement("script");
    injected = script;
    script.src = chartJsUrl;
    script.async = true;
    script.onerror = () => reject(new Error(`Failed to load ${script.src}`));
    script.onload = () => {
      if (window.Chart) {
        resolve(window.Chart);
      } else {
        reject(new Error(`${script.src} loaded without defining window.Chart`));
      }
    };
    document.head.appendChild(script);
  });
  chartLoader = attempt;

  attempt.catch(() => {
    // Clear the memo only if it still points at THIS attempt. A retry that
    // started (or succeeded) in the meantime — or a test hook that reset the
    // singleton — must not be discarded by an older rejection landing late.
    if (chartLoader === attempt) chartLoader = null;
    // Drop the dead tag so repeated retries do not accumulate <script>
    // elements in <head>.
    if (injected) injected.remove();
  });

  return attempt;
}

const STYLE_ID = "cts-chart-styles";

const STYLE_TEXT = css`
  /* Custom elements are inline by default, which collapses the fixed-height
     frame; block is what every caller wants. */
  cts-chart {
    display: block;
  }
  .cts-chart {
    margin: 0;
  }
  .cts-chart-heading {
    margin: 0 0 var(--space-2, 8px);
    font-family: var(--font-sans);
    font-size: var(--fs-16, 16px);
    font-weight: var(--fw-bold, 700);
    line-height: var(--lh-snug, 1.3);
    color: var(--fg);
  }
  /* Chart.js sizes the canvas against this box (responsive +
     maintainAspectRatio:false), so it must have a resolved height and be
     the positioned ancestor. The height covers plot AND x-axis band. */
  .cts-chart-frame {
    position: relative;
    height: ${FRAME_HEIGHT_PX}px;
  }
  /* Horizontal bars grow downwards, so the frame is sized from the category
     count (set inline as --cts-chart-rows) rather than fixed: a fixed height
     would either squeeze twelve bars into 320px or leave three floating in
     it. The axis band is inside the height, so the card never gets a nested
     scrollbar. */
  .cts-chart-frame.is-horizontal {
    height: max(
      ${HORIZONTAL_MIN_HEIGHT_PX}px,
      calc(var(--cts-chart-rows, 1) * ${HORIZONTAL_BAND_PX}px + ${HORIZONTAL_AXIS_PX}px)
    );
  }
  /* A long category name is the normal case here (plan-level variant values,
     certification profile names), so the note that says what the plot leaves
     out sits with the chart, not in the table. */
  .cts-chart-more {
    margin: var(--space-2, 8px) 0 0;
    font-size: var(--fs-12, 12px);
    color: var(--fg-soft);
  }
  .cts-chart-data {
    margin-top: var(--space-3, 12px);
  }
  .cts-chart-data > summary {
    cursor: pointer;
    font-size: var(--fs-13, 13px);
    color: var(--fg-muted);
  }
  .cts-chart-data > summary:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
    border-radius: var(--radius-2, 4px);
  }
  .cts-chart-table {
    width: 100%;
    margin-top: var(--space-2, 8px);
    border-collapse: collapse;
    font-size: var(--fs-13, 13px);
    /* Columns of numbers align; see the data-viz figures rule. */
    font-variant-numeric: tabular-nums;
  }
  .cts-chart-table caption {
    text-align: left;
    padding-bottom: var(--space-2, 8px);
    font-size: var(--fs-12, 12px);
    color: var(--fg-soft);
  }
  .cts-chart-table th,
  .cts-chart-table td {
    padding: var(--space-2, 8px) var(--space-3, 12px);
    border-bottom: 1px solid var(--border);
    text-align: right;
  }
  .cts-chart-table th[scope="col"]:first-child,
  .cts-chart-table th[scope="row"] {
    text-align: left;
    font-weight: var(--fw-regular, 400);
  }
  .cts-chart-table thead th {
    color: var(--fg-soft);
    font-weight: var(--fw-bold, 700);
  }
  /* Keyboard twin of a click on a bar: the plot's hit targets live on a
     <canvas>, which no keyboard can reach, so on a clickable chart every row
     of the data table carries the same action in its category cell. Styled as
     a link rather than a button because it behaves like one — it takes the
     reader somewhere. */
  .cts-chart-row-link {
    padding: 0;
    border: 0;
    background: none;
    font: inherit;
    color: var(--fg-link);
    text-decoration-line: underline;
    text-decoration-thickness: 1px;
    text-underline-offset: 2px;
    text-decoration-color: var(--link-decoration-color);
    cursor: pointer;
  }
  .cts-chart-row-link:hover {
    text-decoration-color: currentColor;
  }
  .cts-chart-row-link:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
    border-radius: var(--radius-2, 4px);
  }
`;

/**
 * Append the scoped stylesheet to `<head>` once per page lifetime. The
 * `STYLE_ID` guard means N charts share one rule set regardless of mount
 * order.
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/** Longest category label a horizontal axis tick renders before eliding. */
const HORIZONTAL_LABEL_MAX = 28;

/**
 * Shorten a category label for a horizontal axis tick. The full text stays in
 * the tooltip and the data table, so nothing is lost — only the axis column's
 * share of the card is capped.
 * @param {unknown} label - The tick's label.
 * @returns {string} The label, elided to {@link HORIZONTAL_LABEL_MAX}.
 */
function elide(label) {
  const text = String(label ?? "");
  if (text.length <= HORIZONTAL_LABEL_MAX) return text;
  return `${text.slice(0, HORIZONTAL_LABEL_MAX - 1).trimEnd()}…`;
}

/**
 * Read a design-system custom property off `:root`.
 * @param {CSSStyleDeclaration} cs - Computed style of `document.documentElement`.
 * @param {string} name - Custom property name, including the leading `--`.
 * @param {string} fallback - Value to use when the property is unset.
 * @returns {string} The resolved value, or `fallback`.
 */
function token(cs, name, fallback) {
  const value = cs.getPropertyValue(name).trim();
  return value || fallback;
}

/**
 * Chart primitive wrapping the vendored Chart.js UMD bundle, which it
 * lazy-loads on first connect. Renders a `<figure>` containing the heading,
 * a fixed-height canvas, and a `<details>` data table carrying the exact
 * values — the table is the accessible twin of the plot, not a fallback, so
 * no value is reachable only by hovering or by discriminating a fill colour.
 *
 * If Chart.js fails to load, the plot is replaced by a warning
 * `<cts-alert>` and the table stays exactly as it was.
 *
 * Light DOM (`createRenderRoot()` returns `this`), so page CSS and the
 * design-system tokens reach the rendered markup directly.
 * @property {string} type - `"bar"` (default) or `"line"`. Unknown values
 *   fall back to `"bar"`.
 * @property {Array<string>} labels - One category label per x position
 *   (e.g. `["2026-01", "2026-02"]`). Also the table's row headers.
 * @property {Array<{label: string, data: Array<number>, colorVar: string}>} datasets -
 *   One entry per series. `colorVar` is a CSS custom-property name that
 *   includes the leading `--` (e.g. `"--chart-cat-1"`), resolved off
 *   `:root`; unresolvable names fall back to `--ink-400`.
 * @property {boolean} stacked - Stack the series on both axes (bar charts).
 * @property {boolean} horizontal - Lay bars out along the x axis, one
 *   category per row (`indexAxis: "y"`). The frame's height then follows the
 *   category count. Use it whenever the category names are long enough to be
 *   unreadable rotated under a column — plan names, variant values,
 *   certification profiles.
 * @property {number} maxBars - Plot at most this many categories (0, the
 *   default, plots them all). The data table is NEVER truncated — it keeps
 *   every row, and a note under the plot says how many were left out — so
 *   capping the bars hides nothing.
 * @property {Array<{label: string, data: Array<number>}>} tableExtras -
 *   Extra columns for the data table only, appended after the plotted
 *   series. For the second measure a category carries that is context rather
 *   than the thing being compared (the plan count behind a user count).
 *   Property-only; not settable as an attribute.
 * @property {string} heading - Chart title. Rendered as the `<h3>`, the
 *   table `<caption>`, and the leading half of the canvas `aria-label`.
 * @property {string} categoryLabel - Header for the table's first (category)
 *   column — the name of the thing each row is (e.g. `"Month"`). Defaults
 *   to `"Period"`. Attribute name: `category-label`.
 * @property {(hoveredIndex: number) => Array<string>} tooltipFooter -
 *   Optional property-only hook returning extra tooltip footer lines for
 *   the hovered label index (used to name the families folded into
 *   "Other"). Not settable as an attribute.
 * @property {boolean} clickable - Make the plot a click target: the pointer
 *   turns into a hand over a mark, a click emits {@link CtsChart#event:cts-chart-click},
 *   and every row of the data table gets a button in its category cell
 *   carrying the same action for keyboard and screen-reader users. Opt-in,
 *   because a chart nothing listens to must not pretend to be actionable.
 * @property {string} clickLabel - What activating a data-table row button
 *   does, as a verb phrase the category label is appended to ("List the test
 *   plans in" → "List the test plans in 2026-06"). It is the button's
 *   accessible name; the visible text stays the bare label. Attribute name:
 *   `click-label`. Only used when `clickable` is set.
 * @fires cts-chart-click - When a mark, or a data-table row button, is
 *   activated on a `clickable` chart. `detail: {periodIndex, datasetIndex,
 *   datasetLabel, label}`. `datasetIndex` is `null` (and `datasetLabel` `""`)
 *   when the activation names a whole category rather than one series — a
 *   click inside the band but not on a mark, and every row button, which is
 *   the keyboard equivalent of "this whole period". Bubbles and is composed.
 */
class CtsChart extends LitElement {
  static properties = {
    type: { type: String },
    labels: { type: Array },
    datasets: { type: Array },
    stacked: { type: Boolean },
    horizontal: { type: Boolean },
    maxBars: { type: Number, attribute: "max-bars" },
    tableExtras: { attribute: false },
    heading: { type: String },
    categoryLabel: { type: String, attribute: "category-label" },
    tooltipFooter: { attribute: false },
    clickable: { type: Boolean },
    clickLabel: { type: String, attribute: "click-label" },
    _status: { state: true },
  };

  constructor() {
    super();
    /** @type {string} */
    this.type = "bar";
    /** @type {Array<string>} */
    this.labels = [];
    /** @type {Array<{label: string, data: Array<number>, colorVar: string}>} */
    this.datasets = [];
    /** @type {boolean} */
    this.stacked = false;
    /** @type {boolean} */
    this.horizontal = false;
    /** @type {number} */
    this.maxBars = 0;
    /** @type {Array<{label: string, data: Array<number>}>} */
    this.tableExtras = [];
    /** @type {string} */
    this.heading = "";
    /** @type {string} */
    this.categoryLabel = "";
    /** @type {((hoveredIndex: number) => Array<string>)|undefined} */
    this.tooltipFooter = undefined;
    /** @type {boolean} */
    this.clickable = false;
    /** @type {string} */
    this.clickLabel = DEFAULT_CLICK_LABEL;
    /** @type {"loading"|"ready"|"error"} */
    this._status = "loading";
    /** @type {any} */
    this._chartCtor = null;
    /** @type {any} */
    this._chart = null;
    /** @type {string|null} Chart type the live instance was constructed with. */
    this._renderedType = null;
    /** @type {string} Ties the <figure>'s aria-labelledby to its own <h3>. */
    this._headingId = `cts-chart-heading-${++headingSeq}`;
  }

  createRenderRoot() {
    return this;
  }

  /**
   * The live Chart.js instance, or `null` before the bundle has loaded and
   * after `disconnectedCallback()` destroyed it.
   *
   * Read-only, and the only supported way to reach the chart's RESOLVED
   * state — the fill a `colorVar` actually resolved to, the data Chart.js is
   * holding. It exists for tests that must assert on those (the statistics
   * page proves a family is never repainted by comparing resolved fills);
   * production callers drive the chart through the reactive properties and
   * should never touch it.
   * @returns {any} The Chart.js instance, or null.
   */
  get chartInstance() {
    return this._chart;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    this._boot();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._destroyChart();
  }

  updated(changed) {
    if (!this._chartCtor) return;
    // A chart that does not exist yet is always created — the update that
    // reveals the canvas may be triggered by any property. Once it exists,
    // only a plot-affecting change is worth pushing into Chart.js.
    if (this._chart && !PLOT_PROPS.some((prop) => changed.has(prop))) return;
    this._syncChart();
  }

  /**
   * Resolve the Chart.js constructor, then draw. Re-entrant: reconnecting a
   * chart that was destroyed on disconnect re-creates it without refetching
   * the bundle.
   */
  async _boot() {
    try {
      this._chartCtor = await loadChartJs();
    } catch {
      this._status = "error";
      return;
    }
    if (!this.isConnected) return;
    this._status = "ready";
    await this.updateComplete;
    this._syncChart();
  }

  /** Tear down the live Chart.js instance, if any. */
  _destroyChart() {
    if (!this._chart) return;
    this._chart.destroy();
    this._chart = null;
    this._renderedType = null;
  }

  /**
   * Create the chart, or push new data/options into the existing one. The
   * chart type is fixed at construction time in Chart.js, so a `type`
   * change destroys and rebuilds rather than updating.
   */
  _syncChart() {
    if (!this._chartCtor || !this.isConnected) return;
    const canvas = this.querySelector("canvas");
    if (!canvas) return;

    const type = CHART_TYPES[this.type] || CHART_TYPES.bar;
    if (this._chart && this._renderedType !== type) this._destroyChart();

    // One computed-style read per sync, shared by both builders: resolving
    // :root custom properties forces a style recalc, and both builders want
    // the same snapshot anyway.
    const cs = window.getComputedStyle(document.documentElement);

    if (this._chart) {
      this._chart.data = this._buildData(type, cs);
      this._chart.options = this._buildOptions(cs);
      this._chart.update();
      return;
    }

    const Ctor = this._chartCtor;
    this._chart = new Ctor(canvas, {
      type,
      data: this._buildData(type, cs),
      options: this._buildOptions(cs),
    });
    this._renderedType = type;
  }

  /**
   * How many categories the PLOT shows. The data table always shows them all;
   * `maxBars` only caps the bars, because past a dozen of them the shortest
   * stop being comparable.
   * @returns {number} The plotted category count.
   */
  _plottedCount() {
    const labels = (this.labels || []).length;
    const cap = Number(this.maxBars) || 0;
    return cap > 0 ? Math.min(cap, labels) : labels;
  }

  /**
   * Build the Chart.js `data` object, resolving each series' `colorVar`
   * against `:root` and applying the mark specs (capped bar thickness,
   * rounded data-end, 2px surface gap between touching fills, 2px surface
   * ring on line markers).
   * @param {string} type - Resolved chart type (`"bar"` or `"line"`).
   * @param {CSSStyleDeclaration} cs - Computed style of `document.documentElement`.
   * @returns {{labels: Array<string>, datasets: Array<object>}} Chart.js data.
   */
  _buildData(type, cs) {
    const fallbackColor = token(cs, "--ink-400", "#A09A8E");
    const surface = token(cs, "--chart-surface", token(cs, "--bg", "#FFFFFF"));
    const isLine = type === "line";
    const plotted = this._plottedCount();

    return {
      labels: (this.labels || []).slice(0, plotted),
      datasets: (this.datasets || []).map((ds) => {
        const color = ds.colorVar ? token(cs, ds.colorVar, fallbackColor) : fallbackColor;
        const base = {
          label: ds.label,
          data: (ds.data || []).slice(0, plotted),
        };
        if (isLine) {
          return {
            ...base,
            borderColor: color,
            backgroundColor: color,
            borderWidth: LINE_WIDTH_PX,
            fill: false,
            pointRadius: POINT_RADIUS_PX,
            pointHoverRadius: POINT_RADIUS_PX + 2,
            pointBackgroundColor: color,
            pointBorderColor: surface,
            pointBorderWidth: SURFACE_GAP_PX,
          };
        }
        return {
          ...base,
          backgroundColor: color,
          // The "border" is painted in the surface colour: it IS the 2px gap
          // that separates stacked segments and adjacent bars, not a stroke
          // adding non-data ink.
          borderColor: surface,
          borderWidth: SURFACE_GAP_PX,
          borderSkipped: false,
          borderRadius: BAR_RADIUS_PX,
          maxBarThickness: MAX_BAR_THICKNESS,
        };
      }),
    };
  }

  /**
   * Build the Chart.js `options` object: recessive grid/axes in design-system
   * tokens, a legend whenever two or more series are present, an index-mode
   * tooltip listing every series at the hovered label (values leading), and
   * animation disabled under `prefers-reduced-motion`.
   * @param {CSSStyleDeclaration} cs - Computed style of `document.documentElement`.
   * @returns {object} Chart.js options.
   */
  _buildOptions(cs) {
    const fontFamily = token(cs, "--font-sans", "sans-serif");
    const tickColor = token(cs, "--fg-soft", "#71695E");
    const gridColor = token(cs, "--border", "#DFDCD5");
    const font = { family: fontFamily, size: 12 };
    const stacked = this.stacked === true;
    const seriesCount = (this.datasets || []).length;
    const isLine = (CHART_TYPES[this.type] || CHART_TYPES.bar) === "line";
    const reduceMotion =
      typeof window.matchMedia === "function" &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    const horizontal = this.horizontal === true;
    const plottedLabels = (this.labels || []).slice(0, this._plottedCount());
    // Gridlines belong to the VALUE axis: on the categorical axis they add
    // ink without carrying a value. Which of x/y that is flips with
    // `horizontal`, so the two axis descriptions are built and then assigned.
    /** @type {Record<string, unknown>} */
    const categoryTicks = {
      color: tickColor,
      font,
      // Every row of a horizontal chart is named — the frame's height is sized
      // from the category count precisely so they all fit. A vertical axis
      // keeps Chart.js's own thinning, or 52 weekly ticks would overlap.
      autoSkip: !horizontal,
    };
    if (horizontal) {
      // Chart.js grows the category axis to fit its longest label, so one
      // 60-character certification profile name would leave the bars a sliver
      // of the card. The label is elided in the tick only; the tooltip and the
      // data table below carry it in full. The index is read out of our own
      // labels rather than off the scale, so the callback stays an arrow with
      // no `this` to bind — and the key is only ADDED when it is needed,
      // because setting `callback: undefined` overrides the category scale's
      // own formatter and leaves an axis of row numbers.
      categoryTicks.callback = (/** @type {unknown} */ value, /** @type {number} */ index) =>
        elide(plottedLabels[index]);
    }
    const categoryAxis = {
      stacked,
      grid: { display: false },
      border: { color: gridColor },
      ticks: categoryTicks,
    };
    const valueAxis = {
      stacked,
      beginAtZero: true,
      grid: { color: gridColor, drawTicks: false },
      border: { display: false },
      // Everything this component plots is a count of something, so a scale
      // that tops out at 1 must tick 0/1 rather than 0.0, 0.2, 0.4 …
      ticks: { color: tickColor, font, precision: 0 },
    };

    /** @type {any} */
    const options = {
      responsive: true,
      maintainAspectRatio: false,
      // Chart.js reads the category axis off indexAxis; "y" is what turns
      // columns into rows.
      indexAxis: horizontal ? "y" : "x",
      // The whole band is the hit target, so the pointer never has to land on
      // a 2px line to read a value. `axis` MUST follow `indexAxis`: index mode
      // defaults to measuring distance in x, which on a horizontal chart picks
      // the row whose VALUE is nearest the cursor rather than the row the
      // pointer is actually over.
      interaction: { mode: "index", intersect: false, axis: horizontal ? "y" : "x" },
      scales: horizontal ? { x: valueAxis, y: categoryAxis } : { x: categoryAxis, y: valueAxis },
      plugins: {
        legend: {
          display: seriesCount >= 2,
          position: "bottom",
          labels: {
            color: tickColor,
            font,
            boxWidth: isLine ? 20 : 12,
            boxHeight: 12,
            // The legend key mirrors the mark it stands for: a rect for
            // bars, a short stroke for lines.
            usePointStyle: isLine,
            pointStyle: isLine ? "line" : undefined,
          },
        },
        tooltip: {
          mode: "index",
          intersect: false,
          axis: horizontal ? "y" : "x",
          callbacks: {
            // Values lead, series names follow — the reader already has the
            // series and wants the number.
            label: (ctx) => `${ctx.formattedValue}  ${ctx.dataset.label ?? ""}`.trimEnd(),
            footer: (items) => {
              if (typeof this.tooltipFooter !== "function" || !items.length) return [];
              return this.tooltipFooter(items[0].dataIndex) || [];
            },
          },
        },
      },
    };
    if (reduceMotion) options.animation = false;
    if (this.clickable === true) {
      // The hit target is the whole band (interaction mode "index",
      // intersect false), so the hand appears wherever a click would land on
      // something — and only there, which keeps the axis gutters honest.
      // `options` is itself typed `any` (Chart.js ships no types this project
      // consumes), so these callbacks' parameters are implicitly `any` too —
      // deliberately left unannotated rather than papered over with a cast per
      // parameter, which is how the rest of this file's Chart.js seam reads.
      options.onHover = (event, elements) => {
        const target = event && event.native && event.native.target;
        if (target && target.style) target.style.cursor = elements.length ? "pointer" : "";
      };

      options.onClick = (event, _elements, chart) => this._emitPlotClick(event, chart);
    }
    return options;
  }

  /**
   * Resolve a canvas click to a category, and to the series it landed on when
   * it landed on one.
   *
   * Two lookups, because they answer different questions. `nearest` +
   * `intersect` is the only mode that names ONE mark — which stacked segment
   * the pointer is actually inside — and that is what identifies the series.
   * When the click is inside the band but above the bars (or between the
   * points of a line), that lookup is empty and the band lookup answers the
   * weaker question the reader is still entitled to ask: which category is
   * this column? That case emits a `datasetIndex` of `null`.
   * @param {any} event - The Chart.js event wrapper.
   * @param {any} chart - The Chart.js instance.
   * @returns {void}
   */
  _emitPlotClick(event, chart) {
    // `useFinalPosition: false` throughout: hit-test against where the marks
    // ARE, not where an in-flight animation is taking them, so a click during
    // the repaint that follows a filter change resolves to the bar the reader
    // actually pointed at. It is also what Chart.js's own hover does.
    const onMark = chart.getElementsAtEventForMode(event, "nearest", { intersect: true }, false);
    if (onMark.length > 0) {
      this._dispatchClick(onMark[0].index, onMark[0].datasetIndex);
      return;
    }
    const inBand = chart.getElementsAtEventForMode(
      event,
      "index",
      { intersect: false, axis: this.horizontal === true ? "y" : "x" },
      false,
    );
    if (inBand.length === 0) return;
    this._dispatchClick(inBand[0].index, null);
  }

  /**
   * Emit `cts-chart-click` for one category, optionally naming a series.
   * @param {number} periodIndex - Position in `labels`.
   * @param {number|null} datasetIndex - Position in `datasets`, or `null` when
   *   the activation names the whole category.
   * @returns {void}
   */
  _dispatchClick(periodIndex, datasetIndex) {
    const dataset = datasetIndex === null ? null : (this.datasets || [])[datasetIndex];
    this.dispatchEvent(
      new CustomEvent("cts-chart-click", {
        bubbles: true,
        composed: true,
        detail: {
          periodIndex,
          datasetIndex,
          datasetLabel: (dataset && dataset.label) || "",
          label: (this.labels || [])[periodIndex] ?? "",
        },
      }),
    );
  }

  /**
   * Put the cursor back when the pointer leaves the plot.
   *
   * Chart.js reports a hover only while the pointer is inside the chart area,
   * so leaving the canvas straight off a bar never clears the hand it set —
   * it would stay on a chart nobody is pointing at, and on the element behind
   * it once the chart is re-rendered. `mouseleave` is the one event that
   * always fires on the way out.
   * @param {Event} event - The canvas's `mouseleave`.
   * @returns {void}
   */
  _handleCanvasLeave(event) {
    if (this.clickable !== true) return;
    const canvas = /** @type {HTMLCanvasElement} */ (event.currentTarget);
    canvas.style.cursor = "";
  }

  /**
   * Row-button handler: the keyboard twin of a click on a bar. It names the
   * category only — a row is a period, not a series — so it emits the same
   * "whole category" event a click inside the band emits.
   * @param {Event} event - The button's click event.
   * @returns {void}
   */
  _handleRowClick(event) {
    const index = Number(/** @type {HTMLElement} */ (event.currentTarget).dataset.index);
    if (!Number.isInteger(index)) return;
    this._dispatchClick(index, null);
  }

  render() {
    const labels = this.labels || [];
    const datasets = this.datasets || [];
    const extras = this.tableExtras || [];
    const type = CHART_TYPES[this.type] || CHART_TYPES.bar;
    const horizontal = this.horizontal === true;
    const plotted = this._plottedCount();
    const unit = horizontal ? "categories" : "periods";
    const ariaLabel =
      `${this.heading}: ${type} chart of ${plotted} ${unit} across ` +
      `${datasets.length} series. Data table available below.`;
    const clickLabel = this.clickLabel || DEFAULT_CLICK_LABEL;

    return html`
      <figure class="cts-chart" aria-labelledby=${this._headingId}>
        <h3 class="cts-chart-heading" id=${this._headingId}>${this.heading}</h3>
        ${this._status === "error"
          ? html`<cts-alert variant="warning"
              >Chart could not be rendered; the data table below is complete.</cts-alert
            >`
          : html`<div
              class=${classMap({ "cts-chart-frame": true, "is-horizontal": horizontal })}
              style="--cts-chart-rows:${plotted}"
            >
              <canvas
                role="img"
                aria-label=${ariaLabel}
                @mouseleave=${this._handleCanvasLeave}
              ></canvas>
            </div>`}
        ${plotted < labels.length
          ? html`<p class="cts-chart-more" data-testid="cts-chart-more">
              Showing the top ${plotted} of ${labels.length}; the rest are in the data table.
            </p>`
          : nothing}
        <details class="cts-chart-data">
          <summary>Show data table</summary>
          <table class="cts-chart-table">
            <caption>${this.heading}</caption>
            <thead>
              <tr>
                <th scope="col">${this.categoryLabel || "Period"}</th>
                ${datasets.map((ds) => html`<th scope="col">${ds.label}</th>`)}
                ${extras.map((extra) => html`<th scope="col">${extra.label}</th>`)}
              </tr>
            </thead>
            <tbody>
              ${labels.map(
                (label, i) =>
                  html`<tr>
                    <th scope="row">
                      ${this.clickable === true
                        ? html`<button
                            type="button"
                            class="cts-chart-row-link"
                            data-index=${i}
                            aria-label="${clickLabel} ${label}"
                            @click=${this._handleRowClick}
                          >
                            ${label}
                          </button>`
                        : label}
                    </th>
                    ${datasets.map((ds) => html`<td>${ds.data?.[i] ?? ""}</td>`)}
                    ${extras.map((extra) => html`<td>${extra.data?.[i] ?? ""}</td>`)}
                  </tr>`,
              )}
            </tbody>
          </table>
        </details>
      </figure>
    `;
  }
}

customElements.define("cts-chart", CtsChart);

/**
 * TEST-ONLY: discard the memoised Chart.js loader promise so the next
 * `<cts-chart>` attempts a full load again. Production callers MUST NOT use
 * this — it exists so the Storybook `LoadFailure` story can simulate a cold
 * load even after a sibling story already booted Chart.js in the same
 * window. Pair it with `delete window.Chart`, which the loader would
 * otherwise short-circuit on.
 */
export function __resetChartLoaderForTests() {
  chartLoader = null;
}

/**
 * TEST-ONLY: point the loader at a different script URL. Pass no argument
 * (or `null`) to restore the vendored path. Production callers MUST NOT use
 * this — the `LoadFailure` story aims the loader at a 404 to exercise the
 * warning-alert path.
 * @param {string|null} [url] - URL to inject, or nullish to restore the default.
 */
export function __setChartJsUrlForTests(url) {
  chartJsUrl = url || CHART_JS_DEFAULT_URL;
}
