/**
 * The one number formatter the statistics page and its primitives share, so
 * a figure reads the same in a tile, a table cell, a tooltip footer and a
 * heatmap legend.
 * @module number-format
 */

/** Grouped figures — 1,420 must not read as 1420. */
export const NUMBER_FORMAT = new Intl.NumberFormat();

/**
 * A figure off the payload, for display.
 * @param {unknown} value - A count, which a truncated payload may leave out.
 * @returns {string} The figure grouped; `"0"` for anything that is not a number.
 */
export function formatCount(value) {
  return NUMBER_FORMAT.format(Number(value) || 0);
}
