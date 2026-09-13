import { html } from "lit";
import { unsafeHTML } from "lit/directives/unsafe-html.js";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { MOCK_USER } from "@fixtures/mock-users.js";
import { MOCK_GUIDED_PLANS } from "../../e2e/fixtures/mock-plans.js";
import FIELD_CATALOG from "../../../src/main/resources/static/js/config-field-catalog.json";
import {
  bootGuidedMode,
  startGuidedJourney,
} from "../../../src/main/resources/static/components/guided-wizard.js";

import "../../../src/main/resources/static/components/cts-icon.js";
import "../../../src/main/resources/static/components/cts-action-bar.js";
import "../../../src/main/resources/static/components/cts-button.js";
import "../../../src/main/resources/static/components/cts-card.js";
import "../../../src/main/resources/static/components/cts-badge.js";

// Recreates the guided half of `schedule-test.html` for visual review of the
// wizard's key screens. The guided journey is not a component: it renders
// into fixed ids inside `#guidedIsland` and is styled by the page's inline
// <style>, so rather than copying ~450 lines of CSS and markup (which would
// drift), the loader fetches the real page from Storybook's static root and
// lifts the mode header, the guided island and the stylesheet out of it.
// The journey then runs on the mocked catalog the Playwright guided spec
// uses, so the stories and the e2e tests see the same plans.
//
// Intentionally omitted, mirroring the other page stories:
// - navbar, skip-link and footer — page chrome not under review here;
// - the advanced island — replaced by a hidden stub so the mode controller
//   has both islands to toggle between.

export default {
  title: "Pages/ScheduleTestGuided",
  parameters: {
    // Page-level story: opt in to Chromatic snapshots. Component stories are
    // excluded by default in frontend/.storybook/preview.js.
    chromatic: { disableSnapshot: false },
  },
  loaders: [
    async () => {
      const res = await fetch("/schedule-test.html");
      const doc = new DOMParser().parseFromString(await res.text(), "text/html");
      const style = doc.querySelector("head style");
      const head = doc.querySelector(".schedule-test-mode-head");
      const island = doc.getElementById("guidedIsland");
      if (!style || !head || !island) {
        throw new Error("schedule-test.html no longer has the markup this story lifts");
      }
      return {
        pageStyle: style.textContent,
        headHtml: head.outerHTML,
        islandHtml: island.outerHTML,
      };
    },
  ],
  /**
   * @param {object} _args
   * @param {{loaded: {pageStyle: string, headHtml: string, islandHtml: string}}} context
   */
  render: (_args, { loaded }) => html`
    ${unsafeHTML(`<style>${loaded.pageStyle}</style>`)}
    <main class="schedule-test-page" id="main-content">
      ${unsafeHTML(loaded.headHtml)} ${unsafeHTML(loaded.islandHtml)}
      <div id="scheduleTestPage" hidden></div>
    </main>
  `,
};

const AVAILABLE_PLANS = Object.fromEntries(MOCK_GUIDED_PLANS.map((p) => [p.planName, p]));

/**
 * Boot the mode controller and start the journey on the rendered island,
 * the way schedule-test.html's init chain does once the catalog is loaded.
 * View transitions are disabled first so each step renders synchronously
 * and Chromatic never captures a cross-fade.
 */
function startJourney() {
  Object.defineProperty(document, "startViewTransition", { value: undefined, configurable: true });
  const controller = bootGuidedMode({
    params: new URLSearchParams(""),
    storage: null,
    session: null,
  });
  startGuidedJourney(controller, {
    availablePlans: AVAILABLE_PLANS,
    fieldCatalog: FIELD_CATALOG,
    getCurrentUser: () => MOCK_USER,
    session: null,
  });
}

/**
 * @param {HTMLElement} canvasElement
 * @param {string} choiceId
 */
async function pickChoice(canvasElement, choiceId) {
  const card = canvasElement.querySelector(`#guidedStage .choice[data-choice="${choiceId}"]`);
  if (!(card instanceof HTMLElement)) throw new Error(`no choice card ${choiceId}`);
  await userEvent.click(card);
}

/**
 * @param {HTMLElement} canvasElement
 * @param {string} text
 */
async function waitForHeading(canvasElement, text) {
  await waitFor(() => {
    const h1 = canvasElement.querySelector("#guidedStage h1");
    expect(h1?.textContent).toContain(text);
  });
}

/** The first screen: one card per ecosystem. */
export const EcosystemPicker = {
  /** @param {{canvasElement: HTMLElement}} ctx */
  play: async ({ canvasElement }) => {
    startJourney();
    await waitForHeading(canvasElement, "Which ecosystem are you certifying for?");
    const canvas = within(canvasElement);
    await expect(canvas.getByRole("radio", { name: /OpenFinance Brazil/ })).toBeInTheDocument();
  },
};

/**
 * The OpenFinance Brazil OP plan question (#1967): the two plans a Brazil OP
 * certification needs each name the other in their description.
 */
export const BrazilOpPlanQuestion = {
  /** @param {{canvasElement: HTMLElement}} ctx */
  play: async ({ canvasElement }) => {
    startJourney();
    await waitForHeading(canvasElement, "Which ecosystem");
    await pickChoice(canvasElement, "open_finance_brazil");
    await waitForHeading(canvasElement, "What is your role?");
    await pickChoice(canvasElement, "op");
    await waitForHeading(canvasElement, "Which certification plan are you creating?");
    const canvas = within(canvasElement);
    await expect(canvas.getByText(/requires the Dynamic Client Registration plan/)).toBeVisible();
    await expect(canvas.getByText(/requires the FAPI Security Profile plan/)).toBeVisible();
  },
};

/** The review step for the Brazil OP FAPI plan: resolved plan + variant table. */
export const BrazilFapiReview = {
  /** @param {{canvasElement: HTMLElement}} ctx */
  play: async ({ canvasElement }) => {
    startJourney();
    await waitForHeading(canvasElement, "Which ecosystem");
    await pickChoice(canvasElement, "open_finance_brazil");
    await waitForHeading(canvasElement, "What is your role?");
    await pickChoice(canvasElement, "op");
    await waitForHeading(canvasElement, "Which certification plan");
    await pickChoice(canvasElement, "fapi1_brazil_op");
    await waitForHeading(canvasElement, "Here's the plan we resolved");
    const canvas = within(canvasElement);
    await expect(canvas.getByText("FAPI1-Advanced-Final: Authorization server test")).toBeVisible();
    await expect(canvas.getByText("Open Finance Brazil")).toBeVisible();
  },
};
