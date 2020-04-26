package net.openid.conformance.openid.client.logout;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfigurationWithSessionManagement;
import net.openid.conformance.condition.as.logout.AddSessionStateToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.logout.AddSidToIdTokenClaims;
import net.openid.conformance.condition.as.logout.CreatePostLogoutRedirectUriParams;
import net.openid.conformance.condition.as.logout.CreatePostLogoutRedirectUri;
import net.openid.conformance.condition.as.logout.GenerateSessionState;
import net.openid.conformance.condition.as.logout.LogCheckSessionIframeRequest;
import net.openid.conformance.condition.as.logout.LogGetSessionStateRequest;
import net.openid.conformance.condition.as.logout.RemoveSessionStateAndLogout;
import net.openid.conformance.condition.as.logout.ValidateIdTokenHintInRPInitiatedLogoutRequest;
import net.openid.conformance.condition.as.logout.ValidatePostLogoutRedirectUri;
import net.openid.conformance.openid.client.AbstractOIDCCClientTest;
import net.openid.conformance.testmodule.TestFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class AbstractOIDCCClientLogoutTest extends AbstractOIDCCClientTest {

	protected boolean receivedCheckSessionRequestBeforeLogout = false;
	protected boolean receivedEndSessionRequest = false;
	protected boolean receivedCheckSessionRequestAfterLogout = false;

	@Override
	protected void configureServerConfiguration() {
		callAndStopOnFailure(OIDCCGenerateServerConfigurationWithSessionManagement.class,
			"OIDCBCL-2.1", "OIDCSM-2.1", "OIDCFCL-3");
	}

	@Override
	protected void addCustomValuesToIdToken() {
		super.addCustomValuesToIdToken();
		callAndStopOnFailure(AddSidToIdTokenClaims.class, "OIDCFCL-3");
	}

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if(receivedAuthorizationRequest && receivedEndSessionRequest) {
			fireTestFinished();
			return true;
		}
		return false;
	}

	@Override
	protected Object handleClientRequestForPath(String requestId, String path, HttpServletResponse servletResponse){
		if("check_session_iframe".equals(path)) {
			return handleCheckSessionIFrameRequest(requestId, servletResponse);
		} else if ("end_session_endpoint".equals(path)) {
			return handleEndSessionEndpointRequest(requestId);
		} else if ("get_session_state".equals(path)) {
			return handleGetSessionStateViaAjaxRequest(requestId);
		} else {
			return super.handleClientRequestForPath(requestId, path, servletResponse);
		}
	}

	/**
	 * session_state
	 * Session State. JSON [RFC7159] string that represents the End-User's login state at the OP.
	 * It MUST NOT contain the space (" ") character. This value is opaque to the RP.
	 * This is REQUIRED if session management is supported.
	 */
	protected void customizeAuthorizationEndpointResponseParams(){
		callAndStopOnFailure(GenerateSessionState.class, "OIDCSM-3");
		callAndStopOnFailure(AddSessionStateToAuthorizationEndpointResponseParams.class, "OIDCSM-3");
	}

	/**
	 * Sets a cookie named op_browser_state which must be reset at logout
	 * @param requestId
	 * @param servletResponse
	 * @return
	 */
	protected Object handleCheckSessionIFrameRequest(String requestId, HttpServletResponse servletResponse){
		call(exec().startBlock("check_session_iframe requested"));
		//TODO we set a cookie here but don't actually use it
		Cookie cookie = new Cookie("op_browser_state", env.getString("op_browser_state"));
		cookie.setHttpOnly(false);
		servletResponse.addCookie(cookie);

		callAndStopOnFailure(LogCheckSessionIframeRequest.class);

		call(exec().endBlock());
		return new ModelAndView("checkSessionIFrame",
			ImmutableMap.of(
				"check_session_ajax_url", env.getString("base_url") + "/get_session_state"
			));
	}

	/**
	 * Based on my analysis of logout tests in the python suite
	 * there are three options, based on tests and client configurations:
	 * - End the session and redirect to post_logout_redirect_uri
	 * - End the session, also send a back channel logout request and then redirect to post_logout_redirect_uri
	 *   (AbstractOIDCCClientBackChannelLogoutTest does this)
	 * - Return a redirect to a page that renders the front channel logout
	 * @param requestId
	 * @return
	 */
	protected Object handleEndSessionEndpointRequest(String requestId){
		receivedEndSessionRequest = true;
		call(exec().startBlock("End session endpoint").mapKey("end_session_endpoint_http_request", requestId));

		String httpMethod = env.getString("end_session_endpoint_http_request", "method");
		JsonObject httpRequestObj = env.getObject("end_session_endpoint_http_request");

		//the spec does not restrict it to GET or POST only
		if("POST".equals(httpMethod)) {
			env.putObject("end_session_endpoint_http_request_params", httpRequestObj.getAsJsonObject("body_form_params"));
		} else if("GET".equals(httpMethod)) {
			env.putObject("end_session_endpoint_http_request_params", httpRequestObj.getAsJsonObject("query_string_params"));
		} else {
			//this should not happen?
			throw new TestFailureException(getId(), "Got unexpected HTTP method to end session endpoint");
		}

		validateEndSessionEndpointParameters();

		createPostLogoutUriRedirect();

		Object viewToReturn = createEndSessionEndpointResponse();

		removeSessionState();

		return viewToReturn;
	}

	protected void removeSessionState() {
		callAndStopOnFailure(RemoveSessionStateAndLogout.class);
	}

	protected void createPostLogoutUriRedirect() {
		callAndStopOnFailure(CreatePostLogoutRedirectUriParams.class, "OIDCSM-5.1");

		customizeEndSessionEndpointResponseParameters();

		callAndStopOnFailure(CreatePostLogoutRedirectUri.class, "OIDCSM-5.1");
	}

	/**
	 * Returns a redirect to post_logout_redirect_uri
	 * @return
	 */
	protected Object createEndSessionEndpointResponse() {

		String redirectTo = env.getString("post_logout_redirect_uri_redirect");

		return new RedirectView(redirectTo, false, false, false);
	}
	/**
	 * Called before the end session endpoint response redirect url is created.
	 * No-op by default, override in child classes to change behavior.
	 */
	protected void customizeEndSessionEndpointResponseParameters(){

	}

	/**
	 * If you override, you will probably want to call super.validateEndSessionEndpointParameters()
	 */
	protected void validateEndSessionEndpointParameters() {
		callAndContinueOnFailure(ValidateIdTokenHintInRPInitiatedLogoutRequest.class, Condition.ConditionResult.FAILURE, "OIDCSM-5");
		callAndContinueOnFailure(ValidatePostLogoutRedirectUri.class, Condition.ConditionResult.FAILURE, "OIDCSM-5.1");
	}
	/**
	 * Called from the check_session_iframe receiveMessage method via ajax
	 * Used to verify that the check_session_iframe received messages via postMessage
	 * Returns session_state_data from env
	 * @param requestId
	 * @return
	 */
	protected Object handleGetSessionStateViaAjaxRequest(String requestId){
		call(exec().startBlock("Get session state - postMessage callback").mapKey("incoming_request", requestId));

		if(receivedEndSessionRequest) {
			receivedCheckSessionRequestAfterLogout = true;
		} else{
			receivedCheckSessionRequestBeforeLogout = true;
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JSON_UTF8);
		JsonObject body = env.getObject("session_state_data");

		callAndStopOnFailure(LogGetSessionStateRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());
		if(body!=null) {
			return new ResponseEntity<Object>(body, headers, HttpStatus.OK);
		} else {
			return new ResponseEntity<Object>(new JsonObject(), headers, HttpStatus.OK);
		}
	}
}
