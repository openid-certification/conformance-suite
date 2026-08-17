import { html } from "lit";
import { expect, spyOn, userEvent, waitFor } from "storybook/test";
import "./cts-chart.js";
import { __resetChartLoaderForTests, __setChartJsUrlForTests } from "./cts-chart.js";

// Eight months of test runs, folded into the three families the statistics
// page shows plus the neutral "Other" bucket — the shape Task 5 renders.
const MONTHS = [
  "2025-12",
  "2026-01",
  "2026-02",
  "2026-03",
  "2026-04",
  "2026-05",
  "2026-06",
  "2026-07",
];

const STACKED_DATASETS = [
  { label: "FAPI 2.0", data: [412, 480, 523, 611, 590, 702, 745, 690], colorVar: "--chart-cat-1" },
  {
    label: "OpenID Connect",
    data: [301, 288, 340, 355, 392, 371, 402, 388],
    colorVar: "--chart-cat-2",
  },
  { label: "Other", data: [96, 120, 88, 143, 131, 167, 152, 174], colorVar: "--chart-other" },
];

const LINE_DATASETS = [
  { label: "Passed", data: [388, 451, 502, 577, 566, 668, 701, 654], colorVar: "--chart-cat-1" },
  { label: "Failed", data: [24, 29, 21, 34, 24, 34, 44, 36], colorVar: "--chart-cat-2" },
];

// Fifteen certification profiles, so a top-12 chart has a tail to leave out,
// and one deliberately long name so the axis tick has something to elide.
const LONG_PROFILE = "Profile 05 — Brazil Open Finance with a very long name";
const PROFILE_LABELS = Array.from({ length: 15 }, (_, i) =>
  i === 5 ? LONG_PROFILE : `Profile ${String(i).padStart(2, "0")}`,
);

const PROFILE_DATASETS = [
  {
    label: "Users",
    data: Array.from({ length: 15 }, (_, i) => 60 - i * 4),
    colorVar: "--chart-cat-1",
  },
];

// The plan count is context for the user count, not a second thing being
// compared, so it rides in the data table rather than the plot.
const PROFILE_EXTRAS = [
  { label: "Plans", data: Array.from({ length: 15 }, (_, i) => 120 - i * 8) },
];

// The families the "Other" bucket folds together, keyed by month index — the
// tooltipFooter contract Task 5 uses so a reader can see what "Other" means
// without leaving the chart.
const OTHER_FAMILIES = [
  ["CIBA", "eKYC"],
  ["CIBA", "eKYC", "SSF"],
  ["CIBA"],
  ["CIBA", "eKYC", "VCI"],
  ["eKYC", "VCI"],
  ["CIBA", "VCI", "VP"],
  ["CIBA", "eKYC", "VCI", "VP"],
  ["VCI", "VP"],
];

/**
 * Wait until Chart.js has finished its lazy load and attached an instance to
 * the canvas. `Chart.getChart(canvas)` is Chart.js's own public lookup, so
 * this asserts the real load-and-create path rather than a private field.
 * @param {Element} canvasElement - The story canvas.
 * @returns {Promise<{canvas: HTMLCanvasElement, chart: any}>} The rendered canvas and its chart.
 */
async function waitForChart(canvasElement) {
  const canvas = /** @type {HTMLCanvasElement} */ (canvasElement.querySelector("cts-chart canvas"));
  expect(canvas).toBeTruthy();
  let chart = null;
  await waitFor(() => {
    chart = window.Chart?.getChart(canvas);
    expect(chart).toBeTruthy();
  });
  return { canvas, chart };
}

export default {
  title: "Components/cts-chart",
  component: "cts-chart",
  argTypes: {
    type: { control: "select", options: ["bar", "line"] },
    stacked: { control: "boolean" },
    heading: { control: "text" },
    categoryLabel: { control: "text" },
  },
};

// --- Stories ---

/**
 * The statistics-page default: a stacked bar chart of three series over eight
 * months, with a `tooltipFooter` naming the families folded into "Other".
 * The `<details>` data table below the plot carries the exact values — it is
 * the accessible twin of the chart, not a fallback.
 */
export const Default = {
  args: {
    type: "bar",
    stacked: true,
    heading: "Test runs by specification family",
    categoryLabel: "Month",
  },
  render: ({ type, stacked, heading, categoryLabel }) => html`
    <cts-chart
      type=${type}
      ?stacked=${stacked}
      heading=${heading}
      category-label=${categoryLabel}
      .labels=${MONTHS}
      .datasets=${STACKED_DATASETS}
      .tooltipFooter=${(index) => [`Other includes: ${OTHER_FAMILIES[index].join(", ")}`]}
    ></cts-chart>
  `,

  async play({ canvasElement, step }) {
    const { canvas, chart } = await waitForChart(canvasElement);

    await step("the figure is named by its own heading", async () => {
      const figure = canvasElement.querySelector("cts-chart figure.cts-chart");
      const h3 = canvasElement.querySelector("cts-chart h3.cts-chart-heading");
      expect(h3.id).toBeTruthy();
      expect(figure.getAttribute("aria-labelledby")).toBe(h3.id);
      expect(h3.textContent).toBe("Test runs by specification family");
    });

    await step("canvas exposes an image role and a descriptive label", async () => {
      expect(canvas.getAttribute("role")).toBe("img");
      expect(canvas.getAttribute("aria-label")).toBe(
        "Test runs by specification family: bar chart of 8 periods across 3 series. " +
          "Data table available below.",
      );
    });

    await step("chart is a stacked bar with a legend for its three series", async () => {
      expect(chart.config.type).toBe("bar");
      expect(chart.options.scales.x.stacked).toBe(true);
      expect(chart.options.scales.y.stacked).toBe(true);
      expect(chart.options.plugins.legend.display).toBe(true);
      // The legend key mirrors the mark: a rect for bars.
      expect(chart.options.plugins.legend.labels.usePointStyle).toBe(false);
      // colorVar is resolved off :root, not passed through as a var() string.
      expect(chart.data.datasets[0].backgroundColor).toBe("#2a78d6");
      // Bar borders are painted in the surface colour: they ARE the 2px gap
      // separating stacked segments, not a stroke around the mark.
      expect(chart.data.datasets[0].borderColor).toBe("#FFFFFF");
      // Regression guard: the gap sits on the side facing the next segment
      // only, never on the category sides (Chart.js already spaces categories,
      // and at 90 monthly bars a bar is about 4px wide, so two 2px side
      // borders painted the whole plot surface-on-surface) - and a segment
      // with no value gets no border at all, or an empty bucket would paint a
      // solid surface-coloured band across every bar.
      const borderWidth = chart.data.datasets[0].borderWidth;
      expect(borderWidth({ raw: 120 })).toEqual({ top: 2, bottom: 0, left: 0, right: 0 });
      expect(borderWidth({ raw: 0 })).toEqual({ top: 0, bottom: 0, left: 0, right: 0 });
    });

    await step("the category axis renders the labels, not row numbers", async () => {
      // Regression guard: setting `ticks.callback: undefined` on a category
      // scale overrides Chart.js's own formatter and leaves an axis of
      // indices — invisible to every assertion that reads the data table.
      expect(chart.scales.x.type).toBe("category");
      expect(chart.scales.x.ticks.map((/** @type {any} */ tick) => tick.label)).toEqual(MONTHS);
      // The column is the hit target, and on a vertical chart that is the x
      // axis — the mirror of the horizontal story's assertion.
      expect(chart.options.interaction.axis).toBe("x");
      // Counts, so the value axis ticks are whole numbers even when the
      // biggest value is 1.
      expect(chart.options.scales.y.ticks.precision).toBe(0);
    });

    await step("tooltip lists every series at the hovered label, values leading", async () => {
      expect(chart.options.plugins.tooltip.mode).toBe("index");
      const { label, footer } = chart.options.plugins.tooltip.callbacks;
      expect(label({ formattedValue: "412", dataset: { label: "FAPI 2.0" } })).toBe(
        "412  FAPI 2.0",
      );
      expect(footer([{ dataIndex: 1 }])).toEqual(["Other includes: CIBA, eKYC, SSF"]);
    });

    await step("data table has a column per series plus the category column", async () => {
      const table = canvasElement.querySelector("cts-chart .cts-chart-data table");
      expect(table).toBeTruthy();
      // No .trim(): the template must not leak whitespace into the caption.
      expect(table.querySelector("caption").textContent).toBe("Test runs by specification family");
      const colHeaders = [...table.querySelectorAll('th[scope="col"]')].map((th) =>
        th.textContent.trim(),
      );
      expect(colHeaders).toEqual(["Month", "FAPI 2.0", "OpenID Connect", "Other"]);
      expect(colHeaders.length).toBe(STACKED_DATASETS.length + 1);

      const rows = [...table.querySelectorAll("tbody tr")];
      expect(rows.length).toBe(MONTHS.length);
      const firstRow = [...rows[0].querySelectorAll("th, td")].map((cell) =>
        cell.textContent.trim(),
      );
      expect(firstRow).toEqual(["2025-12", "412", "301", "96"]);
      expect(rows[0].querySelector("th").getAttribute("scope")).toBe("row");
    });

    // Kept last: it mutates the host, so earlier steps see the pristine render.
    await step("only plot-affecting changes reach Chart.js", async () => {
      const host = canvasElement.querySelector("cts-chart");
      const updateSpy = spyOn(chart, "update");

      host.heading = "Renamed heading";
      await host.updateComplete;
      // The markup re-rendered...
      expect(canvasElement.querySelector("cts-chart h3").textContent).toBe("Renamed heading");
      // ...but a heading is not plot data, so the chart was left alone (no
      // replayed animation, no computed-style re-read).
      expect(updateSpy).not.toHaveBeenCalled();

      host.labels = [...MONTHS].reverse();
      await host.updateComplete;
      expect(updateSpy).toHaveBeenCalled();
      expect(chart.data.labels[0]).toBe("2026-07");
    });
  },
};

/**
 * Two series over time as lines. A `line` chart still gets the legend (two or
 * more series) and the same index-mode tooltip, so the reader never has to
 * land on a 2px stroke to read a value.
 */
export const Line = {
  args: {
    type: "line",
    stacked: false,
    heading: "Test outcomes over time",
    categoryLabel: "Month",
  },
  render: ({ type, stacked, heading, categoryLabel }) => html`
    <cts-chart
      type=${type}
      ?stacked=${stacked}
      heading=${heading}
      category-label=${categoryLabel}
      .labels=${MONTHS}
      .datasets=${LINE_DATASETS}
    ></cts-chart>
  `,

  async play({ canvasElement, step }) {
    const { canvas, chart } = await waitForChart(canvasElement);

    await step("canvas label names the line form and the series count", async () => {
      expect(canvas.getAttribute("aria-label")).toBe(
        "Test outcomes over time: line chart of 8 periods across 2 series. " +
          "Data table available below.",
      );
    });

    await step("line marks carry a 2px stroke and surface-ringed points", async () => {
      expect(chart.config.type).toBe("line");
      const [passed] = chart.data.datasets;
      expect(passed.borderColor).toBe("#2a78d6");
      expect(passed.borderWidth).toBe(2);
      expect(passed.pointRadius).toBe(4);
      expect(passed.pointBorderColor).toBe("#FFFFFF");
      expect(passed.pointBorderWidth).toBe(2);
      expect(chart.options.plugins.legend.display).toBe(true);
      // The legend key mirrors the mark: a short stroke for lines.
      expect(chart.options.plugins.legend.labels.usePointStyle).toBe(true);
      expect(chart.options.plugins.legend.labels.pointStyle).toBe("line");
    });

    await step("data table mirrors the two series", async () => {
      const table = canvasElement.querySelector("cts-chart .cts-chart-data table");
      const colHeaders = [...table.querySelectorAll('th[scope="col"]')].map((th) =>
        th.textContent.trim(),
      );
      expect(colHeaders).toEqual(["Month", "Passed", "Failed"]);
    });
  },
};

/**
 * A single-series horizontal bar chart — the form the statistics page's
 * distributions use, because a certification profile name is unreadable
 * rotated under a column.
 *
 * It exercises the three properties that form needs: `horizontal` (the
 * category axis moves to y and the frame's height follows the row count),
 * `max-bars` (only the leading rows are plotted; the data table keeps every
 * one and a note says so) and `tableExtras` (a second measure that is context
 * rather than the thing being compared, so it belongs in the table and not in
 * the plot).
 */
/**
 * A distribution whose categories are orders of magnitude apart: on a linear axis
 * everything below the top one or two is an invisible sliver, so the value axis goes
 * logarithmic and the heading says so.
 */
export const LogScale = {
  args: {
    heading: "Entity under test — runs (log scale)",
    categoryLabel: "Entity",
  },
  render: ({ heading, categoryLabel }) => html`
    <cts-chart
      horizontal
      log-scale
      max-bars="12"
      heading=${heading}
      category-label=${categoryLabel}
      .labels=${["Provider", "Relying party", "VCI issuer", "AuthZEN PDP"]}
      .datasets=${[
        {
          label: "Runs",
          colorVar: "--chart-cat-1",
          data: [4379734, 2341305, 11988, 810],
        },
      ]}
    ></cts-chart>
  `,

  async play({ canvasElement, step }) {
    const { chart } = await waitForChart(canvasElement);

    await step("the value axis is logarithmic and starts at one", async () => {
      expect(chart.options.scales.x.type).toBe("logarithmic");
      // A log axis has no zero to begin at; Chart.js drops non-positive values,
      // so the floor is one. (beginAtZero is not asserted: Chart.js resolves it
      // to its own default whatever we configure, and ignores it on a log axis.)
      expect(chart.options.scales.x.min).toBe(1);
    });

    await step("every category is still plotted, four decades apart", async () => {
      expect(chart.data.labels).toHaveLength(4);
      // The smallest bar is 1/5000th of the largest: on a linear axis it would
      // round to nothing, which is what the log scale exists to avoid.
      const rows = canvasElement.querySelectorAll(".cts-chart-table tbody tr");
      expect(rows).toHaveLength(4);
    });
  },
};

export const Horizontal = {
  args: {
    heading: "Certification profiles — distinct users",
    categoryLabel: "Certification profile",
  },
  render: ({ heading, categoryLabel }) => html`
    <cts-chart
      horizontal
      max-bars="12"
      heading=${heading}
      category-label=${categoryLabel}
      .labels=${PROFILE_LABELS}
      .datasets=${PROFILE_DATASETS}
      .tableExtras=${PROFILE_EXTRAS}
    ></cts-chart>
  `,

  async play({ canvasElement, step }) {
    const { canvas, chart } = await waitForChart(canvasElement);

    await step("the category axis is y and the value axis carries the grid", async () => {
      expect(chart.options.indexAxis).toBe("y");
      // Index mode measures distance in x unless told otherwise, which on a
      // horizontal chart selects the row whose VALUE is nearest the cursor
      // rather than the row the pointer is over.
      expect(chart.options.interaction.axis).toBe("y");
      expect(chart.options.plugins.tooltip.axis).toBe("y");
      // Gridlines belong to the value axis; on the categorical one they add
      // ink without carrying a value.
      expect(chart.options.scales.y.grid.display).toBe(false);
      expect(chart.options.scales.x.beginAtZero).toBe(true);
      // The surface gap follows the stacking axis, which flips with the bars.
      const borderWidth = chart.data.datasets[0].borderWidth;
      expect(borderWidth({ raw: 120 })).toEqual({ left: 0, right: 2, top: 0, bottom: 0 });
      expect(borderWidth({ raw: 0 })).toEqual({ left: 0, right: 0, top: 0, bottom: 0 });
      // Every row is named: the frame is sized from the row count precisely
      // so Chart.js never has to thin them out.
      expect(chart.options.scales.y.ticks.autoSkip).toBe(false);
    });

    await step("one series means one hue and no legend box", async () => {
      expect(chart.data.datasets.length).toBe(1);
      expect(chart.data.datasets[0].backgroundColor).toBe("#2a78d6");
      // The title already names what is plotted; a box with one swatch would
      // only restate it.
      expect(chart.options.plugins.legend.display).toBe(false);
    });

    await step("the frame's height grows with the number of rows", async () => {
      const frame = canvasElement.querySelector("cts-chart .cts-chart-frame");
      expect(frame.classList.contains("is-horizontal")).toBe(true);
      expect(frame.getAttribute("style")).toMatch(/--cts-chart-rows:\s*12/);
    });

    await step("only the twelve biggest are plotted, and it says so", async () => {
      expect(chart.data.labels.length).toBe(12);
      expect(chart.data.labels[0]).toBe("Profile 00");
      expect(canvas.getAttribute("aria-label")).toBe(
        "Certification profiles — distinct users: bar chart of 12 categories across 1 series. " +
          "Data table available below.",
      );
      const note = canvasElement.querySelector('[data-testid="cts-chart-more"]');
      expect(note.textContent.replace(/\s+/g, " ").trim()).toBe(
        "Showing the top 12 of 15; the rest are in the data table.",
      );
    });

    await step("a long name is elided on the axis, never clipped", async () => {
      // The tick callback reads the label out of the chart's own list by
      // index, so it is called the way Chart.js calls it: (value, index).
      const tickAt = (/** @type {number} */ index) =>
        chart.options.scales.y.ticks.callback(index, index);
      expect(tickAt(0)).toBe("Profile 00");
      const longTick = tickAt(chart.data.labels.indexOf(LONG_PROFILE));
      // Elided, never clipped: the full name is still in the tooltip and the
      // data table, and the axis column cannot swallow the plot.
      expect(longTick.endsWith("…")).toBe(true);
      expect(longTick.length).toBeLessThanOrEqual(28);
      expect(LONG_PROFILE.startsWith(longTick.slice(0, -1))).toBe(true);
    });

    await step("the data table is complete, extra column included", async () => {
      const table = canvasElement.querySelector("cts-chart .cts-chart-data table");
      const colHeaders = [...table.querySelectorAll('th[scope="col"]')].map((th) =>
        th.textContent.trim(),
      );
      expect(colHeaders).toEqual(["Certification profile", "Users", "Plans"]);
      const rows = [...table.querySelectorAll("tbody tr")];
      // Every row, not just the plotted twelve — capping the bars hides
      // nothing.
      expect(rows.length).toBe(15);
      const lastRow = [...rows[14].querySelectorAll("th, td")].map((cell) =>
        cell.textContent.trim(),
      );
      expect(lastRow).toEqual(["Profile 14", "4", "8"]);
    });
  },
};

/**
 * The opt-in `clickable` form: the statistics page's drill-down. A click on a
 * mark names the series it landed on; a click inside the band but not on a
 * mark names the category alone (`datasetIndex: null`), because that is all
 * the reader pointed at.
 *
 * The plot's hit targets live on a `<canvas>`, which no keyboard can reach —
 * so every row of the data table gets a button in its category cell emitting
 * the same "whole category" event. That is the keyboard and screen-reader
 * twin of the click, not a lesser version of it.
 */
export const Clickable = {
  args: {
    heading: "Test runs by specification family",
    categoryLabel: "Month",
  },
  render: ({ heading, categoryLabel }) => html`
    <cts-chart
      stacked
      clickable
      click-label="List the test plans in"
      heading=${heading}
      category-label=${categoryLabel}
      .labels=${MONTHS}
      .datasets=${STACKED_DATASETS}
    ></cts-chart>
  `,

  async play({ canvasElement, step }) {
    const { canvas, chart } = await waitForChart(canvasElement);
    const host = canvasElement.querySelector("cts-chart");
    /** @type {Array<any>} */
    const events = [];
    host.addEventListener("cts-chart-click", (/** @type {any} */ evt) => events.push(evt.detail));

    /**
     * Dispatch a real mouse event at a point in canvas space. Chart.js reads
     * the position off the native event, so this exercises its own hit
     * testing rather than a private entry point.
     * @param {string} type - Event type.
     * @param {number} x - Canvas-space x.
     * @param {number} y - Canvas-space y.
     * @returns {void}
     */
    const fire = (type, x, y) => {
      const rect = canvas.getBoundingClientRect();
      canvas.dispatchEvent(
        new MouseEvent(type, {
          clientX: rect.left + x,
          clientY: rect.top + y,
          bubbles: true,
          cancelable: true,
        }),
      );
    };

    /**
     * The vertical middle of one bar segment. A bar's own `y` is its data
     * END — the top edge — so aiming at it lands on the boundary rather than
     * inside the mark.
     * @param {any} bar - A Chart.js bar element.
     * @returns {number} Canvas-space y inside it.
     */
    const midOf = (bar) => (bar.y + bar.base) / 2;

    // A click is hit-tested against where the marks ARE, and Chart.js animates
    // them in from the baseline on first render. The story iframe's
    // requestAnimationFrame barely ticks in headless Chromium, so that entry
    // animation would still be sitting on the axis when the clicks below
    // land — every one of them would miss. Turn animation off in the chart's
    // own config, drop the entry animation already queued, and jump to the
    // final layout: the state a real reader clicks at.
    chart.config.options.animation = false;
    chart.stop();
    await waitFor(() => {
      chart.update("none");
      const bar = chart.getDatasetMeta(1).data[3];
      expect(bar.base - bar.y).toBeGreaterThan(1);
    });

    await step("the pointer says the marks are targets", async () => {
      const bar = chart.getDatasetMeta(0).data[2];
      fire("mousemove", bar.x, midOf(bar));
      await waitFor(() => {
        expect(canvas.style.cursor).toBe("pointer");
      });

      // Chart.js reports a hover only inside the chart area, so leaving the
      // canvas off a bar would otherwise leave the hand behind on a chart
      // nobody is pointing at.
      canvas.dispatchEvent(new MouseEvent("mouseleave", { bubbles: false }));
      await waitFor(() => {
        expect(canvas.style.cursor).toBe("");
      });
    });

    await step("a click on a mark names the period AND its series", async () => {
      const bar = chart.getDatasetMeta(1).data[3];
      fire("click", bar.x, midOf(bar));
      await waitFor(() => {
        expect(events.length).toBe(1);
      });
      expect(events[0]).toEqual({
        periodIndex: 3,
        datasetIndex: 1,
        datasetLabel: "OpenID Connect",
        label: "2026-03",
      });
    });

    await step("a click in the band but not on a mark names the period only", async () => {
      const bar = chart.getDatasetMeta(0).data[5];
      // Two pixels below the top of the plot area: inside the column, above
      // every stacked segment in it.
      fire("click", bar.x, chart.chartArea.top + 2);
      await waitFor(() => {
        expect(events.length).toBe(2);
      });
      expect(events[1]).toEqual({
        periodIndex: 5,
        datasetIndex: null,
        datasetLabel: "",
        label: "2026-05",
      });
    });

    await step("every data-table row carries the same action for the keyboard", async () => {
      const buttons = canvasElement.querySelectorAll("cts-chart .cts-chart-row-link");
      expect(buttons.length).toBe(MONTHS.length);
      // Named by what activating it does, not by the bare label — "2025-12,
      // button" would tell a screen-reader user nothing.
      expect(buttons[0].getAttribute("aria-label")).toBe("List the test plans in 2025-12");
      expect(buttons[0].textContent.trim()).toBe("2025-12");
      expect(buttons[0].getAttribute("type")).toBe("button");

      await userEvent.click(buttons[6]);
      await waitFor(() => {
        expect(events.length).toBe(3);
      });
      // A row is a period, not a series, so it emits the same "whole period"
      // shape a click inside the band does.
      expect(events[2]).toEqual({
        periodIndex: 6,
        datasetIndex: null,
        datasetLabel: "",
        label: "2026-06",
      });
    });
  },
};

/**
 * When the Chart.js bundle fails to load the plot is replaced by a warning
 * alert and the data table stays exactly as it was — every value remains
 * reachable, so a failed CDN/network fetch degrades the page rather than
 * blanking it.
 *
 * The `beforeEach` cleanup restores the loader singleton and the real vendored
 * URL, so story order does not matter.
 */
export const LoadFailure = {
  args: {
    type: "bar",
    stacked: true,
    heading: "Test runs by specification family",
    categoryLabel: "Month",
  },
  beforeEach() {
    // Sibling stories that already ran in this browser session populated the
    // loader singleton and window.Chart; drop both so the wrapper re-attempts
    // a cold load against the 404 URL below.
    const saved = window.Chart;
    delete window.Chart;
    __resetChartLoaderForTests();
    __setChartJsUrlForTests("/vendor/chart.js/does-not-exist.js");
    return () => {
      __setChartJsUrlForTests(null);
      __resetChartLoaderForTests();
      if (saved) window.Chart = saved;
    };
  },
  render: ({ type, stacked, heading, categoryLabel }) => html`
    <cts-chart
      type=${type}
      ?stacked=${stacked}
      heading=${heading}
      category-label=${categoryLabel}
      .labels=${MONTHS}
      .datasets=${STACKED_DATASETS}
    ></cts-chart>
  `,

  async play({ canvasElement, step }) {
    await step("a warning alert replaces the plot", async () => {
      await waitFor(() => {
        expect(canvasElement.querySelector("cts-chart .oidf-alert-warning")).toBeTruthy();
      });
      const alert = canvasElement.querySelector("cts-chart .oidf-alert-warning");
      expect(alert.getAttribute("role")).toBe("alert");
      expect(alert.textContent).toContain("Chart could not be rendered");
      expect(canvasElement.querySelector("cts-chart canvas")).toBeNull();
      // The failed tag is removed, so retries cannot accumulate dead scripts.
      expect(document.querySelectorAll('script[src*="does-not-exist"]').length).toBe(0);
    });

    await step("the data table is unaffected and complete", async () => {
      const table = canvasElement.querySelector("cts-chart .cts-chart-data table");
      expect(table).toBeTruthy();
      const colHeaders = [...table.querySelectorAll('th[scope="col"]')].map((th) =>
        th.textContent.trim(),
      );
      expect(colHeaders).toEqual(["Month", "FAPI 2.0", "OpenID Connect", "Other"]);
      expect(table.querySelectorAll("tbody tr").length).toBe(MONTHS.length);
      const lastRow = [...table.querySelectorAll("tbody tr")].at(-1);
      expect([...lastRow.querySelectorAll("th, td")].map((c) => c.textContent.trim())).toEqual([
        "2026-07",
        "690",
        "388",
        "174",
      ]);
    });
  },
};
