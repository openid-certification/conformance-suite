package net.openid.conformance.statistics;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class StatisticsQuery_UnitTest {

	@Test
	void anEmptyRequestIsTheUnfilteredMonthlyQuery() {
		StatisticsQuery query = StatisticsQuery.parse(Map.of());

		assertThat(query).isEqualTo(StatisticsQuery.defaults());
		assertThat(query.granularity()).isEqualTo(Granularity.MONTH);
		assertThat(query.from()).isNull();
		assertThat(query.to()).isNull();
		assertThat(query.family()).isNull();
		assertThat(query.plan()).isNull();
		assertThat(query.cert()).isNull();
		assertThat(query.variant()).isEmpty();
	}

	@Test
	void everyFilterIsRead() {
		StatisticsQuery query = StatisticsQuery.parse(params(
			"granularity", "week",
			"from", "2026-01-05",
			"to", "2026-03-15",
			"family", "FAPI1 Advanced",
			"plan", "fapi1-advanced-final-test-plan",
			"variant.fapi_profile", "openbanking_brazil",
			"variant.client_auth_type", "mtls",
			"cert", "FAPI Adv. OP w/ MTLS"));

		assertThat(query.granularity()).isEqualTo(Granularity.WEEK);
		assertThat(query.from()).isEqualTo("2026-01-05");
		// a mid-week 'to' is snapped to the Monday of its ISO week
		assertThat(query.to()).isEqualTo("2026-03-09");
		assertThat(query.family()).isEqualTo("FAPI1 Advanced");
		assertThat(query.plan()).isEqualTo("fapi1-advanced-final-test-plan");
		assertThat(query.cert()).isEqualTo("FAPI Adv. OP w/ MTLS");
		assertThat(query.variant()).containsOnly(
			Map.entry("fapi_profile", "openbanking_brazil"),
			Map.entry("client_auth_type", "mtls"));
	}

	@Test
	void blankValuesAndUnknownParametersAreIgnored() {
		StatisticsQuery query = StatisticsQuery.parse(params(
			"granularity", "  ",
			"from", "",
			"family", "   ",
			"variant.fapi_profile", "",
			"refresh", "true",
			"something_else", "x"));

		assertThat(query).isEqualTo(StatisticsQuery.defaults());
	}

	@Test
	void valuesAreTrimmed() {
		StatisticsQuery query = StatisticsQuery.parse(params("granularity", " month ", "plan", " oidcc-plan "));

		assertThat(query.granularity()).isEqualTo(Granularity.MONTH);
		assertThat(query.plan()).isEqualTo("oidcc-plan");
	}

	@Test
	void anUnknownGranularityIsRejectedWithAReadableMessage() {
		assertThatThrownBy(() -> StatisticsQuery.parse(params("granularity", "yearly")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("granularity")
			.hasMessageContaining("month")
			.hasMessageContaining("week");
	}

	@Test
	void aRangeMustUseThePeriodFormatOfTheGranularity() {
		assertThatThrownBy(() -> StatisticsQuery.parse(params("from", "2026-01-05")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("from")
			.hasMessageContaining("YYYY-MM");
		assertThatThrownBy(() -> StatisticsQuery.parse(params("granularity", "week", "to", "2026-01")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("to")
			.hasMessageContaining("YYYY-MM-DD");
	}

	@Test
	void fromMustNotBeAfterTo() {
		assertThatThrownBy(() -> StatisticsQuery.parse(params("from", "2026-05", "to", "2026-03")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("2026-05")
			.hasMessageContaining("2026-03");

		assertThat(StatisticsQuery.parse(params("from", "2026-03", "to", "2026-03")).from()).isEqualTo("2026-03");
	}

	@Test
	void aVariantParameterMustBeNamed() {
		assertThatThrownBy(() -> StatisticsQuery.parse(params("variant.", "mtls")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("variant.");
	}

	@Test
	void aVariantParameterNameThatIsNotOneIsRejected() {
		// the same parameters reach the plan listing, where the name becomes a field path
		assertThatThrownBy(() -> StatisticsQuery.parse(params("variant.$where", "1")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("$where");

		assertThat(StatisticsQuery.parse(params("variant.__variant__", "legacy")).variant())
			.containsExactly(entry("__variant__", "legacy"));
	}

	@Test
	void theFirstValueWinsWhenAParameterIsRepeated() {
		Map<String, String[]> params = new LinkedHashMap<>();
		params.put("plan", new String[] {"oidcc-plan", "fapi1-plan"});
		params.put("family", new String[] {});

		StatisticsQuery query = StatisticsQuery.parse(params);

		assertThat(query.plan()).isEqualTo("oidcc-plan");
		assertThat(query.family()).isNull();
	}

	@Test
	void aQueryCanBeRelaxedByOneFilterAtATime() {
		StatisticsQuery query = StatisticsQuery.parse(Map.of(
			"family", new String[] {"OIDCC"}, "plan", new String[] {"oidcc-plan"},
			"variant.a", new String[] {"1"}, "variant.b", new String[] {"2"}, "cert", new String[] {"Cert A"}));

		assertThat(query.withoutPlan()).isEqualTo(new StatisticsQuery(Granularity.MONTH, null, null, "OIDCC", null,
			Map.of("a", "1", "b", "2"), "Cert A"));
		assertThat(query.withoutCert()).isEqualTo(new StatisticsQuery(Granularity.MONTH, null, null, "OIDCC",
			"oidcc-plan", Map.of("a", "1", "b", "2"), null));
		assertThat(query.withoutVariant("a")).isEqualTo(new StatisticsQuery(Granularity.MONTH, null, null, "OIDCC",
			"oidcc-plan", Map.of("b", "2"), "Cert A"));
		// relaxing what is not there changes nothing
		assertThat(query.withoutVariant("c")).isEqualTo(query);
		assertThat(StatisticsQuery.defaults().withoutPlan()).isEqualTo(StatisticsQuery.defaults());
	}

	@Test
	void theVariantMapIsImmutable() {
		StatisticsQuery query = StatisticsQuery.parse(params("variant.fapi_profile", "plain"));

		assertThatThrownBy(() -> query.variant().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
	}

	private static Map<String, String[]> params(String... keysAndValues) {
		Map<String, String[]> params = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			params.put(keysAndValues[i], new String[] {keysAndValues[i + 1]});
		}
		return params;
	}
}
