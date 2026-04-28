import { LitElement, html, nothing } from "lit";
import { repeat } from "lit/directives/repeat.js";
import { ifDefined } from "lit/directives/if-defined.js";
import "./cts-icon.js";

const SERVER_INFO_LABELS = {
  external_ip: "External IP",
  version: "Version",
  revision: "Revision",
  tag: "Tag",
  build_time: "Build Time",
};

/**
 * Tile descriptor used to drive the dashboard grid.
 * Each tile renders as a single anchor styled like the design system's
 * "stat" card: large display numeral replaced by an icon, label below.
 * `authOnly: true` hides the tile when `isAuthenticated` is false.
 * @typedef {object} DashboardTile
 * @property {string} key - Stable identifier for the `repeat` directive.
 * @property {string} href - Navigation target.
 * @property {string} icon - Bootstrap Icons name (without the `bi-` prefix).
 * @property {string} label - Visible tile label.
 * @property {boolean} [authOnly] - When true, only render for authenticated users.
 * @property {boolean} [external] - When true, the tile opens in a new tab and
 *   shows an external-link affordance + screen-reader hint. Use for tiles that
 *   navigate to a separate app surface (e.g. Swagger-UI) rather than another
 *   page of the conformance-suite UI.
 */

/** @type {DashboardTile[]} */
const TILES = [
  {
    key: "schedule",
    href: "schedule-test.html",
    icon: "file-add",
    label: "Create a new test plan",
    authOnly: true,
  },
  {
    key: "my-logs",
    href: "logs.html",
    icon: "book",
    label: "View my test logs",
    authOnly: true,
  },
  {
    key: "my-plans",
    href: "plans.html",
    icon: "bookmark",
    label: "View my test plans",
    authOnly: true,
  },
  {
    key: "public-logs",
    href: "logs.html?public=true",
    icon: "book",
    label: "View all published test logs",
  },
  {
    key: "public-plans",
    href: "plans.html?public=true",
    icon: "bookmark",
    label: "View all published test plans",
  },
  {
    key: "api-docs",
    href: "api-document.html",
    icon: "book",
    label: "View API Documentation",
    external: true,
  },
];

const STYLE_ID = "cts-dashboard-styles";

const STYLE_TEXT = `
.oidf-dashboard {
  padding: var(--space-8) var(--space-6);
  max-width: var(--maxw-page);
  margin: 0 auto;
}
.oidf-dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}
.oidf-dashboard-tile {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  background: var(--bg-elev);
  border: 1px solid var(--border);
  border-radius: var(--radius-3);
  padding: var(--space-5) var(--space-5);
  color: var(--fg);
  text-decoration: none;
  box-shadow: var(--shadow-1);
  transition: border-color var(--dur-1) var(--ease-standard),
              box-shadow var(--dur-1) var(--ease-standard),
              transform var(--dur-1) var(--ease-standard);
}
.oidf-dashboard-tile:hover {
  border-color: var(--border-strong);
  box-shadow: var(--shadow-2);
  color: var(--fg);
  text-decoration: none;
  transform: translateY(-1px);
}
.oidf-dashboard-tile:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
}
.oidf-dashboard-tile-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--space-10);
  height: var(--space-10);
  border-radius: var(--radius-2);
  background: var(--orange-50);
  color: var(--orange-500);
  font-size: var(--fs-20);
  line-height: 1;
}
.oidf-dashboard-tile-label {
  font-family: var(--font-sans);
  font-weight: var(--fw-medium);
  font-size: var(--fs-15);
  line-height: var(--lh-snug);
  color: var(--fg);
}
.oidf-dashboard-tile-external {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--fg-soft);
  transition: color var(--dur-1) var(--ease-standard);
}
.oidf-dashboard-tile:hover .oidf-dashboard-tile-external,
.oidf-dashboard-tile:focus-visible .oidf-dashboard-tile-external {
  color: var(--orange-500);
}
.oidf-sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
.oidf-dashboard-footer {
  margin-top: var(--space-12);
  padding: var(--space-6) var(--space-6);
  border-top: 1px solid var(--border);
  text-align: center;
}
.oidf-dashboard-footer-brand {
  display: block;
  margin-bottom: var(--space-2);
}
.oidf-dashboard-footer .serverInfo {
  display: block;
}

@media (max-width: 1024px) {
  .oidf-dashboard-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 600px) {
  .oidf-dashboard-grid {
    grid-template-columns: 1fr;
  }
}
`;

/**
 * Inject the scoped stylesheet for `cts-dashboard` into `<head>` once. The
 * `STYLE_ID` flag makes this a no-op on subsequent component mounts so
 * multiple instances on the same page do not duplicate the rules.
 */
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Home-page dashboard. Renders a token-styled tile grid of primary-task
 * shortcuts and a footer with server info fetched from `/api/server`.
 * Layout matches the design archive's `.stats` block
 * (`grid-template-columns: repeat(4, 1fr)`); tile surface is the same
 * `oidf-card`-style chrome used elsewhere in the design system.
 * @property {boolean} isAuthenticated - Whether the current user is logged in;
 *   gates the authenticated-only tiles. Reflects the `is-authenticated`
 *   attribute.
 */
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
    injectStyles();
    this._fetchServerInfo();
  }

  async _fetchServerInfo() {
    this._loading = true;
    try {
      const response = await fetch("/api/server");
      if (response.ok) {
        /** @type {import('@cts-api/api-types').paths['/api/server']['get']['responses']['200']['content']['application/json']} */
        const data = await response.json();
        this._serverInfo = data;
      } else {
        // Server info is non-critical; tiles render regardless. Log so operators
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
    return html`<div> ${separated} </div>`;
  }

  _visibleTiles() {
    return TILES.filter((tile) => this.isAuthenticated || !tile.authOnly);
  }

  _renderTile(tile) {
    return html`
      <a
        class="oidf-dashboard-tile"
        href=${tile.href}
        target=${ifDefined(tile.external ? "_blank" : undefined)}
        rel=${ifDefined(tile.external ? "noopener noreferrer" : undefined)}
      >
        <span class="oidf-dashboard-tile-icon" aria-hidden="true">
          <cts-icon name="${tile.icon}"></cts-icon>
        </span>
        <span class="oidf-dashboard-tile-label">${tile.label}</span>
        ${tile.external
          ? html`<span class="oidf-dashboard-tile-external" aria-hidden="true">
                <cts-icon name="external-link" size="16"></cts-icon>
              </span>
              <span class="oidf-sr-only">(opens in a new tab)</span>`
          : nothing}
      </a>
    `;
  }

  render() {
    return html`
      <section class="oidf-dashboard" id="homePage">
        <div class="oidf-dashboard-grid">
          ${repeat(
            this._visibleTiles(),
            (tile) => tile.key,
            (tile) => this._renderTile(tile),
          )}
        </div>
      </section>

      <footer class="oidf-dashboard-footer pageFooter t-meta">
        <span class="oidf-dashboard-footer-brand">OpenID Foundation conformance suite</span>
        <div class="serverInfo">${this._renderServerInfo()}</div>
      </footer>
    `;
  }
}

customElements.define("cts-dashboard", CtsDashboard);

export {};
