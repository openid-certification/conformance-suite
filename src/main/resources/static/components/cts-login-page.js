import { LitElement, html, nothing } from "lit";
import "./cts-link-button.js";
import "./cts-alert.js";
import "./cts-icon.js";

// Brand-mark SVGs for the social-login buttons. These are NOT part of the
// coolicons set — coolicons does not ship brand glyphs. Inlining the marks
// here keeps the brand assets colocated with their only consumer (per the
// 2026-04-25 brand-icon carve-out decision in plan 2026-04-25-004 U6) and
// avoids a parallel "brand registry" component to maintain.
//
// Google: official multicolour G mark from Google Identity Branding
// Guidelines (https://developers.google.com/identity/branding-guidelines).
// Multicolour fills are baked in — the mark must NOT inherit currentColor,
// per Google's brand requirements.
const GOOGLE_BRAND_SVG = html`<svg
  class="oidf-login-brand-mark"
  xmlns="http://www.w3.org/2000/svg"
  viewBox="0 0 24 24"
  data-brand="google"
  aria-hidden="true"
>
  <path
    fill="#4285F4"
    d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
  ></path>
  <path
    fill="#34A853"
    d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
  ></path>
  <path
    fill="#FBBC05"
    d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
  ></path>
  <path
    fill="#EA4335"
    d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
  ></path>
</svg>`;

// GitLab: tanuki silhouette from GitLab Press Kit
// (https://about.gitlab.com/press/press-kit/). currentColor lets the mark
// follow the button text colour through hover/focus states, matching the
// rest of the design system.
const GITLAB_BRAND_SVG = html`<svg
  class="oidf-login-brand-mark"
  xmlns="http://www.w3.org/2000/svg"
  viewBox="0 0 24 24"
  data-brand="gitlab"
  fill="currentColor"
  aria-hidden="true"
>
  <path
    d="m23.6004 9.5927-.0337-.0862L20.3.9814a.851.851 0 0 0-.3362-.405.875.875 0 0 0-.9997.0539.875.875 0 0 0-.29.4399l-2.2055 6.748H7.5375l-2.2057-6.748a.8573.8573 0 0 0-.29-.4412.8748.8748 0 0 0-.9997-.0537.8585.8585 0 0 0-.3362.4049L.4332 9.5015l-.0335.0875a6.0959 6.0959 0 0 0 2.0218 7.0432l.0113.0083.03.0224 5.0008 3.7462 2.4744 1.8732 1.5071 1.1389a1.0098 1.0098 0 0 0 1.2197 0l1.5071-1.1389 2.4744-1.8732 5.0307-3.7686.0125-.0098a6.0959 6.0959 0 0 0 2.021-7.042z"
  ></path>
</svg>`;

const STYLE_ID = "cts-login-page-styles";

// Scoped styles for the login page. Two-pane layout at >=860px (warm-ink
// brand band + white form panel inside one rounded shell), single column
// below. Wrapper centres the shell on a `--bg-muted` page with quiet warm
// glow accents pulled from the OIDF orange/sand ramps. Typography, spacing,
// radii, shadows, motion and palette all come from `oidf-tokens.css` — no
// Bootstrap leakage.
const STYLE_TEXT = `
.oidf-login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 60px);
  padding: var(--space-8) var(--space-4);
  background:
    radial-gradient(1200px 600px at 8% -10%, var(--sand-50), transparent 60%),
    radial-gradient(900px 500px at 110% 110%, var(--orange-50), transparent 55%),
    var(--bg-muted);
}

.oidf-login-card {
  width: 100%;
  max-width: 960px;
  display: grid;
  grid-template-columns: 1fr;
  background: var(--bg-elev);
  border: 1px solid var(--border);
  border-radius: var(--radius-4);
  box-shadow: var(--shadow-3);
  overflow: hidden;
  animation: oidf-login-rise var(--dur-3) var(--ease-standard) both;
}
@media (prefers-reduced-motion: reduce) {
  .oidf-login-card { animation: none; }
}
@keyframes oidf-login-rise {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: none; }
}
@media (min-width: 860px) {
  .oidf-login-card {
    grid-template-columns: 5fr 7fr;
  }
}

/* ----- Brand panel ----- */
.oidf-login-brand {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  padding: var(--space-8) var(--space-6);
  color: var(--ink-0);
  background:
    linear-gradient(180deg, rgba(235, 139, 53, 0.14) 0%, transparent 55%),
    var(--bg-ink);
}
@media (min-width: 860px) {
  .oidf-login-brand {
    padding: var(--space-10) var(--space-8);
  }
}
.oidf-login-brand__head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.oidf-login-brand__logo {
  display: block;
  height: 28px;
  width: auto;
  position: relative;
  top: -3px;
}
.oidf-login-brand__eyebrow {
  display: inline-block;
  font-size: var(--fs-12);
  font-weight: var(--fw-bold);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--orange-200);
}
.oidf-login-brand__headline {
  margin: 0;
  font-family: var(--font-display);
  font-weight: var(--fw-black);
  font-size: var(--fs-20);
  line-height: var(--lh-snug);
  letter-spacing: -0.01em;
  color: var(--ink-0);
}
@media (min-width: 860px) {
  .oidf-login-brand__headline {
    font-size: var(--fs-24);
  }
}
.oidf-login-brand__pillars {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  color: rgba(255, 255, 255, 0.82);
  font-size: var(--fs-14);
  line-height: var(--lh-snug);
}
.oidf-login-brand__pillars li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.oidf-login-brand__pillars .bi {
  color: var(--orange-300);
  flex: 0 0 auto;
}
.oidf-login-brand__footer {
  margin-top: auto;
  padding-top: var(--space-4);
  border-top: 1px solid rgba(255, 255, 255, 0.10);
  color: rgba(255, 255, 255, 0.60);
  font-size: var(--fs-12);
}

/* ----- Form panel ----- */
.oidf-login-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  padding: var(--space-8) var(--space-6);
}
@media (min-width: 860px) {
  .oidf-login-form {
    padding: var(--space-10) var(--space-8);
  }
}
.oidf-login-form__head {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.oidf-login-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-24);
  font-weight: var(--fw-black);
  line-height: var(--lh-tight);
  letter-spacing: -0.01em;
  color: var(--ink-900);
}
.oidf-login-subtitle {
  margin: 0;
  font-size: var(--fs-14);
  line-height: var(--lh-base);
  color: var(--fg-muted);
}

.oidf-login-providers {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
/* Brand-button anchors render as plain <a> rather than cts-link-button
   (so the inline brand SVG can replace the icon slot). They still need
   the design-system full-width treatment that [full-width] gives to
   cts-link-button, plus an inline-flex layout so the brand mark and
   label centre on the button. */
.oidf-login-providers > a.oidf-btn {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
}

/* Brand-mark SVGs sit inside the social-login anchors. They share the
   button's text-color via currentColor for the GitLab tanuki silhouette,
   while the Google G keeps its official multicolor palette. Sized to
   1em so they track the surrounding label font-size, matching the
   button height the design system gives them. */
.oidf-login-page .oidf-login-brand-mark {
  display: inline-block;
  width: 1em;
  height: 1em;
  vertical-align: -0.125em;
  flex-shrink: 0;
}

/* Labelled divider between primary and secondary actions */
.oidf-login-divider {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: var(--space-3);
  margin: var(--space-1) 0;
  color: var(--fg-soft);
  font-size: var(--fs-12);
  font-weight: var(--fw-bold);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.oidf-login-divider::before,
.oidf-login-divider::after {
  content: "";
  height: 1px;
  background: var(--border);
}

/* Public-resource rich list */
.oidf-login-secondary {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.oidf-login-link {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-3);
  background: var(--bg-elev);
  color: var(--ink-900);
  text-decoration: none;
  transition: background var(--dur-1) var(--ease-standard),
              border-color var(--dur-1) var(--ease-standard);
}
.oidf-login-link:hover {
  background: var(--ink-50);
  border-color: var(--border-strong);
  color: var(--ink-900);
  text-decoration: none;
}
.oidf-login-link:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
}
.oidf-login-link__icon {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-2);
  background: var(--sand-50);
  border: 1px solid var(--sand-200);
  color: var(--orange-600);
  font-size: var(--fs-16);
}
.oidf-login-link__body {
  display: flex;
  flex-direction: column;
  line-height: var(--lh-snug);
  min-width: 0;
}
.oidf-login-link__title {
  font-weight: var(--fw-bold);
  font-size: var(--fs-14);
  color: var(--ink-900);
}
.oidf-login-link__meta {
  font-size: var(--fs-12);
  color: var(--fg-muted);
}
.oidf-login-link__chevron {
  color: var(--fg-soft);
  font-size: var(--fs-14);
  transition: transform var(--dur-1) var(--ease-standard),
              color var(--dur-1) var(--ease-standard);
}
.oidf-login-link:hover .oidf-login-link__chevron {
  transform: translateX(2px);
  color: var(--ink-900);
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
 * Login/register landing page. Offers Google/GitLab OAuth buttons (each with
 * its vendor mark) and rich-list links to the public logs and plans listings.
 *
 * Layout is a single rounded shell containing two panels: a warm-ink brand
 * band (OIDF wordmark + capability pillars) on the left and a focused form
 * panel on the right at viewports >=860px, collapsing to a single column on
 * narrow screens. The error banner uses `cts-alert variant="danger"`; the
 * post-logout banner uses `cts-alert variant="info"`. Provider buttons render
 * via `cts-link-button` (`variant="secondary"` per U23's design-system
 * mapping) and pick up Bootstrap-Icons vendor glyphs through the `icon` prop
 * (`google`, `gitlab`). All styling comes from `oidf-tokens.css` plus the
 * scoped `.oidf-login-*` rules injected on first mount.
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

  _renderBrand() {
    return html`<aside class="oidf-login-brand" aria-label="OpenID Foundation Conformance Suite">
      <div class="oidf-login-brand__head">
        <img
          class="oidf-login-brand__logo"
          src="/images/openid-dark.svg"
          alt="OpenID Foundation"
          width="93"
          height="28"
        />
        <span class="oidf-login-brand__eyebrow">Conformance Suite</span>
      </div>
      <h2 class="oidf-login-brand__headline">
        Certification testing for the OpenID family of specifications.
      </h2>
      <ul class="oidf-login-brand__pillars">
        <li>
          <cts-icon name="shield-check" aria-hidden="true"></cts-icon>
          OpenID Connect, FAPI 1 &amp; FAPI 2
        </li>
        <li>
          <cts-icon name="shield-check" aria-hidden="true"></cts-icon>
          Identity Assurance &amp; OpenID Federation
        </li>
        <li>
          <cts-icon name="shield-check" aria-hidden="true"></cts-icon>
          Verifiable Credentials &amp; Presentations
        </li>
        <li>
          <cts-icon name="shield-check" aria-hidden="true"></cts-icon>
          CIBA, SSF &amp; AuthZEN
        </li>
      </ul>
      <div class="oidf-login-brand__footer">Operated by the OpenID Foundation</div>
    </aside>`;
  }

  render() {
    return html`
      <main class="oidf-login-page" id="loginContent">
        <section class="oidf-login-card">
          ${this._renderBrand()}
          <div class="oidf-login-form">
            <header class="oidf-login-form__head">
              <h1 class="oidf-login-title">Sign in to continue</h1>
              <p class="oidf-login-subtitle">
                Use your Google or GitLab account. New users are registered automatically on first
                sign in.
              </p>
            </header>
            ${this._renderError()}${this._renderLogout()}
            <div class="oidf-login-providers">
              <a
                class="oidf-btn oidf-btn-secondary oidf-btn-lg"
                href="/oauth2/authorization/google"
                role="button"
                >${GOOGLE_BRAND_SVG} Proceed with Google</a
              >
              <a
                class="oidf-btn oidf-btn-secondary oidf-btn-lg"
                href="/oauth2/authorization/gitlab"
                role="button"
                >${GITLAB_BRAND_SVG} Proceed with GitLab</a
              >
            </div>
            <div class="oidf-login-divider" role="separator">
              <span>Or browse without signing in</span>
            </div>
            <nav class="oidf-login-secondary" aria-label="Public resources">
              <a class="oidf-login-link" href="logs.html?public=true">
                <span class="oidf-login-link__icon">
                  <cts-icon name="files" aria-hidden="true"></cts-icon>
                </span>
                <span class="oidf-login-link__body">
                  <span class="oidf-login-link__title">View published logs</span>
                  <span class="oidf-login-link__meta">Browse historical certification runs</span>
                </span>
                <span class="oidf-login-link__chevron">
                  <cts-icon name="arrow-right-md" aria-hidden="true"></cts-icon>
                </span>
              </a>
              <a class="oidf-login-link" href="plans.html?public=true">
                <span class="oidf-login-link__icon">
                  <cts-icon name="bookmark" aria-hidden="true"></cts-icon>
                </span>
                <span class="oidf-login-link__body">
                  <span class="oidf-login-link__title">View published plans</span>
                  <span class="oidf-login-link__meta">Explore current and past test plans</span>
                </span>
                <span class="oidf-login-link__chevron">
                  <cts-icon name="arrow-right-md" aria-hidden="true"></cts-icon>
                </span>
              </a>
            </nav>
          </div>
        </section>
        ${this._renderTokenIframe()}
      </main>
    `;
  }
}

customElements.define("cts-login-page", CtsLoginPage);

export {};
