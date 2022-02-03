package net.openid.conformance.openbanking_brasil.testmodules.opendata.insurance;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.opendata.insurance.HomeInsuranceListValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.opendata.utils.PrepareToGetOpenDataApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Banking Brasil - Opendata Insurance - HomeInsurance API test plan",
	profile = OBBProfile.OBB_PROFIlE_PHASE4,
	displayName = "Functional tests for Opendata Insurance - HomeInsurance API - based on Swagger version: 1.0.0",
	summary = "Structural and logical tests for Opendata Insurance - HomeInsurance API"
)
public class HomeInsuranceTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					HomeInsuranceApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = "Open Banking Brasil - Opendata Insurance - Home Insurance API test module",
		displayName = "Validate structure of Opendata Insurance - Home Insurance API Api resources",
		summary = "Validate structure of Opendata Insurance - Home Insurance Api resources",
		profile = OBBProfile.OBB_PROFIlE_PHASE4)
	public static class HomeInsuranceApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate Opendata Insurance - HomeInsurance response", () -> {
				callAndStopOnFailure(PrepareToGetOpenDataApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(HomeInsuranceListValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
