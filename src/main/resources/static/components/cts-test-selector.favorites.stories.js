import { html } from "lit";
import { expect, waitFor, userEvent, spyOn } from "storybook/test";
import { MOCK_PLANS } from "@fixtures/mock-plans.js";
import "./cts-test-selector.js";
// Side-effect import installs window.ctsToast so the failure stories can spy
// on it (the store raises an error toast through that global, KTD4).
import "../js/cts-toast-api.js";
import { attachFavorites } from "./cts-test-selector.favorites-store.js";
import { createFakeFavoritesController } from "./cts-test-selector.favorites-store.fake.js";

// The favorites LIST is account data stored server-side (the production store
// does `fetch /api/favorite-plans`), which can't run against a server in
// Storybook — so the persist/failure stories drive the shared attachFavorites
// logic with an in-memory fake controller. Only the component-internal FILTER
// preference is localStorage, so that's the single key to reset between stories.
const FILTER_KEY = "cts:test-selector-filter";

/**
 * Favorites for cts-test-selector — the converged, always-on saved-view design.
 *
 * Favorites is a *controlled* capability: the host takes a `favorites` array in
 * and emits `cts-favorite-toggle` out; it never mutates its own array. The
 * "★ Favorites (n)" entry sits atop the family listbox and filters the plan
 * list to favorites only; every row carries a star toggle. Most stories
 * simulate the caller's optimistic update inline (`wireOptimistic`); the
 * persist/failure stories drive `attachFavorites` with an in-memory fake
 * controller — the same wiring schedule-test.html uses against the real
 * `/api/favorite-plans` server store.
 *
 * The selected filter choice (a spec family or the ★ Favorites view) is
 * persisted component-internally to `cts:test-selector-filter` and restored on
 * mount; the favorites list itself is server-side, keyed on the principal.
 */
export default {
  title: "Components/cts-test-selector/Favorites",
  component: "cts-test-selector",
  // Reset the filter preference before every story so it never leaks across
  // stories. The favorites list lives in each story's own fake controller, so
  // there is no shared favorites state to clear. (No URL writes → no history
  // reset.) removeItem never throws for a missing key.
  beforeEach: () => {
    localStorage.removeItem(FILTER_KEY);
  },
};

const FAV = "fapi2-security-profile-final-test-plan";
const NOT_FAV = "oidcc-basic-certification-test-plan";
const OIDCC_BASIC = NOT_FAV;
const STALE = "retired-plan-no-longer-in-catalog";

/** The star button for a plan in the main list. */
function starFor(host, planName) {
  const row = host.querySelector(
    `.oidf-test-selector__list .oidf-test-selector__row[data-plan-name="${planName}"]`,
  );
  return row?.closest(".oidf-test-selector__item")?.querySelector(".oidf-test-selector__fav");
}

/** All star buttons in the main list (includes a stale row's remove control). */
function mainStars(host) {
  return host.querySelectorAll(".oidf-test-selector__list .oidf-test-selector__fav");
}

/** Rows + stale entries rendered in the main list. */
function mainRows(host) {
  return host.querySelectorAll(".oidf-test-selector__list .oidf-test-selector__item");
}

/** The synthetic "★ Favorites" option in the family listbox. */
function viewOption(host) {
  return host.querySelector(".oidf-test-selector__family-view");
}

/** Select the saved-view entry in the family listbox. */
async function selectFavoritesView(host) {
  await userEvent.selectOptions(
    host.querySelector(".oidf-test-selector__family"),
    viewOption(host),
  );
}

/**
 * Wire the controlled loop a real caller would: on `cts-favorite-toggle`,
 * optimistically add/remove the plan from `host.favorites`. No persistence, no
 * latency — that is the store's job. Returns the captured event details so a
 * play function can assert payloads.
 */
function wireOptimistic(host) {
  /** @type {any[]} */
  const events = [];
  host.addEventListener("cts-favorite-toggle", (e) => {
    const { plan, favorite } = /** @type {CustomEvent} */ (e).detail;
    events.push(/** @type {CustomEvent} */ (e).detail);
    const next = new Set(host.favorites);
    if (favorite) next.add(plan);
    else next.delete(plan);
    host.favorites = [...next];
  });
  return events;
}

// ───────────────────────────────────────────────────────────────────────────
// Row-level lifecycle (always-on; layout-independent).
// ───────────────────────────────────────────────────────────────────────────

/**
 * Read render: a starred plan shows the filled star with aria-pressed=true; an
 * unstarred plan shows the outline star with aria-pressed=false. The star is a
 * *sibling* of the select button, never nested inside it.
 */
export const RowStarsReflectFavoriteState = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} .favorites=${[FAV]}></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;

    await step(
      "every main-list row has a sibling favorite button (no nested buttons)",
      async () => {
        expect(mainStars(host).length).toBe(MOCK_PLANS.length);
        // Button-in-button is invalid and fails a11y — the star must not live
        // inside the select button.
        expect(host.querySelector(".oidf-test-selector__row .oidf-test-selector__fav")).toBeNull();
      },
    );

    await step("the favorited row reads pressed with the filled star", async () => {
      const star = starFor(host, FAV);
      expect(star.getAttribute("aria-pressed")).toBe("true");
      expect(star.getAttribute("aria-label")).toBe("Remove favorite: FAPI 2.0 Security Profile");
      expect(star.querySelector("cts-icon").getAttribute("name")).toBe("star-fill");
      expect(star.classList.contains("is-favorited")).toBe(true);
    });

    await step("a non-favorited row reads unpressed with the outline star", async () => {
      const star = starFor(host, NOT_FAV);
      expect(star.getAttribute("aria-pressed")).toBe("false");
      expect(star.getAttribute("aria-label")).toContain("Add favorite:");
      expect(star.querySelector("cts-icon").getAttribute("name")).toBe("star");
    });
  },
};

/**
 * Create via star click: clicking an unstarred row's star fires
 * `cts-favorite-toggle { favorite:true, via:'click' }`. Once the caller applies
 * the optimistic update, the star flips to pressed/filled.
 */
export const StarClickFiresToggleAndOptimisticallyAdds = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    const events = wireOptimistic(host);

    await step("clicking the star fires the add toggle tagged via:'click'", async () => {
      await userEvent.click(starFor(host, NOT_FAV));
      expect(events.length).toBe(1);
      expect(events[0]).toEqual({ plan: NOT_FAV, favorite: true, via: "click" });
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
      expect(events[1]).toEqual({ plan: NOT_FAV, favorite: false, via: "click" });
      await waitFor(() => {
        expect(starFor(host, NOT_FAV).getAttribute("aria-pressed")).toBe("false");
      });
    });
  },
};

/**
 * Keyboard create/delete: the "f" shortcut toggles the focused row's favorite
 * without leaving the roving model, tagged via:'keyboard'.
 */
export const KeyboardFTogglesFocusedRow = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
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

    await step("'f' toggles the focused row's favorite via keyboard", async () => {
      await userEvent.keyboard("f");
      expect(events.length).toBe(1);
      expect(events[0]).toEqual({
        plan: MOCK_PLANS[0].planName,
        favorite: true,
        via: "keyboard",
      });
    });

    await step("the focused row's star is also a tab stop", async () => {
      // Roving tabindex: the focused row exposes both its select button and its
      // star as tab stops; every other row's controls stay at -1.
      const star = starFor(host, MOCK_PLANS[0].planName);
      await waitFor(() => expect(star.getAttribute("tabindex")).toBe("0"));
      const otherStar = starFor(host, MOCK_PLANS[2].planName);
      expect(otherStar.getAttribute("tabindex")).toBe("-1");
    });
  },
};

/**
 * Cannot favorite: when the favorites API can't be reached (a 401 with no
 * signed-in principal, or a network error) the page sets canFavorite=false. The
 * stars render disabled with an explanatory tooltip rather than vanishing,
 * clicking is a no-op, and the ★ Favorites view explains why it is empty.
 */
export const CannotFavoriteDisablesStar = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} .canFavorite=${false}></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    /** @type {any[]} */
    const events = [];
    host.addEventListener("cts-favorite-toggle", (e) =>
      events.push(/** @type {CustomEvent} */ (e).detail),
    );

    await step("stars render aria-disabled with an explanatory tooltip", () => {
      const star = starFor(host, NOT_FAV);
      expect(star.getAttribute("aria-disabled")).toBe("true");
      expect(star.getAttribute("aria-label")).toContain("Sign in to save favorites");
      expect(star.closest("cts-tooltip")).not.toBeNull();
    });

    await step("clicking a disabled star fires nothing", async () => {
      await userEvent.click(starFor(host, NOT_FAV));
      expect(events.length).toBe(0);
    });

    await step("the ★ Favorites view explains it needs sign-in", async () => {
      await selectFavoritesView(host);
      await waitFor(() => {
        const empty = host.querySelector(".oidf-test-selector__empty");
        expect(empty.textContent).toContain("Sign in to save favorites");
      });
    });
  },
};

// ───────────────────────────────────────────────────────────────────────────
// Saved view: "★ Favorites (n)" entry filters the list to favorites only.
// ───────────────────────────────────────────────────────────────────────────

/** View Read: selecting "★ Favorites" filters the right list to favorites. */
export const ViewFiltersListToFavorites = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} .favorites=${[FAV, OIDCC_BASIC]}></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;

    await step("the saved view sits atop the listbox with the count", async () => {
      expect(viewOption(host).textContent).toContain("★ Favorites (2)");
      // Before selection the full list is shown.
      expect(mainRows(host).length).toBe(MOCK_PLANS.length);
    });

    await step("selecting it narrows the list to favorites only", async () => {
      await selectFavoritesView(host);
      await waitFor(() => {
        expect(mainRows(host).length).toBe(2);
        expect(starFor(host, FAV)).toBeTruthy();
        expect(starFor(host, "fapi-ciba-test-plan")).toBeFalsy();
      });
    });
  },
};

/** View count + unstar: removing a favorite from the view drops it live. */
export const ViewCountAndUnstarUpdateLive = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} .favorites=${[FAV, OIDCC_BASIC]}></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    wireOptimistic(host);

    await step("enter the favorites view", async () => {
      await selectFavoritesView(host);
      await waitFor(() => expect(mainRows(host).length).toBe(2));
    });

    await step("unstarring a row removes it and decrements the count", async () => {
      await userEvent.click(starFor(host, FAV));
      await waitFor(() => {
        expect(mainRows(host).length).toBe(1);
        expect(starFor(host, FAV)).toBeFalsy();
        expect(viewOption(host).textContent).toContain("★ Favorites (1)");
      });
    });
  },
};

/** View Read (empty): the favorites view with no favorites shows its own copy. */
export const ViewEmptyStateCopy = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;

    await step("the empty view shows favorites-specific copy, not a plain miss", async () => {
      await selectFavoritesView(host);
      await waitFor(() => {
        const empty = host.querySelector(".oidf-test-selector__empty");
        expect(empty).toBeTruthy();
        expect(empty.textContent).toContain("No favorites yet");
        expect(empty.textContent).not.toContain("No plans match your search");
      });
    });
  },
};

/** Family ∩ favorites: picking a real family leaves the favorites view. */
export const ViewFamilySelectionExitsView = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} .favorites=${[FAV]}></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    const select = host.querySelector(".oidf-test-selector__family");

    await step("enter the view, then switch to a real family", async () => {
      await selectFavoritesView(host);
      await waitFor(() => expect(mainRows(host).length).toBe(1));
      await userEvent.selectOptions(select, "OIDCC");
      await waitFor(() => {
        // The OIDCC family has 3 plans; the favorites view is no longer active.
        expect(mainRows(host).length).toBe(3);
        expect(viewOption(host).selected).toBe(false);
      });
    });
  },
};

/** View loading: the saved-view option shows "(…)" until favorites arrive. */
export const ViewLoadingShowsEllipsisCount = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} favorites-loading></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    await step("the count placeholder reads … while favorites load", async () => {
      expect(viewOption(host).textContent).toContain("★ Favorites (…)");
    });
  },
};

/**
 * Stale favorite: a favorited planName missing from `plans` renders in the view
 * as a crossed-out, non-interactive row with an explicit remove control — but
 * only alongside a live favorite. It sits outside the arrow-roving index (it
 * carries no select button) and only the remove control is actionable.
 */
export const ViewStaleFavoriteIsRemovable = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} .favorites=${[FAV, STALE]}></cts-test-selector>
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

    await step("enter the view — the live favorite and the stale row both show", async () => {
      await selectFavoritesView(host);
      await waitFor(() => expect(mainRows(host).length).toBe(2));
    });

    await step("the stale row is crossed out, with a 'no longer available' note", async () => {
      const stale = host.querySelector(".oidf-test-selector__item--stale");
      expect(stale).toBeTruthy();
      expect(stale.textContent).toContain("No longer available");
      expect(stale.querySelector(".oidf-test-selector__row-name").textContent).toBe(STALE);
      // No select button → outside the roving index and the "f" shortcut.
      expect(stale.querySelector(".oidf-test-selector__row")).toBeNull();
    });

    await step("clicking the stale body selects nothing", async () => {
      await userEvent.click(host.querySelector(".oidf-test-selector__stale"));
      expect(selects.length).toBe(0);
    });

    await step("the remove control unpins it", async () => {
      const remove = host.querySelector(
        ".oidf-test-selector__item--stale .oidf-test-selector__fav",
      );
      expect(remove.getAttribute("aria-label")).toBe(`Remove favorite: ${STALE}`);
      await userEvent.click(remove);
      expect(events[0]).toEqual({ plan: STALE, favorite: false, via: "click" });
      await waitFor(() => {
        // Only the live favorite remains; the stale row is gone.
        expect(mainRows(host).length).toBe(1);
        expect(host.querySelector(".oidf-test-selector__item--stale")).toBeNull();
      });
    });
  },
};

/**
 * All favorites retired: when every favorite is missing from `plans`, the view
 * shows the "all unavailable" empty copy rather than a list of only-dead rows.
 */
export const ViewAllFavoritesRetiredShowsUnavailableCopy = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} .favorites=${[STALE]}></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;

    await step("the empty copy reads 'all unavailable', not 'no favorites yet'", async () => {
      await selectFavoritesView(host);
      await waitFor(() => {
        const empty = host.querySelector(".oidf-test-selector__empty");
        expect(empty).toBeTruthy();
        expect(empty.textContent).toContain("All your favorites are unavailable");
        expect(empty.textContent).not.toContain("No favorites yet");
        // No dead rows rendered.
        expect(host.querySelector(".oidf-test-selector__item--stale")).toBeNull();
      });
    });
  },
};

// ───────────────────────────────────────────────────────────────────────────
// Filter persistence + escape hatch + separator spacing.
// ───────────────────────────────────────────────────────────────────────────

/**
 * Filter persistence (U2): the selected filter is remembered across a remount,
 * because a freshly-mounted selector seeds its filter from
 * `cts:test-selector-filter`. Restoring a filter is not a plan pick, so it
 * never fires `cts-plan-select`.
 */
export const FilterChoicePersistsAcrossRemount = {
  render: () => html`
    <div class="reload-harness">
      <cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>
    </div>
  `,
  async play({ canvasElement, step }) {
    const harness = canvasElement.querySelector(".reload-harness");
    const first = harness.querySelector("cts-test-selector");
    await first.updateComplete;
    /** @type {any[]} */
    const selects = [];
    first.addEventListener("cts-plan-select", (e) =>
      selects.push(/** @type {CustomEvent} */ (e).detail),
    );

    await step("select the OIDCC family on the first mount", async () => {
      await userEvent.selectOptions(first.querySelector(".oidf-test-selector__family"), "OIDCC");
      await waitFor(() => expect(mainRows(first).length).toBe(3));
    });

    await step("a remounted selector restores the OIDCC filter from storage", async () => {
      first.remove();
      const second = /** @type {any} */ (document.createElement("cts-test-selector"));
      second.plans = MOCK_PLANS;
      harness.appendChild(second);
      await second.updateComplete;
      await waitFor(() => {
        expect(mainRows(second).length).toBe(3);
        expect(second.querySelector(".oidf-test-selector__family").value).toBe("OIDCC");
      });
      // Restoring a filter is not a plan pick — no selection event fired.
      expect(selects.length).toBe(0);
    });
  },
};

/**
 * Escape hatch (U3): a "Search all specifications" link appears under the
 * listbox whenever a non-"All" filter is active. Activating it clears the
 * filter (back to All), persists that, and drops focus into the search input.
 */
export const EscapeHatchClearsFilterAndFocusesSearch = {
  render: () => html`
    <cts-test-selector .plans=${MOCK_PLANS} .favorites=${[FAV]}></cts-test-selector>
  `,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    const searchInput = host.querySelector(".oidf-test-selector__search");

    await step("no escape hatch while 'All specifications' is active", async () => {
      expect(host.querySelector(".oidf-test-selector__escape")).toBeNull();
    });

    await step("selecting a family reveals the escape link (a real button)", async () => {
      await userEvent.selectOptions(host.querySelector(".oidf-test-selector__family"), "OIDCC");
      await waitFor(() => {
        const link = host.querySelector(".oidf-test-selector__escape-link");
        expect(link).toBeTruthy();
        expect(link.tagName).toBe("BUTTON");
        expect(link.textContent.trim()).toBe("Search all specifications");
      });
    });

    await step(
      "activating it restores the full list, clears the filter, focuses search",
      async () => {
        await userEvent.click(host.querySelector(".oidf-test-selector__escape-link"));
        await waitFor(() => {
          expect(mainRows(host).length).toBe(MOCK_PLANS.length);
          expect(host.querySelector(".oidf-test-selector__family").value).toBe("");
          expect(host.querySelector(".oidf-test-selector__escape")).toBeNull();
        });
        expect(document.activeElement).toBe(searchInput);
        // The cleared "All" state is persisted so it survives a reload.
        expect(JSON.parse(/** @type {string} */ (localStorage.getItem(FILTER_KEY)))).toEqual({
          filter: "",
        });
      },
    );

    await step("the escape hatch also appears for the ★ Favorites view", async () => {
      await selectFavoritesView(host);
      await waitFor(() =>
        expect(host.querySelector(".oidf-test-selector__escape-link")).toBeTruthy(),
      );
    });
  },
};

/**
 * Separator (U4): the ★ Favorites divider is a real <hr> in the listbox flow,
 * NOT a border on either adjacent option — an option's orange :checked fill
 * abuts its own border, so a border-divider would lose its breathing room
 * whenever that option is selected. The <hr> floats in the inter-option gap
 * with symmetric 4px white above and below in both states.
 */
export const SeparatorRendersAsHr = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;

    await step("an <hr> divider sits between the ★ Favorites and the first option", () => {
      const hr = host.querySelector(".oidf-test-selector__family-divider");
      expect(hr).toBeTruthy();
      expect(hr.tagName).toBe("HR");
      // Immediately follows the saved-view option in the listbox.
      expect(hr.previousElementSibling?.classList.contains("oidf-test-selector__family-view")).toBe(
        true,
      );
      // It is not a selectable option, so it never disturbs the family value.
      expect(host.querySelector(".oidf-test-selector__family").value).toBe("");
    });

    await step("the divider style is registered with 4px breathing above/below", () => {
      const css = document.getElementById("cts-test-selector-styles")?.textContent || "";
      expect(css).toContain(".oidf-test-selector__family-divider");
      expect(css).toContain("border-top: 1px solid var(--divider)");
      expect(css).toContain("margin: var(--space-1) var(--space-3)");
    });
  },
};

// ───────────────────────────────────────────────────────────────────────────
// Store-driven lifecycle: persist across reload + failure → revert + toast.
// These drive attachFavorites with an in-memory fake controller — the same
// wiring schedule-test.html uses against the real /api/favorite-plans server.
// ───────────────────────────────────────────────────────────────────────────

/**
 * Persist across reload: a favorite saved through the store survives a full
 * re-mount, because the remounted selector is seeded from the same store (the
 * server in production; the fake controller's in-memory state here).
 */
export const PersistsAcrossReload = {
  render: () => html`
    <div class="reload-harness">
      <cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>
    </div>
  `,
  async play({ canvasElement, step }) {
    const harness = canvasElement.querySelector(".reload-harness");
    const first = harness.querySelector("cts-test-selector");
    // One controller stands in for the server: its state survives the remount.
    const controller = createFakeFavoritesController();
    first.favorites = (await controller.get()).plans;
    const detach = attachFavorites(first, controller);
    await first.updateComplete;

    await step("starring a plan persists it through the store", async () => {
      await userEvent.click(starFor(first, NOT_FAV));
      await waitFor(() => expect(controller.snapshot()).toEqual([NOT_FAV]));
    });

    await step("a remounted selector re-seeded from the store shows it starred", async () => {
      detach();
      first.remove();
      // "Reload": a brand-new component, re-seeded from the same store (the
      // server still has the favorite).
      const second = /** @type {any} */ (document.createElement("cts-test-selector"));
      second.plans = MOCK_PLANS;
      second.favorites = (await controller.get()).plans;
      harness.appendChild(second);
      await second.updateComplete;
      const star = starFor(second, NOT_FAV);
      expect(star.getAttribute("aria-pressed")).toBe("true");
      expect(star.querySelector("cts-icon").getAttribute("name")).toBe("star-fill");
    });
  },
};

/**
 * Save failure: a rejected add reverts the optimistic star and raises a plain
 * error toast (KTD4 — no undo affordance; re-starring is the undo path).
 */
export const SaveFailureRevertsWithErrorToast = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    const toastSpy = spyOn(/** @type {any} */ (window), "ctsToast").mockImplementation(() => {});
    // Latency separates the optimistic state from the rejection so the
    // intermediate add is observable (with latency 0 both land in one flush).
    const controller = createFakeFavoritesController({ failOn: NOT_FAV, latency: 60 });
    host.favorites = (await controller.get()).plans;
    attachFavorites(host, controller);

    await step("the optimistic add lands on the prop, then reverts on failure", async () => {
      await userEvent.click(starFor(host, NOT_FAV));
      // The optimistic add is applied to the controlled prop synchronously.
      expect(host.favorites).toEqual([NOT_FAV]);
      // The rejected save reverts the prop and the star.
      await waitFor(() => expect(host.favorites).toEqual([]));
      await waitFor(() =>
        expect(starFor(host, NOT_FAV).getAttribute("aria-pressed")).toBe("false"),
      );
      // Storage was never written.
      expect(controller.snapshot()).toEqual([]);
    });

    await step("a kind:error toast is raised", () => {
      expect(toastSpy).toHaveBeenCalledTimes(1);
      expect(toastSpy).toHaveBeenCalledWith(expect.objectContaining({ kind: "error" }));
    });
  },
};

/**
 * Remove failure: a rejected delete restores the unstarred favorite and raises
 * an error toast.
 */
export const RemoveFailureRevertsWithErrorToast = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;
    const toastSpy = spyOn(/** @type {any} */ (window), "ctsToast").mockImplementation(() => {});
    // Seed one favorite, then make remove reject (latency separates the
    // optimistic remove from the rejection so the intermediate state shows).
    const controller = createFakeFavoritesController({
      failOn: (_n, op) => op === "remove",
      latency: 60,
    });
    await controller.add(FAV);
    host.favorites = (await controller.get()).plans;
    attachFavorites(host, controller);
    await host.updateComplete;

    await step("unstarring optimistically removes, then restores on failure", async () => {
      expect(starFor(host, FAV).getAttribute("aria-pressed")).toBe("true");
      await userEvent.click(starFor(host, FAV));
      // Optimistic remove drops it from the controlled prop...
      expect(host.favorites).toEqual([]);
      // ...then the rejected delete restores it.
      await waitFor(() => expect(host.favorites).toEqual([FAV]));
      expect(controller.snapshot()).toEqual([FAV]);
    });

    await step("a kind:error toast is raised", () => {
      expect(toastSpy).toHaveBeenCalledWith(expect.objectContaining({ kind: "error" }));
    });
  },
};
