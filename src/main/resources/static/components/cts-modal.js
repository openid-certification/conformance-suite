class CtsModal extends HTMLElement {
  connectedCallback() {
    const heading = this.getAttribute("heading");
    const children = Array.from(this.childNodes);

    // Build modal wrapper
    const modal = document.createElement("div");
    modal.className = "modal";
    modal.setAttribute("tabindex", "-1");
    modal.setAttribute("role", "dialog");
    if (this.id) {
      modal.setAttribute("aria-labelledby", this.id + "-title");
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
    if (this.id) {
      title.id = this.id + "-title";
    }
    title.textContent = heading || "";

    const closeBtn = document.createElement("button");
    closeBtn.type = "button";
    closeBtn.className = "btn-close";
    closeBtn.setAttribute("data-bs-dismiss", "modal");
    closeBtn.setAttribute("aria-label", "Close");

    header.appendChild(title);
    header.appendChild(closeBtn);

    // modal-body — move captured children here
    const body = document.createElement("div");
    body.className = "modal-body";
    for (const child of children) {
      body.appendChild(child);
    }

    // modal-footer
    const footer = document.createElement("div");
    footer.className = "modal-footer";

    const footerCloseBtn = document.createElement("button");
    footerCloseBtn.type = "button";
    footerCloseBtn.className =
      "btn btn-sm btn-light bg-gradient border border-secondary";
    footerCloseBtn.setAttribute("data-bs-dismiss", "modal");
    footerCloseBtn.textContent = "Close";

    footer.appendChild(footerCloseBtn);

    // Assemble
    content.appendChild(header);
    content.appendChild(body);
    content.appendChild(footer);
    dialog.appendChild(content);
    modal.appendChild(dialog);
    this.appendChild(modal);

    // Dispatch cts-modal-close when Bootstrap hides the modal
    modal.addEventListener("hidden.bs.modal", () => {
      this.dispatchEvent(
        new CustomEvent("cts-modal-close", { bubbles: true, composed: true }),
      );
    });
  }

  show() {
    if (typeof bootstrap === "undefined") return;
    const modal = this.querySelector(".modal");
    if (modal) {
      bootstrap.Modal.getOrCreateInstance(modal).show();
    }
  }

  hide() {
    if (typeof bootstrap === "undefined") return;
    const modal = this.querySelector(".modal");
    if (modal) {
      bootstrap.Modal.getOrCreateInstance(modal).hide();
    }
  }
}

customElements.define("cts-modal", CtsModal);
