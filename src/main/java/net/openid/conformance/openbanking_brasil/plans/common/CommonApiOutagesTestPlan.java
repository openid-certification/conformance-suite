package net.openid.conformance.openbanking_brasil.plans.common;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.common.GetOutagesValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;
@PublishTestPlan(
	testPlanName = "Common Outages api test",
	profile = OBBProfile.OBB_PROFIlE_PHASE1,
	displayName = "Functional tests for Common - Outages API - based on Swagger version: 1.0.2 (WIP)",
	summary = "Structural and logical tests for OpenBanking Common API"
)
public class CommonApiOutagesTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					CommonApiOutagesTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = "Common Outages api test",
		displayName = "Validate structure of Common Outages API resources",
		summary = "Validates the structure of Common Outages API resources",
		profile = OBBProfile.OBB_PROFIlE_PHASE1
	)
	public static class CommonApiOutagesTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate Common outages response", () -> {
				callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class);
				preCallResource();
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetOutagesValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
