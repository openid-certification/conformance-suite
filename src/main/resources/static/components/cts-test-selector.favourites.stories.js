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

/**
 * Find a row's star button in the MAIN list (not the V1 group region, which
 * renders duplicate rows for favourited plans). Scoped so star counts and
 * lookups stay deterministic regardless of the active layout.
 */
function starFor(host, planName) {
  const row = host.querySelector(
    `.oidf-test-selector__list .oidf-test-selector__row[data-plan-name="${planName}"]`,
  );
  return row?.closest(".oidf-test-selector__item")?.querySelector(".oidf-test-selector__fav");
}

/** The star/remove button for a plan inside the V1 "★ Favourites" group region. */
function groupStarFor(host, planName) {
  const row = host.querySelector(`.oidf-test-selector__favourites [data-plan-name="${planName}"]`);
  return row?.closest(".oidf-test-selector__item")?.querySelector(".oidf-test-selector__fav");
}

/** All star buttons in the main list. */
function mainStars(host) {
  return host.querySelectorAll(".oidf-test-selector__list .oidf-test-selector__fav");
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

    await step(
      "every main-list row has a sibling favourite button (no nested buttons)",
      async () => {
        expect(mainStars(host).length).toBe(MOCK_PLANS.length);
        // Button-in-button is invalid and fails a11y — the star must not live
        // inside the select button.
        expect(host.querySelector(".oidf-test-selector__row .oidf-test-selector__fav")).toBeNull();
      },
    );

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

// ───────────────────────────────────────────────────────────────────────────
// V1 — "group" layout: pinned "★ Favourites" section above the list.
// ───────────────────────────────────────────────────────────────────────────

const OIDCC_BASIC = "oidcc-basic-certification-test-plan";
const STALE = "retired-plan-no-longer-in-catalogue";

/** Rows rendered inside the V1 group region. */
function groupRows(host) {
  return host.querySelectorAll(".oidf-test-selector__favourites-list .oidf-test-selector__item");
}
/** The count badge text in the group header. */
function groupCount(host) {
  return host.querySelector(".oidf-test-selector__favourites-count")?.textContent?.trim();
}

/**
 * Group Read: favourites render in the pinned section in caller order, with a
 * count badge. Favourited plans also remain in the main list (Open Question
 * default: "remain") carrying a filled star.
 */
export const GroupRendersFavouritesWithCount = {
  render: () => html`
    <cts-test-selector
      .plans=${MOCK_PLANS}
      .favourites=${[FAV, OIDCC_BASIC]}
      favourites-layout="group"
    ></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;

    await step("the group lists both favourites, in caller order, with a count", async () => {
      const rows = groupRows(host);
      expect(rows.length).toBe(2);
      expect(rows[0].querySelector(".oidf-test-selector__row-name").textContent).toContain(
        "FAPI 2.0 Security Profile",
      );
      expect(groupCount(host)).toBe("2");
    });

    await step("the favourited plans still appear in the main list, filled", async () => {
      expect(starFor(host, FAV).querySelector("cts-icon").getAttribute("name")).toBe("star-fill");
      // The main list still shows every plan.
      expect(mainStars(host).length).toBe(MOCK_PLANS.length);
    });
  },
};

/** Group Read (empty): no favourites yet shows a CTA, not an empty box. */
export const GroupEmptyStateShowsCta = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} favourites-layout="group"></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    await step("empty favourites renders the CTA, no group rows", async () => {
      expect(groupRows(host).length).toBe(0);
      const empty = host.querySelector(".oidf-test-selector__favourites-empty");
      expect(empty.textContent).toContain("Star a plan to pin it here");
      expect(groupCount(host)).toBe("0");
    });
  },
};

/** Group Read (loading): the section shows a skeleton, not rows or the CTA. */
export const GroupLoadingShowsSkeleton = {
  render: () => html`
    <cts-test-selector
      .plans=${MOCK_PLANS}
      .favourites=${[FAV]}
      favourites-layout="group"
      favourites-loading
    ></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    await step("skeleton renders instead of rows while favourites load", async () => {
      expect(host.querySelector(".oidf-test-selector__favourites-skeleton")).toBeTruthy();
      expect(groupRows(host).length).toBe(0);
      expect(host.querySelector(".oidf-test-selector__favourites-empty")).toBeNull();
    });
  },
};

/** Group Create: starring a plan in the main list pins it into the group. */
export const GroupAddViaStarPinsToGroup = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} favourites-layout="group"></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    const events = wireOptimistic(host);

    await step("starring a main-list row pins it into the group", async () => {
      await userEvent.click(starFor(host, NOT_FAV));
      expect(events[0]).toEqual({ plan: NOT_FAV, favourite: true, via: "click" });
      await waitFor(() => {
        expect(groupRows(host).length).toBe(1);
        expect(groupStarFor(host, NOT_FAV)).toBeTruthy();
        expect(groupCount(host)).toBe("1");
      });
    });
  },
};

/** Group Delete: unstarring from the group removes the pin and updates count. */
export const GroupRemoveViaUnstar = {
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
    const events = wireOptimistic(host);

    await step("unstarring inside the group removes it and reveals the CTA", async () => {
      await userEvent.click(groupStarFor(host, FAV));
      expect(events[0]).toEqual({ plan: FAV, favourite: false, via: "click" });
      await waitFor(() => {
        expect(groupRows(host).length).toBe(0);
        expect(host.querySelector(".oidf-test-selector__favourites-empty")).toBeTruthy();
        // The main-list star reverts to the outline.
        expect(starFor(host, FAV).querySelector("cts-icon").getAttribute("name")).toBe("star");
      });
    });
  },
};

/** Idempotent: star → unstar → star nets exactly one group entry. */
export const GroupIdempotentToggle = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} favourites-layout="group"></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    wireOptimistic(host);

    await step("star, unstar, star again ends with one entry", async () => {
      await userEvent.click(starFor(host, NOT_FAV));
      await waitFor(() => expect(groupRows(host).length).toBe(1));
      await userEvent.click(groupStarFor(host, NOT_FAV));
      await waitFor(() => expect(groupRows(host).length).toBe(0));
      await userEvent.click(starFor(host, NOT_FAV));
      await waitFor(() => {
        expect(groupRows(host).length).toBe(1);
        expect(groupCount(host)).toBe("1");
      });
    });
  },
};

/** KTD6: search narrows the group as well as the main list. */
export const GroupRespectsSearch = {
  render: () => html`
    <cts-test-selector
      .plans=${MOCK_PLANS}
      .favourites=${[FAV, OIDCC_BASIC]}
      favourites-layout="group"
    ></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    const search = host.querySelector(".oidf-test-selector__search");

    await step("typing FAPI narrows the group to the matching favourite", async () => {
      await userEvent.type(search, "FAPI");
      await waitFor(() => {
        // Only the FAPI favourite remains pinned; the OIDCC one is filtered out.
        expect(groupRows(host).length).toBe(1);
        expect(groupStarFor(host, FAV)).toBeTruthy();
        expect(groupStarFor(host, OIDCC_BASIC)).toBeFalsy();
      });
    });
  },
};

/** KTD6: the family filter narrows the group too. */
export const GroupRespectsFamilyFilter = {
  render: () => html`
    <cts-test-selector
      .plans=${MOCK_PLANS}
      .favourites=${[FAV, OIDCC_BASIC]}
      favourites-layout="group"
    ></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    const select = host.querySelector(".oidf-test-selector__family");

    await step("selecting the FAPI family leaves only the FAPI favourite", async () => {
      await userEvent.selectOptions(select, "FAPI");
      await waitFor(() => {
        expect(groupRows(host).length).toBe(1);
        expect(groupStarFor(host, FAV)).toBeTruthy();
      });
    });
  },
};

/**
 * KTD5 stale pin: a favourited planName missing from `plans` renders disabled
 * with an explicit remove control and a "no longer available" note. Selecting
 * the body is a no-op; only remove is actionable.
 */
export const GroupStaleFavouriteIsRemovable = {
  render: () => html`
    <cts-test-selector
      .plans=${MOCK_PLANS}
      .favourites=${[STALE]}
      favourites-layout="group"
    ></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    /** @type {any[]} */
    const selects = [];
    host.addEventListener("cts-plan-select", (e) =>
      selects.push(/** @type {CustomEvent} */ (e).detail),
    );
    const events = wireOptimistic(host);

    await step("the stale pin renders disabled with a 'no longer available' note", async () => {
      const stale = host.querySelector(".oidf-test-selector__row--stale");
      expect(stale).toBeTruthy();
      expect(stale.getAttribute("aria-disabled")).toBe("true");
      expect(stale.textContent).toContain("No longer available");
      expect(stale.querySelector(".oidf-test-selector__row-name").textContent).toBe(STALE);
    });

    await step("clicking the disabled body selects nothing", async () => {
      await userEvent.click(host.querySelector(".oidf-test-selector__row--stale"));
      expect(selects.length).toBe(0);
    });

    await step("the remove control unpins it", async () => {
      await userEvent.click(groupStarFor(host, STALE));
      expect(events[0]).toEqual({ plan: STALE, favourite: false, via: "click" });
      await waitFor(() => expect(groupRows(host).length).toBe(0));
    });
  },
};

/**
 * Many favourites: every favourite pins into the group, which is bounded so it
 * scrolls within a fixed height rather than pushing the main list off-screen.
 */
export const GroupManyFavouritesOverflow = {
  render: () => html`
    <cts-test-selector
      .plans=${MOCK_PLANS}
      .favourites=${MOCK_PLANS.map((p) => p.planName)}
      favourites-layout="group"
    ></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;

    await step("all favourites pin into the group", async () => {
      expect(groupRows(host).length).toBe(MOCK_PLANS.length);
      expect(groupCount(host)).toBe(String(MOCK_PLANS.length));
    });

    await step("the group region is height-bounded so it scrolls", async () => {
      const css = document.getElementById("cts-test-selector-styles")?.textContent || "";
      expect(css).toContain(".oidf-test-selector__favourites-list");
      expect(css).toContain("max-height");
      expect(css).toContain("overflow-y: auto");
    });
  },
};
