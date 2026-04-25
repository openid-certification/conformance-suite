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

// Single-injection scoped CSS (same pattern as cts-card). The rules are
// scoped to the .cts-nav class tree so they cannot leak onto unrelated
// nav-* Bootstrap markup elsewhere on the page. The :not(:defined)
// fallback in css/layout.css takes care of FOUC reservation; these
// rules only apply post-upgrade.
const STYLE_ID = "cts-navbar-styles";

const STYLE_TEXT = `
.cts-nav {
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 var(--space-5);
  background: var(--ink-900);
  color: var(--ink-0);
  gap: var(--space-6);
  font-family: var(--font-sans);
}
.cts-nav .cts-brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  text-decoration: none;
  color: var(--ink-0);
}
.cts-nav .cts-brand img {
  height: 28px;
  width: auto;
}
.cts-nav .cts-brand-name {
  font-family: var(--font-display);
  font-weight: var(--fw-black);
  font-size: var(--fs-13);
  letter-spacing: 0.04em;
  color: var(--ink-0);
}
.cts-nav .cts-brand-tag {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--ink-400);
  padding-left: var(--space-2);
  border-left: 1px solid var(--ink-600);
  margin-left: var(--space-2);
}
.cts-nav .cts-navlinks {
  display: flex;
  gap: var(--space-1);
  flex: 1;
  list-style: none;
  margin: 0;
  padding: 0;
}
.cts-nav .cts-navlink {
  display: inline-block;
  padding: var(--space-2) var(--space-3);
  color: var(--ink-200);
  font-size: var(--fs-13);
  font-weight: 600;
  text-decoration: none;
  border-radius: var(--radius-2);
  background: transparent;
  border: 0;
  cursor: pointer;
  font-family: inherit;
  line-height: 1;
}
.cts-nav .cts-navlink:hover:not(.active) {
  background: var(--ink-800);
  color: var(--ink-0);
  text-decoration: none;
}
.cts-nav .cts-navlink.active {
  background: var(--ink-700);
  color: var(--ink-0);
}
.cts-nav .cts-navright {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.cts-nav .cts-nav-text {
  color: var(--ink-200);
  font-size: var(--fs-13);
}
.cts-nav .cts-nav-name {
  color: var(--ink-0);
  font-weight: var(--fw-bold);
  margin-left: var(--space-1);
}
.cts-nav .cts-nav-admin-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px var(--space-2);
  font-size: 10px;
  font-weight: var(--fw-bold);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  background: var(--rust-400);
  color: var(--ink-0);
  border-radius: var(--radius-pill);
  margin-left: var(--space-1);
}
.cts-nav .cts-nav-action {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 var(--space-3);
  background: transparent;
  color: var(--ink-200);
  border: 1px solid var(--ink-600);
  border-radius: var(--radius-2);
  font-size: var(--fs-12);
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  text-decoration: none;
  line-height: 1;
}
.cts-nav .cts-nav-action:hover {
  background: var(--ink-800);
  color: var(--ink-0);
  text-decoration: none;
}
.cts-nav .cts-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--orange-400);
  color: var(--ink-0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-12);
  font-weight: var(--fw-bold);
  font-family: var(--font-sans);
  margin-left: var(--space-2);
}
.cts-nav .cts-nav-loading {
  color: var(--ink-400);
  font-size: var(--fs-13);
}
.cts-nav .cts-nav-logout {
  display: inline-flex;
  margin: 0;
  padding: 0;
}
`;

/**
 * Inject the scoped navbar stylesheet into <head> exactly once across all
 * cts-navbar instances. Idempotent — safe to call from every connectedCallback.
 */
function injectStyles() {
  if (typeof document === "undefined") return;
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Compute up to two-letter initials from a display name. Falls back to a
 * single dot when the name is empty so the avatar circle never renders
 * empty (which would look like a misaligned colored dot).
 * @param {string} name - Full display name to derive initials from.
 * @returns {string} Up to two uppercase letters, or "·" for empty input.
 */
function computeInitials(name) {
  if (!name || typeof name !== "string") return "·";
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "·";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

/**
 * Top-level navigation bar. Fetches the current user from `/api/currentuser`
 * and renders either the authenticated nav or the public nav.
 *
 * Visual contract: matches the OIDF design system navbar preview at
 * `project/preview/components-navbar.html` — ink-900 background, brand block
 * on the left, link buttons centered, action buttons + avatar on the right.
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

  // Light DOM so the global tokens (--ink-900 etc.) and existing page
  // selectors continue to reach inside the component.
  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
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
        <li>
          <a
            class=${classMap({
              "cts-navlink": true,
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
      return html`<span class="cts-nav-loading navbar-text">Loading&hellip;</span>`;
    }
    if (!this._user) return nothing;

    const initials = computeInitials(this._user.displayName);
    return html`
      <span class="cts-nav-text navbar-text">
        Logged in as
        ${when(this._user.isAdmin, () => html`<span class="cts-nav-admin-badge">ADMIN</span>`)}
        <span
          class="cts-nav-name"
          data-bs-toggle="tooltip"
          title="${this._user.principal}"
          data-bs-placement="bottom"
          >${this._user.displayName}</span
        >
      </span>
      ${when(
        !this._user.isAdmin && !this._user.isGuest,
        () => html`<a class="cts-nav-action btn" href="tokens.html">Tokens</a>`,
      )}
      <form action="/logout" method="post" class="cts-nav-logout">
        <input type="submit" class="cts-nav-action btn" value="Logout" />
      </form>
      <div class="cts-avatar" aria-hidden="true">${initials}</div>
    `;
  }

  render() {
    return html`
      <nav class="cts-nav">
        <a class="cts-brand navbar-brand" href="index.html">
          <img src="/images/openid.png" alt="OpenID" />
          <span class="cts-brand-name">CONFORMANCE SUITE</span>
        </a>
        <ul class="cts-navlinks navbar-nav">
          ${this._renderNavLinks()}
        </ul>
        <div class="cts-navright">${this._renderUserInfo()}</div>
      </nav>
    `;
  }
}

customElements.define("cts-navbar", CtsNavbar);
