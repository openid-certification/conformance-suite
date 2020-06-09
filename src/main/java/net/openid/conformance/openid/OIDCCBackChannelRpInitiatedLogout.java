package net.openid.conformance.openid;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddBackchannelLogoutSessionRequiredTrueToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddBackchannelLogoutUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddPostLogoutRedirectUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddPromptNoneToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildRedirectToEndSessionEndpoint;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointIsOneThatRequiredAUserInterface;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInBackchannelLogoutRequest;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInPostLogoutRedirect;
import net.openid.conformance.condition.client.CheckIdTokenSidMatchesLogoutToken;
import net.openid.conformance.condition.client.CheckIdTokenSubMatchesLogoutToken;
import net.openid.conformance.condition.client.CheckLogoutTokenHasSubOrSid;
import net.openid.conformance.condition.client.CheckLogoutTokenNoNonce;
import net.openid.conformance.condition.client.CheckPostLogoutState;
import net.openid.conformance.condition.client.CreateBackchannelLogoutUri;
import net.openid.conformance.condition.client.CreateEndSessionEndpointRequest;
import net.openid.conformance.condition.client.CreatePostLogoutRedirectUri;
import net.openid.conformance.condition.client.CreateRandomEndSessionState;
import net.openid.conformance.condition.client.ExtractLogoutTokenFromBackchannelLogoutRequest;
import net.openid.conformance.condition.client.ExtractSessionStateFromAuthorizationResponse;
import net.openid.conformance.condition.client.ValidateLogoutTokenClaims;
import net.openid.conformance.condition.client.ValidateLogoutTokenSignature;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_BackChannel_RpInitLogout
// https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-BackChannel-RpInitLogout.json
@PublishTestModule(
	testName = "oidcc-backchannel-rp-initiated-logout",
	displayName = "OIDCC: Backchannel rp initiated logout",
	summary = "This test performs a normal authorization flow at the OP, then sends the user to the end_session_endpoint. It validates the OP correctly calls the backchannel_logout_uri and sends the user to the post_logout_redirect_uri, then tries another authentication with prompt=none which must return an error (as the user has been logged out). If using static client registration you must register backchannel_logout_uri to the same url as the redirect url, but replacing the portion after the alias with /backchannel_logout and similarly register /post_logout_redirect as a post_logout_redirect_uri.",
	profile = "OIDCC"
)
public class OIDCCBackChannelRpInitiatedLogout extends AbstractOIDCCServerTest {
	private boolean firstTime = true;
	private JsonObject postLogoutRedirectRequestParts = null;
	private JsonObject backchannelLogoutRequestParts = null;

	@Override
	protected String currentClientString() {
		return firstTime ? "" : "Second authorization: ";
	}

	@Override
	protected void configureClient() {
		callAndStopOnFailure(CreateBackchannelLogoutUri.class, "OIDCBCL-2.2");
		callAndStopOnFailure(CreatePostLogoutRedirectUri.class, "OIDCSM-5", "OIDCSM-5.1.1");
		super.configureClient();
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddBackchannelLogoutSessionRequiredTrueToDynamicRegistrationRequest.class, "OIDCBCL-2.2");
		callAndStopOnFailure(AddBackchannelLogoutUriToDynamicRegistrationRequest.class, "OIDCBCL-2.2");
		callAndStopOnFailure(AddPostLogoutRedirectUriToDynamicRegistrationRequest.class, "OIDCSM-5.1.1");
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		// use a longer state value to check OP doesn't corrupt it
		env.putInteger("requested_state_length", 128);
	}

	@Override
	protected void createAuthorizationRequest() {
		// python includes the offline_access scope in all authorization requests; I checked with Roland (see 9th June
		// 2020 email) and there was no reason he could remember for doing this and he suspected it was likely a C&P
		// error, so java does not include offline_access.

		if (firstTime) {
			super.createAuthorizationRequest();
		} else {
			// with prompt=none this time
			call(new CreateAuthorizationRequestSteps(formPost)
				.then(condition(AddPromptNoneToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1", "OIDCC-15.1")));
		}
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		if (firstTime) {
			firstTime = false;
			super.onAuthorizationCallbackResponse();
		} else {
			performGenericAuthorizationEndpointErrorResponseValidation();

			callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointIsOneThatRequiredAUserInterface.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

			fireTestFinished();
		}
	}

	protected void onPostAuthorizationFlowComplete() {
		callAndStopOnFailure(ExtractSessionStateFromAuthorizationResponse.class, "OIDCSM-3");

		eventLog.startBlock("Redirect to end session endpoint & wait for front channel and backchannel responses");
		callAndStopOnFailure(CreateRandomEndSessionState.class, "OIDCSM-5", "RFC6749A-A.5");
		callAndStopOnFailure(CreateEndSessionEndpointRequest.class, "OIDCSM-5");
		callAndStopOnFailure(BuildRedirectToEndSessionEndpoint.class, "OIDCSM-5");
		performRedirectToEndSessionEndpoint();
	}

	protected void performRedirectToEndSessionEndpoint() {
		String redirectTo = env.getString("redirect_to_end_session_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to end session endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo);
	}

	protected void validateLogoutResults(){
		String bcLogoutEnvKey = "backchannel_logout_request";

		env.putObject("post_logout_redirect", postLogoutRedirectRequestParts);
		env.putObject(bcLogoutEnvKey, backchannelLogoutRequestParts);

		eventLog.startBlock("Verify backchannel logout request");

		env.mapKey("client_request", bcLogoutEnvKey);
		// This is a mixture of must & recommended in BCP195, but BCP195 is not a normative reference of OIDCC so only raise a warning
		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, Condition.ConditionResult.WARNING, "BCP195-3.1.1");

		callAndStopOnFailure(ExtractLogoutTokenFromBackchannelLogoutRequest.class, "OIDCBCL-2.5");
		callAndContinueOnFailure(CheckForUnexpectedParametersInBackchannelLogoutRequest.class, "OIDCBCL-2.5");
		callAndContinueOnFailure(ValidateLogoutTokenSignature.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
		callAndContinueOnFailure(ValidateLogoutTokenClaims.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
		skipIfElementMissing("logout_token", "claims.sub",
			Condition.ConditionResult.INFO, CheckIdTokenSubMatchesLogoutToken.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
		skipIfElementMissing("logout_token", "claims.sid",
			Condition.ConditionResult.INFO, CheckIdTokenSidMatchesLogoutToken.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
		callAndContinueOnFailure(CheckLogoutTokenNoNonce.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");
		callAndContinueOnFailure(CheckLogoutTokenHasSubOrSid.class, Condition.ConditionResult.FAILURE, "OIDCBCL-2.4");

		env.unmapKey("client_request");

		eventLog.endBlock();

		eventLog.startBlock("Verify frontchannel post logout redirect");
		callAndContinueOnFailure(CheckPostLogoutState.class, Condition.ConditionResult.FAILURE, "OIDCSM-5");
		callAndContinueOnFailure(CheckForUnexpectedParametersInPostLogoutRedirect.class, "OIDCSM-5");

		eventLog.endBlock();

		// do the prompt=none authorization request to check logout happened
		performAuthorizationFlow();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("backchannel_logout")) {
			return handleBackchannelLogout(requestParts);
		} else if (path.equals("post_logout_redirect")) {
			return handlePostLogoutRedirect(requestParts);
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	protected Object handlePostLogoutRedirect(JsonObject requestParts) {
		setStatus(Status.RUNNING);
		postLogoutRedirectRequestParts = requestParts;
		if (backchannelLogoutRequestParts != null) {
			validateLogoutResultsInBackground();
		}
		setStatus(Status.WAITING);
		return new ModelAndView("postLogout",
			ImmutableMap.of(
				"returnUrl", "/log-detail.html?log=" + getId()
			));
	}

	protected Object handleBackchannelLogout(JsonObject requestParts) {
		setStatus(Status.RUNNING);
		backchannelLogoutRequestParts = requestParts;
		if (postLogoutRedirectRequestParts != null) {
			validateLogoutResultsInBackground();
		}
		setStatus(Status.WAITING);

		// as per https://openid.net/specs/openid-connect-backchannel-1_0.html#BCResponse
		// we always return a successful response as this test expects a valid request
		HttpHeaders headers = new HttpHeaders();
		headers.set("Cache-Control", "no-cache, no-store");
		headers.set("Pragma", "no-cache");
		return new ResponseEntity<Object>("", headers, HttpStatus.OK);
	}

	private void validateLogoutResultsInBackground() {
		getTestExecutionManager().runInBackground(() -> {
			setStatus(Status.RUNNING);
			validateLogoutResults();
			return "done";
		});
	}

	@Override
	public void cleanup() {
		firstTime = true; // to avoid any blocks created in cleanup being prefixed in currentClientString()
		super.cleanup();
	}

}
