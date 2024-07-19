package net.openid.conformance.openid;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddFrontchannelLogoutSessionRequiredTrueToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddFrontchannelLogoutUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInFrontchannelLogoutRequest;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInPostLogoutRedirect;
import net.openid.conformance.condition.client.CheckIdTokenSidMatchesFrontChannelLogoutRequest;
import net.openid.conformance.condition.client.CheckPostLogoutState;
import net.openid.conformance.condition.client.CreateFrontchannelLogoutUri;
import net.openid.conformance.condition.client.ValidateFrontchannelLogoutIss;
import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_FrontChannel_RpInitLogout
// https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-FrontChannel-RpInitLogout.json
@PublishTestModule(
	testName = "oidcc-frontchannel-rp-initiated-logout",
	displayName = "OIDCC: Frontchannel rp initiated logout",
	summary = "This test performs a normal authorization flow at the OP, then sends the user to the end_session_endpoint. It validates the OP correctly loads the frontchannel_logout_uri and sends the user to the post_logout_redirect_uri, then tries another authentication with prompt=none which must return an error (as the user has been logged out).\n\nIf using static client registration you must register frontchannel_logout_uri to the same url as the redirect url, but replacing the portion after the alias with /frontchannel_logout and similarly register /post_logout_redirect as a post_logout_redirect_uri.",
	profile = "OIDCC"
)
public class OIDCCFrontChannelRpInitiatedLogout extends AbstractOIDCCRpInitiatedLogout {
	private JsonObject postLogoutRedirectRequestParts = null;
	private JsonObject frontchannelLogoutRequestParts = null;

	@Override
	protected void configureClient() {
		callAndStopOnFailure(CreateFrontchannelLogoutUri.class, "OIDCFCL-2");
		super.configureClient();
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddFrontchannelLogoutSessionRequiredTrueToDynamicRegistrationRequest.class, "OIDCFCL-2");
		callAndStopOnFailure(AddFrontchannelLogoutUriToDynamicRegistrationRequest.class, "OIDCFCL-2");
	}

	protected void validateLogoutResults(){
		String fcLogoutEnvKey = "frontchannel_logout_request";

		env.putObject("post_logout_redirect", postLogoutRedirectRequestParts);
		env.putObject(fcLogoutEnvKey, frontchannelLogoutRequestParts);

		eventLog.startBlock("Verify frontchannel logout request");

		env.mapKey("client_request", fcLogoutEnvKey);

		callAndContinueOnFailure(CheckForUnexpectedParametersInFrontchannelLogoutRequest.class, Condition.ConditionResult.WARNING, "OIDCFCL-2");
		callAndContinueOnFailure(ValidateFrontchannelLogoutIss.class, Condition.ConditionResult.FAILURE, "OIDCFCL-2");
		callAndContinueOnFailure(CheckIdTokenSidMatchesFrontChannelLogoutRequest.class, Condition.ConditionResult.FAILURE, "OIDCFCL-2", "OIDCFCL-3");

		env.unmapKey("client_request");

		eventLog.endBlock();

		eventLog.startBlock("Verify frontchannel post logout redirect");
		callAndContinueOnFailure(CheckPostLogoutState.class, Condition.ConditionResult.FAILURE, "OIDCRIL-2");
		callAndContinueOnFailure(CheckForUnexpectedParametersInPostLogoutRedirect.class, Condition.ConditionResult.WARNING, "OIDCRIL-2");

		eventLog.endBlock();

		// do the prompt=none authorization request to check logout happened
		performAuthorizationFlow();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("frontchannel_logout")) {
			return handleFrontchannelLogout(requestParts);
		} else if (path.equals("post_logout_redirect")) {
			return handlePostLogoutRedirect(requestParts);
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	protected Object handlePostLogoutRedirect(JsonObject requestParts) {
		setStatus(Status.RUNNING);
		postLogoutRedirectRequestParts = requestParts;
		if (frontchannelLogoutRequestParts == null) {
			eventLog.log(getName(), args("msg", "Received front channel redirect; waiting for front channel request"));
		} else {
			validateLogoutResultsInBackground();
		}
		setStatus(Status.WAITING);
		return new ModelAndView("resultCaptured",
			ImmutableMap.of(
				"returnUrl", "/log-detail.html?log=" + getId()
			));
	}

	protected Object handleFrontchannelLogout(JsonObject requestParts) {
		setStatus(Status.RUNNING);
		frontchannelLogoutRequestParts = requestParts;
		if (postLogoutRedirectRequestParts == null) {
			eventLog.log(getName(), args("msg", "Received frontchannel request; waiting for front channel redirect"));
		} else {
			validateLogoutResultsInBackground();
		}
		setStatus(Status.WAITING);

		// https://openid.net/specs/openid-connect-frontchannel-1_0.html#RPLogout
		HttpHeaders headers = new HttpHeaders();
		headers.set("Cache-Control", "no-store");
		return new ResponseEntity<Object>("", headers, HttpStatus.OK);
	}

	private void validateLogoutResultsInBackground() {
		getTestExecutionManager().runInBackground(() -> {
			setStatus(Status.RUNNING);
			validateLogoutResults();
			return "done";
		});
	}

}
