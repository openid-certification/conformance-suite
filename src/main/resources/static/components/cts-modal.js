class CtsModal extends HTMLElement {
  connectedCallback() {
    const heading = this.getAttribute("heading");
    const staticBackdrop = this.hasAttribute("static-backdrop");
    const noKeyboard = this.hasAttribute("no-keyboard");
    const hostId = this.id;
    const children = Array.from(this.childNodes);

    // Build modal wrapper
    const modal = document.createElement("div");
    modal.className = "modal";
    modal.setAttribute("tabindex", "-1");
    modal.setAttribute("role", "dialog");

    // Transfer id from host to inner .modal so getElementById + bootstrap.Modal works
    if (hostId) {
      modal.id = hostId;
      modal.setAttribute("aria-labelledby", hostId + "-title");
      this.removeAttribute("id");
    }

    if (staticBackdrop) {
      modal.setAttribute("data-bs-backdrop", "static");
    }
    if (noKeyboard) {
      modal.setAttribute("data-bs-keyboard", "false");
    }

    // modal-dialog
    const dialog = document.createElement("div");
    dialog.className = "modal-dialog";
    dialog.setAttribute("role", "document");

    // modal-content
    const content = document.createElement("div");
    content.className = "modal-content";

    // modal-header
    const header = document.createElement("div");
    header.className = "modal-header";

    const title = document.createElement("h4");
    title.className = "modal-title";
    if (hostId) {
      title.id = hostId + "-title";
    }
    title.textContent = heading || "";

    header.appendChild(title);

    // Close button in header (skip for static-backdrop loading modals)
    if (!staticBackdrop) {
      const closeBtn = document.createElement("button");
      closeBtn.type = "button";
      closeBtn.className = "btn-close";
      closeBtn.setAttribute("data-bs-dismiss", "modal");
      closeBtn.setAttribute("aria-label", "Close");
      header.appendChild(closeBtn);
    }

    // modal-body — move captured children here
    const body = document.createElement("div");
    body.className = "modal-body";
    for (const child of children) {
      body.appendChild(child);
    }

    content.appendChild(header);
    content.appendChild(body);

    // modal-footer (skip for static-backdrop loading modals)
    if (!staticBackdrop) {
      const footer = document.createElement("div");
      footer.className = "modal-footer";

      const footerCloseBtn = document.createElement("button");
      footerCloseBtn.type = "button";
      footerCloseBtn.className =
        "btn btn-sm btn-light bg-gradient border border-secondary";
      footerCloseBtn.setAttribute("data-bs-dismiss", "modal");
      footerCloseBtn.textContent = "Close";

      footer.appendChild(footerCloseBtn);
      content.appendChild(footer);
    }

    // Assemble
    dialog.appendChild(content);
    modal.appendChild(dialog);
    this.appendChild(modal);

    // Dispatch cts-modal-close when Bootstrap hides the modal
    modal.addEventListener("hidden.bs.modal", () => {
      this.dispatchEvent(
        new CustomEvent("cts-modal-close", { bubbles: true, composed: true }),
      );
    });

    this._modalEl = modal;
  }

  show() {
    if (typeof bootstrap === "undefined" || !this._modalEl) return;
    bootstrap.Modal.getOrCreateInstance(this._modalEl).show();
  }

  hide() {
    if (typeof bootstrap === "undefined" || !this._modalEl) return;
    bootstrap.Modal.getOrCreateInstance(this._modalEl).hide();
  }
}

customElements.define("cts-modal", CtsModal);
