package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
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
					FAPI1AdvancedFinalBrazilDCRInvalidSoftwareStatementSignature.class,
					FAPI1AdvancedFinalBrazilDCRInvalidRedirectUri.class
				),
				List.of(new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil"))
			)
		);

	}
	public static String certificationProfileName(VariantSelection variant) {
		return "BR-OB Adv. OP DCR";
	}
}
