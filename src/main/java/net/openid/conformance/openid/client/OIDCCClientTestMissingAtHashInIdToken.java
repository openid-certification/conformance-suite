package net.openid.conformance.openid.client;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-client-test-missing-athash",
	displayName = "OIDCC: Relying party test. Missing at_hash in id_token.",
	summary = "The client must identify the missing 'at_hash' value and reject the ID Token after doing Access Token validation." +
		" Make an authentication request using response_type='id_token token' for Implicit Flow or " +
		"response_type='code id_token token' for Hybrid Flow. Verify the 'at_hash' presence in the returned ID Token." +
		" Corresponds to rp-id_token-missing-at_hash test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"code", "id_token", "code id_token", "code token"})
public class OIDCCClientTestMissingAtHashInIdToken extends AbstractOIDCCClientTestExpectingNothingInvalidIdToken {

	@Override
	protected void addAtHashToIdToken() {
		//do not add it
	}

	@Override
	protected boolean isAuthorizationCodeRequestUnexpected() {
		return responseType.includesIdToken() && responseType.includesToken();
	}

	@Override
	protected String getAuthorizationCodeGrantTypeErrorMessage() {
		return "Client has incorrectly called token_endpoint after receiving an id_token without an at_hash value from the authorization_endpoint.";
	}

	@Override
	protected String getHandleUserinfoEndpointRequestErrorMessage() {
		return "Client has incorrectly called userinfo_endpoint after receiving an id_token without an at_hash value.";
	}

}
