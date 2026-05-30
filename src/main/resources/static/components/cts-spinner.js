const STYLE_ID = "cts-spinner-styles";

// Scoped CSS for the OIDF spinner. The host renders a tiny inline SVG sized
// via a token-aware data-size attribute. The ring is two-tone: a full-circle
// track stroked in --ink-100 (a faint warm grey) under a small indicator arc
// stroked in the brand --orange-400, so the spinner reads as a coloured bead
// travelling around a quiet track rather than a mono sweep. Animation is gated
// by prefers-reduced-motion: rotation when motion is allowed, a slow opacity
// pulse when the user has asked for reduced motion (mirroring the reduced
// motion treatment already used in cts-navbar.js and cts-log-detail-header.js
// where transitions stay present but motion is dampened rather than removed).
const STYLE_TEXT = `
cts-spinner {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
}
cts-spinner[data-size="sm"] svg { width: 24px; height: 24px; }
cts-spinner[data-size="md"] svg { width: 40px; height: 40px; }
cts-spinner[data-size="lg"] svg { width: 64px; height: 64px; }
cts-spinner svg {
  display: block;
  animation: cts-spinner-rotate 1.1s linear infinite;
}
cts-spinner svg circle {
  fill: none;
  stroke-linecap: round;
}
cts-spinner svg .cts-spinner-track {
  stroke: var(--ink-100);
}
cts-spinner svg .cts-spinner-indicator {
  stroke: var(--orange-400);
}
.cts-spinner-sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@keyframes cts-spinner-rotate {
  to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
  cts-spinner svg {
    animation: cts-spinner-pulse 1.4s ease-in-out infinite;
  }
  @keyframes cts-spinner-pulse {
    0%, 100% { opacity: 1; }
    50%      { opacity: 0.55; }
  }
}
`;

/**
 * Inject the cts-spinner scoped stylesheet into `<head>` exactly once.
 * Mirrors the idempotent pattern in cts-modal.js so the rule sheet is shared
 * across every spinner instance on a page without rendering duplicate style
 * tags.
 * @returns {void}
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Valid public size keys. Sizes map to a fixed px scale (24/40/64) rather
 * than design-system spacing tokens so the spinner remains usable on dense
 * surfaces (sm) and inside dedicated loading modals (lg) without callers
 * having to override box dimensions.
 * @type {Set<string>}
 */
const VALID_SIZES = new Set(["sm", "md", "lg"]);

/**
 * Token-driven activity spinner built on two inline SVG `<circle>`s — a
 * full-circle track (`--ink-100`) under a dash-clipped indicator arc
 * (`--orange-400`) — spun by a CSS rotation. Replaces the legacy animated GIF
 * used by the page-level loading modal on logs.html, schedule-test.html, plan-
 * detail.html, log-detail.html, running-test.html, and upload.html.
 *
 * The host carries `role="status"`, so AT consumers receive a live-region
 * announcement when the element appears. The optional `label` attribute is
 * mirrored to both `aria-label` on the host and a visually-hidden child span
 * — duplicating the accessible name in DOM means screen readers that prefer
 * DOM text over ARIA still announce it.
 *
 * @property {string} [size] - One of "sm" (24px), "md" (40px, default), or "lg" (64px).
 * @property {string} [label] - Accessible name announced when the spinner
 *   appears. Defaults to "Loading".
 */
class CtsSpinner extends HTMLElement {
  connectedCallback() {
    if (this._initialized) return;
    this._initialized = true;
    injectStyles();

    const rawSize = this.getAttribute("size");
    const size = rawSize && VALID_SIZES.has(rawSize) ? rawSize : "md";
    this.setAttribute("data-size", size);

    const label = this.getAttribute("label") || "Loading";
    this.setAttribute("role", "status");
    this.setAttribute("aria-label", label);

    // Inline SVG ring on a viewBox 0 0 50 50, r=20 centred. Two concentric
    // circles share the geometry: the track paints the full ring as a quiet
    // --ink-100 backdrop, and the indicator exposes a small ~quarter arc via
    // stroke-dasharray (circumference ≈ 125.7, so "31 95" shows ~25%). The
    // parent <svg> rotates, sweeping the orange indicator around the static-
    // looking (rotationally symmetric) track. Colours are set via CSS classes.
    const NS = "http://www.w3.org/2000/svg";
    const svg = document.createElementNS(NS, "svg");
    svg.setAttribute("viewBox", "0 0 50 50");
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("focusable", "false");

    const track = document.createElementNS(NS, "circle");
    track.setAttribute("class", "cts-spinner-track");
    track.setAttribute("cx", "25");
    track.setAttribute("cy", "25");
    track.setAttribute("r", "20");
    track.setAttribute("stroke-width", "4");

    const indicator = document.createElementNS(NS, "circle");
    indicator.setAttribute("class", "cts-spinner-indicator");
    indicator.setAttribute("cx", "25");
    indicator.setAttribute("cy", "25");
    indicator.setAttribute("r", "20");
    indicator.setAttribute("stroke-width", "4");
    indicator.setAttribute("stroke-dasharray", "31 95");
    indicator.setAttribute("stroke-dashoffset", "0");
    svg.append(track, indicator);

    const srText = document.createElement("span");
    srText.className = "cts-spinner-sr-only";
    srText.textContent = label;

    this.replaceChildren(svg, srText);
  }
}

if (!customElements.get("cts-spinner")) {
  customElements.define("cts-spinner", CtsSpinner);
}

export {};
