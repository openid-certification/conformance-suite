package net.openid.conformance.statistics;

import net.openid.conformance.statistics.StatisticsOverview.Module;

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
 * <p>Two filters of the query apply, both as registry membership: a module belongs to a
 * family if any plan of that family runs it, and to a plan if that plan runs it. A module
 * can therefore appear under several families. The variant and certification profile
 * filters do <em>not</em> apply - a test run records neither in a form these cells carry -
 * and the page says so next to the table. The two synthetic families select nothing, for
 * the same reason: they stand for plans that are not in the registry, and the registry is
 * the only thing that knows which modules a plan runs.
 *
 * <p>The range is applied by month whatever the granularity of the axis is: the modules
 * table is monthly, so a weekly range is widened to the months its weeks fall in.
 */
final class ModuleRanker {

	/** How many modules each of the two rankings contributes to the table. */
	private static final int TOP = 50;

	private static final int MONTH_KEY_LENGTH = "YYYY-MM".length();

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
	 *         the most users hit a failure on, most run first
	 */
	static List<Module> rank(StatisticsCube cube, StatisticsQuery query) {
		String from = month(query.from());
		String to = month(query.to());
		Map<String, Counts> counted = new LinkedHashMap<>();
		for (ModuleUserCell cell : cube.modules()) {
			if (!inRange(cell.month(), from, to) || !matches(cube, query, cell.testName())) {
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
	 * @return the union of the two rankings, ordered by runs. Both are cut at
	 *         {@value #TOP}: a module can be in the table because a lot of people run it or
	 *         because a lot of people fail it, and the two lists together are what the
	 *         client's two charts draw from.
	 */
	private static List<Module> top(List<Module> modules) {
		List<Module> byRuns = new ArrayList<>(modules);
		byRuns.sort(BY_RUNS);
		List<Module> byFailingUsers = new ArrayList<>(modules);
		byFailingUsers.sort(BY_FAILING_USERS);

		Set<String> keep = new HashSet<>();
		keepTop(byRuns, keep);
		keepTop(byFailingUsers, keep);

		List<Module> top = new ArrayList<>(keep.size());
		for (Module module : byRuns) {
			if (keep.contains(module.testName())) {
				top.add(module);
			}
		}
		return List.copyOf(top);
	}

	private static void keepTop(List<Module> ranked, Set<String> keep) {
		for (int at = 0; at < ranked.size() && at < TOP; at++) {
			keep.add(ranked.get(at).testName());
		}
	}

	/** @return how many of the module's users hit a failure on it, rounded to three decimals */
	private static double share(Counts counts) {
		if (counts.users.isEmpty()) {
			return 0;
		}
		double share = (double) counts.failingUsers.cardinality() / counts.users.cardinality();
		return Math.round(share * SHARE_SCALE) / SHARE_SCALE;
	}

	private static boolean matches(StatisticsCube cube, StatisticsQuery query, String testName) {
		if (query.family() != null && !cube.moduleFamilies(testName).contains(query.family())) {
			return false;
		}
		return query.plan() == null || cube.modulePlans(testName).contains(query.plan());
	}

	private static boolean inRange(String month, String from, String to) {
		return (from == null || month.compareTo(from) >= 0) && (to == null || month.compareTo(to) <= 0);
	}

	/**
	 * @param period a period key from the query, or null for no bound
	 * @return the month it falls in. A weekly key is cut down to its month rather than
	 *         being refused, which widens a weekly range to whole months - the modules
	 *         table has no weekly form to clip to.
	 */
	private static String month(String period) {
		return period == null || period.length() < MONTH_KEY_LENGTH ? null : period.substring(0, MONTH_KEY_LENGTH);
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
