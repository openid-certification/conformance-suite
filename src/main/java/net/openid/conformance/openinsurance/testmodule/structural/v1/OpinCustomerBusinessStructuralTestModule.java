package net.openid.conformance.openinsurance.testmodule.structural.v1;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testmodule.support.PrepareToGetBusinessComplimentaryInformation;
import net.openid.conformance.openinsurance.testmodule.support.PrepareToGetBusinessIdentifications;
import net.openid.conformance.openinsurance.testmodule.support.PrepareToGetBusinessQualifications;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersBusinessComplimentaryInformationListValidatorV1;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersBusinessIdentificationListValidatorV1;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersBusinessQualificationListValidatorV1;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "Open Insurance - Business - Structural API test",
	displayName = "Validate structure of Customer - Business API resources",
	summary = "Call the “/business/identifications\" endpoint - Expect 200 and validate response\n" +
		"Call the “/business/qualifications\" endpoint - Expect 200 and validate response\n" +
		"Call the “/business/complimentary-information\" endpoint - Expect 200 and validate response",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	configurationFields = {
		"resource.resourceUrl",
		"resource.mockPolicyId"
	}
)
public class OpinCustomerBusinessStructuralTestModule extends AbstractNoAuthFunctionalTestModule{

	@Override
	protected void runTests() {
		runInBlock("Validate Business - Identifications response", () -> {
			callAndStopOnFailure(PrepareToGetBusinessIdentifications.class);
			callAndStopOnFailure(CallNoCacheResource.class);
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(OpinCustomersBusinessIdentificationListValidatorV1.class, Condition.ConditionResult.FAILURE);
		});
		runInBlock("Validate Business - Qualifications response", () -> {
			callAndStopOnFailure(PrepareToGetBusinessQualifications.class);
			callAndStopOnFailure(CallNoCacheResource.class);
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(OpinCustomersBusinessQualificationListValidatorV1.class, Condition.ConditionResult.FAILURE);
		});
		runInBlock("Validate Business - Complimentary-Information response", () -> {
			callAndStopOnFailure(PrepareToGetBusinessComplimentaryInformation.class);
			callAndStopOnFailure(CallNoCacheResource.class);
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(OpinCustomersBusinessComplimentaryInformationListValidatorV1.class, Condition.ConditionResult.FAILURE);
		});
	}
}
