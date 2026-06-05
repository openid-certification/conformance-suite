import { html } from "lit";
import { expect, waitFor, within } from "storybook/test";

export default {
  title: "Tokens",
};

// ---- Shared helpers ------------------------------------------------------
//
// The token sheet (css/oidf-tokens.css) is injected into every story canvas
// via .storybook/preview-head.html, so token *values* are always read live —
// the only curated data in this file is token *names* (grouping and ordering
// are editorial decisions a stylesheet cannot express). A renamed or
// re-vendored value re-renders every swatch and caption automatically.

/**
 * Read a custom property's computed value off the canvas root.
 *
 * @param {string} name
 */
const readToken = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

// ---- Stories ---------------------------------------------------------------

export const Overview = {
  render: () => html`
    <div style="max-width: var(--maxw-narrow); display: grid; gap: var(--space-4); padding: var(--space-6);">
      <h2>Design Tokens</h2>
      <p class="t-body">
        The foundations of the conformance-suite UI live in a single stylesheet,
        <code>css/oidf-tokens.css</code>, vendored verbatim from the OIDF certification design
        archive (pinned 2026-04-25). It is <strong>re-vendored, never edited in place</strong> —
        when upstream changes, the whole sheet is replaced and the deliberate deviations listed in
        its header comment are reviewed.
      </p>
      <p class="t-body">
        The sheet is two-tier: <strong>primitive ramps</strong> (<code>--ink-*</code>,
        <code>--sand-*</code>, <code>--orange-*</code>, <code>--rust-*</code> and the
        <code>--oidf-*</code> brand anchors) feed <strong>semantic aliases</strong>
        (<code>--bg</code>, <code>--fg</code>, <code>--border</code>, <code>--status-*</code>)
        via <code>var()</code>. Components should consume the semantic tier; the primitives exist
        so the semantic layer has a stable palette to point at.
      </p>
      <p class="t-body">
        This sheet is loaded into every story canvas, so the catalog stories in this section render
        from the live custom-property values — the stories curate token <em>names</em> only and can
        never disagree with the sheet about a <em>value</em>.
      </p>
    </div>
  `,

  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Design Tokens")).toBeInTheDocument();
    });
    // Smoke check: the token sheet is actually loaded in this canvas. Assert
    // the exact value (not merely truthy) so a missing sheet fails loudly.
    expect(readToken("--space-4")).toBe("16px");
  },
};
