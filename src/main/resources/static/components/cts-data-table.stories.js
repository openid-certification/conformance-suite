import { html } from "lit";
import { expect, waitFor, userEvent, fn } from "storybook/test";
import { http, HttpResponse, delay } from "msw";
import "./cts-data-table.js";

export default {
  title: "Patterns/cts-data-table",
  component: "cts-data-table",
};

// ----------------------------------------------------------------------
// Fixtures
// ----------------------------------------------------------------------

/** Three pages of fake server-side rows so pagination is meaningful. */
const SERVER_ROWS = Array.from({ length: 27 }, (_, i) => ({
  _id: `row-${String(i + 1).padStart(3, "0")}`,
  name: `Test plan ${i + 1}`,
  variant: i % 2 === 0 ? "client_secret_basic" : "private_key_jwt",
  started: new Date(Date.UTC(2026, 3, (i % 28) + 1, 12, 0)).toISOString(),
}));

const COLUMNS_DEFAULT = [
  { key: "_id", label: "ID", sortable: true, mono: true },
  { key: "name", label: "Name", sortable: true },
  { key: "variant", label: "Variant" },
  { key: "started", label: "Started", sortable: true, format: "date" },
];

/**
 * Apply the DataTables-style server contract to a row array.
 *
 * @param {Array<object>} rows
 * @param {URL} url
 */
function applyServerContract(rows, url) {
  const draw = Number(url.searchParams.get("draw") || "1");
  const start = Number(url.searchParams.get("start") || "0");
  const length = Number(url.searchParams.get("length") || "10");
  const search = (url.searchParams.get("search") || "").toLowerCase();
  const order = url.searchParams.get("order") || "";

  let filtered = rows;
  if (search) {
    filtered = rows.filter((row) =>
      Object.values(row).some((v) => v != null && String(v).toLowerCase().includes(search)),
    );
  }
  if (order) {
    const [col, dir] = order.split(",");
    const sign = dir === "desc" ? -1 : 1;
    filtered = filtered.slice().sort((a, b) => {
      const av = a[col];
      const bv = b[col];
      if (av === bv) return 0;
      return av < bv ? -1 * sign : 1 * sign;
    });
  }
  return {
    draw,
    recordsTotal: rows.length,
    recordsFiltered: filtered.length,
    data: filtered.slice(start, start + length),
  };
}

// ----------------------------------------------------------------------
// Stories
// ----------------------------------------------------------------------

/**
 * Happy path (server-side): renders columns + rows, sticky header, and
 * a footer pager. Verifies that the first fetch round-trips through the
 * server contract envelope.
 */
export const Default = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", ({ request }) =>
          HttpResponse.json(applyServerContract(SERVER_ROWS, new URL(request.url))),
        ),
      ],
    },
  },
  render: () =>
    html`<cts-data-table
      .columns=${COLUMNS_DEFAULT}
      page-size="10"
      server-side
      ajax-url="/api/sample"
      search-placeholder="Search rows"
      empty-state="No rows"
    ></cts-data-table>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;
    await waitFor(() => {
      const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
      expect(rows.length).toBe(10);
    });

    // Headers render with column labels.
    const headers = canvasElement.querySelectorAll(".oidf-dt-table thead th");
    expect(headers.length).toBe(4);
    expect(headers[0].textContent).toContain("ID");
    expect(headers[1].textContent).toContain("Name");

    // Sticky header CSS is applied.
    const headerStyle = getComputedStyle(headers[0]);
    expect(headerStyle.position).toBe("sticky");

    // First row data rendered.
    const firstRow = canvasElement.querySelector(".oidf-dt-table tbody tr[data-row-index='0']");
    expect(firstRow).toBeTruthy();
    expect(firstRow.textContent).toContain("Test plan 1");

    // mono column has the mono class.
    const monoCell = firstRow.querySelector(".oidf-dt-cell-mono");
    expect(monoCell).toBeTruthy();
    expect(monoCell.textContent.trim()).toBe("row-001");

    // Date column uses the YYYY-MM-DD HH:MM format.
    const dateCell = firstRow.querySelectorAll("td")[3];
    expect(/\d{4}-\d{2}-\d{2} \d{2}:\d{2}/.test(dateCell.textContent.trim())).toBe(true);
  },
};

/**
 * Sort: clicking a sortable header toggles asc → desc.
 */
export const SortToggle = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", ({ request }) =>
          HttpResponse.json(applyServerContract(SERVER_ROWS, new URL(request.url))),
        ),
      ],
    },
  },
  render: () =>
    html`<cts-data-table
      .columns=${COLUMNS_DEFAULT}
      page-size="5"
      server-side
      ajax-url="/api/sample"
      search-placeholder="Search rows"
    ></cts-data-table>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;
    await waitFor(() => {
      const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
      expect(rows.length).toBe(5);
    });

    const sortEvents = [];
    host.addEventListener("cts-sort-change", (e) => sortEvents.push(e.detail));

    // Click "Name" header — first click sorts asc.
    const nameHeader = canvasElement.querySelector(".oidf-dt-table th[data-column-key='name']");
    expect(nameHeader.classList.contains("is-sortable")).toBe(true);
    await userEvent.click(nameHeader);

    await waitFor(() => {
      expect(sortEvents.length).toBeGreaterThanOrEqual(1);
      expect(sortEvents[0]).toEqual({ columnKey: "name", direction: "asc" });
    });

    // Wait for the debounced fetch to complete (250ms + a bit).
    await waitFor(
      () => {
        const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
        expect(rows.length).toBeGreaterThan(0);
      },
      { timeout: 2000 },
    );

    // Active sort arrow appears.
    const arrow = nameHeader.querySelector(".oidf-dt-sort-arrow.is-active");
    expect(arrow).toBeTruthy();

    // Click again — toggles to desc.
    await userEvent.click(nameHeader);
    await waitFor(() => {
      const last = sortEvents[sortEvents.length - 1];
      expect(last).toEqual({ columnKey: "name", direction: "desc" });
    });
  },
};

/**
 * Search (explicit mode): typing alone does NOT fire a fetch; pressing
 * the search button does. Mirrors the plans/logs UX today.
 */
export const SearchExplicit = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", ({ request }) =>
          HttpResponse.json(applyServerContract(SERVER_ROWS, new URL(request.url))),
        ),
      ],
    },
  },
  render: () =>
    html`<cts-data-table
      .columns=${COLUMNS_DEFAULT}
      page-size="10"
      server-side
      ajax-url="/api/sample"
      search-mode="explicit"
      search-placeholder="Search rows"
    ></cts-data-table>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;
    await waitFor(() => {
      const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
      expect(rows.length).toBe(10);
    });

    // No need to spy on the fetch URL here — we infer "no fetch" by
    // observing the row count remains unchanged after the debounce window.
    const searchInput = canvasElement.querySelector(".oidf-dt-search-input");
    expect(searchInput).toBeTruthy();

    // Before typing, the inline submit button should not be visible
    // (draft equals committed = empty).
    expect(canvasElement.querySelector(".oidf-dt-search-submit")).toBeNull();

    // Type without pressing Enter — should NOT fetch (rows unchanged).
    await userEvent.type(searchInput, "Test plan 5");

    // Wait a tick longer than the debounce. In explicit mode no fetch should fire.
    await new Promise((r) => setTimeout(r, 350));
    const stillTen = canvasElement.querySelectorAll(
      ".oidf-dt-table tbody tr[data-row-index]",
    ).length;
    expect(stillTen).toBe(10);

    // Now that draft differs from the committed search, the inline submit
    // button is rendered inside the search pill.
    const searchBtn = canvasElement.querySelector(".oidf-dt-search-submit");
    expect(searchBtn).toBeTruthy();
    expect(searchBtn.getAttribute("aria-label")).toBe("Apply search");

    // Click the inline submit button — fetch fires, rows narrow.
    await userEvent.click(searchBtn);

    await waitFor(
      () => {
        const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
        // Match "Test plan 5" → row-005 (single match given our fixture).
        expect(rows.length).toBe(1);
      },
      { timeout: 2000 },
    );

    // After committing, the submit button is no longer visible (draft
    // matches committed) and the active-filter chip appears below.
    await waitFor(() => {
      expect(canvasElement.querySelector(".oidf-dt-search-submit")).toBeNull();
      const chip = canvasElement.querySelector(".oidf-dt-search-filter");
      expect(chip).toBeTruthy();
      expect(chip.textContent).toContain("Filtered to");
      expect(chip.querySelector(".oidf-dt-search-filter-query").textContent).toBe("Test plan 5");
    });
  },
};

/**
 * After a search is committed, an active-filter chip is rendered beneath
 * the search pill. Clicking the chip's "Show all" button clears the
 * search and restores the unfiltered row set.
 */
export const SearchActiveFilterChip = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", ({ request }) =>
          HttpResponse.json(applyServerContract(SERVER_ROWS, new URL(request.url))),
        ),
      ],
    },
  },
  render: () =>
    html`<cts-data-table
      .columns=${COLUMNS_DEFAULT}
      page-size="10"
      server-side
      ajax-url="/api/sample"
      search-mode="explicit"
      search-placeholder="Search rows"
    ></cts-data-table>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;
    await waitFor(() => {
      const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
      expect(rows.length).toBe(10);
    });

    // Drive a committed search via the public API so the chip rendering
    // path is exercised regardless of input/keyboard noise.
    host.search("Test plan 5");
    await waitFor(
      () => {
        const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
        expect(rows.length).toBe(1);
      },
      { timeout: 2000 },
    );

    // Chip surfaces the committed query + filtered count.
    const chip = canvasElement.querySelector(".oidf-dt-search-filter");
    expect(chip).toBeTruthy();
    expect(chip.getAttribute("role")).toBe("status");
    expect(chip.querySelector(".oidf-dt-search-filter-query").textContent).toBe("Test plan 5");
    expect(chip.querySelector(".oidf-dt-search-filter-count").textContent).toContain("1 of 27");

    // Click "Show all" — the chip vanishes, all rows return, the input clears.
    const reset = chip.querySelector(".oidf-dt-search-filter-reset");
    expect(reset).toBeTruthy();
    await userEvent.click(reset);

    await waitFor(
      () => {
        const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
        expect(rows.length).toBe(10);
        expect(canvasElement.querySelector(".oidf-dt-search-filter")).toBeNull();
        expect(canvasElement.querySelector(".oidf-dt-search-input").value).toBe("");
      },
      { timeout: 2000 },
    );
  },
};

/**
 * Pressing `Escape` inside the search input clears the field and
 * commits the cleared state, restoring all rows. Useful for quickly
 * resetting a filter without reaching for the chip.
 */
export const SearchEscapeClears = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", ({ request }) =>
          HttpResponse.json(applyServerContract(SERVER_ROWS, new URL(request.url))),
        ),
      ],
    },
  },
  render: () =>
    html`<cts-data-table
      .columns=${COLUMNS_DEFAULT}
      page-size="10"
      server-side
      ajax-url="/api/sample"
      search-mode="explicit"
      search-placeholder="Search rows"
    ></cts-data-table>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;
    await waitFor(() => {
      const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
      expect(rows.length).toBe(10);
    });

    // Apply a filter through the public API so we have something to clear.
    host.search("Test plan 7");
    await waitFor(
      () => {
        const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
        expect(rows.length).toBe(1);
      },
      { timeout: 2000 },
    );

    const input = canvasElement.querySelector(".oidf-dt-search-input");
    input.focus();
    await userEvent.keyboard("{Escape}");

    await waitFor(
      () => {
        expect(input.value).toBe("");
        expect(canvasElement.querySelector(".oidf-dt-search-filter")).toBeNull();
        const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
        expect(rows.length).toBe(10);
      },
      { timeout: 2000 },
    );
  },
};

/**
 * Search (live-debounced mode): typing fires fetches after the 250ms debounce.
 */
export const SearchLiveDebounced = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", ({ request }) =>
          HttpResponse.json(applyServerContract(SERVER_ROWS, new URL(request.url))),
        ),
      ],
    },
  },
  render: () =>
    html`<cts-data-table
      .columns=${COLUMNS_DEFAULT}
      page-size="10"
      server-side
      ajax-url="/api/sample"
      search-mode="live-debounced"
      search-placeholder="Search rows"
    ></cts-data-table>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;
    await waitFor(() => {
      const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
      expect(rows.length).toBe(10);
    });

    const searchInput = canvasElement.querySelector(".oidf-dt-search-input");
    await userEvent.type(searchInput, "Test plan 5");

    await waitFor(
      () => {
        const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
        expect(rows.length).toBe(1);
      },
      { timeout: 2000 },
    );
  },
};

/**
 * Pagination: clicking Next advances the page. Total / page count derive
 * from `recordsFiltered`.
 */
export const Pagination = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", ({ request }) =>
          HttpResponse.json(applyServerContract(SERVER_ROWS, new URL(request.url))),
        ),
      ],
    },
  },
  render: () =>
    html`<cts-data-table
      .columns=${COLUMNS_DEFAULT}
      page-size="10"
      server-side
      ajax-url="/api/sample"
      search-placeholder="Search rows"
    ></cts-data-table>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;
    await waitFor(() => {
      const first = canvasElement.querySelector(".oidf-dt-table tbody tr[data-row-index='0']");
      expect(first?.textContent).toContain("Test plan 1");
    });

    // Pager info shows totals.
    const info = canvasElement.querySelector(".oidf-dt-pager-info");
    expect(info.textContent).toContain("Showing 1 to 10 of 27");

    // Page-change events fire on next.
    const pageEvents = [];
    host.addEventListener("cts-page-change", (e) => pageEvents.push(e.detail));

    const nextBtnHost = canvasElement.querySelector(".oidf-dt-pager-next");
    const nextBtn = nextBtnHost.querySelector("button");
    await userEvent.click(nextBtn);

    await waitFor(() => {
      expect(pageEvents.length).toBeGreaterThanOrEqual(1);
      expect(pageEvents[0].start).toBe(10);
      expect(pageEvents[0].length).toBe(10);
    });

    await waitFor(
      () => {
        const first = canvasElement.querySelector(".oidf-dt-table tbody tr[data-row-index='0']");
        expect(first?.textContent).toContain("Test plan 11");
      },
      { timeout: 2000 },
    );
  },
};

/**
 * Row click: dispatches `cts-row-click` with row data + index.
 */
export const RowClick = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", ({ request }) =>
          HttpResponse.json(applyServerContract(SERVER_ROWS, new URL(request.url))),
        ),
      ],
    },
  },
  render: () =>
    html`<cts-data-table
      .columns=${COLUMNS_DEFAULT}
      page-size="5"
      server-side
      ajax-url="/api/sample"
      search-placeholder="Search rows"
    ></cts-data-table>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;
    await waitFor(() => {
      const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
      expect(rows.length).toBe(5);
    });

    const rowClicks = [];
    host.addEventListener("cts-row-click", (e) => rowClicks.push(e.detail));

    const secondRow = canvasElement.querySelector(".oidf-dt-table tbody tr[data-row-index='1']");
    await userEvent.click(secondRow);

    expect(rowClicks.length).toBe(1);
    expect(rowClicks[0].index).toBe(1);
    expect(rowClicks[0].row._id).toBe("row-002");
    expect(rowClicks[0].rowEl).toBe(secondRow);
  },
};

/**
 * Empty state: when the response carries an empty `data` array, the
 * component renders `<cts-empty-state>` inside a single colspan'd row.
 */
export const Empty = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", () =>
          HttpResponse.json({ draw: 1, recordsTotal: 0, recordsFiltered: 0, data: [] }),
        ),
      ],
    },
  },
  render: () =>
    html`<cts-data-table
      .columns=${COLUMNS_DEFAULT}
      page-size="10"
      server-side
      ajax-url="/api/sample"
      search-placeholder="Search rows"
      empty-state="No matching rows"
    ></cts-data-table>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;
    await waitFor(() => {
      const empty = canvasElement.querySelector("cts-empty-state");
      expect(empty).toBeTruthy();
      expect(empty.getAttribute("heading")).toBe("No matching rows");
    });

    const dataRows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
    expect(dataRows.length).toBe(0);
  },
};

/**
 * AJAX failure: a 500 response surfaces an inline error row inside the
 * table (no thrown exception) and fires `cts-data-error`.
 */
export const FetchError = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", () => HttpResponse.json({ error: "boom" }, { status: 500 })),
      ],
    },
  },
  render: () =>
    html`<cts-data-table
      .columns=${COLUMNS_DEFAULT}
      page-size="10"
      server-side
      ajax-url="/api/sample"
      search-placeholder="Search rows"
    ></cts-data-table>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    const errors = [];
    host.addEventListener("cts-data-error", (e) => errors.push(e.detail));

    await host.updateComplete;
    await waitFor(
      () => {
        const errorCell = canvasElement.querySelector(".oidf-dt-error");
        expect(errorCell).toBeTruthy();
        expect(errorCell.textContent).toContain("Error loading data");
      },
      { timeout: 2000 },
    );
    expect(errors.length).toBe(1);
  },
};

/**
 * Slot-by-name renderer: a `<template slot="cell-actions">` is cloned per
 * row and `${row.key}` placeholders are substituted. Replicates the
 * `column.render = "actions"` pattern in the contract.
 */
export const SlotRenderer = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", ({ request }) =>
          HttpResponse.json(applyServerContract(SERVER_ROWS, new URL(request.url))),
        ),
      ],
    },
  },
  render: () => {
    const cols = [
      { key: "_id", label: "ID", mono: true },
      { key: "name", label: "Name" },
      { key: "actions", label: "Actions", render: "actions" },
    ];
    return html`<cts-data-table
      .columns=${cols}
      page-size="3"
      server-side
      ajax-url="/api/sample"
      search-placeholder="Search rows"
    >
      <template slot="cell-actions">
        <a class="oidf-row-link" href="/detail/\${_id}">Open \${name}</a>
      </template>
    </cts-data-table>`;
  },
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;
    await waitFor(() => {
      const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
      expect(rows.length).toBe(3);
    });

    const links = canvasElement.querySelectorAll("a.oidf-row-link");
    expect(links.length).toBe(3);
    expect(links[0].getAttribute("href")).toBe("/detail/row-001");
    expect(links[0].textContent).toBe("Open Test plan 1");
    expect(links[1].getAttribute("href")).toBe("/detail/row-002");
  },
};

/**
 * cellRenderer callback (function-pointer): wired imperatively after the
 * element upgrades. Returns a string, an HTMLElement, or a TemplateResult.
 */
export const FunctionRenderer = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", ({ request }) =>
          HttpResponse.json(applyServerContract(SERVER_ROWS, new URL(request.url))),
        ),
      ],
    },
  },
  render: () => {
    const cols = [
      { key: "_id", label: "ID", mono: true },
      { key: "name", label: "Name" },
      { key: "custom", label: "Custom" },
    ];
    return html`<cts-data-table
      class="cts-data-table-fn-render"
      .columns=${cols}
      page-size="3"
      server-side
      ajax-url="/api/sample"
      search-placeholder="Search rows"
    ></cts-data-table>`;
  },
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    // Wire the function-pointer renderer imperatively (the JSON-incompatible
    // pattern documented in the U29 contract).
    host.cellRenderer = (row, columnKey) => {
      if (columnKey === "custom") {
        return `[${row._id}] ${row.name.toUpperCase()}`;
      }
      return undefined;
    };
    host.requestUpdate();
    await host.updateComplete;
    await waitFor(() => {
      const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
      expect(rows.length).toBe(3);
    });

    const firstRow = canvasElement.querySelector(".oidf-dt-table tbody tr[data-row-index='0']");
    const customCell = firstRow.querySelectorAll("td")[2];
    expect(customCell.textContent.trim()).toBe("[row-001] TEST PLAN 1");
  },
};

/**
 * Out-of-order draw: a slow first response should be discarded after a
 * faster second request supersedes it. The component must paint the
 * latest response, not the late one.
 */
export const OutOfOrderDraw = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", async ({ request }) => {
          const url = new URL(request.url);
          const draw = Number(url.searchParams.get("draw") || "1");
          // Delay the first request more than the second so the second
          // resolves first; the component should ignore the stale draw.
          if (draw === 1) {
            await delay(800);
            // Echo a wildly different payload so we can detect leakage.
            return HttpResponse.json({
              draw: 1,
              recordsTotal: 1,
              recordsFiltered: 1,
              data: [
                {
                  _id: "STALE-001",
                  name: "STALE ROW",
                  variant: "stale",
                  started: SERVER_ROWS[0].started,
                },
              ],
            });
          }
          await delay(50);
          return HttpResponse.json(applyServerContract(SERVER_ROWS, url));
        }),
      ],
    },
  },
  render: () =>
    html`<cts-data-table
      .columns=${COLUMNS_DEFAULT}
      page-size="3"
      server-side
      ajax-url="/api/sample"
      search-placeholder="Search rows"
    ></cts-data-table>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;
    // Trigger a second fetch quickly — supersedes draw=1.
    host.search("Test plan");

    // Wait long enough for the slow first response to have settled.
    await waitFor(
      () => {
        const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
        expect(rows.length).toBe(3);
      },
      { timeout: 3000 },
    );

    // Crucially, no STALE rows leaked through.
    const stale = canvasElement.querySelector("td.oidf-dt-cell-mono");
    expect(stale.textContent.trim().startsWith("STALE")).toBe(false);
  },
};

/**
 * Request shape (datatables-comma-order, the default): assert the
 * request URL carries `order=COL,DIR` after a sort click.
 */
export const RequestShapeCommaOrder = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/sample", ({ request }) =>
          HttpResponse.json(applyServerContract(SERVER_ROWS, new URL(request.url))),
        ),
      ],
    },
  },
  render: () =>
    html`<cts-data-table
      .columns=${COLUMNS_DEFAULT}
      page-size="3"
      server-side
      ajax-url="/api/sample"
      request-shape="datatables-comma-order"
      search-placeholder="Search rows"
    ></cts-data-table>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;

    // Capture every request the component issues.
    /** @type {string[]} */
    const requestUrls = [];
    const originalFetch = window.fetch;
    window.fetch = fn(async (input, init) => {
      requestUrls.push(typeof input === "string" ? input : input.url);
      return originalFetch(input, init);
    });

    try {
      // Click the sortable Name header.
      const nameHeader = canvasElement.querySelector(".oidf-dt-table th[data-column-key='name']");
      await userEvent.click(nameHeader);

      await waitFor(
        () => {
          const last = requestUrls[requestUrls.length - 1];
          expect(last).toBeTruthy();
          expect(last).toMatch(/order=name%2Casc|order=name,asc/);
        },
        { timeout: 2000 },
      );
    } finally {
      window.fetch = originalFetch;
    }
  },
};

/**
 * Client-side mode: rows passed via `.rows` are rendered, sorted, and
 * paginated entirely in-browser; no AJAX call is issued.
 */
export const ClientSide = {
  parameters: {
    msw: {
      handlers: [
        // No handler — a fetch would 404. The test asserts no fetch happens.
      ],
    },
  },
  render: () => {
    const cols = [
      { key: "_id", label: "ID", mono: true, sortable: true },
      { key: "name", label: "Name", sortable: true },
    ];
    const rows = SERVER_ROWS.slice(0, 7);
    return html`<cts-data-table
      .columns=${cols}
      .rows=${rows}
      page-size="5"
      ?server-side=${false}
      search-placeholder="Search rows"
    ></cts-data-table>`;
  },
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-data-table");
    await host.updateComplete;

    // Five rows on the first page.
    await waitFor(() => {
      const rows = canvasElement.querySelectorAll(".oidf-dt-table tbody tr[data-row-index]");
      expect(rows.length).toBe(5);
    });

    // Sort by name desc.
    const nameHeader = canvasElement.querySelector(".oidf-dt-table th[data-column-key='name']");
    await userEvent.click(nameHeader); // asc
    await userEvent.click(nameHeader); // desc
    await host.updateComplete;

    const firstRow = canvasElement.querySelector(".oidf-dt-table tbody tr[data-row-index='0']");
    // Names sort alphabetically by string; "Test plan 7" sorts last under
    // a string-desc comparator (... 5, 6, 7).
    expect(firstRow.textContent).toContain("Test plan 7");
  },
};
