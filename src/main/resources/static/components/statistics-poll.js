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
 * No DOM, no fetch, no Lit: it is a timer with a schedule, unit-tested in the
 * vitest `unit` project (`statistics-poll.test.js`).
 */

/**
 * Poll cadence while the server is computing a snapshot. Fast for the first
 * half-minute (a warm database answers in seconds and the admin is watching a
 * spinner), then slower, because past that point this is a multi-minute
 * whole-collection aggregation and there is no point hammering it.
 */
export const POLL_FAST_MS = 2000;
export const POLL_SLOW_MS = 5000;
export const POLL_FAST_WINDOW_MS = 30000;
/** Stop polling and offer a Retry rather than spinning forever. */
export const POLL_GIVE_UP_MS = 600000;

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
  }

  /**
   * Schedule the next poll, or give up.
   * @returns {void}
   */
  schedule() {
    this._clear();
    if (this._startedAt === null) this._startedAt = Date.now();
    const elapsed = Date.now() - this._startedAt;
    if (elapsed >= POLL_GIVE_UP_MS) {
      // The consumer's give-up handler is expected to stop() this poll (it is
      // a terminal state), so nothing is scheduled here afterwards.
      this._onGiveUp();
      return;
    }
    const delay = elapsed < POLL_FAST_WINDOW_MS ? POLL_FAST_MS : POLL_SLOW_MS;
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
