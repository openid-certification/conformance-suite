# marked (vendored)

[marked](https://github.com/markedjs/marked) — fast Markdown → HTML parser. Used
by `src/main/resources/static/components/format-description.js` to render
test-author prose (descriptions, summaries, instructions) as HTML.

- **Version:** 18.0.4
- **File:** `marked.esm.js` — the upstream single-file ESM build
  (`node_modules/marked/lib/marked.esm.js`), self-contained (no external imports),
  loaded verbatim. No build step.
- **Why marked:** the prose is dense with snake_case identifiers
  (`access_token`, `claims_supported`). marked follows CommonMark's
  intraword-underscore rule and leaves them intact, where naive 1-KB parsers
  (snarkdown/nano-markdown) mangle them into `<em>`. GFM is enabled in the
  consumer so bare `https://` URLs autolink.

## How it is loaded

- **Browser (production):** resolved via the `"marked"` importmap entry on every
  static HTML page → `/vendor/marked/marked.esm.js`.
- **Tests / type-check (frontend/):** resolved from `frontend/node_modules/marked`
  (a pinned `devDependency`) via the `.storybook/main.js` Vite alias and the
  `tsconfig.json` `paths` entry. The vendored copy and the devDependency MUST be
  the same version.

## Bumping

No update script (npm is a normal distribution channel). To bump:

1. `cd frontend && npm install --save-dev --save-exact marked@<version>`
2. `cp frontend/node_modules/marked/lib/marked.esm.js src/main/resources/static/vendor/marked/marked.esm.js`
3. Update the version above.
4. Run `npm run test-storybook` (cts-test-summary / cts-log-detail-header stories
   exercise the parser) and `npm run type-check`.
