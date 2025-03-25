package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan (
	testPlanName = "fapi2-security-profile-final-brazil-dcr-test-plan",
	displayName = "FAPI2-Security-Profile-Final: Brazil Dynamic Client Registration Authorization server test - INCORRECT/INCOMPLETE, DO NOT USE",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI2SPFinalBrazilDCRTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {

		return List.of(
			new ModuleListEntry(
				List.of(
					FAPI2SPFinalBrazilDCRHappyFlow.class,
					FAPI2SPFinalBrazilDCRHappyFlowVariant.class,
					FAPI2SPFinalBrazilDCRHappyFlowVariant2.class,
					FAPI2SPFinalBrazilDCRClientDeletion.class,
					FAPI2SPFinalBrazilDCRInvalidRegistrationAccessToken.class,
					FAPI2SPFinalBrazilDCRInvalidSoftwareStatementSignature.class,
					FAPI2SPFinalBrazilDCRNoSoftwareStatement.class,
					FAPI2SPFinalBrazilDCRNoMTLS.class,
					FAPI2SPFinalBrazilDCRBadMTLS.class,
					FAPI2SPFinalBrazilDCRUpdateClientConfig.class,
					FAPI2SPFinalBrazilDCRUpdateClientConfigBadJwksUri.class,
					FAPI2SPFinalBrazilDCRUpdateClientConfigInvalidJwksByValue.class,
					FAPI2SPFinalBrazilDCRUpdateClientConfigInvalidRedirectUri.class,
					FAPI2SPFinalBrazilDCRNoRedirectUri.class,
					FAPI2SPFinalBrazilDCRInvalidRedirectUri.class,
					FAPI2SPFinalBrazilDCRInvalidJwksUri.class,
					FAPI2SPFinalBrazilDCRInvalidJwksByValue.class
				),
				List.of(new Variant(FAPI2FinalOPProfile.class, "openbanking_brazil"))
			)
		);

	}
	public static String certificationProfileName(VariantSelection variant) {
		return "BR-OB Adv. OP DCR";
	}
}
