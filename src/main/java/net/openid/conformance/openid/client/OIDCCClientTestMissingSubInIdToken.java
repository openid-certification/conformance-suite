package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.RemoveSubFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-missing-sub",
	displayName = "OIDCC: Relying party test. No sub in id_token.",
	summary = "The client must identify the missing 'sub' claim and must reject the ID Token." +
		" Corresponds to rp-id_token-sub test in the old test suite",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
public class OIDCCClientTestMissingSubInIdToken extends AbstractOIDCCClientTestExpectingNothingInvalidIdToken {

	@Override
	protected void generateIdTokenClaims() {
		super.generateIdTokenClaims();
		callAndStopOnFailure(RemoveSubFromIdToken.class, "OIDCC-2");
	}

	@Override
	protected String getAuthorizationCodeGrantTypeErrorMessage() {
		return "Client has incorrectly called token_endpoint after receiving an id_token without a sub value from the authorization_endpoint.";
	}

	@Override
	protected String getHandleUserinfoEndpointRequestErrorMessage() {
		return "Client has incorrectly called userinfo_endpoint after receiving an id_token without a sub value.";
	}

}
