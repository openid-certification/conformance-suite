import "./cts-icon.js";

/**
 * Canonical design-system status variants. These map to the
 * `--status-*` token group in `oidf-tokens.css` via the scoped CSS in
 * STYLE_TEXT. Use these for any badge that conveys test outcome.
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
 *   - `secondary` — neutral tag/chip (e.g. spec requirement labels)
 *   - `danger`    — role marker (e.g. the ADMIN badge in the navbar)
 *   - `info-subtle` — informational call-out, retokenized onto the
 *     `--status-info-*` palette (used by federation entity section blocks
 *     in schedule-test.html and aggregated info-message counts)
 * These intentionally live outside STATUS_VARIANT_CLASSES; they are not
 * test outcomes.
 * @type {Object.<string, string>}
 */
const UTILITY_VARIANT_CLASSES = {
  primary: "bg-primary",
  secondary: "bg-secondary",
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
 * `b-rev` (Review) reuses the `--status-info-*` palette: the design
 * archive's preview renders Review on a white surface with a
 * `--ink-300` border / `--ink-700` text. The token system does not
 * currently define a separate Review palette, so we use the calmer
 * blue info palette to differentiate Review from neutral skipped
 * content while staying within the published tokens. Document this
 * deviation in the JSDoc and revisit if a `--status-review-*` group
 * lands in the archive.
 *
 * `b-info-subtle` retokenizes the legacy Bootstrap `info-subtle` look
 * (used for section description blocks in `schedule-test.html`) onto
 * the same `--status-info-*` palette.
 */
const STYLE_TEXT = `
  cts-badge .badge {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    /* Padding reduced from 3px to 2px to absorb the line-height bump from
       11px (line-height: 1) to 16px while keeping visible height close to
       the previous 17px (was 3+11+3, now 2+16+2 = 20px). */
    padding: 2px 10px;
    border-radius: var(--radius-pill, 999px);
    font-size: 11px;
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
       placement is independent of inner content. Pair with border-box so the
       1px border on b-rev doesn't grow the box and shift it up either. */
    vertical-align: middle;
    box-sizing: border-box;
  }
  cts-badge .badge:has(br) {
    border-radius: 9px;
    text-transform: none;
    letter-spacing: 0;
  }
  cts-badge .badge i.bi {
    font-size: 11px;
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
    background: var(--bg, #fff);
    border: 1px solid var(--border-strong, #C7C2B8);
    color: var(--ink-700, #322E28);
  }
  cts-badge .b-info-subtle {
    background: var(--status-info-bg);
    border: 1px solid var(--status-info-border);
    color: var(--ink-900);
    text-transform: none;
    letter-spacing: 0;
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
 * circular spinner" decision (no `bi bi-arrow-clockwise` icon).
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
 * @property {boolean} clickable - Gives the badge a `button` role,
 *   keyboard support, and emits `cts-badge-click` on activation
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
  static observedAttributes = ["variant", "label", "count", "icon", "pill", "clickable"];

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
   * @returns {boolean}
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
    const icon = this.getAttribute("icon") || "";
    const label = this.getAttribute("label") || "";
    const countAttr = this.getAttribute("count");
    const hasCount = countAttr !== null && countAttr !== "";
    const hasLabel = label !== "";
    const useSlot = !hasCount && !hasLabel && this._capturedChildren.length > 0;
    const running = this._isRunning();

    const span = document.createElement("span");
    span.className = `badge ${variantClass}`;
    if (clickable) {
      span.setAttribute("role", "button");
      span.setAttribute("tabindex", "0");
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
