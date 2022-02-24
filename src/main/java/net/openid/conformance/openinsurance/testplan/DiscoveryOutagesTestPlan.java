package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.discovery.OutagesListValidator;
import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetDiscoveryOpenInsuranceApi;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - Discovery Outages API test plan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = PlanNames.OPIN_DISCOVERY_OUTAGES_TEST_PLAN,
	summary = "Structural and logical tests for Discovery API"
)
public class DiscoveryOutagesTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					DiscoveryOutagesTestPlan.DiscoveryOutagesTestModule.class
				),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Discovery - Outages API test module",
		displayName = "Validate structure of Discovery - Outages response",
		summary = "Validate structure of Discovery - Outages response",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE
	)
	public static class DiscoveryOutagesTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate Discovery Outages response", () -> {
				callAndStopOnFailure(PrepareToGetDiscoveryOpenInsuranceApi.class, "outages");
				preCallResource();
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(OutagesListValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}


