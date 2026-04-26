import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import { MOCK_PLAN_DETAIL, MOCK_MODULES_WITH_STATUS } from "@fixtures/mock-test-data.js";

import "../../../src/main/resources/static/components/cts-plan-header.js";
import "../../../src/main/resources/static/components/cts-plan-modules.js";
import "../../../src/main/resources/static/components/cts-plan-actions.js";

export default {
  title: "Pages/PlanDetail",
};

const PLAN_WITH_MODULE_STATUS = {
  ...MOCK_PLAN_DETAIL,
  modules: MOCK_MODULES_WITH_STATUS,
};

export const Default = {
  render: () => html`
    <div class="container-fluid p-3">
      <div class="card mb-3">
        <div class="card-body bg-gradient">
          <div class="row">
            <div class="col-md-10">
              <cts-plan-header .plan=${PLAN_WITH_MODULE_STATUS}></cts-plan-header>
            </div>
            <div class="col-md-2">
              <cts-plan-actions .plan=${PLAN_WITH_MODULE_STATUS}></cts-plan-actions>
            </div>
          </div>
        </div>
      </div>
      <cts-plan-modules
        .modules=${MOCK_MODULES_WITH_STATUS}
        plan-id="plan-abc-123"
      ></cts-plan-modules>
    </div>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Header region: plan name and ID
    await waitFor(() => {
      expect(canvas.getByText("oidcc-basic-certification-test-plan")).toBeInTheDocument();
    });
    expect(canvas.getByText("plan-abc-123")).toBeInTheDocument();

    // Modules region: at least two module rows render
    expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    expect(canvas.getByText("oidcc-server-rotate-keys")).toBeInTheDocument();

    // Modules region: status badges present
    const moduleBadges = canvasElement
      .querySelector("cts-plan-modules")
      .querySelectorAll("cts-badge");
    expect(moduleBadges.length).toBe(MOCK_MODULES_WITH_STATUS.length);

    // Actions region: View configuration button present (always visible)
    const viewConfigBtn = canvasElement.querySelector('[data-testid="view-config-btn"]');
    expect(viewConfigBtn).toBeTruthy();

    // All three regions present in the same canvas
    expect(canvasElement.querySelector("cts-plan-header")).toBeTruthy();
    expect(canvasElement.querySelector("cts-plan-modules")).toBeTruthy();
    expect(canvasElement.querySelector("cts-plan-actions")).toBeTruthy();

    // Non-admin: Delete plan button is present (non-readonly + non-immutable),
    // Download all Logs button is NOT present (admin-only)
    expect(canvasElement.querySelector('[data-testid="delete-plan-btn"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="download-all-btn"]')).toBeNull();
  },
};

export const AdminView = {
  render: () => html`
    <div class="container-fluid p-3">
      <div class="card mb-3">
        <div class="card-body bg-gradient">
          <div class="row">
            <div class="col-md-10">
              <cts-plan-header .plan=${PLAN_WITH_MODULE_STATUS} is-admin></cts-plan-header>
            </div>
            <div class="col-md-2">
              <cts-plan-actions .plan=${PLAN_WITH_MODULE_STATUS} is-admin></cts-plan-actions>
            </div>
          </div>
        </div>
      </div>
      <cts-plan-modules
        .modules=${MOCK_MODULES_WITH_STATUS}
        plan-id="plan-abc-123"
      ></cts-plan-modules>
    </div>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Header region: admin-only Owner row visible
    await waitFor(() => {
      const ownerRow = canvasElement.querySelector('[data-testid="owner-row"]');
      expect(ownerRow).toBeTruthy();
    });
    expect(canvas.getByText("Test Owner:")).toBeInTheDocument();

    // Actions region: admin-only buttons visible
    expect(canvasElement.querySelector('[data-testid="download-all-btn"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="delete-plan-btn"]')).toBeTruthy();

    // Publish summary/everything visible (admin + not published)
    expect(canvasElement.querySelector('[data-testid="publish-summary-btn"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="publish-everything-btn"]')).toBeTruthy();
  },
};
