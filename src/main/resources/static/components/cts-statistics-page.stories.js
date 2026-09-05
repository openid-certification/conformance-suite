import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { delay, http, HttpResponse } from "msw";
import {
  MOCK_STATS_EMPTY,
  MOCK_STATS_ERROR,
  MOCK_STATS_INVALID,
  MOCK_STATS_LAST_ERROR,
  MOCK_STATS_MODULES,
  MOCK_STATS_MONTHS,
  MOCK_STATS_PENDING,
  MOCK_STATS_READY,
  MOCK_STATS_REFRESHING,
  MOCK_STATS_WEEKS,
  statisticsOverviewFor,
} from "@fixtures/mock-statistics.js";
import "./cts-statistics-page.js";

export default {
  title: "Pages/cts-statistics-page",
  component: "cts-statistics-page",
  // The page reads its range and filters from location.search and writes them
  // back with replaceState, so without a reset one story's filters would
  // hydrate the next story's page.
  beforeEach() {
    history.replaceState(null, "", "/iframe.html");
    REQUESTS.length = 0;
  },
};

const ENDPOINT = "/api/statistics/overview";

/**
 * Every query string the page asked for, in order — the only place a play
 * function can prove that a control really went to the server rather than
 * re-slicing something locally. Reset by the meta `beforeEach`.
 * @type {Array<string>}
 */
const REQUESTS = [];

/** The page polls at 2 s; give every wait comfortable headroom over that. */
const POLL_TIMEOUT = { timeout: 10000 };

/** How many charts the page draws. */
const CHART_COUNT = 5;

/**
 * The page fetches one extra, deliberately unfiltered snapshot on load: it is
 * where the family colours and the family select's options come from, so that
 * no filter can repaint a family or hide the option that would widen things
 * back out. It is the only request with no query string at all.
 * @param {URL} url - The request URL.
 * @returns {boolean} True for that baseline request.
 */
function isBaseline(url) {
  return url.search === "";
}

/**
 * The default handler: answer every request from the fixture the way the
 * server would, applying the query. Records what was asked for.
 * @returns {any} An msw handler for the statistics endpoint.
 */
function slicingHandler() {
  return http.get(ENDPOINT, ({ request }) => {
    const url = new URL(request.url);
    REQUESTS.push(url.search);
    return HttpResponse.json(statisticsOverviewFor(url));
  });
}

/** @returns {Array<string>} The query strings of the filtered (non-baseline) requests. */
function filteredRequests() {
  return REQUESTS.filter((search) => search !== "");
}

/**
 * The query strings the handler has ANSWERED, in the order it answered them —
 * REQUESTS records when a request was made, which says nothing about which
 * reply landed first. Reset by the story that uses it.
 * @type {Array<string>}
 */
const ANSWERED = [];

/** How far behind the view's reply the slow baseline lands. */
const BASELINE_DELAY_MS = 600;

/**
 * Answer as {@link slicingHandler} does, but hold the baseline back so the
 * narrowed view's reply arrives first — the ordering the page has no control
 * over and must not paint twice because of.
 * @returns {any} An msw handler for the statistics endpoint.
 */
function slowBaselineHandler() {
  return http.get(ENDPOINT, async ({ request }) => {
    const url = new URL(request.url);
    REQUESTS.push(url.search);
    if (isBaseline(url)) await delay(BASELINE_DELAY_MS);
    ANSWERED.push(url.search);
    return HttpResponse.json(statisticsOverviewFor(url));
  });
}

/**
 * Each plotted family and the fill it ended up with. The resolved colour, not
 * the `--chart-cat-N` token: a repaint is only observable here.
 * @param {HTMLElement} canvasElement - The story root.
 * @returns {Record<string, string>} Family → colour.
 */
function coloursByFamily(canvasElement) {
  return Object.fromEntries(
    runsChartInstance(canvasElement).data.datasets.map((/** @type {any} */ ds) => [
      ds.label,
      ds.backgroundColor,
    ]),
  );
}

/**
 * @param {HTMLElement} canvasElement - The story root.
 * @returns {Array<string>} The family select's options, in order.
 */
function familyOptions(canvasElement) {
  return Array.from(select(canvasElement, "stats-family").options).map((option) => option.value);
}

/**
 * Wait until the page has painted all of its charts.
 * @param {HTMLElement} canvasElement - The story root.
 * @returns {Promise<void>}
 */
async function waitForCharts(canvasElement) {
  await waitFor(() => {
    expect(canvasElement.querySelector('[data-testid="stats-charts"]')).toBeTruthy();
  }, POLL_TIMEOUT);
  await waitFor(() => {
    const charts = /** @type {Array<any>} */ (
      Array.from(canvasElement.querySelectorAll(".cts-stats-chart cts-chart"))
    );
    expect(charts.length).toBe(CHART_COUNT);
    expect(canvasElement.querySelectorAll(".cts-stats-chart canvas").length).toBe(CHART_COUNT);
    // Chart.js is lazy-loaded on first connect, so the <canvas> is in the DOM
    // a beat before anything is painted into it. `chartInstance` is the
    // reliable "the plot exists" signal.
    expect(charts.filter((el) => el.chartInstance).length).toBe(CHART_COUNT);
  }, POLL_TIMEOUT);
}

/**
 * Read one chart's `<details>` data table — the accessible twin of the plot,
 * and the only place a play function can assert exact values.
 * @param {HTMLElement} canvasElement - The story root.
 * @param {string} testid - Chart wrapper test id, e.g. `stats-chart-runs`.
 * @returns {{rows: Array<HTMLTableRowElement>, headers: Array<string>}} Table contents.
 */
function chartTable(canvasElement, testid) {
  const table = /** @type {HTMLTableElement} */ (
    canvasElement.querySelector(`[data-testid="${testid}"] table`)
  );
  expect(table).toBeTruthy();
  return {
    rows: /** @type {Array<HTMLTableRowElement>} */ (
      Array.from(table.querySelectorAll("tbody tr"))
    ),
    headers: Array.from(table.querySelectorAll("thead th")).map((th) => th.textContent.trim()),
  };
}

/**
 * The runs chart's live Chart.js instance, via `cts-chart`'s public
 * `chartInstance` getter. The RESOLVED fill on each dataset is the only place
 * "this family was not repainted" is observable — the DOM only carries the
 * `--chart-cat-N` token name.
 * @param {HTMLElement} canvasElement - The story root.
 * @returns {any} The Chart.js instance.
 */
function runsChartInstance(canvasElement) {
  const host = /** @type {any} */ (
    canvasElement.querySelector('[data-testid="stats-chart-runs"] cts-chart')
  );
  return host.chartInstance;
}

/**
 * One of the two module charts, once it has painted.
 * @param {HTMLElement} canvasElement - The story root.
 * @param {string} testid - `stats-modules-runs` or `stats-modules-failing`.
 * @returns {Promise<any>} The `<cts-chart>` element.
 */
async function moduleChart(canvasElement, testid) {
  /** @type {any} */
  let host = null;
  await waitFor(() => {
    host = canvasElement.querySelector(`[data-testid="${testid}"] cts-chart`);
    expect(host).toBeTruthy();
    expect(host.chartInstance).toBeTruthy();
  }, POLL_TIMEOUT);
  return host;
}

/**
 * The modules section's own table — every module, not the twelve either chart
 * plots. Opened first: it is a disclosure, and a closed one tells a play
 * function nothing about what it contains.
 *
 * NEVER call this from inside a `waitFor` callback. Opening the disclosure is
 * a DOM mutation, `waitFor` re-runs its callback on every mutation it
 * observes, and a callback that mutates therefore re-arms itself in a
 * microtask loop the timeout timer never gets a turn to break.
 * @param {HTMLElement} canvasElement - The story root.
 * @returns {{rows: Array<Array<string>>, headers: Array<string>, summary: string,
 *   hint: string}} Its contents.
 */
function moduleTable(canvasElement) {
  const details = /** @type {HTMLDetailsElement} */ (
    canvasElement.querySelector('[data-testid="stats-modules-table"]')
  );
  expect(details).toBeTruthy();
  details.open = true;
  return {
    summary: /** @type {HTMLElement} */ (details.querySelector("summary")).textContent.trim(),
    hint: /** @type {HTMLElement} */ (details.querySelector("p")).textContent
      .replace(/\s+/g, " ")
      .trim(),
    headers: [...details.querySelectorAll("thead th")].map((th) => th.textContent.trim()),
    rows: [...details.querySelectorAll("tbody tr")].map((row) =>
      [...row.querySelectorAll("th, td")].map((cell) => cell.textContent.trim()),
    ),
  };
}

/**
 * One of the page's filter selects.
 * @param {HTMLElement} canvasElement - The story root.
 * @param {string} testid - e.g. `stats-family`.
 * @returns {HTMLSelectElement} The select.
 */
function select(canvasElement, testid) {
  return /** @type {HTMLSelectElement} */ (
    canvasElement.querySelector(`[data-testid="${testid}"]`)
  );
}

/**
 * Click a range preset by its label and wait for the click to register.
 * @param {HTMLElement} canvasElement - The story root.
 * @param {string} label - e.g. "26 weeks".
 * @returns {Promise<void>}
 */
async function pickRange(canvasElement, label) {
  const button = /** @type {HTMLButtonElement} */ (
    Array.from(canvasElement.querySelectorAll('[data-testid="stats-range"] button')).find(
      (btn) => btn.textContent.trim() === label,
    )
  );
  expect(button).toBeTruthy();
  await userEvent.click(button);
  await waitFor(() => {
    expect(button.getAttribute("aria-pressed")).toBe("true");
  });
}

// --- Stories ---

/**
 * The ordinary case: a fresh monthly snapshot, ten tiles, five charts.
 */
export const Ready = {
  parameters: { msw: { handlers: [slicingHandler()] } },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitForCharts(canvasElement);

    await step("ten unfiltered tiles carry the snapshot's counters", async () => {
      const tiles = canvasElement.querySelectorAll('[data-testid="stats-tiles"] .cts-stats-tile');
      expect(tiles.length).toBe(10);
      const value = (/** @type {string} */ key) =>
        canvasElement
          .querySelector(`[data-testid="stat-tile-${key}"] .cts-stats-tile-value`)
          .textContent.trim();
      // Exact grouped figures, never compacted — this is an admin console.
      expect(value("totalTests")).toBe("91,800");
      expect(value("totalPlans")).toBe("1,932");
      expect(value("totalUsers")).toBe("210");
      expect(value("inProgress")).toBe("3");
      expect(value("stuck")).toBe("7");
      expect(value("certifiedPlans")).toBe("88");
      expect(value("publishedPlans")).toBe("45");
      expect(canvas.getByText("Stuck / abandoned (>24 h)")).toBeInTheDocument();
      // Three counters are not what a reader would assume from the label
      // alone — the run total is the collection's own estimate, users are
      // counted over plans, and the two liveness counters only scan the last
      // year — so the hints say so.
      const hint = (/** @type {string} */ key) =>
        canvasElement
          .querySelector(`[data-testid="stat-tile-${key}"] .cts-stats-tile-hint`)
          .textContent.trim();
      expect(hint("totalTests")).toBe("All time, estimated");
      expect(hint("totalUsers")).toBe("Plan owners, all time");
      expect(hint("inProgress")).toBe("Running or waiting, last year");
      expect(hint("stuck")).toBe("Non-terminal >24 h, within the last year");
    });

    await step("the default range is sent to the server, not applied locally", async () => {
      // 12 months back from the frozen clock (2026-06-01), open-ended at the
      // top because the server's axis already ends at today.
      expect(filteredRequests()[0]).toBe("?granularity=month&from=2025-07");
      // ...and the page URL says the same thing, so the view is shareable.
      expect(location.search).toBe("?range=12m");
    });

    await step("toolbar reports the snapshot age and is not busy", async () => {
      const asOf = canvasElement.querySelector('[data-testid="stats-computed-at"]');
      expect(asOf.textContent).toContain("Data as of");
      expect(asOf.querySelector("time")?.getAttribute("datetime")).toBe("2026-06-01T09:12:33.000Z");
      const refresh = canvasElement.querySelector('[data-testid="stats-refresh"] button');
      expect(refresh.hasAttribute("disabled")).toBe(false);
      expect(canvasElement.querySelector("cts-spinner")).toBeNull();
      expect(canvasElement.querySelector('[data-testid="stats-charts"]').classList).not.toContain(
        "is-busy",
      );
    });

    await step('the two range groups default to "12 months"', async () => {
      const weekly = canvasElement.querySelector('[data-testid="stats-range-weekly"]');
      const monthly = canvasElement.querySelector('[data-testid="stats-range-monthly"]');
      expect(weekly.getAttribute("role")).toBe("group");
      expect(weekly.getAttribute("aria-label")).toBe("Weekly range");
      expect(monthly.getAttribute("aria-label")).toBe("Monthly range");
      expect(
        Array.from(weekly.querySelectorAll("button")).map((b) => b.textContent.trim()),
      ).toEqual(["12 weeks", "26 weeks", "52 weeks"]);
      const pressed = Array.from(
        canvasElement.querySelectorAll('[data-testid="stats-range"] button'),
      )
        .filter((btn) => btn.getAttribute("aria-pressed") === "true")
        .map((btn) => btn.textContent.trim());
      expect(pressed).toEqual(["12 months"]);
    });

    await step("the family select lists only families with runs", async () => {
      const family = select(canvasElement, "stats-family");
      expect(family.getAttribute("aria-label")).toBe("Spec family");
      const options = Array.from(family.options).map((option) => option.value);
      expect(options[0]).toBe("");
      // Shared Signals Framework has never run, so it is not offered.
      expect(options).not.toContain("Shared Signals Framework");
      expect(options).toContain("FAPI2 Security Profile");
      expect(options.length).toBe(11);
    });

    await step("nothing is filtered, so there is nothing to clear", async () => {
      expect(canvasElement.querySelector('[data-testid="stats-clear-filters"]')).toBeNull();
    });

    await step("the variant selects wait until the view is narrowed", async () => {
      // Unfiltered, the suite mentions getting on for forty plan-level variant
      // parameters; a row of forty selects is not a filter row.
      expect(canvasElement.querySelectorAll('[data-testid^="stats-variant-"]').length).toBe(0);
      // Four certification profiles plus "All profiles".
      expect(select(canvasElement, "stats-cert").options.length).toBe(5);
    });

    await step("five charts, each on the server's 12-month axis", async () => {
      const firstRowMonth = MOCK_STATS_MONTHS[MOCK_STATS_MONTHS.length - 12];
      for (const id of [
        "stats-chart-runs",
        "stats-chart-plans",
        "stats-chart-results",
        "stats-chart-users",
        "stats-chart-certified",
      ]) {
        const { rows, headers } = chartTable(canvasElement, id);
        expect(rows.length).toBe(12);
        expect(headers[0]).toBe("Month");
        const rowHeader = /** @type {HTMLElement} */ (rows[0].querySelector("th"));
        expect(rowHeader.textContent.trim()).toBe(firstRowMonth);
      }
    });

    await step("the runs chart folds the tail families into one Other series", async () => {
      const { headers } = chartTable(canvasElement, "stats-chart-runs");
      // Month + six categorical families (OpenID Connect Logout is idle in
      // the last 12 months, so it does not render at the default range) + Other.
      expect(headers.length).toBe(8);
      expect(headers[headers.length - 1]).toBe("Other");
      expect(headers).toContain("FAPI2 Security Profile");
      expect(headers).not.toContain("OpenID Connect Logout");
      // Folded families are named in the tooltip footer, not as their own
      // columns.
      expect(headers).not.toContain("No plan");
      expect(headers).not.toContain("OpenID Federation");
    });

    await step("the results chart is keyed by outcome, not by family", async () => {
      const { headers } = chartTable(canvasElement, "stats-chart-results");
      expect(headers).toEqual([
        "Month",
        "PASSED",
        "WARNING",
        "REVIEW",
        "FAILED",
        "SKIPPED",
        "NEVER_FINISHED",
      ]);
    });

    await step("certification activity is charted by family, like the runs", async () => {
      const heading = /** @type {HTMLElement} */ (
        canvasElement.querySelector('[data-testid="stats-chart-certified"] h3')
      );
      expect(heading.textContent.trim()).toBe("Certified plans per month");
      const { headers } = chartTable(canvasElement, "stats-chart-certified");
      expect(headers[0]).toBe("Month");
      expect(headers).toContain("FAPI2 Security Profile");
      // Families that never certified anything are not given a dead legend key.
      expect(headers).not.toContain("OID4VP");
    });

    await step("the plans that fell into Other / retired are listed under the charts", async () => {
      const unresolved = MOCK_STATS_READY.data.unresolvedPlans;
      const details = /** @type {HTMLDetailsElement} */ (
        canvasElement.querySelector('[data-testid="stats-unresolved"]')
      );
      expect(details).toBeTruthy();
      // Collapsed by default: it explains one bar segment and must not
      // compete with the charts it sits under.
      expect(details.open).toBe(false);
      const summary = /** @type {HTMLElement} */ (details.querySelector("summary"));
      expect(summary.textContent.trim()).toBe(
        `Plans not mapped to a spec family (${unresolved.length})`,
      );
      expect(details.querySelectorAll("tbody tr").length).toBe(unresolved.length);
      expect(
        Array.from(details.querySelectorAll("tbody tr th")).map((cell) => cell.textContent.trim()),
      ).toEqual(unresolved.map((plan) => plan.planName));
    });

    await step('the "Other" tooltip footer names what it is listing', async () => {
      const host = /** @type {any} */ (
        canvasElement.querySelector('[data-testid="stats-chart-runs"] cts-chart')
      );
      const footers = Array.from({ length: 12 }, (_, index) => host.tooltipFooter(index)).filter(
        (lines) => lines.length > 0,
      );
      expect(footers.length).toBeGreaterThan(0);
      for (const lines of footers) {
        // Without the lead line the family counts read as extra detail about
        // whichever series the pointer happens to be on.
        expect(lines[0]).toBe("Other includes:");
        expect(lines.length).toBeGreaterThan(1);
        expect(lines[1]).toMatch(/^.+: \d+$/);
      }
    });

    await step("the users chart says whose users it is counting", async () => {
      const { headers } = chartTable(canvasElement, "stats-chart-users");
      expect(headers).toEqual(["Month", "Active", "New"]);
      // The heading, not getByText: cts-chart repeats it in the table caption.
      const heading = /** @type {HTMLElement} */ (
        canvasElement.querySelector('[data-testid="stats-chart-users"] h3')
      );
      expect(heading.textContent.trim()).toBe("Users per month — by plan owner");
    });

    await step("the modules section is on the ordinary page too", async () => {
      // Its own story exercises the rankings; here it only has to be present,
      // so a regression that drops the section shows up in the default view.
      expect(canvasElement.querySelector('[data-testid="stats-modules"]')).toBeTruthy();
      expect(canvasElement.querySelectorAll('[data-testid="stats-modules"] cts-chart').length).toBe(
        2,
      );
      expect(canvasElement.querySelector('[data-testid="stats-modules-table"]')).toBeTruthy();
    });
  },
};

/**
 * The weekly presets: a different granularity, not just a shorter range, so
 * the axis, the labels and the headings all change with them.
 */
export const Weekly = {
  parameters: { msw: { handlers: [slicingHandler()] } },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await waitForCharts(canvasElement);

    await step("26 weeks asks the server for weekly cells", async () => {
      await pickRange(canvasElement, "26 weeks");
      await waitFor(() => {
        expect(filteredRequests().at(-1)).toBe("?granularity=week&from=2025-12-08");
      }, POLL_TIMEOUT);
      expect(location.search).toBe("?range=26w");
    });

    await step("the axis is Mondays, and it says so", async () => {
      await waitFor(() => {
        expect(chartTable(canvasElement, "stats-chart-runs").rows.length).toBe(
          MOCK_STATS_WEEKS.length,
        );
      }, POLL_TIMEOUT);
      const { headers, rows } = chartTable(canvasElement, "stats-chart-runs");
      expect(headers[0]).toBe("Week starting");
      // Compact labels: the day and month, with the year carried only where
      // it changes — the first label, and the first week of the new year.
      const labels = rows.map((row) =>
        /** @type {HTMLElement} */ (row.querySelector("th")).textContent.trim(),
      );
      expect(labels[0]).toBe("8 Dec 2025");
      expect(labels[1]).toBe("15 Dec");
      expect(labels).toContain("29 Dec");
      expect(labels).toContain("5 Jan 2026");
      expect(labels.at(-1)).toBe("1 Jun");
    });

    await step("every chart follows, headings included", async () => {
      const heading = (/** @type {string} */ testid) =>
        /** @type {HTMLElement} */ (
          canvasElement.querySelector(`[data-testid="${testid}"] h3`)
        ).textContent.trim();
      expect(heading("stats-chart-runs")).toBe("Test module runs per week");
      expect(heading("stats-chart-plans")).toBe("Test plans per week");
      expect(heading("stats-chart-users")).toBe("Users per week — by plan owner");
      expect(heading("stats-chart-certified")).toBe("Certified plans per week");
      expect(chartTable(canvasElement, "stats-chart-users").rows.length).toBe(
        MOCK_STATS_WEEKS.length,
      );
    });

    await step("12 weeks narrows the axis without changing granularity", async () => {
      await pickRange(canvasElement, "12 weeks");
      await waitFor(() => {
        expect(filteredRequests().at(-1)).toBe("?granularity=week&from=2026-03-16");
      }, POLL_TIMEOUT);
      await waitFor(() => {
        expect(chartTable(canvasElement, "stats-chart-runs").rows.length).toBe(12);
      }, POLL_TIMEOUT);
    });

    await step("going back to a monthly preset restores the monthly axis", async () => {
      await pickRange(canvasElement, "All time");
      await waitFor(() => {
        expect(filteredRequests().at(-1)).toBe("?granularity=month");
      }, POLL_TIMEOUT);
      await waitFor(() => {
        expect(chartTable(canvasElement, "stats-chart-runs").headers[0]).toBe("Month");
      }, POLL_TIMEOUT);
      expect(chartTable(canvasElement, "stats-chart-runs").rows.length).toBe(
        MOCK_STATS_MONTHS.length,
      );
    });
  },
};

/**
 * The filter cascade: spec family → test plan → plan-level variant →
 * certification profile. Every step is a request, every step is in the URL,
 * and no step repaints a family.
 */
export const FiltersCascade = {
  parameters: { msw: { handlers: [slicingHandler()] } },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await waitForCharts(canvasElement);

    /** @type {Record<string, string>} Column → fill, before any filtering. */
    const paintBefore = {};
    for (const dataset of runsChartInstance(canvasElement).data.datasets) {
      paintBefore[dataset.label] = dataset.backgroundColor;
    }

    await step("picking a family filters on the server", async () => {
      await userEvent.selectOptions(
        select(canvasElement, "stats-family"),
        "FAPI2 Security Profile",
      );
      await waitFor(() => {
        expect(filteredRequests().at(-1)).toBe(
          "?granularity=month&from=2025-07&family=FAPI2+Security+Profile",
        );
      }, POLL_TIMEOUT);
      await waitFor(() => {
        expect(chartTable(canvasElement, "stats-chart-runs").headers).toEqual([
          "Month",
          "FAPI2 Security Profile",
        ]);
      }, POLL_TIMEOUT);
      // ...and the users chart is filtered too now, unlike in phase 1.
      expect(chartTable(canvasElement, "stats-chart-users").headers).toEqual([
        "Month",
        "Active",
        "New",
      ]);
    });

    await step("...without repainting the family that was already on screen", async () => {
      const datasets = runsChartInstance(canvasElement).data.datasets;
      expect(datasets.length).toBe(1);
      expect(datasets[0].backgroundColor).toBe(paintBefore["FAPI2 Security Profile"]);
    });

    await step("the plan select now offers that family's plans", async () => {
      // The filter row and the charts are separate Lit updates, so the row can
      // still be a render behind the chart the previous step waited for.
      await waitFor(() => {
        expect(
          Array.from(select(canvasElement, "stats-plan").options).map((option) => option.value),
        ).toEqual([
          "",
          "fapi2-message-signing-final-test-plan",
          "fapi2-security-profile-final-test-plan",
        ]);
      }, POLL_TIMEOUT);
      expect(select(canvasElement, "stats-plan").getAttribute("aria-label")).toBe("Test plan");
    });

    await step("picking a plan keeps its siblings selectable", async () => {
      await userEvent.selectOptions(
        select(canvasElement, "stats-plan"),
        "fapi2-security-profile-final-test-plan",
      );
      await waitFor(() => {
        expect(filteredRequests().at(-1)).toContain("plan=fapi2-security-profile-final-test-plan");
      }, POLL_TIMEOUT);
      // The server counts dimensions under the whole query, so the payload
      // now offers this one plan only. Rendering that straight would make the
      // choice a dead end, so the page keeps the list it had.
      await waitFor(() => {
        expect(select(canvasElement, "stats-plan").options.length).toBe(3);
      });
      expect(select(canvasElement, "stats-plan").value).toBe(
        "fapi2-security-profile-final-test-plan",
      );
    });

    await step("a variant parameter narrows it further", async () => {
      // They appear now, because a family and a plan have narrowed the view —
      // one per parameter with more than one value to choose between, so the
      // fixture's single-valued `client_registration` is not among them.
      await waitFor(() => {
        expect(canvasElement.querySelectorAll('[data-testid^="stats-variant-"]').length).toBe(3);
      }, POLL_TIMEOUT);
      const variant = select(canvasElement, "stats-variant-client_auth_type");
      expect(variant.getAttribute("aria-label")).toBe("Variant: client_auth_type");
      // The "any" option names the parameter, so the collapsed control says
      // what it filters before anything is picked.
      expect(variant.options[0].textContent.trim()).toBe("Any client_auth_type");
      expect(variant.options[1].textContent.trim()).toBe("private_key_jwt (88)");
      await userEvent.selectOptions(variant, "mtls");
      await waitFor(() => {
        expect(filteredRequests().at(-1)).toContain("variant.client_auth_type=mtls");
      }, POLL_TIMEOUT);
    });

    await step("and so does a certification profile", async () => {
      await userEvent.selectOptions(
        select(canvasElement, "stats-cert"),
        "FAPI2 Security Profile Final",
      );
      await waitFor(() => {
        expect(filteredRequests().at(-1)).toContain("cert=FAPI2+Security+Profile+Final");
      }, POLL_TIMEOUT);
    });

    await step("every filter is in the URL, so the view can be shared", async () => {
      const params = new URLSearchParams(location.search);
      expect(params.get("range")).toBe("12m");
      expect(params.get("family")).toBe("FAPI2 Security Profile");
      expect(params.get("plan")).toBe("fapi2-security-profile-final-test-plan");
      expect(params.get("variant.client_auth_type")).toBe("mtls");
      expect(params.get("cert")).toBe("FAPI2 Security Profile Final");
    });

    await step("changing the family drops the filters that belonged to the old one", async () => {
      // A certification profile and a variant parameter belong to a family;
      // carrying them into another one leaves five charts of zeros and
      // nothing on screen to explain why.
      await userEvent.selectOptions(select(canvasElement, "stats-family"), "OID4VP");
      await waitFor(() => {
        expect(filteredRequests().at(-1)).toBe("?granularity=month&from=2025-07&family=OID4VP");
      }, POLL_TIMEOUT);
      expect(location.search).toBe("?range=12m&family=OID4VP");
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="stats-cert"]').value).toBe("");
      });
    });

    await step("the tiles never move — they are whole-database counters", async () => {
      expect(
        canvasElement
          .querySelector('[data-testid="stat-tile-totalTests"] .cts-stats-tile-value')
          .textContent.trim(),
      ).toBe("91,800");
    });

    await step("Clear filters drops all of them and keeps the range", async () => {
      await waitFor(() => {
        expect(
          canvasElement.querySelector('[data-testid="stats-clear-filters"] button'),
        ).toBeTruthy();
      }, POLL_TIMEOUT);
      const clear = canvasElement.querySelector('[data-testid="stats-clear-filters"] button');
      await userEvent.click(clear);
      await waitFor(() => {
        expect(filteredRequests().at(-1)).toBe("?granularity=month&from=2025-07");
      }, POLL_TIMEOUT);
      expect(location.search).toBe("?range=12m");
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="stats-clear-filters"]')).toBeNull();
      });
    });

    await step("...and nobody was repainted along the way", async () => {
      await waitFor(() => {
        expect(runsChartInstance(canvasElement).data.datasets.length).toBeGreaterThan(1);
      }, POLL_TIMEOUT);
      for (const dataset of runsChartInstance(canvasElement).data.datasets) {
        expect(dataset.backgroundColor).toBe(paintBefore[dataset.label]);
      }
    });
  },
};

/**
 * Everything below the trend charts: the storage row, the three
 * distributions, the activity heatmap and the external servers.
 *
 * The three groups are scoped differently and the page has to say so — the
 * distributions follow the filters, the heatmap follows the range only, the
 * hosts nothing at all — so this story checks the wording as well as the
 * numbers.
 */
export const PhaseTwoSections = {
  parameters: { msw: { handlers: [slicingHandler()] } },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitForCharts(canvasElement);

    await step("a storage tile per collection, in human bytes", async () => {
      const tiles = canvasElement.querySelectorAll('[data-testid="stats-storage"] .cts-stats-tile');
      expect(tiles.length).toBe(3);
      const tile = /** @type {HTMLElement} */ (
        canvasElement.querySelector('[data-testid="stat-storage-TEST_INFO"]')
      );
      const part = (/** @type {string} */ selector) =>
        /** @type {HTMLElement} */ (tile.querySelector(selector)).textContent;
      expect(part(".cts-stats-tile-value").trim()).toBe("91,800");
      expect(part(".cts-stats-tile-label").trim()).toBe("TEST_INFO documents");
      const hint = part(".cts-stats-tile-hint").replace(/\s+/g, " ");
      expect(hint).toContain("1.7 GB data");
      expect(hint).toContain("581.7 MB on disk");
      expect(hint).toContain("91.6 MB indexes");
    });

    await step("variant usage waits for a family or a plan", async () => {
      // Unfiltered, the suite mentions getting on for forty variant
      // parameters; forty small multiples is not a section.
      const variants = canvasElement.querySelector('[data-testid="stats-dist-variants"]');
      expect(variants.textContent).toContain("Select a family or plan to see variant usage");
      expect(variants.querySelectorAll("cts-chart").length).toBe(0);
    });

    await step("certification profiles are charted by distinct users", async () => {
      const { headers, rows } = chartTable(canvasElement, "stats-dist-certs");
      expect(headers).toEqual(["Certification profile", "Users", "Plans"]);
      expect(rows.length).toBe(4);
      const first = [...rows[0].querySelectorAll("th, td")].map((cell) => cell.textContent.trim());
      expect(first).toEqual(["FAPI2 Security Profile Final", "31", "120"]);
      // One series, so no legend: the heading already names what is plotted.
      const chart = /** @type {any} */ (
        canvasElement.querySelector('[data-testid="stats-dist-certs"] cts-chart')
      );
      expect(chart.chartInstance.options.indexAxis).toBe("y");
      expect(chart.chartInstance.options.interaction.axis).toBe("y");
      expect(chart.chartInstance.options.plugins.legend.display).toBe(false);
      expect(chart.chartInstance.data.datasets.length).toBe(1);
      // The plan count is not plotted — one bar, one measure — so hovering has
      // to be able to reach it, not just the data table.
      expect(chart.tooltipFooter(0)).toEqual(["120 plans"]);
      expect(chart.tooltipFooter(3)).toEqual(["21 plans"]);
    });

    await step("entity under test is charted by runs, with no second column", async () => {
      const { headers, rows } = chartTable(canvasElement, "stats-dist-entities");
      expect(headers).toEqual(["Entity", "Runs"]);
      expect(rows.length).toBe(3);
      // Nothing but runs to report, so no footer is wired at all.
      const chart = /** @type {any} */ (
        canvasElement.querySelector('[data-testid="stats-dist-entities"] cts-chart')
      );
      expect(chart.tooltipFooter).toBeUndefined();
      expect(/** @type {HTMLElement} */ (rows[0].querySelector("th")).textContent.trim()).toBe(
        "Test an OpenID Provider / Authorization Server",
      );
    });

    await step("picking a family brings the variant small multiples in", async () => {
      await userEvent.selectOptions(
        select(canvasElement, "stats-family"),
        "FAPI2 Security Profile",
      );
      await waitFor(() => {
        expect(canvasElement.querySelectorAll('[data-testid^="stats-dist-variant-"]').length).toBe(
          3,
        );
      }, POLL_TIMEOUT);
      // The single-valued parameter is not among them: one bar says only that
      // everything used the one value it could have used.
      expect(
        canvasElement.querySelector('[data-testid="stats-dist-variant-client_registration"]'),
      ).toBeNull();
      const headings = [
        ...canvasElement.querySelectorAll('[data-testid^="stats-dist-variant-"] h3'),
      ].map((h3) => h3.textContent.trim());
      expect(headings).toEqual(["client_auth_type", "fapi_profile", "server_metadata"]);
    });

    await step("...ranked on the measure they plot, not on the delivered order", async () => {
      // The fixture delivers server_metadata ranked by PLANS (discovery
      // first); the chart plots USERS, so it has to re-rank.
      const { headers, rows } = chartTable(canvasElement, "stats-dist-variant-server_metadata");
      expect(headers).toEqual(["Value", "Users", "Plans"]);
      expect(
        rows.map((row) => /** @type {HTMLElement} */ (row.querySelector("th")).textContent.trim()),
      ).toEqual(["static", "discovery"]);
      const chart = /** @type {any} */ (
        canvasElement.querySelector('[data-testid="stats-dist-variant-server_metadata"] cts-chart')
      );
      expect(chart.tooltipFooter(0)).toEqual(["145 plans"]);
      expect([...rows[0].querySelectorAll("td")].map((cell) => cell.textContent.trim())).toEqual([
        "71",
        "145",
      ]);
    });

    await step("modules follow the family that was just picked, and say so", async () => {
      // A family IS selected by the previous step, and modules — unlike the
      // heatmap below — do follow it.
      await waitFor(() => {
        const runs = /** @type {any} */ (
          canvasElement.querySelector('[data-testid="stats-modules-runs"] cts-chart')
        );
        expect(runs.labels.length).toBe(4);
      }, POLL_TIMEOUT);
      const { rows } = moduleTable(canvasElement);
      expect(rows.map((row) => row[0])).toEqual([
        "fapi2-security-profile-final-ensure-request-object-signature-algorithm-is-not-none",
        "fapi2-security-profile-final-user-rejects-authentication",
        "fapi2-message-signing-final-signed-request-object",
        "fapi2-security-profile-final-par-without-request-uri",
      ]);
    });

    await step("the heatmap is 7 x 24 and says what it is and is not filtered by", async () => {
      const heatmap = canvasElement.querySelector('[data-testid="stats-heatmap"]');
      expect(heatmap.querySelectorAll(".cts-heatmap-cell").length).toBe(7 * 24);
      const caption = heatmap
        .querySelector(".cts-heatmap-caption")
        .textContent.replace(/\s+/g, " ");
      // How much the grid is counting altogether, then what it does and does
      // not follow.
      expect(caption).toMatch(/^[\d,]+ runs in this range\./);
      expect(caption).toContain("All hours are UTC");
      // The server only keeps heat cells for a trailing 24 months, so the
      // range is a window INSIDE that and the caption has to say both.
      expect(caption).toContain("Within the last 24 months");
      expect(caption).toContain("selected range (12 months) only");
      expect(caption).toContain("not by family, plan, variant or certification profile");
      // A family IS selected by now, and the heatmap must not have followed it.
      const cells = heatmap.querySelectorAll(".cts-heatmap-cell");
      expect(cells[0].getAttribute("title")).toBe("Mon 00:00 UTC — 18 runs");
      expect(canvas.getByText("Activity (UTC)")).toBeInTheDocument();
    });

    await step("external servers are a disclosure, 24 months and unfiltered", async () => {
      const hosts = /** @type {HTMLDetailsElement} */ (
        canvasElement.querySelector('[data-testid="stats-hosts"]')
      );
      expect(hosts.open).toBe(false);
      expect(/** @type {HTMLElement} */ (hosts.querySelector("summary")).textContent.trim()).toBe(
        "External servers under test (6)",
      );
      expect(hosts.textContent.replace(/\s+/g, " ")).toContain(
        "The top 100 by runs over the last 24 months",
      );
      expect(hosts.textContent.replace(/\s+/g, " ")).toContain(
        "the suite's own endpoints are excluded",
      );
      const rows = [...hosts.querySelectorAll("tbody tr")];
      expect(rows.length).toBe(6);
      const first = [...rows[0].querySelectorAll("th, td")].map((cell) => cell.textContent.trim());
      expect(first.slice(0, 3)).toEqual(["as.example.com", "8,200", "41"]);
      expect(
        /** @type {HTMLElement} */ (rows[0].querySelector("time")).getAttribute("datetime"),
      ).toBe("2026-05-31T22:14:02.000Z");
    });
  },
};

/**
 * The modules section: which test modules the suite runs most, and which ones
 * most users hit a failure on. Two rankings out of one array, twelve bars
 * each, and a table carrying every module either of them was taken from.
 */
export const Modules = {
  parameters: { msw: { handlers: [slicingHandler()] } },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitForCharts(canvasElement);

    await step("the section says what it counts and which filters it follows", async () => {
      // The default range, not the 24 months of cells the server keeps: the
      // modules ARE clipped to the range, so the heading has to say which.
      expect(canvas.getByText("Modules (12 months)")).toBeInTheDocument();
      expect(canvasElement.querySelector('[data-testid="stats-modules"]')).toBeTruthy();
      const caption = canvas
        .getByText(/counts identified users once per module/)
        .textContent.replace(/\s+/g, " ");
      expect(caption).toContain("Within the last 24 months, over the selected range");
      expect(caption).toContain("however many times a module failed for them");
      expect(caption).toContain("family and plan filters apply");
      expect(caption).toContain("variant and certification filters do not");
      expect(canvasElement.querySelector('[data-testid="stats-modules-empty"]')).toBeNull();
    });

    await step("most-run modules: twelve bars, one hue, no legend", async () => {
      const chart = await moduleChart(canvasElement, "stats-modules-runs");
      expect(chart.chartInstance.options.indexAxis).toBe("y");
      expect(chart.chartInstance.options.plugins.legend.display).toBe(false);
      expect(chart.chartInstance.data.datasets.length).toBe(1);
      // Twelve of the fifteen: the tail is the table's job, so the chart is
      // not left saying "showing the top 12 of 15" as well.
      expect(chart.labels.length).toBe(12);
      expect(
        canvasElement.querySelector('[data-testid="stats-modules"] [data-testid="cts-chart-more"]'),
      ).toBeNull();
      expect(chart.labels[0]).toBe(MOCK_STATS_MODULES[0].testName);
      // The 640-run tie breaks on the name, exactly as the server breaks it.
      expect(chart.labels.slice(5, 7)).toEqual([
        "fapi2-message-signing-final-signed-request-object",
        "fapi2-security-profile-final-par-without-request-uri",
      ]);
      // The measures the bar does not carry are one hover away, not only in
      // the table below.
      expect(chart.tooltipFooter(0)).toEqual(["38 users", "9 hit a failure (23.7%)"]);
    });

    await step("...and the failing ranking is re-ranked, not the delivered order", async () => {
      const chart = await moduleChart(canvasElement, "stats-modules-failing");
      expect(chart.labels.length).toBe(12);
      expect(chart.labels.slice(0, 4)).toEqual([
        "fapi2-message-signing-final-signed-request-object",
        "fapi2-security-profile-final-user-rejects-authentication",
        "oidcc-server",
        "openid-federation-op-fetch-endpoint",
      ]);
      // Three modules on nine failing users. The busier one wins the tie, the
      // way the server's BY_FAILING_USERS breaks it — which here is the exact
      // reverse of what the name alone would have given.
      expect(chart.labels.slice(4, 7)).toEqual([
        "fapi2-security-profile-final-ensure-request-object-signature-algorithm-is-not-none",
        "fapi-ciba-id1-poll-happy-path",
        "fapi1-advanced-final-ensure-request-object-signature-algorithm-is-not-none",
      ]);
      // A bar of 24 means nothing without the denominator.
      expect(chart.tooltipFooter(0)).toEqual(["24 of 24 users (100.0%)", "640 runs"]);
      // The module nobody failed is not on this chart at all.
      expect(chart.labels).not.toContain("oidcc-discovery-endpoint-verification");
    });

    await step("the table carries every module, with the share as a percentage", async () => {
      const { summary, hint, headers, rows } = moduleTable(canvasElement);
      // Fifteen modules, twelve bars: the hint says where the other three went.
      expect(hint).toContain("The charts above plot the top 12 of each ranking");
      expect(summary).toBe(`All modules (${MOCK_STATS_MODULES.length})`);
      expect(headers).toEqual([
        "Module",
        "Runs",
        "Users",
        "Users who hit a failure",
        "Failing share",
      ]);
      expect(rows.length).toBe(MOCK_STATS_MODULES.length);
      expect(rows[0]).toEqual([
        "fapi2-security-profile-final-ensure-request-object-signature-algorithm-is-not-none",
        "1,420",
        "38",
        "9",
        "23.7%",
      ]);
      // Nobody failed it: 0.0%, not a blank, and it is here even though only
      // one of the two charts would ever have shown it.
      expect(rows[2]).toEqual(["oidcc-discovery-endpoint-verification", "960", "44", "0", "0.0%"]);
      // The last three rows are exactly what the runs chart left out.
      expect(rows[14][0]).toBe("oid4vp-1final-verifier-invalid-nonce");
    });

    await step("and it follows the family filter", async () => {
      await userEvent.selectOptions(select(canvasElement, "stats-family"), "OID4VP");
      await waitFor(() => {
        expect(
          canvasElement.querySelectorAll('[data-testid="stats-modules-table"] tbody tr').length,
        ).toBe(2);
      }, POLL_TIMEOUT);
      const chart = await moduleChart(canvasElement, "stats-modules-runs");
      expect(chart.labels).toEqual([
        "oid4vp-1final-verifier-happy-path",
        "oid4vp-1final-verifier-invalid-nonce",
      ]);
      // Two modules, two bars: nothing was cut, so the hint no longer claims
      // anything is only in the table.
      expect(moduleTable(canvasElement).hint).toBe(
        "Most-run first, the order the server ranks them in.",
      );
    });

    await step("the heading follows the range, because the counts do", async () => {
      await userEvent.selectOptions(select(canvasElement, "stats-family"), "");
      await pickRange(canvasElement, "26 weeks");
      await waitFor(() => {
        // Module cells are monthly, so a weekly range is answered with the
        // whole months its weeks fall in — the heading says as much.
        expect(canvas.getByText("Modules (26 weeks, whole months)")).toBeInTheDocument();
      }, POLL_TIMEOUT);
      await pickRange(canvasElement, "All time");
      await waitFor(() => {
        // The one range wider than the window the server keeps cells for.
        expect(canvas.getByText("Modules (last 24 months)")).toBeInTheDocument();
      }, POLL_TIMEOUT);
    });

    await step("a synthetic family has no modules, and the section says so", async () => {
      // "No plan" is runs that belong to no plan at all, so no module maps to
      // it — the server answers `[]`. The page is NOT in its no-match state
      // here (the family has traffic), so the section is on screen and has to
      // account for itself.
      await userEvent.selectOptions(select(canvasElement, "stats-family"), "No plan");
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="stats-modules-empty"]')).toBeTruthy();
      }, POLL_TIMEOUT);
      expect(canvasElement.querySelector('[data-testid="stats-no-match"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="stats-modules-runs"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="stats-modules-table"]')).toBeNull();
    });
  },
};

/**
 * A window with no module runs in it — a synthetic family, or a database
 * quiet for two years. The section stays and says so, rather than leaving the
 * reader wondering whether it failed to load.
 */
export const ModulesEmpty = {
  parameters: {
    msw: {
      handlers: [
        http.get(ENDPOINT, ({ request }) => {
          const url = new URL(request.url);
          REQUESTS.push(url.search);
          const body = statisticsOverviewFor(url);
          body.data.modules = [];
          return HttpResponse.json(body);
        }),
      ],
    },
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitForCharts(canvasElement);

    await step("one sentence instead of two empty charts and an empty table", async () => {
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="stats-modules-empty"]')).toBeTruthy();
      }, POLL_TIMEOUT);
      expect(
        canvas.getByText("No module runs in this window for the current filters."),
      ).toBeInTheDocument();
      expect(canvasElement.querySelector('[data-testid="stats-modules-runs"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="stats-modules-failing"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="stats-modules-table"]')).toBeNull();
      // The heading and its caption stay: an empty section still has to say
      // what it would have counted.
      expect(canvas.getByText("Modules (12 months)")).toBeInTheDocument();
    });
  },
};

/**
 * A link into a filtered view: the page must open on exactly what was shared,
 * including a plan the current payload no longer offers as an option.
 */
export const DeepLinkedFilters = {
  parameters: { msw: { handlers: [slicingHandler()] } },
  beforeEach() {
    history.replaceState(
      null,
      "",
      "/iframe.html?range=24m&family=FAPI1+Advanced&plan=fapi1-advanced-final-test-plan" +
        "&variant.fapi_profile=openbanking_brazil",
    );
    REQUESTS.length = 0;
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await waitForCharts(canvasElement);

    await step("the shared filters are what the first request asks for", async () => {
      expect(filteredRequests()[0]).toBe(
        "?granularity=month&from=2024-07&family=FAPI1+Advanced" +
          "&plan=fapi1-advanced-final-test-plan&variant.fapi_profile=openbanking_brazil",
      );
    });

    await step("every control shows the filter it is carrying", async () => {
      await waitFor(() => {
        expect(select(canvasElement, "stats-family").value).toBe("FAPI1 Advanced");
      }, POLL_TIMEOUT);
      expect(
        canvasElement
          .querySelector('[data-testid="stats-range-monthly"] button[data-range="24m"]')
          .getAttribute("aria-pressed"),
      ).toBe("true");
      // The payload's dimensions only carry the selected plan, and the page
      // has nothing remembered on a cold open — it must still be selected.
      expect(select(canvasElement, "stats-plan").value).toBe("fapi1-advanced-final-test-plan");
      expect(select(canvasElement, "stats-variant-fapi_profile").value).toBe("openbanking_brazil");
      expect(canvasElement.querySelector('[data-testid="stats-clear-filters"]')).toBeTruthy();
    });

    await step("and the charts show that family alone", async () => {
      expect(chartTable(canvasElement, "stats-chart-runs").headers).toEqual([
        "Month",
        "FAPI1 Advanced",
      ]);
    });
  },
};

/**
 * Drill-down: a chart bar is a link to the plans behind it.
 *
 * The page builds the `plans.html?…` URL and emits a cancelable
 * `cts-drill-down` before navigating, which is what lets this story assert
 * the URL without the iframe leaving for another page. Production has no
 * listener, so a click there navigates.
 */
export const DrillDown = {
  parameters: { msw: { handlers: [slicingHandler()] } },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await waitForCharts(canvasElement);
    const page = canvasElement.querySelector("cts-statistics-page");
    /** @type {Array<string>} */
    const urls = [];
    page.addEventListener("cts-drill-down", (/** @type {any} */ event) => {
      // Cancel the navigation: the assertion is the URL the page built.
      event.preventDefault();
      urls.push(event.detail.url);
    });

    const chart = runsChartInstance(canvasElement);
    const canvas = /** @type {HTMLCanvasElement} */ (
      canvasElement.querySelector('[data-testid="stats-chart-runs"] canvas')
    );

    // A click is hit-tested against where the marks ARE, and Chart.js animates
    // them in from the baseline. The story iframe's requestAnimationFrame
    // barely ticks in headless Chromium, so without this the bars would still
    // be flat on the axis and every click would miss. Jump to the final
    // layout — the state a real reader clicks at.
    chart.config.options.animation = false;
    chart.stop();
    await waitFor(() => {
      chart.update("none");
      const bar = chart.getDatasetMeta(0).data[0];
      expect(bar.base - bar.y).toBeGreaterThan(1);
    });

    /**
     * Click the middle of one bar segment, through a real mouse event so
     * Chart.js's own hit testing is what resolves it.
     * @param {number} datasetIndex - Which series.
     * @param {number} index - Which period.
     * @returns {void}
     */
    const clickBar = (datasetIndex, index) => {
      const bar = chart.getDatasetMeta(datasetIndex).data[index];
      const rect = canvas.getBoundingClientRect();
      canvas.dispatchEvent(
        new MouseEvent("click", {
          clientX: rect.left + bar.x,
          clientY: rect.top + (bar.y + bar.base) / 2,
          bubbles: true,
          cancelable: true,
        }),
      );
    };

    /**
     * The half-open date bounds of a `YYYY-MM` period, computed here rather
     * than imported so the assertion does not restate the implementation.
     * @param {string} period - The period key.
     * @returns {string} `from=…&to=…`.
     */
    const bounds = (period) => {
      const [year, month] = period.split("-").map(Number);
      const next =
        month === 12 ? `${year + 1}-01-01` : `${year}-${String(month + 1).padStart(2, "0")}-01`;
      return `from=${period}-01&to=${next}`;
    };

    await step("a click on a family's segment lists that family, that month", async () => {
      const datasetIndex = chart.data.datasets.findIndex(
        (/** @type {any} */ ds) => ds.label !== "Other",
      );
      const family = chart.data.datasets[datasetIndex].label;
      const values = chart.data.datasets[datasetIndex].data;
      const index = values.indexOf(Math.max(...values));
      const period = chart.data.labels[index];

      clickBar(datasetIndex, index);
      await waitFor(() => {
        expect(urls.length).toBe(1);
      });
      expect(urls[0]).toBe(
        `plans.html?family=${encodeURIComponent(family).replace(/%20/g, "+")}&${bounds(period)}`,
      );
    });

    await step("the folded Other series is refused, with a toast that says why", async () => {
      const datasetIndex = chart.data.datasets.findIndex(
        (/** @type {any} */ ds) => ds.label === "Other",
      );
      expect(datasetIndex).toBeGreaterThan(-1);
      const values = chart.data.datasets[datasetIndex].data;
      const index = values.indexOf(Math.max(...values));

      clickBar(datasetIndex, index);
      const toast = /** @type {HTMLElement} */ (
        await waitFor(() => {
          const found = document.querySelector("cts-toast-host cts-toast");
          expect(found).toBeTruthy();
          return found;
        })
      );
      // "Other" is a fold of the families outside the seven colour slots, so
      // /api/plan has no way to be asked for it.
      expect(toast.textContent).toContain("No drill-down for");
      expect(toast.textContent).toContain("Other");
      expect(urls.length).toBe(1);
      /** @type {any} */ (toast).dismiss();
      await waitFor(() => {
        expect(document.querySelector("cts-toast-host cts-toast")).toBeNull();
      });
    });

    await step("a data-table row is the keyboard route into a whole period", async () => {
      const rows = Array.from(
        canvasElement.querySelectorAll('[data-testid="stats-chart-runs"] .cts-chart-row-link'),
      );
      const period = rows[2].textContent.trim();
      expect(rows[2].getAttribute("aria-label")).toBe(`List the test plans in ${period}`);

      await userEvent.click(rows[2]);
      await waitFor(() => {
        expect(urls.length).toBe(2);
      });
      // A row names a period, not a series, and no family is filtered — so
      // the link narrows by the period alone.
      expect(urls[1]).toBe(`plans.html?${bounds(period)}`);
    });

    await step("with a family filtered, even the keyboard route carries it", async () => {
      await userEvent.selectOptions(select(canvasElement, "stats-family"), "OID4VP");
      await waitFor(() => {
        expect(location.search).toContain("family=OID4VP");
      }, POLL_TIMEOUT);
      await waitForCharts(canvasElement);

      const rows = Array.from(
        canvasElement.querySelectorAll('[data-testid="stats-chart-runs"] .cts-chart-row-link'),
      );
      const period = rows[1].textContent.trim();
      await userEvent.click(rows[1]);
      await waitFor(() => {
        expect(urls.length).toBe(3);
      });
      expect(urls[2]).toBe(`plans.html?family=OID4VP&${bounds(period)}`);
    });

    await step("a synthetic family is refused in its own words, not with advice", async () => {
      // Both are buckets the cube counts and the listing cannot be asked for,
      // and the filter row is already set to them: "pick a family" would be
      // telling the reader to do what they have just done.
      for (const [family, sentence] of [
        ["No plan", "Runs without a plan can't be listed"],
        ["Other / retired", "Unresolved plan names aren't in the registry"],
      ]) {
        await userEvent.selectOptions(select(canvasElement, "stats-family"), family);
        await waitFor(() => {
          // URLSearchParams spells a space as '+', not %20.
          expect(location.search).toContain(
            `family=${encodeURIComponent(family).replace(/%20/g, "+")}`,
          );
        }, POLL_TIMEOUT);
        await waitForCharts(canvasElement);

        const rows = Array.from(
          canvasElement.querySelectorAll('[data-testid="stats-chart-runs"] .cts-chart-row-link'),
        );
        await userEvent.click(rows[1]);
        const toast = /** @type {HTMLElement} */ (
          await waitFor(() => {
            const found = document.querySelector("cts-toast-host cts-toast");
            expect(found).toBeTruthy();
            return found;
          })
        );
        expect(toast.textContent).toContain(`No drill-down for “${family}”`);
        expect(toast.textContent).toContain(sentence);
        expect(toast.textContent).not.toContain("pick a family in the filter row");
        /** @type {any} */ (toast).dismiss();
        await waitFor(() => {
          expect(document.querySelector("cts-toast-host cts-toast")).toBeNull();
        });
      }
      // Refused, so nothing was navigated to.
      expect(urls.length).toBe(3);
    });
  },
};

/**
 * A filter combination the data has nothing for. The axis still comes back
 * full — the periods are the cube's, not the filter's — so without this the
 * reader would be left interpreting five charts of zeros.
 */
export const NoMatch = {
  parameters: { msw: { handlers: [slicingHandler()] } },
  beforeEach() {
    // A family that has never run: every series comes back zero.
    history.replaceState(null, "", "/iframe.html?range=12m&family=Shared+Signals+Framework");
    REQUESTS.length = 0;
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("one sentence stands in for five empty charts", async () => {
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="stats-no-match"]')).toBeTruthy();
      }, POLL_TIMEOUT);
      expect(canvas.getByText("No test plans match these filters")).toBeInTheDocument();
      expect(canvasElement.querySelector('[data-testid="stats-charts"]')).toBeNull();
      // Not the same thing as an empty database: the tiles and the filter row
      // are still there, and this is not the "no data at all" state.
      expect(canvasElement.querySelector('[data-testid="stats-empty"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="stats-tiles"]')).toBeTruthy();
      // Modules are withheld with the distributions: the server does not
      // narrow them by variant or certification profile, so under some
      // no-match queries they would otherwise sit full of bars directly under
      // a banner saying nothing matches.
      expect(canvasElement.querySelector('[data-testid="stats-modules"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="stats-filters"]')).toBeTruthy();
      // The filter that emptied it is selected, even though the select would
      // not otherwise offer a family with no runs.
      expect(select(canvasElement, "stats-family").value).toBe("Shared Signals Framework");
    });

    await step("and the way out is one click", async () => {
      await userEvent.click(
        canvasElement.querySelector('[data-testid="stats-no-match-clear"] button'),
      );
      await waitForCharts(canvasElement);
      expect(canvasElement.querySelector('[data-testid="stats-no-match"]')).toBeNull();
      expect(location.search).toBe("?range=12m");
    });
  },
};

/**
 * The unfiltered, whole-history view IS the baseline the colours and the
 * family options come from, so it must not be fetched twice.
 */
export const WholeHistoryNeedsNoBaseline = {
  parameters: { msw: { handlers: [slicingHandler()] } },
  beforeEach() {
    history.replaceState(null, "", "/iframe.html?range=all");
    REQUESTS.length = 0;
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await waitForCharts(canvasElement);

    await step("exactly one request, and no bare baseline one", async () => {
      expect(REQUESTS).toEqual(["?granularity=month"]);
    });

    await step("the family options and the colours come from it all the same", async () => {
      expect(select(canvasElement, "stats-family").options.length).toBe(11);
      const labels = runsChartInstance(canvasElement).data.datasets.map((d) => d.label);
      // Seven categorical families plus the folded tail — the full history, so
      // the family that retired early is on screen too.
      expect(labels).toContain("OpenID Connect Logout");
      expect(labels.at(-1)).toBe("Other");
    });

    await step("...and narrowing from here does fetch the baseline", async () => {
      await pickRange(canvasElement, "12 months");
      await waitFor(() => {
        expect(filteredRequests().at(-1)).toBe("?granularity=month&from=2025-07");
      }, POLL_TIMEOUT);
      // It was already adopted from the first payload, so still no bare one.
      expect(REQUESTS.filter((search) => search === "")).toHaveLength(0);
    });
  },
};

/**
 * The baseline is the slower of the two requests a narrowed view — the default
 * 12 months included — fires. It is what RANKS the families, and the rank is
 * the colour, so adopting the narrowed payload's own ranking first and the
 * baseline's a moment later would repaint all five charts and reorder the
 * family select in front of the reader. The first paint waits for it instead.
 */
export const BaselineAnswersAfterTheView = {
  parameters: { msw: { handlers: [slowBaselineHandler()] } },
  beforeEach() {
    ANSWERED.length = 0;
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await step("the narrowed payload does not paint on its own", async () => {
      await waitFor(() => {
        expect(ANSWERED).toEqual(["?granularity=month&from=2025-07"]);
      }, POLL_TIMEOUT);
      // Well inside the baseline's delay: without the wait the charts would
      // be up by now, in colours the baseline is about to change.
      await delay(BASELINE_DELAY_MS / 3);
      expect(ANSWERED.length).toBe(1);
      expect(canvasElement.querySelector('[data-testid="stats-charts"]')).toBeNull();
    });

    await step("the first paint is already ranked from the baseline", async () => {
      await waitForCharts(canvasElement);
      expect(ANSWERED).toContain("");
      // Busy in the fixture's first two months and retired since, so it is in
      // the all-time payload only: seeing it here is the baseline's ranking.
      expect(familyOptions(canvasElement)).toContain("OpenID Connect Logout");
    });

    await step("and nothing is repainted or reordered afterwards", async () => {
      const colours = coloursByFamily(canvasElement);
      const families = familyOptions(canvasElement);
      expect(Object.keys(colours).length).toBeGreaterThan(1);
      await delay(400);
      expect(coloursByFamily(canvasElement)).toEqual(colours);
      expect(familyOptions(canvasElement)).toEqual(families);
    });
  },
};

/**
 * A range or filter the server cannot use at all: nothing was computed, so
 * retrying the same request is pointless and the page offers the way out.
 */
export const InvalidQuery = {
  parameters: {
    msw: {
      handlers: [
        http.get(ENDPOINT, ({ request }) => {
          const url = new URL(request.url);
          REQUESTS.push(url.search);
          // What the server itself rejects: a variant parameter whose name is
          // not one (QueryParams.variant).
          const bad = [...url.searchParams.keys()].some(
            (key) => key.startsWith("variant.") && !/^[A-Za-z0-9_-]+$/.test(key.slice(8)),
          );
          return bad
            ? HttpResponse.json(MOCK_STATS_INVALID, { status: 400 })
            : HttpResponse.json(statisticsOverviewFor(url));
        }),
      ],
    },
  },
  beforeEach() {
    // A hand-edited or stale link, which is the realistic way to reach a 400.
    history.replaceState(null, "", "/iframe.html?range=12m&variant.bad%20name=x");
    REQUESTS.length = 0;
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await step("the server's own message is shown", async () => {
      await waitFor(() => {
        const alert = canvasElement.querySelector('[data-testid="stats-error"]');
        expect(alert).toBeTruthy();
        expect(alert.textContent).toContain("is not a variant parameter name");
        expect(alert.querySelector(".oidf-alert-danger")).toBeTruthy();
      }, POLL_TIMEOUT);
      // A 400 is terminal: the page must not poll a request the server has
      // already refused.
      expect(canvasElement.querySelector('[data-testid="stats-loading"]')).toBeNull();
    });

    await step("the way out is to reset the filters, not to retry", async () => {
      expect(canvasElement.querySelector('[data-testid="stats-retry"]')).toBeNull();
      const reset = canvasElement.querySelector('[data-testid="stats-reset-filters"] button');
      expect(reset.textContent.trim()).toBe("Reset filters");
      await userEvent.click(reset);
      await waitForCharts(canvasElement);
      expect(canvasElement.querySelector('[data-testid="stats-error"]')).toBeNull();
      expect(location.search).toBe("?range=12m");
    });
  },
};

/**
 * No snapshot exists yet: the endpoint answers 202 twice and the page polls
 * through to the first READY payload without ever showing an error.
 */
export const PendingThenReady = {
  parameters: {
    msw: {
      handlers: [
        (() => {
          let calls = 0;
          return http.get(ENDPOINT, ({ request }) => {
            const url = new URL(request.url);
            // The baseline request rides along with the real one and must not
            // move the counter that drives this story.
            if (isBaseline(url)) {
              return calls <= 2
                ? HttpResponse.json(MOCK_STATS_PENDING, { status: 202 })
                : HttpResponse.json(statisticsOverviewFor(url));
            }
            calls += 1;
            if (calls <= 2) {
              return HttpResponse.json(MOCK_STATS_PENDING, {
                status: 202,
                headers: { "Retry-After": "2" },
              });
            }
            return HttpResponse.json(statisticsOverviewFor(url));
          });
        })(),
      ],
    },
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await step("the first-computation state is announced while polling", async () => {
      await waitFor(() => {
        const loading = canvasElement.querySelector('[data-testid="stats-loading"]');
        expect(loading).toBeTruthy();
        expect(loading.getAttribute("label")).toBe("Computing statistics for the first time");
      });
      // Nothing to chart yet, and no error.
      expect(canvasElement.querySelector('[data-testid="stats-charts"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="stats-error"]')).toBeNull();
    });

    await step("polling lands on the snapshot and the loading block goes away", async () => {
      await waitForCharts(canvasElement);
      expect(canvasElement.querySelector('[data-testid="stats-loading"]')).toBeNull();
      const tiles = canvasElement.querySelectorAll('[data-testid="stats-tiles"] .cts-stats-tile');
      expect(tiles.length).toBe(10);
    });

    await step("the baseline that 202'd on load is picked up once one exists", async () => {
      // Without it the family select would be empty and every family would
      // wear the neutral.
      await waitFor(() => {
        expect(select(canvasElement, "stats-family").options.length).toBe(11);
      }, POLL_TIMEOUT);
    });
  },
};

/**
 * A 202 arriving when a payload is already on screen: the server restarted and
 * lost its cache, or the TTL expired while the page was open. The label must
 * say it is RE-computing — "for the first time" contradicts the (dimmed)
 * charts the admin is looking at.
 */
export const RecomputingOverSnapshot = {
  parameters: {
    msw: {
      handlers: [
        (() => {
          let calls = 0;
          return http.get(ENDPOINT, ({ request }) => {
            const url = new URL(request.url);
            if (isBaseline(url)) return HttpResponse.json(statisticsOverviewFor(url));
            calls += 1;
            // 1: the snapshot. 2: the forced recompute finds no cache at all.
            // 3+: the recompute has landed.
            if (calls === 2) {
              return HttpResponse.json(MOCK_STATS_PENDING, {
                status: 202,
                headers: { "Retry-After": "2" },
              });
            }
            return HttpResponse.json(statisticsOverviewFor(url));
          });
        })(),
      ],
    },
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await waitForCharts(canvasElement);
    const charts = canvasElement.querySelector('[data-testid="stats-charts"]');

    await step("the 202 is announced as a recompute, not a first computation", async () => {
      await userEvent.click(canvasElement.querySelector('[data-testid="stats-refresh"] button'));
      await waitFor(() => {
        const loading = canvasElement.querySelector('[data-testid="stats-loading"]');
        expect(loading).toBeTruthy();
        expect(loading.getAttribute("label")).toBe("Recomputing statistics");
      });
      // The snapshot the label is talking over is still on screen.
      expect(canvasElement.querySelectorAll(".cts-stats-chart canvas").length).toBe(CHART_COUNT);
      expect(charts.classList.contains("is-busy")).toBe(true);
      expect(canvasElement.querySelector('[data-testid="stats-error"]')).toBeNull();
    });

    await step("polling through to the new snapshot clears the label", async () => {
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="stats-loading"]')).toBeNull();
      }, POLL_TIMEOUT);
      expect(charts.classList.contains("is-busy")).toBe(false);
      expect(canvasElement.querySelectorAll(".cts-stats-chart canvas").length).toBe(CHART_COUNT);
    });
  },
};

/**
 * A newer snapshot is being computed while an older one is served: the
 * charts stay on screen, dimmed, and the page polls until `refreshing`
 * clears. Also covers the Refresh button's `?refresh=true`.
 */
export const RefreshingSnapshot = {
  parameters: {
    msw: {
      handlers: [
        http.get(ENDPOINT, async ({ request }) => {
          const url = new URL(request.url);
          REQUESTS.push(url.search);
          // Mirrors the endpoint: a forced refresh while a snapshot exists
          // answers 200 with refreshing:true, and the poll that follows
          // (which carries no ?refresh) finds the settled snapshot. The
          // refresh is slowed down so the play function can observe the
          // busy-but-unchanged render on its own.
          if (url.searchParams.get("refresh") !== "true") {
            return HttpResponse.json(statisticsOverviewFor(url));
          }
          await delay(400);
          return HttpResponse.json({
            ...MOCK_STATS_REFRESHING,
            data: statisticsOverviewFor(url).data,
          });
        }),
      ],
    },
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await waitForCharts(canvasElement);
    const charts = canvasElement.querySelector('[data-testid="stats-charts"]');

    await step("the initial request never asks for a recompute", async () => {
      expect(REQUESTS.every((search) => !search.includes("refresh"))).toBe(true);
      expect(charts.getAttribute("aria-busy")).toBe("false");
    });

    await step("Refresh dims the charts instead of tearing them down", async () => {
      const before = REQUESTS.length;
      const button = canvasElement.querySelector('[data-testid="stats-refresh"] button');
      // Count the re-plots across the busy toggle: the data has not changed,
      // so pushing it into Chart.js again would replay every animation for
      // nothing. The page hands <cts-chart> the same array instances until a
      // new payload arrives, and cts-chart only syncs when they change.
      const chart = runsChartInstance(canvasElement);
      const realUpdate = chart.update.bind(chart);
      let plots = 0;
      chart.update = (/** @type {Array<any>} */ ...args) => {
        plots += 1;
        return realUpdate(...args);
      };
      // Let the load settle first: the unfiltered baseline lands a moment
      // after the first payload and may legitimately re-colour the charts.
      await delay(300);
      plots = 0;
      await userEvent.click(button);
      await waitFor(() => {
        expect(REQUESTS.length).toBeGreaterThan(before);
      });
      expect(REQUESTS[before]).toBe("?granularity=month&from=2025-07&refresh=true");
      await waitFor(() => {
        expect(charts.classList.contains("is-busy")).toBe(true);
      });
      // The previous render is still mounted — no skeleton, no layout jump.
      expect(canvasElement.querySelectorAll(".cts-stats-chart canvas").length).toBe(CHART_COUNT);
      expect(charts.getAttribute("aria-busy")).toBe("true");
      expect(canvasElement.querySelector("cts-spinner")).toBeTruthy();
      expect(
        canvasElement
          .querySelector('[data-testid="stats-refresh"] button')
          .hasAttribute("disabled"),
      ).toBe(true);
      // Dimming is a class on the wrapper, not new data for the plots.
      expect(plots).toBe(0);
      chart.update = realUpdate;
    });

    await step("the poll that finds refreshing:false clears the busy state", async () => {
      await waitFor(() => {
        expect(charts.classList.contains("is-busy")).toBe(false);
      }, POLL_TIMEOUT);
      expect(canvasElement.querySelector("cts-spinner")).toBeNull();
      expect(
        canvasElement
          .querySelector('[data-testid="stats-refresh"] button')
          .hasAttribute("disabled"),
      ).toBe(false);
    });
  },
};

/**
 * A Refresh clicked while an earlier refresh's error is still on screen must
 * clear that error, not leave a message-less danger alert hanging over the
 * charts until the response lands.
 */
export const RefreshAfterFailedRefresh = {
  parameters: {
    msw: {
      handlers: [
        (() => {
          let calls = 0;
          return http.get(ENDPOINT, async ({ request }) => {
            const url = new URL(request.url);
            if (isBaseline(url)) return HttpResponse.json(statisticsOverviewFor(url));
            calls += 1;
            // 1: the snapshot. 2: the first Refresh fails. 3: the second
            // Refresh is slow, so the play can observe the interim state.
            if (calls === 2) return HttpResponse.json(MOCK_STATS_ERROR, { status: 500 });
            if (calls === 3) await delay(400);
            return HttpResponse.json(statisticsOverviewFor(url));
          });
        })(),
      ],
    },
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await waitForCharts(canvasElement);
    const charts = canvasElement.querySelector('[data-testid="stats-charts"]');
    const refresh = () =>
      /** @type {HTMLButtonElement} */ (
        canvasElement.querySelector('[data-testid="stats-refresh"] button')
      );

    await step("a failed Refresh reports without blanking the snapshot", async () => {
      await userEvent.click(refresh());
      await waitFor(() => {
        const alert = canvasElement.querySelector('[data-testid="stats-error"]');
        expect(alert).toBeTruthy();
        expect(alert.textContent).toContain("MongoSocketReadException");
      });
      expect(canvasElement.querySelector('[data-testid="stats-charts"]')).toBeTruthy();
      expect(canvasElement.querySelectorAll(".cts-stats-chart canvas").length).toBe(CHART_COUNT);
    });

    await step("clicking Refresh again clears the error immediately", async () => {
      await userEvent.click(refresh());
      await waitFor(() => {
        expect(charts.classList.contains("is-busy")).toBe(true);
      });
      // The whole point: no message-less danger alert while the request is
      // away, and the charts are still there behind the dim.
      expect(canvasElement.querySelector('[data-testid="stats-error"]')).toBeNull();
      expect(canvasElement.querySelectorAll(".cts-stats-chart canvas").length).toBe(CHART_COUNT);
      expect(canvasElement.querySelector('[data-testid="stats-tiles"]')).toBeTruthy();
    });

    await step("the slow response settles the page", async () => {
      await waitFor(() => {
        expect(charts.classList.contains("is-busy")).toBe(false);
      }, POLL_TIMEOUT);
      expect(canvasElement.querySelector('[data-testid="stats-error"]')).toBeNull();
    });
  },
};

/**
 * A failed recompute alongside a still-usable older snapshot: a dismissible
 * warning, and the charts unaffected.
 */
export const StaleWithLastError = {
  parameters: {
    msw: {
      handlers: [
        http.get(ENDPOINT, ({ request }) => {
          const url = new URL(request.url);
          return HttpResponse.json({
            ...MOCK_STATS_LAST_ERROR,
            data: statisticsOverviewFor(url).data,
          });
        }),
      ],
    },
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await waitForCharts(canvasElement);

    await step("the failed recompute is a warning, not a blocking error", async () => {
      const alert = canvasElement.querySelector('[data-testid="stats-last-error"]');
      expect(alert).toBeTruthy();
      expect(alert.textContent).toContain("MongoTimeoutException");
      expect(alert.querySelector(".oidf-alert-warning")).toBeTruthy();
      expect(canvasElement.querySelector('[data-testid="stats-error"]')).toBeNull();
    });

    await step("dismissing it does not bring it back on the next render", async () => {
      const close = canvasElement.querySelector(
        '[data-testid="stats-last-error"] .oidf-alert-close',
      );
      await userEvent.click(close);
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="stats-last-error"]')).toBeNull();
      });
      // Force another render (and another response carrying the same
      // lastError); the alert must stay dismissed.
      await pickRange(canvasElement, "24 months");
      expect(canvasElement.querySelector('[data-testid="stats-last-error"]')).toBeNull();
    });
  },
};

/**
 * Non-admin: the endpoint's 403 is the authoritative check, so the page says
 * so in place of everything else.
 */
export const Forbidden = {
  parameters: {
    msw: { handlers: [http.get(ENDPOINT, () => new HttpResponse(null, { status: 403 }))] },
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await step("an admin-only message replaces the whole dashboard", async () => {
      await waitFor(() => {
        const alert = canvasElement.querySelector('[data-testid="stats-forbidden"]');
        expect(alert).toBeTruthy();
        expect(alert.textContent).toContain("Statistics are only available to administrators.");
      });
      expect(canvasElement.querySelector('[data-testid="stats-tiles"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="stats-charts"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="stats-filters"]')).toBeNull();
      expect(canvasElement.querySelectorAll("canvas").length).toBe(0);
    });
  },
};

/**
 * No snapshot and computing one failed: the server's message plus a Retry
 * that actually re-requests.
 */
export const ErrorState = {
  parameters: {
    msw: {
      handlers: [
        (() => {
          let calls = 0;
          return http.get(ENDPOINT, ({ request }) => {
            const url = new URL(request.url);
            if (isBaseline(url)) return HttpResponse.error();
            calls += 1;
            // 1: the endpoint is unreachable. 2: it answers, but has no
            // snapshot and could not compute one. 3: it answers 200 with a
            // payload that violates the contract. 4: it recovers.
            if (calls === 1) return HttpResponse.error();
            if (calls === 2) return HttpResponse.json(MOCK_STATS_ERROR, { status: 500 });
            if (calls === 3) {
              return HttpResponse.json({
                status: "ready",
                computedAt: "2026-06-01T09:12:33Z",
                data: { families: "not-a-list", periods: ["2026-06"] },
              });
            }
            return HttpResponse.json(statisticsOverviewFor(url));
          });
        })(),
      ],
    },
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await step("an unreachable endpoint is reported, not swallowed", async () => {
      await waitFor(() => {
        const alert = canvasElement.querySelector('[data-testid="stats-error"]');
        expect(alert).toBeTruthy();
        expect(alert.textContent).toContain("Could not load statistics");
        expect(alert.querySelector(".oidf-alert-danger")).toBeTruthy();
      });
      expect(canvasElement.querySelector('[data-testid="stats-charts"]')).toBeNull();
      // The loading block must not be left spinning behind the alert.
      expect(canvasElement.querySelector('[data-testid="stats-loading"]')).toBeNull();
    });

    await step("Retry re-requests; a 500 shows the server's own message", async () => {
      const retry = canvasElement.querySelector('[data-testid="stats-retry"] button');
      expect(retry.textContent.trim()).toBe("Retry");
      await userEvent.click(retry);
      await waitFor(() => {
        const alert = canvasElement.querySelector('[data-testid="stats-error"]');
        expect(alert.textContent).toContain("MongoSocketReadException");
      });
    });

    await step("a 200 whose payload violates the contract is rejected outright", async () => {
      // Regression guard: the page must not commit half a payload — doing so
      // put `_payload` in front of render() while the shaping helpers could
      // not read it, which threw during the Lit update instead of reporting.
      const retry = canvasElement.querySelector('[data-testid="stats-retry"] button');
      await userEvent.click(retry);
      await waitFor(() => {
        const alert = canvasElement.querySelector('[data-testid="stats-error"]');
        expect(alert).toBeTruthy();
        expect(alert.textContent).toContain(
          "The statistics endpoint returned an unexpected response.",
        );
      });
      expect(canvasElement.querySelector('[data-testid="stats-loading"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="stats-charts"]')).toBeNull();
      // Nothing from the bad payload reached the page.
      expect(canvasElement.querySelector('[data-testid="stats-tiles"]')).toBeNull();
    });

    await step("a final Retry recovers the whole page", async () => {
      const retry = canvasElement.querySelector('[data-testid="stats-retry"] button');
      await userEvent.click(retry);
      await waitForCharts(canvasElement);
      expect(canvasElement.querySelector('[data-testid="stats-error"]')).toBeNull();
    });
  },
};

/**
 * A database with no test runs at all: the tiles still render (all zero), the
 * charts are replaced by an empty state — and the filter row stays, because
 * the range that emptied the view is the thing that has to be widened.
 */
export const Empty = {
  parameters: {
    msw: { handlers: [http.get(ENDPOINT, () => HttpResponse.json(MOCK_STATS_EMPTY))] },
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("zero tiles are shown rather than hidden", async () => {
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="stats-tiles"]')).toBeTruthy();
      });
      expect(
        canvasElement
          .querySelector('[data-testid="stat-tile-totalTests"] .cts-stats-tile-value')
          .textContent.trim(),
      ).toBe("0");
    });

    await step("an empty state stands in for the charts", async () => {
      const empty = canvasElement.querySelector('[data-testid="stats-empty"]');
      expect(empty).toBeTruthy();
      // The default range is 12 months, so this is "nothing here", not
      // "nothing anywhere" — the wording must not claim more than it knows.
      expect(canvas.getByText("Nothing in this range")).toBeInTheDocument();
      expect(canvasElement.querySelector('[data-testid="stats-charts"]')).toBeNull();
      expect(canvasElement.querySelectorAll("canvas").length).toBe(0);
    });

    await step("the range control stays, so the view can be widened", async () => {
      expect(canvasElement.querySelector('[data-testid="stats-range"]')).toBeTruthy();
      await pickRange(canvasElement, "All time");
      await waitFor(() => {
        expect(canvas.getByText("No test data yet")).toBeInTheDocument();
      }, POLL_TIMEOUT);
    });
  },
};
