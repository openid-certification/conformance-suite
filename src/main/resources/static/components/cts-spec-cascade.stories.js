import { html } from "lit";
import { expect, within, waitFor, userEvent, fn } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_PLANS } from "@fixtures/mock-plans.js";
import "./cts-spec-cascade.js";

export default {
  title: "Components/cts-spec-cascade",
  component: "cts-spec-cascade",
};

// --- Stories ---

/** Only the Specification dropdown is visible on initial load. */
export const InitialState = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS)),
      ],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });
    // Entity, Version, and Plan rows should be hidden
    const entityRow = canvasElement.querySelector("#entitySelect")?.closest(".mb-3.row");
    expect(entityRow?.style.display).toBe("none");
    const versionRow = canvasElement.querySelector("#specVersionSelect")?.closest(".mb-3.row");
    expect(versionRow?.style.display).toBe("none");
    const planRow = canvasElement.querySelector("#planSelect")?.closest(".mb-3.row");
    expect(planRow?.style.display).toBe("none");
  },
};

/** Selecting a family reveals the Entity dropdown when multiple entities exist. */
export const SelectFamily = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS)),
      ],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });
    const familySelect = canvas.getByLabelText("Specification");
    await userEvent.selectOptions(familySelect, "OIDCC");
    await waitFor(() => {
      const entitySelect = canvasElement.querySelector("#entitySelect");
      expect(entitySelect).toBeTruthy();
      // OIDCC has OP and RP entities, so the entity row should be visible
      const entityRow = entitySelect.closest(".mb-3.row");
      expect(entityRow.style.display).not.toBe("none");
      // Should have OP and RP options plus the placeholder
      const options = entitySelect.querySelectorAll("option");
      expect(options.length).toBe(3); // placeholder + OP + RP
    });
  },
};

/** Full cascade: family -> entity -> version -> plan. Each step reveals the next. */
export const FullCascade = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS)),
      ],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });

    // Step 1: Select family
    const familySelect = canvas.getByLabelText("Specification");
    await userEvent.selectOptions(familySelect, "OIDCC");

    // Step 2: Entity row becomes visible, select OP
    await waitFor(() => {
      const entityRow = canvasElement.querySelector("#entitySelect").closest(".mb-3.row");
      expect(entityRow.style.display).not.toBe("none");
    });
    const entitySelect = canvasElement.querySelector("#entitySelect");
    await userEvent.selectOptions(entitySelect, "OP");

    // Step 3: OIDCC + OP has only one version (Final), so version auto-selects
    // and plan row should appear
    await waitFor(() => {
      const planRow = canvasElement.querySelector("#planSelect").closest(".mb-3.row");
      expect(planRow.style.display).not.toBe("none");
    });

    // Step 4: Select a plan
    const planSelect = canvasElement.querySelector("#planSelect");
    const planOptions = planSelect.querySelectorAll("option");
    // Should have placeholder + the available OIDCC OP Final plans
    expect(planOptions.length).toBeGreaterThan(1);
    await userEvent.selectOptions(planSelect, "oidcc-basic-certification-test-plan");
    expect(planSelect.value).toBe("oidcc-basic-certification-test-plan");
  },
};

/** Verify that cts-plan-selected event fires when a plan is chosen. */
export const PlanSelectedEvent = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS)),
      ],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    const spy = fn();
    canvasElement.addEventListener("cts-plan-selected", spy);

    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });

    // Navigate to a plan
    const familySelect = canvas.getByLabelText("Specification");
    await userEvent.selectOptions(familySelect, "OIDCC");

    await waitFor(() => {
      const entityRow = canvasElement.querySelector("#entitySelect").closest(".mb-3.row");
      expect(entityRow.style.display).not.toBe("none");
    });

    const entitySelect = canvasElement.querySelector("#entitySelect");
    await userEvent.selectOptions(entitySelect, "OP");

    await waitFor(() => {
      const planRow = canvasElement.querySelector("#planSelect").closest(".mb-3.row");
      expect(planRow.style.display).not.toBe("none");
    });

    const planSelect = canvasElement.querySelector("#planSelect");
    await userEvent.selectOptions(planSelect, "oidcc-basic-certification-test-plan");

    await waitFor(() => {
      expect(spy).toHaveBeenCalled();
      const eventDetail = spy.mock.calls[0][0].detail;
      expect(eventDetail.plan.planName).toBe("oidcc-basic-certification-test-plan");
    });
  },
};

/** Changing the family resets all downstream selections. */
export const ResetOnFamilyChange = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS)),
      ],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });

    // First, navigate through OIDCC cascade
    const familySelect = canvas.getByLabelText("Specification");
    await userEvent.selectOptions(familySelect, "OIDCC");

    await waitFor(() => {
      const entityRow = canvasElement.querySelector("#entitySelect").closest(".mb-3.row");
      expect(entityRow.style.display).not.toBe("none");
    });

    const entitySelect = canvasElement.querySelector("#entitySelect");
    await userEvent.selectOptions(entitySelect, "OP");

    await waitFor(() => {
      const planRow = canvasElement.querySelector("#planSelect").closest(".mb-3.row");
      expect(planRow.style.display).not.toBe("none");
    });

    // Now change family to FAPI
    await userEvent.selectOptions(familySelect, "FAPI");

    // Downstream should reset: FAPI has only one entity (OP), so entity auto-selects
    // The previously selected OIDCC plan should no longer be the value
    await waitFor(() => {
      const planSelect = canvasElement.querySelector("#planSelect");
      expect(planSelect.value).not.toBe("oidcc-basic-certification-test-plan");
    });
  },
};

/** A family with only one entity auto-selects that entity (row stays hidden). */
export const SingleEntity = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS)),
      ],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });

    // FAPI has only one entity (OP), so entity row should remain hidden
    const familySelect = canvas.getByLabelText("Specification");
    await userEvent.selectOptions(familySelect, "FAPI");

    await waitFor(() => {
      const entitySelect = canvasElement.querySelector("#entitySelect");
      // Entity auto-selected to OP
      expect(entitySelect.value).toBe("OP");
      // Entity row should be hidden since there is only one entity
      const entityRow = entitySelect.closest(".mb-3.row");
      expect(entityRow.style.display).toBe("none");
    });
  },
};

/** Pass plans as a prop instead of fetching. No MSW needed. */
export const WithProvidedPlans = {
  render: () => html`<cts-spec-cascade .plans=${MOCK_PLANS}></cts-spec-cascade>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    // Plans are provided synchronously, so Specification should be immediately available
    const familySelect = canvas.getByLabelText("Specification");
    expect(familySelect).toBeInTheDocument();

    // Should have the correct family options
    const options = familySelect.querySelectorAll("option");
    // Placeholder + FAPI + FAPI-CIBA + OIDCC + SSF = 5
    expect(options.length).toBe(5);

    // Select a family and verify cascade works
    await userEvent.selectOptions(familySelect, "SSF");

    // SSF has only one entity (Transmitter), one version (Draft), and one plan
    // so everything should auto-select
    await waitFor(() => {
      const planSelect = canvasElement.querySelector("#planSelect");
      expect(planSelect.value).toBe("ssf-transmitter-test-plan");
    });
  },
};

/** While loading from the API, a spinner is shown. */
export const LoadingState = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/runner/available", async () => {
          await new Promise((resolve) => setTimeout(resolve, 60000));
          return HttpResponse.json(MOCK_PLANS);
        }),
      ],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement }) {
    // The spinner should be visible while the request is in flight
    await waitFor(() => {
      const spinner = canvasElement.querySelector(".spinner-border");
      expect(spinner).toBeTruthy();
    });

    const srText = canvasElement.querySelector(".visually-hidden");
    expect(srText.textContent).toBe("Loading available test plans...");
  },
};
