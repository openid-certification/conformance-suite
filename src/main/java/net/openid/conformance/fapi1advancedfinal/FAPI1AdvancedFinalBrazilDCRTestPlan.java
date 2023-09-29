package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantSelection;

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
					new Variant(ClientAuthType.class, "private_key_jwt"),
					new Variant(FAPIResponseMode.class, "plain_response"),
					new Variant(FAPIAuthRequestMethod.class, "pushed")
				)
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
				throw new RuntimeException("This plan can only be used for Brazil OpenBanking/OpenFinance or OpenInsurance.");
		}
	}
}
