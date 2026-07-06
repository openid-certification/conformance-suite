# CTS Static UI — Agent Guide

UI conventions for this directory (component authoring rules:
`components/AGENTS.md`). Expected workflow for any UI change: preflight
(`scripts/ui-preflight.sh`), implement per these conventions, let the edit
hooks run `frontend/scripts/agent-edit-check.sh`, review the rendered
result with the `cts-design-eye` skill, and run
`cd frontend && npm run test:ci` before committing. Never present visually
unverified work as verified.

## Dev loop (save-and-see)

The `dev` profile activates `spring-boot-devtools` plus a source-tree
static handler: edits here reflect on the next browser load (LiveReload
auto-reloads; F5 works too), and Java edits trigger a fast classloader
restart in ~5 s.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

In IntelliJ: active profile `dev`, "Build project automatically", Registry
flag `compiler.automake.allow.when.app.running`. LiveReload runs on port
35729 (DevTools serves `livereload.js` same-origin). Static-only edits need
no JVM restart — DevTools watches `src/main/resources/static` per
`spring.devtools.restart.additional-paths` in `application-dev.properties`.

If save-and-see does not work, you are most likely running the packaged fat
JAR instead of `spring-boot:run` — the production JAR intentionally excludes
DevTools. If a static edit still doesn't show, a stale classpath copy is the
usual cause: sync `src/main/resources/static` → `target/classes/static`
(`/bin/cp -f`).

**Production-parity invariant.** `spring-boot-devtools` MUST stay
`<scope>provided</scope>` in `pom.xml`. The `maven-enforcer-plugin` rule
`enforce-devtools-scope` fails the build at `validate` phase if the scope
drifts. Do not bypass the rule.

## Legacy UI profile

The opt-in `legacy-ui` profile serves a frozen pre-redesign snapshot
(`static-legacy/` + `templates-legacy/`, tag `release-v5.1.45`) from the
same JAR. See `application-legacy-ui.properties` and `LegacyUiConfig`.

## Icons

All icons render via `<cts-icon name="<kebab>" size="16|20|24">`. The
library is coolicons v4.1, vendored one-SVG-per-icon at
`vendor/coolicons/icons/{name}.svg`; `name` is the filename without `.svg`.
Sizes track `--space-4/5/6`; stroke follows `currentColor`.

- **Never guess names** — `close-md` exists, `x` does not. Look up on
  demand: `ls` the vendored icons dir, or Storybook (cts-icon → AllIcons).
  Do not paste the 442-icon list into context.
- **Enforcement:** `lint:icons` (in `test:ci`) fails on any literal
  `cts-icon name` without a vendored SVG; dynamic names warn at runtime.
- **Adding an icon:** one-shot extract per `vendor/coolicons/README.md`;
  no build step.
- **Brand glyphs** (Google, GitLab) stay outside cts-icon — inline `html`
  constants colocated in `components/cts-login-page.js` only.
- **Do NOT** hand-roll inline `<svg>` icons, reference removed Bootstrap
  Icons (`bi-*`), or build icon names by string concatenation.

## Badges

All status pills, label chips, and count badges render via
`<cts-badge variant="<name>">`. Status palette (`pass/fail/warn/running/
skip/review`) and utility variants (`primary/secondary/danger/info-subtle`)
are token-routed; see `components/cts-badge.js` and its Storybook page.

**Affordance rule:** the visual state must reflect whether clicking does
anything.

- **Read-only (default):** fill only, no ring — the badge is a label.
- **Interactive:** fill + 1px inset box-shadow ring + hover/focus — the
  badge is (or carries) the click affordance.

Attributes: `interactive` is visual-only (use when the badge sits inside an
`<a>`/`<button>` that stripped its own affordance); `clickable` adds
`role="button"`, keyboard activation, and `cts-badge-click` (use when the
badge IS the target and has no interactive wrapper; implies the ring).

Decision tree: (1) badge itself the click target → `clickable`; (2) not
wrapped in an interactive element → read-only; (3) wrapped, but the wrapper
shows its own affordance → read-only, else → `interactive`.

Token deviation: the readonly `b-rev` (Review) chip fills with
`var(--bg-muted)` until a `--status-review-bg` token exists (then update
`cts-badge.js` JSDoc).

**Do NOT** fake the ring with a 1px `border` (inset box-shadow keeps
box-model dimensions identical; a border shifts them), put `clickable` on a
badge inside a clickable parent (nested `role="button"`), or use the removed
Bootstrap utilities (`bg-warning`, `bg-info`, `bg-info-subtle`,
`border-info-subtle`, `text-info-emphasis`).

## Frontend E2E tests

Playwright specs in `frontend/e2e/` validate these static pages with mocked
API responses — no backend. Run after modifying anything here (HTML,
`js/fapi.ui.js`, `templates/`, `css/`): `cd frontend && npm run test:e2e`
(one spec: `npx playwright test e2e/home.spec.js`).

Conventions: one spec per page (`journeys.spec.js` for cross-page flows);
`setupFailFast()` FIRST, then specific routes (Playwright matches routes in
reverse registration order); all `page.route()` calls before `page.goto()`
(`fapi.ui.js` fires an API call at parse time); fixtures are ES modules in
`frontend/e2e/fixtures/` — update them when an API response shape changes;
`wrapDataTablesResponse()` wraps arrays in the DataTables envelope for
server-side-paginated pages (plans.html, logs.html).

## Frontend quality gates

`cd frontend && npm run test:ci` mirrors the `frontend_lint` CI job:
format:check → lint → type-check → lint:jsdoc → lint:icons → lint:agents →
lint:lit-analyzer → codegen:check. `lint:agents` enforces the AGENTS.md path
budgets (Codex caps instruction files at 32 KiB per path). Failure-mode decoder and severity ladder: `frontend/README.md`.
Default lint severity is `error`; per-file exceptions live only in the
named Legacy Overrides block of `frontend/eslint.config.js` — never
blanket `"off"`.

## Key dependencies

- **Lit**: vendored full bundle at `vendor/lit/lit.js` (`lit-all.min.js`) —
  every directive available without a bundler. Bump via
  `frontend/scripts/update-vendor-lit.sh` (tag + SHA-256 pinned).
- **Monaco**: vendored AMD subset at `vendor/monaco-editor/vs/`. The single
  supported entry point is `<cts-json-editor>` — never call
  `monaco.editor.create(...)` directly (bypasses lazy-load, textarea
  fallback, and disposal). Bump via
  `frontend/scripts/update-vendor-monaco.sh`; rationale in
  `vendor/monaco-editor/README.md`.
