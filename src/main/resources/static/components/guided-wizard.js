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
 * - `startGuidedJourney()` — the guided certification journey (ported from
 *   prototype 4 on feat/redesign-wizard): ecosystem cards → tree questions →
 *   upfront multi-plan bundle checklist → plain-language review → config.
 *   Runs on the live catalog (`FAPI_UI.availablePlans`); a tree leaf whose
 *   plan is absent from the catalog renders a dead-end with the Advanced
 *   escape hatch instead of an empty config form (R4). The guided→advanced
 *   bridge offers — never forces — to prefill the advanced island from a
 *   resolved journey (R8); declines are remembered per plan.
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

import { GUIDED_WIZARD_TREE } from "./guided-wizard-tree.js";
import { buildConfigFormSchema, computeHiddenFields } from "./config-form-adapter.js";

/** localStorage key for the persistent mode preference (MR !2029 parity). */
export const MODE_STORAGE_KEY = "oidf-guided-mode";

/**
 * sessionStorage key for the guided create-failure recovery record. Written
 * before the guided POST /api/plan, cleared on success; its presence makes a
 * reload re-enter guided at the config step (R5).
 */
export const RECOVERY_STORAGE_KEY = "oidf-guided-recovery";

/**
 * sessionStorage key for the post-create handoff record (MR !2029 parity).
 * Written after a successful guided create whose tree node still has
 * remaining `also_required` siblings; consumed once by plan-detail.html to
 * render the "finish your certification" banner (R12). The
 * `completedPlanNames` ledger inside it terminates the sibling loop (R14).
 */
export const HANDOFF_STORAGE_KEY = "oidf-also-required";

/**
 * Probe whether a browser storage area is usable (private browsing or
 * storage-disabled environments throw on any access, including reading
 * `window[type]`).
 *
 * Keep in sync with the inline `tryGetStorage` in schedule-test.html —
 * that classic script cannot import this module, so the probe is
 * deliberately duplicated across the boundary.
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
 * @property {(deps: GuidedJourneyDeps) => void} [startJourney] - Late-bound
 *   by the page's boot module (the journey needs the live catalog, which
 *   the init chain loads after boot): calls `startGuidedJourney` with this
 *   controller.
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

// ════════════════════════════════════════════════════════════════════
//  Guided journey — presentation maps (ported from prototype 4)
// ════════════════════════════════════════════════════════════════════

/**
 * Friendly value labels for variant values (synthesized — the raw enum
 * values are developer-facing; the journey speaks plainly).
 * @type {Record<string, Record<string, string>>}
 */
const VALUE_LABELS = {
  client_auth_type: { private_key_jwt: "Private Key JWT", mtls: "Mutual TLS (mTLS)" },
  fapi_profile: {
    plain_fapi: "Plain FAPI",
    openbanking_uk: "Open Banking UK",
    openbanking_brazil: "Open Finance Brazil",
    openinsurance_brazil: "Open Insurance Brazil",
    openbanking_ksa: "Open Banking KSA (SAMA v1)",
    ksa: "SAMA (KSA v2)",
    consumerdataright_au: "CDR Australia",
    connectid_au: "ConnectID Australia",
    cbuae: "CBUAE (UAE)",
  },
  fapi_ciba_profile: {
    plain_fapi: "Plain FAPI",
    openbanking_uk: "Open Banking UK",
    openbanking_brazil: "Open Finance Brazil",
    connectid_au: "ConnectID Australia",
  },
  fapi_auth_request_method: {
    plain: "By value (plain)",
    by_value: "By value",
    pushed: "Pushed (PAR)",
  },
  fapi_request_method: { unsigned: "Unsigned", signed_non_repudiation: "Signed (non-repudiation)" },
  fapi_response_mode: { plain_response: "Plain response", jarm: "JARM" },
  fapi_client_type: { oidc: "OpenID Connect", plain_oauth: "Plain OAuth2" },
  openid: { openid_connect: "OpenID Connect", plain_oauth: "Plain OAuth2" },
  sender_constrain: { mtls: "Mutual TLS (mTLS)", dpop: "DPoP" },
  authorization_request_type: { simple: "Simple", rar: "Rich Authorization Requests (RAR)" },
  ciba_mode: { ping: "Ping", poll: "Poll" },
  client_registration: {
    static_client: "Static (pre-registered) client",
    dynamic_client: "Dynamic client registration",
  },
  brazil_client_scope: {
    "openid-accounts": "openid + accounts",
    "openid-payments": "openid + payments",
  },
};

/**
 * Param-level display names for synthetic params (not declared on the plan)
 * that still need a label in the review table.
 * @type {Record<string, {displayName: string, description: string}>}
 */
const PARAM_DISPLAY = {
  brazil_client_scope: {
    displayName: "Brazil Client Scope",
    description: "Which Open Finance Brazil scope this client requests.",
  },
};

/** @type {Record<string, string>} One-line ecosystem blurbs for the picker cards. */
const ECOSYSTEM_DESC = {
  open_finance_brazil: "Open Finance Brazil — banking APIs (FAPI1 + DCR).",
  open_insurance_brazil: "Open Insurance Brazil — insurance APIs on FAPI1.",
  open_banking_uk: "UK Open Banking — FAPI1 Advanced.",
  cdr_au: "Australian Consumer Data Right — FAPI1 Advanced.",
  connectid_au: "ConnectID Australia — FAPI2 Message Signing.",
  cbuae: "Central Bank of the UAE — FAPI2 Message Signing.",
  ksa: "Saudi Central Bank (SAMA) — FAPI1 (v1) or FAPI2 (v2).",
};

// ════════════════════════════════════════════════════════════════════
//  Guided journey — pure helpers
// ════════════════════════════════════════════════════════════════════

/** @type {Record<string, string>} */
const ESC_MAP = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };

/**
 * HTML-escape for innerHTML-rendered journey content.
 * @param {unknown} s
 * @returns {string}
 */
function esc(s) {
  return String(s).replace(/[&<>"']/g, (c) => ESC_MAP[c]);
}

/**
 * Flag emoji are two regional-indicator codepoints; a naive single-codepoint
 * match leaves a half-flag that renders as a letter. Split on the first ASCII
 * space instead — the label always reads "<flag> <name>".
 * @param {string} label
 * @returns {{flag: string, rest: string}}
 */
function splitFlag(label) {
  const sp = label.indexOf(" ");
  if (sp === -1) return { flag: "", rest: label };
  return { flag: label.slice(0, sp), rest: label.slice(sp + 1) };
}

/**
 * "client.client_id" → "Client ID"; "server.discoveryUrl" → "Discovery URL".
 * @param {string} key
 * @returns {string}
 */
function humanizeKey(key) {
  const leaf = key.includes(".") ? key.split(".").slice(1).join(".") : key;
  return (
    leaf
      .replace(/([a-z])([A-Z])/g, "$1 $2")
      .replace(/[_.]/g, " ")
      .replace(/^\w/, (c) => c.toUpperCase())
      // Acronym fixups run after capitalization so "ca"→"Ca"→"CA" lands.
      .replace(/\bId\b/gi, "ID")
      .replace(/\bUrl\b/gi, "URL")
      .replace(/\bJwks\b/gi, "JWKS")
      .replace(/\bCa\b/gi, "CA")
      .replace(/\bDpop\b/gi, "DPoP")
      .replace(/\bMtls\b/gi, "mTLS")
      .replace(/\bAcr\b/gi, "ACR")
      .trim()
  );
}

/**
 * @param {string} param
 * @param {string} value
 * @returns {string}
 */
function valueLabel(param, value) {
  return (VALUE_LABELS[param] && VALUE_LABELS[param][value]) || value;
}

/**
 * Display metadata for a variant parameter: the plan's own variantInfo when
 * declared, a synthetic PARAM_DISPLAY entry, or a humanized fallback.
 * @param {object|null} plan - PlanInfo from the live catalog.
 * @param {string} param
 * @returns {{displayName: string, description: string}}
 */
function variantDisplayInfo(plan, param) {
  const declared =
    plan && plan.variants && plan.variants[param] && plan.variants[param].variantInfo;
  if (declared) return declared;
  return PARAM_DISPLAY[param] || { displayName: humanizeKey(param), description: "" };
}

/**
 * Compress the verbose questions into a chip eyebrow.
 * @param {string} q
 * @returns {string}
 */
function shortKey(q) {
  if (/role/i.test(q)) return "Role";
  if (/version/i.test(q)) return "Spec version";
  if (/authentication/i.test(q)) return "Client auth";
  if (/type of client/i.test(q)) return "Client type";
  if (/which.*plan/i.test(q)) return "Plan";
  if (/ecosystem/i.test(q)) return "Ecosystem";
  return q.replace(/\?$/, "");
}

/**
 * Supporting copy under each question heading.
 * @param {string} stepId
 * @returns {string}
 */
function questionLede(stepId) {
  const m = /** @type {Record<string, string>} */ ({
    role: "Are you certifying the server that issues tokens, or a client that consumes them?",
    client_auth: "How does your software authenticate the client to the authorization server?",
    ksa_spec_version: "SAMA v1 maps to FAPI1 Advanced; SAMA v2 maps to FAPI2 Message Signing.",
    scope: "The kind of access this client requests determines the scope under test.",
    plan: "Each plan is one part of the full certification. We'll show the full set next.",
  });
  return m[stepId] || "Choose the option that matches your deployment.";
}

/**
 * Visible descriptions for choices that don't carry one in the tree.
 * @param {string} stepId
 * @param {{id: string}} choice
 * @returns {string}
 */
function implicitDescription(stepId, choice) {
  if (stepId === "role") {
    if (choice.id === "rp")
      return "You operate a relying party / OAuth2 client that calls a provider.";
    if (choice.id === "op")
      return "You operate an OpenID Provider / authorization server that issues tokens.";
  }
  if (stepId === "client_auth") {
    if (/pkjwt/.test(choice.id)) return "Asymmetric key client assertions (private_key_jwt).";
    if (/mtls/.test(choice.id))
      return "Mutual-TLS client authentication using client certificates.";
  }
  if (stepId === "ksa_spec_version") {
    if (choice.id === "ksav1") return "The original SAMA profile, built on FAPI1 Advanced.";
    if (choice.id === "ksav2") return "The current SAMA profile, built on FAPI2 Message Signing.";
  }
  if (stepId === "scope") {
    if (choice.id === "accounts") return "Account Information access (AISP-style).";
    if (choice.id === "payments") return "Payment Initiation client (PISP-style).";
    if (choice.id === "ciba") return "Decoupled / backchannel authentication (CIBA).";
  }
  return "";
}

/**
 * Keep monospace-y enum labels readable where they leaked into the tree.
 * @param {{label: string}} choice
 * @returns {string}
 */
function prettyChoiceLabel(choice) {
  if (choice.label === "private_key_jwt") return "Private Key JWT";
  if (choice.label === "mTLS") return "Mutual TLS (mTLS)";
  return choice.label;
}

/**
 * Normalize copy inconsistencies in the tree data.
 * @param {string} q
 * @returns {string}
 */
function normalizeQuestion(q) {
  return q.replace(/^Which version\?$/, "Which version of the specification?");
}

/**
 * Depth-first search for a choice id anywhere under a step (including steps
 * nested via `next`). Returns the choice only when it carries a result.
 *
 * @param {import("./guided-wizard-tree.js").WizardStep} step
 * @param {string} id
 * @returns {import("./guided-wizard-tree.js").WizardChoice|null}
 */
function findChoiceById(step, id) {
  for (const c of step.choices) {
    if (c.id === id && c.result) return c;
    if (c.next) {
      const r = findChoiceById(c.next, id);
      if (r) return r;
    }
  }
  return null;
}

/**
 * Filter a result's `also_required` list down to siblings that BOTH resolve
 * to a choice in the tree AND name a plan present in the live catalog (R4:
 * tree/catalog skew must never dead-end the bundle loop).
 *
 * @param {import("./guided-wizard-tree.js").WizardResult} result
 * @param {import("./guided-wizard-tree.js").WizardEcosystem} ecosystem
 * @param {Record<string, object>} availablePlans
 * @returns {Array<import("./guided-wizard-tree.js").AlsoRequired & {planName: string}>}
 */
export function filterResolvableSiblings(result, ecosystem, availablePlans) {
  const out = [];
  for (const sibling of result.also_required || []) {
    const choice = ecosystem.steps[0] ? findChoiceById(ecosystem.steps[0], sibling.id) : null;
    const planName = choice && choice.result ? choice.result.plan_name : null;
    if (planName && planName in availablePlans) {
      out.push({ ...sibling, planName });
    }
  }
  return out;
}

/**
 * Best-effort replay of a recorded answer trail against the live tree.
 * Pure. Each hop is validated; the first unresolvable hop stops the walk
 * with the valid prefix intact, so callers drop the user at the last valid
 * step instead of erroring (R13 — the tree may have changed since the trail
 * was recorded). Used by both the create-failure recovery restore (R5) and
 * the `wizard_preset` replay (R13).
 *
 * @param {string} ecosystemId
 * @param {string[]} answerIds - Choice ids in journey order.
 * @param {import("./guided-wizard-tree.js").WizardTree} [tree]
 * @returns {{ecosystem: import("./guided-wizard-tree.js").WizardEcosystem, path: JourneyAnswer[], result: import("./guided-wizard-tree.js").WizardResult|null}|null}
 *   `null` when the ecosystem id itself doesn't resolve.
 */
/**
 * @typedef {object} WizardPreset
 * @property {string} ecosystemId
 * @property {string[]} answers - Choice ids in journey order (the trail up
 *   to — not including — the answer that resolves a plan).
 * @property {string[]} completedPlanNames - The R14 ledger: plans already
 *   created for this certification, so replayed journeys stop re-offering
 *   them.
 */

/**
 * Parse + validate a raw `wizard_preset` URL param value. Pure. Returns
 * null on any malformed input (same try/parse/warn discipline as the
 * page's applyConfigJsonParam — the caller owns the console.warn).
 *
 * @param {string|null} raw - The (already URL-decoded) param value.
 * @returns {WizardPreset|null}
 */
export function decodeWizardPreset(raw) {
  if (!raw) return null;
  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return null;
  }
  if (!parsed || typeof parsed !== "object") return null;
  if (typeof parsed.ecosystemId !== "string" || !Array.isArray(parsed.answers)) return null;
  return {
    ecosystemId: parsed.ecosystemId,
    answers: parsed.answers.filter((/** @type {unknown} */ a) => typeof a === "string"),
    completedPlanNames: Array.isArray(parsed.completedPlanNames)
      ? parsed.completedPlanNames.filter((/** @type {unknown} */ p) => typeof p === "string")
      : [],
  };
}

export function replayAnswers(ecosystemId, answerIds, tree = GUIDED_WIZARD_TREE) {
  const ecosystem = tree.ecosystems.find((e) => e.id === ecosystemId) || null;
  if (!ecosystem || !ecosystem.steps[0]) return null;
  /** @type {JourneyAnswer[]} */
  const path = [];
  /** @type {import("./guided-wizard-tree.js").WizardStep|null} */
  let step = ecosystem.steps[0];
  /** @type {import("./guided-wizard-tree.js").WizardResult|null} */
  let result = null;
  for (const id of answerIds) {
    if (!step) break; // trailing ids past a leaf — ignore the rest
    const choice = step.choices.find((c) => c.id === id);
    if (!choice) break; // first unresolvable hop — stop at the last valid step
    path.push({ stepId: step.id, question: step.question, choice });
    if (choice.result) {
      result = choice.result;
      step = null;
    } else {
      step = choice.next || null;
    }
  }
  return { ecosystem, path, result };
}

// ════════════════════════════════════════════════════════════════════
//  Guided journey — controller
// ════════════════════════════════════════════════════════════════════

/**
 * @typedef {object} JourneyAnswer
 * @property {string} stepId
 * @property {string} question
 * @property {import("./guided-wizard-tree.js").WizardChoice} choice
 */

/**
 * @typedef {object} GuidedJourneyDeps
 * @property {Record<string, any>} availablePlans - FAPI_UI.availablePlans
 *   (live catalog keyed by planName).
 * @property {object|null} fieldCatalog - The page's vendored field catalog.
 * @property {() => object|undefined} [getCurrentUser] - Returns
 *   FAPI_UI.currentUser; absent/undefined means anonymous (R6).
 * @property {(planName: string, variant: Record<string, string>, config: object) => Promise<{id: string}>} [createPlan] -
 *   The page's shared create helper (POST /api/plan). Rejects with the raw
 *   fetch error / Response for normalizeCreateError.
 * @property {(error: unknown) => Promise<{code: string, message: string}>} [normalizeCreateError] -
 *   The page's shared error normalizer (same one the advanced modal uses).
 * @property {() => void} [revealPlanSelection] - The page's shared arrival
 *   cue: scroll the spec cascade into view, flash the selection group once
 *   the scroll settles, focus the first variant select. Called after a
 *   bridge prefill so accepting reads exactly like picking the plan from
 *   the search list.
 * @property {Storage|null} [session] - sessionStorage (or test double) for
 *   the recovery + handoff records.
 */

/**
 * Start the guided journey inside #guidedIsland. Call once, after the live
 * catalog has loaded (the page's init chain provides it). The journey renders
 * eagerly even when the island is hidden so a later switch to guided shows
 * the in-progress state (two-island independence, R8).
 *
 * @param {GuidedModeController} modeController
 * @param {GuidedJourneyDeps} deps
 */
export function startGuidedJourney(modeController, deps) {
  const stage = mustGet("guidedStage");
  const trailEl = mustGet("guidedTrail");
  const progressEl = mustGet("guidedProgress");
  const liveEl = mustGet("guidedLiveRegion");
  const actionBar = mustGet("guidedStageActions");
  const actionBarContent = mustGet("guidedActionsContent");
  const session = deps.session !== undefined ? deps.session : tryGetStorage("sessionStorage");

  /**
   * Journey state. A journey is: ecosystem → tree path → [bundle preview] →
   * review → config. The path is a list of answers so chips can backtrack
   * and downstream answers reset.
   */
  const state = {
    /** @type {"ecosystem"|"question"|"bundle"|"review"|"config"|"deadend"} */
    phase: "ecosystem",
    /** @type {import("./guided-wizard-tree.js").WizardEcosystem|null} */
    ecosystem: null,
    /** @type {JourneyAnswer[]} */
    path: [],
    /** @type {import("./guided-wizard-tree.js").WizardResult|null} */
    result: null,
    bundleSeen: false,
    /** @type {object} Guided config island — never the page's currentConfig. */
    configValues: {},
    /** @type {string[]} Plans already created this certification (R14 ledger). */
    completedPlanNames: [],
  };

  /** @param {string} name @returns {any|null} */
  const planByName = (name) => deps.availablePlans[name] || null;

  /**
   * Siblings still owed for this certification: resolvable against tree +
   * catalog (R4) AND not already completed (R14).
   * @param {import("./guided-wizard-tree.js").WizardResult} result
   */
  const remainingSiblings = (result) =>
    filterResolvableSiblings(
      result,
      /** @type {import("./guided-wizard-tree.js").WizardEcosystem} */ (state.ecosystem),
      deps.availablePlans,
    ).filter((s) => !state.completedPlanNames.includes(s.planName));

  // ── Guided-only dirty check (R11/R5) ───────────────────────────────
  // The page's cts-unsaved-changes-guard stays scoped to the ADVANCED form
  // (its beforeunload + link-click interceptors are global; pointing it at
  // guided would pop "Leave page?" on intra-journey clicks). Guided gets
  // this lightweight beforeunload-only check instead: armed by config
  // edits, disarmed before the create redirect and on any journey
  // backtrack. Guided internal navigation is button-based, so no link
  // interceptor is needed.
  let guidedDirty = false;
  window.addEventListener("beforeunload", (e) => {
    if (guidedDirty && modeController.getMode() === "guided") {
      e.preventDefault();
    }
  });

  function clearRecoveryRecord() {
    if (session) session.removeItem(RECOVERY_STORAGE_KEY);
  }

  /** Snapshot the journey + config before the create POST (R5). */
  function writeRecoveryRecord() {
    if (!session || !state.ecosystem || !state.result) return;
    try {
      session.setItem(
        RECOVERY_STORAGE_KEY,
        JSON.stringify({
          ecosystemId: state.ecosystem.id,
          answers: state.path.map((a) => a.choice.id),
          planName: state.result.plan_name,
          config: state.configValues,
          completedPlanNames: state.completedPlanNames,
        }),
      );
    } catch {
      // Quota/serialization failure — recovery is best-effort; the create
      // itself must not be blocked by it.
    }
  }

  /**
   * After a successful create with siblings remaining, hand the loop over
   * to plan-detail (R12/R14). With nothing remaining, any stale record is
   * cleared so the banner cannot resurrect a finished loop.
   * @param {string} planId
   */
  function writeHandoffRecord(planId) {
    if (!session || !state.ecosystem || !state.result) return;
    const completed = [...state.completedPlanNames, state.result.plan_name];
    const remaining = remainingSiblings(state.result).filter(
      (s) => !completed.includes(s.planName),
    );
    try {
      if (!remaining.length) {
        session.removeItem(HANDOFF_STORAGE_KEY);
        return;
      }
      session.setItem(
        HANDOFF_STORAGE_KEY,
        JSON.stringify({
          planId,
          ecosystemId: state.ecosystem.id,
          ecosystemLabel: state.ecosystem.label,
          // The preset replays the journey UP TO the question that picks
          // the plan — the final leaf-resolving answer is dropped so the
          // user lands where they choose the next sibling (R13).
          preset: {
            ecosystemId: state.ecosystem.id,
            answers: state.path.slice(0, -1).map((a) => a.choice.id),
            completedPlanNames: completed,
          },
          remainingSiblings: remaining,
          completedPlanNames: completed,
        }),
      );
    } catch {
      // Best-effort: losing the banner must not break the redirect.
    }
  }

  /** Per-plan decline memory for the guided→advanced prefill bridge (R8). */
  const bridgeDeclinedFor = new Set();

  /** @param {string} msg */
  function announce(msg) {
    liveEl.textContent = "";
    requestAnimationFrame(() => {
      liveEl.textContent = msg;
    });
  }

  /**
   * Run a stage update inside a directional view transition, then refresh
   * trail + progress and move focus to the new stage heading.
   *
   * @param {() => void} updateFn
   * @param {"forward"|"backward"} [direction]
   * @param {{focus?: boolean}} [opts]
   */
  function navigate(updateFn, direction = "forward", { focus = true } = {}) {
    const run = () => {
      updateFn();
      renderTrail();
      renderProgress();
      if (focus) {
        requestAnimationFrame(() => {
          const h = /** @type {HTMLElement} */ (stage.querySelector("h1") || stage);
          h.focus();
        });
      }
    };
    const docAny = /** @type {any} */ (document);
    if (!docAny.startViewTransition) {
      run();
      return;
    }
    const t = docAny.startViewTransition({ update: run, types: [direction] });
    // Both promises caught so a skipped transition (rapid double-commit)
    // never surfaces as an unhandled rejection — same guard this branch
    // ships in its page heads.
    t.ready.catch(() => {});
    t.finished.catch(() => {});
  }

  const PHASES_FOR_PROGRESS = [
    { key: "choose", label: "Choose" },
    { key: "review", label: "Review" },
    { key: "config", label: "Configure" },
    { key: "done", label: "Create" },
  ];

  function progressKeyForPhase() {
    if (["ecosystem", "question", "bundle", "deadend"].includes(state.phase)) return "choose";
    if (state.phase === "review") return "review";
    if (state.phase === "config") return "config";
    return "done";
  }

  function renderProgress() {
    const active = progressKeyForPhase();
    const activeIdx = PHASES_FOR_PROGRESS.findIndex((p) => p.key === active);
    progressEl.innerHTML = PHASES_FOR_PROGRESS.map((p, i) => {
      const st = i < activeIdx ? "done" : i === activeIdx ? "current" : "todo";
      const sep =
        i < PHASES_FOR_PROGRESS.length - 1 ? '<span class="sep" aria-hidden="true"></span>' : "";
      const ariaCur = st === "current" ? ' aria-current="step"' : "";
      return `<li data-state="${st}"${ariaCur}><span class="dot" aria-hidden="true"></span>${esc(p.label)}</li>${sep}`;
    }).join("");
  }

  function renderTrail() {
    /** @type {Array<{key: string, val: string, idx: number}>} */
    const chips = [];
    if (state.ecosystem) {
      chips.push({ key: "Ecosystem", val: state.ecosystem.label, idx: -1 });
    }
    state.path.forEach((ans, i) => {
      chips.push({ key: ans.question, val: prettyChoiceLabel(ans.choice), idx: i });
    });
    if (!chips.length) {
      trailEl.innerHTML = "";
      return;
    }
    trailEl.innerHTML =
      '<span class="trail-label">Your path</span>' +
      chips
        .map(
          (c) =>
            `<button type="button" class="chip" data-idx="${c.idx}">` +
            `<span class="chip-key">${esc(shortKey(c.key))}</span>` +
            `<span class="chip-val">${esc(c.val)}</span>` +
            `<cts-icon name="edit-pencil-01" size="16"></cts-icon></button>`,
        )
        .join("");
    trailEl.querySelectorAll(".chip").forEach((btn) => {
      btn.addEventListener("click", () =>
        backtrackTo(parseInt(/** @type {HTMLElement} */ (btn).dataset.idx || "0", 10)),
      );
    });
  }

  /**
   * Clicking a chip: backtrack to that answer's step. idx === -1 means the
   * ecosystem chip → back to the ecosystem picker. Everything downstream
   * resets (result, bundle flag, config state) — including the recovery
   * record: a journey mutation invalidates the snapshot, otherwise a later
   * reload would restore an attempt the user already abandoned.
   * @param {number} idx
   */
  function backtrackTo(idx) {
    state.result = null;
    state.bundleSeen = false;
    state.configValues = {};
    guidedDirty = false;
    clearRecoveryRecord();
    if (idx === -1) {
      state.ecosystem = null;
      state.path = [];
      state.phase = "ecosystem";
    } else {
      state.path = state.path.slice(0, idx);
      state.phase = "question";
    }
    renderCurrent("backward");
  }

  /** The shared step-level Back action (bundle/review/config/deadend phases). */
  const BACK_BUTTON = {
    variant: "secondary",
    icon: "arrow-left-md",
    label: "Back",
    on: () => goBack(),
  };

  /**
   * The step-level Back button (bundle/review/config/deadend phases).
   */
  function goBack() {
    if (state.phase === "config") {
      state.phase = "review";
      renderCurrent("backward");
      return;
    }
    if (
      state.phase === "review" &&
      state.result &&
      remainingSiblings(state.result).length &&
      state.bundleSeen
    ) {
      state.phase = "bundle";
      renderCurrent("backward");
      return;
    }
    // bundle / review-without-bundle / deadend: undo the last answer.
    backtrackTo(state.path.length - 1);
  }

  // ── The router ─────────────────────────────────────────────────────
  /**
   * @param {"forward"|"backward"} [direction]
   * @param {{focus?: boolean}} [opts]
   */
  function renderCurrent(direction = "forward", opts = {}) {
    navigate(
      () => {
        if (state.phase === "ecosystem") return renderEcosystemStep();
        if (state.phase === "question") return renderQuestionStep();
        if (state.phase === "bundle") return renderBundleStep();
        if (state.phase === "review") return renderReviewStep();
        if (state.phase === "config") return renderConfigStep();
        if (state.phase === "deadend") return renderDeadEndStep();
      },
      direction,
      opts,
    );
  }

  /**
   * Walk the tree to find the step the user must answer next, based on the
   * answers in state.path. Returns { step } or { leaf, choice }.
   * @returns {{step?: any, leaf?: any}|null}
   */
  function currentNode() {
    if (!state.ecosystem) return null;
    let step = state.ecosystem.steps[0];
    for (const ans of state.path) {
      const choice = step.choices.find((c) => c.id === ans.choice.id);
      if (!choice) return { step };
      if (choice.result) return { leaf: choice.result };
      if (choice.next) step = choice.next;
    }
    return { step };
  }

  // ── STEP: ecosystem selection (+ escape hatch) ─────────────────────
  function renderEcosystemStep() {
    stage.innerHTML = `
      <p class="stage-eyebrow">Get certified</p>
      <h1 tabindex="-1">Which ecosystem are you certifying for?</h1>
      <p class="stage-lede">Pick the certification program you are targeting. We'll work out exactly which test plan and settings you need — you won't have to know any internal plan names.</p>
      <fieldset class="choice-grid" role="radiogroup" aria-label="Ecosystem">
        <legend>Which ecosystem are you certifying for?</legend>
        ${GUIDED_WIZARD_TREE.ecosystems
          .map((e, i) => {
            const { flag, rest } = splitFlag(e.label);
            return choiceCardHTML({
              id: e.id,
              flag,
              label: rest,
              desc: ECOSYSTEM_DESC[e.id] || "",
              index: i,
            });
          })
          .join("")}
      </fieldset>

      <div class="escape-hatch">
        <button type="button" class="escape-card" id="guidedBrowseAll">
          <cts-icon name="search-magnifying-glass" size="24"></cts-icon>
          <span class="ec-body">
            <span class="ec-title">Not certifying for an ecosystem?</span>
            <span class="ec-sub">Switch to advanced mode to browse the full test-plan catalog — plain OIDC, generic FAPI2, CIBA and more.</span>
          </span>
          <cts-icon class="ec-chev" name="arrow-right-md" size="20"></cts-icon>
        </button>
      </div>`;
    clearActionBar();
    wireChoiceGroup((choiceId) => pickEcosystem(choiceId));
    // The escape hatch routes INTO advanced mode (the real catalog browser).
    mustGet("guidedBrowseAll").addEventListener("click", () => modeController.setMode("advanced"));
  }

  // ── STEP: a tree question ──────────────────────────────────────────
  function renderQuestionStep() {
    const node = currentNode();
    if (node && node.leaf) {
      enterLeaf(node.leaf);
      return;
    }
    const step = node && node.step;
    if (!step || !state.ecosystem) return;
    const narrow = step.choices.length <= 3;
    const question = esc(normalizeQuestion(step.question));
    stage.innerHTML = `
      <p class="stage-eyebrow">${esc(state.ecosystem.label)}</p>
      <h1 tabindex="-1">${question}</h1>
      <p class="stage-lede">${esc(questionLede(step.id))}</p>
      <fieldset class="choice-grid${narrow ? " is-narrow" : ""}" role="radiogroup" aria-label="${question}">
        <legend>${question}</legend>
        ${step.choices
          .map((c, i) =>
            choiceCardHTML({
              id: c.id,
              label: prettyChoiceLabel(c),
              desc: c.description || implicitDescription(step.id, c),
              badge: bundleBadge(c),
              index: i,
            }),
          )
          .join("")}
      </fieldset>`;
    clearActionBar();
    wireChoiceGroup((choiceId) => answerQuestion(step, choiceId));
  }

  /**
   * @param {import("./guided-wizard-tree.js").WizardChoice} choice
   * @returns {string}
   */
  function bundleBadge(choice) {
    if (choice.result && choice.result.also_required && choice.result.also_required.length) {
      const n = choice.result.also_required.length + 1;
      return `<cts-badge variant="info-subtle">${n}-plan certification</cts-badge>`;
    }
    return "";
  }

  // ── Choice-card markup + shared radiogroup keyboard wiring ─────────
  /**
   * @param {{id: string, label: string, desc?: string, flag?: string, badge?: string, index: number}} card
   * @returns {string}
   */
  function choiceCardHTML({ id, label, desc, flag, badge, index }) {
    return `
      <label class="choice" data-choice="${esc(id)}">
        <input type="radio" name="guidedChoiceGroup" value="${esc(id)}" tabindex="${index === 0 ? "0" : "-1"}">
        ${
          flag
            ? `<span class="flag" aria-hidden="true">${esc(flag)}</span>`
            : `<span class="marker" aria-hidden="true"><cts-icon name="check" size="16"></cts-icon></span>`
        }
        <span class="choice-body">
          <span class="choice-label">${esc(label)}</span>
          ${desc ? `<span class="choice-desc">${esc(desc)}</span>` : ""}
          ${badge ? `<span class="choice-desc choice-badge-row">${badge}</span>` : ""}
        </span>
      </label>`;
  }

  /**
   * Radiogroup keyboard model (WAI-ARIA): arrows move + select, Enter/Space
   * commits the active choice. Selection is what advances the journey.
   * @param {(choiceId: string) => void} onCommit
   */
  function wireChoiceGroup(onCommit) {
    const radios = /** @type {HTMLInputElement[]} */ (
      Array.from(stage.querySelectorAll('input[name="guidedChoiceGroup"]'))
    );
    if (!radios.length) return;
    /** @param {number} i */
    const focusAt = (i) => {
      radios.forEach((r) => (r.tabIndex = -1));
      radios[i].tabIndex = 0;
      radios[i].focus();
    };
    radios.forEach((radio, i) => {
      const label = radio.closest(".choice");
      radio.addEventListener("change", () => onCommit(radio.value));
      if (!label) return;
      label.addEventListener("keydown", (e) => {
        const key = /** @type {KeyboardEvent} */ (e).key;
        if (key === "ArrowDown" || key === "ArrowRight") {
          e.preventDefault();
          focusAt((i + 1) % radios.length);
        } else if (key === "ArrowUp" || key === "ArrowLeft") {
          e.preventDefault();
          focusAt((i - 1 + radios.length) % radios.length);
        } else if (key === "Enter" || key === " ") {
          e.preventDefault();
          radio.checked = true;
          onCommit(radio.value);
        }
      });
    });
  }

  // ── Transitions between phases ─────────────────────────────────────
  /** @param {string} id */
  function pickEcosystem(id) {
    state.ecosystem = GUIDED_WIZARD_TREE.ecosystems.find((e) => e.id === id) || null;
    state.path = [];
    // A user-driven ecosystem pick starts a NEW certification: the R14
    // ledger from a replayed/restored loop must not leak into it (plan
    // names repeat across ecosystems, so a stale ledger would silently
    // drop valid siblings from the new journey's bundle).
    state.completedPlanNames = [];
    state.phase = "question";
    renderCurrent("forward");
  }

  /**
   * @param {import("./guided-wizard-tree.js").WizardStep} step
   * @param {string} choiceId
   */
  function answerQuestion(step, choiceId) {
    const choice = step.choices.find((c) => c.id === choiceId);
    if (!choice) return;
    state.path.push({ stepId: step.id, question: step.question, choice });
    if (choice.result) {
      enterLeaf(choice.result);
    } else {
      state.phase = "question";
      renderCurrent("forward");
    }
  }

  /**
   * Reaching a leaf. The catalog guard runs FIRST (R4): a tree plan absent
   * from the live catalog dead-ends with the Advanced escape hatch — never
   * an empty config form. With a multi-plan certification, surface the
   * bundle BEFORE the user commits; otherwise go straight to review.
   * @param {import("./guided-wizard-tree.js").WizardResult} result
   */
  function enterLeaf(result) {
    state.result = result;
    const plan = planByName(result.plan_name);
    if (!plan) {
      state.phase = "deadend";
      announce("This certification path is not available on this server.");
      renderCurrent("forward");
      return;
    }
    announce(`Resolved to ${plan.displayName || result.plan_name}.`);
    state.phase = remainingSiblings(result).length && !state.bundleSeen ? "bundle" : "review";
    renderCurrent("forward");
  }

  // ── STEP: tree/catalog skew dead-end (R4) ──────────────────────────
  function renderDeadEndStep() {
    const planName = state.result ? state.result.plan_name : "";
    stage.innerHTML = `
      <p class="stage-eyebrow">${esc(state.ecosystem ? state.ecosystem.label : "Get certified")}</p>
      <h1 tabindex="-1">This path isn't available on this server</h1>
      <p class="stage-lede">Your answers resolve to <code class="plan-name-code">${esc(planName)}</code>, which this conformance-suite deployment doesn't currently offer. You can go back and pick a different path, or browse every plan this server does offer in advanced mode.</p>
      <div class="escape-hatch">
        <button type="button" class="escape-card" id="guidedDeadEndEscape">
          <cts-icon name="search-magnifying-glass" size="24"></cts-icon>
          <span class="ec-body">
            <span class="ec-title">Browse all available plans</span>
            <span class="ec-sub">Switch to advanced mode and pick from the full catalog on this server.</span>
          </span>
          <cts-icon class="ec-chev" name="arrow-right-md" size="20"></cts-icon>
        </button>
      </div>`;
    renderActionBar([BACK_BUTTON]);
    mustGet("guidedDeadEndEscape").addEventListener("click", () =>
      modeController.setMode("advanced"),
    );
  }

  // ── STEP: multi-plan certification preview (also_required) ─────────
  function renderBundleStep() {
    const result = /** @type {import("./guided-wizard-tree.js").WizardResult} */ (state.result);
    const selfPlan = planByName(result.plan_name);
    const siblings = remainingSiblings(result);
    const total = siblings.length + 1;
    stage.innerHTML = `
      <p class="stage-eyebrow">${esc(state.ecosystem ? state.ecosystem.label : "")}</p>
      <h1 tabindex="-1">This certification needs ${total} test plans</h1>
      <p class="stage-lede">Full ${esc(ecoName())} certification for this role requires more than one plan. You'll set these up one at a time — we'll bring you straight back for the next one. Here's the whole checklist:</p>
      <cts-card>
        <ul class="bundle-list">
          <li data-self="true">
            <span class="step-num">1</span>
            <span class="name">${esc(selfPlan ? selfPlan.displayName : result.plan_name)}</span>
            <span class="here">Starting here</span>
          </li>
          ${siblings
            .map(
              (s, i) => `
            <li>
              <span class="step-num">${i + 2}</span>
              <span class="name">${esc(s.label)}</span>
              <span class="later">Set up next</span>
            </li>`,
            )
            .join("")}
        </ul>
      </cts-card>`;
    renderActionBar([
      BACK_BUTTON,
      {
        variant: "primary",
        icon: "arrow-right-md",
        label: "Continue with plan 1",
        spacer: true,
        on: () => {
          state.bundleSeen = true;
          state.phase = "review";
          renderCurrent("forward");
        },
      },
    ]);
  }

  function ecoName() {
    return state.ecosystem ? splitFlag(state.ecosystem.label).rest : "";
  }

  // ── STEP: review (plain-language, read-only variant table) ─────────
  function renderReviewStep() {
    const result = /** @type {import("./guided-wizard-tree.js").WizardResult} */ (state.result);
    const plan = planByName(result.plan_name);
    const variants = result.variants;
    const siblings = remainingSiblings(result);

    const variantRows = Object.entries(variants)
      .map(([param, val]) => {
        const info = variantDisplayInfo(plan, param);
        return `<tr>
          <td class="vt-param">${esc(info.displayName || humanizeKey(param))}</td>
          <td><span class="vt-value">${esc(valueLabel(param, val))}</span></td>
          <td class="vt-desc">${esc(info.description || "")}</td>
        </tr>`;
      })
      .join("");

    // Section chips for "configuration you'll provide next", derived from
    // the same adapter call the config step uses — one source of truth.
    let configChips = "";
    if (plan && deps.fieldCatalog) {
      const { uiSchema } = buildConfigFormSchema(plan, deps.fieldCatalog, variants);
      configChips = (uiSchema.sections || [])
        .filter((s) => s.key !== "_root")
        .map(
          (s) =>
            `<cts-badge variant="secondary">${esc(String(s.title).replace(/\s*\(.*\)\s*$/, ""))}</cts-badge>`,
        )
        .join("");
    }

    const moduleCount = plan && Array.isArray(plan.modules) ? plan.modules.length : 0;

    stage.innerHTML = `
      <p class="stage-eyebrow">Review before you create</p>
      <h1 tabindex="-1">Here's the plan we resolved</h1>
      <p class="stage-lede">Everything below is derived from your answers. Review it, then move on to fill in your endpoints and keys.</p>

      <div class="review-section">
        <h2>Test plan</h2>
        <cts-card tone="orange">
          <div class="resolved-plan">
            <span class="pip" aria-hidden="true"><cts-icon name="shield-check" size="24"></cts-icon></span>
            <div>
              <p class="plan-name-display">${esc(plan ? plan.displayName : result.plan_name)}</p>
              <div class="plan-meta">
                ${plan && plan.specFamily ? `<cts-badge variant="secondary">${esc(plan.specFamily)}</cts-badge>` : ""}
                ${plan && plan.specVersion ? `<cts-badge variant="secondary">${esc(plan.specVersion)}</cts-badge>` : ""}
                <span class="plan-name-code">${esc(result.plan_name)}</span>
                ${moduleCount ? `<span class="plan-name-code">${moduleCount} test modules</span>` : ""}
              </div>
            </div>
          </div>
        </cts-card>
      </div>

      <div class="review-section">
        <h2>Your settings, in plain language</h2>
        <cts-card>
          <table class="variant-table">
            <thead><tr><th scope="col">Setting</th><th scope="col">Value</th><th scope="col">What it means</th></tr></thead>
            <tbody>${variantRows}</tbody>
          </table>
        </cts-card>
      </div>

      <div class="review-section">
        <h2>Configuration you'll provide next</h2>
        <cts-card>
          <p class="review-config-hint">These sections of the test configuration will need your input:</p>
          <div class="config-preview">${configChips || '<cts-badge variant="secondary">Test information</cts-badge>'}</div>
        </cts-card>
      </div>

      ${
        siblings.length
          ? `
      <div class="review-section">
        <h2>Full certification checklist</h2>
        <cts-card>
          <ul class="bundle-list">
            <li data-self="true"><span class="step-num">1</span><span class="name">${esc(plan ? plan.displayName : result.plan_name)}</span><span class="here">This plan</span></li>
            ${siblings.map((s, i) => `<li><span class="step-num">${i + 2}</span><span class="name">${esc(s.label)}</span><span class="later">After this</span></li>`).join("")}
          </ul>
        </cts-card>
      </div>`
          : ""
      }`;

    renderActionBar([
      BACK_BUTTON,
      {
        variant: "primary",
        icon: "arrow-right-md",
        label: "Configure this plan",
        spacer: true,
        on: () => {
          state.phase = "config";
          renderCurrent("forward");
        },
      },
    ]);
  }

  // ── STEP: config (real cts-config-form + real create) ──────────────
  function renderConfigStep() {
    const result = /** @type {import("./guided-wizard-tree.js").WizardResult} */ (state.result);
    const plan = planByName(result.plan_name);
    // Guests (isGuest) get the sign-in prompt too — the same convention
    // plan-detail.html applies for its readonly gate (R6). The backend is
    // the real authz boundary; this only keeps the UI honest.
    const currentUser = deps.getCurrentUser && deps.getCurrentUser();
    const signedIn = !!(currentUser && !currentUser.isGuest);
    stage.innerHTML = `
      <p class="stage-eyebrow">${esc(plan ? plan.displayName : "")}</p>
      <h1 tabindex="-1">Configure your test</h1>
      <p class="stage-lede">Fill in the endpoints and keys for your deployment, then create the plan. Use the JSON tab to paste a whole configuration at once.</p>
      <div id="guidedConfigError" hidden></div>
      ${
        signedIn
          ? ""
          : `<cts-alert variant="info" id="guidedSignInPrompt">
               You're browsing as a guest. <a href="/login.html">Sign in</a> to create this test plan — everything above stays free to explore.
             </cts-alert>`
      }
      <cts-config-form id="guidedConfigForm"></cts-config-form>`;

    const form = /** @type {any} */ (document.getElementById("guidedConfigForm"));
    if (form && plan && deps.fieldCatalog) {
      // Same adapter pipeline as the advanced island, but bound to the
      // guided island's own form instance and config mirror — never the
      // page's #ctsConfigForm / currentConfig (two-island independence).
      const { schema, uiSchema } = buildConfigFormSchema(plan, deps.fieldCatalog, result.variants);
      form.schema = schema;
      form.uiSchema = uiSchema;
      form.hiddenFields = computeHiddenFields(plan, result.variants, deps.fieldCatalog);
      form.config = state.configValues;
      form.addEventListener(
        "cts-config-change",
        /** @param {any} e */ (e) => {
          state.configValues = (e.detail && e.detail.config) || {};
          guidedDirty = true;
        },
      );
    }

    renderActionBar([
      BACK_BUTTON,
      ...(signedIn
        ? [
            {
              variant: "primary",
              icon: "paper-plane",
              label: "Create test plan",
              spacer: true,
              id: "guidedCreateBtn",
              on: () => void submitGuidedCreate(),
            },
          ]
        : []),
    ]);
  }

  function hideConfigError() {
    const box = document.getElementById("guidedConfigError");
    if (box) {
      box.hidden = true;
      box.replaceChildren();
    }
  }

  /**
   * Render the normalized create failure inline on the config step — the
   * journey (answers + config) stays exactly where it was (R5).
   * @param {string} code
   * @param {string} message
   */
  function showConfigError(code, message) {
    const box = document.getElementById("guidedConfigError");
    if (!box) return;
    const alert = document.createElement("cts-alert");
    alert.setAttribute("variant", "danger");
    alert.textContent = code
      ? `Creating the test plan failed (HTTP ${code}): ${message}`
      : `Creating the test plan failed: ${message}`;
    box.replaceChildren(alert);
    box.hidden = false;
  }

  /** @param {boolean} pending */
  function setCreatePending(pending) {
    const btn = document.getElementById("guidedCreateBtn");
    if (!btn) return;
    if (pending) {
      btn.setAttribute("loading", "");
      btn.setAttribute("disabled", "");
    } else {
      btn.removeAttribute("loading");
      btn.removeAttribute("disabled");
    }
  }

  /**
   * The real create. Recovery record first (R5), then POST via the page's
   * shared helper; success clears recovery, writes the handoff when
   * siblings remain (R12/R14), disarms the dirty check, and redirects.
   * Failure stays on the config step with the same normalized error the
   * advanced modal would show.
   */
  async function submitGuidedCreate() {
    const result = state.result;
    const plan = result && planByName(result.plan_name);
    if (!result || !plan || !deps.createPlan || !deps.normalizeCreateError) return;
    hideConfigError();

    // POST only variant params the plan actually declares — the tree may
    // carry presentation-only params (e.g. brazil_client_scope on plans
    // that don't declare it); an undeclared param would fail the backend's
    // variant validation.
    /** @type {Record<string, string>} */
    const variant = {};
    for (const [param, value] of Object.entries(result.variants || {})) {
      if (plan.variants && plan.variants[param]) variant[param] = value;
    }

    writeRecoveryRecord();
    setCreatePending(true);
    try {
      const data = await deps.createPlan(result.plan_name, variant, state.configValues);
      clearRecoveryRecord();
      guidedDirty = false;
      writeHandoffRecord(data.id);
      window.location.assign("/plan-detail.html?plan=" + encodeURIComponent(data.id));
    } catch (error) {
      setCreatePending(false);
      const { code, message } = await deps.normalizeCreateError(error);
      showConfigError(code, message);
      announce("Creating the test plan failed. " + message);
    }
  }

  // ── Sticky action bar helper (persistent element in the island) ────
  /**
   * Render the per-step buttons into #guidedActionsContent — the persistent
   * `display: contents` span that cts-action-bar adopted into its sticky
   * wrapper at first connect. Never replace the HOST's children: the
   * component captures them once, so mutating the host would tear out the
   * `.oidf-action-bar` wrapper and leave the buttons in normal flow.
   * @param {Array<{variant: string, icon?: string, label: string, spacer?: boolean, id?: string, on: () => void}>} buttons
   */
  function renderActionBar(buttons) {
    actionBarContent.replaceChildren();
    buttons.forEach((b) => {
      const btn = document.createElement("cts-button");
      btn.setAttribute("variant", b.variant);
      // Sticky action bars use the large size, matching #launchButtons.
      btn.setAttribute("size", "lg");
      if (b.icon) btn.setAttribute("icon", b.icon);
      if (b.id) btn.id = b.id;
      btn.setAttribute("label", b.label);
      btn.addEventListener("cts-click", b.on);
      if (b.spacer) {
        const spacer = document.createElement("span");
        spacer.className = "actions-right";
        spacer.appendChild(btn);
        actionBarContent.appendChild(spacer);
      } else {
        actionBarContent.appendChild(btn);
      }
    });
    actionBar.hidden = false;
  }

  function clearActionBar() {
    actionBarContent.replaceChildren();
    actionBar.hidden = true;
  }

  // ── Guided → Advanced prefill bridge (R8) ──────────────────────────
  // When entering advanced and the guided journey has a resolved plan that
  // advanced has not already adopted, offer (never force) a prefill.
  const bridgePrompt = document.getElementById("bridgePrompt");

  function hideBridge() {
    if (bridgePrompt) bridgePrompt.hidden = true;
  }

  function maybeOfferBridge() {
    if (!bridgePrompt) return;
    const resolved = state.result;
    if (!resolved || !resolved.plan_name || state.phase === "deadend") {
      hideBridge();
      return;
    }
    const plan = planByName(resolved.plan_name);
    if (!plan) {
      hideBridge();
      return;
    }
    if (bridgeDeclinedFor.has(resolved.plan_name)) {
      hideBridge();
      return;
    }
    const planSelectEl = /** @type {HTMLSelectElement|null} */ (
      document.getElementById("planSelect")
    );
    if (planSelectEl && planSelectEl.value === resolved.plan_name) {
      hideBridge();
      return;
    }
    const sub = document.getElementById("bridgeSub");
    if (sub) {
      sub.textContent = `Your guided answers resolved to “${plan.displayName || resolved.plan_name}”. Prefill the form with this plan and its variants, or keep your current advanced selection.`;
    }
    bridgePrompt.hidden = false;
  }

  async function acceptBridge() {
    const resolved = state.result;
    const plan = resolved ? planByName(resolved.plan_name) : null;
    const cascade = /** @type {any} */ (document.getElementById("specCascade"));
    if (!resolved || !plan || !cascade) {
      hideBridge();
      return;
    }
    // Route through the cascade with user-pick semantics: the page's
    // cts-plan-selected listener sees isSystemSelectingPlan === 0 and clears
    // the in-flight config (the system-event-suppression counter is reserved
    // for restore flows). The dispatched event is deferred via
    // updateComplete, so awaiting it here means the variant <select>s are
    // rendered when we overlay the journey's values below.
    cascade.selectPlanByName(resolved.plan_name);
    await cascade.updateComplete;
    for (const [param, value] of Object.entries(resolved.variants || {})) {
      const el = /** @type {HTMLSelectElement|null} */ (
        document.querySelector(`.variant-selector[data-variant-parameter="${param}"]`)
      );
      const declared = plan.variants && plan.variants[param] && plan.variants[param].variantValues;
      if (el && declared && value in declared) {
        el.value = value;
      }
    }
    // One change dispatch re-runs the page's variant-visibility + config-
    // field-visibility listeners against the overlaid values.
    const variantForm = document.getElementById("variantSelectors");
    if (variantForm) variantForm.dispatchEvent(new Event("change"));
    hideBridge();
    // Same arrival cue as picking the plan from the search list: scroll the
    // cascade into view, flash the selection group, focus the first variant.
    // After hideBridge() so the rAF inside measures the post-hide layout.
    if (deps.revealPlanSelection) deps.revealPlanSelection();
    announce(`Prefilled from your guided answers: ${plan.displayName || resolved.plan_name}.`);
  }

  function declineBridge() {
    if (state.result && state.result.plan_name) {
      bridgeDeclinedFor.add(state.result.plan_name);
    }
    hideBridge();
  }

  const acceptBtn = document.getElementById("bridgeAcceptBtn");
  const declineBtn = document.getElementById("bridgeDeclineBtn");
  if (acceptBtn) acceptBtn.addEventListener("cts-click", () => void acceptBridge());
  if (declineBtn) declineBtn.addEventListener("cts-click", declineBridge);

  modeController.onModeChange((mode) => {
    if (mode === "advanced") {
      maybeOfferBridge();
    } else {
      hideBridge();
    }
  });

  // ── Boot ───────────────────────────────────────────────────────────
  // R13: consume the wizard_preset param exactly once — stripped via
  // replaceState whenever present, even when a higher ladder slot (e.g. a
  // recovery record or an advanced-forcing param) won the mode decision.
  // Without the unconditional strip, a preset that lost to the recovery
  // slot would linger in the URL and replay unexpectedly on a later reload
  // after the user backtracked (which clears the recovery record).
  const bootParams = new URLSearchParams(window.location.search);
  const rawPreset = bootParams.get("wizard_preset");
  if (rawPreset !== null) {
    bootParams.delete("wizard_preset");
    const qs = bootParams.toString();
    history.replaceState(
      null,
      "",
      window.location.pathname + (qs ? "?" + qs : "") + window.location.hash,
    );
  }

  // Best-effort replay when the ladder resolved via wizard_preset. An
  // unresolvable hop drops the user at the last valid step; a garbage
  // preset opens the ecosystem screen with a console warning only.
  if (modeController.source === "wizard_preset") {
    const preset = decodeWizardPreset(rawPreset);
    const replay = preset ? replayAnswers(preset.ecosystemId, preset.answers) : null;
    if (preset && replay) {
      state.ecosystem = replay.ecosystem;
      state.path = replay.path;
      state.completedPlanNames = preset.completedPlanNames;
      if (replay.result) {
        // The full trail resolved a leaf — apply the same entry guards as
        // enterLeaf (catalog presence, remaining-sibling bundle).
        state.result = replay.result;
        state.phase = !planByName(replay.result.plan_name)
          ? "deadend"
          : remainingSiblings(replay.result).length
            ? "bundle"
            : "review";
      } else {
        state.phase = "question";
      }
    } else {
      console.warn(
        "[guided-wizard] wizard_preset did not decode or replay; starting at the ecosystem screen",
      );
    }
  }

  // R5: when the mode ladder resolved via the recovery slot, restore the
  // snapshotted journey at the config step. The trail is replayed against
  // the live tree; any mismatch (tree drift, plan gone from the catalog)
  // drops the record and starts fresh rather than restoring a broken state.
  if (modeController.source === "recovery" && session) {
    try {
      const raw = session.getItem(RECOVERY_STORAGE_KEY);
      const record = raw ? JSON.parse(raw) : null;
      const replay = record ? replayAnswers(record.ecosystemId, record.answers || []) : null;
      const restorable =
        replay &&
        replay.result &&
        replay.result.plan_name === record.planName &&
        planByName(record.planName);
      if (restorable) {
        state.ecosystem = replay.ecosystem;
        state.path = replay.path;
        state.result = replay.result;
        state.bundleSeen = true;
        state.configValues = record.config || {};
        state.completedPlanNames = Array.isArray(record.completedPlanNames)
          ? record.completedPlanNames
          : [];
        state.phase = "config";
      } else {
        console.warn("[guided-wizard] recovery record no longer replays; starting fresh");
        clearRecoveryRecord();
      }
    } catch (e) {
      console.warn("[guided-wizard] malformed recovery record; starting fresh", e);
      clearRecoveryRecord();
    }
  }

  // Render eagerly (even when the island is hidden) without stealing focus.
  renderCurrent("forward", { focus: false });

  return {
    /** Test/diagnostic seam: the journey's current phase. */
    getPhase: () => state.phase,
  };
}
