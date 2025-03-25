package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.AddInvalidAudValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-invalid-aud",
	displayName = "FAPI2-Security-Profile-Final: client test - invalid aud in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the aud value in the id_token does not match the client id",
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

public class FAPI2SPFinalClientTestInvalidAud extends AbstractFAPI2SPFinalClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidAudValueToIdToken.class, "OIDCC-3.1.3.7-3");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "invalid aud value";
	}

}
