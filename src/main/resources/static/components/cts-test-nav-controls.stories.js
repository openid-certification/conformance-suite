import { html } from "lit";
import { expect, within, waitFor, fn, userEvent } from "storybook/test";
import "./cts-test-nav-controls.js";

export default {
  title: "Components/cts-test-nav-controls",
  component: "cts-test-nav-controls",
};

const TEST_ID = "test-instance-001";
const PLAN_ID = "plan-instance-001";

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
      .currentIndex=${5}
      .totalCount=${30}
      .nextEnabled=${true}
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Module 6 of 30")).toBeInTheDocument();
    });

    // Group is semantically labelled
    const group = canvasElement.querySelector('[role="group"]');
    expect(group).toBeTruthy();
    expect(group.getAttribute("aria-label")).toBe("Test plan navigation");

    // Progress bar uses 1-based aria values
    const progressBar = canvasElement.querySelector('[role="progressbar"]');
    expect(progressBar).toBeTruthy();
    expect(progressBar.getAttribute("aria-valuenow")).toBe("6");
    expect(progressBar.getAttribute("aria-valuemin")).toBe("1");
    expect(progressBar.getAttribute("aria-valuemax")).toBe("30");

    // All three controls present (default / non-slim mode — the
    // widget's full contract; the live log-detail page consumes the
    // slim variant via cts-log-detail-header).
    expect(canvas.getByText(/Return to Plan/)).toBeInTheDocument();
    expect(canvas.getByText(/Repeat Test/)).toBeInTheDocument();
    expect(canvas.getByText(/Continue Plan/)).toBeInTheDocument();

    // Return to Plan link href is correctly encoded
    const backLink = innerLink(canvasElement, "back-btn");
    expect(backLink).toBeTruthy();
    expect(backLink.getAttribute("href")).toBe(
      `plan-detail.html?plan=${encodeURIComponent(PLAN_ID)}`,
    );
  },
};

export const PublicViewBackLinkAppendsPublicFlag = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .currentIndex=${5}
      .totalCount=${30}
      .nextEnabled=${false}
      readonly
      public-view
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      expect(canvasElement.querySelector('[data-testid="back-btn"]')).toBeTruthy();
    });

    const backLink = innerLink(canvasElement, "back-btn");
    expect(backLink).toBeTruthy();
    // public-view appends &public=true so the linked plan page renders
    // its public-share variant — same semantic as the legacy planBtn JS
    // appended this flag conditionally.
    expect(backLink.getAttribute("href")).toBe(
      `plan-detail.html?plan=${encodeURIComponent(PLAN_ID)}&public=true`,
    );
  },
};

export const RepeatFiresEvent = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .currentIndex=${5}
      .totalCount=${30}
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
      .currentIndex=${5}
      .totalCount=${30}
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

export const FirstModule = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .currentIndex=${0}
      .totalCount=${30}
      .nextEnabled=${true}
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Module 1 of 30")).toBeInTheDocument();
    });

    const progressBar = canvasElement.querySelector('[role="progressbar"]');
    expect(progressBar.getAttribute("aria-valuenow")).toBe("1");

    expect(canvas.getByText(/Continue Plan/)).toBeInTheDocument();
  },
};

export const LastModule = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .currentIndex=${29}
      .totalCount=${30}
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
      .currentIndex=${0}
      .totalCount=${1}
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
      .currentIndex=${5}
      .totalCount=${30}
      .nextEnabled=${true}
      readonly
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Module 6 of 30")).toBeInTheDocument();
    });

    // Public/readonly view: only Return to Plan is rendered.
    expect(canvasElement.querySelector('[data-testid="back-btn"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="repeat-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="continue-btn"]')).toBeNull();
  },
};

export const NoPlan = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id=""
      .currentIndex=${0}
      .totalCount=${0}
      .nextEnabled=${false}
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    // Without a planId, the widget renders nothing — no group, no
    // buttons, nothing to find.
    expect(canvasElement.querySelector('[role="group"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="back-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="repeat-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="continue-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[role="progressbar"]')).toBeNull();
  },
};

export const ProgressClampsOutOfRangeIndex = {
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .currentIndex=${50}
      .totalCount=${30}
      .nextEnabled=${false}
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      // Defensive against transient props during page load — clamp at
      // total instead of overflowing to "Module 51 of 30".
      expect(canvas.getByText("Module 30 of 30")).toBeInTheDocument();
    });

    const progressBar = canvasElement.querySelector('[role="progressbar"]');
    expect(progressBar.getAttribute("aria-valuenow")).toBe("30");
    expect(progressBar.getAttribute("aria-valuemax")).toBe("30");
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
      .currentIndex=${5}
      .totalCount=${30}
      .nextEnabled=${true}
      slim
    ></cts-test-nav-controls>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Module 6 of 30")).toBeInTheDocument();
    });

    expect(canvasElement.querySelector('[data-testid="back-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="repeat-btn"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="continue-btn"]')).toBeTruthy();
    expect(canvas.getByText(/Continue Plan/)).toBeInTheDocument();
  },
};

export const SlimEmptyDuringPlanLoad = {
  // Transient state during the /api/plan fetch: planId is set but
  // totalCount is still 0 and nextEnabled is false. The slim cluster
  // has nothing useful to render, so it returns `nothing` — leaving
  // the host element empty. The page-level
  // `.ctsNavRow:has(cts-test-nav-controls:empty)` rule then hides
  // the wrapping nav row so its border-bottom doesn't paint a stray
  // divider under the sticky status bar.
  render: () =>
    html`<cts-test-nav-controls
      test-id="${TEST_ID}"
      plan-id="${PLAN_ID}"
      .currentIndex=${0}
      .totalCount=${0}
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
    expect(host.querySelector('[role="progressbar"]')).toBeNull();
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
      .currentIndex=${29}
      .totalCount=${30}
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
