package net.openid.conformance.statistics;

import com.mongodb.MongoException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import net.openid.conformance.info.DBTestInfoService;
import net.openid.conformance.info.DBTestPlanService;
import net.openid.conformance.logging.DBEventLog;
import net.openid.conformance.testmodule.TestModule.Result;
import net.openid.conformance.testmodule.TestModule.Status;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs the whole-collection aggregations behind the admin statistics page - the only
 * class in this package that talks to MongoDB.
 *
 * <p>These pipelines group over whole collections, so they are expensive by construction
 * and must never run on a request thread; {@link DBStatisticsService} runs them on a
 * background thread at most once per TTL. Each one groups the collection down to at most a
 * few thousand cells before anything crosses the wire, so what Java receives is small
 * however big the database is.
 *
 * <p>Everything that only needs recent data starts with a {@code $match} on {@code started},
 * which the {@code {started: 1}} index of {@code TEST_INFO} turns into a range scan instead
 * of a collection scan - on a production sized database that is the difference between
 * reading tens of gigabytes and reading a few (see {@link #ensureStartedIndex()} for where
 * that index comes from). Only the all-time series - the run and plan cubes, which are what
 * the charts are - still read a whole collection.
 *
 * <p>Deliberately not built on {@code DBTestInfoService} / {@code DBTestPlanService}:
 * those scope every query to the authenticated user, and this computation runs with no
 * security context and has to see every user's data.
 */
@Component
public class MongoStatisticsSource {

	/** Identifies these aggregations in {@code currentOp} and the Mongo slow-query log. */
	private static final String COMMENT = "statistics-overview";

	/**
	 * How long a statistics pipeline may run before the server kills it.
	 *
	 * <p>Generous because these are background computations on a schedule of hours, and the
	 * alternative to letting a slow one finish is a page that never has a snapshot at all:
	 * nothing waits on them, the caches serve whatever they last produced, and a runaway is
	 * still bounded. Read on the server, so it also caps the recomputation the failure
	 * backoff of {@link DBStatisticsService} is sized against.
	 */
	private static final long MAX_TIME_MINUTES = 30;

	/** The longest of the rolling recency windows the tiles report. */
	private static final Duration LONGEST_RECENCY_WINDOW = Duration.ofDays(30);

	private static final List<String> IN_PROGRESS_STATUSES = names(Status.RUNNING, Status.WAITING);

	private static final List<String> NON_TERMINAL_STATUSES =
		names(Status.CREATED, Status.CONFIGURED, Status.RUNNING, Status.WAITING);

	/** The collections the storage tiles report on, in the order they are shown. */
	private static final List<String> STORAGE_COLLECTIONS =
		List.of(DBTestInfoService.COLLECTION, DBTestPlanService.COLLECTION, DBEventLog.COLLECTION);

	/** The configuration fields naming the server under test, most specific first. */
	private static final List<String> TARGET_URL_FIELDS = List.of(
		"$config.server.discoveryUrl", "$config.server.issuer",
		"$config.vci.credential_issuer_url", "$config.federation.entity_identifier");

	/** Marks a URL served by this suite's own test endpoints rather than by a real server. */
	private static final String EMULATED_SERVER_PATH = "/test/a/";

	/** The host part of a URL: everything between the scheme and the port, path or query. */
	private static final String HOST_OF_URL = "^https?://([^/:?#]+)";

	/** How many external servers are reported, by descending run count. */
	private static final int MAX_EXTERNAL_HOSTS = 100;

	private static final Logger logger = LoggerFactory.getLogger(MongoStatisticsSource.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	/**
	 * Module runs and their result buckets, per period, test plan, plan level variant and
	 * certification profile.
	 *
	 * <p>Grouped by plan id first (one bucket per plan ever run, per period), then joined
	 * to {@code TEST_PLAN} by {@code _id} to pick up what the plan was - an index seek per
	 * bucket, rather than pulling every plan document over to Java, and carrying nothing
	 * but three fields out of the join (see {@link #planLookup()}) - then projected down
	 * immediately so only the counters and those fields reach the second group, which
	 * collapses the per-plan-id buckets into one cell per plan configuration.
	 *
	 * <p>The second group keys on the variant sub-document as MongoDB stored it rather than
	 * on a canonical form of it: field order within one stored document is stable, so two
	 * plans configured the same way group together unless MongoDB happened to store their
	 * fields in different orders, and {@link VariantKeys#canonical} plus the roll-up in
	 * {@link StatisticsCube} merge those in Java.
	 *
	 * @param nowUtc today in UTC, which the weekly window is measured back from
	 * @return one cell per month, ISO week, plan name, standalone-ness, variant and
	 *         certification profile
	 */
	public List<RunCell> runs(LocalDate nowUtc) {
		String oldestWeek = StatisticsCube.oldestWeek(nowUtc);
		Document runsByPlanId = new Document("month", monthExpression())
			.append("week", weekExpression(oldestWeek))
			// null for a standalone run; explicit so it survives as a group key
			.append("planId", new Document("$ifNull", Arrays.asList("$planId", null)));
		List<Bson> pipeline = List.of(
			Aggregates.group(runsByPlanId,
				Accumulators.sum("runs", 1),
				Accumulators.sum("passed", isResult(Result.PASSED)),
				Accumulators.sum("failed", isResult(Result.FAILED)),
				Accumulators.sum("warning", isResult(Result.WARNING)),
				Accumulators.sum("review", isResult(Result.REVIEW)),
				Accumulators.sum("skipped", isResult(Result.SKIPPED))),
			planLookup(),
			Aggregates.project(Projections.fields(
				Projections.excludeId(),
				Projections.computed("month", "$_id.month"),
				Projections.computed("week", "$_id.week"),
				Projections.computed("standalone", new Document("$eq", Arrays.asList("$_id.planId", null))),
				// missing if the plan document has been deleted since the run
				Projections.computed("planName", new Document("$arrayElemAt", List.of("$plan.planName", 0))),
				Projections.computed("variant", new Document("$arrayElemAt", List.of("$plan.variant", 0))),
				Projections.computed("cert",
					new Document("$arrayElemAt", List.of("$plan.certificationProfileName", 0))),
				Projections.include("runs", "passed", "failed", "warning", "review", "skipped"))),
			Aggregates.group(new Document("month", "$month")
					.append("week", "$week")
					.append("planName", "$planName")
					.append("standalone", "$standalone")
					.append("variant", "$variant")
					.append("cert", "$cert"),
				Accumulators.sum("runs", "$runs"),
				Accumulators.sum("passed", "$passed"),
				Accumulators.sum("failed", "$failed"),
				Accumulators.sum("warning", "$warning"),
				Accumulators.sum("review", "$review"),
				Accumulators.sum("skipped", "$skipped")));

		List<RunCell> cells = new ArrayList<>();
		for (Document document : aggregate(DBTestInfoService.COLLECTION, pipeline)) {
			cells.add(runCell(document));
		}
		return cells;
	}

	/**
	 * @param document one grouped row of {@link #runs(LocalDate)}
	 * @return the cell it stands for. Where the raw variant sub-document and the raw
	 *         certification profile list are turned into keys - so a test can feed rows
	 *         shaped like MongoDB's straight through and see that two plans configured the
	 *         same way end up in one cell however their fields happened to be ordered
	 */
	static RunCell runCell(Document document) {
		Document id = document.get("_id", Document.class);
		return new RunCell(id.getString("month"), id.getString("week"), id.getString("planName"),
			Boolean.TRUE.equals(id.getBoolean("standalone")),
			VariantKeys.canonical(id.get("variant")), CertKeys.canonical(id.get("cert")),
			count(document, "runs"), count(document, "passed"), count(document, "failed"),
			count(document, "warning"), count(document, "review"), count(document, "skipped"));
	}

	/**
	 * Test plans created per period, plan, plan level variant and certification profile,
	 * how many of them were made immutable - which is what downloading a certification
	 * package does - and how many were published.
	 *
	 * @param nowUtc today in UTC, which the weekly window is measured back from
	 * @return one cell per month, ISO week, plan name, variant and certification profile
	 */
	public List<PlanCell> plans(LocalDate nowUtc) {
		List<Bson> pipeline = List.of(
			Aggregates.group(new Document("month", monthExpression())
					.append("week", weekExpression(StatisticsCube.oldestWeek(nowUtc)))
					.append("planName", "$planName")
					.append("variant", "$variant")
					.append("cert", "$certificationProfileName"),
				Accumulators.sum("plans", 1),
				Accumulators.sum("certified", ifThenOne(new Document("$eq", List.of("$immutable", true)))),
				Accumulators.sum("published", ifThenOne(isSet("$publish")))));

		List<PlanCell> cells = new ArrayList<>();
		for (Document document : aggregate(DBTestPlanService.COLLECTION, pipeline)) {
			cells.add(planCell(document));
		}
		return cells;
	}

	/**
	 * @param document one grouped row of {@link #plans(LocalDate)}
	 * @return the cell it stands for, keyed exactly as {@link #runCell(Document)} keys a run
	 *         cell - which is what lets the plan series and the run series be filtered by the
	 *         same variant and certification profile keys
	 */
	static PlanCell planCell(Document document) {
		Document id = document.get("_id", Document.class);
		return new PlanCell(id.getString("month"), id.getString("week"), id.getString("planName"),
			VariantKeys.canonical(id.get("variant")), CertKeys.canonical(id.get("cert")),
			count(document, "plans"), count(document, "certified"), count(document, "published"));
	}

	/**
	 * The periods each user created test plans in, per plan, plan level variant and
	 * certification profile.
	 *
	 * <p>Counted on a plan basis rather than over test runs, which is what lets the users
	 * series be filtered the same way every other series is: a run only knows which plan
	 * instance it belonged to, a plan knows what it is.
	 *
	 * @param nowUtc today in UTC, which the weekly window is measured back from
	 * @return one tuple per user, plan, variant and certification profile. Users are
	 *         identified by a counter rather than by their {@code iss} and {@code sub},
	 *         which are never needed beyond counting distinct users and are dropped here.
	 */
	public List<UserTuple> users(LocalDate nowUtc) {
		List<Bson> pipeline = List.of(
			Aggregates.group(new Document("planName", "$planName")
					.append("variant", "$variant")
					.append("cert", "$certificationProfileName")
					.append("iss", "$owner.iss")
					.append("sub", "$owner.sub"),
				Accumulators.addToSet("months", monthExpression()),
				Accumulators.addToSet("weeks", weekExpression(StatisticsCube.oldestWeek(nowUtc)))));

		List<UserTuple> tuples = new ArrayList<>();
		OwnerIds owners = new OwnerIds();
		for (Document document : aggregate(DBTestPlanService.COLLECTION, pipeline)) {
			Document id = document.get("_id", Document.class);
			String iss = id.getString("iss");
			String sub = id.getString("sub");
			if (!OwnerIds.isUser(iss, sub)) {
				// a plan written before authentication completed is not a user
				continue;
			}
			tuples.add(new UserTuple(id.getString("planName"), VariantKeys.canonical(id.get("variant")),
				CertKeys.canonical(id.get("cert")), owners.idFor(iss, sub),
				strings(document, "months"), strings(document, "weeks")));
		}
		return tuples;
	}

	/**
	 * Runs of each test module, per month and user, over the trailing
	 * {@value StatisticsCube#MODULE_MONTHS} months.
	 *
	 * <p>The user is part of the group key because the modules table counts people rather
	 * than runs: a user who failed the same module twenty times is one user who hit a
	 * failure on it. Grouping that out in the database keeps what crosses the wire to one
	 * row per month, module and user, and leaves the {@code iss} and {@code sub} behind.
	 *
	 * <p>The window is a string comparison on {@code started}, which {@code TestInfo} writes
	 * as an ISO-8601 UTC string, so the {@code $match} in front of the group is an index
	 * range rather than a scan of the collection - the same shape as the heatmap, the
	 * external hosts and the tiles, which are all windowed for the same reason.
	 *
	 * @param nowUtc today in UTC; the window ends with the month it falls in
	 * @return one cell per month, test module and user. Rows with no test module name, and
	 *         rows whose owner is not a real identity, are dropped: neither can be counted
	 *         as a user of a named module.
	 */
	public List<ModuleUserCell> modules(LocalDate nowUtc) {
		List<Bson> pipeline = List.of(
			Aggregates.match(Filters.gte("started", StatisticsCube.oldestModuleMonth(nowUtc))),
			Aggregates.group(new Document("month", monthExpression())
					.append("testName", "$testName")
					.append("iss", "$owner.iss")
					.append("sub", "$owner.sub"),
				Accumulators.sum("runs", 1),
				Accumulators.sum("failed", isResult(Result.FAILED))));

		List<ModuleUserCell> cells = new ArrayList<>();
		OwnerIds owners = new OwnerIds();
		Map<String, String> pool = new HashMap<>();
		for (Document document : aggregate(DBTestInfoService.COLLECTION, pipeline)) {
			ModuleUserCell cell = moduleCell(document, owners, pool);
			if (cell != null) {
				cells.add(cell);
			}
		}
		return cells;
	}

	/**
	 * @param document one grouped row of {@link #modules(LocalDate)}
	 * @param owners   the ids handed out to this pipeline's users
	 * @param pool     the month keys and module names already seen, shared by every cell
	 * @return the cell it stands for, or null if the row cannot be counted: a run written
	 *         before authentication completed has no user to count, and a run with no
	 *         {@code testName} has no module to count it against
	 */
	static ModuleUserCell moduleCell(Document document, OwnerIds owners, Map<String, String> pool) {
		Document id = document.get("_id", Document.class);
		String iss = id.getString("iss");
		String sub = id.getString("sub");
		String testName = id.getString("testName");
		if (!OwnerIds.isUser(iss, sub) || testName == null || testName.isBlank()) {
			return null;
		}
		return new ModuleUserCell(pooled(pool, id.getString("month")), pooled(pool, testName),
			owners.idFor(iss, sub), count(document, "runs"), count(document, "failed"));
	}

	/**
	 * @param pool  the strings this pipeline has already seen
	 * @param value one of the two strings a module cell carries
	 * @return the one instance of it. There is a cell per user, module and month, and the
	 *         driver decodes a fresh String for every row, so without this the cells hold
	 *         one copy of the same two dozen month keys and few hundred module names each -
	 *         which on a production sized database is most of what the cells weigh. The pool
	 *         is local to the computation and thrown away with it; {@link String#intern()}
	 *         would put the same strings somewhere they can never be collected from.
	 */
	private static String pooled(Map<String, String> pool, String value) {
		return value == null ? null : pool.computeIfAbsent(value, string -> string);
	}

	/**
	 * Creates the {@code {started: 1}} index of {@code TEST_INFO} unless it is there
	 * already: the index every windowed pipeline here depends on to be a range of the
	 * collection rather than the whole of it.
	 *
	 * <p>A deployment big enough for that to matter should have had the index built out of
	 * band before the release went out, off peak, with {@code mongosh} on the pod:
	 * {@code db.TEST_INFO.createIndex({started: 1})} - minutes of work, a few hundred MB on
	 * 7 million documents - and this then reads the index list and creates nothing. Where it
	 * does have to build it, the build is minutes of work on a large collection, so it must
	 * not happen on any path something waits for: {@link DBStatisticsService} calls this on
	 * a background thread of its own, and it is not declared with the suite's other indexes
	 * in {@code DBTestInfoService}, which are built before the server serves its first
	 * request.
	 *
	 * <p>The index is looked for by its key, not its name: production already has one,
	 * built long ago under the name {@code started} rather than the {@code started_1}
	 * MongoDB would derive today, and {@code createIndex} for the same key under a different
	 * name is not a no-op, it fails with {@code IndexOptionsConflict}.
	 *
	 * @throws MongoException if the index cannot be created
	 */
	void ensureStartedIndex() {
		Document key = new Document("started", 1);
		MongoCollection<Document> collection = mongoTemplate.getCollection(DBTestInfoService.COLLECTION);
		for (Document index : collection.listIndexes()) {
			if (key.equals(index.get("key"))) {
				return;
			}
		}
		collection.createIndex(key);
	}

	/**
	 * When tests are run: every run of the trailing {@value StatisticsCube#MODULE_MONTHS}
	 * months bucketed by the UTC day and hour it started in.
	 *
	 * <p>Both keys are cut out of the {@code started} string rather than parsed, because
	 * that is all the heatmap needs; a document whose {@code started} is unusable produces
	 * keys that do not parse, and {@link StatisticsCube} drops those when it bins the cells.
	 *
	 * <p>Windowed like {@link #modules(LocalDate)}, and for the same reason: the heatmap is
	 * a question about when people work, which nobody asks of a run from four years ago, and
	 * the window is what lets the {@code {started: 1}} index turn this into a range scan.
	 * A range older than the window therefore draws an empty heatmap rather than a
	 * historical one.
	 *
	 * @param nowUtc today in UTC; the window ends with the month it falls in
	 * @return one cell per day and hour that has any runs in it
	 */
	public List<HeatCell> heat(LocalDate nowUtc) {
		List<Bson> pipeline = List.of(
			// a string comparison is type-bracketed, so a started written as a BSON date
			// falls outside the window rather than into it
			Aggregates.match(Filters.gte("started", StatisticsCube.oldestModuleMonth(nowUtc))),
			Aggregates.group(new Document("day", new Document("$substrBytes", List.of(startedAsString(), 0, 10)))
					.append("hour", new Document("$substrBytes", List.of(startedAsString(), 11, 2))),
				Accumulators.sum("runs", 1)));

		List<HeatCell> cells = new ArrayList<>();
		for (Document document : aggregate(DBTestInfoService.COLLECTION, pipeline)) {
			Document id = document.get("_id", Document.class);
			cells.add(new HeatCell(id.getString("day"), id.getString("hour"), count(document, "runs")));
		}
		return cells;
	}

	/**
	 * The external servers the suite has been pointed at over the trailing
	 * {@value StatisticsCube#HOST_MONTHS} months, by descending run count. The window is
	 * shorter than the modules' because this is the snapshot's most expensive scan: it reads
	 * every run's configuration, which is most of a {@code TEST_INFO} document.
	 *
	 * <p>The server under test is named by a different configuration field depending on
	 * what is being tested, so the first field of {@link #TARGET_URL_FIELDS} that is set
	 * wins. URLs served by this suite's own emulated endpoints are dropped: a run against
	 * {@code /test/a/...} is the suite testing a client, and the "external server" it names
	 * is this deployment.
	 *
	 * <p>The host is cut out of the URL and lower-cased by MongoDB rather than in Java so
	 * that the whole thing - including counting distinct users per host, which has to
	 * happen after several URLs have collapsed onto one host - stays server side, and the
	 * users' {@code iss} and {@code sub} never leave the database.
	 *
	 * <p>Windowed like {@link #heat(LocalDate)}: what this answers is who is testing against
	 * the suite now, and reading the whole of {@code TEST_INFO} - configuration blobs
	 * included, since the URLs are in them - to answer it is the single most expensive thing
	 * the overview did. A server nobody has used for two years drops off the list.
	 *
	 * @param nowUtc today in UTC; the window ends with the month it falls in
	 * @return at most {@value #MAX_EXTERNAL_HOSTS} hosts, most used first
	 */
	public List<HostRow> externalHosts(LocalDate nowUtc) {
		List<Bson> pipeline = List.of(
			// type-bracketed like the heatmap's: a started written as a BSON date is outside
			// this window, and the host it named is not reported
			Aggregates.match(Filters.gte("started", StatisticsCube.oldestHostMonth(nowUtc))),
			Aggregates.project(Projections.fields(
				Projections.excludeId(),
				Projections.computed("url", targetUrl()),
				Projections.computed("iss", "$owner.iss"),
				Projections.computed("sub", "$owner.sub"),
				Projections.computed("started", startedAsString()))),
			Aggregates.match(Filters.ne("url", "")),
			Aggregates.project(Projections.fields(
				Projections.computed("host", externalHost()),
				Projections.include("iss", "sub", "started"))),
			Aggregates.match(Filters.ne("host", null)),
			Aggregates.group(new Document("host", "$host").append("iss", "$iss").append("sub", "$sub"),
				Accumulators.sum("runs", 1),
				Accumulators.max("last", "$started")),
			Aggregates.group("$_id.host",
				Accumulators.sum("runs", "$runs"),
				// the group is one owner, but an unauthenticated run's owner is not a user
				Accumulators.sum("users", ifThenOne(ownerIsIdentified("$_id.iss", "$_id.sub"))),
				Accumulators.max("lastSeen", "$last")),
			// the host breaks ties so the cut is the same list every recomputation:
			// most of the tail has one run each, and the client sees this list churn
			Aggregates.sort(Sorts.orderBy(Sorts.descending("runs"), Sorts.ascending("_id"))),
			Aggregates.limit(MAX_EXTERNAL_HOSTS));

		List<HostRow> hosts = new ArrayList<>();
		for (Document document : aggregate(DBTestInfoService.COLLECTION, pipeline)) {
			hosts.add(new HostRow(document.getString("_id"), count(document, "runs"), count(document, "users"),
				document.getString("lastSeen")));
		}
		return hosts;
	}

	/**
	 * How much space the test data takes up.
	 *
	 * @return one row per collection of {@link #STORAGE_COLLECTIONS}, in that order; a
	 *         collection that does not exist yet reports zeros rather than being left out,
	 *         so the tiles do not move around on a fresh deployment
	 */
	public List<StorageRow> storage() {
		List<StorageRow> rows = new ArrayList<>(STORAGE_COLLECTIONS.size());
		for (String collection : STORAGE_COLLECTIONS) {
			rows.add(collectionStorage(collection));
		}
		return rows;
	}

	private StorageRow collectionStorage(String collection) {
		try {
			Document stats = mongoTemplate.getDb().runCommand(new Document("collStats", collection));
			return new StorageRow(collection, count(stats, "count"), count(stats, "size"),
				count(stats, "storageSize"), count(stats, "totalIndexSize"));
		} catch (MongoException e) {
			// a collection nothing has been written to yet, or a server that will not say
			logger.warn("Could not read the storage statistics of {}", collection, e);
			return new StorageRow(collection, 0, 0, 0, 0);
		}
	}

	/**
	 * The summary tile counters: one range scan over the recent runs, plus the total that
	 * cannot come out of it.
	 *
	 * <p>{@code started} is stored as an ISO-8601 UTC string, so both the window and the
	 * cutoffs are string comparisons, and the {@code $match} is an index range rather than a
	 * scan of the collection. It reaches back to the earlier of thirty days ago (the longest
	 * recency counter) and the moment this server started, which is where {@code inProgress}
	 * and {@code stuck} are bounded: a run still RUNNING or WAITING from before the server
	 * came up was orphaned by the restart, not left in progress, so it is nobody's problem
	 * and neither tile counts it.
	 *
	 * <p>{@code total} is the collection's own document count rather than a counted scan: it
	 * is a metadata read whatever the size of the database, and a tile reading
	 * "7,138,402 tests" does not become wrong in any way a person cares about if the count
	 * is a few documents stale after an unclean shutdown.
	 *
	 * @param now             the instant the "last 24 hours / 7 days / 30 days" windows end at
	 * @param serverStartedAt when this server instance came up; runs started before it
	 *                        cannot be in progress
	 * @param totalUsers      how many distinct users have ever created a test plan: the
	 *                        number of distinct owners {@link #users(LocalDate)} handed an id to, so
	 *                        the tile costs no scan of its own and counts users on exactly
	 *                        the basis the "active users" series does
	 * @return the counters; the windowed ones all zero if nothing has run inside the window
	 */
	public TileRow tiles(Instant now, Instant serverStartedAt, long totalUsers) {
		String cutoff24h = now.minus(Duration.ofHours(24)).toString();
		String cutoff7d = now.minus(Duration.ofDays(7)).toString();
		String cutoff30d = now.minus(LONGEST_RECENCY_WINDOW).toString();
		String sinceServerStart = serverStartedAt.toString();
		Instant oldest = serverStartedAt.isBefore(now.minus(LONGEST_RECENCY_WINDOW))
			? serverStartedAt : now.minus(LONGEST_RECENCY_WINDOW);
		Document started = startedAsString();
		Document sinceRestart = new Document("$gte", List.of(started, sinceServerStart));

		List<Bson> pipeline = List.of(
			// type-bracketed like the other windows: a started written as a BSON date is
			// outside this one, so the run is in no counter but the estimated total
			Aggregates.match(Filters.gte("started", oldest.toString())),
			Aggregates.group(null,
				Accumulators.sum("last24h", ifThenOne(new Document("$gte", List.of(started, cutoff24h)))),
				Accumulators.sum("last7d", ifThenOne(new Document("$gte", List.of(started, cutoff7d)))),
				Accumulators.sum("last30d", ifThenOne(new Document("$gte", List.of(started, cutoff30d)))),
				Accumulators.sum("inProgress", ifThenOne(new Document("$and", List.of(
					new Document("$in", List.of("$status", IN_PROGRESS_STATUSES)), sinceRestart)))),
				// non-terminal, started on this server, and too old to still be making progress
				Accumulators.sum("stuck", ifThenOne(new Document("$and", List.of(
					new Document("$in", List.of("$status", NON_TERMINAL_STATUSES)), sinceRestart,
					new Document("$lt", List.of(started, cutoff24h))))))));

		long total = mongoTemplate.getCollection(DBTestInfoService.COLLECTION).estimatedDocumentCount();
		List<Document> results = aggregate(DBTestInfoService.COLLECTION, pipeline);
		// count() reads a missing counter as 0, which is what an empty window means
		Document document = results.isEmpty() ? new Document() : results.get(0);
		return new TileRow(total, totalUsers, count(document, "last24h"), count(document, "last7d"),
			count(document, "last30d"), count(document, "inProgress"), count(document, "stuck"));
	}

	/**
	 * The plan join used by {@link #runs(LocalDate)}, in the concise correlated form MongoDB has
	 * supported since 5.0 (the suite targets FCV 6.0): the equality match on
	 * {@code TEST_PLAN._id} still uses the primary key index, but the inner pipeline
	 * projects every matched plan down to the three fields a run cell is keyed by
	 * <em>inside the server</em>, so the plans' {@code config} blobs are never materialised
	 * into the {@code plan} array. Without it the join streams whole plan documents -
	 * configuration included - through one bucket per plan ever run, which on
	 * production-scale data is most of the pipeline's memory and network cost.
	 *
	 * <p>Built as a raw stage because the driver has no {@code Aggregates.lookup} overload
	 * taking {@code localField} / {@code foreignField} <em>and</em> a pipeline.
	 *
	 * @return the {@code $lookup} stage yielding
	 *         {@code plan: [{planName, variant, certificationProfileName}]}
	 */
	private static Document planLookup() {
		Document projection = new Document("_id", 0)
			.append("planName", 1)
			.append("variant", 1)
			.append("certificationProfileName", 1);
		return new Document("$lookup", new Document("from", DBTestPlanService.COLLECTION)
			.append("localField", "_id.planId")
			.append("foreignField", "_id")
			.append("pipeline", List.of(new Document("$project", projection)))
			.append("as", "plan"));
	}

	private List<Document> aggregate(String collection, List<Bson> pipeline) {
		AggregateIterable<Document> aggregation = mongoTemplate.getCollection(collection)
			.aggregate(pipeline)
			.allowDiskUse(true)
			.maxTime(MAX_TIME_MINUTES, TimeUnit.MINUTES)
			.comment(COMMENT);
		return aggregation.into(new ArrayList<>());
	}

	/**
	 * @return the {@code YYYY-MM} key of a document's {@code started} field; the empty
	 *         string - which the cube never matches to a period - if it is missing or not
	 *         string-like
	 */
	private static Document monthExpression() {
		return new Document("$substrBytes", List.of(startedAsString(), 0, 7));
	}

	/**
	 * The ISO week key, computed only for a document that can still be in the weekly
	 * window.
	 *
	 * <p>Deriving it means parsing a date and truncating it, which is by far the most
	 * expensive thing any of these pipelines asks of a document - and
	 * {@link StatisticsCube} throws away every weekly cell older than
	 * {@link StatisticsCube#WEEKS_KEPT} weeks, which on a mature database is most of the
	 * collection. So the date maths is put behind a fixed width string compare that
	 * decides, cheaply, whether the answer can survive.
	 *
	 * <p>The bound is exact rather than approximate because {@code oldestWeek} is itself a
	 * Monday: a {@code started} at or after it falls in that ISO week or a later one, and
	 * one before it falls in an earlier week, so no document that would have contributed
	 * to a retained week is gated out. Unlike a {@code $match} on {@code started}, the
	 * comparison is against the <em>converted</em> string, so a {@code started} written as
	 * a BSON date rather than as a string is not silently excluded by MongoDB bracketing
	 * its comparisons by type; a missing or unusable one converts to {@code ""}, which
	 * sorts below any date and gates to null - the same value the date maths would have
	 * produced for it.
	 *
	 * @param oldestWeek the Monday of the oldest ISO week still kept, from
	 *                   {@link StatisticsCube#oldestWeek}
	 * @return the {@code YYYY-MM-DD} Monday of the ISO week a document's {@code started}
	 *         falls in, in UTC, or null if it is outside the window or cannot be parsed as
	 *         a date. Needs a real date rather than a substring because a week straddles
	 *         months and years.
	 */
	private static Document weekExpression(String oldestWeek) {
		Document date = new Document("$dateFromString", new Document("dateString", startedAsString())
			.append("onError", null)
			.append("onNull", null));
		Document monday = new Document("$dateTrunc", new Document("date", "$$parsed")
			.append("unit", "week")
			.append("startOfWeek", "monday"));
		Document week = new Document("$dateToString", new Document("format", "%Y-%m-%d").append("date", monday));
		// parsed once, and $dateTrunc is only reached for a date that parsed: an unusable
		// started must yield null rather than depend on how $dateTrunc handles a null date,
		// because an error there would fail the whole computation
		Document parsed = new Document("$let", new Document("vars", new Document("parsed", date))
			.append("in", new Document("$cond",
				Arrays.asList(new Document("$eq", Arrays.asList("$$parsed", null)), null, week))));
		return new Document("$cond", Arrays.asList(
			new Document("$gte", Arrays.asList(startedAsString(), oldestWeek)), parsed, null));
	}

	/** @return {@code started} as a string, tolerating a missing field or a BSON date. */
	private static Document startedAsString() {
		return new Document("$convert", new Document("input", "$started")
			.append("to", "string")
			.append("onError", "")
			.append("onNull", ""));
	}

	/**
	 * @return the URL of the server under test, trimmed; the empty string if the run names
	 *         none. The candidates are normalised <em>before</em> the fallback rather than
	 *         with {@code $ifNull}, because a field that is present but blank - which the
	 *         configuration form can produce - must not stop a later field being used: a
	 *         run with an empty {@code discoveryUrl} and a real {@code issuer} names its
	 *         issuer. Anything that is not a string is skipped for the same reason.
	 */
	private static Document targetUrl() {
		Document normalise = new Document("$cond", List.of(
			new Document("$eq", List.of(new Document("$type", "$$this"), "string")),
			new Document("$trim", new Document("input", "$$this")),
			""));
		Document candidates = new Document("$map",
			new Document("input", TARGET_URL_FIELDS).append("in", normalise));
		Document set = new Document("$filter", new Document("input", candidates)
			.append("cond", new Document("$gt", List.of("$$this", ""))));
		return new Document("$ifNull", Arrays.asList(new Document("$first", set), ""));
	}

	/**
	 * @return the lower-cased host of the projected {@code url} field, or null (for a URL
	 *         of this suite's own emulated endpoints) or missing (for anything that is not
	 *         an http URL) - both of which the following {@code $match} drops
	 */
	private static Document externalHost() {
		Document match = new Document("$regexFind", new Document("input", new Document("$toLower", "$url"))
			.append("regex", HOST_OF_URL));
		Document host = new Document("$let", new Document("vars", new Document("match", match))
			.append("in", new Document("$arrayElemAt",
				List.of(new Document("$ifNull", Arrays.asList("$$match.captures", List.of())), 0))));
		Document emulated = new Document("$regexMatch",
			new Document("input", "$url").append("regex", EMULATED_SERVER_PATH));
		return new Document("$cond", Arrays.asList(emulated, null, host));
	}

	/**
	 * @param issPath the field path of the owner's issuer
	 * @param subPath the field path of the owner's subject
	 * @return an expression that is true only for a real identity - the same rule as
	 *         {@link OwnerIds#isUser}, expressed for MongoDB
	 */
	private static Document ownerIsIdentified(String issPath, String subPath) {
		return new Document("$and", List.of(isSet(issPath), isSet(subPath)));
	}

	/**
	 * @param path a field path
	 * @return an expression that is true when the field holds something. The
	 *         {@code $ifNull} is not redundant: unlike a query, an aggregation expression
	 *         does <em>not</em> treat a missing field as null, so {@code {$ne: [path,
	 *         null]}} on its own is true for a field that is not there at all.
	 */
	private static Document isSet(String path) {
		Document value = new Document("$ifNull", Arrays.asList(path, null));
		return new Document("$and", List.of(
			new Document("$ne", Arrays.asList(value, null)),
			new Document("$ne", List.of(value, ""))));
	}

	private static Document isResult(Result result) {
		return ifThenOne(new Document("$eq", List.of("$result", result.name())));
	}

	/** @return the names the statuses are stored under, for a {@code $in} */
	private static List<String> names(Status... statuses) {
		return Arrays.stream(statuses).map(Enum::name).toList();
	}

	/** @return an expression yielding 1 when {@code condition} holds and 0 otherwise. */
	private static Document ifThenOne(Document condition) {
		return new Document("$cond", List.of(condition, 1, 0));
	}

	/**
	 * @return a numeric field, whichever of the BSON number types the server reported it
	 *         in - the aggregations return Integer or Long depending on the value, and
	 *         {@code collStats} may also use a Double
	 */
	private static long count(Document document, String key) {
		Object value = document.get(key);
		return value instanceof Number number ? number.longValue() : 0;
	}

	/** @return the strings of an {@code $addToSet} result, skipping anything else. */
	private static List<String> strings(Document document, String key) {
		List<String> strings = new ArrayList<>();
		Object value = document.get(key);
		if (value instanceof List<?> list) {
			for (Object element : list) {
				if (element instanceof String string) {
					strings.add(string);
				}
			}
		}
		return strings;
	}
}
