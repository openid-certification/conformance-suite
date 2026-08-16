package net.openid.conformance.statistics;

import net.openid.conformance.plan.TestPlan.ProfileNames;
import net.openid.conformance.plan.TestPlan.SpecFamilyNames;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class StatisticsCube_UnitTest {

	/** A Thursday; the Monday of its ISO week is 2026-03-09. */
	private static final LocalDate NOW = LocalDate.of(2026, 3, 12);

	/** The oldest week 104 weeks of history reaches back to, from {@link #NOW}. */
	private static final String OLDEST_WEEK = "2024-03-18";

	private static final TileRow NO_TILES = new TileRow(0, 0, 0, 0, 0, 0, 0);

	private static final SpecFamilyResolver RESOLVER = new SpecFamilyResolver(
		Map.of("fapi1-plan", SpecFamilyNames.fapi1Advanced, "oidcc-plan", SpecFamilyNames.oidcc),
		Map.of("fapi1-plan", ProfileNames.optest, "oidcc-plan", ProfileNames.rptest));

	@Test
	void weeklyCellsAreRolledUpIntoOneCellPerMonth() {
		StatisticsCube cube = cube(List.of(
			runCell("2026-02", "2026-02-02", "oidcc-plan", 3),
			runCell("2026-02", "2026-02-09", "oidcc-plan", 4),
			runCell("2026-03", "2026-03-02", "oidcc-plan", 5)));

		assertThat(cube.runs(Granularity.MONTH))
			.extracting(RunCell::month, RunCell::week, RunCell::runs)
			.containsExactlyInAnyOrder(
				tuple("2026-02", null, 7L),
				tuple("2026-03", null, 5L));
		assertThat(cube.runs(Granularity.WEEK))
			.extracting(RunCell::month, RunCell::week, RunCell::runs)
			.containsExactlyInAnyOrder(
				tuple(null, "2026-02-02", 3L),
				tuple(null, "2026-02-09", 4L),
				tuple(null, "2026-03-02", 5L));
	}

	@Test
	void everyResultBucketIsSummedByTheMonthlyRollup() {
		StatisticsCube cube = cube(List.of(
			new RunCell("2026-02", "2026-02-02", "oidcc-plan", false, "", "", 10, 1, 2, 3, 4, 0),
			new RunCell("2026-02", "2026-02-09", "oidcc-plan", false, "", "", 5, 5, 0, 0, 0, 0)));

		assertThat(cube.runs(Granularity.MONTH)).singleElement()
			.satisfies(cell -> {
				assertThat(cell.runs()).isEqualTo(15);
				assertThat(cell.passed()).isEqualTo(6);
				assertThat(cell.failed()).isEqualTo(2);
				assertThat(cell.warning()).isEqualTo(3);
				assertThat(cell.review()).isEqualTo(4);
				assertThat(cell.skipped()).isZero();
			});
	}

	@Test
	void cellsAreKeptApartByPlanStandalonenessVariantAndCertificationProfile() {
		StatisticsCube cube = cube(List.of(
			new RunCell("2026-02", null, "oidcc-plan", false, "a=1", "", 1, 0, 0, 0, 0, 0),
			new RunCell("2026-02", null, "oidcc-plan", false, "a=2", "", 2, 0, 0, 0, 0, 0),
			new RunCell("2026-02", null, "oidcc-plan", false, "a=1", "Cert A", 4, 0, 0, 0, 0, 0),
			new RunCell("2026-02", null, null, true, "", "", 8, 0, 0, 0, 0, 0)));

		assertThat(cube.runs(Granularity.MONTH)).hasSize(4);
	}

	@Test
	void weeksOlderThanTheRetainedWindowAreDroppedButStillCountTowardsTheirMonth() {
		StatisticsCube cube = cube(List.of(
			runCell("2024-03", "2024-03-11", "oidcc-plan", 100),
			runCell("2024-03", OLDEST_WEEK, "oidcc-plan", 7)));

		assertThat(cube.runs(Granularity.MONTH))
			.extracting(RunCell::month, RunCell::runs)
			.containsExactly(tuple("2024-03", 107L));
		assertThat(cube.runs(Granularity.WEEK))
			.extracting(RunCell::week, RunCell::runs)
			.containsExactly(tuple(OLDEST_WEEK, 7L));
	}

	@Test
	void weekKeysThatAreNotAnIsoWeekMondayAreDroppedFromTheWeeklyCells() {
		StatisticsCube cube = cube(List.of(
			runCell("2026-03", null, "oidcc-plan", 1),
			runCell("2026-03", "", "oidcc-plan", 2),
			runCell("2026-03", "2026-03-10", "oidcc-plan", 4),
			runCell("2026-03", "not-a-week", "oidcc-plan", 8)));

		assertThat(cube.runs(Granularity.MONTH)).singleElement()
			.extracting(RunCell::runs).isEqualTo(15L);
		assertThat(cube.runs(Granularity.WEEK)).isEmpty();
	}

	@Test
	void planCellsAreRolledUpTheSameWay() {
		StatisticsCube cube = new StatisticsCube(List.of(),
			List.of(new PlanCell("2026-02", "2026-02-02", "oidcc-plan", null, null, 3, 1, 1),
				new PlanCell("2026-02", "2026-02-09", "oidcc-plan", "", "", 4, 0, 2),
				new PlanCell("2026-02", "2024-01-01", "oidcc-plan", "", "", 5, 5, 5)),
			List.of(), List.of(), List.of(), List.of(), List.of(), NO_TILES, RESOLVER, NOW);

		assertThat(cube.plans(Granularity.MONTH)).singleElement().satisfies(cell -> {
			assertThat(cell.plans()).isEqualTo(12);
			assertThat(cell.certified()).isEqualTo(6);
			assertThat(cell.published()).isEqualTo(8);
			assertThat(cell.variantKey()).isEmpty();
			assertThat(cell.certKey()).isEmpty();
		});
		assertThat(cube.plans(Granularity.WEEK)).hasSize(2);
	}

	@Test
	void nullVariantAndCertificationKeysAreNormalisedToTheEmptyKey() {
		StatisticsCube cube = cube(List.of(
			new RunCell("2026-02", null, "oidcc-plan", false, null, null, 1, 0, 0, 0, 0, 0)));

		assertThat(cube.runs(Granularity.MONTH)).singleElement().satisfies(cell -> {
			assertThat(cell.variantKey()).isEmpty();
			assertThat(cell.certKey()).isEmpty();
		});
	}

	@Test
	void variantKeysAreParsedOnceAndSharedByEveryLookup() {
		StatisticsCube cube = cube(List.of(new RunCell("2026-02", null, "oidcc-plan", false,
			"client_auth_type=mtls;fapi_profile=plain", "", 1, 0, 0, 0, 0, 0)));

		assertThat(cube.variantOf("client_auth_type=mtls;fapi_profile=plain"))
			.containsExactly(Map.entry("client_auth_type", "mtls"), Map.entry("fapi_profile", "plain"));
		assertThat(cube.variantOf("client_auth_type=mtls;fapi_profile=plain"))
			.isSameAs(cube.variantOf("client_auth_type=mtls;fapi_profile=plain"));
		assertThat(cube.variantOf("")).isEmpty();
		assertThat(cube.variantOf(null)).isEmpty();
	}

	@Test
	void familyAndEntityComeFromTheRegistry() {
		StatisticsCube cube = cube(List.of());

		assertThat(cube.familyOf("oidcc-plan")).isEqualTo(SpecFamilyNames.oidcc);
		assertThat(cube.entityOf("oidcc-plan")).isEqualTo(ProfileNames.rptest);
		assertThat(cube.entityOf("fapi1-plan")).isEqualTo(ProfileNames.optest);
		assertThat(cube.familyOf("retired-plan")).isEqualTo(SpecFamilyResolver.OTHER_RETIRED);
		assertThat(cube.entityOf("retired-plan")).isEqualTo(SpecFamilyResolver.OTHER_RETIRED);
		assertThat(cube.familyOf(null)).isEqualTo(SpecFamilyResolver.OTHER_RETIRED);
		assertThat(cube.familyOrder()).isEqualTo(RESOLVER.familyOrder());
	}

	@Test
	void monthlyPeriodsRunFromTheEarliestRowToTheCurrentMonth() {
		StatisticsCube cube = cube(List.of(
			runCell("2025-12", "2025-12-01", "oidcc-plan", 1),
			runCell("nonsense", null, "oidcc-plan", 1),
			runCell("2099-01", null, "oidcc-plan", 1)));

		assertThat(cube.periods(Granularity.MONTH))
			.containsExactly("2025-12", "2026-01", "2026-02", "2026-03");
	}

	@Test
	void weeklyPeriodsRunFromTheEarliestRetainedWeekToTheCurrentWeek() {
		StatisticsCube cube = cube(List.of(
			runCell("2024-03", "2024-03-11", "oidcc-plan", 1),
			runCell("2026-02", "2026-02-23", "oidcc-plan", 1)));

		assertThat(cube.periods(Granularity.WEEK))
			.containsExactly("2026-02-23", "2026-03-02", "2026-03-09");
	}

	@Test
	void userTupleWeeksAreWindowedAndMonthsAreKeptForever() {
		StatisticsCube cube = new StatisticsCube(List.of(), List.of(),
			List.of(new UserTuple("oidcc-plan", null, null, 1,
				List.of("2026-02", "2020-01"), List.of("2026-02-23", "2024-03-11", "not-a-week"))),
			List.of(), List.of(), List.of(), List.of(), NO_TILES, RESOLVER, NOW);

		UserTuple tuple = cube.users().get(0);
		assertThat(tuple.months()).containsExactly("2020-01", "2026-02");
		assertThat(tuple.weeks()).containsExactly("2026-02-23");
		assertThat(tuple.variantKey()).isEmpty();
		assertThat(tuple.certKey()).isEmpty();
		assertThat(tuple.periods(Granularity.MONTH)).containsExactly("2020-01", "2026-02");
		assertThat(tuple.periods(Granularity.WEEK)).containsExactly("2026-02-23");
		assertThat(cube.periods(Granularity.MONTH)).hasSize(75);
	}

	@Test
	void anEmptyCubeHasNoPeriodsButStillAnswersEveryAccessor() {
		StatisticsCube cube = cube(List.of());

		assertThat(cube.periods(Granularity.MONTH)).isEmpty();
		assertThat(cube.periods(Granularity.WEEK)).isEmpty();
		assertThat(cube.runs(Granularity.MONTH)).isEmpty();
		assertThat(cube.plans(Granularity.WEEK)).isEmpty();
		assertThat(cube.users()).isEmpty();
		assertThat(cube.heat()).isEmpty();
		assertThat(cube.externalHosts()).isEmpty();
		assertThat(cube.storage()).isEmpty();
		assertThat(cube.tiles()).isEqualTo(NO_TILES);
	}

	@Test
	void heatCellsAreBinnedByDayOfWeekAndHourWithTheirPeriodKeys() {
		StatisticsCube cube = new StatisticsCube(List.of(), List.of(), List.of(),
			List.of(new HeatCell("2026-03-09", "07", 5),
				new HeatCell("2026-03-15", "23", 2),
				new HeatCell("2024-01-01", "00", 9),
				new HeatCell("bad-day", "07", 1),
				new HeatCell("2026-03-09", "xx", 1)),
			List.of(), List.of(), List.of(), NO_TILES, RESOLVER, NOW);

		assertThat(cube.heat()).extracting(HeatBin::month, HeatBin::week, HeatBin::dow, HeatBin::hour, HeatBin::runs)
			.containsExactlyInAnyOrder(
				tuple("2026-03", "2026-03-09", 0, 7, 5L),
				tuple("2026-03", "2026-03-09", 6, 23, 2L),
				// outside the 104 week window: still monthly, no longer weekly
				tuple("2024-01", null, 0, 0, 9L));
	}

	private static StatisticsCube cube(List<RunCell> runCells) {
		return new StatisticsCube(runCells, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
			NO_TILES, RESOLVER, NOW);
	}

	private static RunCell runCell(String month, String week, String planName, long runs) {
		return new RunCell(month, week, planName, false, "", "", runs, 0, 0, 0, 0, 0);
	}
}
