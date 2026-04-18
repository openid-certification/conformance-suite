import { html } from "lit";
import { expect, within, waitFor, userEvent, fn } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_PLAN_LIST } from "@fixtures/mock-plans.js";
import "./cts-plan-list.js";

export default {
  title: "Pages/cts-plan-list",
  component: "cts-plan-list",
};

// --- Helpers ---

async function waitForPlansToLoad(canvasElement) {
  await waitFor(
    () => {
      const spinner = canvasElement.querySelector(".spinner-border");
      expect(spinner).toBeNull();
    },
    { timeout: 3000 },
  );
}

// --- Stories ---

export const Default = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST))],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    // Table renders with correct column headers
    expect(canvas.getByText("Plan Name")).toBeInTheDocument();
    expect(canvas.getByText("Variant")).toBeInTheDocument();
    expect(canvas.getByText("Description")).toBeInTheDocument();
    expect(canvas.getByText("Started")).toBeInTheDocument();
    expect(canvas.getByText("Modules")).toBeInTheDocument();
    expect(canvas.getByText("Config")).toBeInTheDocument();

    // Owner column should NOT be visible for non-admin
    expect(canvas.queryByText("Owner")).toBeNull();

    // Plan names render
    expect(canvas.getByText("oidcc-basic-certification-test-plan")).toBeInTheDocument();
    expect(canvas.getByText("fapi2-security-profile-final-test-plan")).toBeInTheDocument();
    expect(canvas.getByText("oidcc-implicit-certification-test-plan")).toBeInTheDocument();

    // Module badges render
    const badges = canvasElement.querySelectorAll("cts-badge");
    expect(badges.length).toBeGreaterThan(0);

    // Table has correct number of data rows
    const rows = canvasElement.querySelectorAll("tbody tr");
    expect(rows.length).toBe(MOCK_PLAN_LIST.length);
  },
};

export const Search = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST))],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    // All plans visible initially
    let rows = canvasElement.querySelectorAll("tbody tr");
    expect(rows.length).toBe(MOCK_PLAN_LIST.length);

    // Type a search query
    const searchInput = canvasElement.querySelector('input[placeholder="Search test plans..."]');
    expect(searchInput).toBeTruthy();
    await userEvent.type(searchInput, "fapi2");

    // Table filters to matching plans
    await waitFor(() => {
      rows = canvasElement.querySelectorAll("tbody tr");
      expect(rows.length).toBe(1);
    });
    expect(canvas.getByText("fapi2-security-profile-final-test-plan")).toBeInTheDocument();
    expect(canvas.queryByText("oidcc-basic-certification-test-plan")).toBeNull();
  },
};

export const ClickPlanName = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST))],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    // Listen for the navigate event
    let receivedPlanId = null;
    canvasElement.addEventListener("cts-plan-navigate", (e) => {
      receivedPlanId = e.detail.planId;
    });

    // Click the first plan name link
    const planLink = canvas.getByText("oidcc-basic-certification-test-plan");
    await userEvent.click(planLink);

    // Event should fire with the correct plan ID
    expect(receivedPlanId).toBe("plan-001");
  },
};

export const ViewConfig = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST))],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    await waitForPlansToLoad(canvasElement);

    // Click the first Config button
    const configBtn = canvasElement.querySelector(".showConfigBtn");
    expect(configBtn).toBeTruthy();
    await userEvent.click(configBtn);

    // Modal should open showing config JSON
    await waitFor(() => {
      const configJson = canvasElement.querySelector(".config-json");
      expect(configJson).toBeTruthy();
      expect(configJson.textContent).toContain("server.issuer");
      expect(configJson.textContent).toContain("https://op.example.com");
    });

    // Plan ID shown in the modal
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("plan-001")).toBeInTheDocument();
    });

    // Mock clipboard and test copy button
    const clipboardSpy = fn();
    const originalClipboard = navigator.clipboard;
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: clipboardSpy },
      writable: true,
      configurable: true,
    });

    const copyBtn = canvasElement.querySelector(".copy-config-btn");
    expect(copyBtn).toBeTruthy();
    await userEvent.click(copyBtn);

    await waitFor(() => {
      expect(clipboardSpy).toHaveBeenCalled();
    });

    // Restore clipboard
    Object.defineProperty(navigator, "clipboard", {
      value: originalClipboard,
      writable: true,
      configurable: true,
    });
  },
};

export const EmptyList = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan", () => HttpResponse.json([]))],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    // Should show empty message
    expect(canvas.getByText("No test plans found")).toBeInTheDocument();

    // No table rendered
    const table = canvasElement.querySelector("table");
    expect(table).toBeNull();
  },
};

export const LoadingState = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", async () => {
          await new Promise((resolve) => setTimeout(resolve, 60000));
          return HttpResponse.json(MOCK_PLAN_LIST);
        }),
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Spinner should be visible
    await waitFor(() => {
      const spinner = canvasElement.querySelector(".spinner-border");
      expect(spinner).toBeTruthy();
    });
    expect(canvas.getByText("Loading test plans...")).toBeInTheDocument();

    // No table rendered while loading
    const table = canvasElement.querySelector("table");
    expect(table).toBeNull();
  },
};

export const ApiError = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan", () => new HttpResponse(null, { status: 500 }))],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    // Error alert should be displayed
    const alert = canvasElement.querySelector(".alert-danger");
    expect(alert).toBeTruthy();
    expect(canvas.getByText(/Failed to load test plans/)).toBeInTheDocument();

    // No table rendered
    const table = canvasElement.querySelector("table");
    expect(table).toBeNull();
  },
};

export const AdminView = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST))],
    },
  },
  render: () => html`<cts-plan-list is-admin></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    // Owner column header should be visible
    expect(canvas.getByText("Owner")).toBeInTheDocument();

    // Owner cells should be rendered
    const ownerCells = canvasElement.querySelectorAll(".owner-cell");
    expect(ownerCells.length).toBe(MOCK_PLAN_LIST.length);

    // First plan's owner sub should be visible
    expect(ownerCells[0].textContent).toBe("12345");

    // All other columns still present
    expect(canvas.getByText("Plan Name")).toBeInTheDocument();
    expect(canvas.getByText("oidcc-basic-certification-test-plan")).toBeInTheDocument();
  },
};

export const PublicView = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", ({ request }) => {
          const url = new URL(request.url);
          const isPublic = url.searchParams.get("public") === "true";
          const plans = isPublic ? MOCK_PLAN_LIST.filter((p) => p.publish) : MOCK_PLAN_LIST;
          return HttpResponse.json(plans);
        }),
      ],
    },
  },
  render: () => html`<cts-plan-list is-public></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    // Only published plans should be shown (plan-002 and plan-003 have publish set)
    const rows = canvasElement.querySelectorAll("tbody tr");
    expect(rows.length).toBe(2);

    expect(canvas.getByText("fapi2-security-profile-final-test-plan")).toBeInTheDocument();
    expect(canvas.getByText("oidcc-implicit-certification-test-plan")).toBeInTheDocument();
    expect(canvas.queryByText("oidcc-basic-certification-test-plan")).toBeNull();

    // Owner column should NOT be visible in public view
    expect(canvas.queryByText("Owner")).toBeNull();
  },
};
