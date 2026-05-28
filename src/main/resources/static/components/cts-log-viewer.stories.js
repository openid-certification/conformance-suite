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

export const NonCollapsibleBlocks = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () => html`<cts-log-viewer test-id="test-abc-123"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);
    // Each block renders as a plain <div class="logBlock"> with a
    // presentational <div class="startBlock"> header. Blocks are not
    // collapsible, so the header carries no chevron, is not focusable, and
    // there is no open/closed state.
    const blocks = canvasElement.querySelectorAll("div.logBlock");
    expect(blocks.length).toBeGreaterThan(0);

    const firstBlock = blocks[0];
    const header = firstBlock.querySelector(".startBlock");
    expect(header).toBeTruthy();
    expect(header.tagName).toBe("DIV");

    // Presentational header: no chevron icon, not in the tab order, no
    // button role. Assert the absence of the tabindex attribute (rather
    // than tabIndex === -1, which is the default for any <div> and would
    // not catch an accidental explicit tabindex="-1").
    expect(header.querySelector("cts-icon")).toBeNull();
    expect(header.hasAttribute("tabindex")).toBe(false);
    expect(header.getAttribute("role")).toBeNull();

    // Block children are always rendered and visible (no collapse). The
    // cts-log-entry host is display:contents in the wide layout (subgrid
    // relay), so it has no box — assert on the painted .logItem inside it.
    const children = firstBlock.querySelectorAll("cts-log-entry");
    expect(children.length).toBeGreaterThan(0);
    const firstItem = firstBlock.querySelector("cts-log-entry .logItem");
    expect(firstItem).toBeTruthy();
    expect(firstItem.checkVisibility()).toBe(true);
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

// --- U5: per-block status aggregation ---
// Plan: docs/plans/2026-04-26-006-feat-r27-per-block-status-aggregation-plan.md

export const BlocksWithStatus = {
  decorators: [withMockFetch("/api/log/", MOCK_BLOCKS_WITH_STATUS)],
  render: () => html`<cts-log-viewer test-id="test-blocks-001"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);

    // Three .logBlock divs rendered, each with a .startBlock header.
    const blocks = canvasElement.querySelectorAll(".logBlock");
    expect(blocks.length).toBe(3);

    const blockA = canvasElement.querySelector('[data-block-id="block-a"]');
    const blockB = canvasElement.querySelector('[data-block-id="block-b"]');
    const blockC = canvasElement.querySelector('[data-block-id="block-c"]');
    expect(blockA).toBeTruthy();
    expect(blockB).toBeTruthy();
    expect(blockC).toBeTruthy();

    // Block A: 2 successes — single ✓2 chip.
    const aBadges = blockA.querySelectorAll(".startBlockCounts cts-badge");
    expect(aBadges.length).toBe(1);
    expect(aBadges[0].getAttribute("variant")).toBe("pass");
    expect(aBadges[0].getAttribute("label")).toBe("✓2");

    // Block B: 1 success + 1 failure — ✓1 then ✗1, in spec order.
    const bBadges = blockB.querySelectorAll(".startBlockCounts cts-badge");
    expect(bBadges.length).toBe(2);
    expect(bBadges[0].getAttribute("variant")).toBe("pass");
    expect(bBadges[0].getAttribute("label")).toBe("✓1");
    expect(bBadges[1].getAttribute("variant")).toBe("fail");
    expect(bBadges[1].getAttribute("label")).toBe("✗1");

    // Block C: 1 warning + 1 info — INFO is excluded by design, so the
    // cluster shows only ⚠1.
    const cBadges = blockC.querySelectorAll(".startBlockCounts cts-badge");
    expect(cBadges.length).toBe(1);
    expect(cBadges[0].getAttribute("variant")).toBe("warn");
    expect(cBadges[0].getAttribute("label")).toBe("⚠1");
  },
};

export const EmptyBlock = {
  decorators: [withMockFetch("/api/log/", MOCK_EMPTY_BLOCK)],
  render: () => html`<cts-log-viewer test-id="test-empty-block-001"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);

    const block = canvasElement.querySelector('[data-block-id="block-empty"]');
    expect(block).toBeTruthy();

    // No children → no badges in the cluster (graceful empty state).
    const badges = block.querySelectorAll(".startBlockCounts cts-badge");
    expect(badges.length).toBe(0);

    // Header still renders the text from msg.
    const header = block.querySelector(".startBlock");
    expect(header.textContent).toContain("Awaiting checks");
  },
};

export const BlockCountsUpdateOnPolling = {
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
  async play({ canvasElement }) {
    try {
      // First wait: badges land on ✓2 (single chip).
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

      // After the second poll, the cluster transitions to ✓3 ✗1.
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
  async play({ canvasElement }) {
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
        scrolled.add(/** @type {HTMLElement} */ (this));
        // Don't actually scroll — the canvas iframe's geometry trips up
        // other tests in the same suite if we yield real scroll motion.
      }
    );

    try {
      await waitForLogLoad(canvasElement);

      // Wait for the deferred (microtask + updateComplete) hash scroll.
      await waitFor(() => {
        const target = /** @type {HTMLElement | null} */ (canvasElement.querySelector("#LOG-0003"));
        expect(target).toBeTruthy();
        if (target) expect(scrolled.has(target)).toBe(true);
      });

      // The host carries id=LOG-0003 (mirrored from the referenceId prop).
      const target = canvasElement.querySelector("#LOG-0003");
      if (!target) throw new Error("anchor target not present");
      expect(target.tagName).toBe("CTS-LOG-ENTRY");
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
        scrolled.add(/** @type {HTMLElement} */ (this));
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
  async play({ canvasElement }) {
    const previousHash = window.location.hash;

    /** @type {Set<HTMLElement>} */
    const scrolled = new Set();
    const realScrollIntoView = Element.prototype.scrollIntoView;
    Element.prototype.scrollIntoView = /** @type {Element["scrollIntoView"]} */ (
      function () {
        scrolled.add(/** @type {HTMLElement} */ (this));
      }
    );

    try {
      await waitForLogLoad(canvasElement);
      // Page is loaded with no hash; nothing scrolled yet.
      expect(scrolled.size).toBe(0);

      // Simulate a timestamp deep-link click by navigating the fragment.
      window.location.hash = "#LOG-0006";

      await waitFor(() => {
        const target = /** @type {HTMLElement | null} */ (canvasElement.querySelector("#LOG-0006"));
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
        scrolled.add(/** @type {HTMLElement} */ (this));
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
        scrolled.add(/** @type {HTMLElement} */ (this));
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
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);

    const left = (el) => Math.round(el.getBoundingClientRect().left);

    // The leading non-block entry is a direct child of .logEntries.
    const topItem = /** @type {HTMLElement} */ (
      canvasElement.querySelector(".logEntries > cts-log-entry > .logItem")
    );
    expect(topItem).toBeTruthy();

    // Block rows live one level deeper, inside the .logBlock div.
    const block = /** @type {HTMLElement} */ (canvasElement.querySelector(".logBlock"));
    expect(block).toBeTruthy();
    const blockItems = [...block.querySelectorAll(":scope > cts-log-entry > .logItem")];
    expect(blockItems.length).toBe(3);

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

    // Block rows are always rendered and visible — there is no collapse.
    for (const item of blockItems) {
      expect(item.checkVisibility()).toBe(true);
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
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);
    const viewer = canvasElement.querySelector("cts-log-viewer");

    // Unfiltered baseline: both blocks, all six leaf entries, four toggle
    // badges in a labelled group.
    expect(canvasElement.querySelectorAll("cts-log-viewer .logBlock").length).toBe(2);
    expect(visibleEntryIds(canvasElement).length).toBe(6);
    const group = canvasElement.querySelector("cts-log-viewer .logResultSummary");
    expect(group.getAttribute("role")).toBe("group");
    expect(group.getAttribute("aria-label")).toBe("Filter log entries by result");
    for (const r of ["SUCCESS", "FAILURE", "REVIEW", "WARNING"]) {
      expect(summaryBadgeButton(canvasElement, r).getAttribute("aria-pressed")).toBe("false");
    }
    // Not filtering yet → host has no is-filtering marker.
    expect(viewer.classList.contains("is-filtering")).toBe(false);

    // Toggle FAILURE: only the lone failure entry survives; Block B (no
    // failure) is elided entirely; the badge reads pressed; the host flips
    // into the filtering state (which mutes block-header counts via CSS).
    await userEvent.click(summaryBadgeButton(canvasElement, "FAILURE"));
    await viewer.updateComplete;
    expect(summaryBadgeButton(canvasElement, "FAILURE").getAttribute("aria-pressed")).toBe("true");
    expect(visibleEntryIds(canvasElement)).toEqual(["flt-a-3"]);
    expect(canvasElement.querySelectorAll("cts-log-viewer .logBlock").length).toBe(1);
    expect(viewer.classList.contains("is-filtering")).toBe(true);

    // Count badges always show the TRUE total, never the filtered subset.
    expect(summaryBadge(canvasElement, "SUCCESS").getAttribute("label")).toBe("SUCCESS (3)");
    expect(summaryBadge(canvasElement, "FAILURE").getAttribute("label")).toBe("FAILURE (1)");

    // The live region announced the user action (description only — no
    // entry count, so polling can't perturb it).
    const announce = canvasElement.querySelector("cts-log-viewer .logFilterAnnounce");
    expect(announce.textContent).toBe("Filtering by FAILURE");

    // Add REVIEW → union: the failure (Block A) and the review (Block B),
    // one surviving child per block. Both badges pressed.
    await userEvent.click(summaryBadgeButton(canvasElement, "REVIEW"));
    await viewer.updateComplete;
    expect(summaryBadgeButton(canvasElement, "REVIEW").getAttribute("aria-pressed")).toBe("true");
    expect(new Set(visibleEntryIds(canvasElement))).toEqual(new Set(["flt-a-3", "flt-b-2"]));
    expect(canvasElement.querySelectorAll("cts-log-viewer .logBlock").length).toBe(2);
    expect(announce.textContent).toBe("Filtering by FAILURE, REVIEW");

    // Clear restores the full stream and drops the filtering state.
    const clearBtn = canvasElement.querySelector("cts-log-viewer .logFilterClear");
    expect(clearBtn).toBeTruthy();
    await userEvent.click(clearBtn);
    await viewer.updateComplete;
    expect(visibleEntryIds(canvasElement).length).toBe(6);
    expect(canvasElement.querySelectorAll("cts-log-viewer .logBlock").length).toBe(2);
    expect(viewer.classList.contains("is-filtering")).toBe(false);
    expect(summaryBadgeButton(canvasElement, "FAILURE").getAttribute("aria-pressed")).toBe("false");
    expect(announce.textContent).toBe("Filters cleared");
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
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);
    const viewer = /** @type {any} */ (canvasElement.querySelector("cts-log-viewer"));

    // No SKIPPED entries exist in the fixture, so this filter matches nothing.
    viewer._activeFilters = new Set(["SKIPPED"]);
    await viewer.updateComplete;

    const empty = canvasElement.querySelector("cts-log-viewer .logFilterEmpty");
    expect(empty).toBeTruthy();
    expect(empty?.textContent).toContain("No entries match the active filters");
    // No entry rows and no blocks render in the empty state.
    expect(canvasElement.querySelectorAll("cts-log-viewer cts-log-entry").length).toBe(0);
    expect(canvasElement.querySelectorAll("cts-log-viewer .logBlock").length).toBe(0);

    // The inline Clear affordance restores the full stream.
    const inlineClear = empty?.querySelector(".logFilterClear");
    expect(inlineClear).toBeTruthy();
    await userEvent.click(/** @type {Element} */ (inlineClear));
    await viewer.updateComplete;
    expect(canvasElement.querySelectorAll("cts-log-viewer cts-log-entry").length).toBe(6);
    expect(canvasElement.querySelector("cts-log-viewer .logFilterEmpty")).toBeNull();
  },
};

/**
 * Single result type → the lone summary badge stays a read-only label
 * (nothing to filter against), never a no-op toggle into an empty view.
 */
export const SingleResultTypeReadOnly = {
  decorators: [withMockFetch("/api/log/", MOCK_SUCCESS_LOG)],
  render: () => html`<cts-log-viewer test-id="test-ok-456"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);
    const summary = canvasElement.querySelector("cts-log-viewer .logResultSummary");
    expect(summary).toBeTruthy();
    // No group semantics, no discoverability hint, no clear control.
    expect(summary.getAttribute("role")).toBeNull();
    expect(summary.querySelector(".logResultSummaryHint")).toBeNull();
    expect(summary.querySelector(".logFilterClear")).toBeNull();

    // The lone SUCCESS badge is a plain label: no toggle affordance.
    const badge = summary.querySelector("cts-badge");
    expect(badge.getAttribute("label")).toBe("SUCCESS (3)");
    expect(badge.hasAttribute("clickable")).toBe(false);
    expect(badge.querySelector(".badge").getAttribute("role")).toBeNull();
    expect(badge.querySelector(".badge").getAttribute("aria-pressed")).toBeNull();
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
  async play({ canvasElement }) {
    const state = window.__ctsLogViewerFetchState;
    try {
      await waitForLogLoad(canvasElement);
      const viewer = canvasElement.querySelector("cts-log-viewer");

      // Activate the FAILURE filter BEFORE any delta arrives → exactly one
      // matching entry. (Empty polls don't re-render, so this is stable.)
      await userEvent.click(summaryBadgeButton(canvasElement, "FAILURE"));
      await viewer.updateComplete;
      expect(visibleEntryIds(canvasElement)).toEqual(["flt-a-3"]);
      expect(summaryBadgeButton(canvasElement, "FAILURE").getAttribute("aria-pressed")).toBe(
        "true",
      );

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

      // The poll re-render did NOT re-announce: the one-shot announcement was
      // consumed on the toggle render, so the live region is empty after the
      // append (the append render reset it without producing a new phrase).
      const announce = canvasElement.querySelector("cts-log-viewer .logFilterAnnounce");
      expect(announce.textContent).toBe("");
    } finally {
      const patched = /** @type {typeof fetch & { __realFetch?: typeof fetch }} */ (window.fetch);
      if (patched.__realFetch) window.fetch = patched.__realFetch;
      delete window.__ctsLogViewerFetchState;
    }
  },
};
