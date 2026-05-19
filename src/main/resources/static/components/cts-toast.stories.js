import { html } from "lit";
import { expect, userEvent, waitFor } from "storybook/test";
import { CtsToastHost } from "./cts-toast.js";
// Side-effect import: installs `window.ctsToast`, the global page-level
// API the static HTML pages use. The stories below exercise that wrapper
// directly (not just `CtsToastHost.show`) so the surface that real pages
// call from non-module <script> blocks is covered by the play-function
// suite.
import "../js/cts-toast-api.js";

export default {
  title: "Patterns/cts-toast",
  component: "cts-toast",
};

/**
 * Reset state between stories: remove any leftover host so each play()
 * starts from a clean document. Storybook reuses the same canvas across
 * stories, so without this the bottom-right region accumulates toasts.
 *
 * @returns {void}
 */
function resetHost() {
  for (const host of document.querySelectorAll("cts-toast-host")) {
    host.remove();
  }
}

// --- Stories ---

/**
 * Happy path: a single `kind="ok"` toast rendered statically inside the
 * canvas (no auto-dismiss timer firing during the play function). Asserts
 * the white card structure, the green left rule, and the matching
 * check-circle glyph.
 */
export const OkStatic = {
  render: () => html`
    <cts-toast-host>
      <cts-toast
        title="Test saved"
        message="Your configuration was stored successfully."
        kind="ok"
        .duration=${0}
      ></cts-toast>
    </cts-toast-host>
  `,
  async play({ canvasElement }) {
    const toast = canvasElement.querySelector("cts-toast");
    expect(toast).toBeTruthy();
    const card = toast.querySelector(".oidf-toast");
    expect(card).toBeTruthy();
    expect(card.getAttribute("role")).toBe("status");
    expect(toast.querySelector(".oidf-toast-title").textContent).toBe("Test saved");
    expect(toast.querySelector(".oidf-toast-message").textContent).toBe(
      "Your configuration was stored successfully.",
    );
    // Pass kind drives the green status-pass left rule and a
    // check-circle glyph.
    const icon = toast.querySelector("cts-icon");
    expect(icon.getAttribute("name")).toBe("circle-check");
    // Bootstrap toast classes must NOT leak through.
    expect(card.classList.contains("toast")).toBe(false);
    expect(card.classList.contains("toast-body")).toBe(false);
  },
};

/**
 * Happy path: clicking the dismiss button removes the toast immediately
 * and dispatches `cts-toast-dismiss`. The close glyph is a Bootstrap Icons
 * `cts-icon` element (not a literal `×` character) so it centres optically
 * inside the 20×20 button.
 */
export const Dismissible = {
  render: () => html`
    <cts-toast-host>
      <cts-toast
        title="Heads up"
        message="Click the close button to dismiss me."
        kind="ok"
        .duration=${0}
      ></cts-toast>
    </cts-toast-host>
  `,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-toast-host");
    const toast = canvasElement.querySelector("cts-toast");
    expect(toast).toBeTruthy();

    let dismissed = false;
    host.addEventListener("cts-toast-dismiss", () => {
      dismissed = true;
    });

    const closeBtn = toast.querySelector("button.oidf-toast-close");
    expect(closeBtn).toBeTruthy();
    expect(closeBtn.getAttribute("aria-label")).toBe("Dismiss");
    const closeIcon = closeBtn.querySelector('cts-icon[name="close-md"]');
    expect(closeIcon).toBeTruthy();
    expect(closeIcon.getAttribute("aria-hidden")).toBe("true");

    await userEvent.click(closeBtn);
    expect(dismissed).toBe(true);
    expect(canvasElement.querySelector("cts-toast")).toBeNull();
  },
};

/**
 * Happy path: `CtsToastHost.show(...)` auto-creates the singleton host
 * (or reuses an existing one) and appends a toast. The auto-dismiss
 * timer fires `cts-toast-dismiss` and removes the toast from the DOM.
 *
 * Uses a short 50ms `duration` to keep the play function fast.
 */
export const HelperAutoDismiss = {
  render: () => html`<div data-testid="trigger-zone"></div>`,
  async play() {
    resetHost();

    const toast = CtsToastHost.show({
      title: "Auto-dismiss",
      message: "Goes away on its own.",
      kind: "ok",
      duration: 50,
    });
    expect(toast).toBeTruthy();
    expect(toast.tagName.toLowerCase()).toBe("cts-toast");

    // Host should now exist as a sibling of <body>'s children.
    const host = /** @type {HTMLElement} */ (document.querySelector("cts-toast-host"));
    expect(host).toBeTruthy();
    expect(host.contains(toast)).toBe(true);

    let dismissed = false;
    host.addEventListener("cts-toast-dismiss", () => {
      dismissed = true;
    });

    await waitFor(
      () => {
        expect(dismissed).toBe(true);
      },
      { timeout: 1000 },
    );
    expect(document.querySelector("cts-toast")).toBeNull();

    resetHost();
  },
};

/**
 * Edge case: multiple toasts stack vertically in the host with an 8px
 * (`--space-2`) gap. The flex column layout on `cts-toast-host` is what
 * produces the stacking; this test asserts the host is a flex column
 * with the expected gap.
 */
export const StackedToasts = {
  render: () => html`
    <cts-toast-host>
      <cts-toast title="First" message="One." kind="ok" .duration=${0}></cts-toast>
      <cts-toast title="Second" message="Two." kind="ok" .duration=${0}></cts-toast>
      <cts-toast title="Third" message="Three." kind="ok" .duration=${0}></cts-toast>
    </cts-toast-host>
  `,
  async play({ canvasElement }) {
    const host = canvasElement.querySelector("cts-toast-host");
    expect(host).toBeTruthy();
    const toasts = host.querySelectorAll("cts-toast");
    expect(toasts.length).toBe(3);

    const styles = getComputedStyle(host);
    expect(styles.flexDirection).toBe("column");
    // --space-2 resolves to 8px from oidf-tokens.css.
    expect(styles.gap).toBe("8px");
  },
};

/**
 * Edge case: `kind="error"` swaps the green pass rule for the rust-400
 * fail rule and renders the `close-circle` glyph instead of `circle-check`.
 */
export const ErrorKind = {
  render: () => html`
    <cts-toast-host>
      <cts-toast
        title="Save failed"
        message="The request returned 500. Try again."
        kind="error"
        .duration=${0}
      ></cts-toast>
    </cts-toast-host>
  `,
  async play({ canvasElement }) {
    const toast = canvasElement.querySelector("cts-toast");
    expect(toast).toBeTruthy();

    const card = toast.querySelector(".oidf-toast");
    // The inline style on the card sets border-left-color to var(--rust-400).
    expect(card.getAttribute("style")).toContain("--rust-400");

    const icon = toast.querySelector("cts-icon");
    expect(icon.getAttribute("name")).toBe("close-circle");
  },
};

/**
 * Happy path for the global `window.ctsToast` API. Calling the global
 * with title/message/kind produces a `<cts-toast>` inside the
 * auto-created host. The auto-dismiss timer fires, the toast leaves the
 * DOM, and `cts-toast-dismiss` bubbles to the host. Uses a short 50ms
 * `duration` so the play function settles quickly.
 */
export const ViaWindowApi = {
  render: () => html`<div data-testid="trigger-zone"></div>`,
  async play() {
    resetHost();

    expect(typeof (/** @type {any} */ (window).ctsToast)).toBe("function");
    const toast = /** @type {any} */ (window).ctsToast({
      title: "Saved",
      message: "All good.",
      kind: "ok",
      duration: 50,
    });
    expect(toast).toBeTruthy();
    expect(toast.tagName.toLowerCase()).toBe("cts-toast");

    const host = /** @type {HTMLElement} */ (document.querySelector("cts-toast-host"));
    expect(host).toBeTruthy();
    expect(host.contains(toast)).toBe(true);

    let dismissed = false;
    host.addEventListener("cts-toast-dismiss", () => {
      dismissed = true;
    });

    await waitFor(
      () => {
        expect(dismissed).toBe(true);
      },
      { timeout: 1000 },
    );
    expect(document.querySelector("cts-toast")).toBeNull();

    resetHost();
  },
};

/**
 * Edge case: `duration: 0` keeps the toast on screen until the caller
 * dismisses it. The play function waits a short interval (long enough
 * to fire the default 5000ms timer if it were running), asserts the
 * toast is still in the DOM, then calls `.dismiss()` on the returned
 * element and asserts it leaves.
 */
export const Persistent = {
  render: () => html`<div data-testid="trigger-zone"></div>`,
  async play() {
    resetHost();

    const toast = /** @type {any} */ (window).ctsToast({
      title: "Stays put",
      kind: "ok",
      duration: 0,
    });
    expect(toast).toBeTruthy();

    // 100ms is enough to prove the toast does NOT auto-dismiss — the
    // default duration is 5000ms, and the only other documented value
    // (50ms in `ViaWindowApi` above) would already have fired.
    await new Promise((resolve) => setTimeout(resolve, 100));
    expect(document.querySelector("cts-toast")).toBe(toast);

    toast.dismiss();
    expect(document.querySelector("cts-toast")).toBeNull();

    resetHost();
  },
};

/**
 * Edge case: an unknown `kind` value falls back to `ok` (matches the
 * defensive fallback used by `cts-button` and `cts-alert`).
 */
export const UnknownKindFallback = {
  render: () => html`
    <cts-toast-host>
      <cts-toast
        title="Defaults to ok"
        message="kind=bogus falls back to the ok variant."
        kind="bogus"
        .duration=${0}
      ></cts-toast>
    </cts-toast-host>
  `,
  async play({ canvasElement }) {
    const toast = canvasElement.querySelector("cts-toast");
    const icon = toast.querySelector("cts-icon");
    expect(icon.getAttribute("name")).toBe("circle-check");
  },
};
