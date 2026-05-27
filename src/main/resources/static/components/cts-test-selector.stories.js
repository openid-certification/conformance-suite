import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { MOCK_PLANS } from "@fixtures/mock-plans.js";
import "./cts-test-selector.js";

export default {
  title: "Components/cts-test-selector",
  component: "cts-test-selector",
};

export const Default = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    const searchInput = canvasElement.querySelector(".oidf-test-selector__search");
    expect(searchInput).toBeTruthy();
    expect(searchInput.getAttribute("placeholder")).toBe("Search test plans...");
    const select = canvasElement.querySelector(".oidf-test-selector__family");
    expect(select).toBeTruthy();
    // The family filter has no visible label in the toolbar — assistive tech relies on
    // the explicit aria-label for an accessible name.
    expect(select.getAttribute("aria-label")).toBe("Filter test plans by specification family");
    // Rendered as an always-open listbox (size attribute) rather than a
    // dropdown, so every spec family is visible in the left rail at once.
    expect(select.getAttribute("size")).toBe("14");
    // A sized listbox does not auto-select its first option, so the "All
    // specifications" option (value="") is selected explicitly by default.
    expect(select.value).toBe("");
    expect(select.selectedOptions[0]?.textContent?.trim()).toBe("All specifications");
    const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
    expect(items.length).toBe(MOCK_PLANS.length);
    expect(canvas.getByText("OpenID Connect Core: Basic Certification Profile")).toBeTruthy();
    // No Bootstrap remnants in the rendered output
    expect(canvasElement.querySelector(".form-control")).toBeNull();
    expect(canvasElement.querySelector(".form-select")).toBeNull();
    expect(canvasElement.querySelector(".list-group-item")).toBeNull();
  },
};

export const SearchFilter = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const searchInput = canvasElement.querySelector(".oidf-test-selector__search");
    await userEvent.type(searchInput, "FAPI");
    await waitFor(() => {
      const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
      expect(items.length).toBe(2);
    });
  },
};

/**
 * The trailing × button clears the search and refocuses the input.
 * Mirrors the simplified live-debounced shape of the cts-data-table
 * pattern — no submit affordance, since filtering happens as the user
 * types.
 */
export const SearchClearButton = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );

    // No clear button while the field is empty.
    expect(canvasElement.querySelector(".oidf-test-selector__search-clear")).toBeNull();

    await userEvent.type(searchInput, "FAPI");
    await waitFor(() => {
      const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
      expect(items.length).toBe(2);
    });

    // Clear button appears once the field has content.
    const clearBtn = /** @type {HTMLButtonElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search-clear")
    );
    expect(clearBtn).toBeTruthy();
    expect(clearBtn.getAttribute("aria-label")).toBe("Clear search");

    await userEvent.click(clearBtn);

    // Field is empty, all rows are back, clear button is gone, focus
    // is back on the input so the user can keep typing.
    await waitFor(() => {
      expect(searchInput.value).toBe("");
      const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
      expect(items.length).toBe(MOCK_PLANS.length);
    });
    expect(canvasElement.querySelector(".oidf-test-selector__search-clear")).toBeNull();
    expect(document.activeElement).toBe(searchInput);
  },
};

/**
 * Pressing Escape inside the search input clears it. Same affordance
 * as the clear button, exposed to keyboard users without reaching for
 * the mouse.
 */
export const SearchEscapeClears = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );

    await userEvent.type(searchInput, "FAPI");
    await waitFor(() => {
      const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
      expect(items.length).toBe(2);
    });

    searchInput.focus();
    await userEvent.keyboard("{Escape}");

    await waitFor(() => {
      expect(searchInput.value).toBe("");
      const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
      expect(items.length).toBe(MOCK_PLANS.length);
    });
  },
};

export const FamilyFilter = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const select = canvasElement.querySelector(".oidf-test-selector__family");
    await userEvent.selectOptions(select, "OIDCC");
    await waitFor(() => {
      const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
      expect(items.length).toBe(3);
    });
  },
};

export const SelectPlan = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    /** @type {any[]} */
    const dispatched = [];
    canvasElement.addEventListener("cts-plan-select", (e) => {
      dispatched.push(/** @type {CustomEvent} */ (e).detail);
    });
    const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
    await userEvent.click(items[1]);
    expect(dispatched.length).toBe(1);
    expect(dispatched[0].plan.planName).toBe("oidcc-implicit-certification-test-plan");
    // The page-level listener in schedule-test.html branches on `via` to
    // decide whether to steal focus into #specCascade. Mouse clicks must
    // stay polite (no focus shift), so the channel carries 'click'.
    expect(dispatched[0].via).toBe("click");
    await waitFor(() => {
      expect(items[1].classList.contains("is-active")).toBe(true);
    });
  },
};

/**
 * ArrowDown in the search input moves real DOM focus into the first
 * visible row. This is the entry point of the keyboard-only flow that
 * lets a user search → arrow → Enter without touching the mouse.
 */
export const ArrowDownFromSearchFocusesFirstRow = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );
    searchInput.focus();
    await userEvent.keyboard("{ArrowDown}");

    await waitFor(() => {
      const firstRow = canvasElement.querySelector(".oidf-test-selector__row");
      expect(document.activeElement).toBe(firstRow);
      // Roving tabindex: the focused row carries tabindex=0; the rest
      // are -1 so Tab escapes the list cleanly.
      expect(firstRow?.getAttribute("tabindex")).toBe("0");
    });
  },
};

/**
 * Once focus is on a row, ArrowDown/ArrowUp rove across rows in
 * document order. ArrowDown on the last row is a no-op (no wrap).
 */
export const ArrowNavRovesAcrossRows = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );
    searchInput.focus();
    await userEvent.keyboard("{ArrowDown}");

    const rows = canvasElement.querySelectorAll(".oidf-test-selector__row");
    await waitFor(() => expect(document.activeElement).toBe(rows[0]));

    await userEvent.keyboard("{ArrowDown}");
    await waitFor(() => expect(document.activeElement).toBe(rows[1]));

    await userEvent.keyboard("{ArrowUp}");
    await waitFor(() => expect(document.activeElement).toBe(rows[0]));
  },
};

/**
 * ArrowUp on the first row returns focus to the search input — closing
 * the loop so the user can refine the query without reaching for the
 * mouse or hammering Shift+Tab through all the rows above.
 */
export const ArrowUpFromFirstRowReturnsToSearch = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );
    searchInput.focus();
    await userEvent.keyboard("{ArrowDown}");

    const firstRow = canvasElement.querySelector(".oidf-test-selector__row");
    await waitFor(() => expect(document.activeElement).toBe(firstRow));

    await userEvent.keyboard("{ArrowUp}");
    await waitFor(() => expect(document.activeElement).toBe(searchInput));
  },
};

/**
 * Pressing Enter on a focused row commits the selection and tags the
 * dispatched event with via:'keyboard'. The page-level listener uses
 * that channel to decide whether to advance focus into #specCascade.
 *
 * Asserts exactly one dispatch — guards against the keyup→synthetic-click
 * double-fire that <button> elements produce on keyboard activation.
 */
export const EnterOnFocusedRowSelects = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    /** @type {any[]} */
    const dispatched = [];
    canvasElement.addEventListener("cts-plan-select", (e) => {
      dispatched.push(/** @type {CustomEvent} */ (e).detail);
    });

    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );
    searchInput.focus();
    await userEvent.keyboard("{ArrowDown}");

    const firstRow = canvasElement.querySelector(".oidf-test-selector__row");
    await waitFor(() => expect(document.activeElement).toBe(firstRow));

    await userEvent.keyboard("{Enter}");

    await waitFor(() => {
      expect(dispatched.length).toBe(1);
      expect(dispatched[0].plan.planName).toBe(MOCK_PLANS[0].planName);
      expect(dispatched[0].via).toBe("keyboard");
    });
  },
};

/**
 * When the filter clears the result list, ArrowDown in the search has
 * nothing to focus and must be a no-op — no event, no focus change,
 * no thrown error from indexing into an empty list.
 */
export const ArrowDownOnEmptyListIsNoOp = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    /** @type {any[]} */
    const dispatched = [];
    canvasElement.addEventListener("cts-plan-select", (e) => {
      dispatched.push(/** @type {CustomEvent} */ (e).detail);
    });

    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );
    await userEvent.type(searchInput, "nonexistent-plan-xyz");
    await waitFor(() => {
      const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
      expect(items.length).toBe(0);
    });

    searchInput.focus();
    await userEvent.keyboard("{ArrowDown}");

    expect(document.activeElement).toBe(searchInput);
    expect(dispatched.length).toBe(0);
  },
};

/**
 * The `selected` attribute is the externally-driven counterpart of the click
 * path: callers (e.g. schedule-test.html bridging cts-spec-cascade's
 * `cts-plan-selected` back to the search list) set `planSearch.selected = name`
 * to highlight the matching row without triggering a `cts-plan-select` event.
 * This story exercises that path independently of the click handler so the
 * highlight contract is locked even when the user picks via the cascade
 * dropdown.
 */
export const WithSelection = {
  render: () => html`
    <cts-test-selector
      .plans=${MOCK_PLANS}
      selected="fapi2-security-profile-final-test-plan"
    ></cts-test-selector>
  `,
  async play({ canvasElement }) {
    // The externally-driven path must not fabricate a synthetic
    // `cts-plan-select` — that event signals "user picked this plan."
    // schedule-test.html routes click events through cascade.selectPlanByName,
    // which clears the in-flight config. If WithSelection re-fired the event
    // every time a caller set `selected`, the bridge would wipe the config
    // any time the cascade pushed a highlight back to the selector.
    let dispatched = 0;
    canvasElement.addEventListener("cts-plan-select", () => {
      dispatched += 1;
    });

    const rows = canvasElement.querySelectorAll(".oidf-test-selector__row");
    const activeRows = Array.from(rows).filter((r) => r.classList.contains("is-active"));
    expect(activeRows.length).toBe(1);
    expect(activeRows[0].getAttribute("data-plan-name")).toBe(
      "fapi2-security-profile-final-test-plan",
    );
    expect(dispatched).toBe(0);
  },
};

/**
 * Hover swaps the row background to `--ink-50`. We can't observe the actual
 * pseudo-class style from JSDOM, but we can verify the rule is registered in
 * the injected stylesheet so a regression in the head-injection pipeline is
 * caught.
 */
export const RowHoverStyleRegistered = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play() {
    const styleEl = document.getElementById("cts-test-selector-styles");
    expect(styleEl).toBeTruthy();
    const css = styleEl?.textContent || "";
    expect(css).toContain(".oidf-test-selector__row:hover");
    expect(css).toContain("var(--ink-50)");
    expect(css).toContain(".oidf-test-selector__row:focus-visible");
    expect(css).toContain("var(--focus-ring)");
  },
};

/**
 * The family filter renders as a listbox (size attribute), so long spec
 * names must wrap instead of clipping to one row. We can't read the
 * computed option layout from the test runner, but we can assert the
 * wrapping rule is present in the injected stylesheet so a regression in
 * the head-injection pipeline is caught.
 */
export const FamilyListboxStyleRegistered = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play() {
    const styleEl = document.getElementById("cts-test-selector-styles");
    expect(styleEl).toBeTruthy();
    const css = styleEl?.textContent || "";
    expect(css).toContain(".oidf-test-selector__family option");
    expect(css).toContain("white-space: normal");
  },
};

export const NoResults = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    const searchInput = canvasElement.querySelector(".oidf-test-selector__search");
    await userEvent.type(searchInput, "nonexistent-plan-xyz");
    await waitFor(() => {
      expect(canvas.getByText("No plans match your search")).toBeTruthy();
    });
  },
};

export const EmptyPlans = {
  render: () => html`<cts-test-selector .plans=${[]}></cts-test-selector>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    expect(canvas.getByText("No plans match your search")).toBeTruthy();
  },
};

export const ModuleCount = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const badges = canvasElement.querySelectorAll(".oidf-test-selector__row-count");
    expect(badges.length).toBeGreaterThan(0);
    expect(badges[0].textContent).toBe("4");
  },
};
