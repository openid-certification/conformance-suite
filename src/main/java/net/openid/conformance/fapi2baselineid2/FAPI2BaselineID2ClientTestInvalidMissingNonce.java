package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.as.RemoveNonceFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-baseline-id2-client-test-invalid-missing-nonce",
	displayName = "FAPI2-Baseline-ID2: client test - missing nonce value in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the nonce in the id_token from the authorization_endpoint is missing",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"directory.keystore"
	}
)

public class FAPI2BaselineID2ClientTestInvalidMissingNonce extends AbstractFAPI2BaselineID2ClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveNonceFromIdToken.class, "OIDCC-3.1.3.7-11");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "missing nonce value";
	}
}
