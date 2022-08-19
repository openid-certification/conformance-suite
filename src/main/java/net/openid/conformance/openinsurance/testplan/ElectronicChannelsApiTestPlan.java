package net.openid.conformance.openinsurance.testplan;


import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.LogOnlyFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetOpenInsuranceApi;
import net.openid.conformance.openinsurance.validator.channels.v1.ElectronicChannelsValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - Channels - Electronic Channels API test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = PlanNames.OPIN_ELECTRONIC_CHANNELS_API_TEST_PLAN,
	summary = "Structural and logical tests for Channels - Electronic Channels API"
)
public class ElectronicChannelsApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(List.of(
				ElectronicChannelsApiTestModule.class
			), List.of(
				new Variant(ClientAuthType.class, "none")
			))
		);
	}

	@PublishTestModule(
		testName = "Open Insurance - Channels - Electronic Channels API test",
		displayName = "Validate structure of Channels - Electronic Channels Api resources",
		summary = "Validate structure of Channels - Electronic Channels Api resources",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE
	)
	public static class ElectronicChannelsApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate Channels - Electronic Channels response", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(LogOnlyFailure.class);
				callAndContinueOnFailure(ElectronicChannelsValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
