package net.openid.conformance.openid;

import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenIdPhone;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-phone
@PublishTestModule(
	testName = "oidcc-scope-phone",
	displayName = "OIDCC: check phone scope",
	summary = "This test requests authorization with phone scope.",
	profile = "OIDCC"
)
public class OIDCCScopePhone extends AbstractOIDCCReturnedClaimsServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		callAndStopOnFailure(SetScopeInClientConfigurationToOpenIdPhone.class);
		super.skipTestIfScopesNotSupported();
	}

}
