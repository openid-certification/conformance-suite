---
title: "feat: Reduce log-entry chrome — tertiary cURL button, clickable timestamps, anchor scroll + highlight"
type: feat
status: active
date: 2026-05-27
---

# feat: Reduce log-entry chrome — tertiary cURL button, clickable timestamps, anchor scroll + highlight

## Summary

Three coordinated changes to the log-detail stream (`cts-log-entry` / `cts-log-viewer`) to cut visual noise and fix a broken deep-link affordance:

1. Demote the per-request **cURL copy button** from `secondary` to the tertiary (`ghost`) variant so it recedes into the row.
2. Remove the **`LOG-xxxx` reference chip** from every log entry and instead make the entry **timestamp a deep-link anchor** (`<a href="#LOG-xxxx">`). Citing an entry becomes "right-click the time → Copy Link Address"; left-click jumps to the entry. The chip's dedicated click/right-click copy affordance is retired from the log stream.
3. Fix the bug where loading a URL with `#LOG-xxxx` **fails to scroll** to that entry, and additionally **highlight the targeted entry** with a fairly light background so the user can see where they landed.

---

## Problem Frame

The log-detail stream renders one row per protocol event. Each row currently carries two low-value-but-high-noise controls: a white bordered `secondary` cURL button (on request rows) and a white bordered `LOG-xxxx` reference chip (on every referenced row, in two responsive positions). In a dense stream of dozens-to-hundreds of rows, these bordered chips fight the actual content (severity, HTTP type, message) for attention.

Separately, deep links into the log (`log-detail.html?log=<testId>#LOG-0042`) are advertised by the reference chip's "copy link" behavior, but the landing experience is broken: opening such a URL frequently does **not** scroll to the entry, and even when it does there is no visual cue identifying which row was the target.

**Root cause of the scroll bug** (see `cts-log-viewer.js:614-655`): the host `id` that fragment navigation targets (`#LOG-0042`) is assigned in the child `cts-log-entry`'s `willUpdate` (`cts-log-entry.js:627-632`), not by the viewer. The viewer's one-shot `_scrollToHashIfPresent()` runs on `this.updateComplete` after the **first** successful fetch — but `this.updateComplete` resolves when the *viewer's* render settles, not when the child entries have completed their own update cycles and written their `id` attributes. So `document.getElementById("LOG-0042")` can return `null` and the scroll silently no-ops. The one-shot gate (`_initialHashScrolled`) then prevents any retry, so entries that arrive in a later poll (long-running tests) are never scrolled to either. The browser's own native fragment scroll at page load also fails because entries render asynchronously after the `/api/log` fetch, so the `#LOG-0042` element does not exist at parse time.

---

## Scope Boundaries

**In scope:**
- The cURL button variant in `cts-log-entry.js`.
- Removing the `cts-log-entry-id` chip from `cts-log-entry.js` and making the timestamp a deep-link anchor.
- Reliable scroll-to-anchor on initial load, on later-poll arrival, and on in-page hash change (timestamp click), including opening collapsed `<details>` ancestors.
- A `:target`-driven light-background highlight on the landed entry.
- Updating the affected Storybook stories and Playwright e2e specs/fixtures.

### Deferred to Follow-Up Work
- **`cts-failure-summary` keeps its `cts-log-entry-id` chip.** The failure summary is a compact jump-list where the reference chip is the *primary* navigation affordance, not stream noise. Removing it there would regress deep-linking to failures with no timestamp to replace it. The `cts-log-entry-id.js` component therefore stays in the repo (still imported by `cts-failure-summary.js`); only its usage inside `cts-log-entry.js` is removed. Revisit consistency separately if desired.
- **No new `tertiary` button variant.** "Tertiary" maps to the existing `ghost` variant (transparent fill, transparent border, subtle hover) — the design system's lowest-emphasis button. Adding a visually identical `tertiary` alias would be redundant (see KTD1).

---

## Key Technical Decisions

**KTD1 — "Tertiary" = the existing `ghost` variant.** `cts-button` exposes `primary | secondary | ghost | danger` (`cts-button.js:23-42`). `ghost` is already the transparent, borderless, lowest-emphasis button — exactly what a tertiary button is in a primary/secondary/tertiary ladder. We reuse `ghost` rather than introduce a new variant, consistent with the existing `Details`/`More` toggle which is already `ghost`.

**KTD2 — Timestamp anchor uses a relative fragment `href="#LOG-xxxx"`, not an absolute deep URL.** A relative fragment gives correct in-page anchor semantics: left-click is a pure fragment navigation (no document reload), and the browser's "Copy Link Address" resolves the fragment against the current document URL — which on `log-detail.html` already carries `?log=<testId>` — yielding the same canonical `…/log-detail.html?log=<testId>#LOG-xxxx` that the old chip built explicitly (`cts-log-entry-id.js:106-111`). This avoids the reload risk of an absolute `href` whose path/query might differ byte-for-byte from the current URL. Trade-off: the copied link depends on the current page already having `?log=`, which is always true for `log-detail.html`.

**KTD3 — Disambiguate the timestamp link's accessible name.** Two entries logged in the same second would produce links with identical visible text but different `href`s, which violates the "don't reuse an accessible name across hyperlinks with different targets" rule. The anchor gets an `aria-label` combining the reference id and the time (e.g. `"Log entry LOG-0042, 9:42:13 AM"`). This also restores, for screen-reader users, the `LOG-xxxx` identity that sighted users lose when the chip goes away.

**KTD4 — Highlight via the `:target` pseudo-class, not the `cts-flash-highlight` primitive.** The request is a "fairly light background" on the landed row — a persistent state, not a transient flash. `:target` is declarative, requires zero JS, automatically tracks the active fragment (the highlight moves when the user navigates to another entry and clears when the fragment is removed), and survives the viewer's polling re-renders. The host already carries `id="LOG-xxxx"` (light DOM, so global CSS applies). `cts-flash-highlight` (a 1600ms orange wash that dissolves to 0, `cts-flash-highlight.js`) was considered and rejected: its dissolve contradicts "background," and it would add a JS trigger where CSS suffices. Background token: `--orange-50` (#FDF1E5, the lightest brand tint, already used for `--status-warning-bg`); escalate to `--orange-100` if `--orange-50` reads too subtle in review.

**KTD5 — Scroll handler becomes idempotent-until-success and shared across triggers.** Replace the one-shot `_initialHashScrolled` gate with a "retry on each successful fetch until the target is found and scrolled once" gate, and drive the same routine from a `hashchange` listener (covering timestamp clicks). Before measuring, await the matching child entry's `updateComplete` (or defer a frame) so its host `id` is present. This fixes both the child-id timing race and the late-poll-arrival miss.

---

## High-Level Technical Design

The scroll bug is fundamentally a timing/ordering problem. The corrected sequence (KTD5):

```mermaid
sequenceDiagram
    participant URL as URL (#LOG-0042)
    participant V as cts-log-viewer
    participant E as cts-log-entry (host)
    participant Br as Browser

    Note over URL,Br: Initial load
    URL->>Br: parse, attempt native scroll to #LOG-0042
    Br-->>URL: element absent (entries not fetched yet) → no scroll
    V->>V: fetch /api/log (poll N)
    V->>E: render entries; child willUpdate sets host id="LOG-0042"
    V->>V: await viewer.updateComplete AND target child.updateComplete
    alt target host now in DOM with id set
        V->>E: open collapsed <details> ancestors
        V->>Br: scrollIntoView (honors scroll-margin-top)
        V->>V: mark scrolled (stop retrying)
        Br->>E: :target matches → light background paints
    else target not yet delivered
        V->>V: leave "scrolled" false; retry after next poll
    end

    Note over URL,Br: Later — user clicks a timestamp
    E->>Br: <a href="#LOG-0099"> → fragment nav, no reload
    Br->>V: hashchange event
    V->>E: open collapsed ancestors + scrollIntoView
    Br->>E: :target moves highlight to LOG-0099
```

---

## Implementation Units

### U1. Demote the cURL copy button to the tertiary (ghost) variant

**Goal:** The per-request cURL copy affordance recedes visually so it stops competing with row content.

**Requirements:** Change 1 ("set these buttons as tertiary to remove noise").

**Dependencies:** none.

**Files:**
- `src/main/resources/static/components/cts-log-entry.js` — the cURL `cts-button` (≈ line 695-703).
- `src/main/resources/static/components/cts-log-entry.stories.js` — cURL stories (`HttpRequestEntry`, `CopyAsCurl`) if any assert the variant/class.

**Approach:** Change the cURL button's `variant="secondary"` to `variant="ghost"`. Keep `size="xxs"`, `icon="copy"`, and the `aria-label="Copy as cURL"` / tooltip from the prior change intact. No other button changes — the `Details` toggle is already `ghost`.

**Patterns to follow:** The existing `ghost` `Details` button in the same file (`cts-log-entry.js`, `_renderMoreButton`).

**Test scenarios:**
- The cURL button renders with the ghost class (`oidf-btn-ghost`) on a request entry. (Story: `HttpRequestEntry`.)
- `CopyAsCurl` interaction still copies the curl command (variant change must not affect the click handler).

**Verification:** Storybook `cts-log-entry` request stories show a borderless, transparent cURL button; `npm run test-storybook` for `cts-log-entry` passes.

---

### U2. Replace the LOG-xxxx chip with a deep-link timestamp anchor

**Goal:** Remove the reference chip from log entries; make the timestamp the citation handle (left-click jumps, right-click → Copy Link Address).

**Requirements:** Change 2 ("LOG-xxx copy buttons are too noisy … make timestamps clickable and remove the LOG-xxx buttons").

**Dependencies:** none (independent of U1).

**Files:**
- `src/main/resources/static/components/cts-log-entry.js`:
  - Remove `import "./cts-log-entry-id.js"` (line 7) — verify no other reference remains in this file.
  - Remove the `idChip` const and both render positions (`.logIdRow`, `.logIdInline` — render block ≈ 802-820).
  - Wrap the `<cts-time>` in `.logTime` with an anchor when `referenceId` is present: `<a class="logTimeLink" href="#${referenceId}" aria-label="Log entry ${referenceId}, <time text>"><cts-time …></a>`; render bare `<cts-time>` when `referenceId` is absent (block entries, envelope-only rows). Wrap the anchor in a `cts-tooltip` (e.g. content `"Link to this entry — right-click to copy"`) for discoverability of the copy affordance.
  - Remove the now-dead `.logIdRow` / `.logIdInline` CSS in the injected style block (lines 267, 353-354, 523-524) and add subtle `.logTimeLink` styling: inherit color, no persistent underline, underline on hover, `:focus-visible` ring via `--focus-ring`. Keep `cts-time`'s `display: contents` working by making the anchor the layout box inside `.logTime`.
  - Keep `willUpdate` host-id mirroring (still needed as the anchor target) and keep the `testId` property for now (used by the viewer binding; flag as possibly-unused after this change but do not remove in this unit to limit churn).
- `src/main/resources/static/components/cts-log-entry.stories.js`:
  - Update stories that assert the chip: `EntryIdAtSmallLayout`, `EntryIdAtDesktopLayout`, and any `getByText("LOG-…")` / `cts-log-entry-id` queries → assert the timestamp anchor (`a.logTimeLink[href="#LOG-…"]`) instead, and assert `cts-log-entry-id` is **absent**.

**Approach:** The anchor's accessible name is set explicitly via `aria-label` (KTD3), so the visible `<time>` text stays the link content. Do not set `title` (reserved; tooltip carries the hint). The aria-label's time portion should mirror the rendered `time-of-day` text.

**Patterns to follow:** The removed `cts-log-entry-id.js` `_buildDeepUrl` semantics (canonical `?log=<testId>#LOG-xxxx`) — replicated implicitly by the relative fragment per KTD2. `cts-time` usage already in `.logTime`.

**Test scenarios:**
- Request/response/info entry with `referenceId` set → `.logTime` contains an `<a class="logTimeLink" href="#LOG-0042">` wrapping `<cts-time>`; the anchor's `aria-label` includes `LOG-0042`.
- Entry **without** `referenceId` (e.g. block-only) → `.logTime` renders a bare `<cts-time>` with no anchor.
- No `cts-log-entry-id` element is present anywhere in a rendered entry.
- Left-click the timestamp updates `location.hash` to `#LOG-0042` (jsdom/Storybook: assert `href`; full nav asserted in e2e/U3).
- The chip's old positions (`.logIdRow`, `.logIdInline`) are gone from the DOM.

**Verification:** `cts-log-entry` stories pass with updated assertions; `cts-failure-summary` stories still pass (it imports its own `cts-log-entry-id`); no console warning about a missing `cts-log-entry-id` element.

---

### U3. Make scroll-to-anchor reliable on load, late arrival, and hash change

**Goal:** Opening `…#LOG-xxxx` (or clicking a timestamp) reliably scrolls the entry into view, opening any collapsed block first.

**Requirements:** Change 3 ("fix a bug where the page doesn't scroll to that anchor … should scroll to it").

**Dependencies:** U2 (timestamp clicks are the in-page hash-change source; `:target`/anchor id already present via `willUpdate`).

**Files:**
- `src/main/resources/static/components/cts-log-viewer.js`:
  - Replace the one-shot `_initialHashScrolled` gate (lines 618-621) with retry-until-success: on every successful fetch, if the hash matches `^#LOG-\d+$` and we have not yet scrolled, attempt the scroll; only set the "scrolled" flag when the target was found and scrolled.
  - In `_scrollToHashIfPresent()` (lines 641-655): before lookup, await the matching child `cts-log-entry`'s `updateComplete` (or defer one animation frame) so the host `id` is written; keep the `<details>`-ancestor-opening loop; keep `scrollIntoView({ behavior: "smooth", block: "start" })`.
  - Add a `hashchange` window listener (registered in `connectedCallback`, removed in `disconnectedCallback`) that runs the same open-ancestors-then-scroll routine — covering timestamp clicks after initial render. Reset the "scrolled" flag on hash change so a new target is honored.
- `src/main/resources/static/components/cts-log-viewer.stories.js` — extend `InitialLoadHashScroll`; add late-arrival and hash-change stories.

**Approach:** Factor the open-ancestors + scrollIntoView body into one private method called by both the post-fetch path and the `hashchange` handler. Guard for `typeof window === "undefined"` (SSR/test) as the existing code does. No behavior change for the out-of-range hash (graceful no-op).

**Execution note:** Reproduce the failure first — a story/e2e where the hash is present at mount and the target entry is in the first fetch should currently fail to scroll (or be racy); make it deterministically pass.

**Patterns to follow:** Existing `_scrollToHashIfPresent` structure; `log-detail.js` `handleScrollToEntry` (lines 930-948) which already opens `<details>` ancestors then `scrollIntoView` — mirror its ancestor-walk.

**Test scenarios:**
- *Initial load, target in first fetch:* hash `#LOG-0002` at mount → after fetch+render the matching host's `scrollIntoView` is invoked (spy) / it is the scroll target.
- *Late arrival:* target not in poll 1 but present in poll 2 → scroll fires after poll 2, not before.
- *Collapsed block:* target inside a collapsed `<details>` → the `<details>` is opened (`open === true`) before scroll.
- *Hash change after load:* dispatch `hashchange` to `#LOG-0003` (simulating a timestamp click) → ancestors opened + `scrollIntoView` on LOG-0003.
- *Out-of-range / malformed hash:* `#LOG-9999`, `#nope` → no throw, no scroll.

**Verification:** `cts-log-viewer` stories (existing + new) pass; manual: load `log-detail.html?log=<id>#LOG-00NN` deep in a long log and confirm it lands on the row below the sticky bar.

---

### U4. Highlight the targeted entry with a light background

**Goal:** The entry the user landed on is visually marked with a fairly light background that tracks the active fragment.

**Requirements:** Change 3 ("also highlight the log item with a fairly light background").

**Dependencies:** U2 (host `id` is the `:target` hook — already present; U2 establishes timestamp-driven fragment changes), U3 (scroll brings it into view).

**Files:**
- `src/main/resources/static/components/cts-log-entry.js` injected styles (or `src/main/resources/static/css/layout.css` if global scoping is cleaner) — add a `:target` rule.
- `src/main/resources/static/components/cts-log-viewer.stories.js` or `cts-log-entry.stories.js` — a highlight story.

**Approach:** Add `cts-log-entry:target .logItem { background: var(--orange-50); transition: background var(--dur-1) var(--ease-standard); }` (token per KTD4). Ensure specificity beats the global `.logItem:hover` repaint noted in prior work (memory: `.logItem:hover` bleeds near-white #f6fefe) — qualify the selector and/or place it after the hover rule so the target highlight wins; verify dark-surfaced rows (`is-fail`/`is-warn`) still read correctly with the wash, overriding per-state backgrounds only as needed. The highlight is persistent while the fragment matches and clears automatically when the user navigates away — no JS, no teardown.

**Patterns to follow:** Token usage from `oidf-tokens.css` (`--orange-50`, `--dur-1`, `--ease-standard`); the scoped `:hover` override pattern called out in prior log-item work.

**Test scenarios:**
- With `window.location.hash = "#LOG-0002"`, the `LOG-0002` entry's `.logItem` computed `background-color` equals the highlight token; sibling entries do not.
- Changing the hash to `#LOG-0003` moves the highlight (LOG-0003 highlighted, LOG-0002 not).
- Removing the hash clears the highlight from all entries.
- `is-fail` / `is-warn` rows: the highlight is visible and does not destroy the failure/warn affordance (visual assertion).

**Verification:** Storybook highlight story passes; manual deep-link landing shows a subtle tinted row; navigating between entries moves the tint.

---

## System-Wide Impact

- **`cts-log-entry-id.js` stays alive** — still imported by `cts-failure-summary.js`. Confirm no dangling imports after U2 and that failure-summary stories/e2e remain green.
- **e2e**: `frontend/e2e/` specs for `log-detail` (and `schedule-test.spec.js`, already modified on this branch) may assert on the `LOG-xxxx` chip or on anchor scroll. Update fixtures/specs as part of U2/U3. Per project memory there are known pre-existing log-detail e2e flakes on this branch — reproduce on HEAD before attributing any failure to this change.
- **Accessibility**: the timestamp gains a link role + disambiguated `aria-label`; the chip's two-mode copy (click=URL, right-click=plain) is reduced to browser-native "Copy Link Address" (URL only). Plain-`LOG-xxxx` copy is no longer offered in the stream — acceptable per the request.

---

## Risks & Dependencies

- **`:target` vs. `.logItem:hover` bleed (medium):** the documented global hover repaint can visually fight the target highlight. Mitigated in U4 by selector ordering/specificity and a per-state check on `is-fail`/`is-warn` rows.
- **Child-update timing (medium):** the scroll fix depends on the child host `id` being set. If awaiting `child.updateComplete` proves unreliable in tests, fall back to binding `id=${referenceId}` directly on the `<cts-log-entry>` in the viewer render as a deterministic alternative (keeping `willUpdate` for standalone use). Note this as the execution-time fallback.
- **Copied-link correctness (low):** relative-fragment "Copy Link Address" depends on the current URL carrying `?log=` — always true for `log-detail.html`.

---

## Test Strategy Notes

- Storybook interaction tests are the primary gate for U1, U2, U4 (all CTS components require play-function coverage per project convention).
- U3's timing/late-arrival scenarios are best expressed as `cts-log-viewer` stories that drive the mocked poll sequence; the hash-change path can be exercised by dispatching a `hashchange` event.
- Run `cts-log-entry`, `cts-log-viewer`, and `cts-failure-summary` story suites plus the `log-detail` e2e spec. Reproduce any failure on branch HEAD first (pre-existing flakes are documented in project memory).
