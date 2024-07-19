package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.ExpectSuccessfulLogoutPage;
import net.openid.conformance.condition.client.RemoveIdTokenHintFromEndSessionEndpointRequest;
import net.openid.conformance.condition.client.RemovePostLogoutRedirectUriFromEndSessionEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_RpInitLogout_Only_state
@PublishTestModule(
	testName = "oidcc-rp-initiated-logout-only-state",
	displayName = "OIDCC: rp initiated logout - only state",
	summary = "This test performs a normal authorization flow at the OP, then sends the user to the end_session_endpoint with only a state parameter - the OP must log the user out, a screenshot of the 'you are logged out' screen should be uploaded.",
	profile = "OIDCC"
)
public class OIDCCRpInitiatedLogoutOnlyState extends AbstractOIDCCRpInitiatedLogout {

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("post_logout_redirect")) {
			throw new TestFailureException(getId(), "OP has incorrectly called the registered post_logout_redirect_uri when it wasn't in the request.");
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	@Override
	protected void customiseEndSessionEndpointRequest() {
		callAndStopOnFailure(RemoveIdTokenHintFromEndSessionEndpointRequest.class);
		callAndStopOnFailure(RemovePostLogoutRedirectUriFromEndSessionEndpointRequest.class);
	}

	@Override
	protected String createLogoutPlaceholder() {
		callAndStopOnFailure(ExpectSuccessfulLogoutPage.class, "OIDCRIL-2");

		return env.getString("successful_logout_page");
	}

}
