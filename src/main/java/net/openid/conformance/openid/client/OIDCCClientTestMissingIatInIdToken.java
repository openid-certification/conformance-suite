package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.RemoveIatFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-missing-iat",
	displayName = "OIDCC: Relying party test. Missing iat value in id token.",
	summary = "The client must identify the missing 'iat' value and reject the ID Token after doing ID Token validation." +
		" Corresponds to rp-id_token-iat test in the old test suite",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
public class OIDCCClientTestMissingIatInIdToken extends AbstractOIDCCClientTestExpectingNothingInvalidIdToken {

	@Override
	protected void generateIdTokenClaims() {
		super.generateIdTokenClaims();
		callAndStopOnFailure(RemoveIatFromIdToken.class, "OIDCC-2");
	}

	@Override
	protected String getAuthorizationCodeGrantTypeErrorMessage() {
		return "Client has incorrectly called token_endpoint after receiving an id_token with no iat claim from the authorization_endpoint.";
	}

	@Override
	protected String getHandleUserinfoEndpointRequestErrorMessage() {
		return "Client has incorrectly called userinfo_endpoint after receiving an id_token with no iat claim.";
	}

}
