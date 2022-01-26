package net.openid.conformance.openbanking_brasil.testmodules.opendata.pension;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.opendata.pension.SurvivalCoveragesValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.opendata.utils.PrepareToGetOpenDataApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

//@PublishTestPlan(
//	testPlanName = "Pension - Survival Coverages API test plan",
//	profile = OBBProfile.OBB_PROFIlE_PHASE4,
//	displayName = "Functional tests for Pension - Survival Coverages - based on Swagger version: 1.0.0",
//	summary = "Structural and logical tests for Pension API"
//)
public class SurvivalCoveragesTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(SurvivalCoveragesTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Pension - Survival Coverages API test module",
		displayName = "Validate structure of Pension Survival Coverages response",
		summary = "Validate structure of Pension Survival Coverages response",
		profile = OBBProfile.OBB_PROFIlE_PHASE4
	)
	public static class SurvivalCoveragesTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate Pension Survival Coverages response", () -> {
				callAndStopOnFailure(PrepareToGetOpenDataApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(SurvivalCoveragesValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
