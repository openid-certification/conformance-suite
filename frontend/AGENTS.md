# frontend/ — Agent Guide

npm toolchain and lockfile rules for the frontend workspace. UI conventions
(icons, badges, E2E, quality gates) live in
`../src/main/resources/static/AGENTS.md`; component authoring rules in
`../src/main/resources/static/components/AGENTS.md`.

## Toolchain

- Node `>= 22`; npm is pinned via the `packageManager` field in
  `package.json` (npm 11.13.x through corepack). Run `corepack enable` once
  per machine.
- Bootstrap with `npm ci --ignore-scripts`.
- `package.json` pins exact dependency versions — no `^` or `~` ranges.

## Lockfile rules

Regenerate `package-lock.json` only with the corepack-pinned npm, and with
explicit platform flags so it records the Linux optional deps CI needs
(a macOS-biased lockfile fails `npm ci` inside `node:22-alpine`):

```bash
rm -rf node_modules package-lock.json
npm install --ignore-scripts --os=linux --cpu=x64 --libc=musl
npm ci --ignore-scripts    # restore a host-native node_modules/
```

Commit only `package-lock.json` alongside the `package.json` change. Full
rationale: `README.md` "Getting started".

## Formatting

Format via `npm run format` (or `format:check`). Never run bare
`npx prettier` — without the explicit `--config ./.prettierrc.json` it picks
up the root `.editorconfig` tabs and corrupts indentation.

## Quality gates

`npm run test:ci` is the local mirror of the `frontend_lint` CI job
(format:check → lint → type-check → lint:jsdoc → lint:icons → lint:agents →
lint:lit-analyzer → codegen:check). Run it before committing any frontend or
`static/` change. Failure-mode decoder: `README.md` in this directory.

`npm run test-storybook` runs the Storybook component + a11y suite; run it
when components, stories, play functions, or a11y-relevant behavior change.
