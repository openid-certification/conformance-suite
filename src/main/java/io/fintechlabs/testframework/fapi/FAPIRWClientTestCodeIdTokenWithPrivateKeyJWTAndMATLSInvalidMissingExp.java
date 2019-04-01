package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.RemoveExpFromIdToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-missing-exp",
	displayName = "FAPI-RW: client test - missing exp value in id_token from authorization_endpoint, should be rejected (code id_token with private_key_jwt and MATLS)",
	summary = "This test should end with the client displaying an error message that the exp value in the id_token from the authorization_endpoint is missing",
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

public class FAPIRWClientTestCodeIdTokenWithPrivateKeyJWTAndMATLSInvalidMissingExp extends AbstractFAPIRWClientPrivateKeyExpectNothingAfterAuthorisationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveExpFromIdToken.class, "OIDCC-3.1.3.7-9");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with a missing exp value from the authorization_endpoint.");

	}

}
