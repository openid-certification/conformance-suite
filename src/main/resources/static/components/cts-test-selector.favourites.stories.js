import { html } from "lit";
import { expect, waitFor, userEvent } from "storybook/test";
import { MOCK_PLANS } from "@fixtures/mock-plans.js";
import "./cts-test-selector.js";

/**
 * Favourites prototype for cts-test-selector.
 *
 * Favourites is a *controlled* capability: the host takes a `favourites`
 * array in and emits `cts-favourite-toggle` out; it never mutates its own
 * array. These stories stand in for the future caller (schedule-test.html /
 * `/api/favourite-plans`) — most simulate the caller's optimistic update by
 * setting `host.favourites` from the event in the play function. The
 * localStorage-backed adapter (U6) and the failure/persist matrix (U7) build
 * on top of this contract.
 *
 * The three layouts are driven by one `favourites-layout` attribute (KTD2):
 * `group` (V1, pinned section), `view` (V2, saved-view listbox entry), and
 * `chip` (V3, filter toggle). This file starts with the shared row-level
 * contract; the variant-specific surfaces and the full State Matrix follow.
 */
export default {
  title: "Components/cts-test-selector/Favourites",
  component: "cts-test-selector",
};

const FAV = "fapi2-security-profile-final-test-plan";
const NOT_FAV = "oidcc-basic-certification-test-plan";

/** Find a row's star button by the plan name carried on the select button. */
function starFor(host, planName) {
  const row = host.querySelector(`.oidf-test-selector__row[data-plan-name="${planName}"]`);
  return row?.closest(".oidf-test-selector__item")?.querySelector(".oidf-test-selector__fav");
}

/**
 * Wire the controlled loop a real caller would: on `cts-favourite-toggle`,
 * optimistically add/remove the plan from `host.favourites`. No persistence,
 * no latency — that is the U6 adapter's job. Returns the captured events so a
 * play function can assert payloads.
 */
function wireOptimistic(host) {
  /** @type {any[]} */
  const events = [];
  host.addEventListener("cts-favourite-toggle", (e) => {
    const { plan, favourite } = /** @type {CustomEvent} */ (e).detail;
    events.push(/** @type {CustomEvent} */ (e).detail);
    const next = new Set(host.favourites);
    if (favourite) next.add(plan);
    else next.delete(plan);
    host.favourites = [...next];
  });
  return events;
}

/**
 * Read render: a starred plan shows the filled star with aria-pressed=true;
 * an unstarred plan shows the outline star with aria-pressed=false. The star
 * is a *sibling* of the select button, never nested inside it.
 */
export const RowStarsReflectFavouriteState = {
  render: () => html`
    <cts-test-selector
      .plans=${MOCK_PLANS}
      .favourites=${[FAV]}
      favourites-layout="group"
    ></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;

    await step("every row has a sibling favourite button (no nested buttons)", async () => {
      const stars = host.querySelectorAll(".oidf-test-selector__fav");
      expect(stars.length).toBe(MOCK_PLANS.length);
      // Button-in-button is invalid and fails a11y — the star must not live
      // inside the select button.
      expect(host.querySelector(".oidf-test-selector__row .oidf-test-selector__fav")).toBeNull();
    });

    await step("the favourited row reads pressed with the filled star", async () => {
      const star = starFor(host, FAV);
      expect(star.getAttribute("aria-pressed")).toBe("true");
      expect(star.getAttribute("aria-label")).toBe("Remove favourite: FAPI 2.0 Security Profile");
      expect(star.querySelector("cts-icon").getAttribute("name")).toBe("star-fill");
      expect(star.classList.contains("is-favourited")).toBe(true);
    });

    await step("a non-favourited row reads unpressed with the outline star", async () => {
      const star = starFor(host, NOT_FAV);
      expect(star.getAttribute("aria-pressed")).toBe("false");
      expect(star.getAttribute("aria-label")).toContain("Add favourite:");
      expect(star.querySelector("cts-icon").getAttribute("name")).toBe("star");
    });
  },
};

/**
 * Create via star click: clicking an unstarred row's star fires
 * `cts-favourite-toggle { favourite:true, via:'click' }`. Once the caller
 * applies the optimistic update, the star flips to pressed/filled.
 */
export const StarClickFiresToggleAndOptimisticallyAdds = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} favourites-layout="group"></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    const events = wireOptimistic(host);

    await step("clicking the star fires the add toggle tagged via:'click'", async () => {
      await userEvent.click(starFor(host, NOT_FAV));
      expect(events.length).toBe(1);
      expect(events[0]).toEqual({
        plan: NOT_FAV,
        favourite: true,
        via: "click",
      });
    });

    await step("the optimistic update flips the star to pressed/filled", async () => {
      await waitFor(() => {
        const star = starFor(host, NOT_FAV);
        expect(star.getAttribute("aria-pressed")).toBe("true");
        expect(star.querySelector("cts-icon").getAttribute("name")).toBe("star-fill");
      });
    });

    await step("clicking again fires the remove toggle", async () => {
      await userEvent.click(starFor(host, NOT_FAV));
      expect(events[1]).toEqual({
        plan: NOT_FAV,
        favourite: false,
        via: "click",
      });
      await waitFor(() => {
        expect(starFor(host, NOT_FAV).getAttribute("aria-pressed")).toBe("false");
      });
    });
  },
};

/**
 * Keyboard create/delete: the "f" shortcut toggles the focused row's
 * favourite without leaving the roving model, tagged via:'keyboard'.
 */
export const KeyboardFTogglesFocusedRow = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} favourites-layout="group"></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    const events = wireOptimistic(host);
    const searchInput = host.querySelector(".oidf-test-selector__search");
    const firstRow = host.querySelector(".oidf-test-selector__row");

    await step("arrow into the first row", async () => {
      searchInput.focus();
      await userEvent.keyboard("{ArrowDown}");
      await waitFor(() => expect(document.activeElement).toBe(firstRow));
    });

    await step("'f' toggles the focused row's favourite via keyboard", async () => {
      await userEvent.keyboard("f");
      expect(events.length).toBe(1);
      expect(events[0]).toEqual({
        plan: MOCK_PLANS[0].planName,
        favourite: true,
        via: "keyboard",
      });
    });

    await step("the focused row's star is also a tab stop", async () => {
      // Roving tabindex: the focused row exposes both its select button and
      // its star as tab stops; every other row's controls stay at -1.
      const star = starFor(host, MOCK_PLANS[0].planName);
      await waitFor(() => expect(star.getAttribute("tabindex")).toBe("0"));
      const otherStar = starFor(host, MOCK_PLANS[2].planName);
      expect(otherStar.getAttribute("tabindex")).toBe("-1");
    });
  },
};

/**
 * Cannot-favourite (no principal): anonymous / private-link users have no
 * server-side principal to key a favourite on, so the star renders disabled
 * with an explanatory tooltip rather than vanishing, and clicking is a no-op.
 */
export const CannotFavouriteDisablesStar = {
  render: () => html`
    <cts-test-selector
      .plans=${MOCK_PLANS}
      .canFavourite=${false}
      favourites-layout="group"
    ></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    /** @type {any[]} */
    const events = [];
    host.addEventListener("cts-favourite-toggle", (e) =>
      events.push(/** @type {CustomEvent} */ (e).detail),
    );

    await step("stars render aria-disabled with an explanatory label", async () => {
      const star = starFor(host, NOT_FAV);
      expect(star.getAttribute("aria-disabled")).toBe("true");
      expect(star.getAttribute("aria-label")).toContain("Sign in to save favourites");
      expect(star.closest("cts-tooltip")).not.toBeNull();
    });

    await step("clicking a disabled star fires nothing", async () => {
      await userEvent.click(starFor(host, NOT_FAV));
      expect(events.length).toBe(0);
    });
  },
};

/**
 * Back-compat guard: with no `favourites-layout`, no stars render and the
 * list is exactly today's plain list.
 */
export const NoLayoutRendersNoStars = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    expect(host.querySelector(".oidf-test-selector__fav")).toBeNull();
    expect(host.querySelectorAll(".oidf-test-selector__row").length).toBe(MOCK_PLANS.length);
  },
};
