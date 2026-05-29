import { LitElement, html, nothing } from "lit";
import "./cts-link-button.js";
import "./cts-tooltip.js";
import "./cts-icon.js";

const STYLE_ID = "cts-view-tabs-styles";

const STYLE_TEXT = `
.cts-view-tabs {
  display: flex;
  gap: var(--space-1);
  margin-bottom: var(--space-4);
  border-bottom: 1px solid var(--border);
}
/* Direct-child combinator (> a), NOT a descendant selector: the My/Published
   tabs are the nav's own anchors, while the opt-in "Schedule test" CTA renders a
   nested <a class="oidf-btn"> (inside cts-link-button) that is a grandchild.
   A bare ".cts-view-tabs a" would outspecify ".oidf-btn" (0,1,1 vs 0,1,0) and
   leak the tab styling (grey text, 2px bottom border, -1px margin) onto the
   button. Keep this scoped to > a. */
.cts-view-tabs > a {
  display: inline-flex;
  align-items: center;
  padding: var(--space-2) var(--space-3);
  font-family: var(--font-sans);
  font-size: var(--fs-14);
  font-weight: var(--fw-medium);
  line-height: var(--lh-snug);
  color: var(--fg-soft);
  text-decoration: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: color var(--dur-1) var(--ease-standard),
              border-color var(--dur-1) var(--ease-standard);
}
.cts-view-tabs > a:hover {
  color: var(--fg);
}
.cts-view-tabs > a:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
  border-radius: var(--radius-1);
}
.cts-view-tabs > a[aria-current="page"] {
  color: var(--fg);
  border-bottom-color: var(--orange-400);
}
/* Opt-in "Schedule test" CTA (R11), rendered at the end of the tabs row in
   every state whenever a page sets create-test-href. Pushed to the right edge
   (margin-left:auto) and vertically centered so it sits in the tab row without
   inheriting the anchors' bottom-border overlap. */
.cts-view-tabs .cts-view-tabs-cta {
  margin-left: auto;
  align-self: center;
}
/* Opt-in Published help affordance (R22): a circled-question-mark icon sitting
   immediately after the Published anchor, revealing a descriptor tooltip on
   hover/focus. The cts-icon itself is the focusable trigger (tabindex=0) so the
   tooltip is keyboard-reachable; align-self centers it in the tab row. Mirrors
   the established cts-plan-modules help-icon colour + focus-ring treatment. */
.cts-view-tabs .cts-view-tabs-help-icon {
  align-self: center;
  color: var(--fg-faint);
  cursor: help;
}
.cts-view-tabs .cts-view-tabs-help-icon:hover {
  color: var(--fg);
}
.cts-view-tabs .cts-view-tabs-help-icon:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
  border-radius: var(--radius-1);
}
`;

/**
 * Inject the scoped stylesheet for `cts-view-tabs` into `<head>` once. The
 * `STYLE_ID` flag makes this a no-op on subsequent component mounts so
 * multiple instances on the same page do not duplicate the rules. Mirrors
 * the head-style injection pattern used by `cts-footer`.
 */
function ensureStylesInjected() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * URL-driven **navigation** control for the My / Published dataset split,
 * consumed by both `plans.html` and `logs.html`.
 *
 * This is deliberately a `<nav>` landmark with anchor links and
 * `aria-current="page"` on the active view — NOT an ARIA tablist
 * (`role="tab"`/`role="tablist"`). My vs Published changes the dataset that is
 * fetched and must be URL-addressable (deep-link, back/forward, the
 * `?public=true` alias), which is a navigation concern, not in-page content
 * switching (KTD1). The existing `cts-tabs` is a content-switching widget with
 * no URL state and is the wrong primitive here.
 *
 * An optional "Schedule test" CTA (opt-in via `create-test-href`, see that
 * property) renders at the end of the row in every state (My, Published, and
 * anonymous) as a persistent entry point to start a test (R11). It is hosted
 * inside this `<nav>` by deliberate choice: a "Schedule test" link navigates to
 * another page, so it is navigation, and the visual placement at the end of the
 * tabs row is the product intent. The accessible name stays "Dataset view".
 *
 * The canonical "Published" signal is `?public=true`; "My" is the absence of
 * the `public` param (KTD2). The control merely reads/writes that param:
 * clicking a tab `pushState`s the new URL (so each switch is a back/forward
 * step) and emits `cts-view-tab-change`. The host `<nav aria-label="Dataset
 * view">` is uniquely labelled so screen-reader landmark navigation
 * distinguishes it from the main `cts-navbar` nav (WCAG 2.4.1).
 *
 * popstate ownership: because `pushState` changes the URL without re-mounting
 * the component, a `popstate` listener re-derives the active tab from
 * `location.search`, re-renders, AND re-emits `cts-view-tab-change` so the page
 * (which hydrates from the URL only once at connect) re-fetches the matching
 * dataset on back/forward. Without this, back/forward would change the address
 * bar but not the dataset (R5).
 *
 * On initial connect the component derives + renders the active state from the
 * URL but does NOT emit `cts-view-tab-change` — the page already issues the
 * initial fetch via its auth-gated inline path, so emitting here would
 * double-fetch.
 *
 * Light DOM (`createRenderRoot` returns `this`). Scoped CSS is injected into
 * `<head>` once on first connect. The `:not(:defined)` block-height
 * reservation lives in `css/layout.css` (KTD6).
 * @property {boolean} authenticated - When false (the anon-safe default), the
 *   My anchor is NOT rendered — anonymous visitors only see Published. Reflects
 *   the `authenticated` attribute. Set authoritatively by the page once
 *   `/api/currentuser` resolves.
 * @property {string} createTestHref - Opt-in destination for a "Schedule test"
 *   CTA rendered at the end of the tabs row (R11). When set, the CTA appears in
 *   every state — on the My and Published views, and for anonymous visitors —
 *   so it is a persistent entry point to start a test (an anonymous click lands
 *   on the server-auth-gated schedule page). Pages that should not offer test
 *   scheduling (e.g. `logs.html`) simply leave it unset, so the shared control
 *   stays page-neutral. Reflects the `create-test-href` attribute.
 * @property {string} publishedHelp - Opt-in descriptor text for the Published
 *   view (R22). When set, a circled-question-mark help icon renders immediately
 *   after the Published anchor; hovering or keyboard-focusing it reveals a
 *   tooltip carrying this text, and the same text is the icon's `aria-label`
 *   (the only screen-reader channel, since `cts-tooltip` has no
 *   `aria-describedby`). The copy differs per page (published plans vs.
 *   published logs), so each page sets its own; pages that want no help affordance
 *   leave it unset. Reflects the `published-help` attribute.
 * @fires cts-view-tab-change - When the active view changes (click or
 *   back/forward), with `{ detail: { view, isPublic } }` where `view` is
 *   `"my"` | `"published"`; bubbles and is composed.
 */
class CtsViewTabs extends LitElement {
  static properties = {
    authenticated: { type: Boolean, attribute: "authenticated" },
    createTestHref: { type: String, attribute: "create-test-href" },
    publishedHelp: { type: String, attribute: "published-help" },
  };

  constructor() {
    super();
    // Anon-safe default: never flash the My tab to an anonymous visitor. The
    // page sets this to true once /api/currentuser confirms a session.
    this.authenticated = false;
    // Empty by default: the CTA is opt-in per page (only plans.html sets it).
    this.createTestHref = "";
    // Empty by default: the Published help affordance is opt-in per page.
    this.publishedHelp = "";
    this._handlePopState = this._handlePopState.bind(this);
  }

  createRenderRoot() {
    ensureStylesInjected();
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    // Re-render on back/forward and re-emit so the page re-fetches the
    // dataset matching the restored URL — connectedCallback-only hydration
    // does not satisfy the back/forward requirement (R5).
    window.addEventListener("popstate", this._handlePopState);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    window.removeEventListener("popstate", this._handlePopState);
  }

  /**
   * The currently-active view, derived from auth + the URL. Anonymous visitors
   * are always on Published; otherwise the canonical Published signal is
   * `?public=true` (an exact `=== 'true'` match), and its absence means My.
   * @returns {"my" | "published"} The active view.
   */
  get _activeView() {
    if (!this.authenticated) return "published";
    return new URLSearchParams(location.search).get("public") === "true" ? "published" : "my";
  }

  /**
   * Re-render and re-emit when the user navigates back/forward. The active
   * view is re-derived from the new URL inside render()/`_activeView`, so this
   * only needs to request an update and dispatch the change for the page.
   * @returns {void}
   */
  _handlePopState() {
    this.requestUpdate();
    this._emitChange();
  }

  /**
   * Dispatch `cts-view-tab-change` carrying the current active view so the
   * page can set/clear `is-public` and re-fetch.
   * @returns {void}
   */
  _emitChange() {
    const view = this._activeView;
    this.dispatchEvent(
      new CustomEvent("cts-view-tab-change", {
        bubbles: true,
        composed: true,
        detail: { view, isPublic: view === "published" },
      }),
    );
  }

  /**
   * Click handler for both anchors. Prevents the default navigation, writes
   * the target view into the URL via `pushState` (preserving `location.hash`
   * and any non-`public` params), re-renders so `aria-current` updates, then
   * emits `cts-view-tab-change`.
   * @param {MouseEvent} event - The anchor click event.
   * @returns {void}
   */
  _handleTabClick(event) {
    // Let the browser handle modifier-key / non-primary clicks natively so
    // "open in new tab/window" and middle-click work on the anchors.
    const me = /** @type {MouseEvent} */ (event);
    if (me.metaKey || me.ctrlKey || me.shiftKey || me.altKey || me.button !== 0) {
      return;
    }
    event.preventDefault();
    const view = /** @type {HTMLElement} */ (event.currentTarget).dataset.view;
    const params = new URLSearchParams(location.search);
    if (view === "published") {
      params.set("public", "true");
    } else {
      params.delete("public");
    }
    const query = params.toString();
    const newUrl = location.pathname + (query ? `?${query}` : "") + location.hash;
    history.pushState(null, "", newUrl);
    this.requestUpdate();
    this._emitChange();
  }

  /**
   * Href for a view's anchor, computed from `location.pathname` so the control
   * is page-agnostic (reused by plans.html and logs.html). Used for graceful
   * degradation and middle-click; the click handler preventDefaults the
   * in-page navigation.
   * @param {"my" | "published"} view - The view the anchor targets.
   * @returns {string} The href for that view.
   */
  _hrefFor(view) {
    return view === "published" ? `${location.pathname}?public=true` : location.pathname;
  }

  render() {
    const active = this._activeView;
    // Inactive anchors carry aria-current="false" (a valid token) rather than
    // omitting the attribute, mirroring cts-log-toc. Only the active anchor
    // matches the `[aria-current='page']` selector the URL-compat gate asserts.
    return html`
      <nav class="cts-view-tabs" aria-label="Dataset view">
        ${this.authenticated
          ? html`<a
              data-view="my"
              href="${this._hrefFor("my")}"
              aria-current="${active === "my" ? "page" : "false"}"
              @click=${this._handleTabClick}
              >My</a
            >`
          : nothing}
        <a
          data-view="published"
          href="${this._hrefFor("published")}"
          aria-current="${active === "published" ? "page" : "false"}"
          @click=${this._handleTabClick}
          >Published</a
        >
        ${this.publishedHelp
          ? html`<cts-tooltip content="${this.publishedHelp}" placement="bottom"
              ><cts-icon
                name="circle-help"
                size="16"
                class="cts-view-tabs-help-icon"
                tabindex="0"
                aria-label="${this.publishedHelp}"
                data-testid="published-help"
              ></cts-icon
            ></cts-tooltip>`
          : nothing}
        ${this.createTestHref
          ? html`<cts-link-button
              class="cts-view-tabs-cta"
              variant="primary"
              size="sm"
              icon="add-plus"
              href="${this.createTestHref}"
              label="Schedule test"
              data-testid="schedule-test-cta"
            ></cts-link-button>`
          : nothing}
      </nav>
    `;
  }
}

customElements.define("cts-view-tabs", CtsViewTabs);

export {};
