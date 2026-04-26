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

// Helper: locate the rendered SVG inside a cts-icon host. The component
// uses light DOM, so the SVG is a direct descendant of the host element.
const findIconSvg = (canvasElement) =>
  canvasElement.querySelector("cts-icon svg[data-cts-icon-size]");

// --- Stories ---

export const Default = {
  args: { name: "play", size: "20" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    const svg = findIconSvg(canvasElement);
    expect(svg).toBeTruthy();
    expect(svg.getAttribute("aria-hidden")).toBe("true");
    expect(svg.getAttribute("data-cts-icon-size")).toBe("20");
    expect(svg.getAttribute("viewBox")).toBe("0 0 24 24");

    const use = svg.querySelector("use");
    expect(use).toBeTruthy();
    expect(use.getAttribute("href")).toBe("/vendor/coolicons/icons/play.svg#i");

    // Default size resolves to 20px from --space-5.
    const computed = getComputedStyle(svg);
    expect(computed.width).toBe("20px");
    expect(computed.height).toBe("20px");
  },
};

export const Size16 = {
  args: { name: "search-magnifying-glass", size: "16" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    const svg = findIconSvg(canvasElement);
    expect(svg).toBeTruthy();
    expect(svg.getAttribute("data-cts-icon-size")).toBe("16");
    expect(svg.querySelector("use").getAttribute("href")).toBe(
      "/vendor/coolicons/icons/search-magnifying-glass.svg#i",
    );

    const computed = getComputedStyle(svg);
    expect(computed.width).toBe("16px");
    expect(computed.height).toBe("16px");
  },
};

export const Size20 = {
  args: { name: "edit-pencil-01", size: "20" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    const svg = findIconSvg(canvasElement);
    expect(svg).toBeTruthy();
    expect(svg.getAttribute("data-cts-icon-size")).toBe("20");
    expect(svg.querySelector("use").getAttribute("href")).toBe(
      "/vendor/coolicons/icons/edit-pencil-01.svg#i",
    );

    const computed = getComputedStyle(svg);
    expect(computed.width).toBe("20px");
    expect(computed.height).toBe("20px");
  },
};

export const Size24 = {
  args: { name: "trash-empty", size: "24" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    const svg = findIconSvg(canvasElement);
    expect(svg).toBeTruthy();
    expect(svg.getAttribute("aria-hidden")).toBe("true");
    expect(svg.getAttribute("data-cts-icon-size")).toBe("24");
    expect(svg.querySelector("use").getAttribute("href")).toBe(
      "/vendor/coolicons/icons/trash-empty.svg#i",
    );

    const computed = getComputedStyle(svg);
    expect(computed.width).toBe("24px");
    expect(computed.height).toBe("24px");
  },
};

export const InheritsCurrentColor = {
  args: { name: "info", size: "20" },
  render: ({ name, size }) => html`
    <span style="color: rgb(255, 0, 0);">
      <cts-icon name="${name}" size="${size}"></cts-icon>
    </span>
  `,

  async play({ canvasElement }) {
    const svg = findIconSvg(canvasElement);
    expect(svg).toBeTruthy();
    // currentColor resolves to the parent's red. The per-icon SVG's path
    // declares stroke="currentColor", so the stroke colour follows the
    // host's `color`.
    expect(getComputedStyle(svg).color).toBe("rgb(255, 0, 0)");
  },
};

export const MissingName = {
  args: { name: "", size: "20" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    // Empty name renders nothing — no SVG, no broken markup.
    const host = canvasElement.querySelector("cts-icon");
    expect(host).toBeTruthy();
    expect(host.querySelector("svg")).toBeNull();
  },
};

export const LegacySizeAliases = {
  args: { name: "files", size: "lg" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement }) {
    // Legacy `lg` maps to the new `24` size for back-compat with templates
    // that pre-date the OIDF design-system retokenization.
    const svg = findIconSvg(canvasElement);
    expect(svg).toBeTruthy();
    expect(svg.getAttribute("data-cts-icon-size")).toBe("24");
    expect(getComputedStyle(svg).width).toBe("24px");
  },
};

// AllIcons: a grid of every coolicons file currently vendored. The list
// matches the actual filenames in
// src/main/resources/static/vendor/coolicons/icons/, so adding a new icon
// to that directory means appending the filename here. The story doubles
// as a discoverability surface (browseable catalog) and as a smoke test
// (any broken file shows up as an empty grid cell).
const VENDORED_ICON_NAMES = [
  "arrow-circle-down",
  "arrow-down-md",
  "arrow-left-md",
  "arrow-right-md",
  "arrow-undo-down-left",
  "arrow-up-md",
  "bookmark",
  "camera",
  "chevron-down",
  "chevron-left",
  "chevron-right",
  "chevron-up",
  "circle-check",
  "circle-help",
  "close-circle",
  "close-lg",
  "close-md",
  "cloud-upload",
  "copy",
  "edit-pencil-01",
  "external-link",
  "file-blank",
  "files",
  "globe",
  "info",
  "log-out",
  "paper-plane",
  "search-magnifying-glass",
  "settings",
  "shield-check",
  "trash-empty",
  "triangle-warning",
  "user-01",
];

export const AllIcons = {
  render: () => html`
    <div
      style="display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 1rem; padding: 1rem;"
    >
      ${VENDORED_ICON_NAMES.map(
        (iconName) => html`
          <figure
            style="display: flex; flex-direction: column; align-items: center; gap: 0.25rem; margin: 0; text-align: center;"
          >
            <cts-icon name="${iconName}" size="24"></cts-icon>
            <figcaption style="font-size: 0.75rem; word-break: break-all; color: #555;">
              ${iconName}
            </figcaption>
          </figure>
        `,
      )}
    </div>
  `,

  async play({ canvasElement }) {
    const figures = canvasElement.querySelectorAll("figure");
    expect(figures.length).toBe(VENDORED_ICON_NAMES.length);

    // Every figure has a cts-icon whose rendered <use> points at a real
    // file in /vendor/coolicons/icons/.
    const allHaveUseHref = Array.from(figures).every((fig) => {
      const use = fig.querySelector("cts-icon svg use");
      const href = use?.getAttribute("href") ?? "";
      return href.startsWith("/vendor/coolicons/icons/") && href.endsWith(".svg#i");
    });
    expect(allHaveUseHref).toBe(true);
  },
};
