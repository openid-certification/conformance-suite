package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.as.AddInvalidExpiredExpValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-invalid-expired-exp",
	displayName = "FAPI2-Security-Profile-ID2: client test - expired exp value in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the exp value in the id_token from the token_endpoint has expired more than 5 minutes in the past",
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

public class FAPI2SPID2ClientTestInvalidExpiredExp extends AbstractFAPI2SPID2ClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidExpiredExpValueToIdToken.class, "OIDCC-3.1.3.7-9");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "expired exp value";
	}
}
