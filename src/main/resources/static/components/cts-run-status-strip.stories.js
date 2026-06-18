import { html } from "lit";
import { expect, within, waitFor, fn } from "storybook/test";
import { http, HttpResponse } from "msw";
import "./cts-run-status-strip.js";

export default {
  title: "Components/cts-run-status-strip",
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

  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("fetchRuns resolves into the actionable state", async () => {
      await strip(canvasElement).fetchRuns();
      await waitFor(() => {
        expect(canvasElement.querySelector(".runStrip--actionable")).toBeTruthy();
      });
    });

    await step("in-progress link deep-links ?status=running,waiting with count 2", async () => {
      const inProgress = canvasElement.querySelector('a[href="logs.html?status=running,waiting"]');
      expect(inProgress).toBeTruthy();
      expect(inProgress.textContent).toContain("in progress");
      expect(inProgress.querySelector("cts-badge").getAttribute("count")).toBe("2");
      // The count badge carries the affordance ring (interactive), NOT a
      // role=button (it sits inside the <a>).
      const badge = inProgress.querySelector("cts-badge");
      expect(badge.hasAttribute("interactive")).toBe(true);
      expect(badge.hasAttribute("clickable")).toBe(false);
    });

    await step("failing link deep-links ?result=failed,unknown with count 2", async () => {
      const failing = canvasElement.querySelector('a[href="logs.html?result=failed,unknown"]');
      expect(failing).toBeTruthy();
      expect(failing.textContent).toContain("failing");
      expect(failing.querySelector("cts-badge").getAttribute("count")).toBe("2");
    });

    await step("no skeleton remains and host is a polite live region (R19)", async () => {
      expect(canvasElement.querySelector(".runStrip-skeleton")).toBeNull();
      expect(strip(canvasElement).getAttribute("aria-live")).toBe("polite");
      expect(canvas).toBeTruthy();
    });
  },
};

// AE2: in-progress only → exactly one link; NO fabricated "0 failing" element.
export const InProgressOnly = {
  parameters: { msw: { handlers: logHandler(RUNS_IN_PROGRESS_ONLY) } },
  render: () => html`<cts-run-status-strip></cts-run-status-strip>`,

  async play({ canvasElement, step }) {
    await step("in-progress link renders with count 2", async () => {
      await strip(canvasElement).fetchRuns();
      await waitFor(() => {
        expect(
          canvasElement.querySelector('a[href="logs.html?status=running,waiting"]'),
        ).toBeTruthy();
      });
      const inProgress = canvasElement.querySelector('a[href="logs.html?status=running,waiting"]');
      expect(inProgress.querySelector("cts-badge").getAttribute("count")).toBe("2");
    });

    await step("the failing link is absent entirely — no '0 failing' rendered (AE2)", async () => {
      expect(canvasElement.querySelector('a[href="logs.html?result=failed,unknown"]')).toBeNull();
    });
  },
};

// AE1/R8: has runs, none actionable → all-caught confirmation, no count, NOT hidden.
export const AllCaughtUp = {
  parameters: { msw: { handlers: logHandler(RUNS_ALL_CLEAR) } },
  render: () => html`<cts-run-status-strip></cts-run-status-strip>`,

  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    await step("fetchRuns resolves into the all-clear state", async () => {
      await strip(canvasElement).fetchRuns();
      await waitFor(() => {
        expect(canvasElement.querySelector(".runStrip--clear")).toBeTruthy();
      });
    });

    await step("shows the all-caught confirmation with no count links or badges", async () => {
      expect(canvas.getByText(/all caught up/i)).toBeInTheDocument();
      expect(canvasElement.querySelector("a[href^='logs.html?']")).toBeNull();
      expect(canvasElement.querySelector("cts-badge")).toBeNull();
    });
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

  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;

    try {
      await step("a failed fetch surfaces the degraded error state, not all-clear", async () => {
        await strip(canvasElement).fetchRuns();
        await waitFor(() => {
          expect(canvasElement.querySelector(".runStrip--error")).toBeTruthy();
        });
        expect(canvas.getByText(/couldn't load run status/i)).toBeInTheDocument();
        // Degraded, not all-clear: the all-caught marker must NOT appear.
        expect(canvasElement.querySelector(".runStrip--clear")).toBeNull();
      });

      await step("a diagnostic warn names the component and endpoint", async () => {
        const joined = warnSpy.mock.calls.flat().join(" ");
        expect(joined).toContain("cts-run-status-strip");
        expect(joined).toContain("/api/log");
      });
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

// R9 regression: a stale fetchRuns() must NOT resurrect the strip after hide().
// `await fetch()` yields to the microtask queue, so calling hide() synchronously
// after fetchRuns() returns — before the response resolves — reproduces the
// "My → Published while /api/log in flight" race deterministically, no delay
// needed. The fetch-generation guard must discard the stale resolution so the
// strip stays collapsed (personal counts never leak onto the Published view).
export const RaceHideAfterFetchWins = {
  parameters: { msw: { handlers: logHandler(RUNS_WITH_BOTH) } },
  render: () => html`<cts-run-status-strip></cts-run-status-strip>`,

  async play({ canvasElement, step }) {
    const el = strip(canvasElement);

    await step(
      "hide() supersedes the in-flight fetch and the stale resolution is discarded",
      async () => {
        const pending = el.fetchRuns(); // status → loading, fetch in flight
        el.hide(); // user switched to Published: supersede + collapse
        await pending; // the now-stale fetch resolves; the guard must discard it
      },
    );

    await step("the strip stays collapsed — no actionable counts leak in", async () => {
      // Give any (incorrect) re-render a tick, then assert it stayed hidden.
      await waitFor(() => {
        expect(el.querySelector(".runStrip")).toBeNull();
      });
      expect(el.querySelector(".runStrip--actionable")).toBeNull();
    });
  },
};

// R20 regression: a 200 with a non-array, non-{data:array} body is a contract
// violation, not "zero runs" — it must surface the degraded error state, not
// silently hide the strip.
export const MalformedBody = {
  parameters: {
    msw: { handlers: [http.get("/api/log", () => HttpResponse.json({ unexpected: "shape" }))] },
  },
  render: () => html`<cts-run-status-strip></cts-run-status-strip>`,

  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;

    try {
      await step("a malformed body surfaces the error state, not silently hidden", async () => {
        await strip(canvasElement).fetchRuns();
        await waitFor(() => {
          expect(canvasElement.querySelector(".runStrip--error")).toBeTruthy();
        });
        expect(canvas.getByText(/couldn't load run status/i)).toBeInTheDocument();
        // Not silently hidden, and not implying all-clear.
        expect(canvasElement.querySelector(".runStrip--clear")).toBeNull();
      });

      await step("a diagnostic warn names the contract violation", async () => {
        expect(warnSpy.mock.calls.flat().join(" ")).toContain("unexpected body shape");
      });
    } finally {
      console.warn = origWarn;
    }
  },
};
