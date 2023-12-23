package net.openid.conformance.fapi1advancedfinalfapibrv1;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi1-advanced-final-br-v1-brazil-dcr-test-plan",
	displayName = "FAPI1-Advanced-Final-Br-v1: Brazil Dynamic Client Registration Authorization server test - only available till 31st Dec 2023",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI1AdvancedFinalBrV1BrazilDCRTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {

		return List.of(
			new ModuleListEntry(
				List.of(
					FAPI1AdvancedFinalBrV1BrazilDCRHappyFlow.class,
					FAPI1AdvancedFinalBrV1BrazilDCRHappyFlowVariant.class,
					FAPI1AdvancedFinalBrV1BrazilDCRHappyFlowVariant2.class,
					FAPI1AdvancedFinalBrV1BrazilDCRClientDeletion.class,
					FAPI1AdvancedFinalBrV1BrazilDCRInvalidRegistrationAccessToken.class,
					FAPI1AdvancedFinalBrV1BrazilDCRInvalidSoftwareStatementSignature.class,
					FAPI1AdvancedFinalBrV1BrazilDCRNoSoftwareStatement.class,
					FAPI1AdvancedFinalBrV1BrazilDCRNoMTLS.class,
					FAPI1AdvancedFinalBrV1BrazilDCRBadMTLS.class,
					FAPI1AdvancedFinalBrV1BrazilDCRUpdateClientConfig.class,
					FAPI1AdvancedFinalBrV1BrazilDCRUpdateClientConfigBadJwksUri.class,
					FAPI1AdvancedFinalBrV1BrazilDCRUpdateClientConfigInvalidJwksByValue.class,
					FAPI1AdvancedFinalBrV1BrazilDCRUpdateClientConfigInvalidRedirectUri.class,
					FAPI1AdvancedFinalBrV1BrazilDCRNoRedirectUri.class,
					FAPI1AdvancedFinalBrV1BrazilDCRInvalidRedirectUri.class,
					FAPI1AdvancedFinalBrV1BrazilDCRInvalidJwksUri.class,
					FAPI1AdvancedFinalBrV1BrazilDCRInvalidJwksByValue.class
				),
				List.of()
			)
		);

	}
	public static String certificationProfileName(VariantSelection variant) {
		Map<String, String> v = variant.getVariant();

		String profile = v.get("fapi_profile");

		switch (profile) {
			case "openbanking_brazil":
				return "BR-OB Adv. OP DCR";
			case "openinsurance_brazil":
				return "BR-OPIN Adv. OP DCR";
			default:
				throw new RuntimeException("This plan can only be used for Brazil OpenBanking or OpenInsurance.");
		}
	}
}
