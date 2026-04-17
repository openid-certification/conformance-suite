import { LitElement, html, nothing } from "lit";

const NAV_LINKS = [
  { page: "home", label: "Home", href: "index.html" },
  { page: "create-test", label: "Create Test", href: "schedule-test.html" },
  { page: "plans", label: "Test Plans", href: "plans.html" },
  { page: "logs", label: "Test Logs", href: "logs.html" },
  { page: "tokens", label: "Tokens", href: "tokens.html" },
  { page: "api-docs", label: "API Docs", href: "api-document.html" },
];

const PUBLIC_NAV_LINKS = [
  { page: "public-logs", label: "Published Logs", href: "logs.html?public=true" },
  { page: "public-plans", label: "Published Plans", href: "plans.html?public=true" },
  { page: "api-docs", label: "API Docs", href: "api-document.html" },
];

class CtsNavbar extends LitElement {
  static properties = {
    currentPage: { type: String, attribute: "current-page" },
    _user: { state: true },
    _loading: { state: true },
  };

  constructor() {
    super();
    this.currentPage = "";
    this._user = null;
    this._loading = true;
  }

  // Use light DOM so Bootstrap CSS applies
  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    this._fetchUser();
  }

  async _fetchUser() {
    try {
      const response = await fetch("/api/currentuser");
      if (response.ok) {
        this._user = await response.json();
      } else {
        // 401 is the expected "not logged in" response; anything else is a real
        // error we want operators to see in the console. Either way the UI
        // falls back to the public nav — that's the only affordance that works
        // without a user.
        if (response.status !== 401) {
          console.warn(`[cts-navbar] /api/currentuser responded ${response.status}`);
        }
        this._user = null;
      }
    } catch (err) {
      console.warn("[cts-navbar] /api/currentuser fetch failed:", err);
      this._user = null;
    } finally {
      this._loading = false;
    }
  }

  _renderNavLinks() {
    const links = this._user ? NAV_LINKS : PUBLIC_NAV_LINKS;
    const filteredLinks = this._user
      ? links.filter(
          (link) =>
            link.page !== "tokens" ||
            (!this._user.isAdmin && !this._user.isGuest),
        )
      : links;

    return filteredLinks.map(
      (link) => html`
        <li class="nav-item">
          <a
            class="nav-link${this.currentPage === link.page ? " active" : ""}"
            href="${link.href}"
            >${link.label}</a
          >
        </li>
      `,
    );
  }

  _renderUserInfo() {
    if (this._loading) {
      return html`
        <span class="navbar-text text-muted">Loading&hellip;</span>
      `;
    }
    if (!this._user) return nothing;

    return html`
      <div class="d-flex align-items-center">
        <span class="navbar-text me-2">
          Logged in as
          ${this._user.isAdmin
            ? html`<small class="badge bg-danger ms-1">ADMIN</small>`
            : nothing}
          <span
            class="text-primary ms-1"
            data-bs-toggle="tooltip"
            title="${this._user.principal}"
            data-bs-placement="bottom"
            >${this._user.displayName}</span
          >
        </span>
        ${!this._user.isAdmin && !this._user.isGuest
          ? html`<a
              class="btn btn-sm btn-light bg-gradient border border-secondary me-2"
              href="tokens.html"
              >Tokens</a
            >`
          : nothing}
        <form action="/logout" method="post" class="d-inline">
          <input
            type="submit"
            class="btn btn-sm btn-primary bg-gradient border border-secondary"
            value="Logout"
          />
        </form>
      </div>
    `;
  }

  render() {
    return html`
      <nav class="navbar navbar-expand-md pageHeader container-fluid">
        <a class="navbar-brand" href="index.html">
          <img src="/images/openid.png" alt="OpenID" />
        </a>
        <button
          class="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#cts-navbar-collapse"
          aria-controls="cts-navbar-collapse"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="cts-navbar-collapse">
          <ul class="navbar-nav me-auto">
            ${this._renderNavLinks()}
          </ul>
          ${this._renderUserInfo()}
        </div>
      </nav>
    `;
  }
}

customElements.define("cts-navbar", CtsNavbar);
