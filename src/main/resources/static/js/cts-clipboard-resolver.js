// Shared resolver used by the inline ClipboardJS handlers on
// log-detail.html, logs.html, and plans.html. Exposed as a global so the
// non-module inline `<script>` blocks on those pages can pick it up after
// `<script src="/js/cts-clipboard-resolver.js"></script>` loads.
//
// Why a separate file: the same six-line `text:` callback was duplicated
// in three pages after the cts-json-editor migration. Each page wraps a
// copy button against `.btn-clipboard > button`, points at a `cts-json-
// editor` (or `<input>`/`<textarea>`) via `data-clipboard-target`, and
// has to prefer `.value` over `textContent` because Monaco virtualises
// long content. Pulling the body into one place means the
// "prefer .value" rule lives in one place.
//
// The helper is intentionally not an ES module — the consumer pages load
// it via `<script src="...">` alongside ClipboardJS itself, which is also
// non-module. Switching either to module loading would re-order script
// execution relative to the inline blocks below them.
(function (window) {
  /**
   * Resolve the clipboard text for a button inside a `.btn-clipboard`
   * wrapper. Reads the wrapper's `data-clipboard-target` selector,
   * locates the target node, and prefers `.value` (works for cts-json-
   * editor, `<input>`, `<textarea>`) over `textContent` (only correct
   * for `<pre>`). Returns the empty string if either lookup fails so
   * ClipboardJS still triggers a `success` event with no surprise crash.
   * @param {Element} trigger - The `<button>` ClipboardJS dispatched on.
   * @returns {string} Text to copy to the clipboard.
   */
  function resolveClipboardJsonText(trigger) {
    var host = trigger.closest(".btn-clipboard");
    var sel = host && host.getAttribute("data-clipboard-target");
    var target = sel ? document.querySelector(sel) : null;
    if (!target) return "";
    if (typeof target.value === "string") return target.value;
    return target.textContent;
  }

  window.ctsResolveClipboardJsonText = resolveClipboardJsonText;
})(window);
