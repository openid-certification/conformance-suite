import { LitElement, html, css } from "lit";
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
 * Properties whose change actually affects the plot. Everything else
 * (`heading`, `categoryLabel`) only re-renders the surrounding markup, so
 * pushing it into Chart.js would replay the animation and re-read computed
 * styles for nothing.
 * @type {Array<string>}
 */
const PLOT_PROPS = ["type", "labels", "datasets", "stacked", "_status"];

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
 * @property {string} heading - Chart title. Rendered as the `<h3>`, the
 *   table `<caption>`, and the leading half of the canvas `aria-label`.
 * @property {string} categoryLabel - Header for the table's first (category)
 *   column — the name of the thing each row is (e.g. `"Month"`). Defaults
 *   to `"Period"`. Attribute name: `category-label`.
 * @property {(hoveredIndex: number) => Array<string>} tooltipFooter -
 *   Optional property-only hook returning extra tooltip footer lines for
 *   the hovered label index (used to name the families folded into
 *   "Other"). Not settable as an attribute.
 */
class CtsChart extends LitElement {
  static properties = {
    type: { type: String },
    labels: { type: Array },
    datasets: { type: Array },
    stacked: { type: Boolean },
    heading: { type: String },
    categoryLabel: { type: String, attribute: "category-label" },
    tooltipFooter: { attribute: false },
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
    /** @type {string} */
    this.heading = "";
    /** @type {string} */
    this.categoryLabel = "";
    /** @type {((hoveredIndex: number) => Array<string>)|undefined} */
    this.tooltipFooter = undefined;
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

    return {
      labels: [...(this.labels || [])],
      datasets: (this.datasets || []).map((ds) => {
        const color = ds.colorVar ? token(cs, ds.colorVar, fallbackColor) : fallbackColor;
        const base = {
          label: ds.label,
          data: [...(ds.data || [])],
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

    /** @type {any} */
    const options = {
      responsive: true,
      maintainAspectRatio: false,
      // The whole column is the hit target, so the pointer never has to land
      // on a 2px line to read a value.
      interaction: { mode: "index", intersect: false },
      scales: {
        x: {
          stacked,
          // Vertical gridlines add ink without carrying a value on a
          // categorical axis; the y grid alone locates the marks.
          grid: { display: false },
          border: { color: gridColor },
          ticks: { color: tickColor, font },
        },
        y: {
          stacked,
          beginAtZero: true,
          grid: { color: gridColor, drawTicks: false },
          border: { display: false },
          ticks: { color: tickColor, font },
        },
      },
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
    return options;
  }

  render() {
    const labels = this.labels || [];
    const datasets = this.datasets || [];
    const type = CHART_TYPES[this.type] || CHART_TYPES.bar;
    const ariaLabel =
      `${this.heading}: ${type} chart of ${labels.length} periods across ` +
      `${datasets.length} series. Data table available below.`;

    return html`
      <figure class="cts-chart" aria-labelledby=${this._headingId}>
        <h3 class="cts-chart-heading" id=${this._headingId}>${this.heading}</h3>
        ${this._status === "error"
          ? html`<cts-alert variant="warning"
              >Chart could not be rendered; the data table below is complete.</cts-alert
            >`
          : html`<div class="cts-chart-frame">
              <canvas role="img" aria-label=${ariaLabel}></canvas>
            </div>`}
        <details class="cts-chart-data">
          <summary>Show data table</summary>
          <table class="cts-chart-table">
            <caption>${this.heading}</caption>
            <thead>
              <tr>
                <th scope="col">${this.categoryLabel || "Period"}</th>
                ${datasets.map((ds) => html`<th scope="col">${ds.label}</th>`)}
              </tr>
            </thead>
            <tbody>
              ${labels.map(
                (label, i) =>
                  html`<tr>
                    <th scope="row">${label}</th>
                    ${datasets.map((ds) => html`<td>${ds.data?.[i] ?? ""}</td>`)}
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
