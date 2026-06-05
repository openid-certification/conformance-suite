import { html } from "lit";
import { expect, fn, waitFor } from "storybook/test";
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

  async play({ canvasElement, step }) {
    const svg = findIconSvg(canvasElement);

    await step("svg renders with the expected attributes", async () => {
      expect(svg).toBeTruthy();
      expect(svg.getAttribute("aria-hidden")).toBe("true");
      expect(svg.getAttribute("data-cts-icon-size")).toBe("20");
      expect(svg.getAttribute("viewBox")).toBe("0 0 24 24");
    });

    await step("use element points at the vendored svg", async () => {
      const use = svg.querySelector("use");
      expect(use).toBeTruthy();
      expect(use.getAttribute("href")).toBe("/vendor/coolicons/icons/play.svg#i");
    });

    await step("default size resolves to 20px from --space-5", async () => {
      const computed = getComputedStyle(svg);
      expect(computed.width).toBe("20px");
      expect(computed.height).toBe("20px");
    });
  },
};

export const Size16 = {
  args: { name: "search-magnifying-glass", size: "16" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement, step }) {
    const svg = findIconSvg(canvasElement);

    await step("svg renders with the 16px size attribute and vendored href", async () => {
      expect(svg).toBeTruthy();
      expect(svg.getAttribute("data-cts-icon-size")).toBe("16");
      expect(svg.querySelector("use").getAttribute("href")).toBe(
        "/vendor/coolicons/icons/search-magnifying-glass.svg#i",
      );
    });

    await step("computed dimensions resolve to 16px", async () => {
      const computed = getComputedStyle(svg);
      expect(computed.width).toBe("16px");
      expect(computed.height).toBe("16px");
    });
  },
};

export const Size20 = {
  args: { name: "edit-pencil-01", size: "20" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement, step }) {
    const svg = findIconSvg(canvasElement);

    await step("svg renders with the 20px size attribute and vendored href", async () => {
      expect(svg).toBeTruthy();
      expect(svg.getAttribute("data-cts-icon-size")).toBe("20");
      expect(svg.querySelector("use").getAttribute("href")).toBe(
        "/vendor/coolicons/icons/edit-pencil-01.svg#i",
      );
    });

    await step("computed dimensions resolve to 20px", async () => {
      const computed = getComputedStyle(svg);
      expect(computed.width).toBe("20px");
      expect(computed.height).toBe("20px");
    });
  },
};

export const Size24 = {
  args: { name: "trash-empty", size: "24" },
  render: ({ name, size }) => html`<cts-icon name="${name}" size="${size}"></cts-icon>`,

  async play({ canvasElement, step }) {
    const svg = findIconSvg(canvasElement);

    await step("svg renders with the 24px size attribute and vendored href", async () => {
      expect(svg).toBeTruthy();
      expect(svg.getAttribute("aria-hidden")).toBe("true");
      expect(svg.getAttribute("data-cts-icon-size")).toBe("24");
      expect(svg.querySelector("use").getAttribute("href")).toBe(
        "/vendor/coolicons/icons/trash-empty.svg#i",
      );
    });

    await step("computed dimensions resolve to 24px", async () => {
      const computed = getComputedStyle(svg);
      expect(computed.width).toBe("24px");
      expect(computed.height).toBe("24px");
    });
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

// AllIcons: the actively-used subset of vendored coolicons — every icon
// that production templates, Lit components, dashboard configs, and
// stories currently render. The full vendored set under
// src/main/resources/static/vendor/coolicons/icons/ is much larger
// (~440 files); this catalog is the curated browseable surface for the
// icons the app actually uses. When a feature starts rendering a new
// icon, append it here in alphabetical position. The story doubles as a
// discoverability surface and as a smoke test — any broken file shows
// up as an empty grid cell, and the play assertion below verifies every
// entry resolves to a real /vendor/coolicons/icons/<name>.svg#i URL.
const VENDORED_ICON_NAMES = [
  "arrow-circle-down",
  "arrow-circle-up",
  "arrow-down-md",
  "arrow-down-up",
  "arrow-left-md",
  "arrow-reload-02",
  "arrow-right-md",
  "arrow-undo-down-left",
  "arrow-up-md",
  "arrows-reload-01",
  "book",
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
  "file-add",
  "file-blank",
  "files",
  "flag",
  "folder-open",
  "globe",
  "image-01",
  "info",
  "label",
  "link",
  "lock",
  "log-out",
  "paper-plane",
  "play",
  "save",
  "search-magnifying-glass",
  "settings",
  "shield-check",
  "skip-forward",
  "stop",
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

  async play({ canvasElement, step }) {
    const figures = canvasElement.querySelectorAll("figure");
    const hrefs = Array.from(figures).map((fig) => {
      const use = fig.querySelector("cts-icon svg use");
      return use?.getAttribute("href") ?? "";
    });

    await step("renders one figure per curated icon", async () => {
      expect(figures.length).toBe(VENDORED_ICON_NAMES.length);
    });

    await step("every href has the vendored coolicons URL shape", async () => {
      expect(
        hrefs.every((h) => h.startsWith("/vendor/coolicons/icons/") && h.endsWith(".svg#i")),
      ).toBe(true);
    });

    await step("every icon resolves to a real vendored SVG file", async () => {
      // Fetch each URL with a HEAD request and assert 200 OK so the smoke
      // test catches the case where the curated list drifts past a renamed
      // or deleted upstream icon. URL shape alone (the previous step) would
      // pass even for a literal `x.svg` that doesn't exist.
      const results = await Promise.all(
        hrefs.map(async (href) => {
          const url = href.replace(/#i$/, "");
          const res = await fetch(url, { method: "HEAD" });
          return { url, ok: res.ok, status: res.status };
        }),
      );
      const failed = results.filter((r) => !r.ok);
      expect(failed).toEqual([]);
    });
  },
};

/**
 * Runtime warning: when a cts-icon's name doesn't resolve to a vendored
 * SVG, the component emits a console.warn the first time that name is
 * loaded. Catches dynamic / templated names (e.g. `name="${tile.icon}"`)
 * at dev time — the static CI lint (`npm run lint:icons`) catches literal
 * names earlier in the chain, but anything computed at runtime can only
 * surface here.
 */
export const MissingIconWarns = {
  render: () => html`<div id="mount" style="padding: 1rem;"></div>`,

  async play({ canvasElement }) {
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;
    try {
      const mount = canvasElement.querySelector("#mount");
      // Per-story unique name so the module-level dedupe Set never short-
      // circuits this assertion across repeat runs of the suite.
      const badName = `definitely-missing-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
      const icon = document.createElement("cts-icon");
      icon.setAttribute("name", badName);
      icon.setAttribute("size", "16");
      mount.appendChild(icon);

      await waitFor(
        () => {
          expect(warnSpy).toHaveBeenCalled();
          const joined = warnSpy.mock.calls.flat().join(" ");
          expect(joined).toContain(badName);
          expect(joined).toContain("cts-icon");
        },
        { timeout: 3000 },
      );
    } finally {
      console.warn = origWarn;
    }
  },
};

/**
 * Inverse of MissingIconWarns: a name that resolves to a real vendored
 * SVG must NOT emit a console warning. Guards against a future
 * refactor that accidentally fires the warning on success too.
 */
export const ValidIconNoWarning = {
  render: () => html`<div id="mount" style="padding: 1rem;"></div>`,

  async play({ canvasElement }) {
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;
    try {
      const mount = canvasElement.querySelector("#mount");
      const icon = document.createElement("cts-icon");
      icon.setAttribute("name", "close-md");
      icon.setAttribute("size", "16");
      mount.appendChild(icon);

      // Wait for the actual <use> element to mount and its `load` event to
      // fire — the definitive signal that the SVG fetch resolved without
      // error. A fixed setTimeout would either burn budget needlessly or
      // race past a slow CI fetch and produce a false-negative "no warning
      // fired" (because the fetch hadn't completed yet).
      await waitFor(
        () => {
          const useEl = icon.querySelector("svg use");
          expect(useEl).toBeTruthy();
        },
        { timeout: 3000 },
      );
      // Give one more microtask tick so any same-tick error event would have
      // dispatched onto our listener.
      await new Promise((r) => setTimeout(r, 50));

      // Filter out any unrelated warnings (e.g. Lit dev-mode chatter) that
      // are not from our component.
      const ourCalls = warnSpy.mock.calls.filter((args) =>
        String(args[0] ?? "").includes("[cts-icon]"),
      );
      expect(ourCalls.length).toBe(0);
    } finally {
      console.warn = origWarn;
    }
  },
};

/**
 * Dedupe: the same invalid name appearing N times on a page produces
 * exactly one warning. A long list view re-rendering the same broken
 * icon name would otherwise flood DevTools.
 */
export const WarnsOncePerName = {
  render: () => html`<div id="mount" style="padding: 1rem;"></div>`,

  async play({ canvasElement }) {
    const warnSpy = fn();
    const origWarn = console.warn;
    console.warn = warnSpy;
    try {
      const mount = canvasElement.querySelector("#mount");
      const badName = `dup-missing-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
      for (let i = 0; i < 3; i++) {
        const icon = document.createElement("cts-icon");
        icon.setAttribute("name", badName);
        icon.setAttribute("size", "16");
        mount.appendChild(icon);
      }

      await waitFor(
        () => {
          const ourCalls = warnSpy.mock.calls.filter((args) =>
            String(args[0] ?? "").includes(badName),
          );
          expect(ourCalls.length).toBe(1);
        },
        { timeout: 3000 },
      );
    } finally {
      console.warn = origWarn;
    }
  },
};
