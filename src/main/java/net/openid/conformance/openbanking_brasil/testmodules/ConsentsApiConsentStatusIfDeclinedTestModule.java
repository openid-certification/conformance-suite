package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consent-api-status-declined-test",
	displayName = "Validate that consents are rejected on decline",
	summary = "Validate that consents are rejected on decline",
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
public class ConsentsApiConsentStatusIfDeclinedTestModule extends AbstractOBBrasilFunctionalTestModule {
	@Override
	protected void validateResponse() {
		runInBlock("Validating get consent response", () -> {
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndStopOnFailure(TransformConsentRequestForProtectedResource.class);
			call(createGetAccessTokenWithClientCredentialsSequence(addTokenEndpointClientAuthentication));
			preCallProtectedResource("Fetch consent");

			callAndStopOnFailure(ConsentWasRejectedOrDeleted.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureConsentWasRejected.class, Condition.ConditionResult.WARNING);
		});
	}

	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckMatchingCallbackParameters.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(RejectStateInUrlQueryForHybridFlow.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndStopOnFailure(CheckAuthorizationEndpointHasError.class);

		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

		callAndContinueOnFailure(EnsureAccessDeniedErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.2.2.5", "JARM-4.4-2");

		callAndContinueOnFailure(ValidateIssInAuthorizationResponse.class, Condition.ConditionResult.WARNING, "OAuth2-iss-2");

		eventLog.startBlock(currentClientString() + "Validate response");
		validateResponse();
		eventLog.endBlock();

		fireTestFinished();
	}

}
