import { describe, it, expect } from "vitest";
import {
  resolveMode,
  replayAnswers,
  decodeWizardPreset,
  filterResolvableSiblings,
} from "./guided-wizard.js";
import { GUIDED_WIZARD_TREE } from "./guided-wizard-tree.js";

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

describe("replayAnswers", () => {
  it("replays a full valid trail to its leaf result", () => {
    const replay = replayAnswers("ksa", ["op", "pkjwt", "ksav2"]);
    expect(replay).not.toBeNull();
    expect(replay?.ecosystem.id).toBe("ksa");
    expect(replay?.path.map((a) => a.choice.id)).toEqual(["op", "pkjwt", "ksav2"]);
    expect(replay?.result?.plan_name).toBe("fapi2-message-signing-final-test-plan");
  });

  it("stops at the last valid step on the first unresolvable hop (R13)", () => {
    const replay = replayAnswers("ksa", ["op", "no-such-choice", "ksav2"]);
    expect(replay?.path.map((a) => a.choice.id)).toEqual(["op"]);
    expect(replay?.result).toBeNull();
  });

  it("returns null for an unknown ecosystem", () => {
    expect(replayAnswers("atlantis", ["op"])).toBeNull();
  });

  it("ignores trailing ids past a leaf", () => {
    const replay = replayAnswers("cbuae", ["op", "extra", "ids"]);
    expect(replay?.path.map((a) => a.choice.id)).toEqual(["op"]);
    expect(replay?.result?.plan_name).toBe("fapi2-message-signing-final-test-plan");
  });

  it("replays a partial trail to the next unanswered question", () => {
    const replay = replayAnswers("open_finance_brazil", ["op"]);
    expect(replay?.path.map((a) => a.choice.id)).toEqual(["op"]);
    expect(replay?.result).toBeNull();
  });
});

describe("decodeWizardPreset", () => {
  it("decodes a valid preset", () => {
    const raw = JSON.stringify({
      ecosystemId: "open_finance_brazil",
      answers: ["op"],
      completedPlanNames: ["fapi1-advanced-final-test-plan"],
    });
    expect(decodeWizardPreset(raw)).toEqual({
      ecosystemId: "open_finance_brazil",
      answers: ["op"],
      completedPlanNames: ["fapi1-advanced-final-test-plan"],
    });
  });

  it("defaults a missing ledger to an empty array", () => {
    const raw = JSON.stringify({ ecosystemId: "ksa", answers: [] });
    expect(decodeWizardPreset(raw)?.completedPlanNames).toEqual([]);
  });

  it("returns null on garbage, wrong shapes, and empty input", () => {
    expect(decodeWizardPreset("not-json{{{")).toBeNull();
    expect(decodeWizardPreset("")).toBeNull();
    expect(decodeWizardPreset(null)).toBeNull();
    expect(decodeWizardPreset(JSON.stringify("a string"))).toBeNull();
    expect(decodeWizardPreset(JSON.stringify({ answers: ["op"] }))).toBeNull();
    expect(decodeWizardPreset(JSON.stringify({ ecosystemId: "ksa", answers: "op" }))).toBeNull();
  });

  it("filters non-string entries from answers and the ledger", () => {
    const raw = JSON.stringify({
      ecosystemId: "ksa",
      answers: ["op", 7, null, "pkjwt"],
      completedPlanNames: [42, "real-plan"],
    });
    expect(decodeWizardPreset(raw)).toEqual({
      ecosystemId: "ksa",
      answers: ["op", "pkjwt"],
      completedPlanNames: ["real-plan"],
    });
  });
});

describe("filterResolvableSiblings", () => {
  const brazil = /** @type {import("./guided-wizard-tree.js").WizardEcosystem} */ (
    GUIDED_WIZARD_TREE.ecosystems.find((e) => e.id === "open_finance_brazil")
  );
  const fapiResult = {
    plan_name: "fapi1-advanced-final-test-plan",
    variants: {},
    also_required: [{ id: "dcr_brazil_op", label: "Dynamic Client Registration" }],
  };

  it("resolves a sibling present in both the tree and the catalog (R4)", () => {
    const catalog = { "fapi1-advanced-final-brazil-dcr-test-plan": {} };
    expect(filterResolvableSiblings(fapiResult, brazil, catalog)).toEqual([
      {
        id: "dcr_brazil_op",
        label: "Dynamic Client Registration",
        planName: "fapi1-advanced-final-brazil-dcr-test-plan",
      },
    ]);
  });

  it("filters a sibling whose plan is absent from the live catalog (R4)", () => {
    expect(filterResolvableSiblings(fapiResult, brazil, {})).toEqual([]);
  });

  it("filters a sibling id that resolves nowhere in the tree", () => {
    const result = { ...fapiResult, also_required: [{ id: "no-such-choice", label: "Ghost" }] };
    const catalog = { "fapi1-advanced-final-brazil-dcr-test-plan": {} };
    expect(filterResolvableSiblings(result, brazil, catalog)).toEqual([]);
  });

  it("returns empty for results without also_required", () => {
    expect(filterResolvableSiblings({ plan_name: "x", variants: {} }, brazil, {})).toEqual([]);
  });
});
