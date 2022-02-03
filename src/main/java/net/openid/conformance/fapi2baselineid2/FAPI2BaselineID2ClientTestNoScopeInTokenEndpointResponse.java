package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.testmodule.PublishTestModule;

/**
 * 5.2.2-14 Scopes granted in the token endpoint response can now be omitted except in the case where the
 * authorization request was passed in the front channel (via a web browser) and was not integrity protected.
 * This means requests using a signed request object or PAR can adopt the standard OAuth2 behaviour of only
 * returning the granted scopes if they're different from the requested scopes.
 */
@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-no-scope-in-token-endpoint-response",
	displayName = "FAPI1-Advanced-Final: client test - token endpoint response will not contain the granted scopes, should be accepted",
	summary = "Same as the happy path flow except the token endpoint response will not contain the granted scopes. The client must assume that they are the same as the requested scopes.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"directory.keystore"
	}
)
public class FAPI2BaselineID2ClientTestNoScopeInTokenEndpointResponse extends AbstractFAPI2BaselineID2ClientTest {

	@Override
	protected void addCustomValuesToIdToken() {
	}

	@Override
	protected void createTokenEndpointResponse() {
		String scope = env.getString("scope");
		env.removeNativeValue("scope");
		callAndStopOnFailure(CreateTokenEndpointResponse.class);
		env.putString("scope", scope);
	}
}
