package net.openid.conformance.openid.client.logout;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfigurationWithSessionManagement;
import net.openid.conformance.condition.as.logout.AddSessionStateToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.logout.AddSidToIdTokenClaims;
import net.openid.conformance.condition.as.logout.CallRPBackChannelLogoutEndpoint;
import net.openid.conformance.condition.as.logout.CreatePostLogoutRedirectUriParams;
import net.openid.conformance.condition.as.logout.CreatePostLogoutRedirectUriRedirect;
import net.openid.conformance.condition.as.logout.CreateRPFrontChannelLogoutRequestUrl;
import net.openid.conformance.condition.as.logout.EncryptLogoutToken;
import net.openid.conformance.condition.as.logout.EnsureBackChannelLogoutEndpointResponseContainsCacheHeaders;
import net.openid.conformance.condition.as.logout.EnsureClientHasBackChannelLogoutUri;
import net.openid.conformance.condition.as.logout.EnsureClientHasFrontChannelLogoutUri;
import net.openid.conformance.condition.as.logout.GenerateLogoutTokenClaims;
import net.openid.conformance.condition.as.logout.GenerateSessionState;
import net.openid.conformance.condition.as.logout.LogCheckSessionIframeRequest;
import net.openid.conformance.condition.as.logout.LogGetSessionStateRequest;
import net.openid.conformance.condition.as.logout.LogoutByRemovingSessionState;
import net.openid.conformance.condition.as.logout.OIDCCSignLogoutToken;
import net.openid.conformance.condition.as.logout.ValidateIdTokenHintInRPInitiatedLogoutRequest;
import net.openid.conformance.condition.as.logout.ValidatePostLogoutRedirectUri;
import net.openid.conformance.openid.client.AbstractOIDCCClientTest;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantConfigurationFields;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletResponse;

@VariantConfigurationFields(parameter = ClientRegistration.class, value = "static_client", configurationFields = {
	"client.post_logout_redirect_uri"
})

public class AbstractOIDCCClientLogoutTest extends AbstractOIDCCClientTest {

	protected boolean receivedCheckSessionRequestBeforeLogout = false;
	protected boolean receivedEndSessionRequest = false;
	protected boolean receivedCheckSessionRequestAfterLogout = false;
	protected boolean sentBackChannelLogoutRequest = false;
	protected boolean receivedFrontChannelLogoutCompletedCallback;

	@Override
	protected void configureServerConfiguration() {
		callAndStopOnFailure(OIDCCGenerateServerConfigurationWithSessionManagement.class,
			"OIDCBCL-2.1", "OIDCSM-3.3", "OIDCFCL-3", "OIDCRIL-2.1");
		expose("end_session_endpoint", env.getString("base_url") + "/end_session_endpoint");
	}

	@Override
	protected void validateAuthorizationEndpointRequestParameters() {
		super.validateAuthorizationEndpointRequestParameters();
		callAndStopOnFailure(GenerateSessionState.class, "OIDCSM-3");
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
		} else if("frontchannel_logout_handler".equals(path)) {
			return createFrontChannelLogoutModelAndView(true);
		} else if ("frontchannel_logout_callback".equals(path)) {
			return handleFrontChannelLogoutCallbackHandler(requestId, path, servletResponse);
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
	@Override
	protected void customizeAuthorizationEndpointResponseParams(){
		callAndStopOnFailure(AddSessionStateToAuthorizationEndpointResponseParams.class, "OIDCSM-3");
	}

	/**
	 * The check session iframe does not calculate session_state itself, it just sends an ajax request
	 * to check_session_ajax_url which is used to track test progress state
	 *
	 * @param requestId
	 * @param servletResponse
	 * @return
	 */
	protected Object handleCheckSessionIFrameRequest(String requestId, HttpServletResponse servletResponse){
		call(exec().startBlock("check_session_iframe requested"));

		callAndStopOnFailure(LogCheckSessionIframeRequest.class);

		call(exec().endBlock());
		return new ModelAndView("checkSessionIFrame",
			ImmutableMap.of(
				"check_session_ajax_url", env.getString("base_url") + "/get_session_state"
			));
	}

	protected Object handleEndSessionEndpointRequest(String requestId){
		receivedEndSessionRequest = true;
		call(exec().startBlock("End session endpoint").mapKey("end_session_endpoint_http_request", requestId));

		setEndSessionEndpointRequestParamsSource();

		validateEndSessionEndpointParameters();

		createPostLogoutUriRedirect();

		Object viewToReturn = createEndSessionEndpointResponse();

		removeSessionState();

		return viewToReturn;
	}

	protected void setEndSessionEndpointRequestParamsSource() {
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
	}

	protected void removeSessionState() {
		callAndStopOnFailure(LogoutByRemovingSessionState.class);
	}

	protected void createPostLogoutUriRedirect() {
		callAndStopOnFailure(CreatePostLogoutRedirectUriParams.class, "OIDCRIL-3");

		customizeEndSessionEndpointResponseParameters();

		callAndStopOnFailure(CreatePostLogoutRedirectUriRedirect.class, "OIDCRIL-3");
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
		callAndContinueOnFailure(ValidateIdTokenHintInRPInitiatedLogoutRequest.class, Condition.ConditionResult.FAILURE, "OIDCRIL-2");
		callAndContinueOnFailure(ValidatePostLogoutRedirectUri.class, Condition.ConditionResult.FAILURE, "OIDCRIL-3.1");
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
		headers.setContentType(MediaType.APPLICATION_JSON);
		JsonObject body = env.getObject("session_state_data");

		callAndStopOnFailure(LogGetSessionStateRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());
		if(body!=null) {
			return new ResponseEntity<Object>(body, headers, HttpStatus.OK);
		} else {
			return new ResponseEntity<Object>(new JsonObject(), headers, HttpStatus.OK);
		}
	}

	protected void createLogoutToken() {
		call(exec().startBlock("Create Logout Token"));
		generateLogoutTokenClaims();
		customizeLogoutTokenClaims();

		signLogoutToken();

		customizeLogoutTokenSignature();

		encryptLogoutTokenIfNecessary();
		call(exec().endBlock());
	}

	protected void generateLogoutTokenClaims() {
		callAndContinueOnFailure(GenerateLogoutTokenClaims.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
	}

	protected void sendBackChannelLogoutRequest() {
		call(exec().startBlock("Send Back Channel Logout Request"));
		callAndContinueOnFailure(EnsureClientHasBackChannelLogoutUri.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.2");
		callAndContinueOnFailure(CallRPBackChannelLogoutEndpoint.class, Condition.ConditionResult.FAILURE,"OIDCBCL-2.5");
		validateBackChannelLogoutResponse();
		call(exec().endBlock());
		sentBackChannelLogoutRequest=true;
	}

	protected void validateBackChannelLogoutResponse() {
		callAndContinueOnFailure(EnsureBackChannelLogoutEndpointResponseContainsCacheHeaders.class,
			Condition.ConditionResult.WARNING, "OIDCBCL-2.8");
	}


	/**
	 * Override to modify logout token claims
	 * Called right after generateLogoutTokenClaims
	 */
	protected void customizeLogoutTokenClaims(){

	}

	protected void customizeLogoutTokenSignature(){

	}


	protected void signLogoutToken() {
		callAndContinueOnFailure(OIDCCSignLogoutToken.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
	}

	protected void encryptLogoutTokenIfNecessary() {
		skipIfElementMissing("client", "id_token_encrypted_response_alg", Condition.ConditionResult.INFO,
			EncryptLogoutToken.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4", "OIDCC-10.2");
	}

	//TODO since it's a cross domain request we can't know if the page actually loaded or not.
	// iframe onload event gets triggered even when loading the url actually fails,
	// due to for example an x-frame-options restriction.
	// That's why fireTestReviewNeeded is called
	protected Object handleFrontChannelLogoutCallbackHandler(String requestId, String path, HttpServletResponse servletResponse) {
		call(exec().startBlock("Front Channel Logout Ajax Callback Handler Request"));
		call(exec().endBlock());
		receivedFrontChannelLogoutCompletedCallback = true;
		fireTestReviewNeeded();
		JsonObject response = new JsonObject();
		response.addProperty("ok", true);
		return new ResponseEntity<Object>(response, HttpStatus.OK);
	}

	protected Object createFrontChannelLogoutModelAndView(boolean isOPinit) {
		call(exec().startBlock("Render Page With Front Channel Logout Iframe"));
		call(exec().endBlock());
		//'OPINIT' value is used in the template(templates/oidccFrontChannelLogout.html) to check if it's OP init or not
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

	protected void createFrontChannelLogoutRequestUrl() {
		call(exec().startBlock("Create Front Channel Logout Request"));
		callAndContinueOnFailure(EnsureClientHasFrontChannelLogoutUri.class, Condition.ConditionResult.FAILURE, "OIDCFCL-2");
		callAndStopOnFailure(CreateRPFrontChannelLogoutRequestUrl.class, "OIDCFCL-2");
		call(exec().endBlock());
	}

}
