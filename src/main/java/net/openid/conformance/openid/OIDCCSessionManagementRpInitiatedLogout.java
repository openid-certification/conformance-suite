package net.openid.conformance.openid;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscCheckSessionIframe;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInPostLogoutRedirect;
import net.openid.conformance.condition.client.CheckPostLogoutState;
import net.openid.conformance.condition.client.CheckSecondSessionResultIsChanged;
import net.openid.conformance.condition.client.CheckSessionResultIsUnchanged;
import net.openid.conformance.condition.client.ExtractSessionStateFromAuthorizationResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Corresponds to https://www.heenan.me.uk/~joseph/2020-06-05-test_desc_op.html#OP_Session_RpInitLogout
// https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-Session-RpInitLogout.json
@PublishTestModule(
	testName = "oidcc-session-management-rp-initiated-logout",
	displayName = "OIDCC: Session management - rp initiated logout",
	summary = "This test performs a normal authorization flow at the OP, then sends the user to the end_session_endpoint, and uses check_session_iframe to check the session state before and after the logout.\n\nIf using static client registration you must register a post_logout_redirect_uri, the same url as the redirect url but replacing the portion after the alias with '/post_logout_redirect'.\n\nPlease note that this test may not work in some browsers.",
	profile = "OIDCC"
)
public class OIDCCSessionManagementRpInitiatedLogout extends AbstractOIDCCRpInitiatedLogout {

	protected void validateLogoutResults(JsonObject requestParts){

		env.putObject("post_logout_redirect", requestParts);

		eventLog.startBlock("Verify frontchannel post logout redirect");
		callAndContinueOnFailure(CheckPostLogoutState.class, Condition.ConditionResult.FAILURE, "OIDCRIL-2");
		callAndContinueOnFailure(CheckForUnexpectedParametersInPostLogoutRedirect.class, Condition.ConditionResult.WARNING, "OIDCRIL-2");
		eventLog.endBlock();

		checkSessionState(false);
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		callAndStopOnFailure(CheckDiscCheckSessionIframe.class, "OIDCSM-3.3");
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		checkSessionState(true);
	}

	private void checkSessionState(boolean firstTime) {
		// check session state with iframe
		callAndStopOnFailure(ExtractSessionStateFromAuthorizationResponse.class, "OIDCSM-2");

		String redirectTo = env.getString("base_url") +
			(firstTime ? "/session_verify" : "/second_session_verify");

		eventLog.log(getName(), args("msg", "Redirecting to our session check page",
			"redirect_to", redirectTo,
			"http", "redirect"));

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("post_logout_redirect")) {
			return handlePostLogoutRedirect(requestParts);
		} else if (path.equals("session_verify")) {
			return handleSessionVerify(requestParts, true);
		} else if (path.equals("second_session_verify")) {
			return handleSessionVerify(requestParts, false);
		} else if (path.equals("rp_session_iframe")) {
			return handleRpSessionIframe(requestParts, true);
		} else if (path.equals("second_rp_session_iframe")) {
			return handleRpSessionIframe(requestParts, false);
		} else if (path.equals("session_result")) {
			return handleSessionResult(requestParts, true);
		} else if (path.equals("second_session_result")) {
			return handleSessionResult(requestParts, false);
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	protected void validateFirstSessionCheckResult(JsonObject requestParts) {
		env.putObject("session_result", requestParts);
		callAndContinueOnFailure(CheckSessionResultIsUnchanged.class, Condition.ConditionResult.FAILURE, "OIDCSM-3.1");

		// now carry on and log the user out
		super.onPostAuthorizationFlowComplete();
	}

	protected void validateSecondSessionCheckResult(JsonObject requestParts) {
		env.putObject("second_session_result", requestParts);

		callAndContinueOnFailure(CheckSecondSessionResultIsChanged.class, Condition.ConditionResult.FAILURE, "OIDCSM-3.1");

		// we could call performAuthorizationFlow() to do a prompt=none authorization request to check logout
		// happened, but the python didn't so don't

		fireTestFinished();
	}

	protected Object handleSessionResult(JsonObject requestParts, boolean firstTime) {
		getTestExecutionManager().runInBackground(() -> {
			setStatus(Status.RUNNING);
			if (firstTime) {
				validateFirstSessionCheckResult(requestParts);
			} else {
				validateSecondSessionCheckResult(requestParts);
			}
			return "done";
		});

		return new ModelAndView("resultCaptured",
			ImmutableMap.of(
				"returnUrl", "/log-detail.html?log=" + getId()
			));

	}

	protected Object handleSessionVerify(JsonObject requestParts, boolean firstTime) {
		setStatus(Status.RUNNING);
		String checkSessionIframeUrl = env.getString("server", "check_session_iframe");
		String baseUrl = env.getString("base_url");
		String rpSessionIframeUrl = baseUrl + (firstTime ? "/rp_session_iframe" : "/second_rp_session_iframe");
		setStatus(Status.WAITING);
		return new ModelAndView("sessionVerify",
			ImmutableMap.of(
				"check_session_iframe", checkSessionIframeUrl,
				"session_iframe_unchanged", rpSessionIframeUrl
			));
	}

	protected Object handleRpSessionIframe(JsonObject requestParts, boolean firstTime) {
		setStatus(Status.RUNNING);
		String clientId = env.getString("client", "client_id");
		String sessionState = env.getString("session_state");
		String issuer = env.getString("server", "issuer");
		String sessionResultUrl;
		if (firstTime) {
			sessionResultUrl = env.getString("base_url") + "/session_result";
		} else {
			sessionResultUrl = env.getString("base_url") + "/second_session_result";
		}
		setStatus(Status.WAITING);
		return new ModelAndView("rpSessionIframe",
			ImmutableMap.of(
				"client_id", clientId,
				"session_state", sessionState,
				"issuer", issuer,
				"service_url", sessionResultUrl
			));
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
