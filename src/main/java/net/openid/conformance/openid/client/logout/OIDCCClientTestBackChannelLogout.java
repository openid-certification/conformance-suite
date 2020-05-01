package net.openid.conformance.openid.client.logout;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.logout.EnsureBackChannelLogoutUriResponseStatusCodeIs200;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-rp-backchannel-rpinitlogout",
	displayName = "OIDCC: Relying party test, back channel logout.",
	summary = "The client is expected to make an authorization request " +
		"(also a token request and a optionally a userinfo request when applicable)," +
		" then terminate the session by calling the end_session_endpoint (RP-Initiated Logout)," +
		" at this point the conformance suite will send a back channel logout request to the RP, " +
		" then the RP is expected to handle post logout URI redirect." +
		" Corresponds to rp-backchannel-rpinitlogout in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestBackChannelLogout extends AbstractOIDCCClientBackChannelLogoutTest
{

	@Override
	protected void validateBackChannelLogoutResponse() {
		super.validateBackChannelLogoutResponse();
		callAndContinueOnFailure(EnsureBackChannelLogoutUriResponseStatusCodeIs200.class, Condition.ConditionResult.FAILURE,
			"OIDCBCL-2.8");
	}

}
