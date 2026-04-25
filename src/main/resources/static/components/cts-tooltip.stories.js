import { html } from "lit";
import { expect, userEvent, waitFor } from "storybook/test";
import "./cts-tooltip.js";

export default {
  title: "Primitives/cts-tooltip",
  component: "cts-tooltip",
};

// --- Helpers ---

/**
 * Returns the singleton .oidf-tooltip currently rendered into document.body,
 * or null when there isn't one. The component appends to body so it escapes
 * any clipping ancestor; this is the integration contract.
 *
 * @returns {HTMLElement | null}
 */
function getTooltipEl() {
  return /** @type {HTMLElement | null} */ (document.body.querySelector(".oidf-tooltip"));
}

// --- Stories ---

export const Default = {
  render: () => html`
    <div style="padding: 80px;">
      <cts-tooltip content="Click to copy the share URL" placement="top">
        <button>Copy</button>
      </cts-tooltip>
    </div>
  `,

  async play({ canvasElement }) {
    const button = /** @type {HTMLButtonElement} */ (canvasElement.querySelector("button"));
    expect(button).toBeTruthy();

    // No Bootstrap leftovers: data-bs-toggle MUST NOT be set on the trigger.
    expect(button.hasAttribute("data-bs-toggle")).toBe(false);
    expect(button.hasAttribute("data-bs-placement")).toBe(false);

    // Hover renders the tooltip into document.body.
    await userEvent.hover(button);
    await waitFor(() => {
      const tip = getTooltipEl();
      expect(tip).toBeTruthy();
      expect(tip?.textContent).toContain("Click to copy the share URL");
      expect(tip?.getAttribute("data-placement")).toBe("top");
    });

    // Unhover removes it.
    await userEvent.unhover(button);
    await waitFor(() => {
      expect(getTooltipEl()).toBeNull();
    });
  },
};

export const BottomPlacement = {
  render: () => html`
    <div style="padding: 80px;">
      <cts-tooltip content="Visit the OpenID Foundation" placement="bottom">
        <a href="https://openid.net">OpenID Foundation</a>
      </cts-tooltip>
    </div>
  `,

  async play({ canvasElement }) {
    const anchor = /** @type {HTMLAnchorElement} */ (canvasElement.querySelector("a"));
    expect(anchor).toBeTruthy();

    await userEvent.hover(anchor);
    await waitFor(() => {
      const tip = getTooltipEl();
      expect(tip).toBeTruthy();
      expect(tip?.getAttribute("data-placement")).toBe("bottom");

      // Verify the popup actually renders BELOW the anchor — placement
      // attribute presence alone is not enough; a regression that forgets
      // to use placement in _position() would still set the attribute
      // but render in the wrong spot.
      const anchorRect = anchor.getBoundingClientRect();
      const tipRect = /** @type {HTMLElement} */ (tip).getBoundingClientRect();
      expect(tipRect.top).toBeGreaterThanOrEqual(anchorRect.bottom - 1);
    });

    await userEvent.unhover(anchor);
    await waitFor(() => expect(getTooltipEl()).toBeNull());
  },
};

export const NoContent = {
  render: () => html`
    <cts-tooltip>
      <button>No tooltip</button>
    </cts-tooltip>
  `,

  async play({ canvasElement }) {
    const button = /** @type {HTMLButtonElement} */ (canvasElement.querySelector("button"));
    expect(button).toBeTruthy();

    // Without content, hover is a no-op.
    await userEvent.hover(button);
    // Give the (non-existent) tooltip a chance not to appear.
    await new Promise((r) => setTimeout(r, 50));
    expect(getTooltipEl()).toBeNull();
  },
};

export const TooltipAppearsOnHover = {
  render: () => html`
    <div style="padding: 60px;">
      <cts-tooltip content="Hover tooltip text" placement="top">
        <button>Hover me</button>
      </cts-tooltip>
    </div>
  `,

  async play({ canvasElement }) {
    const button = /** @type {HTMLButtonElement} */ (canvasElement.querySelector("button"));

    expect(getTooltipEl()).toBeNull();

    await userEvent.hover(button);
    await waitFor(() => {
      const tip = getTooltipEl();
      expect(tip).toBeTruthy();
      expect(tip?.textContent).toContain("Hover tooltip text");
    });

    await userEvent.unhover(button);
    await waitFor(() => expect(getTooltipEl()).toBeNull());
  },
};

export const FocusAndEscapeDismiss = {
  render: () => html`
    <div style="padding: 60px;">
      <cts-tooltip content="Focus tooltip text" placement="top">
        <button>Focus me</button>
      </cts-tooltip>
      <button id="other">Elsewhere</button>
    </div>
  `,

  async play({ canvasElement }) {
    const button = /** @type {HTMLButtonElement} */ (canvasElement.querySelector("button"));
    const other = /** @type {HTMLButtonElement} */ (canvasElement.querySelector("#other"));

    // Focusing the trigger shows the tooltip.
    button.focus();
    await waitFor(() => {
      expect(getTooltipEl()).toBeTruthy();
    });

    // Escape dismisses without moving focus.
    await userEvent.keyboard("{Escape}");
    await waitFor(() => expect(getTooltipEl()).toBeNull());
    expect(document.activeElement).toBe(button);

    // Re-focus → blur to another element dismisses.
    button.focus();
    await waitFor(() => expect(getTooltipEl()).toBeTruthy());
    other.focus();
    await waitFor(() => expect(getTooltipEl()).toBeNull());
  },
};

export const DynamicallyInsertedChild = {
  render: () => html`
    <div style="padding: 60px;">
      <cts-tooltip
        id="dynamic-tooltip"
        content="Wired after insertion"
        placement="top"
      ></cts-tooltip>
    </div>
  `,

  async play({ canvasElement }) {
    const host = /** @type {HTMLElement} */ (canvasElement.querySelector("#dynamic-tooltip"));
    expect(host).toBeTruthy();

    // Dynamically insert the trigger after connect — the MutationObserver
    // inside cts-tooltip should still wire up listeners.
    const button = document.createElement("button");
    button.textContent = "Dynamic trigger";
    host.appendChild(button);

    await waitFor(() => {
      // Hover should now show the tooltip — confirms the observer wired up.
      const promise = userEvent.hover(button);
      return promise.then(() => {
        const tip = getTooltipEl();
        expect(tip).toBeTruthy();
        expect(tip?.textContent).toContain("Wired after insertion");
      });
    });

    await userEvent.unhover(button);
    await waitFor(() => expect(getTooltipEl()).toBeNull());
  },
};

export const AutoPlacementFlipsBelow = {
  render: () => html`
    <!-- Trigger pinned to the very top of the viewport with no room above.
         Auto placement should pick "bottom". -->
    <div style="padding: 0; margin: 0; position: relative;">
      <cts-tooltip content="Flips below" placement="auto">
        <button style="position: fixed; top: 4px; left: 200px;">Top edge</button>
      </cts-tooltip>
    </div>
  `,

  async play() {
    const button = /** @type {HTMLButtonElement} */ (document.querySelector("button"));
    expect(button).toBeTruthy();

    await userEvent.hover(button);
    await waitFor(() => {
      const tip = getTooltipEl();
      expect(tip).toBeTruthy();
      // Top has 4px of room — far less than the tip's height — so auto
      // must flip to bottom.
      expect(tip?.getAttribute("data-placement")).toBe("bottom");
    });

    await userEvent.unhover(button);
    await waitFor(() => expect(getTooltipEl()).toBeNull());
  },
};

export const RepositionsOnReshow = {
  render: () => html`
    <div style="padding: 80px;">
      <cts-tooltip content="Recomputed each show" placement="top">
        <button id="moving-trigger">Recompute me</button>
      </cts-tooltip>
    </div>
  `,

  async play({ canvasElement }) {
    const button = /** @type {HTMLButtonElement} */ (canvasElement.querySelector("button"));

    // First show.
    await userEvent.hover(button);
    /** @type {HTMLElement | null} */
    let tip = null;
    await waitFor(() => {
      tip = getTooltipEl();
      expect(tip).toBeTruthy();
    });
    const firstLeft = parseFloat(
      /** @type {HTMLElement} */ (/** @type {unknown} */ (tip)).style.left,
    );
    await userEvent.unhover(button);
    await waitFor(() => expect(getTooltipEl()).toBeNull());

    // Move the trigger sideways. On the next mouseenter, position must be
    // recomputed against the new bounding rect — so the tooltip's left
    // coordinate should be different from the first show.
    button.style.position = "relative";
    button.style.left = "200px";

    await userEvent.hover(button);
    await waitFor(() => {
      tip = getTooltipEl();
      expect(tip).toBeTruthy();
    });
    const secondLeft = parseFloat(
      /** @type {HTMLElement} */ (/** @type {unknown} */ (tip)).style.left,
    );
    expect(secondLeft).not.toBe(firstLeft);

    await userEvent.unhover(button);
    await waitFor(() => expect(getTooltipEl()).toBeNull());
  },
};
