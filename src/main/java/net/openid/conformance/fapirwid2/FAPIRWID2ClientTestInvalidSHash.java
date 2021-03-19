package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.as.AddInvalidSHashValueToIdToken;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-invalid-shash",
	displayName = "FAPI-RW-ID2: client test - invalid s_hash in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the s_hash value in the id_token does not match the state the client sent",
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

public class FAPIRWID2ClientTestInvalidSHash extends AbstractFAPIRWID2ClientExpectNothingAfterAuthorizationEndpoint {

	@Override
	protected void endTestIfRequiredParametersAreMissing() {

		String shash = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE);
		if (shash == null) {
			// This throws an exception: the test will stop here
			fireTestSkipped("This test is being skipped as it relies on the client supplying a state value - since none is supplied, this can not be tested.");
		}
	}

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidSHashValueToIdToken.class, "FAPI-RW-5.2.3");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new TestFailureException(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with an invalid s_hash value from the authorization_endpoint.");

	}

}
