import { html } from "lit";
import { expect } from "storybook/test";
import "../../../src/main/resources/static/components/cts-tooltip.js";

export default {
  title: "Primitives/cts-tooltip",
  component: "cts-tooltip",
};

// --- Stories ---

export const Default = {
  render: () => html`
    <cts-tooltip content="Click to copy the share URL" placement="top">
      <button class="btn btn-sm btn-light bg-gradient border border-secondary">
        <span class="bi bi-files" aria-hidden="true"></span> Copy
      </button>
    </cts-tooltip>
  `,

  async play({ canvasElement }) {
    const button = canvasElement.querySelector("button");
    expect(button).toBeTruthy();
    expect(button.getAttribute("data-bs-toggle")).toBe("tooltip");
    expect(button.getAttribute("data-bs-placement")).toBe("top");
    expect(button.getAttribute("title")).toBe("Click to copy the share URL");
  },
};

export const BottomPlacement = {
  render: () => html`
    <cts-tooltip content="Visit the OpenID Foundation" placement="bottom">
      <a href="https://openid.net" class="btn btn-sm btn-outline-primary"
        >OpenID Foundation</a
      >
    </cts-tooltip>
  `,

  async play({ canvasElement }) {
    const anchor = canvasElement.querySelector("a");
    expect(anchor).toBeTruthy();
    expect(anchor.getAttribute("data-bs-placement")).toBe("bottom");
    expect(anchor.getAttribute("data-bs-toggle")).toBe("tooltip");
    expect(anchor.getAttribute("title")).toBe("Visit the OpenID Foundation");
  },
};

export const NoContent = {
  render: () => html`
    <cts-tooltip>
      <button class="btn btn-sm btn-secondary">No tooltip</button>
    </cts-tooltip>
  `,

  async play({ canvasElement }) {
    const button = canvasElement.querySelector("button");
    expect(button).toBeTruthy();
    expect(button.hasAttribute("data-bs-toggle")).toBe(false);
  },
};
