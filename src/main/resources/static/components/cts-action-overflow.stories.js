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
    const popover = canvasElement.querySelector('[data-testid="overflow-popover"]');

    // APG Menu Button: trigger advertises the relationship and the
    // closed-state of the controlled menu. aria-controls + aria-expanded
    // are the WCAG 4.1.2 (Name/Role/Value) contract; aria-haspopup tells
    // SR users the activation produces a menu, not a dialog or listbox.
    expect(trigger.getAttribute("aria-label")).toBe("More actions");
    expect(trigger.getAttribute("aria-haspopup")).toBe("menu");
    expect(trigger.getAttribute("aria-controls")).toBe(popover.id);
    expect(trigger.getAttribute("aria-expanded")).toBe("false");

    // Menu uses aria-labelledby pointing at the trigger so SRs read the
    // trigger's accessible name once when entering the menu, not twice.
    expect(popover.getAttribute("aria-labelledby")).toBe(trigger.id);

    // Six visible items in the popover even though it's not open yet —
    // the popover element exists in the DOM, hidden by display:none until
    // showPopover() flips it to the top layer.
    const items = canvasElement.querySelectorAll(".overflowItem");
    expect(items).toHaveLength(6);
    expect(items[0].textContent).toContain("Upload Images");
    expect(items[5].textContent).toContain("Share Link");
    // APG Menu: items are out of the page tab order; movement between
    // them is via Arrow/Home/End. Tab from any item closes the menu and
    // lets the browser advance to the next focusable element on the page.
    items.forEach((item) => expect(item.getAttribute("tabindex")).toBe("-1"));
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
    const trigger = await openPopover(canvasElement);
    const popover = canvasElement.querySelector('[data-testid="overflow-popover"]');
    expect(popover.matches(":popover-open")).toBe(true);
    expect(popover.getAttribute("role")).toBe("menu");
    // aria-expanded mirrors the popover's open state for AT users.
    await waitFor(() => {
      expect(trigger.getAttribute("aria-expanded")).toBe("true");
    });
    // First item gets focus when the popover opens (toggle event runs
    // after the openPopover waitFor resolves).
    await waitFor(() => {
      const first = canvasElement.querySelector(".overflowItem");
      expect(document.activeElement).toBe(first);
    });
  },
};

export const TriggerArrowDownOpensAndFocusesFirst = {
  // APG Menu Button: ArrowDown on the closed trigger opens the menu and
  // moves focus to the first item, identically to clicking the trigger.
  render: () => html`<cts-action-overflow .actions=${ACTIONS}></cts-action-overflow>`,
  async play({ canvasElement }) {
    const trigger = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="overflow-trigger"]');
      if (!el) throw new Error("overflow-trigger not yet rendered");
      return /** @type {HTMLElement} */ (el);
    });
    trigger.focus();
    await userEvent.keyboard("{ArrowDown}");
    await waitFor(() => {
      const popover = canvasElement.querySelector('[data-testid="overflow-popover"]');
      expect(popover.matches(":popover-open")).toBe(true);
    });
    await waitFor(() => {
      const items = canvasElement.querySelectorAll(".overflowItem");
      expect(document.activeElement).toBe(items[0]);
    });
  },
};

export const TriggerArrowUpOpensAndFocusesLast = {
  // APG Menu Button: ArrowUp on the closed trigger opens the menu and
  // moves focus to the last item — useful when the user knows the
  // destructive/quit action is at the bottom of the menu.
  render: () => html`<cts-action-overflow .actions=${ACTIONS}></cts-action-overflow>`,
  async play({ canvasElement }) {
    const trigger = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="overflow-trigger"]');
      if (!el) throw new Error("overflow-trigger not yet rendered");
      return /** @type {HTMLElement} */ (el);
    });
    trigger.focus();
    await userEvent.keyboard("{ArrowUp}");
    await waitFor(() => {
      const popover = canvasElement.querySelector('[data-testid="overflow-popover"]');
      expect(popover.matches(":popover-open")).toBe(true);
    });
    await waitFor(() => {
      const items = canvasElement.querySelectorAll(".overflowItem");
      expect(document.activeElement).toBe(items[items.length - 1]);
    });
  },
};

export const TabClosesMenu = {
  // APG Menu: Tab inside an open menu closes the menu and advances focus
  // to the next focusable element on the page. Native popover light-
  // dismiss covers Escape and outside-click but does not dismiss on
  // focus moving outside via Tab — this story locks in the explicit
  // dismissal handler that keeps visual state and AT state in sync.
  render: () => html`<cts-action-overflow .actions=${ACTIONS}></cts-action-overflow>`,
  async play({ canvasElement }) {
    const trigger = await openPopover(canvasElement);
    await userEvent.keyboard("{Tab}");
    await waitFor(() => {
      const popover = canvasElement.querySelector('[data-testid="overflow-popover"]');
      expect(popover.matches(":popover-open")).toBe(false);
    });
    await waitFor(() => {
      expect(trigger.getAttribute("aria-expanded")).toBe("false");
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
