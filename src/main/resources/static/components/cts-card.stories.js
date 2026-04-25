import { html } from "lit";
import { expect } from "storybook/test";
import "./cts-card.js";

export default {
  title: "Primitives/cts-card",
  component: "cts-card",
};

// --- Stories ---

export const WithHeader = {
  render: () =>
    html`<cts-card header="Server Configuration"
      ><p>Body content</p> <p>More content</p></cts-card
    >`,

  async play({ canvasElement }) {
    const card = canvasElement.querySelector(".oidf-card");
    expect(card).toBeTruthy();

    const cardHeader = canvasElement.querySelector(".oidf-card-header");
    expect(cardHeader).toBeTruthy();
    expect(cardHeader.textContent.trim()).toBe("Server Configuration");

    const cardBody = canvasElement.querySelector(".oidf-card-body");
    expect(cardBody).toBeTruthy();

    const paragraphs = cardBody.querySelectorAll("p");
    expect(paragraphs.length).toBe(2);
    expect(paragraphs[0].textContent).toBe("Body content");
    expect(paragraphs[1].textContent).toBe("More content");

    // No brand bar unless tone is set.
    const bar = canvasElement.querySelector(".oidf-card-bar");
    expect(bar).toBeNull();
  },
};

export const WithoutHeader = {
  render: () => html`<cts-card><p>Body only.</p></cts-card>`,

  async play({ canvasElement }) {
    const card = canvasElement.querySelector(".oidf-card");
    expect(card).toBeTruthy();

    const cardHeader = canvasElement.querySelector(".oidf-card-header");
    expect(cardHeader).toBeNull();

    const cardBody = canvasElement.querySelector(".oidf-card-body");
    expect(cardBody).toBeTruthy();

    const paragraph = cardBody.querySelector("p");
    expect(paragraph).toBeTruthy();
    expect(paragraph.textContent).toBe("Body only.");

    // No brand bar unless tone is set.
    const bar = canvasElement.querySelector(".oidf-card-bar");
    expect(bar).toBeNull();
  },
};

export const NestedContent = {
  render: () =>
    html`<cts-card header="Nested Example">
      <div class="container">
        <strong>Label:</strong>
        <span class="badge bg-primary">Active</span>
      </div>
    </cts-card>`,

  async play({ canvasElement }) {
    const card = canvasElement.querySelector(".oidf-card");
    expect(card).toBeTruthy();

    const cardBody = canvasElement.querySelector(".oidf-card-body");
    expect(cardBody).toBeTruthy();

    const container = cardBody.querySelector(".container");
    expect(container).toBeTruthy();

    const strong = container.querySelector("strong");
    expect(strong).toBeTruthy();
    expect(strong.textContent).toBe("Label:");

    const badge = container.querySelector("span.badge.bg-primary");
    expect(badge).toBeTruthy();
    expect(badge.textContent).toBe("Active");
  },
};

export const ToneOrange = {
  render: () =>
    html`<cts-card header="Active Run" tone="orange"><p>Brand bar in OIDF orange.</p></cts-card>`,

  async play({ canvasElement }) {
    const card = canvasElement.querySelector(".oidf-card");
    expect(card).toBeTruthy();

    const bar = card.querySelector(".oidf-card-bar");
    expect(bar).toBeTruthy();
    expect(bar.style.background).toBe("var(--orange-400)");

    // Bar must be the first child of the card (sits above the header).
    expect(card.firstElementChild).toBe(bar);

    const computed = getComputedStyle(bar);
    expect(computed.position).toBe("absolute");
    expect(computed.height).toBe("3px");
  },
};

export const ToneRust = {
  render: () =>
    html`<cts-card header="Failed Run" tone="rust"><p>Brand bar in OIDF rust.</p></cts-card>`,

  async play({ canvasElement }) {
    const card = canvasElement.querySelector(".oidf-card");
    expect(card).toBeTruthy();

    const bar = card.querySelector(".oidf-card-bar");
    expect(bar).toBeTruthy();
    expect(bar.style.background).toBe("var(--rust-400)");
  },
};

export const ToneSand = {
  render: () =>
    html`<cts-card header="Idle Plan" tone="sand"><p>Brand bar in OIDF sand.</p></cts-card>`,

  async play({ canvasElement }) {
    const card = canvasElement.querySelector(".oidf-card");
    expect(card).toBeTruthy();

    const bar = card.querySelector(".oidf-card-bar");
    expect(bar).toBeTruthy();
    expect(bar.style.background).toBe("var(--sand-300)");
  },
};

export const UnknownToneNoBar = {
  render: () =>
    html`<cts-card header="Unknown tone" tone="periwinkle"
      ><p>Unknown tone values render no brand bar.</p></cts-card
    >`,

  async play({ canvasElement }) {
    const card = canvasElement.querySelector(".oidf-card");
    expect(card).toBeTruthy();

    const bar = canvasElement.querySelector(".oidf-card-bar");
    expect(bar).toBeNull();
  },
};
