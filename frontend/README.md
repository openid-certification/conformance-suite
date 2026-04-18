# frontend/

Tooling home for the CTS frontend: Prettier, ESLint, tsc, Storybook, Playwright.
The actual web components live in `../src/main/resources/static/components/` —
lint/format/type-check globs reach into that directory via `../` paths.

## Local commands

| Script                    | What it does                                                                           | When to run                                          |
| ------------------------- | -------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| `npm run format`          | Prettier `--write` across `frontend/` and `../src/main/resources/static/components/`.  | Before committing style-only changes.                |
| `npm run format:check`    | Prettier `--check` (no writes).                                                        | Part of `test:ci`.                                   |
| `npm run lint`            | ESLint flat config over frontend + components.                                         | Before committing.                                   |
| `npm run lint:fix`        | ESLint `--fix` for auto-fixable findings.                                              | After initial lint failures.                         |
| `npm run type-check`      | `tsc --noEmit` for root + `e2e/` tsconfigs.                                            | After touching JS/JSDoc types.                       |
| `npm run lint:jsdoc`      | Shell presence check that every `cts-*` class has a JSDoc block with `@property` tags. | Before committing component changes.                 |
| `npm run test:ci`         | `format:check && lint && type-check && lint:jsdoc`. Exactly what CI runs.              | Before pushing.                                      |
| `npm run test`            | `test:ci && test-storybook`. Local full run; requires a browser.                       | Before opening an MR.                                |
| `npm run test:e2e`        | Playwright against legacy static HTML.                                                 | After editing `../src/main/resources/static/*.html`. |
| `npm run storybook`       | Launch Storybook dev server on port 6006.                                              | While authoring components.                          |
| `npm run build-storybook` | Static Storybook build.                                                                | Rarely; CI handles this.                             |
| `npm run test-storybook`  | Vitest runner for Storybook play functions.                                            | After adding or editing interaction tests.           |

## Reproduce CI locally

```bash
cd frontend && npm ci && npm run test:ci
```

## Failure-mode decoder

- **`format:check` fails** — Prettier reports a diff. Fix: `npm run format` (writes `--write`).
- **`lint` fails** — ESLint reports errors (or warnings that the job promoted to errors). Fix auto-fixable findings with `npm run lint:fix`; read the remaining output and edit the offending lines.
- **`type-check` fails** — `tsc` reports a type error. Fix: correct the JSDoc `@type` or the call site; narrow a constructor initializer when the inferred literal type is too loose.
- **`lint:jsdoc` fails** — A `cts-*` class is missing its class-level JSDoc block with `@property` tags. Fix: add the JSDoc block above the class (see "JSDoc dual-convention" below).

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

## Storybook tests are separate

`npm run test` includes `test-storybook`; `npm run test:ci` does not. Storybook
play-function tests run in a browser and live in a different CI job (deferred;
not yet wired).

## `--ignore-rev` candidates

Two mechanical commits are safe to skip in `git blame`:

- `088b4041b` — Prettier reformat (Unit 2)
- `c1249016e` — `eslint --fix` mechanical (Unit 4)

To apply locally, create `.git-blame-ignore-revs` at the repo root with:

```
# Prettier 3.8 reformat (no semantic changes)
088b4041b
# ESLint auto-fixes (no semantic changes)
c1249016e
```

Then point git at it:

```bash
git config blame.ignoreRevsFile .git-blame-ignore-revs
```
