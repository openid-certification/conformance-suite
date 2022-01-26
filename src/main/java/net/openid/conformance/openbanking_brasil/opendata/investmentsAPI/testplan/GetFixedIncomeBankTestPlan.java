package net.openid.conformance.openbanking_brasil.opendata.investmentsAPI.testplan;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.opendata.investmentsAPI.testplan.utils.PrepareInvestmentsUrl;
import net.openid.conformance.openbanking_brasil.opendata.investmentsAPI.validator.GetFixedIncomeBankValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Investments - Bank Fixed Income API test plan",
	profile = OBBProfile.OBB_PROFIlE_PHASE4,
	displayName = "Functional tests for Investments - Bank Fixed Income API - based on Swagger version: 1.0.0-rc1.0 (WIP)",
	summary = "Structural and logical tests for Investments API"
)
public class GetFixedIncomeBankTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(FixedIncomeBankTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Investments - Bank Fixed Income API test module",
		displayName = "Validate structure of Investments Bank Fixed Income response",
		summary = "Validate structure of Investments Bank Fixed Income response",
		profile = OBBProfile.OBB_PROFIlE_PHASE4
	)
	public static class FixedIncomeBankTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate Investments Bank Fixed Income response", () -> {
				callAndStopOnFailure(PrepareInvestmentsUrl.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetFixedIncomeBankValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
