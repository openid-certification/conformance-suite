package net.openid.conformance.openid;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInPostLogoutRedirect;
import net.openid.conformance.condition.client.CheckNoPostLogoutState;
import net.openid.conformance.condition.client.RemoveStateFromEndSessionEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_RpInitLogout_No_state
@PublishTestModule(
	testName = "oidcc-rp-initiated-logout-no-state",
	displayName = "OIDCC: rp initiated logout",
	summary = "This test performs a normal authorization flow at the OP, then sends the user to the end_session_endpoint with no state parameter. It validates the OP correctly sends the user to the post_logout_redirect_uri with no state, then tries another authentication with prompt=none which must return an error (as the user has been logged out).\n\nIf using static client registration you must register a post_logout_redirect_uri, the same url as the redirect url but replacing the portion after the alias with '/post_logout_redirect'.",
	profile = "OIDCC"
)
public class OIDCCRpInitiatedLogoutNoState extends AbstractOIDCCRpInitiatedLogout {

	protected void validateLogoutResults(JsonObject requestParts){

		env.putObject("post_logout_redirect", requestParts);

		eventLog.startBlock("Verify frontchannel post logout redirect");
		callAndContinueOnFailure(CheckNoPostLogoutState.class, Condition.ConditionResult.FAILURE, "OIDCRIL-2");
		callAndContinueOnFailure(CheckForUnexpectedParametersInPostLogoutRedirect.class, Condition.ConditionResult.WARNING, "OIDCRIL-3");
		eventLog.endBlock();

		// do the prompt=none authorization request to check logout happened
		performAuthorizationFlow();
	}

	@Override
	protected void customiseEndSessionEndpointRequest() {
		callAndStopOnFailure(RemoveStateFromEndSessionEndpointRequest.class);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("post_logout_redirect")) {
			return handlePostLogoutRedirect(requestParts);
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	protected Object handlePostLogoutRedirect(JsonObject requestParts) {
		getTestExecutionManager().runInBackground(() -> {
			setStatus(Status.RUNNING);
			validateLogoutResults(requestParts);
			return "done";
		});
		return new ModelAndView("resultCaptured",
			ImmutableMap.of(
				"returnUrl", "/log-detail.html?log=" + getId()
			));
	}

}
