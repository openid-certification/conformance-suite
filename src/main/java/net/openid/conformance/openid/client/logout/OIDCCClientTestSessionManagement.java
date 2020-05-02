package net.openid.conformance.openid.client.logout;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-session-management",
	displayName = "OIDCC: Relying party test, session management",
	summary = "The client is expected to make an authorization request " +
		"(also a token request and a optionally a userinfo request when applicable)," +
		" then check session status expecting unchanged " +
		" (send at least one http request to check_session_iframe and at least one postMessage call)," +
		" then terminate the session by calling the end_session_endpoint (RP-Initiated Logout)," +
		" then Handle Post Logout URI Redirect, " +
		" then send a postMessage call to check_session_iframe and" +
		" postMessage returns 'changed'." +
		" Corresponds to rp-init-logout-session in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestSessionManagement extends AbstractOIDCCClientLogoutTest {

	/**
	 * This test expects at least one postMessage call to check_session_iframe before logout
	 * and one after logout
	 * Number of allowed postMessage calls before logout is unlimited
	 * @return
	 */
	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if(receivedAuthorizationRequest && receivedEndSessionRequest && receivedCheckSessionRequestBeforeLogout && receivedCheckSessionRequestAfterLogout) {
			fireTestFinished();
			return true;
		}
		return false;
	}

	@Override
	protected void configureServerConfiguration() {
		super.configureServerConfiguration();
		expose("check_session_iframe", env.getString("base_url") + "/check_session_iframe");
	}
}
