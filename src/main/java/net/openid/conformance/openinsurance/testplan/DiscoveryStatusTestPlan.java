package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.validator.discovery.StatusListValidator;
import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetDiscoveryOpenInsuranceApi;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - Discovery Status API test plan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = PlanNames.OPIN_DISCOVERY_STATUS_TEST_PLAN,
	summary = "Structural and logical tests for Discovery API"
)
public class DiscoveryStatusTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					DiscoveryStatusTestPlan.DiscoveryStatusTestModule.class
				),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}
	@PublishTestModule(
		testName = "Discovery - Status API test module",
		displayName = "Validate structure of Discovery - Status response",
		summary = "Validate structure of Discovery - Status response",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE
	)
	public static class DiscoveryStatusTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate Discovery Status response", () -> {
				callAndStopOnFailure(PrepareToGetDiscoveryOpenInsuranceApi.class, "status");
				preCallResource();
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(StatusListValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
