import { LitElement, html, nothing } from "lit";

// Screen-reader announcement + visible feedback should stay long enough for
// assistive tech to finish reading the message. 2-3s is a common mistake.
const COPY_FEEDBACK_DURATION_MS = 5000;

/**
 * Token management UI. Lists existing API tokens via `/api/token`, lets the
 * user create temporary or permanent tokens, and confirms deletion. Admins
 * see a read-only message instead (admins cannot create tokens).
 * @property {boolean} isAdmin - Renders the admin read-only view instead of
 *   the create/delete UI. Reflects the `is-admin` attribute.
 */
class CtsTokenManager extends LitElement {
  static properties = {
    isAdmin: { type: Boolean, attribute: "is-admin" },
    _tokens: { state: true },
    _loading: { state: true },
    _createdToken: { state: true },
    _deleteTokenId: { state: true },
    _error: { state: true },
    _showCreated: { state: true },
    _showDelete: { state: true },
    _showError: { state: true },
    _copyFeedback: { state: true },
  };

  constructor() {
    super();
    this.isAdmin = false;
    this._tokens = [];
    this._loading = true;
    this._createdToken = "";
    this._deleteTokenId = "";
    this._error = "";
    this._showCreated = false;
    this._showDelete = false;
    this._showError = false;
    this._copyFeedback = "";
    this._copyFeedbackTimer = null;
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._copyFeedbackTimer) {
      clearTimeout(this._copyFeedbackTimer);
      this._copyFeedbackTimer = null;
    }
  }

  // Use light DOM so Bootstrap CSS applies
  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    this._fetchTokens();
  }

  async _fetchTokens() {
    this._loading = true;
    try {
      const response = await fetch("/api/token");
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      this._tokens = await response.json();
    } catch (err) {
      this._error = (err instanceof Error && err.message) || "Failed to load tokens";
      this._tokens = [];
    } finally {
      this._loading = false;
    }
  }

  _createTemporaryToken() {
    return this._createToken(false);
  }

  _createPermanentToken() {
    return this._createToken(true);
  }

  async _createToken(permanent) {
    this._error = "";
    try {
      const response = await fetch("/api/token", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ permanent }),
      });
      if (!response.ok) {
        let message = response.statusText;
        try {
          const body = await response.json();
          if (body.error) {
            message = body.error;
          }
        } catch {
          // Use statusText as fallback
        }
        this._error = message;
        this._showError = true;
        return;
      }
      const data = await response.json();
      this._createdToken = data.token;
      this._showCreated = true;
      await this._fetchTokens();
    } catch (err) {
      this._error = (err instanceof Error && err.message) || "Failed to create token";
      this._showError = true;
    }
  }

  async _deleteToken(tokenId) {
    this._error = "";
    try {
      const response = await fetch("/api/token/" + encodeURIComponent(tokenId), {
        method: "DELETE",
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      await this._fetchTokens();
    } catch (err) {
      this._error = (err instanceof Error && err.message) || "Failed to delete token";
      this._showError = true;
    }
  }

  _openDeleteModal(tokenId) {
    this._deleteTokenId = tokenId;
    this._showDelete = true;
  }

  _handleDeleteClick(event) {
    const tokenId = event.currentTarget.dataset.tokenId;
    if (tokenId) this._openDeleteModal(tokenId);
  }

  _confirmDelete() {
    if (this._deleteTokenId) {
      this._deleteToken(this._deleteTokenId);
      this._deleteTokenId = "";
    }
    this._showDelete = false;
  }

  _cancelDelete() {
    this._deleteTokenId = "";
    this._showDelete = false;
  }

  _closeCreatedModal() {
    this._showCreated = false;
  }

  _closeErrorModal() {
    this._showError = false;
  }

  async _copyToken() {
    if (!this._createdToken) return;
    if (!navigator.clipboard) {
      this._showCopyFeedback("Clipboard not available — please select the token text manually.");
      return;
    }
    try {
      await navigator.clipboard.writeText(this._createdToken);
    } catch (err) {
      console.warn("[cts-token-manager] clipboard.writeText failed:", err);
      this._showCopyFeedback("Copy failed — please select the token text manually.");
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

  _formatDate(timestamp) {
    if (!timestamp) {
      return "Never";
    }
    const date = new Date(timestamp);
    return date.toLocaleDateString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  _renderAdminView() {
    return html`
      <div class="container-fluid">
        <div class="row">
          <div class="col-md-12">
            <p class="admin-message">
              Admin users cannot create tokens - please login as a non-admin user.
            </p>
          </div>
        </div>
      </div>
    `;
  }

  _renderLoading() {
    return html`
      <div class="container-fluid">
        <div class="row">
          <div class="col-md-12">
            <p>Loading...</p>
          </div>
        </div>
      </div>
    `;
  }

  _renderTokenTable() {
    if (this._tokens.length === 0) {
      return html`<p class="no-tokens-message"> No tokens have been created yet. </p>`;
    }

    return html`
      <table class="table table-striped table-bordered table-hover" id="tokensListing">
        <thead>
          <tr>
            <th>Token ID</th>
            <th>Expires</th>
            <th>Delete</th>
          </tr>
        </thead>
        <tbody>${this._renderTokenRows()}</tbody>
      </table>
    `;
  }

  _renderCreateButtons() {
    return html`
      <p>
        <button
          type="button"
          class="btn btn-lg btn-primary bg-gradient border border-secondary"
          @click=${this._createTemporaryToken}
        >
          New temporary token
        </button>
        <button
          type="button"
          class="btn btn-lg btn-primary bg-gradient border border-secondary"
          @click=${this._createPermanentToken}
        >
          New permanent token
        </button>
        <a
          class="btn btn-lg btn-primary bg-gradient border border-secondary"
          href="/api-document.html"
          >API Documentation</a
        >
      </p>
    `;
  }

  _renderCreatedModal() {
    if (!this._showCreated) return nothing;

    return html`
      <div class="modal-backdrop-lite" role="dialog" aria-label="Token created" aria-modal="true">
        <div class="modal d-block" tabindex="-1">
          <div class="modal-dialog modal-lg">
            <div class="modal-content">
              <div class="modal-header">
                <h4 class="modal-title">Token created</h4>
                <button
                  type="button"
                  class="btn-close"
                  aria-label="Close"
                  @click=${this._closeCreatedModal}
                ></button>
              </div>
              <div class="modal-body">
                <div class="wrapLongStrings">
                  <p class="d-flex align-items-center gap-2">
                    <button
                      class="btn btn-sm btn-outline-secondary"
                      @click=${this._copyToken}
                      title="Copy token to clipboard"
                    >
                      <span class="bi bi-clipboard"></span> Copy
                    </button>
                    ${this._copyFeedback
                      ? html`<span
                          class="text-danger small"
                          role="status"
                          aria-live="polite"
                          data-testid="copy-feedback"
                          >${this._copyFeedback}</span
                        >`
                      : nothing}
                  </p>
                  <p> Here is your new token. This value will only be displayed once. </p>
                  <pre class="created-token-value">${this._createdToken}</pre>
                </div>
              </div>
              <div class="modal-footer">
                <button
                  type="button"
                  class="btn btn-sm btn-light bg-gradient border border-secondary"
                  @click=${this._closeCreatedModal}
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  _renderDeleteModal() {
    if (!this._showDelete) return nothing;

    return html`
      <div class="modal-backdrop-lite" role="dialog" aria-label="Delete token" aria-modal="true">
        <div class="modal d-block" tabindex="-1">
          <div class="modal-dialog">
            <div class="modal-content">
              <div class="modal-header">
                <h4 class="modal-title">Delete</h4>
                <button
                  type="button"
                  class="btn-close"
                  aria-label="Close"
                  @click=${this._cancelDelete}
                ></button>
              </div>
              <div class="modal-body">
                <p> Are you sure? This will permanently remove this token. </p>
              </div>
              <div class="modal-footer">
                <button
                  type="button"
                  class="btn btn-sm btn-danger bg-gradient border border-secondary"
                  @click=${this._confirmDelete}
                >
                  Delete
                </button>
                <button
                  type="button"
                  class="btn btn-sm btn-light bg-gradient border border-secondary"
                  @click=${this._cancelDelete}
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  _renderErrorModal() {
    if (!this._showError) return nothing;

    return html`
      <div class="modal-backdrop-lite" role="dialog" aria-label="Error" aria-modal="true">
        <div class="modal d-block" tabindex="-1">
          <div class="modal-dialog">
            <div class="modal-content">
              <div class="modal-header">
                <h4 class="modal-title">Error</h4>
                <button
                  type="button"
                  class="btn-close"
                  aria-label="Close"
                  @click=${this._closeErrorModal}
                ></button>
              </div>
              <div class="modal-body">
                <p class="error-message">Error: ${this._error}</p>
              </div>
              <div class="modal-footer">
                <button
                  type="button"
                  class="btn btn-sm btn-light bg-gradient border border-secondary"
                  @click=${this._closeErrorModal}
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  _renderTokenRows() {
    return this._tokens.map(
      (token) => html`
        <tr>
          <td>${token._id}</td>
          <td>${this._formatDate(token.expires)}</td>
          <td>
            <button
              class="btn btn-sm btn-danger bg-gradient border border-secondary"
              data-token-id=${token._id}
              @click=${this._handleDeleteClick}
            >
              Delete
            </button>
          </td>
        </tr>
      `,
    );
  }

  render() {
    if (this.isAdmin) {
      return this._renderAdminView();
    }

    if (this._loading) {
      return this._renderLoading();
    }

    return html`
      <div class="container-fluid">
        <div class="row">
          <div class="col-md-12"> ${this._renderCreateButtons()} ${this._renderTokenTable()} </div>
        </div>
      </div>
      ${this._renderCreatedModal()} ${this._renderDeleteModal()} ${this._renderErrorModal()}
    `;
  }
}

customElements.define("cts-token-manager", CtsTokenManager);
