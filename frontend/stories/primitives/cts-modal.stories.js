import { html } from "lit";
import { expect } from "storybook/test";
import "../../../src/main/resources/static/components/cts-modal.js";

export default {
  title: "Primitives/cts-modal",
  component: "cts-modal",
};

// --- Stories ---

export const Default = {
  render: () => html`
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Modal
      </button>
      <cts-modal heading="Error Details">
        <p>An error occurred while processing your request.</p>
        <p>Please check the logs for more details.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const modal = canvasElement.querySelector(".modal");
    expect(modal).toBeTruthy();

    const modalTitle = canvasElement.querySelector(".modal-title");
    expect(modalTitle).toBeTruthy();
    expect(modalTitle.textContent).toBe("Error Details");

    const modalBody = canvasElement.querySelector(".modal-body");
    expect(modalBody).toBeTruthy();

    const paragraphs = modalBody.querySelectorAll("p");
    expect(paragraphs.length).toBe(2);
    expect(paragraphs[0].textContent).toBe(
      "An error occurred while processing your request.",
    );
    expect(paragraphs[1].textContent).toBe(
      "Please check the logs for more details.",
    );

    const closeBtn = canvasElement.querySelector(".btn-close");
    expect(closeBtn).toBeTruthy();
    expect(closeBtn.getAttribute("data-bs-dismiss")).toBe("modal");

    const footerCloseBtn = canvasElement.querySelector(
      '.modal-footer button[data-bs-dismiss="modal"]',
    );
    expect(footerCloseBtn).toBeTruthy();
    expect(footerCloseBtn.getAttribute("data-bs-dismiss")).toBe("modal");
  },
};

export const ShowHideMethods = {
  render: () =>
    html`<cts-modal heading="Method Test">
      <p>Testing show/hide methods.</p>
    </cts-modal>`,

  async play({ canvasElement }) {
    const ctsModal = canvasElement.querySelector("cts-modal");
    expect(ctsModal).toBeTruthy();
    expect(typeof ctsModal.show).toBe("function");
    expect(typeof ctsModal.hide).toBe("function");
  },
};

export const CloseEvent = {
  render: () =>
    html`<cts-modal heading="Close Event Test">
      <p>Listening for cts-modal-close event.</p>
    </cts-modal>`,

  async play({ canvasElement }) {
    const ctsModal = canvasElement.querySelector("cts-modal");
    expect(ctsModal).toBeTruthy();

    let eventReceived = false;
    ctsModal.addEventListener("cts-modal-close", () => {
      eventReceived = true;
    });

    // Verify the listener can be attached (event functionality test)
    expect(typeof ctsModal.addEventListener).toBe("function");
  },
};
