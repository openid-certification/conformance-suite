package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.AddInvalidIssValueToIdToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ob-client-test-code-id-token-with-matls-invalid-iss",
	displayName = "FAPI-OB: client test - invalid iss in id_token from authorization_endpoint should be rejected (code id_token with MATLS)",
	summary = "This test should end with the client displaying an error message that the iss value in the id_token does not match the authorization server's issuer",
	profile = "FAPI-OB",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)

public class FAPIOBClientTestCodeIdTokenWithMATLSInvalidIss extends AbstractFAPIOBClientMATLSExpectNothingAfterAuthorisationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidIssValueToIdToken.class, "OIDCC-3.1.3.7-2");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with an invalid iss value from the authorization_endpoint.");

	}

}
