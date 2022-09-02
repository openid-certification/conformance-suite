package net.openid.conformance.openinsurance.testmodule.structural.v1;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openinsurance.testmodule.support.PrepareToGetPersonalComplimentaryInformation;
import net.openid.conformance.openinsurance.testmodule.support.PrepareToGetPersonalIdentifications;
import net.openid.conformance.openinsurance.testmodule.support.PrepareToGetPersonalQualifications;
import net.openid.conformance.openinsurance.testplan.utils.CallNoCacheResource;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersPersonalComplimentaryInformationListValidatorV1;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersPersonalIdentificationListValidatorV1;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersPersonalQualificationListValidatorV1;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "Open Insurance - Personal - Structural API test",
	displayName = "Validate structure of Customer - Personal API resources",
	summary = "Call the “/personal/identifications\" endpoint - Expect 200 and validate response\n" +
		      "Call the “/personal/qualifications\" endpoint - Expect 200 and validate response\n" +
		      "Call the “/personal/complimentary-information\" endpoint - Expect 200 and validate response",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1,
	configurationFields = {
	"resource.resourceUrl",
	"resource.mockPolicyId"
	}
)
public class OpinCustomerPersonalStructuralTestModule extends AbstractNoAuthFunctionalTestModule{

		@Override
		protected void runTests() {
			runInBlock("Validate Personal - Identifications response", () -> {
				callAndStopOnFailure(PrepareToGetPersonalIdentifications.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(OpinCustomersPersonalIdentificationListValidatorV1.class, Condition.ConditionResult.FAILURE);
			});
			runInBlock("Validate Personal - Qualifications response", () -> {
				callAndStopOnFailure(PrepareToGetPersonalQualifications.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(OpinCustomersPersonalQualificationListValidatorV1.class, Condition.ConditionResult.FAILURE);
			});
			runInBlock("Validate Personal - Complimentary-Information response", () -> {
				callAndStopOnFailure(PrepareToGetPersonalComplimentaryInformation.class);
				callAndStopOnFailure(CallNoCacheResource.class);
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(OpinCustomersPersonalComplimentaryInformationListValidatorV1.class, Condition.ConditionResult.FAILURE);
			});
		}
	}

