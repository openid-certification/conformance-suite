import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { withMockFetch, withProgrammableFetch } from "@fixtures/helpers.js";
import {
  MOCK_LOG_ENTRIES,
  MOCK_EMPTY_LOG,
  MOCK_SUCCESS_LOG,
  MOCK_BLOCKS_WITH_STATUS,
  MOCK_BLOCKS_FILTERABLE,
  MOCK_BLOCKS_POLL_FIRST,
  MOCK_BLOCKS_POLL_SECOND,
  MOCK_EMPTY_BLOCK,
  MOCK_BLOCKS_ALIGN,
} from "@fixtures/mock-log-entries.js";
import { MOCK_TEST_STATUS } from "@fixtures/mock-test-data.js";
import { BACKOFF_MAX_MULTIPLIER, RESUME_GAP_MULTIPLIER } from "./cts-log-viewer.js";
import "./cts-log-viewer.js";

export default {
  title: "Components/cts-log-viewer",
  component: "cts-log-viewer",
};

// Polling stories never settle: the viewer re-fetches /api/log on an interval
// (20ms via the `_pollIntervalMs` test hook), which trips Chromatic's
// reload-loop detector during capture ("URL reload loop detected"). They are
// behavior tests, not visual states — the static stories above cover the
// visuals — so exclude them from Chromatic snapshots. `test-storybook` still
// runs their play functions.
const pollingStoryParameters = {
  chromatic: { disableSnapshot: true },
};

async function waitForLogLoad(canvasElement) {
  await waitFor(
    () => {
      const spinner = canvasElement.querySelector(".spinner-border");
      expect(spinner).toBeNull();
    },
    { timeout: 3000 },
  );
}

export const WithEntries = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () => html`<cts-log-viewer test-id="test-abc-123"></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    await waitForLogLoad(canvasElement);
    await step("result badges and entry rows render", async () => {
      const badges = canvasElement.querySelectorAll("cts-badge");
      expect(badges.length).toBeGreaterThan(0);
      const entries = canvasElement.querySelectorAll(".logItem");
      expect(entries.length).toBeGreaterThan(0);
    });
    await step("entry message text renders", async () => {
      const canvas = within(canvasElement);
      expect(canvas.getByText(/ID token signature validation failed/)).toBeTruthy();
    });
  },
};

export const NonCollapsibleBlocks = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () => html`<cts-log-viewer test-id="test-abc-123"></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    await waitForLogLoad(canvasElement);
    const blocks = canvasElement.querySelectorAll("div.logBlock");
    const firstBlock = blocks[0];
    await step("blocks render as plain divs with a presentational header", async () => {
      // Each block renders as a plain <div class="logBlock"> with a
      // presentational <div class="startBlock"> header. Blocks are not
      // collapsible, so there is no open/closed state.
      expect(blocks.length).toBeGreaterThan(0);
      const header = firstBlock.querySelector(".startBlock");
      expect(header).toBeTruthy();
      expect(header.tagName).toBe("DIV");
    });

    await step("presentational header: no chevron, not focusable, no button role", async () => {
      const header = firstBlock.querySelector(".startBlock");
      // Assert the absence of the tabindex attribute (rather than
      // tabIndex === -1, which is the default for any <div> and would not
      // catch an accidental explicit tabindex="-1").
      expect(header.querySelector("cts-icon")).toBeNull();
      expect(header.hasAttribute("tabindex")).toBe(false);
      expect(header.getAttribute("role")).toBeNull();
    });

    await step("block children are always rendered and visible (no collapse)", async () => {
      // The cts-log-entry host is display:contents in the wide layout
      // (subgrid relay), so it has no box — assert on the painted .logItem
      // inside it.
      const children = firstBlock.querySelectorAll("cts-log-entry");
      expect(children.length).toBeGreaterThan(0);
      const firstItem = firstBlock.querySelector("cts-log-entry .logItem");
      expect(firstItem).toBeTruthy();
      expect(firstItem.checkVisibility()).toBe(true);
    });
  },
};

export const EmptyLog = {
  decorators: [withMockFetch("/api/log/", MOCK_EMPTY_LOG)],
  render: () => html`<cts-log-viewer test-id="test-empty-789"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);
    const canvas = within(canvasElement);
    expect(canvas.getByText("No log entries")).toBeTruthy();
  },
};

export const Loading = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES, { delay: 60000 })],
  render: () => html`<cts-log-viewer test-id="test-loading"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const spinner = canvasElement.querySelector(".spinner-border");
      expect(spinner).toBeTruthy();
    });
  },
};

export const AllSuccess = {
  decorators: [withMockFetch("/api/log/", MOCK_SUCCESS_LOG)],
  render: () => html`<cts-log-viewer test-id="test-ok-456"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);
    // The story asserts that every result chip renders as `pass` when no
    // entry has failed. cts-log-entry-id ships its own `secondary` badge
    // for the LOG-NNNN reference (per design), so the assertion must
    // scope to result badges only — not every cts-badge on the page.
    const resultBadges = canvasElement.querySelectorAll(
      'cts-log-entry cts-badge:not([data-testid="log-entry-id-chip"])',
    );
    expect(resultBadges.length).toBeGreaterThan(0);
    for (const badge of resultBadges) {
      expect(badge.getAttribute("variant")).toBe("pass");
    }
  },
};

// Retry banner tests use `_pollIntervalMs` (internal test hook) bound via Lit
// `.prop` syntax so the fast interval is set before connectedCallback fires.
// The programmable-fetch state is mutable so the play function can flip
// responses mid-test to exercise recovery and lifecycle.

export const PersistentFailureBanner = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        responder: () => new Response("Server error", { status: 500 }),
      };
      // Smuggle state to the play function via a module-scoped WeakRef-less
      // approach: tack it onto window for the duration of the story.
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () => html`
    <cts-log-viewer test-id="test-failing-log" ._pollIntervalMs=${20}></cts-log-viewer>
  `,
  async play({ canvasElement, step }) {
    try {
      await step("banner appears after sustained 500s", async () => {
        await waitFor(
          () => {
            const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
            expect(banner).toBeTruthy();
            expect(banner.textContent).toContain("Log connection lost");
            // The terminal "gave-up" copy also starts "Log connection lost",
            // so the text alone no longer distinguishes the two. Pin the
            // kind, or this story would pass if 500s wrongly went terminal.
            expect(banner.getAttribute("data-error-kind")).toBe("retrying");
          },
          { timeout: 3000 },
        );
      });
      await step("banner is a polite aria-live region", async () => {
        // Screen readers announce softly.
        const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
        expect(banner.getAttribute("aria-live")).toBe("polite");
      });
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

export const RecoveryClearsBanner = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        responder: () => new Response("Server error", { status: 500 }),
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () => html`
    <cts-log-viewer test-id="test-recovery-log" ._pollIntervalMs=${20}></cts-log-viewer>
  `,
  async play({ canvasElement, step }) {
    const state = window.__ctsLogViewerFetchState;
    try {
      await step("banner appears after three 500s", async () => {
        await waitFor(
          () => {
            expect(canvasElement.querySelector('[data-testid="log-viewer-error"]')).toBeTruthy();
          },
          { timeout: 3000 },
        );
      });
      await step("flipping the responder to success clears the banner", async () => {
        state.responder = () =>
          new Response(JSON.stringify(MOCK_SUCCESS_LOG), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          });
        await waitFor(
          () => {
            expect(canvasElement.querySelector('[data-testid="log-viewer-error"]')).toBeNull();
          },
          { timeout: 2000 },
        );
      });
      await step("recovery also resets the give-up budget", async () => {
        // Without this, a tab that failed once at minute 0 would give up 15
        // minutes later even on a connection that healed immediately — a
        // slow, unreproducible bug no banner assertion would catch.
        const viewer = canvasElement.querySelector("cts-log-viewer");
        expect(viewer._firstFailureAt).toBe(0);
        expect(viewer._lastFailureAt).toBe(0);
        expect(viewer._consecutiveFailures).toBe(0);
        expect(viewer._consecutiveAuthFailures).toBe(0);
      });
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

// --- Terminal (polling-stopped) states, GitLab #1890 ---
// A 401 from the backend's RestAuthenticationEntryPoint used to be flattened
// into the same "Log connection lost — retrying…" banner as a network blip,
// and the viewer then retried every 3s forever. These stories pin the two
// states where polling must STOP: an authentication rejection, and a
// generic failure that outlives the give-up budget.

// The fast poll interval every story below uses. Named so the settle window
// in expectPollingStopped can be derived from it rather than guessed.
const FAST_POLL_MS = 20;

// BACKOFF_MAX_MULTIPLIER and RESUME_GAP_MULTIPLIER are imported from the
// component itself, not mirrored here — a hand-copied constant would drift
// the moment someone tuned the real one, silently shrinking the settle
// window below and defanging the threshold assertions further down.

/**
 * Assert the viewer really stopped polling, rather than merely re-labelling
 * its banner.
 *
 * Deliberately NOT built on `waitFor`: that helper re-runs its callback on
 * every DOM mutation as well as on its interval, so two invocations can land
 * in the same tick, observe the same count, and "settle" while requests are
 * still flowing. This sleeps on its own timer for longer than the widest
 * backed-off delay and asserts the count did not move — the only sampling
 * that actually proves a negative.
 *
 * The `minRequests` floor guards the other failure mode: if a future refactor
 * broke the responder's URL filter, `urls` would stay empty and a naive
 * stability check would pass vacuously.
 * @param {{ urls: string[] }} state Programmable-fetch state recording each polled URL.
 * @param {number} [minRequests] Requests that must have been recorded before the settle check.
 * @returns {Promise<void>} Resolves once the count has held still.
 */
/**
 * Detach the story's viewer so `disconnectedCallback` clears its poll timer.
 * Storybook leaves the previous story's DOM mounted until the next render,
 * so a story that ends while the viewer is still healthily polling every
 * 20ms keeps firing requests into the NEXT story's fetch mock — which is how
 * `DisconnectStopsPolling`'s unscoped counter starts seeing phantom calls.
 * Call from the `finally` of any story that does not end in a terminal state.
 * @param {any} canvasElement The story root.
 * @returns {void}
 */
function stopViewer(canvasElement) {
  const viewer = canvasElement.querySelector("cts-log-viewer");
  if (viewer) viewer.remove();
}

async function expectPollingStopped(state, minRequests = 2) {
  expect(state.urls.length).toBeGreaterThanOrEqual(minRequests);
  const before = state.urls.length;
  // Wider than the widest backed-off delay the component can schedule, with
  // headroom, so a still-running loop is guaranteed at least one more
  // request inside the window.
  const settleWindowMs = FAST_POLL_MS * BACKOFF_MAX_MULTIPLIER * 3;
  await new Promise((resolve) => setTimeout(resolve, settleWindowMs));
  expect(state.urls.length).toBe(before);
}

export const SessionExpiredStopsPolling = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        /** @type {string[]} */
        urls: [],
        responder: (url) => {
          // Scope to THIS story's viewer: a previous fast-polling story can
          // still have an in-flight poll when this story patches fetch, and
          // that stray request must not read as "still polling".
          if (!url.includes("test-expired-session")) {
            return new Response("[]", {
              status: 200,
              headers: { "Content-Type": "application/json" },
            });
          }
          state.urls.push(url);
          // Shape mirrors RestAuthenticationEntryPoint's JSON body.
          return new Response(JSON.stringify({ error: "Unauthorized", message: "expired" }), {
            status: 401,
            headers: { "Content-Type": "application/json" },
          });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () => html`
    <cts-log-viewer test-id="test-expired-session" ._pollIntervalMs=${20}></cts-log-viewer>
  `,
  async play({ canvasElement, step }) {
    const state = window.__ctsLogViewerFetchState;
    try {
      await step("401s surface a session-expiry banner, not 'connection lost'", async () => {
        await waitFor(
          () => {
            const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
            expect(banner).toBeTruthy();
            expect(banner.getAttribute("data-error-kind")).toBe("session-expired");
          },
          { timeout: 3000 },
        );
        const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
        expect(banner.textContent).toContain("session has expired");
        expect(banner.textContent).not.toContain("retrying");
      });
      await step("terminal banner is danger, not the warning retry banner", async () => {
        const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
        expect(banner.getAttribute("variant")).toBe("danger");
      });
      await step("empty log does not also claim 'No log entries'", async () => {
        // Nothing ever fetched, so `_entries` is empty — but saying the test
        // produced no output would be a different (and wrong) claim.
        expect(canvasElement.textContent).not.toContain("No log entries");
      });
      await step("banner offers both a retry and a reload affordance", async () => {
        const retry = canvasElement.querySelector('[data-testid="log-viewer-retry"]');
        const reload = canvasElement.querySelector('[data-testid="log-viewer-reload"]');
        expect(retry).toBeTruthy();
        expect(reload).toBeTruthy();
        // Neither is clicked here: reload would tear down the test runner's
        // page, and the retry path gets its own story (RetryResumesPolling).
        expect(retry.getAttribute("label")).toBe("Try again");
        expect(reload.getAttribute("label")).toBe("Reload page");
        // `cts-button icon=` is outside lint:icons' literal `cts-icon name=`
        // check, so pin the vendored name here.
        expect(retry.getAttribute("icon")).toBe("arrow-reload-02");
      });
      await step("polling stops", async () => {
        await expectPollingStopped(state);
      });
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

export const PublicViewAccessDenied = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        /** @type {string[]} */
        urls: [],
        responder: (url) => {
          if (!url.includes("test-unpublished-log")) {
            return new Response("[]", {
              status: 200,
              headers: { "Content-Type": "application/json" },
            });
          }
          state.urls.push(url);
          // 403, not 401: an unpublished log is the realistic
          // authenticated-but-denied shape, and it also covers the second
          // half of the `status === 401 || status === 403` branch.
          return new Response(JSON.stringify({ error: "Forbidden" }), {
            status: 403,
            headers: { "Content-Type": "application/json" },
          });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () => html`
    <cts-log-viewer
      test-id="test-unpublished-log"
      is-public
      ._pollIntervalMs=${20}
    ></cts-log-viewer>
  `,
  async play({ canvasElement, step }) {
    const state = window.__ctsLogViewerFetchState;
    try {
      await step("anonymous viewers never see a 'session expired' message", async () => {
        await waitFor(
          () => {
            const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
            expect(banner).toBeTruthy();
            expect(banner.getAttribute("data-error-kind")).toBe("access-denied");
          },
          { timeout: 3000 },
        );
        const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
        // There is no session on the public view, so "expired" is wrong.
        expect(banner.textContent).toContain("Access to this log was refused");
        expect(banner.textContent).not.toContain("session");
        // And it must NOT blame unpublishing: LogApi.getTestResults returns
        // HTTP 200 with an empty list for an unpublished log, so a 401/403
        // here cannot have come from the application.
        expect(banner.textContent).not.toContain("unpublish");
      });
      await step("polling stops", async () => {
        await expectPollingStopped(state);
      });
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

export const GivesUpAfterSustainedFailure = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        /** @type {string[]} */
        urls: [],
        responder: (url) => {
          if (!url.includes("test-give-up")) {
            return new Response("[]", {
              status: 200,
              headers: { "Content-Type": "application/json" },
            });
          }
          state.urls.push(url);
          return new Response("Bad gateway", { status: 502 });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  // The budget starts large so the "retrying" phase cannot expire while the
  // play function is still starting up (a loaded CI runner can easily burn
  // several hundred ms before the first assertion). The play function then
  // shrinks it to 0, and the next failure flips deterministically — no
  // wall-clock window to miss.
  render: () => html`
    <cts-log-viewer
      test-id="test-give-up"
      ._pollIntervalMs=${FAST_POLL_MS}
      ._giveUpAfterMs=${60000}
    ></cts-log-viewer>
  `,
  async play({ canvasElement, step }) {
    const state = window.__ctsLogViewerFetchState;
    const viewer = canvasElement.querySelector("cts-log-viewer");
    try {
      await step("sustained 502s keep retrying under a healthy budget", async () => {
        await waitFor(
          () => {
            const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
            expect(banner).toBeTruthy();
            expect(banner.getAttribute("data-error-kind")).toBe("retrying");
          },
          { timeout: 3000 },
        );
        const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
        expect(banner.getAttribute("variant")).toBe("warning");
      });
      await step("retries back off geometrically up to the 4x cap", async () => {
        // White-box: timing assertions on the real scheduler would be flaky,
        // but the delay function itself is pure. Restore the live counter
        // afterwards so the give-up step below runs against real state.
        const live = viewer._consecutiveFailures;
        const delays = [0, 1, 2, 3, 4, 9].map((n) => {
          viewer._consecutiveFailures = n;
          return viewer._nextPollDelay();
        });
        viewer._consecutiveFailures = live;
        expect(delays).toEqual([20, 20, 40, 80, 80, 80]);
      });
      await step("budget exhausted: banner turns terminal", async () => {
        viewer._giveUpAfterMs = 0;
        await waitFor(
          () => {
            const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
            expect(banner).toBeTruthy();
            expect(banner.getAttribute("data-error-kind")).toBe("gave-up");
          },
          { timeout: 3000 },
        );
        const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
        expect(banner.getAttribute("variant")).toBe("danger");
        expect(banner.textContent).toContain("Automatic retrying has stopped");
        expect(canvasElement.querySelector('[data-testid="log-viewer-reload"]')).toBeTruthy();
      });
      await step("polling stops", async () => {
        await expectPollingStopped(state);
      });
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

export const PrivateLinkExpired = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        /** @type {string[]} */
        urls: [],
        responder: (url) => {
          if (!url.includes("test-private-link")) {
            return new Response("[]", {
              status: 200,
              headers: { "Content-Type": "application/json" },
            });
          }
          state.urls.push(url);
          return new Response(JSON.stringify({ error: "Unauthorized" }), {
            status: 401,
            headers: { "Content-Type": "application/json" },
          });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () => html`
    <cts-log-viewer
      test-id="test-private-link"
      is-guest
      ._pollIntervalMs=${FAST_POLL_MS}
    ></cts-log-viewer>
  `,
  async play({ canvasElement, step }) {
    const state = window.__ctsLogViewerFetchState;
    try {
      await step("share-link viewers are not told to sign in again", async () => {
        await waitFor(
          () => {
            const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
            expect(banner).toBeTruthy();
            expect(banner.getAttribute("data-error-kind")).toBe("private-link-expired");
          },
          { timeout: 3000 },
        );
        const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
        // A private-link viewer never signed in, and the one-time token is
        // no longer in the address bar — "sign in again" is advice they
        // cannot act on. Point them back at the link instead.
        expect(banner.textContent).toContain("private link is no longer valid");
        expect(banner.textContent).not.toContain("sign in again");
      });
      await step("no Reload affordance — reloading would blank their page", async () => {
        // The one-time token is gone from the address bar, so a reload drops
        // a share-link viewer to an unauthenticated page and throws away the
        // log still on screen. Retry stays: it costs one request and keeps
        // everything.
        expect(canvasElement.querySelector('[data-testid="log-viewer-reload"]')).toBeNull();
        expect(canvasElement.querySelector('[data-testid="log-viewer-retry"]')).toBeTruthy();
      });
      await step("polling stops", async () => {
        await expectPollingStopped(state);
      });
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

export const SingleAuthFailureIsTolerated = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        /** @type {string[]} */
        urls: [],
        responder: (url) => {
          if (!url.includes("test-transient-401")) {
            return new Response("[]", {
              status: 200,
              headers: { "Content-Type": "application/json" },
            });
          }
          state.urls.push(url);
          // Exactly one rejection, then healthy — the rolling-restart shape
          // that AUTH_FAILURE_THRESHOLD = 2 exists to absorb.
          if (state.urls.length === 1) {
            return new Response(JSON.stringify({ error: "Unauthorized" }), {
              status: 401,
              headers: { "Content-Type": "application/json" },
            });
          }
          return new Response(JSON.stringify(MOCK_SUCCESS_LOG), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  // testId is assigned imperatively in the play function, NOT via the
  // attribute. The attribute path starts two overlapping poll loops
  // (connectedCallback plus the first updated() cycle), which would land two
  // 401s at once and hide the very debounce this story exists to prove.
  // log-detail.js assigns testId the same way in production.
  render: () => html`<cts-log-viewer ._pollIntervalMs=${FAST_POLL_MS}></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    const viewer = canvasElement.querySelector("cts-log-viewer");
    try {
      await step("a lone 401 does not end the stream", async () => {
        viewer.testId = "test-transient-401";
        await waitFor(
          () => {
            expect(canvasElement.querySelectorAll(".logItem").length).toBeGreaterThan(0);
          },
          { timeout: 3000 },
        );
        expect(viewer._terminal).toBe("");
        expect(canvasElement.querySelector('[data-testid="log-viewer-error"]')).toBeNull();
      });
    } finally {
      stopViewer(canvasElement);
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

export const AuthRunBrokenByServerError = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        /** @type {string[]} */
        urls: [],
        responder: (url) => {
          if (!url.includes("test-alternating-401")) {
            return new Response("[]", {
              status: 200,
              headers: { "Content-Type": "application/json" },
            });
          }
          state.urls.push(url);
          // 401, 500, 401, 500, … — never two auth failures in a row, so the
          // run is broken every time and must never be called a dead session.
          return state.urls.length % 2 === 1
            ? new Response(JSON.stringify({ error: "Unauthorized" }), {
                status: 401,
                headers: { "Content-Type": "application/json" },
              })
            : new Response("Server error", { status: 500 });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () => html`<cts-log-viewer ._pollIntervalMs=${FAST_POLL_MS}></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    const viewer = canvasElement.querySelector("cts-log-viewer");
    try {
      await step("interleaved 500s keep the auth run broken", async () => {
        viewer.testId = "test-alternating-401";
        await waitFor(
          () => {
            const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
            expect(banner).toBeTruthy();
            expect(banner.getAttribute("data-error-kind")).toBe("retrying");
          },
          { timeout: 3000 },
        );
      });
      await step("still retrying after many alternating failures", async () => {
        await new Promise((resolve) => setTimeout(resolve, FAST_POLL_MS * 10));
        expect(viewer._terminal).toBe("");
        const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
        expect(banner.getAttribute("data-error-kind")).toBe("retrying");
      });
    } finally {
      stopViewer(canvasElement);
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

export const SuspendedTabDoesNotBurnTheBudget = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        /** @type {string[]} */
        urls: [],
        responder: (url) => {
          if (!url.includes("test-suspend-gap")) {
            return new Response("[]", {
              status: 200,
              headers: { "Content-Type": "application/json" },
            });
          }
          state.urls.push(url);
          return new Response("Bad gateway", { status: 502 });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () => html`
    <cts-log-viewer test-id="test-suspend-gap" ._pollIntervalMs=${FAST_POLL_MS}></cts-log-viewer>
  `,
  async play({ canvasElement, step }) {
    const viewer = canvasElement.querySelector("cts-log-viewer");
    try {
      await step("failures accumulate normally", async () => {
        await waitFor(
          () => {
            const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
            expect(banner).toBeTruthy();
            expect(banner.getAttribute("data-error-kind")).toBe("retrying");
          },
          { timeout: 3000 },
        );
      });
      await step("a frozen-tab gap re-seeds instead of exhausting the budget", async () => {
        // Simulate what a frozen background tab or a suspended laptop looks
        // like from inside the component: the clock jumped far past the
        // retry delay we asked for, without any retrying having happened.
        // Backdate both marks by an hour and squeeze the budget to 15s; the
        // elapsed-time reading alone would say "gave up an hour ago".
        viewer._giveUpAfterMs = 15000;
        viewer._firstFailureAt -= 3600000;
        viewer._lastFailureAt -= 3600000;
        viewer._recordFailure(new Error("network down"));
        expect(viewer._terminal).toBe("");
        // Budget restarted from the resumed failure, not the pre-sleep one.
        expect(viewer._lastFailureAt - viewer._firstFailureAt).toBe(0);
      });
    } finally {
      stopViewer(canvasElement);
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

export const ResumeGapThresholdIsSizedForBackgroundTabs = {
  parameters: pollingStoryParameters,
  // No test-id: nothing is fetched, so _recordFailure can be driven directly
  // with the production poll interval. This is a unit test of the threshold
  // arithmetic wearing a story's clothes — real timers cannot express a
  // 50-second gap in a play function.
  render: () => html`<cts-log-viewer></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    const viewer = canvasElement.querySelector("cts-log-viewer");
    // Production cadence, and enough failures that the backoff has reached
    // its cap — the widest delay is what the threshold is measured against.
    viewer._pollIntervalMs = 3000;
    viewer._consecutiveFailures = 5;
    const cappedDelay = viewer._nextPollDelay();
    const threshold = cappedDelay * RESUME_GAP_MULTIPLIER;

    /**
     * Backdate the failure marks by `gapMs`, record one more failure, and
     * report whether the budget was re-seeded.
     * @param {number} gapMs Simulated gap since the previous failure.
     * @returns {boolean} True when the budget restarted from this failure.
     */
    const reSeededAfterGap = (gapMs) => {
      viewer._terminal = "";
      viewer._consecutiveFailures = 5;
      const base = performance.now();
      viewer._firstFailureAt = base - gapMs;
      viewer._lastFailureAt = base - gapMs;
      viewer._recordFailure(new Error("network down"));
      // A re-seed moves _firstFailureAt up to the current failure, collapsing
      // the span to ~0; carrying the budget forward leaves it at ~gapMs.
      return viewer._lastFailureAt - viewer._firstFailureAt < gapMs / 2;
    };

    await step("the threshold sits in the band that makes it meaningful", async () => {
      // Both bounds are absolute, because an assertion phrased in terms of
      // `threshold` moves with the constant it is supposed to pin.
      //
      // Lower: browsers clamp hidden-tab timers to roughly one wake per
      // minute, so a 50s gap must not read as a resume — if it did, a
      // backgrounded tab would re-seed on every wake and the ceiling would
      // never fire in the "left open overnight" case it exists for.
      expect(threshold).toBeGreaterThan(50000);
      // Upper: a threshold of minutes means almost no gap counts as a
      // resume, which disables the give-up ceiling from the other side —
      // the tab would sit in "retrying…" indefinitely again.
      expect(threshold).toBeLessThan(5 * 60 * 1000);
      expect(reSeededAfterGap(50000)).toBe(false);
    });

    await step("a genuine suspend still re-seeds", async () => {
      // The mechanism itself: past its own threshold the page really was
      // not retrying, and charging that dead time would give up on a
      // connection that is healthy two seconds later.
      expect(reSeededAfterGap(threshold * 2)).toBe(true);
    });
  },
};

export const RetryResumesPolling = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        /** @type {string[]} */
        urls: [],
        healthy: false,
        responder: (url) => {
          if (!url.includes("test-retry-resume")) {
            return new Response("[]", {
              status: 200,
              headers: { "Content-Type": "application/json" },
            });
          }
          state.urls.push(url);
          if (state.healthy) {
            return new Response(JSON.stringify(MOCK_SUCCESS_LOG), {
              status: 200,
              headers: { "Content-Type": "application/json" },
            });
          }
          // A WAF/proxy 403 is the case the terminal decision can get wrong:
          // it looks exactly like a dead session but clears on its own.
          return new Response("Forbidden", { status: 403 });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () => html`
    <cts-log-viewer test-id="test-retry-resume" ._pollIntervalMs=${FAST_POLL_MS}></cts-log-viewer>
  `,
  async play({ canvasElement, step }) {
    const state = window.__ctsLogViewerFetchState;
    try {
      await step("403s stop the stream and announce it to the page", async () => {
        /** @type {string[]} */
        const stopped = [];
        const record = (/** @type {Event} */ e) => stopped.push(e.type);
        document.addEventListener("cts-log-polling-stopped", record);
        await waitFor(
          () => {
            const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
            expect(banner).toBeTruthy();
            expect(banner.getAttribute("data-error-kind")).toBe("session-expired");
          },
          { timeout: 3000 },
        );
        await expectPollingStopped(state);
        document.removeEventListener("cts-log-polling-stopped", record);
        expect(stopped).toEqual(["cts-log-polling-stopped"]);
      });
      await step("Try again resumes without a reload once the block clears", async () => {
        state.healthy = true;
        // The stop/resume pair is a contract with the host page: log-detail.js
        // shuts its runner poll down on `stopped` and must restart it on
        // `resumed`, or the log revives while the header stays frozen.
        /** @type {string[]} */
        const lifecycle = [];
        const record = (/** @type {Event} */ e) => lifecycle.push(e.type);
        document.addEventListener("cts-log-polling-resumed", record);
        const retry = canvasElement.querySelector('[data-testid="log-viewer-retry"] button');
        expect(retry).toBeTruthy();
        await userEvent.click(retry);
        document.removeEventListener("cts-log-polling-resumed", record);
        expect(lifecycle).toEqual(["cts-log-polling-resumed"]);
        await waitFor(
          () => {
            expect(canvasElement.querySelector('[data-testid="log-viewer-error"]')).toBeNull();
            expect(canvasElement.querySelectorAll(".logItem").length).toBeGreaterThan(0);
          },
          { timeout: 3000 },
        );
      });
    } finally {
      stopViewer(canvasElement);
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

// --- Public-mode fetch threading ---
// Plan: docs/plans/2026-06-03-003-fix-log-detail-public-mode-plan.md
// Anonymous viewers of published logs only pass the security filter's
// public matcher when the request carries `public=true` — on EVERY poll
// cycle, not just the first fetch. The programmable-fetch responder
// records each requested URL so the play function can assert the exact
// query-param composition (`public` alone first, `public` + `since` on
// subsequent polls).

export const PublicModePolling = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        /** @type {string[]} */
        urls: [],
        responder: (url) => {
          // Scope recording to THIS story's viewer — a previous story's
          // fast-polling viewer can still be mid-teardown when this
          // story patches fetch, and its stray poll must not pollute the
          // recorded URL sequence.
          if (!url.includes("test-public-001")) {
            return new Response("[]", {
              status: 200,
              headers: { "Content-Type": "application/json" },
            });
          }
          state.urls.push(url);
          // Entries on the first fetch (sets _latestTimestamp so the next
          // poll adds `since`); empty array on subsequent polls.
          const body = state.urls.length === 1 ? MOCK_SUCCESS_LOG : [];
          return new Response(JSON.stringify(body), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () => html`
    <cts-log-viewer test-id="test-public-001" is-public ._pollIntervalMs=${20}></cts-log-viewer>
  `,
  async play({ canvasElement, step }) {
    const state = window.__ctsLogViewerFetchState;
    try {
      await waitForLogLoad(canvasElement);
      await step("first fetch carries public=true, no since", async () => {
        // Nothing cached yet.
        expect(state.urls[0]).toBe("/api/log/test-public-001?public=true");
        // Entries render for the anonymous viewer.
        expect(canvasElement.querySelectorAll(".logItem").length).toBeGreaterThan(0);
      });
      await step("poll cycle keeps public=true alongside since=<ts>", async () => {
        // Wait for the first poll that carries `since` (attribute-mounted
        // viewers issue a duplicate initial fetch — connectedCallback and
        // the first updated() cycle both fire before either resolves — so
        // the since-bearing poll is not necessarily urls[1]).
        await waitFor(
          () => {
            expect(state.urls.some((u) => u.includes("since="))).toBe(true);
          },
          { timeout: 3000 },
        );
        const pollUrl = new URL(
          state.urls.find((u) => u.includes("since=")),
          window.location.origin,
        );
        expect(pollUrl.pathname).toBe("/api/log/test-public-001");
        expect(pollUrl.searchParams.get("public")).toBe("true");
        expect(Number(pollUrl.searchParams.get("since"))).toBeGreaterThan(0);
      });
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

export const DefaultModeOmitsPublicParam = {
  decorators: [
    (storyFn) => {
      const state = {
        /** @type {string[]} */
        urls: [],
        responder: (url) => {
          // Same stray-poll scoping as PublicModePolling above.
          if (!url.includes("test-default-001")) {
            return new Response("[]", {
              status: 200,
              headers: { "Content-Type": "application/json" },
            });
          }
          state.urls.push(url);
          return new Response(JSON.stringify(MOCK_SUCCESS_LOG), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () => html`<cts-log-viewer test-id="test-default-001"></cts-log-viewer>`,
  async play({ canvasElement }) {
    const state = window.__ctsLogViewerFetchState;
    try {
      await waitForLogLoad(canvasElement);
      // Authenticated view: bare URL, no query string (R4 regression guard).
      expect(state.urls[0]).toBe("/api/log/test-default-001");
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

// --- U1: log-detail page integration ---
// Plan: docs/plans/2026-04-26-002-refactor-log-detail-page-to-lit-triad-plan.md
// MountedFromExistingPage simulates how log-detail.js mounts the viewer:
// the bootstrap pre-fetches /api/info and passes the result via the new
// `testInfo` reactive property, then the viewer's first /api/log poll fires
// the cts-first-fetch-resolved event so the page can defer hash-anchor
// scrolling until rows are in the DOM.

export const MountedFromExistingPage = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () =>
    html`<cts-log-viewer test-id="test-inst-001" .testInfo=${MOCK_TEST_STATUS}></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    await waitForLogLoad(canvasElement);

    await step("testInfo flows through unchanged", async () => {
      // The viewer doesn't render it (the header owns metadata rendering)
      // but consumers may read it.
      const viewer = canvasElement.querySelector("cts-log-viewer");
      expect(viewer.testInfo).toEqual(MOCK_TEST_STATUS);
    });

    await step("entries rendered as usual", async () => {
      const entries = canvasElement.querySelectorAll(".logItem");
      expect(entries.length).toBeGreaterThan(0);
    });
  },
};

// Note: cts-first-fetch-resolved is a forward-looking hook for U6 (R32
// reference IDs + deep-URL hash navigation). The event wiring is covered
// by source review and JSDoc; runtime verification will land alongside
// U6's hash-navigation handler that consumes the event. Adding a story
// today that races the listener against the synchronous-microtask first
// fetch was flaky in vitest browser mode (the event sometimes fires
// before the test runner's listener attaches), so coverage is deferred
// to the consumer.

// --- U3: container-query reflow at narrow widths ---
// Plan: docs/plans/2026-04-26-004-feat-log-entry-container-query-reflow-plan.md
// Renders the viewer at 360px container width so each cts-log-entry triggers
// its small-layout reflow. Validates that the rail of entries (and any
// start-block headers) stack legibly without horizontal overflow when the
// container is narrower than the 640px container-query threshold.

export const MobileContainer = {
  decorators: [
    withMockFetch("/api/log/", MOCK_LOG_ENTRIES),
    (storyFn) => html`
      <div
        style="width: 360px; max-width: 100%; border: 1px dashed var(--ink-300); resize: horizontal; overflow: auto;"
      >
        ${storyFn()}
      </div>
    `,
  ],
  render: () => html`<cts-log-viewer test-id="test-mobile-001"></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    await waitForLogLoad(canvasElement);

    const entries = canvasElement.querySelectorAll(".logItem");
    await step("entries render", async () => {
      expect(entries.length).toBeGreaterThan(0);
    });

    await step("each row uses the small 2-track grid", async () => {
      // Every entry's container query is satisfied (host width < 640px), so
      // each row should be on the small grid (1fr auto) — not the wide
      // 5-track layout. Sample the first one.
      const firstItem = entries[0];
      const style = getComputedStyle(firstItem);
      const tracks = style.gridTemplateColumns.split(/\s+/).filter(Boolean);
      expect(tracks.length).toBe(2);
    });

    await step("no horizontal overflow on the entries stream", async () => {
      // The regression this story is meant to catch.
      const stream = canvasElement.querySelector(".logEntries") ?? entries[0].parentElement;
      if (stream) {
        expect(stream.scrollWidth).toBeLessThanOrEqual(stream.clientWidth);
      }
    });
  },
};

// --- U5: per-block status aggregation ---
// Plan: docs/plans/2026-04-26-006-feat-r27-per-block-status-aggregation-plan.md

export const BlocksWithStatus = {
  decorators: [withMockFetch("/api/log/", MOCK_BLOCKS_WITH_STATUS)],
  render: () => html`<cts-log-viewer test-id="test-blocks-001"></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    await waitForLogLoad(canvasElement);

    const blockA = canvasElement.querySelector('[data-block-id="block-a"]');
    const blockB = canvasElement.querySelector('[data-block-id="block-b"]');
    const blockC = canvasElement.querySelector('[data-block-id="block-c"]');

    await step("three blocks render, each with a header", async () => {
      // Three .logBlock divs rendered, each with a .startBlock header.
      const blocks = canvasElement.querySelectorAll(".logBlock");
      expect(blocks.length).toBe(3);
      expect(blockA).toBeTruthy();
      expect(blockB).toBeTruthy();
      expect(blockC).toBeTruthy();
    });

    await step("block A: 2 successes → single ✓2 chip", async () => {
      const aBadges = blockA.querySelectorAll(".startBlockCounts cts-badge");
      expect(aBadges.length).toBe(1);
      expect(aBadges[0].getAttribute("variant")).toBe("pass");
      expect(aBadges[0].getAttribute("label")).toBe("✓2");
    });

    await step("block B: 1 success + 1 failure → ✓1 then ✗1 in spec order", async () => {
      const bBadges = blockB.querySelectorAll(".startBlockCounts cts-badge");
      expect(bBadges.length).toBe(2);
      expect(bBadges[0].getAttribute("variant")).toBe("pass");
      expect(bBadges[0].getAttribute("label")).toBe("✓1");
      expect(bBadges[1].getAttribute("variant")).toBe("fail");
      expect(bBadges[1].getAttribute("label")).toBe("✗1");
    });

    await step("block C: 1 warning + 1 info → only ⚠1 (INFO excluded by design)", async () => {
      const cBadges = blockC.querySelectorAll(".startBlockCounts cts-badge");
      expect(cBadges.length).toBe(1);
      expect(cBadges[0].getAttribute("variant")).toBe("warn");
      expect(cBadges[0].getAttribute("label")).toBe("⚠1");
    });
  },
};

export const EmptyBlock = {
  decorators: [withMockFetch("/api/log/", MOCK_EMPTY_BLOCK)],
  render: () => html`<cts-log-viewer test-id="test-empty-block-001"></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    await waitForLogLoad(canvasElement);

    const block = canvasElement.querySelector('[data-block-id="block-empty"]');

    await step("empty block has no badges in the cluster", async () => {
      expect(block).toBeTruthy();
      // No children → graceful empty state.
      const badges = block.querySelectorAll(".startBlockCounts cts-badge");
      expect(badges.length).toBe(0);
    });

    await step("header still renders the text from msg", async () => {
      const header = block.querySelector(".startBlock");
      expect(header.textContent).toContain("Awaiting checks");
    });
  },
};

export const BlockCountsUpdateOnPolling = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        callCount: 0,
        responder: function () {
          this.callCount += 1;
          // First poll returns 3 entries (block start + 2 successes);
          // every subsequent poll returns the second batch (the third
          // success + failure). The viewer uses `since` to dedupe but
          // appends new entries; returning the same delta repeatedly is
          // fine for the assertion (we only need to observe the
          // ✓2 → ✓3 ✗1 transition once).
          const body = this.callCount === 1 ? MOCK_BLOCKS_POLL_FIRST : MOCK_BLOCKS_POLL_SECOND;
          return new Response(JSON.stringify(body), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          });
        },
      };
      // The fetch mock is left in place across the polling cycle; the
      // play function restores real fetch in its finally block.
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () =>
    html`<cts-log-viewer test-id="test-poll-001" ._pollIntervalMs=${20}></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    try {
      await step("first poll: badges land on ✓2 (single chip)", async () => {
        await waitFor(
          () => {
            const block = canvasElement.querySelector('[data-block-id="block-poll"]');
            expect(block).toBeTruthy();
            const badges = block.querySelectorAll(".startBlockCounts cts-badge");
            expect(badges.length).toBe(1);
            expect(badges[0].getAttribute("label")).toBe("✓2");
          },
          { timeout: 1500 },
        );
      });

      await step("second poll: cluster transitions to ✓3 ✗1", async () => {
        await waitFor(
          () => {
            const block = canvasElement.querySelector('[data-block-id="block-poll"]');
            const badges = block.querySelectorAll(".startBlockCounts cts-badge");
            expect(badges.length).toBe(2);
            expect(badges[0].getAttribute("label")).toBe("✓3");
            expect(badges[1].getAttribute("label")).toBe("✗1");
          },
          { timeout: 2000 },
        );
      });
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
    }
  },
};

/**
 * U6: when the page loads with a `#LOG-NNNN` URL fragment, the viewer
 * scrolls to the matching entry after its first successful fetch
 * resolves. Storybook can't manipulate the real address bar, so the
 * story uses the History API to seed a hash before mounting and spies
 * on `Element.prototype.scrollIntoView` to confirm the right entry was
 * targeted (the test environment doesn't actually scroll fixed-height
 * Storybook canvases).
 */
export const InitialLoadHashScroll = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () => html`<cts-log-viewer test-id="test-abc-123"></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    // Seed the hash *before* the viewer's first fetch resolves. The
    // first row in MOCK_LOG_ENTRIES becomes LOG-0001, etc.; LOG-0003
    // points at the third entry.
    const previousHash = window.location.hash;
    history.replaceState(null, "", "#LOG-0003");

    /** @type {Set<HTMLElement>} */
    const scrolled = new Set();
    const realScrollIntoView = Element.prototype.scrollIntoView;
    Element.prototype.scrollIntoView = /** @type {Element["scrollIntoView"]} */ (
      function () {
        // scrollEntryIntoView may scroll the painted .logItem INSIDE the
        // host (the host is display:contents at the wide layout) —
        // attribute the call to the owning entry so assertions can keep
        // targeting hosts by id.
        scrolled.add(/** @type {HTMLElement} */ (this.closest("cts-log-entry") ?? this));
        // Don't actually scroll — the canvas iframe's geometry trips up
        // other tests in the same suite if we yield real scroll motion.
      }
    );

    try {
      await waitForLogLoad(canvasElement);

      await step("the seeded #LOG-0003 anchor scrolls after the first fetch", async () => {
        // Wait for the deferred (microtask + updateComplete) hash scroll.
        await waitFor(() => {
          const target = /** @type {HTMLElement | null} */ (
            canvasElement.querySelector("#LOG-0003")
          );
          expect(target).toBeTruthy();
          if (target) expect(scrolled.has(target)).toBe(true);
        });
      });

      await step("the host carries id=LOG-0003 (mirrored from referenceId)", async () => {
        const target = canvasElement.querySelector("#LOG-0003");
        if (!target) throw new Error("anchor target not present");
        expect(target.tagName).toBe("CTS-LOG-ENTRY");
      });
    } finally {
      Element.prototype.scrollIntoView = realScrollIntoView;
      history.replaceState(null, "", previousHash || " ");
    }
  },
};

/**
 * Late-arriving target: the hash points at an entry that is NOT in the
 * first poll's payload but arrives in a later poll. The fixed-once gate
 * used to give up after the first fetch, so the row never scrolled. The
 * retry-until-success gate keeps trying each poll and scrolls once the
 * row finally lands. Poll 1 yields LOG-0001..0003; poll 2 appends
 * LOG-0004..0005, so #LOG-0005 only resolves after the second poll.
 */
export const LateArrivalHashScroll = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        callCount: 0,
        responder: function () {
          this.callCount += 1;
          const body = this.callCount === 1 ? MOCK_BLOCKS_POLL_FIRST : MOCK_BLOCKS_POLL_SECOND;
          return new Response(JSON.stringify(body), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          });
        },
      };
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () =>
    html`<cts-log-viewer test-id="test-late-001" ._pollIntervalMs=${20}></cts-log-viewer>`,
  async play({ canvasElement }) {
    const previousHash = window.location.hash;
    history.replaceState(null, "", "#LOG-0005");

    /** @type {Set<HTMLElement>} */
    const scrolled = new Set();
    const realScrollIntoView = Element.prototype.scrollIntoView;
    Element.prototype.scrollIntoView = /** @type {Element["scrollIntoView"]} */ (
      function () {
        // scrollEntryIntoView may scroll the painted .logItem INSIDE the
        // host (the host is display:contents at the wide layout) —
        // attribute the call to the owning entry so assertions can keep
        // targeting hosts by id.
        scrolled.add(/** @type {HTMLElement} */ (this.closest("cts-log-entry") ?? this));
      }
    );

    try {
      // LOG-0005 only exists after the SECOND poll appends it. The old
      // one-shot gate gave up after poll 1, so the row would appear but
      // never scroll; the retry gate keeps trying and scrolls it once it
      // lands. Asserting the row is BOTH present AND scrolled is the
      // discriminating check for retry-until-success.
      await waitFor(
        () => {
          const target = /** @type {HTMLElement | null} */ (
            canvasElement.querySelector("#LOG-0005")
          );
          expect(target).toBeTruthy();
          if (target) expect(scrolled.has(target)).toBe(true);
        },
        { timeout: 2500 },
      );
    } finally {
      Element.prototype.scrollIntoView = realScrollIntoView;
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      history.replaceState(null, "", previousHash || " ");
    }
  },
};

/**
 * In-page fragment change after load — the path a timestamp deep-link
 * click takes. The viewer listens for `hashchange` and runs the scroll
 * routine, so clicking an entry's timestamp (which sets `location.hash`)
 * scrolls to that entry without a reload.
 */
export const HashChangeScroll = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () => html`<cts-log-viewer test-id="test-hashchange-1"></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    const previousHash = window.location.hash;

    /** @type {Set<HTMLElement>} */
    const scrolled = new Set();
    const realScrollIntoView = Element.prototype.scrollIntoView;
    Element.prototype.scrollIntoView = /** @type {Element["scrollIntoView"]} */ (
      function () {
        // scrollEntryIntoView may scroll the painted .logItem INSIDE the
        // host (the host is display:contents at the wide layout) —
        // attribute the call to the owning entry so assertions can keep
        // targeting hosts by id.
        scrolled.add(/** @type {HTMLElement} */ (this.closest("cts-log-entry") ?? this));
      }
    );

    try {
      await waitForLogLoad(canvasElement);
      await step("page loaded with no hash → nothing scrolled yet", async () => {
        expect(scrolled.size).toBe(0);
      });

      await step("navigating the fragment scrolls to the targeted row", async () => {
        // Simulate a timestamp deep-link click by navigating the fragment.
        window.location.hash = "#LOG-0006";

        await waitFor(() => {
          const target = /** @type {HTMLElement | null} */ (
            canvasElement.querySelector("#LOG-0006")
          );
          expect(target).toBeTruthy();
          if (target) expect(scrolled.has(target)).toBe(true);
        });
      });
    } finally {
      Element.prototype.scrollIntoView = realScrollIntoView;
      if (previousHash) window.location.hash = previousHash;
      else history.replaceState(null, "", window.location.pathname + window.location.search);
    }
  },
};

/**
 * A fragment change targeting a row inside a block scrolls that row into
 * view. Blocks are not collapsible, so the row is always in the layout —
 * the scroll routine finds it directly with no ancestor-reveal step.
 */
export const ScrollToRowInsideBlock = {
  decorators: [withMockFetch("/api/log/", MOCK_BLOCKS_WITH_STATUS)],
  render: () => html`<cts-log-viewer test-id="test-block-scroll-1"></cts-log-viewer>`,
  async play({ canvasElement }) {
    const previousHash = window.location.hash;

    /** @type {Set<HTMLElement>} */
    const scrolled = new Set();
    const realScrollIntoView = Element.prototype.scrollIntoView;
    Element.prototype.scrollIntoView = /** @type {Element["scrollIntoView"]} */ (
      function () {
        // scrollEntryIntoView may scroll the painted .logItem INSIDE the
        // host (the host is display:contents at the wide layout) —
        // attribute the call to the owning entry so assertions can keep
        // targeting hosts by id.
        scrolled.add(/** @type {HTMLElement} */ (this.closest("cts-log-entry") ?? this));
      }
    );

    try {
      await waitForLogLoad(canvasElement);
      const block = /** @type {HTMLElement | null} */ (
        canvasElement.querySelector('[data-block-id="block-a"]')
      );
      if (!block) throw new Error("block-a did not render");

      // Target a row inside the block (blk-a-1 → LOG-0002). It is already
      // visible — no collapse to undo.
      window.location.hash = "#LOG-0002";

      await waitFor(() => {
        const target = /** @type {HTMLElement | null} */ (canvasElement.querySelector("#LOG-0002"));
        expect(target).toBeTruthy();
        if (target) expect(scrolled.has(target)).toBe(true);
      });
    } finally {
      Element.prototype.scrollIntoView = realScrollIntoView;
      if (previousHash) window.location.hash = previousHash;
      else history.replaceState(null, "", window.location.pathname + window.location.search);
    }
  },
};

/**
 * An out-of-range or malformed fragment is a graceful no-op: the viewer
 * does not throw and does not scroll. `#LOG-9999` on a 10-entry log has no
 * matching host.
 */
export const OutOfRangeHashNoop = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () => html`<cts-log-viewer test-id="test-oob-1"></cts-log-viewer>`,
  async play({ canvasElement }) {
    const previousHash = window.location.hash;

    /** @type {Set<HTMLElement>} */
    const scrolled = new Set();
    const realScrollIntoView = Element.prototype.scrollIntoView;
    Element.prototype.scrollIntoView = /** @type {Element["scrollIntoView"]} */ (
      function () {
        // scrollEntryIntoView may scroll the painted .logItem INSIDE the
        // host (the host is display:contents at the wide layout) —
        // attribute the call to the owning entry so assertions can keep
        // targeting hosts by id.
        scrolled.add(/** @type {HTMLElement} */ (this.closest("cts-log-entry") ?? this));
      }
    );

    try {
      await waitForLogLoad(canvasElement);

      // No matching host for an out-of-range ordinal.
      window.location.hash = "#LOG-9999";
      await waitFor(() => expect(canvasElement.querySelector("#LOG-0001")).toBeTruthy());
      expect(canvasElement.querySelector("#LOG-9999")).toBeNull();
      expect(scrolled.size).toBe(0);
    } finally {
      Element.prototype.scrollIntoView = realScrollIntoView;
      if (previousHash) window.location.hash = previousHash;
      else history.replaceState(null, "", window.location.pathname + window.location.search);
    }
  },
};

/**
 * Wide-layout column alignment between block (is-block) entries and
 * top-level entries. Regression guard for the subgrid relay: each
 * .logBlock is a subgrid that relays the master grid's tracks, and each
 * nested .logItem subgrids into it. The net effect is that every row —
 * block or not — shares one set of column positions.
 *
 * The block was previously a <details>, whose UA ::details-content wrapper
 * broke this relay (it defaulted to display: block, collapsing the subgrid
 * to one column). De-collapsing to a plain <div> removed that wrapper, so
 * the relay now propagates with no neutralising hack — this story guards
 * against the alignment regressing if the .logBlock subgrid rule is lost.
 *
 * The fixture deliberately gives block rows DIFFERING badge widths (no
 * marker / wide REDIRECT-IN / REQUEST icon, plus SUCCESS/WARNING/FAILURE
 * severities). Under a per-entry max-content grid each row would size its
 * own columns, so the message column would drift row-to-row. The story
 * asserts the message column starts at the same x on every block row AND
 * matches the top-level reference row.
 */
export const AlignedBlocks = {
  decorators: [withMockFetch("/api/log/", MOCK_BLOCKS_ALIGN)],
  render: () => html`<cts-log-viewer test-id="test-blocks-align-001"></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    await waitForLogLoad(canvasElement);

    const left = (el) => Math.round(el.getBoundingClientRect().left);

    // The leading non-block entry is a direct child of .logEntries.
    const topItem = /** @type {HTMLElement} */ (
      canvasElement.querySelector(".logEntries > cts-log-entry > .logItem")
    );
    // Block rows live one level deeper, inside the .logBlock div.
    const block = /** @type {HTMLElement} */ (canvasElement.querySelector(".logBlock"));
    const blockItems = [...block.querySelectorAll(":scope > cts-log-entry > .logItem")];

    await step("top-level and block rows render", async () => {
      expect(topItem).toBeTruthy();
      expect(block).toBeTruthy();
      expect(blockItems.length).toBe(3);
    });

    await step("message columns align across block and top-level rows", async () => {
      // Badges (cts-badge) render asynchronously after the rows mount, and
      // the subgrid relay settles a frame after the top-level direct subgrid.
      // Both feed the auto track widths, so poll the geometry until it
      // stabilises rather than measuring a single (possibly mid-render) frame.
      await waitFor(
        () => {
          const topBody = /** @type {HTMLElement} */ (topItem.querySelector(".logBody"));
          const topTime = /** @type {HTMLElement} */ (topItem.querySelector(".logTime"));

          // The container query must be active (canvas is wide in the
          // runner): the message column sits past the 92px timestamp
          // track, not crammed against the row's left edge. This is the
          // discriminating check for the "cells collapsed into column 1"
          // regression.
          expect(left(topBody) - left(topTime)).toBeGreaterThan(90);

          // Every block row's message column starts at the same x (exact
          // within-block alignment) AND matches the top-level reference
          // row (exact cross-boundary alignment via the subgrid relay).
          // 1px of slack absorbs sub-pixel track rounding only.
          for (const item of blockItems) {
            const body = /** @type {HTMLElement} */ (item.querySelector(".logBody"));
            const time = /** @type {HTMLElement} */ (item.querySelector(".logTime"));
            expect(Math.abs(left(body) - left(topBody))).toBeLessThanOrEqual(1);
            expect(Math.abs(left(time) - left(topTime))).toBeLessThanOrEqual(1);
            // Not collapsed: the message still sits past the timestamp track.
            expect(left(body) - left(time)).toBeGreaterThan(90);
          }
        },
        { timeout: 3000 },
      );
    });

    await step("block rows are always rendered and visible (no collapse)", async () => {
      for (const item of blockItems) {
        expect(item.checkVisibility()).toBe(true);
      }
    });
  },
};

export const SingleFetchChainOnAttributePath = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      const state = {
        callCount: 0,
        responder: function () {
          this.callCount += 1;
          return new Response(JSON.stringify(MOCK_EMPTY_LOG), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  // 100s interval: a timer-driven second fetch is impossible inside the
  // assertion window, so callCount > 1 can only mean a second kickoff.
  render: () => html`
    <cts-log-viewer test-id="test-single-chain" ._pollIntervalMs=${100000}></cts-log-viewer>
  `,
  async play() {
    const state = window.__ctsLogViewerFetchState;
    try {
      // The attribute path starts polling in connectedCallback; the
      // updated() kickoff exists only for the imperative-testId path and
      // must NOT start a second chain here. A double kickoff lands within
      // microtasks of mount (it is not timer-driven), so 300ms is ample.
      await waitFor(() => expect(state.callCount).toBeGreaterThanOrEqual(1), {
        timeout: 2000,
      });
      await new Promise((r) => setTimeout(r, 300));
      expect(state.callCount).toBe(1);
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

export const DisconnectStopsPolling = {
  decorators: [
    (storyFn) => {
      const state = {
        callCount: 0,
        responder: function () {
          this.callCount += 1;
          return new Response(JSON.stringify(MOCK_EMPTY_LOG), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () => html`
    <div id="log-viewer-host">
      <cts-log-viewer test-id="test-disconnect-log" ._pollIntervalMs=${20}></cts-log-viewer>
    </div>
  `,
  async play({ canvasElement }) {
    const state = window.__ctsLogViewerFetchState;
    try {
      const el = canvasElement.querySelector("cts-log-viewer");
      // Wait for the first few polls to fire.
      await waitFor(() => expect(state.callCount).toBeGreaterThanOrEqual(3), {
        timeout: 2000,
      });
      el.remove();
      // Drain: a poll whose timer had already fired (or whose fetch was in
      // flight) at remove() time finishes inside this window — its finally
      // sees isConnected === false and schedules nothing further.
      await new Promise((r) => setTimeout(r, 100));
      const settledCount = state.callCount;
      // The invariant under test: once drained, polling has fully stopped,
      // so the count must not grow across ten further poll intervals. A
      // "no further growth" assertion (rather than an absolute bound
      // relative to the pre-remove count) is immune to scheduler jitter on
      // loaded CI runners — the previous countAtDisconnect + 1 bound flaked
      // when two queued cycles drained instead of one.
      await new Promise((r) => setTimeout(r, 200));
      expect(state.callCount).toBe(settledCount);
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

// ──────────────────────────────────────────────────────────────────────
// Result-summary filter (U2/U3)
// Plan: docs/plans/2026-05-28-001-feat-log-result-summary-filter-plan.md
// The count badges in .logResultSummary become multi-select toggle filters
// that narrow the rendered entry stream. The model stays unfiltered, so
// LOG-NNNN ordinals, block counts, and the TOC are unaffected (R8).
// ──────────────────────────────────────────────────────────────────────

// Result-summary toggle badge for a given result type (e.g. "FAILURE").
// canvasElement is left untyped (any) to match the surrounding stories'
// play-function convention, so querySelector chains don't trip strict-null.
function summaryBadge(canvasElement, result) {
  return canvasElement.querySelector(
    `cts-log-viewer .logResultSummary cts-badge[data-result="${result}"]`,
  );
}

// Inner role="button" span of a result-summary toggle badge.
function summaryBadgeButton(canvasElement, result) {
  const badge = summaryBadge(canvasElement, result);
  return badge ? badge.querySelector(".badge") : null;
}

const visibleEntryIds = (canvasElement) =>
  [...canvasElement.querySelectorAll("cts-log-viewer cts-log-entry")].map((el) =>
    el.getAttribute("data-entry-id"),
  );

export const ResultSummaryFilter = {
  decorators: [withMockFetch("/api/log/", MOCK_BLOCKS_FILTERABLE)],
  render: () => html`<cts-log-viewer test-id="test-filter-001"></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    await waitForLogLoad(canvasElement);
    const viewer = canvasElement.querySelector("cts-log-viewer");
    const announce = canvasElement.querySelector("cts-log-viewer .logFilterAnnounce");

    await step("unfiltered baseline: both blocks, six entries, four toggle badges", async () => {
      expect(canvasElement.querySelectorAll("cts-log-viewer .logBlock").length).toBe(2);
      expect(visibleEntryIds(canvasElement).length).toBe(6);
      const group = canvasElement.querySelector("cts-log-viewer .logResultSummary");
      expect(group.getAttribute("role")).toBe("group");
      expect(group.getAttribute("aria-label")).toBe("Filter log entries by result");
      // Each badge is an activatable toggle button; until pressed it carries
      // no aria-pressed (a plain command button), so it is never mis-announced
      // as an inactive toggle.
      for (const r of ["SUCCESS", "FAILURE", "REVIEW", "WARNING"]) {
        const btn = summaryBadgeButton(canvasElement, r);
        expect(btn.getAttribute("role")).toBe("button");
        expect(btn.getAttribute("aria-pressed")).toBeNull();
      }
      // Not filtering yet → host has no is-filtering marker.
      expect(viewer.classList.contains("is-filtering")).toBe(false);
    });

    await step("toggle FAILURE narrows to the lone failure entry", async () => {
      // Block B (no failure) is elided entirely; the badge reads pressed; the
      // host flips into the filtering state (which mutes block-header counts
      // via CSS).
      await userEvent.click(summaryBadgeButton(canvasElement, "FAILURE"));
      await viewer.updateComplete;
      expect(summaryBadgeButton(canvasElement, "FAILURE").getAttribute("aria-pressed")).toBe(
        "true",
      );
      expect(visibleEntryIds(canvasElement)).toEqual(["flt-a-3"]);
      expect(canvasElement.querySelectorAll("cts-log-viewer .logBlock").length).toBe(1);
      expect(viewer.classList.contains("is-filtering")).toBe(true);
    });

    await step("count badges show the TRUE total, never the filtered subset", async () => {
      expect(summaryBadge(canvasElement, "SUCCESS").getAttribute("label")).toBe("SUCCESS (3)");
      expect(summaryBadge(canvasElement, "FAILURE").getAttribute("label")).toBe("FAILURE (1)");
    });

    await step("the live region announced the user action", async () => {
      // Description only — no entry count, so polling can't perturb it.
      expect(announce.textContent).toBe("Filtering by FAILURE");
    });

    await step("adding REVIEW unions failure (Block A) and review (Block B)", async () => {
      // One surviving child per block. Both badges pressed.
      await userEvent.click(summaryBadgeButton(canvasElement, "REVIEW"));
      await viewer.updateComplete;
      expect(summaryBadgeButton(canvasElement, "REVIEW").getAttribute("aria-pressed")).toBe("true");
      expect(new Set(visibleEntryIds(canvasElement))).toEqual(new Set(["flt-a-3", "flt-b-2"]));
      expect(canvasElement.querySelectorAll("cts-log-viewer .logBlock").length).toBe(2);
      expect(announce.textContent).toBe("Filtering by FAILURE, REVIEW");
    });

    await step("toggling FAILURE off (without clearing) leaves only the review", async () => {
      // The remove branch of the filter set. REVIEW stays active, so the
      // stream narrows to just the review entry, Block A (failure-only) is
      // elided again, FAILURE drops its pressed state, and filtering is still
      // active.
      await userEvent.click(summaryBadgeButton(canvasElement, "FAILURE"));
      await viewer.updateComplete;
      expect(summaryBadgeButton(canvasElement, "FAILURE").getAttribute("aria-pressed")).toBeNull();
      expect(summaryBadgeButton(canvasElement, "REVIEW").getAttribute("aria-pressed")).toBe("true");
      expect(visibleEntryIds(canvasElement)).toEqual(["flt-b-2"]);
      expect(canvasElement.querySelectorAll("cts-log-viewer .logBlock").length).toBe(1);
      expect(viewer.classList.contains("is-filtering")).toBe(true);
      expect(announce.textContent).toBe("Filtering by REVIEW");
    });

    await step("clearing restores the full stream and drops the filtering state", async () => {
      const clearBtn = canvasElement.querySelector("cts-log-viewer .logFilterClear");
      expect(clearBtn).toBeTruthy();
      await userEvent.click(clearBtn);
      await viewer.updateComplete;
      expect(visibleEntryIds(canvasElement).length).toBe(6);
      expect(canvasElement.querySelectorAll("cts-log-viewer .logBlock").length).toBe(2);
      expect(viewer.classList.contains("is-filtering")).toBe(false);
      expect(summaryBadgeButton(canvasElement, "REVIEW").getAttribute("aria-pressed")).toBeNull();
      expect(announce.textContent).toBe("Filters cleared");
    });

    await step("focus moves to the first toggle badge after clearing", async () => {
      // Clearing removes the Clear button from the DOM, so focus must not drop
      // to <body> — it moves to the first toggle badge.
      await viewer.updateComplete;
      expect(document.activeElement).toBe(summaryBadgeButton(canvasElement, "SUCCESS"));
    });
  },
};

/**
 * Empty state: when an active filter matches zero entries across the whole
 * log, an accessible empty-state message with an inline Clear affordance
 * renders inside .logEntries so the user isn't stranded when the top filter
 * row has scrolled away.
 *
 * This state is NOT reachable by toggling the count badges — a badge only
 * exists for a result type that has >=1 entry, so filtering by it always
 * matches >=1. It is the defensive path for an externally-driven filter
 * (the public clearFilters() / future reveal-on-navigate API). The test
 * therefore drives the reactive filter state directly to a result value
 * with no matching entries, mirroring that programmatic path.
 */
export const FilterEmptyState = {
  decorators: [withMockFetch("/api/log/", MOCK_BLOCKS_FILTERABLE)],
  render: () => html`<cts-log-viewer test-id="test-filter-001"></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    await waitForLogLoad(canvasElement);
    const viewer = /** @type {any} */ (canvasElement.querySelector("cts-log-viewer"));

    await step("a filter matching zero entries renders the empty state", async () => {
      // No SKIPPED entries exist in the fixture, so this filter matches nothing.
      viewer._activeFilters = new Set(["SKIPPED"]);
      await viewer.updateComplete;

      const empty = canvasElement.querySelector("cts-log-viewer .logFilterEmpty");
      expect(empty).toBeTruthy();
      expect(empty?.textContent).toContain("No entries match the active filters");
      // No entry rows and no blocks render in the empty state.
      expect(canvasElement.querySelectorAll("cts-log-viewer cts-log-entry").length).toBe(0);
      expect(canvasElement.querySelectorAll("cts-log-viewer .logBlock").length).toBe(0);
    });

    await step("the inline Clear affordance restores the full stream", async () => {
      const empty = canvasElement.querySelector("cts-log-viewer .logFilterEmpty");
      const inlineClear = empty?.querySelector(".logFilterClear");
      expect(inlineClear).toBeTruthy();
      await userEvent.click(/** @type {Element} */ (inlineClear));
      await viewer.updateComplete;
      expect(canvasElement.querySelectorAll("cts-log-viewer cts-log-entry").length).toBe(6);
      expect(canvasElement.querySelector("cts-log-viewer .logFilterEmpty")).toBeNull();
    });
  },
};

/**
 * Single result type → the lone summary badge stays a read-only label
 * (nothing to filter against), never a no-op toggle into an empty view.
 */
export const SingleResultTypeReadOnly = {
  decorators: [withMockFetch("/api/log/", MOCK_SUCCESS_LOG)],
  render: () => html`<cts-log-viewer test-id="test-ok-456"></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    await waitForLogLoad(canvasElement);
    const summary = canvasElement.querySelector("cts-log-viewer .logResultSummary");

    await step("summary has no group semantics, hint, or clear control", async () => {
      expect(summary).toBeTruthy();
      expect(summary.getAttribute("role")).toBeNull();
      expect(summary.querySelector(".logResultSummaryHint")).toBeNull();
      expect(summary.querySelector(".logFilterClear")).toBeNull();
    });

    await step("the lone SUCCESS badge is a plain label, no toggle affordance", async () => {
      const badge = summary.querySelector("cts-badge");
      expect(badge.getAttribute("label")).toBe("SUCCESS (3)");
      expect(badge.hasAttribute("clickable")).toBe(false);
      expect(badge.querySelector(".badge").getAttribute("role")).toBeNull();
      expect(badge.querySelector(".badge").getAttribute("aria-pressed")).toBeNull();
    });
  },
};

/**
 * Filter state is reactive component state, not derived from `_entries`, so
 * it survives polling. With a filter active, a poll that appends a matching
 * and a non-matching entry shows only the matching one and never resets the
 * filter — and the poll does NOT re-announce (the one-shot announcement was
 * already consumed, so the live region is empty afterwards).
 */
export const FilterSurvivesPolling = {
  parameters: pollingStoryParameters,
  decorators: [
    (storyFn) => {
      // The delta is WITHHELD until the play function arms it (after the
      // filter is active), so the "filter active BEFORE the appending poll"
      // ordering is deterministic rather than racing the 20ms poll. After
      // seeding the base set, polls return [] (no re-render) until armed,
      // then deliver the delta exactly once.
      const state = {
        seeded: false,
        deliverDelta: false,
        deltaSent: false,
        responder: function () {
          if (!this.seeded) {
            this.seeded = true;
            return new Response(JSON.stringify(MOCK_BLOCKS_FILTERABLE), {
              status: 200,
              headers: { "Content-Type": "application/json" },
            });
          }
          let delta = [];
          if (this.deliverDelta && !this.deltaSent) {
            this.deltaSent = true;
            // A late FAILURE (matches the active filter) and a late SUCCESS
            // (does not). Times are well past the base set's max so the
            // append is in-order. Date.now() is fine in browser stories.
            delta = [
              {
                _id: "flt-b-4",
                testId: "test-filter-001",
                src: "LateFailure",
                time: Date.now(),
                msg: "Late failure arrived",
                blockId: "block-b",
                result: "FAILURE",
              },
              {
                _id: "flt-b-5",
                testId: "test-filter-001",
                src: "LateSuccess",
                time: Date.now() + 1,
                msg: "Late success arrived",
                blockId: "block-b",
                result: "SUCCESS",
              },
            ];
          }
          return new Response(JSON.stringify(delta), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          });
        },
      };
      window.__ctsLogViewerFetchState = state;
      return withProgrammableFetch("/api/log/", state)(storyFn);
    },
  ],
  render: () =>
    html`<cts-log-viewer test-id="test-filter-001" ._pollIntervalMs=${20}></cts-log-viewer>`,
  async play({ canvasElement, step }) {
    const state = window.__ctsLogViewerFetchState;
    try {
      await waitForLogLoad(canvasElement);
      const viewer = canvasElement.querySelector("cts-log-viewer");

      await step("activate the FAILURE filter before any delta arrives", async () => {
        // Exactly one matching entry. (Empty polls don't re-render, so this is
        // stable.)
        await userEvent.click(summaryBadgeButton(canvasElement, "FAILURE"));
        await viewer.updateComplete;
        expect(visibleEntryIds(canvasElement)).toEqual(["flt-a-3"]);
        expect(summaryBadgeButton(canvasElement, "FAILURE").getAttribute("aria-pressed")).toBe(
          "true",
        );
      });

      await step(
        "the filter survives a poll that appends matching + non-matching entries",
        async () => {
          // Arm the delta: the next poll appends flt-b-4 (FAILURE, matches) and
          // flt-b-5 (SUCCESS, does not). The filter must survive the re-render —
          // the new failure appears, the new success stays hidden.
          state.deliverDelta = true;
          await waitFor(
            () => {
              expect(
                canvasElement.querySelector('cts-log-entry[data-entry-id="flt-b-4"]'),
              ).toBeTruthy();
            },
            { timeout: 2000 },
          );
          expect(canvasElement.querySelector('cts-log-entry[data-entry-id="flt-b-5"]')).toBeNull();
          expect(new Set(visibleEntryIds(canvasElement))).toEqual(new Set(["flt-a-3", "flt-b-4"]));

          // Filter still pressed after the poll-driven re-render.
          expect(summaryBadgeButton(canvasElement, "FAILURE").getAttribute("aria-pressed")).toBe(
            "true",
          );
        },
      );

      await step("the poll re-render did NOT re-announce", async () => {
        // The one-shot announcement was consumed on the toggle render, so the
        // live region is empty after the append (the append render reset it
        // without producing a new phrase).
        const announce = canvasElement.querySelector("cts-log-viewer .logFilterAnnounce");
        expect(announce.textContent).toBe("");
      });
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};
