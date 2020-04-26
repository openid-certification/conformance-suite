package net.openid.conformance.openid.client.logout;

import net.openid.conformance.condition.as.logout.EnsureEndSessionEndpointRequestContainsStateParameter;
import net.openid.conformance.condition.as.logout.RemoveStateFromPostLogoutRedirectUriParams;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-rp-init-logout-no-state",
	displayName = "OIDCC: Relying party test, session management. " +
		"The OP does not return the 'state' query parameter when redirecting the User Agent back to the RP.",
	summary = "The client is expected to make an authorization request " +
		"(also a token request and a optionally a userinfo request when applicable)," +
		" then terminate the session by calling the end_session_endpoint (RP-Initiated Logout) with a state parameter," +
		" then Handle Post Logout URI Redirect which will be called without the state parameter." +
		" Corresponds to rp-init-logout-no-state in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestRPInitLogoutNoState extends AbstractOIDCCClientLogoutTest
{

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if( receivedAuthorizationRequest && receivedEndSessionRequest) {
			fireTestFinished();
			return true;
		}
		return false;
	}

	@Override
	protected void validateEndSessionEndpointParameters() {
		super.validateEndSessionEndpointParameters();
		callAndStopOnFailure(EnsureEndSessionEndpointRequestContainsStateParameter.class);
	}

	@Override
	protected void customizeEndSessionEndpointResponseParameters() {
		callAndStopOnFailure(RemoveStateFromPostLogoutRedirectUriParams.class, "OIDCSM-5");
	}
}
