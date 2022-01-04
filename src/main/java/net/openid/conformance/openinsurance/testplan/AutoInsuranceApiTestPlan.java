package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetOpenInsuranceApi;
import net.openid.conformance.openinsurance.validator.productsNServices.GetAutoInsuranceValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - ProductsNServices - Auto Insurance API test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = PlanNames.AUTO_INSURANCE_PLAN_API_TEST_PLAN,
	summary = "Structural and logical tests for ProductsNServices - Auto Insurance API"
)
public class AutoInsuranceApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					AutoInsuranceApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = "Open Insurance - ProductsNServices - Auto Insurance API test",
		displayName = "Validate structure of ProductsNServices - Auto Insurance API Api resources",
		summary = "Validate structure of ProductsNServices - Auto Insurance Api resources",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
		configurationFields = {
			"resource.commercializationArea",
			"resource.fipeCode",
			"resource.year"
		}
	)
	public static class AutoInsuranceApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate ProductsNServices - Auto Insurance response", () -> {
				callAndStopOnFailure(PrepareToGetAutoInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetAutoInsuranceValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}

	public static class PrepareToGetAutoInsuranceApi extends AbstractCondition {

		@Override
		public Environment evaluate(Environment env) {
			String baseURL = env.getString("config", "resource.resourceUrl");
			baseURL = StringUtils.removeEnd(baseURL, "/");
			String commercializationArea =  env.getString("config", "resource.commercializationArea");
			String fipeCode =  env.getString("config", "resource.fipeCode");
			String year =  env.getString("config", "resource.year");
			String protectedUrl = String.format("%s/%s/%s/%s",
				baseURL, commercializationArea, fipeCode, year);
			env.putString("protected_resource_url", protectedUrl);
			return env;
		}
	}
}
