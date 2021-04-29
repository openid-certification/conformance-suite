package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.FAPIProfile;
import net.openid.conformance.variant.VariantNotApplicable;

import java.util.UUID;

/**
 * 5.2.3-10 Clients are now required to verify that the scope received in the token response is
 * either an exact match, or contains a subset of the scope sent in the authorization request.
 */
@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-invalid-scope-in-token-endpoint-response",
	displayName = "FAPI1-Advanced-Final: client test - token endpoint response will not contain a scope that was not requested, should be rejected",
	summary = "A random scope value, which was not requested by the client, will be included in the scope value in the token endpoint response.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)
public class FAPI1AdvancedFinalClientTestInvalidScopeInTokenEndpointResponse extends AbstractFAPI1AdvancedFinalClientTest {

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
	protected Object userinfoEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client has incorrectly called userinfo endpoint after receiving " +
			"an invalid scope value in token response.");
	}
}
