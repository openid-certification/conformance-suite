package net.openid.conformance.statistics;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.openid.conformance.statistics.AsyncSnapshotCache.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

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
		this.executor = Executors.newSingleThreadExecutor(daemon("statistics-compute"));
		this.cache = new AsyncSnapshotCache<>(this::compute, executor, Clock.systemUTC(), TTL, FAILURE_BACKOFF);
	}

	@Override
	public State<StatisticsCube> getCube(boolean refresh) {
		return cache.get(refresh);
	}

	/**
	 * Makes sure {@code TEST_INFO} has the {@code {started: 1}} index that every windowed
	 * pipeline of {@link MongoStatisticsSource} reads through, on a thread of its own so
	 * that nothing waits for it.
	 *
	 * <p>Deliberately not declared with the suite's other indexes in
	 * {@code DBTestInfoService.createIndexes()}: that runs in a {@code @PostConstruct}
	 * before the server accepts its first request, and on a production sized {@code
	 * TEST_INFO} - millions of documents, tens of gigabytes - the build takes longer than
	 * the pod's liveness probe allows, so the deployment would be killed and restarted
	 * mid-build, for ever. Not on the statistics executor either: a build of minutes
	 * would be minutes the first overview sat behind, and the overview does not need the
	 * index to be correct, only to be quick.
	 *
	 * <p>So this is belt and braces. On a big deployment the index is built out of band
	 * before the release goes out (see {@link MongoStatisticsSource#ensureStartedIndex()} for
	 * the command) and this finds it already there, which costs one command; on every other deployment - a
	 * laptop, a review environment, a fresh install - it is a second of work nobody has to
	 * remember to do. A failure is logged and left alone: the pipelines are all correct
	 * without the index, they just read more.
	 */
	@PostConstruct
	private void ensureStartedIndexInBackground() {
		// daemon for the same reason the compute threads are: an index build is never worth
		// holding a shutdown up for, and the next startup will pick up where this left off
		Thread thread = new Thread(this::ensureStartedIndex, "statistics-index");
		thread.setDaemon(true);
		thread.start();
	}

	/** Runs on the {@code statistics-index} thread; must not throw, there is nobody to catch it. */
	private void ensureStartedIndex() {
		Instant startedAt = Instant.now();
		logger.info("Ensuring TEST_INFO has the {started: 1} index the statistics windows read through; "
			+ "this is a no-op where it has been built already, and minutes of work where it has not");
		try {
			source.ensureStartedIndex();
			logger.info("TEST_INFO has the {started: 1} index (took {}ms)",
				Duration.between(startedAt, Instant.now()).toMillis());
		} catch (RuntimeException e) {
			logger.warn("Could not create the {started: 1} index of TEST_INFO after {}ms; the statistics "
					+ "pipelines still work without it, they just read more of the collection",
				Duration.between(startedAt, Instant.now()).toMillis(), e);
		}
	}

	/**
	 * @return when this JVM came up. A run still RUNNING or WAITING from before then was
	 *         orphaned by the restart - the suite keeps running tests in memory - so the
	 *         in-progress tiles are bounded by it rather than by a calendar window.
	 */
	private static Instant serverStartedAt() {
		return Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime());
	}

	/** @return a thread factory for one background thread; a snapshot is never worth holding up a shutdown for */
	private static ThreadFactory daemon(String name) {
		return runnable -> {
			Thread thread = new Thread(runnable, name);
			thread.setDaemon(true);
			return thread;
		};
	}

	/**
	 * Runs on the {@code statistics-compute} thread; may throw, which the cache records.
	 * Each pipeline is timed on its own, so the log says which of them a slow snapshot is
	 * spent in.
	 */
	private StatisticsCube compute() {
		Instant startedAt = Instant.now();
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		Timings timings = new Timings();
		List<RunCell> runs = timings.time("runs", source::runs);
		List<PlanCell> plans = timings.time("plans", source::plans);
		List<UserTuple> users = timings.time("users", source::users);
		List<HeatCell> heat = timings.time("heat", () -> source.heat(today));
		List<ModuleUserCell> modules = timings.time("modules", () -> source.modules(today));
		List<HostRow> hosts = timings.time("hosts", () -> source.externalHosts(today));
		List<StorageRow> storage = timings.time("storage", source::storage);
		long totalUsers = users.stream().mapToInt(UserTuple::ownerId).distinct().count();
		TileRow tiles = timings.time("tiles", () -> source.tiles(startedAt, serverStartedAt(), totalUsers));
		StatisticsCube cube = timings.time("cube",
			() -> new StatisticsCube(runs, plans, users, heat, modules, hosts, storage, tiles, resolver, today));
		logger.info("Computed the statistics cube in {}ms ({}): {} run cells, {} plan cells, {} user tuples, "
				+ "{} heat cells, {} module cells, {} external hosts; {} months, {} weeks",
			Duration.between(startedAt, Instant.now()).toMillis(), timings,
			runs.size(), plans.size(), users.size(), heat.size(), modules.size(), hosts.size(),
			cube.periods(Granularity.MONTH).size(), cube.periods(Granularity.WEEK).size());
		return cube;
	}

	/** How long each step of a computation took, in the order they ran, for the log line. */
	private static final class Timings {

		private final List<String> steps = new ArrayList<>();

		<T> T time(String step, Supplier<T> work) {
			Instant before = Instant.now();
			T result = work.get();
			steps.add(step + " " + Duration.between(before, Instant.now()).toMillis() + "ms");
			return result;
		}

		@Override
		public String toString() {
			return String.join(", ", steps);
		}
	}

	@PreDestroy
	public void shutdown() {
		executor.shutdownNow();
	}
}
