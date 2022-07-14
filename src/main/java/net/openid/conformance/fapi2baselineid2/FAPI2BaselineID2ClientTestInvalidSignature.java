package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.as.InvalidateIdTokenSignature;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-baseline-id2-client-test-invalid-signature",
	displayName = "FAPI2-Baseline-ID2: client test - invalid signature in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the signature in the id_token from the token_endpoint is invalid",
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

public class FAPI2BaselineID2ClientTestInvalidSignature extends AbstractFAPI2BaselineID2ClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		//Do Nothing
	}

	@Override
	protected void addCustomSignatureOfIdToken(){

		callAndStopOnFailure(InvalidateIdTokenSignature.class, "OIDCC-3.1.3.7-6");

	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "invalid signature";
	}
}
