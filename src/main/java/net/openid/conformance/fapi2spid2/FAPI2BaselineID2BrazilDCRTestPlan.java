package net.openid.conformance.fapi2spid2;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan (
	testPlanName = "fapi2-baseline-id2-brazil-dcr-test-plan",
	displayName = "FAPI2-Baseline-ID2: Brazil Dynamic Client Registration Authorization server test - INCORRECT/INCOMPLETE, DO NOT USE",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI2BaselineID2BrazilDCRTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {

		return List.of(
			new ModuleListEntry(
				List.of(
					FAPI2BaselineID2BrazilDCRHappyFlow.class,
					FAPI2BaselineID2BrazilDCRHappyFlowVariant.class,
					FAPI2BaselineID2BrazilDCRHappyFlowVariant2.class,
					FAPI2BaselineID2BrazilDCRClientDeletion.class,
					FAPI2BaselineID2BrazilDCRInvalidRegistrationAccessToken.class,
					FAPI2BaselineID2BrazilDCRInvalidSoftwareStatementSignature.class,
					FAPI2BaselineID2BrazilDCRNoSoftwareStatement.class,
					FAPI2BaselineID2BrazilDCRNoMTLS.class,
					FAPI2BaselineID2BrazilDCRBadMTLS.class,
					FAPI2BaselineID2BrazilDCRUpdateClientConfig.class,
					FAPI2BaselineID2BrazilDCRUpdateClientConfigBadJwksUri.class,
					FAPI2BaselineID2BrazilDCRUpdateClientConfigInvalidJwksByValue.class,
					FAPI2BaselineID2BrazilDCRUpdateClientConfigInvalidRedirectUri.class,
					FAPI2BaselineID2BrazilDCRNoRedirectUri.class,
					FAPI2BaselineID2BrazilDCRInvalidRedirectUri.class,
					FAPI2BaselineID2BrazilDCRInvalidJwksUri.class,
					FAPI2BaselineID2BrazilDCRInvalidJwksByValue.class
				),
				List.of(new Variant(FAPI2ID2OPProfile.class, "openbanking_brazil"))
			)
		);

	}
	public static String certificationProfileName(VariantSelection variant) {
		return "BR-OB Adv. OP DCR";
	}
}
