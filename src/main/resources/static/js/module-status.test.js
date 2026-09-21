import { describe, expect, it } from "vitest";
import {
  segmentStatusWord,
  segmentVariant,
  statusBadgeVariant,
  statusLabel,
} from "./module-status.js";

describe("statusBadgeVariant", () => {
  it("maps a SKIPPED verdict to the distinct skip variant", () => {
    expect(statusBadgeVariant("FINISHED", "SKIPPED")).toBe("skip");
  });

  it("maps never-run / verdict-less states to neutral, not skip", () => {
    expect(statusBadgeVariant(null, null)).toBe("neutral");
    expect(statusBadgeVariant(undefined, undefined)).toBe("neutral");
    expect(statusBadgeVariant("WAITING", null)).toBe("neutral");
    expect(statusBadgeVariant("INTERRUPTED", null)).toBe("neutral");
    expect(statusBadgeVariant("FINISHED", "UNKNOWN")).toBe("neutral");
  });

  it("lets a settled verdict win over the lifecycle status", () => {
    expect(statusBadgeVariant("INTERRUPTED", "FAILED")).toBe("fail");
    expect(statusBadgeVariant("RUNNING", null)).toBe("running");
  });

  it("does not let an interim WARNING or REVIEW pass off a stopped test as a verdict", () => {
    // WARNING and REVIEW are written mid-run; a test stopped before completion has no verdict.
    expect(statusBadgeVariant("INTERRUPTED", "WARNING")).toBe("neutral");
    expect(statusBadgeVariant("INTERRUPTED", "REVIEW")).toBe("neutral");
    expect(statusBadgeVariant("FINISHED", "WARNING")).toBe("warn");
    expect(statusBadgeVariant("FINISHED", "REVIEW")).toBe("review");
  });
});

describe("statusLabel", () => {
  it("labels a stopped test with an interim WARNING or REVIEW as INTERRUPTED", () => {
    expect(statusLabel("INTERRUPTED", "WARNING")).toBe("INTERRUPTED");
    expect(statusLabel("INTERRUPTED", "REVIEW")).toBe("INTERRUPTED");
    expect(statusLabel("INTERRUPTED", "FAILED")).toBe("FAILED");
    expect(statusLabel("FINISHED", "WARNING")).toBe("WARNING");
  });
});

describe("segmentVariant", () => {
  it("is neutral for a never-run module", () => {
    expect(segmentVariant({})).toBe("neutral");
    expect(segmentVariant({ instances: [] })).toBe("neutral");
  });

  it("is pending while the status fetch is outstanding", () => {
    expect(segmentVariant({ instances: ["a"] })).toBe("pending");
  });

  it("is skip only for a resolved SKIPPED verdict", () => {
    expect(
      segmentVariant({
        instances: ["a"],
        _statusResolved: true,
        status: "FINISHED",
        result: "SKIPPED",
      }),
    ).toBe("skip");
    expect(segmentVariant({ instances: ["a"], _statusResolved: true })).toBe("neutral");
  });
});

describe("segmentStatusWord", () => {
  it("says 'skipped' for a SKIPPED verdict, not 'no result'", () => {
    expect(
      segmentStatusWord({
        instances: ["a"],
        _statusResolved: true,
        status: "FINISHED",
        result: "SKIPPED",
      }),
    ).toBe("skipped");
  });

  it("distinguishes never-run from pending", () => {
    expect(segmentStatusWord({})).toBe("not run");
    expect(segmentStatusWord({ instances: ["a"] })).toBe("checking status");
  });

  it("follows the module's real status label once resolved", () => {
    const resolved = (status, result) => ({
      instances: ["a"],
      _statusResolved: true,
      status,
      result,
    });
    expect(segmentStatusWord(resolved("FINISHED", "PASSED"))).toBe("passed");
    expect(segmentStatusWord(resolved("INTERRUPTED", "FAILED"))).toBe("failed");
    expect(segmentStatusWord(resolved("RUNNING", null))).toBe("running");
    expect(segmentStatusWord(resolved("WAITING", null))).toBe("waiting");
    expect(segmentStatusWord(resolved("INTERRUPTED", null))).toBe("interrupted");
    expect(segmentStatusWord(resolved("INTERRUPTED", "WARNING"))).toBe("interrupted");
    // A 404'd fetch settles with no status at all: it reads as not run.
    expect(segmentStatusWord(resolved(undefined, undefined))).toBe("not run");
    expect(statusLabel(undefined, undefined)).toBe("PENDING");
  });
});
