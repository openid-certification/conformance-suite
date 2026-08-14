import { LitElement, html, nothing, css } from "lit";
import "./cts-link-button.js";
import "./cts-alert.js";
import "./cts-icon.js";

const STYLE_ID = "cts-login-page-styles";

// Scoped styles for the login page. Two-pane layout at >=860px (warm-ink
// brand band + white form panel inside one rounded shell), single column
// below. Wrapper centres the shell on a `--bg-muted` page with quiet warm
// glow accents pulled from the OIDF orange/sand ramps. Typography, spacing,
// radii, shadows, motion and palette all come from `oidf-tokens.css` — no
// Bootstrap leakage.
const STYLE_TEXT = css`
  .oidf-login-page {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: calc(100vh - 60px);
    padding: var(--space-8) var(--space-4);
    background:
      radial-gradient(1200px 600px at 8% -10%, var(--sand-50), transparent 60%),
      radial-gradient(900px 500px at 110% 110%, var(--orange-50), transparent 55%), var(--bg-muted);
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
    .oidf-login-card {
      animation: none;
    }
  }
  @keyframes oidf-login-rise {
    from {
      opacity: 0;
      transform: translateY(8px);
    }
    to {
      opacity: 1;
      transform: none;
    }
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
      linear-gradient(180deg, rgba(235, 139, 53, 0.14) 0%, transparent 55%), var(--bg-ink);
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
    top: -4px;
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
    font-weight: var(--fw-bold);
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
  .oidf-login-brand__pillars cts-icon {
    color: var(--orange-300);
    flex: 0 0 auto;
  }
  .oidf-login-brand__footer {
    margin-top: auto;
    padding-top: var(--space-4);
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.6);
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
    font-weight: var(--fw-bold);
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
  /* The sign-in anchor renders as a plain <a> rather than a cts-link-button,
   so it needs the design-system full-width treatment that [full-width]
   gives to cts-link-button, plus an inline-flex layout to centre the
   label on the button. */
  .oidf-login-providers > a.oidf-btn {
    width: 100%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-2);
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
    text-decoration-line: none;
    transition:
      background var(--dur-1) var(--ease-standard),
      border-color var(--dur-1) var(--ease-standard);
  }
  .oidf-login-link:hover {
    background: var(--ink-50);
    border-color: var(--border-strong);
    color: var(--ink-900);
    text-decoration-line: none;
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
    display: contents;
    color: var(--fg-soft);
    font-size: var(--fs-14);
    transition:
      transform var(--dur-1) var(--ease-standard),
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
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/**
 * Login/register landing page. Offers a single sign-in button that hands off to
 * the OpenID Provider brokering the upstream accounts, plus rich-list links to
 * the public logs and plans listings.
 *
 * Layout is a single rounded shell containing two panels: a warm-ink brand
 * band (OIDF wordmark + capability pillars) on the left and a focused form
 * panel on the right at viewports >=860px, collapsing to a single column on
 * narrow screens. The error banner uses `cts-alert variant="danger"`; the
 * post-logout banner uses `cts-alert variant="info"`. The sign-in button renders
 * as a plain `<a class="oidf-btn oidf-btn-secondary oidf-btn-lg">` anchor with a
 * `cts-icon` in the leading-icon slot. It is a plain anchor rather than a
 * `cts-link-button` because it is a full-page navigation to the IdP that must
 * work before any component has upgraded. The `oidf-btn` styles arrive via the
 * `cts-link-button.js` import which injects them at module load. All
 * other styling comes from `oidf-tokens.css` plus the scoped
 * `.oidf-login-*` rules injected on first mount.
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
      <div class="oidf-login-brand__footer"> Operated by the OpenID Foundation </div>
    </aside>`;
  }

  render() {
    return html`
      <main class="oidf-login-page" id="loginContent">
        <section class="oidf-login-card">
          ${this._renderBrand()}
          <div class="oidf-login-form">
            <header class="oidf-login-form__head">
              <h1 class="oidf-login-title"> Sign in to continue </h1>
              <p class="oidf-login-subtitle">
                Sign in with your OpenID Foundation account. New users are registered automatically
                on first sign in.
              </p>
            </header>
            ${this._renderError()}${this._renderLogout()}
            <div class="oidf-login-providers">
              <a
                class="oidf-btn oidf-btn-secondary oidf-btn-lg"
                href="/oauth2/authorization/idp"
                role="button"
              >
                <cts-icon name="user-01" aria-hidden="true"></cts-icon>
                Sign in with OpenID
              </a>
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
