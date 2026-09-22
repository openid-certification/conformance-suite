package net.openid.conformance.statistics;

import net.openid.conformance.statistics.StatisticsOverview.TopUser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ranks the users of a slice by how many test modules they ran.
 *
 * <p>Counted over the module cells, so the ranking covers the same trailing
 * {@value StatisticsCube#MODULE_MONTHS} months as the modules table and follows the same
 * filters; {@link ModuleCellFilter} says which.
 *
 * <p>This is the one place the statistics name a user rather than count one: the
 * {@value #TOP} rows that are returned carry their {@code iss} and {@code sub}, which is
 * what lets the page link each of them to that user's plans.
 */
final class UserRanker {

	/** How many users the table lists. */
	static final int TOP = 20;

	/** Most runs first; the identity breaks ties so the table is the same list every request. */
	private static final Comparator<TopUser> BY_RUNS =
		Comparator.comparingLong(TopUser::runs).reversed().thenComparing(TopUser::iss).thenComparing(TopUser::sub);

	private UserRanker() {
	}

	/**
	 * @param cube  the cube being sliced
	 * @param query what to show; its range, family and plan apply, its variant and
	 *              certification profile filters do not
	 * @return the {@value #TOP} users who ran the most test modules, most runs first
	 */
	static List<TopUser> rank(StatisticsCube cube, StatisticsQuery query) {
		ModuleCellFilter filter = new ModuleCellFilter(cube, query);
		Map<Integer, Counts> counted = new HashMap<>();
		for (ModuleUserCell cell : cube.modules()) {
			if (!filter.includes(cell)) {
				continue;
			}
			Counts counts = counted.computeIfAbsent(cell.ownerId(), ownerId -> new Counts());
			counts.runs += cell.runs();
			counts.failed += cell.failed();
			counts.modules.add(cell.testName());
		}

		List<TopUser> users = new ArrayList<>(counted.size());
		counted.forEach((ownerId, counts) -> {
			Owner owner = cube.moduleOwner(ownerId);
			users.add(new TopUser(owner.iss(), owner.sub(), counts.runs, counts.failed, counts.modules.size()));
		});
		users.sort(BY_RUNS);
		return List.copyOf(users.subList(0, Math.min(TOP, users.size())));
	}

	/** What is counted per user while the cells are walked. */
	private static final class Counts {

		private final Set<String> modules = new HashSet<>();

		private long runs;

		private long failed;
	}
}
