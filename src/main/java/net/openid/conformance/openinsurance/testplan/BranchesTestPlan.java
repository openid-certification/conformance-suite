package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.validator.channels.BranchesValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Channels - Branches API test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = "Functional tests for Channels - Branches API - based on Swagger version: 1.0.0",
	summary = "Structural and logical tests for Channels - Branches API"
)
public class BranchesTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					BranchesApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = "Channels - Branches API test",
		displayName = "Validate structure of Channels - Branches API Api resources",
		summary = "Validate structure of Channels - Branches Api resources",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE)
	public static class BranchesApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate Channels - Branches response", () -> {
				callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "branches");
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(BranchesValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
