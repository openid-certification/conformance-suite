package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.RemoveExpFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-invalid-missing-exp",
	displayName = "FAPI2-Security-Profile-Final: client test - missing exp value in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the exp value in the id_token from the token_endpoint is missing",
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

public class FAPI2SPFinalClientTestInvalidMissingExp extends AbstractFAPI2SPFinalClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveExpFromIdToken.class, "OIDCC-3.1.3.7-9");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "missing exp value";
	}
}
