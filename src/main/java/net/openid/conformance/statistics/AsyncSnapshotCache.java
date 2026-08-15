package net.openid.conformance.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Holds one expensive-to-compute snapshot and recomputes it in the background.
 *
 * <p>Stale-while-revalidate: once a snapshot exists it is always served, even while a
 * newer one is being computed and even if the last recompute failed. Only one computation
 * is ever in flight, so any number of polling callers cause at most one computation.
 *
 * <p>A computation is started by {@link #get(boolean)} when none is running and either the
 * caller forced it, or there is no snapshot yet, or the snapshot is older than the TTL -
 * and, unless forced, only if the last failure is older than the failure backoff, so a
 * broken data source is not hammered by pollers.
 *
 * @param <T> the snapshot type
 */
public class AsyncSnapshotCache<T> {

	private static final Logger logger = LoggerFactory.getLogger(AsyncSnapshotCache.class);

	private final Supplier<T> computation;

	private final Executor executor;

	private final Clock clock;

	private final Duration ttl;

	private final Duration failureBackoff;

	private T value;

	private Instant computedAt;

	private Duration computeDuration;

	private Failure lastFailure;

	private CompletableFuture<Void> inFlight;

	private Instant inFlightStartedAt;

	/**
	 * @param computation    produces a snapshot; runs on {@code executor}, may throw
	 * @param executor       where computations run; a single thread is enough
	 * @param clock          the clock all timestamps and ages are measured against
	 * @param ttl            how long a snapshot is served before a refresh is triggered
	 * @param failureBackoff how long after a failed computation before another is tried
	 */
	public AsyncSnapshotCache(Supplier<T> computation, Executor executor, Clock clock, Duration ttl, Duration failureBackoff) {
		this.computation = computation;
		this.executor = executor;
		this.clock = clock;
		this.ttl = ttl;
		this.failureBackoff = failureBackoff;
	}

	/**
	 * @param force recompute even if the snapshot is fresh and even if the failure backoff
	 *              has not expired; never starts a second computation while one is running
	 * @return {@link Ready} whenever a snapshot exists, otherwise {@link Pending} while a
	 *         computation is running, otherwise {@link Failed}
	 */
	public synchronized State<T> get(boolean force) {
		boolean running = inFlight != null && !inFlight.isDone();
		Instant now = clock.instant();
		if (!running && shouldStart(force, now)) {
			inFlightStartedAt = now;
			inFlight = CompletableFuture.runAsync(this::runOnce, executor);
			running = true;
		}
		if (value != null) {
			return new Ready<>(value, computedAt, computeDuration, running, lastFailure);
		}
		if (running) {
			return new Pending<>(inFlightStartedAt);
		}
		return new Failed<>(lastFailure);
	}

	/** Called from {@link #get(boolean)} only, so already under the lock. */
	private boolean shouldStart(boolean force, Instant now) {
		boolean stale = value == null || Duration.between(computedAt, now).compareTo(ttl) > 0;
		boolean backoffExpired = lastFailure == null
			|| Duration.between(lastFailure.failedAt(), now).compareTo(failureBackoff) > 0;
		return (force || stale) && (force || backoffExpired);
	}

	private void runOnce() {
		Instant startedAt = clock.instant();
		try {
			T computed = computation.get();
			Instant finishedAt = clock.instant();
			synchronized (this) {
				value = computed;
				computedAt = finishedAt;
				computeDuration = Duration.between(startedAt, finishedAt);
				lastFailure = null;
			}
		} catch (Exception e) {
			logger.error("Snapshot computation failed", e);
			synchronized (this) {
				lastFailure = new Failure(e.getMessage() == null ? e.toString() : e.getMessage(), clock.instant());
			}
		}
	}

	/** What the cache can currently serve. */
	public sealed interface State<T> permits Ready, Pending, Failed {
	}

	/**
	 * A snapshot is available.
	 *
	 * @param <T>             the snapshot type
	 * @param value           the snapshot
	 * @param computedAt      when it finished being computed
	 * @param computeDuration how long computing it took
	 * @param refreshing      true if a newer snapshot is being computed right now
	 * @param lastFailure     the failure of the most recent computation, or null if it succeeded
	 */
	public record Ready<T>(T value, Instant computedAt, Duration computeDuration, boolean refreshing, Failure lastFailure) implements State<T> {
	}

	/**
	 * No snapshot yet; the first computation is running.
	 *
	 * @param <T>       the snapshot type
	 * @param startedAt when the running computation was started
	 */
	public record Pending<T>(Instant startedAt) implements State<T> {
	}

	/**
	 * No snapshot, and the last computation failed.
	 *
	 * @param <T>     the snapshot type
	 * @param failure what went wrong
	 */
	public record Failed<T>(Failure failure) implements State<T> {
	}

	/**
	 * @param message  the exception message
	 * @param failedAt when the computation failed
	 */
	public record Failure(String message, Instant failedAt) {
	}
}
