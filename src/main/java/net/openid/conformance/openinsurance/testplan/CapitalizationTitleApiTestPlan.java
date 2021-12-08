package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.openinsurance.validator.productsNServices.GetCapitalizationTitleValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "ProductsNServices - Capitalization Title API test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = PlanNames.CAPITALIZATION_TITLE_PLAN_API_TEST_PLAN,
	summary = "Structural and logical tests for ProductsNServices - Capitalization Title API"
)
public class CapitalizationTitleApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					CapitalizationTitleApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = " ProductsNServices - Capitalization Title API test",
		displayName = "Validate structure of ProductsNServices - Capitalization Title API Api resources",
		summary = "Validate structure of ProductsNServices - Capitalization Title Api resources",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE
	)
	public static class CapitalizationTitleApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate ProductsNServices - Capitalization Title response", () -> {
				callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "capitalization-title");
				preCallResource();
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetCapitalizationTitleValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
