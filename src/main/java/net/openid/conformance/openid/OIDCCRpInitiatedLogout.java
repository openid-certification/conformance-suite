package net.openid.conformance.openid;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInPostLogoutRedirect;
import net.openid.conformance.condition.client.CheckPostLogoutState;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_RpInitLogout
// https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-RpInitLogout.json
@PublishTestModule(
	testName = "oidcc-rp-initiated-logout",
	displayName = "OIDCC: rp initiated logout",
	summary = "This test performs a normal authorization flow at the OP, then sends the user to the end_session_endpoint. It validates the OP correctly sends the user to the post_logout_redirect_uri, then tries another authentication with prompt=none which must return an error (as the user has been logged out).\n\nIf using static client registration you must register a post_logout_redirect_uri, the same url as the redirect url but replacing the portion after the alias with '/post_logout_redirect'.",
	profile = "OIDCC"
)
public class OIDCCRpInitiatedLogout extends AbstractOIDCCRpInitiatedLogout {

	protected void validateLogoutResults(JsonObject requestParts){

		env.putObject("post_logout_redirect", requestParts);

		eventLog.startBlock("Verify frontchannel post logout redirect");
		callAndContinueOnFailure(CheckPostLogoutState.class, Condition.ConditionResult.FAILURE, "OIDCRIL-2");
		callAndContinueOnFailure(CheckForUnexpectedParametersInPostLogoutRedirect.class, Condition.ConditionResult.WARNING, "OIDCRIL-2");
		eventLog.endBlock();

		// do the prompt=none authorization request to check logout happened
		performAuthorizationFlow();
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
			if (!expectingLogoutConfirmation) {
				throw new TestFailureException(getId(), "post_logout_redirect called when not expected");
			}
			expectingLogoutConfirmation = false;
			validateLogoutResults(requestParts);
			return "done";
		});
		return new ModelAndView("resultCaptured",
			ImmutableMap.of(
				"returnUrl", "/log-detail.html?log=" + getId()
			));
	}

}
