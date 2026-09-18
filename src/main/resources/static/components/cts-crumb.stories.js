import { html } from "lit";
import { expect, userEvent, waitFor } from "storybook/test";
import "./cts-crumb.js";

export default {
  title: "Components/cts-crumb",
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

  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-crumb");
    await host.updateComplete;

    await step("non-terminal entries render as link buttons", async () => {
      const buttons = host.querySelectorAll("button.crumbLink");
      expect(buttons.length).toBe(2);
      expect(buttons[0].textContent.trim()).toBe("Plans");
      expect(buttons[1].textContent.trim()).toBe("oidcc-basic");
    });

    await step("terminal entry renders as the current page", async () => {
      const current = host.querySelector("span.crumbCurrent");
      expect(current).toBeTruthy();
      expect(current.textContent.trim()).toBe("Log");
      expect(current.getAttribute("aria-current")).toBe("page");
    });

    await step("chevron separators render between items", async () => {
      // One chevron between each pair of items — for 3 items that's 2 separators.
      const separators = host.querySelectorAll(".crumbSeparator");
      expect(separators.length).toBe(2);
      const icons = host.querySelectorAll("cts-icon");
      expect(icons.length).toBe(2);
      icons.forEach((icon) => {
        expect(icon.getAttribute("name")).toBe("chevron-right");
        expect(icon.getAttribute("size")).toBe("16");
      });
    });
  },
};

/**
 * Clicking a non-terminal crumb dispatches `cts-crumb-navigate` with the
 * clicked item's `target` as detail. The terminal entry is a `<span>` and
 * cannot fire the event.
 */
export const ClickDispatchesNavigate = {
  render: () => {
    const el = /** @type {any} */ (document.createElement("cts-crumb"));
    el.items = SAMPLE_ITEMS;
    return el;
  },

  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-crumb");
    await host.updateComplete;

    /** @type {string[]} */
    const received = [];
    canvasElement.addEventListener("cts-crumb-navigate", (e) => {
      received.push(/** @type {CustomEvent} */ (e).detail.target);
    });
    const buttons = host.querySelectorAll("button.crumbLink");

    await step("clicking the first crumb dispatches its target", async () => {
      await userEvent.click(buttons[0]);
      expect(received).toEqual(["plans.html"]);
    });

    await step("clicking the second crumb dispatches its target", async () => {
      await userEvent.click(buttons[1]);
      expect(received).toEqual(["plans.html", "plan-detail.html?plan=plan-001"]);
    });
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
    expect(host.querySelector("span.crumbCurrent")).toBeNull();
  },
};

/**
 * Edge case: a single-item trail renders just the terminal label — no
 * buttons, no chevrons.
 */
export const SingleItem = {
  render: () => {
    const el = /** @type {any} */ (document.createElement("cts-crumb"));
    el.items = [{ label: "Plans", target: "plans.html" }];
    return el;
  },

  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-crumb");
    await host.updateComplete;

    await step("no link buttons, separators, or icons render", async () => {
      expect(host.querySelectorAll("button.crumbLink").length).toBe(0);
      expect(host.querySelectorAll(".crumbSeparator").length).toBe(0);
      expect(host.querySelectorAll("cts-icon").length).toBe(0);
    });

    await step("the sole entry renders as the current page", async () => {
      const current = host.querySelector("span.crumbCurrent");
      expect(current).toBeTruthy();
      expect(current.textContent.trim()).toBe("Plans");
      expect(current.getAttribute("aria-current")).toBe("page");
    });
  },
};

// Real names from a published production log: a 64-character plan name and
// an 88-character test name. At phone widths neither fits on one line.
const LONG_ITEMS = [
  { label: "Plans", target: "plans.html" },
  {
    label: "fapi-rw-id2-ob-with-private-key-and-mtls-holder-of-key-test-plan",
    target: "plan-detail.html?plan=1QC2F6gC79mL8",
  },
  {
    label:
      "fapi-rw-id2-ob-ensure-request-object-without-state-with-private-key-and-mtls-holder-of-key",
    target: "log-detail.html?log=kKpHTsblXH",
  },
];

/**
 * Long crumbs on a phone. Pinned to mobile1 (320×568) so neither long name
 * fits its line. The trail must wrap as left-aligned lines: a crumb that does
 * not fit moves to the next line, led by its chevron, and wraps its text
 * across the full nav width. The regression this guards
 * (logplan-02) laid the crumbs out side by side, so each one wrapped inside
 * a narrow column, and the <button> crumb centred its text.
 */
export const LongNamesOnMobile = {
  parameters: {
    viewport: { defaultViewport: "mobile1" },
  },
  globals: {
    viewport: { value: "mobile1", isRotated: false },
  },
  render: () => {
    const el = /** @type {any} */ (document.createElement("cts-crumb"));
    el.items = LONG_ITEMS;
    return el;
  },

  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-crumb");
    await host.updateComplete;
    const nav = /** @type {HTMLElement} */ (
      await waitFor(() => {
        const el = host.querySelector("nav.crumbNav");
        if (!el) throw new Error("nav not yet rendered");
        return el;
      })
    );
    const lineHeight = parseFloat(getComputedStyle(nav).lineHeight);
    const items = Array.from(host.querySelectorAll(".crumbItem"));
    const crumbs = Array.from(host.querySelectorAll(".crumbLink, .crumbCurrent"));

    await step("no crumb centres its text", () => {
      crumbs.forEach((crumb) => {
        expect(getComputedStyle(crumb).textAlign).not.toBe("center");
      });
    });

    await step("a wrapped crumb spans the nav width instead of a narrow column", () => {
      const navWidth = nav.getBoundingClientRect().width;
      const wrapped = items.filter(
        (item) => item.getBoundingClientRect().height > lineHeight * 1.5,
      );
      expect(wrapped.length).toBeGreaterThan(0);
      wrapped.forEach((item) => {
        expect(item.getBoundingClientRect().width).toBeGreaterThan(navWidth * 0.9);
      });
    });

    await step("each crumb wraps only as many lines as its text needs", () => {
      // The 88-character name needs at most three lines of a 320px phone;
      // the old side-by-side layout ran it to eight.
      items.forEach((item) => {
        expect(item.getBoundingClientRect().height).toBeLessThan(lineHeight * 3.5);
      });
    });

    await step("the chevron leads its crumb on the crumb's first line", () => {
      host.querySelectorAll(".crumbSeparator").forEach((sep) => {
        const item = sep.closest(".crumbItem");
        const sepRect = sep.getBoundingClientRect();
        const itemRect = item.getBoundingClientRect();
        expect(sepRect.left).toBeLessThanOrEqual(itemRect.left + 1);
        expect(sepRect.top).toBeGreaterThanOrEqual(itemRect.top - 1);
        expect(sepRect.bottom).toBeLessThanOrEqual(itemRect.top + lineHeight + 1);
      });
    });
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
