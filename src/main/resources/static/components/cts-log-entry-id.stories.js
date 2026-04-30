import { html } from "lit";
import { expect, fn, spyOn, waitFor } from "storybook/test";
import "./cts-log-entry-id.js";

export default {
  title: "Components/cts-log-entry-id",
  component: "cts-log-entry-id",
};

const REFERENCE_ID = "LOG-0042";
const TEST_ID = "abc123";

/**
 * Resolves the chip's clickable inner span. The chip renders as a
 * <cts-badge variant="secondary" clickable>, which puts role="button"
 * + tabindex on the inner <span class="badge"> rather than the host.
 * Tests dispatch native clicks against that inner element; the custom
 * cts-badge-click event then bubbles to the cts-log-entry-id host.
 * @param {Element} canvasElement
 */
async function getClickTarget(canvasElement) {
  return waitFor(() => {
    const host = canvasElement.querySelector('[data-testid="log-entry-id-chip"]');
    if (!host) throw new Error("chip not yet rendered");
    const target = host.querySelector('[role="button"]');
    if (!target) throw new Error("clickable inner span not yet rendered");
    return /** @type {HTMLElement} */ (target);
  });
}

/** Default chip — left-click copies the deep URL. */
export const Default = {
  render: () =>
    html`<cts-log-entry-id reference-id=${REFERENCE_ID} test-id=${TEST_ID}></cts-log-entry-id>`,
  async play({ canvasElement }) {
    const writeSpy = spyOn(navigator.clipboard, "writeText").mockResolvedValue();
    const handler = fn();
    document.addEventListener("cts-reference-copied", handler);

    try {
      const clickTarget = await getClickTarget(canvasElement);

      // The visible label is the reference id; the icon comes from cts-icon.
      expect(clickTarget.textContent).toContain(REFERENCE_ID);
      expect(canvasElement.querySelector('cts-icon[name="copy"]')).toBeTruthy();

      // Inner role=button span — keyboard-activated by cts-badge.
      expect(clickTarget.getAttribute("role")).toBe("button");
      expect(clickTarget.getAttribute("aria-label")).toContain(REFERENCE_ID);

      await clickTarget.click();

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
      const clickTarget = await getClickTarget(canvasElement);

      // contextmenu dispatched on the inner span bubbles to the
      // cts-log-entry-id host, where _handleContextMenu calls
      // event.preventDefault().
      const ctx = new MouseEvent("contextmenu", { bubbles: true, cancelable: true });
      const ok = clickTarget.dispatchEvent(ctx);
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
 * After a successful copy the icon briefly flips to a checkmark via the
 * shared cts-copy-flash helper, then reverts to the original copy icon.
 * The animation is the canonical local-success affordance for every
 * "copy to clipboard" button in the suite.
 */
export const CopiedFeedback = {
  render: () =>
    html`<cts-log-entry-id reference-id=${REFERENCE_ID} test-id=${TEST_ID}></cts-log-entry-id>`,
  async play({ canvasElement }) {
    spyOn(navigator.clipboard, "writeText").mockResolvedValue();

    const clickTarget = await getClickTarget(canvasElement);
    const badge = canvasElement.querySelector('[data-testid="log-entry-id-chip"]');

    // Pre-click: copy icon is showing.
    expect(badge.querySelector('cts-icon[name="copy"]')).toBeTruthy();
    expect(badge.querySelector('cts-icon[name="check"]')).toBeNull();

    await clickTarget.click();

    // After a successful copy, the icon swaps to "check".
    await waitFor(() => {
      expect(badge.querySelector('cts-icon[name="check"]')).toBeTruthy();
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
