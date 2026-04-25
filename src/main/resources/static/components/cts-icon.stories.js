import { html } from "lit";
import { expect } from "storybook/test";
import "./cts-icon.js";

export default {
  title: "Primitives/cts-icon",
  component: "cts-icon",
  argTypes: {
    name: { control: "text" },
    size: {
      control: "select",
      options: ["16", "20", "24"],
    },
  },
};

// --- Stories ---

export const Default = {
  args: { name: "play-fill", size: "20" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    const icon = canvasElement.querySelector("span.bi");
    expect(icon).toBeTruthy();
    expect(icon.classList.contains("bi-play-fill")).toBe(true);
    expect(icon.getAttribute("aria-hidden")).toBe("true");
    expect(icon.getAttribute("data-cts-icon-size")).toBe("20");

    // Default size resolves to 20px from --space-5.
    const computed = getComputedStyle(icon);
    expect(computed.fontSize).toBe("20px");
    expect(computed.width).toBe("20px");
    expect(computed.height).toBe("20px");
  },
};

export const Size16 = {
  args: { name: "search", size: "16" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    const icon = canvasElement.querySelector("span.bi");
    expect(icon).toBeTruthy();
    expect(icon.classList.contains("bi-search")).toBe(true);
    expect(icon.getAttribute("aria-hidden")).toBe("true");
    expect(icon.getAttribute("data-cts-icon-size")).toBe("16");

    const computed = getComputedStyle(icon);
    expect(computed.fontSize).toBe("16px");
    expect(computed.width).toBe("16px");
    expect(computed.height).toBe("16px");
  },
};

export const Size20 = {
  args: { name: "pencil-square", size: "20" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    const icon = canvasElement.querySelector("span.bi");
    expect(icon).toBeTruthy();
    expect(icon.classList.contains("bi-pencil-square")).toBe(true);
    expect(icon.getAttribute("data-cts-icon-size")).toBe("20");

    const computed = getComputedStyle(icon);
    expect(computed.fontSize).toBe("20px");
    expect(computed.width).toBe("20px");
    expect(computed.height).toBe("20px");
  },
};

export const Size24 = {
  args: { name: "trash", size: "24" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    const icon = canvasElement.querySelector("span.bi");
    expect(icon).toBeTruthy();
    expect(icon.classList.contains("bi-trash")).toBe(true);
    expect(icon.getAttribute("aria-hidden")).toBe("true");
    expect(icon.getAttribute("data-cts-icon-size")).toBe("24");

    const computed = getComputedStyle(icon);
    expect(computed.fontSize).toBe("24px");
    expect(computed.width).toBe("24px");
    expect(computed.height).toBe("24px");
  },
};

export const InheritsCurrentColor = {
  args: { name: "info-circle-fill", size: "20" },
  render: ({ name, size }) => html`
    <span style="color: rgb(255, 0, 0);">
      <cts-icon name="${name}" size="${size}"></cts-icon>
    </span>
  `,

  async play({ canvasElement }) {
    const icon = canvasElement.querySelector("span.bi");
    expect(icon).toBeTruthy();
    // currentColor resolves to the parent's red.
    expect(getComputedStyle(icon).color).toBe("rgb(255, 0, 0)");
  },
};

export const MissingName = {
  args: { name: "", size: "20" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    // No span at all — including no broken `bi bi-` class.
    const icon = canvasElement.querySelector("span.bi");
    expect(icon).toBeNull();
    const host = canvasElement.querySelector("cts-icon");
    expect(host).toBeTruthy();
    expect(host.querySelector("span")).toBeNull();
  },
};

export const LegacySizeAliases = {
  args: { name: "save2", size: "lg" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    // Legacy `lg` maps to the new `24` size for back-compat with templates
    // that pre-date the U6 retokenization.
    const icon = canvasElement.querySelector("span.bi");
    expect(icon).toBeTruthy();
    expect(icon.getAttribute("data-cts-icon-size")).toBe("24");
    expect(getComputedStyle(icon).fontSize).toBe("24px");
  },
};

export const AllIcons = {
  render: () => html`
    <div style="display: flex; flex-wrap: wrap; gap: 1rem; padding: 1rem;">
      ${[
        "play-fill",
        "search",
        "trash",
        "pencil-square",
        "files",
        "save2",
        "wrench-adjustable",
        "chevron-down",
        "arrow-right",
        "question-circle-fill",
        "info-circle-fill",
        "box-arrow-in-right",
        "link-45deg",
        "send-fill",
      ].map(
        (iconName) => html`
          <div
            style="display: flex; flex-direction: column; align-items: center; gap: 0.25rem; width: 5rem; text-align: center;"
          >
            <cts-icon name="${iconName}" size="24"></cts-icon>
            <small style="font-size: 0.65rem; word-break: break-all;">${iconName}</small>
          </div>
        `,
      )}
    </div>
  `,
};
