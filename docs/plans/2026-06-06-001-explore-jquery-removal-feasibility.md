# jQuery Removal Feasibility — feat/redesign

**Date:** 2026-06-06
**Branch analyzed:** `feat/redesign` @ `58e11d625` (via worktree branch `explore/jquery-removal-feasibility`)
**Method:** 5 parallel census lenses + 2 adversarial refuters + empirical deletion experiment (8 agents)

## Verdict

**Removal is not just feasible — it is effectively already done.** jQuery on
`feat/redesign` is a single orphaned 89,795-byte file
(`src/main/resources/static/vendor/jquery/js/jquery.min.js`, jQuery 3.6.4)
that **nothing loads**: no script tag, no importmap entry, no dynamic
injection, no Java-served page, no Thymeleaf template, no Storybook/e2e/CI
config, no CDN fallback. The remaining work is a trailing cleanup commit:
delete one directory, remove one stale ESLint global, and trim a handful of
comments.

Both adversarial refuters (server-rendered lens, ops/build lens) failed to
refute the orphan claim. An empirical experiment deleted the file and ran the
full frontend gates: **e2e 245 passed / 0 failed, Storybook 625 passed / 0
failed, `npm run test:ci` green.** The worktree was restored clean afterward.

## How jQuery became orphaned (history lens)

1. **2023** — `cb872699b` "Remove JQuery" stripped jQuery from every page
   that didn't need it. It survived *only* because DataTables required it
   (the commit says so explicitly).
2. **Redesign, vendoring** — `30220e9b7` "serve all third-party dependencies
   locally" vendored jQuery 3.6.4 as DataTables' peer dependency (replacing
   CDN loads). This is the only commit that ever touched `vendor/jquery/`.
3. **Redesign, Phase D** — U33/U37/U38 (`2d8c078e1`, `d1ed90cdb`,
   `60a7a8c2c`) migrated the three DataTables pages (tokens, plans, logs) to
   the native Lit `cts-data-table`.
4. **Redesign, Phase E** — U43 (`e1e6097af`) deleted the `bootstrap`,
   `datatables`, and `popper` vendor dirs but its commit body explicitly
   lists jquery under "Preserves:" — a deliberate deferral, not an
   oversight. From that commit on, jquery.min.js has zero consumers.

**No existing plan schedules the deletion** — no file in `docs/plans/`
mentions jquery. This cleanup is unplanned and unclaimed.

**Contrast with master:** master still CDN-loads jQuery 3.6.4 + DataTables
1.13.4 on `logs.html`, `plans.html`, `tokens.html` and vendors neither. All
the hard removal work already happened on `feat/redesign`; master inherits it
when MR !1998 merges.

## Evidence summary by lens

### Loader hunt (adversarial) — orphaned

- Zero `<script src>` referencing jquery anywhere (static HTML, Thymeleaf
  templates, Java strings, CDN URLs).
- Every importmap lists only lit/marked/dompurify. Classic `/vendor/` loaders
  are lodash, clipboard, qrcode only.
- The only dynamic `createElement('script')` in app code loads Monaco's AMD
  loader (`cts-json-editor.js:103-135`).
- Monaco's `amd={jQuery:!0}` strings in `vendor/monaco-editor/vs/loader.js`
  are an internal AMD-detection flag, not a dependency.

### Runtime usage — zero executable jQuery

- Zero hits for `jQuery(`, `$.ajax`, `$.fn`, `.DataTable(`, `window.$`, etc.
- The single `$(` token in app code is JSDoc prose (`cts-button.js:297`).
  `fapi.ui.js:255`'s `$` is a regex anchor. Shell `$(...)` in
  `frontend/scripts/*.sh` are command substitutions.

### Server-rendered refuter — NOT REFUTED

- All 11 Thymeleaf templates outside `static/` (implicitCallback, error,
  sessionVerify, formPostResponseMode, oidccFrontChannelLogout,
  self-contained-export, …) contain zero jquery references. The only external
  scripts are Popper + Bootstrap 5.3.3 from jsdelivr in
  `implicitCallback.html:41-42` — Bootstrap 5 has no jQuery dependency.
- No HTML is built in Java string literals; both `TemplateProcessor.process`
  call sites emit JSON, not HTML.

### Ops/build refuter — NOT REFUTED

- nginx configs serve `/static/` via a generic alias; pom.xml packages
  resources by directory glob; Spring Security permits `/vendor/**` as a
  glob; `.codeclimate.yml` excludes `vendor/`. Nothing enumerates jquery.
- `LICENSE.txt` carries no jQuery attribution; GitLab dependency/license
  scanning is commented out; no GitHub Actions workflows exist in-repo.

### Backend protocol — zero backend changes needed

The DataTables *wire protocol* outlives the library, spoken by jQuery-free
code on both sides:

- **Producers:** `PaginationRequest`/`PaginationResponse` behind
  `GET /api/plan` (`TestPlanApi.java:172-194`) and `GET /api/log`
  (`LogApi.java:109-130`).
- **Consumers:** `cts-plan-list.js:579-592` and `cts-log-list.js:768-786`
  via native `fetch()`; `cts-data-table.js:644-711` reimplements the
  protocol client natively (`requestShape`: `datatables-comma-order` /
  `datatables-default`). Production pages use cts-data-table only in
  client-side mode (`cts-token-manager`, `cts-test-selector`).
- Simplifying the protocol itself (dropping `draw`, renaming fields) is a
  separate backend-touching refactor — explicitly **out of scope** per the
  standing minimal-backend-touching preference.

### Empirical deletion experiment — green

With `vendor/jquery/` deleted:

| Gate | Result |
| --- | --- |
| Playwright e2e | 245 passed, 1 intentionally-skipped, **0 failed** (37.9s); schedule-test.spec.js passed without isolation |
| Storybook vitest | **625 passed, 0 failed** (55 files, 17.4s) |
| `npm run test:ci` | exit 0 — format, lint (0 errors), type-check, jsdoc, icons, lit-analyzer, codegen all green |

No `/vendor/jquery` 404s observed. Tree restored; `git status --porcelain`
clean.

## The removal commit (complete change-list)

**Required:**

1. `git rm -r src/main/resources/static/vendor/jquery/` (one file, 88 KB).
2. `eslint.config.js` — delete the `$: "readonly"` global (line 98) and the
   `$` (jQuery) mention in the comment block (line 84). The
   `static/vendor/**` and `**/*.min.js` ignores need no edit.

**Recommended doc hygiene (same commit):**

3. `CLAUDE.md:358` — rewrite the `wrapDataTablesResponse()` bullet: the
   envelope is real but "jQuery DataTables" is stale; attribute it to
   `cts-data-table` serverSide pagination instead.
4. `components/cts-button.js:317` and `cts-button.stories.js:248` — drop the
   "jQuery delegated handlers" phrase; **keep** the surrounding light-DOM
   bubbling documentation and the `HostClickDoesNotDispatch` story (it tests
   native bubbling, not jQuery).
5. `components/AGENTS.md:61-62` — replace the `$('.deleteBtn').on(...)`
   example with vanilla `addEventListener` delegation.

**Optional / leave (keep the commit frontend-only):**

- `WebSecurityOidcLoginConfig.java:159`, `RestAuthenticationEntryPoint.java:15`
  (backend comments), `update-vendor-lit.sh:14`, `layout.css:148`,
  `docs/solutions/...request-cache-2026-06-04.md:64`,
  `cts-data-table.js:11` JSDoc (accurate as a historical description —
  keep).

**Explicit no-ops confirmed:** no npm dependency, no lockfile change, no
update-vendor-jquery.sh to retire, no pom/CI/nginx/Docker/devenv/security
edits, no LICENSE edit.

## Risks

Effectively none. The only theoretical exposure: the file is HTTP-reachable
today by typing its URL (`/vendor/**` is permitAll), so an *external* script
hotlinking jQuery from a conformance-suite deployment would break — but
nothing in this repo, its templates, or its test harnesses does that, and
hotlinking was never a supported contract. `../conformance-suite-private`
test configs were out of audit scope (external user input, not app code).

## Recommendation

Ship the removal as one small commit on `feat/redesign` (rides MR !1998, per
the established workflow — no separate PR). Estimated size: ~5 files, minutes
of work, fully covered by existing green gates.

---

# Companion audit: all other vendored libraries (2026-06-06)

Follow-up question: are there more orphans like jQuery? A second 12-agent
workflow audited every remaining `vendor/` library (one adversarial auditor
per lib, then an empirical deletion experiment for the orphans).

## Classification table

| Library | Status | Size | Disposition |
| --- | --- | --- | --- |
| `jquery` | **orphaned** | 88 KB | delete (prior audit) |
| `chroma` | **orphaned** (high) | 40 KB | delete — zero refs; legacy consumer (log-block text-contrast in old log-detail.html) deleted with the v2 rewrite |
| `prettify` | **orphaned** (high) | 20 KB | delete — never loaded; only vestigial `pre.prettyprint` CSS class remains (`layout.css:702`, export templates) |
| `randomcolor` | **orphaned** (high) | 12 KB | delete — sole consumer (per-block log colors) removed by log-block de-collapse refactor; plan doc explicitly says "not resurrected" |
| `clipboard` (ClipboardJS) | **loaded-but-unused** (high) | 12 KB | removable — loaded only by `log-detail.html:40` but `new ClipboardJS` appears nowhere; all copy buttons use `navigator.clipboard`. Remove tag + delete dead `js/cts-clipboard-resolver.js` + trim stale comments + e2e spy cleanup |
| `lodash` | **mixed** (high) | 72 KB | partial — `logs.html:17` tag is dead (nothing on that page reaches `_.`); 4 pages still genuinely use it (`fapi.ui.js` `_.template`/`_.escape`/`_.isObject`, inline `_.each`/`_.keyBy`/etc. on schedule-test/running-test/upload). Full removal requires de-lodashing fapi.ui.js + 3 pages |
| `qrcode` | used | 20 KB | keep — `log-detail.js:514-532` renders wallet QR codes; flag set by 4 wallet test families (VP-ID2/ID3/VCI/VP1-Final) |
| `marked` | used | 48 KB | keep — markdown pipeline (`format-description.js`) for test descriptions on 4 pages; e2e-guarded |
| `dompurify` | used | 88 KB | keep — the XSS boundary for `unsafeHTML`; paired with marked |
| `lit` | used | 32 KB | keep — core framework, 100 component modules import it |
| `monaco-editor` | used | 4.1 MB | keep — JSON editor on schedule-test + log-detail drawer; R12 e2e spec |
| `coolicons` | used | 1.7 MB | keep — sole icon system (442 SVGs), CI-enforced via lint:icons |

`datatables`, `bootstrap`, `popper`: already deleted by U43 (`e1e6097af`).

## Empirical verification

chroma + prettify + randomcolor were deleted together and all gates re-run:
**e2e 245 passed / 0 failed, Storybook 625 passed / 0 failed, `test:ci` exit
0** — every suite matched baseline exactly, zero deletion-related 404s. Tree
restored clean.

## Cleanup tiers

1. **Tier 1 — pure deletions (zero risk, empirically verified):** jquery,
   chroma, prettify, randomcolor. 160 KB of dead vendored code. Side-edits:
   eslint `$` global, comment trims (jQuery section above).
2. **Tier 2 — one-page edits (low risk):** clipboard (remove
   `log-detail.html:40-41`, delete `js/cts-clipboard-resolver.js`, stale
   comment trims in `cts-copy-flash.js:21`/`cts-button.js:316`, optional
   `clipboard.spec.js` spy cleanup); lodash tag on `logs.html:17`
   (loaded-but-unused; `_` is only referenced lazily inside fapi.ui.js
   function bodies that logs.html never calls).
3. **Tier 3 — small refactor (separate effort):** full lodash removal.
   Replace `_.template` (21 EJS-style templates in `fapi.ui.js:9-143`),
   `_.escape` ×3, `_.isObject`/`_.isArray`, and ~12 inline `_.*` calls
   across schedule-test/running-test/upload with native equivalents
   (`Object.fromEntries`, `Array.prototype.forEach/some`, etc.). Then drop
   the remaining 4 script tags, the vendor dir, and the `_` eslint global.

## Outcome (2026-06-06)

Tiers 1 + 2 shipped on `explore/jquery-removal-feasibility` as four atomic
commits, each gate-verified before committing:

1. `0f8c9314e` — chore(vendor): delete orphaned jquery directory
2. `b671f7ce0` — chore(vendor): delete orphaned chroma, prettify,
   randomcolor directories
3. `c17682e31` — chore(vendor): remove unused ClipboardJS and its dead
   resolver glue
4. `1dc58da29` — chore(logs): drop the unused lodash script tag

Tier 3 (full lodash removal) remains open as a follow-up refactor.
