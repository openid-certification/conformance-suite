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
 * Show a transient bottom-right toast notification. The option shape is
 * defined once on the component side -- see the `ToastOptions` typedef
 * inside `CtsToastHost.show`'s JSDoc in
 * `src/main/resources/static/components/cts-toast.js`. Keeping a single
 * canonical definition prevents the api wrapper and the component from
 * drifting when a property is added or its default changes.
 *
 * @param {import("../components/cts-toast.js").ToastOptions} [options]
 *   Toast configuration. See the component-side typedef for the full
 *   field list and defaults.
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
