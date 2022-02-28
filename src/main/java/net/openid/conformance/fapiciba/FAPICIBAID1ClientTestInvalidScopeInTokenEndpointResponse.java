package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import java.util.UUID;

/**
 * 5.2.3-10 Clients are now required to verify that the scope received in the token response is
 * either an exact match, or contains a subset of the scope sent in the authorization request.
 */
// TODO: Just copied the class here and changed the testName, displayName and profile.
@PublishTestModule(
	testName = "fapi-ciba-id1-client-test-invalid-scope-in-token-endpoint-response",
	displayName = "FAPI-CIBA-ID1: client test - token endpoint response will not contain a scope that was not requested, should be rejected",
	summary = "A random scope value, which was not requested by the client, will be included in the scope value in the token endpoint response.",
	profile = "FAPI-CIBA-ID1",
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
public class FAPICIBAID1ClientTestInvalidScopeInTokenEndpointResponse extends AbstractFAPICIBAID1ClientTest {

	@Override
	protected void addCustomValuesToIdToken() {
	}

	@Override
	protected void createTokenEndpointResponse() {
		String scope = env.getString("scope");
		String modifiedScope = scope + " " + UUID.randomUUID().toString();
		env.putString("scope", modifiedScope);
		callAndStopOnFailure(CreateTokenEndpointResponse.class);
		//reset
		env.putString("scope", scope);

		startWaitingForTimeout();
	}

	@Override
	protected Object accountsEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client has incorrectly called accounts endpoint after receiving " +
			"an invalid scope value in token response.");
	}

	@Override
	protected Object userinfoEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client has incorrectly called userinfo endpoint after receiving " +
			"an invalid scope value in token response.");
	}
}
