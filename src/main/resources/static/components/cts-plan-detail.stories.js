import { html } from "lit";
import { expect, within, waitFor, userEvent, fn } from "storybook/test";
import {
  MOCK_PLAN_DETAIL,
  MOCK_PLAN_PUBLISHED,
  MOCK_MODULES_WITH_STATUS,
} from "@fixtures/mock-test-data.js";
import "./cts-plan-header.js";
import "./cts-plan-modules.js";
import "./cts-plan-actions.js";

export default {
  title: "Components/cts-plan-detail",
};

const MODULES_WITH_STATUS = MOCK_MODULES_WITH_STATUS;

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

    // Click the inner native button — userEvent.click on the cts-button host
    // doesn't reach the inner @click handler.
    const runBtn = canvasElement.querySelector(
      '[data-testid="run-test-btn"] button',
    );
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

/**
 * Covers the second half of cts-plan-modules' disabled Run Test branch:
 * when a plan is marked `immutable` (published / certification-locked),
 * `_canRunTest()` returns false and the Run Test cts-button is omitted.
 * The readonly and immutable flags are OR'd inside the component, so either
 * one alone must disable the branch.
 */
export const ModulesImmutable = {
  render: () => html`
    <cts-plan-modules
      .modules=${MODULES_WITH_STATUS}
      plan-id="plan-immutable-001"
      is-immutable
    ></cts-plan-modules>
  `,
  async play({ canvasElement }) {
    const runBtns = canvasElement.querySelectorAll('[data-testid="run-test-btn"]');
    expect(runBtns.length).toBe(0);

    // Download Logs buttons still render for modules with instances.
    const downloadBtns = canvasElement.querySelectorAll("cts-button.downloadBtn");
    expect(downloadBtns.length).toBe(3);

    // View Logs links still render.
    const viewBtns = canvasElement.querySelectorAll(".viewBtn");
    expect(viewBtns.length).toBe(3);
  },
};

/**
 * Both flags set together — certified+published plans viewed by the owner.
 * Keeps the disabled branch robust against future refactors that might
 * change the OR to an AND.
 */
export const ModulesReadonlyAndImmutable = {
  render: () => html`
    <cts-plan-modules
      .modules=${MODULES_WITH_STATUS}
      plan-id="plan-abc-123"
      is-readonly
      is-immutable
    ></cts-plan-modules>
  `,
  async play({ canvasElement }) {
    const runBtns = canvasElement.querySelectorAll('[data-testid="run-test-btn"]');
    expect(runBtns.length).toBe(0);
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

export const ActionsGenerateLinkResult = {
  render: () => html`
    <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions>
  `,
  async play({ canvasElement }) {
    // Open private link panel
    const privateLinkBtn = canvasElement.querySelector(
      '[data-testid="private-link-btn"]',
    );
    await userEvent.click(privateLinkBtn);

    await waitFor(() => {
      const panel = canvasElement.querySelector(
        '[data-testid="private-link-panel"]',
      );
      expect(panel).toBeTruthy();
    });

    // Set days to 7 (valid)
    const daysInput = canvasElement.querySelector("#privateLinkDays");
    await userEvent.clear(daysInput);
    await userEvent.type(daysInput, "7");

    // Listen for the generate event before clicking
    const spy = fn();
    canvasElement.addEventListener("cts-generate-private-link", spy);

    // Click Generate
    const generateBtn = canvasElement.querySelector(".generate-link-btn");
    await waitFor(() => expect(generateBtn.disabled).toBe(false));
    await userEvent.click(generateBtn);

    expect(spy).toHaveBeenCalledTimes(1);
    const detail = spy.mock.calls[0][0].detail;
    expect(detail.planId).toBe("plan-abc-123");
    expect(detail.days).toBe(7);

    // Simulate the parent wiring the result back into the component.
    // The component never sets _privateLinkResult itself; the parent receives
    // the cts-generate-private-link event, calls the server, and sets the URL
    // on the element. This story exercises the full display path.
    const el = canvasElement.querySelector("cts-plan-actions");
    const generatedUrl =
      "https://localhost.emobix.co.uk:8443/plan-detail.html?plan=plan-abc-123&token=mock-token-7d";
    el._privateLinkResult = generatedUrl;
    await el.requestUpdate();

    await waitFor(() => {
      const result = canvasElement.querySelector(
        '[data-testid="private-link-result"]',
      );
      expect(result).toBeTruthy();
      expect(result.textContent).toContain(generatedUrl);
    });
  },
};

export const ActionsDeletePlanCancel = {
  render: () => html`
    <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions>
  `,
  async play({ canvasElement }) {
    // Listen for delete event before any clicks — must NOT fire on cancel
    const deleteSpy = fn();
    canvasElement.addEventListener("cts-delete-plan", deleteSpy);

    // Open delete confirm panel
    const deleteBtn = canvasElement.querySelector(
      '[data-testid="delete-plan-btn"]',
    );
    await userEvent.click(deleteBtn);

    let confirmPanel;
    await waitFor(() => {
      confirmPanel = canvasElement.querySelector(
        '[data-testid="delete-confirm-panel"]',
      );
      expect(confirmPanel).toBeTruthy();
    });

    // Click Cancel
    const cancelBtn = Array.from(confirmPanel.querySelectorAll("button")).find(
      (b) => b.textContent.trim() === "Cancel",
    );
    expect(cancelBtn).toBeTruthy();
    await userEvent.click(cancelBtn);

    // Panel should close
    await waitFor(() => {
      expect(
        canvasElement.querySelector('[data-testid="delete-confirm-panel"]'),
      ).toBeNull();
    });

    // No delete event fired
    expect(deleteSpy).not.toHaveBeenCalled();

    // Clicking Cancel-equivalent again is a no-op (panel already closed) —
    // verify by re-opening and confirming the panel re-renders cleanly.
    await userEvent.click(deleteBtn);
    await waitFor(() => {
      expect(
        canvasElement.querySelector('[data-testid="delete-confirm-panel"]'),
      ).toBeTruthy();
    });
    expect(deleteSpy).not.toHaveBeenCalled();
  },
};

export const ActionsCopyConfig = {
  render: () => html`
    <cts-plan-actions .plan=${PLAN_WITH_CONFIG}></cts-plan-actions>
  `,
  async play({ canvasElement }) {
    // Mock navigator.clipboard.writeText (same pattern as cts-log-entry CopyAsCurl)
    const mockWriteText = fn().mockResolvedValue(undefined);
    const originalClipboard = navigator.clipboard;
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: mockWriteText },
      writable: true,
      configurable: true,
    });

    try {
      // Open the config panel
      const viewConfigBtn = canvasElement.querySelector(
        '[data-testid="view-config-btn"]',
      );
      await userEvent.click(viewConfigBtn);

      await waitFor(() => {
        const panel = canvasElement.querySelector(
          '[data-testid="config-panel"]',
        );
        expect(panel).toBeTruthy();
      });

      // Click the Copy button inside the panel
      const copyBtn = canvasElement.querySelector(".copy-config-btn");
      expect(copyBtn).toBeTruthy();
      await userEvent.click(copyBtn);

      // Clipboard should have been called once with pretty-printed JSON
      expect(mockWriteText).toHaveBeenCalledOnce();
      const written = mockWriteText.mock.calls[0][0];
      expect(written).toBe(JSON.stringify(PLAN_WITH_CONFIG.config, null, 4));
    } finally {
      Object.defineProperty(navigator, "clipboard", {
        value: originalClipboard,
        writable: true,
        configurable: true,
      });
    }
  },
};
