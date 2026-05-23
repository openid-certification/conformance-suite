import { html } from "lit";
import { expect } from "storybook/test";
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

  async play({ canvasElement }) {
    const header = /** @type {HTMLElement} */ (canvasElement.querySelector("cts-plan-header"));
    expect(header).toBeTruthy();
    expect(header.textContent).toContain(PLAN.planName);

    // The Started value renders through cts-time: a native <time> whose title
    // carries the full absolute date for hover disambiguation.
    const timeEl = /** @type {HTMLTimeElement | null} */ (header.querySelector("dd time"));
    expect(timeEl).toBeTruthy();
    expect(timeEl?.textContent?.trim()).toBe(formatAbsolute(PLAN.started));
    expect(timeEl?.getAttribute("title")).toBe(formatAbsolute(PLAN.started));
    expect(timeEl?.getAttribute("datetime")).toBe(new Date(PLAN.started).toISOString());
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
