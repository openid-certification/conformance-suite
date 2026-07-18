package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.info.Plan;
import net.openid.conformance.variant.VariantSelection;
import net.openid.conformance.variant.VariantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Grant management requires an authorization flow that yields a grant, so the grant-management OP
 * modules must not apply under the {@code fapi_client_credentials_grant} profile (which has no such
 * flow). The test plans additionally exclude that profile at the plan level, but that guard only
 * fires when a plan is instantiated through the UI/plan API; a module invoked directly with a
 * config (as the integration tests do) is only protected by the module's own
 * {@code @VariantNotApplicable}. This is a regression test for grant-management modules being
 * applicable under the client credentials grant profile — see
 * {@link AbstractFAPI2SPFinalGrantManagementTestModule}.
 */
class FAPI2MessageSigningFinalGrantManagementApplicability_UnitTest {

	private static final String PLAN_NAME = "fapi2-message-signing-final-test-plan";

	// One module from the shared GM base, and the two GM modules that keep a different behavioural
	// parent and declare the profile exclusion inline.
	private static final List<String> GM_MODULE_NAMES = List.of(
		"fapi2-security-profile-final-grant-management-happy-flow",
		"fapi2-security-profile-final-grant-management-ensure-wrong-client-cannot-query-grant",
		"fapi2-security-profile-final-grant-management-ensure-invalid-grant-id-fails"
	);

	// Client (RP) GM modules, sharing AbstractFAPI2SPFinalClientTestGrantManagement.
	private static final List<String> CLIENT_GM_MODULE_NAMES = List.of(
		"fapi2-security-profile-final-client-test-grant-management-happy-path",
		"fapi2-security-profile-final-client-test-grant-management-invalid-grant-id-fails"
	);

	private VariantService variantService;

	@BeforeEach
	void setUp() {
		variantService = new VariantService(holder -> true);
	}

	@Test
	void grantManagementModulesAreNotApplicableForClientCredentialsGrant() {
		Map<String, String> variant = new HashMap<>();
		variant.put("fapi_profile", "fapi_client_credentials_grant");
		variant.put("client_auth_type", "private_key_jwt");
		variant.put("sender_constrain", "dpop");
		variant.put("authorization_request_type", "simple");
		VariantSelection selection = new VariantSelection(variant);

		for (String moduleName : GM_MODULE_NAMES) {
			assertThat(variantService.getTestModule(moduleName).isApplicableForVariant(selection))
				.as("%s must not be applicable for fapi_client_credentials_grant", moduleName)
				.isFalse();
		}
	}

	@Test
	void grantManagementModulesAreApplicableForPlainFapiWhenEnabled() {
		// Positive control: with a real authorization-flow profile and grant management enabled, the
		// grant-management modules must still apply.
		Map<String, String> variant = new HashMap<>();
		variant.put("fapi_profile", "plain_fapi");
		variant.put("client_auth_type", "private_key_jwt");
		variant.put("sender_constrain", "dpop");
		variant.put("openid", "plain_oauth");
		variant.put("fapi_response_mode", "plain_response");
		variant.put("fapi_request_method", "unsigned");
		variant.put("authorization_request_type", "simple");
		variant.put("grant_management", "enabled");
		VariantSelection selection = new VariantSelection(variant);

		for (String moduleName : GM_MODULE_NAMES) {
			assertThat(variantService.getTestModule(moduleName).isApplicableForVariant(selection))
				.as("%s must be applicable for plain_fapi with grant management enabled", moduleName)
				.isTrue();
		}
	}

	@Test
	void clientGrantManagementModulesAreNotApplicableForClientCredentialsGrant() {
		Map<String, String> variant = new HashMap<>();
		variant.put("fapi_profile", "fapi_client_credentials_grant");
		variant.put("client_auth_type", "private_key_jwt");
		variant.put("sender_constrain", "dpop");
		variant.put("authorization_request_type", "simple");
		VariantSelection selection = new VariantSelection(variant);

		for (String moduleName : CLIENT_GM_MODULE_NAMES) {
			assertThat(variantService.getTestModule(moduleName).isApplicableForVariant(selection))
				.as("%s must not be applicable for fapi_client_credentials_grant", moduleName)
				.isFalse();
		}
	}

	@Test
	void clientGrantManagementModulesAreApplicableForPlainFapiWhenEnabled() {
		Map<String, String> variant = new HashMap<>();
		variant.put("fapi_profile", "plain_fapi");
		variant.put("client_auth_type", "private_key_jwt");
		variant.put("sender_constrain", "dpop");
		variant.put("fapi_client_type", "plain_oauth");
		variant.put("fapi_response_mode", "plain_response");
		variant.put("fapi_request_method", "unsigned");
		variant.put("authorization_request_type", "simple");
		variant.put("grant_management", "enabled");
		VariantSelection selection = new VariantSelection(variant);

		for (String moduleName : CLIENT_GM_MODULE_NAMES) {
			assertThat(variantService.getTestModule(moduleName).isApplicableForVariant(selection))
				.as("%s must be applicable for plain_fapi with grant management enabled", moduleName)
				.isTrue();
		}
	}

	@Test
	void planGeneratesGrantManagementModulesForPlainFapiWhenEnabled() {
		// End-to-end control through the plan: the grant-management modules are still produced for a
		// normal authorization-flow profile with grant management enabled.
		Map<String, String> variant = new HashMap<>();
		variant.put("fapi_profile", "plain_fapi");
		variant.put("client_auth_type", "private_key_jwt");
		variant.put("sender_constrain", "dpop");
		variant.put("openid", "plain_oauth");
		variant.put("fapi_response_mode", "plain_response");
		variant.put("fapi_request_method", "unsigned");
		variant.put("authorization_request_type", "simple");
		variant.put("grant_management", "enabled");

		List<Plan.Module> modules = variantService.getTestPlan(PLAN_NAME)
			.getTestModulesForVariant(new VariantSelection(variant));

		assertThat(modules)
			.extracting(Plan.Module::getTestModule)
			.anyMatch(name -> name.contains("grant-management"));
	}
}
