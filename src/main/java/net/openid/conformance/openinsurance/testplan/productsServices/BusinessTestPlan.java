package net.openid.conformance.openinsurance.testplan.productsServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetOpenInsuranceApi;
import net.openid.conformance.openinsurance.validator.productsServices.GetBusinessValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - ProductsServices - Business API test plan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1,
	displayName = PlanNames.BUSINESS_API_TEST_PLAN,
	summary = "Structural and logical tests for Business API"
)
public class BusinessTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(BusinessTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Open Insurance - Business API test",
		displayName = "Validate structure of Business response",
		summary = "Validate structure of Business response",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1
	)
	public static class BusinessTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate ProductsServices Business response", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetBusinessValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
