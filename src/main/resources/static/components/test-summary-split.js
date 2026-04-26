/**
 * R24: split a `test.summary` string into a descriptive half ("About this
 * test") and an imperative half ("What you need to do").
 *
 * Test authors opt in by inserting the marker `\n\n---\n\n` (a Markdown
 * horizontal rule on its own line, with blank lines around it) inside
 * the `summary` argument of `@PublishTestModule`. CRLF inputs are
 * normalised to LF before searching, so summaries saved with Windows
 * line endings still split correctly. The first marker wins, so a
 * description containing inline `---` blocks survives intact.
 *
 * Both renderers consume this module:
 *   - the Lit component `cts-log-detail-header` imports `splitTestSummary`
 *   - the lodash Mustache template `templates/logHeader.html` reads
 *     `window.CTS_summarySplit.splitTestSummary` (set up at module load
 *     when running in a browser)
 *
 * Backend `@PublishTestModule.summary()` is unchanged; splitting is a
 * render-time concern only.
 *
 * Plan: docs/plans/2026-04-25-008-feat-r24-test-description-vs-instructions-plan.md
 * Origin: docs/brainstorms/2026-04-13-cts-ux-improvement-plan-requirements.md
 *   (R24, P1, P5)
 */

export const SUMMARY_SPLIT_MARKER = "\n\n---\n\n";

/**
 * @param {string|null|undefined} rawSummary - Raw `test.summary` value
 *   carried over from the backend; may contain the split marker.
 * @returns {{ description: string, instructions: string }} Trimmed
 *   halves; either may be the empty string when the input is falsy,
 *   not a string, or whitespace-only on that side of the marker.
 */
export function splitTestSummary(rawSummary) {
  if (typeof rawSummary !== "string" || rawSummary.length === 0) {
    return { description: "", instructions: "" };
  }
  const normalized = rawSummary.replace(/\r\n/g, "\n");
  const idx = normalized.indexOf(SUMMARY_SPLIT_MARKER);
  if (idx < 0) {
    return { description: normalized.trim(), instructions: "" };
  }
  return {
    description: normalized.slice(0, idx).trim(),
    instructions: normalized.slice(idx + SUMMARY_SPLIT_MARKER.length).trim(),
  };
}

if (typeof window !== "undefined") {
  window.CTS_summarySplit = { SUMMARY_SPLIT_MARKER, splitTestSummary };
}
