package net.openid.conformance.variant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modules that test a rule which exists only in specific ecosystems declare those ecosystems with
 * {@code @VariantApplicableOnly}. Each must stay applicable for exactly the listed profile values:
 * a new profile value must not pick the module up, and the ecosystem it is for must not lose it.
 */
class EcosystemSpecificModuleApplicability_UnitTest {

	private static VariantService variantService;

	@BeforeAll
	static void setUp() {
		variantService = new VariantService(holder -> true);
	}

	@ParameterizedTest(name = "{0} is only applicable for {2}")
	@CsvSource(delimiter = '|', value = {
		// FAPI1 Advanced Final
		"fapi1-advanced-final-brazil-ensure-bad-payment-signature-fails | fapi_profile | openbanking_brazil",
		"fapi1-advanced-final-client-test-payment-consent-response-valid-aud-as-array | fapi_profile | openbanking_brazil",
		"fapi1-advanced-final-client-brazildcr-happypath-test | fapi_profile | openbanking_brazil openinsurance_brazil",
		"fapi1-advanced-final-ensure-server-handles-non-matching-intent-id | fapi_profile | openbanking_uk",
		"fapi1-advanced-final-test-essential-acr-sca-claim | fapi_profile | openbanking_uk",
		"fapi1-advanced-final-client-test-invalid-openbanking-intent-id | fapi_profile | openbanking_uk consumerdataright_au",
	})
	void moduleIsApplicableForExactlyItsEcosystems(String moduleName, String parameterName, String expectedValues) {
		VariantService.TestModuleHolder module = variantService.getTestModule(moduleName);

		Set<String> allowed = module.parameters.stream()
			.filter(p -> p.parameter.variantParameter.name().equals(parameterName))
			.findFirst()
			.orElseThrow(() -> new AssertionError(moduleName + " has no variant parameter " + parameterName))
			.allowedValues.stream()
			.map(Object::toString)
			.collect(toSet());

		assertThat(allowed).containsExactlyInAnyOrderElementsOf(Arrays.asList(expectedValues.trim().split(" ")));
	}
}
