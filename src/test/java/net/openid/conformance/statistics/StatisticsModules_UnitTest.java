package net.openid.conformance.statistics;

import net.openid.conformance.plan.TestPlan.ProfileNames;
import net.openid.conformance.plan.TestPlan.SpecFamilyNames;
import net.openid.conformance.statistics.StatisticsOverview.Module;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The modules table of the overview: which test modules are run most, and which ones the
 * most users hit a failure on.
 *
 * <p>The rule the whole section exists for is that a user counts <b>once</b> per module
 * however many times they failed it, so that one determined implementer retrying the same
 * module fifty times cannot make it look like the module everybody struggles with.
 */
class StatisticsModules_UnitTest {

	/** A Thursday; the Monday of its ISO week is 2026-03-09. */
	private static final LocalDate NOW = LocalDate.of(2026, 3, 12);

	/** The oldest month a 24 month window reaches back to, from {@link #NOW}. */
	private static final String OLDEST_MONTH = "2024-04";

	private static final TileRow NO_TILES = new TileRow(0, 0, 0, 0, 0, 0, 0);

	private static final String SHARED = "shared-module";

	private static final String OIDCC = "oidcc-module";

	private static final String FAPI1 = "fapi1-module";

	/**
	 * Two plans of different families both run {@link #SHARED}, which is what makes a
	 * module belong to more than one family.
	 */
	private static final SpecFamilyResolver RESOLVER = new SpecFamilyResolver(
		Map.of("fapi1-plan", SpecFamilyNames.fapi1Advanced, "oidcc-plan", SpecFamilyNames.oidcc),
		Map.of("fapi1-plan", ProfileNames.optest, "oidcc-plan", ProfileNames.rptest),
		Map.of("fapi1-plan", List.of(SHARED, FAPI1), "oidcc-plan", List.of(SHARED, OIDCC)));

	@Test
	void runsAreSummedAndEveryUserOfAModuleIsCountedOnce() {
		StatisticsOverview overview = slice(cube(
			cell("2026-01", OIDCC, 1, 3, 0),
			cell("2026-02", OIDCC, 1, 2, 0),
			cell("2026-02", OIDCC, 2, 5, 0)), StatisticsQuery.defaults());

		assertThat(overview.modules()).singleElement().satisfies(module -> {
			assertThat(module.testName()).isEqualTo(OIDCC);
			assertThat(module.runs()).isEqualTo(10);
			assertThat(module.users()).isEqualTo(2);
			assertThat(module.failingUsers()).isZero();
			assertThat(module.failingShare()).isZero();
		});
	}

	@Test
	void aUserWhoFailedTheSameModuleEveryMonthIsOneFailingUser() {
		StatisticsOverview overview = slice(cube(
			// one user, three months, seven failures
			cell("2026-01", OIDCC, 1, 2, 2),
			cell("2026-02", OIDCC, 1, 1, 1),
			cell("2026-03", OIDCC, 1, 8, 4),
			// another user who never failed it
			cell("2026-03", OIDCC, 2, 10, 0)), StatisticsQuery.defaults());

		assertThat(overview.modules()).singleElement().satisfies(module -> {
			assertThat(module.runs()).isEqualTo(21);
			assertThat(module.users()).isEqualTo(2);
			assertThat(module.failingUsers()).isEqualTo(1);
			assertThat(module.failingShare()).isEqualTo(0.5);
		});
	}

	@Test
	void theFailingShareIsRoundedToThreeDecimals() {
		StatisticsOverview overview = slice(cube(
			cell("2026-03", OIDCC, 1, 1, 1),
			cell("2026-03", OIDCC, 2, 1, 1),
			cell("2026-03", OIDCC, 3, 1, 0)), StatisticsQuery.defaults());

		assertThat(overview.modules()).singleElement().satisfies(module -> {
			assertThat(module.failingUsers()).isEqualTo(2);
			assertThat(module.failingShare()).isEqualTo(0.667, within(1e-9));
		});
	}

	@Test
	void cellsOlderThanTheTwentyFourMonthWindowAreDropped() {
		StatisticsCube cube = cube(
			cell(OLDEST_MONTH, OIDCC, 1, 5, 0),
			cell("2024-03", OIDCC, 1, 99, 99),
			cell("2019-01", OIDCC, 2, 99, 99),
			// a month key the aggregation could not derive from an unusable started
			cell("", OIDCC, 3, 99, 99));

		assertThat(cube.modules()).extracting(ModuleUserCell::month).containsExactly(OLDEST_MONTH);
		assertThat(slice(cube, StatisticsQuery.defaults()).modules()).singleElement().satisfies(module -> {
			assertThat(module.runs()).isEqualTo(5);
			assertThat(module.users()).isEqualTo(1);
		});
	}

	@Test
	void cellsDatedAfterThisMonthAreDropped() {
		StatisticsCube cube = cube(
			cell("2026-03", OIDCC, 1, 5, 0),
			// a clock skewed deployment, or a started that is not a string and so passed the
			// aggregation's string comparison: neither may show up in an unranged table
			cell("2026-04", OIDCC, 2, 99, 99),
			cell("2099-12", FAPI1, 3, 99, 99));

		assertThat(cube.modules()).extracting(ModuleUserCell::month).containsExactly("2026-03");
		assertThat(slice(cube, StatisticsQuery.defaults()).modules()).singleElement().satisfies(module -> {
			assertThat(module.testName()).isEqualTo(OIDCC);
			assertThat(module.runs()).isEqualTo(5);
			assertThat(module.users()).isEqualTo(1);
			assertThat(module.failingUsers()).isZero();
		});
	}

	@Test
	void theRangeSelectsWhichMonthsCount() {
		StatisticsCube cube = cube(
			cell("2025-12", OIDCC, 1, 1, 1),
			cell("2026-01", OIDCC, 1, 2, 0),
			cell("2026-02", OIDCC, 2, 4, 0),
			cell("2026-03", OIDCC, 3, 8, 8));

		StatisticsOverview overview = slice(cube, query("from", "2026-01", "to", "2026-02"));

		assertThat(overview.modules()).singleElement().satisfies(module -> {
			assertThat(module.runs()).isEqualTo(6);
			assertThat(module.users()).isEqualTo(2);
			// the only failures are outside the range
			assertThat(module.failingUsers()).isZero();
		});
	}

	@Test
	void aWeeklyRangeIsWidenedToTheMonthsItsWeeksFallIn() {
		StatisticsCube cube = cube(
			cell("2026-01", OIDCC, 1, 1, 0),
			cell("2026-02", OIDCC, 1, 2, 0),
			cell("2026-03", OIDCC, 2, 4, 0));

		StatisticsOverview overview = slice(cube,
			query("granularity", "week", "from", "2026-02-23", "to", "2026-03-09"));

		assertThat(overview.modules()).singleElement().extracting(Module::runs).isEqualTo(6L);
	}

	@Test
	void theFamilyFilterKeepsTheModulesOfEveryPlanOfThatFamily() {
		StatisticsCube cube = cube(
			cell("2026-03", SHARED, 1, 1, 0),
			cell("2026-03", OIDCC, 1, 2, 0),
			cell("2026-03", FAPI1, 1, 4, 0),
			cell("2026-03", "retired-module", 1, 8, 0));

		assertThat(names(slice(cube, query("family", SpecFamilyNames.oidcc)))).containsExactly(OIDCC, SHARED);
		// the same module, in a plan of another family
		assertThat(names(slice(cube, query("family", SpecFamilyNames.fapi1Advanced)))).containsExactly(FAPI1, SHARED);
		assertThat(names(slice(cube, query("family", SpecFamilyNames.ssf)))).isEmpty();
		assertThat(names(slice(cube, query("family", SpecFamilyResolver.OTHER_RETIRED)))).isEmpty();
		assertThat(names(slice(cube, StatisticsQuery.defaults())))
			.containsExactly("retired-module", FAPI1, OIDCC, SHARED);
	}

	@Test
	void thePlanFilterKeepsOnlyTheModulesOfThatPlan() {
		StatisticsCube cube = cube(
			cell("2026-03", SHARED, 1, 1, 0),
			cell("2026-03", OIDCC, 1, 2, 0),
			cell("2026-03", FAPI1, 1, 4, 0));

		assertThat(names(slice(cube, query("plan", "oidcc-plan")))).containsExactly(OIDCC, SHARED);
		assertThat(names(slice(cube, query("plan", "retired-plan")))).isEmpty();
	}

	@Test
	void theVariantAndCertificationFiltersDoNotNarrowTheModules() {
		StatisticsCube cube = cube(cell("2026-03", OIDCC, 1, 3, 1));

		StatisticsOverview overview = slice(cube,
			query("variant.fapi_profile", "openbanking_brazil", "cert", "No Such Profile"));

		assertThat(names(overview)).containsExactly(OIDCC);
		// ... and the runs chart, which does apply them, has nothing left
		assertThat(overview.dimensions().plans()).isEmpty();
	}

	@Test
	void theTableIsTheUnionOfTheFiftyMostRunAndTheFiftyMostFailedModules() {
		List<ModuleUserCell> cells = new ArrayList<>();
		for (int module = 1; module <= 60; module++) {
			// runs and failing users both fall as the module number rises
			cells.add(cell("2026-03", name(module), 0, 1000L - module, 0));
			for (int user = 1; user <= 61 - module; user++) {
				cells.add(cell("2026-03", name(module), user, 1, 1));
			}
		}
		// hardly run, but more users have hit a failure on it than on anything else
		for (int user = 100; user < 200; user++) {
			cells.add(cell("2026-03", "failing-favourite", user, 1, 1));
		}

		List<Module> modules = slice(cube(cells.toArray(new ModuleUserCell[0])),
			StatisticsQuery.defaults()).modules();

		assertThat(modules).hasSize(51);
		assertThat(modules).extracting(Module::testName)
			.startsWith(name(1), name(2))
			.contains("failing-favourite")
			.doesNotContain(name(51), name(60));
		// sorted by runs, most run first, whichever list a module got in by
		assertThat(modules).extracting(Module::runs).isSortedAccordingTo(Comparator.reverseOrder());
		assertThat(modules.get(modules.size() - 1)).isEqualTo(new Module("failing-favourite", 100, 100, 100, 1.0));
	}

	@Test
	void anEmptyCubeHasNoModules() {
		assertThat(slice(cube(), StatisticsQuery.defaults()).modules()).isEmpty();
	}

	private static List<String> names(StatisticsOverview overview) {
		return overview.modules().stream().map(Module::testName).toList();
	}

	private static String name(int module) {
		return String.format("mod-%02d", module);
	}

	private static StatisticsOverview slice(StatisticsCube cube, StatisticsQuery query) {
		return StatisticsSlicer.slice(cube, query);
	}

	private static StatisticsCube cube(ModuleUserCell... cells) {
		return new StatisticsCube(List.of(), List.of(), List.of(), List.of(), List.of(cells), List.of(), List.of(),
			NO_TILES, RESOLVER, NOW);
	}

	private static ModuleUserCell cell(String month, String testName, int ownerId, long runs, long failed) {
		return new ModuleUserCell(month, testName, ownerId, runs, failed);
	}

	private static StatisticsQuery query(String... keysAndValues) {
		Map<String, String[]> params = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			params.put(keysAndValues[i], new String[] {keysAndValues[i + 1]});
		}
		return StatisticsQuery.parse(params);
	}
}
