import { html } from "lit";
import { expect, userEvent } from "storybook/test";
import "./cts-crumb.js";

export default {
  title: "Primitives/cts-crumb",
  component: "cts-crumb",
};

const SAMPLE_ITEMS = [
  { label: "Plans", target: "plans.html" },
  { label: "oidcc-basic", target: "plan-detail.html?plan=plan-001" },
  { label: "Log", target: "log-detail.html?log=log-001" },
];

/**
 * Default trail with three entries: two clickable links and a final bold
 * label. Verifies the chevron separators render between items, the trailing
 * entry is a `<b>` and not a button, and the link buttons carry the OIDF
 * link colour styling.
 */
export const Default = {
  render: () => {
    const el = /** @type {any} */ (document.createElement("cts-crumb"));
    el.items = SAMPLE_ITEMS;
    return el;
  },

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-crumb");
    await host.updateComplete;

    const buttons = host.querySelectorAll("button.crumbLink");
    expect(buttons.length).toBe(2);
    expect(buttons[0].textContent.trim()).toBe("Plans");
    expect(buttons[1].textContent.trim()).toBe("oidcc-basic");

    const current = host.querySelector("b.crumbCurrent");
    expect(current).toBeTruthy();
    expect(current.textContent.trim()).toBe("Log");
    expect(current.getAttribute("aria-current")).toBe("page");

    // One chevron between each pair of items — for 3 items that's 2 separators.
    const separators = host.querySelectorAll(".crumbSeparator");
    expect(separators.length).toBe(2);
    const icons = host.querySelectorAll("cts-icon");
    expect(icons.length).toBe(2);
    icons.forEach((icon) => {
      expect(icon.getAttribute("name")).toBe("chevron-right");
      expect(icon.getAttribute("size")).toBe("16");
    });
  },
};

/**
 * Clicking a non-terminal crumb dispatches `cts-crumb-navigate` with the
 * clicked item's `target` as detail. The terminal entry is a `<b>` and
 * cannot fire the event.
 */
export const ClickDispatchesNavigate = {
  render: () => {
    const el = /** @type {any} */ (document.createElement("cts-crumb"));
    el.items = SAMPLE_ITEMS;
    return el;
  },

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-crumb");
    await host.updateComplete;

    /** @type {string[]} */
    const received = [];
    canvasElement.addEventListener("cts-crumb-navigate", (e) => {
      received.push(/** @type {CustomEvent} */ (e).detail.target);
    });

    const buttons = host.querySelectorAll("button.crumbLink");
    await userEvent.click(buttons[0]);
    expect(received).toEqual(["plans.html"]);

    await userEvent.click(buttons[1]);
    expect(received).toEqual(["plans.html", "plan-detail.html?plan=plan-001"]);
  },
};

/**
 * Edge case: empty `items` array renders nothing — no `<nav>`, no buttons,
 * no chevrons.
 */
export const EmptyRendersNothing = {
  render: () => {
    const el = /** @type {any} */ (document.createElement("cts-crumb"));
    el.items = [];
    return el;
  },

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-crumb");
    await host.updateComplete;

    expect(host.querySelector("nav")).toBeNull();
    expect(host.querySelector("button")).toBeNull();
    expect(host.querySelector("cts-icon")).toBeNull();
    expect(host.querySelector("b")).toBeNull();
  },
};

/**
 * Edge case: a single-item trail renders just the bold label — no buttons,
 * no chevrons.
 */
export const SingleItem = {
  render: () => {
    const el = /** @type {any} */ (document.createElement("cts-crumb"));
    el.items = [{ label: "Plans", target: "plans.html" }];
    return el;
  },

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-crumb");
    await host.updateComplete;

    expect(host.querySelectorAll("button.crumbLink").length).toBe(0);
    expect(host.querySelectorAll(".crumbSeparator").length).toBe(0);
    expect(host.querySelectorAll("cts-icon").length).toBe(0);

    const current = host.querySelector("b.crumbCurrent");
    expect(current).toBeTruthy();
    expect(current.textContent.trim()).toBe("Plans");
    expect(current.getAttribute("aria-current")).toBe("page");
  },
};

/**
 * Visual reference showing the three documented arrangements side-by-side
 * for design review.
 */
export const Variations = {
  render: () => html`
    <div style="display: grid; gap: var(--space-4); padding: var(--space-3);">
      <div>
        <p style="margin: 0 0 var(--space-1); color: var(--fg-soft); font-size: 12px;"
          >Three items</p
        >
        ${(() => {
          const el = /** @type {any} */ (document.createElement("cts-crumb"));
          el.items = SAMPLE_ITEMS;
          return el;
        })()}
      </div>
      <div>
        <p style="margin: 0 0 var(--space-1); color: var(--fg-soft); font-size: 12px;">Two items</p>
        ${(() => {
          const el = /** @type {any} */ (document.createElement("cts-crumb"));
          el.items = [
            { label: "Plans", target: "plans.html" },
            { label: "oidcc-basic", target: "plan-detail.html?plan=plan-001" },
          ];
          return el;
        })()}
      </div>
      <div>
        <p style="margin: 0 0 var(--space-1); color: var(--fg-soft); font-size: 12px;"
          >Single item</p
        >
        ${(() => {
          const el = /** @type {any} */ (document.createElement("cts-crumb"));
          el.items = [{ label: "Plans", target: "plans.html" }];
          return el;
        })()}
      </div>
    </div>
  `,
};
