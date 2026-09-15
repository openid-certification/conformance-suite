package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantSelection;
import net.openid.conformance.variant.VariantService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The ecosystem-specific FAPI2 Final modules exercise a rule that exists only in one ecosystem,
 * so each must be applicable for exactly that {@code fapi_profile} value: a new profile value
 * must not pick them up, and the ecosystem they are for must not lose them.
 */
class FAPI2ProfileSpecificModuleApplicability_UnitTest {

	private static VariantService variantService;

	@BeforeAll
	static void setUp() {
		variantService = new VariantService(holder -> true);
	}

	// The one combination AU-CDR permits, which every other profile also accepts.
	private static VariantSelection serverVariant(FAPI2FinalOPProfile profile) {
		Map<String, String> variant = new HashMap<>();
		variant.put("fapi_profile", profile.toString());
		variant.put("client_auth_type", "private_key_jwt");
		variant.put("sender_constrain", "mtls");
		variant.put("openid", "openid_connect");
		variant.put("fapi_response_mode", "jarm");
		variant.put("fapi_request_method", "signed_non_repudiation");
		variant.put("authorization_request_type", "simple");
		variant.put("grant_management", "disabled");
		return new VariantSelection(variant);
	}

	@ParameterizedTest(name = "{0} is only applicable for {1}")
	@CsvSource({
		"fapi2-security-profile-final-brazil-ensure-bad-payment-signature-fails, openbanking_brazil",
		"fapi2-security-profile-final-australia-connectid-ensure-invalid-purpose-fails, connectid_au",
		"fapi2-security-profile-final-australia-connectid-ensure-request-object-with-nbf-over-15-fails, connectid_au",
		"fapi2-security-profile-final-australia-connectid-ensure-request-object-with-exp-over-10-fails, connectid_au",
		"fapi2-security-profile-final-australia-connectid-test-claims-parameter-idtoken-identity-claims, connectid_au",
		"fapi2-security-profile-final-ksa-ensure-request-object-with-exp-over-10-fails, ksa",
		"fapi2-security-profile-final-ksa-ensure-request-object-with-nbf-over-10-fails, ksa",
		"fapi2-security-profile-final-cdr-arrangement-amendment, consumerdataright_au",
		"fapi2-security-profile-final-cdr-negative-sharing-duration, consumerdataright_au",
		"fapi2-security-profile-final-cdr-sharing-duration-zero, consumerdataright_au",
		"fapi2-security-profile-final-cdr-unrecognised-arrangement-id, consumerdataright_au",
		"fapi2-security-profile-final-cdr-refresh-token-introspection-expiry, consumerdataright_au",
	})
	void moduleIsApplicableForExactlyItsEcosystem(String moduleName, String expectedProfile) {
		VariantService.TestModuleHolder module = variantService.getTestModule(moduleName);

		Set<String> applicableProfiles = Arrays.stream(FAPI2FinalOPProfile.values())
			.filter(profile -> module.isApplicableForVariant(serverVariant(profile)))
			.map(FAPI2FinalOPProfile::toString)
			.collect(toSet());

		assertThat(applicableProfiles).containsExactly(expectedProfile);
	}
}
