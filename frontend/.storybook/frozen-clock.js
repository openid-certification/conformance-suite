// Freeze the calendar clock for every Storybook context (dev server, built
// Storybook / Chromatic capture, and the vitest runner) so stories render
// deterministically. Story fixtures compute timestamps from `Date.now()` at
// module load (e.g. `const NOW = Date.now()` in stories/fixtures/*), and
// components render relative time against the live clock (cts-time's
// formatRelative, cts-running-test-card). Without a frozen clock every
// Chromatic build captures different timestamp text and flags spurious
// visual changes.
//
// This module must be imported FIRST in .storybook/preview.js: ES module
// imports execute in order, and story modules load lazily after the preview
// annotations evaluate, so the patch lands before any fixture reads the
// clock.
//
// Only `Date` is patched. Timers (setTimeout/setInterval) are untouched, so
// polling stories, mock-fetch delays, and waitFor() behave exactly as in
// production. `performance.now()` also keeps ticking.

const FROZEN_NOW = new Date("2026-06-01T12:00:00.000Z").getTime();

const RealDate = Date;

class FrozenDate extends RealDate {
  constructor(...args) {
    // Zero args means "now" — return the frozen instant. Every other
    // signature (epoch ms, ISO string, date parts) passes through, including
    // `new Date(undefined)` which must stay an Invalid Date.
    if (args.length === 0) {
      super(FROZEN_NOW);
    } else {
      super(...args);
    }
  }

  static now() {
    return FROZEN_NOW;
  }
}

globalThis.Date = FrozenDate;

export {};
