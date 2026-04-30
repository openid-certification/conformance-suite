import { LitElement, html, nothing } from "lit";
import "./cts-button.js";
import "./cts-link-button.js";
import "./cts-json-editor.js";
import { flashCopyConfirmed } from "../js/cts-copy-flash.js";

// Screen-reader announcement + visible feedback should stay long enough for
// assistive tech to finish reading the message.
const COPY_FEEDBACK_DURATION_MS = 5000;

const STYLE_ID = "cts-plan-actions-styles";

// Scoped CSS for the plan-detail action rail. Buttons stack vertically
// with a small gap; the inline panels (config, private link, delete
// confirm) reuse the OIDF card surface tokens. The destructive confirm
// panel switches to the rust palette for emphasis (matches the design
// archive's failure tone).
const STYLE_TEXT = `
  cts-plan-actions {
    display: block;
  }
  cts-plan-actions .planActionStack {
    display: grid;
    gap: var(--space-1);
  }
  cts-plan-actions .planActionPanel {
    margin-top: var(--space-3);
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    padding: var(--space-4);
  }
  cts-plan-actions .planActionPanel.is-danger {
    border-color: var(--status-fail-border);
    background: var(--status-fail-bg);
  }
  cts-plan-actions .planActionPanel h6 {
    margin: 0 0 var(--space-2);
    font-size: var(--fs-13);
    font-weight: var(--fw-bold);
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: var(--fg-soft);
  }
  cts-plan-actions .planConfigToolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);
    margin-bottom: var(--space-2);
  }
  cts-plan-actions .planConfigToolbar code {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    color: var(--fg-soft);
    background: var(--ink-50);
    padding: 1px 6px;
    border-radius: var(--radius-1);
  }
  cts-plan-actions .planConfigToolbarRight {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }
  cts-plan-actions .planConfigToolbar .copy-feedback {
    font-size: var(--fs-12);
    color: var(--rust-400);
  }
  /* '.planConfigJson' (and the '.config-json' sibling on the same element)
     are pre-existing class names from when this slot rendered a <pre>;
     the slot is now a <cts-json-editor>. Do not rename without updating
     all three call sites at once: this CSS rule, the '.config-json'
     selector that frontend/e2e/clipboard.spec.js + plan-detail.spec.js
     match on, and the data-clipboard-target=".config-json" attribute
     on the cts-button copy host. */
  cts-plan-actions .planConfigJson {
    display: block;
    margin: 0;
    max-height: 60vh;
    min-height: calc(var(--space-6) * 14);
  }
  cts-plan-actions .planLinkInput {
    display: block;
    width: 100%;
    box-sizing: border-box;
    height: 32px;
    padding: 0 var(--space-2);
    font-family: var(--font-sans);
    font-size: var(--fs-13);
    color: var(--fg);
    background: var(--bg-elev);
    border: 1px solid var(--ink-300);
    border-radius: var(--radius-2);
  }
  cts-plan-actions .planLinkInput:focus {
    outline: none;
    border-color: var(--orange-400);
    box-shadow: var(--focus-ring);
  }
  cts-plan-actions .planLinkLabel {
    display: block;
    font-size: var(--fs-12);
    color: var(--fg-soft);
    margin-bottom: var(--space-1);
  }
  cts-plan-actions .planLinkResult {
    margin-top: var(--space-2);
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    color: var(--ink-900);
    background: var(--ink-50);
    padding: var(--space-2);
    border-radius: var(--radius-2);
    word-break: break-all;
  }
  cts-plan-actions .planDeleteWarning {
    color: var(--ink-900);
    font-size: var(--fs-13);
    line-height: var(--lh-base);
  }
  cts-plan-actions .planDeleteWarning ul {
    margin: var(--space-2) 0;
    padding-left: var(--space-5);
  }
  cts-plan-actions .planDeleteActions {
    display: flex;
    gap: var(--space-2);
    margin-top: var(--space-3);
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
 * Action rail for a plan-detail page. Renders a stack of buttons whose
 * visibility depends on admin / readonly / publish / immutable state, and
 * inline panels for view-config, private-link generation, and delete
 * confirmation. Does not perform the actions itself — emits events for the
 * host page to handle.
 *
 * Light DOM. Scoped CSS is injected once on first connect; every button
 * is a `cts-button` (or `cts-link-button` for navigations), so the visual
 * layer follows the OIDF token system.
 *
 * @property {object} plan - Plan object; expects `_id`, `config`,
 *   `publish`, `immutable`.
 * @property {boolean} isAdmin - Reveals publish / download-all / make-mutable
 *   actions. Reflects the `is-admin` attribute.
 * @property {boolean} isReadonly - Public view — hides edit, publish,
 *   private-link, certify, and delete actions. Reflects the `is-readonly`
 *   attribute.
 * @property {boolean} canCertify - Gates the "Publish for certification"
 *   button visibility (R26). True when the plan has at least one FINISHED
 *   test and none of the finished tests have a FAILED result. Computed by
 *   the host page after polling per-module status. Reflects the
 *   `can-certify` attribute.
 * @fires cts-download-all - When the Download all Logs button is clicked,
 *   with `{ detail: { planId } }`; bubbles.
 * @fires cts-publish - When a Publish button is clicked, with
 *   `{ detail: { planId, mode } }` where `mode` is `summary` or
 *   `everything`; bubbles.
 * @fires cts-unpublish - When the Unpublish button is clicked, with
 *   `{ detail: { planId } }`; bubbles.
 * @fires cts-generate-private-link - When the Generate button in the
 *   private-link panel is clicked, with `{ detail: { planId, days } }`;
 *   bubbles.
 * @fires cts-certify - When the Publish for certification button is clicked,
 *   with `{ detail: { planId } }`; bubbles.
 * @fires cts-make-mutable - When the Make plan Mutable button is clicked,
 *   with `{ detail: { planId } }`; bubbles.
 * @fires cts-delete-plan - When the delete is confirmed, with
 *   `{ detail: { planId } }`; bubbles.
 */
class CtsPlanActions extends LitElement {
  static properties = {
    plan: { type: Object },
    isAdmin: { type: Boolean, attribute: "is-admin" },
    isReadonly: { type: Boolean, attribute: "is-readonly" },
    canCertify: { type: Boolean, attribute: "can-certify" },
    _showConfig: { state: true },
    _showDeleteConfirm: { state: true },
    _showPrivateLink: { state: true },
    _privateLinkDays: { state: true },
    _privateLinkResult: { state: true },
    _copyFeedback: { state: true },
  };

  constructor() {
    super();
    this.plan = {};
    this.isAdmin = false;
    this.isReadonly = false;
    this.canCertify = false;
    this._showConfig = false;
    this._showDeleteConfirm = false;
    this._showPrivateLink = false;
    this._privateLinkDays = 30;
    this._privateLinkResult = "";
    this._copyFeedback = "";
    this._copyFeedbackTimer = null;
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._copyFeedbackTimer) {
      clearTimeout(this._copyFeedbackTimer);
      this._copyFeedbackTimer = null;
    }
  }

  _handleViewConfig() {
    this._showConfig = !this._showConfig;
  }

  async _handleCopyConfig(event) {
    // Capture currentTarget synchronously: the await below clears it
    // because event dispatch has completed by the time we resume.
    const trigger = event && event.currentTarget;
    if (!this.plan || !this.plan.config) return;
    const text = JSON.stringify(this.plan.config, null, 4);
    if (!navigator.clipboard) {
      this._showCopyFeedback("Clipboard not available — please copy the JSON below manually.");
      return;
    }
    try {
      await navigator.clipboard.writeText(text);
    } catch (err) {
      console.warn("[cts-plan-actions] clipboard.writeText failed:", err);
      this._showCopyFeedback("Copy failed — please copy the JSON below manually.");
      return;
    }
    flashCopyConfirmed(trigger);
  }

  _showCopyFeedback(message) {
    this._copyFeedback = message;
    if (this._copyFeedbackTimer) clearTimeout(this._copyFeedbackTimer);
    this._copyFeedbackTimer = setTimeout(() => {
      this._copyFeedback = "";
      this._copyFeedbackTimer = null;
    }, COPY_FEEDBACK_DURATION_MS);
  }

  _handleDownloadAll() {
    this.dispatchEvent(
      new CustomEvent("cts-download-all", {
        bubbles: true,
        detail: { planId: this.plan._id },
      }),
    );
  }

  _handlePublish(mode) {
    this.dispatchEvent(
      new CustomEvent("cts-publish", {
        bubbles: true,
        detail: { planId: this.plan._id, mode },
      }),
    );
  }

  _handlePublishFromEvent(event) {
    const mode = event.currentTarget.dataset.publishMode;
    this._handlePublish(mode);
  }

  _handleUnpublish() {
    this.dispatchEvent(
      new CustomEvent("cts-unpublish", {
        bubbles: true,
        detail: { planId: this.plan._id },
      }),
    );
  }

  _handleTogglePrivateLink() {
    this._showPrivateLink = !this._showPrivateLink;
    this._privateLinkResult = "";
  }

  _handlePrivateLinkDaysInput(e) {
    this._privateLinkDays = Number(e.target.value);
  }

  _isPrivateLinkDaysValid() {
    return this._privateLinkDays >= 1 && this._privateLinkDays <= 1000;
  }

  _handleGeneratePrivateLink() {
    this.dispatchEvent(
      new CustomEvent("cts-generate-private-link", {
        bubbles: true,
        detail: {
          planId: this.plan._id,
          days: this._privateLinkDays,
        },
      }),
    );
  }

  _handleCertify() {
    this.dispatchEvent(
      new CustomEvent("cts-certify", {
        bubbles: true,
        detail: { planId: this.plan._id },
      }),
    );
  }

  _handleMakeMutable() {
    this.dispatchEvent(
      new CustomEvent("cts-make-mutable", {
        bubbles: true,
        detail: { planId: this.plan._id },
      }),
    );
  }

  _handleDeleteClick() {
    this._showDeleteConfirm = true;
  }

  _handleDeleteCancel() {
    this._showDeleteConfirm = false;
  }

  _handleDeleteConfirm() {
    this._showDeleteConfirm = false;
    this.dispatchEvent(
      new CustomEvent("cts-delete-plan", {
        bubbles: true,
        detail: { planId: this.plan._id },
      }),
    );
  }

  _renderConfigPanel() {
    if (!this._showConfig) return nothing;
    const configJson = this.plan.config
      ? JSON.stringify(this.plan.config, null, 4)
      : "No configuration available";

    return html`
      <div class="planActionPanel" data-testid="config-panel">
        <div class="planConfigToolbar">
          <strong>Configuration for <code>${this.plan._id}</code></strong>
          <div class="planConfigToolbarRight">
            ${this._copyFeedback
              ? html`<span
                  class="copy-feedback"
                  role="status"
                  aria-live="polite"
                  data-testid="copy-feedback"
                  >${this._copyFeedback}</span
                >`
              : nothing}
            <cts-button
              class="copy-config-btn"
              variant="secondary"
              size="sm"
              icon="copy"
              label="Copy"
              title="Copy config to clipboard"
              @cts-click=${this._handleCopyConfig}
            ></cts-button>
          </div>
        </div>
        <cts-json-editor
          class="planConfigJson config-json"
          readonly
          aria-label="Plan configuration JSON"
          .value=${configJson}
        ></cts-json-editor>
      </div>
    `;
  }

  _renderPrivateLinkPanel() {
    if (!this._showPrivateLink) return nothing;
    const isValid = this._isPrivateLinkDaysValid();

    return html`
      <div class="planActionPanel" data-testid="private-link-panel">
        <h6>Generate Private Link</h6>
        <label for="privateLinkDays" class="planLinkLabel">
          Number of days the link will be valid (1-1000):
        </label>
        <input
          type="number"
          id="privateLinkDays"
          class="planLinkInput"
          min="1"
          max="1000"
          .value=${String(this._privateLinkDays)}
          @input=${this._handlePrivateLinkDaysInput}
        />
        <div style="margin-top: var(--space-2);">
          <cts-button
            class="generate-link-btn"
            variant="primary"
            size="sm"
            label="Generate"
            ?disabled=${!isValid}
            @cts-click=${this._handleGeneratePrivateLink}
          ></cts-button>
        </div>
        ${this._privateLinkResult
          ? html`<div class="planLinkResult" data-testid="private-link-result">
              <code>${this._privateLinkResult}</code>
            </div>`
          : nothing}
      </div>
    `;
  }

  _renderDeleteConfirm() {
    if (!this._showDeleteConfirm) return nothing;

    return html`
      <div class="planActionPanel is-danger" data-testid="delete-confirm-panel">
        <div class="planDeleteWarning">
          <p
            ><strong>Clicking the "Delete plan" button will permanently and irrevocably:</strong></p
          >
          <ul>
            <li>Delete the test plan.</li>
            <li>Delete the test plan configuration.</li>
            <li>Delete the individual tests and logs belonging to the plan.</li>
          </ul>
          <p
            ><strong
              >This action cannot be undone and the data cannot be recovered after deletion.</strong
            ></p
          >
        </div>
        <div class="planDeleteActions">
          <cts-button
            variant="secondary"
            size="sm"
            label="Cancel"
            @cts-click=${this._handleDeleteCancel}
          ></cts-button>
          <cts-button
            class="confirm-delete-btn"
            variant="danger"
            size="sm"
            label="Delete plan"
            @cts-click=${this._handleDeleteConfirm}
          ></cts-button>
        </div>
      </div>
    `;
  }

  render() {
    const plan = this.plan;
    if (!plan || !plan._id) return nothing;

    const isPublished = Boolean(plan.publish);
    const isImmutable = Boolean(plan.immutable);

    return html`
      <div class="planActionStack" data-testid="plan-actions">
        <cts-button
          variant="secondary"
          size="sm"
          icon="settings"
          label="View configuration"
          full-width
          data-testid="view-config-btn"
          @cts-click=${this._handleViewConfig}
        ></cts-button>

        ${!this.isReadonly
          ? html`<cts-link-button
              href="schedule-test.html?edit-plan=${plan._id}"
              variant="secondary"
              size="sm"
              icon="edit-pencil-01"
              label="Edit configuration"
              full-width
              data-testid="edit-config-btn"
              title="Create a new test plan based on the configuration used in this one"
            ></cts-link-button>`
          : nothing}
        ${this.isAdmin
          ? html`<cts-button
              variant="secondary"
              size="sm"
              icon="save"
              label="Download all Logs"
              full-width
              data-testid="download-all-btn"
              @cts-click=${this._handleDownloadAll}
            ></cts-button>`
          : nothing}
        ${!this.isReadonly && !isPublished && this.isAdmin
          ? html`<cts-button
                variant="secondary"
                size="sm"
                icon="bookmark"
                label="Publish summary"
                full-width
                data-testid="publish-summary-btn"
                data-publish-mode="summary"
                @cts-click=${this._handlePublishFromEvent}
              ></cts-button>
              <cts-button
                variant="secondary"
                size="sm"
                icon="bookmark"
                label="Publish everything"
                full-width
                data-testid="publish-everything-btn"
                data-publish-mode="everything"
                @cts-click=${this._handlePublishFromEvent}
              ></cts-button>`
          : nothing}
        ${!this.isReadonly && isPublished && this.isAdmin
          ? html`<cts-button
                variant="secondary"
                size="sm"
                icon="close-circle"
                label="Unpublish"
                full-width
                data-testid="unpublish-btn"
                @cts-click=${this._handleUnpublish}
              ></cts-button>
              <cts-link-button
                href="plan-detail.html?plan=${plan._id}&amp;public=true"
                variant="primary"
                size="sm"
                icon="bookmark"
                label="Public link"
                full-width
              ></cts-link-button>`
          : nothing}
        ${!this.isReadonly
          ? html`<cts-button
              variant="secondary"
              size="sm"
              icon="bookmark"
              label="Private link"
              full-width
              data-testid="private-link-btn"
              @cts-click=${this._handleTogglePrivateLink}
            ></cts-button>`
          : nothing}
        ${!this.isReadonly && this.canCertify
          ? html`<cts-button
              variant="primary"
              size="sm"
              icon="save"
              label="Publish for certification"
              full-width
              title="Publish and prepare certification submission package"
              data-testid="certify-btn"
              @cts-click=${this._handleCertify}
            ></cts-button>`
          : nothing}
        ${isImmutable && this.isAdmin
          ? html`<cts-button
              variant="secondary"
              size="sm"
              icon="edit-pencil-01"
              label="Make plan Mutable"
              full-width
              data-testid="make-mutable-btn"
              @cts-click=${this._handleMakeMutable}
            ></cts-button>`
          : nothing}
        ${!isImmutable && !this.isReadonly
          ? html`<cts-button
              variant="ghost"
              size="sm"
              icon="trash-empty"
              label="Delete plan"
              full-width
              data-testid="delete-plan-btn"
              @cts-click=${this._handleDeleteClick}
            ></cts-button>`
          : nothing}
      </div>

      ${this._renderConfigPanel()} ${this._renderPrivateLinkPanel()} ${this._renderDeleteConfirm()}
    `;
  }
}

customElements.define("cts-plan-actions", CtsPlanActions);

export {};
