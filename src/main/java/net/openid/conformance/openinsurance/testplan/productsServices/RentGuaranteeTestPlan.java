package net.openid.conformance.openinsurance.testplan.productsServices;

	import net.openid.conformance.condition.Condition;
	import net.openid.conformance.openbanking_brasil.OBBProfile;
	import net.openid.conformance.openbanking_brasil.plans.PlanNames;
	import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
	import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
	import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
	import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetOpenInsuranceApi;
	import net.openid.conformance.openinsurance.validator.productsServices.GetRentGuaranteeValidator;
	import net.openid.conformance.plan.PublishTestPlan;
	import net.openid.conformance.plan.TestPlan;
	import net.openid.conformance.testmodule.PublishTestModule;
	import net.openid.conformance.variant.ClientAuthType;

	import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - ProductsServices - Rent Guarantee API test plan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = PlanNames.RENT_GUARANTEE_API_TEST_PLAN,
	summary = "Structural and logical tests for Rent Guarantee API"
)
public class RentGuaranteeTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(RentGuaranteeTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Open Insurance - Rent Guarantee API test",
		displayName = "Validate structure of Rent Guarantee response",
		summary = "Validate structure of Rent Guarantee response",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE
	)
	public static class RentGuaranteeTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate ProductsServices Rent Guarantee response", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetRentGuaranteeValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
