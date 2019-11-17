package net.openid.conformance.openid.client;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.as.AddInvalidNonceValueToIdToken;
import net.openid.conformance.condition.as.RemoveSubFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-missing-sub",
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
	protected void generateIdTokenClaims()
	{
		super.generateIdTokenClaims();
		callAndStopOnFailure(RemoveSubFromIdToken.class);
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {
		if(responseType.includesIdToken()) {
			throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token without a sub value from the authorization_endpoint.");
		} else {
			startWaitingForTimeout();
		}
		return super.authorizationCodeGrantType(requestId);
	}

	@Override
	protected Object handleUserinfoEndpointRequest(String requestId)
	{
		throw new ConditionError(getId(), "Client has incorrectly called userinfo_endpoint after receiving an id_token without a sub value.");
	}
}
