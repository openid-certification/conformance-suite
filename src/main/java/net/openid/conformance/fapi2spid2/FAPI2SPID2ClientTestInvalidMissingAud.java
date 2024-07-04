package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.as.RemoveAudFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-invalid-missing-aud",
	displayName = "FAPI2-Security-Profile-ID2: client test - missing aud value in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the aud value in the id_token from the token_endpoint is missing",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"waitTimeoutSeconds"
	}
)

public class FAPI2SPID2ClientTestInvalidMissingAud extends AbstractFAPI2SPID2ClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveAudFromIdToken.class, "OIDCC-3.1.3.7-3");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "missing aud value";
	}
}
