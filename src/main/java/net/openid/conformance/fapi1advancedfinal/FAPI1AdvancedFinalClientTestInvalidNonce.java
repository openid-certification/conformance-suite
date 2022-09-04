package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.AddInvalidNonceValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-invalid-nonce",
	displayName = "FAPI1-Advanced-Final: client test - invalid nonce in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the nonce value in the id_token from the authorization_endpoint does not match the nonce value in the request object",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)

public class FAPI1AdvancedFinalClientTestInvalidNonce extends AbstractFAPI1AdvancedFinalClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidNonceValueToIdToken.class, "OIDCC-3.1.3.7-11");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "invalid nonce value";
	}
}
