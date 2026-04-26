import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { withMockFetch, withProgrammableFetch } from "@fixtures/helpers.js";
import { MOCK_LOG_ENTRIES, MOCK_EMPTY_LOG, MOCK_SUCCESS_LOG } from "@fixtures/mock-log-entries.js";
import { MOCK_TEST_STATUS } from "@fixtures/mock-test-data.js";
import "./cts-log-viewer.js";

export default {
  title: "Components/cts-log-viewer",
  component: "cts-log-viewer",
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
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);
    const badges = canvasElement.querySelectorAll("cts-badge");
    expect(badges.length).toBeGreaterThan(0);
    const entries = canvasElement.querySelectorAll(".logItem");
    expect(entries.length).toBeGreaterThan(0);
    const canvas = within(canvasElement);
    expect(canvas.getByText(/ID token signature validation failed/)).toBeTruthy();
  },
};

export const CollapsibleBlocks = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () => html`<cts-log-viewer test-id="test-abc-123"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);
    const blockStarts = canvasElement.querySelectorAll(".startBlock");
    expect(blockStarts.length).toBeGreaterThan(0);
    const firstBlock = blockStarts[0];
    await userEvent.click(firstBlock);
    await waitFor(() => {
      const chevron = firstBlock.querySelector("cts-icon");
      expect(chevron.getAttribute("name")).toBe("chevron-right");
    });
    await userEvent.click(firstBlock);
    await waitFor(() => {
      const chevron = firstBlock.querySelector("cts-icon");
      expect(chevron.getAttribute("name")).toBe("chevron-down");
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
    const badges = canvasElement.querySelectorAll("cts-badge");
    for (const badge of badges) {
      if (badge.getAttribute("variant")) {
        expect(badge.getAttribute("variant")).toBe("pass");
      }
    }
  },
};

// Retry banner tests use `_pollIntervalMs` (internal test hook) bound via Lit
// `.prop` syntax so the fast interval is set before connectedCallback fires.
// The programmable-fetch state is mutable so the play function can flip
// responses mid-test to exercise recovery and lifecycle.

export const PersistentFailureBanner = {
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
  async play({ canvasElement }) {
    try {
      await waitFor(
        () => {
          const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
          expect(banner).toBeTruthy();
          expect(banner.textContent).toContain("Log connection lost");
        },
        { timeout: 3000 },
      );
      // Banner is a polite aria-live region (screen readers announce softly).
      const banner = canvasElement.querySelector('[data-testid="log-viewer-error"]');
      expect(banner.getAttribute("aria-live")).toBe("polite");
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

export const RecoveryClearsBanner = {
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
  async play({ canvasElement }) {
    const state = window.__ctsLogViewerFetchState;
    try {
      // Wait for banner to appear after three 500s.
      await waitFor(
        () => {
          expect(canvasElement.querySelector('[data-testid="log-viewer-error"]')).toBeTruthy();
        },
        { timeout: 3000 },
      );
      // Flip the responder to success; next poll should clear the banner.
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
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};

// --- U1: log-detail-v2 page integration ---
// Plan: docs/plans/2026-04-26-002-refactor-log-detail-page-to-lit-triad-plan.md
// MountedFromExistingPage simulates how log-detail-v2.js mounts the viewer:
// the bootstrap pre-fetches /api/info and passes the result via the new
// `testInfo` reactive property, then the viewer's first /api/log poll fires
// the cts-first-fetch-resolved event so the page can defer hash-anchor
// scrolling until rows are in the DOM.

export const MountedFromExistingPage = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () =>
    html`<cts-log-viewer test-id="test-inst-001" .testInfo=${MOCK_TEST_STATUS}></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);

    // testInfo flows through unchanged — the viewer doesn't render it
    // (the header owns metadata rendering) but consumers may read it.
    const viewer = canvasElement.querySelector("cts-log-viewer");
    expect(viewer.testInfo).toEqual(MOCK_TEST_STATUS);

    // Entries rendered as usual.
    const entries = canvasElement.querySelectorAll(".logItem");
    expect(entries.length).toBeGreaterThan(0);
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
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);

    const entries = canvasElement.querySelectorAll(".logItem");
    expect(entries.length).toBeGreaterThan(0);

    // Every entry's container query is satisfied (host width < 640px), so
    // each row should be on the small grid (1fr auto) — not the wide
    // 5-track layout. Sample the first one.
    const firstItem = entries[0];
    const style = getComputedStyle(firstItem);
    const tracks = style.gridTemplateColumns.split(/\s+/).filter(Boolean);
    expect(tracks.length).toBe(2);

    // No horizontal overflow on the entries stream — the regression this
    // story is meant to catch.
    const stream = canvasElement.querySelector(".logEntries") ?? entries[0].parentElement;
    if (stream) {
      expect(stream.scrollWidth).toBeLessThanOrEqual(stream.clientWidth);
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
      const countAtDisconnect = state.callCount;
      el.remove();
      // Wait long enough for several more polls to have been scheduled.
      await new Promise((r) => setTimeout(r, 200));
      // After remove(), isConnected is false so finally skips setTimeout.
      // At most one in-flight poll may finish after remove(); we allow +1.
      expect(state.callCount).toBeLessThanOrEqual(countAtDisconnect + 1);
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};
