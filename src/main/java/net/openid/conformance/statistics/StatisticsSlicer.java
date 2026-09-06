package net.openid.conformance.statistics;

import net.openid.conformance.statistics.StatisticsOverview.Tiles;
import net.openid.conformance.statistics.StatisticsOverview.UnresolvedPlan;
import net.openid.conformance.statistics.StatisticsOverview.Users;
import net.openid.conformance.testmodule.TestModule.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Answers one {@link StatisticsQuery} from a {@link StatisticsCube}: walks the cells the
 * query selects, adds them up into the series the client draws, and zero fills everything
 * else. Pure and cheap enough to run on the request thread - the expensive part was
 * building the cube.
 *
 * <p>What the query does <em>not</em> touch is deliberate:
 *
 * <ul>
 * <li>the summary tiles and the storage counters describe the whole database, not the
 * slice;</li>
 * <li>the heatmap is clipped to the range but not to the family, plan, variant or
 * certification profile - it answers "when do people run tests", and splitting it further
 * leaves too little in each of its 168 cells to read;</li>
 * <li>the modules table is clipped to the range, by month, and filtered by family and
 * plan, but not by variant or certification profile: a test run records neither in a form
 * the module cells carry - see {@link ModuleRanker};</li>
 * <li>the unresolved plan names are all time, and the external hosts cover the trailing
 * {@value StatisticsCube#MODULE_MONTHS} months, so that narrowing the range does not hide
 * the diagnostics an admin came for.</li>
 * </ul>
 */
public final class StatisticsSlicer {

	private static final String PASSED = Result.PASSED.name();

	private static final String WARNING = Result.WARNING.name();

	private static final String REVIEW = Result.REVIEW.name();

	private static final String FAILED = Result.FAILED.name();

	private static final String SKIPPED = Result.SKIPPED.name();

	/** The one bucket that is not a {@link Result}: runs that never reached a result at all. */
	private static final String NEVER_FINISHED = "NEVER_FINISHED";

	/** The result buckets of the stacked results chart, in rendering order. */
	public static final List<String> RESULT_BUCKETS = List.of(PASSED, WARNING, REVIEW, FAILED, SKIPPED, NEVER_FINISHED);

	/** How many unresolved plan names to report. */
	private static final int MAX_UNRESOLVED_PLANS = 20;

	private StatisticsSlicer() {
	}

	/**
	 * @param cube  the cube to slice
	 * @param query what to show
	 * @return the payload; every list is aligned to {@link StatisticsOverview#periods()}
	 */
	public static StatisticsOverview slice(StatisticsCube cube, StatisticsQuery query) {
		Granularity granularity = query.granularity();
		List<String> periods = periods(cube, query);
		Set<String> onTheAxis = Set.copyOf(periods);
		Map<String, Integer> index = index(periods);
		List<String> families = cube.familyOrder();
		CellFilter filter = new CellFilter(cube, query);

		Map<String, long[]> runsByFamily = counters(families, periods.size());
		Map<String, Map<String, long[]>> resultsByFamily = bucketCounters(families, periods.size());
		for (RunCell cell : cube.runs(granularity)) {
			Integer at = index.get(cell.period(granularity));
			if (at == null || !filter.matches(cell)) {
				continue;
			}
			String family = filter.familyOf(cell);
			runsByFamily.get(family)[at] += cell.runs();
			Map<String, long[]> buckets = resultsByFamily.get(family);
			buckets.get(PASSED)[at] += cell.passed();
			buckets.get(WARNING)[at] += cell.warning();
			buckets.get(REVIEW)[at] += cell.review();
			buckets.get(FAILED)[at] += cell.failed();
			buckets.get(SKIPPED)[at] += cell.skipped();
			long finished = cell.passed() + cell.warning() + cell.review() + cell.failed() + cell.skipped();
			buckets.get(NEVER_FINISHED)[at] += Math.max(0, cell.runs() - finished);
		}

		Map<String, long[]> plansByFamily = counters(families, periods.size());
		Map<String, long[]> certifiedByFamily = counters(families, periods.size());
		for (PlanCell cell : cube.plans(granularity)) {
			Integer at = index.get(cell.period(granularity));
			if (at == null || !filter.matches(cell)) {
				continue;
			}
			String family = cube.familyOf(cell.planName());
			plansByFamily.get(family)[at] += cell.plans();
			certifiedByFamily.get(family)[at] += cell.certified();
		}

		return new StatisticsOverview(periods, granularity.key(), families, SpecFamilyResolver.SYNTHETIC_FAMILIES,
			RESULT_BUCKETS, freeze(runsByFamily), freeze(plansByFamily), freezeBuckets(resultsByFamily),
			freeze(certifiedByFamily), cube.familyTotals(),
			users(cube, filter, granularity, index, periods.size()), tiles(cube), cube.storage(),
			DimensionCounter.count(cube, query, granularity, onTheAxis),
			HeatmapBinner.heatmap(cube.heat(), granularity, onTheAxis), ModuleRanker.rank(cube, query),
			cube.externalHosts(), unresolvedPlans(cube));
	}

	/** @return the cube's axis clipped to the query's range; both ends are inclusive */
	private static List<String> periods(StatisticsCube cube, StatisticsQuery query) {
		List<String> periods = new ArrayList<>();
		for (String period : cube.periods(query.granularity())) {
			boolean afterFrom = query.from() == null || period.compareTo(query.from()) >= 0;
			boolean beforeTo = query.to() == null || period.compareTo(query.to()) <= 0;
			if (afterFrom && beforeTo) {
				periods.add(period);
			}
		}
		return List.copyOf(periods);
	}

	/**
	 * Users on a plan basis: active in every period they created a matching plan in, new in
	 * the first period any of their matching tuples mentions.
	 *
	 * <p>The weekly window would make long standing users look new the moment their oldest
	 * retained week enters the axis, so a user whose first <em>month</em> is earlier than the
	 * month their first retained week falls in is never counted as new in the weekly view.
	 */
	private static Users users(StatisticsCube cube, CellFilter filter, Granularity granularity,
			Map<String, Integer> index, int size) {
		List<Set<Integer>> active = new ArrayList<>(size);
		for (int period = 0; period < size; period++) {
			active.add(new HashSet<>());
		}
		Map<Integer, String> firstPeriod = new HashMap<>();
		Map<Integer, String> firstMonth = new HashMap<>();
		for (UserTuple tuple : cube.users()) {
			if (!filter.matches(tuple)) {
				continue;
			}
			for (String period : tuple.periods(granularity)) {
				Integer at = index.get(period);
				if (at != null) {
					active.get(at).add(tuple.ownerId());
				}
				firstPeriod.merge(tuple.ownerId(), period, StatisticsSlicer::earlier);
			}
			for (String month : tuple.months()) {
				firstMonth.merge(tuple.ownerId(), month, StatisticsSlicer::earlier);
			}
		}

		long[] newUsers = new long[size];
		firstPeriod.forEach((ownerId, period) -> {
			Integer at = index.get(period);
			if (at != null && !activeBeforeTheWindow(granularity, period, firstMonth.get(ownerId))) {
				newUsers[at]++;
			}
		});
		return new Users(active.stream().map(owners -> (long) owners.size()).toList(), boxed(newUsers));
	}

	private static boolean activeBeforeTheWindow(Granularity granularity, String firstWeek, String firstMonth) {
		return granularity == Granularity.WEEK && firstMonth != null
			&& firstMonth.compareTo(firstWeek.substring(0, "YYYY-MM".length())) < 0;
	}

	private static String earlier(String period, String candidate) {
		return candidate.compareTo(period) < 0 ? candidate : period;
	}

	/** @return the whole-database tiles; plan counts include periods outside the axis */
	private static Tiles tiles(StatisticsCube cube) {
		long plans = 0;
		long certified = 0;
		long published = 0;
		for (PlanCell cell : cube.plans(Granularity.MONTH)) {
			plans += cell.plans();
			certified += cell.certified();
			published += cell.published();
		}
		TileRow tiles = cube.tiles();
		return new Tiles(tiles.total(), plans, tiles.totalUsers(), tiles.last24h(), tiles.last7d(), tiles.last30d(),
			tiles.inProgress(), tiles.stuck(), certified, published);
	}

	/**
	 * @return the busiest plan names the statistics cannot attribute to a family, all time and
	 *         unfiltered. A retired plan name that {@link SpecFamilyResolver}'s alias map knows
	 *         resolves to its family and so is not listed here: what is left is what nothing in
	 *         the suite, current or historic, can name.
	 */
	private static List<UnresolvedPlan> unresolvedPlans(StatisticsCube cube) {
		Map<String, Long> runs = new HashMap<>();
		for (RunCell cell : cube.runs(Granularity.MONTH)) {
			if (cell.standalone() || cell.planName() == null
				|| !SpecFamilyResolver.OTHER_RETIRED.equals(cube.familyOf(cell.planName()))) {
				continue;
			}
			runs.merge(cell.planName(), cell.runs(), Long::sum);
		}
		return runs.entrySet().stream()
			.map(entry -> new UnresolvedPlan(entry.getKey(), entry.getValue()))
			.sorted(Comparator.comparingLong(UnresolvedPlan::runs).reversed()
				.thenComparing(UnresolvedPlan::planName))
			.limit(MAX_UNRESOLVED_PLANS)
			.toList();
	}

	private static Map<String, Integer> index(List<String> periods) {
		Map<String, Integer> index = new HashMap<>();
		for (int at = 0; at < periods.size(); at++) {
			index.put(periods.get(at), at);
		}
		return index;
	}

	private static Map<String, long[]> counters(List<String> families, int size) {
		Map<String, long[]> counters = new LinkedHashMap<>();
		for (String family : families) {
			counters.put(family, new long[size]);
		}
		return counters;
	}

	private static Map<String, Map<String, long[]>> bucketCounters(List<String> families, int size) {
		Map<String, Map<String, long[]>> counters = new LinkedHashMap<>();
		for (String family : families) {
			counters.put(family, counters(RESULT_BUCKETS, size));
		}
		return counters;
	}

	private static List<Long> boxed(long... counters) {
		return Arrays.stream(counters).boxed().toList();
	}

	private static Map<String, List<Long>> freeze(Map<String, long[]> counters) {
		Map<String, List<Long>> frozen = new LinkedHashMap<>();
		counters.forEach((family, values) -> frozen.put(family, boxed(values)));
		return Collections.unmodifiableMap(frozen);
	}

	private static Map<String, Map<String, List<Long>>> freezeBuckets(Map<String, Map<String, long[]>> counters) {
		Map<String, Map<String, List<Long>>> frozen = new LinkedHashMap<>();
		counters.forEach((family, buckets) -> frozen.put(family, freeze(buckets)));
		return Collections.unmodifiableMap(frozen);
	}
}
