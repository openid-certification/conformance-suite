/**
 * DOM-level invariant assertions for the schedule-test.html baseline tests.
 *
 * These complement the snapshot diffs by catching failure classes that
 * snapshots cannot reliably detect:
 *
 *   - id collisions: snapshot diffs show the duplicated id but reviewers
 *     can miss them in a sea of innocuous changes; this asserts uniqueness
 *     directly.
 *   - label/input misassociation: `<label for="X">…<input id="Y">` produces
 *     the same aria-tree shape as no label at all (input shows `name: ""`),
 *     so aria snapshots cannot tell "missing label" apart from "wrong
 *     label". This walks the DOM and verifies the pairing directly.
 *
 * Both matter most for R42, which adds <label for> to ~134 dynamic config
 * fields — several keys (e.g., `jwks`) appear twice in the static markup,
 * so naive id generation could collide.
 */

import { expect } from "@playwright/test";

/**
 * Capture a Locator's innerHTML with non-deterministic markup normalized so
 * DOM snapshots are stable across runs.
 *
 * Two sources of run-to-run variance are scrubbed:
 *
 * 1. Lit per-template-instance markers — Lit emits comments like
 *    `<!--?lit$869712824$-->` to mark dynamic content positions. The numeric
 *    ID is generated fresh on each render. Digits are replaced with `NNNN`
 *    so the marker's structural position is preserved without the variance.
 *
 * 2. Monaco editor internals inside `<cts-json-editor>` — Monaco renders a
 *    deeply nested tree whose scrollbar visibility classes (`visible scrollbar
 *    horizontal` vs `invisible scrollbar horizontal fade`), inline `style`
 *    geometry, and per-instance ARIA wiring shift between renders. The
 *    baseline tests care that a `<cts-json-editor>` is at a given position
 *    with the expected attributes — not what Monaco draws inside it. The
 *    children of every `<cts-json-editor>` are replaced with a stable
 *    `<!--monaco-internals-->` placeholder.
 *
 * @param {import('@playwright/test').Locator} locator
 * @returns {Promise<string>}
 */
export async function getNormalizedInnerHTML(locator) {
  const html = await locator.innerHTML();
  return html
    .replace(/<!--\?lit\$\d+\$-->/g, "<!--?lit$NNNN$-->")
    .replace(
      /(<cts-json-editor\b[^>]*>)[\s\S]*?(<\/cts-json-editor>)/g,
      "$1<!--monaco-internals-->$2",
    );
}

/**
 * Assert that every element with an `id` attribute has a unique id.
 *
 * @param {import('@playwright/test').Page} page
 */
export async function assertNoIdCollisions(page) {
  const result = await page.evaluate(() => {
    const ids = [...document.querySelectorAll("[id]")].map((el) => el.id);
    const seen = new Set();
    const collisions = new Set();
    for (const id of ids) {
      if (seen.has(id)) collisions.add(id);
      else seen.add(id);
    }
    return { total: ids.length, collisions: [...collisions] };
  });
  expect(result.collisions, `Duplicate ids found in DOM: ${result.collisions.join(", ")}`).toEqual(
    [],
  );
}

/**
 * Assert that every form control inside `#scheduleTestPage` that declares
 * a label has its label text match the visually-adjacent `.col-md-2.key`
 * (or `.col-md-2.col-form-label`) text in the same row.
 *
 * Pre-R42 today: dynamic config inputs do not yet have <label for>, so
 * `input.labels` is empty for them and the assertion is vacuous. Post-R42:
 * every labeled input must pair correctly with its adjacent key text.
 * Either way, the assertion catches misassociation if/when it occurs.
 *
 * Form controls without an `.col-md-2.*` sibling are skipped (they live
 * outside the config-form key/value pattern this assertion targets).
 *
 * @param {import('@playwright/test').Page} page
 */
export async function assertLabelInputPairing(page) {
  const result = await page.evaluate(() => {
    const controls = [
      ...document.querySelectorAll(
        "#scheduleTestPage input, #scheduleTestPage select, #scheduleTestPage textarea",
      ),
    ];
    const mismatches = [];
    let labeledCount = 0;
    for (const ctrl of controls) {
      const labels = /** @type {HTMLInputElement} */ (ctrl).labels;
      if (!labels || labels.length === 0) continue;
      labeledCount++;
      const labelText = labels[0].textContent?.trim() ?? "";
      const row = ctrl.closest(".row, .mb-3");
      const keyEl = row?.querySelector(".col-md-2.key, .col-md-2.col-form-label");
      const keyText = keyEl?.textContent?.trim();
      if (keyText !== undefined && keyText !== labelText) {
        mismatches.push({
          controlId: ctrl.id || "(no id)",
          controlTag: ctrl.tagName,
          labelText,
          adjacentKeyText: keyText,
        });
      }
    }
    return { totalControls: controls.length, labeledCount, mismatches };
  });
  expect(
    result.mismatches,
    `Label/input misassociation in #scheduleTestPage:\n${JSON.stringify(result.mismatches, null, 2)}`,
  ).toEqual([]);
}
