import { html } from "lit";
import { expect, userEvent, waitFor } from "storybook/test";
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

    // Bootstrap moves title to data-bs-original-title after initialization
    const tooltipText =
      button.getAttribute("data-bs-original-title") ||
      button.getAttribute("title");
    expect(tooltipText).toBe("Click to copy the share URL");

    // Verify Bootstrap actually initialized the tooltip instance
    const instance = bootstrap.Tooltip.getInstance(button);
    expect(instance).toBeTruthy();
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

    // Verify Bootstrap tooltip is initialized
    const instance = bootstrap.Tooltip.getInstance(anchor);
    expect(instance).toBeTruthy();
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
    // No content attribute means no tooltip should be initialized
    expect(button.hasAttribute("data-bs-toggle")).toBe(false);
  },
};

export const TooltipAppearsOnHover = {
  render: () => html`
    <div style="padding: 60px;">
      <cts-tooltip content="Hover tooltip text" placement="top">
        <button class="btn btn-primary">Hover me</button>
      </cts-tooltip>
    </div>
  `,

  async play({ canvasElement }) {
    const button = canvasElement.querySelector("button");

    // No tooltip visible initially
    expect(document.querySelector(".tooltip.show")).toBeNull();

    // Hover to trigger tooltip
    await userEvent.hover(button);
    await waitFor(() => {
      expect(document.querySelector(".tooltip.show")).toBeTruthy();
    });

    // Unhover to dismiss
    await userEvent.unhover(button);
    await waitFor(() => {
      expect(document.querySelector(".tooltip.show")).toBeNull();
    });
  },
};
