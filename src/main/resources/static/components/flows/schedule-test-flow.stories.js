import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_PLANS } from "@fixtures/mock-plans.js";
import "../cts-spec-cascade.js";
import "../cts-config-form.js";
import "../cts-button.js";

export default {
  title: "Flows/Schedule Test",
};

// Minimal schema fixture for OIDCC Basic
const OIDCC_SCHEMA = {
  type: "object",
  properties: {
    "server.issuer": {
      type: "string",
      title: "Server Issuer",
      description: "The issuer URL of the OpenID Provider",
    },
    "client.client_id": {
      type: "string",
      title: "Client ID",
      description: "The client_id for the test client",
    },
    "client.client_secret": {
      type: "string",
      title: "Client Secret",
      description: "The client_secret for the test client",
    },
  },
  required: ["server.issuer", "client.client_id"],
};

/**
 * Full schedule test flow: select spec → cascade through dropdowns →
 * config form appears → fill form → submit.
 *
 * This story wires cts-spec-cascade's cts-plan-selected event to
 * cts-config-form's schema/config props inline in the render function.
 */
export const FullFlow = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS)),
        http.post("/api/runner", async ({ request }) => {
          const body = /** @type {{ planName?: string } | null} */ (await request.json());
          return HttpResponse.json({
            name: (body && body.planName) || "oidcc-server",
            id: "test-new-001",
            url: "https://localhost.emobix.co.uk:8443/log-detail.html?log=test-new-001",
          });
        }),
      ],
    },
  },
  render: () => {
    // Wire cascade → config form inline
    function handlePlanSelected() {
      const form = /** @type {any} */ (document.querySelector("#flow-config-form"));
      if (form) {
        form.schema = OIDCC_SCHEMA;
        form.config = {};
      }
      // Show config section
      const configSection = /** @type {HTMLElement | null} */ (
        document.querySelector("#config-section")
      );
      if (configSection) configSection.style.display = "block";
    }

    return html`
      <div class="schedule-test-page">
        <h2>Create a new test plan</h2>
        <cts-spec-cascade @cts-plan-selected=${handlePlanSelected}></cts-spec-cascade>
        <div id="config-section" style="display:none">
          <h3>Configuration</h3>
          <cts-config-form id="flow-config-form"></cts-config-form>
          <div class="launch-panel">
            <cts-button
              id="submit-plan"
              size="lg"
              variant="primary"
              icon="flag"
              label="Create Test Plan"
            ></cts-button>
          </div>
        </div>
      </div>
    `;
  },
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("Wait for cascade to load", async () => {
      await waitFor(() => {
        expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
      });
    });

    await step("Select spec family OIDCC", async () => {
      const specSelect = canvasElement.querySelector("#specFamilySelect");
      await userEvent.selectOptions(specSelect, "OIDCC");
    });

    await step("Select entity OP", async () => {
      await waitFor(() => {
        const entityRow = canvasElement.querySelector('[data-testid="entitySelect-field"]');
        expect(entityRow?.style.display).not.toBe("none");
      });
      const entitySelect = canvasElement.querySelector("#entitySelect");
      await userEvent.selectOptions(entitySelect, "OP");
    });

    await step("Select plan", async () => {
      await waitFor(() => {
        const planRow = canvasElement.querySelector('[data-testid="planSelect-field"]');
        expect(planRow?.style.display).not.toBe("none");
      });
      const planSelect = canvasElement.querySelector("#planSelect");
      await userEvent.selectOptions(planSelect, "oidcc-basic-certification-test-plan");
    });

    await step("Verify config form appears", async () => {
      await waitFor(() => {
        const configSection = canvasElement.querySelector("#config-section");
        expect(configSection.style.display).toBe("block");
      });

      // Config form should have fields from the schema
      await waitFor(() => {
        expect(canvas.getByText("Server Issuer")).toBeInTheDocument();
      });
    });

    await step("Fill the config form", async () => {
      const issuerInput = canvasElement.querySelector('input[name="server.issuer"]');
      if (issuerInput) {
        await userEvent.type(issuerInput, "https://op.example.com");
      }
    });

    await step("Verify submit button is present", async () => {
      const submitBtn = canvas.getByText("Create Test Plan");
      expect(submitBtn).toBeInTheDocument();
    });
  },
};

/**
 * Edge case: Change plan mid-flow — form should reset.
 */
export const ChangePlanMidFlow = {
  parameters: {
    msw: {
      handlers: [http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS))],
    },
  },
  render: () => {
    function handlePlanSelected() {
      const form = /** @type {any} */ (document.querySelector("#flow-config-form-2"));
      if (form) {
        form.schema = OIDCC_SCHEMA;
        form.config = {};
      }
      const configSection = /** @type {HTMLElement | null} */ (
        document.querySelector("#config-section-2")
      );
      if (configSection) configSection.style.display = "block";
    }

    return html`
      <div class="schedule-test-page">
        <cts-spec-cascade @cts-plan-selected=${handlePlanSelected}></cts-spec-cascade>
        <div id="config-section-2" style="display:none">
          <cts-config-form id="flow-config-form-2"></cts-config-form>
        </div>
      </div>
    `;
  },
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("Load and select first plan", async () => {
      await waitFor(() => {
        expect(canvas.getByLabelText("Specification")).toBeInTheDocument();
      });
      const specSelect = canvasElement.querySelector("#specFamilySelect");
      await userEvent.selectOptions(specSelect, "OIDCC");

      await waitFor(() => {
        const entityRow = canvasElement.querySelector('[data-testid="entitySelect-field"]');
        expect(entityRow?.style.display).not.toBe("none");
      });
      const entitySelect = canvasElement.querySelector("#entitySelect");
      await userEvent.selectOptions(entitySelect, "OP");

      await waitFor(() => {
        const planRow = canvasElement.querySelector('[data-testid="planSelect-field"]');
        expect(planRow?.style.display).not.toBe("none");
      });
      const planSelect = canvasElement.querySelector("#planSelect");
      await userEvent.selectOptions(planSelect, "oidcc-basic-certification-test-plan");
    });

    await step("Config form shows", async () => {
      await waitFor(() => {
        const configSection = canvasElement.querySelector("#config-section-2");
        expect(configSection.style.display).toBe("block");
      });
    });

    await step("Change family to trigger reset", async () => {
      const specSelect = canvasElement.querySelector("#specFamilySelect");
      await userEvent.selectOptions(specSelect, "FAPI");

      // FAPI auto-cascades through — new plan selected event fires
      // Config form should still be visible (new schema applied)
      await waitFor(() => {
        const configSection = canvasElement.querySelector("#config-section-2");
        expect(configSection.style.display).toBe("block");
      });
    });
  },
};
