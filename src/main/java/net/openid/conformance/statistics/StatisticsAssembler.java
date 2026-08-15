package net.openid.conformance.statistics;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Turns the raw monthly aggregation rows into the {@link StatisticsOverview} the admin
 * statistics page renders. Pure: no Mongo, no clock of its own - the caller passes the
 * current UTC month.
 */
public class StatisticsAssembler {

	private static final String PASSED = "PASSED";
	private static final String WARNING = "WARNING";
	private static final String REVIEW = "REVIEW";
	private static final String FAILED = "FAILED";
	private static final String SKIPPED = "SKIPPED";
	private static final String NEVER_FINISHED = "NEVER_FINISHED";

	/** The result buckets of the stacked results chart, in rendering order. */
	public static final List<String> RESULT_BUCKETS = List.of(PASSED, WARNING, REVIEW, FAILED, SKIPPED, NEVER_FINISHED);

	/** How many unresolved plan names to report. */
	private static final int MAX_UNRESOLVED_PLANS = 20;

	private static final Pattern MONTH_KEY = Pattern.compile("\\d{4}-\\d{2}");

	private final SpecFamilyResolver resolver;

	public StatisticsAssembler(SpecFamilyResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * @param runs   monthly module runs per plan
	 * @param plans  monthly plans created per plan name
	 * @param users  the months each user was active in
	 * @param tiles  the whole-collection counters
	 * @param nowUtc the month the month axis runs up to (inclusive)
	 * @return the assembled payload; every list is aligned to {@link StatisticsOverview#months()}
	 */
	public StatisticsOverview assemble(List<RunsRow> runs, List<PlanRow> plans, List<UserRow> users, TileRow tiles, YearMonth nowUtc) {
		List<String> families = resolver.familyOrder();
		List<String> months = monthAxis(runs, plans, users, nowUtc);
		Map<String, Integer> monthIndex = indexMonths(months);

		Map<String, long[]> runsByFamily = counters(families, months.size());
		Map<String, long[]> plansByFamily = counters(families, months.size());
		Map<String, Map<String, long[]>> resultsByFamily = bucketCounters(families, months.size());
		Map<String, Long> unresolvedRuns = new HashMap<>();

		for (RunsRow row : runs) {
			String family = familyFor(row.planName(), row.standalone());
			if (SpecFamilyResolver.OTHER_RETIRED.equals(family) && row.planName() != null) {
				// count these regardless of the month key, so a plan with unparseable
				// rows still shows up in the list of names an alias map should cover
				unresolvedRuns.merge(row.planName(), row.runs(), Long::sum);
			}
			// rows outside the axis (a month in the future) are dropped
			Integer index = monthIndex.get(row.month());
			if (index == null) {
				continue;
			}
			runsByFamily.get(family)[index] += row.runs();
			Map<String, long[]> buckets = resultsByFamily.get(family);
			buckets.get(PASSED)[index] += row.passed();
			buckets.get(WARNING)[index] += row.warning();
			buckets.get(REVIEW)[index] += row.review();
			buckets.get(FAILED)[index] += row.failed();
			buckets.get(SKIPPED)[index] += row.skipped();
			long finished = row.passed() + row.warning() + row.review() + row.failed() + row.skipped();
			buckets.get(NEVER_FINISHED)[index] += Math.max(0, row.runs() - finished);
		}

		// the plan tiles are whole-collection totals, so they count every row, including
		// rows the month axis dropped
		long totalPlans = 0;
		long certifiedPlans = 0;
		for (PlanRow row : plans) {
			totalPlans += row.plans();
			certifiedPlans += row.certified();
			Integer index = monthIndex.get(row.month());
			if (index == null) {
				continue;
			}
			plansByFamily.get(familyFor(row.planName(), false))[index] += row.plans();
		}

		long[] activeUsers = new long[months.size()];
		long[] newUsers = new long[months.size()];
		long totalUsers = 0;
		for (UserRow row : users) {
			if (!hasIdentity(row)) {
				continue;
			}
			totalUsers++;
			String firstMonth = null;
			for (String month : row.months()) {
				if (parseMonth(month) == null) {
					continue;
				}
				if (firstMonth == null || month.compareTo(firstMonth) < 0) {
					firstMonth = month;
				}
				Integer index = monthIndex.get(month);
				if (index != null) {
					activeUsers[index]++;
				}
			}
			Integer firstIndex = firstMonth == null ? null : monthIndex.get(firstMonth);
			if (firstIndex != null) {
				newUsers[firstIndex]++;
			}
		}

		StatisticsOverview.Tiles assembledTiles = new StatisticsOverview.Tiles(tiles.total(), totalPlans, totalUsers,
			tiles.last24h(), tiles.last7d(), tiles.last30d(), tiles.inProgress(), tiles.stuck(), certifiedPlans);

		return new StatisticsOverview(families, RESULT_BUCKETS, months,
			freeze(runsByFamily), freeze(plansByFamily), freezeBuckets(resultsByFamily),
			new StatisticsOverview.Users(Arrays.stream(activeUsers).boxed().toList(), Arrays.stream(newUsers).boxed().toList()),
			assembledTiles, topUnresolvedPlans(unresolvedRuns));
	}

	private String familyFor(String planName, boolean standalone) {
		if (standalone) {
			return SpecFamilyResolver.NO_PLAN;
		}
		if (!resolver.isKnownPlan(planName)) {
			return SpecFamilyResolver.OTHER_RETIRED;
		}
		return resolver.familyForPlan(planName);
	}

	/** @return contiguous month keys from the earliest month any row mentions to {@code nowUtc}. */
	private static List<String> monthAxis(List<RunsRow> runs, List<PlanRow> plans, List<UserRow> users, YearMonth nowUtc) {
		YearMonth earliest = null;
		for (RunsRow row : runs) {
			earliest = earliest(earliest, parseMonth(row.month()));
		}
		for (PlanRow row : plans) {
			earliest = earliest(earliest, parseMonth(row.month()));
		}
		for (UserRow row : users) {
			if (!hasIdentity(row)) {
				continue;
			}
			for (String month : row.months()) {
				earliest = earliest(earliest, parseMonth(month));
			}
		}
		if (earliest == null) {
			return List.of();
		}
		List<String> months = new ArrayList<>();
		for (YearMonth month = earliest; !month.isAfter(nowUtc); month = month.plusMonths(1)) {
			months.add(month.toString());
		}
		return List.copyOf(months);
	}

	private static YearMonth earliest(YearMonth current, YearMonth candidate) {
		if (candidate == null) {
			return current;
		}
		return current == null || candidate.isBefore(current) ? candidate : current;
	}

	/** @return the parsed month, or null if {@code key} is not a usable {@code YYYY-MM} key. */
	private static YearMonth parseMonth(String key) {
		if (key == null || !MONTH_KEY.matcher(key).matches()) {
			return null;
		}
		try {
			return YearMonth.parse(key);
		} catch (DateTimeParseException e) {
			return null; // digits in the right shape but not a real month, e.g. "2026-13"
		}
	}

	private static boolean hasIdentity(UserRow row) {
		return row.iss() != null && !row.iss().isBlank() && row.sub() != null && !row.sub().isBlank();
	}

	private static Map<String, Integer> indexMonths(List<String> months) {
		Map<String, Integer> index = new HashMap<>();
		for (int i = 0; i < months.size(); i++) {
			index.put(months.get(i), i);
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

	private static List<StatisticsOverview.UnresolvedPlan> topUnresolvedPlans(Map<String, Long> unresolvedRuns) {
		return unresolvedRuns.entrySet().stream()
			.map(entry -> new StatisticsOverview.UnresolvedPlan(entry.getKey(), entry.getValue()))
			.sorted(Comparator.comparingLong(StatisticsOverview.UnresolvedPlan::runs).reversed()
				.thenComparing(StatisticsOverview.UnresolvedPlan::planName))
			.limit(MAX_UNRESOLVED_PLANS)
			.toList();
	}

	private static Map<String, List<Long>> freeze(Map<String, long[]> counters) {
		Map<String, List<Long>> frozen = new LinkedHashMap<>();
		counters.forEach((family, values) -> frozen.put(family, Arrays.stream(values).boxed().toList()));
		return Collections.unmodifiableMap(frozen);
	}

	private static Map<String, Map<String, List<Long>>> freezeBuckets(Map<String, Map<String, long[]>> counters) {
		Map<String, Map<String, List<Long>>> frozen = new LinkedHashMap<>();
		counters.forEach((family, buckets) -> frozen.put(family, freeze(buckets)));
		return Collections.unmodifiableMap(frozen);
	}
}
