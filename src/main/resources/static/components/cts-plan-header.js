import { LitElement, html, nothing } from "lit";

const STYLE_ID = "cts-plan-header-styles";

// Scoped CSS for the plan-detail page header. Mirrors the design archive's
// `.page-head` composition (`project/ui_kits/certification-suite/app.css`):
// title in `.t-h1`, sub-meta as a row of mono key/value pairs, optional
// admin/certification rows, and a soft "summary" callout. The token-based
// label/value grid replaces the prior Bootstrap col-md-1/col-md-11 layout.
const STYLE_TEXT = `
  cts-plan-header {
    display: block;
  }
  cts-plan-header .planHeadCard {
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: var(--radius-3);
    padding: var(--space-5);
    margin-bottom: var(--space-4);
  }
  cts-plan-header .planTitle {
    margin: 0 0 var(--space-2);
  }
  cts-plan-header .planMeta {
    display: grid;
    grid-template-columns: max-content 1fr;
    column-gap: var(--space-4);
    row-gap: var(--space-2);
    align-items: baseline;
    margin-top: var(--space-3);
  }
  cts-plan-header .planMeta dt {
    font-size: var(--fs-12);
    font-weight: var(--fw-bold);
    color: var(--fg-soft);
    text-transform: uppercase;
    letter-spacing: 0.06em;
    margin: 0;
  }
  cts-plan-header .planMeta dd {
    font-size: var(--fs-13);
    color: var(--fg);
    margin: 0;
    word-break: break-word;
  }
  cts-plan-header .planMeta dd code,
  cts-plan-header .planMeta .mono {
    font-family: var(--font-mono);
    font-size: var(--fs-12);
    color: var(--fg);
    background: var(--ink-50);
    padding: 1px 6px;
    border-radius: var(--radius-1);
  }
  cts-plan-header .planSummary {
    margin-top: var(--space-4);
    padding: var(--space-3) var(--space-4);
    background: var(--status-info-bg);
    color: var(--ink-900);
    border-left: 3px solid var(--status-info);
    border-radius: var(--radius-2);
    font-size: var(--fs-13);
  }
  cts-plan-header .planDisclaimer {
    margin-top: var(--space-4);
    padding-top: var(--space-3);
    border-top: 1px solid var(--divider);
    font-size: var(--fs-12);
    color: var(--fg-soft);
    line-height: var(--lh-base);
  }
  cts-plan-header .planDisclaimer a {
    color: var(--fg-link);
  }
`;

function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Renders the header card for a plan-detail page: plan name, variant, ID,
 * description, version, start time, owner (admin only), and certification
 * profile.
 *
 * Light DOM. Scoped CSS is injected once on first connect; the title uses
 * the `.t-h1` typography utility from the OIDF token sheet, and the
 * meta-pairs render as a `<dl>` styled with the token grid.
 *
 * @property {object} plan - Plan object from `/api/plan/{id}`; expects
 *   `_id`, `planName`, `variant`, `description`, `version`, `started`,
 *   `owner`, `certificationProfileName`, `summary`.
 * @property {boolean} isAdmin - Reveals the Test Owner row. Reflects the
 *   `is-admin` attribute.
 * @property {boolean} isPublic - Hides owner row on public views. Reflects
 *   the `is-public` attribute.
 */
class CtsPlanHeader extends LitElement {
  static properties = {
    plan: { type: Object },
    isAdmin: { type: Boolean, attribute: "is-admin" },
    isPublic: { type: Boolean, attribute: "is-public" },
  };

  constructor() {
    super();
    this.plan = {};
    this.isAdmin = false;
    this.isPublic = false;
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  _formatVariant(variant) {
    if (!variant) return "";
    if (typeof variant === "string") return variant;
    return Object.entries(variant)
      .map(([key, value]) => `${key}=${value}`)
      .join(", ");
  }

  _formatDate(dateStr) {
    if (!dateStr) return "";
    return new Date(dateStr).toLocaleString();
  }

  _formatCertificationProfile(name) {
    if (!name) return "";
    return Array.isArray(name) ? name.join(", ") : name;
  }

  render() {
    const plan = this.plan;
    if (!plan || !plan._id) return nothing;

    const variantText = this._formatVariant(plan.variant);
    const startedText = this._formatDate(plan.started);
    const showOwner = !this.isPublic && this.isAdmin && plan.owner;
    const ownerText = showOwner
      ? `${plan.owner.sub}${plan.owner.iss ? ` (${plan.owner.iss})` : ""}`
      : "";

    return html`
      <div class="planHeadCard" id="planHeader">
        <h1 class="planTitle t-h1">${plan.planName}</h1>
        <dl class="planMeta">
          <dt>Variant:</dt>
          <dd><span class="mono">${variantText}</span></dd>

          <dt>Plan ID:</dt>
          <dd><span class="mono">${plan._id}</span></dd>

          ${plan.description
            ? html`
                <dt data-testid="description-row">Description:</dt>
                <dd>${plan.description}</dd>
              `
            : nothing}

          <dt>Plan Version:</dt>
          <dd><span class="mono">${plan.version}</span></dd>

          <dt>Started:</dt>
          <dd>${startedText}</dd>

          ${showOwner
            ? html`
                <dt data-testid="owner-row">Test Owner:</dt>
                <dd>${ownerText}</dd>
              `
            : nothing}
          ${plan.certificationProfileName
            ? html`
                <dt data-testid="certification-row">Certification profile:</dt>
                <dd>${this._formatCertificationProfile(plan.certificationProfileName)}</dd>
              `
            : nothing}
        </dl>

        ${plan.summary ? html`<div class="planSummary">${plan.summary}</div>` : nothing}

        <div class="planDisclaimer">
          These test results were generated by the OpenID Foundation conformance suite. By
          themselves, they are not proof that a deployment is conformant nor that it meets the
          requirements for certification. For a list of certified deployments, see
          <a href="https://openid.net/certification/">https://openid.net/certification/</a> - to be
          added to this list follow
          <a href="https://openid.net/certification/instructions/"
            >the certification instructions.</a
          >
        </div>
      </div>
    `;
  }
}

customElements.define("cts-plan-header", CtsPlanHeader);

export {};
