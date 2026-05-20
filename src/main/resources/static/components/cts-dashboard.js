import { LitElement, html, nothing } from "lit";
import { repeat } from "lit/directives/repeat.js";
import { ifDefined } from "lit/directives/if-defined.js";
import "./cts-icon.js";
import "./cts-stat.js";

const SERVER_INFO_LABELS = {
  external_ip: "External IP",
  version: "Version",
  revision: "Revision",
  tag: "Tag",
  build_time: "Build Time",
};

// Backend caps PaginationRequest.length at 1000 (see PaginationRequest.java).
// We fetch the max page so the count derived from the response array is
// accurate up to 1000; if recordsTotal exceeds data.length the tile renders
// "<count>+" to signal truncation.
const STATS_PAGE_SIZE = 1000;

// Placeholder shown while a stats fetch is in flight or when it fails.
// Em-dash matches the convention used elsewhere in the design system for
// "data unavailable" rather than "data is zero".
const STATS_PLACEHOLDER = "—";

/**
 * Tile descriptor for a single <cts-stat> entry in the dashboard stats row.
 * Each tile renders as an anchor wrapping a <cts-stat>; clicking navigates
 * to the filtered list view encoded in `href`.
 * @typedef {object} StatTile
 * @property {string} key - Stable identifier for the `repeat` directive.
 * @property {string} label - Overline label shown above the value.
 * @property {string} value - Display value (number string or placeholder).
 * @property {string} href - Navigation target for the wrapping anchor. The
 *   "in-progress" and "failures" tiles route through `?status=` / `?result=`
 *   query params consumed by logs.html.
 * @property {string} [delta] - Optional secondary line beneath the value.
 * @property {string} [tone] - "pass" / "fail" / unset (default).
 */

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
.oidf-dashboard-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}
.oidf-dashboard-stat-tile {
  display: block;
  background: var(--bg-elev);
  border: 1px solid var(--border);
  border-radius: var(--radius-3);
  padding: var(--space-5);
  box-shadow: var(--shadow-1);
  color: var(--fg);
  text-decoration: none;
  transition: border-color var(--dur-1) var(--ease-standard),
              box-shadow var(--dur-1) var(--ease-standard),
              transform var(--dur-1) var(--ease-standard);
}
.oidf-dashboard-stat-tile:hover {
  border-color: var(--border-strong);
  box-shadow: var(--shadow-2);
  color: var(--fg);
  text-decoration: none;
  transform: translateY(-1px);
}
.oidf-dashboard-stat-tile:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
}
.oidf-dashboard-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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
  .oidf-dashboard-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 600px) {
  .oidf-dashboard-grid {
    grid-template-columns: 1fr;
  }
  .oidf-dashboard-stats {
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
 * Layout uses a 3-column grid at desktop widths; tile surface is the same
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
    _stats: { state: true },
  };

  constructor() {
    super();
    this.isAuthenticated = true;
    this._serverInfo = null;
    this._loading = true;
    /** @type {StatTile[] | null} */
    this._stats = null;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    injectStyles();
    this._fetchServerInfo();
    if (this.isAuthenticated) {
      this._stats = this._placeholderStats();
      this._fetchStats();
    }
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

  /** @returns {StatTile[]} Initial em-dash tiles that reserve grid space while the fetch is in flight, preventing layout shift. */
  _placeholderStats() {
    return [
      { key: "plans", label: "Your test plans", value: STATS_PLACEHOLDER, href: "plans.html" },
      { key: "logs", label: "Your test logs", value: STATS_PLACEHOLDER, href: "logs.html" },
      {
        key: "in-progress",
        label: "Logs in progress",
        value: STATS_PLACEHOLDER,
        href: "logs.html?status=running,waiting",
      },
      {
        key: "failed",
        label: "Logs with failures",
        value: STATS_PLACEHOLDER,
        href: "logs.html?result=failed,unknown",
      },
    ];
  }

  async _fetchStats() {
    const [plansResult, logsResult] = await Promise.all([
      this._fetchListEndpoint(`/api/plan?start=0&length=${STATS_PAGE_SIZE}`),
      this._fetchListEndpoint(`/api/log?start=0&length=${STATS_PAGE_SIZE}`),
    ]);
    this._stats = this._buildStats(plansResult, logsResult);
  }

  /**
   * Fetch a paginated list endpoint and normalize the wire shape. The real
   * backend returns a PaginationResponse envelope (`{ data, recordsTotal }`);
   * MSW stories and some mocks return a plain array. Accept both — this is
   * the same dual-shape handling that cts-plan-list uses.
   * @param {string} url - Endpoint URL with start/length query params.
   * @returns {Promise<{failed: boolean, data?: object[], total?: number}>} Normalized result; `failed: true` means the tile shows the em-dash placeholder.
   */
  async _fetchListEndpoint(url) {
    try {
      const response = await fetch(url);
      if (!response.ok) {
        console.warn(`[cts-dashboard] ${url} responded ${response.status}`);
        return { failed: true };
      }
      const payload = await response.json();
      const data = Array.isArray(payload)
        ? payload
        : Array.isArray(payload?.data)
          ? payload.data
          : [];
      const total = typeof payload?.recordsTotal === "number" ? payload.recordsTotal : data.length;
      return { failed: false, data, total };
    } catch (err) {
      console.warn(`[cts-dashboard] ${url} fetch failed:`, err);
      return { failed: true };
    }
  }

  /**
   * Derive the four dashboard tile descriptors from the parallel-fetched
   * plan and log results. Each tile degrades to "—" independently when its
   * underlying fetch failed, so a transient `/api/log` outage does not
   * blank the plans count.
   * @param {{failed: boolean, data?: object[], total?: number}} plansResult - Normalized /api/plan response.
   * @param {{failed: boolean, data?: object[], total?: number}} logsResult - Normalized /api/log response.
   * @returns {StatTile[]} Four tile descriptors in display order: plans, logs, in-progress, failed.
   */
  _buildStats(plansResult, logsResult) {
    const plansTile = {
      key: "plans",
      label: "Your test plans",
      value: this._formatCount(plansResult),
      href: "plans.html",
    };
    const logsTile = {
      key: "logs",
      label: "Your test logs",
      value: this._formatCount(logsResult),
      href: "logs.html",
    };

    let inProgressTile;
    let failedTile;
    if (logsResult.failed) {
      inProgressTile = {
        key: "in-progress",
        label: "Logs in progress",
        value: STATS_PLACEHOLDER,
        href: "logs.html?status=running,waiting",
      };
      failedTile = {
        key: "failed",
        label: "Logs with failures",
        value: STATS_PLACEHOLDER,
        href: "logs.html?result=failed,unknown",
      };
    } else {
      const logs = logsResult.data || [];
      // Whitelist the actively-executing statuses. The negation-against-FINISHED
      // shape would also count INTERRUPTED (terminal: stopped before completion)
      // and the pre-execution NOT_YET_CREATED/CREATED/CONFIGURED states from
      // TestModule.Status, inflating the "in progress" tile. See
      // src/main/java/net/openid/conformance/testmodule/TestModule.java for the
      // 7-value enum.
      const inProgressCount = logs.filter(
        (log) => log.status === "RUNNING" || log.status === "WAITING",
      ).length;
      // Match LogApi.java's existing "failed" convention (LogApi.java:691-695):
      // both FAILED and UNKNOWN results count as failures in the certification
      // package builder, so the dashboard tile must do the same to avoid
      // hiding UNKNOWN failures from the user.
      const failedCount = logs.filter(
        (log) => log.result === "FAILED" || log.result === "UNKNOWN",
      ).length;
      inProgressTile = {
        key: "in-progress",
        label: "Logs in progress",
        value: String(inProgressCount),
        href: "logs.html?status=running,waiting",
      };
      failedTile = {
        key: "failed",
        label: "Logs with failures",
        value: String(failedCount),
        tone: failedCount > 0 ? "fail" : "pass",
        href: "logs.html?result=failed,unknown",
      };
    }

    return [plansTile, logsTile, inProgressTile, failedTile];
  }

  /**
   * Format the count value for a tile. Returns "—" when the fetch failed,
   * the raw count when the dataset fits in one page, and "<count>+" when
   * the backend reports more rows than this page contained OR the response
   * filled the page exactly (which is a truncation signal in pagination
   * semantics — we cannot tell whether row N+1 exists without another fetch).
   * @param {{failed: boolean, data?: object[], total?: number}} result - Normalized list-endpoint result.
   * @returns {string} Display string: numeric count, "<count>+" overflow indicator, or em-dash placeholder.
   */
  _formatCount(result) {
    if (result.failed || !result.data) return STATS_PLACEHOLDER;
    const count = result.data.length;
    const total = result.total ?? count;
    const truncated = total > count || count >= STATS_PAGE_SIZE;
    return truncated ? `${count}+` : String(count);
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

  _renderStats() {
    if (!this.isAuthenticated || !this._stats) return nothing;
    return html`
      <div class="oidf-dashboard-stats" id="dashboardStats">
        ${repeat(
          this._stats,
          (tile) => tile.key,
          (tile) => html`
            <a
              class="oidf-dashboard-stat-tile"
              data-stat-key="${tile.key}"
              href="${tile.href}"
              aria-label="${tile.label}: ${tile.value === STATS_PLACEHOLDER
                ? "loading"
                : tile.value}"
            >
              <cts-stat
                label="${tile.label}"
                value="${tile.value}"
                delta="${ifDefined(tile.delta)}"
                tone="${ifDefined(tile.tone)}"
              ></cts-stat>
            </a>
          `,
        )}
      </div>
    `;
  }

  render() {
    return html`
      <section class="oidf-dashboard" id="homePage">
        ${this._renderStats()}
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
