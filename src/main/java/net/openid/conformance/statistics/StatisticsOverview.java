package net.openid.conformance.statistics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * The payload of {@code GET /api/statistics/overview}: every chart on the admin
 * statistics page, computed in one pass so they all share one {@code computedAt}.
 *
 * <p>Every per-month list has exactly {@link #months()} entries, and every per-family map
 * has an entry for every family in {@link #families()}, so the client never has to fill
 * gaps.
 *
 * @param families         every family a series may be keyed by, in a fixed order
 * @param resultBuckets    the test result buckets, in a fixed order
 * @param months           contiguous {@code YYYY-MM} keys, oldest first
 * @param testRunsByFamily family -&gt; test module runs per month
 * @param plansByFamily    family -&gt; test plans created per month
 * @param resultsByFamily  family -&gt; result bucket -&gt; runs per month
 * @param users            active and first-seen user counts per month
 * @param tiles            the whole-collection summary counters
 * @param unresolvedPlans  the busiest plan names that could not be resolved to a family
 */
@Schema(name = "StatisticsOverview")
public record StatisticsOverview(List<String> families, List<String> resultBuckets, List<String> months,
	Map<String, List<Long>> testRunsByFamily, Map<String, List<Long>> plansByFamily,
	Map<String, Map<String, List<Long>>> resultsByFamily, Users users, Tiles tiles,
	List<UnresolvedPlan> unresolvedPlans) {

	/**
	 * @param activeByMonth users who ran at least one test in the month
	 * @param newByMonth    users whose first-ever test run was in the month
	 */
	@Schema(name = "StatisticsUsers")
	public record Users(List<Long> activeByMonth, List<Long> newByMonth) {
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
	 */
	@Schema(name = "StatisticsTiles")
	public record Tiles(long totalTests, long totalPlans, long totalUsers, long testsLast24h, long testsLast7d, long testsLast30d, long inProgress, long stuck, long certifiedPlans) {
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
