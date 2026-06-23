/**
 * cts-test-selector interaction helpers for the schedule-test specs.
 *
 * cts-test-selector is the sole plan-entry point after cts-spec-cascade was
 * removed: there is no family → entity → version → plan cascade to walk, so a
 * plan is chosen by clicking its row in the search list. The picker's
 * `.is-active` row is the page's canonical "current plan" signal — the page's
 * cts-plan-selected listener mirrors the chosen plan back to
 * planSearch.selected on every path (user pick, deep-link, edit-plan,
 * load-last-config), so the highlighted row reflects the active plan no matter
 * how the selection was driven.
 */
import { expect } from "@playwright/test";

/**
 * The picker's currently-selected row (canonical current-plan signal). Resolves
 * to zero rows before any selection.
 *
 * @param {import('@playwright/test').Page} page
 * @returns {import('@playwright/test').Locator}
 */
export function selectedPlanRow(page) {
  return page.locator("#planSearch .oidf-test-selector__row.is-active");
}

/**
 * Select a plan through the search picker by clicking its row, then wait for
 * the picker to mark it active. Rows only render once the picker's loader
 * clears — which happens after the page wires the cts-plan-select handler — so
 * a click on a visible row is safe to drive selection.
 *
 * @param {import('@playwright/test').Page} page
 * @param {string} planName - The planName to pick (matches data-plan-name).
 */
export async function selectPlanViaSearch(page, planName) {
  const row = page.locator(`#planSearch [data-plan-name="${planName}"]`);
  await row.click();
  await expect(row).toHaveClass(/is-active/);
}
