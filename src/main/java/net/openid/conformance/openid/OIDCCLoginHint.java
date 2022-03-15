package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddLoginHintFromConfigurationToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Req_login_hint
@PublishTestModule(
	testName = "oidcc-login-hint",
	displayName = "OIDCC: login_hint test",
	// cookies message it taken from python test. I don't think anything goes wrong if you there's an existing login session.
	summary = "This test calls the authorization endpoint with a login_hint (provided in configuration, or buffy@<your issuer hostname> if not), which must at a minimum not result in errors. Please remove any cookies you may have received from the OpenID Provider before proceeding. This test requests that you log in as a specific user, so a fresh login page is needed.",
	profile = "OIDCC",
	configurationFields = {
		"server.login_hint"
	}
)
public class OIDCCLoginHint extends AbstractOIDCCServerTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(condition(AddLoginHintFromConfigurationToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1"));
	}

}
