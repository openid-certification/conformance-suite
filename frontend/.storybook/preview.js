import { definePreview } from "@storybook/web-components-vite";
import a11yAddon from "@storybook/addon-a11y";
import { setupWorker } from "msw/browser";
import { MINIMAL_VIEWPORTS } from "storybook/viewport";

// Create a shared MSW worker instance (no default handlers — stories provide them)
const worker = setupWorker();
let started = false;

/**
 * Custom MSW loader that replaces msw-storybook-addon.
 * Stories declare handlers via parameters.msw.handlers; the loader
 * starts the worker on first run and swaps handlers per-story.
 */
async function mswLoader(context) {
  const { parameters } = context;
  const handlers = parameters?.msw?.handlers ?? [];
  const handlerArray = Array.isArray(handlers) ? handlers : Object.values(handlers).flat();

  if (!started) {
    await worker.start({ onUnhandledRequest: "bypass", quiet: true });
    started = true;
  }

  worker.resetHandlers();
  if (handlerArray.length > 0) {
    worker.use(...handlerArray);
  }
}

export default definePreview({
  // Compose the a11y addon so its axe `afterEach` runner is part of the project
  // annotations the Vitest runner actually executes. Registering the addon in
  // main.js only wires the manager panel — the CSF-factory `addons` array is
  // what surfaces the addon's preview annotations (afterEach/decorators) into
  // `npm run test-storybook`.
  addons: [a11yAddon()],
  parameters: {
    options: {
      // Foundational-first sidebar order. Entries match top-level title
      // segments verbatim (case-sensitive; note the British "Behaviour").
      // "*" catches future unlisted categories so Behaviour stays at the
      // very bottom. No `method`: story exports keep their deliberate
      // in-file order (e.g. cts-badge's Pass→Fail→Warn palette order);
      // component entries are alphabetical via glob discovery anyway.
      storySort: {
        order: ["Tokens", "Components", "Pages", "Flows", "*", "Behaviour"],
      },
    },
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    // Register a small set of viewport presets so individual stories can
    // pin themselves to "mobile1" (320×568) etc. via globals.viewport.
    // MINIMAL_VIEWPORTS is the four-preset variant (small mobile, large
    // mobile, tablet, desktop) — enough to prove responsive contracts
    // without flooding the picker with 30 device profiles.
    viewport: {
      options: MINIMAL_VIEWPORTS,
    },
    // Accessibility testing (axe-core via @storybook/addon-a11y, composed into
    // the Vitest runner through `addons: [a11yAddon()]` above). `test` gates how
    // violations surface:
    //   "off"   — skip a11y checks (still inspectable in the addon panel)
    //   "todo"  — run checks; violations are warnings, do NOT fail the run
    //   "error" — run checks; violations FAIL `npm run test-storybook`
    // Project default is "error" so every non-backlog rule is enforced on every
    // story. To park a single story's failure as review-on-fail, set
    // `a11y: { test: "todo" }` on that story (narrowest scope) with an inline
    // backlog comment. See frontend/README.md → "Accessibility testing".
    a11y: {
      test: "error",
      config: {
        // a11y review backlog — rules that currently fail across the redesign.
        // `reviewOnFail: true` downgrades each from a hard "violation" (which
        // would fail the run) to "needs review" (surfaced in the a11y panel,
        // does NOT fail the run), per the axe-core config contract. All OTHER
        // rules stay enforced everywhere, and a NEW failure of any rule not in
        // this list still fails the build. Remove an entry once its debt is
        // fixed so the rule re-arms. Counts are from the 2026-06-10 baseline
        // (`npm run test-storybook`); see docs/plans/2026-06-10-002-*.
        rules: [
          // color-contrast has TWO dispositions in one rule:
          //  - `selector` PERMANENTLY exempts the orange primary button
          //    (`.oidf-btn-primary` — the --orange-400 CTA with white text). This
          //    is a deliberate, accepted brand trade-off, not pending work — see
          //    the Bounteous "Orange You Accessible?" case study:
          //    https://www.bounteous.com/insights/2019/03/22/orange-you-accessible-mini-case-study-color-ratio/
          //    Scoping it here (vs littering stories with per-button overrides)
          //    keeps the exemption in one place; it survives backlog cleanup.
          //  - `reviewOnFail` TEMPORARILY parks the remaining contrast debt
          //    (~415 of the 2026-06-10 baseline — mostly status badges and
          //    log-card links). When that debt is fixed, drop `reviewOnFail` and
          //    keep `selector`: contrast then enforces everywhere except the
          //    deliberately-exempt primary button.
          { id: "color-contrast", selector: "*:not(.oidf-btn-primary)", reviewOnFail: true },
          { id: "aria-prohibited-attr", reviewOnFail: true }, // 58
          { id: "aria-allowed-role", reviewOnFail: true }, // 36
          { id: "label", reviewOnFail: true }, // 2
          { id: "aria-required-children", reviewOnFail: true }, // 2
          { id: "landmark-unique", reviewOnFail: true }, // 1
          { id: "image-alt", reviewOnFail: true }, // 1
          { id: "heading-order", reviewOnFail: true }, // 1
        ],
      },
    },
  },
  loaders: [mswLoader],
});
