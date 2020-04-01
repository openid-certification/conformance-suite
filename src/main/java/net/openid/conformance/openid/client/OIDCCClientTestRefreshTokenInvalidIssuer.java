package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.AddInvalidIssValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * 12.2.  Successful Refresh Response
 *     - its iss Claim Value MUST be the same as in the ID Token issued when the original authentication occurred,
 */
@PublishTestModule(
	testName = "oidcc-client-test-refresh-token-invalid-issuer",
	displayName = "OIDCC: Relying party refresh token test, invalid iss in id_token returned in refresh response",
	summary = "The client is expected to detect that the id_token returned in refresh response contains an iss value " +
		"that is not the same as the iss value in the original id_token." +
		" This is a new test with no corresponding test in the old conformance suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token", "id_token token"})
public class OIDCCClientTestRefreshTokenInvalidIssuer extends AbstractOIDCCClientTestExpectingNothingAfterRefreshResponse {
	@Override
	protected String getHandleUserinfoEndpointRequestErrorMessage() {
		return "The client should not send a userinfo request after receiving an invalid refresh response. " +
			"Clients are expected to send a token request to " +
			"obtain a refresh token and then use that refresh token to send a refresh request and " +
			"detect that the id_token in the refresh response contains an invalid iss value.";
	}

	@Override
	protected void addCustomValuesToIdTokenForRefreshResponse() {
		callAndStopOnFailure(AddInvalidIssValueToIdToken.class, "OIDCC-12.2");
	}
}
