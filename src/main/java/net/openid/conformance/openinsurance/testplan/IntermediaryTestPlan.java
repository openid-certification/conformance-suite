package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.LogOnlyFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetOpenInsuranceApi;
import net.openid.conformance.openinsurance.validator.channels.GetIntermediaryValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - Channels - Intermediary API test plan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = PlanNames.INTERMEDIARY_API_TEST_PLAN,
	summary = "Structural and logical tests for Channels - Intermediary API"
)
public class IntermediaryTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(IntermediaryTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Open Insurance - Channels - Intermediary API test",
		displayName = "Validate structure of Channels - Intermediary response",
		summary = "Validate structure of Channels - Intermediary response",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE
	)
	public static class IntermediaryTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate Channels - Intermediary response", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetIntermediaryValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
