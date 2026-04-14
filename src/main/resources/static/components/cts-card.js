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
