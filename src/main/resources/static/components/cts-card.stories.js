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
    const card = canvasElement.querySelector(".card");
    expect(card).toBeTruthy();

    const cardHeader = canvasElement.querySelector(".card-header");
    expect(cardHeader).toBeTruthy();
    expect(cardHeader.textContent.trim()).toBe("Server Configuration");
    expect(cardHeader.classList.contains("bg-gradient")).toBe(true);

    const cardBody = canvasElement.querySelector(".card-body");
    expect(cardBody).toBeTruthy();

    const paragraphs = cardBody.querySelectorAll("p");
    expect(paragraphs.length).toBe(2);
    expect(paragraphs[0].textContent).toBe("Body content");
    expect(paragraphs[1].textContent).toBe("More content");
  },
};

export const WithoutHeader = {
  render: () => html`<cts-card><p>Body only.</p></cts-card>`,

  async play({ canvasElement }) {
    const card = canvasElement.querySelector(".card");
    expect(card).toBeTruthy();

    const cardHeader = canvasElement.querySelector(".card-header");
    expect(cardHeader).toBeNull();

    const cardBody = canvasElement.querySelector(".card-body");
    expect(cardBody).toBeTruthy();

    const paragraph = cardBody.querySelector("p");
    expect(paragraph).toBeTruthy();
    expect(paragraph.textContent).toBe("Body only.");
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
    const card = canvasElement.querySelector(".card");
    expect(card).toBeTruthy();

    const cardBody = canvasElement.querySelector(".card-body");
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
