---
title: "feat: Live-backend story for cts-log-detail-header"
status: completed
date: 2026-06-08
type: feat
---

# feat: Live-backend story for cts-log-detail-header

## Summary

Add a Storybook story that lets a developer paste a `log-detail.html?log=<id>` URL
into the Controls (args) panel; the story extracts the `log` param, fetches
`GET /api/info/<id>` from the running CTS backend, and injects the response into
`<cts-log-detail-header>` via its `.testInfo` property. This turns Storybook into a
quick viewer for any real test log without spinning up the full page.

---

## Problem Frame

The component renders from injected `testInfo`; today its stories only feed static
fixtures. Developers debugging a specific real log have to open the full app. A
"paste the URL you're looking at" story gives the fastest path from a real test id
to the isolated component, mirroring the URL→endpoint mapping the page already does
(`js/log-detail.js`: `?log=<id>` → `GET /api/info/<id>`).

---

## Scope & Decisions

- **URL formats accepted (confirmed):** full page URL only — a `log-detail.html?log=<id>`
  URL. Parse with the `URL` API, read `searchParams.get("log")`. Reject anything
  without a `log` param with a clear inline message. (Raw id / direct API URL support
  is explicitly out of scope.)
- **Cross-origin handling (confirmed approach):** attempt a live cross-origin fetch and
  fail gracefully. **CORS does apply** and is independent of the self-signed cert:
  Storybook (`http://localhost:6006`) → backend (`https://localhost.emobix.co.uk:8443`)
  is cross-origin on scheme+host+port. The browser enforces same-origin policy
  regardless of the cert, so the request succeeds only if the backend returns
  `Access-Control-Allow-Origin` **or** the developer has trusted the cert AND the
  backend allows the origin. The story does not change backend/CORS config; it shows a
  clear error with setup guidance on failure.
- **No backend or build changes.** The story is self-contained in the existing
  `.stories.js` file and helpers.

### Deferred to Follow-Up Work
- Optional Storybook dev proxy (`/api/*` → backend) to make the request same-origin and
  sidestep CORS. Cleaner UX but couples the story to proxy config; revisit if developers
  hit CORS friction often.
- Accepting raw test ids or direct `/api/info/<id>` URLs.

---

## Key Technical Decisions

- **Use an async `render` (or a small custom element wrapper) that fetches on demand,
  not a fetch decorator.** `withMockFetch` mocks; here we want a *real* fetch keyed off
  a live arg value that changes from the Controls panel. The story render reads the
  `logUrl` arg, parses the id, fetches, and sets `.testInfo` once resolved, showing
  loading / error states in between.
- **Arg name `logUrl`, `argType` `text`**, with a sensible default placeholder
  (`https://localhost.emobix.co.uk:8443/log-detail.html?log=YO7tc06oegAVgbt`). Changing
  it in Controls re-renders and re-fetches.
- **Reuse the page's fetch shape** from `js/log-detail.js::fetchTestInfo` (non-public
  variant): `GET /api/info/<encodeURIComponent(id)>`, parse JSON, surface `body.error`
  when present, otherwise a generic `HTTP <status>` message.
- **Render absolute backend origin.** Derive the API base from the pasted URL's origin
  (`new URL(logUrl).origin + "/api/info/" + id`) so the fetch targets the same backend
  the developer is looking at, not Storybook's origin.

---

## Implementation Units

### U1. Add URL→id parse + fetch helper for the story

**Goal:** A small, testable function that turns a pasted page URL into the API URL and
the test id, with explicit error messages.

**Files:**
- `src/main/resources/static/components/cts-log-detail-header.stories.js` (modify — add local helper, or)
- `frontend/stories/fixtures/helpers.js` (modify — only if the helper is reused elsewhere; default to colocating in the story file)

**Approach:**
- `parseLogUrl(logUrl)` → `{ id, apiUrl }` or throws `Error` with a UI-facing message:
  - empty/blank input → "Paste a log-detail.html?log=<id> URL."
  - not a valid URL → "That doesn't look like a URL."
  - no `log` param → "URL is missing the ?log=<id> parameter."
- `apiUrl = new URL(logUrl).origin + "/api/info/" + encodeURIComponent(id)`.

**Patterns to follow:** `js/log-detail.js::fetchTestInfo` for the fetch/error shape;
existing helper style in `frontend/stories/fixtures/helpers.js`.

**Test scenarios:**
- Happy path: `…/log-detail.html?log=ABC123` → `id="ABC123"`, `apiUrl` ends with `/api/info/ABC123` and carries the URL's origin.
- Edge: id needing encoding (e.g. contains `/` or space) is `encodeURIComponent`-escaped in `apiUrl`.
- Error: blank string throws the "Paste a …" message.
- Error: `"not a url"` throws the invalid-URL message.
- Error: `…/log-detail.html` (no query) throws the missing-param message.

### U2. Add the live-backend story

**Goal:** A Storybook story exposing a `logUrl` control that fetches and injects `testInfo`.

**Dependencies:** U1.

**Files:**
- `src/main/resources/static/components/cts-log-detail-header.stories.js` (modify)

**Approach:**
- New export e.g. `LiveBackend` with `argTypes: { logUrl: { control: "text" } }` and a
  default `args.logUrl`.
- `render({ logUrl })` returns a wrapper that shows: loading state while fetching,
  the populated `<cts-log-detail-header .testInfo=${data}>` on success, and a readable
  error panel (with the CORS/cert setup hint) on failure.
- On fetch failure, the error panel text should name the two gates: "Trust the backend
  cert (visit the URL once) and ensure the backend allows this origin (CORS), or run
  Storybook behind a proxy."
- Keep the existing fixture-based stories untouched.

**Patterns to follow:** existing `render: () => html\`…\`` stories in the same file;
`storybook/test` is only needed if a `play` assertion is added (see U3).

**Test scenarios:**
- `Test expectation: none for the render path itself` — a live fetch can't run
  deterministically in CI. The deterministic coverage lives in U1 (parse) and U3 (mocked play).

### U3. Optional play-function smoke test with a mocked fetch

**Goal:** Prove the story wires parse→fetch→inject correctly without a live backend.

**Dependencies:** U2.

**Files:**
- `src/main/resources/static/components/cts-log-detail-header.stories.js` (modify)

**Approach:**
- A separate story (or the same one under a mocked decorator) using `withMockFetch`
  from `frontend/stories/fixtures/helpers.js` to intercept `/api/info/` and return a
  fixture (`MOCK_TEST_STATUS`), with a `play` that asserts the header renders the
  fixture's `testName`/status.
- This keeps the interactive `LiveBackend` story real while giving CI a deterministic check.

**Patterns to follow:** `cts-log-viewer.stories.js` `withMockFetch` + `play` pattern;
the `MOCK_TEST_STATUS` fixture already imported in this story file.

**Test scenarios:**
- Covers wiring: with `/api/info/` mocked to `MOCK_TEST_STATUS`, the rendered header
  shows the fixture's test name and status (assert via `play`/`expect`).
- Error path: mock `/api/info/` to `status: 500`; the story renders the error panel,
  not a half-populated header.

**Verification:** `cd frontend && npm run test:e2e`-adjacent storybook run — i.e.
`npx vitest --project=storybook --run` passes for this component's stories; the
`LiveBackend` story renders and, against a running backend with a trusted cert, loads a
real log.

---

## Risks & Notes

- **CORS / cert friction is the main UX risk.** Without backend CORS headers, the live
  fetch fails cross-origin; the graceful error path is what makes this acceptable. If it
  proves annoying, pick up the proxy follow-up.
- **No new dependencies; no backend changes.** Lowest-risk surface — one stories file
  plus a tiny parse helper.
