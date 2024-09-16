package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi1-advanced-final-brazil-dcr-test-plan",
	displayName = "FAPI1-Advanced-Final: Brazil Dynamic Client Registration Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI1AdvancedFinalBrazilDCRTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {

		return List.of(
			new ModuleListEntry(
				List.of(
					FAPI1AdvancedFinalBrazilDCRHappyFlow.class,
					FAPI1AdvancedFinalBrazilDCRHappyFlowVariant.class,
					FAPI1AdvancedFinalBrazilDCRHappyFlowVariant2.class,
					FAPI1AdvancedFinalBrazilDCRPaymentConsentRequestAudAsArray.class,
					FAPI1AdvancedFinalBrazilDCRClientDeletion.class,
					FAPI1AdvancedFinalBrazilDCRInvalidRegistrationAccessToken.class,
					FAPI1AdvancedFinalBrazilDCRInvalidSoftwareStatementSignature.class,
					FAPI1AdvancedFinalBrazilDCRNoSoftwareStatement.class,
					FAPI1AdvancedFinalBrazilDCRNoMTLS.class,
					FAPI1AdvancedFinalBrazilDCRBadMTLS.class,
					FAPI1AdvancedFinalBrazilDCRUpdateClientConfig.class,
					FAPI1AdvancedFinalBrazilDCRUpdateClientConfigBadJwksUri.class,
					FAPI1AdvancedFinalBrazilDCRUpdateClientConfigInvalidJwksByValue.class,
					FAPI1AdvancedFinalBrazilDCRUpdateClientConfigInvalidRedirectUri.class,
					FAPI1AdvancedFinalBrazilDCRNoRedirectUri.class,
					FAPI1AdvancedFinalBrazilDCRInvalidRedirectUri.class,
					FAPI1AdvancedFinalBrazilDCRInvalidJwksUri.class,
					FAPI1AdvancedFinalBrazilDCRInvalidJwksByValue.class
				),
				List.of(
					/* This plan is only used for Brazil, so when OpenInsurance switches to the new profile
					 * we could remove the need for the user to select these variants by uncommenting these lines:
					new Variant(ClientAuthType.class, "private_key_jwt"),
					new Variant(FAPIResponseMode.class, "plain_response"),
					new Variant(FAPIAuthRequestMethod.class, "pushed")
					 */
				)
			)
		);

	}
	public static String certificationProfileName(VariantSelection variant) {
		Map<String, String> v = variant.getVariant();

		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String requestMethod = v.get("fapi_auth_request_method");
		String responseMode = v.get("fapi_response_mode");
		boolean par = requestMethod.equals("pushed");
		boolean jarm = responseMode.equals("jarm");
		boolean privateKey = clientAuth.equals("private_key_jwt");
		if (!par || jarm || !privateKey) {
			throw new RuntimeException("Invalid configuration for %s: PAR & private_key_jwt are required in Brazil OpenFinance & JARM is not used".formatted(
				MethodHandles.lookup().lookupClass().getSimpleName()));
		}
		switch (profile) {
			case "openbanking_brazil":
				return "BR-OF Adv. OP DCR (FAPI-BR v2)";
			case "openinsurance_brazil":
				return "BR-OPIN Adv. OP DCR (FAPI-BR v2)";
			default:
				throw new RuntimeException("This plan can only be used for Brazil OpenBanking/OpenFinance or OpenInsurance.");
		}
	}
}
