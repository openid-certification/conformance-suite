package net.openid.conformance.openinsurance.testplan.deprecated;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.testplan.utils.PrepareToGetOpenInsuranceApi;
import net.openid.conformance.openinsurance.validator.rural.v1.OpinInsuranceRuralClaimListValidatorV1;
import net.openid.conformance.openinsurance.validator.rural.v1.OpinInsuranceRuralListValidatorV1;
import net.openid.conformance.openinsurance.validator.rural.v1.OpinInsuranceRuralPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.rural.v1.OpinInsuranceRuralPremiumValidatorV1;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "OpenInsurance Brasil - Rural API - Phase 2 test plan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	displayName = PlanNames.INSURANCE_RURAL_API_TEST_PLAN_PHASE2,
	summary = "Structural and logical tests for Rural API"
)
public class OpinInsuranceRuralTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(OpenInsuranceRuralTestModule.class),
				List.of(new Variant(ClientAuthType.class, "none"))
			)
		);
	}

	@PublishTestModule(
		testName = "OpenInsurance Brasil - Rural API test",
		displayName = "Validate structure of Rural API",
		summary = "Validate structure of Rural API",
		profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2
	)
	public static class OpenInsuranceRuralTestModule extends AbstractNoAuthFunctionalTestModule {
		@Override
		protected void runTests() {
			runInBlock("Validate OpenInsurance Brasil Rural List", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(OpinInsuranceRuralListValidatorV1.class, Condition.ConditionResult.FAILURE);
			});

			runInBlock("Validate OpenInsurance Brasil Rural Claim list", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(OpinInsuranceRuralClaimListValidatorV1.class, Condition.ConditionResult.FAILURE);
			});

			runInBlock("Validate OpenInsurance Brasil Rural Policy Info response", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(OpinInsuranceRuralPolicyInfoValidatorV1.class, Condition.ConditionResult.FAILURE);
			});

			runInBlock("Validate OpenInsurance Brasil Rural Premium response", () -> {
				callAndStopOnFailure(PrepareToGetOpenInsuranceApi.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(OpinInsuranceRuralPremiumValidatorV1.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
