package net.openid.conformance.openid.client.logout;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.as.logout.CreateRPFrontChannelLogoutRequestUrl;
import net.openid.conformance.condition.as.logout.EnsureClientHasFrontChannelLogoutUri;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;

@PublishTestModule(
	testName = "oidcc-client-test-rp-frontchannel-opinitlogout",
	displayName = "OIDCC: Relying party test, OP initiated front channel logout.",
	summary = "The client is expected to make an authorization request " +
		"(also a token request and a optionally a userinfo request when applicable)," +
		" then the OP terminates the session by calling the frontchannel_logout_uri (OP-Initiated Logout)," +
		" then Handle Post Logout URI Redirect." +
		" Corresponds to rp-frontchannel-rpinitlogout in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestFrontChannelLogoutOPInitiated extends AbstractOIDCCClientLogoutTest
{

	protected boolean receivedOPLogoutCompletedCallback;

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if( receivedAuthorizationRequest && receivedOPLogoutCompletedCallback) {
			fireTestFinished();
			return true;
		}
		return false;
	}

	@Override
	protected Object handleClientRequestForPath(String requestId, String path, HttpServletResponse servletResponse) {
		if("op_initiated_frontchannel_logout_handler".equals(path)) {
			return handleOPInitiatedFrontChannelLogoutHandler(requestId, path, servletResponse);
		} else if ("op_init_fc_logout_callback".equals(path)) {
			return handleOPInitiatedFrontChannelLogoutCallbackHandler(requestId, path, servletResponse);
		}
		return super.handleClientRequestForPath(requestId, path, servletResponse);
	}

	//TODO since it's a cross domain request we can't know if the page actually loaded or not. onload event gets triggered
	// when loading the url actually fails due to for example a x-frame-options restriction. We can end the test
	// after handleOPInitiatedFrontChannelLogoutHandler is called.
	// That's why fireTestReviewNeeded is called
	protected Object handleOPInitiatedFrontChannelLogoutCallbackHandler(String requestId, String path, HttpServletResponse servletResponse) {
		call(exec().startBlock("OP initiated logout ajax callback handler requested"));
		call(exec().endBlock());
		receivedOPLogoutCompletedCallback = true;
		fireTestReviewNeeded();
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		return new ResponseEntity<Object>(response, HttpStatus.OK);
	}

	protected Object handleOPInitiatedFrontChannelLogoutHandler(String requestId, String path, HttpServletResponse servletResponse) {
		call(exec().startBlock("OP initiated logout handler (page with the iframe) requested"));
		call(exec().endBlock());
		return new ModelAndView("opInitiatedFrontChannelLogout",
			ImmutableMap.of(
				"rp_frontchannel_logout_uri", StringEscapeUtils.escapeEcmaScript(env.getString("rp_frontchannel_logout_uri_request_url")),
				"iframe_loaded_callback_url", env.getString("base_url") + "/op_init_fc_logout_callback"
			));
	}

	protected boolean isAuthorizationCodeRequestUnexpected() {
		return responseType.includesIdToken();
	}

	@Override
	protected Object handleAuthorizationEndpointRequest(String requestId) {
		if(isAuthorizationCodeRequestUnexpected()) {
			waitAndSendLogoutRequest();
		}
		return super.handleAuthorizationEndpointRequest(requestId);
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {
		if(isAuthorizationCodeRequestUnexpected()) {
			throw new TestFailureException(getId(), "Token request is unexpected for this test");
		} else {
			waitAndSendLogoutRequest();
		}
		return super.authorizationCodeGrantType(requestId);
	}

	protected void sendFrontChannelLogoutRequest() {
		call(exec().startBlock("Create RP frontchannel_logout_uri request"));
		//TODO add references
		callAndContinueOnFailure(EnsureClientHasFrontChannelLogoutUri.class);
		callAndContinueOnFailure(CreateRPFrontChannelLogoutRequestUrl.class);
		performRedirect();
		call(exec().endBlock());
	}

	protected void performRedirect() {
		String redirectTo = env.getString("base_url") + "/op_initiated_frontchannel_logout_handler";

		eventLog.log(getName(), args("msg", "Redirecting to RP front channel logout handler",
			"redirect_to", redirectTo,
			"http", "redirect"));

		setStatus(Status.WAITING);
		browser.goToUrl(redirectTo);
	}

	protected void waitAndSendLogoutRequest() {
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(2 * 1000);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				sendFrontChannelLogoutRequest();
			}
			return "done";
		});
	}
}
