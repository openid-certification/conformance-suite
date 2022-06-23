package net.openid.conformance.openbanking_brasil.plans.v1;

import net.openid.conformance.fapi1advancedfinal.dcr_no_authorization_flow.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.DcrAttemptClientTakeoverTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.DcrNoSubjectTypeTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.DcrSandboxCredentialsTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.DcrSubjectDnTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;


@PublishTestPlan(
	testPlanName = "brazil-dcr-without-browser-interaction",
	displayName = PlanNames.OBB_DCR_WITHOUT_BROWSER_INTERACTION_TEST_PLAN,
	profile = OBBProfile.OBB_PROFIlE_PHASE3
)
public class BrazilDCRWithoutBrowserInteractionTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {

		return List.of(
			new ModuleListEntry(
				List.of(
					FAPI1AdvancedFinalBrazilDCRHappyFlowNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRHappyFlowVariantNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRHappyFlowVariant2NoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRClientDeletionNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRInvalidRegistrationAccessTokenNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRInvalidSoftwareStatementSignatureNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRNoSoftwareStatementNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRNoMTLSNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRBadMTLSNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRUpdateClientConfigNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRUpdateClientConfigBadJwksUriNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRUpdateClientConfigInvalidJwksByValueNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRUpdateClientConfigInvalidRedirectUriNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRNoRedirectUriNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRInvalidRedirectUriNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRInvalidJwksUriNoAuthFlow.class,
					FAPI1AdvancedFinalBrazilDCRInvalidJwksByValueNoAuthFlow.class,
					DcrSubjectDnTestModule.class,
//					DcrAttemptClientTakeoverTestModule.class,
					DCRConsentsBadLoggedUser.class,
					DcrSandboxCredentialsTestModule.class,
					DcrNoSubjectTypeTestModule.class
				),
				List.of(new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil"))
			)
		);

	}
	public static String certificationProfileName(VariantSelection variant) {
		return "BR-OB Adv. OP DCR";
	}
}
