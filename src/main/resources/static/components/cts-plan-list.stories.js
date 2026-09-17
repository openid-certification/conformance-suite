import { html } from "lit";
import { expect, within, waitFor, userEvent, spyOn } from "storybook/test";
import { delay, http, HttpResponse } from "msw";
import { MOCK_PLAN_LIST } from "@fixtures/mock-plans.js";
import { PLAN_SEARCH_FIELDS, serveListing } from "@fixtures/listing-server.js";
import "./cts-plan-list.js";
import { emptyFilter } from "./plan-list-filter.js";

export default {
  title: "Pages/cts-plan-list",
  component: "cts-plan-list",
  parameters: {
    // Page-level story: opt in to Chromatic snapshots. Component stories are
    // excluded by default in frontend/.storybook/preview.js.
    chromatic: { disableSnapshot: false },
  },
};

// --- Helpers ---

async function waitForPlansToLoad(canvasElement) {
  await waitFor(
    () => {
      const loading = canvasElement.querySelector("cts-loading-state");
      expect(loading).toBeNull();
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

/**
 * An `/api/plan` handler that answers each request as the server would: one
 * page of `rows`, searched, ordered and offset as the request asks (see
 * `@fixtures/listing-server.js`), so a story exercises the same round trips
 * the page makes.
 * @param {ReadonlyArray<Record<string, unknown>>} rows - The whole dataset.
 */
function planListing(rows) {
  return http.get("/api/plan", ({ request }) =>
    HttpResponse.json(serveListing(rows, request.url, { searchFields: PLAN_SEARCH_FIELDS })),
  );
}

/**
 * Read the status-segment color variant for a module from its
 * `<cts-plan-status mode="overview">` bar. The per-module segment is found by
 * its accessible name (`"<moduleId>: <status word>"`); the trailing colon
 * disambiguates prefixes (e.g. "oidcc-server" vs "oidcc-server-rotate-keys").
 * Returns the variant suffix (e.g. "pass") or null when no segment is present.
 * @param {ParentNode} root
 * @param {string} moduleId
 * @returns {string|null}
 */
function boxVariant(root, moduleId) {
  const segments = root.querySelectorAll('cts-plan-status [data-testid="plan-status-segment"]');
  const seg = Array.from(segments).find((s) =>
    (s.getAttribute("aria-label") || "").startsWith(`${moduleId}:`),
  );
  if (!seg) return null;
  const cls = [...seg.classList].find((c) => c.startsWith("cts-pst-seg--"));
  return cls ? cls.replace("cts-pst-seg--", "") : null;
}

/**
 * Every `/api/plan` query the FilteredByChips story caused, in order — the
 * only place a play function can prove that removing a chip really went back
 * to the SERVER rather than re-filtering what was already on screen. Reset by
 * that story's `beforeEach`.
 * @type {Array<string>}
 */
const PLAN_REQUESTS = [];

/**
 * The filter a drill-down from a weekly bar of the statistics page produces.
 * @returns {import("./plan-list-filter.js").PlanListFilter} That filter.
 */
function drillDownFilter() {
  return {
    ...emptyFilter(),
    family: "OID4VP",
    plan: "",
    variant: { client_auth_type: "private_key_jwt" },
    cert: "",
    from: "2026-05-04",
    to: "2026-05-11",
  };
}

/**
 * The chip badge with a given test id, and the `role="button"` span inside it
 * that is the actual click target (cts-badge renders to its own light DOM).
 * @param {ParentNode} root - The story canvas.
 * @param {string} key - Chip key, e.g. `family`.
 * @returns {HTMLElement} The clickable span.
 */
function chipButton(root, key) {
  const badge = /** @type {HTMLElement} */ (
    root.querySelector(`[data-testid="plan-filter-${key}"]`)
  );
  expect(badge).toBeTruthy();
  const span = /** @type {HTMLElement} */ (badge.querySelector('span[role="button"]'));
  expect(span).toBeTruthy();
  return span;
}

// --- Stories ---

export const Default = {
  parameters: {
    msw: {
      handlers: [planListing(MOCK_PLAN_LIST)],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    // Target plan-001 by id rather than DOM position — the default sort is
    // Started (newest), so the first card is not necessarily the first fixture
    // entry. Shared across phases below.
    const planCard = canvasElement.querySelector(
      '[data-testid="plan-list-item"][data-plan-id="plan-001"]',
    );

    await step("search + sort toolbar renders (mirrors cts-log-list)", async () => {
      expect(canvasElement.querySelector(".cts-plan-list-search input")).toBeTruthy();
      expect(canvasElement.querySelector(".cts-plan-list-sort select")).toBeTruthy();
    });

    await step("one card per plan", async () => {
      const cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
      expect(cards.length).toBe(MOCK_PLAN_LIST.length);
    });

    await step("plan names render", async () => {
      expect(canvas.getByText("oidcc-basic-certification-test-plan")).toBeInTheDocument();
      expect(canvas.getByText("fapi2-security-profile-final-test-plan")).toBeInTheDocument();
    });

    await step("plan-name anchor carries the real destination URL", async () => {
      // Real href (not "#") so cmd-click, middle-click, right-click "Open in new
      // tab", browser hover preview, and screen-reader destination announcement
      // all work.
      expect(planCard).toBeTruthy();
      const planLink = /** @type {HTMLAnchorElement | null} */ (
        planCard.querySelector("a.plan-name-link")
      );
      expect(planLink?.getAttribute("href")).toBe("plan-detail.html?plan=plan-001");
    });

    await step("plan id renders as the card slug", async () => {
      expect(planCard.querySelector(".cts-plan-card-slug")?.textContent).toBe("plan-001");
    });

    await step("each module renders one color-coded status segment", async () => {
      const totalModules = MOCK_PLAN_LIST.reduce((n, p) => n + (p.modules?.length || 0), 0);
      const segments = canvasElement.querySelectorAll(
        'cts-plan-status [data-testid="plan-status-segment"]',
      );
      expect(segments.length).toBe(totalModules);
    });

    await step("Started value renders through cts-time", async () => {
      // A native <time> whose title carries the full absolute date on hover.
      const startedTime = planCard.querySelector("cts-time time");
      expect(startedTime).toBeTruthy();
      expect(startedTime?.getAttribute("datetime")).toBeTruthy();
    });

    await step("non-admin users do not see the owner pill", async () => {
      expect(canvasElement.querySelector(".plan-owner")).toBeNull();
    });
  },
};

export const SearchAndSort = {
  parameters: {
    msw: {
      handlers: [planListing(MOCK_PLAN_LIST)],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    const searchInput = canvasElement.querySelector('input[placeholder="Search test plans..."]');

    await step("all plans render initially", async () => {
      const cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
      expect(cards.length).toBe(MOCK_PLAN_LIST.length);
    });

    await step("search asks the server, which narrows the cards", async () => {
      expect(searchInput).toBeTruthy();
      await userEvent.type(searchInput, "fapi2");
      await waitFor(() => {
        const cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
        expect(cards.length).toBe(1);
      });
      expect(canvas.getByText("fapi2-security-profile-final-test-plan")).toBeInTheDocument();
      expect(canvas.queryByText("oidcc-basic-certification-test-plan")).toBeNull();
    });

    await step("clearing then sorting by name (A–Z) asks the server to order", async () => {
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
    });
  },
};

export const ClickPlanName = {
  parameters: {
    msw: {
      handlers: [planListing(MOCK_PLAN_LIST)],
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
      handlers: [planListing(MOCK_PLAN_LIST)],
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
      handlers: [planListing(MOCK_PLAN_LIST)],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement, step }) {
    await waitForPlansToLoad(canvasElement);

    // The config JSON renders inside a read-only <cts-json-view>; the handle
    // is shared across the assertion phases below.
    let editor;

    await step("clicking the Config button opens the modal", async () => {
      // Click plan-001's Config button (target the inner <button> rendered by
      // cts-button — clicking the host bypasses Lit's @click handler). Target
      // plan-001 by id: the default Started-newest sort means it is not the
      // first card in the DOM.
      const configBtnHost = canvasElement.querySelector('.showConfigBtn[data-plan-id="plan-001"]');
      expect(configBtnHost).toBeTruthy();
      await userEvent.click(innerButton(configBtnHost));

      editor = /** @type {any} */ (
        await waitFor(
          () => {
            const el = document.querySelector("cts-json-view.config-json");
            if (!el) throw new Error("cts-json-view.config-json not yet attached");
            return el;
          },
          { timeout: 10000 },
        )
      );
    });

    await step("config JSON renders in the read-only editor", async () => {
      // Read the editor's `.value` rather than textContent.
      await editor.whenReady();
      expect(editor.value).toContain("server.issuer");
      expect(editor.value).toContain("https://op.example.com");
    });

    await step("exactly one JSON view is rendered", async () => {
      // cts-modal relocates slotted children; assert the swap did not leave a
      // duplicate view behind.
      const views = editor.querySelectorAll(".oidf-json-view");
      expect(views.length).toBe(1);
    });

    await step("plan id shows in the modal toolbar", async () => {
      // Scope to the modal's <code> — the card slug also renders "plan-001", so
      // a global getByText would match two nodes.
      await waitFor(() => {
        const modalCode = document.querySelector(".cts-plan-list-config-toolbar code");
        expect(modalCode?.textContent).toBe("plan-001");
      });
    });

    await step("Copy config writes the formatted payload to the clipboard", async () => {
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
 * Module status segments take their colour from the `status`/`result` each
 * listing row carries for the module's latest run — the server looks those up
 * for the whole page, so the listing makes no request per module. Every
 * colour the mapping produces is exercised (pass / warn / fail), a never-run
 * module (empty `instances`) is a static neutral segment, and so is a module
 * whose latest run the server could not find (instances, but no status): it
 * settles at neutral rather than pulsing forever. Each segment is wrapped in a
 * tooltip naming the module + status. The `/api/info` handler records any
 * per-module request, so the story proves there are none.
 */
export const ModuleStatusSegments = {
  /** @type {string[]} */
  _infoRequests: [],
  parameters: {
    msw: {
      handlers: [
        planListing([
          ...MOCK_PLAN_LIST,
          {
            _id: "plan-run-gone",
            planName: "oidcc-basic-certification-test-plan",
            description: "A module whose latest run was deleted",
            variant: {},
            started: new Date().toISOString(),
            owner: { sub: "12345", iss: "https://accounts.google.com" },
            modules: [{ testModule: "module-run-gone", instances: ["inst-deleted"] }],
            config: {},
            publish: null,
            immutable: false,
          },
        ]),
        http.get("/api/info/:testId", ({ params }) => {
          ModuleStatusSegments._infoRequests.push(/** @type {string} */ (params.testId));
          return HttpResponse.json({ status: "FINISHED", result: "PASSED" });
        }),
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement, step }) {
    ModuleStatusSegments._infoRequests.length = 0;
    await waitForPlansToLoad(canvasElement);

    await step("one segment per module, each wrapped in a tooltip naming it", async () => {
      const totalModules = MOCK_PLAN_LIST.reduce((n, p) => n + (p.modules?.length || 0), 0) + 1;
      const segments = canvasElement.querySelectorAll(
        'cts-plan-status [data-testid="plan-status-segment"]',
      );
      expect(segments.length).toBe(totalModules);
      expect(
        canvasElement.querySelector(
          'cts-tooltip[content^="oidcc-server —"] [data-testid="plan-status-segment"]',
        ),
      ).toBeTruthy();
    });

    await step("the colour is the row's status/result — the full mapping", async () => {
      // inst-001 PASSED → pass; inst-002 WARNING → warn; inst-004 FAILED → fail.
      expect(boxVariant(canvasElement, "oidcc-server")).toBe("pass");
      expect(boxVariant(canvasElement, "oidcc-server-rotate-keys")).toBe("warn");
      expect(boxVariant(canvasElement, "fapi2-security-profile-ensure-signed-request")).toBe(
        "fail",
      );
    });

    await step("a never-run module and a module whose run is gone are neutral", async () => {
      expect(boxVariant(canvasElement, "oidcc-codereuse")).toBe("neutral");
      expect(boxVariant(canvasElement, "module-run-gone")).toBe("neutral");
    });

    await step("no per-module request was made", async () => {
      expect(ModuleStatusSegments._infoRequests).toEqual([]);
    });
  },
};

// The default empty state (My view, no search): a distinct heading plus a
// Schedule-test action that guides the user to start their first test (R18),
// and a secondary View-published-plans action so the empty personal list
// still offers something to browse.
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

    const empty = canvasElement.querySelector('[data-testid="plan-list-empty"]');
    expect(empty).toBeTruthy();
    expect(canvas.getByText("No test plans yet")).toBeInTheDocument();
    expect(canvasElement.querySelector('[data-testid="plan-list-item"]')).toBeNull();

    // The empty state offers a Schedule-test action.
    const action = /** @type {HTMLAnchorElement} */ (
      await waitFor(() => {
        const a = empty.querySelector('a[href="schedule-test.html"]');
        expect(a).toBeTruthy();
        return a;
      })
    );
    expect(action.textContent?.trim()).toContain("Create a new test");

    // ... and a secondary action pointing at the Published view, side by
    // side with the primary.
    const secondary = /** @type {HTMLAnchorElement} */ (
      await waitFor(() => {
        const a = empty.querySelector('a[href="plans.html?public=true"]');
        expect(a).toBeTruthy();
        return a;
      })
    );
    expect(secondary.textContent?.trim()).toContain("View published plans");
    expect(secondary.classList.contains("oidf-btn-secondary")).toBe(true);
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
      handlers: [planListing(MOCK_PLAN_LIST)],
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
      const spinner = canvasElement.querySelector("cts-loading-state cts-spinner");
      expect(spinner).toBeTruthy();
    });
    expect(canvas.getByText("Loading test plans…")).toBeInTheDocument();
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

/**
 * A filtered listing whose fetch fails keeps its chips. A filter is what can
 * CAUSE the failure — `/api/plan` answers 400 for a variant parameter name it
 * cannot parse — so an error with the filters hidden would leave the reader
 * unable to see what was asked for, let alone clear it.
 */
export const FilteredApiError = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", () =>
          HttpResponse.json({ error: "variant.$where is not a variant name" }, { status: 400 }),
        ),
      ],
    },
  },
  beforeEach() {
    history.replaceState(null, "", "/iframe.html");
  },
  render: () => html`<cts-plan-list .filters=${drillDownFilter()}></cts-plan-list>`,
  async play({ canvasElement, step }) {
    await waitForPlansToLoad(canvasElement);

    await step("the error is shown, and says which parameter the server refused", async () => {
      const alert = canvasElement.querySelector(".oidf-alert-danger");
      expect(alert).toBeTruthy();
      // `{"error": …}` is what TestPlanApi answers a bad filter with, and it
      // is the only thing that says WHICH chip to remove; the status code on
      // its own leaves the reader guessing.
      expect(alert.textContent).toContain("variant.$where is not a variant name");
      expect(canvasElement.querySelector('[data-testid="plan-list-item"]')).toBeNull();
    });

    await step("and the filters are still there to be read and removed", async () => {
      expect(canvasElement.querySelector('[data-testid="plan-filters"]')).toBeTruthy();
      expect(canvasElement.querySelectorAll('[data-testid="plan-filters"] cts-badge').length).toBe(
        3,
      );
      await userEvent.click(chipButton(canvasElement, "family"));
      await waitFor(() => {
        expect(
          canvasElement.querySelectorAll('[data-testid="plan-filters"] cts-badge').length,
        ).toBe(2);
      });
    });
  },
};

export const AdminView = {
  parameters: {
    msw: {
      handlers: [planListing(MOCK_PLAN_LIST)],
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
      ],
    },
  },
  render: () => html`<cts-plan-list is-public></cts-plan-list>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    await step("only published plans surface in the public view", async () => {
      const publishedCount = MOCK_PLAN_LIST.filter((p) => p.publish).length;
      const cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
      expect(cards.length).toBe(publishedCount);

      expect(canvas.getByText("fapi2-security-profile-final-test-plan")).toBeInTheDocument();
      expect(canvas.queryByText("oidcc-basic-certification-test-plan")).toBeNull();
    });

    await step("owner pill and config button stay hidden", async () => {
      expect(canvasElement.querySelector(".plan-owner")).toBeNull();
      expect(canvasElement.querySelector(".showConfigBtn")).toBeNull();
    });

    await step("the detail link threads public=true", async () => {
      // So anonymous click-through (and open-in-new-tab) resolves —
      // plan-detail.html is public ONLY with the param.
      const publishedCard = canvasElement.querySelector(
        '[data-testid="plan-list-item"][data-plan-id="plan-002"]',
      );
      const publishedLink = /** @type {HTMLAnchorElement | null} */ (
        publishedCard?.querySelector("a.plan-name-link")
      );
      expect(publishedLink?.getAttribute("href")).toBe(
        "plan-detail.html?plan=plan-002&public=true",
      );
    });
  },
};

/**
 * A re-query (sort, search, chip) keeps the rows on screen, dimmed and marked
 * busy, with a status line, until the server answers — the spinner is only
 * for the first load and a My/Published swap. The sort change's response is
 * held so the state is visible.
 */
export const RefreshInPlace = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", async ({ request }) => {
          const params = new URL(request.url).searchParams;
          if (params.get("order") !== "started,desc") await delay(1500);
          return HttpResponse.json(
            serveListing(MOCK_PLAN_LIST, request.url, { searchFields: PLAN_SEARCH_FIELDS }),
          );
        }),
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement, step }) {
    await waitForPlansToLoad(canvasElement);
    const sortSelect = /** @type {HTMLSelectElement} */ (
      canvasElement.querySelector(".cts-plan-list-sort select")
    );
    await userEvent.selectOptions(sortSelect, "name-asc");

    await step("the rows stay, busy, with a status line and no spinner", async () => {
      await waitFor(() => {
        const list = canvasElement.querySelector('[data-testid="plan-list-items"]');
        expect(list?.getAttribute("aria-busy")).toBe("true");
      });
      expect(canvasElement.querySelectorAll('[data-testid="plan-list-item"]').length).toBe(
        MOCK_PLAN_LIST.length,
      );
      expect(
        canvasElement.querySelector('[data-testid="plan-list-refreshing"]')?.textContent,
      ).toContain("Updating");
      expect(canvasElement.querySelector("cts-loading-state")).toBeNull();
    });

    await step(
      "once answered, the rows are the sorted ones and the busy state clears",
      async () => {
        await waitFor(
          () => {
            const list = canvasElement.querySelector('[data-testid="plan-list-items"]');
            expect(list?.getAttribute("aria-busy")).toBe("false");
          },
          { timeout: 3000 },
        );
        const names = Array.from(
          canvasElement.querySelectorAll('[data-testid="plan-list-item"] .cts-plan-card-name'),
        ).map((el) => el.textContent);
        expect(names).toEqual([...names].sort((a, b) => a.localeCompare(b)));
      },
    );
  },
};

/**
 * The SERVER pages the listing, 25 rows at a time: the first request asks for
 * `start=0&length=25`, and "Show more" asks for the page after the rows
 * already shown and appends it. The synthetic `recordsTotal` the server
 * answers with (start+length+1 while there is more) is what tells the listing
 * to offer the button at all.
 */
export const ShowMorePagination = {
  /** @type {string[]} */
  _requests: [],
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", ({ request }) => {
          ShowMorePagination._requests.push(new URL(request.url).search);
          return HttpResponse.json(
            serveListing(
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
              request.url,
              { searchFields: PLAN_SEARCH_FIELDS },
            ),
          );
        }),
      ],
    },
  },
  render: () => html`<cts-plan-list></cts-plan-list>`,
  async play({ canvasElement, step }) {
    ShowMorePagination._requests.length = 0;
    await waitForPlansToLoad(canvasElement);

    await step("the first page is 25 rows, asked for newest-first", async () => {
      const cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
      expect(cards.length).toBe(25);
      const first = new URLSearchParams(ShowMorePagination._requests[0]);
      expect(first.get("start")).toBe("0");
      expect(first.get("length")).toBe("25");
      expect(first.get("order")).toBe("started,desc");
    });

    await step("Show more asks for the next page and appends it", async () => {
      const showMore = canvasElement.querySelector('[data-testid="plan-list-show-more"]');
      expect(showMore).toBeTruthy();
      expect(innerButton(showMore).textContent).toContain("Show more (25 loaded)");
      await userEvent.click(innerButton(showMore));

      await waitFor(() => {
        const cards = canvasElement.querySelectorAll('[data-testid="plan-list-item"]');
        expect(cards.length).toBe(30);
      });
      const next = new URLSearchParams(ShowMorePagination._requests.at(-1) || "");
      expect(next.get("start")).toBe("25");
      // Nothing left, so nothing more is offered.
      expect(canvasElement.querySelector('[data-testid="plan-list-show-more"]')).toBeNull();
    });
  },
};

/**
 * U8 — the Published view, when empty, shows orienting placeholder copy AND
 * offers a Schedule-test action, so the persistent entry point to start a test
 * is present here too (R11/R18). The body copy uses the same "Published test
 * plans" vocabulary as the page-level Published descriptor (R21/R22, U12).
 * (The My-view empty state is covered by EmptyList, which renders the same
 * default empty.)
 */
export const EmptyPublishedView = {
  parameters: {
    msw: {
      handlers: [http.get("/api/plan", () => HttpResponse.json([]))],
    },
  },
  render: () => html`<cts-plan-list is-public></cts-plan-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    const empty = canvasElement.querySelector('[data-testid="plan-list-empty"]');
    expect(empty).toBeTruthy();
    expect(canvas.getByText("No published plans yet")).toBeInTheDocument();
    // Finalized R18 body copy, sharing the "Published test plans" vocabulary
    // with the page-level Published descriptor (R21/R22, U12).
    expect(
      canvas.getByText("Published test plans will appear here once they are shared."),
    ).toBeInTheDocument();

    // The Published-empty state also offers the Schedule-test action.
    const createLink = /** @type {HTMLAnchorElement} */ (
      await waitFor(() => {
        const a = empty.querySelector('a[href="schedule-test.html"]');
        expect(a).toBeTruthy();
        return a;
      })
    );
    expect(createLink.textContent?.trim()).toContain("Create a new test");

    // No secondary View-published-plans action here — it would link to the
    // view the user is already on. The Schedule-test anchor (awaited above)
    // proves the CTA row has rendered, so asserting "exactly one anchor"
    // can't pass vacuously and also catches a secondary rendering with a
    // mangled href.
    expect(empty.querySelectorAll("a").length).toBe(1);
  },
};

/**
 * The drill-down landing state: `plans.html` has read the filters out of its
 * query string and handed them over as a property, so the FIRST request
 * already carries them — the listing never paints an unfiltered set it then
 * has to correct.
 *
 * Each filter is a removable chip. The chips are `clickable` badges (the
 * badge IS the click target and nothing wraps it), so each one carries
 * `role="button"`, keyboard activation and the stronger affordance ring.
 * Removing one refetches — the SERVER applies these filters, as it does the
 * search box above the list.
 */
export const FilteredByChips = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", ({ request }) => {
          const url = new URL(request.url);
          PLAN_REQUESTS.push(url.search);
          // Stand in for the server: this family has nothing in the window,
          // which is exactly the state a drill-down can land in.
          if (url.searchParams.get("family")) return HttpResponse.json([]);
          return HttpResponse.json(MOCK_PLAN_LIST.filter((plan) => plan.publish));
        }),
      ],
    },
  },
  beforeEach() {
    // The component mirrors chip removals into the page URL, so a story that
    // did not reset it would hydrate the next one.
    history.replaceState(null, "", "/iframe.html?public=true");
    PLAN_REQUESTS.length = 0;
  },
  render: () => html`<cts-plan-list is-public .filters=${drillDownFilter()}></cts-plan-list>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await waitForPlansToLoad(canvasElement);

    await step("the first request already carries every filter", async () => {
      expect(PLAN_REQUESTS.length).toBe(1);
      const params = new URLSearchParams(PLAN_REQUESTS[0]);
      expect(params.get("public")).toBe("true");
      expect(params.get("family")).toBe("OID4VP");
      expect(params.get("variant.client_auth_type")).toBe("private_key_jwt");
      expect(params.get("from")).toBe("2026-05-04");
      // Exclusive, so it is the NEXT period's start — the day after the last
      // day the chip names.
      expect(params.get("to")).toBe("2026-05-11");
      expect(params.get("start")).toBe("0");
      expect(params.get("length")).toBe("25");
      expect(params.get("order")).toBe("started,desc");
    });

    await step("one chip per filter, each announced as a remove action", async () => {
      const row = canvasElement.querySelector('[data-testid="plan-filters"]');
      expect(row).toBeTruthy();
      const chips = Array.from(row.querySelectorAll("cts-badge"));
      expect(chips.map((chip) => chip.getAttribute("label"))).toEqual([
        "Family: OID4VP",
        "client_auth_type: private_key_jwt",
        "Started 4 May 2026 – 10 May 2026",
      ]);
      // The badge is the click target and nothing wraps it, so it is
      // `clickable`: a real button role, reachable by keyboard, with a name
      // that says what activating it does.
      const family = chipButton(canvasElement, "family");
      expect(family.getAttribute("tabindex")).toBe("0");
      // WCAG 2.5.3: the accessible name contains the visible label verbatim.
      expect(family.getAttribute("aria-label")).toBe("Remove filter: Family: OID4VP");
      expect(family.classList.contains("is-clickable")).toBe(true);
      // Not a toggle: no aria-pressed on a one-shot command.
      expect(family.hasAttribute("aria-pressed")).toBe(false);
    });

    await step("a filter that matches nothing says so, and offers the way out", async () => {
      expect(canvas.getByText("No plans match these filters")).toBeInTheDocument();
      const empty = canvasElement.querySelector('[data-testid="plan-list-empty"]');
      const clear = /** @type {HTMLAnchorElement} */ (
        empty.querySelector('a[href="plans.html?public=true"]')
      );
      expect(clear.textContent?.trim()).toContain("Clear filters");
    });

    await step("removing a chip refetches without it and rewrites the URL", async () => {
      await userEvent.click(chipButton(canvasElement, "family"));
      await waitFor(() => {
        expect(PLAN_REQUESTS.length).toBe(2);
      });
      const params = new URLSearchParams(PLAN_REQUESTS[1]);
      expect(params.get("family")).toBeNull();
      expect(params.get("variant.client_auth_type")).toBe("private_key_jwt");
      expect(params.get("from")).toBe("2026-05-04");
      // The dataset the user is on survives the rewrite: dropping `public`
      // would switch the Published tab back to My behind their back.
      expect(location.search).toBe(
        "?public=true&variant.client_auth_type=private_key_jwt&from=2026-05-04&to=2026-05-11",
      );
      await waitFor(() => {
        expect(canvasElement.querySelectorAll('[data-testid="plan-list-item"]').length).toBe(
          MOCK_PLAN_LIST.filter((plan) => plan.publish).length,
        );
      });
      expect(canvasElement.querySelectorAll('[data-testid="plan-filters"] cts-badge').length).toBe(
        2,
      );
    });

    await step("Clear all drops the rest in one go", async () => {
      await userEvent.click(
        /** @type {HTMLElement} */ (
          canvasElement.querySelector('[data-testid="plan-filters-clear"]')
        ),
      );
      await waitFor(() => {
        expect(PLAN_REQUESTS.length).toBe(3);
      });
      const params = new URLSearchParams(PLAN_REQUESTS[2]);
      expect(params.get("from")).toBeNull();
      expect(params.get("variant.client_auth_type")).toBeNull();
      expect(params.get("public")).toBe("true");
      expect(location.search).toBe("?public=true");
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="plan-filters"]')).toBeNull();
      });
    });
  },
};

/**
 * Two filter changes in a row, answered out of order. The first request is
 * slow and the second overtakes it, so the listing must show the SECOND
 * answer — and go on showing it when the first finally lands. Without a
 * request sequence guard the stale rows win simply by arriving last.
 */
export const StaleResponsesAreIgnored = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/plan", async ({ request }) => {
          const url = new URL(request.url);
          PLAN_REQUESTS.push(url.search);
          if (url.searchParams.get("family")) {
            // The filtered listing is the slow one, so removing its chip
            // starts a second request that answers first.
            await delay(400);
            return HttpResponse.json(MOCK_PLAN_LIST.slice(0, 1));
          }
          return HttpResponse.json(MOCK_PLAN_LIST);
        }),
      ],
    },
  },
  beforeEach() {
    history.replaceState(null, "", "/iframe.html");
    PLAN_REQUESTS.length = 0;
  },
  render: () => html`<cts-plan-list .filters=${drillDownFilter()}></cts-plan-list>`,
  async play({ canvasElement, step }) {
    await step("the chip is removed while the filtered request is still out", async () => {
      // The filter row renders during loading precisely so this is possible.
      await waitFor(() => {
        expect(canvasElement.querySelector('[data-testid="plan-filter-family"]')).toBeTruthy();
        // msw intercepts asynchronously, so the chip is on screen a tick
        // before the request it was rendered alongside is recorded.
        expect(PLAN_REQUESTS.length).toBe(1);
      });
      await userEvent.click(chipButton(canvasElement, "family"));
      await waitFor(() => {
        expect(PLAN_REQUESTS.length).toBe(2);
      });
      expect(new URLSearchParams(PLAN_REQUESTS[1]).get("family")).toBeNull();
    });

    await step("the second answer is what the reader gets", async () => {
      await waitForPlansToLoad(canvasElement);
      expect(canvasElement.querySelectorAll('[data-testid="plan-list-item"]').length).toBe(
        MOCK_PLAN_LIST.length,
      );
    });

    await step("and the overtaken one does not replace it when it lands", async () => {
      await delay(600);
      expect(canvasElement.querySelectorAll('[data-testid="plan-list-item"]').length).toBe(
        MOCK_PLAN_LIST.length,
      );
      // Nor does it put the list back into the loading state it left.
      expect(canvasElement.querySelector("cts-loading-state")).toBeNull();
      expect(canvasElement.querySelectorAll('[data-testid="plan-filters"] cts-badge').length).toBe(
        2,
      );
    });
  },
};

export {};
