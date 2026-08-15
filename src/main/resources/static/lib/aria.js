/**
 * Render a boolean as an ARIA state string. Returning the literal union
 * rather than `String(value)` keeps `lit-analyzer` happy about
 * `aria-pressed` / `aria-busy`, which only accept `"true"` / `"false"`.
 * @param {boolean} value - The state.
 * @returns {"true"|"false"} The attribute value.
 * @module lib/aria
 */
export function aria(value) {
  return value ? "true" : "false";
}
