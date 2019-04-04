package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.AddSecondAudValueToIdToken;
import io.fintechlabs.testframework.openbanking.AbstractFAPIOBClientPrivateKeyExpectNothingAfterAuthorisationEndpoint;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-secondary-aud",
	displayName = "FAPI-RW: client test - multiple aud values in id_token from authorization_endpoint should be rejected (code id_token with private_key_jwt and MATLS)",
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

public class FAPIRWClientTestCodeIdTokenWithPrivateKeyJWTAndMATLSInvalidSecondaryAud extends AbstractFAPIRWClientPrivateKeyExpectNothingAfterAuthorisationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddSecondAudValueToIdToken.class, "OIDCC-3.1.3.7.8");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with multiple aud values from the authorization_endpoint.");

	}

}
