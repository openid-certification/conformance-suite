---
title: "feat: Show Alias on the plan-detail header"
type: feat
status: active
date: 2026-06-09
depth: standard
---

# feat: Show Alias on the plan-detail header

## Summary

On `plan-detail.html`, the plan header (`cts-plan-header`) currently surfaces the
user-set **description** as a lede plus a metadata grid (Variant, Plan ID, Plan
Version, Started, optional Owner / Certification profile). It does **not** show the
**alias** — the user-specified, URL-safe logical name that becomes part of the
generated callback/redirect URLs. This plan adds a conditional **Alias** row to the
header metadata grid, sourced from the data the API already returns.

This is a **frontend-only** change. The alias is already present in the authenticated
`GET /api/plan/{id}` response at `config.alias`; no backend, schema, or data-migration
work is required.

---

## Problem Frame

The alias is the most human-meaningful identifier a tester assigns to a plan run — it
is what disambiguates their callback URLs from other testers' and (for some ecosystems,
e.g. KSA) is the regulator-assigned value. Today a tester viewing a plan-detail page
can see the freeform `description` but not the `alias`, so they cannot confirm which
alias a given plan run was created under without re-opening the config JSON.

The request: surface the alias in the plan-detail "test plan listing" (the header
card), alongside the existing description.

---

## Key Technical Decisions

### KTD1 — Read the alias from `plan.config.alias`, not a top-level `plan.alias`

The backend never hoists `alias` to a top-level `Plan` field. The `Plan` constructor
(`src/main/java/net/openid/conformance/info/Plan.java`) stores the **entire** submitted
config object verbatim:

```java
this.config = org.bson.Document.parse(new GsonBuilder().serializeNulls().create()
    .toJson(MongoKeyWrapper.wrap(config)));
```

The POST `/api/plan` handler (`src/main/java/net/openid/conformance/info/TestPlanApi.java`,
~lines 98-105) reads `config.get("alias")` only to **validate** it against
`^([a-zA-Z0-9_-]+)$`; it does not remove it. The authenticated GET handler serializes
the full entity with `getDbObjectCollapsingGson()`, which unwraps `MongoKeyWrapper`
keys (confirmed: sibling keys like `server.issuer` / `client.client_id` round-trip with
their dots intact). Therefore the alias arrives at the frontend as `data.config.alias`.

Only `description` is the special case — it is extracted to a top-level column for
indexing. Do **not** mirror that for alias: a top-level `plan.alias` read would always
be `undefined`.

### KTD2 — Render conditionally; accept that the alias is authenticated-only

The public projection `PublicPlan`
(`src/main/java/net/openid/conformance/info/PublicPlan.java`) deliberately omits the
`config` field entirely (it also carries `client.client_secret`). So on public/anonymous
plan-detail views (`?public=true`), `config` — and thus the alias — is absent. The row
is rendered only when `plan.config?.alias` is a non-empty string, so it:
- shows on authenticated owner/admin views where the alias was set,
- is hidden when the user left the alias blank (dynamic-registration plans get a random
  per-instance alias, so the plan-level value is legitimately empty),
- is hidden on public views, with no extra guarding.

Exposing the alias publicly would require a backend change to `PublicPlan` and is **out
of scope** (see Scope Boundaries). This is acceptable: the alias is user-private plan
metadata, and the graceful-degradation behavior matches the existing security posture.

### KTD3 — Render the value as a `.mono` code chip, in the metadata grid

The alias is a URL-safe token (`[a-zA-Z0-9_-]+`), not prose. Render it like the Plan ID:
a `<span class="mono">` inside a `<dd>`, as a new `<dt>/<dd>` pair in the existing
`<dl class="planMeta">`. This reuses the established grid layout and `.mono` styling with
no CSS changes. Place it **after the Variant row and before the Plan ID row**, so the
user-specified logical name sits adjacent to the system-generated Plan ID for easy
comparison. (Placement is a defensible default; the implementer may reorder within the
grid if review prefers another slot — it does not affect the data path or tests beyond
DOM order.)

### KTD4 — Component reads only `config.alias`, never the whole config

The authenticated response already ships the full `config` (including secrets) to the
owner/admin client today; this change introduces no new data over the wire. The component
must read only `config.alias` and must not render or log any other config key.

---

## Requirements

- **R1** — On an authenticated plan-detail view, when the plan was created with an alias,
  the header displays an "Alias" row showing that alias value. (KTD1, KTD3)
- **R2** — When the plan has no alias (blank/missing `config.alias`, or `config` absent
  as on public views), no Alias row and no empty placeholder is rendered. (KTD2)
- **R3** — The existing description lede and all current metadata rows are unchanged in
  content and order, except for the inserted Alias row. (KTD3)
- **R4** — The Alias row is reachable for tests via `data-testid="alias-row"`, matching
  the existing `{field}-row` convention (`description-row`, `owner-row`,
  `certification-row`). (KTD3)

---

## Scope Boundaries

**In scope**
- Adding the conditional Alias row to `cts-plan-header` (render + JSDoc).
- Storybook coverage (present + absent) via play-function assertions.
- E2E coverage on `plan-detail.html` (present + absent).
- Updating the shared mock plan fixtures so `config.alias` is exercised.

**Out of scope / non-goals**
- No backend changes. The alias is already in the authenticated API response.
- No change to the public plan endpoint or `PublicPlan` to expose the alias publicly.
- No change to `plans.html` (the plan **list** page) — the request is specific to
  `plan-detail.html`.

### Deferred to Follow-Up Work
- If product later wants the alias visible on public/shared plan views, that is a
  separate backend change: add `alias` (or a redacted config subset) to `PublicPlan`
  and its repository projection. Track separately; not required here.

---

## Implementation Units

### U1. Add the conditional Alias row to `cts-plan-header`

**Goal:** Surface `plan.config.alias` as a metadata row in the header, conditionally.

**Requirements:** R1, R2, R3, R4

**Dependencies:** none

**Files:**
- `src/main/resources/static/components/cts-plan-header.js` (modify)

**Approach:**
- In `render()`, between the Variant `<dt>/<dd>` pair and the Plan ID pair, add:

  ```js
  ${plan.config?.alias
    ? html`
        <dt data-testid="alias-row">Alias:</dt>
        <dd><span class="mono">${plan.config.alias}</span></dd>
      `
    : nothing}
  ```

  (Directional sketch — not a spec. Use optional chaining so a missing `config`
  on public views is safe; the falsy guard also covers `alias: ""` and `alias: null`.)
- Update the component JSDoc `@property {object} plan` line to note that `config.alias`
  is read and rendered as the Alias row. The current JSDoc enumerates expected plan keys
  (`_id`, `planName`, `variant`, `description`, ...); add `config.alias` to that list and
  to the prose summary ("...optional alias from `config.alias`...").
- No CSS change — `.planMeta` already lays out arbitrary `<dt>/<dd>` pairs and `.mono`
  already styles code chips.

**Patterns to follow:** the existing optional rows in the same file —
`certification-row` and `owner-row` use the `${cond ? html\`...\` : nothing}` idiom with
a `data-testid` on the `<dt>`. The Plan ID `<dd>` shows the `.mono` chip pattern to mirror.

**Test scenarios** (verified via U2 stories; this unit is the behavior under test):
- Alias present → an element with `data-testid="alias-row"` exists and the alias value
  text is rendered in a `.mono` chip.
- `config` present but `alias` empty string → no `alias-row` element.
- `config` absent entirely (public-view shape) → no `alias-row` element, no thrown error
  (optional chaining holds).
- Description, Variant, Plan ID, Plan Version, Started rows render unchanged when alias
  is present (no regression to sibling rows).

**Verification:** `cts-plan-header` renders an Alias row only when `config.alias` is a
non-empty string; all existing rows are intact; no console errors on the public-shape
fixture.

---

### U2. Storybook coverage for the Alias row

**Goal:** Exercise present/absent alias states with play-function assertions, per the
project convention that every CTS component carries Storybook interaction tests.

**Requirements:** R1, R2, R4

**Dependencies:** U1

**Files:**
- `src/main/resources/static/components/cts-plan-header.stories.js` (modify)
- `src/main/resources/static/components/cts-plan-detail.stories.js` (modify — add an
  alias assertion to the existing plan-header story; reuse shared fixture)
- `frontend/stories/fixtures/mock-test-data.js` (modify — add `alias` inside the
  existing `config` object of `MOCK_PLAN_DETAIL`)

**Approach:**
- In `cts-plan-header.stories.js`, give the base `PLAN` fixture a nested `config` object
  carrying an alias, e.g. `config: { alias: "my-fapi2-bank" }`. **Critical:** the alias
  must be nested under `config`, not a top-level `PLAN.alias` — the component reads
  `plan.config.alias`. A top-level key would silently never render and give a false-green
  fixture.
- Extend the `Default` play function with a step asserting the `alias-row` exists and the
  alias text renders.
- Add a `NoAlias` story rendering `{ ...PLAN, config: undefined }` (or `config: {}`) whose
  play function asserts `querySelector('[data-testid="alias-row"]')` is `null`.
- In `cts-plan-detail.stories.js` / `frontend/stories/fixtures/mock-test-data.js`, add the
  alias into the existing `config` object so the page-level story also shows the row; add
  a light assertion if that story has a play function.

**Patterns to follow:** existing stories in the same file (`AdminShowsOwner`,
`MissingStarted`) for the present/absent play-function shape; the `expect(...).toBeNull()`
absent-row idiom.

**Test scenarios:**
- `Default` (alias present): `alias-row` present; alias text visible.
- `NoAlias` (config/alias absent): `alias-row` absent.

**Verification:** `npx vitest --project=storybook --run` (or the storybook MCP
`run-story-tests`) is green for `cts-plan-header` and `cts-plan-detail`; no regressions in
sibling stories.

---

### U3. E2E coverage on `plan-detail.html`

**Goal:** Assert the Alias row renders end-to-end with the mocked plan API response, and
is absent when the alias is missing.

**Requirements:** R1, R2, R4

**Dependencies:** U1

**Files:**
- `frontend/e2e/fixtures/mock-test-data.js` (modify — add `alias` inside the existing
  `config` object of `MOCK_PLAN_DETAIL`)
- `frontend/e2e/plan-detail.spec.js` (modify)

**Approach:**
- Add `alias: "oidcc-basic-run-1"` (URL-safe, matching the backend regex) to the existing
  `config` object in `MOCK_PLAN_DETAIL` (it already holds `server.issuer`,
  `client.client_id`, `client.client_secret`).
- In the existing "loads and renders plan info with modules (R28)" test, add an assertion
  that `#planDetailHeader` contains the alias value (and/or `[data-testid="alias-row"]`
  has count 1).
- Add a new test that routes `/api/plan/plan-abc-123` to a fixture variant with
  `config.alias` removed (or `config` omitted) and asserts
  `header.locator('[data-testid="alias-row"]')` has count `0`. Follow the spec's existing
  route-setup order (`setupFailFast` first, then specific routes before `goto`).

**Patterns to follow:** the existing `#planDetailHeader` `toContainText` assertions in
`plan-detail.spec.js`; the route-override pattern used by other tests in the file that
fulfill `/api/plan/plan-abc-123` with a modified fixture.

**Test scenarios:**
- Default fixture (alias present): header contains the alias value; one `alias-row`.
- Alias-removed fixture: zero `alias-row` elements; page renders without error.

**Verification:** `cd frontend && ./node_modules/.bin/playwright test e2e/plan-detail.spec.js`
passes; the rest of the e2e baseline is unaffected.

---

## Risks & Dependencies

- **Wrong data path (highest-likelihood bug).** Reading `plan.alias` instead of
  `plan.config.alias` produces a silently-never-rendered row that nonetheless passes a
  badly-built fixture if that fixture also sets a top-level `alias`. Mitigation: KTD1 is
  explicit, and U2/U3 fixtures put alias **only** under `config` so a wrong-path read
  fails the tests.
- **Public-view expectation mismatch.** A reviewer may expect the alias on shared/public
  plan links. KTD2 documents that this is intentionally authenticated-only; the
  follow-up backend path is captured under Deferred work.
- **No new data exposure.** The full `config` (incl. secrets) is already sent to
  authenticated clients today; KTD4 constrains the component to read only `config.alias`.

---

## Verification Strategy

1. `npm run test:ci` from `frontend/` (format/lint/type-check/lint:jsdoc/lit-analyzer)
   passes — JSDoc on `cts-plan-header` must stay valid after the `@property` edit.
2. Storybook interaction tests green for `cts-plan-header` + `cts-plan-detail` (U2).
3. `plan-detail.spec.js` e2e green, including the new absent-alias test (U3).
4. Manual spot-check (optional): an authenticated plan-detail view created with an alias
   shows the Alias row; a public view of the same plan does not.

---

## Sources & Research

- Backend storage of full config (incl. alias): `src/main/java/net/openid/conformance/info/Plan.java` constructor.
- Alias validated-but-retained in config on create: `src/main/java/net/openid/conformance/info/TestPlanApi.java` (POST `/api/plan`, ~L98-105).
- Authenticated GET serializes full config with key-unwrapping Gson: `TestPlanApi.java` `getTestPlan` (~L213-262).
- Public projection omits config: `src/main/java/net/openid/conformance/info/PublicPlan.java` (no `config` field).
- Alias config-field semantics/tooltip ("makes URLs unique ... affects the redirect url"): `src/main/resources/static/js/config-field-catalog.json`.
- Current header rendering + `{field}-row` testid convention: `src/main/resources/static/components/cts-plan-header.js`.
