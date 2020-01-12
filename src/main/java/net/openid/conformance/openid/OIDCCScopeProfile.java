package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddProfileScopeToClientConfiguration;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-profile
@PublishTestModule(
	testName = "oidcc-scope-profile",
	displayName = "OIDCC: check profile scope",
	summary = "This test requests authorization with profile scope.",
	profile = "OIDCC",
	configurationFields = {
			"server.discoveryUrl",
			"client.scope",
			"client2.scope",
			"resource.resourceUrl"
	}
)
public class OIDCCScopeProfile extends AbstractOIDCCScopesServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		callAndStopOnFailure(AddProfileScopeToClientConfiguration.class);
		super.skipTestIfScopesNotSupported();
	}

}
