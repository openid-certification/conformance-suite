/**
 * Declarative wrapper around Bootstrap's Tooltip plugin. Applies
 * `data-bs-toggle="tooltip"` + `title` to the first child element and
 * instantiates a `bootstrap.Tooltip` on connect; disposes the instance on
 * disconnect.
 *
 * Vanilla HTMLElement — has no `static properties`; attributes are read
 * directly in `connectedCallback`.
 *
 * @property {string} content - Tooltip text (read from the `content`
 *   attribute).
 * @property {string} placement - Tooltip placement (read from the
 *   `placement` attribute); defaults to `top`.
 */
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
