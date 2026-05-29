import { LitElement, html, nothing } from "lit";

/**
 * Ordered map of `/api/server` response keys to their display labels. The
 * iteration order here is the render order in the footer, so the labels
 * appear as: External IP | Version | Revision | Tag | Build Time. Lifted
 * verbatim from `cts-dashboard` (retired in U11) so the server-info line is
 * preserved when the dashboard goes away (origin requirement R3).
 */
const SERVER_INFO_LABELS = {
  external_ip: "External IP",
  version: "Version",
  revision: "Revision",
  tag: "Tag",
  build_time: "Build Time",
};

const STYLE_ID = "cts-footer-styles";

const STYLE_TEXT = `
.oidf-footer {
  margin-top: var(--space-12);
  padding: var(--space-6) var(--space-6);
  border-top: 1px solid var(--border);
  text-align: center;
}
.oidf-footer-brand {
  display: block;
  margin-bottom: var(--space-2);
}
.oidf-footer .serverInfo {
  display: block;
}
`;

/**
 * Inject the scoped stylesheet for `cts-footer` into `<head>` once. The
 * `STYLE_ID` flag makes this a no-op on subsequent component mounts so
 * multiple instances on the same page do not duplicate the rules. Mirrors
 * the head-style injection pattern used by `cts-empty-state` / `cts-card`.
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Suite-wide page footer. Always renders the static "OpenID Foundation
 * conformance suite" brand line, and — when `/api/server` resolves — appends
 * the server-info line (version, revision, tag, build time, external IP).
 *
 * Fail-soft: a failed or empty `/api/server` response leaves only the static
 * brand line; the fetch error is logged via `console.warn` so operators can
 * diagnose a silently-failing endpoint, but it never throws and never blanks
 * the footer.
 *
 * Light DOM (`createRenderRoot` returns `this`) like the other `cts-*`
 * components, so the footer inherits the page's typographic tokens. Scoped
 * CSS is injected into `<head>` once on first connect. The `:not(:defined)`
 * block-height reservation lives in `css/layout.css` (KTD6) so the host
 * reserves ~56px before upgrade and the page does not shift (CLS) when the
 * component defines.
 *
 * The component exposes no public properties or attributes — it self-fetches
 * `/api/server` on connect — so its only reactive member is internal state.
 * @property {object|null} _serverInfo - Internal reactive state: the parsed
 *   `/api/server` response (version, revision, tag, build time, external IP),
 *   or `null` before the fetch resolves or after a fail-soft error. Drives the
 *   server-info line.
 * @fires nothing - The component emits no custom events.
 */
class CtsFooter extends LitElement {
  static properties = {
    _serverInfo: { state: true },
  };

  constructor() {
    super();
    /**
     * @type {import('@cts-api/api-types').paths['/api/server']['get']['responses']['200']['content']['application/json'] | null}
     */
    this._serverInfo = null;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    this._fetchServerInfo();
  }

  /**
   * Fetch the non-critical server-info payload. The brand line renders
   * regardless of the outcome, so any error is logged and swallowed rather
   * than surfaced — the footer must never throw or disappear.
   * @returns {Promise<void>} Resolves once the fetch settles (success or failure).
   */
  async _fetchServerInfo() {
    try {
      const response = await fetch("/api/server");
      if (response.ok) {
        this._serverInfo = await response.json();
      } else {
        console.warn(`[cts-footer] /api/server responded ${response.status}`);
      }
    } catch (err) {
      console.warn("[cts-footer] /api/server fetch failed:", err);
    }
  }

  /**
   * Build the server-info line from `_serverInfo`, filtered/ordered by
   * `SERVER_INFO_LABELS`. Returns `nothing` when no server info is available
   * or none of the known keys are present, so the footer collapses to the
   * static brand line on the fail-soft path.
   * @returns {import('lit').TemplateResult | typeof nothing} The server-info fragment, or `nothing`.
   */
  _renderServerInfo() {
    const serverInfo = this._serverInfo;
    if (!serverInfo) {
      return nothing;
    }
    const parts = Object.entries(SERVER_INFO_LABELS)
      .filter(([key]) => key in serverInfo)
      .map(
        ([key, label]) => html`${label}: <span id="serverinfo-${key}">${serverInfo[key]}</span>`,
      );
    if (parts.length === 0) {
      return nothing;
    }
    const separated = parts.flatMap((part, i) => (i > 0 ? [" | ", part] : [part]));
    return html`<div>${separated}</div>`;
  }

  render() {
    return html`
      <footer class="oidf-footer pageFooter t-meta">
        <span class="oidf-footer-brand">OpenID Foundation conformance suite</span>
        <div class="serverInfo">${this._renderServerInfo()}</div>
      </footer>
    `;
  }
}

customElements.define("cts-footer", CtsFooter);

export {};
