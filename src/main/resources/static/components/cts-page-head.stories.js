import { html } from "lit";
import { expect, waitFor } from "storybook/test";
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

  async play({ canvasElement, step }) {
    const head = canvasElement.querySelector(".oidf-page-head");
    expect(head).toBeTruthy();

    await step("title renders with token style", async () => {
      const title = canvasElement.querySelector("h1");
      expect(title).toBeTruthy();
      expect(title.textContent).toBe("Test plans");
      // Token-styled title — uses .t-title (32px display) from oidf-tokens.css.
      expect(title.classList.contains("t-title")).toBe(true);
    });

    await step("subtitle renders with token style", async () => {
      const sub = canvasElement.querySelector(".oidf-page-head-sub");
      expect(sub).toBeTruthy();
      expect(sub.textContent).toBe("Browse and rerun published plans");
      // Subtitle uses the .t-meta token style (13px, fg-soft).
      expect(sub.classList.contains("t-meta")).toBe(true);
    });

    await step("actions container renders even when empty", async () => {
      const actions = canvasElement.querySelector(".oidf-page-head-actions");
      expect(actions).toBeTruthy();
      expect(actions.children.length).toBe(0);
    });

    await step("bottom border divider is present", async () => {
      // Present via the scoped `.oidf-page-head` class.
      const computed = getComputedStyle(head);
      expect(computed.borderBottomStyle).toBe("solid");
    });
  },
};

export const TitleOnly = {
  render: () => html`<cts-page-head title="Logs"></cts-page-head>`,

  async play({ canvasElement, step }) {
    await step("title renders", async () => {
      const title = canvasElement.querySelector("h1");
      expect(title).toBeTruthy();
      expect(title.textContent).toBe("Logs");
    });

    await step("subtitle paragraph must not render without a sub attribute", async () => {
      const sub = canvasElement.querySelector(".oidf-page-head-sub");
      expect(sub).toBeNull();
    });

    await step("actions container still renders (just empty)", async () => {
      const actions = canvasElement.querySelector(".oidf-page-head-actions");
      expect(actions).toBeTruthy();
    });
  },
};

export const WithActions = {
  render: () =>
    html`<cts-page-head title="Test plans" sub="Browse and rerun published plans">
      <cts-link-button slot="actions" href="#" variant="secondary" label="Export"></cts-link-button>
      <cts-button slot="actions" variant="primary" label="New plan"></cts-button>
    </cts-page-head>`,

  async play({ canvasElement, step }) {
    const head = canvasElement.querySelector(".oidf-page-head");
    expect(head).toBeTruthy();

    const actions = canvasElement.querySelector(".oidf-page-head-actions");
    expect(actions).toBeTruthy();
    const linkButton = actions.querySelector("cts-link-button");
    const button = actions.querySelector("cts-button");

    await step("both slotted action elements are moved into the actions container", async () => {
      expect(linkButton).toBeTruthy();
      expect(button).toBeTruthy();
    });

    await step("slot attribute is stripped after the move", async () => {
      // So the rendered tree does not carry leftover slot metadata.
      expect(linkButton.hasAttribute("slot")).toBe(false);
      expect(button.hasAttribute("slot")).toBe(false);
    });

    await step("order is preserved (link-button first, button second)", async () => {
      expect(actions.children[0]).toBe(linkButton);
      expect(actions.children[1]).toBe(button);
    });

    await step("inner anchors/buttons rendered by the action components", async () => {
      const anchor = linkButton.querySelector("a");
      expect(anchor).toBeTruthy();
      expect(anchor.textContent.trim()).toBe("Export");

      const innerBtn = button.querySelector("button");
      expect(innerBtn).toBeTruthy();
      expect(innerBtn.textContent.trim()).toBe("New plan");
    });
  },
};

/**
 * Phone layout (mobile1, 320px): the actions cluster drops under the title
 * and subtitle as its own full-width row instead of squeezing the H1 into
 * a two-line wrap beside the buttons.
 */
export const WithActionsOnMobile = {
  parameters: {
    viewport: { defaultViewport: "mobile1" },
  },
  globals: {
    viewport: { value: "mobile1", isRotated: false },
  },
  render: () =>
    html`<cts-page-head title="Test plans" sub="Browse and rerun published plans">
      <cts-link-button slot="actions" href="#" variant="secondary" label="Export"></cts-link-button>
      <cts-button slot="actions" variant="primary" label="New plan"></cts-button>
    </cts-page-head>`,

  async play({ canvasElement, step }) {
    const actions = /** @type {HTMLElement} */ (
      await waitFor(() => {
        const el = canvasElement.querySelector(".oidf-page-head-actions");
        expect(el.children.length).toBe(2);
        return el;
      })
    );
    const title = /** @type {HTMLElement} */ (canvasElement.querySelector(".oidf-page-head-title"));
    const sub = /** @type {HTMLElement} */ (canvasElement.querySelector(".oidf-page-head-sub"));

    await step("the head wraps on phones", async () => {
      const head = canvasElement.querySelector(".oidf-page-head");
      expect(getComputedStyle(head).flexWrap).toBe("wrap");
    });

    await step("actions row sits below the title and subtitle", async () => {
      const actionsBox = actions.getBoundingClientRect();
      expect(actionsBox.top).toBeGreaterThanOrEqual(title.getBoundingClientRect().bottom);
      expect(actionsBox.top).toBeGreaterThanOrEqual(sub.getBoundingClientRect().bottom);
    });

    await step("title stays on a single line", async () => {
      const lineHeight = parseFloat(getComputedStyle(title).lineHeight);
      expect(title.getBoundingClientRect().height).toBeLessThan(lineHeight * 1.5);
    });

    await step("actions row is left-aligned and full width", async () => {
      const headBox = canvasElement.querySelector(".oidf-page-head").getBoundingClientRect();
      const actionsBox = actions.getBoundingClientRect();
      expect(actionsBox.left).toBeCloseTo(headBox.left, 0);
      expect(actionsBox.width).toBeCloseTo(headBox.width, 0);
    });
  },
};
