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

// U6 (MR 1998 finding C2): a description with `\n\n` paragraph breaks
// renders as multiple <p> blocks instead of one wall of prose.
const MULTI_PARAGRAPH_DESCRIPTION =
  "First paragraph explains what the test exercises.\n\nSecond paragraph explains the dependency on an external IdP.";

export const WithParagraphBreaks = {
  render: () => html`<cts-test-summary .summary=${MULTI_PARAGRAPH_DESCRIPTION}></cts-test-summary>`,
  async play({ canvasElement }) {
    const body = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="about-test-zone"] .summaryBody');
      if (!el) throw new Error("summaryBody not yet rendered");
      return el;
    });
    const paragraphs = body.querySelectorAll("p");
    expect(paragraphs.length).toBe(2);
    expect(paragraphs[0].textContent).toContain("First paragraph");
    expect(paragraphs[1].textContent).toContain("Second paragraph");
  },
};

// U6 (MR 1998 finding C2): backtick spans render as <code>. The same
// helper handles plain prose between the code spans without rewriting it.
const DESCRIPTION_WITH_INLINE_CODE =
  "The client posts to the `/token` endpoint with `grant_type=authorization_code`.";

export const WithInlineCode = {
  render: () =>
    html`<cts-test-summary .summary=${DESCRIPTION_WITH_INLINE_CODE}></cts-test-summary>`,
  async play({ canvasElement }) {
    const body = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="about-test-zone"] .summaryBody');
      if (!el) throw new Error("summaryBody not yet rendered");
      return el;
    });
    const codeNodes = body.querySelectorAll("code");
    expect(codeNodes.length).toBe(2);
    expect(codeNodes[0].textContent).toBe("/token");
    expect(codeNodes[1].textContent).toBe("grant_type=authorization_code");
    // The raw backtick character does not leak into the rendered text.
    expect(body.textContent).not.toContain("`");
  },
};

// Unbalanced backticks render literally so a typo never produces a
// dangling <code> span.
const UNBALANCED_BACKTICKS = "The token has a single backtick ` in this sentence.";

export const WithUnbalancedBackticks = {
  render: () => html`<cts-test-summary .summary=${UNBALANCED_BACKTICKS}></cts-test-summary>`,
  async play({ canvasElement }) {
    const body = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="about-test-zone"] .summaryBody');
      if (!el) throw new Error("summaryBody not yet rendered");
      return el;
    });
    expect(body.querySelectorAll("code").length).toBe(0);
    expect(body.textContent).toContain("`");
  },
};

// Plan: docs/plans/2026-05-27-001-feat-autolink-and-format-test-prose-plan.md
// (U1). Bare http/https URLs — which pepper these summaries (RFC, spec, and
// Bitbucket links) — render as new-tab anchors. snake_case identifiers in the
// same prose are left untouched.
const DESCRIPTION_WITH_BARE_URL =
  "Checks the clock skew handling as per https://openid.net/specs/fapi-2_0-security-profile.html and verifies the access_token is rejected.";

export const WithAutolinkedUrl = {
  render: () => html`<cts-test-summary .summary=${DESCRIPTION_WITH_BARE_URL}></cts-test-summary>`,
  async play({ canvasElement }) {
    const body = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="about-test-zone"] .summaryBody');
      if (!el) throw new Error("summaryBody not yet rendered");
      return el;
    });
    const link = body.querySelector("a");
    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe(
      "https://openid.net/specs/fapi-2_0-security-profile.html",
    );
    expect(link.getAttribute("target")).toBe("_blank");
    expect(link.getAttribute("rel")).toBe("noopener noreferrer");
    // snake_case prose survives — no spurious emphasis, identifier intact.
    expect(body.textContent).toContain("access_token");
    expect(body.querySelectorAll("em").length).toBe(0);
  },
};

// Plan U1: markdown is rendered via unsafeHTML, so DOMPurify must strip any
// dangerous markup that slips into a summary. Test prose is build-time authored,
// but sanitization is the contract that makes unsafeHTML safe here.
const DESCRIPTION_WITH_XSS =
  'Run the test then check the result.\n\n<img src=x onerror="globalThis.__xss=1"><script>globalThis.__xss=1</script>';

export const WithDangerousHtmlSanitized = {
  render: () => html`<cts-test-summary .summary=${DESCRIPTION_WITH_XSS}></cts-test-summary>`,
  async play({ canvasElement }) {
    const body = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="about-test-zone"] .summaryBody');
      if (!el) throw new Error("summaryBody not yet rendered");
      return el;
    });
    // The benign prose still renders.
    expect(body.textContent).toContain("Run the test then check the result.");
    // No script element survives, and the onerror handler never fired.
    expect(body.querySelector("script")).toBeNull();
    expect(body.querySelector("[onerror]")).toBeNull();
    expect(globalThis.__xss).toBeUndefined();
  },
};
