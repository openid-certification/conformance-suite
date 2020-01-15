package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddAddressScopeToClientConfiguration;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-address
@PublishTestModule(
	testName = "oidcc-scope-address",
	displayName = "OIDCC: check address scope",
	summary = "This test requests authorization with address scope.",
	profile = "OIDCC",
	configurationFields = {
			"server.discoveryUrl",
			"client.scope",
			"client2.scope"
	}
)
public class OIDCCScopeAddress extends AbstractOIDCCScopesServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		callAndStopOnFailure(AddAddressScopeToClientConfiguration.class);
		super.skipTestIfScopesNotSupported();
	}

}
