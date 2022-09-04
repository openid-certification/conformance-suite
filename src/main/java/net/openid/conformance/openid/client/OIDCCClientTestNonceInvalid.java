package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.AddInvalidNonceValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-nonce-invalid",
	displayName = "OIDCC: Relying party test. Invalid nonce in id token.",
	summary = "The client must identify that the 'nonce' value in the ID Token is invalid and must reject the ID Token." +
		" Corresponds to rp-nonce-invalid test in the old test suite",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
public class OIDCCClientTestNonceInvalid extends AbstractOIDCCClientTestExpectingNothingInvalidIdToken {

	@Override
	protected void generateIdTokenClaims() {
		super.generateIdTokenClaims();
		callAndStopOnFailure(AddInvalidNonceValueToIdToken.class, "OIDCC-2");
	}

	@Override
	protected String getAuthorizationCodeGrantTypeErrorMessage() {
		return "Client has incorrectly called token_endpoint after receiving an id_token with an invalid nonce value from the authorization_endpoint.";
	}

	@Override
	protected String getHandleUserinfoEndpointRequestErrorMessage() {
		return "Client has incorrectly called userinfo_endpoint after receiving an id_token with an invalid nonce value.";
	}

}
