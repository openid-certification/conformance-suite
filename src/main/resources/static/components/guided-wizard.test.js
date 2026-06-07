import { describe, it, expect } from "vitest";
import { resolveMode } from "./guided-wizard.js";

/**
 * Table tests for the R9 mode-resolution ladder:
 * edit-plan > edit-test > configJson > test_plan > recovery record >
 * wizard_preset > stored preference > guided default.
 */
describe("resolveMode", () => {
  /** @type {Array<[string, {search: string, storedMode?: string|null, hasRecoveryRecord?: boolean}, {mode: string, source: string}]>} */
  const table = [
    [
      "no params + no storage → guided default",
      { search: "" },
      { mode: "guided", source: "default" },
    ],
    [
      "stored advanced → advanced",
      { search: "", storedMode: "advanced" },
      { mode: "advanced", source: "preference" },
    ],
    [
      "stored guided → guided",
      { search: "", storedMode: "guided" },
      { mode: "guided", source: "preference" },
    ],
    [
      "?test_plan= forces advanced over stored guided",
      { search: "?test_plan=some-plan", storedMode: "guided" },
      { mode: "advanced", source: "test_plan" },
    ],
    [
      "?wizard_preset= forces guided over stored advanced",
      { search: "?wizard_preset=abc", storedMode: "advanced" },
      { mode: "guided", source: "wizard_preset" },
    ],
    [
      "?wizard_preset= + ?edit-test= → advanced (edit-test outranks)",
      { search: "?wizard_preset=abc&edit-test=t123" },
      { mode: "advanced", source: "edit-test" },
    ],
    [
      "recovery record + no params → guided at recovery slot",
      { search: "", hasRecoveryRecord: true },
      { mode: "guided", source: "recovery" },
    ],
    [
      "recovery record outranks wizard_preset",
      { search: "?wizard_preset=abc", hasRecoveryRecord: true },
      { mode: "guided", source: "recovery" },
    ],
    [
      "?test_plan= outranks the recovery record",
      { search: "?test_plan=some-plan", hasRecoveryRecord: true },
      { mode: "advanced", source: "test_plan" },
    ],
    [
      "?configJson= → advanced",
      { search: "?configJson=%7B%7D" },
      { mode: "advanced", source: "configJson" },
    ],
    [
      "?edit-plan= → advanced",
      { search: "?edit-plan=p1" },
      { mode: "advanced", source: "edit-plan" },
    ],
    [
      "edit-plan outranks edit-test",
      { search: "?edit-plan=p1&edit-test=t1" },
      { mode: "advanced", source: "edit-plan" },
    ],
    [
      "empty-string params count as absent (matches the page's truthiness checks)",
      {
        search: "?edit-plan=&edit-test=&configJson=&test_plan=&wizard_preset=",
        storedMode: "advanced",
      },
      { mode: "advanced", source: "preference" },
    ],
    [
      "unknown stored value falls through to the guided default",
      { search: "", storedMode: "bananas" },
      { mode: "guided", source: "default" },
    ],
  ];

  it.each(table)("%s", (_name, input, expected) => {
    const decision = resolveMode({
      params: new URLSearchParams(input.search),
      storedMode: input.storedMode ?? null,
      hasRecoveryRecord: input.hasRecoveryRecord ?? false,
    });
    expect(decision).toEqual(expected);
  });
});
