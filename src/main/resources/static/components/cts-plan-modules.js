import { LitElement, html, nothing, css } from "lit";
import "./cts-badge.js";
import "./cts-button.js";
import "./cts-link-button.js";
import "./cts-tooltip.js";
import { statusBadgeVariant, statusLabel } from "../js/module-status.js";

/**
 * Stable identity key for a module entry. Used as the data-module-key
 * attribute the Run button reads back so the click handler resolves to
 * the right module regardless of array order.
 * @param {object} mod - Plan module with `testModule` and optional `variant`.
 * @returns {string} Content-derived key (testModule plus serialized variant).
 */
function _moduleKey(mod) {
  return `${mod.testModule}|${JSON.stringify(mod.variant ?? null)}`;
}

const STYLE_ID = "cts-plan-modules-styles";

// Scoped CSS for the plan-modules list. Adopts the design archive's
// `.module-row` pattern (`project/ui_kits/certification-suite/app.css`):
// 3-column grid `28px 1fr auto` with a mono row number, a name stack (name +
// status badge + instance id + variant), and a right-side action stack. The
// status badge lives inside the name stack rather than in its own column.
// Mono row-number styling uses `--font-mono` per the archive.
const STYLE_TEXT = css`
  cts-plan-modules {
    display: block;
  }
  cts-plan-modules .planModulesCard {
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    overflow: hidden;
    /* Establish a query container so per-row reflow tracks the card's
       own width rather than the viewport. At desktop widths with the
       cts-plan-actions rail beside this card the card can be ~700px
       even though the viewport is much wider, and the squeeze shows
       up there long before the 640px viewport breakpoint trips. */
    container-type: inline-size;
    container-name: planModulesCard;
  }
  /* Adrian Roselli "block link" pattern (pseudo-element overlay): the row is
     a non-interactive grid; the status-badge anchor (.moduleStatusLink) is the
     single real row-spanning link, and its ::after overlay covers the whole
     row so a click anywhere on the row lands on the test's log page. Nested
     interactive controls (action buttons, the module-name link, the help
     tooltip, and the badge anchor's own box) sit on z-index: 1 so they receive
     their own clicks. Rows with no test instance render no .moduleStatusLink,
     so they have no overlay and stay inert (R5).
     https://adrianroselli.com/2020/02/block-links-cards-clickable-regions-etc.html */
  cts-plan-modules .module-row {
    position: relative;
    display: grid;
    grid-template-columns: 28px 1fr auto;
    gap: var(--space-3);
    padding: var(--space-3) var(--space-4);
    border-bottom: 1px solid var(--ink-100);
    align-items: center;
    transition: background var(--dur-1) var(--ease-standard);
  }
  cts-plan-modules .module-row:last-child {
    border-bottom: 0;
  }
  cts-plan-modules .module-row:hover {
    background: var(--bg);
  }
  /* One-shot highlight flash applied by highlightModule() when a
     cts-plan-status overview segment is activated (R11), so the click lands
     the user on the matching row. The animation is gated behind
     prefers-reduced-motion: no-preference; under reduce the class is added but
     no animation plays (and highlightModule scrolls with behavior:'auto'). */
  @media (prefers-reduced-motion: no-preference) {
    cts-plan-modules .module-row.is-flash {
      animation: cts-plan-modules-row-flash 1.5s var(--ease-standard) 1;
    }
  }
  @keyframes cts-plan-modules-row-flash {
    from {
      background-color: var(--orange-100);
    }
    to {
      background-color: transparent;
    }
  }
  cts-plan-modules .module-row .num {
    font-family: var(--font-mono);
    font-size: var(--fs-11);
    color: var(--fg-soft);
  }
  cts-plan-modules .module-row .name {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    font-weight: var(--fw-regular);
    font-size: var(--fs-13);
    color: var(--fg);
    word-break: break-word;
  }
  /* Status badge sits directly under the name with the test instance id as
     plain mono text immediately after it (no label, no callout chrome). */
  cts-plan-modules .module-row .name .statusLine {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }
  /* The test instance id is a handle operators copy into other tools, so lift
     it above the row block-link overlay (z-index: 1) to keep it text-selectable
     — clicking the surrounding row still navigates, but the id itself can be
     dragged/copied. font-family is set here directly because the bare \`mono\`
     class has no rule outside \`.desc\`. */
  cts-plan-modules .module-row .name .statusLine .instanceId {
    position: relative;
    z-index: 1;
    color: var(--fg-soft);
    font-family: var(--font-mono);
    font-size: var(--fs-12);
  }
  /* The module name links to the same log-detail URL as the "View Logs"
     button when an instance exists. It reads as plain text at rest (inherits
     the row name colour, no underline). The whole-row block link drives its
     hover affordance: hovering the block-link surface (or the name itself)
     colours the name --orange-600 with no underline. This matches cts-plan-card,
     whose title likewise turns --orange-600 on hover — note that comes from the
     global \`a:hover\` rule (oidf-tokens.css), which is why cts-plan-card-name's
     own \`:hover\` rule only has to suppress the underline; the orange is
     inherited. Same here: the selectors below out-specify this base rule's
     \`color: inherit\` so the orange lands, while hovering a nested control
     (buttons, help, id) leaves the name at rest. Keyboard focus keeps the
     shared --orange-400 ring (.help-icon / .moduleStatusLink). */
  cts-plan-modules .module-row .name .moduleNameLink {
    position: relative;
    z-index: 1;
    color: inherit;
    font-weight: var(--fw-bold);
    text-decoration-line: none;
  }
  /* Only the block-link surface (the .moduleStatusLink ::after overlay, which
     spans the row) and the name itself highlight the name — NOT the nested
     controls lifted above the overlay (action buttons, help icon, instance id).
     This mirrors cts-plan-card's structural selectivity: hovering a nested
     control leaves the title at its resting colour because the control occludes
     the title's ::after. Here the overlay is owned by .moduleStatusLink, so
     \`:has(.moduleStatusLink:hover)\` matches "pointer over the block-link
     surface (or the badge)"; the second selector adds the name's own direct
     hover (the name is lifted above the overlay, so it never satisfies the
     :has() arm). A hovered button/help/id is above the overlay, so neither arm
     matches and the name stays at rest. */
  cts-plan-modules .module-row:has(.moduleStatusLink:hover) .name .moduleNameLink,
  cts-plan-modules .module-row .name .moduleNameLink:hover {
    color: var(--orange-600);
  }
  cts-plan-modules .module-row .name .moduleNameLink:focus-visible {
    outline: 2px solid var(--orange-400);
    outline-offset: 2px;
    border-radius: var(--radius-1);
  }
  /* Inline-flex row keeps the help-icon optically centred on the test
     module name without relying on vertical-align hacks. align-items:
     center sits the 16px icon on the same axis as the 13px text;
     the gap replaces a margin so icon-only rows behave the same as
     icon+text rows. flex-wrap lets long module names break across
     multiple visual lines while keeping the icon hugged to the end. */
  cts-plan-modules .module-row .name .nameLine {
    display: inline-flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-1);
  }
  cts-plan-modules .module-row .name .desc {
    color: var(--fg-soft);
    font-weight: var(--fw-regular);
    font-size: var(--fs-12);
    margin-top: 1px;
  }
  cts-plan-modules .module-row .name .desc .mono {
    font-family: var(--font-mono);
  }
  /* display: contents removes cts-tooltip's own box from layout so its
     child cts-icon becomes a direct flex child of .nameLine. Without
     this, cts-tooltip (a custom element, display: inline by default)
     carries an inherited text line-box even though it has no text
     content — the icon then sits within that ghost line-box, and the
     flex container centres the line-box rather than the icon itself,
     producing a sub-pixel optical drift. With contents, align-items:
     center aligns the 16×16 icon box directly. cts-tooltip's hover/
     focus wiring is unaffected because it targets the cts-icon child,
     not cts-tooltip's own box. */
  cts-plan-modules .module-row .name .help {
    display: contents;
  }
  /* position/z-index keep the help icon above the row overlay so its tooltip
     hover and keyboard focus still work. display: contents on .help means the
     lift lands on the icon itself. */
  cts-plan-modules .module-row .name .help-icon {
    position: relative;
    z-index: 1;
    color: var(--fg-faint);
  }
  cts-plan-modules .module-row .name .help-icon:focus-visible {
    outline: 2px solid var(--orange-400);
    outline-offset: 2px;
    border-radius: var(--radius-1);
  }
  /* Flex (not grid) so the action buttons can wrap onto a second line
     when the row reflows on narrow cards — grid-auto-flow does not wrap.
     justify-content hugs the buttons to the right edge whether they sit
     alongside the badge (wide) or fill their own row (narrow). The
     explicit align-items keeps button text on the same y-baseline as
     the badge label when the row's content height is taller than a
     single button (e.g. when the name column wraps to multiple lines). */
  cts-plan-modules .module-row .actionStack {
    /* z-index lift keeps the action buttons above the row block-link overlay
       so they receive their own clicks instead of the row navigation. */
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: row-reverse;
    flex-wrap: wrap;
    justify-content: flex-end;
    align-items: center;
    gap: var(--space-3);
  }
  cts-plan-modules .planModulesEmpty {
    padding: var(--space-5);
    text-align: center;
    color: var(--fg-soft);
  }
  /* R28 + whole-row block link: the status badge is wrapped in an anchor to
     log-detail when an instance exists. The anchor stays STATIC-positioned so
     its ::after overlay (position: absolute; inset: 0) resolves to the nearest
     positioned ancestor — the position: relative .module-row — and therefore
     spans the entire row, making the whole row a click target. The other
     interactive controls (.moduleNameLink, .help-icon, .actionStack) are
     lifted to z-index: 1 so they sit above this overlay and receive their own
     clicks. The badge's own foreground keeps charge of its text colour. */
  cts-plan-modules .moduleStatusLink {
    display: inline-flex;
    align-items: center;
    text-decoration-line: none;
    color: inherit;
    border-radius: var(--radius-pill);
  }
  cts-plan-modules .moduleStatusLink::after {
    content: "";
    position: absolute;
    inset: 0;
  }
  /* The outline draws on the anchor's own border box (a pseudo-element never
     carries its parent's outline), so the focus ring already hugs the badge —
     no positioning needed. Crucially the anchor MUST stay static here: making
     it position:relative on focus would re-anchor its ::after overlay to the
     badge box and collapse the whole-row hit area while the badge is focused. */
  cts-plan-modules .moduleStatusLink:focus-visible {
    outline: 2px solid var(--orange-400);
    outline-offset: 2px;
  }
  /* Narrow-card reflow. Drops the actions column and lets the action
     stack span the full row width on a second line, so module names
     and status badges keep their breathing room and touch targets
     stay at the cts-button minimum height. The 780px threshold
     covers two squeeze zones the viewport-only breakpoint missed:
     (1) ~900–1000px viewports where the action rail is still beside
     the card and the card itself is ~700px wide, and (2) the
     general phone path. Container queries route the decision through
     the card's own inline size, so the rail-beside vs rail-below
     state does not matter. */
  @container planModulesCard (max-width: 780px) {
    cts-plan-modules .module-row {
      grid-template-columns: 28px minmax(0, 1fr) auto;
      padding: var(--space-3);
      row-gap: var(--space-2);
    }
    cts-plan-modules .module-row .actionStack {
      grid-column: 1 / -1;
    }
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/**
 * Per-module rows for a plan-detail page. Each row shows status/result
 * badges, the test module name and variant, the last test instance, and
 * action buttons (Run / View Logs / Download Logs).
 *
 * Light DOM. Scoped CSS is injected once on first connect; rows adopt the
 * design archive's `.module-row` 3-column grid (`28px 1fr auto`), with the
 * status badge stacked under the name rather than in its own column. The whole
 * row is a block link to the test's log page (Adrian Roselli `::after` overlay
 * on the status anchor; nested controls lifted on z-index).
 *
 * @property {Array<object>} modules - Modules rendered from the plan-detail
 *   API response; see cts-plan-detail.stories.js for shape. Each module
 *   may carry an optional `firstFailureRef` string (e.g. `"LOG-0042"`)
 *   resolved by the page-level shim from `/api/log/{lastInstance}`. When
 *   `mod.result === "FAILED"` AND `firstFailureRef` is a non-empty
 *   string, the lozenge href is appended with `#{firstFailureRef}` so a
 *   click lands on the failure entry rather than the top of the log.
 *   The result-gate is intentional: a fixture that sets the field on a
 *   non-FAILED row produces no fragment.
 * @property {string} planId - Parent plan ID. Reflects the `plan-id`
 *   attribute.
 * @property {boolean} isReadonly - Hides the Run Test button. Reflects the
 *   `is-readonly` attribute.
 * @property {boolean} isImmutable - Hides the Run Test button on immutable
 *   plans. Reflects the `is-immutable` attribute.
 * @property {boolean} isPublic - Appends `&public=true` to log-detail links.
 *   Reflects the `is-public` attribute.
 * The status badge and the module name are each rendered as a real `<a>`
 * link to `log-detail.html` when a test instance exists (R28), so clicking
 * the lozenge or the name takes the user to that test's log page — the same
 * destination as the "View Logs" button. Modules with no instance render the
 * badge and name unwrapped.
 *
 * @fires cts-run-test - When the Run Test button is clicked, with
 *   `{ detail: { testModule, variant } }`; bubbles.
 * @fires cts-download-log - When the Download Logs button is clicked, with
 *   `{ detail: { testId } }`; bubbles.
 */
class CtsPlanModules extends LitElement {
  static properties = {
    modules: { type: Array },
    planId: { type: String, attribute: "plan-id" },
    isReadonly: { type: Boolean, attribute: "is-readonly" },
    isImmutable: { type: Boolean, attribute: "is-immutable" },
    isPublic: { type: Boolean, attribute: "is-public" },
  };

  constructor() {
    super();
    this.modules = [];
    this.planId = "";
    this.isReadonly = false;
    this.isImmutable = false;
    this.isPublic = false;
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  _formatVariant(variant) {
    if (!variant || typeof variant !== "object") return "";
    return Object.entries(variant)
      .map(([key, value]) => `${key}=${value}`)
      .join(", ");
  }

  _handleRunTest(e) {
    const key = e.currentTarget.dataset.moduleKey;
    const mod = this.modules?.find((m) => _moduleKey(m) === key);
    if (!mod) return;
    this.dispatchEvent(
      new CustomEvent("cts-run-test", {
        bubbles: true,
        detail: {
          testModule: mod.testModule,
          variant: mod.variant,
        },
      }),
    );
  }

  _handleDownloadLog(e) {
    const testId = e.currentTarget.dataset.instanceId;
    if (!testId) return;
    this.dispatchEvent(
      new CustomEvent("cts-download-log", {
        bubbles: true,
        detail: { testId },
      }),
    );
  }

  _getLastInstance(mod) {
    if (!mod.instances || mod.instances.length === 0) return null;
    return mod.instances[mod.instances.length - 1];
  }

  _canRunTest() {
    return !this.isReadonly && !this.isImmutable;
  }

  /**
   * Format a 1-based row number as a zero-padded string (matches the design
   * archive's "01" / "02" mono row numbers).
   * @param {number} index - 1-based row index.
   * @returns {string} 2-digit zero-padded number (e.g. "01"); falls back to
   *   the raw string when the count exceeds 2 digits.
   */
  _rowNumber(index) {
    return String(index).padStart(2, "0");
  }

  /**
   * Scroll the module row at `index` into view and play a one-shot highlight
   * flash, so activating a `cts-plan-status` overview segment lands the user on
   * the matching row (R11). The row at `index` is the module at that plan-order
   * index (rows render in `modules` order). Honors `prefers-reduced-motion`:
   * the scroll uses `behavior:'auto'` and the CSS flash animation is gated off,
   * though the `is-flash` class is still applied (a no-op visual) and cleared on
   * a timer so it never lingers.
   * @param {number} index - The module's index in plan order.
   * @returns {Promise<void>} Resolves once the row has been located and flashed.
   */
  async highlightModule(index) {
    await this.updateComplete;
    const rows = this.querySelectorAll(".module-row");
    const row = rows[index];
    if (!row) return;
    const reduce =
      typeof window.matchMedia === "function" &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    row.scrollIntoView({ behavior: reduce ? "auto" : "smooth", block: "center" });
    clearTimeout(this._flashTimer);
    for (const r of rows) r.classList.remove("is-flash");
    // Force a reflow so re-adding the class restarts the animation mid-play.
    row.getBoundingClientRect();
    row.classList.add("is-flash");
    this._flashTimer = setTimeout(() => row.classList.remove("is-flash"), 1600);
  }

  _renderModuleRow(mod, index) {
    const lastInstance = this._getLastInstance(mod);
    const variant = statusBadgeVariant(mod.status, mod.result);
    const label = statusLabel(mod.status, mod.result);
    const variantStr = this._formatVariant(mod.variant);
    // R28 deep-link: when the row is FAILED and the page-level shim has
    // resolved the first failure's `LOG-NNNN` reference, append the
    // fragment so the click lands on the failure entry rather than the
    // top of the log. Result-gated so a fixture that carries the field
    // on a non-FAILED row produces no misleading fragment.
    const isFailedWithRef =
      mod.result === "FAILED" && typeof mod.firstFailureRef === "string" && mod.firstFailureRef;
    const logHref = lastInstance
      ? `log-detail.html?log=${encodeURIComponent(lastInstance)}` +
        `${this.isPublic ? "&public=true" : ""}` +
        `${isFailedWithRef ? `#${mod.firstFailureRef}` : ""}`
      : null;

    // The status badge is a plain read-only status pill (no `interactive`
    // ring). The whole row is now the click target and carries its own
    // affordance — the row-hover background plus the module name colouring
    // --orange-600 on block-link hover — so a ring on the badge would be
    // redundant noise (badge affordance rule, decision-tree step 3: the
    // wrapper provides a visible affordance, so the chip stays read-only).
    const badge = html`<cts-badge variant="${variant}" label="${label}"></cts-badge>`;
    // Two-state aria-label per R7: when the click lands mid-log on the
    // failure entry, announce "Jump to first failure"; otherwise keep
    // the R28-current "View logs (label)" form so the announcement
    // matches the actual destination.
    const linkAriaLabel = isFailedWithRef
      ? `Jump to first failure in logs for ${mod.testModule}`
      : `View logs for ${mod.testModule} (${label})`;
    const linkedBadge = lastInstance
      ? html`<a
          class="moduleStatusLink"
          data-testid="module-status-link"
          href="${logHref}"
          aria-label="${linkAriaLabel}"
          >${badge}</a
        >`
      : badge;

    // The module name links to the same log-detail URL as the status
    // badge and the "View Logs" button (logHref) when an instance
    // exists; otherwise it stays plain text. The link's accessible name
    // is the module name itself, so no aria-label is needed.
    const moduleName = lastInstance
      ? html`<a class="moduleNameLink" data-testid="module-name-link" href="${logHref}"
          >${mod.testModule}</a
        >`
      : html`<span class="moduleName">${mod.testModule}</span>`;

    return html`
      <div class="module-row" data-instance-id="${lastInstance || ""}">
        <span class="num">${this._rowNumber(index + 1)}</span>
        <div class="name">
          <span class="nameLine">
            ${moduleName}
            ${mod.testSummary
              ? html`<cts-tooltip class="help" content="${mod.testSummary}" placement="top"
                  ><cts-icon
                    name="circle-help"
                    size="16"
                    class="help-icon"
                    tabindex="0"
                    aria-label="Test summary"
                  ></cts-icon
                ></cts-tooltip>`
              : nothing}
          </span>
          <span class="statusLine">
            ${linkedBadge}${lastInstance
              ? html`<span class="instanceId">${lastInstance}</span>`
              : nothing}
          </span>
          ${variantStr
            ? html`<div class="desc"><span class="mono">${variantStr}</span></div>`
            : nothing}
        </div>
        <div class="actionStack">
          ${this._canRunTest()
            ? html`<cts-button
                class="startBtn"
                data-testid="run-test-btn"
                data-module-key="${_moduleKey(mod)}"
                variant="primary"
                size="sm"
                icon="play"
                label="Run Test"
                @cts-click=${this._handleRunTest}
              ></cts-button>`
            : nothing}
          ${lastInstance
            ? html`<cts-link-button
                  class="viewBtn"
                  href="${logHref}"
                  variant="secondary"
                  size="sm"
                  icon="file-blank"
                  label="View Logs"
                ></cts-link-button>
                <cts-button
                  class="downloadBtn"
                  data-instance-id="${lastInstance}"
                  variant="ghost"
                  size="sm"
                  icon="save"
                  label="Download Logs"
                  @cts-click=${this._handleDownloadLog}
                ></cts-button>`
            : nothing}
        </div>
      </div>
    `;
  }

  render() {
    if (!this.modules || this.modules.length === 0) {
      return html`<div class="planModulesEmpty">No modules in this plan</div>`;
    }

    return html`<div class="planModulesCard" id="planItems">${this._renderModuleRows()}</div>`;
  }

  _renderModuleRows() {
    return this.modules.map((mod, idx) => this._renderModuleRow(mod, idx));
  }
}

customElements.define("cts-plan-modules", CtsPlanModules);

export {};
