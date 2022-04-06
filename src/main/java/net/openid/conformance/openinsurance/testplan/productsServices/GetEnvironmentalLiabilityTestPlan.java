package net.openid.conformance.openinsurance.testplan.productsServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetOpenInsuranceApi;
import net.openid.conformance.openinsurance.validator.productsServices.GetEnvironmentalLiabilityValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - ProductsServices - Environmental Liability API test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = PlanNames.ENVIRONMENT_VARIABLE_API_TEST_PLAN,
	summary = "Structural and logical tests for ProductsServices - Environmental Liability API"
)
public class GetEnvironmentalLiabilityTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					EnvironmentalLiabilityApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = "Open Insurance - ProductsServices - Environmental Liability API test",
		displayName = "Validate structure of ProductsServices - Environmental Liability API Api resources",
		summary = "Validate structure of ProductsServices - Environmental Liability Api resources",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE)

	public static class EnvironmentalLiabilityApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate ProductsServices - Environmental Liability response", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetEnvironmentalLiabilityValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
