---
name: fix-logs-owner-cell-and-icon-guardrails
description: Repair the logs.html owner cell and active-filter chip after the bi-* → cts-icon migration, and add a durable, progressively-discoverable icon-name guardrail so future contributors (human or agent) cannot ship invalid `cts-icon name=""` values without a clear failure signal at PR time.
status: active
created: 2026-05-19
type: fix
depth: standard
---

# Fix logs owner cell + add icon-name guardrails

## Problem Frame

Three follow-ups on `https://localhost.emobix.co.uk:8443/logs.html`, plus one cross-cutting infrastructure improvement that addresses the root cause they share.

1. **Owner cell visual is broken.** The Owner column's two-tone pill (purple "subject" half + blue "issuer" half with small user/globe glyphs) was collapsed in 2018 (`3a6603a6d`) so that long DN/URI values fit in a compact data-table cell with full values exposed only on hover. The April 2026 coolicons migration (`0dd20bd84`) regressed the markup: the outer `.log-owner` wrapper was rewritten into a `cts-icon` element, and the inner `.ownerSub` / `.ownerIss` pills were stripped of the icon classes that gave them visual content. Today the markup is malformed (closing `</cts-icon>` appears before the inner `</span>` closes) so the browser auto-corrects in a way that produces:
   - the user-01 icon **outside** the colored pills (it became the wrapper),
   - an empty `.ownerSub` pill with only a `title=""` tooltip on an element that has no text and inconsistent hover targets,
   - a globe pill whose `title=""` lives on the `cts-icon` host element rather than the pill itself, so the tooltip is inconsistent across browsers,
   - and an inline layout that wraps onto two lines in narrow cells because nothing scopes the wrap behavior.

2. **Globe icon's purpose reads as cryptic.** The visual semantic was "user-icon = subject, globe-icon = issuer" with the actual values disclosed on hover. After the migration the tooltip is effectively orphaned and the globe sits next to an empty colored bar, so a user looking at the cell has no affordance telling them what either glyph represents.

3. **Active-filter chip's close (x) icon doesn't render.** `src/main/resources/static/logs.html:234` calls `trailIcon.setAttribute("name", "x")` — but `vendor/coolicons/icons/x.svg` does not exist. The coolicons set ships `close-md.svg`, `close-sm.svg`, `close-lg.svg`. The migration commit (`0dd20bd84`) explicitly mapped `x → close-md`, but the regression slipped back in when the active-filter chip was added later. CI passed because nothing validates that a literal `cts-icon name="…"` resolves to a vendored SVG.

4. **Root cause of (3) — and risk for every future call site.** The fact that an agent could ship `name="x"` proves the discoverability + enforcement gap: CLAUDE.md *describes* where to look (Storybook AllIcons, `ls vendor/coolicons/icons/`) but no automated check fails the build when a literal name is invalid, and no runtime warning fires either. The set has 442 vendored icons, which means listing them in CLAUDE.md would bloat every agent's context window — the user explicitly called out that the fix must use **progressive disclosure**.

## Scope

In-scope:
- Repair the active-filter chip's close icon on `logs.html`.
- Repair the owner-cell markup in `templates/owner.html` and its layout in `css/layout.css` so the icons sit visually inside the colored pills, hover tooltips work, and the cell never wraps.
- Restore an affordance that tells the reader the user and globe glyphs represent subject and issuer respectively (visible-on-hover tooltip + `aria-label`).
- Add a `lint:icons` script wired into `npm run test:ci` that fails the build whenever a literal `cts-icon name="…"` attribute references an SVG that is not vendored.
- Add a runtime dev-time warning to `cts-icon.js` so dynamic / template-string `name=` values that 404 surface a console warning instead of silently rendering empty.
- Tighten `CLAUDE.md`'s icon section to make the discoverability path explicit and short, without listing the 442 names inline.
- Tighten the `AllIcons` Storybook play function to fetch each `<use href>` URL and assert it returns 200, so the smoke test catches new broken names if they're added to the curated list.

### Deferred to Follow-Up Work
- Regenerating the `VENDORED_ICON_NAMES` array in `cts-icon.stories.js` from disk via a codegen step (the manual curated list is fine as a discoverability surface; the new CI lint catches misuse anywhere, not just in stories).
- Pruning unused vendored icons. Out of scope; we're solving the *guardrail*, not the inventory.
- Redesigning the Owner cell visually (replacing the two-tone pill with a different affordance). The current design is restored, not re-imagined.

### Out of scope
- Backend changes — frontend-only per `feedback_minimal_backend_touching` memory.
- Bootstrap-icons remnants — already removed in `0dd20bd84`.
- Mobile / responsive redesign of the logs table.

## Requirements

- **R1.** `logs.html` active-filter chip renders a visible close glyph (sourced from `vendor/coolicons/icons/close-md.svg`) and remains keyboard-clearable.
- **R2.** The Owner cell in `logs.html`'s `Owner` column renders as a single-line, two-tone pill: subject icon inside the `.ownerSub` half, globe inside the `.ownerIss` half. Long `sub` / `iss` values do not cause row wrap.
- **R3.** Hovering either half shows the full subject or issuer string. Keyboard focus on either half exposes the same information via accessible name.
- **R4.** `npm run test:ci` fails when a literal `cts-icon name="<name>"` references an SVG that is not under `src/main/resources/static/vendor/coolicons/icons/`. The failure message names the file, the literal name, and at least one likely correct alternative when there's an obvious near-match (e.g. `x` → suggest `close-md`).
- **R5.** A `cts-icon` whose name resolves to a 404 at runtime emits exactly one console warning per unique name per page-load, with the offending name and a pointer to the AllIcons Storybook page. Production builds must not regress on this — the warning is gated by `console` availability, not by an env flag, so it surfaces in dev DevTools without changing production rendering.
- **R6.** `CLAUDE.md`'s icon section names the canonical discovery path (`ls` + AllIcons) in ≤ 3 lines, calls out that there is now a CI lint and a runtime warning, and explicitly tells contributors "don't paste the list anywhere; the CI catches typos."

## High-Level Technical Design

### Owner cell — markup recovery

The pre-coolicons template was:

```html
<span class="log-owner">
  <span class="ownerSub bi bi-person-fill" title="{{ owner.sub }}"></span>
  <span class="ownerIss bi bi-globe-americas" title="{{ owner.iss }}"></span>
</span>
```

The repair restores the wrapper-then-pills-then-icons hierarchy, putting `cts-icon` *inside* each pill rather than letting it become the pill:

```html
<span class="log-owner">
  <span class="ownerSub" title="{{ owner.sub }}" aria-label="Subject: {{ owner.sub }}">
    <cts-icon name="user-01" size="16" aria-hidden="true"></cts-icon>
  </span>
  <span class="ownerIss" title="{{ owner.iss }}" aria-label="Issuer: {{ owner.iss }}">
    <cts-icon name="globe" size="16" aria-hidden="true"></cts-icon>
  </span>
</span>
```

This is directional — the implementing agent should treat it as the intended shape, not as the exact tokens to commit. The accessible-name strategy (`aria-label` containing the same text the tooltip shows) is what restores R3; the `title` attribute alone is not announced consistently to assistive tech.

### Layout — anti-wrap

`.log-owner` becomes a `display: inline-flex` row with `align-items: stretch` and `flex-wrap: nowrap`. The `.ownerSub` / `.ownerIss` pills become `display: inline-flex; align-items: center;` so the cts-icon inside each one is vertically centered against the pill's padding box (the existing `padding: 2px 10px` is preserved). This is the "display: contents for behavior-only wrappers" alignment pattern memorialized in `feedback_display_contents_for_wrapper_custom_elements` — except here we keep the pills as visible containers (they carry the background colors), so we go with `inline-flex` not `display: contents`.

### Icon name guardrail — three layers

| Layer | Catches | Cost | Runs when |
|---|---|---|---|
| `lint:icons` bash script in `frontend/scripts/` | Literal `cts-icon name="…"` typos that reference a non-vendored SVG | One grep + one `test -f` per match | Every PR (via `npm run test:ci`) |
| `cts-icon` runtime warning | Dynamic / template-string `name=` values that 404 (`name="${tile.icon}"` etc.) | One `fetch` HEAD per icon load, scoped to dev DevTools console | Page load in any environment |
| `CLAUDE.md` pointer | Authoring-time confusion | Three lines | Every new conversation |

This is the progressive-disclosure shape the user asked for: the 442-name list lives on disk, the lookup recipe lives in `CLAUDE.md` (≤ 3 lines), and the *enforcement* lives in CI / runtime — agents don't need the list in context to ship correct code.

The lint script uses POSIX tools so it works on macOS, Linux CI, and inside the Nix devenv. Pseudocode (directional, not implementation):

```bash
# For each cts-icon name="<literal>" usage outside vendor/ and stories' test fixtures,
# assert the corresponding .svg exists. Skip dynamic names (${…}). Print a "did you
# mean close-md?" hint when the literal matches a known regression pattern (x, cross,
# close).
```

The runtime warning hooks the `<use>` element's `error` event. Modern browsers fire `onerror` on `<use>` when the referenced fragment fails to resolve (404 on the SVG file, or `#i` missing). One warning per unique `name` per session, cached on a module-level `Set`.

## Output Structure

No new directories. Files added:

```
frontend/scripts/lint-icon-names.sh   ← new
```

Files modified:

```
src/main/resources/static/logs.html
src/main/resources/static/templates/owner.html
src/main/resources/static/css/layout.css
src/main/resources/static/components/cts-icon.js
src/main/resources/static/components/cts-icon.stories.js
frontend/package.json
frontend/e2e/logs.spec.js
CLAUDE.md
```

## Key Technical Decisions

- **Restore the 2018 design rather than redesign the cell.** The original "two-tone pill with hover tooltip" works; the issue is regression, not affordance. Avoids scope creep and avoids re-litigating UX choices Justin Richer landed eight years ago.
- **`inline-flex` on `.log-owner`, not `display: contents`.** The pills carry visible background colors, so the wrapper needs to be a real box-model participant. `display: contents` is reserved for behavior-only wrappers (per `feedback_display_contents_for_wrapper_custom_elements`).
- **CI gate is a bash script, not an ESLint rule.** The existing `lint:jsdoc` pattern is bash, the existing `codegen.sh` is bash, and the lint target is `cts-icon name="…"` attributes in plain HTML — not JSX, not template literals where ESLint adds value. Bash is the right grain.
- **Lint script handles dynamic names by ignoring them.** Catching `name="${this.icon}"` requires either runtime instrumentation (we already have that) or a constant-folder for Lit templates (out of scope). Two-layer defense is cheaper than over-engineering one layer.
- **AllIcons play function upgraded to fetch each URL.** Currently it only checks the URL *shape*. The cheaper improvement is to actually `fetch(url, { method: 'HEAD' })` and assert 200 OK. This makes the curated list self-policing without needing codegen.
- **Don't list icons in CLAUDE.md.** Explicitly per user constraint. The CLAUDE.md note says "find names via `ls` or Storybook; CI fails on typos" and stops there.

## Implementation Units

### U1. Fix active-filter chip close icon

**Goal:** The active-filter chip on `logs.html` renders a visible close-md glyph instead of an empty span.

**Requirements:** R1

**Dependencies:** none

**Files:**
- `src/main/resources/static/logs.html` (modify line ~234)
- `frontend/e2e/logs.spec.js` (add assertion)

**Approach:** Change `trailIcon.setAttribute("name", "x")` to `trailIcon.setAttribute("name", "close-md")`. Add a more descriptive `aria-label` on the chip (the existing one is fine; we don't need to touch it) but the inner cts-icon stays `aria-hidden="true"` because the surrounding button already labels itself.

**Patterns to follow:** Other call sites in the codebase use `close-md` for in-pill dismiss affordances — see `cts-data-table.js` filter-reset pill (referenced in the `logs.html` comments).

**Test scenarios:**
- Visiting `logs.html?status=FINISHED` renders an `.oidf-page-filter-chip` whose trailing `cts-icon` has `name="close-md"` and whose rendered `<use href>` ends in `/vendor/coolicons/icons/close-md.svg#i`.
- Clicking the chip navigates to `logs.html` (filter cleared) — preserve existing behavior; this is a regression-guard, not a new behavior.

**Verification:** Load `logs.html?status=FINISHED` in a browser; the chip displays "Status: FINISHED" with a visible × on the right. Playwright e2e (`logs.spec.js`) passes the new assertion.

---

### U2. Repair owner cell markup, layout, and accessible names

**Goal:** Restore the two-tone owner pill: user glyph inside `.ownerSub`, globe glyph inside `.ownerIss`, single line, hover/focus reveals subject + issuer.

**Requirements:** R2, R3

**Dependencies:** none (independent of U1)

**Files:**
- `src/main/resources/static/templates/owner.html` (rewrite)
- `src/main/resources/static/css/layout.css` (extend `.ownerSub` / `.ownerIss` rules; add `.log-owner` rule)
- `frontend/e2e/logs.spec.js` (add owner-cell rendering assertion under an admin-mocked fixture)

**Approach:** Replace the malformed markup with the wrapper-then-pill-then-icon hierarchy described in the High-Level Technical Design section. Add `aria-label="Subject: <value>"` and `aria-label="Issuer: <value>"` so screen readers announce meaning, not just "image." Add a `.log-owner { display: inline-flex; align-items: stretch; flex-wrap: nowrap; }` rule and convert `.ownerSub` / `.ownerIss` to `display: inline-flex; align-items: center;` so the inner cts-icon is vertically centered without depending on parent line-height. Verify that the underscore.js template (`<%- %>`) still escapes the `aria-label` interpolation — `<%- %>` is HTML-escaping in lodash templates, so this is safe.

**Patterns to follow:**
- The 2018 `3a6603a6d` commit shape (glyphicon era) for the pill structure intent.
- `cts-badge` interactive ring (inset box-shadow, no border) for the visual cohesion language (informational reference; we're not adding a badge).

**Test scenarios:**
- E2E: visiting `logs.html` with an admin user fixture that returns at least one row with `owner: { sub: 'alice@example.com', iss: 'https://issuer.example' }` renders an `.ownerSub` element containing one `cts-icon[name="user-01"]` whose parent has `aria-label="Subject: alice@example.com"` and `title="alice@example.com"`.
- E2E: the same row renders `.ownerIss` with one `cts-icon[name="globe"]` and `aria-label="Issuer: https://issuer.example"`.
- E2E: shrink the viewport / cell width so the pills would historically wrap, assert that `getBoundingClientRect()` of `.log-owner` reports a single line height (≤ 32px including padding).
- Playwright assertion: `.ownerSub > cts-icon` exists (not `cts-icon > .ownerSub` — that's the bug shape, and we want to fail loudly if it ever returns).

**Verification:** Visual review on `logs.html` with the dev profile + a logged-in admin user (memory says dev profile injects `DummyUserFilter` as admin) shows the two-tone pill restored; hover reveals tooltips; `.log-owner` never wraps.

---

### U3. Add icon-name CI lint

**Goal:** `npm run test:ci` fails when any literal `cts-icon name="…"` references an SVG not under `src/main/resources/static/vendor/coolicons/icons/`.

**Requirements:** R4

**Dependencies:** none (independent of U1, U2 — but the new gate must pass once those land, so this unit's PR-time validation is the regression guard for them)

**Files:**
- `frontend/scripts/lint-icon-names.sh` (new)
- `frontend/package.json` (add `lint:icons` script; insert into `test:ci` chain after `lint:lit-analyzer` and before `codegen:check`)

**Approach:** Bash script that:
1. `find`s every literal `cts-icon name="<name>"` occurrence under `src/main/resources/static/` (HTML + JS components + templates), excluding the vendor directory and excluding Storybook docs that intentionally show invalid names (none currently exist).
2. Skips matches where the value is a `${…}` template expression or contains a `<%- %>` lodash interpolation — these are dynamic; the runtime warning catches them.
3. For each remaining literal `<name>`, checks `test -f "src/main/resources/static/vendor/coolicons/icons/<name>.svg"`.
4. On any miss, prints `file:line: cts-icon name="<name>" — no such file at src/main/resources/static/vendor/coolicons/icons/<name>.svg`, and exits non-zero.
5. When the literal is exactly `x`, `cross`, or `close`, append `(did you mean close-md, close-sm, or close-lg?)` to the error.

**Patterns to follow:**
- `frontend/scripts/lint-jsdoc-properties.sh` for the bash + grep + exit-code shape.
- `frontend/scripts/codegen.sh` for the macOS-vs-Linux portability conventions.

**Test scenarios:**
- Run the script against current `feat/redesign` HEAD (after U1 and U2 have landed): exits 0.
- Manually plant a `<cts-icon name="totally-not-an-icon"></cts-icon>` in a scratch HTML file under `src/main/resources/static/`, re-run: exits non-zero with the offending file:line, the bad name, and (if the planted name is `x`) the `close-md` hint.
- Run on a file that contains only dynamic names (`name="${foo}"`): exits 0 — dynamic names are not flagged.

**Verification:** Add and commit the script + package.json change; run `npm run test:ci` from `frontend/` — passes. Run with a deliberate bad name — fails with the right message. Revert the deliberate bad name.

---

### U4. Runtime warning on missing-icon load

**Goal:** A `cts-icon` whose resolved SVG URL 404s emits exactly one console warning per unique `name` per page-load.

**Requirements:** R5

**Dependencies:** none

**Files:**
- `src/main/resources/static/components/cts-icon.js` (modify)
- `src/main/resources/static/components/cts-icon.stories.js` (add a story / play function that asserts the warning fires for an invalid name and does not fire for a valid one)

**Approach:** After the SVG is rendered in `render()`, attach a one-time `error` listener to the inner `<use>` element via Lit's `firstUpdated` lifecycle (or a render-completion microtask if `firstUpdated` isn't already in use — it isn't; the component is currently render-only). The listener:
1. Reads `this.name`.
2. Checks a module-level `WeakSet`-or-`Set` of already-warned names (a plain `Set<string>` is fine; names are strings).
3. If new, calls `console.warn` with the message: `[cts-icon] No vendored SVG found for name="${name}". See Storybook → Primitives/cts-icon → AllIcons, or run: ls src/main/resources/static/vendor/coolicons/icons/`.

`<use>` element's `error` event fires reliably in current Chromium/WebKit/Gecko for missing-fragment / missing-file cases. The memory note `feedback_browser_support_policy` confirms we target only modern browsers, so no polyfill.

**Patterns to follow:**
- The component already imports `LitElement, html, nothing` and the size-validation method `_resolvedSize()` is a precedent for defensive runtime checks. Place the warning logic in a sibling method.

**Test scenarios:**
- Storybook story `MissingIconWarns`: renders `<cts-icon name="totally-not-an-icon"></cts-icon>` and a stub for `console.warn`. Play function: `await new Promise(r => setTimeout(r, 50))` (wait for the `<use>` error event), then assert the stub was called exactly once with a string containing `totally-not-an-icon`.
- Storybook story `ValidIconNoWarning`: renders `<cts-icon name="close-md"></cts-icon>`. Play function: assert the stub was NOT called.
- Storybook story `WarnsOncePerName`: renders three copies of the same invalid name. Play function: assert the stub was called exactly once (de-dup by name).

**Verification:** Run `npm run test-storybook` (just the cts-icon project) — all three new stories pass. Manually load `logs.html` with a deliberate bad name and verify DevTools console shows the warning once.

---

### U5. Tighten AllIcons Storybook play function

**Goal:** The `AllIcons` story's play function actually fetches each `<use href>` and asserts 200, so the curated catalog can't silently drift into pointing at a missing file.

**Requirements:** R4 (complementary: U3 catches *any* invalid literal; U5 catches drift in the curated story specifically)

**Dependencies:** U4 is independent but lands first because the warning helps debug a failing fetch.

**Files:**
- `src/main/resources/static/components/cts-icon.stories.js` (modify the `AllIcons.play` function)

**Approach:** Replace the existing assertion that only checks URL shape with `await Promise.all(figures.map(async fig => { const use = …; const url = use.getAttribute('href').replace(/#i$/, ''); const res = await fetch(url, { method: 'HEAD' }); expect(res.ok).toBe(true); }))`. Storybook's vitest runner already runs against the live Vite dev server, so the fetch resolves against the same `/vendor/coolicons/icons/` path the real app uses.

**Patterns to follow:**
- The existing play function shape in `AllIcons` (querySelectorAll → expect).
- The Lit-marker normalization caveat is irrelevant here (we're not snapshotting DOM); but the `feedback_vitest_browser_stale_cache` memory says: if this play function ever flakes, restart the storybook test runner before debugging.

**Test scenarios:**
- Run `npm run test-storybook -- --testNamePattern='AllIcons'` on the current curated list — passes (all 48 currently-listed icons resolve).
- Manually inject a fake icon name into the array, re-run — fails with a clear "expected true to be true" pointing at the bad URL.

**Verification:** Storybook smoke test passes; manual breakage is caught.

---

### U6. CLAUDE.md progressive-disclosure pointer

**Goal:** Contributors (humans + agents) discover valid icon names via a 3-line pointer in CLAUDE.md, not by carrying the 442-name list in context.

**Requirements:** R6

**Dependencies:** U3 + U4 must land first (CLAUDE.md will reference both as the enforcement layers).

**Files:**
- `CLAUDE.md` (modify the "Icons" section)
- `src/main/resources/static/vendor/coolicons/README.md` (optional: cross-link to the CI lint script)

**Approach:** Replace the current Icons section's discoverability paragraph with three short bullets:
1. **Find a name:** `ls src/main/resources/static/vendor/coolicons/icons/` or browse Storybook → Primitives/cts-icon → AllIcons.
2. **Don't guess:** `npm run test:ci` runs `lint:icons` which fails the PR when a literal `cts-icon name="…"` references a non-vendored file. The error message names a likely correction.
3. **Don't paste the full list anywhere:** the 442-name catalog stays on disk so it doesn't bloat agent context windows. Discover names on demand; the CI catches typos.

Keep the existing "Adding a new icon" pointer to the vendor README unchanged. Keep the brand-glyph carve-out paragraph unchanged.

**Patterns to follow:**
- The existing CLAUDE.md tone — terse, oriented toward stopping bad patterns.
- The existing "Frontend quality gates" section's compact reference style.

**Test scenarios:**
- No automated tests — this is docs. Manual check: read CLAUDE.md top-to-bottom and verify the Icons section reads as standalone guidance, no inline list of 442 names, and references the new `lint:icons` gate by name.
- *Test expectation: none — documentation-only change.*

**Verification:** `git diff CLAUDE.md` review; visually scan that the section is short and points at three concrete affordances (ls, Storybook, CI lint).

---

## Sequencing

U1, U2, U3, U4, U5 can each be implemented in isolation. U6 lands last because it references the names of U3's script and U4's warning. A reasonable single-PR ordering is **U1 → U2 → U3 → U4 → U5 → U6** so each commit independently builds, lints, and tests green; the final PR description summarizes the bundled fix.

## System-Wide Impact

- **Every page that renders icons** gains the dev-time runtime warning. Cost: one `error` event listener per `<use>`. Surface: dev DevTools only (production users don't see warnings). Risk: low — the listener is one closure per icon.
- **CI runtime grows by ≤ 1 second** (the bash lint script is a `find` + `grep` over `src/main/resources/static/` + a `test -f` per match). Acceptable inside `npm run test:ci`'s existing ~30-second budget.
- **Owner cell on `logs.html`** now displays correctly for admin users on the logs listing. No backend change.
- **Plans listing (`plans.html`)** — has no Owner column today, so unaffected. Worth a 5-minute manual sanity check that the markup change in `owner.html` doesn't ripple anywhere unexpected (the template is currently imported via lodash on `logs.html` only — confirmed by the call-site in the data-table cellRenderer).

## Risk Analysis & Mitigation

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| `<use>` `error` event doesn't fire on all targeted browsers | Low | A runtime-warning miss in some browser — not a regression, just a coverage gap | Cross-check on Chrome / Safari / Firefox at U4 verification; the memory `feedback_browser_support_policy` confirms latest-only support |
| Bash lint script's grep regex matches inside an HTML comment or a Storybook fixture intentionally showing an invalid name | Low | False positive failure | Script's grep excludes lines whose entire content is inside an HTML comment; if any Storybook fixture intentionally renders an invalid name, add it to an explicit allowlist file (`frontend/.icon-lint-allowlist`) — none needed today |
| Template change to `owner.html` breaks an existing snapshot test | Low-medium | E2E snapshot diff | Update the snapshot in the same commit as U2; per `feedback_lit_marker_snapshot_normalization`, use the normalized-innerHTML helper if a Playwright DOM snapshot is involved |
| Lint script flags `cts-icon name="${name}"` despite the dynamic check | Medium | False positive | The script extracts the literal value between the quotes; skip when the value starts with `$` or contains `<%-` — explicitly covered in U3's test scenarios |

## Verification Strategy

- `mvn -B -Dmaven.test.skip -Dpmd.skip clean package` (frontend-only changes don't require this, but per workflow rule run the project build before claiming complete — frontend changes don't break Java tests, this is a fast sanity step).
- `cd frontend && npm run test:ci` — must exit 0 with the new `lint:icons` script in the chain.
- `cd frontend && npm run test-storybook` — must exit 0; the three new cts-icon stories and the tightened AllIcons play function pass.
- `cd frontend && npm run test:e2e -- e2e/logs.spec.js` — must exit 0; new assertions for the close-icon and the owner-cell shape pass.
- Manual visual check on `https://localhost.emobix.co.uk:8443/logs.html?status=FINISHED` with an admin user: chip × visible, owner-cell pill restored, hover shows tooltips, cell doesn't wrap.
- Confirm regression coverage: revert U1's icon-name fix locally, re-run `npm run test:ci` — fails on `lint:icons` with the "did you mean close-md?" hint. Restore.

## Patterns to Follow

- `frontend/scripts/lint-jsdoc-properties.sh` — bash lint script wired into `test:ci`.
- `cts-data-table.js` filter-reset pill — existing close-md affordance to mirror.
- `cts-icon.js` `_resolvedSize()` — defensive runtime check precedent.
- `templates/plan.html` / `templates/logHeader.html` — other lodash templates with `<%- %>` interpolation, for confirming `aria-label="<%- owner.sub %>"` is escaped correctly.

## Existing Tests / Stories That Will Change

- `frontend/e2e/logs.spec.js` — add assertions for U1 + U2.
- `src/main/resources/static/components/cts-icon.stories.js` — add U4 stories; modify `AllIcons.play` for U5.
- No deletions; this is additive plus targeted fixes.

## Open Questions Deferred to Implementation

- Exact wording of the `aria-label` (Subject / Issuer vs. fully qualified strings) — the implementing agent should mirror existing accessible-name patterns in `cts-log-detail-header.js` if they exist.
- Whether to add a top-of-file `// regenerate via …` comment to the AllIcons array — out of scope, but the implementer can include if trivial.

## Acceptance

- All three user-reported bugs no longer reproduce on `logs.html`.
- `npm run test:ci` includes `lint:icons` and fails on any new invalid literal `cts-icon name=""`.
- A dev opening DevTools on a page with an invalid dynamic icon name sees one console warning per unique name.
- `CLAUDE.md`'s Icons section is ≤ 6 lines longer than it is today and contains no inline icon-name list.
