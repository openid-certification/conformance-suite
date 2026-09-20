package net.openid.conformance.statistics;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Which module cells of a cube a query selects: what the modules table and the top users
 * table are both counted over.
 *
 * <p>Two filters of the query apply, both as registry membership: a module belongs to a
 * family if any plan of that family runs it, and to a plan if that plan runs it. A module
 * can therefore appear under several families. The variant and certification profile
 * filters do <em>not</em> apply - a test run records neither in a form these cells carry -
 * and the page says so next to the tables. The two synthetic families select nothing, for
 * the same reason: they stand for plans that are not in the registry, and the registry is
 * the only thing that knows which modules a plan runs.
 *
 * <p>The range is applied by month whatever the granularity of the axis is: the cells are
 * monthly, so a weekly range is widened to the months its weeks fall in.
 *
 * <p>Not thread safe: one instance belongs to one walk over the cells.
 */
final class ModuleCellFilter {

	private static final int MONTH_KEY_LENGTH = "YYYY-MM".length();

	private final StatisticsCube cube;

	private final StatisticsQuery query;

	private final String from;

	private final String to;

	/**
	 * The family/plan verdict depends only on the module name, of which there are a few
	 * hundred, while the cells are one per month, module and user - so it is memoized
	 * rather than re-derived per cell.
	 */
	private final Map<String, Boolean> included = new HashMap<>();

	/**
	 * @param cube  the cube whose module cells are being walked
	 * @param query what to show; its range, family and plan apply, its variant and
	 *              certification profile filters do not
	 */
	ModuleCellFilter(StatisticsCube cube, StatisticsQuery query) {
		this.cube = cube;
		this.query = query;
		this.from = month(query.from());
		this.to = month(lastDay(query));
	}

	/** @return true if the query selects {@code cell} */
	boolean includes(ModuleUserCell cell) {
		return inRange(cell.month()) && included.computeIfAbsent(cell.testName(), this::matches);
	}

	private boolean matches(String testName) {
		if (query.family() != null && !cube.moduleFamilies(testName).contains(query.family())) {
			return false;
		}
		return query.plan() == null || cube.modulePlans(testName).contains(query.plan());
	}

	private boolean inRange(String month) {
		return (from == null || month.compareTo(from) >= 0) && (to == null || month.compareTo(to) <= 0);
	}

	/**
	 * @param period a period key from the query, or null for no bound
	 * @return the month it falls in. A weekly key is cut down to its month rather than
	 *         being refused, which widens a weekly range to whole months - the cells have
	 *         no weekly form to clip to.
	 */
	private static String month(String period) {
		return period == null || period.length() < MONTH_KEY_LENGTH ? null : period.substring(0, MONTH_KEY_LENGTH);
	}

	/**
	 * @return the last day of the query's range, or null for no bound. A week key names its
	 *         Monday, so the week's Sunday is what decides the month a weekly range ends in.
	 */
	private static String lastDay(StatisticsQuery query) {
		if (query.to() == null || query.granularity() != Granularity.WEEK) {
			return query.to();
		}
		return LocalDate.parse(query.to()).plusDays(6).toString();
	}
}
