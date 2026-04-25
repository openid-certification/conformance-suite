import { html } from "lit";
import { expect, within, waitFor, userEvent, fn } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_PLANS } from "@fixtures/mock-plans.js";
import "./cts-spec-cascade.js";

export default {
  title: "Components/cts-spec-cascade",
  component: "cts-spec-cascade",
};

// Helper: locate the wrapping field for a given <select id="...">. The
// component wraps each select in a `.oidf-spec-cascade__field` element with a
// `data-testid="<selectId>-field"` so tests can assert visibility regardless
// of the OIDF class names.
function fieldFor(canvasElement, selectId) {
  return canvasElement.querySelector(`[data-testid="${selectId}-field"]`);
}

// --- Stories ---

/** Only the Specification dropdown is visible on initial load. */
export const InitialState = {
  parameters: {
    msw: {
      handlers: [http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS))],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });
    // Entity, Version, and Plan fields should be hidden
    expect(fieldFor(canvasElement, "entitySelect")?.style.display).toBe("none");
    expect(fieldFor(canvasElement, "specVersionSelect")?.style.display).toBe("none");
    expect(fieldFor(canvasElement, "planSelect")?.style.display).toBe("none");
    // The Specification select should carry the OIDF class
    const familySelect = canvasElement.querySelector("#specFamilySelect");
    expect(familySelect?.classList.contains("oidf-spec-cascade__select")).toBe(true);
  },
};

/** Selecting a family reveals the Entity dropdown when multiple entities exist. */
export const SelectFamily = {
  parameters: {
    msw: {
      handlers: [http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS))],
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
      // OIDCC has OP and RP entities, so the entity field should be visible
      const entityField = fieldFor(canvasElement, "entitySelect");
      expect(entityField.style.display).not.toBe("none");
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
      handlers: [http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS))],
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

    // Step 2: Entity field becomes visible, select OP
    await waitFor(() => {
      const entityField = fieldFor(canvasElement, "entitySelect");
      expect(entityField.style.display).not.toBe("none");
    });
    const entitySelect = canvasElement.querySelector("#entitySelect");
    await userEvent.selectOptions(entitySelect, "OP");

    // Step 3: OIDCC + OP has only one version (Final), so version auto-selects
    // and plan field should appear
    await waitFor(() => {
      const planField = fieldFor(canvasElement, "planSelect");
      expect(planField.style.display).not.toBe("none");
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
      handlers: [http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS))],
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
      const entityField = fieldFor(canvasElement, "entitySelect");
      expect(entityField.style.display).not.toBe("none");
    });

    const entitySelect = canvasElement.querySelector("#entitySelect");
    await userEvent.selectOptions(entitySelect, "OP");

    await waitFor(() => {
      const planField = fieldFor(canvasElement, "planSelect");
      expect(planField.style.display).not.toBe("none");
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
      handlers: [http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS))],
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
      const entityField = fieldFor(canvasElement, "entitySelect");
      expect(entityField.style.display).not.toBe("none");
    });

    const entitySelect = canvasElement.querySelector("#entitySelect");
    await userEvent.selectOptions(entitySelect, "OP");

    await waitFor(() => {
      const planField = fieldFor(canvasElement, "planSelect");
      expect(planField.style.display).not.toBe("none");
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

/** A family with only one entity auto-selects that entity (field stays hidden). */
export const SingleEntity = {
  parameters: {
    msw: {
      handlers: [http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS))],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });

    // FAPI has only one entity (OP), so entity field should remain hidden
    const familySelect = canvas.getByLabelText("Specification");
    await userEvent.selectOptions(familySelect, "FAPI");

    await waitFor(() => {
      const entitySelect = canvasElement.querySelector("#entitySelect");
      // Entity auto-selected to OP
      expect(entitySelect.value).toBe("OP");
      // Entity field should be hidden since there is only one entity
      expect(fieldFor(canvasElement, "entitySelect").style.display).toBe("none");
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

/** While loading from the API, a status banner is shown. */
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
    // The loading banner should be visible while the request is in flight
    await waitFor(() => {
      const loading = canvasElement.querySelector(".oidf-spec-cascade__loading");
      expect(loading).toBeTruthy();
      expect(loading.textContent).toContain("Loading available test plans...");
    });
  },
};

/**
 * A 5xx from /api/runner/available used to return a JSON error payload that
 * the component then treated as "plans = [error object]", leaving the user
 * staring at an empty dropdown with no idea what happened. The component now
 * distinguishes load-failure from truly-empty.
 */
export const LoadErrorShowsBanner = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/runner/available", () =>
          HttpResponse.json({ error: "backend down" }, { status: 500 }),
        ),
      ],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement }) {
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;
    try {
      await waitFor(() => {
        const banner = canvasElement.querySelector('[data-testid="spec-cascade-error"]');
        expect(banner).toBeTruthy();
        expect(banner.textContent).toContain("Unable to load plans");
        // The error banner uses the OIDF alert tokens
        expect(banner.classList.contains("oidf-spec-cascade__alert--error")).toBe(true);
      });

      // No dropdowns should render in the error state.
      expect(canvasElement.querySelector("#specFamilySelect")).toBeNull();
      expect(canvasElement.querySelector('[data-testid="spec-cascade-empty"]')).toBeNull();

      await waitFor(() => {
        expect(warnSpy).toHaveBeenCalled();
        const joined = warnSpy.mock.calls.flat().join(" ");
        expect(joined).toContain("cts-spec-cascade");
      });
    } finally {
      console.warn = origWarn;
    }
  },
};

/**
 * The server returned an empty list — no plans for this deployment yet. This
 * is the distinct-from-error case: the backend is healthy, there's just
 * nothing to show. Route: empty response → info banner (not danger).
 */
export const LoadsEmptyShowsInfoBanner = {
  parameters: {
    msw: {
      handlers: [http.get("/api/runner/available", () => HttpResponse.json([]))],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const empty = canvasElement.querySelector('[data-testid="spec-cascade-empty"]');
      expect(empty).toBeTruthy();
      expect(empty.textContent).toContain("No test plans are available");
      expect(empty.classList.contains("oidf-spec-cascade__alert--info")).toBe(true);
    });
    // Error banner should NOT be showing — this is the healthy-but-empty case.
    expect(canvasElement.querySelector('[data-testid="spec-cascade-error"]')).toBeNull();
  },
};
