/**
 * The poll cadence the statistics endpoint is read with.
 *
 * `GET /api/statistics/overview` serves a cached snapshot recomputed in the
 * background, which answers `202 pending` while it is being computed and
 * `refreshing: true` while a newer one is on the way — so the page has to
 * poll, and has to give up rather than spin forever. The timing is all this
 * module carries (what to fetch, what to do with the body and what to say
 * while waiting stay in the component), so that a second snapshot-backed
 * section can share the cadence without sharing anything else.
 *
 * A hidden tab does not poll: the next request waits until the tab is shown
 * again and is made at once then, with the budget still measured from the
 * start of the episode. Nobody is watching a spinner in a background tab, and
 * a multi-minute recompute is not made any quicker by being asked about.
 *
 * No fetch, no Lit, and the only DOM is `document.visibilityState`, absent
 * where there is no document: a timer with a schedule, unit-tested in the
 * vitest `unit` project (`statistics-poll.test.js`).
 */

/**
 * Poll cadence while the server is computing a snapshot. Fast for the first
 * half-minute (a warm database answers in seconds and the admin is watching a
 * spinner), slower for the next few minutes, and slower again past that:
 * a production sized snapshot is a ten to twenty minute whole-collection
 * aggregation, a `202` costs the server nothing but a poll against a live
 * snapshot slices the cube each time, and nobody is watching a spinner that
 * closely by then.
 */
export const POLL_FAST_MS = 2000;
export const POLL_SLOW_MS = 5000;
export const POLL_SLOWEST_MS = 30000;
export const POLL_FAST_WINDOW_MS = 30000;
export const POLL_SLOW_WINDOW_MS = 300000;
/**
 * Stop polling and offer a Retry rather than spinning forever. Sized from a
 * measured recompute of a 7 million run database (13 minutes) with room for
 * a slower box.
 */
export const POLL_GIVE_UP_MS = 1800000;
/** The budget, as the give-up message states it. */
export const POLL_GIVE_UP_MINUTES = POLL_GIVE_UP_MS / 60000;

/**
 * @param {number} elapsed - Milliseconds since the episode began.
 * @returns {number} How long to wait before the next poll.
 */
function delayAfter(elapsed) {
  if (elapsed < POLL_FAST_WINDOW_MS) return POLL_FAST_MS;
  if (elapsed < POLL_SLOW_WINDOW_MS) return POLL_SLOW_MS;
  return POLL_SLOWEST_MS;
}

/** @returns {boolean} True when there is a document and it is hidden. */
function isHidden() {
  return typeof document !== "undefined" && document.visibilityState === "hidden";
}

/**
 * A polling episode: a timer plus the moment the episode began.
 *
 * The cadence is measured from the START of the episode, not from the last
 * request, so a slow response cannot stretch the fast window and a request
 * that takes longer than the budget still ends the episode.
 *
 * An episode begins at the first {@link SnapshotPoll#schedule} after a
 * {@link SnapshotPoll#stop} (or after construction), and ends at the next
 * `stop()` — which every terminal path of a consumer already calls: a
 * forbidden response, a failure, a manual Refresh, a filter change and
 * disconnect.
 */
export class SnapshotPoll {
  /**
   * @param {() => void} onPoll - Make the next request. Called on the timer.
   * @param {() => void} onGiveUp - The budget is spent; no further request
   *   will be made until the consumer starts a new episode. Called instead of
   *   scheduling, on the same stack as the `schedule()` that noticed.
   */
  constructor(onPoll, onGiveUp) {
    /** @type {() => void} */
    this._onPoll = onPoll;
    /** @type {() => void} */
    this._onGiveUp = onGiveUp;
    /** @type {ReturnType<typeof setTimeout>|null} */
    this._timer = null;
    /** @type {number|null} When the current polling episode began. */
    this._startedAt = null;
    /** @type {boolean} A poll is due but waiting for the tab to be shown. */
    this._paused = false;
    /** @type {() => void} Bound once, so it can be removed again. */
    this._onVisibility = () => this._handleVisibility();
  }

  /**
   * Schedule the next poll, or give up.
   * @returns {void}
   */
  schedule() {
    this._clear();
    this._paused = false;
    if (this._startedAt === null) {
      this._startedAt = Date.now();
      this._listen(true);
    }
    const elapsed = Date.now() - this._startedAt;
    if (elapsed >= POLL_GIVE_UP_MS) {
      // The consumer's give-up handler is expected to stop() this poll (it is
      // a terminal state), so nothing is scheduled here afterwards.
      this._onGiveUp();
      return;
    }
    if (isHidden()) {
      // Wait for the tab rather than the clock: see _handleVisibility.
      this._paused = true;
      return;
    }
    const delay = delayAfter(elapsed);
    this._timer = setTimeout(() => {
      this._timer = null;
      this._onPoll();
    }, delay);
  }

  /**
   * End the episode: cancel any pending poll and reset the budget, so the
   * next `schedule()` starts counting again.
   * @returns {void}
   */
  stop() {
    this._clear();
    this._startedAt = null;
    this._paused = false;
    this._listen(false);
  }

  /**
   * A tab going hidden cancels the pending poll and remembers that one was
   * due; a tab coming back polls at once (or gives up, if the budget ran out
   * meanwhile) rather than waiting out a delay that was meant for a reader
   * watching the page.
   * @returns {void}
   */
  _handleVisibility() {
    if (isHidden()) {
      if (this._timer === null) return;
      this._clear();
      this._paused = true;
      return;
    }
    if (!this._paused || this._startedAt === null) return;
    this._paused = false;
    if (Date.now() - this._startedAt >= POLL_GIVE_UP_MS) {
      this._onGiveUp();
    } else {
      this._onPoll();
    }
  }

  /**
   * Listen for the tab being hidden or shown for the length of one episode,
   * so a poll that is stopped for good holds no listener.
   * @param {boolean} on - Whether to listen.
   * @returns {void}
   */
  _listen(on) {
    if (typeof document === "undefined") return;
    if (on) document.addEventListener("visibilitychange", this._onVisibility);
    else document.removeEventListener("visibilitychange", this._onVisibility);
  }

  /**
   * @returns {void}
   */
  _clear() {
    if (this._timer === null) return;
    clearTimeout(this._timer);
    this._timer = null;
  }
}
