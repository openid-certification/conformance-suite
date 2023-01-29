package net.openid.conformance.fapi2spid2;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan (
	testPlanName = "fapi2-security-profile-id2-brazil-dcr-test-plan",
	displayName = "FAPI2-Security-Profile-ID2: Brazil Dynamic Client Registration Authorization server test - INCORRECT/INCOMPLETE, DO NOT USE",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI2SPID2BrazilDCRTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {

		return List.of(
			new ModuleListEntry(
				List.of(
					FAPI2SPID2BrazilDCRHappyFlow.class,
					FAPI2SPID2BrazilDCRHappyFlowVariant.class,
					FAPI2SPID2BrazilDCRHappyFlowVariant2.class,
					FAPI2SPID2BrazilDCRClientDeletion.class,
					FAPI2SPID2BrazilDCRInvalidRegistrationAccessToken.class,
					FAPI2SPID2BrazilDCRInvalidSoftwareStatementSignature.class,
					FAPI2SPID2BrazilDCRNoSoftwareStatement.class,
					FAPI2SPID2BrazilDCRNoMTLS.class,
					FAPI2SPID2BrazilDCRBadMTLS.class,
					FAPI2SPID2BrazilDCRUpdateClientConfig.class,
					FAPI2SPID2BrazilDCRUpdateClientConfigBadJwksUri.class,
					FAPI2SPID2BrazilDCRUpdateClientConfigInvalidJwksByValue.class,
					FAPI2SPID2BrazilDCRUpdateClientConfigInvalidRedirectUri.class,
					FAPI2SPID2BrazilDCRNoRedirectUri.class,
					FAPI2SPID2BrazilDCRInvalidRedirectUri.class,
					FAPI2SPID2BrazilDCRInvalidJwksUri.class,
					FAPI2SPID2BrazilDCRInvalidJwksByValue.class
				),
				List.of(new Variant(FAPI2ID2OPProfile.class, "openbanking_brazil"))
			)
		);

	}
	public static String certificationProfileName(VariantSelection variant) {
		return "BR-OB Adv. OP DCR";
	}
}
