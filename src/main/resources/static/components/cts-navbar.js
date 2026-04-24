import { LitElement, html, nothing } from "lit";
import { classMap } from "lit/directives/class-map.js";
import { repeat } from "lit/directives/repeat.js";
import { when } from "lit/directives/when.js";

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

// sessionStorage key for the last-seen /api/currentuser response. Read
// synchronously in connectedCallback so the first paint across
// navigations matches the outgoing page's state (no "Loading…" flash
// between pages the user is already authenticated on). Cleared on 401
// / explicit login-page visit / logout redirect.
const USER_CACHE_KEY = "cts-navbar:user";

/**
 * Top-level navigation bar. Fetches the current user from `/api/currentuser`
 * and renders either the authenticated nav or the public nav.
 * @property {string} currentPage - Key of the active page (e.g. `home`,
 *   `plans`, `logs`, `tokens`, `api-docs`); used to highlight the matching
 *   link. Reflects the `current-page` attribute.
 */
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
    // Skip the auth probe on pages with statically-known anonymous state
    // (login.html is the only one today). Previously every login-page load
    // fired a /api/currentuser that inevitably 401'd, generating server
    // log noise and a spurious ~150ms of loading chrome.
    if (window.location.pathname.endsWith("/login.html")) {
      // Clear the cache too — the user reached login.html because they
      // logged out or their session expired; the next authenticated visit
      // should re-fetch rather than render a ghost logged-in state.
      try {
        sessionStorage.removeItem(USER_CACHE_KEY);
      } catch {
        // sessionStorage may throw in privacy-mode or sandboxed contexts;
        // the cache is best-effort, so swallow and continue.
      }
      this._user = null;
      this._loading = false;
      return;
    }
    // Seed from the cache before fetching so the first render on this
    // page matches what the outgoing page showed. The async fetch still
    // runs and overwrites the cache, but any state-equal result causes
    // no visual change — the navbar appears to stay put across pages
    // and the View Transitions morph (layout.css) is a visual no-op.
    try {
      const cached = sessionStorage.getItem(USER_CACHE_KEY);
      if (cached) {
        this._user = JSON.parse(cached);
        this._loading = false;
      }
    } catch {
      // Corrupt cache or sessionStorage unavailable; fall through to
      // fetch-as-loading.
    }
    this._fetchUser();
  }

  async _fetchUser() {
    try {
      const response = await fetch("/api/currentuser");
      if (response.ok) {
        this._user = await response.json();
        try {
          sessionStorage.setItem(USER_CACHE_KEY, JSON.stringify(this._user));
        } catch {
          // Quota exceeded or sessionStorage unavailable; best-effort.
        }
      } else {
        // 401 is the expected "not logged in" response; anything else is a real
        // error we want operators to see in the console. Either way the UI
        // falls back to the public nav — that's the only affordance that works
        // without a user.
        if (response.status !== 401) {
          console.warn(`[cts-navbar] /api/currentuser responded ${response.status}`);
        }
        this._user = null;
        try {
          sessionStorage.removeItem(USER_CACHE_KEY);
        } catch {
          // best-effort
        }
      }
    } catch (err) {
      console.warn("[cts-navbar] /api/currentuser fetch failed:", err);
      // Network failure: preserve the cache and the currently-rendered
      // user state. A transient offline blip shouldn't make the user
      // appear logged out.
    } finally {
      this._loading = false;
    }
  }

  _renderNavLinks() {
    const links = this._user ? NAV_LINKS : PUBLIC_NAV_LINKS;
    const filteredLinks = this._user
      ? links.filter(
          (link) => link.page !== "tokens" || (!this._user.isAdmin && !this._user.isGuest),
        )
      : links;
    // Prefix key by list context so `api-docs` (present in both NAV_LINKS and
    // PUBLIC_NAV_LINKS) does not reuse DOM across an auth-state flip.
    const keyPrefix = this._user ? "auth" : "pub";

    return repeat(
      filteredLinks,
      (link) => `${keyPrefix}:${link.page}`,
      (link) => html`
        <li class="nav-item">
          <a
            class=${classMap({
              "nav-link": true,
              active: this.currentPage === link.page,
            })}
            href="${link.href}"
            >${link.label}</a
          >
        </li>
      `,
    );
  }

  _renderUserInfo() {
    if (this._loading) {
      return html` <span class="navbar-text text-muted">Loading&hellip;</span> `;
    }
    if (!this._user) return nothing;

    return html`
      <div class="d-flex align-items-center">
        <span class="navbar-text me-2">
          Logged in as
          ${when(this._user.isAdmin, () => html`<small class="badge bg-danger ms-1">ADMIN</small>`)}
          <span
            class="text-primary ms-1"
            data-bs-toggle="tooltip"
            title="${this._user.principal}"
            data-bs-placement="bottom"
            >${this._user.displayName}</span
          >
        </span>
        ${when(
          !this._user.isAdmin && !this._user.isGuest,
          () =>
            html`<a
              class="btn btn-sm btn-light bg-gradient border border-secondary me-2"
              href="tokens.html"
              >Tokens</a
            >`,
        )}
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
