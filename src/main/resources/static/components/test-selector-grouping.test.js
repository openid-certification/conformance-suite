import { describe, it, expect } from "vitest";
import {
  compareVersions,
  entityLabel,
  groupPlans,
  isListedByDefault,
  rowName,
  sortFlat,
  statusBadges,
} from "./test-selector-grouping.js";

const VERIFIER = "Test a OpenID4VP Verifier";
const WALLET = "Test a OpenID4VP wallet";

/**
 * A minimal OID4VP plan.
 * @param {string} planName - Plan identifier.
 * @param {string} profile - Entity under test.
 * @param {string} specVersion - Spec version.
 * @param {string} shortName - Name within family, entity and version.
 * @param {boolean} certifiable - Part of the certification program.
 * @param {boolean} alpha - Alpha plan.
 * @returns {import("./test-selector-grouping.js").SelectorPlan} The plan.
 */
function vp(planName, profile, specVersion, shortName, certifiable, alpha) {
  return {
    planName,
    displayName: `OID4VP ${shortName}`,
    profile,
    specFamily: "OID4VP",
    specVersion,
    shortName,
    certifiable,
    alpha,
  };
}

const OID4VP = [
  vp("verifier-id2", VERIFIER, "ID2", "ID2", false, true),
  vp("wallet-haip", WALLET, "1.0 Final", "1.0 Final + HAIP", true, false),
  vp("verifier-final", VERIFIER, "1.0 Final", "1.0 Final", false, true),
  vp("verifier-id3", VERIFIER, "ID3", "ID3 (plus draft 24)", false, true),
  vp("verifier-haip", VERIFIER, "1.0 Final", "1.0 Final + HAIP", true, false),
];

/**
 * @param {import("./test-selector-grouping.js").PlanGrouping} grouping - Result of groupPlans.
 * @returns {Array<[string, string[]]>} Each group's label and plan names, for compact assertions.
 */
function shape(grouping) {
  return grouping.groups.map((g) => [g.label, g.plans.map((p) => p.planName)]);
}

describe("entityLabel", () => {
  it("relabels known profiles and merges both relying-party profiles", () => {
    expect(entityLabel(VERIFIER)).toBe("OpenID4VP Verifier");
    expect(entityLabel("Test a Relying Party / OAuth2 Client")).toBe("Relying Party / Client");
    expect(entityLabel("Test a Relying Party / OAuth2 Client Logout Support")).toBe(
      "Relying Party / Client",
    );
  });

  it("falls back to the raw profile, or empty when there is none", () => {
    expect(entityLabel("Test something new")).toBe("Test something new");
    expect(entityLabel(undefined)).toBe("");
  });
});

describe("compareVersions", () => {
  it("puts Final first, then newer drafts, then no version", () => {
    const versions = ["", "ID2", "1.0 Final", "ID3"];
    expect(versions.sort(compareVersions)).toEqual(["1.0 Final", "ID3", "ID2", ""]);
  });
});

describe("groupPlans", () => {
  it("groups a multi-entity family under sorted entity headings", () => {
    const grouping = groupPlans(OID4VP, OID4VP);
    expect(grouping.headed).toBe(true);
    expect(shape(grouping)).toEqual([
      ["OpenID4VP Verifier", ["verifier-haip", "verifier-final", "verifier-id3", "verifier-id2"]],
      ["OpenID4VP Wallet", ["wallet-haip"]],
    ]);
  });

  it("keeps headings when a search leaves only one entity visible", () => {
    const visible = OID4VP.filter((p) => p.profile === WALLET);
    const grouping = groupPlans(OID4VP, visible);
    expect(grouping.headed).toBe(true);
    expect(shape(grouping)).toEqual([["OpenID4VP Wallet", ["wallet-haip"]]]);
  });

  it("drops headings for a single-entity family", () => {
    const verifiers = OID4VP.filter((p) => p.profile === VERIFIER);
    const grouping = groupPlans(verifiers, verifiers);
    expect(grouping.headed).toBe(false);
    expect(grouping.groups).toHaveLength(1);
    expect(grouping.groups[0].label).toBe("");
  });

  it("returns no groups when nothing is visible", () => {
    expect(groupPlans(OID4VP, []).groups).toEqual([]);
  });

  it("does not reorder its input", () => {
    const input = OID4VP.slice();
    groupPlans(input, input);
    expect(input).toEqual(OID4VP);
  });

  it("orders by version before status", () => {
    const final = vp("final", VERIFIER, "Final", "Final", true, false);
    const finalAlpha = vp("final-alpha", VERIFIER, "Final", "Final + Brazil", true, true);
    const id2 = vp("id2", VERIFIER, "ID2", "ID2", true, false);
    const plans = [id2, finalAlpha, final];
    expect(shape(groupPlans(plans, plans))).toEqual([["", ["final", "final-alpha", "id2"]]]);
  });

  it("treats a plan without status fields as certifiable and not alpha", () => {
    const legacy = {
      planName: "legacy",
      displayName: "Zeta",
      profile: VERIFIER,
    };
    const plans = [legacy, vp("plain", VERIFIER, "", "Plain", false, false)];
    expect(shape(groupPlans(plans, plans))).toEqual([["", ["legacy", "plain"]]]);
  });
});

describe("statusBadges", () => {
  const CERTIFICATION = { variant: "pass", label: "Certification" };
  const ALPHA = { variant: "warn", label: "Alpha" };

  it("shows Certification for a certifiable plan and Alpha for an alpha one", () => {
    const badges = (certifiable, alpha) => statusBadges({ planName: "p", certifiable, alpha });
    expect(badges(true, false)).toEqual([CERTIFICATION]);
    expect(badges(true, true)).toEqual([CERTIFICATION, ALPHA]);
    expect(badges(false, true)).toEqual([ALPHA]);
  });

  it("shows nothing when the payload has no certification status", () => {
    expect(statusBadges({ planName: "p" })).toEqual([]);
  });
});

describe("isListedByDefault", () => {
  it("hides only plans outside the certification program", () => {
    expect(
      isListedByDefault({
        planName: "p",
        certifiable: true,
        alpha: true,
      }),
    ).toBe(true);
    expect(
      isListedByDefault({
        planName: "p",
        certifiable: false,
        alpha: true,
      }),
    ).toBe(false);
    expect(isListedByDefault({ planName: "p" })).toBe(true);
  });
});

describe("rowName", () => {
  it("prefers shortName, then displayName, then planName", () => {
    expect(rowName({ planName: "p", displayName: "D", shortName: "S" })).toBe("S");
    expect(rowName({ planName: "p", displayName: "D" })).toBe("D");
    expect(rowName({ planName: "p" })).toBe("p");
  });
});

describe("sortFlat", () => {
  it("sorts by family then display name, without mutating the input", () => {
    const plans = [
      { planName: "b", displayName: "Beta", specFamily: "OIDCC" },
      { planName: "c", displayName: "Alpha", specFamily: "OIDCC" },
      { planName: "a", displayName: "Zed", specFamily: "FAPI" },
    ];
    expect(sortFlat(plans).map((p) => p.planName)).toEqual(["a", "c", "b"]);
    expect(plans.map((p) => p.planName)).toEqual(["b", "c", "a"]);
  });
});
