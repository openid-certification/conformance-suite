package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.as.RemoveExpFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-invalid-missing-exp",
	displayName = "FAPI-RW-ID2: client test - missing exp value in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the exp value in the id_token from the authorization_endpoint is missing",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)

public class FAPIRWID2ClientTestInvalidMissingExp extends AbstractFAPIRWID2ClientExpectNothingAfterAuthorizationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(RemoveExpFromIdToken.class, "OIDCC-3.1.3.7-9");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new TestFailureException(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with a missing exp value from the authorization_endpoint.");
	}

}
