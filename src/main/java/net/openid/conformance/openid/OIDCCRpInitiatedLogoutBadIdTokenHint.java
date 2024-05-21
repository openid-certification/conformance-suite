package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.ExpectInvalidIdTokenHintErrorPage;
import net.openid.conformance.condition.client.GenerateFakeIdTokenClaims;
import net.openid.conformance.condition.client.GenerateJWKsFromClientSecret;
import net.openid.conformance.condition.client.SignFakeIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_RpInitLogout_Wrong_id_token_hint
// https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-RpInitLogout-Wrong-id_token_hint.json
@PublishTestModule(
	testName = "oidcc-rp-initiated-logout-bad-id-token-hint",
	displayName = "OIDCC: rp initiated logout - bad id_token_hint",
	summary = "This test performs a normal authorization flow at the OP, then sends the user to the end_session_endpoint with an id_token_hint signed by the test suite.\n\nThe OP must not redirect back and must either show an error screen or confirm with the user if they want to logout - a screenshot of which should be uploaded.",
	profile = "OIDCC"
)
public class OIDCCRpInitiatedLogoutBadIdTokenHint extends AbstractOIDCCRpInitiatedLogout {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		if (env.getObject("client_jwks") == null) {
			// we need a client jwks for SignFakeIdToken
			callAndStopOnFailure(GenerateJWKsFromClientSecret.class);
		}
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("post_logout_redirect")) {
			throw new TestFailureException(getId(), "OP has incorrectly called the registered post_logout_redirect_uri even though an invalid id_token_hint was provided.");
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		callAndStopOnFailure(GenerateFakeIdTokenClaims.class);
		callAndStopOnFailure(SignFakeIdToken.class);
		super.onPostAuthorizationFlowComplete();
	}

	@Override
	protected String createLogoutPlaceholder() {
		callAndStopOnFailure(ExpectInvalidIdTokenHintErrorPage.class, "OIDCRIL-2");

		return env.getString("invalid_id_token_hint_error");
	}

}
