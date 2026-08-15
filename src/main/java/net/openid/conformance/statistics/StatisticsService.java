package net.openid.conformance.statistics;

import net.openid.conformance.statistics.AsyncSnapshotCache.State;

/**
 * Serves the cached suite-wide statistics snapshot behind {@code GET /api/statistics/overview}.
 */
// Not @FunctionalInterface: this is the service abstraction the controller depends on,
// not a callback meant to be passed as a lambda.
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface StatisticsService {

	/**
	 * Never computes anything on the calling thread: if no snapshot is available yet the
	 * caller gets {@link AsyncSnapshotCache.Pending} and is expected to poll.
	 *
	 * @param refresh start a recomputation even if the current snapshot is still fresh
	 * @return the current state of the snapshot
	 */
	State<StatisticsOverview> getOverview(boolean refresh);
}
