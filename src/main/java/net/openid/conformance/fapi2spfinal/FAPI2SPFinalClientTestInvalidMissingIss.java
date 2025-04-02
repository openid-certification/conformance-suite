package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.RemoveIssFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-invalid-missing-iss",
	displayName = "FAPI2-Security-Profile-Final: client test - missing iss value in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the iss value in the id_token from the token_endpoint is missing",
	profile = "FAPI2-Security-Profile-Final",
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

public class FAPI2SPFinalClientTestInvalidMissingIss extends AbstractFAPI2SPFinalClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveIssFromIdToken.class, "OIDCC-3.1.3.7-2");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "missing iss value";
	}


}
