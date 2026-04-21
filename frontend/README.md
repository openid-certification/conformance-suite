# frontend/

Tooling home for the CTS frontend: Prettier, ESLint, tsc, Storybook, Playwright.
The actual web components live in `../src/main/resources/static/components/` —
lint/format/type-check globs reach into that directory via `../` paths.

## Local commands

| Script                      | What it does                                                                                               | When to run                                          |
| --------------------------- | ---------------------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| `npm run format`            | Prettier `--write` across `frontend/` and `../src/main/resources/static/components/`.                      | Before committing style-only changes.                |
| `npm run format:check`      | Prettier `--check` (no writes).                                                                            | Part of `test:ci`.                                   |
| `npm run lint`              | ESLint flat config over frontend + components.                                                             | Before committing.                                   |
| `npm run lint:fix`          | ESLint `--fix` for auto-fixable findings.                                                                  | After initial lint failures.                         |
| `npm run type-check`        | `tsc --noEmit` for root + `e2e/` tsconfigs.                                                                | After touching JS/JSDoc types.                       |
| `npm run lint:jsdoc`        | Shell presence check that every `cts-*` class has a JSDoc block with `@property` tags.                     | Before committing component changes.                 |
| `npm run lint:lit-analyzer` | `lit-analyzer` CLI over `cts-*.js`. Lit-aware template checks (unknown tags, bad bindings, unclosed tags). | Before committing component changes.                 |
| `npm run test:ci`           | `format:check && lint && type-check && lint:jsdoc && lint:lit-analyzer`. Exactly what CI runs.             | Before pushing.                                      |
| `npm run test`              | `test:ci && test-storybook`. Local full run; requires a browser.                                           | Before opening an MR.                                |
| `npm run test:e2e`          | Playwright against legacy static HTML.                                                                     | After editing `../src/main/resources/static/*.html`. |
| `npm run storybook`         | Launch Storybook dev server on port 6006.                                                                  | While authoring components.                          |
| `npm run build-storybook`   | Static Storybook build.                                                                                    | Rarely; CI handles this.                             |
| `npm run test-storybook`    | Vitest runner for Storybook play functions.                                                                | After adding or editing interaction tests.           |

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

## Install vs lockfile regeneration

Two different jobs, two different commands:

- **Sync with the committed lockfile** (the common case — after `git pull`,
  or when setting up a fresh checkout): `npm ci --ignore-scripts`. Does not
  mutate `package-lock.json`.
- **Regenerate the lockfile** (only after you change `package.json` —
  bumping a dep, adding a new one, removing one):
  `./scripts/regen-lockfile.sh`. The script runs `npm install` inside the
  same `node:22-alpine` image CI uses, via Corepack-pinned npm, and writes
  the result back to your working tree. The regen leaves `node_modules/`
  removed because the container's install is Linux-native; follow it with
  `npm ci --ignore-scripts` to restore a host-native `node_modules/`.

  Avoid running `npm install` directly on macOS — npm records
  platform-biased optional deps there, which then break `npm ci` in Linux
  CI.

Commit only `frontend/package-lock.json` after a regeneration (your
`frontend/package.json` change should already be staged separately).

## Failure-mode decoder

- **`format:check` fails** — Prettier reports a diff. Fix: `npm run format` (writes `--write`).
- **`lint` fails** — ESLint reports errors (or warnings that the job promoted to errors). Fix auto-fixable findings with `npm run lint:fix`; read the remaining output and edit the offending lines.
- **`type-check` fails** — `tsc` reports a type error. Fix: correct the JSDoc `@type` or the call site; narrow a constructor initializer when the inferred literal type is too loose.
- **`lint:jsdoc` fails** — A `cts-*` class is missing its class-level JSDoc block with `@property` tags. Fix: add the JSDoc block above the class (see "JSDoc dual-convention" below).
- **`lint:lit-analyzer` fails** — A Lit template diagnostic errored. Common causes: unknown HTML tag name (typo), wrong binding sigil (`class=` vs `.class=` vs `?class=`), unclosed tag, property name mismatch on a `cts-*` child. Fix: the CLI output points at the offending line; read the rule name in the message and the Lit error docs (https://lit.dev/msg/) for context.

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
