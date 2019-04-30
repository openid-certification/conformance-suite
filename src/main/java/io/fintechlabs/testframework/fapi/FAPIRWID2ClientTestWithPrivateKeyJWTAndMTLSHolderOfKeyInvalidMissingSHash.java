package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.RemoveSHashFromIdToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-missing-shash",
	displayName = "FAPI-RW-ID2: client test - missing shash value in id_token from authorization_endpoint, should be rejected (with private_key_jwt and MTLS)",
	summary = "This test should end with the client displaying an error message that the shash value in the id_token from the authorization_endpoint is missing",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)

public class FAPIRWID2ClientTestWithPrivateKeyJWTAndMTLSHolderOfKeyInvalidMissingSHash extends AbstractFAPIRWID2ClientPrivateKeyExpectNothingAfterAuthorisationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveSHashFromIdToken.class, "FAPI-RW-5.2.3");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with a missing s_hash value from the authorization_endpoint.");

	}

}
