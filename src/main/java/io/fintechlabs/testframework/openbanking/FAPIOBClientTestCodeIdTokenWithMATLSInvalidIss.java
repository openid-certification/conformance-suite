package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.as.AddInvalidIssValueToIdToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ob-client-test-code-id-token-with-matls-invalid-iss",
	displayName = "FAPI-OB: client test - invalid iss in id_token from authorization_endpoint should be rejected (code id_token with MATLS)",
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

		callAndStopOnFailure(AddInvalidIssValueToIdToken.class, "OIDCC-3.1.3.7.2");
	}

}
