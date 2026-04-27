import { html } from "lit";
import { expect, within, waitFor, userEvent, fn, spyOn } from "storybook/test";
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

/**
 * Resolve the inner `<button>` rendered inside a cts-button host located by
 * data-testid. cts-button renders to its own light DOM and Lit binds
 * `@click` on the inner `<button>`, so a click on the host doesn't fire
 * the inner handler (see components/AGENTS.md §2).
 *
 * @param {HTMLElement} canvasElement
 * @param {string} testId
 * @returns {HTMLButtonElement}
 */
function innerButton(canvasElement, testId) {
  const host = canvasElement.querySelector(`[data-testid="${testId}"]`);
  if (!host) throw new Error(`No element with data-testid="${testId}"`);
  const btn = host.querySelector("button");
  if (!btn) throw new Error(`No <button> inside [data-testid="${testId}"]`);
  return /** @type {HTMLButtonElement} */ (btn);
}

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
    expect(
      canvas.getByText(/Basic Certification Profile authorization server test/),
    ).toBeInTheDocument();

    // Version displayed
    expect(canvas.getByText("5.1.24-SNAPSHOT (9063a08)")).toBeInTheDocument();

    // Variant displayed as key=value
    expect(canvas.getByText(/client_auth_type=client_secret_basic/)).toBeInTheDocument();

    // Started date is rendered (Started: term + non-empty value cell).
    // After U17 the meta rows are a token-styled <dl>; the term + colon
    // text matches the legacy "Label:" convention so test queries keep
    // working unchanged.
    expect(canvas.getByText("Started:")).toBeInTheDocument();

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

/**
 * R9 (Unit 5): a plan without a description must NOT render an empty
 * "Description:" row. The header conditionally suppresses the dt/dd pair
 * when `plan.description` is falsy.
 */
export const PlanHeaderNoDescription = {
  render: () => {
    const plan = { ...MOCK_PLAN_DETAIL, description: "" };
    return html`<cts-plan-header .plan=${plan}></cts-plan-header>`;
  },
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Plan name still rendered
    expect(canvas.getByText("oidcc-basic-certification-test-plan")).toBeInTheDocument();

    // The Description row is suppressed entirely
    const descRow = canvasElement.querySelector('[data-testid="description-row"]');
    expect(descRow).toBeNull();
    expect(canvas.queryByText("Description:")).toBeNull();
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
    <cts-plan-modules .modules=${MODULES_WITH_STATUS} plan-id="plan-abc-123"></cts-plan-modules>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // All module names rendered
    expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    expect(canvas.getByText("oidcc-server-rotate-keys")).toBeInTheDocument();
    expect(
      canvas.getByText("oidcc-ensure-redirect-uri-in-authorization-request"),
    ).toBeInTheDocument();
    expect(canvas.getByText("oidcc-codereuse")).toBeInTheDocument();

    // Badges rendered with correct variants. cts-plan-modules maps
    // FINISHED+result onto the canonical status palette names defined by
    // cts-badge: pass, warn, fail, skip, review, running.
    const badges = canvasElement.querySelectorAll("cts-badge");
    expect(badges.length).toBe(4);

    // First badge: PASSED -> pass
    expect(badges[0].getAttribute("variant")).toBe("pass");
    expect(badges[0].getAttribute("label")).toBe("PASSED");

    // Second badge: WARNING -> warn
    expect(badges[1].getAttribute("variant")).toBe("warn");
    expect(badges[1].getAttribute("label")).toBe("WARNING");

    // Third badge: FAILED -> fail
    expect(badges[2].getAttribute("variant")).toBe("fail");
    expect(badges[2].getAttribute("label")).toBe("FAILED");

    // Fourth badge: no status -> PENDING (skip palette)
    expect(badges[3].getAttribute("variant")).toBe("skip");
    expect(badges[3].getAttribute("label")).toBe("PENDING");

    // R28: badges for modules with a test instance are wrapped in an
    // anchor that links to the test's log page. The fourth module has
    // no instance, so its badge is unwrapped.
    const statusLinks = canvasElement.querySelectorAll('[data-testid="module-status-link"]');
    expect(statusLinks.length).toBe(3);
    expect(statusLinks[0].getAttribute("href")).toContain("log-detail.html?log=test-inst-001");
    expect(statusLinks[2].getAttribute("href")).toContain("log-detail.html?log=test-inst-003");

    // The link wraps the badge; the badge itself is unchanged in shape.
    expect(statusLinks[0].querySelector("cts-badge")).toBe(badges[0]);

    // The fourth badge is rendered without a wrapping anchor.
    expect(badges[3].closest('[data-testid="module-status-link"]')).toBeNull();

    // Test IDs rendered
    expect(canvas.getByText("test-inst-001")).toBeInTheDocument();
    expect(canvas.getByText("test-inst-002")).toBeInTheDocument();
    expect(canvas.getByText("test-inst-003")).toBeInTheDocument();

    // Module with no instance shows NONE in the test-ID slot of .desc .mono.
    // After U17 the per-row meta lives in a single .desc line ("Variant · Test
    // ID: <mono>"), so we find the mono spans and count the ones reading
    // "NONE".
    const noneTexts = Array.from(canvasElement.querySelectorAll(".module-row .desc .mono")).filter(
      (el) => el.textContent.trim() === "NONE",
    );
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

    // testSummary is exposed via a cts-tooltip wrapper (not a native `title=`),
    // and the help-icon trigger sits inside an inline-flex .nameLine row so
    // it stays optically centred next to the bold module name. The icon must
    // be focusable for keyboard users (cts-tooltip listens for focusin).
    const helpTooltips = canvasElement.querySelectorAll("cts-tooltip.help");
    expect(helpTooltips.length).toBe(MODULES_WITH_STATUS.length);
    expect(helpTooltips[0].getAttribute("content")).toMatch(/Verify basic OpenID Connect/);
    const firstHelpIcon = helpTooltips[0].querySelector(".help-icon");
    expect(firstHelpIcon).toBeTruthy();
    expect(firstHelpIcon.getAttribute("size")).toBe("16");
    expect(firstHelpIcon.getAttribute("tabindex")).toBe("0");
    expect(firstHelpIcon.hasAttribute("title")).toBe(false);
    expect(firstHelpIcon.closest(".nameLine")).toBeTruthy();
  },
};

export const ModulesRunTest = {
  render: () => html`
    <cts-plan-modules .modules=${MODULES_WITH_STATUS} plan-id="plan-abc-123"></cts-plan-modules>
  `,
  async play({ canvasElement }) {
    const spy = fn();
    canvasElement.addEventListener("cts-run-test", spy);

    // Click the inner native button — userEvent.click on the cts-button host
    // doesn't reach the inner @click handler.
    const runBtn = canvasElement.querySelector('[data-testid="run-test-btn"] button');
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

/**
 * R23 (Unit 3): the "Delete plan" trigger sits in the action rail next to
 * the other plan actions. It must remain discoverable but no longer use
 * the prominent destructive variant — that visual emphasis is reserved
 * for the destructive confirm button inside the panel itself.
 */
export const ActionsDeleteVariant = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions> `,
  async play({ canvasElement }) {
    const deleteHost = canvasElement.querySelector('[data-testid="delete-plan-btn"]');
    expect(deleteHost).toBeTruthy();

    // Trigger is no longer the destructive variant; the inner button keeps
    // the ghost styling so it sits visually with the secondary actions.
    expect(deleteHost?.getAttribute("variant")).toBe("ghost");

    // Open the confirm panel.
    await userEvent.click(innerButton(canvasElement, "delete-plan-btn"));
    await waitFor(() => {
      expect(canvasElement.querySelector('[data-testid="delete-confirm-panel"]')).toBeTruthy();
    });

    // The confirmation step keeps the destructive variant so the
    // irreversible action is still visually distinct at the moment the
    // user makes the final commitment.
    const confirmHost = canvasElement.querySelector(".confirm-delete-btn");
    expect(confirmHost?.getAttribute("variant")).toBe("danger");
  },
};

/**
 * R26 (Unit 3): the "Publish for certification" button is hidden by
 * default. The host page flips `can-certify` once it has confirmed at
 * least one FINISHED test exists with no FAILED result.
 */
export const ActionsCertifyHiddenByDefault = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions> `,
  async play({ canvasElement }) {
    const certifyBtn = canvasElement.querySelector('[data-testid="certify-btn"]');
    expect(certifyBtn).toBeNull();

    // Other actions still render so the rail is not empty.
    const privateLinkBtn = canvasElement.querySelector('[data-testid="private-link-btn"]');
    expect(privateLinkBtn).toBeTruthy();
  },
};

export const ActionsCertifyVisibleWhenCanCertify = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL} can-certify></cts-plan-actions> `,
  async play({ canvasElement }) {
    const certifyBtn = canvasElement.querySelector('[data-testid="certify-btn"]');
    expect(certifyBtn).toBeTruthy();
    expect(certifyBtn?.textContent).toContain("Publish for certification");

    // The certify event fires when the inner button is clicked.
    const spy = fn();
    canvasElement.addEventListener("cts-certify", spy);
    await userEvent.click(innerButton(canvasElement, "certify-btn"));
    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy.mock.calls[0][0].detail.planId).toBe("plan-abc-123");
  },
};

export const ActionsViewConfig = {
  render: () => html` <cts-plan-actions .plan=${PLAN_WITH_CONFIG}></cts-plan-actions> `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Config panel not visible initially
    let configPanel = canvasElement.querySelector('[data-testid="config-panel"]');
    expect(configPanel).toBeNull();

    // Click View configuration button (target the inner <button>)
    await userEvent.click(innerButton(canvasElement, "view-config-btn"));

    // Config panel should now be visible
    await waitFor(() => {
      configPanel = canvasElement.querySelector('[data-testid="config-panel"]');
      expect(configPanel).toBeTruthy();
    });

    // JSON content displayed in the Monaco-backed read-only editor.
    // The editor is the only way the plan reaches the user — assert via
    // its `.value` property; Monaco's text content is virtualised so
    // `textContent` may not contain the full JSON until the user scrolls.
    // `whenReady()` resolves regardless of whether Monaco mounted or the
    // fallback textarea took over; both expose `.value` identically.
    const configJson = /** @type {any} */ (
      await waitFor(() => {
        const el = canvasElement.querySelector("cts-json-editor.config-json");
        if (!el) throw new Error("cts-json-editor.config-json not yet attached");
        return el;
      })
    );
    await configJson.whenReady();
    expect(configJson.getAttribute("readonly")).not.toBeNull();
    expect(configJson.value).toContain("server.issuer");
    expect(configJson.value).toContain("https://op.example.com");
    expect(configJson.value).toContain("client.client_id");

    // Plan ID displayed
    expect(canvas.getByText("plan-abc-123")).toBeInTheDocument();
  },
};

export const ActionsPrivateLink = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions> `,
  async play({ canvasElement }) {
    // Private link panel not visible initially
    let panel = canvasElement.querySelector('[data-testid="private-link-panel"]');
    expect(panel).toBeNull();

    // Click Private link button
    await userEvent.click(innerButton(canvasElement, "private-link-btn"));

    // Panel should now be visible
    await waitFor(() => {
      panel = canvasElement.querySelector('[data-testid="private-link-panel"]');
      expect(panel).toBeTruthy();
    });

    // Days input present with default value of 30
    const daysInput = canvasElement.querySelector("#privateLinkDays");
    expect(daysInput).toBeTruthy();
    expect(daysInput.value).toBe("30");

    // Generate button should be enabled (30 is valid). The disabled state
    // is reflected onto the inner <button> rendered by cts-button.
    const generateHost = canvasElement.querySelector(".generate-link-btn");
    expect(generateHost).toBeTruthy();
    const generateBtnInner = generateHost?.querySelector("button");
    expect(generateBtnInner).toBeTruthy();
    expect(generateBtnInner?.disabled).toBe(false);
  },
};

export const ActionsPrivateLinkValidation = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions> `,
  async play({ canvasElement }) {
    // Open private link panel (target the inner <button>)
    await userEvent.click(innerButton(canvasElement, "private-link-btn"));

    await waitFor(() => {
      const panel = canvasElement.querySelector('[data-testid="private-link-panel"]');
      expect(panel).toBeTruthy();
    });

    const daysInput = canvasElement.querySelector("#privateLinkDays");

    // Clear and type 0 (invalid). The disabled state is reflected onto the
    // inner <button> rendered by cts-button — the host doesn't carry it.
    await userEvent.clear(daysInput);
    await userEvent.type(daysInput, "0");

    await waitFor(() => {
      const inner = canvasElement.querySelector(".generate-link-btn button");
      expect(inner?.disabled).toBe(true);
    });

    // Clear and type 1001 (invalid)
    await userEvent.clear(daysInput);
    await userEvent.type(daysInput, "1001");

    await waitFor(() => {
      const inner = canvasElement.querySelector(".generate-link-btn button");
      expect(inner?.disabled).toBe(true);
    });

    // Clear and type 500 (valid)
    await userEvent.clear(daysInput);
    await userEvent.type(daysInput, "500");

    await waitFor(() => {
      const inner = canvasElement.querySelector(".generate-link-btn button");
      expect(inner?.disabled).toBe(false);
    });
  },
};

export const ActionsDeletePlan = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions> `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Delete confirm not visible initially
    let confirmPanel = canvasElement.querySelector('[data-testid="delete-confirm-panel"]');
    expect(confirmPanel).toBeNull();

    // Click Delete plan button (target the inner <button>)
    await userEvent.click(innerButton(canvasElement, "delete-plan-btn"));

    // Confirm panel should appear
    await waitFor(() => {
      confirmPanel = canvasElement.querySelector('[data-testid="delete-confirm-panel"]');
      expect(confirmPanel).toBeTruthy();
    });

    // Warning text present
    expect(canvas.getByText(/permanently and irrevocably/)).toBeInTheDocument();
    expect(canvas.getByText(/cannot be undone/)).toBeInTheDocument();

    // Confirm delete button present (cts-button host carries the class).
    const confirmBtnHost = canvasElement.querySelector(".confirm-delete-btn");
    expect(confirmBtnHost).toBeTruthy();
    expect(confirmBtnHost?.textContent?.trim()).toBe("Delete plan");
    const confirmBtn = confirmBtnHost?.querySelector("button");
    expect(confirmBtn).toBeTruthy();

    // Cancel button present (no testid — find by inner button text).
    const innerButtons = confirmPanel ? Array.from(confirmPanel.querySelectorAll("button")) : [];
    const cancelBtn = innerButtons.find((b) => (b.textContent || "").trim() === "Cancel");
    expect(cancelBtn).toBeTruthy();

    // Verify cts-delete-plan event fires on confirm
    const spy = fn();
    canvasElement.addEventListener("cts-delete-plan", spy);
    if (!confirmBtn) throw new Error("confirm-delete-btn inner button missing");
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
    // Edit configuration should NOT be visible (readonly)
    const editBtn = canvasElement.querySelector('[data-testid="edit-config-btn"]');
    expect(editBtn).toBeNull();

    // Delete plan should NOT be visible (immutable makes it hidden since readonly)
    const deleteBtn = canvasElement.querySelector('[data-testid="delete-plan-btn"]');
    expect(deleteBtn).toBeNull();

    // Make Mutable should be visible (admin + immutable)
    const mutableBtnHost = canvasElement.querySelector('[data-testid="make-mutable-btn"]');
    expect(mutableBtnHost).toBeTruthy();
    expect(mutableBtnHost?.textContent).toContain("Make plan Mutable");

    // Verify event fires on click (target the inner <button>)
    const spy = fn();
    canvasElement.addEventListener("cts-make-mutable", spy);
    await userEvent.click(innerButton(canvasElement, "make-mutable-btn"));

    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy.mock.calls[0][0].detail.planId).toBe("plan-immutable-001");

    // View configuration should still be available
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
    const unpublishBtnHost = canvasElement.querySelector('[data-testid="unpublish-btn"]');
    expect(unpublishBtnHost).toBeTruthy();
    expect(unpublishBtnHost?.textContent).toContain("Unpublish");

    // Publish summary/everything should NOT be visible (already published)
    const publishSummaryBtn = canvasElement.querySelector('[data-testid="publish-summary-btn"]');
    expect(publishSummaryBtn).toBeNull();
    const publishEverythingBtn = canvasElement.querySelector(
      '[data-testid="publish-everything-btn"]',
    );
    expect(publishEverythingBtn).toBeNull();

    // Public link should be visible (rendered as a cts-link-button → <a>)
    expect(canvas.getByText("Public link")).toBeInTheDocument();

    // Verify unpublish event (target the inner <button>)
    const spy = fn();
    canvasElement.addEventListener("cts-unpublish", spy);
    await userEvent.click(innerButton(canvasElement, "unpublish-btn"));

    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy.mock.calls[0][0].detail.planId).toBe("plan-abc-123");

    // Download all should be visible (admin)
    const downloadAllBtn = canvasElement.querySelector('[data-testid="download-all-btn"]');
    expect(downloadAllBtn).toBeTruthy();
  },
};

export const ActionsGenerateLinkResult = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions> `,
  async play({ canvasElement }) {
    // Open private link panel
    await userEvent.click(innerButton(canvasElement, "private-link-btn"));

    await waitFor(() => {
      const panel = canvasElement.querySelector('[data-testid="private-link-panel"]');
      expect(panel).toBeTruthy();
    });

    // Set days to 7 (valid)
    const daysInput = canvasElement.querySelector("#privateLinkDays");
    await userEvent.clear(daysInput);
    await userEvent.type(daysInput, "7");

    // Listen for the generate event before clicking
    const spy = fn();
    canvasElement.addEventListener("cts-generate-private-link", spy);

    // Click Generate (target the inner <button>; cts-button host carries
    // the .generate-link-btn class).
    const generateInner = canvasElement.querySelector(".generate-link-btn button");
    await waitFor(() => expect(generateInner?.disabled).toBe(false));
    if (!generateInner) throw new Error("generate-link-btn inner <button> missing");
    await userEvent.click(generateInner);

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
      const result = canvasElement.querySelector('[data-testid="private-link-result"]');
      expect(result).toBeTruthy();
      expect(result.textContent).toContain(generatedUrl);
    });
  },
};

export const ActionsDeletePlanCancel = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions> `,
  async play({ canvasElement }) {
    // Listen for delete event before any clicks — must NOT fire on cancel
    const deleteSpy = fn();
    canvasElement.addEventListener("cts-delete-plan", deleteSpy);

    // Open delete confirm panel (target the inner <button>)
    await userEvent.click(innerButton(canvasElement, "delete-plan-btn"));

    /** @type {Element | null | undefined} */
    let confirmPanel;
    await waitFor(() => {
      confirmPanel = canvasElement.querySelector('[data-testid="delete-confirm-panel"]');
      expect(confirmPanel).toBeTruthy();
    });
    if (!confirmPanel) throw new Error("delete-confirm-panel did not appear");

    // Click Cancel — find the inner <button> whose visible text is "Cancel".
    const cancelBtn = Array.from(confirmPanel.querySelectorAll("button")).find(
      (b) => (b.textContent || "").trim() === "Cancel",
    );
    expect(cancelBtn).toBeTruthy();
    if (!cancelBtn) throw new Error("Cancel button not found");
    await userEvent.click(cancelBtn);

    // Panel should close
    await waitFor(() => {
      expect(canvasElement.querySelector('[data-testid="delete-confirm-panel"]')).toBeNull();
    });

    // No delete event fired
    expect(deleteSpy).not.toHaveBeenCalled();

    // Clicking the delete-plan trigger again re-opens the panel cleanly
    // (regression guard against state leaking across cancel + reopen).
    await userEvent.click(innerButton(canvasElement, "delete-plan-btn"));
    await waitFor(() => {
      expect(canvasElement.querySelector('[data-testid="delete-confirm-panel"]')).toBeTruthy();
    });
    expect(deleteSpy).not.toHaveBeenCalled();
  },
};

export const ActionsCopyConfig = {
  render: () => html` <cts-plan-actions .plan=${PLAN_WITH_CONFIG}></cts-plan-actions> `,
  async play({ canvasElement }) {
    // Spy on navigator.clipboard.writeText. Headless Chromium denies real
    // clipboard writes (NotAllowedError + "document not focused"), so the
    // spy both observes the call and replaces the implementation.
    // restoreMocks: true in vitest.config.js handles teardown.
    const mockWriteText = spyOn(navigator.clipboard, "writeText").mockResolvedValue();

    // Open the config panel (target the inner <button>)
    await userEvent.click(innerButton(canvasElement, "view-config-btn"));

    await waitFor(() => {
      const panel = canvasElement.querySelector('[data-testid="config-panel"]');
      expect(panel).toBeTruthy();
    });

    // Click the Copy button inside the panel (cts-button host carries
    // the .copy-config-btn class; click the inner <button>).
    const copyHost = canvasElement.querySelector(".copy-config-btn");
    expect(copyHost).toBeTruthy();
    const copyInner = copyHost?.querySelector("button");
    expect(copyInner).toBeTruthy();
    if (!copyInner) throw new Error("copy-config-btn inner <button> missing");
    await userEvent.click(copyInner);

    // Clipboard should have been called once with pretty-printed JSON.
    // _handleCopyConfig is async and awaits writeText, so wait for the
    // spy rather than asserting synchronously after the click.
    await waitFor(() => {
      expect(mockWriteText).toHaveBeenCalledOnce();
    });
    const written = mockWriteText.mock.calls[0][0];
    expect(written).toBe(JSON.stringify(PLAN_WITH_CONFIG.config, null, 4));
  },
};

export const ActionsCopyConfigClipboardFailure = {
  render: () => html` <cts-plan-actions .plan=${PLAN_WITH_CONFIG}></cts-plan-actions> `,
  async play({ canvasElement }) {
    // Spy with a rejecting implementation — simulates permissions-denied /
    // insecure-context. restoreMocks: true in vitest.config.js auto-restores
    // the original method after the test.
    const writeTextSpy = spyOn(navigator.clipboard, "writeText").mockRejectedValue(
      new Error("permission denied"),
    );

    // Open the config panel (target the inner <button>)
    await userEvent.click(innerButton(canvasElement, "view-config-btn"));

    await waitFor(() => {
      expect(canvasElement.querySelector('[data-testid="config-panel"]')).toBeTruthy();
    });

    const copyInner = canvasElement.querySelector(".copy-config-btn button");
    if (!copyInner) throw new Error("copy-config-btn inner <button> missing");
    await userEvent.click(copyInner);

    // Anchor the failure-path assertion on the spy first (testing-reviewer
    // T6): a regression that silently no-ops the click could still render
    // unrelated feedback, so we want writeText itself as the ground truth
    // before checking the user-visible copy-failed message.
    await waitFor(() => {
      expect(writeTextSpy).toHaveBeenCalledOnce();
    });

    // Failure feedback should render in the same flex container as Copy,
    // announced politely so SRs read it without interrupting.
    await waitFor(() => {
      const feedback = canvasElement.querySelector('[data-testid="copy-feedback"]');
      expect(feedback).toBeTruthy();
      expect(feedback?.textContent).toContain("Copy failed");
      expect(feedback?.getAttribute("aria-live")).toBe("polite");
    });
  },
};

export {};
