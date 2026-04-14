import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { withMockFetch } from "@fixtures/helpers.js";
import { MOCK_LOG_ENTRIES, MOCK_EMPTY_LOG, MOCK_SUCCESS_LOG } from "@fixtures/mock-log-entries.js";
import "./cts-log-viewer.js";

export default {
  title: "Components/cts-log-viewer",
  component: "cts-log-viewer",
};

async function waitForLogLoad(canvasElement) {
  await waitFor(
    () => {
      const spinner = canvasElement.querySelector(".spinner-border");
      expect(spinner).toBeNull();
    },
    { timeout: 3000 },
  );
}

export const WithEntries = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () => html`<cts-log-viewer test-id="test-abc-123"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);
    const badges = canvasElement.querySelectorAll("cts-badge");
    expect(badges.length).toBeGreaterThan(0);
    const entries = canvasElement.querySelectorAll(".logItem");
    expect(entries.length).toBeGreaterThan(0);
    const canvas = within(canvasElement);
    expect(canvas.getByText(/ID token signature validation failed/)).toBeTruthy();
  },
};

export const CollapsibleBlocks = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES)],
  render: () => html`<cts-log-viewer test-id="test-abc-123"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);
    const blockStarts = canvasElement.querySelectorAll(".startBlock");
    expect(blockStarts.length).toBeGreaterThan(0);
    const firstBlock = blockStarts[0];
    await userEvent.click(firstBlock);
    const chevronAfterCollapse = firstBlock.querySelector(".bi");
    expect(chevronAfterCollapse.classList.contains("bi-chevron-right")).toBe(true);
    await userEvent.click(firstBlock);
    const chevronAfterExpand = firstBlock.querySelector(".bi");
    expect(chevronAfterExpand.classList.contains("bi-chevron-down")).toBe(true);
  },
};

export const EmptyLog = {
  decorators: [withMockFetch("/api/log/", MOCK_EMPTY_LOG)],
  render: () => html`<cts-log-viewer test-id="test-empty-789"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);
    const canvas = within(canvasElement);
    expect(canvas.getByText("No log entries")).toBeTruthy();
  },
};

export const Loading = {
  decorators: [withMockFetch("/api/log/", MOCK_LOG_ENTRIES, { delay: 60000 })],
  render: () => html`<cts-log-viewer test-id="test-loading"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitFor(() => {
      const spinner = canvasElement.querySelector(".spinner-border");
      expect(spinner).toBeTruthy();
    });
  },
};

export const AllSuccess = {
  decorators: [withMockFetch("/api/log/", MOCK_SUCCESS_LOG)],
  render: () => html`<cts-log-viewer test-id="test-ok-456"></cts-log-viewer>`,
  async play({ canvasElement }) {
    await waitForLogLoad(canvasElement);
    const badges = canvasElement.querySelectorAll("cts-badge");
    for (const badge of badges) {
      if (badge.getAttribute("variant")) {
        expect(badge.getAttribute("variant")).toBe("success");
      }
    }
  },
};
