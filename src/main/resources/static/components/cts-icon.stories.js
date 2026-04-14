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
      options: ["sm", "md", "lg"],
    },
  },
};

// --- Stories ---

export const Default = {
  args: { name: "play-fill", size: "md" },
  render: ({ name, size }) =>
    html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    const icon = canvasElement.querySelector("span.bi");
    expect(icon).toBeTruthy();
    expect(icon.classList.contains("bi-play-fill")).toBe(true);
    expect(icon.getAttribute("aria-hidden")).toBe("true");

    // "md" size has no extra size class
    expect(icon.classList.contains("fs-4")).toBe(false);
    expect(icon.classList.contains("fs-6")).toBe(false);
  },
};

export const Small = {
  args: { name: "search", size: "sm" },
  render: ({ name, size }) =>
    html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    const icon = canvasElement.querySelector("span.bi");
    expect(icon).toBeTruthy();
    expect(icon.classList.contains("bi-search")).toBe(true);
    expect(icon.classList.contains("fs-6")).toBe(true);
    expect(icon.getAttribute("aria-hidden")).toBe("true");
  },
};

export const Large = {
  args: { name: "trash", size: "lg" },
  render: ({ name, size }) =>
    html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    const icon = canvasElement.querySelector("span.bi");
    expect(icon).toBeTruthy();
    expect(icon.classList.contains("bi-trash")).toBe(true);
    expect(icon.classList.contains("fs-4")).toBe(true);
    expect(icon.getAttribute("aria-hidden")).toBe("true");
  },
};

export const MissingName = {
  args: { name: "", size: "md" },
  render: ({ name, size }) =>
    html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    const icon = canvasElement.querySelector("span.bi");
    expect(icon).toBeNull();
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
            <cts-icon name="${iconName}" size="lg"></cts-icon>
            <small style="font-size: 0.65rem; word-break: break-all;"
              >${iconName}</small
            >
          </div>
        `,
      )}
    </div>
  `,
};
