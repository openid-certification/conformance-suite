package net.openid.conformance.openinsurance.testmodule.structural.v1;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testmodule.support.*;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialClaimValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialListValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialPolicyInfoValidatorV1;
import net.openid.conformance.openinsurance.validator.insurancePatrimonial.v1.OpinInsurancePatrimonialPremiumValidatorV1;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "opin-patrimonial-api-structural-test",
	displayName = "Validate structure of Patrimonial API Endpoint 200 response",
	summary = "Validate structure of Patrimonial API Endpoint 200 response \n"+
	"\u2022 Call the “/\" endpoint - Expect 200 and validate response\n" +
	"\u2022 Call the “/{policyId}/policy-info\" endpoint - Expect 200 and validate response\n" +
	"\u2022 Call the “/{policyId}/premium\" endpoint - Expect 200 and validate response\n" +
	"\u2022 Call the “/{policyId}/claim\" endpoint - Expect 200 and validate response",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	configurationFields = {
			"resource.resourceUrl",
			"resource.mockPolicyId",
			})

public class OpinPatrimonialStructuralTestModule extends AbstractNoAuthFunctionalTestModule{

	@Override
	protected void runTests() {
		runInBlock("Validate Patrimonial - Root", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingPatrimonialRoot.class);
			callAndStopOnFailure(CallNoCacheResource.class);
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(OpinInsurancePatrimonialListValidatorV1.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validate Patrimonial - Policy-info", () -> {
			callAndContinueOnFailure(PolicyIDSelector.class);
			callAndStopOnFailure(PrepareUrlForFetchingPatrimonialPolicyInfo.class);
			callAndStopOnFailure(CallNoCacheResource.class);
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(OpinInsurancePatrimonialPolicyInfoValidatorV1.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validate Patrimonial - Premium", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingPatrimonialPremium.class);
			callAndStopOnFailure(CallNoCacheResource.class);
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(OpinInsurancePatrimonialPremiumValidatorV1.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validate Patrimonial - Claim", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingPatrimonialClaim.class);
			callAndStopOnFailure(CallNoCacheResource.class);
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(OpinInsurancePatrimonialClaimValidatorV1.class, Condition.ConditionResult.FAILURE);
		});

	}
}

