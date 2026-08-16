import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  POLL_FAST_MS,
  POLL_FAST_WINDOW_MS,
  POLL_GIVE_UP_MS,
  POLL_SLOW_MS,
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
