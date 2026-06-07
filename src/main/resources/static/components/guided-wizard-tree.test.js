import { describe, it, expect } from "vitest";
import { GUIDED_WIZARD_TREE } from "./guided-wizard-tree.js";

/**
 * Integrity tests for the guided-mode decision tree. The tree is hand-edited
 * data (converted from MR !2029's guided-wizard.yaml), so these tests are the
 * build-time guard against the class of breakage the YAML shipped with: a
 * dangling `also_required` reference (`fapi2_brazil_op`) that no sibling
 * choice declares. Any future content edit that reintroduces a dangling
 * reference, a choice with both/neither of `next`/`result`, or a result
 * without `plan_name`/`variants` fails here before it can dead-end a user.
 */

/**
 * Collects every step in the tree, including steps nested under choices via
 * `next`, with a human-readable path for failure messages.
 *
 * @returns {Array<{path: string, step: import("./guided-wizard-tree.js").WizardStep}>}
 */
function collectSteps() {
  /** @type {Array<{path: string, step: import("./guided-wizard-tree.js").WizardStep}>} */
  const collected = [];
  /**
   * @param {import("./guided-wizard-tree.js").WizardStep} step
   * @param {string} path
   */
  const visit = (step, path) => {
    collected.push({ path, step });
    for (const choice of step.choices ?? []) {
      if (choice.next) {
        visit(choice.next, `${path}/${choice.id}→${choice.next.id}`);
      }
    }
  };
  for (const ecosystem of GUIDED_WIZARD_TREE.ecosystems) {
    for (const step of ecosystem.steps) {
      visit(step, `${ecosystem.id}/${step.id}`);
    }
  }
  return collected;
}

describe("GUIDED_WIZARD_TREE shape", () => {
  it("has a non-empty ecosystem list", () => {
    expect(GUIDED_WIZARD_TREE.ecosystems.length).toBeGreaterThan(0);
  });

  it("gives every ecosystem an id, a label, and an entry step", () => {
    for (const ecosystem of GUIDED_WIZARD_TREE.ecosystems) {
      expect(ecosystem.id, "ecosystem id").toBeTruthy();
      expect(ecosystem.label, `label of ${ecosystem.id}`).toBeTruthy();
      expect(ecosystem.steps?.[0], `steps[0] of ${ecosystem.id}`).toBeTruthy();
    }
  });

  it("gives every step an id, a question, and at least one choice", () => {
    for (const { path, step } of collectSteps()) {
      expect(step.id, `id at ${path}`).toBeTruthy();
      expect(step.question, `question at ${path}`).toBeTruthy();
      expect(step.choices?.length, `choices at ${path}`).toBeGreaterThan(0);
    }
  });
});

describe("GUIDED_WIZARD_TREE integrity", () => {
  it("gives every choice exactly one of next/result", () => {
    for (const { path, step } of collectSteps()) {
      for (const choice of step.choices) {
        const branches = [choice.next, choice.result].filter(Boolean).length;
        expect(branches, `next/result count of ${path}/${choice.id}`).toBe(1);
      }
    }
  });

  it("gives every result a plan_name and variants", () => {
    for (const { path, step } of collectSteps()) {
      for (const choice of step.choices) {
        if (!choice.result) continue;
        expect(choice.result.plan_name, `plan_name of ${path}/${choice.id}`).toBeTruthy();
        expect(choice.result.variants, `variants of ${path}/${choice.id}`).toBeTruthy();
      }
    }
  });

  it("resolves every also_required id to a sibling choice with a result.plan_name", () => {
    // Guards against the dangling reference the YAML shipped with:
    // dcr_brazil_op.also_required pointed at `fapi2_brazil_op`, which exists
    // nowhere in the `plan` step. Reverting the data fix fails this test.
    for (const { path, step } of collectSteps()) {
      for (const choice of step.choices) {
        for (const sibling of choice.result?.also_required ?? []) {
          const target = step.choices.find((c) => c.id === sibling.id);
          expect(target, `also_required ${path}/${choice.id} → ${sibling.id}`).toBeTruthy();
          expect(
            target?.result?.plan_name,
            `result.plan_name of also_required target ${path}/${sibling.id}`,
          ).toBeTruthy();
        }
      }
    }
  });
});
