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
		// FAPI-CIBA ID1, Brazil
		"fapi-ciba-id1-brazil-discovery-end-point-verification | fapi_ciba_profile | openbanking_brazil",
		"fapi-ciba-id1-ensure-authorization-request-with-invalid-login-hint-fails-for-brazil | fapi_ciba_profile | openbanking_brazil",
		"fapi-ciba-id1-ensure-authorization-request-with-url-binding-message-warns-for-brazil | fapi_ciba_profile | openbanking_brazil",
		"fapi-ciba-id1-ensure-authorization-request-with-user-code-fails-for-brazil | fapi_ciba_profile | openbanking_brazil",
		"fapi-ciba-id1-ensure-requested-expiry-above-maximum-is-capped-for-brazil | fapi_ciba_profile | openbanking_brazil",
		"fapi-ciba-id1-ensure-requested-expiry-negative-fails-for-brazil | fapi_ciba_profile | openbanking_brazil",
		"fapi-ciba-id1-ensure-requested-expiry-zero-fails-for-brazil | fapi_ciba_profile | openbanking_brazil",
		"fapi-ciba-id1-ping-notification-endpoint-retries-after-transient-error-for-brazil | fapi_ciba_profile | openbanking_brazil",
		"fapi-ciba-id1-client-brazildcr-happypath-test | fapi_ciba_profile | openbanking_brazil",
		"fapi-ciba-id1-client-ping-duplicate-notification-test | fapi_ciba_profile | openbanking_brazil",
		"fapi-ciba-id1-client-ping-mode-poll-fallback-test | fapi_ciba_profile | openbanking_brazil",
		"fapi-ciba-id1-client-ping-with-wrong-auth-req-id-test | fapi_ciba_profile | openbanking_brazil",
		"fapi-ciba-id1-client-ping-without-bearer-token-test | fapi_ciba_profile | openbanking_brazil",
		// FAPI-CIBA ID1, ConnectID
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-3ds-card-login-hint-succeeds | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-3ds-payment-authorization-details-succeeds | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-id-token-hint-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-login-hint-token-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-malformed-3ds-payment-amount-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-malformed-3ds-payment-currency-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-malformed-3ds-payment-source-account-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-malformed-card-login-hint-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-non-ascii-purpose | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-purpose-succeeds | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-too-long-purpose-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-too-short-purpose-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-with-wrong-3ds-payment-authorization-details-type-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-without-3ds-payment-amount-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-without-3ds-payment-beneficiary-name-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-without-3ds-payment-currency-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-without-3ds-payment-desc-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-without-3ds-payment-instructed-amount-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-without-3ds-payment-source-account-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-authorization-request-without-binding-message-fails | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-connectid-ensure-txn-returned-when-not-requested | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-client-connectid-invalid-missing-trust-framework-test | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-client-connectid-invalid-missing-txn-test | fapi_ciba_profile | connectid_au",
		"fapi-ciba-id1-client-connectid-invalid-wrong-trust-framework-test | fapi_ciba_profile | connectid_au",
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
