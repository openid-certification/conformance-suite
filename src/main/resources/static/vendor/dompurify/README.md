# DOMPurify (vendored)

[DOMPurify](https://github.com/cure53/DOMPurify) — HTML sanitizer. Used by
`src/main/resources/static/components/format-description.js` to sanitize
`marked` output before it is handed to Lit's `unsafeHTML`. This is the contract
that makes `unsafeHTML` safe for test-author markdown.

- **Version:** 3.4.7
- **File:** `purify.es.mjs` — the upstream single-file ESM build
  (`node_modules/dompurify/dist/purify.es.mjs`), self-contained, loaded verbatim.
  No build step.

## How it is loaded

- **Browser (production):** resolved via the `"dompurify"` importmap entry on
  every static HTML page → `/vendor/dompurify/purify.es.mjs`.
- **Tests / type-check (frontend/):** resolved from
  `frontend/node_modules/dompurify` (a pinned `devDependency`) via the
  `.storybook/main.js` Vite alias and the `tsconfig.json` `paths` entry. The
  vendored copy and the devDependency MUST be the same version.

## Bumping

No update script (npm is a normal distribution channel). To bump:

1. `cd frontend && npm install --save-dev --save-exact dompurify@<version>`
2. `cp frontend/node_modules/dompurify/dist/purify.es.mjs src/main/resources/static/vendor/dompurify/purify.es.mjs`
3. Update the version above.
4. Run `npm run test-storybook` (the `WithDangerousHtmlSanitized` story asserts
   sanitization) and `npm run type-check`.
