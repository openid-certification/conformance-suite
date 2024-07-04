package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.RemoveAccessTokenExpiration;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-token-endpoint-response-without-expires_in",
	displayName = "FAPI2-Security-Profile-ID2: client test - return the token endpoint response without the expires_in parameter",
	summary = "Tests a 'happy path' flow; the client should perform OpenID discovery from the displayed discoveryUrl, call the authorization endpoint (which will immediately redirect back), exchange the authorization code for an access token at the token endpoint and make a GET request to the resource endpoint displayed (usually the 'accounts' or 'userinfo' endpoint).",
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

public class FAPI2SPID2ClientTestTokenEndpointResponseWithoutExpiresIn extends AbstractFAPI2SPID2ClientTest {

	@Override
	protected void addCustomValuesToIdToken() {
	}

	@Override
	protected void issueAccessToken() {
		super.issueAccessToken();
		callAndContinueOnFailure(RemoveAccessTokenExpiration.class, Condition.ConditionResult.INFO);
	}

}
