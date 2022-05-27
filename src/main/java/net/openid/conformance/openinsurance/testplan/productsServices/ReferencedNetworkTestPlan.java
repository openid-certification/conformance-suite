package net.openid.conformance.openinsurance.testplan.productsServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetOpenInsuranceApi;
import net.openid.conformance.openinsurance.validator.productsServices.GetReferencedNetworkValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - ProductsServices - Referenced Network API test plan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = PlanNames.REFERENCED_NETWORK_API_TEST_PLAN,
	summary = "Structural and logical tests for Referenced Network API"
)
public class ReferencedNetworkTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(ReferencedNetworkTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Open Insurance - Referenced Network API test",
		displayName = "Validate structure of Referenced Network response",
		summary = "Validate structure of Referenced Network response",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE
	)
	public static class ReferencedNetworkTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate ProductsServices Referenced Network response", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetReferencedNetworkValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
