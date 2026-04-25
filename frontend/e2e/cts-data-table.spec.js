import { test, expect } from "@playwright/test";
import { setupCommonRoutes, setupFailFast, expectNoUnmockedCalls } from "./helpers/routes.js";

/**
 * Component-level e2e for `cts-data-table`. Per-page integration (the
 * actual plans.html / logs.html / tokens.html migrations) lands in U37/U38
 * and gets its own spec. This file covers the contract surface in
 * isolation: server-side AJAX round-trip, the comma-order request shape,
 * out-of-order draw suppression, search / sort / pagination event flow,
 * empty state, and the slot-by-name renderer.
 *
 * The component is mounted dynamically inside `/login.html` because that
 * page already ships the importmap + Lit bundle and has minimal page-level
 * JS (so unrelated AJAX endpoints don't pollute the unmocked-call check).
 */

const FIXTURE_ROWS = Array.from({ length: 27 }, (_, i) => ({
  _id: `row-${String(i + 1).padStart(3, "0")}`,
  name: `Test plan ${i + 1}`,
  variant: i % 2 === 0 ? "client_secret_basic" : "private_key_jwt",
  started: new Date(Date.UTC(2026, 3, (i % 28) + 1, 12, 0)).toISOString(),
}));

/**
 * Apply the DataTables-style server contract from a request URL to a row
 * array. Mirrors the helper in cts-data-table.stories.js — kept duplicated
 * (rather than extracted to fixtures) because Playwright + Storybook
 * share no module resolution today.
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

/**
 * Mount cts-data-table inside the page and wait for its first render.
 * The component is loaded as an ES module via the page's existing
 * importmap; the page owns the Lit bundle and importmap aliases.
 *
 * The mount fixture is built imperatively (createElement + appendChild)
 * rather than via `innerHTML` to avoid the test-side security-lint warning
 * about HTML injection, even though the input is fully controlled here.
 *
 * `slotAction` describes a single anchor template inserted as
 * `<template slot="cell-actions"><a href="..." class="row-link">…</a></template>`
 * — the small surface needed by the slot-renderer test.
 *
 * @param {import('@playwright/test').Page} page
 * @param {object} init
 */
async function mountTable(page, init) {
  await page.evaluate(
    async ({ init }) => {
      // The cts-data-table module is served by the static dir at runtime;
      // tsc can't resolve absolute static URLs. Cast the host through any
      // below for property assignments — it's a custom element whose props
      // are JS class fields, not HTMLElement members.
      // @ts-ignore — runtime-served static path
      await import("/components/cts-data-table.js");
      await customElements.whenDefined("cts-data-table");

      const mount = document.createElement("div");
      mount.id = "mount";
      mount.style.padding = "24px";
      document.body.replaceChildren(mount);

      const host = /** @type {any} */ (document.createElement("cts-data-table"));
      host.columns = init.columns;
      host.pageSize = init.pageSize;
      host.serverSide = init.serverSide !== false;
      if (init.ajaxUrl) host.ajaxUrl = init.ajaxUrl;
      if (init.requestShape) host.requestShape = init.requestShape;
      if (init.searchMode) host.searchMode = init.searchMode;
      if (init.searchPlaceholder !== undefined) host.searchPlaceholder = init.searchPlaceholder;
      if (init.emptyState) host.emptyState = init.emptyState;
      if (init.rows) host.rows = init.rows;

      if (init.slotAction) {
        const template = document.createElement("template");
        template.setAttribute("slot", "cell-actions");
        const anchor = document.createElement("a");
        anchor.setAttribute("href", init.slotAction.href);
        anchor.setAttribute("class", "row-link");
        anchor.textContent = init.slotAction.text;
        template.content.appendChild(anchor);
        host.appendChild(template);
      }

      mount.appendChild(host);
      await host.updateComplete;
    },
    { init },
  );
}

test.describe("cts-data-table — server-side contract", () => {
  test.afterEach(async ({ page }) => {
    expectNoUnmockedCalls(page);
  });

  test("issues comma-order request and renders the envelope's data", async ({ page }) => {
    await setupFailFast(page);

    /** @type {string[]} */
    const requestUrls = [];
    await page.route("**/api/sample*", (route) => {
      const url = new URL(route.request().url());
      requestUrls.push(url.pathname + url.search);
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(applyServerContract(FIXTURE_ROWS, url)),
      });
    });

    await setupCommonRoutes(page);
    await page.goto("/login.html");

    await mountTable(page, {
      columns: [
        { key: "_id", label: "ID", sortable: true, mono: true },
        { key: "name", label: "Name", sortable: true },
      ],
      pageSize: 5,
      serverSide: true,
      ajaxUrl: "/api/sample",
      searchPlaceholder: "Search rows",
    });

    // Five rows visible.
    await expect(page.locator(".oidf-dt-table tbody tr[data-row-index]")).toHaveCount(5);

    // The first request includes draw, start, length, search.
    expect(requestUrls.length).toBeGreaterThan(0);
    const firstUrl = new URL(requestUrls[0], "http://localhost");
    expect(firstUrl.searchParams.get("draw")).toBe("1");
    expect(firstUrl.searchParams.get("start")).toBe("0");
    expect(firstUrl.searchParams.get("length")).toBe("5");

    // Click the sortable Name header — request gains order=name,asc.
    await page.locator(".oidf-dt-table th[data-column-key='name']").click();
    await expect
      .poll(() => requestUrls[requestUrls.length - 1], { timeout: 3000 })
      .toMatch(/order=name(?:%2C|,)asc/);
  });

  test("ignores out-of-order responses (mismatched draw)", async ({ page }) => {
    await setupFailFast(page);

    /**
     * Slow-walk the first response so it resolves AFTER the second.
     * The component should keep the second response on screen.
     */
    let callCount = 0;
    await page.route("**/api/sample*", async (route) => {
      callCount++;
      const url = new URL(route.request().url());
      const draw = Number(url.searchParams.get("draw") || "1");
      if (callCount === 1) {
        // Slow first response — return STALE data tagged as draw=1.
        await new Promise((r) => setTimeout(r, 700));
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            draw,
            recordsTotal: 1,
            recordsFiltered: 1,
            data: [
              {
                _id: "STALE-001",
                name: "STALE ROW",
                variant: "stale",
                started: FIXTURE_ROWS[0].started,
              },
            ],
          }),
        });
      }
      // Quick second response.
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(applyServerContract(FIXTURE_ROWS, url)),
      });
    });

    await setupCommonRoutes(page);
    await page.goto("/login.html");

    await mountTable(page, {
      columns: [
        { key: "_id", label: "ID", sortable: true, mono: true },
        { key: "name", label: "Name", sortable: true },
      ],
      pageSize: 5,
      serverSide: true,
      ajaxUrl: "/api/sample",
      searchPlaceholder: "Search rows",
    });

    // Trigger the second request immediately via the public search() API.
    await page.evaluate(() => {
      const host = /** @type {any} */ (document.querySelector("cts-data-table"));
      host.search("Test plan");
    });

    // Wait long enough for the slow first response to settle.
    await page.waitForTimeout(1100);

    // STALE row never paints; the second response wins.
    const monoCell = page.locator(".oidf-dt-table tbody td.oidf-dt-cell-mono").first();
    await expect(monoCell).not.toHaveText(/STALE/);
  });

  test("renders the empty state when the response has no data", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/sample*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ draw: 1, recordsTotal: 0, recordsFiltered: 0, data: [] }),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/login.html");

    await mountTable(page, {
      columns: [
        { key: "_id", label: "ID", mono: true },
        { key: "name", label: "Name" },
      ],
      pageSize: 5,
      serverSide: true,
      ajaxUrl: "/api/sample",
      searchPlaceholder: "Search rows",
      emptyState: "Nothing to show",
    });

    const empty = page.locator("cts-empty-state");
    await expect(empty).toHaveAttribute("heading", "Nothing to show");
    await expect(page.locator(".oidf-dt-table tbody tr[data-row-index]")).toHaveCount(0);
  });

  test("surfaces fetch failure as an inline error row, not a thrown exception", async ({
    page,
  }) => {
    await setupFailFast(page);

    await page.route("**/api/sample*", (route) =>
      route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ error: "boom" }),
      }),
    );

    await setupCommonRoutes(page);
    await page.goto("/login.html");

    await mountTable(page, {
      columns: [
        { key: "_id", label: "ID" },
        { key: "name", label: "Name" },
      ],
      pageSize: 5,
      serverSide: true,
      ajaxUrl: "/api/sample",
      searchPlaceholder: "Search rows",
    });

    await expect(page.locator(".oidf-dt-error")).toContainText("Error loading data");
  });

  test("explicit search mode does not fetch on keystroke, only on Enter or button", async ({
    page,
  }) => {
    await setupFailFast(page);

    let callCount = 0;
    await page.route("**/api/sample*", (route) => {
      callCount++;
      const url = new URL(route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(applyServerContract(FIXTURE_ROWS, url)),
      });
    });

    await setupCommonRoutes(page);
    await page.goto("/login.html");

    await mountTable(page, {
      columns: [
        { key: "_id", label: "ID", mono: true },
        { key: "name", label: "Name" },
      ],
      pageSize: 10,
      serverSide: true,
      ajaxUrl: "/api/sample",
      searchMode: "explicit",
      searchPlaceholder: "Search rows",
    });

    await expect(page.locator(".oidf-dt-table tbody tr[data-row-index]")).toHaveCount(10);
    expect(callCount).toBe(1);

    // Type without committing — should NOT fetch.
    await page.locator(".oidf-dt-search-input").fill("Test plan 5");
    await page.waitForTimeout(400);
    expect(callCount).toBe(1);

    // Press Enter — fetch fires.
    await page.locator(".oidf-dt-search-input").press("Enter");
    await expect(page.locator(".oidf-dt-table tbody tr[data-row-index]")).toHaveCount(1);
    expect(callCount).toBe(2);
  });

  test("slot-by-name template renders cells with row-key substitution", async ({ page }) => {
    await setupFailFast(page);

    await page.route("**/api/sample*", (route) => {
      const url = new URL(route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(applyServerContract(FIXTURE_ROWS, url)),
      });
    });

    await setupCommonRoutes(page);
    await page.goto("/login.html");

    await mountTable(page, {
      columns: [
        { key: "_id", label: "ID", mono: true },
        { key: "name", label: "Name" },
        { key: "actions", label: "Actions", render: "actions" },
      ],
      pageSize: 3,
      serverSide: true,
      ajaxUrl: "/api/sample",
      searchPlaceholder: "Search rows",
      slotAction: { href: "/x/${_id}", text: "Open ${name}" },
    });

    const links = page.locator("a.row-link");
    await expect(links).toHaveCount(3);
    await expect(links.first()).toHaveAttribute("href", "/x/row-001");
    await expect(links.first()).toHaveText("Open Test plan 1");
  });
});
