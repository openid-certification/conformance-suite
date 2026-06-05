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
const readToken = (name) =>
  getComputedStyle(document.documentElement).getPropertyValue(name).trim();

/**
 * Overline-styled section wrapper shared by the catalog stories.
 *
 * @param {string} title
 * @param {unknown} body
 */
const section = (title, body) => html`
  <section data-token-section="${title}" style="margin: 0 0 var(--space-8) 0;">
    <h3 class="t-overline" style="margin: 0 0 var(--space-3) 0;">${title}</h3>
    ${body}
  </section>
`;

/**
 * Responsive catalog grid shared by the swatch/ramp sections.
 *
 * @param {unknown} cells
 * @param {string} [minWidth]
 */
const grid = (cells, minWidth = "160px") => html`
  <div
    style="display: grid; grid-template-columns: repeat(auto-fill, minmax(${minWidth}, 1fr)); gap: var(--space-3);"
  >
    ${cells}
  </div>
`;

/**
 * Color swatch cell. Every swatch carries a uniform 1px inset hairline ring
 * (an inset box-shadow, not a border, so box dimensions stay identical) so
 * white and near-white tokens (--ink-0, --bg, --bg-elev, --sand-50, ...)
 * stay visible against the white canvas. The caption shows the live value;
 * color-mix()-based tokens show their declared expression, which is
 * informative rather than a bug.
 *
 * @param {string} name
 * @param {string} [note]
 */
const swatch = (name, note) => html`
  <figure data-token="${name}" style="margin: 0;">
    <div
      data-swatch-fill
      style="height: var(--space-10); border-radius: var(--radius-2); background: var(${name}); box-shadow: inset 0 0 0 1px var(--ink-200);"
    ></div>
    <figcaption class="t-mono-sm" style="padding-top: var(--space-1); word-break: break-all;">
      ${name}
      <span class="t-meta" style="display: block;">${note ?? readToken(name)}</span>
    </figcaption>
  </figure>
`;

// ---- Curated color rosters ---------------------------------------------
//
// Names only — exact rosters, not loose sweeps, so the completeness gate in
// the Overview play balances against the sheet on first run.

const BRAND = [
  "--oidf-ink",
  "--oidf-fog",
  "--oidf-sand",
  "--oidf-orange",
  "--oidf-rust",
  "--oidf-orange-pure",
];

const INK_RAMP = [
  "--ink-0",
  "--ink-50",
  "--ink-100",
  "--ink-200",
  "--ink-300",
  "--ink-400",
  "--ink-500",
  "--ink-600",
  "--ink-700",
  "--ink-800",
  "--ink-900",
];

const SAND_RAMP = [
  "--sand-50",
  "--sand-100",
  "--sand-200",
  "--sand-300",
  "--sand-400",
  "--sand-500",
];

const ORANGE_RAMP = [
  "--orange-50",
  "--orange-100",
  "--orange-200",
  "--orange-300",
  "--orange-400",
  "--orange-500",
  "--orange-600",
  "--orange-700",
];

const RUST_RAMP = [
  "--rust-50",
  "--rust-100",
  "--rust-200",
  "--rust-300",
  "--rust-400",
  "--rust-500",
];

const SEMANTIC_BG = ["--bg", "--bg-muted", "--bg-sunken", "--bg-elev", "--bg-ink", "--bg-sand"];

const SEMANTIC_FG = [
  "--fg",
  "--fg-muted",
  "--fg-soft",
  "--fg-faint",
  "--fg-on-ink",
  "--fg-on-orange",
  "--fg-link",
];

const SEMANTIC_BORDER = ["--border", "--border-strong", "--border-ink", "--divider"];

const SEMANTIC_MISC = ["--link-decoration-color"];

const STATUS = [
  "--status-pass",
  "--status-pass-bg",
  "--status-pass-border",
  "--status-fail",
  "--status-fail-bg",
  "--status-fail-border",
  "--status-warning",
  "--status-warning-bg",
  "--status-warning-border",
  "--status-running",
  "--status-running-bg",
  "--status-running-border",
  "--status-skipped",
  "--status-skipped-bg",
  "--status-skipped-border",
  "--status-info",
  "--status-info-bg",
  "--status-info-border",
];

const BADGE_RINGS = ["--badge-ring", "--badge-ring-clickable", "--badge-ring-pressed"];

/** @type {[string, string[]][]} */
const COLOR_GROUPS = [
  ["Brand palette", BRAND],
  ["Ink ramp", INK_RAMP],
  ["Sand ramp", SAND_RAMP],
  ["Orange ramp", ORANGE_RAMP],
  ["Rust ramp", RUST_RAMP],
  ["Surfaces", SEMANTIC_BG],
  ["Foreground", SEMANTIC_FG],
  ["Borders & dividers", SEMANTIC_BORDER],
  ["Link decoration", SEMANTIC_MISC],
  ["Status palette", STATUS],
  ["Badge affordance rings", BADGE_RINGS],
];

// ---- Stories ---------------------------------------------------------------

export const Overview = {
  render: () => html`
    <div
      style="max-width: var(--maxw-narrow); display: grid; gap: var(--space-4); padding: var(--space-6);"
    >
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
        <code>--oidf-*</code> brand anchors) feed
        <strong>semantic aliases</strong> (<code>--bg</code>, <code>--fg</code>,
        <code>--border</code>, <code>--status-*</code>) via <code>var()</code>. Components should
        consume the semantic tier; the primitives exist so the semantic layer has a stable palette
        to point at.
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

export const Colors = {
  render: () => html`
    <div style="padding: var(--space-6);">
      <h2 style="margin-bottom: var(--space-2);">Color</h2>
      <p class="t-meta" style="margin-bottom: var(--space-6); max-width: var(--maxw-narrow);">
        Primitive ramps feed the semantic aliases below them; components consume the semantic tier.
        <code>--oidf-orange-pure</code> is reserved for the logo and Certified mark only. The badge
        affordance rings and the link decoration are <code>color-mix()</code> overlays — their
        captions show the declared expression, and the swatch shows the composite over white.
      </p>
      ${COLOR_GROUPS.map(([title, names]) =>
        section(title, grid(names.map((name) => swatch(name)))),
      )}
    </div>
  `,

  async play({ canvasElement, step }) {
    await step("renders one swatch per curated token in every group", async () => {
      for (const [title, names] of COLOR_GROUPS) {
        const cells = canvasElement.querySelectorAll(
          `[data-token-section="${title}"] figure[data-token]`,
        );
        expect(cells.length, `swatch count for ${title}`).toBe(names.length);
      }
    });

    await step("--oidf-orange paints the brand orange", async () => {
      const fill = canvasElement.querySelector('[data-token="--oidf-orange"] [data-swatch-fill]');
      expect(getComputedStyle(/** @type {Element} */ (fill)).backgroundColor).toBe(
        "rgb(235, 139, 53)",
      );
    });

    await step("--status-fail aliases --rust-400 (semantic tier proves the aliasing)", async () => {
      const fail = canvasElement.querySelector('[data-token="--status-fail"] [data-swatch-fill]');
      const rust = canvasElement.querySelector('[data-token="--rust-400"] [data-swatch-fill]');
      const failColor = getComputedStyle(/** @type {Element} */ (fail)).backgroundColor;
      const rustColor = getComputedStyle(/** @type {Element} */ (rust)).backgroundColor;
      expect(failColor).toBe(rustColor);
      // Guard against vacuous equality (both transparent/empty).
      expect(failColor).toBe("rgb(164, 54, 4)");
    });
  },
};
