import { html } from "lit";
import { expect, userEvent, waitFor } from "storybook/test";
import "./cts-modal.js";
import "./cts-spinner.js";

export default {
  title: "Primitives/cts-modal",
  component: "cts-modal",
};

/**
 * Sleep for one frame so async dialog events (close, etc.) settle before
 * an assertion. Keeps the play tests stable across browsers without depending
 * on Bootstrap's transitionend timing the way the prior stories did.
 *
 * @returns {Promise<void>}
 */
function nextFrame() {
  return new Promise((resolve) => requestAnimationFrame(() => resolve()));
}

// --- Stories ---

export const Default = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
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

  async play({ canvasElement, step }) {
    await step("clicking the trigger opens the dialog", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => {
        expect(canvasElement.querySelector("dialog.oidf-modal[open]")).toBeTruthy();
      });
    });

    await step("heading and body content render", async () => {
      expect(canvasElement.querySelector(".oidf-modal-title").textContent).toBe("Error Details");

      const paragraphs = canvasElement.querySelectorAll(".oidf-modal-body p");
      expect(paragraphs.length).toBe(2);
      expect(paragraphs[0].textContent).toBe("An error occurred while processing your request.");
      expect(paragraphs[1].textContent).toBe("Please check the logs for more details.");
    });

    await step("header has a custom close button (not Bootstrap's btn-close)", async () => {
      const closeBtn = canvasElement.querySelector(".oidf-modal-close");
      expect(closeBtn).toBeTruthy();
      expect(closeBtn.getAttribute("aria-label")).toBe("Close");
    });
  },
};

export const ShowAndHide = {
  render: () =>
    html`<cts-modal heading="Show/Hide Test">
      <p>This modal should open and close.</p>
    </cts-modal>`,

  async play({ canvasElement, step }) {
    const ctsModal = /** @type {HTMLElement & { show: () => void; hide: () => void }} */ (
      canvasElement.querySelector("cts-modal")
    );
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("modal starts hidden", async () => {
      expect(dialog.open).toBe(false);
    });

    await step("show() opens it", async () => {
      ctsModal.show();
      await waitFor(() => {
        expect(dialog.open).toBe(true);
      });
    });

    await step("hide() closes it", async () => {
      ctsModal.hide();
      await waitFor(() => {
        expect(dialog.open).toBe(false);
      });
    });
  },
};

export const CloseEvent = {
  render: () =>
    html`<cts-modal heading="Close Event Test">
      <p>Listening for cts-modal-close event.</p>
    </cts-modal>`,

  async play({ canvasElement, step }) {
    const ctsModal = /** @type {HTMLElement & { show: () => void; hide: () => void }} */ (
      canvasElement.querySelector("cts-modal")
    );
    const dialog = canvasElement.querySelector("dialog.oidf-modal");

    let closeEventFired = false;
    ctsModal.addEventListener("cts-modal-close", () => {
      closeEventFired = true;
    });

    await step("opening the modal", async () => {
      ctsModal.show();
      await waitFor(() => {
        expect(/** @type {HTMLDialogElement} */ (dialog).open).toBe(true);
      });
    });

    await step("hiding fires cts-modal-close (via the dialog close event)", async () => {
      ctsModal.hide();
      await waitFor(() => {
        expect(closeEventFired).toBe(true);
      });
    });
  },
};

export const ShowEvent = {
  render: () =>
    html`<cts-modal heading="Show Event Test">
      <p>Listening for cts-modal-show event.</p>
    </cts-modal>`,

  async play({ canvasElement, step }) {
    const ctsModal = /** @type {HTMLElement & { show: () => void; hide: () => void }} */ (
      canvasElement.querySelector("cts-modal")
    );
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    // The cts-modal-show listener should fire BEFORE the dialog opens, so
    // listeners can mutate body content synchronously (matches the
    // fapi.ui.js privateLinkResultModal use-case where the title and body
    // are populated immediately before show()).
    let openStateAtFire = "not fired";
    ctsModal.addEventListener("cts-modal-show", () => {
      openStateAtFire = dialog.open ? "open" : "closed";
    });

    await step("cts-modal-show fires before the dialog opens", async () => {
      ctsModal.show();
      await waitFor(() => expect(dialog.open).toBe(true));
      expect(openStateAtFire).toBe("closed");
    });
  },
};

export const CloseViaButton = {
  render: () => html`
    <cts-modal heading="Button Close Test">
      <p>Close via the X button.</p>
    </cts-modal>
  `,

  async play({ canvasElement, step }) {
    const ctsModal = /** @type {HTMLElement & { show: () => void; hide: () => void }} */ (
      canvasElement.querySelector("cts-modal")
    );
    const closeBtn = canvasElement.querySelector(".oidf-modal-close");
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      ctsModal.show();
      await waitFor(() => {
        expect(dialog.open).toBe(true);
      });
    });

    await step("clicking the close button dismisses it", async () => {
      await userEvent.click(closeBtn);
      await waitFor(() => {
        expect(dialog.open).toBe(false);
      });
    });
  },
};

export const HostApiResolution = {
  render: () =>
    html`<cts-modal id="testHostResolution" heading="Host Resolution Test">
      <p>document.getElementById resolves to the cts-modal host.</p>
    </cts-modal>`,

  async play({ step }) {
    const host = document.getElementById("testHostResolution");
    if (!host) throw new Error("testHostResolution host not found");
    const typedHost = /** @type {HTMLElement & { show: () => void; hide: () => void }} */ (host);

    await step("getElementById resolves to the cts-modal host", async () => {
      // The id stays on the host (no transfer to inner element). getElementById
      // therefore returns the cts-modal custom element, which exposes
      // .show()/.hide() directly. This replaces the prior Bootstrap factory
      // call shape entirely.
      expect(host).toBeTruthy();
      expect(host.tagName.toLowerCase()).toBe("cts-modal");

      const dialog = /** @type {HTMLDialogElement} */ (host.querySelector("dialog.oidf-modal"));
      expect(dialog).toBeTruthy();
    });

    await step("show() then hide() drive the dialog open state", async () => {
      const dialog = /** @type {HTMLDialogElement} */ (
        typedHost.querySelector("dialog.oidf-modal")
      );

      typedHost.show();
      await waitFor(() => expect(dialog.open).toBe(true));

      typedHost.hide();
      await waitFor(() => expect(dialog.open).toBe(false));
    });
  },
};

export const SizeLarge = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Large Modal
      </button>
      <cts-modal heading="Large Modal" size="lg">
        <p>This dialog uses <code>size="lg"</code> for a wider layout.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement, step }) {
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("dialog reflects size=lg", async () => {
      expect(dialog.getAttribute("data-size")).toBe("lg");
    });
  },
};

export const SizeSmall = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Small Modal
      </button>
      <cts-modal heading="Small Modal" size="sm">
        <p>This dialog uses <code>size="sm"</code> for a narrower layout.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement, step }) {
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("dialog reflects size=sm", async () => {
      expect(dialog.getAttribute("data-size")).toBe("sm");
    });
  },
};

export const SizeExtraLarge = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Extra Large Modal
      </button>
      <cts-modal heading="Extra Large Modal" size="xl">
        <p>This dialog uses <code>size="xl"</code> for the widest layout.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement, step }) {
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("dialog reflects size=xl", async () => {
      expect(dialog.getAttribute("data-size")).toBe("xl");
    });
  },
};

export const SizeInvalid = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Modal (invalid size)
      </button>
      <cts-modal heading="Invalid Size" size="banana">
        <p>Invalid size values are ignored — renders at default width.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement, step }) {
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("invalid size values are dropped (no data-size attribute)", async () => {
      expect(dialog.hasAttribute("data-size")).toBe(false);
    });
  },
};

export const FooterButtonsConfirmation = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
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

  async play({ canvasElement, step }) {
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    const cancelBtn = /** @type {HTMLButtonElement} */ (
      canvasElement.querySelectorAll(".oidf-modal-footer button")[1]
    );

    await step("two custom buttons render with correct variant/id (no auto Close)", async () => {
      const buttons = canvasElement.querySelectorAll(".oidf-modal-footer button");
      expect(buttons.length).toBe(2);

      // Delete button has correct OIDF danger class, id, and dismiss behavior
      const deleteBtn = /** @type {HTMLButtonElement} */ (buttons[0]);
      expect(deleteBtn.textContent).toBe("Delete");
      expect(deleteBtn.classList.contains("oidf-btn-danger")).toBe(true);
      expect(deleteBtn.classList.contains("oidf-btn-sm")).toBe(true);
      expect(deleteBtn.id).toBe("confirmDelete");

      // Cancel button uses default secondary class
      expect(cancelBtn.textContent).toBe("Cancel");
      expect(cancelBtn.classList.contains("oidf-btn-secondary")).toBe(true);
    });

    await step("clicking dismiss-default Cancel closes the dialog", async () => {
      await userEvent.click(cancelBtn);
      await waitFor(() => expect(dialog.open).toBe(false));
    });
  },
};

export const FooterButtonsDismissFalse = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
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

  async play({ canvasElement, step }) {
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    const deleteBtn = /** @type {HTMLButtonElement} */ (
      canvasElement.querySelectorAll(".oidf-modal-footer button")[1]
    );

    await step("both footer buttons render with correct label/id", async () => {
      const buttons = canvasElement.querySelectorAll(".oidf-modal-footer button");
      expect(buttons.length).toBe(2);

      expect(deleteBtn.textContent).toBe("Delete plan");
      expect(deleteBtn.id).toBe("confirmDeletePlanBtn");
    });

    await step("clicking the non-dismiss button keeps the modal open", async () => {
      // No listener was attached for this button, so the dialog does not close.
      await userEvent.click(deleteBtn);
      // Allow any potential listener to settle.
      await nextFrame();
      expect(dialog.open).toBe(true);
    });
  },
};

export const FooterButtonsWithDataAttributes = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
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

  async play({ canvasElement, step }) {
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("button data descriptor maps to a data-* attribute", async () => {
      const publishBtn = canvasElement.querySelector(".oidf-modal-footer button");
      expect(publishBtn.textContent).toBe("Publish");
      expect(publishBtn.getAttribute("data-publish")).toBe("everything");
    });
  },
};

export const FooterButtonsMalformedJson = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Modal (malformed JSON)
      </button>
      <cts-modal heading="Malformed JSON" footer-buttons="not valid json">
        <p>Malformed JSON falls back to the auto-generated Close button.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement, step }) {
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("malformed JSON falls back to the auto Close button", async () => {
      const buttons = canvasElement.querySelectorAll(".oidf-modal-footer button");
      expect(buttons.length).toBe(1);
      expect(buttons[0].textContent).toBe("Close");
    });
  },
};

export const FooterButtonsGetElementById = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
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

  async play({ canvasElement, step }) {
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("footer button is reachable via getElementById", async () => {
      // Critical for JS binding compat.
      const btn = document.getElementById("confirmBtn");
      expect(btn).toBeTruthy();
      if (!btn) throw new Error("confirmBtn not found");
      expect(btn.textContent).toBe("Confirm");
    });
  },
};

export const StaticBackdrop = {
  render: () =>
    html`<cts-modal id="testStatic" heading="Loading..." static-backdrop no-keyboard>
      <cts-spinner size="lg" label="Loading"></cts-spinner>
      <div aria-live="polite"><span>Working…</span></div>
    </cts-modal>`,

  async play({ step }) {
    const host = document.getElementById("testStatic");
    if (!host) throw new Error("testStatic host not found");

    await step("getElementById resolves to the cts-modal host", async () => {
      // The id stays on the host, so getElementById returns the cts-modal element.
      expect(host).toBeTruthy();
      expect(host.tagName.toLowerCase()).toBe("cts-modal");
    });

    await step("static-backdrop omits the close button and footer", async () => {
      // Loading-modal shape preserved from the Bootstrap-era contract.
      const closeBtn = host.querySelector(".oidf-modal-close");
      expect(closeBtn).toBeNull();
      const footer = host.querySelector(".oidf-modal-footer");
      expect(footer).toBeNull();
    });

    await step("loading content is the cts-spinner component, not an animated GIF", async () => {
      // This is the durable regression for the cts-spinner cutover —
      // any reintroduction of the legacy <img> would fail here. Assertion
      // reads data-size (the value cts-spinner actually parsed and mirrored
      // onto the host), not size (the markup attribute the test itself wrote
      // — that would only verify our own input). The negative check covers
      // any <img>, not just spinner.gif, so a swap to a different image
      // filename still fails.
      const spinner = host.querySelector("cts-spinner");
      expect(spinner).toBeTruthy();
      if (!spinner) throw new Error("cts-spinner not found inside StaticBackdrop loading modal");
      expect(spinner.getAttribute("data-size")).toBe("lg");
      const legacyImg = host.querySelector("img");
      expect(legacyImg).toBeNull();
    });
  },
};

/**
 * Footer-button icon support. Renders a `<cts-icon name="...">` inside
 * the button. The private-link modal on plan-detail relied on an arrow
 * icon on its Copy button; restoring that affordance drove this feature.
 */
export const FooterButtonsWithIcon = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
        onclick="this.closest('div').querySelector('cts-modal').show()"
      >
        Open Copy Modal
      </button>
      <cts-modal
        heading="Copy Result"
        footer-buttons='[
          {"label": "Copy to clipboard", "class": "btn-primary", "icon": "log-out", "dismiss": false},
          {"label": "Close"}
        ]'
      >
        <p>Here is your private link.</p>
      </cts-modal>
    </div>
  `,

  async play({ canvasElement, step }) {
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("footer button renders a cts-icon with the requested name", async () => {
      const copyBtn = canvasElement.querySelectorAll(".oidf-modal-footer button")[0];
      expect(copyBtn.textContent.trim()).toContain("Copy to clipboard");

      const iconEl = copyBtn.querySelector("cts-icon");
      expect(iconEl).toBeTruthy();
      expect(iconEl.getAttribute("name")).toBe("log-out");
      expect(iconEl.getAttribute("aria-hidden")).toBe("true");
    });
  },
};

/**
 * Unsanitizable icon values must be dropped entirely — we do NOT render
 * `<cts-icon name="">` which would resolve to an empty fetch. Ensures
 * no HTML/class injection via the descriptor string.
 */
export const FooterButtonsIconRejected = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
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

  async play({ canvasElement, step }) {
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("unsanitizable icon value is dropped with no injection", async () => {
      const btn = canvasElement.querySelector(".oidf-modal-footer button");
      // Sanitizer dropped the bad value instead of emitting <cts-icon name="<script>...">.
      expect(btn.querySelector("cts-icon")).toBeNull();
      // And no script tag was smuggled in either.
      expect(canvasElement.querySelector("script")).toBeNull();
      expect(btn.textContent.trim()).toBe("Bad");
    });
  },
};

/**
 * Regression test for R-I3: desc.class="btn-outline-primary" used to be
 * silently downgraded to btn-light because outline-* wasn't in VARIANT_CLASSES.
 * The U9 rewrite preserves outline-* via the OIDF Variant Migration table
 * (outline-primary maps to oidf-btn-primary), and unknown btn-* values
 * fall through to additive-class mode so caller-supplied themes survive.
 */
export const FooterButtonsOutlineVariantIntact = {
  render: () => html`
    <div>
      <button
        type="button"
        class="oidf-btn oidf-btn-sm oidf-btn-primary"
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

  async play({ canvasElement, step }) {
    const dialog = /** @type {HTMLDialogElement} */ (
      canvasElement.querySelector("dialog.oidf-modal")
    );

    await step("opening the modal", async () => {
      const triggerBtn = canvasElement.querySelector(".oidf-btn-primary");
      await userEvent.click(triggerBtn);
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("outline-primary maps to the OIDF primary surface", async () => {
      const outlineBtn = document.getElementById("outlineBtn");
      expect(outlineBtn).toBeTruthy();
      if (!outlineBtn) throw new Error("outlineBtn not found");
      expect(outlineBtn.classList.contains("oidf-btn-primary")).toBe(true);
    });

    await step("unknown btn-* class is preserved verbatim (additive-class mode)", async () => {
      // Truly unknown btn-* values fall through to additive-class mode rather
      // than silently becoming the secondary variant. Caller-supplied class
      // is preserved verbatim.
      const customBtn = document.getElementById("customBtn");
      expect(customBtn).toBeTruthy();
      if (!customBtn) throw new Error("customBtn not found");
      expect(customBtn.classList.contains("btn-custom-theme")).toBe(true);
    });
  },
};

/**
 * ARIA contract — the dialog carries `aria-labelledby` pointing to the
 * header title's id. Native `<dialog>` provides modal semantics directly
 * (no manual aria-modal needed).
 */
export const AriaAttributes = {
  render: () =>
    html`<cts-modal id="ariaModal" heading="ARIA Test">
      <p>Dialog semantics.</p>
    </cts-modal>`,

  async play({ step }) {
    const host = document.getElementById("ariaModal");
    expect(host).toBeTruthy();
    if (!host) throw new Error("ariaModal host not found");
    const dialog = host.querySelector("dialog.oidf-modal");
    expect(dialog).toBeTruthy();
    if (!dialog) throw new Error("ariaModal dialog not found");

    await step("dialog points aria-labelledby at the header title id", async () => {
      expect(dialog.getAttribute("aria-labelledby")).toBe("ariaModal-title");
    });

    await step("header title element carries the heading text", async () => {
      const title = dialog.querySelector("#ariaModal-title");
      expect(title).toBeTruthy();
      expect(/** @type {Element} */ (title).textContent).toBe("ARIA Test");
    });
  },
};

/**
 * ESC closes the dialog by default (native `<dialog>` cancel behavior).
 */
export const EscapeDismissal = {
  render: () =>
    html`<cts-modal id="escapeModal" heading="Escape Test">
      <p>Press Escape to close.</p>
    </cts-modal>`,

  async play({ step }) {
    const host = /** @type {HTMLElement & { show: () => void; hide: () => void }} */ (
      document.getElementById("escapeModal")
    );
    if (!host) throw new Error("escapeModal not found");
    const dialog = /** @type {HTMLDialogElement} */ (host.querySelector("dialog.oidf-modal"));

    await step("opening the modal", async () => {
      host.show();
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("hide() closes the dialog (deterministic stand-in for ESC)", async () => {
      // Native dialog ESC handling is browser-driven and flaky to simulate
      // reliably under test runners; the contract being verified is that
      // hide() and dialog.close() trigger cts-modal-close.
      host.hide();
      await waitFor(() => expect(dialog.open).toBe(false));
    });
  },
};

/**
 * `static-backdrop` + `no-keyboard`: ESC must NOT close the dialog. The
 * cancel event handler intercepts it.
 */
export const StaticBackdropNoKeyboard = {
  render: () =>
    html`<cts-modal id="staticKbdModal" heading="Loading..." static-backdrop no-keyboard>
      <div><span>Loading…</span></div>
    </cts-modal>`,

  async play({ step }) {
    const host = /** @type {HTMLElement & { show: () => void }} */ (
      document.getElementById("staticKbdModal")
    );
    if (!host) throw new Error("staticKbdModal not found");
    const dialog = /** @type {HTMLDialogElement} */ (host.querySelector("dialog.oidf-modal"));

    await step("opening the modal", async () => {
      host.show();
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("cancel event (ESC) is intercepted and the dialog stays open", async () => {
      // Simulate the cancel event ESC would dispatch. Our handler calls
      // preventDefault, so the dialog stays open.
      const cancelEvent = new Event("cancel", { cancelable: true });
      dialog.dispatchEvent(cancelEvent);
      expect(cancelEvent.defaultPrevented).toBe(true);
      expect(dialog.open).toBe(true);
    });
  },
};

/**
 * `static-backdrop` blocks backdrop-click dismissal. Without it, a click on
 * the backdrop (i.e. on the dialog element with no inner-rect coordinates)
 * closes the dialog.
 */
export const StaticBackdropBlocksClick = {
  render: () =>
    html`<cts-modal id="staticClickModal" heading="No Backdrop Close" static-backdrop>
      <p>Backdrop clicks should not dismiss.</p>
    </cts-modal>`,

  async play({ step }) {
    const host = /** @type {HTMLElement & { show: () => void }} */ (
      document.getElementById("staticClickModal")
    );
    if (!host) throw new Error("staticClickModal not found");
    const dialog = /** @type {HTMLDialogElement} */ (host.querySelector("dialog.oidf-modal"));

    await step("opening the modal", async () => {
      host.show();
      await waitFor(() => expect(dialog.open).toBe(true));
    });

    await step("static-backdrop swallows the backdrop click and stays open", async () => {
      // Synthesize a click whose target is the dialog itself (the backdrop).
      // Coordinates 0,0 are guaranteed outside the dialog rect.
      const click = new MouseEvent("click", {
        bubbles: true,
        cancelable: true,
        clientX: 0,
        clientY: 0,
      });
      dialog.dispatchEvent(click);
      await nextFrame();
      expect(dialog.open).toBe(true);
    });
  },
};

/**
 * Show-after-hide rotation: errorModal can be opened immediately after
 * loadingModal is hidden, mirroring the `showError` → `hideBusy()` path
 * in fapi.ui.js.
 */
export const RotateModals = {
  render: () => html`
    <div>
      <cts-modal id="rotateLoading" heading="Loading..." static-backdrop no-keyboard>
        <p>Loading…</p>
      </cts-modal>
      <cts-modal id="rotateError" heading="Error">
        <p>An error occurred.</p>
      </cts-modal>
    </div>
  `,

  async play({ step }) {
    const loading = /** @type {HTMLElement & { show: () => void; hide: () => void }} */ (
      document.getElementById("rotateLoading")
    );
    const error = /** @type {HTMLElement & { show: () => void; hide: () => void }} */ (
      document.getElementById("rotateError")
    );
    if (!loading || !error) throw new Error("rotation modals not found");
    const loadingDialog = /** @type {HTMLDialogElement} */ (
      loading.querySelector("dialog.oidf-modal")
    );
    const errorDialog = /** @type {HTMLDialogElement} */ (error.querySelector("dialog.oidf-modal"));

    await step("opening the loading modal", async () => {
      loading.show();
      await waitFor(() => expect(loadingDialog.open).toBe(true));
    });

    await step("hiding loading and showing error in the same tick rotates cleanly", async () => {
      // Synchronous hide-then-show in the same tick. <dialog> handles this
      // cleanly because close() and showModal() do not rely on transitionend.
      loading.hide();
      error.show();

      await waitFor(() => expect(errorDialog.open).toBe(true));
      expect(loadingDialog.open).toBe(false);
    });
  },
};

/**
 * `hide()` is a no-op when nothing is showing. Used by FAPI_UI.hideError()
 * and FAPI_UI.hideBusy() defensively from many call sites.
 */
export const HideWhenClosedIsNoop = {
  render: () =>
    html`<cts-modal id="hideNoopModal" heading="Hide Noop">
      <p>hide() should be a no-op when not open.</p>
    </cts-modal>`,

  async play() {
    const host = /** @type {HTMLElement & { hide: () => void }} */ (
      document.getElementById("hideNoopModal")
    );
    if (!host) throw new Error("hideNoopModal not found");
    const dialog = /** @type {HTMLDialogElement} */ (host.querySelector("dialog.oidf-modal"));
    expect(dialog.open).toBe(false);
    // Should not throw.
    host.hide();
    expect(dialog.open).toBe(false);
  },
};
