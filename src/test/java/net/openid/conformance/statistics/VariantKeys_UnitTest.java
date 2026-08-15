package net.openid.conformance.statistics;

import net.openid.conformance.variant.VariantSelection;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VariantKeys_UnitTest {

	@Test
	void keysAreSortedSoTheSameSelectionAlwaysProducesTheSameKey() {
		Document variant = new Document("server_metadata", "discovery")
			.append("client_auth_type", "mtls")
			.append("fapi_profile", "openbanking_brazil");
		Document sameInAnotherOrder = new Document("fapi_profile", "openbanking_brazil")
			.append("server_metadata", "discovery")
			.append("client_auth_type", "mtls");

		assertThat(VariantKeys.canonical(variant))
			.isEqualTo("client_auth_type=mtls;fapi_profile=openbanking_brazil;server_metadata=discovery");
		assertThat(VariantKeys.canonical(sameInAnotherOrder)).isEqualTo(VariantKeys.canonical(variant));
	}

	@Test
	void aPlainStringVariantBecomesTheLegacyKey() {
		assertThat(VariantKeys.canonical("openbanking_brazil")).isEqualTo("legacy=openbanking_brazil");
	}

	@Test
	void aVariantMapHoldingOnlyTheLegacyPlaceholderAlsoBecomesTheLegacyKey() {
		assertThat(VariantKeys.canonical(new Document(VariantSelection.LEGACY_VARIANT_NAME, "plain")))
			.isEqualTo("legacy=plain");
	}

	@Test
	void anAbsentOrEmptyVariantIsTheEmptyKey() {
		assertThat(VariantKeys.canonical(null)).isEmpty();
		assertThat(VariantKeys.canonical("")).isEmpty();
		assertThat(VariantKeys.canonical("  ")).isEmpty();
		assertThat(VariantKeys.canonical(new Document())).isEmpty();
		assertThat(VariantKeys.canonical(Map.of())).isEmpty();
	}

	@Test
	void nonStringValuesAreStringifiedAndNullValuesAreDropped() {
		Map<String, Object> variant = new LinkedHashMap<>();
		variant.put("retries", 3);
		variant.put("enabled", true);
		variant.put("missing", null);

		assertThat(VariantKeys.canonical(variant)).isEqualTo("enabled=true;retries=3");
	}

	@Test
	void parseSplitsACanonicalKeyBackIntoItsPairs() {
		Map<String, String> parsed = VariantKeys.parse("client_auth_type=mtls;fapi_profile=openbanking_brazil");

		assertThat(parsed).containsExactly(
			Map.entry("client_auth_type", "mtls"),
			Map.entry("fapi_profile", "openbanking_brazil"));
	}

	@Test
	void parseToleratesMalformedAndEmptyKeys() {
		assertThat(VariantKeys.parse(null)).isEmpty();
		assertThat(VariantKeys.parse("")).isEmpty();
		assertThat(VariantKeys.parse("a=1;;no-equals;b=2")).containsExactly(
			Map.entry("a", "1"), Map.entry("b", "2"));
		assertThat(VariantKeys.parse("a=1=2")).containsExactly(Map.entry("a", "1=2"));
	}

	@Test
	void separatorsInsideAValueAreReplacedSoTheKeyStaysParseable() {
		assertThat(VariantKeys.canonical(Map.of("k", "a;b=c"))).isEqualTo("k=a_b_c");
		assertThat(VariantKeys.parse(VariantKeys.canonical(Map.of("k", "a;b=c"))))
			.containsExactly(Map.entry("k", "a_b_c"));
	}
}
