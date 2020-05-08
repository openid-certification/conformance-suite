package net.openid.conformance.openid;

import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenIdProfile;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-profile
@PublishTestModule(
	testName = "oidcc-scope-profile",
	displayName = "OIDCC: check profile scope",
	summary = "This test requests authorization with profile scope.",
	profile = "OIDCC"
)
public class OIDCCScopeProfile extends AbstractOIDCCReturnedClaimsServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		callAndStopOnFailure(SetScopeInClientConfigurationToOpenIdProfile.class);
		super.skipTestIfScopesNotSupported();
	}

}
