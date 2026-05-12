import { LitElement, html, nothing } from "lit";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-link-button.js";

/**
 * Maps a backend test status to a canonical cts-badge variant. RUNNING
 * triggers the spinning circular SVG; WAITING uses the warn palette to
 * signal that user action is required; INTERRUPTED maps to `fail` because
 * an interrupted run did not complete successfully.
 * @type {Object.<string, string>}
 */
const STATUS_BADGE_VARIANTS = {
  RUNNING: "running",
  WAITING: "warn",
  INTERRUPTED: "fail",
};

const STYLE_ID = "cts-running-test-card-styles";

// Identity-first layout: badge stacks above the test name as a single visual
// unit, the test ID becomes a quiet monospace caption, and metadata flows as
// a wrapping list rather than labelled rows. The host establishes
// `container-type: inline-size` (mirroring cts-log-entry / cts-plan-modules)
// so actions reflow from a right-aligned stack to a full-width bottom block
// based on the card's *container* width — the card behaves correctly in the
// running-test list, embedded panels, and on a phone, without viewport
// breakpoints.
//
// Metadata typography matches cts-plan-header / cts-log-detail-header
// conventions: bold keys (--fg-soft), full-contrast values (--fg), and a
// concatenated mono string for the variant set (matches
// cts-plan-header._formatVariant exactly so a reader sees variants the same
// way across the running-test, plan-detail, and log-detail surfaces).
//
// The 8px progress track on --ink-100 with a solid --orange-400 fill (no
// striped/pulsing animation) follows the design archive's
// components-progress.html "steady continuous tick" rule. The badge spinner
// is provided by cts-badge[variant="running"]; no extra motion is added.
const STYLE_TEXT = `
  cts-running-test-card {
    display: block;
    container-type: inline-size;
    container-name: ctsRunningTestCard;
  }
  .cts-rtc-card {
    display: grid;
    gap: var(--space-3);
    padding: var(--space-4);
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    box-shadow: var(--shadow-1);
    font-family: var(--font-sans);
    color: var(--fg);
  }
  .cts-rtc-header {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
    min-width: 0;
  }
  .cts-rtc-headline {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    min-width: 0;
    width: 100%;
  }
  .cts-rtc-name {
    font-size: var(--fs-18);
    line-height: var(--lh-tight);
    font-weight: var(--fw-bold);
    color: var(--fg);
    word-break: break-word;
  }
  .cts-rtc-id {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    line-height: var(--lh-snug);
    color: var(--fg-soft);
    word-break: break-all;
  }
  .cts-rtc-progress-track {
    height: 8px;
    background: var(--ink-100);
    border-radius: var(--radius-pill);
    overflow: hidden;
  }
  .cts-rtc-progress-fill {
    height: 100%;
    background: var(--orange-400);
    border-radius: var(--radius-pill);
    transition: width var(--dur-3) var(--ease-standard);
  }
  /* Meta strip stacks as a vertical list by default; items reflow into a
     wrapping single line on wider containers (see @container block below).
     Pure-gap separation — no inline middle-dots — because dots produce three
     wrap artifacts (orphaned trailing dot at end-of-row, leading bullet at
     start-of-row, uneven left/right whitespace when a dot ends a row). Both
     cts-plan-header and cts-log-detail-header use pure-gap meta layouts;
     matching that. */
  .cts-rtc-meta {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    font-size: var(--fs-13);
    line-height: var(--lh-snug);
    color: var(--fg);
  }
  .cts-rtc-meta-item {
    display: inline-flex;
    align-items: baseline;
    gap: var(--space-1);
  }
  .cts-rtc-meta-key {
    color: var(--fg-soft);
    font-weight: var(--fw-bold);
  }
  .cts-rtc-meta-value {
    color: var(--fg);
  }
  /* Mono treatment for technical identifiers (variant string, version).
     Mirrors cts-plan-header's .mono helper but without the ink-50 chip
     background and 1px 6px padding — those work in plan-header's grid cells
     where each value sits in its own row, but stack uncomfortably as a row
     of chips inside this card's wrapping meta strip. Mono font + smaller
     size is enough to distinguish them. */
  .cts-rtc-meta-value.is-mono {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
  }
  .cts-rtc-actions {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }
  .cts-rtc-actions cts-button,
  .cts-rtc-actions cts-link-button {
    display: block;
  }
  /* Wide layout: pull actions into a right-aligned stack alongside the
     content column. 560px is the same threshold cts-log-entry uses for its
     two-column → five-column flip and matches the visual point at which a
     ~200px action stack still leaves the headline a comfortable reading
     width. Below this, everything stacks; the actions become a full-width
     block at the bottom of the card. The meta strip flips to row-flow with
     a 16px column-gap; bare whitespace is enough visual separation, no
     dot separators needed. */
  @container ctsRunningTestCard (min-width: 560px) {
    .cts-rtc-card {
      grid-template-columns: minmax(0, 1fr) auto;
      grid-template-areas:
        "header   actions"
        "progress actions"
        "meta     actions";
      column-gap: var(--space-5);
      padding: var(--space-5);
    }
    .cts-rtc-header {
      grid-area: header;
    }
    .cts-rtc-progress {
      grid-area: progress;
    }
    .cts-rtc-meta {
      grid-area: meta;
      flex-direction: row;
      flex-wrap: wrap;
      /* Baseline-align across items so the mono variant/version (--fs-12)
         and the sans-serif "Created 5m ago" / "Owner: ..." items (--fs-13)
         share the same text baseline. Without this, the smaller mono items
         settle to the bottom of the flex line and visually drop ~1-2px
         below the larger items. */
      align-items: baseline;
      gap: var(--space-1) var(--space-4);
    }
    .cts-rtc-actions {
      grid-area: actions;
      align-self: start;
      min-width: 200px;
    }
  }
`;

function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Card summarising a currently running / waiting / interrupted test instance
 * with quick actions for downloading the log and viewing details.
 *
 * Identity-first layout: status badge stacks above the test name; the test
 * ID is a quiet monospace caption; metadata (created, variant, version,
 * owner) flows as an inline list rather than labelled rows. Actions reflow
 * from a right-aligned stack on wider containers to a full-width bottom
 * block on narrow containers via a container query at 560px.
 *
 * The progress bar is driven by the numeric `progress` prop (0-100, clamped).
 * It uses an `--ink-100` track with an `--orange-400` solid fill, per the
 * design archive's components-progress.html "steady continuous tick" rule
 * (no striped/pulsing animation). The fill width transitions with
 * `var(--dur-3) var(--ease-standard)` when the prop changes.
 *
 * The running-status spinner is provided by cts-badge[variant="running"]
 * (a 1.1s `currentColor` SVG arc). A static glyph is not rendered.
 *
 * @property {object} test - Test instance object; expects `_id`, `testName`,
 *   `created`, `status`, `variant`, `version`, `owner`.
 * @property {boolean} isAdmin - Reveals the Test Owner row. Reflects the
 *   `is-admin` attribute.
 * @property {number} progress - Optional progress percentage (0-100). When
 *   set, an orange progress bar is rendered between the headline and meta
 *   rows. Values outside the range are clamped.
 * @fires cts-download-log - When the Download Logs button is clicked, with
 *   `{ detail: { testId } }`; bubbles.
 */
class CtsRunningTestCard extends LitElement {
  static properties = {
    test: { type: Object },
    isAdmin: { type: Boolean, attribute: "is-admin" },
    progress: { type: Number },
  };

  constructor() {
    super();
    this.test = {};
    this.isAdmin = false;
    this.progress = NaN;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  createRenderRoot() {
    return this;
  }

  /**
   * Concatenates a `{key: value}` variant map into a single string (e.g.,
   * `client_auth_type: client_secret_basic, response_type: code`). Mirrors
   * cts-plan-header._formatVariant so the running-test card and plan
   * header surface variant data in identical form.
   * @param {Record<string, string> | undefined} variant - Map of variant keys to selected values from the runner payload.
   * @returns {string} The formatted `"key: value, key: value"` string, or `""` when no variant is provided.
   */
  _formatVariant(variant) {
    if (!variant || typeof variant !== "object") return "";
    return Object.entries(variant)
      .map(([key, value]) => `${key}: ${value}`)
      .join(", ");
  }

  /**
   * Returns a short relative-time string ("just now", "5m ago", "3h ago",
   * "2d ago") for recent timestamps and falls back to a locale date for
   * anything older than 7 days. Used in place of `Date.toString()` (which
   * on a phone renders 50+ characters of GMT pleasantries that crowd out
   * everything else). For a *running* test, "5m ago" is the operational
   * question — siblings render absolute dates because they describe
   * historical work.
   * @param {string | undefined} dateStr - ISO 8601 date string from the runner payload.
   * @returns {string} A short relative-time label, or a locale date for timestamps older than 7 days; `""` when input is missing/unparseable.
   */
  _formatRelativeDate(dateStr) {
    if (!dateStr) return "";
    const ts = Date.parse(dateStr);
    if (Number.isNaN(ts)) return "";
    const diffMs = Date.now() - ts;
    if (diffMs < 0) return "just now";
    const sec = Math.floor(diffMs / 1000);
    if (sec < 45) return "just now";
    const min = Math.floor(sec / 60);
    if (min < 60) return `${min}m ago`;
    const hr = Math.floor(min / 60);
    if (hr < 24) return `${hr}h ago`;
    const day = Math.floor(hr / 24);
    if (day < 7) return `${day}d ago`;
    return new Date(ts).toLocaleDateString();
  }

  /**
   * @param {string | undefined} dateStr - ISO 8601 date string from the runner payload.
   * @returns {string} The locale-formatted absolute date/time, or `""` when input is missing/unparseable.
   */
  _formatAbsoluteDate(dateStr) {
    if (!dateStr) return "";
    const ts = Date.parse(dateStr);
    if (Number.isNaN(ts)) return "";
    return new Date(ts).toLocaleString();
  }

  _handleDownload() {
    this.dispatchEvent(
      new CustomEvent("cts-download-log", {
        bubbles: true,
        detail: { testId: this.test._id },
      }),
    );
  }

  _renderProgressBar() {
    const raw = Number(this.progress);
    if (!Number.isFinite(raw)) return nothing;
    const clamped = Math.max(0, Math.min(100, raw));
    return html`
      <div
        class="cts-rtc-progress"
        role="progressbar"
        aria-valuenow="${clamped}"
        aria-valuemin="0"
        aria-valuemax="100"
        aria-label="Test progress"
      >
        <div class="cts-rtc-progress-track">
          <div class="cts-rtc-progress-fill" style="width: ${clamped}%;"></div>
        </div>
      </div>
    `;
  }

  /**
   * Builds the metadata strip. Each entry is a `.cts-rtc-meta-item`. The
   * variant set is concatenated into a single mono string ("a: x, b: y")
   * matching cts-plan-header's variant convention. "Created" and "Owner"
   * keep visible labels because their values are meaningless without
   * context (a relative time or a sub/iss pair); the variant string and
   * version are self-describing technical identifiers, so they appear
   * unlabelled.
   * @param {{ created?: string; variant?: Record<string, string>; version?: string; owner?: { sub?: string; iss?: string } }} test - Runner test payload whose metadata populates the strip.
   * @returns {ReturnType<typeof html> | typeof nothing} The metadata strip template, or `nothing` when no fields are present.
   */
  _renderMeta(test) {
    const items = [];

    if (test.created) {
      items.push(html`
        <span class="cts-rtc-meta-item" data-testid="meta-created">
          <span class="cts-rtc-meta-key">Created</span>
          <span
            class="cts-rtc-meta-value tabular-nums"
            title="${this._formatAbsoluteDate(test.created)}"
            >${this._formatRelativeDate(test.created)}</span
          >
        </span>
      `);
    }

    const variantStr = this._formatVariant(test.variant);
    if (variantStr) {
      items.push(html`
        <span class="cts-rtc-meta-item" data-testid="meta-variant">
          <span class="cts-rtc-meta-value is-mono">${variantStr}</span>
        </span>
      `);
    }

    if (test.version) {
      items.push(html`
        <span class="cts-rtc-meta-item" data-testid="meta-version">
          <span class="cts-rtc-meta-value is-mono">${test.version}</span>
        </span>
      `);
    }

    if (this.isAdmin && test.owner) {
      const ownerText = `${test.owner.sub}${test.owner.iss ? ` (${test.owner.iss})` : ""}`;
      items.push(html`
        <span class="cts-rtc-meta-item" data-testid="owner-row">
          <span class="cts-rtc-meta-key">Owner:</span>
          <span class="cts-rtc-meta-value">${ownerText}</span>
        </span>
      `);
    }

    if (items.length === 0) return nothing;
    return html`<div class="cts-rtc-meta">${items}</div>`;
  }

  render() {
    const test = this.test;
    if (!test || !test._id) return nothing;

    const badgeVariant = STATUS_BADGE_VARIANTS[test.status] || "skip";
    // R19: WAITING surfaces an action prompt as the visible label so the
    // user can tell at a glance that the system is waiting on them, not
    // working. The variant lookup above and any data-status hooks keep
    // reading the canonical enum literal (`test.status`) — only the
    // rendered text node carries the friendly label.
    const badgeLabel =
      test.status === "WAITING" ? "Waiting for user input" : test.status || "UNKNOWN";

    return html`
      <div class="cts-rtc-card" data-instance-id="${test._id}">
        <div class="cts-rtc-header">
          <cts-badge variant="${badgeVariant}" label="${badgeLabel}"></cts-badge>
          <div class="cts-rtc-headline">
            <span class="cts-rtc-name" data-testid="test-name">${test.testName}</span>
            <span class="cts-rtc-id" data-testid="test-id">${test._id}</span>
          </div>
        </div>
        ${this._renderProgressBar()} ${this._renderMeta(test)}
        <div class="cts-rtc-actions">
          <cts-button
            class="downloadBtn"
            variant="secondary"
            size="sm"
            icon="save"
            label="Download Logs"
            full-width
            @cts-click="${this._handleDownload}"
          ></cts-button>
          <cts-link-button
            class="viewBtn"
            variant="secondary"
            size="sm"
            icon="label"
            label="View Test Details"
            full-width
            href="log-detail.html?log=${encodeURIComponent(test._id)}"
          ></cts-link-button>
        </div>
      </div>
    `;
  }
}

customElements.define("cts-running-test-card", CtsRunningTestCard);

export {};
