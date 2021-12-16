package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.validator.productsNServices.GetPensionPlanValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;
@PublishTestPlan(
	testPlanName = "ProductsNServices - Pension Plan API test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = PlanNames.PERSON_PENSION_PLAN_API_TEST_PLAN,
	summary = "Structural and logical tests for ProductsNServices - Pension Plan API"
)
public class PensionPlanApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PensionPlanApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = " ProductsNServices - Pension Plan API test",
		displayName = "Validate structure of ProductsNServices - Pension Plan API Api resources",
		summary = "Validate structure of ProductsNServices - Pension Plan Api resources",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE)
	public static class PensionPlanApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate ProductsNServices - Pension Plan response", () -> {
				callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "pension-plan");
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetPensionPlanValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
