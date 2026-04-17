import { html } from "lit";
import { expect, userEvent, waitFor } from "storybook/test";
import "./cts-tabs.js";

export default {
  title: "Components/cts-tabs",
  component: "cts-tabs",
};

export const Default = {
  render: () => html`
    <cts-tabs>
      <cts-tab-panel label="Form" id="formTab">
        <p>Form content here</p>
      </cts-tab-panel>
      <cts-tab-panel label="JSON" id="jsonTab">
        <p>JSON content here</p>
      </cts-tab-panel>
    </cts-tabs>
  `,

  async play({ canvasElement }) {
    const tablist = canvasElement.querySelector('[role="tablist"]');
    expect(tablist).toBeTruthy();

    const tabs = canvasElement.querySelectorAll('[role="tab"]');
    expect(tabs.length).toBe(2);

    expect(tabs[0].getAttribute("aria-selected")).toBe("true");
    expect(tabs[1].getAttribute("aria-selected")).toBe("false");

    // Tab controls panel by original id
    expect(tabs[0].getAttribute("aria-controls")).toBe("formTab");

    const panels = canvasElement.querySelectorAll('[role="tabpanel"]');
    expect(panels.length).toBe(2);
    expect(panels[0].id).toBe("formTab");
    expect(panels[0].getAttribute("aria-labelledby")).toBe("formTab-tab");
    expect(panels[0].hidden).toBe(false);
    expect(panels[1].hidden).toBe(true);

    expect(panels[0].textContent).toContain("Form content here");
  },
};

export const SwitchTab = {
  render: () => html`
    <cts-tabs>
      <cts-tab-panel label="Form" id="tabA"><p>Form tab</p></cts-tab-panel>
      <cts-tab-panel label="JSON" id="tabB"><p>JSON tab</p></cts-tab-panel>
    </cts-tabs>
  `,

  async play({ canvasElement }) {
    const tabs = canvasElement.querySelectorAll('[role="tab"]');
    const panels = canvasElement.querySelectorAll('[role="tabpanel"]');

    await userEvent.click(tabs[1]);

    await waitFor(() => {
      expect(tabs[0].getAttribute("aria-selected")).toBe("false");
      expect(tabs[1].getAttribute("aria-selected")).toBe("true");
      expect(panels[0].hidden).toBe(true);
      expect(panels[1].hidden).toBe(false);
    });

    expect(panels[1].textContent).toContain("JSON tab");
  },
};

export const KeyboardNavigation = {
  render: () => html`
    <cts-tabs>
      <cts-tab-panel label="Tab A" id="kbA"><p>A</p></cts-tab-panel>
      <cts-tab-panel label="Tab B" id="kbB"><p>B</p></cts-tab-panel>
      <cts-tab-panel label="Tab C" id="kbC"><p>C</p></cts-tab-panel>
    </cts-tabs>
  `,

  async play({ canvasElement }) {
    const tabs = canvasElement.querySelectorAll('[role="tab"]');

    tabs[0].focus();
    expect(document.activeElement).toBe(tabs[0]);

    await userEvent.keyboard("{ArrowRight}");
    expect(document.activeElement).toBe(tabs[1]);
    expect(tabs[1].getAttribute("aria-selected")).toBe("true");

    await userEvent.keyboard("{ArrowRight}");
    expect(document.activeElement).toBe(tabs[2]);

    // Wraps around
    await userEvent.keyboard("{ArrowRight}");
    expect(document.activeElement).toBe(tabs[0]);

    await userEvent.keyboard("{ArrowLeft}");
    expect(document.activeElement).toBe(tabs[2]);

    await userEvent.keyboard("{Home}");
    expect(document.activeElement).toBe(tabs[0]);

    await userEvent.keyboard("{End}");
    expect(document.activeElement).toBe(tabs[2]);
  },
};

/**
 * Enter / Space on the focused tab re-dispatches `cts-tab-change`. With
 * automatic activation (arrow keys already select on focus), these are
 * defensive no-op aliases — but consumers writing manual-activation UIs
 * expect Enter/Space to activate, so cts-tabs supports both patterns.
 */
export const KeyboardActivation = {
  render: () => html`
    <cts-tabs>
      <cts-tab-panel label="Alpha" id="kaA"><p>Alpha</p></cts-tab-panel>
      <cts-tab-panel label="Beta" id="kaB"><p>Beta</p></cts-tab-panel>
    </cts-tabs>
  `,

  async play({ canvasElement }) {
    const tabs = canvasElement.querySelectorAll('[role="tab"]');
    const events = [];
    canvasElement.addEventListener("cts-tab-change", (e) => events.push(e.detail.id));

    tabs[1].focus();
    // Arrow keys already selected tab[1]; Enter on the focused tab should
    // re-fire cts-tab-change (defensively).
    await userEvent.keyboard("{Enter}");
    await waitFor(() => {
      expect(events.filter((id) => id === "kaB").length).toBeGreaterThanOrEqual(1);
    });

    // Space on the focused tab also fires.
    const beforeSpace = events.length;
    await userEvent.keyboard(" ");
    await waitFor(() => {
      expect(events.length).toBeGreaterThan(beforeSpace);
    });
  },
};

export const TabChangeEvent = {
  render: () => html`
    <cts-tabs>
      <cts-tab-panel label="First" id="evtA"><p>First</p></cts-tab-panel>
      <cts-tab-panel label="Second" id="evtB"><p>Second</p></cts-tab-panel>
    </cts-tabs>
  `,

  async play({ canvasElement }) {
    let selectedId = null;
    canvasElement.addEventListener("cts-tab-change", (e) => {
      selectedId = e.detail.id;
    });

    const tabs = canvasElement.querySelectorAll('[role="tab"]');
    await userEvent.click(tabs[1]);

    expect(selectedId).toBe("evtB");
  },
};

export const ThreeTabs = {
  render: () => html`
    <cts-tabs>
      <cts-tab-panel label="Overview" id="t1"><h3>Overview</h3><p>Summary info.</p></cts-tab-panel>
      <cts-tab-panel label="Details" id="t2"><h3>Details</h3><p>Detailed info.</p></cts-tab-panel>
      <cts-tab-panel label="History" id="t3"><h3>History</h3><p>Change log.</p></cts-tab-panel>
    </cts-tabs>
  `,

  async play({ canvasElement }) {
    const tabs = canvasElement.querySelectorAll('[role="tab"]');
    expect(tabs.length).toBe(3);
    expect(tabs[0].textContent.trim()).toBe("Overview");
    expect(tabs[1].textContent.trim()).toBe("Details");
    expect(tabs[2].textContent.trim()).toBe("History");
  },
};
