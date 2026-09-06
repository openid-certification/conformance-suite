package net.openid.conformance.statistics;

import net.openid.conformance.statistics.StatisticsOverview.Users;
import net.openid.conformance.testmodule.TestModule.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * leaves too little in each of its 168 cells to read. The range is the one asked for, not
 * the axis the charts end up with: an open-ended axis is trimmed to where the filtered
 * series start, and neither the heatmap nor the filter choices follow that trim, or a
 * filter would narrow them through the back door;</li>
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

		Users users = users(cube, filter, granularity, index, periods.size());
		// An open-ended range starts where the SLICE starts, not where the cube does: a
		// family that first ran in 2025 charted against an axis reaching back to 2019 is
		// six years of empty bars in front of the data. A range with a from is what was
		// asked for and is left alone, empty leading periods and all. So is a slice with
		// nothing in it: the whole axis of zeros tells the client the filter matched
		// nothing, where an empty axis would say the range covers no data at all.
		int first = query.from() == null
			? firstPeriodWithData(periods.size(), runsByFamily, plansByFamily, certifiedByFamily, users) : 0;
		if (first == periods.size()) {
			first = 0;
		}
		List<String> shown = periods.subList(first, periods.size());
		Set<String> range = Set.copyOf(periods);

		return new StatisticsOverview(shown, granularity.key(), families, SpecFamilyResolver.SYNTHETIC_FAMILIES,
			RESULT_BUCKETS, freeze(runsByFamily, first), freeze(plansByFamily, first),
			freezeBuckets(resultsByFamily, first), freeze(certifiedByFamily, first), cube.familyTotals(),
			new Users(users.activeByPeriod().subList(first, periods.size()),
				users.newByPeriod().subList(first, periods.size())),
			cube.summaryTiles(), cube.storage(),
			DimensionCounter.count(cube, query, granularity, range),
			HeatmapBinner.heatmap(cube.heat(), granularity, range), ModuleRanker.rank(cube, query),
			cube.externalHosts(), cube.unresolvedPlans());
	}

	/**
	 * @return the index of the first period any series has something in, or {@code size}
	 *         if none has. The users series count too, so a plan created before the first
	 *         run of a family still opens its axis.
	 */
	private static int firstPeriodWithData(int size, Map<String, long[]> runs, Map<String, long[]> plans,
			Map<String, long[]> certified, Users users) {
		int first = size;
		for (Map<String, long[]> series : List.of(runs, plans, certified)) {
			for (long[] values : series.values()) {
				first = Math.min(first, firstNonZero(values, first));
			}
		}
		for (List<Long> values : List.of(users.activeByPeriod(), users.newByPeriod())) {
			first = Math.min(first, firstNonZero(values, first));
		}
		return first;
	}

	private static int firstNonZero(long[] values, int limit) {
		for (int at = 0; at < limit; at++) {
			if (values[at] != 0) {
				return at;
			}
		}
		return limit;
	}

	private static int firstNonZero(List<Long> values, int limit) {
		for (int at = 0; at < limit; at++) {
			if (values.get(at) != 0) {
				return at;
			}
		}
		return limit;
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

	/** @return the counters from {@code first} on, boxed and frozen */
	private static Map<String, List<Long>> freeze(Map<String, long[]> counters, int first) {
		Map<String, List<Long>> frozen = new LinkedHashMap<>();
		counters.forEach((family, values) -> frozen.put(family, boxed(Arrays.copyOfRange(values, first, values.length))));
		return Collections.unmodifiableMap(frozen);
	}

	private static Map<String, Map<String, List<Long>>> freezeBuckets(Map<String, Map<String, long[]>> counters,
			int first) {
		Map<String, Map<String, List<Long>>> frozen = new LinkedHashMap<>();
		counters.forEach((family, buckets) -> frozen.put(family, freeze(buckets, first)));
		return Collections.unmodifiableMap(frozen);
	}
}
