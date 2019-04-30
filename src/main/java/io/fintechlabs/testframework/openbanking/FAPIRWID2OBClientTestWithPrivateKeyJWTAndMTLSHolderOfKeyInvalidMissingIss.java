package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.RemoveIssFromIdToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ob-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-missing-iss",
	displayName = "FAPI-RW-ID2-OB: client test - missing iss value in id_token from authorization_endpoint, should be rejected (with private_key_jwt and MTLS)",
	summary = "This test should end with the client displaying an error message that the iss value in the id_token from the authorization_endpoint is missing",
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

public class FAPIRWID2OBClientTestWithPrivateKeyJWTAndMTLSHolderOfKeyInvalidMissingIss extends AbstractFAPIRWID2OBClientPrivateKeyExpectNothingAfterAuthorisationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveIssFromIdToken.class, "OIDCC-3.1.3.7-2");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with a missing iss value from the authorization_endpoint.");
	}

}
