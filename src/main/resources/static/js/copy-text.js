// Shared "copy this string to the clipboard" helper for the suite's copy
// affordances: the Clipboard API where it is available, the legacy
// execCommand shim otherwise. Pair a successful copy with
// `flashCopyConfirmed` from ./cts-copy-flash.js for the visual confirmation.

/**
 * Best-effort fallback when `navigator.clipboard.writeText` rejects (HTTP
 * pages, missing user-gesture context, older browsers). Mirrors the
 * `document.execCommand('copy')` shim widely used pre-Clipboard-API. Returns
 * `true` on apparent success, `false` otherwise — the caller decides how to
 * surface the result.
 * @param {string} value - Text to write to the clipboard.
 * @returns {boolean} `true` if the legacy copy command reported success.
 */
function fallbackCopy(value) {
  if (typeof document === "undefined") return false;
  const ta = document.createElement("textarea");
  ta.value = value;
  ta.setAttribute("readonly", "");
  ta.style.position = "fixed";
  ta.style.opacity = "0";
  ta.style.pointerEvents = "none";
  document.body.appendChild(ta);
  ta.select();
  let ok;
  try {
    // The Clipboard API requires HTTPS + a user gesture; on legacy / HTTP
    // contexts execCommand is the documented fallback. The deprecation
    // warning is acknowledged but the shim has no replacement on those
    // platforms.
    ok = /** @type {any} */ (document).execCommand("copy");
  } catch {
    ok = false;
  }
  document.body.removeChild(ta);
  return Boolean(ok);
}

/**
 * @param {string} value - Text to write to the clipboard.
 * @returns {Promise<boolean>} Resolves `true` when the copy succeeded via either the Clipboard API or the legacy fallback.
 */
export async function copyText(value) {
  if (
    typeof navigator !== "undefined" &&
    navigator.clipboard &&
    typeof navigator.clipboard.writeText === "function"
  ) {
    try {
      await navigator.clipboard.writeText(value);
      return true;
    } catch {
      return fallbackCopy(value);
    }
  }
  return fallbackCopy(value);
}
