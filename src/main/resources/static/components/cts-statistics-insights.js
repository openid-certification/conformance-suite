import { LitElement, html, nothing, css } from "lit";
import "./cts-chart.js";
import "./cts-heatmap.js";
import "./cts-time.js";
import { injectDataTableStyles } from "./data-table-styles.js";
import {
  DISTRIBUTION_LIMIT,
  MODULE_LIMIT,
  NUMBER_FORMAT,
  formatShare,
  rangePreset,
} from "./statistics-model.js";

/** @typedef {import("./statistics-model.js").Distribution} Distribution */
/** @typedef {import("./statistics-model.js").Distributions} Distributions */
/** @typedef {import("./statistics-model.js").ModuleChart} ModuleChart */
/** @typedef {import("./statistics-model.js").ModuleRow} ModuleRow */

/**
 * The heatmap's rows, Monday first — the order `StatisticsOverview.heatmap`
 * delivers them in.
 * @type {Array<string>}
 */
const DAY_LABELS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

/**
 * Its columns: the 24 UTC hours, as the two-digit label an axis has room for.
 * @type {Array<string>}
 */
const HOUR_LABELS = Array.from({ length: 24 }, (_, hour) => String(hour).padStart(2, "0"));

/**
 * Per-distribution tooltip footers, cached on the distribution object's
 * identity. `<cts-chart>` is handed the SAME callback for as long as the page
 * hands it the same (memoised) distribution, so a re-render for the busy flag
 * cannot churn it.
 * @type {WeakMap<object, (index: number) => Array<string>>}
 */
const EXTRA_FOOTERS = new WeakMap();

/**
 * The tooltip footer that names a distribution's second, unplotted measure —
 * the plan count behind a user count. Without it the plan count is only in the
 * data table, and the reader hovering a bar has no way to see it.
 * @param {Distribution} distribution - One distribution.
 * @returns {((index: number) => Array<string>)|undefined} The footer callback,
 *   or undefined when the distribution carries no second measure.
 */
function extraFooter(distribution) {
  const extra = ((distribution && distribution.extras) || [])[0];
  if (!extra) return undefined;
  const cached = EXTRA_FOOTERS.get(distribution);
  if (cached) return cached;
  const footer = (/** @type {number} */ index) => {
    const value = Number((extra.data || [])[index]);
    if (!Number.isFinite(value)) return [];
    return [`${NUMBER_FORMAT.format(value)} ${extra.label.toLowerCase()}`];
  };
  EXTRA_FOOTERS.set(distribution, footer);
  return footer;
}

/**
 * Per-chart tooltip footers, cached on the chart object's identity — the same
 * bargain {@link extraFooter} strikes: the page hands this component the same
 * memoised `ModuleChart` until the payload changes, so `<cts-chart>` keeps
 * being handed the same callback and a busy-flag re-render re-plots nothing.
 * @type {WeakMap<object, (index: number) => Array<string>>}
 */
const MODULE_FOOTERS = new WeakMap();

/**
 * The tooltip footer carrying the measures a module chart does NOT plot — the
 * user counts behind a run count, the denominator behind a failure count.
 * {@link moduleDatasets} has already written the lines; this only wraps them
 * in the callback `<cts-chart>` wants.
 * @param {ModuleChart} chart - One module chart's inputs.
 * @returns {(index: number) => Array<string>} The footer callback.
 */
function moduleFooter(chart) {
  const cached = MODULE_FOOTERS.get(chart);
  if (cached) return cached;
  const footer = (/** @type {number} */ index) => (chart.footers || [])[index] || [];
  MODULE_FOOTERS.set(chart, footer);
  return footer;
}

/**
 * One heatmap cell's hover text. A module constant, not a closure built per
 * render: `<cts-heatmap>` compares its properties by identity, and a fresh
 * function every render would re-render 168 cells for nothing.
 * @param {number} row - Day index, Monday first.
 * @param {number} col - Hour index, UTC.
 * @param {number} value - Runs started in that hour.
 * @returns {string} e.g. `"Tue 14:00 UTC — 123 runs"`.
 */
function heatmapCellTitle(row, col, value) {
  return `${DAY_LABELS[row]} ${HOUR_LABELS[col]}:00 UTC — ${NUMBER_FORMAT.format(value)} runs`;
}

/**
 * What the modules section counts, said before it is read. Two things about
 * the window, because they are different: the server only keeps module cells
 * for a trailing 24 months, and within that it counts the range the reader
 * selected (which the heading names). Then the counting rule, then which of
 * the filters above reach this section at all.
 *
 * A constant rather than text in the template so the sentence survives as one
 * text node: the formatter would otherwise wrap it, and a reader searching the
 * page — or a test matching the phrase — would be looking for words a line
 * break has come between.
 */
const MODULES_CAPTION =
  "Within the last 24 months, over the selected range · counts identified users once per module, " +
  "however many times a module failed for them · family and plan filters apply; variant and " +
  "certification filters do not";

const STYLE_ID = "cts-statistics-insights-styles";

const STYLE_TEXT = css`
  cts-statistics-insights {
    display: block;
    font-family: var(--font-sans);
    color: var(--fg);
    /* Refetch keeps the frame: the previous render stays put and only dims, so
       there is no skeleton flash and no layout jump. The whole block dims, not
       just the distributions — the heatmap is sliced by the range, so a range
       change restates it too, and half a dimmed section reads as a bug. */
    transition: opacity var(--dur-2) var(--ease-standard);
  }
  cts-statistics-insights[busy] {
    opacity: 0.55;
  }
  @media (prefers-reduced-motion: reduce) {
    cts-statistics-insights {
      transition: none;
    }
  }
  .cts-stats-insights-heading {
    margin: var(--space-6) 0 var(--space-3);
    font-size: var(--fs-16);
    font-weight: var(--fw-bold);
    line-height: var(--lh-snug);
    color: var(--fg);
  }
  .cts-stats-insights-lead {
    margin: 0 0 var(--space-3);
    font-size: var(--fs-13);
    color: var(--fg-soft);
  }
  /* Small multiples: the same chart repeated per variant parameter, so they
     must share a track width — a grid, not a flex row that would size each
     one to its own longest label. */
  .cts-stats-dist {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(360px, 100%), 1fr));
    gap: var(--space-5);
  }
  .cts-stats-dist-card {
    padding: var(--space-4);
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    min-width: 0;
  }
  /* The variant band spans the whole row and lays its charts out itself: half
     a dozen parameters stacked in one column would be a tower beside an
     almost-empty neighbour, and small multiples are meant to be read across. */
  .cts-stats-dist-card.is-variants {
    grid-column: 1 / -1;
  }
  .cts-stats-dist-multiples {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(300px, 100%), 1fr));
    gap: var(--space-4) var(--space-5);
  }
  /* The heatmap is 24 columns wide; sharing a two-column grid with a bar
     chart would leave it scrolling inside a half-width card. */
  .cts-stats-insights-card {
    padding: var(--space-4);
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
  }

  /* Two charts side by side wherever there is room for them, with the full
     listing spanning the row underneath: module names are long, so a track
     narrower than this leaves the axis eliding almost every one of them. */
  .cts-stats-modules {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(420px, 100%), 1fr));
    gap: var(--space-5);
  }
  .cts-stats-modules-full {
    grid-column: 1 / -1;
  }
  /* Module names are long, unspaced and hyphenated; without this the first
     column sets the table's width and pushes the four counts off the card on
     a narrow viewport. */
  .cts-stats-modules-table th[scope="row"] {
    overflow-wrap: anywhere;
  }

  details.cts-stats-hosts {
    margin-top: var(--space-6);
  }
  .cts-stats-hint {
    margin: var(--space-2) 0 0;
    font-size: var(--fs-12);
    color: var(--fg-soft);
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

/**
 * Everything on the statistics page below the trend charts: the three
 * distributions (variant usage, certification profiles, entity under test),
 * the most-run and most-failed test modules, the day × hour activity heatmap,
 * and the external servers the suite has been pointed at.
 *
 * A presentational component — it fetches nothing and owns no state. The page
 * hands it slices of one payload it has already memoised, so a re-render for
 * the busy flag hands `<cts-chart>` the same arrays and nothing re-plots.
 *
 * The four groups are scoped differently on purpose, and the component says
 * so rather than letting the reader assume one filter row covers everything:
 *
 * - distributions are counted under the WHOLE query (range and every filter),
 *   so the page withholds them when nothing matches;
 * - modules are the selected RANGE within a trailing 24 months, narrowed by
 *   family and plan ONLY — the server does not key them by variant or
 *   certification profile — which the section's heading and caption state,
 *   and the page withholds them alongside the distributions when nothing
 *   matches (see `_renderInsights` there);
 * - the heatmap is sliced by the RANGE only — the server does not key it by
 *   family — within a trailing 24 months the server keeps cells for, which
 *   its caption states;
 * - external hosts are the same trailing 24 months, unfiltered.
 *
 * Light DOM (`createRenderRoot()` returns `this`) so the page's design-system
 * tokens and stylesheet reach the rendered markup.
 * @property {object|null} distributions - The three distribution charts, as
 *   `Distributions` from `buildDistributions` (typed loosely here because
 *   `lit-analyzer` cannot resolve an imported typedef in an `@property` tag
 *   and would report every binding of it as a type mismatch). Null hides the
 *   whole section: the filters match nothing, or there is no snapshot to
 *   count.
 * @property {object|null} modules - The modules section's inputs, as
 *   `buildModules` returns them (`{rows, byRuns, byFailingUsers}`; typed
 *   loosely for the same `lit-analyzer` reason as `distributions`). Null
 *   hides the whole section; an object with no `rows` renders its empty
 *   state, because "no module ran in this window" is an answer.
 * @property {Array<Array<number>>} heatmap - 7 rows (Mon-Sun) × 24 UTC hours.
 * @property {string} range - The selected range preset's value ("12m"), for
 *   the heatmap caption and the modules heading. Empty leaves both unqualified.
 * @property {Array<{host: string, runs: number, users: number, lastSeen: string}>} hosts -
 *   External servers, busiest first.
 * @property {boolean} narrowed - Whether a family or a plan is selected.
 *   Variant usage is only shown once it is: unfiltered, the suite has getting
 *   on for forty variant parameters and the section would be a wall of charts.
 * @property {boolean} busy - A request is in flight; dim, do not blank.
 *   Reflected to an attribute, and mirrored into `aria-busy` on the host.
 */
class CtsStatisticsInsights extends LitElement {
  static properties = {
    distributions: { attribute: false },
    modules: { attribute: false },
    heatmap: { attribute: false },
    range: { type: String },
    hosts: { attribute: false },
    narrowed: { type: Boolean },
    // Reflected so one CSS rule can dim the whole block; see the stylesheet.
    busy: { type: Boolean, reflect: true },
  };

  constructor() {
    super();
    /** @type {import("./statistics-model.js").Distributions|null} */
    this.distributions = null;
    /** @type {ReturnType<typeof import("./statistics-model.js").buildModules>|null} */
    this.modules = null;
    /** @type {Array<Array<number>>} */
    this.heatmap = [];
    /** @type {string} */
    this.range = "";
    /** @type {Array<{host: string, runs: number, users: number, lastSeen: string}>} */
    this.hosts = [];
    /** @type {boolean} */
    this.narrowed = false;
    /** @type {boolean} */
    this.busy = false;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  /**
   * Mirror the busy flag into `aria-busy` on the host. It cannot be a template
   * binding — this component renders into its own light DOM, so the host is
   * not part of what `render()` produces.
   * @returns {void}
   */
  updated() {
    this.setAttribute("aria-busy", this.busy ? "true" : "false");
  }

  render() {
    return html`
      ${this._renderDistributions()} ${this._renderModules()} ${this._renderHeatmap()}
      ${this._renderHosts()}
    `;
  }

  // --- Distributions ----------------------------------------------------

  /**
   * @returns {unknown} The distributions section, or nothing when the page
   *   has nothing to count under the current query.
   */
  _renderDistributions() {
    const distributions = this.distributions;
    if (!distributions) return nothing;
    const variants = distributions.variants || [];
    const certProfiles = distributions.certProfiles;
    const entities = distributions.entities;
    // Nothing to count at all (a database with no plans yet): a heading over
    // three empty cards says less than no heading.
    if (variants.length === 0 && !certProfiles && !entities) return nothing;

    return html`
      <h2 class="cts-stats-insights-heading">Distributions</h2>
      <div class="cts-stats-dist" data-testid="stats-distributions">
        ${this._renderVariants(variants)}
        ${certProfiles
          ? this._renderDistributionCard(
              "stats-dist-certs",
              "Certification profiles — distinct users",
              "Certification profile",
              certProfiles,
            )
          : nothing}
        ${entities
          ? this._renderDistributionCard(
              "stats-dist-entities",
              // Four orders of magnitude between an OP run count and an AuthZEN one:
              // on a linear axis every entity below the top two is an invisible sliver.
              "Entity under test — runs (log scale)",
              "Entity",
              entities,
              true,
            )
          : nothing}
      </div>
    `;
  }

  /**
   * Variant usage, as small multiples: one chart per plan-level variant
   * parameter, all on the same measure so they can be read against each
   * other.
   * @param {Array<{key: string, distribution: Distribution}>} variants - One per parameter.
   * @returns {unknown} The group.
   */
  _renderVariants(variants) {
    // The same rule the filter row applies to its variant selects: unfiltered,
    // the suite mentions getting on for forty parameters, and forty charts is
    // not a section.
    if (!this.narrowed) {
      return html`
        <div class="cts-stats-dist-card is-variants" data-testid="stats-dist-variants">
          <p class="cts-stats-insights-lead"> Select a family or plan to see variant usage. </p>
        </div>
      `;
    }
    if (variants.length === 0) return nothing;
    return html`
      <div
        class="cts-stats-dist-card is-variants"
        data-testid="stats-dist-variants"
        role="group"
        aria-label="Variant usage by distinct users"
      >
        <p class="cts-stats-insights-lead">
          Variant usage — distinct users. One chart per plan-level variant parameter; a user is
          counted once however many plans they ran with the value.
        </p>
        <div class="cts-stats-dist-multiples">
          ${variants.map(
            (variant) => html`
              <div data-testid="stats-dist-variant-${variant.key}">
                <cts-chart
                  horizontal
                  max-bars=${DISTRIBUTION_LIMIT}
                  heading=${variant.key}
                  category-label="Value"
                  .labels=${variant.distribution.labels}
                  .datasets=${variant.distribution.datasets}
                  .tableExtras=${variant.distribution.extras}
                  .tooltipFooter=${extraFooter(variant.distribution)}
                ></cts-chart>
              </div>
            `,
          )}
        </div>
      </div>
    `;
  }

  /**
   * @param {string} testid - The card's `data-testid`.
   * @param {string} heading - The chart's title.
   * @param {string} categoryLabel - What one bar is, for the table's first column.
   * @param {Distribution} distribution - Its inputs.
   * @param {boolean} [logScale] - Put the value axis on a log scale, for a distribution
   *   spanning orders of magnitude. The heading has to say so: on a log axis bar length
   *   is not proportional to the value.
   * @returns {unknown} One distribution card.
   */
  _renderDistributionCard(testid, heading, categoryLabel, distribution, logScale = false) {
    return html`
      <div class="cts-stats-dist-card" data-testid=${testid}>
        <cts-chart
          horizontal
          ?log-scale=${logScale}
          max-bars=${DISTRIBUTION_LIMIT}
          heading=${heading}
          category-label=${categoryLabel}
          .labels=${distribution.labels}
          .datasets=${distribution.datasets}
          .tableExtras=${distribution.extras}
          .tooltipFooter=${extraFooter(distribution)}
        ></cts-chart>
      </div>
    `;
  }

  // --- Modules ----------------------------------------------------------

  /**
   * The two module rankings and the listing behind them.
   *
   * Two charts rather than one: "most run" and "most users failed" are
   * different questions, and a single chart with two series would put a run
   * count of 1,420 and a user count of 22 on one scale, which reduces the
   * second measure to a stub against the first.
   *
   * Only the top twelve of each ranking is plotted, and — unlike the
   * distributions — the tail is not left to `<cts-chart max-bars>`: the
   * disclosure below carries every module the server returned, with the two
   * measures neither chart plots, so a second per-chart data table of elided
   * module names would be noise.
   * @returns {unknown} The section, or nothing when the page is withholding it.
   */
  _renderModules() {
    const modules = this.modules;
    if (!modules) return nothing;
    const rows = Array.isArray(modules.rows) ? modules.rows : [];
    return html`
      <h2 class="cts-stats-insights-heading">Modules (${this._moduleRange()})</h2>
      <p class="cts-stats-insights-lead">${MODULES_CAPTION}</p>
      <div class="cts-stats-modules" data-testid="stats-modules">
        ${rows.length === 0
          ? html`
              <div class="cts-stats-dist-card cts-stats-modules-full">
                <p class="cts-stats-insights-lead" data-testid="stats-modules-empty">
                  No module runs in this window for the current filters.
                </p>
              </div>
            `
          : html`
              ${this._renderModuleChart("stats-modules-runs", "Most-run modules", modules.byRuns)}
              ${this._renderModuleChart(
                "stats-modules-failing",
                "Modules most users failed",
                modules.byFailingUsers,
              )}
              ${this._renderModuleTable(rows)}
            `}
      </div>
    `;
  }

  /**
   * What the heading says the section covers.
   *
   * The server clips the modules to the selected range — inside the 24 months
   * it keeps cells for — so a heading fixed at "last 24 months" would claim
   * two years of counts while showing one, and a reader comparing a module's
   * runs against the trend charts above would be comparing two windows. The
   * words are the preset's own, so they match the range select.
   *
   * A weekly range is qualified rather than repeated verbatim: module cells
   * are monthly, so the server widens a range of weeks to the whole months
   * its weeks fall in (`ModuleRanker`), and "26 weeks" on its own would be a
   * narrower claim than what is counted.
   * @returns {string} The window, in the range select's own words.
   */
  _moduleRange() {
    const preset = this.range ? rangePreset(this.range) : null;
    // "All time" is the one preset the modules window is narrower than.
    if (!preset || preset.periods <= 0) return "last 24 months";
    return preset.granularity === "week" ? `${preset.label}, whole months` : preset.label;
  }

  /**
   * @param {string} testid - The card's `data-testid`.
   * @param {string} heading - The chart's title.
   * @param {ModuleChart} chart - Its inputs, already ranked and cut.
   * @returns {unknown} One module chart.
   */
  _renderModuleChart(testid, heading, chart) {
    return html`
      <div class="cts-stats-dist-card" data-testid=${testid}>
        <cts-chart
          horizontal
          heading=${heading}
          category-label="Module"
          .labels=${chart.labels}
          .datasets=${chart.datasets}
          .tooltipFooter=${moduleFooter(chart)}
        ></cts-chart>
      </div>
    `;
  }

  /**
   * Every module the section counted, whichever ranking it did or did not
   * make — a disclosure, because it can run to a hundred rows and the charts
   * are the answer most readers came for.
   * @param {Array<ModuleRow>} rows - The payload's modules, server order.
   * @returns {unknown} The listing.
   */
  _renderModuleTable(rows) {
    // Only worth saying when there IS a tail: at twelve modules or fewer both
    // charts already plot every row this table has, and a note about what was
    // cut would send the reader looking for rows that are not there.
    const cut =
      rows.length > MODULE_LIMIT
        ? ` The charts above plot the top ${MODULE_LIMIT} of each ranking; everything past that ` +
          `is only here.`
        : "";
    return html`
      <details
        class="cts-data-disclosure cts-stats-modules-full cts-stats-modules-table"
        data-testid="stats-modules-table"
      >
        <summary>All modules (${NUMBER_FORMAT.format(rows.length)})</summary>
        <p class="cts-stats-hint"> Most-run first, the order the server ranks them in.${cut} </p>
        <table class="cts-data-table">
          <thead>
            <tr>
              <th scope="col">Module</th>
              <th scope="col">Runs</th>
              <th scope="col">Users</th>
              <th scope="col">Users who hit a failure</th>
              <th scope="col">Failing share</th>
            </tr>
          </thead>
          <tbody>
            ${rows.map(
              (row) => html`
                <tr>
                  <th scope="row">${row.testName}</th>
                  <td>${NUMBER_FORMAT.format(Number(row.runs) || 0)}</td>
                  <td>${NUMBER_FORMAT.format(Number(row.users) || 0)}</td>
                  <td>${NUMBER_FORMAT.format(Number(row.failingUsers) || 0)}</td>
                  <td>${formatShare(row.failingShare)}</td>
                </tr>
              `,
            )}
          </tbody>
        </table>
      </details>
    `;
  }

  // --- Activity ---------------------------------------------------------

  /**
   * @returns {unknown} The activity heatmap section.
   */
  _renderHeatmap() {
    const range = this.range ? ` (${rangePreset(this.range).label})` : "";
    return html`
      <h2 class="cts-stats-insights-heading">Activity (UTC)</h2>
      <div class="cts-stats-insights-card">
        <cts-heatmap
          data-testid="stats-heatmap"
          heading="Test runs by day and hour"
          caption=${`All hours are UTC. Within the last 24 months, sliced by the selected ` +
          `range${range} only — not by family, plan, variant or certification profile.`}
          value-label="runs"
          .rows=${DAY_LABELS}
          .cols=${HOUR_LABELS}
          .values=${this.heatmap}
          .cellTitle=${heatmapCellTitle}
        ></cts-heatmap>
      </div>
    `;
  }

  // --- External servers -------------------------------------------------

  /**
   * The servers the suite has been pointed at over the last 24 months. A
   * disclosure, not a section: it is a long list that answers a question an
   * admin asks occasionally, and it must not compete with the charts above
   * it.
   * @returns {unknown} The disclosure, or nothing when there are none.
   */
  _renderHosts() {
    const hosts = Array.isArray(this.hosts) ? this.hosts : [];
    if (hosts.length === 0) return nothing;
    return html`
      <details class="cts-data-disclosure cts-stats-hosts" data-testid="stats-hosts">
        <summary>External servers under test (${NUMBER_FORMAT.format(hosts.length)})</summary>
        <p class="cts-stats-hint">
          The top 100 by runs over the last 24 months — never scoped by the range or the filters
          above. Hosts are read from the server, issuer, credential-issuer and entity-identifier
          URLs in each test's configuration; the suite's own endpoints are excluded.
        </p>
        <table class="cts-data-table">
          <thead>
            <tr>
              <th scope="col">Host</th>
              <th scope="col">Runs</th>
              <th scope="col">Users</th>
              <th scope="col">Last seen</th>
            </tr>
          </thead>
          <tbody>
            ${hosts.map(
              (host) => html`
                <tr>
                  <th scope="row">${host.host}</th>
                  <td>${NUMBER_FORMAT.format(Number(host.runs) || 0)}</td>
                  <td>${NUMBER_FORMAT.format(Number(host.users) || 0)}</td>
                  <td><cts-time value=${host.lastSeen || ""}></cts-time></td>
                </tr>
              `,
            )}
          </tbody>
        </table>
      </details>
    `;
  }
}

customElements.define("cts-statistics-insights", CtsStatisticsInsights);

export {};
