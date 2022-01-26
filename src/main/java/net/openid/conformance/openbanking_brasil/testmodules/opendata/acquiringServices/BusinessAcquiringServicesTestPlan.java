package net.openid.conformance.openbanking_brasil.testmodules.opendata.acquiringServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.opendata.acquiringServices.BusinessAcquiringServicesValidator;
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
	testPlanName = "Business Acquiring Services API test plan",
	profile = OBBProfile.OBB_PROFIlE_PHASE4,
	displayName = "Functional tests for Acquiring Services - Business API - based on Swagger version: 1.0.0-rc1.0 (WIP)",
	summary = "Structural and logical tests for Acquiring Services API"
)
public class BusinessAcquiringServicesTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(BusinessAcquiringServicesTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Business Acquiring Services API test module",
		displayName = "Validate structure of Business Acquiring Services response",
		summary = "Validate structure of Business Acquiring Services response",
		profile = OBBProfile.OBB_PROFIlE_PHASE4
	)
	public static class BusinessAcquiringServicesTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate Business Acquiring Services response", () -> {
				callAndStopOnFailure(PrepareToGetOpenDataApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(BusinessAcquiringServicesValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
