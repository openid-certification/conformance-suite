import { LitElement, html, nothing, css } from "lit";
import { formatDescription } from "./format-description.js";
import "./cts-time.js";

const STYLE_ID = "cts-plan-header-styles";

// Scoped CSS for the plan-detail page header. Mirrors the design archive's
// `.page-head` composition (`project/ui_kits/certification-suite/app.css`):
// title in `.t-h1`, sub-meta as a row of mono key/value pairs, optional
// admin/certification rows, and a soft "summary" callout. The token-based
// label/value grid replaces the prior Bootstrap col-md-1/col-md-11 layout.
const STYLE_TEXT = css`
  cts-plan-header {
    display: flex;
    flex-direction: column;
    gap: 12px;
    /* Inline-size container so the metadata grid below can key its
       layout on the header's actual available width — correct under
       plan-detail.html's two-column desktop grid (where the header
       column is much narrower than the viewport) and in Storybook
       isolation. Named to avoid colliding with ctsLogViewer /
       ctsLogDrawer / planModulesCard / ctsRunningTestCard. Note:
       inline-size containment also makes the host the containing
       block for absolutely-positioned descendants — this component
       renders none, so the change is inert beyond the query. */
    container: ctsPlanHeader / inline-size;
  }
  cts-plan-header .planTitle {
    margin: 0 0 var(--space-2);
  }
  /* R9: the user-set plan description (freeform identifier they typed
     when scheduling) is the human-readable subject of this plan run.
     Surface it as a lede paragraph directly under the title so the
     reader's eye lands on it before the metadata grid. Suppressed
     entirely when the user did not set one — no empty subtitle slot. */
  cts-plan-header .planLede {
    margin: 0 0 var(--space-2);
    font-size: var(--fs-16);
    line-height: var(--lh-snug);
    color: var(--fg-soft);
    overflow-wrap: anywhere;
  }
  /* Metadata <dl>. Mobile-first: the default is a stacked single
     column (label above value) so values always get the header's
     full width — the legacy two-column grid's max-content label
     track ate ~164px of a ~312px content box at phone widths,
     squeezing every value into a ~132px sliver. The two-column
     layout is restored by the ≥640px container branch below
     (stack-up pattern, mirroring cts-log-detail-header /
     cts-running-test-card). Within a pair the label hugs its value
     (4px gap); pairs are separated by the dt's 12px margin-top so
     each label+value group reads as one block. The margin-top on
     the <dl> itself is unrelated to pair separation despite using
     the same token — it spaces the whole list from the title/lede
     above and holds in both layouts. */
  cts-plan-header .planMeta {
    display: grid;
    grid-template-columns: 1fr;
    gap: var(--space-1);
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
  cts-plan-header .planMeta dt:not(:first-child) {
    margin-top: var(--space-3);
  }
  cts-plan-header .planMeta dd {
    font-size: var(--fs-13);
    color: var(--fg);
    margin: 0;
    /* overflow-wrap (replacing the legacy word-break: break-word)
       so long unbreakable values (plan IDs, variant strings) wrap
       anywhere within their cell — parity with the sibling
       cts-log-detail-header .logMetaValue, which carries the same
       data classes (IDs, variants, mono chips). */
    overflow-wrap: anywhere;
  }
  /* Two-column label/value layout at ≥640px container width — the
     codebase's established "phone vs not" line (cts-log-entry,
     cts-log-detail-header). fit-content(180px) sizes the label
     track to the longest label (~164px today: "Certification
     profile:" uppercase with letter-spacing), clamped at 180px —
     unlike a fixed-max minmax(), which would always maximize to its
     max before the fr track received leftovers (track maximization
     runs before fr distribution). minmax(0, 1fr) drops the value
     track's implicit min-width: auto so long unbreakable values
     wrap (the cts-log-entry R31 idiom) instead of expanding the
     grid. */
  @container ctsPlanHeader (min-width: 640px) {
    cts-plan-header .planMeta {
      grid-template-columns: fit-content(180px) minmax(0, 1fr);
      gap: var(--space-2) var(--space-4);
      /* Only meaningful with two columns: baseline-aligns each
         label with the first line of its value. */
      align-items: baseline;
    }
    cts-plan-header .planMeta dt:not(:first-child) {
      margin-top: 0;
    }
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
    margin-bottom: var(--space-4);
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
  style.textContent = STYLE_TEXT.cssText;
  document.head.appendChild(style);
}

/**
 * Renders the header card for a plan-detail page: plan name, optional
 * description lede, variant, optional alias (from `config.alias`), ID,
 * version, start time, owner (admin only), and certification profile.
 *
 * Light DOM. Scoped CSS is injected once on first connect; the title uses
 * the `.t-h1` typography utility from the OIDF token sheet, the user-set
 * description renders as a prominent `.planLede` paragraph between the
 * title and the metadata grid (R9), and the meta-pairs render as a `<dl>`
 * styled with the token grid.
 *
 * @property {object} plan - Plan object from `/api/plan/{id}`; expects
 *   `_id`, `planName`, `variant`, `description`, `version`, `started`,
 *   `owner`, `certificationProfileName`, `summary`, and `config.alias`
 *   (the user-set, URL-safe alias). The alias lives inside the config
 *   object — it is never hoisted to a top-level `plan.alias` — and is
 *   absent on public views, whose projection omits `config` entirely, so
 *   the Alias row simply does not render there.
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

  _formatCertificationProfile(name) {
    if (!name) return "";
    return Array.isArray(name) ? name.join(", ") : name;
  }

  render() {
    const plan = this.plan;
    if (!plan || !plan._id) return nothing;

    const variantText = this._formatVariant(plan.variant);
    const showOwner = !this.isPublic && this.isAdmin && plan.owner;
    const ownerText = showOwner
      ? `${plan.owner.sub}${plan.owner.iss ? ` (${plan.owner.iss})` : ""}`
      : "";

    return html`
      <h1 class="planTitle t-h1">${plan.planName}</h1>
      ${plan.description
        ? html`<p class="planLede" data-testid="description-row">${plan.description}</p>`
        : nothing}
      <dl class="planMeta">
        <dt>Variant:</dt>
        <dd><span class="mono">${variantText}</span></dd>

        ${plan.config?.alias
          ? html`
              <dt data-testid="alias-row">Alias:</dt>
              <dd><span class="mono">${plan.config.alias}</span></dd>
            `
          : nothing}

        <dt>Plan ID:</dt>
        <dd><span class="mono">${plan._id}</span></dd>

        <dt>Plan Version:</dt>
        <dd><span class="mono">${plan.version}</span></dd>

        <dt>Started:</dt>
        <dd class="tabular-nums">
          <cts-time mode="absolute" value=${plan.started}></cts-time>
        </dd>

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

      ${plan.summary
        ? html`<div class="planSummary">${formatDescription(plan.summary)}</div>`
        : nothing}

      <div class="planDisclaimer">
        These test results were generated by the OpenID Foundation conformance suite. By themselves,
        they are not proof that a deployment is conformant nor that it meets the requirements for
        certification. For a list of certified deployments, see
        <a href="https://openid.net/certification/">https://openid.net/certification/</a> - to be
        added to this list follow
        <a href="https://openid.net/certification/instructions/">the certification instructions</a>.
      </div>
    `;
  }
}

customElements.define("cts-plan-header", CtsPlanHeader);

export {};
