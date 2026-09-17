/**
 * The arithmetic behind `<cts-heatmap>`: the top of its color scale, the
 * intensity of one cell against it, the steps its legend shows and the total
 * its caption reports. Pure functions over a grid of counts, kept out of the
 * component so they can be unit tested without a DOM.
 * @module heatmap-model
 */

/**
 * @param {unknown} value - A grid or a row that may be missing from a truncated payload.
 * @returns {Array<any>} The array, or an empty one.
 */
function rows(value) {
  return Array.isArray(value) ? value : [];
}

/**
 * @param {Array<number>|undefined} values - One row of counts.
 * @returns {number} Their total, or 0 for a missing row.
 */
function rowTotal(values) {
  let total = 0;
  for (const value of rows(values)) total += Number(value) || 0;
  return total;
}

/**
 * The busiest cell in a 2-D grid — the top of the heatmap's color scale.
 * @param {Array<Array<number>>} values - Rows of counts.
 * @returns {number} The largest value, or 0 for an empty or all-zero grid.
 */
export function heatmapMax(values) {
  let max = 0;
  for (const row of rows(values)) {
    for (const value of rows(row)) {
      const n = Number(value) || 0;
      if (n > max) max = n;
    }
  }
  return max;
}

/**
 * Every cell added up — the caption's "N runs in this range".
 * @param {Array<Array<number>>} values - Rows of counts.
 * @returns {number} The sum.
 */
export function heatmapTotal(values) {
  let sum = 0;
  for (const row of rows(values)) sum += rowTotal(row);
  return sum;
}

/** The palest a non-zero cell may be: below this it is indistinguishable
 *  from an empty one, which would hide a quiet hour rather than show it. */
const HEATMAP_MIN_MIX = 10;

/**
 * Where one cell sits on the sequential ramp, as the percentage of
 * `--chart-cat-1` mixed into the surface.
 *
 * The scale is **square-root**, not linear. Suite activity is heavily skewed
 * — a weekday office hour runs an order of magnitude more tests than 03:00 on
 * a Sunday — and on a linear ramp everything but the peak collapses into the
 * same near-white, which is the one thing a heatmap must not do. A sqrt ramp
 * is still monotonic, so it never misstates which cell is busier; it only
 * spends more of the color range on the low end. Exact counts are in the
 * cell tooltip and the data table, so no comparison depends on judging a
 * fill.
 * @param {number} value - The cell's count.
 * @param {number} max - The busiest cell, from {@link heatmapMax}.
 * @returns {number} 0 for an empty cell, otherwise 10-100.
 */
export function heatmapIntensity(value, max) {
  const n = Number(value) || 0;
  const top = Number(max) || 0;
  if (n <= 0 || top <= 0) return 0;
  const share = Math.sqrt(Math.min(n, top) / top);
  return Math.round(HEATMAP_MIN_MIX + (100 - HEATMAP_MIN_MIX) * share);
}

/**
 * Where the legend samples the ramp: fractions of its LENGTH, low end first.
 * The top of the ramp is always sampled, so the darkest swatch is labeled
 * with the busiest cell.
 * @type {Array<number>}
 */
const HEATMAP_SCALE_POSITIONS = [0.25, 0.5, 0.75, 1];

/**
 * The legend's swatches: how dark each one is, and the value it stands for.
 *
 * This is the INVERSE of {@link heatmapIntensity}, and it exists because the
 * ramp is square-root scaled: five evenly spaced swatches labeled only
 * "0 … max" would read as linear and put the middle one at a quarter of the
 * value it actually means. A swatch a fraction `f` along the ramp stands for
 * `f² × max`, so the labels have to say so.
 *
 * The empty-cell swatch is not in here — it is not on the ramp at all (an
 * empty cell keeps the muted surface), so the component renders it itself.
 *
 * Two things happen to the labels on a SMALL scale, where `f² × max` rounds
 * away: a step never reads "0" (a swatch labeled 0 beside the muted 0 swatch
 * says the ramp starts at nothing, when in fact its palest step is one run),
 * so a non-zero step is clamped to 1; and steps that then say the same number
 * are collapsed onto the DARKEST of them, so a peak of 5 shows three swatches
 * rather than four, and the one labeled "1" is the shade a cell holding 1
 * actually gets. The exact inverse of {@link heatmapIntensity} therefore holds
 * only while the rounding is not clamped — with a peak of 1,600 it does; with
 * a peak of 5 the "1" swatch is one step darker than a real cell of 1.
 * @param {number} max - The busiest cell, from {@link heatmapMax}.
 * @returns {Array<{mix: number, value: number}>} Percentage of the hue to mix
 *   in, and the count it represents, palest first. Empty when there is no
 *   scale; never two steps with the same label.
 */
export function heatmapScaleSteps(max) {
  const top = Number(max) || 0;
  if (top <= 0) return [];
  /** @type {Array<{mix: number, value: number}>} */
  const steps = [];
  for (const position of HEATMAP_SCALE_POSITIONS) {
    const step = {
      mix: Math.round(HEATMAP_MIN_MIX + (100 - HEATMAP_MIN_MIX) * position),
      value: Math.max(1, Math.round(top * position * position)),
    };
    // The darkest of the equal steps wins, so the top of the ramp is always
    // sampled and every swatch is at least as dark as the cells it stands for.
    if (steps.length > 0 && steps[steps.length - 1].value === step.value) steps.pop();
    steps.push(step);
  }
  return steps;
}
