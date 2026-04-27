import { html } from "lit";
import { expect, waitFor, userEvent } from "storybook/test";
import "./cts-action-overflow.js";

export default {
  title: "Components/cts-action-overflow",
  component: "cts-action-overflow",
};

const ACTIONS = [
  { id: "upload-images", label: "Upload Images", icon: "image-01" },
  { id: "view-config", label: "View configuration", icon: "settings" },
  { id: "edit-config", label: "Edit configuration", icon: "edit-pencil-01" },
  { id: "download-log", label: "Download Logs", icon: "save" },
  { id: "publish", label: "Publish", icon: "bookmark" },
  { id: "share-link", label: "Share Link", icon: "bookmark" },
];

const ACTIONS_WITH_HIDDEN = [
  { id: "upload-images", label: "Upload Images", icon: "image-01", hidden: true },
  { id: "view-config", label: "View configuration", icon: "settings" },
];

async function openPopover(canvasElement) {
  const trigger = await waitFor(() => {
    const el = canvasElement.querySelector('[data-testid="overflow-trigger"]');
    if (!el) throw new Error("overflow-trigger not yet rendered");
    return /** @type {HTMLElement} */ (el);
  });
  await userEvent.click(trigger);
  // Wait for the popover to flip to open. The popover beforetoggle event
  // fires before display flips, so use the :popover-open match path.
  await waitFor(() => {
    const popover = canvasElement.querySelector('[data-testid="overflow-popover"]');
    if (!popover || !popover.matches(":popover-open")) {
      throw new Error("popover not yet open");
    }
    return popover;
  });
  return trigger;
}

export const Default = {
  render: () => html`<cts-action-overflow .actions=${ACTIONS}></cts-action-overflow>`,
  async play({ canvasElement }) {
    const trigger = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="overflow-trigger"]');
      if (!el) throw new Error("overflow-trigger not yet rendered");
      return el;
    });

    expect(trigger.getAttribute("aria-label")).toBe("More actions");
    expect(trigger.getAttribute("aria-haspopup")).toBe("menu");

    // Six visible items in the popover even though it's not open yet —
    // the popover element exists in the DOM, hidden by display:none until
    // showPopover() flips it to the top layer.
    const items = canvasElement.querySelectorAll(".overflowItem");
    expect(items).toHaveLength(6);
    expect(items[0].textContent).toContain("Upload Images");
    expect(items[5].textContent).toContain("Share Link");
  },
};

export const HiddenActionsOmitted = {
  render: () => html`<cts-action-overflow .actions=${ACTIONS_WITH_HIDDEN}></cts-action-overflow>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="overflow-trigger"]');
      if (!el) throw new Error("overflow-trigger not yet rendered");
      return el;
    });
    // Hidden actions are omitted from the menu; only the second item renders.
    const items = canvasElement.querySelectorAll(".overflowItem");
    expect(items).toHaveLength(1);
    expect(items[0].textContent).toContain("View configuration");
  },
};

export const PopoverOpens = {
  render: () => html`<cts-action-overflow .actions=${ACTIONS}></cts-action-overflow>`,
  async play({ canvasElement }) {
    await openPopover(canvasElement);
    const popover = canvasElement.querySelector('[data-testid="overflow-popover"]');
    expect(popover.matches(":popover-open")).toBe(true);
    expect(popover.getAttribute("role")).toBe("menu");
    // First item gets focus when the popover opens (queued microtask runs
    // after the openPopover waitFor resolves).
    await waitFor(() => {
      const first = canvasElement.querySelector(".overflowItem");
      expect(document.activeElement).toBe(first);
    });
  },
};

export const ActionFiresEvent = {
  render: () => html`<cts-action-overflow .actions=${ACTIONS}></cts-action-overflow>`,
  async play({ canvasElement }) {
    await openPopover(canvasElement);

    /** @type {Array<string>} */
    const fired = [];
    canvasElement.addEventListener("cts-overflow-action", (evt) => {
      fired.push(/** @type {CustomEvent} */ (evt).detail.actionId);
    });

    // Click the second action — "View configuration".
    const items = /** @type {NodeListOf<HTMLButtonElement>} */ (
      canvasElement.querySelectorAll(".overflowItem")
    );
    await userEvent.click(items[1]);

    expect(fired).toEqual(["view-config"]);
    // Activation also closes the popover.
    await waitFor(() => {
      const popover = canvasElement.querySelector('[data-testid="overflow-popover"]');
      expect(popover.matches(":popover-open")).toBe(false);
    });
  },
};

export const KeyboardArrowNavigation = {
  render: () => html`<cts-action-overflow .actions=${ACTIONS}></cts-action-overflow>`,
  async play({ canvasElement }) {
    await openPopover(canvasElement);
    // ArrowDown moves focus to the second item; ArrowUp moves it back.
    await userEvent.keyboard("{ArrowDown}");
    const items = canvasElement.querySelectorAll(".overflowItem");
    await waitFor(() => {
      expect(document.activeElement).toBe(items[1]);
    });
    await userEvent.keyboard("{ArrowUp}");
    await waitFor(() => {
      expect(document.activeElement).toBe(items[0]);
    });
    // Home / End jump to the bookends.
    await userEvent.keyboard("{End}");
    await waitFor(() => {
      expect(document.activeElement).toBe(items[items.length - 1]);
    });
    await userEvent.keyboard("{Home}");
    await waitFor(() => {
      expect(document.activeElement).toBe(items[0]);
    });
  },
};

export const PopoverClosesViaApi = {
  // Native Escape / outside-click dismissal is owned by the HTML Popover
  // API and tested by browser vendors; this story instead verifies the
  // dismissal contract we *do* own — calling hidePopover() programmatically
  // (the same path our _handleItemClick takes after dispatching an event)
  // closes the popover and surfaces the closed state through
  // :popover-open.
  render: () => html`<cts-action-overflow .actions=${ACTIONS}></cts-action-overflow>`,
  async play({ canvasElement }) {
    await openPopover(canvasElement);
    const popover = /** @type {any} */ (
      canvasElement.querySelector('[data-testid="overflow-popover"]')
    );
    expect(popover.matches(":popover-open")).toBe(true);
    popover.hidePopover();
    await waitFor(() => {
      expect(popover.matches(":popover-open")).toBe(false);
    });
  },
};

export const EmptyActions = {
  render: () => html`<cts-action-overflow .actions=${[]}></cts-action-overflow>`,
  async play({ canvasElement }) {
    // No visible actions → nothing rendered, no trigger.
    await Promise.resolve();
    expect(canvasElement.querySelector('[data-testid="overflow-trigger"]')).toBeNull();
  },
};
