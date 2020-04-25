package net.openid.conformance.openid.client.logout;

import net.openid.conformance.condition.as.logout.AddInvalidEventsClaimToLogoutToken;
import net.openid.conformance.condition.as.logout.EnsureBackChannelLogoutUriResponseStatusCodeIs400;
import net.openid.conformance.condition.as.logout.RemoveEventsClaimFromLogoutToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-rp-backchannel-rpinitlogout-wrong-event",
	displayName = "OIDCC: Relying party test, back channel logout request with an invalid iss.",
	summary = "The client is expected to make an authorization request " +
		"(also a token request and a optionally a userinfo request when applicable)," +
		" then the RP terminates the session by calling the end_session_endpoint (RP-Initiated Logout)," +
		" then Handle Post Logout URI Redirect" +
		" then the OP(the test suite) will send a back channel logout request with an invalid events claim." +
		" Corresponds to rp-backchannel-rpinitlogout-lt-wrong-event in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestBackChannelLogoutWrongEvent extends AbstractOIDCCClientBackChannelLogoutTest
{

	@Override
	protected void customizeLogoutTokenClaims(){
		callAndStopOnFailure(AddInvalidEventsClaimToLogoutToken.class, "OIDCBCL-2.4");
	}

	@Override
	protected void validateBackChannelLogoutResponse() {
		super.validateBackChannelLogoutResponse();
		callAndStopOnFailure(EnsureBackChannelLogoutUriResponseStatusCodeIs400.class, "OIDCBCL-2.8");
	}

}
