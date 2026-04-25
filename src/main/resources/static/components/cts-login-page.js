import { LitElement, html, nothing } from "lit";
import "./cts-link-button.js";
import "./cts-alert.js";

const STYLE_ID = "cts-login-page-styles";

// Scoped styles for the login page. The wrapper centers a single card on a
// `--bg-muted` background, mirroring the design archive's auth-form preview
// (`project/preview/components-forms.html`). The 60px subtraction on
// `min-height` accounts for the `cts-navbar` height so the card sits
// vertically centered in the remaining viewport. All colors, spacing,
// radii, and shadows come from `oidf-tokens.css` — no Bootstrap classes
// are emitted by this component anymore.
const STYLE_TEXT = `
.oidf-login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 60px);
  padding: var(--space-8) var(--space-4);
  background: var(--bg-muted);
}
.oidf-login-card {
  width: 100%;
  max-width: 480px;
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  padding: var(--space-8) var(--space-6);
  background: var(--bg-elev);
  border: 1px solid var(--border);
  border-radius: var(--radius-3);
  box-shadow: var(--shadow-2);
}
.oidf-login-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-24);
  font-weight: var(--fw-bold);
  line-height: var(--lh-snug);
  color: var(--ink-900);
  text-align: center;
}
.oidf-login-providers {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.oidf-login-divider {
  display: block;
  height: 1px;
  margin: var(--space-2) 0;
  background: var(--border);
  border: 0;
}
.oidf-login-secondary {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.oidf-login-error-details {
  font-family: var(--font-mono);
  font-size: var(--fs-13);
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
 * Login/register landing page. Offers Google/GitLab OAuth buttons and links to
 * the public logs and plans listings.
 *
 * Renders a centered card on a `--bg-muted` background. The error banner uses
 * `cts-alert variant="danger"`; the post-logout banner uses
 * `cts-alert variant="info"`. Provider buttons render via `cts-link-button`
 * (`variant="secondary"` per U23's design-system mapping). All Bootstrap
 * `container`/`row`/`col-*`/`text-center`/`bg-*`/`btn-*` classes have been
 * removed; styling is driven entirely by `oidf-tokens.css` plus the scoped
 * `.oidf-login-page*` rules injected on first mount.
 *
 * @property {string} error - OAuth error message to display; empty hides the
 *   alert.
 * @property {boolean} logoutMessage - Shows the "You have been logged out"
 *   banner. Reflects the `logout-message` attribute.
 * @property {string} tokenAuthUrl - Optional URL loaded in a hidden iframe to
 *   exchange a token. Reflects the `token-auth-url` attribute.
 */
class CtsLoginPage extends LitElement {
  static properties = {
    error: { type: String },
    logoutMessage: { type: Boolean, attribute: "logout-message" },
    tokenAuthUrl: { type: String, attribute: "token-auth-url" },
  };

  constructor() {
    super();
    this.error = "";
    this.logoutMessage = false;
    this.tokenAuthUrl = "";
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
  }

  // Light DOM keeps the component composable with sibling pages and lets
  // global styles (oidf-tokens.css, layout.css) cascade in. The render-root
  // contract is preserved from the pre-U23 implementation.
  createRenderRoot() {
    return this;
  }

  _renderError() {
    if (!this.error) return nothing;
    return html`<cts-alert variant="danger">
      There was an error logging you in:
      <span class="oidf-login-error-details error-details">${this.error}</span>
    </cts-alert>`;
  }

  _renderLogout() {
    if (!this.logoutMessage) return nothing;
    return html`<cts-alert variant="info" role="status"> You have been logged out. </cts-alert>`;
  }

  _renderTokenIframe() {
    if (!this.tokenAuthUrl) return nothing;
    return html`<iframe
      src="${this.tokenAuthUrl}"
      style="display: none"
      title="Token authentication"
    ></iframe>`;
  }

  render() {
    return html`
      <div class="oidf-login-page" id="loginContent">
        <section class="oidf-login-card">
          <h1 class="oidf-login-title">
            Login to or Register with the OpenID Foundation Conformance Suite
          </h1>
          ${this._renderError()}${this._renderLogout()}
          <div class="oidf-login-providers">
            <cts-link-button
              variant="secondary"
              size="lg"
              href="/oauth2/authorization/google"
              label="Proceed with Google"
              full-width
            ></cts-link-button>
            <cts-link-button
              variant="secondary"
              size="lg"
              href="/oauth2/authorization/gitlab"
              label="Proceed with GitLab"
              full-width
            ></cts-link-button>
          </div>
          <hr class="oidf-login-divider" />
          <div class="oidf-login-secondary">
            <cts-link-button
              href="logs.html?public=true"
              variant="ghost"
              icon="files"
              label="View published logs"
              full-width
            ></cts-link-button>
            <cts-link-button
              href="plans.html?public=true"
              variant="ghost"
              icon="bookmarks"
              label="View published plans"
              full-width
            ></cts-link-button>
          </div>
        </section>
        ${this._renderTokenIframe()}
      </div>
    `;
  }
}

customElements.define("cts-login-page", CtsLoginPage);

export {};
