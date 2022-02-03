package net.openid.conformance.openbanking_brasil.testmodules.opendata.investementsAPI;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.opendata.investementsAPI.utils.PrepareInvestmentsUrl;
import net.openid.conformance.openbanking_brasil.opendata.investmentsAPI.validator.GetVariableIncomeValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Investments - Variable Incomes API test plan",
	profile = OBBProfile.OBB_PROFIlE_PHASE4,
	displayName = "Functional tests for Investments - Variable Incomes API - based on Swagger version: 1.0.0-rc1.0 (WIP)",
	summary = "Structural and logical tests for Investments API"
)
public class GetVariableIncomeTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(VariableIncomeTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Investments - Variable Incomes API test module",
		displayName = "Validate structure of Investments Variable Incomes response",
		summary = "Validate structure of Investments Variable Incomes response",
		profile = OBBProfile.OBB_PROFIlE_PHASE4
	)
	public static class VariableIncomeTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate Investments Variable Incomes response", () -> {
				callAndStopOnFailure(PrepareInvestmentsUrl.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetVariableIncomeValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
