import { LitElement, html, nothing } from "lit";
import { classMap } from "lit/directives/class-map.js";
import "./cts-badge.js";
import "./cts-failure-summary.js";

/**
 * Symbol + variant for the per-block status pills inside each rail row.
 * Matches BLOCK_BADGE_SPECS in cts-log-viewer (single source of truth lives
 * with the inline summaries — duplicated as a tiny constant here so the rail
 * doesn't import private internals from the viewer). Keep the two in
 * lockstep when adding/removing keys. INFO is intentionally absent — a block
 * with 47 INFO and zero problems should read clean, not noisy.
 * @type {Array<{ key: string, symbol: string, variant: string }>}
 */
const BLOCK_BADGE_SPECS = [
  { key: "success", symbol: "✓", variant: "pass" },
  { key: "failure", symbol: "✗", variant: "fail" },
  { key: "warning", symbol: "⚠", variant: "warn" },
  { key: "review", symbol: "◆", variant: "review" },
];

const STYLE_ID = "cts-log-toc-styles";

// Scoped CSS for the right-rail TOC. The host's outer visibility (≥ 1440px
// AND user-preference enabled) is owned by the page CSS in
// log-detail-v2.html — this stylesheet just describes the rail's own
// internal layout.
const STYLE_TEXT = `
  cts-log-toc {
    display: block;
    position: sticky;
    top: var(--status-bar-height, 0);
    align-self: start;
    max-height: calc(100vh - var(--status-bar-height, 0px) - var(--space-4));
    overflow-y: auto;
    padding: var(--space-4);
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    box-shadow: var(--shadow-1);
  }

  cts-log-toc .ctsLogTocTitle {
    margin: 0 0 var(--space-3) 0;
    font-size: var(--fs-12);
    font-weight: var(--fw-bold);
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--fg-soft);
  }

  cts-log-toc .ctsLogTocList {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  cts-log-toc .ctsLogTocItem {
    display: block;
  }

  cts-log-toc .ctsLogTocRow {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    width: 100%;
    padding: var(--space-2) var(--space-3);
    background: transparent;
    color: var(--fg);
    border: 1px solid transparent;
    border-radius: var(--radius-2);
    font: inherit;
    text-align: left;
    cursor: pointer;
  }
  cts-log-toc .ctsLogTocRow:hover {
    background: var(--bg-muted);
  }
  cts-log-toc .ctsLogTocRow:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-log-toc .ctsLogTocItem.is-active .ctsLogTocRow {
    background: var(--bg-muted);
    border-color: var(--border-strong);
    font-weight: var(--fw-bold);
  }

  cts-log-toc .ctsLogTocLabel {
    font-size: var(--fs-13);
    color: var(--fg);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  cts-log-toc .ctsLogTocCounts {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-1);
  }

  cts-log-toc .ctsLogTocFailures {
    margin-top: var(--space-4);
    padding-top: var(--space-3);
    border-top: 1px solid var(--border);
  }
  cts-log-toc .ctsLogTocFailuresHeader {
    margin: 0 0 var(--space-2) 0;
    font-size: var(--fs-12);
    font-weight: var(--fw-bold);
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--fg-soft);
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

const USER_PREF_KEY = "cts-log-toc-rail-enabled";

function readPreference() {
  try {
    return localStorage.getItem(USER_PREF_KEY) !== "false";
  } catch {
    return true;
  }
}

function writePreference(enabled) {
  try {
    localStorage.setItem(USER_PREF_KEY, enabled ? "true" : "false");
  } catch {
    // Ignore — incognito or storage-disabled environments. The page-level
    // grid still adapts; the toggle just won't persist.
  }
}

/**
 * @typedef {object} BlockSummary
 * @property {string} blockId
 * @property {string} label
 * @property {{ success: number, failure: number, warning: number, review: number, info: number, total: number }} counts
 */

/**
 * Right-rail table of contents for the wide-viewport log-detail layout.
 * Lists each block in the entries stream with per-block status pills (R27
 * data sourced via cts-log-viewer.getBlockSummaries()). Acts as a
 * scroll-spy via IntersectionObserver and embeds a compact
 * cts-failure-summary below the block list when failures are present.
 *
 * Renders only when the page CSS context allows (≥ 1440px AND user
 * preference enabled). Below 1440px the page hides the host with
 * `display: none`; below the preference threshold the component itself
 * sets `display: none` so a future toggle from anywhere can flip the
 * preference without re-mounting.
 *
 * Light DOM. Scoped CSS injected once on first render.
 *
 * @property {Array<BlockSummary>} blocks - Block descriptors keyed by
 *   blockId, with the startBlock entry's `msg` as the human label.
 *   Sourced from cts-log-viewer.getBlockSummaries().
 * @property {Array<object>} failures - Failure list passed through to the
 *   embedded compact failure summary; empty array hides the section.
 * @property {string} testId - Forwarded to the compact failure summary
 *   so deep-URL chips include the test id.
 * @fires cts-scroll-to-block - When a block row is clicked, with
 *   `{ detail: { blockId } }`. Bubbles + composed. Distinct from
 *   cts-scroll-to-entry because the target is a <details> block-start,
 *   not a leaf entry.
 */
class CtsLogToc extends LitElement {
  static properties = {
    blocks: { type: Array },
    failures: { type: Array },
    testId: { type: String, attribute: "test-id" },
    _activeBlockId: { state: true },
  };

  constructor() {
    super();
    /** @type {Array<BlockSummary>} */
    this.blocks = [];
    /** @type {Array<object>} */
    this.failures = [];
    this.testId = "";
    /** @type {string | null} */
    this._activeBlockId = null;
    /** @type {IntersectionObserver | null} */
    this._intersectionObserver = null;
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    if (!readPreference()) {
      this.style.display = "none";
    }
  }

  /**
   * Programmatic toggle for the user preference. Default home for the
   * toggle is U7's overflow popover — until that wires up the
   * "Hide log structure rail" / "Show log structure rail" item, this
   * setter is also reachable from the DevTools console:
   *   document.getElementById("ctsLogToc").setEnabled(false)
   */
  setEnabled(enabled) {
    writePreference(enabled);
    this.style.display = enabled ? "" : "none";
  }

  firstUpdated() {
    this._setupScrollSpy();
  }

  updated(changed) {
    super.updated?.(changed);
    // Re-observe whenever blocks change — the page bootstrap pushes a new
    // blocks array as the viewer polls; the corresponding <details> nodes
    // arrive on the same Lit microtask, so a single re-attach picks them
    // all up. IntersectionObserver targets must be DOM-attached before
    // .observe() runs, hence the firstUpdated + per-update reattach.
    if (changed.has("blocks")) {
      this._setupScrollSpy();
    }
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._intersectionObserver) {
      this._intersectionObserver.disconnect();
      this._intersectionObserver = null;
    }
  }

  _setupScrollSpy() {
    if (this._intersectionObserver) {
      this._intersectionObserver.disconnect();
      this._intersectionObserver = null;
    }
    if (typeof IntersectionObserver === "undefined") return;
    const targets = Array.from(document.querySelectorAll("details.logBlock")).filter(
      (el) => /** @type {HTMLElement} */ (el).dataset.blockId,
    );
    if (targets.length === 0) return;
    this._intersectionObserver = new IntersectionObserver(
      (entries) => this._handleIntersect(entries),
      {
        // Trigger when a block's top crosses below a fixed offset that
        // matches the typical sticky bar + banner stack height. Reading
        // --status-bar-height at observe-time is not supported, so this is
        // an approximation; if the active highlight feels off-by-one in
        // practice, replace with a ResizeObserver on the bar.
        rootMargin: "-80px 0px -50% 0px",
        threshold: [0, 0.1, 0.5, 1],
      },
    );
    const observer = this._intersectionObserver;
    targets.forEach((el) => observer.observe(el));
  }

  _handleIntersect(entries) {
    const inView = entries
      .filter((e) => e.isIntersecting)
      .sort((a, b) => a.target.getBoundingClientRect().top - b.target.getBoundingClientRect().top);
    if (inView.length === 0) return;
    const topMost = /** @type {HTMLElement} */ (inView[0].target).dataset.blockId;
    if (topMost && topMost !== this._activeBlockId) {
      this._activeBlockId = topMost;
    }
  }

  _handleBlockClick(event) {
    const target = /** @type {HTMLElement} */ (event.currentTarget);
    const blockId = target && target.dataset ? target.dataset.blockId : undefined;
    if (!blockId) return;
    this.dispatchEvent(
      new CustomEvent("cts-scroll-to-block", {
        bubbles: true,
        composed: true,
        detail: { blockId },
      }),
    );
  }

  _renderBlockBadges(counts) {
    if (!counts || counts.total === 0) return nothing;
    return BLOCK_BADGE_SPECS.map(({ key, symbol, variant }) => {
      const n = counts[key];
      if (!n) return nothing;
      return html`<cts-badge variant="${variant}" label="${symbol}${n}"></cts-badge>`;
    });
  }

  _renderRow(block) {
    const isActive = block.blockId === this._activeBlockId;
    return html`<li
      class=${classMap({ ctsLogTocItem: true, "is-active": isActive })}
      data-testid="toc-row-${block.blockId}"
    >
      <button
        type="button"
        class="ctsLogTocRow"
        data-block-id="${block.blockId}"
        aria-current=${isActive ? "location" : "false"}
        @click=${this._handleBlockClick}
      >
        <span class="ctsLogTocLabel">${block.label}</span>
        <span class="ctsLogTocCounts">${this._renderBlockBadges(block.counts)}</span>
      </button>
    </li>`;
  }

  render() {
    const blocks = Array.isArray(this.blocks) ? this.blocks : [];
    const failures = Array.isArray(this.failures) ? this.failures : [];
    return html`<aside aria-label="Log table of contents">
      <h3 class="ctsLogTocTitle">Test structure</h3>
      ${blocks.length === 0
        ? nothing
        : html`<ol class="ctsLogTocList" role="list" data-testid="toc-list">
            ${blocks.map((block) => this._renderRow(block))}
          </ol>`}
      ${failures.length > 0
        ? html`<section class="ctsLogTocFailures" aria-label="Failure summary">
            <h4 class="ctsLogTocFailuresHeader">Failure summary</h4>
            <cts-failure-summary
              compact
              .failures=${failures}
              .testId=${this.testId}
            ></cts-failure-summary>
          </section>`
        : nothing}
    </aside>`;
  }
}

customElements.define("cts-log-toc", CtsLogToc);

export { USER_PREF_KEY as CTS_LOG_TOC_PREF_KEY };
