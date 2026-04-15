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

export const SizeLarge = {
  render: () => html`
    <cts-modal heading="Large Modal" size="lg">
      <p>This modal should render with <code>modal-lg</code> on the dialog.</p>
    </cts-modal>
  `,

  async play({ canvasElement }) {
    const dialog = canvasElement.querySelector(".modal-dialog");
    expect(dialog).toBeTruthy();
    expect(dialog.classList.contains("modal-lg")).toBe(true);
  },
};

export const SizeSmall = {
  render: () => html`
    <cts-modal heading="Small Modal" size="sm">
      <p>This modal should render with <code>modal-sm</code> on the dialog.</p>
    </cts-modal>
  `,

  async play({ canvasElement }) {
    const dialog = canvasElement.querySelector(".modal-dialog");
    expect(dialog).toBeTruthy();
    expect(dialog.classList.contains("modal-sm")).toBe(true);
  },
};

export const SizeExtraLarge = {
  render: () => html`
    <cts-modal heading="Extra Large Modal" size="xl">
      <p>This modal should render with <code>modal-xl</code> on the dialog.</p>
    </cts-modal>
  `,

  async play({ canvasElement }) {
    const dialog = canvasElement.querySelector(".modal-dialog");
    expect(dialog).toBeTruthy();
    expect(dialog.classList.contains("modal-xl")).toBe(true);
  },
};

export const SizeInvalid = {
  render: () => html`
    <cts-modal heading="Invalid Size" size="banana">
      <p>Invalid size values should be ignored.</p>
    </cts-modal>
  `,

  async play({ canvasElement }) {
    const dialog = canvasElement.querySelector(".modal-dialog");
    expect(dialog).toBeTruthy();
    // Should only have "modal-dialog", no size class
    expect(dialog.className).toBe("modal-dialog");
  },
};

export const SizeDefault = {
  render: () => html`
    <cts-modal heading="Default Size">
      <p>No size attribute means default dialog width.</p>
    </cts-modal>
  `,

  async play({ canvasElement }) {
    const dialog = canvasElement.querySelector(".modal-dialog");
    expect(dialog).toBeTruthy();
    expect(dialog.className).toBe("modal-dialog");
  },
};

export const FooterButtonsConfirmation = {
  render: () => html`
    <cts-modal
      heading="Delete Token"
      footer-buttons='[
        {"label": "Delete", "class": "btn-danger", "id": "confirmDelete"},
        {"label": "Cancel"}
      ]'
    >
      <p>Are you sure you want to delete this token?</p>
    </cts-modal>
  `,

  async play({ canvasElement }) {
    const footer = canvasElement.querySelector(".modal-footer");
    expect(footer).toBeTruthy();

    const buttons = footer.querySelectorAll("button");
    expect(buttons.length).toBe(2);

    // Delete button
    const deleteBtn = buttons[0];
    expect(deleteBtn.textContent).toBe("Delete");
    expect(deleteBtn.classList.contains("btn-danger")).toBe(true);
    expect(deleteBtn.classList.contains("btn-sm")).toBe(true);
    expect(deleteBtn.id).toBe("confirmDelete");
    expect(deleteBtn.getAttribute("data-bs-dismiss")).toBe("modal");

    // Cancel button — default class is btn-light
    const cancelBtn = buttons[1];
    expect(cancelBtn.textContent).toBe("Cancel");
    expect(cancelBtn.classList.contains("btn-light")).toBe(true);
    expect(cancelBtn.getAttribute("data-bs-dismiss")).toBe("modal");

    // No auto-generated Close button
    const closeButtons = [...footer.querySelectorAll("button")].filter(
      (b) => b.textContent === "Close",
    );
    expect(closeButtons.length).toBe(0);
  },
};

export const FooterButtonsDismissFalse = {
  render: () => html`
    <cts-modal
      heading="Delete Plan"
      footer-buttons='[
        {"label": "Cancel"},
        {"label": "Delete plan", "class": "btn-danger", "id": "confirmDeletePlanBtn", "dismiss": false}
      ]'
    >
      <p>This will permanently delete the plan and all test results.</p>
    </cts-modal>
  `,

  async play({ canvasElement }) {
    const buttons = canvasElement.querySelectorAll(".modal-footer button");
    expect(buttons.length).toBe(2);

    // Cancel dismisses
    expect(buttons[0].getAttribute("data-bs-dismiss")).toBe("modal");

    // Delete does NOT dismiss (JS handles lifecycle)
    const deleteBtn = buttons[1];
    expect(deleteBtn.textContent).toBe("Delete plan");
    expect(deleteBtn.id).toBe("confirmDeletePlanBtn");
    expect(deleteBtn.hasAttribute("data-bs-dismiss")).toBe(false);
  },
};

export const FooterButtonsWithDataAttributes = {
  render: () => html`
    <cts-modal
      heading="Publish"
      footer-buttons='[
        {"label": "Publish", "data": {"publish": "everything"}},
        {"label": "Cancel"}
      ]'
    >
      <p>Publish all results?</p>
    </cts-modal>
  `,

  async play({ canvasElement }) {
    const buttons = canvasElement.querySelectorAll(".modal-footer button");
    expect(buttons.length).toBe(2);

    const publishBtn = buttons[0];
    expect(publishBtn.textContent).toBe("Publish");
    expect(publishBtn.getAttribute("data-publish")).toBe("everything");
    expect(publishBtn.getAttribute("data-bs-dismiss")).toBe("modal");
  },
};

export const FooterButtonsEmpty = {
  render: () => html`
    <cts-modal heading="Empty Footer" footer-buttons="[]">
      <p>Empty array renders footer with no buttons.</p>
    </cts-modal>
  `,

  async play({ canvasElement }) {
    const footer = canvasElement.querySelector(".modal-footer");
    expect(footer).toBeTruthy();
    expect(footer.querySelectorAll("button").length).toBe(0);
  },
};

export const FooterButtonsMalformedJson = {
  render: () => html`
    <cts-modal heading="Malformed JSON" footer-buttons="not valid json">
      <p>Malformed JSON falls back to auto Close button.</p>
    </cts-modal>
  `,

  async play({ canvasElement }) {
    // Malformed JSON → fallback to auto Close button
    const footer = canvasElement.querySelector(".modal-footer");
    expect(footer).toBeTruthy();

    const buttons = footer.querySelectorAll("button");
    expect(buttons.length).toBe(1);
    expect(buttons[0].textContent).toBe("Close");
    expect(buttons[0].getAttribute("data-bs-dismiss")).toBe("modal");
  },
};

export const FooterButtonsDefaultClosePreserved = {
  render: () => html`
    <cts-modal heading="Default Footer">
      <p>No footer-buttons → auto Close button.</p>
    </cts-modal>
  `,

  async play({ canvasElement }) {
    const footer = canvasElement.querySelector(".modal-footer");
    expect(footer).toBeTruthy();

    const buttons = footer.querySelectorAll("button");
    expect(buttons.length).toBe(1);
    expect(buttons[0].textContent).toBe("Close");
  },
};

export const FooterButtonsGetElementById = {
  render: () => html`
    <cts-modal
      id="testFooterBtnId"
      heading="ID Test"
      footer-buttons='[{"label": "Confirm", "id": "confirmBtn"}]'
    >
      <p>Button should be reachable via getElementById.</p>
    </cts-modal>
  `,

  async play() {
    // The button id should be findable via document.getElementById
    const btn = document.getElementById("confirmBtn");
    expect(btn).toBeTruthy();
    expect(btn.textContent).toBe("Confirm");
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
