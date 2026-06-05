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

// ---- Curated typography rosters ------------------------------------------

const FONT_FAMILIES = ["--font-sans", "--font-display", "--font-mono"];

const FONT_SIZES = [
  "--fs-11",
  "--fs-12",
  "--fs-13",
  "--fs-14",
  "--fs-15",
  "--fs-16",
  "--fs-18",
  "--fs-20",
  "--fs-24",
  "--fs-28",
  "--fs-32",
  "--fs-40",
  "--fs-56",
  "--fs-72",
];

const LINE_HEIGHTS = ["--lh-tight", "--lh-snug", "--lh-base", "--lh-loose"];

const FONT_WEIGHTS = ["--fw-regular", "--fw-medium", "--fw-bold"];

/**
 * The .t-* utility classes are the type scale the app actually consumes —
 * each composes family + weight + size + line-height. The `composes` strings
 * are curated token names (editorial metadata), never values.
 *
 * @type {{ cls: string, composes: string, note?: string }[]}
 */
const TYPE_SPECIMENS = [
  { cls: "t-display", composes: "--font-display · --fw-bold · --fs-56 · --lh-tight" },
  { cls: "t-title", composes: "--font-display · --fw-bold · --fs-32 · --lh-tight" },
  { cls: "t-h1", composes: "--font-display · --fw-bold · --fs-28 · --lh-tight" },
  { cls: "t-h2", composes: "--font-sans · --fw-bold · --fs-20 · --lh-snug" },
  { cls: "t-h3", composes: "--font-sans · --fw-bold · --fs-16 · --lh-snug" },
  { cls: "t-body", composes: "--font-sans · --fw-regular · --fs-14 · --lh-base" },
  { cls: "t-body-lg", composes: "--font-sans · --fw-regular · --fs-16 · --lh-base" },
  {
    cls: "t-meta",
    composes: "--font-sans · --fw-regular · --fs-13 · --lh-snug",
    note: "color baked in: --fg-soft",
  },
  {
    cls: "t-overline",
    composes: "--font-sans · --fw-bold · --fs-12 · --lh-snug · uppercase",
    note: "color baked in: --fg-soft",
  },
  { cls: "t-mono", composes: "--font-mono · --fs-13 · --lh-snug" },
  { cls: "t-mono-sm", composes: "--font-mono · --fs-12 · --lh-snug" },
];

// ---- Curated spacing & layout rosters --------------------------------------

// 4px base; the scale deliberately skips 7/9/11/13/14/15/17/18/19.
const SPACING = [
  "--space-0",
  "--space-1",
  "--space-2",
  "--space-3",
  "--space-4",
  "--space-5",
  "--space-6",
  "--space-8",
  "--space-10",
  "--space-12",
  "--space-16",
  "--space-20",
];

const CONTROL_HEIGHT = "--control-height";

const LAYOUT_WIDTHS = ["--maxw-narrow", "--maxw-page", "--maxw-wide"];

// ---- Curated radii / elevation / motion rosters -----------------------------

const RADII = [
  "--radius-0",
  "--radius-1",
  "--radius-2",
  "--radius-3",
  "--radius-4",
  "--radius-pill",
];

const SHADOWS = ["--shadow-1", "--shadow-2", "--shadow-3", "--shadow-inset"];

// A complete box-shadow value, not a color.
const FOCUS_RING = "--focus-ring";

const MOTION = ["--ease-standard", "--ease-emphasized", "--dur-1", "--dur-2", "--dur-3"];

// Published at runtime by <cts-log-detail-header>'s sticky status bar;
// defaults to 0px on pages that do not mount the component.
const STATUS_BAR_HEIGHT = "--status-bar-height";

// ---- Completeness roster ----------------------------------------------------
//
// The union of every curated group above. The Overview play set-equals this
// against the :root custom properties actually declared in oidf-tokens.css,
// so an undocumented (or stale) token fails the suite by name — the same
// philosophy as lint:icons.

const ALL_DOCUMENTED_PROPS = [
  ...BRAND,
  ...INK_RAMP,
  ...SAND_RAMP,
  ...ORANGE_RAMP,
  ...RUST_RAMP,
  ...SEMANTIC_BG,
  ...SEMANTIC_FG,
  ...SEMANTIC_BORDER,
  ...SEMANTIC_MISC,
  ...STATUS,
  ...BADGE_RINGS,
  ...FONT_FAMILIES,
  ...FONT_SIZES,
  ...LINE_HEIGHTS,
  ...FONT_WEIGHTS,
  ...SPACING,
  CONTROL_HEIGHT,
  ...LAYOUT_WIDTHS,
  ...RADII,
  ...SHADOWS,
  FOCUS_RING,
  ...MOTION,
  STATUS_BAR_HEIGHT,
];

const DOCUMENTED_T_CLASSES = TYPE_SPECIMENS.map((s) => `.${s.cls}`);

/** Locate the token sheet's CSSStyleSheet object in the canvas document. */
const findTokenSheet = () =>
  Array.from(document.styleSheets).find((s) => (s.href || "").includes("/css/oidf-tokens.css"));

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

  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("Design Tokens")).toBeInTheDocument();
    });
    // Smoke check: the token sheet is actually loaded in this canvas. Assert
    // the exact value (not merely truthy) so a missing sheet fails loudly.
    expect(readToken("--space-4")).toBe("16px");

    const sheet = findTokenSheet();
    expect(sheet).toBeTruthy();
    const rules = Array.from(/** @type {CSSStyleSheet} */ (sheet).cssRules);

    await step("every :root custom property is documented, and nothing stale", async () => {
      const rootRule = /** @type {CSSStyleRule | undefined} */ (
        rules.find((r) => /** @type {CSSStyleRule} */ (r).selectorText === ":root")
      );
      expect(rootRule).toBeTruthy();
      const declared = Array.from(/** @type {CSSStyleRule} */ (rootRule).style).filter((p) =>
        p.startsWith("--"),
      );
      const documented = new Set(ALL_DOCUMENTED_PROPS);
      const declaredSet = new Set(declared);
      const undocumented = declared.filter((n) => !documented.has(n));
      const stale = ALL_DOCUMENTED_PROPS.filter((n) => !declaredSet.has(n));
      expect(undocumented, `tokens missing from this catalog: ${undocumented.join(", ")}`).toEqual(
        [],
      );
      expect(stale, `catalog names no longer in the sheet: ${stale.join(", ")}`).toEqual([]);
    });

    await step("every .t-* utility class has a specimen, and nothing stale", async () => {
      const declaredTClasses = rules
        .map((r) => /** @type {CSSStyleRule} */ (r).selectorText)
        .filter((s) => s && s.startsWith(".t-"));
      const documented = new Set(DOCUMENTED_T_CLASSES);
      const declaredSet = new Set(declaredTClasses);
      const undocumented = declaredTClasses.filter((s) => !documented.has(s));
      const stale = DOCUMENTED_T_CLASSES.filter((s) => !declaredSet.has(s));
      expect(
        undocumented,
        `type utilities missing from this catalog: ${undocumented.join(", ")}`,
      ).toEqual([]);
      expect(stale, `catalog type utilities no longer in the sheet: ${stale.join(", ")}`).toEqual(
        [],
      );
    });
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

export const Typography = {
  render: () => html`
    <div style="padding: var(--space-6);">
      <h2 style="margin-bottom: var(--space-2);">Typography</h2>
      <p class="t-meta" style="margin-bottom: var(--space-6); max-width: var(--maxw-narrow);">
        The <code>.t-*</code> utility classes are the type scale the app consumes — each composes
        family, weight, size, and line-height. The raw ramps below them exist for the rare case a
        composition does not fit. <code>.t-meta</code> and <code>.t-overline</code> bake their own
        foreground color (<code>--fg-soft</code>) into the class, so they are not surface-neutral.
      </p>

      ${section(
        "Type styles",
        html`
          <div style="display: grid; gap: var(--space-4);">
            ${TYPE_SPECIMENS.map(
              ({ cls, composes, note }) => html`
                <div data-type-specimen="${cls}">
                  <div class="${cls}">The quick brown fox jumps over the lazy dog</div>
                  <div
                    class="t-mono-sm"
                    style="color: var(--fg-soft); padding-top: var(--space-1);"
                  >
                    .${cls} — ${composes}${note ? html` · ${note}` : ""}
                  </div>
                </div>
              `,
            )}
            <div data-type-specimen="tabular-nums">
              <div class="t-mono tabular-nums">1111.11<br />9009.09</div>
              <div class="t-mono-sm" style="color: var(--fg-soft); padding-top: var(--space-1);">
                .tabular-nums — digit columns stay aligned as widths change row-to-row
              </div>
            </div>
          </div>
        `,
      )}
      ${section(
        "Font sizes",
        html`
          <div style="display: grid; gap: var(--space-2);">
            ${FONT_SIZES.map(
              (name) => html`
                <div
                  data-token="${name}"
                  style="display: flex; align-items: baseline; gap: var(--space-3);"
                >
                  <code class="t-mono-sm" style="min-width: 8ch;">${name}</code>
                  <span class="t-meta" style="min-width: 5ch;">${readToken(name)}</span>
                  <span style="font-size: var(${name}); line-height: var(--lh-tight);">Aa</span>
                </div>
              `,
            )}
          </div>
        `,
      )}
      ${section(
        "Weights",
        html`
          <div style="display: grid; gap: var(--space-2);">
            ${FONT_WEIGHTS.map(
              (name) => html`
                <div
                  data-token="${name}"
                  style="display: flex; align-items: baseline; gap: var(--space-3);"
                >
                  <code class="t-mono-sm" style="min-width: 12ch;">${name}</code>
                  <span class="t-meta" style="min-width: 4ch;">${readToken(name)}</span>
                  <span style="font-weight: var(${name}); font-size: var(--fs-18);">
                    Interactive UI sits at medium, headings at bold
                  </span>
                </div>
              `,
            )}
          </div>
        `,
      )}
      ${section(
        "Line heights",
        html`
          <div
            style="display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: var(--space-4);"
          >
            ${LINE_HEIGHTS.map(
              (name) => html`
                <div data-token="${name}">
                  <code class="t-mono-sm">${name}</code>
                  <span class="t-meta">(${readToken(name)})</span>
                  <p class="t-body" style="line-height: var(${name}); margin-top: var(--space-1);">
                    Multiple lines of body copy show how the leading changes the texture of a
                    paragraph at each step of the ramp.
                  </p>
                </div>
              `,
            )}
          </div>
        `,
      )}
      ${section(
        "Families",
        html`
          <div style="display: grid; gap: var(--space-3);">
            ${FONT_FAMILIES.map(
              (name) => html`
                <div data-token="${name}">
                  <span style="font-family: var(${name}); font-size: var(--fs-18);">
                    The quick brown fox jumps over the lazy dog
                  </span>
                  <div class="t-mono-sm" style="color: var(--fg-soft);"
                    >${name} — ${readToken(name)}</div
                  >
                </div>
              `,
            )}
          </div>
        `,
      )}
    </div>
  `,

  async play({ canvasElement, step }) {
    await step("renders one specimen per utility class plus the tabular-nums demo", async () => {
      const specimens = canvasElement.querySelectorAll("[data-type-specimen]");
      expect(specimens.length).toBe(TYPE_SPECIMENS.length + 1);
    });

    await step(".t-body composes the body rung (14px)", async () => {
      const body = canvasElement.querySelector('[data-type-specimen="t-body"] .t-body');
      expect(getComputedStyle(/** @type {Element} */ (body)).fontSize).toBe("14px");
    });

    await step("the sans stack declares self-hosted Inter", async () => {
      // Assert the declared stack, never the painted font — paint races the
      // webfont load and would flake.
      expect(readToken("--font-sans")).toContain("Inter");
    });

    await step("--fw-medium carries the interactive-controls weight contract", async () => {
      expect(readToken("--fw-medium")).toBe("500");
    });
  },
};

export const Spacing = {
  render: () => html`
    <div style="padding: var(--space-6);">
      <h2 style="margin-bottom: var(--space-2);">Spacing &amp; layout</h2>
      <p class="t-meta" style="margin-bottom: var(--space-6); max-width: var(--maxw-narrow);">
        The spacing scale sits on a 4px base and deliberately skips steps above
        <code>--space-6</code> (no 7, 9, 11, …) — large gaps come from the named rungs, not
        arithmetic. <code>--control-height</code> and the <code>--maxw-*</code> page widths are
        sizing/layout coordination tokens, grouped here because they answer the same "how big is the
        chrome" question.
      </p>

      ${section(
        "Spacing scale",
        html`
          <div style="display: grid; gap: var(--space-2);">
            ${SPACING.map(
              (name) => html`
                <div
                  data-token="${name}"
                  style="display: flex; align-items: center; gap: var(--space-3);"
                >
                  <code class="t-mono-sm" style="min-width: 10ch;">${name}</code>
                  <span class="t-meta" style="min-width: 5ch;">${readToken(name)}</span>
                  <div
                    data-space-bar
                    style="width: var(${name}); height: var(--space-3); background: var(--orange-300); border-radius: var(--radius-1);"
                  ></div>
                </div>
              `,
            )}
          </div>
        `,
      )}
      ${section(
        "Control height",
        html`
          <div style="display: flex; align-items: center; gap: var(--space-3);">
            <div
              data-token="${CONTROL_HEIGHT}"
              style="height: var(--control-height); display: inline-flex; align-items: center; padding: 0 var(--space-3); border: 1px solid var(--border); border-radius: var(--radius-2); background: var(--bg-muted);"
            >
              <span class="t-body">Sample control</span>
            </div>
            <span class="t-meta">
              --control-height (${readToken(CONTROL_HEIGHT)}) — the md rung for buttons, selects,
              and inputs so adjacent controls align.
            </span>
          </div>
        `,
      )}
      ${section(
        "Layout widths",
        html`
          <div style="display: grid; gap: var(--space-2);">
            ${LAYOUT_WIDTHS.map(
              (name) => html`
                <div data-token="${name}">
                  <code class="t-mono-sm">${name}</code>
                  <span class="t-meta">(${readToken(name)} — bar shown at 1:8 scale)</span>
                  <div
                    style="width: calc(var(${name}) / 8); height: var(--space-3); background: var(--sand-300); border-radius: var(--radius-1); margin-top: var(--space-1);"
                  ></div>
                </div>
              `,
            )}
          </div>
        `,
      )}
    </div>
  `,

  async play({ canvasElement, step }) {
    await step("renders one bar per spacing step", async () => {
      const bars = canvasElement.querySelectorAll("[data-space-bar]");
      expect(bars.length).toBe(SPACING.length);
    });

    await step("--space-4 resolves to the 16px anchor", async () => {
      expect(readToken("--space-4")).toBe("16px");
    });

    await step("the --space-8 bar measures its real 32px width", async () => {
      const bar = canvasElement.querySelector('[data-token="--space-8"] [data-space-bar]');
      expect(/** @type {Element} */ (bar).getBoundingClientRect().width).toBe(32);
    });
  },
};

export const RadiiElevationMotion = {
  name: "Radii, Elevation & Motion",

  render: () => html`
    <div style="padding: var(--space-6);">
      <h2 style="margin-bottom: var(--space-2);">Radii, elevation &amp; motion</h2>
      <p class="t-meta" style="margin-bottom: var(--space-6); max-width: var(--maxw-narrow);">
        Radii stay tight and rectilinear to match the technical tone; elevation shadows are
        warm-tinted from <code>--ink-900</code> so they read as the same material as the text.
        Motion tokens are value rows — the easings and durations are consumed by transitions, not
        demos.
      </p>

      ${section(
        "Radii",
        grid(
          RADII.map(
            (name) => html`
              <figure data-token="${name}" style="margin: 0;">
                <div
                  data-radius-chip
                  style="height: var(--space-12); border-radius: var(${name}); background: var(--bg-muted); box-shadow: inset 0 0 0 1px var(--border-strong);"
                ></div>
                <figcaption class="t-mono-sm" style="padding-top: var(--space-1);">
                  ${name}
                  <span class="t-meta" style="display: block;">${readToken(name)}</span>
                </figcaption>
              </figure>
            `,
          ),
          "120px",
        ),
      )}
      ${section(
        "Elevation",
        html`
          <div
            style="background: var(--bg-sunken); padding: var(--space-6); border-radius: var(--radius-3); display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: var(--space-6);"
          >
            ${SHADOWS.map(
              (name) => html`
                <figure data-token="${name}" style="margin: 0;">
                  <div
                    data-shadow-card
                    style="height: var(--space-16); border-radius: var(--radius-2); background: var(--bg); box-shadow: var(${name});"
                  ></div>
                  <figcaption class="t-mono-sm" style="padding-top: var(--space-2);">
                    ${name}
                  </figcaption>
                </figure>
              `,
            )}
          </div>
        `,
      )}
      ${section(
        "Focus ring",
        html`
          <div
            style="display: flex; align-items: center; gap: var(--space-4); padding: var(--space-2);"
          >
            <div
              data-token="${FOCUS_RING}"
              data-focus-sample
              style="height: var(--control-height); display: inline-flex; align-items: center; padding: 0 var(--space-3); border: 1px solid var(--border); border-radius: var(--radius-2); background: var(--bg); box-shadow: var(--focus-ring);"
            >
              <span class="t-body">Focused control</span>
            </div>
            <span class="t-meta">
              --focus-ring — a complete box-shadow value (not a color), shown here permanently
              applied; real controls paint it on :focus-visible.
            </span>
          </div>
        `,
      )}
      ${section(
        "Motion",
        html`
          <div style="display: grid; gap: var(--space-2);">
            ${MOTION.map(
              (name) => html`
                <div
                  data-token="${name}"
                  data-motion-row
                  style="display: flex; gap: var(--space-3);"
                >
                  <code class="t-mono-sm" style="min-width: 18ch;">${name}</code>
                  <span class="t-meta">${readToken(name)}</span>
                </div>
              `,
            )}
          </div>
        `,
      )}
      ${section(
        "Runtime layout",
        html`
          <div data-token="${STATUS_BAR_HEIGHT}">
            <code class="t-mono-sm">--status-bar-height</code>
            <span class="t-meta">
              (currently ${readToken(STATUS_BAR_HEIGHT)}) — published at runtime by
              &lt;cts-log-detail-header&gt;'s sticky status bar; defaults to 0px so
              <code>top: var(--status-bar-height)</code> resolves cleanly on pages that never mount
              it.
            </span>
          </div>
        `,
      )}
    </div>
  `,

  async play({ canvasElement, step }) {
    await step("renders one chip per radius and one card per shadow", async () => {
      expect(canvasElement.querySelectorAll("[data-radius-chip]").length).toBe(RADII.length);
      expect(canvasElement.querySelectorAll("[data-shadow-card]").length).toBe(SHADOWS.length);
    });

    await step("--radius-pill paints the pill radius on its chip", async () => {
      const chip = canvasElement.querySelector('[data-token="--radius-pill"] [data-radius-chip]');
      expect(getComputedStyle(/** @type {Element} */ (chip)).borderRadius).toBe("999px");
    });

    await step("the focus-ring sample paints a real shadow", async () => {
      const sample = canvasElement.querySelector("[data-focus-sample]");
      const boxShadow = getComputedStyle(/** @type {Element} */ (sample)).boxShadow;
      expect(boxShadow).not.toBe("none");
      expect(boxShadow).toContain("3px");
    });

    await step("renders one value row per motion token", async () => {
      expect(canvasElement.querySelectorAll("[data-motion-row]").length).toBe(MOTION.length);
    });
  },
};
