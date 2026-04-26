import { describe, it, expect } from "vitest";
import { SUMMARY_SPLIT_MARKER, splitTestSummary } from "./test-summary-split.js";

describe("splitTestSummary", () => {
  describe("input validation", () => {
    it.each([
      ["undefined", undefined],
      ["null", null],
      ["empty string", ""],
      ["non-string number", 0],
      ["non-string boolean", true],
      ["non-string object", { summary: "hi" }],
      ["non-string array", ["hi"]],
    ])("returns empty halves for %s", (_label, value) => {
      // Cast widens to `any` so the test can pass non-string values that
      // the runtime guard is meant to reject; the JSDoc-typed signature
      // would otherwise refuse them at type-check time.
      expect(splitTestSummary(/** @type {any} */ (value))).toEqual({
        description: "",
        instructions: "",
      });
    });
  });

  describe("without marker", () => {
    it("returns the full summary as the description, trimmed", () => {
      expect(splitTestSummary("  Plain summary text.  ")).toEqual({
        description: "Plain summary text.",
        instructions: "",
      });
    });

    it("returns empty halves when the input is whitespace only", () => {
      expect(splitTestSummary("   \n   \t   ")).toEqual({
        description: "",
        instructions: "",
      });
    });
  });

  describe("with marker", () => {
    it("splits a typical summary into description and instructions", () => {
      const summary =
        "This test verifies the OP returns a normal login page.\n\n---\n\nPlease remove cookies before proceeding.";
      expect(splitTestSummary(summary)).toEqual({
        description: "This test verifies the OP returns a normal login page.",
        instructions: "Please remove cookies before proceeding.",
      });
    });

    it("trims whitespace around each half", () => {
      const summary = "   Description.   \n\n---\n\n   Instructions.   ";
      expect(splitTestSummary(summary)).toEqual({
        description: "Description.",
        instructions: "Instructions.",
      });
    });

    it("returns empty description when only the instructions side is populated", () => {
      const summary = "\n\n---\n\nOnly the instructions.";
      expect(splitTestSummary(summary)).toEqual({
        description: "",
        instructions: "Only the instructions.",
      });
    });

    it("returns empty instructions when only the description side is populated", () => {
      const summary = "Only the description.\n\n---\n\n   ";
      expect(splitTestSummary(summary)).toEqual({
        description: "Only the description.",
        instructions: "",
      });
    });

    it("splits on the first marker and preserves later inline `---` blocks in the description side (when reachable)", () => {
      // First marker is the only split; trailing `---` line that follows a
      // marker lands inside the instructions zone and is preserved.
      const summary = "Description\n\n---\n\nInstructions step 1.\n\n---\n\nInstructions step 2.";
      const result = splitTestSummary(summary);
      expect(result.description).toBe("Description");
      expect(result.instructions).toBe("Instructions step 1.\n\n---\n\nInstructions step 2.");
    });

    it("treats marker-without-surrounding-blanks (e.g. `\\n---\\n`) as part of the description", () => {
      // The marker is exactly `\n\n---\n\n`; less padding does not match.
      const summary =
        "Description with inline\n---\nrule before any marker.\n\n---\n\nInstructions.";
      expect(splitTestSummary(summary)).toEqual({
        description: "Description with inline\n---\nrule before any marker.",
        instructions: "Instructions.",
      });
    });
  });

  describe("CRLF normalization (correctness-4)", () => {
    it("splits a CRLF-encoded summary the same way as an LF-encoded one", () => {
      const lf = "Description.\n\n---\n\nInstructions.";
      const crlf = lf.replace(/\n/g, "\r\n");
      expect(splitTestSummary(crlf)).toEqual(splitTestSummary(lf));
    });

    it("normalizes CRLF inside each half so renderers see consistent text", () => {
      const summary = "Line one.\r\nLine two.\r\n\r\n---\r\n\r\nDo this.\r\nThen this.";
      expect(splitTestSummary(summary)).toEqual({
        description: "Line one.\nLine two.",
        instructions: "Do this.\nThen this.",
      });
    });
  });

  describe("marker constant", () => {
    it("is the documented sentinel", () => {
      expect(SUMMARY_SPLIT_MARKER).toBe("\n\n---\n\n");
    });
  });
});
