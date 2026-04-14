import { html } from "lit";
import { expect, within, waitFor, userEvent, fn } from "storybook/test";
import { MOCK_PLAN_DETAIL, MOCK_PLAN_PUBLISHED } from "@fixtures/mock-test-data.js";
import { MOCK_USER, MOCK_ADMIN_USER } from "@fixtures/mock-users.js";
import "./cts-plan-header.js";
import "./cts-plan-modules.js";
import "./cts-plan-actions.js";

export default {
  title: "Components/cts-plan-detail",
};

// --- Mock data with status/result on modules for the modules component ---

const MODULES_WITH_STATUS = [
  {
    ...MOCK_PLAN_DETAIL.modules[0],
    status: "FINISHED",
    result: "PASSED",
  },
  {
    ...MOCK_PLAN_DETAIL.modules[1],
    status: "FINISHED",
    result: "WARNING",
  },
  {
    ...MOCK_PLAN_DETAIL.modules[2],
    status: "FINISHED",
    result: "FAILED",
  },
  {
    ...MOCK_PLAN_DETAIL.modules[3],
    status: null,
    result: null,
  },
];

const PLAN_WITH_CONFIG = {
  ...MOCK_PLAN_DETAIL,
  config: {
    "server.issuer": "https://op.example.com",
    "client.client_id": "test-client-id",
    "client.client_secret": "test-client-secret",
  },
};

const PLAN_IMMUTABLE = {
  ...MOCK_PLAN_DETAIL,
  _id: "plan-immutable-001",
  immutable: true,
  publish: "everything",
};

// ==========================================================================
// Plan Header stories
// ==========================================================================

export const PlanHeaderDefault = {
  render: () => html`<cts-plan-header .plan=${MOCK_PLAN_DETAIL}></cts-plan-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Plan name displayed
    expect(canvas.getByText("oidcc-basic-certification-test-plan")).toBeInTheDocument();

    // Plan ID displayed
    expect(canvas.getByText("plan-abc-123")).toBeInTheDocument();

    // Description displayed
    expect(canvas.getByText(/Basic Certification Profile authorization server test/)).toBeInTheDocument();

    // Version displayed
    expect(canvas.getByText("5.1.24-SNAPSHOT (9063a08)")).toBeInTheDocument();

    // Variant displayed as key=value
    expect(canvas.getByText(/client_auth_type=client_secret_basic/)).toBeInTheDocument();

    // Started date is rendered (non-empty)
    const startedRow = canvasElement.querySelectorAll(".row");
    const startedValues = Array.from(startedRow)
      .filter((r) => r.textContent.includes("Started:"));
    expect(startedValues.length).toBeGreaterThan(0);

    // Certification profile is displayed
    expect(canvas.getByText("OC Basic OP")).toBeInTheDocument();

    // Summary is displayed
    expect(canvas.getByText(/Basic OP certification test plan/)).toBeInTheDocument();

    // Owner row should NOT be visible (isAdmin is false)
    const ownerRow = canvasElement.querySelector('[data-testid="owner-row"]');
    expect(ownerRow).toBeNull();

    // Certification disclaimer text present
    expect(canvas.getByText(/OpenID Foundation conformance suite/)).toBeInTheDocument();
  },
};

export const PlanHeaderAdmin = {
  render: () => html`<cts-plan-header .plan=${MOCK_PLAN_DETAIL} is-admin></cts-plan-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Owner row IS visible for admin
    const ownerRow = canvasElement.querySelector('[data-testid="owner-row"]');
    expect(ownerRow).toBeTruthy();
    expect(canvas.getByText("Test Owner:")).toBeInTheDocument();
    expect(canvas.getByText(/12345/)).toBeInTheDocument();

    // All other fields still present
    expect(canvas.getByText("oidcc-basic-certification-test-plan")).toBeInTheDocument();
    expect(canvas.getByText("plan-abc-123")).toBeInTheDocument();
  },
};

export const PlanHeaderPublished = {
  render: () => html`<cts-plan-header .plan=${MOCK_PLAN_PUBLISHED} is-admin></cts-plan-header>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Published plan ID
    expect(canvas.getByText("plan-pub-789")).toBeInTheDocument();

    // Certification profile displayed
    const certRow = canvasElement.querySelector('[data-testid="certification-row"]');
    expect(certRow).toBeTruthy();
    expect(canvas.getByText("OC Basic OP")).toBeInTheDocument();

    // Summary displayed
    expect(canvas.getByText(/Basic OP certification test plan/)).toBeInTheDocument();

    // Owner visible for admin
    const ownerRow = canvasElement.querySelector('[data-testid="owner-row"]');
    expect(ownerRow).toBeTruthy();
  },
};

// ==========================================================================
// Plan Modules stories
// ==========================================================================

export const ModulesDefault = {
  render: () => html`
    <cts-plan-modules
      .modules=${MODULES_WITH_STATUS}
      plan-id="plan-abc-123"
    ></cts-plan-modules>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // All module names rendered
    expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    expect(canvas.getByText("oidcc-server-rotate-keys")).toBeInTheDocument();
    expect(canvas.getByText("oidcc-ensure-redirect-uri-in-authorization-request")).toBeInTheDocument();
    expect(canvas.getByText("oidcc-codereuse")).toBeInTheDocument();

    // Badges rendered with correct variants
    const badges = canvasElement.querySelectorAll("cts-badge");
    expect(badges.length).toBe(4);

    // First badge: PASSED -> success
    expect(badges[0].getAttribute("variant")).toBe("success");
    expect(badges[0].getAttribute("label")).toBe("PASSED");

    // Second badge: WARNING -> warning
    expect(badges[1].getAttribute("variant")).toBe("warning");
    expect(badges[1].getAttribute("label")).toBe("WARNING");

    // Third badge: FAILED -> failure
    expect(badges[2].getAttribute("variant")).toBe("failure");
    expect(badges[2].getAttribute("label")).toBe("FAILED");

    // Fourth badge: null -> PENDING (secondary)
    expect(badges[3].getAttribute("variant")).toBe("secondary");
    expect(badges[3].getAttribute("label")).toBe("PENDING");

    // Test IDs rendered
    expect(canvas.getByText("test-inst-001")).toBeInTheDocument();
    expect(canvas.getByText("test-inst-002")).toBeInTheDocument();
    expect(canvas.getByText("test-inst-003")).toBeInTheDocument();

    // Module with no instance shows NONE
    const noneTexts = Array.from(canvasElement.querySelectorAll(".col-md-10"))
      .filter((el) => el.textContent.trim() === "NONE");
    expect(noneTexts.length).toBe(1);

    // Run Test buttons present (not readonly, not immutable by default)
    const runBtns = canvasElement.querySelectorAll('[data-testid="run-test-btn"]');
    expect(runBtns.length).toBe(4);

    // View Logs links present for modules with instances
    const viewBtns = canvasElement.querySelectorAll(".viewBtn");
    expect(viewBtns.length).toBe(3);

    // Download buttons present for modules with instances
    const downloadBtns = canvasElement.querySelectorAll(".downloadBtn");
    expect(downloadBtns.length).toBe(3);
  },
};

export const ModulesRunTest = {
  render: () => html`
    <cts-plan-modules
      .modules=${MODULES_WITH_STATUS}
      plan-id="plan-abc-123"
    ></cts-plan-modules>
  `,
  async play({ canvasElement }) {
    const spy = fn();
    canvasElement.addEventListener("cts-run-test", spy);

    // Click the first Run Test button
    const runBtn = canvasElement.querySelector('[data-testid="run-test-btn"]');
    expect(runBtn).toBeTruthy();
    await userEvent.click(runBtn);

    // Event should fire with correct detail
    expect(spy).toHaveBeenCalledTimes(1);
    const detail = spy.mock.calls[0][0].detail;
    expect(detail.testModule).toBe("oidcc-server");
    expect(detail.variant).toEqual({
      client_auth_type: "client_secret_basic",
      response_type: "code",
    });
  },
};

export const ModulesReadonly = {
  render: () => html`
    <cts-plan-modules
      .modules=${MODULES_WITH_STATUS}
      plan-id="plan-abc-123"
      is-readonly
    ></cts-plan-modules>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Run Test buttons should NOT be present
    const runBtns = canvasElement.querySelectorAll('[data-testid="run-test-btn"]');
    expect(runBtns.length).toBe(0);

    // Module names still displayed
    expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    expect(canvas.getByText("oidcc-server-rotate-keys")).toBeInTheDocument();

    // View Logs links still present for modules with instances
    const viewBtns = canvasElement.querySelectorAll(".viewBtn");
    expect(viewBtns.length).toBe(3);
  },
};

// ==========================================================================
// Plan Actions stories
// ==========================================================================

export const ActionsViewConfig = {
  render: () => html`
    <cts-plan-actions .plan=${PLAN_WITH_CONFIG}></cts-plan-actions>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Config panel not visible initially
    let configPanel = canvasElement.querySelector('[data-testid="config-panel"]');
    expect(configPanel).toBeNull();

    // Click View Config button
    const viewConfigBtn = canvasElement.querySelector('[data-testid="view-config-btn"]');
    expect(viewConfigBtn).toBeTruthy();
    await userEvent.click(viewConfigBtn);

    // Config panel should now be visible
    await waitFor(() => {
      configPanel = canvasElement.querySelector('[data-testid="config-panel"]');
      expect(configPanel).toBeTruthy();
    });

    // JSON content displayed
    const configJson = canvasElement.querySelector(".config-json");
    expect(configJson).toBeTruthy();
    expect(configJson.textContent).toContain("server.issuer");
    expect(configJson.textContent).toContain("https://op.example.com");
    expect(configJson.textContent).toContain("client.client_id");

    // Plan ID displayed
    expect(canvas.getByText("plan-abc-123")).toBeInTheDocument();
  },
};

export const ActionsPrivateLink = {
  render: () => html`
    <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions>
  `,
  async play({ canvasElement }) {
    // Private link panel not visible initially
    let panel = canvasElement.querySelector('[data-testid="private-link-panel"]');
    expect(panel).toBeNull();

    // Click Private link button
    const privateLinkBtn = canvasElement.querySelector('[data-testid="private-link-btn"]');
    expect(privateLinkBtn).toBeTruthy();
    await userEvent.click(privateLinkBtn);

    // Panel should now be visible
    await waitFor(() => {
      panel = canvasElement.querySelector('[data-testid="private-link-panel"]');
      expect(panel).toBeTruthy();
    });

    // Days input present with default value of 30
    const daysInput = canvasElement.querySelector("#privateLinkDays");
    expect(daysInput).toBeTruthy();
    expect(daysInput.value).toBe("30");

    // Generate button should be enabled (30 is valid)
    const generateBtn = canvasElement.querySelector(".generate-link-btn");
    expect(generateBtn).toBeTruthy();
    expect(generateBtn.disabled).toBe(false);
  },
};

export const ActionsPrivateLinkValidation = {
  render: () => html`
    <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions>
  `,
  async play({ canvasElement }) {
    // Open private link panel
    const privateLinkBtn = canvasElement.querySelector('[data-testid="private-link-btn"]');
    await userEvent.click(privateLinkBtn);

    await waitFor(() => {
      const panel = canvasElement.querySelector('[data-testid="private-link-panel"]');
      expect(panel).toBeTruthy();
    });

    const daysInput = canvasElement.querySelector("#privateLinkDays");
    const generateBtn = canvasElement.querySelector(".generate-link-btn");

    // Clear and type 0 (invalid)
    await userEvent.clear(daysInput);
    await userEvent.type(daysInput, "0");

    await waitFor(() => {
      const btn = canvasElement.querySelector(".generate-link-btn");
      expect(btn.disabled).toBe(true);
    });

    // Clear and type 1001 (invalid)
    await userEvent.clear(daysInput);
    await userEvent.type(daysInput, "1001");

    await waitFor(() => {
      const btn = canvasElement.querySelector(".generate-link-btn");
      expect(btn.disabled).toBe(true);
    });

    // Clear and type 500 (valid)
    await userEvent.clear(daysInput);
    await userEvent.type(daysInput, "500");

    await waitFor(() => {
      const btn = canvasElement.querySelector(".generate-link-btn");
      expect(btn.disabled).toBe(false);
    });
  },
};

export const ActionsDeletePlan = {
  render: () => html`
    <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Delete confirm not visible initially
    let confirmPanel = canvasElement.querySelector('[data-testid="delete-confirm-panel"]');
    expect(confirmPanel).toBeNull();

    // Click Delete plan button
    const deleteBtn = canvasElement.querySelector('[data-testid="delete-plan-btn"]');
    expect(deleteBtn).toBeTruthy();
    await userEvent.click(deleteBtn);

    // Confirm panel should appear
    await waitFor(() => {
      confirmPanel = canvasElement.querySelector('[data-testid="delete-confirm-panel"]');
      expect(confirmPanel).toBeTruthy();
    });

    // Warning text present
    expect(canvas.getByText(/permanently and irrevocably/)).toBeInTheDocument();
    expect(canvas.getByText(/cannot be undone/)).toBeInTheDocument();

    // Confirm delete button present
    const confirmBtn = canvasElement.querySelector(".confirm-delete-btn");
    expect(confirmBtn).toBeTruthy();
    expect(confirmBtn.textContent.trim()).toBe("Delete plan");

    // Cancel button present
    const cancelBtns = confirmPanel.querySelectorAll("button");
    const cancelBtn = Array.from(cancelBtns).find(
      (b) => b.textContent.trim() === "Cancel",
    );
    expect(cancelBtn).toBeTruthy();

    // Verify cts-delete-plan event fires on confirm
    const spy = fn();
    canvasElement.addEventListener("cts-delete-plan", spy);
    await userEvent.click(confirmBtn);

    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy.mock.calls[0][0].detail.planId).toBe("plan-abc-123");
  },
};

export const ActionsImmutablePlan = {
  render: () => html`
    <cts-plan-actions .plan=${PLAN_IMMUTABLE} is-admin is-readonly></cts-plan-actions>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Edit config should NOT be visible (readonly)
    const editBtn = canvasElement.querySelector('[data-testid="edit-config-btn"]');
    expect(editBtn).toBeNull();

    // Delete plan should NOT be visible (immutable makes it hidden since readonly)
    const deleteBtn = canvasElement.querySelector('[data-testid="delete-plan-btn"]');
    expect(deleteBtn).toBeNull();

    // Make Mutable should be visible (admin + immutable)
    const mutableBtn = canvasElement.querySelector('[data-testid="make-mutable-btn"]');
    expect(mutableBtn).toBeTruthy();
    expect(mutableBtn.textContent).toContain("Make plan Mutable");

    // Verify event fires on click
    const spy = fn();
    canvasElement.addEventListener("cts-make-mutable", spy);
    await userEvent.click(mutableBtn);

    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy.mock.calls[0][0].detail.planId).toBe("plan-immutable-001");

    // View Config should still be available
    const viewConfigBtn = canvasElement.querySelector('[data-testid="view-config-btn"]');
    expect(viewConfigBtn).toBeTruthy();
  },
};

export const ActionsPublishedPlan = {
  render: () => html`
    <cts-plan-actions
      .plan=${{ ...MOCK_PLAN_DETAIL, publish: "everything" }}
      is-admin
    ></cts-plan-actions>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Unpublish button visible (admin + published + not readonly)
    const unpublishBtn = canvasElement.querySelector('[data-testid="unpublish-btn"]');
    expect(unpublishBtn).toBeTruthy();
    expect(unpublishBtn.textContent).toContain("Unpublish");

    // Publish summary/everything should NOT be visible (already published)
    const publishSummaryBtn = canvasElement.querySelector('[data-testid="publish-summary-btn"]');
    expect(publishSummaryBtn).toBeNull();
    const publishEverythingBtn = canvasElement.querySelector('[data-testid="publish-everything-btn"]');
    expect(publishEverythingBtn).toBeNull();

    // Public link should be visible
    expect(canvas.getByText("Public link")).toBeInTheDocument();

    // Verify unpublish event
    const spy = fn();
    canvasElement.addEventListener("cts-unpublish", spy);
    await userEvent.click(unpublishBtn);

    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy.mock.calls[0][0].detail.planId).toBe("plan-abc-123");

    // Download all should be visible (admin)
    const downloadAllBtn = canvasElement.querySelector('[data-testid="download-all-btn"]');
    expect(downloadAllBtn).toBeTruthy();
  },
};
