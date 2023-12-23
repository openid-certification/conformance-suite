package net.openid.conformance.fapi1advancedfinalfapibrv1;

import net.openid.conformance.condition.as.AddIatValueIsWeekInPastToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-br-v1-client-test-iat-is-week-in-past",
	displayName = "FAPI1-Advanced-Final-Br-v1: client test - iat value which is a week in the past in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the iat value in the id_token (from the authorization_endpoint) has expired (in the request object)",
	profile = "FAPI1-Advanced-Final-Br-v1",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)

public class FAPI1AdvancedFinalBrV1ClientTestIatIsWeekInPast extends AbstractFAPI1AdvancedFinalBrV1ClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddIatValueIsWeekInPastToIdToken.class, "OIDCC-3.1.3.7-10");
	}
/*
TODO why was this class different from other similar classes?
	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		callAndContinueOnFailure(ClientContinuedAfterReceivingIdTokenIssuedInPast.class, ConditionResult.WARNING);
		setStatus(Status.WAITING);
		fireTestFinished();
		return new ResponseEntity<Object>("Client has incorrectly called token_endpoint after receiving an id_token with an iat value which is a week in the past from the authorization_endpoint.", HttpStatus.BAD_REQUEST);
	}
*/
	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "iat value is a week in the past";
	}
}
