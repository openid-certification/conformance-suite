package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.SignIdTokenInvalid;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-client-test-code-id-token-with-matls-invalid-signature",
	displayName = "FAPI-RW: client test - invalid signature in id_token from authorization_endpoint should be rejected (code id_token with MTLS)",
	summary = "This test should end with the client displaying an error message that the signature in the id_token from the authorization_endpoint does not match the signature value in the request object",
	profile = "FAPI-RW",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)

public class FAPIRWClientTestCodeIdTokenWithMATLSInvalidSignature extends AbstractFAPIRWClientExpectNothingAfterAuthorisationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {
		//Do Nothing
	}

	protected void addCustomSignatureOfIdToken(){

		callAndStopOnFailure(SignIdTokenInvalid.class, "OIDCC-3.1.3.7-6");

	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with an invalid signature from the authorization_endpoint.");

	}

}
