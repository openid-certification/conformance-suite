const FINDING_RESULTS = new Set(["FAILURE", "WARNING", "REVIEW", "SKIPPED", "INTERRUPTED"]);

/**
 * Select log entries that should appear in cts-failure-summary. If a concrete
 * finding exists, omit runner-level INTERRUPTED entries because they only
 * explain the stop condition and can duplicate the actionable failure.
 *
 * @param {Array<any> | null | undefined} entries
 * @returns {Array<any>}
 */
export function selectFailureSummaryFindings(entries) {
  if (!Array.isArray(entries)) return [];
  const findings = entries.filter((entry) => entry && FINDING_RESULTS.has(entry.result));
  return findings.some((entry) => entry.result !== "INTERRUPTED")
    ? findings.filter((entry) => entry.result !== "INTERRUPTED")
    : findings;
}
