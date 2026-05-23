import { describe, it, expect, vi, afterEach } from "vitest";
import {
  formatRelative,
  formatAbsolute,
  formatTimeOfDay,
  formatCompact,
  formatAuto,
  toIso,
} from "./time-format.js";

// A fixed instant used for byte-for-byte locale comparisons. Tests assert
// against the same `Date` API the implementation uses rather than hardcoded
// locale strings, so they stay stable across the CI runner's locale/timezone.
const FIXED_ISO = "2026-05-22T09:42:13.482Z";

describe("time-format", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  describe("input validation (every helper, bad input)", () => {
    it.each([
      ["undefined", undefined],
      ["null", null],
      ["empty string", ""],
      ["unparseable string", "not-a-date"],
      ["NaN number", Number.NaN],
      ["Infinity", Number.POSITIVE_INFINITY],
      ["invalid Date", new Date("nonsense")],
    ])("returns '' for %s", (_label, value) => {
      expect(formatRelative(/** @type {any} */ (value))).toBe("");
      expect(formatAbsolute(/** @type {any} */ (value))).toBe("");
      expect(formatTimeOfDay(/** @type {any} */ (value))).toBe("");
      expect(formatCompact(/** @type {any} */ (value))).toBe("");
      expect(toIso(/** @type {any} */ (value))).toBe("");
      expect(formatAuto(/** @type {any} */ (value))).toEqual({
        display: "",
        absolute: "",
      });
    });

    it("does NOT return the raw input on parse failure (tightens legacy behavior)", () => {
      // The old cts-log-list helper returned the raw string when parsing
      // failed; this module deliberately returns "" instead.
      expect(formatRelative("garbage")).toBe("");
    });
  });

  describe("formatRelative", () => {
    it("returns the 'now' bucket for a timestamp seconds old", () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date("2026-05-22T09:42:18.000Z")); // 5s after FIXED_ISO
      const rtf = new Intl.RelativeTimeFormat(undefined, {
        numeric: "auto",
      });
      expect(formatRelative(FIXED_ISO)).toBe(rtf.format(-5, "second"));
    });

    it("returns the minute bucket ~5 minutes ago", () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date("2026-05-22T09:47:13.482Z")); // +5 min
      const rtf = new Intl.RelativeTimeFormat(undefined, {
        numeric: "auto",
      });
      expect(formatRelative(FIXED_ISO)).toBe(rtf.format(-5, "minute"));
    });

    it("rolls into the hour bucket beyond 60 minutes", () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date("2026-05-22T11:42:13.482Z")); // +2 h
      const rtf = new Intl.RelativeTimeFormat(undefined, {
        numeric: "auto",
      });
      expect(formatRelative(FIXED_ISO)).toBe(rtf.format(-2, "hour"));
    });

    it("rolls into the day bucket beyond 24 hours", () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date("2026-05-24T10:42:13.482Z")); // ~2 days
      const rtf = new Intl.RelativeTimeFormat(undefined, {
        numeric: "auto",
      });
      expect(formatRelative(FIXED_ISO)).toBe(rtf.format(-2, "day"));
    });

    it("falls back to the absolute string beyond 30 days", () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date("2026-07-15T09:42:13.482Z")); // > 30 days
      expect(formatRelative(FIXED_ISO)).toBe(formatAbsolute(FIXED_ISO));
    });

    it("clamps a future timestamp to 'now' (client-clock skew defense)", () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date("2026-05-22T09:42:00.000Z")); // before FIXED_ISO
      const rtf = new Intl.RelativeTimeFormat(undefined, {
        numeric: "auto",
      });
      expect(formatRelative(FIXED_ISO)).toBe(rtf.format(-0, "second"));
    });
  });

  describe("formatAbsolute", () => {
    it("matches new Date(iso).toLocaleString() byte-for-byte", () => {
      expect(formatAbsolute(FIXED_ISO)).toBe(new Date(FIXED_ISO).toLocaleString());
    });
  });

  describe("formatTimeOfDay", () => {
    it("matches new Date(iso).toLocaleTimeString() byte-for-byte", () => {
      expect(formatTimeOfDay(FIXED_ISO)).toBe(new Date(FIXED_ISO).toLocaleTimeString());
    });
  });

  describe("formatCompact", () => {
    it("matches the medium-date / short-time locale form byte-for-byte", () => {
      expect(formatCompact(FIXED_ISO)).toBe(
        new Date(FIXED_ISO).toLocaleString(undefined, {
          dateStyle: "medium",
          timeStyle: "short",
        }),
      );
    });
  });

  describe("toIso", () => {
    it("returns the canonical ISO 8601 string", () => {
      expect(toIso(FIXED_ISO)).toBe(new Date(FIXED_ISO).toISOString());
    });
  });

  describe("formatAuto", () => {
    it("returns { display, absolute } matching the individual helpers", () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date("2026-05-22T09:47:13.482Z")); // +5 min
      expect(formatAuto(FIXED_ISO)).toEqual({
        display: formatRelative(FIXED_ISO),
        absolute: formatAbsolute(FIXED_ISO),
      });
    });
  });

  describe("input type parity", () => {
    it("accepts Date, epoch-ms number, and ISO string identically", () => {
      const d = new Date(FIXED_ISO);
      const ms = d.getTime();
      expect(formatAbsolute(d)).toBe(formatAbsolute(FIXED_ISO));
      expect(formatAbsolute(ms)).toBe(formatAbsolute(FIXED_ISO));
      expect(formatTimeOfDay(d)).toBe(formatTimeOfDay(FIXED_ISO));
      expect(toIso(ms)).toBe(toIso(FIXED_ISO));
    });

    it("accepts a bare epoch-ms STRING (cts-time value attribute is a string)", () => {
      // Lit stringifies a numeric `value` binding, so log-entry epoch-ms
      // timestamps arrive as digit strings. They must parse as ms, not get
      // rejected by Date.parse.
      const msString = String(new Date(FIXED_ISO).getTime());
      expect(toIso(msString)).toBe(toIso(FIXED_ISO));
      expect(formatAbsolute(msString)).toBe(formatAbsolute(FIXED_ISO));
    });

    it("treats a short numeric string like '2026' as a calendar year, not epoch ms", () => {
      // 4-digit strings stay on the Date.parse path (year 2026), so the
      // epoch-ms shortcut does not hijack year-only inputs.
      expect(toIso("2026")).toBe(new Date("2026").toISOString());
    });
  });
});
