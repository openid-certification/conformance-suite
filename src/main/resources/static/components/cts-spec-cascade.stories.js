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
      handlers: [
        http.get("/api/plan/available", ({ request }) => {
          // Locks the contract: the cascade must fetch from /api/plan/available.
          // If a future change reverts this to /api/runner/available, the MSW
          // handler will not match and the story will fall through to the
          // empty-state branch instead of populating the cascade.
          if (!request.url.endsWith("/api/plan/available")) {
            return HttpResponse.json({ error: "wrong endpoint" }, { status: 404 });
          }
          return HttpResponse.json(MOCK_PLANS);
        }),
      ],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });
    await step("entity, version, and plan fields stay hidden", async () => {
      expect(fieldFor(canvasElement, "entitySelect")?.style.display).toBe("none");
      expect(fieldFor(canvasElement, "specVersionSelect")?.style.display).toBe("none");
      expect(fieldFor(canvasElement, "planSelect")?.style.display).toBe("none");
    });
    await step("specification select carries the OIDF class", async () => {
      const familySelect = canvasElement.querySelector("#specFamilySelect");
      expect(familySelect?.classList.contains("oidf-spec-cascade__select")).toBe(true);
    });
    await step("family dropdown populated from /api/plan/available", async () => {
      const familySelect = canvasElement.querySelector("#specFamilySelect");
      // Endpoint contract: the family dropdown only populates when the
      // MSW handler keyed to /api/plan/available matched. If we accidentally
      // ship a URL drift, this option count drops to 1 (placeholder only).
      expect(familySelect.querySelectorAll("option").length).toBeGreaterThan(1);
    });
  },
};

/** Selecting a family reveals the Entity dropdown when multiple entities exist. */
export const SelectFamily = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan/available", () => HttpResponse.json(MOCK_PLANS))],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });
    await step("selecting OIDCC reveals the entity dropdown", async () => {
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
    });
  },
};

/** Full cascade: family -> entity -> version -> plan. Each step reveals the next. */
export const FullCascade = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan/available", () => HttpResponse.json(MOCK_PLANS))],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });

    await step("select family", async () => {
      const familySelect = canvas.getByLabelText("Specification");
      await userEvent.selectOptions(familySelect, "OIDCC");
    });

    await step("entity field becomes visible, select OP", async () => {
      await waitFor(() => {
        const entityField = fieldFor(canvasElement, "entitySelect");
        expect(entityField.style.display).not.toBe("none");
      });
      const entitySelect = canvasElement.querySelector("#entitySelect");
      await userEvent.selectOptions(entitySelect, "OP");
    });

    await step("version auto-selects, plan field appears", async () => {
      // OIDCC + OP has only one version (Final), so version auto-selects
      // and plan field should appear
      await waitFor(() => {
        const planField = fieldFor(canvasElement, "planSelect");
        expect(planField.style.display).not.toBe("none");
      });
    });

    await step("select a plan", async () => {
      const planSelect = canvasElement.querySelector("#planSelect");
      const planOptions = planSelect.querySelectorAll("option");
      // Should have placeholder + the available OIDCC OP Final plans
      expect(planOptions.length).toBeGreaterThan(1);
      await userEvent.selectOptions(planSelect, "oidcc-basic-certification-test-plan");
      expect(planSelect.value).toBe("oidcc-basic-certification-test-plan");
    });
  },
};

/** Verify that cts-plan-selected event fires when a plan is chosen. */
export const PlanSelectedEvent = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan/available", () => HttpResponse.json(MOCK_PLANS))],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    const spy = fn();
    canvasElement.addEventListener("cts-plan-selected", spy);

    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });

    await step("navigate the cascade to a plan", async () => {
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
    });

    await step("cts-plan-selected fires with the chosen plan", async () => {
      await waitFor(() => {
        expect(spy).toHaveBeenCalled();
        const eventDetail = spy.mock.calls[0][0].detail;
        expect(eventDetail.plan.planName).toBe("oidcc-basic-certification-test-plan");
      });
    });
  },
};

/** Changing the family resets all downstream selections. */
export const ResetOnFamilyChange = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan/available", () => HttpResponse.json(MOCK_PLANS))],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });

    const familySelect = canvas.getByLabelText("Specification");

    await step("navigate through the OIDCC cascade to a plan", async () => {
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
    });

    await step("changing family to FAPI resets the downstream plan", async () => {
      await userEvent.selectOptions(familySelect, "FAPI");

      // Downstream should reset: FAPI has only one entity (OP), so entity auto-selects.
      // The previously selected OIDCC plan should no longer be the value.
      await waitFor(() => {
        const planSelect = canvasElement.querySelector("#planSelect");
        expect(planSelect.value).not.toBe("oidcc-basic-certification-test-plan");
      });
    });
  },
};

/** A family with only one entity auto-selects that entity (field stays hidden). */
export const SingleEntity = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan/available", () => HttpResponse.json(MOCK_PLANS))],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });

    await step("selecting FAPI auto-selects the sole entity and keeps it hidden", async () => {
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
    });
  },
};

/** Pass plans as a prop instead of fetching. No MSW needed. */
export const WithProvidedPlans = {
  render: () => html`<cts-spec-cascade .plans=${MOCK_PLANS}></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    // Plans are provided synchronously, so Specification should be immediately available
    const familySelect = canvas.getByLabelText("Specification");

    await step("specification renders with the correct family options", async () => {
      expect(familySelect).toBeInTheDocument();
      const options = familySelect.querySelectorAll("option");
      // Placeholder + FAPI + FAPI-CIBA + OIDCC + SSF = 5
      expect(options.length).toBe(5);
    });

    await step("selecting SSF auto-selects through to the single plan", async () => {
      await userEvent.selectOptions(familySelect, "SSF");

      // SSF has only one entity (Transmitter), one version (Draft), and one plan
      // so everything should auto-select
      await waitFor(() => {
        const planSelect = canvasElement.querySelector("#planSelect");
        expect(planSelect.value).toBe("ssf-transmitter-test-plan");
      });
    });
  },
};

/** While loading from the API, a status banner is shown. */
export const LoadingState = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan/available", async () => {
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
 * A 5xx from /api/plan/available used to return a JSON error payload that
 * the component then treated as "plans = [error object]", leaving the user
 * staring at an empty dropdown with no idea what happened. The component now
 * distinguishes load-failure from truly-empty.
 */
export const LoadErrorShowsBanner = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan/available", () =>
          HttpResponse.json({ error: "backend down" }, { status: 500 }),
        ),
      ],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;
    try {
      await step("error banner renders with the OIDF alert tokens", async () => {
        await waitFor(() => {
          const banner = canvasElement.querySelector('[data-testid="spec-cascade-error"]');
          expect(banner).toBeTruthy();
          expect(banner.textContent).toContain("Unable to load plans");
          // The error banner uses the OIDF alert tokens
          expect(banner.classList.contains("oidf-spec-cascade__alert--error")).toBe(true);
        });
      });

      await step("no dropdowns or empty banner render in the error state", async () => {
        expect(canvasElement.querySelector("#specFamilySelect")).toBeNull();
        expect(canvasElement.querySelector('[data-testid="spec-cascade-empty"]')).toBeNull();
      });

      await step("the failure is logged via console.warn", async () => {
        await waitFor(() => {
          expect(warnSpy).toHaveBeenCalled();
          const joined = warnSpy.mock.calls.flat().join(" ");
          expect(joined).toContain("cts-spec-cascade");
        });
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
      handlers: [http.get("/api/plan/available", () => HttpResponse.json([]))],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    await step("empty info banner renders for the healthy-but-empty case", async () => {
      await waitFor(() => {
        const empty = canvasElement.querySelector('[data-testid="spec-cascade-empty"]');
        expect(empty).toBeTruthy();
        expect(empty.textContent).toContain("No test plans are available");
        expect(empty.classList.contains("oidf-spec-cascade__alert--info")).toBe(true);
      });
    });
    await step("error banner is not shown", async () => {
      // Error banner should NOT be showing — this is the healthy-but-empty case.
      expect(canvasElement.querySelector('[data-testid="spec-cascade-error"]')).toBeNull();
    });
  },
};

/**
 * A 200 OK response with a non-array body is coerced to `[]` and routes to
 * the empty-state info banner. The `Array.isArray` guard in `_planIndex`
 * exists to prevent `for…of` from crashing on a malformed payload.
 */
export const NonArrayResponseShowsEmptyBanner = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan/available", () =>
          HttpResponse.json({ error: "shape drift" }, { status: 200 }),
        ),
      ],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    await step("non-array body routes to the empty info banner", async () => {
      await waitFor(() => {
        const empty = canvasElement.querySelector('[data-testid="spec-cascade-empty"]');
        expect(empty).toBeTruthy();
        expect(empty.classList.contains("oidf-spec-cascade__alert--info")).toBe(true);
      });
    });
    await step("neither the error banner nor the cascade selects render", async () => {
      // Not an error — the backend was healthy, the body just wasn't shaped
      // as expected, so the empty-state info banner is the correct route.
      expect(canvasElement.querySelector('[data-testid="spec-cascade-error"]')).toBeNull();
      // The cascade selects are absent in this state.
      expect(canvasElement.querySelector("#specFamilySelect")).toBeNull();
    });
  },
};

/**
 * Programmatic `selectPlanByName` drives the cascade to a named plan and
 * dispatches `cts-plan-selected`. Exercised by `schedule-test.html` to apply
 * a `?test_plan=...` URL param, the "Load last configuration" toolbar
 * button, and the edit-plan flow.
 */
export const ProgrammaticSelection = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan/available", () => HttpResponse.json(MOCK_PLANS))],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    const element = canvasElement.querySelector("cts-spec-cascade");
    const spy = fn();
    element.addEventListener("cts-plan-selected", spy);

    await waitFor(() => {
      expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
    });

    await step("selecting a known plan accepts and dispatches one event", async () => {
      const accepted = element.selectPlanByName("fapi2-security-profile-final-test-plan");
      expect(accepted).toBe(true);

      await waitFor(() => {
        expect(spy).toHaveBeenCalledTimes(1);
      });
      const eventDetail = spy.mock.calls[0][0].detail;
      expect(eventDetail.plan.planName).toBe("fapi2-security-profile-final-test-plan");
    });

    await step("internal tier state propagates to the rendered selects", async () => {
      await waitFor(() => {
        const familySelect = canvasElement.querySelector("#specFamilySelect");
        expect(familySelect.value).toBe("FAPI");
      });
      const planSelect = canvasElement.querySelector("#planSelect");
      expect(planSelect.value).toBe("fapi2-security-profile-final-test-plan");
    });

    await step("an unknown plan is rejected with no extra event", async () => {
      const rejected = element.selectPlanByName("does-not-exist-test-plan");
      expect(rejected).toBe(false);
      expect(spy).toHaveBeenCalledTimes(1);
    });
  },
};

/**
 * Re-selecting a plan from a *different* spec family must drive the Test Type
 * control to the new plan, not reset it to the placeholder.
 *
 * Regression test for the schedule-test.html report (FAPI2 -> AuthZen): when
 * `selectPlanByName` swaps the option set, lit-html commits the `<select>`'s
 * `.value` binding before its `<option>` children, so the new value is
 * applied while the *previous* family's options are still in the DOM — it
 * matches nothing and the control falls back to the placeholder. The fixture
 * mirrors the failing shape: a small family (2 plans) followed by a larger one
 * (5 plans) whose target plan sorts first.
 */
const TWO_FAMILY_PLANS = [
  {
    planName: "alpha-a-test-plan",
    displayName: "Alpha A",
    profile: "alpha-pdp",
    specFamily: "Alpha",
    specVersion: "v1",
    modules: [],
  },
  {
    planName: "alpha-b-test-plan",
    displayName: "Alpha B",
    profile: "alpha-pdp",
    specFamily: "Alpha",
    specVersion: "v1",
    modules: [],
  },
  {
    planName: "beta-a-test-plan",
    displayName: "Beta A",
    profile: "beta-pdp",
    specFamily: "Beta",
    specVersion: "v1",
    modules: [],
  },
  {
    planName: "beta-b-test-plan",
    displayName: "Beta B",
    profile: "beta-pdp",
    specFamily: "Beta",
    specVersion: "v1",
    modules: [],
  },
  {
    planName: "beta-c-test-plan",
    displayName: "Beta C",
    profile: "beta-pdp",
    specFamily: "Beta",
    specVersion: "v1",
    modules: [],
  },
  {
    planName: "beta-d-test-plan",
    displayName: "Beta D",
    profile: "beta-pdp",
    specFamily: "Beta",
    specVersion: "v1",
    modules: [],
  },
  {
    planName: "beta-e-test-plan",
    displayName: "Beta E",
    profile: "beta-pdp",
    specFamily: "Beta",
    specVersion: "v1",
    modules: [],
  },
];

export const ReselectAcrossFamiliesUpdatesTestType = {
  render: () => html`<cts-spec-cascade .plans=${TWO_FAMILY_PLANS}></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    const element = canvasElement.querySelector("cts-spec-cascade");
    await element.updateComplete;

    await step("first selection lands a plan in the small Alpha family", async () => {
      element.selectPlanByName("alpha-a-test-plan");
      await element.updateComplete;
      expect(canvasElement.querySelector("#planSelect").value).toBe("alpha-a-test-plan");
    });

    await step("re-selecting across to the larger Beta family follows the new plan", async () => {
      // The Test Type control must follow the new selection rather than reset
      // to placeholder.
      element.selectPlanByName("beta-a-test-plan");
      await element.updateComplete;
      expect(canvasElement.querySelector("#planSelect").value).toBe("beta-a-test-plan");
    });
  },
};

/**
 * Calling `selectPlanByName` while plans are still loading should queue the
 * request and replay it exactly once when plans arrive. This is the page-load
 * race in `schedule-test.html`: `applyConfigPreset` can fire before
 * `loadAvailablePlans()` resolves.
 */
export const ProgrammaticSelectionBeforeLoad = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan/available", async () => {
          // Delay so the cascade is still loading when play() starts.
          await new Promise((resolve) => setTimeout(resolve, 100));
          return HttpResponse.json(MOCK_PLANS);
        }),
      ],
    },
  },
  render: () => html`<cts-spec-cascade></cts-spec-cascade>`,
  async play({ canvasElement, step }) {
    const element = canvasElement.querySelector("cts-spec-cascade");
    const spy = fn();
    element.addEventListener("cts-plan-selected", spy);

    await step("selecting before load queues without firing the event", async () => {
      // Plans haven't loaded yet — the call should queue, not fire the event.
      const accepted = element.selectPlanByName("oidcc-basic-certification-test-plan");
      expect(accepted).toBe(true);
      expect(spy).not.toHaveBeenCalled();
    });

    await step("queued selection drains once plans arrive", async () => {
      // Once MSW resolves and the component receives plans, the queued
      // selection should drain and dispatch exactly one event.
      await waitFor(
        () => {
          expect(spy).toHaveBeenCalledTimes(1);
        },
        { timeout: 2000 },
      );

      const eventDetail = spy.mock.calls[0][0].detail;
      expect(eventDetail.plan.planName).toBe("oidcc-basic-certification-test-plan");

      const planSelect = canvasElement.querySelector("#planSelect");
      expect(planSelect.value).toBe("oidcc-basic-certification-test-plan");
    });
  },
};
