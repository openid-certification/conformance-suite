# frontend/

Tooling home for the CTS frontend: Prettier, ESLint, tsc, Storybook, Playwright.
The actual web components live in `../src/main/resources/static/components/` —
lint/format/type-check globs reach into that directory via `../` paths.

## Dev loop (save-and-see)

Run the backend with the `dev` profile so static-asset edits under
`../src/main/resources/static/` reflect on the next browser load without a
rebuild:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

LiveReload (port 35729) auto-refreshes the tab; plain F5 also works. Java edits
trigger a fast classloader restart (~5s). See the "Dev loop" section in the
top-level [`CLAUDE.md`](../CLAUDE.md) for the full launch path, the
production-parity invariant, and the `SPRING_PROFILES_ACTIVE=dev` warning.

For Java↔JS API type parity, the codegen pipeline at `src/api/` snapshots
`/v3/api-docs` and runtime sample responses into committed `.d.ts` types — see
[`src/api/README.md`](src/api/README.md) for the `npm run codegen` workflow.

## Local commands

| Script                      | What it does                                                                                               | When to run                                           |
| --------------------------- | ---------------------------------------------------------------------------------------------------------- | ----------------------------------------------------- |
| `npm run format`            | Prettier `--write` across `frontend/` and `../src/main/resources/static/components/`.                      | Before committing style-only changes.                 |
| `npm run format:check`      | Prettier `--check` (no writes).                                                                            | Part of `test:ci`.                                    |
| `npm run lint`              | ESLint flat config over frontend + components.                                                             | Before committing.                                    |
| `npm run lint:fix`          | ESLint `--fix` for auto-fixable findings.                                                                  | After initial lint failures.                          |
| `npm run type-check`        | `tsc --noEmit` for root + `e2e/` tsconfigs.                                                                | After touching JS/JSDoc types.                        |
| `npm run lint:jsdoc`        | Shell presence check that every `cts-*` class has a JSDoc block with `@property` tags.                     | Before committing component changes.                  |
| `npm run lint:lit-analyzer` | `lit-analyzer` CLI over `cts-*.js`. Lit-aware template checks (unknown tags, bad bindings, unclosed tags). | Before committing component changes.                  |
| `npm run test:ci`           | `format:check && lint && type-check && lint:jsdoc && lint:lit-analyzer`. Exactly what CI runs.             | Before pushing.                                       |
| `npm run test`              | `test:ci && test-storybook`. Local full run; requires a browser.                                           | Before opening an MR.                                 |
| `npm run test:e2e`          | Playwright against legacy static HTML.                                                                     | After editing `../src/main/resources/static/*.html`.  |
| `npm run storybook`         | Launch Storybook dev server on port 6006.                                                                  | While authoring components.                           |
| `npm run build-storybook`   | Static Storybook build.                                                                                    | Rarely; CI handles this.                              |
| `npm run test-storybook`    | Vitest runner for Storybook play functions **and** axe-core a11y checks (see "Accessibility testing").     | After adding/editing interaction tests or components. |

## Getting started

Run this once per machine so `npm` inside `frontend/` matches the
`packageManager` pin in `package.json` (currently `npm@11.12.1`). Corepack
ships with Node 16.13+ and is on by default in Node 22+, but the shim for
`npm` itself needs one explicit opt-in:

```bash
corepack enable npm
```

Afterwards, `npm --version` inside `frontend/` will report the pinned
version regardless of what your global `npm` happens to be. CI does the
same in `frontend_lint`, so contributors and CI agree on one tool.

Then, to install dependencies and reproduce the CI lint chain:

```bash
cd frontend && npm ci --ignore-scripts && npm run test:ci
```

`npm ci --ignore-scripts` syncs your `node_modules/` to the committed
`package-lock.json` exactly (no lockfile mutation, no postinstall scripts —
the MSW worker is pre-committed at `frontend/public/mockServiceWorker.js`).

When you **bump or add a dependency**, regenerate the lockfile with
explicit platform flags so it records the Linux optional deps CI needs
(otherwise `npm install` on macOS produces a host-biased lockfile that
`npm ci` rejects inside `node:22-alpine`):

```bash
rm -rf node_modules package-lock.json
npm install --ignore-scripts --os=linux --cpu=x64 --libc=musl
npm ci --ignore-scripts    # restore a host-native node_modules/
```

Commit only `frontend/package-lock.json` alongside your `package.json` change.

## Design tokens

The OIDF design system ships as two token files served from
`/css/` on every page (link order matters):

1. **`oidf-tokens.css`** — vendored verbatim from the design archive.
   Single source of truth for `--oidf-*`, `--ink-*`, `--orange-*`,
   `--rust-*`, `--sand-*`, `--space-*`, `--radius-*`, `--shadow-*`,
   `--font-sans`, `--font-mono`, `--ease-standard`, `--focus-ring`.
   Re-vendor from upstream rather than editing in place. The rendered
   catalog lives in Storybook under **Tokens** (`npm run storybook`):
   color swatches, type specimens, spacing ramps, and
   radii/elevation/motion, all read live from the sheet and
   completeness-gated by the story tests — a token added or removed
   without a catalog update fails the suite by name.
2. **`oidf-app.css`** — load-bearing compatibility layer. Carries the
   body `font-family: var(--font-sans)` rule (replaces the historical
   PT Sans link), the `.collapse` / `.collapse.show` toggle (mirrors
   Bootstrap's behavior so collapsed-by-default markup keeps its
   intent once `bootstrap.min.css` leaves), and the
   `dialog:not([open]) { display: none }` first-paint hide rule for
   the new `<dialog>`-based `cts-modal`. Links AFTER `layout.css`
   so its body rule wins.

**Typography:** self-hosted Inter (variable woff2, weight axis
100..900) declared by an `@font-face` block inlined at the top of
`oidf-tokens.css` — the `system-ui` chain in `--font-sans` only
renders during the brief swap window before Inter loads. JetBrains
Mono via Google Fonts CDN for code/JWT/JSON pages (`log-detail`,
`schedule-test`, `tokens`, `upload`).

**`cts-*` consumption rule:** components reach for tokens through
scoped `<style>` blocks in their render method. No hard-coded hex,
spacing, or radius values in `cts-*` markup. No project-wide utility
CSS — page and component layouts own their own styles.

## Failure-mode decoder

- **`format:check` fails** — Prettier reports a diff. Fix: `npm run format` (writes `--write`).
- **`lint` fails** — ESLint reports errors (or warnings that the job promoted to errors). Fix auto-fixable findings with `npm run lint:fix`; read the remaining output and edit the offending lines.
- **`type-check` fails** — `tsc` reports a type error. Fix: correct the JSDoc `@type` or the call site; narrow a constructor initializer when the inferred literal type is too loose.
- **`lint:jsdoc` fails** — A `cts-*` class is missing its class-level JSDoc block with `@property` tags. Fix: add the JSDoc block above the class (see "JSDoc dual-convention" below).
- **`lint:icons` fails** — A literal `cts-icon name="<value>"` references an SVG that is not under `src/main/resources/static/vendor/coolicons/icons/`. Fix: run `ls src/main/resources/static/vendor/coolicons/icons/` (or browse Storybook → Components/cts-icon → AllIcons) to find a valid name, or vendor a new icon per `src/main/resources/static/vendor/coolicons/README.md` if your case is genuinely new. The script suggests `close-md` / `close-sm` / `close-lg` for the common `x` / `cross` / `close` typos.
- **`lint:lit-analyzer` fails** — A Lit template diagnostic errored. Common causes: unknown HTML tag name (typo), wrong binding sigil (`class=` vs `.class=` vs `?class=`), unclosed tag, property name mismatch on a `cts-*` child. Fix: the CLI output points at the offending line; read the rule name in the message and the Lit error docs (https://lit.dev/msg/) for context.

## `schedule-test.html` snapshot baselines

`e2e/schedule-test-baselines.spec.js` captures DOM HTML + accessibility-tree
snapshots for `schedule-test.html` in three states (empty new plan; cascade
mid-selection; plan with config loaded). The point is to make drift visible
when the five Tier 1 R-MRs (R9, R13, R14, R15, R42) land on this page —
unintended changes show up as snapshot diffs you can read line by line.

**When to refresh** — only when an MR intentionally changes `schedule-test.html`'s
rendered output or accessibility tree. Unintended diffs are bugs, not noise.

**How to refresh** — `cd frontend && npx playwright test schedule-test-baselines.spec.js --update-snapshots`.

**First-run UX caveat** — Playwright fails the test on the same run that
generates a missing snapshot (this is by design — `updateSnapshots: "missing"`
default + `_failWithError`). Re-run without `--update-snapshots` to confirm
the regenerated snapshot passes. The first failure isn't a bug.

**What to commit** — both the spec change and the regenerated snapshot files
in the same commit, so reviewers see the diff as one reviewable unit.

**What snapshots cover** — missing labels, structural drift, aria role/name
changes. They do **not** reliably catch label/input _misassociation_
(`<label for="X">…<input id="Y">` looks identical to "no label" in the
aria tree). The dedicated `assertLabelInputPairing` and `assertNoIdCollisions`
helpers in `e2e/helpers/assertions.js` catch that class. Reviewers should
verify both pass on every refresh.

Stress-test command before committing baselines:
`npx playwright test schedule-test-baselines.spec.js --repeat-each=10 --workers=5`.

## Severity ladder (R10)

Default severity is `error`. Plugin-preset warnings from `eslint-plugin-lit` /
`eslint-plugin-wc` that don't reflect real bugs stay at `warn` (they don't
affect the CI exit code). When a preset-warn rule is elevated to `error` and
existing code surfaces findings, per-file exceptions land in the **Legacy
Overrides** block in `../eslint.config.js` — named, bounded, and tracked to
zero. Never blanket `"off"`, never an unbounded glob.

## `_*.js` helper convention (R8a)

Files starting with `_` (e.g. `_button-classes.js`) are plain JS helpers, not
custom-element definitions. They import into `cts-*` components but are
exempted from `wc/*`, `lit/*`, and `jsdoc/require-property` rules. New `_*.js`
helpers are welcome and get the same treatment — this is convention, not a
legacy override.

## JSDoc dual-convention (R14)

Two JSDoc annotations serve two different consumers. Both are required.

- **Class-level `@property`** — for human readers, the CEM analyzer, and
  Storybook docs.
- **Constructor `/** @type {...} \*/`** — for `tsc` inference where the
  initializer's literal type is too narrow.

```js
/**
 * @property {string} variant - One of: light, info, primary, danger, ...
 * @property {boolean} disabled - Disables the button
 */
class CtsButton extends LitElement {
  constructor() {
    super();
    /** @type {string} */
    this.variant = "light";
    this.disabled = false;
  }
}
```

## Directives we reach for

The vendored Lit bundle at `../src/main/resources/static/vendor/lit/lit.js`
is the full [`lit-all.min.js`](https://github.com/lit/dist) bundle (~29 KB
minified), so every directive from `lit/directives/*` is available at
runtime without a bundler. Import the directive from its sub-path —
`import { classMap } from "lit/directives/class-map.js";` — so readers
see exactly which directive is in use. The importmap across the static
HTML pages aliases every `lit/directives/*.js` specifier back to the
same single bundle file.

| Problem                                                          | Reach for   | Why                                                                              |
| ---------------------------------------------------------------- | ----------- | -------------------------------------------------------------------------------- |
| Dynamic class list (toggle `active`, `is-invalid`)               | `classMap`  | Replaces template-string concatenation, which `AGENTS.md §7` forbids.            |
| Conditional subtree (render X if Y else nothing)                 | `when`      | More scannable than `cond ? html\`...\` : nothing` ternaries.                    |
| Keyed list where item identity matters (`link.page`, `entry.id`) | `repeat`    | Preserves DOM identity across re-renders — focus, selection, animations survive. |
| Conditional attribute (omit if `undefined`)                      | `ifDefined` | `${ifDefined(x)}` renders nothing when `x` is `undefined`.                       |
| Element reference                                                | `ref`       | Replaces `this.renderRoot.querySelector(...)` patterns.                          |
| Inline style object                                              | `styleMap`  | Type-safe alternative to `style="..."` string assembly.                          |

Hoisting a `.map()` call out of a template into a class method returning
`TemplateResult[]` is still valid — use it when there is no stable key.
With a stable key, prefer `repeat`.

The `ts-lit-plugin` plugin is wired into `tsconfig.json` for IDEs that
speak TypeScript language-service (VS Code, JetBrains, Neovim with the
`typescript` LSP). No install step beyond `npm ci`. The `lit-analyzer`
CLI (`npm run lint:lit-analyzer`) is the headless equivalent and runs
under `test:ci`. Baseline: 9 warnings in 4 files (type-binding and
nullable-attribute findings in `cts-button`, `cts-image-upload.stories`,
`cts-link-button`, `cts-plan-modules`) — tracked for triage before the
CLI is promoted to strict / error severity.

## Storybook tests are separate

`npm run test` includes `test-storybook`; `npm run test:ci` does not. Storybook
play-function tests run in a browser and live in a different CI job (deferred;
not yet wired).

## Accessibility testing

`npm run test-storybook` runs axe-core (via `@storybook/addon-a11y`) on **every**
story, in addition to its play function. Setup lives in two places:

- `.storybook/main.js` registers the addon for the manager **panel** only.
- `.storybook/preview.js` composes the addon into the Vitest runner via
  `definePreview({ addons: [a11yAddon()] })` — this is what actually surfaces the
  axe `afterEach` into `test-storybook`. Registering in `main.js` alone is **not**
  enough for the test run (CSF-factory previews compose addons through the
  `addons` array, not through `main.js`).

The project default is `parameters.a11y.test: "error"` — a violation fails
`test-storybook`. The three values: `"off"` (skip; panel only), `"todo"` (run,
violations warn but do not fail), `"error"` (run, violations fail CLI/CI).

**Review-on-fail backlog.** The redesign carries a known a11y backlog (color
contrast below WCAG AA, some ARIA role/attr usage). Rather than turning a11y
off, the offending **rules** are downgraded project-wide to "needs review" via
`parameters.a11y.config.rules: [{ id, reviewOnFail: true }]` in `preview.js`.
`reviewOnFail` moves a rule from "violation" (fails) to "needs review" (panel
only, does not fail), per the axe-core config contract. Every **other** rule
stays enforced on every story, and a **new** failure of any rule not on the
backlog list still fails the build. The `config.rules` list in `preview.js` IS
the fix-later tracker — each entry is one rule's debt; delete an entry to re-arm
that rule once it is fixed. (For the rare case of parking one specific story
rather than a rule project-wide, set `a11y: { test: "todo" }` on that story with
an inline `// a11y review backlog: <rules>` comment — no story currently needs
this.)

**Primary buttons are a permanent contrast exemption, not backlog.** The orange
primary button (`.oidf-btn-primary`, the `--orange-400` CTA with white text) is
a deliberate brand trade-off, [defensible on accessibility grounds](https://www.bounteous.com/insights/2019/03/22/orange-you-accessible-mini-case-study-color-ratio/).
It is exempted from `color-contrast` via the rule's `selector`
(`*:not(.oidf-btn-primary)`) — a one-place decision rather than a per-button
override, so it survives backlog cleanup. The `reviewOnFail` on the same rule
parks the _remaining_ contrast debt (mostly status badges and log-card links).
Fixing that debt means dropping `reviewOnFail` from the `color-contrast` entry
while keeping the `selector` — contrast then enforces everywhere except the
exempt primary button.

The Storybook a11y suite is **not** in CI today (only `npm run test:ci` runs in
the `frontend_lint` job, which excludes `test-storybook`). So a11y is a local +
Storybook-MCP gate for now: **`npm run test`** (which includes `test-storybook`)
is the correct pre-push gate — a green `npm run test:ci` does **not** exercise
a11y. Wiring `test-storybook` into CI is deferred.

## `--ignore-rev` candidates

Three mechanical commits are safe to skip in `git blame`:

- `088b4041b` — Prettier reformat (Unit 2)
- `c1249016e` — `eslint --fix` mechanical (Unit 4)
- `625ac0992` — exact-pin normalization of `package.json` (no resolved versions changed beyond two pre-captured caret drifts)

To apply locally, create `.git-blame-ignore-revs` at the repo root with:

```
# Prettier 3.8 reformat (no semantic changes)
088b4041b
# ESLint auto-fixes (no semantic changes)
c1249016e
# Exact-pin normalization (specifier strings only)
625ac0992
```

Then point git at it:

```bash
git config blame.ignoreRevsFile .git-blame-ignore-revs
```
