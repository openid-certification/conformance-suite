class CtsTooltip extends HTMLElement {
  connectedCallback() {
    const content = this.getAttribute("content") || "";
    const placement = this.getAttribute("placement") || "top";
    const trigger = this.querySelector(":scope > *");

    if (trigger && content) {
      trigger.setAttribute("data-bs-toggle", "tooltip");
      trigger.setAttribute("data-bs-placement", placement);
      trigger.setAttribute("title", content);

      if (typeof bootstrap !== "undefined") {
        new bootstrap.Tooltip(trigger);
      }
    }
  }

  disconnectedCallback() {
    const trigger = this.querySelector(':scope > [data-bs-toggle="tooltip"]');
    if (trigger && typeof bootstrap !== "undefined") {
      const instance = bootstrap.Tooltip.getInstance(trigger);
      if (instance) {
        instance.dispose();
      }
    }
  }
}

customElements.define("cts-tooltip", CtsTooltip);
