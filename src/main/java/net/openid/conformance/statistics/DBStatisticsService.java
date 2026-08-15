package net.openid.conformance.statistics;

import jakarta.annotation.PreDestroy;
import net.openid.conformance.statistics.AsyncSnapshotCache.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Computes the statistics snapshot from MongoDB, off the request path.
 *
 * <p>The aggregations group over whole collections, so the snapshot is computed at most
 * once every {@link #TTL} on a single background thread and served to every admin from
 * memory. A snapshot that exists is always served, even while it is being recomputed and
 * even if the last recomputation failed - see {@link AsyncSnapshotCache}.
 */
@Service
public class DBStatisticsService implements StatisticsService {

	/** How long a snapshot is served before the next request triggers a recomputation. */
	private static final Duration TTL = Duration.ofHours(12);

	/** How long to wait after a failed computation before another one is attempted. */
	private static final Duration FAILURE_BACKOFF = Duration.ofSeconds(60);

	private static final Logger logger = LoggerFactory.getLogger(DBStatisticsService.class);

	private final MongoStatisticsSource source;

	private final StatisticsAssembler assembler;

	private final ExecutorService executor;

	private final AsyncSnapshotCache<StatisticsOverview> cache;

	@Autowired
	public DBStatisticsService(MongoStatisticsSource source, SpecFamilyResolver resolver) {
		this.source = source;
		this.assembler = new StatisticsAssembler(resolver);
		this.executor = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "statistics-compute");
			// the snapshot is never worth holding up a shutdown for
			thread.setDaemon(true);
			return thread;
		});
		this.cache = new AsyncSnapshotCache<>(this::compute, executor, Clock.systemUTC(), TTL, FAILURE_BACKOFF);
	}

	@Override
	public State<StatisticsOverview> getOverview(boolean refresh) {
		return cache.get(refresh);
	}

	/** Runs on the {@code statistics-compute} thread; may throw, which the cache records. */
	private StatisticsOverview compute() {
		Instant startedAt = Instant.now();
		StatisticsOverview overview = assembler.assemble(
			source.runsByMonthAndPlan(),
			source.plansByMonthAndName(),
			source.usersByMonth(),
			source.tiles(startedAt),
			YearMonth.now(ZoneOffset.UTC));
		logger.info("Computed statistics overview in {}ms: {} test runs, {} plans, {} users over {} months",
			Duration.between(startedAt, Instant.now()).toMillis(),
			overview.tiles().totalTests(), overview.tiles().totalPlans(), overview.tiles().totalUsers(),
			overview.months().size());
		return overview;
	}

	@PreDestroy
	public void shutdown() {
		executor.shutdownNow();
	}
}
