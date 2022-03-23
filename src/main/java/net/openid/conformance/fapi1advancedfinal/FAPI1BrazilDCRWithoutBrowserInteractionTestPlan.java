package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.openbanking_brasil.testmodules.DcrSubjectDnTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.*;

import java.util.List;


@PublishTestPlan(
	testPlanName = "brazil-dcr-without-browser-interaction",
	displayName = "Brazil DCR Test without Browser Interaction",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI1BrazilDCRWithoutBrowserInteractionTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {

		return List.of(
			new ModuleListEntry(
				List.of(
					FAPI1AdvancedFinalBrazilDCRHappyFlowNoAuth.class,
					FAPI1AdvancedFinalBrazilDCRHappyFlowVariantNoAuth.class,
					FAPI1AdvancedFinalBrazilDCRHappyFlowVariant2NoAuth.class,
					FAPI1AdvancedFinalBrazilDCRClientDeletion.class,
					FAPI1AdvancedFinalBrazilDCRInvalidRegistrationAccessToken.class,
					FAPI1AdvancedFinalBrazilDCRInvalidSoftwareStatementSignature.class,
					FAPI1AdvancedFinalBrazilDCRNoSoftwareStatement.class,
					FAPI1AdvancedFinalBrazilDCRNoMTLS.class,
					FAPI1AdvancedFinalBrazilDCRBadMTLS.class,
					FAPI1AdvancedFinalBrazilDCRUpdateClientConfigNoAuth.class,
					FAPI1AdvancedFinalBrazilDCRUpdateClientConfigBadJwksUri.class,
					FAPI1AdvancedFinalBrazilDCRUpdateClientConfigInvalidJwksByValue.class,
					FAPI1AdvancedFinalBrazilDCRUpdateClientConfigInvalidRedirectUri.class,
					FAPI1AdvancedFinalBrazilDCRNoRedirectUri.class,
					FAPI1AdvancedFinalBrazilDCRInvalidRedirectUri.class,
					FAPI1AdvancedFinalBrazilDCRInvalidJwksUri.class,
					FAPI1AdvancedFinalBrazilDCRInvalidJwksByValue.class,
					DcrSubjectDnTestModule.class
				),
				List.of(new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil"))
			)
		);

	}
	public static String certificationProfileName(VariantSelection variant) {
		return "BR-OB Adv. OP DCR";
	}
}
