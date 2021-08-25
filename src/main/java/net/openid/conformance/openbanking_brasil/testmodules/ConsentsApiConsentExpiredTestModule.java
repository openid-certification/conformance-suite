package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddExpirationInOneMinute;
import net.openid.conformance.openbanking_brasil.testmodules.support.CheckAuthorizationEndpointHasError;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consent-api-expired-consent-test",
	displayName = "Validate that consents can expire",
	summary = "Validate that consents can expire",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl"
	}
)
public class ConsentsApiConsentExpiredTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		return super.createOBBPreauthSteps().
			replace(FAPIBrazilAddExpirationToConsentRequest.class, condition(AddExpirationInOneMinute.class));
	}

	@Override
	protected void performPreAuthorizationSteps() {
		super.performPreAuthorizationSteps();
		callAndContinueOnFailure(WaitFor2Seconds.class);
		callAndContinueOnFailure(WaitFor60Seconds.class);
	}



	@Override
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckMatchingCallbackParameters.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(RejectStateInUrlQueryForHybridFlow.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndStopOnFailure(CheckAuthorizationEndpointHasError.class);

		if (jarm) {
			callAndContinueOnFailure(ValidateSuccessfulJARMResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING);
		} else {
			callAndContinueOnFailure(ValidateSuccessfulHybridResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING);
		}

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.2.2.5", "JARM-4.4-2");

		callAndContinueOnFailure(ValidateIssInAuthorizationResponse.class, Condition.ConditionResult.WARNING, "OAuth2-iss-2");

		fireTestFinished();
	}


	@Override
	protected void validateResponse() {

	}

}
