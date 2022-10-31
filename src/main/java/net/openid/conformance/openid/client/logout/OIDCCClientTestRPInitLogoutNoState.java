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
		" at this point the conformance suite will " +
		" send a back channel logout request to the RP if only backchannel_logout_uri is set" +
		" or will send a front channel logout request to the RP if only frontchannel_logout_uri is set" +
		" or will send both front and back channel logout requests" +
		" if both backchannel_logout_uri and frontchannel_logout_uri are set," +
		" then the RP is expected to handle post logout URI redirect which will be called without the state parameter." +
		" Corresponds to rp-init-logout-no-state in the old test suite. " +
		"If the client does not send a state parameter, the test is skipped.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestRPInitLogoutNoState extends OIDCCClientTestRPInitLogout
{

	@Override
	protected void validateEndSessionEndpointParameters() {
		super.validateEndSessionEndpointParameters();
		skipTestIfStateIsOmitted();
		callAndStopOnFailure(EnsureEndSessionEndpointRequestContainsStateParameter.class);
	}

	@Override
	protected void customizeEndSessionEndpointResponseParameters() {
		callAndStopOnFailure(RemoveStateFromPostLogoutRedirectUriParams.class, "OIDCRIL-2");
	}
}
