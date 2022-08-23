package net.openid.conformance.openinsurance.testplan.productsServices;

	import net.openid.conformance.condition.Condition;
	import net.openid.conformance.openbanking_brasil.OBBProfile;
	import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
	import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
	import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
	import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
	import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetOpenInsuranceApi;
	import net.openid.conformance.openinsurance.validator.productsServices.GetPublicGuaranteeValidator;
	import net.openid.conformance.plan.PublishTestPlan;
	import net.openid.conformance.plan.TestPlan;
	import net.openid.conformance.testmodule.PublishTestModule;
	import net.openid.conformance.variant.ClientAuthType;

	import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - ProductsServices - Public Guarantee API test plan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1,
	displayName = PlanNames.PUBLIC_GUARANTEE_API_TEST_PLAN,
	summary = "Structural and logical tests for Public Guarantee API"
)
public class PublicGuaranteeTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(PublicGuaranteeTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Open Insurance - Public Guarantee API test",
		displayName = "Validate structure of Public Guarantee response",
		summary = "Validate structure of Public Guarantee response",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1
	)
	public static class PublicGuaranteeTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate ProductsServices Public Guarantee response", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetPublicGuaranteeValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
