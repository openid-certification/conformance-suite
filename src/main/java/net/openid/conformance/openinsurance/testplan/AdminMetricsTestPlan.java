package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.validator.admin.AdminMetricsValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance Admin API test plan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = "Functional tests for Admin API - based on Swagger version: 1.0.0",
	summary = "Structural and logical tests for Admin API"
)
public class AdminMetricsTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(AdminMetricsTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Admin Metrics API test",
		displayName = "Validate structure of Admin Metrics response",
		summary = "Validate structure of Admin Metrics response",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE
	)
	public static class AdminMetricsTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate ProductsNServices Admin Metrics response", () -> {
				callAndStopOnFailure(PrepareAdminMetricsUrl.class);
				preCallResource();
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(AdminMetricsValidator.class, Condition.ConditionResult.FAILURE);
			});
		}

		public static class PrepareAdminMetricsUrl extends AbstractCondition {
			@Override
			public Environment evaluate(Environment env) {
				String baseURL = env.getString("config", "resource.resourceUrl");
				String protectedUrl = String.format("%s/%s", baseURL, "metrics");
				env.putString("protected_resource_url", protectedUrl);
				return env;
			}
		}
	}
}
