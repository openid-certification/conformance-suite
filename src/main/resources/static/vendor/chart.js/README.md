# Chart.js (vendored)

[Chart.js](https://www.chartjs.org/) vendored as the UMD build, `chart.umd.js`.
Served straight from the static directory at `/vendor/chart.js/chart.umd.js`.
No bundler involved.

## Source

- **Project:** [Chart.js](https://www.chartjs.org/), npm package [`chart.js`](https://www.npmjs.com/package/chart.js)
- **License:** MIT (see `LICENSE.md`)
- **Distribution shape:** UMD bundle (`chart.js/dist/chart.umd.js` on npm)
- **Pinned version:** 4.5.1

## Why the UMD build, not ESM

The npm package also ships an ESM distribution at `dist/chart.js`, but it is
not usable without a bundler:

- It is **multi-file**: `dist/chart.js` itself `import`s
  `./chunks/helpers.dataset.js`, which in turn `import`s the bare specifier
  `@kurkle/color` (a separate Chart.js dependency, not part of this package).
  A bare specifier has no meaning to the browser without an import map or a
  bundler resolving `node_modules`, and we vendor `@kurkle/color` nowhere.
- It is **unminified** (~11.6k lines across the tree).

`dist/chart.umd.js` has neither problem: it is a single minified file
(~204 KB) with `@kurkle/color` bundled directly into it — no `import` or
`require` of anything external. That makes it a drop-in `<script>`-tag
dependency, consistent with how every other vendored library in this tree is
loaded.

## How it loads

Pages must NEVER add a page-level `<script src="/vendor/chart.js/chart.umd.js">`
tag or call `new Chart(...)` directly. The single supported entry point is
the `<cts-chart>` Lit primitive (`src/main/resources/static/components/cts-chart.js`),
which lazily injects the `<script>` tag on first connect (memoized — multiple
chart instances on the same page share one load) and reads the global
`window.Chart` it defines. This is the same rule CLAUDE.md documents for
`<cts-json-editor>` and Monaco: the wrapper component owns the load and
disposal lifecycle, so nothing else may reach around it.

## Bumping Chart.js

Run `frontend/scripts/update-vendor-chartjs.sh`. It downloads a pinned
tarball, verifies the SHA-256, sanity-checks the extracted UMD file's banner
comment, and rewrites `chart.umd.js` + `LICENSE.md` atomically. To bump to a
new version:

1. Edit `CHARTJS_VERSION` in the script.
2. Run the script — it will fail at the digest check.
3. If the downloaded tarball looks right, replace `EXPECTED_TARBALL_SHA256`
   with the new digest shown in the error.
4. Re-run; the script should succeed.
5. Commit the script change and the regenerated `chart.umd.js` + `LICENSE.md`
   together in one MR.

Do NOT edit `chart.umd.js` by hand — it is a generated artefact and any local
change will be overwritten by the next vendor refresh.
