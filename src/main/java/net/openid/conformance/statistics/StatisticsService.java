package net.openid.conformance.statistics;

import net.openid.conformance.statistics.AsyncSnapshotCache.State;

/**
 * Serves the cached suite-wide statistics cube behind {@code GET /api/statistics/overview}.
 * What the client asked to see is applied to the cube by {@link StatisticsSlicer} on the
 * request thread, so every filter and range is answered from the same cached computation.
 */
// Not @FunctionalInterface: this is the service abstraction the controller depends on,
// not a callback meant to be passed as a lambda.
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface StatisticsService {

	/**
	 * Never computes anything on the calling thread: if no cube is available yet the
	 * caller gets {@link AsyncSnapshotCache.Pending} and is expected to poll.
	 *
	 * @param refresh start a recomputation even if the current cube is still fresh
	 * @return the current state of the cube
	 */
	State<StatisticsCube> getCube(boolean refresh);
}
