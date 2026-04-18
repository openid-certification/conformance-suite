/**
 * Bootstrap-styled card container. Wraps its children in a `.card` /
 * `.card-body` structure, with an optional `.card-header` driven by the
 * `header` attribute.
 *
 * Vanilla HTMLElement — has no `static properties`; attributes are read
 * directly in `connectedCallback`.
 * @property {string} header - Optional header text (read from the `header`
 *   attribute). When omitted, no header is rendered.
 */
class CtsCard extends HTMLElement {
  connectedCallback() {
    const header = this.getAttribute("header");
    const children = Array.from(this.childNodes);

    const card = document.createElement("div");
    card.className = "card";

    if (header) {
      const cardHeader = document.createElement("div");
      cardHeader.className = "card-header bg-gradient";
      cardHeader.textContent = header;
      card.appendChild(cardHeader);
    }

    const cardBody = document.createElement("div");
    cardBody.className = "card-body";
    for (const child of children) {
      cardBody.appendChild(child);
    }
    card.appendChild(cardBody);

    this.appendChild(card);
  }
}

customElements.define("cts-card", CtsCard);
