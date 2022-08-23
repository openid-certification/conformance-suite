package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.validator.productsNServices.GetHomeInsuranceValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
@PublishTestPlan(
	testPlanName = "Open Insurance - ProductsNServices - Home Insurance API test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1,
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
		testName = "Open Insurance - ProductsNServices - Home Insurance API test",
		displayName = "Validate structure of ProductsNServices - Home Insurance API Api resources",
		summary = "Validate structure of ProductsNServices - Home Insurance Api resources",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1,
		configurationFields = {
			"resource.commercializationArea"
		}
	)
	public static class HomeInsuranceApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate ProductsNServices - Home Insurance response", () -> {
				callAndStopOnFailure(PrepareToGetHomeInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetHomeInsuranceValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}

	public static class PrepareToGetHomeInsuranceApi extends AbstractCondition {

		@Override
		public Environment evaluate(Environment env) {
			String baseURL = env.getString("config", "resource.resourceUrl");
			baseURL = StringUtils.removeEnd(baseURL, "/");
			String commercializationArea =  env.getString("config", "resource.commercializationArea");
			String protectedUrl = baseURL;
			if (commercializationArea != null){
				protectedUrl = String.format("%s/%s", baseURL, commercializationArea);
			}
			env.putString("protected_resource_url", protectedUrl);
			return env;
		}
	}
}
