import { LitElement, html, nothing } from "lit";
import "./cts-icon.js";
import "./cts-badge.js";

/**
 * Maps a failure entry's `result` value to the canonical `cts-badge` variant
 * used as the severity indicator in front of each row. Lookup table per
 * components/AGENTS.md §7 (no dynamic class concatenation).
 * @type {Object.<string, string>}
 */
const RESULT_BADGE_VARIANTS = {
  FAILURE: "fail",
  WARNING: "warn",
  SKIPPED: "skip",
  INTERRUPTED: "fail",
};

const STYLE_ID = "cts-failure-summary-styles";

// Scoped CSS for the failure summary. Migrated verbatim from
// cts-log-detail-header.js (lines 223–276 of the file before U4) so the
// rendered look is identical when the component lives inside the header
// card. The `[compact]` block adds the wide-rail rendering — no chevron,
// no requirement badges, single-line ellipsis truncation. The component
// renders to its own light DOM, so selectors are scoped via the host
// element name (mirrors every other cts-* component in this directory).
const STYLE_TEXT = `
  cts-failure-summary {
    display: block;
  }

  cts-failure-summary .failureSummary {
    margin-top: var(--space-4);
    border-top: 1px solid var(--border);
    padding-top: var(--space-3);
  }
  cts-failure-summary .failureSummaryTitle {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-weight: var(--fw-bold);
    color: var(--fg);
    cursor: pointer;
    border-radius: var(--radius-2);
  }
  cts-failure-summary .failureSummaryTitle:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-failure-summary .failureList {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin-top: var(--space-3);
  }
  /* In grouped mode the outer list stacks groups (not rows). Bump the gap
     between groups (so each cluster reads as discrete) and the gap below
     the section title (so the section break is unambiguously larger than
     the inter-group break). Spacing scale: 20 / 16 / 8 / 4 from
     section -> group -> header-row -> row-row. */
  cts-failure-summary[group-by-block] .failureList {
    gap: var(--space-4);
    margin-top: var(--space-5);
  }
  cts-failure-summary .failureBlockGroup + .failureBlockGroup {
    /* Gap on the outer flex list now owns the inter-group spacing; the
       legacy sibling-margin would stack on top of it. */
    margin-top: 0;
  }
  /* Block-group label sits one step below the section title in the
     hierarchy: smaller, lighter, slightly tracked. Reads as a data-
     structure label, not as competing prose. */
  cts-failure-summary .failureBlockHeader {
    font-size: var(--fs-12);
    font-weight: var(--fw-medium);
    color: var(--fg-soft);
    letter-spacing: 0.02em;
    margin-top: 0;
    margin-bottom: var(--space-2);
  }
  cts-failure-summary .failureBlockCount {
    color: var(--fg-faint);
    font-weight: var(--fw-regular);
    margin-left: var(--space-2);
    /* tabular-nums keeps multi-digit counts (e.g. (2 failures) vs.
       (11 failures)) baseline-aligned across stacked group headers. */
    font-variant-numeric: tabular-nums;
  }
  /* Tighter row rhythm inside a group — rows feel like a cluster bound
     to the label above, separated from neighboring clusters by the
     larger outer-list gap. */
  cts-failure-summary .failureBlockGroup .failureItem + .failureItem {
    margin-top: var(--space-1);
  }
  cts-failure-summary .failureItem {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--fs-13);
  }
  cts-failure-summary .failureText {
    color: var(--fg);
    cursor: pointer;
    text-decoration: underline;
    text-decoration-thickness: 1px;
    text-underline-offset: 2px;
    border-radius: var(--radius-2);
  }
  cts-failure-summary .failureText:hover { color: var(--fg-link); }
  cts-failure-summary .failureText:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-failure-summary .logRequirementBadge {
    display: inline-block;
    background: var(--ink-50);
    border: 1px solid var(--border);
    border-radius: var(--radius-pill);
    color: var(--fg-muted);
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    padding: 1px var(--space-2);
  }

  /* Compact rendering for the wide-viewport rail (U8). Drop the title +
     requirement badges to stay inside the rail's narrow width budget;
     each row collapses to a single ellipsised line. */
  cts-failure-summary[compact] .failureSummaryTitle { display: none; }
  cts-failure-summary[compact] .failureBlockHeader { display: none; }
  cts-failure-summary[compact] .failureList {
    margin-top: 0;
    gap: var(--space-1);
  }
  cts-failure-summary[compact] .failureItem {
    font-size: var(--fs-12);
    flex-wrap: nowrap;
    min-width: 0;
  }
  cts-failure-summary[compact] .failureText {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    min-width: 0;
  }
  cts-failure-summary[compact] .logRequirementBadge { display: none; }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * @typedef {object} FailureEntry
 * @property {string} _id - Server-side log entry ID; emitted in the
 *   `cts-scroll-to-entry` event so the page-level handler can locate
 *   the matching `<cts-log-entry>`.
 * @property {string} result - One of FAILURE / WARNING / SKIPPED / INTERRUPTED.
 * @property {string} src - The condition class name (e.g. `EnsureValidAud`).
 * @property {string} msg - Human-readable failure message.
 * @property {Array<string>} [requirements] - Optional requirement IDs (rendered as chips).
 * @property {string} [blockId] - Optional block ID; consumed by `groupByBlock`.
 */

/**
 * Renders the failure summary block: heading + chevron + clickable list of
 * failure / warning / skipped / interrupted entries. Hoistable — the same
 * component renders inside `cts-log-detail-header` (desktop), directly below
 * the sticky status bar (mobile / tablet), and inside the wide-viewport rail
 * (U8) with `compact=true`. Three positions, one component, one event seam.
 *
 * Light DOM. Scoped CSS is injected once on first render. All visual styling
 * routes through the OIDF tokens vendored in `oidf-tokens.css`.
 *
 * The dispatched `cts-scroll-to-entry` event is `composed: true` AND
 * `bubbles: true` so the listener established at `document` level
 * (`js/log-detail-v2.js`) catches it from any DOM position the component
 * is mounted in — including future shadow-DOM hosts (e.g. if U8's rail
 * ever moves into a shadow root).
 *
 * @property {Array<FailureEntry>} failures - Failure / warning / skipped /
 *   interrupted entries. Filtered upstream from `testInfo.results`. Required.
 * @property {boolean} compact - Wide-viewport rail rendering: smaller
 *   typography, no chevron, single-line ellipsis truncation, no
 *   requirement chips. Reflects the `compact` attribute.
 * @property {boolean} groupByBlock - Group failures by `blockId` with a
 *   block header above each group. Reflects the `group-by-block`
 *   attribute. Defaults to false (flat list).
 * @fires cts-scroll-to-entry - When a failure row is clicked or activated
 *   by Enter / Space, with `{ detail: { entryId } }`. Bubbles AND is
 *   composed so the document-level listener catches the event regardless
 *   of where the component is mounted.
 */
class CtsFailureSummary extends LitElement {
  static properties = {
    failures: { type: Array },
    compact: { type: Boolean, reflect: true },
    groupByBlock: { type: Boolean, attribute: "group-by-block", reflect: true },
    _expanded: { state: true },
  };

  constructor() {
    super();
    /** @type {Array<FailureEntry>} */
    this.failures = [];
    this.compact = false;
    this.groupByBlock = false;
    this._expanded = true;
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  _toggle() {
    this._expanded = !this._expanded;
  }

  _handleTitleKeydown(event) {
    if (event.key !== "Enter" && event.key !== " ") return;
    event.preventDefault();
    this._toggle();
  }

  _dispatchScrollToEntry(entryId) {
    this.dispatchEvent(
      new CustomEvent("cts-scroll-to-entry", {
        bubbles: true,
        composed: true,
        detail: { entryId },
      }),
    );
  }

  _handleRowClick(event) {
    const entryId = event.currentTarget.dataset.entryId;
    if (entryId) this._dispatchScrollToEntry(entryId);
  }

  _handleRowKeydown(event) {
    if (event.key !== "Enter" && event.key !== " ") return;
    event.preventDefault();
    const entryId = event.currentTarget.dataset.entryId;
    if (entryId) this._dispatchScrollToEntry(entryId);
  }

  _renderRequirementBadges(requirements) {
    return (requirements || []).map((req) => html`<span class="logRequirementBadge">${req}</span>`);
  }

  _renderFailureRow(item) {
    return html`
      <div class="failureItem">
        <cts-badge
          variant="${RESULT_BADGE_VARIANTS[item.result] || "skip"}"
          label="${item.result}"
        ></cts-badge>
        ${this._renderRequirementBadges(item.requirements)}
        <span
          class="failureText"
          role="button"
          tabindex="0"
          data-entry-id=${item._id}
          @click=${this._handleRowClick}
          @keydown=${this._handleRowKeydown}
          >${item.src}: ${item.msg}</span
        >
      </div>
    `;
  }

  _renderFlatList(failures) {
    return html`
      <div class="failureList" data-testid="failure-list">
        ${failures.map((item) => this._renderFailureRow(item))}
      </div>
    `;
  }

  /**
   * Bucket failures by `blockId` while preserving each block's first-seen
   * order. `Object.groupBy` is intentionally avoided so this works on the
   * older runtime targets the conformance suite supports — same reason
   * other components hand-roll their grouping (`cts-log-viewer.js`).
   * @param {Array<FailureEntry>} failures
   */
  _groupFailuresByBlock(failures) {
    /** @type {Array<{ blockId: string, headerText: string, items: Array<FailureEntry> }>} */
    const groups = [];
    /** @type {Object.<string, number>} */
    const indexById = {};
    for (const item of failures) {
      const key = item.blockId || "";
      if (!(key in indexById)) {
        indexById[key] = groups.length;
        groups.push({
          blockId: key,
          headerText: item.msg || key || "Other failures",
          items: [],
        });
      }
      groups[indexById[key]].items.push(item);
    }
    return groups;
  }

  _renderGroupedList(failures) {
    const groups = this._groupFailuresByBlock(failures);
    return html`
      <div class="failureList" data-testid="failure-list">
        ${groups.map(
          (group) => html`
            <div class="failureBlockGroup" data-testid="failure-block-group">
              <div class="failureBlockHeader">
                ${group.headerText}
                <span class="failureBlockCount">${this._formatGroupCount(group.items.length)}</span>
              </div>
              ${group.items.map((item) => this._renderFailureRow(item))}
            </div>
          `,
        )}
      </div>
    `;
  }

  _formatGroupCount(n) {
    return `(${n} ${n === 1 ? "failure" : "failures"})`;
  }

  render() {
    const failures = Array.isArray(this.failures) ? this.failures : [];
    if (failures.length === 0) return nothing;

    const list = this.groupByBlock
      ? this._renderGroupedList(failures)
      : this._renderFlatList(failures);

    // Compact mode (U8 rail) skips the title+chevron entirely — the rail's
    // own header carries the section label, and a chevron toggle in the
    // narrow rail width is a poor affordance.
    if (this.compact) {
      return html`<div class="failureSummary" data-testid="failure-summary">${list}</div>`;
    }

    return html`
      <div class="failureSummary" data-testid="failure-summary">
        <div
          class="failureSummaryTitle"
          role="button"
          tabindex="0"
          @click=${this._toggle}
          @keydown=${this._handleTitleKeydown}
        >
          Failure summary:
          <cts-icon name="${this._expanded ? "chevron-up" : "chevron-down"}"></cts-icon>
        </div>
        ${this._expanded ? list : nothing}
      </div>
    `;
  }
}

customElements.define("cts-failure-summary", CtsFailureSummary);

export {};
