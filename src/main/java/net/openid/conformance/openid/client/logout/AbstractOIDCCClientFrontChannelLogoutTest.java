package net.openid.conformance.openid.client.logout;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.logout.CreateRPFrontChannelLogoutRequestUrl;
import net.openid.conformance.condition.as.logout.EnsureClientHasFrontChannelLogoutUri;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;


public abstract class AbstractOIDCCClientFrontChannelLogoutTest extends AbstractOIDCCClientLogoutTest
{

	protected boolean receivedFrontChannelLogoutCompletedCallback;

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if( receivedAuthorizationRequest && receivedFrontChannelLogoutCompletedCallback) {
			fireTestFinished();
			return true;
		}
		return false;
	}

	@Override
	protected Object handleClientRequestForPath(String requestId, String path, HttpServletResponse servletResponse) {
		if("frontchannel_logout_handler".equals(path)) {
			return createFrontChannelLogoutModelAndView(true);
		} else if ("frontchannel_logout_callback".equals(path)) {
			return handleFrontChannelLogoutCallbackHandler(requestId, path, servletResponse);
		}
		return super.handleClientRequestForPath(requestId, path, servletResponse);
	}

	//TODO since it's a cross domain request we can't know if the page actually loaded or not. onload event gets triggered
	// when loading the url actually fails due to for example a x-frame-options restriction. We can end the test
	// after handleOPInitiatedFrontChannelLogoutHandler is called.
	// That's why fireTestReviewNeeded is called
	protected Object handleFrontChannelLogoutCallbackHandler(String requestId, String path, HttpServletResponse servletResponse) {
		call(exec().startBlock("Front channel logout ajax callback handler requested"));
		call(exec().endBlock());
		receivedFrontChannelLogoutCompletedCallback = true;
		fireTestReviewNeeded();
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		return new ResponseEntity<Object>(response, HttpStatus.OK);
	}

	protected Object createFrontChannelLogoutModelAndView(boolean isOPinit) {
		call(exec().startBlock("Rendering page with logout iframe"));
		call(exec().endBlock());
		String postLogoutRedir = "OPINIT";
		if(!isOPinit) {
			postLogoutRedir = env.getString("post_logout_redirect_uri_redirect");
		}
		return new ModelAndView("oidccFrontChannelLogout",
			ImmutableMap.of(
				"rp_frontchannel_logout_uri", StringEscapeUtils.escapeEcmaScript(env.getString("rp_frontchannel_logout_uri_request_url")),
				"iframe_loaded_callback_url", env.getString("base_url") + "/frontchannel_logout_callback",
				"post_logout_redirect_uri_redirect", postLogoutRedir
			));
	}

	@Override
	protected Object createEndSessionEndpointResponse() {
		createFrontChannelLogoutRequestUrl();
		return createFrontChannelLogoutModelAndView(false);
	}

	protected void createFrontChannelLogoutRequestUrl() {
		call(exec().startBlock("Create Front Channel Logout Request"));
		callAndContinueOnFailure(EnsureClientHasFrontChannelLogoutUri.class, Condition.ConditionResult.FAILURE, "OIDCFCL-2");
		callAndStopOnFailure(CreateRPFrontChannelLogoutRequestUrl.class, "OIDCFCL-2");
		call(exec().endBlock());
	}

}
