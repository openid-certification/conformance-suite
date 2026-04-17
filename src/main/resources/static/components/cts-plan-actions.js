import { LitElement, html, nothing } from "lit";

// Screen-reader announcement + visible feedback should stay long enough for
// assistive tech to finish reading the message.
const COPY_FEEDBACK_DURATION_MS = 5000;

/**
 * Action rail for a plan-detail page. Renders a stack of buttons whose
 * visibility depends on admin / readonly / publish / immutable state, and
 * inline panels for view-config, private-link generation, and delete
 * confirmation. Does not perform the actions itself — emits events for the
 * host page to handle.
 *
 * @property {Object} plan - Plan object; expects `_id`, `config`,
 *   `publish`, `immutable`.
 * @property {boolean} isAdmin - Reveals publish / download-all / make-mutable
 *   actions. Reflects the `is-admin` attribute.
 * @property {boolean} isReadonly - Public view — hides edit, publish,
 *   private-link, certify, and delete actions. Reflects the `is-readonly`
 *   attribute.
 *
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
    this._showConfig = false;
    this._showDeleteConfirm = false;
    this._showPrivateLink = false;
    this._privateLinkDays = 30;
    this._privateLinkResult = "";
    this._copyFeedback = "";
    this._copyFeedbackTimer = null;
  }

  createRenderRoot() { return this; }

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

  async _handleCopyConfig() {
    if (!this.plan || !this.plan.config) return;
    const text = JSON.stringify(this.plan.config, null, 4);
    if (!navigator.clipboard) {
      this._showCopyFeedback(
        "Clipboard not available — please copy the JSON below manually.",
      );
      return;
    }
    try {
      await navigator.clipboard.writeText(text);
    } catch (err) {
      console.warn("[cts-plan-actions] clipboard.writeText failed:", err);
      this._showCopyFeedback(
        "Copy failed — please copy the JSON below manually.",
      );
    }
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
      <div class="card mt-2" data-testid="config-panel">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-center mb-2">
            <strong>Configuration for <code class="text-muted">${this.plan._id}</code></strong>
            <div class="d-flex align-items-center gap-2">
              ${this._copyFeedback
                ? html`<span
                    class="text-danger small"
                    role="status"
                    aria-live="polite"
                    data-testid="copy-feedback"
                    >${this._copyFeedback}</span
                  >`
                : nothing}
              <button
                class="btn btn-sm btn-outline-secondary copy-config-btn"
                title="Copy config to clipboard"
                @click=${() => this._handleCopyConfig()}
              >
                <span class="bi bi-clipboard" aria-hidden="true"></span> Copy
              </button>
            </div>
          </div>
          <div class="wrapLongStrings">
            <pre class="row-bg-light p-1 config-json">${configJson}</pre>
          </div>
        </div>
      </div>
    `;
  }

  _renderPrivateLinkPanel() {
    if (!this._showPrivateLink) return nothing;
    const isValid = this._isPrivateLinkDaysValid();

    return html`
      <div class="card mt-2" data-testid="private-link-panel">
        <div class="card-body">
          <h6>Generate Private Link</h6>
          <div class="mb-2">
            <label for="privateLinkDays" class="form-label">
              Number of days the link will be valid (1-1000):
            </label>
            <input
              type="number"
              id="privateLinkDays"
              class="form-control form-control-sm"
              min="1"
              max="1000"
              .value=${String(this._privateLinkDays)}
              @input=${this._handlePrivateLinkDaysInput}
            />
          </div>
          <button
            class="btn btn-sm btn-primary generate-link-btn"
            ?disabled=${!isValid}
            @click=${() => this._handleGeneratePrivateLink()}
          >Generate</button>
          ${this._privateLinkResult
            ? html`
              <div class="mt-2" data-testid="private-link-result">
                <code>${this._privateLinkResult}</code>
              </div>`
            : nothing}
        </div>
      </div>
    `;
  }

  _renderDeleteConfirm() {
    if (!this._showDeleteConfirm) return nothing;

    return html`
      <div class="card mt-2 border-danger" data-testid="delete-confirm-panel">
        <div class="card-body">
          <p><strong>Clicking the "Delete plan" button will permanently and irrevocably:</strong></p>
          <ul>
            <li>Delete the test plan.</li>
            <li>Delete the test plan configuration.</li>
            <li>Delete the individual tests and logs belonging to the plan.</li>
          </ul>
          <p><strong>This action cannot be undone and the data cannot be recovered after deletion.</strong></p>
          <div class="d-flex gap-2">
            <button
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              @click=${() => this._handleDeleteCancel()}
            >Cancel</button>
            <button
              class="btn btn-sm btn-danger bg-gradient border border-secondary confirm-delete-btn"
              @click=${() => this._handleDeleteConfirm()}
            >Delete plan</button>
          </div>
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
      <div class="d-grid gap-1" data-testid="plan-actions">
        <button
          class="btn btn-sm btn-light bg-gradient border border-secondary"
          data-testid="view-config-btn"
          @click=${() => this._handleViewConfig()}
        ><span class="bi bi-wrench-adjustable"></span> View Config</button>

        ${!this.isReadonly
          ? html`
            <a
              href="schedule-test.html?edit-plan=${plan._id}"
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              data-testid="edit-config-btn"
              title="Create a new test plan based on the configuration used in this one"
            ><span class="bi bi-pencil-square"></span> Edit configuration</a>`
          : nothing}

        ${this.isAdmin
          ? html`
            <button
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              data-testid="download-all-btn"
              @click=${() => this._handleDownloadAll()}
            ><span class="bi bi-save2"></span> Download all Logs</button>`
          : nothing}

        ${!this.isReadonly && !isPublished && this.isAdmin
          ? html`
            <button
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              data-testid="publish-summary-btn"
              @click=${() => this._handlePublish("summary")}
            ><span class="bi bi-bookmarks"></span> Publish summary</button>
            <button
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              data-testid="publish-everything-btn"
              @click=${() => this._handlePublish("everything")}
            ><span class="bi bi-bookmarks"></span> Publish everything</button>`
          : nothing}

        ${!this.isReadonly && isPublished && this.isAdmin
          ? html`
            <button
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              data-testid="unpublish-btn"
              @click=${() => this._handleUnpublish()}
            ><span class="bi bi-slash-circle"></span> Unpublish</button>
            <a
              href="plan-detail.html?plan=${plan._id}&public=true"
              class="btn btn-sm btn-info bg-gradient border border-secondary"
            ><span class="bi bi-bookmarks"></span> Public link</a>`
          : nothing}

        ${!this.isReadonly
          ? html`
            <button
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              data-testid="private-link-btn"
              @click=${() => this._handleTogglePrivateLink()}
            ><span class="bi bi-bookmarks"></span> Private link</button>
            <button
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              data-testid="certify-btn"
              disabled
              title="Publish and prepare certification submission package"
              @click=${() => this._handleCertify()}
            ><span class="bi bi-save2"></span> Publish for certification</button>`
          : nothing}

        ${isImmutable && this.isAdmin
          ? html`
            <button
              class="btn btn-sm btn-light bg-gradient border border-secondary"
              data-testid="make-mutable-btn"
              @click=${() => this._handleMakeMutable()}
            ><span class="bi bi-pencil-square"></span> Make plan Mutable</button>`
          : nothing}

        ${!isImmutable && !this.isReadonly
          ? html`
            <button
              class="btn btn-sm btn-danger bg-gradient border border-secondary"
              data-testid="delete-plan-btn"
              @click=${() => this._handleDeleteClick()}
            ><span class="bi bi-trash"></span> Delete plan</button>`
          : nothing}
      </div>

      ${this._renderConfigPanel()}
      ${this._renderPrivateLinkPanel()}
      ${this._renderDeleteConfirm()}
    `;
  }
}

customElements.define("cts-plan-actions", CtsPlanActions);
