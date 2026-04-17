import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_PLAN_LIST } from "@fixtures/mock-plans.js";
import { authHandlers, serverHandlers } from "@fixtures/msw-handlers.js";
import "./cts-home-plan-grid.js";

/**
 * Exploration prototype for the home page.
 * Re-grounded against the real /api/plan PaginationResponse envelope.
 * Target-today surface: components/cts-dashboard.js.
 * Workshop UX references: UX-04 (primary); UX-01, UX-05 (incidentally).
 */

const DAY_MS = 86400000;

function paginationEnvelope(data) {
  return {
    draw: 1,
    recordsTotal: data.length,
    recordsFiltered: data.length,
    data,
  };
}

function planHandlerReturning(data) {
  return http.get("/api/plan", () => HttpResponse.json(paginationEnvelope(data)));
}

async function waitForPlansHydrated(canvasElement) {
  await waitFor(
    () => {
      const loading = canvasElement.querySelector(".cts-hpg-loading");
      expect(loading).toBeNull();
    },
    { timeout: 3000 },
  );
}

export default {
  title: "Exploration/Home Plan Grid",
  component: "cts-home-plan-grid",
};

// ---------------------------------------------------------------------------
// Happy path — three plans, full home-page rendering.
// ---------------------------------------------------------------------------
export const Active = {
  parameters: {
    msw: {
      handlers: [
        planHandlerReturning(MOCK_PLAN_LIST),
        ...authHandlers,
        ...serverHandlers,
      ],
    },
  },
  render: () => html`<cts-home-plan-grid></cts-home-plan-grid>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansHydrated(canvasElement);

    expect(canvas.getByText("Your test plans")).toBeInTheDocument();

    const cards = canvasElement.querySelectorAll(".cts-hpg-card");
    expect(cards.length).toBe(3);

    // Six existing home-page links preserved verbatim.
    expect(canvas.getByText("Create a new test plan")).toBeInTheDocument();
    expect(canvas.getByText("View my test logs")).toBeInTheDocument();
    expect(canvas.getByText("View my test plans")).toBeInTheDocument();
    expect(canvas.getByText("View all published test logs")).toBeInTheDocument();
    expect(canvas.getByText("View all published test plans")).toBeInTheDocument();
    expect(canvas.getByText("View API Documentation")).toBeInTheDocument();

    // Server info footer shows the MOCK_SERVER_INFO.version.
    await waitFor(
      () => {
        const serverInfo = canvasElement.querySelector(".serverInfo");
        expect(serverInfo).toBeTruthy();
        expect(serverInfo.textContent).toContain("5.1.24-SNAPSHOT");
      },
      { timeout: 3000 },
    );

    // plan-002 has 1 PASSED + 1 FAILED (0 warning, 0 not-run).
    const plan002 = canvasElement.querySelector('a[href="plan-detail.html?plan=plan-002"]');
    expect(plan002).toBeTruthy();
    const plan002Counts = plan002.querySelector(".cts-hpg-card-counts");
    expect(plan002Counts.textContent.trim()).toBe("1 passed \u00B7 1 failed");

    // Segmented bar has exactly 2 non-zero segments.
    const segments = plan002.querySelectorAll(".cts-hpg-bar > span");
    const nonZeroSegments = Array.from(segments).filter(
      (el) => parseFloat(el.style.width) > 0,
    );
    expect(nonZeroSegments.length).toBe(2);
  },
};

// ---------------------------------------------------------------------------
// Empty — no plans at all.
// ---------------------------------------------------------------------------
export const Empty = {
  parameters: {
    msw: {
      handlers: [
        planHandlerReturning([]),
        ...authHandlers,
        ...serverHandlers,
      ],
    },
  },
  render: () => html`<cts-home-plan-grid></cts-home-plan-grid>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansHydrated(canvasElement);

    expect(canvas.getByText("No test plans yet.")).toBeInTheDocument();
    expect(
      canvas.getByText("Create one to begin conformance testing."),
    ).toBeInTheDocument();

    const cards = canvasElement.querySelectorAll(".cts-hpg-card");
    expect(cards.length).toBe(0);

    // Six-link column still visible in the empty state.
    expect(canvas.getByText("Create a new test plan")).toBeInTheDocument();
    expect(canvas.getByText("View API Documentation")).toBeInTheDocument();
  },
};

// ---------------------------------------------------------------------------
// Mixed — one plan whose modules are all not-started (null result).
// ---------------------------------------------------------------------------
const ALL_NOT_STARTED_PLAN = {
  _id: "plan-all-null",
  planName: "vci10-issuer-happy-flow-test-plan",
  description: "VCI 1.0 Issuer happy-flow",
  variant: { credential_format: "sdjwt" },
  started: new Date(Date.now() - DAY_MS).toISOString(),
  owner: { sub: "12345", iss: "https://accounts.google.com" },
  modules: [
    { testModule: "vci10-issuer-metadata", instances: [], status: null, result: null },
    { testModule: "vci10-issuer-discovery", instances: [], status: null, result: null },
  ],
  config: {},
  publish: null,
  immutable: false,
};

export const Mixed = {
  parameters: {
    msw: {
      handlers: [
        planHandlerReturning([...MOCK_PLAN_LIST, ALL_NOT_STARTED_PLAN]),
        ...authHandlers,
        ...serverHandlers,
      ],
    },
  },
  render: () => html`<cts-home-plan-grid></cts-home-plan-grid>`,
  async play({ canvasElement }) {
    await waitForPlansHydrated(canvasElement);

    const allNull = canvasElement.querySelector('a[href="plan-detail.html?plan=plan-all-null"]');
    expect(allNull).toBeTruthy();

    const counts = allNull.querySelector(".cts-hpg-card-counts");
    expect(counts.textContent.trim()).toBe("2 not started");

    // The not-run segment should be the only non-zero one, at 100%.
    const segments = allNull.querySelectorAll(".cts-hpg-bar > span");
    const notrunSegment = allNull.querySelector(".cts-hpg-bar-notrun");
    expect(notrunSegment.style.width).toBe("100%");

    const nonZero = Array.from(segments).filter(
      (el) => parseFloat(el.style.width) > 0,
    );
    expect(nonZero.length).toBe(1);
  },
};

// ---------------------------------------------------------------------------
// Responsive narrow viewport — 375×800, cards stack single column.
// ---------------------------------------------------------------------------
export const ResponsiveNarrow = {
  parameters: {
    msw: {
      handlers: [
        planHandlerReturning(MOCK_PLAN_LIST),
        ...authHandlers,
        ...serverHandlers,
      ],
    },
  },
  render: () => html`
    <div style="width: 375px; border: 1px dashed #ccc;">
      <cts-home-plan-grid></cts-home-plan-grid>
    </div>
  `,
  async play({ canvasElement }) {
    await waitForPlansHydrated(canvasElement);

    const grid = canvasElement.querySelector(".cts-hpg-grid");
    expect(grid).toBeTruthy();

    // Below 600px the grid collapses to a single column.
    const gridColumns = getComputedStyle(grid).gridTemplateColumns;
    const columnCount = gridColumns.trim().split(/\s+/).length;
    expect(columnCount).toBe(1);

    // Three cards still render; each takes the full grid width.
    const cards = canvasElement.querySelectorAll(".cts-hpg-card");
    expect(cards.length).toBe(3);
  },
};

// ---------------------------------------------------------------------------
// Integration check — verifies the component reads response.data from the
// PaginationResponse envelope rather than treating the response as an array.
// A non-envelope response would leave plans undefined and never reach empty
// state; seeing the empty state proves .data was read correctly.
// ---------------------------------------------------------------------------
export const IntegrationEnvelope = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () =>
          HttpResponse.json({
            draw: 1,
            recordsTotal: 0,
            recordsFiltered: 0,
            data: [],
          }),
        ),
        ...authHandlers,
        ...serverHandlers,
      ],
    },
  },
  render: () => html`<cts-home-plan-grid></cts-home-plan-grid>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansHydrated(canvasElement);

    // Envelope's data field is empty → empty state renders.
    expect(canvas.getByText("No test plans yet.")).toBeInTheDocument();
    const cards = canvasElement.querySelectorAll(".cts-hpg-card");
    expect(cards.length).toBe(0);
  },
};
