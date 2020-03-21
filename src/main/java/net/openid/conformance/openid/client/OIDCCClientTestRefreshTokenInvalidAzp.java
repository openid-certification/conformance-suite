package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.AddInvalidAudValueToIdToken;
import net.openid.conformance.condition.as.AddInvalidAzpValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;


/**
 * 12.2.  Successful Refresh Response
 *     - its azp Claim Value MUST be the same as in the ID Token issued when the original authentication occurred;
 *     if no azp Claim was present in the original ID Token, one MUST NOT be present in the new ID Token,
 *
 *  By default id_tokens issued by the suite does not contain azp so we add one in this test and expect the client to
 *  detect the unexpected azp claim
 */
@PublishTestModule(
	testName = "oidcc-client-test-refresh-token-invalid-azp",
	displayName = "OIDCC: Relying party refresh token test, invalid azp in id_token returned in refresh response",
	summary = "The client is expected to detect that the id_token returned in refresh response contains an azp claim " +
		"although the original id_token did not contain an azp claim." +
		" This is a new test with no corresponding test in the old conformance suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token"})
public class OIDCCClientTestRefreshTokenInvalidAzp extends AbstractOIDCCClientTestExpectingNothingAfterRefreshResponse {
	@Override
	protected String getHandleUserinfoEndpointRequestErrorMessage() {
		return "The client is not expected to send a userinfo request for this test. " +
			"The client is expected to send a token request to " +
			"obtain a refresh token and then use that refresh token to send a refresh request and " +
			"detect that the id_token in the refresh response contains an invalid azp value.";
	}

	@Override
	protected void addCustomValuesToIdTokenForRefreshResponse() {
		callAndStopOnFailure(AddInvalidAzpValueToIdToken.class, "OIDCC-12.2");
	}
}
