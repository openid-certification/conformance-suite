package net.openid.conformance.openid;

import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenIdEmailPhoneAddressProfile;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-all
@PublishTestModule(
	testName = "oidcc-scope-all",
	displayName = "OIDCC: check all scopes",
	summary = "This test requests authorization with address, email, phone and profile scopes.",
	profile = "OIDCC"
)
public class OIDCCScopeAll extends AbstractOIDCCReturnedClaimsServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		callAndStopOnFailure(SetScopeInClientConfigurationToOpenIdEmailPhoneAddressProfile.class);
		super.skipTestIfScopesNotSupported();
	}

}
