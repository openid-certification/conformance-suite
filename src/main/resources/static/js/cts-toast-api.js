// Public, page-level toast API.
//
// `cts-toast-host` and `cts-toast` are the custom-element building blocks;
// this module is the thin global surface that page-level <script> blocks
// and non-component code use to fire a toast without knowing the host
// element exists. The component already exposes `CtsToastHost.show(opts)`
// as the canonical entry point — this wrapper:
//
//   1. Forces the side-effect import (registers the custom elements).
//   2. Installs `window.ctsToast(opts)` so non-module scripts can call it.
//   3. Returns the created <cts-toast> element. Callers that need to
//      dismiss before auto-dismiss fires call `.dismiss()` on the return.
//
// Pages should mount `<cts-toast-host></cts-toast-host>` at the end of
// `<body>` and import this module. If a page forgets, `CtsToastHost.show`
// auto-creates the host on first use, so the API never throws — but the
// explicit mount keeps the bottom-right region in the document's static
// layout where stylers and snapshot tests can see it.

import { CtsToastHost } from "../components/cts-toast.js";

/**
 * @typedef {object} ToastOptions
 * @property {string} [title] - Bold heading line.
 * @property {string} [message] - Optional secondary copy under the title.
 * @property {"ok"|"error"} [kind="ok"] - Visual variant. `error` swaps the
 *   green left rule and check glyph for rust + close-circle.
 * @property {number} [duration=5000] - Auto-dismiss delay in milliseconds.
 *   Pass `0` to keep the toast on screen until the user dismisses it or
 *   the caller invokes `.dismiss()` on the returned element.
 */

/**
 * Show a transient bottom-right toast notification.
 *
 * @param {ToastOptions} [options] - Toast configuration.
 * @returns {import("../components/cts-toast.js").CtsToast} The created toast
 *   element. Call `.dismiss()` on the returned element to remove the toast
 *   programmatically; the element also fires `cts-toast-dismiss` on both
 *   auto and manual dismissal.
 */
export function ctsToast(options) {
  return CtsToastHost.show(options);
}

if (typeof window !== "undefined") {
  /** @type {any} */ (window).ctsToast = ctsToast;
}
