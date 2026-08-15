package net.openid.conformance.statistics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncSnapshotCache_UnitTest {

	private static final Instant START = Instant.parse("2026-08-15T09:00:00Z");
	private static final Duration TTL = Duration.ofHours(12);
	private static final Duration BACKOFF = Duration.ofSeconds(60);

	private QueuedExecutor executor;
	private MutableClock clock;
	private AtomicReference<Supplier<String>> computation;
	private AsyncSnapshotCache<String> cache;

	@BeforeEach
	void setUp() {
		executor = new QueuedExecutor();
		clock = new MutableClock(START);
		computation = new AtomicReference<>(() -> "first");
		cache = new AsyncSnapshotCache<>(() -> computation.get().get(), executor, clock, TTL, BACKOFF);
	}

	@Test
	void firstGetStartsOneComputationAndReportsPending() {
		AsyncSnapshotCache.State<String> state = cache.get(false);

		assertThat(executor.submitCount()).isEqualTo(1);
		assertThat(pending(state).startedAt()).isEqualTo(START);
	}

	@Test
	void aSecondGetWhileTheComputationRunsDoesNotStartAnother() {
		cache.get(false);

		AsyncSnapshotCache.State<String> state = cache.get(false);

		assertThat(executor.submitCount()).isEqualTo(1);
		assertThat(pending(state).startedAt()).isEqualTo(START);
	}

	@Test
	void forceWhileTheComputationRunsDoesNotStartAnother() {
		cache.get(false);

		AsyncSnapshotCache.State<String> state = cache.get(true);

		assertThat(executor.submitCount()).isEqualTo(1);
		assertThat(state).isInstanceOf(AsyncSnapshotCache.Pending.class);
	}

	@Test
	void onceTheComputationHasRunTheValueIsServedWithItsDuration() {
		computation.set(() -> {
			clock.advance(Duration.ofSeconds(3));
			return "first";
		});
		cache.get(false);
		executor.runAll();

		AsyncSnapshotCache.Ready<String> ready = ready(cache.get(false));

		assertThat(ready.value()).isEqualTo("first");
		assertThat(ready.computedAt()).isEqualTo(START.plusSeconds(3));
		assertThat(ready.computeDuration()).isEqualTo(Duration.ofSeconds(3));
		assertThat(ready.refreshing()).isFalse();
		assertThat(ready.lastFailure()).isNull();
		assertThat(executor.submitCount()).isEqualTo(1);
	}

	@Test
	void aFreshValueIsServedWithoutRecomputing() {
		cache.get(false);
		executor.runAll();
		clock.advance(TTL.minusMinutes(1));

		AsyncSnapshotCache.Ready<String> ready = ready(cache.get(false));

		assertThat(executor.submitCount()).isEqualTo(1);
		assertThat(ready.refreshing()).isFalse();
	}

	@Test
	void anExpiredValueIsStillServedWhileItIsRecomputed() {
		cache.get(false);
		executor.runAll();
		clock.advance(TTL.plusMinutes(1));
		computation.set(() -> "second");

		AsyncSnapshotCache.Ready<String> stale = ready(cache.get(false));

		assertThat(executor.submitCount()).isEqualTo(2);
		assertThat(stale.value()).isEqualTo("first");
		assertThat(stale.refreshing()).isTrue();

		executor.runAll();
		AsyncSnapshotCache.Ready<String> refreshed = ready(cache.get(false));
		assertThat(refreshed.value()).isEqualTo("second");
		assertThat(refreshed.refreshing()).isFalse();
	}

	@Test
	void forceRecomputesAFreshValue() {
		cache.get(false);
		executor.runAll();
		computation.set(() -> "second");

		AsyncSnapshotCache.Ready<String> ready = ready(cache.get(true));

		assertThat(executor.submitCount()).isEqualTo(2);
		assertThat(ready.value()).isEqualTo("first");
		assertThat(ready.refreshing()).isTrue();
	}

	@Test
	void aFailureWithNoSnapshotIsReportedAndNotRetriedUntilTheBackoffExpires() {
		computation.set(() -> {
			throw new IllegalStateException("mongo is unhappy");
		});
		cache.get(false);
		executor.runAll();

		AsyncSnapshotCache.Failed<String> failed = failed(cache.get(false));
		assertThat(failed.failure().message()).isEqualTo("mongo is unhappy");
		assertThat(failed.failure().failedAt()).isEqualTo(START);
		assertThat(executor.submitCount()).isEqualTo(1);

		clock.advance(BACKOFF);
		cache.get(false);
		assertThat(executor.submitCount()).as("still inside the backoff window").isEqualTo(1);

		clock.advance(Duration.ofSeconds(1));
		assertThat(cache.get(false)).isInstanceOf(AsyncSnapshotCache.Pending.class);
		assertThat(executor.submitCount()).isEqualTo(2);
	}

	@Test
	void forceBypassesTheFailureBackoff() {
		computation.set(() -> {
			throw new IllegalStateException("mongo is unhappy");
		});
		cache.get(false);
		executor.runAll();

		cache.get(true);

		assertThat(executor.submitCount()).isEqualTo(2);
	}

	@Test
	void aFailedRecomputeKeepsThePreviousSnapshotAndReportsTheFailure() {
		cache.get(false);
		executor.runAll();
		clock.advance(TTL.plusMinutes(1));
		computation.set(() -> {
			throw new IllegalStateException("mongo is unhappy");
		});
		cache.get(false);
		executor.runAll();

		AsyncSnapshotCache.Ready<String> ready = ready(cache.get(false));

		assertThat(ready.value()).isEqualTo("first");
		assertThat(ready.refreshing()).isFalse();
		assertThat(ready.lastFailure()).isNotNull();
		assertThat(ready.lastFailure().message()).isEqualTo("mongo is unhappy");
	}

	@Test
	void aSuccessfulRecomputeClearsTheRecordedFailure() {
		cache.get(false);
		executor.runAll();
		clock.advance(TTL.plusMinutes(1));
		computation.set(() -> {
			throw new IllegalStateException("mongo is unhappy");
		});
		cache.get(false);
		executor.runAll();
		clock.advance(BACKOFF.plusSeconds(1));
		computation.set(() -> "second");

		cache.get(false);
		executor.runAll();
		AsyncSnapshotCache.Ready<String> ready = ready(cache.get(false));

		assertThat(ready.value()).isEqualTo("second");
		assertThat(ready.lastFailure()).isNull();
	}

	private static AsyncSnapshotCache.Ready<String> ready(AsyncSnapshotCache.State<String> state) {
		if (state instanceof AsyncSnapshotCache.Ready<String> ready) {
			return ready;
		}
		throw new AssertionError("expected Ready but got " + state);
	}

	private static AsyncSnapshotCache.Pending<String> pending(AsyncSnapshotCache.State<String> state) {
		if (state instanceof AsyncSnapshotCache.Pending<String> pending) {
			return pending;
		}
		throw new AssertionError("expected Pending but got " + state);
	}

	private static AsyncSnapshotCache.Failed<String> failed(AsyncSnapshotCache.State<String> state) {
		if (state instanceof AsyncSnapshotCache.Failed<String> failed) {
			return failed;
		}
		throw new AssertionError("expected Failed but got " + state);
	}

	/** Collects submitted tasks so the test decides when (and whether) they run. */
	private static final class QueuedExecutor implements Executor {

		private final Deque<Runnable> queue = new ArrayDeque<>();
		private int submitCount;

		@Override
		public void execute(Runnable command) {
			queue.add(command);
			submitCount++;
		}

		int submitCount() {
			return submitCount;
		}

		void runAll() {
			while (!queue.isEmpty()) {
				queue.poll().run();
			}
		}
	}

	/** A clock the test moves by hand. */
	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration amount) {
			now = now.plus(amount);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}
}
