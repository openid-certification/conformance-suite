import { describe, it, expect } from "vitest";
import { heatmapIntensity, heatmapMax, heatmapScaleSteps, heatmapTotal } from "./heatmap-model.js";

describe("heatmapMax / heatmapTotal", () => {
  const grid = [
    [0, 3, 9],
    [1, 0, 2],
  ];

  it("finds the busiest cell and the sum of every cell", () => {
    expect(heatmapMax(grid)).toBe(9);
    expect(heatmapTotal(grid)).toBe(15);
  });

  it("reports 0 for an empty or all-zero grid", () => {
    expect(heatmapMax([])).toBe(0);
    expect(heatmapMax([[0, 0]])).toBe(0);
    expect(heatmapTotal(/** @type {any} */ (undefined))).toBe(0);
  });

  it("tolerates a ragged or malformed grid", () => {
    expect(heatmapMax(/** @type {any} */ ([[1], null, [4, 2]]))).toBe(4);
  });
});

describe("heatmapIntensity", () => {
  it("is 0 only for an empty cell, so a quiet hour is still visible", () => {
    expect(heatmapIntensity(0, 100)).toBe(0);
    expect(heatmapIntensity(1, 100)).toBeGreaterThanOrEqual(10);
  });

  it("puts the busiest cell at the dark end", () => {
    expect(heatmapIntensity(100, 100)).toBe(100);
  });

  it("is square-root scaled, so a skewed grid keeps its low end apart", () => {
    // Linear would put a cell at 4% of the peak at 4% of the ramp —
    // indistinguishable from the surface. The sqrt ramp gives it a fifth of
    // the range.
    expect(heatmapIntensity(4, 100)).toBe(28);
    expect(heatmapIntensity(25, 100)).toBe(55);
  });

  it("never decreases as the value grows", () => {
    let previous = -1;
    for (let value = 0; value <= 200; value += 1) {
      const intensity = heatmapIntensity(value, 200);
      expect(intensity).toBeGreaterThanOrEqual(previous);
      previous = intensity;
    }
  });

  it("is 0 when there is no scale to be on", () => {
    expect(heatmapIntensity(5, 0)).toBe(0);
    expect(heatmapIntensity(/** @type {any} */ ("x"), 10)).toBe(0);
  });
});

describe("heatmapScaleSteps", () => {
  it("labels each swatch with the value it actually stands for", () => {
    // The inverse of the sqrt ramp: a swatch a quarter of the way along stands
    // for a SIXTEENTH of the peak, not a quarter of it. Labeling the ends
    // only would leave the reader assuming the latter.
    expect(heatmapScaleSteps(1600)).toEqual([
      { mix: 33, value: 100 },
      { mix: 55, value: 400 },
      { mix: 78, value: 900 },
      { mix: 100, value: 1600 },
    ]);
  });

  it("agrees with heatmapIntensity, which is what makes the legend honest", () => {
    for (const step of heatmapScaleSteps(1600)) {
      expect(heatmapIntensity(step.value, 1600)).toBe(step.mix);
    }
  });

  it("has no steps when there is no scale to show", () => {
    expect(heatmapScaleSteps(0)).toEqual([]);
    expect(heatmapScaleSteps(/** @type {any} */ (undefined))).toEqual([]);
  });

  it("never labels a swatch 0, because the legend already has a 0 swatch", () => {
    // f² × max rounds away on a small scale: a quarter of the way along a
    // ramp topping out at 7 is 0.44 runs. A step labeled "0" next to the
    // muted "no runs" swatch says the ramp starts at nothing.
    for (const max of [1, 2, 3, 5, 7, 8]) {
      for (const step of heatmapScaleSteps(max)) expect(step.value).toBeGreaterThanOrEqual(1);
    }
  });

  it("collapses steps that would carry the same label onto the darkest of them", () => {
    // One run is all there is: one swatch, and it is the color a cell with
    // one run actually gets.
    expect(heatmapScaleSteps(1)).toEqual([{ mix: 100, value: 1 }]);
    expect(heatmapScaleSteps(5)).toEqual([
      { mix: 55, value: 1 },
      { mix: 78, value: 3 },
      { mix: 100, value: 5 },
    ]);
    // Four distinct labels again from a peak of 7 upwards.
    expect(heatmapScaleSteps(7)).toEqual([
      { mix: 33, value: 1 },
      { mix: 55, value: 2 },
      { mix: 78, value: 4 },
      { mix: 100, value: 7 },
    ]);
    expect(heatmapScaleSteps(8)).toEqual([
      { mix: 33, value: 1 },
      { mix: 55, value: 2 },
      { mix: 78, value: 5 },
      { mix: 100, value: 8 },
    ]);
  });

  it("always samples the top of the ramp, and never paler than the cells it stands for", () => {
    for (const max of [1, 2, 5, 7, 8, 16, 1600]) {
      const steps = heatmapScaleSteps(max);
      const darkest = steps[steps.length - 1];
      const labels = steps.map((step) => step.value);
      expect(labels.at(-1)).toBe(max);
      expect(darkest.mix).toBe(100);
      // strictly increasing, so no two swatches say the same thing
      expect([...labels].sort((a, b) => a - b)).toEqual(labels);
      expect(new Set(labels).size).toBe(labels.length);
      // and the ramp itself still darkens step by step
      const mixes = steps.map((step) => step.mix);
      expect([...mixes].sort((a, b) => a - b)).toEqual(mixes);
      // The exact inverse of heatmapIntensity only survives while the labels
      // are not rounded: on a small scale a swatch can be a step off the shade
      // a cell of that count gets, which is why the label carries the count.
      expect(heatmapIntensity(darkest.value, max)).toBe(darkest.mix);
    }
  });
});
