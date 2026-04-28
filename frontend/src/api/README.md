# `frontend/src/api/` — committed cache for Java↔JS API type parity

This directory is a **versioned cache** of the conformance suite's `/api/**`
REST surface, refreshed manually with `npm run codegen` from `frontend/`. JS
consumers under `src/main/resources/static/` reference the generated types
through the `@cts-api/*` path alias declared in `frontend/tsconfig.json`.

The cache replaces a Java↔JS DTO-sharing pipeline that is impractical here:
the `/api/**` controllers return `ResponseEntity<Object>` with bodies built
at runtime from Gson `JsonObject`, so springdoc's reflective schema is empty
where it matters most. The cache splits the type sources:

| Source                                       | Carries                                  | Coverage                 |
| -------------------------------------------- | ---------------------------------------- | ------------------------ |
| `openapi.json` (springdoc)                   | path / method / parameter / status types | every `/api/**` endpoint |
| `samples/<key>.json` (captured wire payload) | response field types                     | endpoints with a fixture |

Both are merged into a single `api-types.d.ts` so consumers import one shape.

## Files

| Path                  | Status                   | What                                                                              |
| --------------------- | ------------------------ | --------------------------------------------------------------------------------- |
| `openapi.json`        | generated                | `/v3/api-docs` snapshot. Pretty-printed for diff readability.                     |
| `samples/<key>.json`  | generated                | Live response captured by replaying `fixtures/<key>.json` against the dev server. |
| `fixtures/<key>.json` | hand-authored            | `{ method, path, body? }` for one endpoint. The naming key is the filename.       |
| `api-types.d.ts`      | generated, **read-only** | Final merged TypeScript declarations. Regenerated end-to-end; never hand-edit.    |

## Regeneration

A dev Spring server must be running (see [`CLAUDE.md`](../../../CLAUDE.md)
"Dev loop"). The dev profile activates `DummyUserFilter` so codegen passes
auth without any token plumbing.

```bash
# from frontend/
mvn -B -Dmaven.test.skip -Dpmd.skip clean package    # only if Java has changed
# in another terminal:
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # leave running
# back in frontend/:
npm run codegen
```

Then `git diff frontend/src/api/` and commit. The diff size tells you the
shape of the drift; large diffs after a Java DTO change are the **intended**
drift-detection signal — review them as you would any other generated artifact
update.

Override the base URL with `CTS_BASE_URL`:

```bash
CTS_BASE_URL=http://127.0.0.1:8080 npm run codegen
```

### When to regenerate

- After a Java change that touches `/api/**` controller signatures or
  response bodies — even when the change feels mechanical.
- After adding a new fixture under `fixtures/` (a new endpoint enters
  field-level coverage).
- When a JS consumer's `npm run type-check` complains about a field that you
  know exists at runtime — the sample is stale; refresh it.

### Offline re-merge

```bash
npm run codegen:offline
```

Skips network steps (1, 2) and re-runs the merge from the already-committed
`openapi.json` and `samples/`. CI invokes this in `frontend_lint` and asserts
`git diff --exit-code frontend/src/api/api-types.d.ts` so a hand-edited or
partially-regenerated cache fails CI. CI does **not** boot Spring; Java drift
surfaces only when a contributor next runs `npm run codegen`.

## Authoring fixtures

Each fixture is a small JSON object describing one HTTP request to replay.

```json
{
  "method": "GET",
  "path": "/api/server"
}
```

For request bodies:

```json
{
  "method": "POST",
  "path": "/api/runner",
  "body": { "planId": "abc-123" }
}
```

Pick a stable `<key>` filename (kebab-case is conventional) — the same key
becomes `samples/<key>.json` and the top-level type name `<KeySample>` in
`api-types.d.ts`. Keys do not have to match the URL; favor a name that reads
clearly in `git diff` and JSDoc imports.

A fixture that does not exercise an optional field will produce a sample
missing that field, and the generated type will lack it. That gap is caught
by `npm run type-check` the first time a consumer reads the missing field —
fix by enriching the fixture and re-running codegen.

## Read-only invariant for `api-types.d.ts`

Do not hand-edit `api-types.d.ts`. Regeneration overwrites every line. If a
type looks wrong:

1. Is it a path-level miss? Check `openapi.json` — usually the underlying
   controller signature changed and springdoc reflects it correctly.
2. Is it a field-level miss? Enrich or replace the relevant
   `fixtures/<key>.json` so the captured sample exercises the field.
3. Re-run `npm run codegen` and commit the regenerated file.

If you genuinely need to override a generated type (very rare), put the
override in a separate `.d.ts` under `frontend/src/api/overrides/` and
augment the merged file at consumption time. This directory does not exist
yet; create it on first need rather than speculatively.

## Reference consumer

`src/main/resources/static/components/cts-dashboard.js` — `_fetchServerInfo()`
carries the canonical JSDoc-import pattern:

```js
/** @type {import('@cts-api/api-types').paths['/api/server']
            ['get']['responses']['200']['content']['application/json']} */
const data = await response.json();
```

Copy that shape when adding a new consumer; the alias resolves under `tsc`
(see `frontend/tsconfig.json` `paths` and `include`).

### Why a Lit component, not `js/fapi.ui.js`

The plan originally proposed `fapi.ui.js`'s `loadServerInfo` as the U4 site,
but `frontend/tsconfig.json` only includes `../src/main/resources/static/js/`
into a future migration scope. Bringing that directory under `checkJs: true`
surfaced a large pile of pre-existing strict-null and missing-global errors
in `fapi.ui.js` (lodash `_` calls, untyped DOM lookups). Cleaning those up
is well outside U4's "single annotation" scope. The Lit components directory
(`components/**`) is already type-checked, so `cts-dashboard.js` proves the
JSDoc-import pattern with no broader cleanup. Migrating `js/**` into the
include list is deferred — see the plan's `Open Questions`.

## Installing the codegen toolchain

`openapi-typescript` declares `peerDependencies.typescript: "^5.x"`, but the
project pins `typescript@6.0.3`. The CLI is invoked as a binary (not imported
as a library), so the constraint is cosmetic at runtime. Install with
`--legacy-peer-deps` once when bumping or first installing:

```bash
cd frontend
npm install --legacy-peer-deps
```

CI's `npm ci --ignore-scripts` reproduces the lockfile and does not re-run
peer resolution, so no CI flag change is needed.

## Why we commit captured payloads to a public repo

The repository is open-source and the Java sources already enumerate every
endpoint. A committed cache adds no marginal disclosure beyond what a reader
would see by grepping `*.java`. A future private fork that wants to redact
endpoint paths can `.gitignore` `frontend/src/api/openapi.json` and
`frontend/src/api/samples/`, then run codegen locally — the merge step still
produces a usable `api-types.d.ts`.
