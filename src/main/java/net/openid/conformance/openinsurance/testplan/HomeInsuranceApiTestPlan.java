package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.validator.productsNServices.GetHomeInsuranceValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;
@PublishTestPlan(
	testPlanName = "ProductsNServices - Home Insurance API test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = PlanNames.HOME_INSURANCE_PLAN_API_TEST_PLAN,
	summary = "Structural and logical tests for ProductsNServices - Home Insurance API"
)
public class HomeInsuranceApiTestPlan implements TestPlan {
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
		testName = " ProductsNServices - Home Insurance API test",
		displayName = "Validate structure of ProductsNServices - Home Insurance API Api resources",
		summary = "Validate structure of ProductsNServices - Home Insurance Api resources",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
		configurationFields = {
			"resource.commercializationArea"
		}
	)
	public static class HomeInsuranceApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate ProductsNServices - Home Insurance response", () -> {
				callAndStopOnFailure(PrepareToGetHomeInsuranceApi.class, "home-insurance/commercializationArea");
				preCallResource();
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetHomeInsuranceValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}

	public static class PrepareToGetHomeInsuranceApi extends AbstractCondition {

		@Override
		public Environment evaluate(Environment env) {

			if (getRequirements().isEmpty()) {
				throw error("Url part to resource must be add in environment, when configure test module.");
			}
			String urlPart = getRequirements().iterator().next();
			String baseURL = env.getString("config", "resource.resourceUrl");
			String commercializationArea =  env.getString("config", "resource.commercializationArea");

			String protectedUrl = String.format("%s/%s/%s", baseURL, urlPart, commercializationArea);
			env.putString("protected_resource_url", protectedUrl);
			return env;
		}
	}
}
