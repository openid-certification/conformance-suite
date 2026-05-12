import "./cts-icon.js";

/**
 * Canonical design-system status variants. These map to the
 * `--status-*` token group in `oidf-tokens.css` via the scoped CSS in
 * STYLE_TEXT. Use these for any badge that conveys test outcome.
 *
 * Every variant supports two affordance states:
 *   - **Read-only** (default) — fill only, no border ring. The badge is
 *     a label for state, not a click target.
 *   - **Interactive** — fill + 1px inset `box-shadow` ring + hover/focus.
 *     Set via `interactive` (visual only) or `clickable` (visual +
 *     `role="button"` + keyboard support + event). The ring is the
 *     affordance signal that distinguishes interactable badges from
 *     read-only labels at a glance.
 * @type {Object.<string, string>}
 */
const STATUS_VARIANT_CLASSES = {
  pass: "b-pass",
  fail: "b-fail",
  warn: "b-warn",
  running: "b-run",
  skip: "b-skip",
  review: "b-rev",
};

/**
 * Utility variants kept for non-status uses where the canonical status
 * palette is the wrong semantic fit:
 *   - `primary`   — emphasis (e.g. an active filter pill)
 *   - `secondary` — neutral tag/chip (e.g. spec requirement labels);
 *     scoped in STYLE_TEXT as `b-secondary` (mono font, neutral surface).
 *     Read-only by default; opt into the ring via `interactive` /
 *     `clickable` to signal affordance.
 *   - `danger`    — role marker (e.g. the ADMIN badge in the navbar)
 *   - `info-subtle` — informational call-out, retokenized onto the
 *     `--status-info-*` palette (used by federation entity section blocks
 *     in schedule-test.html and aggregated info-message counts)
 * These intentionally live outside STATUS_VARIANT_CLASSES; they are not
 * test outcomes. Like the status variants, all utility variants support
 * both read-only and interactive affordance states.
 * @type {Object.<string, string>}
 */
const UTILITY_VARIANT_CLASSES = {
  primary: "bg-primary",
  secondary: "b-secondary",
  danger: "bg-danger",
  "info-subtle": "b-info-subtle",
};

const STYLE_ID = "cts-badge-styles";

/**
 * Scoped CSS for the design-system badge variants. Mirrors the design
 * archive's `project/preview/components-badges.html` rules but routes
 * every color through the status palette tokens vendored in
 * `oidf-tokens.css`. Single-line badges use the pill radius
 * (`--radius-pill`); multi-line content (badges containing a `<br>`)
 * collapses to the 9px corner specified in the design archive.
 *
 * **Affordance rule:** every variant supports both an interactive and a
 * read-only state. The interactive state adds a 1px inset `box-shadow`
 * ring on top of the variant's fill; the read-only state renders the
 * fill only. The class `is-interactive` on the inner `<span class="badge">`
 * toggles the ring — the render path adds it whenever `interactive` or
 * `clickable` is set on the host. Read-only is the default; the ring
 * communicates "you can click this" and is reserved for badges where the
 * affordance is real.
 *
 * `b-rev` (Review) renders on `--bg-muted` (warm-neutral, #F8F7F5) with
 * `--ink-700` text. The fill is identical in both states — the ring is
 * the only difference. Without the ring, a white background would
 * vanish against the page; `--bg-muted` keeps Review legible as a
 * read-only chip. The token system does not currently define a
 * `--status-review-bg`; revisit if one lands in the archive.
 *
 * `b-info-subtle` retokenizes the legacy Bootstrap `info-subtle` look
 * (used for section description blocks in `schedule-test.html`) onto
 * the same `--status-info-*` palette.
 *
 * Interactive variants use an inset 1px box-shadow as a "simili-border"
 * rather than the `border` property. This keeps the box-model
 * dimensions identical to the read-only state — toggling `interactive`
 * on/off produces no 1px reflow, and inline-flex centering is
 * insensitive to whether a ring is drawn. Inset (vs. outset) keeps
 * the rendered footprint inside the border box, so a ringed chip
 * occupies the exact same pixels as a ringless one.
 */
const STYLE_TEXT = `
  cts-badge {
    /* Host defaults to inline, which means its height collapses to the
       inherited line-box (e.g. ~24px at line-height 1.5 × 16px) rather
       than the actual 20px badge. In a flex container with
       align-items: center, that mismatched line-box pushes the visible
       pill off-center relative to sibling text (e.g. cts-batch-runner
       tiles, where the PASSED/FAILED/WARNING badges sat ~2px below the
       runner name). Promoting the host to inline-flex makes its box
       exactly track the inner .badge, so flex centering aligns to what
       the user sees. vertical-align: middle preserves the existing
       inline-text behavior next to surrounding prose. */
    display: inline-flex;
    vertical-align: middle;
  }
  cts-badge .badge {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    /* Padding reduced from 3px to 2px to absorb the line-height bump from
       11px (line-height: 1) to 16px while keeping visible height close to
       the previous 17px (was 3+11+3, now 2+16+2 = 20px). */
    padding: 2px 10px;
    border-radius: var(--radius-pill, 999px);
    font-size: var(--fs-11);
    font-weight: var(--fw-medium, 500);
    letter-spacing: 0.06em;
    text-transform: uppercase;
    /* Even-pixel line-height aligned to the 16px inline-chrome rhythm shared
       with cts-button. Mixed inline (badge next to button) now have matching
       line-box heights, so cap-heights land at the same y-coordinate instead
       of drifting by Inter's metric residuals. */
    line-height: 16px;
    /* The inline-flex baseline is synthesized from the first flex item, so a
       text-only badge anchors on the text baseline while a spinner-led badge
       (running) anchors on the bottom of the empty inline-block, which pushes
       the badge upward relative to its text-only neighbours. Pin to middle so
       placement is independent of inner content. Bordered variants use an
       inset box-shadow ring (see below) instead of the border property,
       so the box dimensions are independent of the variant. */
    vertical-align: middle;
  }
  cts-badge .badge:has(br) {
    border-radius: 9px;
    text-transform: none;
    letter-spacing: 0;
  }
  cts-badge .b-pass {
    background: var(--status-pass-bg);
    color: var(--status-pass);
  }
  cts-badge .b-fail {
    background: var(--status-fail-bg);
    color: var(--status-fail);
  }
  cts-badge .b-warn {
    background: var(--status-warning-bg);
    color: var(--status-warning);
  }
  cts-badge .b-run {
    background: var(--status-running-bg);
    color: var(--status-running);
  }
  cts-badge .b-skip {
    background: var(--status-skipped-bg);
    color: var(--status-skipped);
  }
  cts-badge .b-rev {
    background: var(--bg-muted, #F8F7F5);
    color: var(--ink-700, #322E28);
  }
  cts-badge .b-info-subtle {
    background: var(--status-info-bg);
    color: var(--ink-900);
    text-transform: none;
    letter-spacing: 0;
  }
  /* Neutral chip variant — used for spec requirement labels (e.g.
     OIDCC-3.1.3.7-6) and other tag-like content that should not adopt
     a saturated status color. Mono font signals "code-like identifier",
     normal-case + zero tracking signals "this is a label, not a banner".
     Horizontal padding is tightened from the default 10px to 8px to
     keep the chip compact next to neighbouring prose; vertical padding
     stays at the default 2px so the outer height matches the status
     pills (2 + 16 + 2 = 20px) — mixed rows of chips and status pills
     should sit at the same height. */
  cts-badge .b-secondary {
    background: var(--ink-50);
    color: var(--fg-muted);
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    font-weight: var(--fw-regular, 400);
    letter-spacing: 0;
    text-transform: none;
    padding: 2px var(--space-2);
  }
  /* Affordance rule: interactive badges (set via the 'interactive' or
     'clickable' host attribute, both reflected as 'is-interactive' on
     the inner span by the render path) carry a 1px inset box-shadow
     ring on top of the variant fill. Read-only badges have no ring.
     The ring color is tonally matched to each variant's fill — pure
     grey on a saturated fill reads as a misaligned border, so each
     status variant uses its own foreground token (the dark sibling of
     the variant fill) which keeps the ring inside the same hue family.
     Neutral variants (skip, secondary, info-subtle, review) keep their
     existing neutral ring tokens. */
  cts-badge .badge.is-interactive {
    box-shadow: inset 0 0 0 1px var(--border);
    cursor: pointer;
  }
  cts-badge .b-pass.is-interactive {
    box-shadow: inset 0 0 0 1px var(--status-pass);
  }
  cts-badge .b-fail.is-interactive {
    box-shadow: inset 0 0 0 1px var(--status-fail);
  }
  cts-badge .b-warn.is-interactive {
    box-shadow: inset 0 0 0 1px var(--status-warning);
  }
  cts-badge .b-run.is-interactive {
    box-shadow: inset 0 0 0 1px var(--status-running);
  }
  cts-badge .b-skip.is-interactive {
    box-shadow: inset 0 0 0 1px var(--status-skipped);
  }
  cts-badge .b-info-subtle.is-interactive {
    box-shadow: inset 0 0 0 1px var(--status-info-border);
  }
  cts-badge .b-rev.is-interactive {
    box-shadow: inset 0 0 0 1px var(--border-strong, #C7C2B8);
  }
  /* Hover and focus affordance — only on interactive badges. Read-only
     badges deliberately render no hover state so they read as labels,
     not buttons. filter: brightness darkens any variant fill
     uniformly without per-variant tuning. */
  cts-badge .badge.is-interactive:hover {
    filter: brightness(0.97);
  }
  cts-badge .badge.is-interactive:focus-visible {
    outline: 2px solid var(--rust-400, #C75A3F);
    outline-offset: 2px;
  }
  /* Inline anchors carry the badge's own color so a link inside an
     "info-subtle" badge reads blueish, not the global orange link color. */
  cts-badge .badge a {
    color: currentColor;
    text-decoration: underline;
    text-decoration-thickness: 1px;
    text-underline-offset: 2px;
  }
  cts-badge .badge a:hover {
    color: currentColor;
    opacity: 0.85;
  }
  /* info-subtle's text color is --ink-900 for prose readability, so links
     would inherit black. Nudge them to --status-info (the matching darker
     blue) so they read as links inside the blueish badge surface. */
  cts-badge .b-info-subtle a,
  cts-badge .b-info-subtle a:hover {
    color: var(--status-info);
  }
  cts-badge .cts-badge-spin {
    display: inline-block;
    width: 11px;
    height: 11px;
    animation: cts-badge-spin 1.1s linear infinite;
    transform-origin: 50% 50%;
  }
  cts-badge .cts-badge-spin svg {
    display: block;
  }
  @keyframes cts-badge-spin {
    to { transform: rotate(360deg); }
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Builds the inline SVG spinner used by the `running` variant. Matches
 * the design archive's "perfectly symmetric circular spinner" decision
 * (a 16x16 circle with a 90-degree arc rotating once every 1.1s). The
 * SVG inherits `currentColor` so the surrounding badge's text color
 * controls the spinner stroke.
 * @returns {HTMLSpanElement} A span wrapper with the spinning class and
 *   the static SVG glyph as its only child.
 */
function buildSpinner() {
  const wrap = document.createElement("span");
  wrap.className = "cts-badge-spin";
  wrap.setAttribute("aria-hidden", "true");
  // SVG nodes must be created in the SVG namespace; otherwise they render
  // as inert HTMLUnknownElements and the circle/path are invisible.
  const svgNs = "http://www.w3.org/2000/svg";
  const svg = document.createElementNS(svgNs, "svg");
  svg.setAttribute("viewBox", "0 0 16 16");
  svg.setAttribute("width", "11");
  svg.setAttribute("height", "11");
  const circle = document.createElementNS(svgNs, "circle");
  circle.setAttribute("cx", "8");
  circle.setAttribute("cy", "8");
  circle.setAttribute("r", "6");
  circle.setAttribute("fill", "none");
  circle.setAttribute("stroke", "currentColor");
  circle.setAttribute("stroke-opacity", "0.25");
  circle.setAttribute("stroke-width", "2");
  const arc = document.createElementNS(svgNs, "path");
  arc.setAttribute("d", "M8 2 a6 6 0 0 1 6 6");
  arc.setAttribute("fill", "none");
  arc.setAttribute("stroke", "currentColor");
  arc.setAttribute("stroke-width", "2");
  arc.setAttribute("stroke-linecap", "round");
  svg.appendChild(circle);
  svg.appendChild(arc);
  wrap.appendChild(svg);
  return wrap;
}

/**
 * Token-styled status badge. The canonical variants are the design-system
 * status palette names: `pass`, `fail`, `warn`, `running`, `skip`,
 * `review`. Each maps to a scoped class (`b-pass` / `b-fail` / `b-warn` /
 * `b-run` / `b-skip` / `b-rev`) that draws color from the
 * `--status-*` token group in `oidf-tokens.css`. Single-line badges use
 * the pill radius (`--radius-pill`); badges whose label wraps (detected
 * via a `<br>` in the slot) collapse to the 9px corner specified in the
 * design archive.
 *
 * `running` automatically renders an inline SVG spinner instead of any
 * `icon` attribute, matching the design archive's "perfectly symmetric
 * circular spinner" decision (no static glyph).
 *
 * Utility variants `primary`, `secondary`, `danger`, and `info-subtle`
 * are retained for non-status uses (filter emphasis, requirement chips,
 * the ADMIN role marker, federation entity section blocks). They are
 * intentionally *not* part of the status palette — pick one of the
 * canonical status names for any badge that conveys test outcome.
 *
 * @property {string} variant - Canonical status: pass, fail, warn,
 *   running, skip, review. Utility (non-status): primary, secondary,
 *   danger, info-subtle.
 * @property {string} label - Visible text
 * @property {number} count - Numeric content; overrides `label` when set
 * @property {string} icon - Bootstrap Icons name (without the `bi-`
 *   prefix). Ignored when `variant="running"` (the spinner replaces it).
 * @property {boolean} pill - Reserved for backwards compatibility. The
 *   default badge radius is now the pill radius, so this attribute is a
 *   no-op for status variants. It is still read so existing markup that
 *   sets `pill` continues to render unchanged.
 * @property {boolean} interactive - Visual-only affordance. Adds a 1px
 *   inset ring (and hover state) on top of the variant's fill so the
 *   badge reads as "you can click this." Use when the badge sits inside
 *   an interactive wrapper (`<a>`, `<button>`, parent click handler) and
 *   the wrapper does not already provide its own visible affordance. Does
 *   NOT add `role="button"` or keyboard handling — for that, use
 *   `clickable`.
 * @property {boolean} clickable - Gives the badge a `button` role,
 *   keyboard support, and emits `cts-badge-click` on activation. Implies
 *   `interactive` visually — a clickable badge always carries the
 *   affordance ring even when `interactive` is not set.
 *
 * When neither `label` nor `count` is set, the badge wraps whatever child
 * nodes are inside the host element. This is the only way to embed inline
 * `<a>`, `<em>`, or other rich content inside a badge.
 *
 * The slot children are captured ONCE on the first render and cached on
 * the instance (`_capturedChildren`). Every subsequent re-render moves
 * the same cached nodes into the newly-built badge wrapper, so inline
 * rich content survives attribute changes. HOWEVER, children APPENDED
 * AFTER the first render are not picked up — they land inside the
 * current `<span class="badge">` wrapper and get discarded when the next
 * re-render rebuilds the wrapper. If you need to swap in different rich
 * content dynamically, set the `label` attribute (for plain text), remove
 * + re-insert the element, or update `_capturedChildren` directly before
 * triggering a re-render.
 * @fires cts-badge-click - When the badge is clicked/activated while
 *   `clickable` is set. Bubbles and is composed.
 */
class CtsBadge extends HTMLElement {
  static observedAttributes = [
    "variant",
    "label",
    "count",
    "icon",
    "pill",
    "clickable",
    "interactive",
    "aria-label",
  ];

  connectedCallback() {
    if (this._initialized) return;
    this._initialized = true;
    ensureStylesInjected();
    this._render();
  }

  attributeChangedCallback() {
    if (this._initialized) this._render();
  }

  /**
   * Resolves the configured variant to its scoped CSS class. Falls back
   * to `b-rev` for unknown variants so the badge still renders with a
   * defined background/foreground rather than as unstyled inline text.
   * @returns {string} Class name for the inner span.
   */
  _variantClass() {
    const raw = this.getAttribute("variant") || "running";
    return (
      STATUS_VARIANT_CLASSES[raw] || UTILITY_VARIANT_CLASSES[raw] || STATUS_VARIANT_CLASSES.review
    );
  }

  /**
   * Returns true when the configured variant is the spinner-bearing
   * `running` variant.
   * @returns {boolean} Whether this badge should render the spinner glyph.
   */
  _isRunning() {
    return (this.getAttribute("variant") || "running") === "running";
  }

  _render() {
    // Capture the original child nodes once. After the first render the
    // host contains the rendered <span class="badge"> wrapper, so re-
    // rendering would otherwise nest spans recursively.
    if (this._capturedChildren === undefined) {
      this._capturedChildren = Array.from(this.childNodes);
    }

    const variantClass = this._variantClass();
    const clickable = this.hasAttribute("clickable");
    // `clickable` implies `interactive` visually; the inner-span class
    // is the single source of truth for the affordance ring. Reading
    // both attributes here (rather than reflecting one onto the other
    // via setAttribute) avoids any attributeChangedCallback re-entry.
    const interactive = clickable || this.hasAttribute("interactive");
    const icon = this.getAttribute("icon") || "";
    const label = this.getAttribute("label") || "";
    const countAttr = this.getAttribute("count");
    const hasCount = countAttr !== null && countAttr !== "";
    const hasLabel = label !== "";
    const useSlot = !hasCount && !hasLabel && this._capturedChildren.length > 0;
    const running = this._isRunning();

    const span = document.createElement("span");
    span.className = interactive ? `badge ${variantClass} is-interactive` : `badge ${variantClass}`;
    if (clickable) {
      span.setAttribute("role", "button");
      span.setAttribute("tabindex", "0");
      // Forward aria-label from the host so the role="button" inner span
      // has an accessible name. Without this, screen readers announce the
      // visible text only, which for icon-led badges (e.g. the log-entry
      // ID chip) is just a code like "LOG-0042" — losing the click hint.
      const ariaLabel = this.getAttribute("aria-label");
      if (ariaLabel) span.setAttribute("aria-label", ariaLabel);
      span.addEventListener("click", () => this._dispatchClick());
      span.addEventListener("keydown", (e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          this._dispatchClick();
        }
      });
    }

    if (running) {
      // Spinner replaces any icon attribute on the running variant.
      span.appendChild(buildSpinner());
      if (hasCount || hasLabel || useSlot) {
        span.appendChild(document.createTextNode(" "));
      }
    } else if (icon) {
      const iconEl = document.createElement("cts-icon");
      iconEl.setAttribute("name", icon);
      iconEl.setAttribute("aria-hidden", "true");
      span.appendChild(iconEl);
      if (hasCount || hasLabel || useSlot) {
        span.appendChild(document.createTextNode(" "));
      }
    }

    if (hasCount) {
      span.appendChild(document.createTextNode(countAttr));
    } else if (hasLabel) {
      span.appendChild(document.createTextNode(label));
    } else if (useSlot) {
      for (const child of this._capturedChildren) {
        span.appendChild(child);
      }
    }

    this.replaceChildren(span);
  }

  _dispatchClick() {
    this.dispatchEvent(new CustomEvent("cts-badge-click", { bubbles: true, composed: true }));
  }
}

customElements.define("cts-badge", CtsBadge);

export {};
