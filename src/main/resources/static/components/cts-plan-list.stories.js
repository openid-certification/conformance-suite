import { html } from "lit";
import { expect, within, waitFor, userEvent, spyOn } from "storybook/test";
import { http, HttpResponse } from "msw";
import { MOCK_PLAN_LIST } from "@fixtures/mock-plans.js";
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

// An /api/info handler that never resolves, so module status dots stay in
// their initial `pending` state for deterministic assertions. Used by the
// pending-dot story; the resolution stories (DotsResolveToStatus etc.) live
// in the U3 follow-up with instance-keyed handlers.
const neverResolvingInfo = http.get(
  "/api/info/:testId",
  () => new Promise(() => {}), // intentionally never settles
);

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

    // Each module chip carries a status dot — one per module across all cards.
    const totalModules = MOCK_PLAN_LIST.reduce((n, p) => n + (p.modules?.length || 0), 0);
    const dots = canvasElement.querySelectorAll(".moduleBadgeStack .cts-badge-dot");
    expect(dots.length).toBe(totalModules);

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
 * Module status dots: a module that has run shows a pulsing `pending` dot
 * (the /api/info fetch is mocked to never resolve here, pinning the initial
 * state); a never-run module (empty `instances`) shows a static `skip` dot.
 * The neutral `secondary` chip fill is unchanged — only the dot carries
 * status color.
 */
export const ModuleStatusDots = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () =>
          HttpResponse.json([
            {
              _id: "plan-dots",
              planName: "oidcc-basic-certification-test-plan",
              description: "Dot states",
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

    const chips = canvasElement.querySelectorAll(".moduleBadgeStack cts-badge");
    expect(chips.length).toBe(2);

    // Every chip is a neutral name chip with a dot.
    chips.forEach((chip) => {
      expect(chip.querySelector(".badge")?.classList.contains("b-secondary")).toBe(true);
      expect(chip.querySelector(".cts-badge-dot")).toBeTruthy();
    });

    // Has-run module → pending (pulsing) dot; never-run → static skip dot.
    expect(canvasElement.querySelector(".cts-badge-dot-pending")).toBeTruthy();
    expect(canvasElement.querySelector(".cts-badge-dot-skip")).toBeTruthy();
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
