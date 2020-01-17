package net.openid.conformance.openid;

import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Registration_Dynamic
@PublishTestModule(
	testName = "oidcc-registration-dynamic",
	displayName = "OIDCC: dynamic registration",
	summary = "This test calls the dynamic registration endpoint. This should result in a successful registration.",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl"
	}
)
public class OIDCCRegistrationDynamic extends AbstractOIDCCDynamicRegistrationTest {

	@Override
	protected void performAuthorizationFlow() {
		// Don't need to test authorization here.
		fireTestFinished();
	}

}
