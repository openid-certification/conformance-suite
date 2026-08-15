import { html } from "lit";
import { expect, spyOn, waitFor } from "storybook/test";
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
      expect(chart.data.datasets[0].borderWidth).toBe(2);
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
