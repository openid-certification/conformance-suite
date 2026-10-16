import { html } from "lit";
import { expect, within, waitFor, userEvent } from "storybook/test";
import { MOCK_PLANS, MOCK_GROUPED_PLANS } from "@fixtures/mock-plans.js";
import "./cts-test-selector.js";

export default {
  title: "Components/cts-test-selector",
  component: "cts-test-selector",
  // Favorites is always-on, so selecting a family (FamilyFilter) now persists
  // the choice to cts:test-selector-filter, and the "Show
  // non-certifiable plans" checkbox persists to
  // cts:test-selector-show-non-certifiable. Clear both before every
  // story so neither leaks into a sibling story that expects the default "All
  // specifications" view with non-certifiable plans hidden. The favorites list itself
  // needs no reset: it is server-side account data, passed in as a prop per
  // story.
  beforeEach: () => {
    localStorage.removeItem("cts:test-selector-filter");
    localStorage.removeItem("cts:test-selector-show-non-certifiable");
  },
};

const SEARCH_FILTER_QUERY = "Security Profile";
const SEARCH_FILTER_EXPECTED_ROWS = 1;

export const Default = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const canvas = within(canvasElement);
    await step("search input renders with placeholder", async () => {
      const searchInput = canvasElement.querySelector(".oidf-test-selector__search");
      expect(searchInput).toBeTruthy();
      expect(searchInput.getAttribute("placeholder")).toBe("Search test plans...");
    });
    await step("family filter renders as an accessible sized listbox", async () => {
      const select = canvasElement.querySelector(".oidf-test-selector__family");
      expect(select).toBeTruthy();
      // The family filter has no visible label in the toolbar — assistive tech relies on
      // the explicit aria-label for an accessible name.
      expect(select.getAttribute("aria-label")).toBe("Filter test plans by specification family");
      // Rendered as an always-open listbox (size attribute) rather than a
      // dropdown, so every spec family is visible in the left rail at once.
      expect(select.getAttribute("size")).toBe("16");
      // A sized listbox does not auto-select its first option, so the "All
      // specifications" option (value="") is selected explicitly by default.
      expect(select.value).toBe("");
      expect(select.selectedOptions[0]?.textContent?.trim()).toBe("All specifications");
    });
    await step("all plans render as rows", async () => {
      const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
      expect(items.length).toBe(MOCK_PLANS.length);
      expect(canvas.getByText("OpenID Connect Core: Basic Certification Profile")).toBeTruthy();
    });
    await step("no Bootstrap remnants in the rendered output", async () => {
      expect(canvasElement.querySelector(".form-control")).toBeNull();
      expect(canvasElement.querySelector(".form-select")).toBeNull();
      expect(canvasElement.querySelector(".list-group-item")).toBeNull();
    });
  },
};

/**
 * Loading state: while the page fetches `/api/plan/available`, the host is
 * mounted with `loading` set so the list area shows the shared
 * `<cts-loading-state>` spinner instead of rows or the empty message. The rail
 * (search + family filter) stays visible, matching cts-log-list /
 * cts-plan-list, which keep their toolbar during load.
 */
export const Loading = {
  render: () => html`<cts-test-selector loading></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-test-selector");
    await host.updateComplete;

    const loader = host.querySelector(".oidf-test-selector__list cts-loading-state");
    await step(
      "the list area shows the shared loader — not rows, not the empty message",
      async () => {
        expect(loader).toBeTruthy();
        expect(host.querySelector(".oidf-test-selector__row")).toBeNull();
        expect(host.querySelector(".oidf-test-selector__empty")).toBeNull();
      },
    );

    await step("caption + spinner render through the shared component", async () => {
      expect(loader.querySelector("cts-spinner")).toBeTruthy();
      expect(loader.querySelector(".cts-loading-state-caption").textContent.trim()).toBe(
        "Loading test plans…",
      );
    });

    await step("the search rail stays visible during load", async () => {
      expect(host.querySelector(".oidf-test-selector__search")).toBeTruthy();
    });
  },
};

export const SearchFilter = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const searchInput = canvasElement.querySelector(".oidf-test-selector__search");
    await userEvent.type(searchInput, SEARCH_FILTER_QUERY);
    await waitFor(() => {
      const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
      expect(items.length).toBe(SEARCH_FILTER_EXPECTED_ROWS);
    });
  },
};

/**
 * The trailing × button clears the search and refocuses the input.
 * Mirrors the simplified live-debounced shape of the cts-data-table
 * pattern — no submit affordance, since filtering happens as the user
 * types.
 */
export const SearchClearButton = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );

    await step("no clear button while the field is empty", async () => {
      expect(canvasElement.querySelector(".oidf-test-selector__search-clear")).toBeNull();
    });

    await step("typing filters the rows and reveals the clear button", async () => {
      await userEvent.type(searchInput, SEARCH_FILTER_QUERY);
      await waitFor(() => {
        const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
        expect(items.length).toBe(SEARCH_FILTER_EXPECTED_ROWS);
      });

      const clearBtn = /** @type {HTMLButtonElement} */ (
        canvasElement.querySelector(".oidf-test-selector__search-clear")
      );
      expect(clearBtn).toBeTruthy();
      expect(clearBtn.getAttribute("aria-label")).toBe("Clear search");
    });

    await step(
      "clicking clear resets the field, restores rows, and refocuses the input",
      async () => {
        const clearBtn = /** @type {HTMLButtonElement} */ (
          canvasElement.querySelector(".oidf-test-selector__search-clear")
        );
        await userEvent.click(clearBtn);

        // Field is empty, all rows are back, clear button is gone, focus
        // is back on the input so the user can keep typing.
        await waitFor(() => {
          expect(searchInput.value).toBe("");
          const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
          expect(items.length).toBe(MOCK_PLANS.length);
        });
        expect(canvasElement.querySelector(".oidf-test-selector__search-clear")).toBeNull();
        expect(document.activeElement).toBe(searchInput);
      },
    );
  },
};

/**
 * Pressing Escape inside the search input clears it. Same affordance
 * as the clear button, exposed to keyboard users without reaching for
 * the mouse.
 */
export const SearchEscapeClears = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );

    await step("typing filters the rows", async () => {
      await userEvent.type(searchInput, SEARCH_FILTER_QUERY);
      await waitFor(() => {
        const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
        expect(items.length).toBe(SEARCH_FILTER_EXPECTED_ROWS);
      });
    });

    await step("Escape clears the field and restores all rows", async () => {
      searchInput.focus();
      await userEvent.keyboard("{Escape}");

      await waitFor(() => {
        expect(searchInput.value).toBe("");
        const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
        expect(items.length).toBe(MOCK_PLANS.length);
      });
    });
  },
};

export const FamilyFilter = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const select = canvasElement.querySelector(".oidf-test-selector__family");
    await userEvent.selectOptions(select, "OIDCC");
    await waitFor(() => {
      const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
      expect(items.length).toBe(3);
    });
  },
};

export const SelectPlan = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    /** @type {any[]} */
    const dispatched = [];
    canvasElement.addEventListener("cts-plan-select", (e) => {
      dispatched.push(/** @type {CustomEvent} */ (e).detail);
    });
    const items = canvasElement.querySelectorAll(".oidf-test-selector__row");

    await step("clicking a row dispatches cts-plan-select tagged via:'click'", async () => {
      await userEvent.click(items[1]);
      expect(dispatched.length).toBe(1);
      expect(dispatched[0].plan.planName).toBe(items[1].getAttribute("data-plan-name"));
      // The picker reports how the selection was made via the `via` channel
      // so a consumer can distinguish a mouse click from keyboard activation;
      // a mouse click carries 'click'.
      expect(dispatched[0].via).toBe("click");
    });

    await step("the clicked row becomes active", async () => {
      await waitFor(() => {
        expect(items[1].classList.contains("is-active")).toBe(true);
      });
    });
  },
};

/**
 * ArrowDown in the search input moves real DOM focus into the first
 * visible row. This is the entry point of the keyboard-only flow that
 * lets a user search → arrow → Enter without touching the mouse.
 */
export const ArrowDownFromSearchFocusesFirstRow = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );

    await step("ArrowDown from the search input", async () => {
      searchInput.focus();
      await userEvent.keyboard("{ArrowDown}");
    });

    await step("focus lands on the first row with roving tabindex=0", async () => {
      await waitFor(() => {
        const firstRow = canvasElement.querySelector(".oidf-test-selector__row");
        expect(document.activeElement).toBe(firstRow);
        // Roving tabindex: the focused row carries tabindex=0; the rest
        // are -1 so Tab escapes the list cleanly.
        expect(firstRow?.getAttribute("tabindex")).toBe("0");
      });
    });
  },
};

/**
 * Once focus is on a row, ArrowDown/ArrowUp rove across rows in
 * document order. ArrowDown on the last row is a no-op (no wrap).
 */
export const ArrowNavRovesAcrossRows = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );
    const rows = canvasElement.querySelectorAll(".oidf-test-selector__row");

    await step("ArrowDown from search focuses the first row", async () => {
      searchInput.focus();
      await userEvent.keyboard("{ArrowDown}");
      await waitFor(() => expect(document.activeElement).toBe(rows[0]));
    });

    await step("ArrowDown roves to the next row", async () => {
      await userEvent.keyboard("{ArrowDown}");
      await waitFor(() => expect(document.activeElement).toBe(rows[1]));
    });

    await step("ArrowUp roves back to the previous row", async () => {
      await userEvent.keyboard("{ArrowUp}");
      await waitFor(() => expect(document.activeElement).toBe(rows[0]));
    });
  },
};

/**
 * ArrowUp on the first row returns focus to the search input — closing
 * the loop so the user can refine the query without reaching for the
 * mouse or hammering Shift+Tab through all the rows above.
 */
export const ArrowUpFromFirstRowReturnsToSearch = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );
    const firstRow = canvasElement.querySelector(".oidf-test-selector__row");

    await step("ArrowDown from search focuses the first row", async () => {
      searchInput.focus();
      await userEvent.keyboard("{ArrowDown}");
      await waitFor(() => expect(document.activeElement).toBe(firstRow));
    });

    await step("ArrowUp on the first row returns focus to the search input", async () => {
      await userEvent.keyboard("{ArrowUp}");
      await waitFor(() => expect(document.activeElement).toBe(searchInput));
    });
  },
};

/**
 * Pressing Enter on a focused row commits the selection and tags the
 * dispatched event with via:'keyboard'. The `via` channel lets a consumer
 * distinguish keyboard activation from a mouse click.
 *
 * Asserts exactly one dispatch — guards against the keyup→synthetic-click
 * double-fire that <button> elements produce on keyboard activation.
 */
export const EnterOnFocusedRowSelects = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    /** @type {any[]} */
    const dispatched = [];
    canvasElement.addEventListener("cts-plan-select", (e) => {
      dispatched.push(/** @type {CustomEvent} */ (e).detail);
    });

    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );
    const firstRow = canvasElement.querySelector(".oidf-test-selector__row");

    await step("ArrowDown from search focuses the first row", async () => {
      searchInput.focus();
      await userEvent.keyboard("{ArrowDown}");
      await waitFor(() => expect(document.activeElement).toBe(firstRow));
    });

    await step("Enter commits exactly one selection tagged via:'keyboard'", async () => {
      await userEvent.keyboard("{Enter}");

      await waitFor(() => {
        expect(dispatched.length).toBe(1);
        expect(dispatched[0].plan.planName).toBe(firstRow.getAttribute("data-plan-name"));
        expect(dispatched[0].via).toBe("keyboard");
      });
    });
  },
};

/**
 * When the filter clears the result list, ArrowDown in the search has
 * nothing to focus and must be a no-op — no event, no focus change,
 * no thrown error from indexing into an empty list.
 */
export const ArrowDownOnEmptyListIsNoOp = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    /** @type {any[]} */
    const dispatched = [];
    canvasElement.addEventListener("cts-plan-select", (e) => {
      dispatched.push(/** @type {CustomEvent} */ (e).detail);
    });

    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );

    await step("a non-matching query empties the result list", async () => {
      await userEvent.type(searchInput, "nonexistent-plan-xyz");
      await waitFor(() => {
        const items = canvasElement.querySelectorAll(".oidf-test-selector__row");
        expect(items.length).toBe(0);
      });
    });

    await step("ArrowDown is a no-op — focus stays, no event fires", async () => {
      searchInput.focus();
      await userEvent.keyboard("{ArrowDown}");

      expect(document.activeElement).toBe(searchInput);
      expect(dispatched.length).toBe(0);
    });
  },
};

/**
 * The `selected` attribute is the externally-driven counterpart of the click
 * path: callers (e.g. schedule-test.html's cts-plan-selected listener syncing
 * the chosen plan back to the search list) set `planSearch.selected = name`
 * to highlight the matching row without triggering a `cts-plan-select` event.
 * This story exercises that path independently of the click handler so the
 * highlight contract is locked even when the selection is driven
 * programmatically (deep-link, edit-plan, load-last-config).
 */
export const WithSelection = {
  render: () => html`
    <cts-test-selector
      .plans=${MOCK_PLANS}
      selected="fapi2-security-profile-final-test-plan"
    ></cts-test-selector>
  `,
  async play({ canvasElement }) {
    // The externally-driven path must not fabricate a synthetic
    // `cts-plan-select` — that event signals "user picked this plan."
    // schedule-test.html routes click events through dispatchPlanSelection,
    // which clears the in-flight config. If WithSelection re-fired the event
    // every time a caller set `selected`, a programmatic highlight (deep-link,
    // edit-plan, load-last-config) would wipe the config it just restored.
    let dispatched = 0;
    canvasElement.addEventListener("cts-plan-select", () => {
      dispatched += 1;
    });

    const rows = canvasElement.querySelectorAll(".oidf-test-selector__row");
    const activeRows = Array.from(rows).filter((r) => r.classList.contains("is-active"));
    expect(activeRows.length).toBe(1);
    expect(activeRows[0].getAttribute("data-plan-name")).toBe(
      "fapi2-security-profile-final-test-plan",
    );
    expect(dispatched).toBe(0);
  },
};

/**
 * Hover swaps the row background to `--ink-50`. We can't observe the actual
 * pseudo-class style from JSDOM, but we can verify the rule is registered in
 * the injected stylesheet so a regression in the head-injection pipeline is
 * caught.
 */
export const RowHoverStyleRegistered = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play() {
    const styleEl = document.getElementById("cts-test-selector-styles");
    expect(styleEl).toBeTruthy();
    const css = styleEl?.textContent || "";
    expect(css).toContain(".oidf-test-selector__row:hover");
    expect(css).toContain("var(--ink-50)");
    expect(css).toContain(".oidf-test-selector__row:focus-visible");
    expect(css).toContain("outline-offset: -2px");
  },
};

/**
 * The list clips its rows to its rounded corners (overflow: hidden) and the
 * rows sit flush against its edges, so an outer focus ring loses its top
 * and left segments. The row and star rings are drawn inside their own
 * box (negative outline-offset) so every edge paints.
 */
export const RowAndStarFocusRingsAreInset = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const searchInput = /** @type {HTMLInputElement} */ (
      canvasElement.querySelector(".oidf-test-selector__search")
    );
    const list = /** @type {HTMLElement} */ (
      canvasElement.querySelector(".oidf-test-selector__list")
    );
    const firstRow = /** @type {HTMLElement} */ (
      canvasElement.querySelector(".oidf-test-selector__row")
    );
    const firstStar = /** @type {HTMLElement} */ (
      canvasElement.querySelector(".oidf-test-selector__fav")
    );

    /** @param {HTMLElement} el */
    const expectInsetRing = (el) => {
      const style = getComputedStyle(el);
      expect(style.outlineStyle).toBe("solid");
      expect(parseFloat(style.outlineWidth)).toBeGreaterThan(0);
      // Negative offset at least as wide as the ring keeps it inside the box.
      expect(parseFloat(style.outlineOffset)).toBeLessThanOrEqual(-parseFloat(style.outlineWidth));
      expect(style.boxShadow).toBe("none");
    };

    await step("the list clips its children", async () => {
      expect(getComputedStyle(list).overflow).toBe("hidden");
    });

    await step("ArrowDown from the search focuses the first row with an inset ring", async () => {
      searchInput.focus();
      await userEvent.keyboard("{ArrowDown}");
      await waitFor(() => expect(document.activeElement).toBe(firstRow));
      expectInsetRing(firstRow);
    });

    await step("Tab moves to the row's star, also with an inset ring", async () => {
      await userEvent.keyboard("{Tab}");
      await waitFor(() => expect(document.activeElement).toBe(firstStar));
      expectInsetRing(firstStar);
    });
  },
};

/**
 * The family filter renders as a listbox (size attribute), so long spec
 * names must wrap instead of clipping to one row. We can't read the
 * computed option layout from the test runner, but we can assert the
 * wrapping rule is present in the injected stylesheet so a regression in
 * the head-injection pipeline is caught.
 */
export const FamilyListboxStyleRegistered = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play() {
    const styleEl = document.getElementById("cts-test-selector-styles");
    expect(styleEl).toBeTruthy();
    const css = styleEl?.textContent || "";
    expect(css).toContain(".oidf-test-selector__family option");
    expect(css).toContain("white-space: normal");
  },
};

export const NoResults = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    const searchInput = canvasElement.querySelector(".oidf-test-selector__search");
    await userEvent.type(searchInput, "nonexistent-plan-xyz");
    await waitFor(() => {
      expect(canvas.getByText("No plans match your search")).toBeTruthy();
    });
  },
};

export const EmptyPlans = {
  render: () => html`<cts-test-selector .plans=${[]}></cts-test-selector>`,
  async play({ canvasElement }) {
    const canvas = within(canvasElement);
    expect(canvas.getByText("No plans match your search")).toBeTruthy();
  },
};

// Plan: docs/plans/2026-05-27-001-feat-autolink-and-format-test-prose-plan.md
// (U2). The row summary is a markdown teaser nested inside the plan-row
// <button>, so it renders inline-only: backtick spans become <code>, but no
// <a> (interactive nesting is invalid) and no <p> (breaks the one-line teaser).
const PLAN_WITH_MARKDOWN_SUMMARY = [
  {
    planName: "demo-markdown-summary-plan",
    displayName: "Markdown summary demo",
    specFamily: "OpenID Connect",
    modules: [{ testModule: "m1" }],
    summary:
      "Checks the `purpose` parameter.\n\nSee https://openid.net/specs/x for the rules and the request_uri handling.",
  },
];

export const RowSummaryRendersInlineMarkdown = {
  render: () => html`<cts-test-selector .plans=${PLAN_WITH_MARKDOWN_SUMMARY}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const summary = await waitFor(() => {
      const el = canvasElement.querySelector(".oidf-test-selector__row-summary");
      if (!el) throw new Error("row-summary not yet rendered");
      return el;
    });

    await step("backtick span renders as inline <code>", async () => {
      const code = summary.querySelector("code");
      expect(code).toBeTruthy();
      expect(code.textContent).toBe("purpose");
    });

    await step("no anchors or block paragraphs in the inline teaser", async () => {
      // No anchors (would be invalid interactive content inside the row <button>)
      // and no block paragraphs (the teaser stays a single line).
      expect(summary.querySelector("a")).toBeNull();
      expect(summary.querySelector("p")).toBeNull();
    });

    await step("snake_case prose is preserved and the backtick does not leak", async () => {
      expect(summary.textContent).toContain("request_uri");
      expect(summary.textContent).not.toContain("`");
    });

    await step("the autolinked URL degrades to readable text", async () => {
      // The <a> is stripped, its text survives, rather than vanishing from the teaser.
      expect(summary.textContent).toContain("https://openid.net/specs/x");
    });
  },
};

export const ModuleCount = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    const badges = canvasElement.querySelectorAll(
      '[data-plan-name="oidcc-basic-certification-test-plan"] .oidf-test-selector__row-count',
    );
    expect(badges.length).toBe(1);
    expect(badges[0].textContent).toBe("4");
    // The count carries an aria-label so screen readers announce its meaning
    // ("4 test modules") rather than a bare "4".
    expect(badges[0].getAttribute("aria-label")).toBe("4 test modules");
    // ...and a cts-tooltip explains it on hover for sighted users.
    const tooltip = badges[0].closest("cts-tooltip");
    expect(tooltip).not.toBeNull();
    expect(tooltip.getAttribute("content")).toBe("Number of test modules in this plan");
  },
};

/** The labels of the rendered entity group headings. */
function groupHeadings(canvasElement) {
  return Array.from(canvasElement.querySelectorAll(".oidf-test-selector__group-label")).map((h) =>
    h.textContent.trim(),
  );
}

/** The `[variant, label]` of every status badge in a row. */
function rowBadges(row) {
  return Array.from(row.querySelectorAll("cts-badge")).map((b) => [
    b.getAttribute("variant"),
    b.getAttribute("label"),
  ]);
}

/** Tick "Show non-certifiable plans", which MOCK_GROUPED_PLANS' non-HAIP plans need. */
async function showNonCertifiablePlans(canvasElement) {
  await userEvent.click(
    canvasElement.querySelector(".oidf-test-selector__certification-toggle input"),
  );
}

/** The `data-plan-name` of every rendered row, in order. */
function rowPlanNames(canvasElement) {
  return Array.from(canvasElement.querySelectorAll(".oidf-test-selector__row")).map((r) =>
    r.getAttribute("data-plan-name"),
  );
}

/**
 * Selecting a family whose plans test more than one entity groups the rows
 * under an entity heading per `profile` with its plan count, names each row by
 * its version-led `shortName` (the full `displayName` moves to the tooltip),
 * and shows its status badges. Arrow keys rove straight across group
 * boundaries.
 */
export const FamilyGroupedByEntity = {
  render: () =>
    html`<cts-test-selector .plans=${[...MOCK_PLANS, ...MOCK_GROUPED_PLANS]}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    await showNonCertifiablePlans(canvasElement);
    const select = canvasElement.querySelector(".oidf-test-selector__family");
    await userEvent.selectOptions(select, "OID4VP");

    await step("rows are grouped under entity headings, newest version first", async () => {
      await waitFor(() =>
        expect(groupHeadings(canvasElement)).toEqual(["OpenID4VP Verifier", "OpenID4VP Wallet"]),
      );
      const counts = Array.from(
        canvasElement.querySelectorAll(".oidf-test-selector__group-count"),
      ).map((c) => c.textContent.trim());
      expect(counts).toEqual(["3 plans", "1 plan"]);
      expect(rowPlanNames(canvasElement)).toEqual([
        "oid4vp-1final-verifier-haip-test-plan",
        "oid4vp-1final-verifier-test-plan",
        "oid4vp-id2-verifier-test-plan",
        "oid4vp-1final-wallet-haip-test-plan",
      ]);
      const group = canvasElement.querySelector(".oidf-test-selector__group");
      const heading = group.querySelector(".oidf-test-selector__group-heading");
      expect(group.getAttribute("role")).toBe("group");
      expect(group.getAttribute("aria-labelledby")).toBe(heading.id);
      expect(group.querySelector('[role="list"] [role="listitem"]')).toBeTruthy();
    });

    await step("rows show the short name and their status badges", async () => {
      const row = canvasElement.querySelector(
        '[data-plan-name="oid4vp-1final-verifier-test-plan"]',
      );
      const name = row.querySelector(".oidf-test-selector__row-name");
      expect(name.textContent.trim()).toBe("1.0 Final");
      expect(rowBadges(row)).toEqual([["warn", "Alpha"]]);
      expect(row.querySelector(".oidf-test-selector__row-family")).toBeNull();
      const haip = canvasElement.querySelector(
        '[data-plan-name="oid4vp-1final-verifier-haip-test-plan"]',
      );
      expect(haip.querySelector(".oidf-test-selector__row-name").textContent.trim()).toBe(
        "1.0 Final + HAIP",
      );
      expect(rowBadges(haip)).toEqual([["pass", "Certification"]]);
    });

    await step("an unselected row carries its full name as description and tooltip", async () => {
      const row = canvasElement.querySelector(
        '[data-plan-name="oid4vp-1final-verifier-test-plan"]',
      );
      const fullName = canvasElement.querySelector(`#${row.getAttribute("aria-describedby")}`);
      expect(fullName.textContent.trim()).toBe(MOCK_GROUPED_PLANS[1].displayName);
      expect(fullName.hidden).toBe(true);
      expect(
        row.querySelector(".oidf-test-selector__row-title cts-tooltip")?.getAttribute("content"),
      ).toBe(MOCK_GROUPED_PLANS[1].displayName);
    });

    await step("the selected row shows its full name instead", async () => {
      const host = /** @type {any} */ (canvasElement.querySelector("cts-test-selector"));
      host.selected = "oid4vp-1final-verifier-test-plan";
      await host.updateComplete;
      const row = canvasElement.querySelector(
        '[data-plan-name="oid4vp-1final-verifier-test-plan"]',
      );
      const fullName = row.querySelector(".oidf-test-selector__row-full-name");
      expect(fullName.hidden).toBe(false);
      expect(fullName.textContent.trim()).toBe(MOCK_GROUPED_PLANS[1].displayName);
      expect(row.hasAttribute("aria-describedby")).toBe(false);
      expect(row.querySelector(".oidf-test-selector__row-title cts-tooltip")).toBeNull();
      host.selected = "";
      await host.updateComplete;
    });

    await step("ArrowDown crosses from the last verifier to the wallet", async () => {
      const rows = canvasElement.querySelectorAll(".oidf-test-selector__row");
      const searchInput = canvasElement.querySelector(".oidf-test-selector__search");
      searchInput.focus();
      await userEvent.keyboard("{ArrowDown}{ArrowDown}{ArrowDown}");
      await waitFor(() => expect(document.activeElement).toBe(rows[2]));
      await userEvent.keyboard("{ArrowDown}");
      await waitFor(() => expect(document.activeElement).toBe(rows[3]));
      expect(document.activeElement?.getAttribute("data-plan-name")).toBe(
        "oid4vp-1final-wallet-haip-test-plan",
      );
    });
  },
};

/**
 * Typing in a grouped family keeps the headings even when only one entity is
 * left, so the layout doesn't jump between grouped and flat while typing. The
 * search also matches the short name.
 */
export const SearchKeepsHeadingsStable = {
  render: () => html`<cts-test-selector .plans=${MOCK_GROUPED_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    await showNonCertifiablePlans(canvasElement);
    const select = canvasElement.querySelector(".oidf-test-selector__family");
    await userEvent.selectOptions(select, "OID4VP");
    await userEvent.type(canvasElement.querySelector(".oidf-test-selector__search"), "ID2");
    await waitFor(() => expect(groupHeadings(canvasElement)).toEqual(["OpenID4VP Verifier"]));
    expect(rowPlanNames(canvasElement)).toEqual(["oid4vp-id2-verifier-test-plan"]);
    expect(canvasElement.querySelector(".oidf-test-selector__group-count").textContent.trim()).toBe(
      "1 plan",
    );
  },
};

/**
 * A family whose plans all test one entity has nothing to group by, so it
 * renders a single flat list with no heading — but still uses short names.
 */
export const SingleEntityFamilyHasNoHeadings = {
  render: () =>
    html`<cts-test-selector
      .plans=${MOCK_GROUPED_PLANS.filter((p) => p.profile === "Test a OpenID4VP Verifier")}
    ></cts-test-selector>`,
  async play({ canvasElement }) {
    await showNonCertifiablePlans(canvasElement);
    const select = canvasElement.querySelector(".oidf-test-selector__family");
    await userEvent.selectOptions(select, "OID4VP");
    await waitFor(() => expect(rowPlanNames(canvasElement)).toHaveLength(3));
    expect(groupHeadings(canvasElement)).toEqual([]);
    expect(canvasElement.querySelector(".oidf-test-selector__list").getAttribute("role")).toBe(
      "list",
    );
    const names = Array.from(canvasElement.querySelectorAll(".oidf-test-selector__row-name")).map(
      (n) => n.textContent.trim(),
    );
    expect(names).toEqual(["1.0 Final + HAIP", "1.0 Final", "ID2"]);
  },
};

/**
 * "All specifications" mixes families, so rows keep the full displayName and
 * family label (no headings) and are sorted by family, then name. The status
 * badge still shows.
 */
export const AllViewStaysFlat = {
  render: () =>
    html`<cts-test-selector .plans=${[...MOCK_GROUPED_PLANS, ...MOCK_PLANS]}></cts-test-selector>`,
  async play({ canvasElement }) {
    await showNonCertifiablePlans(canvasElement);
    await waitFor(() =>
      expect(rowPlanNames(canvasElement)).toHaveLength(
        MOCK_GROUPED_PLANS.length + MOCK_PLANS.length,
      ),
    );
    expect(groupHeadings(canvasElement)).toEqual([]);
    const families = Array.from(
      canvasElement.querySelectorAll(".oidf-test-selector__row-family"),
    ).map((f) => f.textContent.trim());
    expect(families).toEqual([...families].sort((a, b) => a.localeCompare(b)));
    const row = canvasElement.querySelector('[data-plan-name="oid4vp-1final-verifier-test-plan"]');
    expect(row.querySelector(".oidf-test-selector__row-name").textContent.trim()).toBe(
      MOCK_GROUPED_PLANS[1].displayName,
    );
    expect(rowBadges(row)).toEqual([["warn", "Alpha"]]);
  },
};

const STATUS_PLANS = [
  {
    planName: "certified-plan",
    displayName: "Spec A: Certified",
    shortName: "Certified",
    certifiable: true,
    alpha: false,
    profile: "Test a OpenID Provider / Authorization Server",
    specFamily: "Spec A",
  },
  {
    planName: "certified-alpha-plan",
    displayName: "Spec A: Certified alpha (alpha version - may be incomplete or incorrect)",
    shortName: "Certified alpha",
    certifiable: true,
    alpha: true,
    profile: "Test a OpenID Provider / Authorization Server",
    specFamily: "Spec A",
  },
  {
    planName: "uncertified-plan",
    displayName: "Spec A: Uncertified (alpha version - not part of certification program)",
    shortName: "Uncertified",
    certifiable: false,
    alpha: true,
    profile: "Test a OpenID Provider / Authorization Server",
    specFamily: "Spec A",
  },
  {
    planName: "uncertified-only-plan",
    displayName: "Spec B: Uncertified (alpha version - not part of certification program)",
    shortName: "Uncertified",
    certifiable: false,
    alpha: true,
    profile: "Test a OpenID Provider / Authorization Server",
    specFamily: "Spec B",
  },
];

/** The values of the family listbox's spec-family options. */
function familyOptions(canvasElement) {
  return Array.from(canvasElement.querySelectorAll(".oidf-test-selector__family option"))
    .map((o) => o.value)
    .filter((v) => v && !v.startsWith("__"));
}

/**
 * Plans outside the certification program are hidden until "Show
 * non-certifiable plans" is ticked, along with any family that has no other plans. A certifiable
 * alpha plan stays listed and carries both badges. A search that only matches
 * hidden plans offers to show them, and the checkbox state is persisted.
 */
export const NonCertifiablePlansHiddenByDefault = {
  render: () => html`<cts-test-selector .plans=${STATUS_PLANS}></cts-test-selector>`,
  async play({ canvasElement, step }) {
    const toggle = () =>
      /** @type {HTMLInputElement} */ (
        canvasElement.querySelector(".oidf-test-selector__certification-toggle input")
      );

    await step("non-certifiable plans and their families are hidden", async () => {
      await waitFor(() =>
        expect(rowPlanNames(canvasElement)).toEqual(["certified-plan", "certified-alpha-plan"]),
      );
      expect(familyOptions(canvasElement)).toEqual(["Spec A"]);
      expect(toggle().checked).toBe(false);
      expect(
        canvasElement.querySelector(".oidf-test-selector__certification-toggle").textContent.trim(),
      ).toBe("Show non-certifiable plans (2)");
    });

    await step("a certifiable alpha plan carries both badges", async () => {
      const row = canvasElement.querySelector('[data-plan-name="certified-alpha-plan"]');
      expect(rowBadges(row)).toEqual([
        ["pass", "Certification"],
        ["warn", "Alpha"],
      ]);
    });

    await step("a search matching only hidden plans offers to show them", async () => {
      const search = canvasElement.querySelector(".oidf-test-selector__search");
      await userEvent.type(search, "Uncertified");
      const empty = canvasElement.querySelector(".oidf-test-selector__empty");
      await waitFor(() => expect(empty.textContent).toContain("2 non-certifiable plans match."));
      await userEvent.click(
        within(empty).getByRole("button", { name: "Show non-certifiable plans" }),
      );
      await waitFor(() =>
        expect(rowPlanNames(canvasElement)).toEqual(["uncertified-plan", "uncertified-only-plan"]),
      );
      expect(document.activeElement).toBe(search);
      expect(toggle().checked).toBe(true);
      expect(localStorage.getItem("cts:test-selector-show-non-certifiable")).toBe("true");
      expect(familyOptions(canvasElement)).toEqual(["Spec A", "Spec B"]);
      expect(rowBadges(canvasElement.querySelector('[data-plan-name="uncertified-plan"]'))).toEqual(
        [["warn", "Alpha"]],
      );
    });

    await step("unticking leaves a family that only had non-certifiable plans", async () => {
      await userEvent.clear(canvasElement.querySelector(".oidf-test-selector__search"));
      await userEvent.selectOptions(
        canvasElement.querySelector(".oidf-test-selector__family"),
        "Spec B",
      );
      await waitFor(() => expect(rowPlanNames(canvasElement)).toEqual(["uncertified-only-plan"]));
      await userEvent.click(toggle());
      await waitFor(() =>
        expect(rowPlanNames(canvasElement)).toEqual(["certified-plan", "certified-alpha-plan"]),
      );
      expect(
        /** @type {HTMLSelectElement} */ (
          canvasElement.querySelector(".oidf-test-selector__family")
        ).value,
      ).toBe("");
      expect(localStorage.getItem("cts:test-selector-show-non-certifiable")).toBe("false");
    });
  },
};

/**
 * A selected plan that would be hidden turns "Show non-certifiable plans" on, so a
 * preset or a return to the picker never selects an invisible row.
 */
export const SelectedNonCertifiablePlanIsShown = {
  render: () =>
    html`<cts-test-selector
      .plans=${STATUS_PLANS}
      selected="uncertified-plan"
    ></cts-test-selector>`,
  async play({ canvasElement }) {
    await waitFor(() =>
      expect(
        canvasElement.querySelector('[data-plan-name="uncertified-plan"]').classList,
      ).toContain("is-active"),
    );
    expect(
      /** @type {HTMLInputElement} */ (
        canvasElement.querySelector(".oidf-test-selector__certification-toggle input")
      ).checked,
    ).toBe(true);
    expect(localStorage.getItem("cts:test-selector-show-non-certifiable")).toBeNull();
  },
};

/** The ★ Favorites view lists a favorite even while non-certifiable plans are hidden. */
export const FavoritesListHiddenPlans = {
  render: () =>
    html`<cts-test-selector
      .plans=${STATUS_PLANS}
      .favorites=${["uncertified-plan"]}
    ></cts-test-selector>`,
  async play({ canvasElement }) {
    await userEvent.selectOptions(
      canvasElement.querySelector(".oidf-test-selector__family"),
      "__cts_favorites_view__",
    );
    await waitFor(() => expect(rowPlanNames(canvasElement)).toEqual(["uncertified-plan"]));
    expect(
      /** @type {HTMLInputElement} */ (
        canvasElement.querySelector(".oidf-test-selector__certification-toggle input")
      ).checked,
    ).toBe(false);
  },
};

/** With no plan outside the certification program there is nothing to hide. */
export const NoCertificationToggleWithoutHiddenPlans = {
  render: () => html`<cts-test-selector .plans=${MOCK_PLANS}></cts-test-selector>`,
  async play({ canvasElement }) {
    await waitFor(() => expect(rowPlanNames(canvasElement)).toHaveLength(MOCK_PLANS.length));
    expect(canvasElement.querySelector(".oidf-test-selector__certification-toggle")).toBeNull();
  },
};
