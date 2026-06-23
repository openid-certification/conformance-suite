import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { MOCK_PLANS } from "@fixtures/mock-plans.js";
import "../cts-test-selector.js";
import "../cts-config-form.js";
import "../cts-button.js";
import "../cts-action-bar.js";

export default {
  title: "Flows/Schedule Test",
  // cts-test-selector persists the active family filter to localStorage; clear
  // it before the story so a sibling story's filter never hides the row this
  // flow clicks. Mirrors the cts-test-selector meta beforeEach.
  beforeEach: () => {
    localStorage.removeItem("cts:test-selector-filter");
    localStorage.removeItem("cts:favorite-plans");
  },
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
 * Full schedule-test flow: pick a plan in cts-test-selector (the sole
 * plan-entry point now that cts-spec-cascade is removed) → the config form
 * appears → fill it → the submit button is present.
 *
 * On the real page, clicking a row fires `cts-plan-select`; the page handler
 * resolves the plan and dispatches `cts-plan-selected`, which binds
 * cts-config-form's schema (see schedule-test.html). This story wires the
 * picker's `cts-plan-select` straight to the config form to illustrate that
 * cross-component flow in isolation — the per-component stories cover the
 * picker and the form individually, but not the hand-off between them.
 */
export const FullFlow = {
  render: () => {
    // Wire picker → config form inline (stands in for the page's
    // cts-plan-select handler + cts-plan-selected listener).
    function handlePlanSelected() {
      const form = /** @type {any} */ (document.querySelector("#flow-config-form"));
      if (form) {
        form.schema = OIDCC_SCHEMA;
        form.config = {};
      }
      const configSection = /** @type {HTMLElement | null} */ (
        document.querySelector("#config-section")
      );
      if (configSection) configSection.style.display = "block";
    }

    return html`
      <div class="schedule-test-page">
        <h2>Create a new test plan</h2>
        <cts-test-selector
          .plans=${MOCK_PLANS}
          @cts-plan-select=${handlePlanSelected}
        ></cts-test-selector>
        <div id="config-section" style="display:none">
          <h3>Configuration</h3>
          <cts-config-form id="flow-config-form"></cts-config-form>
          <cts-action-bar position="static" align-to="schedule-test-page">
            <cts-button
              id="submit-plan"
              size="lg"
              variant="primary"
              icon="flag"
              label="Create Test Plan"
            ></cts-button>
          </cts-action-bar>
        </div>
      </div>
    `;
  },
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("Wait for the picker to render plan rows", async () => {
      await waitFor(() => {
        expect(
          canvasElement.querySelector('[data-plan-name="oidcc-basic-certification-test-plan"]'),
        ).toBeInTheDocument();
      });
    });

    await step("Pick the OIDCC Basic plan", async () => {
      // waitFor above already guaranteed the row exists, so the non-null
      // cast is safe; the toBeTruthy assertion still catches a regression.
      const row = /** @type {HTMLElement} */ (
        canvasElement.querySelector('[data-plan-name="oidcc-basic-certification-test-plan"]')
      );
      expect(row).toBeTruthy();
      await userEvent.click(row);
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
      // cts-form-field carries `name` on the HOST element, not the inner
      // <input>. The inner input has only `id="${this._uid}"`. Query the
      // host first, then the descendant input. Asserting the inner input
      // is found ensures the fill step does not silently no-op if the
      // host structure changes.
      const fieldHost = canvasElement.querySelector('cts-form-field[name="server.issuer"]');
      expect(fieldHost).toBeTruthy();
      const issuerInput = fieldHost.querySelector("input");
      expect(issuerInput).toBeTruthy();
      await userEvent.type(issuerInput, "https://op.example.com");
    });

    await step("Verify submit button is present", async () => {
      const submitBtn = canvas.getByText("Create Test Plan");
      expect(submitBtn).toBeInTheDocument();
    });
  },
};
