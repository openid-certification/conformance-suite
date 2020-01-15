package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddEmailPhoneAddressProfileScopeToClientConfiguration;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-all
@PublishTestModule(
	testName = "oidcc-scope-all",
	displayName = "OIDCC: check all scopes",
	summary = "This test requests authorization with address, email, phone and profile scopes.",
	profile = "OIDCC",
	configurationFields = {
			"server.discoveryUrl",
			"client.scope",
			"client2.scope"
	}
)
public class OIDCCScopeAll extends AbstractOIDCCScopesServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		callAndStopOnFailure(AddEmailPhoneAddressProfileScopeToClientConfiguration.class);
		super.skipTestIfScopesNotSupported();
	}

}
