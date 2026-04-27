import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import "./cts-test-summary.js";

export default {
  title: "Components/cts-test-summary",
  component: "cts-test-summary",
};

// Mirrors the R24 split-marker contract from
// `./test-summary-split.js`: the marker `\n\n---\n\n` separates the
// description from the user instructions. Tests use the same fixtures
// the existing R24 coverage uses so the extracted component renders
// identical DOM.
const DESCRIPTION_ONLY = "This test exercises the OAuth2 token endpoint with a happy-path client.";

const INSTRUCTIONS_ONLY = `\n\n---\n\nClick the Visit URL button below to launch the test in a new tab.`;

const DESCRIPTION_AND_INSTRUCTIONS = `${DESCRIPTION_ONLY}\n\n---\n\nClick the Visit URL button below to launch the test in a new tab.`;

export const WithDescriptionOnly = {
  render: () => html`<cts-test-summary .summary=${DESCRIPTION_ONLY}></cts-test-summary>`,
  async play({ canvasElement }) {
    const aboutZone = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="about-test-zone"]');
      if (!el) throw new Error("about-test-zone not yet rendered");
      return el;
    });

    expect(within(aboutZone).getByText("About this test")).toBeInTheDocument();
    expect(within(aboutZone).getByText(DESCRIPTION_ONLY)).toBeInTheDocument();
    // Description-only renders only the about zone, not the instructions zone.
    expect(canvasElement.querySelector('[data-testid="user-instructions-zone"]')).toBeNull();
  },
};

export const WithUserInstructions = {
  render: () =>
    html`<cts-test-summary .summary=${DESCRIPTION_AND_INSTRUCTIONS}></cts-test-summary>`,
  async play({ canvasElement }) {
    const aboutZone = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="about-test-zone"]');
      if (!el) throw new Error("about-test-zone not yet rendered");
      return el;
    });
    const instructionsZone = canvasElement.querySelector('[data-testid="user-instructions-zone"]');

    expect(aboutZone).toBeTruthy();
    expect(instructionsZone).toBeTruthy();
    expect(within(aboutZone).getByText("About this test")).toBeInTheDocument();
    expect(within(instructionsZone).getByText("What you need to do")).toBeInTheDocument();
  },
};

export const InstructionsOnly = {
  // Used by the page-level B1 instance — when a WAITING test only carries
  // instructions, the splitter returns description="" + instructions=text.
  render: () => html`<cts-test-summary .summary=${INSTRUCTIONS_ONLY}></cts-test-summary>`,
  async play({ canvasElement }) {
    const instructionsZone = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="user-instructions-zone"]');
      if (!el) throw new Error("user-instructions-zone not yet rendered");
      return el;
    });

    expect(within(instructionsZone).getByText("What you need to do")).toBeInTheDocument();
    expect(canvasElement.querySelector('[data-testid="about-test-zone"]')).toBeNull();
  },
};

export const WithoutSummary = {
  render: () => html`<cts-test-summary .summary=${""}></cts-test-summary>`,
  async play({ canvasElement }) {
    // The component returns `nothing` when the summary is empty. Wait one
    // microtask so Lit's first render flushes before asserting absence.
    await Promise.resolve();
    expect(canvasElement.querySelector('[data-testid="about-test-zone"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="user-instructions-zone"]')).toBeNull();
  },
};
