import { describe, it, expect } from "vitest";
import { classifyRuns, IN_PROGRESS_LOGS_QUERY, FAILING_LOGS_QUERY } from "./run-classification.js";

const status = (s) => ({ status: s });
const result = (r) => ({ result: r });

describe("run-classification", () => {
  describe("classifyRuns", () => {
    it.each([
      [
        "RUNNING and WAITING count as in progress; FINISHED/INTERRUPTED/CREATED do not",
        [
          status("RUNNING"),
          status("WAITING"),
          status("FINISHED"),
          status("INTERRUPTED"),
          status("CREATED"),
        ],
        { inProgressCount: 2, failingCount: 0 },
      ],
      [
        "FAILED and UNKNOWN count as failing; PASSED/WARNING do not",
        [result("FAILED"), result("UNKNOWN"), result("PASSED"), result("WARNING")],
        { inProgressCount: 0, failingCount: 2 },
      ],
      ["empty array yields zero counts", [], { inProgressCount: 0, failingCount: 0 }],
      [
        "lowercase enum values normalize correctly",
        [status("running"), status("waiting"), result("failed"), result("unknown")],
        { inProgressCount: 2, failingCount: 2 },
      ],
      [
        "INTERRUPTED and CREATED do NOT count as in progress (whitelist, not !FINISHED)",
        [status("INTERRUPTED"), status("CREATED"), status("NOT_YET_CREATED"), status("CONFIGURED")],
        { inProgressCount: 0, failingCount: 0 },
      ],
    ])("%s", (_label, logs, expected) => {
      expect(classifyRuns(logs)).toEqual(expected);
    });

    it("counts status and result independently on the same record", () => {
      // A running test has no terminal result yet, but the buckets are derived
      // from independent fields, so a single record can match both.
      expect(classifyRuns([{ status: "RUNNING", result: "FAILED" }])).toEqual({
        inProgressCount: 1,
        failingCount: 1,
      });
    });

    it("ignores records missing the relevant field, and non-array input", () => {
      expect(classifyRuns([{}, { status: null }, { result: undefined }])).toEqual({
        inProgressCount: 0,
        failingCount: 0,
      });
      expect(classifyRuns(/** @type {any} */ (null))).toEqual({
        inProgressCount: 0,
        failingCount: 0,
      });
      expect(classifyRuns(/** @type {any} */ (undefined))).toEqual({
        inProgressCount: 0,
        failingCount: 0,
      });
    });
  });

  describe("deep-link query constants", () => {
    it("expose the in-progress and failing logs filters", () => {
      expect(IN_PROGRESS_LOGS_QUERY).toBe("status=running,waiting");
      expect(FAILING_LOGS_QUERY).toBe("result=failed,unknown");
    });
  });
});
