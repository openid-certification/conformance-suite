import { LitElement, html, nothing, css } from "lit";
import "./cts-chart.js";
import "./cts-heatmap.js";
import "./cts-time.js";
import { DISTRIBUTION_LIMIT } from "./statistics-model.js";

/** @typedef {import("./statistics-model.js").Distribution} Distribution */
/** @typedef {import("./statistics-model.js").Distributions} Distributions */

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

/** Grouped figures, so 1,024 does not read as 1024. */
const NUMBER_FORMAT = new Intl.NumberFormat();

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

  details.cts-stats-hosts {
    margin-top: var(--space-6);
  }
  .cts-stats-hosts > summary {
    cursor: pointer;
    font-size: var(--fs-13);
    color: var(--fg-muted);
  }
  .cts-stats-hosts > summary:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
    border-radius: var(--radius-2);
  }
  .cts-stats-hosts-hint {
    margin: var(--space-2) 0 0;
    font-size: var(--fs-12);
    color: var(--fg-soft);
  }
  /* Repeated rather than borrowed from <cts-chart>: this table renders in
     states where the page draws no chart at all (a filter that matches
     nothing), so it cannot depend on that component's stylesheet. */
  .cts-stats-table {
    width: 100%;
    margin-top: var(--space-2);
    border-collapse: collapse;
    font-size: var(--fs-13);
    font-variant-numeric: tabular-nums;
  }
  .cts-stats-table th,
  .cts-stats-table td {
    padding: var(--space-2) var(--space-3);
    border-bottom: 1px solid var(--border);
    text-align: right;
  }
  .cts-stats-table th[scope="col"]:first-child,
  .cts-stats-table th[scope="row"] {
    text-align: left;
    font-weight: var(--fw-regular);
  }
  .cts-stats-table thead th {
    color: var(--fg-soft);
    font-weight: var(--fw-bold);
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
 * Everything on the statistics page below the trend charts: the three
 * distributions (variant usage, certification profiles, entity under test),
 * the day × hour activity heatmap, and the external servers the suite has
 * been pointed at.
 *
 * A presentational component — it fetches nothing and owns no state. The page
 * hands it slices of one payload it has already memoised, so a re-render for
 * the busy flag hands `<cts-chart>` the same arrays and nothing re-plots.
 *
 * The three groups are scoped differently on purpose, and the component says
 * so rather than letting the reader assume one filter row covers everything:
 *
 * - distributions are counted under the WHOLE query (range and every filter),
 *   so the page withholds them when nothing matches;
 * - the heatmap is sliced by the RANGE only — the server does not key it by
 *   family — which its caption states;
 * - external hosts are all-time and unfiltered.
 *
 * Light DOM (`createRenderRoot()` returns `this`) so the page's design-system
 * tokens and stylesheet reach the rendered markup.
 * @property {object|null} distributions - The three distribution charts, as
 *   `Distributions` from `buildDistributions` (typed loosely here because
 *   `lit-analyzer` cannot resolve an imported typedef in an `@property` tag
 *   and would report every binding of it as a type mismatch). Null hides the
 *   whole section: the filters match nothing, or there is no snapshot to
 *   count.
 * @property {Array<Array<number>>} heatmap - 7 rows (Mon-Sun) × 24 UTC hours.
 * @property {string} rangeLabel - The selected range preset's label, for the
 *   heatmap caption ("12 months").
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
    heatmap: { attribute: false },
    rangeLabel: { type: String, attribute: "range-label" },
    hosts: { attribute: false },
    narrowed: { type: Boolean },
    // Reflected so one CSS rule can dim the whole block; see the stylesheet.
    busy: { type: Boolean, reflect: true },
  };

  constructor() {
    super();
    /** @type {import("./statistics-model.js").Distributions|null} */
    this.distributions = null;
    /** @type {Array<Array<number>>} */
    this.heatmap = [];
    /** @type {string} */
    this.rangeLabel = "";
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
    return html` ${this._renderDistributions()} ${this._renderHeatmap()} ${this._renderHosts()} `;
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
              "Entity under test — runs",
              "Entity",
              entities,
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
   * @returns {unknown} One distribution card.
   */
  _renderDistributionCard(testid, heading, categoryLabel, distribution) {
    return html`
      <div class="cts-stats-dist-card" data-testid=${testid}>
        <cts-chart
          horizontal
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

  // --- Activity ---------------------------------------------------------

  /**
   * @returns {unknown} The activity heatmap section.
   */
  _renderHeatmap() {
    const range = this.rangeLabel ? ` (${this.rangeLabel})` : "";
    return html`
      <h2 class="cts-stats-insights-heading">Activity (UTC)</h2>
      <div class="cts-stats-insights-card">
        <cts-heatmap
          data-testid="stats-heatmap"
          heading="Test runs by day and hour"
          caption=${`All hours are UTC. Sliced by the selected range${range} only — not by family, ` +
          `plan, variant or certification profile.`}
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
   * The servers the suite has been pointed at. A disclosure, not a section:
   * it is a long, all-time list that answers a question an admin asks
   * occasionally, and it must not compete with the charts above it.
   * @returns {unknown} The disclosure, or nothing when there are none.
   */
  _renderHosts() {
    const hosts = Array.isArray(this.hosts) ? this.hosts : [];
    if (hosts.length === 0) return nothing;
    return html`
      <details class="cts-stats-hosts" data-testid="stats-hosts">
        <summary>External servers under test (${NUMBER_FORMAT.format(hosts.length)})</summary>
        <p class="cts-stats-hosts-hint">
          The top 100 by runs, over the whole history of the database — never scoped by the range or
          the filters above. Hosts are read from the server, issuer, credential-issuer and
          entity-identifier URLs in each test's configuration; the suite's own endpoints are
          excluded.
        </p>
        <table class="cts-stats-table">
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
