import { html } from "lit";
import { expect, within, waitFor, fn, userEvent } from "storybook/test";
import "./cts-test-nav-controls.js";

export default {
  title: "Components/cts-test-nav-controls",
  component: "cts-test-nav-controls",
};

const TEST_ID = "test-instance-001";
const PLAN_ID = "plan-instance-001";

// Plan-level progress is now the cts-plan-status segment bar (U6): one
// resolved segment per module, the "you are here" marker on the module whose
// `instances` includes `currentInstanceId`, and a "Module N of M" label the
// component derives from that match. Build a 30-module plan whose 6th module
// is the one currently being viewed (instance "i-6").
/**
 * @param {number} count - Number of plan modules to generate.
 * @param {{ currentIndex?: number }} [opts] - Which module index is the
 *   viewed one (rendered FAILED so it stands out from the PASSED siblings).
 * @returns {Array<object>} Resolved plan modules in plan order.
 */
function makeModules(count, opts = {}) {
  const currentIndex = opts.currentIndex;
  return Array.from({ length: count }, (_, i) => ({
    testModule: `mod-${i + 1}`,
    instances: [`i-${i + 1}`],
    status: "FINISHED",
    result: i === currentIndex ? "FAILED" : "PASSED",
    _statusResolved: true,
    // The page supplies each segment's navigation target; href presence makes
    // the segment a navigable link in cts-plan-status.
    href: `/log-detail.html?log=i-${i + 1}`,
  }));
}

const MODULES_30 = makeModules(30, { currentIndex: 5 });
const CURRENT_INSTANCE_6 = "i-6";

// A published-plan public view: two siblings the page confirmed publicly
// reachable (its /api/info fan-out returned 200, so the page set an `href`
// threading &public=true) and one it did not (a 404 — left without an href).
// Drives the decoupling assertion: progress navigates to the reachable siblings
// (href-bearing links) while `readonly` hides Repeat/Continue.
const PUBLIC_MIXED_MODULES = [
  {
    testModule: "m1",
    instances: ["i-1"],
    status: "FINISHED",
    result: "PASSED",
    _statusResolved: true,
    href: "/log-detail.html?log=i-1&public=true",
  },
  {
    testModule: "m6",
    instances: ["i-6"],
    status: "FINISHED",
    result: "FAILED",
    _statusResolved: true,
    href: "/log-detail.html?log=i-6&public=true",
  },
  {
    testModule: "m9",
    instances: ["i-9"],
    status: "FINISHED",
    result: "PASSED",
    _statusResolved: true,
  },
];

// cts-button host.click() bypasses Lit's @click handler. Mirror the
// pattern from cts-log-detail-header.stories.js: target the inner
// <button> when interacting in tests so cts-click fires.
function innerButton(canvasElement, testId) {
  const host = canvasElement.querySelector(`[data-testid="${testId}"]`);
  return host ? host.querySelector("button") : null;
}

function innerLink(canvasElement, testId) {
  const host = canvasElement.querySelector(`[data-testid="${testId}"]`);
  return host ? host.querySelector("a") : null;
}

// --- Stories ---

export const MidPlan = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .modules=${MODULES_30}
      current-instance-id="${CURRENT_INSTANCE_6}"
      .nextEnabled=${true}
    ></cts-test-nav-controls>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("progress is the cts-plan-status segment bar", async () => {
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="progress"]')).toBeTruthy();
      });
      const bar = canvasElement.querySelector('[data-testid="progress"]');
      expect(bar.tagName.toLowerCase()).toBe("cts-plan-status");
    });

    await step("the bar shows the Module N of M label (R14)", async () => {
      await waitFor(() => {
        expect(canvas.getByText("Module 6 of 30")).toBeInTheDocument();
      });
    });

    await step("the 'you are here' marker lands on the viewed module's segment", async () => {
      const segments = canvasElement.querySelectorAll('[data-testid="plan-status-segment"]');
      expect(segments.length).toBe(30);
      expect(segments[5].classList.contains("is-current")).toBe(true);
      expect(segments[0].classList.contains("is-current")).toBe(false);
    });

    await step("group is semantically labelled", async () => {
      const group = canvasElement.querySelector('[role="group"]');
      expect(group).toBeTruthy();
      expect(group.getAttribute("aria-label")).toBe("Test plan navigation");
    });

    await step("all three controls present in default (non-slim) mode", async () => {
      // The widget's full contract; the live log-detail page consumes the
      // slim variant via cts-log-detail-header.
      expect(canvas.getByText(/Return to Plan/)).toBeInTheDocument();
      expect(canvas.getByText(/Repeat Test/)).toBeInTheDocument();
      expect(canvas.getByText(/Continue Plan/)).toBeInTheDocument();
    });

    await step("Return to Plan link href is correctly encoded", async () => {
      const backLink = innerLink(canvasElement, "back-btn");
      expect(backLink).toBeTruthy();
      expect(backLink.getAttribute("href")).toBe(
        `plan-detail.html?plan=${encodeURIComponent(PLAN_ID)}`,
      );
    });
  },
};

export const PublicViewBackLinkAppendsPublicFlag = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .modules=${PUBLIC_MIXED_MODULES}
      current-instance-id="i-6"
      .nextEnabled=${false}
      readonly
      public-view
    ></cts-test-nav-controls>`,
  async play({ canvasElement, step }) {
    await waitFor(() => {
      expect(canvasElement.querySelector('[data-testid="back-btn"]')).toBeTruthy();
    });

    await step("back link appends &public=true", () => {
      const backLink = innerLink(canvasElement, "back-btn");
      expect(backLink).toBeTruthy();
      // public-view appends &public=true so the linked plan page renders
      // its public-share variant — same semantic as the legacy planBtn JS
      // appended this flag conditionally.
      expect(backLink.getAttribute("href")).toBe(
        `plan-detail.html?plan=${encodeURIComponent(PLAN_ID)}&public=true`,
      );
    });

    await step("progress navigates to reachable siblings while actions stay hidden", () => {
      // The decoupling: `readonly` hides Repeat/Continue, but progress-bar
      // navigation is carried per segment by `modules[].href`. On this
      // published-plan public view the two reachable siblings are href-bearing
      // links; the unreachable one (no href) is an inert anchor.
      expect(canvasElement.querySelectorAll("a.cts-pst-seg[href]").length).toBe(2);
      expect(canvasElement.querySelectorAll("a.cts-pst-seg:not([href])").length).toBe(1);
      expect(canvasElement.querySelector('[data-testid="repeat-btn"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="continue-btn"]')).toBeNull();
    });
  },
};

export const RepeatFiresEvent = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .modules=${MODULES_30}
      current-instance-id="${CURRENT_INSTANCE_6}"
      .nextEnabled=${true}
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      expect(canvasElement.querySelector('[data-testid="repeat-btn"]')).toBeTruthy();
    });

    const repeatHandler = fn();
    canvasElement.addEventListener("cts-repeat", repeatHandler);

    const repeatBtn = innerButton(canvasElement, "repeat-btn");
    await userEvent.click(repeatBtn);

    expect(repeatHandler).toHaveBeenCalledOnce();
    expect(repeatHandler.mock.calls[0][0].detail.testId).toBe(TEST_ID);
    expect(repeatHandler.mock.calls[0][0].detail.planId).toBe(PLAN_ID);
  },
};

export const ContinueFiresEvent = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .modules=${MODULES_30}
      current-instance-id="${CURRENT_INSTANCE_6}"
      .nextEnabled=${true}
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      expect(canvasElement.querySelector('[data-testid="continue-btn"]')).toBeTruthy();
    });

    const continueHandler = fn();
    canvasElement.addEventListener("cts-continue", continueHandler);

    const continueBtn = innerButton(canvasElement, "continue-btn");
    await userEvent.click(continueBtn);

    expect(continueHandler).toHaveBeenCalledOnce();
    expect(continueHandler.mock.calls[0][0].detail.testId).toBe(TEST_ID);
    expect(continueHandler.mock.calls[0][0].detail.planId).toBe(PLAN_ID);
  },
};

// R7: a progress segment is a real link — the page supplies its href and the
// browser navigates natively. The widget neither emits nor re-dispatches
// cts-plan-status-activate; clicking a segment fires no such event.
export const SegmentsAreNativeLinks = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .modules=${MODULES_30}
      current-instance-id="${CURRENT_INSTANCE_6}"
      .nextEnabled=${true}
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    const events = [];
    canvasElement.addEventListener("cts-plan-status-activate", (e) => events.push(e));
    // Swallow the default so a click does not navigate the test page away.
    canvasElement.addEventListener("click", (e) => e.preventDefault());
    await waitFor(() => {
      expect(canvasElement.querySelector("a.cts-pst-seg[href]")).toBeTruthy();
    });
    const segments = canvasElement.querySelectorAll("a.cts-pst-seg");
    // The page-built href points at the module's most-recent instance.
    expect(segments[2].getAttribute("href")).toBe("/log-detail.html?log=i-3");
    segments[2].click();
    expect(events.length).toBe(0);
  },
};

export const FirstModule = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .modules=${makeModules(30, { currentIndex: 0 })}
      current-instance-id="i-1"
      .nextEnabled=${true}
    ></cts-test-nav-controls>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Module 1 of 30")).toBeInTheDocument();
    });

    await step("the marker lands on the first segment", async () => {
      const segments = canvasElement.querySelectorAll('[data-testid="plan-status-segment"]');
      expect(segments[0].classList.contains("is-current")).toBe(true);
    });

    await step("Continue control is present", async () => {
      expect(canvas.getByText(/Continue Plan/)).toBeInTheDocument();
    });
  },
};

export const LastModule = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .modules=${makeModules(30, { currentIndex: 29 })}
      current-instance-id="i-30"
      .nextEnabled=${false}
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Module 30 of 30")).toBeInTheDocument();
    });

    // Continue button is hidden on the last module (matches legacy
    // hide-when-no-next behavior in templates/logHeader.html).
    expect(canvasElement.querySelector('[data-testid="continue-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="back-btn"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="repeat-btn"]')).toBeTruthy();
  },
};

export const SingleModulePlan = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .modules=${makeModules(1, { currentIndex: 0 })}
      current-instance-id="i-1"
      .nextEnabled=${false}
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Module 1 of 1")).toBeInTheDocument();
    });

    // Continue button is hidden — only one module in this plan.
    expect(canvasElement.querySelector('[data-testid="continue-btn"]')).toBeNull();
  },
};

export const Readonly = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .modules=${MODULES_30}
      current-instance-id="${CURRENT_INSTANCE_6}"
      .nextEnabled=${true}
      readonly
    ></cts-test-nav-controls>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Module 6 of 30")).toBeInTheDocument();
    });

    await step("only Return to Plan is rendered (no Repeat / Continue)", () => {
      expect(canvasElement.querySelector('[data-testid="back-btn"]')).toBeTruthy();
      expect(canvasElement.querySelector('[data-testid="repeat-btn"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="continue-btn"]')).toBeNull();
    });

    await step("the progress bar still navigates (readonly governs actions, not progress)", () => {
      // Decoupling: the widget's `readonly` hides Repeat/Continue but is not
      // forwarded to the bar, and the modules carry hrefs, so progress segments
      // stay navigable links.
      expect(canvasElement.querySelector("a.cts-pst-seg[href]")).toBeTruthy();
    });
  },
};

export const NoPlan = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id=""
      .modules=${MODULES_30}
      .nextEnabled=${false}
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    // Without a planId, the widget renders nothing — no group, no
    // buttons, no progress bar.
    expect(canvasElement.querySelector('[role="group"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="back-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="repeat-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="continue-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="progress"]')).toBeNull();
  },
};

export const SlimMidPlan = {
  // Slim mode is what the log-detail page uses: the page-level
  // breadcrumb owns "Return to Plan" and the sticky status bar primary
  // owns "Repeat", so this widget contributes only progress + Continue.
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .modules=${MODULES_30}
      current-instance-id="${CURRENT_INSTANCE_6}"
      .nextEnabled=${true}
      slim
    ></cts-test-nav-controls>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Module 6 of 30")).toBeInTheDocument();
    });

    await step("the progress bar marks the viewed module in slim mode too", () => {
      // The log-detail page is the live consumer of slim, so this is the
      // shape Thomas saw in MR 1998's A4 screenshot.
      const segments = canvasElement.querySelectorAll('[data-testid="plan-status-segment"]');
      expect(segments[5].classList.contains("is-current")).toBe(true);
    });

    await step("only progress + Continue render in slim mode", () => {
      expect(canvasElement.querySelector('[data-testid="back-btn"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="repeat-btn"]')).toBeNull();
      expect(canvasElement.querySelector('[data-testid="continue-btn"]')).toBeTruthy();
      expect(canvas.getByText(/Continue Plan/)).toBeInTheDocument();
    });
  },
};

export const SlimEmptyDuringPlanLoad = {
  // Transient state during the /api/plan fetch: planId is set but the
  // modules array is still empty and nextEnabled is false. The slim cluster
  // has nothing useful to render, so it returns `nothing` — leaving
  // the host element empty. The page-level
  // `.ctsNavRow:has(cts-test-nav-controls:empty)` rule then hides
  // the wrapping nav row so its border-bottom doesn't paint a stray
  // divider under the sticky status bar.
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .modules=${[]}
      .nextEnabled=${false}
      slim
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    const host = /** @type {any} */ (
      await waitFor(() => {
        const el = canvasElement.querySelector("cts-test-nav-controls");
        if (!el) throw new Error("host not yet mounted");
        return el;
      })
    );
    await host.updateComplete;
    // No semantic content rendered — the page-level
    // `:has(cts-test-nav-controls:empty)` selector relies on the
    // host having no element children, which is what render() ->
    // nothing produces (Lit only commits a comment marker, never an
    // element, when the top-level template is `nothing`).
    expect(host.querySelector('[role="group"]')).toBeNull();
    expect(host.querySelector('[data-testid="progress"]')).toBeNull();
    expect(host.querySelector('[data-testid="continue-btn"]')).toBeNull();
  },
};

export const SlimLastModuleProgressOnly = {
  // End of the plan in slim mode: Continue is hidden because there is
  // no next module, so the cluster collapses to just the progress
  // indicator. Back nav lives in the breadcrumb; Repeat lives in the
  // sticky status bar primary.
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .modules=${makeModules(30, { currentIndex: 29 })}
      current-instance-id="i-30"
      .nextEnabled=${false}
      slim
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Module 30 of 30")).toBeInTheDocument();
    });

    expect(canvasElement.querySelector('[data-testid="back-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="repeat-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="continue-btn"]')).toBeNull();
  },
};
