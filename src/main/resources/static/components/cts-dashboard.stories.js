import { html } from "lit";
import { expect, within, waitFor, fn } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_SERVER_INFO } from "@fixtures/mock-test-data.js";
import "./cts-dashboard.js";

// Stats-row fixtures. Real backend returns a PaginationResponse envelope
// (`{ data: [...], recordsTotal }`); the component accepts plain arrays too.
// We exercise both shapes: STATS_PLANS_SAMPLE is a plain array (the legacy
// compatibility path), STATS_LOGS_SAMPLE_ENVELOPE wraps the same content in
// the real backend envelope so the production wire shape is covered.
const STATS_PLANS_SAMPLE = [{ _id: "plan-1" }, { _id: "plan-2" }, { _id: "plan-3" }];

const STATS_LOGS_SAMPLE = [
  // INTERRUPTED is terminal (stopped before completion) and CREATED is a
  // pre-execution state — the in-progress filter must whitelist RUNNING +
  // WAITING and exclude both. UNKNOWN matches the LogApi.java failure
  // convention (FAILED || UNKNOWN both count as failures).
  { testId: "log-1", status: "FINISHED", result: "PASSED" },
  { testId: "log-2", status: "FINISHED", result: "PASSED" },
  { testId: "log-3", status: "FINISHED", result: "FAILED" },
  { testId: "log-4", status: "RUNNING", result: null },
  { testId: "log-5", status: "WAITING", result: null },
  { testId: "log-6", status: "INTERRUPTED", result: "FAILED" },
  { testId: "log-7", status: "CREATED", result: null },
  { testId: "log-8", status: "FINISHED", result: "UNKNOWN" },
];

// Envelope-shape fixture for the same content, used by the envelope story
// below so the production wire shape (`{ data, recordsTotal }`) is exercised.
const STATS_LOGS_SAMPLE_ENVELOPE = {
  data: STATS_LOGS_SAMPLE,
  recordsTotal: STATS_LOGS_SAMPLE.length,
};

// Default MSW handler set used by stories that don't focus on the stats row.
// Returns empty data for the list endpoints so the stats row renders with
// zero counts (and `pass` tone on the failures tile) without pulling in the
// larger sample fixtures.
function defaultStatsHandlers() {
  return [
    http.get("/api/server", () => HttpResponse.json(MOCK_SERVER_INFO)),
    http.get("/api/plan", () => HttpResponse.json([])),
    http.get("/api/log", () => HttpResponse.json([])),
  ];
}

export default {
  title: "Pages/cts-dashboard",
  component: "cts-dashboard",
};

// --- Stories ---

export const Authenticated = {
  parameters: {
    msw: {
      handlers: defaultStatsHandlers(),
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // All 6 tiles should be visible for authenticated users
    expect(canvas.getByText("Create a new test plan")).toBeInTheDocument();
    expect(canvas.getByText("View my test logs")).toBeInTheDocument();
    expect(canvas.getByText("View my test plans")).toBeInTheDocument();
    expect(canvas.getByText("View all published test logs")).toBeInTheDocument();
    expect(canvas.getByText("View all published test plans")).toBeInTheDocument();
    expect(canvas.getByText("View API Documentation")).toBeInTheDocument();

    // Verify hrefs on tile anchors
    const tiles = canvasElement.querySelectorAll("a.oidf-dashboard-tile");
    expect(tiles.length).toBe(6);

    const hrefs = Array.from(tiles).map((tile) => tile.getAttribute("href"));
    expect(hrefs).toContain("schedule-test.html");
    expect(hrefs).toContain("logs.html");
    expect(hrefs).toContain("plans.html");
    expect(hrefs).toContain("logs.html?public=true");
    expect(hrefs).toContain("plans.html?public=true");
    expect(hrefs).toContain("api-document.html");

    // Each tile renders a coolicons glyph via cts-icon.
    for (const tile of tiles) {
      expect(tile.querySelector(".oidf-dashboard-tile-icon cts-icon")).toBeTruthy();
      expect(tile.querySelector(".oidf-dashboard-tile-label")).toBeTruthy();
    }

    // The "View API Documentation" tile points at a separate app surface
    // (Swagger-UI), so it opens in a new tab and shows an external-link
    // affordance + screen-reader hint. Other tiles must NOT carry these.
    const apiDocsTile = Array.from(tiles).find(
      (t) => t.getAttribute("href") === "api-document.html",
    );
    expect(apiDocsTile).toBeTruthy();
    expect(apiDocsTile.getAttribute("target")).toBe("_blank");
    expect(apiDocsTile.getAttribute("rel")).toBe("noopener noreferrer");
    expect(apiDocsTile.querySelector(".oidf-dashboard-tile-external cts-icon")).toBeTruthy();
    expect(
      apiDocsTile.querySelector('.oidf-dashboard-tile-external cts-icon[name="external-link"]'),
    ).toBeTruthy();
    expect(apiDocsTile.querySelector(".oidf-sr-only")?.textContent).toContain("opens in a new tab");
    for (const tile of tiles) {
      if (tile === apiDocsTile) continue;
      expect(tile.getAttribute("target")).toBeNull();
      expect(tile.querySelector(".oidf-dashboard-tile-external")).toBeNull();
    }

    // Grid container present
    const grid = canvasElement.querySelector(".oidf-dashboard-grid");
    expect(grid).toBeTruthy();

    // Footer text present
    expect(canvas.getByText("OpenID Foundation conformance suite")).toBeInTheDocument();
    // Footer carries the .t-meta token class so type stays on the design-system scale
    const footer = canvasElement.querySelector("footer.oidf-dashboard-footer");
    expect(footer).toBeTruthy();
    expect(footer.classList.contains("t-meta")).toBe(true);
  },
};

export const ServerInfo = {
  parameters: {
    msw: {
      handlers: defaultStatsHandlers(),
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Wait for server info to appear after fetch
    await waitFor(
      () => {
        expect(canvas.getByText(/Version:/)).toBeInTheDocument();
      },
      { timeout: 3000 },
    );

    // Verify individual server info fields rendered
    const versionEl = canvasElement.querySelector("#serverinfo-version");
    expect(versionEl).toBeTruthy();
    expect(versionEl.textContent).toBe(MOCK_SERVER_INFO.version);

    const buildTimeEl = canvasElement.querySelector("#serverinfo-build_time");
    expect(buildTimeEl).toBeTruthy();
    expect(buildTimeEl.textContent).toBe(MOCK_SERVER_INFO.build_time);

    const revisionEl = canvasElement.querySelector("#serverinfo-revision");
    expect(revisionEl).toBeTruthy();
    expect(revisionEl.textContent).toBe(MOCK_SERVER_INFO.revision);
  },
};

export const Unauthenticated = {
  parameters: {
    msw: {
      // Stats endpoints are still registered defensively. The component
      // gates its stats fetch on isAuthenticated, so these handlers should
      // not be invoked — but if a future refactor changes that gating, an
      // absent handler would surface as a noisy MSW warning rather than a
      // clean assertion failure.
      handlers: defaultStatsHandlers(),
    },
  },
  render: () => html`<cts-dashboard .isAuthenticated=${false}></cts-dashboard>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Auth-only tiles should be hidden
    expect(canvas.queryByText("Create a new test plan")).toBeNull();
    expect(canvas.queryByText("View my test logs")).toBeNull();
    expect(canvas.queryByText("View my test plans")).toBeNull();

    // Public tiles should be visible
    expect(canvas.getByText("View all published test logs")).toBeInTheDocument();
    expect(canvas.getByText("View all published test plans")).toBeInTheDocument();
    expect(canvas.getByText("View API Documentation")).toBeInTheDocument();

    // Only 3 tile anchors rendered
    const tiles = canvasElement.querySelectorAll("a.oidf-dashboard-tile");
    expect(tiles.length).toBe(3);

    // Stats row is gated on isAuthenticated — public users must not see it.
    expect(canvasElement.querySelector("#dashboardStats")).toBeNull();

    // Footer still present
    expect(canvas.getByText("OpenID Foundation conformance suite")).toBeInTheDocument();
  },
};

export const ServerInfoError = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/server", () => new HttpResponse(null, { status: 500 })),
        http.get("/api/plan", () => HttpResponse.json([])),
        http.get("/api/log", () => HttpResponse.json([])),
      ],
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // The server-info failure is non-critical, but silently swallowing it was
    // the bug — we should see a console.warn so operators can diagnose.
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;

    try {
      // Tiles should still render despite server info error
      expect(canvas.getByText("Create a new test plan")).toBeInTheDocument();
      expect(canvas.getByText("View all published test logs")).toBeInTheDocument();
      expect(canvas.getByText("View API Documentation")).toBeInTheDocument();

      // All 6 tiles present (authenticated by default)
      const tiles = canvasElement.querySelectorAll("a.oidf-dashboard-tile");
      expect(tiles.length).toBe(6);

      // Footer text still present
      expect(canvas.getByText("OpenID Foundation conformance suite")).toBeInTheDocument();

      // Wait a tick for the fetch to complete, then verify no server info rendered
      await waitFor(
        () => {
          const serverInfoDiv = canvasElement.querySelector(".serverInfo");
          // The div exists but should have no version info inside
          expect(serverInfoDiv.querySelector("#serverinfo-version")).toBeNull();
        },
        { timeout: 3000 },
      );

      // The 500 should have produced a console.warn mentioning the endpoint.
      await waitFor(() => {
        expect(warnSpy).toHaveBeenCalled();
        const joined = warnSpy.mock.calls.flat().join(" ");
        expect(joined).toContain("cts-dashboard");
        expect(joined).toContain("/api/server");
      });
    } finally {
      console.warn = origWarn;
    }
  },
};

export const Loading = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/server", async () => {
          // Simulate a very long delay — server info never arrives
          await new Promise(() => {});
          return HttpResponse.json(MOCK_SERVER_INFO);
        }),
        http.get("/api/plan", async () => {
          // Mirror the server delay so the stats row stays in its em-dash
          // placeholder state for the duration of this story.
          await new Promise(() => {});
          return HttpResponse.json([]);
        }),
        http.get("/api/log", async () => {
          await new Promise(() => {});
          return HttpResponse.json([]);
        }),
      ],
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Tiles render immediately, regardless of server info loading
    expect(canvas.getByText("Create a new test plan")).toBeInTheDocument();
    expect(canvas.getByText("View my test logs")).toBeInTheDocument();
    expect(canvas.getByText("View my test plans")).toBeInTheDocument();
    expect(canvas.getByText("View all published test logs")).toBeInTheDocument();
    expect(canvas.getByText("View all published test plans")).toBeInTheDocument();
    expect(canvas.getByText("View API Documentation")).toBeInTheDocument();

    // Footer text is present
    expect(canvas.getByText("OpenID Foundation conformance suite")).toBeInTheDocument();

    // Stats row renders with em-dash placeholders while the fetches hang.
    // This proves R6 (no layout shift) at the story level: the row exists
    // before the data lands, just with placeholder values.
    const stats = canvasElement.querySelector("#dashboardStats");
    expect(stats).toBeTruthy();
    const tiles = stats.querySelectorAll(".oidf-dashboard-stat-tile");
    expect(tiles.length).toBe(4);
    for (const tile of tiles) {
      expect(tile.querySelector(".oidf-stat-value")?.textContent.trim()).toBe("—");
    }

    // Server info has not loaded yet — no version info displayed
    const versionEl = canvasElement.querySelector("#serverinfo-version");
    expect(versionEl).toBeNull();
  },
};

export const AuthenticatedWithStats = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/server", () => HttpResponse.json(MOCK_SERVER_INFO)),
        http.get("/api/plan", () => HttpResponse.json(STATS_PLANS_SAMPLE)),
        http.get("/api/log", () => HttpResponse.json(STATS_LOGS_SAMPLE)),
      ],
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // R6: no console errors on the happy path. Spy before the fetches
    // resolve so any Lit warnings or unhandled rejections during render
    // are captured.
    const errorSpy = fn();
    const origError = console.error;
    console.error = errorSpy;

    try {
      // The stats row mounts immediately with em-dash placeholders, then
      // re-renders once the parallel fetches resolve. Wait for the real
      // values to land — the first tile flipping off "—" is sufficient
      // because Lit batches the _stats reactive-property update.
      const stats = await waitFor(() => {
        const row = canvasElement.querySelector("#dashboardStats");
        expect(row).toBeTruthy();
        const firstValue = row
          .querySelector('[data-stat-key="plans"] .oidf-stat-value')
          ?.textContent.trim();
        expect(firstValue).not.toBe("—");
        return row;
      });

      // Plans tile: 3 sample entries → "3".
      expect(
        stats.querySelector('[data-stat-key="plans"] .oidf-stat-value').textContent.trim(),
      ).toBe("3");
      // Logs tile: 8 sample entries → "8".
      expect(
        stats.querySelector('[data-stat-key="logs"] .oidf-stat-value').textContent.trim(),
      ).toBe("8");
      // In-progress tile: only RUNNING + WAITING count. The fixture has
      // log-4 (RUNNING) and log-5 (WAITING). INTERRUPTED and CREATED in
      // the fixture must NOT be counted — that's the C1 correctness fix.
      expect(
        stats.querySelector('[data-stat-key="in-progress"] .oidf-stat-value').textContent.trim(),
      ).toBe("2");
      // Failures tile: FAILED + UNKNOWN count. The fixture has log-3
      // (FAILED), log-6 (FAILED), and log-8 (UNKNOWN) — three. Matches
      // LogApi.java's existing "failed" convention (C2 correctness fix).
      const failedTile = stats.querySelector('[data-stat-key="failed"]');
      const failedValue = failedTile.querySelector(".oidf-stat-value");
      expect(failedValue.textContent.trim()).toBe("3");
      // Attribute-level contract check: tone is forwarded. The visual
      // colour is asserted in cts-stat's own stories so retuning the
      // --status-fail token doesn't break dashboard tests.
      expect(failedTile.querySelector("cts-stat").getAttribute("tone")).toBe("fail");

      // Navigation grid still renders all six authenticated tiles below the
      // stats row — the stats row is additive, not a replacement.
      const navTiles = canvasElement.querySelectorAll("a.oidf-dashboard-tile");
      expect(navTiles.length).toBe(6);

      // Server info renders too — the parallel fetch resolved alongside.
      await waitFor(() => {
        expect(canvas.getByText(/Version:/)).toBeInTheDocument();
      });

      // R6: no console.error fired during the full mount + fetch + render
      // cycle. Lit dev-mode warnings on this build use console.warn (not
      // error), so console.error stays clean unless an unhandled rejection
      // or render exception slipped through.
      expect(errorSpy).not.toHaveBeenCalled();
    } finally {
      console.error = origError;
    }
  },
};

export const AuthenticatedWithStatsEnvelopeShape = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/server", () => HttpResponse.json(MOCK_SERVER_INFO)),
        // PaginationResponse envelope — the real production wire shape.
        // The component's _fetchListEndpoint accepts both this and the
        // plain-array form; this story exercises the envelope branch.
        http.get("/api/plan", () =>
          HttpResponse.json({ data: STATS_PLANS_SAMPLE, recordsTotal: STATS_PLANS_SAMPLE.length }),
        ),
        http.get("/api/log", () => HttpResponse.json(STATS_LOGS_SAMPLE_ENVELOPE)),
      ],
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    await waitFor(() => {
      const tile = canvasElement.querySelector('[data-stat-key="plans"] .oidf-stat-value');
      expect(tile?.textContent.trim()).toBe("3");
    });
    // Envelope and plain-array shapes must produce identical counts when
    // the data is the same — proves the dual-shape normalization in
    // _fetchListEndpoint preserves semantics across the wire format.
    expect(
      canvasElement.querySelector('[data-stat-key="logs"] .oidf-stat-value').textContent.trim(),
    ).toBe("8");
    expect(
      canvasElement
        .querySelector('[data-stat-key="in-progress"] .oidf-stat-value')
        .textContent.trim(),
    ).toBe("2");
    expect(
      canvasElement.querySelector('[data-stat-key="failed"] .oidf-stat-value').textContent.trim(),
    ).toBe("3");

    // Each stat tile is a clickable anchor wrapping the cts-stat. The "in
    // progress" and "with failures" tiles route through ?status= / ?result=
    // URL params on logs.html so users can drill from the summary count into
    // the matching rows.
    const tileExpectations = [
      { key: "plans", href: "plans.html", labelStart: "Your test plans" },
      { key: "logs", href: "logs.html", labelStart: "Your test logs" },
      {
        key: "in-progress",
        href: "logs.html?status=running,waiting",
        labelStart: "Logs in progress",
      },
      { key: "failed", href: "logs.html?result=failed,unknown", labelStart: "Logs with failures" },
    ];
    for (const { key, href, labelStart } of tileExpectations) {
      const tile = canvasElement.querySelector(
        `a.oidf-dashboard-stat-tile[data-stat-key="${key}"]`,
      );
      expect(tile).toBeTruthy();
      expect(tile.tagName).toBe("A");
      expect(tile.getAttribute("href")).toBe(href);
      const aria = tile.getAttribute("aria-label") || "";
      expect(aria.startsWith(labelStart)).toBe(true);
      // aria-label must carry the numeric value, not just "loading", once
      // the fetches have resolved. The waitFor() above guarantees that.
      expect(/[0-9]+/.test(aria)).toBe(true);
    }
  },
};

export const AuthenticatedWithStatsOverflow = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/server", () => HttpResponse.json(MOCK_SERVER_INFO)),
        // Plain array exactly at the page-size cap. The component must
        // render "1000+" because a full page is itself a truncation signal
        // (we cannot tell if row 1001 exists without another fetch).
        // STATS_PAGE_SIZE in the component is 1000; building the fixture
        // here keeps the story self-contained.
        http.get("/api/plan", () =>
          HttpResponse.json(Array.from({ length: 1000 }, (_, i) => ({ _id: `plan-${i}` }))),
        ),
        http.get("/api/log", () =>
          // Envelope variant of overflow: data.length < recordsTotal.
          HttpResponse.json({
            data: Array.from({ length: 5 }, (_, i) => ({
              testId: `log-${i}`,
              status: "FINISHED",
              result: "PASSED",
            })),
            recordsTotal: 4200,
          }),
        ),
      ],
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    await waitFor(() => {
      const tile = canvasElement.querySelector('[data-stat-key="plans"] .oidf-stat-value');
      // Plain-array-at-cap path: count >= STATS_PAGE_SIZE forces the "+".
      expect(tile?.textContent.trim()).toBe("1000+");
    });
    // Envelope overflow path: recordsTotal > data.length forces the "+".
    expect(
      canvasElement.querySelector('[data-stat-key="logs"] .oidf-stat-value').textContent.trim(),
    ).toBe("5+");
  },
};

export const AuthenticatedWithStatsAllPassing = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/server", () => HttpResponse.json(MOCK_SERVER_INFO)),
        http.get("/api/plan", () => HttpResponse.json(STATS_PLANS_SAMPLE)),
        // All five logs FINISHED + PASSED. Zero failures means the failures
        // tile should carry the `pass` tone (calm green, not rust).
        http.get("/api/log", () =>
          HttpResponse.json([
            { testId: "log-1", status: "FINISHED", result: "PASSED" },
            { testId: "log-2", status: "FINISHED", result: "PASSED" },
            { testId: "log-3", status: "FINISHED", result: "PASSED" },
          ]),
        ),
      ],
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    await waitFor(() => {
      const tile = canvasElement.querySelector('[data-stat-key="failed"] .oidf-stat-value');
      expect(tile?.textContent.trim()).toBe("0");
    });
    // Attribute-level tone contract. The actual --status-pass colour is
    // asserted in cts-stat's own stories — testing the rendered RGB here
    // would couple this story to design-token retunes that don't affect
    // dashboard behaviour.
    const failedTile = canvasElement.querySelector('[data-stat-key="failed"]');
    expect(failedTile.querySelector("cts-stat").getAttribute("tone")).toBe("pass");
  },
};

export const AuthenticatedWithStatsEndpointError = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/server", () => HttpResponse.json(MOCK_SERVER_INFO)),
        // Both list endpoints 500 — every tile should degrade to "—"
        // independently and the nav grid below should still render.
        http.get("/api/plan", () => new HttpResponse(null, { status: 500 })),
        http.get("/api/log", () => new HttpResponse(null, { status: 500 })),
      ],
    },
  },
  render: () => html`<cts-dashboard></cts-dashboard>`,

  async play({ canvasElement }) {
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;

    try {
      // After the fetches reject, the tiles stay at the em-dash placeholder.
      // The em-dash was the placeholder before the fetch too, so we wait
      // for BOTH endpoint warns to fire before asserting the final state.
      // Both endpoints failing must each produce a [cts-dashboard] warn —
      // a regression where one branch swallows the warn (or where Promise.all
      // short-circuits) would otherwise pass with a single warn.
      await waitFor(() => {
        const joined = warnSpy.mock.calls.flat().join(" ");
        expect(joined).toContain("/api/plan");
        expect(joined).toContain("/api/log");
        expect(joined).toContain("cts-dashboard");
      });

      const tiles = canvasElement.querySelectorAll("#dashboardStats .oidf-dashboard-stat-tile");
      expect(tiles.length).toBe(4);
      for (const tile of tiles) {
        expect(tile.querySelector(".oidf-stat-value")?.textContent.trim()).toBe("—");
      }

      // Navigation tiles below still render — the failure was contained.
      const navTiles = canvasElement.querySelectorAll("a.oidf-dashboard-tile");
      expect(navTiles.length).toBe(6);
    } finally {
      console.warn = origWarn;
    }
  },
};
