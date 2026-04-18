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
    const searchInput = canvasElement.querySelector('input[placeholder="Search test plans..."]');
    expect(searchInput).toBeTruthy();
    const select = canvasElement.querySelector("select.form-select");
    expect(select).toBeTruthy();
    const items = canvasElement.querySelectorAll(".list-group-item");
    expect(items.length).toBe(MOCK_PLANS.length);
    expect(canvas.getByText("OpenID Connect Core: Basic Certification Profile")).toBeTruthy();
  },
};

export const SearchFilter = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const searchInput = canvasElement.querySelector('input[placeholder="Search test plans..."]');
    await userEvent.type(searchInput, "FAPI");
    await waitFor(() => {
      const items = canvasElement.querySelectorAll(".list-group-item-action");
      expect(items.length).toBe(2);
    });
  },
};

export const FamilyFilter = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const select = canvasElement.querySelector("select.form-select");
    await userEvent.selectOptions(select, "OIDCC");
    await waitFor(() => {
      const items = canvasElement.querySelectorAll(".list-group-item-action");
      expect(items.length).toBe(3);
    });
  },
};

export const SelectPlan = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    let selectedPlan = null;
    canvasElement.addEventListener("cts-plan-select", (e) => {
      selectedPlan = e.detail.plan;
    });
    const items = canvasElement.querySelectorAll(".list-group-item-action");
    await userEvent.click(items[1]);
    expect(selectedPlan).toBeTruthy();
    expect(selectedPlan.planName).toBe("oidcc-implicit-certification-test-plan");
    await waitFor(() => {
      expect(items[1].classList.contains("active")).toBe(true);
    });
  },
};

export const NoResults = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    const searchInput = canvasElement.querySelector('input[placeholder="Search test plans..."]');
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
    const badges = canvasElement.querySelectorAll(".badge.bg-secondary.rounded-pill");
    expect(badges.length).toBeGreaterThan(0);
    expect(badges[0].textContent).toBe("4");
  },
};
