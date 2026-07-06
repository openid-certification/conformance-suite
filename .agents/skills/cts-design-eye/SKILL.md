---
name: cts-design-eye
description: Design-eye review for CTS UI changes — conventions critique, desktop and mobile screenshots, component and a11y checks via Storybook, before presenting any UI work
---

# CTS Design-Eye Review

The judgment loop to run before presenting any change under
`src/main/resources/static/` (or the `frontend/` toolchain that serves it).
It is written for a Java-fluent, design-novice agent: it turns "does this look
right?" into a checklist a reviewer can execute. The root `AGENTS.md` UI
workflow directs every UI agent here as the review step. The skill critiques
the diff against the documented conventions, verifies the rendered result in
real pixels at desktop and mobile widths, runs the component and accessibility
tests, and emits findings as durable MR evidence. It never presents visually
unverified work as verified.

## Process

1. **Preflight (gate).** Run `scripts/ui-preflight.sh`. If the Spring app
   (8443/8080) or Storybook (6006) probes FAIL, the eyes are blind — recommend
   the `cts-ui-setup` skill to start them and stop here. The user MAY bypass;
   if they do, switch to the degraded contract in *Entry preflight* below.
2. **Scope the change.** List the touched UI surface with
   `git diff --name-only origin/master...HEAD -- src/main/resources/static/ frontend/`
   (triple-dot = only this branch's changes). Read the diff for each file with
   `git diff origin/master...HEAD -- <path>`.
3. **Conventions critique — before pixels.** Walk the *Conventions critique
   checklist* below against the diff. Cite the owning convention surface for
   each finding so critiques point at a doc, not at taste.
4. **Open the story.** For each changed `cts-*` component derive its Storybook
   id and preview URL (*Story to preview URL*) and open it.
5. **Component + a11y tests.** Run the story tests via the storybook-mcp
   `run-story-tests` tool, or `cd frontend && npm run test-storybook` as
   fallback (*Component and accessibility tests*). a11y results are findings.
6. **Real-pixel verification.** In a dedicated agent-browser session capture
   the rendered page at 1440 and 390 widths and critique it (*Real-pixel
   verification*).
7. **Consult modern-web-guidance.** For any platform-level pattern in the
   change (dialog, popover, anchor positioning, scroll effect, form, focus
   management), run the `modern-web-guidance` skill before accepting a
   hand-rolled approach (*modern-web-guidance consultation*).
8. **Self-amendment check + findings.** Confirm any deliberate deviation also
   updated its owning convention surface (*Self-amendment duty*), then emit
   findings grouped by severity (*Findings output*). Never bury the
   degraded-mode banner.

### Entry preflight and the bypass contract

- The preflight table's app and Storybook rows are the gate: a FAIL on either
  means screenshots and story tests cannot run. Recommend starting them via
  the `cts-ui-setup` skill (it owns the MongoDB + proxy + app + Storybook
  bootstrap), then re-run the preflight.
- **The user MAY bypass** the gate and ask for the review anyway. If they do,
  you are in **degraded mode** and the whole review is a static read only:
  - Prefix **every** finding in the final output with a
    `NOT visually verified` banner.
  - Never state or imply that anything rendered correctly, that a screenshot
    was captured, or that a11y/story tests passed — you have not run them.
  - End the report by recommending a verified rerun once the app and Storybook
    are up. Degraded output is a triage note, not a sign-off.

### Conventions critique checklist

Static review of the diff, before any pixel is rendered. Each item names the
surface that owns the rule (per the root `AGENTS.md` conventions map).

- **Design tokens** — no hard-coded hex, px, or radius literals in `cts-*`
  markup or page CSS; use the `var(--…)` tokens from `oidf-tokens.css`. A
  deliberate token *decision* is appended to that file's "Deliberate
  deviations" ledger, never patched into the value in place. *(owner:
  `src/main/resources/static/css/oidf-tokens.css`)*
- **Spacing rhythm** — margins/padding/gaps come from the `--space-*` ramp
  (`--space-1`=4px … `--space-12`=48px); an arbitrary `13px` or `1.15rem` is a
  finding. Radii come from `--radius-*`. *(owner: `oidf-tokens.css`)*
- **Icons** — rendered only via `<cts-icon name="<kebab>" size="16|20|24">`
  with a name that resolves to a vendored SVG under
  `vendor/coolicons/icons/`. No hand-rolled inline `<svg>`, no removed `bi-*`
  Bootstrap Icons, no string-concatenated names. Guessed names (`x`,
  `person-fill`) fail `lint:icons`. *(owner:
  `src/main/resources/static/AGENTS.md` → Icons)*
- **Badges** — `<cts-badge>` affordance must match behavior: read-only is
  fill-only (a label); `interactive` adds the ring for a badge inside an
  affordance-stripped `<a>`/`<button>`; `clickable` (adds `role="button"` +
  keyboard) is for a badge that IS the target. Walk the decision tree; never
  fake the ring with a 1px `border`, never nest `clickable` in a clickable
  parent. *(owner: `src/main/resources/static/AGENTS.md` → Badges)*
- **Callout semantics** — `<cts-alert>` is a genuine callout (info/warning/
  error message the user must read), not a decoration wrapper for arbitrary
  prominence. If a box just needs emphasis, question whether a callout is the
  right pattern before reaching for it; prefer a card, a token-styled
  container, or spacing. *(owner:
  `src/main/resources/static/components/AGENTS.md` → §4 slot-children)*
- **Component patterns** — light-DOM rendering (`createRenderRoot(){return
  this}`); state changed through **property setters**, never `classList` on
  the host; variant→class via an explicit **lookup table**, never
  `` `btn-${x}` ``; a JSDoc `@property` for every public attribute; a `play()`
  interaction test per component that asserts on the **rendered** DOM. *(owner:
  `src/main/resources/static/components/AGENTS.md` → §2, §3, §5, §6, §7)*
- **Page integration** — the `<script type="importmap">` appears **before**
  any `type="module"` script, and every `cts-*` used on the page has its own
  `<script type="module" src="/components/…">` tag. A new directive specifier
  must be added to all HTML pages + `DIRECTIVE_PROBES`. *(owner:
  `src/main/resources/static/components/AGENTS.md` → §8)*

### Story to preview URL

Stories are globbed from
`src/main/resources/static/components/**/*.stories.js`
(`frontend/.storybook/main.js`). Derive the id from the story file, not by
guessing:

- The id is `<kebab(meta.title)>--<kebab(exportName)>`. `meta.title`
  `Components/cts-badge` + export `Pass` → id `components-cts-badge--pass`.
- Docs/preview URL: `http://localhost:6006/?path=/story/<id>`
- Raw iframe (screenshot / fetch target):
  `http://localhost:6006/iframe.html?id=<id>&viewMode=story`
- **Verified live** (this recipe, against a real component):
  `http://localhost:6006/iframe.html?id=components-cts-badge--pass&viewMode=story`
  serves HTTP 200. Confirm the id resolves before relying on it.
- **Stale-index warning:** a long-running Storybook dev server can serve a
  stale story index after files change — if a new or renamed story does not
  appear, restart it (`cd frontend && npm run storybook`) rather than assuming
  the id is wrong.

### Component and accessibility tests

- **Primary:** the storybook-mcp server (`storybook-mcp` in `.mcp.json`,
  `http://localhost:6006/mcp`; Codex loads it from `.codex/config.toml`)
  exposes a `run-story-tests` tool — run it against the changed component's
  stories. It executes the `play()` interaction tests and the
  `@storybook/addon-a11y` axe checks.
- **Fallback** when the MCP server is unavailable:
  `cd frontend && npm run test-storybook`.
- Accessibility violations (axe rules: color contrast, names/roles,
  `aria-*` validity, focus order) are **findings, not suggestions** — report
  them at the severity axe assigns, do not soften them to "nice to have".

### Real-pixel verification

Load the agent-browser workflow first: `agent-browser skills get core`. Then
follow this discipline exactly.

- The HTTPS-error bypass is **daemon-global**, not per-session: agent-browser
  ignores `--ignore-https-errors` when a daemon is already running. So fence
  the critique with a daemon restart: run `agent-browser close --all`, then
  open a **new dedicated session used only for**
  `https://localhost.emobix.co.uk:8443` with `--ignore-https-errors
  --allowed-domains localhost.emobix.co.uk` (the first command starts the
  daemon with both flags). The bypass exists solely because the local dev
  proxy serves a self-signed cert for this one origin — navigate only within
  it, and finish with `agent-browser close --all` so no bypass-enabled daemon
  lingers for unrelated browsing.
- Capture at both widths, into the gitignored `tmp/screenshots/`:
  - Desktop: `set viewport 1440 900`, screenshot →
    `tmp/screenshots/<change-name>-desktop.png`
  - Mobile: `set viewport 390 844`, screenshot →
    `tmp/screenshots/<change-name>-mobile.png`
- Critique the rendered result against the checklist: spacing/alignment,
  horizontal overflow, focus rings, hover states, and the empty / error /
  loading states where the change has them. Note anything the static pass
  could not see.
- Close the session, then `agent-browser close --all`, at the end.
- **agent-browser quirks to encode in the run:** use `set viewport <w> <h>`
  (not bare `viewport`); a viewport change **renumbers element refs**, so
  re-snapshot before acting on `@eN` refs; a link click can silently fail to
  navigate — read the `href` and `open` it directly when a click does not move
  the page.

### modern-web-guidance consultation

Web platform APIs evolve faster than training weights. For any platform-level
pattern the change introduces or touches — `<dialog>`/modal, popover, anchor
positioning, scroll-driven animation or reveal, forms and validation
(`:user-valid`), focus management — run the `modern-web-guidance` skill (pinned
per the `cts-ui-setup` skill) and check the change against it **before**
accepting a hand-rolled implementation. A bespoke approach where a modern
platform primitive exists is a finding.

### Self-amendment duty

- When the change **deliberately deviates** from a documented convention, the
  same change must update the owning convention surface (per the root
  `AGENTS.md` conventions map) — superseding the conflicting guidance, never
  appending a second contradictory rule beside it. Critique the change against
  its **new** stated intent, not the pre-change doc.
- If you notice two convention surfaces that **contradict each other**, flag it
  as a finding rather than silently picking one.
- The enforcement layer (hooks, check scripts, workflow directives, the
  review-sensitive file set) is **outside** self-amendment — a change there is
  a maintainer-reviewed edit, not something this skill treats as self-amending.

### Findings output

- Summarize for the MR description, grouped by severity (blocking / important /
  minor). Make it durable, user-visible evidence: what was checked, the
  screenshot paths (`tmp/screenshots/…-desktop.png` / `-mobile.png`), the
  a11y/story-test results, and every convention deviation found with its owning
  surface.
- If the review ran in degraded mode, the `NOT visually verified` banner leads
  the report and every finding carries it — never bury it under a clean-looking
  summary, and always recommend the verified rerun.
