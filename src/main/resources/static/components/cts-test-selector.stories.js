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
    /** @type {any} */
    let selectedPlan = null;
    canvasElement.addEventListener("cts-plan-select", (e) => {
      selectedPlan = /** @type {CustomEvent} */ (e).detail.plan;
    });
    const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
    await userEvent.click(items[1]);
    expect(selectedPlan).toBeTruthy();
    expect(selectedPlan.planName).toBe("oidcc-implicit-certification-test-plan");
    await waitFor(() => {
      expect(items[1].classList.contains("is-active")).toBe(true);
    });
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
