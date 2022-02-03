package net.openid.conformance.openbanking_brasil.testmodules.opendata.insurance;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.opendata.insurance.PersonalInsuranceListValidator;
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
	testPlanName = "Open Banking Brasil - Opendata Insurance - PersonalInsurance API test plan",
	profile = OBBProfile.OBB_PROFIlE_PHASE4,
	displayName = "Functional tests for Opendata Insurance - PersonalInsurance API - based on Swagger version: 1.0.0",
	summary = "Structural and logical tests for Opendata Insurance - PersonalInsurance API"
)
public class PersonalInsuranceTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PersonalInsuranceApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = "Open Banking Brasil - Opendata Insurance - PersonalInsurance API test module",
		displayName = "Validate structure of Opendata Insurance - PersonalInsurance API Api resources",
		summary = "Validate structure of Opendata Insurance - PersonalInsurance Api resources",
		profile = OBBProfile.OBB_PROFIlE_PHASE4)
	public static class PersonalInsuranceApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate Opendata Insurance - PersonalInsurance response", () -> {
				callAndStopOnFailure(PrepareToGetOpenDataApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(PersonalInsuranceListValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
