import { html } from "lit";
import { expect } from "storybook/test";
import "./cts-stat.js";

export default {
  title: "Primitives/cts-stat",
  component: "cts-stat",
  argTypes: {
    label: { control: "text" },
    value: { control: "text" },
    delta: { control: "text" },
    tone: {
      control: "select",
      options: ["", "pass", "fail"],
    },
  },
};

// --- Stories ---

export const Default = {
  args: {
    label: "Total tests",
    value: "128",
    delta: "+12 since yesterday",
    tone: "",
  },
  render: ({ label, value, delta, tone }) =>
    html`<cts-stat label="${label}" value="${value}" delta="${delta}" tone="${tone}"></cts-stat>`,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-stat");
    expect(host).toBeTruthy();

    const label = host.querySelector(".oidf-stat-label");
    const value = host.querySelector(".oidf-stat-value");
    const delta = host.querySelector(".oidf-stat-delta");

    expect(label).toBeTruthy();
    expect(label.classList.contains("t-overline")).toBe(true);
    expect(label.textContent.trim()).toBe("Total tests");

    expect(value).toBeTruthy();
    expect(value.textContent.trim()).toBe("128");
    // Default tone leaves the value at --fg (no tone colour).
    const valueComputed = window.getComputedStyle(value);
    expect(valueComputed.fontFamily).toContain("Inter");
    expect(valueComputed.fontWeight).toBe("700");
    expect(valueComputed.fontSize).toBe("32px");

    expect(delta).toBeTruthy();
    expect(delta.classList.contains("t-meta")).toBe(true);
    expect(delta.textContent.trim()).toBe("+12 since yesterday");
  },
};

export const TonePass = {
  args: {
    label: "Passed",
    value: "98",
    delta: "+4 today",
    tone: "pass",
  },
  render: ({ label, value, delta, tone }) =>
    html`<cts-stat label="${label}" value="${value}" delta="${delta}" tone="${tone}"></cts-stat>`,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-stat");
    const value = host.querySelector(".oidf-stat-value");
    const delta = host.querySelector(".oidf-stat-delta");

    expect(value).toBeTruthy();
    // tone="pass" must paint the value with --status-pass (#2F7D3C).
    expect(window.getComputedStyle(value).color).toBe("rgb(47, 125, 60)");

    // The delta mirrors the tone so the trend reads at a glance.
    expect(delta).toBeTruthy();
    expect(window.getComputedStyle(delta).color).toBe("rgb(47, 125, 60)");
  },
};

export const ToneFail = {
  args: {
    label: "Failed",
    value: "7",
    delta: "+2 today",
    tone: "fail",
  },
  render: ({ label, value, delta, tone }) =>
    html`<cts-stat label="${label}" value="${value}" delta="${delta}" tone="${tone}"></cts-stat>`,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-stat");
    const value = host.querySelector(".oidf-stat-value");
    const delta = host.querySelector(".oidf-stat-delta");

    expect(value).toBeTruthy();
    // tone="fail" must paint the value with --rust-400 (#A43604).
    expect(window.getComputedStyle(value).color).toBe("rgb(164, 54, 4)");

    expect(delta).toBeTruthy();
    expect(window.getComputedStyle(delta).color).toBe("rgb(164, 54, 4)");
  },
};

export const NoDelta = {
  args: {
    label: "In review",
    value: "3",
    delta: "",
    tone: "",
  },
  render: ({ label, value, delta, tone }) =>
    html`<cts-stat label="${label}" value="${value}" delta="${delta}" tone="${tone}"></cts-stat>`,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-stat");
    const label = host.querySelector(".oidf-stat-label");
    const value = host.querySelector(".oidf-stat-value");
    const delta = host.querySelector(".oidf-stat-delta");

    expect(label).toBeTruthy();
    expect(value).toBeTruthy();
    expect(value.textContent.trim()).toBe("3");

    // Edge case: missing delta omits the delta line entirely.
    expect(delta).toBeNull();
  },
};

export const UnknownToneFallsBackToDefault = {
  args: {
    label: "Unknown",
    value: "42",
    delta: "no change",
    tone: "wat",
  },
  render: ({ label, value, delta, tone }) =>
    html`<cts-stat label="${label}" value="${value}" delta="${delta}" tone="${tone}"></cts-stat>`,

  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-stat");
    const value = host.querySelector(".oidf-stat-value");
    const delta = host.querySelector(".oidf-stat-delta");

    expect(value).toBeTruthy();
    // Unknown tones fall back to --fg (ink-900 = #1A1611).
    expect(window.getComputedStyle(value).color).toBe("rgb(26, 22, 17)");

    // Delta falls back to --fg-soft (ink-500 = #71695E).
    expect(delta).toBeTruthy();
    expect(window.getComputedStyle(delta).color).toBe("rgb(113, 105, 94)");
  },
};

export const Grid = {
  render: () => html`
    <div
      style="display: grid; grid-template-columns: repeat(3, minmax(180px, 1fr)); gap: var(--space-6); padding: var(--space-6);"
    >
      <cts-stat label="Total tests" value="128" delta="+12 since yesterday"></cts-stat>
      <cts-stat tone="pass" label="Passed" value="98" delta="+4 today"></cts-stat>
      <cts-stat tone="fail" label="Failed" value="7" delta="+2 today"></cts-stat>
    </div>
  `,

  async play({ canvasElement }) {
    const tiles = canvasElement.querySelectorAll("cts-stat");
    expect(tiles.length).toBe(3);
    expect(tiles[0].querySelector(".oidf-stat-value").textContent.trim()).toBe("128");
    expect(tiles[1].querySelector(".oidf-stat-value").textContent.trim()).toBe("98");
    expect(tiles[2].querySelector(".oidf-stat-value").textContent.trim()).toBe("7");
  },
};
