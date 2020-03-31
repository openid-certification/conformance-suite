package net.openid.conformance.openid.client.logout;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfigurationWithSessionManagement;
import net.openid.conformance.condition.as.logout.AddSessionStateToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.logout.GenerateSessionState;
import net.openid.conformance.openid.client.AbstractOIDCCClientTest;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class AbstractOIDCCClientLogoutTest extends AbstractOIDCCClientTest {

	protected boolean receivedCheckSessionRequest = false;
	protected boolean receivedEndSessionRequest = false;

	@Override
	protected void configureServerConfiguration() {
		callAndStopOnFailure(OIDCCGenerateServerConfigurationWithSessionManagement.class);
	}

	@Override
	protected Object handleClientRequestForPath(String requestId, String path, HttpServletResponse servletResponse){
		if("check_session_iframe".equals(path)) {
			return handleCheckSessionIFrameRequest(requestId, servletResponse);
		} else if ("end_session_endpoint".equals(path)) {
			return handleEndSessionRequest(requestId);
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
		//add session_state
		//TODO add references
		callAndStopOnFailure(GenerateSessionState.class);
		callAndStopOnFailure(AddSessionStateToAuthorizationEndpointResponseParams.class);
	}

	/**
	 * Sets a cookie named op_browser_state which must be reset at logout
	 * @param requestId
	 * @param servletResponse
	 * @return
	 */
	protected Object handleCheckSessionIFrameRequest(String requestId, HttpServletResponse servletResponse){
		Cookie cookie = new Cookie("op_browser_state", env.getString("op_browser_state"));
		cookie.setHttpOnly(false);
		servletResponse.addCookie(cookie);

		return new ModelAndView("checkSessionIFrame",
			ImmutableMap.of(
				//because client ids are VSChar
				"clientId", StringEscapeUtils.escapeEcmaScript(env.getString("client", "client_id"))
			));
	}

	protected Object handleEndSessionRequest(String requestId){
		return null;
	}

	/**
	 * Called from the check_session_iframe to get browser state and salt to be used in
	 * calculation of session_state
	 * @param requestId
	 * @return
	 */
	protected Object handleGetSessionStateViaAjaxRequest(String requestId){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JSON_UTF8);
		JsonObject body = new JsonObject();
		body.addProperty("op_browser_state", env.getString("op_browser_state"));
		body.addProperty("session_state_salt", env.getString("session_state_salt"));
		return new ResponseEntity<Object>(body, headers, HttpStatus.OK);
	}
}
