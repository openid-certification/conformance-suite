package net.openid.conformance.statistics;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Every number the statistics page can show, aggregated as far as it can be without
 * knowing what the user is going to ask for. One cube is computed in the background and
 * then sliced per request by {@link StatisticsSlicer}, so a filter change costs a walk over
 * a few thousand cells rather than another pass over the whole database.
 *
 * <p>Two things happen when a cube is built:
 *
 * <ul>
 * <li><b>Monthly rollup.</b> The aggregations produce cells keyed by both month and ISO
 * week; they are summed into one set of cells per month and one per week, so slicing at
 * either granularity is a single pass with no arithmetic on keys.</li>
 * <li><b>The weekly window.</b> Weekly cells are kept only for the trailing
 * {@value #WEEKS_KEPT} weeks. That happens <em>after</em> the monthly rollup, so nothing is
 * lost from the monthly view - the whole history stays available by month, while the weekly
 * view, which is only ever used to look at recent activity, does not grow without bound.</li>
 * <li><b>The module window.</b> Module cells are keyed by user as well as by month, so
 * there are far more of them than there are run cells; only the trailing
 * {@value #MODULE_MONTHS} months of them are kept. The aggregation already asks MongoDB
 * for that window, and it is enforced again here, at <em>both</em> ends, so that a cell the
 * database let through cannot widen it: a clock skewed deployment can date a run in the
 * future, and the month key each cell carries is derived from the same field the window was
 * matched on, so whatever the match let past is checked once more against the months the
 * table has room for. What the second bound is <em>not</em> for is a {@code started} that
 * is not a string: MongoDB's comparisons are bracketed by BSON type, so a run whose
 * {@code started} was written as a date rather than as the ISO-8601 string
 * {@code TestInfo} writes matches no {@code $gte} of a string at all and is never counted
 * in the first place.</li>
 * </ul>
 *
 * <p>Immutable and safe to share between request threads.
 */
public class StatisticsCube {

	/** How many ISO weeks of weekly cells are kept; older data stays available by month. */
	public static final int WEEKS_KEPT = 104;

	/** How many months of module cells are kept, the current month included. */
	public static final int MODULE_MONTHS = 24;

	private final List<RunCell> monthlyRuns;

	private final List<RunCell> weeklyRuns;

	private final List<PlanCell> monthlyPlans;

	private final List<PlanCell> weeklyPlans;

	private final List<UserTuple> users;

	private final List<HeatBin> heat;

	private final List<ModuleUserCell> modules;

	private final List<HostRow> externalHosts;

	private final List<StorageRow> storage;

	private final TileRow tiles;

	private final SpecFamilyResolver resolver;

	private final Map<String, Map<String, String>> variantsByKey;

	private final List<String> monthlyPeriods;

	private final List<String> weeklyPeriods;

	/**
	 * @param runCells      module runs per period, plan, variant and certification profile
	 * @param planCells     plans created per period, plan, variant and certification profile
	 * @param userTuples    the periods each user was active in, per plan, variant and profile
	 * @param heatCells     runs per day and hour
	 * @param moduleCells   runs of each test module per month and user
	 * @param externalHosts the external servers the suite has been pointed at, all time
	 * @param storage       per collection storage counters
	 * @param tiles         the whole-collection counters behind the summary tiles
	 * @param resolver      maps plan names to their spec family and entity under test
	 * @param nowUtc        today in UTC: the last period of the axis, and what the weekly
	 *                      and module windows are measured back from
	 */
	public StatisticsCube(List<RunCell> runCells, List<PlanCell> planCells, List<UserTuple> userTuples,
			List<HeatCell> heatCells, List<ModuleUserCell> moduleCells, List<HostRow> externalHosts,
			List<StorageRow> storage, TileRow tiles, SpecFamilyResolver resolver, LocalDate nowUtc) {
		String oldestWeek = LocalDate.parse(Granularity.WEEK.periodOf(nowUtc)).minusWeeks(WEEKS_KEPT - 1L).toString();
		this.monthlyRuns = rollUpRuns(runCells, Granularity.MONTH, oldestWeek);
		this.weeklyRuns = rollUpRuns(runCells, Granularity.WEEK, oldestWeek);
		this.monthlyPlans = rollUpPlans(planCells, Granularity.MONTH, oldestWeek);
		this.weeklyPlans = rollUpPlans(planCells, Granularity.WEEK, oldestWeek);
		this.users = normaliseUsers(userTuples, oldestWeek);
		this.heat = HeatmapBinner.bin(heatCells, oldestWeek);
		this.modules = inTheModuleWindow(moduleCells, oldestModuleMonth(nowUtc), Granularity.MONTH.periodOf(nowUtc));
		this.externalHosts = List.copyOf(externalHosts);
		this.storage = List.copyOf(storage);
		this.tiles = tiles;
		this.resolver = resolver;
		this.variantsByKey = parseVariants(monthlyRuns, monthlyPlans, users);
		this.monthlyPeriods = axis(monthlyRuns, monthlyPlans, users, Granularity.MONTH, nowUtc);
		this.weeklyPeriods = axis(weeklyRuns, weeklyPlans, users, Granularity.WEEK, nowUtc);
	}

	/** @return the run cells at {@code granularity}; monthly cells cover the whole history */
	public List<RunCell> runs(Granularity granularity) {
		return granularity == Granularity.MONTH ? monthlyRuns : weeklyRuns;
	}

	/** @return the plan cells at {@code granularity}; monthly cells cover the whole history */
	public List<PlanCell> plans(Granularity granularity) {
		return granularity == Granularity.MONTH ? monthlyPlans : weeklyPlans;
	}

	/** @return one tuple per user, plan, variant and certification profile */
	public List<UserTuple> users() {
		return users;
	}

	/** @return every run binned by day of the week and hour, with its period keys */
	public List<HeatBin> heat() {
		return heat;
	}

	/**
	 * @return runs of each test module per month and user, over the trailing
	 *         {@value #MODULE_MONTHS} months. Not rolled up any further and not part of the
	 *         period axis: the modules table is monthly whatever the axis granularity is.
	 */
	public List<ModuleUserCell> modules() {
		return modules;
	}

	/**
	 * @param nowUtc today in UTC
	 * @return the first month of the module window: the month {@value #MODULE_MONTHS}
	 *         months long window that ends with the month {@code nowUtc} falls in starts
	 *         with. Static because the aggregation asks MongoDB for the same window.
	 */
	public static String oldestModuleMonth(LocalDate nowUtc) {
		return YearMonth.from(nowUtc).minusMonths(MODULE_MONTHS - 1L).toString();
	}

	/**
	 * @param testName a test module name
	 * @return the families of the plans that run it; empty for a module the registry no
	 *         longer has. A module in plans of several families belongs to all of them.
	 */
	public Set<String> moduleFamilies(String testName) {
		return resolver.familiesForModule(testName);
	}

	/**
	 * @param testName a test module name
	 * @return the names of the plans that run it; empty for a module the registry no longer
	 *         has
	 */
	public Set<String> modulePlans(String testName) {
		return resolver.plansForModule(testName);
	}

	/** @return the external servers the suite has been pointed at, all time */
	public List<HostRow> externalHosts() {
		return externalHosts;
	}

	/** @return the per collection storage counters */
	public List<StorageRow> storage() {
		return storage;
	}

	/** @return the whole-collection counters behind the summary tiles */
	public TileRow tiles() {
		return tiles;
	}

	/**
	 * @param granularity the granularity to get the axis of
	 * @return contiguous period keys from the earliest one any cell mentions up to today,
	 *         oldest first; empty if the cube holds no dated data. The weekly axis never
	 *         starts before the retained window.
	 */
	public List<String> periods(Granularity granularity) {
		return granularity == Granularity.MONTH ? monthlyPeriods : weeklyPeriods;
	}

	/** @return every family a series may need, in the order the client colours them in */
	public List<String> familyOrder() {
		return resolver.familyOrder();
	}

	/** @return the spec family of {@code planName}, or "Other / retired" if it is unknown */
	public String familyOf(String planName) {
		return resolver.familyForPlan(planName);
	}

	/** @return what {@code planName} tests, or "Other / retired" if the plan is unknown */
	public String entityOf(String planName) {
		return resolver.entityForPlan(planName);
	}

	/**
	 * @param variantKey a canonical variant key
	 * @return its parameters; parsed once per distinct key in the cube, so filtering and
	 *         counting by variant does not re-parse the same string thousands of times
	 */
	public Map<String, String> variantOf(String variantKey) {
		if (variantKey == null || variantKey.isEmpty()) {
			return Map.of();
		}
		Map<String, String> parsed = variantsByKey.get(variantKey);
		// a key that is not in the cube can only come from a filter, so parse it as it comes
		return parsed == null ? VariantKeys.parse(variantKey) : parsed;
	}

	/**
	 * @param cells       the module cells as the aggregation delivered them
	 * @param oldestMonth the first month of the window
	 * @param newestMonth the last month of it, the month today falls in: the window is
	 *                    bounded at both ends, so a run dated in the future - a clock skewed
	 *                    deployment, or a {@code started} that is not a string and therefore
	 *                    passed the aggregation's string comparison - cannot show up in a
	 *                    modules table that was not asked for it
	 * @return the cells inside it. A cell whose month key is not a usable one - which is
	 *         what a document with no usable {@code started} produces - is dropped rather
	 *         than kept the way an undated run cell is: nothing counts unresolved module
	 *         names, so such a cell could only ever add runs to a month that is not there.
	 */
	private static List<ModuleUserCell> inTheModuleWindow(List<ModuleUserCell> cells, String oldestMonth,
			String newestMonth) {
		List<ModuleUserCell> kept = new ArrayList<>(cells.size());
		for (ModuleUserCell cell : cells) {
			if (Granularity.MONTH.isPeriod(cell.month()) && cell.month().compareTo(oldestMonth) >= 0
				&& cell.month().compareTo(newestMonth) <= 0) {
				kept.add(cell);
			}
		}
		return List.copyOf(kept);
	}

	private static List<RunCell> rollUpRuns(List<RunCell> cells, Granularity granularity, String oldestWeek) {
		Map<CellKey, long[]> totals = new LinkedHashMap<>();
		for (RunCell cell : cells) {
			String period = period(cell.period(granularity), granularity, oldestWeek);
			if (period == null) {
				continue;
			}
			long[] counters = totals.computeIfAbsent(
				new CellKey(period, cell.planName(), cell.standalone(), key(cell.variantKey()), key(cell.certKey())),
				key -> new long[6]);
			counters[0] += cell.runs();
			counters[1] += cell.passed();
			counters[2] += cell.failed();
			counters[3] += cell.warning();
			counters[4] += cell.review();
			counters[5] += cell.skipped();
		}
		List<RunCell> rolled = new ArrayList<>(totals.size());
		totals.forEach((key, counters) -> rolled.add(new RunCell(
			granularity == Granularity.MONTH ? key.period() : null,
			granularity == Granularity.WEEK ? key.period() : null,
			key.planName(), key.standalone(), key.variantKey(), key.certKey(),
			counters[0], counters[1], counters[2], counters[3], counters[4], counters[5])));
		return List.copyOf(rolled);
	}

	private static List<PlanCell> rollUpPlans(List<PlanCell> cells, Granularity granularity, String oldestWeek) {
		Map<CellKey, long[]> totals = new LinkedHashMap<>();
		for (PlanCell cell : cells) {
			String period = period(cell.period(granularity), granularity, oldestWeek);
			if (period == null) {
				continue;
			}
			long[] counters = totals.computeIfAbsent(
				new CellKey(period, cell.planName(), false, key(cell.variantKey()), key(cell.certKey())),
				key -> new long[3]);
			counters[0] += cell.plans();
			counters[1] += cell.certified();
			counters[2] += cell.published();
		}
		List<PlanCell> rolled = new ArrayList<>(totals.size());
		totals.forEach((key, counters) -> rolled.add(new PlanCell(
			granularity == Granularity.MONTH ? key.period() : null,
			granularity == Granularity.WEEK ? key.period() : null,
			key.planName(), key.variantKey(), key.certKey(), counters[0], counters[1], counters[2])));
		return List.copyOf(rolled);
	}

	/**
	 * @return the period key to file a cell under, or null if it does not belong at this
	 *         granularity. Monthly keys are taken as they come, even the empty key a
	 *         document with no usable {@code started} produces: they never match a period on
	 *         the axis, but they still count towards the unresolved plan names an admin is
	 *         shown. Weekly keys have to be a real ISO week Monday inside the window.
	 */
	private static String period(String period, Granularity granularity, String oldestWeek) {
		if (granularity == Granularity.MONTH) {
			return key(period);
		}
		return granularity.isPeriod(period) && period.compareTo(oldestWeek) >= 0 ? period : null;
	}

	private static List<UserTuple> normaliseUsers(List<UserTuple> tuples, String oldestWeek) {
		List<UserTuple> normalised = new ArrayList<>(tuples.size());
		for (UserTuple tuple : tuples) {
			normalised.add(new UserTuple(tuple.planName(), key(tuple.variantKey()), key(tuple.certKey()),
				tuple.ownerId(),
				periods(tuple.months(), Granularity.MONTH, null),
				periods(tuple.weeks(), Granularity.WEEK, oldestWeek)));
		}
		return List.copyOf(normalised);
	}

	/** @return the usable keys of {@code periods}, sorted, without duplicates */
	private static List<String> periods(List<String> periods, Granularity granularity, String oldestWeek) {
		if (periods == null) {
			return List.of();
		}
		TreeSet<String> usable = new TreeSet<>();
		for (String period : periods) {
			if (granularity.isPeriod(period) && (oldestWeek == null || period.compareTo(oldestWeek) >= 0)) {
				usable.add(period);
			}
		}
		return List.copyOf(usable);
	}

	private static Map<String, Map<String, String>> parseVariants(List<RunCell> runs, List<PlanCell> plans,
			List<UserTuple> users) {
		Map<String, Map<String, String>> parsed = new HashMap<>();
		for (RunCell cell : runs) {
			parsed.computeIfAbsent(cell.variantKey(), VariantKeys::parse);
		}
		for (PlanCell cell : plans) {
			parsed.computeIfAbsent(cell.variantKey(), VariantKeys::parse);
		}
		for (UserTuple tuple : users) {
			parsed.computeIfAbsent(tuple.variantKey(), VariantKeys::parse);
		}
		return Map.copyOf(parsed);
	}

	/** @return contiguous periods from the earliest one the cube knows about up to today */
	private static List<String> axis(List<RunCell> runs, List<PlanCell> plans, List<UserTuple> users,
			Granularity granularity, LocalDate nowUtc) {
		String earliest = earliestPeriod(runs, plans, users, granularity);
		String last = granularity.periodOf(nowUtc);
		if (earliest == null || earliest.compareTo(last) > 0) {
			return List.of();
		}
		List<String> periods = new ArrayList<>();
		for (String period = earliest; period.compareTo(last) <= 0; period = granularity.next(period)) {
			periods.add(period);
		}
		return List.copyOf(periods);
	}

	/** @return the earliest usable period key any cell mentions, or null if there is none */
	private static String earliestPeriod(List<RunCell> runs, List<PlanCell> plans, List<UserTuple> users,
			Granularity granularity) {
		String earliest = null;
		for (RunCell cell : runs) {
			earliest = earliest(earliest, cell.period(granularity), granularity);
		}
		for (PlanCell cell : plans) {
			earliest = earliest(earliest, cell.period(granularity), granularity);
		}
		for (UserTuple tuple : users) {
			for (String period : tuple.periods(granularity)) {
				earliest = earliest(earliest, period, granularity);
			}
		}
		return earliest;
	}

	private static String earliest(String earliest, String candidate, Granularity granularity) {
		if (!granularity.isPeriod(candidate)) {
			return earliest;
		}
		return earliest == null || candidate.compareTo(earliest) < 0 ? candidate : earliest;
	}

	/** @return {@code key}, or the empty string if it is null, so grouping is null safe */
	private static String key(String key) {
		return key == null ? "" : key;
	}

	/**
	 * What makes two cells the same cell. {@code standalone} is always false for plan
	 * cells, which have no such thing.
	 */
	private record CellKey(String period, String planName, boolean standalone, String variantKey, String certKey) {
	}
}
