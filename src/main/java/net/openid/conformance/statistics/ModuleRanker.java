package net.openid.conformance.statistics;

import net.openid.conformance.statistics.StatisticsOverview.Module;
import net.openid.conformance.statistics.StatisticsOverview.Modules;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ranks the test modules of a slice: the ones that are run most, and the ones the most
 * users hit a failure on.
 *
 * <p>Users are counted <b>distinct</b> on both sides. A user who ran one module a hundred
 * times, failing every time, is one user and one failing user, so the second ranking says
 * "this many different people got stuck here" rather than "this many runs went wrong" -
 * which one determined implementer retrying the same module all afternoon would otherwise
 * dominate.
 *
 * <p>Which cells of the cube a query selects is {@link ModuleCellFilter}'s to say.
 */
final class ModuleRanker {

	/** How many modules each of the two rankings contributes to the table. */
	private static final int TOP = 50;

	/** Three decimals: the client shows the share as a whole percentage or one decimal. */
	private static final double SHARE_SCALE = 1000;

	/** Most run first; the name breaks ties so the table is the same list every request. */
	private static final Comparator<Module> BY_RUNS =
		Comparator.comparingLong(Module::runs).reversed().thenComparing(Module::testName);

	/** Most failed-on first, then the busier module, then the name. */
	private static final Comparator<Module> BY_FAILING_USERS =
		Comparator.comparingLong(Module::failingUsers).reversed()
			.thenComparing(Comparator.comparingLong(Module::runs).reversed())
			.thenComparing(Module::testName);

	private ModuleRanker() {
	}

	/**
	 * @param cube  the cube being sliced
	 * @param query what to show; its range, family and plan apply, its variant and
	 *              certification profile filters do not
	 * @return the union of the {@value #TOP} most run modules and the {@value #TOP} modules
	 *         the most users hit a failure on, most run first, plus the two rankings
	 */
	static Modules rank(StatisticsCube cube, StatisticsQuery query) {
		ModuleCellFilter filter = new ModuleCellFilter(cube, query);
		Map<String, Counts> counted = new LinkedHashMap<>();
		for (ModuleUserCell cell : cube.modules()) {
			if (!filter.includes(cell)) {
				continue;
			}
			Counts counts = counted.computeIfAbsent(cell.testName(), testName -> new Counts());
			counts.runs += cell.runs();
			counts.users.set(cell.ownerId());
			if (cell.failed() > 0) {
				// one cell is one user's whole month of that module, so this is that user
				// counted once however many of their runs failed
				counts.failingUsers.set(cell.ownerId());
			}
		}

		List<Module> modules = new ArrayList<>(counted.size());
		counted.forEach((testName, counts) -> modules.add(new Module(testName, counts.runs,
			counts.users.cardinality(), counts.failingUsers.cardinality(), share(counts))));
		return top(modules);
	}

	/**
	 * @param modules every module of the slice
	 * @return the two rankings, each cut at {@value #TOP}, and their union ordered by runs:
	 *         a module can be in the table because a lot of people run it or because a lot
	 *         of people fail it, and each chart plots the head of its own ranking
	 */
	private static Modules top(List<Module> modules) {
		List<Module> byRuns = new ArrayList<>(modules);
		byRuns.sort(BY_RUNS);
		List<Module> byFailingUsers = new ArrayList<>(modules);
		byFailingUsers.sort(BY_FAILING_USERS);
		List<String> topByRuns = names(byRuns);
		List<String> topByFailingUsers = names(byFailingUsers);

		Set<String> keep = new HashSet<>(topByRuns);
		keep.addAll(topByFailingUsers);
		List<Module> rows = new ArrayList<>(keep.size());
		for (Module module : byRuns) {
			if (keep.contains(module.testName())) {
				rows.add(module);
			}
		}
		return new Modules(List.copyOf(rows), topByRuns, topByFailingUsers);
	}

	/** @return the names of the first {@value #TOP} of a ranking, in its order */
	private static List<String> names(List<Module> ranked) {
		return ranked.stream().limit(TOP).map(Module::testName).toList();
	}

	/** @return how many of the module's users hit a failure on it, rounded to three decimals */
	private static double share(Counts counts) {
		if (counts.users.isEmpty()) {
			return 0;
		}
		double share = (double) counts.failingUsers.cardinality() / counts.users.cardinality();
		return Math.round(share * SHARE_SCALE) / SHARE_SCALE;
	}

	/**
	 * What is counted per module while the cells are walked.
	 *
	 * <p>The two user sets are bit sets rather than {@code Set<Integer>}: {@link OwnerIds}
	 * hands out dense ids from zero, so a user is one bit here instead of a boxed Integer
	 * and a hash table entry, and this runs on the request thread over every module cell.
	 */
	private static final class Counts {

		private final BitSet users = new BitSet();

		private final BitSet failingUsers = new BitSet();

		private long runs;
	}
}
