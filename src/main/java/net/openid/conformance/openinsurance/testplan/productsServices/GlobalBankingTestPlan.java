package net.openid.conformance.openinsurance.testplan.productsServices;

	import net.openid.conformance.condition.Condition;
	import net.openid.conformance.openbanking_brasil.OBBProfile;
	import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
	import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
	import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
	import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
	import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetOpenInsuranceApi;
	import net.openid.conformance.openinsurance.validator.productsServices.GetGlobalBankingValidator;
	import net.openid.conformance.plan.PublishTestPlan;
	import net.openid.conformance.plan.TestPlan;
	import net.openid.conformance.testmodule.PublishTestModule;
	import net.openid.conformance.variant.ClientAuthType;

	import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - ProductsServices - Global Banking API test plan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1,
	displayName = PlanNames.GLOBAL_BANKING_API_TEST_PLAN,
	summary = "Structural and logical tests for Global Banking API"
)
public class GlobalBankingTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(GlobalBankingyTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Open Insurance - Global Banking API test",
		displayName = "Validate structure of Global Banking response",
		summary = "Validate structure of Global Banking response",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1
	)
	public static class GlobalBankingyTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate ProductsServices Global Banking response", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetGlobalBankingValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
