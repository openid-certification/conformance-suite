import { html } from "lit";
import { expect, within, waitFor, userEvent, spyOn } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_PLAN_LIST, MOCK_PLAN_INFO } from "@fixtures/mock-plans.js";
import "./cts-plan-list.js";

export default {
  title: "Pages/cts-plan-list",
  component: "cts-plan-list",
};

// --- Helpers ---

async function waitForPlansToLoad(canvasElement) {
  await waitFor(
    () => {
      const spinner = canvasElement.querySelector(".spinner-border");
      expect(spinner).toBeNull();
    },
    { timeout: 3000 },
  );
}

/**
 * Resolve the inner `<button>` rendered inside a cts-button host with the
 * given selector. Required because cts-button renders to its own light DOM
 * and Lit binds `@click` on the inner `<button>` (a click on the host does
 * not fire the inner handler — see components/AGENTS.md §2).
 *
 * @param {Element | null | undefined} host
 * @returns {HTMLButtonElement}
 */
function innerButton(host) {
  if (!host) throw new Error("innerButton: host element is null");
  const btn = host.querySelector("button");
  if (!btn) throw new Error("innerButton: no <button> inside host");
  return /** @type {HTMLButtonElement} */ (btn);
}

// An /api/info handler that never resolves, so module status boxes stay in
// their initial `pending` state for deterministic assertions. Used by the
// pending-box story; the resolution stories use instance-keyed handlers.
const neverResolvingInfo = http.get(
  "/api/info/:testId",
  () => new Promise(() => {}), // intentionally never settles
);

/**
 * Read the status-box color variant for a module from its tooltip-wrapped box
 * (the box is keyed by the module id in the tooltip's `content`). Returns the
 * variant suffix (e.g. "pass") or null when no box/variant is present.
 * @param {ParentNode} root
 * @param {string} moduleId
 * @returns {string|null}
 */
function boxVariant(root, moduleId) {
  const box = root.querySelector(`cts-tooltip[content="${moduleId}"] .moduleStatusBox`);
  if (!box) return null;
  const cls = [...box.classList].find((c) => c.startsWith("moduleStatusBox--"));
  return cls ? cls.replace("moduleStatusBox--", "") : null;
}

/**
 * Build an instance-keyed `/api/info/:testId` handler. Returns the per-instance
 * `{ status, result }` from `infoMap` so module dots resolve to distinct
 * colors; an unknown id 404s (exercising the fail-soft → skip path). Pass a
 * `requested` array to record which instance ids were fetched (used to assert
 * the visible-card fetch gate and that no-instance modules trigger no fetch).
 *
 * @param {Record<string, {status: string, result: string}>} [infoMap]
 * @param {string[]} [requested]
 */
function infoHandler(infoMap = MOCK_PLAN_INFO, requested) {
  return http.get("/api/info/:testId", ({ params }) => {
    const id = /** @type {string} */ (params.testId);
    if (requested) requested.push(id);
    const info = infoMap[id];
    if (!info) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json(info);
  });
}

// --- Stories ---

export const Default = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST)),
        neverResolvingInfo,
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    // The search + sort toolbar mirrors cts-log-list.
    expect(canvasElement.querySelector(".cts-plan-list-search input")).toBeTruthy();
    expect(canvasElement.querySelector(".cts-plan-list-sort select")).toBeTruthy();

    // One card per plan.
    const cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
    expect(cards.length).toBe(MOCK_PLAN_LIST.length);

    // Plan names render.
    expect(canvas.getByText("oidcc-basic-certification-test-plan")).toBeInTheDocument();
    expect(canvas.getByText("fapi2-security-profile-final-test-plan")).toBeInTheDocument();

    // Plan-name anchors carry the real destination URL (not "#") so cmd-click,
    // middle-click, right-click "Open in new tab", browser hover preview, and
    // screen-reader destination announcement all work. Target plan-001 by id
    // rather than DOM position — the default sort is Started (newest), so the
    // first card is not necessarily the first fixture entry.
    const planCard = canvasElement.querySelector(
      '[data-testid="plan-list-item"][data-plan-id="plan-001"]',
    );
    expect(planCard).toBeTruthy();
    const planLink = /** @type {HTMLAnchorElement | null} */ (
      planCard.querySelector("a.plan-name-link")
    );
    expect(planLink?.getAttribute("href")).toBe("plan-detail.html?plan=plan-001");

    // The plan id renders as the card slug.
    expect(planCard.querySelector(".cts-plan-card-slug")?.textContent).toBe("plan-001");

    // Each module renders one color-coded status box — one per module across
    // all cards.
    const totalModules = MOCK_PLAN_LIST.reduce((n, p) => n + (p.modules?.length || 0), 0);
    const boxes = canvasElement.querySelectorAll(".moduleStatusGrid .moduleStatusBox");
    expect(boxes.length).toBe(totalModules);

    // The Started value renders through cts-time: a native <time> whose title
    // carries the full absolute date on hover.
    const startedTime = planCard.querySelector("cts-time time");
    expect(startedTime).toBeTruthy();
    expect(startedTime?.getAttribute("datetime")).toBeTruthy();

    // Non-admin users do not see the owner pill.
    expect(canvasElement.querySelector(".plan-owner")).toBeNull();
  },
};

export const SearchAndSort = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST)),
        neverResolvingInfo,
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    let cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
    expect(cards.length).toBe(MOCK_PLAN_LIST.length);

    // Search narrows the rendered cards client-side.
    const searchInput = canvasElement.querySelector('input[placeholder="Search test plans..."]');
    expect(searchInput).toBeTruthy();
    await userEvent.type(searchInput, "fapi2");
    await waitFor(() => {
      cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
      expect(cards.length).toBe(1);
    });
    expect(canvas.getByText("fapi2-security-profile-final-test-plan")).toBeInTheDocument();
    expect(canvas.queryByText("oidcc-basic-certification-test-plan")).toBeNull();

    // Clear the search, then sort by plan name (A–Z) and confirm the first
    // card is the alphabetically-first plan.
    await userEvent.clear(searchInput);
    const sortSelect = /** @type {HTMLSelectElement} */ (
      canvasElement.querySelector(".cts-plan-list-sort select")
    );
    await userEvent.selectOptions(sortSelect, "name-asc");
    await waitFor(() => {
      const names = Array.from(
        canvasElement.querySelectorAll('[data-testid="plan-list-item"] .cts-plan-card-name'),
      ).map((el) => el.textContent);
      const sorted = [...names].sort((a, b) => a.localeCompare(b));
      expect(names).toEqual(sorted);
    });
  },
};

export const ClickPlanName = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST)),
        neverResolvingInfo,
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    let receivedPlanId = null;
    canvasElement.addEventListener("cts-plan-navigate", (e) => {
      receivedPlanId = e.detail.planId;
    });

    const planLink = canvas.getByText("oidcc-basic-certification-test-plan");
    await userEvent.click(planLink);

    expect(receivedPlanId).toBe("plan-001");
  },
};

/**
 * Modifier-key clicks (cmd/ctrl/shift/alt) and non-primary mouse buttons must
 * NOT trigger the custom `cts-plan-navigate` event — the browser handles those
 * natively by following the anchor's real href (e.g., opening in a new tab).
 * The component's `_handlePlanLinkClick` early-returns for those cases so the
 * page consumer doesn't ALSO navigate the current tab.
 */
export const ModifierKeyClickDoesNotDispatch = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST)),
        neverResolvingInfo,
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    await waitForPlansToLoad(canvasElement);

    let receivedPlanId = null;
    canvasElement.addEventListener("cts-plan-navigate", (e) => {
      receivedPlanId = e.detail.planId;
    });

    const planLink = /** @type {HTMLAnchorElement} */ (
      canvasElement.querySelector("a.plan-name-link")
    );
    expect(planLink).toBeTruthy();

    // Suppress the browser's default link-follow during the test so the test
    // iframe does not navigate away. In production the modifier-key path
    // INTENTIONALLY does not preventDefault so the browser opens a new tab.
    const stopNativeNav = (/** @type {Event} */ e) => e.preventDefault();
    planLink.addEventListener("click", stopNativeNav);

    try {
      planLink.dispatchEvent(
        new MouseEvent("click", { ctrlKey: true, bubbles: true, cancelable: true }),
      );
      expect(receivedPlanId).toBeNull();
    } finally {
      planLink.removeEventListener("click", stopNativeNav);
    }
  },
};

export const ViewConfig = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST)),
        neverResolvingInfo,
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    await waitForPlansToLoad(canvasElement);

    // Click plan-001's Config button (target the inner <button> rendered by
    // cts-button — clicking the host bypasses Lit's @click handler). Target
    // plan-001 by id: the default Started-newest sort means it is not the
    // first card in the DOM.
    const configBtnHost = canvasElement.querySelector('.showConfigBtn[data-plan-id="plan-001"]');
    expect(configBtnHost).toBeTruthy();
    await userEvent.click(innerButton(configBtnHost));

    // Modal should open. The config JSON renders inside a read-only
    // <cts-json-editor>; read the editor's `.value` rather than textContent.
    const editor = /** @type {any} */ (
      await waitFor(
        () => {
          const el = document.querySelector("cts-json-editor.config-json");
          if (!el) throw new Error("cts-json-editor.config-json not yet attached");
          return el;
        },
        { timeout: 10000 },
      )
    );
    await editor.whenReady();
    expect(editor.value).toContain("server.issuer");
    expect(editor.value).toContain("https://op.example.com");

    // Exactly one Monaco editor must be mounted (cts-modal relocates slotted
    // children; the reentrancy guard in _bootMonaco prevents a duplicate).
    const monacoInstances = editor.querySelectorAll(".monaco-editor");
    expect(monacoInstances.length).toBe(1);

    // Plan ID shown in the modal toolbar. Scope to the modal's <code> — the
    // card slug also renders "plan-001", so a global getByText would match
    // two nodes.
    await waitFor(() => {
      const modalCode = document.querySelector(".cts-plan-list-config-toolbar code");
      expect(modalCode?.textContent).toBe("plan-001");
    });

    // Spy on navigator.clipboard.writeText (headless Chromium denies real
    // clipboard writes). restoreMocks: true auto-restores after the test.
    const clipboardSpy = spyOn(navigator.clipboard, "writeText").mockResolvedValue();

    const copyBtnHost = canvasElement.querySelector(".copy-config-btn");
    expect(copyBtnHost).toBeTruthy();
    await userEvent.click(innerButton(copyBtnHost));

    const expectedPayload = JSON.stringify(MOCK_PLAN_LIST[0].config, null, 4);
    await waitFor(() => {
      expect(clipboardSpy).toHaveBeenCalledWith(expectedPayload);
    });
  },
};

/**
 * Plans created without saved configuration come over the wire as
 * `config: {}` (DBTestPlanService persists an empty `org.bson.Document`).
 * Those cards must render no Config button — a button that opens an empty
 * modal is the bug MR 1998's E1 finding called out.
 */
export const ConfigButtonHiddenWhenConfigIsEmpty = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () =>
          HttpResponse.json([
            {
              _id: "plan-empty",
              planName: "oidcc-implicit-certification-test-plan",
              description: "Plan created without saved configuration",
              variant: { response_type: "id_token" },
              started: new Date().toISOString(),
              owner: { sub: "12345", iss: "https://accounts.google.com" },
              modules: [{ testModule: "oidcc-server-implicit", instances: [] }],
              config: {},
              publish: null,
              immutable: false,
            },
          ]),
        ),
        neverResolvingInfo,
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    await waitForPlansToLoad(canvasElement);

    const cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
    expect(cards.length).toBe(1);
    const configBtns = canvasElement.querySelectorAll("cts-button.showConfigBtn");
    expect(configBtns.length).toBe(0);
  },
};

/**
 * Module status boxes: a module that has run shows a pulsing `pending` box
 * (the /api/info fetch is mocked to never resolve here, pinning the initial
 * state); a never-run module (empty `instances`) shows a static `skip` box.
 * Each box is wrapped in a tooltip that reveals the full module id.
 */
export const ModuleStatusBoxes = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () =>
          HttpResponse.json([
            {
              _id: "plan-boxes",
              planName: "oidcc-basic-certification-test-plan",
              description: "Box states",
              variant: {},
              started: new Date().toISOString(),
              owner: { sub: "12345", iss: "https://accounts.google.com" },
              modules: [
                { testModule: "module-has-run", instances: ["inst-aaa"] },
                { testModule: "module-never-run", instances: [] },
              ],
              config: {},
              publish: null,
              immutable: false,
            },
          ]),
        ),
        neverResolvingInfo,
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    await waitForPlansToLoad(canvasElement);

    // One box per module, each wrapped in a tooltip carrying the module id.
    const boxes = canvasElement.querySelectorAll(".moduleStatusGrid .moduleStatusBox");
    expect(boxes.length).toBe(2);
    expect(
      canvasElement.querySelector('cts-tooltip[content="module-has-run"] .moduleStatusBox'),
    ).toBeTruthy();

    // Has-run module → pending (pulsing) box; never-run → static skip box.
    expect(boxVariant(canvasElement, "module-has-run")).toBe("pending");
    expect(boxVariant(canvasElement, "module-never-run")).toBe("skip");
  },
};

/**
 * Module status boxes resolve to their concrete color once `/api/info`
 * returns, driven by the instance-keyed handler. Each module's last instance
 * maps to a distinct result in MOCK_PLAN_INFO (pass / warn / fail), so the
 * full mapping is exercised — not just the happy path. A never-run module
 * (empty instances) stays a static skip box and is never fetched.
 */
export const BoxesResolveToStatus = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST)), infoHandler()],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    await waitForPlansToLoad(canvasElement);

    // inst-001 PASSED → pass box (wait for the async resolution).
    await waitFor(() => {
      expect(boxVariant(canvasElement, "oidcc-server")).toBe("pass");
    });
    // inst-002 WARNING → warn; inst-004 FAILED → fail — the full mapping.
    expect(boxVariant(canvasElement, "oidcc-server-rotate-keys")).toBe("warn");
    expect(boxVariant(canvasElement, "fapi2-security-profile-ensure-signed-request")).toBe("fail");
    // Never-run module (empty instances) stays a static skip box.
    expect(boxVariant(canvasElement, "oidcc-codereuse")).toBe("skip");
  },
};

/**
 * A no-instance module renders a static skip box and triggers no `/api/info`
 * fetch — only modules that have actually run are resolved. The recording
 * handler proves the fetched ids are exactly the modules that have instances.
 */
export const NoInstanceModuleNotFetched = {
  /** @type {string[]} */
  _requested: [],
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST)),
        http.get("/api/info/:testId", ({ params }) => {
          const id = /** @type {string} */ (params.testId);
          NoInstanceModuleNotFetched._requested.push(id);
          const info = MOCK_PLAN_INFO[id];
          return info ? HttpResponse.json(info) : new HttpResponse(null, { status: 404 });
        }),
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    NoInstanceModuleNotFetched._requested.length = 0;
    await waitForPlansToLoad(canvasElement);

    // The never-run module (empty instances) renders a static skip box.
    await waitFor(() => {
      expect(boxVariant(canvasElement, "oidcc-codereuse")).toBe("skip");
    });

    // Wait for the has-instance modules to resolve, then assert the fetched
    // ids are exactly the five real instances — the no-instance module added
    // none.
    await waitFor(() => {
      expect(boxVariant(canvasElement, "oidcc-server")).toBe("pass");
    });
    const unique = [...new Set(NoInstanceModuleNotFetched._requested)].sort();
    expect(unique).toEqual(["inst-001", "inst-002", "inst-003", "inst-004", "inst-005"]);
  },
};

/**
 * A failed `/api/info` (404 / unpublished / deleted run) settles the box at
 * the neutral skip color rather than leaving it pulsing — and does not throw
 * or blank the card.
 */
export const InfoErrorSettlesToSkip = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST)),
        // Empty map → every /api/info is a 404.
        infoHandler({}),
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    await waitForPlansToLoad(canvasElement);

    // A module that has run but whose /api/info 404s settles at skip.
    await waitFor(() => {
      expect(boxVariant(canvasElement, "oidcc-server")).toBe("skip");
    });
    // The card is still intact (not blanked by the error).
    expect(canvasElement.querySelectorAll('[data-testid="plan-list-item"]').length).toBe(
      MOCK_PLAN_LIST.length,
    );
  },
};

/**
 * The `/api/info` fan-out is gated to visible cards: with more than PAGE_SIZE
 * (25) plans loaded, only the first page's module instances are fetched on
 * load. "Show more" reveals page two and lazily fetches its instances.
 */
export const OffScreenModulesNotFetched = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () =>
          HttpResponse.json(
            Array.from({ length: 30 }, (_, i) => {
              const id = `plan-${String(i).padStart(3, "0")}`;
              return {
                _id: id,
                planName: `${id}-name`,
                description: "",
                variant: {},
                // Descending started so DOM order matches index order.
                started: new Date(Date.now() - i * 1000).toISOString(),
                owner: { sub: "12345", iss: "https://accounts.google.com" },
                modules: [{ testModule: "m", instances: [`inst-${String(i).padStart(3, "0")}`] }],
                config: {},
                publish: null,
                immutable: false,
              };
            }),
          ),
        ),
        // Any instance resolves to PASSED; record which ids were fetched.
        http.get("/api/info/:testId", ({ params }) => {
          OffScreenModulesNotFetched._requested.push(/** @type {string} */ (params.testId));
          return HttpResponse.json({ status: "FINISHED", result: "PASSED" });
        }),
      ],
    },
  },
  /** @type {string[]} */
  _requested: [],
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    OffScreenModulesNotFetched._requested.length = 0;
    await waitForPlansToLoad(canvasElement);

    // First page (25) instances fetched; page-two instances (inst-025..029)
    // are NOT fetched until revealed.
    await waitFor(() => {
      expect(OffScreenModulesNotFetched._requested.length).toBe(25);
    });
    expect(OffScreenModulesNotFetched._requested).not.toContain("inst-029");

    // Reveal page two — its instances now get fetched.
    const showMore = canvasElement.querySelector('[data-testid="plan-list-show-more"]');
    await userEvent.click(innerButton(showMore));
    await waitFor(() => {
      expect(OffScreenModulesNotFetched._requested).toContain("inst-029");
    });
  },
};

export const EmptyList = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan", () => HttpResponse.json([]))],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    expect(canvasElement.querySelector('[data-testid="plan-list-empty"]')).toBeTruthy();
    expect(canvas.getByText("No test plans found")).toBeInTheDocument();
    expect(canvasElement.querySelector('[data-testid="plan-list-item"]')).toBeNull();
  },
};

/**
 * Searching to zero results shows distinct copy from the nothing-loaded
 * empty state, so the user understands the list is filtered rather than
 * empty (mirrors cts-log-list's two-case empty state).
 */
export const EmptySearch = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST)),
        neverResolvingInfo,
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    const searchInput = canvasElement.querySelector('input[placeholder="Search test plans..."]');
    await userEvent.type(searchInput, "zzz-no-such-plan-zzz");

    await waitFor(() => {
      expect(canvas.getByText("No plans match your search")).toBeInTheDocument();
    });
    expect(canvasElement.querySelector('[data-testid="plan-list-item"]')).toBeNull();
  },
};

export const LoadingState = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", async () => {
          await new Promise((resolve) => setTimeout(resolve, 60000));
          return HttpResponse.json(MOCK_PLAN_LIST);
        }),
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);

    await waitFor(() => {
      const spinner = canvasElement.querySelector(".spinner-border");
      expect(spinner).toBeTruthy();
    });
    expect(canvas.getByText("Loading test plans...")).toBeInTheDocument();
    expect(canvasElement.querySelector('[data-testid="plan-list-item"]')).toBeNull();
  },
};

export const ApiError = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan", () => new HttpResponse(null, { status: 500 }))],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    const alert = canvasElement.querySelector(".oidf-alert-danger");
    expect(alert).toBeTruthy();
    expect(canvas.getByText(/Failed to load test plans/)).toBeInTheDocument();
    expect(canvasElement.querySelector('[data-testid="plan-list-item"]')).toBeNull();
  },
};

export const AdminView = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () => HttpResponse.json(MOCK_PLAN_LIST)),
        neverResolvingInfo,
      ],
    },
  },
  render: () => html`<cts-plan-list is-admin></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    // One owner pill per card for admins.
    const ownerPills = canvasElement.querySelectorAll(".plan-owner");
    expect(ownerPills.length).toBe(MOCK_PLAN_LIST.length);

    // The owner subject is exposed via the pill's accessible label.
    const firstSub = ownerPills[0].querySelector(".ownerSub");
    expect(firstSub?.getAttribute("aria-label")).toBe("Subject: 12345");

    // Cards still render normally.
    expect(canvas.getByText("oidcc-basic-certification-test-plan")).toBeInTheDocument();
  },
};

export const PublicView = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", ({ request }) => {
          const url = new URL(request.url);
          const isPublic = url.searchParams.get("public") === "true";
          const plans = isPublic ? MOCK_PLAN_LIST.filter((p) => p.publish) : MOCK_PLAN_LIST;
          return HttpResponse.json(plans);
        }),
        neverResolvingInfo,
      ],
    },
  },
  render: () => html`<cts-plan-list is-public></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    // Only published plans surface in the public view.
    const publishedCount = MOCK_PLAN_LIST.filter((p) => p.publish).length;
    const cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
    expect(cards.length).toBe(publishedCount);

    expect(canvas.getByText("fapi2-security-profile-final-test-plan")).toBeInTheDocument();
    expect(canvas.queryByText("oidcc-basic-certification-test-plan")).toBeNull();

    // Owner pill and config button stay hidden in the public view.
    expect(canvasElement.querySelector(".plan-owner")).toBeNull();
    expect(canvasElement.querySelector(".showConfigBtn")).toBeNull();
  },
};

/**
 * Past PAGE_SIZE (25) plans, the listing paginates client-side with a
 * "Show more" button that reveals the next page.
 */
export const ShowMorePagination = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () =>
          HttpResponse.json(
            Array.from({ length: 30 }, (_, i) => ({
              _id: `plan-${String(i).padStart(3, "0")}`,
              planName: `plan-${String(i).padStart(3, "0")}-name`,
              description: "",
              variant: {},
              started: new Date(Date.now() - i * 1000).toISOString(),
              owner: { sub: "12345", iss: "https://accounts.google.com" },
              modules: [{ testModule: "m", instances: [] }],
              config: {},
              publish: null,
              immutable: false,
            })),
          ),
        ),
        neverResolvingInfo,
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement }) {
    await waitForPlansToLoad(canvasElement);

    // First page caps at PAGE_SIZE (25).
    let cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
    expect(cards.length).toBe(25);

    const showMore = canvasElement.querySelector('[data-testid="plan-list-show-more"]');
    expect(showMore).toBeTruthy();
    await userEvent.click(innerButton(showMore));

    await waitFor(() => {
      cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
      expect(cards.length).toBe(30);
    });
  },
};

export {};
