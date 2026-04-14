import { html } from "lit";
import { expect, userEvent, waitFor } from "storybook/test";
import "./cts-modal.js";

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
  },
};

export const ShowAndHide = {
  render: () =>
    html`<cts-modal heading="Show/Hide Test">
      <p>This modal should open and close.</p>
    </cts-modal>`,

  async play({ canvasElement }) {
    const ctsModal = canvasElement.querySelector("cts-modal");
    const modalEl = canvasElement.querySelector(".modal");

    // Modal should start hidden
    expect(modalEl.classList.contains("show")).toBe(false);

    // show() should open it
    ctsModal.show();
    await waitFor(() => {
      expect(modalEl.classList.contains("show")).toBe(true);
    });

    // hide() should close it
    ctsModal.hide();
    await waitFor(() => {
      expect(modalEl.classList.contains("show")).toBe(false);
    });
  },
};

export const CloseEvent = {
  render: () =>
    html`<cts-modal heading="Close Event Test">
      <p>Listening for cts-modal-close event.</p>
    </cts-modal>`,

  async play({ canvasElement }) {
    const ctsModal = canvasElement.querySelector("cts-modal");

    let closeEventFired = false;
    ctsModal.addEventListener("cts-modal-close", () => {
      closeEventFired = true;
    });

    // Open then close — the hidden.bs.modal event should fire cts-modal-close
    ctsModal.show();
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    ctsModal.hide();
    await waitFor(() => {
      expect(closeEventFired).toBe(true);
    });
  },
};

export const CloseViaButton = {
  render: () => html`
    <cts-modal heading="Button Close Test">
      <p>Close via the X button.</p>
    </cts-modal>
  `,

  async play({ canvasElement }) {
    const ctsModal = canvasElement.querySelector("cts-modal");
    const closeBtn = canvasElement.querySelector(".btn-close");

    // Open the modal
    ctsModal.show();
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    // Click the close button
    await userEvent.click(closeBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeNull();
    });
  },
};

export const BootstrapApiCompat = {
  render: () =>
    html`<cts-modal id="testBackcompat" heading="Bootstrap API Test">
      <p>Works with bootstrap.Modal.getOrCreateInstance()</p>
    </cts-modal>`,

  async play() {
    // getElementById should find the inner .modal div (id transferred from host)
    const modalDiv = document.getElementById("testBackcompat");
    expect(modalDiv).toBeTruthy();
    expect(modalDiv.classList.contains("modal")).toBe(true);

    // bootstrap.Modal API should work directly
    const bsModal = bootstrap.Modal.getOrCreateInstance(modalDiv);
    expect(bsModal).toBeTruthy();

    bsModal.show();
    await waitFor(() => {
      expect(modalDiv.classList.contains("show")).toBe(true);
    });

    bsModal.hide();
    await waitFor(() => {
      expect(modalDiv.classList.contains("show")).toBe(false);
    });
  },
};

export const StaticBackdrop = {
  render: () =>
    html`<cts-modal id="testStatic" heading="Loading..." static-backdrop no-keyboard>
      <div class="text-center"><span class="spinner-border"></span></div>
    </cts-modal>`,

  async play({ canvasElement }) {
    const modalDiv = document.getElementById("testStatic");
    expect(modalDiv).toBeTruthy();
    expect(modalDiv.getAttribute("data-bs-backdrop")).toBe("static");
    expect(modalDiv.getAttribute("data-bs-keyboard")).toBe("false");

    // Static backdrop modals should not have a close button or footer
    const closeBtn = canvasElement.querySelector(".btn-close");
    expect(closeBtn).toBeNull();
    const footer = canvasElement.querySelector(".modal-footer");
    expect(footer).toBeNull();
  },
};
