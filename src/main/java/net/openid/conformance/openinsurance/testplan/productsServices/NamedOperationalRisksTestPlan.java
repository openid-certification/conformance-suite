package net.openid.conformance.openinsurance.testplan.productsServices;

	import net.openid.conformance.condition.Condition;
	import net.openid.conformance.openbanking_brasil.OBBProfile;
	import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
	import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
	import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
	import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
	import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetOpenInsuranceApi;
	import net.openid.conformance.openinsurance.validator.productsServices.GetNamedOperationalRisksValidator;
	import net.openid.conformance.plan.PublishTestPlan;
	import net.openid.conformance.plan.TestPlan;
	import net.openid.conformance.testmodule.PublishTestModule;
	import net.openid.conformance.variant.ClientAuthType;

	import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance - ProductsServices - Named Operational Risks API test plan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1,
	displayName = PlanNames.NAMED_OPERATIONAL_RISKS_API_TEST_PLAN,
	summary = "Structural and logical tests for Named Operational Risks API"
)
public class NamedOperationalRisksTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(NamedOperationalRisksTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "Open Insurance - Named Operational Risks API test",
		displayName = "Validate structure of Named Operational Risks response",
		summary = "Validate structure of Named Operational Risks response",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1
	)
	public static class NamedOperationalRisksTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate ProductsServices Named Operational Risks response", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(GetNamedOperationalRisksValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
