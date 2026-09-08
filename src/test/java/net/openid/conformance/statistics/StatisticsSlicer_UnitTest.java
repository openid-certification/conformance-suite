package net.openid.conformance.statistics;

import net.openid.conformance.plan.TestPlan.ProfileNames;
import net.openid.conformance.plan.TestPlan.SpecFamilyNames;
import net.openid.conformance.statistics.StatisticsOverview.CertProfile;
import net.openid.conformance.statistics.StatisticsOverview.Entity;
import net.openid.conformance.statistics.StatisticsOverview.PlanDimension;
import net.openid.conformance.statistics.StatisticsOverview.UnresolvedPlan;
import net.openid.conformance.statistics.StatisticsOverview.VariantValue;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class StatisticsSlicer_UnitTest {

	/** A Thursday; the Monday of its ISO week is 2026-03-09. */
	private static final LocalDate NOW = LocalDate.of(2026, 3, 12);

	private static final TileRow NO_TILES = new TileRow(0, 0, 0, 0, 0, 0, 0);

	private static final SpecFamilyResolver RESOLVER = new SpecFamilyResolver(
		Map.of("fapi1-plan", SpecFamilyNames.fapi1Advanced, "oidcc-plan", SpecFamilyNames.oidcc),
		Map.of("fapi1-plan", ProfileNames.optest, "oidcc-plan", ProfileNames.rptest));

	@Test
	void everyFamilySeriesIsZeroFilledOverAContiguousMonthlyAxis() {
		StatisticsOverview overview = slice(cube(
			List.of(runs("2026-01", null, "oidcc-plan", 5), runs("2026-03", null, "fapi1-plan", 3)),
			List.of(), List.of()), StatisticsQuery.defaults());

		assertThat(overview.granularity()).isEqualTo("month");
		assertThat(overview.periods()).containsExactly("2026-01", "2026-02", "2026-03");
		assertThat(overview.families()).isEqualTo(RESOLVER.familyOrder());
		assertThat(overview.resultBuckets()).isEqualTo(StatisticsSlicer.RESULT_BUCKETS);
		assertThat(overview.testRunsByFamily().keySet()).containsExactlyElementsOf(RESOLVER.familyOrder());
		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.oidcc)).containsExactly(5L, 0L, 0L);
		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.fapi1Advanced)).containsExactly(0L, 0L, 3L);
		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.ssf)).containsExactly(0L, 0L, 0L);
		assertThat(overview.plansByFamily().keySet()).containsExactlyElementsOf(RESOLVER.familyOrder());
		assertThat(overview.plansByFamily().get(SpecFamilyNames.oidcc)).containsExactly(0L, 0L, 0L);
		assertThat(overview.certifiedByFamily().keySet()).containsExactlyElementsOf(RESOLVER.familyOrder());
		assertThat(overview.certifiedByFamily().get(SpecFamilyNames.oidcc)).containsExactly(0L, 0L, 0L);
		assertThat(overview.resultsByFamily().get(SpecFamilyNames.oidcc).keySet())
			.containsExactlyElementsOf(StatisticsSlicer.RESULT_BUCKETS);
		assertThat(overview.users().activeByPeriod()).containsExactly(0L, 0L, 0L);
		assertThat(overview.users().newByPeriod()).containsExactly(0L, 0L, 0L);
	}

	@Test
	void neverFinishedIsTheResidualOfTheFiveResultBucketsClampedAtZero() {
		StatisticsOverview overview = slice(cube(
			List.of(new RunCell("2026-03", null, "oidcc-plan", false, "", "", 10, 3, 2, 1, 1, 1),
				new RunCell("2026-03", null, "fapi1-plan", false, "", "", 1, 2, 0, 0, 0, 0)),
			List.of(), List.of()), StatisticsQuery.defaults());

		Map<String, List<Long>> oidcc = overview.resultsByFamily().get(SpecFamilyNames.oidcc);
		assertThat(oidcc.get("PASSED")).containsExactly(3L);
		assertThat(oidcc.get("FAILED")).containsExactly(2L);
		assertThat(oidcc.get("WARNING")).containsExactly(1L);
		assertThat(oidcc.get("REVIEW")).containsExactly(1L);
		assertThat(oidcc.get("SKIPPED")).containsExactly(1L);
		assertThat(oidcc.get("NEVER_FINISHED")).containsExactly(2L);
		assertThat(overview.resultsByFamily().get(SpecFamilyNames.fapi1Advanced).get("NEVER_FINISHED"))
			.containsExactly(0L);
	}

	@Test
	void standaloneRunsAndUnknownPlansUseTheSyntheticFamilies() {
		StatisticsOverview overview = slice(cube(
			List.of(new RunCell("2026-03", null, null, true, "", "", 4, 0, 0, 0, 0, 0),
				new RunCell("2026-03", null, "oidcc-plan", true, "", "", 6, 0, 0, 0, 0, 0),
				runs("2026-03", null, null, 2),
				runs("2026-03", null, "retired-plan", 7),
				runs("2026-03", null, "oidcc-plan", 1)),
			List.of(), List.of()), StatisticsQuery.defaults());

		assertThat(overview.testRunsByFamily().get(SpecFamilyResolver.NO_PLAN)).containsExactly(10L);
		assertThat(overview.testRunsByFamily().get(SpecFamilyResolver.OTHER_RETIRED)).containsExactly(9L);
		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.oidcc)).containsExactly(1L);
	}

	@Test
	void weeklyGranularityUsesTheMondayAxisOfTheRetainedWindow() {
		StatisticsOverview overview = slice(cube(
			List.of(runs("2026-02", "2026-02-23", "oidcc-plan", 5),
				runs("2026-03", "2026-03-09", "oidcc-plan", 2),
				// outside the 104 week window: monthly only
				runs("2024-03", "2024-03-11", "oidcc-plan", 99)),
			List.of(), List.of()), query("granularity", "week"));

		assertThat(overview.granularity()).isEqualTo("week");
		assertThat(overview.periods()).containsExactly("2026-02-23", "2026-03-02", "2026-03-09");
		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.oidcc)).containsExactly(5L, 0L, 2L);
	}

	@Test
	void fromAndToClipThePeriodAxisAndTheSeries() {
		StatisticsCube cube = cube(
			List.of(runs("2025-12", null, "oidcc-plan", 1), runs("2026-01", null, "oidcc-plan", 2),
				runs("2026-02", null, "oidcc-plan", 4), runs("2026-03", null, "oidcc-plan", 8)),
			List.of(), List.of());

		StatisticsOverview overview = slice(cube, query("from", "2026-01", "to", "2026-02"));

		assertThat(overview.periods()).containsExactly("2026-01", "2026-02");
		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.oidcc)).containsExactly(2L, 4L);
	}

	@Test
	void theFamilyFilterLeavesEveryOtherFamilyAtZero() {
		StatisticsOverview overview = slice(cube(
			List.of(runs("2026-03", null, "oidcc-plan", 5), runs("2026-03", null, "fapi1-plan", 3)),
			List.of(), List.of()), query("family", SpecFamilyNames.oidcc));

		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.oidcc)).containsExactly(5L);
		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.fapi1Advanced)).containsExactly(0L);
		assertThat(overview.periods()).containsExactly("2026-03");
	}

	@Test
	void anOpenEndedAxisStartsWhereTheSliceStartsNotWhereTheCubeDoes() {
		StatisticsCube cube = cube(
			List.of(runs("2019-03", null, "oidcc-plan", 5), runs("2026-02", null, "fapi1-plan", 3)),
			List.of(new PlanCell("2026-01", null, "fapi1-plan", "", "", 1, 0, 0)),
			List.of(new UserTuple("fapi1-plan", "", "", 1, List.of("2025-12"), List.of(), false)));

		// unfiltered: the whole history, from the first run
		assertThat(slice(cube, StatisticsQuery.defaults()).periods()).startsWith("2019-03", "2019-04");

		// a family that first appears years later starts its axis there - at its first
		// plan owner, which predates its first plan and its first run
		StatisticsOverview filtered = slice(cube, query("family", SpecFamilyNames.fapi1Advanced));
		assertThat(filtered.periods()).startsWith("2025-12", "2026-01", "2026-02");
		assertThat(filtered.users().activeByPeriod()).startsWith(1L, 0L, 0L);
		assertThat(filtered.plansByFamily().get(SpecFamilyNames.fapi1Advanced)).startsWith(0L, 1L, 0L);
		assertThat(filtered.testRunsByFamily().get(SpecFamilyNames.fapi1Advanced)).startsWith(0L, 0L, 3L);
		assertThat(filtered.testRunsByFamily().get(SpecFamilyNames.oidcc)).hasSameSizeAs(filtered.periods());
		assertThat(filtered.resultsByFamily().get(SpecFamilyNames.fapi1Advanced).get("PASSED"))
			.hasSameSizeAs(filtered.periods());

		// an explicit from is what was asked for, empty leading periods and all
		assertThat(slice(cube, query("family", SpecFamilyNames.fapi1Advanced, "from", "2024-01")).periods())
			.startsWith("2024-01");

		// a filter that matches nothing keeps the whole axis, zero filled: an empty axis
		// means the range covers no data at all, which is a different message to the reader
		StatisticsOverview nothing = slice(cube, query("plan", "no-such-plan"));
		assertThat(nothing.periods()).isEqualTo(slice(cube, StatisticsQuery.defaults()).periods());
		assertThat(nothing.testRunsByFamily().get(SpecFamilyNames.oidcc)).containsOnly(0L);
	}

	@Test
	void thePlanVariantAndCertificationFiltersSelectCells() {
		List<RunCell> runCells = List.of(
			new RunCell("2026-03", null, "oidcc-plan", false, "client_auth_type=mtls;fapi_profile=plain", "Cert A", 1, 0, 0, 0, 0, 0),
			new RunCell("2026-03", null, "oidcc-plan", false, "client_auth_type=mtls;fapi_profile=brazil", "Cert B", 2, 0, 0, 0, 0, 0),
			new RunCell("2026-03", null, "oidcc-plan", false, "client_auth_type=jwt;fapi_profile=plain", "Cert A", 4, 0, 0, 0, 0, 0),
			new RunCell("2026-03", null, "fapi1-plan", false, "client_auth_type=mtls;fapi_profile=plain", "Cert A", 8, 0, 0, 0, 0, 0));
		StatisticsCube cube = cube(runCells, List.of(), List.of());

		assertThat(slice(cube, query("plan", "oidcc-plan")).testRunsByFamily().get(SpecFamilyNames.oidcc))
			.containsExactly(7L);
		assertThat(slice(cube, query("variant.client_auth_type", "mtls")).testRunsByFamily().get(SpecFamilyNames.oidcc))
			.containsExactly(3L);
		assertThat(slice(cube, query("variant.client_auth_type", "mtls", "variant.fapi_profile", "plain"))
			.testRunsByFamily().get(SpecFamilyNames.oidcc)).containsExactly(1L);
		assertThat(slice(cube, query("cert", "Cert A")).testRunsByFamily().get(SpecFamilyNames.oidcc))
			.containsExactly(5L);
		// a filter that selects nothing is zero on the whole axis
		assertThat(slice(cube, query("plan", "no-such-plan")).testRunsByFamily().get(SpecFamilyNames.oidcc))
			.containsExactly(0L);
	}

	@Test
	void plansAndCertifiedPlansAreSeriesOfTheirOwn() {
		StatisticsOverview overview = slice(cube(List.of(),
			List.of(new PlanCell("2026-02", null, "oidcc-plan", "", "", 7, 2, 1),
				new PlanCell("2026-03", null, "retired-plan", "", "", 3, 1, 0)),
			List.of()), StatisticsQuery.defaults());

		assertThat(overview.plansByFamily().get(SpecFamilyNames.oidcc)).containsExactly(7L, 0L);
		assertThat(overview.plansByFamily().get(SpecFamilyResolver.OTHER_RETIRED)).containsExactly(0L, 3L);
		assertThat(overview.certifiedByFamily().get(SpecFamilyNames.oidcc)).containsExactly(2L, 0L);
		assertThat(overview.certifiedByFamily().get(SpecFamilyResolver.OTHER_RETIRED)).containsExactly(0L, 1L);
		assertThat(overview.tiles().totalPlans()).isEqualTo(10);
		assertThat(overview.tiles().certifiedPlans()).isEqualTo(3);
		assertThat(overview.tiles().publishedPlans()).isEqualTo(1);
	}

	@Test
	void aUserWithSeveralTuplesIsCountedOncePerPeriodAndIsNewOnlyInTheirFirstPeriod() {
		StatisticsOverview overview = slice(cube(List.of(), List.of(),
			List.of(new UserTuple("oidcc-plan", "fapi_profile=plain", "", 1, List.of("2026-01", "2026-02"), List.of(), false),
				new UserTuple("fapi1-plan", "fapi_profile=plain", "", 1, List.of("2026-02"), List.of(), false),
				new UserTuple("oidcc-plan", "fapi_profile=brazil", "", 2, List.of("2026-02"), List.of(), false))),
			StatisticsQuery.defaults());

		assertThat(overview.periods()).containsExactly("2026-01", "2026-02", "2026-03");
		assertThat(overview.users().activeByPeriod()).containsExactly(1L, 2L, 0L);
		assertThat(overview.users().newByPeriod()).containsExactly(1L, 1L, 0L);
	}

	@Test
	void theUsersSeriesIsFilteredByPlanAndVariantLikeEveryOtherSeries() {
		StatisticsCube cube = cube(List.of(), List.of(),
			List.of(new UserTuple("oidcc-plan", "fapi_profile=plain", "", 1, List.of("2026-02"), List.of(), false),
				new UserTuple("fapi1-plan", "fapi_profile=brazil", "", 2, List.of("2026-02"), List.of(), false)));

		StatisticsOverview overview = slice(cube, query("plan", "oidcc-plan"));

		assertThat(overview.periods()).containsExactly("2026-02", "2026-03");
		assertThat(overview.users().activeByPeriod()).containsExactly(1L, 0L);
		assertThat(slice(cube, query("variant.fapi_profile", "brazil")).users().activeByPeriod())
			.containsExactly(1L, 0L);
	}

	@Test
	void aUserWhoWasAlreadyActiveBeforeTheRangeIsNotNewInIt() {
		StatisticsOverview overview = slice(cube(List.of(), List.of(),
			List.of(new UserTuple("oidcc-plan", "", "", 1, List.of("2025-12", "2026-02"), List.of(), false),
				new UserTuple("oidcc-plan", "", "", 2, List.of("2026-02"), List.of(), false))),
			query("from", "2026-01"));

		assertThat(overview.periods()).containsExactly("2026-01", "2026-02", "2026-03");
		assertThat(overview.users().activeByPeriod()).containsExactly(0L, 2L, 0L);
		assertThat(overview.users().newByPeriod()).containsExactly(0L, 1L, 0L);
	}

	@Test
	void weeklyNewUsersIgnoreUsersWhoWereActiveBeforeTheRetainedWindow() {
		// The window opens on 2024-03-18. User 1 also created a plan on 2024-03-04: the same
		// month, but a week the cube no longer has, so only the tuple's flag can say so.
		StatisticsOverview overview = slice(cube(List.of(), List.of(),
			List.of(new UserTuple("oidcc-plan", "", "", 1, List.of("2024-03"), List.of("2024-03-18"), true),
				new UserTuple("oidcc-plan", "", "", 2, List.of("2024-03"), List.of("2024-03-18"), false),
				new UserTuple("oidcc-plan", "", "", 3, List.of("2019-01", "2026-03"), List.of("2026-03-09"), true))),
			query("granularity", "week"));

		assertThat(overview.periods()).startsWith("2024-03-18").endsWith("2026-03-09");
		assertThat(overview.users().activeByPeriod().get(0)).isEqualTo(2L);
		assertThat(overview.users().newByPeriod().get(0)).isEqualTo(1L);
		assertThat(overview.users().newByPeriod().get(overview.periods().size() - 1)).isEqualTo(0L);

		// the flag is about the weekly window only: by month, the history is complete
		StatisticsOverview monthly = slice(cube(List.of(), List.of(),
			List.of(new UserTuple("oidcc-plan", "", "", 1, List.of("2024-03"), List.of("2024-03-18"), true))),
			StatisticsQuery.defaults());
		assertThat(monthly.users().newByPeriod().get(0)).isEqualTo(1L);
	}

	@Test
	void dimensionsCountPlansVariantsCertificationProfilesAndEntitiesUnderTheQuery() {
		StatisticsCube cube = cube(
			List.of(new RunCell("2026-03", null, "oidcc-plan", false, "fapi_profile=plain", "Cert A", 5, 0, 0, 0, 0, 0),
				new RunCell("2026-03", null, "fapi1-plan", false, "fapi_profile=brazil", "Cert B", 9, 0, 0, 0, 0, 0),
				new RunCell("2025-01", null, "fapi1-plan", false, "fapi_profile=brazil", "Cert B", 100, 0, 0, 0, 0, 0),
				new RunCell("2026-03", null, null, true, "", "", 3, 0, 0, 0, 0, 0)),
			List.of(new PlanCell("2026-03", null, "oidcc-plan", "fapi_profile=plain", "Cert A", 2, 0, 0),
				new PlanCell("2026-03", null, "fapi1-plan", "fapi_profile=brazil", "Cert B", 1, 0, 0)),
			List.of(new UserTuple("oidcc-plan", "fapi_profile=plain", "Cert A", 1, List.of("2026-03"), List.of(), false),
				new UserTuple("fapi1-plan", "fapi_profile=plain", "Cert A", 1, List.of("2026-03"), List.of(), false),
				new UserTuple("fapi1-plan", "fapi_profile=brazil", "Cert B", 2, List.of("2026-03"), List.of(), false)));

		StatisticsOverview.Dimensions dimensions = slice(cube, query("from", "2026-03")).dimensions();

		assertThat(dimensions.plans()).extracting(PlanDimension::planName, PlanDimension::family,
				PlanDimension::runs, PlanDimension::plans)
			.containsExactly(
				tuple("fapi1-plan", SpecFamilyNames.fapi1Advanced, 9L, 1L),
				tuple("oidcc-plan", SpecFamilyNames.oidcc, 5L, 2L));
		assertThat(dimensions.variants()).containsOnlyKeys("fapi_profile");
		assertThat(dimensions.variants().get("fapi_profile"))
			.extracting(VariantValue::value, VariantValue::users, VariantValue::plans)
			.containsExactly(tuple("plain", 1L, 2L), tuple("brazil", 1L, 1L));
		assertThat(dimensions.certProfiles()).extracting(CertProfile::name, CertProfile::users, CertProfile::plans)
			.containsExactly(tuple("Cert A", 1L, 2L), tuple("Cert B", 1L, 1L));
		assertThat(dimensions.entities()).extracting(Entity::entity, Entity::runs)
			.containsExactly(tuple(ProfileNames.optest, 9L), tuple(ProfileNames.rptest, 5L),
				tuple(SpecFamilyResolver.NO_PLAN, 3L));
	}

	@Test
	void dimensionsFollowTheCurrentFilterToo() {
		StatisticsCube cube = cube(
			List.of(new RunCell("2026-03", null, "oidcc-plan", false, "fapi_profile=plain", "Cert A", 5, 0, 0, 0, 0, 0),
				new RunCell("2026-03", null, "fapi1-plan", false, "fapi_profile=brazil", "Cert B", 9, 0, 0, 0, 0, 0)),
			List.of(), List.of());

		StatisticsOverview.Dimensions dimensions = slice(cube, query("family", SpecFamilyNames.oidcc)).dimensions();

		assertThat(dimensions.plans()).extracting(PlanDimension::planName).containsExactly("oidcc-plan");
		assertThat(dimensions.variants().get("fapi_profile")).extracting(VariantValue::value).containsExactly("plain");
		assertThat(dimensions.certProfiles()).extracting(CertProfile::name).containsExactly("Cert A");
		assertThat(dimensions.entities()).extracting(Entity::entity).containsExactly(ProfileNames.rptest);
	}

	@Test
	void aPlanCertifiedForSeveralProfilesCountsUnderEachAndIsSelectedByAnyOfThem() {
		String both = CertKeys.canonical(List.of("Cert A", "Cert B"));
		StatisticsCube cube = cube(
			List.of(new RunCell("2026-03", null, "oidcc-plan", false, "", both, 5, 0, 0, 0, 0, 0),
				new RunCell("2026-03", null, "fapi1-plan", false, "", "Cert B", 9, 0, 0, 0, 0, 0)),
			List.of(new PlanCell("2026-03", null, "oidcc-plan", "", both, 2, 0, 0),
				new PlanCell("2026-03", null, "fapi1-plan", "", "Cert B", 1, 0, 0)),
			List.of(new UserTuple("oidcc-plan", "", both, 1, List.of("2026-03"), List.of(), false),
				new UserTuple("fapi1-plan", "", "Cert B", 2, List.of("2026-03"), List.of(), false)));

		StatisticsOverview unfiltered = slice(cube, StatisticsQuery.defaults());
		// one name per entry, the two-profile plan counted under both, busiest first
		assertThat(unfiltered.dimensions().certProfiles())
			.extracting(CertProfile::name, CertProfile::users, CertProfile::plans)
			.containsExactly(tuple("Cert B", 2L, 3L), tuple("Cert A", 1L, 2L));

		// the filter is membership: Cert A selects the two-profile plan, Cert B selects both plans
		assertThat(slice(cube, query("cert", "Cert A")).testRunsByFamily().get(SpecFamilyNames.oidcc))
			.containsExactly(5L);
		assertThat(slice(cube, query("cert", "Cert A")).testRunsByFamily().get(SpecFamilyNames.fapi1Advanced))
			.containsExactly(0L);
		assertThat(slice(cube, query("cert", "Cert B")).testRunsByFamily().get(SpecFamilyNames.fapi1Advanced))
			.containsExactly(9L);
		// the joined key is not a name and selects nothing
		assertThat(slice(cube, query("cert", both)).testRunsByFamily().get(SpecFamilyNames.oidcc)).containsExactly(0L);
	}

	@Test
	void eachDimensionIsCountedWithItsOwnFilterLeftOut() {
		StatisticsCube cube = cube(
			List.of(new RunCell("2026-03", null, "oidcc-plan", false, "fapi_profile=plain", "Cert A", 5, 0, 0, 0, 0, 0),
				new RunCell("2026-03", null, "oidcc-plan", false, "fapi_profile=brazil", "Cert B", 7, 0, 0, 0, 0, 0),
				new RunCell("2026-03", null, "fapi1-plan", false, "fapi_profile=brazil", "Cert B", 9, 0, 0, 0, 0, 0)),
			List.of(), List.of());

		// the plan select still offers the sibling (busiest first), everything else narrows to the plan
		StatisticsOverview.Dimensions byPlan = slice(cube, query("plan", "oidcc-plan")).dimensions();
		assertThat(byPlan.plans()).extracting(PlanDimension::planName).containsExactly("oidcc-plan", "fapi1-plan");
		assertThat(byPlan.variants().get("fapi_profile")).extracting(VariantValue::value)
			.containsExactly("brazil", "plain");
		assertThat(byPlan.certProfiles()).extracting(CertProfile::name).containsExactly("Cert A", "Cert B");
		assertThat(byPlan.entities()).extracting(Entity::entity).containsExactly(ProfileNames.rptest);

		// a variant parameter's select offers its other values; the plans narrow to the value
		StatisticsOverview.Dimensions byVariant = slice(cube, query("variant.fapi_profile", "plain")).dimensions();
		assertThat(byVariant.plans()).extracting(PlanDimension::planName).containsExactly("oidcc-plan");
		assertThat(byVariant.variants().get("fapi_profile")).extracting(VariantValue::value)
			.containsExactly("brazil", "plain");
		assertThat(byVariant.certProfiles()).extracting(CertProfile::name).containsExactly("Cert A");

		// and the certification profile select offers the other profile
		StatisticsOverview.Dimensions byCert = slice(cube, query("cert", "Cert A")).dimensions();
		assertThat(byCert.plans()).extracting(PlanDimension::planName).containsExactly("oidcc-plan");
		assertThat(byCert.certProfiles()).extracting(CertProfile::name).containsExactly("Cert A", "Cert B");

		// two filters at once: each select leaves out only its own
		StatisticsOverview.Dimensions both =
			slice(cube, query("plan", "oidcc-plan", "variant.fapi_profile", "plain")).dimensions();
		assertThat(both.plans()).extracting(PlanDimension::planName).containsExactly("oidcc-plan");
		assertThat(both.variants().get("fapi_profile")).extracting(VariantValue::value)
			.containsExactly("brazil", "plain");
		assertThat(both.certProfiles()).extracting(CertProfile::name).containsExactly("Cert A");
	}

	@Test
	void dimensionsAreCountedOverTheRequestedRangeNotTheTrimmedAxis() {
		StatisticsCube cube = cube(
			List.of(runs("2026-01", null, "oidcc-plan", 5), runs("2026-03", null, "fapi1-plan", 3)),
			List.of(), List.of());

		// picking the plan first used in March trims the axis to March, but the plan select is
		// counted with its own filter left out and must still offer the sibling used in January
		StatisticsOverview overview = slice(cube, query("plan", "fapi1-plan"));
		assertThat(overview.periods()).containsExactly("2026-03");
		assertThat(overview.dimensions().plans()).extracting(PlanDimension::planName, PlanDimension::runs)
			.containsExactly(tuple("oidcc-plan", 5L), tuple("fapi1-plan", 3L));

		// an explicit range is still the range
		assertThat(slice(cube, query("plan", "fapi1-plan", "from", "2026-02")).dimensions().plans())
			.extracting(PlanDimension::planName).containsExactly("fapi1-plan");
	}

	@Test
	void variantParametersAreOfferedInAStableAlphabeticalOrder() {
		StatisticsCube cube = cube(List.of(new RunCell("2026-03", null, "oidcc-plan", false,
			"server_metadata=discovery;client_auth_type=mtls", "", 1, 0, 0, 0, 0, 0)), List.of(), List.of());

		assertThat(slice(cube, StatisticsQuery.defaults()).dimensions().variants().keySet())
			.containsExactly("client_auth_type", "server_metadata");
	}

	@Test
	void theHeatmapIsSevenRowsOfTwentyFourHoursSlicedByTheRangeOnly() {
		StatisticsCube cube = new StatisticsCube(
			List.of(runs("2026-02", "2026-02-23", "oidcc-plan", 1), runs("2026-03", "2026-03-09", "fapi1-plan", 1)),
			List.of(), List.of(),
			List.of(new HeatCell("2026-03-09", "07", 5), new HeatCell("2026-03-15", "23", 2),
				new HeatCell("2026-02-23", "07", 8)),
			List.of(), List.of(), List.of(), NO_TILES, RESOLVER, NOW);

		List<List<Long>> all = slice(cube, StatisticsQuery.defaults()).heatmap();
		assertThat(all).hasSize(7);
		assertThat(all).allSatisfy(row -> assertThat(row).hasSize(24));
		assertThat(all.get(0).get(7)).isEqualTo(13L);
		assertThat(all.get(6).get(23)).isEqualTo(2L);
		assertThat(total(all)).isEqualTo(15L);

		// the family filter must not touch the heatmap, but the range must - and neither
		// must the axis trimming a filter causes: fapi1 first ran in March, but its heatmap
		// is still February's runs too
		List<List<Long>> filtered = slice(cube, query("family", SpecFamilyNames.oidcc)).heatmap();
		assertThat(total(filtered)).isEqualTo(15L);
		assertThat(slice(cube, query("family", SpecFamilyNames.fapi1Advanced)).periods()).containsExactly("2026-03");
		assertThat(total(slice(cube, query("family", SpecFamilyNames.fapi1Advanced)).heatmap())).isEqualTo(15L);

		List<List<Long>> ranged = slice(cube, query("from", "2026-03")).heatmap();
		assertThat(ranged.get(0).get(7)).isEqualTo(5L);
		assertThat(total(ranged)).isEqualTo(7L);

		List<List<Long>> weekly = slice(cube, query("granularity", "week", "from", "2026-03-09")).heatmap();
		assertThat(total(weekly)).isEqualTo(7L);
	}

	@Test
	void tilesStorageAndExternalHostsArePassedThrough() {
		List<StorageRow> storage = List.of(new StorageRow("TEST_INFO", 10, 20, 30, 40));
		List<HostRow> hosts = List.of(new HostRow("as.example.com", 9, 3, "2026-03-01T00:00:00Z"));
		StatisticsCube cube = new StatisticsCube(List.of(runs("2026-03", null, "oidcc-plan", 1)), List.of(),
			List.of(), List.of(), List.of(), hosts, storage, new TileRow(1000, 42, 10, 40, 120, 5, 2), RESOLVER, NOW);

		StatisticsOverview overview = slice(cube, query("family", SpecFamilyNames.fapi1Advanced));

		assertThat(overview.storage()).isEqualTo(storage);
		assertThat(overview.externalHosts()).isEqualTo(hosts);
		StatisticsOverview.Tiles tiles = overview.tiles();
		assertThat(tiles.totalTests()).isEqualTo(1000);
		assertThat(tiles.totalUsers()).isEqualTo(42);
		assertThat(tiles.testsLast24h()).isEqualTo(10);
		assertThat(tiles.testsLast7d()).isEqualTo(40);
		assertThat(tiles.testsLast30d()).isEqualTo(120);
		assertThat(tiles.inProgress()).isEqualTo(5);
		assertThat(tiles.stuck()).isEqualTo(2);
	}

	@Test
	void familyTotalsAreAllTimeAndUnfilteredAndTheSyntheticFamiliesAreNamed() {
		StatisticsCube cube = cube(
			List.of(runs("2026-03", null, "oidcc-plan", 5), runs("2019-01", null, "fapi1-plan", 100),
				new RunCell("2026-03", null, null, true, "", "", 3, 0, 0, 0, 0, 0)),
			List.of(new PlanCell("2026-03", null, "oidcc-plan", "", "", 2, 1, 0),
				new PlanCell("2019-01", null, "fapi1-plan", "", "", 4, 0, 0)),
			List.of());

		// a slice that shows one family over one month...
		StatisticsOverview overview = slice(cube, query("family", SpecFamilyNames.oidcc, "from", "2026-03"));

		// ...still carries every family's whole history, zero filled in the families' order
		assertThat(overview.familyTotals().keySet()).containsExactlyElementsOf(overview.families());
		assertThat(overview.familyTotals().get(SpecFamilyNames.oidcc))
			.isEqualTo(new StatisticsOverview.FamilyTotals(5, 2, 1));
		assertThat(overview.familyTotals().get(SpecFamilyNames.fapi1Advanced))
			.isEqualTo(new StatisticsOverview.FamilyTotals(100, 4, 0));
		assertThat(overview.familyTotals().get(SpecFamilyResolver.NO_PLAN))
			.isEqualTo(new StatisticsOverview.FamilyTotals(3, 0, 0));
		assertThat(overview.familyTotals().get(SpecFamilyNames.oid4vp))
			.isEqualTo(new StatisticsOverview.FamilyTotals(0, 0, 0));
		assertThat(overview.syntheticFamilies())
			.containsExactly(SpecFamilyResolver.NO_PLAN, SpecFamilyResolver.OTHER_RETIRED);
	}

	@Test
	void unresolvedPlansAreTheTwentyBusiestUnknownPlanNamesAllTime() {
		List<RunCell> cells = new ArrayList<>();
		for (int i = 1; i <= 25; i++) {
			cells.add(runs("2026-03", null, String.format("retired-plan-%02d", i), i));
		}
		cells.add(runs("2026-03", null, "oidcc-plan", 999));
		cells.add(new RunCell("2026-03", null, null, true, "", "", 999, 0, 0, 0, 0, 0));
		cells.add(runs("2026-03", null, null, 999));
		// an old, out of range month: unresolved plans are counted all time
		cells.add(runs("2019-01", null, "retired-plan-01", 40));
		Collections.shuffle(cells, new Random(42));

		StatisticsOverview overview = slice(cube(cells, List.of(), List.of()),
			query("family", SpecFamilyNames.oidcc, "from", "2026-03"));

		assertThat(overview.unresolvedPlans()).hasSize(20);
		// 40 runs in 2019 plus 1 in range, so the busiest even though the range starts in 2026
		assertThat(overview.unresolvedPlans().get(0)).isEqualTo(new UnresolvedPlan("retired-plan-01", 41));
		assertThat(overview.unresolvedPlans()).extracting(UnresolvedPlan::planName)
			.startsWith("retired-plan-01", "retired-plan-25", "retired-plan-24")
			.endsWith("retired-plan-07")
			.doesNotContain("oidcc-plan");
	}

	@Test
	void anAliasedPlanIsChartedUnderItsFamilyAndIsNotReportedAsUnresolved() {
		SpecFamilyResolver resolver = new SpecFamilyResolver(
			Map.of("fapi1-plan", SpecFamilyNames.fapi1Advanced), Map.of(), Map.of(),
			Map.of("fapi-rw-id2-test-plan", SpecFamilyNames.fapi1Advanced));
		StatisticsCube cube = new StatisticsCube(
			List.of(runs("2026-03", null, "fapi1-plan", 4), runs("2026-03", null, "fapi-rw-id2-test-plan", 6),
				runs("2026-03", null, "a-plan-nobody-remembers", 2)),
			List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), NO_TILES, resolver, NOW);

		StatisticsOverview overview = slice(cube, StatisticsQuery.defaults());

		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.fapi1Advanced)).containsExactly(10L);
		assertThat(overview.testRunsByFamily().get(SpecFamilyResolver.OTHER_RETIRED)).containsExactly(2L);
		assertThat(overview.unresolvedPlans())
			.containsExactly(new UnresolvedPlan("a-plan-nobody-remembers", 2));
	}

	@Test
	void anEmptyCubeProducesAnEmptyAxisWithEveryFamilyStillPresent() {
		StatisticsOverview overview = slice(cube(List.of(), List.of(), List.of()), StatisticsQuery.defaults());

		assertThat(overview.periods()).isEmpty();
		assertThat(overview.families()).isEqualTo(RESOLVER.familyOrder());
		assertThat(overview.testRunsByFamily().keySet()).containsExactlyElementsOf(RESOLVER.familyOrder());
		assertThat(overview.testRunsByFamily().values()).allSatisfy(series -> assertThat(series).isEmpty());
		assertThat(overview.resultsByFamily().get(SpecFamilyNames.oidcc).values())
			.allSatisfy(series -> assertThat(series).isEmpty());
		assertThat(overview.users().activeByPeriod()).isEmpty();
		assertThat(overview.users().newByPeriod()).isEmpty();
		assertThat(overview.dimensions().plans()).isEmpty();
		assertThat(overview.dimensions().variants()).isEmpty();
		assertThat(overview.dimensions().certProfiles()).isEmpty();
		assertThat(overview.dimensions().entities()).isEmpty();
		assertThat(overview.unresolvedPlans()).isEmpty();
		assertThat(overview.heatmap()).hasSize(7);
		assertThat(total(overview.heatmap())).isZero();
		assertThat(overview.storage()).isEmpty();
		assertThat(overview.externalHosts()).isEmpty();
	}

	@Test
	void aRangeThatSelectsNothingStillReturnsAWellFormedPayload() {
		StatisticsOverview overview = slice(cube(List.of(runs("2026-03", null, "oidcc-plan", 5)), List.of(), List.of()),
			query("from", "2026-01", "to", "2026-02"));

		assertThat(overview.periods()).isEmpty();
		assertThat(overview.testRunsByFamily().get(SpecFamilyNames.oidcc)).isEmpty();
		assertThat(overview.dimensions().plans()).isEmpty();
		assertThat(total(overview.heatmap())).isZero();
	}

	private static long total(List<List<Long>> heatmap) {
		return heatmap.stream().flatMap(List::stream).mapToLong(Long::longValue).sum();
	}

	private static StatisticsOverview slice(StatisticsCube cube, StatisticsQuery query) {
		return StatisticsSlicer.slice(cube, query);
	}

	private static StatisticsCube cube(List<RunCell> runCells, List<PlanCell> planCells, List<UserTuple> userTuples) {
		return new StatisticsCube(runCells, planCells, userTuples, List.of(), List.of(), List.of(), List.of(),
			NO_TILES, RESOLVER, NOW);
	}

	private static RunCell runs(String month, String week, String planName, long runs) {
		return new RunCell(month, week, planName, false, "", "", runs, 0, 0, 0, 0, 0);
	}

	private static StatisticsQuery query(String... keysAndValues) {
		Map<String, String[]> params = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			params.put(keysAndValues[i], new String[] {keysAndValues[i + 1]});
		}
		return StatisticsQuery.parse(params);
	}
}
