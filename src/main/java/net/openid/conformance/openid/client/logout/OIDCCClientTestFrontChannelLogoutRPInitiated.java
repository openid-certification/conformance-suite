package net.openid.conformance.openid.client.logout;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-rp-frontchannel-rpinitlogout",
	displayName = "OIDCC: Relying party test, RP initiated front channel logout.",
	summary = "The client is expected to make an authorization request " +
		"(also a token request and a optionally a userinfo request when applicable)," +
		" then the RP terminates the session by calling the end_session_endpoint (RP-Initiated Logout)," +
		" then Handle Post Logout URI Redirect." +
		" Corresponds to rp-frontchannel-rpinitlogout in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestFrontChannelLogoutRPInitiated extends AbstractOIDCCClientLogoutTest
{

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if( receivedAuthorizationRequest && receivedEndSessionRequest) {
			fireTestFinished();
			return true;
		}
		return false;
	}


}
