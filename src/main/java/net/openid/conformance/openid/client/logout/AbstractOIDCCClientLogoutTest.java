package net.openid.conformance.openid.client.logout;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfigurationWithSessionManagement;
import net.openid.conformance.condition.as.logout.AddSessionStateToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.logout.AddSidToIdTokenClaims;
import net.openid.conformance.condition.as.logout.CreateEndSessionEndpointResponseParams;
import net.openid.conformance.condition.as.logout.CreateEndSessionResponseRedirect;
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
	 * An RP can notify the OP that the End-User has logged out of the site and might want to log out of the OP as well.
	 * In this case, the RP, after having logged the End-User out of the RP, redirects the End-User's User Agent to the
	 * OP's logout endpoint URL. This URL is normally obtained via the end_session_endpoint element of the OP's Discovery
	 * response or may be learned via other mechanisms.
	 *
	 * This specification also defines the following parameters that are passed as query parameters in the logout request:
	 *
	 * id_token_hint
	 * RECOMMENDED. Previously issued ID Token passed to the logout endpoint as a hint about the End-User's current
	 * authenticated session with the Client. This is used as an indication of the identity of the End-User that the
	 * RP is requesting be logged out by the OP. The OP need not be listed as an audience of the ID Token when it is
	 * used as an id_token_hint value.
	 * post_logout_redirect_uri
	 * OPTIONAL. URL to which the RP is requesting that the End-User's User Agent be redirected after a logout has been
	 * performed. The value MUST have been previously registered with the OP, either using the post_logout_redirect_uris
	 * Registration parameter or via another mechanism. If supplied, the OP SHOULD honor this request following the logout.
	 * state
	 * OPTIONAL. Opaque value used by the RP to maintain state between the logout request and the callback to the
	 * endpoint specified by the post_logout_redirect_uri query parameter. If included in the logout request, the OP
	 * passes this value back to the RP using the state query parameter when redirecting the User Agent back to the RP.
	 * At the logout endpoint, the OP SHOULD ask the End-User whether he wants to log out of the OP as well. If the
	 * End-User says "yes", then the OP MUST log out the End-User.
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

		callAndStopOnFailure(RemoveSessionStateAndLogout.class);

		callAndStopOnFailure(CreateEndSessionEndpointResponseParams.class, "OIDCSM-5.1");

		customizeEndSessionEndpointResponseParameters();

		callAndStopOnFailure(CreateEndSessionResponseRedirect.class, "OIDCSM-5.1");

		String redirectTo = env.getString("end_session_endpoint_response_redirect");

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
