package net.openid.conformance.openid.client.logout;

import net.openid.conformance.condition.as.logout.AddInvalidStateToPostLogoutRedirectUriParams;
import net.openid.conformance.condition.as.logout.EnsureEndSessionEndpointRequestContainsStateParameter;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-rp-init-logout-other-state",
	displayName = "OIDCC: Relying party test, RP initiated logout. " +
		" The OP does return random 'state' query parameter when redirecting the User Agent back to the RP. ",
	summary = "The client is expected to make an authorization request " +
		"(also a token request and a optionally a userinfo request when applicable)," +
		" then terminate the session by calling the end_session_endpoint (RP-Initiated Logout) with a state parameter," +
		" at this point the conformance suite will " +
		" send a back channel logout request to the RP if only backchannel_logout_uri is set" +
		" or will send a front channel logout request to the RP if only frontchannel_logout_uri is set" +
		" or will send both front and back channel logout requests" +
		" if both backchannel_logout_uri and frontchannel_logout_uri are set," +
		" then the RP is expected to handle post logout URI redirect despite it being called with a different state parameter." +
		" Corresponds to rp-init-logout-other-state in the old test suite. " +
		"If the client does not send a state parameter, the test is skipped.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestRPInitLogoutInvalidState extends OIDCCClientTestRPInitLogout
{

	@Override
	protected void validateEndSessionEndpointParameters() {
		super.validateEndSessionEndpointParameters();
		skipTestIfStateIsOmitted();
		callAndStopOnFailure(EnsureEndSessionEndpointRequestContainsStateParameter.class);
	}

	@Override
	protected void customizeEndSessionEndpointResponseParameters() {
		callAndStopOnFailure(AddInvalidStateToPostLogoutRedirectUriParams.class, "OIDCRIL-2");
	}

}
