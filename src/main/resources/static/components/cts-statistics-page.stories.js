import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { delay, http, HttpResponse } from "msw";
import {
  MOCK_STATS_EMPTY,
  MOCK_STATS_ERROR,
  MOCK_STATS_LAST_ERROR,
  MOCK_STATS_MONTHS,
  MOCK_STATS_PENDING,
  MOCK_STATS_READY,
  MOCK_STATS_REFRESHING,
} from "@fixtures/mock-statistics.js";
import "./cts-statistics-page.js";

export default {
  title: "Pages/cts-statistics-page",
  component: "cts-statistics-page",
};

const ENDPOINT = "/api/statistics/overview";

/**
 * Query strings the RefreshingSnapshot handler saw, so the play function can
 * assert that `?refresh=true` is sent only by the Refresh button.
 * @type {Array<string>}
 */
const REFRESH_LOG = [];

/** The page polls at 2 s; give every wait comfortable headroom over that. */
const POLL_TIMEOUT = { timeout: 10000 };

/**
 * Wait until the page has painted its four charts.
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
    expect(charts.length).toBe(4);
    expect(canvasElement.querySelectorAll(".cts-stats-chart canvas").length).toBe(4);
    // Chart.js is lazy-loaded on first connect, so the <canvas> is in the DOM
    // a beat before anything is painted into it. `chartInstance` is the
    // reliable "the plot exists" signal.
    expect(charts.filter((el) => el.chartInstance).length).toBe(4);
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

// --- Stories ---

/**
 * The ordinary case: a fresh snapshot, nine tiles, four charts.
 */
export const Ready = {
  parameters: {
    msw: { handlers: [http.get(ENDPOINT, () => HttpResponse.json(MOCK_STATS_READY))] },
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitForCharts(canvasElement);

    await step("nine unfiltered tiles carry the snapshot's counters", async () => {
      const tiles = canvasElement.querySelectorAll('[data-testid="stats-tiles"] .cts-stats-tile');
      expect(tiles.length).toBe(9);
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
      expect(canvas.getByText("Stuck / abandoned (>24 h)")).toBeInTheDocument();
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

    await step('the range group defaults to "12 months"', async () => {
      const group = canvasElement.querySelector('[data-testid="stats-range"]');
      expect(group.getAttribute("role")).toBe("group");
      expect(group.getAttribute("aria-label")).toBe("Range");
      const pressed = Array.from(group.querySelectorAll("button"))
        .filter((btn) => btn.getAttribute("aria-pressed") === "true")
        .map((btn) => btn.textContent.trim());
      expect(pressed).toEqual(["12 months"]);
    });

    await step("the family select lists only families with runs", async () => {
      const select = /** @type {HTMLSelectElement} */ (
        canvasElement.querySelector('[data-testid="stats-family"]')
      );
      expect(select.getAttribute("aria-label")).toBe("Spec family");
      const options = Array.from(select.options).map((option) => option.value);
      expect(options[0]).toBe("");
      // Shared Signals Framework has never run, so it is not offered.
      expect(options).not.toContain("Shared Signals Framework");
      expect(options).toContain("FAPI2 Security Profile");
      expect(options.length).toBe(11);
    });

    await step("four charts, each sliced to the default 12-month range in its table", async () => {
      const firstRowMonth = MOCK_STATS_MONTHS[MOCK_STATS_MONTHS.length - 12];
      for (const id of [
        "stats-chart-runs",
        "stats-chart-plans",
        "stats-chart-results",
        "stats-chart-users",
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
      expect(
        Array.from(details.querySelectorAll("tbody tr td")).map((cell) => cell.textContent.trim()),
      ).toEqual(unresolved.map((plan) => String(plan.runs)));
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

    await step("the users chart is captioned as unfiltered", async () => {
      const { headers } = chartTable(canvasElement, "stats-chart-users");
      expect(headers).toEqual(["Month", "Active", "New"]);
      // The heading, not getByText: cts-chart repeats it in the table caption.
      const heading = /** @type {HTMLElement} */ (
        canvasElement.querySelector('[data-testid="stats-chart-users"] h3')
      );
      expect(heading.textContent.trim()).toBe("Users per month — all families");
    });
  },
};

/**
 * The filter row scopes charts 1-3 (never the tiles, never the users chart)
 * and — the point of the whole slot mechanism — never repaints a family.
 */
export const RangeAndFamilyFilters = {
  parameters: {
    msw: { handlers: [http.get(ENDPOINT, () => HttpResponse.json(MOCK_STATS_READY))] },
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await waitForCharts(canvasElement);

    /** @type {Record<string, string>} Column → fill, before any filtering. */
    const paintBefore = {};

    await step("record each family's colour at the default 12-month range", async () => {
      for (const dataset of runsChartInstance(canvasElement).data.datasets) {
        paintBefore[dataset.label] = dataset.backgroundColor;
      }
      // Six categorical families plus the folded neutral tail — OpenID
      // Connect Logout is idle in the last 12 months, so it does not even
      // reach the default range's chart.
      expect(Object.keys(paintBefore).length).toBe(7);
    });

    await step("All time restores the full 14-month history", async () => {
      const button = Array.from(
        canvasElement.querySelectorAll('[data-testid="stats-range"] button'),
      ).find((btn) => btn.textContent.trim() === "All time");
      await userEvent.click(button);
      await waitFor(() => {
        expect(button.getAttribute("aria-pressed")).toBe("true");
      });
      expect(chartTable(canvasElement, "stats-chart-runs").rows.length).toBe(14);
      // The users chart follows the range even though it ignores the family.
      expect(chartTable(canvasElement, "stats-chart-users").rows.length).toBe(14);
      // The family idle in the last 12 months is back once the range widens.
      const { headers } = chartTable(canvasElement, "stats-chart-runs");
      expect(headers).toContain("OpenID Connect Logout");
    });

    await step(
      "...without repainting a family that was already on screen at 12 months",
      async () => {
        for (const dataset of runsChartInstance(canvasElement).data.datasets) {
          if (paintBefore[dataset.label] === undefined) continue;
          expect(dataset.backgroundColor).toBe(paintBefore[dataset.label]);
        }
      },
    );

    await step("back to 12 months narrows every chart table to 12 rows again", async () => {
      const button = Array.from(
        canvasElement.querySelectorAll('[data-testid="stats-range"] button'),
      ).find((btn) => btn.textContent.trim() === "12 months");
      await userEvent.click(button);
      await waitFor(() => {
        expect(button.getAttribute("aria-pressed")).toBe("true");
      });
      expect(chartTable(canvasElement, "stats-chart-runs").rows.length).toBe(12);
      expect(chartTable(canvasElement, "stats-chart-users").rows.length).toBe(12);
    });

    await step("a family idle in the last 12 months drops out", async () => {
      const { headers } = chartTable(canvasElement, "stats-chart-runs");
      expect(headers).not.toContain("OpenID Connect Logout");
      expect(headers).toContain("FAPI2 Security Profile");
    });

    await step("...but nobody is repainted — colour is identity, not rank", async () => {
      for (const dataset of runsChartInstance(canvasElement).data.datasets) {
        expect(dataset.backgroundColor).toBe(paintBefore[dataset.label]);
      }
    });

    await step("tiles stay unfiltered", async () => {
      expect(
        canvasElement
          .querySelector('[data-testid="stat-tile-totalTests"] .cts-stats-tile-value')
          .textContent.trim(),
      ).toBe("91,800");
    });

    await step("selecting a family reduces charts 1-3 to that family", async () => {
      const select = /** @type {HTMLSelectElement} */ (
        canvasElement.querySelector('[data-testid="stats-family"]')
      );
      await userEvent.selectOptions(select, "FAPI2 Security Profile");
      await waitFor(() => {
        expect(chartTable(canvasElement, "stats-chart-runs").headers).toEqual([
          "Month",
          "FAPI2 Security Profile",
        ]);
      });
      expect(chartTable(canvasElement, "stats-chart-plans").headers).toEqual([
        "Month",
        "FAPI2 Security Profile",
      ]);
      // Chart 3 switches to that family's own outcome mix...
      expect(chartTable(canvasElement, "stats-chart-results").headers[1]).toBe("PASSED");
      // ...and chart 4 is untouched by the family filter.
      expect(chartTable(canvasElement, "stats-chart-users").headers).toEqual([
        "Month",
        "Active",
        "New",
      ]);
    });

    await step("a tail family keeps its own name — it is not renamed Other", async () => {
      // Regression guard: "OpenID Federation" wears --chart-other, so an
      // unconditional foldOther would relabel its single series "Other" and
      // lose the name the user just picked.
      const select = /** @type {HTMLSelectElement} */ (
        canvasElement.querySelector('[data-testid="stats-family"]')
      );
      await userEvent.selectOptions(select, "OpenID Federation");
      await waitFor(() => {
        expect(chartTable(canvasElement, "stats-chart-runs").headers).toEqual([
          "Month",
          "OpenID Federation",
        ]);
      });
      expect(chartTable(canvasElement, "stats-chart-plans").headers).toEqual([
        "Month",
        "OpenID Federation",
      ]);
      const datasets = runsChartInstance(canvasElement).data.datasets;
      expect(datasets.length).toBe(1);
      expect(datasets[0].label).toBe("OpenID Federation");
      // ...and it keeps the neutral it has always worn.
      expect(datasets[0].backgroundColor).toBe(paintBefore["Other"]);
      await userEvent.selectOptions(select, "FAPI2 Security Profile");
      await waitFor(() => {
        expect(chartTable(canvasElement, "stats-chart-runs").headers[1]).toBe(
          "FAPI2 Security Profile",
        );
      });
    });

    await step("the selected family keeps its own slot colour", async () => {
      const datasets = runsChartInstance(canvasElement).data.datasets;
      expect(datasets.length).toBe(1);
      expect(datasets[0].backgroundColor).toBe(paintBefore["FAPI2 Security Profile"]);
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
          return http.get(ENDPOINT, () => {
            calls += 1;
            if (calls <= 2) {
              return HttpResponse.json(MOCK_STATS_PENDING, {
                status: 202,
                headers: { "Retry-After": "2" },
              });
            }
            return HttpResponse.json(MOCK_STATS_READY);
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
      expect(tiles.length).toBe(9);
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
          return http.get(ENDPOINT, () => {
            calls += 1;
            // 1: the snapshot. 2: the forced recompute finds no cache at all.
            // 3+: the recompute has landed.
            if (calls === 2) {
              return HttpResponse.json(MOCK_STATS_PENDING, {
                status: 202,
                headers: { "Retry-After": "2" },
              });
            }
            return HttpResponse.json(MOCK_STATS_READY);
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
      expect(canvasElement.querySelectorAll(".cts-stats-chart canvas").length).toBe(4);
      expect(charts.classList.contains("is-busy")).toBe(true);
      expect(canvasElement.querySelector('[data-testid="stats-error"]')).toBeNull();
    });

    await step("polling through to the new snapshot clears the label", async () => {
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="stats-loading"]')).toBeNull();
      }, POLL_TIMEOUT);
      expect(charts.classList.contains("is-busy")).toBe(false);
      expect(canvasElement.querySelectorAll(".cts-stats-chart canvas").length).toBe(4);
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
        http.get(ENDPOINT, ({ request }) => {
          const url = new URL(request.url);
          REFRESH_LOG.push(url.search);
          // Mirrors the endpoint: a forced refresh while a snapshot exists
          // answers 200 with refreshing:true, and the poll that follows
          // (which carries no ?refresh) finds the settled snapshot.
          return HttpResponse.json(
            url.searchParams.get("refresh") === "true" ? MOCK_STATS_REFRESHING : MOCK_STATS_READY,
          );
        }),
      ],
    },
  },
  render: () => html`<cts-statistics-page></cts-statistics-page>`,
  async play({ canvasElement, step }) {
    await waitForCharts(canvasElement);
    const charts = canvasElement.querySelector('[data-testid="stats-charts"]');

    await step("the initial request never asks for a recompute", async () => {
      // The log is module-scoped, so a re-run (watch mode, a retry) would
      // otherwise read the previous run's entries. The story's own first
      // request is already in it, so keep the last entry and drop the rest.
      REFRESH_LOG.splice(0, REFRESH_LOG.length - 1);
      expect(REFRESH_LOG[0]).toBe("");
      expect(charts.getAttribute("aria-busy")).toBe("false");
    });

    await step("Refresh dims the charts instead of tearing them down", async () => {
      const before = REFRESH_LOG.length;
      const button = canvasElement.querySelector('[data-testid="stats-refresh"] button');
      await userEvent.click(button);
      await waitFor(() => {
        expect(REFRESH_LOG.length).toBeGreaterThan(before);
      });
      expect(REFRESH_LOG[before]).toBe("?refresh=true");
      await waitFor(() => {
        expect(charts.classList.contains("is-busy")).toBe(true);
      });
      // The previous render is still mounted — no skeleton, no layout jump.
      expect(canvasElement.querySelectorAll(".cts-stats-chart canvas").length).toBe(4);
      expect(charts.getAttribute("aria-busy")).toBe("true");
      expect(canvasElement.querySelector("cts-spinner")).toBeTruthy();
      expect(
        canvasElement
          .querySelector('[data-testid="stats-refresh"] button')
          .hasAttribute("disabled"),
      ).toBe(true);
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
          return http.get(ENDPOINT, async () => {
            calls += 1;
            // 1: the snapshot. 2: the first Refresh fails. 3: the second
            // Refresh is slow, so the play can observe the interim state.
            if (calls === 2) return HttpResponse.json(MOCK_STATS_ERROR, { status: 500 });
            if (calls === 3) await delay(400);
            return HttpResponse.json(MOCK_STATS_READY);
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
      expect(canvasElement.querySelectorAll(".cts-stats-chart canvas").length).toBe(4);
    });

    await step("clicking Refresh again clears the error immediately", async () => {
      await userEvent.click(refresh());
      await waitFor(() => {
        expect(charts.classList.contains("is-busy")).toBe(true);
      });
      // The whole point: no message-less danger alert while the request is
      // away, and the charts are still there behind the dim.
      expect(canvasElement.querySelector('[data-testid="stats-error"]')).toBeNull();
      expect(canvasElement.querySelectorAll(".cts-stats-chart canvas").length).toBe(4);
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
    msw: { handlers: [http.get(ENDPOINT, () => HttpResponse.json(MOCK_STATS_LAST_ERROR))] },
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
      // Force another render; the alert must stay dismissed.
      const button = Array.from(
        canvasElement.querySelectorAll('[data-testid="stats-range"] button'),
      ).find((btn) => btn.textContent.trim() === "24 months");
      await userEvent.click(button);
      await waitFor(() => {
        expect(button.getAttribute("aria-pressed")).toBe("true");
      });
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
          return http.get(ENDPOINT, () => {
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
                data: { families: "not-a-list", months: ["2026-06"] },
              });
            }
            return HttpResponse.json(MOCK_STATS_READY);
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
 * A database with no test runs at all: the tiles still render (all zero) and
 * the charts are replaced by an empty state.
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
      expect(canvas.getByText("No test data yet")).toBeInTheDocument();
      expect(canvasElement.querySelector('[data-testid="stats-charts"]')).toBeNull();
      expect(canvasElement.querySelectorAll("canvas").length).toBe(0);
    });
  },
};
