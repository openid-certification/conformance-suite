package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.as.AddInvalidExpiredExpValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-invalid-expired-exp",
	displayName = "FAPI-RW-ID2: client test - expired exp value in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the exp value in the id_token from the authorization_endpoint has expired more than 5 minutes in the past",
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

public class FAPIRWID2ClientTestInvalidExpiredExp extends AbstractFAPIRWID2ClientExpectNothingAfterAuthorizationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidExpiredExpValueToIdToken.class, "OIDCC-3.1.3.7-9");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new TestFailureException(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with a expired exp value from the authorization_endpoint.");
	}

}
