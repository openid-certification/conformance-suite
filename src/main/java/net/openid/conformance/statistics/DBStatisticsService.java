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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Computes the statistics cube from MongoDB, off the request path.
 *
 * <p>The aggregations group over whole collections, so the cube is computed at most
 * once every {@link #TTL} on a single background thread and then sliced per request by
 * {@link StatisticsSlicer}, so that changing a filter costs a walk over a few thousand
 * cells rather than another pass over the database. A cube that exists is always served,
 * even while it is being recomputed and even if the last recomputation failed - see
 * {@link AsyncSnapshotCache}.
 */
@Service
public class DBStatisticsService implements StatisticsService {

	/** How long a cube is served before the next request triggers a recomputation. */
	private static final Duration TTL = Duration.ofHours(12);

	/** How long to wait after a failed computation before another one is attempted. */
	private static final Duration FAILURE_BACKOFF = Duration.ofSeconds(60);

	private static final Logger logger = LoggerFactory.getLogger(DBStatisticsService.class);

	private final MongoStatisticsSource source;

	private final SpecFamilyResolver resolver;

	private final ExecutorService executor;

	private final AsyncSnapshotCache<StatisticsCube> cache;

	@Autowired
	public DBStatisticsService(MongoStatisticsSource source, SpecFamilyResolver resolver) {
		this.source = source;
		this.resolver = resolver;
		this.executor = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "statistics-compute");
			// the snapshot is never worth holding up a shutdown for
			thread.setDaemon(true);
			return thread;
		});
		this.cache = new AsyncSnapshotCache<>(this::compute, executor, Clock.systemUTC(), TTL, FAILURE_BACKOFF);
	}

	@Override
	public State<StatisticsCube> getCube(boolean refresh) {
		return cache.get(refresh);
	}

	/** Runs on the {@code statistics-compute} thread; may throw, which the cache records. */
	private StatisticsCube compute() {
		Instant startedAt = Instant.now();
		List<RunCell> runs = source.runs();
		List<PlanCell> plans = source.plans();
		List<UserTuple> users = source.users();
		List<HeatCell> heat = source.heat();
		List<HostRow> hosts = source.externalHosts();
		StatisticsCube cube = new StatisticsCube(runs, plans, users, heat, hosts, source.storage(),
			source.tiles(startedAt), resolver, LocalDate.now(ZoneOffset.UTC));
		logger.info("Computed the statistics cube in {}ms: {} run cells, {} plan cells, {} user tuples, "
				+ "{} heat cells, {} external hosts; {} months, {} weeks",
			Duration.between(startedAt, Instant.now()).toMillis(),
			runs.size(), plans.size(), users.size(), heat.size(), hosts.size(),
			cube.periods(Granularity.MONTH).size(), cube.periods(Granularity.WEEK).size());
		return cube;
	}

	@PreDestroy
	public void shutdown() {
		executor.shutdownNow();
	}
}
