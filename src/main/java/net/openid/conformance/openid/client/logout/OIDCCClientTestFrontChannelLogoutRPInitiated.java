package net.openid.conformance.openid.client.logout;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-rp-frontchannel-rpinitlogout",
	displayName = "OIDCC: Relying party test, RP initiated front channel logout.",
	summary = "The client is expected to make an authorization request " +
		"(also a token request and a optionally a userinfo request when applicable)." +
		" Then the RP should terminate the session by calling the end_session_endpoint (RP-Initiated Logout)," +
		" at this point the conformance suite will render the front channel logout page and " +
		" then the RP is expected to handle post logout URI redirect." +
		" Corresponds to rp-frontchannel-rpinitlogout in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestFrontChannelLogoutRPInitiated extends AbstractOIDCCClientFrontChannelLogoutTest
{

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if( receivedAuthorizationRequest && receivedEndSessionRequest && receivedFrontChannelLogoutCompletedCallback) {
			fireTestFinished();
			return true;
		}
		return false;
	}


}
