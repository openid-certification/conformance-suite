import { html } from "lit";
import { expect } from "storybook/test";
import "./cts-heatmap.js";

/** Monday-first days, the order `StatisticsOverview.heatmap` delivers. */
const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

/** The 24 UTC hours as two-digit axis labels. */
const HOURS = Array.from({ length: 24 }, (_, hour) => String(hour).padStart(2, "0"));

/**
 * Office hours on weekdays, a fifth of that at the weekend, near-nothing
 * overnight — the shape the real database has, and the one that makes a
 * linear ramp useless (which is why `heatmapIntensity` is square-root
 * scaled).
 * @type {Array<Array<number>>}
 */
const VALUES = DAYS.map((_, day) =>
  HOURS.map((_hour, hour) => {
    const workday = day < 5 ? 1 : 0.2;
    const office = hour >= 7 && hour <= 18 ? 1 : 0.15;
    return Math.round(120 * workday * office + (hour % 5) * 3);
  }),
);

/** A database that has recorded nothing in the selected range. */
const EMPTY_VALUES = DAYS.map(() => HOURS.map(() => 0));

/**
 * @param {number} row - Day index.
 * @param {number} col - Hour index.
 * @param {number} value - The count.
 * @returns {string} The cell's hover text.
 */
const cellTitle = (row, col, value) => `${DAYS[row]} ${HOURS[col]}:00 UTC — ${value} runs`;

export default {
  title: "Components/cts-heatmap",
  component: "cts-heatmap",
  argTypes: {
    heading: { control: "text" },
    caption: { control: "text" },
    valueLabel: { control: "text" },
  },
};

// --- Stories ---

/**
 * The statistics page's activity chart: 7 days × 24 UTC hours on a sequential
 * single-hue ramp, with the exact counts in the cell tooltips and the data
 * table below.
 */
export const Default = {
  args: {
    heading: "Test runs by day and hour",
    caption: "All hours are UTC. Sliced by the selected range only.",
    valueLabel: "runs",
  },
  render: ({ heading, caption, valueLabel }) => html`
    <cts-heatmap
      heading=${heading}
      caption=${caption}
      value-label=${valueLabel}
      .rows=${DAYS}
      .cols=${HOURS}
      .values=${VALUES}
      .cellTitle=${cellTitle}
    ></cts-heatmap>
  `,

  async play({ canvasElement, step }) {
    const host = /** @type {any} */ (canvasElement.querySelector("cts-heatmap"));
    await host.updateComplete;

    await step("the figure is named by its own heading", async () => {
      const figure = host.querySelector("figure.cts-heatmap");
      const h3 = host.querySelector("h3.cts-heatmap-heading");
      expect(h3.id).toBeTruthy();
      expect(figure.getAttribute("aria-labelledby")).toBe(h3.id);
      expect(h3.textContent).toBe("Test runs by day and hour");
      const caption = /** @type {HTMLElement} */ (host.querySelector(".cts-heatmap-caption"))
        .textContent;
      expect(caption).toContain("UTC");
      // The grid shows the shape of the week; the caption says how much it is
      // made of, which no cell can.
      expect(caption).toMatch(/^[\d,]+ runs in this range\./);
    });

    await step("7 x 24 cells, each with its own headers", async () => {
      expect(host.querySelectorAll(".cts-heatmap-cell").length).toBe(7 * 24);
      expect(
        [...host.querySelectorAll(".cts-heatmap-rowhead")].map((el) => el.textContent),
      ).toEqual(DAYS);
      expect(host.querySelectorAll(".cts-heatmap-colhead").length).toBe(24);
    });

    await step("the grid is one labelled image, not 168 announced boxes", async () => {
      const grid = host.querySelector(".cts-heatmap-grid");
      expect(grid.getAttribute("role")).toBe("img");
      const label = grid.getAttribute("aria-label");
      expect(label).toContain("7 rows by 24 columns");
      // The question the chart answers is "when is it busiest", so the answer
      // is in the label rather than only in the fills.
      expect(label).toMatch(/busiest \w{3} \d{2} with [\d,]+ runs/);
      expect(label).toContain("Data table available below");
    });

    await step("more is darker, and an empty cell is not a pale one", async () => {
      const cells = /** @type {Array<HTMLElement>} */ ([
        ...host.querySelectorAll(".cts-heatmap-cell"),
      ]);
      // Monday 03:00 is overnight; Monday 14:00 is the busiest hour there is.
      const quiet = cells[0 * 24 + 3];
      const busy = cells[0 * 24 + 14];
      const mix = (/** @type {HTMLElement} */ cell) =>
        Number((cell.getAttribute("style") || "").match(/(\d+)%/)?.[1] ?? 0);
      expect(mix(busy)).toBeGreaterThan(mix(quiet));
      // Every non-zero cell is on the ramp; the ramp's floor keeps the
      // quietest hour visible instead of collapsing it into the surface.
      expect(mix(quiet)).toBeGreaterThanOrEqual(10);
      expect(mix(busy)).toBe(100);
    });

    await step("every cell says what it is on hover", async () => {
      const cells = [...host.querySelectorAll(".cts-heatmap-cell")];
      expect(cells[0].getAttribute("title")).toBe("Mon 00:00 UTC — 18 runs");
      expect(cells[24 + 14].getAttribute("title")).toBe("Tue 14:00 UTC — 132 runs");
    });

    await step("every swatch is labelled with the value it stands for", async () => {
      const scale = /** @type {HTMLElement} */ (host.querySelector(".cts-heatmap-scale"));
      // The ramp is square-root scaled, so labelling only the two ends would
      // read as linear and put the middle swatch at a quarter of what it
      // means. The busiest cell here is 132.
      expect(scale.textContent).toContain("square-root scale");
      const steps = [...host.querySelectorAll(".cts-heatmap-scale-step")];
      expect(steps.length).toBe(5);
      expect(steps.map((step) => step.textContent.trim())).toEqual(["0", "8", "33", "74", "132"]);
      // ...and the empty-cell swatch is the muted surface, not the palest step
      // of the ramp: it is not on the ramp at all.
      const swatch = (/** @type {number} */ index) =>
        /** @type {HTMLElement} */ (steps[index].querySelector(".cts-heatmap-swatch")).getAttribute(
          "style",
        );
      expect(swatch(0)).toContain("var(--bg-muted)");
      expect(swatch(4)).toContain("100%");
    });

    await step("the data table is the accessible twin, not a fallback", async () => {
      const details = /** @type {HTMLDetailsElement} */ (
        host.querySelector("details.cts-heatmap-data")
      );
      expect(details.open).toBe(false);
      const table = /** @type {HTMLTableElement} */ (details.querySelector("table"));
      expect(/** @type {HTMLElement} */ (table.querySelector("caption")).textContent).toBe(
        "Test runs by day and hour",
      );
      const headers = [...table.querySelectorAll("thead th")].map((th) => th.textContent);
      expect(headers[0]).toBe("Day");
      expect(headers.length).toBe(25);
      const rows = [...table.querySelectorAll("tbody tr")];
      expect(rows.length).toBe(7);
      expect(rows[1].querySelectorAll("td")[14].textContent).toBe("132");
    });
  },
};

/**
 * A range with no runs in it. An all-zero grid would be a wall of identical
 * empty cells pretending to be a chart, so it says so in words instead.
 */
export const Empty = {
  args: {
    heading: "Test runs by day and hour",
    caption: "All hours are UTC. Sliced by the selected range only.",
    valueLabel: "runs",
  },
  render: ({ heading, caption, valueLabel }) => html`
    <cts-heatmap
      heading=${heading}
      caption=${caption}
      value-label=${valueLabel}
      .rows=${DAYS}
      .cols=${HOURS}
      .values=${EMPTY_VALUES}
    ></cts-heatmap>
  `,

  async play({ canvasElement, step }) {
    const host = /** @type {any} */ (canvasElement.querySelector("cts-heatmap"));
    await host.updateComplete;

    await step("one sentence instead of 168 empty cells", async () => {
      const empty = host.querySelector('[data-testid="cts-heatmap-empty"]');
      expect(empty).toBeTruthy();
      expect(empty.textContent.trim()).toBe("No runs were recorded in this range.");
      expect(host.querySelectorAll(".cts-heatmap-cell").length).toBe(0);
      // ...and no scale legend either: there is no scale.
      expect(host.querySelector(".cts-heatmap-scale")).toBeNull();
      expect(host.querySelector("details")).toBeNull();
    });

    await step("the heading and the caption stay, so the section is not blank", async () => {
      expect(host.querySelector("h3").textContent).toBe("Test runs by day and hour");
      expect(host.querySelector(".cts-heatmap-caption")).toBeTruthy();
    });
  },
};
