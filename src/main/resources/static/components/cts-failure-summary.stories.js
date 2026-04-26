import { html } from "lit";
import { expect, within, waitFor, fn, userEvent } from "storybook/test";
import "./cts-failure-summary.js";

export default {
  title: "Components/cts-failure-summary",
  component: "cts-failure-summary",
};

// Three-row baseline: two FAILUREs and a WARNING. Mirrors the shape the
// header builds from `testInfo.results.filter(...)` — `_id`, `result`,
// `src`, `msg`, optional `requirements`, optional `blockId`.
const FAILURES = [
  {
    _id: "r5",
    result: "FAILURE",
    src: "ValidateIdToken",
    msg: "Signature invalid",
  },
  {
    _id: "r6",
    result: "FAILURE",
    src: "CheckClaims",
    msg: "Missing sub claim",
  },
  {
    _id: "r4",
    result: "WARNING",
    src: "CheckScope",
    msg: "Extra scope",
  },
];

const FAILURES_WITH_REQUIREMENTS = [
  {
    _id: "r5",
    result: "FAILURE",
    src: "ValidateIdToken",
    msg: "Signature invalid",
    requirements: ["OIDCC-3.1.3.7-6"],
  },
  {
    _id: "r6",
    result: "FAILURE",
    src: "CheckClaims",
    msg: "Missing sub claim",
    requirements: ["OIDCC-5.1", "FAPI1.0-5.2.2"],
  },
];

const FAILURES_GROUPED = [
  {
    _id: "g1",
    result: "FAILURE",
    src: "EnsureValidAud",
    msg: "Authorization request",
    blockId: "auth",
  },
  {
    _id: "g2",
    result: "FAILURE",
    src: "EnsureNonce",
    msg: "Nonce mismatch in authorization response",
    blockId: "auth",
  },
  {
    _id: "g3",
    result: "WARNING",
    src: "EnsureToken",
    msg: "Token request",
    blockId: "token",
  },
];

export const Default = {
  render: () => html`<cts-failure-summary .failures=${FAILURES}></cts-failure-summary>`,
  async play({ canvasElement }) {
    const summary = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="failure-summary"]');
      if (!el) throw new Error("failure-summary not yet rendered");
      return el;
    });

    expect(within(summary).getByText("Failure summary:")).toBeInTheDocument();

    // Expanded by default — list rendered with three rows.
    const list = canvasElement.querySelector('[data-testid="failure-list"]');
    expect(list).toBeTruthy();
    expect(list.querySelectorAll(".failureItem")).toHaveLength(3);

    // Each row carries the right severity badge.
    expect(canvasElement.querySelectorAll('cts-badge[label="FAILURE"]')).toHaveLength(2);
    expect(canvasElement.querySelectorAll('cts-badge[label="WARNING"]')).toHaveLength(1);

    // Failure text combines `src: msg`.
    expect(within(summary).getByText("ValidateIdToken: Signature invalid")).toBeInTheDocument();
  },
};

export const Empty = {
  render: () => html`<cts-failure-summary .failures=${[]}></cts-failure-summary>`,
  async play({ canvasElement }) {
    // The component returns `nothing` when failures is empty. Wait one
    // microtask so Lit's first render flushes before asserting absence.
    await Promise.resolve();
    const summary = canvasElement.querySelector('[data-testid="failure-summary"]');
    expect(summary).toBeNull();
  },
};

export const Collapsed = {
  render: () => html`<cts-failure-summary .failures=${FAILURES}></cts-failure-summary>`,
  async play({ canvasElement }) {
    const title = await waitFor(() => {
      const el = canvasElement.querySelector(".failureSummaryTitle");
      if (!el) throw new Error("failureSummaryTitle not yet rendered");
      return el;
    });

    // Initial state: list visible, chevron points up.
    expect(canvasElement.querySelector('[data-testid="failure-list"]')).toBeTruthy();
    expect(canvasElement.querySelector('cts-icon[name="chevron-up"]')).toBeTruthy();

    // Click the title → collapse.
    await userEvent.click(title);

    await waitFor(() => {
      expect(canvasElement.querySelector('[data-testid="failure-list"]')).toBeNull();
      expect(canvasElement.querySelector('cts-icon[name="chevron-down"]')).toBeTruthy();
    });

    // Click again → re-expand.
    await userEvent.click(title);
    await waitFor(() => {
      expect(canvasElement.querySelector('[data-testid="failure-list"]')).toBeTruthy();
    });
  },
};

export const WithRequirements = {
  render: () =>
    html`<cts-failure-summary .failures=${FAILURES_WITH_REQUIREMENTS}></cts-failure-summary>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="failure-list"]');
      if (!el) throw new Error("failure-list not yet rendered");
      return el;
    });

    // Three requirement chips total (one + two).
    const chips = canvasElement.querySelectorAll(".logRequirementBadge");
    expect(chips).toHaveLength(3);
    expect(chips[0].textContent.trim()).toBe("OIDCC-3.1.3.7-6");
  },
};

export const Compact = {
  render: () =>
    html`<cts-failure-summary
      compact
      .failures=${FAILURES_WITH_REQUIREMENTS}
    ></cts-failure-summary>`,
  async play({ canvasElement }) {
    const host = await waitFor(() => {
      const el = canvasElement.querySelector("cts-failure-summary");
      if (!el) throw new Error("cts-failure-summary not yet rendered");
      return el;
    });

    // The `compact` attribute reflects so the [compact] CSS selectors apply.
    expect(host.hasAttribute("compact")).toBe(true);

    // Title and chevron are not rendered in compact mode (they're
    // omitted from the render output, not just hidden).
    expect(canvasElement.querySelector(".failureSummaryTitle")).toBeNull();
    expect(canvasElement.querySelector('cts-icon[name="chevron-up"]')).toBeNull();

    // Rows still render; each row's failure text is present.
    const rows = canvasElement.querySelectorAll(".failureItem");
    expect(rows).toHaveLength(2);

    // Requirement chips are hidden by [compact] CSS — the elements are
    // still in the DOM but `display: none`.
    const chip = canvasElement.querySelector(".logRequirementBadge");
    expect(chip).toBeTruthy();
    expect(getComputedStyle(chip).display).toBe("none");
  },
};

export const GroupedByBlock = {
  render: () =>
    html`<cts-failure-summary group-by-block .failures=${FAILURES_GROUPED}></cts-failure-summary>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="failure-list"]');
      if (!el) throw new Error("failure-list not yet rendered");
      return el;
    });

    // Two block groups — `auth` (two failures) and `token` (one warning).
    const groups = canvasElement.querySelectorAll('[data-testid="failure-block-group"]');
    expect(groups).toHaveLength(2);

    // First group's header text comes from the first entry's `msg`.
    const firstHeader = groups[0].querySelector(".failureBlockHeader");
    expect(firstHeader.textContent).toContain("Authorization request");
    expect(firstHeader.textContent).toContain("(2 failures)");

    const secondHeader = groups[1].querySelector(".failureBlockHeader");
    expect(secondHeader.textContent).toContain("Token request");
    expect(secondHeader.textContent).toContain("(1 failure)");
  },
};

export const EmitsScrollEvent = {
  render: () => html`<cts-failure-summary .failures=${FAILURES}></cts-failure-summary>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="failure-list"]');
      if (!el) throw new Error("failure-list not yet rendered");
      return el;
    });

    // Listen at document level — that's where the page-level handler
    // (js/log-detail-v2.js) attaches. The event is bubbles + composed, so
    // it reaches document regardless of where the component is mounted.
    const handler = fn();
    document.addEventListener("cts-scroll-to-entry", handler);

    try {
      const failureText = within(canvasElement).getByText("ValidateIdToken: Signature invalid");
      await userEvent.click(failureText);

      expect(handler).toHaveBeenCalledOnce();
      const detail = handler.mock.calls[0][0].detail;
      expect(detail.entryId).toBe("r5");

      // Forward-compat for U8 (rail in shadow DOM): event must be composed.
      expect(handler.mock.calls[0][0].composed).toBe(true);
    } finally {
      document.removeEventListener("cts-scroll-to-entry", handler);
    }
  },
};

export const KeyboardActivation = {
  render: () => html`<cts-failure-summary .failures=${FAILURES}></cts-failure-summary>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="failure-list"]');
      if (!el) throw new Error("failure-list not yet rendered");
      return el;
    });

    const handler = fn();
    document.addEventListener("cts-scroll-to-entry", handler);

    try {
      // First row's failure-text span has role="button" and tabindex="0",
      // so it accepts focus; Enter activates the same code path as click.
      const firstRow = /** @type {HTMLElement} */ (
        canvasElement.querySelector('.failureItem .failureText[data-entry-id="r5"]')
      );
      firstRow.focus();
      expect(document.activeElement).toBe(firstRow);

      await userEvent.keyboard("{Enter}");
      expect(handler).toHaveBeenCalledOnce();
      expect(handler.mock.calls[0][0].detail.entryId).toBe("r5");

      // Space also activates (per ARIA "button" widget convention).
      await userEvent.keyboard(" ");
      expect(handler).toHaveBeenCalledTimes(2);
    } finally {
      document.removeEventListener("cts-scroll-to-entry", handler);
    }
  },
};
