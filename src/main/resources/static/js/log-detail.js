/**
 * Bootstrap for log-detail.html — the Lit-triad-based replacement for
 * log-detail.html. This module:
 *
 *   1. Fetches `/api/info`, `/api/plan`, and `/api/runner` for the test
 *      whose ID is in `?log=…`, populating the cts-log-detail-header,
 *      cts-log-viewer, and the cts-test-nav-controls cluster the header
 *      renders inside itself.
 *   2. Wires the header's bubbling action events (cts-edit-config,
 *      cts-share-link, cts-publish, cts-upload-images, cts-download-log,
 *      cts-start-test, cts-stop-test) plus the cluster's cts-repeat /
 *      cts-continue events to page-level handlers.
 *   3. Renders the running-test browser-URL prompt (QR code + "Visit"
 *      button + clipboard copy) into the header's [data-slot="browser"]
 *      placeholder. Renders the FINAL_ERROR alert into [data-slot="error"]
 *      when the test is INTERRUPTED.
 *   4. Registers a single document-level cts-scroll-to-entry listener so
 *      U4's failure-summary, U6's hash navigation, and U8's TOC rail can
 *      all bubble to the same handler.
 *   5. Binds Cmd/Ctrl+Shift+X (Repeat Test) and Cmd/Ctrl+Shift+U
 *      (Continue Plan) keyboard shortcuts.
 *
 * No FAPI_UI.logTemplates.* references: this page is the new render path,
 * full stop. The legacy log-detail.html keeps its template loaders alive
 * for cookie-less visitors during the rollout window only.
 *
 * Plan: docs/plans/2026-04-26-002-refactor-log-detail-page-to-lit-triad-plan.md
 */

import { renderErrorIntoSlot } from "./log-detail-error-slot.js";
import { scrollEntryIntoView } from "../components/cts-log-entry.js";
import { tryGetStorage } from "../components/guided-wizard.js";

const POLL_INTERVAL_MS = 3000;

/**
 * localStorage key for the Test-structure rail collapse preference
 * (feat/log-toc-collapsible). Stored as the string "true" / "false". Read
 * pre-paint by the inline <head> script in log-detail.html and again here so
 * the toggle's label + aria state match the rendered rail. Global (one key),
 * so the choice carries across logs.
 */
const TOC_COLLAPSE_STORAGE_KEY = "oidf-log-toc-collapsed";

/** @type {string} */
let testId = "";
/** @type {boolean} */
let isPublic = false;
/** @type {boolean} */
let isAdmin = false;

/** @type {{ active: number | null }} */
const runnerPollState = { active: null };

/**
 * Latest /api/info payload (testName, planId, variant). Read by
 * handleRepeat/handleContinue to build the correct /api/runner URL —
 * the components emit `{ testId }` as their action-event detail because
 * the runtime test instance is all they own, but the runner endpoint's
 * `test=` query param wants the *module name* (e.g. `oidcc-server`),
 * not the runtime ID. Cache it module-scope so the action handlers can
 * resolve testName + variant without re-fetching.
 *
 * @type {any}
 */
let latestTestInfo = null;

/**
 * Modules list from /api/plan/{planId}, cached at fetch time so
 * handleContinue can find the *next* module without a second roundtrip.
 * Each entry has { testModule: string, variant: object }.
 *
 * @type {Array<any>}
 */
let cachedPlanModules = [];

/** Resolve query string params we care about exactly once. */
function readUrlParams() {
  const params = new URLSearchParams(window.location.search);
  testId = params.get("log") || "";
  isPublic = params.get("public") === "true";
}

/** ──────────── Modal helpers (replacing FAPI_UI.showError/showBusy) ──────────── */

function showError(message) {
  const modal = document.getElementById("errorModal");
  const text = document.getElementById("errorMessage");
  if (text) text.textContent = String(message || "An error occurred");
  if (modal && typeof modal.show === "function") modal.show();
}

function showBusy(message) {
  const modal = document.getElementById("loadingModal");
  const text = document.getElementById("loadingMessage");
  if (text) text.textContent = String(message || "Loading…");
  if (modal && typeof modal.show === "function") modal.show();
}

function hideBusy() {
  const modal = document.getElementById("loadingModal");
  if (modal && typeof modal.hide === "function") modal.hide();
}

/** ──────────── /api/currentuser ──────────── */

async function fetchCurrentUser() {
  try {
    const response = await fetch("/api/currentuser");
    if (!response.ok) return;
    const user = await response.json();
    isAdmin = !!(user && user.isAdmin);
  } catch (err) {
    console.warn("[log-detail] /api/currentuser failed:", err);
  }
}

/** ──────────── testInfo fan-out ──────────── */

/**
 * Filter the per-condition `results` array down to the entries the failure
 * summary surfaces. Mirrors `cts-log-detail-header._getFailures()` so the
 * page-level `#ctsTopFailureSummary` and the in-header instance render
 * the same list. Kept in this file (not exported from the component) to
 * avoid coupling the bootstrap to component internals.
 *
 * @param {any} testInfo
 * @returns {Array<any>}
 */
function selectFailures(testInfo) {
  if (!testInfo || !Array.isArray(testInfo.results)) return [];
  return testInfo.results.filter(
    (entry) =>
      entry.result === "FAILURE" ||
      entry.result === "WARNING" ||
      entry.result === "SKIPPED" ||
      entry.result === "INTERRUPTED",
  );
}

/**
 * Push a fresh `testInfo` to the header and the filtered failures to the
 * page-level summary. Single update site so any future re-fetch path
 * (uploaded-images stamping, runner-poll state changes, etc.) keeps both
 * instances in sync without duplicating the filter.
 *
 * @param {any} testInfo
 */
function applyTestInfo(testInfo) {
  latestTestInfo = testInfo;
  /** @type {any} */
  const header = document.getElementById("logDetailHeader");
  if (header) header.testInfo = testInfo;
  /** @type {any} */
  const topFailureSummary = document.getElementById("ctsTopFailureSummary");
  if (topFailureSummary) {
    topFailureSummary.failures = selectFailures(testInfo);
    topFailureSummary.testId = testId;
  }
  /** @type {any} */
  const topTestSummary = document.getElementById("ctsTopTestSummary");
  if (topTestSummary) {
    // U7 / Region B1: the page-level instance shows the WAITING-state
    // instructions banner above the failure summary. The header card's
    // instance keeps showing the full split (description + instructions)
    // at desktop. Pass the raw summary; the cts-test-summary component's
    // splitter renders the instructions zone whenever it exists, and the
    // cts-test-summary returns `nothing` when neither half is present
    // (e.g. a FINISHED test with no summary), so the empty instance
    // takes zero vertical room.
    topTestSummary.summary = (testInfo && testInfo.summary) || "";
  }
  /** @type {any} */
  const rail = document.getElementById("ctsLogToc");
  if (rail) {
    // U8 — keep the rail's compact failure summary in lockstep with the
    // page-level instance. The viewer-driven blocks list arrives via the
    // cts-blocks-updated event in setupLogToc().
    rail.failures = selectFailures(testInfo);
    rail.testId = testId;
  }
}

/**
 * Apply the latest `entry._id` → `LOG-NNNN` map (U6) to every failure
 * summary instance on the page so reference chips render alongside each
 * failure row. Two instances: the page-level `#ctsTopFailureSummary`
 * (mobile / tablet position) and the in-header instance the
 * cts-log-detail-header renders inside its card. Both consume the same
 * map; missing entries simply omit the chip.
 *
 * @param {Object.<string, string>} references
 */
function applyReferences(references) {
  /** @type {any} */
  const topFailureSummary = document.getElementById("ctsTopFailureSummary");
  if (topFailureSummary) {
    topFailureSummary.references = references;
    topFailureSummary.testId = testId;
  }
  /** @type {any} */
  const header = document.getElementById("logDetailHeader");
  if (header) {
    const inHeaderSummary = header.querySelector("cts-failure-summary");
    if (inHeaderSummary) {
      inHeaderSummary.references = references;
      inHeaderSummary.testId = testId;
    }
  }
}

/** ──────────── /api/info ──────────── */

async function fetchTestInfo() {
  const url = `/api/info/${encodeURIComponent(testId)}` + (isPublic ? "?public=true" : "");
  const response = await fetch(url);
  if (!response.ok) {
    // Surface the server's own error message when the body carries one
    // (e.g. {"error": "log not found"}) rather than the leaky API path —
    // the URL is an implementation detail; the message is what the user
    // can act on. Falls back to a generic status string when parsing fails.
    let message = `Could not load test info (HTTP ${response.status}).`;
    try {
      const body = await response.json();
      if (body && typeof body.error === "string" && body.error.trim()) {
        message = body.error;
      }
    } catch {
      /* response wasn't JSON — keep the generic message */
    }
    throw new Error(message);
  }
  return response.json();
}

/**
 * Update breadcrumb with the orientation trail.
 * Planned test → `Plans > <planName | "Plan"> > <testName | testId>`.
 * Ad-hoc test → `Logs > <testName | testId>`.
 *
 * `planName` is optional and resolved asynchronously from `/api/plan/<id>`
 * (see fetchAndApplyPlanState). Call once before the fetch lands with
 * planName=null for an optimistic render, then a second time once the
 * plan-name is available.
 *
 * @param {any} testInfo - /api/info payload (testName, planId, testId).
 * @param {string | null | undefined} [planName] - Resolved plan name, or
 *   null/undefined to render the literal "Plan" label as a fallback.
 */
function updateBreadcrumb(testInfo, planName) {
  const crumb = document.getElementById("logDetailCrumb");
  if (!crumb) return;
  const terminalLabel = testInfo.testName || testInfo.testId;
  const publicSuffix = isPublic ? "?public=true" : "";
  const items = [];
  if (testInfo.planId) {
    items.push({ label: "Plans", target: "/plans.html" + publicSuffix });
    items.push({
      label: planName || "Plan",
      target:
        "/plan-detail.html?plan=" +
        encodeURIComponent(testInfo.planId) +
        (isPublic ? "&public=true" : ""),
    });
  } else {
    items.push({ label: "Logs", target: "/logs.html" + publicSuffix });
  }
  items.push({ label: terminalLabel, target: "" });
  crumb.items = items;
  if (!crumb.dataset.navWired) {
    crumb.addEventListener("cts-crumb-navigate", (evt) => {
      if (evt.detail && evt.detail.target) {
        window.location.assign(evt.detail.target);
      }
    });
    crumb.dataset.navWired = "true";
  }
}

/** ──────────── /api/plan ──────────── */

/**
 * Fetch the plan record and apply it to:
 *   - the header's `planModules` (forwarded to the nav row's
 *     cts-test-nav-controls → cts-plan-status progress bar) plus the
 *     nav-controls `nextEnabled` flag (Continue Plan visibility)
 *   - the page-level breadcrumb's middle label (planName)
 *   - the post-paint per-sibling /api/info fan-out that colours segments
 * Returns the parsed plan JSON on success, or null on any failure / missing planId.
 *
 * @param {any} testInfo - /api/info payload.
 * @returns {Promise<any | null>} Parsed `/api/plan/<id>` response, or null.
 */
async function fetchAndApplyPlanState(testInfo) {
  if (!testInfo.planId) return null;
  try {
    // Same public threading as fetchTestInfo: anonymous viewers only pass
    // the security filter's public matcher with public=true, and the
    // public branch returns the published PublicPlan (planName included).
    const response = await fetch(
      "/api/plan/" + encodeURIComponent(testInfo.planId) + (isPublic ? "?public=true" : ""),
    );
    if (!response.ok) return null;
    const planData = await response.json();
    if (planData && planData.planName) {
      updateBreadcrumb(testInfo, planData.planName);
    }
    const modules = Array.isArray(planData.modules) ? planData.modules : [];
    cachedPlanModules = modules;

    // Locate the current module in the plan. The /api/plan modules list
    // carries only the *constraint* keys per module (e.g. client_auth_type,
    // response_type), while /api/info's testInfo.variant carries the full
    // resolved variant including plan-level defaults (server_metadata,
    // client_registration). Match by subset — every key the module
    // constrains must equal the test's value; the test may carry extra
    // keys. This both gives Continue Plan a correct `next module` lookup
    // AND removes the silent currentIndex=0 fallback the legacy
    // equality check relied on.
    const thisModuleIndex = modules.findIndex(
      (m) => m.testModule === testInfo.testName && variantsMatch(m.variant, testInfo.variant),
    );
    const safeIndex = thisModuleIndex >= 0 ? thisModuleIndex : 0;

    // Seed the nav row's progress bar from the cached plan modules. Copy
    // each module's `instances` into a fresh array so the fan-out below can
    // mutate the working set without aliasing the cached `/api/plan` data.
    // Segments render instantly (topology + the "you are here" marker +
    // sibling navigation, KTD5/R17); per-sibling status colours arrive from
    // the post-paint /api/info fan-out (R5/R18).
    //
    // `href` is the per-segment navigation target cts-plan-status renders as a
    // real link. Off the public view every sibling with an instance is reachable
    // immediately, so seed its href here for navigation at first paint. On the
    // public view href is withheld until the fan-out confirms the target
    // instance returns 200 (set in resolveOneSegment), so a published-plan viewer
    // never dead-ends on an unpublished sibling.
    const navModules = modules.map((mod) => {
      const instances = Array.isArray(mod.instances) ? mod.instances.slice() : [];
      const entry = { ...mod, instances };
      if (!isPublic) {
        const last = instances.length ? instances[instances.length - 1] : null;
        if (last) entry.href = buildSiblingHref(last);
      }
      return entry;
    });

    /** @type {any} */
    const header = document.getElementById("logDetailHeader");
    if (header) {
      header.planModules = navModules;
    }

    // `nextEnabled` lives on the nav-controls element itself (the header
    // does not bind it as a Lit attribute, so an imperative assignment
    // survives the header's re-renders). Continue Plan shows when a next
    // module exists.
    const navControls = document.querySelector("cts-test-nav-controls");
    if (navControls) {
      navControls.nextEnabled = safeIndex >= 0 && safeIndex + 1 < modules.length;
    }

    // After first paint, colour each sibling segment by fetching its most-
    // recent instance's status (KTD5). Frontend-only, public-flag-threaded,
    // concurrency-capped, and memoized per instance.
    resolveSegmentStatuses(navModules);

    return planData;
  } catch (err) {
    console.warn("[log-detail] /api/plan failed:", err);
    return null;
  }
}

/** ──────────── plan-status segment colouring (KTD5) ──────────── */

/**
 * Memo of resolved `/api/info/<instance>` payloads keyed by instance id, so
 * re-navigating between siblings never refetches a status already in hand.
 * Stores the `{ status, result }` slice (or `null` for a settled 404 / error,
 * which still counts as "resolved" so the segment stops pulsing — R18/KTD3).
 * @type {Map<string, { status?: string, result?: string } | null>}
 */
const segmentStatusMemo = new Map();

/** Max concurrent `/api/info` fan-out requests (KTD5 — bound the burst). */
const SEGMENT_FANOUT_CONCURRENCY = 6;

/**
 * Fetch one sibling module's most-recent-instance status and merge it into
 * the working module entry, setting `_statusResolved` in BOTH the success
 * and the error/404 branches so the segment settles (colours, or falls back
 * to the neutral skip) instead of pulsing pending forever (R18/KTD3). Threads
 * the public flag exactly like the page's other `/api/info` calls.
 *
 * @param {{instances?: string[], status?: string, result?: string,
 *   _statusResolved?: boolean, href?: string}} mod - The working module entry to
 *   mutate. On the public view a 200 means the target instance is publicly
 *   reachable, so its `href` is set and cts-plan-status renders the segment as a
 *   navigable link; a 404/error leaves `href` unset (inert). Off public, `href`
 *   was already seeded at map time, so this only resolves the status colour.
 * @returns {Promise<void>}
 */
async function resolveOneSegment(mod) {
  const instances = Array.isArray(mod.instances) ? mod.instances : [];
  const lastInstance = instances.length ? instances[instances.length - 1] : null;
  if (!lastInstance) return; // never-run module → static skip, no fetch
  if (segmentStatusMemo.has(lastInstance)) {
    const cached = segmentStatusMemo.get(lastInstance);
    if (cached) {
      mod.status = cached.status;
      mod.result = cached.result;
      // A cached 200 means the target instance is publicly reachable, so on the
      // public view the segment becomes a navigable link (R1). A cached 404 is
      // `null`, leaving `href` unset so the segment stays inert (R2). Only set on
      // the public view — off public the href was seeded at map time.
      if (isPublic) mod.href = buildSiblingHref(lastInstance);
    }
    mod._statusResolved = true;
    return;
  }
  try {
    const response = await fetch(
      "/api/info/" + encodeURIComponent(lastInstance) + (isPublic ? "?public=true" : ""),
    );
    if (!response.ok) {
      // Settle without colour (e.g. a 404 for an unpublished sibling). The
      // segment lands on the neutral skip fill rather than pulsing forever.
      segmentStatusMemo.set(lastInstance, null);
      mod._statusResolved = true;
      return;
    }
    const info = await response.json();
    const slice = { status: info.status, result: info.result };
    segmentStatusMemo.set(lastInstance, slice);
    mod.status = slice.status;
    mod.result = slice.result;
    // 200 → the target instance is publicly reachable, so on the public view
    // the segment becomes a navigable link (R1). The 404 branch above leaves
    // `href` unset, so unreachable siblings stay inert (R2). Only set on the
    // public view — off public the href was seeded at map time.
    if (isPublic) mod.href = buildSiblingHref(lastInstance);
    mod._statusResolved = true;
  } catch (err) {
    // Network / parse failure: settle the segment too (R18). Do NOT memoize a
    // transient failure — a later navigation may retry the fetch (and `href`
    // stays unset, so on a public view the segment is inert until that retry
    // succeeds).
    console.warn("[log-detail] segment status fetch failed:", err);
    mod._statusResolved = true;
  }
}

/**
 * Fan out `/api/info/<lastInstance>` per sibling module to colour the
 * plan-status segments after first paint (KTD5). Concurrency is capped via a
 * fixed-size worker pool; each instance is memoized so re-navigating siblings
 * does not refetch. When the pool drains, re-assigns `header.planModules`
 * with a FRESH array so Lit's reference-equality `hasChanged` fires and the
 * pending segments settle to their colours.
 *
 * @param {Array<{instances?: string[], status?: string, result?: string,
 *   _statusResolved?: boolean}>} navModules - The working module set, mutated
 *   in place as each sibling resolves.
 * @returns {Promise<void>}
 */
async function resolveSegmentStatuses(navModules) {
  const queue = navModules.slice();
  async function worker() {
    for (;;) {
      const mod = queue.shift();
      if (!mod) return;
      await resolveOneSegment(mod);
    }
  }
  const poolSize = Math.min(SEGMENT_FANOUT_CONCURRENCY, queue.length);
  await Promise.all(Array.from({ length: poolSize }, worker));

  // Reassign with a fresh array so cts-plan-status observes the change (Lit's
  // default hasChanged is reference equality). A slice suffices — the workers
  // mutated the module objects in place, so the existing element references
  // already carry the resolved status; no per-element copy is needed (mirrors
  // plan-detail.html's fan-out reassign).
  /** @type {any} */
  const header = document.getElementById("logDetailHeader");
  if (header) {
    header.planModules = navModules.slice();
  }
}

/**
 * Build the log-detail URL for a sibling instance, threading the public flag so
 * an anonymous viewer stays in the public view. cts-plan-status renders a
 * segment with this href as a real `<a>` link the browser navigates natively —
 * no event round-trip — so middle-click / Cmd-click / copy-link all work (R15).
 *
 * @param {string} instanceId - The sibling module's most-recent instance id.
 * @returns {string} The `/log-detail.html?log=…` href.
 */
function buildSiblingHref(instanceId) {
  return (
    "/log-detail.html?log=" + encodeURIComponent(instanceId) + (isPublic ? "&public=true" : "")
  );
}

/** ──────────── /api/uploaded-images ──────────── */

async function fetchUploadedImageCount(testInfo) {
  if (isPublic) return; // Public viewers don't see upload affordances.
  try {
    const response = await fetch("/api/uploaded-images");
    if (!response.ok) return;
    const images = await response.json();
    if (!Array.isArray(images)) return;
    const count = images.filter((img) => img && img.testId === testInfo.testId).length;
    if (count > 0 && testInfo.results) {
      // Stamp each result entry with `upload: true` for the count we've
      // observed. The header's _getUploadCount() reads that bit. This
      // keeps the API contract compatible with the existing component
      // without introducing a new property.
      const stamped = testInfo.results.slice();
      for (let i = 0; i < count && i < stamped.length; i++) {
        stamped[i] = { ...stamped[i], upload: true };
      }
      applyTestInfo({ ...testInfo, results: stamped });
    }
  } catch (err) {
    console.warn("[log-detail] /api/uploaded-images failed:", err);
  }
}

/** ──────────── /api/runner — running-test card slot rendering ──────────── */

/**
 * "Has this test reached a verdict?" — true only when /api/info reports
 * a terminal status AND a result has been assigned. Used to gate the
 * polling loop's exit: a test in `{status: "FINISHED", result: null}`
 * is the transient state where the runner has flagged terminal but the
 * verdict hasn't been persisted yet, and polling should continue until
 * the verdict lands. Mirrors cts-log-detail-header's TERMINAL_RESULTS
 * set — REVIEW, WARNING, and SKIPPED all qualify as verdicts.
 * @param {{status?: string, result?: string} | null | undefined} testInfo
 * @returns {boolean}
 */
function isFullyTerminal(testInfo) {
  if (!testInfo) return false;
  const status = (testInfo.status || "").toUpperCase();
  const result = (testInfo.result || "").toUpperCase();
  return (status === "FINISHED" || status === "INTERRUPTED") && result !== "";
}

/**
 * Polls /api/info AND /api/runner on a 3s cadence until the test
 * reaches a fully terminal verdict (status terminal + result set).
 *
 * - /api/info is the source of truth for status / result. Pushing the
 *   fresh payload through `applyTestInfo` re-runs the Lit reactive
 *   render path on cts-log-detail-header, which surfaces the result
 *   pill, the terminal banner, and the lifecycle-driven hero in
 *   lockstep. The header reads test.status / test.result via
 *   `_derivePhase`, so the banner appears for PASSED, FAILED, WARNING,
 *   REVIEW, SKIPPED, and INTERRUPTED — every TERMINAL_BANNER_BY_PHASE
 *   key, not just PASSED.
 * - /api/runner is best-effort and only feeds the in-card slots
 *   ([data-slot="browser"] for the visit-URL prompt and
 *   [data-slot="error"] for the FINAL_ERROR alert). A 404 means the
 *   runner has flushed the test from memory — that's expected on
 *   long-finished tests, not an error condition.
 *
 * The two endpoints are independently fault-tolerant within each
 * cycle: an /api/info hiccup doesn't stop the slot refresh, and an
 * /api/runner outage doesn't stop the verdict refresh. The loop ends
 * only when /api/info confirms a verdict has landed.
 *
 * The polling lives on the page (not inside the Lit header) because
 * the QR-code generator and clipboard.js wiring are page-specific
 * external libraries; the header just exposes the slot positions.
 */
function startRunnerPolling(testInfo) {
  if (isFullyTerminal(testInfo)) return;

  async function pollOnce() {
    /** @type {any} */
    let fresh = null;

    // /api/info — verdict refresh. Fault-isolated so a transient
    // outage doesn't stall the /api/runner slot rendering below.
    try {
      fresh = await fetchTestInfo();
      applyTestInfo(fresh);
    } catch (err) {
      console.warn("[log-detail] /api/info refresh failed:", err);
    }

    // /api/runner — slot rendering. Fault-isolated; 404 is benign
    // (the runner has flushed a long-finished test from memory), so
    // only non-404 non-2xx responses trigger a back-off.
    //
    // Skipped entirely in public mode: /api/runner has no public matcher
    // entry (anonymous GETs 401 on every cycle), and the slots it feeds
    // (visit-URL prompt, FINAL_ERROR alert) are interaction affordances
    // public viewers don't get. The /api/info refresh above keeps live
    // status updates working for public viewers of a running test.
    let runnerFailed = false;
    if (!isPublic) {
      try {
        const response = await fetch("/api/runner/" + encodeURIComponent(testId));
        if (response.status !== 404) {
          if (!response.ok) throw new Error(`HTTP ${response.status}`);
          const data = await response.json();
          renderBrowserSlot(data.browser);
          renderErrorSlot(data.error);
        }
      } catch (err) {
        console.warn("[log-detail] /api/runner failed:", err);
        runnerFailed = true;
      }
    }

    // Stop only when /api/info confirms a verdict. Until then keep
    // polling — this catches every non-terminal lifecycle state
    // (CREATED, RUNNING, WAITING, the transient FINISHED-but-no-result
    // race, INTERRUPTED-but-no-result race) without enumerating them.
    if (isFullyTerminal(fresh)) return;

    const delay = runnerFailed ? POLL_INTERVAL_MS * 2 : POLL_INTERVAL_MS;
    runnerPollState.active = window.setTimeout(pollOnce, delay);
  }

  runnerPollState.active = window.setTimeout(pollOnce, 0);
}

function findSlot(name) {
  return document.querySelector(`cts-log-detail-header [data-slot="${name}"]`);
}

/** Empty a slot's children — used before re-rendering on each poll. */
function clearSlot(slot) {
  while (slot.firstChild) slot.removeChild(slot.firstChild);
}

/**
 * Render the running-test browser-URL prompt into the browser slot.
 * Near-verbatim port of log-detail.html's BROWSER template + handlers,
 * but assembled with DOM methods instead of Underscore string templates.
 */
function renderBrowserSlot(browser) {
  const slot = findSlot("browser");
  if (!slot) return;
  clearSlot(slot);
  if (!browser) return;
  const hasUrls = Array.isArray(browser.urls) && browser.urls.length > 0;
  const hasApiRequests =
    Array.isArray(browser.browserApiRequests) && browser.browserApiRequests.length > 0;
  if (!hasUrls && !hasApiRequests) return;

  const wrapper = document.createElement("div");
  wrapper.className = "v2-browser-wrapper";

  if (hasUrls) {
    const heading = document.createElement("p");
    heading.textContent = "Visit one of the following URLs to interact with the test:";
    wrapper.appendChild(heading);
  }

  for (const entry of hasUrls ? browser.urls : []) {
    const url = typeof entry === "string" ? entry : entry.url;
    const method = typeof entry === "string" ? "GET" : entry.method || "GET";
    if (!url) continue;

    const row = document.createElement("div");
    row.className = "v2-browser-row";
    row.style.display = "flex";
    row.style.flexDirection = "column";
    row.style.gap = "var(--space-2)";
    row.style.marginBottom = "var(--space-3)";

    const urlLabel = document.createElement("code");
    urlLabel.textContent = url;
    urlLabel.style.fontFamily = "var(--font-mono)";
    urlLabel.style.fontSize = "var(--fs-13)";
    urlLabel.style.wordBreak = "break-all";
    row.appendChild(urlLabel);

    const visitBtn = document.createElement("cts-button");
    visitBtn.setAttribute("variant", "primary");
    visitBtn.setAttribute("size", "sm");
    visitBtn.setAttribute("icon", "external-link");
    visitBtn.setAttribute("label", "Visit URL");
    visitBtn.dataset.url = url;
    visitBtn.dataset.method = method;
    visitBtn.addEventListener("cts-click", handleVisitUrl);
    row.appendChild(visitBtn);

    if (browser.show_qr_code) {
      const qrHost = document.createElement("div");
      qrHost.className = "qr";
      qrHost.dataset.url = url;
      qrHost.style.padding = "var(--space-2)";
      qrHost.style.background = "white";
      qrHost.style.display = "inline-block";
      row.appendChild(qrHost);
      // QRCode is loaded as a global by /vendor/qrcode/js/qrcode.min.js.
      // The library mutates the host in-place; safe to call after append.
      if (typeof window.QRCode === "function") {
        // eslint-disable-next-line no-new
        new window.QRCode(qrHost, {
          text: url,
          // eslint-disable-next-line no-undef
          correctLevel: window.QRCode.CorrectLevel.L,
          version: 20,
        });
      }
    }

    wrapper.appendChild(row);
  }

  // browserApiRequests — Digital Credentials API rows (mirrors the
  // legacy `templates/browser.html:27–30` block). Each entry has
  // `{ request, submitUrl }`. The button stays presentational; the page
  // owns navigator.credentials.* and the POST. Wire format is frozen —
  // see `handleVisitBrowserApi` and the Java consumers
  // `ExtractBrowserApiResponse.java` / `ExtractVP1FinalBrowserApiResponse.java`.
  if (Array.isArray(browser.browserApiRequests)) {
    for (const apiReq of browser.browserApiRequests) {
      if (!apiReq || !apiReq.request) continue;
      const requestJson = JSON.stringify(apiReq.request);
      const submitUrl = apiReq.submitUrl || "";

      const row = document.createElement("div");
      row.className = "v2-browser-row";
      row.style.display = "flex";
      row.style.flexDirection = "column";
      row.style.gap = "var(--space-2)";
      row.style.marginBottom = "var(--space-3)";

      const apiBtn = document.createElement("cts-button");
      apiBtn.classList.add("visitBrowserApiBtn");
      apiBtn.setAttribute("variant", "primary");
      apiBtn.setAttribute("size", "sm");
      apiBtn.setAttribute("icon", "paper-plane");
      apiBtn.setAttribute("label", "Proceed with test via browser API (preview)");
      apiBtn.dataset.browserapirequest = requestJson;
      apiBtn.dataset.browserapisubmiturl = submitUrl;
      apiBtn.addEventListener("cts-click", handleVisitBrowserApi);
      row.appendChild(apiBtn);

      const reqLabel = document.createElement("code");
      reqLabel.textContent = requestJson;
      reqLabel.style.fontFamily = "var(--font-mono)";
      reqLabel.style.fontSize = "var(--fs-13)";
      reqLabel.style.wordBreak = "break-all";
      row.appendChild(reqLabel);

      wrapper.appendChild(row);
    }
  }

  slot.appendChild(wrapper);
}

/**
 * Digital Credentials API handler. Wire format is frozen — Java parses it
 * structurally in `ExtractBrowserApiResponse` /
 * `ExtractVP1FinalBrowserApiResponse`. Three branches:
 *   - `submitUrl === ""`: legacy `navigator.credentials.create` path.
 *   - success + DigitalCredential: POST `{data, protocol}`.
 *   - success + non-DigitalCredential: POST `{bad_response_type}`.
 *   - exception: POST `{exception: {name, message}}` and surface via
 *     `showError` (legacy used `alert()` — replaced per the leaf-component
 *     principle's "error chrome at the page level" rule).
 *
 * @param {Event} evt
 */
async function handleVisitBrowserApi(evt) {
  const host = /** @type {HTMLElement} */ (evt.currentTarget);
  /** @type {any} */
  let request;
  try {
    request = JSON.parse(host.dataset.browserapirequest || "");
  } catch (parseErr) {
    showError(`Invalid browser API request payload: ${parseErr.message}`);
    return;
  }
  const submitUrl = host.dataset.browserapisubmiturl || "";

  if (submitUrl === "") {
    // Legacy parity: when no submitUrl is provided, the test wants the
    // wallet to be created (not got). Fire-and-forget; the wallet does
    // its own thing from there.
    try {
      // eslint-disable-next-line compat/compat
      navigator.credentials.create(request);
    } catch (err) {
      showError(`navigator.credentials.create failed: ${err.message}`);
    }
    return;
  }

  /**
   * @param {Record<string, unknown>} body
   */
  const postResult = (body) =>
    fetch(submitUrl, {
      method: "POST",
      body: JSON.stringify(body),
    });

  try {
    // eslint-disable-next-line compat/compat
    const credentialResponse = await navigator.credentials.get(request);
    if (credentialResponse && credentialResponse.constructor.name === "DigitalCredential") {
      /** @type {any} */
      const cred = credentialResponse;
      await postResult({ data: cred.data, protocol: cred.protocol });
    } else {
      await postResult({
        bad_response_type: credentialResponse ? credentialResponse.constructor.name : "null",
      });
    }
  } catch (err) {
    try {
      await postResult({ exception: { name: err.name, message: err.message } });
    } catch (postErr) {
      console.warn("[log-detail] failed to POST browser API exception:", postErr);
    }
    showError(err.message || String(err));
  }
}

async function handleVisitUrl(evt) {
  const host = evt.currentTarget;
  const url = host.dataset.url;
  if (!url) return;
  showBusy(`Opening: ${url}`);
  const win = window.open(url, "_blank");
  if (win) win.focus();
  try {
    const response = await fetch(
      `/api/runner/browser/${encodeURIComponent(testId)}/visit?url=${encodeURIComponent(url)}`,
      { method: "POST" },
    );
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    // Reload to refresh the runner state.
    window.location.reload();
  } catch (err) {
    hideBusy();
    showError(`Failed to mark URL as visited: ${err.message}`);
  }
}

/**
 * Render the FINAL_ERROR alert into the error slot. Construction logic
 * lives in `log-detail-error-slot.js` so Storybook's
 * `WithFinalErrorSlotPopulated` story can call the same code and stay
 * faithful to the live render path.
 */
function renderErrorSlot(error) {
  renderErrorIntoSlot(findSlot("error"), error);
}

/** ──────────── Header action event handlers ──────────── */

function handleEditConfig(evt) {
  const detail = evt.detail || {};
  if (detail.planId) {
    window.location.assign("/schedule-test.html?from-plan=" + encodeURIComponent(detail.planId));
  } else if (detail.testId) {
    window.location.assign("/schedule-test.html?edit-test=" + encodeURIComponent(detail.testId));
  }
}

async function handleShareLink(evt) {
  const eventTestId = (evt.detail && evt.detail.testId) || testId;
  const expirationModal = document.getElementById("privateLinkExpirationModal");
  const resultModal = document.getElementById("privateLinkResultModal");
  if (!expirationModal || !resultModal) return;

  const createBtn = document.getElementById("privateLinkCreateBtn");
  const expirationInput = document.getElementById("privateLinkExpirationDays");

  const onCreate = async () => {
    if (createBtn) createBtn.removeEventListener("click", onCreate);
    const days = expirationInput ? Number(expirationInput.value) || 30 : 30;
    expirationModal.hide();
    showBusy("Creating private link…");
    try {
      const response = await fetch(
        `/api/info/${encodeURIComponent(eventTestId)}/share?exp=${encodeURIComponent(days)}`,
        { method: "POST" },
      );
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      hideBusy();
      const messageEl = document.getElementById("privateLinkMessage");
      const urlEl = document.getElementById("privateLinkUrl");
      if (messageEl) messageEl.textContent = data.message || "";
      if (urlEl) urlEl.value = data.link || "";
      resultModal.show();
    } catch (err) {
      hideBusy();
      showError(`Failed to create private link: ${err.message}`);
    }
  };

  if (createBtn) createBtn.addEventListener("click", onCreate);
  expirationModal.show();
}

async function handlePublish(evt) {
  const detail = evt.detail || {};
  const action = detail.action;
  const mode = detail.mode;
  const eventTestId = detail.testId || testId;
  showBusy(action === "unpublish" ? "Unpublishing…" : "Publishing…");
  try {
    const response = await fetch(`/api/info/${encodeURIComponent(eventTestId)}/publish`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ publish: action === "unpublish" ? "" : mode || "everything" }),
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    // Reload the page to pick up the new publish state. Send admin
    // through ?public=true after publishing so they immediately see
    // the public read-only view (matches legacy behaviour).
    const next =
      action === "publish"
        ? `/log-detail.html?log=${encodeURIComponent(eventTestId)}&public=true`
        : `/log-detail.html?log=${encodeURIComponent(eventTestId)}`;
    window.location.assign(next);
  } catch (err) {
    hideBusy();
    showError(`Failed to ${action}: ${err.message}`);
  }
}

function handleUploadImages(evt) {
  const eventTestId = (evt.detail && evt.detail.testId) || testId;
  window.location.assign("/upload.html?test=" + encodeURIComponent(eventTestId));
}

function handleDownloadLog(evt) {
  const eventTestId = (evt.detail && evt.detail.testId) || testId;
  // The export route is /api/log/export/{id} (LogApi.java) — same order
  // plan-detail.html uses. The swapped /api/log/{id}/export form has no
  // server-side route and 404s.
  window.location.assign(
    "/api/log/export/" + encodeURIComponent(eventTestId) + (isPublic ? "?public=true" : ""),
  );
}

async function handleStartTest(evt) {
  const eventTestId = (evt.detail && evt.detail.testId) || testId;
  showBusy("Starting test…");
  try {
    const response = await fetch(`/api/runner/${encodeURIComponent(eventTestId)}`, {
      method: "POST",
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    window.location.reload();
  } catch (err) {
    hideBusy();
    showError(`Failed to start test: ${err.message}`);
  }
}

async function handleStopTest(evt) {
  const eventTestId = (evt.detail && evt.detail.testId) || testId;
  showBusy("Stopping test…");
  try {
    const response = await fetch(`/api/runner/${encodeURIComponent(eventTestId)}`, {
      method: "DELETE",
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    window.location.reload();
  } catch (err) {
    hideBusy();
    showError(`Failed to stop test: ${err.message}`);
  }
}

/**
 * Build the /api/runner POST URL the same way plan-detail.html does
 * (its `buildRunnerUrl` is the single source of truth for query-param
 * shape). `test` is the module name (NOT the runtime test ID); `plan`
 * is optional; `variant` is pass-through when string, JSON when object,
 * omitted when nullish.
 *
 * @param {string} testName
 * @param {string | null | undefined} planId
 * @param {object | string | null | undefined} variant
 * @returns {string}
 */
function buildRunnerUrl(testName, planId, variant) {
  let url = `/api/runner?test=${encodeURIComponent(testName)}`;
  if (planId) {
    url += `&plan=${encodeURIComponent(planId)}`;
  }
  if (variant !== null && variant !== undefined) {
    const encoded = typeof variant === "string" ? variant : JSON.stringify(variant);
    if (encoded && encoded !== "{}") {
      url += `&variant=${encodeURIComponent(encoded)}`;
    }
  }
  return url;
}

/**
 * Surface a runner-API failure with the server's own error message
 * when the body carries one ({error: "..."}), mirroring plan-detail.html's
 * handleApiError. Falls back to the generic statusText. Returns the
 * resolved message for the caller's showError().
 *
 * @param {Response} response
 * @returns {Promise<string>}
 */
async function readRunnerError(response) {
  try {
    const body = await response.json();
    if (body && typeof body.error === "string" && body.error.trim()) {
      return body.error;
    }
    if (body && typeof body.message === "string" && body.message.trim()) {
      return body.message;
    }
  } catch {
    /* not JSON — fall through */
  }
  return `HTTP ${response.status} ${response.statusText || ""}`.trim();
}

async function handleRepeat() {
  // Resolve testName + variant from the latest /api/info payload — the
  // event detail only carries the runtime test ID, but the runner
  // endpoint wants the module name (e.g. `oidcc-server`) in `test=`.
  // Passing the runtime ID is what caused the 404/400 modal Thomas
  // reported (A3).
  const info = latestTestInfo;
  if (!info || !info.testName) {
    showError("Test info hasn't loaded yet — wait a moment and try again.");
    return;
  }
  showBusy("Repeating test…");
  try {
    const url = buildRunnerUrl(info.testName, info.planId || null, info.variant || null);
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
    });
    if (!response.ok) {
      const message = await readRunnerError(response);
      throw new Error(message);
    }
    const data = await response.json();
    if (data && data.id) {
      window.location.assign(`/log-detail.html?log=${encodeURIComponent(data.id)}`);
    } else {
      window.location.reload();
    }
  } catch (err) {
    hideBusy();
    showError(`Failed to repeat test: ${err.message}`);
  }
}

/**
 * True when every key the module's variant constrains is present and
 * equal in the test's full resolved variant. The plan's per-module
 * variant carries only constraint keys; the test's variant carries
 * those plus plan-level defaults — so the relationship is subset, not
 * equality. Used by both the in-plan currentIndex lookup and the
 * Continue Plan next-module lookup, so the two stay consistent.
 *
 * @param {object | undefined | null} moduleVariant - the plan-module variant (subset)
 * @param {object | undefined | null} testVariant - the test's full resolved variant
 */
function variantsMatch(moduleVariant, testVariant) {
  const mv = moduleVariant || {};
  const tv = testVariant || {};
  for (const key of Object.keys(mv)) {
    if (mv[key] !== tv[key]) return false;
  }
  return true;
}

async function handleContinue() {
  const info = latestTestInfo;
  if (!info || !info.planId || !info.testName) return;
  // Find the *next* module in the plan. The runner endpoint's `test=`
  // param wants a module name, so Continue Plan can't just POST
  // `?plan=...` — that produces the "Required parameter 'test' is not
  // present" 400 Thomas reported. Reuse the plan modules already
  // fetched by fetchAndApplyPlanState (cached at module scope) to find
  // the next entry without a second roundtrip.
  const currentIndex = cachedPlanModules.findIndex(
    (m) => m.testModule === info.testName && variantsMatch(m.variant, info.variant),
  );
  const next = currentIndex >= 0 ? cachedPlanModules[currentIndex + 1] : null;
  if (!next || !next.testModule) {
    showError("No next module found in this plan.");
    return;
  }
  showBusy("Continuing to next module…");
  try {
    const url = buildRunnerUrl(next.testModule, info.planId, next.variant || null);
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
    });
    if (!response.ok) {
      const message = await readRunnerError(response);
      throw new Error(message);
    }
    const data = await response.json();
    if (data && data.id) {
      window.location.assign(`/log-detail.html?log=${encodeURIComponent(data.id)}`);
    } else {
      window.location.reload();
    }
  } catch (err) {
    hideBusy();
    showError(`Failed to continue plan: ${err.message}`);
  }
}

/** ──────────── Document-level cts-scroll-to-entry ──────────── */

function handleScrollToEntry(evt) {
  const entryId = evt.detail && evt.detail.entryId;
  if (!entryId) return;
  // Find the entry by its server-side _id. cts-log-entry exposes the id
  // via `data-entry-id` so this query is stable across U6 (R32 reference
  // IDs) and U8 (TOC rail) — those features layer additional anchors but
  // do not break the legacy _id contract.
  const target = document.querySelector(
    `cts-log-entry[data-entry-id="${entryId.replace(/"/g, '\\"')}"]`,
  );
  if (!target) return;
  // Blocks are non-collapsible (always-rendered .logBlock divs), so the
  // entry is already in the layout — scroll straight to it with no
  // collapsed-ancestor reveal step. scrollEntryIntoView handles the wide
  // layout where the host is display:contents (boxless — a bare
  // scrollIntoView would silently no-op) by scrolling the painted
  // .logItem row instead.
  scrollEntryIntoView(target, { behavior: "smooth", block: "start" });
}

/**
 * U8 — handle a click on a cts-log-toc rail row. The rail dispatches
 * `cts-scroll-to-block` with `{ blockId }`; the matching `.logBlock` in the
 * entries stream scrolls into view so the block header is the visible anchor.
 * Blocks are non-collapsible, so there is no open step — the block is always
 * in the layout.
 *
 * @param {Event} evt
 */
function handleScrollToBlock(evt) {
  const detail = /** @type {CustomEvent} */ (evt).detail || {};
  const blockId = detail.blockId;
  if (!blockId) return;
  const target = /** @type {HTMLElement | null} */ (
    document.querySelector(`.logBlock[data-block-id="${blockId.replace(/"/g, '\\"')}"]`)
  );
  if (!target) return;
  target.scrollIntoView({ behavior: "smooth", block: "start" });
}

/**
 * U8 — wire the wide-viewport rail. Adds the `--with-toc` modifier on
 * <main> so the page grid reserves the rail column; the responsive CSS
 * (≥ 1440px) and the rail's own empty-data `hidden` attribute gate whether
 * the column actually paints.
 *
 * Listens for `cts-blocks-updated` from cts-log-viewer so the rail's
 * blocks array re-syncs with each polling cycle.
 */
function setupLogToc() {
  /** @type {any} */
  const rail = document.getElementById("ctsLogToc");
  if (!rail) return;
  const main = document.getElementById("main-content");
  if (main) {
    main.classList.add("log-page--with-toc");
  }
  document.addEventListener("cts-scroll-to-block", handleScrollToBlock);
  document.addEventListener("cts-blocks-updated", (evt) => {
    const blocks = /** @type {CustomEvent} */ (evt).detail && evt.detail.blocks;
    if (Array.isArray(blocks)) rail.blocks = blocks;
  });
}

/**
 * Read the `--dur-3` motion token (e.g. "280ms") off the page root and return
 * it in milliseconds, so the JS `inert` settle stays in lockstep with the CSS
 * collapse transition without hard-coding the duration. Falls back to 280 if
 * the token is unreadable.
 *
 * @returns {number}
 */
function readMotionDurationMs() {
  const raw = getComputedStyle(document.documentElement).getPropertyValue("--dur-3").trim();
  const value = parseFloat(raw);
  if (Number.isNaN(value)) return 280;
  // Tokens are authored in ms ("280ms"); tolerate a seconds form ("0.28s").
  return raw.endsWith("ms") ? value : value * 1000;
}

/**
 * feat/log-toc-collapsible — wire the Test-structure rail collapse toggle.
 *
 * The collapsed/expanded choice persists in `localStorage` (global, one key)
 * and is applied PRE-PAINT by the inline `<head>` script in log-detail.html
 * (which sets `documentElement.classList.toc-collapsed` before first paint so a
 * collapsed-by-preference load never shows an expanded frame — R6). This
 * function only re-syncs the toggle's label + `aria-expanded` to that pre-paint
 * state, manages the rail's `inert` lifecycle, persists user toggles, and keeps
 * keyboard focus off the collapsed rail.
 *
 * Named (not an inline closure in bootstrap) so a future keyboard shortcut can
 * call the same toggle handler — see the plan's "Deferred to follow-up work".
 *
 * R10 (suppress the toggle when the rail is empty) is handled declaratively in
 * the page CSS via `#main-content:has(#ctsLogToc[hidden]) .log-toc-toggle-slot`,
 * NOT here: the rail's `[hidden]` state is set inside cts-log-toc's async Lit
 * `updated()` cycle, so a synchronous read on `cts-blocks-updated` would race
 * it. The `:has()` selector tracks the attribute reactively with no timing
 * coupling.
 */
function setupTocCollapse() {
  /** @type {any} */
  const toggle = document.getElementById("ctsLogTocToggle");
  /** @type {HTMLElement | null} */
  const rail = document.getElementById("ctsLogToc");
  const root = document.documentElement;
  if (!toggle || !rail) return;

  /** The cts-tooltip wrapping the toggle (hover/focus discoverability). */
  const tooltip = toggle.closest("cts-tooltip");

  const storage = tryGetStorage("localStorage");
  const motionDurationMs = readMotionDurationMs();

  /**
   * Pending timeout id for the duration-matched `inert` settle. Cleared on
   * every toggle so a rapid collapse→expand never re-applies `inert` to a
   * now-visible rail.
   * @type {number}
   */
  let inertTimer = 0;

  const prefersReducedMotion = () =>
    window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  /**
   * Sync the toggle's accessible label, `aria-expanded` (forwarded by
   * cts-button onto the inner button), and hover-tooltip text to the given
   * collapsed state. The button is icon-only, so the action lives in
   * `aria-label` + the tooltip, not visible text. Driven through `setAttribute`
   * so the host attributes stay consistent with the rendered inner button.
   * @param {boolean} collapsed
   */
  function syncToggle(collapsed) {
    const action = collapsed ? "Show test structure" : "Hide test structure";
    toggle.setAttribute("aria-expanded", collapsed ? "false" : "true");
    toggle.setAttribute("aria-label", action);
    if (tooltip) tooltip.setAttribute("content", action);
  }

  // Mirror the pre-paint state onto the toggle + the rail's inert state without
  // animating (the animate class is enabled one frame later, below).
  const initialCollapsed = root.classList.contains("toc-collapsed");
  syncToggle(initialCollapsed);
  rail.inert = initialCollapsed;

  // Enable the collapse transition one frame after first paint so the
  // load-time state applies instantly and only later user toggles animate (R6).
  requestAnimationFrame(() => {
    const main = document.getElementById("main-content");
    if (main) main.classList.add("log-page--toc-animate");
  });

  toggle.addEventListener("cts-click", () => {
    // Clear any pending inert settle so an interrupted collapse→expand never
    // re-applies inert to a visible rail.
    if (inertTimer) {
      clearTimeout(inertTimer);
      inertTimer = 0;
    }

    const willCollapse = !root.classList.contains("toc-collapsed");

    if (willCollapse) {
      // R11 — never silently drop focus to <body>: move focus to the toggle
      // BEFORE the rail becomes inert if focus currently sits inside the rail.
      // The focus target is cts-button's inner <button> (light DOM); the host
      // itself is not focusable, so guard the query rather than focusing the
      // host. Via the real triggers the inner button is always rendered by now
      // (a pointer click already moved focus onto it; a future keyboard
      // shortcut fires on a fully-loaded page), so this is defensive.
      if (rail.contains(document.activeElement)) {
        const innerBtn = toggle.querySelector("button");
        if (innerBtn) innerBtn.focus();
      }
      root.classList.add("toc-collapsed");
    } else {
      // Expand — restore focus-reachability synchronously.
      rail.inert = false;
      root.classList.remove("toc-collapsed");
    }

    syncToggle(willCollapse);

    // A tooltip shown from the pre-click hover keeps its old text (cts-tooltip
    // only re-reads `content` on the next show), which would read e.g. "Hide
    // test structure" over an already-hidden rail. Dismiss it via the trigger's
    // own hide event; the next hover/focus shows the updated action.
    if (tooltip) toggle.dispatchEvent(new MouseEvent("mouseleave"));

    if (willCollapse) {
      if (prefersReducedMotion()) {
        // No animation, so settle inert immediately (R5 + R9).
        rail.inert = true;
      } else {
        // Settle inert once the collapse animation has finished. Driven off a
        // duration-matched timeout, NOT `transitionend` for the registered
        // custom property (engine-inconsistent; see the plan's KTDs).
        inertTimer = window.setTimeout(() => {
          rail.inert = true;
          inertTimer = 0;
        }, motionDurationMs);
      }
    }

    // Persist the choice. Degrades to session-only (no-op) when storage is
    // unavailable, so the in-session toggle still works (R7).
    if (storage) {
      try {
        storage.setItem(TOC_COLLAPSE_STORAGE_KEY, willCollapse ? "true" : "false");
      } catch {
        /* storage became unavailable mid-session — ignore, stay in-session */
      }
    }
  });
}

/** ──────────── Keyboard shortcuts ──────────── */

function handleKeydown(event) {
  // navigator.platform is deprecated, but its modern replacement
  // (navigator.userAgentData.platform) ships only in Chromium today and
  // the legacy page we mirror here uses navigator.platform. Falling
  // back to userAgent string-matching gives Safari/Firefox coverage
  // without introducing a divergence in keyboard-shortcut behaviour
  // between the two pages during the rollout window.
  // eslint-disable-next-line deprecation/deprecation -- see comment above
  const legacyPlatform = /** @type {string} */ (/** @type {any} */ (navigator).platform || "");
  const platform =
    (navigator.userAgentData && navigator.userAgentData.platform) ||
    legacyPlatform ||
    navigator.userAgent ||
    "";
  const isMac = platform.toUpperCase().indexOf("MAC") >= 0;
  const isModifier = isMac ? event.metaKey : event.ctrlKey;
  if (!isModifier || !event.shiftKey) return;
  const key = event.key.toLowerCase();
  if (key === "x") {
    event.preventDefault();
    // The Repeat button lives in the status bar primary slot now —
    // cts-test-nav-controls' "Repeat Test" was removed once the status
    // bar took over the affordance. Both clicks dispatch the same
    // cts-repeat-test event the page already wires through handleRepeat.
    const inner = document.querySelector(
      'cts-log-detail-header [data-testid="status-bar-primary"] button',
    );
    if (inner) inner.click();
  } else if (key === "u") {
    event.preventDefault();
    const inner = document.querySelector(
      'cts-test-nav-controls [data-testid="continue-btn"] button',
    );
    if (inner) inner.click();
  }
}

/** ──────────── Entry point ──────────── */

async function bootstrap() {
  readUrlParams();
  if (!testId) {
    showError("Missing `log` query parameter.");
    return;
  }

  document.addEventListener("cts-scroll-to-entry", handleScrollToEntry);
  document.addEventListener("keydown", handleKeydown);
  setupLogToc();
  setupTocCollapse();
  // U6: cts-log-viewer dispatches cts-references-updated after each
  // successful poll that appended rows. Forward the map to every
  // failure summary instance so chips render in lockstep with the
  // entries stream.
  document.addEventListener("cts-references-updated", (evt) => {
    const refs = /** @type {CustomEvent} */ (evt).detail && evt.detail.references;
    if (refs) applyReferences(refs);
  });

  await fetchCurrentUser();

  /** @type {any} */
  const header = document.getElementById("logDetailHeader");
  /** @type {any} */
  const viewer = document.getElementById("logViewer");

  if (header) {
    header.isAdmin = isAdmin;
    header.isPublic = isPublic;
    // The instance being viewed drives the plan-status "you are here"
    // marker + "Module N of M" label in the nav row's progress bar (R14/
    // R17). Set it before /api/plan resolves so the marker lands as soon
    // as the modules arrive.
    header.currentInstanceId = testId;
    header.addEventListener("cts-edit-config", handleEditConfig);
    header.addEventListener("cts-share-link", handleShareLink);
    header.addEventListener("cts-publish", handlePublish);
    header.addEventListener("cts-upload-images", handleUploadImages);
    header.addEventListener("cts-download-log", handleDownloadLog);
    header.addEventListener("cts-start-test", handleStartTest);
    header.addEventListener("cts-stop-test", handleStopTest);
    header.addEventListener("cts-repeat-test", handleRepeat);
    // cts-test-nav-controls only bubbles cts-continue now — the
    // duplicate "Repeat Test" button was removed; the status bar
    // primary owns the cts-repeat-test event by itself.
    header.addEventListener("cts-continue", handleContinue);
    // R15: progress segments are real links now — each reachable sibling's
    // cts-plan-status segment carries the href built by buildSiblingHref (set in
    // navModules off public, and after the fan-out confirms reachability on
    // public), so clicking navigates natively. No cts-plan-status-activate
    // listener: the component does not emit it in log mode.
  }

  let testInfo;
  try {
    testInfo = await fetchTestInfo();
  } catch (err) {
    showError(err.message);
    return;
  }

  applyTestInfo(testInfo);
  updateBreadcrumb(testInfo);

  if (viewer) {
    // isPublic MUST be assigned in the same synchronous block as testId
    // (conventionally first) — the testId assignment triggers the first
    // /api/log fetch, which must already carry public=true for anonymous
    // viewers. Never defer the isPublic assignment past an await.
    viewer.isPublic = isPublic;
    viewer.testInfo = testInfo;
    viewer.testId = testId;
  }

  // Plan + uploaded-images run in parallel — neither blocks the other.
  await Promise.all([fetchAndApplyPlanState(testInfo), fetchUploadedImageCount(testInfo)]);

  startRunnerPolling(testInfo);
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", bootstrap);
} else {
  bootstrap();
}
