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
	testPlanName = "Investments - Fixed Income Bank API test plan",
	profile = OBBProfile.OBB_PROFIlE_PHASE4,
	displayName = "Functional tests for Investments - Fixed Income Bank API - based on Swagger version: 1.0.0",
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
		testName = "Investments - Fixed Income Bank API test module",
		displayName = "Validate structure of Investments Fixed Income Bank response",
		summary = "Validate structure of Investments Fixed Income Bank response",
		profile = OBBProfile.OBB_PROFIlE_PHASE4
	)
	public static class FixedIncomeBankTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate Investments Fixed Income Bank response", () -> {
				callAndStopOnFailure(PrepareInvestmentsUrl.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetFixedIncomeBankValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
