package net.openid.conformance.openid.client.logout;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.logout.EnsureBackChannelLogoutUriResponseStatusCodeIs400;
import net.openid.conformance.condition.as.logout.OIDCCSignLogoutTokenWithAlgNone;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-rp-backchannel-rpinitlogout-alg-none",
	displayName = "OIDCC: Relying party test, back channel logout request signed using alg 'none'.",
	summary = "The client is expected to make an authorization request " +
		"(also a token request and a optionally a userinfo request when applicable)," +
		" then terminate the session by calling the end_session_endpoint (RP-Initiated Logout)," +
		" at this point the conformance suite will send a back channel logout request containing " +
		" a logout_token signed using alg 'none' which should be rejected, " +
		" then the RP is expected to handle post logout URI redirect." +
		" Corresponds to rp-backchannel-rpinitlogout-lt-alg-none in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestBackChannelLogoutAlgNone extends AbstractOIDCCClientBackChannelLogoutTest
{

	@Override
	protected void signLogoutToken() {
		callAndStopOnFailure(OIDCCSignLogoutTokenWithAlgNone.class);
	}

	@Override
	protected void validateBackChannelLogoutResponse() {
		super.validateBackChannelLogoutResponse();
		callAndContinueOnFailure(EnsureBackChannelLogoutUriResponseStatusCodeIs400.class, Condition.ConditionResult.FAILURE,
			"OIDCBCL-2.8");
	}

}
