package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.as.AddIatValueIsWeekInPastToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-baseline-id2-client-test-iat-is-week-in-past",
	displayName = "FAPI2-Baseline-ID2: client test - iat value which is a week in the past in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the iat value in the id_token (from the authorization_endpoint) has expired (in the request object)",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"directory.keystore"
	}
)

public class FAPI2BaselineID2ClientTestIatIsWeekInPast extends AbstractFAPI2BaselineID2ClientExpectNothingAfterIdTokenIssued {

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
