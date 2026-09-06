import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  POLL_FAST_MS,
  POLL_FAST_WINDOW_MS,
  POLL_GIVE_UP_MINUTES,
  POLL_GIVE_UP_MS,
  POLL_SLOWEST_MS,
  POLL_SLOW_MS,
  POLL_SLOW_WINDOW_MS,
  SnapshotPoll,
} from "./statistics-poll.js";

/**
 * A poll and the two callbacks it was built with, so a test can assert on
 * both without keeping three variables in scope.
 * @returns {{poll: SnapshotPoll, polled: any, gaveUp: any}} The fixture.
 */
function makePoll() {
  const polled = vi.fn();
  const gaveUp = vi.fn();
  const poll = new SnapshotPoll(polled, gaveUp);
  return { poll, polled, gaveUp };
}

describe("SnapshotPoll", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("polls on the fast cadence for the first half-minute", () => {
    const { poll, polled } = makePoll();
    poll.schedule();
    vi.advanceTimersByTime(POLL_FAST_MS - 1);
    expect(polled).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(polled).toHaveBeenCalledTimes(1);
  });

  it("slows down once the fast window is spent, measured from the episode start", () => {
    const { poll, polled } = makePoll();
    poll.schedule();
    // A slow response must not stretch the fast window: the cadence is
    // measured from the start of the episode, not from the last request.
    vi.advanceTimersByTime(POLL_FAST_WINDOW_MS);
    poll.schedule();
    vi.advanceTimersByTime(POLL_FAST_MS);
    expect(polled).toHaveBeenCalledTimes(1); // the first, fast one
    vi.advanceTimersByTime(POLL_SLOW_MS - POLL_FAST_MS);
    expect(polled).toHaveBeenCalledTimes(2);
  });

  it("slows down again once the slow window is spent", () => {
    const { poll, polled } = makePoll();
    poll.schedule();
    vi.advanceTimersByTime(POLL_SLOW_WINDOW_MS);
    poll.schedule();
    vi.advanceTimersByTime(POLL_SLOW_MS);
    expect(polled).toHaveBeenCalledTimes(1); // the first, fast one
    vi.advanceTimersByTime(POLL_SLOWEST_MS - POLL_SLOW_MS);
    expect(polled).toHaveBeenCalledTimes(2);
  });

  it("budgets for a production sized recompute and says so in whole minutes", () => {
    // Measured: 13 minutes for a 7 million run database on a laptop-class box.
    expect(POLL_GIVE_UP_MS).toBeGreaterThanOrEqual(20 * 60000);
    expect(POLL_GIVE_UP_MINUTES).toBe(Math.round(POLL_GIVE_UP_MINUTES));
  });

  it("gives up instead of scheduling once the budget is spent", () => {
    const { poll, polled, gaveUp } = makePoll();
    poll.schedule();
    vi.advanceTimersByTime(POLL_GIVE_UP_MS);
    polled.mockClear();
    poll.schedule();
    expect(gaveUp).toHaveBeenCalledTimes(1);
    // Nothing was scheduled, so no amount of waiting produces another request.
    vi.advanceTimersByTime(POLL_GIVE_UP_MS);
    expect(polled).not.toHaveBeenCalled();
  });

  it("stop() cancels the pending poll", () => {
    const { poll, polled } = makePoll();
    poll.schedule();
    poll.stop();
    vi.advanceTimersByTime(POLL_GIVE_UP_MS);
    expect(polled).not.toHaveBeenCalled();
  });

  it("stop() resets the budget, so a new episode starts fast again", () => {
    const { poll, polled, gaveUp } = makePoll();
    poll.schedule();
    vi.advanceTimersByTime(POLL_GIVE_UP_MS);
    poll.stop();
    poll.schedule();
    expect(gaveUp).not.toHaveBeenCalled();
    vi.advanceTimersByTime(POLL_FAST_MS);
    // Two: the timer armed before the budget was spent fired as well.
    expect(polled).toHaveBeenCalledTimes(2);
  });

  it("scheduling twice arms one timer, not two", () => {
    const { poll, polled } = makePoll();
    poll.schedule();
    poll.schedule();
    vi.advanceTimersByTime(POLL_FAST_MS);
    expect(polled).toHaveBeenCalledTimes(1);
  });
});

describe("SnapshotPoll in a tab that can be hidden", () => {
  /**
   * A stand-in for `document`: the visibility state, and the one listener
   * the poll registers, which the tests fire by hand.
   * @type {{visibilityState: string, listeners: Array<() => void>,
   *   addEventListener: (type: string, fn: () => void) => void,
   *   removeEventListener: (type: string, fn: () => void) => void}}
   */
  const doc = {
    visibilityState: "visible",
    listeners: [],
    addEventListener(type, fn) {
      if (type === "visibilitychange") this.listeners.push(fn);
    },
    removeEventListener(type, fn) {
      this.listeners = this.listeners.filter((listener) => listener !== fn);
    },
  };

  /**
   * @param {string} state - `"hidden"` or `"visible"`.
   * @returns {void}
   */
  function show(state) {
    doc.visibilityState = state;
    for (const listener of [...doc.listeners]) listener();
  }

  beforeEach(() => {
    vi.useFakeTimers();
    doc.visibilityState = "visible";
    doc.listeners = [];
    vi.stubGlobal("document", doc);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it("holds a pending poll while the tab is hidden and makes it as soon as it is shown", () => {
    const { poll, polled } = makePoll();
    poll.schedule();
    show("hidden");
    vi.advanceTimersByTime(POLL_GIVE_UP_MS / 2);
    // Nobody is watching: no request while hidden, however long.
    expect(polled).not.toHaveBeenCalled();
    show("visible");
    // ...and the reader coming back is not made to wait out a delay.
    expect(polled).toHaveBeenCalledTimes(1);
  });

  it("does not arm a timer for a schedule() made while hidden", () => {
    const { poll, polled } = makePoll();
    doc.visibilityState = "hidden";
    poll.schedule();
    vi.advanceTimersByTime(POLL_SLOW_MS * 2);
    expect(polled).not.toHaveBeenCalled();
    show("visible");
    expect(polled).toHaveBeenCalledTimes(1);
  });

  it("gives up on return if the budget ran out while hidden", () => {
    const { poll, polled, gaveUp } = makePoll();
    poll.schedule();
    show("hidden");
    vi.advanceTimersByTime(POLL_GIVE_UP_MS);
    show("visible");
    expect(polled).not.toHaveBeenCalled();
    expect(gaveUp).toHaveBeenCalledTimes(1);
  });

  it("ignores visibility changes when nothing is pending", () => {
    const { poll, polled } = makePoll();
    show("hidden");
    show("visible");
    expect(polled).not.toHaveBeenCalled();
    // A poll that has already fired has nothing to resume either.
    poll.schedule();
    vi.advanceTimersByTime(POLL_FAST_MS);
    expect(polled).toHaveBeenCalledTimes(1);
    show("hidden");
    show("visible");
    expect(polled).toHaveBeenCalledTimes(1);
  });

  it("listens only for the length of an episode", () => {
    const { poll } = makePoll();
    expect(doc.listeners).toHaveLength(0);
    poll.schedule();
    expect(doc.listeners).toHaveLength(1);
    poll.schedule();
    expect(doc.listeners).toHaveLength(1);
    poll.stop();
    expect(doc.listeners).toHaveLength(0);
  });
});
