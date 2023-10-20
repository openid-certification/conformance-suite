package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.RemoveAccessTokenExpiration;
import net.openid.conformance.testmodule.PublishTestModule;

/**
 * 5.2.2-14 Scopes granted in the token endpoint response can now be omitted except in the case where the
 * authorization request was passed in the front channel (via a web browser) and was not integrity protected.
 * This means requests using a signed request object or PAR can adopt the standard OAuth2 behaviour of only
 * returning the granted scopes if they're different from the requested scopes.
 */
@PublishTestModule(
	testName = "fapi-ciba-id1-client-no-scope-in-token-endpoint-response-test",
	displayName = "FAPI-CIBA-ID1: Client test - token endpoint response will not contain the granted scopes, should be accepted",
	summary = "Same as the happy path flow except the token endpoint response will not contain the granted scopes. " +
		"The client must assume that they are the same as the requested scopes. " +
		"This test also does not return the 'expires_in' parameter from the token endpoint, which is valid and the client must accept.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.backchannel_client_notification_endpoint",
		"client.certificate",
		"client.jwks"
	}
)
public class FAPICIBAClientNoScopeInTokenEndpointResponseTest extends AbstractFAPICIBAClientTest {

	@Override
	protected void issueAccessToken() {
		super.issueAccessToken();
		callAndContinueOnFailure(RemoveAccessTokenExpiration.class, Condition.ConditionResult.INFO);
	}

	@Override
	protected void createFinalTokenResponse() {
		String scope = env.getString("scope");
		env.removeNativeValue("scope");
		callAndStopOnFailure(CreateTokenEndpointResponse.class);
		env.putString("scope", scope);
	}

}
