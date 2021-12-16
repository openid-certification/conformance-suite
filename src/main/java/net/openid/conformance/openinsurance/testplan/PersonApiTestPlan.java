package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.openinsurance.validator.productsNServices.GetPersonValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "ProductsNServices - Person API test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = PlanNames.PRODUCTS_N_SERVICES_PERSON_API_TEST_PLAN,
	summary = "Structural and logical tests for ProductsNServices - Person API"
)
public class PersonApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PersonApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = " ProductsNServices - Person API test",
		displayName = "Validate structure of ProductsNServices - Person API Api resources",
		summary = "Validate structure of ProductsNServices - Person Api resources",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE)
	public static class PersonApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate ProductsNServices - Person response", () -> {
				callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "person");
				preCallResource();
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetPersonValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
