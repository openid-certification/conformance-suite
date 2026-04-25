import { LitElement, html } from "lit";
import "./cts-modal.js";
import "./cts-button.js";
import "./cts-link-button.js";

// Screen-reader announcement + visible feedback should stay long enough for
// assistive tech to finish reading the message. 2-3s is a common mistake.
const COPY_FEEDBACK_DURATION_MS = 5000;

const STYLE_ID = "cts-token-manager-styles";

// Scoped CSS for the OIDF-tokenized token-manager surface. Replaces the
// Bootstrap container/row/col scaffolding and the legacy `.table table-*`
// styling that comes from Bootstrap. The token list table keeps its
// `id="tokensListing"` so the existing E2E selectors still match; it picks
// up token-driven cell padding, header background, and row separators here.
// U33 will swap the table for `<cts-data-table>` — until then this scoped
// CSS keeps the table legible after Bootstrap removal.
const STYLE_TEXT = `
.cts-token-manager {
  display: block;
  padding: var(--space-4) var(--space-5);
  font-family: var(--font-sans);
  color: var(--fg);
}
.cts-token-manager-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}
.cts-token-manager-message {
  font-size: var(--fs-13);
  color: var(--fg-muted);
  margin: 0 0 var(--space-3) 0;
}
.cts-token-manager-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--fs-13);
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-2);
  overflow: hidden;
}
.cts-token-manager-table thead th {
  text-align: left;
  font-weight: var(--fw-bold);
  padding: var(--space-3) var(--space-3);
  background: var(--ink-50);
  color: var(--ink-900);
  border-bottom: 1px solid var(--border);
}
.cts-token-manager-table tbody td {
  padding: var(--space-3) var(--space-3);
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
  color: var(--fg);
}
.cts-token-manager-table tbody tr:last-child td {
  border-bottom: 0;
}
.cts-token-manager-table tbody tr:nth-child(odd) td {
  background: var(--ink-50);
}
.cts-token-manager-table tbody tr:hover td {
  background: var(--ink-100);
}
.cts-token-manager-created-modal-body p {
  margin: 0 0 var(--space-3) 0;
}
.cts-token-manager-created-modal-body p:last-child {
  margin-bottom: 0;
}
.cts-token-manager-copy-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.cts-token-manager-copy-feedback {
  font-size: var(--fs-12);
  color: var(--rust-500);
}
.cts-token-manager-token-value {
  background: var(--ink-50);
  border: 1px solid var(--border);
  border-radius: var(--radius-2);
  padding: var(--space-3);
  font-family: var(--font-mono);
  font-size: var(--fs-13);
  color: var(--fg);
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
.cts-token-manager-error-message {
  margin: 0;
  color: var(--rust-500);
  font-size: var(--fs-13);
}
`;

/**
 * Inject the cts-token-manager scoped stylesheet into `<head>` exactly once.
 * Idempotent: subsequent calls find the existing `<style>` tag by id and
 * bail.
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
 * Token management UI. Lists existing API tokens via `/api/token`, lets the
 * user create temporary or permanent tokens, and confirms deletion. Admins
 * see a read-only message instead (admins cannot create tokens).
 *
 * The three modals (Token created / Delete confirmation / Error) are
 * rendered as `<cts-modal>` instances so they survive Bootstrap CSS removal
 * (Phase E of the OIDF design-system migration). The host's `.show()` /
 * `.hide()` methods drive open/close; `cts-modal-close` on the delete
 * modal clears `_deleteTokenId` regardless of how the dialog was dismissed
 * (Cancel button, ESC, backdrop click, or header close).
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
    this._copyFeedback = "";
    this._copyFeedbackTimer = null;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    this._fetchTokens();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._copyFeedbackTimer) {
      clearTimeout(this._copyFeedbackTimer);
      this._copyFeedbackTimer = null;
    }
  }

  // Light DOM keeps document.getElementById and global CSS tokens in scope.
  createRenderRoot() {
    return this;
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
        await this._showModal("createdErrorModal");
        return;
      }
      const data = await response.json();
      this._createdToken = data.token;
      await this._showModal("createdTokenModal");
      await this._fetchTokens();
    } catch (err) {
      this._error = (err instanceof Error && err.message) || "Failed to create token";
      await this._showModal("createdErrorModal");
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
      await this._showModal("createdErrorModal");
    }
  }

  /**
   * Imperatively open one of the local cts-modal hosts after the next Lit
   * render flushes. The cts-modal element is rendered via Lit, so we wait
   * for `updateComplete` before reaching for it — otherwise the first
   * open-after-state-change would race the Lit microtask.
   * @param {string} modalRefId - Local id of the cts-modal in light DOM
   * @returns {Promise<void>}
   */
  async _showModal(modalRefId) {
    await this.updateComplete;
    const modal = /** @type {(HTMLElement & { show?: () => void }) | null} */ (
      this.querySelector(`#${modalRefId}`)
    );
    if (modal && typeof modal.show === "function") modal.show();
  }

  /**
   * Imperatively close one of the local cts-modal hosts.
   * @param {string} modalRefId - Local id of the cts-modal in light DOM
   * @returns {void}
   */
  _hideModal(modalRefId) {
    const modal = /** @type {(HTMLElement & { hide?: () => void }) | null} */ (
      this.querySelector(`#${modalRefId}`)
    );
    if (modal && typeof modal.hide === "function") modal.hide();
  }

  _handleDeleteClick(event) {
    const host = /** @type {HTMLElement} */ (event.currentTarget);
    const tokenId = host.dataset.tokenId;
    if (!tokenId) return;
    this._deleteTokenId = tokenId;
    this._showModal("deleteTokenModal");
  }

  /**
   * Confirm-delete handler wired via the cts-modal `footer-buttons`
   * descriptor. The descriptor sets `id: "confirmDeleteBtn"` and
   * `dismiss: false` so the button stays under our control — we close the
   * modal first, then issue the delete. Listener is attached once in
   * `updated()` (see comment there).
   * @returns {void}
   */
  _handleConfirmDelete() {
    const tokenId = this._deleteTokenId;
    this._deleteTokenId = "";
    this._hideModal("deleteTokenModal");
    if (tokenId) {
      this._deleteToken(tokenId);
    }
  }

  /**
   * Wire up the confirm-delete button click after every render. The footer
   * button is created by cts-modal during its `connectedCallback`, which
   * only runs once Lit appends the cts-modal host. The first render in
   * the loading state does not include the modals, so we re-check on every
   * `updated()` and attach the listener exactly once via the
   * `_confirmDeleteWired` flag.
   * @returns {void}
   */
  updated() {
    if (this.isAdmin || this._confirmDeleteWired) return;
    const confirmBtn = this.querySelector("#confirmDeleteBtn");
    if (confirmBtn) {
      confirmBtn.addEventListener("click", () => this._handleConfirmDelete());
      this._confirmDeleteWired = true;
    }
  }

  /**
   * Reset state when the delete modal closes — covers Cancel button, ESC,
   * backdrop click, and header close button uniformly.
   * @returns {void}
   */
  _handleDeleteModalClose() {
    this._deleteTokenId = "";
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
      <div class="cts-token-manager">
        <p class="cts-token-manager-message admin-message">
          Admin users cannot create tokens - please login as a non-admin user.
        </p>
      </div>
    `;
  }

  _renderTokenTable() {
    if (this._tokens.length === 0) {
      return html`<p class="cts-token-manager-message no-tokens-message">
        No tokens have been created yet.
      </p>`;
    }

    return html`
      <table class="cts-token-manager-table" id="tokensListing">
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
      <div class="cts-token-manager-actions">
        <cts-button
          variant="primary"
          size="lg"
          label="New temporary token"
          @cts-click=${this._createTemporaryToken}
        ></cts-button>
        <cts-button
          variant="primary"
          size="lg"
          label="New permanent token"
          @cts-click=${this._createPermanentToken}
        ></cts-button>
        <cts-link-button
          variant="secondary"
          size="lg"
          href="/api-document.html"
          label="API Documentation"
        ></cts-link-button>
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
            <cts-button
              class="deleteBtn"
              variant="danger"
              size="sm"
              label="Delete"
              data-token-id=${token._id}
              @cts-click=${this._handleDeleteClick}
            ></cts-button>
          </td>
        </tr>
      `,
    );
  }

  /**
   * Three modals rendered as cts-modal hosts. Children are captured by
   * cts-modal once on first connect and physically moved into the inner
   * `.oidf-modal-body` div — Lit retains direct references to those nodes
   * so subsequent text-content updates (e.g. `${this._createdToken}`)
   * still flow through. Adding/removing top-level children of the
   * cts-modal host between renders is unsupported, so the structure here
   * stays stable: the `_copyFeedback` span is always rendered and only
   * its content varies.
   * @returns {ReturnType<typeof html>} Lit template containing the three
   *   `<cts-modal>` instances (Token created / Delete / Error).
   */
  _renderModals() {
    return html`
      <cts-modal id="createdTokenModal" heading="Token created" size="lg">
        <div class="cts-token-manager-created-modal-body">
          <div class="cts-token-manager-copy-row">
            <cts-button
              variant="secondary"
              size="sm"
              icon="clipboard"
              label="Copy"
              title="Copy token to clipboard"
              @cts-click=${this._copyToken}
            ></cts-button>
            <span
              class="cts-token-manager-copy-feedback"
              role="status"
              aria-live="polite"
              data-testid="copy-feedback"
              >${this._copyFeedback || ""}</span
            >
          </div>
          <p>Here is your new token. This value will only be displayed once.</p>
          <pre class="cts-token-manager-token-value created-token-value">${this._createdToken}</pre>
        </div>
      </cts-modal>

      <cts-modal
        id="deleteTokenModal"
        heading="Delete"
        footer-buttons='[{"label":"Delete","class":"btn-danger","id":"confirmDeleteBtn","dismiss":false},{"label":"Cancel"}]'
        @cts-modal-close=${this._handleDeleteModalClose}
      >
        <p>Are you sure? This will permanently remove this token.</p>
      </cts-modal>

      <cts-modal id="createdErrorModal" heading="Error">
        <p class="cts-token-manager-error-message error-message">Error: ${this._error}</p>
      </cts-modal>
    `;
  }

  /**
   * Render the table area: a loading placeholder during fetches, an empty
   * state when the API returns no tokens, or the data table otherwise.
   * Kept separate from `render()` so the modals stay mounted across
   * fetch / refetch cycles — re-mounting `<cts-modal>` would lose any
   * imperatively wired listeners (e.g. confirm-delete).
   * @returns {ReturnType<typeof html>} Lit template for the loading,
   *   empty, or populated table state.
   */
  _renderTableArea() {
    if (this._loading) {
      return html`<p class="cts-token-manager-message">Loading...</p>`;
    }
    return this._renderTokenTable();
  }

  render() {
    if (this.isAdmin) {
      return this._renderAdminView();
    }

    return html`
      <div class="cts-token-manager">
        ${this._renderCreateButtons()} ${this._renderTableArea()}
      </div>
      ${this._renderModals()}
    `;
  }
}

customElements.define("cts-token-manager", CtsTokenManager);

export {};
