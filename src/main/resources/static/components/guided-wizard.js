/**
 * Guided-mode controller for schedule-test.html.
 *
 * The page hosts two independent state islands — the guided journey
 * (#guidedIsland) and the classic advanced surface (#scheduleTestPage) —
 * swapped by a persistent Guided | Advanced toggle. This module owns:
 *
 * - `resolveMode()` — the pure mode-resolution ladder (R9). Deep-link params
 *   that carry concrete config payloads only the advanced surface can render
 *   (`edit-plan`, `edit-test`, `configJson`, `test_plan`) outrank everything,
 *   so share links keep working for guided-defaulted users. The guided
 *   recovery record outranks `wizard_preset` (a failed create's restore
 *   beats a fresh replay), which outranks the stored preference, which
 *   outranks the guided default.
 * - `bootGuidedMode()` — applies the resolved mode to the islands, wires the
 *   header toggle (aria-pressed, button-based), persists explicit switches
 *   to localStorage under `oidf-guided-mode`, and moves focus to the revealed
 *   island. Switching is non-destructive in both directions: it only flips
 *   visibility — neither island's state is reset (R8).
 *
 * The page boots this controller from a small inline module script and
 * resolves `window.__guidedModeReady` with the returned controller; the
 * legacy inline init chain awaits that promise and skips the advanced
 * hydration steps (`applyConfigPreset`, `restoreEditPlanVariants`,
 * `applyConfigJsonParam`) when the boot decision is guided — without the
 * short-circuit those steps would mutate the hidden advanced island and
 * `currentConfig` would diverge from what guided shows.
 *
 * Storage-unavailable environments fall back to the guided default without
 * persisting (R7) — `tryGetStorage` returns null and every storage access
 * no-ops.
 */

/** localStorage key for the persistent mode preference (MR !2029 parity). */
export const MODE_STORAGE_KEY = "oidf-guided-mode";

/**
 * sessionStorage key for the guided create-failure recovery record. Written
 * before the guided POST /api/plan, cleared on success; its presence makes a
 * reload re-enter guided at the config step (R5).
 */
export const RECOVERY_STORAGE_KEY = "oidf-guided-recovery";

/**
 * Probe whether a browser storage area is usable (same probe as the page's
 * inline `tryGetStorage` — private browsing or storage-disabled environments
 * throw on any access, including reading `window[type]`).
 *
 * @param {"localStorage"|"sessionStorage"} type
 * @returns {Storage|null}
 */
export function tryGetStorage(type) {
  try {
    const storage = window[type];
    const probe = "__storage_test__";
    storage.setItem(probe, probe);
    storage.removeItem(probe);
    return storage;
  } catch {
    return null;
  }
}

/**
 * @typedef {object} ModeDecision
 * @property {"guided"|"advanced"} mode
 * @property {"edit-plan"|"edit-test"|"configJson"|"test_plan"|"recovery"|"wizard_preset"|"preference"|"default"} source -
 *   Which ladder slot decided the mode. Callers branch on this for entry
 *   behavior (e.g. `recovery` re-enters guided at the config step while
 *   `wizard_preset` replays answers).
 */

/**
 * The R9 mode-resolution ladder. Pure: no DOM, no storage access — callers
 * supply the URL params, the stored preference, and whether a guided
 * recovery record exists.
 *
 * Empty-string params count as absent, matching the page's existing
 * truthiness checks (`applyConfigPreset`, `applyConfigJsonParam`, and the
 * legacy `?edit-plan=` head redirect all skip empty values).
 *
 * @param {object} input
 * @param {URLSearchParams} input.params - Current URL query params.
 * @param {string|null} [input.storedMode] - Raw `oidf-guided-mode` value.
 * @param {boolean} [input.hasRecoveryRecord] - Guided recovery record present.
 * @returns {ModeDecision}
 */
export function resolveMode({ params, storedMode = null, hasRecoveryRecord = false }) {
  if (params.get("edit-plan")) return { mode: "advanced", source: "edit-plan" };
  if (params.get("edit-test")) return { mode: "advanced", source: "edit-test" };
  if (params.get("configJson")) return { mode: "advanced", source: "configJson" };
  if (params.get("test_plan")) return { mode: "advanced", source: "test_plan" };
  if (hasRecoveryRecord) return { mode: "guided", source: "recovery" };
  if (params.get("wizard_preset")) return { mode: "guided", source: "wizard_preset" };
  if (storedMode === "advanced") return { mode: "advanced", source: "preference" };
  if (storedMode === "guided") return { mode: "guided", source: "preference" };
  return { mode: "guided", source: "default" };
}

/**
 * @typedef {object} GuidedModeController
 * @property {"guided"|"advanced"} initialMode - The boot-time decision. The
 *   page's init chain uses this (not the live mode) to decide whether the
 *   advanced hydration steps run — URL params are fixed at load time.
 * @property {ModeDecision["source"]} source
 * @property {() => "guided"|"advanced"} getMode - The live mode.
 * @property {(mode: "guided"|"advanced", opts?: {persist?: boolean, focus?: boolean}) => void} setMode -
 *   Switch islands. Defaults to persisting + moving focus (user-driven
 *   semantics); pass opts to override.
 * @property {(fn: (mode: "guided"|"advanced") => void) => void} onModeChange -
 *   Observe switches (used by the guided→advanced prefill bridge).
 */

/**
 * Fetch a structurally-required page element, failing loud when the page
 * markup and this controller drift apart.
 *
 * @param {string} id
 * @returns {HTMLElement}
 */
function mustGet(id) {
  const el = document.getElementById(id);
  if (!el) throw new Error(`[guided-wizard] required element #${id} is missing from the page`);
  return el;
}

/**
 * Resolve the mode and wire the toggle + islands. Call once, after the DOM
 * is parsed (the page boots it from an inline module script in <body>).
 *
 * @param {object} [opts]
 * @param {URLSearchParams} [opts.params]
 * @param {Storage|null} [opts.storage] - localStorage (or test double).
 * @param {Storage|null} [opts.session] - sessionStorage (or test double).
 * @returns {GuidedModeController}
 */
export function bootGuidedMode({
  params = new URLSearchParams(window.location.search),
  storage = tryGetStorage("localStorage"),
  session = tryGetStorage("sessionStorage"),
} = {}) {
  const decision = resolveMode({
    params,
    storedMode: storage ? storage.getItem(MODE_STORAGE_KEY) : null,
    hasRecoveryRecord: !!(session && session.getItem(RECOVERY_STORAGE_KEY)),
  });

  const guidedIsland = mustGet("guidedIsland");
  const advancedIsland = mustGet("scheduleTestPage");
  const guidedBtn = mustGet("modeGuidedBtn");
  const advancedBtn = mustGet("modeAdvancedBtn");

  let mode = decision.mode;
  /** @type {Array<(mode: "guided"|"advanced") => void>} */
  const listeners = [];

  /**
   * @param {"guided"|"advanced"} next
   * @param {{persist?: boolean, focus?: boolean}} [opts]
   */
  function applyMode(next, { persist = true, focus = true } = {}) {
    mode = next;
    guidedIsland.hidden = next !== "guided";
    advancedIsland.hidden = next !== "advanced";
    guidedBtn.setAttribute("aria-pressed", String(next === "guided"));
    advancedBtn.setAttribute("aria-pressed", String(next === "advanced"));
    if (persist && storage) {
      try {
        storage.setItem(MODE_STORAGE_KEY, next);
      } catch {
        // Storage went away mid-session (quota, private mode) — the switch
        // still works for this page view, it just won't persist (R7).
      }
    }
    if (focus) {
      const target = /** @type {HTMLElement} */ (
        next === "guided" ? guidedIsland.querySelector("h1") || guidedIsland : advancedIsland
      );
      requestAnimationFrame(() => target.focus());
    }
    for (const fn of listeners) fn(next);
  }

  // Boot: apply the resolved mode without persisting (deep-link forcing must
  // not overwrite the stored preference) and without stealing focus.
  applyMode(decision.mode, { persist: false, focus: false });

  guidedBtn.addEventListener("click", () => applyMode("guided"));
  advancedBtn.addEventListener("click", () => applyMode("advanced"));

  return {
    initialMode: decision.mode,
    source: decision.source,
    getMode: () => mode,
    setMode: applyMode,
    onModeChange: (fn) => listeners.push(fn),
  };
}
