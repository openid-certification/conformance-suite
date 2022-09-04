package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.RemoveExpFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-invalid-missing-exp",
	displayName = "FAPI1-Advanced-Final: client test - missing exp value in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the exp value in the id_token from the authorization_endpoint is missing",
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

public class FAPI1AdvancedFinalClientTestInvalidMissingExp extends AbstractFAPI1AdvancedFinalClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveExpFromIdToken.class, "OIDCC-3.1.3.7-9");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "missing exp value";
	}
}
