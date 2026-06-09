import { html } from "lit";
import { expect, within, waitFor } from "storybook/test";
import { MOCK_PLAN_DETAIL, MOCK_MODULES_WITH_STATUS } from "@fixtures/mock-test-data.js";

import "../../../src/main/resources/static/components/cts-crumb.js";
import "../../../src/main/resources/static/components/cts-plan-header.js";
import "../../../src/main/resources/static/components/cts-plan-modules.js";
import "../../../src/main/resources/static/components/cts-plan-status.js";
import "../../../src/main/resources/static/components/cts-plan-actions.js";

// Recreates `plan-detail.html` for design review without requiring a live
// backend. Mirrors the page chrome (breadcrumb above a content grid whose
// 240px right rail holds the actions panel, with the page-level padding
// and max-width from plan-detail.html's inline <style>) so the visual
// diff matches what a real user sees on the app page.
//
// Intentionally omitted, mirroring the running-test page story:
// - navbar + skip-link — both depend on /api/currentuser and add chrome
//   that's not under review here;
// - cts-footer — page-level chrome below the composition under review;
// - crumb navigation — cts-crumb-navigate fires on click but no handler
//   is wired here (the window.location.assign wiring belongs to
//   plan-detail.html), so clicking "Plans" is a visual no-op.

export default {
  title: "Pages/PlanDetail",
};

// Copied from plan-detail.html's inline <style> — keep in sync with the
// page. Storybook's preview-head.html already loads the shared
// stylesheets (oidf-tokens.css, layout.css, oidf-app.css); the page's
// inline rules are the only CSS a page story must supply itself.
const PAGE_STYLES = html`
  <style>
    .oidf-plan-detail-page {
      padding: var(--space-5) var(--space-6);
      max-width: 1320px;
      margin: 0 auto;
      font-family: var(--font-sans);
    }
    .oidf-plan-detail-crumb {
      margin-bottom: var(--space-3);
    }
    .oidf-plan-detail-grid {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 240px;
      gap: var(--space-5);
      align-items: start;
    }
    @media (max-width: 900px) {
      .oidf-plan-detail-grid {
        grid-template-columns: 1fr;
      }
    }
    cts-plan-status:not(:defined) {
      display: block;
      min-height: 38px;
    }
  </style>
`;

const PLAN_WITH_MODULE_STATUS = {
  ...MOCK_PLAN_DETAIL,
  modules: MOCK_MODULES_WITH_STATUS,
};

// The page's /api/info fan-out marks each module `_statusResolved` once its
// status settles; the static story mirrors that resolved state so the segment
// bar colours rather than pulsing. Same order as MOCK_MODULES_WITH_STATUS, so a
// segment index maps to the same module row.
const MODULES_RESOLVED = MOCK_MODULES_WITH_STATUS.map((m) => ({ ...m, _statusResolved: true }));

// Mirrors the "Plans > <plan name>" trail plan-detail.html builds from
// the fetched plan; static here because the story's data is static too.
const CRUMB_ITEMS = [
  { label: "Plans", target: "/plans.html" },
  { label: PLAN_WITH_MODULE_STATUS.planName, target: "" },
];

/**
 * Assert the page grid composes the 240px actions rail beside the
 * content column — the contract that rotted when the story's Bootstrap
 * wrappers went inert (the old markup computed no grid at all, stacking
 * the actions panel full-width below the header).
 */
function expectRailLayout(canvasElement) {
  const grid = canvasElement.querySelector(".oidf-plan-detail-grid");
  expect(grid).toBeTruthy();
  const tracks = getComputedStyle(grid).gridTemplateColumns.trim().split(/\s+/);
  expect(tracks).toHaveLength(2);
  expect(tracks[1]).toBe("240px");
}

export const Default = {
  // Pinned to the desktop preset (1280px — comfortably above the grid's
  // 900px stacking media query) so the rail-layout assertion below is
  // deterministic rather than dependent on the runner's canvas width.
  parameters: {
    viewport: { defaultViewport: "desktop" },
  },
  globals: {
    viewport: { value: "desktop", isRotated: false },
  },
  render: () => html`
    ${PAGE_STYLES}
    <div class="oidf-plan-detail-page">
      <cts-crumb class="oidf-plan-detail-crumb" .items=${CRUMB_ITEMS}></cts-crumb>
      <div class="oidf-plan-detail-grid">
        <div>
          <cts-plan-header .plan=${PLAN_WITH_MODULE_STATUS}></cts-plan-header>
          <cts-plan-status mode="detail" .modules=${MODULES_RESOLVED}></cts-plan-status>
          <cts-plan-modules
            .modules=${MOCK_MODULES_WITH_STATUS}
            plan-id="plan-abc-123"
          ></cts-plan-modules>
        </div>
        <cts-plan-actions .plan=${PLAN_WITH_MODULE_STATUS}></cts-plan-actions>
      </div>
    </div>
  `,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    // Header region: plan name and ID. Scoped to cts-plan-header — the
    // crumb also carries the plan name now, so an unscoped query would
    // be satisfied by the crumb alone even if the header rendered
    // nothing.
    await waitFor(() => {
      const header = canvasElement.querySelector("cts-plan-header");
      expect(header?.textContent).toContain("oidcc-basic-certification-test-plan");
    });
    expect(canvas.getAllByText("plan-abc-123").length).toBeGreaterThanOrEqual(1);

    await step("breadcrumb mirrors the page's Plans → plan-name trail", async () => {
      const crumb = canvasElement.querySelector("cts-crumb");
      expect(crumb).toBeTruthy();
      expect(crumb.textContent).toContain("Plans");
      expect(crumb.textContent).toContain(PLAN_WITH_MODULE_STATUS.planName);
    });

    await step("content grid composes the 240px actions rail", async () => {
      expectRailLayout(canvasElement);
    });

    // Modules region: at least two module rows render
    expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    expect(canvas.getByText("oidcc-server-rotate-keys")).toBeInTheDocument();

    // Modules region: status badges present
    const moduleBadges = canvasElement
      .querySelector("cts-plan-modules")
      .querySelectorAll("cts-badge");
    expect(moduleBadges.length).toBe(MOCK_MODULES_WITH_STATUS.length);

    await step("the whole-plan status bar renders one segment per module (R8)", async () => {
      const status = canvasElement.querySelector('cts-plan-status[mode="detail"]');
      expect(status).toBeTruthy();
      const segments = status.querySelectorAll('[data-testid="plan-status-segment"]');
      expect(segments.length).toBe(MODULES_RESOLVED.length);
      // At desktop width the responsive switch yields the bar (segments shrink
      // to hairlines: flex:1 1 0; min-width:0). A getComputedStyle layout
      // assertion catches the bar collapsing if the page chrome stops applying
      // (page-story-rot guard).
      expect(getComputedStyle(segments[0]).minWidth).toBe("0px");
    });

    await step("activating a segment scrolls to + flashes the matching row (R11)", async () => {
      const status = canvasElement.querySelector('cts-plan-status[mode="detail"]');
      const modules = canvasElement.querySelector("cts-plan-modules");
      // Mirror the page wiring (plan-detail.html getPlan): activate → highlight.
      status.addEventListener("cts-plan-status-activate", (e) =>
        modules.highlightModule(e.detail.index),
      );
      const segments = status.querySelectorAll("button.cts-pst-seg");
      segments[1].click();
      const rows = modules.querySelectorAll(".module-row");
      await waitFor(() => {
        expect(rows[1].classList.contains("is-flash")).toBe(true);
      });
    });

    // Actions region: View configuration button present (always visible)
    const viewConfigBtn = canvasElement.querySelector('[data-testid="view-config-btn"]');
    expect(viewConfigBtn).toBeTruthy();

    // All three regions present in the same canvas
    expect(canvasElement.querySelector("cts-plan-header")).toBeTruthy();
    expect(canvasElement.querySelector("cts-plan-modules")).toBeTruthy();
    expect(canvasElement.querySelector("cts-plan-actions")).toBeTruthy();

    // Non-admin: Delete plan button is present (non-readonly + non-immutable),
    // Download all Logs button is NOT present (admin-only)
    expect(canvasElement.querySelector('[data-testid="delete-plan-btn"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="download-all-btn"]')).toBeNull();
  },
};

export const AdminView = {
  parameters: {
    viewport: { defaultViewport: "desktop" },
  },
  globals: {
    viewport: { value: "desktop", isRotated: false },
  },
  render: () => html`
    ${PAGE_STYLES}
    <div class="oidf-plan-detail-page">
      <cts-crumb class="oidf-plan-detail-crumb" .items=${CRUMB_ITEMS}></cts-crumb>
      <div class="oidf-plan-detail-grid">
        <div>
          <cts-plan-header .plan=${PLAN_WITH_MODULE_STATUS} is-admin></cts-plan-header>
          <cts-plan-status mode="detail" .modules=${MODULES_RESOLVED}></cts-plan-status>
          <cts-plan-modules
            .modules=${MOCK_MODULES_WITH_STATUS}
            plan-id="plan-abc-123"
          ></cts-plan-modules>
        </div>
        <cts-plan-actions .plan=${PLAN_WITH_MODULE_STATUS} is-admin></cts-plan-actions>
      </div>
    </div>
  `,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);

    // Header region: admin-only Owner row visible
    await waitFor(() => {
      const ownerRow = canvasElement.querySelector('[data-testid="owner-row"]');
      expect(ownerRow).toBeTruthy();
    });
    expect(canvas.getByText("Test Owner:")).toBeInTheDocument();

    await step("content grid composes the 240px actions rail", async () => {
      expectRailLayout(canvasElement);
    });

    // Actions region: admin-only buttons visible
    expect(canvasElement.querySelector('[data-testid="download-all-btn"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="delete-plan-btn"]')).toBeTruthy();

    // Publish summary/everything visible (admin + not published)
    expect(canvasElement.querySelector('[data-testid="publish-summary-btn"]')).toBeTruthy();
    expect(canvasElement.querySelector('[data-testid="publish-everything-btn"]')).toBeTruthy();
  },
};
