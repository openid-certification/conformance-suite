# Partner theming layer — feasibility spike

**Branch:** `feat/redesign-theming` · **Date:** 2026-06-09 · **Status:** spike (not production-ready)

The audience is partners (e.g. HelseID, open-finance ecosystems) who use the
conformance suite to provide certification services to their members. The spike
answers: *what would a theming layer look like, what does it cost, and what
should the team decide before building it for real?*

What a partner can customize in this spike:

- **Accent color** — one hex color; hover/active shades and tints are derived in
  the browser (`color-mix(in oklab, …)`). Buttons, links, focus rings, tab
  highlights, selection, card accents all follow. Type, shadows, and radii are
  deliberately **not** themeable.
- **Logo** — PNG/JPG/SVG up to 512 KB, rendered 28 px tall in the header (22 px
  on phones), width capped at 220 px. PNG and SVG can be cropped in the admin
  UI (SVG via `viewBox` math, so it stays vector). An optional "light plate"
  rescues logos that vanish on the dark chrome.
- **Pre-baked test configurations ("presets")** — named bundles of
  `{plan, variants, configuration}` (client IDs, secrets, mTLS certs, …) that
  appear as "Certification programs from <Partner>" cards on the plans home and
  prefill the schedule-test form via `schedule-test.html?preset=<id>`.
- **Attribution** — footer becomes "<Partner> conformance service — powered by
  the OpenID Foundation"; the login panel says "Certification service operated
  by <Partner>, in partnership with the OpenID Foundation".

## The two approaches

Both feed **one canonical `theme.json` schema** — that is the load-bearing
decision. The admin UI exports the exact file the self-hosting journey
consumes, so a theme can graduate from "clicked together on a hosted instance"
to "reviewed configuration in a deployment repo" without translation.

### Prototype affordances for reviewers (spike-only, remove before production)

- **Demo bar** — a thin 11px-mono strip at the top of every page
  (`js/theme-demo-bar.js`): switch between `oidf-default` / `helseid` / `verde`
  / `lumina` in one click, jump to the active theme's presets, open
  theme-admin. Disabled (with a note) on config-as-code deployments.
  Screenshots: `d1-demo-bar-default.png`, `d2-demo-bar-verde-active.png`.
- **Open write access** — `POST/DELETE /api/theme` accept any signed-in user so
  reviewers can exercise the flow without admin rights; theme-admin.html
  carries a "Spike demo mode" banner saying production would be admin-only.
- **Component preview card** on theme-admin.html — real suite components
  (primary/secondary/danger buttons, links, checkbox/radio/range, a focus-ring
  sample, pass/warn/fail status badges) re-render live as the accent is edited,
  via a scoped custom-property surface. Screenshot:
  `d3-component-preview-live.png`. Implementation gotcha worth keeping: alias
  tokens computed at `:root` (`--fg-link: var(--orange-500)`) inherit their
  already-substituted value, so a scoped preview must re-declare them.

### Approach A — self-serve UI on an OIDF-hosted instance

A signed-in user (demo mode; production: admin) opens **Theming** from the
account menu (`theme-admin.html`), picks the accent (live contrast guidance,
component preview, on-page preview), uploads and crops the logo,
pastes/validates presets, and saves. The theme is stored as a single document in
the `THEME` Mongo collection and applies to every visitor immediately.

Journey (screenshots in `./screenshots/`):

| Step | Screenshot |
|---|---|
| Baseline, OIDF branding | `a1-plans-default-oidf.png` |
| Admin account menu → "Theming" | `a2-admin-menu-theming-item.png` |
| Admin page, default state (note honest contrast report on OIDF orange) | `a3-theme-admin-default.png` |
| Accent set + live on-page preview + AA badges | `a4-accent-set-live-preview.png` |
| Logo uploaded, crop sliders over checkerboard stage | `a5-logo-uploaded-crop-panel.png` |
| Crop applied — header preview shows the real 28px render | `a6-crop-applied-header-preview.png` |
| Presets JSON validated against the live plan catalog | `a7-presets-validated.png` |
| Saved | `a8-theme-saved-toast.png` |
| Plans home: themed + partner program cards | `a9-plans-verde-themed-presets.png` |
| `?preset=` deep link prefills plan, variants, config | `a10-schedule-test-preset-prefilled.png` |
| Login page co-branded | `a11-login-verde-themed.png` |

### Approach B — configuration as code (self-hosting partner)

The partner keeps a theme directory in their deployment repo and points the
suite at it:

```
docs/theming-spike/demo-themes/helseid/
├── theme.json          # canonical schema, logo referenced by file
└── logo.svg
```

```bash
java -jar fapi-test-suite.jar --fintechlabs.theme.dir=/path/to/theme
```

The file theme **wins over** any database theme and **locks the admin UI**
read-only with an explanatory banner (`b1-theme-admin-file-managed-readonly.png`)
— infrastructure-as-code must never silently lose to a click. Writes return
`409` with the same explanation. Verified: with the Verde theme still in Mongo,
starting with `--fintechlabs.theme.dir=…/helseid` serves HelseID everywhere.

| Step | Screenshot |
|---|---|
| Admin page locked, "managed as configuration-as-code" banner | `b1-theme-admin-file-managed-readonly.png` |
| Plans home themed from file | `b2-plans-helseid-themed.png` |
| HelseID RP preset prefilled (plan + both variants + client credentials) | `b3-schedule-test-helseid-preset.png` |
| Login co-branded | `b4-login-helseid-themed.png` |
| Mobile: 22px logo cap holds | `b5-mobile-navbar-helseid-22px.png` |

### Gallery — three color schemes and logos

| Theme | Accent | Shots |
|---|---|---|
| HelseID (blue) | `#0067C5` | `b2`, `b4`, `b5` |
| Verde Open Finance (green) | `#1E7A46` | `a9`, `a10`, `a11` |
| Lumina Trust (purple) | `#5B3FA8` | `c1-plans-lumina-themed.png`, `c2-login-lumina-themed.png` |

## How it works (spike architecture)

```
fintechlabs.theme.dir ──► ThemeService (file wins, Mongo THEME doc otherwise)
                              │
        ┌─────────────────────┼───────────────────────┐
   GET /api/theme        GET /api/theme/css       GET /api/theme/logo
   (public; presets      (public; :root custom-   (public; CSP-sandboxed
    stripped for anon)    property overrides)       binary)
                              ▲
   POST/DELETE /api/theme (admin-only; 409 when file-managed)
```

- Every page head links `/api/theme/css` right after `oidf-tokens.css` — a
  static render-blocking `<link>`, so the accent is correct on first paint with
  zero JS and no flash.
- `js/theme-client.js` is the one fetch for theme *data* (memoized +
  sessionStorage-seeded, mirroring the navbar's user cache). `cts-navbar`,
  `cts-login-page`, `cts-footer`, plans.html, and schedule-test.html consume it.
- The generated CSS re-points the `--orange-*` ramp at the partner accent and
  derives shades with `color-mix(in oklab, …)`, with an optional explicit
  `brand.ramp` override per step.

New/changed surface: `theme/ThemeService.java`, `theme/ThemeApi.java`,
`WebSecurityResourceServerConfig` (public GETs + private-link allowlist),
`theme-admin.html`, `js/theme-client.js`, 9 page heads, `cts-navbar`,
`cts-login-page`, `cts-footer`, `guided-wizard.js` (mode ladder),
`schedule-test.html` (`?preset=`), `plans.html` (program cards),
`ThemeService_UnitTest`, e2e route stub.

## theme.json schema (canonical contract)

```jsonc
{
  "version": 1,
  "partner": { "name": "HelseID" },                  // required, ≤60 chars
  "brand": {
    "accent": "#0067C5",                              // required, #RRGGBB
    "ramp": { "500": "#004A8F" },                     // optional explicit shade overrides
    "logo": {
      "file": "logo.svg",                             // config-as-code form, OR:
      "data": "data:image/svg+xml;base64,…",          // UI/database form (≤512 KB)
      "alt": "HelseID",
      "plate": false                                  // light plate behind the logo
    }
  },
  "presets": [{
    "id": "helseid-rp-basic",                         // [a-z0-9-], unique
    "label": "HelseID RP certification — Basic profile",
    "description": "…",
    "planName": "oidcc-client-basic-certification-test-plan",
    "variant": { "client_registration": "static_client", "request_type": "plain_http_request" },
    "configuration": { "alias": "helseid-rp", "client": { "client_id": "…", "client_secret": "…" } }
  }]
}
```

## Recommendations on how to proceed

1. **Keep the single-schema, two-provider architecture.** It fell out almost
   for free (~1 service, 1 controller, 1 admin page) and both journeys proved
   out end-to-end. The UI's "Export theme.json" makes the hosted instance a
   *theme authoring tool* for self-hosters.
2. **Introduce semantic `--accent-*` tokens before productionizing.** The spike
   overrides the `--orange-*` ramp directly because components consume it by
   name. That works, but it is the fragile part: every new component that
   hardcodes an orange hex silently escapes theming (the login gradient and the
   focus ring already had hardcoded orange — fixed here). Renaming the ramp to
   `--accent-*` in `oidf-tokens.css` (one mechanical sweep) makes the theme
   contract explicit and lint-able.
3. **One deployment = one theme.** Resist per-hostname multi-tenant theming on
   the shared OIDF instance until there's a real driver: presets carry
   partner secrets, certification provenance must stay legible, and per-tenant
   theme resolution drags auth/tenancy into every page load. Partner-dedicated
   deployments (already the OIDF hosting model for some programs) keep the
   theming layer trivial.
4. **Treat presets as the actual product.** The color and logo are the demo
   candy; "a member clicks one card and lands in a correctly-configured test
   plan" is what HelseID is really asking for. Presets deserve their own
   iteration: server-side validation against the plan catalog (the spike
   validates client-side), secret handling (below), and a "managed
   config" story for fields members must still fill themselves.
5. **Decide certification-mark boundaries with OIDF comms.** The spike keeps
   `--oidf-orange-pure` (logo + Certified mark) un-themeable and adds mandatory
   "powered by the OpenID Foundation" attribution. Whatever ships should be
   reviewed as a brand/legal question, not just a CSS one.
6. **Production hardening checklist** (deliberately skipped in the spike):
   re-lock theme writes to admins and delete the review affordances (the demo
   bar `js/theme-demo-bar.js` + its script tags, the demo-mode banner);
   ETag/long-cache on `/api/theme/css` + `/logo` keyed on theme hash (currently
   `no-cache` on every page load); a real SVG sanitizer (the spike rejects
   `<script`/`onload=` substrings); audit log entry on theme changes; the three
   Thymeleaf templates (`implicitCallback`, `resultCaptured`, `error`) don't
   load the theme link yet; one canonical ramp derivation (the admin preview
   duplicates the color-mix expressions in JS).

## Gotchas we ran into (worth institutional memory)

- **`--status-warning` aliases `--orange-500`.** CSS custom properties resolve
  at use-time, so re-pointing the orange ramp would have repainted *warning
  badges* in partner colors. The generated CSS re-pins the status-warning trio
  to OIDF literals. Status colors are semantics, not brand — any future token
  rework should decouple them structurally.
- **Logos live on dark chrome.** The navbar and login band are near-black.
  Partner logos are usually designed for white paper; a white-on-transparent
  logo on a white "plate" is *invisible* (we shipped exactly this bug into the
  first screenshot run via a `qlmanage`-generated white-background PNG). The
  admin UI now previews the logo on a dark strip at true 28px/22px sizes —
  that preview is the single most valuable UX element on the page.
- **The focus ring was hardcoded** (`rgba(235,139,53,.35)`), so focus states
  needed an explicit derived override; same for the login band gradient. Grep
  for raw orange hexes whenever theming regresses.
- **Two Spring Security chains + a private-link `denyAll`.** Theme assets must
  be (a) public for the anonymous login page, (b) allowlisted for private-link
  users, (c) writable only by admins. Forgetting (b) would 401 the stylesheet
  for exactly the users who view shared results.
- **Spec deep links interact with guided mode.** `?preset=` had to be added to
  the guided/advanced mode-resolution ladder or guided-mode users would never
  see the prefilled advanced form.
- **`host.click()` on `<cts-button>` does not reach the Lit handler** (the
  inner `<button>` must be clicked). Bit us in automation; will bite Playwright
  authors too.
- **e2e fail-fast mocking:** every page now boots two theme requests, so the
  Playwright `setupFailFast` helper stubs the unthemed baseline centrally —
  remember this pattern for any future "every page fetches X" feature.

## Requirements the team will need to think about

- **Who may edit the theme on a hosted instance?** The spike currently opens
  writes to any signed-in user purely so reviewers can try the flow (see the
  demo-mode banner). Production needs a decision: global admins only, a
  partner-admin role, or OIDF-operated (simplest, and may be fine).
- **Secrets in presets.** Pre-baked client secrets/mTLS keys are stored
  plaintext in the theme doc and served to any *authenticated* user of that
  deployment. Right model for a partner-dedicated instance whose members are
  all part of the program — wrong for a shared instance. Needs an explicit
  decision (encrypt at rest? placeholder fields the member must fill? both?).
- **Catalog scoping.** HelseID's "a UI with only our test visible" is *not*
  implemented — presets highlight, they don't restrict. Hard-hiding plans
  touches `/api/plan/available` server-side and has certification-governance
  implications (can a partner hide a test a member is entitled to run?).
- **Logo governance.** File size, formats, animated SVG (rejected? frozen?),
  aspect-ratio extremes, accessibility of `alt` text, and whether OIDF reviews
  partner branding before it goes live on an `*.openid.net` host.
- **Accessibility responsibility.** Per the brief, accessible colors are the
  partner's responsibility — the spike's contrast meter is guidance, not a
  gate. Decide whether a hosted OIDF instance should enforce a minimum (it is
  OIDF's accessibility reputation on that hostname).
- **Versioning/migration.** `theme.json` carries `"version": 1`; define the
  compatibility policy before partners check these into their repos.
- **Certification provenance.** Logs/exports/cert packages should arguably keep
  OIDF branding even on themed deployments — certification artifacts outlive
  the portal session.

## UX-friendly choices worth keeping

- **One color in, whole ramp out** — partners supply a single accent; oklab
  color-mix derives the eight steps. The expert escape hatch (`brand.ramp`)
  exists but never confronts a casual user.
- **Honest, non-blocking contrast guidance** — the meter computes WCAG ratios
  for "white on accent" and "link shade on white", and (delightfully) reports
  that the default OIDF orange itself fails AA. Guidance + responsibility
  framing, not a hard gate.
- **Preview before save, on the real page** — the "preview this accent on the
  current page" toggle applies the derived ramp to the live admin page;
  the logo preview renders on true-size dark header strips (desktop + mobile).
  Both close the feedback loop without a deploy.
- **Crop in-place, vector-preserving for SVG** — inset sliders + checkerboard
  stage + live clip-path preview; SVG crops by `viewBox` so no rasterization.
  (Drag-handles would be nicer; sliders were the spike-sized version and are
  keyboard-accessible by default.)
- **Height is sacred, width is negotiable** — the 28px/22px header cap is
  enforced in CSS, with a generous 220px width allowance, exactly per brief.
- **The export/import bridge** — UI authoring and config-as-code are the same
  artifact, so partners can prototype on a sandbox and ship to their infra repo.
- **Presets validate against the live catalog** — unknown plan names are
  flagged before save, with the toast naming the offending preset.
- **File-managed lock with explanation** — when config-as-code owns the theme,
  the UI says so and points at `theme.json` instead of failing mysteriously.

## Running the demos locally

```bash
# Approach A (UI): plain dev profile, then visit /theme-admin.html as admin
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Approach B (config-as-code): HelseID demo theme
mvn spring-boot:run -Dspring-boot.run.profiles=dev \
  '-Dspring-boot.run.arguments=--fintechlabs.theme.dir=docs/theming-spike/demo-themes/helseid'
```

Demo bundles: `demo-themes/{helseid,verde,lumina}/` (fictional/demo artwork,
not official partner assets). `verde/logo-padded.png` is deliberately padded
for exercising the crop flow.
