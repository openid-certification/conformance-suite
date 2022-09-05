package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.AddInvalidAtHashValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-client-test-invalid-athash",
	displayName = "OIDCC: Relying party test. Invalid at_hash in id_token.",
	summary = "The client must Identify the incorrect 'at_hash' value and reject the ID Token after doing Access Token validation." +
		" Corresponds to rp-id_token-bad-at_hash test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"code", "id_token", "code id_token", "code token"})
public class OIDCCClientTestInvalidAtHashInIdToken extends AbstractOIDCCClientTestExpectingNothingInvalidIdToken {

	@Override
	protected void addAtHashToIdToken() {
		callAndStopOnFailure(AddInvalidAtHashValueToIdToken.class, "OIDCC-3.3.2.11");
	}

	@Override
	protected boolean isAuthorizationCodeRequestUnexpected() {
		return responseType.includesIdToken() && responseType.includesToken();
	}

	@Override
	protected String getAuthorizationCodeGrantTypeErrorMessage() {
		return "Client has incorrectly called token_endpoint after receiving an id_token with an invalid at_hash value from the authorization_endpoint.";
	}

	@Override
	protected String getHandleUserinfoEndpointRequestErrorMessage() {
		return "Client has incorrectly called userinfo_endpoint after receiving an id_token with an invalid at_hash value.";
	}

}
