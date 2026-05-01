import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import { MOCK_RUNNING_TESTS } from "@fixtures/mock-test-data.js";

import "../../../src/main/resources/static/components/cts-page-head.js";
import "../../../src/main/resources/static/components/cts-button.js";
import "../../../src/main/resources/static/components/cts-running-test-card.js";
import "../../../src/main/resources/static/components/cts-empty-state.js";

// Recreates `running-test.html` for design review without requiring a live
// backend. Mirrors the page chrome (page-head + refresh action, vertical
// list of cards inside `.oidf-running-tests-list`, page-level padding and
// max-width from running-test.html's inline <style>) so the visual diff
// matches what a real user sees.
//
// The navbar and skip-link are intentionally omitted — both depend on
// /api/currentuser and add chrome that's not under review here. To preview
// the page including the navbar, run Playwright UI mode against
// frontend/e2e/running-test.spec.js, which mocks the auth endpoint too.

export default {
  title: "Pages/RunningTest",
};

const PAGE_STYLES = html`
  <style>
    .oidf-running-tests-page {
      padding: var(--space-5) var(--space-6);
      max-width: 1320px;
      margin: 0 auto;
      font-family: var(--font-sans);
    }
    .oidf-running-tests-list {
      display: flex;
      flex-direction: column;
      gap: var(--space-4);
    }
  </style>
`;

const RUNNING_TEST = MOCK_RUNNING_TESTS[0]; // status: RUNNING
const WAITING_TEST = MOCK_RUNNING_TESTS[1]; // status: WAITING

/**
 * Default page render — a mixed list of RUNNING and WAITING tests, the
 * shape a user typically sees on this page during an interactive test
 * session. R19 ships here: the WAITING card's badge reads "Waiting for
 * user input" instead of the bare enum literal, while the RUNNING card
 * keeps the `RUNNING` literal to demonstrate the mapping is targeted.
 */
export const Default = {
  render: () => html`
    ${PAGE_STYLES}
    <main id="viewRunningTestPage" class="oidf-running-tests-page">
      <cts-page-head title="Running tests">
        <cts-button slot="actions" id="refresh" icon="arrow-reload-02" label="Refresh"></cts-button>
      </cts-page-head>

      <div id="running-tests" class="oidf-running-tests-list">
        <cts-running-test-card .test=${RUNNING_TEST}></cts-running-test-card>
        <cts-running-test-card .test=${WAITING_TEST}></cts-running-test-card>
      </div>
    </main>
  `,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    // Page header renders the descriptive title (R15 — already shipped).
    await waitFor(() => {
      expect(canvas.getByText("Running tests")).toBeInTheDocument();
    });
    expect(canvas.getByText(/Refresh/)).toBeInTheDocument();

    // Both cards rendered.
    const cards = canvasElement.querySelectorAll("cts-running-test-card");
    expect(cards.length).toBe(2);

    // R19 lives here: WAITING card surfaces the friendly label.
    const waitingCard = cards[1];
    const waitingBadge = waitingCard.querySelector("cts-badge");
    expect(waitingBadge.getAttribute("label")).toBe("Waiting for user input");
    expect(waitingBadge.getAttribute("variant")).toBe("warn");

    // RUNNING card keeps the enum literal — mapping is targeted, not blanket.
    const runningCard = cards[0];
    const runningBadge = runningCard.querySelector("cts-badge");
    expect(runningBadge.getAttribute("label")).toBe("RUNNING");
    expect(runningBadge.getAttribute("variant")).toBe("running");
  },
};

/**
 * Empty state — what the page looks like when no tests are running for
 * the signed-in user. Page chrome (header, refresh action) renders, and
 * the list area is filled with a `cts-empty-state` that explains why the
 * area is blank and offers a primary action to schedule a new test.
 *
 * The real page wires this up inside `updateRunningTable()` in
 * `running-test.html`: when `/api/runner/running` returns an empty array,
 * a `cts-empty-state` element with the same attributes is appended to
 * `#running-tests`. Mirroring the rendered shape here means the visual
 * diff in Storybook matches what the real user sees.
 */
export const Empty = {
  render: () => html`
    ${PAGE_STYLES}
    <main id="viewRunningTestPage" class="oidf-running-tests-page">
      <cts-page-head title="Running tests">
        <cts-button
          slot="actions"
          id="refresh"
          icon="arrow-reload-02"
          label="Refresh"
        ></cts-button>
      </cts-page-head>

      <div id="running-tests" class="oidf-running-tests-list">
        <cts-empty-state
          icon="play-circle"
          heading="No tests are currently running"
          body="Schedule a test to see it appear here while it runs."
          cta-label="Schedule a test"
          cta-href="schedule-test.html"
        ></cts-empty-state>
      </div>
    </main>
  `,
  async play({ canvasElement }) {
    await waitFor(() => {
      expect(within(canvasElement).getByText("Running tests")).toBeInTheDocument();
    });

    // No cards rendered.
    const cards = canvasElement.querySelectorAll("cts-running-test-card");
    expect(cards.length).toBe(0);

    // Empty state explains the blank list and offers a path forward.
    const emptyState = canvasElement.querySelector("cts-empty-state");
    expect(emptyState).toBeTruthy();
    expect(emptyState.textContent).toContain("No tests are currently running");
    expect(emptyState.textContent).toContain("Schedule a test");

    // The primary CTA renders the built-in cts-link-button to the
    // schedule-test page.
    const cta = emptyState.querySelector("cts-link-button");
    expect(cta).toBeTruthy();
    expect(cta.getAttribute("href")).toBe("schedule-test.html");
  },
};

/**
 * Admin view — every card reveals the Test Owner row. Useful for
 * verifying the meta-row layout when extra rows push the card height up
 * and the action stack reflows.
 */
export const AdminView = {
  render: () => html`
    ${PAGE_STYLES}
    <main id="viewRunningTestPage" class="oidf-running-tests-page">
      <cts-page-head title="Running tests">
        <cts-button slot="actions" id="refresh" icon="arrow-reload-02" label="Refresh"></cts-button>
      </cts-page-head>

      <div id="running-tests" class="oidf-running-tests-list">
        <cts-running-test-card .test=${RUNNING_TEST} is-admin></cts-running-test-card>
        <cts-running-test-card .test=${WAITING_TEST} is-admin></cts-running-test-card>
      </div>
    </main>
  `,
  async play({ canvasElement }) {
    await waitFor(() => {
      const ownerRows = canvasElement.querySelectorAll('[data-testid="owner-row"]');
      expect(ownerRows.length).toBe(2);
    });

    // R19 still surfaces under admin view — the badge mapping is
    // independent of the admin flag.
    const cards = canvasElement.querySelectorAll("cts-running-test-card");
    const waitingBadge = cards[1].querySelector("cts-badge");
    expect(waitingBadge.getAttribute("label")).toBe("Waiting for user input");
  },
};
