import { html } from "lit";
import { expect, within, waitFor, fn } from "storybook/test";
import { http, HttpResponse } from "msw";
import "./cts-run-status-strip.js";

export default {
  title: "Primitives/cts-run-status-strip",
  component: "cts-run-status-strip",
};

// --- Fixtures ---

// classifyRuns reads only `status` / `result`, so the rows are intentionally
// minimal. RUNNING + WAITING count as in-progress; FAILED + UNKNOWN as failing.
const RUNS_WITH_BOTH = [
  { status: "RUNNING" },
  { status: "WAITING" },
  { result: "FAILED" },
  { result: "UNKNOWN" },
  { status: "FINISHED", result: "PASSED" },
];
const RUNS_IN_PROGRESS_ONLY = [
  { status: "RUNNING" },
  { status: "WAITING" },
  { status: "FINISHED", result: "PASSED" },
];
const RUNS_ALL_CLEAR = [
  { status: "FINISHED", result: "PASSED" },
  { status: "FINISHED", result: "WARNING" },
];

const logHandler = (rows) => [
  http.get("/api/log", () =>
    HttpResponse.json({
      draw: 1,
      recordsTotal: rows.length,
      recordsFiltered: rows.length,
      data: rows,
    }),
  ),
];

/**
 * The strip never fetches at connect — the owning page drives it. Stories
 * reproduce that by grabbing the element and calling fetchRuns() in play.
 * @param {HTMLElement} canvasElement
 * @returns {*} the mounted cts-run-status-strip element (typed loose so play()
 *   can call its fetchRuns()/hide() instance methods without a class import)
 */
function strip(canvasElement) {
  return canvasElement.querySelector("cts-run-status-strip");
}

// --- Stories ---

// Actionable: 2 in-progress (RUNNING + WAITING) and 2 failing (FAILED +
// UNKNOWN) → two count links, each deep-linking the matching filtered logs.
export const Actionable = {
  parameters: { msw: { handlers: logHandler(RUNS_WITH_BOTH) } },
  render: () => html`<cts-run-status-strip></cts-run-status-strip>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await strip(canvasElement).fetchRuns();

    await waitFor(() => {
      expect(canvasElement.querySelector(".runStrip--actionable")).toBeTruthy();
    });

    // In-progress link → ?status=running,waiting, count 2.
    const inProgress = canvasElement.querySelector('a[href="logs.html?status=running,waiting"]');
    expect(inProgress).toBeTruthy();
    expect(inProgress.textContent).toContain("in progress");
    expect(inProgress.querySelector("cts-badge").getAttribute("count")).toBe("2");
    // The count badge carries the affordance ring (interactive), NOT a
    // role=button (it sits inside the <a>).
    const badge = inProgress.querySelector("cts-badge");
    expect(badge.hasAttribute("interactive")).toBe(true);
    expect(badge.hasAttribute("clickable")).toBe(false);

    // Failing link → ?result=failed,unknown, count 2.
    const failing = canvasElement.querySelector('a[href="logs.html?result=failed,unknown"]');
    expect(failing).toBeTruthy();
    expect(failing.textContent).toContain("failing");
    expect(failing.querySelector("cts-badge").getAttribute("count")).toBe("2");

    // No skeleton once resolved.
    expect(canvasElement.querySelector(".runStrip-skeleton")).toBeNull();
    // Host is a polite live region (R19).
    expect(strip(canvasElement).getAttribute("aria-live")).toBe("polite");
    expect(canvas).toBeTruthy();
  },
};

// AE2: in-progress only → exactly one link; NO fabricated "0 failing" element.
export const InProgressOnly = {
  parameters: { msw: { handlers: logHandler(RUNS_IN_PROGRESS_ONLY) } },
  render: () => html`<cts-run-status-strip></cts-run-status-strip>`,

  async play({ canvasElement }) {
    await strip(canvasElement).fetchRuns();

    await waitFor(() => {
      expect(
        canvasElement.querySelector('a[href="logs.html?status=running,waiting"]'),
      ).toBeTruthy();
    });

    const inProgress = canvasElement.querySelector('a[href="logs.html?status=running,waiting"]');
    expect(inProgress.querySelector("cts-badge").getAttribute("count")).toBe("2");

    // AE2: the failing link is absent entirely — no "0 failing" rendered.
    expect(canvasElement.querySelector('a[href="logs.html?result=failed,unknown"]')).toBeNull();
  },
};

// AE1/R8: has runs, none actionable → all-caught confirmation, no count, NOT hidden.
export const AllCaughtUp = {
  parameters: { msw: { handlers: logHandler(RUNS_ALL_CLEAR) } },
  render: () => html`<cts-run-status-strip></cts-run-status-strip>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await strip(canvasElement).fetchRuns();

    await waitFor(() => {
      expect(canvasElement.querySelector(".runStrip--clear")).toBeTruthy();
    });

    expect(canvas.getByText(/all caught up/i)).toBeInTheDocument();
    // No count links and no fabricated counts.
    expect(canvasElement.querySelector("a[href^='logs.html?']")).toBeNull();
    expect(canvasElement.querySelector("cts-badge")).toBeNull();
  },
};

// AE1b: a zero-runs account renders nothing at all (no .runStrip).
export const NoRuns = {
  parameters: { msw: { handlers: logHandler([]) } },
  render: () => html`<cts-run-status-strip></cts-run-status-strip>`,

  async play({ canvasElement }) {
    await strip(canvasElement).fetchRuns();

    // Give the re-render a tick, then assert the strip collapsed to nothing.
    await waitFor(() => {
      expect(strip(canvasElement).querySelector(".runStrip")).toBeNull();
    });
  },
};

// R20: /api/log fails → calm "couldn't load" line, a diagnostic warn, never
// hidden and never implying all-clear.
export const FetchError = {
  parameters: {
    msw: { handlers: [http.get("/api/log", () => new HttpResponse(null, { status: 500 }))] },
  },
  render: () => html`<cts-run-status-strip></cts-run-status-strip>`,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;

    try {
      await strip(canvasElement).fetchRuns();

      await waitFor(() => {
        expect(canvasElement.querySelector(".runStrip--error")).toBeTruthy();
      });
      expect(canvas.getByText(/couldn't load run status/i)).toBeInTheDocument();
      // Degraded, not all-clear: the all-caught marker must NOT appear.
      expect(canvasElement.querySelector(".runStrip--clear")).toBeNull();

      const joined = warnSpy.mock.calls.flat().join(" ");
      expect(joined).toContain("cts-run-status-strip");
      expect(joined).toContain("/api/log");
    } finally {
      console.warn = origWarn;
    }
  },
};

// R9: hide() (the anon / Published path) collapses the strip to nothing.
export const Hidden = {
  render: () => html`<cts-run-status-strip></cts-run-status-strip>`,

  async play({ canvasElement }) {
    strip(canvasElement).hide();

    await waitFor(() => {
      expect(strip(canvasElement).querySelector(".runStrip")).toBeNull();
    });
  },
};
