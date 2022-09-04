package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.RemoveAudFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-missing-aud",
	displayName = "OIDCC: Relying party test. Missing aud value in id token.",
	summary = "The client must identify that the 'aud' value is missing and reject the ID Token after doing ID Token validation." +
		" Corresponds to rp-id_token-aud test in the old test suite",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
public class OIDCCClientTestMissingAudInIdToken extends AbstractOIDCCClientTestExpectingNothingInvalidIdToken {

	@Override
	protected void generateIdTokenClaims() {
		super.generateIdTokenClaims();
		callAndStopOnFailure(RemoveAudFromIdToken.class, "OIDCC-2");
	}

	@Override
	protected String getAuthorizationCodeGrantTypeErrorMessage() {
		return "Client has incorrectly called token_endpoint after receiving an id_token with no aud claim from the authorization_endpoint.";
	}

	@Override
	protected String getHandleUserinfoEndpointRequestErrorMessage() {
		return "Client has incorrectly called userinfo_endpoint after receiving an id_token with no aud claim.";
	}

}
