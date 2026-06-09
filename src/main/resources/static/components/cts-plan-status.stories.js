import { html } from "lit";
import { expect, waitFor, userEvent } from "storybook/test";
import "./cts-plan-status.js";
import { segmentVariant, NOT_RUN_FILTER_VALUE } from "../js/module-status.js";

// One module per palette entry, in plan order. Covers every segmentVariant
// branch: resolved PASSED/FAILED/WARNING/REVIEW, RUNNING, FINISHED+SKIPPED
// (→ skip), a never-run module (no instances → skip), and a has-run-but-
// unresolved module (→ pending).
const PALETTE_MODULES = [
  {
    testModule: "test-passed",
    instances: ["i-pass"],
    status: "FINISHED",
    result: "PASSED",
    _statusResolved: true,
  },
  {
    testModule: "test-failed",
    instances: ["i-fail"],
    status: "FINISHED",
    result: "FAILED",
    _statusResolved: true,
  },
  {
    testModule: "test-warning",
    variant: { client_auth_type: "mtls" },
    instances: ["i-warn"],
    status: "FINISHED",
    result: "WARNING",
    _statusResolved: true,
  },
  {
    testModule: "test-review",
    instances: ["i-rev"],
    status: "FINISHED",
    result: "REVIEW",
    _statusResolved: true,
  },
  {
    testModule: "test-running",
    instances: ["i-run"],
    status: "RUNNING",
    _statusResolved: true,
  },
  {
    testModule: "test-skipped",
    instances: ["i-skip"],
    status: "FINISHED",
    result: "SKIPPED",
    _statusResolved: true,
  },
  // never run → static skip
  { testModule: "test-not-run" },
  // has an instance but status not yet fetched → pending (pulsing)
  { testModule: "test-pending", instances: ["i-pend"] },
];

// Expected segment fill class per module index above.
const EXPECTED_CLASS = [
  "cts-pst-seg--pass",
  "cts-pst-seg--fail",
  "cts-pst-seg--warn",
  "cts-pst-seg--review",
  "cts-pst-seg--running",
  "cts-pst-seg--skip",
  "cts-pst-seg--skip",
  "cts-pst-seg--pending",
];

export default {
  title: "Components/cts-plan-status",
  component: "cts-plan-status",
};

// R1/R2: one segment per module, in order, each carrying the expected palette
// class. Read-only overview mode renders <span role="img"> segments.
export const PaletteOverview = {
  render: () =>
    html`<cts-plan-status mode="overview" .modules=${PALETTE_MODULES}></cts-plan-status>`,

  async play({ canvasElement, step }) {
    const segments = canvasElement.querySelectorAll('[data-testid="plan-status-segment"]');

    await step("renders one segment per module, in plan order", () => {
      expect(segments.length).toBe(PALETTE_MODULES.length);
    });

    await step("each segment carries its palette class (incl. review + running)", () => {
      segments.forEach((seg, i) => {
        expect(seg.classList.contains(EXPECTED_CLASS[i])).toBe(true);
      });
    });

    await step("overview segments are read-only role=img, not buttons", () => {
      expect(canvasElement.querySelector("button.cts-pst-seg")).toBeNull();
      segments.forEach((seg) => expect(seg.getAttribute("role")).toBe("img"));
    });

    await step("each segment carries an accessible name (module + status)", () => {
      expect(segments[0].getAttribute("aria-label")).toBe("test-passed: passed");
      expect(segments[1].getAttribute("aria-label")).toBe("test-failed: failed");
      // Variant is included in the name (R3).
      expect(segments[2].getAttribute("aria-label")).toBe(
        "test-warning (client_auth_type=mtls): warning",
      );
      expect(segments[7].getAttribute("aria-label")).toBe("test-pending: checking status");
    });
  },
};

// segmentVariant() is the shared source of truth (KTD3). Assert its three
// branches directly so the helper's contract is pinned independently of the
// component's class wiring.
export const VariantHelperContract = {
  render: () =>
    html`<cts-plan-status mode="overview" .modules=${PALETTE_MODULES}></cts-plan-status>`,

  async play() {
    // never-run (no instances) → skip
    expect(segmentVariant({ instances: [] })).toBe("skip");
    // has instances but unresolved → pending
    expect(segmentVariant({ instances: ["a"] })).toBe("pending");
    // resolved → statusBadgeVariant result
    expect(
      segmentVariant({
        instances: ["a"],
        status: "FINISHED",
        result: "PASSED",
        _statusResolved: true,
      }),
    ).toBe("pass");
    expect(
      segmentVariant({
        instances: ["a"],
        status: "FINISHED",
        result: "REVIEW",
        _statusResolved: true,
      }),
    ).toBe("review");
  },
};

// R3 / Accessibility: hovering OR keyboard-focusing a segment shows a tooltip
// naming the module and its status. The tooltip is owned by cts-tooltip, which
// appends a `.oidf-tooltip` to <body> on mouseenter/focusin.
export const TooltipOnHoverAndFocus = {
  render: () => html`<cts-plan-status mode="detail" .modules=${PALETTE_MODULES}></cts-plan-status>`,

  async play({ canvasElement, step }) {
    // Detail segments are anchors (deep-link to the row); log segments are buttons.
    const first = canvasElement.querySelector("a.cts-pst-seg");

    await step("hover shows a tooltip naming the module + status", async () => {
      first.dispatchEvent(new MouseEvent("mouseenter"));
      const tip = await waitFor(() => {
        const t = document.body.querySelector(".oidf-tooltip");
        expect(t).toBeTruthy();
        return t;
      });
      expect(tip?.textContent).toContain("test-passed");
      expect(tip?.textContent).toContain("passed");
      first.dispatchEvent(new MouseEvent("mouseleave"));
      await waitFor(() => expect(document.body.querySelector(".oidf-tooltip")).toBeNull());
    });

    await step("keyboard focus also shows the tooltip (not just hover)", async () => {
      first.focus();
      await waitFor(() => {
        const t = document.body.querySelector(".oidf-tooltip");
        expect(t).toBeTruthy();
        expect(t?.textContent).toContain("test-passed");
      });
      first.blur();
      await waitFor(() => expect(document.body.querySelector(".oidf-tooltip")).toBeNull());
    });
  },
};

// R9: detail mode renders the merged count summary + "Filter by result"
// control as a row of count badges (mirrors log-detail's logResultSummary).
// Counts are TOTALS in fixed order; the six result categories are interactive
// toggles, RUNNING/PENDING are display-only.
export const DetailCountBadges = {
  render: () => html`<cts-plan-status mode="detail" .modules=${PALETTE_MODULES}></cts-plan-status>`,

  async play({ canvasElement, step }) {
    const filter = canvasElement.querySelector('[data-testid="plan-status-filter"]');
    const badges = canvasElement.querySelectorAll('[data-testid="plan-status-filter"] cts-badge');

    await step("one count badge per non-zero category, in fixed order", () => {
      expect(filter).toBeTruthy();
      expect(Array.from(badges).map((b) => b.getAttribute("label"))).toEqual([
        "Passed 1",
        "Failed 1",
        "Warning 1",
        "Review 1",
        "Running 1",
        "Checking 1",
        "Skipped 1",
        "Not run 1",
      ]);
    });

    await step("result categories are interactive toggles; transient ones are not", () => {
      const clickable = (label) =>
        Array.from(badges)
          .find((b) => b.getAttribute("label") === label)
          .hasAttribute("clickable");
      expect(clickable("Passed 1")).toBe(true);
      expect(clickable("Not run 1")).toBe(true);
      expect(clickable("Running 1")).toBe(false);
      expect(clickable("Checking 1")).toBe(false);
    });
  },
};

// R9/R4: with a result filter active the badge COUNTS are still totals (never
// the filtered subset); the matching badge presses, the row is .is-filtering,
// and the "Clear filters" affordance appears.
export const BadgeFilterActive = {
  render: () => html`
    <cts-plan-status
      mode="detail"
      .modules=${PALETTE_MODULES}
      .activeResultFilter=${new Set(["FAILED"])}
    ></cts-plan-status>
  `,

  async play({ canvasElement, step }) {
    const filter = canvasElement.querySelector('[data-testid="plan-status-filter"]');
    const failed = canvasElement.querySelector('cts-badge[data-result="FAILED"]');

    await step("counts stay totals; the matching badge is pressed", () => {
      expect(failed.getAttribute("label")).toBe("Failed 1");
      expect(failed.hasAttribute("pressed")).toBe(true);
      expect(filter.classList.contains("is-filtering")).toBe(true);
    });

    await step("the Clear-filters affordance is shown", () => {
      expect(canvasElement.querySelector('[data-testid="plan-status-filter-clear"]')).toBeTruthy();
    });
  },
};

// R9: toggling a count badge emits cts-plan-status-filter with the result
// token; "Clear filters" emits { clear: true }. The page coordinator owns the
// Set — the component only reports the change.
export const BadgeEmitsFilterEvent = {
  render: () => html`
    <cts-plan-status
      mode="detail"
      .modules=${PALETTE_MODULES}
      .activeResultFilter=${new Set(["FAILED"])}
    ></cts-plan-status>
  `,

  async play({ canvasElement, step }) {
    const host = canvasElement.querySelector("cts-plan-status");
    const events = [];
    host.addEventListener("cts-plan-status-filter", (e) => events.push(e.detail));

    await step("clicking a count badge emits its result token", () => {
      canvasElement.querySelector('cts-badge[data-result="WARNING"] .badge').click();
      expect(events.at(-1)).toEqual({ value: "WARNING" });
    });

    await step("Clear filters emits { clear: true }", () => {
      canvasElement.querySelector('[data-testid="plan-status-filter-clear"]').click();
      expect(events.at(-1)).toEqual({ clear: true });
    });
  },
};

// R6/R7/AE3 — wide container: a single-row bar whose segments shrink to
// hairlines (flex:1 1 0; min-width:0). Pinned to an 800px host so the
// @container branch does NOT trip.
export const ResponsiveBarWide = {
  render: () => html`
    <div style="width: 800px;">
      <cts-plan-status mode="detail" .modules=${PALETTE_MODULES}></cts-plan-status>
    </div>
  `,

  async play({ canvasElement, step }) {
    const segments = canvasElement.querySelectorAll('[data-testid="plan-status-segment"]');

    await step("segments are flexible hairlines (min-width:0)", () => {
      const cs = getComputedStyle(segments[0]);
      expect(cs.minWidth).toBe("0px");
      expect(cs.flexGrow).toBe("1");
    });

    await step("all segments sit on a single row (no wrap)", () => {
      const top = segments[0].offsetTop;
      segments.forEach((seg) => expect(seg.offsetTop).toBe(top));
    });
  },
};

// R6/R7/AE3 — narrow container: the same component wraps into a grid of
// tappable rectangles (min-width:40px; flex-wrap). Pinned to a 300px host so
// the @container (max-width:520px) branch trips.
export const ResponsiveGridNarrow = {
  render: () => html`
    <div style="width: 300px;">
      <cts-plan-status mode="detail" .modules=${PALETTE_MODULES}></cts-plan-status>
    </div>
  `,

  async play({ canvasElement, step }) {
    const segments = canvasElement.querySelectorAll('[data-testid="plan-status-segment"]');

    await step("segments gain a tappable min-width", () => {
      expect(getComputedStyle(segments[0]).minWidth).toBe("40px");
    });

    await step("the row wraps (segments occupy more than one row)", () => {
      const tops = new Set(Array.from(segments).map((s) => s.offsetTop));
      expect(tops.size).toBeGreaterThan(1);
    });
  },
};

// R10/R18: with a FAILED filter active, the failed segment keeps full colour
// while every non-matching segment — including the still-pending one (R18) —
// gets is-dimmed. The class is applied in render(), so it is identical in the
// bar and grid layouts.
export const FilterDimsNonMatching = {
  render: () => html`
    <cts-plan-status
      mode="detail"
      .modules=${PALETTE_MODULES}
      .activeResultFilter=${new Set(["FAILED"])}
    ></cts-plan-status>
  `,

  async play({ canvasElement, step }) {
    const segments = canvasElement.querySelectorAll('[data-testid="plan-status-segment"]');

    await step("the matching (failed) segment keeps full colour", () => {
      const failed = segments[1];
      expect(failed.classList.contains("cts-pst-seg--fail")).toBe(true);
      expect(failed.classList.contains("is-dimmed")).toBe(false);
    });

    await step("non-matching segments dim", () => {
      expect(segments[0].classList.contains("is-dimmed")).toBe(true); // passed
      expect(segments[3].classList.contains("is-dimmed")).toBe(true); // review
    });

    await step("a still-pending segment is treated as non-matching (R18)", () => {
      const pending = segments[7];
      expect(pending.classList.contains("cts-pst-seg--pending")).toBe(true);
      expect(pending.classList.contains("is-dimmed")).toBe(true);
    });

    await step("a never-run segment matches the 'Not yet run' filter, not FAILED", () => {
      // Sanity-check the shared matcher the dimming relies on.
      expect(segments[6].classList.contains("is-dimmed")).toBe(true);
    });
  },
};

// R9/U5 boundary, exercised through the dimming path: the "Not yet run" filter
// selects a never-run module but NOT a FINISHED+SKIPPED one (raw status/result,
// not the collapsed skip variant).
export const NotYetRunFilterSkipsFinishedSkipped = {
  render: () => html`
    <cts-plan-status
      mode="detail"
      .modules=${PALETTE_MODULES}
      .activeResultFilter=${new Set([NOT_RUN_FILTER_VALUE])}
    ></cts-plan-status>
  `,

  async play({ canvasElement, step }) {
    const segments = canvasElement.querySelectorAll('[data-testid="plan-status-segment"]');

    await step("the never-run segment matches (not dimmed)", () => {
      expect(segments[6].classList.contains("is-dimmed")).toBe(false);
    });

    await step("the FINISHED+SKIPPED segment does NOT match (dimmed)", () => {
      expect(segments[5].classList.contains("cts-pst-seg--skip")).toBe(true);
      expect(segments[5].classList.contains("is-dimmed")).toBe(true);
    });
  },
};

// Modules with a re-run history, for the log "you are here" marker.
const LOG_MODULES = [
  {
    testModule: "log-m1",
    instances: ["la"],
    status: "FINISHED",
    result: "PASSED",
    _statusResolved: true,
  },
  // viewed instance "lb1" is the FIRST of two — an older re-run.
  {
    testModule: "log-m2",
    instances: ["lb1", "lb2"],
    status: "FINISHED",
    result: "FAILED",
    _statusResolved: true,
  },
  {
    testModule: "log-m3",
    instances: ["lc"],
    status: "FINISHED",
    result: "PASSED",
    _statusResolved: true,
  },
];

// R14/R17: viewing an OLDER re-run (instance not the module's last) still marks
// the correct segment, the marker overlays (does not replace) the status fill,
// and the position label reads "Module N of M".
export const LogCurrentMarker = {
  render: () => html`
    <cts-plan-status mode="log" current-instance-id="lb1" .modules=${LOG_MODULES}></cts-plan-status>
  `,

  async play({ canvasElement, step }) {
    const segments = canvasElement.querySelectorAll('[data-testid="plan-status-segment"]');

    await step("the marker lands on the module that ran the viewed instance (R17)", () => {
      expect(segments[1].classList.contains("is-current")).toBe(true);
      expect(segments[0].classList.contains("is-current")).toBe(false);
      expect(segments[2].classList.contains("is-current")).toBe(false);
    });

    await step("the marker overlays — it does not replace — the status fill", () => {
      const current = segments[1];
      expect(current.classList.contains("cts-pst-seg--fail")).toBe(true);
      // The 2px inset ring is a box-shadow on top of the fill.
      expect(getComputedStyle(current).boxShadow).not.toBe("none");
    });

    await step("the position label reads Module N of M (R14)", () => {
      const position = canvasElement.querySelector('[data-testid="plan-status-position"]');
      expect(position.textContent.trim()).toBe("Module 2 of 3");
    });
  },
};

// Decision 1 (U6): a readonly log surface (public / readonly view) renders
// NON-interactive segments — <span role="img">, no <button>, no activate
// emission — while the "you are here" marker and the "Module N of M" label
// still render. Used by cts-test-nav-controls on the public log view so
// sibling navigation is suppressed (a UX affordance; backend /api/info
// gating remains the real access boundary).
export const LogReadonlyNonNavigating = {
  render: () => html`
    <cts-plan-status
      mode="log"
      current-instance-id="lb1"
      .modules=${LOG_MODULES}
      readonly
    ></cts-plan-status>
  `,

  async play({ canvasElement, step }) {
    await step("no segment is a button (non-navigating)", () => {
      expect(canvasElement.querySelector("button.cts-pst-seg")).toBeNull();
      const spans = canvasElement.querySelectorAll("span.cts-pst-seg");
      expect(spans.length).toBe(LOG_MODULES.length);
      spans.forEach((seg) => expect(seg.getAttribute("role")).toBe("img"));
    });

    await step("activating a segment emits no event", () => {
      const events = [];
      canvasElement.addEventListener("cts-plan-status-activate", (e) => events.push(e));
      const span = canvasElement.querySelector("span.cts-pst-seg");
      span.click();
      expect(events.length).toBe(0);
    });

    await step("the 'you are here' marker still renders on the current segment", () => {
      const segments = canvasElement.querySelectorAll('[data-testid="plan-status-segment"]');
      expect(segments[1].classList.contains("is-current")).toBe(true);
      expect(segments[0].classList.contains("is-current")).toBe(false);
    });

    await step("the position label still shows", () => {
      const position = canvasElement.querySelector('[data-testid="plan-status-position"]');
      expect(position.textContent.trim()).toBe("Module 2 of 3");
    });
  },
};

// hide-label: a host that wants to place the "Module N of M" label itself
// (cts-test-nav-controls, so the bar and the Continue button stay centred as
// siblings with the label on its own row below) sets hide-label to suppress the
// built-in label. The bar, the "you are here" marker, and segment interactivity
// are unaffected — only the label is withheld.
export const LogHideLabel = {
  render: () => html`
    <cts-plan-status
      mode="log"
      current-instance-id="lb1"
      .modules=${LOG_MODULES}
      hide-label
    ></cts-plan-status>
  `,

  async play({ canvasElement, step }) {
    await step("the built-in position label is suppressed", () => {
      expect(canvasElement.querySelector('[data-testid="plan-status-position"]')).toBeNull();
    });

    await step("the bar and the 'you are here' marker still render", () => {
      const segments = canvasElement.querySelectorAll('[data-testid="plan-status-segment"]');
      expect(segments.length).toBe(LOG_MODULES.length);
      expect(segments[1].classList.contains("is-current")).toBe(true);
    });
  },
};

// R15/R16: in log mode segments are buttons that emit cts-plan-status-activate
// (with index, module, and the module's most-recent instance) on click AND on
// keyboard activation.
export const LogSegmentActivates = {
  render: () => html`
    <cts-plan-status mode="log" current-instance-id="lb1" .modules=${LOG_MODULES}></cts-plan-status>
  `,

  async play({ canvasElement, step }) {
    const events = [];
    canvasElement.addEventListener("cts-plan-status-activate", (e) => events.push(e.detail));
    const segments = canvasElement.querySelectorAll("button.cts-pst-seg");

    await step("clicking a segment emits with the module's most-recent instance", () => {
      segments[1].click();
      expect(events.length).toBe(1);
      expect(events[0].index).toBe(1);
      expect(events[0].module.testModule).toBe("log-m2");
      // R17: the event carries the module's LAST instance (lb2), not the viewed lb1.
      expect(events[0].instanceId).toBe("lb2");
    });

    await step("Enter and Space activate the focused segment", async () => {
      segments[0].focus();
      await userEvent.keyboard("{Enter}");
      expect(events.length).toBe(2);
      expect(events[1].index).toBe(0);
      await userEvent.keyboard(" ");
      expect(events.length).toBe(3);
    });
  },
};

// R11 support: the activate event carries `dimmed` (whether an active result
// filter dims the clicked segment), so a coordinator can clear-then-scroll
// without re-deriving the match.
export const DetailActivateCarriesDimmed = {
  render: () => html`
    <cts-plan-status
      mode="detail"
      .modules=${PALETTE_MODULES}
      .activeResultFilter=${new Set(["FAILED"])}
    ></cts-plan-status>
  `,

  async play({ canvasElement, step }) {
    const events = [];
    canvasElement.addEventListener("cts-plan-status-activate", (e) => events.push(e.detail));
    // Detail segments are in-page anchors; swallow the default so a click does
    // not pollute location.hash for later stories (the activate event still
    // fires from _onActivate before the default would run).
    canvasElement.addEventListener("click", (e) => e.preventDefault());
    const segments = canvasElement.querySelectorAll("a.cts-pst-seg");

    await step("a matching (failed) segment activates with dimmed=false", () => {
      segments[1].click(); // test-failed matches the FAILED filter
      expect(events.at(-1).index).toBe(1);
      expect(events.at(-1).dimmed).toBe(false);
    });

    await step("a non-matching (passed) segment activates with dimmed=true", () => {
      segments[0].click(); // test-passed is dimmed under the FAILED filter
      expect(events.at(-1).index).toBe(0);
      expect(events.at(-1).dimmed).toBe(true);
    });
  },
};

// R16: overview segments are NOT interactive — a click emits no activate event
// and the card (not the segment) remains the click target.
export const OverviewSegmentsInert = {
  render: () => html`<cts-plan-status mode="overview" .modules=${LOG_MODULES}></cts-plan-status>`,

  async play({ canvasElement }) {
    const events = [];
    canvasElement.addEventListener("cts-plan-status-activate", (e) => events.push(e));
    expect(canvasElement.querySelector("button.cts-pst-seg")).toBeNull();
    const span = canvasElement.querySelector("span.cts-pst-seg");
    span.click();
    expect(events.length).toBe(0);
  },
};

// Edge state: a zero-module plan renders nothing (hides itself).
export const EmptyRendersNothing = {
  render: () => html`<cts-plan-status mode="detail" .modules=${[]}></cts-plan-status>`,

  async play({ canvasElement }) {
    expect(canvasElement.querySelector('[data-testid="plan-status-track"]')).toBeNull();
    expect(canvasElement.querySelector('[data-testid="plan-status-filter"]')).toBeNull();
  },
};

// Edge state: a single-module plan renders one full-width segment plus the
// count-badge filter.
export const SingleModule = {
  render: () => html`
    <div style="width: 600px;">
      <cts-plan-status
        mode="detail"
        .modules=${[
          {
            testModule: "only-one",
            instances: ["i-only"],
            status: "FINISHED",
            result: "PASSED",
            _statusResolved: true,
          },
        ]}
      ></cts-plan-status>
    </div>
  `,

  async play({ canvasElement, step }) {
    const segments = canvasElement.querySelectorAll('[data-testid="plan-status-segment"]');

    await step("exactly one segment renders", () => {
      expect(segments.length).toBe(1);
      expect(segments[0].classList.contains("cts-pst-seg--pass")).toBe(true);
    });

    await step("the single bar segment fills the width", () => {
      // flex:1 1 0 with one item → fills the track.
      expect(segments[0].getBoundingClientRect().width).toBeGreaterThan(400);
    });

    await step("the count-badge filter still renders", () => {
      const filter = canvasElement.querySelector('[data-testid="plan-status-filter"]');
      expect(filter.textContent).toContain("Passed 1");
    });
  },
};
