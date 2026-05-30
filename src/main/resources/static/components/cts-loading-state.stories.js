import { html } from "lit";
import { expect } from "storybook/test";
import "./cts-loading-state.js";

export default {
  title: "Patterns/cts-loading-state",
  component: "cts-loading-state",
  argTypes: {
    label: { control: "text" },
  },
};

// --- Stories ---

/**
 * Happy path: spinner above a caption. The caption appends an `…` ellipsis to
 * the supplied label; the spinner carries the bare label as its accessible
 * name (role="status").
 */
export const Default = {
  args: {
    label: "Loading logs",
  },
  render: ({ label }) => html`<cts-loading-state label=${label}></cts-loading-state>`,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-loading-state");
    await host.updateComplete;

    const wrapper = host.querySelector(".cts-loading-state");
    expect(wrapper).toBeTruthy();

    // Spinner renders with the bare label as its accessible name.
    const spinner = host.querySelector("cts-spinner");
    expect(spinner).toBeTruthy();
    expect(spinner.getAttribute("label")).toBe("Loading logs");
    expect(spinner.getAttribute("role")).toBe("status");

    // Visible caption is the label plus an ellipsis.
    const caption = host.querySelector(".cts-loading-state-caption");
    expect(caption).toBeTruthy();
    expect(caption.textContent.trim()).toBe("Loading logs…");
  },
};

/**
 * Default label: when no `label` is supplied the component falls back to
 * "Loading" for both the accessible name and the visible caption.
 */
export const DefaultLabel = {
  render: () => html`<cts-loading-state></cts-loading-state>`,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-loading-state");
    await host.updateComplete;

    expect(host.querySelector("cts-spinner").getAttribute("label")).toBe("Loading");
    expect(host.querySelector(".cts-loading-state-caption").textContent.trim()).toBe("Loading…");
  },
};

/**
 * The two real call sites: cts-log-list ("Loading logs") and cts-plan-list
 * ("Loading test plans"). Rendering both side by side documents that they
 * share one component and differ only by caption text.
 */
export const CallSites = {
  render: () => html`
    <cts-loading-state label="Loading logs"></cts-loading-state>
    <cts-loading-state label="Loading test plans"></cts-loading-state>
  `,

  async play({ canvasElement }) {
    const hosts = canvasElement.querySelectorAll("cts-loading-state");
    expect(hosts.length).toBe(2);
    for (const host of hosts) await host.updateComplete;
    expect(hosts[0].querySelector(".cts-loading-state-caption").textContent.trim()).toBe(
      "Loading logs…",
    );
    expect(hosts[1].querySelector(".cts-loading-state-caption").textContent.trim()).toBe(
      "Loading test plans…",
    );
  },
};
