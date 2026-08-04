import { describe, it, expect } from "vitest";
import { selectFindings } from "./log-findings.js";

/**
 * `selectFindings` is the single predicate behind every "Findings" surface on
 * log-detail (the hero list, the page-level summary below the sticky bar, and
 * the wide-viewport TOC rail). It used to exist as two byte-identical copies
 * that both omitted REVIEW — GitLab #1866.
 */
describe("selectFindings", () => {
  const entry = (result) => ({ _id: `e-${result}`, result, src: "Cond", msg: "m" });

  it("keeps every actionable per-condition result", () => {
    const entries = ["FAILURE", "WARNING", "REVIEW", "SKIPPED", "INTERRUPTED"].map(entry);
    expect(selectFindings(entries).map((e) => e.result)).toEqual([
      "FAILURE",
      "WARNING",
      "REVIEW",
      "SKIPPED",
      "INTERRUPTED",
    ]);
  });

  // The #1866 regression: a REVIEW-only test yielded an empty list, so the
  // hero fell through to its empty-state placeholder over a full log.
  it("keeps REVIEW entries", () => {
    const entries = [entry("SUCCESS"), entry("REVIEW"), entry("INFO")];
    expect(selectFindings(entries)).toEqual([entry("REVIEW")]);
  });

  it("drops the two non-actionable results", () => {
    expect(selectFindings([entry("SUCCESS"), entry("INFO")])).toEqual([]);
  });

  it("preserves stream order", () => {
    const entries = [entry("WARNING"), entry("SUCCESS"), entry("FAILURE")];
    expect(selectFindings(entries).map((e) => e.result)).toEqual(["WARNING", "FAILURE"]);
  });

  it("is idempotent, so a pre-filtered list can be passed through again", () => {
    const entries = [entry("SUCCESS"), entry("FAILURE"), entry("REVIEW")];
    const once = selectFindings(entries);
    expect(selectFindings(once)).toEqual(once);
  });

  it("treats a missing or unknown result as not-a-finding", () => {
    expect(selectFindings([{ _id: "x" }, entry("UNKNOWN"), entry(null)])).toEqual([]);
  });

  // Callers hand it `testInfo.results`, which /api/info never serializes, so
  // undefined is the normal production input — it must not throw.
  it("returns an empty list for non-array input", () => {
    expect(selectFindings(undefined)).toEqual([]);
    expect(selectFindings(null)).toEqual([]);
    expect(selectFindings({})).toEqual([]);
  });

  it("skips null holes without throwing", () => {
    expect(selectFindings([null, entry("FAILURE"), undefined])).toEqual([entry("FAILURE")]);
  });
});
