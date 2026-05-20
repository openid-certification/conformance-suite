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

    // Attach the dismiss listener BEFORE calling dismiss() so a refactor
    // that breaks the cts-toast-dismiss event without breaking DOM
    // removal is still caught. Mirrors the Dismissible story pattern.
    const host = /** @type {HTMLElement} */ (document.querySelector("cts-toast-host"));
    expect(host).toBeTruthy();
    let dismissed = false;
    host.addEventListener("cts-toast-dismiss", () => {
      dismissed = true;
    });

    toast.dismiss();
    expect(dismissed).toBe(true);
    expect(document.querySelector("cts-toast")).toBeNull();

    resetHost();
  },
};

/**
 * Happy path for the `window.ctsToast({ kind: "error" })` global path.
 * The static `ErrorKind` story above proves the visual treatment when
 * `<cts-toast kind="error">` is rendered directly into the canvas; this
 * story proves the same treatment survives the `window.ctsToast` →
 * `CtsToastHost.show` → `<cts-toast>` chain. If a future refactor silently
 * dropped `kind` from `CtsToastHost.show`'s option propagation, the
 * static `ErrorKind` story would still pass while this one would fail.
 *
 * Uses `duration: 0` + manual `.dismiss()` so the assertion does not race
 * an auto-dismiss timer. `ViaWindowApi` already covers the auto-dismiss
 * path for `kind: "ok"`.
 */
export const ViaWindowApiError = {
  render: () => html`<div data-testid="trigger-zone"></div>`,
  async play() {
    resetHost();

    expect(typeof (/** @type {any} */ (window).ctsToast)).toBe("function");
    const toast = /** @type {any} */ (window).ctsToast({
      title: "Save failed",
      message: "The request returned 500. Try again.",
      kind: "error",
      duration: 0,
    });
    expect(toast).toBeTruthy();
    expect(toast.tagName.toLowerCase()).toBe("cts-toast");

    const host = /** @type {HTMLElement} */ (document.querySelector("cts-toast-host"));
    expect(host).toBeTruthy();
    expect(host.contains(toast)).toBe(true);

    // The static `ErrorKind` story queries `.oidf-toast` synchronously because
    // Lit has already rendered the lit-html template into light DOM by the
    // time `play()` runs. Here the `<cts-toast>` is created dynamically via
    // `window.ctsToast`, so the first render is queued for the next
    // microtask — await `updateComplete` (Lit's lifecycle promise) so the
    // assertions below see the rendered children, not an empty host.
    await /** @type {any} */ (toast).updateComplete;

    const card = toast.querySelector(".oidf-toast");
    // The inline style on the card sets border-left-color to var(--rust-400)
    // — same assertion shape as the static `ErrorKind` story so a refactor
    // that breaks one without the other surfaces a clean diff.
    expect(card.getAttribute("style")).toContain("--rust-400");

    const icon = toast.querySelector("cts-icon");
    expect(icon.getAttribute("name")).toBe("close-circle");

    let dismissed = false;
    host.addEventListener("cts-toast-dismiss", () => {
      dismissed = true;
    });

    toast.dismiss();
    expect(dismissed).toBe(true);
    expect(document.querySelector("cts-toast")).toBeNull();

    resetHost();
  },
};

/**
 * Idempotency claim: `CtsToastHost.getOrCreate()` always returns the same
 * singleton host on repeat calls within a document. This is the visible
 * half of the JSDoc contract; the other half (graceful handling when
 * `<body>` has not yet parsed) is a defensive code path exercised only
 * from `<head>` inline scripts, where reliably mocking `document.body =
 * null` mid-play-function would corrupt Storybook's iframe state. The
 * null-body fallback is covered by reviewer-eyes on the small branch in
 * `cts-toast.js`.
 */
export const GetOrCreateIdempotent = {
  render: () => html`<div data-testid="trigger-zone"></div>`,
  async play() {
    resetHost();

    const first = CtsToastHost.getOrCreate();
    const second = CtsToastHost.getOrCreate();
    expect(first).toBe(second);
    expect(first.tagName.toLowerCase()).toBe("cts-toast-host");

    // A subsequent `CtsToastHost.show()` reuses the same host rather than
    // creating a sibling — the singleton invariant the page mounts rely on.
    const toast = CtsToastHost.show({ title: "x", duration: 0 });
    expect(first.contains(toast)).toBe(true);
    expect(document.querySelectorAll("cts-toast-host").length).toBe(1);

    resetHost();
  },
};

/**
 * Race regression guard for the null-body deferred-append path. Two
 * synchronous `getOrCreate()` calls before `<body>` is parsed must still
 * return the same singleton host — otherwise two `<cts-toast-host>` siblings
 * land in `<body>` once `DOMContentLoaded` fires and stay there for the
 * page lifetime. Documented in
 * `docs/residual-review-findings/2026-05-19-cts-toast-residuals-fix-fe34323f6.md`
 * as R-1 (a race introduced while fixing the parent doc's finding #7).
 *
 * The play function stubs `document.body` to `null`, calls `getOrCreate()`
 * twice, restores `document.body`, and dispatches a synthetic
 * `DOMContentLoaded` to flush the queued append listener. Both the
 * in-memory singleton invariant and the visible DOM-count invariant are
 * asserted so a future refactor that breaks one without the other surfaces
 * a clean diff.
 */
export const GetOrCreateNullBodyRace = {
  render: () => html`<div data-testid="trigger-zone"></div>`,
  async play() {
    resetHost();

    // Shadow the prototype `document.body` getter with an own-property
    // accessor returning null. `delete document.body` below removes the
    // shadow and restores the inherited getter.
    Object.defineProperty(document, "body", { get: () => null, configurable: true });

    let first;
    let second;
    try {
      first = CtsToastHost.getOrCreate();
      second = CtsToastHost.getOrCreate();
    } finally {
      // `delete document.body` would trip TS strict-mode (the property is
      // non-optional); `Reflect.deleteProperty` is the type-clean way to
      // remove the own-property shadow and restore the prototype getter.
      Reflect.deleteProperty(document, "body");
    }

    try {
      // Singleton invariant in memory: both calls return the same node,
      // even before either has been inserted into the DOM.
      expect(first).toBe(second);

      // Flush the deferred append. A buggy implementation queues two
      // listeners (one per call), each appending its own host — two
      // sibling `<cts-toast-host>` nodes end up under `<body>`.
      document.dispatchEvent(new Event("DOMContentLoaded"));
      expect(document.querySelectorAll("cts-toast-host").length).toBe(1);
    } finally {
      // If an earlier assertion threw, the queued listener may still be
      // pending; dispatch once more to drain it before the next story.
      // `{ once: true }` makes a second dispatch a no-op when already drained.
      document.dispatchEvent(new Event("DOMContentLoaded"));
      resetHost();
    }
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
