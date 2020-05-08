package net.openid.conformance.openid;

import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenIdAddress;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-address
@PublishTestModule(
	testName = "oidcc-scope-address",
	displayName = "OIDCC: check address scope",
	summary = "This test requests authorization with address scope.",
	profile = "OIDCC"
)
public class OIDCCScopeAddress extends AbstractOIDCCReturnedClaimsServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		callAndStopOnFailure(SetScopeInClientConfigurationToOpenIdAddress.class);
		super.skipTestIfScopesNotSupported();
	}

}
