package net.openid.conformance.statistics;

import net.openid.conformance.plan.TestPlan.SpecFamilyNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsAssembler_UnitTest {

	private static final TileRow NO_TILES = new TileRow(0, 0, 0, 0, 0, 0);

	private StatisticsAssembler assembler;
	private List<String> familyOrder;

	@BeforeEach
	void setUp() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(Map.of(
			"fapi1-plan", SpecFamilyNames.fapi1Advanced,
			"oidcc-plan", SpecFamilyNames.oidcc));
		assembler = new StatisticsAssembler(resolver);
		familyOrder = resolver.familyOrder();
	}

	@Test
	void monthsAreContiguousFromTheEarliestRowToNowAndListsAreZeroFilled() {
		StatisticsOverview overview = assembler.assemble(
			List.of(runsRow("2026-01", "fapi1-plan", 5), runsRow("2026-04", "fapi1-plan", 3)),
			List.of(), List.of(), NO_TILES, YearMonth.of(2026, 5));

		assertThat(overview.months()).containsExactly("2026-01", "2026-02", "2026-03", "2026-04", "2026-05");
		assertThat(overview.families()).isEqualTo(familyOrder);
		assertThat(overview.resultBuckets()).isEqualTo(StatisticsAssembler.RESULT_BUCKETS);
		assertThat(overview.testRunsByFamily().keySet()).containsExactlyElementsOf(familyOrder);
		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.fapi1Advanced)).containsExactly(5L, 0L, 0L, 3L, 0L);
		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.oidcc)).containsExactlyElementsOf(zeros(5));
		assertThat(overview.plansByFamily().keySet()).containsExactlyElementsOf(familyOrder);
		assertThat(overview.plansByFamily().get(SpecFamilyNames.fapi1Advanced)).containsExactlyElementsOf(zeros(5));
		assertThat(overview.resultsByFamily().keySet()).containsExactlyElementsOf(familyOrder);
		assertThat(overview.resultsByFamily().get(SpecFamilyNames.oidcc).keySet())
			.containsExactlyElementsOf(StatisticsAssembler.RESULT_BUCKETS);
		assertThat(overview.resultsByFamily().get(SpecFamilyNames.fapi1Advanced).get("NEVER_FINISHED"))
			.containsExactly(5L, 0L, 0L, 3L, 0L);
		assertThat(overview.users().activeByMonth()).containsExactlyElementsOf(zeros(5));
		assertThat(overview.users().newByMonth()).containsExactlyElementsOf(zeros(5));
	}

	@Test
	void rowsWithAMalformedMonthKeyAreDropped() {
		StatisticsOverview overview = assembler.assemble(
			List.of(runsRow("2026-3", "oidcc-plan", 100),
				runsRow("not-a-month", "oidcc-plan", 100),
				runsRow("", "oidcc-plan", 100),
				runsRow("2026-03", "oidcc-plan", 4)),
			List.of(new PlanRow("nonsense", "oidcc-plan", 9, 9)),
			List.of(new UserRow("iss", "sub", List.of("2026-3"))),
			NO_TILES, YearMonth.of(2026, 3));

		assertThat(overview.months()).containsExactly("2026-03");
		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.oidcc)).containsExactly(4L);
		assertThat(overview.plansByFamily().get(SpecFamilyNames.oidcc)).containsExactly(0L);
		assertThat(overview.users().activeByMonth()).containsExactly(0L);
		assertThat(overview.users().newByMonth()).containsExactly(0L);
	}

	@Test
	void runsAreAttributedToNoPlanStandaloneOtherRetiredOrTheSpecFamily() {
		StatisticsOverview overview = assembler.assemble(
			List.of(new RunsRow("2026-02", null, true, 4, 0, 0, 0, 0, 0),
				new RunsRow("2026-02", "oidcc-plan", true, 6, 0, 0, 0, 0, 0),
				runsRow("2026-02", null, 2),
				runsRow("2026-02", "retired-plan", 7),
				runsRow("2026-02", "oidcc-plan", 1)),
			List.of(), List.of(), NO_TILES, YearMonth.of(2026, 2));

		assertThat(overview.testRunsByFamily().get(SpecFamilyResolver.NO_PLAN)).containsExactly(10L);
		assertThat(overview.testRunsByFamily().get(SpecFamilyResolver.OTHER_RETIRED)).containsExactly(9L);
		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.oidcc)).containsExactly(1L);
	}

	@Test
	void neverFinishedIsTheResidualOfTheFiveResultBucketsClampedAtZero() {
		StatisticsOverview overview = assembler.assemble(
			List.of(new RunsRow("2026-02", "oidcc-plan", false, 10, 3, 2, 1, 1, 1),
				new RunsRow("2026-02", "fapi1-plan", false, 1, 2, 0, 0, 0, 0)),
			List.of(), List.of(), NO_TILES, YearMonth.of(2026, 2));

		Map<String, List<Long>> oidcc = overview.resultsByFamily().get(SpecFamilyNames.oidcc);
		assertThat(oidcc.get("PASSED")).containsExactly(3L);
		assertThat(oidcc.get("FAILED")).containsExactly(2L);
		assertThat(oidcc.get("WARNING")).containsExactly(1L);
		assertThat(oidcc.get("REVIEW")).containsExactly(1L);
		assertThat(oidcc.get("SKIPPED")).containsExactly(1L);
		assertThat(oidcc.get("NEVER_FINISHED")).containsExactly(2L);

		Map<String, List<Long>> fapi1 = overview.resultsByFamily().get(SpecFamilyNames.fapi1Advanced);
		assertThat(fapi1.get("PASSED")).containsExactly(2L);
		assertThat(fapi1.get("NEVER_FINISHED")).containsExactly(0L);
	}

	@Test
	void plansAreAttributedByPlanNameWithUnknownNamesFallingBackToOtherRetired() {
		StatisticsOverview overview = assembler.assemble(
			List.of(),
			List.of(new PlanRow("2026-01", "oidcc-plan", 7, 2),
				new PlanRow("2026-02", "retired-plan", 3, 1),
				new PlanRow("2026-02", null, 5, 0)),
			List.of(), NO_TILES, YearMonth.of(2026, 2));

		assertThat(overview.plansByFamily().get(SpecFamilyNames.oidcc)).containsExactly(7L, 0L);
		assertThat(overview.plansByFamily().get(SpecFamilyResolver.OTHER_RETIRED)).containsExactly(0L, 8L);
		assertThat(overview.tiles().totalPlans()).isEqualTo(15);
		assertThat(overview.tiles().certifiedPlans()).isEqualTo(3);
	}

	@Test
	void usersAreCountedAsActivePerMonthAndNewInTheirFirstMonth() {
		StatisticsOverview overview = assembler.assemble(
			List.of(), List.of(),
			List.of(new UserRow("iss1", "sub1", List.of("2026-03", "2026-01")),
				new UserRow("iss1", "sub2", List.of("2026-03")),
				new UserRow("", "sub3", List.of("2026-01")),
				new UserRow(null, "sub4", List.of("2026-01")),
				new UserRow("iss2", " ", List.of("2026-01")),
				new UserRow("iss3", "sub5", List.of("bogus", "2026-02"))),
			NO_TILES, YearMonth.of(2026, 3));

		assertThat(overview.months()).containsExactly("2026-01", "2026-02", "2026-03");
		assertThat(overview.users().activeByMonth()).containsExactly(1L, 1L, 2L);
		assertThat(overview.users().newByMonth()).containsExactly(1L, 1L, 1L);
		assertThat(overview.tiles().totalUsers()).isEqualTo(3);
	}

	@Test
	void tilesCopyTheRowCountsAndSumThePlanCounts() {
		StatisticsOverview overview = assembler.assemble(
			List.of(runsRow("2026-01", "oidcc-plan", 4)),
			List.of(new PlanRow("2026-01", "oidcc-plan", 7, 2)),
			List.of(new UserRow("iss1", "sub1", List.of("2026-01"))),
			new TileRow(1000, 10, 40, 120, 5, 2), YearMonth.of(2026, 1));

		StatisticsOverview.Tiles tiles = overview.tiles();
		assertThat(tiles.totalTests()).isEqualTo(1000);
		assertThat(tiles.totalPlans()).isEqualTo(7);
		assertThat(tiles.certifiedPlans()).isEqualTo(2);
		assertThat(tiles.totalUsers()).isEqualTo(1);
		assertThat(tiles.testsLast24h()).isEqualTo(10);
		assertThat(tiles.testsLast7d()).isEqualTo(40);
		assertThat(tiles.testsLast30d()).isEqualTo(120);
		assertThat(tiles.inProgress()).isEqualTo(5);
		assertThat(tiles.stuck()).isEqualTo(2);
	}

	@Test
	void unresolvedPlansAreTheTwentyBusiestOtherRetiredPlanNames() {
		List<RunsRow> rows = new ArrayList<>();
		for (int i = 1; i <= 25; i++) {
			rows.add(runsRow("2026-01", String.format("retired-plan-%02d", i), i));
		}
		rows.add(runsRow("2026-01", "oidcc-plan", 999));
		rows.add(new RunsRow("2026-01", null, true, 999, 0, 0, 0, 0, 0));
		rows.add(runsRow("2026-01", null, 999));
		Collections.shuffle(rows, new Random(42));

		StatisticsOverview overview = assembler.assemble(rows, List.of(), List.of(), NO_TILES, YearMonth.of(2026, 1));

		assertThat(overview.unresolvedPlans()).hasSize(20);
		assertThat(overview.unresolvedPlans().get(0)).isEqualTo(new StatisticsOverview.UnresolvedPlan("retired-plan-25", 25));
		assertThat(overview.unresolvedPlans().get(19)).isEqualTo(new StatisticsOverview.UnresolvedPlan("retired-plan-06", 6));
		assertThat(overview.unresolvedPlans()).extracting(StatisticsOverview.UnresolvedPlan::planName)
			.startsWith("retired-plan-25", "retired-plan-24", "retired-plan-23")
			.endsWith("retired-plan-07", "retired-plan-06")
			.doesNotContain("oidcc-plan");
	}

	@Test
	void unresolvedPlanRunsAreSummedAcrossMonths() {
		StatisticsOverview overview = assembler.assemble(
			List.of(runsRow("2026-01", "retired-plan", 2), runsRow("2026-02", "retired-plan", 3)),
			List.of(), List.of(), NO_TILES, YearMonth.of(2026, 2));

		assertThat(overview.unresolvedPlans())
			.containsExactly(new StatisticsOverview.UnresolvedPlan("retired-plan", 5));
	}

	@Test
	void emptyInputProducesAnEmptyMonthAxisWithEveryFamilyStillPresent() {
		StatisticsOverview overview = assembler.assemble(List.of(), List.of(), List.of(), NO_TILES, YearMonth.of(2026, 8));

		assertThat(overview.months()).isEmpty();
		assertThat(overview.families()).isEqualTo(familyOrder);
		assertThat(overview.testRunsByFamily().keySet()).containsExactlyElementsOf(familyOrder);
		assertThat(overview.testRunsByFamily().values()).allSatisfy(counts -> assertThat(counts).isEmpty());
		assertThat(overview.plansByFamily().values()).allSatisfy(counts -> assertThat(counts).isEmpty());
		assertThat(overview.resultsByFamily().get(SpecFamilyNames.oidcc).values())
			.allSatisfy(counts -> assertThat(counts).isEmpty());
		assertThat(overview.users().activeByMonth()).isEmpty();
		assertThat(overview.users().newByMonth()).isEmpty();
		assertThat(overview.unresolvedPlans()).isEmpty();
		assertThat(overview.tiles().totalTests()).isZero();
		assertThat(overview.tiles().totalPlans()).isZero();
		assertThat(overview.tiles().totalUsers()).isZero();
	}

	private static RunsRow runsRow(String month, String planName, long runs) {
		return new RunsRow(month, planName, false, runs, 0, 0, 0, 0, 0);
	}

	private static List<Long> zeros(int size) {
		return Collections.nCopies(size, 0L);
	}
}
