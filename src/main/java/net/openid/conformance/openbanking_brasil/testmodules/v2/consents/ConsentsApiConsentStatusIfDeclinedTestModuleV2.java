package net.openid.conformance.openbanking_brasil.testmodules.v2.consents;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckMatchingCallbackParameters;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.RejectStateInUrlQueryForHybridFlow;
import net.openid.conformance.condition.client.ValidateIssInAuthorizationResponse;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildAccountsConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.CheckAuthorizationEndpointHasError;
import net.openid.conformance.openbanking_brasil.testmodules.support.ConsentWasRejectedOrDeleted;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureConsentWasRejected;
import net.openid.conformance.openbanking_brasil.testmodules.support.ObtainAccessTokenWithClientCredentials;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToFetchConsentRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.TransformConsentRequestForProtectedResource;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consent-api-status-declined-test-v2",
	displayName = "Validate that consents V2 are rejected on decline",
	summary = "Validates that consents V2 are rejected on the decline\n" +
		"\u2022 Creates a Consent V2 with all of the existing permissions\n" +
		"\u2022 Checks all of the fields sent on the consent API V2 are specification compliant\n" +
		"\u2022 Expects a valid consent creation 201\n" +
		"\u2022 Redirects the User to Reject the Consent\n" +
		"\u2022 Checks if rejection resulted in an error message on the redirect\n" +
		"\u2022 Calls the GET Consents V2 endpoint\n" +
		"\u2022 Expects a 200 with the Consent being on a rejected status",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
public class ConsentsApiConsentStatusIfDeclinedTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient(){
		//Arbitrary resource
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void validateResponse() {
		runInBlock("Validating get consent response V2", () -> {
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

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.2.2.5", "JARM-4.4-2");

		callAndContinueOnFailure(ValidateIssInAuthorizationResponse.class, Condition.ConditionResult.WARNING, "OAuth2-iss-2");

		eventLog.startBlock(currentClientString() + "Validate response");
		validateResponse();
		eventLog.endBlock();

		fireTestFinished();
	}

}
