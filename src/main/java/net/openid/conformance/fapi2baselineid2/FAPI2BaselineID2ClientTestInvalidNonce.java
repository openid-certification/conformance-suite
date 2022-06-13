package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.as.AddInvalidIssValueToIdToken;
import net.openid.conformance.condition.as.AddInvalidNonceValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-baseline-id2-client-test-invalid-nonce",
	displayName = "FAPI2-Baseline-ID2: client test - invalid nonce in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the nonce value in the id_token does not match the authorization_endpoint's nonce value",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)

public class FAPI2BaselineID2ClientTestInvalidNonce extends AbstractFAPI2BaselineID2ClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidNonceValueToIdToken.class, "OIDCC-3.1.3.7-11");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "invalid nonce value";
	}
}
