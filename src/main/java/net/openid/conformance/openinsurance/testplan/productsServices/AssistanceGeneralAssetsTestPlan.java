package net.openid.conformance.openinsurance.testplan.productsServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetOpenInsuranceApi;
import net.openid.conformance.openinsurance.validator.productsServices.GetAssistanceGeneralAssetsValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - ProductsServices - Assistance General Assets API test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1,
	displayName = PlanNames.ASSISTANCE_GENERAL_ASSETS_API_TEST_PLAN,
	summary = "Structural and logical tests for ProductsServices - Assistance General Assets API"
)
public class AssistanceGeneralAssetsTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					AssistanceGeneralAssetsApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = "Open Insurance - ProductsServices - Assistance General Assets API test",
		displayName = "Validate structure of ProductsServices - Assistance General Assets API Api resources",
		summary = "Validate structure of ProductsServices - Assistance General Assets Api resources",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1)

	public static class AssistanceGeneralAssetsApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate ProductsServices - Assistance General Assets response", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetAssistanceGeneralAssetsValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
