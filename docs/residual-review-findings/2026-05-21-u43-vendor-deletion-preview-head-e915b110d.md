# Residual Review Findings — U43 vendor deletion + U43a Storybook preview-head

**Source PR run:** `ce-code-review mode:autofix`, run-id `20260521-012840-6246b520`
**Branch:** `feat/redesign`
**Tip after autofix + commit:** `e915b110d`
**Plan:** [docs/plans/2026-05-21-001-feat-u43-vendor-deletion-storybook-preview-head-plan.md](../plans/2026-05-21-001-feat-u43-vendor-deletion-storybook-preview-head-plan.md)
**Parent plan unit closures:** U43 + U43a in [docs/plans/2026-04-25-001-feat-oidf-design-system-bootstrap-removal-plan.md](../plans/2026-04-25-001-feat-oidf-design-system-bootstrap-removal-plan.md)
**Reviewers (6/6 returned):** correctness, testing, maintainability, project-standards, agent-native, learnings-researcher.
**Tracker:** Repo lives on GitLab. No GitHub Issues sink configured. These findings are inlined verbatim as the durable record.

## Verdict

**Ready to merge.** One `safe_auto` finding applied in commit `e915b110d` (delete `cleanupBootstrapModals` from `frontend/.storybook/preview.js` — became dead code as a direct consequence of U43a). Five residual items below need owner decisions before the broader Bootstrap-removal Phase E closure can be declared complete.

## Applied safe_auto fixes (in commit e915b110d)

1. **Delete `cleanupBootstrapModals` function and decorator from `frontend/.storybook/preview.js`** (cross-reviewer corroboration: correctness + maintainability, both anchor 90+). The function cleared `.modal-backdrop`, `body.modal-open`, and `body.overflow` between Storybook stories — all artifacts that Bootstrap JS produced. With Bootstrap JS gone in U43 and the preview-head script tag stripped in U43a, the function is provably unreachable; cts-modal uses native `<dialog>` and does not touch body styles. Verified by re-running `npm run test-storybook` (499/499 pass) and `playwright test e2e/login.spec.js` (8/8 pass, including the "Bootstrap CSS/JS assets are not requested by the page" assertion at line 72).

## Residual Actionable Work

### 1. [P2] Mustache templates still pull Bootstrap from cdn.jsdelivr.net

- **Files:** `src/main/resources/templates/error.html`, `src/main/resources/templates/implicitCallback.html`, `src/main/resources/templates/resultCaptured.html`
- **Reviewer:** correctness (anchor 75).
- **What's wrong:** Three Spring Boot–rendered Mustache views still load Bootstrap 5.3.3 + Popper from `cdn.jsdelivr.net`. Phase D retokenized the static HTML pages under `src/main/resources/static/`, but these server-rendered templates were out of scope. They survive U43 because they don't depend on the deleted `vendor/bootstrap/` directory — they fetch directly from CDN — but they keep Bootstrap as a runtime dependency for users hitting error / implicit-callback / result-captured flows.
- **Fix paths:**
  1. Retokenize the three Mustache templates against `oidf-tokens.css` + `oidf-app.css`, mirroring the Phase D treatment.
  2. Document the surviving Bootstrap CDN surface explicitly in `CLAUDE.md` (or the parent plan's Out of Scope section) so the next contributor knows it's deliberate.
- **Recommendation:** Path 1 in a separate plan after U44–U47 of the parent plan land. The three templates are low-traffic error pages, so the work is low-priority but non-zero.

### 2. [P3] No automated test pins `frontend/.storybook/preview-head.html` to per-page production HTML heads

- **Files:** `frontend/.storybook/preview-head.html`, `src/main/resources/static/index.html` (and siblings).
- **Reviewers:** correctness (anchor 75), learnings-researcher (corroborated via `docs/residual-review-findings/feat-redesign-8cf6356b3-inter-font.md` R-2 precedent).
- **What's wrong:** U43a's invariant — "Storybook preview mirrors production" — is enforced only by reviewer eyeballs. A future commit that adds a stylesheet to `index.html` without updating `preview-head.html` (or vice versa) silently breaks Storybook-vs-production parity. The Phase D + U43a divergence has been live for ~2 weeks before being caught.
- **Fix:** Tiny Playwright spec under `frontend/e2e/` that asserts the `<link rel="stylesheet">` / `<script src>` set in `preview-head.html` matches a representative production page (e.g., `index.html`). Adapted from the Inter-font residual's R-2 recommendation.

### 3. [P3] No CI grep guard prevents reintroducing `/vendor/bootstrap/`, `/vendor/popper/`, `/vendor/datatables/` references

- **Files:** `src/main/resources/static/**` (any future contributor adding markup that references these paths).
- **Reviewer:** correctness (anchor 75).
- **What's wrong:** The parent plan's R5 verification (no production HTML or component should reference the deleted vendor paths) is implementer-gated, not CI-enforced. A new HTML file that re-introduces `<link href="/vendor/bootstrap/...">` would not fail any existing CI check; it would just 404 at runtime.
- **Fix:** Small grep-based CI guard (a 5-line bash script in `.gitlab-ci/`) that fails if any tracked file under `src/main/resources/static/` matches `/vendor/(bootstrap|popper|datatables)/`. The `frontend/scripts/lint-icon-names.sh` is the analogous precedent.

### 4. [P3] Parent plan calls out `vendor/bootstrap-icons/` as preserved scope, but the directory was deleted weeks ago

- **Files:** `docs/plans/2026-04-25-001-feat-oidf-design-system-bootstrap-removal-plan.md` (lines 73, 89, 101, 234, 1571, 1578, 1598, 1708).
- **Reviewer:** maintainability (anchor 75).
- **What's wrong:** The parent plan still reads as if `vendor/bootstrap-icons/` is on disk, but commit `0935e37fc` deleted it. The current execution plan (`2026-05-21-001`) accommodated the discrepancy at runtime but the parent plan's verification text is now factually wrong.
- **Fix:** Append a dated reconciliation note to the parent plan U43 section, e.g., `**Update 2026-05-21:** vendor/bootstrap-icons was deleted earlier by commit 0935e37fc; only bootstrap, datatables, and popper remained for U43.` Either that or close the parent plan and treat `2026-05-21-001` as the canonical record going forward.

### 5. [P3] Capture this work with `/ce-compound` to seed `docs/solutions/`

- **Reviewer:** learnings-researcher (anchor 75).
- **What's wrong:** No existing `docs/solutions/` entry covers (a) Storybook preview-head parity with production, (b) the evidence-based CSS-reset-decision tree, or (c) the enumeration of what Bootstrap Reboot was carrying for this codebase. The Inter-font residual flagged the same kind of gap for self-hosted webfonts; the same gap exists for the Bootstrap-removal capstone.
- **Fix:** Run `/ce-compound` after Phase E lands. Captures the visual-diff workflow, the reset-decision tree, and the host-vs-inner discipline for any future framework removal.

## Pre-existing maintainability debt (not addressed; surfaced for tracking)

These items pre-date U43/U43a and document accumulated Bootstrap-removal debt the parent plan defers to U45 (fold layout.css residue) and U46 (docs refresh):

1. `src/main/resources/static/css/oidf-app.css:64–69` — `.btn, .btn:hover, .btn:focus` legacy compatibility selectors have no remaining production consumers; the comment on line 56 lists "Bootstrap `.btn` on `<a>`" as a consumer that no longer exists.
2. `src/main/resources/static/css/oidf-app.css:27–34` — `.collapse` comment claims "12+ markup sites" but only one remains (`templates/implicitCallback.html:35` — itself a Mustache template covered by R-1 above).
3. `src/main/resources/static/css/layout.css:1–9` — scrollbar-gutter rule comment justifies itself with non-existent Bootstrap modal behavior; rewrite to reference cts-modal / native `<dialog>` instead.
4. `src/main/resources/static/css/layout.css:77` — comment claims "the Bootstrap `.modal` wrapper itself is display:none" which is no longer true; cts-modal uses native `<dialog>`.
5. `src/main/resources/static/css/layout.css:655` — `.card` rule comment labels itself as a Bootstrap-card override, but it is now the sole provider for the single remaining consumer (`templates/error.html:27` — itself a Mustache template covered by R-1 above).
6. `src/main/resources/static/components/cts-modal.stories.js` — the Default and other stories render `<button class="oidf-btn-primary">` triggers without importing `./cts-button.js`. The `.oidf-btn` style block is injected by `cts-button.js` at module-load (STYLE_ID-gated), so without the import the button silently falls through to UA defaults in Storybook. Surfaced by the U4 visual diff. Explicitly out-of-scope per the current plan's findings.md.

## Verification artifacts

- **Visual diff (U4):** `tmp/screenshots/u43a-diff/` — six BEFORE/AFTER PNG pairs + `findings.md`. Two pairs SHA-256-identical; four had trivial UA-default shifts ≤4px (one was a 40px better-centered shift, one was the story-isolation gap captured as #6 above). No CSS reset rules needed.
- **Test gates (U6):** `npm run test:ci` exit 0 (0 errors, 64 pre-existing implicit-any warnings); `npm run test-storybook` 499/499 pass; `playwright test e2e/login.spec.js` 8/8 pass including the line-72 vendor-asset assertion.
- **Synthesis:** `/tmp/compound-engineering/ce-code-review/20260521-012840-6246b520/synthesis.md`.
