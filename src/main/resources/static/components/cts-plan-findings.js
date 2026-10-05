import { LitElement, html, nothing, css } from "lit";
import { createRef, ref } from "lit/directives/ref.js";
import { ifDefined } from "lit/directives/if-defined.js";
import "./cts-badge.js";
import "./cts-icon.js";
import "./cts-modal.js";
import "./cts-button.js";
import { formatVariant, moduleRowId } from "../js/module-status.js";
import { copyText } from "../js/copy-text.js";
import { flashCopyConfirmed } from "../js/cts-copy-flash.js";
import { saveBlob } from "../js/save-blob.js";

const STYLE_ID = "cts-plan-findings-styles";

// Lookup tables per components/AGENTS.md (no dynamic class concatenation).
/** @type {Object.<string, string>} */
const KIND_BADGE_VARIANTS = {
  FAILURE: "fail",
  WARNING: "warn",
  IMAGE: "review",
  RESPONSE: "review",
  INTERRUPTED: "fail",
  IN_PROGRESS: "running",
  NOT_RUN: "neutral",
};

/** @type {Object.<string, string>} */
const KIND_BADGE_LABELS = {
  IN_PROGRESS: "IN PROGRESS",
  NOT_RUN: "NOT RUN",
};

/**
 * @param {string} kind - A finding's or a module state's kind.
 * @returns {string} Its badge label.
 */
function kindLabel(kind) {
  return KIND_BADGE_LABELS[kind] || kind;
}

/** @type {Object.<string, [string, string]>} */
const KIND_HEADLINE_NOUNS = {
  FAILURE: ["failure", "failures"],
  WARNING: ["warning", "warnings"],
  IMAGE: ["image to review", "images to review"],
  RESPONSE: ["captured response to review", "captured responses to review"],
  INTERRUPTED: ["module interrupted", "modules interrupted"],
  IN_PROGRESS: ["module in progress", "modules in progress"],
  NOT_RUN: ["module not run", "modules not run"],
};

// The kinds this component derives from the plan's modules rather than
// receiving as findings: one row per state, listing the modules in it.
/** @type {Object.<string, string>} */
const MODULE_STATE_TEXT = {
  INTERRUPTED: "Stopped before running to completion",
  IN_PROGRESS: "Still running, or waiting to be completed",
  NOT_RUN: "Not run yet",
};

// A test that has finished is FINISHED; INTERRUPTED is one that was stopped
// (a failed test included); every other status is a test still under way.
const FINISHED_STATUS = "FINISHED";
const INTERRUPTED_STATUS = "INTERRUPTED";

const KIND_ORDER = [
  "FAILURE",
  "WARNING",
  "IMAGE",
  "RESPONSE",
  "INTERRUPTED",
  "IN_PROGRESS",
  "NOT_RUN",
];

// The source the image upload endpoint logs under, for an image the tester
// added that no condition asked for.
const FREE_UPLOAD_SRC = "_image-api";

const STYLE_TEXT = css`
  cts-plan-findings {
    display: block;
    /* The kind badge's column; one width keeps every row's text on one left
       edge whatever the badge says, and indents what sits under the text. */
    --badge-track: 6.75rem;
  }
  cts-plan-findings .planFindingsCard {
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    margin: var(--space-4) 0;
    font-size: var(--fs-13);
  }
  cts-plan-findings summary {
    cursor: pointer;
    border-radius: var(--radius-2);
  }
  cts-plan-findings summary:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-plan-findings .planFindingsTitle {
    padding: var(--space-3) var(--space-4);
    font-size: var(--fs-14);
    font-weight: var(--fw-bold);
    color: var(--fg);
  }
  cts-plan-findings .planFindingsActions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    padding: 0 var(--space-4) var(--space-3);
  }
  cts-plan-findings .planFindingsList {
    list-style: none;
    margin: 0;
    padding: 0;
  }
  cts-plan-findings .planFinding {
    padding: var(--space-3) var(--space-4);
    border-top: 1px solid var(--ink-100);
  }
  /* Two columns: the kind badge, then the finding. */
  cts-plan-findings .planFindingHead {
    display: grid;
    grid-template-columns: var(--badge-track) minmax(0, 1fr);
    column-gap: var(--space-3);
    align-items: start;
  }
  cts-plan-findings .planFindingBody {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    min-width: 0;
    overflow-wrap: anywhere;
  }
  cts-plan-findings .planFindingSrc {
    font-family: var(--font-mono);
    font-weight: var(--fw-bold);
    color: var(--fg);
  }
  cts-plan-findings .planFindingMsg {
    color: var(--fg-soft);
  }
  cts-plan-findings .planFindingMeta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    margin-top: var(--space-1);
  }
  cts-plan-findings .planFindingWhere {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    min-width: 0;
    color: var(--fg-muted);
  }
  /* The row summary is a grid, which drops the native disclosure marker, so
     the module count carries its own chevron: pointing down when collapsed
     and flipped when the module list is open. */
  cts-plan-findings .planFindingWhere cts-icon {
    transition: transform var(--dur-1) var(--ease-standard);
    transform-origin: center;
  }
  cts-plan-findings details[open] > .planFindingHead .planFindingWhere cts-icon {
    transform: rotate(180deg);
  }
  /* Indented to the finding's text column, past the badge track. */
  cts-plan-findings .planFindingModules {
    list-style: none;
    margin: var(--space-2) 0 0;
    padding: 0 0 0 calc(var(--badge-track) + var(--space-3));
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }
  /* On a phone the badge track would take a third of the row, so the badge
     sits above the text instead. */
  @media (max-width: 640px) {
    cts-plan-findings .planFindingHead {
      grid-template-columns: minmax(0, 1fr);
      row-gap: var(--space-2);
    }
    cts-plan-findings .planFindingHead > cts-badge {
      justify-self: start;
    }
    cts-plan-findings .planFindingModules {
      padding-left: var(--space-4);
    }
    cts-plan-findings .planFindingThumbs {
      margin-left: 0;
    }
  }
  /* Uploaded images of an IMAGE finding, one per module run, in the finding's
     text column. Each is a button that opens the image at full size. */
  cts-plan-findings .planFindingThumbs {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    margin: var(--space-2) 0 0 calc(var(--badge-track) + var(--space-3));
  }
  cts-plan-findings .planFindingThumb {
    padding: 0;
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    background: var(--bg);
    cursor: zoom-in;
    line-height: 0;
    overflow: hidden;
  }
  cts-plan-findings .planFindingThumb:hover {
    border-color: var(--orange-500);
  }
  cts-plan-findings .planFindingThumb:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  cts-plan-findings .planFindingThumb img {
    display: block;
    height: 120px;
    width: auto;
    max-width: 200px;
    object-fit: contain;
  }
  cts-plan-findings .planFindingResponseMeta {
    display: grid;
    grid-template-columns: max-content minmax(0, 1fr);
    gap: var(--space-1) var(--space-3);
    margin: 0 0 var(--space-3);
    font-size: var(--fs-13);
    overflow-wrap: anywhere;
  }
  cts-plan-findings .planFindingResponseMeta dt {
    color: var(--fg-muted);
  }
  cts-plan-findings .planFindingResponseMeta dd {
    margin: 0;
    font-family: var(--font-mono);
  }
  cts-plan-findings .planFindingResponseBody {
    max-height: 70vh;
    overflow: auto;
    margin: 0;
    padding: var(--space-3);
    background: var(--bg);
    border: 1px solid var(--border);
    border-radius: var(--radius-2);
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }
  cts-plan-findings .planFindingExpected {
    font-size: var(--fs-14);
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    margin: 0 0 var(--space-2);
    overflow-wrap: anywhere;
  }
  cts-plan-findings .planFindingViewedWhere {
    font-size: var(--fs-13);
    margin: 0 0 var(--space-3);
    color: var(--fg-muted);
    overflow-wrap: anywhere;
  }
  /* Large enough to read the text in a screenshot: as wide as the popup
     allows, and up to a screen and a half tall before it is scaled down. The
     popup scrolls. */
  cts-plan-findings .planFindingFullImage {
    display: block;
    max-width: 100%;
    max-height: 150vh;
    margin: 0 auto;
    border-radius: var(--radius-2);
  }
  cts-plan-findings .planFindingModules li {
    overflow-wrap: anywhere;
  }
  cts-plan-findings .planFindingVariant,
  cts-plan-findings .planFindingRepeat {
    color: var(--fg-muted);
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
 * @typedef {object} PlanFindingOccurrence
 * A module run a finding occurred in.
 * @property {number} moduleIndex - The module's position in the plan.
 * @property {string} testModule - The module's name.
 * @property {Object.<string, string>} [variant] - The variant it runs with.
 * @property {string} testId - The id of the module's latest run.
 * @property {Array<string>} [entryIds] - The ids of the run's matching log
 *   entries, in the order they were logged: the first is where a link lands,
 *   and each image or captured response can be viewed.
 * @property {string} [href] - Where the module links to, when not a log entry.
 */

/**
 * @typedef {object} PlanFinding
 * @property {string} kind - `FAILURE`, `WARNING`, `IMAGE` (an uploaded image)
 *   or `RESPONSE` (the response the browser automation captured in place of
 *   an image).
 * @property {string} [src] - The condition that logged the entry.
 * @property {string} [msg] - The logged message.
 * @property {Array<string>} [requirements] - Requirement IDs (rendered as chips).
 * @property {Array<PlanFindingOccurrence>} occurrences - The runs it occurred in.
 */

/**
 * Plan-wide summary of the failures, warnings and items awaiting review
 * (uploaded images, and responses the browser automation captured in their
 * place) across the latest run of every module, as returned by
 * `GET /api/plan/{id}/findings`: one row per distinct finding, listing the
 * modules it occurred in and linking to the entry in each module's log, then
 * one row per state for the modules that have not finished. Images and
 * captured responses open in a popup beside what they were expected to show.
 * The whole summary can be copied as text or downloaded as CSV. Renders
 * nothing when there is nothing to report.
 *
 * Light DOM. Scoped CSS is injected once on first render.
 *
 * @property {Array<PlanFinding>} findings - The findings, in display order.
 * @property {Array<object>} modules - The plan's modules, each with
 *   `testModule`, `variant`, `instances` and the `status` of its latest run.
 * @property {string} planId - The plan the findings belong to; without it no
 *   thumbnails are shown.
 */
class CtsPlanFindings extends LitElement {
  static properties = {
    findings: { type: Array },
    modules: { type: Array },
    planId: { type: String, attribute: "plan-id" },
    _viewed: { state: true },
    _viewedResponse: { state: true },
  };

  constructor() {
    super();
    /** @type {Array<PlanFinding>} */
    this.findings = [];
    /** @type {Array<any>} */
    this.modules = [];
    this.planId = "";
    /** @type {{finding: PlanFinding, occurrence: PlanFindingOccurrence, entryId: string} | null} */
    this._viewed = null;
    this._imageModalRef = createRef();
    /** @type {{finding: PlanFinding, occurrence: PlanFindingOccurrence, entryId: string, response: {contentType?: string, matchedRegexp?: string, body?: string} | null, error: string} | null} */
    this._viewedResponse = null;
    this._responseModalRef = createRef();
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  /**
   * @param {PlanFindingOccurrence} occurrence - A run a finding occurred in.
   * @returns {Array<string>} The ids of its matching log entries.
   */
  _entryIdsOf(occurrence) {
    return Array.isArray(occurrence.entryIds) ? occurrence.entryIds : [];
  }

  /**
   * Counts distinct findings for failures and warnings, every image or
   * captured response (each is one to review), and the modules in a state.
   * @param {Array<PlanFinding>} findings - The findings being rendered.
   * @returns {string} e.g. `3 failures, 1 warning, 2 images to review`.
   */
  _headline(findings) {
    return KIND_ORDER.map((kind) => {
      const ofKind = findings.filter((finding) => finding.kind === kind);
      let n = ofKind.length;
      if (kind in MODULE_STATE_TEXT) {
        n = ofKind.reduce((sum, finding) => sum + finding.occurrences.length, 0);
      } else if (kind === "IMAGE" || kind === "RESPONSE") {
        n = ofKind.reduce(
          (sum, finding) =>
            sum + finding.occurrences.reduce((s, o) => s + this._entryIdsOf(o).length, 0),
          0,
        );
      }
      if (n === 0) return "";
      const [one, many] = KIND_HEADLINE_NOUNS[kind];
      return `${n} ${n === 1 ? one : many}`;
    })
      .filter(Boolean)
      .join(", ");
  }

  /**
   * The modules that have not finished, as one finding-shaped row per state.
   * A module whose latest run carries no status (it is not visible to this
   * viewer) is in no state this can name, and is left out.
   * @returns {Array<PlanFinding>} Rows for INTERRUPTED, IN_PROGRESS and NOT_RUN.
   */
  _unfinishedRows() {
    /** @type {Object.<string, Array<PlanFindingOccurrence>>} */
    const byState = { INTERRUPTED: [], IN_PROGRESS: [], NOT_RUN: [] };
    (Array.isArray(this.modules) ? this.modules : []).forEach((mod, index) => {
      const instances = Array.isArray(mod.instances) ? mod.instances : [];
      let state;
      if (instances.length === 0) state = "NOT_RUN";
      else if (!mod.status || mod.status === FINISHED_STATUS) return;
      else state = mod.status === INTERRUPTED_STATUS ? "INTERRUPTED" : "IN_PROGRESS";
      byState[state].push({
        moduleIndex: index,
        testModule: mod.testModule,
        variant: mod.variant,
        testId: instances[instances.length - 1] || "",
        href: `#${moduleRowId(index)}`,
      });
    });
    return Object.entries(byState)
      .filter(([, occurrences]) => occurrences.length > 0)
      .map(([kind, occurrences]) => ({ kind, msg: MODULE_STATE_TEXT[kind], occurrences }));
  }

  /**
   * @param {PlanFinding} finding - The finding to describe.
   * @returns {{src: string, msg: string}} The row's source label and message.
   */
  _rowText(finding) {
    const msg = typeof finding.msg === "string" ? finding.msg.trim() : "";
    if (finding.src === FREE_UPLOAD_SRC) return { src: "", msg: msg || "Uploaded image" };
    if (!msg && finding.kind === "IMAGE") {
      return { src: finding.src || "", msg: "uploaded image awaiting review" };
    }
    if (!msg && finding.kind === "RESPONSE") {
      return {
        src: finding.src || "",
        msg: "response captured by browser automation, awaiting review",
      };
    }
    return { src: finding.src || "", msg };
  }

  /**
   * @param {PlanFindingOccurrence} occurrence - The run to link to.
   * @param {string} [entryId] - The entry to land on; the run's first by default.
   * @returns {string} The log page URL that lands on that entry.
   */
  _entryHref(occurrence, entryId = this._entryIdsOf(occurrence)[0]) {
    if (occurrence.href) return occurrence.href;
    return `log-detail.html?log=${encodeURIComponent(occurrence.testId)}#entry-${encodeURIComponent(entryId || "")}`;
  }

  /**
   * @param {PlanFindingOccurrence} occurrence - The run to render.
   * @param {boolean} showVariant - Whether the module name alone is ambiguous.
   * @returns {import("lit").TemplateResult} The link and its qualifiers.
   */
  _renderOccurrence(occurrence, showVariant) {
    const variant = showVariant ? formatVariant(occurrence.variant) : "";
    const count = this._entryIdsOf(occurrence).length;
    return html`<a
        class="planFindingLink"
        href=${this._entryHref(occurrence)}
        data-testid="plan-finding-link"
        >${occurrence.testModule}</a
      >${variant ? html` <span class="planFindingVariant">(${variant})</span>` : nothing}${count > 1
        ? html` <span class="planFindingRepeat">×${count}</span>`
        : nothing}`;
  }

  /**
   * @param {PlanFinding} finding - The finding to render.
   * @param {unknown} where - Where it occurred: a module link or a module count.
   * @returns {import("lit").TemplateResult} The badge and the finding's text column.
   */
  _renderHead(finding, where) {
    const { src, msg } = this._rowText(finding);
    return html`
      <cts-badge
        variant=${KIND_BADGE_VARIANTS[finding.kind] || "neutral"}
        label=${kindLabel(finding.kind)}
      ></cts-badge>
      <span class="planFindingBody">
        ${src ? html`<span class="planFindingSrc">${src}</span>` : nothing}
        ${msg ? html`<span class="planFindingMsg">${msg}</span>` : nothing}
        <span class="planFindingMeta">
          <span class="planFindingWhere">${where}</span>
          ${(finding.requirements || []).map(
            (req) => html`<cts-badge variant="secondary" label=${req}></cts-badge>`,
          )}
        </span>
      </span>
    `;
  }

  /**
   * @param {string} entryId - An entry of one of the plan's latest runs.
   * @param {"image" | "response"} what - What the entry carries.
   * @returns {string} The URL that serves it.
   */
  _findingUrl(entryId, what) {
    return `/api/plan/${encodeURIComponent(this.planId)}/findings/${encodeURIComponent(entryId)}/${what}`;
  }

  /**
   * Finds the entry a thumbnail or a response button stands for, from the id
   * it carries. (Templates pass handlers by method, not by closure.)
   * @param {string} kind - `IMAGE` or `RESPONSE`.
   * @param {Event} event - The click on the thumbnail or button.
   * @returns {{finding: PlanFinding, occurrence: PlanFindingOccurrence, entryId: string} | null}
   *   The entry, or null if nothing shown carries that id.
   */
  _clickedEntry(kind, event) {
    const entryId = /** @type {HTMLElement} */ (event.currentTarget).dataset.entryId || "";
    for (const finding of this.findings || []) {
      if (finding.kind !== kind) continue;
      const occurrence = (finding.occurrences || []).find((o) =>
        this._entryIdsOf(o).includes(entryId),
      );
      if (occurrence) return { finding, occurrence, entryId };
    }
    return null;
  }

  /**
   * Opens the popup on the uploaded image whose thumbnail was activated.
   * @param {Event} event - The click on a thumbnail button.
   * @returns {void}
   */
  _onThumbClick(event) {
    const viewed = this._clickedEntry("IMAGE", event);
    if (!viewed) return;
    this._viewed = viewed;
    /** @type {any} */ (this._imageModalRef.value)?.show();
  }

  /**
   * @param {PlanFinding} finding - The finding to render thumbnails for.
   * @returns {import("lit").TemplateResult | typeof nothing} One thumbnail per
   *   uploaded image, or nothing for other kinds.
   */
  _renderThumbnails(finding) {
    if (finding.kind !== "IMAGE" || !this.planId) return nothing;
    return html`
      <div class="planFindingThumbs">
        ${(finding.occurrences || []).flatMap((occurrence) =>
          this._entryIdsOf(occurrence).map(
            (entryId) =>
              html`<button
                type="button"
                class="planFindingThumb"
                data-testid="plan-finding-thumb"
                title=${occurrence.testModule}
                aria-label="View the image uploaded in ${occurrence.testModule}"
                data-entry-id=${entryId}
                @click=${this._onThumbClick}
              >
                <img loading="lazy" src=${this._findingUrl(entryId, "image")} alt="" />
              </button>`,
          ),
        )}
      </div>
    `;
  }

  /**
   * Opens the popup on the captured response whose button was activated,
   * fetching it while the popup shows it is loading.
   * @param {Event} event - The click on a response button.
   * @returns {Promise<void>}
   */
  async _onResponseClick(event) {
    const viewed = this._clickedEntry("RESPONSE", event);
    if (!viewed) return;
    this._viewedResponse = { ...viewed, response: null, error: "" };
    /** @type {any} */ (this._responseModalRef.value)?.show();
    let loaded;
    try {
      const reply = await fetch(this._findingUrl(viewed.entryId, "response"));
      if (!reply.ok) throw new Error(`HTTP ${reply.status}`);
      loaded = { response: await reply.json(), error: "" };
    } catch (err) {
      loaded = { response: null, error: String(err) };
    }
    // Unless another response was opened meanwhile.
    if (this._viewedResponse?.entryId === viewed.entryId) {
      this._viewedResponse = { ...viewed, ...loaded };
    }
  }

  /**
   * @param {PlanFinding} finding - The finding to render buttons for.
   * @returns {import("lit").TemplateResult | typeof nothing} One button per
   *   captured response, or nothing for other kinds.
   */
  _renderResponseButtons(finding) {
    if (finding.kind !== "RESPONSE" || !this.planId) return nothing;
    return html`
      <div class="planFindingThumbs">
        ${(finding.occurrences || []).flatMap((occurrence) => {
          const entryIds = this._entryIdsOf(occurrence);
          return entryIds.map(
            (entryId, i) =>
              html`<cts-button
                size="xs"
                icon="file-document"
                label=${`View response${entryIds.length > 1 ? ` ${i + 1}` : ""}: ${occurrence.testModule}`}
                data-entry-id=${entryId}
                data-testid="plan-finding-response"
                @cts-click=${this._onResponseClick}
              ></cts-button>`,
          );
        })}
      </div>
    `;
  }

  /**
   * @param {PlanFinding} finding - The finding to render.
   * @returns {import("lit").TemplateResult} Its row: the badge and text, with
   *   the module named inline when there is one and under a disclosure when
   *   there are several, then any thumbnails or response buttons.
   */
  _renderFinding(finding) {
    const occurrences = Array.isArray(finding.occurrences) ? finding.occurrences : [];
    let head;
    if (occurrences.length === 1) {
      head = html`<div class="planFindingHead">
        ${this._renderHead(
          finding,
          html`<span>in ${this._renderOccurrence(occurrences[0], false)}</span>`,
        )}
      </div>`;
    } else {
      const names = occurrences.map((occurrence) => occurrence.testModule);
      const ambiguous = new Set(names.filter((name, i) => names.indexOf(name) !== i));
      head = html`<details>
        <summary class="planFindingHead">
          ${this._renderHead(
            finding,
            html`in ${occurrences.length} modules
              <cts-icon name="chevron-down" size="16"></cts-icon>`,
          )}
        </summary>
        <ul class="planFindingModules">
          ${occurrences.map(
            (occurrence) =>
              html`<li>
                ${this._renderOccurrence(occurrence, ambiguous.has(occurrence.testModule))}
              </li>`,
          )}
        </ul>
      </details>`;
    }
    return html`
      <li class="planFinding" data-testid="plan-finding" data-kind=${finding.kind}>
        ${head} ${this._renderThumbnails(finding)} ${this._renderResponseButtons(finding)}
      </li>
    `;
  }

  /**
   * @param {string} href - A link as rendered on the page.
   * @returns {string} The same link, absolute, so it works once pasted elsewhere.
   */
  _absolute(href) {
    if (typeof window === "undefined") return href;
    return new URL(href, window.location.href).toString();
  }

  /**
   * @param {Array<PlanFinding>} rows - What the summary shows, in order.
   * @returns {string} The summary as plain text, one block per row.
   */
  _asText(rows) {
    const lines = [`Summary of results: ${this._headline(rows)}`];
    if (typeof window !== "undefined") lines.push(window.location.href.split("#")[0]);
    for (const row of rows) {
      const { src, msg } = this._rowText(row);
      lines.push("", `${kindLabel(row.kind)}: ${src || msg}`);
      if (src && msg) lines.push(`  ${msg}`);
      if (row.requirements && row.requirements.length) {
        lines.push(`  Requirements: ${row.requirements.join(", ")}`);
      }
      const occurrences = row.occurrences || [];
      lines.push(`  In ${occurrences.length} ${occurrences.length === 1 ? "module" : "modules"}:`);
      for (const occurrence of occurrences) {
        const variant = formatVariant(occurrence.variant);
        const count = this._entryIdsOf(occurrence).length;
        const repeat = count > 1 ? ` (x${count})` : "";
        lines.push(
          `  - ${occurrence.testModule}${variant ? ` (${variant})` : ""}${repeat}: ${this._absolute(this._entryHref(occurrence))}`,
        );
      }
    }
    return lines.join("\n") + "\n";
  }

  /**
   * @param {Array<PlanFinding>} rows - What the summary shows, in order.
   * @returns {string} The summary as CSV, one line per module occurrence.
   */
  _asCsv(rows) {
    const cell = (value) => {
      let text = value == null ? "" : String(value);
      // A message can echo text the server under test sent, and a
      // spreadsheet runs a cell starting with one of these as a formula.
      if (/^[=+\-@\t\r]/.test(text)) text = `'${text}`;
      return /[",\r\n']/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
    };
    const lines = [
      [
        "kind",
        "condition",
        "message",
        "requirements",
        "module",
        "variant",
        "test_id",
        "count",
        "link",
      ]
        .map(cell)
        .join(","),
    ];
    for (const row of rows) {
      const { src, msg } = this._rowText(row);
      for (const occurrence of row.occurrences || []) {
        lines.push(
          [
            kindLabel(row.kind),
            src,
            msg,
            (row.requirements || []).join(" "),
            occurrence.testModule,
            formatVariant(occurrence.variant),
            occurrence.testId,
            occurrence.entryIds ? occurrence.entryIds.length : "",
            this._absolute(this._entryHref(occurrence)),
          ]
            .map(cell)
            .join(","),
        );
      }
    }
    return lines.join("\r\n") + "\r\n";
  }

  /**
   * @returns {Array<PlanFinding>} Everything the summary shows, findings then
   *   unfinished modules.
   */
  _rows() {
    return [...(Array.isArray(this.findings) ? this.findings : []), ...this._unfinishedRows()];
  }

  /**
   * Copies the summary as text and flashes the button's icon on success.
   * @param {Event} event - The click on the copy button.
   * @returns {Promise<void>}
   */
  async _onCopyClick(event) {
    const button = /** @type {HTMLElement} */ (event.currentTarget);
    if (await copyText(this._asText(this._rows()))) flashCopyConfirmed(button);
  }

  /**
   * Downloads the summary as `summary-of-results-<planId>.csv`.
   * @returns {void}
   */
  _onDownloadCsvClick() {
    saveBlob(
      new Blob([this._asCsv(this._rows())], { type: "text/csv;charset=utf-8" }),
      `summary-of-results-${this.planId || "plan"}.csv`,
    );
  }

  /**
   * What a popup is about: the condition and message the viewed entry is
   * expected to satisfy, and the module run it belongs to.
   * @param {{finding: PlanFinding, occurrence: PlanFindingOccurrence, entryId: string} | null} viewed - The
   *   entry shown, if any; the structure renders regardless, because `cts-modal`
   *   moves its children once on connect.
   * @param {string} how - How the entry got there: "Uploaded in" or "Captured in".
   * @param {string} testIdPrefix - Prefix of the `data-testid`s of the two parts.
   * @returns {import("lit").TemplateResult} The two paragraphs.
   */
  _renderViewedHeader(viewed, how, testIdPrefix) {
    const text = viewed ? this._rowText(viewed.finding) : { src: "", msg: "" };
    return html`
      <p class="planFindingExpected" data-testid="${testIdPrefix}-expected">
        <span class="planFindingSrc">${text.src}</span>
        <span class="planFindingMsg">${text.msg}</span>
      </p>
      <p class="planFindingViewedWhere">
        ${how}
        <a
          data-testid="${testIdPrefix}-link"
          href=${ifDefined(viewed ? this._entryHref(viewed.occurrence, viewed.entryId) : undefined)}
          >${viewed ? viewed.occurrence.testModule : ""}</a
        >
      </p>
    `;
  }

  /**
   * The single shared popup for every thumbnail: what the image is expected
   * to show, the module run it was uploaded in, and the image at full size.
   * @returns {import("lit").TemplateResult} The modal.
   */
  _renderImageModal() {
    const viewed = this._viewed;
    return html`
      <cts-modal
        ${ref(this._imageModalRef)}
        heading="Image to review"
        size="xl"
        data-testid="plan-finding-image-modal"
      >
        ${this._renderViewedHeader(viewed, "Uploaded in", "plan-finding-image")}
        <img
          class="planFindingFullImage"
          src=${ifDefined(viewed ? this._findingUrl(viewed.entryId, "image") : undefined)}
          alt=${viewed ? `Image uploaded in ${viewed.occurrence.testModule}` : ""}
        />
      </cts-modal>
    `;
  }

  /**
   * @param {{contentType?: string, body?: string} | null} response - The captured response.
   * @returns {string} Its body, pretty-printed when it is JSON.
   */
  _responseBodyText(response) {
    const body = response && typeof response.body === "string" ? response.body : "";
    if (/json/i.test(response?.contentType || "")) {
      try {
        return JSON.stringify(JSON.parse(body), null, 2);
      } catch {
        return body;
      }
    }
    return body;
  }

  /**
   * The single shared popup for every captured response: what it is expected
   * to show, the module run it was captured in, its content type, the pattern
   * it matched and its body.
   * @returns {import("lit").TemplateResult} The modal.
   */
  _renderResponseModal() {
    const viewed = this._viewedResponse;
    const response = viewed ? viewed.response : null;
    let body = "Loading…";
    if (viewed && viewed.error) body = `Could not load the response: ${viewed.error}`;
    else if (response) body = this._responseBodyText(response);
    return html`
      <cts-modal
        ${ref(this._responseModalRef)}
        heading="Captured response to review"
        size="xl"
        data-testid="plan-finding-response-modal"
      >
        ${this._renderViewedHeader(viewed, "Captured in", "plan-finding-response")}
        <dl class="planFindingResponseMeta">
          <dt>Content type</dt>
          <dd data-testid="plan-finding-response-type">${response?.contentType || ""}</dd>
          <dt>Matched</dt>
          <dd data-testid="plan-finding-response-regexp">${response?.matchedRegexp || ""}</dd>
        </dl>
        <pre class="planFindingResponseBody" data-testid="plan-finding-response-body">${body}</pre>
      </cts-modal>
    `;
  }

  render() {
    const findings = this._rows();
    if (findings.length === 0) return nothing;

    return html`
      <details class="planFindingsCard" data-testid="plan-findings" open>
        <summary class="planFindingsTitle">Summary of results: ${this._headline(findings)}</summary>
        <div class="planFindingsActions">
          <cts-button
            size="xs"
            icon="copy"
            label="Copy as text"
            data-testid="plan-findings-copy-text"
            @cts-click=${this._onCopyClick}
          ></cts-button>
          <cts-button
            size="xs"
            icon="download"
            label="Download CSV"
            data-testid="plan-findings-download-csv"
            @cts-click=${this._onDownloadCsvClick}
          ></cts-button>
        </div>
        <ul class="planFindingsList">
          ${findings.map((finding) => this._renderFinding(finding))}
        </ul>
      </details>
      ${this._renderImageModal()} ${this._renderResponseModal()}
    `;
  }
}

customElements.define("cts-plan-findings", CtsPlanFindings);

export {};
