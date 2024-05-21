package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddPostLogoutRedirectUriWithQueryAddedToEndSessionEndpointRequest;
import net.openid.conformance.condition.client.ExpectPostLogoutRedirectUriNotRegisteredErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_RpInitLogout_Unregistered_post_logout_redirect_uri
@PublishTestModule(
	testName = "oidcc-rp-initiated-logout-query-added-to-post-logout-redirect-uri",
	displayName = "OIDCC: rp initiated logout - query added to post_logout_redirect_uri",
	summary = "This test performs a normal authorization flow at the OP, then sends the user to the end_session_endpoint with a post_logout_redirect_uri that has had '?foo=bar' added.\n\nThe OP must not redirect back and must either show an error screen or confirm with the user if they want to logout - a screenshot of which should be uploaded.",
	profile = "OIDCC"
)
public class OIDCCRpInitiatedLogoutQueryAddedToLogoutRedirectUri extends AbstractOIDCCRpInitiatedLogout {

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("post_logout_redirect")) {
			throw new TestFailureException(getId(), "OP has incorrectly called the registered post_logout_redirect_uri even though a different uri was requested.");
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	@Override
	protected void customiseEndSessionEndpointRequest() {
		callAndStopOnFailure(AddPostLogoutRedirectUriWithQueryAddedToEndSessionEndpointRequest.class);
	}

	@Override
	protected String createLogoutPlaceholder() {
		callAndStopOnFailure(ExpectPostLogoutRedirectUriNotRegisteredErrorPage.class, "OIDCRIL-2");

		return env.getString("post_logout_redirect_uri_not_registered_error");
	}

}
