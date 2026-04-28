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

const POLL_INTERVAL_MS = 3000;

/** @type {string} */
let testId = "";
/** @type {boolean} */
let isPublic = false;
/** @type {boolean} */
let isAdmin = false;

/** @type {{ active: number | null }} */
const runnerPollState = { active: null };

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
    throw new Error(`/api/info returned ${response.status}`);
  }
  return response.json();
}

/** Update breadcrumb with plan / logs root + this test's ID. */
function updateBreadcrumb(testInfo) {
  const crumb = document.getElementById("logDetailCrumb");
  if (!crumb) return;
  const items = [];
  if (testInfo.planId) {
    items.push({
      label: "Plan",
      target:
        "/plan-detail.html?plan=" +
        encodeURIComponent(testInfo.planId) +
        (isPublic ? "&public=true" : ""),
    });
  } else {
    items.push({ label: "Logs", target: "/logs.html" });
  }
  items.push({ label: testInfo.testId, target: "" });
  crumb.items = items;
  crumb.addEventListener("cts-crumb-navigate", (evt) => {
    if (evt.detail && evt.detail.target) {
      window.location.assign(evt.detail.target);
    }
  });
}

/** ──────────── /api/plan ──────────── */

async function fetchAndApplyPlanState(testInfo) {
  if (!testInfo.planId) return;
  try {
    const response = await fetch("/api/plan/" + encodeURIComponent(testInfo.planId));
    if (!response.ok) return;
    const planData = await response.json();
    const modules = Array.isArray(planData.modules) ? planData.modules : [];

    // Locate the current module in the plan. The legacy page matches by
    // testName + variant equality; we mirror that. `_.findIndex` from
    // lodash isn't available here so we use Array.findIndex with a
    // shallow-equal helper.
    const variant = testInfo.variant || {};
    const thisModuleIndex = modules.findIndex((m) => {
      if (m.testModule !== testInfo.testName) return false;
      const mv = m.variant || {};
      const keys = new Set([...Object.keys(variant), ...Object.keys(mv)]);
      for (const key of keys) {
        if (variant[key] !== mv[key]) return false;
      }
      return true;
    });

    const navControls = document.querySelector("cts-test-nav-controls");
    if (!navControls) return;

    const safeIndex = thisModuleIndex >= 0 ? thisModuleIndex : 0;
    navControls.currentIndex = safeIndex;
    navControls.totalCount = modules.length;
    navControls.nextEnabled = safeIndex >= 0 && safeIndex + 1 < modules.length;
  } catch (err) {
    console.warn("[log-detail] /api/plan failed:", err);
  }
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
 * Polls `/api/runner/:testId` for live state on running / waiting tests.
 * Renders `data.browser` into the header's [data-slot="browser"] and
 * `data.error` into [data-slot="error"]. Stops once the runner reports
 * a non-active state and the slots are flushed accordingly.
 *
 * The polling lives on the page (not inside the Lit header) because the
 * QR-code generator and clipboard.js wiring are page-specific external
 * libraries; the header just exposes the slot positions.
 */
function startRunnerPolling(testInfo) {
  const status = (testInfo.status || "").toUpperCase();
  if (status !== "RUNNING" && status !== "WAITING" && status !== "INTERRUPTED") return;

  async function pollOnce() {
    try {
      const response = await fetch("/api/runner/" + encodeURIComponent(testId));
      if (response.status === 404) {
        // Runner no longer holds state for this test — mirror the legacy
        // page's archived-banner trigger (`log-detail.html:1662–1664`).
        setArchivedBanner(true);
        return;
      }
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      renderBrowserSlot(data.browser);
      renderErrorSlot(data.error);
      // Continue polling while the runner is still active.
      const runnerStatus = (data.status || "").toUpperCase();
      if (runnerStatus === "RUNNING" || runnerStatus === "WAITING") {
        setArchivedBanner(false);
        runnerPollState.active = window.setTimeout(pollOnce, POLL_INTERVAL_MS);
      } else {
        // Runner has reported a terminal state (FINISHED / INTERRUPTED) —
        // legacy hid the archived banner here too because the runner still
        // owned the record. Keep parity.
        setArchivedBanner(false);
      }
    } catch (err) {
      console.warn("[log-detail] /api/runner failed:", err);
      // Back off and retry once.
      runnerPollState.active = window.setTimeout(pollOnce, POLL_INTERVAL_MS * 2);
    }
  }

  function setArchivedBanner(archived) {
    /** @type {any} */
    const header = document.getElementById("logDetailHeader");
    if (header) header.archived = !!archived;
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
    window.location.assign("/schedule-test.html?edit-plan=" + encodeURIComponent(detail.planId));
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
  window.location.assign(
    "/api/log/" + encodeURIComponent(eventTestId) + "/export" + (isPublic ? "?public=true" : ""),
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

async function handleRepeat(evt) {
  const detail = evt.detail || {};
  const eventTestId = detail.testId || testId;
  const planId = detail.planId;
  showBusy("Repeating test…");
  try {
    const url = planId
      ? `/api/runner?test=${encodeURIComponent(eventTestId)}&plan=${encodeURIComponent(planId)}`
      : `/api/runner?test=${encodeURIComponent(eventTestId)}`;
    const response = await fetch(url, { method: "POST" });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
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

async function handleContinue(evt) {
  // cts-test-nav-controls' Continue Plan button: launches the next
  // module in the plan. Same /api/runner POST shape as Repeat with the
  // plan progression delta handled server-side.
  const detail = evt.detail || {};
  const planId = detail.planId;
  if (!planId) return;
  showBusy("Continuing to next module…");
  try {
    const response = await fetch(`/api/runner?plan=${encodeURIComponent(planId)}`, {
      method: "POST",
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
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
  // U5 will swap block headers from <button> to <details> with `open`
  // toggling. Walking up to the nearest <details> ancestor lets the
  // scroll target reveal itself when the block is collapsed.
  let ancestor = target.parentElement;
  while (ancestor && ancestor.tagName !== "DETAILS") ancestor = ancestor.parentElement;
  if (ancestor) ancestor.open = true;
  target.scrollIntoView({ behavior: "smooth", block: "start" });
}

/**
 * U8 — handle a click on a cts-log-toc rail row. The rail dispatches
 * `cts-scroll-to-block` with `{ blockId }`; the matching <details> in the
 * entries stream opens before scrolling so the row above the block is
 * the visible anchor instead of the block header. Mirrors the
 * scroll-to-entry handler's open-on-collapse contract.
 *
 * @param {Event} evt
 */
function handleScrollToBlock(evt) {
  const detail = /** @type {CustomEvent} */ (evt).detail || {};
  const blockId = detail.blockId;
  if (!blockId) return;
  const target = /** @type {HTMLDetailsElement | null} */ (
    document.querySelector(`details.logBlock[data-block-id="${blockId.replace(/"/g, '\\"')}"]`)
  );
  if (!target) return;
  target.open = true;
  target.scrollIntoView({ behavior: "smooth", block: "start" });
}

/**
 * U8 — wire the wide-viewport rail. Reads the user preference once at
 * bootstrap and adds the `--with-toc` modifier on <main> when enabled.
 * The rail itself owns its own `display: none` toggle (so a future
 * setEnabled() call from the U7 overflow can flip visibility without
 * re-mounting), but the page-level grid switch only happens here.
 *
 * Listens for `cts-blocks-updated` from cts-log-viewer so the rail's
 * blocks array re-syncs with each polling cycle.
 */
function setupLogToc() {
  /** @type {any} */
  const rail = document.getElementById("ctsLogToc");
  if (!rail) return;
  let preferenceEnabled = true;
  try {
    preferenceEnabled = localStorage.getItem("cts-log-toc-rail-enabled") !== "false";
  } catch {
    preferenceEnabled = true;
  }
  const main = document.getElementById("main-content");
  if (preferenceEnabled && main) {
    main.classList.add("log-page--with-toc");
  }
  document.addEventListener("cts-scroll-to-block", handleScrollToBlock);
  document.addEventListener("cts-blocks-updated", (evt) => {
    const blocks = /** @type {CustomEvent} */ (evt).detail && evt.detail.blocks;
    if (Array.isArray(blocks)) rail.blocks = blocks;
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
