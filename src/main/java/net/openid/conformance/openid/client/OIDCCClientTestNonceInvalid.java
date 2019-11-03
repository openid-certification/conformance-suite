package net.openid.conformance.openid.client;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.as.AddInvalidNonceValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

/**
 * the default happy path test
 */
@PublishTestModule(
	testName = "oidcc-client-nonce-invalid",
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
	protected void generateIdTokenClaims()
	{
		super.generateIdTokenClaims();
		callAndStopOnFailure(AddInvalidNonceValueToIdToken.class);
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {
		if(responseType.includesIdToken()) {
			throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with an invalid nonce value from the authorization_endpoint.");
		} else {
			startWaitingForTimeout();
		}
		return super.authorizationCodeGrantType(requestId);
	}

	@Override
	protected Object handleUserinfoEndpointRequest(String requestId)
	{
		throw new ConditionError(getId(), "Client has incorrectly called userinfo_endpoint after receiving an id_token with an invalid nonce value.");
	}
}
