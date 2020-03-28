package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.AddInvalidCHashValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-client-test-invalid-chash",
	displayName = "OIDCC: Relying party test. Invalid c_hash in id_token.",
	summary = "The client must identify invalid 'c_hash' value and reject the ID Token." +
		" 'id_token_signed_response_alg' for the client must NOT be 'none'." +
		" Corresponds to rp-id_token-bad-c_hash test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"code", "id_token", "id_token token", "code token"})
public class OIDCCClientTestInvalidCHashInIdToken extends AbstractOIDCCClientTestExpectingNothingInvalidIdToken {

	@Override
	protected void addCHashToIdToken() {
		callAndStopOnFailure(AddInvalidCHashValueToIdToken.class, "OIDCC-3.3.2.10");
	}

	@Override
	protected String getAuthorizationCodeGrantTypeErrorMessage() {
		return "Client has incorrectly called token_endpoint after receiving an id_token with an invalid c_hash value from the authorization_endpoint.";
	}

	@Override
	protected String getHandleUserinfoEndpointRequestErrorMessage() {
		return "Client has incorrectly called userinfo_endpoint after receiving an id_token with an invalid c_hash value.";
	}

}
