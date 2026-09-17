package net.openid.conformance.statistics;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The body of {@code GET /api/statistics/overview}: one shape per state the snapshot cache
 * can be in, plus the one for a request that could not be answered at all. Every field is
 * a JSON primitive or the payload itself, so the client never has to parse a Java-specific
 * rendering of a date or a duration.
 */
public sealed interface StatisticsResponse permits StatisticsResponse.Ready, StatisticsResponse.Pending, StatisticsResponse.Failed, StatisticsResponse.Invalid {

	/**
	 * A snapshot is being served (HTTP 200), possibly while a newer one is computed.
	 *
	 * @param status            always {@code "ready"}
	 * @param computedAt        when the snapshot being served was computed, ISO-8601 UTC
	 * @param computeDurationMs how long computing it took, in milliseconds
	 * @param refreshing        true if a newer snapshot is being computed right now
	 * @param lastError         the failure of the most recent computation, or null if it
	 *                          succeeded; a snapshot is served either way
	 * @param data              the snapshot
	 */
	@Schema(name = "StatisticsReady")
	record Ready(String status, String computedAt, long computeDurationMs, boolean refreshing, LastError lastError, StatisticsOverview data) implements StatisticsResponse {
		Ready(String computedAt, long computeDurationMs, boolean refreshing, LastError lastError, StatisticsOverview data) {
			this("ready", computedAt, computeDurationMs, refreshing, lastError, data);
		}
	}

	/**
	 * No snapshot yet and the first computation is running (HTTP 202); the client should
	 * retry after the {@code Retry-After} interval.
	 *
	 * @param status    always {@code "pending"}
	 * @param startedAt when the running computation started, ISO-8601 UTC
	 */
	@Schema(name = "StatisticsPending")
	record Pending(String status, String startedAt) implements StatisticsResponse {
		Pending(String startedAt) {
			this("pending", startedAt);
		}
	}

	/**
	 * There is no snapshot to serve and the last computation failed (HTTP 500).
	 *
	 * @param status   always {@code "error"}
	 * @param message  what went wrong
	 * @param failedAt when the computation failed, ISO-8601 UTC
	 */
	@Schema(name = "StatisticsFailed")
	record Failed(String status, String message, String failedAt) implements StatisticsResponse {
		Failed(String message, String failedAt) {
			this("error", message, failedAt);
		}
	}

	/**
	 * The request asked for something that cannot be shown (HTTP 400) - an unknown
	 * granularity, a malformed period, a range that runs backwards. Nothing was computed;
	 * correcting the parameter and asking again is all that is needed.
	 *
	 * @param status  always {@code "invalid"}, so that a client switching on {@code status}
	 *                can tell a request it can fix from the {@code "error"} of a failed
	 *                computation, which it cannot
	 * @param message which parameter could not be used and why, meant to be shown as is
	 */
	@Schema(name = "StatisticsInvalid")
	record Invalid(String status, String message) implements StatisticsResponse {
		Invalid(String message) {
			this("invalid", message);
		}
	}

	/**
	 * A failed recomputation reported alongside an older, still-served snapshot.
	 *
	 * @param message  what went wrong
	 * @param failedAt when the computation failed, ISO-8601 UTC
	 */
	@Schema(name = "StatisticsLastError")
	record LastError(String message, String failedAt) {
	}
}
