import { LitElement, html, nothing } from "lit";
import "./cts-link-button.js";

const SERVER_INFO_LABELS = {
  external_ip: "External IP",
  version: "Version",
  revision: "Revision",
  tag: "Tag",
  build_time: "Build Time",
};

class CtsDashboard extends LitElement {
  static properties = {
    isAuthenticated: { type: Boolean, attribute: "is-authenticated" },
    _serverInfo: { state: true },
    _loading: { state: true },
  };

  constructor() {
    super();
    this.isAuthenticated = true;
    this._serverInfo = null;
    this._loading = true;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    this._fetchServerInfo();
  }

  async _fetchServerInfo() {
    this._loading = true;
    try {
      const response = await fetch("/api/server");
      if (response.ok) {
        this._serverInfo = await response.json();
      } else {
        // Server info is non-critical; cards render regardless. Log so operators
        // can diagnose if /api/server starts failing silently.
        console.warn(`[cts-dashboard] /api/server responded ${response.status}`);
      }
    } catch (err) {
      console.warn("[cts-dashboard] /api/server fetch failed:", err);
    } finally {
      this._loading = false;
    }
  }

  _renderServerInfo() {
    if (!this._serverInfo) {
      return nothing;
    }
    const parts = Object.entries(SERVER_INFO_LABELS)
      .filter(([key]) => key in this._serverInfo)
      .map(
        ([key, label]) =>
          html`${label}:
            <span id="serverinfo-${key}">${this._serverInfo[key]}</span>`,
      );
    if (parts.length === 0) {
      return nothing;
    }
    return html`<div>
      ${parts.map(
        (part, i) => html`${i > 0 ? " | " : ""}${part}`,
      )}
    </div>`;
  }

  render() {
    return html`
      <div class="container-fluid">
        <div id="homePage">
          <div class="row">
            <div class="col-md-4"></div>
            <div class="col-md-4">
              <div class="d-grid gap-0">
                ${this.isAuthenticated
                  ? html`
                      <cts-link-button
                        href="schedule-test.html"
                        variant="info"
                        icon="files"
                        label="Create a new test plan"
                        full-width
                      ></cts-link-button>
                      <br />
                      <cts-link-button
                        href="logs.html"
                        variant="info"
                        icon="files"
                        label="View my test logs"
                        full-width
                      ></cts-link-button>
                      <br />
                      <cts-link-button
                        href="plans.html"
                        variant="info"
                        icon="bookmarks"
                        label="View my test plans"
                        full-width
                      ></cts-link-button>
                      <br />
                    `
                  : nothing}
                <cts-link-button
                  href="logs.html?public=true"
                  variant="info"
                  icon="files"
                  label="View all published test logs"
                  full-width
                ></cts-link-button>
                <br />
                <cts-link-button
                  href="plans.html?public=true"
                  variant="info"
                  icon="bookmarks"
                  label="View all published test plans"
                  full-width
                ></cts-link-button>
                <br />
                <cts-link-button
                  href="api-document.html"
                  variant="info"
                  icon="bookmarks"
                  label="View API Documentation"
                  full-width
                ></cts-link-button>
              </div>
            </div>
            <div class="col-md-4"></div>
          </div>
        </div>
      </div>

      <footer class="pageFooter">
        <span class="muted">OpenID Foundation conformance suite</span>
        <div class="serverInfo">${this._renderServerInfo()}</div>
      </footer>
    `;
  }
}

customElements.define("cts-dashboard", CtsDashboard);
