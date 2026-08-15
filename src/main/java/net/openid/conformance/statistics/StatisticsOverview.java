package net.openid.conformance.statistics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * The payload of {@code GET /api/statistics/overview}: every chart on the admin statistics
 * page for one {@link StatisticsQuery}, sliced out of one cube so they all share one
 * {@code computedAt}.
 *
 * <p>Every per-period list has exactly {@link #periods()} entries, and every per-family map
 * has an entry for every family in {@link #families()} - zero filled where the current
 * filter excludes them - so the client never has to fill gaps or guess at an axis.
 *
 * @param periods          contiguous period keys, oldest first: {@code YYYY-MM} months or
 *                         the {@code YYYY-MM-DD} Mondays of ISO weeks
 * @param granularity      which of the two {@link #periods()} are, {@code month} or {@code week}
 * @param families         every family a series may be keyed by, in a fixed order
 * @param resultBuckets    the test result buckets, in a fixed order
 * @param testRunsByFamily family -&gt; test module runs per period
 * @param plansByFamily    family -&gt; test plans created per period
 * @param resultsByFamily  family -&gt; result bucket -&gt; runs per period
 * @param certifiedByFamily family -&gt; plans made immutable per period, i.e. certification activity
 * @param users            active and first-seen user counts per period, on a plan basis
 * @param tiles            the whole-collection summary counters; never filtered
 * @param storage          how much space each collection takes up; never filtered
 * @param dimensions       what the filter selects can offer, counted under the current query
 * @param heatmap          runs by day of the week (Monday first) and hour of the day, in
 *                         UTC: 7 rows of 24 counts, filtered by the range but not by
 *                         family, plan, variant or certification profile
 * @param externalHosts    the external servers the suite has been pointed at, all time
 * @param unresolvedPlans  the busiest plan names that could not be resolved to a family,
 *                         all time
 */
@Schema(name = "StatisticsOverview")
public record StatisticsOverview(List<String> periods, String granularity, List<String> families,
	List<String> resultBuckets, Map<String, List<Long>> testRunsByFamily, Map<String, List<Long>> plansByFamily,
	Map<String, Map<String, List<Long>>> resultsByFamily, Map<String, List<Long>> certifiedByFamily,
	Users users, Tiles tiles, List<StorageRow> storage, Dimensions dimensions, List<List<Long>> heatmap,
	List<HostRow> externalHosts, List<UnresolvedPlan> unresolvedPlans) {

	/**
	 * Users on a plan basis: a user is active in the period they created a test plan, which
	 * is what lets these two series be filtered like every other one.
	 *
	 * @param activeByPeriod users who created at least one matching plan in the period
	 * @param newByPeriod    users whose first matching plan was in the period
	 */
	@Schema(name = "StatisticsUsers")
	public record Users(List<Long> activeByPeriod, List<Long> newByPeriod) {
	}

	/**
	 * @param totalTests     all test runs ever recorded
	 * @param totalPlans     all test plans ever created
	 * @param totalUsers     all users who ever ran a test
	 * @param testsLast24h   runs started in the last 24 hours
	 * @param testsLast7d    runs started in the last 7 days
	 * @param testsLast30d   runs started in the last 30 days
	 * @param inProgress     runs currently RUNNING or WAITING
	 * @param stuck          non-terminal runs that started more than 24 hours ago
	 * @param certifiedPlans plans that were made immutable (certification submissions)
	 * @param publishedPlans plans that were published
	 */
	@Schema(name = "StatisticsTiles")
	public record Tiles(long totalTests, long totalPlans, long totalUsers, long testsLast24h, long testsLast7d,
		long testsLast30d, long inProgress, long stuck, long certifiedPlans, long publishedPlans) {
	}

	/**
	 * The values the filter selects can offer, and how much data is behind each of them,
	 * counted under the query that produced this payload - so drilling into a family leaves
	 * only that family's plans, variants and certification profiles to choose from.
	 *
	 * @param plans        the plans with data, busiest first
	 * @param variants     variant parameter name -&gt; its values, most used first
	 * @param certProfiles the certification profiles with data, most used first
	 * @param entities     what was under test, busiest first
	 */
	@Schema(name = "StatisticsDimensions")
	public record Dimensions(List<PlanDimension> plans, Map<String, List<VariantValue>> variants,
		List<CertProfile> certProfiles, List<Entity> entities) {
	}

	/**
	 * @param planName the plan name as stored on the plan documents
	 * @param family   the spec family it belongs to
	 * @param runs     test module runs recorded against it
	 * @param plans    test plans created from it
	 */
	@Schema(name = "StatisticsPlanDimension")
	public record PlanDimension(String planName, String family, long runs, long plans) {
	}

	/**
	 * @param value the value of the variant parameter
	 * @param users distinct users who created a plan with it - counted once per user, however
	 *              many plans they ran with it
	 * @param plans test plans created with it
	 */
	@Schema(name = "StatisticsVariantValue")
	public record VariantValue(String value, long users, long plans) {
	}

	/**
	 * @param name  the certification profile names of the plan, joined with {@code  | }
	 * @param users distinct users who created a plan for it
	 * @param plans test plans created for it
	 */
	@Schema(name = "StatisticsCertProfile")
	public record CertProfile(String name, long users, long plans) {
	}

	/**
	 * @param entity what the plan tests, e.g. "Test an OpenID Provider / Authorization Server"
	 * @param runs   test module runs recorded against plans that test it
	 */
	@Schema(name = "StatisticsEntity")
	public record Entity(String entity, long runs) {
	}

	/**
	 * A plan name that fell into {@link SpecFamilyResolver#OTHER_RETIRED}, so an admin can
	 * see what a future alias map would need to cover.
	 *
	 * @param planName the unresolved plan name
	 * @param runs     test module runs recorded against it
	 */
	@Schema(name = "StatisticsUnresolvedPlan")
	public record UnresolvedPlan(String planName, long runs) {
	}
}
