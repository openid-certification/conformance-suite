import { html } from "lit";
import { expect, waitFor } from "storybook/test";
import "./cts-plan-header.js";
import { formatAbsolute } from "../lib/time-format.js";

export default {
  title: "Components/cts-plan-header",
  component: "cts-plan-header",
};

const PLAN = {
  _id: "kVxZ3p9QwTabc",
  planName: "fapi2-security-profile-final-test-plan",
  description: "FAPI 2.0 Security Profile Final certification plan.",
  variant: { client_auth_type: "private_key_jwt", fapi_response_mode: "jarm" },
  version: "5.1.24",
  started: "2026-05-22T09:42:13.482Z",
  owner: { sub: "user-123", iss: "https://accounts.google.com" },
};

export const Default = {
  render: () => html`<cts-plan-header .plan=${PLAN}></cts-plan-header>`,

  async play({ canvasElement, step }) {
    const header = /** @type {HTMLElement} */ (canvasElement.querySelector("cts-plan-header"));

    await step("header renders the plan name", async () => {
      expect(header).toBeTruthy();
      expect(header.textContent).toContain(PLAN.planName);
    });

    await step("Started renders through cts-time with a hover title", async () => {
      // The Started value renders through cts-time: a native <time> whose title
      // carries the full absolute date for hover disambiguation.
      const timeEl = /** @type {HTMLTimeElement | null} */ (header.querySelector("dd time"));
      expect(timeEl).toBeTruthy();
      expect(timeEl?.textContent?.trim()).toBe(formatAbsolute(PLAN.started));
      expect(timeEl?.getAttribute("title")).toBe(formatAbsolute(PLAN.started));
      expect(timeEl?.getAttribute("datetime")).toBe(new Date(PLAN.started).toISOString());
    });
  },
};

export const AdminShowsOwner = {
  render: () => html`<cts-plan-header .plan=${PLAN} is-admin></cts-plan-header>`,

  async play({ canvasElement }) {
    const header = /** @type {HTMLElement} */ (canvasElement.querySelector("cts-plan-header"));
    expect(header.textContent).toContain(PLAN.owner.sub);
  },
};

export const MissingStarted = {
  // When the plan has no `started`, cts-time renders nothing — the Started
  // <dd> simply has no <time> child rather than showing an empty/invalid date.
  render: () => html`<cts-plan-header .plan=${{ ...PLAN, started: undefined }}></cts-plan-header>`,

  async play({ canvasElement }) {
    const header = /** @type {HTMLElement} */ (canvasElement.querySelector("cts-plan-header"));
    expect(header.querySelector("dd time")).toBeNull();
  },
};

// Plan: docs/plans/2026-05-27-001-feat-autolink-and-format-test-prose-plan.md
// (U3). The plan summary callout renders markdown (block parity with the
// log-detail hero): paragraphs, autolinked bare URLs, and inline code.
const PLAN_WITH_MARKDOWN_SUMMARY = {
  ...PLAN,
  summary:
    "First paragraph of the plan summary.\n\nSee https://openid.net/specs/x and the `request_uri` handling.",
};

export const SummaryRendersMarkdown = {
  render: () => html`<cts-plan-header .plan=${PLAN_WITH_MARKDOWN_SUMMARY}></cts-plan-header>`,

  async play({ canvasElement, step }) {
    const summary = /** @type {HTMLElement} */ (
      canvasElement.querySelector("cts-plan-header .planSummary")
    );
    expect(summary).toBeTruthy();

    await step("paragraph break splits into <p> blocks", async () => {
      expect(summary.querySelectorAll("p").length).toBe(2);
    });

    await step("bare URL autolinks to a safe new-tab anchor", async () => {
      const link = summary.querySelector("a");
      expect(link).toBeTruthy();
      expect(link?.getAttribute("href")).toBe("https://openid.net/specs/x");
      expect(link?.getAttribute("target")).toBe("_blank");
      expect(link?.getAttribute("rel")).toBe("noopener noreferrer");
    });

    await step("inline code renders and snake_case prose survives", async () => {
      expect(summary.querySelector("code")?.textContent).toBe("request_uri");
    });
  },
};

// Long-values data reproducing the mobile readability bug
// (docs/plans/2026-06-05-004-fix-plan-header-mobile-responsive-plan.md):
// a ~66-char plan name, a five-entry variant map, and a two-entry
// certification profile. Story-local on purpose — the shared PLAN
// fixture omits certificationProfileName ("Certification profile:" is
// the longest label, the one that sized the legacy max-content track),
// and its short values would make the stacking and track-bound
// assertions below pass vacuously.
const LONG_PLAN = {
  ...PLAN,
  planName: "oid4vci-id2-issuer-test-plan-credential-offer-pre-authorized-code",
  variant: {
    client_auth_type: "client_secret_basic",
    response_type: "code",
    credential_format: "sd_jwt_vc",
    sender_constrain: "dpop",
    response_mode: "direct_post.jwt",
  },
  certificationProfileName: ["FAPI2SP Final OP w/ MTLS", "FAPI2MS ID1 OP w/ Private Key"],
};

/**
 * Resolve the rendered .planMeta <dl>, waiting out the Lit render.
 * Computed-style assertions only in the stories below — no
 * getBoundingClientRect() (rects lie inside display: contents
 * subtrees, and computed tracks are the contract under test).
 */
async function waitForPlanMeta(canvasElement) {
  return waitFor(() => {
    const el = canvasElement.querySelector("cts-plan-header .planMeta");
    if (!el) throw new Error(".planMeta not yet rendered");
    return el;
  });
}

/**
 * Mobile stacked-metadata contract
 * (docs/plans/2026-06-05-004-fix-plan-header-mobile-responsive-plan.md).
 * Pinned to mobile1 (320×568) so the ctsPlanHeader inline-size
 * container sits below the 640px threshold: the metadata <dl> must
 * collapse to a single stacked column (label above value) — the legacy
 * max-content 1fr grid let the longest label eat ~164px of a ~312px
 * content box at phone widths, squeezing every value into a ~132px
 * sliver.
 */
export const MetaStacksOnMobile = {
  parameters: {
    viewport: { defaultViewport: "mobile1" },
  },
  globals: {
    viewport: { value: "mobile1", isRotated: false },
  },
  render: () => html`<cts-plan-header .plan=${LONG_PLAN}></cts-plan-header>`,
  async play({ canvasElement, step }) {
    const meta = /** @type {HTMLElement} */ (await waitForPlanMeta(canvasElement));

    await step("metadata list stacks to a single column", async () => {
      const tracks = getComputedStyle(meta).gridTemplateColumns.trim().split(/\s+/);
      expect(tracks).toHaveLength(1);
    });

    await step("pairs are separated by the dt margin rhythm", async () => {
      // Within a pair the value hugs its label (4px grid gap); the 12px
      // dt margin-top separates one label/value pair from the next. The
      // first dt carries no margin — the <dl>'s own margin-top spaces
      // the list from the title/lede above.
      const dts = meta.querySelectorAll("dt");
      expect(dts.length).toBeGreaterThan(1);
      expect(getComputedStyle(dts[0]).marginTop).toBe("0px");
      expect(getComputedStyle(dts[1]).marginTop).toBe("12px");
    });
  },
};

/**
 * Wide two-column contract
 * (docs/plans/2026-06-05-004-fix-plan-header-mobile-responsive-plan.md).
 * Pinned to the desktop preset so the ctsPlanHeader inline-size
 * container is deterministically above the 640px threshold — the
 * two-track assertion must not depend on the default canvas width.
 */
export const MetaTwoColumnOnDesktop = {
  parameters: {
    viewport: { defaultViewport: "desktop" },
  },
  globals: {
    viewport: { value: "desktop", isRotated: false },
  },
  render: () => html`<cts-plan-header .plan=${LONG_PLAN}></cts-plan-header>`,
  async play({ canvasElement, step }) {
    const meta = /** @type {HTMLElement} */ (await waitForPlanMeta(canvasElement));

    await step("two columns with a content-hugging label track", async () => {
      // fit-content(180px) sizes the label track to the longest label
      // ("Certification profile:", ~164px measured), clamped at 180px.
      // The bound is strict (< 176) so a revert to a fixed-max
      // maximizing minmax() — which resolves to exactly 180px — fails
      // this step. 176 rather than log-detail's 160 because this
      // component's longest label genuinely measures ~164px (vs ~84px
      // there); the bound sits between the measured healthy value and
      // the 180px failure mode.
      const tracks = getComputedStyle(meta).gridTemplateColumns.trim().split(/\s+/);
      expect(tracks).toHaveLength(2);
      expect(parseFloat(tracks[0])).toBeLessThan(176);
    });
  },
};
