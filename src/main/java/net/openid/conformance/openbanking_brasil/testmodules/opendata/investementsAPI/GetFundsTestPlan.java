package net.openid.conformance.openbanking_brasil.testmodules.opendata.investementsAPI;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.testmodules.opendata.investementsAPI.utils.PrepareInvestmentsUrl;
import net.openid.conformance.openbanking_brasil.opendata.investments.GetFundsValidator;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Investments - Funds API test plan",
	profile = OBBProfile.OBB_PROFIlE_PHASE4,
	displayName = "Functional tests for Investments - Funds API - based on Swagger version: 1.0.0-rc1.0 (WIP)",
	summary = "Structural and logical tests for Investments API"
)
public class GetFundsTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(FundsTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Investments - Funds API test module",
		displayName = "Validate structure of Investments Funds response",
		summary = "Validate structure of Investments Funds response",
		profile = OBBProfile.OBB_PROFIlE_PHASE4
	)
	public static class FundsTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate Investments Funds response", () -> {
				callAndStopOnFailure(PrepareInvestmentsUrl.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetFundsValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
