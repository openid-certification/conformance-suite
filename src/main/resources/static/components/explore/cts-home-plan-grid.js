import { LitElement, html, nothing } from "lit";
import "../cts-link-button.js";

const RELATIVE_TIME = new Intl.RelativeTimeFormat("en", { numeric: "auto" });

function formatRelativeTime(isoString) {
  if (!isoString) return "";
  const diffMs = Date.now() - new Date(isoString).getTime();
  const diffSec = Math.round(diffMs / 1000);
  const diffMin = Math.round(diffSec / 60);
  const diffHour = Math.round(diffMin / 60);
  const diffDay = Math.round(diffHour / 24);
  if (Math.abs(diffDay) >= 1) return RELATIVE_TIME.format(-diffDay, "day");
  if (Math.abs(diffHour) >= 1) return RELATIVE_TIME.format(-diffHour, "hour");
  if (Math.abs(diffMin) >= 1) return RELATIVE_TIME.format(-diffMin, "minute");
  return RELATIVE_TIME.format(-diffSec, "second");
}

function formatVariant(variant) {
  if (!variant || typeof variant !== "object") return "";
  return Object.entries(variant)
    .map(([key, value]) => `${key}: ${value}`)
    .join(", ");
}

function resultCounts(modules) {
  const counts = { passed: 0, failed: 0, warning: 0, notRun: 0 };
  if (!Array.isArray(modules)) return counts;
  for (const mod of modules) {
    switch (mod.result) {
      case "PASSED":
        counts.passed += 1;
        break;
      case "FAILED":
        counts.failed += 1;
        break;
      case "WARNING":
        counts.warning += 1;
        break;
      default:
        counts.notRun += 1;
    }
  }
  return counts;
}

function countsLabel(counts) {
  const parts = [];
  if (counts.passed > 0) parts.push(`${counts.passed} passed`);
  if (counts.failed > 0) parts.push(`${counts.failed} failed`);
  if (counts.warning > 0) parts.push(`${counts.warning} warning`);
  if (counts.notRun > 0) parts.push(`${counts.notRun} not started`);
  return parts.join(" \u00B7 ");
}

/**
 * <cts-home-plan-grid> — Exploration prototype for the home page.
 *
 * Renders the current user's in-progress test plans as a CSS Grid of cards
 * above the existing home-page link column. Backed by the real /api/plan
 * endpoint (reads the PaginationResponse `data` envelope) with no invented
 * fields; module counts are derived from `plan.modules[].result`.
 *
 * Target-today surface: src/main/resources/static/components/cts-dashboard.js.
 * Workshop UX references: UX-04 (primary); UX-01, UX-05 (incidentally).
 *
 * @property {Array} plans - Plans rendered as cards. Hydrated from /api/plan
 *   in connectedCallback; may be preset externally for testing.
 * @property {Object} serverInfo - Server build info rendered in the footer.
 *   Hydrated from /api/server in connectedCallback.
 */
class CtsHomePlanGrid extends LitElement {
  static properties = {
    plans: { type: Array },
    serverInfo: { type: Object },
    _loading: { state: true },
    _isAuthenticated: { state: true },
  };

  constructor() {
    super();
    this.plans = [];
    this.serverInfo = null;
    this._loading = true;
    this._isAuthenticated = true;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    this._hydrate();
  }

  async _hydrate() {
    this._loading = true;
    const [plansResponse, userResponse, serverResponse] = await Promise.allSettled([
      fetch("/api/plan"),
      fetch("/api/currentuser"),
      fetch("/api/server"),
    ]);

    if (plansResponse.status === "fulfilled" && plansResponse.value.ok) {
      try {
        const body = await plansResponse.value.json();
        this.plans = Array.isArray(body?.data) ? body.data : [];
      } catch {
        this.plans = [];
      }
    }

    if (userResponse.status === "fulfilled") {
      this._isAuthenticated = userResponse.value.ok;
    } else {
      this._isAuthenticated = false;
    }

    if (serverResponse.status === "fulfilled" && serverResponse.value.ok) {
      try {
        this.serverInfo = await serverResponse.value.json();
      } catch {
        this.serverInfo = null;
      }
    }

    this._loading = false;
  }

  _renderSegmentedBar(counts, total) {
    if (total <= 0) return nothing;
    const pct = (n) => `${(n / total) * 100}%`;
    return html`<div class="cts-hpg-bar" role="presentation">
      <span class="cts-hpg-bar-pass" style="width: ${pct(counts.passed)}"></span>
      <span class="cts-hpg-bar-fail" style="width: ${pct(counts.failed)}"></span>
      <span class="cts-hpg-bar-warn" style="width: ${pct(counts.warning)}"></span>
      <span class="cts-hpg-bar-notrun" style="width: ${pct(counts.notRun)}"></span>
    </div>`;
  }

  _renderCard(plan) {
    const counts = resultCounts(plan.modules);
    const total = Array.isArray(plan.modules) ? plan.modules.length : 0;
    const variantSubtitle = formatVariant(plan.variant);
    const label = countsLabel(counts);

    return html`<a class="cts-hpg-card" href="plan-detail.html?plan=${plan._id}">
      <h3 class="cts-hpg-card-title">${plan.planName}</h3>
      ${variantSubtitle
        ? html`<p class="cts-hpg-card-variant">${variantSubtitle}</p>`
        : nothing}
      ${this._renderSegmentedBar(counts, total)}
      ${label
        ? html`<p class="cts-hpg-card-counts">${label}</p>`
        : nothing}
      <p class="cts-hpg-card-started">
        ${formatRelativeTime(plan.started)}
      </p>
    </a>`;
  }

  _renderEmptyState() {
    return html`<div class="cts-hpg-empty">
      <h2 class="cts-hpg-empty-title">No test plans yet.</h2>
      <p class="cts-hpg-empty-body">
        Create one to begin conformance testing.
      </p>
      <cts-link-button
        href="schedule-test.html"
        variant="primary"
        icon="files"
        label="Create a test plan"
      ></cts-link-button>
    </div>`;
  }

  _renderGrid() {
    if (this._loading) {
      return html`<div class="cts-hpg-loading" aria-live="polite">
        Loading test plans…
      </div>`;
    }
    if (!Array.isArray(this.plans) || this.plans.length === 0) {
      return this._renderEmptyState();
    }
    return html`<div class="cts-hpg-grid">
      ${this.plans.map((plan) => this._renderCard(plan))}
    </div>`;
  }

  _renderLinkColumn() {
    return html`<div class="cts-hpg-links">
      <div class="d-grid gap-0">
        ${this._isAuthenticated
          ? html`
              <cts-link-button
                href="schedule-test.html"
                variant="info"
                icon="files"
                label="Create a new test plan"
              ></cts-link-button>
              <br />
              <cts-link-button
                href="logs.html"
                variant="info"
                icon="files"
                label="View my test logs"
              ></cts-link-button>
              <br />
              <cts-link-button
                href="plans.html"
                variant="info"
                icon="bookmarks"
                label="View my test plans"
              ></cts-link-button>
              <br />
            `
          : nothing}
        <cts-link-button
          href="logs.html?public=true"
          variant="info"
          icon="files"
          label="View all published test logs"
        ></cts-link-button>
        <br />
        <cts-link-button
          href="plans.html?public=true"
          variant="info"
          icon="bookmarks"
          label="View all published test plans"
        ></cts-link-button>
        <br />
        <cts-link-button
          href="api-document.html"
          variant="info"
          icon="bookmarks"
          label="View API Documentation"
        ></cts-link-button>
      </div>
    </div>`;
  }

  _renderServerFooter() {
    if (!this.serverInfo) return nothing;
    const { version, revision, tag, build_time, external_ip } = this.serverInfo;
    const parts = [];
    if (external_ip) parts.push(html`External IP: <span>${external_ip}</span>`);
    if (version) parts.push(html`Version: <span>${version}</span>`);
    if (revision) parts.push(html`Revision: <span>${revision}</span>`);
    if (tag) parts.push(html`Tag: <span>${tag}</span>`);
    if (build_time) parts.push(html`Build Time: <span>${build_time}</span>`);
    if (parts.length === 0) return nothing;
    return html`<div class="serverInfo">
      ${parts.map((part, i) => html`${i > 0 ? " | " : ""}${part}`)}
    </div>`;
  }

  render() {
    return html`
      <style>
        .cts-home-plan-grid {
          display: flex;
          flex-direction: column;
          gap: var(--cts-space-8);
          padding: var(--cts-space-6) var(--cts-space-4);
          font-family: var(--cts-font-sans);
          color: var(--cts-text);
        }
        .cts-hpg-title {
          font-size: var(--cts-text-2xl);
          font-weight: var(--cts-weight-semibold);
          letter-spacing: var(--cts-tracking-tight);
          margin: 0;
        }
        .cts-hpg-loading {
          color: var(--cts-text-secondary);
          font-size: var(--cts-text-sm);
        }
        .cts-hpg-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
          gap: var(--cts-space-4);
        }
        @media (max-width: 600px) {
          .cts-hpg-grid {
            grid-template-columns: 1fr;
          }
        }
        .cts-hpg-card {
          display: flex;
          flex-direction: column;
          gap: var(--cts-space-2);
          background: var(--cts-comp-card-bg);
          border: 1px solid var(--cts-comp-card-border);
          border-radius: var(--cts-comp-card-radius);
          box-shadow: var(--cts-comp-card-shadow);
          padding: var(--cts-comp-card-padding);
          text-decoration: none;
          color: inherit;
          transition:
            box-shadow var(--cts-duration-fast) var(--cts-ease-default),
            transform var(--cts-duration-fast) var(--cts-ease-default);
        }
        .cts-hpg-card:hover {
          box-shadow: var(--cts-shadow-md);
          transform: translateY(-1px);
        }
        .cts-hpg-card:focus-visible {
          outline: none;
          box-shadow: var(--cts-shadow-ring);
        }
        .cts-hpg-card-title {
          font-size: var(--cts-text-md);
          font-weight: var(--cts-weight-semibold);
          margin: 0;
          word-break: break-word;
        }
        .cts-hpg-card-variant {
          font-size: var(--cts-text-xs);
          color: var(--cts-text-secondary);
          margin: 0;
          font-family: var(--cts-font-mono);
        }
        .cts-hpg-bar {
          display: flex;
          width: 100%;
          height: 8px;
          border-radius: var(--cts-radius-sm);
          overflow: hidden;
          background: var(--cts-color-gray-100);
        }
        .cts-hpg-bar-pass   { background: var(--cts-result-pass); }
        .cts-hpg-bar-fail   { background: var(--cts-result-fail); }
        .cts-hpg-bar-warn   { background: var(--cts-result-warning); }
        .cts-hpg-bar-notrun { background: var(--cts-color-gray-400); }
        .cts-hpg-card-counts {
          font-size: var(--cts-text-xs);
          color: var(--cts-text);
          margin: 0;
        }
        .cts-hpg-card-started {
          font-size: var(--cts-text-xs);
          color: var(--cts-text-tertiary);
          margin: 0;
        }
        .cts-hpg-empty {
          display: flex;
          flex-direction: column;
          gap: var(--cts-space-3);
          align-items: flex-start;
          padding: var(--cts-space-6);
          background: var(--cts-bg-sunken);
          border-radius: var(--cts-radius-lg);
        }
        .cts-hpg-empty-title {
          font-size: var(--cts-text-lg);
          font-weight: var(--cts-weight-semibold);
          margin: 0;
        }
        .cts-hpg-empty-body {
          font-size: var(--cts-text-sm);
          color: var(--cts-text-secondary);
          margin: 0;
        }
        .cts-hpg-links {
          max-width: 400px;
        }
      </style>
      <div class="cts-home-plan-grid" id="homePage">
        <h1 class="cts-hpg-title">Your test plans</h1>
        ${this._renderGrid()}
        ${this._renderLinkColumn()}
      </div>
      <footer class="pageFooter">
        <span class="muted">OpenID Foundation conformance suite</span>
        ${this._renderServerFooter()}
      </footer>
    `;
  }
}

customElements.define("cts-home-plan-grid", CtsHomePlanGrid);
