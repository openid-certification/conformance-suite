package net.openid.conformance.openid.client;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-client-test-missing-chash",
	displayName = "OIDCC: Relying party test. No c_hash in id_token.",
	summary = "The client must identify missing 'c_hash' value and reject the ID Token." +
		" Corresponds to rp-id_token-missing-c_hash test in the old test suite",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"code", "id_token", "id_token token", "code token"})
public class OIDCCClientTestMissingCHashInIdToken extends AbstractOIDCCClientTestExpectingNothingInvalidIdToken {

	@Override
	protected void addCHashToIdToken() {
		//do nothing, don't add it
	}

	@Override
	protected String getAuthorizationCodeGrantTypeErrorMessage() {
		return "Client has incorrectly called token_endpoint after receiving an id_token without a c_hash value from the authorization_endpoint.";
	}

	@Override
	protected String getHandleUserinfoEndpointRequestErrorMessage() {
		return "Client has incorrectly called userinfo_endpoint after receiving an id_token without a c_hash value.";
	}

}
