package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.as.AddIatValueIsWeekInPastToIdToken;
import io.fintechlabs.testframework.condition.as.ClientContinuedAfterReceivingIdTokenIssuedInPast;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


@PublishTestModule(
	testName = "fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-iat-is-week-in-past",
	displayName = "FAPI-RW-ID2-OB: client test - iat value which is a week in the past in id_token from authorization_endpoint, should be rejected (with MTLS)",
	summary = "This test should end with the client displaying an error message that the iat value in the id_token (from the authorization_endpoint) has expired (in the request object)",
	profile = "FAPI-RW-ID2-OB",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)

public class FAPIRWID2OBClientTestWithMTLSHolderOfKeyIatIsWeekInPast extends AbstractFAPIRWID2OBClientMTLSHolderOfKeyExpectNothingAfterAuthorisationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddIatValueIsWeekInPastToIdToken.class,"OIDCC-3.1.3.7-10");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		callAndContinueOnFailure(ClientContinuedAfterReceivingIdTokenIssuedInPast.class, ConditionResult.WARNING);
		setStatus(Status.WAITING);
		fireTestFinished();
		return new ResponseEntity<Object>("Client has incorrectly called token_endpoint after receiving an id_token with an iat value which is a week in the past from the authorization_endpoint.", HttpStatus.BAD_REQUEST);
	}

}
