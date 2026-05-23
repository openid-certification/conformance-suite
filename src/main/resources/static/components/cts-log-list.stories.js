import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { http, HttpResponse, delay } from "msw";
import { MOCK_LOG_LIST, MOCK_LOG_LIST_LARGE } from "@fixtures/mock-log-list.js";
import "./cts-log-list.js";

export default {
  title: "Pages/cts-log-list",
  component: "cts-log-list",
  parameters: {
    layout: "padded",
  },
  // Reset URL state before each story so filter persistence from one
  // story (history.replaceState writes ?status=…, ?result=…) does not
  // hydrate into the next story's component on connectedCallback.
  beforeEach() {
    window.history.replaceState({}, "", window.location.pathname);
  },
};

// --- Helpers ---

/**
 * Wait for the initial fetch spinner to clear. The component renders a
 * cts-spinner inside `.cts-log-list-loading` while `_loading === true`.
 */
async function waitForLogsToLoad(canvasElement) {
  await waitFor(
    () => {
      const loading = canvasElement.querySelector(".cts-log-list-loading");
      expect(loading).toBeNull();
    },
    { timeout: 3000 },
  );
}

function paginationEnvelope(rows) {
  return {
    draw: 1,
    recordsTotal: rows.length,
    recordsFiltered: rows.length,
    data: rows,
  };
}

// `cts-log-list` resolves a kebab-case `planName` per unique `planId` via
// `/api/plan/<id>` so the meta-row "Plan" chip shows the spec identifier
// instead of the opaque MongoDB id. Every story that mounts the component
// needs this handler — otherwise the resolver's fetches hit MSW's
// onUnhandledRequest and pollute the story console with warnings.
function planResolveHandler(planNamesById = {}) {
  return http.get("/api/plan/:planId", ({ params }) => {
    const planId = /** @type {string} */ (params.planId);
    return HttpResponse.json({
      _id: planId,
      planName: planNamesById[planId] || `mock-plan-name-${planId}`,
    });
  });
}

// --- Stories ---

export const Default = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/log", () => HttpResponse.json(paginationEnvelope(MOCK_LOG_LIST))),
        planResolveHandler(),
      ],
    },
  },
  render: () => html`<cts-log-list></cts-log-list>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    await waitForLogsToLoad(canvasElement);

    const items = canvasElement.querySelectorAll('[data-testid="log-list-item"]');
    expect(items.length).toBe(MOCK_LOG_LIST.length);

    // Headline test names render.
    expect(canvas.getByText("oidcc-server")).toBeInTheDocument();
    expect(canvas.getByText("vci-failed")).toBeInTheDocument();

    // Status and result badges render — sample one of each.
    const passedBadges = canvasElement.querySelectorAll('cts-badge[label="PASSED"]');
    expect(passedBadges.length).toBeGreaterThan(0);
    const failedBadges = canvasElement.querySelectorAll('cts-badge[label="FAILED"]');
    expect(failedBadges.length).toBe(1);

    // Sort selector defaults to started-desc.
    const sortSelect = canvasElement.querySelector(".cts-log-list-sort select");
    expect(sortSelect.value).toBe("started-desc");

    // Owner pill is NOT rendered when is-admin is unset.
    expect(canvasElement.querySelectorAll(".log-owner").length).toBe(0);

    // The Started timestamp renders through cts-time: a native <time> whose
    // title carries the absolute form on hover (replacing the former
    // cts-tooltip + formatRelativeTime/formatAbsoluteTime path).
    const startedTime = canvasElement.querySelector(".cts-log-card-meta-value time");
    expect(startedTime).toBeTruthy();
    expect(startedTime?.getAttribute("title")).toBeTruthy();
    expect(startedTime?.getAttribute("datetime")).toBeTruthy();
  },
};

export const WithResolvedPlanNames = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/log", () => HttpResponse.json(paginationEnvelope(MOCK_LOG_LIST))),
        planResolveHandler({
          "plan-001": "oidcc-basic-certification-test-plan",
          "plan-002": "fapi2-security-profile-final-test-plan",
          "plan-003": "vci-id-1-wallet-test-plan",
        }),
      ],
    },
  },
  render: () => html`<cts-log-list></cts-log-list>`,
  async play({ canvasElement }) {
    await waitForLogsToLoad(canvasElement);

    // Wait for the async /api/plan/<id> resolutions to land — the chip
    // text flips from optimistic planId to resolved planName.
    await waitFor(() => {
      const link = canvasElement.querySelector(
        '[data-test-id="test-log-001"] .cts-log-card-plan-link',
      );
      expect(link).not.toBeNull();
      expect(link.textContent.trim()).toBe("oidcc-basic-certification-test-plan");
    });

    // Rows that share a planId pick the same resolved name.
    const card002 = canvasElement.querySelector(
      '[data-test-id="test-log-002"] .cts-log-card-plan-link',
    );
    expect(card002.textContent.trim()).toBe("oidcc-basic-certification-test-plan");

    // Distinct planId resolves independently.
    const card003 = canvasElement.querySelector(
      '[data-test-id="test-log-003"] .cts-log-card-plan-link',
    );
    expect(card003.textContent.trim()).toBe("fapi2-security-profile-final-test-plan");

    // Link target is still keyed by planId — only the visible text changes.
    const card001Link = canvasElement.querySelector(
      '[data-test-id="test-log-001"] .cts-log-card-plan-link',
    );
    expect(card001Link.getAttribute("href")).toContain("plan=plan-001");
  },
};

export const AdminListing = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/log", () => HttpResponse.json(paginationEnvelope(MOCK_LOG_LIST))),
        planResolveHandler(),
      ],
    },
  },
  render: () => html`<cts-log-list is-admin></cts-log-list>`,
  async play({ canvasElement }) {
    await waitForLogsToLoad(canvasElement);

    // Each card with an owner renders the two-tone pill with both glyphs.
    const ownerPills = canvasElement.querySelectorAll(".cts-log-card .log-owner");
    expect(ownerPills.length).toBe(MOCK_LOG_LIST.length);

    const firstPill = ownerPills[0];
    expect(firstPill.querySelectorAll(".ownerSub").length).toBe(1);
    expect(firstPill.querySelectorAll(".ownerIss").length).toBe(1);
    expect(firstPill.querySelectorAll('cts-icon[name="user-01"]').length).toBe(1);
    expect(firstPill.querySelectorAll('cts-icon[name="globe"]').length).toBe(1);
  },
};

export const PublicListing = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/log", ({ request }) => {
          const url = new URL(request.url);
          // Verify the public flag is forwarded.
          if (url.searchParams.get("public") !== "true") {
            return HttpResponse.json({ error: "expected ?public=true" }, { status: 400 });
          }
          return HttpResponse.json(paginationEnvelope(MOCK_LOG_LIST));
        }),
        http.get("/api/plan/:planId", ({ request, params }) => {
          const url = new URL(request.url);
          // Verify the public flag is forwarded on plan-name resolution too.
          if (url.searchParams.get("public") !== "true") {
            return HttpResponse.json({ error: "expected ?public=true" }, { status: 400 });
          }
          const planId = /** @type {string} */ (params.planId);
          return HttpResponse.json({ _id: planId, planName: `mock-plan-name-${planId}` });
        }),
      ],
    },
  },
  render: () => html`<cts-log-list is-public></cts-log-list>`,
  async play({ canvasElement }) {
    await waitForLogsToLoad(canvasElement);

    // Owner pill suppressed in public listing.
    expect(canvasElement.querySelectorAll(".log-owner").length).toBe(0);
    // Config button suppressed in public listing.
    expect(canvasElement.querySelectorAll(".showConfigBtn").length).toBe(0);
  },
};

export const Loading = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/log", async () => {
          // Hold the response open long enough for the spinner to render.
          await delay(10000);
          return HttpResponse.json(paginationEnvelope(MOCK_LOG_LIST));
        }),
        planResolveHandler(),
      ],
    },
  },
  render: () => html`<cts-log-list></cts-log-list>`,
  async play({ canvasElement }) {
    // Spinner is visible while the fetch is in flight.
    await waitFor(() => {
      const spinner = canvasElement.querySelector(".cts-log-list-loading cts-spinner");
      expect(spinner).not.toBeNull();
    });
  },
};

export const EmptyDataset = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/log", () => HttpResponse.json(paginationEnvelope([]))),
        planResolveHandler(),
      ],
    },
  },
  render: () => html`<cts-log-list></cts-log-list>`,
  async play({ canvasElement }) {
    await waitForLogsToLoad(canvasElement);
    const empty = canvasElement.querySelector('[data-testid="log-list-empty"]');
    expect(empty).not.toBeNull();
    expect(empty.getAttribute("heading")).toBe("No logs to show");
  },
};

export const FilterByStatus = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/log", () => HttpResponse.json(paginationEnvelope(MOCK_LOG_LIST))),
        planResolveHandler(),
      ],
    },
  },
  render: () => html`<cts-log-list></cts-log-list>`,
  async play({ canvasElement }) {
    await waitForLogsToLoad(canvasElement);

    // Initial: all 6 cards visible.
    let items = canvasElement.querySelectorAll('[data-testid="log-list-item"]');
    expect(items.length).toBe(MOCK_LOG_LIST.length);

    // Click the RUNNING status chip — only the running row should remain.
    const runningChip = canvasElement.querySelector('.cts-log-filter-chip[data-status="RUNNING"]');
    expect(runningChip).not.toBeNull();
    await userEvent.click(runningChip);

    await waitFor(() => {
      items = canvasElement.querySelectorAll('[data-testid="log-list-item"]');
      expect(items.length).toBe(1);
    });
    expect(canvasElement.textContent).toContain("fapi2-running");

    // Active-filter summary appears with match count.
    const summary = canvasElement.querySelector('[data-testid="active-filter-summary"]');
    expect(summary).not.toBeNull();
    expect(summary.textContent).toContain("Status: running");
    expect(summary.textContent).toContain("(1 match)");

    // URL reflects the filter via history.replaceState.
    expect(window.location.search).toContain("status=running");
  },
};

export const FilterActiveZeroMatches = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/log", () => HttpResponse.json(paginationEnvelope(MOCK_LOG_LIST))),
        planResolveHandler(),
      ],
    },
  },
  render: () => html`<cts-log-list></cts-log-list>`,
  async play({ canvasElement }) {
    await waitForLogsToLoad(canvasElement);

    // Activate SKIPPED — no fixture row has SKIPPED.
    const skippedChip = canvasElement.querySelector('.cts-log-filter-chip[data-result="SKIPPED"]');
    await userEvent.click(skippedChip);

    await waitFor(() => {
      const empty = canvasElement.querySelector('[data-testid="log-list-empty"]');
      expect(empty).not.toBeNull();
      expect(empty.getAttribute("heading")).toBe("No logs match the active filter");
    });
  },
};

export const SearchActive = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/log", () => HttpResponse.json(paginationEnvelope(MOCK_LOG_LIST))),
        planResolveHandler(),
      ],
    },
  },
  render: () => html`<cts-log-list></cts-log-list>`,
  async play({ canvasElement }) {
    await waitForLogsToLoad(canvasElement);

    const search = canvasElement.querySelector(".cts-log-list-search input");
    await userEvent.type(search, "rotate");

    await waitFor(() => {
      const items = canvasElement.querySelectorAll('[data-testid="log-list-item"]');
      expect(items.length).toBe(1);
    });
    expect(canvasElement.textContent).toContain("oidcc-server-rotate-keys");
  },
};

export const SortByName = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/log", () => HttpResponse.json(paginationEnvelope(MOCK_LOG_LIST))),
        planResolveHandler(),
      ],
    },
  },
  render: () => html`<cts-log-list></cts-log-list>`,
  async play({ canvasElement }) {
    await waitForLogsToLoad(canvasElement);

    const sortSelect = canvasElement.querySelector(".cts-log-list-sort select");
    await userEvent.selectOptions(sortSelect, "name-asc");

    await waitFor(() => {
      const names = Array.from(canvasElement.querySelectorAll(".cts-log-card-name")).map((el) =>
        el.textContent.trim(),
      );
      // Alphabetical: fapi2-running, fapi2-waiting, oidcc-server, ...
      expect(names[0].startsWith("f")).toBe(true);
    });
  },
};

export const Paginated60Items = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/log", () => HttpResponse.json(paginationEnvelope(MOCK_LOG_LIST_LARGE))),
        planResolveHandler(),
      ],
    },
  },
  render: () => html`<cts-log-list></cts-log-list>`,
  async play({ canvasElement }) {
    await waitForLogsToLoad(canvasElement);

    let items = canvasElement.querySelectorAll('[data-testid="log-list-item"]');
    expect(items.length).toBe(25);

    const showMore = canvasElement.querySelector('[data-testid="log-list-show-more"]');
    expect(showMore).not.toBeNull();
    expect(showMore.getAttribute("label")).toContain("25 of 60");

    // Click the inner <button> of the cts-button host.
    const inner = showMore.querySelector("button");
    await userEvent.click(inner);

    await waitFor(() => {
      items = canvasElement.querySelectorAll('[data-testid="log-list-item"]');
      expect(items.length).toBe(50);
    });
  },
};

export const Truncated = {
  parameters: {
    msw: {
      handlers: [
        http.get("/api/log", () =>
          HttpResponse.json({
            draw: 1,
            recordsTotal: 5000,
            recordsFiltered: 5000,
            data: MOCK_LOG_LIST,
          }),
        ),
        planResolveHandler(),
      ],
    },
  },
  render: () => html`<cts-log-list></cts-log-list>`,
  async play({ canvasElement }) {
    await waitForLogsToLoad(canvasElement);

    const hint = canvasElement.querySelector('[data-testid="log-list-truncation"]');
    expect(hint).not.toBeNull();
    expect(hint.textContent).toContain("Refine the filter to narrow further");
  },
};
