package net.openid.conformance.openbanking_brasil.testmodules.opendata.acquiringServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.opendata.acquiringServices.PersonalAcquiringServicesValidator;
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
	testPlanName = "Personal Acquiring Services API test plan",
	profile = OBBProfile.OBB_PROFIlE_PHASE4,
	displayName = "Functional tests for Acquiring Services - Personal API - based on Swagger version: 1.0.0",
	summary = "Structural and logical tests for Acquiring Services API"
)
public class PersonalAcquiringServicesTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(PersonalAcquiringServicesTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Acquiring Services - Personal API test module",
		displayName = "Validate structure of Acquiring Services Personal Acquiring Services response",
		summary = "Validate structure of Acquiring Services Personal Acquiring Services response",
		profile = OBBProfile.OBB_PROFIlE_PHASE4
	)
	public static class PersonalAcquiringServicesTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate Personal Acquiring Services response", () -> {
				callAndStopOnFailure(PrepareToGetOpenDataApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(PersonalAcquiringServicesValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
