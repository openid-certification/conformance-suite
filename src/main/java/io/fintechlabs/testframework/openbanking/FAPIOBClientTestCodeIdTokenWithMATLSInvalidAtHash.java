package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.AddInvalidAtHashValueToIdToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ob-client-test-code-id-token-with-matls-invalid-athash",
	displayName = "FAPI-OB: client test (code id_token with MATLS and an invalid at_hash value)",
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

public class FAPIOBClientTestCodeIdTokenWithMATLSInvalidAtHash extends AbstractFAPIOBClientMATLSExpectNothingAfterAuthorisationEndpoint {


	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidAtHashValueToIdToken.class, "OIDCC-3.2.2.10");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with an invalid at_hash value from the authorization_endpoint.");

	}

}
