# monaco-editor (vendored)

Monaco — the editor that powers VS Code — vendored as the AMD distribution at
`vs/`. Served straight from the static directory at `/vendor/monaco-editor/vs/`
and bootstrapped at runtime by `vs/loader.js`. No bundler involved.

## Source

- **Project:** [Monaco editor](https://microsoft.github.io/monaco-editor/) by Microsoft
- **License:** MIT (see `LICENSE`)
- **Distribution shape:** AMD bundle (`monaco-editor/min/` on npm)
- **Pinned version:** see `VERSION.txt`

## What lives here

A curated, minimal subset of the upstream `min/` tree:

```
vs/
  loader.js                    AMD loader; the only file the page <script>-tags directly
  editor/
    editor.main.js             Core editor bundle (the bulk of the bytes)
    editor.main.css            Editor stylesheet
    editor.main.nls.js         English language strings
  base/worker/workerMain.js    Bootstrap for Web Workers
  language/json/
    jsonMode.js                JSON tokenizer / language contribution
    jsonWorker.js              JSON parser running in the Web Worker
LICENSE
VERSION.txt
```

We deliberately do NOT vendor:

- Other locales (`editor.main.nls.de.js`, etc.) — the CTS UI is English only.
- Other Monaco languages (CSS, HTML, TypeScript, basic-languages) — `<cts-json-editor>` is JSON-only.
- The `dev/` (unminified) tree — production-only repo.

If a future use case needs another language, extend the `KEEP=(…)` list in
`frontend/scripts/update-vendor-monaco.sh`.

## How it loads

Pages must NEVER call `monaco.editor.create(...)` directly. The single supported
entry point is the `<cts-json-editor>` Lit primitive at
`src/main/resources/static/components/cts-json-editor.js`. That component:

1. Lazy-loads `vs/loader.js` on first connect (memoized — multiple instances on
   the same page share one boot).
2. Sets `window.MonacoEnvironment.getWorkerUrl` to a same-origin URL inside this
   directory so the JSON Web Worker passes the worker-src CSP without a Blob.
3. Falls back to a plain `<textarea>` if any of those steps fail (network, CSP,
   parse error in upstream code). The page never breaks.

Direct `<script src="vs/loader.js">` use anywhere else in the codebase is a
review failure: it bypasses the fallback path and the disposal lifecycle.

## Bumping Monaco

Run `frontend/scripts/update-vendor-monaco.sh`. It downloads a pinned tarball,
verifies the SHA-256, and rewrites the `vs/` tree atomically. See the comment
header in that script for the bump procedure.

Do NOT edit files under `vs/` by hand — they are generated artefacts and any
local change will be overwritten by the next vendor refresh.
