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
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    expect(canvasElement.querySelector(".modal-title").textContent).toBe(
      "Error Details",
    );

    const paragraphs = canvasElement.querySelectorAll(".modal-body p");
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
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Large Modal
      </button>
      <cts-modal heading="Large Modal" size="lg">
        <p>This dialog uses <code>size="lg"</code> for a wider layout.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    const dialog = canvasElement.querySelector(".modal-dialog");
    expect(dialog.classList.contains("modal-lg")).toBe(true);
  },
};

export const SizeSmall = {
  render: () => html`
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Small Modal
      </button>
      <cts-modal heading="Small Modal" size="sm">
        <p>This dialog uses <code>size="sm"</code> for a narrower layout.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    const dialog = canvasElement.querySelector(".modal-dialog");
    expect(dialog.classList.contains("modal-sm")).toBe(true);
  },
};

export const SizeExtraLarge = {
  render: () => html`
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Extra Large Modal
      </button>
      <cts-modal heading="Extra Large Modal" size="xl">
        <p>This dialog uses <code>size="xl"</code> for the widest layout.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    const dialog = canvasElement.querySelector(".modal-dialog");
    expect(dialog.classList.contains("modal-xl")).toBe(true);
  },
};

export const SizeInvalid = {
  render: () => html`
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Modal (invalid size)
      </button>
      <cts-modal heading="Invalid Size" size="banana">
        <p>Invalid size values are ignored — renders at default width.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    const dialog = canvasElement.querySelector(".modal-dialog");
    expect(dialog.className).toBe("modal-dialog");
  },
};

export const FooterButtonsConfirmation = {
  render: () => html`
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Delete Token
      </button>
      <cts-modal
        heading="Delete Token"
        footer-buttons='[
          {"label": "Delete", "class": "btn-danger", "id": "confirmDelete"},
          {"label": "Cancel"}
        ]'
      >
        <p>Are you sure you want to delete this token?</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    // Verify two custom buttons rendered (no auto Close)
    const buttons = canvasElement.querySelectorAll(".modal-footer button");
    expect(buttons.length).toBe(2);

    // Delete button has correct class, id, and dismiss
    const deleteBtn = buttons[0];
    expect(deleteBtn.textContent).toBe("Delete");
    expect(deleteBtn.classList.contains("btn-danger")).toBe(true);
    expect(deleteBtn.classList.contains("btn-sm")).toBe(true);
    expect(deleteBtn.id).toBe("confirmDelete");
    expect(deleteBtn.getAttribute("data-bs-dismiss")).toBe("modal");

    // Cancel button uses default btn-light class
    const cancelBtn = buttons[1];
    expect(cancelBtn.textContent).toBe("Cancel");
    expect(cancelBtn.classList.contains("btn-light")).toBe(true);
  },
};

export const FooterButtonsDismissFalse = {
  render: () => html`
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Delete Plan
      </button>
      <cts-modal
        heading="Delete Plan"
        footer-buttons='[
          {"label": "Cancel"},
          {"label": "Delete plan", "class": "btn-danger", "id": "confirmDeletePlanBtn", "dismiss": false}
        ]'
      >
        <p>This will permanently delete the plan and all test results.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    const buttons = canvasElement.querySelectorAll(".modal-footer button");
    expect(buttons.length).toBe(2);

    // Cancel has dismiss
    expect(buttons[0].getAttribute("data-bs-dismiss")).toBe("modal");

    // Delete button does NOT dismiss (JS controls the lifecycle)
    const deleteBtn = buttons[1];
    expect(deleteBtn.textContent).toBe("Delete plan");
    expect(deleteBtn.id).toBe("confirmDeletePlanBtn");
    expect(deleteBtn.hasAttribute("data-bs-dismiss")).toBe(false);

    // Clicking the non-dismiss button keeps the modal open
    await userEvent.click(deleteBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });
  },
};

export const FooterButtonsWithDataAttributes = {
  render: () => html`
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Publish
      </button>
      <cts-modal
        heading="Publish"
        footer-buttons='[
          {"label": "Publish", "data": {"publish": "everything"}},
          {"label": "Cancel"}
        ]'
      >
        <p>Publish all results?</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    const publishBtn = canvasElement.querySelector(".modal-footer button");
    expect(publishBtn.textContent).toBe("Publish");
    expect(publishBtn.getAttribute("data-publish")).toBe("everything");
  },
};

export const FooterButtonsMalformedJson = {
  render: () => html`
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Modal (malformed JSON)
      </button>
      <cts-modal heading="Malformed JSON" footer-buttons="not valid json">
        <p>Malformed JSON falls back to the auto-generated Close button.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    // Malformed JSON → fallback to auto Close button
    const buttons = canvasElement.querySelectorAll(".modal-footer button");
    expect(buttons.length).toBe(1);
    expect(buttons[0].textContent).toBe("Close");
  },
};

export const FooterButtonsGetElementById = {
  render: () => html`
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Modal
      </button>
      <cts-modal
        id="testFooterBtnId"
        heading="ID Test"
        footer-buttons='[{"label": "Confirm", "id": "confirmBtn"}]'
      >
        <p>The Confirm button should be reachable via getElementById.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    // Button is reachable via getElementById (critical for JS binding compat)
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

/**
 * Footer-button icon support. Renders a `<span class="bi bi-...">` inside
 * the button. The private-link modal on plan-detail relied on a pre-MR
 * box-arrow-in-right icon on its Copy button; restoring that affordance
 * drove this feature.
 */
export const FooterButtonsWithIcon = {
  render: () => html`
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Copy Modal
      </button>
      <cts-modal
        heading="Copy Result"
        footer-buttons='[
          {"label": "Copy to clipboard", "class": "btn-primary", "icon": "box-arrow-in-right", "dismiss": false},
          {"label": "Close"}
        ]'
      >
        <p>Here is your private link.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    const copyBtn = canvasElement.querySelectorAll(".modal-footer button")[0];
    expect(copyBtn.textContent.trim()).toContain("Copy to clipboard");

    const icon = copyBtn.querySelector("span.bi");
    expect(icon).toBeTruthy();
    expect(icon.classList.contains("bi-box-arrow-in-right")).toBe(true);
    expect(icon.getAttribute("aria-hidden")).toBe("true");
  },
};

/**
 * Unsanitizable icon values must be dropped entirely — we do NOT render
 * `<span class="bi bi-">` which would show as a broken invisible icon. Ensures
 * no HTML/class injection via the descriptor string.
 */
export const FooterButtonsIconRejected = {
  render: () => html`
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open
      </button>
      <cts-modal
        heading="Injection Test"
        footer-buttons='[{"label": "Bad", "icon": "<script>alert(1)</script>"}]'
      >
        <p>Bad icon value must be rejected.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    const btn = canvasElement.querySelector(".modal-footer button");
    // No span — sanitizer dropped the bad value instead of emitting bi-<script>...
    expect(btn.querySelector("span.bi")).toBeNull();
    // And no script tag was smuggled in either.
    expect(canvasElement.querySelector("script")).toBeNull();
    expect(btn.textContent.trim()).toBe("Bad");
  },
};

/**
 * Regression test for R-I3: desc.class="btn-outline-primary" used to be
 * silently downgraded to btn-light because outline-* wasn't in VARIANT_CLASSES.
 */
export const FooterButtonsOutlineVariantIntact = {
  render: () => html`
    <div>
      <button
        type="button"
        class="btn btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open
      </button>
      <cts-modal
        heading="Variant Test"
        footer-buttons='[
          {"label": "Outline Primary", "class": "btn-outline-primary", "id": "outlineBtn"},
          {"label": "Unknown Class", "class": "btn-custom-theme", "id": "customBtn"}
        ]'
      >
        <p>Both buttons should keep their caller-supplied class intact.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement }) {
    const triggerBtn = canvasElement.querySelector(".btn-primary");
    await userEvent.click(triggerBtn);
    await waitFor(() => {
      expect(canvasElement.querySelector(".modal.show")).toBeTruthy();
    });

    const outlineBtn = document.getElementById("outlineBtn");
    expect(outlineBtn).toBeTruthy();
    // The outline variant must be preserved — NOT downgraded to btn-light.
    expect(outlineBtn.classList.contains("btn-outline-primary")).toBe(true);
    expect(outlineBtn.classList.contains("btn-light")).toBe(false);

    // Truly unknown btn-* values fall through to additive-class mode rather
    // than silently becoming btn-light. Caller-supplied class is preserved.
    const customBtn = document.getElementById("customBtn");
    expect(customBtn).toBeTruthy();
    expect(customBtn.classList.contains("btn-custom-theme")).toBe(true);
  },
};

/**
 * aria-modal contract — set at connectedCallback regardless of show state,
 * per the solution doc at docs/solutions/web-components/cts-modal-bootstrap-interop-2026-04-17.md.
 */
export const AriaAttributes = {
  render: () =>
    html`<cts-modal id="ariaModal" heading="ARIA Test">
      <p>Dialog semantics.</p>
    </cts-modal>`,

  async play() {
    const modalDiv = document.getElementById("ariaModal");
    expect(modalDiv).toBeTruthy();
    expect(modalDiv.getAttribute("role")).toBe("dialog");
    expect(modalDiv.getAttribute("aria-modal")).toBe("true");
    expect(modalDiv.getAttribute("aria-labelledby")).toBe("ariaModal-title");
    const title = modalDiv.querySelector("#ariaModal-title");
    expect(title).toBeTruthy();
    expect(title.textContent).toBe("ARIA Test");
  },
};
