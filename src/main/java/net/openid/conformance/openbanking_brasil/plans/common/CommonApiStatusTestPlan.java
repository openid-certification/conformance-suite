package net.openid.conformance.openbanking_brasil.plans.common;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.common.GetOutagesValidator;
import net.openid.conformance.openbanking_brasil.common.GetStatusValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;
@PublishTestPlan(
	testPlanName = "Common outages api test",
	profile = OBBProfile.OBB_PROFIlE_PHASE1,
	displayName = "Functional tests for Common - Status API - based on Swagger version: 1.0.2 (WIP)",
	summary = "Structural and logical tests for OpenBanking Common API"
)
public class CommonApiStatusTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					CommonApiStatusTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = "Common status api test",
		displayName = "Validate structure of Common status API resources",
		summary = "Validates the structure of Common status API resources",
		profile = OBBProfile.OBB_PROFIlE_PHASE1
	)
	public static class CommonApiStatusTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate Common status response", () -> {
				callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class);
				preCallResource();
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetStatusValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
