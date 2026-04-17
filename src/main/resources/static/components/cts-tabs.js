// <cts-tab-panel> is a semantic container that holds tab content and metadata.
// <cts-tabs> restructures its <cts-tab-panel> children into a WCAG tablist.

/**
 * Semantic container used as a direct child of `<cts-tabs>`. A vanilla
 * HTMLElement — all attributes are consumed by the parent `<cts-tabs>`
 * during its `connectedCallback` restructure pass.
 *
 * @property {string} label - Visible tab label (read from the `label`
 *   attribute by the parent `<cts-tabs>`).
 * @property {string} id - Optional panel id used for the generated
 *   `role="tabpanel"` wrapper (defaults to `cts-tab-{index}`).
 */
class CtsTabPanel extends HTMLElement {}
customElements.define("cts-tab-panel", CtsTabPanel);

/**
 * Restructures its `<cts-tab-panel>` children into a WCAG-compliant tablist
 * with `role="tab"` buttons and `role="tabpanel"` wrappers. Supports
 * arrow-key / Home / End navigation.
 *
 * Vanilla HTMLElement — has no `static properties`; panel metadata comes
 * from attributes on each child `<cts-tab-panel>`.
 *
 * @fires cts-tab-change - When the active tab changes (click or keyboard),
 *   with `{ detail: { id } }` where `id` is the selected panel's id;
 *   bubbles.
 */
class CtsTabs extends HTMLElement {
  connectedCallback() {
    const panels = Array.from(this.querySelectorAll(":scope > cts-tab-panel"));
    if (panels.length === 0) return;

    const tablist = document.createElement("ul");
    tablist.className = "nav nav-tabs mb-3";
    tablist.setAttribute("role", "tablist");

    const panelContainer = document.createElement("div");

    panels.forEach((panel, index) => {
      const label = panel.getAttribute("label") || "Tab " + (index + 1);
      const panelId = panel.id || "cts-tab-" + index;
      const tabId = panelId + "-tab";
      const isFirst = index === 0;

      // Tab button
      const li = document.createElement("li");
      li.className = "nav-item";
      li.setAttribute("role", "presentation");

      const button = document.createElement("button");
      button.className = isFirst ? "nav-link active" : "nav-link";
      button.id = tabId;
      button.setAttribute("role", "tab");
      button.setAttribute("aria-selected", isFirst ? "true" : "false");
      button.setAttribute("aria-controls", panelId);
      button.setAttribute("tabindex", isFirst ? "0" : "-1");
      button.textContent = label;

      button.addEventListener("click", () => this._selectTab(tabId));
      button.addEventListener("keydown", (e) => this._handleKeydown(e));

      li.appendChild(button);
      tablist.appendChild(li);

      // Panel wrapper — uses original cts-tab-panel id so getElementById still works
      const wrapper = document.createElement("div");
      wrapper.id = panelId;
      wrapper.setAttribute("role", "tabpanel");
      wrapper.setAttribute("aria-labelledby", tabId);
      wrapper.hidden = !isFirst;

      while (panel.firstChild) {
        wrapper.appendChild(panel.firstChild);
      }

      panelContainer.appendChild(wrapper);
    });

    // Remove original <cts-tab-panel> elements
    panels.forEach((panel) => panel.remove());

    this.prepend(panelContainer);
    this.prepend(tablist);
  }

  _selectTab(tabId) {
    const allTabs = this.querySelectorAll('[role="tab"]');
    const allPanels = this.querySelectorAll('[role="tabpanel"]');

    allTabs.forEach((tab) => {
      const isSelected = tab.id === tabId;
      tab.setAttribute("aria-selected", isSelected ? "true" : "false");
      tab.setAttribute("tabindex", isSelected ? "0" : "-1");
      tab.classList.toggle("active", isSelected);
      if (isSelected) tab.focus();
    });

    allPanels.forEach((panel) => {
      panel.hidden = panel.getAttribute("aria-labelledby") !== tabId;
    });

    // Fire change event with original panel id
    const selectedTab = this.querySelector("#" + tabId);
    const panelId = selectedTab ? selectedTab.getAttribute("aria-controls") : "";
    this.dispatchEvent(
      new CustomEvent("cts-tab-change", {
        bubbles: true,
        detail: { id: panelId },
      }),
    );
  }

  _handleKeydown(e) {
    const tabs = Array.from(this.querySelectorAll('[role="tab"]'));
    const currentIndex = tabs.indexOf(e.target);
    let newIndex;

    switch (e.key) {
      case "ArrowRight":
        newIndex = (currentIndex + 1) % tabs.length;
        break;
      case "ArrowLeft":
        newIndex = (currentIndex - 1 + tabs.length) % tabs.length;
        break;
      case "Home":
        newIndex = 0;
        break;
      case "End":
        newIndex = tabs.length - 1;
        break;
      default:
        return;
    }

    e.preventDefault();
    this._selectTab(tabs[newIndex].id);
  }
}

customElements.define("cts-tabs", CtsTabs);
