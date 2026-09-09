# Static UI conventions

Conventions for the pages, components, CSS and vendored assets under `src/main/resources/static/`. Component-authoring rules for `cts-*` elements are in `components/AGENTS.md`.

After changing any file here, run the frontend gates and the Playwright E2E specs described in `frontend/AGENTS.md` (`npm run test:ci` and the relevant `frontend/e2e/*.spec.js`); Storybook tests too when a `cts-*` component or story changes.

## Icons

All icons render via `<cts-icon name="<kebab>" size="16|20|24">`. The icon library is **coolicons v4.1**, vendored as per-icon SVG files at `src/main/resources/static/vendor/coolicons/icons/{name}.svg` — one file per icon, each ~3 KB, served individually. HTTP/2 multiplexes the small parallel requests, the browser caches per URL, and only the icons actually used hit the network.

- **Usage:** `<cts-icon name="external-link" size="20">` — the `name` attribute is the filename (without `.svg`); the `size` value is one of `16` / `20` / `24` (legacy aliases `sm`/`md`/`lg` still work). Sizes track `--space-4`/`--space-5`/`--space-6` and stroke colour follows `currentColor`, so consumers do not need any additional styling for theming.
- **Finding a name (progressive disclosure):** the vendored set ships 442 icons; do **not** paste the full list into agent context or memory. Look it up on demand: `ls src/main/resources/static/vendor/coolicons/icons/` for the canonical filenames, or browse Storybook **Components/cts-icon → AllIcons** for the curated catalog. Don't guess names — `close-md` exists, `x` does not; `user-01` exists, `person-fill` does not.
- **Enforcement:** `npm run test:ci` (frontend) runs `lint:icons` (`frontend/scripts/lint-icon-names.sh`), which fails the build when any literal `cts-icon name="<value>"` references an SVG that is not vendored. The error names the file, line, and offending value, and hints `close-md`/`close-sm`/`close-lg` for the common `x`/`cross`/`close` mistakes. Dynamic / templated names (`name="${this.icon}"`, `name="<%- foo %>"`) are caught at runtime: `cts-icon.js` listens for `error` on the inner `<use>` and emits one `console.warn` per unique unresolved name per page-load.
- **Adding a new icon:** when a call site needs an icon that isn't already vendored, follow the one-shot Python snippet documented in `src/main/resources/static/vendor/coolicons/README.md` (extract one symbol from the upstream sprite into a per-icon file). Do NOT add a build step.
- **Brand glyphs (Google, GitLab):** intentionally outside `cts-icon`. coolicons does not ship brand marks. Brand SVGs are inlined as `html\`<svg ...>\`` constants in `src/main/resources/static/components/cts-login-page.js` and used only there. If a future call site needs a brand mark, follow the same colocation pattern; do NOT add brand glyphs to the coolicons set.
- **Do NOT:** hand-roll inline `<svg>` paths for icons (use `cts-icon`); reference Bootstrap Icons (`bi-*` classes — removed); construct icon classes via string concatenation (no `className = \`bi bi-\${name}\`` — create a `cts-icon` element instead).

## Badges

All status pills, label chips, and count badges render via `<cts-badge variant="<name>">`. The status palette (`pass` / `fail` / `warn` / `running` / `skip` / `neutral` / `review`; `skip` is the SKIPPED verdict only, `neutral` the grey for never-run/pending/unknown) and the utility variants (`primary` / `secondary` / `danger` / `info-subtle`) are token-routed through `oidf-tokens.css`. See `src/main/resources/static/components/cts-badge.js` and Storybook **Components/cts-badge** for the full inventory.

**Affordance rule:** every variant supports two visual states. The state must reflect whether clicking the badge does anything.

- **Read-only (default):** fill only, no border. The badge is a label for state — pass/fail/warn/running/skip status, role marker (`ADMIN`), spec requirement chip, count summary, etc. The user does not click on it to do anything.
- **Interactive:** fill + 1px inset `box-shadow` ring + hover/focus. The badge is itself a click target, or it sits inside a wrapper that has stripped its own affordance (e.g., an `<a>` with `text-decoration: none`) so the badge silhouette is what the user perceives as clickable.

**When to use which attribute:**
- **`interactive`** — visual only. Adds the ring without `role="button"`. Use when the badge sits inside an `<a>` or `<button>` whose own affordance is invisible (no underline, no hover) so the badge needs to carry the affordance signal itself. Existing example: `cts-plan-modules.js` wraps the module status pill in a no-decoration anchor to log-detail; the badge is marked `interactive` so the affordance reads.
- **`clickable`** — semantic + visual. Adds `role="button"`, `tabindex="0"`, keyboard activation, and emits `cts-badge-click`. Implies `interactive` visually — a clickable badge always renders the ring even when `interactive` is not set. Use when the badge IS the click target and is not already wrapped in an `<a>`/`<button>`/parent click handler.

**Affordance decision tree (from the badge sweep plan, `docs/plans/2026-04-27-001-feat-badge-affordance-rule-plan.md`):**
1. Is the cts-badge itself the click target? Yes → `clickable`. No → step 2.
2. Is the badge wrapped in an interactive element (`<a>`, `<button>`, parent click handler)? No → leave read-only. Yes → step 3.
3. Does the wrapper provide its own visible affordance (link underline, button background, hover state)? Yes → leave read-only (the wrapper is doing the work; adding the ring is redundant noise). No → `interactive`.

**Token deviation:** The readonly `b-rev` (Review) chip uses `var(--bg-muted)` (#F8F7F5) as its background fill. The token system does not currently define a `--status-review-bg`; if one lands in the archive, switch the fill to that token. Update the JSDoc block in `cts-badge.js` if the deviation is resolved.

- **Do NOT:** hand-roll a 1px `border` around a chip-like element to fake the affordance ring — use `cts-badge` with `interactive`/`clickable`. The component implements the ring as an inset `box-shadow` so the box-model dimensions are identical in both states; a real `border` would shift the box by 1px when toggling affordance. Do NOT add `clickable` to a badge that is already inside a clickable parent (`<a>` or `<button>`) — that nests `role="button"` inside link/button semantics and produces ambiguous keyboard activation. Do NOT use `bg-warning` / `bg-info` / `bg-info-subtle` / `border-info-subtle` / `text-info-emphasis` Bootstrap utility classes — those are removed; use the canonical variants instead.
