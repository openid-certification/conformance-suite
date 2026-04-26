// <cts-tab-panel> is a semantic container that holds tab content and metadata.
// <cts-tabs> restructures its <cts-tab-panel> children into a WCAG tablist.

/**
 * Semantic container used as a direct child of `<cts-tabs>`. A vanilla
 * HTMLElement — all attributes are consumed by the parent `<cts-tabs>`
 * during its `connectedCallback` restructure pass.
 * @property {string} label - Visible tab label (read from the `label`
 *   attribute by the parent `<cts-tabs>`).
 * @property {string} count - Optional count badge text shown next to the
 *   label (read from the `count` attribute). Any string is rendered
 *   verbatim — e.g. `"24"`, `"1.4k"`. Omit to hide the badge.
 * @property {string} id - Optional panel id used for the generated
 *   `role="tabpanel"` wrapper (defaults to `cts-tab-{index}`).
 */
class CtsTabPanel extends HTMLElement {}
customElements.define("cts-tab-panel", CtsTabPanel);

const STYLE_ID = "cts-tabs-styles";

// Scoped CSS for the tablist. Mirrors project/preview/components-tabs.html
// from the OIDF design archive (underline tab style with an orange-400
// active indicator, plus count-badge tokens). All values flow from
// oidf-tokens.css; nothing here is hard-coded.
const STYLE_TEXT = `
.oidf-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--border);
  margin: 0 0 var(--space-5) 0;
  padding: 0;
  list-style: none;
  font-family: var(--font-sans);
}
.oidf-tabs > li {
  margin: 0;
  padding: 0;
  list-style: none;
}
.oidf-tab {
  appearance: none;
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  padding: var(--space-3) var(--space-4);
  font-family: inherit;
  font-size: var(--fs-13);
  font-weight: var(--fw-medium);
  color: var(--ink-500);
  cursor: pointer;
  transition: color var(--dur-1) var(--ease-standard),
              border-color var(--dur-1) var(--ease-standard);
}
.oidf-tab:hover {
  color: var(--ink-900);
}
.oidf-tab:focus {
  outline: none;
}
.oidf-tab:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
  border-radius: var(--radius-2);
}
.oidf-tab-active {
  color: var(--ink-900);
  border-bottom-color: var(--orange-400);
}
.oidf-tab-count {
  display: inline-block;
  margin-left: var(--space-2);
  padding: 1px var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--ink-100);
  color: var(--fg-muted);
  font-size: var(--fs-12);
  font-weight: var(--fw-medium);
  line-height: 1.4;
}
.oidf-tab-active .oidf-tab-count {
  background: var(--orange-50);
  color: var(--orange-600);
}
`;

function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE_TEXT;
  document.head.appendChild(style);
}

/**
 * Restructures its `<cts-tab-panel>` children into a WCAG-compliant tablist
 * with `role="tab"` buttons and `role="tabpanel"` wrappers. Supports
 * arrow-key / Home / End navigation.
 *
 * Vanilla HTMLElement — has no `static properties`; panel metadata comes
 * from attributes on each child `<cts-tab-panel>`.
 *
 * Light DOM. Scoped CSS lives in a single `<style>` element injected into
 * `<head>` on first connect (gated by a module-level flag) so the rules
 * appear once regardless of how many `cts-tabs` instances are on the page.
 *
 * @fires cts-tab-change - When the active tab changes (click or keyboard),
 *   with `{ detail: { id } }` where `id` is the selected panel's id;
 *   bubbles.
 */
class CtsTabs extends HTMLElement {
  connectedCallback() {
    injectStyles();

    const panels = Array.from(this.querySelectorAll(":scope > cts-tab-panel"));
    if (panels.length === 0) return;

    const tablist = document.createElement("ul");
    tablist.className = "oidf-tabs";
    tablist.setAttribute("role", "tablist");

    const panelContainer = document.createElement("div");

    panels.forEach((panel, index) => {
      const label = panel.getAttribute("label") || "Tab " + (index + 1);
      const count = panel.getAttribute("count");
      const panelId = panel.id || "cts-tab-" + index;
      const tabId = panelId + "-tab";
      const isFirst = index === 0;

      // Tab button
      const li = document.createElement("li");
      li.setAttribute("role", "presentation");

      const button = document.createElement("button");
      button.type = "button";
      button.className = isFirst ? "oidf-tab oidf-tab-active" : "oidf-tab";
      button.id = tabId;
      button.setAttribute("role", "tab");
      button.setAttribute("aria-selected", isFirst ? "true" : "false");
      button.setAttribute("aria-controls", panelId);
      button.setAttribute("tabindex", isFirst ? "0" : "-1");

      // Use a label span so the optional count badge can sit alongside it
      // without disturbing the tab's textContent contract used by stories.
      const labelSpan = document.createElement("span");
      labelSpan.className = "oidf-tab-label";
      labelSpan.textContent = label;
      button.appendChild(labelSpan);

      if (count !== null) {
        const countSpan = document.createElement("span");
        countSpan.className = "oidf-tab-count";
        countSpan.textContent = count;
        button.appendChild(countSpan);
      }

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
      tab.classList.toggle("oidf-tab-active", isSelected);
      if (isSelected) /** @type {HTMLElement} */ (tab).focus();
    });

    allPanels.forEach((panel) => {
      /** @type {HTMLElement} */ (panel).hidden = panel.getAttribute("aria-labelledby") !== tabId;
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

    // Enter / Space explicitly re-activate the focused tab. With automatic
    // activation (arrow keys already select on focus), these are defensive
    // redundancy — if a user presses Enter on the already-selected tab the
    // change event fires again, matching what a manual-activation tablist
    // would do. Consistent WCAG expectation, zero surprise.
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      if (currentIndex >= 0) this._selectTab(tabs[currentIndex].id);
      return;
    }

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

export {};
