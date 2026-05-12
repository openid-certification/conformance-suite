import { LitElement, html } from "lit";
import { classMap } from "lit/directives/class-map.js";
import { repeat } from "lit/directives/repeat.js";
import { when } from "lit/directives/when.js";
import "./cts-icon.js";

const NAV_LINKS = [
  { page: "home", label: "Home", href: "index.html" },
  { page: "create-test", label: "Create Test", href: "schedule-test.html" },
  { page: "plans", label: "Test Plans", href: "plans.html" },
  { page: "logs", label: "Test Logs", href: "logs.html" },
  { page: "tokens", label: "Tokens", href: "tokens.html" },
  { page: "api-docs", label: "API Docs", href: "api-document.html", external: true },
];

const PUBLIC_NAV_LINKS = [
  { page: "public-logs", label: "Published Logs", href: "logs.html?public=true" },
  { page: "public-plans", label: "Published Plans", href: "plans.html?public=true" },
  { page: "api-docs", label: "API Docs", href: "api-document.html", external: true },
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
  position: relative;
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 var(--space-5);
  background: var(--ink-900);
  color: var(--ink-0);
  gap: var(--space-6);
  font-family: var(--font-sans);
}
/* Hamburger button — hidden at wide widths, revealed inside the
 * narrow-viewport media query below. Lives in the right cluster so
 * the chrome reads "logo … hamburger / avatar" — both controls land
 * under the user's right-thumb reach on phones. */
.cts-nav .cts-menu-toggle {
  display: none;
  width: 36px;
  height: 36px;
  padding: 0;
  margin: 0;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-2);
  color: var(--ink-200);
  cursor: pointer;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.cts-nav .cts-menu-toggle:hover {
  background: var(--ink-800);
  color: var(--ink-0);
}
.cts-nav .cts-menu-toggle:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 2px var(--ink-900),
    0 0 0 4px var(--orange-400);
}
.cts-nav .cts-menu-toggle svg {
  display: block;
}
.cts-nav .cts-menu-toggle .cts-menu-toggle-bar {
  transition:
    transform 160ms ease,
    opacity 160ms ease;
  /* fill-box scopes transform-origin to each <line>'s own bbox so
   * "center" means each bar's own midpoint, not the SVG's. Without
   * this, rotate(45deg) pivots around the viewBox center and the
   * bars don't converge into a tidy X. */
  transform-box: fill-box;
  transform-origin: center;
}
/* Hamburger → close glyph. The svg uses a 20×20 viewBox rendered at
 * 20×20 CSS pixels so 1 user unit == 1 CSS pixel — that lets the
 * translateY(4px) values exactly bridge the gap between bars (which
 * sit at y=6, y=10, y=14). */
.cts-nav[data-mobile-open="true"] .cts-menu-toggle .cts-menu-toggle-bar--top {
  transform: translateY(4px) rotate(45deg);
}
.cts-nav[data-mobile-open="true"] .cts-menu-toggle .cts-menu-toggle-bar--mid {
  opacity: 0;
}
.cts-nav[data-mobile-open="true"] .cts-menu-toggle .cts-menu-toggle-bar--bot {
  transform: translateY(-4px) rotate(-45deg);
}
.cts-nav .cts-brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  text-decoration: none;
  color: var(--ink-0);
  flex-shrink: 0;
}
.cts-nav .cts-brand img {
  height: 28px;
  width: auto;
  /* Optical lift — the OpenID mark's baseline sits a touch low against
   * the wordmark; -4px aligns the visual center of the logo with the
   * wordmark's cap-height row. Reset to 0 on mobile where the wordmark
   * is hidden and the lift would just look like a misalignment. */
  position: relative;
  top: -4px;
}
.cts-nav .cts-brand-name {
  font-family: var(--font-display);
  font-weight: var(--fw-bold);
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
  /* Bootstrap's .navbar-nav rule sets flex-direction: column (mobile-first
   * default; only flipped to row by .navbar-expand-* on the parent, which
   * this nav does not use). Spelling row out explicitly keeps the link
   * row horizontal regardless of Bootstrap's cascade. */
  flex-direction: row;
  align-items: center;
  gap: var(--space-1);
  flex: 1;
  min-width: 0;
  list-style: none;
  margin: 0;
  padding: 0;
}
.cts-nav .cts-navlink {
  display: inline-block;
  padding: var(--space-2) var(--space-3);
  color: var(--ink-200);
  font-size: var(--fs-13);
  font-weight: var(--fw-medium);
  text-decoration: none;
  border-radius: var(--radius-2);
  background: transparent;
  border: 0;
  cursor: pointer;
  font-family: inherit;
  line-height: 1;
  white-space: nowrap;
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
.cts-nav .cts-navlink-external {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}
/* Pull the icon a hair toward the baseline so it visually centers next
 * to the cap-height of the label rather than the line-height midpoint. */
.cts-nav .cts-navlink-external cts-icon {
  display: block;
  color: var(--ink-400);
  transform: translateY(0.5px);
}
.cts-nav .cts-navlink-external:hover:not(.active) cts-icon,
.cts-nav .cts-navlink-external.active cts-icon {
  color: currentColor;
}
.cts-nav .cts-navright {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
}
.cts-nav .cts-nav-admin-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px var(--space-2);
  font-size: 10px;
  font-weight: var(--fw-medium);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  background: var(--rust-400);
  color: var(--ink-0);
  border-radius: var(--radius-pill);
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
  font-weight: var(--fw-medium);
  font-family: inherit;
  cursor: pointer;
  text-decoration: none;
  line-height: 1;
  white-space: nowrap;
}
.cts-nav .cts-nav-action:hover {
  background: var(--ink-800);
  color: var(--ink-0);
  text-decoration: none;
}
.cts-nav .cts-nav-action:focus-visible {
  outline: 2px solid var(--orange-400);
  outline-offset: 2px;
}

/* Account zone — avatar trigger + popover menu. Replaces the previous
 * inline "Logged in as X" + Tokens + Logout cluster. The trigger is
 * always 30x30 regardless of name length, so the navbar's right edge
 * never reflows when the user resolves. */
.cts-nav .cts-account {
  position: relative;
  display: inline-flex;
}
.cts-nav .cts-account-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  margin: 0;
  background: transparent;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  font-family: inherit;
  /* Pad the focus ring slightly off the avatar so it reads against
   * --ink-900 chrome. */
  transition: box-shadow 120ms ease;
}
.cts-nav .cts-account-trigger:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 2px var(--ink-900),
    0 0 0 4px var(--orange-400);
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
  font-weight: var(--fw-medium);
  font-family: var(--font-sans);
  flex-shrink: 0;
  /* Subtle inner shadow on hover keeps the affordance discoverable
   * without inventing a second hover state for an already-saturated chip. */
  transition: filter 120ms ease;
}
.cts-nav .cts-account-trigger:hover .cts-avatar,
.cts-nav .cts-account[data-open="true"] .cts-avatar {
  filter: brightness(1.08);
}
/* Admin identity is also surfaced peripherally via a rust-toned ring on
 * the avatar so it's visible when the menu is closed. */
.cts-nav .cts-avatar.is-admin {
  box-shadow: inset 0 0 0 2px var(--rust-400);
}
.cts-nav .cts-skel-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--ink-700);
  flex-shrink: 0;
}
.cts-nav .cts-account-menu {
  position: absolute;
  top: calc(100% + var(--space-2));
  right: 0;
  min-width: 240px;
  background: var(--ink-800);
  color: var(--ink-100);
  border: 1px solid var(--ink-700);
  border-radius: var(--radius-3);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.04) inset,
    0 12px 32px rgba(0, 0, 0, 0.45);
  padding: var(--space-2);
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-family: var(--font-sans);
  opacity: 0;
  pointer-events: none;
  transform: translateY(-4px);
  transition:
    opacity 120ms ease,
    transform 120ms ease;
  z-index: 1000;
}
.cts-nav .cts-account[data-open="true"] .cts-account-menu {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}
.cts-nav .cts-account-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-3);
}
.cts-nav .cts-account-name {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
  font-weight: var(--fw-medium);
  font-size: var(--fs-14);
  color: var(--ink-0);
  line-height: 1.25;
}
.cts-nav .cts-account-principal {
  font-family: var(--font-mono);
  font-size: var(--fs-12);
  color: var(--ink-400);
  word-break: break-all;
  line-height: 1.4;
}
.cts-nav .cts-account-divider {
  height: 1px;
  margin: 0 var(--space-1);
  background: var(--ink-700);
}
.cts-nav .cts-account-form {
  margin: 0;
  padding: 0;
}
.cts-nav .cts-account-item {
  display: flex;
  align-items: center;
  width: 100%;
  padding: var(--space-2) var(--space-3);
  background: transparent;
  border: 0;
  border-radius: var(--radius-2);
  color: var(--ink-100);
  font-size: var(--fs-13);
  font-weight: var(--fw-medium);
  font-family: inherit;
  text-decoration: none;
  text-align: left;
  cursor: pointer;
  line-height: 1.4;
}
.cts-nav .cts-account-item:hover,
.cts-nav .cts-account-item:focus-visible {
  background: var(--ink-700);
  color: var(--ink-0);
  outline: none;
  text-decoration: none;
}
.cts-nav .cts-account-item--danger:hover,
.cts-nav .cts-account-item--danger:focus-visible {
  background: var(--rust-500);
  color: var(--ink-0);
}

/* Tablet landscape and below — tighten the brand block. The user zone
 * is now a single avatar so there's nothing on the right to compress. */
@media (max-width: 1023px) {
  .cts-nav {
    gap: var(--space-4);
    padding: 0 var(--space-4);
  }
}

/* Below tablet portrait — collapse brand to logo only and migrate the
 * link row into a vertical panel that drops below the chrome when the
 * hamburger is toggled. The same <ul> serves both layouts; only its
 * positioning and flex direction swap. */
@media (max-width: 820px) {
  .cts-nav {
    gap: var(--space-3);
  }
  /* Nudge the brand up a few pixels on small screens to optically align
   * with the hamburger and avatar icons in the collapsed row. */
  .cts-nav .cts-brand {
    position: relative;
    top: -3px;
  }
  .cts-nav .cts-brand-name {
    display: none;
  }
  /* Shrink the logo a notch on phones — the wordmark is gone so the
   * mark itself can come down without losing legibility, leaving more
   * room for the hamburger and avatar at the row edges. The desktop
   * optical lift is reset since there's no wordmark to align against. */
  .cts-nav .cts-brand img {
    height: 22px;
    top: 0;
  }
  .cts-nav .cts-menu-toggle {
    display: inline-flex;
  }
  /* With navlinks pulled out of flow (position: absolute), the in-flow
   * row collapses to [hamburger][brand][navright] packed by gap. Shove
   * the account zone to the far right edge so the chrome reads
   * "navigation > brand > identity" on phones. */
  .cts-nav .cts-navright {
    margin-left: auto;
  }
  /* The link row pulls out of the in-flow row and becomes a popover
   * pinned to the navbar's bottom edge. Hidden by default; revealed
   * when [data-mobile-open="true"] flips on the parent. */
  .cts-nav .cts-navlinks {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    flex: 0 0 auto;
    flex-direction: column;
    align-items: stretch;
    gap: 0;
    padding: var(--space-2);
    background: var(--ink-900);
    border-top: 1px solid var(--ink-800);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.45);
    opacity: 0;
    pointer-events: none;
    transform: translateY(-4px);
    transition:
      opacity 140ms ease,
      transform 140ms ease;
    z-index: 999;
  }
  .cts-nav[data-mobile-open="true"] .cts-navlinks {
    opacity: 1;
    pointer-events: auto;
    transform: translateY(0);
  }
  .cts-nav .cts-navlink {
    display: block;
    padding: var(--space-3);
    font-size: var(--fs-14);
    border-radius: var(--radius-2);
  }
  /* External links keep their flex layout in the mobile panel so the
   * icon stays inline with the label. Without this, the base
   * .cts-navlink { display: block } above would override the
   * .cts-navlink-external { display: inline-flex } from the
   * non-media-query rule (same specificity, later wins) and the
   * block-level icon would drop to its own line. */
  .cts-nav .cts-navlink-external {
    display: flex;
    align-items: center;
  }
}

/* Phones — tighten edge padding; the menu pins to the right edge of
 * the trigger so it stays inside the viewport. */
@media (max-width: 640px) {
  .cts-nav {
    padding: 0 var(--space-3);
    gap: var(--space-2);
  }
  .cts-nav .cts-account-menu {
    /* Anchor a few px in from the right edge so the popover never
     * brushes the viewport border on small phones. */
    right: calc(var(--space-3) * -1 + var(--space-2));
  }
}

@media (prefers-reduced-motion: reduce) {
  .cts-nav .cts-account-menu,
  .cts-nav .cts-avatar,
  .cts-nav .cts-account-trigger,
  .cts-nav .cts-navlinks,
  .cts-nav .cts-menu-toggle .cts-menu-toggle-bar {
    transition: none;
  }
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
 * on the left, link buttons centered, single avatar trigger on the right
 * that opens an account popover (matches GitHub/Linear/Slack conventions).
 *
 * Responsive behavior (the navbar stays a single 60px row at every width):
 * - ≤ 1023px: tighten gaps and edge padding.
 * - ≤ 820px:  drop the "CONFORMANCE SUITE" wordmark; reveal a hamburger
 *             toggle that pulls the link row out of the chrome and into
 *             a vertical popover panel pinned below the navbar.
 * - ≤ 640px:  tighten edge padding further; the account menu nudges in
 *             so it does not brush the viewport edge on small phones.
 *
 * Account zone:
 * - Authenticated → 30×30 avatar button → popover with name, principal,
 *   ADMIN chip (also surfaced as a rust ring on the avatar), Tokens link
 *   for non-admin/non-guest users, and a Sign out form button.
 * - Unauthenticated → "Sign in" button linking to /login.html.
 * - Loading → skeleton avatar circle (no horizontal text reservation).
 * @property {string} currentPage - Key of the active page (e.g. `home`,
 *   `plans`, `logs`, `tokens`, `api-docs`); used to highlight the matching
 *   link. Reflects the `current-page` attribute.
 */
class CtsNavbar extends LitElement {
  static properties = {
    currentPage: { type: String, attribute: "current-page" },
    _user: { state: true },
    _loading: { state: true },
    _menuOpen: { state: true },
    _mobileMenuOpen: { state: true },
  };

  constructor() {
    super();
    this.currentPage = "";
    this._user = null;
    this._loading = true;
    this._menuOpen = false;
    this._mobileMenuOpen = false;
    this._onDocPointerDown = this._onDocPointerDown.bind(this);
    this._onDocKeydown = this._onDocKeydown.bind(this);
  }

  // Light DOM so the global tokens (--ink-900 etc.) and existing page
  // selectors continue to reach inside the component.
  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    // Outside-click + Escape close the account menu. Bound on the document
    // (not the host) because pointerdown lands wherever the user actually
    // clicked — the host wouldn't see clicks elsewhere on the page.
    document.addEventListener("pointerdown", this._onDocPointerDown);
    document.addEventListener("keydown", this._onDocKeydown);
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

  disconnectedCallback() {
    document.removeEventListener("pointerdown", this._onDocPointerDown);
    document.removeEventListener("keydown", this._onDocKeydown);
    super.disconnectedCallback();
  }

  /** @param {PointerEvent} e - Document-level pointerdown used to close open menus on outside click. */
  _onDocPointerDown(e) {
    const target = /** @type {Node | null} */ (e.target);
    if (this._menuOpen) {
      const account = this.querySelector(".cts-account");
      if (account && target && !account.contains(target)) {
        this._menuOpen = false;
      }
    }
    if (this._mobileMenuOpen) {
      const navlinks = this.querySelector(".cts-navlinks");
      const toggle = this.querySelector(".cts-menu-toggle");
      const insideMenu = navlinks && target && navlinks.contains(target);
      const insideToggle = toggle && target && toggle.contains(target);
      if (!insideMenu && !insideToggle) {
        this._mobileMenuOpen = false;
      }
    }
  }

  /** @param {KeyboardEvent} e - Document-level keydown; Escape closes open menus and restores focus to the trigger. */
  _onDocKeydown(e) {
    if (e.key !== "Escape") return;
    if (this._menuOpen) {
      this._menuOpen = false;
      // Return focus to the trigger so keyboard users land back where
      // they invoked the menu, matching native <details>/popover behavior.
      const trigger = /** @type {HTMLButtonElement | null} */ (
        this.querySelector(".cts-account-trigger")
      );
      trigger?.focus();
      return;
    }
    if (this._mobileMenuOpen) {
      this._mobileMenuOpen = false;
      const toggle = /** @type {HTMLButtonElement | null} */ (
        this.querySelector(".cts-menu-toggle")
      );
      toggle?.focus();
    }
  }

  _toggleMenu() {
    this._menuOpen = !this._menuOpen;
  }

  _toggleMobileMenu() {
    this._mobileMenuOpen = !this._mobileMenuOpen;
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
              "cts-navlink-external": link.external === true,
              active: this.currentPage === link.page,
            })}
            href="${link.href}"
            target=${link.external ? "_blank" : "_self"}
            rel=${link.external ? "noopener noreferrer" : ""}
            >${link.label}${link.external
              ? html`<cts-icon name="external-link" size="16"></cts-icon>`
              : ""}</a
          >
        </li>
      `,
    );
  }

  _renderAccount() {
    if (this._loading) {
      // Skeleton circle reserves the avatar's 30x30 box without flashing
      // text. Aria-hidden because the loading state is purely visual —
      // screen readers shouldn't announce a placeholder.
      return html`<span class="cts-skel-avatar" aria-hidden="true"></span>`;
    }
    if (!this._user) {
      return html`<a class="cts-nav-action" href="login.html">Sign in</a>`;
    }

    const initials = computeInitials(this._user.displayName);
    const isAdmin = this._user.isAdmin === true;
    const showTokens = !isAdmin && this._user.isGuest !== true;
    const open = this._menuOpen;

    return html`
      <div class="cts-account" data-open=${open ? "true" : "false"}>
        <button
          type="button"
          class="cts-account-trigger"
          aria-haspopup="true"
          aria-expanded=${open ? "true" : "false"}
          aria-controls="cts-account-menu"
          aria-label=${`Account menu for ${this._user.displayName}`}
          @click=${this._toggleMenu}
        >
          <span class=${classMap({ "cts-avatar": true, "is-admin": isAdmin })} aria-hidden="true"
            >${initials}</span
          >
        </button>
        <div id="cts-account-menu" class="cts-account-menu" role="menu" aria-label="Account">
          <header class="cts-account-header">
            <span class="cts-account-name">
              ${this._user.displayName}
              ${when(isAdmin, () => html`<span class="cts-nav-admin-badge">ADMIN</span>`)}
            </span>
            <span class="cts-account-principal">${this._user.principal}</span>
          </header>
          <div class="cts-account-divider" role="separator"></div>
          ${when(
            showTokens,
            () => html`<a class="cts-account-item" href="tokens.html" role="menuitem">Tokens</a>`,
          )}
          <form action="/logout" method="post" class="cts-account-form">
            <button type="submit" class="cts-account-item cts-account-item--danger" role="menuitem">
              Sign out
            </button>
          </form>
        </div>
      </div>
    `;
  }

  render() {
    const mobileOpen = this._mobileMenuOpen;
    return html`
      <nav class="cts-nav" data-mobile-open=${mobileOpen ? "true" : "false"}>
        <button
          type="button"
          class="cts-menu-toggle"
          aria-haspopup="true"
          aria-expanded=${mobileOpen ? "true" : "false"}
          aria-controls="cts-navlinks"
          aria-label=${mobileOpen ? "Close navigation menu" : "Open navigation menu"}
          @click=${this._toggleMobileMenu}
        >
          <svg viewBox="0 0 20 20" width="20" height="20" aria-hidden="true">
            <line
              class="cts-menu-toggle-bar cts-menu-toggle-bar--top"
              x1="3"
              y1="6"
              x2="17"
              y2="6"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            />
            <line
              class="cts-menu-toggle-bar cts-menu-toggle-bar--mid"
              x1="3"
              y1="10"
              x2="17"
              y2="10"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            />
            <line
              class="cts-menu-toggle-bar cts-menu-toggle-bar--bot"
              x1="3"
              y1="14"
              x2="17"
              y2="14"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            />
          </svg>
        </button>
        <a class="cts-brand navbar-brand" href="index.html">
          <img src="/images/openid-dark.svg" alt="OpenID Foundation" width="93" height="28" />
          <span class="cts-brand-name">CONFORMANCE SUITE</span>
        </a>
        <ul class="cts-navlinks navbar-nav" id="cts-navlinks">
          ${this._renderNavLinks()}
        </ul>
        <div class="cts-navright">${this._renderAccount()}</div>
      </nav>
    `;
  }
}

customElements.define("cts-navbar", CtsNavbar);
