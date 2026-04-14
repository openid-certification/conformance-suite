import { LitElement, html, nothing } from "lit";

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

  // Use light DOM so Bootstrap CSS applies
  createRenderRoot() {
    return this;
  }

  _renderError() {
    if (!this.error) return nothing;
    return html`<p class="bg-danger p-2 rounded text-white" role="alert">
      There was an error logging you in:
      <span class="error-details">${this.error}</span>
    </p>`;
  }

  _renderLogout() {
    if (!this.logoutMessage) return nothing;
    return html`<p class="bg-info p-2 rounded" role="status">
      You have been logged out.
    </p>`;
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
      <div class="container-fluid" id="loginContent">
        <div class="row">
          <div class="col-xs-12 col-md-6 col-md-offset-3 mx-auto center-text">
            <h1 class="text-center">
              Login to or Register with the OpenID Foundation Conformance Suite
            </h1>
            ${this._renderError()} ${this._renderLogout()}
            <p class="text-center">
              <a
                class="btn btn-lg btn-danger bg-gradient border border-secondary"
                href="/oauth2/authorization/google"
                >Proceed with Google</a
              >
              <a
                class="btn btn-lg btn-primary bg-gradient border border-secondary"
                href="/oauth2/authorization/gitlab"
                >Proceed with GitLab</a
              >
            </p>
          </div>
        </div>
        <div class="row">
          <div class="mx-auto col-md-2">
            <div class="d-grid gap-2">
              <cts-link-button
                href="logs.html?public=true"
                variant="info"
                icon="files"
                label="View published logs"
              ></cts-link-button>
              <cts-link-button
                href="plans.html?public=true"
                variant="info"
                icon="bookmarks"
                label="View published plans"
              ></cts-link-button>
            </div>
          </div>
        </div>
        ${this._renderTokenIframe()}
      </div>
    `;
  }
}

customElements.define("cts-login-page", CtsLoginPage);
