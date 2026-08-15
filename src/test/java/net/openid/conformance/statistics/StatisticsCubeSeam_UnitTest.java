package net.openid.conformance.statistics;

import net.openid.conformance.plan.TestPlan.ProfileNames;
import net.openid.conformance.plan.TestPlan.SpecFamilyNames;
import net.openid.conformance.variant.VariantSelection;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The seam between MongoDB and the cube: rows shaped exactly as the aggregations of
 * {@link MongoStatisticsSource} deliver them, fed through the conversion the source really
 * uses and then through {@link StatisticsCube}.
 *
 * <p>{@link StatisticsCube_UnitTest} starts from cells that already carry canonical keys, so
 * it cannot see the one thing that can silently split a cell in production: the runs
 * pipeline (A') groups on the {@code variant} sub-document as MongoDB stored it, and the
 * plans pipeline (B') does the same over a different collection, so the two only agree - and
 * the roll-up only merges two plans configured the same way - because both sides go through
 * {@link VariantKeys#canonical} and {@link CertKeys#canonical}. That is what these tests
 * hold onto.
 */
class StatisticsCubeSeam_UnitTest {

	/** A Thursday; the Monday of its ISO week is 2026-03-09. */
	private static final LocalDate NOW = LocalDate.of(2026, 3, 12);

	private static final String MONTH = "2026-02";

	private static final String WEEK = "2026-02-23";

	private static final String PLAN = "oidcc-plan";

	private static final TileRow NO_TILES = new TileRow(0, 0, 0, 0, 0, 0, 0);

	private static final SpecFamilyResolver RESOLVER = new SpecFamilyResolver(
		Map.of(PLAN, SpecFamilyNames.oidcc), Map.of(PLAN, ProfileNames.rptest));

	/** The two orders MongoDB may have stored the same variant selection in. */
	private static final Document VARIANT_ONE_ORDER =
		new Document("client_auth_type", "mtls").append("fapi_profile", "plain");

	private static final Document VARIANT_OTHER_ORDER =
		new Document("fapi_profile", "plain").append("client_auth_type", "mtls");

	private static final List<String> TWO_PROFILES = List.of("FAPI-CIBA: Poll w/ MTLS", "FAPI2: DCR");

	@Test
	void twoPlansConfiguredTheSameWayAreOneCellWhateverOrderMongoStoredTheirVariantIn() {
		StatisticsCube cube = cube(
			List.of(runRow(VARIANT_ONE_ORDER, TWO_PROFILES, 3), runRow(VARIANT_OTHER_ORDER, TWO_PROFILES, 4)),
			List.of(planRow(VARIANT_ONE_ORDER, TWO_PROFILES, 1), planRow(VARIANT_OTHER_ORDER, TWO_PROFILES, 2)));

		assertThat(cube.runs(Granularity.MONTH)).singleElement().satisfies(cell -> {
			assertThat(cell.runs()).isEqualTo(7);
			assertThat(cell.variantKey()).isEqualTo("client_auth_type=mtls;fapi_profile=plain");
			assertThat(cell.certKey()).isEqualTo("FAPI-CIBA: Poll w/ MTLS | FAPI2: DCR");
		});
		assertThat(cube.plans(Granularity.MONTH)).singleElement()
			.extracting(PlanCell::plans).isEqualTo(3L);
		// ... and the weekly cells, which are rolled up separately, say the same
		assertThat(cube.runs(Granularity.WEEK)).singleElement().extracting(RunCell::runs).isEqualTo(7L);
		assertThat(cube.plans(Granularity.WEEK)).singleElement().extracting(PlanCell::plans).isEqualTo(3L);
	}

	@Test
	void theRunsAndThePlansCellsOfOnePlanCarryTheSameVariantAndCertificationKeys() {
		StatisticsCube cube = cube(List.of(runRow(VARIANT_ONE_ORDER, TWO_PROFILES, 3)),
			List.of(planRow(VARIANT_OTHER_ORDER, TWO_PROFILES, 1)));

		RunCell runs = cube.runs(Granularity.MONTH).get(0);
		PlanCell plans = cube.plans(Granularity.MONTH).get(0);
		// what makes one filter narrow both series to the same slice
		assertThat(plans.variantKey()).isEqualTo(runs.variantKey());
		assertThat(plans.certKey()).isEqualTo(runs.certKey());
		assertThat(cube.variantOf(runs.variantKey()))
			.containsExactlyInAnyOrderEntriesOf(Map.of("client_auth_type", "mtls", "fapi_profile", "plain"));
	}

	@Test
	void aCertificationProfileListBecomesOneKeyAndABareNameTheSameOne() {
		StatisticsCube cube = cube(
			List.of(runRow(VARIANT_ONE_ORDER, List.of("OpenID Connect Basic OP"), 3),
				runRow(VARIANT_ONE_ORDER, "OpenID Connect Basic OP", 4)),
			List.of());

		assertThat(cube.runs(Granularity.MONTH)).singleElement().satisfies(cell -> {
			assertThat(cell.runs()).isEqualTo(7);
			assertThat(cell.certKey()).isEqualTo("OpenID Connect Basic OP");
		});
	}

	@Test
	void aLegacyStringVariantIsCanonicalisedTheSameWayOnBothSides() {
		// what a plan created before variants were structured stores instead of a sub-document
		String legacy = "openbanking_brazil";
		StatisticsCube cube = cube(List.of(runRow(legacy, List.of(), 3)), List.of(planRow(legacy, List.of(), 1)));

		String expected = VariantKeys.LEGACY + "=" + legacy;
		assertThat(cube.runs(Granularity.MONTH)).singleElement()
			.extracting(RunCell::variantKey).isEqualTo(expected);
		assertThat(cube.plans(Granularity.MONTH)).singleElement()
			.extracting(PlanCell::variantKey).isEqualTo(expected);
		// the parameter name a drill-down carries; the plan itself stores the string under
		// VariantSelection.LEGACY_VARIANT_NAME, which is what PlanListFilter maps it back to
		assertThat(cube.variantOf(expected)).containsExactly(Map.entry(VariantKeys.LEGACY, legacy));
		assertThat(VariantKeys.LEGACY).isNotEqualTo(VariantSelection.LEGACY_VARIANT_NAME);
	}

	@Test
	void aStandaloneRunHasNoPlanAndIsKeptApartFromEveryPlansCell() {
		Document standalone = new Document("_id", new Document("month", MONTH)
				.append("week", WEEK)
				// both missing, exactly as the pipeline projects an unmatched $lookup
				.append("standalone", true))
			.append("runs", 5);
		StatisticsCube cube = cube(List.of(standalone, runRow(VARIANT_ONE_ORDER, TWO_PROFILES, 3)), List.of());

		assertThat(cube.runs(Granularity.MONTH))
			.extracting(RunCell::planName, RunCell::standalone, RunCell::variantKey, RunCell::runs)
			.containsExactlyInAnyOrder(
				tuple(null, true, "", 5L),
				tuple(PLAN, false, "client_auth_type=mtls;fapi_profile=plain", 3L));
		assertThat(cube.familyOf(null)).isEqualTo(SpecFamilyResolver.OTHER_RETIRED);
	}

	/**
	 * @param variant a raw {@code variant} field: a sub-document or a legacy plain string
	 * @param cert    a raw {@code certificationProfileName} field: a list or a bare name
	 * @param runs    how many runs the row counts
	 * @return one row of the runs aggregation (A')
	 */
	private static Document runRow(Object variant, Object cert, long runs) {
		return new Document("_id", new Document("month", MONTH)
				.append("week", WEEK)
				.append("planName", PLAN)
				.append("standalone", false)
				.append("variant", variant)
				.append("cert", cert))
			.append("runs", runs)
			.append("passed", runs)
			.append("failed", 0);
	}

	/**
	 * @param variant a raw {@code variant} field: a sub-document or a legacy plain string
	 * @param cert    a raw {@code certificationProfileName} field: a list or a bare name
	 * @param plans   how many plans the row counts
	 * @return one row of the plans aggregation (B')
	 */
	private static Document planRow(Object variant, Object cert, long plans) {
		return new Document("_id", new Document("month", MONTH)
				.append("week", WEEK)
				.append("planName", PLAN)
				.append("variant", variant)
				.append("cert", cert))
			.append("plans", plans)
			.append("certified", 0)
			.append("published", plans);
	}

	/**
	 * @param runRows  rows as the runs aggregation delivers them
	 * @param planRows rows as the plans aggregation delivers them
	 * @return the cube they build, through the source's own conversion
	 */
	private static StatisticsCube cube(List<Document> runRows, List<Document> planRows) {
		return new StatisticsCube(
			runRows.stream().map(MongoStatisticsSource::runCell).toList(),
			planRows.stream().map(MongoStatisticsSource::planCell).toList(),
			List.of(), List.of(), List.of(), List.of(), NO_TILES, RESOLVER, NOW);
	}
}
