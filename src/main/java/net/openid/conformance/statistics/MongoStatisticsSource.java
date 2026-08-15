package net.openid.conformance.statistics;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import net.openid.conformance.info.DBTestInfoService;
import net.openid.conformance.info.DBTestPlanService;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs the whole-collection aggregations behind the admin statistics page - the only
 * class in this package that talks to MongoDB.
 *
 * <p>These pipelines group over every document in {@code TEST_INFO} / {@code TEST_PLAN}
 * and cannot use an index, so they are expensive by construction and must never run on a
 * request thread; {@link DBStatisticsService} runs them on a background thread at most
 * once per TTL.
 *
 * <p>Deliberately not built on {@code DBTestInfoService} / {@code DBTestPlanService}:
 * those scope every query to the authenticated user, and this computation runs with no
 * security context and has to see every user's data.
 */
@Component
public class MongoStatisticsSource {

	/** Identifies these aggregations in {@code currentOp} and the Mongo slow-query log. */
	private static final String COMMENT = "statistics-overview";

	private static final long MAX_TIME_MINUTES = 10;

	private static final List<String> IN_PROGRESS_STATUSES = List.of("RUNNING", "WAITING");

	private static final List<String> NON_TERMINAL_STATUSES = List.of("CREATED", "CONFIGURED", "RUNNING", "WAITING");

	@Autowired
	private MongoTemplate mongoTemplate;

	/**
	 * Module runs and their result buckets, per month and test plan.
	 *
	 * <p>Grouped by plan id first (one bucket per plan ever run, plus one standalone
	 * bucket per month), then joined to {@code TEST_PLAN} by {@code _id} to pick up the
	 * plan name - an index seek per bucket, rather than pulling every plan document over
	 * to Java, and carrying nothing but the plan's name out of the join (see
	 * {@link #planNameLookup()}) - then projected down immediately so only the counters
	 * and the plan name reach the second group.
	 *
	 * @return one row per month, plan name and standalone-ness
	 */
	public List<RunsRow> runsByMonthAndPlan() {
		Document runsByPlanId = new Document("month", monthExpression())
			// null for a standalone run; explicit so it survives as a group key
			.append("planId", new Document("$ifNull", Arrays.asList("$planId", null)));
		List<Bson> pipeline = List.of(
			Aggregates.group(runsByPlanId,
				Accumulators.sum("runs", 1),
				Accumulators.sum("passed", isResult("PASSED")),
				Accumulators.sum("failed", isResult("FAILED")),
				Accumulators.sum("warning", isResult("WARNING")),
				Accumulators.sum("review", isResult("REVIEW")),
				Accumulators.sum("skipped", isResult("SKIPPED"))),
			planNameLookup(),
			Aggregates.project(Projections.fields(
				Projections.excludeId(),
				Projections.computed("month", "$_id.month"),
				Projections.computed("standalone", new Document("$eq", Arrays.asList("$_id.planId", null))),
				// missing if the plan document has been deleted since the run
				Projections.computed("planName", new Document("$arrayElemAt", List.of("$plan.planName", 0))),
				Projections.include("runs", "passed", "failed", "warning", "review", "skipped"))),
			Aggregates.group(new Document("month", "$month")
					.append("planName", "$planName")
					.append("standalone", "$standalone"),
				Accumulators.sum("runs", "$runs"),
				Accumulators.sum("passed", "$passed"),
				Accumulators.sum("failed", "$failed"),
				Accumulators.sum("warning", "$warning"),
				Accumulators.sum("review", "$review"),
				Accumulators.sum("skipped", "$skipped")));

		List<RunsRow> rows = new ArrayList<>();
		for (Document document : aggregate(DBTestInfoService.COLLECTION, pipeline).into(new ArrayList<>())) {
			Document id = document.get("_id", Document.class);
			rows.add(new RunsRow(id.getString("month"), id.getString("planName"),
				Boolean.TRUE.equals(id.getBoolean("standalone")),
				count(document, "runs"), count(document, "passed"), count(document, "failed"),
				count(document, "warning"), count(document, "review"), count(document, "skipped")));
		}
		return rows;
	}

	/**
	 * Test plans created per month and plan name, and how many of them were made
	 * immutable - which is what downloading a certification package does.
	 *
	 * @return one row per month and plan name
	 */
	public List<PlanRow> plansByMonthAndName() {
		List<Bson> pipeline = List.of(
			Aggregates.group(new Document("month", monthExpression()).append("planName", "$planName"),
				Accumulators.sum("plans", 1),
				Accumulators.sum("certified", ifThenOne(new Document("$eq", List.of("$immutable", true))))));

		List<PlanRow> rows = new ArrayList<>();
		for (Document document : aggregate(DBTestPlanService.COLLECTION, pipeline).into(new ArrayList<>())) {
			Document id = document.get("_id", Document.class);
			rows.add(new PlanRow(id.getString("month"), id.getString("planName"),
				count(document, "plans"), count(document, "certified")));
		}
		return rows;
	}

	/**
	 * The months each user ran at least one test in. Running a test is the activity signal
	 * available - logins are not recorded.
	 *
	 * @return one row per user; only one row per user crosses the wire
	 */
	public List<UserRow> usersByMonth() {
		List<Bson> pipeline = List.of(
			Aggregates.group(new Document("month", monthExpression())
				.append("iss", "$owner.iss")
				.append("sub", "$owner.sub")),
			Aggregates.group(new Document("iss", "$_id.iss").append("sub", "$_id.sub"),
				Accumulators.addToSet("months", "$_id.month")));

		List<UserRow> rows = new ArrayList<>();
		for (Document document : aggregate(DBTestInfoService.COLLECTION, pipeline).into(new ArrayList<>())) {
			Document id = document.get("_id", Document.class);
			rows.add(new UserRow(id.getString("iss"), id.getString("sub"), months(document)));
		}
		return rows;
	}

	/**
	 * The summary tile counters, in one pass over {@code TEST_INFO}. {@code started} is
	 * stored as an ISO-8601 UTC string, so the recency cutoffs are string comparisons.
	 *
	 * @param now the instant the "last 24 hours / 7 days / 30 days" windows end at
	 * @return the counters; all zero if the collection is empty
	 */
	public TileRow tiles(Instant now) {
		String cutoff24h = now.minus(Duration.ofHours(24)).toString();
		String cutoff7d = now.minus(Duration.ofDays(7)).toString();
		String cutoff30d = now.minus(Duration.ofDays(30)).toString();
		Document started = startedAsString();

		List<Bson> pipeline = List.of(
			Aggregates.group(null,
				Accumulators.sum("total", 1),
				Accumulators.sum("last24h", ifThenOne(new Document("$gte", List.of(started, cutoff24h)))),
				Accumulators.sum("last7d", ifThenOne(new Document("$gte", List.of(started, cutoff7d)))),
				Accumulators.sum("last30d", ifThenOne(new Document("$gte", List.of(started, cutoff30d)))),
				Accumulators.sum("inProgress", ifThenOne(new Document("$in", List.of("$status", IN_PROGRESS_STATUSES)))),
				// non-terminal but too old to still be making progress
				Accumulators.sum("stuck", ifThenOne(new Document("$and", List.of(
					new Document("$in", List.of("$status", NON_TERMINAL_STATUSES)),
					new Document("$lt", List.of(started, cutoff24h))))))));

		List<Document> results = aggregate(DBTestInfoService.COLLECTION, pipeline).into(new ArrayList<>());
		if (results.isEmpty()) {
			return new TileRow(0, 0, 0, 0, 0, 0);
		}
		Document document = results.get(0);
		return new TileRow(count(document, "total"), count(document, "last24h"), count(document, "last7d"),
			count(document, "last30d"), count(document, "inProgress"), count(document, "stuck"));
	}

	/**
	 * The plan-name join used by {@link #runsByMonthAndPlan()}, in the concise correlated
	 * form MongoDB has supported since 5.0 (the suite targets FCV 6.0): the equality match
	 * on {@code TEST_PLAN._id} still uses the primary key index, but the inner pipeline
	 * projects every matched plan down to its name <em>inside the server</em>, so the
	 * plans' {@code config} blobs are never materialised into the {@code plan} array.
	 * Without it the join streams whole plan documents - configuration included - through
	 * one bucket per plan ever run, which on production-scale data is most of the
	 * pipeline's memory and network cost.
	 *
	 * <p>Built as a raw stage because the driver has no {@code Aggregates.lookup} overload
	 * taking {@code localField} / {@code foreignField} <em>and</em> a pipeline.
	 *
	 * @return the {@code $lookup} stage yielding {@code plan: [{planName}]}
	 */
	private static Document planNameLookup() {
		return new Document("$lookup", new Document("from", DBTestPlanService.COLLECTION)
			.append("localField", "_id.planId")
			.append("foreignField", "_id")
			.append("pipeline", List.of(new Document("$project", new Document("_id", 0).append("planName", 1))))
			.append("as", "plan"));
	}

	private AggregateIterable<Document> aggregate(String collection, List<Bson> pipeline) {
		return mongoTemplate.getCollection(collection)
			.aggregate(pipeline)
			.allowDiskUse(true)
			.maxTime(MAX_TIME_MINUTES, TimeUnit.MINUTES)
			.comment(COMMENT);
	}

	/**
	 * @return the {@code YYYY-MM} key of a document's {@code started} field; the empty
	 *         string - which the assembler drops - if it is missing or not string-like
	 */
	private static Document monthExpression() {
		return new Document("$substrBytes", List.of(startedAsString(), 0, 7));
	}

	/** @return {@code started} as a string, tolerating a missing field or a BSON date. */
	private static Document startedAsString() {
		return new Document("$convert", new Document("input", "$started")
			.append("to", "string")
			.append("onError", "")
			.append("onNull", ""));
	}

	private static Document isResult(String result) {
		return ifThenOne(new Document("$eq", List.of("$result", result)));
	}

	/** @return an expression yielding 1 when {@code condition} holds and 0 otherwise. */
	private static Document ifThenOne(Document condition) {
		return new Document("$cond", List.of(condition, 1, 0));
	}

	/** @return the counter, tolerating the driver returning an Integer or a Long. */
	private static long count(Document document, String key) {
		Object value = document.get(key);
		return value instanceof Number number ? number.longValue() : 0;
	}

	private static List<String> months(Document document) {
		List<String> months = new ArrayList<>();
		Object value = document.get("months");
		if (value instanceof List<?> list) {
			for (Object month : list) {
				if (month instanceof String string) {
					months.add(string);
				}
			}
		}
		return months;
	}
}
