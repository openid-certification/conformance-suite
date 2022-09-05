package net.openid.conformance.openid.client.logout;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

/**
 * The difference between this one and OIDCCClientTestFrontChannelLogoutRPInitiated is:
 * - This test does not wait for the RP to call the end_session_endpoint
 * - Prompts the tester to click the proceed button to view the front channel logout page
 */
@PublishTestModule(
	testName = "oidcc-client-test-rp-frontchannel-opinitlogout",
	displayName = "OIDCC: Relying party test, OP initiated front channel logout.",
	summary = "The client is expected to make an authorization request " +
		"(also a token request and a optionally a userinfo request when applicable)," +
		" then the tester will be prompted to visit the OP initiated logout page" +
		" then the RP is expected to handle post logout URI redirect." +
		" This is a new test with no equivalent in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestFrontChannelLogoutOPInitiated extends AbstractOIDCCClientFrontChannelLogoutTest
{

	protected boolean isAuthorizationCodeRequestUnexpected() {
		return responseType.includesIdToken();
	}

	@Override
	protected Object handleAuthorizationEndpointRequest(String requestId) {
		if(isAuthorizationCodeRequestUnexpected()) {
			waitAndSendLogoutRequest();
		}
		return super.handleAuthorizationEndpointRequest(requestId);
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {
		if(isAuthorizationCodeRequestUnexpected()) {
			throw new TestFailureException(getId(), "Token request is unexpected for this test");
		} else {
			waitAndSendLogoutRequest();
		}
		return super.authorizationCodeGrantType(requestId);
	}

	protected void sendFrontChannelLogoutRequest() {
		createFrontChannelLogoutRequestUrl();
		performRedirect();
	}

	protected void performRedirect() {
		String redirectTo = env.getString("base_url") + "/frontchannel_logout_handler";

		eventLog.log(getName(), args("msg", "Redirecting to front channel logout handler",
			"redirect_to", redirectTo,
			"http", "redirect"));

		setStatus(Status.WAITING);
		browser.goToUrl(redirectTo);
	}

	protected void waitAndSendLogoutRequest() {
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(2 * 1000);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				sendFrontChannelLogoutRequest();
			}
			return "done";
		});
	}
}
