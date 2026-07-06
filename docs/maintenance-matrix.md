# Maintenance matrix

Cross-cutting "if you change X, also update Y" cascades, in one table. Each
rule's canonical home is the owning surface in the root `AGENTS.md`
conventions map — this file is the navigational index, not a second
canonical copy. When a cascade changes, update the owning surface AND this
row in the same change (see "Self-amending conventions" in `AGENTS.md`).

| If you change… | Also update… |
|---|---|
| A condition reading a new config field (`env.getElementFromObject("client", "x")`) | `@ConfigurationFields` on the owning abstract base class, else the field never appears in the schedule-test UI |
| A configuration-error message in a condition | Keep it matching the UI label shown on `schedule-test.html` (not the internal JSON key) |
| An API response shape consumed by the frontend | Fixtures in `frontend/e2e/fixtures/` and the affected spec files |
| Page DOM structure (IDs/classes used by JS) | The page's spec file in `frontend/e2e/` |
| The Lit importmap (new directive entry) | All `src/main/resources/static/*.html` pages + `DIRECTIVE_PROBES` in `frontend/e2e/lit-importmap.spec.js` |
| A `cts-*` component's `static properties` | Its JSDoc `@property` block (`lint:jsdoc` + eslint-plugin-jsdoc enforce) and its stories' `play()` assertions |
| Any `cts-icon name="..."` literal | A vendored SVG must exist at `src/main/resources/static/vendor/coolicons/icons/<name>.svg` (`lint:icons` fails otherwise) |
| A design-token decision | Append to the "Deliberate deviations" ledger in `src/main/resources/static/css/oidf-tokens.css` — never edit vendored values in place |
| A new test module class | `@PublishTestModule` annotation + membership in a `@PublishTestPlan` plan class |
| A spec reference string prefix (e.g., `"FAPI2SP-5.2.2"`) | The `specLinks` map in `src/main/java/net/openid/conformance/export/LogEntryHelper.java` |
| `frontend/package.json` dependencies | Regenerate `package-lock.json` with the corepack-pinned npm and Linux platform flags (see `frontend/AGENTS.md`) |
| Any `AGENTS.md` file | Stay inside the per-path byte budgets (`npm run lint:agents`); keep the sibling `CLAUDE.md` shim intact |
| `.codex/hooks.json` | Teammates must re-run the Codex hook-trust review (hash-based; changed hooks are silently skipped until re-trusted) |
| Vendored Lit or Monaco | Bump only via `frontend/scripts/update-vendor-lit.sh` / `update-vendor-monaco.sh` (SHA-pinned) |
