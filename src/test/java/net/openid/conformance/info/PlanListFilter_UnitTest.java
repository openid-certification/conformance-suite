package net.openid.conformance.info;

import net.openid.conformance.plan.TestPlan.SpecFamilyNames;
import net.openid.conformance.statistics.SpecFamilyResolver;
import net.openid.conformance.variant.VariantSelection;
import net.openid.conformance.variant.VariantService;
import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanListFilter_UnitTest {

	/** A plan name the suite no longer publishes; the alias map still knows its family. */
	private static final String RETIRED_PLAN = "fapi-rw-id2-test-plan";

	private static SpecFamilyResolver families;

	@BeforeAll
	static void registry() {
		families = new SpecFamilyResolver(new VariantService(holder -> true));
	}

	private static Map<String, String[]> params(String... keysAndValues) {
		Map<String, String[]> params = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			params.put(keysAndValues[i], new String[] { keysAndValues[i + 1] });
		}
		return params;
	}

	private static Document criteriaOf(String... keysAndValues) {
		return PlanListFilter.parse(params(keysAndValues), families).toCriteria().getCriteriaObject();
	}

	@Test
	void noParametersMeansNoFilterAtAll() {
		PlanListFilter filter = PlanListFilter.parse(params(), families);

		assertThat(filter.isEmpty()).isTrue();
		assertThat(filter.toCriteria().getCriteriaObject()).isEmpty();
	}

	@Test
	void parametersOfOtherEndpointsAreIgnored() {
		PlanListFilter filter = PlanListFilter.parse(
			params("public", "true", "length", "10", "search", "ciba", "order", "started,desc"), families);

		assertThat(filter.isEmpty()).isTrue();
	}

	@Test
	void blankValuesAreIgnored() {
		PlanListFilter filter = PlanListFilter.parse(
			params("family", "", "plan", "   ", "cert", "", "from", "", "to", "", "variant.fapi_profile", " "), families);

		assertThat(filter.isEmpty()).isTrue();
		assertThat(filter.toCriteria().getCriteriaObject()).isEmpty();
	}

	@Test
	void familyMatchesEveryPlanNameOfThatFamily() {
		Set<String> ciba = families.plansEverIn(SpecFamilyNames.fapiCiba);
		assertThat(ciba).isNotEmpty();

		PlanListFilter filter = PlanListFilter.parse(params("family", SpecFamilyNames.fapiCiba), families);

		assertThat(filter.isEmpty()).isFalse();
		assertThat(filter.planNames()).isEqualTo(ciba);
		assertThat(filter.toCriteria().getCriteriaObject())
			.isEqualTo(new Document("planName", new Document("$in", List.copyOf(ciba))));
	}

	@Test
	void familyAlsoMatchesThePlanNamesItHasRetired() {
		// the drill-down has to list what the chart bar counted, and the bar counted the runs
		// of this name under FAPI1 Advanced - see SpecFamilyResolver's alias map
		assertThat(families.isKnownPlan(RETIRED_PLAN)).isFalse();
		assertThat(families.familyForPlan(RETIRED_PLAN)).isEqualTo(SpecFamilyNames.fapi1Advanced);
		assertThat(families.plansIn(SpecFamilyNames.fapi1Advanced)).doesNotContain(RETIRED_PLAN);

		PlanListFilter filter = PlanListFilter.parse(params("family", SpecFamilyNames.fapi1Advanced), families);

		assertThat(filter.planNames()).contains(RETIRED_PLAN)
			.containsAll(families.plansIn(SpecFamilyNames.fapi1Advanced));
	}

	@Test
	void aRetiredPlanNameIsAcceptedOnItsOwnAndWithItsFamily() {
		assertThat(criteriaOf("plan", RETIRED_PLAN)).isEqualTo(new Document("planName", RETIRED_PLAN));
		assertThat(criteriaOf("family", SpecFamilyNames.fapi1Advanced, "plan", RETIRED_PLAN))
			.isEqualTo(new Document("planName", RETIRED_PLAN));
		// ...but only under the family it was actually run for
		assertThat(criteriaOf("family", SpecFamilyNames.oidcc, "plan", RETIRED_PLAN))
			.isEqualTo(new Document("planName", new Document("$in", List.of())));
	}

	@Test
	void anUnknownFamilyMatchesNoPlanAtAll() {
		PlanListFilter filter = PlanListFilter.parse(params("family", "No Such Family"), families);

		assertThat(filter.isEmpty()).isFalse();
		assertThat(filter.planNames()).isEmpty();
		assertThat(filter.toCriteria().getCriteriaObject())
			.isEqualTo(new Document("planName", new Document("$in", List.of())));
	}

	@Test
	void planMatchesThatPlanNameExactly() {
		assertThat(criteriaOf("plan", "fapi-ciba-id1-test-plan"))
			.isEqualTo(new Document("planName", "fapi-ciba-id1-test-plan"));
	}

	@Test
	void planAndFamilyTogetherIntersect() {
		String cibaPlan = families.plansIn(SpecFamilyNames.fapiCiba).iterator().next();

		assertThat(criteriaOf("family", SpecFamilyNames.fapiCiba, "plan", cibaPlan))
			.isEqualTo(new Document("planName", cibaPlan));
		assertThat(criteriaOf("family", SpecFamilyNames.oidcc, "plan", cibaPlan))
			.isEqualTo(new Document("planName", new Document("$in", List.of())));
	}

	@Test
	void everyVariantParameterIsMatchedOnThePlanLevelVariant() {
		Document criteria = criteriaOf(
			"variant.fapi_profile", "openbanking_brazil", "variant.client_auth_type", "mtls");

		assertThat(criteria).isEqualTo(new Document()
			.append("variant.fapi_profile", "openbanking_brazil")
			.append("variant.client_auth_type", "mtls"));
	}

	@Test
	void aLegacyStringVariantIsMatchedInBothShapesAPlanCanStoreItIn() {
		// The statistics cube renames a plain string variant to `legacy` (VariantKeys.LEGACY),
		// so that is what a drill-down into one of its cells sends. No plan has a
		// `variant.legacy` field: a legacy variant is stored either as the bare string it
		// always was, or as the one-entry sub-document VariantSelection is written out as
		// (VariantConverters reads both back), and the cube counts them as the same cell.
		assertThat(criteriaOf("variant.legacy", "openbanking_brazil")).isEqualTo(legacyIs("openbanking_brazil"));

		assertThat(criteriaOf("variant.legacy", "openbanking_brazil", "variant.client_auth_type", "mtls"))
			.isEqualTo(legacyIs("openbanking_brazil").append("variant.client_auth_type", "mtls"));
	}

	@Test
	void theStoredNameOfTheLegacyVariantMeansTheSameFilterAsTheCubesNameForIt() {
		assertThat(criteriaOf("variant." + VariantSelection.LEGACY_VARIANT_NAME, "openbanking_brazil"))
			.isEqualTo(legacyIs("openbanking_brazil"));

		// both spellings name one filter, and a query document cannot carry it twice
		assertThat(criteriaOf("variant.legacy", "openbanking_brazil",
			"variant." + VariantSelection.LEGACY_VARIANT_NAME, "openbanking_brazil"))
			.isEqualTo(legacyIs("openbanking_brazil"));
	}

	/** @return the criteria document a legacy variant filter produces */
	private static Document legacyIs(String value) {
		return new Document("$or", List.of(
			new Document("variant", value),
			new Document("variant." + VariantSelection.LEGACY_VARIANT_NAME, value)));
	}

	@Test
	void aVariantParameterWithoutANameIsRejected() {
		assertThatThrownBy(() -> PlanListFilter.parse(params("variant.", "mtls"), families))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("variant.");
	}

	@Test
	void aVariantParameterNameThatCouldNotBeAFieldNameIsRejected() {
		assertThatThrownBy(() -> PlanListFilter.parse(params("variant.$where", "1"), families))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("$where");
	}

	@Test
	void certMatchesOneOfTheCertificationProfilesOfThePlan() {
		assertThat(criteriaOf("cert", "FAPI-CIBA: Poll w/ MTLS"))
			.isEqualTo(new Document("certificationProfileName", "FAPI-CIBA: Poll w/ MTLS"));
	}

	@Test
	void immutableTrueMatchesOnlyThePlansACertificationPackageWasDownloadedFor() {
		assertThat(criteriaOf("immutable", "true")).isEqualTo(new Document("immutable", true));
	}

	@Test
	void immutableFalseAlsoMatchesThePlansThatHaveNoSuchFieldAtAll() {
		// the field is only written when a certification package is downloaded, so every plan
		// before that has none; $eq false would list none of them
		assertThat(criteriaOf("immutable", "false"))
			.isEqualTo(new Document("immutable", new Document("$ne", true)));
	}

	@Test
	void immutableIsReadWhateverCaseItIsSentIn() {
		assertThat(criteriaOf("immutable", "TRUE")).isEqualTo(new Document("immutable", true));
		assertThat(criteriaOf("immutable", "False"))
			.isEqualTo(new Document("immutable", new Document("$ne", true)));
	}

	@Test
	void anImmutableThatIsNeitherTrueNorFalseIsRejected() {
		// rather than reading anything that is not "true" as false, which would quietly turn a
		// typo into a filter that lists the opposite of what was asked for
		assertThatThrownBy(() -> PlanListFilter.parse(params("immutable", "yes"), families))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("immutable")
			.hasMessageContaining("yes");
	}

	@Test
	void fromAndToAreAHalfOpenRangeOnStarted() {
		assertThat(criteriaOf("from", "2026-06-01", "to", "2026-07-01"))
			.isEqualTo(new Document("started", new Document("$gte", "2026-06-01").append("$lt", "2026-07-01")));
		assertThat(criteriaOf("from", "2026-06-01"))
			.isEqualTo(new Document("started", new Document("$gte", "2026-06-01")));
		assertThat(criteriaOf("to", "2026-07-01"))
			.isEqualTo(new Document("started", new Document("$lt", "2026-07-01")));
	}

	@Test
	void fullTimestampsAreNormalisedToUtcSoTheyCompareAgainstStarted() {
		assertThat(criteriaOf("from", "2026-06-01T10:00:00Z", "to", "2026-06-01T14:30:00.500+02:00"))
			.isEqualTo(new Document("started",
				new Document("$gte", "2026-06-01T10:00:00").append("$lt", "2026-06-01T12:30:00")));
	}

	@Test
	void aTimestampBoundIsAnExactSecondBoundaryAtBothEnds() {
		// stored timestamps carry a fraction only when it is not zero, so a bound has to be a
		// prefix of the second it names to be able to compare against both forms
		PlanListFilter filter = PlanListFilter.parse(
			params("from", "2026-06-02T15:54:10Z", "to", "2026-06-02T15:54:12.999Z"), families);

		assertThat(filter.from()).isEqualTo("2026-06-02T15:54:10");
		assertThat(filter.to()).isEqualTo("2026-06-02T15:54:12");

		// from includes the whole of the second it names ...
		assertThat("2026-06-02T15:54:10Z").isGreaterThan(filter.from());
		assertThat("2026-06-02T15:54:10.162803375Z").isGreaterThan(filter.from());
		assertThat("2026-06-02T15:54:09.999Z").isLessThan(filter.from());
		// ... and to excludes the whole of the second it names
		assertThat("2026-06-02T15:54:12Z").isGreaterThanOrEqualTo(filter.to());
		assertThat("2026-06-02T15:54:12.000000001Z").isGreaterThanOrEqualTo(filter.to());
		assertThat("2026-06-02T15:54:11.999Z").isLessThan(filter.to());
	}

	@Test
	void aRangeThatStartsWhereItEndsMatchesNothing() {
		Document criteria = criteriaOf("from", "2026-06-01T00:00:00Z", "to", "2026-06-01T00:00:00Z");

		assertThat(criteria).isEqualTo(new Document("started",
			new Document("$gte", "2026-06-01T00:00:00").append("$lt", "2026-06-01T00:00:00")));
	}

	@Test
	void aFromOrToThatIsNotADateOrTimestampIsRejected() {
		assertThatThrownBy(() -> PlanListFilter.parse(params("from", "nonsense"), families))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("from")
			.hasMessageContaining("nonsense");
		assertThatThrownBy(() -> PlanListFilter.parse(params("to", "2026-13-01"), families))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("to");
		assertThatThrownBy(() -> PlanListFilter.parse(params("from", "2026-06-01T10:00:00"), families))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("from");
	}

	@Test
	void everyFilterAppliesAtOnce() {
		String cibaPlan = families.plansIn(SpecFamilyNames.fapiCiba).iterator().next();

		Document criteria = criteriaOf(
			"family", SpecFamilyNames.fapiCiba,
			"plan", cibaPlan,
			"variant.fapi_profile", "openbanking_brazil",
			"cert", "a profile",
			"immutable", "true",
			"from", "2026-06-01",
			"to", "2026-07-01");

		assertThat(criteria).isEqualTo(new Document()
			.append("planName", cibaPlan)
			.append("variant.fapi_profile", "openbanking_brazil")
			.append("certificationProfileName", "a profile")
			.append("immutable", true)
			.append("started", new Document("$gte", "2026-06-01").append("$lt", "2026-07-01")));
	}

	@Test
	void onlyTheFirstValueOfARepeatedParameterIsUsed() {
		Map<String, String[]> params = new LinkedHashMap<>();
		params.put("plan", new String[] { "fapi-ciba-id1-test-plan", "oidcc-basic-certification-test-plan" });

		assertThat(PlanListFilter.parse(params, families).toCriteria().getCriteriaObject())
			.isEqualTo(new Document("planName", "fapi-ciba-id1-test-plan"));
	}
}
