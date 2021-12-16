package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.validator.productsNServices.GetLifePensionValidator;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance ProductsNServices - Life Pension API test plan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE,
	displayName = "Functional tests for ProductsNServices - Life Pension API - based on Swagger version: 1.0.0",
	summary = "Structural and logical tests for Life Pension API"
)
public class LifePensionTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(LifePensionTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Life Pension API test",
		displayName = "Validate structure of Life Pension response",
		summary = "Validate structure of Life Pension response",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE
	)
	public static class LifePensionTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate ProductsNServices Life Pension response", () -> {
				callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "life-pension");
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetLifePensionValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
