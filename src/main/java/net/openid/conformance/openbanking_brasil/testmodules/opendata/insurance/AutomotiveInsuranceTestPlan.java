package net.openid.conformance.openbanking_brasil.testmodules.opendata.insurance;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.opendata.insurance.AutomotiveInsuranceListValidator;
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
	testPlanName = "Open Banking Brasil - Opendata Insurance - AutomotiveInsurance API test plan",
	profile = OBBProfile.OBB_PROFIlE_PHASE4,
	displayName = "Functional tests for Insurance - Automotive Insurance API - based on Swagger version: 1.0.0-rc1.0 (WIP)",
	summary = "Structural and logical tests for Opendata Insurance - AutomotiveInsurance API"
)
public class AutomotiveInsuranceTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					AutomotiveInsuranceApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = "Open Banking Brasil - Opendata Insurance - AutomotiveInsurance API test module",
		displayName = "Validate structure of Opendata Insurance - AutomotiveInsurance API Api resources",
		summary = "Validate structure of Opendata Insurance - AutomotiveInsurance Api resources",
		profile = OBBProfile.OBB_PROFIlE_PHASE4)
	public static class AutomotiveInsuranceApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate Opendata Insurance - AutomotiveInsurance response", () -> {
				callAndStopOnFailure(PrepareToGetOpenDataApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(AutomotiveInsuranceListValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
