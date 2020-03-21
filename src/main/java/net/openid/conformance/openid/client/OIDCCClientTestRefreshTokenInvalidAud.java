package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.AddInvalidAudValueToIdToken;
import net.openid.conformance.condition.as.AddInvalidSubValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;


/**
 * 12.2.  Successful Refresh Response
 *     - its aud Claim Value MUST be the same as in the ID Token issued when the original authentication occurred,
 */
@PublishTestModule(
	testName = "oidcc-client-test-refresh-token-invalid-aud",
	displayName = "OIDCC: Relying party refresh token test, invalid aud in id_token returned in refresh response",
	summary = "The client is expected to detect that the id_token returned in refresh response contains an aud value that is" +
		"not the same as the aud value in the original id_token." +
		" This is a new test with no corresponding test in the old conformance suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token"})
public class OIDCCClientTestRefreshTokenInvalidAud extends AbstractOIDCCClientTestExpectingNothingAfterRefreshResponse {
	@Override
	protected String getHandleUserinfoEndpointRequestErrorMessage() {
		return "The client is not expected to send a userinfo request for this test. " +
			"The client is expected to send a token request to " +
			"obtain a refresh token and then use that refresh token to send a refresh request and " +
			"detect that the id_token in the refresh response contains an invalid aud value.";
	}

	@Override
	protected void addCustomValuesToIdTokenForRefreshResponse() {
		callAndStopOnFailure(AddInvalidAudValueToIdToken.class, "OIDCC-12.2");
	}
}
