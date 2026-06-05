import { html } from "lit";
import { expect, within, waitFor, userEvent, fn, spyOn } from "storybook/test";
import {
  MOCK_PLAN_DETAIL,
  MOCK_PLAN_PUBLISHED,
  MOCK_MODULES_WITH_STATUS,
  MOCK_MODULES_FAILED_WITHOUT_REF,
  MOCK_MODULES_WRONG_REF_PLACEMENT,
} from "@fixtures/mock-test-data.js";
import "./cts-plan-header.js";
import "./cts-plan-modules.js";
import "./cts-plan-actions.js";
import "./cts-modal.js";
import "./cts-button.js";

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
 * lede paragraph. The header conditionally suppresses the .planLede
 * element when `plan.description` is falsy.
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

    // The description lede is suppressed entirely (no empty subtitle slot)
    const descRow = canvasElement.querySelector('[data-testid="description-row"]');
    expect(descRow).toBeNull();
    expect(canvasElement.querySelector(".planLede")).toBeNull();
  },
};

/**
 * R9 (Unit 5): when the user set a description while scheduling the plan,
 * it surfaces as a prominent lede paragraph between the plan title and
 * the metadata grid — not buried inside the `<dl>`. The lede uses larger
 * prose typography so the human-readable identifier the user typed is
 * the first thing the eye lands on after the title.
 */
export const PlanHeaderDescriptionLede = {
  render: () =>
    html`<cts-plan-header
      .plan=${{
        ...MOCK_PLAN_DETAIL,
        description: "BixeLab partner conformance run #5 for Acme Corp",
      }}
    ></cts-plan-header>`,
  async play({ canvasElement }) {
    const lede = /** @type {HTMLElement | null} */ (
      canvasElement.querySelector('[data-testid="description-row"]')
    );
    if (!lede) throw new Error("description lede did not render");

    // The lede is a <p>, not a dt/dd pair — it sits outside the meta grid.
    expect(lede.tagName).toBe("P");
    expect(lede.classList.contains("planLede")).toBe(true);
    expect(lede.textContent).toContain("BixeLab partner conformance run #5 for Acme Corp");

    // DOM order: title comes before the lede, lede comes before the meta grid.
    // documentPosition asserts visual reading order without coupling to
    // pixel coordinates that depend on canvas width.
    const title = canvasElement.querySelector(".planTitle");
    const meta = canvasElement.querySelector(".planMeta");
    if (!title || !meta) throw new Error("title or meta grid did not render");
    const FOLLOWING = Node.DOCUMENT_POSITION_FOLLOWING;
    expect(title.compareDocumentPosition(lede) & FOLLOWING).toBeTruthy();
    expect(lede.compareDocumentPosition(meta) & FOLLOWING).toBeTruthy();

    // Typography: lede prose is larger than the metadata text. --fs-16
    // resolves to 16 px in the OIDF token sheet; meta dd resolves to
    // --fs-13 (13 px). The exact px values are an implementation detail,
    // so assert the relationship rather than the literal pixels.
    const ledeFs = parseFloat(getComputedStyle(lede).fontSize);
    const sampleDd = canvasElement.querySelector(".planMeta dd");
    if (!sampleDd) throw new Error(".planMeta dd did not render");
    const ddFs = parseFloat(getComputedStyle(sampleDd).fontSize);
    expect(ledeFs).toBeGreaterThan(ddFs);
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

    // R28 deep-link: the FAILED row carries `firstFailureRef: "LOG-0042"`,
    // so its href ends with the fragment and its aria-label switches to
    // the "Jump to first failure" form. Non-FAILED rows do NOT carry the
    // field, so their hrefs have no fragment and the original aria-label
    // shape is preserved (R7 two-state contract).
    expect(statusLinks[2].getAttribute("href")).toBe("log-detail.html?log=test-inst-003#LOG-0042");
    expect(statusLinks[2].getAttribute("aria-label")).toBe(
      "Jump to first failure in logs for oidcc-ensure-redirect-uri-in-authorization-request",
    );
    expect(statusLinks[0].getAttribute("href")).toBe("log-detail.html?log=test-inst-001");
    expect(statusLinks[0].getAttribute("aria-label")).toBe("View logs for oidcc-server (PASSED)");
    expect(statusLinks[1].getAttribute("href")).toBe("log-detail.html?log=test-inst-002");
    expect(statusLinks[1].getAttribute("aria-label")).toBe(
      "View logs for oidcc-server-rotate-keys (WARNING)",
    );

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
  async play({ canvasElement, step }) {
    const spy = fn();
    canvasElement.addEventListener("cts-run-test", spy);

    await step("clicking Run Test fires cts-run-test", async () => {
      // Click the inner native button — userEvent.click on the cts-button host
      // doesn't reach the inner @click handler.
      const runBtn = canvasElement.querySelector('[data-testid="run-test-btn"] button');
      expect(runBtn).toBeTruthy();
      await userEvent.click(runBtn);
    });

    await step("event fires with correct detail", async () => {
      expect(spy).toHaveBeenCalledTimes(1);
      const detail = spy.mock.calls[0][0].detail;
      expect(detail.testModule).toBe("oidcc-server");
      expect(detail.variant).toEqual({
        client_auth_type: "client_secret_basic",
        response_type: "code",
      });
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

/**
 * R28 deep-link follow-on, fail-soft branch (R1, AE3): a FAILED row whose
 * `firstFailureRef` has not resolved (the per-FAILED log fetch is still
 * pending, errored, or returned no FAILURE entry). The lozenge must
 * fall back to the R28 top-of-log href — never a stray `#` with no
 * fragment — and the aria-label must keep the original "View logs"
 * shape so the screen-reader announcement matches the actual landing
 * (top of log, not mid-page).
 */
export const ModulesFailedWithoutRef = {
  render: () => html`
    <cts-plan-modules
      .modules=${MOCK_MODULES_FAILED_WITHOUT_REF}
      plan-id="plan-abc-123"
    ></cts-plan-modules>
  `,
  async play({ canvasElement }) {
    const statusLinks = canvasElement.querySelectorAll('[data-testid="module-status-link"]');
    expect(statusLinks.length).toBe(2);

    // PASSED row is unchanged from R28-current behaviour.
    expect(statusLinks[0].getAttribute("href")).toBe("log-detail.html?log=test-inst-001");
    expect(statusLinks[0].getAttribute("aria-label")).toBe("View logs for oidcc-server (PASSED)");

    // FAILED row falls back to the R28 top-of-log href — no fragment,
    // no broken `#`, no jump aria-label. This is the "data still
    // loading" / "fetch failed" state on the live page.
    const failedHref = statusLinks[1].getAttribute("href");
    expect(failedHref).toBe("log-detail.html?log=test-inst-003");
    expect(failedHref?.includes("#")).toBe(false);
    expect(statusLinks[1].getAttribute("aria-label")).toBe(
      "View logs for oidcc-ensure-redirect-uri-in-authorization-request (FAILED)",
    );
  },
};

/**
 * R28 deep-link follow-on, defensive result-gate (R2): a fixture that
 * mistakenly carries `firstFailureRef` on a non-FAILED row must NOT
 * produce a deep-link. The component's gate is on
 * `mod.result === "FAILED"`, not on the field's mere presence; this
 * story pins that gate so a future refactor can't accidentally relax
 * it (e.g. by checking only `firstFailureRef && ...`).
 */
export const ModulesWrongRefPlacement = {
  render: () => html`
    <cts-plan-modules
      .modules=${MOCK_MODULES_WRONG_REF_PLACEMENT}
      plan-id="plan-abc-123"
    ></cts-plan-modules>
  `,
  async play({ canvasElement }) {
    const statusLinks = canvasElement.querySelectorAll('[data-testid="module-status-link"]');
    expect(statusLinks.length).toBe(3);

    // None of the three rows produce a fragment, despite each carrying
    // a non-empty firstFailureRef. The result-gate keeps the deep-link
    // contract honest: only FAILED rows ever deep-link. Loop also
    // verifies the aria-label keeps the original "View logs ..." shape.
    for (const link of Array.from(statusLinks)) {
      const href = link.getAttribute("href") || "";
      expect(href.includes("#")).toBe(false);
      const ariaLabel = link.getAttribute("aria-label") || "";
      expect(ariaLabel.startsWith("View logs for ")).toBe(true);
      expect(ariaLabel.startsWith("Jump to first failure")).toBe(false);
    }
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
  async play({ canvasElement, step }) {
    await step("trigger uses the ghost variant, not destructive", async () => {
      const deleteHost = canvasElement.querySelector('[data-testid="delete-plan-btn"]');
      expect(deleteHost).toBeTruthy();

      // Trigger is no longer the destructive variant; the inner button keeps
      // the ghost styling so it sits visually with the secondary actions.
      expect(deleteHost?.getAttribute("variant")).toBe("ghost");
    });

    await step("opening the confirm panel exposes a destructive confirm button", async () => {
      await userEvent.click(innerButton(canvasElement, "delete-plan-btn"));
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="delete-confirm-panel"]')).toBeTruthy();
      });

      // The confirmation step keeps the destructive variant so the
      // irreversible action is still visually distinct at the moment the
      // user makes the final commitment.
      const confirmHost = canvasElement.querySelector(".confirm-delete-btn");
      expect(confirmHost?.getAttribute("variant")).toBe("danger");
    });
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
  async play({ canvasElement, step }) {
    await step("certify button is visible", async () => {
      const certifyBtn = canvasElement.querySelector('[data-testid="certify-btn"]');
      expect(certifyBtn).toBeTruthy();
      expect(certifyBtn?.textContent).toContain("Publish for certification");
    });

    await step("clicking it fires cts-certify with the plan id", async () => {
      const spy = fn();
      canvasElement.addEventListener("cts-certify", spy);
      await userEvent.click(innerButton(canvasElement, "certify-btn"));
      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy.mock.calls[0][0].detail.planId).toBe("plan-abc-123");
    });
  },
};

export const ActionsViewConfig = {
  render: () => html` <cts-plan-actions .plan=${PLAN_WITH_CONFIG}></cts-plan-actions> `,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("config panel is hidden until requested", async () => {
      const configPanel = canvasElement.querySelector('[data-testid="config-panel"]');
      expect(configPanel).toBeNull();
    });

    await step("clicking View configuration opens the panel", async () => {
      // Target the inner <button>.
      await userEvent.click(innerButton(canvasElement, "view-config-btn"));
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="config-panel"]')).toBeTruthy();
      });
    });

    await step("editor renders the plan config as read-only JSON", async () => {
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
    });

    await step("plan ID is displayed", async () => {
      expect(canvas.getByText("plan-abc-123")).toBeInTheDocument();
    });
  },
};

export const ActionsPrivateLink = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions> `,
  async play({ canvasElement, step }) {
    await step("private link panel is hidden initially", async () => {
      const panel = canvasElement.querySelector('[data-testid="private-link-panel"]');
      expect(panel).toBeNull();
    });

    await step("clicking Private link opens the panel", async () => {
      await userEvent.click(innerButton(canvasElement, "private-link-btn"));
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="private-link-panel"]')).toBeTruthy();
      });
    });

    await step("days input defaults to 30 and Generate is enabled", async () => {
      // Days input present with default value of 30.
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
    });
  },
};

export const ActionsPrivateLinkValidation = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions> `,
  async play({ canvasElement, step }) {
    await step("open the private link panel", async () => {
      // Target the inner <button>.
      await userEvent.click(innerButton(canvasElement, "private-link-btn"));
      await waitFor(() => {
        const panel = canvasElement.querySelector('[data-testid="private-link-panel"]');
        expect(panel).toBeTruthy();
      });
    });

    const daysInput = canvasElement.querySelector("#privateLinkDays");

    await step("0 days is invalid → Generate disabled", async () => {
      // The disabled state is reflected onto the inner <button> rendered by
      // cts-button — the host doesn't carry it.
      await userEvent.clear(daysInput);
      await userEvent.type(daysInput, "0");
      await waitFor(() => {
        const inner = canvasElement.querySelector(".generate-link-btn button");
        expect(inner?.disabled).toBe(true);
      });
    });

    await step("1001 days is invalid → Generate disabled", async () => {
      await userEvent.clear(daysInput);
      await userEvent.type(daysInput, "1001");
      await waitFor(() => {
        const inner = canvasElement.querySelector(".generate-link-btn button");
        expect(inner?.disabled).toBe(true);
      });
    });

    await step("500 days is valid → Generate enabled", async () => {
      await userEvent.clear(daysInput);
      await userEvent.type(daysInput, "500");
      await waitFor(() => {
        const inner = canvasElement.querySelector(".generate-link-btn button");
        expect(inner?.disabled).toBe(false);
      });
    });
  },
};

export const ActionsDeletePlan = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions> `,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("delete confirm panel is hidden initially", async () => {
      const confirmPanel = canvasElement.querySelector('[data-testid="delete-confirm-panel"]');
      expect(confirmPanel).toBeNull();
    });

    await step("clicking Delete plan opens the confirm panel", async () => {
      // Target the inner <button>.
      await userEvent.click(innerButton(canvasElement, "delete-plan-btn"));
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="delete-confirm-panel"]')).toBeTruthy();
      });
    });

    await step("confirm panel shows warning text and Delete/Cancel buttons", async () => {
      // Warning text present.
      expect(canvas.getByText(/permanently and irrevocably/)).toBeInTheDocument();
      expect(canvas.getByText(/cannot be undone/)).toBeInTheDocument();

      // Confirm delete button present (cts-button host carries the class).
      const confirmBtnHost = canvasElement.querySelector(".confirm-delete-btn");
      expect(confirmBtnHost).toBeTruthy();
      expect(confirmBtnHost?.textContent?.trim()).toBe("Delete plan");
      expect(confirmBtnHost?.querySelector("button")).toBeTruthy();

      // Cancel button present (no testid — find by inner button text).
      const confirmPanel = canvasElement.querySelector('[data-testid="delete-confirm-panel"]');
      const innerButtons = confirmPanel ? Array.from(confirmPanel.querySelectorAll("button")) : [];
      const cancelBtn = innerButtons.find((b) => (b.textContent || "").trim() === "Cancel");
      expect(cancelBtn).toBeTruthy();
    });

    await step("confirming fires cts-delete-plan with the plan id", async () => {
      const spy = fn();
      canvasElement.addEventListener("cts-delete-plan", spy);
      const confirmBtn = canvasElement.querySelector(".confirm-delete-btn button");
      if (!confirmBtn) throw new Error("confirm-delete-btn inner button missing");
      await userEvent.click(confirmBtn);

      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy.mock.calls[0][0].detail.planId).toBe("plan-abc-123");
    });
  },
};

export const ActionsImmutablePlan = {
  render: () => html`
    <cts-plan-actions .plan=${PLAN_IMMUTABLE} is-admin is-readonly></cts-plan-actions>
  `,
  async play({ canvasElement, step }) {
    await step("readonly hides Edit configuration and Delete plan", async () => {
      // Edit configuration should NOT be visible (readonly).
      const editBtn = canvasElement.querySelector('[data-testid="edit-config-btn"]');
      expect(editBtn).toBeNull();

      // Delete plan should NOT be visible (immutable makes it hidden since readonly).
      const deleteBtn = canvasElement.querySelector('[data-testid="delete-plan-btn"]');
      expect(deleteBtn).toBeNull();
    });

    await step("Make Mutable is visible for admin + immutable", async () => {
      const mutableBtnHost = canvasElement.querySelector('[data-testid="make-mutable-btn"]');
      expect(mutableBtnHost).toBeTruthy();
      expect(mutableBtnHost?.textContent).toContain("Make plan Mutable");
    });

    await step("clicking Make Mutable fires cts-make-mutable", async () => {
      // Target the inner <button>.
      const spy = fn();
      canvasElement.addEventListener("cts-make-mutable", spy);
      await userEvent.click(innerButton(canvasElement, "make-mutable-btn"));

      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy.mock.calls[0][0].detail.planId).toBe("plan-immutable-001");
    });

    await step("View configuration remains available", async () => {
      const viewConfigBtn = canvasElement.querySelector('[data-testid="view-config-btn"]');
      expect(viewConfigBtn).toBeTruthy();
    });
  },
};

export const ActionsPublishedPlan = {
  render: () => html`
    <cts-plan-actions
      .plan=${{ ...MOCK_PLAN_DETAIL, publish: "everything" }}
      is-admin
    ></cts-plan-actions>
  `,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("Unpublish is visible for an already-published plan", async () => {
      // Admin + published + not readonly.
      const unpublishBtnHost = canvasElement.querySelector('[data-testid="unpublish-btn"]');
      expect(unpublishBtnHost).toBeTruthy();
      expect(unpublishBtnHost?.textContent).toContain("Unpublish");
    });

    await step("Publish summary/everything are hidden once published", async () => {
      const publishSummaryBtn = canvasElement.querySelector('[data-testid="publish-summary-btn"]');
      expect(publishSummaryBtn).toBeNull();
      const publishEverythingBtn = canvasElement.querySelector(
        '[data-testid="publish-everything-btn"]',
      );
      expect(publishEverythingBtn).toBeNull();

      // Public link should be visible (rendered as a cts-link-button → <a>).
      expect(canvas.getByText("Public link")).toBeInTheDocument();
    });

    await step("clicking Unpublish fires cts-unpublish", async () => {
      // Target the inner <button>.
      const spy = fn();
      canvasElement.addEventListener("cts-unpublish", spy);
      await userEvent.click(innerButton(canvasElement, "unpublish-btn"));

      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy.mock.calls[0][0].detail.planId).toBe("plan-abc-123");
    });

    await step("Download all is visible for admin", async () => {
      const downloadAllBtn = canvasElement.querySelector('[data-testid="download-all-btn"]');
      expect(downloadAllBtn).toBeTruthy();
    });
  },
};

export const ActionsGenerateLinkResult = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions> `,
  async play({ canvasElement, step }) {
    await step("open the panel and set days to 7", async () => {
      await userEvent.click(innerButton(canvasElement, "private-link-btn"));
      await waitFor(() => {
        const panel = canvasElement.querySelector('[data-testid="private-link-panel"]');
        expect(panel).toBeTruthy();
      });

      const daysInput = canvasElement.querySelector("#privateLinkDays");
      await userEvent.clear(daysInput);
      await userEvent.type(daysInput, "7");
    });

    await step("clicking Generate fires cts-generate-private-link", async () => {
      // Listen for the generate event before clicking.
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
    });

    await step("parent wires the generated URL back into the component", async () => {
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
    });
  },
};

export const ActionsDeletePlanCancel = {
  render: () => html` <cts-plan-actions .plan=${MOCK_PLAN_DETAIL}></cts-plan-actions> `,
  async play({ canvasElement, step }) {
    // Listen for delete event before any clicks — must NOT fire on cancel.
    const deleteSpy = fn();
    canvasElement.addEventListener("cts-delete-plan", deleteSpy);

    await step("open the delete confirm panel", async () => {
      // Target the inner <button>.
      await userEvent.click(innerButton(canvasElement, "delete-plan-btn"));
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="delete-confirm-panel"]')).toBeTruthy();
      });
    });

    await step("Cancel closes the panel without firing cts-delete-plan", async () => {
      const confirmPanel = canvasElement.querySelector('[data-testid="delete-confirm-panel"]');
      if (!confirmPanel) throw new Error("delete-confirm-panel did not appear");

      // Find the inner <button> whose visible text is "Cancel".
      const cancelBtn = Array.from(confirmPanel.querySelectorAll("button")).find(
        (b) => (b.textContent || "").trim() === "Cancel",
      );
      expect(cancelBtn).toBeTruthy();
      if (!cancelBtn) throw new Error("Cancel button not found");
      await userEvent.click(cancelBtn);

      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="delete-confirm-panel"]')).toBeNull();
      });
      expect(deleteSpy).not.toHaveBeenCalled();
    });

    await step("re-clicking the trigger re-opens the panel cleanly", async () => {
      // Regression guard against state leaking across cancel + reopen.
      await userEvent.click(innerButton(canvasElement, "delete-plan-btn"));
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="delete-confirm-panel"]')).toBeTruthy();
      });
      expect(deleteSpy).not.toHaveBeenCalled();
    });
  },
};

export const ActionsCopyConfig = {
  render: () => html` <cts-plan-actions .plan=${PLAN_WITH_CONFIG}></cts-plan-actions> `,
  async play({ canvasElement, step }) {
    // Spy on navigator.clipboard.writeText. Headless Chromium denies real
    // clipboard writes (NotAllowedError + "document not focused"), so the
    // spy both observes the call and replaces the implementation.
    // restoreMocks: true in vitest.config.js handles teardown.
    const mockWriteText = spyOn(navigator.clipboard, "writeText").mockResolvedValue();

    await step("open the config panel", async () => {
      // Target the inner <button>.
      await userEvent.click(innerButton(canvasElement, "view-config-btn"));
      await waitFor(() => {
        const panel = canvasElement.querySelector('[data-testid="config-panel"]');
        expect(panel).toBeTruthy();
      });
    });

    await step("click the Copy button", async () => {
      // cts-button host carries the .copy-config-btn class; click the inner
      // <button>.
      const copyHost = canvasElement.querySelector(".copy-config-btn");
      expect(copyHost).toBeTruthy();
      const copyInner = copyHost?.querySelector("button");
      expect(copyInner).toBeTruthy();
      if (!copyInner) throw new Error("copy-config-btn inner <button> missing");
      await userEvent.click(copyInner);
    });

    await step("clipboard receives pretty-printed config JSON", async () => {
      // _handleCopyConfig is async and awaits writeText, so wait for the
      // spy rather than asserting synchronously after the click.
      await waitFor(() => {
        expect(mockWriteText).toHaveBeenCalledOnce();
      });
      const written = mockWriteText.mock.calls[0][0];
      expect(written).toBe(JSON.stringify(PLAN_WITH_CONFIG.config, null, 4));
    });
  },
};

export const ActionsCopyConfigClipboardFailure = {
  render: () => html` <cts-plan-actions .plan=${PLAN_WITH_CONFIG}></cts-plan-actions> `,
  async play({ canvasElement, step }) {
    // Spy with a rejecting implementation — simulates permissions-denied /
    // insecure-context. restoreMocks: true in vitest.config.js auto-restores
    // the original method after the test.
    const writeTextSpy = spyOn(navigator.clipboard, "writeText").mockRejectedValue(
      new Error("permission denied"),
    );

    await step("open the config panel and click Copy", async () => {
      // Target the inner <button>.
      await userEvent.click(innerButton(canvasElement, "view-config-btn"));
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="config-panel"]')).toBeTruthy();
      });

      const copyInner = canvasElement.querySelector(".copy-config-btn button");
      if (!copyInner) throw new Error("copy-config-btn inner <button> missing");
      await userEvent.click(copyInner);
    });

    await step("writeText was actually attempted", async () => {
      // Anchor the failure-path assertion on the spy first (testing-reviewer
      // T6): a regression that silently no-ops the click could still render
      // unrelated feedback, so we want writeText itself as the ground truth
      // before checking the user-visible copy-failed message.
      await waitFor(() => {
        expect(writeTextSpy).toHaveBeenCalledOnce();
      });
    });

    await step("copy-failed feedback renders and is announced politely", async () => {
      // Failure feedback should render in the same flex container as Copy,
      // announced politely so SRs read it without interrupting.
      await waitFor(() => {
        const feedback = canvasElement.querySelector('[data-testid="copy-feedback"]');
        expect(feedback).toBeTruthy();
        expect(feedback?.textContent).toContain("Copy failed");
        expect(feedback?.getAttribute("aria-live")).toBe("polite");
      });
    });
  },
};

/**
 * Certification submission package modal (plan-detail's
 * `#certificationPackageModal`), rendered here so the modal — page content
 * slotted into `<cts-modal>`, not a component — is reviewable in Storybook.
 * Styling comes from the shared `/css/cert-package.css` (linked in
 * `.storybook/preview-head.html`), the same file `plan-detail.html` loads, so
 * there is no duplicated CSS to keep in sync.
 *
 * The "Client data" upload field caps the native file control to the row width
 * (`.oidf-cert-package-form input[type="file"] { width: 100% }`) so a long
 * filename can't grow the control and give the dialog a horizontal scrollbar.
 * That overflow only reproduces in browsers that render the full filename
 * (e.g. Firefox) — open this story at `localhost:6006` in Firefox to verify
 * visually. Storybook's automated runner is Chromium, which truncates the
 * filename, so the overflow itself cannot be reproduced here.
 *
 * The play function therefore verifies what *is* deterministic in Chromium:
 * the modal opens and renders its heading and upload field, the action buttons
 * render their `cts-button` inner-button variant classes, and the shared
 * `cert-package.css` is loaded and applied to the file control
 * (`box-sizing: border-box` — only set by that stylesheet). It is a wiring +
 * rendering guard, not a Firefox-overflow regression test; the overflow fix
 * was verified cross-browser by hand (see the plan).
 *
 * The form markup mirrors `plan-detail.html` but intentionally omits the
 * production-only `#certificationPackageFormErrors`, `#certificationPackageDownloaded`,
 * and Close-button elements, which are runtime wiring the page's JS manages and
 * are not relevant to the styling this story exercises.
 */
export const CertificationSubmissionModal = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Publish for certification
      </button>
      <cts-modal heading="Prepare Certification Submission Package" size="lg" static-backdrop>
        <form class="oidf-cert-package-form">
          <div>
            <p>
              <strong
                >Clicking the "Create Certification Package" button will trigger the
                following:</strong
              >
            </p>
            <ol class="top15">
              <li>
                <strong>The test plan will be published.</strong>
                <p class="form-text">
                  This will make all keys, secrets, and all other test information publicly visible.
                </p>
              </li>
              <li class="top10">
                <strong>The test plan will be marked as immutable.</strong>
                <p class="form-text">
                  You will not be able to run tests under this plan, once you click the Create
                  Certification Package button.
                </p>
              </li>
              <li class="top10">
                <strong
                  >A zip file containing your certification submission package will be downloaded to
                  your computer.</strong
                >
                <p class="form-text">
                  Please follow
                  <a href="https://openid.net/certification/instructions/" target="instructions"
                    >the submission instructions</a
                  >
                  to complete the certification process.
                </p>
              </li>
            </ol>
            <p class="top30">
              <strong>Please upload the required files to be included the package:</strong>
            </p>
            <fieldset>
              <strong>1.</strong> <strong>Client data</strong> (For RP Tests Only)
              <p class="top10">
                <input
                  type="file"
                  id="storyClientSideData"
                  name="clientSideData"
                  accept="application/zip"
                />
              </p>
              <p class="form-text">
                Client side logs and similar additional data. Only needed for RP tests. Must be a
                zip file.
              </p>
            </fieldset>
          </div>
          <div class="oidf-cert-package-actions">
            <cts-button label="Cancel"></cts-button>
            <cts-button
              type="submit"
              variant="primary"
              label="Create Certification Package"
            ></cts-button>
          </div>
        </form>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement, step }) {
    await step("clicking the trigger opens the modal", async () => {
      const openBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(openBtn);
      const dialog = /** @type {HTMLDialogElement} */ (
        canvasElement.querySelector("dialog.oidf-modal")
      );
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("modal renders the heading and Client data upload field", async () => {
      expect(canvasElement.querySelector(".oidf-modal-title")?.textContent).toBe(
        "Prepare Certification Submission Package",
      );
      const input = canvasElement.querySelector("#storyClientSideData");
      expect(input).toBeTruthy();
    });

    await step("action buttons render their cts-button variant classes", async () => {
      // AGENTS.md §6: assert the inner <button>, not the cts-button host.
      const actionButtons = canvasElement.querySelectorAll(".oidf-cert-package-actions cts-button");
      expect(actionButtons.length).toBe(2);
      const cancelInner = actionButtons[0].querySelector("button");
      const createInner = actionButtons[1].querySelector("button");
      expect(cancelInner?.textContent?.trim()).toBe("Cancel");
      expect(cancelInner?.classList.contains("oidf-btn-secondary")).toBe(true);
      expect(createInner?.textContent?.trim()).toBe("Create Certification Package");
      expect(createInner?.classList.contains("oidf-btn-primary")).toBe(true);
    });

    await step("uploading a very long filename is accepted", async () => {
      // The exact edge case that overflows in Firefox. (Chromium truncates the
      // filename, so this documents the flow rather than reproducing overflow.)
      const input = /** @type {HTMLInputElement} */ (
        canvasElement.querySelector("#storyClientSideData")
      );
      const longName =
        "test-log-fapi2-security-profile-final-ensure-authorization-request-without-state-success-private_key_jwt-mtls-additional-client-data.zip";
      await userEvent.upload(input, new File(["dummy"], longName, { type: "application/zip" }));
      await waitFor(() => expect(input.files?.length).toBe(1));
      const files = input.files;
      if (!files) throw new Error("file input has no FileList after upload");
      expect(files[0].name).toBe(longName);
    });

    await step("shared cert-package.css is applied to the file control", async () => {
      // box-sizing:border-box is set only by that stylesheet's fix rule
      // (the UA default is content-box), so this is a non-vacuous guard that the
      // no-drift wiring works and the rule is present — it fails if the rule is
      // removed or cert-package.css stops loading. The control also fills its row
      // (display:block + width:100%). This is a wiring/rendering guard, NOT a
      // Firefox-overflow regression test: Chromium truncates the filename so the
      // control never grows here, and the overflow fix was verified cross-browser
      // by hand. offsetWidth (a layout metric) is used because the dialog's entry
      // animation transiently scales the dialog, which would skew client rects.
      const input = /** @type {HTMLInputElement} */ (
        canvasElement.querySelector("#storyClientSideData")
      );
      const style = getComputedStyle(input);
      expect(style.boxSizing).toBe("border-box");
      expect(style.display).toBe("block");
      const row = /** @type {HTMLElement} */ (input.closest(".top10") ?? input.parentElement);
      expect(Math.abs(input.offsetWidth - row.clientWidth)).toBeLessThanOrEqual(1);
    });
  },
};

export {};
