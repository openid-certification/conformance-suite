import { html } from "lit";
import { expect } from "storybook/test";
import "./cts-page-head.js";
import "./cts-button.js";
import "./cts-link-button.js";

export default {
  title: "Components/cts-page-head",
  component: "cts-page-head",
};

// --- Stories ---

export const TitleAndSub = {
  render: () =>
    html`<cts-page-head title="Test plans" sub="Browse and rerun published plans"></cts-page-head>`,

  async play({ canvasElement }) {
    const head = canvasElement.querySelector(".oidf-page-head");
    expect(head).toBeTruthy();

    const title = canvasElement.querySelector("h1");
    expect(title).toBeTruthy();
    expect(title.textContent).toBe("Test plans");
    // Token-styled title — uses .t-title (32px display) from oidf-tokens.css.
    expect(title.classList.contains("t-title")).toBe(true);

    const sub = canvasElement.querySelector(".oidf-page-head-sub");
    expect(sub).toBeTruthy();
    expect(sub.textContent).toBe("Browse and rerun published plans");
    // Subtitle uses the .t-meta token style (13px, fg-soft).
    expect(sub.classList.contains("t-meta")).toBe(true);

    // Actions container renders even when no actions are slotted.
    const actions = canvasElement.querySelector(".oidf-page-head-actions");
    expect(actions).toBeTruthy();
    expect(actions.children.length).toBe(0);

    // Bottom border divider — present via the scoped `.oidf-page-head` class.
    const computed = getComputedStyle(head);
    expect(computed.borderBottomStyle).toBe("solid");
  },
};

export const TitleOnly = {
  render: () => html`<cts-page-head title="Logs"></cts-page-head>`,

  async play({ canvasElement }) {
    const title = canvasElement.querySelector("h1");
    expect(title).toBeTruthy();
    expect(title.textContent).toBe("Logs");

    // Edge case: no `sub` attribute means the subtitle paragraph must not render.
    const sub = canvasElement.querySelector(".oidf-page-head-sub");
    expect(sub).toBeNull();

    // Actions container still renders (just empty).
    const actions = canvasElement.querySelector(".oidf-page-head-actions");
    expect(actions).toBeTruthy();
  },
};

export const WithActions = {
  render: () =>
    html`<cts-page-head title="Test plans" sub="Browse and rerun published plans">
      <cts-link-button slot="actions" href="#" variant="secondary" label="Export"></cts-link-button>
      <cts-button slot="actions" variant="primary" label="New plan"></cts-button>
    </cts-page-head>`,

  async play({ canvasElement }) {
    const head = canvasElement.querySelector(".oidf-page-head");
    expect(head).toBeTruthy();

    const actions = canvasElement.querySelector(".oidf-page-head-actions");
    expect(actions).toBeTruthy();

    // Both slotted action elements are moved into the actions container.
    const linkButton = actions.querySelector("cts-link-button");
    const button = actions.querySelector("cts-button");
    expect(linkButton).toBeTruthy();
    expect(button).toBeTruthy();

    // The slot="actions" attribute is stripped after the move so the
    // rendered tree does not carry leftover slot metadata.
    expect(linkButton.hasAttribute("slot")).toBe(false);
    expect(button.hasAttribute("slot")).toBe(false);

    // Order is preserved (link-button first, button second).
    expect(actions.children[0]).toBe(linkButton);
    expect(actions.children[1]).toBe(button);

    // Sanity: the inner anchors/buttons rendered by the action components.
    const anchor = linkButton.querySelector("a");
    expect(anchor).toBeTruthy();
    expect(anchor.textContent.trim()).toBe("Export");

    const innerBtn = button.querySelector("button");
    expect(innerBtn).toBeTruthy();
    expect(innerBtn.textContent.trim()).toBe("New plan");
  },
};
