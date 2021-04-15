package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.as.AddInvalidCHashValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-invalid-chash",
	displayName = "FAPI-RW-ID2: client test - invalid c_hash in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the c_hash value in the id_token from the authorization_endpoint does not match the c_hash value in the request object",
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

public class FAPIRWID2ClientTestInvalidCHash extends AbstractFAPIRWID2ClientExpectNothingAfterAuthorizationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidCHashValueToIdToken.class, "OIDCC-3.3.2-10");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new TestFailureException(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with an invalid c_hash value from the authorization_endpoint.");
	}

}
