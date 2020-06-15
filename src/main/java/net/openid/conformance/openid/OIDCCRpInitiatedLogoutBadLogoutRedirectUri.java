package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddBadPostLogoutRedirectUriToEndSessionEndpointRequest;
import net.openid.conformance.condition.client.AddPostLogoutRedirectUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.BuildRedirectToEndSessionEndpoint;
import net.openid.conformance.condition.client.CreateEndSessionEndpointRequest;
import net.openid.conformance.condition.client.CreatePostLogoutRedirectUri;
import net.openid.conformance.condition.client.CreateRandomEndSessionState;
import net.openid.conformance.condition.client.ExpectPostLogoutRedirectUriNotRegisteredErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_RpInitLogout_Bad_redirect_uri
// https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-RpInitLogout-Bad_redirect_uri.json
@PublishTestModule(
	testName = "oidcc-rp-initiated-logout-bad-post-logout-redirect-uri",
	displayName = "OIDCC: rp initiated logout - bad post_logout_redirect_uri",
	summary = "This test performs a normal authorization flow at the OP, then sends the user to the end_session_endpoint with a post_logout_redirect_uri that is not registered at the OP - the OP must show an error screen, a screenshot of which should be uploaded.",
	profile = "OIDCC"
)
public class OIDCCRpInitiatedLogoutBadLogoutRedirectUri extends AbstractOIDCCRpInitiatedLogout {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(CreatePostLogoutRedirectUri.class, "OIDCSM-5", "OIDCSM-5.1.1");
		super.configureClient();
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddPostLogoutRedirectUriToDynamicRegistrationRequest.class, "OIDCSM-5.1.1");
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("post_logout_redirect")) {
			throw new TestFailureException(getId(), "OP has incorrectly called the registered post_logout_redirect_uri even though a different uri was requested.");
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	protected void onPostAuthorizationFlowComplete() {
		eventLog.startBlock("Redirect to end session endpoint & wait for response");
		callAndStopOnFailure(CreateRandomEndSessionState.class, "OIDCSM-5", "RFC6749A-A.5");
		callAndStopOnFailure(CreateEndSessionEndpointRequest.class, "OIDCSM-5");
		callAndStopOnFailure(AddBadPostLogoutRedirectUriToEndSessionEndpointRequest.class);
		callAndStopOnFailure(BuildRedirectToEndSessionEndpoint.class, "OIDCSM-5");
		performRedirectToEndSessionEndpoint(true);
	}

	protected void performRedirectToEndSessionEndpoint(boolean expectError) {
		String placeholderId = null;
		String redirectTo = env.getString("redirect_to_end_session_endpoint");

		if (expectError) {
			placeholderId = createLogoutPlaceholder();
			waitForPlaceholders();
		}

		eventLog.log(getName(), args("msg", "Redirecting to end session endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo, placeholderId);
	}

	protected String createLogoutPlaceholder() {
		callAndStopOnFailure(ExpectPostLogoutRedirectUriNotRegisteredErrorPage.class, "OIDCSM-5");

		return env.getString("post_logout_redirect_uri_not_registered_error");
	}

}
