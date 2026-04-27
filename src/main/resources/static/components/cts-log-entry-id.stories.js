import { html } from "lit";
import { expect, fn, spyOn, waitFor } from "storybook/test";
import "./cts-log-entry-id.js";

export default {
  title: "Components/cts-log-entry-id",
  component: "cts-log-entry-id",
};

const REFERENCE_ID = "LOG-0042";
const TEST_ID = "abc123";

/** Default chip — left-click copies the deep URL. */
export const Default = {
  render: () =>
    html`<cts-log-entry-id reference-id=${REFERENCE_ID} test-id=${TEST_ID}></cts-log-entry-id>`,
  async play({ canvasElement }) {
    const writeSpy = spyOn(navigator.clipboard, "writeText").mockResolvedValue();
    const handler = fn();
    document.addEventListener("cts-reference-copied", handler);

    try {
      const button = await waitFor(() => {
        const el = canvasElement.querySelector('[data-testid="log-entry-id-chip"]');
        if (!el) throw new Error("chip not yet rendered");
        return /** @type {HTMLButtonElement} */ (el);
      });

      // The visible label is the reference id; the icon comes from cts-icon.
      expect(button.textContent).toContain(REFERENCE_ID);
      expect(canvasElement.querySelector('cts-icon[name="link"]')).toBeTruthy();

      // Real button (focusable, keyboard-activated by default).
      expect(button.tagName).toBe("BUTTON");
      expect(button.getAttribute("aria-label")).toContain(REFERENCE_ID);

      await button.click();

      await waitFor(() => expect(writeSpy).toHaveBeenCalledOnce());
      const copied = writeSpy.mock.calls[0][0];
      // The deep URL must always carry both the testId and the reference
      // hash so cross-run citation in Slack/Jira disambiguates by test.
      expect(copied).toContain(`log=${TEST_ID}`);
      expect(copied).toContain(`#${REFERENCE_ID}`);
      expect(copied).toContain("log-detail.html");

      // The component dispatches cts-reference-copied with mode 'url'.
      expect(handler).toHaveBeenCalledOnce();
      const evt = handler.mock.calls[0][0];
      expect(evt.detail.mode).toBe("url");
      expect(evt.detail.referenceId).toBe(REFERENCE_ID);
      expect(evt.detail.value).toBe(copied);
      // composed: true so a document-level listener catches the event
      // even if a future host moves the chip into a shadow DOM.
      expect(evt.composed).toBe(true);
    } finally {
      document.removeEventListener("cts-reference-copied", handler);
    }
  },
};

/**
 * Right-click / long-press copies the plain `LOG-NNNN` reference (no
 * URL) for in-document citation. The synthesized `contextmenu` event is
 * how mobile long-press surfaces in modern browsers.
 */
export const RightClickCopiesPlain = {
  render: () =>
    html`<cts-log-entry-id reference-id=${REFERENCE_ID} test-id=${TEST_ID}></cts-log-entry-id>`,
  async play({ canvasElement }) {
    const writeSpy = spyOn(navigator.clipboard, "writeText").mockResolvedValue();
    const handler = fn();
    document.addEventListener("cts-reference-copied", handler);

    try {
      const button = await waitFor(() => {
        const el = canvasElement.querySelector('[data-testid="log-entry-id-chip"]');
        if (!el) throw new Error("chip not yet rendered");
        return /** @type {HTMLButtonElement} */ (el);
      });

      // dispatchEvent with cancelable: true so preventDefault can be observed.
      const ctx = new MouseEvent("contextmenu", { bubbles: true, cancelable: true });
      const ok = button.dispatchEvent(ctx);

      // The chip preventDefaults the context menu so the native browser
      // menu does not appear; dispatchEvent returns false when canceled.
      expect(ok).toBe(false);

      await waitFor(() => expect(writeSpy).toHaveBeenCalledOnce());
      const copied = writeSpy.mock.calls[0][0];
      // Plain reference — no URL, no hash, no testId.
      expect(copied).toBe(REFERENCE_ID);

      expect(handler).toHaveBeenCalledOnce();
      expect(handler.mock.calls[0][0].detail.mode).toBe("plain");
    } finally {
      document.removeEventListener("cts-reference-copied", handler);
    }
  },
};

/**
 * After a successful copy the chip briefly carries the
 * `.logIdChip--copied` modifier so the user gets visual confirmation
 * even though the clipboard write itself is invisible.
 */
export const CopiedFeedback = {
  render: () =>
    html`<cts-log-entry-id reference-id=${REFERENCE_ID} test-id=${TEST_ID}></cts-log-entry-id>`,
  async play({ canvasElement }) {
    spyOn(navigator.clipboard, "writeText").mockResolvedValue();

    const button = await waitFor(() => {
      const el = canvasElement.querySelector('[data-testid="log-entry-id-chip"]');
      if (!el) throw new Error("chip not yet rendered");
      return /** @type {HTMLButtonElement} */ (el);
    });

    expect(button.classList.contains("logIdChip--copied")).toBe(false);
    await button.click();
    await waitFor(() => {
      expect(button.classList.contains("logIdChip--copied")).toBe(true);
    });
  },
};

/**
 * No `referenceId` → the component renders nothing. Lets host components
 * pass an empty string when references aren't yet available without
 * having to conditionally include the element.
 */
export const EmptyReference = {
  render: () => html`<cts-log-entry-id reference-id="" test-id=${TEST_ID}></cts-log-entry-id>`,
  async play({ canvasElement }) {
    // Wait one microtask for Lit's first render to flush.
    await Promise.resolve();
    const chip = canvasElement.querySelector('[data-testid="log-entry-id-chip"]');
    expect(chip).toBeNull();
  },
};
